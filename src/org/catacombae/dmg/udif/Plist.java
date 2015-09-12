/*-
 * Copyright (C) 2006-2008 Erik Larsson
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

package org.catacombae.dmg.udif;

import java.io.*;
import java.nio.charset.Charset;
import java.util.LinkedList;
import net.iharder.base64.Base64;
import org.catacombae.dmgextractor.Util;
import org.catacombae.dmgextractor.io.*;
import org.catacombae.plist.PlistNode;
import org.catacombae.plist.XmlPlist;

public class Plist extends XmlPlist {

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
        super(data, offset, length, useSAXParser);
    }

    //public byte[] getData() { return Util.createCopy(plistData); }

    public PlistPartition[] getPartitions() throws IOException {
	LinkedList<PlistPartition> partitionList = new LinkedList<PlistPartition>();
	PlistNode current = getRootNode();
	current = current.cd("dict");
	current = current.cdkey("resource-fork");
	current = current.cdkey("blkx");

	// Variables to keep track of the pointers of the previous partition
	long previousOutOffset = 0;
	long previousInOffset = 0;

	// Iterate over the partitions and gather data
        for(PlistNode pn : current.getChildren()) {
            String partitionName = Util.readFully(pn.getKeyValue("Name"));
            String partitionID = Util.readFully(pn.getKeyValue("ID"));
            String partitionAttributes = Util.readFully(pn.getKeyValue("Attributes"));
            //System.err.println("Retrieving data...");
            //(new BufferedReader(new InputStreamReader(System.in))).readLine();
            Reader base64Data = pn.getKeyValue("Data");
            //System.gc();
            //System.err.println("Converting data to binary form... free memory: " + Runtime.getRuntime().freeMemory() + " total memory: " + Runtime.getRuntime().totalMemory());
            //byte[] data = Base64.decode(base64Data);

//             try {
//                 InputStream yo = new Base64.InputStream(new ReaderInputStream(base64Data, Charset.forName("US-ASCII")));
//                 String filename1 = "dump_plist_java-" + System.currentTimeMillis() + ".datadpp";
//                 System.err.println("Dumping output from ReaderInputStream to file \"" + filename1 + "\"");
//                 FileOutputStream fos = new FileOutputStream(filename1);
//                 if(false) { // Standard way
//                     byte[] buffer = new byte[4096];
//                     int curBytesRead = yo.read(buffer);
//                     while(curBytesRead == buffer.length) {
//                         fos.write(buffer, 0, curBytesRead);
//                         curBytesRead = yo.read(buffer);
//                     }
//                     if(curBytesRead > 0)
//                         fos.write(buffer, 0, curBytesRead);
//                 }
//                 else { // Simulating PlistPartition constructor
//                     byte[] buf1 = new byte[0xCC];
//                     byte[] buf2 = new byte[0x28];
//                     int curBytesRead = (int)yo.skip(0xCC); // SKIP OPERATION FUCKS UP!one
//                     fos.write(buf1, 0, curBytesRead);
//                     curBytesRead = yo.read(buf2);
//                     while(curBytesRead == buf2.length) {
//                         fos.write(buf2, 0, curBytesRead);
//                         curBytesRead = yo.read(buf2);
//                     }
//                     if(curBytesRead > 0)
//                         fos.write(buf2, 0, curBytesRead);

//                 }
//                 fos.close();
//             } catch(Exception e) { e.printStackTrace(); }

            InputStream base64DataInputStream = new Base64.InputStream(new ReaderInputStream(base64Data, Charset.forName("US-ASCII")));

            //System.err.println("Creating PlistPartition.");
            //System.out.println("Block list for partition " + i++ + ":");
            PlistPartition dpp = new PlistPartition(partitionName, partitionID, partitionAttributes,
                                                          base64DataInputStream, previousOutOffset, previousInOffset);
            previousOutOffset = dpp.getFinalOutOffset();
            previousInOffset = dpp.getFinalInOffset();
            partitionList.addLast(dpp);
        }

	return partitionList.toArray(new PlistPartition[partitionList.size()]);
    }
}
