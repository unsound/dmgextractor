/*-
 * Copyright (C) 2006-2008 Erik Larsson
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

package org.catacombae.dmgextractor;

import org.catacombae.dmg.udif.UDIFBlockInputStream;
import org.catacombae.dmg.udif.UDIFBlock;
import java.io.*;
import org.catacombae.io.RandomAccessStream;
import org.catacombae.io.ReadableRandomAccessStream;

/** Please don't try to use this code with concurrent threads... :) Use external synchronization to protect
    the shared data in this class. */
class DMGBlockHandlers {
    private static byte[] inBuffer = new byte[0x40000];
    
    /** Extracts a DMGBlock describing a region of the file dmgRaf to the file isoRaf. If the testOnly flag
	is set, nothing is written to isoRaf (in fact, it can be null in this case). ui may not be null. in
	that case, use UserInterface.NullUI. */
    static long processBlock(UDIFBlock block, ReadableRandomAccessStream dmgRaf, RandomAccessStream isoRaf, 
				    boolean testOnly, UserInterface ui) throws IOException {
	UDIFBlockInputStream is = UDIFBlockInputStream.getStream(dmgRaf, block);
	long res = processStream(is, isoRaf, testOnly, ui);
	is.close();
	if(res != block.getOutSize())
	    System.err.println("WARNING: Could not extract entire block! Extracted " + res + " of " + block.getOutSize() + " bytes");
	return res;
    }
    
    private static long processStream(UDIFBlockInputStream is, RandomAccessStream isoRaf, 
				      boolean testOnly, UserInterface ui) throws IOException {
	long totalBytesRead = 0;
	int bytesRead = is.read(inBuffer);
	while(bytesRead > 0) {
	    totalBytesRead += bytesRead;
	    //ui.reportProgress((int)(dmgRaf.getFilePointer()*100/dmgRaf.length()));
	    ui.addProgressRaw(bytesRead);
	    if(!testOnly) isoRaf.write(inBuffer, 0, bytesRead);
	    bytesRead = is.read(inBuffer);
	}
	return totalBytesRead;
    }
}
