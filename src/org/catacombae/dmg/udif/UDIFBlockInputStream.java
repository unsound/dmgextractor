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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.catacombae.dmgextractor.Util;
import org.catacombae.dmgextractor.DmgException;
import org.catacombae.dmgextractor.io.RandomAccessInputStream;
import org.catacombae.dmgextractor.io.SynchronizedRandomAccessStream;
import org.catacombae.io.ReadableRandomAccessStream;
import org.catacombae.io.RuntimeIOException;

public abstract class UDIFBlockInputStream extends InputStream {
    protected ReadableRandomAccessStream raf;
    protected UDIFBlock block;
    protected final int addInOffset;
    private long globalBytesRead;
    // 16 KiB buffer... is it reasonable?
    protected final byte[] buffer = new byte[16384];
    protected int bufferPos = 0;
    // Initializing this to zero will make read call fillBuffer at first call
    protected int bufferDataLength = 0;
    private final byte[] skipBuffer = new byte[4096];

    /**
     * Subclasses use this variable to report how many bytes were read into the
     * buffer.
     */
    protected int fillSize;

    /**
     * Creates a new UDIFBlockInputStream.
     *
     * @param raf the RandomAccessFile representing the UDIF file
     * @param block the block that we should read (usually obtained via
     * {@link PlistPartition#getBlocks()})
     * @param addInOffset the number to add to the block's inOffset to find the
     * data.
     */
    protected UDIFBlockInputStream(ReadableRandomAccessStream raf,
            UDIFBlock block, int addInOffset) {

        this.raf = raf;
        this.block = block;
        this.addInOffset = addInOffset;
        //fillBuffer();
        //bufferPos = buffer.length;
    }

    /**
     * This method WILL throw a RuntimeException if <code>block</code> has a
     * type that there is no handler for.
     */
    public static UDIFBlockInputStream getStream(ReadableRandomAccessStream raf,
            UDIFBlock block) throws IOException, RuntimeIOException {

        switch(block.getBlockType()) {
            case UDIFBlock.BT_ZLIB:
                return new ZlibBlockInputStream(raf, block, 0);
            case UDIFBlock.BT_BZIP2:
                return new Bzip2BlockInputStream(raf, block, 0);
            case UDIFBlock.BT_COPY:
                return new CopyBlockInputStream(raf, block, 0);
            case UDIFBlock.BT_ZERO:
            case UDIFBlock.BT_ZERO2:
                return new ZeroBlockInputStream(raf, block, 0);
            case UDIFBlock.BT_END:
            case UDIFBlock.BT_UNKNOWN:
                throw new RuntimeException("Block type is a marker and " +
                        "contains no data.");
            case UDIFBlock.BT_ADC:
            default:
                throw new RuntimeException("No handler for block type " +
                        block.getBlockTypeAsString());
        }
    }
    
    /**
     * In case the available amount of bytes is larger than Integer.MAX_INT,
     * Integer.MAX_INT is returned.
     */
    @Override
    public int available() throws IOException {
        long available = block.getOutSize() - globalBytesRead;
        if(available > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        else
            return (int)available;
    }

    /**
     * This method does NOT close the underlying RandomAccessFile. It can be
     * reused afterwards.
     */
    @Override
    public void close() throws IOException {}

    /** Not supported. */
    @Override
    public void mark(int readlimit) {}

    /** Returns false, because it isn't supported. */
    @Override
    public boolean markSupported() {
        return false;
    }

    /** @see java.io.InputStream */
    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        return read(b, 0, 1);
    }

    /** @see java.io.InputStream */
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /** @see java.io.InputStream */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
// 	System.out.println("UDIFBlockInputStream.read(b, " + off + ", " + len + ") {");

        final int bytesToRead = len;

        int bytesRead = 0;
        int outPos = off;
        while(bytesRead < bytesToRead) {
            int bytesRemainingInBuffer = bufferDataLength - bufferPos;
            if(bytesRemainingInBuffer == 0) {
// 		System.out.println("  first call to fillBuffer");
                fillBuffer();
// 		System.out.println("  bufferDataLength=" + bufferDataLength + ",bufferPos=" + bufferPos);
                bytesRemainingInBuffer = bufferDataLength - bufferPos;
                if(bytesRemainingInBuffer == 0) { // We apparently have no more data.
                    if(bytesRead == 0) {
                        //System.out.println("return: -1 }");
                        return -1;
                    }
                    else
                        break;
                }
            }
//          System.out.println("  bytesRemainingInBuffer=" +
//                  bytesRemainingInBuffer + ",bufferPos=" + bufferPos +
//                  ",bufferDataLength=" + bufferDataLength);
            int bytesToReadFromBuffer =
                    Math.min(bytesToRead - bytesRead, bytesRemainingInBuffer);
// 	    System.out.println("  bytesToReadFromBuffer=" +
//                  bytesToReadFromBuffer);
// 	    System.out.println("  System.arraycopy(buffer, " + bufferPos +
//                  ", b, " + outPos + ", " + bytesToReadFromBuffer + ");");
            System.arraycopy(buffer, bufferPos, b, outPos, bytesToReadFromBuffer);

            outPos += bytesToReadFromBuffer;
            bufferPos += bytesToReadFromBuffer;

            bytesRead += bytesToReadFromBuffer;
        }

