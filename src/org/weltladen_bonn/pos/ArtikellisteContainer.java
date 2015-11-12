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
    boolean artListShown = false;

    private JPanel prodListPanel;
    private JTextField searchField;
    private JButton emptySearchButton;
    private JButton searchButton;

    private JButton saveButton;
    private JButton revertButton;
    private JButton editButton;
    private JButton newButton;
    private JButton importButton;
    private JButton exportButton;

    // Methoden:
    public ArtikellisteContainer(Connection conn, MainWindowGrundlage mw) {
	super(conn, mw);

        showProdListPanel();

        showBottomPanel();
        enableButtons();

        artList = new Artikelliste(this.conn, this);
    }

    private void showProdListPanel() {
        prodListPanel = new JPanel(new BorderLayout());
        showTopPanel();
        prodList = new ProduktgruppenbaumArtikelliste(this.conn, this.mainWindow, this);
        prodListPanel.add(prodList, BorderLayout.CENTER);
        this.add(prodListPanel, BorderLayout.CENTER);
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
        this.remove(prodListPanel);
        this.revalidate();
        artList = new Artikelliste(this.conn, this, topid, subid, subsubid, gruppenname);
        this.add(artList, BorderLayout.CENTER);
        artListShown = true;
    }

    public void switchToProduktgruppenliste() {
        this.remove(artList);
        this.revalidate();
        artList = new Artikelliste(this.conn, this);
        showProdListPanel();
        JTree tree = prodList.getTree();
        for (Integer i : sortedExpandedRows){
            tree.expandRow(i);
        }
        artListShown = false;
    }

    private void showTopPanel() {
        JPanel topPanel = new JPanel();
          JLabel searchLabel = new JLabel("Suche:");
          topPanel.add(searchLabel);
          searchField = new JTextField("");
          searchField.setColumns(20);
          topPanel.add(searchField);
          searchLabel.setLabelFor(searchField);
          emptySearchButton = new JButton("x");
          emptySearchButton.addActionListener(this);
          topPanel.add(emptySearchButton);
          searchButton = new JButton("Los!");
          searchButton.addActionListener(this);
          topPanel.add(searchButton);
        prodListPanel.add(topPanel, BorderLayout.NORTH);
    }

    private void showBottomPanel() {
        JPanel bottomPanel = new JPanel();
	bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
          JPanel bottomLeftPanel = new JPanel();
          bottomLeftPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
            saveButton = new JButton("Änderungen speichern");
            saveButton.addActionListener(this);
            bottomLeftPanel.add(saveButton);

            revertButton = new JButton("Änderungen verwerfen");
            revertButton.addActionListener(this);
            bottomLeftPanel.add(revertButton);

            editButton = new JButton("Markierte Artikel bearbeiten");
            editButton.setMnemonic(KeyEvent.VK_B);
            editButton.addActionListener(this);
            bottomLeftPanel.add(editButton);
        bottomPanel.add(bottomLeftPanel);

          JPanel bottomRightPanel = new JPanel();
          bottomRightPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
            newButton = new JButton("Neue Artikel eingeben");
            newButton.setMnemonic(KeyEvent.VK_N);
            newButton.addActionListener(this);
            bottomRightPanel.add(newButton);

            importButton = new JButton("Artikel importieren");
            importButton.setMnemonic(KeyEvent.VK_D);
            importButton.addActionListener(this);
            bottomRightPanel.add(importButton);

            exportButton = new JButton("Artikel exportieren");
            exportButton.setMnemonic(KeyEvent.VK_B);
            exportButton.addActionListener(this);
            bottomRightPanel.add(exportButton);
        bottomPanel.add(bottomRightPanel);
        this.add(bottomPanel, BorderLayout.SOUTH);
    }


    protected void enableButtons() {
        if (artListShown) {
            saveButton.setEnabled(artList.editedArticles.size() > 0);
            revertButton.setEnabled(artList.editedArticles.size() > 0);
            editButton.setEnabled(artList.myTable.getSelectedRowCount() > 0);
            newButton.setEnabled(artList.editedArticles.size() == 0);
            importButton.setEnabled(artList.editedArticles.size() == 0);
            exportButton.setEnabled(artList.editedArticles.size() == 0);
        } else {
            saveButton.setEnabled(false);
            revertButton.setEnabled(false);
            editButton.setEnabled(false);
            newButton.setEnabled(true);
            importButton.setEnabled(true);
            exportButton.setEnabled(true);
        }
    }


    protected void updateTree() {
        prodList.updateTree();
    }


    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e){
        if (e.getSource() == emptySearchButton){
            searchField.setText("");
            searchField.requestFocus();
	    return;
	}
        if (e.getSource() == saveButton){
            artList.putChangesIntoDB();
            artList.updateAll();
            return;
        }
        if (e.getSource() == revertButton){
            artList.updateAll();
            return;
        }
        if (e.getSource() == editButton){
            artList.showEditDialog();
            return;
        }
        if (e.getSource() == newButton){
            artList.showNewItemDialog();
            return;
        }
        if (e.getSource() == importButton){
            artList.showReadFromFileDialog();
            return;
        }
        if (e.getSource() == exportButton){
            if (!artListShown) {
                // in order to import *all* articles:
                artList = new Artikelliste(this.conn, this, null, null, null, "Alle Artikel");
            }
            artList.showExportDialog();
            if (!artListShown) {
                artList = new Artikelliste(this.conn, this);
            }
            return;
        }
    }
}
