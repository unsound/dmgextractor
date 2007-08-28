/*-
 * Copyright (C) 2006 Erik Larsson
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

package org.catacombae.dmgx;

import org.catacombae.io.*;
import java.io.*;

public class DmgFileView {
    //private File file;
    private RandomAccessStream dmgRaf;
    
    public DmgFileView(File file) {
	try {
	    //this.file = file;
	    this.dmgRaf = new RandomAccessFileStream(new RandomAccessFile(file, "r"));
	} catch(IOException ioe) {
	    throw new RuntimeException(ioe);
	}
    }
    public DmgFileView(RandomAccessStream dmgRaf) {
	this.dmgRaf = dmgRaf;
    }
    
    public byte[] getPlistData() throws IOException {
	Koly koly = getKoly();
	byte[] plistData = new byte[(int)koly.getPlistSize()]; // Let's hope the plistsize is within int range... (though memory will run out long before that)
	
	dmgRaf.seek(koly.getPlistBegin1());
	if(dmgRaf.read(plistData) == plistData.length)
	    return plistData;
	else
	    throw new RuntimeException("Could not read the entire region of data containing the Plist");
    }
    
    public Plist getPlist() throws IOException {
	return new Plist(getPlistData());
    }
    
    public Koly getKoly() throws IOException {
	dmgRaf.seek(dmgRaf.length()-512);
	byte[] kolyData = new byte[512];
	dmgRaf.read(kolyData);
	return new Koly(kolyData, 0);
    }
    
    public void close() throws IOException {
	dmgRaf.close();
    }
}
