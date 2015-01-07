package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector
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

// DateTime from date4j (http://www.date4j.net/javadoc/index.html)
import hirondelle.date4j.DateTime;

import WeltladenDB.MainWindowGrundlage;
import WeltladenDB.BarcodeComboBox;
import WeltladenDB.ArtikelNameComboBox;
import WeltladenDB.ArtikelNummerComboBox;
import WeltladenDB.NumberDocumentFilter;
import WeltladenDB.BoundsPopupMenuListener;

public class Kassieren extends RechnungsGrundlage implements ItemListener, DocumentListener {
    // Attribute:
    private final BigDecimal mitarbeiterRabatt = new BigDecimal("0.1");
    private final boolean allowMitarbeiterRabatt = true;
    private final BigDecimal ecSchwelle = new BigDecimal("20.00");
    private final BigDecimal minusOne = new BigDecimal(-1);
    private final BigDecimal percent = new BigDecimal("0.01");
    private int artikelRabattArtikelID = 1;
    private int rechnungRabattArtikelID = 2;
    private String zahlungsModus = "unbekannt";

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
    private JSpinner anzahlSpinner;
    private JTextField anzahlField;
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
    private Vector<JButton> removeButtons;
    private JButton zwischensummeButton;
    private JButton barButton;
    private JButton ecButton;
    private JButton stornoButton;
    private JButton gutscheinButton;
    private JLabel zahlungsLabel;
    private JButton quittungsButton;
    private JButton neuerKundeButton;
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

    private Vector<Integer> positions;
    private Vector<Integer> artikelIDs;
    private Vector<String> articleNames;
    private Vector<Integer> rabattIDs;
    private Vector<Integer> stueckzahlen;
    private Vector<BigDecimal> einzelpreise;

    private NumberDocumentFilter geldFilter = new NumberDocumentFilter(2, 13);

    // Methoden:

