package org.catacombae.dmgx;

import java.io.*;

public class DmgFileView {
    //private File file;
    private RandomAccessFile dmgRaf;
    
    public DmgFileView(File file) {
	try {
	    //this.file = file;
	    this.dmgRaf = new RandomAccessFile(file, "r");
	} catch(IOException ioe) {
	    throw new RuntimeException(ioe);
	}
    }
    public DmgFileView(RandomAccessFile dmgRaf) {
	this.dmgRaf = dmgRaf;
    }
    
    public Plist getPlist() throws IOException {
	Koly koly = getKoly();
	byte[] plistData = new byte[(int)koly.getPlistSize()]; // Let's hope the plistsize is within int range...
	dmgRaf.seek(koly.getPlistBegin1());
	dmgRaf.read(plistData);
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