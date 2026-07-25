package com.retro.pixelanimator.engine

import android.content.Context
import android.os.Build
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Handles automatic on-device crash tracking, local error storage, and automatic
 * cloud diagnostic reporting to help developers easily locate JNI or model failures.
 */
object CrashReporter {
    private const val TAG = "CrashReporter"
    private val client = OkHttpClient()
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext

        // Setup automatic uncaught exception handler to prevent silent failures
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            saveCrashLog(throwable)
            uploadCrashReportSilently(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Captures and saves complete stack traces with device details to a local file.
     */
    fun saveCrashLog(throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val report = """
            === SYSTEM DIAGNOSTICS & CRASH REPORT ===
            Device Model: ${Build.MANUFACTURER} ${Build.MODEL}
            Android Version: SDK ${Build.VERSION.SDK_INT}
            CPU ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}
            Exception Message: ${throwable.localizedMessage}

            --- STACK TRACE ---
            $stackTrace
        """.trimIndent()

        try {
            appContext?.let { context ->
                val file = File(context.filesDir, "latest_crash_report.txt")
                file.writeText(report)
                Log.i(TAG, "Successfully saved crash log locally to: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log locally: ${e.localizedMessage}")
        }
        return report
    }

    /**
     * Automatically uploads the crash report to an anonymous diagnostic sharing API
     * (such as GoFile / File.io) and prints the shareable debug link to help developers.
     */
    fun uploadCrashReportSilently(throwable: Throwable) {
        val reportText = saveCrashLog(throwable)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Post crash report to an anonymous secure text API (File.io)
                val mediaType = "text/plain".toMediaType()
                val body = reportText.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://file.io")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string() ?: "{}")
                        val link = json.optString("link")
                        Log.i(TAG, "🚨 CRASH UPLOADED SUCCESSFULLY! Direct link to diagnostic report: $link")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send diagnostic report to server: ${e.localizedMessage}")
            }
        }
    }

    fun getLatestCrashReport(): String {
        return try {
            appContext?.let { context ->
                val file = File(context.filesDir, "latest_crash_report.txt")
                if (file.exists()) file.readText() else "لا توجد تقارير أخطاء سابقة."
            } ?: "تعديل المكونات لم يكتمل بعد."
        } catch (e: Exception) {
            "فشل قراءة السجلات: ${e.localizedMessage}"
        }
    }
}
