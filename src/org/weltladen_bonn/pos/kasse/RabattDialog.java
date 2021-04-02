package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.util.Date;
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.text.SimpleDateFormat;
import java.text.ParseException;

// MySQL Connector/J stuff:
import java.sql.*;
import org.mariadb.jdbc.MariaDbPoolDataSource;

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
import javax.swing.text.*; // for AbstractDocument, JTextComponent
import javax.swing.event.*;
//import java.beans.PropertyChangeEvent;
//import java.beans.PropertyChangeListener;

// JCalendarButton
import org.weltladen_bonn.pos.jcalendarbutton.JCalendarButton;

import org.weltladen_bonn.pos.*;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RabattDialog extends DialogWindow implements ChangeListener, DocumentListener, ItemListener, ArticleSelectUser {
    // Attribute:
    private static final Logger logger = LogManager.getLogger(RabattDialog.class);

    protected final BigDecimal percent = new BigDecimal("0.01");
    protected Rabattaktionen rabattaktionen;
    protected OptionTabbedPane tabbedPane;
    protected SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    protected final String[] produktDropDownOptions = {"Einzelner Artikel", "Ganze Produktgruppe"};
    protected final String[] rabattDropDownOptions = {"Einzelrabatt", "Mengenrabatt"};

    protected Vector<String> produktgruppenNamen;
    protected Vector<Integer> produktgruppenIDs;
    protected Vector< Vector<Integer> > produktgruppenIDsList;
    protected String produktModus, rabattModus;

    // Text Fields
    protected JTextField nameField;
    protected JComboBox<String> produktDropDown;
    protected JPanel produktCards;
    protected ArticleSelectPanelRabattDialog artikelCard;
    protected JComboBox<String> produktgruppenBox;
    protected JComboBox<String> rabattDropDown;
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
    protected int presetRabattID = -1;

    // preset values:
    protected String aktionsname;
    protected String von;
    protected String bis;
    protected boolean unlimited;
    protected String artikelName;
    protected String liefID;
    protected String artikelNummer;
    protected Integer produktgruppenID;
    protected BigDecimal rabattAbsolut;
    protected BigDecimal rabattRelativ;
    protected Integer mengenrabattSchwelle;
    protected Integer mengenrabattAnzahl;
    protected BigDecimal mengenrabattRelativ;
    protected String presetProduktModus;
    protected String presetRabattModus;

    // Methoden:

    /**
     *    The constructor.
     *       */
    public RabattDialog(MariaDbPoolDataSource pool, MainWindowGrundlage mw, Rabattaktionen r, JDialog dia, OptionTabbedPane tabbedPane) {
	    super(pool, mw, dia);
        this.rabattaktionen = r;
        this.tabbedPane = tabbedPane;

        fillComboBoxes();
        showAll();
    }
    public RabattDialog(MariaDbPoolDataSource pool, MainWindowGrundlage mw, Rabattaktionen r, JDialog dia, OptionTabbedPane tabbedPane,
            String bt, boolean editMode, int rabattID, boolean nandb) {
	    super(pool, mw, dia);
        this.rabattaktionen = r;
        this.tabbedPane = tabbedPane;
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
    public void setPresetRabattID(int rabattID) { this.presetRabattID = rabattID; updateAll(); }

    void fillComboBoxes() {
        produktgruppenNamen = new Vector<String>();
        produktgruppenIDs = new Vector<Integer>();
        produktgruppenIDsList = new Vector< Vector<Integer> >();
        produktgruppenNamen.add("");
        produktgruppenIDs.add(null);
        Vector<Integer> nullIDs = new Vector<Integer>();
        nullIDs.add(null); nullIDs.add(null); nullIDs.add(null);
        produktgruppenIDsList.add(nullIDs);
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT produktgruppen_id, toplevel_id, sub_id, subsub_id, produktgruppen_name "+
                    "FROM produktgruppe WHERE mwst_id IS NOT NULL AND toplevel_id IS NOT NULL "+
                    "ORDER BY toplevel_id, sub_id, subsub_id"
                    );
            while (rs.next()) {
                Integer id = rs.getInt(1);
                Vector<Integer> ids = new Vector<Integer>();
                ids.add(rs.getInt(2));
                ids.add(rs.getInt(3));
                ids.add(rs.getInt(4));
                String name = rs.getString(5);

                produktgruppenNamen.add(name);
                produktgruppenIDs.add(id);
                produktgruppenIDsList.add(ids);
            }
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    void queryPresetValues() {
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT r.aktionsname, r.von, r.bis, a.artikel_name, a.lieferant_id, a.artikel_nr, "+
                    "r.produktgruppen_id, r.rabatt_absolut, r.rabatt_relativ, "+
                    "r.mengenrabatt_schwelle, r.mengenrabatt_anzahl_kostenlos, r.mengenrabatt_relativ "+
                    "FROM rabattaktion AS r LEFT JOIN artikel AS a USING (artikel_id) "+
                    "WHERE r.rabatt_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, this.presetRabattID);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                this.aktionsname = rs.getString(1) == null ? "" : rs.getString(1);
                this.von = rs.getString(2) == null ? "" : rs.getString(2).split(" ")[0];
                this.bis = rs.getString(3) == null ? "" : rs.getString(3).split(" ")[0];
                if (this.bis.length() == 0) this.unlimited = true;
                else this.unlimited = false;
                this.artikelName = rs.getString(4) == null ? "" : rs.getString(4);
                this.liefID = rs.getString(5) == null ? "" : rs.getString(5);
                this.artikelNummer = rs.getString(6) == null ? "" : rs.getString(6);
                this.produktgruppenID = rs.getInt(7);
                this.rabattAbsolut = rs.getBigDecimal(8);
                this.rabattRelativ = rs.getBigDecimal(9);
                this.mengenrabattSchwelle = rs.getInt(10);
                this.mengenrabattAnzahl = rs.getInt(11);
                this.mengenrabattRelativ = rs.getBigDecimal(12);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
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
            String[] name = new String[]{this.artikelName,
                this.rabattaktionen.getShortLieferantName(Integer.parseInt(this.liefID)),
                this.rabattaktionen.getSalePrice(Integer.parseInt(this.liefID)),
                this.rabattaktionen.getSortimentBool(Integer.parseInt(this.liefID)).toString(),
                this.liefID};
            artikelCard.artikelBox.setBox(name);
            artikelCard.nummerBox.setBox(new String[]{this.artikelNummer});
        } else if (!this.produktgruppenID.equals("")){ // Produktgruppe
            produktModus = produktDropDownOptions[1];
            int presetIndex = produktgruppenIDs.indexOf(this.produktgruppenID);
            produktgruppenBox.setSelectedIndex(presetIndex);
        }
        this.presetProduktModus = produktModus;
        produktDropDown.setSelectedItem(produktModus);
        if ( (this.rabattAbsolut != null) || (this.rabattRelativ != null) ){ // Einzelrabatt
            rabattModus = rabattDropDownOptions[0];
            this.presetRabattModus = rabattModus;
            if (this.rabattAbsolut != null) absolutField.setText( bc.priceFormatter(this.rabattAbsolut) );
            if (this.rabattRelativ != null){
                relativField.setText( bc.vatFormatter(this.rabattRelativ).replace(" %","") );
            }
        } else if (!this.mengenrabattSchwelle.equals("")){ // Mengenrabatt
            rabattModus = rabattDropDownOptions[1];
            this.presetRabattModus = rabattModus;
            mengenrabattSchwelleSpinner.setValue(this.mengenrabattSchwelle);
            if (!this.mengenrabattAnzahl.equals("")){
                kostenlosSpinner.setValue(this.mengenrabattAnzahl);
            }
            if (!this.mengenrabattRelativ.equals("")){
                mengenrabattRelativField.setText( bc.vatFormatter(this.mengenrabattRelativ).replace(" %","") );
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
        artikelCard.artikelBox.setEnabled(false);
        artikelCard.emptyArtikelButton.setEnabled(false);
        artikelCard.nummerBox.setEnabled(false);
        artikelCard.emptyNummerButton.setEnabled(false);
        produktgruppenBox.setEnabled(false);
        produktDropDown.setEnabled(false);
        absolutField.setEnabled(false);
        relativField.setEnabled(false);
        mengenrabattSchwelleSpinner.setEnabled(false);
        kostenlosSpinner.setEnabled(false);
        mengenrabattRelativField.setEnabled(false);
        rabattDropDown.setEnabled(false);
    }

    protected void showMiddle() { }

    protected void showHeader(){
        headerPanel = new JPanel();
	headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
	headerPanel.setBorder(BorderFactory.createTitledBorder(borderTitle));
        //
        JPanel generalPanel = new JPanel();
            JPanel namePanel = new JPanel();
                namePanel.setBorder(BorderFactory.createTitledBorder("Aktionsname"));
                nameField = new JTextField("", 25);
                nameField.getDocument().addDocumentListener(this);
                StringDocumentFilter sdf = new StringDocumentFilter(50);
                ((AbstractDocument)nameField.getDocument()).setDocumentFilter(sdf);
                namePanel.add(nameField);
            generalPanel.add(namePanel);
            JPanel vonPanel = new JPanel();
                vonPanel.setBorder(BorderFactory.createTitledBorder("Startdatum"));
                Calendar nowCal = Calendar.getInstance();
                //Date nowDate = nowCal.getTime();
                now = nowDate();
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
        headerPanel.add(generalPanel);
        //
	JPanel artikelPanel = new JPanel();
	    artikelPanel.add(new JLabel("Wähle:"));
            produktDropDown = new JComboBox<String>(produktDropDownOptions);
            produktDropDown.addItemListener(this);
            artikelPanel.add(produktDropDown);
            produktModus = produktDropDownOptions[0];
                artikelCard = new ArticleSelectPanelRabattDialog(this.pool, mainWindow, this, tabbedPane);

                JPanel produktgruppenCard = new JPanel();
                    JPanel produktgruppenPanel = new JPanel();// produktgruppenPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
                    produktgruppenPanel.setBorder(BorderFactory.createTitledBorder("Produktgruppe"));
                    produktgruppenBox = new JComboBox<String>(produktgruppenNamen);
                    produktgruppenBox.setRenderer(new ProduktgruppenIndentedRenderer(produktgruppenIDsList));
                    ( (JTextComponent)produktgruppenBox.getEditor().getEditorComponent() ).getDocument().addDocumentListener(this);
                    produktgruppenBox.addItemListener(this);
                    produktgruppenPanel.add(produktgruppenBox);
                produktgruppenCard.add(produktgruppenPanel);

            produktCards = new JPanel(new CardLayout());
            produktCards.add(artikelCard, produktDropDownOptions[0]);
            produktCards.add(produktgruppenCard, produktDropDownOptions[1]);
            artikelPanel.add(produktCards);
	headerPanel.add(artikelPanel);
        //
	JPanel wertPanel = new JPanel();
	    wertPanel.add(new JLabel("Wähle:"));
            rabattDropDown = new JComboBox<String>(rabattDropDownOptions);
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
                    ((AbstractDocument)absolutField.getDocument()).setDocumentFilter(bc.geldFilter);
                    absolutPanel.add(absolutField);
                    absolutPanel.add(new JLabel(bc.currencySymbol));
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
                    ((AbstractDocument)relativField.getDocument()).setDocumentFilter(bc.relFilter);
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
                    ((AbstractDocument)mengenrabattRelativField.getDocument()).setDocumentFilter(bc.relFilter);
                    mengenrabattRelativPanel.add(mengenrabattRelativField);
                    mengenrabattRelativPanel.add(new JLabel("%"));
                    mengenCard.add(mengenrabattRelativPanel);
            rabattCards = new JPanel(new CardLayout());
            rabattCards.add(einzelCard, rabattDropDownOptions[0]);
            rabattCards.add(mengenCard, rabattDropDownOptions[1]);
            wertPanel.add(rabattCards);
	headerPanel.add(wertPanel);

	allPanel.add(headerPanel, BorderLayout.NORTH);

        if (this.editMode) setPresetValues();
        if (this.enableOnlyNameAndBis) enableOnlyNameAndBis();
    }

    protected void showFooter() {
        footerPanel = new JPanel();
	footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
        if (this.editMode){
            JPanel statusPanel = new JPanel();
            editStatus = new JLabel("Keine Änderungen");
            statusPanel.add(editStatus);
            footerPanel.add(statusPanel);
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
	footerPanel.add(buttonPanel);

        allPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    protected void updateAll(){
	this.remove(allPanel);
	this.revalidate();
        if (this.editMode) queryPresetValues();
	showAll();
    }

    protected Object[] retrieveValuesFromForm() {
        String aktname = null, von = null, bis = null,
               artName = null, liefID = null, artNummer = null;
        Integer artikelID = null, prodGrID = null;
        BigDecimal absolutValue = null, relativValue = null;
        Integer schwelle = null, mengeKostenlos = null;
        BigDecimal mengeRel = null;

        aktname = nameValue();

        String vonDate = vonValue();
        String bisDate = bisValue();
        String today = this.sdf.format(now);
        if ( vonDate.equals(today) )
            von = "NOW()";
        else
            von = "\'" + vonDate + " 00:00:00\'";
        if ( unlimitedValue() )
            bis = "NULL";
        else
            bis = "\'" + bisDate + " 23:59:59\'";

        if (rabattModus == rabattDropDownOptions[0]){ // Einzelrabatt
            absolutValue = absolutValue();
            relativValue = relativValue();
        }

        if (rabattModus == rabattDropDownOptions[1]){ // Mengenrabatt
            schwelle = schwelleValue();
            mengeKostenlos = kostenlosValue();
            mengeKostenlos = mengeKostenlos <= 0 ? null : mengeKostenlos;
            mengeRel = mengeRelativValue();
            mengeRel = mengeRel.signum() <= 0 ? null : mengeRel;
        }

        if (produktModus == produktDropDownOptions[0]){ // Einzelprodukt
            artName = artNameValue()[0];
            liefID = artNameValue()[1];
            artNummer = artNummerValue();
            logger.debug("getting id for name: '{}', liefID: '{}', nummer: '{}'", artName, liefID, artNummer);
            artikelID = this.rabattaktionen.getArticleID(Integer.parseInt(liefID), artNummer); // get the internal artikelID from the DB
        }

        if (produktModus == produktDropDownOptions[1]){ // Produktgruppe
            prodGrID = produktgruppenValue();
        }

        return new Object[]{aktname, von, bis,
            artName, liefID, artNummer,
            artikelID, prodGrID,
            absolutValue, relativValue,
            schwelle, mengeKostenlos, mengeRel};
    }

    protected void insertRabattaktion() {
        Object[] values = retrieveValuesFromForm();
        String aktname = (String)values[0];
        String von = (String)values[1];
        String bis = (String)values[2];
        String artName = (String)values[3];
        String liefID = (String)values[4];
        String artNummer = (String)values[5];
        Integer artikelID = (Integer)values[6];
        Integer prodGrID = (Integer)values[7];
        BigDecimal absolutValue = (BigDecimal)values[8];
        BigDecimal relativValue = (BigDecimal)values[9];
        Integer schwelle = (Integer)values[10];
        Integer mengeKostenlos = (Integer)values[11];
        BigDecimal mengeRel = (BigDecimal)values[12];
        logger.debug(
                "aktname: " + aktname + "\n" +
                "absolutValue: " + absolutValue + "\n" +
                "relativValue: " + relativValue + "\n" +
                "schwelle: " + schwelle + "\n" +
                "mengeKostenlos: " + mengeKostenlos + "\n" +
                "mengeRel: " + mengeRel + "\n" +
                "von: " + von + "\n" +
                "bis: " + bis + "\n" +
                "prodGrID: " + prodGrID + "\n" +
                "artikelID: " + artikelID + "\n" +
                "artName: " + artName + "\n" +
                "liefID: " + liefID + "\n" +
                "artNummer: " + artNummer);
        int answer = JOptionPane.showConfirmDialog(this,
                "Rabattaktion \""+aktname+"\" in Datenbank eingeben?",
                "Rabattaktion speichern",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.YES_OPTION){
            try {
                Connection connection = this.pool.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(
                        "INSERT INTO rabattaktion SET aktionsname = ?, "+
                        "rabatt_relativ = ?, "+
                        "rabatt_absolut = ?, "+
                        "mengenrabatt_schwelle = ?, "+
                        "mengenrabatt_anzahl_kostenlos = ?, "+
                        "mengenrabatt_relativ = ?, "+
                        "von = "+von+", "+"bis = "+bis+", "+
                        "produktgruppen_id = ?, "+
                        "artikel_id = ?"
                        );
                pstmt.setString(1, aktname);
                pstmt.setBigDecimal(2, relativValue);
                pstmt.setBigDecimal(3, absolutValue);
                pstmtSetInteger(pstmt, 4, schwelle);
                pstmtSetInteger(pstmt, 5, mengeKostenlos);
                pstmt.setBigDecimal(6, mengeRel);
                pstmtSetInteger(pstmt, 7, prodGrID);
                pstmtSetInteger(pstmt, 8, artikelID);
                int result = pstmt.executeUpdate();
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
                pstmt.close();
                connection.close();
            } catch (SQLException ex) {
                logger.error("Exception:", ex);
                showDBErrorDialog(ex.getMessage());
            }
        } else { // NO_OPTION
            JOptionPane.showMessageDialog(this, "Datenbank unverändert!",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    protected void updateRabattaktion() {
        Object[] values = retrieveValuesFromForm();
        String aktname = (String)values[0];
        String von = (String)values[1];
        String bis = (String)values[2];
        String artName = (String)values[3];
        String liefID = (String)values[4];
        String artNummer = (String)values[5];
        Integer artikelID = (Integer)values[6];
        Integer prodGrID = (Integer)values[7];
        BigDecimal absolutValue = (BigDecimal)values[8];
        BigDecimal relativValue = (BigDecimal)values[9];
        Integer schwelle = (Integer)values[10];
        Integer mengeKostenlos = (Integer)values[11];
        BigDecimal mengeRel = (BigDecimal)values[12];
        logger.debug(
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
                Connection connection = this.pool.getConnection();
                // Create statement for MySQL database
                Statement stmt = connection.createStatement();
                // Run MySQL command
                PreparedStatement pstmt = connection.prepareStatement(
                        "UPDATE rabattaktion SET aktionsname = ?, rabatt_relativ = ?, "+
                        "rabatt_absolut = ?, "+ "mengenrabatt_schwelle = ?, "+
                        "mengenrabatt_anzahl_kostenlos = ?, mengenrabatt_relativ = ?, "+
                        "von = "+von+", bis = "+bis+", "+
                        "produktgruppen_id = ?, artikel_id = ? WHERE rabatt_id = ?"
                        );
                pstmt.setString(1, aktname);
                pstmt.setBigDecimal(2, relativValue);
                pstmt.setBigDecimal(3, absolutValue);
                pstmtSetInteger(pstmt, 4, schwelle);
                pstmtSetInteger(pstmt, 5, mengeKostenlos);
                pstmt.setBigDecimal(6, mengeRel);
                pstmtSetInteger(pstmt, 7, prodGrID);
                pstmtSetInteger(pstmt, 8, artikelID);
                pstmtSetInteger(pstmt, 9, this.presetRabattID);
                int result = pstmt.executeUpdate();
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
                pstmt.close();
                connection.close();
            } catch (SQLException ex) {
                logger.error("Exception:", ex);
                showDBErrorDialog(ex.getMessage());
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



    private void checkBarcodeBox(ActionEvent e) {
        if (!this.editMode){
            checkIfFormIsComplete();
        } else {
            checkIfChangesCanBeSaved();
        }
    }
    protected void checkArtikelBox(ActionEvent e) {
        if (!this.editMode){
            checkIfFormIsComplete();
        } else {
            checkIfChangesCanBeSaved();
        }
    }
    protected void checkNummerBox(ActionEvent e) {
        if (!this.editMode){
            checkIfFormIsComplete();
        } else {
            checkIfChangesCanBeSaved();
        }
    }



    boolean checkProductSelection() {
        boolean conditionsFulfilled = true;
        if ( produktModus == produktDropDownOptions[0] ){ // Einzelprodukt
            int nummerNumber = artikelCard.nummerBox.getItemCount();
            int artikelNumber = artikelCard.artikelBox.getItemCount();
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
    String[] artNameValue() { return artikelCard.artikelBox.parseArtikelName(); }
    String artNummerValue() { return (String)artikelCard.nummerBox.getSelectedItem(); }
    Integer produktgruppenValue() { return produktgruppenIDs.get(produktgruppenBox.getSelectedIndex()); }
    BigDecimal absolutValue() {
        if (absolutField.getText().equals("")) return null;
        return new BigDecimal(absolutField.getText().replace(',','.'));
    }
    BigDecimal relativValue() {
        if (relativField.getText().equals("")) return null;
        return new BigDecimal(relativField.getText().replace(',','.')).multiply(percent);
    }
    Integer schwelleValue() { return (Integer)mengenrabattSchwelleSpinner.getValue(); }
    Integer kostenlosValue() { return (Integer)kostenlosSpinner.getValue(); }
    BigDecimal mengeRelativValue() {
        if (mengenrabattRelativField.getText().equals("")) return null;
        return new BigDecimal(mengenrabattRelativField.getText().replace(',','.')).multiply(percent);
    }

    boolean areThereChanges() {
        boolean changes = false;
        if (!changes) changes = !nameValue().equals(this.aktionsname);
        logger.trace("\nchanges 1: "+changes);
        if (!changes) changes = !vonValue().equals(this.von);
        logger.trace("changes 2: "+changes);
        if ( this.unlimited ){
            if (!changes) changes = unlimitedValue() != this.unlimited;
        } else {
            if (!changes) changes = !bisValue().equals(this.bis);
        }
        logger.trace("changes 3: "+changes);
        if (!changes) changes = !produktModus.equals(this.presetProduktModus);
        logger.trace("changes 4: "+changes);
        if (!changes) changes = !rabattModus.equals(this.presetRabattModus);
        logger.trace("changes 5: "+changes);
        if (!changes && produktModus == produktDropDownOptions[0])
            changes = !artNameValue()[0].equals(this.artikelName);
        logger.trace("changes 6: "+changes);
        if (!changes && produktModus == produktDropDownOptions[0])
            changes = !artNummerValue().equals(this.artikelNummer);
        logger.trace("changes 7: "+changes);
        if (!changes && produktModus == produktDropDownOptions[1])
            changes = !produktgruppenValue().equals(this.produktgruppenID);
        logger.trace("changes 8: "+changes);

        if (this.rabattAbsolut == null && absolutValue() != null)
            changes = true;
        if (absolutValue() == null && this.rabattAbsolut != null)
            changes = true;
        if (this.rabattAbsolut != null){
            if (!changes && rabattModus == rabattDropDownOptions[0])
                changes = !absolutValue().equals(this.rabattAbsolut);
        }
        logger.trace("changes 9: "+changes);

        if (this.rabattRelativ == null && relativValue() != null)
            changes = true;
        if (relativValue() == null && this.rabattRelativ != null)
            changes = true;
        if (this.rabattRelativ != null){
            if (!changes && rabattModus == rabattDropDownOptions[0])
                changes = !relativValue().equals(this.rabattRelativ);
        }
        logger.trace("changes 10: "+changes);

        if (!changes && rabattModus == rabattDropDownOptions[1])
            changes = !schwelleValue().equals(this.mengenrabattSchwelle);
        logger.trace("changes 11: "+changes);
        if (!changes && rabattModus == rabattDropDownOptions[1])
            changes = !kostenlosValue().equals(this.mengenrabattAnzahl);
        logger.trace("changes 12: "+changes);
        if (!changes && rabattModus == rabattDropDownOptions[1])
            changes = !mengeRelativValue().equals(this.mengenrabattRelativ);
        logger.trace("changes 13: "+changes);
        return changes;
    }

    void checkIfFormIsComplete() {
        // are conditions fulfilled?
        insertButton.setEnabled(isFormComplete());
    }

    void checkIfChangesCanBeSaved() {
        // are conditions fulfilled?
        if (!areThereChanges()){ 
            editStatus.setText("Keine Änderungen");
        } else if (areThereChanges() && !isFormComplete()){ 
            editStatus.setText("Änderungen, aber unvollständig");
        } else if (areThereChanges() && isFormComplete()){
            editStatus.setText("Änderungen vorgenommen");
        }
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
        if (e.getSource() == artikelCard.barcodeBox){
            checkBarcodeBox(e);
            return;
        }
        if (e.getSource() == artikelCard.artikelBox){
            checkArtikelBox(e);
            return;
        }
        if (e.getSource() == artikelCard.nummerBox){
            checkNummerBox(e);
            return;
        }
        if (e.getSource() == closeButton){
            WindowAdapter adapter = (WindowAdapter)this.window.getWindowListeners()[0];
            adapter.windowClosing(new WindowEvent((Window)this.window, WindowEvent.WINDOW_CLOSING));
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

    protected int submit() { return 0; }

    // will data be lost on close? No.
    protected boolean willDataBeLost() {
        return false;
    }

    @Override
    public void updateSelectedArticleID(int selectedArticleID) {
    }
}
