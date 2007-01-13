package org.catacombae.dmgx;

import java.io.*;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import org.xml.sax.SAXException;
//import org.xml.sax.helpers.DefaultHandler;
import org.catacombae.xml.parser.*;

public class Plist {
    private final byte[] plistData;
    private XMLNode rootNode;
    private boolean useSaxParser = false;
    
    public Plist(byte[] data) {
	this(data, 0, data.length);
    }
    
    public Plist(byte[] data, int offset, int length) {
	plistData = new byte[length];
	System.arraycopy(data, offset, plistData, 0, length);
    }
    
    public byte[] getData() { return Util.createCopy(plistData); }
    
    public XMLNode parseXMLData() {
	//InputStream is = new ByteArrayInputStream(plistData);
	NodeBuilder handler = new NodeBuilder();
	
	/* First try to parse with the internal homebrew parser, and if it
	 * doesn't succeed, go for the SAX parser. */
	try {
	    parseXMLDataAss(plistData, handler);
	} catch(Exception e) {
	    handler = new NodeBuilder();
	    parseXMLDataSAX(plistData, handler);
	}
	
	XMLNode[] rootNodes = handler.getRoots();
	if(rootNodes.length != 1)
	    throw new RuntimeException("Could not parse DMG-file!");
	else
	    return rootNodes[0];
    }

    private void parseXMLDataAss(byte[] buffer, NodeBuilder handler) {
	try {
	    InputStream is = new ByteArrayInputStream(buffer);
	    Ass encodingParser = Ass.create(new InputStreamReader(is, "US-ASCII"), new NullXMLContentHandler());
	    String encoding = encodingParser.xmlDecl();
	    //if(verbose) System.out.println("XML Encoding: " + encoding);
	    if(encoding == null)
		encoding = "US-ASCII";
	    
	    is = new ByteArrayInputStream(buffer);
	    Reader usedReader = new BufferedReader(new InputStreamReader(is, encoding));
	    Ass assParser = Ass.create(usedReader, new NodeBuilderContentHandler(handler));
	    assParser.xmlDocument();
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
