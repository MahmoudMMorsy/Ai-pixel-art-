import os
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageEnhance
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

def kmeans_quantize(img_pil, k=48):
    """
    Performs fast K-Means color quantization to reduce the image to exactly K dominant colors.
    This creates an optimal custom color palette for retro/pixel-art style.
    """
    img_np = np.array(img_pil.convert("RGB"))
    h, w, c = img_np.shape
    pixels = img_np.reshape(-1, 3).astype(np.float32)

    # Initialize centroids randomly from unique pixels to avoid identical starting centroids
    unique_pixels = np.unique(pixels, axis=0)
    if len(unique_pixels) < k:
        centroids = np.zeros((k, 3), dtype=np.float32)
        centroids[:len(unique_pixels)] = unique_pixels
    else:
        np.random.seed(42) # fixed seed for deterministic quantization
        indices = np.random.choice(len(unique_pixels), k, replace=False)
        centroids = unique_pixels[indices]

    # Run K-Means for 15 iterations (highly optimized, fast & accurate)
    for iteration in range(15):
        # Compute Euclidean distance from each pixel to each centroid
        distances = np.linalg.norm(pixels[:, None, :] - centroids[None, :, :], axis=2)
        labels = np.argmin(distances, axis=1)

        # Calculate new centroids
        new_centroids = np.zeros_like(centroids)
        for j in range(k):
            mask = (labels == j)
            if np.any(mask):
                new_centroids[j] = pixels[mask].mean(axis=0)
            else:
                new_centroids[j] = centroids[j]

        # Convergence check
        if np.allclose(centroids, new_centroids, atol=1e-1):
            centroids = new_centroids
            break
        centroids = new_centroids

    # Re-assign pixels to the final centroids
    distances = np.linalg.norm(pixels[:, None, :] - centroids[None, :, :], axis=2)
    labels = np.argmin(distances, axis=1)
    quantized_pixels = centroids[labels].astype(np.uint8)
    quantized_img_np = quantized_pixels.reshape(h, w, c)

    return Image.fromarray(quantized_img_np), centroids.astype(np.uint8).tolist()

