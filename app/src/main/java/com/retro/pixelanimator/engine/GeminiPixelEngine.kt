package com.retro.pixelanimator.engine

import android.util.Log
import com.retro.pixelanimator.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini REST API Request & Response Schema ---

data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String
)

data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null,
    val imageConfig: ImageConfig? = null,
    val responseModalities: List<String>? = null
)

data class ImageConfig(
    val aspectRatio: String = "1:1",
    val imageSize: String = "1K"
)

data class GenerateContentResponse(
    val candidates: List<Candidate>
)

data class Candidate(
    val content: ContentResponse
)

data class ContentResponse(
    val parts: List<PartResponse>
)

data class PartResponse(
    val text: String? = null,
    val inlineData: InlineData? = null
)

// --- Our custom target JSON Schema from Gemini ---

data class PixelArtAnimationResponse(
    val title: String,
    val description: String,
    val palette: List<String>, // Hex values: #RRGGBB
    val frameCount: Int,
    val frames: List<String>   // Each string has length 256 for a 16x16 grid
)

// --- Retrofit API Service ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @POST("v1beta/models/gemini-2.5-flash-image:generateContent")
    suspend fun generateImageContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(GeminiApiService::class.java)
    }
}

class GeminiPixelEngine {
    private val gson = Gson()

    suspend fun generatePixelAnimation(
        prompt: String,
        frameCount: Int,
        paletteHint: String
    ): PixelArtAnimationResponse {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
            throw Exception("رابط واجهة برمجة تطبيقات Gemini (API Key) مفقود! يرجى تكوين مفتاح GEMINI_API_KEY في لوحة الأسرار (Secrets Panel) في AI Studio.")
        }

        // Configure system instruction to guide Gemini to act as a pure pixel-art JSON synthesizer
        val systemInstruction = Content(
            parts = listOf(
                Part(
                    text = """
                    You are an expert retro pixel-art animator and visual sprite generator.
                    Your sole task is to generate complete frame-by-frame animations for a 16x16 grid.
                    You MUST return a JSON object adhering strictly to the requested structure:
                    
                    {
                      "title": "...",
                      "description": "...",
                      "palette": ["#...", "#..."],
                      "frameCount": X,
                      "frames": ["...", "..."]
                    }

                    RULES:
                    1. Generate between 4 to 8 animation frames based on the frameCount provided.
                    2. Provide a custom 'palette' of up to 16 hex color codes (e.g., "#FF0011"). The index 0 color MUST be transparent or background (e.g., "#141218" or black "#000000").
                    3. Each frame inside the 'frames' array MUST be a single flat string of exactly 256 characters (representing a 16x16 grid row-by-row).
                    4. Each character inside the frame string MUST be a hexadecimal index representing the color in your 'palette'.
                       '0' refers to index 0, '1' to index 1, ... '9' to index 9, 'a' to index 10, 'b' to index 11, 'c' to index 12, 'd' to index 13, 'e' to index 14, and 'f' to index 15.
                    5. Ensure true animation progression (e.g., cycling wing beats, running strides, weapon swings, explosion expansion) across successive frames.
                    6. The generated pixel art must strictly match the style requested in the user prompt (such as classic RPG characters, Spaceships, fireballs).
                    """.trimIndent()
                )
            )
        )

        // Leverage Structured JSON Output from Gemini via Gson JsonObject
        val schemaJsonObject = JsonObject().apply {
            addProperty("type", "OBJECT")
            val properties = JsonObject()
            
            properties.add("title", JsonObject().apply { addProperty("type", "STRING") })
            properties.add("description", JsonObject().apply { addProperty("type", "STRING") })
            properties.add("palette", JsonObject().apply {
                addProperty("type", "ARRAY")
                add("items", JsonObject().apply { addProperty("type", "STRING") })
            })
            properties.add("frameCount", JsonObject().apply { addProperty("type", "INTEGER") })
            properties.add("frames", JsonObject().apply {
                addProperty("type", "ARRAY")
                add("items", JsonObject().apply { addProperty("type", "STRING") })
            })
            
            add("properties", properties)
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(
                            text = "Please generate a pixel art animation for: '$prompt'. Frame Count requested: $frameCount. Preferred color palette theme or hint: $paletteHint."
                        )
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = schemaJsonObject,
                temperature = 0.4f
            ),
            systemInstruction = systemInstruction
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("قاعدة نموذج الذكاء الاصطناعي لم تعيد أي محتوى توليدي.")

            Log.d("GeminiPixelEngine", "Received JSON: $jsonText")

            // Parse response json via Gson
            return gson.fromJson(jsonText, PixelArtAnimationResponse::class.java)
        } catch (e: Exception) {
            Log.e("GeminiPixelEngine", "Error calling Gemini:", e)
            throw e
        }
    }

    suspend fun generateRealImage(prompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
            throw Exception("رابط واجهة برمجة تطبيقات Gemini (API Key) مفقود! يرجى تكوين مفتاح GEMINI_API_KEY في لوحة الأسرار (Secrets Panel) في AI Studio.")
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt)
                    )
                )
            ),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(aspectRatio = "1:1", imageSize = "1K"),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )

        try {
            val response = RetrofitClient.service.generateImageContent(apiKey, request)
            val base64Data = response.candidates.firstOrNull()?.content?.parts?.safeGetInlineData()
                ?: throw Exception("نموذج توليد الصور لم يُرجع أي بيانات. يرجى التحقق من صياغة المبرومبت أو مفتاح الـ API.")

            return base64Data
        } catch (e: Exception) {
            Log.e("GeminiPixelEngine", "Error calling Gemini image generation:", e)
            throw e
        }
    }
}

// Extension to safely extract first inlineData in parts list
private fun List<PartResponse>.safeGetInlineData(): String? {
    for (part in this) {
        if (part.inlineData != null) {
            return part.inlineData.data
        }
    }
    return null
}
