package Weltladenbesteller;

// Basic Java stuff:
import java.util.*; // for Vector
import java.io.*; // for File
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
//import javax.swing.JTextArea;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.*; // for DocumentFilter
import javax.swing.event.*;
//import java.beans.PropertyChangeEvent;
//import java.beans.PropertyChangeListener;

import WeltladenDB.MainWindowGrundlage;
import WeltladenDB.BarcodeComboBox;
import WeltladenDB.ArtikelNameComboBox;
import WeltladenDB.ArtikelNummerComboBox;
import WeltladenDB.StringDocumentFilter;
import WeltladenDB.BoundsPopupMenuListener;

public class Bestellen extends BestellungsGrundlage implements ItemListener, DocumentListener {
    // Attribute:
    private final String backupFilename = System.getProperty("user.home")+fileSep+".Weltladenkasse_Bestellung.backup";
    private final BigDecimal minusOne = new BigDecimal(-1);
    private final BigDecimal percent = new BigDecimal("0.01");

    protected int selBestellNr = -1;
    protected String selTyp = "";
    protected int selJahr = -1;
    protected int selKW = -1;

    private TabbedPane tabbedPane;

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
    private JSpinner vpeSpinner;
    private JTextField anzahlField;
    private JTextField vpeSpinnerField;
    private boolean vpeOrAnzahlIsChanged = false;
    private JTextField vpeField;
    private JTextField preisField;
    private JSpinner jahrSpinner;
    private JTextField jahrField;
    private JSpinner kwSpinner;
    private JTextField kwField;
    private JTextField typField;
    private JTextField filterField;
    // Buttons
    private JButton emptyBarcodeButton;
    private JButton emptyArtikelButton;
    private JButton emptyNummerButton;
    private JButton hinzufuegenButton;
    private Vector<JButton> removeButtons;
    private JButton abschliessenButton;
    private JButton verwerfenButton;

    // The panels
    private JPanel allPanel;
    private JPanel articleListPanel;
    private JScrollPane articleScrollPane;
    private JPanel abschliessenPanel;

    // The table holding the purchase articles.
    private BestellungsTable orderTable;
    protected Vector< Vector<Object> > data; // holding full/original data
    protected Vector< Vector<Object> > displayData; // holding the subset of data used for display
                                                    // (after applying filter)
    private Vector<Integer> displayIndices; // holding indices that rows in displayData have in data
    private Vector<Integer> artikelIDs;
    private Vector<Boolean> sortimentBools;
    private Vector<Integer> stueckzahlen;
    private Vector<Integer> positions;

    private String filterStr = "";

    // Methoden:

