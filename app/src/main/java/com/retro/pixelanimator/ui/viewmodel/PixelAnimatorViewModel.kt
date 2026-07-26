package com.retro.pixelanimator.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.retro.pixelanimator.engine.GeminiPixelEngine
import com.retro.pixelanimator.engine.LocalHDImageEngine
import com.retro.pixelanimator.engine.LocalPixelEngine
import com.retro.pixelanimator.engine.PixelArtAnimationResponse
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val data: PixelArtAnimationResponse) : UiState
    data class RealImageSuccess(val base64Data: String) : UiState
    data class Error(val message: String) : UiState
}

class PixelAnimatorViewModel(application: Application) : AndroidViewModel(application) {
    private val geminiEngine = GeminiPixelEngine()
    private val localEngine = LocalPixelEngine()
    private val localHDEngine = LocalHDImageEngine(application)

    // --- Retro Pixelator & Fusion Mode Parameters ---
    val isImagePixelatorMode = MutableStateFlow(false)
    val pixelatorImagesUris = MutableStateFlow<List<Uri>>(emptyList())
    val pixelatorGridSize = MutableStateFlow(256)
    val pixelatorColorCount = MutableStateFlow(48)
    val pixelatorBlendStrength = MutableStateFlow(0.5f)
    val pixelatedBitmap = MutableStateFlow<Bitmap?>(null)
    val isPixelating = MutableStateFlow(false)
    val pixelatorError = MutableStateFlow<String?>(null)
    val pixelatorArabicText = MutableStateFlow("")

    // --- Core Parameters ---
    val prompt = MutableStateFlow("مكعب ناري متفجر يتلاشى")
    val frameCount = MutableStateFlow(6)
    val paletteHint = MutableStateFlow("ألوان نارية مشبعة (برتقالي وأصفر مع رمادي داكن)")

    // --- Local Model Selection ---
    val isCloudEnabled = MutableStateFlow(true) // Run Gemini Cloud AI by default (Real Image Generation Model!)
    val selectedLocalModel = MutableStateFlow(LocalPixelEngine.MODEL_DIFFUSION)
    
    val localModelsList = listOf(
        LocalPixelEngine.MODEL_DIFFUSION,
        LocalPixelEngine.MODEL_NEURAL_CPPN,
        LocalPixelEngine.MODEL_FIRE,
        LocalPixelEngine.MODEL_FLUID,
        LocalPixelEngine.MODEL_SPIN3D,
        LocalPixelEngine.MODEL_CELLULAR,
        LocalPixelEngine.MODEL_PLASMA,
        LocalPixelEngine.MODEL_GRAVITY,
        LocalPixelEngine.MODEL_WALKER
    )

    // --- Dynamic Model Import Support ---
    val isModelImported = MutableStateFlow(false)
    val importedModelName = MutableStateFlow<String?>(null)
    val importedModelSize = MutableStateFlow<String?>(null)
    val isImporting = MutableStateFlow(false)
    val importError = MutableStateFlow<String?>(null)

    // --- UI/API State ---
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // --- Mode Control (Pixel Art vs Real HD Image Mode) ---
    val isRealImageMode = MutableStateFlow(false)
    val isLocalHDMode = MutableStateFlow(false) // Toggle for Local vs Cloud HD generation
    val selectedHDModel = MutableStateFlow(LocalHDImageEngine.ModelArchitecture.STABLE_DIFFUSION_V1_5)

    private val _realImageBase64 = MutableStateFlow<String?>(null)
    val realImageBase64: StateFlow<String?> = _realImageBase64.asStateFlow()

    // --- Playback State ---
    private val _currentFrameIndex = MutableStateFlow(0)
    val currentFrameIndex: StateFlow<Int> = _currentFrameIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackSpeedMs = MutableStateFlow(250L) // Normal, Fast, Slow
    val playbackSpeedMs: StateFlow<Long> = _playbackSpeedMs.asStateFlow()

    // --- History Tracking ---
    private val _history = MutableStateFlow<List<PixelArtAnimationResponse>>(emptyList())
    val history: StateFlow<List<PixelArtAnimationResponse>> = _history.asStateFlow()

    private var playbackJob: Job? = null

