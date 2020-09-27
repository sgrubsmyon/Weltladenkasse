package org.weltladen_bonn.pos.kasse;

import java.util.Vector;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

// GUI stuff:
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TSE: (BSI, use implementation by Bundesdruckerei/D-Trust/cryptovision)
import com.cryptovision.SEAPI.TSE;
import com.cryptovision.SEAPI.exceptions.SEException;

// Class holding only the main function
public class Kasse {
    private static Logger logger = null;
    private static TSE tse = null;

    /**
     * Check if there is a TSE and if it needs to be initialized.
     */
    private static void checkTSE(MainWindow myWindow) {
        // Configure TSE:
        try {
            try {
                tse = TSE.getInstance("config_tse.txt");
            } catch (FileNotFoundException ex) {
                logger.warn("TSE config file not found under '{}'", "config_tse.txt");
                logger.warn("Exception: {}", ex);
                JOptionPane.showMessageDialog(myWindow,
                    "ACHTUNG: Es wird ohne TSE gearbeitet (weil keine Datei 'config_tse.txt' vorhanden ist)!!!\n"+
                    "Dies ist im Geschäftsbetrieb ILLEGAL und darf also nur für Testzwecke sein!!!\n"+
                    "Wurde aus Versehen der Testmodus gewählt?",
                    "Wirklich ohne TSE kassieren?", JOptionPane.WARNING_MESSAGE);
                logger.info("TSE object tse = {}", tse);
            } catch (IOException ex) {
                logger.fatal("There is a TSE config file '{}', but it could not be read from it.", "config_tse.txt");
                logger.fatal("Exception: {}", ex);
                JOptionPane.showMessageDialog(myWindow,
                    "ACHTUNG: Die Datei 'config_tse.txt' konnte nicht eingelesen werden!\n"+
                    "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                    "wird die Kassensoftware jetzt beendet. Bitte Fehler in der Datei beheben und\n"+
                    "erneut versuchen.",
                    "Konfiguration der TSE nicht lesbar", JOptionPane.ERROR_MESSAGE);
                // Exit application upon this fatal error, because a TSE config file is present
                // (so it seems usage of TSE is desired), but it could not be read from it.
                System.exit(1);
            }
            boolean[] pin_status = tse.getPinStatus();
            boolean transport_state = pin_status[0];
            logger.info(transport_state);
            if (transport_state) {
                JOptionPane.showMessageDialog(myWindow,
                    "ACHTUNG: Eine noch nicht initialisierte TSE wurde gefunden.\n"+
                    "Dies kommt vor, wenn eine neue TSE zum ersten mal eingesetzt wird.\n"+
                    "Es können jetzt die PINs und PUKs gesetzt werden, um die TSE zu initialisieren.\n"+
                    "Danach ist die TSE 'in Benutzung'.",
                    "Uninitialisierte TSE gefunden", JOptionPane.INFORMATION_MESSAGE);
                initializeTSE(myWindow);
                // Re-check if TSE was actually initialized:
                if (false) {
                    logger.fatal("TSE initialization failed!");
                    JOptionPane.showMessageDialog(myWindow,
                        "ACHTUNG: Die Initialisierung der TSE ist fehlgeschlagen!\n"+
                        "Ohne Initialisierung kann eine neue TSE nicht verwendet werden.\n"+
                        "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                        "Bitte beim nächsten Start der Kassensoftware erneut probieren.",
                        "Fehler bei der Initialisierung der TSE", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
        } catch (SEException ex) {
            logger.fatal("Unable to open connection to TSE, given configuration provided by '{}'.", "config.txt");
            logger.fatal("Exception: {}", ex);
            JOptionPane.showMessageDialog(myWindow,
                "ACHTUNG: Es konnte keine Verbindung zur TSE aufgebaut werden!\n"+
                "Entweder die TSE (eine SD-Karte, die rechts am Laptop in einem Schlitz steckt)\n"+
                "sitzt nicht richtig drin oder die Konfiguration in der Datei 'config_tse.txt'\n"+
                "ist falsch.\n"+
                "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                "Bitte Fehler beheben und erneut versuchen.",
                "Verbindung zur TSE nicht möglich", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error, because a TSE config file is present
            // (so it seems usage of TSE is desired), but maybe configuration is wrong or
            // TSE is not plugged in and no connection to TSE could be made.
            System.exit(1);
        }
        return;
    }

    private static void initializeTSE(MainWindow myWindow) {
        // JOptionPane jop = new JOptionPane(new Object[]{"TSE PIN/PUK Test"},
        //     JOptionPane.QUESTION_MESSAGE,
        //     JOptionPane.OK_CANCEL_OPTION);
        // JDialog dialog = jop.createDialog("Bitte PINs und PUKs eingeben");
        // // dialog.addWindowFocusListener(new WindowAdapter(){
        // //     @Override
        // //     public void windowGainedFocus(WindowEvent e){
        // //         adminpinfield.requestFocusInWindow();
        // //     }
        // // });
        // dialog.setVisible(true);
        // dialog.dispose();
        JDialog dialog = new JDialog(myWindow, "Bitte PINs und PUKs der TSE eingeben", true);
        TSEInitDialog tseid = new TSEInitDialog(myWindow, dialog);
        dialog.getContentPane().add(tseid, BorderLayout.CENTER);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setVisible(true);
    }

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
            checkTSE(myWindow);

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
                    } else if ( myWindow.myTabbedPane.kassenstandNeedsToChange ){
                        int answer = JOptionPane.showConfirmDialog(myWindow,
                            "Es muss noch der Kassenstand geändert werden (unter 'Kassenstand').\n"+
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
            // myWindow.setPreferredSize(new Dimension(1024, 768));
            myWindow.setPreferredSize(new Dimension(1200, 675));
            myWindow.pack();

            //WelcomeScreen welcome = new WelcomeScreen();
            //myWindow.setContentPanel(welcome);

            // Show it!
            myWindow.setVisible(true);
            logger.info("Password was correct.");

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
        // Configure logger:
        System.setProperty("log4j.configurationFile", "config_log4j2.xml");
        logger = LogManager.getLogger(Kasse.class);

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
