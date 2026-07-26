import os
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import arabic_reshaper
from bidi.algorithm import get_display

def blend_images(images, target_size=(256, 256), blend_strength=0.5):
    """
    Blends multiple images together with transparency.
    """
    if not images:
        return Image.new("RGB", target_size, (12, 12, 24))

    resized = [img.resize(target_size, Image.Resampling.BILINEAR).convert("RGBA") for img in images]

    base = resized[0]
    for i in range(1, len(resized)):
        overlay = resized[i]
        # Blend other images onto the base with alpha transparency
        base = Image.blend(base, overlay, blend_strength)

    return base.convert("RGB")

def quantize_kmeans(image, num_colors=48):
    """
    Applies high-quality K-Means color clustering to restrict the image colors.
    """
    img_arr = np.array(image)
    h, w, c = img_arr.shape
    pixels = img_arr.reshape(-1, c).astype(np.float32)

    unique_pixels = np.unique(pixels, axis=0)
    if len(unique_pixels) <= num_colors:
        return image

    # Choose random unique pixels as initial centroids
    centroids = unique_pixels[np.random.choice(len(unique_pixels), num_colors, replace=False)]

    # Fast 5 iterations for immediate high-quality convergence
    for _ in range(5):
        distances = np.linalg.norm(pixels[:, None, :] - centroids, axis=2)
        assignments = np.argmin(distances, axis=1)
        for i in range(num_colors):
            cluster_pixels = pixels[assignments == i]
            if len(cluster_pixels) > 0:
                centroids[i] = cluster_pixels.mean(axis=0)

    quantized_pixels = centroids[assignments].astype(np.uint8)
    quantized_img = quantized_pixels.reshape(h, w, c)
    return Image.fromarray(quantized_img)

def overlay_arabic_text(image, text, font_path=None, font_size=24):
    """
    Overlays beautiful, properly shaped Arabic text centered at the bottom of the image.
    """
    draw = ImageDraw.Draw(image)
    w, h = image.size

    # Cursive shaping and RTL bidi handling
    reshaped_text = arabic_reshaper.reshape(text)
    bidi_text = get_display(reshaped_text)

    if font_path and os.path.exists(font_path):
        try:
            font = ImageFont.truetype(font_path, font_size)
        except Exception:
            font = ImageFont.load_default()
    else:
        font = ImageFont.load_default()

    try:
        if hasattr(draw, "textbbox"):
            bbox = draw.textbbox((0, 0), bidi_text, font=font)
            text_w = bbox[2] - bbox[0]
            text_h = bbox[3] - bbox[1]
        else:
            text_w, text_h = draw.textsize(bidi_text, font=font)
    except Exception:
        text_w, text_h = len(text) * (font_size // 2), font_size

    x = (w - text_w) // 2
    y = int(h * 0.82)

    outline_color = (0, 0, 0)
    for dx in [-2, -1, 0, 1, 2]:
        for dy in [-2, -1, 0, 1, 2]:
            if dx != 0 or dy != 0:
                draw.text((x + dx, y + dy), bidi_text, font=font, fill=outline_color)

    draw.text((x, y), bidi_text, font=font, fill=(255, 255, 255))
    return image

def process_pixelator(image_paths, output_path="retro_pixelator_result.png", grid_size=256, color_count=48, blend_strength=0.5, arabic_text=""):
    print("================================================================")
    print("       🎨 Bilingual Retro Pixelator & Fusion Engine Pro 🎨")
    print("================================================================")
    print(f"📂 Inputs: {image_paths}")
    print(f"📐 Grid Size: {grid_size}x{grid_size} blocks")
    print(f"🌈 Color Palette Restricton: {color_count} colors (K-Means)")

    images = []
    for path in image_paths:
        if os.path.exists(path):
            images.append(Image.open(path))

    if not images:
        print("❌ Error: No valid input images found. Please check image paths.")
        return False

    blended = blend_images(images, target_size=(grid_size, grid_size), blend_strength=blend_strength)
    quantized = quantize_kmeans(blended, num_colors=color_count)

    if arabic_text:
        font_p = "tiny_retro_diffusion/fonts/NotoNaskhArabic-Regular.ttf"
        quantized = overlay_arabic_text(quantized, arabic_text, font_path=font_p, font_size=int(grid_size * 0.09))

    quantized.save(output_path)
    print(f"🎉 Success! Beautiful retro pixel-art saved to: {output_path}")
    return True

if __name__ == "__main__":
    # Example usage
    example_images = ["retro_arabic_generated.png"]
    if len(sys.argv) > 1:
        example_images = sys.argv[1:]

    process_pixelator(
        image_paths=example_images,
        output_path="retro_pixelated_result.png",
        grid_size=256,
        color_count=48,
        blend_strength=0.5,
        arabic_text="بطل الأساطير"
    )
