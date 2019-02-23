package com.awolity.secftp.utils

import android.content.Context
import com.awolity.yapel.Yapel

const val YAPEL_KEY = "YAPEL_KEY"
const val KEY_ONLY_TRUSTED_SERVERS = "trusted server setting"
const val KEY_SHOW_HIDDEN_FILES = "show hidden files"

fun getOnlyTrustedServers(context: Context): Boolean {
    return Yapel.get(YAPEL_KEY, context).getBoolean(KEY_ONLY_TRUSTED_SERVERS, true)
}

fun setOnlyTrustedServers(context: Context, setting: Boolean) {
    Yapel.get(YAPEL_KEY, context).setBoolean(KEY_ONLY_TRUSTED_SERVERS, setting)
}

fun isOnlyTrustedServersSet(context: Context): Boolean {
    return Yapel.get(YAPEL_KEY, context).contains(KEY_ONLY_TRUSTED_SERVERS)
}

fun getShowHiddenFiles(context: Context): Boolean {
    return Yapel.get(YAPEL_KEY, context).getBoolean(KEY_SHOW_HIDDEN_FILES, true)
}

fun setShowHiddenFiles(context: Context, setting: Boolean) {
    Yapel.get(YAPEL_KEY, context).setBoolean(KEY_SHOW_HIDDEN_FILES, setting)
}


