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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import org.catacombae.io.ReadableFileStream;
import org.catacombae.io.ReadableRandomAccessStream;
import org.catacombae.io.ReadableRandomAccessSubstream;
import org.catacombae.io.RuntimeIOException;
import org.catacombae.io.SynchronizedReadableRandomAccessStream;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
class SparseBundle {
    private static final String mainInfoFilename = "Info.plist";
    private static final String backupInfoFilename = "Info.bckup";
    private static final String tokenFilename = "token";
    private static final String bandsDirname = "bands";

    private final Info mainInfo;
    private final Info backupInfo;
    private final Token token;
    private final FileAccessor bandsDir;

    private long size;
    private long bandSize;
    private long bandCount;

    public SparseBundle(final File sparseBundleDir) throws RuntimeIOException {
        this(new JavaFileAccessor(sparseBundleDir));
    }

    public SparseBundle(final FileAccessor sparseBundleDir) {
        FileAccessor[] files = sparseBundleDir.listFiles();
        FileAccessor mainInfoFile = null;
        FileAccessor backupInfoFile = null;
        FileAccessor tokenFile = null;
        FileAccessor bandsDirFile = null;

        boolean mainInfoFileLocked = false;
        boolean backupInfoFileLocked = false;
        boolean tokenFileLocked = false;

        for(FileAccessor f : files) {
            if(f.getName().equals(mainInfoFilename))
                mainInfoFile = f;
            else if(f.getName().equals(backupInfoFilename))
                backupInfoFile = f;
            else if(f.getName().equals(tokenFilename))
                tokenFile = f;
            else if(f.getName().equals(bandsDirname))
                bandsDirFile = f;
            else
                System.err.println("Warning: Encountered unknown file \"" +
                        f.getName() + " in sparse bundle base dir (\"" +
                        sparseBundleDir.getAbsolutePath() + "\").");
        }

        if(mainInfoFile == null || backupInfoFile == null ||
                tokenFile == null || bandsDirFile == null) {
            throw new RuntimeIOException("Some files are missing from the " +
                    "sparse bundle directory (\"" +
                    sparseBundleDir.getAbsolutePath() + "\").");
        }
        else if(!bandsDirFile.exists() || !bandsDirFile.isDirectory())
            throw new RuntimeIOException("Invalid '" + bandsDirname + "' " +
                    "directory.");

        try {
            mainInfoFile.lock();
            mainInfoFileLocked = true;
        } catch(RuntimeIOException ex) {
            System.err.println("Warning: Failed to acquire a shared lock on " +
                    "'" + mainInfoFilename + "'.");
        }

        try {
            backupInfoFile.lock();
            backupInfoFileLocked = true;
        } catch(RuntimeIOException ex) {
            System.err.println("Warning: Failed to acquire a shared lock on " +
                    "'" + backupInfoFilename + "'.");
        }

        try {
            tokenFile.lock();
            tokenFileLocked = true;
        } catch(RuntimeIOException ex) {
            System.err.println("Warning: Failed to acquire a shared lock on " +
                    "'" + tokenFilename + "'.");
        }

        try {
            this.mainInfo = new Info(mainInfoFile, mainInfoFileLocked);
        }
        catch(RuntimeIOException ex) {
            final IOException cause = ex.getIOCause();

            if(cause != null) {
                throw new RuntimeIOException("Exception while parsing '" +
                        mainInfoFilename + "'.", cause);
            }

            throw ex;
        }

        try {
            this.backupInfo = new Info(backupInfoFile, backupInfoFileLocked);
        }
        catch(RuntimeIOException ex) {
            final IOException cause = ex.getIOCause();

            if(cause != null) {
                throw new RuntimeIOException("Exception while parsing '" +
                        backupInfoFilename + "'.", cause);
            }

            throw ex;
        }

        this.token = new Token(tokenFile, tokenFileLocked);

        this.bandsDir = bandsDirFile;

        /* Cached variables. */
        this.size = mainInfo.getSize();
        this.bandSize = mainInfo.getBandSize();
        this.bandCount = (mainInfo.getSize() + mainInfo.getBandSize() - 1) /
                mainInfo.getBandSize();

        /* Check the 'bands' directory. */
        checkBandsDir();
    }

    private void checkBandsDir() throws RuntimeIOException {
        for(FileAccessor f : bandsDir.listFiles()) {
            if(!f.isFile())
                throw new RuntimeIOException("Encountered non-file content " +
                        "inside bands directory.");

            final String curName = f.getName();
            final long bandNumber;
            try {
                bandNumber = Long.parseLong(curName, 16);
            } catch(NumberFormatException nfe) {
                throw new RuntimeIOException("Encountered non-parseable " +
                        "filename in bands directory: \"" + curName + "\"");
            }

            //System.err.println("Found band number: " + bandNumber + " " +
            //        "(filename: \"" + curName + "\")");

            if(bandNumber < 0 || bandNumber > bandCount - 1)
                throw new RuntimeException("Invalid band number: " +
                        bandNumber);
        }
    }

