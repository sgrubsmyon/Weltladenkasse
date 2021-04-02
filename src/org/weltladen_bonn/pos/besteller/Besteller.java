package org.weltladen_bonn.pos.besteller;

import java.util.Vector;

// GUI stuff:
import java.awt.Dimension;

import javax.swing.*;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.weltladen_bonn.pos.SplashScreen;

// Class holding only the main function
public class Besteller {
    private static Logger logger = null;

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {
        //JFrame.setDefaultLookAndFeelDecorated(true);

        ImageIcon myImage = new ImageIcon(Besteller.class.getResource("/resources/images/splash_besteller.jpg"));
        final SplashScreen splash = new SplashScreen(myImage, "Bitte warten, Besteller lädt...");
        int nTasks = 5;

        final MainWindow myWindow = new MainWindow(splash, nTasks);
        if (myWindow.dbconn.passwordReturn == "CANCEL"){
            splash.dispose();
            myWindow.dispose();
            return;
        }
        if (myWindow.dbconn.passwordReturn == "OK" && myWindow.dbconn.connectionWorks){
            splash.setStatusLabel("Stelle GUI-Fenster dar...");
            splash.setProgress(5 * 100 / nTasks);

            //myWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            myWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // (maybe better)

            myWindow.setTitle("Weltladenbesteller");
            // myWindow.setPreferredSize(new Dimension(1024, 768));
            myWindow.setPreferredSize(new Dimension(1200, 740));
            myWindow.pack();
            // Specify where it will appear on the screen:
            //myWindow.setLocation(200, 100);
            //myWindow.setSize(1024, 768);
            //myWindow.setSize(1024, 400);
            myWindow.setLocationRelativeTo(null); // this centers the window if called after "pack()"

            // Remove splash screen
            splash.dispose();

            // Show the GUI!
            myWindow.setVisible(true);
            logger.info("Password was correct.");
            return;
        }
    }

    public static void main(String[] args) {
        // Configure logger:
        System.setProperty("log4j.configurationFile", "config_log4j2.xml");
        logger = LogManager.getLogger(Besteller.class);

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

                logger.info("Hello from Log4j2");
                createAndShowGUI();
            }
        });
    }
}
