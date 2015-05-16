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

import WeltladenDB.MainWindowGrundlage;
import WeltladenDB.TabbedPaneGrundlage;
import WeltladenDB.ArtikellisteContainer;
import WeltladenDB.Lieferantliste;
import WeltladenDB.Produktgruppenliste;
import WeltladenDB.DumpDatabase;

// Klasse, die Produktgruppenliste und Artikelliste speichert und anzeigt
public class PreisschilderTabbedPane extends TabbedPaneGrundlage {

    // Methoden:
    public PreisschilderTabbedPane(Connection conn, MainWindowGrundlage mw, TabbedPaneGrundlage ptp) {
	super(conn, mw, ptp);
    }

    @Override
    protected void createTabbedPane() {
        tabbedPane = new JTabbedPane();

        PreisschilderFormular myPreisschForm = new PreisschilderFormular(this.conn, this.mainWindow, this);
        PreisschilderListeContainer myPreisschList = new PreisschilderListeContainer(this.conn, this.mainWindow);
        tabbedPane.addTab("Artikel scannen", null, myPreisschForm, "Artikel durch Scannen auswählen");
        tabbedPane.addTab("Artikel auswählen", null, myPreisschList, "Artikel aus Liste auswählen");

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
