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
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.*;
import javax.swing.event.*;

import org.weltladen_bonn.pos.*;

// Klasse, die Bestellfenster und Artikelliste speichert und anzeigt
public class TabbedPane extends TabbedPaneGrundlage implements ChangeListener {
    private Kassieren myKassieren;
    private RechnungenTabbedPane myRech;
    private AbrechnungenTabbedPane myAbrech;
    private Kassenstand myKassenstand;
    private PreisschilderFormular myPreisschild;
    private OptionTabbedPane myOptPane;
    public Boolean esWirdKassiert = false; /** This needs to live in TabbedPane (which does not get recreated)
                                             and cannot be local to Kassieren, even though it is only used
                                             inside Kassieren. */
    public Boolean kassenstandNeedsToChange = false;

    // Methoden:
    public TabbedPane(MariaDbPoolDataSource pool, MainWindow mw) {
	    super(pool, mw, null);
    }

    @Override
    protected void createTabbedPane() {
        tabbedPane = new JTabbedPane();

        myKassieren = new Kassieren(this.pool, this.mainWindow, this);
        myRech = new RechnungenTabbedPane(this.pool, this.mainWindow, this);
        myAbrech = new AbrechnungenTabbedPane(this.pool, this.mainWindow, this);
        myKassenstand = new Kassenstand(this.pool, this.mainWindow, this);
        myPreisschild = new PreisschilderFormular(this.pool, this.mainWindow, this);
        myOptPane = new OptionTabbedPane(this.pool, this.mainWindow, this);
        tabbedPane.addTab("Kassieren", null, myKassieren, "Kunden abkassieren");
        tabbedPane.addTab("Rechnungen", null, myRech, "Rechnungen ansehen/stornieren");
        tabbedPane.addTab("Abrechnungen", null, myAbrech, "Tages-/Monats-/Jahresabschluss");
        tabbedPane.addTab("Kassenstand", null, myKassenstand, "Kassenstand ansehen/ändern");
        tabbedPane.addTab("Preisschilder", null, myPreisschild, "Preisschilder drucken");
        tabbedPane.addTab("Optionen", null, myOptPane, "Artikelliste/Rabattaktionen/Import/Export");
        tabbedPane.addChangeListener(this);

        this.add(tabbedPane, BorderLayout.CENTER);

        myKassieren.asPanel.emptyArtikelBox();
    }

    public Artikelliste getArtikelliste() {
        return myOptPane.getArtikelliste();
    }

    public boolean isThereIncompleteAbrechnungTag() {
        return myAbrech.isThereIncompleteAbrechnungTag();
    }

    /**
     *    * Each non abstract class that implements the ChangeListener
     *      must have this method.
     *
     *    @param e the change event.
     **/
    public void stateChanged(ChangeEvent e){
        if (kassenstandNeedsToChange && tabbedPane.getSelectedIndex() != 3) {
            JOptionPane.showMessageDialog(this, "Es muss noch der Kassenstand auf 150 € geändert werden\n(unter 'Kassenstand')!",
                "Fehler", JOptionPane.WARNING_MESSAGE);
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
