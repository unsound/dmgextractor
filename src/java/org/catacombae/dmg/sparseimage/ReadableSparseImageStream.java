/*-
 * Copyright (C) 2014 Erik Larsson
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

package org.catacombae.dmg.sparseimage;

import org.catacombae.io.BasicReadableRandomAccessStream;
import org.catacombae.io.ReadableRandomAccessStream;
import org.catacombae.io.RuntimeIOException;
import org.catacombae.io.SynchronizedReadableRandomAccessStream;
import org.catacombae.util.Util;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public class ReadableSparseImageStream extends BasicReadableRandomAccessStream {
    private final ReadableRandomAccessStream backingStream;
    private final SparseImageHeader header;
    private final long size;
    private final long blockSize;
    private final int[] blockMap;

    private long fp = 0;

    public ReadableSparseImageStream(
            final ReadableRandomAccessStream backingStream)
    {
        this.backingStream = backingStream;

        byte[] headerData = new byte[4096];
        backingStream.seek(0);
        backingStream.readFully(headerData);

        this.header = new SparseImageHeader(headerData, 0);

        final String signature =
                Util.readString(this.header.getSignature(), "US-ASCII");
        if(!signature.equals("sprs")) {
            throw new RuntimeException("Invalid signature: \"" + signature +
                    "\"");
        }

        /* Sector size appears to be fixed at 512. */
        final long sectorCount = header.getSectorCount();
        final long sectorsPerBlock = header.getSectorsPerBlock();

        this.size = sectorCount * 512;
        this.blockSize = header.getSectorsPerBlock() * 512;

        final long blockMapElementCount =
                (sectorCount + sectorsPerBlock - 1) / sectorsPerBlock;
        if(blockMapElementCount > Integer.MAX_VALUE) {
            throw new RuntimeException("Block map size too large for address " +
                    "space: " + blockMapElementCount);
        }

        this.blockMap = new int[(int) blockMapElementCount];

        /* Iterate over the (reverse) block map in header and fill in the
         * gaps. */
        int curBlock = 0;
        int curHeader = 0;
        for(; curBlock < blockMap.length; ++curHeader) {
            final int blockMapOffsetInHeader;
            final int blockMapEntriesInHeader;

            if(curHeader == 0) {
                blockMapOffsetInHeader = 64;
                blockMapEntriesInHeader = 1008;
            }
            else {
                final long nextHeaderOffset =
                        4096 + 1008 * blockSize +
                        (curHeader - 1) * (4096 + 1010 * blockSize);

                backingStream.seek(nextHeaderOffset);
                if(backingStream.read(headerData) == -1) {
                    /* If we reach end of file here, all non-hole blocks have
                     * been processed. The rest are just holes. */
                    break;
                }

                blockMapOffsetInHeader = 56;
                blockMapEntriesInHeader = 1010;
            }

            final int remainingBlocks = blockMap.length - curBlock;
            final int curEntriesToRead =
                    remainingBlocks < blockMapEntriesInHeader ?
                    remainingBlocks : blockMapEntriesInHeader;

            for(int i = 0; i < curEntriesToRead; ++i, ++curBlock) {
                final long curMapping =
                        Util.unsign(Util.readIntBE(headerData,
                        blockMapOffsetInHeader + i * 4));

                if(curMapping == 0) {
                    /* Hole. */
                    continue;
                }
                else if((curMapping - 1) > blockMap.length) {
                    throw new RuntimeException("Inconsistent mapping at " +
                            curBlock + ": Points at block " + (curMapping - 1) +
                            ". Number of blocks in sparse image: " +
                            blockMap.length);
                }
                else if(blockMap[(int) curMapping - 1] != 0) {
                    throw new RuntimeException("Cross-linked mapping: " +
                            "Physical blocks " +
                            blockMap[(int) curMapping - 1] + " and " +
                            curBlock + " both point at virtual block " +
                            (curMapping - 1) + ".");
                }

                blockMap[(int) curMapping - 1] = curBlock + 1;
            }
        }
    }

    @Override
    public void close() throws RuntimeIOException {
        this.backingStream.close();
    }

    @Override
    public synchronized void seek(final long offset) throws RuntimeIOException {
        if(offset < 0) {
            throw new RuntimeIOException("Negative seek offset (" + offset +
                    ")");
        }

        this.fp = offset;
    }

    @Override
    public long length() throws RuntimeIOException {
        return this.size;
    }

    @Override
    public synchronized long getFilePointer() throws RuntimeIOException {
        return fp;
    }

    @Override
    public synchronized int read(final byte[] data, final int pos,
            final int len) throws RuntimeIOException
    {
        int curPos = pos;
        int remaining;
        if(fp < size) {
            remaining = len > size || fp > size - len ? (int) (size - fp) : len;
        }
        else {
            remaining = 0;
        }

        while(remaining > 0) {
            final long virtualBlockIndex = fp / blockSize;
            final long offsetInBlock = fp % blockSize;
            final long remainingInBlock = blockSize - offsetInBlock;
            final int bytesToRead =
                    remaining < remainingInBlock ? remaining :
                    (int) remainingInBlock;

            if(virtualBlockIndex >= blockMap.length) {
                break;
            }

            final int blockMapValue = blockMap[(int) virtualBlockIndex];
            int bytesRead = 0;

            if(blockMapValue != 0) {
                final int physicalBlockIndex = blockMapValue - 1;
                final long segmentShift =
                        4096 + (physicalBlockIndex < 1008 ? 0 :
                        4096 + ((physicalBlockIndex - 1008) / 1010) * 4096);
                final long seekOffset = segmentShift +
                        ((long) physicalBlockIndex) * blockSize +
                        offsetInBlock;
                backingStream.seek(seekOffset);
                bytesRead = backingStream.read(data, curPos, bytesToRead);
                if(bytesRead < 0) {
                    bytesRead = 0;
                }
            }

            if(bytesRead != bytesToRead) {
                /* Not allocated. Just fill with zeroes. */
                Util.zero(data, curPos + bytesRead, bytesToRead - bytesRead);
                bytesRead = bytesToRead;
            }

            remaining -= bytesRead;
            curPos += bytesRead;
            fp += bytesRead;

            if(bytesRead != bytesToRead) {
                break;
            }
        }

        final int result;
        if(curPos == pos) {
            result = -1;
        }
        else {
            result = curPos - pos;
        }

        return result;
    }
}
