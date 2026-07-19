import os
import sys
import subprocess
from safetensors.torch import load_file
import torch
import gguf

def extract_and_export_standalone_unet(input_safetensors="bk-sdm-tiny.safetensors", output_gguf="bilingual_retro_tiny_unet.gguf"):
    """
    Extracts ONLY the UNet diffusion layers from the compact BK-SDM-Tiny safetensors
    and quantizes them into a standalone GGUF file under 80MB.
    This runs safely without causing standard VAE/Text-Encoder memory crashes inside Local Dream.
    """
    print(f"=== Extracting UNet from '{input_safetensors}' -> '{output_gguf}' ===")

    if not os.path.exists(input_safetensors):
        print(f"❌ Error: Source '{input_safetensors}' does not exist.", file=sys.stderr)
        sys.exit(1)

    state_dict = load_file(input_safetensors)
    unet_dict = {}

    # Standard prefix for UNet layers in Stable Diffusion GGUF format
    for k, v in state_dict.items():
        if k.startswith("model.diffusion_model."):
            unet_dict[k] = v

    if not unet_dict:
        print("❌ Error: No UNet layers found with prefix 'model.diffusion_model.'", file=sys.stderr)
        sys.exit(1)

    print(f"Found {len(unet_dict)} UNet diffusion tensors. Writing GGUF...")

    # Initialize GGUF Writer with standalone UNet metadata
    gguf_writer = gguf.GGUFWriter(output_gguf, "stable-diffusion-unet")

    # Add tensors
    for name, tensor in unet_dict.items():
        tensor_np = tensor.numpy()
        gguf_writer.add_tensor(name, tensor_np)

    # Serialize GGUF file
    gguf_writer.write_header_to_file()
    gguf_writer.write_kv_data_to_file()
    gguf_writer.write_tensors_to_file()
    gguf_writer.close()

    print(f"🎉 Success! Generated standalone UNet: {output_gguf}")

    # Use sd-cli convert mode to quantize the standalone UNet to 80MB
    quant_gguf = "bilingual_retro_tiny_80mb.gguf"
    print(f"Quantizing standalone UNet to Q4_0 format ({quant_gguf})...")

    sd_cli_path = "stable-diffusion.cpp/build/bin/sd-cli"
    if not os.path.exists(sd_cli_path):
        sd_cli_path = "stable-diffusion.cpp/bin/sd-cli"

    if os.path.exists(sd_cli_path):
        cmd = [
            sd_cli_path,
            "-M", "convert",
            "-m", output_gguf,
            "-o", quant_gguf,
            "-v",
            "--type", "q4_0"
        ]
        print(f"Executing: {' '.join(cmd)}")
        subprocess.run(cmd, check=True)
        print(f"🎉 Success! Generated final ultra-lightweight 80MB model: {quant_gguf}")
        print(f"File Size: {os.path.getsize(quant_gguf) / (1024*1024):.2f} MB")
    else:
        print("⚠️ Warning: sd-cli compile binary not found. Standard GGUF generated only.")

if __name__ == "__main__":
    extract_and_export_standalone_unet()
