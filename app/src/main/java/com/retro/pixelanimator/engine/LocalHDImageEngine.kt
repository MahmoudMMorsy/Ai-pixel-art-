package com.retro.pixelanimator.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.google.mediapipe.framework.image.BitmapExtractor
import com.google.mediapipe.tasks.vision.imagegenerator.ImageGenerator
import com.google.mediapipe.tasks.vision.imagegenerator.ImageGenerator.ImageGeneratorOptions
import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Engine for high-performance, on-device local image generation using MediaPipe Image Generator (Diffusion).
 */
class LocalHDImageEngine(private val context: Context) {

    private var jniLoadError: String? = null

    // Load the native compiled stable-diffusion.cpp JNI wrapper
    init {
        try {
            System.loadLibrary("stable_diffusion_jni")
        } catch (e: UnsatisfiedLinkError) {
            jniLoadError = "فشل تحميل مكتبة الـ JNI: ${e.localizedMessage}"
        } catch (e: Exception) {
            jniLoadError = "خطأ غير متوقع أثناء تحميل المكتبة: ${e.localizedMessage}"
        }
    }

    // Declare the C++ JNI bridge native external methods
    private external fun initModel(modelPath: String): Long
    private external fun generateImageFromC(ctxPtr: Long, prompt: String, steps: Int, width: Int, height: Int): ByteArray?
    private external fun freeModelContext(ctxPtr: Long)

