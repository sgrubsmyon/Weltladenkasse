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

public class ArtikelNeuEingeben extends DialogWindow
    implements ArtikelNeuInterface, ArtikelFormularInterface, DocumentListener, ItemListener, ChangeListener {
    // Attribute:
    protected Artikelliste artikelListe;
    protected ArtikelNeu artikelNeu;
    protected ArtikelFormular artikelFormular;
    private Integer toplevel_id;
    private Integer sub_id;
    private Integer subsub_id;

    protected JButton hinzufuegenButton;
    protected JButton submitButton;
    protected JButton deleteButton;

    protected boolean updating = false; /** against the "Attempt to mutate in notification" */

    // Methoden:
    public ArtikelNeuEingeben(Connection conn, MainWindowGrundlage mw,
            Artikelliste pw, JDialog dia, Integer tid, Integer sid, Integer ssid) {
	super(conn, mw, dia);
        artikelListe = pw;
        toplevel_id = tid;
        sub_id = sid;
        subsub_id = ssid;
        UpdateTableFunctor utf = new UpdateTableFunctor() {
            public void updateTable() {
                allPanel.remove(footerPanel);
                artikelNeu.updateTable(allPanel);
                showFooter();
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

    protected void showHeader() {
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

        artikelFormular.beliebtBox.setSelectedIndex(artikelFormular.beliebtNamen.indexOf("keine Angabe"));

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
        artikelFormular.kurznameField.getDocument().addDocumentListener(this);
        artikelFormular.kurznameField.addKeyListener(enterAdapter);
        artikelFormular.mengeField.getDocument().addDocumentListener(this);
        artikelFormular.mengeField.addKeyListener(enterAdapter);
        artikelFormular.einheitField.getDocument().addDocumentListener(this);
        artikelFormular.einheitField.addKeyListener(enterAdapter);
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
    }

    protected void showMiddle() {
        artikelNeu.showTable(allPanel);
        artikelNeu.myTable.getSelectionModel().addListSelectionListener(new RowSelectListener());
    }

    private class RowSelectListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int[] selRows = artikelNeu.myTable.getSelectedRows();
            if ( selRows.length == 1 ){
                int row = artikelNeu.myTable.convertRowIndexToModel(selRows[0]); // user might have changed row order

                // first row:
                setProduktgruppenBox(row);
                setLieferantBox(row);
                setNummerField(row);
                setBarcodeField(row);

                // second row:
                setNameField(row);
                setKurznameField(row);
                setMengeField(row);
                setEinheitField(row);
                setHerkunftField(row);

                // third row:
                setVPESpinner(row);
                setSetSpinner(row);
                setSortimentBox(row);
                setLieferbarBox(row);
                setBeliebtBox(row);

                // fourth row:
                setPreisVariabelBox(row);
                setVkpreisField(row);
                String empfvkpreis = artikelNeu.empfvkPreise.get(row);
                String ekrabatt = artikelNeu.ekRabatte.get(row);
                if ( empfVKPAndEKPValid(empfvkpreis, ekrabatt) ){
                    setEmpfvkpreisField(row);
                    setEkrabattField(row);
                } else {
                    setEkpreisField(row);
                }
            }
        }

        void setProduktgruppenBox(int row){
            int prodgrID = artikelNeu.selProduktgruppenIDs.get(row);
            int prodGrIndex = artikelFormular.produktgruppenIDs.indexOf(prodgrID);
            artikelFormular.produktgruppenBox.setSelectedIndex(prodGrIndex);
        }

        void setLieferantBox(int row){
            int liefID = artikelNeu.selLieferantIDs.get(row);
            int liefIndex = artikelFormular.lieferantIDs.indexOf(liefID);
            artikelFormular.lieferantBox.setSelectedIndex(liefIndex);
        }

        void setNummerField(int row){
            String nummer = artikelNeu.artikelNummern.get(row);
            artikelFormular.nummerField.setText(nummer);
        }

        void setBarcodeField(int row){
            String barcode = artikelNeu.barcodes.get(row);
            artikelFormular.barcodeField.setText(barcode);
        }

        void setNameField(int row){
            String name = artikelNeu.artikelNamen.get(row);
            artikelFormular.nameField.setText(name);
        }

        void setKurznameField(int row){
            String kurzname = artikelNeu.kurznamen.get(row);
            artikelFormular.kurznameField.setText(kurzname);
        }

        void setMengeField(int row){
            if (artikelNeu.mengen.get(row) != null){
                String menge = unifyDecimal( artikelNeu.mengen.get(row) );
                artikelFormular.mengeField.setText(menge);
            } else {
                artikelFormular.mengeField.setText("");
            }
        }

        void setEinheitField(int row){
            String einheit = artikelNeu.einheiten.get(row);
            artikelFormular.einheitField.setText(einheit);
        }

        void setHerkunftField(int row){
            String herkunft = artikelNeu.herkuenfte.get(row);
            artikelFormular.herkunftField.setText(herkunft);
        }

        void setVPESpinner(int row){
            if (artikelNeu.vpes.get(row) != null){
                Integer vpe = artikelNeu.vpes.get(row);
                artikelFormular.vpeSpinner.setValue(vpe);
            } else {
                artikelFormular.vpeSpinner.setValue(0);
            }
        }

        void setSetSpinner(int row){
            Integer setgroesse = artikelNeu.sets.get(row);
            artikelFormular.setSpinner.setValue(setgroesse);
        }

        void setSortimentBox(int row){
            Boolean sortiment = artikelNeu.sortimente.get(row);
            artikelFormular.sortimentBox.setSelected(sortiment);
        }

        void setLieferbarBox(int row){
            Boolean lieferbar = artikelNeu.lieferbarBools.get(row);
            artikelFormular.lieferbarBox.setSelected(lieferbar);
        }

        void setBeliebtBox(int row){
            Integer beliebtWert = artikelNeu.beliebtWerte.get(row);
            Integer beliebtIndex = artikelFormular.beliebtWerte.indexOf(beliebtWert);
            artikelFormular.beliebtBox.setSelectedIndex(beliebtIndex);
        }

        void setVkpreisField(int row){
            String vkpreis = artikelNeu.vkPreise.get(row);
            artikelFormular.vkpreisField.setText(vkpreis);
        }

        void setEmpfvkpreisField(int row){
            String empfvkpreis = artikelNeu.empfvkPreise.get(row);
            artikelFormular.empfvkpreisField.setText(empfvkpreis);
        }

        void setEkrabattField(int row){
            String ekrabatt = artikelNeu.ekRabatte.get(row);
            artikelFormular.ekrabattField.setText(ekrabatt);
        }

        void setEkpreisField(int row){
            String ekpreis = artikelNeu.ekPreise.get(row);
            artikelFormular.ekpreisField.setText(ekpreis);
        }

        void setPreisVariabelBox(int row){
            Boolean var = artikelNeu.variablePreise.get(row);
            artikelFormular.preisVariabelBox.setSelected(var);
        }
    }

    protected void showFooter() {
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
        Integer prodgrID = artikelFormular.produktgruppenIDs.get(
                artikelFormular.produktgruppenBox.getSelectedIndex() );
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
        String kurzname = artikelFormular.kurznameField.getText();
        BigDecimal menge;
        try {
            menge = new BigDecimal(artikelFormular.mengeField.getText().replace(',','.'));
        } catch (NumberFormatException ex) {
            menge = null;
        }
        String einheit = artikelFormular.einheitField.getText();
        String barcode = artikelFormular.barcodeField.getText();
        String herkunft = artikelFormular.herkunftField.getText();
        Integer vpe = (Integer)artikelFormular.vpeSpinner.getValue();
        vpe = vpe == 0 ? null : vpe;
        Integer set = (Integer)artikelFormular.setSpinner.getValue();
        String vkpreis = artikelFormular.vkpreisField.getText();
        String empfvkpreis = artikelFormular.empfvkpreisField.getText();
        String ekrabatt = artikelFormular.ekrabattField.getText();
        String ekpreis = artikelFormular.ekpreisField.getText();
        Boolean var = artikelFormular.preisVariabelBox.isSelected();
        Boolean sortiment = artikelFormular.sortimentBox.isSelected();
        Boolean lieferbar = artikelFormular.lieferbarBox.isSelected();
        Integer beliebtWert = artikelFormular.beliebtWerte.get(artikelFormular.beliebtBox.getSelectedIndex());
        String beliebt = artikelFormular.beliebtBox.getSelectedItem().toString();

        artikelNeu.selProduktgruppenIDs.add(prodgrID);
        artikelNeu.selLieferantIDs.add(lieferantID);
        artikelNeu.lieferanten.add(lieferant);
        artikelNeu.artikelNummern.add(nummer);
        artikelNeu.artikelNamen.add(name);
        artikelNeu.kurznamen.add(kurzname);
        artikelNeu.mengen.add(menge);
        artikelNeu.einheiten.add(einheit);
        artikelNeu.barcodes.add(barcode);
        artikelNeu.herkuenfte.add(herkunft);
        artikelNeu.vpes.add(vpe);
        artikelNeu.sets.add(set);
        if (var){
            artikelNeu.variablePreise.add(true);
            artikelNeu.vkPreise.add("");
            artikelNeu.empfvkPreise.add("");
            artikelNeu.ekRabatte.add("");
            artikelNeu.ekPreise.add("");
        } else {
            artikelNeu.variablePreise.add(false);
            artikelNeu.vkPreise.add(vkpreis);
            artikelNeu.empfvkPreise.add(empfvkpreis);
            artikelNeu.ekRabatte.add(ekrabatt);
            artikelNeu.ekPreise.add( priceFormatter(figureOutEKP(empfvkpreis, ekrabatt, ekpreis)) );
        }
        artikelNeu.sortimente.add(sortiment);
        artikelNeu.lieferbarBools.add(lieferbar);
        artikelNeu.beliebtWerte.add(beliebtWert);
        artikelNeu.bestaende.add(null);
        artikelNeu.removeButtons.add(new JButton("-"));
        artikelNeu.removeButtons.lastElement().addActionListener(this);

        Vector<Color> colors = new Vector<Color>();
        colors.add(Color.black); // produktgruppe
        colors.add(Color.black); // lieferant
        colors.add(Color.black); // nummer
        colors.add(Color.black); // name
        colors.add(Color.black); // kurzname
        colors.add(Color.black); // menge
        colors.add(Color.black); // einheit
        colors.add(Color.black); // sortiment
        colors.add(Color.black); // lieferbar
        colors.add(Color.black); // beliebt
        colors.add(Color.black); // barcode
        colors.add(Color.black); // vpe
        colors.add(Color.black); // set
        colors.add(Color.black); // vkpreis
        colors.add(Color.black); // empfvkpreis
        colors.add(Color.black); // ekrabatt
        colors.add(Color.black); // ekpreis
        colors.add(Color.black); // variabel
        colors.add(Color.black); // herkunft
        colors.add(Color.black); // bestand
        colors.add(Color.black); // entfernen
        artikelNeu.colorMatrix.add(colors);

        Vector<Object> row = new Vector<Object>();
            row.add( artikelFormular.produktgruppenBox.getSelectedItem().toString() );
            row.add(lieferant);
            row.add(nummer);
            row.add(name);
            row.add(kurzname);
            row.add( menge == null ? "" : menge.toString() );
            row.add(einheit);
            row.add(sortiment);
            row.add(lieferbar);
            row.add(beliebt);
            row.add(barcode);
            row.add( vpe == null ? "" : vpe.toString() );
            row.add(set);
            // prices:
                String vkp = priceFormatter(artikelNeu.vkPreise.lastElement());
                if (vkp.length() > 0) vkp += " "+currencySymbol;
                String empfvkp = priceFormatter(artikelNeu.empfvkPreise.lastElement());
                if (empfvkp.length() > 0) empfvkp += " "+currencySymbol;
                String rabatt = vatFormatter( vatParser(artikelNeu.ekRabatte.lastElement()) );
                String ekp = priceFormatter(artikelNeu.ekPreise.lastElement());
                if (ekp.length() > 0) ekp += " "+currencySymbol;
                row.add(vkp);
                row.add(empfvkp);
                row.add(rabatt);
                row.add(ekp);
            row.add(var);
            row.add(herkunft);
            row.add(null); // bestand
            row.add( artikelNeu.removeButtons.lastElement() );
        artikelNeu.data.add(row);
        return 0;
    }

    // will data be lost on close?
    protected boolean willDataBeLost() {
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
        artikelFormular.updateEKPreisField();
        hinzufuegenButton.setEnabled( checkIfFormIsComplete() );
    }

    /** Needed for ItemListener. */
    public void itemStateChanged(ItemEvent e) {
        artikelFormular.itemStateChanged(e);
        artikelFormular.updateEKPreisField();
        hinzufuegenButton.setEnabled( checkIfFormIsComplete() );
    }

    /**
     *    * Each non abstract class that implements the DocumentListener
     *      must have these methods.
     *
     *    @param e the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
        if (updating){
            return; // tackle the "Attempt to mutate in notification"
        }
        // don't let the ekpreisField update itself:
        if (e.getDocument() != artikelFormular.ekpreisField.getDocument()){
            updating = true;
            artikelFormular.updateEKPreisField();
            updating = false;
        } else {
            // clear the other price boxes
            artikelFormular.empfvkpreisField.setText("");
            artikelFormular.ekrabattField.setText("");
        }
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
	if (e.getSource() == hinzufuegenButton){
            int retVal = hinzufuegen();
            if (retVal == 0){ updateAll(); }
            return;
        }
        hinzufuegenButton.setEnabled( checkIfFormIsComplete() );
        super.actionPerformed(e);
        artikelNeu.actionPerformed(e);
    }
}
