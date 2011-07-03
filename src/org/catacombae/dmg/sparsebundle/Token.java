/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.catacombae.dmg.sparsebundle;

import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

/**
 *
 * @author erik
 */
class Token extends BundleMember {

    public Token(RandomAccessFile tokenFile, FileLock tokenFileLock) {
        super(tokenFile, tokenFileLock);
    }
}
