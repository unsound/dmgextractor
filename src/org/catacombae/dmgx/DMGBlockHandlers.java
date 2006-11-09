package org.catacombae.dmgx;

import java.io.*;
import java.util.zip.*;

public class DMGBlockHandlers {
    private static byte[] inBuffer = new byte[0x40000];
    private static byte[] outBuffer = new byte[0x40000];
    public static void processZlibBlock(DMGBlock block, RandomAccessFile dmgRaf, RandomAccessFile isoRaf, 
					boolean testOnly, UserInterface ui) throws IOException, DataFormatException {
	dmgRaf.seek(/*block.lastOffs+*/block.inOffset);
			
	    /*
	     * medan det finns komprimerat data att läsa:
	     *   läs in komprimerat data i inbuffer
	     *   medan det finns data kvar att läsa i inbuffer
	     *     dekomprimera data från inbuffer till utbuffer
	     *     skriv utbuffer till fil
	     */
	    
	    Inflater inflater = new Inflater();
	    long totalBytesRead = 0;
	    while(totalBytesRead < block.inSize) {
		long bytesRemainingToRead = block.inSize-totalBytesRead;
		int curBytesRead = dmgRaf.read(inBuffer, 0, 
					       (int)Math.min(bytesRemainingToRead, inBuffer.length));
		// 							   ((inSize-totalBytesRead) < inBuffer.length? //Math.min
		// 							    (int)(inSize-totalBytesRead):
		// 							    inBuffer.length));
		ui.reportProgress((int)(dmgRaf.getFilePointer()*100/dmgRaf.length()));

		if(curBytesRead < 0)
		    throw new RuntimeException("Unexpectedly reached end of file");
		else {
		    totalBytesRead += curBytesRead;
		    inflater.setInput(inBuffer, 0, curBytesRead);
		    long totalBytesInflated = 0;
		    while(!inflater.needsInput() && !inflater.finished()) {
			long bytesRemainingToInflate = block.outSize-totalBytesInflated;
			int curBytesInflated = inflater.inflate(outBuffer, 0, 
								(int)Math.min(bytesRemainingToInflate, outBuffer.length));
			if(curBytesInflated == 0)
			    throw new RuntimeException("Unexpectedly blocked inflate.");
			else {
			    totalBytesInflated += curBytesInflated;
			    if(!testOnly)
				isoRaf.write(outBuffer, 0, curBytesInflated);
			}
		    }
		}
	    }
	    if(!inflater.finished())
		throw new RuntimeException("Unclosed ZLIB stream!");

    }
}