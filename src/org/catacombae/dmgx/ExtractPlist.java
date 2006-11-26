package org.catacombae.dmgx;

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
	
	DmgFileView dfw = new DmgFileView(inFile);
	byte[] plistData = dfw.getPlist().getData();
	/*int bytesWritten = */outStream.write(plistData);
// 	if(bytesWritten != plistData.length)
// 	    System.out.println("ERROR: Could not write all data to output file. " + bytesWritten + " of " + plistData.length + " bytes written.");

	inFile.close();
	outStream.close();
    }
}