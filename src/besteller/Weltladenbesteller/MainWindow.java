package Weltladenbesteller;

// Basic Java stuff:
import java.util.*; // for Vector

// MySQL Connector/J stuff:
import java.sql.*;

// GUI stuff:
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import WeltladenDB.MainWindowGrundlage;

// Class holding the window of the application GUI:
public class MainWindow extends MainWindowGrundlage implements ActionListener {
    //***************************************************
    // Members
    //***************************************************
    TabbedPane myTabbedPane;

    //***************************************************
    // Methods
    //***************************************************

    /**
     *    The constructor.
     *       */
    public MainWindow(){
        super();
        if (dbconn.connectionWorks){
            myTabbedPane = new TabbedPane(this.conn, this);
            setContentPanel(myTabbedPane);
        }
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e){
    }
}
