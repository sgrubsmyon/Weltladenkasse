package WeltladenDB;

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

public class ArtikelNeuEingeben extends ArtikelDialogWindowGrundlage
    implements ArtikelNeuInterface, ArtikelFormularInterface, DocumentListener, ItemListener {
    // Attribute:
    protected ArtikelNeu artikelNeu;
    protected ArtikelFormular artikelFormular;
    protected UpdateTableFunctor utf;
    private Integer toplevel_id;
    private Integer sub_id;
    private Integer subsub_id;

    protected JButton hinzufuegenButton;
    protected JButton submitButton;
    protected JButton deleteButton;

    // Methoden:
    public ArtikelNeuEingeben(Connection conn, MainWindowGrundlage mw, Artikelliste pw, JDialog dia, Integer tid, Integer sid, Integer ssid) {
	super(conn, mw, pw, dia);
        this.toplevel_id = tid;
        this.sub_id = sid;
        this.subsub_id = ssid;
        utf = new UpdateTableFunctor() {
            public void updateTable() {
                artikelNeu.updateTable(allPanel);
            }
        };
        artikelNeu = new ArtikelNeu(conn, mw, utf);
        artikelFormular = new ArtikelFormular(conn, mw);
        showAll();
    }

    private String retrieveGruppenID() {
        String result = "";
        String subStr = this.sub_id == null ? "sub_id IS NULL" : "sub_id = ?";
        String subsubStr = this.subsub_id == null ? "subsub_id IS NULL" : "subsub_id = ";
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT produktgruppen_id FROM produktgruppe WHERE "+
                    "toplevel_id = ? AND "+subStr+" AND "+subsubStr+" AND aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, this.toplevel_id);
            int itemCounter = 2;
            if (this.sub_id != null){
                pstmtSetInteger(pstmt, itemCounter, this.sub_id);
                itemCounter++;
            }
            if (this.subsub_id != null){
                pstmtSetInteger(pstmt, itemCounter, this.subsub_id);
                itemCounter++;
            }
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            result = rs.getString(1);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    void showHeader() {
        headerPanel = new JPanel();
        artikelFormular.showHeader(headerPanel, allPanel);

        JPanel buttonPanel = new JPanel();
        hinzufuegenButton = new JButton("Hinzufügen");
        hinzufuegenButton.setMnemonic(KeyEvent.VK_H);
	hinzufuegenButton.addActionListener(this);
	hinzufuegenButton.setEnabled(false);
        buttonPanel.add(hinzufuegenButton);
        headerPanel.add(buttonPanel);

        String gruppenID = retrieveGruppenID();
        int prodGrIndex = artikelFormular.produktgruppenIDs.indexOf(gruppenID);
        artikelFormular.produktgruppenBox.setSelectedIndex(prodGrIndex);

        KeyAdapter enterAdapter = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
                    if (hinzufuegenButton.isEnabled()){
                        hinzufuegenButton.doClick();
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
        artikelNeu.showTable(allPanel);
    }

    void showFooter() {
        footerPanel = new JPanel();
        submitButton = new JButton("Abschicken");
        submitButton.setMnemonic(KeyEvent.VK_A);
        submitButton.addActionListener(this);
        if (artikelNeu.data.size() == 0){
            submitButton.setEnabled(false);
        } else {
            submitButton.setEnabled(true);
        }
        footerPanel.add(submitButton);
        deleteButton = new JButton("Verwerfen");
        deleteButton.setMnemonic(KeyEvent.VK_V);
        deleteButton.addActionListener(this);
        if (artikelNeu.data.size() == 0){
            deleteButton.setEnabled(false);
        } else {
            deleteButton.setEnabled(true);
        }
        footerPanel.add(deleteButton);
        closeButton = new JButton("Schließen");
        closeButton.setMnemonic(KeyEvent.VK_S);
        closeButton.addActionListener(this);
        if ( !willDataBeLost() ){
            closeButton.setEnabled(true);
        } else {
            closeButton.setEnabled(false);
        }
        footerPanel.add(closeButton);
        allPanel.add(footerPanel);
    }

    public void emptyTable() {
        artikelNeu.emptyTable();
    }

    public int checkIfItemAlreadyKnown(String name, String nummer) {
        return artikelNeu.checkIfItemAlreadyKnown(name, nummer);
    }

    protected int hinzufuegen() {
        String name = artikelFormular.nameField.getText();
        String nummer = artikelFormular.nummerField.getText();
        String barcodeDisplay = artikelFormular.barcodeField.getText();
        String barcode = barcodeDisplay;
        if (barcodeDisplay.length() == 0){ barcode = "NULL"; }
        int itemAlreadyKnown = checkIfItemAlreadyKnown(name, nummer);
        if (itemAlreadyKnown == 1){
            JOptionPane.showMessageDialog(this,
                    "Ein Artikel mit diesem Namen und dieser Nummer ist bereits in der Datenbank.\n" +
                    "Änderungen können in der Artikelliste vorgenommen werden.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return 1;
        }
        else if (itemAlreadyKnown == 2){
            JOptionPane.showMessageDialog(this,
                    "Ein Artikel mit diesem Namen und dieser Nummer ist bereits in der angezeigten Tabelle.\n" +
                    ""
                    //"Änderungen können in der Artikelliste vorgenommen werden."
                    ,
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return 1;
        }
        artikelNeu.artikelNamen.add(name);
        artikelNeu.artikelNummern.add(nummer);
        artikelNeu.barcodes.add(barcode);
        if (artikelFormular.preisVariabelBox.isSelected()){
            artikelNeu.vkPreise.add("NULL");
            artikelNeu.ekPreise.add("NULL");
            artikelNeu.variablePreise.add(true);
        } else {
            artikelNeu.vkPreise.add( priceFormatterIntern(artikelFormular.vkpreisField.getText()) );
            if ( artikelFormular.ekpreisField.getText().length() == 0 )
                artikelNeu.ekPreise.add("NULL");
            else
                artikelNeu.ekPreise.add( priceFormatterIntern(artikelFormular.ekpreisField.getText()) );
            artikelNeu.variablePreise.add(false);
        }
        int vpeInt = (Integer)artikelFormular.vpeSpinner.getValue();
        String vpe = vpeInt == 0 ? "NULL" : Integer.toString(vpeInt);
        artikelNeu.vpes.add(vpe);
        artikelNeu.selLieferantIDs.add( artikelFormular.lieferantIDs.get(artikelFormular.lieferantBox.getSelectedIndex()) );
        artikelNeu.selProduktgruppenIDs.add( artikelFormular.produktgruppenIDs.get(artikelFormular.produktgruppenBox.getSelectedIndex()) );
        artikelNeu.herkuenfte.add(artikelFormular.herkunftField.getText());
        artikelNeu.removeButtons.add(new JButton("-"));
        artikelNeu.removeButtons.lastElement().addActionListener(this);
        Vector<Color> colors = new Vector<Color>();
        colors.add(Color.black); // produktgruppe
        colors.add(Color.black); // name
        colors.add(Color.black); // nummer
        colors.add(Color.black); // barcode
        colors.add(Color.black); // vkpreis
        colors.add(Color.black); // ekpreis
        colors.add(Color.black); // variabel
        colors.add(Color.black); // vpe
        colors.add(Color.black); // lieferantid
        colors.add(Color.black); // herkunft
        colors.add(Color.black); // entfernen
        artikelNeu.colorMatrix.add(colors);

        Vector<Object> row = new Vector<Object>();
        row.add((String)artikelFormular.produktgruppenBox.getSelectedItem());
        row.add(name);
        row.add(artikelNeu.artikelNummern.lastElement());
        row.add(barcodeDisplay);
        String le = artikelNeu.vkPreise.lastElement(); row.add( le.equals("NULL") ? "" : le.replace('.',',')+" "+currencySymbol );
        le = artikelNeu.ekPreise.lastElement(); row.add( le.equals("NULL") ? "" : le.replace('.',',')+" "+currencySymbol );
        row.add( artikelNeu.variablePreise.lastElement() );
        row.add( vpe.equals("NULL") ? "" : vpe);
        row.add((String)artikelFormular.lieferantBox.getSelectedItem());
        row.add(artikelNeu.herkuenfte.lastElement());
        row.add(artikelNeu.removeButtons.lastElement());
        artikelNeu.data.add(row);
        return 0;
    }

    // will data be lost on close?
    public boolean willDataBeLost() {
        return artikelNeu.willDataBeLost();
    }

    public void fillComboBoxes() {
        artikelFormular.fillComboBoxes();
    }

    public boolean checkIfFormIsComplete() {
        return artikelFormular.checkIfFormIsComplete();
    }

    public void submit() {
        artikelNeu.submit();
    }

    /** Needed for ItemListener. */
    public void itemStateChanged(ItemEvent e) {
        artikelFormular.itemStateChanged(e);
        hinzufuegenButton.setEnabled( checkIfFormIsComplete() );
    }

    /**
     *    * Each non abstract class that implements the DocumentListener
     *      must have these methods.
     *
     *    @param e the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
	// check if form is valid (if item can be added to list)
        hinzufuegenButton.setEnabled( checkIfFormIsComplete() );
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
            submit();
            artikelListe.updateAll();
            emptyTable();
            utf.updateTable();
            return;
        }
	if (e.getSource() == deleteButton){
            emptyTable();
            utf.updateTable();
            return;
        }
	if (e.getSource() == artikelFormular.produktgruppenBox){
            hinzufuegenButton.setEnabled( checkIfFormIsComplete() );
            return;
        }
	if (e.getSource() == artikelFormular.lieferantBox){
            hinzufuegenButton.setEnabled( checkIfFormIsComplete() );
            return;
        }
	if (e.getSource() == hinzufuegenButton){
            int retVal = hinzufuegen();
            if (retVal == 0){ updateAll(); }
            return;
        }
        super.actionPerformed(e);
        artikelNeu.actionPerformed(e);
    }
}
