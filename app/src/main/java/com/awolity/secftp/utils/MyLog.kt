package com.awolity.secftp.utils

import android.annotation.SuppressLint
import com.awolity.secftp.BuildConfig
import timber.log.Timber

@SuppressLint("LogNotTimber")
object MyLog {

    private val LOG = BuildConfig.DEBUG

    fun i(tag: String, string: String) {
        if (LOG) {
            android.util.Log.i(tag, string)
            //Timber.i("$tag: $string")
        }
    }

    fun d(tag: String, string: String) {
        if (LOG) {
            android.util.Log.d(tag, string)
            //Timber.d("$tag: $string")
        }
    }

    fun e(tag: String, string: String, e: Throwable) {
        if (LOG) {
            android.util.Log.e(tag, string)
            //Timber.e(e, "$tag: $string")
        }
    }

    fun v(tag: String, string: String) {
        if (LOG) {
            android.util.Log.v(tag, string)
            //Timber.v("$tag: $string")
        }
    }

    fun w(tag: String, string: String) {
        if (LOG) {
            android.util.Log.w(tag, string)
           // Timber.w("$tag: $string")
        }
    }

    fun wtf(tag: String, string: String) {
        if (LOG) {
            android.util.Log.wtf(tag, string)
           // Timber.wtf("$tag: $string")
        }
    }
}
