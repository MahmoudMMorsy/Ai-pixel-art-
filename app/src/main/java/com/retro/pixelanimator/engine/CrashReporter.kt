package com.retro.pixelanimator.engine

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Handles automatic on-device crash tracking and local error storage
 * to help developers easily locate JNI or model failures.
 */
object CrashReporter {
    private const val TAG = "CrashReporter"
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext

        // Setup automatic uncaught exception handler to prevent silent failures
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            saveCrashLog(throwable)
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
