package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.GeminiPixelEngine
import com.example.engine.LocalPixelEngine
import com.example.engine.PixelArtAnimationResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val data: PixelArtAnimationResponse) : UiState
    data class RealImageSuccess(val base64Data: String) : UiState
    data class Error(val message: String) : UiState
}

class PixelAnimatorViewModel : ViewModel() {
    private val geminiEngine = GeminiPixelEngine()
    private val localEngine = LocalPixelEngine()

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

    // --- UI/API State ---
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // --- Mode Control (Pixel Art vs Real HD Image Mode) ---
    val isRealImageMode = MutableStateFlow(false)
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

            // Simulate slight delay to make the offline calculation feel like a high-end local AI model inference
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
                val base64 = geminiEngine.generateRealImage(prompt.value)
                _realImageBase64.value = base64
                _uiState.value = UiState.RealImageSuccess(base64)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "حدث خطأ أثناء توليد الصورة الواقعية. يرجى التأكد من أن مفتاح الـ API مضاف بشكل صحيح.")
            }
        }
    }

    private fun getActiveFrameCount(): Int {
        return when (val state = _uiState.value) {
            is UiState.Success -> state.data.frameCount
            else -> 4 // Fallback matching initial template size
        }
    }
}
