package Weltladenkasse;

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

import WeltladenDB.TabbedPaneGrundlage;
import WeltladenDB.ArtikellisteContainer;
import WeltladenDB.DumpDatabase;

// Klasse, die Bestellfenster und Artikelliste speichert und anzeigt
public class TabbedPane extends TabbedPaneGrundlage {
    private Kassieren myKassieren;
    private HeutigeRechnungen myRech;
    private AbrechnungenTag myAbrech;
    private Kassenstand myKassenstand;
    private OptionTabbedPane myOptPane;

    // Methoden:
    public TabbedPane(Connection conn, MainWindow mw) {
	super(conn, mw);
    }

    @Override
    protected void createTabbedPane() {
        tabbedPane = new JTabbedPane();
        myKassieren = new Kassieren(this.conn, this.mainWindow);
        myRech = new HeutigeRechnungen(this.conn, this.mainWindow);
        myAbrech = new AbrechnungenTag(this.conn, this.mainWindow);
        myKassenstand = new Kassenstand(this.conn, this.mainWindow);
        myOptPane = new OptionTabbedPane(this.conn, this.mainWindow);
        tabbedPane.addTab("Kassieren", null, myKassenstand, "Kunden abkassieren");
        tabbedPane.addTab("Rechnungen", null, myRech, "Rechnungen ansehen/stornieren");
        tabbedPane.addTab("Abrechnungen", null, myAbrech, "Tages-/Monats-/Jahresabschluss");
        tabbedPane.addTab("Kassenstand", null, myKassenstand, "Kassenstand ansehen/ändern");
        tabbedPane.addTab("Optionen", null, myOptPane, "Artikelliste/Rabattaktionen/Import/Export");

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
