package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector
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

// DateTime from date4j (http://www.date4j.net/javadoc/index.html)
import hirondelle.date4j.DateTime;

import WeltladenDB.*;

public class Kassieren extends RechnungsGrundlage implements ItemListener, DocumentListener {
    // Attribute:
    private final BigDecimal mitarbeiterRabatt = new BigDecimal("0.1");
    private final boolean allowMitarbeiterRabatt = true;
    private final BigDecimal ecSchwelle = new BigDecimal("20.00");
    private int artikelRabattArtikelID = 1;
    private int rechnungRabattArtikelID = 2;
    private String zahlungsModus = "unbekannt";

    private Kundendisplay display;
    private TabbedPane tabbedPane;
    private Kassieren myKassieren;

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
    private JButton sonstigesButton;
    private JButton sevenPercentButton;
    private JButton nineteenPercentButton;
    private JSpinner anzahlSpinner;
    private JFormattedTextField anzahlField;
    private JTextField preisField;
    private JTextField kundeGibtField;
    private JTextField rueckgeldField;
    private JTextField gutscheinField;
    private JTextField bigPriceField;
    private JTextField individuellRabattRelativField;
    private JTextField individuellRabattAbsolutField;
    // Buttons
    private JButton emptyBarcodeButton;
    private JButton emptyArtikelButton;
    private JButton emptyNummerButton;
    private JButton hinzufuegenButton;
    private JButton leergutButton;
    private JButton ruecknahmeButton;
    private JButton zwischensummeButton;
    private JButton barButton;
    private JButton ecButton;
    private JButton stornoButton;
    private JButton gutscheinButton;
    private JLabel zahlungsLabel;
    private JButton neuerKundeButton;
    private JButton quittungsButton;
    private JButton individuellRabattRelativButton;
    private JButton individuellRabattAbsolutButton;
    private JButton mitarbeiterRabattButton;

    private Vector<JButton> rabattButtons;

    // The panels
    private JPanel allPanel;
    private JPanel rabattPanel;
    private JPanel articleListPanel;

    // The table holding the purchase articles.
    private RechnungsTable myTable;
    private JScrollPane scrollPane;
    private Vector< Vector<Object> > data;
    private Vector<JButton> removeButtons;
    private Vector<BigDecimal> mwsts;

    // Methoden:

