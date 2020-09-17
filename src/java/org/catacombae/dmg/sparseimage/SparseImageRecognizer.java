/*-
 * Copyright (C) 2014 Erik Larsson
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

package org.catacombae.dmg.sparseimage;

import org.catacombae.io.ReadableRandomAccessStream;
import org.catacombae.util.Util;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
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
