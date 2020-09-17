/*-
 * Copyright (C) 2006 Erik Larsson
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
package org.catacombae.dmgextractor.utils;

import java.io.IOException;
import java.io.RandomAccessFile;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class DMGInfo {

    public static void main(String[] args) throws IOException {
        RandomAccessFile inRaf = new RandomAccessFile(args[0], "r");

        // Check opening signature "koly"
        inRaf.seek(inRaf.length() - 512);
        byte[] koly = new byte[4];
        inRaf.readFully(koly);
        String kolySignature = new String(koly, "US-ASCII");
        if(!kolySignature.equals("koly"))
            System.out.println("ERROR: Signature incorrect. Found \"" + kolySignature + "\" instead of \"koly\".");
        else
            System.out.println("\"koly\" signature OK.");

        // Read partition list start location 1 and end location
        inRaf.seek(inRaf.length() - 0x1E0);

        // -0x1E0: address to plist xml structure (8 bytes)
        long plistAddress1 = inRaf.readLong();
        System.out.println("Address to plist: 0x" + Long.toHexString(plistAddress1));


        // -0x1D8: address to end of plist xml structure (8 bytes)
        long plistEndAddress = inRaf.readLong();
        System.out.println("Address to end of plist: 0x" + Long.toHexString(plistEndAddress));
        System.out.println("  Implication: plist size = " + (plistEndAddress - plistAddress1) + " B");


        long unknown_0x1D0 = inRaf.readLong();


        long unknown_0x1C8 = inRaf.readLong();
        if(unknown_0x1C8 != 0x0000000100000001L)
            System.out.println("Assertion failed! unknown_0x1C8 == 0x" +
                    Long.toHexString(unknown_0x1C8) + " and not 0x0000000100000001");


        long unknown_0x1C0 = inRaf.readLong();


        long unknown_0x1B8 = inRaf.readLong();
        System.out.println("Some kind of signature? Value: 0x" + Long.toHexString(unknown_0x1B8));


        long unknown_0x1B0 = inRaf.readLong();
        if(unknown_0x1B0 != 0x0000000200000020L)
            System.out.println("Assertion failed! unknown_0x1B0 == 0x" +
                    Long.toHexString(unknown_0x1B0) + " and not 0x0000000200000020");


        int unknown_0x1A8 = inRaf.readInt();


        int unknown_0x1A4 = inRaf.readInt();
        System.out.println("Some kind of unit size? Value: 0x" +
                Integer.toHexString(unknown_0x1A4) + " / " + unknown_0x1A4);


        // Unknown chunk of data (120 bytes)
        byte[] unknown_0x1A0 = new byte[120];
        inRaf.readFully(unknown_0x1A0);


        // -0x128: address to beginning of plist xml structure (second occurrence) (8 bytes)
        long plistAddress2 = inRaf.readLong();
        System.out.println("Address to plist (2): 0x" + Long.toHexString(plistAddress2));


        // -0x120: size of plist xml structure (8 bytes)
        long plistSize = inRaf.readLong();
        System.out.println("plist size: " + plistSize + " B");


        // Unknown chunk of data (120 bytes)
        byte[] unknown_0x118 = new byte[120];
        inRaf.readFully(unknown_0x118);


        // -0x0A0: Checksum type identifier (4 bytes)
        System.out.print("Checksum type");
        int cs_type = inRaf.readInt();
        if(cs_type == 0x00000002)
            System.out.println(": CRC-32");
        else if(cs_type == 0x00000004)
            System.out.println(": MD5");
        else
            System.out.println(" unknown! Data: 0x" + Integer.toHexString(cs_type));


        // -0x09C: Length of checksum in bits (4 bytes)
        int cs_length = inRaf.readInt();
        System.out.println("Checksum length: " + cs_length + " bits");


        // -0x098: Checksum ((cs_length/8) bytes)
        byte[] checksum = new byte[cs_length / 8];
        inRaf.readFully(checksum);
        System.out.println("Checksum: 0x" + byteArrayToHexString(checksum).toUpperCase());

    /*
    if(unknown_0x != 0xL)
    System.out.println("Assertion failed! unknown_0x == 0x" + Long.toHexString(unknown_0x) + " and not 0xL");
     */
    }

    public static String byteArrayToHexString(byte[] array) {
        StringBuilder result = new StringBuilder();
        for(byte b : array) {
            String s = Integer.toHexString(b & 0xFF);
            if(s.length() == 1)
                s = "0" + s;
            result.append(s);
        }
        return result.toString();
    }
}

class DMGInfoFrame extends JFrame {

    private JTabbedPane mainPane;

    public DMGInfoFrame() {
        super("DMGInfo");

        mainPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

        StatisticsPanel statisticsPanel;
        statisticsPanel = new StatisticsPanel();
    //mainPane.addTab(statisticsPanel, "Statistics");
    }
}

class StatisticsPanel extends JPanel {

    JPanel blocktypeCountPanel;

    public StatisticsPanel(/*DMGFile dmgFile*/) {
    }
}

/*
class DMGFile extends RandomAccessFile {
public DMGFile(File file, String mode) {
super(file, mode);
}
public DMGFile(String name, String mode) {
super(name, mode);
}
}
 */
