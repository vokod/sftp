package com.awolity.secftp

import android.app.Application
import java.security.Security

class SecftpApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
    }
}