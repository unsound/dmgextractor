/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.catacombae.dmg.sparsebundle;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

/**
 *
 * @author erik
 */
abstract class BundleMember {

    /* Backing store. */
    protected RandomAccessFile file;
    protected FileLock fileLock;

    public BundleMember(RandomAccessFile file, FileLock fileLock) {
        this.file = file;
        this.fileLock = fileLock;
    }

    public void close() {
        try {
            fileLock.release();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

        try {
            file.close();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

}
