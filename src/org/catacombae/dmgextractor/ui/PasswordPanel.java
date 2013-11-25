/*-
 * Copyright (C) 2008 Erik Larsson
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

package org.catacombae.dmgextractor.ui;

import java.awt.event.ActionListener;
//import javax.swing.JFrame;

/**
 * The UI design component for PasswordDialog.
 * 
 * @author Erik Larsson
 */
class PasswordPanel extends javax.swing.JPanel {

    /** Creates new form PasswordPanel */
    public PasswordPanel(String messageLine, ActionListener okButtonListener, ActionListener cancelButtonListener) {
        this();
        instructionsLabel.setText(messageLine);
        passwordField.addActionListener(okButtonListener);
        okButton.addActionListener(okButtonListener);
        cancelButton.addActionListener(cancelButtonListener);
    }
    
    private PasswordPanel() {
        initComponents();
    }
    
    char[] getPassword() {
        return passwordField.getPassword();
    }
       
    /*
    public static void main(String[] args) {
        JFrame jp = new JFrame();
        PasswordPanel pp = new PasswordPanel();
        pp.instructionsLabel.setText("tada\nYADA\nBADA");
        jp.add(pp);
        jp.pack();
        jp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jp.setVisible(true);
    }
    */

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        passwordField = new javax.swing.JPasswordField();
        instructionsLabel = new javax.swing.JLabel();
        buttonLayoutHelper = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        instructionsLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        instructionsLabel.setText("You need to enter a password to unlock this disk image:");

        buttonLayoutHelper.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 0));

        okButton.setText("OK");
        buttonLayoutHelper.add(okButton);

        cancelButton.setText("Cancel");
        buttonLayoutHelper.add(cancelButton);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(passwordField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE)
                    .add(instructionsLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE)
                    .add(buttonLayoutHelper, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(instructionsLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(passwordField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(buttonLayoutHelper, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel buttonLayoutHelper;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel instructionsLabel;
    private javax.swing.JButton okButton;
    private javax.swing.JPasswordField passwordField;
    // End of variables declaration//GEN-END:variables
 }
