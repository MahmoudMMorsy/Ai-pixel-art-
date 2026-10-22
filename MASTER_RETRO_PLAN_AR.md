# 🗺️ الخطة الاستراتيجية الشاملة لتطوير نموذج توليد وتعديل الصور المحلي الضخم (بصيغة GGUF) لريترو بكسل آرت ثنائي اللغة

أهلاً بك يا صديقي المطور البطل في وثيقة التطوير المركزية والشاملة! بناءً على طلبك، قمنا بوضع خارطة طريق استراتيجية شاملة وعملية لبناء نموذج محلي مكتمل لتوليد وتعديل الصور عالي الجودة والسرعة، والمصمم خصيصاً ليعمل بكفاءة مطلقة على الهواتف المحمولة والكمبيوترات الضعيفة دون الحاجة نهائياً لكارت شاشة (CPU-only Optimized)، لإنتاج رسومات بكسل آرت جبارة وتفصيلية تماثل خلفيات وألعاب "الجيم بوي" (Game Boy) والـ "فاميكوم" (Famicom) والرسومات المسحوبة من أشهر منصات الألعاب العالمية.

---

## 💎 الرؤية التقنية ومعمارية النموذج (Model Architecture)

بدلاً من الاعتماد على النماذج الصغيرة المهووسة والتي تفتقر إلى التدريب المسبق الكثيف، تعتمد خطتنا الاستراتيجية على معمارية هجينة ومصقولة تجمع بين جودة النماذج العملاقة وخفة الحجم الفائقة:

1. **معمارية BK-SDM-Tiny المقطرة (Distilled Architecture):**
   - **تقليل المعلمات:** تم تقليص المعالجة العصبية لطبقات الـ UNet من **860 مليون معلمة** إلى **330 مليون معلمة فقط** (بنسبة تقليص تزيد عن 60%) مع الحفاظ التام على القدرات الإبداعية لنموذج **Stable Diffusion v1.5**.
   - **ذاكرة استهلاك منخفضة:** يستهلك النموذج أقل من **1 جيجا بايت** من الذاكرة العشوائية (RAM) أثناء عملية التوليد، مما يجعله آمناً تماماً للتشغيل على الأجهزة الضعيفة والهواتف المحمولة دون التسبب في أي مشكلة خروج مفاجئ للبرنامج (Crash-free).

2. **التكميم الفائق المتناسق (Super-Quantization & GGUF Serialization):**
   - استخدام دقة **Q4_0** المتقدمة لتكميم الأوزان لجميع المكونات (UNet, Text Encoder, VAE) لإنتاج ملف GGUF مكتمل وعالي الجودة بحجم **~710MB** (`bilingual_retro_tiny_230mb.gguf` و `models/bilingual_retro_tiny_compatible.gguf`)، متكامل 100% مع مفسر `stable-diffusion.cpp` والـ JNI المدمج في تطبيق الأندرويد.

---

## 📥 خط سحب وتنظيم البيانات والروابط الرسمية (Bilingual Dataset Pipeline & Sources)

تضمن الخطة استخدام أداة `gallery-dl` واستهداف كافة المصادر والمواقع العالمية لبناء أقوى قاعدة بيانات بكسل آرت:

### 🔗 روابط الداتا سيت المعتمدة:
1. **GitHub Pixel Art Topics:** `https://github.com/topics/pixel-art`
2. **KenneyNL Game Assets:** `https://github.com/KenneyNL`
3. **OpenGameArt Pixel Art:** `https://opengameart.org/art-search-advanced?keys=&field_art_type_tid%5B%5D=9`
4. **Wikimedia Commons Pixel Art:** `https://commons.wikimedia.org/wiki/Category:Pixel_art`
5. **HuggingFace Pixel Art Datasets:** `https://huggingface.co/datasets?search=pixel-art`
6. **Kaggle Pixel Art Datasets:** `https://www.kaggle.com/datasets?search=pixel+art`
7. **The Spriters Resource:** `https://www.spriters-resource.com/`
8. **Artstation Pixel Art Channels:** `https://www.artstation.com/channels/pixel_art`
9. **Artstation Sprite Sheets Search:** `https://www.artstation.com/search?sort_by=relevance&query=pixel%20art%20sprite%20sheet`
10. **Pinterest Pixel Art Sprite Sheets:** `https://www.pinterest.com/search/pins/?q=pixel%20art%20sprite%20sheet`
11. **Pinterest Pixel Art Game Assets:** `https://www.pinterest.com/search/pins/?q=pixel%20art%20game%20assets`

