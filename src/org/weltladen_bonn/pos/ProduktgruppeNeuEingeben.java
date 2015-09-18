package org.weltladen_bonn.pos;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
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
import javax.swing.table.*;
import javax.swing.event.*; // for DocumentListener
import javax.swing.text.*; // for DocumentFilter

public class ProduktgruppeNeuEingeben extends DialogWindow
    implements ProduktgruppeFormularInterface, DocumentListener, ItemListener {
    // Attribute:
    protected Produktgruppenliste produktgruppenListe;
    protected ProduktgruppeFormular produktgruppeFormular;

    protected JButton submitButton;

    // Methoden:
    public ProduktgruppeNeuEingeben(Connection conn, MainWindowGrundlage mw, Produktgruppenliste pw, JDialog dia) {
	super(conn, mw, dia);
        produktgruppenListe = pw;
        produktgruppeFormular = new ProduktgruppeFormular(conn, mw);
        showAll();
    }

    protected void showHeader() {
        headerPanel = new JPanel();
        produktgruppeFormular.showHeader(headerPanel, allPanel);

        KeyAdapter enterAdapter = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
                    if (submitButton.isEnabled()){
                        submitButton.doClick();
                    }
                }
            }
        };

        produktgruppeFormular.parentProdGrBox.addActionListener(this);
        produktgruppeFormular.nameField.addKeyListener(enterAdapter);
        produktgruppeFormular.mwstBox.addActionListener(this);
        produktgruppeFormular.pfandBox.addActionListener(this);
        produktgruppeFormular.nameField.getDocument().addDocumentListener(this);
    }

    protected void showMiddle() { }

    protected void showFooter() {
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

    public void fillComboBoxes() {
        produktgruppeFormular.fillComboBoxes();
    }

    public boolean checkIfFormIsComplete() {
        return produktgruppeFormular.checkIfFormIsComplete();
    }

    // will data be lost on close?
    protected boolean willDataBeLost() {
        return checkIfFormIsComplete();
    }

    public int submit() {
        String newName = produktgruppeFormular.nameField.getText();
        if ( isProdGrAlreadyKnown(newName) ){
            // not allowed: changing name to one that is already registered in DB
            JOptionPane.showMessageDialog(this,
                    "Fehler: Produktgruppe '"+newName+"' bereits vorhanden!",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            produktgruppeFormular.nameField.setText("");
            return 0;
        }
        Integer parentProdGrID = produktgruppeFormular.parentProdGrIDs.get(
                produktgruppeFormular.parentProdGrBox.getSelectedIndex()
                );
        Vector<Integer> idsNew = produktgruppeFormular.idsOfNewProdGr(parentProdGrID);
        Integer topID = idsNew.get(0);
        Integer subID = idsNew.get(1);
        Integer subsubID = idsNew.get(2);
        Integer mwstID = produktgruppeFormular.mwstIDs.get(
                produktgruppeFormular.mwstBox.getSelectedIndex()
                );
        Integer pfandID = produktgruppeFormular.pfandIDs.get(
                produktgruppeFormular.pfandBox.getSelectedIndex()
                );
        return insertNewProdGr(topID, subID, subsubID, newName, mwstID, pfandID);
    }

    /** Needed for ItemListener. */
    public void itemStateChanged(ItemEvent e) {
        produktgruppeFormular.itemStateChanged(e);
        submitButton.setEnabled( checkIfFormIsComplete() );
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
	if (e.getSource() == submitButton){
            int result = submit();
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Produktgruppe "+produktgruppeFormular.nameField.getText()+" konnte nicht eingefügt werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            } else {
                produktgruppenListe.updateAll();
                closeButton.doClick();
            }
            return;
        }
        super.actionPerformed(e);
    }
}
