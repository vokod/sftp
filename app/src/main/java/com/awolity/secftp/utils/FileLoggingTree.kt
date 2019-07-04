package com.awolity.secftp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.util.Log

import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import timber.log.Timber

class FileLoggingTree(private val context: Context) : Timber.DebugTree() {

    @SuppressLint("LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val folder = File(PATH)

            if (!folder.exists()) {
                folder.mkdir()
            }

            val fileNameTimeStamp = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).format(Date())
            val logTimeStamp = SimpleDateFormat(
                "yyyy-MM-dd 'at' hh:mm:ss:SSS aaa",
                Locale.getDefault()
            ).format(Date())

            val fileName = "$fileNameTimeStamp.html"

            val file = File(
                PATH
                        + File.separator + fileName
            )

            file.createNewFile()

            if (file.exists()) {
                val fileOutputStream = FileOutputStream(file, true)
                fileOutputStream.write("<p style=\"background:lightgray;\"><strong style=\"background:lightblue;\">&nbsp&nbsp$logTimeStamp :&nbsp&nbsp</strong>&nbsp&nbsp$message</p>".toByteArray())
                fileOutputStream.close()

            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while logging into file : $e")
        }

    }

    companion object {

        private val TAG = FileLoggingTree::class.java.simpleName
        private val PATH = Environment.getExternalStorageDirectory().toString() + "/Nojz"
    }
}