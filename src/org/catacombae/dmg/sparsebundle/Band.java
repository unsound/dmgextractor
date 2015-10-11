/*-
 * Copyright (C) 2011-2014 Erik Larsson
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

package org.catacombae.dmg.sparsebundle;

import java.util.Arrays;
import org.catacombae.io.RuntimeIOException;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
class Band extends BundleMember {
    private final long bandActualSize;
    private final long bandVirtualSize;

    public Band(FileAccessor tokenFile, boolean bandFileLocked, long bandSize)
    {
        super(tokenFile, bandFileLocked);

        this.bandVirtualSize = bandSize;
        try {
            this.bandActualSize = tokenFile.length();
        } catch(RuntimeIOException ex) {
            super.close();
            throw ex;
        }
    }

    public int read(long offset, byte[] dest, int destOffset, int destLength)
            throws RuntimeIOException {
        if(offset < 0)
            throw new IllegalArgumentException("negative offset.");
        if(dest == null)
            throw new IllegalArgumentException("dest is null.");
        if(destOffset < 0 || destOffset > dest.length)
            throw new IllegalArgumentException("destOffset out of range.");
        if(destLength < 0 || destLength > (dest.length - destOffset))
            throw new IllegalArgumentException("destLength out of range.");

        if(offset >= bandVirtualSize)
            return -1;

        final int readLength;
        if(destLength > bandVirtualSize - offset)
            readLength = (int) (bandVirtualSize - offset);
        else
            readLength = destLength;

        final int actualLength;
        if(offset >= bandActualSize)
            actualLength = 0;
        else {
            long remainingActualBytes = bandActualSize - offset;
            if(readLength > remainingActualBytes)
                actualLength = (int) remainingActualBytes;
            else
                actualLength = readLength;
        }

        if(actualLength > 0) {
            this.stream.seek(offset);
            int bytesRead = this.stream.read(dest, destOffset, actualLength);
            if(bytesRead != actualLength)
                return bytesRead;
        }

        if(actualLength != readLength) {
            Arrays.fill(dest, destOffset + actualLength,
                    destOffset + readLength, (byte) 0);
        }

        return readLength;
    }
}
