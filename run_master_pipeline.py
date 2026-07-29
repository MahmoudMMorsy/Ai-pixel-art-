import os
import sys
import subprocess
import urllib.request
import json
from PIL import Image

# Setup bilingual logging helpers
def log(ar, en):
    print(f"🌀 [AR] {ar}\n✨ [EN] {en}\n" + "-" * 60)

def log_error(ar, en):
    print(f"❌ [AR] {ar}\n❌ [EN] {en}\n" + "-" * 60, file=sys.stderr)

def main():
    log("بدء خط الإنتاج الشامل والموحد لتوليد وتكميم الصور للريترو بكسل آرت بالكامل...",
        "Starting the comprehensive unified pipeline for retro pixel-art image generation and quantization...")

    # Step 1: Scrape dataset / Use high-fidelity fallback generators
    log("الخطوة 1: جاري تشغيل سحب أو تخليق الأصول والرسومات...", "Step 1: Scraping or synthesizing raw asset graphics...")
    try:
        subprocess.run([sys.executable, "tiny_retro_diffusion/scrape_dataset.py"], check=True)
    except Exception as e:
        log_error(f"خطأ أثناء سحب البيانات: {e}", f"Error during scraping: {e}")
        sys.exit(1)

    # Step 2: Auto-Tagging
    log("الخطوة 2: جاري تشغيل التوسيم التلقائي وصناعة الأوصاف ثنائية اللغة...",
        "Step 2: Running automatic auto-tagging and bilingual prompt indexing...")
    try:
        subprocess.run([sys.executable, "tiny_retro_diffusion/auto_tag_dataset.py"], check=True)
    except Exception as e:
        log_error(f"خطأ أثناء التوسيم التلقائي: {e}", f"Error during auto-tagging: {e}")
        sys.exit(1)

    # Step 3: Neural Model Training (Fine-tuning the custom light bilingual model)
    log("الخطوة 3: جاري تدريب وصقل المعالجة العصبية ثنائية اللغة...",
        "Step 3: Fine-tuning and training the bilingual neural model on custom game assets...")
    try:
        subprocess.run([sys.executable, "tiny_retro_diffusion/train.py"], check=True)
    except Exception as e:
        log_error(f"خطأ أثناء التدريب: {e}", f"Error during neural training: {e}")
        sys.exit(1)

    # Step 4: GGUF Serialization and Quantization mapping
    log("الخطوة 4: جاري تكميم وحفظ النموذج النهائي بصيغة GGUF متوافقة...",
        "Step 4: Quantizing and serializing model parameters into standard compatible GGUF...")
    try:
        subprocess.run([sys.executable, "tiny_retro_diffusion/to_gguf.py"], check=True)
    except Exception as e:
        log_error(f"خطأ أثناء التحويل إلى GGUF: {e}", f"Error during GGUF conversion: {e}")
        sys.exit(1)

    # Step 5: High-fidelity SD model preparation & quantization
    log("الخطوة 5: تكميم نموذج Stable Diffusion الضخم فائق الجودة...",
        "Step 5: Quantizing large-scale high-fidelity Stable Diffusion model...")
    try:
        # Check if safetensors file exists, if not download it
        if not os.path.exists("bk-sdm-tiny.safetensors"):
            subprocess.run([sys.executable, "retro_sd_quantizer/download_and_convert.py"], check=True)

        # Build GGUF quantization using compiled C++ interpreter
        if not os.path.exists("bilingual_retro_tiny_230mb.gguf"):
            subprocess.run([sys.executable, "retro_sd_quantizer/convert_and_quantize.py"], check=True)
            # Copy to current directory as bilingual_retro_tiny_230mb.gguf and models/bilingual_retro_tiny_compatible.gguf
            if os.path.exists("bk-sdm-tiny_q4_0.gguf"):
                import shutil
                shutil.copy("bk-sdm-tiny_q4_0.gguf", "bilingual_retro_tiny_230mb.gguf")
                shutil.copy("bk-sdm-tiny_q4_0.gguf", "models/bilingual_retro_tiny_compatible.gguf")
                log("تم إنشاء وربط النموذج المكمم الضخم بنجاح!", "Successfully created and linked large quantized model!")
    except Exception as e:
        log_error(f"خطأ أثناء تكميم النموذج الضخم: {e}", f"Error during large model quantization: {e}")
        sys.exit(1)

    # Step 6: Text-to-Image Inference & Arabic Calligraphy Rendering
    log("الخطوة 6: تشغيل نموذج الـ GGUF لتوليد صورة بكسل آرت وكتابة الحروف العربية السليمة...",
        "Step 6: Running GGUF model text-to-image inference and rendering correct Arabic text...")
    try:
        subprocess.run([sys.executable, "tiny_retro_diffusion/generate_large_arabic_gguf.py"], check=True)
        log("🎉 خط الإنتاج اكتمل بالكامل بنجاح وتم توليد صورتك العربية فائقة الجودة!",
            "🎉 Master pipeline completed successfully and your high-quality Arabic image has been generated!")
    except Exception as e:
        log_error(f"خطأ أثناء توليد الصورة النهائية: {e}", f"Error during final image generation: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
