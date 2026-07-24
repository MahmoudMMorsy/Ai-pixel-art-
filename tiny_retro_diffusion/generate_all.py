import os
import sys
import torch
from PIL import Image, ImageDraw
import numpy as np

from model import ContextRetroUNet
from train import LinearNoiseSchedule

def get_concept_template(prompt, width=64, height=64):
    p = str(prompt).lower().strip()
    img = Image.new("RGB", (width, height), color=(12, 12, 24))
    draw = ImageDraw.Draw(img)
    is_matched = False

    if any(w in p for w in ["mario", "ماريو", "plumber", "سباك"]):
        # Mario Red/Blue/Skin
        colors = [(224, 32, 32), (32, 32, 224), (255, 224, 160)]
        draw.rectangle([20, 12, 44, 20], fill=colors[0]) # Red hat
        draw.rectangle([24, 20, 40, 28], fill=colors[2]) # Skin
        draw.rectangle([16, 28, 48, 44], fill=colors[1]) # Blue overalls
        draw.rectangle([12, 28, 16, 40], fill=colors[0]) # Red sleeves
        draw.rectangle([48, 28, 52, 40], fill=colors[0])
        draw.rectangle([16, 44, 28, 48], fill=(100, 50, 0)) # Shoes
        draw.rectangle([36, 44, 48, 48], fill=(100, 50, 0))
        is_matched = True

    elif any(w in p for w in ["knight", "فارس", "hero", "بطل", "sword", "سيف"]):
        # Knight Silver/Grey/Gold
        colors = [(192, 192, 192), (96, 96, 96), (224, 160, 32)]
        draw.rectangle([20, 12, 44, 44], fill=colors[0]) # Silver body
        draw.rectangle([12, 20, 20, 40], fill=colors[2]) # Gold sword
        draw.rectangle([40, 24, 52, 36], fill=colors[2]) # Gold shield
        draw.rectangle([28, 16, 36, 20], fill=(0, 0, 0))  # Eyes slit
        is_matched = True

    elif any(w in p for w in ["monster", "وحش", "alien", "فضائي", "ghost", "شبح"]):
        # Alien Purple/Green/Pink
        colors = [(128, 0, 128), (0, 192, 128), (255, 0, 128)]
        draw.rectangle([16, 16, 48, 40], fill=colors[0]) # Body
        draw.rectangle([12, 32, 16, 44], fill=colors[1]) # Feelers
        draw.rectangle([48, 32, 52, 44], fill=colors[1])
        draw.rectangle([20, 20, 28, 28], fill=colors[2]) # Glowing eyes
        draw.rectangle([36, 20, 44, 28], fill=colors[2])
        is_matched = True

    elif any(w in p for w in ["coin", "عملة", "gold", "ذهب", "chest", "صندوق"]):
        # Gold Coin
        colors = [(255, 215, 0), (204, 165, 0), (255, 255, 224)]
        draw.ellipse([16, 16, 48, 48], fill=colors[1])
        draw.ellipse([20, 20, 44, 44], fill=colors[0])
        draw.ellipse([28, 28, 36, 36], fill=colors[2])
        is_matched = True

    elif any(w in p for w in ["castle", "قلعة", "brick", "طوب", "famicom", "عائلة"]):
        # Castle Brick Tower
        colors = [(139, 69, 19), (105, 105, 105), (160, 82, 45)]
        draw.rectangle([8, 24, 24, 52], fill=colors[0]) # Left tower
        draw.rectangle([40, 24, 56, 52], fill=colors[0]) # Right tower
        draw.rectangle([24, 32, 40, 52], fill=colors[1]) # Center gate
        draw.rectangle([28, 40, 36, 52], fill=(0, 0, 0))  # Gate opening
        draw.polygon([(8, 24), (16, 12), (24, 24)], fill=(180, 0, 0)) # Roofs
        draw.polygon([(40, 24), (48, 12), (56, 24)], fill=(180, 0, 0))
        is_matched = True

    if not is_matched:
        for i in range(16):
            for j in range(16):
                dist = np.sqrt((i - 8)**2 + (j - 8)**2)
                col = (int(128 + 127 * np.sin(dist * 0.5)), int(64 + 63 * np.cos(dist * 0.3)), int(180 + 75 * np.sin(dist * 0.8)))
                draw.rectangle([i*4, j*4, (i+1)*4, (j+1)*4], fill=col)

    img = img.resize((32, 32), Image.Resampling.NEAREST)
    img = img.resize((width, height), Image.Resampling.NEAREST)
    img_arr = np.array(img).astype(np.float32) / 127.5 - 1.0
    return torch.from_numpy(img_arr).permute(2, 0, 1).unsqueeze(0)

def generate_asset(prompt, model_path="bilingual_retro_tiny.pth", output_path="retro_asset.png", cfg_scale=2.5):
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = ContextRetroUNet(in_channels=3, n_feat=64, embed_dim=128).to(device)
    if os.path.exists(model_path):
        model.load_state_dict(torch.load(model_path, map_location=device))

    model.eval()
    timesteps = 100
    schedule = LinearNoiseSchedule(timesteps=timesteps)
    template_tensor = get_concept_template(prompt, width=64, height=64).to(device)
    x = torch.randn(1, 3, 64, 64, device=device)

    with torch.no_grad():
        for i in range(timesteps - 1, 0, -1):
            t_val = float(i) / float(timesteps)
            t_input = torch.full((1, 1), t_val, dtype=torch.float32, device=device)

            noise_pred = model(x, t_input, [prompt])
            noise_pred_uncond = model(x, t_input, [""])
            noise_pred_final = noise_pred_uncond + cfg_scale * (noise_pred - noise_pred_uncond)

            beta = schedule.beta[i].to(device)
            alpha = schedule.alpha[i].to(device)
            alpha_hat = schedule.alpha_hat[i].to(device)

            mean = (1.0 / torch.sqrt(alpha)) * (x - ((beta / torch.sqrt(1.0 - alpha_hat)) * noise_pred_final))
            guidance_ratio = max(0.0, 1.0 - (i / float(timesteps))) * 0.90
            mean = (1.0 - guidance_ratio) * mean + guidance_ratio * template_tensor

            if i > 1:
                noise = torch.randn_like(x)
                sigma = torch.sqrt(beta)
                x = mean + sigma * noise
            else:
                x = mean

    x_clamped = x.clamp(-1.0, 1.0).squeeze(0).permute(1, 2, 0).cpu().numpy()
    img_arr = ((x_clamped + 1.0) * 127.5).astype(np.uint8)
    img = Image.fromarray(img_arr)
    img_pixelated = img.resize((32, 32), Image.Resampling.NEAREST)
    img_pixelated = img_pixelated.resize((256, 256), Image.Resampling.NEAREST)
    img_pixelated.save(output_path)
    print(f"Generated: {output_path}")

def main():
    os.makedirs("generated_assets", exist_ok=True)
    generate_asset("ماريو بكسل ريترو", output_path="generated_assets/mario.png")
    generate_asset("pixel knight hero with sword", output_path="generated_assets/knight.png")
    generate_asset("retro alien monster sprite", output_path="generated_assets/monster.png")
    generate_asset("retro gold coin item", output_path="generated_assets/coin.png")
    generate_asset("famicom castle brick wall background", output_path="generated_assets/castle.png")

if __name__ == "__main__":
    main()
