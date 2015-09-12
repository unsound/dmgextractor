/*-
 * Copyright (C) 2006-2008 Erik Larsson
 *           (C) 2004 vu1tur (not the actual java code, but the C-code which
 *                            has been used for reference)
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

package org.catacombae.dmgextractor;

import org.catacombae.dmg.udif.Debug;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Collections;
import javax.swing.JOptionPane;
import org.catacombae.dmg.encrypted.ReadableCEncryptedEncodingStream;
import org.catacombae.dmg.sparsebundle.ReadableSparseBundleStream;
import org.catacombae.dmg.sparseimage.ReadableSparseImageStream;
import org.catacombae.dmg.sparseimage.SparseImageRecognizer;
import org.catacombae.io.FileStream;
import org.catacombae.io.ReadableFileStream;
import org.catacombae.io.ReadableRandomAccessStream;
import org.catacombae.io.TruncatableRandomAccessStream;
import org.catacombae.dmg.udif.Koly;
import org.catacombae.dmg.udif.Plist;
import org.catacombae.dmg.udif.PlistPartition;
import org.catacombae.dmg.udif.UDIFBlock;
import org.catacombae.dmg.udif.UDIFDetector;
import org.catacombae.io.RuntimeIOException;
import org.catacombae.io.SynchronizedReadableRandomAccessStream;
import org.xml.sax.XMLReader;

public class DMGExtractor {
    public static final String BASE_APP_NAME = "DMGExtractor";
    public static final String VERSION = "0.70";
    
    public static final String APPNAME = BASE_APP_NAME + " " + VERSION;
    public static final String BUILDSTRING = "(Build #" + BuildNumber.BUILD_NUMBER + ")";
    public static final String[] COPYRIGHT_MESSAGE = new String[] {
        APPNAME + " " + BUILDSTRING,
        "Copyright \u00A9 2006-2008 Erik Larsson <erik82@kth.se>",
        "  based on dmg2iso, Copyright \u00A9 2004 vu1tur <v@vu1tur.eu.org>",
        "  using the libraries:",
        "    iHarder Base64 Encoder/Decoder <http://iharder.sf.net>",
        "      released into the public domain",
        "    Apache Ant bzip2 library <http://ant.apache.org/>",
        "      released under The Apache Software License Version 2.0",
        "",
        "This program is distributed under the GNU Lesser General Public ",
        "License (LGPL) version 3.",
        "See <http://www.gnu.org/licenses/lgpl-3.0.html> for the details.",
        ""
    };

    /**
     * Contains settings variables for a DMGExtractor session.
     */
    private static class Session {
        public String parseArgsErrorMessage = null;
        public boolean useSaxParser = false;
        public boolean verbose = false;
        public boolean debug = false;
        public boolean graphical = false;
        public String startupCommand = "java DMGExtractor";
        public File dmgFile = null;
        public File isoFile = null;
    }
    
    public static void main(String[] args) throws Exception {
        Session ses = null;
        try {
            ses = parseArgs(args);

            if(ses.debug)
                ses.verbose = true;

            final UserInterface ui;
            if(ses.graphical)
                ui = new SwingUI(ses.verbose);
            else
                ui = new TextModeUI(ses.verbose);

            ui.displayMessage(COPYRIGHT_MESSAGE);
            if(ses.parseArgsErrorMessage == null) {
                if(ses.graphical) {
                    if(ses.dmgFile == null) {
                        ses.dmgFile = ui.getInputFileFromUser();
                    }
                    if(ses.dmgFile != null && ses.isoFile == null) {
                        if(ui.getOutputConfirmationFromUser()) {
                            ses.isoFile = ui.getOutputFileFromUser(ses.dmgFile);
                            if(ses.isoFile == null)
                                System.exit(0);
                        }
                    }
                }

                if(ses.dmgFile != null) {
                    String dmgFilename = null;
                    String isoFilename = null;

                    if(ses.dmgFile != null)
                        dmgFilename = ses.dmgFile.getName();
                    if(ses.isoFile != null)
                        isoFilename = ses.isoFile.getName();

                    ui.setProgressFilenames(dmgFilename, isoFilename);
                    if(!extractProcedure(ses, ui)) {
                        System.exit(1);
                    }
                    else {
                        System.exit(0);
                    }
                }
                else if(!ses.graphical)
                    printUsageInstructions(ui, ses.startupCommand,
                            "Error: No input file specified.");
            }
            else
                printUsageInstructions(ui, ses.startupCommand, ses.parseArgsErrorMessage);
            
        } catch(Exception e) {
            if(ses != null && ses.graphical) {
                String stackTrace = e.toString() + "\n";
                for(StackTraceElement ste : e.getStackTrace())
                    stackTrace += "    " + ste.toString() + "\n";
                JOptionPane.showMessageDialog(null, "The program encountered an uncaught exception:\n" + stackTrace +
                        "\nCan not recover. Exiting...", "Error", JOptionPane.ERROR_MESSAGE);
            }
            throw e;
        }
    }
 
    private static boolean extractProcedure(Session ses, UserInterface ui)
            throws Exception
    {
        
        ui.displayMessageVerbose("Processing: \"" + ses.dmgFile + "\"");

        ReadableRandomAccessStream dmgRaf = null;

        final boolean sparseBundle;
        if(ses.dmgFile.isDirectory()) {
            ReadableSparseBundleStream sbStream = null;
            try {
                sbStream = new ReadableSparseBundleStream(ses.dmgFile);
            } catch(RuntimeIOException e) {
                /* Not a sparse bundle, apparently. */
            }

            if(sbStream != null) {
                dmgRaf = sbStream;
                sparseBundle = true;
            }
            else {
                sparseBundle = false;
            }
        }
        else {
            sparseBundle = false;
        }

        if(dmgRaf == null) {
            dmgRaf = new ReadableFileStream(new RandomAccessFile(ses.dmgFile,
                    "r"));
        }

        final boolean encrypted;
        if(ReadableCEncryptedEncodingStream.isCEncryptedEncoding(dmgRaf)) {
            encrypted = true;
            char[] password;
            while(true) {
                password = ui.getPasswordFromUser();
                if(password == null) {
                    ui.displayMessage("No password specified. Can not continue...");
                    dmgRaf.close();
                    return true;
                }
                try {
                    ReadableCEncryptedEncodingStream encryptionFilter =
                            new ReadableCEncryptedEncodingStream(dmgRaf, password);
                    dmgRaf = encryptionFilter;
                    break;
                } catch(Exception e) {
                    ui.displayMessage("Incorrect password!");
                }
            }
        }
        else
            encrypted = false;

        boolean sparseImage = false;
        if(!sparseBundle && SparseImageRecognizer.isSparseImage(dmgRaf)) {
            ReadableSparseImageStream sparseImageStream =
                    new ReadableSparseImageStream(dmgRaf);
            dmgRaf = sparseImageStream;
            sparseImage = true;
        }

        TruncatableRandomAccessStream isoRaf = null;
        if(ses.isoFile != null) {
            isoRaf = new FileStream(ses.isoFile);
            isoRaf.setLength(0);
            ui.displayMessageVerbose("Extracting to: \"" + ses.isoFile + "\"");
        }
        else {
            ui.displayMessageVerbose("Simulating extraction...");
        }

        final boolean result;

        if(!UDIFDetector.isUDIFEncoded(dmgRaf)) {
            if(!sparseBundle && !encrypted && !sparseImage &&
                    !ui.warning("The image you selected does not seem to be " +
                    "UDIF encoded, sparse or encrypted.",
                    "Its contents will be copied unchanged to the " +
                    "destination."))
            {
                result = false;
            }
            else {
                copyData(dmgRaf, isoRaf, ui);
                result = true;
            }
        }

        else {
            extractUDIF(dmgRaf, isoRaf, ui, ses);
            result = true;
        }

        if(isoRaf != null) {
            isoRaf.close();
        }

        dmgRaf.close();

        return result;
    }

    private static void extractUDIF(ReadableRandomAccessStream dmgRaf,
            TruncatableRandomAccessStream isoRaf, UserInterface ui, Session ses)
            throws Exception
    {
        final boolean testOnly = ses.isoFile == null;

        Koly koly;
        {
            dmgRaf.seek(dmgRaf.length() - Koly.length());
            byte[] kolyData = new byte[512];
            int kolyDataRead = dmgRaf.read(kolyData);
            if(kolyDataRead != kolyData.length)
                throw new RuntimeException("Could not read koly completely. Read " + kolyDataRead + "/" +
                        kolyData.length + " bytes.");
            else
                koly = new Koly(kolyData, 0);
        }

        if(ses.debug) {
            ui.displayMessage("plist addresses:",
                    "  " + koly.getPlistBegin1(),
                    "  " + koly.getPlistBegin2());
        }
        if(koly.getPlistBegin1() != koly.getPlistBegin2()) {
            ui.displayMessage("WARNING: Addresses not equal! Assumption false.",
                    koly.getPlistBegin1() + " != " + koly.getPlistBegin2());
        //System.exit(0);
        }
        // if(false && plistSize != (plistEnd-plistBegin1)) { // This assumption is proven false. plistEnd means something else
        //     println("NOTE: plistSize field does not match plistEnd marker. Assumption false.",
        // 	    "plistSize=" + plistSize + " plistBegin1=" + plistBegin1 + " plistEnd=" + plistEnd + " plistEnd-plistBegin1=" + (plistEnd-plistBegin1));
        // }
        ui.displayMessageVerbose("Jumping to address...");
        dmgRaf.seek(koly.getPlistBegin1());
        final long plistSize = koly.getPlistSize();
        if(plistSize > Integer.MAX_VALUE)
            throw new RuntimeException("getPlistSize() way too large (" + plistSize + ")!");
        else if(plistSize < 0)
            throw new RuntimeException("getPlistSize() way too small (" + plistSize + ")!");
        byte[] buffer = new byte[(int) koly.getPlistSize()];
        dmgRaf.read(buffer);

        Plist plist = new Plist(buffer, ses.useSaxParser);
        PlistPartition[] partitions = plist.getPartitions();

        long totalOutSize = 0;
        for(PlistPartition p : partitions) {
            Iterator<UDIFBlock> blockIt = p.getBlockIterator();
            while(blockIt.hasNext())
                totalOutSize += blockIt.next().getOutSize();
        }
        ui.displayMessageVerbose("Target size: " + totalOutSize + " bytes");
        ui.setTotalProgressLength(totalOutSize);

        byte[] zeroblock = new byte[4096];
        Util.zero(zeroblock);

        int partitionNumber = 0;
        int errorsReported = 0;
        int warningsReported = 0;
        long totalSize = 0;
        ui.reportProgress(0);
        for(PlistPartition dpp : partitions) {
            long partitionSize = dpp.getPartitionSize();
            totalSize += partitionSize;

            ui.displayMessageVerbose("  " + dpp.getName(),
                    "    ID: " + dpp.getID(),
                    "    Attributes: " + dpp.getAttributes(),
                    "    Partition map block count: " + dpp.getBlockCount(),
                    "    Partition size: " + partitionSize + " bytes");

            int blockCount = 0;
            Iterator<UDIFBlock> blockIterator = dpp.getBlockIterator();
            while(blockIterator.hasNext()) {
                if(ui.cancelSignaled())
                    return;
                UDIFBlock currentBlock = blockIterator.next();

                /* Offset of the input data for the current block in the input file */
                final int blockType = currentBlock.getBlockType();
                /* Offset of the input data for the current block in the input file */
                final long inOffset = currentBlock.getTrueInOffset();
                /* Size of the input data for the current block */
                final long inSize = currentBlock.getInSize();
                /* Offset of the output data for the current block in the output file */
                final long outOffset = currentBlock.getTrueOutOffset();
                /* Size of the output data (possibly larger than inSize because of
                decompression, zero expansion...) */
                final long outSize = currentBlock.getOutSize();

                final long trueOutOffset = currentBlock.getTrueOutOffset();
                final long trueInOffset = currentBlock.getTrueInOffset();
                final String blockTypeString = currentBlock.getBlockTypeAsString();

                /*
                String[] variableStatus = {"outOffset=" + outOffset + " outSize=" + outSize,
                "inOffset=" + inOffset + " inSize=" + inSize,
                "trueOutOffset=" + trueOutOffset + " trueInOffset=" + trueInOffset};
                 */

                if(ses.debug) {
                    ui.displayMessage(
                            "      " + partitionNumber + ":" + blockCount + ". " + blockTypeString + " processing...",
                            "        outOffset=" + outOffset + " outSize=" + outSize,
                            "        inOffset=" + inOffset + " inSize=" + inSize,
                            "        trueOutOffset=" + trueOutOffset + " trueInOffset=" + trueInOffset);
                }
                else
                    ui.displayMessageVerbose("      Processing " + blockTypeString +
                            " block. In: " + inSize +
                            " bytes. Out: " + outSize + " bytes.");

                if(!testOnly && isoRaf.getFilePointer() != trueOutOffset) {
                    ++warningsReported;
                    boolean proceed = ui.warning(blockTypeString +
                            " FP != trueOutOffset (" + isoRaf.getFilePointer() +
                            " != " + trueOutOffset + ")");

                    if(!proceed)
                        return;
                }


                if(blockType == UDIFBlock.BT_ADC) {
                    ++errorsReported;

                    if(extractionError(ui, testOnly, "BT_ADC not supported."))
                        break;
                    else
                        return;
                }
                else if(blockType == UDIFBlock.BT_ZLIB) {
                    try {
                        DMGBlockHandlers.processBlock(currentBlock, dmgRaf, isoRaf, testOnly, ui);
                    } catch(DmgException de) {
                        de.printStackTrace();
                        String[] message = { "BT_ZLIB Could not decode..." };

                        ++errorsReported;
                        if(!ses.debug) {
                            String[] appended = { "outOffset=" + outOffset + " outSize=" + outSize,
                                "inOffset=" + inOffset + " inSize=" + inSize,
                                "trueOutOffset=" + trueOutOffset + " trueInOffset=" + trueInOffset };
                            message = Util.concatenate(message, appended);
                        }

                        if(extractionError(ui, testOnly, message))
                            break;
                        else
                            return;
                    }
                }
                else if(blockType == UDIFBlock.BT_BZIP2) {
                    DMGBlockHandlers.processBlock(currentBlock, dmgRaf, isoRaf, testOnly, ui);
                }
                else if(blockType == UDIFBlock.BT_COPY) {
                    DMGBlockHandlers.processBlock(currentBlock, dmgRaf, isoRaf, testOnly, ui);
                }
                else if(blockType == UDIFBlock.BT_ZERO) {
                    DMGBlockHandlers.processBlock(currentBlock, dmgRaf, isoRaf, testOnly, ui);
                }
                else if(blockType == UDIFBlock.BT_ZERO2) {
                    DMGBlockHandlers.processBlock(currentBlock, dmgRaf, isoRaf, testOnly, ui);
                }
                else if(blockType == UDIFBlock.BT_UNKNOWN) {
                    /* I have no idea what this blocktype is... but it's common, and usually
                    doesn't appear more than 2-3 times in a dmg. As long as its input and
                    output sizes are 0, there's no reason to complain... is there? */
                    if(!(inSize == 0 && outSize == 0)) {
                        String[] message = { "Blocktype BT_UNKNOWN had non-zero sizes...",
                            "  inSize=" + inSize + ", outSize=" + outSize,
                            "  Please contact the author of the program to report this bug!" };

                        ++errorsReported;
                        if(!ses.debug) {
                            String[] appended = { "outOffset=" + outOffset + " outSize=" + outSize,
                                "inOffset=" + inOffset + " inSize=" + inSize,
                                "trueOutOffset=" + trueOutOffset + " trueInOffset=" + trueInOffset };
                            message = Util.concatenate(message, appended);
                        }

                        if(extractionError(ui, testOnly, message))
                            break;
                        else
                            return;
                    }
                }
                else if(blockType == UDIFBlock.BT_END) {
                    // Nothing needs to be done in this pass.
                }
                else {
                    if(inSize == 0 && outSize == 0) {
                        ui.warning("previously unseen blocktype " + blockType + " [0x" + Integer.toHexString(blockType) + "]",
                                ("outOffset=" + outOffset + " outSize=" + outSize +
                                " inOffset=" + inOffset + " inSize=" + inSize),
                                "As inSize and outSize is 0 (block is a marker?), we can try to continue the operation...");
                        ++warningsReported;
                    }
                    else {
                        String[] message = { "previously unseen blocktype " + blockType + " [0x" + Integer.toHexString(blockType) + "]",
                            "outOffset=" + outOffset + " outSize=" + outSize + " inOffset=" + inOffset + " inSize=" + inSize,
                            "CRITICAL. inSize and/or outSize are not 0!" };
                        //errorMessage("previously unseen blocktype " + blockType + " [0x" + Integer.toHexString(blockType) + "]",
                        //       ("  outOffset=" + outOffset + " outSize=" + outSize +
                        //        " inOffset=" + inOffset + " inSize=" + inSize),
                        //        "  CRITICAL. inSize and/or outSize are not 0!");
                        ++errorsReported;
                        if(!ses.debug) {
                            String[] appended = { "outOffset=" + outOffset + " outSize=" + outSize,
                                "inOffset=" + inOffset + " inSize=" + inSize,
                                "trueOutOffset=" + trueOutOffset + " trueInOffset=" + trueInOffset };
                            message = Util.concatenate(message, appended);
                        }

                        if(extractionError(ui, testOnly, message))
                            break;
                        else
                            return;
                    }

                }

                ++blockCount;
            }
            ++partitionNumber;
        }

        ui.reportProgress(100);
        ui.reportFinished(isoRaf == null, errorsReported, warningsReported, totalSize);

        if(ses.debug) {
            ConcatenatedIterator<UDIFBlock> cit = new ConcatenatedIterator<UDIFBlock>();
            for(PlistPartition dpp : partitions)
                cit.add(dpp.getBlockIterator());

            LinkedList<UDIFBlock> blocks = new LinkedList<UDIFBlock>();
            while(cit.hasNext()) {
                UDIFBlock b = cit.next();
                if(b.getInSize() == 0)
                    continue; // Not relevant to the calculation
                else if(b.getInSize() > 0)
                    blocks.add(b);
                else
                    throw new RuntimeException("Negative inSize! inSize=" + b.getInSize());
            }
            Collections.sort(blocks);

            LinkedList<UDIFBlock> merged = mergeBlocks(blocks.iterator());

            String[] mergedRegions = new String[] { "Merged regions (size: " + merged.size() + "):" };
            for(UDIFBlock b : merged)
                Util.concatenate(mergedRegions, "  " + b.getTrueInOffset() + " - " +
                        (b.getTrueInOffset() + b.getInSize()));
            Util.concatenate(mergedRegions, "", "Extracting the regions not " +
                    "containing block data from source file...");
            ui.displayMessage(mergedRegions);
            int i = 1;
            Iterator<UDIFBlock> mergedIt = merged.iterator();
            UDIFBlock previous = null;
            if(merged.size() > 0 && merged.getFirst().getTrueInOffset() == 0)
                previous = mergedIt.next();

            while(mergedIt.hasNext() || previous != null) {
                UDIFBlock b = null;
                if(mergedIt.hasNext())
                    b = mergedIt.next();
                //else
                //    b =
                if(b == null || b.getTrueInOffset() > 0) {
                    long offset;
                    int size;
                    if(previous == null) {
                        offset = 0;
                        size = (int) (b.getInOffset());
                        if(size == 0)
                            continue; // First part may be empty, then we just continue
                    }
                    else if(b == null) {
                        offset = previous.getTrueInOffset() + previous.getInSize();
                        size = (int) (dmgRaf.length() - offset);
                        if(size == 0)
                            break; // Last part may be empty (in theory, though not in practice with true UDIF files)
                    }
                    else {
                        offset = previous.getTrueInOffset() + previous.getInSize();
                        size = (int) (b.getInOffset() - offset);
                    }

                    String filename = "[" + ses.dmgFile.getName() + "]-" + i++ + "-[" + offset + "-" + (offset + size) + "].region";
                    ui.displayMessage("  " + new File(filename).getCanonicalPath() + " (" + size + " bytes)...");
                    FileOutputStream curFos = new FileOutputStream(new File(filename));

                    if(size < 0) {
                        ui.error("ERROR: Negative array size (" + size + ")...",
                                "  current:",
                                "    " + b.toString(),
                                "  previous:",
                                "    " + previous.toString());
                    }

                    byte[] data = new byte[size];
                    dmgRaf.seek(offset);
                    dmgRaf.read(data);
                    curFos.write(data);
                    curFos.close();
                }
                previous = b;
            }
            ui.displayMessage("Done!");
        }
    }

    private static void copyData(ReadableRandomAccessStream inStream,
            TruncatableRandomAccessStream outStream, UserInterface ui) {
        byte[] buffer = new byte[64*1024];

        ui.setTotalProgressLength(inStream.length());
        long totalBytesCopied = 0;
        inStream.seek(0);

        int bytesRead;
        while((bytesRead = inStream.read(buffer)) > 0 && !ui.cancelSignaled()) {
            if(outStream != null)
                outStream.write(buffer, 0, bytesRead);
            ui.addProgressRaw(bytesRead);
            totalBytesCopied += bytesRead;
        }

        ui.reportProgress(100);
        ui.reportFinished(outStream == null, 0, 0, totalBytesCopied);
    }


    /**
     *
     * @param message
     * @param ui
     * @param testOnly
     * @return true if the extraction should proceed, false if it should not.
     */
    private static boolean extractionError(UserInterface ui, boolean testOnly,
            String... message) {
        if(!testOnly) {
            ui.error(message);
            return false;
        }
        else {
            message = Util.concatenate(message, "The program is " +
                    "run in testing mode, so we can continue...");
            return ui.warning(message);
        }
    }

    private static Session parseArgs(String[] args) {
        Session ses = new Session();
        try {
            /* Take care of the options... */
            int i;
            for(i = 0; i < args.length; ++i) {
                String cur = args[i];
                //System.err.println("Parsing argument: \"" + cur + "\"");
                if(!cur.startsWith("-"))
                    break;
                else if(cur.equals("-gui"))
                    ses.graphical = true;
                else if(cur.equals("-saxparser"))
                    ses.useSaxParser = true;
                else if(cur.equals("-v"))
                    ses.verbose = true;
                else if(cur.equals("-debug")) {
                    Debug.debug = true;
                    ses.debug = true;
                }
                else if(cur.equals("-startupcommand")) {
                    ses.startupCommand = args[i + 1];
                    ++i;
                }
                else {
                    ses.parseArgsErrorMessage = "Invalid argument: " + cur;
                    return ses;
                }
            }

            /*
             * This isn't a very good hack... clearly the invoker is doing
             * something wrong when this situation comes up.
             */
            int emptyTrailingEntries = 0;
            for(int j = i; j < args.length; ++j) {
                if(args[i].equals(""))
                    ++emptyTrailingEntries;
            }
            //System.err.println("empty: " + emptyTrailingEntries);

            if(i != args.length && (args.length - i) != emptyTrailingEntries) {
                ses.dmgFile = new File(args[i++]);
                if(ses.dmgFile.exists()) {

                    if(i <= args.length - 1 && !args[i].trim().equals(""))
                        ses.isoFile = new File(args[i++]);

                    if(i != args.length) {
                        if(!args[i].trim().equals(""))
                            ses.parseArgsErrorMessage = "Invalid argument: " +
                                    args[i];
                    }
                }
                else {
                    ses.parseArgsErrorMessage =
                            "Input file \"" + ses.dmgFile + "\" not found!";
                }
            }
            else if(!ses.graphical) {
                ses.parseArgsErrorMessage = "Error: No input file specified.";
            }
        } catch(Exception e) {
            e.printStackTrace();
            ses.parseArgsErrorMessage = "Unhandled exception: " + e.toString() +
                    " (see console for stacktrace)";
        }
        return ses;
    }

    private static void printUsageInstructions(UserInterface ui, String startupCommand) {
        printUsageInstructions(ui, startupCommand, null);
    }

    private static void printUsageInstructions(UserInterface ui, String startupCommand, String errorMessage) {
        String[] prefixMessage = new String[0];

        if(errorMessage != null)
            prefixMessage = Util.concatenate(prefixMessage, errorMessage);

        // 80 char ruler:
        //  <-------------------------------------------------------------------------------->
        String[] mainMessage = new String[] {
            "  usage: " + startupCommand + " [options] <dmgFile> [<outputFile>]",
            "",
            "  If an output file is not supplied, the program will simulate an extraction",
            "  (useful for detecting errors in dmg-files).",
            "",
            "  options:",
            "    -v          verbose operation... for finding out what went wrong",
            "    -saxparser  use the standard SAX parser for XML processing instead of",
            "                the APX parser (will connect to Apple's website for DTD",
            "                validation)",
            "    -gui        starts the program in graphical mode",
            "    -debug      performs unspecified debug operations (only intended for",
            "                development use)",
            ""
        };


        ui.displayMessage(Util.concatenate(prefixMessage, mainMessage));

    }
    
    private static void printSAXParserInfo(XMLReader saxParser, PrintStream ps, String prefix) throws Exception {
        ps.println(prefix + "Features:");
        ps.println(prefix + "  external-general-entities: " + saxParser.getFeature("http://xml.org/sax/features/external-general-entities"));
        ps.println(prefix + "  external-parameter-entities: " + saxParser.getFeature("http://xml.org/sax/features/external-parameter-entities"));
        ps.println(prefix + "  is-standalone: " + saxParser.getFeature("http://xml.org/sax/features/is-standalone"));
        ps.println(prefix + "  lexical-handler/parameter-entities: " + saxParser.getFeature("http://xml.org/sax/features/lexical-handler/parameter-entities"));
        //ps.println(prefix + "  parameter-entities: " + saxParser.getFeature("http://xml.org/sax/features/parameter-entities"));
        ps.println(prefix + "  namespaces: " + saxParser.getFeature("http://xml.org/sax/features/namespaces"));
        ps.println(prefix + "  namespace-prefixes: " + saxParser.getFeature("http://xml.org/sax/features/namespace-prefixes"));
        ps.println(prefix + "  resolve-dtd-uris: " + saxParser.getFeature("http://xml.org/sax/features/resolve-dtd-uris"));
        ps.println(prefix + "  string-interning: " + saxParser.getFeature("http://xml.org/sax/features/string-interning"));
        ps.println(prefix + "  unicode-normalization-checking: " + saxParser.getFeature("http://xml.org/sax/features/unicode-normalization-checking"));
        ps.println(prefix + "  use-attributes2: " + saxParser.getFeature("http://xml.org/sax/features/use-attributes2"));
        ps.println(prefix + "  use-locator2: " + saxParser.getFeature("http://xml.org/sax/features/use-locator2"));
        ps.println(prefix + "  use-entity-resolver2: " + saxParser.getFeature("http://xml.org/sax/features/use-entity-resolver2"));
        ps.println(prefix + "  validation: " + saxParser.getFeature("http://xml.org/sax/features/validation"));
        ps.println(prefix + "  xmlns-uris: " + saxParser.getFeature("http://xml.org/sax/features/xmlns-uris"));
        ps.println(prefix + "  xml-1.1: " + saxParser.getFeature("http://xml.org/sax/features/xml-1.1"));

        ps.println("Properties: ");
        ps.println(prefix + "  declaration-handler: " + saxParser.getProperty("http://xml.org/sax/properties/declaration-handler"));
        ps.println(prefix + "  document-xml-version: " + saxParser.getProperty("http://xml.org/sax/properties/document-xml-version"));
        ps.println(prefix + "  dom-node: " + saxParser.getProperty("http://xml.org/sax/properties/dom-node"));
        ps.println(prefix + "  lexical-handler: " + saxParser.getProperty("http://xml.org/sax/properties/lexical-handler"));
        ps.println(prefix + "  xml-string: " + saxParser.getProperty("http://xml.org/sax/properties/xml-string"));

        //ps.println("isValidating: " + saxParser.isValidating());
    }

    private static LinkedList<UDIFBlock> mergeBlocks(LinkedList<UDIFBlock> blockList) {
        Iterator<UDIFBlock> it = blockList.iterator();
        return mergeBlocks(it);
    }

    private static LinkedList<UDIFBlock> mergeBlocks(Iterator<UDIFBlock> it) {
        LinkedList<UDIFBlock> result = new LinkedList<UDIFBlock>();
        UDIFBlock previous = it.next();
        while(previous.getInSize() == 0 && it.hasNext()) {
            //System.err.println("Skipping: " + previous.toString());
            previous = it.next();
        }
        //System.err.println("First block in merge sequence: " + previous.toString());

        UDIFBlock current;
        while(it.hasNext()) {
            current = it.next();
            if(current.getInSize() != 0) {
                if(current.getTrueInOffset() == previous.getTrueInOffset() + previous.getInSize()) {
                    UDIFBlock mergedBlock = new UDIFBlock(
                            previous.getBlockType(),
                            previous.getReserved(),
                            previous.getOutOffset(),
                            previous.getOutSize() + current.getOutSize(),
                            previous.getInOffset(),
                            previous.getInSize() + current.getInSize(),
                            previous.getOutOffsetCompensation(),
                            previous.getInOffsetCompensation());
                    previous = mergedBlock;
                }
                else {
                    result.addLast(previous);
                    previous = current;
                }
            }
        }
        result.addLast(previous);
        return result;
    }

}

