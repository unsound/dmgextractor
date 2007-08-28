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
import java.util.zip.*;

/** Please don't try to use this code with concurrent threads... :) It would be easy to synchronize the code
    but i don't wanna. */
public class DMGBlockHandlers {
    private static byte[] inBuffer = new byte[0x40000];
    private static byte[] outBuffer = new byte[0x40000];
    private static Inflater inflater = new Inflater();
    
    /** Extracts a DMGBlock describing a region of the file dmgRaf to the file isoRaf. If the testOnly flag
	is set, nothing is written to isoRaf (in fact, it can be null in this case). ui may not be null. in
	that case, use UserInterface.NullUI. */
    public static long processBlock(DMGBlock block, RandomAccessFile dmgRaf, RandomAccessFile isoRaf, 
				    boolean testOnly, UserInterface ui) throws IOException {
	DMGBlockInputStream is = DMGBlockInputStream.getStream(new RandomAccessFileStream(dmgRaf), block);
	long res = processStream(is, dmgRaf, isoRaf, testOnly, ui);
	is.close();
	if(res != block.getOutSize())
	    System.err.println("WARNING: Could not extract entire block! Extracted " + res + " of " + block.getOutSize() + " bytes");
	return res;
    }
    private static long processStream(DMGBlockInputStream is, RandomAccessFile dmgRaf, RandomAccessFile isoRaf, 
				      boolean testOnly, UserInterface ui) throws IOException {
	long totalBytesRead = 0;
	int bytesRead = is.read(inBuffer);
	while(bytesRead > 0) {
	    totalBytesRead += bytesRead;
	    ui.reportProgress((int)(dmgRaf.getFilePointer()*100/dmgRaf.length()));
	    if(!testOnly) isoRaf.write(inBuffer, 0, bytesRead);
	    bytesRead = is.read(inBuffer);
	}
	return totalBytesRead;
    }
    
//     public static void oldprocessZlibBlock(DMGBlock block, RandomAccessFile dmgRaf, RandomAccessFile isoRaf, 
// 					   boolean testOnly, UserInterface ui) throws IOException, DataFormatException {
// 	inflater.reset();
	
// 	dmgRaf.seek(/*block.lastOffs+*/block.getInOffset());
	
// 	/*
// 	 * medan det finns komprimerat data att läsa:
// 	 *   läs in komprimerat data i inbuffer
// 	 *   medan det finns data kvar att läsa i inbuffer
// 	 *     dekomprimera data från inbuffer till utbuffer
// 	 *     skriv utbuffer till fil
// 	 */
	    
// 	long totalBytesRead = 0;
// 	while(totalBytesRead < block.getInSize()) {
// 	    long bytesRemainingToRead = block.getInSize()-totalBytesRead;
// 	    int curBytesRead = dmgRaf.read(inBuffer, 0, 
// 					   (int)Math.min(bytesRemainingToRead, inBuffer.length));
		
// 	    ui.reportProgress((int)(dmgRaf.getFilePointer()*100/dmgRaf.length()));

// 	    if(curBytesRead < 0)
// 		throw new RuntimeException("Unexpectedly reached end of file. (bytesRemainingToRead=" + bytesRemainingToRead + ", curBytesRead=" + curBytesRead + ", totalBytesRead=" + totalBytesRead + ", block.getInSize()=" + block.getInSize() + ", inBuffer.length=" + inBuffer.length + ")");
// 	    else {
// 		totalBytesRead += curBytesRead;
// 		inflater.setInput(inBuffer, 0, curBytesRead);
// 		long totalBytesInflated = 0;
// 		while(!inflater.needsInput() && !inflater.finished()) {
// 		    long bytesRemainingToInflate = block.getOutSize()-totalBytesInflated;
// 		    //System.out.println();
// 		    //System.out.println("inflater.needsInput()" + inflater.needsInput());
// 		    int curBytesInflated = inflater.inflate(outBuffer, 0, 
// 							    (int)Math.min(bytesRemainingToInflate, outBuffer.length));
// 		    if(curBytesInflated == 0 && !inflater.needsInput()) {
// 			System.out.println("inflater.finished()" + inflater.finished());
// 			System.out.println("inflater.needsDictionary()" + inflater.needsDictionary());
// 			System.out.println("inflater.needsInput()" + inflater.needsInput());
// 			//System.out.println("inflater.()" + inflater.());
// 			throw new RuntimeException("Unexpectedly blocked inflate.");
// 		    }
// 		    else {
// 			totalBytesInflated += curBytesInflated;
// 			if(!testOnly)
// 			    isoRaf.write(outBuffer, 0, curBytesInflated);
// 		    }
// 		}
// 	    }
// 	}
// 	if(!inflater.finished())
// 	    throw new RuntimeException("Unclosed ZLIB stream!");
//     }
}
