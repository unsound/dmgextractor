/*-
 * Copyright (C) 2011-2014 Erik Larsson
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

package org.catacombae.dmg.sparsebundle;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import org.catacombae.io.BasicReadableRandomAccessStream;
import org.catacombae.io.RuntimeIOException;
import org.catacombae.util.Util.Pair;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public class ReadableSparseBundleStream extends BasicReadableRandomAccessStream
{
    private final SparseBundle bundle;
    private long pos = 0;

    /* Band state variables. */
    private long bandNumber = 0;
    private Band band = null;

    public ReadableSparseBundleStream(final File sparseBundleDir) {
        this(new SparseBundle(sparseBundleDir));
    }

    public ReadableSparseBundleStream(final FileAccessor sparseBundleDir) {
        this(new SparseBundle(sparseBundleDir));
    }

    ReadableSparseBundleStream(final SparseBundle bundle) {
        this.bundle = bundle;
    }

    public long getBandSize() {
        return bundle.getBandSize();
    }

    @Override
    public void close() throws RuntimeIOException {
        if(this.band != null)
            this.band.close();
        this.bundle.close();
    }

    @Override
    public void seek(long pos) throws RuntimeIOException {
        this.pos = pos;
    }

    @Override
    public long length() throws RuntimeIOException {
        return bundle.getToken().getSize() + bundle.getSize();
    }

    @Override
    public long getFilePointer() throws RuntimeIOException {
        return pos;
    }

    @Override
    public int read(final byte[] data, final int off, final int len)
            throws RuntimeIOException {
        return read(data, off, len, null);
    }

    public int read(final byte[] data, LinkedList<Pair<Long, Long>> holeList)
    {
        return read(data, 0, data.length, holeList);
    }

    public int read(final byte[] data, final int off, final int len,
            LinkedList<Pair<Long, Long>> holeList)
            throws RuntimeIOException
    {
        if(data == null)
            throw new IllegalArgumentException("data is null.");
        if(off < 0 || off > data.length)
            throw new IllegalArgumentException("pos out of range.");
        if(len < 0 || len > (data.length - off))
            throw new IllegalArgumentException("len out of range.");

        final Token token = bundle.getToken();
        final long tokenSize = token.getSize();
        if(tokenSize < 0) {
            throw new RuntimeException("Internal error: token size " +
                    "(" + tokenSize + ") < 0");
        }

        final long bundleSize = tokenSize + bundle.getSize();
        if(pos >= bundleSize)
            return -1;

        final long bytesRemainingInStream = bundleSize - pos;
        final int readSize;
        if(len > bytesRemainingInStream)
            readSize = (int) bytesRemainingInStream;
        else
            readSize = len;

        final long bandSize = bundle.getBandSize();
        long curHoleStart = 0;
        long curHoleLength = 0;

        int curOff = off;
        int remainingSize = readSize;
        while(remainingSize > 0) {
            final int bytesToRead;
            final int bytesRead;

            if(pos < tokenSize) {
                final long remainingInToken = tokenSize - pos;

                bytesToRead =
                        remainingInToken < len ? (int) remainingInToken : len;

                try {
                    bytesRead = token.read(pos, data, curOff, bytesToRead);
                } catch(RuntimeIOException ex) {
                    final IOException cause = ex.getIOCause();

                    if(cause != null) {
                        throw new RuntimeIOException("Exception while " +
                                "reading from token.", cause);
                    }

                    throw ex;
                }
            }
            else {
                final long posInData = pos - tokenSize;
                final long curBandNumber = posInData / bandSize;
                final long posInBand = posInData % bandSize;

                long bandHoleStart;
                long bandHoleLength;

                if(band == null)
                    band = bundle.lookupBand(curBandNumber);
                else if(curBandNumber != bandNumber) {
                    band.close();
                    band = bundle.lookupBand(curBandNumber);
                }

                bandNumber = curBandNumber;

                final long remainingInBand = bandSize - posInBand;
                if(remainingSize > remainingInBand)
                    bytesToRead = (int) remainingInBand;
                else
                    bytesToRead = remainingSize;

                if(band == null) {
                    Arrays.fill(data, curOff, curOff+bytesToRead, (byte) 0);
                    bytesRead = bytesToRead;

                    bandHoleStart = curOff;
                    bandHoleLength = bytesRead;
                }
                else {
                    try {
                        bytesRead = band.read(posInBand, data, curOff,
                                bytesToRead);
                    } catch(RuntimeIOException ex) {
                        final IOException cause = ex.getIOCause();

                        if(cause != null) {
                            throw new RuntimeIOException("Exception while " +
                                    "reading from band " + bandNumber + ".",
                                    cause);
                        }

                        throw ex;
                    }

                    if(bytesRead < 0) {
                        if(bytesRead != -1)
                            throw new RuntimeException("Unexpected return " +
                                    "value from Band.read: " + bytesRead);
                        break;
                    }

                    bandHoleStart = 0;
                    bandHoleLength = 0;
                }

                if(holeList != null) {
                    if(curHoleLength != 0) {
                        if(bandHoleLength != 0 &&
                            (curHoleStart + curHoleLength) == bandHoleStart)
                        {
                            /* Concatenate with previous hole. */
                            curHoleLength += bandHoleLength;
                        }
                        else {
                            /* Add previous hole to list and begin new hole. */
                            holeList.add(new Pair<Long, Long>(curHoleStart,
                                    curHoleLength));

                            if(bandHoleLength != 0) {
                                curHoleStart = bandHoleStart;
                                curHoleLength = bandHoleLength;
                            }
                        }
                    }
                    else if(bandHoleLength != 0) {
                        curHoleStart = bandHoleStart;
                        curHoleLength = bandHoleLength;
                    }
                }
            }

            curOff += bytesRead;
            pos += bytesRead;
            remainingSize -= bytesRead;

            if(bytesRead != bytesToRead)
                break;
        }

        if(holeList != null && curHoleLength != 0) {
            holeList.add(new Pair<Long, Long>(curHoleStart,
                    curHoleLength));
        }

        return readSize - remainingSize;
    }
}