        globalBytesRead += bytesRead;

// 	System.out.println("return: " + bytesRead + " }");
        return bytesRead;
    }

    /** Does nothing. Not supported. */
    @Override
    public void reset() throws IOException {}

    /**
     * Skips as many bytes as possible. If end of file is reached, the number
     * of bytes skipped is returned.
     */
    @Override
    public long skip(long n) throws IOException {
        long bytesSkipped = 0;
        while(bytesSkipped < n) {
            int curSkip = (int) Math.min(n - bytesSkipped, skipBuffer.length);
            if(curSkip < 0) {
                throw new RuntimeException("Internal error: curSkip is " +
                        "negative (" + curSkip + ").");
            }

            int res = read(skipBuffer, 0, curSkip);
            if(res > 0)
                bytesSkipped += res;
            else
                break;
        }
        return bytesSkipped;
    }

    protected abstract void fillBuffer() throws IOException;

    public static class ZlibBlockInputStream extends UDIFBlockInputStream {

        private final Inflater inflater;
        private final byte[] inBuffer;
        private long inPos;

        public ZlibBlockInputStream(ReadableRandomAccessStream raf,
                UDIFBlock block, int addInOffset) throws IOException {
            super(raf, block, addInOffset);
            inflater = new Inflater();
            inBuffer = new byte[4096];
            inPos = 0;
            feedInflater();
        }

        private void feedInflater() throws IOException {
            //System.err.println("ZlibBlockInputStream.feedInflater() {");
            long seekPos = addInOffset + inPos + block.getTrueInOffset();
            //System.out.println("  seeking to " + seekPos + " (file length: " +
            //        raf.length() + ")");
            raf.seek(seekPos);
            long bytesLeftToRead = block.getInSize() - inPos;
            int bytesToFeed = (int) Math.min(inBuffer.length, bytesLeftToRead);
            //System.out.println("  bytesToFeed=" + bytesToFeed);

            int curBytesRead = raf.read(inBuffer, 0, bytesToFeed);
            inPos += curBytesRead;
            inflater.setInput(inBuffer, 0, curBytesRead);
            //System.out.println("  curBytesRead=" + curBytesRead);
            //System.out.println("}");
        }

        protected void fillBuffer() throws RuntimeIOException, IOException {
            //System.err.println("ZlibBlockInputStream.fillBuffer() {");
            //if(inflater == null)
            //    System.err.println("INFLATER IS NULL");
            //if(inBuffer == null)
            //    System.err.println("INBUFFER IS NULL");
            if(inflater.finished()) {
                //System.out.println("inflater claims to be finished...");
                bufferPos = 0;
                bufferDataLength = 0;
            }
            try {
                int bytesInflated = 0;
                while(bytesInflated < buffer.length && !inflater.finished()) {
                    if(inflater.needsInput())
                        feedInflater();
                    int res = inflater.inflate(buffer, bytesInflated,
                            buffer.length - bytesInflated);
                    if(res >= 0)
                        bytesInflated += res;
                    else
                        throw new DmgException("Negative return value when " +
                                "inflating");
                }

                // The fillBuffer method is responsible for updating bufferPos
                // and bufferDataLength
                bufferPos = 0;
                bufferDataLength = bytesInflated;
            } catch(DataFormatException e) {
                DmgException re = new DmgException("Invalid zlib data!");
                re.initCause(e);
                throw re;
            }
            //System.out.println("}");
        }
    }

    public static class CopyBlockInputStream extends UDIFBlockInputStream {

        private long inPos = 0;

        public CopyBlockInputStream(ReadableRandomAccessStream raf,
                UDIFBlock block, int addInOffset) throws RuntimeIOException {
            super(raf, block, addInOffset);
        }

        protected void fillBuffer() throws IOException {
            raf.seek(addInOffset + inPos + block.getTrueInOffset());

            final int bytesToRead = (int) Math.min(block.getInSize() - inPos,
                    buffer.length);
            int totalBytesRead = 0;
            while(totalBytesRead < bytesToRead) {
                int bytesRead = raf.read(buffer, totalBytesRead,
                        bytesToRead - totalBytesRead);
                if(bytesRead < 0)
                    break;
                else {
                    totalBytesRead += bytesRead;
                    inPos += bytesRead;
                }
            }

            // The fillBuffer method is responsible for updating bufferPos and
            // bufferDataLength
            bufferPos = 0;
            bufferDataLength = totalBytesRead;
        }

        /** Extremely more efficient skip method! */
        @Override
        public long skip(long n) throws IOException {
            final long bytesToSkip =
                    Math.min(block.getInSize() - inPos, n);
            if(bytesToSkip < 0) {
                throw new RuntimeException("Internal error: bytesToSkip is " +
                        "negative (" + bytesToSkip + ").");
            }

            inPos += bytesToSkip;

            // make read() refill buffer at next call..
            bufferPos = 0;
            bufferDataLength = 0;

            return bytesToSkip;
        }
    }

    public static class ZeroBlockInputStream extends UDIFBlockInputStream {

        private long outPos = 0;

        public ZeroBlockInputStream(ReadableRandomAccessStream raf,
                UDIFBlock block, int addInOffset) throws RuntimeIOException {
            super(raf, block, addInOffset);
        }

        protected void fillBuffer() throws IOException {
            final int bytesToWrite =
                    (int) Math.min(block.getOutSize() - outPos, buffer.length);
            Util.zero(buffer, 0, bytesToWrite);
            outPos += bytesToWrite;

            // The fillBuffer method is responsible for updating bufferPos and
            // bufferDataLength
            bufferPos = 0;
            bufferDataLength = bytesToWrite;
        }

        /** Extremely more efficient skip method! */
        @Override
        public long skip(long n) throws IOException {
            final long bytesToSkip =
                    Math.min(block.getOutSize() - outPos, n);
            if(bytesToSkip < 0) {
                throw new RuntimeException("Internal error: bytesToSkip is " +
                        "negative (" + bytesToSkip + ").");
            }

            outPos += bytesToSkip;

            // make read() refill buffer at next call..
            bufferPos = 0;
            bufferDataLength = 0;

            return bytesToSkip;
        }
    }

    public static class Bzip2BlockInputStream extends UDIFBlockInputStream {

        private final byte[] BZIP2_SIGNATURE = { 0x42, 0x5A }; // 'BZ'
        private InputStream bzip2DataStream;
        private CBZip2InputStream decompressingStream;
        private long outPos = 0;

        public Bzip2BlockInputStream(ReadableRandomAccessStream raf,
                UDIFBlock block, int addInOffset)
                throws IOException, RuntimeIOException {

            super(raf, block, addInOffset);

            if(false) {
                byte[] inBuffer = new byte[4096];
                String basename = System.nanoTime() + "";
                File outFile = new File(basename + "_bz2.bin");
                int i = 1;
                while(outFile.exists())
                    outFile = new File(basename + "_" + i++ + "_bz2.bin");
                System.err.println("Creating a new Bzip2BlockInputStream. " +
                        "Dumping bzip2 block data to file \"" + outFile + "\"");
                FileOutputStream outStream = new FileOutputStream(outFile);
                raf.seek(block.getTrueInOffset());
                long bytesWritten = 0;
                long bytesToWrite = block.getInSize();
                while(bytesWritten < bytesToWrite) {
                    int curBytesRead = raf.read(inBuffer, 0,
                            (int) Math.min(bytesToWrite - bytesWritten,
                            inBuffer.length));
                    if(curBytesRead <= 0)
                        throw new RuntimeException("Unable to read bzip2 " +
                                "block fully.");
                    outStream.write(inBuffer, 0, curBytesRead);
                    bytesWritten += curBytesRead;
                }
                outStream.close();
            }

            bzip2DataStream = new RandomAccessInputStream(
                    new SynchronizedRandomAccessStream(raf),
                    block.getTrueInOffset(), block.getInSize());

            byte[] signature = new byte[2];
            if(bzip2DataStream.read(signature) != signature.length)
                throw new RuntimeException("Read error!");
            if(!Util.arraysEqual(signature, BZIP2_SIGNATURE))
                throw new RuntimeException("Invalid bzip2 block!");

            /* Buffering needed because of implementation issues in
             * CBZip2InputStream. */
            decompressingStream = new CBZip2InputStream(
                    new BufferedInputStream(bzip2DataStream));
        }

        protected void fillBuffer() throws IOException {

            final int bytesToRead = (int) Math.min(block.getOutSize() - outPos,
                    buffer.length);
            int totalBytesRead = 0;
            while(totalBytesRead < bytesToRead) {
                int bytesRead = decompressingStream.read(buffer, totalBytesRead,
                        bytesToRead - totalBytesRead);
                if(bytesRead < 0)
                    break;
                else {
                    totalBytesRead += bytesRead;
                    outPos += bytesRead;
                }
            }

            // The fillBuffer method is responsible for updating bufferPos and
            // bufferDataLength
            bufferPos = 0;
            bufferDataLength = totalBytesRead;
        }

        @Override
        public void close() throws IOException {
            decompressingStream.close();
            bzip2DataStream.close();
        }
    }
}
