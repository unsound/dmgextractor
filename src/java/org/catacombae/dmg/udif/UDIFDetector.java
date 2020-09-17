/*-
 * Copyright (C) 2007 Erik Larsson
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

package org.catacombae.dmg.udif;

import java.io.RandomAccessFile;
import org.catacombae.io.ReadableFileStream;
import org.catacombae.io.ReadableRandomAccessStream;
import org.catacombae.io.RuntimeIOException;

/**
 * Contains a few static utility methods for easily detecting an UDIF encoded
 * disk image.
 *
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public class UDIFDetector {
    /**
     * Convenience method. Equivalent to
     * <code>isUDIFEncoded(new ReadableFileStream(raf));</code>.
     */
    public static boolean isUDIFEncoded(RandomAccessFile raf, String openPath)
            throws RuntimeIOException
    {
        return isUDIFEncoded(new ReadableFileStream(raf, openPath));
    }

    /**
     * Searches through the supplied RandomAccessStream for signature data that validates
     * the data as UDIF encoded.
     * @throws RuntimeIOException on I/O error
     */
    public static boolean isUDIFEncoded(ReadableRandomAccessStream ras) throws RuntimeIOException {
        if(ras.length() < 512)
            return false;

        byte[] kolyData = new byte[Koly.length()];
        ras.seek(ras.length() - 512);
        if(ras.read(kolyData) != kolyData.length)
            throw new RuntimeException("Could not read all koly data...");
        Koly koly = new Koly(kolyData, 0);
        return koly.isValid() &&
                koly.getPlistBegin1() >= 0 && koly.getPlistSize() > 0 &&
                (koly.getPlistBegin1() + koly.getPlistSize()) <= (ras.length() - 512);
    }
}
