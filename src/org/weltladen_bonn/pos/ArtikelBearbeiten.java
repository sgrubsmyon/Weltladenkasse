package org.weltladen_bonn.pos;

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
    protected Vector<Artikel> originalArticles;

    protected JCheckBox aktivBox;
    protected JButton submitButton;

    protected boolean updating = false; /** against the "Attempt to mutate in notification" */

    // Methoden:
    public ArtikelBearbeiten(Connection conn, MainWindowGrundlage mw, Artikelliste pw, JDialog dia,
            Vector<Artikel> origArties) {
	super(conn, mw, dia);
        artikelListe = pw;
        artikelFormular = new ArtikelFormular(conn, mw, true, true, true);
        originalArticles = new Vector<Artikel>(origArties);

        showAll();
    }

    public ArtikelFormular getArtikelFormular() {
        return artikelFormular;
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
        allPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    private void setOriginalValues() {
        Artikel firstArticle = originalArticles.get(0);

        if ( allArticlesEqualInAttribute("prodGrID") ){
            int prodGrIndex = artikelFormular.produktgruppenIDs.indexOf(firstArticle.getProdGrID());
            artikelFormular.produktgruppenBox.setSelectedIndex(prodGrIndex);
        } else {
            artikelFormular.produktgruppenBox.setSelectedIndex(-1);
        }
        if ( allArticlesEqualInAttribute("liefID") ){
            int liefIndex = artikelFormular.lieferantIDs.indexOf(firstArticle.getLiefID());
            artikelFormular.lieferantBox.setSelectedIndex(liefIndex);
        } else {
            artikelFormular.lieferantBox.setEnabled(false);
        }
        if ( allArticlesEqualInAttribute("nummer") ){
            artikelFormular.nummerField.setText(firstArticle.getNummer());
        } else {
            artikelFormular.nummerField.setEnabled(false);
        }
        if ( allArticlesEqualInAttribute("name") ){
            artikelFormular.nameField.setText(firstArticle.getName());
        } else {
            artikelFormular.nameField.setText("");
        }
        if ( allArticlesEqualInAttribute("kurzname") && firstArticle.getKurzname() != null ){
            artikelFormular.kurznameField.setText(firstArticle.getKurzname());
        } else {
            artikelFormular.kurznameField.setText("");
        }
        if ( allArticlesEqualInAttribute("menge") && firstArticle.getMenge() != null ){
            artikelFormular.mengeField.setText( bc.decimalMark(firstArticle.getMenge().toString()) );
        } else {
            artikelFormular.mengeField.setText("");
        }
        if ( allArticlesEqualInAttribute("einheit") && firstArticle.getEinheit() != null ){
            artikelFormular.einheitField.setText(firstArticle.getEinheit());
        } else {
            artikelFormular.einheitField.setText("");
        }
        if ( allArticlesEqualInAttribute("barcode") && firstArticle.getBarcode() != null ){
            artikelFormular.barcodeField.setText(firstArticle.getBarcode());
        } else {
            artikelFormular.barcodeField.setText("");
        }
        if ( allArticlesEqualInAttribute("herkunft") && firstArticle.getHerkunft() != null ){
            artikelFormular.herkunftField.setText(firstArticle.getHerkunft());
        } else {
            artikelFormular.herkunftField.setText("");
        }
        if ( allArticlesEqualInAttribute("vpe") && firstArticle.getVPE() != null ){
            artikelFormular.vpeSpinner.setValue(firstArticle.getVPE());
        } else {
            artikelFormular.vpeSpinner.setValue(0);
        }
        if ( allArticlesEqualInAttribute("setgroesse") ){
            artikelFormular.setSpinner.setValue(firstArticle.getSetgroesse());
        } else {
            artikelFormular.setSpinner.setValue(1);
        }
        if ( allArticlesEqualInAttribute("varPreis") ){
            artikelFormular.preisVariabelBox.setSelected(firstArticle.getVarPreis());
            if (!firstArticle.getVarPreis()){ // if all items have non-variable prices
                if ( allArticlesEqualInAttribute("vkp") && firstArticle.getVKP() != null ){
                    artikelFormular.vkpreisField.setText( bc.priceFormatter(firstArticle.getVKP()) );
                } else {
                    artikelFormular.vkpreisField.setText("");
                }
                if ( allArticlesEqualInAttribute("empfVKP") && firstArticle.getEmpfVKP() != null ){
                    artikelFormular.empfvkpreisField.setText( bc.priceFormatter(firstArticle.getEmpfVKP()) );
                } else {
                    artikelFormular.empfvkpreisField.setText("");
                }
                if ( allArticlesEqualInAttribute("ekRabatt") && firstArticle.getEKRabatt() != null ){
                    artikelFormular.ekrabattField.setText( bc.vatPercentRemover(firstArticle.getEKRabatt()) );
                } else {
                    artikelFormular.ekrabattField.setText("");
                }
                if ( allArticlesEqualInAttribute("ekp") && firstArticle.getEKP() != null ){
                    artikelFormular.ekpreisField.setText( bc.priceFormatter(firstArticle.getEKP()) );
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
        if ( allArticlesEqualInAttribute("sortiment") ){
            artikelFormular.sortimentBox.setSelected(firstArticle.getSortiment());
        } else {
            artikelFormular.sortimentBox.setEnabled(false);
        }
        if ( allArticlesEqualInAttribute("lieferbar") ){
            artikelFormular.lieferbarBox.setSelected(firstArticle.getLieferbar());
        } else {
            artikelFormular.lieferbarBox.setEnabled(false);
        }
        if ( allArticlesEqualInAttribute("beliebt") ){
            artikelFormular.beliebtBox.setSelectedIndex(
                    bc.beliebtWerte.indexOf(firstArticle.getBeliebt()));
        } else {
            artikelFormular.beliebtBox.setEnabled(false);
        }
        if ( allArticlesEqualInAttribute("aktiv") ){
            aktivBox.setSelected(firstArticle.getAktiv());
        } else {
            aktivBox.setEnabled(false);
        }

        artikelFormular.updateEKPreisField();
    }

    private boolean allArticlesEqualInAttribute(String attr) {
        Artikel article = originalArticles.get(0);
        for (Artikel a : originalArticles){
            if ( ! a.equalsInAttribute(attr, article) ){
                return false;
            }
        }
        return true;
    }

    // will data be lost on close?
    public boolean willDataBeLost() {
        for (int index=0; index < originalArticles.size(); index++){
            Artikel origArticle = originalArticles.get(index);
            Artikel newArticle = getNewArticle(origArticle);
            if ( !newArticle.equals(origArticle) ){
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


    private Artikel getNewArticle(Artikel origArticle) {
        Artikel a = new Artikel(bc);

        Integer newLieferantID = origArticle.getLiefID();
        if (artikelFormular.lieferantBox.isEnabled()){
            int selIndex = artikelFormular.lieferantBox.getSelectedIndex();
            // -1 means "no selection done"
            if (selIndex != -1){
                newLieferantID = artikelFormular.lieferantIDs.get(selIndex);
            }
        }
        a.setLiefID(newLieferantID);
        ////////
        String newNummer = origArticle.getNummer();
        if (artikelFormular.nummerField.isEnabled()){
            String str = artikelFormular.nummerField.getText();
            // "" means "no selection done"
            if (!str.equals("")){
                newNummer = str;
            }
        }
        a.setNummer(newNummer);
        ////////
        if ( (!newLieferantID.equals(origArticle.getLiefID())) || (!newNummer.equals(origArticle.getNummer())) ){
            if ( isArticleAlreadyKnown(newLieferantID, newNummer) ){
                // not allowed: changing name and nummer to a pair that is already registered in DB
                JOptionPane.showMessageDialog(this,
                        "Fehler: Kombination Lieferant/Nummer bereits vorhanden! Wird zurückgesetzt.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                artikelFormular.lieferantBox.setSelectedIndex(
                        artikelFormular.lieferantIDs.indexOf(origArticle.getLiefID())
                        );
                artikelFormular.nummerField.setText(origArticle.getNummer());
                return null;
            }
        }
        ////////
        Integer produktgruppen_id = origArticle.getProdGrID();
        if (artikelFormular.produktgruppenBox.isEnabled()){
            int selIndex = artikelFormular.produktgruppenBox.getSelectedIndex();
            // -1 means "no selection done"
            if (selIndex != -1){
                produktgruppen_id = artikelFormular.produktgruppenIDs.get(selIndex);
            }
        }
        a.setProdGrID(produktgruppen_id);
        ////////
        String newName = origArticle.getName();
        if (artikelFormular.nameField.isEnabled()){
            String str = artikelFormular.nameField.getText();
            // "" means "no selection done"
            if (!str.equals("")){
                newName = str;
            }
        }
        a.setName(newName);
        ////////
        String newKurzname = origArticle.getKurzname();
        if (artikelFormular.kurznameField.isEnabled()){
            String str = artikelFormular.kurznameField.getText();
            // "" means "no selection done"
            if (!str.equals("")){
                newKurzname = str;
            }
        }
        a.setKurzname(newKurzname);
        ////////
        BigDecimal menge = origArticle.getMenge();
        if (artikelFormular.mengeField.isEnabled()){
            try {
                menge = new BigDecimal( artikelFormular.mengeField.getText().replace(',', '.') );
            } catch (NumberFormatException ex) {
            }
        }
        a.setMenge(menge);
        ////////
        String einheit = origArticle.getEinheit();
        if (artikelFormular.einheitField.isEnabled()){
            String str = artikelFormular.einheitField.getText();
            // "" means "no selection done"
            if (!str.equals("")){
                einheit = str;
            }
        }
        a.setEinheit(einheit);
        ////////
        String barcode = origArticle.getBarcode();
        if (artikelFormular.barcodeField.isEnabled()){
            String str = artikelFormular.barcodeField.getText();
            // "" means "no selection done"
            if (!str.equals("")){
                barcode = str;
            }
        }
        a.setBarcode(barcode);
        ////////
        String herkunft = origArticle.getHerkunft();
        if (artikelFormular.herkunftField.isEnabled()){
            String str = artikelFormular.herkunftField.getText();
            // "" means "no selection done"
            if (!str.equals("")){
                herkunft = str;
            }
        }
        a.setHerkunft(herkunft);
        ////////
        Integer vpe = origArticle.getVPE();
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
        a.setVPE(vpe);
        ////////
        Integer setgroesse = origArticle.getSetgroesse();
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
        a.setSetgroesse(setgroesse);
        ////////
        String vkpreis = origArticle.getVKP();
        if (artikelFormular.vkpreisField.isEnabled()){
            String str = artikelFormular.vkpreisField.getText();
            // "" means "no selection done"
            if (!str.equals("")){
                vkpreis = str;
            }
        }
        a.setVKP(vkpreis);
        ////////
        String empfvkpreis = origArticle.getEmpfVKP();
        if (artikelFormular.empfvkpreisField.isEnabled()){
            String str = artikelFormular.empfvkpreisField.getText();
            // "" means "no selection done"
            if (!str.equals("")){
                empfvkpreis = str;
            }
        }
        a.setEmpfVKP(empfvkpreis);
        ////////
        String ekrabatt = origArticle.getEKRabatt();
        if (artikelFormular.ekrabattField.isEnabled()){
            String str = artikelFormular.ekrabattField.getText();
            // "" means "no selection done"
            if (!str.equals("")){
                ekrabatt = str;
            }
        }
        a.setEKRabatt(ekrabatt);
        ////////
        String ekpreis = origArticle.getEKP();
        if (artikelFormular.ekpreisField.isEnabled()){
            String str = artikelFormular.ekpreisField.getText();
            // "" means "no selection done"
            if (!str.equals("")){
                ekpreis = str;
            }
        }
        a.setEKP(ekpreis);
        ////////
        Boolean preisVar = artikelFormular.preisVariabelBox.isEnabled() ?
            artikelFormular.preisVariabelBox.isSelected() :
            origArticle.getVarPreis();
        a.setVarPreis(preisVar);
        ////////
        Boolean sortiment = artikelFormular.sortimentBox.isEnabled() ?
            artikelFormular.sortimentBox.isSelected() :
            origArticle.getSortiment();
        a.setSortiment(sortiment);
        ////////
        Boolean lieferbar = artikelFormular.lieferbarBox.isEnabled() ?
            artikelFormular.lieferbarBox.isSelected() :
            origArticle.getLieferbar();
        a.setLieferbar(lieferbar);
        ////////
        Integer beliebtWert = artikelFormular.beliebtBox.isEnabled() ?
            bc.beliebtWerte.get(artikelFormular.beliebtBox.getSelectedIndex()) :
            origArticle.getBeliebt();
        a.setBeliebt(beliebtWert);
        ////////
        Integer bestand = origArticle.getBestand(); // just keep what it was
        a.setBestand(bestand);
        ////////
        Boolean aktiv = aktivBox.isEnabled() ?
            aktivBox.isSelected() :
            origArticle.getAktiv();
        a.setAktiv(aktiv);

        return a;
    }


    public int submit() {
        for (int i=0; i < originalArticles.size(); i++){
            Artikel origArticle = originalArticles.get(i);
            Artikel newArticle = getNewArticle(origArticle);
            if (newArticle == null){
                return 1;
            }

            updateArticle(origArticle, newArticle);
        }
        return 0;
    }

    /** Needed for ChangeListener. */
    public void stateChanged(ChangeEvent e) {
        artikelFormular.updateEKPreisField();
        submitButton.setEnabled( isSubmittable() );
    }

    /** Needed for ItemListener. */
    public void itemStateChanged(ItemEvent e) {
        artikelFormular.itemStateChanged(e);
        artikelFormular.updateEKPreisField();
        submitButton.setEnabled( isSubmittable() );
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
                if (artikelListe != null){
                    artikelListe.updateAll();
                }
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
