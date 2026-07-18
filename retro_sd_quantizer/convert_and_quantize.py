import os
import sys
import subprocess

def clone_and_build_sd_cpp():
    print("=== Step 1: Clonging and compiling stable-diffusion.cpp backend ===")
    repo_dir = "stable-diffusion.cpp"

    if os.path.exists(repo_dir):
        print(f"Directory '{repo_dir}' already exists. Skipping cloning.")
    else:
        print("Cloning leejet/stable-diffusion.cpp...")
        try:
            subprocess.run(["git", "clone", "--recursive", "https://github.com/leejet/stable-diffusion.cpp.git"], check=True)
            print("Cloned successfully!")
        except Exception as e:
            print(f"❌ Error cloning stable-diffusion.cpp: {e}", file=sys.stderr)
            sys.exit(1)

    print("Compiling stable-diffusion.cpp (standard CPU mode, extremely lightweight)...")
    build_dir = os.path.join(repo_dir, "build")

    # Check if sd-cli or bin/sd-cli already exists
    bin_path = os.path.join(repo_dir, "bin", "sd-cli")
    if os.path.exists(bin_path):
        print("Binary 'sd-cli' is already built. Skipping compile.")
        return

    os.makedirs(build_dir, exist_ok=True)
    try:
        # Run CMake and make inside stable-diffusion.cpp
        subprocess.run(["cmake", "-S", repo_dir, "-B", build_dir], check=True)
        subprocess.run(["cmake", "--build", build_dir, "--config", "Release"], check=True)
        print("🎉 Successfully compiled stable-diffusion.cpp!")
    except Exception as e:
        print(f"❌ Error compiling stable-diffusion.cpp: {e}", file=sys.stderr)
        sys.exit(1)

def run_gguf_conversion():
    print("\n=== Step 2: Converting .safetensors to Quantized GGUF ===")
    input_checkpoint = "bk-sdm-tiny.safetensors"

    # Find the sd-cli binary
    # Usually CMake places the built binaries in stable-diffusion.cpp/build/bin/
    possible_paths = [
        "stable-diffusion.cpp/build/bin/sd-cli",
        "stable-diffusion.cpp/bin/sd-cli",
        "stable-diffusion.cpp/build/Release/sd-cli"
    ]

    sd_cli_path = None
    for p in possible_paths:
        if os.path.exists(p):
            sd_cli_path = p
            break

    if not sd_cli_path:
        print("❌ Could not find compiled 'sd-cli' binary. Please make sure stable-diffusion.cpp compiled successfully.", file=sys.stderr)
        sys.exit(1)

    if not os.path.exists(input_checkpoint):
        print(f"❌ Error: Required checkpoint '{input_checkpoint}' does not exist.", file=sys.stderr)
        print("Please run 'python3 download_and_convert.py' first.", file=sys.stderr)
        sys.exit(1)

    # Quantization types: q4_0 gives ~150MB-200MB, q8_0 gives ~300MB-350MB. Both under 500MB!
    for qtype in ["q4_0", "q8_0"]:
        output_gguf = f"bk-sdm-tiny_{qtype}.gguf"

        if os.path.exists(output_gguf):
            print(f"GGUF model '{output_gguf}' already exists. Skipping.")
            continue

        print(f"Converting and quantizing to '{qtype}' format...")
        # Convert using the sd-cli conversion sub-tool:
        # ./bin/sd-cli -M convert -m <safetensors> -o <gguf> --type <qtype>
        cmd = [
            sd_cli_path,
            "-M", "convert",
            "-m", input_checkpoint,
            "-o", output_gguf,
            "-v",
            "--type", qtype
        ]

        print(f"Executing: {' '.join(cmd)}")
        try:
            subprocess.run(cmd, check=True)
            print(f"🎉 Success! Generated GGUF model: {output_gguf}")
            print(f"Size: {os.path.getsize(output_gguf) / (1024*1024):.2f} MB")
        except Exception as e:
            print(f"❌ Error during GGUF quantization of {qtype}: {e}", file=sys.stderr)
            sys.exit(1)

def main():
    print("=================================================================")
    print("   Stable Diffusion GGUF Conversion & Quantization Tool")
    print("=================================================================")
    clone_and_build_sd_cpp()
    run_gguf_conversion()
    print("\n🎉 Done! Ready GGUF models are stored in the current directory.")

if __name__ == "__main__":
    main()
