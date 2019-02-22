package com.awolity.secftp

import android.content.Context
import android.util.Log
import com.awolity.yapel.Yapel
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader


const val YAPEL_KEY = "YAPEL_KEY"
const val KEY_ONLY_TRUSTED_SERVERS = "trusted server setting"
const val KEY_DEFAULT_AUTH_METHOD = "default auth method"

fun getOnlyTrustedServers(context: Context): Boolean {
    return Yapel.get(YAPEL_KEY, context).getBoolean(KEY_ONLY_TRUSTED_SERVERS, true)
}

fun setOnlyTrustedServers(context: Context, setting: Boolean) {
    Yapel.get(YAPEL_KEY, context).setBoolean(KEY_ONLY_TRUSTED_SERVERS, setting)
}

fun getDefaultAuthenticationMethod(context: Context): Boolean {
    return Yapel.get(YAPEL_KEY, context).getBoolean(KEY_DEFAULT_AUTH_METHOD, true)
}

fun setDefaultAuthenticationMethod(context: Context, setting: Boolean) {
    Yapel.get(YAPEL_KEY, context).setBoolean(KEY_DEFAULT_AUTH_METHOD, setting)
}

fun isHostKnown(context: Context, hostname: String): Boolean {
    if (!knownHostsFileExist(context)) {
        return false
    }
    val matchingHosts = readKnownHostsFile(context).filter { it.startsWith(hostname) }
    return !matchingHosts.isEmpty()
}

fun knownHostsFileExist(context: Context): Boolean {
    return File(context.filesDir, KNOWN_HOSTS_FILE_NAME).exists()
}

private fun readKnownHostsFile(context: Context): List<String> {
    val result = mutableListOf<String>()
    val reader: BufferedReader
    try {
        val inputStream = File(context.filesDir, KNOWN_HOSTS_FILE_NAME).inputStream()
        reader = BufferedReader(InputStreamReader(inputStream))
        var line = reader.readLine()
        while (line != null) {
            result.add(line)
            line = reader.readLine()
        }
    } catch (ioe: IOException) {
        Log.e(TAG, "readKnownHostsFile - IOException: ${ioe.localizedMessage}")
    }
    return result
}

const val TAG = "SettingsUtils"