    init {
        checkExistingModel()
        // Hydrate with some default mock historical creations so the user sees nice templates right away
        _history.value = listOf(
            PixelArtAnimationResponse(
                title = "فايربول ناري (مثال بدئي)",
                description = "تم التوليد بنجاح محاكي محلي كامل (Offline AI Sandbox Model): Thermal Fire Automaton (v2.1)",
                palette = listOf("#110E1D", "#7F1D1D", "#DC2626", "#F97316", "#FACC15", "#FFFFFF"),
                frameCount = 4,
                frames = listOf(
                    "0".repeat(112) + "1221" + "0".repeat(12) + "1331" + "0".repeat(12) + "1221" + "0".repeat(112),
                    "0".repeat(96) + "1221" + "0".repeat(12) + "13331" + "0".repeat(11) + "1221" + "0".repeat(124),
                    "0".repeat(80) + "1221" + "0".repeat(12) + "13431" + "0".repeat(11) + "1221" + "0".repeat(140),
                    "0".repeat(64) + "1221" + "0".repeat(12) + "13531" + "0".repeat(11) + "1221" + "0".repeat(156)
                )
            )
        )
    }

    fun checkExistingModel() {
        val file = File(getApplication<Application>().filesDir, "models/stable_diffusion/bilingual_retro_tiny_q4_0.gguf")
        if (file.exists()) {
            isModelImported.value = true
            importedModelName.value = file.name
            val sizeMB = file.length().toDouble() / (1024 * 1024)
            importedModelSize.value = "${String.format("%.2f", sizeMB)} MB"
        } else {
            isModelImported.value = false
            importedModelName.value = null
            importedModelSize.value = null
        }
    }

