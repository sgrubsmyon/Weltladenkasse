package Weltladenbesteller;

// Basic Java stuff:
import java.util.*; // for Vector
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
//import javax.swing.JTextArea;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.*; // for DocumentFilter
import javax.swing.event.*;
//import java.beans.PropertyChangeEvent;
//import java.beans.PropertyChangeListener;

import WeltladenDB.BarcodeComboBox;
import WeltladenDB.ArtikelNameComboBox;
import WeltladenDB.ArtikelNummerComboBox;
import WeltladenDB.CurrencyDocumentFilter;
import WeltladenDB.JComponentCellRenderer;
import WeltladenDB.JComponentCellEditor;
import WeltladenDB.BoundsPopupMenuListener;

public class Bestellen extends BestellungsGrundlage implements ItemListener, DocumentListener {
    // Attribute:
    private final BigDecimal minusOne = new BigDecimal(-1);
    private final BigDecimal percent = new BigDecimal("0.01");

    private Bestellen myBestellen;

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
    private int selectedStueck;
    private JSpinner anzahlSpinner;
    private JTextField anzahlField;
    private JTextField vpeField;
    private JTextField preisField;
    // Buttons
    private JButton emptyBarcodeButton;
    private JButton emptyArtikelButton;
    private JButton emptyNummerButton;
    private JButton hinzufuegenButton;
    private Vector<JButton> removeButtons;
    private JButton speichernButton;

    // The panels
    private JPanel allPanel;
    private JPanel articleListPanel;

    // The table holding the purchase articles.
    private BestellungsTable myTable;
    private Vector< Vector<Object> > data;

    private Vector<Integer> artikelIDs;
    private Vector<Integer> stueckzahlen;

    private CurrencyDocumentFilter geldFilter = new CurrencyDocumentFilter();

    // Methoden:

    /**
     *    The constructor.
     *       */
    public Bestellen(Connection conn, MainWindow mw)
    {
	super(conn, mw);
        myBestellen = this;

        columnLabels.add("Entfernen");

//        // keyboard shortcuts:
//        KeyStroke barcodeShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK); // Ctrl-C
//        KeyStroke artikelNameShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK); // Ctrl-A
//        KeyStroke artikelNummerShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK); // Ctrl-N
//        KeyStroke zwischensummeShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK); // Ctrl-Z
//        KeyStroke barShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_B, Event.CTRL_MASK); // Ctrl-B
//        KeyStroke ecShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK); // Ctrl-E
//        KeyStroke stornierenShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK); // Ctrl-S
//
//        ShortcutListener shortcutListener = new ShortcutListener();
//
//        this.registerKeyboardAction(shortcutListener, "barcode", barcodeShortcut,
//                JComponent.WHEN_IN_FOCUSED_WINDOW);
//        this.registerKeyboardAction(shortcutListener, "name", artikelNameShortcut,
//                JComponent.WHEN_IN_FOCUSED_WINDOW);
//        this.registerKeyboardAction(shortcutListener, "nummer", artikelNummerShortcut,
//                JComponent.WHEN_IN_FOCUSED_WINDOW);
//        this.registerKeyboardAction(shortcutListener, "zws", zwischensummeShortcut,
//                JComponent.WHEN_IN_FOCUSED_WINDOW);
//        this.registerKeyboardAction(shortcutListener, "bar", barShortcut,
//                JComponent.WHEN_IN_FOCUSED_WINDOW);
//        this.registerKeyboardAction(shortcutListener, "ec", ecShortcut,
//                JComponent.WHEN_IN_FOCUSED_WINDOW);
//        this.registerKeyboardAction(shortcutListener, "stornieren", stornierenShortcut,
//                JComponent.WHEN_IN_FOCUSED_WINDOW);

        emptyTable();
	showAll();
        barcodeBox.requestFocus();
    }

//    // listener for keyboard shortcuts
//    private class ShortcutListener implements ActionListener {
//        public void actionPerformed(ActionEvent e) {
//            if (e.getActionCommand() == "barcode"){
//                barcodeBox.requestFocus();
//                return;
//            }
//            if (e.getActionCommand() == "name"){
//                artikelBox.requestFocus();
//                return;
//            }
//            if (e.getActionCommand() == "nummer"){
//                nummerBox.requestFocus();
//                return;
//            }
//            if (e.getActionCommand() == "zws"){
//                if (zwischensummeButton.isEnabled())
//                    zwischensumme();
//                return;
//            }
//            if (e.getActionCommand() == "bar"){
//                if (barButton.isEnabled())
//                    bar();
//                return;
//            }
//            if (e.getActionCommand() == "ec"){
//                if (ecButton.isEnabled())
//                    ec();
//                return;
//            }
//            if (e.getActionCommand() == "stornieren"){
//                if (stornoButton.isEnabled())
//                    stornieren();
//                return;
//            }
//        }
//    }

