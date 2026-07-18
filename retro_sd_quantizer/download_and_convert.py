import os
import sys
import torch
import urllib.request
from diffusers import StableDiffusionPipeline

def download_and_prepare_distilled_model():
    print("=== Step 1: Downloading distilled BK-SDM-Tiny model from Hugging Face ===")
    model_id = "nota-ai/bk-sdm-tiny"
    local_dir = "./bksdmtiny-raw"

    if os.path.exists(local_dir):
        print(f"Directory '{local_dir}' already exists. Skipping download.")
    else:
        print(f"Downloading model '{model_id}'...")
        try:
            # We use float16 to keep the downloaded weights extremely compact and fast to load.
            # However, since CPU-only setups might load fp16 slow, we download as float16 first.
            pipe = StableDiffusionPipeline.from_pretrained(
                model_id,
                torch_dtype=torch.float16,
                safety_checker=None
            )

            print("Ensuring tensor contiguity to prevent any conversion issues...")
            unet = pipe.unet
            for param in unet.parameters():
                param.data = param.data.contiguous()

            vae = pipe.vae
            for param in vae.parameters():
                param.data = param.data.contiguous()

            text_encoder = pipe.text_encoder
            for param in text_encoder.parameters():
                param.data = param.data.contiguous()

            print(f"Saving prepared model locally to: {local_dir}")
            pipe.save_pretrained(local_dir, safe_serialization=True)
            print("Prepared model saved successfully!")
        except Exception as e:
            print(f"❌ Error during model preparation: {e}", file=sys.stderr)
            sys.exit(1)

def fetch_conversion_script():
    print("\n=== Step 2: Fetching HF conversion script to single .safetensors file ===")
    script_url = "https://raw.githubusercontent.com/huggingface/diffusers/main/scripts/convert_diffusers_to_original_stable_diffusion.py"
    script_path = "convert_diffusers_to_original_stable_diffusion.py"

    if os.path.exists(script_path):
        print(f"Conversion script '{script_path}' already exists.")
    else:
        print(f"Downloading conversion script from: {script_url}")
        try:
            # Set User-Agent header to avoid potential HTTP 403 blocks
            req = urllib.request.Request(
                script_url,
                headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
            )
            with urllib.request.urlopen(req) as response:
                with open(script_path, 'wb') as f:
                    f.write(response.read())
            print("Script downloaded successfully!")
        except Exception as e:
            print(f"❌ Failed to download conversion script: {e}", file=sys.stderr)
            sys.exit(1)

def run_conversion():
    print("\n=== Step 3: Running conversion to unified .safetensors ===")
    input_model_path = "./bksdmtiny-raw"
    output_checkpoint = "bk-sdm-tiny.safetensors"

    if os.path.exists(output_checkpoint):
        print(f"Target checkpoint '{output_checkpoint}' already exists. Skipping conversion.")
        return

    cmd = (
        f"python3 convert_diffusers_to_original_stable_diffusion.py "
        f"--model_path {input_model_path} "
        f"--checkpoint_path {output_checkpoint} "
        f"--half --use_safetensors"
    )

    print(f"Executing: {cmd}")
    status = os.system(cmd)
    if status == 0:
        print(f"🎉 Success! Generated compact checkpoint: {output_checkpoint}")
    else:
        print(f"❌ Error: Conversion script exited with non-zero status: {status}", file=sys.stderr)
        sys.exit(1)

def main():
    print("=================================================================")
    print("   Distilled Stable Diffusion v1.5 Safetensors Generator")
    print("=================================================================")
    download_and_prepare_distilled_model()
    fetch_conversion_script()
    run_conversion()
    print("\n🎉 Done! The prepared 'bk-sdm-tiny.safetensors' is now ready for GGUF conversion.")

if __name__ == "__main__":
    main()
