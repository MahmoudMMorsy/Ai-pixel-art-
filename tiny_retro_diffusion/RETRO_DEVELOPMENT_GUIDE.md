# 🎮 دليل تطوير نموذج ريترو بكسل آرت ثنائي اللغة (GGUF)
# 🎮 Bilingual Retro Pixel Art GGUF Development Guide

أهلاً بك يا صديقي البطل في دليل تطوير خطوط إنتاج وتدريب وتكميم نماذج ريترو بكسل آرت ثنائية اللغة فائقة الخفة والمتوافقة بالكامل مع المعالجات العادية والهواتف المحمولة!
Welcome to the development and training guide for ultra-lightweight, bilingual retro pixel-art diffusion models in GGUF format, optimized for local CPU execution on mobile and weak hardware!

---

## 🚀 خط الإنتاج الكامل / The End-to-End Pipeline

يتكون نظام التطوير المحلي بالكامل من الخطوات التالية:
The local-first pipeline is fully automated and consists of:

```text
[1. Scraper (scrape_dataset.py)] -> [2. Auto-Tagger (auto_tag_dataset.py)] -> [3. Trainer (train.py)] -> [4. GGUF Converter (to_gguf.py)]
```

---

## 📦 المتطلبات البرمجية / Prerequisites & Installation

لتثبيت كامل المكتبات المطلوبة واللازمة لعملية التحميل والتدريب والتكميم:
Install all required libraries for scraping, analytical auto-tagging, training, and GGUF serialization:

```bash
# تفعيل البيئة الافتراضية إذا كانت موجودة
# Activate virtual environment if configured
source venv/bin/activate 2>/dev/null || true

# تثبيت المكتبات
# Install requirements
python3 -m pip install -r tiny_retro_diffusion/requirements.txt gguf safetensors
```

---

## 🛠️ دليل التشغيل خطوة بخطوة / Step-by-Step Execution

### 1. سحب وتجهيز الصور / Automated Dataset Scraping
السكربت مجهز لاستخدام أداة **`gallery-dl`** تلقائياً لسحب بوردات Pinterest وقنوات ArtStation. وإذا لم تتوفر الأداة أو فشلت، سينتقل تلقائياً لتوليد وتحميل أصول ألعاب كلاسيكية فائقة الجودة برمجياً:
The script automatically checks for and installs `gallery-dl` to scrape Pinterest boards, ArtStation channels, and OpenGameArt. If offline or unauthenticated, it seamlessly fallbacks to downloading open assets and procedurally synthesizing clean game sprites:

* **باستخدام رابط سحب مباشر (Using direct scraping link):**
  ```bash
  python3 tiny_retro_diffusion/scrape_dataset.py "https://www.pinterest.com/search/pins/?q=pixel%20art%20sprite%20sheet"
  ```
* **التشغيل الافتراضي والبديل (Standard/Fallback execution):**
  ```bash
  python3 tiny_retro_diffusion/scrape_dataset.py
  ```
*الأصول المحفوظة ستكون في المجلد: `dataset/raw/`*
*The downloaded assets will be saved to: `dataset/raw/`*

---

### 2. التوسيم وصناعة الأوصاف الثنائية / Automatic Auto-Tagging & Captioning
يقوم هذا السكربت بتحليل أبعاد وألوان وصور الألعاب المنسوخة، وينتج تلقائياً ملفات نصوص بملحقات `.txt` مصاحبة لكل صورة، بالإضافة لملف بيانات موحد يحتوي على أوصاف احترافية باللغتين العربية والإنجليزية:
This script analyzes image dimensions, transparency, and palettes to generate high-quality bilingual (Arabic & English) captions. It outputs standard `.txt` sidecar prompt files for each sprite and saves a unified metadata index:

```bash
python3 tiny_retro_diffusion/auto_tag_dataset.py
```
*الأوصاف المنتجة ستكون مثل:*
* `famicom castle brick, size 64x64, colors 4 | قالب طوب قلعة ريترو بكسل، تصميم مستويات جيم بوي، حجم 64×64، عدد الألوان 4`

---

### 3. تدريب وتعديل النموذج / Neural Model Fine-Tuning & Training
يقوم هذا السكربت بتحميل الصور الحقيقية الموسومة تلقائياً وتدريب معمارية الـ UNet ثنائية اللغة (`ContextRetroUNet`) لربط الأوصاف العربية والإنجليزية برسومات ألعاب الطيبين الكلاسيكية:
Loads the scraped real sprites and parses the bilingual annotations to train/fine-tune the joint semantic language embeddings of our `ContextRetroUNet` model:

```bash
python3 tiny_retro_diffusion/train.py
```
*سيتم حفظ الأوزان الناتجة في الملف: `bilingual_retro_tiny.pth`*

---

### 4. التصدير والتكميم لصيغة GGUF / Mapping & GGUF Quantization
لضمان تشغيل النموذج على تطبيق **Local Dream** أو المعالجات الضعيفة دون أي كراش أو استهلاك ضخم للرامات، نقوم بتحويل الأوزان وترميزها لتطابق الطبقات العصبية لـ Stable Diffusion 1.5 لكي يتعرف عليها مفسر `stable-diffusion.cpp` مباشرة:
Converts raw PyTorch `.pth` weights directly to standard-compatible GGUF format, mapping all parameters to standard Stable Diffusion 1.5 layers to prevent loader crashes and memory errors:

```bash
python3 tiny_retro_diffusion/to_gguf.py
```
*الملف النهائي الجاهز للتشغيل سيكون بدقة بكسل آرت جبارة وبحجم 80MB فقط في المسار التالي:*
*The ready-to-run 80MB quantized GGUF model is stored at:*
`models/bilingual_retro_tiny_compatible.gguf`

---

## 🖥️ التوليد والاختبار المحلي / Local Testing & Inference

يمكنك توليد واختبار صور بكسل آرت حقيقية ومصقولة مباشرة من الطرفية عبر كتابة أي وصف باللغة العربية أو الإنجليزية:
Generate and verify beautiful pixel art wallpapers directly on your CPU/GPU using Arabic or English prompts:

* **التوليد باللغة العربية:**
  ```bash
  python3 tiny_retro_diffusion/generate.py "فارس بكسل ريترو"
  ```
* **Generation in English:**
  ```bash
  python3 tiny_retro_diffusion/generate.py "mario sprite retro gameboy style"
  ```
*الصورة الناتجة تحفظ في المجلد الرئيسي باسم `retro_generated.png` بجودة بكسل حقيقية ومصقولة.*
*The resulting high-quality pixelated sprite is saved as `retro_generated.png`.*

---

## 📱 التشغيل داخل تطبيق الأندرويد / Deploying on Mobile App (Pixel Animator)

1. انقل ملف النموذج الناتج `models/bilingual_retro_tiny_compatible.gguf` إلى ذاكرة الهاتف.
2. افتح تطبيق **Pixel Animator** واضغط على زر **"استيراد نموذج GGUF من جهازك"** في لوحة إدارة النماذج المحلية (Model Center).
3. اكتب أي وصف فني ريترو مثل "قلعة ريترو بكسل" وشاهد قوة وسرعة توليد فن البكسل محلياً 100% بدون إنترنت وبأقل موارد ممكنة!

---

💡 **ملاحظة فنية:** تم تصميم وتوسيع خط الإنتاج هذا ليكون حراً بالكامل ومتاحاً للتحديث والتدريب المستمر بمجرد توفر أصول ورسومات ألعاب جديدة! بالتوفيق يا صديقي المطور البطل! 🎮🚀
