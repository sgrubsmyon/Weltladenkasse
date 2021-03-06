package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.math.RoundingMode;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
//import java.awt.BorderLayout;
//import java.awt.FlowLayout;
//import java.awt.Dimension;
import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
import java.awt.event.*;
import java.awt.font.TextAttribute;

//import javax.swing.JFrame;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTable;
//import javax.swing.JTextArea;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;
import javax.swing.table.*;

// DateTime from date4j (http://www.date4j.net/javadoc/index.html)
import hirondelle.date4j.DateTime;

import org.weltladen_bonn.pos.*;
import org.weltladen_bonn.pos.kasse.WeltladenTSE.TSETransaction;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Rechnungen extends RechnungsGrundlage {
    /**
     * Base class for displaying orders
     */
    private static final Logger logger = LogManager.getLogger(Rechnungen.class);

    private static final long serialVersionUID = 1L;
    // Attribute:
    protected int currentPage = 1;
    protected int totalPage;

    protected String filterStr;
    protected String titleStr;
    private String stornoFilterStr = "";

    // The bottom panel which holds button.
    protected JPanel allPanel;
    protected JPanel headerPanel;
    protected JPanel tablePanel;
    // The table holding the invoices. This is "anonymously subclassed" and two methods are overridden
    protected AnyJComponentJTable myTable;

    private JRadioButton alleButton;
    private JRadioButton stornierteButton;
    private JRadioButton ohneStornierteButton;
    protected JButton prevButton;
    protected JButton nextButton;
    protected JButton backButton = new JButton("Zurück");
    protected Vector<JButton> detailButtons;
    protected Vector<JButton> stornoButtons;
    protected JButton quittungsButton;

    protected Vector<Vector<Object>> data;
    protected Vector<String> dates;
    protected Vector<Boolean> stornoStatuses;
    protected Vector<String> overviewLabels;
    protected String rechnungsZahl;
    protected int rechnungsZahlInt;

    protected WeltladenTSE tse;

    // Methoden:

    /**
     *    The constructor.
     *       */
    public Rechnungen(MariaDbPoolDataSource pool, MainWindowGrundlage mw, String fs, String ts) {
	    super(pool, mw);
        if (mw instanceof MainWindow) {
            MainWindow mainw = (MainWindow) mw;
            tse = mainw.getTSE();
        } else {
            tse = null;
        }
        filterStr = fs;
        titleStr = ts;
    }

    protected void setFilterStr(String fs) {
        filterStr = fs;
        fillDataArray();
    }

    void fillDataArray() {
        this.data = new Vector< Vector<Object> >();
        this.dates = new Vector<String>();
        this.stornoStatuses = new Vector<Boolean>();
        overviewLabels = new Vector<String>();
        overviewLabels.add("");
        overviewLabels.add("Rechnungs-Nr.");
        overviewLabels.add("Storniert Rechn.-Nr.");
        overviewLabels.add("Betrag");
        overviewLabels.add("Zahlung");
        overviewLabels.add("Kunde gibt");
        overviewLabels.add("Datum");
        overviewLabels.add("");
        try {
            Connection connection = this.pool.getConnection();
            // Create statement for MySQL database
            Statement stmt = connection.createStatement();
            // Run MySQL command
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM "+tableForMode("verkauf")+" AS v " +
                filterStr +
                stornoFilterStr
            );
            // Now do something with the ResultSet ...
            rs.next();
            rechnungsZahl = rs.getString(1);
            rechnungsZahlInt = Integer.parseInt(rechnungsZahl);
            totalPage = rechnungsZahlInt/bc.rowsPerPage + 1;
            if (currentPage > totalPage) {
                currentPage = totalPage;
            }
            rs.close();
            rs = stmt.executeQuery(
                "SELECT v.rechnungs_nr, v.storno_von, " +
                "SUM(vd.ges_preis) AS rechnungs_betrag, " +
                "v.ec_zahlung, v.kunde_gibt, " +
                "DATE_FORMAT(v.verkaufsdatum, '"+bc.dateFormatSQL+"'), " +
                "v.verkaufsdatum, " +
                "v.storno_von IS NOT NULL OR v.rechnungs_nr IN (SELECT storno_von FROM "+tableForMode("verkauf")+" WHERE storno_von IS NOT NULL) AS storniert " +
                "FROM "+tableForMode("verkauf")+" AS v " +
                "INNER JOIN "+tableForMode("verkauf_details")+" AS vd USING (rechnungs_nr) " +
                filterStr +
                stornoFilterStr +
                "GROUP BY v.rechnungs_nr " +
                "ORDER BY v.rechnungs_nr DESC " +
                "LIMIT " + (currentPage-1)*bc.rowsPerPage + "," + bc.rowsPerPage
            );
            // Now do something with the ResultSet ...
            while (rs.next()) {
                Vector<Object> row = new Vector<Object>();
                row.add("");
                row.add(rs.getString(1));
                row.add(rs.getString(2));
                String p = bc.priceFormatter(rs.getString(3));
                if (!p.equals("")) p += ' ' + bc.currencySymbol;
                row.add(p);
                row.add(rs.getBoolean(4) ? "EC" : "Bar");
                p = bc.priceFormatter(rs.getString(5));
                if (!p.equals("")) p += ' ' + bc.currencySymbol;
                row.add(p);
                row.add(rs.getString(6));
                row.add("");
                data.add(row);

                dates.add(rs.getString(7));
                stornoStatuses.add(rs.getBoolean(8));
            }
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        myTable = new AnyJComponentJTable(this.data, overviewLabels){
            private static final long serialVersionUID = 1L;
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (column > 0 && column < overviewLabels.size()-1 && stornoStatuses.get(row)) { // if this is a storno row
                    Font font = c.getFont();
                    Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>(font.getAttributes());
                    // attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
                    attributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE); // means italic
                    c.setFont(new Font(attributes));
                    c.setForeground(Color.red);
                } else {
                    c.setForeground(Color.black); // paint it black
                }
                return c;
            }
        };
    }

    abstract void addOtherStuff();

    abstract void addButtonsToTable();

    void createAllPanel() {
        allPanel = new JPanel(new BorderLayout());
        allPanel.setBorder(BorderFactory.createTitledBorder(titleStr));
        headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.PAGE_AXIS));

        addOtherStuff();

        JPanel stornoFilterPanel = new JPanel();
        stornoFilterPanel.setLayout(new FlowLayout());
        alleButton = new JRadioButton("Alle");
        alleButton.setSelected(true);
        stornierteButton = new JRadioButton("Nur stornierte");
        ohneStornierteButton = new JRadioButton("Ohne stornierte");
        alleButton.addActionListener(this);
        stornierteButton.addActionListener(this);
        ohneStornierteButton.addActionListener(this);
        // Group the radio buttons
        ButtonGroup stornoSelectGroup = new ButtonGroup();
        stornoSelectGroup.add(alleButton);
        stornoSelectGroup.add(stornierteButton);
        stornoSelectGroup.add(ohneStornierteButton);
        JLabel stornoFilterLabel = new JLabel("Anzeigen:");
        stornoFilterPanel.add(stornoFilterLabel);
        stornoFilterLabel.setLabelFor(alleButton);
        stornoFilterPanel.add(stornoFilterLabel);
        stornoFilterPanel.add(alleButton);
        stornoFilterPanel.add(stornierteButton);
        stornoFilterPanel.add(ohneStornierteButton);
        headerPanel.add(stornoFilterPanel);

        allPanel.add(headerPanel, BorderLayout.NORTH);
        this.add(allPanel, BorderLayout.CENTER);
    }

    void showTable() {
        tablePanel = new JPanel(new BorderLayout());

        JPanel pageChangePanel = new JPanel();
        pageChangePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        // pageChangePanel.setMaximumSize(new Dimension(1024,30));
        prevButton = new JButton("<<");
        if (this.currentPage <= 1) prevButton.setEnabled(false);
        nextButton = new JButton(">>");
        if (this.currentPage >= totalPage) nextButton.setEnabled(false);
        pageChangePanel.add(prevButton);
        pageChangePanel.add(nextButton);
        prevButton.addActionListener(this);
        nextButton.addActionListener(this);
        int currentPageMin = (currentPage-1)*bc.rowsPerPage + 1;
        int currentPageMax = bc.rowsPerPage*currentPage;
        currentPageMax = (currentPageMax <= rechnungsZahlInt) ? currentPageMax : rechnungsZahlInt;
        JLabel header = new JLabel("Seite "+ currentPage +" von "+ totalPage + ", Rechnungen "+
        currentPageMin + " bis "+ currentPageMax +" von "+ rechnungsZahlInt);
        pageChangePanel.add(header);
        tablePanel.add(pageChangePanel, BorderLayout.NORTH);

        addButtonsToTable();
        setOverviewTableProperties(myTable);
        JScrollPane scrollPane = new JScrollPane(myTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        allPanel.add(tablePanel, BorderLayout.CENTER);
    }

    protected void updateTable(){
        allPanel.remove(tablePanel);
        this.revalidate();
        fillDataArray();
        showTable();
    }

    protected void updateAll(){
        this.remove(allPanel);
        this.revalidate();
        createAllPanel();
        fillDataArray();
        showTable();
    }

    private Vector< Vector<Object> > getDetailData() {
        // Now select details of the invoice
        Vector< Vector<Object> > detailData = new Vector< Vector<Object> >();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT vd.position, vd.artikel_id, vd.rabatt_id, "+
                "a.kurzname, a.artikel_name, ra.aktionsname, "+
                "a.artikel_nr, a.sortiment, a.menge, a.einheit, "+
                "(p.toplevel_id IS NULL AND p.sub_id = 3) AS pfand, "+
                "vd.stueckzahl, vd.ges_preis, "+
                "IFNULL(ad.ges_preis / vd.stueckzahl, vd.ges_preis / vd.stueckzahl) AS einzelpreis, "+
                "vd.mwst_satz, ad.ges_preis IS NOT NULL AS part_of_anzahlung, "+
                "gsv.gutschein_nr AS gutschein_nr_verkauf, "+
                "gse.gutschein_nr AS gutschein_nr_einloes "+
                "FROM "+tableForMode("verkauf_details")+" AS vd "+
                "LEFT JOIN artikel AS a USING (artikel_id) "+
                "LEFT JOIN produktgruppe AS p USING (produktgruppen_id) "+
                "LEFT JOIN rabattaktion AS ra USING (rabatt_id) "+
                "LEFT JOIN "+tableForMode("anzahlung_details")+" AS ad USING (vd_id) "+
                "LEFT JOIN "+tableForMode("gutschein")+" AS gsv ON vd.vd_id = gsv.gutschein_in_vd_id AND gsv.einloesung_in_vd_id IS NULL "+
                "LEFT JOIN "+tableForMode("gutschein")+" AS gse ON vd.vd_id = gse.einloesung_in_vd_id "+
                "WHERE vd.rechnungs_nr = ?"
            );
            pstmtSetInteger(pstmt, 1, rechnungsNr);
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet ...
            while (rs.next()) {
                Integer pos = rs.getString(1) == null ? null : rs.getInt(1);
                Integer artikelID = rs.getString(2) == null ? null : rs.getInt(2);
                Integer rabattID = rs.getString(3) == null ? null : rs.getInt(3);
                String kurzname = rs.getString(4);
                String artikelname = rs.getString(5);
                String aktionsname = rs.getString(6);
                String artikelnummer = rs.getString(7);
                boolean sortiment = rs.getBoolean(8);
                BigDecimal menge_bd = rs.getBigDecimal(9);
                String einheit = rs.getString(10);
                String menge = formatMengeForOutput(menge_bd, einheit);
                boolean pfand = rs.getBoolean(11);
                String stueck = rs.getString(12);
                BigDecimal stueckDec = new BigDecimal(0);
                if (stueck != null)
                    stueckDec = new BigDecimal(stueck);
                String gesPreis = rs.getString(13);
                BigDecimal gesPreisDec = new BigDecimal(gesPreis);
                String einzelPreis = "";
                if (stueck != null){
                    einzelPreis = bc.priceFormatter(rs.getString(14))+' '+bc.currencySymbol;
                }
                BigDecimal mwst = new BigDecimal(rs.getString(15));
                Boolean part_of_anzahlung = rs.getBoolean(16);
                Integer gutscheinNrVerkauf = rs.getInt(17);
                Integer gutscheinNrEinloes = rs.getInt(18);
                if (part_of_anzahlung) gesPreis = "";
                else gesPreis = bc.priceFormatter(gesPreisDec)+' '+bc.currencySymbol;
                String name = artikelname;
                String color = "default";
                String type = "artikel";
                Integer gutscheinNr = null;
                // if (artikelID == gutscheinArtikelID) { 
                if (gutscheinNrVerkauf != 0) { // rs.getInt() does not return null, but 0
                    // TODO Implement:
                    // name = artikelNameGutschein();
                    gutscheinNr = gutscheinNrVerkauf;
                    name = "Gutschein Nr. "+gutscheinNr;
                    color = "green";
                    type = "gutschein";
                // } else if (artikelID == gutscheineinloesungArtikelID) {
                } else if (gutscheinNrEinloes != 0) { // rs.getInt() does not return null, but 0
                    // TODO Implement:
                    // name = artikelNameGutscheinEinloes();
                    gutscheinNr = gutscheinNrEinloes;
                    name = "Einlösung Gutschein Nr. "+gutscheinNr;
                    artikelnummer = "GUTSCHEINEINLOES";
                    color = "green";
                    type = "gutscheineinloesung";
                } else if ( aktionsname != null ) { // Aktionsrabatt
                    name = einrueckung+aktionsname;
                    artikelnummer = "RABATT";
                    color = "red";
                    type = "rabatt";
                } else if ( artikelID == artikelRabattArtikelID ){ // Manueller Rabatt auf Artikel
                    name = einrueckung+artikelname;
                    artikelnummer = "RABATT";
                    color = "red";
                    type = "rabatt";
                } else if ( artikelID == rechnungRabattArtikelID ){ // Manueller Rabatt auf Rechnung
                    artikelnummer = "RABATT";
                    color = "red";
                    type = "rabattrechnung";
                    menge = "";
                } else if ( artikelID == preisanpassungArtikelID ){ // Manuelle Preisanpassung auf Artikel
                    name = einrueckung+artikelname;
                    artikelnummer = "ANPASSUNG";
                    color = "red";
                    type = "rabatt";
                } else if ( artikelID == anzahlungArtikelID ){ // Anzahlung
                    artikelnummer = "ANZAHLUNG";
                    color = "red";
                    type = "anzahlung";
                } else if ( artikelID == anzahlungsaufloesungArtikelID ){ // Anzahlungsauflösung
                    name = artikelNameAnzAuflWithDateAndRechNr();
                    artikelnummer = "ANZAHLUNGSAUFLÖSUNG";
                    color = "red";
                    type = "anzahlungsaufloesung";
                } else if ( pfand && stueckDec.signum() > 0 ){
                    name = einrueckung+artikelname;
                    artikelnummer = "PFAND";
                    color = "blue";
                    type = "pfand";
                } else if ( pfand && stueckDec.signum() < 0 ){
                    artikelnummer = "LEERGUT";
                    color = "green";
                    type = "leergut";
                } else {
                    if ( kurzname != null && !kurzname.equals("") ){
                        name = kurzname;
                    } else if (artikelname != null){
                        name = artikelname;
                    } else {
                        name = "";
                    }
                    if ( stueckDec.signum() < 0 ){
                        color = "green";
                        type = "rueckgabe";
                    } else if ( !sortiment ){ color = "gray"; }
                    else { color = "default"; }
                }

                KassierArtikel ka = new KassierArtikel(bc);
                ka.setPosition(pos);
                ka.setArtikelID(artikelID);
                ka.setRabattID(rabattID);
                ka.setName(name);
                ka.setArtikelNummer(artikelnummer);
                ka.setColor(color);
                ka.setType(type);
                ka.setMenge(menge);
                ka.setStueckzahl(stueckDec.intValue());
                ka.setEinzelPreis(new BigDecimal( bc.priceFormatterIntern(einzelPreis) ));
                ka.setGesPreis(gesPreisDec);
                ka.setMwst(mwst);
                ka.setGutscheinNr(gutscheinNr);
                ka.setPartOfAnzahlung(part_of_anzahlung);
                kassierArtikel.add(ka);

                mwsts.add(mwst);

                Vector<Object> row = new Vector<Object>();
                // add units
                row.add(pos);
                row.add(name); row.add(artikelnummer); row.add(stueck);
                row.add(einzelPreis); row.add(gesPreis); row.add(bc.vatFormatter(mwst));
                detailData.add(row);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return detailData;
    }


    private String artikelNameAnzAuflWithDateAndRechNr() {
        String result = "Anzahlungsauflösung";
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT anzahlung_in_rech_nr, DATE_FORMAT(datum, "+
                "'"+bc.dateFormatSQL+"') "+
                "FROM "+tableForMode("anzahlung")+" "+
                "WHERE anzahlung_in_rech_nr = ("+
                "  SELECT anzahlung_in_rech_nr FROM "+tableForMode("anzahlung")+" "+
                "    WHERE aufloesung_in_rech_nr = ? LIMIT 1"+
                ") AND aufloesung_in_rech_nr IS NULL"
            );
            pstmtSetInteger(pstmt, 1, rechnungsNr);
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet ...
            if (rs.next()) {
                result = "Anzahlung vom "+rs.getString(2)+", Rech.-Nr. "+rs.getString(1);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }


    protected void showDetailTable(int detailRow, String detailTitleStr) {
        // clear everything:
        this.remove(allPanel);
        this.revalidate();
        kassierArtikel.clear();
        mwsts.clear();

        allPanel = new JPanel(new BorderLayout());
        allPanel.setBorder(BorderFactory.createTitledBorder(detailTitleStr));
        headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.PAGE_AXIS));

        // First row: Show total of invoice
        Vector<Object> coolRow = this.data.get(detailRow);
        coolRow.set(0, "");
        //coolRow.set(coolRow.size()-1, "");
        Vector<Vector<Object>> overviewData = new Vector<Vector<Object>>(1);
        overviewData.add(coolRow);
        zahlungsModus = coolRow.get(4).toString().toLowerCase();
        try {
            kundeGibt = new BigDecimal( bc.priceFormatterIntern(coolRow.get(5).toString()) );
        } catch (NumberFormatException ex) {
            kundeGibt = null;
        }
        datum = this.dates.get(detailRow);
        rechnungsNr = Integer.parseInt(coolRow.get(1).toString());
        stornoVon = coolRow.get(2) == null ? null : Integer.parseInt(coolRow.get(2).toString());
        storniert = this.stornoStatuses.get(detailRow);

        AnyJComponentJTable overviewTable = new AnyJComponentJTable(overviewData, overviewLabels){
            private static final long serialVersionUID = 1L;
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                // add custom rendering here
                c.setForeground(Color.black); // keep it black
                return c;
            }
        };
        backButton.addActionListener(this);
        overviewTable.setValueAt( backButton, 0, 0 );
        overviewTable.setValueAt( myTable.getValueAt(detailRow, overviewLabels.size()-1), 0, overviewLabels.size()-1 );
        setOverviewTableProperties(overviewTable);

        headerPanel.add(overviewTable.getTableHeader());
        headerPanel.add(overviewTable);
        JTextField header = new JTextField("Details dieser Rechnung:", 25);
        header.setEditable(false);
        headerPanel.add(header);

        allPanel.add(headerPanel, BorderLayout.NORTH);

        Vector< Vector<Object> > detailData = getDetailData();
        Vector<String> colors = new Vector<String>();
        for (KassierArtikel a : kassierArtikel) {
            colors.add(a.getColor());
        }
        ArticleSelectTable detailTable = new ArticleSelectTable(detailData, columnLabels, colors);
        setTableProperties(detailTable);
        JScrollPane detailScrollPane = new JScrollPane(detailTable);

        allPanel.add(detailScrollPane, BorderLayout.CENTER);

        //JTextField footer = new JTextField(artikelZahl+" Artikel", 25);
        //footer.setEditable(false);
        //allPanel.add(footer);

        JPanel footerPanel = new JPanel();
        footerPanel.setLayout(new BorderLayout());
        
        // center
        JPanel centerPanel = new JPanel();
        JPanel bottomPanel = createBottomPanel();
        centerPanel.add(bottomPanel);
        footerPanel.add(centerPanel, BorderLayout.CENTER);
        
        // right
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
        quittungsButton = new JButton("Quittung");
        quittungsButton.setMnemonic(KeyEvent.VK_Q);
        quittungsButton.addActionListener(this);
        rightPanel.add(quittungsButton);
        footerPanel.add(rightPanel, BorderLayout.EAST);

        allPanel.add(footerPanel, BorderLayout.SOUTH);

        this.add(allPanel, BorderLayout.CENTER);
    }

    protected void setOverviewTableProperties(AnyJComponentJTable table){
        // Spalteneigenschaften:
        table.getColumnModel().getColumn(0).setPreferredWidth(10);
        TableColumn rechnr = table.getColumn("Rechnungs-Nr.");
        rechnr.setCellRenderer(rechtsAusrichter);
        rechnr.setPreferredWidth(50);
        TableColumn stornovon = table.getColumn("Storniert Rechn.-Nr.");
        stornovon.setCellRenderer(rechtsAusrichter);
        stornovon.setPreferredWidth(50);
        TableColumn betrag = table.getColumn("Betrag");
        betrag.setCellRenderer(rechtsAusrichter);
        TableColumn zahlung = table.getColumn("Zahlung");
        zahlung.setCellRenderer(rechtsAusrichter);
        TableColumn kgibt = table.getColumn("Kunde gibt");
        kgibt.setCellRenderer(rechtsAusrichter);
        TableColumn datum = table.getColumn("Datum");
        datum.setCellRenderer(rechtsAusrichter);
        datum.setPreferredWidth(100);
        table.getColumnModel().getColumn(overviewLabels.size()-1).setPreferredWidth(20);
    }

    protected abstract String getZKasseId();
    
    protected abstract LinkedHashMap<String, String> getTSEStatusValues();

    protected void printQuittung() {
        DateTime datet = null;
        if (!datum.equals(""))
            datet = new DateTime(datum);
        else
            datet = new DateTime(now());
        TreeMap<BigDecimal, Vector<BigDecimal>> mwstValues = calculateMwStValuesInRechnung();
        BigDecimal totalPrice = new BigDecimal( getTotalPrice() );
        BigDecimal rueckgeld = null;
        if (kundeGibt != null){
            rueckgeld = kundeGibt.subtract(totalPrice);
        }
        TSETransaction tx = tse.getTransactionByRechNr(rechnungsNr);
        Quittung myQuittung = new Quittung(this.pool, this.mainWindow,
            datet, rechnungsNr, stornoVon,
            kassierArtikel, mwstValues, zahlungsModus,
            totalPrice, kundeGibt, rueckgeld,
            tx, getZKasseId(), getTSEStatusValues());
        myQuittung.printReceipt();
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == alleButton) {
            this.stornoFilterStr = ""; // reset to normal
            updateTable();
            return;
        }
        if (e.getSource() == stornierteButton) {
            this.stornoFilterStr = "AND (v.storno_von IS NOT NULL OR v.rechnungs_nr IN (SELECT storno_von FROM "+tableForMode("verkauf")+" WHERE storno_von IS NOT NULL)) ";
            updateTable();
            return;
        }
        if (e.getSource() == ohneStornierteButton) {
            this.stornoFilterStr = "AND NOT (v.storno_von IS NOT NULL OR v.rechnungs_nr IN (SELECT storno_von FROM "+tableForMode("verkauf")+" WHERE storno_von IS NOT NULL)) ";
            updateTable();
            return;
        }
        if (e.getSource() == prevButton) {
            if (this.currentPage > 1)
                this.currentPage--;
            updateTable();
            return;
        }
        if (e.getSource() == nextButton) {
            if (this.currentPage < totalPage)
                this.currentPage++;
            updateTable();
            return;
        }
        if (e.getSource() == backButton) {
            updateAll();
            return;
        }
        final int numberOfRows = detailButtons.size();
        int detailRow = -1;
        for (int i=0; i<numberOfRows; i++) {
            if (e.getSource() == detailButtons.get(i) ) {
            detailRow = i;
            break;
            }
        }
        if (detailRow > -1) {
            showDetailTable(detailRow, this.titleStr);
            return;
        }
        if (e.getSource() == quittungsButton) {
            printQuittung();
            return;
        }
    }
}