    fun importGgufModel(uri: Uri) {
        viewModelScope.launch {
            isImporting.value = true
            importError.value = null
            try {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val resolver = context.contentResolver

                    var displayName = "model.gguf"
                    resolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            displayName = cursor.getString(nameIndex)
                        }
                    }

                    val targetDir = File(context.filesDir, "models/stable_diffusion")
                    if (!targetDir.exists()) {
                        targetDir.mkdirs()
                    }
                    val targetFile = File(targetDir, "bilingual_retro_tiny_q4_0.gguf")

                    resolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            val buffer = ByteArray(1024 * 1024) // 1MB buffer
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        isModelImported.value = true
                        importedModelName.value = displayName
                        val sizeMB = targetFile.length().toDouble() / (1024 * 1024)
                        importedModelSize.value = "${String.format("%.2f", sizeMB)} MB"
                    }
                }
            } catch (e: Exception) {
                importError.value = "فشل استيراد النموذج: ${e.localizedMessage}"
            } finally {
                isImporting.value = false
            }
        }
    }

    fun deleteImportedModel() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val file = File(context.filesDir, "models/stable_diffusion/bilingual_retro_tiny_q4_0.gguf")
            if (file.exists()) {
                file.delete()
            }
            withContext(Dispatchers.Main) {
                isModelImported.value = false
                importedModelName.value = null
                importedModelSize.value = null
            }
        }
    }

    fun startAnimationPlayback() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        val maxFrames = getActiveFrameCount()
        if (maxFrames <= 1) return

        playbackJob = viewModelScope.launch {
            while (true) {
                delay(_playbackSpeedMs.value)
                val nextIndex = (_currentFrameIndex.value + 1) % maxFrames
                _currentFrameIndex.value = nextIndex
            }
        }
    }

    fun stopAnimationPlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
    }

    fun togglePlayback() {
        if (_isPlaying.value) {
            stopAnimationPlayback()
        } else {
            startAnimationPlayback()
        }
    }

    fun updatePlaybackSpeed(ms: Long) {
        _playbackSpeedMs.value = ms
        if (_isPlaying.value) {
            stopAnimationPlayback()
        }
    }

    fun setCurrentFrameIndex(index: Int) {
        val max = getActiveFrameCount()
        if (index in 0 until max) {
            _currentFrameIndex.value = index
        }
    }

    fun loadFromHistory(animation: PixelArtAnimationResponse) {
        stopAnimationPlayback()
        _currentFrameIndex.value = 0
        _uiState.value = UiState.Success(animation)
        prompt.value = animation.title
        frameCount.value = animation.frameCount
    }

    fun generatePixelArt() {
        if (isRealImageMode.value) {
            generateRealImage()
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            stopAnimationPlayback()
            _currentFrameIndex.value = 0

            delay(600)

            try {
                val response = if (isCloudEnabled.value) {
                    geminiEngine.generatePixelAnimation(
                        prompt = prompt.value,
                        frameCount = frameCount.value,
                        paletteHint = paletteHint.value
                    )
                } else if (selectedLocalModel.value == LocalPixelEngine.MODEL_DIFFUSION) {
                    // Actual progressively denoised 10-step Local Diffusion loop!
                    var intermediateResponse = localEngine.denoiseStep(
                        prompt = prompt.value,
                        step = 0,
                        totalSteps = 10,
                        frameCount = frameCount.value,
                        paletteHint = paletteHint.value
                    )
                    
                    // Progressive step delay
                    for (step in 1..10) {
                        intermediateResponse = localEngine.denoiseStep(
                            prompt = prompt.value,
                            step = step,
                            totalSteps = 10,
                            frameCount = frameCount.value,
                            paletteHint = paletteHint.value
                        )
                        _uiState.value = UiState.Success(intermediateResponse)
                        delay(120) // Allow UI canvas to render the noise clearing up step-by-step
                    }
                    
                    // Final response is the fully denoised model
                    intermediateResponse
                } else {
                    localEngine.generateLocalAnimation(
                        prompt = prompt.value,
                        frameCount = frameCount.value,
                        paletteHint = paletteHint.value,
                        selectedModel = selectedLocalModel.value
                    )
                }

                _uiState.value = UiState.Success(response)
                
                // Add to beginning of history list
                val updatedHistory = listOf(response) + _history.value.filter { it.title != response.title }
                _history.value = updatedHistory.take(15) // Keep last 15 items

                // Auto-play the newly synthesized gorgeous animation
                startAnimationPlayback()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "حدث خطأ غير متوقع أثناء توليد الرسوم المتحركة.")
            }
        }
    }

    fun generateRealImage() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            stopAnimationPlayback()
            try {
                if (isLocalHDMode.value) {
                    // Check if model exists first!
                    val file = File(getApplication<Application>().filesDir, "models/stable_diffusion/bilingual_retro_tiny_q4_0.gguf")
                    if (!file.exists()) {
                        throw Exception("الملف غير متوفر! يرجى استيراد نموذج GGUF من 'مركز النماذج' بالأسفل قبل التشغيل المحلي.")
                    }
                    localHDEngine.generateImageIterative(
                        prompt = prompt.value,
                        architecture = selectedHDModel.value
                    ).collect { bitmap ->
                        val base64 = bitmapToBase64(bitmap)
                        _realImageBase64.value = base64
                        _uiState.value = UiState.RealImageSuccess(base64)
                    }
                } else {
                    val base64 = geminiEngine.generateRealImage(prompt.value)
                    _realImageBase64.value = base64
                    _uiState.value = UiState.RealImageSuccess(base64)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "حدث خطأ أثناء توليد الصورة.")
            }
        }
    }

    // --- Retro Pixelator & Fusion Engine Core ---

    fun selectPixelatorImages(uris: List<Uri>) {
        pixelatorImagesUris.value = uris.take(3)
        processPixelation()
    }

    fun setPixelatorGridSize(size: Int) {
        pixelatorGridSize.value = size
        processPixelation()
    }

    fun setPixelatorColorCount(count: Int) {
        pixelatorColorCount.value = count
        processPixelation()
    }

    fun setPixelatorBlendStrength(strength: Float) {
        pixelatorBlendStrength.value = strength
        processPixelation()
    }

    fun setPixelatorArabicText(text: String) {
        pixelatorArabicText.value = text
        processPixelation()
    }

    fun processPixelation() {
        val uris = pixelatorImagesUris.value
        if (uris.isEmpty()) {
            pixelatedBitmap.value = null
            return
        }

        viewModelScope.launch {
            isPixelating.value = true
            pixelatorError.value = null

            try {
                val context = getApplication<Application>()
                val loadedBitmaps = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        try {
                            val inputStream = context.contentResolver.openInputStream(uri)
                            android.graphics.BitmapFactory.decodeStream(inputStream)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                if (loadedBitmaps.isEmpty()) {
                    throw Exception("فشل تحميل الصور المحددة.")
                }

                val gridSize = pixelatorGridSize.value
                val colorCount = pixelatorColorCount.value
                val blendStrength = pixelatorBlendStrength.value
                val text = pixelatorArabicText.value

                val processed = withContext(Dispatchers.Default) {
                    // 1. Blend up to 3 bitmaps
                    val blended = blendBitmaps(loadedBitmaps, gridSize, gridSize, blendStrength)

                    // 2. Perform dynamic K-Means color quantization
                    val quantized = quantizeColors(blended, colorCount)

                    // 3. Overlay Arabic text
                    if (text.isNotEmpty()) {
                        overlayArabicTextOnBitmap(context, quantized, text)
                    } else {
                        quantized
                    }
                }

                pixelatedBitmap.value = processed
            } catch (e: Exception) {
                pixelatorError.value = "حدث خطأ أثناء معالجة البكسل: ${e.localizedMessage}"
            } finally {
                isPixelating.value = false
            }
        }
    }

    private fun blendBitmaps(bitmaps: List<Bitmap>, targetWidth: Int, targetHeight: Int, blendStrength: Float): Bitmap {
        if (bitmaps.isEmpty()) {
            return Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        }
        if (bitmaps.size == 1) {
            return Bitmap.createScaledBitmap(bitmaps[0], targetWidth, targetHeight, true)
        }

        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        val paint = android.graphics.Paint()

        paint.color = android.graphics.Color.BLACK
        canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), paint)

        for (i in bitmaps.indices) {
            val scaled = Bitmap.createScaledBitmap(bitmaps[i], targetWidth, targetHeight, true)
            if (i == 0) {
                paint.alpha = 255
                canvas.drawBitmap(scaled, 0f, 0f, paint)
            } else {
                paint.alpha = (blendStrength * 255).toInt().coerceIn(0, 255)
                canvas.drawBitmap(scaled, 0f, 0f, paint)
            }
        }
        return result
    }

    private fun quantizeColors(bitmap: Bitmap, maxColors: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val random = java.util.Random()
        val centroids = IntArray(maxColors.coerceAtMost(pixels.size))
        for (c in centroids.indices) {
            centroids[c] = pixels[random.nextInt(pixels.size)]
        }

        if (centroids.isEmpty()) return bitmap

        val assignments = IntArray(pixels.size)
        val centroidSumR = LongArray(centroids.size)
        val centroidSumG = LongArray(centroids.size)
        val centroidSumB = LongArray(centroids.size)
        val centroidCount = IntArray(centroids.size)

        for (iter in 0 until 5) {
            centroidSumR.fill(0)
            centroidSumG.fill(0)
            centroidSumB.fill(0)
            centroidCount.fill(0)

            for (i in pixels.indices) {
                val p = pixels[i]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF

                var minDist = Double.MAX_VALUE
                var bestIdx = 0
                for (c in centroids.indices) {
                    val cp = centroids[c]
                    val cr = (cp shr 16) and 0xFF
                    val cg = (cp shr 8) and 0xFF
                    val cb = cp and 0xFF

                    val dist = (r - cr) * (r - cr) + (g - cg) * (g - cg) + (b - cb) * (b - cb)
                    if (dist < minDist) {
                        minDist = dist.toDouble()
                        bestIdx = c
                    }
                }
                assignments[i] = bestIdx
                centroidSumR[bestIdx] = centroidSumR[bestIdx] + r.toLong()
                centroidSumG[bestIdx] = centroidSumG[bestIdx] + g.toLong()
                centroidSumB[bestIdx] = centroidSumB[bestIdx] + b.toLong()
                centroidCount[bestIdx] = centroidCount[bestIdx] + 1
            }

            for (c in centroids.indices) {
                val count = centroidCount[c]
                if (count > 0) {
                    val avgR = (centroidSumR[c] / count).toInt()
                    val avgG = (centroidSumG[c] / count).toInt()
                    val avgB = (centroidSumB[c] / count).toInt()
                    centroids[c] = (0xFF shl 24) or (avgR shl 16) or (avgG shl 8) or avgB
                }
            }
        }

        val quantizedPixels = IntArray(pixels.size)
        for (i in pixels.indices) {
            quantizedPixels[i] = centroids[assignments[i]]
        }

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(quantizedPixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }

    private fun overlayArabicTextOnBitmap(context: Context, bitmap: Bitmap, text: String): Bitmap {
        if (text.isEmpty()) return bitmap

        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(result)

        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = (bitmap.height * 0.09f)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val outlinePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = (bitmap.height * 0.09f)
            textAlign = android.graphics.Paint.Align.CENTER
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 6f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        try {
            val assetManager = context.assets
            val typeface = android.graphics.Typeface.createFromAsset(assetManager, "fonts/NotoNaskhArabic-Regular.ttf")
            paint.typeface = typeface
            outlinePaint.typeface = typeface
        } catch (e: Exception) {
            // Fallback
        }

        val x = bitmap.width / 2f
        val y = bitmap.height * 0.85f

        canvas.drawText(text, x, y, outlinePaint)
        canvas.drawText(text, x, y, paint)

        return result
    }

    fun exportPixelatedImage(context: Context): String {
        val bitmap = pixelatedBitmap.value ?: return "لا توجد صورة للتصدير."
        return try {
            val fileName = "retro_pixel_art_${System.currentTimeMillis()}.png"
            val targetFile = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), fileName)
            FileOutputStream(targetFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            "تم تصدير الصورة بنجاح إلى: ${targetFile.absolutePath}"
        } catch (e: Exception) {
            "فشل تصدير الصورة: ${e.localizedMessage}"
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    override fun onCleared() {
        super.onCleared()
        localHDEngine.close()
    }

    private fun getActiveFrameCount(): Int {
        return when (val state = _uiState.value) {
            is UiState.Success -> state.data.frameCount
            else -> 4 // Fallback matching initial template size
        }
    }
}
