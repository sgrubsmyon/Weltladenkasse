package org.weltladen_bonn.pos;

// Basic Java stuff:
import org.weltladen_bonn.pos.BaseClass.BigLabel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;
import org.mariadb.jdbc.MariaDbPoolDataSource;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// MySQL Connector/J stuff:
// GUI stuff:

public abstract class ArticleSelectPanelGrundlage extends ArtikelGrundlage implements ActionListener, DocumentListener {
    /**
     *
     */
    private static final Logger logger = LogManager.getLogger(ArticleSelectPanelGrundlage.class);

    private static final long serialVersionUID = 1L;
    // Text Fields
    public BarcodeComboBox barcodeBox;
    public ArtikelNameComboBox artikelBox;
    public ArtikelNummerComboBox nummerBox;
    private JTextField barcodeField;
    public JTextField artikelField;
    private JTextField nummerField;
    private String artikelNameText = "";
    private String artikelNummerText = "";
    private String barcodeText = "";
    private String barcodeMemory = "";

    private ArticleSelectUser articleSelectUser;

    // Buttons
    public JButton emptyBarcodeButton;
    public JButton emptyArtikelButton;
    public JButton emptyNummerButton;

    protected int selectedArticleID;
    // show all 'normal' items (toplevel_id IS NOT NULL), and in addition
    // Gutscheine (where toplevel_id is NULL and sub_id is 2):
    private String filterStr = " AND (toplevel_id IS NOT NULL OR sub_id = 2 OR sub_id = 4) "; // exceptions for Gutschein (sub_id = 2) and Pfand optional (sub_id = 4)

    public ArticleSelectPanelGrundlage(MariaDbPoolDataSource pool, MainWindowGrundlage mw, ArticleSelectUser asu, String fs) {
        super(pool, mw);
        if (fs != null) {
          filterStr = fs;
        }
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.articleSelectUser = asu;

        JPanel barcodePanel = new JPanel();
        //barcodePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
        barcodeBox = new BarcodeComboBox(this.pool, filterStr);
        barcodeBox.setFont(BaseClass.mediumFont);
        FontMetrics fm = barcodeBox.getFontMetrics(BaseClass.mediumFont);
        int height = (int)(1.5*(double)fm.getHeight());
        barcodeBox.setPreferredSize(new Dimension(180, height));
        barcodeBox.addActionListener(this);
        barcodeBox.addPopupMouseListener(new MouseListenerBarcodeBox());
        barcodeField = (JTextField) barcodeBox.getEditor().getEditorComponent();
        removeDefaultKeyBindings(barcodeField);
        barcodeField.addKeyListener(removeNumPadAdapter);
        barcodeField.getDocument().addDocumentListener(this);
        BigLabel barcodeLabel = new BigLabel("Barcode: ");
        barcodeLabel.setLabelFor(barcodeBox);
        barcodeLabel.setDisplayedMnemonic(KeyEvent.VK_C);
        barcodePanel.add(barcodeLabel);
        barcodePanel.add(barcodeBox);
        emptyBarcodeButton = new JButton("x");
        emptyBarcodeButton.addActionListener(this);
        barcodePanel.add(emptyBarcodeButton);

        nummerBox = new ArtikelNummerComboBox(this.pool, filterStr);
        nummerBox.setFont(BaseClass.mediumFont);
        nummerBox.setPreferredSize(new Dimension(160, height));
        nummerBox.addActionListener(this);
        nummerBox.addPopupMouseListener(new MouseListenerNummerBox());
        // set preferred width etc.:
        nummerBox.addPopupMenuListener(new BoundsPopupMenuListener(false, true, -1, false));
        nummerField = (JTextField) nummerBox.getEditor().getEditorComponent();
        removeDefaultKeyBindings(nummerField);
        nummerField.addKeyListener(removeNumPadAdapter);
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
        artikelBox = new ArtikelNameComboBox(this.pool, filterStr, bc);
        artikelBox.addActionListener(this);
        artikelBox.addPopupMouseListener(new MouseListenerArtikelBox());
        // set preferred width etc.:
        artikelBox.addPopupMenuListener(new BoundsPopupMenuListener(false, true, -1, false));
        artikelBox.setFont(BaseClass.mediumFont);
        artikelBox.setPreferredSize(new Dimension(460, height));
        artikelBox.setMaximumRowCount(29);
        artikelField = (JTextField) artikelBox.getEditor().getEditorComponent();
        artikelBox.setMaximumSize( artikelBox.getPreferredSize() );
        removeDefaultKeyBindings(artikelField);
        artikelField.addKeyListener(removeNumPadAdapter);
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

        setupKeyboardShortcuts();
    }

