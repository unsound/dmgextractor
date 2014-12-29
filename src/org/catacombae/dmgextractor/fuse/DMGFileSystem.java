/*-
 * Copyright (C) 2014 Erik Larsson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catacombae.dmgextractor.fuse;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.catacombae.dmg.udif.PlistPartition;
import org.catacombae.dmg.udif.UDIFFile;
import org.catacombae.dmg.udif.UDIFRandomAccessStream;
import org.catacombae.io.ReadableRandomAccessStream;
import org.catacombae.jfuse.FUSE26FileSystemAdapter;
import org.catacombae.jfuse.types.fuse26.FUSEFileInfo;
import org.catacombae.jfuse.types.fuse26.FUSEFillDir;
import org.catacombae.jfuse.types.system.Stat;
import org.catacombae.jfuse.util.FUSEUtil;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public class DMGFileSystem extends FUSE26FileSystemAdapter {
    private static final String wholeFilename = "whole.img";
    private static final String udifPartitionFilenamePrefix = "udif_partition_";
    private static final String udifPartitionFilenameSuffix = ".img";

    private static class UDIFPartition {
        final long offset;
        final long length;
        final String id;

        public UDIFPartition(long offset, long length, String id) {
            this.offset = offset;
            this.length = length;
            this.id = id;
        }
    }

    private final ReadableRandomAccessStream dmgStream;
    private final UDIFPartition[] partitions;

    public DMGFileSystem(UDIFFile udifFile) throws IOException {
        final PlistPartition[] plistPartitions =
                udifFile.getView().getPlist().getPartitions();

        this.partitions = new UDIFPartition[plistPartitions.length];

        long offset = 0;
        for(int i = 0; i < partitions.length; ++i) {
            final PlistPartition p = plistPartitions[i];
            final long partitionSize = p.getPartitionSize();

            this.partitions[i] = new UDIFPartition(offset, partitionSize,
                    p.getID());
            offset += partitionSize;
        }

        this.dmgStream = new UDIFRandomAccessStream(udifFile);
    }

    public DMGFileSystem(ReadableRandomAccessStream dmgStream)
            throws IOException
    {
        this.dmgStream = dmgStream;
        this.partitions = new UDIFPartition[0];
    }

    private UDIFPartition getPartitionByPath(final String path) {
        if(partitions.length == 0) {
            return null;
        }

        if(!path.startsWith("/" + udifPartitionFilenamePrefix) ||
                !path.endsWith(udifPartitionFilenameSuffix))
        {
            return null;
        }

        final String partitionId =
                path.substring(("/" + udifPartitionFilenamePrefix).length(),
                path.length() - udifPartitionFilenameSuffix.length());
        for(UDIFPartition p : partitions) {
            if(p.id.equals(partitionId)) {
                return p;
            }
        }

        return null;
    }

    @Override
    public int getattr(ByteBuffer rawPath, Stat stbuf) {
        int ret = 0;
        final String path = FUSEUtil.decodeUTF8(rawPath);

        UDIFPartition p;
        if(path.equals("/")) {
            stbuf.st_mode = Stat.S_IFDIR | 0555;
            stbuf.st_nlink = 2;
        }
        else if(path.equals("/" + wholeFilename)) {
            stbuf.st_mode = Stat.S_IFREG | 0444;
            stbuf.st_nlink = 1;
            stbuf.st_size = dmgStream.length();
        }
        else if((p = getPartitionByPath(path)) != null) {
            stbuf.st_mode = Stat.S_IFREG | 0444;
            stbuf.st_nlink = 1;
            stbuf.st_size = p.length;
        }
        else {
            ret = -ENOENT;
        }

        return ret;
    }

    @Override
    public int open(ByteBuffer rawPath, FUSEFileInfo fi) {
        int ret;
        final String path = FUSEUtil.decodeUTF8(rawPath);

        if(path.equals("/")) {
            ret = -EISDIR;
        }
        else if(path.equals("/" + wholeFilename) ||
                getPartitionByPath(path) != null)
        {
            if(fi.getFlagAppend() || fi.getFlagCreate() || fi.getFlagExcl() ||
                    fi.getFlagExclusiveLock() || fi.getFlagReadWrite() ||
                    fi.getFlagWriteOnly())
            {
                ret = -EACCES;
            }
            else {
                ret = 0;
            }
        }
        else {
            ret = -ENOENT;
        }

        return ret;
    }

    @Override
    public int read(ByteBuffer rawPath, ByteBuffer dest, long off,
            FUSEFileInfo fi)
    {
        int ret = 0;
        final String path = FUSEUtil.decodeUTF8(rawPath);

        UDIFPartition p = null;
        if(path.equals("/")) {
            ret = -EISDIR;
        }
        else if(path.equals("/" + wholeFilename) ||
                (p = getPartitionByPath(path)) != null)
        {
            int readLength;
            if(p == null) {
                readLength = dest.capacity();
                dmgStream.seek(off);
            }
            else {
                final int capacity = dest.capacity();
                final long endOffset = off + capacity;

                readLength =
                        endOffset > p.length ? (int) (p.length - off) :
                            capacity;
                dmgStream.seek(p.offset + off);

            }

            byte[] tmp = new byte[readLength];
            ret = dmgStream.read(tmp);
            dest.put(tmp);
        }
        else {
            ret = -ENOENT;
        }

        return ret;
    }

    @Override
    public int readdir(ByteBuffer rawPath, FUSEFillDir filler, long offset,
            FUSEFileInfo fi)
    {
        int ret = 0;
        final String path = FUSEUtil.decodeUTF8(rawPath);

        if(path.equals("/")) {
            filler.fill(FUSEUtil.encodeUTF8("."), null, 0);
            filler.fill(FUSEUtil.encodeUTF8(".."), null, 0);
            filler.fill(FUSEUtil.encodeUTF8(wholeFilename), null, 0);
            for(int i = 0; i < partitions.length; ++i) {
                filler.fill(FUSEUtil.encodeUTF8(udifPartitionFilenamePrefix +
                        partitions[i].id + udifPartitionFilenameSuffix), null,
                        offset);
            }
        }
        else if(path.equals("/" + wholeFilename)) {
            ret = -ENOTDIR;
        }
        else if(getPartitionByPath(path) != null) {
            ret = -ENOTDIR;
        }
        else {
            ret = -ENOENT;
        }

        return ret;
    }
}
