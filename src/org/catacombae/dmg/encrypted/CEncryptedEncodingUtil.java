/*-
 * Copyright (C) 2008 Erik Larsson
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catacombae.dmg.encrypted;

import org.catacombae.dmgextractor.Util;
import org.catacombae.io.ReadableRandomAccessStream;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
class CEncryptedEncodingUtil {
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
