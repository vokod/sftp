package com.awolity.secftp.utils

import android.content.Context
import java.io.File

fun deleteFileIfExist(context: Context, name: String) {
    if (name.isNotEmpty() && fileExist(context, name)) {
        deleteFile(context, name)
    }
}

fun fileExist(context: Context, name: String): Boolean {
    return (File(context.filesDir, name).exists())
}

fun deleteFile(context: Context, name: String): Boolean {
    return File(context.filesDir, name).delete()
}

fun saveFile(context: Context, file: File, newName: String): File? {
    var resultFile: File? = null
    try {
        val filesDir = context.filesDir
        resultFile = file.copyTo(File(filesDir, newName), overwrite = true)
    } catch (e: Exception) {
        MyLog.e(TAG, "saveFile - exception: $e", e)
    } finally {
        return resultFile
    }
}

private const val TAG = "IoUtils"