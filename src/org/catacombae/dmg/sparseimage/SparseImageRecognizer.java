/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.catacombae.dmg.sparseimage;

import org.catacombae.io.ReadableRandomAccessStream;
import org.catacombae.util.Util;

/**
 *
 * @author erik
 */
public class SparseImageRecognizer {
    public static boolean isSparseImage(final ReadableRandomAccessStream s) {
        byte[] headerData = new byte[4096];
        s.seek(0);
        if(s.read(headerData) != 4096) {
            return false;
        }

        SparseImageHeader header = new SparseImageHeader(headerData, 0);
        if(Util.readString(header.getSignature(), "US-ASCII").
                equals("sprs"))
        {
            return true;
        }

        return false;
    }
}
