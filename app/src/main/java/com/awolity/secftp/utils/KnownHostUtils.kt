package com.awolity.secftp.utils

import android.content.Context
import com.awolity.secftp.view.knownhosts.KnownHost
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

fun isHostKnown(context: Context, hostname: String): Boolean {
    if (!knownHostsFileExist(context)) return false
    val matchingHosts = readKnownHostsFile(
        File(
            context.filesDir,
            KNOWN_HOSTS_FILE_NAME
        )
    )
        .filter { it.startsWith(hostname) }
    return matchingHosts.isNotEmpty()
}

fun knownHostsFileExist(context: Context): Boolean {
    return File(context.filesDir, KNOWN_HOSTS_FILE_NAME).exists()
}

fun getKnownHostsFile(context: Context): File {
    return File(context.filesDir, KNOWN_HOSTS_FILE_NAME)
}

fun readKnownHostsFile(knownHostsFile: File): MutableList<String> {
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
        MyLog.e(TAG, "readKnownHostsFile - IOException: ${ioe.localizedMessage}", ioe)
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

fun writeKnownHostFile(context: Context, knownHosts: List<KnownHost>) {
    val file = File(context.filesDir, KNOWN_HOSTS_FILE_NAME)
    file.delete()
    file.createNewFile()
    var prefix = ""
    val builder = StringBuilder()
    knownHosts.forEach {
        builder.append(prefix + it.address + " " + it.type + " " + it.key)
        prefix = "\n"
    }
    file.writeText(builder.toString())
}

private const val TAG = "KnownHostUtils"