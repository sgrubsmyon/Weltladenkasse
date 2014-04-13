package Weltladenkasse;

// GUI stuff:
import java.awt.*;
import java.awt.event.*;
 
//import javax.swing.JFrame;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTable;
//import javax.swing.JTextArea;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;

import WeltladenDB.WindowContent;

public class WelcomeScreen extends WindowContent {
    JLabel welcomeLabel = new JLabel("Willkommen bei der Weltladenkasse!");
    /**
     *    The constructor.
     *       */
    public WelcomeScreen()
    {
	this.add(welcomeLabel, BorderLayout.CENTER);
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
    }
}
