import os
import torch
import torch.nn as nn
from torch.utils.data import Dataset, DataLoader
from PIL import Image, ImageDraw
import numpy as np
from tqdm import tqdm

from model import ContextRetroUNet

class RetroPixelDataset(Dataset):
    """
    Synthesizes and handles a rich dataset of clean, high-quality classic
    NES, Game Boy, and Famicom style pixel-art sprites (like Mario, knight swords,
    monsters, coins, and platforms) paired with bilingual descriptions (Arabic & English).
    This simulates training on actual retro assets.
    """
    def __init__(self, size=500, img_size=64):
        self.size = size
        self.img_size = img_size

        # Retro concepts, shapes, colors, and prompts
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
        # Choose a class deterministically based on index
        cls_idx = idx % len(self.classes)
        cls_info = self.classes[cls_idx]

        # Pick a random bilingual prompt from the variations
        prompt = cls_info["prompts"][idx % len(cls_info["prompts"])]

        # Synthesize a beautiful pixel-perfect retro canvas
        # 1. Start with solid low-res retro background
        img = Image.new("RGB", (self.img_size, self.img_size), color=(12, 12, 24))
        draw = ImageDraw.Draw(img)

        # Scale factor representing retro block grids (e.g., 4x4 scale pixels for chunky NES feel)
        block = 4
        colors = cls_info["color_scheme"]

        # Draw clean retro shapes aligned to chunky grid boundaries
        if cls_info["shape"] == "plumber":
            # Head (Red hat, skin face)
            draw.rectangle([20, 12, 44, 20], fill=colors[0]) # Red hat
            draw.rectangle([24, 20, 40, 28], fill=colors[2]) # Skin
            # Torso (Blue overalls, red sleeves)
            draw.rectangle([16, 28, 48, 44], fill=colors[1]) # Blue
            draw.rectangle([12, 28, 16, 40], fill=colors[0]) # Red left arm
            draw.rectangle([48, 28, 52, 40], fill=colors[0]) # Red right arm
            # Shoes
            draw.rectangle([16, 44, 28, 48], fill=(100, 50, 0)) # Left shoe
            draw.rectangle([36, 44, 48, 48], fill=(100, 50, 0)) # Right shoe
        elif cls_info["shape"] == "shield":
            # Helmet and body armor (Silver)
            draw.rectangle([20, 12, 44, 44], fill=colors[0])
            # Gold sword/shield
            draw.rectangle([12, 20, 20, 40], fill=colors[2]) # Sword
            draw.rectangle([40, 24, 52, 36], fill=colors[2]) # Gold Shield
            # Eyes slit
            draw.rectangle([28, 16, 36, 20], fill=(0, 0, 0))
        elif cls_info["shape"] == "alien":
            # Alien head and feelers
            draw.rectangle([16, 16, 48, 40], fill=colors[0])
            draw.rectangle([12, 32, 16, 44], fill=colors[1])
            draw.rectangle([48, 32, 52, 44], fill=colors[1])
            # Big creepy retro eyes
            draw.rectangle([20, 20, 28, 28], fill=colors[2])
            draw.rectangle([36, 20, 44, 28], fill=colors[2])
        elif cls_info["shape"] == "circle":
            # Smooth concentric gold ring
            draw.ellipse([16, 16, 48, 48], fill=colors[1])
            draw.ellipse([20, 20, 44, 44], fill=colors[0])
            draw.ellipse([28, 28, 36, 36], fill=colors[2]) # Center highlight
        else: # castle
            # Brick towers and ramparts
            draw.rectangle([8, 24, 24, 52], fill=colors[0]) # Left tower
            draw.rectangle([40, 24, 56, 52], fill=colors[0]) # Right tower
            draw.rectangle([24, 32, 40, 52], fill=colors[1]) # Center gate
            draw.rectangle([28, 40, 36, 52], fill=(0, 0, 0)) # Archway gate opening
            # Triangular roofs
            draw.polygon([(8, 24), (16, 12), (24, 24)], fill=(180, 0, 0))
            draw.polygon([(40, 24), (48, 12), (56, 24)], fill=(180, 0, 0))

        # Pixelate the drawing explicitly by resizing down and scaling back
        img = img.resize((32, 32), Image.Resampling.NEAREST)
        img = img.resize((self.img_size, self.img_size), Image.Resampling.NEAREST)

        # Convert to float PyTorch tensor [-1, 1]
        img_arr = np.array(img).astype(np.float32) / 127.5 - 1.0
        img_tensor = torch.from_numpy(img_arr).permute(2, 0, 1) # [3, H, W]

        return img_tensor, prompt

class LinearNoiseSchedule:
    """Standard Denoising Diffusion scheduler with linear noise variance schedule"""
    def __init__(self, timesteps=300, beta_start=1e-4, beta_end=0.02):
        self.timesteps = timesteps
        self.beta = torch.linspace(beta_start, beta_end, timesteps)
        self.alpha = 1.0 - self.beta
        self.alpha_hat = torch.cumprod(self.alpha, dim=0)

    def noise_images(self, x, t):
        """Adds noise to images based on cumulative alphas (hat) for timestep t"""
        # x: [B, 3, 64, 64]
        # t: [B]
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

    # Initialize dataset and dataloader
    dataset = RetroPixelDataset(size=300, img_size=64)
    dataloader = DataLoader(dataset, batch_size=batch_size, shuffle=True)

    # Instantiate custom lightweight UNet and Noise Schedule
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

            # Sample random timesteps
            t_steps = torch.randint(low=1, high=300, size=(B,), device=device)

            # Corrupt images with noise
            noisy_imgs, noise = schedule.noise_images(images, t_steps)

            # Predict noise using bilingual UNet
            # Format timesteps as relative scalars [0.0, 1.0]
            t_input = (t_steps.float() / 300.0)[:, None]
            predicted_noise = model(noisy_imgs, t_input, prompts)

            # Compute loss and perform backward pass
            loss = loss_fn(predicted_noise, noise)

            optimizer.zero_grad()
            loss.backward()
            optimizer.step()

            epoch_loss += loss.item()
            pbar.set_description(f"Loss: {loss.item():.4f}")

        avg_loss = epoch_loss / len(dataloader)
        print(f"Average Epoch {epoch+1} Loss: {avg_loss:.5f}")

    # Save the trained model parameters
    print(f"\nSaving final trained model parameters to: {save_path}")
    torch.save(model.state_dict(), save_path)
    print("🎉 Training pipeline complete! Model is ready for standalone retro inference.")

if __name__ == "__main__":
    train_model(epochs=3, batch_size=16, lr=1e-3)
