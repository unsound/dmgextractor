/*-
 * Copyright (C) 2006 Erik Larsson
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

package org.catacombae.dmgextractor.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import org.catacombae.io.ReadableFileStream;
import org.catacombae.dmg.udif.UDIFFileView;

public class ExtractPlist {

    public static void main(String[] args) throws IOException {
        RandomAccessFile inFile;
        OutputStream outStream;
        try {
            inFile = new RandomAccessFile(args[0], "r");
            outStream = new FileOutputStream(args[1]);
        } catch(ArrayIndexOutOfBoundsException aioobe) {
            System.out.println("Usage: ExtractPlist <dmgFile> <xml output file>");
            return;//System.exit(0);
        }

        UDIFFileView dfw = new UDIFFileView(new ReadableFileStream(inFile));
        byte[] plistData = dfw.getPlistData();
        /*int bytesWritten = */        outStream.write(plistData);
// 	if(bytesWritten != plistData.length)
// 	    System.out.println("ERROR: Could not write all data to output file. " + bytesWritten + " of " + plistData.length + " bytes written.");

        inFile.close();
        outStream.close();
    }
}
