import os
import sys
import subprocess

def log_ar(msg):
    print(f"🌀 [AR] {msg}")

def log_en(msg):
    print(f"✨ [EN] {msg}")

def log_sep():
    print("-" * 65)

def main():
    print("=================================================================")
    print("       بوابة تدريب وتكميم نماذج ريترو بكسل آرت المخصصة       ")
    print("       Premium Custom Retro Pixel Art Training Portal          ")
    print("=================================================================")

    log_ar("أهلاً بك يا بطل! هذا السكربت يساعدك على تدريب وتحويل النموذج على صورتك الخاصة")
    log_en("Welcome hero! This script guides you to train and convert models on your custom images locally.")
    log_sep()

    # Determine default image or request
    default_img = "test.png"
    if not os.path.exists(default_img):
        # Look for any png or jpg in root
        found_images = [f for f in os.listdir(".") if f.lower().endswith((".png", ".jpg", ".jpeg"))]
        if found_images:
            default_img = found_images[0]
        else:
            default_img = None

    if default_img:
        log_ar(f"الصورة الأساسية المكتشفة تلقائياً: {default_img}")
        log_en(f"Auto-detected base training image: {default_img}")
    else:
        log_ar("تحذير: لم يتم العثور على صورة أساسية مثل test.png في المجلد الحالي.")
        log_en("Warning: No base image like test.png found in the current directory.")

    log_sep()

    # Inform user about offline execution
    log_ar("تدريب النموذج يتم محلياً 100% على جهازك وبسرعة فائقة دون الحاجة لرفع الصورة لأي مكان!")
    log_en("Training runs 100% locally on your machine. No need to upload large files anywhere!")
    log_sep()

    # Add arguments parser for automations
    import argparse
    parser = argparse.ArgumentParser(description="Custom Retro Trainer")
    parser.add_argument("--image", type=str, default=default_img, help="Path to base image")
    parser.add_argument("--epochs", type=int, default=15, help="Number of epochs to train")
    parser.add_argument("--prompt_ar", type=str, default="فارس الأسطورة ريترو", help="Custom Arabic prompt")
    parser.add_argument("--prompt_en", type=str, default="fantasy pixel-art knight standing in front of a majestic castle", help="Custom English prompt")
    args = parser.parse_args()

    if not args.image or not os.path.exists(args.image):
        log_ar("خطأ: يرجى وضع صورتك الخاصة في مجلد المشروع وتمرير اسمها أو تسميتها test.png")
        log_en("Error: Please put your custom image in the project directory and name it test.png or pass its name via --image")
        sys.exit(1)

    # 1. Start Training
    log_ar(f"جاري بدء التدريب على الصورة: {args.image} لـ {args.epochs} دورات (Epochs)...")
    log_en(f"Starting training on image: {args.image} for {args.epochs} epochs...")
    log_sep()

    try:
        # We invoke our newly updated train_on_user_image script
        cmd = [
            sys.executable,
            "tiny_retro_diffusion/train_on_user_image.py",
            "--image", args.image,
            "--epochs", str(args.epochs),
            "--prompt_ar", args.prompt_ar,
            "--prompt_en", args.prompt_en
        ]
        subprocess.run(cmd, check=True)
    except Exception as e:
        log_ar(f"خطأ أثناء التدريب: {e}")
        log_en(f"Error occurred during training: {e}")
        sys.exit(1)

    log_sep()
    log_ar("اكتمل التدريب وصناعة الأوزان العصبية بنجاح! جاري تحويلها لـ GGUF فائق الخفة...")
    log_en("Neural training and weight serialization finished! Converting to lightweight GGUF...")
    log_sep()

    # 2. Convert to GGUF
    try:
        # Invoke GGUF converter
        cmd_gguf = [
            sys.executable,
            "tiny_retro_diffusion/to_gguf.py"
        ]
        subprocess.run(cmd_gguf, check=True)
    except Exception as e:
        log_ar(f"خطأ أثناء التحويل إلى صيغة GGUF: {e}")
        log_en(f"Error occurred during GGUF conversion: {e}")
        sys.exit(1)

    log_sep()
    log_ar("🎉 مبارك يا بطل! تم توليد النموذج مكمماً بالكامل وبأقصى سرعة وصناعة الملف النهائي:")
    log_ar("   models/bilingual_retro_tiny_compatible.gguf")
    log_en("🎉 Congratulations hero! Your fully quantized, CPU-optimized GGUF model has been built at:")
    log_en("   models/bilingual_retro_tiny_compatible.gguf")
    log_ar("يمكنك نقله فوراً لهاتفك الأندرويد واستيراده داخل تطبيق Pixel Animator والبدء بالتوليد محلياً!")
    log_en("You can transfer it to your Android device, import it into Pixel Animator, and run local generation!")
    log_sep()

if __name__ == "__main__":
    main()
