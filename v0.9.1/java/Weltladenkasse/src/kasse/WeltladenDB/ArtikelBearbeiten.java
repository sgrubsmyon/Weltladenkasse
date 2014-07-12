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
    implements ArtikelFormularInterface, DocumentListener, ItemListener, ChangeListener {
    // Attribute:
    protected ArtikelFormular artikelFormular;
    protected Vector< Vector<Object> > originalData;
    protected Vector<Integer> originalProdGrIDs;
    protected Vector<Integer> originalLiefIDs;
    protected Vector<Boolean> originalVarPreisBools;

    protected JCheckBox aktivBox;
    protected JButton submitButton;

    private CurrencyDocumentFilter geldFilter = new CurrencyDocumentFilter();

    // Methoden:
    public ArtikelBearbeiten(Connection conn, MainWindowGrundlage mw, Artikelliste pw, JDialog dia,
            Vector< Vector<Object> > origData,
            Vector<Integer> origPrGrIDs,
            Vector<Integer> origLiefIDs,
            Vector<Boolean> origVPBools) {
	super(conn, mw, pw, dia);
        artikelFormular = new ArtikelFormular(conn, mw);
        originalData = new Vector< Vector<Object> >(origData);
        originalProdGrIDs = new Vector<Integer>(origPrGrIDs);
        originalLiefIDs = new Vector<Integer>(origLiefIDs);
        originalVarPreisBools = new Vector<Boolean>(origVPBools);
        showAll();
    }

    void showHeader() {
        headerPanel = new JPanel();
        artikelFormular.showHeader(headerPanel, allPanel);

        JPanel aktivPanel = new JPanel();
        aktivBox = new JCheckBox("Artikel aktiv");
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
        artikelFormular.vpeSpinner.addChangeListener(this);
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
        String firstName = (String)originalData.get(0).get(0);
        String firstNummer = (String)originalData.get(0).get(1);
        String firstBarcode = (String)originalData.get(0).get(2);
        Integer firstGruppenID = originalProdGrIDs.get(0);
        Boolean firstVarPreis = originalVarPreisBools.get(0);
        String firstVKP = (String)originalData.get(0).get(4);
        String firstEKP = (String)originalData.get(0).get(5);
        Integer firstVPE = (Integer)originalData.get(0).get(6);
        Integer firstLieferantID = originalLiefIDs.get(0);
        String firstHerkunft = (String)originalData.get(0).get(11);
        Boolean firstAktiv = (Boolean)originalData.get(0).get(12);
        if ( allRowsEqual(firstName, 0) ){
            artikelFormular.nameField.setText(firstName);
        } else {
            artikelFormular.nameField.setEnabled(false);
        }
        if ( allRowsEqual(firstNummer, 1) ){
            artikelFormular.nummerField.setText(firstNummer);
        } else {
            artikelFormular.nummerField.setEnabled(false);
        }
        if ( allRowsEqual(firstBarcode, 2) ){
            artikelFormular.barcodeField.setText(firstBarcode);
        } else {
            artikelFormular.barcodeField.setEnabled(false);
        }
        if ( allElementsEqual(firstGruppenID, originalProdGrIDs) ){
            int prodGrIndex = artikelFormular.produktgruppenIDs.indexOf(firstGruppenID);
            artikelFormular.produktgruppenBox.setSelectedIndex(prodGrIndex);
        } else {
            artikelFormular.produktgruppenBox.setEnabled(false);
        }
        if ( allElementsEqual(firstVarPreis, originalVarPreisBools) ){
            artikelFormular.preisVariabelBox.setSelected(firstVarPreis);
            if (!firstVarPreis){ // if all items have non-variable prices
                if ( allRowsEqual(firstVKP, 4) ){
                    artikelFormular.vkpreisField.setText(firstVKP);
                } else {
                    artikelFormular.vkpreisField.setEnabled(false);
                }
                if ( allRowsEqual(firstEKP, 5) ){
                    artikelFormular.ekpreisField.setText(firstEKP);
                } else {
                    artikelFormular.ekpreisField.setEnabled(false);
                }
            } else { // if all items have variable prices
                artikelFormular.vkpreisField.setEnabled(false);
                artikelFormular.ekpreisField.setEnabled(false);
            }
        } else {
            artikelFormular.vkpreisField.setEnabled(false);
            artikelFormular.ekpreisField.setEnabled(false);
            artikelFormular.preisVariabelBox.setEnabled(false);
        }
        if ( allRowsEqual(firstVPE, 6) ){
            //if ( firstVPE.equals("") ){ firstVPE = "0"; }
            artikelFormular.vpeSpinner.setValue(firstVPE);
        } else {
            artikelFormular.vpeSpinner.setEnabled(false);
        }
        if ( allElementsEqual(firstLieferantID, originalLiefIDs) ){
            int liefIndex = artikelFormular.lieferantIDs.indexOf(firstLieferantID);
            artikelFormular.lieferantBox.setSelectedIndex(liefIndex);
        } else {
            artikelFormular.lieferantBox.setEnabled(false);
        }
        if ( allRowsEqual(firstHerkunft, 11) ){
            artikelFormular.herkunftField.setText(firstHerkunft);
        } else {
            artikelFormular.herkunftField.setEnabled(false);
        }
        if ( allRowsEqual(firstAktiv, 12) ){
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
        if ( artikelFormular.nameField.isEnabled() ){
            String origName = (String)originalData.get(0).get(0);
            if ( !origName.equals(artikelFormular.nameField.getText()) )
                    return true;
        }
        if ( artikelFormular.nummerField.isEnabled() ){
            String origNummer = (String)originalData.get(0).get(1);
            if ( !origNummer.equals(artikelFormular.nummerField.getText()) )
                return true;
        }
        if ( artikelFormular.barcodeField.isEnabled() ){
            String origBarcode = (String)originalData.get(0).get(2);
            if ( !origBarcode.equals(artikelFormular.barcodeField.getText()) )
                return true;
        }
        if ( artikelFormular.produktgruppenBox.isEnabled() ){
            Integer origGruppenID = originalProdGrIDs.get(0);
            int selProdIndex = artikelFormular.produktgruppenBox.getSelectedIndex();
            Integer selProdID = artikelFormular.produktgruppenIDs.get(selProdIndex);
            if ( !origGruppenID.equals(selProdID) )
                return true;
        }
        if ( artikelFormular.preisVariabelBox.isEnabled() ){
            Boolean origVarPreis = originalVarPreisBools.get(0);
            if ( !origVarPreis.equals(artikelFormular.preisVariabelBox.isSelected()) )
                return true;
        }
        if ( artikelFormular.vkpreisField.isEnabled() ){
            String origVKP = priceFormatterIntern( (String)originalData.get(0).get(4) );
            String newVKP = priceFormatterIntern( artikelFormular.vkpreisField.getText() );
            if ( !origVKP.equals(newVKP) )
                return true;
        }
        if ( artikelFormular.ekpreisField.isEnabled() ){
            String origEKP = priceFormatterIntern( (String)originalData.get(0).get(5) );
            String newEKP = priceFormatterIntern( artikelFormular.ekpreisField.getText() );
            if ( !origEKP.equals(newEKP) )
                return true;
        }
        if ( artikelFormular.vpeSpinner.isEnabled() ){
            String origVPEStr = (String)originalData.get(0).get(6);
            if ( origVPEStr.equals("") ){ origVPEStr = "0"; }
            Integer origVPE = Integer.parseInt(origVPEStr);
            if ( !origVPE.equals(artikelFormular.vpeSpinner.getValue()) )
                return true;
        }
        if ( artikelFormular.lieferantBox.isEnabled() ){
            Integer origLieferantID = originalLiefIDs.get(0);
            int selLiefIndex = artikelFormular.lieferantBox.getSelectedIndex();
            Integer selLiefID = artikelFormular.lieferantIDs.get(selLiefIndex);
            if ( !origLieferantID.equals(selLiefID) )
                return true;
        }
        if ( artikelFormular.herkunftField.isEnabled() ){
            String origHerkunft = (String)originalData.get(0).get(11);
            if ( !origHerkunft.equals(artikelFormular.herkunftField.getText()) )
                return true;
        }
        if ( aktivBox.isEnabled() ){
            Boolean origAktiv = (Boolean)originalData.get(0).get(12);
            if ( !origAktiv.equals(aktivBox.isSelected()) )
                return true;
        }
        return false;
    }

    public boolean isSubmittable() {
        return checkIfFormIsComplete() && willDataBeLost();
    }

    public void fillComboBoxes() {
        artikelFormular.fillComboBoxes();
    }

    public boolean checkIfFormIsComplete() {
        return artikelFormular.checkIfFormIsComplete();
    }

    public void submit() {
        for (int i=0; i<originalData.size(); i++){
            String origName = (String)originalData.get(i).get(0);
            String origNummer = (String)originalData.get(i).get(1);
            String newName = artikelFormular.nameField.isEnabled() ?
                artikelFormular.nameField.getText() : origName;
            String newNummer = artikelFormular.nummerField.isEnabled() ?
                artikelFormular.nummerField.getText() : origNummer;
            if (newName != origName || newNummer != origNummer){
                if ( isItemAlreadyKnown(newName, newNummer) ){
                    // not allowed: changing name and nummer to a pair that is already registered in DB
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Kombination Namme/Nummer bereits vorhanden! Wird zurückgesetzt.",
                            "Info", JOptionPane.INFORMATION_MESSAGE);
                    artikelFormular.nameField.setText(origName);
                    artikelFormular.nummerField.setText(origNummer);
                    return;
                }
            }
            String barcode = artikelFormular.barcodeField.isEnabled() ?
                artikelFormular.barcodeField.getText() :
                (String)originalData.get(i).get(2);
            Integer produktgruppen_id = artikelFormular.produktgruppenBox.isEnabled() ?
                artikelFormular.produktgruppenIDs.get( artikelFormular.produktgruppenBox.getSelectedIndex() ) :
                originalProdGrIDs.get(i);
            Boolean preisVar = artikelFormular.preisVariabelBox.isEnabled() ?
                artikelFormular.preisVariabelBox.isSelected() :
                (Boolean)originalVarPreisBools.get(i);
            String vkpreis = artikelFormular.vkpreisField.isEnabled() ?
                artikelFormular.vkpreisField.getText() :
                (String)originalData.get(i).get(4);
            String ekpreis = artikelFormular.ekpreisField.isEnabled() ?
                artikelFormular.ekpreisField.getText() :
                (String)originalData.get(i).get(5);
            Integer vpe = artikelFormular.vpeSpinner.isEnabled() ?
                (Integer)artikelFormular.vpeSpinner.getValue() :
                (Integer)originalData.get(i).get(6);
            Integer lieferant_id = artikelFormular.lieferantBox.isEnabled() ?
                artikelFormular.lieferantIDs.get( artikelFormular.lieferantBox.getSelectedIndex() ) :
                (Integer)originalLiefIDs.get(i);
            String herkunft = artikelFormular.herkunftField.isEnabled() ?
                artikelFormular.herkunftField.getText() :
                (String)originalData.get(i).get(11);
            Boolean aktiv = aktivBox.isEnabled() ?
                aktivBox.isSelected() :
                (Boolean)originalData.get(i).get(12);

            // set old item to inactive:
            int result = setItemInactive(origName, origNummer);
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Artikel "+origName+" mit Nummer "+origNummer+" konnte nicht geändert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                continue; // continue with next item
            }
            if ( aktiv == true ){ // only if the item wasn't set inactive voluntarily: add new item with new properties
                result = insertNewItem(newName, newNummer, barcode, preisVar, vkpreis, ekpreis, vpe,
                        produktgruppen_id, lieferant_id, herkunft);
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel "+origName+" mit Nummer "+origNummer+" konnte nicht geändert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    result = setItemActive(origName, origNummer);
                    if (result == 0){
                        JOptionPane.showMessageDialog(this,
                                "Fehler: Artikel "+origName+" mit Nummer "+origNummer+" konnte nicht wieder hergestellt werden. Artikel ist nun gelöscht (inaktiv).",
                                "Fehler", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }

    /** Needed for ChangeListener. */
    public void stateChanged(ChangeEvent e) {
        submitButton.setEnabled( isSubmittable() );
    }

    /** Needed for ItemListener. */
    public void itemStateChanged(ItemEvent e) {
        artikelFormular.itemStateChanged(e);
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
	if (e.getSource() == artikelFormular.produktgruppenBox){
            submitButton.setEnabled( isSubmittable() );
            return;
        }
	if (e.getSource() == artikelFormular.lieferantBox){
            submitButton.setEnabled( isSubmittable() );
            return;
        }
	if (e.getSource() == submitButton){
            submit();
            artikelListe.updateAll();
            this.window.dispose(); // close
            return;
        }
	if (e.getSource() == closeButton){
            // Create the same effect as if user clicks on x or uses Alt-F4:
            // Do this by explicitly calling the method of the WindowAdapter
            // installed in Artikelliste.java
            WindowAdapter wa = (WindowAdapter)this.window.getWindowListeners()[0];
            wa.windowClosing(new WindowEvent(this.window, 0));
            return;
        }
        super.actionPerformed(e);
    }
}
