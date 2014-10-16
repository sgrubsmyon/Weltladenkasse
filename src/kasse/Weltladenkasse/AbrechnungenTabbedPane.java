package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.math.RoundingMode;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// GUI stuff:
//import java.awt.BorderLayout;
//import java.awt.FlowLayout;
//import java.awt.Dimension;
import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
import java.awt.event.*;

//import javax.swing.JFrame;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTable;
//import javax.swing.JTextArea;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;
import javax.swing.table.*;

import WeltladenDB.MainWindowGrundlage;
import WeltladenDB.TabbedPaneGrundlage;

public class AbrechnungenTabbedPane extends TabbedPaneGrundlage {
    private AbrechnungenTag myTag;
    private AbrechnungenMonat myMonat;
    private AbrechnungenJahr myJahr;

    // Methoden:
    /**
     *    The constructor.
     *       */
    public AbrechnungenTabbedPane(Connection conn, MainWindowGrundlage mw) {
	super(conn, mw);
    }

    @Override
    protected void createTabbedPane() {
        tabbedPane = new JTabbedPane();

        myTag = new AbrechnungenTag(this.conn, this.mainWindow, this);
        myMonat = new AbrechnungenMonat(this.conn, this.mainWindow);
        myJahr = new AbrechnungenJahr(this.conn, this.mainWindow);
        tabbedPane.addTab("Tag", null, myTag, "Tagesabschluss");
        tabbedPane.addTab("Monat", null, myMonat, "Monatsabschluss");
        tabbedPane.addTab("Jahr", null, myJahr, "Jahresabschluss");

        this.add(tabbedPane, BorderLayout.CENTER);
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