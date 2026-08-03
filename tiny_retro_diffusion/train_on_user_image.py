import os
import sys
import torch
import torch.nn as nn
from torch.utils.data import Dataset, DataLoader
from PIL import Image
import numpy as np
from tqdm import tqdm

from model import ContextRetroUNet
from train import LinearNoiseSchedule

class UserImageTileDataset(Dataset):
    """
    Slices a user base image (downscaled/cropped dynamically to 512x512) into 64 unique
    64x64 tiles, forming a rich custom dataset to train/fine-tune the
    ContextRetroUNet model on the user's uploaded visual patterns.
    """
    def __init__(self, image_path="test.png", tile_size=64, prompt_ar=None, prompt_en=None):
        self.tile_size = tile_size
        self.tiles = []
        self.prompts = []

        if not os.path.exists(image_path):
            raise FileNotFoundError(f"Source user image '{image_path}' not found!")

        # Load and handle image opening smoothly
        img = Image.open(image_path).convert("RGB")

        # Automatically center crop & downscale to exactly 512x512
        w, h = img.size
        min_dim = min(w, h)
        left = (w - min_dim) // 2
        top = (h - min_dim) // 2
        right = left + min_dim
        bottom = top + min_dim

        img_cropped = img.crop((left, top, right, bottom))
        img_resized = img_cropped.resize((512, 512), Image.Resampling.LANCZOS)

        print(f"📊 Auto-processed user base image '{image_path}' (Original: {w}x{h}) resized & center-cropped to 512x512.")

        # Default prompts if not provided
        ar_p = prompt_ar if prompt_ar else "فارس الأسطورة ريترو"
        en_p = prompt_en if prompt_en else "fantasy pixel-art knight standing in front of a majestic castle"

        # Slice the image into non-overlapping tiles of size tile_size x tile_size
        cols = 512 // tile_size
        rows = 512 // tile_size

        for r in range(rows):
            for c in range(cols):
                box = (c * tile_size, r * tile_size, (c + 1) * tile_size, (r + 1) * tile_size)
                tile = img_resized.crop(box)
                self.tiles.append(tile)

                # Alternate bilingual prompts for each tile to embed deep semantic associations
                if (r + c) % 2 == 0:
                    self.prompts.append(ar_p)
                else:
                    self.prompts.append(en_p)

        print(f"🎉 Successfully sliced image into {len(self.tiles)} high-quality {tile_size}x{tile_size} training samples!")

    def __len__(self):
        return len(self.tiles)

    def __getitem__(self, idx):
        tile = self.tiles[idx]
        prompt = self.prompts[idx]

        # Convert tile to float32 NumPy array and scale to range [-1.0, 1.0]
        img_arr = np.array(tile).astype(np.float32) / 127.5 - 1.0
        # Permute from [H, W, C] to [C, H, W] for PyTorch Convolution compatibility
        img_tensor = torch.from_numpy(img_arr).permute(2, 0, 1)

        return img_tensor, prompt

def train_on_user_image(image_path="test.png", epochs=15, batch_size=8, lr=1e-3, save_path="bilingual_retro_tiny.pth", prompt_ar=None, prompt_en=None):
    print("=======================================================================")
    print("   Training/Fine-tuning ContextRetroUNet Model on Custom User Image")
    print("=======================================================================")

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Device being used: {device}")

    # Initialize the custom sliced tile dataset
    dataset = UserImageTileDataset(image_path=image_path, tile_size=64, prompt_ar=prompt_ar, prompt_en=prompt_en)
    dataloader = DataLoader(dataset, batch_size=batch_size, shuffle=True)

    # Initialize ContextRetroUNet with 3 input channels (RGB), 64 features, 128 embedding dim
    model = ContextRetroUNet(in_channels=3, n_feat=64, embed_dim=128).to(device)

    # Load existing pre-trained weights if available to fine-tune instead of training from scratch
    if os.path.exists(save_path):
        print(f"Loading existing model weights from {save_path} to fine-tune...")
        try:
            model.load_state_dict(torch.load(save_path, map_location=device))
            print("Existing model weights loaded successfully!")
        except Exception as e:
            print(f"⚠️ Could not load existing model weights ({e}). Training model from scratch...")

    # Linear noise schedule for DDPM denoising diffusion
    schedule = LinearNoiseSchedule(timesteps=300)

    # AdamW optimizer with weight decay
    optimizer = torch.optim.AdamW(model.parameters(), lr=lr)
    loss_fn = nn.MSELoss()

    model.train()
    for epoch in range(epochs):
        print(f"\n--- Epoch {epoch+1}/{epochs} ---")
        epoch_loss = 0.0

        pbar = tqdm(dataloader)
        for images, prompts in pbar:
            images = images.to(device)
            B = images.shape[0]

            # Generate random timesteps for each batch sample
            t_steps = torch.randint(low=1, high=300, size=(B,), device=device)
            # Diffuse the image with linear schedule noise
            noisy_imgs, noise = schedule.noise_images(images, t_steps)

            # Normalize t_steps to range [0.0, 1.0] for model input
            t_input = (t_steps.float() / 300.0)[:, None]

            # Predict the noise injected at the given timestep
            predicted_noise = model(noisy_imgs, t_input, prompts)

            # Calculate mean squared error loss between target and prediction
            loss = loss_fn(predicted_noise, noise)

            # Backpropagation and weights update
            optimizer.zero_grad()
            loss.backward()
            optimizer.step()

            epoch_loss += loss.item()
            pbar.set_description(f"Loss: {loss.item():.4f}")

        avg_loss = epoch_loss / len(dataloader)
        print(f"Average Epoch {epoch+1} Training Loss: {avg_loss:.5f}")

    # Save trained checkpoint to root directory for generate.py
    print(f"\nSaving final trained model parameters to: {save_path}")
    torch.save(model.state_dict(), save_path)
    print("🎉 Training on user image completed successfully! Weights saved.")

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", type=str, default="test.png", help="Path to user input image")
    parser.add_argument("--epochs", type=int, default=15, help="Number of training epochs")
    parser.add_argument("--prompt_ar", type=str, default=None, help="Custom Arabic prompt")
    parser.add_argument("--prompt_en", type=str, default=None, help="Custom English prompt")
    args = parser.parse_args()

    train_on_user_image(
        image_path=args.image,
        epochs=args.epochs,
        prompt_ar=args.prompt_ar,
        prompt_en=args.prompt_en
    )
