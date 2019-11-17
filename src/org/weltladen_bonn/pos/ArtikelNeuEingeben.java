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

public class ArtikelNeuEingeben extends DialogWindow
    implements ArtikelNeuInterface, ArtikelFormularInterface, DocumentListener, ItemListener, ChangeListener {
    // Attribute:
    protected Artikelliste artikelListe;
    protected ArtikelNeu artikelNeu;
    protected ArtikelFormular artikelFormular;
    private Integer toplevel_id = null;
    private Integer sub_id = null;
    private Integer subsub_id = null;
    private Integer produktgruppen_id = null;

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
        this.setPreferredSize(new Dimension(980, 600));
        showAll();
    }

    public ArtikelNeuEingeben(Connection conn, MainWindowGrundlage mw,
            Artikelliste pw, JDialog dia, Integer prodgrID) {
	super(conn, mw, dia);
        artikelListe = pw;
        produktgruppen_id = prodgrID;
        UpdateTableFunctor utf = new UpdateTableFunctor() {
            public void updateTable() {
                allPanel.remove(footerPanel);
                artikelNeu.updateTable(allPanel);
                showFooter();
            }
        };
        artikelNeu = new ArtikelNeu(conn, mw, utf);
        artikelFormular = new ArtikelFormular(conn, mw);
        this.setPreferredSize(new Dimension(980, 600));
        showAll();
    }

    private Integer retrieveGruppenID() {
        Integer result = 76; // default: Sonstiges Kunsthandwerk
        String subStr = this.sub_id == null ? "sub_id IS NULL" : "sub_id = ?";
        String subsubStr = this.subsub_id == null ? "subsub_id IS NULL" : "subsub_id = ?";
        System.out.println(this.sub_id + " " + this.subsub_id);
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
                result = 76; // default: Sonstiges Kunsthandwerk
            }
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        System.out.println(result);
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

      System.out.println(this.produktgruppen_id);
      if (this.produktgruppen_id == null) {
        this.produktgruppen_id = retrieveGruppenID();
      }
      System.out.println(this.produktgruppen_id);
      int prodGrIndex = artikelFormular.produktgruppenIDs.indexOf(this.produktgruppen_id);
      artikelFormular.produktgruppenBox.setSelectedIndex(prodGrIndex);

      artikelFormular.beliebtBox.setSelectedIndex(bc.beliebtNamen.indexOf("keine Angabe"));

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
      //artikelFormular.barcodeField.addKeyListener(enterAdapter); // scanner would always submit
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
          setEkpreisField(row);
          setEmpfvkpreisField(row);
          setEkrabattField(row);
        }
      }

      void setProduktgruppenBox(int row){
        int prodgrID = artikelNeu.articles.get(row).getProdGrID();
        int prodGrIndex = artikelFormular.produktgruppenIDs.indexOf(prodgrID);
        artikelFormular.produktgruppenBox.setSelectedIndex(prodGrIndex);
      }

      void setLieferantBox(int row){
        int liefID = artikelNeu.articles.get(row).getLiefID();
        int liefIndex = artikelFormular.lieferantIDs.indexOf(liefID);
        artikelFormular.lieferantBox.setSelectedIndex(liefIndex);
      }

      void setNummerField(int row){
        String nummer = artikelNeu.articles.get(row).getNummer();
        artikelFormular.nummerField.setText(nummer);
      }

      void setBarcodeField(int row){
        String barcode = artikelNeu.articles.get(row).getBarcode();
        artikelFormular.barcodeField.setText(barcode);
      }

      void setNameField(int row){
        String name = artikelNeu.articles.get(row).getName();
        artikelFormular.nameField.setText(name);
      }

      void setKurznameField(int row){
        String kurzname = artikelNeu.articles.get(row).getKurzname();
        artikelFormular.kurznameField.setText(kurzname);
      }

      void setMengeField(int row){
        if (artikelNeu.articles.get(row).getMenge() != null){
          String menge = bc.unifyDecimal( artikelNeu.articles.get(row).getMenge() );
          artikelFormular.mengeField.setText(menge);
        } else {
          artikelFormular.mengeField.setText("");
        }
      }

      void setEinheitField(int row){
        String einheit = artikelNeu.articles.get(row).getEinheit();
        artikelFormular.einheitField.setText(einheit);
      }

      void setHerkunftField(int row){
        String herkunft = artikelNeu.articles.get(row).getHerkunft();
        artikelFormular.herkunftField.setText(herkunft);
      }

      void setVPESpinner(int row){
        if (artikelNeu.articles.get(row).getVPE() != null){
          Integer vpe = artikelNeu.articles.get(row).getVPE();
          artikelFormular.vpeSpinner.setValue(vpe);
        } else {
          artikelFormular.vpeSpinner.setValue(0);
        }
      }

      void setSetSpinner(int row){
        Integer setgroesse = artikelNeu.articles.get(row).getSetgroesse();
        artikelFormular.setSpinner.setValue(setgroesse);
      }

      void setSortimentBox(int row){
        Boolean sortiment = artikelNeu.articles.get(row).getSortiment();
        artikelFormular.sortimentBox.setSelected(sortiment);
      }

      void setLieferbarBox(int row){
        Boolean lieferbar = artikelNeu.articles.get(row).getLieferbar();
        artikelFormular.lieferbarBox.setSelected(lieferbar);
      }

      void setBeliebtBox(int row){
        Integer beliebtWert = artikelNeu.articles.get(row).getBeliebt();
        Integer beliebtIndex = bc.beliebtWerte.indexOf(beliebtWert);
        artikelFormular.beliebtBox.setSelectedIndex(beliebtIndex);
      }

      void setVkpreisField(int row){
        String vkpreis = artikelNeu.articles.get(row).getVKP();
        artikelFormular.vkpreisField.setText(vkpreis);
      }

      void setEmpfvkpreisField(int row){
        String empfvkpreis = artikelNeu.articles.get(row).getEmpfVKP();
        artikelFormular.empfvkpreisField.setText(empfvkpreis);
      }

      void setEkrabattField(int row){
        String ekrabatt = bc.vatPercentRemover( artikelNeu.articles.get(row).getEKRabatt() );
        artikelFormular.ekrabattField.setText(ekrabatt);
      }

      void setEkpreisField(int row){
        String ekpreis = artikelNeu.articles.get(row).getEKP();
        artikelFormular.ekpreisField.setText(ekpreis);
      }

      void setPreisVariabelBox(int row){
        Boolean var = artikelNeu.articles.get(row).getVarPreis();
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
      allPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    protected void setOriginalValues(Artikel origArticle) {
      int prodGrIndex = artikelFormular.produktgruppenIDs.indexOf(origArticle.getProdGrID());
      artikelFormular.produktgruppenBox.setSelectedIndex(prodGrIndex);

      int liefIndex = artikelFormular.lieferantIDs.indexOf(origArticle.getLiefID());
      artikelFormular.lieferantBox.setSelectedIndex(liefIndex);

      artikelFormular.nummerField.setText(origArticle.getNummer());

      artikelFormular.nameField.setText(origArticle.getName());

      if ( origArticle.getKurzname() != null ){
        artikelFormular.kurznameField.setText(origArticle.getKurzname());
      } else {
        artikelFormular.kurznameField.setText("");
      }
      if ( origArticle.getMenge() != null ){
        artikelFormular.mengeField.setText( bc.decimalMark(origArticle.getMenge().toString()) );
      } else {
        artikelFormular.mengeField.setText("");
      }
      if ( origArticle.getEinheit() != null ){
        artikelFormular.einheitField.setText(origArticle.getEinheit());
      } else {
        artikelFormular.einheitField.setText("");
      }
      if ( origArticle.getBarcode() != null ){
        artikelFormular.barcodeField.setText(origArticle.getBarcode());
      } else {
        artikelFormular.barcodeField.setText("");
      }
      if ( origArticle.getHerkunft() != null ){
        artikelFormular.herkunftField.setText(origArticle.getHerkunft());
      } else {
        artikelFormular.herkunftField.setText("");
      }
      if ( origArticle.getVPE() != null ){
        artikelFormular.vpeSpinner.setValue(origArticle.getVPE());
      } else {
        artikelFormular.vpeSpinner.setValue(0);
      }

      artikelFormular.setSpinner.setValue(origArticle.getSetgroesse());

      artikelFormular.preisVariabelBox.setSelected(origArticle.getVarPreis());
      if (!origArticle.getVarPreis()){ // if non-variable price
        if ( origArticle.getVKP() != null ){
          artikelFormular.vkpreisField.setText( bc.priceFormatter(origArticle.getVKP()) );
        } else {
          artikelFormular.vkpreisField.setText("");
        }
        if ( origArticle.getEmpfVKP() != null ){
          artikelFormular.empfvkpreisField.setText( bc.priceFormatter(origArticle.getEmpfVKP()) );
        } else {
          artikelFormular.empfvkpreisField.setText("");
        }
        if ( origArticle.getEKRabatt() != null ){
          artikelFormular.ekrabattField.setText( bc.vatPercentRemover(origArticle.getEKRabatt()) );
        } else {
          artikelFormular.ekrabattField.setText("");
        }
        if ( origArticle.getEKP() != null ){
          artikelFormular.ekpreisField.setText( bc.priceFormatter(origArticle.getEKP()) );
        } else {
          artikelFormular.ekpreisField.setText("");
        }
      } else { // variable prices
        artikelFormular.vkpreisField.setEnabled(false);
        artikelFormular.empfvkpreisField.setEnabled(false);
        artikelFormular.ekrabattField.setEnabled(false);
        artikelFormular.ekpreisField.setEnabled(false);
      }

      artikelFormular.sortimentBox.setSelected(origArticle.getSortiment());

      artikelFormular.lieferbarBox.setSelected(origArticle.getLieferbar());

      artikelFormular.beliebtBox.setSelectedIndex(
      bc.beliebtWerte.indexOf(origArticle.getBeliebt()));

      artikelFormular.updateEKPreisField();
    }

    public void emptyTable() {
        artikelNeu.emptyTable();
    }

    public int checkIfArticleAlreadyKnown(Integer lieferant_id, String nummer) {
        return artikelNeu.checkIfArticleAlreadyKnown(lieferant_id, nummer);
    }

    protected int hinzufuegen() {
        Integer prodgrID = artikelFormular.produktgruppenIDs.get(
                artikelFormular.produktgruppenBox.getSelectedIndex() );
        Integer lieferantID =
            artikelFormular.lieferantIDs.get(artikelFormular.lieferantBox.getSelectedIndex());
        String lieferant = artikelFormular.lieferantBox.getSelectedItem().toString();
        String nummer = artikelFormular.nummerField.getText();
        int itemAlreadyKnown = checkIfArticleAlreadyKnown(lieferantID, nummer);
        if (artikelFormular.showArticleKnownWarning(itemAlreadyKnown)){
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
        Integer setgroesse = (Integer)artikelFormular.setSpinner.getValue();
        String vkpreis = artikelFormular.vkpreisField.getText();
        String empfvkpreis = artikelFormular.empfvkpreisField.getText();
        String ekrabatt = artikelFormular.ekrabattField.getText();
        String ekpreis = artikelFormular.ekpreisField.getText();
        Boolean var = artikelFormular.preisVariabelBox.isSelected();
        Boolean sortiment = artikelFormular.sortimentBox.isSelected();
        Boolean lieferbar = artikelFormular.lieferbarBox.isSelected();
        Integer beliebtWert = bc.beliebtWerte.get(artikelFormular.beliebtBox.getSelectedIndex());
        String beliebt = artikelFormular.beliebtBox.getSelectedItem().toString();

        Artikel newArticle = new Artikel(bc, prodgrID, lieferantID, nummer, name,
                kurzname, menge, einheit, barcode, herkunft, vpe, setgroesse,
                var ? "" : vkpreis,
                var ? "" : empfvkpreis,
                var ? "" : ekrabatt,
                var ? "" : bc.priceFormatter(figureOutEKP(empfvkpreis, ekrabatt, ekpreis)),
                var, sortiment, lieferbar, beliebtWert, null, true);
        artikelNeu.articles.add(newArticle);

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
        colors.add(Color.black); // setgroesse
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
            row.add(setgroesse);
            // prices:
                String vkp = bc.priceFormatter(artikelNeu.articles.lastElement().getVKP());
                if (vkp.length() > 0) vkp += " "+bc.currencySymbol;
                String empfvkp = bc.priceFormatter(artikelNeu.articles.lastElement().getEmpfVKP());
                if (empfvkp.length() > 0) empfvkp += " "+bc.currencySymbol;
                String rabatt = bc.vatFormatter( bc.vatParser(artikelNeu.articles.lastElement().getEKRabatt()) );
                String ekp = bc.priceFormatter(artikelNeu.articles.lastElement().getEKP());
                if (ekp.length() > 0) ekp += " "+bc.currencySymbol;
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
        if (e.getDocument() == artikelFormular.nummerField.getDocument()){
          // check if article already known
          Integer lieferantID =
            artikelFormular.lieferantIDs.get(artikelFormular.lieferantBox.getSelectedIndex());
          String nummer = artikelFormular.nummerField.getText();
          int itemAlreadyKnown = checkIfArticleAlreadyKnown(lieferantID, nummer);
          artikelFormular.showArticleKnownWarning(itemAlreadyKnown);
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
	if (e.getSource() == artikelFormular.lieferantBox){
          // check if article already known
          Integer lieferantID =
            artikelFormular.lieferantIDs.get(artikelFormular.lieferantBox.getSelectedIndex());
          String nummer = artikelFormular.nummerField.getText();
          int itemAlreadyKnown = checkIfArticleAlreadyKnown(lieferantID, nummer);
          artikelFormular.showArticleKnownWarning(itemAlreadyKnown);
          return;
        }
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
