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

import WeltladenDB.TabbedPaneGrundlage;
import WeltladenDB.ArtikellisteContainer;
import WeltladenDB.Lieferantliste;
import WeltladenDB.Produktgruppenliste;
import WeltladenDB.DumpDatabase;

// Klasse, die Bestellfenster und Artikelliste speichert und anzeigt
public class TabbedPane extends TabbedPaneGrundlage {
    private BestellAnzeige bestellAnzeige;
    private Bestellen myBestellen;

    // Methoden:
    public TabbedPane(Connection conn, MainWindow mw) {
	super(conn, mw, null);
    }

    @Override
    protected void createTabbedPane() {
        tabbedPane = new JTabbedPane();
        myBestellen = new Bestellen(this.conn, this.mainWindow, this);
        ArtikellisteContainer myArtikellisteC = new ArtikellisteContainer(this.conn, this.mainWindow);
        bestellAnzeige = new BestellAnzeige(this.conn, this.mainWindow, this);
        Lieferantliste myLieferant = new Lieferantliste(this.conn, this.mainWindow);
        Produktgruppenliste myProduktgruppe = new Produktgruppenliste(this.conn, this.mainWindow);
        DumpDatabase myDump = new DumpDatabase(this.conn, this.mainWindow, this);
        tabbedPane.addTab("Bestellen", null, myBestellen, "Bestellung erstellen");
        tabbedPane.addTab("Artikelliste", null, myArtikellisteC, "Artikel bearbeiten/hinzufügen");
        tabbedPane.addTab("Bestellungen", null, bestellAnzeige, "Bestellung anzeigen/drucken");
        tabbedPane.addTab("Lieferanten", null, myLieferant, "Lieferanten bearbeiten/hinzufügen");
        tabbedPane.addTab("Produktgruppen", null, myProduktgruppe, "Produktgruppen bearbeiten/hinzufügen");
        tabbedPane.addTab("DB Import/Export", null, myDump, "Datenbank exportieren/importieren");

        this.add(tabbedPane, BorderLayout.CENTER);
    }

    public void switchToBestellAnzeige(Vector<Object> bestellNrUndTyp) {
        int tabIndex = tabbedPane.indexOfTab("Bestellungen");
        tabbedPane.setSelectedIndex(tabIndex);
        int rowIndex = bestellAnzeige.bestellNummernUndTyp.indexOf(bestellNrUndTyp);
        bestellAnzeige.orderTable.setRowSelectionInterval(rowIndex, rowIndex);
    }

    public void switchToBestellen() {
        int tabIndex = tabbedPane.indexOfTab("Bestellen");
        tabbedPane.setSelectedIndex(tabIndex);
    }

    public boolean bestellenTableIsEmpty() {
        return myBestellen.numberOfRows() == 0;
    }

    public void setBestellenTable(Vector<Object> bestellNrUndTyp, int jahr, int kw,
            Vector<Integer> artikelIDs, Vector<Boolean> sortimentBools, Vector< Vector<Object> > data) {
        myBestellen.emptyTable();
        myBestellen.selBestellNr = (Integer)bestellNrUndTyp.get(0);
        myBestellen.selTyp = (String)bestellNrUndTyp.get(1);
        myBestellen.selJahr = jahr;
        myBestellen.selKW = kw;
        for (int i=artikelIDs.size()-1; i>=0; i--){
            String lieferant = data.get(i).get(1).toString();
            String artikelNr = data.get(i).get(2).toString();
            String artikelName = data.get(i).get(3).toString();
            String vkp = data.get(i).get(4).toString();
            String vpe = data.get(i).get(5) == null ? null : data.get(i).get(5).toString();
            Integer stueck = Integer.parseInt( data.get(i).get(6).toString() );
            myBestellen.hinzufuegen(artikelIDs.get(i), lieferant, artikelNr,
                    artikelName, vkp, vpe, stueck, sortimentBools.get(i));
        }
        myBestellen.updateAll();
        myBestellen.doCSVBackup();
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
