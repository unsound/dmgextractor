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
import java.io.UnsupportedEncodingException;
import org.catacombae.dmgextractor.Util;
import org.catacombae.dmg.udif.Koly;
import org.catacombae.dmg.udif.UDIFFileView;
import org.catacombae.io.ReadableFileStream;

/**
 * This is a console application that validates a set of dmg files
 * according to the currently known rules (implemented in Koly.validate).
 */
public class ValidateDmg {

    public static void main(String[] filenames) throws IOException {
        if(filenames.length == 0)
            System.out.println("No files to validate.");
        for(String fn : filenames) {
            System.out.println("Processing \"" + fn + "\"...");
            try {
                RandomAccessFile raf = new RandomAccessFile(fn, "r");
                UDIFFileView dfw =
                        new UDIFFileView(new ReadableFileStream(raf, fn));
                Koly koly = dfw.getKoly();
                ValidateResult vr = validateKoly(raf, koly);
                String[] errors = vr.getErrors();
                String[] warnings = vr.getWarnings();
                for(int i = 0; i < errors.length; ++i) {
                    if(i == 0)
                        System.out.println("  " + errors.length + " errors");
                    System.out.println("    " + errors[i].toString());
                }
                for(int i = 0; i < warnings.length; ++i) {
                    if(i == 0)
                        System.out.println("  " + warnings.length + " warnings:");
                    System.out.println("    " + warnings[i].toString());
                }
                dfw.getPlist(); // Creates a new Plist, which automatically parses the XML data (and validates it).
            } catch(Exception e) {
                e.printStackTrace();
            }
            System.out.println();
        }
    }

    public static ValidateResult validateKoly(RandomAccessFile sourceFile, Koly koly) throws IOException {
        /* Validates the data in the koly block, as much as we can validate it
         * given what we know about it.. */

        ValidateResult vr = new ValidateResult();

        // Check that the fourcc reads "koly"
        try {
            String fourCCString = new String(Util.toByteArrayBE(koly.getFourCC()), "US-ASCII");
            if(!fourCCString.equals("koly"))
                vr.addError("Invalid fourCC: \"" + fourCCString + "\" (should be \"koly\")");
        } catch(UnsupportedEncodingException uee) {
            throw new RuntimeException(uee); // This should never happen
        }

        // unknown1 has always been a certain byte-sequence in examples. checkit
        // 0000 0004 0000 0200 0000 0001 0000 0000 0000 0000 0000 0000 0000 0000
        byte[] previouslySeenString = { 0x0, 0x0, 0x0, 0x4, 0x0, 0x0, 0x20, 0x0,
            0x0, 0x0, 0x0, 0x1, 0x0, 0x0, 0x0, 0x0,
            0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
            0x0, 0x0, 0x0, 0x0 };
        if(!Util.arraysEqual(previouslySeenString, koly.getUnknown1()))
            vr.addWarning("unknown1 deviates from earlier observations: 0x" + Util.byteArrayToHexString(koly.getUnknown1()));

        long plistBegin1 = koly.getPlistBegin1();
        long plistBegin2 = koly.getPlistBegin2();
        long plistSize = koly.getPlistSize();

        // Plist addresses must be equal
        if(plistBegin1 != plistBegin2)
            vr.addError("The two plist addresses don't match (" + plistBegin1 + "!=" + plistBegin2 + ")");

        // Address to start of plist must be within file bounds.
        if(plistBegin1 > sourceFile.length())
            vr.addError("plistBegin1 out of bounds (pistBegin1: " + plistBegin1 + " file size: " + sourceFile.length() + ")");
        else {
            // There must be a plist at that address
            sourceFile.seek(plistBegin1);
            byte[] xmlIdentifier = new byte[5];
            sourceFile.read(xmlIdentifier);
            try {
                String xmlIdentifierString = new String(xmlIdentifier, "US-ASCII");
                if(!xmlIdentifierString.equals("<?xml")) {
                    vr.addError("No plist at plistBegin1!");
                }
            } catch(UnsupportedEncodingException uee) {
                throw new RuntimeException(uee);
            }
        }

        // If the begin addresses differ, let's do the same check for plistBegin2
        if(plistBegin1 != plistBegin2) {
            // Address to start of plist must be within file bounds.
            if(plistBegin2 > sourceFile.length())
                vr.addError("plistBegin2 out of bounds (pistBegin1: " + plistBegin2 + " file size: " + sourceFile.length() + ")");
            else {
                // There must be a plist at that address
                sourceFile.seek(plistBegin2);
                byte[] xmlIdentifier = new byte[5];
                sourceFile.read(xmlIdentifier);
                try {
                    String xmlIdentifierString = new String(xmlIdentifier, "US-ASCII");
                    if(!xmlIdentifierString.equals("<?xml")) {
                        vr.addError("No plist at plistBegin2!");
                    }
                } catch(UnsupportedEncodingException uee) {
                    throw new RuntimeException(uee);
                }
            }
        }

        // The plistSize must be within bounds...
        if(plistSize <= sourceFile.length() - 512 && plistSize > 0) {
            if(plistSize + plistBegin1 > sourceFile.length() - 512)
                vr.addError("plist dimensions outside file bounds! (plistSize: " + plistSize + " plistBegin1: " + plistBegin1 + " sourceFile.length()-512: " + (sourceFile.length() - 512));
            if(plistSize + plistBegin2 > sourceFile.length() - 512)
                vr.addError("plist dimensions outside file bounds! (plistSize: " + plistSize + " plistBegin2: " + plistBegin2 + " sourceFile.length()-512: " + (sourceFile.length() - 512));
        }
        else
            vr.addError("plist dimensions outside file bounds! (plistSize: " + plistSize + " sourceFile.length-512: " + (sourceFile.length() - 512));
        return vr;
    }
}
