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

fun getOnlyTrustedServers(context: Context): Boolean {
    return Yapel.get(YAPEL_KEY, context).getBoolean(KEY_ONLY_TRUSTED_SERVERS, true)
}

fun setOnlyTrustedServers(context: Context, setting: Boolean) {
    Yapel.get(YAPEL_KEY, context).setBoolean(KEY_ONLY_TRUSTED_SERVERS, setting)
}

fun isHostKnown(context: Context, hostname: String): Boolean {
    if (!knownHostsFileExist(context)) return false
    val matchingHosts = readKnownHostsFile(File(context.filesDir, KNOWN_HOSTS_FILE_NAME))
        .filter { it.startsWith(hostname) }
    return !matchingHosts.isEmpty()
}

fun knownHostsFileExist(context: Context): Boolean {
    return File(context.filesDir, KNOWN_HOSTS_FILE_NAME).exists()
}

fun getKnownHostsFile(context: Context): File {
    return File(context.filesDir, KNOWN_HOSTS_FILE_NAME)
}

private fun readKnownHostsFile(knownHostsFile: File): MutableList<String> {
    val result = mutableListOf<String>()
    val reader: BufferedReader
    try {
        val inputStream = knownHostsFile.inputStream()
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

fun importKnownHostsFile(context: Context, newFile: File) {
    if (knownHostsFileExist(context)) { //known hosts file exist, append new entries
        val importedKnownHostsFile = File(context.filesDir, KNOWN_HOSTS_FILE_NAME)
        val oldKnownHosts = readKnownHostsFile(importedKnownHostsFile)
        val newKnownHosts = readKnownHostsFile(newFile)
        newKnownHosts.forEach { newKnownHost ->
            if (oldKnownHosts.none { it.startsWith(newKnownHost.substringBefore(' ')) }) {
                oldKnownHosts.add(newKnownHost)
            }
        }
        importedKnownHostsFile.writeText(oldKnownHosts.joinToString("\n"))
    } else { // known hosts file does not exist, simply copy the new file
        newFile.copyTo(File(context.filesDir, KNOWN_HOSTS_FILE_NAME))
    }
}

const val TAG = "SettingsUtils"
