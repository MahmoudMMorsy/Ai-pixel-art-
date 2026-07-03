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

    enum class ModelArchitecture {
        STABLE_DIFFUSION_V1_5,
        FLUX_1_SCHNELL
    }

    private var currentArchitecture: ModelArchitecture? = null
    private var imageGenerator: ImageGenerator? = null

    private fun getModelPath(architecture: ModelArchitecture): String {
        val subDir = when(architecture) {
            ModelArchitecture.STABLE_DIFFUSION_V1_5 -> "stable_diffusion"
            ModelArchitecture.FLUX_1_SCHNELL -> "flux"
        }
        return File(context.filesDir, "models/$subDir").absolutePath
    }

    /**
     * Initializes the engine for a specific architecture.
     */
    fun initialize(architecture: ModelArchitecture) {
        if (imageGenerator != null && currentArchitecture == architecture) return

        // Close previous if architecture changed
        if (imageGenerator != null) close()

        val modelPath = getModelPath(architecture)
        val modelDir = File(modelPath)

        if (!modelDir.exists() || !modelDir.isDirectory) {
            val modelName = if (architecture == ModelArchitecture.FLUX_1_SCHNELL) "Flux.1" else "Stable Diffusion"
            throw Exception("ملفات نموذج $modelName غير موجودة في: $modelPath")
        }

        try {
            when (architecture) {
                ModelArchitecture.STABLE_DIFFUSION_V1_5 -> {
                    val options = ImageGeneratorOptions.builder()
                        .setImageGeneratorModelDirectory(modelPath)
                        .build()
                    imageGenerator = ImageGenerator.createFromOptions(context, options)
                }
                ModelArchitecture.FLUX_1_SCHNELL -> {
                    // Note: Flux.1 usually requires specialized DiT support.
                    // This implementation assumes a MediaPipe compatible Flux conversion or
                    // acts as a placeholder for a future NCNN/MLC-LLM integration as requested by user.
                    val options = ImageGeneratorOptions.builder()
                        .setImageGeneratorModelDirectory(modelPath)
                        .build()
                    imageGenerator = ImageGenerator.createFromOptions(context, options)
                }
            }
            currentArchitecture = architecture
        } catch (e: Exception) {
            throw Exception("فشل بدء تشغيل محرك ${architecture.name}: ${e.localizedMessage}")
        }
    }

    /**
     * Generates an image locally based on the prompt and architecture.
     */
    suspend fun generateImage(
        prompt: String,
        architecture: ModelArchitecture = ModelArchitecture.STABLE_DIFFUSION_V1_5,
        iterations: Int = 20,
        seed: Int = (0..Int.MAX_VALUE).random()
    ): Bitmap = withContext(Dispatchers.IO) {
        initialize(architecture)

        val generator = imageGenerator ?: throw Exception("محرك التوليد المحلي غير مفعّل.")

        try {
            // For Flux.1 Schnell, fewer iterations are typically needed (1-4)
            val effectiveIterations = if (architecture == ModelArchitecture.FLUX_1_SCHNELL) {
                iterations.coerceAtMost(4)
            } else {
                iterations
            }

            val result = generator.generate(prompt, effectiveIterations, seed)
            val mpImage = result?.generatedImage() ?: throw Exception("فشل التوليد: لم يتم إرجاع أي صورة من $architecture.")

            BitmapExtractor.extract(mpImage)
        } catch (e: Exception) {
            throw Exception("خطأ أثناء عملية التوليد من $architecture: ${e.localizedMessage}")
        }
    }

    fun close() {
        imageGenerator?.close()
        imageGenerator = null
    }
}