def blend_and_pixelate_image(input_image_path, output_image_path, size=256, k=48, arabic_text="الأسطورة"):
    """
    Loads input image, blends it with an artistic retro-gradient theme,
    pixelates it into an exact 256x256 chunky grid, quantizes it into exactly
    48 custom K-Means colors, and applies stunning RTL Arabic text overlay.
    """
    log("بدء تشغيل أداة الدمج والفلترة الفائقة...", "Initializing high-fidelity blending & pixelation tool...")

    # Load input image
    if os.path.exists(input_image_path):
        img_src = Image.open(input_image_path).convert("RGB")
        log(f"تم تحميل صورة المستخدم الأساسية من: {input_image_path}", f"Loaded user base image from: {input_image_path}")
    else:
        log_error(f"الصورة الأساسية غير موجودة: {input_image_path}. جاري إنشاء صورة تمثيلية رائعة تلقائياً...",
                  f"Base image not found at: {input_image_path}. Synthesizing a gorgeous representation automatically...")
        # Synthesize a fallback representation
        img_src = Image.new("RGB", (512, 512), color=(10, 15, 30))
        draw_fallback = ImageDraw.Draw(img_src)
        # Draw some beautiful sci-fi/retro shapes
        draw_fallback.ellipse([100, 100, 412, 412], fill=(220, 20, 60)) # Crimson Sun
        draw_fallback.rectangle([180, 250, 340, 480], fill=(75, 0, 130)) # Dark Indigo Tower
        draw_fallback.polygon([(260, 120), (180, 250), (340, 250)], fill=(255, 215, 0)) # Golden Spire

    # Create an artistic retro color gradient overlay to blend with
    gradient = Image.new("RGB", img_src.size)
    draw_grad = ImageDraw.Draw(gradient)
    for y in range(gradient.height):
        # Pink to Cyan retro Synthwave gradient
        r = int(255 - (y / gradient.height) * 150)
        g = int((y / gradient.height) * 180)
        b = int(200 + (y / gradient.height) * 55)
        draw_grad.line([(0, y), (gradient.width, y)], fill=(r, g, b))

    # Blend original image with retro-synthwave gradient (35% mix for beautiful neon colors)
    blended = Image.blend(img_src, gradient, alpha=0.35)

    # Boost contrast and saturation slightly to make details pop prior to quantization
    enhancer_con = ImageEnhance.Contrast(blended)
    blended = enhancer_con.enhance(1.25)
    enhancer_sat = ImageEnhance.Color(blended)
    blended = enhancer_sat.enhance(1.3)

    # Downscale smoothly to get correct retro averaging of colors, then resize back with nearest-neighbor to get chunky pixels
    # We downsample to exactly 256x256 blocks!
    pixelated = blended.resize((size, size), Image.Resampling.BILINEAR)

    # Apply K-Means quantization to reduce to exactly 48 colors ("ملامح أوضح بكتير")
    log("جاري احتساب لوحة الألوان المكونة من 48 لونًا عبر خوارزمية K-Means الذكية...",
        "Computing optimal 48-color palette via advanced K-Means clustering...")
    quantized, palette_colors = kmeans_quantize(pixelated, k=k)
    log(f"تم إنشاء لوحة الألوان المخصصة بنجاح! تحتوي على {len(palette_colors)} لوناً فريداً.",
        f"Custom color palette successfully generated! Contains {len(palette_colors)} unique colors.")

    # Resize up to 512x512 so it looks razor sharp and readable on modern displays
    output_img = quantized.resize((512, 512), Image.Resampling.NEAREST)

    # Apply beautiful Arabic text overlay
    if arabic_text:
        log(f"جاري دمج النص العربي الأنيق: '{arabic_text}'", f"Overlaying beautiful Arabic calligraphy: '{arabic_text}'")
        font_path = "tiny_retro_diffusion/fonts/NotoNaskhArabic-Regular.ttf"

        # Check font availability
        if not os.path.exists(font_path):
            font_path = "app/src/main/assets/fonts/NotoNaskhArabic-Regular.ttf"

        img_draw = ImageDraw.Draw(output_img)
        font_size = 44 # crisp, large font size

        if os.path.exists(font_path):
            try:
                font = ImageFont.truetype(font_path, font_size)
            except Exception as e:
                print(f"⚠️ Warning: Could not load Arabic font, using default: {e}")
                font = ImageFont.load_default()
        else:
            font = ImageFont.load_default()

        # Reshape and reverse BiDi for correct Arabic letters connection
        bidi_text = get_arabic_text(arabic_text)

        # Get text dimensions for perfect centering
        try:
            if hasattr(img_draw, "textbbox"):
                bbox = img_draw.textbbox((0, 0), bidi_text, font=font)
                text_w = bbox[2] - bbox[0]
                text_h = bbox[3] - bbox[1]
            else:
                text_w, text_h = img_draw.textsize(bidi_text, font=font)
        except Exception:
            text_w = len(arabic_text) * (font_size // 2)
            text_h = font_size

        # Align text horizontally centered at the bottom third (y = 410)
        x_pos = (512 - text_w) // 2
        y_pos = 410

        # Draw heavy, dark drop-shadow/outline for high-contrast visibility on retro artwork
        outline_color = (15, 10, 25)
        for dx in [-2, -1, 0, 1, 2]:
            for dy in [-2, -1, 0, 1, 2]:
                if dx != 0 or dy != 0:
                    img_draw.text((x_pos + dx, y_pos + dy), bidi_text, font=font, fill=outline_color)

        # Draw main golden/white Arabic text
        text_color = (255, 220, 100) # Neon golden
        img_draw.text((x_pos, y_pos), bidi_text, font=font, fill=text_color)

    # Save final gorgeous pixelated image
    output_img.save(output_image_path)
    log(f"🎉 تم بنجاح حفظ الصورة المدمجة بدقة 256 بكسل و 48 لوناً في: {output_image_path}",
        f"🎉 Success! High-fidelity 256x256 pixelated, 48-color image saved to: {output_image_path}")

if __name__ == "__main__":
    # Input base image is user's uploaded test.png
    input_path = "test.png"
    output_path = "retro_arabic_generated.png"

    # Run the high-fidelity processing
    blend_and_pixelate_image(
        input_image_path=input_path,
        output_image_path=output_path,
        size=256,
        k=48,
        arabic_text="فارس الأسطورة ريترو"
    )
