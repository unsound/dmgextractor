/*-
 * Copyright (C) 2006 Erik Larsson
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

package org.catacombae.dmgextractor.utils;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import javax.swing.JFrame;
import net.iharder.dnd.FileDrop;
import org.catacombae.dmg.udif.Koly;
import org.catacombae.dmgextractor.utils.gui.DMGInfoPanel;

public class DMGInfoWindow extends JFrame {
    private DMGInfoPanel infoPanel;
    
    public DMGInfoWindow() {
        infoPanel = new DMGInfoPanel();
        add(infoPanel, BorderLayout.CENTER);

        // Register handler for file drag&drop events
        new FileDrop(this, new FileDrop.Listener() {

            public void filesDropped(java.io.File[] files) {
                if(files.length > 0) {
                    try {
                        loadFile(files[0]);
                    } catch(IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        });

        pack();
        setLocationRelativeTo(null);

    }

    private void loadFile(File f) throws IOException {
        RandomAccessFile inputFile;
        byte[] kolyData = new byte[512];
        String kolySignature;

        inputFile = new RandomAccessFile(f, "r");

        inputFile.seek(inputFile.length() - 512);
        inputFile.readFully(kolyData);
        kolySignature = new String(kolyData, 0, 4, "US-ASCII");
        if(!kolySignature.equals("koly")) {
            System.out.println("ERROR: Invalid signature. Found " +
                    "\"" + kolySignature + "\" instead of \"koly\".");
            return;
        }

        infoPanel.setKoly(new Koly(kolyData, 0));

        infoPanel.setGeneralInfo(f.getName(), inputFile.length(), 0);
    }

    public static void main(String[] args) {
        DMGInfoWindow window = new DMGInfoWindow();

        if(args.length > 0) {
            try {
                window.loadFile(new File(args[0]));
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }

        window.setVisible(true);
    }
}
