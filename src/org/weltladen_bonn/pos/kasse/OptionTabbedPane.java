package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import org.mariadb.jdbc.MariaDbPoolDataSource;

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
    public OptionTabbedPane(MariaDbPoolDataSource pool, MainWindowGrundlage mw, TabbedPaneGrundlage ptp) {
	    super(pool, mw, ptp);
    }

    @Override
    protected void createTabbedPane() {
        tabbedPane = new JTabbedPane();

        myArtikellisteC = new ArtikellisteContainer(this.pool, this.mainWindow);
        Rabattaktionen myRabattaktionen = new Rabattaktionen(this.pool, this.mainWindow, this);
        Lieferantliste myLieferant = new Lieferantliste(this.pool, this.mainWindow);
        Produktgruppenliste myProduktgruppe = new Produktgruppenliste(this.pool, this.mainWindow);
        DumpDatabase myDump = new DumpDatabase(this.pool, this.mainWindow, this);
        TSEStatus myTSE = new TSEStatus(this.pool, (MainWindow)this.mainWindow, this);
        tabbedPane.addTab("Artikelliste", null, myArtikellisteC, "Artikel bearbeiten/hinzufügen");
        tabbedPane.addTab("Rabatt", null, myRabattaktionen, "Rabattaktionen bearbeiten/hinzufügen");
        tabbedPane.addTab("Lieferanten", null, myLieferant, "Lieferanten bearbeiten/hinzufügen");
        tabbedPane.addTab("Produktgruppen", null, myProduktgruppe, "Produktgruppen bearbeiten/hinzufügen");
        tabbedPane.addTab("DB Import/Export", null, myDump, "Datenbank exportieren/importieren");
        tabbedPane.addTab("TSE", null, myTSE, "Status der TSE abfragen und TSE-Daten exportieren");

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
