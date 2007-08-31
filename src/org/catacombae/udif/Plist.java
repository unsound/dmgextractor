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

package org.catacombae.udif;

import java.io.*;
import java.nio.charset.Charset;
import java.util.LinkedList;
import net.iharder.base64.Base64;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import org.xml.sax.SAXException;
//import org.xml.sax.helpers.DefaultHandler;
import org.catacombae.dmgx.Util;
import org.catacombae.io.*;
import org.catacombae.xml.*;
import org.catacombae.xml.apx.*;

public class Plist {
    //private final byte[] plistData;
    private XMLNode rootNode;
    private boolean useSaxParser = false;
    
    public Plist(byte[] data) {
	this(data, 0, data.length);
    }
    public Plist(byte[] data, boolean useSAXParser) {
	this(data, 0, data.length, useSAXParser);
    }
    public Plist(byte[] data, int offset, int length) {
	this(data, offset, length, false);
    }
    public Plist(byte[] data, int offset, int length, boolean useSAXParser) {
	//plistData = new byte[length];
	//System.arraycopy(data, offset, plistData, 0, length);
	rootNode = parseXMLData(data, useSAXParser);
    }
    
    //public byte[] getData() { return Util.createCopy(plistData); }
    
    public PlistPartition[] getPartitions() throws IOException {
	LinkedList<PlistPartition> partitionList = new LinkedList<PlistPartition>();
	XMLNode current = rootNode;
	current = current.cd("dict");
	current = current.cdkey("resource-fork");
	current = current.cdkey("blkx");
	int numberOfPartitions = current.getChildren().length;
	
	// Variables to keep track of the pointers of the previous partition
	long previousOutOffset = 0;
	long previousInOffset = 0;
	
	int i = 0;
	// Iterate over the partitions and gather data
	for(XMLElement xe : current.getChildren()) {
	    if(xe instanceof XMLNode) {
		XMLNode xn = (XMLNode)xe;
		
		String partitionName = Util.readFully(xn.getKeyValue("Name"));
		String partitionID = Util.readFully(xn.getKeyValue("ID"));
		String partitionAttributes = Util.readFully(xn.getKeyValue("Attributes"));
		//System.err.println("Retrieving data...");
		//(new BufferedReader(new InputStreamReader(System.in))).readLine();
		Reader base64Data = xn.getKeyValue("Data");
		//System.gc();
		//System.err.println("Converting data to binary form... free memory: " + Runtime.getRuntime().freeMemory() + " total memory: " + Runtime.getRuntime().totalMemory());
		//byte[] data = Base64.decode(base64Data);
		
// 		try {
// 		    InputStream yo = new Base64.InputStream(new ReaderInputStream(base64Data, Charset.forName("US-ASCII")));
// 		    String filename1 = "dump_plist_java-" + System.currentTimeMillis() + ".datadpp";
// 		    System.err.println("Dumping output from ReaderInputStream to file \"" + filename1 + "\"");
// 		    FileOutputStream fos = new FileOutputStream(filename1);
// 		    if(false) { // Standard way
// 			byte[] buffer = new byte[4096];
// 			int curBytesRead = yo.read(buffer);
// 			while(curBytesRead == buffer.length) {
// 			    fos.write(buffer, 0, curBytesRead);
// 			    curBytesRead = yo.read(buffer);
// 			}
// 			if(curBytesRead > 0)
// 			    fos.write(buffer, 0, curBytesRead);
// 		    }
// 		    else { // Simulating PlistPartition constructor
// 			byte[] buf1 = new byte[0xCC];
// 			byte[] buf2 = new byte[0x28];
// 			int curBytesRead = (int)yo.skip(0xCC); // SKIP OPERATION FUCKS UP!one
// 			fos.write(buf1, 0, curBytesRead);
// 			curBytesRead = yo.read(buf2);
// 			while(curBytesRead == buf2.length) {
// 			    fos.write(buf2, 0, curBytesRead);
// 			    curBytesRead = yo.read(buf2);
// 			}
// 			if(curBytesRead > 0)
// 			    fos.write(buf2, 0, curBytesRead);
			
// 		    }
// 		    fos.close();
// 		} catch(Exception e) { e.printStackTrace(); }
		
		InputStream base64DataInputStream = new Base64.InputStream(new ReaderInputStream(base64Data, Charset.forName("US-ASCII")));
		
		//System.err.println("Creating PlistPartition.");
		//System.out.println("Block list for partition " + i++ + ":");
		PlistPartition dpp = new PlistPartition(partitionName, partitionID, partitionAttributes, 
							      base64DataInputStream, previousOutOffset, previousInOffset);
		previousOutOffset = dpp.getFinalOutOffset();
		previousInOffset = dpp.getFinalInOffset();
		partitionList.addLast(dpp);
	    }
	}
	
	return partitionList.toArray(new PlistPartition[partitionList.size()]);
    }
    
