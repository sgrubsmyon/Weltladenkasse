package Weltladenbesteller;

// Basic Java stuff:
import java.util.*; // for Vector
import java.io.*; // for File
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.*;

// GUI stuff:
import java.awt.*; // BorderLayout, FlowLayout, Dimension
import java.awt.event.*; // ActionEvent, ActionListener

import javax.swing.*; // JFrame, JPanel, JTable, JButton, ...
import javax.swing.table.*;
import javax.swing.text.*; // for DocumentFilter
import javax.swing.event.*;

import WeltladenDB.*;

public class Bestellen extends BestellungsGrundlage implements
    DocumentListener, TableModelListener {
    // Attribute:
    private final BigDecimal minusOne = new BigDecimal(-1);
    private final BigDecimal percent = new BigDecimal("0.01");

    protected int selBestellNr = -1;
    protected String selTyp = "LM";
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
    private int selectedNumberOfVPEs = 1;

    private JSpinner anzahlSpinner;
    private JSpinner vpeSpinner;
    private JFormattedTextField anzahlField;
    private JFormattedTextField vpeSpinnerField;
    private boolean vpeOrAnzahlIsChanged = false;
    private JTextField vpeField;
    private JTextField preisField;
    private JLabel setLabel;
    private JSpinner jahrSpinner;
    private JFormattedTextField jahrField;
    private JSpinner kwSpinner;
    private JFormattedTextField kwField;
    private JTextField typField;
    private JTextField filterField;
    private JButton emptyFilterButton;
    // Buttons
    private JButton changeTypButton;
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

        ShortcutListener shortcutListener = new ShortcutListener();

        this.registerKeyboardAction(shortcutListener, "barcode", barcodeShortcut,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.registerKeyboardAction(shortcutListener, "name", artikelNameShortcut,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.registerKeyboardAction(shortcutListener, "nummer", artikelNummerShortcut,
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        emptyTable();
	showAll();
        doCSVBackupReadin();
        barcodeBox.requestFocus();
    }

    // listener for keyboard shortcuts
    private class ShortcutListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("barcode")){
                barcodeBox.requestFocus();
                return;
            }
            if (e.getActionCommand().equals("name")){
                artikelBox.requestFocus();
                return;
            }
            if (e.getActionCommand().equals("nummer")){
                nummerBox.requestFocus();
                return;
            }
        }
    }

    void preventSpinnerOverflow(JSpinner spinner) {
        AbstractDocument doc = new PlainDocument() {
            @Override
            public void setDocumentFilter(DocumentFilter filter) {
                if (filter instanceof IntegerDocumentFilter) { // w/o this if, it's not working
                                // maybe the DocumentFilter is reset to a default filter for Spinners
                    super.setDocumentFilter(filter);
                }
            }
        };
        doc.setDocumentFilter(
                new IntegerDocumentFilter(
                    (Integer)((SpinnerNumberModel)spinner.getModel()).getMinimum(),
                    (Integer)((SpinnerNumberModel)spinner.getModel()).getMaximum(), "Anzahl", this
                    )
                );
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor)spinner.getEditor();
        JFormattedTextField field = editor.getTextField();
        field.setDocument(doc);
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
            jahrSpinner.setEditor(jahrEditor);
            jahrField = jahrEditor.getTextField();
            ( (NumberFormatter) jahrField.getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
            jahrField.getDocument().addDocumentListener(this);
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
            kwSpinner.setEditor(kwEditor);
            kwField = kwEditor.getTextField();
            ( (NumberFormatter) kwField.getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
            kwField.getDocument().addDocumentListener(this);
            kwField.setColumns(2);
	    kwLabel.setLabelFor(kwSpinner);
            datePanel.add(kwSpinner);
            ///////
	    JLabel typLabel = new JLabel("Typ:");
            datePanel.add(typLabel);
            typField = new JTextField(selTyp);
            StringDocumentFilter sdf = new StringDocumentFilter(12);
	    ((AbstractDocument)typField.getDocument()).setDocumentFilter(sdf);
            typField.getDocument().addDocumentListener(this);
            typField.setColumns(6);
	    typLabel.setLabelFor(typField);
            datePanel.add(typField);
            changeTypButton = new JButton("Typ ändern");
            changeTypButton.addActionListener(this);
            datePanel.add(changeTypButton);
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
                                                                    bc.smallintMax, // max (null == no max)
                                                                    1); // step
	    anzahlSpinner = new JSpinner(anzahlModel);
            JSpinner.NumberEditor anzahlEditor = new JSpinner.NumberEditor(anzahlSpinner, "###");
            anzahlSpinner.setEditor(anzahlEditor);
            anzahlField = anzahlEditor.getTextField();
            ( (NumberFormatter) anzahlField.getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
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
            anzahlField.setColumns(4);
            preventSpinnerOverflow(anzahlSpinner);
	    anzahlLabel.setLabelFor(anzahlSpinner);
            chooseArticlePanel2.add(anzahlSpinner);

            SpinnerNumberModel vpeModel = new SpinnerNumberModel(1, // initial value
                                                                 0, // min
                                                                 null, // max (null == no max)
                                                                 1); // step
	    vpeSpinner = new JSpinner(vpeModel);
            JSpinner.NumberEditor vpeEditor = new JSpinner.NumberEditor(vpeSpinner, "###");
            vpeSpinner.setEditor(vpeEditor);
            vpeSpinnerField = vpeEditor.getTextField();
            ( (NumberFormatter) vpeSpinnerField.getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
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

	    JLabel preisLabel = new JLabel("VK-Preis: ");
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
            chooseArticlePanel2.add(new JLabel(bc.currencySymbol));
            setLabel = new JLabel("");
            chooseArticlePanel2.add(setLabel);

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
        orderTable = new BestellungsTable(displayData, columnLabels,
                displayIndices, sortimentBools);
        orderTable.getModel().addTableModelListener(this);
	setTableProperties(orderTable);
	TableColumn entf = orderTable.getColumn("Entfernen");
	entf.setPreferredWidth(200);

        // set up spinner column:
        TableColumn col = orderTable.getColumn("Stückzahl");
        SpinnerNumberModel model = new SpinnerNumberModel(1, 1, bc.smallintMax, 1);
        col.setCellRenderer(new JSpinnerRenderer(model));
        JSpinnerEditor se = new JSpinnerEditor(model);
        preventSpinnerOverflow(se.getSpinner());
        col.setCellEditor(se);
        orderTable.setColEditable(col.getModelIndex(), true);
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
            filterStr = "";
            filterField.setColumns(20);
            filterField.getDocument().addDocumentListener(this);
            filterField.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
            abschliessenPanel.add(filterField);
	    emptyFilterButton = new JButton("x");
	    emptyFilterButton.addActionListener(this);
	    abschliessenPanel.add(emptyFilterButton);
        allPanel.add(abschliessenPanel);
    }

    void emptyTable(){
	data = new Vector< Vector<Object> >();
        displayData = new Vector< Vector<Object> >();
        displayIndices = new Vector<Integer>();
        sortimentBools = new Vector<Boolean>();
        initiateTable();
        artikelIDs = new Vector<Integer>();
        positions = new Vector<Integer>();
        removeButtons = new Vector<JButton>();
    }

    private void clearAll(){
        data.clear();
        displayData.clear();
        displayIndices.clear();
        artikelIDs.clear();
        sortimentBools.clear();
        positions.clear();
        removeButtons.clear();
        selBestellNr = -1;
        selJahr = -1;
        selKW = -1;

        setButtonsEnabled();
    }

    protected void updateAll(){
	this.remove(allPanel);
	this.revalidate();
            // create table anew
            showAll();
            //updateTable();
        setButtonsEnabled(); // for abschliessenButton
    }

    private void updateTable(){
        applyFilter(filterStr, displayData, displayIndices);
        articleListPanel.remove(articleScrollPane);
	articleListPanel.revalidate();

        initiateTable();

        articleScrollPane = new JScrollPane(orderTable);
        articleListPanel.add(articleScrollPane);
        setButtonsEnabled();
    }

    protected int numberOfRows() {
        return data.size();
    }





    // CSV export:
    public void doCSVBackup() {
        String backupFilename =
            System.getProperty("user.home")+bc.fileSep+".Weltladenkasse_Bestellung_"+selTyp+".backup";
        File file = new File(backupFilename);

        String fileStr = "";
        // general infos:
        fileStr += "#BestellNr;Jahr;KW"+bc.lineSep;
        fileStr += selBestellNr + bc.delimiter;
        fileStr += selJahr + bc.delimiter;
        fileStr += selKW + bc.lineSep;
        // format of csv file:
        fileStr += "#Lieferant;Art.-Nr.;Artikelname;VK-Preis;VPE;Stueck;sortiment;artikelID"+bc.lineSep;
        for (int i=data.size()-1; i>=0; i--){
            String lieferant = data.get(i).get(1).toString();
            String nummer = data.get(i).get(2).toString();
            String name = data.get(i).get(3).toString();
            String vkp = data.get(i).get(4).toString(); vkp = vkp == null ? "" : vkp;
            String vpe = data.get(i).get(5).toString(); vpe = vpe == null ? "" : vpe;
            String stueck = data.get(i).get(6).toString();
            String sortiment = sortimentBools.get(i).toString();
            String artikelID = artikelIDs.get(i).toString();

            fileStr += lieferant + bc.delimiter;
            fileStr += nummer + bc.delimiter;
            fileStr += name + bc.delimiter;
            fileStr += vkp + bc.delimiter;
            fileStr += vpe + bc.delimiter;
            fileStr += stueck + bc.delimiter;
            fileStr += sortiment + bc.delimiter;
            fileStr += artikelID + bc.lineSep;
        }

        //System.out.println(fileStr);

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
        // clear possible previous order
        clearAll();
        updateAll();

        String backupFilename =
            System.getProperty("user.home")+bc.fileSep+".Weltladenkasse_Bestellung_"+selTyp+".backup";
        File file = new File(backupFilename);

        try {
            // use Reader classes for text files:
            BufferedReader in = new BufferedReader(new FileReader(file)); // Lesen einer Textdatei mit Default Zeichensatz-Codierung, see http://www.wsoftware.de/practices/charsets.html
            String line;
            // parse general info at top:
            while ( (line = in.readLine()) != null) {
                line = line.replaceAll("#.*",""); // remove commented lines
                // get the fields
                String[] fields = line.split(bc.delimiter);
                if (fields.length < 3 ){
                    continue;
                }
                selBestellNr = Integer.parseInt(fields[0]);
                selJahr = Integer.parseInt(fields[1]);
                selKW = Integer.parseInt(fields[2]);
                break;
            }
            // parse articles:
            while ( (line = in.readLine()) != null) {
                line = line.replaceAll("#.*",""); // remove commented lines
                // get the fields
                String[] fields = line.split(bc.delimiter);
                if (fields.length < 7 ){
                    continue;
                }

                String lieferant = fields[0];
                String nummer = fields[1];
                String name = fields[2];
                String vkp = fields[3];
                String vpe = fields[4];
                Integer stueck = Integer.parseInt(fields[5]);
                Boolean sortimentBool = fields[6].equals("true") ? true : false;
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



    private void renameCSVBackupFile(String newTyp) {
        String oldBackupFilename =
            System.getProperty("user.home")+bc.fileSep+".Weltladenkasse_Bestellung_"+selTyp+".backup";
        File oldFile = new File(oldBackupFilename);
        String newBackupFilename =
            System.getProperty("user.home")+bc.fileSep+".Weltladenkasse_Bestellung_"+newTyp+".backup";
        File newFile = new File(newBackupFilename);
        if ( newFile.exists() ){
            int answer = JOptionPane.showConfirmDialog(this,
                    "Es gibt schon eine Bestellung vom Typ '"+newTyp+"'.\n"+
                    "Überschreiben?", "Warnung",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.NO_OPTION){
                return;
            } else {
                // try to delete the existing file so that rename might work
                if ( !newFile.delete() ){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Andere Bestellung konnte nicht überschrieben werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }
        // Rename backup file
        if ( !oldFile.renameTo(newFile) ) {
            JOptionPane.showMessageDialog(this,
                    "Fehler: Bestellungs-Typ konnte nicht geändert werden.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }
        selTyp = newTyp;
        doCSVBackupReadin();
        barcodeBox.requestFocus();
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


    private void showEditDialog(Vector<Artikel> selectedArticles) {
        JDialog editDialog = new JDialog(this.mainWindow, "Artikel bearbeiten", true);
        ArtikelBearbeiten bearb = new ArtikelBearbeiten(this.conn, this.mainWindow, null, editDialog,
                selectedArticles);
        editDialog.getContentPane().add(bearb, BorderLayout.CENTER);
        editDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        WindowAdapterDialog wad = new WindowAdapterDialog(bearb, editDialog,
                "Achtung: Änderungen gehen verloren (noch nicht abgeschickt).\nWirklich schließen?");
        editDialog.addWindowListener(wad);
        editDialog.pack();
        editDialog.setVisible(true);

        // update selected Artikel
        String[] an = artikelBox.parseArtikelName();
        String artikelName = an[0];
        String lieferant = an[1];
        String artikelNummer = (String)nummerBox.getSelectedItem();
        selectedArtikelID = getArticleID(lieferant, artikelNummer); // get the internal artikelID from the DB
    }

    private void setPriceField() {
        boolean variablerPreis = getVariablePriceBool(selectedArtikelID);
        if ( ! variablerPreis ){
            String artikelPreis = getRecSalePrice(selectedArtikelID);
            if (artikelPreis == null || artikelPreis.equals("")){
                artikelPreis = getSalePrice(selectedArtikelID);
            }
            if (artikelPreis == null || artikelPreis.equals("")){
                JOptionPane.showMessageDialog(this,
                        "Für diesen Artikel muss erst der Preis festgelegt werden!",
                        "Info", JOptionPane.INFORMATION_MESSAGE);

                Artikel article = getArticle(selectedArtikelID);
                Vector<Artikel> selectedArticles = new Vector<Artikel>();
                selectedArticles.add(article);

                showEditDialog(selectedArticles);

                artikelPreis = getRecSalePrice(selectedArtikelID);
                if (artikelPreis == null || artikelPreis.equals("")){
                    artikelPreis = getSalePrice(selectedArtikelID);
                }
                if (artikelPreis == null)
                    artikelPreis = "";
                System.out.println("artikelPreis: "+artikelPreis);
            }
            preisField.getDocument().removeDocumentListener(this);
            preisField.setText( bc.decimalMark(artikelPreis) );
            preisField.getDocument().addDocumentListener(this);
        }
        else {
            preisField.setEditable(true);
        }
        int setgroesse = getSetSize(selectedArtikelID);
        if (setgroesse > 1){
            setLabel.setText("pro Set ("+setgroesse+"-er Set)");
        } else {
            setLabel.setText("");
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
        if (anzahlField.getText().length() == 0){
            return;
        }
        if (vpe <= 0){
            anzahlField.setForeground(Color.black);
        } else {
            Integer stueck = Integer.parseInt(anzahlField.getText());
            if (stueck < vpe){
                anzahlField.setForeground(Color.red);
            } else {
                anzahlField.setForeground(Color.green.darker().darker());
            }
        }
    }

    private void updateAnzahlSpinner(Integer vpe) {
        if (vpe > 0){
            System.out.println("updateAnzahlSpinner at work.");
            Integer nvpe = (Integer)vpeSpinner.getValue();
            Integer stueck = new Integer(nvpe*vpe);
            if (
                    stueck >= (Integer)((SpinnerNumberModel)anzahlSpinner.getModel()).getMinimum() &&
                    stueck <= (Integer)((SpinnerNumberModel)anzahlSpinner.getModel()).getMaximum()
               ){
                this.vpeOrAnzahlIsChanged = true;
                    anzahlSpinner.setValue(stueck);
                this.vpeOrAnzahlIsChanged = false;
            }
        }
    }

    private void updateVPESpinner(Integer vpe) {
        if (vpe > 0){
            System.out.println("updateVPESpinner at work.");
            Integer stueck = (Integer)anzahlSpinner.getValue();
            Integer nvpe = new Integer(stueck/vpe);
            this.vpeOrAnzahlIsChanged = true;
                vpeSpinner.setValue(nvpe);
                selectedNumberOfVPEs = (Integer)vpeSpinner.getValue();
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
            String vkp, String vpe, Integer stueck, Boolean sortiment) {
        artikelIDs.add(0, artikelID);
        sortimentBools.add(0, sortiment);
        Integer lastPos = 0;
        try {
            lastPos = positions.firstElement();
        } catch (NoSuchElementException ex) { }
        positions.add(0, lastPos+1);
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
        String vpe = vpeField.getText();
        Integer vpeInt = vpe.length() > 0 ? Integer.parseInt(vpe) : 0;
        String artikelPreis = bc.priceFormatterIntern( preisField.getText() );
        artikelPreis = bc.decimalMark(artikelPreis)+' '+bc.currencySymbol;
        String artikelMwSt = getVAT(selectedArtikelID);
        artikelMwSt = bc.vatFormatter(artikelMwSt);
        Boolean sortimentBool = getSortimentBool(selectedArtikelID);

        hinzufuegen(selectedArtikelID, lieferant, artikelNummer, artikelName,
                artikelPreis, vpe, stueck, sortimentBool);
        updateAll();
        barcodeBox.requestFocus();

        // save a CSV backup to hard disk
        doCSVBackup();
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
                pstmtSetInteger(pstmt, 5, (Integer)data.get(i).get( columnLabels.indexOf("Stückzahl") ));
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
            JOptionPane.showMessageDialog(this,
                    "Fehler: Bestellung konnte nicht vollständig abgespeichert werden.\n"+
                    "Fehlermeldung: "+ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            bestellNr = -1;
        }
        Vector<Object> bestNrUndTyp = new Vector<Object>();
            bestNrUndTyp.add(bestellNr); bestNrUndTyp.add(typ);
        return bestNrUndTyp;
    }

    private void verwerfen() {
        clearAll();
        updateAll();
        barcodeBox.requestFocus();
        // save a CSV backup to hard disk
        doCSVBackup();
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
        //System.out.println("resetting form from artikel box.");
        barcodeText = "";
        artikelNummerText = "";
        barcodeBox.emptyBox();
        nummerBox.emptyBox();
        vpeField.setText("");
        preisField.setText("");
        preisField.setEditable(false);
    }
    private void resetFormFromNummerBox() {
        //System.out.println("resetting form from nummer box.");
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
     *    * Each non abstract class that implements the TableModelListener
     *      must have these methods.
     *
     *    @param e the table model event.
     **/
    public void tableChanged(TableModelEvent e) {
        // save a CSV backup to hard disk
        doCSVBackup();
    }

    /**
     *    * Each non abstract class that implements the DocumentListener
     *      must have these methods.
     *
     *    @param e the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
        if (e.getDocument() == typField.getDocument()){
            selTyp = typField.getText();
            doCSVBackupReadin();
            typField.requestFocus();
        }
        if (e.getDocument() == preisField.getDocument()){
            setButtonsEnabled();
            return;
        }
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
        if (e.getDocument() == anzahlField.getDocument()){
            if (this.vpeOrAnzahlIsChanged) return;
            //System.out.println("anzahlField DocumentListener fired.");
            //System.out.println("anzahlField.getText(): "+anzahlField.getText());
            //System.out.println("anzahlSpinner.getValue(): "+anzahlSpinner.getValue());
            String vpe = getVPE(selectedArtikelID);
            Integer vpeInt = vpe.length() > 0 ? Integer.parseInt(vpe) : 0;
            updateAnzahlColor(vpeInt);
            updateVPESpinner(vpeInt);
            return;
        }
        if (e.getDocument() == vpeSpinnerField.getDocument()){
            if (this.vpeOrAnzahlIsChanged) return;
            if ( (Integer)vpeSpinner.getValue() == selectedNumberOfVPEs ){
                return; // return if there was no change (e.g. only focus on spinner)
            } else {
                selectedNumberOfVPEs = (Integer)vpeSpinner.getValue();
            }
            //System.out.println("vpeSpinnerField DocumentListener fired.");
            //System.out.println("vpeSpinnerField DocumentListener: vpeOrAnzahlIsChanged = "+vpeOrAnzahlIsChanged);
            //System.out.println("anzahlField.getText(): "+anzahlField.getText());
            //System.out.println("anzahlSpinner.getValue(): "+anzahlSpinner.getValue());
            String vpe = getVPE(selectedArtikelID);
            Integer vpeInt = vpe.length() > 0 ? Integer.parseInt(vpe) : 0;
            updateAnzahlSpinner(vpeInt);
            return;
        }
        if (e.getDocument() == filterField.getDocument()){
            String oldFilterStr = new String(filterStr);
            filterStr = filterField.getText();
            if ( !filterStr.contains(oldFilterStr) ){
                // user has deleted from, not added to the filter string, reset the displayData
                displayData = new Vector< Vector<Object> >(data);
                initiateDisplayIndices();
            }
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

    void refreshPositionsInData() {
        for (int i=0; i<positions.size(); i++){
            data.get(i).set(0, positions.get(i));
        }
    }

    private static Vector<String> showChangeTypDialog(String oldTyp) {
        final JTextField newTypField = new JTextField(oldTyp);
        JOptionPane jop = new JOptionPane(new Object[]{"Neuer Bestellungs-Typ:", newTypField},
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = jop.createDialog("Bitte neuen Bestellungs-Typ eingeben");
        dialog.addWindowFocusListener(new WindowAdapter(){
            @Override
            public void windowGainedFocus(WindowEvent e){
                newTypField.requestFocusInWindow();
            }
        });
        newTypField.addFocusListener(new FocusListener() {
            public void focusGained( FocusEvent e ) {
                newTypField.selectAll();
            }
            public void focusLost( FocusEvent e ) {
                if ( newTypField.getText().length() == 0 ) {
                    newTypField.requestFocusInWindow();
                }
            }
        });
        dialog.setVisible(true);
        int result = (Integer)jop.getValue();
        dialog.dispose();

        Vector<String> okTyp = new Vector<String>(2);
        String ok;
        if (result == JOptionPane.OK_OPTION){ ok = "OK"; }
        else { ok = "CANCEL"; }
        String typ = new String(newTypField.getText());
        okTyp.add(ok);
        okTyp.add(typ);
        return okTyp;
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
        if (e.getSource() == changeTypButton){
            Vector<String> okTyp = showChangeTypDialog(typField.getText());
            if (okTyp.get(0) == "OK"){
                renameCSVBackupFile(okTyp.get(1));
            }
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
            removeButtons.remove(removeIndex);

            positions.remove(removeIndex);
            for (int i=removeIndex-1; i>=0; i--){
                positions.set(i, positions.get(i)-1);
            }
            refreshPositionsInData();

            int removeRow = displayIndices.indexOf(removeIndex);
            displayData.remove(removeRow);
            displayIndices.remove(removeRow);
            // propagate change in displayIndices:
            for (int i=removeRow; i<displayIndices.size(); i++){
                displayIndices.set(i, displayIndices.get(i)-1);
            }

            //updateAll();
            //barcodeBox.requestFocus();
            updateTable();

            // save a CSV backup to hard disk
            doCSVBackup();
            return;
        }
        if (e.getSource() == emptyFilterButton){
            filterField.setText("");
            filterField.requestFocus();
	    return;
	}
    }
}
