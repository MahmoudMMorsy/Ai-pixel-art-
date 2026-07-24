import os
import sys
import subprocess
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
        # Compatibility with different Pillow versions
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

def generate_image_gguf(prompt, arabic_text=None, model_path="bilingual_retro_tiny_230mb.gguf", output_path="retro_generated_gguf.png", steps=15, width=512, height=512, mode="txt2img", init_image=None, strength=0.75):
    """
    Executes native stable-diffusion.cpp GGUF inference engine to generate or edit images,
    and dynamically applies beautifully rendered Arabic text.
    """
    log(f"بدء إنتاج الصورة باستخدام نموذج GGUF الكبير: {model_path}",
        f"Starting high-fidelity GGUF inference using large-scale model: {model_path}")

    sd_cli = "stable-diffusion.cpp/build/bin/sd-cli"
    if not os.path.exists(sd_cli):
        log_error("ملف مفسر GGUF (sd-cli) غير موجود. يرجى تجميعه أولاً.",
                  "Could not find compiled sd-cli binary. Please build stable-diffusion.cpp first.")
        return False

    if not os.path.exists(model_path):
        log_error(f"ملف نموذج GGUF غير موجود في: {model_path}", f"GGUF model file not found at: {model_path}")
        return False

    temp_out = "temp_gguf_out.png"
    if os.path.exists(temp_out):
        os.remove(temp_out)

    # Construct the native stable-diffusion.cpp CLI command
    cmd = [
        sd_cli,
        "-m", model_path,
        "-p", prompt,
        "-o", temp_out,
        "--steps", str(steps),
        "-W", str(width),
        "-H", str(height),
        "-s", "42" # fixed seed for reproducibility
    ]

    # Handle image-to-image (editing) mode if requested
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

    log(f"جاري تشغيل عملية المعالجة العصبية العميقة: {' '.join(cmd)}",
        f"Executing deep neural processing: {' '.join(cmd)}")

    try:
        subprocess.run(cmd, check=True)
        if os.path.exists(temp_out):
            # If Arabic text overlay is requested, process and overlay it
            if arabic_text:
                font_path = "tiny_retro_diffusion/fonts/NotoNaskhArabic-Regular.ttf"
                # Position text nicely at the bottom center
                pos = (width // 2, int(height * 0.82))
                overlay_arabic_text(
                    image_path=temp_out,
                    text=arabic_text,
                    output_path=output_path,
                    font_path=font_path,
                    font_size=int(height * 0.09), # adaptive font size (9% of height)
                    position=pos
                )
                os.remove(temp_out)
            else:
                # No Arabic text, just rename the temporary file to final output
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
    # Test generation from prompt
    prompt_str = "A gorgeous retro fantasy pixel-art knight standing in front of a majestic castle, 8-bit NES color scheme"
    arabic_text_str = "فارس الأسطورة"

    # Run the integration test
    generate_image_gguf(
        prompt=prompt_str,
        arabic_text=arabic_text_str,
        model_path="bilingual_retro_tiny_230mb.gguf",
        output_path="retro_arabic_generated.png",
        steps=10,
        width=512,
        height=512
    )
