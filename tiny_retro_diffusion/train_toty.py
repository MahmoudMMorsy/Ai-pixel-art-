import os
import torch
import torch.nn as nn
from torch.utils.data import Dataset, DataLoader
from PIL import Image
import numpy as np
from tqdm import tqdm

from toty_model import TotyRetroDiffusionModel
from train import LinearNoiseSchedule

class TotyImageDataset(Dataset):
    """
    Slices the user's uploaded 512x512 test.png into 64 unique 64x64 tiles,
    forming a rich dataset to train/fine-tune the brand-new Toty model from scratch.
    """
    def __init__(self, image_path="test.png", tile_size=64):
        self.tile_size = tile_size
        self.tiles = []
        self.prompts = []

        if not os.path.exists(image_path):
            raise FileNotFoundError(f"Source user image '{image_path}' not found at the root!")

        img = Image.open(image_path).convert("RGB")
        w, h = img.size
        print(f"📊 Slicing user image '{image_path}' of size {w}x{h} for the new Toty model...")

        cols = w // tile_size
        rows = h // tile_size

        for r in range(rows):
            for c in range(cols):
                box = (c * tile_size, r * tile_size, (c + 1) * tile_size, (r + 1) * tile_size)
                tile = img.crop(box)
                self.tiles.append(tile)

                # Bilingual concept associations
                if (r + c) % 2 == 0:
                    self.prompts.append("فارس الأسطورة ريترو")
                else:
                    self.prompts.append("fantasy pixel-art knight standing in front of a majestic castle")

        print(f"🎉 Successfully sliced into {len(self.tiles)} tiles for scratch training!")

    def __len__(self):
        return len(self.tiles)

    def __getitem__(self, idx):
        tile = self.tiles[idx]
        prompt = self.prompts[idx]

        # Convert to float32 NumPy array [-1.0, 1.0]
        img_arr = np.array(tile).astype(np.float32) / 127.5 - 1.0
        # Permute to PyTorch format [C, H, W]
        img_tensor = torch.from_numpy(img_arr).permute(2, 0, 1)

        return img_tensor, prompt

def train_toty_model(epochs=15, batch_size=8, lr=1e-3, save_path="toty_raw.pth"):
    print("================================================================")
    print("   Training Brand-New Toty Image Generation Model from Scratch")
    print("================================================================")

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Device being used: {device}")

    # Load custom dataset
    dataset = TotyImageDataset(image_path="test.png", tile_size=64)
    dataloader = DataLoader(dataset, batch_size=batch_size, shuffle=True)

    # Initialize Toty model from scratch
    model = TotyRetroDiffusionModel(in_channels=3, n_feat=64, embed_dim=128).to(device)

    # Linear noise schedule for diffusion
    schedule = LinearNoiseSchedule(timesteps=300)

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

    print(f"\nSaving brand-new scratch-trained weights to: {save_path}")
    torch.save(model.state_dict(), save_path)
    print("🎉 New model scratch training completed successfully!")

if __name__ == "__main__":
    train_toty_model(epochs=15, batch_size=8, lr=1e-3)
