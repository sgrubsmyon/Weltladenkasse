package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.*; // Connection, Statment, ResultSet

// GUI stuff:
import java.awt.*; // BorderLayout, FlowLayout, Dimension
import java.awt.event.*; // ActionEvent, ActionListener

import javax.swing.*; // JFrame, JPanel, JTable, JButton, ...
import javax.swing.table.*;
import javax.swing.text.*; // for DocumentFilter
import javax.swing.event.*;

public class Kassieren extends JPanel implements ActionListener, DocumentListener {
    // Text Fields
    private BarcodeComboBox barcodeBox;
    public ArtikelNameComboBox artikelBox;
    public ArtikelNummerComboBox nummerBox;
    private JTextComponent barcodeField;
    private JTextComponent artikelField;
    private JTextComponent nummerField;
    public String artikelNameText = "";
    public String artikelNummerText = "";
    public String barcodeText = "";
    public String barcodeMemory = "";

    public JTextField preisField;

    // Buttons
    private JButton emptyBarcodeButton;
    private JButton emptyArtikelButton;
    private JButton emptyNummerButton;

    public ArticleSelectPanel(JTextField preisField) {
        super();
        this.preisField = preisField;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel barcodePanel = new JPanel();
        barcodePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
            String filterStr = " AND (toplevel_id IS NOT NULL OR sub_id = 2) ";
                   // show all 'normal' items (toplevel_id IS NOT NULL), and in addition Gutscheine (where toplevel_id is NULL and sub_id is 2)
            barcodeBox = new BarcodeComboBox(this.conn, filterStr);
            barcodeBox.setFont(mediumFont);
            barcodeBox.addActionListener(this);
            barcodeBox.addPopupMouseListener(new MouseListenerBarcodeBox());
            barcodeField = (JTextField)barcodeBox.getEditor().getEditorComponent();
            barcodeField.setColumns(9);
            removeDefaultKeyBindings(barcodeField);
            barcodeField.getDocument().addDocumentListener(this);
            BigLabel barcodeLabel = new BigLabel("Barcode: ");
            barcodeLabel.setLabelFor(barcodeBox);
            barcodeLabel.setDisplayedMnemonic(KeyEvent.VK_C);
            barcodePanel.add(barcodeLabel);
            barcodePanel.add(barcodeBox);
            emptyBarcodeButton = new JButton("x");
            emptyBarcodeButton.addActionListener(this);
            barcodePanel.add(emptyBarcodeButton);

            nummerBox = new ArtikelNummerComboBox(this.conn, filterStr);
            nummerBox.setFont(mediumFont);
            nummerBox.addActionListener(this);
            nummerBox.addPopupMouseListener(new MouseListenerNummerBox());
            // set preferred width etc.:
            nummerBox.addPopupMenuListener(new BoundsPopupMenuListener(false, true, 30, false));
            nummerField = (JTextField)nummerBox.getEditor().getEditorComponent();
            nummerField.setColumns(7);
            removeDefaultKeyBindings(nummerField);
            nummerField.getDocument().addDocumentListener(this);
            BigLabel nummerLabel = new BigLabel("Artikelnr.: ");
            nummerLabel.setLabelFor(nummerBox);
            nummerLabel.setDisplayedMnemonic(KeyEvent.VK_N);
            barcodePanel.add(nummerLabel);
            barcodePanel.add(nummerBox);
            emptyNummerButton = new JButton("x");
            emptyNummerButton.addActionListener(this);
            barcodePanel.add(emptyNummerButton);
        this.add(barcodePanel);

        this.add(Box.createRigidArea(new Dimension(0, 5))); // vertical space

        JPanel artikelNamePanel = new JPanel();
            artikelBox = new ArtikelNameComboBox(this.conn, filterStr);
            artikelBox.setFont(mediumFont);
            artikelBox.addActionListener(this);
            artikelBox.addPopupMouseListener(new MouseListenerArtikelBox());
            // set preferred width etc.:
            artikelBox.addPopupMenuListener(new BoundsPopupMenuListener(false, true, 50, false));
            artikelField = (JTextField)artikelBox.getEditor().getEditorComponent();
            artikelField.setColumns(25);
            removeDefaultKeyBindings(artikelField);
            artikelField.getDocument().addDocumentListener(this);
            BigLabel artikelLabel = new BigLabel("Artikelname: ");
            artikelLabel.setLabelFor(artikelBox);
            artikelLabel.setDisplayedMnemonic(KeyEvent.VK_A);
            artikelNamePanel.add(artikelLabel);
            artikelNamePanel.add(artikelBox);
            emptyArtikelButton = new JButton("x");
            emptyArtikelButton.addActionListener(this);
            artikelNamePanel.add(emptyArtikelButton);
        this.add(artikelNamePanel);
    }

