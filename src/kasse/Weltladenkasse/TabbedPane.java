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
    private RechnungenTabbedPane myRech;
    private AbrechnungenTabbedPane myAbrech;
    private Kassenstand myKassenstand;
    private PreisschilderTabbedPane myPreisschild;
    private OptionTabbedPane myOptPane;

    // Methoden:
    public TabbedPane(Connection conn, MainWindow mw) {
	super(conn, mw, null);
    }

    @Override
    protected void createTabbedPane() {
        tabbedPane = new JTabbedPane();

        myKassieren = new Kassieren(this.conn, this.mainWindow, this);
        myRech = new RechnungenTabbedPane(this.conn, this.mainWindow, this);
        myAbrech = new AbrechnungenTabbedPane(this.conn, this.mainWindow, this);
        myKassenstand = new Kassenstand(this.conn, this.mainWindow, this);
        myPreisschild = new PreisschilderTabbedPane(this.conn, this.mainWindow, this);
        myOptPane = new OptionTabbedPane(this.conn, this.mainWindow, this);
        tabbedPane.addTab("Kassieren", null, myKassieren, "Kunden abkassieren");
        tabbedPane.addTab("Rechnungen", null, myRech, "Rechnungen ansehen/stornieren");
        tabbedPane.addTab("Abrechnungen", null, myAbrech, "Tages-/Monats-/Jahresabschluss");
        tabbedPane.addTab("Kassenstand", null, myKassenstand, "Kassenstand ansehen/ändern");
        tabbedPane.addTab("Preisschilder", null, myPreisschild, "Preisschilder drucken");
        tabbedPane.addTab("Optionen", null, myOptPane, "Artikelliste/Rabattaktionen/Import/Export");

        this.add(tabbedPane, BorderLayout.CENTER);
    }

    public boolean isThereIncompleteAbrechnungTag() {
        return myAbrech.isThereIncompleteAbrechnungTag();
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
