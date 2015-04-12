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

public class ArtikelBearbeiten extends DialogWindow
    implements ArtikelFormularInterface, DocumentListener, ItemListener, ChangeListener {
    // Attribute:
    protected Artikelliste artikelListe;
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
	super(conn, mw, dia);
        artikelListe = pw;
        artikelFormular = new ArtikelFormular(conn, mw, true, true, true);
        originalData = new Vector< Vector<Object> >(origData);
        originalProdGrIDs = new Vector<Integer>(origPrGrIDs);
        originalLiefIDs = new Vector<Integer>(origLiefIDs);
        originalVarPreisBools = new Vector<Boolean>(origVPBools);
        showAll();
    }

    protected void showHeader() {
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
        artikelFormular.kurznameField.getDocument().addDocumentListener(this);
        artikelFormular.kurznameField.addKeyListener(enterAdapter);
        artikelFormular.mengeField.getDocument().addDocumentListener(this);
        artikelFormular.mengeField.addKeyListener(enterAdapter);
        artikelFormular.barcodeField.getDocument().addDocumentListener(this);
        artikelFormular.barcodeField.addKeyListener(enterAdapter);
        artikelFormular.herkunftField.getDocument().addDocumentListener(this);
        artikelFormular.herkunftField.addKeyListener(enterAdapter);
        artikelFormular.vpeSpinner.addChangeListener(this);
        artikelFormular.setSpinner.addChangeListener(this);
        artikelFormular.vkpreisField.addKeyListener(enterAdapter);
        artikelFormular.vkpreisField.getDocument().addDocumentListener(this);
        artikelFormular.empfvkpreisField.addKeyListener(enterAdapter);
        artikelFormular.empfvkpreisField.getDocument().addDocumentListener(this);
        artikelFormular.ekrabattField.addKeyListener(enterAdapter);
        artikelFormular.ekrabattField.getDocument().addDocumentListener(this);
        artikelFormular.ekpreisField.addKeyListener(enterAdapter);
        artikelFormular.ekpreisField.getDocument().addDocumentListener(this);
        artikelFormular.preisVariabelBox.addItemListener(this);
        artikelFormular.sortimentBox.addItemListener(this);
        artikelFormular.lieferbarBox.addItemListener(this);
        artikelFormular.beliebtBox.addActionListener(this);
        aktivBox.addItemListener(this);
    }

    protected void showMiddle() { }

    protected void showFooter() {
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
        String firstKurzname = (String)originalData.get(0).get(4);
        String firstMenge = (String)originalData.get(0).get(5);
        String firstVKP = (String)originalData.get(0).get(6);
        Boolean firstSortiment = (Boolean)originalData.get(0).get(7);
        Boolean firstLieferbar = (Boolean)originalData.get(0).get(8);
        Integer firstBeliebtWert = (Integer)originalData.get(0).get(9);
        String firstBarcode = (String)originalData.get(0).get(10);
        Integer firstVPE = (Integer)originalData.get(0).get(11);
        Integer firstSetgroesse = (Integer)originalData.get(0).get(12);
        String firstEVKP = (String)originalData.get(0).get(13);
        String firstEKR = (String)originalData.get(0).get(14);
        String firstEKP = (String)originalData.get(0).get(15);
        Boolean firstVarPreis = originalVarPreisBools.get(0);
        String firstHerkunft = (String)originalData.get(0).get(17);
        Boolean firstAktiv = (Boolean)originalData.get(0).get(21);

        if ( allElementsEqual(firstGruppenID, originalProdGrIDs) ){
            int prodGrIndex = artikelFormular.produktgruppenIDs.indexOf(firstGruppenID);
            artikelFormular.produktgruppenBox.setSelectedIndex(prodGrIndex);
        } else {
            artikelFormular.produktgruppenBox.setSelectedIndex(-1);
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
            artikelFormular.nameField.setText("");
        }
        if ( allRowsEqual(firstKurzname, 4) ){
            artikelFormular.kurznameField.setText(firstKurzname);
        } else {
            artikelFormular.kurznameField.setText("");
        }
        if ( allRowsEqual(firstMenge, 5) ){
            artikelFormular.mengeField.setText(firstMenge);
        } else {
            artikelFormular.mengeField.setText("");
        }
        if ( allRowsEqual(firstBarcode, 10) ){
            artikelFormular.barcodeField.setText(firstBarcode);
        } else {
            artikelFormular.barcodeField.setText("");
        }
        if ( allRowsEqual(firstHerkunft, 17) ){
            artikelFormular.herkunftField.setText(firstHerkunft);
        } else {
            artikelFormular.herkunftField.setText("");
        }
        if ( allRowsEqual(firstVPE, 11) ){
            if (firstVPE != null){
                artikelFormular.vpeSpinner.setValue(firstVPE);
            } else {
                artikelFormular.vpeSpinner.setValue(0);
            }
        } else {
            artikelFormular.vpeSpinner.setValue(0);
        }
        if ( allRowsEqual(firstSetgroesse, 12) ){
            artikelFormular.setSpinner.setValue(firstSetgroesse);
        } else {
            artikelFormular.setSpinner.setValue(1);
        }
        if ( allElementsEqual(firstVarPreis, originalVarPreisBools) ){
            artikelFormular.preisVariabelBox.setSelected(firstVarPreis);
            if (!firstVarPreis){ // if all items have non-variable prices
                if ( allRowsEqual(firstVKP, 6) ){
                    artikelFormular.vkpreisField.setText( priceFormatter(firstVKP) );
                } else {
                    artikelFormular.vkpreisField.setText("");
                }
                if ( allRowsEqual(firstEVKP, 13) ){
                    artikelFormular.empfvkpreisField.setText( priceFormatter(firstEVKP) );
                } else {
                    artikelFormular.empfvkpreisField.setText("");
                }
                if ( allRowsEqual(firstEKR, 14) ){
                    artikelFormular.ekrabattField.setText( priceFormatter(firstEKR) );
                } else {
                    artikelFormular.ekrabattField.setText("");
                }
                if ( allRowsEqual(firstEKP, 15) ){
                    artikelFormular.ekpreisField.setText( priceFormatter(firstEKP) );
                } else {
                    artikelFormular.ekpreisField.setText("");
                }
            } else { // if all items have variable prices
                artikelFormular.vkpreisField.setEnabled(false);
                artikelFormular.empfvkpreisField.setEnabled(false);
                artikelFormular.ekrabattField.setEnabled(false);
                artikelFormular.ekpreisField.setEnabled(false);
            }
        } else { // mix of variable and non-variable prices
            artikelFormular.preisVariabelBox.setEnabled(false);
            artikelFormular.vkpreisField.setEnabled(false);
            artikelFormular.empfvkpreisField.setEnabled(false);
            artikelFormular.ekrabattField.setEnabled(false);
            artikelFormular.ekpreisField.setEnabled(false);
        }
        if ( allRowsEqual(firstSortiment, 7) ){
            artikelFormular.sortimentBox.setSelected(firstSortiment);
        } else {
            artikelFormular.sortimentBox.setEnabled(false);
        }
        if ( allRowsEqual(firstLieferbar, 8) ){
            artikelFormular.lieferbarBox.setSelected(firstLieferbar);
        } else {
            artikelFormular.lieferbarBox.setEnabled(false);
        }
        if ( allRowsEqual(firstBeliebtWert, 9) ){
            artikelFormular.beliebtBox.setSelectedIndex(artikelFormular.beliebtWerte.indexOf(firstBeliebtWert));
        } else {
            artikelFormular.beliebtBox.setEnabled(false);
        }
        if ( allRowsEqual(firstAktiv, 21) ){
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
            int selIndex = artikelFormular.produktgruppenBox.getSelectedIndex();
            // -1 means "no selection done"
            if (selIndex != -1){
                Integer selProdGrID = artikelFormular.produktgruppenIDs.get(selIndex);
                if ( !allElementsEqual(selProdGrID, originalProdGrIDs) ){
                    return true;
                }
            }
        }
        if ( artikelFormular.lieferantBox.isEnabled() ){
            int selIndex = artikelFormular.lieferantBox.getSelectedIndex();
            // -1 means "no selection done"
            if (selIndex != -1){
                Integer selLiefID = artikelFormular.lieferantIDs.get(selIndex);
                if ( !allElementsEqual(selLiefID, originalLiefIDs) ){
                    return true;
                }
            }
        }
        if ( artikelFormular.nummerField.isEnabled() ){
            String origNummer = (String)originalData.get(0).get(2);
            if ( !origNummer.equals(artikelFormular.nummerField.getText()) ){
                return true;
            }
        }
        if ( artikelFormular.nameField.isEnabled() ){
            String name = artikelFormular.nameField.getText();
            // "" means "not edited"
            if (!name.equals("")){
                Vector<String> origNames = new Vector<String>();
                for (Vector<Object> v : originalData){
                    origNames.add((String)v.get(3));
                }
                if ( !allElementsEqual(name, origNames) ){
                    return true;
                }
            }
        }
        if ( artikelFormular.kurznameField.isEnabled() ){
            String kurzname = artikelFormular.kurznameField.getText();
            // "" means "not edited"
            if (!kurzname.equals("")){
                Vector<String> origKurznames = new Vector<String>();
                for (Vector<Object> v : originalData){
                    origKurznames.add((String)v.get(4));
                }
                if ( !allElementsEqual(kurzname, origKurznames) ){
                    return true;
                }
            }
        }
        if ( artikelFormular.mengeField.isEnabled() ){
            String newMengeStr = artikelFormular.mengeField.getText().replace(',','.');
            BigDecimal newMenge;
            try {
                newMenge = new BigDecimal(newMengeStr).stripTrailingZeros();
            } catch (NumberFormatException ex) {
                newMenge = new BigDecimal("0");
            }
            if ( !newMenge.equals(new BigDecimal("0")) ){ // means not edited (alt. if !newMengeStr.equals(""))
                Vector<BigDecimal> origMengen = new Vector<BigDecimal>();
                for (Vector<Object> v : originalData){
                    try {
                        String origMengeStr = ((String)v.get(5)).replace(',','.');
                        origMengen.add( new BigDecimal(origMengeStr).stripTrailingZeros() );
                    } catch (NumberFormatException ex) {
                        origMengen.add( new BigDecimal("0") );
                    }
                }
                if ( !allElementsEqual(newMenge, origMengen) ){
                    return true;
                }
            }
        }
        if ( artikelFormular.barcodeField.isEnabled() ){
            String barcode = artikelFormular.barcodeField.getText();
            // "" means "not edited"
            if (!barcode.equals("")){
                Vector<String> origBarcodes = new Vector<String>();
                for (Vector<Object> v : originalData){
                    origBarcodes.add((String)v.get(10));
                }
                if ( !allElementsEqual(barcode, origBarcodes) ){
                    return true;
                }
            }
        }
        if ( artikelFormular.herkunftField.isEnabled() ){
            String herkunft = artikelFormular.herkunftField.getText();
            // "" means "not edited"
            if (!herkunft.equals("")){
                Vector<String> origHerkunft = new Vector<String>();
                for (Vector<Object> v : originalData){
                    origHerkunft.add((String)v.get(17));
                }
                if ( !allElementsEqual(herkunft, origHerkunft) ){
                    return true;
                }
            }
        }
        if ( artikelFormular.vpeSpinner.isEnabled() ){
            try {
                Integer newVPE = (Integer)artikelFormular.vpeSpinner.getValue();
                // 0 means "not edited"
                if (newVPE != 0){
                    Vector<Integer> origVPEs = new Vector<Integer>();
                    for (Vector<Object> v : originalData){
                        Integer origVPE = (Integer)v.get(11);
                        if ( origVPE == null ){ origVPE = 0; }
                        origVPEs.add( origVPE );
                    }
                    if ( !allElementsEqual(newVPE, origVPEs) ){
                        return true;
                    }
                }
            } catch (NumberFormatException ex) {
            }
        }
        if ( artikelFormular.setSpinner.isEnabled() ){
            try {
                Integer newSetgroesse = (Integer)artikelFormular.setSpinner.getValue();
                // 0 means "not edited"
                if (newSetgroesse != 0){
                    Vector<Integer> origSetgroessen = new Vector<Integer>();
                    for (Vector<Object> v : originalData){
                        origSetgroessen.add((Integer)v.get(12));
                    }
                    if ( !allElementsEqual(newSetgroesse, origSetgroessen) ){
                        return true;
                    }
                }
            } catch (NumberFormatException ex) {
            }
        }
        if ( artikelFormular.vkpreisField.isEnabled() ){
            String newVKP = priceFormatterIntern( artikelFormular.vkpreisField.getText() );
            // "" means "not edited"
            if (!newVKP.equals("")){
                Vector<String> origVKPs = new Vector<String>();
                for (Vector<Object> v : originalData){
                    origVKPs.add( priceFormatterIntern((String)v.get(6)) );
                }
                if ( !allElementsEqual(newVKP, origVKPs) ){
                    return true;
                }
            }
        }
        if ( artikelFormular.empfvkpreisField.isEnabled() ){
            String newEVKP = priceFormatterIntern( artikelFormular.empfvkpreisField.getText() );
            // "" means "not edited"
            if (!newEVKP.equals("")){
                Vector<String> origEVKPs = new Vector<String>();
                for (Vector<Object> v : originalData){
                    origEVKPs.add( priceFormatterIntern((String)v.get(13)) );
                }
                if ( !allElementsEqual(newEVKP, origEVKPs) ){
                    return true;
                }
            }
        }
        if ( artikelFormular.ekrabattField.isEnabled() ){
            String newEKR = priceFormatterIntern( artikelFormular.ekrabattField.getText() );
            // "" means "not edited"
            if (!newEKR.equals("")){
                Vector<String> origEKRs = new Vector<String>();
                for (Vector<Object> v : originalData){
                    origEKRs.add( priceFormatterIntern((String)v.get(14)) );
                }
                if ( !allElementsEqual(newEKR, origEKRs) ){
                    return true;
                }
            }
        }
        if ( artikelFormular.ekpreisField.isEnabled() ){
            String newEKP = priceFormatterIntern( artikelFormular.ekpreisField.getText() );
            // "" means "not edited"
            if (!newEKP.equals("")){
                Vector<String> origEKPs = new Vector<String>();
                for (Vector<Object> v : originalData){
                    origEKPs.add( priceFormatterIntern((String)v.get(15)) );
                }
                if ( !allElementsEqual(newEKP, origEKPs) ){
                    return true;
                }
            }
        }
        if ( artikelFormular.preisVariabelBox.isEnabled() ){
            Boolean origVarPreis = originalVarPreisBools.get(0);
            if ( !origVarPreis.equals(artikelFormular.preisVariabelBox.isSelected()) ){
                return true;
            }
        }
        if ( artikelFormular.sortimentBox.isEnabled() ){
            Boolean origSortiment = (Boolean)originalData.get(0).get(7);
            if ( !origSortiment.equals(artikelFormular.sortimentBox.isSelected()) ){
                return true;
            }
        }
        if ( artikelFormular.lieferbarBox.isEnabled() ){
            Boolean origLieferbar = (Boolean)originalData.get(0).get(8);
            if ( !origLieferbar.equals(artikelFormular.lieferbarBox.isSelected()) ){
                return true;
            }
        }
        if ( artikelFormular.beliebtBox.isEnabled() ){
            Integer origBeliebtWert = (Integer)originalData.get(0).get(9);
            Integer newBeliebtWert =
                artikelFormular.beliebtWerte.get( artikelFormular.beliebtBox.getSelectedIndex() );
            if ( !origBeliebtWert.equals(newBeliebtWert) ){
                return true;
            }
        }
        if ( aktivBox.isEnabled() ){
            Boolean origAktiv = (Boolean)originalData.get(0).get(21);
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

    public int submit() {
        for (int i=0; i<originalData.size(); i++){
            String origLieferant = (String)originalData.get(i).get(1);
            Integer origLieferantID = originalLiefIDs.get(i);
            String origNummer = (String)originalData.get(i).get(2);
            /////////
            String newLieferant = origLieferant;
            Integer newLieferantID = origLieferantID;
            if (artikelFormular.lieferantBox.isEnabled()){
                int selIndex = artikelFormular.lieferantBox.getSelectedIndex();
                // -1 means "no selection done"
                if (selIndex != -1){
                    newLieferant = (String)artikelFormular.lieferantBox.getSelectedItem();
                    newLieferantID = artikelFormular.lieferantIDs.get(selIndex);
                }
            }
            ////////
            String newNummer = origNummer;
            if (artikelFormular.nummerField.isEnabled()){
                String str = artikelFormular.nummerField.getText();
                // "" means "no selection done"
                if (!str.equals("")){
                    newNummer = str;
                }
            }
            ////////
            if ( (!newLieferant.equals(origLieferant)) || (!newNummer.equals(origNummer)) ){
                if ( isItemAlreadyKnown(newLieferant, newNummer) ){
                    // not allowed: changing name and nummer to a pair that is already registered in DB
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Kombination Lieferant/Nummer bereits vorhanden! Wird zurückgesetzt.",
                            "Info", JOptionPane.INFORMATION_MESSAGE);
                    artikelFormular.lieferantBox.setSelectedIndex(
                            artikelFormular.lieferantIDs.indexOf(origLieferantID)
                            );
                    artikelFormular.nummerField.setText(origNummer);
                    return 1;
                }
            }
            ////////
            Integer produktgruppen_id = originalProdGrIDs.get(i);
            if (artikelFormular.produktgruppenBox.isEnabled()){
                int selIndex = artikelFormular.produktgruppenBox.getSelectedIndex();
                // -1 means "no selection done"
                if (selIndex != -1){
                    produktgruppen_id = artikelFormular.produktgruppenIDs.get(selIndex);
                }
            }
            ////////
            String origName = (String)originalData.get(i).get(3);
            String origKurzname = (String)originalData.get(i).get(4);
            ////////
            String newName = origName;
            if (artikelFormular.nameField.isEnabled()){
                String str = artikelFormular.nameField.getText();
                // "" means "no selection done"
                if (!str.equals("")){
                    newName = str;
                }
            }
            ////////
            String newKurzname = origKurzname;
            if (artikelFormular.kurznameField.isEnabled()){
                String str = artikelFormular.kurznameField.getText();
                // "" means "no selection done"
                if (!str.equals("")){
                    newKurzname = str;
                }
            }
            ////////
            BigDecimal menge = new BigDecimal( ((String)originalData.get(i).get(5)).replace(',', '.') );
            if (artikelFormular.mengeField.isEnabled()){
                try {
                    menge = new BigDecimal( artikelFormular.mengeField.getText().replace(',', '.') );
                } catch (NumberFormatException ex) {
                }
            }
            ////////
            String barcode = (String)originalData.get(i).get(10);
            if (artikelFormular.barcodeField.isEnabled()){
                String str = artikelFormular.barcodeField.getText();
                // "" means "no selection done"
                if (!str.equals("")){
                    barcode = str;
                }
            }
            ////////
            String herkunft = (String)originalData.get(i).get(17);
            if (artikelFormular.herkunftField.isEnabled()){
                String str = artikelFormular.herkunftField.getText();
                // "" means "no selection done"
                if (!str.equals("")){
                    herkunft = str;
                }
            }
            ////////
            Integer vpe = (Integer)originalData.get(i).get(11);
            if (artikelFormular.vpeSpinner.isEnabled()){
                try {
                    Integer newVPE = (Integer)artikelFormular.vpeSpinner.getValue();
                    // 0 means "not edited"
                    if (newVPE != 0){
                        vpe = newVPE;
                    }
                } catch (NumberFormatException ex) {
                }
            }
            ////////
            Integer setgroesse = (Integer)originalData.get(i).get(12);
            if (artikelFormular.setSpinner.isEnabled()){
                try {
                    Integer newSetgroesse = (Integer)artikelFormular.setSpinner.getValue();
                    // 0 means "not edited"
                    if (newSetgroesse != 0){
                        setgroesse = newSetgroesse;
                    }
                } catch (NumberFormatException ex) {
                }
            }
            ////////
            String vkpreis = (String)originalData.get(i).get(6);
            if (artikelFormular.vkpreisField.isEnabled()){
                String str = artikelFormular.vkpreisField.getText();
                // "" means "no selection done"
                if (!str.equals("")){
                    vkpreis = str;
                }
            }
            ////////
            String empfvkpreis = (String)originalData.get(i).get(13);
            if (artikelFormular.empfvkpreisField.isEnabled()){
                String str = artikelFormular.empfvkpreisField.getText();
                // "" means "no selection done"
                if (!str.equals("")){
                    empfvkpreis = str;
                }
            }
            ////////
            String ekrabatt = (String)originalData.get(i).get(14);
            if (artikelFormular.ekrabattField.isEnabled()){
                String str = artikelFormular.ekrabattField.getText();
                // "" means "no selection done"
                if (!str.equals("")){
                    ekrabatt = str;
                }
            }
            ////////
            String ekpreis = (String)originalData.get(i).get(15);
            if (artikelFormular.ekpreisField.isEnabled()){
                String str = artikelFormular.ekpreisField.getText();
                // "" means "no selection done"
                if (!str.equals("")){
                    ekpreis = str;
                }
            }
            ////////
            Boolean preisVar = artikelFormular.preisVariabelBox.isEnabled() ?
                artikelFormular.preisVariabelBox.isSelected() :
                (Boolean)originalVarPreisBools.get(i);
            ////////
            Boolean sortiment = artikelFormular.sortimentBox.isEnabled() ?
                artikelFormular.sortimentBox.isSelected() :
                (Boolean)originalData.get(i).get(7);
            ////////
            Boolean lieferbar = artikelFormular.lieferbarBox.isEnabled() ?
                artikelFormular.lieferbarBox.isSelected() :
                (Boolean)originalData.get(i).get(8);
            ////////
            Integer beliebtWert = artikelFormular.beliebtBox.isEnabled() ?
                artikelFormular.beliebtWerte.get(artikelFormular.beliebtBox.getSelectedIndex()) :
                (Integer)originalData.get(i).get(9);
            ////////
            Integer bestand; // just keep what it was
            try {
                bestand = Integer.parseInt( (String)originalData.get(i).get(18) );
            } catch (NumberFormatException ex){ bestand = null; }
            ////////
            Boolean aktiv = aktivBox.isEnabled() ?
                aktivBox.isSelected() :
                (Boolean)originalData.get(i).get(21);

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
                        newNummer, newName, newKurzname, menge, barcode, herkunft, vpe,
                        setgroesse, vkpreis, empfvkpreis, ekrabatt, ekpreis, 
                        preisVar, sortiment, lieferbar, beliebtWert, bestand);
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
        return 0;
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
	if (e.getSource() == submitButton){
            int result = submit();
            if (result == 0){
                artikelListe.updateAll();
                this.window.dispose(); // close
            }
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
        submitButton.setEnabled( isSubmittable() );
        super.actionPerformed(e);
    }
}
