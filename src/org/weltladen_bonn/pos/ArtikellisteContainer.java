package org.weltladen_bonn.pos;

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

// Klasse, die Produktgruppenliste und Artikelliste speichert und anzeigt
public class ArtikellisteContainer extends WindowContent {
    // Attribute:
    protected ProduktgruppenbaumArtikelliste prodList;
    protected TreeSet<Integer> sortedExpandedRows; // saves (and thus enables to restore) the expansion state of JTree
    private Artikelliste artList;

    // Methoden:
    public ArtikellisteContainer(Connection conn, MainWindowGrundlage mw) {
	super(conn, mw);

        prodList = new ProduktgruppenbaumArtikelliste(conn, mw, this);

        this.add(prodList, BorderLayout.CENTER);
    }

    public Artikelliste getArtikelliste() {
        return artList;
    }

    public void switchToArtikelliste(Integer topid, Integer subid, Integer subsubid, String gruppenname) {
        JTree tree = prodList.getTree();
        Enumeration treeCache = tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
        sortedExpandedRows = new TreeSet<Integer>();
        while (treeCache.hasMoreElements()){
            TreePath treePath = (TreePath)treeCache.nextElement();
            sortedExpandedRows.add( tree.getRowForPath(treePath) );
        }
        this.remove(prodList);
        this.revalidate();
        artList = new Artikelliste(this.conn, this, topid, subid, subsubid, gruppenname);
        this.add(artList, BorderLayout.CENTER);
    }

    public void switchToProduktgruppenliste() {
        this.remove(artList);
        this.revalidate();
        prodList = new ProduktgruppenbaumArtikelliste(this.conn, this.mainWindow, this);
        JTree tree = prodList.getTree();
        for (Integer i : sortedExpandedRows){
            tree.expandRow(i);
        }
        this.add(prodList, BorderLayout.CENTER);
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
