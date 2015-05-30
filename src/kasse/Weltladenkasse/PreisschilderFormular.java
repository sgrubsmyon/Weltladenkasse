package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.*; // Connection, Statment, ResultSet

// GUI stuff:
import java.awt.*; // for BorderLayout, FlowLayout, Dimension
import java.awt.event.*;

import javax.swing.*; // JFrame, JPanel, JTable, JButton etc.
import javax.swing.table.*;
import javax.swing.text.*; // for DocumentFilter
import javax.swing.event.*;

import WeltladenDB.*;

public class PreisschilderFormular extends ArtikelGrundlage implements DocumentListener {
    // Attribute:
    private TabbedPaneGrundlage tabbedPane;

    // Text Fields
    private BarcodeComboBox barcodeBox;
    private ArtikelNameComboBox artikelBox;
    private ArtikelNummerComboBox nummerBox;
    private JTextComponent barcodeField;
    private JTextComponent artikelField;
    private JTextComponent nummerField;
    protected String artikelNameText = "";
    protected String artikelNummerText = "";
    protected String barcodeText = "";
    private int selectedArtikelID;
    private JTextField preisField;

    // Buttons
    private JButton emptyBarcodeButton;
    private JButton emptyArtikelButton;
    private JButton emptyNummerButton;
    private JButton hinzufuegenButton;
    private Vector<JButton> removeButtons;
    private JButton deleteButton;
    private JButton printButton;

    // The panels
    private JPanel allPanel;
    private JPanel articleListPanel;

    // The table holding the purchase articles.
    private ArtikelSelectTable myTable;
    private JScrollPane scrollPane;
    private Vector<String> columnLabels;
    private Vector< Vector<Object> > data;

    private Vector<Integer> artikelIDs;
    private Vector<String> articleNames;
    private Vector<String> mengen;
    private Vector<String> preise;
    private Vector<String> lieferanten;
    private Vector<String> kg_preise;
    protected Vector<String> colors;
    protected Vector<String> types;

    // Methoden:

    /**
     *    The constructor.
     *       */
    public PreisschilderFormular(Connection conn, MainWindowGrundlage mw, TabbedPaneGrundlage tp) {
	super(conn, mw);
        tabbedPane = tp;

        initiateVectors();
        columnLabels.add("Entfernen");

        setupKeyboardShortcuts();

        emptyTable();
	showAll();
        barcodeBox.requestFocus();
    }

    private void initiateVectors() {
	columnLabels = new Vector<String>();
        columnLabels.add("Lieferant"); columnLabels.add("Artikel-Name");
        columnLabels.add("Artikel-Nr."); columnLabels.add("Barcode");
        columnLabels.add("Einzelpreis"); columnLabels.add("MwSt.");
        colors = new Vector<String>();
        types = new Vector<String>();
    }