    /**
     *    The constructor.
     *       */
    public Kassieren(Connection conn, MainWindowGrundlage mw, TabbedPane tp) {
	super(conn, mw);
        if (mw instanceof MainWindow){
            display = ( (MainWindow)mw ).getDisplay();
        } else {
            display = null;
        }
        myKassieren = this;
        tabbedPane = tp;

        columnLabels.add("Entfernen");

        // keyboard shortcuts:
        KeyStroke barcodeShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK); // Ctrl-C
        KeyStroke artikelNameShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK); // Ctrl-A
        KeyStroke artikelNummerShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK); // Ctrl-N
        KeyStroke zwischensummeShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK); // Ctrl-Z
        KeyStroke barShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_B, Event.CTRL_MASK); // Ctrl-B
        KeyStroke ecShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK); // Ctrl-E
        KeyStroke stornierenShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK); // Ctrl-S

        ShortcutListener shortcutListener = new ShortcutListener();

        this.registerKeyboardAction(shortcutListener, "barcode", barcodeShortcut,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.registerKeyboardAction(shortcutListener, "name", artikelNameShortcut,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.registerKeyboardAction(shortcutListener, "nummer", artikelNummerShortcut,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.registerKeyboardAction(shortcutListener, "zws", zwischensummeShortcut,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.registerKeyboardAction(shortcutListener, "bar", barShortcut,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.registerKeyboardAction(shortcutListener, "ec", ecShortcut,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.registerKeyboardAction(shortcutListener, "stornieren", stornierenShortcut,
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        rabattButtons = new Vector<JButton>();
        rabattButtons.add(new JButton("  5%"));
        rabattButtons.add(new JButton("10%"));
        rabattButtons.add(new JButton("15%"));
        rabattButtons.add(new JButton("20%"));
        rabattButtons.add(new JButton("25%"));

        showButtons();
        emptyTable();
	showAll();
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
            if (e.getActionCommand() == "zws"){
                if (zwischensummeButton.isEnabled())
                    zwischensumme();
                return;
            }
            if (e.getActionCommand() == "bar"){
                if (barButton.isEnabled())
                    bar();
                return;
            }
            if (e.getActionCommand() == "ec"){
                if (ecButton.isEnabled())
                    ec();
                return;
            }
            if (e.getActionCommand() == "stornieren"){
                if (stornoButton.isEnabled())
                    stornieren();
                return;
            }
        }
    }

    void preventSpinnerOverflow(JFormattedTextField spinnerField) {
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
                    (Integer)((SpinnerNumberModel)anzahlSpinner.getModel()).getMinimum(),
                    (Integer)((SpinnerNumberModel)anzahlSpinner.getModel()).getMaximum(), "Anzahl", this
                    )
                );
        spinnerField.setDocument(doc);
    }

    void showAll(){
        updateRabattButtons();

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

            JPanel sonstigesPanel = new JPanel();
            sonstigesPanel.setLayout(new BoxLayout(sonstigesPanel, BoxLayout.Y_AXIS));
            sonstigesPanel.setBorder(BorderFactory.createTitledBorder("Variabler Preis"));
                sonstigesButton = new JButton("Sonstiges...");
                sevenPercentButton = new JButton("7% MwSt.");
                nineteenPercentButton = new JButton("19% MwSt.");
                sonstigesButton.setAlignmentX(JComponent.CENTER_ALIGNMENT);
                sevenPercentButton.setAlignmentX(JComponent.CENTER_ALIGNMENT);
                nineteenPercentButton.setAlignmentX(JComponent.CENTER_ALIGNMENT);
                sonstigesButton.addActionListener(this);
                sevenPercentButton.addActionListener(this);
                nineteenPercentButton.addActionListener(this);
                sonstigesPanel.add(sonstigesButton);
                sonstigesPanel.add(Box.createRigidArea(new Dimension(0, 5))); // vertical space
                sonstigesPanel.add(sevenPercentButton);
                sonstigesPanel.add(Box.createRigidArea(new Dimension(0, 5))); // vertical space
                sonstigesPanel.add(nineteenPercentButton);
                sonstigesPanel.add(Box.createRigidArea(new Dimension(0, 5))); // vertical space
            articleSelectPanel.add(sonstigesPanel);

        allPanel.add(articleSelectPanel);

	JPanel spinnerPanel = new JPanel();
	spinnerPanel.setLayout(new FlowLayout());
	    JLabel anzahlLabel = new JLabel("Anzahl: ");
            spinnerPanel.add(anzahlLabel);
            SpinnerNumberModel anzahlModel = new SpinnerNumberModel(1, // initial value
                                                                    1, // min
                                                                    bc.smallintMax, // max (null == no max)
                                                                    1); // step
	    anzahlSpinner = new JSpinner(anzahlModel);
            JSpinner.NumberEditor anzahlEditor = new JSpinner.NumberEditor(anzahlSpinner, "###");
            anzahlSpinner.setEditor(anzahlEditor);
            anzahlField = anzahlEditor.getTextField();
            preventSpinnerOverflow(anzahlField);
            ( (NumberFormatter) anzahlField.getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
            //anzahlField.getDocument().addDocumentListener(this);
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
	    anzahlLabel.setLabelFor(anzahlSpinner);
            spinnerPanel.add(anzahlSpinner);

	    JLabel preisLabel = new JLabel("Preis: ");
            spinnerPanel.add(preisLabel);
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
            spinnerPanel.add(preisField);
            spinnerPanel.add(new JLabel(bc.currencySymbol));

	    hinzufuegenButton = new JButton("Hinzufügen");
            hinzufuegenButton.setMnemonic(KeyEvent.VK_H);
	    hinzufuegenButton.addActionListener(this);
	    hinzufuegenButton.setEnabled(false);
	    spinnerPanel.add(hinzufuegenButton);

	    leergutButton = new JButton("Als Leergut");
            leergutButton.setMnemonic(KeyEvent.VK_L);
	    leergutButton.addActionListener(this);
	    leergutButton.setEnabled(false);
	    spinnerPanel.add(leergutButton);

	    ruecknahmeButton = new JButton("Rückgabe");
            ruecknahmeButton.setMnemonic(KeyEvent.VK_R);
	    ruecknahmeButton.addActionListener(this);
	    ruecknahmeButton.setEnabled(false);
	    spinnerPanel.add(ruecknahmeButton);
        allPanel.add(spinnerPanel);

	showTable();

        JPanel zwischensummePanel = new JPanel();
        zwischensummePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
            barButton = new JButton("Bar");
            barButton.setMnemonic(KeyEvent.VK_B);
            barButton.setEnabled(false);
            barButton.addActionListener(this);
            zwischensummePanel.add(barButton);
            ecButton = new JButton("EC");
            ecButton.setMnemonic(KeyEvent.VK_E);
            ecButton.setEnabled(false);
            ecButton.addActionListener(this);
            zwischensummePanel.add(ecButton);
            stornoButton = new JButton("Storno");
            stornoButton.setMnemonic(KeyEvent.VK_S);
            if (data.size() == 0) stornoButton.setEnabled(false);
            stornoButton.addActionListener(this);
            zwischensummePanel.add(stornoButton);

            JPanel gridPanel = new JPanel();
            gridPanel.setLayout(new GridLayout(0, 2));
                JLabel kundeGibtLabel = new JLabel("Kunde gibt:");
                kundeGibtField = new JTextField("");
                kundeGibtField.setEditable(false);
                kundeGibtField.setColumns(7);
                kundeGibtField.setHorizontalAlignment(JTextField.RIGHT);
                kundeGibtField.getDocument().addDocumentListener(new DocumentListener(){
                    public void insertUpdate(DocumentEvent e) {
                        updateRueckgeld();
                    }
                    public void removeUpdate(DocumentEvent e) {
                        updateRueckgeld();
                    }
                    public void changedUpdate(DocumentEvent e) {
                        // Plain text components do not fire these events
                    }
                });
                kundeGibtField.addKeyListener(new KeyAdapter() {
                    public void keyPressed(KeyEvent e) { if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
                        if (barButton.isEnabled()) bar(); }
                    }
                });
                ((AbstractDocument)kundeGibtField.getDocument()).setDocumentFilter(geldFilter);
                JPanel kundeGibtPanel = new JPanel();
                kundeGibtPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
                kundeGibtPanel.add(kundeGibtField);
                kundeGibtPanel.add(new JLabel(bc.currencySymbol));
                kundeGibtPanel.add(Box.createRigidArea(new Dimension(40, 0)));

                JLabel gutscheinLabel = new JLabel("Gutschein:");
                gutscheinField = new JTextField("");
                gutscheinField.setEditable(false);
                gutscheinField.setColumns(7);
                gutscheinField.setHorizontalAlignment(JTextField.RIGHT);
                gutscheinField.addKeyListener(new KeyAdapter() {
                    public void keyPressed(KeyEvent e) {
                        if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
                            if (gutscheinButton.isEnabled()){ gutscheinButton.doClick(); }
                        }
                    }
                });
                ((AbstractDocument)gutscheinField.getDocument()).setDocumentFilter(geldFilter);
                gutscheinField.getDocument().addDocumentListener(this);
                JPanel gutscheinPanel = new JPanel();
                gutscheinPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
                gutscheinPanel.add(gutscheinField);
                gutscheinPanel.add(new JLabel(bc.currencySymbol));
                gutscheinButton = new JButton("OK");
                gutscheinButton.setEnabled(false);
                gutscheinButton.addActionListener(this);
                gutscheinPanel.add(gutscheinButton);
                gutscheinPanel.add(Box.createHorizontalGlue());

                JLabel rueckgeldLabel = new JLabel("Rückgeld:");
                JPanel rueckgeldPanel = new JPanel();
                rueckgeldPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
                rueckgeldField = new JTextField("");
                rueckgeldField.setEditable(false);
                rueckgeldField.setColumns(7);
                rueckgeldField.setHorizontalAlignment(JTextField.RIGHT);
                rueckgeldField.setFont(new Font("Tahoma", Font.BOLD, 12));
                rueckgeldPanel.add(rueckgeldField);
                rueckgeldPanel.add(new JLabel(bc.currencySymbol));
                rueckgeldPanel.add(Box.createRigidArea(new Dimension(40, 0)));

                gridPanel.add(kundeGibtLabel);
                gridPanel.add(kundeGibtPanel);
                gridPanel.add(gutscheinLabel);
                gridPanel.add(gutscheinPanel);
                gridPanel.add(rueckgeldLabel);
                gridPanel.add(rueckgeldPanel);

            zwischensummePanel.add(gridPanel);

            bigPriceField = new JTextField("");
            bigPriceField.setEditable(false);
            bigPriceField.setColumns(7);
            bigPriceField.setHorizontalAlignment(JTextField.RIGHT);
            bigPriceField.setFont(new Font("Tahoma", Font.BOLD, 32));
            zwischensummePanel.add(bigPriceField);
        allPanel.add(zwischensummePanel);

        JPanel neuerKundePanel = new JPanel();
        neuerKundePanel.setLayout(new BoxLayout(neuerKundePanel, BoxLayout.Y_AXIS));
            zahlungsLabel = new JLabel(" ");
            zahlungsLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
            neuerKundePanel.add(zahlungsLabel);
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BorderLayout());
                // center
                JPanel centerPanel = new JPanel();
                    neuerKundeButton = new JButton("Fertig/Nächster Kunde");
                    neuerKundeButton.setEnabled(false);
                    neuerKundeButton.addActionListener(this);
                    centerPanel.add(neuerKundeButton);
                buttonPanel.add(centerPanel, BorderLayout.CENTER);
                // right
                JPanel rightPanel = new JPanel();
                rightPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
                    quittungsButton = new JButton("Quittung");
                    quittungsButton.setMnemonic(KeyEvent.VK_Q);
                    quittungsButton.setEnabled(false);
                    quittungsButton.addActionListener(this);
                    rightPanel.add(quittungsButton);
                buttonPanel.add(rightPanel, BorderLayout.EAST);
            neuerKundePanel.add(buttonPanel);
        allPanel.add(neuerKundePanel);

	this.add(allPanel, BorderLayout.CENTER);
    }

    void showButtons(){
	rabattPanel = new JPanel();
	rabattPanel.setLayout(new BoxLayout(rabattPanel, BoxLayout.Y_AXIS));
	rabattPanel.setBorder(BorderFactory.createTitledBorder("Rabatt"));
        for (JButton rbutton : rabattButtons){
            rbutton.addActionListener(this);
            rbutton.setAlignmentX(JComponent.CENTER_ALIGNMENT);
            rabattPanel.add(rbutton);
            rabattPanel.add(Box.createRigidArea(new Dimension(0, 5))); // vertical space
        }
        mitarbeiterRabattButton = new JButton("Mitarbeiter");
        mitarbeiterRabattButton.addActionListener(this);
        mitarbeiterRabattButton.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        rabattPanel.add(mitarbeiterRabattButton);
        rabattPanel.add(Box.createRigidArea(new Dimension(0, 5))); // vertical space

        JPanel individuellRabattPanel = new JPanel();
            individuellRabattPanel.setBorder(BorderFactory.createTitledBorder("individuell"));
            individuellRabattPanel.setLayout(new BoxLayout(individuellRabattPanel, BoxLayout.Y_AXIS));

            JPanel relativPanel = new JPanel();
            relativPanel.setLayout(new FlowLayout());
            individuellRabattRelativField = new JTextField("");
            individuellRabattRelativField.setColumns(3);
            individuellRabattRelativField.getDocument().addDocumentListener(new DocumentListener(){
                public void insertUpdate(DocumentEvent e) {
                    if (individuellRabattRelativField.getText().length() > 0){
                        individuellRabattRelativButton.setEnabled(true);
                    } else {
                        individuellRabattRelativButton.setEnabled(false);
                    }
                }
                public void removeUpdate(DocumentEvent e) {
                    this.insertUpdate(e);
                }
                public void changedUpdate(DocumentEvent e) {
                    // Plain text components do not fire these events
                }
            });
            ((AbstractDocument)individuellRabattRelativField.getDocument()).setDocumentFilter(relFilter);
            individuellRabattRelativField.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
                        individuellRabattRelativButton.doClick();
                    }
                }
            });
            relativPanel.add(individuellRabattRelativField);
            relativPanel.add(new JLabel("%"));
            individuellRabattRelativButton = new JButton("OK");
            individuellRabattRelativButton.addActionListener(this);
            relativPanel.add(individuellRabattRelativButton);

            JPanel absolutPanel = new JPanel();
            absolutPanel.setLayout(new FlowLayout());
            individuellRabattAbsolutField = new JTextField("");
            individuellRabattAbsolutField.setColumns(3);
            individuellRabattAbsolutField.getDocument().addDocumentListener(new DocumentListener(){
                public void insertUpdate(DocumentEvent e) {
                    if (individuellRabattAbsolutField.getText().length() > 0){
                        individuellRabattAbsolutButton.setEnabled(true);
                    } else {
                        individuellRabattAbsolutButton.setEnabled(false);
                    }
                }
                public void removeUpdate(DocumentEvent e) {
                    this.insertUpdate(e);
                }
                public void changedUpdate(DocumentEvent e) {
                    // Plain text components do not fire these events
                }
            });
	    ((AbstractDocument)individuellRabattAbsolutField.getDocument()).setDocumentFilter(geldFilter);
            individuellRabattAbsolutField.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
                        individuellRabattAbsolutButton.doClick();
                    }
                }
            });
            absolutPanel.add(individuellRabattAbsolutField);
            absolutPanel.add(new JLabel(bc.currencySymbol));
            individuellRabattAbsolutButton = new JButton("OK");
            individuellRabattAbsolutButton.addActionListener(this);
            absolutPanel.add(individuellRabattAbsolutButton);

            individuellRabattPanel.add(relativPanel);
            individuellRabattPanel.add(absolutPanel);
        rabattPanel.add(individuellRabattPanel);
	this.add(rabattPanel, BorderLayout.WEST);
    }

    void updateRabattButtons(){
        boolean enabled = false;
        if (kassierArtikel.size() > 0){
            enabled = true;
            for (int i=kassierArtikel.size()-1; i>=0; i--){
                if ( kassierArtikel.get(i).getType().equals("rabatt") ){
                    enabled = false; // Artikel hat schon Rabatt, keinen Rabatt mehr erlauben
                    break;
                }
                if ( kassierArtikel.get(i).getType().equals("leergut") ){
                    enabled = false; // Es handelt sich um Leergut, kein Rabatt erlauben
                    break;
                }
                if ( kassierArtikel.get(i).getType().equals("artikel") ){
                    // Artikel hatte wohl noch keinen Rabatt
                    break;
                }
            }
        }
        for (JButton rbutton : rabattButtons){
            rbutton.setEnabled(enabled);
        }
        mitarbeiterRabattButton.setEnabled(false);
        individuellRabattRelativField.setEditable(enabled);
        individuellRabattRelativButton.setEnabled(false);
        individuellRabattAbsolutField.setEditable(enabled);
        individuellRabattAbsolutButton.setEnabled(false);
    }

    void updateRabattButtonsZwischensumme(){
        boolean enabled = false;
        if (!kassierArtikel.lastElement().getType().equals("rabattrechnung")){
            // determine place to start (after last "Rabatt auf Rechnung")
            int startIndex = 0;
            for (int i=kassierArtikel.size()-1; i>=0; i--){
                if (kassierArtikel.get(i).getType().equals("rabattrechnung")){
                    startIndex = i+1;
                    break;
                }
            }
            // scan through artikel list to search for artikels without rabatt
            int artikelCount = 0;
            for (int i=startIndex; i<kassierArtikel.size(); i++){
                if ( kassierArtikel.get(i).getType().equals("artikel") ){
                    artikelCount++;
                }
                else if ( kassierArtikel.get(i).getType().equals("rabatt") ){
                    artikelCount = 0;
                }
                if (artikelCount > 1){ // there was an article without Rabatt
                    enabled = true;
                    break;
                }
            }
            if (artikelCount > 0){ enabled = true; } // (at least) last article had no Rabatt, this is needed in case there is only one article without Rabatt
        }
        for (JButton rbutton : rabattButtons){
            rbutton.setEnabled(enabled);
        }
        if (allowMitarbeiterRabatt){
            mitarbeiterRabattButton.setEnabled(enabled);
        }
        individuellRabattRelativField.setEditable(enabled);
        individuellRabattRelativButton.setEnabled(false);
        individuellRabattAbsolutField.setEditable(false);
        individuellRabattAbsolutButton.setEnabled(false);
    }


    void showTable(){
	myTable = new RechnungsTable(data, columnLabels);
//	myTable.setBounds(71,53,150,100);
//	myTable.setToolTipText("Tabelle kann nur gelesen werden.");
	setTableProperties(myTable);
	TableColumn entf = myTable.getColumn("Entfernen");
	entf.setPreferredWidth(2);
//	myTable.setAutoResizeMode(5);

	articleListPanel = new JPanel();
	articleListPanel.setLayout(new BoxLayout(articleListPanel, BoxLayout.Y_AXIS));
	articleListPanel.setBorder(BorderFactory.createTitledBorder("Gewählte Artikel"));

            scrollPane = new JScrollPane(myTable);
            articleListPanel.add(scrollPane);

            JPanel totalPricePanel = createTotalPricePanel();
            zwischensummeButton = new JButton("Zwischensumme");
            zwischensummeButton.setMnemonic(KeyEvent.VK_Z);
	    zwischensummeButton.addActionListener(this);
	    if (data.size() == 0) zwischensummeButton.setEnabled(false);
	    totalPricePanel.add(zwischensummeButton);
            articleListPanel.add(totalPricePanel);

	allPanel.add(articleListPanel);
    }

    void emptyTable(){
	data = new Vector< Vector<Object> >();
        kassierArtikel = new Vector<KassierArtikel>();
        removeButtons = new Vector<JButton>();
        mwsts = new Vector<BigDecimal>();
    }

    private void clearAll(){
        data.clear();
        kassierArtikel.clear();
        removeButtons.clear();
        mwsts.clear();
        zahlungsModus = "unbekannt";
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

    void updateRueckgeld(){
        if (kundeGibtField.getDocument().getLength() == 0){
            rueckgeldField.setText("");
            //neuerKundeButton.setEnabled(false);
        } else {
            BigDecimal totalPrice = new BigDecimal( getTotalPrice() );
            BigDecimal kundeGibt = new BigDecimal( getKundeGibt() );
            BigDecimal rueckgeld = kundeGibt.subtract(totalPrice);
            if (rueckgeld.signum() < 0){
                rueckgeldField.setForeground(Color.red);
                //neuerKundeButton.setEnabled(false);
            } else {
                rueckgeldField.setForeground(Color.green.darker().darker());
                //neuerKundeButton.setEnabled(true);
            }
            rueckgeldField.setText( bc.priceFormatter(rueckgeld) );
        }
    }

    String getTotalPrice() {
        return bc.priceFormatterIntern( totalPriceField.getText() );
    }

    String getKundeGibt() {
        return bc.priceFormatterIntern( kundeGibtField.getText() );
    }

    //////////////////////////////////
    // DB query functions:
    //////////////////////////////////
    private void checkForRabatt(){
        /*
         * QUERY ALL THE RABATTAKTIONEN, STORE THEM IN VECTORS, ORDERED BY RABATT MODE:
         * ============================================================================
         * 1. rabatt for artikel_id:
         * artikel_id of artikel = x
         * SELECT rabatt_absolut, rabatt_relativ, mengenrabatt_schwelle, mengenrabatt_relativ,
         * mengenrabatt_anzahl_kostenlos FROM rabattaktion WHERE artikel_id = x;
         *
         * 2. rabatt for produktgruppe:
         * artikel_id of artikel = x
         * SELECT toplevel_id AS x, sub_id AS y, subsub_id AS z FROM produktgruppe AS p INNER JOIN
         * artikel AS a USING (produktgruppen_id) WHERE a.artikel_id = x;
         *
         * toplevel_id, sub_id, subsub_id of artikel = x, y, z
         * SELECT rabatt_absolut, rabatt_relativ, mengenrabatt_schwelle, mengenrabatt_relativ,
         * mengenrabatt_anzahl_kostenlos FROM rabattaktion AS r INNER JOIN produktgruppe AS p USING
         * (produktgruppen_id) WHERE (toplevel_id) = x AND (sub_id = y OR sub_id
         * IS NULL) AND (subsub_id = z OR subsub_id IS NULL);
         */
        int artikelID = selectedArtikelID;
        BigDecimal stueck = new BigDecimal(selectedStueck);
        BigDecimal einzelpreis = new BigDecimal(
                bc.priceFormatterIntern(preisField.getText())
                );
        Vector< Vector<Object> > einzelrabattAbsolutVector = new Vector< Vector<Object> >();
        Vector< Vector<Object> > einzelrabattRelativVector = new Vector< Vector<Object> >();
        Vector< Vector<Object> > mengenrabattAnzahlVector = new Vector< Vector<Object> >();
        Vector< Vector<Object> > mengenrabattRelativVector = new Vector< Vector<Object> >();
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    // get alle Rabatte auf artikelID
                    "SELECT rabatt_absolut, rabatt_relativ, mengenrabatt_schwelle, mengenrabatt_anzahl_kostenlos, "+
                    "mengenrabatt_relativ, aktionsname, rabatt_id FROM rabattaktion WHERE artikel_id = ? "+
                    "AND von <= NOW() AND IFNULL(bis >= NOW(), true)"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            while ( rs.next() ){
                BigDecimal einzelAbsolut = rs.getString(1) == null ? null :
                    new BigDecimal(rs.getString(1));
                BigDecimal einzelRelativ = rs.getString(2) == null ? null :
                    new BigDecimal(rs.getString(2));
                BigDecimal mengenSchwelle = rs.getString(3) == null ? null :
                    new BigDecimal(rs.getInt(3));
                BigDecimal mengenAnzahl = rs.getString(4) == null ? null :
                    new BigDecimal(rs.getInt(4));
                BigDecimal mengenRelativ = rs.getString(5) == null ? null :
                    new BigDecimal(rs.getString(5));
                String aktionsname = rs.getString(6);
                int rabattID = rs.getInt(7);
                addRabatteToVectors(einzelAbsolut, einzelRelativ, mengenSchwelle, mengenAnzahl, mengenRelativ, aktionsname, rabattID,
                        einzelrabattAbsolutVector, einzelrabattRelativVector, mengenrabattAnzahlVector, mengenrabattRelativVector);
            }
            rs.close();
            pstmt.close();
            pstmt = this.conn.prepareStatement(
                    // get toplevel_id, sub_id, subsub_id for produktgruppenID of artikelID
                    "SELECT toplevel_id, sub_id, subsub_id FROM produktgruppe AS p INNER JOIN "+
                    "artikel AS a USING (produktgruppen_id) WHERE a.artikel_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            rs = pstmt.executeQuery();
            rs.next();
            int toplevelID = rs.getInt(1);
            int subID = rs.getInt(2);
            int subsubID = rs.getInt(3);
            rs.close();
            pstmt.close();
            pstmt = this.conn.prepareStatement(
                    // get alle Rabatte auf produktgruppe
                    "SELECT rabatt_absolut, rabatt_relativ, mengenrabatt_schwelle, mengenrabatt_anzahl_kostenlos, "+
                    "mengenrabatt_relativ, aktionsname, rabatt_id FROM rabattaktion AS r INNER JOIN produktgruppe AS p "+
                    "USING (produktgruppen_id) WHERE (toplevel_id = ?) AND "+
                    "(sub_id = ? OR sub_id IS NULL) AND (subsub_id = ? OR subsub_id IS NULL) "+
                    "AND von <= NOW() AND IFNULL(bis >= NOW(), true)"
                    );
            pstmtSetInteger(pstmt, 1, toplevelID);
            pstmtSetInteger(pstmt, 2, subID);
            pstmtSetInteger(pstmt, 3, subsubID);
            rs = pstmt.executeQuery();
            while ( rs.next() ){
                BigDecimal einzelAbsolut = rs.getString(1) == null ? null :
                    new BigDecimal(rs.getString(1));
                BigDecimal einzelRelativ = rs.getString(2) == null ? null :
                    new BigDecimal(rs.getString(2));
                BigDecimal mengenSchwelle = rs.getString(3) == null ? null :
                    new BigDecimal(rs.getInt(3));
                BigDecimal mengenAnzahl = rs.getString(4) == null ? null :
                    new BigDecimal(rs.getInt(4));
                BigDecimal mengenRelativ = rs.getString(5) == null ? null :
                    new BigDecimal(rs.getString(5));
                String aktionsname = rs.getString(6);
                int rabattID = rs.getInt(7);
                addRabatteToVectors(einzelAbsolut, einzelRelativ, mengenSchwelle, mengenAnzahl, mengenRelativ, aktionsname, rabattID,
                        einzelrabattAbsolutVector, einzelrabattRelativVector, mengenrabattAnzahlVector, mengenrabattRelativVector);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }

        /*
         * RABATTAKTIONEN IN DIESER REIHENFOLGE ANWENDEN:
         * ==============================================
         * Es gibt 4 Rabatt Modi:
         * 1. Alle Einzelrabatte, absolut
         * 2. Alle Einzelrabatte, relativ
         * 3. Alle Mengenrabatte, Anzahl kostenlos
         * 4. Alle Mengenrabatte, relativ
        */
        for ( Vector<Object> vector : einzelrabattAbsolutVector ){
            int rabattID = (Integer) vector.get(0);
            String aktionsname = (String) vector.get(1);
            BigDecimal einzelAbsRabatt = (BigDecimal) vector.get(2);
            BigDecimal reduktion = stueck.multiply(einzelAbsRabatt).multiply(bc.minusOne);
            einzelpreis = einzelpreis.subtract(einzelAbsRabatt);

            addRabattRow(rabattID, aktionsname, reduktion, stueck);
        }
        for ( Vector<Object> vector : einzelrabattRelativVector ){
            int rabattID = (Integer) vector.get(0);
            String aktionsname = (String) vector.get(1);
            BigDecimal einzelRelRabatt = (BigDecimal) vector.get(2);
            BigDecimal reduktion = new BigDecimal(
                    bc.priceFormatterIntern(
                        stueck.multiply(einzelRelRabatt).multiply(einzelpreis).
                        multiply(bc.minusOne)
                        )
                    );
            einzelpreis = einzelpreis.subtract( einzelRelRabatt.multiply(einzelpreis) );

            addRabattRow(rabattID, aktionsname, reduktion, stueck);
        }
        for ( Vector<Object> vector : mengenrabattAnzahlVector ){
            int rabattID = (Integer) vector.get(0);
            String aktionsname = (String) vector.get(1);
            BigDecimal mengenSchwelle = (BigDecimal) vector.get(2);
            BigDecimal mengenAnzKostenlos = (BigDecimal) vector.get(3);
            if ( stueck.compareTo(mengenSchwelle) >= 0 ){ // if stueck >= mengenSchwelle
                BigDecimal reduktion = (
                        new BigDecimal(stueck.intValue()/mengenSchwelle.intValue())
                        ).multiply(mengenAnzKostenlos).multiply(einzelpreis).
                        multiply(bc.minusOne);
                stueck = stueck.subtract(mengenAnzKostenlos);

                addRabattRow(rabattID, aktionsname, reduktion, stueck);
            }
        }
        for ( Vector<Object> vector : mengenrabattRelativVector ){
            int rabattID = (Integer) vector.get(0);
            String aktionsname = (String) vector.get(1);
            BigDecimal mengenSchwelle = (BigDecimal) vector.get(2);
            BigDecimal mengenRelRabatt = (BigDecimal) vector.get(3);
            if ( stueck.compareTo(mengenSchwelle) >= 0 ){ // if stueck >= mengenSchwelle
                BigDecimal reduktion = new BigDecimal(
                        bc.priceFormatterIntern(
                            stueck.multiply(mengenRelRabatt).multiply(einzelpreis).
                            multiply(bc.minusOne)
                            )
                        );
                einzelpreis = einzelpreis.subtract( mengenRelRabatt.multiply(einzelpreis) );

                addRabattRow(rabattID, aktionsname, reduktion, stueck);
            }
        }
    }

    private void addRabatteToVectors(BigDecimal einzelAbsolut, BigDecimal einzelRelativ, BigDecimal mengenSchwelle, BigDecimal mengenAnzahl, BigDecimal mengenRelativ,
            String aktionsname, int rabattID,
            Vector< Vector<Object> > einzelrabattAbsolutVector, Vector< Vector<Object> > einzelrabattRelativVector,
            Vector< Vector<Object> > mengenrabattAnzahlVector, Vector< Vector<Object> > mengenrabattRelativVector){
        if (einzelAbsolut != null){
            Vector<Object> temp = new Vector<Object>(3);
            temp.add( new Integer(rabattID) ); temp.add( aktionsname );
            temp.add( einzelAbsolut );
            einzelrabattAbsolutVector.add(temp);
        }
        if (einzelRelativ != null){
            Vector<Object> temp = new Vector<Object>(3);
            temp.add( new Integer(rabattID) ); temp.add( aktionsname );
            temp.add( einzelRelativ );
            einzelrabattRelativVector.add(temp);
        }
        if (mengenSchwelle != null && mengenAnzahl != null){
            Vector<Object> temp = new Vector<Object>(4);
            temp.add( new Integer(rabattID) ); temp.add( aktionsname );
            temp.add( mengenSchwelle ); temp.add( mengenAnzahl );
            mengenrabattAnzahlVector.add(temp);
        }
        if (mengenSchwelle != null && mengenRelativ != null){
            Vector<Object> temp = new Vector<Object>(4);
            temp.add( new Integer(rabattID) ); temp.add( aktionsname );
            temp.add( mengenSchwelle ); temp.add( mengenRelativ );
            mengenrabattRelativVector.add(temp);
        }
    }

    private void addRabattRow(int rabattID, String aktionsname, BigDecimal reduktion, BigDecimal stueck){
        BigDecimal artikelMwSt = kassierArtikel.lastElement().getMwst();

        KassierArtikel ka = new KassierArtikel(bc);
        ka.setPosition(null);
        ka.setArtikelID(null);
        ka.setRabattID(rabattID);
        ka.setName(einrueckung+aktionsname);
        ka.setColor("red");
        ka.setType("rabatt");
        ka.setMwst(artikelMwSt);
        ka.setStueckzahl(stueck.intValue());
        ka.setEinzelpreis(reduktion);
        ka.setGesPreis(reduktion);
        kassierArtikel.add(ka);

        mwsts.add(artikelMwSt);
        removeButtons.add(null);

        Vector<Object> row = new Vector<Object>();
            row.add(""); // pos
            row.add(einrueckung+aktionsname);
            row.add("RABATT"); row.add(stueck.toPlainString());
            row.add(bc.priceFormatter(reduktion)+" "+bc.currencySymbol);
            row.add(bc.priceFormatter(reduktion)+" "+bc.currencySymbol);
            row.add(bc.vatFormatter(artikelMwSt));
            row.add("");
        data.add(row);
    }

    private void checkForPfand(){
        BigDecimal stueck = new BigDecimal(selectedStueck);
        int pfandArtikelID = queryPfandArtikelID(selectedArtikelID);
        // gab es Pfand? Wenn ja, fuege Zeile in Tabelle:
        if ( pfandArtikelID > 0 ){
            BigDecimal pfand = new BigDecimal( getSalePrice(pfandArtikelID) );
            String pfandName = getArticleName(pfandArtikelID)[0];
            BigDecimal gesamtPfand = pfand.multiply(stueck);
            BigDecimal pfandMwSt = kassierArtikel.lastElement().getMwst();

            KassierArtikel ka = new KassierArtikel(bc);
            ka.setPosition(null);
            ka.setArtikelID(pfandArtikelID);
            ka.setRabattID(null);
            ka.setName(einrueckung+pfandName);
            ka.setColor("blue");
            ka.setType("pfand");
            ka.setMwst(pfandMwSt);
            ka.setStueckzahl(stueck.intValue());
            ka.setEinzelpreis(new BigDecimal( bc.priceFormatterIntern(pfand) ));
            ka.setGesPreis(new BigDecimal( bc.priceFormatterIntern(gesamtPfand) ));
            kassierArtikel.add(ka);

            mwsts.add(pfandMwSt);
            removeButtons.add(null);

            Vector<Object> row = new Vector<Object>();
                row.add(""); // pos
                row.add(einrueckung+pfandName); row.add("PFAND"); row.add(stueck);
                row.add( bc.priceFormatter(pfand)+' '+bc.currencySymbol );
                row.add( bc.priceFormatter(gesamtPfand)+' '+bc.currencySymbol );
                row.add(bc.vatFormatter(pfandMwSt));
                row.add("");
            data.add(row);
        }
    }

    private int queryPfandArtikelID(int artikelID) {
        int pfandArtikelID = -1;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT pfand.artikel_id FROM artikel INNER JOIN produktgruppe USING (produktgruppen_id) "+
                    "INNER JOIN pfand USING (pfand_id) "+
                    "WHERE artikel.artikel_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) { // artikel hat Pfand
                pfandArtikelID = rs.getInt(1);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return pfandArtikelID;
    }

    private boolean artikelHasPfand(){
        int artikelID = selectedArtikelID;
        boolean hasPfand = false;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT p.pfand_id IS NOT NULL FROM artikel AS a INNER JOIN produktgruppe AS p USING (produktgruppen_id) "+
                    "WHERE a.artikel_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            hasPfand = rs.getBoolean(1);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return hasPfand;
    }

    //////////////////////////////////
    // DB insert functions
    //////////////////////////////////
    private int insertIntoVerkauf(boolean ec) {
        int rechnungsNr = -1;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "INSERT INTO verkauf SET verkaufsdatum = NOW(), ec_zahlung = ?"
                    );
            pstmtSetBoolean(pstmt, 1, ec);
            int result = pstmt.executeUpdate();
            pstmt.close();
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Rechnung konnte nicht abgespeichert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT MAX(rechnungs_nr) FROM verkauf"
                    );
            rs.next(); rechnungsNr = rs.getInt(1); rs.close();
            stmt.close();
            for ( Map.Entry< BigDecimal, Vector<BigDecimal> > entry : this.vatMap.entrySet() ){
                pstmt = this.conn.prepareStatement(
                        "INSERT INTO verkauf_mwst SET rechnungs_nr = ?, mwst_satz = ?, "+
                        "mwst_netto = ?, mwst_betrag = ?"
                        );
                pstmtSetInteger(pstmt, 1, rechnungsNr);
                pstmt.setBigDecimal(2, entry.getKey());
                pstmt.setBigDecimal(3, entry.getValue().get(0));
                pstmt.setBigDecimal(4, entry.getValue().get(1));
                result = pstmt.executeUpdate();
                pstmt.close();
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: MwSt.-Information für Rechnung konnte nicht abgespeichert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
            for (int i=0; i<kassierArtikel.size(); i++){
                pstmt = this.conn.prepareStatement(
                        "INSERT INTO verkauf_details SET rechnungs_nr = ?, position = ?, "+
                        "artikel_id = ?, rabatt_id = ?, stueckzahl = ?, "+
                        "ges_preis = ?, mwst_satz = ?"
                        );
                pstmtSetInteger(pstmt, 1, rechnungsNr);
                pstmtSetInteger(pstmt, 2, kassierArtikel.get(i).getPosition());
                pstmtSetInteger(pstmt, 3, kassierArtikel.get(i).getArtikelID());
                pstmtSetInteger(pstmt, 4, kassierArtikel.get(i).getRabattID());
                pstmtSetInteger(pstmt, 5, kassierArtikel.get(i).getStueckzahl());
                pstmt.setBigDecimal(6, kassierArtikel.get(i).getGesPreis());
                pstmt.setBigDecimal(7, kassierArtikel.get(i).getMwst());
                result = pstmt.executeUpdate();
                pstmt.close();
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel mit ID "+
                            kassierArtikel.get(i).getArtikelID()+" konnte "+
                            "nicht abgespeichert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return rechnungsNr;
    }


    private void insertIntoKassenstand(int rechnungsNr) {
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT verkaufsdatum FROM verkauf WHERE rechnungs_nr = ?"
                    );
            pstmtSetInteger(pstmt, 1, rechnungsNr);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); String verkaufsdatum = rs.getString(1); rs.close();
            pstmt.close();
            BigDecimal betrag = new BigDecimal( getTotalPrice() );
            BigDecimal alterKassenstand = mainWindow.retrieveKassenstand();
            BigDecimal neuerKassenstand = alterKassenstand.add(betrag);
            pstmt = this.conn.prepareStatement(
                    "INSERT INTO kassenstand SET rechnungs_nr = ?,"+
                    "buchungsdatum = ?, "+
                    "manuell = FALSE, neuer_kassenstand = ?"
                    );
            pstmtSetInteger(pstmt, 1, rechnungsNr);
            pstmt.setString(2, verkaufsdatum);
            pstmt.setBigDecimal(3, neuerKassenstand);
            int result = pstmt.executeUpdate();
            pstmt.close();
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Kassenstand konnte nicht geändert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            } else {
                mainWindow.updateBottomPanel();
            }
        } catch (SQLException ex) {
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
            String artikelPreis = getSalePrice(selectedArtikelID);
            if (artikelPreis == null){
                artikelPreis = handleMissingSalePrice("Bitte Verkaufspreis eingeben");
            }
            preisField.getDocument().removeDocumentListener(this);
            preisField.setText( bc.decimalMark(artikelPreis) );
            preisField.getDocument().addDocumentListener(this);
        }
        else {
            preisField.setEditable(true);
        }
    }

    private void setButtonsEnabled() {
        if (preisField.getText().length() > 0) {
            hinzufuegenButton.setEnabled(true);
            leergutButton.setEnabled( artikelHasPfand() );
            ruecknahmeButton.setEnabled(true);
        }
        else {
            hinzufuegenButton.setEnabled(false);
            leergutButton.setEnabled(false);
            ruecknahmeButton.setEnabled(false);
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
            anzahlField.requestFocus();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    anzahlField.selectAll();
                }
            });
        }
        setButtonsEnabled();
    }

    private boolean checkKundeGibtField() {
        if ( kundeGibtField.getDocument().getLength() == 0 ){
            JOptionPane.showMessageDialog(this,
                    "Bitte Betrag bei 'Kunde gibt:' eintragen!",
                    "Fehlender Kundenbetrag", JOptionPane.WARNING_MESSAGE);
            kundeGibtField.requestFocus();
            return false;
        }
        return true;
    }

    private void hinzufuegen(Integer stueck, String color) {
        if (artikelBox.getItemCount() != 1 || nummerBox.getItemCount() != 1){
            System.out.println("Error: article not selected unambiguously.");
            return;
        }
        String[] an = artikelBox.parseArtikelName();
        String artikelName = an[0];
        String lieferant = an[1];
        String artikelNummer = (String)nummerBox.getSelectedItem();
        selectedStueck = stueck;
        String artikelPreis = bc.priceFormatterIntern(preisField.getText());
        BigDecimal gesPreis = new BigDecimal(artikelPreis).multiply(new BigDecimal(stueck));
        String gesPreisString = bc.priceFormatterIntern(gesPreis);
        String kurzname = getShortName(selectedArtikelID);
        String artikelMwSt = getVAT(selectedArtikelID);
        Boolean sortiment = getSortimentBool(selectedArtikelID);
        if (color == "default"){
            color = sortiment ? "default" : "gray";
        }

        Integer lastPos = 0;
        for (int i=kassierArtikel.size()-1; i>=0; i--){
            Integer val = kassierArtikel.get(i).getPosition();
            if (val != null){
                lastPos = val;
                break;
            }
        }
        KassierArtikel ka = new KassierArtikel(bc);
        ka.setPosition(lastPos+1);
        ka.setArtikelID(selectedArtikelID);
        ka.setRabattID(null);
        ka.setName(kurzname);
        ka.setColor(color);
        ka.setType("artikel");
        ka.setMwst(new BigDecimal(artikelMwSt));
        ka.setStueckzahl(stueck);
        ka.setEinzelpreis(new BigDecimal(artikelPreis));
        ka.setGesPreis(gesPreis);
        kassierArtikel.add(ka);

        mwsts.add(new BigDecimal(artikelMwSt));
        removeButtons.add(new JButton("-"));
        removeButtons.lastElement().addActionListener(this);

        gesPreisString = bc.decimalMark(gesPreisString)+' '+bc.currencySymbol;
        artikelPreis = bc.decimalMark(artikelPreis)+' '+bc.currencySymbol;

        Vector<Object> row = new Vector<Object>();
            row.add(ka.getPosition()); row.add(kurzname);
            row.add(artikelNummer); row.add(stueck.toString());
            row.add(artikelPreis); row.add(gesPreisString);
            row.add(bc.vatFormatter(artikelMwSt));
            row.add(removeButtons.lastElement());
        data.add(row);

        checkForRabatt();
        checkForPfand();
        updateAll();

        if (display != null && display.deviceWorks()){
            System.out.println("Going to display article.");
            String zws = bc.priceFormatter(getTotalPrice())+" "+bc.currencySymbol;
            display.printArticle(kurzname, stueck, artikelPreis, zws);
        }
    }

    private void leergutHinzufuegen() {
        int pfandArtikelID = queryPfandArtikelID(selectedArtikelID);
        // gab es Pfand? Wenn ja, fuege Zeile in Tabelle:
        if ( pfandArtikelID > 0 ){
            BigDecimal pfand = new BigDecimal( getSalePrice(pfandArtikelID) );
            String pfandName = getArticleName(pfandArtikelID)[0];
            Integer stueck = -(Integer)anzahlSpinner.getValue();
            selectedStueck = stueck;
            BigDecimal gesamtPfand = pfand.multiply(new BigDecimal(stueck));
            String pfandMwSt = getVAT(selectedArtikelID);

            KassierArtikel ka = new KassierArtikel(bc);
            ka.setPosition(null);
            ka.setArtikelID(pfandArtikelID);
            ka.setRabattID(null);
            ka.setName(pfandName);
            ka.setColor("green");
            ka.setType("leergut");
            ka.setMwst(new BigDecimal(pfandMwSt));
            ka.setStueckzahl(stueck);
            ka.setEinzelpreis(pfand);
            ka.setGesPreis(gesamtPfand);
            kassierArtikel.add(ka);

            mwsts.add(new BigDecimal(pfandMwSt));
            removeButtons.add(new JButton("-"));
            removeButtons.lastElement().addActionListener(this);

            Vector<Object> row = new Vector<Object>();
                row.add(""); // pos
                row.add(pfandName); row.add("LEERGUT"); row.add(stueck.toString());
                row.add( bc.priceFormatter(pfand)+' '+bc.currencySymbol );
                row.add( bc.priceFormatter(gesamtPfand)+' '+bc.currencySymbol );
                row.add(bc.vatFormatter(pfandMwSt));
                row.add(removeButtons.lastElement());
            data.add(row);

            updateAll();
        }
    }

    private void artikelHinzufuegen() {
        Integer stueck = (Integer)anzahlSpinner.getValue();
        hinzufuegen(stueck, "default");
    }

    private void ruecknahmeHinzufuegen() {
        Integer stueck = -(Integer)anzahlSpinner.getValue();
        hinzufuegen(stueck, "green");
    }

    private void zwischensumme() {
        bigPriceField.setText(totalPriceField.getText());
        updateRabattButtonsZwischensumme();

        barButton.setEnabled(true);
        ecButton.setEnabled(true);
        stornoButton.setEnabled(true);
    }

    private void bar() {
        zahlungsModus = "bar";
        zahlungsLabel.setText("Bar-Zahlung. Bitte jetzt abrechnen.");
        kundeGibtField.setEditable(true);
        gutscheinField.setEditable(true);
        //neuerKundeButton.setEnabled(false);
        neuerKundeButton.setEnabled(true);
        quittungsButton.setEnabled(true);
        kundeGibtField.requestFocus();
    }

    private void ec() {
        zahlungsModus = "ec";
        zahlungsLabel.setText("EC-Zahlung. Bitte jetzt EC-Gerät bedienen.");
        kundeGibtField.setText("");
        kundeGibtField.setEditable(false);
        gutscheinField.setEditable(true);
        neuerKundeButton.setEnabled(true);
        quittungsButton.setEnabled(true);
        neuerKundeButton.requestFocus();
    }

    private void gutschein() {
        selectedArtikelID = 3; // internal Gutschein artikel_id
        artikelBox.setBox( getArticleName(selectedArtikelID) );
        nummerBox.setBox( getArticleNumber(selectedArtikelID) );
        preisField.setText( gutscheinField.getText() );
        anzahlSpinner.setValue(1);
        ruecknahmeHinzufuegen();
        zwischensumme();
        if (zahlungsModus == "bar"){ bar(); }
        else if (zahlungsModus == "ec"){ ec(); }
    }

    private void neuerKunde() {
        if ( kundeGibtField.isEditable() ){ // if Barzahlung
            int rechnungsNr = insertIntoVerkauf(false);
            insertIntoKassenstand(rechnungsNr);
        } else {
            insertIntoVerkauf(true);
        }
        clearAll();
        updateAll();
    }

    private void stornieren() {
        clearAll();
        updateAll();
    }

    private void artikelRabattierenRelativ(BigDecimal rabattRelativ) {
        // Get data:
        int i=kassierArtikel.size()-1;
        while ( !kassierArtikel.get(i).getType().equals("artikel") ){
            i--;
        }
        // Now i points to the Artikel that gets the Rabatt
        BigDecimal einzelPreis = kassierArtikel.get(i).getEinzelPreis();
        BigDecimal gesPreis = kassierArtikel.get(i).getGesPreis();
        BigDecimal einzelReduktion = new BigDecimal(
                bc.priceFormatterIntern(
                    rabattRelativ.multiply(einzelPreis).multiply(bc.minusOne)
                    )
                );
        BigDecimal gesReduktion = new BigDecimal(
                bc.priceFormatterIntern(
                    rabattRelativ.multiply(gesPreis).multiply(bc.minusOne)
                    )
                );
        BigDecimal artikelMwSt = kassierArtikel.get(i).getMwst();
        String rabattName = getArticleName(artikelRabattArtikelID)[0];

        KassierArtikel ka = new KassierArtikel(bc);
        ka.setPosition(null);
        ka.setArtikelID(artikelRabattArtikelID);
        ka.setRabattID(null);
        ka.setName(einrueckung+rabattName);
        ka.setColor("red");
        ka.setType("rabatt");
        ka.setMwst(artikelMwSt);
        ka.setStueckzahl(selectedStueck);
        ka.setEinzelpreis(einzelReduktion);
        ka.setGesPreis(gesReduktion);
        kassierArtikel.add(i+1, ka);

        mwsts.add(artikelMwSt);
        removeButtons.add(null);

        Vector<Object> rabattRow = new Vector<Object>();
            rabattRow.add(""); // pos
            rabattRow.add(einrueckung+rabattName); rabattRow.add("RABATT");
            rabattRow.add(Integer.toString(selectedStueck));
            rabattRow.add(bc.priceFormatter(einzelReduktion)+" "+bc.currencySymbol);
            rabattRow.add(bc.priceFormatter(gesReduktion)+" "+bc.currencySymbol);
            rabattRow.add(bc.vatFormatter(artikelMwSt));
            rabattRow.add("");
        data.add(i+1, rabattRow);
        updateAll();
    }

    private void artikelRabattierenAbsolut(BigDecimal rabattAbsolut) {
        // Get data
        int i=kassierArtikel.size()-1;
        while ( !kassierArtikel.get(i).getType().equals("artikel") ){
            i--;
        }
        // Now i points to the Artikel that gets the Rabatt
        BigDecimal einzelReduktion = rabattAbsolut.multiply(bc.minusOne);
        BigDecimal stueck = new BigDecimal(selectedStueck);
        BigDecimal gesReduktion = einzelReduktion.multiply(stueck);
        BigDecimal artikelMwSt = kassierArtikel.get(i).getMwst();
        String rabattName = getArticleName(artikelRabattArtikelID)[0];

        KassierArtikel ka = new KassierArtikel(bc);
        ka.setPosition(null);
        ka.setArtikelID(artikelRabattArtikelID);
        ka.setRabattID(null);
        ka.setName(einrueckung+rabattName);
        ka.setColor("red");
        ka.setType("rabatt");
        ka.setMwst(artikelMwSt);
        ka.setStueckzahl(selectedStueck);
        ka.setEinzelpreis(einzelReduktion);
        ka.setGesPreis(gesReduktion);
        kassierArtikel.add(i+1, ka);

        mwsts.add(artikelMwSt);
        removeButtons.add(null);

        Vector<Object> rabattRow = new Vector<Object>();
            rabattRow.add(""); // pos
            rabattRow.add(einrueckung+rabattName); rabattRow.add("RABATT");
            rabattRow.add(Integer.toString(selectedStueck));
            rabattRow.add(bc.priceFormatter(einzelReduktion)+" "+bc.currencySymbol);
            rabattRow.add(bc.priceFormatter(gesReduktion)+" "+bc.currencySymbol);
            rabattRow.add(bc.vatFormatter(artikelMwSt));
            rabattRow.add("");
        data.add(i+1, rabattRow);
        updateAll();
    }

    private void addToHashMap(HashMap<BigDecimal, BigDecimal> hashMap, int artikelIndex){
        BigDecimal mwst = kassierArtikel.get(artikelIndex).getMwst();
        BigDecimal gesPreis = kassierArtikel.get(artikelIndex).getGesPreis();
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

    private void rechnungRabattierenRelativ(BigDecimal rabattRelativ) {
        int artikelIndex = -1;
        int rabattCounter = -1;
        HashMap<BigDecimal, BigDecimal> rabattArtikelPreise =
            new HashMap<BigDecimal, BigDecimal>();

        // determine place to start (after last "Rabatt auf Rechnung")
        int startIndex = 0;
        for (int i=kassierArtikel.size()-1; i>=0; i--){
            if (kassierArtikel.get(i).getType().equals("rabattrechnung")){
                startIndex = i+1;
                break;
            }
        }

        // scan through artikel list to search for artikels without rabatt
        for (int i=startIndex; i<kassierArtikel.size(); i++){
            if (kassierArtikel.get(i).getType().equals("artikel")){
                if ( rabattCounter == 0 ){ // previous artikel had no rabatt
                    addToHashMap(rabattArtikelPreise, artikelIndex);
                }
                artikelIndex = i;
                rabattCounter = 0;
            } else if (kassierArtikel.get(i).getType().equals("rabatt")){
                rabattCounter++;
            }
        }
        if (rabattCounter == 0){ // for last artikel
            addToHashMap(rabattArtikelPreise, artikelIndex);
        }
        //for ( Map.Entry<BigDecimal, BigDecimal> entry : rabattArtikelPreise.entrySet() ){
        //    System.out.println(entry.getKey() + "  " + entry.getValue());
        //}

        String rabattName = getArticleName(rechnungRabattArtikelID)[0];

        // Loop over the VAT levels:
        for ( Map.Entry<BigDecimal, BigDecimal> entry : rabattArtikelPreise.entrySet() ){
            BigDecimal reduktion = new BigDecimal( bc.priceFormatterIntern(
                        rabattRelativ.multiply(entry.getValue()).multiply(bc.minusOne)
                        ));
            BigDecimal mwst = entry.getKey();

            KassierArtikel ka = new KassierArtikel(bc);
            ka.setPosition(null);
            ka.setArtikelID(rechnungRabattArtikelID);
            ka.setRabattID(null);
            ka.setName(rabattName);
            ka.setColor("red");
            ka.setType("rabattrechnung");
            ka.setMwst(mwst);
            ka.setStueckzahl(1);
            ka.setEinzelpreis(reduktion);
            ka.setGesPreis(reduktion);
            kassierArtikel.add(ka);

            mwsts.add(mwst);
            removeButtons.add(new JButton("-"));
            removeButtons.lastElement().addActionListener(this);

            Vector<Object> rabattRow = new Vector<Object>();
                rabattRow.add(""); // pos
                rabattRow.add(rabattName); rabattRow.add("RABATT"); rabattRow.add(1);
                rabattRow.add(bc.priceFormatter(reduktion)+" "+bc.currencySymbol);
                rabattRow.add(bc.priceFormatter(reduktion)+" "+bc.currencySymbol);
                rabattRow.add(bc.vatFormatter(mwst));
                rabattRow.add(removeButtons.lastElement());
            data.add(rabattRow);

            // updateAll fuer Arme
            this.remove(allPanel);
            this.revalidate();
            showAll();

            zwischensumme();
        }
    }

    private void removeRabattAufRechnung() {
        for (int i=kassierArtikel.size()-1; i>=0; i--){
            if (!kassierArtikel.get(i).getType().equals("rabattrechnung")){
                break; // stop at first row that is no "Rabatt auf Rechnung"
            } else { // remove the rabatt row
                data.remove(i);
                kassierArtikel.remove(i);
                mwsts.remove(i);
                removeButtons.remove(i);
            }
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

    private void checkBarcodeBox(ActionEvent e) {
        if ( barcodeBox.getItemCount() == 1 ){ // if selection is correct and unique
            setArtikelNameAndNummerForBarcode();
        }
        checkIfFormIsComplete();
    }
    private void checkArtikelBox(ActionEvent e) {
        System.out.println("actionPerformed in artikelBox, actionCommand: "+e.getActionCommand()+", modifiers: "+e.getModifiers()+", itemCount: "+artikelBox.getItemCount()+", selectedItem: "+artikelBox.getSelectedItem()+"   artikelNameText: "+artikelNameText);
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
        if (e.getDocument() == gutscheinField.getDocument()){
            gutscheinButton.setEnabled( gutscheinField.getText().length() > 0 );
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
        for (int i=0; i<kassierArtikel.size(); i++){
            data.get(i).set(0, kassierArtikel.get(i).getPosition());
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
        if (e.getSource() == sonstigesButton){
            artikelField.setText(", Sonstige");
            return;
        }
        if (e.getSource() == sevenPercentButton){
            artikelField.setText("Variabler Preis 7%");
            return;
        }
        if (e.getSource() == nineteenPercentButton){
            artikelField.setText("Variabler Preis 19%");
            return;
        }
        if (e.getSource() == hinzufuegenButton){
            removeRabattAufRechnung();
            artikelHinzufuegen();
	    return;
	}
        if (e.getSource() == leergutButton){
            removeRabattAufRechnung();
            leergutHinzufuegen();
	    return;
	}
        if (e.getSource() == ruecknahmeButton){
            removeRabattAufRechnung();
            ruecknahmeHinzufuegen();
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
        if (e.getSource() == zwischensummeButton){
            zwischensumme();
	    return;
	}
	if (e.getSource() == barButton){
            bar();
	    return;
	}
	if (e.getSource() == ecButton){
            if ( ( new BigDecimal(getTotalPrice()) ).compareTo(ecSchwelle) < 0 ){
                int answer = JOptionPane.showConfirmDialog(this,
                        "ACHTUNG: Gesamtbetrag unter "+bc.priceFormatter(ecSchwelle)+" "+
                        bc.currencySymbol+" !\n"+
                        "Wirklich EC-Zahlung erlauben?", "Warnung",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (answer == JOptionPane.NO_OPTION){
                    return;
                }
            }
            ec();
	    return;
	}
	if (e.getSource() == gutscheinButton){
            gutschein();
	    return;
	}
	if (e.getSource() == stornoButton){
            stornieren();
	    return;
	}
	if (e.getSource() == quittungsButton){
            if (zahlungsModus == "bar"){
                boolean ok = checkKundeGibtField();
                if (!ok){
                    return;
                }
            }
            Vector<BigDecimal> vats = retrieveVATs();
            // LinkedHashMap preserves insertion order
            LinkedHashMap< BigDecimal, Vector<BigDecimal> > mwstsAndTheirValues =
                new LinkedHashMap< BigDecimal, Vector<BigDecimal> >();
            for ( BigDecimal vat : vats ){
                //if (vat.signum() != 0){
                    if ( mwsts.contains(vat) ){
                        System.out.println(vat);
                        Vector<BigDecimal> values = new Vector<BigDecimal>();
                        BigDecimal brutto = calculateTotalVATUmsatz(vat);
                        BigDecimal steuer = calculateTotalVATAmount(vat);
                        BigDecimal netto = new BigDecimal(
                                bc.priceFormatterIntern(brutto.subtract(steuer))
                                );
                        values.add(netto); // Netto
                        values.add(steuer); // Steuer
                        values.add(brutto); // Umsatz
                        mwstsAndTheirValues.put(vat, values);
                    }
                //}
            }
            BigDecimal totalPrice = new BigDecimal( getTotalPrice() );
            BigDecimal kundeGibt = null, rueckgeld = null;
            if (kundeGibtField.getDocument().getLength() > 0){
                kundeGibt = new BigDecimal( getKundeGibt() );
                rueckgeld = kundeGibt.subtract(totalPrice);
            }
            Quittung myQuittung = new Quittung(this.conn, this.mainWindow,
                    DateTime.now(TimeZone.getDefault()), kassierArtikel,
                    mwstsAndTheirValues, zahlungsModus,
                    totalPrice, kundeGibt, rueckgeld);
            myQuittung.printReceipt();
	    return;
	}
	if (e.getSource() == neuerKundeButton){
            if (zahlungsModus == "bar"){
                boolean ok = checkKundeGibtField();
                if (!ok){
                    return;
                }
            }
            neuerKunde();
            tabbedPane.recreateTabbedPane();
	    return;
	}
	if (e.getSource() == mitarbeiterRabattButton){
            rechnungRabattierenRelativ(mitarbeiterRabatt);
	    return;
	}
	int rabattIndex = -1;
	for (int i=0; i<rabattButtons.size(); i++){
	    if (e.getSource() == rabattButtons.get(i) ){
		rabattIndex = i;
		break;
	    }
	}
        if (rabattIndex > -1){
            BigDecimal rabattAnteil = new BigDecimal(
                    bc.vatParser( rabattButtons.get(rabattIndex).getText() )
                    );
            if (barButton.isEnabled()){
                rechnungRabattierenRelativ(rabattAnteil);
            }
            else {
                artikelRabattierenRelativ(rabattAnteil);
            }
            return;
        }
	if (e.getSource() == individuellRabattRelativButton){
            BigDecimal rabattAnteil = new BigDecimal(
                    bc.vatParser( individuellRabattRelativField.getText() )
                    );
            if (barButton.isEnabled()){
                rechnungRabattierenRelativ(rabattAnteil);
            }
            else {
                artikelRabattierenRelativ(rabattAnteil);
            }
            individuellRabattRelativField.setText("");
	    return;
	}
	if (e.getSource() == individuellRabattAbsolutButton){
            BigDecimal rabatt = new BigDecimal(
                    bc.priceFormatterIntern( individuellRabattAbsolutField.getText() )
                    );
            if (barButton.isEnabled()){
                // do nothing
            }
            else {
                artikelRabattierenAbsolut(rabatt);
            }
            individuellRabattAbsolutField.setText("");
	    return;
	}
	int removeRow = -1;
	for (int i=0; i<kassierArtikel.size(); i++){
	    if (e.getSource() == removeButtons.get(i) ){
		removeRow = i;
		break;
	    }
	}
        if (removeRow > -1){
            data.remove(removeRow);
            kassierArtikel.remove(removeRow);
            mwsts.remove(removeRow);
            removeButtons.remove(removeRow);

            // remove extra rows (Rabatt oder Pfand):
            while ( removeRow < kassierArtikel.size() &&
                    removeButtons.get(removeRow) == null ){
                data.remove(removeRow);
                kassierArtikel.remove(removeRow);
                mwsts.remove(removeRow);
                removeButtons.remove(removeRow);
            }

            for (int i=removeRow; i<kassierArtikel.size(); i++){
                Integer oldVal = kassierArtikel.get(i).getPosition();
                if (oldVal != null){
                    kassierArtikel.get(i).setPosition(oldVal-1);
                }
            }

            removeRabattAufRechnung();

            refreshPositionsInData();
            updateAll();
            return;
        }
    }

}
