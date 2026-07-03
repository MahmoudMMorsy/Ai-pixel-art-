package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapExtractor
import com.google.mediapipe.tasks.vision.imagegenerator.ImageGenerator
import com.google.mediapipe.tasks.vision.imagegenerator.ImageGenerator.ImageGeneratorOptions
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Engine for high-performance, on-device local image generation using MediaPipe Image Generator (Diffusion).
 */
class LocalHDImageEngine(private val context: Context) {

    private var imageGenerator: ImageGenerator? = null

    // Configurable path for model files. In a production app, this would be the directory where models are downloaded.
    private val modelPath: String by lazy {
        File(context.filesDir, "image_generator/bins").absolutePath
    }

    /**
     * Initializes the Image Generator with the local model files.
     * Throws an Exception if the model files are not found or initialization fails.
     */
    fun initialize() {
        if (imageGenerator != null) return

        val modelDir = File(modelPath)
        if (!modelDir.exists() || !modelDir.isDirectory) {
            throw Exception("ملفات النموذج المحلي غير موجودة. يرجى التأكد من تحميل ملفات Stable Diffusion في المجلد: $modelPath")
        }

        try {
            val options = ImageGeneratorOptions.builder()
                .setImageGeneratorModelDirectory(modelPath)
                .build()

            imageGenerator = ImageGenerator.createFromOptions(context, options)
        } catch (e: Exception) {
            throw Exception("فشل بدء تشغيل محرك التوليد المحلي: ${e.localizedMessage}")
        }
    }

    /**
     * Generates an image locally based on the prompt.
     * This is a heavy operation and should be called from a background thread.
     */
    suspend fun generateImage(
        prompt: String,
        iterations: Int = 20,
        seed: Int = (0..Int.MAX_VALUE).random()
    ): Bitmap = withContext(Dispatchers.IO) {
        initialize()

        val generator = imageGenerator ?: throw Exception("محرك التوليد المحلي غير مفعّل.")

        try {
            val result = generator.generate(prompt, iterations, seed)
            val mpImage = result?.generatedImage() ?: throw Exception("فشل التوليد: لم يتم إرجاع أي صورة.")

            // Extract Bitmap from MediaPipe's MPImage
            BitmapExtractor.extract(mpImage)
        } catch (e: Exception) {
            throw Exception("خطأ أثناء عملية التوليد المحلي: ${e.localizedMessage}")
        }
    }

    fun close() {
        imageGenerator?.close()
        imageGenerator = null
    }
}
