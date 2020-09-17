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

package org.catacombae.dmg.udif;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.catacombae.io.ReadableFileStream;
import org.catacombae.io.ReadableRandomAccessStream;
import org.catacombae.io.RuntimeIOException;

public class UDIFFileView {
    private ReadableRandomAccessStream dmgRaf;
    
    public UDIFFileView(File file) {
	try {
	    //this.file = file;
            this.dmgRaf =
                new ReadableFileStream(new RandomAccessFile(file, "r"),
                file.getPath());
	} catch(IOException ioe) {
	    throw new RuntimeIOException(ioe);
	}
    }
    public UDIFFileView(ReadableRandomAccessStream dmgRaf) {
	this.dmgRaf = dmgRaf;
    }
    
    public byte[] getPlistData() throws RuntimeIOException {
	Koly koly = getKoly();
	byte[] plistData = new byte[(int)koly.getPlistSize()]; // Let's hope the plistsize is within int range... (though memory will run out long before that)
	
	dmgRaf.seek(koly.getPlistBegin1());
	if(dmgRaf.read(plistData) == plistData.length)
	    return plistData;
	else
	    throw new RuntimeException("Could not read the entire region of data containing the Plist");
    }
    
    public Plist getPlist() throws RuntimeIOException {
	return new Plist(getPlistData());
    }
    
    public Koly getKoly() throws RuntimeIOException {
	dmgRaf.seek(dmgRaf.length()-512);
	byte[] kolyData = new byte[512];
	dmgRaf.read(kolyData);
	return new Koly(kolyData, 0);
    }
    
    public void close() throws RuntimeIOException {
	dmgRaf.close();
    }
}
