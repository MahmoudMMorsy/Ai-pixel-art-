import os
import sys
import subprocess
import argparse
from PIL import Image, ImageDraw, ImageFont
import arabic_reshaper
from bidi.algorithm import get_display

def log(ar_msg, en_msg):
    print(f"🌀 [AR] {ar_msg}\n✨ [EN] {en_msg}\n" + "-" * 60)

def log_error(ar_msg, en_msg):
    print(f"❌ [AR] {ar_msg}\n❌ [EN] {en_msg}\n" + "-" * 60, file=sys.stderr)

def get_arabic_text(text):
    """
    Reshapes and aligns Arabic text so it displays correctly from right to left (RTL)
    with proper cursive letter connection.
    """
    reshaped_text = arabic_reshaper.reshape(text)
    bidi_text = get_display(reshaped_text)
    return bidi_text

def overlay_arabic_text(image_path, text, output_path, font_path=None, font_size=40, position=(256, 420), text_color=(255, 255, 255), outline_color=(0, 0, 0)):
    """
    Overlays beautiful Arabic calligraphy/text with a high-contrast outline on the image.
    """
    if not os.path.exists(image_path):
        log_error(f"الصورة المصدر غير موجودة في: {image_path}", f"Source image not found at: {image_path}")
        return False

    img = Image.open(image_path).convert("RGBA")
    draw = ImageDraw.Draw(img)

    # Use specified Arabic font or default system fallback
    if font_path and os.path.exists(font_path):
        try:
            font = ImageFont.truetype(font_path, font_size)
        except Exception as e:
            print(f"⚠️ Warning: Could not load custom font {font_path} ({e}). Using default font.")
            font = ImageFont.load_default()
    else:
        font = ImageFont.load_default()

    # Process Arabic text
    aligned_text = get_arabic_text(text)

    # Calculate text dimensions for centering
    try:
        if hasattr(draw, "textbbox"):
            bbox = draw.textbbox((0, 0), aligned_text, font=font)
            text_w = bbox[2] - bbox[0]
            text_h = bbox[3] - bbox[1]
        else:
            text_w, text_h = draw.textsize(aligned_text, font=font)
    except Exception:
        text_w, text_h = len(text) * (font_size // 2), font_size

    # Adjust position to center horizontally
    x = position[0] - (text_w // 2)
    y = position[1] - (text_h // 2)

    # Draw high-contrast outline (drop-shadow effect)
    for dx in [-2, -1, 0, 1, 2]:
        for dy in [-2, -1, 0, 1, 2]:
            if dx != 0 or dy != 0:
                draw.text((x + dx, y + dy), aligned_text, font=font, fill=outline_color)

    # Draw the main Arabic text
    draw.text((x, y), aligned_text, font=font, fill=text_color)

    # Save final image
    img.convert("RGB").save(output_path)
    log(f"تمت بنجاح كتابة النص العربي وإخراج الصورة إلى: {output_path}",
        f"Arabic text successfully overlayed and saved to: {output_path}")
    return True

def resolve_model_path(requested_path):
    """
    Resolves the best available GGUF model path across standard locations.
    """
    candidates = [
        requested_path,
        "bilingual_retro_tiny_230mb.gguf",
        "models/bilingual_retro_tiny_compatible.gguf",
        "bk-sdm-tiny_q4_0.gguf",
        "bk-sdm-tiny_q8_0.gguf"
    ]
    for c in candidates:
        if c and os.path.exists(c):
            return c
    return None

def resolve_sd_cli():
    """
    Finds the compiled stable-diffusion.cpp CLI executable.
    """
    candidates = [
        "stable-diffusion.cpp/build/bin/sd-cli",
        "stable-diffusion.cpp/bin/sd-cli",
        "stable-diffusion.cpp/build/Release/sd-cli"
    ]
    for c in candidates:
        if os.path.exists(c):
            return c
    return None

def generate_image_gguf(prompt, arabic_text=None, model_path="bilingual_retro_tiny_230mb.gguf", output_path="retro_generated_gguf.png", steps=15, width=512, height=512, mode="txt2img", init_image=None, strength=0.75):
    """
    Executes native stable-diffusion.cpp GGUF inference engine to generate or edit images,
    and dynamically applies beautifully rendered Arabic text.
    """
    resolved_model = resolve_model_path(model_path)
    if not resolved_model:
        log_error(f"لم يتم العثور على أي نموذج GGUF صالح. المسار المطلوب: {model_path}",
                  f"Could not locate any valid GGUF model. Specified path: {model_path}")
        return False

    sd_cli = resolve_sd_cli()
    if not sd_cli:
        log_error("ملف مفسر GGUF (sd-cli) غير موجود. يرجى تجميعه أولاً عبر stable-diffusion.cpp.",
                  "Could not find compiled sd-cli binary. Please build stable-diffusion.cpp first.")
        return False

    log(f"بدء إنتاج الصورة باستخدام نموذج GGUF: {resolved_model}",
        f"Starting high-fidelity GGUF inference using model: {resolved_model}")

    temp_out = "temp_gguf_out.png"
    if os.path.exists(temp_out):
        os.remove(temp_out)

    cmd = [
        sd_cli,
        "-m", resolved_model,
        "-p", prompt,
        "-o", temp_out,
        "--steps", str(steps),
        "-W", str(width),
        "-H", str(height),
        "-s", "42"
    ]

    if mode == "img2img" and init_image:
        if os.path.exists(init_image):
            cmd.extend([
                "--init-img", init_image,
                "--strength", str(strength)
            ])
            log(f"تفعيل نمط تعديل الصور (Img2Img) باستخدام الصورة الأساسية: {init_image}",
                f"Enabled Image-to-Image editing using initial image: {init_image}")
        else:
            log_error(f"الصورة الأساسية للتعديل غير موجودة: {init_image}",
                      f"Initial image for editing not found: {init_image}")
            return False

    log(f"جاري تشغيل المعالجة العصبية العميقة: {' '.join(cmd)}",
        f"Executing deep neural processing: {' '.join(cmd)}")

    try:
        subprocess.run(cmd, check=True)
        if os.path.exists(temp_out):
            if arabic_text:
                font_path = "tiny_retro_diffusion/fonts/NotoNaskhArabic-Regular.ttf"
                pos = (width // 2, int(height * 0.82))
                overlay_arabic_text(
                    image_path=temp_out,
                    text=arabic_text,
                    output_path=output_path,
                    font_path=font_path,
                    font_size=int(height * 0.09),
                    position=pos
                )
                os.remove(temp_out)
            else:
                if os.path.exists(output_path):
                    os.remove(output_path)
                os.rename(temp_out, output_path)
                log(f"تم حفظ الصورة المنتجة في: {output_path}", f"Saved generated image to: {output_path}")
            return True
        else:
            log_error("فشل المعالج: لم يتم إنتاج أي ملف صورة مخرجة.",
                      "Processing failed: Native engine did not write any image file.")
            return False
    except Exception as e:
        log_error(f"خطأ أثناء تشغيل مفسر GGUF: {e}", f"Exception occurred during GGUF execution: {e}")
        return False

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="GGUF Bilingual Image Generation & Editing Engine")
    parser.add_argument("--prompt", type=str, default="A gorgeous retro fantasy pixel-art knight standing in front of a majestic castle, 8-bit NES color scheme", help="Prompt text")
    parser.add_argument("--arabic", type=str, default="فارس الأسطورة", help="Arabic text overlay")
    parser.add_argument("--model", type=str, default="bilingual_retro_tiny_230mb.gguf", help="GGUF model file path")
    parser.add_argument("--output", type=str, default="retro_arabic_generated.png", help="Output PNG file path")
    parser.add_argument("--mode", type=str, choices=["txt2img", "img2img"], default="txt2img", help="Inference mode")
    parser.add_argument("--init_image", type=str, default=None, help="Initial image for img2img")
    parser.add_argument("--strength", type=float, default=0.75, help="Img2Img strength")
    parser.add_argument("--steps", type=int, default=10, help="Denoising steps")
    parser.add_argument("--width", type=int, default=512, help="Image width")
    parser.add_argument("--height", type=int, default=512, help="Image height")

    args = parser.parse_args()

    generate_image_gguf(
        prompt=args.prompt,
        arabic_text=args.arabic,
        model_path=args.model,
        output_path=args.output,
        steps=args.steps,
        width=args.width,
        height=args.height,
        mode=args.mode,
        init_image=args.init_image,
        strength=args.strength
    )
