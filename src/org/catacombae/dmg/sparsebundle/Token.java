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

import java.io.IOException;
import org.catacombae.io.RuntimeIOException;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
class Token extends BundleMember {

    private final long size;

    public Token(FileAccessor tokenFile, boolean tokenFileLocked) {
        super(tokenFile, tokenFileLocked);

        try {
            this.size = this.stream.length();
        } catch(RuntimeIOException ex) {
            final IOException cause = ex.getIOCause();

            if(cause != null) {
                throw new RuntimeIOException("Exception while getting size " +
                        "of 'token' file.", cause);
            }

            throw ex;
        }
    }

    public final long getSize() {
        return this.size;
    }

    public final int read(final long pos, final byte[] data, final int offset,
            final int length) throws RuntimeIOException
    {
        this.stream.seek(pos);

        return this.stream.read(data, offset, length);
    }
}
