package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.text.SimpleDateFormat;
import java.text.ParseException;

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
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.text.*; // for AbstractDocument, JTextComponent
import javax.swing.event.*;
//import java.beans.PropertyChangeEvent;
//import java.beans.PropertyChangeListener;

// JCalendarButton
import jcalendarbutton.org.JCalendarButton;

import WeltladenDB.MainWindowGrundlage;
import WeltladenDB.DialogWindow;
import WeltladenDB.BarcodeComboBox;
import WeltladenDB.ArtikelNameComboBox;
import WeltladenDB.ArtikelNummerComboBox;
import WeltladenDB.ProduktgruppenIndentedRenderer;
import WeltladenDB.CurrencyDocumentFilter;
import WeltladenDB.BoundsPopupMenuListener;

public class RabattDialog extends DialogWindow implements ChangeListener, DocumentListener, ItemListener {
    // Attribute:
    protected final BigDecimal percent = new BigDecimal("0.01");
    protected JPanel allPanel;
    protected Rabattaktionen rabattaktionen;
    protected SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    protected final String[] produktDropDownOptions = {"Einzelner Artikel", "Ganze Produktgruppe"};
    protected final String[] rabattDropDownOptions = {"Einzelrabatt", "Mengenrabatt"};

    protected Vector<String> produktgruppenNamen;
    protected Vector<String> produktgruppenIDs;
    protected Vector< Vector<String> > produktgruppenIDsList;
    protected String produktModus, rabattModus;

    // Text Fields
    protected JTextField nameField;
    protected JComboBox produktDropDown;
    protected JPanel produktCards;
    //protected IncrementalSearchComboBox artikelBox;
    //protected AutoCompleteComboBox_Old nummerBox;
    protected BarcodeComboBox barcodeBox;
    protected ArtikelNameComboBox artikelBox;
    protected ArtikelNummerComboBox nummerBox;
    protected JButton emptyBarcodeButton;
    protected JButton emptyArtikelButton;
    protected JButton emptyNummerButton;
    protected JComboBox produktgruppenBox;
    protected JComboBox rabattDropDown;
    protected JPanel rabattCards;
    protected JTextField absolutField;
    protected JTextField relativField;
    protected JSpinner mengenrabattSchwelleSpinner;
    protected JTextField schwelleField;
    protected JSpinner kostenlosSpinner;
    protected JTextField kostenlosField;
    protected JTextField mengenrabattRelativField;
    //Formats to format and parse numbers
    protected JButton insertButton;
    protected JButton editButton;
    protected JLabel editStatus;

    // date stuff:
    protected JSpinner vonSpinner;
    protected JSpinner bisSpinner;
    protected SpinnerDateModel vonDateModel;
    protected SpinnerDateModel bisDateModel;
    protected JCalendarButton calButtVon;
    protected JCalendarButton calButtBis;
    protected JCheckBox unlimitedCheckBox;
    protected Date now;

    ///////////////////////////////////
    // customization: different values for insert and edit dialogs
    protected String borderTitle = "Neue Rabattaktion";
    protected boolean enableOnlyNameAndBis = false;
    protected boolean editMode = false;
    protected String presetRabattID = "";

    // preset values:
    protected String aktionsname;
    protected String von;
    protected String bis;
    protected boolean unlimited;
    protected String artikelName;
    protected String lieferant;
    protected String artikelNummer;
    protected String produktgruppenID;
    protected String rabattAbsolut;
    protected String rabattRelativ;
    protected String mengenrabattSchwelle;
    protected String mengenrabattAnzahl;
    protected String mengenrabattRelativ;
    protected String presetProduktModus;
    protected String presetRabattModus;

    // Methoden:

    /**
     *    The constructor.
     *       */
    public RabattDialog(Connection conn, MainWindowGrundlage mw, Rabattaktionen r, JDialog dia) {
	super(conn, mw, dia);
        this.rabattaktionen = r;

        fillComboBoxes();
        showAll();
    }
    public RabattDialog(Connection conn, MainWindowGrundlage mw, Rabattaktionen r, JDialog dia,
            String bt, boolean editMode, String rabattID, boolean nandb) {
	super(conn, mw, dia);
        this.rabattaktionen = r;
        this.borderTitle = bt;
        this.editMode = editMode;
        this.presetRabattID = rabattID;
        this.enableOnlyNameAndBis = nandb;

        fillComboBoxes();
        if (this.editMode) queryPresetValues();
        showAll();
    }

    // setters:
    public void setBorderTitle(String bt) { this.borderTitle = bt; updateAll(); }
    public void setEnableOnlyNameAndBis(boolean bool) { this.enableOnlyNameAndBis = bool; updateAll(); }
    public void setEditMode(boolean bool) { this.editMode = bool; updateAll(); }
    public void setPresetRabattID(String rabattID) { this.presetRabattID = rabattID; updateAll(); }

