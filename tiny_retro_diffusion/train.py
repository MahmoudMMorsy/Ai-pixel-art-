import os
import sys
import torch
import torch.nn as nn
from torch.utils.data import Dataset, DataLoader
from PIL import Image, ImageDraw
import numpy as np
from tqdm import tqdm
import json

from model import ContextRetroUNet

class RetroPixelDataset(Dataset):
    """
    Handles a rich dataset of clean, high-quality classic
    NES, Game Boy, and Famicom style pixel-art sprites.
    Loads real images and bilingual descriptions (Arabic & English) from the scraped and auto-tagged
    metadata (`dataset/metadata_tagged.json`) if available. Otherwise, dynamically synthesizes
    high-quality fallback assets.
    """
    def __init__(self, size=500, img_size=64, metadata_path="dataset/metadata_tagged.json"):
        self.size = size
        self.img_size = img_size
        self.real_images = []

        if os.path.exists(metadata_path):
            try:
                with open(metadata_path, "r", encoding="utf-8") as f:
                    self.metadata = json.load(f)

                for img_name, info in self.metadata.items():
                    path = info["filepath"]
                    if os.path.exists(path):
                        self.real_images.append({
                            "path": path,
                            "prompt_en": info["prompt_en"],
                            "prompt_ar": info["prompt_ar"]
                        })

                if self.real_images:
                    print(f"📊 Loaded {len(self.real_images)} real auto-tagged pixel art images for training!")
            except Exception as e:
                print(f"⚠️ Warning: Failed to parse metadata file {metadata_path}: {e}")

        self.classes = [
            {
                "id": "mario",
                "prompts": ["mario sprite", "ماريو بكسل", "retro plumber", "شخصية ماريو"],
                "color_scheme": [(224, 32, 32), (32, 32, 224), (255, 224, 160)], # Red, Blue, Skin
                "shape": "plumber"
            },
            {
                "id": "knight",
                "prompts": ["pixel knight", "فارس بكسل", "hero with sword", "بطل مع سيف"],
                "color_scheme": [(192, 192, 192), (96, 96, 96), (224, 160, 32)], # Silver, Grey, Gold
                "shape": "shield"
            },
            {
                "id": "monster",
                "prompts": ["retro monster", "وحش بكسل", "alien enemy", "وحش فاميكوم"],
                "color_scheme": [(128, 0, 128), (0, 192, 128), (255, 0, 128)], # Purple, Green, Pink
                "shape": "alien"
            },
            {
                "id": "coin",
                "prompts": ["retro gold coin", "عملة ذهبية ريترو", "gold coin sprite", "عملة ذهبية للألعاب"],
                "color_scheme": [(255, 215, 0), (204, 165, 0), (255, 255, 224)], # Gold, Dark Gold, Highlight
                "shape": "circle"
            },
            {
                "id": "castle",
                "prompts": ["retro brick castle", "قلعة ريترو", "famicom castle", "قلعة بكسل"],
                "color_scheme": [(139, 69, 19), (105, 105, 105), (160, 82, 45)], # Brown, Brick Grey, Sienna
                "shape": "castle"
            }
        ]

    def __len__(self):
        return self.size

    def __getitem__(self, idx):
        if self.real_images:
            item = self.real_images[idx % len(self.real_images)]
            prompt = item["prompt_ar"] if idx % 2 == 0 else item["prompt_en"]

            try:
                img = Image.open(item["path"]).convert("RGB")
                img = img.resize((self.img_size, self.img_size), Image.Resampling.NEAREST)

                img_arr = np.array(img).astype(np.float32) / 127.5 - 1.0
                img_tensor = torch.from_numpy(img_arr).permute(2, 0, 1) # [3, H, W]
                return img_tensor, prompt
            except Exception as e:
                pass

        cls_idx = idx % len(self.classes)
        cls_info = self.classes[cls_idx]
        prompt = cls_info["prompts"][idx % len(cls_info["prompts"])]

        img = Image.new("RGB", (self.img_size, self.img_size), color=(12, 12, 24))
        draw = ImageDraw.Draw(img)
        colors = cls_info["color_scheme"]

        if cls_info["shape"] == "plumber":
            draw.rectangle([20, 12, 44, 20], fill=colors[0])
            draw.rectangle([24, 20, 40, 28], fill=colors[2])
            draw.rectangle([16, 28, 48, 44], fill=colors[1])
            draw.rectangle([12, 28, 16, 40], fill=colors[0])
            draw.rectangle([48, 28, 52, 40], fill=colors[0])
            draw.rectangle([16, 44, 28, 48], fill=(100, 50, 0))
            draw.rectangle([36, 44, 48, 48], fill=(100, 50, 0))
        elif cls_info["shape"] == "shield":
            draw.rectangle([20, 12, 44, 44], fill=colors[0])
            draw.rectangle([12, 20, 20, 40], fill=colors[2])
            draw.rectangle([40, 24, 52, 36], fill=colors[2])
            draw.rectangle([28, 16, 36, 20], fill=(0, 0, 0))
        elif cls_info["shape"] == "alien":
            draw.rectangle([16, 16, 48, 40], fill=colors[0])
            draw.rectangle([12, 32, 16, 44], fill=colors[1])
            draw.rectangle([48, 32, 52, 44], fill=colors[1])
            draw.rectangle([20, 20, 28, 28], fill=colors[2])
            draw.rectangle([36, 20, 44, 28], fill=colors[2])
        elif cls_info["shape"] == "circle":
            draw.ellipse([16, 16, 48, 48], fill=colors[1])
            draw.ellipse([20, 20, 44, 44], fill=colors[0])
            draw.ellipse([28, 28, 36, 36], fill=colors[2])
        else:
            draw.rectangle([8, 24, 24, 52], fill=colors[0])
            draw.rectangle([40, 24, 56, 52], fill=colors[0])
            draw.rectangle([24, 32, 40, 52], fill=colors[1])
            draw.rectangle([28, 40, 36, 52], fill=(0, 0, 0))
            draw.polygon([(8, 24), (16, 12), (24, 24)], fill=(180, 0, 0))
            draw.polygon([(40, 24), (48, 12), (56, 24)], fill=(180, 0, 0))

        img = img.resize((32, 32), Image.Resampling.NEAREST)
        img = img.resize((self.img_size, self.img_size), Image.Resampling.NEAREST)

        img_arr = np.array(img).astype(np.float32) / 127.5 - 1.0
        img_tensor = torch.from_numpy(img_arr).permute(2, 0, 1)

        return img_tensor, prompt

