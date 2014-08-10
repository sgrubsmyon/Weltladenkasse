package Weltladenbesteller;

// Basic Java stuff:
import java.util.*; // for Vector

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
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
import javax.swing.tree.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.*;

import WeltladenDB.ArtikellisteContainer;
import WeltladenDB.WindowContent;

// Klasse, die Bestellfenster und Artikelliste speichert und anzeigt
public class TabbedPane extends WindowContent {
    private JTabbedPane tabbedPane;
    private BestellAnzeige bestellAnzeige;

    // Methoden:
    public TabbedPane(Connection conn, MainWindow mw) {
	super(conn, mw);

        createTabbedPane();
    }

    void createTabbedPane() {
        tabbedPane = new JTabbedPane();
        Bestellen myBestellen = new Bestellen(this.conn, this.mainWindow, this);
        ArtikellisteContainer myArtikellisteC = new ArtikellisteContainer(this.conn, this.mainWindow);
        bestellAnzeige = new BestellAnzeige(this.conn, this.mainWindow);
        tabbedPane.addTab("Bestellen", null, myBestellen, "Bestellung erstellen");
        tabbedPane.addTab("Artikelliste", null, myArtikellisteC, "Artikel bearbeiten/hinzufügen");
        tabbedPane.addTab("Bestellungen", null, bestellAnzeige, "Bestellung anzeigen/drucken");

        this.add(tabbedPane, BorderLayout.CENTER);
    }

    public void recreateTabbedPane() {
        this.remove(tabbedPane);
	this.revalidate();
        createTabbedPane();
    }

    public void switchToBestellAnzeige(int bestellNr) {
        int tabIndex = tabbedPane.indexOfTab("Bestellungen");
        tabbedPane.setSelectedIndex(tabIndex);
        int rowIndex = bestellAnzeige.bestellNummern.indexOf(bestellNr);
        bestellAnzeige.orderTable.setRowSelectionInterval(rowIndex, rowIndex);
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
