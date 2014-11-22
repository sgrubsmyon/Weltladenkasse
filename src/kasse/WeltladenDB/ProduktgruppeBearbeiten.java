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
//import javax.swing.table.*;
import javax.swing.event.*; // for DocumentListener
import javax.swing.text.*; // for DocumentFilter

public class ProduktgruppeBearbeiten extends DialogWindow
    implements ProduktgruppeFormularInterface, DocumentListener, ItemListener {
    // Attribute:
    protected Produktgruppenliste produktgruppenListe;
    protected ProduktgruppeFormular produktgruppeFormular;
    protected Vector< Vector<Object> > originalData;
    protected Vector<Integer> originalProdGrIDs;
    protected Vector<Integer> originalParentProdGrIDs;
    protected Vector<Integer> originalMwStIDs;
    protected Vector<Integer> originalPfandIDs;

    protected JCheckBox aktivBox;
    protected JButton submitButton;

    // Methoden:
    public ProduktgruppeBearbeiten(Connection conn, MainWindowGrundlage mw, Produktgruppenliste pw, JDialog dia,
            Vector< Vector<Object> > origData,
            Vector<Integer> origProdGrIDs,
            Vector<Integer> origMwStIDs,
            Vector<Integer> origPfandIDs) {
	super(conn, mw, dia);
        produktgruppenListe = pw;
        produktgruppeFormular = new ProduktgruppeFormular(conn, mw);
        originalData = new Vector< Vector<Object> >(origData);
        originalProdGrIDs = new Vector<Integer>(origProdGrIDs);
        originalParentProdGrIDs = findParentProdGrIDs(origProdGrIDs);
        originalMwStIDs = new Vector<Integer>(origMwStIDs);
        originalPfandIDs = new Vector<Integer>(origPfandIDs);
        showAll();
    }

    private Vector<Integer> findParentProdGrIDs(Vector<Integer> prodGrIDs) {
        Vector<Integer> ppgIDs = new Vector<Integer>();
        for (Integer id : prodGrIDs){
            try {
                // get topid, subid, subsubid
                PreparedStatement pstmt = this.conn.prepareStatement(
                        "SELECT toplevel_id FROM produktgruppe WHERE produktgruppen_id = ?"
                        );
                pstmtSetInteger(pstmt, 1, id);
                ResultSet rs = pstmt.executeQuery();
                rs.next();
                Integer topid = rs.getString(1) != null ? rs.getInt(1) : null;
                rs.close();
                //
                pstmt = this.conn.prepareStatement(
                        "SELECT sub_id FROM produktgruppe WHERE produktgruppen_id = ?"
                        );
                pstmtSetInteger(pstmt, 1, id);
                rs = pstmt.executeQuery();
                rs.next();
                Integer subid = rs.getString(1) != null ? rs.getInt(1) : null;
                rs.close();
                //
                pstmt = this.conn.prepareStatement(
                        "SELECT subsub_id FROM produktgruppe WHERE produktgruppen_id = ?"
                        );
                pstmtSetInteger(pstmt, 1, id);
                rs = pstmt.executeQuery();
                rs.next();
                Integer subsubid = rs.getString(1) != null ? rs.getInt(1) : null;
                rs.close();
                // get the parent group's id
                String query;
                if (subsubid != null){
                    pstmt = this.conn.prepareStatement(
                            "SELECT produktgruppen_id FROM produktgruppe "+
                            "WHERE toplevel_id = ? AND sub_id = ? AND subsub_id IS NULL"
                            );
                    pstmtSetInteger(pstmt, 1, topid);
                    pstmtSetInteger(pstmt, 2, subid);
                    rs = pstmt.executeQuery();
                    rs.next(); ppgIDs.add(rs.getInt(1)); rs.close();
                    pstmt.close();
                } else if (subid != null){
                    pstmt = this.conn.prepareStatement(
                            "SELECT produktgruppen_id FROM produktgruppe "+
                            "WHERE toplevel_id = ? AND sub_id IS NULL AND subsub_id IS NULL"
                            );
                    pstmtSetInteger(pstmt, 1, topid);
                    rs = pstmt.executeQuery();
                    rs.next(); ppgIDs.add(rs.getInt(1)); rs.close();
                    pstmt.close();
                } else {
                    // has no parent, is at top level
                    ppgIDs.add(null);
                }
            } catch (SQLException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        return ppgIDs;
    }

    protected void showHeader() {
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

        produktgruppeFormular.parentProdGrBox.addActionListener(this);
        produktgruppeFormular.nameField.addKeyListener(enterAdapter);
        produktgruppeFormular.mwstBox.addActionListener(this);
        produktgruppeFormular.pfandBox.addActionListener(this);
        produktgruppeFormular.nameField.getDocument().addDocumentListener(this);
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
        Integer firstParentProdGrID = originalParentProdGrIDs.get(0);
        String firstName = (String)originalData.get(0).get(1);
        Integer firstMwStID = originalMwStIDs.get(0);
        Integer firstPfandID = originalPfandIDs.get(0);
        Boolean firstAktiv = (Boolean)originalData.get(0).get(5);

        if ( allElementsEqual(firstParentProdGrID, originalParentProdGrIDs) ){
            int index = produktgruppeFormular.parentProdGrIDs.indexOf(firstParentProdGrID);
            produktgruppeFormular.parentProdGrBox.setSelectedIndex(index);
        } else {
            produktgruppeFormular.parentProdGrBox.setSelectedIndex(-1);
        }
        if ( allRowsEqual(firstName, 1) ){
            produktgruppeFormular.nameField.setText(firstName);
        } else {
            produktgruppeFormular.nameField.setEnabled(false);
        }
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
        if ( allRowsEqual(firstAktiv, 5) ){
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
            if (elem == null && element != null){
                return false;
            }
            if (elem != null && element == null){
                return false;
            }
            if ( elem != null && element != null && (!elem.equals(element)) ){
                return false;
            }
        }
        return true;
    }

    // will data be lost on close?
    public boolean willDataBeLost() {
        if ( produktgruppeFormular.parentProdGrBox.isEnabled() ){
            int selIndex = produktgruppeFormular.parentProdGrBox.getSelectedIndex();
            // -1 means "no selection done"
            if (selIndex != -1){
                Integer selParentProdGrID = produktgruppeFormular.parentProdGrIDs.get(selIndex);
                if ( !allElementsEqual(selParentProdGrID, originalParentProdGrIDs) ){
                    return true;
                }
            }
        }
        if ( produktgruppeFormular.nameField.isEnabled() ){
            String origName = (String)originalData.get(0).get(1);
            if ( !origName.equals(produktgruppeFormular.nameField.getText()) ){
                return true;
            }
        }
        if ( produktgruppeFormular.mwstBox.isEnabled() ){
            int selIndex = produktgruppeFormular.mwstBox.getSelectedIndex();
            // -1 means "no selection done"
            if (selIndex != -1){
                Integer selMwStID = produktgruppeFormular.mwstIDs.get(selIndex);
                if ( !allElementsEqual(selMwStID, originalMwStIDs) ){
                    return true;
                }
            }
        }
        if ( produktgruppeFormular.pfandBox.isEnabled() ){
            int selIndex = produktgruppeFormular.pfandBox.getSelectedIndex();
            // -1 means "no selection done"
            if (selIndex != -1){
                Integer selPfandID = produktgruppeFormular.pfandIDs.get(selIndex);
                if ( !allElementsEqual(selPfandID, originalPfandIDs) ){
                    return true;
                }
            }
        }
        if ( aktivBox.isEnabled() ){
            Boolean origAktiv = (Boolean)originalData.get(0).get(5);
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
            String origName = (String)originalData.get(i).get(1);
            String newName = produktgruppeFormular.nameField.isEnabled() ?
                produktgruppeFormular.nameField.getText() : origName;
            if ( !newName.equals(origName) ){
                if ( isProdGrAlreadyKnown(newName) ){
                    // not allowed: changing name to one that is already registered in DB
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Produktgruppe '"+newName+"' bereits vorhanden! Wird zurückgesetzt.",
                            "Info", JOptionPane.INFORMATION_MESSAGE);
                    produktgruppeFormular.nameField.setText(origName);
                    return 1;
                }
            }
            Integer origProdGrID = originalProdGrIDs.get(i);
            Integer ppgID = originalParentProdGrIDs.get(i);
            if (produktgruppeFormular.parentProdGrBox.isEnabled()){
                int selIndex = produktgruppeFormular.parentProdGrBox.getSelectedIndex();
                // -1 means "no selection done"
                if (selIndex != -1){
                    ppgID = produktgruppeFormular.parentProdGrIDs.get(selIndex);
                }
            }
            Vector<Integer> ids;
            if (ppgID == originalParentProdGrIDs.get(i)){
                // use old ids
                ids = produktgruppeFormular.findProdGrIDs(origProdGrID);
            } else {
                // assign new ids
                ids = produktgruppeFormular.idsOfNewProdGr(ppgID);
            }
            Integer topid = ids.get(0);
            Integer subid = ids.get(1);
            Integer subsubid = ids.get(2);
            Integer mwst_id = originalMwStIDs.get(i);
            if (produktgruppeFormular.mwstBox.isEnabled()){
                int selIndex = produktgruppeFormular.mwstBox.getSelectedIndex();
                // -1 means "no selection done"
                if (selIndex != -1){
                    mwst_id = produktgruppeFormular.mwstIDs.get(selIndex);
                }
            }
            Integer pfand_id = originalPfandIDs.get(i);
            if (produktgruppeFormular.pfandBox.isEnabled()){
                int selIndex = produktgruppeFormular.pfandBox.getSelectedIndex();
                // -1 means "no selection done"
                if (selIndex != -1){
                    pfand_id = produktgruppeFormular.pfandIDs.get(selIndex);
                }
            }
            Boolean aktiv = aktivBox.isEnabled() ?
                aktivBox.isSelected() :
                (Boolean)originalData.get(i).get(5);

            // update the produktgruppe with new values
            int result = updateProdGr(origProdGrID, topid, subid, subsubid, newName, mwst_id, pfand_id, aktiv);
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Produktgruppe "+origName+" konnte nicht geändert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
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
	if (e.getSource() == produktgruppeFormular.parentProdGrBox){
            submitButton.setEnabled( isSubmittable() );
            return;
        }
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
