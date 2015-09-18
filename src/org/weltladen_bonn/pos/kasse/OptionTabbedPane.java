package org.weltladen_bonn.pos.kasse;

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

import org.weltladen_bonn.pos.*;

// Klasse, die Produktgruppenliste und Artikelliste speichert und anzeigt
public class OptionTabbedPane extends TabbedPaneGrundlage {
    private ArtikellisteContainer myArtikellisteC;

    // Methoden:
    public OptionTabbedPane(Connection conn, MainWindowGrundlage mw, TabbedPaneGrundlage ptp) {
	super(conn, mw, ptp);
    }

    @Override
    protected void createTabbedPane() {
        tabbedPane = new JTabbedPane();

        myArtikellisteC = new ArtikellisteContainer(this.conn, this.mainWindow);
        Rabattaktionen myRabattaktionen = new Rabattaktionen(this.conn, this.mainWindow);
        Lieferantliste myLieferant = new Lieferantliste(this.conn, this.mainWindow);
        Produktgruppenliste myProduktgruppe = new Produktgruppenliste(this.conn, this.mainWindow);
        DumpDatabase myDump = new DumpDatabase(this.conn, this.mainWindow, this);
        tabbedPane.addTab("Artikelliste", null, myArtikellisteC, "Artikel bearbeiten/hinzufügen");
        tabbedPane.addTab("Rabatt", null, myRabattaktionen, "Rabattaktionen bearbeiten/hinzufügen");
        tabbedPane.addTab("Lieferanten", null, myLieferant, "Lieferanten bearbeiten/hinzufügen");
        tabbedPane.addTab("Produktgruppen", null, myProduktgruppe, "Produktgruppen bearbeiten/hinzufügen");
        tabbedPane.addTab("DB Import/Export", null, myDump, "Datenbank exportieren/importieren");

        this.add(tabbedPane, BorderLayout.CENTER);
    }

    public Artikelliste getArtikelliste() {
        return myArtikellisteC.getArtikelliste();
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
