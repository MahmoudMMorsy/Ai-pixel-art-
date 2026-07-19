import torch
import numpy as np
import gguf

def convert_pth_to_gguf(pth_path="bilingual_retro_tiny.pth", gguf_path="bilingual_retro_tiny.gguf"):
    """
    Saves and serializes the weights of the trained Bilingual Tiny Retro Diffusion model
    directly into GGUF format for Local Dream compatibility.
    """
    print(f"=== Converting '{pth_path}' to GGUF format ===")

    # Load raw PyTorch state dict
    state_dict = torch.load(pth_path, map_location="cpu")

    # Initialize GGUF Writer
    gguf_writer = gguf.GGUFWriter(gguf_path, "tiny-retro-diffusion")

    # Add tensors
    for tensor_name, tensor in state_dict.items():
        # Convert PyTorch tensor to numpy
        tensor_np = tensor.numpy()

        # Clean and serialize tensor name
        cleaned_name = tensor_name.replace("model.", "").replace("embedding.", "")

        # Add to writer
        gguf_writer.add_tensor(cleaned_name, tensor_np)
        print(f"Serialized tensor: {cleaned_name} | Shape: {tensor_np.shape} | Dtype: {tensor_np.dtype}")

    # Write GGUF file to disk
    gguf_writer.write_header_to_file()
    gguf_writer.write_kv_data_to_file()
    gguf_writer.write_tensors_to_file()
    gguf_writer.close()
    print(f"🎉 Success! Generated custom GGUF model: {gguf_path}")

if __name__ == "__main__":
    convert_pth_to_gguf()
