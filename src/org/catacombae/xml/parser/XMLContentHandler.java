package org.catacombae.xml.parser;

import java.util.List;

public interface XMLContentHandler {
    //public void doctype(String
    public void xmlDecl(String version, String encoding, Boolean standalone);
    public void pi(String id, String content);
    public void comment(String comment);
    public void doctype(String name, ExternalID eid); // Needs a DTD description also
    public void cdata(String cdata);
    public void emptyElement(String name, List<Attribute> attributes);
    public void startElement(String name, List<Attribute> attributes);
    public void endElement(String name);
    public void chardata(String data);
    public void reference(String ref);
}