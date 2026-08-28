package com.lifelensiq.app

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.lifelensiq.app.di.ServiceLocator
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LifeLensIQApp : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashHandler()
        ServiceLocator.init(this)
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                sw.append("Crash captured at ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
                sw.append("Thread: ${thread.name}\n\n")
                throwable.printStackTrace(PrintWriter(sw))
                val file = File(cacheDir, "crash_log.txt")
                file.writeText(sw.toString())
            } catch (_: Throwable) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        fun readCrashLog(app: Application): String? {
            return try {
                val file = File(app.cacheDir, "crash_log.txt")
                if (file.exists()) file.readText() else null
            } catch (_: Throwable) {
                null
            }
        }

        fun clearCrashLog(app: Application) {
            try {
                File(app.cacheDir, "crash_log.txt").delete()
            } catch (_: Throwable) {
            }
        }
    }
}
