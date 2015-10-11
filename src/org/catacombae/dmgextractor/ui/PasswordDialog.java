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

package org.catacombae.dmgextractor.ui;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public class PasswordDialog extends JDialog {
    private final PasswordPanel passwordPanel;
    private char[] password = null;
    
    private PasswordDialog(Frame owner, boolean modal, String dialogTitle, String messageLine) {
        super(owner, dialogTitle, modal);
        
        ActionListener okButtonListener = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                actionOkButtonClicked();
            }
            
        };
        ActionListener cancelButtonListener = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                actionCancelButtonClicked();
            }
            
        };
        this.passwordPanel = new PasswordPanel(messageLine, okButtonListener, cancelButtonListener);
        add(passwordPanel);
    }
    private void actionOkButtonClicked() {
        password = passwordPanel.getPassword();
        /* It is important to call dispose() to dispose of AWT resources, allowing the calling
         * thread to exit the program at will. Otherwise, an active AWT thread will block. */
        dispose();
    }
    
    private void actionCancelButtonClicked() {
        password = null;
        dispose();
    }
    private char[] getPassword() {
        return password;
    }
    
    /**
     * Shows a dialog, modal to <code>owner</code> which requests a password from the user, halts
     * execution until the password has been entered, and then returns the entered password, or
     * <code>null</code> depending on whether the user clicked the "Ok" or the "Cancel" button.
     *
     * @param parentComponent this dialog's parent component.
     * @param dialogTitle the title of the dialog, printed in the window header.
     * @param messageLine the one line message to display above the password field, for example
     * "Please enter password:" or anything similar.
     * @return the entered password, or null if the user canceled the dialog.
     */
    public static char[] showDialog(Component parentComponent, String dialogTitle, String messageLine) {
        PasswordDialog pd = new PasswordDialog(JOptionPane.getFrameForComponent(parentComponent), true, dialogTitle, messageLine);
        pd.pack();
        pd.setResizable(false);
        pd.setLocationRelativeTo(null);
        pd.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pd.setVisible(true);
        return pd.getPassword();
    }
    
    /*
    public static void main(String[] args) {
        char[] pwd = showDialog(null, "hej", "apa");
        //String pwd = JOptionPane.showInputDialog(null, "apa", "hej");
        if(pwd != null)
            System.out.println("Password: \"" + new String(pwd) + "\"");
        else
            System.out.println("User canceled dialog.");
    }
    */
}
