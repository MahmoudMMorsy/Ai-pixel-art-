import os
import sys
import torch
from PIL import Image
import numpy as np

# Let's import Stable Diffusion pipeline directly
from diffusers import StableDiffusionPipeline

def generate_professional_pixel_art(prompt, model_id="nota-ai/bk-sdm-tiny", output_path="retro_generated_professional.png"):
    """
    Downloads and runs the professional nota-ai/bk-sdm-tiny Stable Diffusion v1.5 distilled model
    directly to generate an authentic, stunning AI image from your prompt,
    then processes it dynamically using nearest-neighbor scaling to output a flawless,
    high-contrast, crisp 512x512 retro pixel art masterpiece.
    """
    print(f"=== Running Professional Generative Inference for: '{prompt}' ===")
    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"Executing deep neural tensor processing on: {device}")

    try:
        # Load the distilled Stable Diffusion checkpoint
        # Using CPU-friendly float32 or GPU-friendly float16 depending on hardware
        pipe = StableDiffusionPipeline.from_pretrained(
            model_id,
            torch_dtype=torch.float32,
            safety_checker=None
        ).to(device)

        # Ensure high-quality alignment
        print("Running prompt-guided latent diffusion denoising steps...")
        # nota-ai/bk-sdm-tiny is distilled, so it works beautifully in just 10-15 steps!
        image = pipe(
            prompt + ", pixel art style, high contrast retro game asset, vibrant color palette, nes gameboy faimcom style",
            num_inference_steps=12,
            guidance_scale=6.5
        ).images[0]

        # Enforce crispy, razor-sharp pixel art grid representation via nearest-neighbor resampling
        print("Applying nearest-neighbor pixel grids down-and-up scaling...")
        img_pixelated = image.resize((64, 64), Image.Resampling.NEAREST)
        img_pixelated = img_pixelated.resize((512, 512), Image.Resampling.NEAREST)

        # Save and verify
        img_pixelated.save(output_path)
        print(f"🎉 Success! Professional generated pixel art saved to: {output_path}")
        return True
    except Exception as e:
        print(f"❌ Error during professional generation: {e}", file=sys.stderr)
        return False

if __name__ == "__main__":
    prompt_str = "pixel knight hero with a glowing gold sword"
    if len(sys.argv) > 1:
        prompt_str = " ".join(sys.argv[1:])

    generate_professional_pixel_art(prompt_str)
