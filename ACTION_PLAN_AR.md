# 🚀 خطة العمل التنفيذية السريعة (Strategic Execution Guide)

دليلك العملي لتشغيل سحب البيانات، تدريب النموذج، وتوليد وتعديل صور البيكسل آرت بصيغة GGUF محلياً بالكامل.

---

## ⚡ الخطوات التنفيذية السريعة

### 1. تثبيت الحزم المطلوبة:
```bash
python3 -m pip install -r tiny_retro_diffusion/requirements.txt -r retro_sd_quantizer/requirements.txt gguf safetensors gallery-dl
```

### 2. سحب البيانات عبر gallery-dl أو التخليق الإجرائي:
```bash
python3 tiny_retro_diffusion/scrape_dataset.py
```
*السكربت مبرمج ليفحص تلقائياً أداة gallery-dl، ويسحب من المنصات المعتمدة (Pinterest, ArtStation, OpenGameArt, Kenney, HuggingFace, Kaggle, Wikimedia, Spriters Resource)، مع آلية احتياطية ذكية (Fallback) لإنشاء وتنزيل أصول بيكسل آرت ريترو نقية 100%.*

### 3. التوسيم التلقائي ثنائي اللغة (Bilingual Auto-Tagging):
```bash
python3 tiny_retro_diffusion/auto_tag_dataset.py
```

### 4. توليد وتعديل صورة ريترو بيكسل آرت فورية بنص عربي سليم:
```bash
# توليد جديد بنمط الفاميكوم / نينتندو:
python3 tiny_retro_diffusion/generate_large_arabic_gguf.py --prompt "retro pixel-art knight standing in front of castle" --arabic "فارس الأسطورة" --palette nes

# توليد جديد بنمط الجيم بوي الأصلي:
python3 tiny_retro_diffusion/generate_large_arabic_gguf.py --prompt "game boy space fighter craft" --arabic "صقر الفضاء" --palette gameboy

# تعديل صورة موجودة (Img2Img):
python3 tiny_retro_diffusion/generate_large_arabic_gguf.py --mode img2img --init_image test.png --arabic "تعديل ريترو" --strength 0.65
```

### 5. تشغيل خط الإنتاج الموحد بضغطة زر:
```bash
python3 run_master_pipeline.py
```

---

## 🎮 نقل النموذج واستخدامه في تطبيق الموبايل (Pixel Animator)

1. خذ الملف المكمم الناتج:
   `models/bilingual_retro_tiny_compatible.gguf`
2. ضعه في هاتفك أو افتح تطبيق `PixelAnimatorRetro.apk` وقم باستيراده في نافذة "Pixel Lab".
3. التطبيق سيتعرف على النموذج فوراً ويبدأ التوليد والتعديل السريع بدون استهلاك موارد وبدون كارت شاشة!
