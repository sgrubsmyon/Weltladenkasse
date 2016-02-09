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
import javax.swing.event.*;
import javax.swing.table.*;

// Klasse, die Produktgruppenliste und Artikelliste speichert und anzeigt
public class ArtikellisteContainer extends WindowContent {
    // Attribute:
    private ProduktgruppenbaumArtikelliste prodList;
    private TreeSet<Integer> sortedExpandedRows; // saves (and thus enables to restore) the expansion state of JTree
    private String lastSearchStr; // saves (and thus enables to restore) the search string
    private Artikelliste artList;
    private boolean artListShown = false;

    private JPanel prodListPanel;
    private JTextField searchField;
    private JButton emptySearchButton;
    private JButton searchButton;

    private JButton saveButton;
    private JButton revertButton;
    private JButton editButton;
    private JButton newButton;
    private JButton addSimilarButton;
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

    public void switchToArtikelliste(Artikelliste artList) {
        // remember the expansion state
        JTree tree = prodList.getTree();
        Enumeration treeCache = tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()));
        sortedExpandedRows = new TreeSet<Integer>();
        while (treeCache.hasMoreElements()){
            TreePath treePath = (TreePath)treeCache.nextElement();
            sortedExpandedRows.add( tree.getRowForPath(treePath) );
        }
        this.remove(prodListPanel);
        this.revalidate();
        this.artList = artList;
        this.add(this.artList, BorderLayout.CENTER);
        artListShown = true;
    }

    public void switchToArtikellisteProduktgruppe(Integer topid, Integer subid, Integer subsubid, String gruppenname) {
        Artikelliste artList = new Artikelliste(this.conn, this, topid, subid, subsubid, gruppenname);
        switchToArtikelliste(artList);
    }

    public void switchToArtikellisteSearchString(String searchStr) {
        Artikelliste artList = new Artikelliste(this.conn, this, searchStr);
        switchToArtikelliste(artList);
        // remember the search string
        lastSearchStr = searchStr;
    }

    public void switchToProduktgruppenliste() {
        this.remove(artList);
        this.revalidate();
        artList = new Artikelliste(this.conn, this);
        showProdListPanel();
        // be reminded of expansion state
        JTree tree = prodList.getTree();
        for (Integer i : sortedExpandedRows){
            tree.expandRow(i);
        }
        // be reminded of search string
        searchField.setText(lastSearchStr);
        artListShown = false;
        enableButtons();
    }

    private void showTopPanel() {
        JPanel topPanel = new JPanel();
          JLabel searchLabel = new JLabel("Artikel suchen:");
          topPanel.add(searchLabel);
          searchField = new JTextField("");
          searchField.setColumns(20);
          searchField.getDocument().addDocumentListener(new DocumentListener() {
              public void insertUpdate(DocumentEvent e) {
                  if (searchField.getText().length() >= 3) {
                      searchButton.setEnabled(true);
                  } else {
                      searchButton.setEnabled(false);
                  }
              }

              public void removeUpdate(DocumentEvent e) {
                  this.insertUpdate(e);
              }

              public void changedUpdate(DocumentEvent e) {
                  // Plain text components do not fire these events
              }
          });
          searchField.addKeyListener(new KeyAdapter() {
              public void keyPressed(KeyEvent e) {
                  if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                      searchButton.doClick();
                  }
              }
          });
          topPanel.add(searchField);
          searchLabel.setLabelFor(searchField);
          emptySearchButton = new JButton("x");
          emptySearchButton.addActionListener(this);
          topPanel.add(emptySearchButton);
          searchButton = new JButton("Los!");
          searchButton.setEnabled(false);
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

            addSimilarButton = new JButton("Ähnliche Artikel eingeben");
            addSimilarButton.setMnemonic(KeyEvent.VK_A);
            addSimilarButton.addActionListener(this);
            bottomRightPanel.add(addSimilarButton);

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
            editButton.setEnabled(artList.myTable.getSelectedRowCount() > 0 &&
                    artList.numberOfSelectedInactiveArticles() == 0);
            newButton.setEnabled(artList.editedArticles.size() == 0);
            addSimilarButton.setEnabled(artList.myTable.getSelectedRowCount() == 1);
            importButton.setEnabled(artList.editedArticles.size() == 0);
            exportButton.setEnabled(artList.editedArticles.size() == 0);
        } else {
            saveButton.setEnabled(false);
            revertButton.setEnabled(false);
            editButton.setEnabled(false);
            newButton.setEnabled(true);
            addSimilarButton.setEnabled(false);
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
        if (e.getSource() == searchButton){
            String searchStr = searchField.getText();
            switchToArtikellisteSearchString(searchStr);
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
        if (e.getSource() == addSimilarButton){
            artList.showNewSimilarItemDialog();
            return;
        }
        if (e.getSource() == importButton){
            artList.showReadFromFileDialog();
            return;
        }
        if (e.getSource() == exportButton){
            if (!artListShown) {
                // in order to export *all* articles:
                artList = new Artikelliste(this.conn, this, null, null, null, "Alle Artikel (0)");
            }
            artList.showExportDialog();
            if (!artListShown) {
                artList = new Artikelliste(this.conn, this);
            }
            return;
        }
    }
}
