/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.catacombae.dmg.sparsebundle;

import java.io.IOException;
import org.catacombae.io.ReadableRandomAccessStream;
import org.catacombae.io.RuntimeIOException;

/**
 *
 * @author erik
 */
abstract class BundleMember {

    /* Backing store. */
    private final FileAccessor file;
    protected final ReadableRandomAccessStream stream;

    public BundleMember(FileAccessor file) {
        this.file = file;
        this.stream = file.createReadableStream();
    }

    public void close() {
        stream.close();

        try {
            file.unlock();
        } catch(RuntimeIOException ex) {
            final IOException cause = ex.getIOCause();

            if(cause != null) {
                ex.printStackTrace();
            }
            else {
                throw ex;
            }
        }

        try {
            file.close();
        } catch(RuntimeIOException ex) {
            final IOException cause = ex.getIOCause();
            if(cause != null) {
                ex.printStackTrace();
            }
            else {
                throw ex;
            }
        }
    }

}
