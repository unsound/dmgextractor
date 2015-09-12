/*-
 * Copyright (C) 2006-2011 Erik Larsson
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catacombae.plist;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import org.catacombae.dmgextractor.io.ConcatenatedReader;
import org.catacombae.io.RuntimeIOException;
import org.catacombae.util.Util;
import org.catacombae.xml.XMLElement;
import org.catacombae.xml.XMLNode;
import org.catacombae.xml.XMLText;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public class XmlPlistNode extends PlistNode {
    private final XMLNode xmlNode;

    public XmlPlistNode(XMLNode xmlNode) {
        this.xmlNode = xmlNode;
    }

    private XMLNode getXMLNode() {
        return xmlNode;
    }

    private String[] getKeys() throws RuntimeIOException {
        final LinkedList<String> keyList = new LinkedList<String>();

        for(XMLElement xe : xmlNode.getChildren()) {
            if(xe instanceof XMLNode) {
                XMLNode xn = (XMLNode) xe;
                if(xn.qName.equals("key")) {
                    for(XMLElement xeChild : xn.getChildren()) {
                        if(xeChild instanceof XMLText) {
                            final XMLText xtChild = (XMLText) xeChild;
                            String key;

                            try {
                                key = Util.readFully(xtChild.getText());
                            } catch(IOException e) {
                                throw new RuntimeIOException(e);
                            }

                            keyList.addLast(key);
                        }
                    }
                }
            }
        }

        return keyList.toArray(new String[keyList.size()]);
    }

    public PlistNode[] getChildren() {
        final LinkedList<PlistNode> children = new LinkedList<PlistNode>();

        if(xmlNode.qName.equals("dict")) {
            for(String key : getKeys()) {
                children.add(cdkey(key));
            }
        }
        else if(xmlNode.qName.equals("array")) {
            for(XMLElement xe : xmlNode.getChildren()) {
                if(xe instanceof XMLNode) {
                    children.add(new XmlPlistNode((XMLNode) xe));
                }
                else if(xe instanceof XMLText) {
                    String text = "";
                    try {
                        text = Util.readFully(((XMLText) xe).getText());
                    } catch(IOException ex) {
                        ex.printStackTrace();
                    }
                    throw new RuntimeException("Unexpected text inside array " +
                            "plist element: \"" + text + "\"");
                }
                else {
                    System.err.println(xe.toString());
                    throw new RuntimeException("Unexpected element inside " +
                            "array: " + xe);
                }
            }
        }
        else {
            throw new RuntimeException("getChildren called for " +
                    "non-dict/array type \"" + xmlNode.qName + "\".");
        }

        return children.toArray(new PlistNode[children.size()]);
    }

    /**
     * The concept of "changing directory" in a tree is perhaps not a
     * perfect way to describe things. But this method will look up the
     * first subnode of our node that is of the type <code>type</code>
     * and return it.
     * If you have more than one of the same type, tough luck. You only
     * get the first.
     */
    public PlistNode cd(String type) {
        for(XMLElement xn : xmlNode.getChildren()) {
            if(xn instanceof XMLNode && ((XMLNode)xn).qName.equals(type))
                return new XmlPlistNode((XMLNode)xn);
        }
        return null;
    }

    /**
     * This is different from the <code>cd</code> method in that it
     * searches for a node of the type "key", and looks up the <code>
     * XMLText</code> within. It then compares the text with the String
     * <code>key</code>. If they match, it returns the node coming
     * after the key node. Else it continues to search. If no match is
     * found, <code>null</code> is returned.
     */
    public PlistNode cdkey(String key) {
        return cdkeyXml(key);
    }

    private XmlPlistNode cdkeyXml(String key) {
        boolean keyFound = false;
        for(XMLElement xn : xmlNode.getChildren()) {
            if(xn instanceof XMLNode) {
                if(keyFound)
                    return new XmlPlistNode((XMLNode)xn);

                else if(((XMLNode)xn).qName.equals("key")) {
                    for(XMLElement xn2 : ((XMLNode)xn).getChildren()) {
                        try {
                            if(xn2 instanceof XMLText) {
                                String s = Util.readFully(((XMLText)xn2).getText());
                                //System.err.println("cdkey searching: \"" + s + "\"");
                                if(s.equals(key))
                                   keyFound = true;
                            }
                        } catch(Exception e) { throw new RuntimeException(e); }
                    }
                }
            }
        }
        return null;
    }

    public Reader getKeyValue(String key) {
        //System.out.println("XMLNode.getKeyValue(\"" + key + "\")");
        XmlPlistNode keyNode = cdkeyXml(key);
        if(keyNode == null)
            return null;

        XMLElement[] nodeChildren = keyNode.getXMLNode().getChildren();
        if(nodeChildren.length != 1) {
            //System.out.println("  nodeChildren.length == " + nodeChildren.length);

            LinkedList<Reader> collectedReaders = new LinkedList<Reader>();
            for(XMLElement xe : keyNode.getXMLNode().getChildren()) {
                if(xe instanceof XMLText) {
                    try {
                        Reader xt = ((XMLText)xe).getText();
                        collectedReaders.addLast(xt);
                    } catch(Exception e) { throw new RuntimeException(e); }
                    //System.out.print("\"");
                    //for(int i = 0; i < xt.length(); ++i) System.out.print(xt.charAt(i));
                    //System.out.println("\"");
                    //System.out.println("free memory: " + Runtime.getRuntime().freeMemory() + " total memory: " + Runtime.getRuntime().totalMemory());
                }
            }
            ConcatenatedReader result;
            if(collectedReaders.size() == 0)
                result = null;
            else {
                //System.out.println("doing a toString... free memory: " + Runtime.getRuntime().freeMemory() + " total memory: " + Runtime.getRuntime().totalMemory());
                //result = returnString.toString();
                //System.out.println("done.free memory: " + Runtime.getRuntime().freeMemory() + " total memory: " + Runtime.getRuntime().totalMemory());
                result = new ConcatenatedReader(collectedReaders.toArray(new Reader[collectedReaders.size()]));
            }
            return result;
        }
        else if(nodeChildren[0] instanceof XMLText) {
            //System.err.println("Special case!");
            try {
                return ((XMLText)nodeChildren[0]).getText();
            } catch(Exception e) { throw new RuntimeException(e); }
        }
        else
            return null;
    }
}
