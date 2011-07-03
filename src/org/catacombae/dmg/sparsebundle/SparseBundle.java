/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.catacombae.dmg.sparsebundle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import org.catacombae.io.RuntimeIOException;

/**
 *
 * @author Erik Larsson
 */
class SparseBundle {
    private static final String mainInfoFilename = "Info.plist";
    private static final String backupInfoFilename = "Info.bckup";
    private static final String tokenFilename = "token";
    private static final String bandsDirname = "bands";

    private final Info mainInfo;
    private final Info backupInfo;
    private final Token token;
    private final File bandsDir;

    private long size;
    private long bandSize;
    private long bandCount;

    public SparseBundle(final File sparseBundleDir) throws RuntimeIOException {
        File[] files = sparseBundleDir.listFiles();
        File mainInfoFile = null;
        File backupInfoFile = null;
        File tokenFile = null;
        File bandsDir = null;

        for(File f : files) {
            if(f.getName().equals(mainInfoFilename))
                mainInfoFile = f;
            else if(f.getName().equals(backupInfoFilename))
                backupInfoFile = f;
            else if(f.getName().equals(tokenFilename))
                tokenFile = f;
            else if(f.getName().equals(bandsDirname))
                bandsDir = f;
            else
                System.err.println("Warning: Encountered unknown file \"" +
                        f.getName() + " in sparse bundle base dir (\"" +
                        sparseBundleDir.getAbsolutePath() + "\").");
        }

        if(mainInfoFile == null || backupInfoFile == null ||
                tokenFile == null || bandsDir == null) {
            throw new RuntimeIOException("Some files are missing from the " +
                    "sparse bundle directory (\"" +
                    sparseBundleDir.getAbsolutePath() + "\".");
        }
        else if(!bandsDir.exists() || !bandsDir.isDirectory())
            throw new RuntimeIOException("Invalid '" + bandsDirname + "' " +
                    "directory.");

        final RandomAccessFile mainInfoRaf;
        final RandomAccessFile backupInfoRaf;
        final RandomAccessFile tokenRaf;

        try {
            mainInfoRaf = new RandomAccessFile(mainInfoFile, "r");
        } catch(FileNotFoundException ex) {
            throw new RuntimeIOException("Failed to open '" + mainInfoFilename +
                    "' for reading.", ex);
        }

        try {
            backupInfoRaf = new RandomAccessFile(backupInfoFile, "r");
        } catch(FileNotFoundException ex) {
            throw new RuntimeIOException("Failed to open '" +
                    backupInfoFilename + "' for reading.", ex);
        }

        try {
            tokenRaf = new RandomAccessFile(tokenFile, "r");
        } catch(FileNotFoundException ex) {
            throw new RuntimeIOException("Failed to open '" + tokenFilename +
                    "' for reading.", ex);
        }

        final FileLock mainInfoLock;
        final FileLock backupInfoLock;
        final FileLock tokenLock;

        try {
            mainInfoLock = mainInfoRaf.getChannel().lock(0L, Long.MAX_VALUE,
                    true);
        } catch (IOException ex) {
            throw new RuntimeIOException("Failed to aquire a shared lock on " +
                    "'" + mainInfoFilename + "'.", ex);
        }

        try {
            backupInfoLock = backupInfoRaf.getChannel().lock(0L, Long.MAX_VALUE,
                    true);
        } catch (IOException ex) {
            throw new RuntimeIOException("Failed to aquire a shared lock on " +
                    "'" + backupInfoFilename + "'.", ex);
        }

        try {
            tokenLock = tokenRaf.getChannel().lock(0L, Long.MAX_VALUE,
                    true);
        } catch (IOException ex) {
            throw new RuntimeIOException("Failed to aquire a shared lock on " +
                    "'" + tokenFilename + "'.", ex);
        }

        try { this.mainInfo = new Info(mainInfoRaf, mainInfoLock); }
        catch(IOException ioe) {
            throw new RuntimeIOException("Exception while parsing '" +
                    mainInfoFilename + "'.", ioe);
        }

        try { this.backupInfo = new Info(backupInfoRaf, backupInfoLock); }
        catch(IOException ioe) {
            throw new RuntimeIOException("Exception while parsing '" +
                    backupInfoFilename + "'.", ioe);
        }

        this.token = new Token(tokenRaf, tokenLock);

        this.bandsDir = bandsDir;

        /* Check the 'bands' directory

        /* Cached variables. */
        this.size = mainInfo.getSize();
        this.bandSize = mainInfo.getBandSize();
        this.bandCount = (mainInfo.getSize() + mainInfo.getBandSize() - 1) /
                mainInfo.getBandSize();

        checkBandsDir();
    }

    private void checkBandsDir() throws RuntimeIOException {
        for(File f : bandsDir.listFiles()) {
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
        final File bandFile = new File(bandsDir, bandFilename);
        if(!bandFile.exists())
            return null;

        final RandomAccessFile bandRaf;
        try {
            bandRaf = new RandomAccessFile(bandFile, "r");
        } catch(FileNotFoundException ex) {
            throw new RuntimeIOException("Failed to open '" + bandFilename +
                    "' for reading.", ex);
        }

        final FileLock bandLock;
        try {
            bandLock = bandRaf.getChannel().lock(0L, Long.MAX_VALUE, true);
        } catch (IOException ex) {
            throw new RuntimeIOException("Failed to aquire a shared lock on " +
                    "'" + bandFilename + "'.", ex);
        }

        final long curBandSize;
        try { curBandSize = bandRaf.length(); }
        catch(IOException ioe) {
            throw new RuntimeIOException("Exception while querying band file " +
                    "length.", ioe);
        }

        if(curBandSize > bandSize)
            throw new RuntimeIOException("Invalid band: Size (" + curBandSize +
                    ") is larger than bandSize (" + bandSize + ").");

        try { return new Band(bandRaf, bandLock, bandSize); }
        catch(IOException ioe) {
            throw new RuntimeIOException("Exception while creating Band " +
                    "instance.", ioe);
        }
    }

    void close() {
        mainInfo.close();
        backupInfo.close();
        token.close();
    }
}