class LinearNoiseSchedule:
    def __init__(self, timesteps=300, beta_start=1e-4, beta_end=0.02):
        self.timesteps = timesteps
        self.beta = torch.linspace(beta_start, beta_end, timesteps)
        self.alpha = 1.0 - self.beta
        self.alpha_hat = torch.cumprod(self.alpha, dim=0)

    def noise_images(self, x, t):
        sqrt_alpha_hat = torch.sqrt(self.alpha_hat[t])[:, None, None, None]
        sqrt_one_minus_alpha_hat = torch.sqrt(1.0 - self.alpha_hat[t])[:, None, None, None]
        noise = torch.randn_like(x)
        return sqrt_alpha_hat * x + sqrt_one_minus_alpha_hat * noise, noise

def train_model(epochs=5, batch_size=16, lr=1e-3, save_path="bilingual_retro_tiny.pth"):
    print("================================================================")
    print("   Training Bilingual Tiny Retro Diffusion Model from Scratch")
    print("================================================================")

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Device being used: {device}")

    dataset = RetroPixelDataset(size=300, img_size=64)
    dataloader = DataLoader(dataset, batch_size=batch_size, shuffle=True)

    model = ContextRetroUNet(in_channels=3, n_feat=64, embed_dim=128).to(device)
    schedule = LinearNoiseSchedule(timesteps=300)

    optimizer = torch.optim.AdamW(model.parameters(), lr=lr)
    loss_fn = nn.MSELoss()

    model.train()
    for epoch in range(epochs):
        print(f"\n--- Starting Epoch {epoch+1}/{epochs} ---")
        epoch_loss = 0.0

        pbar = tqdm(dataloader)
        for images, prompts in pbar:
            images = images.to(device)
            B = images.shape[0]

            t_steps = torch.randint(low=1, high=300, size=(B,), device=device)
            noisy_imgs, noise = schedule.noise_images(images, t_steps)

            t_input = (t_steps.float() / 300.0)[:, None]
            predicted_noise = model(noisy_imgs, t_input, prompts)

            loss = loss_fn(predicted_noise, noise)

            optimizer.zero_grad()
            loss.backward()
            optimizer.step()

            epoch_loss += loss.item()
            pbar.set_description(f"Loss: {loss.item():.4f}")

        avg_loss = epoch_loss / len(dataloader)
        print(f"Average Epoch {epoch+1} Loss: {avg_loss:.5f}")

    print(f"\nSaving final trained model parameters to: {save_path}")
    torch.save(model.state_dict(), save_path)
    print("🎉 Training pipeline complete! Model is ready for standalone retro inference.")

if __name__ == "__main__":
    train_model(epochs=3, batch_size=16, lr=1e-3)
