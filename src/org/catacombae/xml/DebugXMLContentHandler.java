package org.catacombae.xml;

import java.util.List;
import java.nio.charset.Charset;

public class DebugXMLContentHandler extends XMLContentHandler {
    public DebugXMLContentHandler(Charset encoding) {
	super(encoding);
    }
    //public void doctype(String
    public void xmlDecl(String version, String encoding, Boolean standalone) {
	print("xmlDecl: <?xml version=\"" + version + "\"");
	if(encoding != null)
	    print(" encoding=\"" + encoding + "\"");
	if(standalone != null) {
	    print(" standalone=\"");
	    if(standalone)
		print("yes");
	    else
		print("no");
	    print("\"");
	}
	println("?>");
    }
    public void pi(String id, String content) {
	print("pi: <?" + id);
	if(content != null)
	    print(" " + content);
	println("?>");
    }
    
    public void comment(String content) {
	println("comment: <!--" + content + "-->");
    }

    public void doctype(String name, ExternalID eid) {
	print("doctype: <!DOCTYPE " + name);
	if(eid != null) {
	    if(eid.type == eid.SYSTEM)
		print("SYSTEM \"" + eid.systemLiteral + "\"");
	    else if(eid.type == eid.PUBLIC)
		print("PUBLIC \"" + eid.pubidLiteral + "\" \"" + eid.systemLiteral + "\"");
	}
	println(">");
    }

    public void cdata(String cdata) {
	println("cdata: <![CDATA[" + cdata + "]]>");
    }

    public void emptyElement(String name, List<Attribute> attributes) {
	print("emptyElement: <" + name);
	for(Attribute attr : attributes)
	    print(" " + attr.identifier + "=\"" + attr.value + "\"");
	println("/>");
    }
    
    public void startElement(String name, List<Attribute> attributes) {
	print("startElement: <" + name);
	for(Attribute attr : attributes)
	    print(" " + attr.identifier + "=\"" + attr.value + "\"");
	println(">");
    }
    
    public void endElement(String name) {
	println("endElement: </" + name + ">");
    }

//     public void chardata(CharSequence data) {
// 	print("chardata: \"");
// 	for(int i = 0; i < data.length(); ++i)
// 	    print(data.charAt(i)+"");
// 	println("\"");
	   
//     }
    public void chardata(int beginLine, int beginColumn, int endLine, int endColumn) {
	println("chardata: starting at (" + beginLine + "," + beginColumn + ") and ending at (" + endLine + "," + endColumn + ")");
    }

    public void reference(String ref) {
	println("reference: \"" + ref + "\"");
    }
    
    private static void print(String s) {
	System.out.print(s);
    }
    private static void println(String s) {
	System.out.println(s);
    }
    
}