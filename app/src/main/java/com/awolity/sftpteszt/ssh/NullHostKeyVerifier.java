package com.awolity.sftpteszt.ssh;

import android.util.Log;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import org.spongycastle.asn1.eac.ECDSAPublicKey;
import org.spongycastle.jcajce.provider.asymmetric.rsa.RSAUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

public class NullHostKeyVerifier implements HostKeyVerifier {

    private static final String TAG = "NullHostKeyVerifier";
    /*
     * This method is used to bypass HostKeyVerification.
     * It returns true for whatever the input is.
     *
     */
    @Override
    public boolean verify(String arg0, int arg1, PublicKey arg2) {
        Log.d(TAG, "verify() called with: arg0 = [" + arg0 + "], arg1 = [" + arg1 + "], arg2 = [" + arg2 + "]");
        Log.d(TAG, "algorithm: "+ arg2.getAlgorithm());
        Log.d(TAG, "fingerprint: "+ toHexString(arg2.getEncoded()));
        //Log.d(TAG, "fingerprint2: "+ getFingerprint((RSAPublicKey)arg2));
        Log.d(TAG, "format: "+ arg2.getFormat());
        return true;
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder("[");

        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
            if(i<bytes.length-1){
                hexString.append(":");
            }
        }

        hexString.append("]");
        return hexString.toString();
    }

    private static String getFingerprintRsa(RSAPublicKey rsapubkey){
        byte[] n = rsapubkey.getModulus().toByteArray(); // Java is 2sC bigendian
        byte[] e = rsapubkey.getPublicExponent().toByteArray(); // and so is SSH
        byte[] tag = "ssh-rsa".getBytes(); // charset very rarely matters here
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(os);
        try {
            dos.writeInt(tag.length);
            dos.write(tag);
            dos.writeInt(e.length); dos.write(e);
            dos.writeInt(n.length); dos.write(n);
        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }

        byte[] encoded = os.toByteArray();
        // now hash that (you don't really need Apache)
        // assuming SHA256-base64 (see below)
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA256");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
            return null;
        }
        byte[] result = digest.digest(encoded);
        return Base64.getEncoder().encodeToString(result);
    }
}