    void showAll(){
	allPanel = new JPanel();
	allPanel.setLayout(new BoxLayout(allPanel, BoxLayout.Y_AXIS));

        JPanel barcodePanel = new JPanel();
	barcodePanel.setLayout(new FlowLayout());
	    JLabel barcodeLabel = new JLabel("Barcode: ");
            barcodeLabel.setLabelFor(barcodeBox);
            barcodeLabel.setDisplayedMnemonic(KeyEvent.VK_C);
            barcodePanel.add(barcodeLabel);
            String filterStr = " AND variabler_preis = FALSE AND toplevel_id IS NOT NULL ";
            barcodeBox = new BarcodeComboBox(this.conn, filterStr);
            barcodeBox.addActionListener(this);
            //barcodeBox.addItemListener(this);
            barcodeBox.addPopupMouseListener(new MouseListenerBarcodeBox());
            barcodeField = (JTextComponent)barcodeBox.getEditor().getEditorComponent();
            barcodeField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK), "none");
                // remove Ctrl-A key binding
	    barcodeField.getDocument().addDocumentListener(this);
            barcodePanel.add(barcodeBox);
	    emptyBarcodeButton = new JButton("x");
	    emptyBarcodeButton.addActionListener(this);
	    barcodePanel.add(emptyBarcodeButton);
        allPanel.add(barcodePanel);

	JPanel chooseArticlePanel1 = new JPanel();
	chooseArticlePanel1.setLayout(new FlowLayout());
	    JLabel artikelLabel = new JLabel("Artikelname: ");
            artikelLabel.setLabelFor(artikelBox);
            artikelLabel.setDisplayedMnemonic(KeyEvent.VK_A);
            chooseArticlePanel1.add(artikelLabel);
            artikelBox = new ArtikelNameComboBox(this.conn, filterStr);
            artikelBox.addActionListener(this);
            //artikelBox.addItemListener(this);
            artikelBox.addPopupMouseListener(new MouseListenerArtikelBox());
            // set preferred width etc.:
            artikelBox.addPopupMenuListener(new BoundsPopupMenuListener(false, true, 50, false));
            artikelBox.setPrototypeDisplayValue("qqqqqqqqqqqqqqqqqqqq");
            artikelField = (JTextComponent)artikelBox.getEditor().getEditorComponent();
	    artikelField.getDocument().addDocumentListener(this);
            chooseArticlePanel1.add(artikelBox);
	    emptyArtikelButton = new JButton("x");
	    emptyArtikelButton.addActionListener(this);
	    chooseArticlePanel1.add(emptyArtikelButton);

	    JLabel nummerLabel = new JLabel("Artikelnr.: ");
            nummerLabel.setLabelFor(nummerBox);
            nummerLabel.setDisplayedMnemonic(KeyEvent.VK_N);
            chooseArticlePanel1.add(nummerLabel);
            nummerBox = new ArtikelNummerComboBox(this.conn, filterStr);
            nummerBox.addActionListener(this);
            //nummerBox.addItemListener(this);
            nummerBox.addPopupMouseListener(new MouseListenerNummerBox());
            // set preferred width etc.:
            nummerBox.addPopupMenuListener(new BoundsPopupMenuListener(false, true, 30, false));
            nummerBox.setPrototypeDisplayValue("qqqqqqqq");
            nummerField = (JTextComponent)nummerBox.getEditor().getEditorComponent();
            nummerField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK), "none");
                // remove Ctrl-A key binding
	    nummerField.getDocument().addDocumentListener(this);
            chooseArticlePanel1.add(nummerBox);
	    emptyNummerButton = new JButton("x");
	    emptyNummerButton.addActionListener(this);
	    chooseArticlePanel1.add(emptyNummerButton);
        allPanel.add(chooseArticlePanel1);

	JPanel chooseArticlePanel2 = new JPanel();
	chooseArticlePanel2.setLayout(new FlowLayout());
	    JLabel anzahlLabel = new JLabel("Anzahl: ");
            chooseArticlePanel2.add(anzahlLabel);
            SpinnerNumberModel anzahlModel = new SpinnerNumberModel(1, // initial value
                                                                    1, // min
                                                                    null, // max (null == no max)
                                                                    1); // step
	    anzahlSpinner = new JSpinner(anzahlModel);
            JSpinner.NumberEditor anzahlEditor = new JSpinner.NumberEditor(anzahlSpinner, "###");
            anzahlField = anzahlEditor.getTextField();
            anzahlField.getDocument().addDocumentListener(this);
            anzahlField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK), "none");
                // remove Ctrl-A key binding
            anzahlField.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
                        if (preisField.isEditable())
                            preisField.requestFocus();
                        else {
                            if (hinzufuegenButton.isEnabled()){
                                anzahlSpinner.setValue(Integer.parseInt(anzahlField.getText()));
                                hinzufuegenButton.doClick();
                            }
                        }
                    }
                }
            });
            anzahlSpinner.setEditor(anzahlEditor);
            ( (NumberFormatter) anzahlEditor.getTextField().getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
            anzahlField.setColumns(3);
	    anzahlLabel.setLabelFor(anzahlSpinner);
            chooseArticlePanel2.add(anzahlSpinner);

            JLabel vpeLabel = new JLabel("VPE: ");
            chooseArticlePanel2.add(vpeLabel);
            vpeField = new JTextField("");
            vpeField.setEditable(false);
            vpeField.setColumns(3);
            vpeField.setHorizontalAlignment(JTextField.RIGHT);
            vpeLabel.setLabelFor(vpeField);
            chooseArticlePanel2.add(vpeField);

	    JLabel preisLabel = new JLabel("Preis: ");
            chooseArticlePanel2.add(preisLabel);
            preisField = new JTextField("");
            preisField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK), "none");
                // remove Ctrl-A key binding
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
            chooseArticlePanel2.add(preisField);
            chooseArticlePanel2.add(new JLabel(currencySymbol));

	    hinzufuegenButton = new JButton("Hinzufügen");
            hinzufuegenButton.setMnemonic(KeyEvent.VK_H);
	    hinzufuegenButton.addActionListener(this);
	    hinzufuegenButton.setEnabled(false);
	    chooseArticlePanel2.add(hinzufuegenButton);
        allPanel.add(chooseArticlePanel2);

	showTable();

        JPanel speichernPanel = new JPanel();
        speichernPanel.setLayout(new BoxLayout(speichernPanel, BoxLayout.Y_AXIS));
            speichernButton = new JButton("Bestellung speichern");
            speichernButton.setEnabled(false);
            speichernButton.addActionListener(this);
            speichernButton.setAlignmentX(JComponent.CENTER_ALIGNMENT);
            speichernPanel.add(speichernButton);
        allPanel.add(speichernPanel);

	this.add(allPanel, BorderLayout.CENTER);
    }


    void showTable(){
	myTable = new BestellungsTable(data, columnLabels);
        myTable.setColEditableTrue(columnLabels.size()-1); // last column has buttons
	myTable.setDefaultRenderer( JComponent.class, new JComponentCellRenderer() );
	myTable.setDefaultEditor( JComponent.class, new JComponentCellEditor() );
//	myTable.setBounds(71,53,150,100);
//	myTable.setToolTipText("Tabelle kann nur gelesen werden.");
	setTableProperties(myTable);
	TableColumn entf = myTable.getColumn("Entfernen");
	entf.setPreferredWidth(2);
//	myTable.setAutoResizeMode(5);

	articleListPanel = new JPanel();
	articleListPanel.setLayout(new BoxLayout(articleListPanel, BoxLayout.Y_AXIS));
	articleListPanel.setBorder(BorderFactory.createTitledBorder("Gewählte Artikel"));

            JScrollPane scrollPane = new JScrollPane(myTable);
            articleListPanel.add(scrollPane);

            //JPanel totalPricePanel = createTotalPricePanel();
            //articleListPanel.add(totalPricePanel);

	allPanel.add(articleListPanel);
    }

    void emptyTable(){
	data = new Vector< Vector<Object> >();
        artikelIDs = new Vector<Integer>();
        preise = new Vector<String>();
        mwsts = new Vector<String>();
        colors = new Vector<String>();
        types = new Vector<String>();
        stueckzahlen = new Vector<Integer>();
        removeButtons = new Vector<JButton>();
    }

    private void clearAll(){
        data.clear();
        artikelIDs.clear();
        preise.clear();
        colors.clear();
        types.clear();
        mwsts.clear();
        stueckzahlen.clear();
        removeButtons.clear();
    }

    private void updateAll(){
	this.remove(allPanel);
	this.revalidate();
	showAll();
        barcodeBox.requestFocus();
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
            // Create statement for MySQL database
            Statement stmt = this.conn.createStatement();
            // Run MySQL command
            ResultSet rs = stmt.executeQuery(
                    "SELECT DISTINCT a.artikel_name, l.lieferant_name, a.artikel_nr FROM artikel AS a " +
                    "LEFT JOIN lieferant AS l USING (lieferant_id) " +
                    "WHERE a.barcode = '"+barcode+"' " +
                    "AND a.aktiv = TRUE"
                    );
            // Now do something with the ResultSet, should be only one result ...
            while ( rs.next() ){
                String lieferant = rs.getString(2) != null ? rs.getString(2) : "";
                artikelNamen.add( new String[]{rs.getString(1), lieferant} );
                artikelNummern.add( new String[]{rs.getString(3)} );
            }
            rs.close();
            stmt.close();
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
            // Create statement for MySQL database
            Statement stmt = this.conn.createStatement();
            // Run MySQL command
            ResultSet rs = stmt.executeQuery(
                    "SELECT DISTINCT a.artikel_name, l.lieferant_name FROM artikel AS a " +
                    "LEFT JOIN lieferant AS l USING (lieferant_id) " +
                    "WHERE a.artikel_nr = '"+artikelNummer+"' " +
                    "AND a.aktiv = TRUE"
                    );
            // Now do something with the ResultSet, should be only one result ...
            while ( rs.next() ){
                String lieferant = rs.getString(2) != null ? rs.getString(2) : "";
                artikelNamen.add( new String[]{rs.getString(1), lieferant} );
            }
            rs.close();
            stmt.close();
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
        String lieferantQuery = an[1].equals("") ? "IS NULL" : "= '"+an[1]+"'";
        Vector<String[]> artikelNummern = new Vector<String[]>();
        // get artikelNummer for artikelName
        try {
            // Create statement for MySQL database
            Statement stmt = this.conn.createStatement();
            // Run MySQL command
            ResultSet rs = stmt.executeQuery(
                    "SELECT DISTINCT a.artikel_nr FROM artikel AS a " +
                    "LEFT JOIN lieferant AS l USING (lieferant_id) " +
                    "WHERE a.artikel_name = '"+artikelName+"' AND l.lieferant_name "+lieferantQuery+" " +
                    "AND a.aktiv = TRUE"
                    );
            // Now do something with the ResultSet, should be only one result ...
            while ( rs.next() ){
                artikelNummern.add( new String[]{rs.getString(1)} );
            }
            rs.close();
            stmt.close();
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
            String artikelPreis = getPrice(selectedArtikelID);
            preisField.getDocument().removeDocumentListener(this);
            preisField.setText("");
            System.out.println("Setze Preis auf: *"+artikelPreis.replace('.',',')+"*");
            preisField.setText(artikelPreis.replace('.',','));
            preisField.getDocument().addDocumentListener(this);
        }
        else {
            preisField.setEditable(true);
        }
    }

    private void setButtonsEnabled() {
        if (preisField.getText().length() > 0) {
            hinzufuegenButton.setEnabled(true);
        }
        else {
            hinzufuegenButton.setEnabled(false);
        }
    }

    private void updateAnzahlColor(Integer vpe) {
        if (vpe <= 0){
            anzahlField.setForeground(Color.black);
        } else {
            Integer stueck = (Integer)anzahlSpinner.getValue();
            if (stueck < vpe){
                anzahlField.setForeground(Color.red);
            } else {
                anzahlField.setForeground(Color.green.darker().darker());
            }
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
            selectedArtikelID = getArticleID(artikelName, lieferant, artikelNummer); // get the internal artikelID from the DB
            String vpe = getVPE(selectedArtikelID);
            Integer vpeInt = vpe.length() > 0 ? Integer.parseInt(vpe) : 0;
            if (vpeInt > 0){
                anzahlSpinner.setValue(vpeInt);
            } else {
                anzahlSpinner.setValue(1);
            }
            setPriceField();
            vpeField.setText(vpe);
            updateAnzahlColor(vpeInt);
            anzahlField.requestFocus();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    anzahlField.selectAll();
                }
            });
        }
        setButtonsEnabled();
    }

    private void hinzufuegen(Integer stueck) {
        if (artikelBox.getItemCount() != 1 || nummerBox.getItemCount() != 1){
            System.out.println("Error: article not selected unambiguously.");
            return;
        }
        String[] an = artikelBox.parseArtikelName();
        String artikelName = an[0];
        String lieferant = an[1];
        String artikelNummer = (String)nummerBox.getSelectedItem();
        selectedStueck = stueck;
        String vpe = vpeField.getText();
        Integer vpeInt = vpe.length() > 0 ? Integer.parseInt(vpe) : 0;
        String artikelPreis = priceFormatterIntern(new BigDecimal( preisField.getText().replace(',','.') ));
        String artikelMwSt = getVAT(selectedArtikelID);

        artikelIDs.add(selectedArtikelID);
        stueckzahlen.add(stueck);
        preise.add(artikelPreis);
        String color = "";
        if (vpeInt <= 0){
            color = "default";
        } else {
            if (stueck < vpeInt){
                color = "red";
            } else {
                color = "green";
            }
        }
        colors.add(color);
        types.add("artikel");
        mwsts.add(artikelMwSt);
        removeButtons.add(new JButton("-"));
        removeButtons.lastElement().addActionListener(this);

        artikelPreis = artikelPreis.replace('.',',')+' '+currencySymbol;
        artikelMwSt = vatFormatter(artikelMwSt);

        Vector<Object> row = new Vector<Object>();
            row.add(lieferant); row.add(artikelNummer); row.add(artikelName);
            row.add(artikelPreis); row.add(vpe); row.add(stueck.toString());
            row.add(removeButtons.lastElement());
        data.add(row);

        updateAll();
    }

    private void artikelHinzufuegen() {
        Integer stueck = (Integer)anzahlSpinner.getValue();
        hinzufuegen(stueck);
    }

    private void speichern() {
    }

    private void stornieren() {
        clearAll();
        updateAll();
    }

    private void addToHashMap(HashMap<BigDecimal, BigDecimal> hashMap, int artikelIndex){
        BigDecimal mwst = new BigDecimal(mwsts.get(artikelIndex));
        BigDecimal gesPreis = new BigDecimal(preise.get(artikelIndex));
        boolean found = false;
        for ( Map.Entry<BigDecimal, BigDecimal> entry : hashMap.entrySet() ){
            if (entry.getKey().compareTo(mwst) == 0){
                entry.setValue( entry.getValue().add(gesPreis) );
                found = true;
                break;
            }
        }
        if (!found){ // make new entry
            hashMap.put(mwst, gesPreis);
        }
    }

    private void resetFormFromBarcodeBox() {
        artikelNameText = "";
        artikelNummerText = "";
        artikelBox.emptyBox();
        nummerBox.emptyBox();
        vpeField.setText("");
        preisField.setText("");
        preisField.setEditable(false);
    }
    private void resetFormFromArtikelBox() {
        System.out.println("resetting form from artikel box.");
        barcodeText = "";
        artikelNummerText = "";
        barcodeBox.emptyBox();
        nummerBox.emptyBox();
        vpeField.setText("");
        preisField.setText("");
        preisField.setEditable(false);
    }
    private void resetFormFromNummerBox() {
        System.out.println("resetting form from nummer box.");
        barcodeText = "";
        artikelNameText = "";
        barcodeBox.emptyBox();
        artikelBox.emptyBox();
        vpeField.setText("");
        preisField.setText("");
        preisField.setEditable(false);
    }

    private void checkBarcodeBox(ActionEvent e) {
        System.out.println("actionPerformed in barcodeBox, actionCommand: "+e.getActionCommand()+", modifiers: "+e.getModifiers()+", itemCount: "+barcodeBox.getItemCount());
        if ( e.getActionCommand().equals("comboBoxEdited") || // if enter was pressed
                ( e.getActionCommand().equals("comboBoxChanged") && e.getModifiers() == 16 ) // if mouse button was clicked
           ){
            System.out.println("Enter or mouse click in barcodeBox, itemCount: "+barcodeBox.getItemCount());
            if ( barcodeBox.getItemCount() == 1 ){ // if selection is correct and unique
                setArtikelNameAndNummerForBarcode();
            //} else {
            //    resetFormFromBarcodeBox();
            }
        }
        checkIfFormIsComplete();
    }
    private void checkArtikelBox(ActionEvent e) {
        System.out.println("actionPerformed in artikelBox, actionCommand: "+e.getActionCommand()+", modifiers: "+e.getModifiers()+", itemCount: "+artikelBox.getItemCount()+", selectedItem: "+artikelBox.getSelectedItem()+"   artikelNameText: "+artikelNameText);
        if ( e.getActionCommand().equals("comboBoxEdited") || // if enter was pressed
                ( e.getActionCommand().equals("comboBoxChanged") && e.getModifiers() == 16 ) // if mouse button was clicked
           ){
            System.out.println("Enter or mouse click in artikelBox, itemCount: "+artikelBox.getItemCount());
            if ( artikelBox.getItemCount() == 1 ){ // if selection is correct and unique
                setArtikelNummerForName();
            //} else {
            //    resetFormFromArtikelBox();
            }
        }
        checkIfFormIsComplete();
    }
    private void checkNummerBox(ActionEvent e) {
        System.out.println("actionPerformed in nummerBox, actionCommand: "+e.getActionCommand()+", modifiers: "+e.getModifiers()+", itemCount: "+nummerBox.getItemCount()+", selectedItem: "+nummerBox.getSelectedItem()+"   artikelNummerText: "+artikelNummerText);
        if ( e.getActionCommand().equals("comboBoxEdited") || // if enter was pressed
                ( e.getActionCommand().equals("comboBoxChanged") && e.getModifiers() == 16 ) // if mouse button was clicked
           ){
            System.out.println("Enter or mouse click in nummerBox, itemCount: "+nummerBox.getItemCount());
            if ( nummerBox.getItemCount() == 1 ){ // if selection is correct and unique
                setArtikelNameForNummer();
            //} else {
            //    resetFormFromNummerBox();
            }
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
            System.out.println("barcodeField DocumentListener fired!");
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
            System.out.println("artikelField DocumentListener fired!");
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
            System.out.println("nummerField DocumentListener fired!");
            System.out.println("selectedItem: "+nummerBox.getSelectedItem());
            System.out.println("nummerField text: "+nummerField.getText()+"   artikelNummerText: "+artikelNummerText);
            if ( !nummerField.getText().equals(artikelNummerText) ) { // some editing change in box
                resetFormFromNummerBox();
                artikelNummerText = nummerField.getText();
            }
            checkIfFormIsComplete();
            return;
        }
        if (e.getDocument() == anzahlField.getDocument()){
            String vpe = getVPE(selectedArtikelID);
            Integer vpeInt = vpe.length() > 0 ? Integer.parseInt(vpe) : 0;
            updateAnzahlColor(vpeInt);
        }
    }
    public void removeUpdate(DocumentEvent e) {
        insertUpdate(e);
    }
    public void changedUpdate(DocumentEvent e) {
	// Plain text components do not fire these events
    }

    /**
     *    * Each non abstract class that implements the ItemListener
     *      must have this method.
     *
     *    @param e the item event.
     **/
    public void itemStateChanged(ItemEvent e) {
        //if (e.getSource() == barcodeBox){
        //    checkBarcodeBox();
        //    return;
        //}
        //if (e.getSource() == artikelBox){
        //    checkArtikelBox();
        //    return;
        //}
        //if (e.getSource() == nummerBox){
        //    checkNummerBox();
        //    return;
        //}
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
	if (e.getSource() == speichernButton){
            speichern();
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
            preise.remove(removeRow);
            colors.remove(removeRow);
            types.remove(removeRow);
            mwsts.remove(removeRow);
            stueckzahlen.remove(removeRow);
            removeButtons.remove(removeRow);
            // remove extra rows (Rabatt oder Pfand):
            while ( removeRow < removeButtons.size() && removeButtons.get(removeRow) == null ){
                data.remove(removeRow);
                artikelIDs.remove(removeRow);
                preise.remove(removeRow);
                colors.remove(removeRow);
                types.remove(removeRow);
                mwsts.remove(removeRow);
                stueckzahlen.remove(removeRow);
                removeButtons.remove(removeRow);
            }
            updateAll();
            return;
        }
    }
}