    private void setupKeyboardShortcuts(){
        // keyboard shortcuts:
        KeyStroke barcodeShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK); // Ctrl-C
        KeyStroke artikelNameShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK); // Ctrl-A
        KeyStroke artikelNummerShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK); // Ctrl-N

        ShortcutListener shortcutListener = new ShortcutListener();

        this.registerKeyboardAction(shortcutListener, "barcode", barcodeShortcut,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.registerKeyboardAction(shortcutListener, "name", artikelNameShortcut,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.registerKeyboardAction(shortcutListener, "nummer", artikelNummerShortcut,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // listener for keyboard shortcuts
    private class ShortcutListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand() == "barcode"){
                barcodeBox.requestFocus();
                return;
            }
            if (e.getActionCommand() == "name"){
                artikelBox.requestFocus();
                return;
            }
            if (e.getActionCommand() == "nummer"){
                nummerBox.requestFocus();
                return;
            }
        }
    }

    void showAll(){
	allPanel = new JPanel();
	allPanel.setLayout(new BoxLayout(allPanel, BoxLayout.Y_AXIS));

        JPanel articleSelectPanel = new JPanel();
        articleSelectPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));

            JPanel comboBoxPanel = new JPanel();
            comboBoxPanel.setLayout(new BoxLayout(comboBoxPanel, BoxLayout.Y_AXIS));

                JPanel barcodePanel = new JPanel();
                barcodePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
                    JLabel barcodeLabel = new JLabel("Barcode: ");
                    barcodeLabel.setLabelFor(barcodeBox);
                    barcodeLabel.setDisplayedMnemonic(KeyEvent.VK_C);
                    barcodePanel.add(barcodeLabel);
                    String filterStr = " AND (toplevel_id IS NOT NULL OR sub_id = 2) ";
                           // show all 'normal' items (toplevel_id IS NOT NULL), and in addition Gutscheine (where toplevel_id is NULL and sub_id is 2)
                    barcodeBox = new BarcodeComboBox(this.conn, filterStr);
                    barcodeBox.addActionListener(this);
                    //barcodeBox.addItemListener(this);
                    barcodeBox.addPopupMouseListener(new MouseListenerBarcodeBox());
                    barcodeBox.setPrototypeDisplayValue("qqqqqqqqqqqqqq");
                    barcodeField = (JTextComponent)barcodeBox.getEditor().getEditorComponent();
                    barcodeField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK), "none");
                        // remove Ctrl-A key binding
                    barcodeField.getDocument().addDocumentListener(this);
                    barcodePanel.add(barcodeBox);
                    emptyBarcodeButton = new JButton("x");
                    emptyBarcodeButton.addActionListener(this);
                    barcodePanel.add(emptyBarcodeButton);

                    JLabel nummerLabel = new JLabel("Artikelnr.: ");
                    nummerLabel.setLabelFor(nummerBox);
                    nummerLabel.setDisplayedMnemonic(KeyEvent.VK_N);
                    barcodePanel.add(nummerLabel);
                    nummerBox = new ArtikelNummerComboBox(this.conn, filterStr);
                    nummerBox.addActionListener(this);
                    //nummerBox.addItemListener(this);
                    nummerBox.addPopupMouseListener(new MouseListenerNummerBox());
                    // set preferred width etc.:
                    nummerBox.addPopupMenuListener(new BoundsPopupMenuListener(false, true, 30, false));
                    nummerBox.setPrototypeDisplayValue("qqqqqqq");
                    nummerField = (JTextComponent)nummerBox.getEditor().getEditorComponent();
                    nummerField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK), "none");
                        // remove Ctrl-A key binding
                    nummerField.getDocument().addDocumentListener(this);
                    barcodePanel.add(nummerBox);
                    emptyNummerButton = new JButton("x");
                    emptyNummerButton.addActionListener(this);
                    barcodePanel.add(emptyNummerButton);
                comboBoxPanel.add(barcodePanel);

                comboBoxPanel.add(Box.createRigidArea(new Dimension(0, 5))); // vertical space

                JPanel artikelNamePanel = new JPanel();
                    JLabel artikelLabel = new JLabel("Artikelname: ");
                    artikelLabel.setLabelFor(artikelBox);
                    artikelLabel.setDisplayedMnemonic(KeyEvent.VK_A);
                    artikelNamePanel.add(artikelLabel);
                    artikelBox = new ArtikelNameComboBox(this.conn, filterStr);
                    artikelBox.addActionListener(this);
                    //artikelBox.addItemListener(this);
                    artikelBox.addPopupMouseListener(new MouseListenerArtikelBox());
                    // set preferred width etc.:
                    artikelBox.addPopupMenuListener(new BoundsPopupMenuListener(false, true, 50, false));
                    artikelBox.setPrototypeDisplayValue("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq");
                    artikelField = (JTextComponent)artikelBox.getEditor().getEditorComponent();
                    artikelField.getDocument().addDocumentListener(this);
                    artikelNamePanel.add(artikelBox);
                    emptyArtikelButton = new JButton("x");
                    emptyArtikelButton.addActionListener(this);
                    artikelNamePanel.add(emptyArtikelButton);
                comboBoxPanel.add(artikelNamePanel);

            articleSelectPanel.add(comboBoxPanel);

        allPanel.add(articleSelectPanel);

	JPanel hinzufuegenPanel = new JPanel();
	hinzufuegenPanel.setLayout(new FlowLayout());
	    JLabel preisLabel = new JLabel("Preis: ");
            hinzufuegenPanel.add(preisLabel);
            preisField = new JTextField("");
            preisField.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) { if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
                    if (hinzufuegenButton.isEnabled()){
                        hinzufuegenButton.doClick();
                    }
                } }
            });
            preisField.getDocument().addDocumentListener(this);
	    ((AbstractDocument)preisField.getDocument()).setDocumentFilter(geldFilter);
            preisField.setEditable(false);
            preisField.setColumns(6);
            preisField.setHorizontalAlignment(JTextField.RIGHT);
            hinzufuegenPanel.add(preisField);
            hinzufuegenPanel.add(new JLabel(currencySymbol));

	    hinzufuegenButton = new JButton("Hinzufügen");
            hinzufuegenButton.setMnemonic(KeyEvent.VK_H);
	    hinzufuegenButton.addActionListener(this);
	    hinzufuegenButton.setEnabled(false);
	    hinzufuegenPanel.add(hinzufuegenButton);
        allPanel.add(hinzufuegenPanel);

	showTable();

        JPanel neuerKundePanel = new JPanel();
        neuerKundePanel.setLayout(new BoxLayout(neuerKundePanel, BoxLayout.Y_AXIS));
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BorderLayout());
                JPanel centerPanel = new JPanel();
                    deleteButton = new JButton("Löschen");
                    deleteButton.setMnemonic(KeyEvent.VK_L);
                    if (data.size() == 0) deleteButton.setEnabled(false);
                    deleteButton.addActionListener(this);
                    centerPanel.add(deleteButton);

                    printButton = new JButton("Artikel drucken");
                    printButton.setMnemonic(KeyEvent.VK_D);
                    if (data.size() == 0) printButton.setEnabled(false);
                    printButton.addActionListener(this);
                    centerPanel.add(printButton);
                buttonPanel.add(centerPanel, BorderLayout.CENTER);
            neuerKundePanel.add(buttonPanel);
        allPanel.add(neuerKundePanel);

	this.add(allPanel, BorderLayout.CENTER);
    }


    protected class ArtikelSelectTable extends AnyJComponentJTable {
        /**
         *    The constructor.
         *       */
        public ArtikelSelectTable(Vector< Vector<Object> > data, Vector<String> columns) {
            super(data, columns);
        }

        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);
            // add custom rendering here
            c.setFont( c.getFont().deriveFont(Font.BOLD) );
            String color = colors.get(row);
            if (color.equals("red")){ c.setForeground(Color.RED); }
            else if (color.equals("blue")){ c.setForeground(Color.BLUE); }
            else if (color.equals("green")){ c.setForeground(Color.GREEN.darker().darker()); }
            else if (color.equals("gray")){ c.setForeground(Color.GRAY); }
            else { c.setForeground(Color.BLACK); }
            //c.setBackground(Color.LIGHT_GRAY);
            return c;
        }
    }

    protected void setTableProperties(ArtikelSelectTable table) {
	// Spalteneigenschaften:
//	table.getColumnModel().getColumn(0).setPreferredWidth(10);
	TableColumn pos = table.getColumn("Lieferant");
	pos.setCellRenderer(zentralAusrichter);
	pos.setPreferredWidth(5);
	TableColumn artikelbez = table.getColumn("Artikel-Name");
	artikelbez.setCellRenderer(linksAusrichter);
	artikelbez.setPreferredWidth(150);
	TableColumn artikelnr = table.getColumn("Artikel-Nr.");
	artikelnr.setCellRenderer(linksAusrichter);
	artikelnr.setPreferredWidth(50);
	TableColumn barcode = table.getColumn("Barcode");
	barcode.setCellRenderer(linksAusrichter);
	TableColumn preis = table.getColumn("Einzelpreis");
	preis.setCellRenderer(rechtsAusrichter);
	TableColumn mwst = table.getColumn("MwSt.");
	mwst.setCellRenderer(rechtsAusrichter);
	mwst.setPreferredWidth(5);
    }


    void showTable(){
	myTable = new ArtikelSelectTable(data, columnLabels);
	setTableProperties(myTable);
	TableColumn entf = myTable.getColumn("Entfernen");
	entf.setPreferredWidth(2);

	articleListPanel = new JPanel();
	articleListPanel.setLayout(new BoxLayout(articleListPanel, BoxLayout.Y_AXIS));
	articleListPanel.setBorder(BorderFactory.createTitledBorder("Gewählte Artikel"));
            scrollPane = new JScrollPane(myTable);
            articleListPanel.add(scrollPane);
	allPanel.add(articleListPanel);
    }

    void emptyTable(){
	data = new Vector< Vector<Object> >();
        artikelIDs = new Vector<Integer>();
        articleNames = new Vector<String>();
        mengen = new Vector<String>();
        preise = new Vector<String>();
        lieferanten = new Vector<String>();
        kg_preise = new Vector<String>();
        colors = new Vector<String>();
        types = new Vector<String>();
        removeButtons = new Vector<JButton>();
    }

    private void clearAll(){
        data.clear();
        artikelIDs.clear();
        articleNames.clear();
        mengen.clear();
        preise.clear();
        lieferanten.clear();
        kg_preise.clear();
        colors.clear();
        types.clear();
        removeButtons.clear();
    }

    private void updateAll(){
	this.remove(allPanel);
	this.revalidate();
	showAll();
        barcodeBox.requestFocus();

        // scroll the table scroll pane to bottom:
        scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
    }

    private void updateTable(){
	allPanel.remove(articleListPanel);
	allPanel.revalidate();
	showTable();
    }




    private void setArtikelNameAndNummerForBarcode() {
        String barcode = (String)barcodeBox.getSelectedItem();
        Vector<String[]> artikelNamen = new Vector<String[]>();
        Vector<String[]> artikelNummern = new Vector<String[]>();
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


    private void setPriceField() {
        boolean variablerPreis = getVariablePriceBool(selectedArtikelID);
        if ( ! variablerPreis ){
            String artikelPreis = getSalePrice(selectedArtikelID);
            preisField.getDocument().removeDocumentListener(this);
            preisField.setText("");
            preisField.setText( decimalMark(artikelPreis) );
            preisField.getDocument().addDocumentListener(this);
        }
        else {
            preisField.setEditable(true);
        }
    }

    private void setButtonsEnabled() {
        if (preisField.getText().length() > 0) {
            hinzufuegenButton.setEnabled(true);
            hinzufuegenButton.requestFocus();
        }
        else {
            hinzufuegenButton.setEnabled(false);
        }
    }

    private void checkIfFormIsComplete() {
        int nummerNumber = nummerBox.getItemCount();
        int artikelNumber = artikelBox.getItemCount();
        if ( artikelNumber == 1 && nummerNumber == 1 ){ // artikel eindeutig festgelegt
            String[] an = artikelBox.parseArtikelName();
            String artikelName = an[0];
            String lieferant = an[1];
            String artikelNummer = (String)nummerBox.getSelectedItem();
            selectedArtikelID = getArticleID(lieferant, artikelNummer); // get the internal artikelID from the DB
            setPriceField();
        }
        setButtonsEnabled();
    }

    private void hinzufuegen() {
        if (artikelBox.getItemCount() != 1 || nummerBox.getItemCount() != 1){
            System.out.println("Error: article not selected unambiguously.");
            return;
        }
        String[] an = artikelBox.parseArtikelName();
        String artikelName = an[0];
        String lieferant = an[1];
        String artikelNummer = (String)nummerBox.getSelectedItem();
        String kurzname = getShortName(selectedArtikelID);
        String liefkurz = getShortLieferantName(selectedArtikelID);
        String barcode = getBarcode(selectedArtikelID);
        String artikelMwSt = getVAT(selectedArtikelID);
        Boolean sortiment = getSortimentBool(selectedArtikelID);
        String color = sortiment ? "default" : "gray";
        String[] menge_preis_kg_preis = getMengePriceAndPricePerKg(selectedArtikelID);
        String menge = menge_preis_kg_preis[0];
        String preis = menge_preis_kg_preis[1];
        String kg_preis = menge_preis_kg_preis[2];

        // for PreisschilderExport:
        artikelIDs.add(selectedArtikelID);
        articleNames.add(kurzname);
        mengen.add(menge);
        preise.add(preis);
        lieferanten.add(liefkurz);
        kg_preise.add(kg_preis);

        colors.add(color);
        types.add("artikel");
        removeButtons.add(new JButton("-"));
        removeButtons.lastElement().addActionListener(this);

        Vector<Object> row = new Vector<Object>();
            row.add(liefkurz);
            row.add(artikelName); row.add(artikelNummer); row.add(barcode);
            row.add(preis); row.add(vatFormatter(artikelMwSt));
            row.add(removeButtons.lastElement());
        data.add(row);

        updateAll();
    }

    private void artikelHinzufuegen() {
        hinzufuegen();
    }

    private void delete() {
        clearAll();
        updateAll();
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

    private void checkBarcodeBox(ActionEvent e) {
        if ( barcodeBox.getItemCount() == 1 ){ // if selection is correct and unique
            setArtikelNameAndNummerForBarcode();
        }
        checkIfFormIsComplete();
    }

    private void checkArtikelBox(ActionEvent e) {
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
        if (e.getDocument() == preisField.getDocument()){
            setButtonsEnabled();
            return;
        }
        if (e.getDocument() == barcodeField.getDocument()){
            if (barcodeBox.setBoxMode){ return; }
            System.out.println("\nbarcodeField DocumentListener fired!");
            System.out.println("selectedItem: "+barcodeBox.getSelectedItem());
            System.out.println("barcodeField text: "+barcodeField.getText()+"   barcodeText: "+barcodeText);
            if ( !barcodeField.getText().equals(barcodeText) ) { // some editing change in box
                resetFormFromBarcodeBox();
                barcodeText = barcodeField.getText();
            }
            checkIfFormIsComplete();
            return;
        }
        if (e.getDocument() == artikelField.getDocument()){
            if (artikelBox.setBoxMode){ return; }
            System.out.println("\nartikelField DocumentListener fired!");
            System.out.println("selectedItem: "+artikelBox.getSelectedItem());
            System.out.println("artikelField text: "+artikelField.getText()+"   artikelNameText: "+artikelNameText);
            if ( !artikelField.getText().equals(artikelNameText) ) { // some editing change in box
                resetFormFromArtikelBox();
                artikelNameText = artikelField.getText();
            }
            checkIfFormIsComplete();
            return;
        }
        if (e.getDocument() == nummerField.getDocument()){
            if (nummerBox.setBoxMode){ return; }
            System.out.println("\nnummerField DocumentListener fired!");
            System.out.println("selectedItem: "+nummerBox.getSelectedItem());
            System.out.println("nummerField text: "+nummerField.getText()+"   artikelNummerText: "+artikelNummerText);
            if ( !nummerField.getText().equals(artikelNummerText) ) { // some editing change in box
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
        if (e.getSource() == hinzufuegenButton){
            artikelHinzufuegen();
	    return;
	}
        if (e.getSource() == emptyBarcodeButton){
            barcodeText = "";
            barcodeBox.emptyBox();
            barcodeBox.requestFocus();
	    return;
	}
        if (e.getSource() == emptyArtikelButton){
            artikelNameText = "";
            artikelBox.emptyBox();
            artikelBox.requestFocus();
	    return;
	}
        if (e.getSource() == emptyNummerButton){
            artikelNummerText = "";
            nummerBox.emptyBox();
            nummerBox.requestFocus();
	    return;
	}
	if (e.getSource() == deleteButton){
            delete();
	    return;
	}
	if (e.getSource() == printButton){
            new PreisschilderExport(this.conn, this.mainWindow,
                    articleNames, mengen, preise, lieferanten, kg_preise);
	    return;
	}
	int removeRow = -1;
	for (int i=0; i<removeButtons.size(); i++){
	    if (e.getSource() == removeButtons.get(i) ){
		removeRow = i;
		break;
	    }
	}
        if (removeRow > -1){
            data.remove(removeRow);
            artikelIDs.remove(removeRow);
            articleNames.remove(removeRow);
            mengen.remove(removeRow);
            preise.remove(removeRow);
            lieferanten.remove(removeRow);
            kg_preise.remove(removeRow);
            colors.remove(removeRow);
            types.remove(removeRow);
            removeButtons.remove(removeRow);

            // remove extra rows (Rabatt oder Pfand):
            while ( removeRow < removeButtons.size() && removeButtons.get(removeRow) == null ){
                data.remove(removeRow);
                artikelIDs.remove(removeRow);
                articleNames.remove(removeRow);
                mengen.remove(removeRow);
                preise.remove(removeRow);
                lieferanten.remove(removeRow);
                kg_preise.remove(removeRow);
                colors.remove(removeRow);
                types.remove(removeRow);
                removeButtons.remove(removeRow);
            }

            updateAll();
            return;
        }
    }

}
