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

public class ProduktgruppeBearbeiten extends DialogWindow
    implements ProduktgruppeFormularInterface, DocumentListener, ItemListener {
    // Attribute:
    protected ProduktgruppeFormular produktgruppeFormular;
    protected Vector< Vector<Object> > originalData;
    protected Vector<Integer> originalMwStIDs;
    protected Vector<Integer> originalPfandIDs;
    protected Produktgruppenliste produktgruppenListe;

    protected JCheckBox aktivBox;
    protected JButton submitButton;

    // Methoden:
    public ProduktgruppeBearbeiten(Connection conn, MainWindowGrundlage mw, Produktgruppenliste pw, JDialog dia,
            Vector< Vector<Object> > origData,
            Vector<Integer> origMwStIDs,
            Vector<Integer> origPfandIDs) {
	super(conn, mw, dia);
        produktgruppenListe = pw;
        produktgruppeFormular = new ProduktgruppeFormular(conn, mw);
        originalData = new Vector< Vector<Object> >(origData);
        originalMwStIDs = new Vector<Integer>(origMwStIDs);
        originalPfandIDs = new Vector<Integer>(origPfandIDs);
        showAll();
    }

    void showHeader() {
        headerPanel = new JPanel();
        produktgruppeFormular.showHeader(headerPanel, allPanel);

        JPanel aktivPanel = new JPanel();
        aktivBox = new JCheckBox("Produktgruppe aktiv");
        aktivBox.setSelected(true);
        aktivPanel.add(aktivBox);
        headerPanel.add(aktivPanel);

        setOriginalValues();

        KeyAdapter enterAdapter = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
                    if (submitButton.isEnabled()){
                        submitButton.doClick();
                    }
                }
            }
        };

        produktgruppeFormular.mwstBox.addActionListener(this);
        produktgruppeFormular.pfandBox.addActionListener(this);
        produktgruppeFormular.nameField.getDocument().addDocumentListener(this);
        produktgruppeFormular.nameField.addKeyListener(enterAdapter);
        aktivBox.addItemListener(this);
    }

    void showMiddle() {
    }

    void showFooter() {
        footerPanel = new JPanel();
        submitButton = new JButton("Abschicken");
        submitButton.setMnemonic(KeyEvent.VK_A);
        submitButton.addActionListener(this);
        submitButton.setEnabled( isSubmittable() );
        footerPanel.add(submitButton);
        closeButton = new JButton("Schließen");
        closeButton.setMnemonic(KeyEvent.VK_S);
        closeButton.addActionListener(this);
        closeButton.setEnabled(true);
        footerPanel.add(closeButton);
        allPanel.add(footerPanel);
    }

    private void setOriginalValues() {
        Integer firstMwStID = originalMwStIDs.get(0);
        Integer firstPfandID = originalPfandIDs.get(0);
        String firstName = (String)originalData.get(0).get(3);
        Boolean firstAktiv = (Boolean)originalData.get(0).get(15);

        if ( allElementsEqual(firstMwStID, originalMwStIDs) ){
            int index = produktgruppeFormular.mwstIDs.indexOf(firstMwStID);
            produktgruppeFormular.mwstBox.setSelectedIndex(index);
        } else {
            produktgruppeFormular.mwstBox.setSelectedIndex(-1);
        }
        if ( allElementsEqual(firstPfandID, originalPfandIDs) ){
            int index = produktgruppeFormular.pfandIDs.indexOf(firstPfandID);
            produktgruppeFormular.pfandBox.setSelectedIndex(index);
        } else {
            produktgruppeFormular.pfandBox.setSelectedIndex(-1);
        }
        if ( allRowsEqual(firstName, 3) ){
            produktgruppeFormular.nameField.setText(firstName);
        } else {
            produktgruppeFormular.nameField.setEnabled(false);
        }
        if ( allRowsEqual(firstAktiv, 15) ){
            aktivBox.setSelected(firstAktiv);
        } else {
            aktivBox.setEnabled(false);
        }
    }

    private boolean allRowsEqual(Object value, int colIndex) {
        for (int i=0; i<originalData.size(); i++){
            if ( ! originalData.get(i).get(colIndex).equals(value) ){
                return false;
            }
        }
        return true;
    }

    private <T> boolean allElementsEqual(T element, Vector<T> vector) {
        for (T elem : vector){
            if ( ! elem.equals(element) ){
                return false;
            }
        }
        return true;
    }

    // will data be lost on close?
    public boolean willDataBeLost() {
        if ( produktgruppeFormular.mwstBox.isEnabled() ){
            int selIndex = produktgruppeFormular.mwstBox.getSelectedIndex();
            Integer selMwStID = produktgruppeFormular.mwstIDs.get(selIndex);
            if ( !allElementsEqual(selMwStID, originalMwStIDs) ){
                return true;
            }
        }
        if ( produktgruppeFormular.pfandBox.isEnabled() ){
            int selIndex = produktgruppeFormular.pfandBox.getSelectedIndex();
            Integer selPfandID = produktgruppeFormular.pfandIDs.get(selIndex);
            if ( !allElementsEqual(selPfandID, originalPfandIDs) ){
                return true;
            }
        }
        if ( produktgruppeFormular.nameField.isEnabled() ){
            String origName = (String)originalData.get(0).get(3);
            if ( !origName.equals(produktgruppeFormular.nameField.getText()) ){
                return true;
            }
        }
        if ( aktivBox.isEnabled() ){
            Boolean origAktiv = (Boolean)originalData.get(0).get(15);
            if ( !origAktiv.equals(aktivBox.isSelected()) ){
                return true;
            }
        }
        return false;
    }

    public boolean isSubmittable() {
        return checkIfFormIsComplete() && willDataBeLost();
    }

    public void fillComboBoxes() {
        produktgruppeFormular.fillComboBoxes();
    }

    public boolean checkIfFormIsComplete() {
        return produktgruppeFormular.checkIfFormIsComplete();
    }

    public int submit() {
        for (int i=0; i<originalData.size(); i++){
            String origName = (String)originalData.get(i).get(3);
            String newName = produktgruppeFormular.nameField.isEnabled() ?
                produktgruppeFormular.nameField.getText() : origName;
            if ( !newName.equals(origName) ){
                if ( isItemAlreadyKnown(newName) ){
                    // not allowed: changing name to one that is already registered in DB
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Produktgruppe '"+newName+"' bereits vorhanden! Wird zurückgesetzt.",
                            "Info", JOptionPane.INFORMATION_MESSAGE);
                    produktgruppeFormular.nameField.setText(origName);
                    return 1;
                }
            }
            // TODO Add a "parent prod. gr." ComboBox and get topid, subid from the parent
            // TODO Query current max. subsubid (if subid != null) or max. subid and increment it by one
            Integer mwst_id = produktgruppeFormular.mwstBox.isEnabled() ?
                produktgruppeFormular.mwstIDs.get( produktgruppeFormular.mwstBox.getSelectedIndex() ) :
                originalMwStIDs.get(i);
            Integer pfand_id = produktgruppeFormular.pfandBox.isEnabled() ?
                produktgruppeFormular.pfandIDs.get( produktgruppeFormular.pfandBox.getSelectedIndex() ) :
                originalPfandIDs.get(i);
            Boolean aktiv = aktivBox.isEnabled() ?
                aktivBox.isSelected() :
                (Boolean)originalData.get(i).get(15);

            // set old item to inactive:
            int result = setProdGrInactive(origName);
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Produktgruppe "+origName+" konnte nicht geändert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                continue; // continue with next item
            }
            if ( aktiv == true ){ // only if the item wasn't set inactive voluntarily: add new item with new properties
                result = insertNewProdGr(topid, subid, subsubid, newName, mwst_id, pfand_id);
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Produktgruppe "+origName+" konnte nicht geändert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    result = setProdGrActive(origName);
                    if (result == 0){
                        JOptionPane.showMessageDialog(this,
                                "Fehler: Produktgruppe "+origName+" konnte nicht wieder hergestellt werden. Produktgruppe ist nun gelöscht (inaktiv).",
                                "Fehler", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
        return 0;
    }

    /** Needed for ItemListener. */
    public void itemStateChanged(ItemEvent e) {
        produktgruppeFormular.itemStateChanged(e);
        submitButton.setEnabled( isSubmittable() );
    }

    /**
     *    * Each non abstract class that implements the DocumentListener
     *      must have these methods.
     *
     *    @param e the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
	// check if form is valid (if item can be added to list)
        submitButton.setEnabled( isSubmittable() );
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
	if (e.getSource() == produktgruppeFormular.mwstBox){
            submitButton.setEnabled( isSubmittable() );
            return;
        }
	if (e.getSource() == produktgruppeFormular.pfandBox){
            submitButton.setEnabled( isSubmittable() );
            return;
        }
	if (e.getSource() == submitButton){
            int result = submit();
            if (result == 0){
                produktgruppenListe.updateAll();
                this.window.dispose(); // close
            }
            return;
        }
	if (e.getSource() == closeButton){
            // Create the same effect as if user clicks on x or uses Alt-F4:
            // Do this by explicitly calling the method of the WindowAdapter
            // installed in Produktgruppenliste.java
            WindowAdapter wa = (WindowAdapter)this.window.getWindowListeners()[0];
            wa.windowClosing(new WindowEvent(this.window, 0));
            return;
        }
        super.actionPerformed(e);
    }
}
