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

public class LieferantNeuEingeben extends ArtikelDialogWindowGrundlage
    implements ArtikelNeuInterface, ArtikelFormularInterface, DocumentListener, ItemListener, ChangeListener {
    // Attribute:
    protected ArtikelNeu artikelNeu;
    protected ArtikelFormular artikelFormular;
    private Integer toplevel_id;
    private Integer sub_id;
    private Integer subsub_id;

    protected JButton hinzufuegenButton;
    protected JButton submitButton;
    protected JButton deleteButton;

    // Methoden:
    public LieferantNeuEingeben(Connection conn, MainWindowGrundlage mw, Artikelliste pw, JDialog dia, Integer tid, Integer sid, Integer ssid) {
	super(conn, mw, pw, dia);
        this.toplevel_id = tid;
        this.sub_id = sid;
        this.subsub_id = ssid;
        UpdateTableFunctor utf = new UpdateTableFunctor() {
            public void updateTable() {
                artikelNeu.updateTable(allPanel);
            }
        };
        artikelNeu = new ArtikelNeu(conn, mw, utf);
        artikelFormular = new ArtikelFormular(conn, mw);
        showAll();
    }

    private Integer retrieveGruppenID() {
        Integer result = 11; // default: Sonstiges, 19% MwSt
        String subStr = this.sub_id == null ? "sub_id IS NULL" : "sub_id = ?";
        String subsubStr = this.subsub_id == null ? "subsub_id IS NULL" : "subsub_id = ?";
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
            try {
                result = rs.getInt(1);
            } catch (SQLException ex) {
                result = 11; // default: Sonstiges, 19% MwSt
            }
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

        Integer gruppenID = retrieveGruppenID();
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

    public int checkIfItemAlreadyKnown(String lieferant, String nummer) {
        return artikelNeu.checkIfItemAlreadyKnown(lieferant, nummer);
    }

    protected int hinzufuegen() {
        Integer prodgrID = artikelFormular.produktgruppenIDs.get( artikelFormular.produktgruppenBox.getSelectedIndex() );
        Integer lieferantID = artikelFormular.lieferantIDs.get(artikelFormular.lieferantBox.getSelectedIndex());
        String lieferant = artikelFormular.lieferantBox.getSelectedItem().toString();
        String nummer = artikelFormular.nummerField.getText();
        int itemAlreadyKnown = checkIfItemAlreadyKnown(lieferant, nummer);
        if (itemAlreadyKnown == 1){
            JOptionPane.showMessageDialog(this,
                    "Ein Artikel mit diesem Lieferant und dieser Nummer ist bereits in der Datenbank.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return 1;
        }
        else if (itemAlreadyKnown == 2){
            JOptionPane.showMessageDialog(this,
                    "Ein Artikel mit diesem Lieferant und dieser Nummer ist bereits in der angezeigten Tabelle.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return 1;
        }
        String name = artikelFormular.nameField.getText();
        BigDecimal menge;
        try {
            menge = new BigDecimal(artikelFormular.mengeField.getText().replace(',','.'));
        } catch (NumberFormatException ex) {
            menge = null;
        }
        String barcode = artikelFormular.barcodeField.getText();
        String herkunft = artikelFormular.herkunftField.getText();
        Integer vpe = (Integer)artikelFormular.vpeSpinner.getValue();
        vpe = vpe == 0 ? null : vpe;
        String vkpreis = artikelFormular.vkpreisField.getText();
        String ekpreis = artikelFormular.ekpreisField.getText();
        Boolean var = artikelFormular.preisVariabelBox.isSelected();
        Boolean sortiment = artikelFormular.sortimentBox.isSelected();

        artikelNeu.selProduktgruppenIDs.add(prodgrID);
        artikelNeu.selLieferantIDs.add(lieferantID);
        artikelNeu.lieferanten.add(lieferant);
        artikelNeu.artikelNummern.add(nummer);
        artikelNeu.artikelNamen.add(name);
        artikelNeu.mengen.add(menge);
        artikelNeu.barcodes.add(barcode.length() == 0 ? "NULL" : barcode);
        artikelNeu.herkuenfte.add(herkunft);
        artikelNeu.vpes.add(vpe);
        if (var){
            artikelNeu.variablePreise.add(true);
            artikelNeu.vkPreise.add("NULL");
            artikelNeu.ekPreise.add("NULL");
        } else {
            artikelNeu.variablePreise.add(false);
            artikelNeu.vkPreise.add( priceFormatterIntern(vkpreis) );
            if ( ekpreis.length() == 0 )
                artikelNeu.ekPreise.add("NULL");
            else
                artikelNeu.ekPreise.add( priceFormatterIntern(ekpreis) );
        }
        artikelNeu.sortimente.add(sortiment);
        artikelNeu.removeButtons.add(new JButton("-"));
        artikelNeu.removeButtons.lastElement().addActionListener(this);
        Vector<Color> colors = new Vector<Color>();
        colors.add(Color.black); // produktgruppe
        colors.add(Color.black); // lieferant
        colors.add(Color.black); // nummer
        colors.add(Color.black); // name
        colors.add(Color.black); // menge
        colors.add(Color.black); // barcode
        colors.add(Color.black); // herkunft
        colors.add(Color.black); // vpe
        colors.add(Color.black); // vkpreis
        colors.add(Color.black); // ekpreis
        colors.add(Color.black); // variabel
        colors.add(Color.black); // sortiment
        colors.add(Color.black); // entfernen
        artikelNeu.colorMatrix.add(colors);

        Vector<Object> row = new Vector<Object>();
            row.add( artikelFormular.produktgruppenBox.getSelectedItem().toString() );
            row.add(lieferant);
            row.add(nummer);
            row.add(name);
            row.add( menge == null ? "" : menge.toString() );
            row.add(barcode);
            row.add(herkunft);
            row.add( vpe == null ? "" : vpe.toString() );
                String vkp = priceFormatter(vkpreis);
                if (vkp.length() > 0) vkp += " "+currencySymbol;
                String ekp = priceFormatter(ekpreis);
                if (ekp.length() > 0) ekp += " "+currencySymbol;
            row.add(vkp);
            row.add(ekp);
            row.add(var);
            row.add(sortiment);
            row.add( artikelNeu.removeButtons.lastElement() );
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

    public int submit() {
        return artikelNeu.submit();
    }

    /** Needed for ChangeListener. */
    public void stateChanged(ChangeEvent e) {
        hinzufuegenButton.setEnabled( checkIfFormIsComplete() );
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
            updateAll();
            return;
        }
	if (e.getSource() == deleteButton){
            emptyTable();
            updateAll();
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
