package org.catacombae.xml;

import java.util.List;
import java.nio.charset.Charset;

public abstract class XMLContentHandler {
    protected Charset encoding;
    public XMLContentHandler(Charset encoding) {
	this.encoding = encoding;
    }
    //public void doctype(String
    public abstract void xmlDecl(String version, String encoding, Boolean standalone);
    public abstract void pi(String id, String content);
    public abstract void comment(String comment);
    public abstract void doctype(String name, ExternalID eid); // Needs a DTD description also
    public abstract void cdata(String cdata);
    public abstract void emptyElement(String name, List<Attribute> attributes);
    public abstract void startElement(String name, List<Attribute> attributes);
    public abstract void endElement(String name);
    public abstract void chardata(int beginLine, int beginColumn, int endLine, int endColumn);
    public abstract void reference(String ref);
}