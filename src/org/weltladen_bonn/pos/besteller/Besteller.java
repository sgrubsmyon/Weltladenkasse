package org.weltladen_bonn.pos.besteller;

import java.util.Vector;

// GUI stuff:
import java.awt.Dimension;

import javax.swing.*;

// Class holding only the main function
public class Besteller {
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {
        //JFrame.setDefaultLookAndFeelDecorated(true);

        final MainWindow myWindow = new MainWindow();
        if (myWindow.dbconn.passwordReturn == "CANCEL"){
            return;
        }
        if (myWindow.dbconn.passwordReturn == "OK" && myWindow.dbconn.connectionWorks){
            //myWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            myWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // (maybe better)
            myWindow.setTitle("Weltladenbesteller");
            // Specify where it will appear on the screen:
            //	myWindow.setLocation(200, 100);
            //myWindow.setSize(1024, 768);
            //myWindow.setSize(1024, 400);
            myWindow.setPreferredSize(new Dimension(1024, 768));
            myWindow.pack();

            //WelcomeScreen welcome = new WelcomeScreen();
            //myWindow.setContentPanel(welcome);

            // Show it!
            myWindow.setVisible(true);
            System.out.println("Password was correct.");
            return;
        }
    }

    public static void main(String[] args) {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                //UIManager.put("swing.boldMetal", Boolean.FALSE);
                /*
                   try {
                // Set System Look&Feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                   }
                   catch (UnsupportedLookAndFeelException ex) {
                   System.out.println("Exception: " + ex.getMessage());
                   ex.printStackTrace();
                   }
                   catch (ClassNotFoundException ex) {
                   System.out.println("Exception: " + ex.getMessage());
                   ex.printStackTrace();
                   }
                   catch (InstantiationException ex) {
                   System.out.println("Exception: " + ex.getMessage());
                   ex.printStackTrace();
                   }
                   catch (IllegalAccessException ex) {
                   System.out.println("Exception: " + ex.getMessage());
                   ex.printStackTrace();
                   }
                   */
                createAndShowGUI();
	//	try {
	//	    myKasse.conn.close();
	//	} catch (SQLException ex) {
	//	    System.out.println("Exception: " + ex.getMessage());
	//	    ex.printStackTrace();
	//	}
            }
        });
    }
}
