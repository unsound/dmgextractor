package org.catacombae.xml;

import java.util.List;
import java.nio.charset.Charset;

public class NullXMLContentHandler extends XMLContentHandler {
    public NullXMLContentHandler(Charset encoding) {
	super(encoding);
    }
    public void xmlDecl(String version, String encoding, Boolean standalone) {}
    public void pi(String id, String content) {}
    public void comment(String comment) {}
    public void doctype(String name, ExternalID eid) {} // Needs a DTD description also
    public void cdata(String cdata) {}
    public void emptyElement(String name, List<Attribute> attributes) {}
    public void startElement(String name, List<Attribute> attributes) {}
    public void endElement(String name) {}
    public void chardata(int beginLine, int beginColumn, int endLine, int endColumn) {}
    public void reference(String ref) {}
}