    /**
     *    The constructor.
     *       */
    public Kassieren(Connection conn, MainWindowGrundlage mw, TabbedPane tp) {
	super(conn, mw);
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

    void showAll(){
        updateRabattButtons();

	allPanel = new JPanel();
	allPanel.setLayout(new BoxLayout(allPanel, BoxLayout.Y_AXIS));

        JPanel barcodePanel = new JPanel();
	barcodePanel.setLayout(new FlowLayout());
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
            nummerBox.setPrototypeDisplayValue("qqqqqqq");
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

	    leergutButton = new JButton("Als Leergut");
            leergutButton.setMnemonic(KeyEvent.VK_L);
	    leergutButton.addActionListener(this);
	    leergutButton.setEnabled(false);
	    chooseArticlePanel2.add(leergutButton);

	    ruecknahmeButton = new JButton("Rückgabe");
            ruecknahmeButton.setMnemonic(KeyEvent.VK_R);
	    ruecknahmeButton.addActionListener(this);
	    ruecknahmeButton.setEnabled(false);
	    chooseArticlePanel2.add(ruecknahmeButton);
        allPanel.add(chooseArticlePanel2);

	showTable();

        JPanel zwischensummePanel = new JPanel();
        zwischensummePanel.setLayout(new FlowLayout());
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

            JPanel kundeGibtRueckgeldPanel = new JPanel();
            kundeGibtRueckgeldPanel.setLayout(new GridLayout(0,2));
                kundeGibtRueckgeldPanel.add(new JLabel("Kunde gibt:"));
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
                kundeGibtPanel.setLayout(new FlowLayout());
                kundeGibtPanel.add(kundeGibtField);
                kundeGibtPanel.add(new JLabel(currencySymbol));
            kundeGibtPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            kundeGibtRueckgeldPanel.add(kundeGibtPanel);
                kundeGibtRueckgeldPanel.add(new JLabel("Gutschein:"));
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
                gutscheinPanel.setLayout(new FlowLayout());
                gutscheinPanel.add(gutscheinField);
                gutscheinPanel.add(new JLabel(currencySymbol));
                gutscheinButton = new JButton("OK");
                gutscheinButton.setEnabled(false);
                gutscheinButton.addActionListener(this);
                gutscheinPanel.add(gutscheinButton);
            gutscheinPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            kundeGibtRueckgeldPanel.add(gutscheinPanel);
                kundeGibtRueckgeldPanel.add(new JLabel("Rückgeld:"));
                JPanel rueckgeldPanel = new JPanel();
                rueckgeldPanel.setLayout(new FlowLayout());
                rueckgeldField = new JTextField("");
                rueckgeldField.setEditable(false);
                rueckgeldField.setColumns(7);
                rueckgeldField.setHorizontalAlignment(JTextField.RIGHT);
                rueckgeldField.setFont(new Font("Tahoma", Font.BOLD, 12));
                rueckgeldPanel.add(rueckgeldField);
                rueckgeldPanel.add(new JLabel(currencySymbol));
            rueckgeldPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            kundeGibtRueckgeldPanel.add(rueckgeldPanel);

            zwischensummePanel.add(kundeGibtRueckgeldPanel);

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
            neuerKundePanel.add(Box.createRigidArea(new Dimension(0,5)));
            JPanel buttonPanel = new JPanel();
                quittungsButton = new JButton("Quittung");
                quittungsButton.setEnabled(false);
                quittungsButton.addActionListener(this);
                quittungsButton.setAlignmentX(JComponent.CENTER_ALIGNMENT);
                buttonPanel.add(quittungsButton);
                neuerKundeButton = new JButton("Fertig/Nächster Kunde");
                neuerKundeButton.setEnabled(false);
                neuerKundeButton.addActionListener(this);
                neuerKundeButton.setAlignmentX(JComponent.CENTER_ALIGNMENT);
                buttonPanel.add(neuerKundeButton);
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
        }
        mitarbeiterRabattButton = new JButton("Mitarbeiter");
        mitarbeiterRabattButton.addActionListener(this);
        mitarbeiterRabattButton.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        rabattPanel.add(mitarbeiterRabattButton);
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
            FloatDocumentFilter fdf = new FloatDocumentFilter();
            ((AbstractDocument)individuellRabattRelativField.getDocument()).setDocumentFilter(fdf);
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
            absolutPanel.add(new JLabel(currencySymbol));
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
        if (types.size() > 0){
            enabled = true;
            for (int i=types.size()-1; i>=0; i--){
                if ( types.get(i).equals("rabatt") ){
                    enabled = false; // Artikel hat schon Rabatt, keinen Rabatt mehr erlauben
                    break;
                }
                if ( types.get(i).equals("leergut") ){
                    enabled = false; // Es handelt sich um Leergut, kein Rabatt erlauben
                    break;
                }
                if ( types.get(i).equals("artikel") ){
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
        if (!types.lastElement().equals("rabattrechnung")){
            // determine place to start (after last "Rabatt auf Rechnung")
            int startIndex = 0;
            for (int i=types.size()-1; i>=0; i--){
                if (types.get(i).equals("rabattrechnung")){
                    startIndex = i+1;
                    break;
                }
            }
            // scan through artikel list to search for artikels without rabatt
            int artikelCount = 0;
            for (int i=startIndex; i<types.size(); i++){
                if ( types.get(i).equals("artikel") ){
                    artikelCount++;
                }
                else if ( types.get(i).equals("rabatt") ){
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
        positions = new Vector<Integer>();
        artikelIDs = new Vector<Integer>();
        articleNames = new Vector<String>();
        rabattIDs = new Vector<Integer>();
        mwsts = new Vector<BigDecimal>();
        colors = new Vector<String>();
        types = new Vector<String>();
        stueckzahlen = new Vector<Integer>();
        einzelpreise = new Vector<BigDecimal>();
        preise = new Vector<BigDecimal>();
        removeButtons = new Vector<JButton>();
    }

    private void clearAll(){
        data.clear();
        positions.clear();
        artikelIDs.clear();
        articleNames.clear();
        rabattIDs.clear();
        colors.clear();
        types.clear();
        mwsts.clear();
        stueckzahlen.clear();
        einzelpreise.clear();
        preise.clear();
        removeButtons.clear();
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
            BigDecimal kundeGibt = new BigDecimal(kundeGibtField.getText().replace(',','.'));
            BigDecimal rueckgeld = kundeGibt.subtract(totalPrice);
            if (rueckgeld.signum() < 0){
                rueckgeldField.setForeground(Color.red);
                //neuerKundeButton.setEnabled(false);
            } else {
                rueckgeldField.setForeground(Color.green.darker().darker());
                //neuerKundeButton.setEnabled(true);
            }
            rueckgeldField.setText( priceFormatter(rueckgeld) );
        }
    }

    String getTotalPrice() {
        return priceFormatterIntern( totalPriceField.getText() );
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
        BigDecimal einzelpreis = new BigDecimal(preisField.getText().replace(',','.').replace("(r) ",""));
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
                BigDecimal einzelAbsolut = rs.getString(1) == null ? null : new BigDecimal(rs.getString(1));
                BigDecimal einzelRelativ = rs.getString(2) == null ? null : new BigDecimal(rs.getString(2));
                BigDecimal mengenSchwelle = rs.getString(3) == null ? null : new BigDecimal(rs.getInt(3));
                BigDecimal mengenAnzahl = rs.getString(4) == null ? null : new BigDecimal(rs.getInt(4));
                BigDecimal mengenRelativ = rs.getString(5) == null ? null : new BigDecimal(rs.getString(5));
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
                BigDecimal einzelAbsolut = rs.getString(1) == null ? null : new BigDecimal(rs.getString(1));
                BigDecimal einzelRelativ = rs.getString(2) == null ? null : new BigDecimal(rs.getString(2));
                BigDecimal mengenSchwelle = rs.getString(3) == null ? null : new BigDecimal(rs.getInt(3));
                BigDecimal mengenAnzahl = rs.getString(4) == null ? null : new BigDecimal(rs.getInt(4));
                BigDecimal mengenRelativ = rs.getString(5) == null ? null : new BigDecimal(rs.getString(5));
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
        System.out.println("Die Vektoren:");
        System.out.println(einzelrabattAbsolutVector);
        System.out.println(einzelrabattRelativVector);
        System.out.println(mengenrabattAnzahlVector);
        System.out.println(mengenrabattRelativVector);
        for ( Vector<Object> vector : einzelrabattAbsolutVector ){
            int rabattID = (Integer) vector.get(0);
            String aktionsname = (String) vector.get(1);
            BigDecimal einzelAbsRabatt = (BigDecimal) vector.get(2);
            BigDecimal reduktion = stueck.multiply(einzelAbsRabatt).multiply(minusOne);
            einzelpreis = einzelpreis.subtract(einzelAbsRabatt);

            addRabattRow(rabattID, aktionsname, reduktion, stueck);
        }
        for ( Vector<Object> vector : einzelrabattRelativVector ){
            int rabattID = (Integer) vector.get(0);
            String aktionsname = (String) vector.get(1);
            BigDecimal einzelRelRabatt = (BigDecimal) vector.get(2);
            BigDecimal reduktion = stueck.multiply(einzelRelRabatt).multiply(einzelpreis).multiply(minusOne);
            einzelpreis = einzelpreis.subtract( einzelRelRabatt.multiply(einzelpreis) );

            addRabattRow(rabattID, aktionsname, reduktion, stueck);
        }
        for ( Vector<Object> vector : mengenrabattAnzahlVector ){
            int rabattID = (Integer) vector.get(0);
            String aktionsname = (String) vector.get(1);
            BigDecimal mengenSchwelle = (BigDecimal) vector.get(2);
            BigDecimal mengenAnzKostenlos = (BigDecimal) vector.get(3);
            if ( stueck.compareTo(mengenSchwelle) >= 0 ){ // if stueck >= mengenSchwelle
                BigDecimal reduktion = (new BigDecimal(stueck.intValue()/mengenSchwelle.intValue())).
                        multiply(mengenAnzKostenlos).multiply(einzelpreis).multiply(minusOne);
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
                BigDecimal reduktion = stueck.multiply(mengenRelRabatt).multiply(einzelpreis).multiply(minusOne);
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
        BigDecimal artikelMwSt = mwsts.lastElement();
        positions.add(null);
        artikelIDs.add(null);
        articleNames.add(einrueckung+aktionsname);
        rabattIDs.add(rabattID);
        colors.add("red");
        types.add("rabatt");
        mwsts.add(artikelMwSt);
        stueckzahlen.add(stueck.intValue());
        einzelpreise.add(null);
        preise.add(reduktion);
        removeButtons.add(null);

        Vector<Object> row = new Vector<Object>();
            row.add(""); // pos
            row.add(einrueckung+aktionsname); row.add("RABATT"); row.add(stueck.toPlainString());
            row.add(""); row.add(priceFormatter(reduktion)+" "+currencySymbol);
            row.add(vatFormatter(artikelMwSt));
            row.add("");
        data.add(row);
    }

    private void checkForPfand(){
        BigDecimal stueck = new BigDecimal(selectedStueck);
        int pfandArtikelID = queryPfandArtikelID(selectedArtikelID);
        // gab es Pfand? Wenn ja, fuege Zeile in Tabelle:
        if ( pfandArtikelID > 0 ){
            BigDecimal pfand = new BigDecimal( getPrice(pfandArtikelID) );
            String pfandName = getArticleName(pfandArtikelID)[0];
            BigDecimal gesamtPfand = pfand.multiply(stueck);
            BigDecimal pfandMwSt = mwsts.lastElement();

            positions.add(null);
            artikelIDs.add(pfandArtikelID);
            articleNames.add(einrueckung+pfandName);
            rabattIDs.add(null);
            colors.add("blue");
            types.add("pfand");
            mwsts.add(pfandMwSt);
            stueckzahlen.add(stueck.intValue());
            einzelpreise.add(new BigDecimal( priceFormatterIntern(pfand) ));
            preise.add(new BigDecimal( priceFormatterIntern(gesamtPfand) ));
            removeButtons.add(null);

            Vector<Object> row = new Vector<Object>();
                row.add(""); // pos
                row.add(einrueckung+pfandName); row.add("PFAND"); row.add(stueck);
                row.add( priceFormatter(pfand)+' '+currencySymbol );
                row.add( priceFormatter(gesamtPfand)+' '+currencySymbol );
                row.add(vatFormatter(pfandMwSt));
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
            System.out.println("vatMap: "+vatMap);
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
            for (int i=0; i<artikelIDs.size(); i++){
                pstmt = this.conn.prepareStatement(
                        "INSERT INTO verkauf_details SET rechnungs_nr = ?, position = ?, "+
                        "artikel_id = ?, rabatt_id = ?, stueckzahl = ?, "+
                        "ges_preis = ?, mwst_satz = ?"
                        );
                pstmtSetInteger(pstmt, 1, rechnungsNr);
                pstmtSetInteger(pstmt, 2, positions.get(i));
                pstmtSetInteger(pstmt, 3, artikelIDs.get(i));
                pstmtSetInteger(pstmt, 4, rabattIDs.get(i));
                pstmtSetInteger(pstmt, 5, stueckzahlen.get(i));
                pstmt.setBigDecimal(6, preise.get(i));
                pstmt.setBigDecimal(7, mwsts.get(i));
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
        String artikelPreis = priceFormatterIntern(preisField.getText());
        BigDecimal gesPreis = new BigDecimal(artikelPreis).multiply(new BigDecimal(stueck));
        String artikelGesamtPreis = priceFormatterIntern(gesPreis);
        String artikelMwSt = getVAT(selectedArtikelID);
        Boolean sortiment = getSortimentBool(selectedArtikelID);
        if (color == "default"){
            color = sortiment ? "default" : "gray";
        }

        Integer lastPos = 0;
        for (int i=positions.size()-1; i>=0; i--){
            Integer val = positions.get(i);
            if (val != null){
                lastPos = val;
                break;
            }
        }
        positions.add(lastPos+1);
        artikelIDs.add(selectedArtikelID);
        articleNames.add(artikelName);
        rabattIDs.add(null);
        stueckzahlen.add(stueck);
        einzelpreise.add(new BigDecimal(artikelPreis));
        preise.add(gesPreis);
        colors.add(color);
        types.add("artikel");
        mwsts.add(new BigDecimal(artikelMwSt));
        removeButtons.add(new JButton("-"));
        removeButtons.lastElement().addActionListener(this);

        artikelGesamtPreis = artikelGesamtPreis.replace('.',',')+' '+currencySymbol;
        artikelPreis = artikelPreis.replace('.',',')+' '+currencySymbol;

        Vector<Object> row = new Vector<Object>();
            row.add(positions.lastElement());
            row.add(artikelName); row.add(artikelNummer); row.add(stueck.toString());
            row.add(artikelPreis); row.add(artikelGesamtPreis); row.add(vatFormatter(artikelMwSt));
            row.add(removeButtons.lastElement());
        data.add(row);

        checkForRabatt();
        checkForPfand();
        updateAll();
    }

    private void leergutHinzufuegen() {
        int pfandArtikelID = queryPfandArtikelID(selectedArtikelID);
        // gab es Pfand? Wenn ja, fuege Zeile in Tabelle:
        if ( pfandArtikelID > 0 ){
            BigDecimal pfand = new BigDecimal( getPrice(pfandArtikelID) );
            String pfandName = getArticleName(pfandArtikelID)[0];
            Integer stueck = -(Integer)anzahlSpinner.getValue();
            selectedStueck = stueck;
            BigDecimal gesamtPfand = pfand.multiply(new BigDecimal(stueck));
            String pfandMwSt = getVAT(selectedArtikelID);

            positions.add(null);
            artikelIDs.add(pfandArtikelID);
            articleNames.add(pfandName);
            rabattIDs.add(null);
            stueckzahlen.add(stueck);
            einzelpreise.add(pfand);
            preise.add(gesamtPfand);
            colors.add("green");
            types.add("leergut");
            mwsts.add(new BigDecimal(pfandMwSt));
            removeButtons.add(new JButton("-"));
            removeButtons.lastElement().addActionListener(this);

            Vector<Object> row = new Vector<Object>();
                row.add(""); // pos
                row.add(pfandName); row.add("LEERGUT"); row.add(stueck.toString());
                row.add( priceFormatter(pfand)+' '+currencySymbol );
                row.add( priceFormatter(gesamtPfand)+' '+currencySymbol );
                row.add(vatFormatter(pfandMwSt));
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
        if ( ( new BigDecimal(getTotalPrice()) ).compareTo(ecSchwelle) >= 0 ){
            ecButton.setEnabled(true);
        }
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
        int i=data.size()-1;
        while ( !types.get(i).equals("artikel") ){
            i--;
        }
        // Now i points to the Artikel that gets the Rabatt
        BigDecimal gesPreis = preise.get(i);
        BigDecimal reduktion = rabattRelativ.multiply(gesPreis).multiply(minusOne);
        BigDecimal artikelMwSt = mwsts.get(i);
        positions.add(i+1, null);
        artikelIDs.add(i+1, artikelRabattArtikelID);
        rabattIDs.add(i+1, null);
        einzelpreise.add(i+1, null);
        preise.add(i+1, reduktion);
        colors.add(i+1, "red");
        types.add(i+1, "rabatt");
        mwsts.add(i+1, artikelMwSt);
        stueckzahlen.add(i+1, selectedStueck);
        removeButtons.add(i+1, null);

        String rabattName = getArticleName(artikelRabattArtikelID)[0];
        articleNames.add(i+1, einrueckung+rabattName);

        Vector<Object> rabattRow = new Vector<Object>();
            rabattRow.add(""); // pos
            rabattRow.add(einrueckung+rabattName); rabattRow.add("RABATT"); rabattRow.add(Integer.toString(selectedStueck));
            rabattRow.add(""); rabattRow.add(priceFormatter(reduktion)+" "+currencySymbol);
            rabattRow.add(vatFormatter(artikelMwSt));
            rabattRow.add("");
        data.add(i+1, rabattRow);
        updateAll();
    }

    private void artikelRabattierenAbsolut(BigDecimal rabattAbsolut) {
        int i=data.size()-1;
        while ( !types.get(i).equals("artikel") ){
            i--;
        }
        // Now i points to the Artikel that gets the Rabatt
        BigDecimal reduktion = rabattAbsolut.multiply(minusOne);
        BigDecimal artikelMwSt = mwsts.get(i);
        positions.add(i+1, null);
        artikelIDs.add(i+1, artikelRabattArtikelID);
        rabattIDs.add(i+1, null);
        einzelpreise.add(i+1, null);
        preise.add(i+1, reduktion);
        colors.add(i+1, "red");
        types.add(i+1, "rabatt");
        mwsts.add(i+1, artikelMwSt);
        stueckzahlen.add(i+1, selectedStueck);
        removeButtons.add(i+1, null);

        String rabattName = getArticleName(artikelRabattArtikelID)[0];
        articleNames.add(i+1, einrueckung+rabattName);

        Vector<Object> rabattRow = new Vector<Object>();
            rabattRow.add(""); // pos
            rabattRow.add(einrueckung+rabattName); rabattRow.add("RABATT"); rabattRow.add(Integer.toString(selectedStueck));
            rabattRow.add(""); rabattRow.add(priceFormatter(reduktion)+" "+currencySymbol);
            rabattRow.add(vatFormatter(artikelMwSt));
            rabattRow.add("");
        data.add(i+1, rabattRow);
        updateAll();
    }

    private void addToHashMap(HashMap<BigDecimal, BigDecimal> hashMap, int artikelIndex){
        BigDecimal mwst = mwsts.get(artikelIndex);
        BigDecimal gesPreis = preise.get(artikelIndex);
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
        HashMap<BigDecimal, BigDecimal> rabattArtikelPreise = new HashMap<BigDecimal, BigDecimal>();
        // determine place to start (after last "Rabatt auf Rechnung")
        int startIndex = 0;
        for (int i=types.size()-1; i>=0; i--){
            if (types.get(i).equals("rabattrechnung")){
                startIndex = i+1;
                break;
            }
        }
        // scan through artikel list to search for artikels without rabatt
        for (int i=startIndex; i<types.size(); i++){
            if (types.get(i).equals("artikel")){
                if ( rabattCounter == 0 ){ // previous artikel had no rabatt
                    addToHashMap(rabattArtikelPreise, artikelIndex);
                }
                artikelIndex = i;
                rabattCounter = 0;
            } else if (types.get(i).equals("rabatt")){
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

        for ( Map.Entry<BigDecimal, BigDecimal> entry : rabattArtikelPreise.entrySet() ){
            positions.add(null);
            artikelIDs.add(rechnungRabattArtikelID);
            articleNames.add(rabattName);
            rabattIDs.add(null);
            BigDecimal reduktion = rabattRelativ.multiply(entry.getValue()).multiply(minusOne);
            einzelpreise.add(null);
            preise.add(reduktion);
            colors.add("red");
            types.add("rabattrechnung");
            BigDecimal mwst = entry.getKey();
            mwsts.add(mwst);
            stueckzahlen.add(null);
            removeButtons.add(new JButton("-"));
            removeButtons.lastElement().addActionListener(this);

            Vector<Object> rabattRow = new Vector<Object>();
                rabattRow.add(""); // pos
                rabattRow.add(rabattName); rabattRow.add("RABATT"); rabattRow.add("");
                rabattRow.add(""); rabattRow.add(priceFormatter(reduktion)+" "+currencySymbol);
                rabattRow.add(vatFormatter(mwst));
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
        for (int i=types.size()-1; i>=0; i--){
            if (!types.get(i).equals("rabattrechnung")){
                break; // stop at first row that is no "Rabatt auf Rechnung"
            } else { // remove the rabatt row
                data.remove(i);
                artikelIDs.remove(i);
                articleNames.remove(i);
                rabattIDs.remove(i);
                einzelpreise.remove(i);
                preise.remove(i);
                colors.remove(i);
                types.remove(i);
                mwsts.remove(i);
                stueckzahlen.remove(i);
                removeButtons.remove(i);

                positions.remove(i);
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
                        BigDecimal netto = new BigDecimal( priceFormatterIntern(brutto.subtract(steuer)) );
                        values.add(netto); // Netto
                        values.add(steuer); // Steuer
                        values.add(brutto); // Umsatz
                        mwstsAndTheirValues.put(vat, values);
                    }
                //}
            }
            BigDecimal totalPrice = new BigDecimal( getTotalPrice() );
            Quittung myQuittung = new Quittung(this.conn, this.mainWindow,
                    DateTime.now(TimeZone.getDefault()), articleNames, stueckzahlen,
                    einzelpreise, preise, mwsts, mwstsAndTheirValues, totalPrice);
            myQuittung.printReceipt();
	    return;
	}
	if (e.getSource() == neuerKundeButton){
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
            BigDecimal rabattAnteil = (new BigDecimal(
                        rabattButtons.get(rabattIndex).getText().replace("%","").replace(',','.').replaceAll(" ","")
                        )
                    ).multiply(percent);
            if (barButton.isEnabled()){
                rechnungRabattierenRelativ(rabattAnteil);
            }
            else {
                artikelRabattierenRelativ(rabattAnteil);
            }
            return;
        }
	if (e.getSource() == individuellRabattRelativButton){
            BigDecimal rabattAnteil = (new BigDecimal( individuellRabattRelativField.getText().replace(',','.') )).multiply(percent);
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
            BigDecimal rabatt = new BigDecimal(individuellRabattAbsolutField.getText().replace(',','.'));
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
            rabattIDs.remove(removeRow);
            einzelpreise.remove(removeRow);
            preise.remove(removeRow);
            colors.remove(removeRow);
            types.remove(removeRow);
            mwsts.remove(removeRow);
            stueckzahlen.remove(removeRow);
            removeButtons.remove(removeRow);

            positions.remove(removeRow);

            // remove extra rows (Rabatt oder Pfand):
            while ( removeRow < removeButtons.size() && removeButtons.get(removeRow) == null ){
                data.remove(removeRow);
                artikelIDs.remove(removeRow);
                articleNames.remove(removeRow);
                rabattIDs.remove(removeRow);
                einzelpreise.remove(removeRow);
                preise.remove(removeRow);
                colors.remove(removeRow);
                types.remove(removeRow);
                mwsts.remove(removeRow);
                stueckzahlen.remove(removeRow);
                removeButtons.remove(removeRow);

                positions.remove(removeRow);
            }

            for (int i=removeRow; i<positions.size(); i++){
                Integer oldVal = positions.get(i);
                if (oldVal != null){
                    positions.set(i, oldVal-1);
                }
            }

            removeRabattAufRechnung();

            refreshPositionsInData();
            updateAll();
            return;
        }
    }

}
