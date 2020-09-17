/*-
 * Copyright (C) 2008 Erik Larsson
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
 *
 * This code was written from studying vfdecrypt, Copyright (c) 2006
 *   Ralf-Philipp Weinmann <ralf@coderpunks.org>
 *   Jacob Appelbaum <jacob@appelbaum.net>
 *   Christian Fromme <kaner@strace.org>
 *
 * [I'm not sure if their copyright and license terms need to be applied,
 *  but in case they do, the original license terms are reprinted below
 *  as required by the license.]
 *
 * The vfdecrypt license says:
 *   Permission is hereby granted, free of charge, to any person
 *   obtaining a copy of this software and associated documentation
 *   files (the "Software"), to deal in the Software without
 *   restriction, including without limitation the rights to use,
 *   copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the
 *   Software is furnished to do so, subject to the following
 *   conditions:
 *
 *   The above copyright notice and this permission notice shall be
 *   included in all copies or substantial portions of the Software.
 */

package org.catacombae.dmg.encrypted;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.util.LinkedList;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.catacombae.dmgextractor.Util;
import org.catacombae.dmg.encrypted.CommonCEncryptedEncodingHeader.KeySet;
import org.catacombae.dmg.sparsebundle.ReadableSparseBundleStream;
import org.catacombae.io.BasicReadableRandomAccessStream;
import org.catacombae.io.ReadableFileStream;
import org.catacombae.io.ReadableRandomAccessStream;
import org.catacombae.io.RuntimeIOException;
import org.catacombae.util.Util.Pair;