    /**
     *    The constructor.
     *       */
    public Bestellen(Connection conn, MainWindowGrundlage mw, TabbedPane tp)
    {
	super(conn, mw);
        tabbedPane = tp;

        columnLabels.add("Entfernen");

        // keyboard shortcuts:
        KeyStroke barcodeShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK); // Ctrl-C
        KeyStroke artikelNameShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK); // Ctrl-A
        KeyStroke artikelNummerShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK); // Ctrl-N
        //KeyStroke zwischensummeShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK); // Ctrl-Z
        //KeyStroke barShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_B, Event.CTRL_MASK); // Ctrl-B
        //KeyStroke ecShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK); // Ctrl-E
        //KeyStroke stornierenShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK); // Ctrl-S

        ShortcutListener shortcutListener = new ShortcutListener();

        this.registerKeyboardAction(shortcutListener, "barcode", barcodeShortcut,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.registerKeyboardAction(shortcutListener, "name", artikelNameShortcut,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.registerKeyboardAction(shortcutListener, "nummer", artikelNummerShortcut,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        //this.registerKeyboardAction(shortcutListener, "zws", zwischensummeShortcut,
        //        JComponent.WHEN_IN_FOCUSED_WINDOW);
        //this.registerKeyboardAction(shortcutListener, "bar", barShortcut,
        //        JComponent.WHEN_IN_FOCUSED_WINDOW);
        //this.registerKeyboardAction(shortcutListener, "ec", ecShortcut,
        //        JComponent.WHEN_IN_FOCUSED_WINDOW);
        //this.registerKeyboardAction(shortcutListener, "stornieren", stornierenShortcut,
        //        JComponent.WHEN_IN_FOCUSED_WINDOW);

        emptyTable();
	showAll();
        doCSVBackupReadin();
        barcodeBox.requestFocus();
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

        JPanel datePanel = new JPanel();
	datePanel.setLayout(new FlowLayout());
            datePanel.add(new JLabel("Rechnung für"));
            /////
	    JLabel jahrLabel = new JLabel("Jahr:");
            datePanel.add(jahrLabel);
            Calendar rightNow = Calendar.getInstance();
            int year = rightNow.get(Calendar.YEAR);
            if (selJahr > 0){
                year = selJahr;
            }
            SpinnerNumberModel jahrModel = new SpinnerNumberModel(year, // initial value
                                                                  0, // min
                                                                  null, // max (null == no max)
                                                                  1); // step
	    jahrSpinner = new JSpinner(jahrModel);
            JSpinner.NumberEditor jahrEditor = new JSpinner.NumberEditor(jahrSpinner, "####");
            jahrField = jahrEditor.getTextField();
            jahrField.getDocument().addDocumentListener(this);
            jahrSpinner.setEditor(jahrEditor);
            ( (NumberFormatter) jahrEditor.getTextField().getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
            jahrField.setColumns(4);
	    jahrLabel.setLabelFor(jahrSpinner);
            datePanel.add(jahrSpinner);
            /////
	    JLabel kwLabel = new JLabel("KW:");
            datePanel.add(kwLabel);
            int week = rightNow.get(Calendar.WEEK_OF_YEAR)+1; // default: following week
            if (selKW > 0){
                week = selKW;
            }
            SpinnerNumberModel kwModel = new SpinnerNumberModel(week, // initial value
                                                                1, // min
                                                                53, // max (null == no max)
                                                                1); // step
	    kwSpinner = new JSpinner(kwModel);
            JSpinner.NumberEditor kwEditor = new JSpinner.NumberEditor(kwSpinner, "##");
            kwField = kwEditor.getTextField();
            kwField.getDocument().addDocumentListener(this);
            kwSpinner.setEditor(kwEditor);
            ( (NumberFormatter) kwEditor.getTextField().getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
            kwField.setColumns(2);
	    kwLabel.setLabelFor(kwSpinner);
            datePanel.add(kwSpinner);
            ///////
	    JLabel typLabel = new JLabel("Typ:");
            datePanel.add(typLabel);
            String typ = "LM";
            if (selTyp.length() > 0){
                typ = selTyp;
            }
            typField = new JTextField(typ);
            StringDocumentFilter sdf = new StringDocumentFilter(12);
	    ((AbstractDocument)typField.getDocument()).setDocumentFilter(sdf);
            typField.getDocument().addDocumentListener(this);
            typField.setColumns(6);
	    typLabel.setLabelFor(typField);
            datePanel.add(typField);
            ///////
            datePanel.add(new JLabel("Bestell-Nr.:"));
            JTextField bestNrField = new JTextField("");
            bestNrField.setColumns(6);
            bestNrField.setHorizontalAlignment(JTextField.RIGHT);
            if (selBestellNr > 0){
                bestNrField.setText(new Integer(selBestellNr).toString());
            }
            bestNrField.setEditable(false);
            datePanel.add(bestNrField);
        allPanel.add(datePanel);

        JPanel barcodePanel = new JPanel();
	barcodePanel.setLayout(new FlowLayout());
	    JLabel barcodeLabel = new JLabel("Barcode: ");
            barcodeLabel.setLabelFor(barcodeBox);
            barcodeLabel.setDisplayedMnemonic(KeyEvent.VK_C);
            barcodePanel.add(barcodeLabel);
            String comboBoxFilterStr = " AND variabler_preis = FALSE AND toplevel_id IS NOT NULL ";
            barcodeBox = new BarcodeComboBox(this.conn, comboBoxFilterStr);
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
            artikelBox = new ArtikelNameComboBox(this.conn, comboBoxFilterStr);
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
            nummerBox = new ArtikelNummerComboBox(this.conn, comboBoxFilterStr);
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

            SpinnerNumberModel vpeModel = new SpinnerNumberModel(1, // initial value
                                                                    0, // min
                                                                    null, // max (null == no max)
                                                                    1); // step
	    vpeSpinner = new JSpinner(vpeModel);
            JSpinner.NumberEditor vpeEditor = new JSpinner.NumberEditor(vpeSpinner, "###");
            vpeSpinnerField = vpeEditor.getTextField();
            vpeSpinnerField.getDocument().addDocumentListener(this);
            vpeSpinnerField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK), "none");
                // remove Ctrl-A key binding
            vpeSpinnerField.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
                        if (preisField.isEditable())
                            preisField.requestFocus();
                        else {
                            if (hinzufuegenButton.isEnabled()){
                                vpeSpinner.setValue(Integer.parseInt(vpeSpinnerField.getText()));
                                hinzufuegenButton.doClick();
                            }
                        }
                    }
                }
            });
            vpeSpinner.setEditor(vpeEditor);
            ( (NumberFormatter) vpeEditor.getTextField().getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
            vpeSpinnerField.setColumns(3);
            chooseArticlePanel2.add(vpeSpinner);

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

	this.add(allPanel, BorderLayout.CENTER);
    }

    void initiateTable() {
	orderTable = new BestellungsTable(displayData, columnLabels, displayIndices, sortimentBools);
//	orderTable.setBounds(71,53,150,100);
//	orderTable.setToolTipText("Tabelle kann nur gelesen werden.");
	setTableProperties(orderTable);
	TableColumn entf = orderTable.getColumn("Entfernen");
	entf.setPreferredWidth(200);
//	orderTable.setAutoResizeMode(5);
    }

    void showTable(){
        initiateTable();

	articleListPanel = new JPanel();
	articleListPanel.setLayout(new BoxLayout(articleListPanel, BoxLayout.Y_AXIS));
	articleListPanel.setBorder(BorderFactory.createTitledBorder("Gewählte Artikel"));

            //articleScrollPane = new ScrollPane();
            //articleScrollPane.add(orderTable);
            articleScrollPane = new JScrollPane(orderTable);
            articleListPanel.add(articleScrollPane);

	allPanel.add(articleListPanel);

        abschliessenPanel = new JPanel();
        abschliessenPanel.setLayout(new FlowLayout());
            abschliessenButton = new JButton("Bestellung abschließen");
            abschliessenButton.setEnabled(false);
            abschliessenButton.addActionListener(this);
            abschliessenButton.setAlignmentX(JComponent.CENTER_ALIGNMENT);
            abschliessenPanel.add(abschliessenButton);

            verwerfenButton = new JButton("Verwerfen");
            verwerfenButton.setEnabled(false);
            verwerfenButton.addActionListener(this);
            verwerfenButton.setAlignmentX(JComponent.CENTER_ALIGNMENT);
            abschliessenPanel.add(verwerfenButton);

            JLabel filterLabel = new JLabel("Filter:");
            filterLabel.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
            abschliessenPanel.add(filterLabel);
            filterField = new JTextField("");
            filterField.setColumns(20);
            filterField.getDocument().addDocumentListener(this);
            filterField.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
            abschliessenPanel.add(filterField);
        allPanel.add(abschliessenPanel);
    }

    void emptyTable(){
	data = new Vector< Vector<Object> >();
        displayData = new Vector< Vector<Object> >();
        displayIndices = new Vector<Integer>();
        sortimentBools = new Vector<Boolean>();
        orderTable = new BestellungsTable(displayData, columnLabels, displayIndices, sortimentBools);
        artikelIDs = new Vector<Integer>();
        stueckzahlen = new Vector<Integer>();
        positions = new Vector<Integer>();
        removeButtons = new Vector<JButton>();
    }

    private void clearAll(){
        data.clear();
        displayData.clear();
        displayIndices.clear();
        artikelIDs.clear();
        sortimentBools.clear();
        stueckzahlen.clear();
        positions.clear();
        removeButtons.clear();
        selBestellNr = -1;
        selTyp = "";
        selJahr = -1;
        selKW = -1;

        setButtonsEnabled();
    }

    protected void updateAll(){
        // save a CSV backup to hard disk
        doCSVBackup();
	this.remove(allPanel);
	this.revalidate();
            // create table anew
            showAll();
            filterStr = filterField.getText();
            applyFilter();
            updateTable();
        barcodeBox.requestFocus();
        setButtonsEnabled(); // for abschliessenButton
    }

    private void updateTable(){
        articleListPanel.remove(articleScrollPane);
	articleListPanel.revalidate();
        initiateTable();
        //articleScrollPane = new ScrollPane();
        //articleScrollPane.add(orderTable);
        articleScrollPane = new JScrollPane(orderTable);
        articleListPanel.add(articleScrollPane);
        setButtonsEnabled();
    }

    protected int numberOfRows() {
        return data.size();
    }





    // CSV export:
    private void doCSVBackup() {
        File file = new File(backupFilename);

        String fileStr = "";
        // general infos:
        fileStr += "#bestellNr;Jahr;KW"+lineSep;
        fileStr += selBestellNr + delimiter;
        fileStr += selTyp + delimiter;
        fileStr += selJahr + delimiter;
        fileStr += selKW + lineSep;
        // format of csv file:
        fileStr += "#Lieferant;Art.-Nr.;Artikelname;VK-Preis;VPE;Stueck;sortiment;artikelID"+lineSep;
        for (int i=data.size()-1; i>=0; i--){
            String lieferant = (String)data.get(i).get(1);
            String nummer = (String)data.get(i).get(2);
            String name = (String)data.get(i).get(3);
            String vkp = (String)data.get(i).get(4); vkp = vkp == null ? "" : vkp;
            String vpe = (String)data.get(i).get(5); vpe = vpe == null ? "" : vpe;
            String stueck = (String)data.get(i).get(6);
            String sortiment = sortimentBools.get(i).toString();
            String artikelID = artikelIDs.get(i).toString();

            fileStr += lieferant + delimiter;
            fileStr += nummer + delimiter;
            fileStr += name + delimiter;
            fileStr += vkp + delimiter;
            fileStr += vpe + delimiter;
            fileStr += stueck + delimiter;
            fileStr += sortiment + delimiter;
            fileStr += artikelID + lineSep;
        }

        System.out.println(fileStr);

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(fileStr);
        } catch (Exception ex) {
            System.out.println("Error writing to file " + file.getName());
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception ex) {
                System.out.println("Error closing file " + file.getName());
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }




    private void doCSVBackupReadin() {
        File file = new File(backupFilename);

        try {
            // use Reader classes for text files:
            BufferedReader in = new BufferedReader(new FileReader(file)); // Lesen einer Textdatei mit Default Zeichensatz-Codierung, see http://www.wsoftware.de/practices/charsets.html
            String line;
            // parse general info at top:
            while ( (line = in.readLine()) != null) {
                line = line.replaceAll("#.*",""); // remove commented lines
                // get the fields
                String[] fields = line.split(delimiter);
                if (fields.length < 3 ){
                    continue;
                }

                selBestellNr = Integer.parseInt(fields[0]);
                selTyp = fields[1];
                selJahr = Integer.parseInt(fields[2]);
                selKW = Integer.parseInt(fields[3]);

                break;
            }
            // parse articles:
            while ( (line = in.readLine()) != null) {
                line = line.replaceAll("#.*",""); // remove commented lines
                // get the fields
                String[] fields = line.split(delimiter);
                if (fields.length < 7 ){
                    continue;
                }

                String lieferant = fields[0];
                String nummer = fields[1];
                String name = fields[2];
                String vkp = fields[3];
                String vpe = fields[4];
                String stueck = fields[5];
                Boolean sortimentBool = fields[6] == "true" ? true : false;
                Integer artikelID = Integer.parseInt(fields[7]);

                hinzufuegen(artikelID, lieferant, nummer, name,
                        vkp, vpe, stueck, sortimentBool);
            }
            updateAll();
        } catch (FileNotFoundException ex) {
            System.out.println("No backup file found. No backed up order loaded.");
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
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
        } else {
            hinzufuegenButton.setEnabled(false);
        }
        if (artikelIDs.size() > 0) {
            abschliessenButton.setEnabled(true);
            verwerfenButton.setEnabled(true);
        } else {
            abschliessenButton.setEnabled(false);
            verwerfenButton.setEnabled(false);
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

    private void updateAnzahlSpinner(Integer vpe) {
        System.out.println("updateAnzahlSpinner: "+vpe);
        if (vpe > 0){
            Integer vpes = (Integer)vpeSpinner.getValue();
            Integer stueck = new Integer(vpes*vpe);
            this.vpeOrAnzahlIsChanged = true;
            anzahlSpinner.setValue(stueck);
            this.vpeOrAnzahlIsChanged = false;
        }
    }

    private void updateVPESpinner(Integer vpe) {
        if (vpe > 0){
            Integer stueck = (Integer)anzahlSpinner.getValue();
            Integer vpes = new Integer(stueck/vpe);
            this.vpeOrAnzahlIsChanged = true;
            vpeSpinner.setValue(vpes);
            this.vpeOrAnzahlIsChanged = false;
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

    protected void hinzufuegen(Integer artikelID,
            String lieferant, String artikelNummer, String artikelName,
            String vkp, String vpe, String stueck, Boolean sortiment) {
        artikelIDs.add(0, artikelID);
        sortimentBools.add(0, sortiment);
        Integer lastPos = 0;
        try {
            lastPos = positions.firstElement();
        } catch (NoSuchElementException ex) { }
        positions.add(0, lastPos+1);
        stueckzahlen.add(0, Integer.parseInt(stueck));
        removeButtons.add(0, new JButton("-"));
        removeButtons.firstElement().addActionListener(this);

        Vector<Object> row = new Vector<Object>();
            row.add(positions.firstElement());
            row.add(lieferant); row.add(artikelNummer); row.add(artikelName);
            row.add(vkp); row.add(vpe); row.add(stueck);
            row.add(removeButtons.firstElement());
        data.add(0, row);

        displayData = new Vector< Vector<Object> >(data);
        initiateDisplayIndices();

        // scroll the table to the bottom
        //articleScrollPane.setScrollPosition(0, orderTable.getHeight());
    }

    private void fuegeArtikelHinzu(Integer stueck) {
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
        artikelPreis = artikelPreis.replace('.',',')+' '+currencySymbol;
        String artikelMwSt = getVAT(selectedArtikelID);
        artikelMwSt = vatFormatter(artikelMwSt);
        Boolean sortimentBool = getSortimentBool(selectedArtikelID);

        hinzufuegen(selectedArtikelID, lieferant, artikelNummer, artikelName,
                artikelPreis, vpe, stueck.toString(), sortimentBool);
        updateAll();
    }

    private Vector<Object> abschliessen() {
        int bestellNr = -1;
        String typ = "";
        try {
            PreparedStatement pstmt;
            if (selBestellNr > 0){
                pstmt = this.conn.prepareStatement("INSERT INTO bestellung "+
                        "SET bestell_nr = ?, typ = ?, bestell_datum = NOW(), jahr = ?, kw = ?");
            } else {
                pstmt = this.conn.prepareStatement("INSERT INTO bestellung "+
                        "SET typ = ?, bestell_datum = NOW(), jahr = ?, kw = ?");
            }
            int fieldCounter = 1;
            if (selBestellNr > 0){
                pstmtSetInteger(pstmt, fieldCounter, selBestellNr); fieldCounter++;
            }
            typ = typField.getText();
            pstmt.setString(fieldCounter, typ); fieldCounter++;
            pstmtSetInteger(pstmt, fieldCounter, Integer.parseInt(jahrField.getText())); fieldCounter++;
            pstmtSetInteger(pstmt, fieldCounter, Integer.parseInt(kwField.getText())); fieldCounter++;
            int result = pstmt.executeUpdate();
            pstmt.close();
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Bestellung konnte nicht abgespeichert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
            if (selBestellNr > 0){
                bestellNr = selBestellNr;
            } else {
                Statement stmt = this.conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT MAX(bestell_nr) FROM bestellung"
                        );
                rs.next(); bestellNr = rs.getInt(1); rs.close();
                stmt.close();
            }
            for (int i=0; i<artikelIDs.size(); i++){
                pstmt = this.conn.prepareStatement(
                        "INSERT INTO bestellung_details SET bestell_nr = ?, "+
                        "typ = ?, position = ?, artikel_id = ?, stueckzahl = ?"
                        );
                pstmtSetInteger(pstmt, 1, bestellNr);
                pstmt.setString(2, typ);
                pstmtSetInteger(pstmt, 3, positions.get(i));
                pstmtSetInteger(pstmt, 4, artikelIDs.get(i));
                pstmtSetInteger(pstmt, 5, stueckzahlen.get(i));
                result = pstmt.executeUpdate();
                pstmt.close();
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel mit ID "+artikelIDs.get(i)+" konnte nicht abgespeichert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            bestellNr = -1;
        }
        Vector<Object> bestNrUndTyp = new Vector<Object>();
            bestNrUndTyp.add(bestellNr); bestNrUndTyp.add(typ);
        return bestNrUndTyp;
    }

    private void verwerfen() {
        clearAll();
        updateAll();
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
        if (e.getDocument() == anzahlField.getDocument()){
            if (this.vpeOrAnzahlIsChanged) return;
            String vpe = getVPE(selectedArtikelID);
            Integer vpeInt = vpe.length() > 0 ? Integer.parseInt(vpe) : 0;
            updateAnzahlColor(vpeInt);
            updateVPESpinner(vpeInt);
            return;
        }
        if (e.getDocument() == vpeSpinnerField.getDocument()){
            if (this.vpeOrAnzahlIsChanged) return;
            String vpe = getVPE(selectedArtikelID);
            Integer vpeInt = vpe.length() > 0 ? Integer.parseInt(vpe) : 0;
            updateAnzahlSpinner(vpeInt);
            return;
        }
        if (e.getDocument() == filterField.getDocument()){
            filterStr = filterField.getText();
            applyFilter();
            updateTable();
            return;
        }
    }
    public void removeUpdate(DocumentEvent e) {
        insertUpdate(e);
    }
    public void changedUpdate(DocumentEvent e) {
	// Plain text components do not fire these events
    }

    private void initiateDisplayIndices() {
        displayIndices = new Vector<Integer>();
        for (int i=0; i<data.size(); i++){
            displayIndices.add(i);
        }
    }

    private void applyFilter() {
        displayData = new Vector< Vector<Object> >(data);
        initiateDisplayIndices();
        if (filterStr.length() == 0){
            return;
        }
        for (int i=0; i<data.size(); i++){
            boolean contains = false;
            for (Object obj : data.get(i)){
                String str;
                try {
                    str = (String) obj;
                    str = str.toLowerCase();
                } catch (ClassCastException ex) {
                    str = "";
                }
                if (str.contains(filterStr.toLowerCase())){
                    contains = true;
                    break;
                }
            }
            if (!contains){
                int display_index = displayIndices.indexOf(i);
                displayData.remove(display_index);
                displayIndices.remove(display_index);
            }
        }
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

    void refreshPositionsInData() {
        for (int i=0; i<positions.size(); i++){
            data.get(i).set(0, positions.get(i));
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
        if (e.getSource() == hinzufuegenButton){
            Integer stueck = (Integer)anzahlSpinner.getValue();
            fuegeArtikelHinzu(stueck);
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
	if (e.getSource() == abschliessenButton){
            Vector<Object> bestellNrUndTyp = abschliessen();
            if ( (Integer)bestellNrUndTyp.get(0) > 0 ){ // if abschliessen was successful
                verwerfen();
                // update the BestellAnzeige tab
                tabbedPane.recreateTabbedPane();
                // switch to BestellAnzeige tab
                tabbedPane.switchToBestellAnzeige(bestellNrUndTyp);
            }
	    return;
	}
	if (e.getSource() == verwerfenButton){
            verwerfen();
	    return;
	}
	int removeIndex = -1;
	for (int i=0; i<removeButtons.size(); i++){
	    if ( e.getSource() == removeButtons.get(i) ){
		removeIndex = i;
		break;
	    }
	}
        if (removeIndex > -1){
            data.remove(removeIndex);
            artikelIDs.remove(removeIndex);
            sortimentBools.remove(removeIndex);
            stueckzahlen.remove(removeIndex);
            removeButtons.remove(removeIndex);

            positions.remove(removeIndex);
            for (int i=removeIndex-1; i>=0; i--){
                positions.set(i, positions.get(i)-1);
            }
            refreshPositionsInData();

            int removeRow = displayIndices.indexOf(removeIndex);
            displayData.remove(removeRow);
            displayIndices.remove(removeRow);

            updateAll();
            return;
        }
    }
}
