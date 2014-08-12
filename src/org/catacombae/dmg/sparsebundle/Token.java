/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.catacombae.dmg.sparsebundle;

import java.io.IOException;
import org.catacombae.io.RuntimeIOException;

/**
 *
 * @author erik
 */
class Token extends BundleMember {

    private final long size;

    public Token(FileAccessor tokenFile) {
        super(tokenFile);

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