    /**
     * Returns the size of the virtual device.
     *
     * @return the size of the virtual device.
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns the size of each band in the sparse bundle.
     *
     * @return the size of each band in the sparse bundle.
     */
    public long getBandSize() {
        return bandSize;
    }

    /**
     * Returns the number of bands that are part of this sparse bundle.
     *
     * @return the number of bands that are part of this sparse bundle.
     */
    public long getBandCount() {
        return bandCount;
    }

    Token getToken() {
        return token;
    }

    /**
     * Looks up and returns the {@link Band} with the specified band number.
     *
     * The caller is responsible for closing the {@link Band} when it is done
     * with it.
     *
     * @return the {@link Band} with the specified band number.
     */
    Band lookupBand(long bandNumber) throws RuntimeIOException {
        final String bandFilename = Long.toHexString(bandNumber);
        final FileAccessor bandFile = bandsDir.lookupChild(bandFilename);
        boolean bandFileLocked = false;
        if(!bandFile.exists())
            return null;

        try {
            bandFile.lock();
            bandFileLocked = true;
        } catch(RuntimeIOException ex) {
            System.err.println("Warning: Failed to acquire a shared lock on " +
                    "'" + bandFilename + "'.");
        }

        final long curBandSize;
        try { curBandSize = bandFile.length(); }
        catch(RuntimeIOException ex) {
            final IOException cause = ex.getIOCause();

            if(cause != null) {
                throw new RuntimeIOException("Exception while querying band " +
                        "file length.", cause);
            }

            throw ex;
        }

        if(curBandSize > bandSize)
            throw new RuntimeIOException("Invalid band: Size (" + curBandSize +
                    ") is larger than bandSize (" + bandSize + ").");

        try { return new Band(bandFile, bandFileLocked, bandSize); }
        catch(RuntimeIOException ex) {
            final IOException cause = ex.getIOCause();

            if(cause != null) {
                throw new RuntimeIOException("Exception while creating Band " +
                        "instance.", ex);
            }

            throw ex;
        }
    }

    void close() {
        mainInfo.close();
        backupInfo.close();
        token.close();
    }

    private static class JavaFileAccessor implements FileAccessor {
        private final File f;
        private RandomAccessFile raf = null;
        private SynchronizedReadableRandomAccessStream rafSyncStream = null;
        private FileLock lock = null;

        public JavaFileAccessor(final File f) {
            this.f = f;
        }

        public FileAccessor[] listFiles() {
            final File[] childFiles = f.listFiles();
            final FileAccessor[] childAccessors =
                    new FileAccessor[childFiles.length];

            for(int i = 0; i < childFiles.length; ++i) {
                childAccessors[i] = new JavaFileAccessor(childFiles[i]);
            }

            return childAccessors;
        }

        public boolean isFile() {
            return f.isFile();
        }

        public boolean isDirectory() {
            return f.isDirectory();
        }

        public String getName() {
            return f.getName();
        }

        public String getAbsolutePath() {
            return f.getAbsolutePath();
        }

        public boolean exists() {
            return f.exists();
        }

        public FileAccessor lookupChild(final String name) {
            return new JavaFileAccessor(new File(f, name));
        }

        private void initRaf() {
            if(raf == null) {
                RandomAccessFile newRaf = null;
                SynchronizedReadableRandomAccessStream newRafSyncStream = null;

                try {
                    newRaf = new RandomAccessFile(f, "r");
                    newRafSyncStream =
                            new SynchronizedReadableRandomAccessStream(
                            new ReadableFileStream(newRaf, f.getPath()));
                } catch(FileNotFoundException ex) {
                    throw new RuntimeIOException(ex);
                } catch(RuntimeException ex) {

                } finally {
                    if(newRafSyncStream == null) {
                        try {
                            newRaf.close();
                        } catch(IOException ex) {
                            throw new RuntimeIOException(ex);
                        }
                    }
                }

                raf = newRaf;
                rafSyncStream = newRafSyncStream;
            }
        }

        public synchronized long length() {
            initRaf();

            try {
                return raf.length();
            } catch(IOException ex) {
                throw new RuntimeIOException(ex);
            }
        }

        public synchronized ReadableRandomAccessStream createReadableStream() {
            initRaf();

            return new ReadableRandomAccessSubstream(rafSyncStream);
        }

        public synchronized void lock() {
            initRaf();

            if(lock != null) {
                throw new RuntimeException("Already locked!");
            }

            try {
                lock = raf.getChannel().lock(0L, Long.MAX_VALUE, true);
            } catch(IOException ex) {
                throw new RuntimeIOException(ex);
            }
        }

        public synchronized void unlock() {
            if(lock == null) {
                throw new RuntimeException("Not locked!");
            }

            try {
                lock.release();
                lock = null;
            } catch(IOException ex) {
                throw new RuntimeIOException(ex);
            }
        }

        public synchronized void close() {
            try {
                rafSyncStream.close();
                raf.close();
            } catch(IOException ex) {
                throw new RuntimeIOException(ex);
            }
        }
    }
}