    private void setupKeyboardShortcuts() {
        // keyboard shortcuts:
        KeyStroke barcodeShortcut1 = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);
        KeyStroke barcodeShortcut2 = KeyStroke.getKeyStroke("ctrl B");
        KeyStroke artikelNameShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
        KeyStroke artikelNummerShortcut1 = KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0);
        KeyStroke artikelNummerShortcut2 = KeyStroke.getKeyStroke("ctrl N");

        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(barcodeShortcut1, "barcode");
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(barcodeShortcut2, "barcode");
        this.getActionMap().put("barcode", new BarcodeAction());
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(artikelNameShortcut, "name");
        this.getActionMap().put("name", new NameAction());
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(artikelNummerShortcut1, "nummer");
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(artikelNummerShortcut2, "nummer");
        this.getActionMap().put("nummer", new NummerAction());
    }

    private class BarcodeAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            emptyBarcodeBox();
        }
    }

    private class NameAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            emptyArtikelBox();
        }
    }

    private class NummerAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            emptyNummerBox();
        }
    }

    public int getSelectedArticleID() {
        return selectedArticleID;
    }

    public void updateSelectedArticleID() {
        // update selected Artikel
        String[] an = artikelBox.parseArtikelName();
        String artikelName = an[0];
        String liefID = an[1];
        String artikelNummer = (String) nummerBox.getSelectedItem();
        try {
          selectedArticleID = getArticleID(Integer.parseInt(liefID), artikelNummer); // get the internal artikelID from the DB
        } catch (java.lang.NumberFormatException ex) {
          logger.warn("liefID is invalid (probably empty string): liefId = {}", liefID);
        }
        articleSelectUser.updateSelectedArticleID(selectedArticleID);
    }

    public void checkIfFormIsComplete() {
        int nummerNumber = nummerBox.getItemCount();
        int artikelNumber = artikelBox.getItemCount();
        if (artikelNumber == 1 && nummerNumber == 1) { // artikel eindeutig
                                                       // festgelegt
            updateSelectedArticleID();
            setPriceField();
            boolean hasBarcode = doesArticleHaveBarcode(selectedArticleID);
            boolean hasVarPrice = doesArticleHaveVarPrice(selectedArticleID);
            if (hasBarcode || hasVarPrice) {
                // Forget the remembered barcode. Another article was selected
                // that already has a barcode.
                //System.out.println();
                //System.out.println("Another article was selected "+
                //        "that already has a barcode. barcodeMemory is forgotten.");
                barcodeMemory = "";
            } else {
                rememberBarcode();
            }
            articleSelectFinishedFocus();
        } else {
            resetPriceField();
        }
        setButtonsEnabled();
    }

    protected abstract void setPriceField();

    protected abstract void resetPriceField();

    protected abstract void articleSelectFinishedFocus();

    protected abstract void setButtonsEnabled();

    private void rememberBarcode() {
        if (barcodeMemory != "") {
            int answer = JOptionPane.showConfirmDialog(mainWindow,
                    "Der zuletzt gescannte Barcode\n" + "    " + barcodeMemory + "\n"
                            + "könnte mit dem ausgewählten Artikel\n" + "    " + getShortName(selectedArticleID) + "\n"
                            + "übereinstimmen.\n"
                            + "Falls ja, dann kann der Barcode jetzt unter diesem Artikel gespeichert werden.\n\n"
                            + "Ist das erwünscht? (Bei Unsicherheit 'Nein' wählen)",
                    "Barcode speichern?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                Artikel origArticle = getArticle(selectedArticleID);
                Artikel newArticle = getArticle(selectedArticleID);
                newArticle.setBarcode(barcodeMemory);
                updateArticle(origArticle, newArticle);

                //System.out.println("old selectedArticleID: "+selectedArticleID);
                updateSelectedArticleID();
                //System.out.println("new selectedArticleID: "+selectedArticleID);
            }
            // Forget about it.
            //System.out.println();
            //System.out.println("barcodeMemory is forgotten.");
            barcodeMemory = "";
        }
    }

    private Vector<Vector<String[]>> getArtikelNameAndNummerForBarcode() {
        String barcode = (String) barcodeBox.getSelectedItem();
        Vector<String[]> artikelNamen = new Vector<String[]>();
        Vector<String[]> artikelNummern = new Vector<String[]>();
        Vector<Vector<String[]>> result = new Vector<Vector<String[]>>();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("SELECT DISTINCT a.artikel_name, l.lieferant_kurzname, a.vk_preis, a.sortiment, a.lieferant_id, "
                            + "a.artikel_nr FROM artikel AS a " + "LEFT JOIN lieferant AS l USING (lieferant_id) "
                            + "WHERE a.barcode = ? " + "AND a.aktiv = TRUE");
            pstmt.setString(1, barcode);
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result
            // ...
            while (rs.next()) {
                String artName = rs.getString(1);
                String liefKurzName = rs.getString(2) != null ? rs.getString(2) : "";
                String vkPreis = rs.getString(3) != null ? rs.getString(3) : "";
                Boolean sortiment = rs.getBoolean(4);
                String liefID = rs.getString(5);
                String artNummer = rs.getString(6);
                if (!vkPreis.equals("")){
                    vkPreis = bc.priceFormatter(vkPreis)+" "+bc.currencySymbol;
                }

                artikelNamen.add(new String[]{artName, liefKurzName, vkPreis, sortiment.toString(), liefID});
                artikelNummern.add(new String[]{artNummer});
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            // System.out.println("Exception: " + ex.getMessage());
            // ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        result.add(artikelNamen);
        result.add(artikelNummern);
        if (artikelNamen.size() == 0) {
            // This barcode is not known to the DB.
            // Remember this barcode and possibly enter it in DB later.
            //System.out.println();
            //System.out.println("barcodeMemory before: "+barcodeMemory);
            barcodeMemory = barcode;
            //System.out.println("barcodeMemory after: "+barcodeMemory);
            //System.out.println();
        } else {
            // Forget the remembered barcode.
            //System.out.println();
            //System.out.println("barcodeMemory before: "+barcodeMemory);
            barcodeMemory = "";
            //System.out.println("barcodeMemory after: "+barcodeMemory);
            //System.out.println();
        }
        return result;
    }

    void robotPressDown() {
        Robot robot = null;
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        // Simulate a key press
        robot.keyPress(KeyEvent.VK_DOWN);
        robot.keyRelease(KeyEvent.VK_DOWN);
        robot.keyPress(KeyEvent.VK_UP);
        robot.keyRelease(KeyEvent.VK_UP);
    }

    private void setArtikelNameAndNummerForBarcode(Vector<String[]> artikelNamen, Vector<String[]> artikelNummern) {
        if (nummerBox.getItemCount() != 1) {
            if (artikelNummern.size() == 1) {
                // update internal cache string before changing name in text
                // field (otherwise document listener causes problems)
                artikelNummerText = artikelNummern.get(0)[0];
            }
            nummerBox.setItems(artikelNummern);
        }
        if (artikelBox.getItemCount() != 1) {
            if (artikelNamen.size() == 1) {
                // update internal cache string before changing name in text
                // field (otherwise document listener causes problems)
                artikelNameText = artikelNamen.get(0)[0];
            }
            artikelBox.setItems(artikelNamen);
            if (artikelNamen.size() > 1) {
                // ambiguous barcode: show drop down for selection of correct article
                // need to hack this with a robot, because pop-up vanishes otherwise
                robotPressDown();
            }
        }
    }

    private void setArtikelNameForNummer() {
        // get artikelNummer
        String artikelNummer = (String) nummerBox.getSelectedItem();
        Vector<String[]> artikelNamen = new Vector<String[]>();
        // get artikelName for artikelNummer
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("SELECT DISTINCT a.artikel_name, l.lieferant_kurzname, a.vk_preis, a.sortiment, a.lieferant_id "+
                            "FROM artikel AS a LEFT JOIN lieferant AS l USING (lieferant_id) "+
                            "WHERE a.artikel_nr = ? AND a.aktiv = TRUE");
            pstmt.setString(1, artikelNummer);
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result
            // ...
            while (rs.next()) {
                String artName = rs.getString(1);
                String liefKurzName = rs.getString(2) != null ? rs.getString(2) : "";
                String vkPreis = rs.getString(3) != null ? rs.getString(3) : "";
                Boolean sortiment = rs.getBoolean(4);
                String liefID = rs.getString(5);
                if (!vkPreis.equals("")){
                    vkPreis = bc.priceFormatter(vkPreis)+" "+bc.currencySymbol;
                }

                artikelNamen.add(new String[]{artName, liefKurzName, vkPreis, sortiment.toString(), liefID});
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            // System.out.println("Exception: " + ex.getMessage());
            // ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        if (artikelBox.getItemCount() != 1) {
            if (artikelNamen.size() == 1) {
                // update internal cache string before changing name in text
                // field (otherwise document listener causes problems)
                artikelNameText = artikelNamen.get(0)[0];
            }
            artikelBox.setItems(artikelNamen);
            if (artikelNamen.size() > 1) {
                // ambiguous nummer: show drop down for selection of correct article (lieferant)
                // need to hack this with a robot, because pop-up vanishes otherwise
                robotPressDown();
            }
        }
    }

    private void setArtikelNummerForName() {
        // get artikelName
        String[] an = artikelBox.parseArtikelName();
        String artikelName = an[0];
        String liefID = an[1];
        Vector<String[]> artikelNummern = new Vector<String[]>();
        // get artikelNummer for artikelName
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement("SELECT DISTINCT a.artikel_nr FROM artikel AS a "
                    + "WHERE a.artikel_name = ? AND a.lieferant_id = ? AND a.aktiv = TRUE");
            pstmt.setString(1, artikelName);
            pstmt.setString(2, liefID);
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result
            // ...
            while (rs.next()) {
                artikelNummern.add(new String[] { rs.getString(1) });
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            // System.out.println("Exception: " + ex.getMessage());
            // ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        if (nummerBox.getItemCount() != 1) {
            if (artikelNummern.size() == 1) {
                // update internal cache string before changing name in text
                // field (otherwise document listener causes problems)
                artikelNummerText = artikelNummern.get(0)[0];
            }
            nummerBox.setItems(artikelNummern);
            if (artikelNummern.size() > 1) {
                // ambiguous name: show drop down for selection of correct article
                // need to hack this with a robot, because pop-up vanishes otherwise
                robotPressDown();
            }
        }
    }

    private void resetFormFromBarcodeBox() {
        artikelNameText = "";
        artikelNummerText = "";
        artikelBox.emptyBox();
        nummerBox.emptyBox();
        resetOther();
    }

    private void resetFormFromArtikelBox() {
        barcodeText = "";
        artikelNummerText = "";
        barcodeBox.emptyBox();
        nummerBox.emptyBox();
        resetOther();
    }

    private void resetFormFromNummerBox() {
        barcodeText = "";
        artikelNameText = "";
        barcodeBox.emptyBox();
        artikelBox.emptyBox();
        resetOther();
    }

    private void resetForm() {
        barcodeText = "";
        artikelNameText = "";
        artikelNummerText = "";
        barcodeBox.emptyBox();
        artikelBox.emptyBox();
        nummerBox.emptyBox();
        resetOther();
    }

    protected abstract void resetOther();

    public void emptyBarcodeBox() {
        resetForm();
        barcodeBox.requestFocus();
    }

    public void emptyArtikelBox() {
        resetForm();
        artikelBox.requestFocus();
    }

    public void emptyNummerBox() {
        resetForm();
        nummerBox.requestFocus();
    }

    private void checkBarcodeBox(ActionEvent e) {
        Vector<Vector<String[]>> res = getArtikelNameAndNummerForBarcode();
        if (barcodeBox.getItemCount() == 1) { // if selection is correct and
                                              // unique
            setArtikelNameAndNummerForBarcode(res.get(0), res.get(1));
        }
        checkIfFormIsComplete();
    }

    private void checkArtikelBox(ActionEvent e) {
        if (artikelBox.getItemCount() == 1) { // if selection is correct and
                                              // unique
            setArtikelNummerForName();
        }
        checkIfFormIsComplete();
    }

    private void checkNummerBox(ActionEvent e) {
        if (nummerBox.getItemCount() == 1) { // if selection is correct and
                                             // unique
            setArtikelNameForNummer();
        }
        checkIfFormIsComplete();
    }

    // need a low-level mouse listener to remove DocumentListeners upon mouse
    // click
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

    // need a low-level mouse listener to remove DocumentListeners upon mouse
    // click
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

    // need a low-level mouse listener to remove DocumentListeners upon mouse
    // click
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
     * * Each non abstract class that implements the DocumentListener must have
     * these methods.
     *
     * @param e
     *            the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
        if (e.getDocument() == barcodeField.getDocument()) {
            if (barcodeBox.setBoxMode) {
                return;
            }
            if (!barcodeField.getText().equals(barcodeText)) { // some editing
                                                               // change in box
                resetFormFromBarcodeBox();
                barcodeText = barcodeField.getText();
            }
            checkIfFormIsComplete();
            return;
        }
        if (e.getDocument() == artikelField.getDocument()) {
            if (artikelBox.setBoxMode) {
                return;
            }
            if (!artikelField.getText().equals(artikelNameText)) { // some
                                                                   // editing
                                                                   // change in
                                                                   // box
                resetFormFromArtikelBox();
                artikelNameText = artikelField.getText();
            }
            checkIfFormIsComplete();
            return;
        }
        if (e.getDocument() == nummerField.getDocument()) {
            if (nummerBox.setBoxMode) {
                return;
            }
            if (!nummerField.getText().equals(artikelNummerText)) { // some
                                                                    // editing
                                                                    // change in
                                                                    // box
                resetFormFromNummerBox();
                artikelNummerText = nummerField.getText();
            }
            checkIfFormIsComplete();
            return;
        }
    }

    public void removeUpdate(DocumentEvent e) {
        insertUpdate(e);
    }

    public void changedUpdate(DocumentEvent e) {
        // Plain text components do not fire these events
    }

    /**
     * * Each non abstract class that implements the ActionListener must have
     * this method.
     *
     * @param e
     *            the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == barcodeBox) {
            if (barcodeBox.changeMode) {
                return;
            }
            checkBarcodeBox(e);
            return;
        }
        if (e.getSource() == artikelBox) {
            if (artikelBox.changeMode) {
                return;
            }
            checkArtikelBox(e);
            return;
        }
        if (e.getSource() == nummerBox) {
            if (nummerBox.changeMode) {
                return;
            }
            checkNummerBox(e);
            return;
        }
        if (e.getSource() == emptyBarcodeButton) {
            emptyBarcodeBox();
            return;
        }
        if (e.getSource() == emptyArtikelButton) {
            emptyArtikelBox();
            return;
        }
        if (e.getSource() == emptyNummerButton) {
            emptyNummerBox();
            return;
        }
    }
}