    private XMLNode parseXMLData(byte[] plistData, boolean defaultToSAX) {
	//InputStream is = new ByteArrayInputStream(plistData);
	NodeBuilder handler = new NodeBuilder();
	
	if(defaultToSAX) {
	    parseXMLDataSAX(plistData, handler);
	}
	else {
	    /* First try to parse with the internal homebrew parser, and if it
	     * doesn't succeed, go for the SAX parser. */
	    //System.err.println("Trying to parse xml data...");
	    try {
		parseXMLDataAPX(plistData, handler);
		//System.err.println("xml data parsed...");
	    } catch(Exception e) {
		e.printStackTrace();
		System.err.println("APX parser threw exception... falling back to SAX parser. Report this error!");
		handler = new NodeBuilder();
		parseXMLDataSAX(plistData, handler);
	    }
	}
	
	XMLNode[] rootNodes = handler.getRoots();
	if(rootNodes.length != 1)
	    throw new RuntimeException("Could not parse DMG-file!");
	else
	    return rootNodes[0];
    }

    private void parseXMLDataAPX(byte[] buffer, NodeBuilder handler) {
	try {
	    ByteArrayStream ya = new ByteArrayStream(buffer);
	    SynchronizedRandomAccessStream bufferStream =
		new SynchronizedRandomAccessStream(ya);//new ByteArrayStream(buffer));
	    
	    // First we parse the xml declaration using a US-ASCII charset just to extract the charset description
	    //System.err.println("parsing encoding");
	    InputStream is = new RandomAccessInputStream(bufferStream);
	    APXParser encodingParser = APXParser.create(new InputStreamReader(is, "US-ASCII"),
					    new NullXMLContentHandler(Charset.forName("US-ASCII")));
	    String encodingName = encodingParser.xmlDecl();
	    //System.err.println("encodingName=" + encodingName);
	    if(encodingName == null)
		encodingName = "US-ASCII";
	    
	    Charset encoding = Charset.forName(encodingName);
	    
	    // Then we proceed to parse the entire document
	    is = new RandomAccessInputStream(bufferStream);
	    Reader usedReader = new BufferedReader(new InputStreamReader(is, encoding));
	    //System.err.println("parsing document");
	    //try { FileOutputStream dump = new FileOutputStream("dump.xml"); dump.write(buffer); dump.close(); }
	    //catch(Exception e) { e.printStackTrace(); }

	    if(false) { // 
		APXParser documentParser = APXParser.create(usedReader, new DebugXMLContentHandler(encoding));
		documentParser.xmlDocument();
		System.exit(0);
	    }
	    else {
		APXParser documentParser = APXParser.create(usedReader, new NodeBuilderContentHandler(handler, bufferStream, encoding));
		documentParser.xmlDocument();
	    }

	} catch(ParseException pe) {
	    //System.err.println("Could not read the partition list...");
	    throw new RuntimeException(pe);
	} catch(UnsupportedEncodingException uee) {
	    throw new RuntimeException(uee);
	}
    }
    
    private void parseXMLDataSAX(byte[] buffer, NodeBuilder handler) {
	try {
	    InputStream is = new ByteArrayInputStream(buffer);
	    SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
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
	    //System.err.println("Could not read the partition list... exiting.");
	    throw new RuntimeException(se);
	} catch(Exception e) {
	    throw new RuntimeException(e);
	}
    }
}
