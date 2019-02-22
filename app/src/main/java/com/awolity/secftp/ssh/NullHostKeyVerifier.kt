package com.awolity.secftp.ssh

import net.schmizz.sshj.transport.verification.HostKeyVerifier

import java.security.PublicKey

class NullHostKeyVerifier : HostKeyVerifier {
    /*
     * This method is used to bypass HostKeyVerification.
     * It returns true for whatever the input is.
     *
     */
    override fun verify(arg0: String, arg1: Int, arg2: PublicKey): Boolean {
        return true
    }

    companion object {
        private val TAG = "NullHostKeyVerifier"
    }
}
