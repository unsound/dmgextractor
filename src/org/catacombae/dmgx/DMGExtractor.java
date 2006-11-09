/*-
 * Copyright (C) 2006 Erik Larsson
 *           (C) 2004 vu1tur (not the actual code but...)
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

import net.iharder.Base64;
import java.io.*;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.ProgressMonitor;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class DMGExtractor {
    public static final String APPNAME = "DMGExtractor 0.51pre";
    public static final String BUILDSTRING = "(Build #" + BuildNumber.BUILD_NUMBER + ")";
    public static final boolean DEBUG = true;
    // Constants defining block types in the dmg file
    public static final int BT_ADC = 0x80000004;
    public static final int BT_ZLIB = 0x80000005;
    public static final int BT_BZIP2 = 0x80000006;
    public static final int BT_COPY = 0x00000001;
    public static final int BT_ZERO = 0x00000002;
    public static final int BT_ZERO2 = 0x00000000; // This one also represents a fill of zeroes. What is the difference?
    public static final int BT_END = 0xffffffff;
    public static final int BT_UNKNOWN = 0x7ffffffe;
    public static final long PLIST_ADDRESS_1 = 0x1E0;
    public static final long PLIST_ADDRESS_2 = 0x128;
    public static final String BACKSPACE79 = "\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b";
//     public static PrintStream stdout = System.out;
//     public static PrintStream stderr = System.err;
    public static BufferedReader stdin = 
	new BufferedReader(new InputStreamReader(System.in));

    public static boolean verbose = false;
    public static boolean graphical = false;
    public static String startupCommand = "java DMGExtractor";
    public static File dmgFile = null;
    public static File isoFile = null;
    
    /** Used to prevent unneccessary updates of the progress meter. */
    public static long previousPercentage = -1;

    public static ProgressMonitor progmon;

    /* temp */
    private static DummyMonitor dummyMonitor = new DummyMonitor();
    
    public static void main(String[] args) throws Exception {
	try {
	    notmain(args);
	} catch(Exception e) {
	    if(graphical)
		JOptionPane.showMessageDialog(null, "The program encountered an unexpected error: " + e.toString() + 
					      "\nClosing...", "Error", JOptionPane.ERROR_MESSAGE);
	    throw e;
	}
    }
    
    public static void notmain(String[] args) throws Exception {
	if(DEBUG) verbose = true;
	
	parseArgs(args);

	printlnVerbose("Processing: " + dmgFile);
	RandomAccessFile dmgRaf = new RandomAccessFile(dmgFile, "r");
	RandomAccessFile isoRaf = null;
	boolean testOnly = false;
	if(isoFile != null) {
	    isoRaf = new RandomAccessFile(isoFile, "rw");
	    isoRaf.setLength(0);
	    printlnVerbose("Extracting to: " + isoFile);
	}
	else {
	    testOnly = true;
	    printlnVerbose("Simulating extraction...");
	}
	
	dmgRaf.seek(dmgRaf.length()-PLIST_ADDRESS_1);
	long plistBegin1 = dmgRaf.readLong();
	long plistEnd = dmgRaf.readLong();
	dmgRaf.seek(dmgRaf.length()-PLIST_ADDRESS_2);
	long plistBegin2 = dmgRaf.readLong();
	long plistSize = dmgRaf.readLong();
	
	if(DEBUG) {
	    println("Read addresses:",
		    "  " + plistBegin1,
		    "  " + plistBegin2);
	}
	if(plistBegin1 != plistBegin2) {
	    println("Addresses not equal! Assumption broken... =/",
		    plistBegin1 + " != " + plistBegin2);
	    System.exit(0);
	}
	if(plistSize != (plistEnd-plistBegin1)) {
	    println("plistSize field does not match plistEnd marker!",
		    "plistSize=" + plistSize + " plistBegin1=" + plistBegin1 + " plistEnd=" + plistEnd + " plistEnd-plistBegin1=" + (plistEnd-plistBegin1));
	}
	printlnVerbose("Jumping to address...");
 	dmgRaf.seek(plistBegin1);
	byte[] buffer = new byte[(int)plistSize];
	dmgRaf.read(buffer);

	InputStream is = new ByteArrayInputStream(buffer);

	NodeBuilder handler = new NodeBuilder();
	SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
	try {
// 	    System.out.println("validation: " + saxParser.getProperty("validation"));
// 	    System.out.println("external-general-entities: " + saxParser.getProperty("external-general-entities"));
// 	    System.out.println("external-parameter-entities: " + saxParser.getProperty("external-parameter-entities"));
// 	    System.out.println("is-standalone: " + saxParser.getProperty("is-standalone"));
// 	    System.out.println("lexical-handler: " + saxParser.getProperty("lexical-handler"));
// 	    System.out.println("parameter-entities: " + saxParser.getProperty("parameter-entities"));
// 	    System.out.println("namespaces: " + saxParser.getProperty("namespaces"));
// 	    System.out.println("namespace-prefixes: " + saxParser.getProperty("namespace-prefixes"));
// 	    System.out.println(": " + saxParser.getProperty(""));
// 	    System.out.println(": " + saxParser.getProperty(""));
// 	    System.out.println(": " + saxParser.getProperty(""));
// 	    System.out.println(": " + saxParser.getProperty(""));
// 	    System.out.println("" + saxParser.getProperty(""));
// 	    System.out.println("" + saxParser.getProperty(""));
// 	    System.out.println("" + saxParser.getProperty(""));
// 	    System.out.println("" + saxParser.getProperty(""));
// 	    System.out.println("" + saxParser.getProperty(""));
// 	    System.out.println("" + saxParser.getProperty(""));
// 	    System.out.println("" + saxParser.getProperty(""));
// 	    System.out.println("" + saxParser.getProperty(""));

	    //System.out.println("isValidating: " + saxParser.isValidating());
	    saxParser.parse(is, handler);
	} catch(SAXException se) {
	    se.printStackTrace();
	    System.err.println("Could not read the partition list... exiting.");
	    System.exit(1);
	}
	
	XMLNode[] rootNodes = handler.getRoots();
	if(rootNodes.length != 1) {
	    println("Could not parse DMG-file!");
	    System.exit(0);
	}

	/* Ok, now we have a tree built from the XML-document. Let's walk to the right place. */
	/* cd plist 
	   cd dict
	   cdkey resource-fork (type:dict)
	   cdkey blkx (type:array) */
	XMLNode current;
	XMLElement[] children;
	boolean keyFound;
	current = rootNodes[0]; //We are at plist... probably (there should be only one root node)
	
	current = current.cd("dict");
	current = current.cdkey("resource-fork");
	current = current.cdkey("blkx");
	printlnVerbose("Found " + current.getChildren().length + " partitions:");
	
	byte[] inBuffer = new byte[0x40000];
	byte[] outBuffer = new byte[0x40000];

	byte[] zeroblock = new byte[4096];
	/* I think java always zeroes its arrays on creation... 
	   but let's play safe. */
	for(int y = 0; y < zeroblock.length; ++y)
	    zeroblock[y] = 0;
	
	LinkedList<DMGBlock> blocks = new LinkedList<DMGBlock>();
	
	long elementNumber = 0;
	//long lastOffs = 0;
	long lastOutOffset = 0;
	long lastInOffset = 0;
	long totalSize = 0;
	int errorsReported = 0;
	int warningsReported = 0;
	reportProgress(0);
	for(XMLElement xe : current.getChildren()) {
	    if(progmon != null && progmon.isCanceled()) System.exit(0);
	    if(xe instanceof XMLNode) {
		XMLNode xn = (XMLNode)xe;
		byte[] data = Base64.decode(xn.getKeyValue("Data"));
		
		long partitionSize = calculatePartitionSize(data);
		totalSize += partitionSize;
		
		printlnVerbose("  " + xn.getKeyValue("Name"));
		printlnVerbose("    ID: " + xn.getKeyValue("ID"));
		printlnVerbose("    Attributes: " + xn.getKeyValue("Attributes"));
		printlnVerbose("    Partition map data length: " + data.length + " bytes");
		printlnVerbose("    Partition size: " + partitionSize + " bytes");
		if(verbose) {
		    printlnVerbose("    Dumping blkx...");
		    FileOutputStream fos = new FileOutputStream(xn.getKeyValue("ID") + ".blkx");
		    fos.write(data);
		    fos.close();
		}

		if(DEBUG) {
		    File dumpFile = new File("data " + xn.getKeyValue("ID") + ".bin");
		    println("    Dumping partition map to file: " + dumpFile);
		    
		    FileOutputStream dump = new FileOutputStream(dumpFile);
		    dump.write(data);
		    dump.close();
		}

		int offset = 0xCC;
		int blockType = 0;
		
		/* Offset of the input data for the current block in the input file */
		long inOffset = 0;
		/* Size of the input data for the current block */
		long inSize = 0;
		/* Offset of the output data for the current block in the output file */
		long outOffset = 0;
		/* Size of the output data (possibly larger than inSize because of
		   decompression, zero expansion...) */
		long outSize = 0;
		
		long lastByteReadInBlock = -1;

		boolean addInOffset = false;
		
		//, lastInOffs = 0;
		int blockCount = 0;
		while(blockType != BT_END) {		    
		    if(progmon != null && progmon.isCanceled()) System.exit(0);
		    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
		    int bytesSkipped = 0;
		    while(bytesSkipped < offset)
			bytesSkipped += dis.skipBytes(offset-bytesSkipped);
		    
		    blockType = dis.readInt();
		    int skipped = dis.readInt(); //Skip 4 bytes forward
		    outOffset = dis.readLong()*0x200;//(dis.readInt() & 0xffffffffL)*0x200; //unsigned int -> long
		    //dis.readInt(); //Skip 4 bytes forward
		    outSize = dis.readLong()*0x200;//(dis.readInt() & 0xffffffffL)*0x200; //unsigned int -> long
		    inOffset = dis.readLong();// & 0xffffffffL; //unsigned int -> long
		    //dis.readInt(); //Skip 4 bytes forward
		    inSize = dis.readLong();//dis.readInt() & 0xffffffffL; //unsigned int -> long
		    
		    if(lastByteReadInBlock == -1)
			lastByteReadInBlock = inOffset;
		    lastByteReadInBlock += inSize;
		    
		    /* The lines below are a "hack" that I had to do to make dmgx work with
		       certain dmg-files. I don't understand the issue at all, which is why
		       this hack is here, but sometimes inOffset == 0 means that it is 0
		       relative to the previous partition's last inOffset. And sometimes it
		       doesn't (meaning the actual position 0 in the dmg file). */
		    if(addInOffset) {
			if(DEBUG)
			    println("!-----addInOffset mode: inOffset tranformation " + inOffset + "->" + (inOffset+lastInOffset));
			inOffset += lastInOffset;
		    }
		    else if(inOffset == 0) {
			if(DEBUG)
			    println("!-----Detected inOffset == 0, setting to " + lastInOffset);
			addInOffset = true;
			inOffset = lastInOffset;
		    }
		    outOffset += lastOutOffset;
		    
		    DMGBlock currentBlock = new DMGBlock(blockType, skipped, outOffset, outSize, inOffset, inSize);
		    blocks.add(currentBlock);

		    if(DEBUG) {
			println("outOffset=" + outOffset + " outSize=" + outSize + 
				" inOffset=" + inOffset + " inSize=" + inSize +
				" lastOutOffset=" + lastOutOffset + " lastInOffset=" + lastInOffset
				/*+ " lastInOffs=" + lastInOffs + " lastOffs=" + lastOffs*/);
		    }
		    
		    if(blockType == BT_ADC) {
			println("      " + elementNumber + ":" + blockCount + ". ERROR: BT_ADC not supported.");
			++errorsReported;
			if(!testOnly)
			    System.exit(0);
		    }
		    else if(blockType == BT_ZLIB) {
			if(DEBUG)
			    println("      " + elementNumber + ":"  + blockCount + ". BT_ZLIB processing...");
			
			if(!testOnly && isoRaf.getFilePointer() != outOffset)
			    println("      " + elementNumber + ":"  + blockCount + ". WARNING: BT_ZLIB FP != outOffset (" +
				    isoRaf.getFilePointer() + " != " + outOffset + ")");

			try {
			    DMGBlockHandlers.processZlibBlock(currentBlock, dmgRaf, isoRaf, testOnly, dummyMonitor);
			} catch(DataFormatException dfe) {
			    println("      " + elementNumber + ":"  + blockCount + ". ERROR: BT_ZLIB Could not decode...");
			    ++errorsReported;
			    if(!DEBUG) {
				println("outOffset=" + outOffset + " outSize=" + outSize + 
					" inOffset=" + inOffset + " inSize=" + inSize +
					" lastOutOffset=" + lastOutOffset + " lastInOffset=" + lastInOffset);
			    }
			    dfe.printStackTrace();
			    if(!testOnly)
				System.exit(0);
			    else {
				println("      Testing mode, so continuing...");
				//System.exit(0);
				break;
			    }
			}
		    }
		    else if(blockType == BT_BZIP2) {
			println("      " + elementNumber + ":" + blockCount + ". ERROR: BT_BZIP2 not currently supported.");
			++errorsReported;
			if(!testOnly)
			    System.exit(0);
		    }
		    else if(blockType == BT_COPY) {
			if(DEBUG)
			    println("      " + elementNumber + ":" + blockCount + ". BT_COPY processing...");

			if(!testOnly && isoRaf.getFilePointer() != outOffset) {
			    println("      " + elementNumber + ":" + blockCount + ". WARNING: BT_COPY FP != outOffset (" + isoRaf.getFilePointer() + " != " + outOffset + ")");
			    ++warningsReported;
			}
			dmgRaf.seek(/*lastOffs+*/inOffset);
			
			int bytesRead = dmgRaf.read(inBuffer, 0, Math.min((int)inSize, inBuffer.length));
			long totalBytesRead = bytesRead;
			while(bytesRead != -1) {
			    reportFilePointerProgress(dmgRaf);

			    if(!testOnly)
				isoRaf.write(inBuffer, 0, bytesRead);
			    if(totalBytesRead >= inSize)
				break;
			    bytesRead = dmgRaf.read(inBuffer, 0, Math.min((int)(inSize-totalBytesRead), inBuffer.length));
			    if(bytesRead > 0)
				totalBytesRead += bytesRead;
			}
			
 			//lastInOffs = inOffset+inSize;
		    }
		    else if(blockType == BT_ZERO) {
			if(DEBUG)
			    println("      " + elementNumber + ":" + blockCount + ". BT_ZERO processing...");
			if(!testOnly && isoRaf.getFilePointer() != outOffset) {
			    println("      " + elementNumber + ":" + blockCount + ". WARNING: BT_ZERO FP != outOffset (" + 
				    isoRaf.getFilePointer() + " != " + outOffset + ")");
			    ++warningsReported;
			}

			reportFilePointerProgress(dmgRaf);

			long numberOfZeroBlocks = outSize/zeroblock.length;
			int numberOfRemainingBytes = (int)(outSize%zeroblock.length);
			for(int j = 0; j < numberOfZeroBlocks; ++j) {
			    if(!testOnly)
				isoRaf.write(zeroblock);
			}
			if(!testOnly)
			    isoRaf.write(zeroblock, 0, numberOfRemainingBytes);
			
 			//lastInOffs = inOffset+inSize;
		    }
		    else if(blockType == BT_ZERO2) {
			if(DEBUG)
			    println("      " + elementNumber + ":" + blockCount + ". BT_ZERO2 processing...");
			if(!testOnly && isoRaf.getFilePointer() != outOffset) {
			    println("      " + elementNumber + ":" + blockCount + ". WARNING: BT_ZERO2 FP != outOffset (" + 
				    isoRaf.getFilePointer() + " != " + outOffset + ")");
			    ++warningsReported;
			}

			reportFilePointerProgress(dmgRaf);

			long numberOfZeroBlocks = outSize/zeroblock.length;
			int numberOfRemainingBytes = (int)(outSize%zeroblock.length);
			for(int j = 0; j < numberOfZeroBlocks; ++j) {
			    if(!testOnly)
				isoRaf.write(zeroblock);
			}
			if(!testOnly)
			    isoRaf.write(zeroblock, 0, numberOfRemainingBytes);
			
 			//lastInOffs = inOffset+inSize;
		    }
		    else if(blockType == BT_UNKNOWN) {
			/* I have no idea what this blocktype is... but it's common, and usually
			   doesn't appear more than 2-3 times in a dmg. As long as its input and
			   output sizes are 0, there's no reason to complain... is there? */
			if(DEBUG)
			    println("      " + elementNumber + ":" + blockCount + ". BT_UNKNOWN processing...");
			if(!(inSize == 0 && outSize == 0)) {
			    println("      " + elementNumber + ":" + blockCount + ". WARNING: Blocktype BT_UNKNOWN had non-zero sizes...",
				    "        inSize=" + inSize + ", outSize=" + outSize);
			    ++warningsReported;
			    //println("        The author of the program would be pleased if you contacted him about this.");
			    // ...or would I?
			}
		    }
		    else if(blockType == BT_END) {
			if(DEBUG)
			    println("      " + elementNumber + ":" + blockCount + ". BT_END processing...");
 			if(!testOnly && isoRaf.getFilePointer() != outOffset)
			    println("      " + elementNumber + ":" + blockCount + ". WARNING: BT_END FP != outOffset (" +
				    isoRaf.getFilePointer() + " != " + outOffset + ")");
			
			//lastOffs += lastInOffs;
			lastOutOffset = outOffset;
			lastInOffset += lastByteReadInBlock;
		    }
 		    else {
 			println("      " + elementNumber + ":" + blockCount + ". WARNING: previously unseen blocktype " + blockType + " [0x" + Integer.toHexString(blockType) + "]",
				"      " + elementNumber + ":" + blockCount + ". outOffset=" + outOffset + " outSize=" + outSize + " inOffset=" + inOffset + " inSize=" + inSize);
			++warningsReported;
			
 			if(!testOnly && isoRaf.getFilePointer() != outOffset)
			    println("      " + elementNumber + ":" + blockCount + ". unknown blocktype FP != outOffset (" +
				    isoRaf.getFilePointer() + " != " + outOffset + ")");
			
		    }
		    
		    offset += 0x28;
		    ++blockCount;
		}
	    }
	    ++elementNumber;
	}
	//printlnVerbose("Progress: 100% Done!");
	reportProgress(100);
	String summary = (errorsReported != 0)?errorsReported+" errors reported":"No errors reported";
	summary += (warningsReported != 0)?" ("+warningsReported+" warnings emitted).":".";
	if(!graphical) {
	    newline();
	    println(summary);
	    printlnVerbose("Total extracted bytes: " + totalSize + " B");
	}
	else {
	    progmon.close();
	    JOptionPane.showMessageDialog(null, "Extraction complete! " + summary + "\n" +
					  "Total extracted bytes: " + totalSize + " B", 
					  "Information", JOptionPane.INFORMATION_MESSAGE);
	    System.exit(0);
	}

	if(!DEBUG) {
	    if(isoRaf != null)
		isoRaf.close();
	    dmgRaf.close();
	}
	else {
	    if(isoRaf != null)
		isoRaf.close();
// 	System.out.println("blocks.size()=" + blocks.size());
// 	for(DMGBlock b : blocks)
// 	    System.out.println("  " + b.toString());
	    LinkedList<DMGBlock> merged = mergeBlocks(blocks);
// 	System.out.println("merged.size()=" + merged.size());
// 	for(DMGBlock b : merged)
// 	    System.out.println("  " + b.toString());
	    println("Extracting all the parts not containing block data from source file:");
	    int i = 1;
	    DMGBlock previous = null;
	    for(DMGBlock b : merged) {
		if(previous == null && b.inOffset > 0) {
		    String filename = i++ + ".block";
		    println("  " + new File(filename).getCanonicalPath() + "...");
		    FileOutputStream curFos = new FileOutputStream(new File(filename));
		    dmgRaf.seek(0);
		    byte[] data = new byte[(int)(b.inOffset)];
		    dmgRaf.read(data);
		    curFos.write(data);
		    curFos.close();
		}
		else if(previous != null) {
		    String filename = i++ + ".block";
		    println("  " + new File(filename).getCanonicalPath() + "...");
		    FileOutputStream curFos = new FileOutputStream(new File(filename));
		    dmgRaf.seek(previous.inOffset+previous.inSize);
		    byte[] data = new byte[(int)(b.inOffset-(previous.inOffset+previous.inSize))];
		    dmgRaf.read(data);
		    curFos.write(data);
		    curFos.close();
		}
		previous = b;
	    }
	    if(previous.inOffset+previous.inSize != dmgRaf.length()) {
		String filename = i++ + ".block";
		println("  " + new File(filename).getCanonicalPath() + "...");
		FileOutputStream curFos = new FileOutputStream(new File(filename));
		dmgRaf.seek(previous.inOffset+previous.inSize);
		byte[] data = new byte[(int)(dmgRaf.length()-(previous.inOffset+previous.inSize))];
		dmgRaf.read(data);
		curFos.write(data);		
		curFos.close();
	    }
	    dmgRaf.close();
	    System.out.println("done!");
	}
    }
    public static void parseArgs(String[] args) {
	boolean parseSuccessful = false;
	try {
	    /* Take care of the options... */
	    int i;
	    for(i = 0; i < args.length; ++i) {
		String cur = args[i];
		if(!cur.startsWith("-"))
		    break;
		else if(cur.equals("-gui")) {
		    graphical = true;

		    // This should be moved to UI class in the future.
		    System.setProperty("swing.aatext", "true"); //Antialiased text
		    try { javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName()); }
		    catch(Exception e) {}
		}
		else if(cur.equals("-v"))
		    verbose = true;
		else if(cur.equals("-startupcommand")) {
		    startupCommand = args[i+1];
		    ++i;
		}
	    }

	    println(APPNAME + " " + BUILDSTRING,
		    "Copyright (c) 2006 Erik Larsson <erik82@kth.se>",
		    "  based upon dmg2iso, Copyright (c) 2004 vu1tur <v@vu1tur.eu.org>",
		    "  also using the iHarder Base64 Encoder/Decoder <http://iharder.sf.net>",
		    "",
		    "This program is distributed under the GNU General Public License version 2 or",
		    "later. See <http://www.gnu.org/copyleft/gpl.html> for the details.",
		    "");
	    
	    if(i == args.length) {
		dmgFile = getInputFileFromUser();
		if(dmgFile == null)
		    System.exit(0);
		if(getOutputConfirmationFromUser()) {
		    isoFile = getOutputFileFromUser();
		    if(isoFile == null)
			System.exit(0);
		}
	    }
	    else {
		dmgFile = new File(args[i++]);
		if(!dmgFile.exists()) {
		    println("File \"" + dmgFile + "\" could not be found!");
		    System.exit(0);
		}
		
		if(i == args.length-1)
		    isoFile = new File(args[i]);
		else if(i != args.length)
		    throw new Exception();
	    }
	    
	    parseSuccessful = true;
	} catch(Exception e) {
	    println();
	    println("  usage: " + startupCommand + " [options] <dmgFile> [<isoFile>]");
	    println("  if an iso-file is not supplied, the program will simulate an extraction");
	    println("  (useful for detecting errors in dmg-files)");
	    println();
	    System.exit(0);
	}
    }
    
    public static long calculatePartitionSize(byte[] data) throws IOException {
	long partitionSize = 0;
	DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
	long totalBytesRead;
	totalBytesRead = 0;
	while(totalBytesRead < 0xCC)
	    totalBytesRead += dis.skip(0xCC);
	
	while(totalBytesRead < data.length) {
	    int bytesRead = 0;
	    while(bytesRead < 0x10)
		bytesRead += dis.skip(0x10-bytesRead);
	    
	    partitionSize += dis.readLong()*0x200;
	    bytesRead += 0x8;
	    
	    while(bytesRead < 0x28)
		bytesRead += dis.skip(0x28-bytesRead);
	    totalBytesRead += bytesRead;
	}
	return partitionSize;
    }

    /** Never used. Java is big-endian. */
    public static int swapEndian(int i) {
	return 
	    ((i & 0xff000000) >> 24) | 
	    ((i & 0x00ff0000) >> 8 ) | 
	    ((i & 0x0000ff00) << 8 ) | 
	    ((i & 0x000000ff) << 24);
    }

    public static void printCurrentLine(String s) {
	System.out.print(BACKSPACE79);
	System.out.print(s);
    }
    public static void println() {
	System.out.print(BACKSPACE79);
	System.out.println();
    }
    public static void println(String... lines) {
	if(!graphical) {
	    System.out.print(BACKSPACE79);
	    for(String s : lines)
		System.out.println(s);
	}
	else {
	    String resultString = null;
	    for(String s : lines) {
		if(resultString == null)
		    resultString = s;
		else
		    resultString += "\n" + s;
	    }
	    JOptionPane.showMessageDialog(null, resultString, 
					  APPNAME, JOptionPane.INFORMATION_MESSAGE);
	}
    }
    public static void printlnVerbose() {
	if(verbose) {
	    System.out.print(BACKSPACE79);
	    System.out.println();
	}
    }
    public static void printlnVerbose(String... lines) {
	if(verbose) {
	    System.out.print(BACKSPACE79);
	    for(String s : lines)
		System.out.println(s);
	}
    }

    public static void newline() {
	System.out.println();
    }
    
    /** Simply calculates the file pointers position relative to the file size as a percentage, and reports it. */
    public static void reportFilePointerProgress(RandomAccessFile raf) throws IOException {
	reportProgress((int)(raf.getFilePointer()*100/raf.length()));
    }

    public static void reportProgress(int progressPercentage) {
	if(progressPercentage != previousPercentage) {
	    previousPercentage = progressPercentage;
	    if(!graphical) {
		printCurrentLine("--->Progress: " + progressPercentage + "%");
	    }
	    else {
		if(progmon == null) {
		    progmon = new ProgressMonitor(null, "Extracting dmg to iso...", "0%", 0, 100);
		    progmon.setProgress(0);
		    progmon.setMillisToPopup(0);
		}
		progmon.setProgress((int)progressPercentage);
		progmon.setNote(progressPercentage + "%");
	    }
	}
    }

    public static File getInputFileFromUser() throws IOException {
	if(!graphical) {
	    //String s = "";
	    while(true) {
		printCurrentLine("Please specify the path to the dmg file to extract from: ");
		File f = new File(stdin.readLine().trim());
		while(!f.exists()) {
		    println("File does not exist!");
		    printCurrentLine("Please specify the path to the dmg file to extract from: ");
		    f = new File(stdin.readLine().trim());
		}
		return f;
	    }
	}
	else {
	    SimpleFileFilter sff = new SimpleFileFilter();
	    sff.addExtension("dmg");
	    sff.setDescription("DMG disk image files");
 	    JFileChooser jfc = new JFileChooser();
	    jfc.setFileFilter(sff);
	    jfc.setMultiSelectionEnabled(false);	    
	    jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
	    jfc.setDialogTitle("Choose the dmg-file to read...");
	    while(true) {
		if(jfc.showDialog(null, "Open") == JFileChooser.APPROVE_OPTION) {
		    File f = jfc.getSelectedFile();
		    if(f.exists())
			return f;
		    else
			JOptionPane.showMessageDialog(null, "The file does not exist! Choose again...", 
						      "Error", JOptionPane.ERROR_MESSAGE);
		}
		else
		    return null;
	    }
	}
    }
    public static boolean getOutputConfirmationFromUser() throws IOException {
	if(!graphical) {
	    String s = "";
	    while(true) {
		printCurrentLine("Do you want to specify an output file (y/n)? ");
		s = stdin.readLine().trim();
		if(s.equalsIgnoreCase("y"))
		    return true;
		else if(s.equalsIgnoreCase("n"))
		    return false;
	    }
	}
	else {
	    return JOptionPane.showConfirmDialog(null, "Do you want to specify an output file?", 
						 "Confirmation", JOptionPane.YES_NO_OPTION, 
						 JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
	}
    }
    public static File getOutputFileFromUser() throws IOException {
	final String msg1 = "Please specify the path of the iso file to extract to: ";
	final String msg2 = "The file already exists. Do you want to overwrite?";
	if(!graphical) {
	    while(true) {
		printCurrentLine(msg1);
		File f = new File(stdin.readLine().trim());
		while(f.exists()) {
		    while(true) {
			printCurrentLine(msg2 + " (y/n)? ");
			String s = stdin.readLine().trim();
			if(s.equalsIgnoreCase("y"))
			    return f;
			else if(s.equalsIgnoreCase("n"))
			    break;
		    }
		    printCurrentLine(msg1);
		    f = new File(stdin.readLine().trim());
		}
		return f;
	    }
	}
	else {
 	    JFileChooser jfc = new JFileChooser();
	    jfc.setMultiSelectionEnabled(false);
	    jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
	    jfc.setDialogTitle("Choose the output iso-file...");
	    while(true) {
		if(jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
		    File f = jfc.getSelectedFile();
		    if(!f.exists())
			return f;
		    else if(JOptionPane.showConfirmDialog(null, msg2, "Confirmation", JOptionPane.YES_NO_OPTION, 
							  JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
			return f;
		    }
		}
		else
		    return null;
	    }
	}
    }
    
    public static LinkedList<DMGBlock> mergeBlocks(LinkedList<DMGBlock> blockList) {
	LinkedList<DMGBlock> result = new LinkedList<DMGBlock>();
	Iterator<DMGBlock> it = blockList.iterator();
	DMGBlock previous = it.next();
	DMGBlock current;
	while(it.hasNext()) {
	    current = it.next();
	    if(current.inSize != 0) {
		if(current.inOffset == previous.inOffset+previous.inSize) {
		    DMGBlock mergedBlock = new DMGBlock(previous.blockType, previous.skipped, previous.outOffset, previous.outSize+current.outSize, previous.inOffset, previous.inSize+current.inSize);
		    previous = mergedBlock;
		}
		else {
		    result.addLast(previous);
		    previous = current;
		}
	    }
	}
	result.addLast(previous);
	return result;
    }
    
    public static class DummyMonitor implements UserInterface {
	public void reportProgress(int progress) {
	    DMGExtractor.reportProgress(progress);
	}
    }
}

