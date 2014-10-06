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
        artikelFormular.lieferantBox.addActionListener(this);
        artikelFormular.nummerField.getDocument().addDocumentListener(this);
        artikelFormular.nummerField.addKeyListener(enterAdapter);
        artikelFormular.nameField.getDocument().addDocumentListener(this);
        artikelFormular.nameField.addKeyListener(enterAdapter);
        artikelFormular.mengeField.getDocument().addDocumentListener(this);
        artikelFormular.mengeField.addKeyListener(enterAdapter);
        artikelFormular.barcodeField.getDocument().addDocumentListener(this);
        artikelFormular.barcodeField.addKeyListener(enterAdapter);
        artikelFormular.herkunftField.getDocument().addDocumentListener(this);
        artikelFormular.herkunftField.addKeyListener(enterAdapter);
        artikelFormular.vpeSpinner.addChangeListener(this);
        artikelFormular.vkpreisField.addKeyListener(enterAdapter);
        artikelFormular.vkpreisField.getDocument().addDocumentListener(this);
        artikelFormular.ekpreisField.addKeyListener(enterAdapter);
        artikelFormular.ekpreisField.getDocument().addDocumentListener(this);
        artikelFormular.preisVariabelBox.addItemListener(this);
        artikelFormular.sortimentBox.addItemListener(this);
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
        Integer firstGruppenID = originalProdGrIDs.get(0);
        Integer firstLieferantID = originalLiefIDs.get(0);
        String firstNummer = (String)originalData.get(0).get(2);
        String firstName = (String)originalData.get(0).get(3);
        String firstMenge = (String)originalData.get(0).get(4);
        String firstBarcode = (String)originalData.get(0).get(5);
        String firstHerkunft = (String)originalData.get(0).get(6);
        Integer firstVPE;
        try {
            firstVPE = Integer.parseInt( (String)originalData.get(0).get(7) );
        } catch (NumberFormatException ex) {
            firstVPE = null;
        }
        String firstVKP = (String)originalData.get(0).get(8);
        String firstEKP = (String)originalData.get(0).get(9);
        Boolean firstVarPreis = originalVarPreisBools.get(0);
        Boolean firstSortiment = (Boolean)originalData.get(0).get(13);
        Boolean firstAktiv = (Boolean)originalData.get(0).get(14);

        if ( allElementsEqual(firstGruppenID, originalProdGrIDs) ){
            int prodGrIndex = artikelFormular.produktgruppenIDs.indexOf(firstGruppenID);
            artikelFormular.produktgruppenBox.setSelectedIndex(prodGrIndex);
        } else {
            artikelFormular.produktgruppenBox.setEnabled(false);
        }
        if ( allElementsEqual(firstLieferantID, originalLiefIDs) ){
            int liefIndex = artikelFormular.lieferantIDs.indexOf(firstLieferantID);
            artikelFormular.lieferantBox.setSelectedIndex(liefIndex);
        } else {
            artikelFormular.lieferantBox.setEnabled(false);
        }
        if ( allRowsEqual(firstNummer, 2) ){
            artikelFormular.nummerField.setText(firstNummer);
        } else {
            artikelFormular.nummerField.setEnabled(false);
        }
        if ( allRowsEqual(firstName, 3) ){
            artikelFormular.nameField.setText(firstName);
        } else {
            artikelFormular.nameField.setEnabled(false);
        }
        if ( allRowsEqual(firstMenge, 4) ){
            artikelFormular.mengeField.setText(firstMenge);
        } else {
            artikelFormular.mengeField.setEnabled(false);
        }
        if ( allRowsEqual(firstBarcode, 5) ){
            artikelFormular.barcodeField.setText(firstBarcode);
        } else {
            artikelFormular.barcodeField.setEnabled(false);
        }
        if ( allRowsEqual(firstHerkunft, 6) ){
            artikelFormular.herkunftField.setText(firstHerkunft);
        } else {
            artikelFormular.herkunftField.setEnabled(false);
        }
        if ( allRowsEqual(firstVPE, 7) ){
            //if ( firstVPE.equals("") ){ firstVPE = "0"; }
            artikelFormular.vpeSpinner.setValue(firstVPE);
        } else {
            artikelFormular.vpeSpinner.setEnabled(false);
        }
        if ( allElementsEqual(firstVarPreis, originalVarPreisBools) ){
            artikelFormular.preisVariabelBox.setSelected(firstVarPreis);
            if (!firstVarPreis){ // if all items have non-variable prices
                if ( allRowsEqual(firstVKP, 8) ){
                    artikelFormular.vkpreisField.setText( priceFormatter(firstVKP) );
                } else {
                    artikelFormular.vkpreisField.setEnabled(false);
                }
                if ( allRowsEqual(firstEKP, 9) ){
                    artikelFormular.ekpreisField.setText( priceFormatter(firstEKP) );
                } else {
                    artikelFormular.ekpreisField.setEnabled(false);
                }
            } else { // if all items have variable prices
                artikelFormular.vkpreisField.setEnabled(false);
                artikelFormular.ekpreisField.setEnabled(false);
            }
        } else {
            artikelFormular.preisVariabelBox.setEnabled(false);
            artikelFormular.vkpreisField.setEnabled(false);
            artikelFormular.ekpreisField.setEnabled(false);
        }
        if ( allRowsEqual(firstSortiment, 13) ){
            artikelFormular.sortimentBox.setSelected(firstSortiment);
        } else {
            artikelFormular.sortimentBox.setEnabled(false);
        }
        if ( allRowsEqual(firstAktiv, 14) ){
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
        if ( artikelFormular.produktgruppenBox.isEnabled() ){
            Integer origGruppenID = originalProdGrIDs.get(0);
            int selProdIndex = artikelFormular.produktgruppenBox.getSelectedIndex();
            Integer selProdID = artikelFormular.produktgruppenIDs.get(selProdIndex);
            if ( !origGruppenID.equals(selProdID) ){
                return true;
            }
        }
        if ( artikelFormular.lieferantBox.isEnabled() ){
            Integer origLieferantID = originalLiefIDs.get(0);
            int selLiefIndex = artikelFormular.lieferantBox.getSelectedIndex();
            Integer selLiefID = artikelFormular.lieferantIDs.get(selLiefIndex);
            if ( !origLieferantID.equals(selLiefID) ){
                return true;
            }
        }
        if ( artikelFormular.nummerField.isEnabled() ){
            String origNummer = (String)originalData.get(0).get(2);
            if ( !origNummer.equals(artikelFormular.nummerField.getText()) ){
                return true;
            }
        }
        if ( artikelFormular.nameField.isEnabled() ){
            String origName = (String)originalData.get(0).get(3);
            if ( !origName.equals(artikelFormular.nameField.getText()) ){
                return true;
            }
        }
        if ( artikelFormular.mengeField.isEnabled() ){
            String origMengeStr = originalData.get(0).get(4).toString().replace(',','.');
            if (origMengeStr == "") origMengeStr = "0";
            String newMengeStr = artikelFormular.mengeField.getText().replace(',','.');
            if (newMengeStr == "") newMengeStr = "0";
            BigDecimal origMenge = new BigDecimal(origMengeStr);
            BigDecimal newMenge = new BigDecimal(newMengeStr);
            if ( !origMenge.equals(newMenge) ){
                return true;
            }
        }
        if ( artikelFormular.barcodeField.isEnabled() ){
            String origBarcode = (String)originalData.get(0).get(5);
            if ( !origBarcode.equals(artikelFormular.barcodeField.getText()) ){
                return true;
            }
        }
        if ( artikelFormular.herkunftField.isEnabled() ){
            String origHerkunft = (String)originalData.get(0).get(6);
            if ( !origHerkunft.equals(artikelFormular.herkunftField.getText()) ){
                return true;
            }
        }
        if ( artikelFormular.vpeSpinner.isEnabled() ){
            String origVPEStr = (String)originalData.get(0).get(7);
            if ( origVPEStr.equals("") ){ origVPEStr = "0"; }
            Integer origVPE = Integer.parseInt(origVPEStr);
            if ( !origVPE.equals(artikelFormular.vpeSpinner.getValue()) ){
                return true;
            }
        }
        if ( artikelFormular.vkpreisField.isEnabled() ){
            String origVKP = priceFormatterIntern( (String)originalData.get(0).get(8) );
            String newVKP = priceFormatterIntern( artikelFormular.vkpreisField.getText() );
            if ( !origVKP.equals(newVKP) ){
                return true;
            }
        }
        if ( artikelFormular.ekpreisField.isEnabled() ){
            String origEKP = priceFormatterIntern( (String)originalData.get(0).get(9) );
            String newEKP = priceFormatterIntern( artikelFormular.ekpreisField.getText() );
            if ( !origEKP.equals(newEKP) ){
                return true;
            }
        }
        if ( artikelFormular.preisVariabelBox.isEnabled() ){
            Boolean origVarPreis = originalVarPreisBools.get(0);
            if ( !origVarPreis.equals(artikelFormular.preisVariabelBox.isSelected()) ){
                return true;
            }
        }
        if ( artikelFormular.sortimentBox.isEnabled() ){
            Boolean origSortiment = (Boolean)originalData.get(0).get(13);
            if ( !origSortiment.equals(artikelFormular.sortimentBox.isSelected()) ){
                return true;
            }
        }
        if ( aktivBox.isEnabled() ){
            Boolean origAktiv = (Boolean)originalData.get(0).get(14);
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
        artikelFormular.fillComboBoxes();
    }

    public boolean checkIfFormIsComplete() {
        return artikelFormular.checkIfFormIsComplete();
    }

    public void submit() {
        for (int i=0; i<originalData.size(); i++){
            String origLieferant = (String)originalData.get(i).get(1);
            Integer origLieferantID = originalLiefIDs.get(i);
            String origNummer = (String)originalData.get(i).get(2);
            String newLieferant = artikelFormular.lieferantBox.isEnabled() ?
                (String)artikelFormular.lieferantBox.getSelectedItem() :
                origLieferant;
            Integer newLieferantID = artikelFormular.lieferantBox.isEnabled() ?
                artikelFormular.lieferantIDs.get( artikelFormular.lieferantBox.getSelectedIndex() ) :
                origLieferantID;
            String newNummer = artikelFormular.nummerField.isEnabled() ?
                artikelFormular.nummerField.getText() : origNummer;
            if (newLieferant != origLieferant || newNummer != origNummer){
                if ( isItemAlreadyKnown(newLieferant, newNummer) ){
                    // not allowed: changing name and nummer to a pair that is already registered in DB
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Kombination Lieferant/Nummer bereits vorhanden! Wird zurückgesetzt.",
                            "Info", JOptionPane.INFORMATION_MESSAGE);
                    artikelFormular.lieferantBox.setSelectedIndex(
                            artikelFormular.lieferantIDs.indexOf(origLieferantID)
                            );
                    artikelFormular.nummerField.setText(origNummer);
                    return;
                }
            }
            Integer produktgruppen_id = artikelFormular.produktgruppenBox.isEnabled() ?
                artikelFormular.produktgruppenIDs.get( artikelFormular.produktgruppenBox.getSelectedIndex() ) :
                originalProdGrIDs.get(i);
            String origName = (String)originalData.get(i).get(3);
            String newName = artikelFormular.nameField.isEnabled() ?
                artikelFormular.nameField.getText() : origName;
            BigDecimal menge = null;
            try {
                menge = artikelFormular.mengeField.isEnabled() ?
                    new BigDecimal( artikelFormular.mengeField.getText().replace(',', '.') ) :
                    new BigDecimal( ((String)originalData.get(i).get(7)).replace(',', '.') );
            } catch (NumberFormatException ex) {
                menge = null;
            }
            String barcode = artikelFormular.barcodeField.isEnabled() ?
                artikelFormular.barcodeField.getText() :
                (String)originalData.get(i).get(5);
            String herkunft = artikelFormular.herkunftField.isEnabled() ?
                artikelFormular.herkunftField.getText() :
                (String)originalData.get(i).get(6);
            Integer vpe = null;
            try {
                vpe = artikelFormular.vpeSpinner.isEnabled() ?
                    (Integer)artikelFormular.vpeSpinner.getValue() :
                    Integer.parseInt( (String)originalData.get(i).get(7) );
            } catch (NumberFormatException ex) {
                vpe = null;
            }
            String vkpreis = artikelFormular.vkpreisField.isEnabled() ?
                artikelFormular.vkpreisField.getText() :
                (String)originalData.get(i).get(8);
            String ekpreis = artikelFormular.ekpreisField.isEnabled() ?
                artikelFormular.ekpreisField.getText() :
                (String)originalData.get(i).get(9);
            Boolean preisVar = artikelFormular.preisVariabelBox.isEnabled() ?
                artikelFormular.preisVariabelBox.isSelected() :
                (Boolean)originalVarPreisBools.get(i);
            Boolean sortiment = artikelFormular.sortimentBox.isEnabled() ?
                artikelFormular.sortimentBox.isSelected() :
                (Boolean)originalData.get(i).get(13);
            Boolean aktiv = aktivBox.isEnabled() ?
                aktivBox.isSelected() :
                (Boolean)originalData.get(i).get(14);

            // set old item to inactive:
            int result = setItemInactive(origLieferantID, origNummer);
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Artikel "+origName+" von "+origLieferant+" mit Nummer "+origNummer+" konnte nicht geändert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                continue; // continue with next item
            }
            if ( aktiv == true ){ // only if the item wasn't set inactive voluntarily: add new item with new properties
                result = insertNewItem(produktgruppen_id, newLieferantID,
                        newNummer, newName, menge, barcode, herkunft, vpe,
                        vkpreis, ekpreis, preisVar, sortiment);
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel "+origName+" von "+origLieferant+" mit Nummer "+origNummer+" konnte nicht geändert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    result = setItemActive(origLieferantID, origNummer);
                    if (result == 0){
                        JOptionPane.showMessageDialog(this,
                                "Fehler: Artikel "+origName+" von "+origLieferant+" mit Nummer "+origNummer+" konnte nicht wieder hergestellt werden. Artikel ist nun gelöscht (inaktiv).",
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
