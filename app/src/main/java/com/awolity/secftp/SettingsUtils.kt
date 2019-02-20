package com.awolity.secftp

import android.content.Context
import com.awolity.yapel.Yapel

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
