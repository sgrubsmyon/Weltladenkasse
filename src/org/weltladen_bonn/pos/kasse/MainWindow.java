package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector

// MySQL Connector/J stuff:
import java.sql.*;
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.*; // JFrame, JPanel, JButton, JLabel, ...
import javax.swing.Timer; // ambiguity with java.util.Timer

import org.weltladen_bonn.pos.MainWindowGrundlage;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    public MainWindow() {
        super();

        display = new Kundendisplay(bc);
        setDisplayBlankTimer();

        if (dbconn.connectionWorks) {
            myTabbedPane = new TabbedPane(this.pool, this);
            setContentPanel(myTabbedPane);
        }
        //topPanel.setLayout(new FlowLayout());
        //beendenButton.addActionListener(this);
        //topPanel.add(beendenButton);
        //holdAll.add(topPanel, BorderLayout.NORTH);
    }

    @Override
    protected void beforeInitiate() {
        tse = new WeltladenTSE(this, this.bc, this.pool);
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
        if (tse.inUse()) {
            logger.debug("Closing connection to TSE");
            tse.disconnectFromTSE();
        }
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
