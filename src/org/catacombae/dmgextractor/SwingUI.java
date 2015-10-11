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
 */

package org.catacombae.dmgextractor;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.filechooser.FileFilter;
import org.catacombae.dmgextractor.ui.PasswordDialog;

/**
 * User interface implementation using Java Swing.
 *
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
class SwingUI extends BasicUI implements UserInterface {

    private ProgressMonitor progmon = null;
    private String inputFilename = null;
    private String outputFilename = null;
    
    public SwingUI(boolean verbose) {
        super(verbose);

        //System.setProperty("swing.aatext", "true"); //Antialiased text
        try {
            javax.swing.UIManager.setLookAndFeel(
                    javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch(Exception e) {
            // No big deal. Go with default.
        }
    }

    /** {@inheritDoc} */
    public boolean warning(String... messageLines) {
        StringBuilder sb = new StringBuilder();

        boolean firstLine = true;
        for(String s : messageLines) {
            if(!firstLine)
                sb.append("\n");
            else
                firstLine = false;
            sb.append(s);
        }

        sb.append("\n\nDo you want to continue?");

        int res = JOptionPane.showConfirmDialog(null, sb.toString(),
                DMGExtractor.APPNAME + ": Warning", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return res == JOptionPane.YES_OPTION;
    }

    /** {@inheritDoc} */
    public void error(String... messageLines) {
        StringBuilder sb = new StringBuilder();

        boolean firstLine = true;
        for(String s : messageLines) {
            if(!firstLine)
                sb.append("\n");
            else
                firstLine = false;
            sb.append(s);
        }

        JOptionPane.showMessageDialog(null, sb.toString(),
                DMGExtractor.APPNAME + ": Error", JOptionPane.ERROR_MESSAGE);
    }

    /** {@inheritDoc} */
    public void reportProgress(int progressPercentage) {
        if(progressPercentage != previousPercentage) {
            if(progmon == null) {
                final String progmonText;
                if(outputFilename != null) {
                    progmonText = "Extracting \"" +
                            inputFilename + "\" to\n    \"" + outputFilename + "\"...";
                }
                else {
                    progmonText = "Simulating extraction of \"" +
                            inputFilename + "\"...";
                }
                progmon = new ProgressMonitor(null, progmonText, "0%", 0, 100);
                progmon.setProgress(0);
                progmon.setMillisToPopup(0);
            }
            progmon.setProgress((int) progressPercentage);
            progmon.setNote(progressPercentage + "%");
        }
    }

    /** {@inheritDoc} */
    public void reportFinished(boolean simulation, int errorsReported, int warningsReported, long totalExtractedSize) {
        StringBuilder message = new StringBuilder();
        if(simulation)
            message.append("Simulation");
        else
            message.append("Extraction");
        message.append(" complete! ");
        if(errorsReported != 0)
            message.append(errorsReported).append(" errors reported");
        else
            message.append("No errors reported");

        if(warningsReported != 0)
            message.append(" (").append(warningsReported).append(" warnings emitted)");

        message.append(".\nSize of extracted data: ").append(totalExtractedSize);
        message.append(" bytes");

        progmon.close();
        JOptionPane.showMessageDialog(null, message.toString(),
                DMGExtractor.APPNAME, JOptionPane.INFORMATION_MESSAGE);
        System.exit(0); // TODO check this
    }

    /** {@inheritDoc} */
    public boolean cancelSignaled() {
        return progmon != null && progmon.isCanceled();
    }

    /** {@inheritDoc} */
    public void displayMessage(String... messageLines) {
        StringBuilder resultString = new StringBuilder();
        boolean firstIteration = true;
        for(String s : messageLines) {
            if(!firstIteration)
                resultString.append("\n");
            else
                firstIteration = false;

            resultString.append(s);
        }

        JOptionPane.showMessageDialog(null, resultString.toString(),
                DMGExtractor.APPNAME, JOptionPane.INFORMATION_MESSAGE);
    }

    /** {@inheritDoc} */
    public File getInputFileFromUser() {
        SimpleFileFilter sff = new SimpleFileFilter();
        sff.addExtension("dmg");
        sff.addExtension("sparsebundle");
        sff.setDescription("Mac OS X disk images (*.dmg, *.sparsebundle)");
        JFileChooser jfc = new JFileChooser();
        jfc.setFileFilter(sff);
        jfc.setMultiSelectionEnabled(false);
        jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        jfc.setDialogTitle("Choose the image file to read...");
        while(true) {
            if(jfc.showDialog(null, "Open") == JFileChooser.APPROVE_OPTION) {
                File f = jfc.getSelectedFile();
                if(f.exists()) {
                    return f;
                }
                else
                    JOptionPane.showMessageDialog(null, "The file does not exist! Choose again...",
                            "Error", JOptionPane.ERROR_MESSAGE);
            }
            else
                return null;
        }
    }

    /** {@inheritDoc} */
    public boolean getOutputConfirmationFromUser() {
        return JOptionPane.showConfirmDialog(null, "Do you want to specify an output file?\n" +
                "(Choosing \"No\" means the extraction will only be simulated,\n" +
                "which can be useful for detecting errors in .dmg files...)",
                "Confirmation", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** {@inheritDoc} */
    public File getOutputFileFromUser(File inputFile) {
        final String msgFileExists = "The file already exists. Do you want to overwrite?";

        JFileChooser jfc = new JFileChooser();
        jfc.setMultiSelectionEnabled(false);
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        SimplerFileFilter defaultFileFilter = new SimplerFileFilter(".iso", "CD/DVD image (*.iso)");
        jfc.addChoosableFileFilter(defaultFileFilter);
        jfc.addChoosableFileFilter(new SimplerFileFilter(".img", "Raw image (*.img)"));
        jfc.addChoosableFileFilter(new SimplerFileFilter(".bin", "Binary file (*.bin)"));
        jfc.addChoosableFileFilter(new SimplerFileFilter(".dmg", "Mac OS X read/write disk image (*.dmg)"));
        jfc.setFileFilter(defaultFileFilter);
        jfc.setDialogTitle("Select your output file");
        
        if(inputFile != null) {
            String name = inputFile.getName();
            String defaultOutName = name;
            int lastDotIndex = defaultOutName.lastIndexOf(".");
            if(lastDotIndex >= 0) {
                defaultOutName = defaultOutName.substring(0, lastDotIndex);
            }
            jfc.setSelectedFile(new File(inputFile.getParentFile(),
                    defaultOutName));
        }

        while(true) {
            if(jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = jfc.getSelectedFile();
                final File saveFile;
                FileFilter selectedFileFilter = jfc.getFileFilter();
                if(selectedFileFilter instanceof SimplerFileFilter) {
                    SimplerFileFilter sff = (SimplerFileFilter) selectedFileFilter;
                    if(!selectedFile.getName().endsWith(sff.getExtension()))
                        saveFile = new File(selectedFile.getParentFile(), selectedFile.getName() + sff.getExtension());
                    else
                        saveFile = selectedFile;
                }
                else {
                    saveFile = selectedFile;
                }

                if(!saveFile.exists())
                    return saveFile;
                else if(JOptionPane.showConfirmDialog(null, msgFileExists,
                        "Confirmation", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                    return saveFile;
                }
            }
            else
                return null;
        }
    }

    /** {@inheritDoc} */
    public char[] getPasswordFromUser() {
        return PasswordDialog.showDialog(null, "Reading encrypted disk image...",
                "You need to enter a password to unlock this disk image:");
    }

    public void setProgressFilenames(String inputFilename, String outputFilename) {
        this.inputFilename = inputFilename;
        this.outputFilename = outputFilename;
    }
}
