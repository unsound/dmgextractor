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