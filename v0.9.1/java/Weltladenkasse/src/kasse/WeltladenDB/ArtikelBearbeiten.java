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
    protected Vector< Vector<Object> > originalData;
    protected Vector<Object> originalProdGrIDs;
    protected Vector<Object> originalLiefIDs;
    protected Vector<Object> originalVarPreisBools;

    protected JButton submitButton;

    private CurrencyDocumentFilter geldFilter = new CurrencyDocumentFilter();

    // Methoden:
    public ArtikelBearbeiten(Connection conn, MainWindowGrundlage mw, Artikelliste pw, JDialog dia,
            Vector< Vector<Object> > origData,
            Vector<String> origPrGrIDs,
            Vector<String> origLiefIDs,
            Vector<Boolean> origVPBools) {
	super(conn, mw, pw, dia);
        artikelFormular = new ArtikelFormular(conn, mw, "", "", "");
        originalData = new Vector< Vector<Object> >(origData);
        originalProdGrIDs = new Vector<Object>(origPrGrIDs);
        originalLiefIDs = new Vector<Object>(origLiefIDs);
        originalVarPreisBools = new Vector<Object>(origVPBools);
        showAll();
    }

    void showHeader() {
        headerPanel = new JPanel();
        artikelFormular.showHeader(headerPanel, allPanel);

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
        submitButton.setEnabled( checkIfFormIsComplete() && willDataBeLost() );
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
        String firstGruppenID = (String)originalProdGrIDs.get(0);
        Boolean firstVarPreis = (Boolean)originalVarPreisBools.get(0);
        String firstVKP = (String)originalData.get(0).get(4);
        String firstEKP = (String)originalData.get(0).get(5);
        String firstVPE = (String)originalData.get(0).get(6);
        String firstLieferantID = (String)originalLiefIDs.get(0);
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
            }
        } else {
            artikelFormular.vkpreisField.setEnabled(false);
            artikelFormular.ekpreisField.setEnabled(false);
            artikelFormular.preisVariabelBox.setEnabled(false);
        }
        if ( allRowsEqual(firstVPE, 6) ){
            if ( firstVPE.equals("") ){ firstVPE = "0"; }
            artikelFormular.vpeField.setText(firstVPE);
        } else {
            artikelFormular.vpeField.setEnabled(false);
        }
        if ( allElementsEqual(firstLieferantID, originalLiefIDs) ){
            liefIndex = artikelFormular.lieferantIDs.indexOf(firstLieferantID);
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
            aktivCheckBox.setText(firstAktiv);
        } else {
            artikelFormular.nameField.setEnabled(false);
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

    private boolean allElementsEqual(Object element, Vector<Object> vector) {
        for (Object elem : vector){
            if ( ! elem.equals(element) ){
                return false;
            }
        }
        return true;
    }

    // will data be lost on close?
    public boolean willDataBeLost() {
        return false;
    }

    public void fillComboBoxes() {
        artikelFormular.fillComboBoxes();
    }

    public boolean checkIfFormIsComplete() {
        return artikelFormular.checkIfFormIsComplete();
    }

    public void submit() {
    }

    /** Needed for ItemListener. */
    public void itemStateChanged(ItemEvent e) {
        artikelFormular.itemStateChanged(e);
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
            // close
            return;
        }
        super.actionPerformed(e);
    }
}
