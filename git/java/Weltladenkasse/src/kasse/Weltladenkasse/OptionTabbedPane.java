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

import WeltladenDB.WindowContent;
import WeltladenDB.ArtikellisteContainer;

// Klasse, die Produktgruppenliste und Artikelliste speichert und anzeigt
public class OptionTabbedPane extends WindowContent {
    private JTabbedPane tabbedPane;
    
    // Methoden:
    public OptionTabbedPane(Connection conn, MainWindow mw) {
	super(conn, mw);

        tabbedPane = new JTabbedPane();
        ArtikellisteContainer myArtikellisteC = new ArtikellisteContainer(this.conn, mw);
        Rabattaktionen myRabattaktionen = new Rabattaktionen(this.conn, mw);
        tabbedPane.addTab("Artikelliste", null, myArtikellisteC, "Artikel bearbeiten/hinzufügen");
        tabbedPane.addTab("Rabatt", null, myRabattaktionen, "Rabattaktionen bearbeiten/hinzufügen");

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
