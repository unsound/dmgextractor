/*-
 * Copyright (C) 2006 Erik Larsson
 * 
 * All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.catacombae.dmgx;

import org.catacombae.io.*;
import java.io.*;

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
	
	DmgFileView dfw = new DmgFileView(new RandomAccessFileStream(inFile));
	byte[] plistData = dfw.getPlist().getData();
	/*int bytesWritten = */outStream.write(plistData);
// 	if(bytesWritten != plistData.length)
// 	    System.out.println("ERROR: Could not write all data to output file. " + bytesWritten + " of " + plistData.length + " bytes written.");

	inFile.close();
	outStream.close();
    }
}
