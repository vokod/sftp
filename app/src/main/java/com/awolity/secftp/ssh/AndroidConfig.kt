package com.awolity.secftp.ssh

import com.hierynomus.sshj.signature.SignatureEdDSA
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.signature.SignatureDSA
import net.schmizz.sshj.signature.SignatureECDSA
import net.schmizz.sshj.signature.SignatureRSA
import net.schmizz.sshj.transport.random.JCERandom
import net.schmizz.sshj.transport.random.SingletonRandomFactory

/**
 * Registers SpongyCastle as JCE provider.
 */
class AndroidConfig : DefaultConfig() {

    // don't add ECDSA
    override fun initSignatureFactories() {
        setSignatureFactories( SignatureRSA.Factory(),
            SignatureDSA.Factory(),
            // but add EdDSA
            SignatureECDSA.Factory256(),
            SignatureEdDSA.Factory()
        )
    }

    override fun initRandomFactory(ignored: Boolean) {
        randomFactory = SingletonRandomFactory(JCERandom.Factory())
    }

    companion object {
        init {
            SecurityUtils.registerSecurityProvider("org.spongycastle.jce.provider.BouncyCastleProvider")
        }
    }
}