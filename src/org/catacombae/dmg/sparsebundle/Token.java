/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.catacombae.dmg.sparsebundle;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import org.catacombae.io.RuntimeIOException;

/**
 *
 * @author erik
 */
class Token extends BundleMember {

    private final long size;

    public Token(RandomAccessFile tokenFile, FileLock tokenFileLock) {
        super(tokenFile, tokenFileLock);

        try {
            this.size = this.file.length();
        } catch(IOException e) {
            throw new RuntimeIOException("Exception while getting size of " +
                    "'token' file.", e);
        }
    }

    public final long getSize() {
        return this.size;
    }

    public final int read(final long pos, final byte[] data, final int offset,
            final int length) throws IOException
    {
        this.file.seek(pos);

        return this.file.read(data, offset, length);
    }
}
