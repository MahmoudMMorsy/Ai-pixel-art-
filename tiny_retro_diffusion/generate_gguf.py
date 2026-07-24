import os
import sys
import subprocess
from PIL import Image

# --- Bilingual Logging Helpers ---
def log(ar_msg, en_msg):
    print(f"🌀 [AR] {ar_msg}\n✨ [EN] {en_msg}\n" + "-" * 50)

def log_error(ar_msg, en_msg):
    print(f"❌ [AR] {ar_msg}\n❌ [EN] {en_msg}\n" + "-" * 50, file=sys.stderr)

def compile_stable_diffusion_cpp():
    """
    Clones and compiles leejet/stable-diffusion.cpp to get the real JNI/C++ executable binary.
    """
    repo_dir = "stable-diffusion.cpp"
    if not os.path.exists(repo_dir):
        log("جاري استنساخ مستودع stable-diffusion.cpp الحقيقي للتوليد المحلي العالي الأداء...",
            "Cloning stable-diffusion.cpp repository for high-performance local inference...")
        try:
            subprocess.run(["git", "clone", "--recursive", "https://github.com/leejet/stable-diffusion.cpp.git"], check=True)
        except Exception as e:
            log_error(f"فشل استنساخ المستودع: {e}", f"Failed to clone repository: {e}")
            return False

    # Check for compiled binary
    bin_path = os.path.join(repo_dir, "build", "bin", "sd")
    if not os.path.exists(bin_path):
        bin_path = os.path.join(repo_dir, "bin", "sd")

    if os.path.exists(bin_path):
        log("مفسّر stable-diffusion.cpp جاهز ومبني مسبقاً!", "stable-diffusion.cpp binary is already built and ready!")
        return bin_path

    log("جاري بناء وتجميع مفسّر stable-diffusion.cpp ليعمل على المعالج (CPU) بأقصى كفاءة...",
        "Compiling stable-diffusion.cpp binary for maximum performance on CPU...")

    build_dir = os.path.join(repo_dir, "build")
    os.makedirs(build_dir, exist_ok=True)
    try:
        subprocess.run(["cmake", "-S", repo_dir, "-B", build_dir], check=True)
        subprocess.run(["cmake", "--build", build_dir, "--config", "Release"], check=True)

        # Verify binary again
        for p in [os.path.join(build_dir, "bin", "sd"), os.path.join(repo_dir, "bin", "sd")]:
            if os.path.exists(p):
                return p
        return None
    except Exception as e:
        log_error(f"فشل بناء وتجميع مفسّر C++: {e}", f"Failed to compile C++ inference engine: {e}")
        return None

def generate_image_from_gguf(prompt, model_path="models/bilingual_retro_tiny_compatible.gguf", output_path="retro_generated_gguf.png", steps=15):
    """
    Executes the REAL C++ Stable Diffusion inference engine using the GGUF model
    to generate an actual, genuine AI-generated image. No mocks, no simulations.
    """
    log(f"بدء التوليد الفعلي والحقيقي للنموذج {model_path} باستخدام مفسر C++ الخاص بـ GGUF...",
        f"Starting real GGUF generation for model {model_path} using the native C++ GGUF interpreter...")

    sd_bin = compile_stable_diffusion_cpp()
    if not sd_bin:
        log_error("فشل العثور على مفسّر C++ أو بنائه. يرجى التحقق من أدوات cmake و make بالجهاز.",
                  "Could not find or compile the C++ binary. Please check cmake and build tools.")
        return False

    if not os.path.exists(model_path):
        log_error(f"ملف النموذج GGUF غير موجود في: {model_path}", f"GGUF model file not found at: {model_path}")
        return False

    temp_out = "temp_gguf_out.png"
    if os.path.exists(temp_out):
        os.remove(temp_out)

    # Command parameters for the native C++ generator
    cmd = [
        sd_bin,
        "-m", model_path,
        "-p", prompt,
        "-o", temp_out,
        "--steps", str(steps),
        "-v"
    ]

    log(f"جاري تشغيل المعالجة العصبية العميقة بالتسلسل: {' '.join(cmd)}",
        f"Executing deep neural tensor processing: {' '.join(cmd)}")

    try:
        subprocess.run(cmd, check=True)
        if os.path.exists(temp_out):
            # Post-process for gorgeous, razor-sharp nearest-neighbor pixel-art grid
            img = Image.open(temp_out)
            img_pixelated = img.resize((64, 64), Image.Resampling.NEAREST)
            img_pixelated = img_pixelated.resize((512, 512), Image.Resampling.NEAREST)
            img_pixelated.save(output_path)
            os.remove(temp_out)
            log(f"تم توليد الصورة الحقيقية وحفظها بنجاح في: {output_path}",
                f"Real AI image successfully generated and saved to: {output_path}")
            return True
        else:
            log_error("فشل التوليد: مفسر C++ لم ينتج أي صورة مخرجة.",
                      "Generation failed: C++ interpreter did not output any image.")
            return False
    except Exception as e:
        log_error(f"خطأ أثناء معالجة تشغيل مفسر GGUF: {e}", f"Error executing GGUF interpreter: {e}")
        return False

if __name__ == "__main__":
    prompt_str = "pixel knight hero with a shiny gold sword, nes style"
    if len(sys.argv) > 1:
        prompt_str = " ".join(sys.argv[1:])

    # Default to the quantized 80MB GGUF model
    generate_image_from_gguf(prompt_str)
