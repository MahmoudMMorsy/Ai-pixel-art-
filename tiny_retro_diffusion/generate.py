import os
import sys
import torch
from PIL import Image
import numpy as np

from model import ContextRetroUNet
from train import LinearNoiseSchedule

def sample_images(prompt, model_path="bilingual_retro_tiny.pth", output_path="retro_generated.png", cfg_scale=2.0):
    """
    Infers the ContextRetroUNet model locally on CPU/GPU to generate a pixel-perfect
    retro image corresponding to the Arabic or English text prompt.
    Uses Classifier-Free Guidance (CFG) for maximum prompt alignment.
    """
    print(f"=== Standalone Retro Generation for Prompt: '{prompt}' ===")
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

    # Load model and state dictionary
    model = ContextRetroUNet(in_channels=3, n_feat=64, embed_dim=128).to(device)
    if os.path.exists(model_path):
        print(f"Loading weights from: {model_path}")
        model.load_state_dict(torch.load(model_path, map_location=device))
    else:
        print(f"⚠️ Warning: Checkpoint '{model_path}' not found! Generating with initialized weights (for testing/structure check).")

    model.eval()

    # Initialize schedule and reverse diffusion sampler parameters
    timesteps = 300
    schedule = LinearNoiseSchedule(timesteps=timesteps)

    # Generate starting random Gaussian noise
    x = torch.randn(1, 3, 64, 64, device=device)

    # Run reverse iterative denoising loop
    with torch.no_grad():
        for i in range(timesteps - 1, 0, -1):
            # Compute time-step inputs
            t_val = float(i) / float(timesteps)
            t_input = torch.full((1, 1), t_val, dtype=torch.float32, device=device)

            # Predict noise with prompt and unconditioned null prompt (CFG)
            noise_pred = model(x, t_input, [prompt])
            noise_pred_uncond = model(x, t_input, [""])

            # Perform Classifier-Free Guidance mix
            # formula: uncond + cfg_scale * (cond - uncond)
            noise_pred_final = noise_pred_uncond + cfg_scale * (noise_pred - noise_pred_uncond)

            # Retrieve parameters from schedule
            beta = schedule.beta[i].to(device)
            alpha = schedule.alpha[i].to(device)
            alpha_hat = schedule.alpha_hat[i].to(device)

            # One step reverse diffusion step formulation (Ho 2020)
            mean = (1.0 / torch.sqrt(alpha)) * (x - ((beta / torch.sqrt(1.0 - alpha_hat)) * noise_pred_final))

            if i > 1:
                noise = torch.randn_like(x)
                # Add tiny variance noise
                sigma = torch.sqrt(beta)
                x = mean + sigma * noise
            else:
                x = mean

    # Post-process tensor [-1, 1] to [0, 255] RGB PIL Image
    x_clamped = x.clamp(-1.0, 1.0).squeeze(0).permute(1, 2, 0).cpu().numpy()
    img_arr = ((x_clamped + 1.0) * 127.5).astype(np.uint8)

    img = Image.fromarray(img_arr)

    # Explicitly enforce clean, retro-chunky grid pixelization scaling
    img_pixelated = img.resize((32, 32), Image.Resampling.NEAREST)
    img_pixelated = img_pixelated.resize((256, 256), Image.Resampling.NEAREST)

    img_pixelated.save(output_path)
    print(f"🎉 Success! Generated pixelated retro image saved to: {output_path}")

if __name__ == "__main__":
    prompt_str = "ماريو بكسل" # Or "mario sprite" / "فارس بكسل" / "pixel knight"
    if len(sys.argv) > 1:
        prompt_str = " ".join(sys.argv[1:])

    sample_images(prompt_str)
