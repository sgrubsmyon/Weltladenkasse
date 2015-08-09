package Weltladenkasse;

import java.util.Vector;

// GUI stuff:
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

// Class holding only the main function
public class Kasse {
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
            //myWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // (maybe better)
            myWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // (first check if Tagesabrechnung has been done:)
            myWindow.addWindowListener(new WindowAdapter(){
                @Override
                public void windowClosing(WindowEvent we) {
                    if ( myWindow.isThereIncompleteAbrechnungTag() ){
                        int answer = JOptionPane.showConfirmDialog(myWindow,
                            "Es wurde heute noch keine Tagesabrechnung (unter 'Abrechnungen') gemacht.\n"+
                            "Wirklich schließen?", "Warnung",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (answer == JOptionPane.YES_OPTION){
                            myWindow.dispose();
                        } else {
                            // do nothing
                        }
                    } else {
                        myWindow.dispose();
                    }
                }
            });

            myWindow.setTitle("Weltladenkasse");
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

            // Check if there is an incomplete Tagesabrechnung from the start!
            if ( myWindow.isThereIncompleteAbrechnungTag() ){
                JOptionPane.showMessageDialog(myWindow,
                        "Hinweis: Es gibt eine offene Tagesabrechnung.\n"+
                        "Wurde vergessen, die Tagesabrechnung zu machen (unter 'Abrechnungen')?",
                        "Ausstehende Abrechnung", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
    }

    /**
     * @param args input strings value.
     */
    public static void main(String[] args) {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                //UIManager.put("swing.boldMetal", Boolean.FALSE);
                /*
                // Set System Look&Feel
                try {
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
