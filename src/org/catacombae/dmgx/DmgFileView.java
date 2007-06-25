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
    
    public Plist getPlist() throws IOException {
	System.err.println("getting koly");
	Koly koly = getKoly();
	System.err.println("plist size: " + koly.getPlistSize());
	byte[] plistData = new byte[(int)koly.getPlistSize()]; // Let's hope the plistsize is within int range... (though memory will run out long before that)
	System.err.println("seek&read");
	dmgRaf.seek(koly.getPlistBegin1());
	dmgRaf.read(plistData);
	System.err.println("new Plist");
	return new Plist(plistData);
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
