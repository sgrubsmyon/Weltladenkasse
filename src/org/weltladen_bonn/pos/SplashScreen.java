package org.weltladen_bonn.pos;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;

import org.weltladen_bonn.pos.BaseClass.BigLabel;

public class SplashScreen extends JFrame {
    private static final long serialVersionUID = 1L;

    BigLabel statusLabel;
    JProgressBar progressBar;
    JButton cancelButton;

    public SplashScreen(ImageIcon imageIcon, String title) {
        setTitle(title);

        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(imageIcon);
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(imageLabel, BorderLayout.CENTER);
        
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.PAGE_AXIS));
        southPanel.setBackground(Color.BLACK);
        JPanel statusPanel = new JPanel(new FlowLayout());
        statusPanel.setBackground(Color.BLACK);
        statusPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel = new BigLabel("...");
        statusLabel.setForeground(Color.WHITE);
        cancelButton = new JButton("Abbrechen");
        cancelButton.setEnabled(false);
        statusPanel.add(statusLabel);
        statusPanel.add(cancelButton);
        southPanel.add(statusPanel);

        progressBar = new JProgressBar();
        // progressBar.setIndeterminate(true);
        progressBar.setMaximum(100);
        progressBar.setPreferredSize(new Dimension(400, 30));
        progressBar.setMaximumSize(new Dimension(400, 30));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        southPanel.add(progressBar);
        this.getContentPane().add(southPanel, BorderLayout.SOUTH);
        
        this.pack();
        this.setLocationRelativeTo(null); // this centers the window if called after "pack()"
        this.setVisible(true);
    }

    public void setStatusLabel(String status) {
        this.statusLabel.setText(status);
    }

    public void setProgress(int progress) {
        this.progressBar.setValue(progress);
    }

    public void enableCancelButton(boolean enabled) {
        this.cancelButton.setEnabled(enabled);
    }

    public void addCancelButtonListener(ActionListener listener) {
        this.cancelButton.addActionListener(listener);
    }
    
    public void removeCancelButtonListener(ActionListener listener) {
        this.cancelButton.removeActionListener(listener);
    }
}
