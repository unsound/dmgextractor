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

import java.io.IOException;
import java.io.Reader;
import org.catacombae.io.RuntimeIOException;
import org.catacombae.plist.PlistNode;
import org.catacombae.plist.XmlPlist;
import org.catacombae.util.Util;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
class Info extends BundleMember {
    private long bandSize;
    private long size;

    public Info(FileAccessor file, boolean fileLocked) throws RuntimeIOException
    {
        super(file, fileLocked);
        refresh();
    }

    public long getBandSize() { return bandSize; }
    public long getSize() { return size; }

    /**
     * Re-reads the contents of the .plist file and updates the cached data.
     *
     * @throws IOException if there is an I/O error, or the data in the plist is
     * invalid.
     */
    protected void refresh() throws RuntimeIOException {
        long fileLength = stream.length();
        if(fileLength > Integer.MAX_VALUE)
            throw new ArrayIndexOutOfBoundsException("Info.plist is " +
                    "unreasonably large and doesn't fit in memory.");

        byte[] plistData = new byte[(int) fileLength];
        int bytesRead = stream.read(plistData);
        if(bytesRead != fileLength)
            throw new RuntimeIOException("Failed to read entire file. Read " +
                    bytesRead + "/" + fileLength + " bytes.");

        XmlPlist plist = new XmlPlist(plistData, true);
        PlistNode dictNode = plist.getRootNode().cd("dict");
        if(dictNode == null) {
            throw new RuntimeIOException("Malformed Info.plist file: No " +
                    "'dict' element at root.");
        }

        final String cfBundleInfoDictionaryVersionKey =
                "CFBundleInfoDictionaryVersion";
        final String bandSizeKey =
                "band-size";
        final String bundleBackingstoreVersionKey =
                "bundle-backingstore-version";
        final String diskImageBundleTypeKey =
                "diskimage-bundle-type";
        final String sizeKey =
                "size";

        Reader cfBundleInfoDictionaryVersionReader =
                dictNode.getKeyValue(cfBundleInfoDictionaryVersionKey);
        Reader bandSizeReader =
                dictNode.getKeyValue(bandSizeKey);
        Reader bundleBackingstoreVersionReader =
                dictNode.getKeyValue(bundleBackingstoreVersionKey);
        Reader diskImageBundleTypeReader =
                dictNode.getKeyValue(diskImageBundleTypeKey);
        Reader sizeReader =
                dictNode.getKeyValue(sizeKey);

        if(cfBundleInfoDictionaryVersionReader == null)
            throw new RuntimeIOException("Could not find '" +
                    cfBundleInfoDictionaryVersionKey + "' key in Info.plist " +
                    "file.");
        if(bandSizeReader == null)
            throw new RuntimeIOException("Could not find '" + bandSizeKey +
                    "' key in Info.plist file.");
        if(bundleBackingstoreVersionReader == null)
            throw new RuntimeIOException("Could not find '" +
                    bundleBackingstoreVersionKey + "' key in Info.plist file.");
        if(diskImageBundleTypeReader == null)
            throw new RuntimeIOException("Could not find '" +
                    diskImageBundleTypeKey + "' key in Info.plist file.");
        if(sizeReader == null)
            throw new RuntimeIOException("Could not find '" + sizeKey + "' " +
                    "key in Info.plist file.");

        // We ignore the value of the dictionary version.
        //String cfBundleInfoDictionaryVersionString =
        //        Util.readFully(cfBundleInfoDictionaryVersionReader);
        String bandSizeString;
        String bundleBackingstoreVersionString;
        String diskImageBundleTypeString;
        String sizeString;

        try {
            bandSizeString = Util.readFully(bandSizeReader);
            bundleBackingstoreVersionString =
                    Util.readFully(bundleBackingstoreVersionReader);
            diskImageBundleTypeString =
                    Util.readFully(diskImageBundleTypeReader);
            sizeString = Util.readFully(sizeReader);
        } catch(IOException ex) {
            throw new RuntimeIOException(ex);
        }

        if(!diskImageBundleTypeString.equals(
                "com.apple.diskimage.sparsebundle")) {
            throw new RuntimeIOException("Unexpected value for '" +
                    diskImageBundleTypeKey + "': " + diskImageBundleTypeString);

        }

        if(!bundleBackingstoreVersionString.equals("1")) {
            throw new RuntimeIOException("Unknown backing store version: " +
                    bundleBackingstoreVersionString);

        }

        final long bandSizeLong;
        try {
            bandSizeLong = Long.parseLong(bandSizeString);
        } catch(NumberFormatException nfe) {
            throw new RuntimeIOException("Illegal numeric value for " +
                    bandSizeKey + ": " + bandSizeString);
        }

        final long sizeLong;
        try {
            sizeLong = Long.parseLong(sizeString);
        } catch(NumberFormatException nfe) {
            throw new RuntimeIOException("Illegal numeric value for " +
                    sizeKey + ": " + sizeString);
        }

        this.bandSize = bandSizeLong;
        this.size = sizeLong;
    }
}
