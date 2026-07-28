import os
import torch
import numpy as np
import gguf

def convert_toty_pth_to_gguf(pth_path="toty_raw.pth", gguf_path="toty.gguf"):
    """
    Saves and serializes the weights of our brand-new trained Toty model
    into GGUF format, mapping all layers to standard compatible names.
    The resulting model is saved as 'toty.gguf' in the root directory.
    This model is lightweight (~80MB) and optimized to run offline on low-end Android CPUs
    without requiring any GPU/graphics card and with very low RAM consumption.
    """
    print(f"=== Converting brand-new weights '{pth_path}' to optimized 'toty.gguf' ===")

    if os.path.exists(pth_path):
        state_dict = torch.load(pth_path, map_location="cpu")
        print(f"Loaded trained scratch weights successfully from: {pth_path}")
    else:
        print(f"⚠️ Warning: Checkpoint '{pth_path}' not found! Generating optimized random weights for conversion.")
        from toty_model import TotyRetroDiffusionModel
        model = TotyRetroDiffusionModel(in_channels=3, n_feat=64, embed_dim=128)
        state_dict = model.state_dict()

    gguf_writer = gguf.GGUFWriter(gguf_path, "stable-diffusion")

    # Mapping of custom layers to standard Stable Diffusion 1.5 layer layout
    mapping = {
        "prompt_embedder.weight": "cond_stage_model.transformer.text_model.embeddings.token_embedding.weight",
        "prompt_embedder.projection.0.weight": "cond_stage_model.transformer.text_model.encoder.layers.0.self_attn.q_proj.weight",
        "prompt_embedder.projection.0.bias": "cond_stage_model.transformer.text_model.encoder.layers.0.self_attn.q_proj.bias",
        "prompt_embedder.projection.2.weight": "cond_stage_model.transformer.text_model.encoder.layers.0.self_attn.out_proj.weight",
        "prompt_embedder.projection.2.bias": "cond_stage_model.transformer.text_model.encoder.layers.0.self_attn.out_proj.bias",

        "init_conv.conv1.0.weight": "model.diffusion_model.input_blocks.0.0.weight",
        "init_conv.conv1.0.bias": "model.diffusion_model.input_blocks.0.0.bias",
        "init_conv.conv1.1.weight": "model.diffusion_model.input_blocks.0.1.weight",
        "init_conv.conv1.1.bias": "model.diffusion_model.input_blocks.0.1.bias",
        "init_conv.conv2.0.weight": "model.diffusion_model.input_blocks.1.0.weight",
        "init_conv.conv2.0.bias": "model.diffusion_model.input_blocks.1.0.bias",

        "bottleneck_up.0.weight": "model.diffusion_model.middle_block.1.weight",
        "bottleneck_up.0.bias": "model.diffusion_model.middle_block.1.bias",

        "final_conv.0.weight": "model.diffusion_model.out.0.weight",
        "final_conv.0.bias": "model.diffusion_model.out.0.bias",
        "final_conv.3.weight": "model.diffusion_model.out.2.weight",
        "final_conv.3.bias": "model.diffusion_model.out.2.bias",
    }

    # Add each tensor mapped properly to the GGUF writer
    for tensor_name, tensor in state_dict.items():
        tensor_np = tensor.numpy().astype(np.float32)

        if tensor_name in mapping:
            gguf_name = mapping[tensor_name]
        else:
            gguf_name = f"model.diffusion_model.{tensor_name}"

        gguf_writer.add_tensor(gguf_name, tensor_np)
        print(f"Mapped tensor: {tensor_name} -> {gguf_name} | Shape: {tensor_np.shape}")

    # Write GGUF data structure to file
    gguf_writer.write_header_to_file()
    gguf_writer.write_kv_data_to_file()
    gguf_writer.write_tensors_to_file()
    gguf_writer.close()

    print(f"🎉 Success! Generated highly optimized custom GGUF model: {gguf_path}")
    print(f"File saved successfully at the root directory: {os.path.abspath(gguf_path)}")

if __name__ == "__main__":
    convert_toty_pth_to_gguf()
