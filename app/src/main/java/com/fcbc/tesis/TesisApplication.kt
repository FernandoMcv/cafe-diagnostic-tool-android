package com.fcbc.tesis

import android.app.Application
import com.fcbc.tesis.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

import android.content.Intent
import android.util.Log

class TesisApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Capturador global de errores para mostrar pantalla con StackTrace completo en lugar de "se detuvo"
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("TesisApplication", "UNCAUGHT EXCEPTION DETECTED", throwable)
            try {
                val stackTraceString = Log.getStackTraceString(throwable)
                val intent = Intent(this, CrashActivity::class.java).apply {
                    putExtra("error_details", stackTraceString)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(1)
            } catch (e: Exception) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        startKoin {
            androidContext(this@TesisApplication)
            modules(appModule)
        }
    }
}