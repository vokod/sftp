package com.awolity.secftp.ssh;

import com.hierynomus.sshj.signature.SignatureEdDSA;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.signature.SignatureECDSA;
import net.schmizz.sshj.transport.random.JCERandom;
import net.schmizz.sshj.transport.random.SingletonRandomFactory;

/**
 * Registers SpongyCastle as JCE provider.
 */
public class AndroidConfig
        extends DefaultConfig {

    static {
        SecurityUtils.registerSecurityProvider("org.spongycastle.jce.provider.BouncyCastleProvider");
    }

    // don't add ECDSA
    protected void initSignatureFactories() {
        setSignatureFactories(/*new SignatureRSA.Factory(),*/
               /* new SignatureDSA.Factory(),*/
                // but add EdDSA
                new SignatureECDSA.Factory256(),
                new SignatureEdDSA.Factory());
    }

    @Override
    protected void initRandomFactory(boolean ignored) {
        setRandomFactory(new SingletonRandomFactory(new JCERandom.Factory()));
    }

}