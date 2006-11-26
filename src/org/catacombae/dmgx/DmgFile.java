package org.catacombae.dmgx;

import java.io.*;

public class DmgFile {
    private File file;
    private DmgFileView dmgView;
    
    public DmgFile(File file) {
	this.file = file;
	this.dmgView = new DmgFileView(file);
    }
}