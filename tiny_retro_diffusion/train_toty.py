import os
import torch
import torch.nn as nn
from torch.utils.data import Dataset, DataLoader
from PIL import Image
import numpy as np
from tqdm import tqdm
import json

from toty_model import TotyRetroDiffusionModel
from train import LinearNoiseSchedule
from scrape_dataset import run_scraper
from auto_tag_dataset import auto_tag_images

class TotyImageDataset(Dataset):
    """
    Loads real scraped and auto-tagged NES character sprites and background images
    from 'dataset/metadata_tagged.json' representing real visual patterns of characters like Mario, Luigi,
    gold coins, retro knights, castle blocks, and treasure chests.
    """
    def __init__(self, metadata_path="dataset/metadata_tagged.json", img_size=64):
        self.img_size = img_size
        self.real_images = []

        # Run scraping and auto-tagging of NES characters and backgrounds if not already present
        if not os.path.exists(metadata_path):
            print("🚀 Scraped metadata not found. Triggering NES characters and backgrounds scraper/tagger...")
            run_scraper(output_dir="dataset/raw")
            auto_tag_images(input_dir="dataset/raw", output_dir="dataset")

        if os.path.exists(metadata_path):
            try:
                with open(metadata_path, "r", encoding="utf-8") as f:
                    metadata = json.load(f)

                for img_name, info in metadata.items():
                    path = info["filepath"]
                    if os.path.exists(path):
                        self.real_images.append({
                            "path": path,
                            "prompt_en": info["prompt_en"],
                            "prompt_ar": info["prompt_ar"]
                        })
                print(f"📊 Loaded {len(self.real_images)} real auto-tagged NES character/background images for training!")
            except Exception as e:
                print(f"⚠️ Warning: Failed to parse metadata file {metadata_path}: {e}")

        # Fallback if empty
        if not self.real_images:
            raise FileNotFoundError("Could not find or synthesize any NES character/background image dataset!")

    def __len__(self):
        # We can artificially repeat the dataset to have a stable epoch size
        return max(len(self.real_images), 120)

    def __getitem__(self, idx):
        item = self.real_images[idx % len(self.real_images)]
        prompt = item["prompt_ar"] if idx % 2 == 0 else item["prompt_en"]

        try:
            img = Image.open(item["path"]).convert("RGB")
            # Resize cleanly with nearest-neighbor to retain sharp NES details
            img = img.resize((self.img_size, self.img_size), Image.Resampling.NEAREST)

            img_arr = np.array(img).astype(np.float32) / 127.5 - 1.0
            img_tensor = torch.from_numpy(img_arr).permute(2, 0, 1) # [3, H, W]
            return img_tensor, prompt
        except Exception as e:
            # Fallback to random tensor if loading fails
            return torch.randn(3, self.img_size, self.img_size), "فارس الأسطورة ريترو"

def train_toty_model(epochs=15, batch_size=8, lr=1e-3, save_path="toty_raw.pth"):
    print("==================================================================")
    print("   Training Brand-New Toty Image Generation Model from Scratch")
    print("      Target: Real NES Game Characters and Backgrounds")
    print("==================================================================")

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Device being used: {device}")

    # Load real custom NES characters/backgrounds dataset
    dataset = TotyImageDataset(metadata_path="dataset/metadata_tagged.json", img_size=64)
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
    print("🎉 New model scratch training on NES characters completed successfully!")

if __name__ == "__main__":
    train_toty_model(epochs=15, batch_size=8, lr=1e-3)
