/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.catacombae.dmg.sparsebundle;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.catacombae.io.BasicReadableRandomAccessStream;
import org.catacombae.io.RuntimeIOException;

/**
 *
 * @author erik
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

    ReadableSparseBundleStream(final SparseBundle bundle) {
        this.bundle = bundle;
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
        return bundle.getSize();
    }

    @Override
    public long getFilePointer() throws RuntimeIOException {
        return pos;
    }

    @Override
    public int read(final byte[] data, final int off, final int len)
            throws RuntimeIOException {
        if(data == null)
            throw new IllegalArgumentException("data is null.");
        if(off < 0 || off > data.length)
            throw new IllegalArgumentException("pos out of range.");
        if(len < 0 || len > (data.length - off))
            throw new IllegalArgumentException("len out of range.");

        final long bundleSize = bundle.getSize();
        if(pos >= bundleSize)
            return -1;

        final long bytesRemainingInStream = bundleSize - pos;
        final int readSize;
        if(len > bytesRemainingInStream)
            readSize = (int) bytesRemainingInStream;
        else
            readSize = len;

        final long bandSize = bundle.getBandSize();
        int curOff = off;
        int remainingSize = readSize;
        while(remainingSize > 0) {
            final long curBandNumber = pos / bandSize;
            final long posInBand = pos % bandSize;

            if(band == null)
                band = bundle.lookupBand(curBandNumber);
            else if(curBandNumber != bandNumber) {
                band.close();
                band = bundle.lookupBand(curBandNumber);
            }

            bandNumber = curBandNumber;

            final long remainingInBand = bandSize - posInBand;
            final int bytesToRead;
            if(remainingSize > remainingInBand)
                bytesToRead = (int) remainingInBand;
            else
                bytesToRead = remainingSize;

            final int bytesRead;
            if(band == null) {
                Arrays.fill(data, curOff, curOff+bytesToRead, (byte) 0);
                bytesRead = bytesToRead;
            }
            else {
                try {
                    bytesRead = band.read(posInBand, data, curOff, bytesToRead);
                } catch(IOException ex) {
                    throw new RuntimeIOException("Exception while reading " +
                            "from band " + bandNumber + ".", ex);
                }

                if(bytesRead < 0) {
                    if(bytesRead != -1)
                        throw new RuntimeException("Unexpected return value " +
                                "from Band.read: " + bytesRead);
                    break;
                }
            }

            curOff += bytesRead;
            pos += bytesRead;
            remainingSize -= bytesRead;

            if(bytesRead != bytesToRead)
                break;
        }

        return readSize - remainingSize;
    }
}
