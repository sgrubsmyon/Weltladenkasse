package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

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
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;
//import javax.swing.table.*;
import javax.swing.event.*; // for DocumentListener
import javax.swing.text.*; // for DocumentFilter

public class ArtikelBearbeiten extends ArtikelDialogWindowGrundlage 
    implements ArtikelFormularInterface, DocumentListener, ItemListener {
    // Attribute:
    protected ArtikelFormular artikelFormular;

    protected JButton submitButton;
    protected JButton closeButton;

    private CurrencyDocumentFilter geldFilter = new CurrencyDocumentFilter();

    // Methoden:
    public ArtikelBearbeiten(Connection conn, MainWindowGrundlage mw, Artikelliste pw, JDialog dia, String tid, String sid, String ssid) {
	super(conn, mw, pw, dia);
        artikelFormular = new ArtikelFormular(conn, mw, tid, sid, ssid);
        showAll();
    }

    void showHeader() {
        artikelFormular.showHeader(headerPanel, allPanel);

        KeyAdapter enterAdapter = new KeyAdapter() {
            public void keyPressed(KeyEvent e) { 
                if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
                    if (submitButton.isEnabled()){ 
                        submitButton.doClick();
                    }
                } 
            }
        };

        artikelFormular.produktgruppenBox.addActionListener(this);
        artikelFormular.nameField.getDocument().addDocumentListener(this);
        artikelFormular.nameField.addKeyListener(enterAdapter);
        artikelFormular.nummerField.getDocument().addDocumentListener(this);
        artikelFormular.nummerField.addKeyListener(enterAdapter);
        artikelFormular.barcodeField.getDocument().addDocumentListener(this);
        artikelFormular.barcodeField.addKeyListener(enterAdapter);
        artikelFormular.vkpreisField.addKeyListener(enterAdapter);
        artikelFormular.vkpreisField.getDocument().addDocumentListener(this);
        artikelFormular.ekpreisField.addKeyListener(enterAdapter);
        artikelFormular.ekpreisField.getDocument().addDocumentListener(this);
        artikelFormular.preisVariabelBox.addItemListener(this);
        artikelFormular.lieferantBox.addActionListener(this);
        artikelFormular.herkunftField.getDocument().addDocumentListener(this);
        artikelFormular.herkunftField.addKeyListener(enterAdapter);
    }

    void showMiddle() {
    }

    void showFooter() {
        footerPanel = new JPanel();
        submitButton = new JButton("Abschicken");
        submitButton.setMnemonic(KeyEvent.VK_A);
        submitButton.addActionListener(this);
        submitButton.setEnabled( checkIfFormIsComplete() );
        footerPanel.add(submitButton);
        closeButton = new JButton("Schließen");
        closeButton.setMnemonic(KeyEvent.VK_S);
        closeButton.addActionListener(this);
        closeButton.setEnabled(true);
        footerPanel.add(closeButton);
        allPanel.add(footerPanel);
    }

    public boolean checkIfFormIsComplete() {
        return artikelFormular.checkIfFormIsComplete();
    }

    public void fillComboBoxes() {
        artikelFormular.fillComboBoxes();
    }

    void submit() {
    }

    // will data be lost on close?
    public boolean willDataBeLost() {
        return false;
    }

    /** Needed for ItemListener. */
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
        if (source == artikelFormular.preisVariabelBox) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                artikelFormular.vkpreisField.setEnabled(false);
                artikelFormular.ekpreisField.setEnabled(false);
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                artikelFormular.vkpreisField.setEnabled(true);
                artikelFormular.ekpreisField.setEnabled(true);
            }
            submitButton.setEnabled( checkIfFormIsComplete() );
        }
    }

    /**
     *    * Each non abstract class that implements the DocumentListener
     *      must have these methods.
     *
     *    @param e the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
	// check if form is valid (if item can be added to list)
        submitButton.setEnabled( checkIfFormIsComplete() );
    }
    public void removeUpdate(DocumentEvent e) {
	// check if form is valid (if item can be added to list)
        insertUpdate(e);
    }
    public void changedUpdate(DocumentEvent e) {
	// Plain text components do not fire these events
    }


    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
	if (e.getSource() == artikelFormular.produktgruppenBox){
            submitButton.setEnabled( checkIfFormIsComplete() );
            return;
        }
	if (e.getSource() == artikelFormular.lieferantBox){
            submitButton.setEnabled( checkIfFormIsComplete() );
            return;
        }
	if (e.getSource() == submitButton){
            submit();
            updateAll();
            return;
        }
        super.actionPerformed(e);
    }
}
