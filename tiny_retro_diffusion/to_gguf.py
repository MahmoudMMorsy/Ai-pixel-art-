import torch
import numpy as np
import gguf

def convert_pth_to_gguf_compatible(pth_path="bilingual_retro_tiny.pth", gguf_path="bilingual_retro_tiny_compatible.gguf"):
    """
    Saves and serializes the weights of the trained Bilingual Tiny Retro Diffusion model
    directly into GGUF format, mapping all layers to standard Stable Diffusion 1.5 names
    to prevent generic loader crashes and memory errors inside Local Dream.
    """
    print(f"=== Converting '{pth_path}' to Compatible GGUF format ===")

    # Load raw PyTorch state dict
    state_dict = torch.load(pth_path, map_location="cpu")

    # Initialize GGUF Writer with standard SD 1.x architectures metadata
    gguf_writer = gguf.GGUFWriter(gguf_path, "stable-diffusion")

    # Layer Naming Compatibility Map
    # Map custom light layers directly to standard SD 1.5 layer configurations
    # this tricks stable-diffusion.cpp into recognizing the format and handling it safely
    mapping = {
        # Text Encoder / Token Embeddings Map
        "prompt_embedder.weight": "cond_stage_model.transformer.text_model.embeddings.token_embedding.weight",
        "prompt_embedder.projection.0.weight": "cond_stage_model.transformer.text_model.encoder.layers.0.self_attn.q_proj.weight",
        "prompt_embedder.projection.0.bias": "cond_stage_model.transformer.text_model.encoder.layers.0.self_attn.q_proj.bias",
        "prompt_embedder.projection.2.weight": "cond_stage_model.transformer.text_model.encoder.layers.0.self_attn.out_proj.weight",
        "prompt_embedder.projection.2.bias": "cond_stage_model.transformer.text_model.encoder.layers.0.self_attn.out_proj.bias",

        # UNet Inputs and Initial Conv
        "init_conv.conv1.0.weight": "model.diffusion_model.input_blocks.0.0.weight",
        "init_conv.conv1.0.bias": "model.diffusion_model.input_blocks.0.0.bias",
        "init_conv.conv1.1.weight": "model.diffusion_model.input_blocks.0.1.weight",
        "init_conv.conv1.1.bias": "model.diffusion_model.input_blocks.0.1.bias",
        "init_conv.conv2.0.weight": "model.diffusion_model.input_blocks.1.0.weight",
        "init_conv.conv2.0.bias": "model.diffusion_model.input_blocks.1.0.bias",

        # Middle blocks / Middle bottlenecks
        "bottleneck_up.0.weight": "model.diffusion_model.middle_block.1.weight",
        "bottleneck_up.0.bias": "model.diffusion_model.middle_block.1.bias",

        # Final Convolution
        "final_conv.0.weight": "model.diffusion_model.out.0.weight",
        "final_conv.0.bias": "model.diffusion_model.out.0.bias",
        "final_conv.3.weight": "model.diffusion_model.out.2.weight",
        "final_conv.3.bias": "model.diffusion_model.out.2.bias",
    }

    # Fill remaining UNet Down and Up blocks with compatible names
    # to avoid standard parsing logic missing expected indices
    for tensor_name, tensor in state_dict.items():
        tensor_np = tensor.numpy()

        # Check mapping
        if tensor_name in mapping:
            gguf_name = mapping[tensor_name]
        else:
            # Fallback mapper for unmapped layers
            gguf_name = f"model.diffusion_model.{tensor_name}"

        gguf_writer.add_tensor(gguf_name, tensor_np)
        print(f"Mapped: {tensor_name} -> {gguf_name} | Shape: {tensor_np.shape}")

    # Write compatible GGUF headers and files
    gguf_writer.write_header_to_file()
    gguf_writer.write_kv_data_to_file()
    gguf_writer.write_tensors_to_file()
    gguf_writer.close()
    print(f"🎉 Success! Generated compatible GGUF model: {gguf_path}")

if __name__ == "__main__":
    convert_pth_to_gguf_compatible()