/**
 * Filtering stream that takes the data of a Mac OS X encrypted disk image and a password as input
 * and acts as a transparent decryption layer, allowing the user to access the unencrypted
 * underlying disk image data. (The encryption format isn't disk image specific, so it might be used
 * by other parts of Mac OS X as well, making this filter even more useful...)
 * <p>
 * Documentation on how encrypted disk images work was retrieved from the "Unlocking
 * FileVault" slides, published by Jacob Appelbaum and Ralf-Philipp Weinmann, and the source code of
 * the utility vfdecrypt in VileFault, copyright Ralf-Philipp Weinmann, Jacob Appelbaum and
 * Christian Fromme.
 *
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public class ReadableCEncryptedEncodingStream extends BasicReadableRandomAccessStream {
    private final ReadableRandomAccessStream backingStream;
    private final ReadableSparseBundleStream sbStream;
    private final CommonCEncryptedEncodingHeader header;
    private final SecretKeySpec aesKey;
    private final SecretKeySpec hmacSha1Key;
    private final Mac hmacSha1;
    private final Cipher aesCipher;
    private final long streamLength;

    // Tracker variables
    private long blockNumber = 0;
    private int posInBlock = 0;

    public ReadableCEncryptedEncodingStream(ReadableRandomAccessStream backingStream,
            char[] password) throws RuntimeIOException {
        Debug.print("ReadableCEncryptedEncodingStream(" + backingStream + ", " + password +");");
        this.backingStream = backingStream;

        if(backingStream instanceof ReadableSparseBundleStream) {
            this.sbStream = (ReadableSparseBundleStream) backingStream;
        }
        else {
            this.sbStream = null;
        }

        int headerVersion = CEncryptedEncodingUtil.detectVersion(backingStream);
        Debug.print("  headerVersion = " + headerVersion);
        switch(headerVersion) {
            case 1:
                byte[] v1HeaderData = new byte[V1Header.length()];
                backingStream.seek(backingStream.length()-V1Header.length());
                backingStream.readFully(v1HeaderData);
                V1Header v1header = new V1Header(v1HeaderData, 0);
                Debug.print("  V1 header:");
                v1header.print(Debug.ps, "    ");
                header = CommonCEncryptedEncodingHeader.create(v1header);
                break;
            case 2:
                backingStream.seek(0);
                V2Header v2header = new V2Header(backingStream);
                Debug.print("  V2 header:");
                v2header.print(Debug.ps, "    ");
                header = CommonCEncryptedEncodingHeader.create(v2header);
                break;
            case -1:
                throw new RuntimeException("No CEncryptedEncoding header found!");
            default:
                throw new RuntimeException("Unknown header version: " + headerVersion);
        }

        this.streamLength = header.getEncryptedDataLength();
        /*
        if(this.length % header.getBlockSize() != 0) {
            System.err.println("WARNING: Block data area length (" + this.length +
                    ") is not aligned to block size (" + header.getBlockSize() + ")!");
        }
         * */

        RuntimeException firstException = null;
        SecretKeySpec curAesKey = null;
        SecretKeySpec curHmacSha1Key = null;
        Mac curHmacSha1 = null;
        Cipher curAesCipher = null;

        for(CommonCEncryptedEncodingHeader.KeyData key : header.getKeys()) {
            try {
                final String pbeAlgorithmName = "PBKDF2WithHmacSHA1";
                // Note: Why doesn't"PBEWithHmacSHA1AndDESede" work?

                // Derive the proper key from our password.
                PBEKeySpec ks = new PBEKeySpec(password, key.getKdfSalt(),
                        key.getKdfIterationCount(), 192);
                SecretKeyFactory fact =
                        SecretKeyFactory.getInstance(pbeAlgorithmName);
                Key k = fact.generateSecret(ks);

                byte[] keyData = k.getEncoded();
                Debug.print("Derived key: 0x" +
                        Util.byteArrayToHexString(keyData));

                // Set up the cipher
                final String cipherAlgorithmName = "DESede/CBC/PKCS5Padding";
                Cipher keyDecryptionCipher =
                        Cipher.getInstance(cipherAlgorithmName);
                SecretKeyFactory fact2 = SecretKeyFactory.getInstance("DESede");
                Key k2 = fact2.generateSecret(new DESedeKeySpec(keyData));

                // Call the version specific unwrap function.
                KeySet keys = key.unwrapKeys(k2, keyDecryptionCipher);

                Debug.print("AES key: 0x" +
                        Util.byteArrayToHexString(keys.getAesKey()));
                Debug.print("HmacSHA1 key: 0x" +
                        Util.byteArrayToHexString(keys.getHmacSha1Key()));

                curAesKey = new SecretKeySpec(keys.getAesKey(), "AES");
                curHmacSha1Key =
                        new SecretKeySpec(keys.getHmacSha1Key(), "HmacSHA1");

                keys.clearData(); // No unused keys in memory please.

                curHmacSha1 = Mac.getInstance("HmacSHA1");
                curHmacSha1.init(curHmacSha1Key);

                curAesCipher = Cipher.getInstance("AES/CBC/NoPadding");
                break;
            } catch(Exception e) {
                if(firstException == null) {
                    firstException = new RuntimeException("Exception while " +
                                "trying to decrypt keys.", e);
                }
            }
        }

        if(curAesCipher != null) {
            aesKey = curAesKey;
            hmacSha1Key = curHmacSha1Key;
            hmacSha1 = curHmacSha1;
            aesCipher = curAesCipher;
        }
        else if(firstException != null) {
            throw firstException;
        }
        else {
            throw new RuntimeException("No keys in header.");
        }
    }
    
    /**
     * Tells whether <code>stream</code> is encoded with CEncryptedEncoding or not. If this method
     * returns true, the stream can be fed to the ReadableCEncryptedEncoding constructor.
     *
     * @param stream the stream to check for the signatures of a CEncryptedEncoding.
     * @return whether <code>stream</code> is encoded with CEncryptedEncoding or not.
     */
    public static boolean isCEncryptedEncoding(ReadableRandomAccessStream stream) {
        int version = CEncryptedEncodingUtil.detectVersion(stream);
        return version == 1 || version == 2;
    }
    
    @Override
    public void close() throws RuntimeIOException {
        backingStream.close();
    }

    @Override
    public void seek(long pos) throws RuntimeIOException {
        if(pos < 0)
            throw new IllegalArgumentException("Negative seek request: pos (" + pos + ") < 0");
        else if(streamLength != 0 && pos > streamLength) {
            // throw new IllegalArgumentException("Trying to seek beyond EOF: pos (" + pos +
            //        ") > length (" + length + ")");

            // Let's just seek to the end of file instead of throwing stuff around us.
            this.blockNumber = streamLength/header.getBlockSize();
            this.posInBlock = 0;
        }
        else {
            long nextBlockNumber = pos / header.getBlockSize();
            int nextPosInBlock = (int) (pos % header.getBlockSize());

            /*
            if(header.getBlockDataStart() + (nextBlockNumber+1)*header.getBlockSize() > backingStream.length()) {
            nextBlockNumber = (backingStream.length()-header.getBlockDataStart())/header.getBlockSize();
            nextPosInBlock = 0;
            }
             * */

            this.blockNumber = nextBlockNumber;
            this.posInBlock = nextPosInBlock;
        }
    }

    @Override
    public long length() throws RuntimeIOException {
        return streamLength;
    }

    @Override
    public long getFilePointer() throws RuntimeIOException {
        return blockNumber*header.getBlockSize() + posInBlock;
    }

    @Override
    public int read(byte[] b, int off, int len) throws RuntimeIOException {
        // <Input check>
        if(len == 0)
            return 0;
        else if(len < 0)
            throw new IndexOutOfBoundsException("len (" + len + ") < 0");
        else if(off < 0)
            throw new IndexOutOfBoundsException("off (" + off + ") < 0");
        else if(off+len > b.length)
            throw new IndexOutOfBoundsException("off+len (" + (off+len) +
                    ") > b.length (" + b.length + ")");
        // </Input check>
        
        backingStream.seek(header.getBlockDataStart() + blockNumber*header.getBlockSize());

        byte[] encBlockData = new byte[header.getBlockSize()];
        byte[] decBlockData = new byte[encBlockData.length];
        LinkedList<Pair<Long, Long>> holeList = null;
        long bandBlockCount;

        if(sbStream != null) {
            bandBlockCount = sbStream.getBandSize() / header.getBlockSize();
        }
        else {
            bandBlockCount = 0;
        }

        try {
            int totalBytesRead = 0;
            while(totalBytesRead < len && (streamLength == 0 ||
                    blockNumber*header.getBlockSize() < streamLength))
            {
                int bytesRead;

                if(sbStream != null) {
                    if(holeList == null) {
                        holeList = new LinkedList<Pair<Long, Long>>();
                    }
                    else {
                        holeList.clear();
                    }

                    bytesRead = sbStream.read(encBlockData, holeList);
                }
                else {
                    bytesRead = backingStream.read(encBlockData);
                }

                if(bytesRead != encBlockData.length) {
                    if(bytesRead > 0)
                        System.err.println("WARNING: Could not read entire block! " +
                                "blockNumber=" + blockNumber + ", bytesRead=" + bytesRead);
                    break;
                }

                boolean isHole;
                if(holeList != null) {
                    switch(holeList.size()) {
                        case 0:
                            isHole = false;
                            break;
                        case 1:
                            final Pair<Long, Long> hole = holeList.getFirst();

                            if(hole.getA() != 0 ||
                                hole.getB() != encBlockData.length)
                            {
                                throw new RuntimeIOException("Unexpected: " +
                                        "Hole only partially covers the " +
                                        "block (hole start: " + hole.getA() +
                                        ", hole length: " + hole.getB() +
                                        ", block size: " + encBlockData.length +
                                        ").");
                            }

                            isHole = true;
                            break;
                        default:
                            throw new RuntimeIOException("Unexpected: Got " +
                                    holeList.size() + " holes in " +
                                    encBlockData.length + " byte read.");
                    }
                }
                else {
                    isHole = false;
                }

                if(isHole) {
                    Util.arrayCopy(encBlockData, decBlockData);
                }
                else {
                    final long virtualBlockNumber;
                    if(sbStream != null) {
                        virtualBlockNumber = blockNumber % bandBlockCount;
                    }
                    else {
                        virtualBlockNumber = blockNumber;
                    }

                    int bytesDecrypted =
                            decrypt(encBlockData, decBlockData,
                            virtualBlockNumber);
                    Assert.eq(bytesDecrypted, decBlockData.length);
                }
                
                final int blockSize;
                if(streamLength > 0) {
                    final long bytesLeftInStream =
                            streamLength - blockNumber * header.getBlockSize();

                    blockSize =
                            (int) (bytesLeftInStream < decBlockData.length ?
                            bytesLeftInStream : decBlockData.length);
                }
                else {
                    blockSize = decBlockData.length;
                }


                final int bytesLeftToRead = len-totalBytesRead;
                final int bytesLeftInBlock = blockSize-posInBlock;
                int bytesToCopy = bytesLeftToRead < bytesLeftInBlock ? bytesLeftToRead : bytesLeftInBlock;
                
                System.arraycopy(decBlockData, posInBlock, b, off + totalBytesRead, bytesToCopy);

                totalBytesRead += bytesToCopy;

                if(bytesToCopy == bytesLeftInBlock) {
                    ++blockNumber;
                    posInBlock = 0;
                }
                else {
                    posInBlock += bytesLeftToRead;
                }
            }

            if(totalBytesRead > 0)
                return totalBytesRead;
            else
                return -1;
        } finally {
            Util.zero(encBlockData);
            Util.zero(decBlockData);
        }
    }

    private int decrypt(byte[] encBlockData, byte[] decBlockData, long blockNumber) {
        Debug.print("decrypt(byte[" + encBlockData.length + "], byte[" +
                decBlockData.length + "], " + blockNumber + ");");
        if(blockNumber < 0 || blockNumber > Integer.MAX_VALUE)
            throw new RuntimeException("Block number out of range: " + blockNumber);
        int blockNumberInt = (int)(blockNumber & 0xFFFFFFFF);
        hmacSha1.reset();
        hmacSha1.update(Util.toByteArrayBE(blockNumberInt));
        byte[] iv = new byte[16];

        /* The 160-bit MAC value is truncated to 16 bytes (128 bits) to be
         * used as the cipher's IV. */
        System.arraycopy(hmacSha1.doFinal(), 0, iv, 0, iv.length);
        //Debug.print("  iv: 0x" + Util.byteArrayToHexString(iv));

        try {
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
            int bytesDecrypted =
                    aesCipher.doFinal(encBlockData, 0, encBlockData.length, decBlockData, 0);

            return bytesDecrypted;
        } catch(Exception e) {
            throw new RuntimeException("Unexpected exception when trying to " +
                    "decrypt block " + blockNumber + ".", e);
        } finally {
            Util.zero(iv);
        }
    }

    private static void printHelp() {
        System.err.println("usage: " + ReadableCEncryptedEncodingStream.class.getName() +
                " -i in-file -p password -o out-file");
        System.exit(-1);
    }
    public static void main(String[] args) throws IOException {
        /*
        boolean debugMode = true;
        if(debugMode && args.length == 0) {
            String imageprefix = "v2";
            File inFile = new File("/Users/erik/devel/reference/vilefault/vfdecrypt/" +
                    imageprefix + "image.dmg");
            File outFile = new File("/Users/erik/devel/reference/vilefault/vfdecrypt/" +
                    imageprefix + "image_javadec.dmg");
            char[] password = SecretPassword.PASSWORD;
            runTest(inFile, outFile, password);
        }
         * */

        String inputFilename = null;
        String outputFilename = null;
        String password = null;
        for(int i = 0; i < args.length; ++i) {
            String curArg = args[i];
            if(curArg.startsWith("-i")) {
                if(i+1 < args.length)
                    inputFilename = args[i+1];
                else
                    printHelp();
            }
            else if(curArg.startsWith("-p")) {
                if(i+1 < args.length)
                    password = args[i+1];
                else
                    printHelp();
            }
            else if(curArg.startsWith("-o")) {
                if(i+1 < args.length)
                    outputFilename = args[i+1];
                else
                    printHelp();
            }
        }
        if(inputFilename == null || outputFilename == null || password == null)
            printHelp();

        runTest(inputFilename, outputFilename, password);
    }
    
    private static void runTest(String inputFilename, String outputFilename, String password) throws IOException {
        ReadableRandomAccessStream backingStream = new ReadableFileStream(inputFilename);

        ReadableRandomAccessStream rras =
                new ReadableCEncryptedEncodingStream(backingStream, password.toCharArray());

        System.out.println("Length of encrypted data: " + rras.length() + " bytes");

        byte[] lastBlock = new byte[4096];
        rras.seek(rras.length()-4096);
        rras.readFully(lastBlock);
        System.out.println("Last block: 0x" + Util.byteArrayToHexString(lastBlock));

        byte[] sig = new byte[2];
        rras.seek(0);
        rras.readFully(sig);
        System.out.println("Signature: " + Util.toASCIIString(sig));
        System.out.println("fp=" + rras.getFilePointer());
        byte[] following = new byte[3];
        rras.readFully(following);
        System.out.println("Following(" + following.length + "): 0x" + Util.byteArrayToHexString(following));
        System.out.println("fp=" + rras.getFilePointer());
        rras.readFully(following);
        System.out.println("Following(" + following.length + "): 0x" + Util.byteArrayToHexString(following));
        System.out.println("fp=" + rras.getFilePointer());
        rras.readFully(following);
        System.out.println("Following(" + following.length + "): 0x" + Util.byteArrayToHexString(following));
        System.out.println("fp=" + rras.getFilePointer());

        rras.seek(33792);
        rras.readFully(sig);
        System.out.println("Signature: " + Util.toASCIIString(sig));
        System.out.println("fp=" + rras.getFilePointer());

        System.out.println("Checking boundary passage:");
        byte[] boundaryBytes = new byte[9];
        rras.seek(36859);
        rras.readFully(boundaryBytes);
        System.out.println("boundaryBytes(" + boundaryBytes.length + "): 0x" + Util.byteArrayToHexString(boundaryBytes));
        System.out.println("fp=" + rras.getFilePointer());

        System.out.println("Checking reading until eof:");
        {
            byte[] buffer = new byte[5001];
            rras.seek(rras.length()-4096*3);
            int bytesRead = rras.read(buffer);
            long totBytesRead = 0;
            while(bytesRead != -1) {
                System.out.println("Read " + bytesRead + " bytes.");
                totBytesRead += bytesRead;
                bytesRead = rras.read(buffer);
            }
            System.out.println("Finished. bytesRead=" + bytesRead + " totBytesRead=" + totBytesRead);
        }

        //System.exit(0);

        FileOutputStream out = new FileOutputStream(outputFilename);
        System.out.println("Extracting encrypted data to file: " + outputFilename);
        rras.seek(0);
        byte[] buffer = new byte[9119];
        int bytesRead = rras.read(buffer);
        long totalBytesWritten = 0;
        while(bytesRead > 0) {
            System.out.println("Read " + bytesRead + " bytes.");
            out.write(buffer, 0, bytesRead);
            totalBytesWritten += bytesRead;
            bytesRead = rras.read(buffer);
        }
        System.out.println("Wrote " + totalBytesWritten + " bytes.");
        out.close();
    }
}