    void fillComboBoxes() {
        produktgruppenNamen = new Vector<String>();
        produktgruppenIDs = new Vector<String>();
        produktgruppenIDsList = new Vector< Vector<String> >();
        produktgruppenNamen.add("");
        produktgruppenIDs.add("");
        Vector<String> nullIDs = new Vector<String>();
        nullIDs.add(null); nullIDs.add(null); nullIDs.add(null);
        produktgruppenIDsList.add(nullIDs);
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT produktgruppen_id, toplevel_id, sub_id, subsub_id, produktgruppen_name FROM produktgruppe WHERE mwst_id IS NOT NULL AND toplevel_id IS NOT NULL ORDER BY toplevel_id, sub_id, subsub_id"
                    );
            while (rs.next()) {
                String id = rs.getString(1);
                Vector<String> ids = new Vector<String>();
                ids.add(rs.getString(2));
                ids.add(rs.getString(3));
                ids.add(rs.getString(4));
                String name = rs.getString(5);

                produktgruppenNamen.add(name);
                produktgruppenIDs.add(id);
                produktgruppenIDsList.add(ids);
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void queryPresetValues() {
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT r.aktionsname, r.von, r.bis, a.artikel_name, l.lieferant_name, a.artikel_nr, "+
                    "r.produktgruppen_id, r.rabatt_absolut, r.rabatt_relativ, "+
                    "r.mengenrabatt_schwelle, r.mengenrabatt_anzahl_kostenlos, r.mengenrabatt_relativ "+
                    "FROM rabattaktion AS r LEFT JOIN artikel AS a USING (artikel_id) "+
                    "LEFT JOIN lieferant AS l USING (lieferant_id) "+
                    "WHERE r.rabatt_id = "+
                    this.presetRabattID
                    );
            while (rs.next()) {  
                this.aktionsname = rs.getString(1) == null ? "" : rs.getString(1);
                this.von = rs.getString(2) == null ? "" : rs.getString(2).split(" ")[0];
                this.bis = rs.getString(3) == null ? "" : rs.getString(3).split(" ")[0];
                if (this.bis.length() == 0) this.unlimited = true;
                else this.unlimited = false;
                this.artikelName = rs.getString(4) == null ? "" : rs.getString(4);
                this.lieferant = rs.getString(5) == null ? "" : rs.getString(4);
                this.artikelNummer = rs.getString(6) == null ? "" : rs.getString(5);
                this.produktgruppenID = rs.getString(7) == null ? "" : rs.getString(6);
                this.rabattAbsolut = rs.getString(8) == null ? "" : rs.getString(7);
                this.rabattRelativ = rs.getString(9) == null ? "" : rs.getString(8);
                this.mengenrabattSchwelle = rs.getString(10) == null ? "" : rs.getString(9);
                this.mengenrabattAnzahl = rs.getString(11) == null ? "" : rs.getString(10);
                this.mengenrabattRelativ = rs.getString(12) == null ? "" : rs.getString(11);
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void setPresetValues() {
        nameField.setText(this.aktionsname);
        vonDateModel.setValue(java.sql.Date.valueOf(this.von));
        if ( this.unlimited ){
            unlimitedCheckBox.setSelected(true);
        } else {
            bisDateModel.setValue(java.sql.Date.valueOf(this.bis));
        }
        if ( (!this.artikelName.equals("")) && (!this.artikelNummer.equals("")) ){ // Einzelprodukt
            produktModus = produktDropDownOptions[0];
            artikelBox.setBox(new String[]{this.artikelName, this.lieferant});
            nummerBox.setBox(new String[]{this.artikelNummer});
        } else if (!this.produktgruppenID.equals("")){ // Produktgruppe
            produktModus = produktDropDownOptions[1];
            int presetIndex = produktgruppenIDs.indexOf(this.produktgruppenID);
            produktgruppenBox.setSelectedIndex(presetIndex);
        }
        this.presetProduktModus = produktModus;
        produktDropDown.setSelectedItem(produktModus);
        if ( (!this.rabattAbsolut.equals("")) || (!this.rabattRelativ.equals("")) ){ // Einzelrabatt
            rabattModus = rabattDropDownOptions[0];
            this.presetRabattModus = rabattModus;
            if (!this.rabattAbsolut.equals("")) absolutField.setText(this.rabattAbsolut);
            if (!this.rabattRelativ.equals("")){
                relativField.setText( vatFormatter(this.rabattRelativ).replace(" %","") );
            }
        } else if (!this.mengenrabattSchwelle.equals("")){ // Mengenrabatt
            rabattModus = rabattDropDownOptions[1];
            this.presetRabattModus = rabattModus;
            mengenrabattSchwelleSpinner.setValue(Integer.parseInt(this.mengenrabattSchwelle));
            if (!this.mengenrabattAnzahl.equals("")){
                kostenlosSpinner.setValue(Integer.parseInt(this.mengenrabattAnzahl));
            }
            if (!this.mengenrabattRelativ.equals("")){
                mengenrabattRelativField.setText( vatFormatter(this.mengenrabattRelativ).replace(" %","") );
            }
        }
        rabattDropDown.setSelectedItem(rabattModus);
    }

    void cleanSetEnabled(Object obj, boolean state) {
        if ( obj instanceof JSpinner ){
            JSpinner myObj = (JSpinner)obj;
            myObj.removeChangeListener(this);
            myObj.setEnabled(state);
            myObj.addChangeListener(this);
        } else if ( obj instanceof JButton ){
            JButton myObj = (JButton)obj;
            myObj.removeChangeListener(this);
            myObj.setEnabled(state);
            myObj.addChangeListener(this);
        }
    }

    void enableOnlyNameAndBis() {
        nameField.setEnabled(true);
        cleanSetEnabled(vonSpinner, false);
        cleanSetEnabled(calButtVon, false);
        unlimitedCheckBox.setEnabled(true);
        if ( unlimitedValue() ){
            cleanSetEnabled(bisSpinner, false);
            cleanSetEnabled(calButtBis, false);
        } else {
            cleanSetEnabled(bisSpinner, true);
            cleanSetEnabled(calButtBis, true);
        }
        artikelBox.setEnabled(false);
        emptyArtikelButton.setEnabled(false);
        nummerBox.setEnabled(false);
        emptyNummerButton.setEnabled(false);
        produktgruppenBox.setEnabled(false);
        produktDropDown.setEnabled(false);
        absolutField.setEnabled(false);
        relativField.setEnabled(false);
        mengenrabattSchwelleSpinner.setEnabled(false);
        kostenlosSpinner.setEnabled(false);
        mengenrabattRelativField.setEnabled(false);
        rabattDropDown.setEnabled(false);
    }

    void showAll(){
	allPanel = new JPanel();
	allPanel.setLayout(new BoxLayout(allPanel, BoxLayout.Y_AXIS));

	//allPanel.add(Box.createRigidArea(new Dimension(0,10))); // add empty space

	JPanel rabattaktionPanel = new JPanel();
	rabattaktionPanel.setLayout(new BoxLayout(rabattaktionPanel, BoxLayout.Y_AXIS));
	rabattaktionPanel.setBorder(BorderFactory.createTitledBorder(borderTitle));
	//
        JPanel generalPanel = new JPanel();
            JPanel namePanel = new JPanel();
                namePanel.setBorder(BorderFactory.createTitledBorder("Aktionsname"));
                nameField = new JTextField("", 25);
                nameField.getDocument().addDocumentListener(this);
                DocumentSizeFilter dsf = new DocumentSizeFilter(50);
                ((AbstractDocument)nameField.getDocument()).setDocumentFilter(dsf);
                namePanel.add(nameField);
            generalPanel.add(namePanel);
            JPanel vonPanel = new JPanel();
                vonPanel.setBorder(BorderFactory.createTitledBorder("Startdatum"));
                Calendar nowCal = Calendar.getInstance();
                //Date nowDate = nowCal.getTime();
                now = new Date();
                nowCal.add(Calendar.DATE, -1); // for strange reasons, we need day-1
                Date yesterday = nowCal.getTime();
                vonDateModel = new SpinnerDateModel(now, // Startwert
                                             yesterday, // kleinster Wert
                                             null, // groesster Wert
                                             Calendar.YEAR);//ignored for user input
                vonSpinner = new JSpinner(vonDateModel);
                vonSpinner.setEditor(new JSpinner.DateEditor(vonSpinner, "dd/MM/yyyy"));
                vonSpinner.addChangeListener(this);
                calButtVon = new JCalendarButton(now);
                calButtVon.addChangeListener(this);
                vonPanel.add(vonSpinner); vonPanel.add(calButtVon);
            generalPanel.add(vonPanel);
            JPanel bisPanel = new JPanel();
                bisPanel.setBorder(BorderFactory.createTitledBorder("Enddatum"));
                bisDateModel = new SpinnerDateModel(now, // Startwert
                                             yesterday, // kleinster Wert
                                             null, // groesster Wert
                                             Calendar.YEAR);//ignored for user input
                bisSpinner = new JSpinner(bisDateModel);
                bisSpinner.setEditor(new JSpinner.DateEditor(bisSpinner, "dd/MM/yyyy"));
                bisSpinner.addChangeListener(this);
                calButtBis = new JCalendarButton(vonDateModel.getDate());
                calButtBis.addChangeListener(this);
                bisPanel.add(bisSpinner); bisPanel.add(calButtBis);
                unlimitedCheckBox = new JCheckBox("keins");
                unlimitedCheckBox.addItemListener(this);
                //unlimitedCheckBox.addActionListener(this);
                unlimitedCheckBox.setSelected(false);
                bisPanel.add(unlimitedCheckBox);
            generalPanel.add(bisPanel);
        rabattaktionPanel.add(generalPanel);
        //
	JPanel artikelPanel = new JPanel();
	    artikelPanel.add(new JLabel("Wähle:"));
            produktDropDown = new JComboBox(produktDropDownOptions);
            produktDropDown.addItemListener(this);
            artikelPanel.add(produktDropDown);
            produktModus = produktDropDownOptions[0];
                JPanel artikelCard = new JPanel();
                artikelCard.setLayout(new BoxLayout(artikelCard, BoxLayout.Y_AXIS));

                JPanel barcodePanel = new JPanel();
                barcodePanel.setBorder(BorderFactory.createTitledBorder("Barcode"));
                    String filterStr = " AND variabler_preis = FALSE AND toplevel_id IS NOT NULL ";
                    barcodeBox = new BarcodeComboBox(this.conn, filterStr);
                    barcodeBox.addActionListener(this);
                    barcodePanel.add(barcodeBox);
                    emptyBarcodeButton = new JButton("x");
                    emptyBarcodeButton.addActionListener(this);
                    barcodePanel.add(emptyBarcodeButton);
                artikelCard.add(barcodePanel);

                JPanel nameNummerPanel = new JPanel();
                    JPanel artNamePanel = new JPanel();
                    artNamePanel.setBorder(BorderFactory.createTitledBorder("Artikelname"));
                    artikelBox = new ArtikelNameComboBox(this.conn, filterStr);
                    artikelBox.addActionListener(this);
                    // set preferred width etc.:
                    artikelBox.addPopupMenuListener(new BoundsPopupMenuListener(false, true, 50, false));
                    artikelBox.setPrototypeDisplayValue("qqqqqqqqqqqqqqqqqqqq");
                    artNamePanel.add(artikelBox);
                    emptyArtikelButton = new JButton("x");
                    emptyArtikelButton.addActionListener(this);
                    artNamePanel.add(emptyArtikelButton);
                    nameNummerPanel.add(artNamePanel);

                    JPanel artNummerPanel = new JPanel();
                    artNummerPanel.setBorder(BorderFactory.createTitledBorder("Artikelnummer"));
                    nummerBox = new ArtikelNummerComboBox(this.conn, filterStr);
                    nummerBox.addActionListener(this);
                    // set preferred width etc.:
                    nummerBox.addPopupMenuListener(new BoundsPopupMenuListener(false, true, 30, false));
                    nummerBox.setPrototypeDisplayValue("qqqqqqq");
                    artNummerPanel.add(nummerBox);
                    emptyNummerButton = new JButton("x");
                    emptyNummerButton.addActionListener(this);
                    artNummerPanel.add(emptyNummerButton);
                    nameNummerPanel.add(artNummerPanel);
                artikelCard.add(nameNummerPanel);

                JPanel produktgruppenCard = new JPanel();
                    JPanel produktgruppenPanel = new JPanel();// produktgruppenPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
                    produktgruppenPanel.setBorder(BorderFactory.createTitledBorder("Produktgruppe"));
                    produktgruppenBox = new JComboBox(produktgruppenNamen);
                    produktgruppenBox.setRenderer(new ProduktgruppenIndentedRenderer(produktgruppenIDsList));
                    ( (JTextComponent)produktgruppenBox.getEditor().getEditorComponent() ).getDocument().addDocumentListener(this);
                    //produktgruppenBox.addKeyListener(enterAdapter);
                    produktgruppenBox.addItemListener(this);
                    produktgruppenPanel.add(produktgruppenBox);
                produktgruppenCard.add(produktgruppenPanel);

            produktCards = new JPanel(new CardLayout());
            produktCards.add(artikelCard, produktDropDownOptions[0]);
            produktCards.add(produktgruppenCard, produktDropDownOptions[1]);
            artikelPanel.add(produktCards);
	rabattaktionPanel.add(artikelPanel);
        //
	JPanel wertPanel = new JPanel();
	    wertPanel.add(new JLabel("Wähle:"));
            rabattDropDown = new JComboBox(rabattDropDownOptions);
            rabattDropDown.addItemListener(this);
            wertPanel.add(rabattDropDown);
            rabattModus = rabattDropDownOptions[0];
                JPanel einzelCard = new JPanel();
                    einzelCard.add(new JLabel("ENTWEDER:"));
                    JPanel absolutPanel = new JPanel();
                    absolutPanel.setBorder(BorderFactory.createTitledBorder("Absolutbetrag"));
                    absolutField = new JTextField();
                    absolutField.setColumns(10);
                    absolutField.setHorizontalAlignment(JTextField.RIGHT);
                    absolutField.getDocument().addDocumentListener(this);
                    CurrencyDocumentFilter cdf = new CurrencyDocumentFilter();
                    ((AbstractDocument)absolutField.getDocument()).setDocumentFilter(cdf);
                    absolutPanel.add(absolutField);
                    absolutPanel.add(new JLabel(currencySymbol));
                    einzelCard.add(absolutPanel);
                    //
                    einzelCard.add(Box.createRigidArea(new Dimension(12,0)));
                    //
                    einzelCard.add(new JLabel("ODER:"));
                    JPanel relativPanel = new JPanel();
                    relativPanel.setBorder(BorderFactory.createTitledBorder("Relativbetrag"));
                    relativField = new JTextField();
                    relativField.setColumns(10);
                    relativField.setHorizontalAlignment(JTextField.RIGHT);
                    relativField.getDocument().addDocumentListener(this);
                    FloatDocumentFilter fdf = new FloatDocumentFilter();
                    ((AbstractDocument)relativField.getDocument()).setDocumentFilter(fdf);
                    relativPanel.add(relativField);
                    relativPanel.add(new JLabel("%"));
                    einzelCard.add(relativPanel);

                JPanel mengenCard = new JPanel();
                    JPanel schwellePanel = new JPanel();
                    schwellePanel.setBorder(BorderFactory.createTitledBorder("Schwelle"));
                    SpinnerNumberModel schwelleModel = new SpinnerNumberModel(2, // initial value
                                                                            2, // min
                                                                            null, // max (null == no max)
                                                                            1); // step
                    mengenrabattSchwelleSpinner = new JSpinner(schwelleModel);
                    JSpinner.NumberEditor schwelleEditor = new JSpinner.NumberEditor(mengenrabattSchwelleSpinner, "###");
                    mengenrabattSchwelleSpinner.setEditor(schwelleEditor);
                    schwelleField = schwelleEditor.getTextField();
                    schwelleField.getDocument().addDocumentListener(this);
                    schwelleField.setColumns(3);
                    schwellePanel.add(mengenrabattSchwelleSpinner);
                    schwellePanel.add(new JLabel("Artikel"));
                    mengenCard.add(schwellePanel);

                    mengenCard.add(new JLabel("ENTWEDER:"));
                    JPanel kostenlosPanel = new JPanel();
                    kostenlosPanel.setBorder(BorderFactory.createTitledBorder("kostenlose Artikel"));
                    SpinnerNumberModel kostenlosModel = new SpinnerNumberModel(0, // initial value
                                                                            0, // min
                                                                            null, // max (null == no max)
                                                                            1); // step
                    kostenlosSpinner = new JSpinner(kostenlosModel);
                    JSpinner.NumberEditor kostenlosEditor = new JSpinner.NumberEditor(kostenlosSpinner, "###");
                    kostenlosSpinner.setEditor(kostenlosEditor);
                    kostenlosField = kostenlosEditor.getTextField();
                    kostenlosField.getDocument().addDocumentListener(this);
                    kostenlosField.setColumns(3);
                    kostenlosPanel.add(kostenlosSpinner);
                    kostenlosPanel.add(new JLabel("Artikel"));
                    mengenCard.add(kostenlosPanel);
                    //
                    mengenCard.add(Box.createRigidArea(new Dimension(12,0)));
                    //
                    mengenCard.add(new JLabel("ODER:"));
                    JPanel mengenrabattRelativPanel = new JPanel();
                    mengenrabattRelativPanel.setBorder(BorderFactory.createTitledBorder("Relativbetrag"));
                    mengenrabattRelativField = new JTextField();
                    mengenrabattRelativField.setColumns(10);
                    mengenrabattRelativField.setHorizontalAlignment(JTextField.RIGHT);
                    mengenrabattRelativField.getDocument().addDocumentListener(this);
                    ((AbstractDocument)mengenrabattRelativField.getDocument()).setDocumentFilter(fdf);
                    mengenrabattRelativPanel.add(mengenrabattRelativField);
                    mengenrabattRelativPanel.add(new JLabel("%"));
                    mengenCard.add(mengenrabattRelativPanel);
            rabattCards = new JPanel(new CardLayout());
            rabattCards.add(einzelCard, rabattDropDownOptions[0]);
            rabattCards.add(mengenCard, rabattDropDownOptions[1]);
            wertPanel.add(rabattCards);
	rabattaktionPanel.add(wertPanel);
	    //
        if (this.editMode){
            JPanel statusPanel = new JPanel();
            editStatus = new JLabel("Keine Änderungen");
            statusPanel.add(editStatus);
            rabattaktionPanel.add(statusPanel);
        }
	JPanel buttonPanel = new JPanel();
            if (!this.editMode){
                insertButton = new JButton("Abschicken");
                insertButton.setEnabled(false);
                insertButton.addActionListener(this);
                buttonPanel.add(insertButton);
            } else {
                editButton = new JButton("Änderungen speichern");
                editButton.setEnabled(false);
                editButton.addActionListener(this);
                buttonPanel.add(editButton);
            }
	    closeButton = new JButton("Schließen");
            closeButton.setMnemonic(KeyEvent.VK_S);
	    closeButton.addActionListener(this);
	    buttonPanel.add(closeButton);
	rabattaktionPanel.add(buttonPanel);
	//
	allPanel.add(rabattaktionPanel);

        if (this.editMode) setPresetValues();
        if (this.enableOnlyNameAndBis) enableOnlyNameAndBis();

	this.add(allPanel, BorderLayout.CENTER);
    }

    protected void updateAll(){
	this.remove(allPanel);
	this.revalidate();
        if (this.editMode) queryPresetValues();
	showAll();
    }

    protected void retrieveValuesFromForm(StringBuffer aktname, StringBuffer von, StringBuffer bis, 
            StringBuffer artName, StringBuffer lieferant, StringBuffer artNummer, 
            StringBuffer artikelID, StringBuffer prodGrID, 
            StringBuffer absolutValue, StringBuffer relativValue, StringBuffer schwelle, StringBuffer mengeKostenlos, StringBuffer mengeRel) {
        aktname.replace(0, aktname.length(), nameValue());
        String vonDate = vonValue();
        String bisDate = bisValue();
        String today = this.sdf.format(now);
        if ( vonDate.equals(today) ) 
            von.replace(0, von.length(), "NOW()");
        else 
            von.replace(0, von.length(), "\'" + vonDate + " 00:00:00\'");
        if ( unlimitedValue() ) 
            bis.replace(0, bis.length(), "NULL");
        else 
            bis.replace(0, bis.length(), "\'" + bisDate + " 23:59:59\'");
        if (rabattModus == rabattDropDownOptions[0]){ // Einzelrabatt
            absolutValue.replace(0, absolutValue.length(), absolutValue());
            relativValue.replace(0, relativValue.length(), relativValue());
        }
        if (rabattModus == rabattDropDownOptions[1]){ // Mengenrabatt
            schwelle.replace(0, schwelle.length(), schwelleValue());
            mengeKostenlos.replace(0, mengeKostenlos.length(), kostenlosValue());
            mengeRel.replace(0, mengeRel.length(), mengeRelativValue());
        }
        if (produktModus == produktDropDownOptions[0]){ // Einzelprodukt
            artName.replace(0, artName.length(), artNameValue()[0]);
            lieferant.replace(0, lieferant.length(), artNameValue()[1]);
            artNummer.replace(0, artNummer.length(), artNummerValue());
            artikelID.replace(0, artikelID.length(), Integer.toString( this.rabattaktionen.getArticleID(artName.toString(), lieferant.toString(), artNummer.toString()) )); // get the internal artikelID from the DB
        }
        if (produktModus == produktDropDownOptions[1]){ // Produktgruppe
            prodGrID.replace(0, prodGrID.length(), produktgruppenValue());
        }
        artikelID.replace(0, artikelID.length(), artikelID.toString().equals("") ? "NULL" : artikelID.toString());
        prodGrID.replace(0, prodGrID.length(), prodGrID.toString().equals("") ? "NULL" : prodGrID.toString());
        absolutValue.replace(0, absolutValue.length(), absolutValue.toString().equals("") ? "NULL" : absolutValue.toString());
        relativValue.replace(0, relativValue.length(), relativValue.toString().equals("") ? "NULL" : relativValue.toString());
        schwelle.replace(0, schwelle.length(), schwelle.toString().equals("") ? "NULL" : schwelle.toString());
        mengeKostenlos.replace(0, mengeKostenlos.length(), mengeKostenlos.toString().equals("") ? "NULL" : mengeKostenlos.toString());
        mengeKostenlos.replace(0, mengeKostenlos.length(), mengeKostenlos.toString().equals("0") ? "NULL" : mengeKostenlos.toString());
        mengeRel.replace(0, mengeRel.length(), mengeRel.toString().equals("") ? "NULL" : mengeRel.toString());
    }

    protected void insertRabattaktion() {
        StringBuffer aktname = new StringBuffer(""), von = new StringBuffer(""), bis = new StringBuffer(""), 
                     artName = new StringBuffer(""), lieferant = new StringBuffer(""), 
                     artNummer = new StringBuffer(""), artikelID = new StringBuffer(""), prodGrID = new StringBuffer(""), 
                     absolutValue = new StringBuffer(""), relativValue = new StringBuffer(""), 
                     schwelle = new StringBuffer(""), mengeKostenlos = new StringBuffer(""), mengeRel = new StringBuffer("");
        retrieveValuesFromForm(aktname, von, bis, artName, lieferant, artNummer, artikelID, prodGrID, absolutValue, relativValue, schwelle, mengeKostenlos, mengeRel);
        System.out.println(
                "aktname: " + aktname + "\n" +
                "absolutValue: " + absolutValue + "\n" +
                "relativValue: " + relativValue + "\n" +
                "schwelle: " + schwelle + "\n" +
                "mengeKostenlos: " + mengeKostenlos + "\n" +
                "mengeRel: " + mengeRel + "\n" +
                "von: " + von + "\n" +
                "bis: " + bis + "\n" +
                "prodGrID: " + prodGrID + "\n" +
                "artikelID: " + artikelID);
        int answer = JOptionPane.showConfirmDialog(this,
                "Rabattaktion \""+aktname+"\" in Datenbank eingeben?",
                "Rabattaktion speichern",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.YES_OPTION){
            try {
                // Create statement for MySQL database
                Statement stmt = this.conn.createStatement();
                // Run MySQL command
                int result = stmt.executeUpdate(
                        "INSERT INTO rabattaktion SET aktionsname = \'"+aktname+"\', rabatt_relativ = "+relativValue+", rabatt_absolut = "+absolutValue+", "+
                        "mengenrabatt_schwelle = "+schwelle+", mengenrabatt_anzahl_kostenlos = "+mengeKostenlos+", mengenrabatt_relativ = "+mengeRel+", " +
                        "von = "+von+", bis = "+bis+", " +
                        "produktgruppen_id = "+prodGrID+", artikel_id = "+artikelID
                        );
                if (result != 0){
                    // update everything
                    JOptionPane.showMessageDialog(this, "Rabattaktion wurde in Datenbank eingetragen!",
                            "Info", JOptionPane.INFORMATION_MESSAGE);
                    this.rabattaktionen.initiateSpinners();
                    this.rabattaktionen.updateAll();
                    updateAll();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Rabattaktion konnte nicht in Datenbank eingetragen werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
                stmt.close();
            } catch (SQLException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else { // NO_OPTION
            JOptionPane.showMessageDialog(this, "Datenbank unverändert!",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    protected void updateRabattaktion() {
        StringBuffer aktname = new StringBuffer(""), von = new StringBuffer(""), bis = new StringBuffer(""), 
                     artName = new StringBuffer(""), lieferant = new StringBuffer(""), 
                     artNummer = new StringBuffer(""), artikelID = new StringBuffer(""), prodGrID = new StringBuffer(""), 
                     absolutValue = new StringBuffer(""), relativValue = new StringBuffer(""), 
                     schwelle = new StringBuffer(""), mengeKostenlos = new StringBuffer(""), mengeRel = new StringBuffer("");
        retrieveValuesFromForm(aktname, von, bis, artName, lieferant, artNummer, artikelID, prodGrID, absolutValue, relativValue, schwelle, mengeKostenlos, mengeRel);
        System.out.println(
                "aktname: " + aktname + "\n" +
                "absolutValue: " + absolutValue + "\n" +
                "relativValue: " + relativValue + "\n" +
                "schwelle: " + schwelle + "\n" +
                "mengeKostenlos: " + mengeKostenlos + "\n" +
                "mengeRel: " + mengeRel + "\n" +
                "von: " + von + "\n" +
                "bis: " + bis + "\n" +
                "prodGrID: " + prodGrID + "\n" +
                "artikelID: " + artikelID);
        int answer = JOptionPane.showConfirmDialog(this,
                "Rabattaktion \""+this.aktionsname+"\" wirklich verändern?",
                "Rabattaktion ändern",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.YES_OPTION){
            try {
                // Create statement for MySQL database
                Statement stmt = this.conn.createStatement();
                // Run MySQL command
                int result = stmt.executeUpdate(
                        "UPDATE rabattaktion SET aktionsname = \'"+aktname+"\', rabatt_relativ = "+relativValue+", rabatt_absolut = "+absolutValue+", "+
                        "mengenrabatt_schwelle = "+schwelle+", mengenrabatt_anzahl_kostenlos = "+mengeKostenlos+", mengenrabatt_relativ = "+mengeRel+", " +
                        "von = "+von+", bis = "+bis+", " +
                        "produktgruppen_id = "+prodGrID+", artikel_id = "+artikelID+" WHERE rabatt_id = "+this.presetRabattID
                        );
                if (result != 0){
                    // update everything
                    JOptionPane.showMessageDialog(this, "Änderungen wurden gespeichert!",
                            "Info", JOptionPane.INFORMATION_MESSAGE);
                    this.rabattaktionen.initiateSpinners();
                    this.rabattaktionen.updateAll();
                    this.window.dispose();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Änderungen konnten nicht gespeichert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
                stmt.close();
            } catch (SQLException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else { // NO_OPTION
            JOptionPane.showMessageDialog(this, "Datenbank unverändert!",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    protected void updateBisDate() {
        if ( bisDateModel.getDate().before(vonDateModel.getDate()) ){
            bisDateModel.setValue(vonDateModel.getDate()); // JCalendarButton updated automatically
        }
    }

    /**
     *    * Each non abstract class that implements the ChangeListener
     *      must have this method.
     *
     *    @param e the change event.
     **/
    public void stateChanged(ChangeEvent e) {
	if (e.getSource() == vonSpinner){
            calButtVon.removeChangeListener(this);
            setCalButtFromSpinner(vonDateModel, calButtVon);
            calButtVon.addChangeListener(this);
            updateBisDate();
	}
        else if (e.getSource() == bisSpinner){
            calButtBis.removeChangeListener(this);
            setCalButtFromSpinner(bisDateModel, calButtBis);
            calButtBis.addChangeListener(this);
	}
	else if (e.getSource() == calButtVon){
            vonSpinner.removeChangeListener(this);
            setSpinnerFromCalButt(vonDateModel, calButtVon, now, null);
            vonSpinner.addChangeListener(this);
	}
	else if (e.getSource() == calButtBis){
            bisSpinner.removeChangeListener(this);
            setSpinnerFromCalButt(bisDateModel, calButtBis, vonDateModel.getDate(), null);
            bisSpinner.addChangeListener(this);
	}
        if (this.editMode){
            checkIfChangesCanBeSaved();
        }
    }


    void setOtherFieldEditable(DocumentEvent e) {
	JTextField thisFieldPointer = null;
	JTextField otherFieldPointer = null;
	if ( e.getDocument() == absolutField.getDocument() ){
	    thisFieldPointer = absolutField;
	    otherFieldPointer = relativField;
	}
	else if ( e.getDocument() == relativField.getDocument() ){
	    thisFieldPointer = relativField;
	    otherFieldPointer = absolutField;
	}
	else if ( e.getDocument() == kostenlosField.getDocument() ){
            mengenrabattRelativField.setEditable( (Integer)kostenlosSpinner.getValue() <= 0 );
            return;
	}
	else if ( e.getDocument() == mengenrabattRelativField.getDocument() ){
            kostenlosSpinner.setEnabled( mengenrabattRelativField.getText().length() <= 0 );
            return;
	}
	else {
	    return;
	}
        otherFieldPointer.setEditable( thisFieldPointer.getText().length() <= 0 );
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
                ////if (artikelNamen.size() == 1){ 
                ////    // update internal cache string before changing name in text field (otherwise document listener causes problems)
                ////    artikelNameText = artikelNamen.get(0)[0];
                ////}
                artikelBox.setItems(artikelNamen);
            //artikelBox.addActionListener(this);
        }
        if (nummerBox.getItemCount() != 1){
            //nummerBox.removeActionListener(this);
                ////if (artikelNummern.size() == 1){ 
                ////    // update internal cache string before changing name in text field (otherwise document listener causes problems)
                ////    artikelNummerText = artikelNummern.get(0)[0];
                ////}
                nummerBox.setItems(artikelNummern);
            //nummerBox.addActionListener(this);
        }
        if ( artikelBox.getItemCount() > 1 ){ artikelBox.requestFocus(); artikelBox.showPopup(); }
        else { artikelBox.hidePopup(); }
        //if ( nummerBox.getItemCount() > 1 ){ nummerBox.showPopup(); }
        //else { nummerBox.hidePopup(); }
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
                ////if (artikelNummern.size() == 1){ 
                ////    // update internal cache string before changing name in text field (otherwise document listener causes problems)
                ////    artikelNummerText = artikelNummern.get(0)[0];
                ////}
                nummerBox.setItems(artikelNummern);
            //nummerBox.addActionListener(this);
        }
        if ( nummerBox.getItemCount() > 1 ){ nummerBox.requestFocus(); nummerBox.showPopup(); }
        else { nummerBox.hidePopup(); }
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
                ////if (artikelNamen.size() == 1){ 
                ////    // update internal cache string before changing name in text field (otherwise document listener causes problems)
                ////    artikelNameText = artikelNamen.get(0)[0];
                ////}
                artikelBox.setItems(artikelNamen);
            //artikelBox.addActionListener(this);
        }
        if ( artikelBox.getItemCount() > 1 ){ artikelBox.requestFocus(); artikelBox.showPopup(); }
        else { artikelBox.hidePopup(); }
    }


    private void resetFormFromBarcodeBox() {
        artikelBox.emptyBox();
        nummerBox.emptyBox();
    }
    private void resetFormFromArtikelBox() {
        barcodeBox.emptyBox();
        nummerBox.emptyBox();
    }
    private void resetFormFromNummerBox() {
        barcodeBox.emptyBox();
        artikelBox.emptyBox();
    }

    private void checkBarcodeBox(ActionEvent e) {
        if ( e.getActionCommand().equals("comboBoxEdited") || // if enter was pressed
                ( e.getActionCommand().equals("comboBoxChanged") && e.getModifiers() == 16 ) // if mouse button was clicked
           ){
            if ( barcodeBox.getItemCount() == 1 ){ // if selection is correct and unique
                setArtikelNameAndNummerForBarcode();
            } else {
                resetFormFromBarcodeBox();
            }
        } else if ( e.getActionCommand().equals("comboBoxChanged") ){ // all other changes (either removal or up/down key etc.)
            resetFormFromBarcodeBox();
        }
        if (!this.editMode){
            checkIfFormIsComplete();
        } else {
            checkIfChangesCanBeSaved();
        }
    }
    protected void checkArtikelBox(ActionEvent e) {
        if ( e.getActionCommand().equals("comboBoxEdited") || // if enter was pressed
                ( e.getActionCommand().equals("comboBoxChanged") && e.getModifiers() == 16 ) // if mouse button was clicked
           ){
            if ( artikelBox.getItemCount() == 1 ){ // if selection is correct and unique
                setArtikelNummerForName();
            } else {
                resetFormFromArtikelBox();
            }
        } else if ( e.getActionCommand().equals("comboBoxChanged") ){ // all other changes (either removal or up/down key etc.)
            resetFormFromArtikelBox();
        }
        if (!this.editMode){
            checkIfFormIsComplete();
        } else {
            checkIfChangesCanBeSaved();
        }
    }
    protected void checkNummerBox(ActionEvent e) {
        if ( e.getActionCommand().equals("comboBoxEdited") || // if enter was pressed
                ( e.getActionCommand().equals("comboBoxChanged") && e.getModifiers() == 16 ) // if mouse button was clicked
           ){
            if ( nummerBox.getItemCount() == 1 ){ // if selection is correct and unique
                setArtikelNameForNummer();
            } else {
                resetFormFromNummerBox();
            }
        } else if ( e.getActionCommand().equals("comboBoxChanged") ){ // all other changes (either removal or up/down key etc.)
            resetFormFromNummerBox();
        }
        if (!this.editMode){
            checkIfFormIsComplete();
        } else {
            checkIfChangesCanBeSaved();
        }
    }



    boolean checkProductSelection() {
        boolean conditionsFulfilled = true;
        if ( produktModus == produktDropDownOptions[0] ){ // Einzelprodukt
            int nummerNumber = nummerBox.getItemCount();
            int artikelNumber = artikelBox.getItemCount();
            if ( ! (artikelNumber == 1 && nummerNumber == 1) ){
                conditionsFulfilled = false;
            }
        } else if ( produktModus == produktDropDownOptions[1] ){ // Produktgruppe
            if ( produktgruppenBox.getSelectedItem().equals("") ){
                conditionsFulfilled = false;
            }
        }
        return conditionsFulfilled;
    }

    boolean checkRabattSelection() {
        boolean conditionsFulfilled = true;
        if ( rabattModus == rabattDropDownOptions[0] ){ // Einzelrabatt
            if ( (absolutField.getText().length() == 0 && relativField.getText().length() == 0) ||
                    (absolutField.getText().length() > 0 && relativField.getText().length() > 0) ){
                conditionsFulfilled = false;
            }
        } else if ( rabattModus == rabattDropDownOptions[1] ){ // Mengenrabatt
            if ( ((Integer)kostenlosSpinner.getValue() <= 0 && mengenrabattRelativField.getText().length() == 0) ||
                    ((Integer)kostenlosSpinner.getValue() > 0 && mengenrabattRelativField.getText().length() > 0) ){
                conditionsFulfilled = false;
            }
        }
        return conditionsFulfilled;
    }

    boolean isFormComplete() {
        boolean conditionsFulfilled = true; // start with conditions fulfilled, set to false if *any* condition is not fulfilled
        // check aktionsname
        if ( nameField.getText().length() == 0) conditionsFulfilled = false;
        // check product selection
        if (conditionsFulfilled) conditionsFulfilled = checkProductSelection();
        // check rabatt selection
        if (conditionsFulfilled) conditionsFulfilled = checkRabattSelection();
        // are conditions fulfilled?
        return conditionsFulfilled;
    }

    String nameValue() { return nameField.getText(); }
    String vonValue() { return this.sdf.format(vonDateModel.getDate()); }
        // alternative:
        //String vonDate = (new java.sql.Date( vonDateModel.getDate().getTime() )).toString();
    boolean unlimitedValue() { return unlimitedCheckBox.isSelected(); }
    String bisValue() { 
        if ( unlimitedValue() )
            return "NULL";
        else
            return this.sdf.format(bisDateModel.getDate());
    }
    String[] artNameValue() { return artikelBox.parseArtikelName(); }
    String artNummerValue() { return (String)nummerBox.getSelectedItem(); }
    String produktgruppenValue() { return produktgruppenIDs.get(produktgruppenBox.getSelectedIndex()); }
    String absolutValue() { 
        if (absolutField.getText().equals("")) return "";
        return priceFormatterIntern( new BigDecimal(absolutField.getText().replace(',','.')) );
    }
    String relativValue() { 
        if (relativField.getText().equals("")) return "";
        return vatFormat.format( new BigDecimal(relativField.getText().replace(',','.')).multiply(percent) );
    }
    String schwelleValue() { return schwelleField.getText(); }
    String kostenlosValue() { return kostenlosField.getText(); }
    String mengeRelativValue() { 
        if (mengenrabattRelativField.getText().equals("")) return "";
        return vatFormat.format( new BigDecimal(mengenrabattRelativField.getText().replace(',','.')).multiply(percent) ); 
    }

    boolean areThereChanges() {
        boolean changes = false;
        if (!changes) changes = !nameValue().equals(this.aktionsname);
        if (!changes) changes = !vonValue().equals(this.von);
        if ( this.unlimited ){
            if (!changes) changes = unlimitedValue() != this.unlimited;
        } else {
            if (!changes) changes = !bisValue().equals(this.bis);
        }
        if (!changes) changes = !produktModus.equals(this.presetProduktModus);
        if (!changes) changes = !rabattModus.equals(this.presetRabattModus);
        if (!changes && produktModus == produktDropDownOptions[0]) 
            changes = !artNameValue()[0].equals(this.artikelName);
        if (!changes && produktModus == produktDropDownOptions[0]) 
            changes = !artNummerValue().equals(this.artikelNummer);
        if (!changes && produktModus == produktDropDownOptions[1]) 
            changes = !produktgruppenValue().equals(this.produktgruppenID);
        if (!changes && rabattModus == rabattDropDownOptions[0]) 
            changes = !absolutValue().equals(this.rabattAbsolut);
        if (!changes && rabattModus == rabattDropDownOptions[0]) 
            changes = !relativValue().equals(this.rabattRelativ);
        if (!changes && rabattModus == rabattDropDownOptions[1]) 
            changes = !schwelleValue().equals(this.mengenrabattSchwelle);
        if (!changes && rabattModus == rabattDropDownOptions[1]) 
            changes = !kostenlosValue().equals(this.mengenrabattAnzahl);
        if (!changes && rabattModus == rabattDropDownOptions[1]) 
            changes = !mengeRelativValue().equals(this.mengenrabattRelativ);
        return changes;
    }

    void checkIfFormIsComplete() {
        // are conditions fulfilled?
        insertButton.setEnabled(isFormComplete());
    }

    void checkIfChangesCanBeSaved() {
        // are conditions fulfilled?
        if (!areThereChanges()) editStatus.setText("Keine Änderungen");
        else if (areThereChanges() && !isFormComplete()) editStatus.setText("Änderungen, aber unvollständig");
        else if (areThereChanges() && isFormComplete()) editStatus.setText("Änderungen vorgenommen");
        editButton.setEnabled(areThereChanges() && isFormComplete());
    }

    /**
     *    * Each non abstract class that implements the DocumentListener
     *      must have these methods.
     *
     *    @param e the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
	setOtherFieldEditable(e);
        if (!this.editMode){
            checkIfFormIsComplete();
        } else {
            checkIfChangesCanBeSaved();
        }
    }
    public void removeUpdate(DocumentEvent e) {
	setOtherFieldEditable(e);
        if (!this.editMode){
            checkIfFormIsComplete();
        } else {
            checkIfChangesCanBeSaved();
        }
    }
    public void changedUpdate(DocumentEvent e) {
	//Plain text components do not fire these events
    }




    /** Needed for ItemListener. */
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
        if (source == unlimitedCheckBox) {
            //Now that we know which button was pushed, find out
            //whether it was selected or deselected.
            if (e.getStateChange() == ItemEvent.SELECTED) {
                cleanSetEnabled(bisSpinner, false);
                cleanSetEnabled(calButtBis, false);
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                cleanSetEnabled(bisSpinner, true);
                cleanSetEnabled(calButtBis, true);
            }
        } else if (source == produktDropDown) {
            CardLayout cl = (CardLayout)(produktCards.getLayout());
            produktModus = (String)e.getItem();
            cl.show(produktCards, produktModus);
        } else if (source == rabattDropDown) {
            CardLayout cl = (CardLayout)(rabattCards.getLayout());
            rabattModus = (String)e.getItem();
            cl.show(rabattCards, rabattModus);
        }
        if (!this.editMode){
            checkIfFormIsComplete();
        } else {
            checkIfChangesCanBeSaved();
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
            checkBarcodeBox(e);
            return;
        }
        if (e.getSource() == artikelBox){
            checkArtikelBox(e);
            return;
        }
        if (e.getSource() == nummerBox){
            checkNummerBox(e);
            return;
        }
	if (e.getSource() == closeButton){
            WindowAdapter adapter = (WindowAdapter)this.window.getWindowListeners()[0];
            adapter.windowClosing(new WindowEvent((Window)this.window, WindowEvent.WINDOW_CLOSING));
	    return;
        }
        if (e.getSource() == emptyBarcodeButton){
            barcodeBox.emptyBox();
            barcodeBox.requestFocus();
	    return;
	}
	if (e.getSource() == emptyArtikelButton){
            artikelBox.emptyBox();
            artikelBox.requestFocus();
	    return;
        }
        if (e.getSource() == emptyNummerButton){
            nummerBox.emptyBox();
            nummerBox.requestFocus();
	    return;
        }
        if (e.getSource() == insertButton){
            insertRabattaktion();
            return;
        }
        if (e.getSource() == editButton){
            updateRabattaktion();
            return;
        }
        super.actionPerformed(e);
    }
}