    private var modelCtxPointer: Long = 0

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
        val path = File(context.filesDir, "models/$subDir").absolutePath
        // Ensure parent models directory exists
        File(context.filesDir, "models/$subDir").mkdirs()
        return path
    }

    /**
     * Looks up any available GGUF model in the local models/stable_diffusion/ directory.
     */
    fun findAvailableGgufModel(): File? {
        val targetDir = File(context.filesDir, "models/stable_diffusion")
        if (targetDir.exists() && targetDir.isDirectory) {
            val files = targetDir.listFiles { file -> file.isFile && file.name.endsWith(".gguf") }
            if (files != null && files.isNotEmpty()) {
                // Return the first GGUF file found
                return files[0]
            }
        }
        return null
    }

    /**
     * Copies the GGUF model from assets to internal storage if not already extracted.
     */
    private fun extractModelFromAssetsIfNecessary() {
        val targetDir = File(context.filesDir, "models/stable_diffusion")
        val targetFile = File(targetDir, "bilingual_retro_tiny_q4_0.gguf")
        if (!targetFile.exists() && findAvailableGgufModel() == null) {
            targetDir.mkdirs()
            try {
                context.assets.open("models/stable_diffusion/bilingual_retro_tiny_q4_0.gguf").use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                // If asset doesn't exist, we fall back gracefully
            }
        }
    }

    /**
     * Initializes the engine for a specific architecture.
     */
    fun initialize(architecture: ModelArchitecture) {
        if (imageGenerator != null && currentArchitecture == architecture) return

        // Close previous if architecture changed
        if (imageGenerator != null) close()

        if (jniLoadError != null) {
            throw Exception(jniLoadError)
        }

        extractModelFromAssetsIfNecessary()

        val modelPath = getModelPath(architecture)
        val modelDir = File(modelPath)

        val activeGgufFile = findAvailableGgufModel()
        val modelFileExists = activeGgufFile != null && activeGgufFile.exists()

        if (!modelFileExists && (!modelDir.exists() || !modelDir.isDirectory)) {
            val modelName = if (architecture == ModelArchitecture.FLUX_1_SCHNELL) "Flux.1" else "Stable Diffusion"
            throw Exception("ملفات نموذج $modelName غير موجودة في: $modelPath. يرجى استيراد ملف GGUF من لوحة التحكم بالأسفل أولاً.")
        }

        try {
            if (modelFileExists && activeGgufFile != null) {
                // Natively initialize and load the GGUF model via our compiled stable-diffusion.cpp JNI wrapper
                if (modelCtxPointer == 0L) {
                    modelCtxPointer = initModel(activeGgufFile.absolutePath)
                }
                if (modelCtxPointer == 0L) {
                    throw Exception("فشل تحميل الموديل: ملف GGUF غير متوافق أو تالف.")
                }
                currentArchitecture = architecture
            } else {
                when (architecture) {
                    ModelArchitecture.STABLE_DIFFUSION_V1_5 -> {
                        val options = ImageGeneratorOptions.builder()
                            .setImageGeneratorModelDirectory(modelPath)
                            .build()
                        imageGenerator = ImageGenerator.createFromOptions(context, options)
                    }
                    ModelArchitecture.FLUX_1_SCHNELL -> {
                        val options = ImageGeneratorOptions.builder()
                            .setImageGeneratorModelDirectory(modelPath)
                            .build()
                        imageGenerator = ImageGenerator.createFromOptions(context, options)
                    }
                }
                currentArchitecture = architecture
            }
        } catch (e: Exception) {
            if (modelFileExists) {
                throw Exception("فشل بدء تشغيل محرك C++ JNI: ${e.localizedMessage}")
            } else {
                throw Exception("فشل بدء تشغيل محرك ${architecture.name}: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Renders a fallback bitmap using our fast local neural compositional pixel engine.
     */
    private fun generateFallbackBitmap(prompt: String): Bitmap {
        val localEngine = LocalPixelEngine()
        val response = localEngine.generateLocalAnimation(prompt, 1, "")

        // Render the frames to intermediate bitmaps
        val p = response.palette
        val frameString = response.frames.firstOrNull() ?: "0".repeat(256)

        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()

        val cellSize = 16f
        for (row in 0 until 16) {
            for (col in 0 until 16) {
                val charIdx = row * 16 + col
                val char = if (charIdx < frameString.length) frameString[charIdx] else '0'
                val index = try { char.toString().toInt(16) } catch (e: Exception) { 0 }
                val hexColor = p.getOrNull(index) ?: "#0F172A"
                paint.color = android.graphics.Color.parseColor(hexColor)

                canvas.drawRect(
                    col * cellSize,
                    row * cellSize,
                    (col + 1) * cellSize,
                    (row + 1) * cellSize,
                    paint
                )
            }
        }
        return bitmap
    }

    /**
     * Generates an image locally with iterative feedback (Progressive Denoising).
     * Emits intermediate bitmaps as the image is refined.
     */
    fun generateImageIterative(
        prompt: String,
        architecture: ModelArchitecture = ModelArchitecture.STABLE_DIFFUSION_V1_5,
        iterations: Int = 20,
        seed: Int = (0..Int.MAX_VALUE).random()
    ): Flow<Bitmap> = flow {
        initialize(architecture)
        val activeGgufFile = findAvailableGgufModel()

        val effectiveIterations = if (architecture == ModelArchitecture.FLUX_1_SCHNELL) {
            iterations.coerceAtMost(4)
        } else {
            iterations
        }

        if (modelCtxPointer != 0L) {
            // Native GGUF loading and generation through our stable-diffusion.cpp JNI wrapper!
            val argbData = generateImageFromC(modelCtxPointer, prompt, effectiveIterations, 256, 256)
            if (argbData != null) {
                val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(argbData))
                emit(bitmap)
                return@flow
            } else {
                throw Exception("فشل التوليد: فشل مفسر C++ في تخليق مصفوفة البكسل.")
            }
        }

        val generator = imageGenerator ?: throw Exception("محرك التوليد المحلي غير مفعّل.")

        try {
            // Initialize inputs for the iterative process
            generator.setInputs(prompt, effectiveIterations, seed)

            for (step in 0 until effectiveIterations) {
                // Execute a single step and request intermediate result
                val result = generator.execute(true)
                val mpImage = result?.generatedImage()
                if (mpImage != null) {
                    emit(BitmapExtractor.extract(mpImage))
                }
            }
        } catch (e: Exception) {
            throw Exception("خطأ أثناء عملية التوليد التدرجي من $architecture: ${e.localizedMessage}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Generates an image locally based on the prompt and architecture (Non-iterative).
     */
    suspend fun generateImage(
        prompt: String,
        architecture: ModelArchitecture = ModelArchitecture.STABLE_DIFFUSION_V1_5,
        iterations: Int = 20,
        seed: Int = (0..Int.MAX_VALUE).random()
    ): Bitmap = withContext(Dispatchers.IO) {
        initialize(architecture)

        val effectiveIterations = if (architecture == ModelArchitecture.FLUX_1_SCHNELL) {
            iterations.coerceAtMost(4)
        } else {
            iterations
        }

        if (modelCtxPointer != 0L) {
            // Native GGUF loading and generation through our stable-diffusion.cpp JNI wrapper!
            val argbData = generateImageFromC(modelCtxPointer, prompt, effectiveIterations, 256, 256)
            if (argbData != null) {
                val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(argbData))
                return@withContext bitmap
            } else {
                throw Exception("فشل التوليد: فشل مفسر C++ في تخليق مصفوفة البكسل.")
            }
        }

        val generator = imageGenerator
        if (generator == null) {
            // Smoothly fallback to the gorgeous responsive local neural pixel engine
            return@withContext generateFallbackBitmap(prompt)
        }

        try {
            val result = generator.generate(prompt, effectiveIterations, seed)
            val mpImage = result?.generatedImage() ?: throw Exception("فشل التوليد: لم يتم إرجاع أي صورة من $architecture.")

            BitmapExtractor.extract(mpImage)
        } catch (e: Exception) {
            throw Exception("خطأ أثناء عملية التوليد من $architecture: ${e.localizedMessage}")
        }
    }

    /**
     * Blends the base image with an artistic retro-gradient theme,
     * pixelates it to exactly 256x256 chunky blocks, reduces it to exactly 48 colors
     * using on-device K-Means clustering, and draws high-contrast RTL Arabic text.
     */
    fun blendAndPixelateOnDevice(
        baseBitmap: Bitmap,
        arabicText: String?,
        size: Int = 256,
        k: Int = 48
    ): Bitmap {
        // 1. Create Synthwave Gradient Overlay
        val widthOrig = baseBitmap.width
        val heightOrig = baseBitmap.height
        val gradientBitmap = Bitmap.createBitmap(widthOrig, heightOrig, Bitmap.Config.ARGB_8888)
        val gradCanvas = Canvas(gradientBitmap)
        val gradPaint = Paint()
        for (y in 0 until heightOrig) {
            val ratio = y.toFloat() / heightOrig
            val r = (255 - ratio * 150).toInt().coerceIn(0, 255)
            val g = (ratio * 180).toInt().coerceIn(0, 255)
            val b = (200 + ratio * 55).toInt().coerceIn(0, 255)
            gradPaint.color = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            gradCanvas.drawLine(0f, y.toFloat(), widthOrig.toFloat(), y.toFloat(), gradPaint)
        }

        // Blend base image with 35% synthwave gradient
        val blendedBitmap = Bitmap.createBitmap(widthOrig, heightOrig, Bitmap.Config.ARGB_8888)
        val blendCanvas = Canvas(blendedBitmap)
        blendCanvas.drawBitmap(baseBitmap, 0f, 0f, null)
        val blendPaint = Paint().apply {
            alpha = (0.35f * 255).toInt()
        }
        blendCanvas.drawBitmap(gradientBitmap, 0f, 0f, blendPaint)

        // 2. Downsample smoothly to exactly 256x256 chunky blocks
        val scaledBitmap = Bitmap.createScaledBitmap(blendedBitmap, size, size, true)

        // 3. K-Means Quantization to exactly 48 colors (for clearer retro features)
        val pixels = IntArray(size * size)
        scaledBitmap.getPixels(pixels, 0, size, 0, 0, size, size)

        val pixelColors = Array(pixels.size) { FloatArray(3) }
        for (i in pixels.indices) {
            val color = pixels[i]
            pixelColors[i][0] = ((color shr 16) and 0xFF).toFloat()
            pixelColors[i][1] = ((color shr 8) and 0xFF).toFloat()
            pixelColors[i][2] = (color and 0xFF).toFloat()
        }

        // Extract unique colors to initialize unique centroids
        val uniqueColors = pixelColors.distinctBy { it[0].toInt() shl 16 or (it[1].toInt() shl 8) or it[2].toInt() }
        val centroids = Array(k) { FloatArray(3) }
        val random = java.util.Random(42)

        if (uniqueColors.size < k) {
            for (i in 0 until k) {
                val source = uniqueColors.getOrNull(i % uniqueColors.size) ?: floatArrayOf(0f, 0f, 0f)
                centroids[i] = source.clone()
            }
        } else {
            val chosenIndices = mutableSetOf<Int>()
            for (i in 0 until k) {
                var idx = random.nextInt(uniqueColors.size)
                while (chosenIndices.contains(idx)) {
                    idx = random.nextInt(uniqueColors.size)
                }
                chosenIndices.add(idx)
                centroids[i] = uniqueColors[idx].clone()
            }
        }

        val labels = IntArray(pixels.size)
        val iterations = 8 // fast and extremely accurate for 256x256
        for (iter in 0 until iterations) {
            // Assign pixels to closest centroid
            for (i in pixels.indices) {
                val p = pixelColors[i]
                var minDist = Float.MAX_VALUE
                var minLabel = 0
                for (j in 0 until k) {
                    val c = centroids[j]
                    val dx = p[0] - c[0]
                    val dy = p[1] - c[1]
                    val dz = p[2] - c[2]
                    val dist = dx*dx + dy*dy + dz*dz
                    if (dist < minDist) {
                        minDist = dist
                        minLabel = j
                    }
                }
                labels[i] = minLabel
            }

            // Calculate new centroids
            val sum = Array(k) { FloatArray(3) }
            val count = IntArray(k)
            for (i in pixels.indices) {
                val label = labels[i]
                val p = pixelColors[i]
                sum[label][0] += p[0]
                sum[label][1] += p[1]
                sum[label][2] += p[2]
                count[label]++
            }

            for (j in 0 until k) {
                if (count[j] > 0) {
                    centroids[j][0] = sum[j][0] / count[j]
                    centroids[j][1] = sum[j][1] / count[j]
                    centroids[j][2] = sum[j][2] / count[j]
                }
            }
        }

        // Reconstruct image with quantized pixels
        val quantizedPixels = IntArray(pixels.size)
        for (i in pixels.indices) {
            val c = centroids[labels[i]]
            val r = c[0].coerceIn(0f, 255f).toInt()
            val g = c[1].coerceIn(0f, 255f).toInt()
            val b = c[2].coerceIn(0f, 255f).toInt()
            quantizedPixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        val quantizedBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        quantizedBitmap.setPixels(quantizedPixels, 0, size, 0, 0, size, size)

        // 4. Upscale with NEAREST-NEIGHBOR to exactly 512x512 so it's super sharp
        val finalBitmap = Bitmap.createScaledBitmap(quantizedBitmap, 512, 512, false)

        // 5. Draw Arabic Text Overlay at the bottom center
        if (!arabicText.isNullOrBlank()) {
            val finalCanvas = Canvas(finalBitmap)
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#FFDC64") // Neon golden
                textSize = 40f
                textAlign = Paint.Align.CENTER
                try {
                    typeface = Typeface.createFromAsset(context.assets, "fonts/NotoNaskhArabic-Regular.ttf")
                } catch (e: Exception) {
                    typeface = Typeface.DEFAULT_BOLD
                }
            }

            val x = 256f // Center of 512f
            val y = 430f // Bottom third

            // High-contrast dark outline
            val outlinePaint = Paint(textPaint).apply {
                color = android.graphics.Color.parseColor("#0F0A19")
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
            finalCanvas.drawText(arabicText, x, y, outlinePaint)
            finalCanvas.drawText(arabicText, x, y, textPaint)
        }

        return finalBitmap
    }

    fun close() {
        if (modelCtxPointer != 0L) {
            freeModelContext(modelCtxPointer)
            modelCtxPointer = 0L
        }
        imageGenerator?.close()
        imageGenerator = null
    }
}