    public void updateSelectedArticleID() {
        // update selected Artikel
        String[] an = artikelBox.parseArtikelName();
        String artikelName = an[0];
        String lieferant = an[1];
        String artikelNummer = (String)nummerBox.getSelectedItem();
        selectedArticleID = getArticleID(lieferant, artikelNummer); // get the internal artikelID from the DB
    }

    private void checkIfFormIsComplete() {
        int nummerNumber = nummerBox.getItemCount();
        int artikelNumber = artikelBox.getItemCount();
        if ( artikelNumber == 1 && nummerNumber == 1 ){ // artikel eindeutig festgelegt
            updateSelectedArticleID();
            setPriceField();
            boolean hasBarcode = doesArticleHaveBarcode(selectedArticleID);
            if (hasBarcode) {
                // Forget the remembered barcode. Another article was selected
                // that already has a barcode.
                barcodeMemory = "";
            } else {
                rememberBarcode();
            }

            anzahlField.requestFocus();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    anzahlField.selectAll();
                }
            });
        }
        setButtonsEnabled();
    }

    private void rememberBarcode() {
        if (barcodeMemory != "") {
            int answer = JOptionPane.showConfirmDialog(mainWindow,
                    "Der zuletzt gescannte Barcode\n"+
                    "    "+barcodeMemory+"\n"+
                    "könnte mit dem ausgewählten Artikel\n"+
                    "    "+getShortName(selectedArticleID)+"\n"+
                    "übereinstimmen.\n"+
                    "Falls ja, dann kann der Barcode jetzt unter diesem Artikel gespeichert werden.\n\n"+
                    "Ist das erwünscht? (Bei Unsicherheit 'Nein' wählen)", "Barcode speichern?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            System.out.println(answer);
            if (answer == JOptionPane.YES_OPTION) {
                Artikel origArticle = getArticle(selectedArticleID);
                Artikel newArticle = getArticle(selectedArticleID);
                newArticle.setBarcode(barcodeMemory);
                updateArticle(origArticle, newArticle);

                System.out.println("old selectedArticleID: "+selectedArticleID);
                updateSelectedArticleID();
                System.out.println("new selectedArticleID: "+selectedArticleID);
            }
            // Forget about it.
            barcodeMemory = "";
        }
    }

    private Vector< Vector<String[]> > getArtikelNameAndNummerForBarcode() {
        String barcode = (String)barcodeBox.getSelectedItem();
        Vector<String[]> artikelNamen = new Vector<String[]>();
        Vector<String[]> artikelNummern = new Vector<String[]>();
        Vector< Vector<String[]> > result = new Vector< Vector<String[]> >();
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT DISTINCT a.artikel_name, l.lieferant_name, a.sortiment, "+
                    "a.artikel_nr FROM artikel AS a " +
                    "LEFT JOIN lieferant AS l USING (lieferant_id) " +
                    "WHERE a.barcode = ? " +
                    "AND a.aktiv = TRUE"
                    );
            pstmt.setString(1, barcode);
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result ...
            while ( rs.next() ){
                String lieferant = rs.getString(2) != null ? rs.getString(2) : "";
                Boolean sortiment = rs.getBoolean(3);
                artikelNamen.add( new String[]{rs.getString(1), lieferant, sortiment.toString()} );
                artikelNummern.add( new String[]{rs.getString(4)} );
            }
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        result.add(artikelNamen);
        result.add(artikelNummern);
        if (artikelNamen.size() == 0){
            // This barcode is not known to the DB.
            // Remember this barcode and possibly enter it in DB later.
            System.out.println();
            System.out.println("barcodeMemory before: "+barcodeMemory);
            barcodeMemory = barcode;
            System.out.println("barcodeMemory after: "+barcodeMemory);
            System.out.println();
        } else {
            // Forget the remembered barcode.
            System.out.println();
            System.out.println("barcodeMemory before: "+barcodeMemory);
            barcodeMemory = "";
            System.out.println("barcodeMemory after: "+barcodeMemory);
            System.out.println();
        }
        return result;
    }

    private void setArtikelNameAndNummerForBarcode(Vector<String[]> artikelNamen,
            Vector<String[]> artikelNummern) {
        if (artikelBox.getItemCount() != 1){
            //artikelBox.removeActionListener(this);
                if (artikelNamen.size() == 1){
                    // update internal cache string before changing name in text field (otherwise document listener causes problems)
                    artikelNameText = artikelNamen.get(0)[0];
                }
                artikelBox.setItems(artikelNamen);
            //artikelBox.addActionListener(this);
        }
        if (nummerBox.getItemCount() != 1){
            //nummerBox.removeActionListener(this);
                if (artikelNummern.size() == 1){
                    // update internal cache string before changing name in text field (otherwise document listener causes problems)
                    artikelNummerText = artikelNummern.get(0)[0];
                }
                nummerBox.setItems(artikelNummern);
            //nummerBox.addActionListener(this);
        }
    }

    private void setArtikelNameForNummer() {
        // get artikelNummer
        String artikelNummer = (String)nummerBox.getSelectedItem();
        Vector<String[]> artikelNamen = new Vector<String[]>();
        // get artikelName for artikelNummer
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT DISTINCT a.artikel_name, l.lieferant_name, a.sortiment FROM artikel AS a " +
                    "LEFT JOIN lieferant AS l USING (lieferant_id) " +
                    "WHERE a.artikel_nr = ? " +
                    "AND a.aktiv = TRUE"
                    );
            pstmt.setString(1, artikelNummer);
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result ...
            while ( rs.next() ){
                String lieferant = rs.getString(2) != null ? rs.getString(2) : "";
                Boolean sortiment = rs.getBoolean(3);
                artikelNamen.add( new String[]{rs.getString(1), lieferant, sortiment.toString()} );
            }
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        if (artikelBox.getItemCount() != 1){
            //artikelBox.removeActionListener(this);
                if (artikelNamen.size() == 1){
                    // update internal cache string before changing name in text field (otherwise document listener causes problems)
                    artikelNameText = artikelNamen.get(0)[0];
                }
                artikelBox.setItems(artikelNamen);
            //artikelBox.addActionListener(this);
        }
    }

    private void setArtikelNummerForName() {
        // get artikelName
        String[] an = artikelBox.parseArtikelName();
        String artikelName = an[0];
        String lieferant = an[1];
        String lieferantQuery = lieferant.equals("") ? "IS NULL" : "= ?";
        Vector<String[]> artikelNummern = new Vector<String[]>();
        // get artikelNummer for artikelName
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT DISTINCT a.artikel_nr FROM artikel AS a " +
                    "LEFT JOIN lieferant AS l USING (lieferant_id) " +
                    "WHERE a.artikel_name = ? AND l.lieferant_name "+lieferantQuery+" " +
                    "AND a.aktiv = TRUE"
                    );
            pstmt.setString(1, artikelName);
            if (!lieferant.equals("")){
                pstmt.setString(2, lieferant);
            }
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result ...
            while ( rs.next() ){
                artikelNummern.add( new String[]{rs.getString(1)} );
            }
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        if (nummerBox.getItemCount() != 1){
            //nummerBox.removeActionListener(this);
                if (artikelNummern.size() == 1){
                    // update internal cache string before changing name in text field (otherwise document listener causes problems)
                    artikelNummerText = artikelNummern.get(0)[0];
                }
                nummerBox.setItems(artikelNummern);
            //nummerBox.addActionListener(this);
        }
    }

    private void resetFormFromBarcodeBox() {
        artikelNameText = "";
        artikelNummerText = "";
        artikelBox.emptyBox();
        nummerBox.emptyBox();
        preisField.setText("");
        preisField.setEditable(false);
    }
    private void resetFormFromArtikelBox() {
        System.out.println("resetting form from artikel box.");
        barcodeText = "";
        artikelNummerText = "";
        barcodeBox.emptyBox();
        nummerBox.emptyBox();
        preisField.setText("");
        preisField.setEditable(false);
    }
    private void resetFormFromNummerBox() {
        System.out.println("resetting form from nummer box.");
        barcodeText = "";
        artikelNameText = "";
        barcodeBox.emptyBox();
        artikelBox.emptyBox();
        preisField.setText("");
        preisField.setEditable(false);
    }

    public void emptyBarcodeBox() {
        barcodeText = "";
        barcodeBox.emptyBox();
        barcodeBox.requestFocus();
    }

    public void emptyArtikelBox() {
        artikelNameText = "";
        artikelBox.emptyBox();
        artikelBox.requestFocus();
    }

    public void emptyNummerBox() {
        artikelNummerText = "";
        nummerBox.emptyBox();
        nummerBox.requestFocus();
    }

    private void checkBarcodeBox(ActionEvent e) {
        Vector< Vector<String[]> > res = getArtikelNameAndNummerForBarcode();
        if ( barcodeBox.getItemCount() == 1 ){ // if selection is correct and unique
            setArtikelNameAndNummerForBarcode(res.get(0), res.get(1));
        }
        checkIfFormIsComplete();
    }

    private void checkArtikelBox(ActionEvent e) {
        //System.out.println("actionPerformed in artikelBox, actionCommand: "+e.getActionCommand()+", modifiers: "+e.getModifiers()+", itemCount: "+artikelBox.getItemCount()+", selectedItem: "+artikelBox.getSelectedItem()+"   artikelNameText: "+artikelNameText);
        if ( artikelBox.getItemCount() == 1 ){ // if selection is correct and unique
            setArtikelNummerForName();
        }
        checkIfFormIsComplete();
    }

    private void checkNummerBox(ActionEvent e) {
        if ( nummerBox.getItemCount() == 1 ){ // if selection is correct and unique
            setArtikelNameForNummer();
        }
        checkIfFormIsComplete();
    }

    // need a low-level mouse listener to remove DocumentListeners upon mouse click
    public class MouseListenerBarcodeBox extends MouseAdapter {
        @Override
            public void mousePressed(MouseEvent e) {
                barcodeBox.setBoxMode = true;
            }
        @Override
            public void mouseReleased(MouseEvent e) {
                barcodeBox.setBoxMode = false;
            }
    }
    // need a low-level mouse listener to remove DocumentListeners upon mouse click
    public class MouseListenerArtikelBox extends MouseAdapter {
        @Override
            public void mousePressed(MouseEvent e) {
                artikelBox.setBoxMode = true;
            }
        @Override
            public void mouseReleased(MouseEvent e) {
                artikelBox.setBoxMode = false;
            }
    }
    // need a low-level mouse listener to remove DocumentListeners upon mouse click
    public class MouseListenerNummerBox extends MouseAdapter {
        @Override
            public void mousePressed(MouseEvent e) {
                nummerBox.setBoxMode = true;
            }
        @Override
            public void mouseReleased(MouseEvent e) {
                nummerBox.setBoxMode = false;
            }
    }


    /**
     *    * Each non abstract class that implements the DocumentListener
     *      must have these methods.
     *
     *    @param e the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
        if (e.getDocument() == barcodeField.getDocument()){
            if (barcodeBox.setBoxMode){ return; }
            //System.out.println("\nbarcodeField DocumentListener fired!");
            //System.out.println("selectedItem: "+barcodeBox.getSelectedItem());
            //System.out.println("barcodeField text: "+barcodeField.getText()+"   barcodeText: "+barcodeText);
            if ( !barcodeField.getText().equals(barcodeText) ) { // some editing change in box
                resetFormFromBarcodeBox();
                barcodeText = barcodeField.getText();
            }
            checkIfFormIsComplete();
            return;
        }
        if (e.getDocument() == artikelField.getDocument()){
            if (artikelBox.setBoxMode){ return; }
            //System.out.println("\nartikelField DocumentListener fired!");
            //System.out.println("selectedItem: "+artikelBox.getSelectedItem());
            //System.out.println("artikelField text: "+artikelField.getText()+"   artikelNameText: "+artikelNameText);
            if ( !artikelField.getText().equals(artikelNameText) ) { // some editing change in box
                resetFormFromArtikelBox();
                artikelNameText = artikelField.getText();
            }
            checkIfFormIsComplete();
            return;
        }
        if (e.getDocument() == nummerField.getDocument()){
            if (nummerBox.setBoxMode){ return; }
            //System.out.println("\nnummerField DocumentListener fired!");
            //System.out.println("selectedItem: "+nummerBox.getSelectedItem());
            //System.out.println("nummerField text: "+nummerField.getText()+"   artikelNummerText: "+artikelNummerText);
            if ( !nummerField.getText().equals(artikelNummerText) ) { // some editing change in box
                resetFormFromNummerBox();
                artikelNummerText = nummerField.getText();
            }
            checkIfFormIsComplete();
            return;
        }
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == barcodeBox){
            if (barcodeBox.changeMode){ return; }
            checkBarcodeBox(e);
            return;
        }
        if (e.getSource() == artikelBox){
            if (artikelBox.changeMode){ return; }
            checkArtikelBox(e);
            return;
        }
        if (e.getSource() == nummerBox){
            if (nummerBox.changeMode){ return; }
            checkNummerBox(e);
            return;
        }
        if (e.getSource() == emptyBarcodeButton){
            emptyBarcodeBox();
	    return;
	}
        if (e.getSource() == emptyArtikelButton){
            emptyArtikelBox();
	    return;
	}
        if (e.getSource() == emptyNummerButton){
            emptyNummerBox();
	    return;
	}
    }
}
