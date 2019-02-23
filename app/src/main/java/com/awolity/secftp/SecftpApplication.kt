package com.awolity.secftp

import android.app.Application
import com.awolity.secftp.utils.isOnlyTrustedServersSet
import com.awolity.secftp.utils.knownHostsFileExist
import com.awolity.secftp.utils.setOnlyTrustedServers
import java.security.Security

class SecftpApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)

        if (!knownHostsFileExist(this) && !isOnlyTrustedServersSet(
                this
            )
        ) {
            // if there is no known_hosts file and OnlyTrustedServer setting is not yet set, than be promiscous ;)
            setOnlyTrustedServers(this, false)
        }
    }
}