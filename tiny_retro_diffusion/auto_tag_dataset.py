import os
import sys
import json
from PIL import Image

# --- Bilingual Logging Helpers ---
def log(ar_msg, en_msg):
    print(f"🏷️ [AR] {ar_msg}\n🏷️ [EN] {en_msg}\n" + "-" * 50)

def auto_tag_images(input_dir="dataset/raw", output_dir="dataset"):
    log("بدء عملية التوسيم التلقائي وصناعة الأوصاف الثنائية...",
        "Starting automatic auto-tagging and bilingual captioning process...")

    os.makedirs(output_dir, exist_ok=True)
    unified_metadata = {}

    scraper_meta_path = os.path.join(input_dir, "metadata_fallback.json")
    scraper_meta = {}
    if os.path.exists(scraper_meta_path):
        try:
            with open(scraper_meta_path, "r", encoding="utf-8") as f:
                data = json.load(f)
                for item in data:
                    scraper_meta[item["filename"]] = item
            log("تم العثور على أوزان وبيانات وصفية مرجعية مسبقة للصور المنسوخة.",
                "Found reference metadata for scraped images.")
        except Exception as e:
            log(f"فشل قراءة بيانات المرجعية المسبقة: {e}. سيتم التوسيم التحليلي المباشر.",
                f"Failed to read reference metadata: {e}. Continuing with direct analytical tagging.")

    valid_extensions = (".png", ".jpg", ".jpeg")
    images = [f for f in os.listdir(input_dir) if f.lower().endswith(valid_extensions)]

    if not images:
        log("تنبيه: لا توجد صور في المجلد لتوسيمها تلقائياً.",
            "Warning: No images found in the target directory to tag.")
        return

    for img_name in images:
        img_path = os.path.join(input_dir, img_name)

        try:
            with Image.open(img_path) as img:
                width, height = img.size
                colors = img.getcolors(maxcolors=100000)
                num_colors = len(colors) if colors else 0
                mode = img.mode
        except Exception as e:
            log(f"خطأ أثناء قراءة الصورة {img_name}: {e}", f"Error opening image {img_name}: {e}")
            continue

        if img_name in scraper_meta:
            desc_en = scraper_meta[img_name]["desc_en"]
            desc_ar = scraper_meta[img_name]["desc_ar"]
            category = scraper_meta[img_name]["category"]
        else:
            name_clean = os.path.splitext(img_name)[0].replace("_", " ").replace("-", " ")
            category = "items"
            if "character" in name_clean or "hero" in name_clean or "sprite" in name_clean:
                category = "characters"
                desc_en = f"{name_clean} sprite, classic retro game hero, nes gameboy style"
                desc_ar = f"شخصية بكسل {name_clean} ريترو كلاسيكية، طراز جيم بوي فاميكوم"
            elif "tile" in name_clean or "brick" in name_clean or "ground" in name_clean:
                category = "backgrounds"
                desc_en = f"{name_clean} texture block pattern, retro classic game environment design tile"
                desc_ar = f"نمط {name_clean} كتل ريترو بكسل، مربعات تصميم بيئة ألعاب كلاسيكية"
            elif "monster" in name_clean or "enemy" in name_clean or "alien" in name_clean:
                category = "monsters"
                desc_en = f"{name_clean} alien enemy monster sprite, glowing eyes, retro nes style"
                desc_ar = f"وحش عدو {name_clean} ريترو فضائي، عينان متوهجتان، طراز عائلة"
            else:
                desc_en = f"{name_clean} retro game pixel asset, high contrast gaming collectible"
                desc_ar = f"عنصر لعبة بكسل {name_clean} ريترو، قطعة مجمعة لألعاب كلاسيكية عالية التباين"

        features_en = f", size {width}x{height}, colors {num_colors}"
        features_ar = f"، حجم {width}×{height}، عدد الألوان {num_colors}"

        final_desc_en = desc_en + features_en
        final_desc_ar = desc_ar + features_ar

        txt_name = os.path.splitext(img_name)[0] + ".txt"
        txt_path = os.path.join(input_dir, txt_name)
        with open(txt_path, "w", encoding="utf-8") as tf:
            tf.write(f"{final_desc_en} | {final_desc_ar}")

        unified_metadata[img_name] = {
            "filepath": os.path.abspath(img_path),
            "category": category,
            "dimensions": f"{width}x{height}",
            "color_count": num_colors,
            "prompt_en": final_desc_en,
            "prompt_ar": final_desc_ar
        }

        log(f"تم توسيم الصورة {img_name}: {final_desc_ar}", f"Tagged image {img_name}: {final_desc_en}")

    meta_out_path = os.path.join(output_dir, "metadata_tagged.json")
    with open(meta_out_path, "w", encoding="utf-8") as jf:
        json.dump(unified_metadata, jf, ensure_ascii=False, indent=4)

    log(f"تم حفظ البيانات الموسومة الموحدة في: {meta_out_path}",
        f"Unified tagged metadata saved successfully at: {meta_out_path}")

if __name__ == "__main__":
    auto_tag_images()
