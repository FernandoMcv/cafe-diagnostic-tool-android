package com.fcbc.tesis.data.repository

import android.content.Context
import com.fcbc.tesis.domain.repository.AppMetricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppMetricsRepositoryImpl(private val context: Context) : AppMetricsRepository {

    override suspend fun getAppSize(): String = withContext(Dispatchers.IO) {
        val cacheSize = getFolderSize(context.cacheDir)
        val filesSize = getFolderSize(context.filesDir)
        
        val totalBytes = cacheSize + filesSize
        val totalMb = totalBytes / (1024.0 * 1024.0)
        
        String.format("%.1f MB", totalMb)
    }

    private fun getFolderSize(dir: File): Long {
        var size: Long = 0
        if (dir.isDirectory) {
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    size += if (file.isFile) file.length() else getFolderSize(file)
                }
            }
        } else if (dir.isFile) {
            size = dir.length()
        }
        return size
    }
}
