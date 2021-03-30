package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector

// MySQL Connector/J stuff:
import java.sql.*;
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.*; // JFrame, JPanel, JButton, JLabel, ...
import javax.swing.Timer; // ambiguity with java.util.Timer

import org.weltladen_bonn.pos.MainWindowGrundlage;
import org.weltladen_bonn.pos.BaseClass;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.weltladen_bonn.pos.SplashScreen;

// Class holding the window of the application GUI:
public class MainWindow extends MainWindowGrundlage implements ActionListener {
    //***************************************************
    // Members
    //***************************************************
    private static final Logger logger = LogManager.getLogger(MainWindow.class);

    TabbedPane myTabbedPane;

    // class to talk to Kundendisplay
    private Kundendisplay display;

    // class to talk to TSE
    private WeltladenTSE tse;

    private JButton beendenButton = new JButton("Beenden");

    //***************************************************
    // Methods
    //***************************************************

    /**
     *    The constructor.
     *       */
    public MainWindow(SplashScreen spl, int nTasks) {
        super(spl, nTasks);

        // Initiate TSE (after connecting to DB)
        // This can take up to 30-60 seconds of time, so do this while showing the user a splash screen to inform
        splash.setStatusLabel("Stelle Verbindung zur TSE her (das kann 30 bis 60 Sekunden dauern)...");
        splash.setProgress(4 * 100 / nTasks);
        tse = new WeltladenTSE(this.pool, this, splash);

        splash.setStatusLabel("Stelle Verbindung zum Kundendisplay her...");
        splash.setProgress(5 * 100 / nTasks);
        display = new Kundendisplay(bc);
        setDisplayBlankTimer();

        if (dbconn.connectionWorks) {
            splash.setStatusLabel("Lade die Tabs...");
            splash.setProgress(6 * 100 / nTasks);
            myTabbedPane = new TabbedPane(this.pool, this);
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            if (isInTrainingMode()) {
                JPanel trainingPanel = new JPanel(new BorderLayout());
                // center
                JPanel centerPanel = new JPanel();
                JButton trainingBadge = new BaseClass.BigButton("TRAININGSMODUS");
                trainingBadge.setBackground(Color.RED);
                trainingBadge.setForeground(Color.WHITE.brighter().brighter().brighter());
                trainingBadge.setEnabled(false);
                centerPanel.add(trainingBadge);
                trainingPanel.add(centerPanel, BorderLayout.CENTER);
                contentPanel.add(trainingPanel);
            }
            contentPanel.add(myTabbedPane);
            setContentPanel(contentPanel);
        }
        //topPanel.setLayout(new FlowLayout());
        //beendenButton.addActionListener(this);
        //topPanel.add(beendenButton);
        //holdAll.add(topPanel, BorderLayout.NORTH);
    }

    public void setDisplayWelcomeTimer() {
        if (display != null && display.deviceWorks()){
            ActionListener displayResetter = new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if ( !myTabbedPane.esWirdKassiert && display != null && display.deviceWorks()){
                        display.showWelcomeScreen();
                    }
                }
            };
            Timer t1 = new Timer(bc.displayShowWelcomeInterval, displayResetter);
            t1.setRepeats(false);
            t1.start();
        }
    }

    public void setDisplayBlankTimer() {
        if (display != null && display.deviceWorks()){
            ActionListener displayBlanker = new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if ( !myTabbedPane.esWirdKassiert && display != null && display.deviceWorks()){
                        display.clearScreen();
                    }
                }
            };
            Timer t2 = new Timer(bc.displayBlankInterval, displayBlanker);
            t2.setRepeats(false);
            t2.start();
        }
    }

    @Override
    public void dispose() {
        // Do clean-up:
        if (display != null) {
            logger.debug("Closing connection to customer display");
            display.closeDevice();
        }
        logger.debug("Closing connection to TSE");
        tse.disconnectFromTSE();
        super.dispose();
    }


    public Kundendisplay getDisplay() {
        return display;
    }

    public WeltladenTSE getTSE() {
        return tse;
    }


    public boolean isThereIncompleteAbrechnungTag() {
        return myTabbedPane.isThereIncompleteAbrechnungTag();
    }

    public boolean isInTrainingMode() {
        return !bc.operationMode.equals("normal");
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
	//if (e.getSource() == beendenButton){
	//    int answer = JOptionPane.showConfirmDialog(this,
	//	    "Programm beenden?", "Beenden",
	//	    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
	//    if (answer == JOptionPane.YES_OPTION)
        //        System.exit(0);
	//    return;
	//}
    }
}
