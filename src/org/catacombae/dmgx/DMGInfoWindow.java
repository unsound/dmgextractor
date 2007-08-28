/*-
 * Copyright (C) 2006 Erik Larsson
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

package org.catacombae.dmgx;

import net.iharder.dnd.FileDrop;
import org.catacombae.dmgx.gui.*;
import java.awt.*;
import javax.swing.*;

public class DMGInfoWindow extends JFrame {
    private DMGInfoPanel infoPanel;
    
    public DMGInfoWindow() {
	infoPanel = new DMGInfoPanel();
	add(infoPanel, BorderLayout.CENTER);
	
	// Register handler for file drag&drop events
	new FileDrop(this, new FileDrop.Listener() {
		public void filesDropped(java.io.File[] files) {   
		    if(files.length > 0)
			;//loadFile(files[0]);
		}
	    });
	
	pack();
	setLocationRelativeTo(null);

    }
    public static void main(String[] args) {
	new DMGInfoWindow().setVisible(true);
    }
}