### 🛠️ استخدام أداة gallery-dl للسحب التلقائي:
```bash
# تثبيت الأداة
pip install gallery-dl

# سحب ألبوم بينترست بكسل آرت بأعلى جودة
gallery-dl "https://www.pinterest.com/search/pins/?q=pixel%20art%20sprite%20sheet"

# سحب أصول من آرت ستيشن
gallery-dl "https://www.artstation.com/channels/pixel_art"
```

2. **التوسيم التلقائي ثنائي اللغة (Bilingual Auto-Tagging):**
   - تحليل الصور ديناميكياً لاستخلاص الأبعاد وعدد الألوان الفريدة ونمط الرسم (NES / Gameboy / Famicom).
   - كتابة وتوليد أوصاف مدمجة واحترافية باللغتين العربية والإنجليزية لتمكين التوليد والتحكم ثنائي اللغة بشكل كامل (مثل: `famicom castle brick, size 64x64, colors 4 | قالب طوب قلعة ريترو بكسل، تصميم مستويات جيم بوي`).

---

## ✍️ الدمج المعالج لحل معضلة النصوص العربية (Arabic Text Synthesis Problem)

تقوم الخطة على الدمج الذكي والكامل لحل مشكلة كتابة الحروف العربية المعكوسة والمتقطعة في نماذج الذكاء الاصطناعي:
- **إعادة تشكيل الحروف (Reshaping):** ربط الحروف العربية المتشابكة باستخدام `arabic_reshaper`.
- **التوجيه البصري (BiDi):** معالجة وعكس النصوص من اليمين إلى اليسار بأسلوب مثالي ومطابق للقواعد الإملائية باستخدام `python-bidi`.
- **الرسم الهجين والظلال التباينية (Hybrid Rendering):** دمج النصوص العربية المصقولة بخطوط فائقة الجمال (مثل `NotoNaskhArabic-Regular.ttf`) مباشرة فوق الطبقات التوليدية بدقة متناهية وظلال ثنائية عالية التباين (Drop-Shadow) لضمان ظهور النصوص بوضوح مذهل وجمال لا يقارن.

---

## 🛠️ خطة الخطوات العملية للتشغيل والتوليد (Step-by-Step Execution Plan)

1. **تحميل وتحويل الأوزان:**
   تحميل نموذج الـ BK-SDM-Tiny وتحويله لملف `.safetensors` موحد عبر `retro_sd_quantizer/download_and_convert.py`.
2. **تجميع مفسر GGUF وتكميم النموذج:**
   بناء مكتبة ومفسر `stable-diffusion.cpp` وتكميم ملفات النموذج للحصول على ملف GGUF المكتمل بحجم 710MB عبر `retro_sd_quantizer/convert_and_quantize.py`.
3. **التوليد والتعديل مع الكتابة العربية الفورية (Txt2Img & Img2Img):**
   استدعاء محرك التوليد `tiny_retro_diffusion/generate_large_arabic_gguf.py` لإنتاج وتعديل صور بكسل آرت خرافية مع كتابة النصوص العربية السليمة والمنسقة تلقائياً.
4. **النشر والتشغيل على الموبايل (Pixel Animator):**
   تطبيق الـ APK جاهز بالكامل لدعم استيراد النموذج وتوليد وتعديل رسومات ريترو مذهلة محلياً 100% دون الحاجة إلى إنترنت وبأقل موارد ممكنة.

معاً نصنع أقوى وأخف النماذج المحلية الإبداعية لتوليد الصور والريترو بكسل آرت في العالم! 🎮🚀
