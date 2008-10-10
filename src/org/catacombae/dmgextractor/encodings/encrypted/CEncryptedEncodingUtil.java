/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.catacombae.dmgextractor.encodings.encrypted;

import org.catacombae.dmgextractor.Util;
import org.catacombae.io.ReadableRandomAccessStream;

/**
 *
 * @author erik
 */
public class CEncryptedEncodingUtil {
    private static final String V1_SIGNATURE = "cdsaencr";
    private static final String V2_SIGNATURE = "encrcdsa";

    public static int detectVersion(ReadableRandomAccessStream stream) {
        byte[] signatureBytes = new byte[8];
        try {
            stream.seek(0);
            stream.readFully(signatureBytes);
            if(Util.toASCIIString(signatureBytes).equals(V2_SIGNATURE))
                return 2;
        } catch(Exception e) {
            System.err.println("Non-critical exception while detecting version 2" +
                    " CEncryptedEncoding header:");
            e.printStackTrace();
        }

        try {
            stream.seek(stream.length()-signatureBytes.length);
            stream.readFully(signatureBytes);
            if(Util.toASCIIString(signatureBytes).equals(V1_SIGNATURE))
                return 1;
        } catch(Exception e) {
            System.err.println("Non-critical exception while detecting version 1" +
                    " CEncryptedEncoding header:");
            e.printStackTrace();
        }

        return -1;
    }
}
