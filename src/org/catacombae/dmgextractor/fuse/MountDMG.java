/*-
 * Copyright (C) 2014 Erik Larsson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catacombae.dmgextractor.fuse;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import org.catacombae.dmg.encrypted.ReadableCEncryptedEncodingStream;
import org.catacombae.dmg.sparsebundle.ReadableSparseBundleStream;
import org.catacombae.dmg.sparseimage.ReadableSparseImageStream;
import org.catacombae.dmg.sparseimage.SparseImageRecognizer;
import org.catacombae.dmg.udif.UDIFDetector;
import org.catacombae.dmg.udif.UDIFFile;
import org.catacombae.dmgextractor.TextModeUI;
import org.catacombae.io.ReadableFileStream;
import org.catacombae.io.ReadableRandomAccessStream;
import org.catacombae.io.RuntimeIOException;
import org.catacombae.jfuse.FUSE;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public class MountDMG {
    private static final TextModeUI ui = new TextModeUI(false);

    private static void printUsage(PrintStream ps) {
        ps.println("usage: mount_dmg <file> <mountpoint> [<FUSE options>]");
    }

    public static void main(String[] args) throws IOException {
        if(args.length < 2) {
            printUsage(System.err);
            System.exit(1);
            return;
        }

        final File dmgFile = new File(args[0]);

        ReadableRandomAccessStream dmgStream = null;

        final boolean sparseBundle;
        if(dmgFile.isDirectory()) {
            ReadableSparseBundleStream sbStream = null;
            try {
                sbStream = new ReadableSparseBundleStream(dmgFile);
            } catch(RuntimeIOException e) {
                /* Not a sparse bundle, apparently. */
            }

            if(sbStream != null) {
                dmgStream = sbStream;
                sparseBundle = true;
            }
            else {
                sparseBundle = false;
            }
        }
        else {
            sparseBundle = false;
        }

        if(dmgStream == null) {
            dmgStream = new ReadableFileStream(dmgFile);
        }

        final boolean encrypted;
        if(ReadableCEncryptedEncodingStream.isCEncryptedEncoding(dmgStream)) {
            encrypted = true;
            char[] password;
            while(true) {
                password = ui.getPasswordFromUser();
                if(password == null) {
                    ui.displayMessage("No password specified. Can not " +
                            "continue...");
                    dmgStream.close();
                    System.exit(1);
                    return;
                }
                try {
                    ReadableCEncryptedEncodingStream encryptionFilter =
                            new ReadableCEncryptedEncodingStream(dmgStream,
                            password);
                    dmgStream = encryptionFilter;
                    break;
                } catch(Exception e) {
                    ui.displayMessage("Incorrect password!");
                }
            }
        }
        else {
            encrypted = false;
        }

        boolean sparseImage = false;
        if(!sparseBundle && SparseImageRecognizer.isSparseImage(dmgStream)) {
            ReadableSparseImageStream sparseImageStream =
                    new ReadableSparseImageStream(dmgStream);
            dmgStream = sparseImageStream;
            sparseImage = true;
        }

        final DMGFileSystem fs;
        if(UDIFDetector.isUDIFEncoded(dmgStream)) {
            UDIFFile udifFile = new UDIFFile(dmgStream);
            fs = new DMGFileSystem(udifFile);
        }
        else {
            if(!sparseBundle && !encrypted && !sparseImage) {
                System.err.println("Warning: The image you selected does not " +
                        "seem to be UDIF encoded, sparse or encrypted.");
                System.err.println("Its contents will be exposed unchanged " +
                        "through the filesystem mount point.");
            }

            fs = new DMGFileSystem(dmgStream);
        }

        LinkedList<String> fuseArgs = new LinkedList<String>();
        for(int i = 1; i < args.length; ++i) {
            fuseArgs.add(args[i]);
        }

        /* DMG access is read only at the moment. */
        fuseArgs.add("-oro");

        FUSE.main(fuseArgs.toArray(new String[fuseArgs.size()]), fs);

        System.exit(0);
    }
}
