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

    // The bottom panel which holds button.
    protected JPanel allPanel;
    protected JPanel headerPanel;
    // The table holding the invoices. This is "anonymously subclassed" and two method are overridden
    protected AnyJComponentJTable myTable;

    protected JButton prevButton;
    protected JButton nextButton;
    protected JButton backButton = new JButton("Zurück");
    protected Vector<JButton> detailButtons;
    protected Vector<JButton> stornoButtons;
    protected JButton quittungsButton;

    protected Vector<Vector<Object>> data;
    protected Vector<String> dates;
    protected Vector<String> overviewLabels;
    protected String rechnungsZahl;
    protected int rechnungsZahlInt;

    private MainWindow mw;
    private WeltladenTSE tse;

    // Methoden:

    /**
     *    The constructor.
     *       */
    public Rechnungen(MariaDbPoolDataSource pool, MainWindowGrundlage mw, String fs, String ts)
    {
	    super(pool, mw);
        if (mw instanceof MainWindow) {
            this.mw = (MainWindow) mw;
            tse = this.mw.getTSE();
        } else {
            this.mw = null;
            tse = null;
        }
        filterStr = fs;
        titleStr = ts;

	    fillDataArray();
    }

    void fillDataArray(){
	this.data = new Vector< Vector<Object> >();
	this.dates = new Vector<String>();
	overviewLabels = new Vector<String>();
	overviewLabels.add("");
	overviewLabels.add("Rechnungs-Nr."); overviewLabels.add("Betrag");
        overviewLabels.add("Zahlung"); overviewLabels.add("Kunde gibt");
        overviewLabels.add("Datum");
	overviewLabels.add("");
	try {
        Connection connection = this.pool.getConnection();
	    // Create statement for MySQL database
	    Statement stmt = connection.createStatement();
	    // Run MySQL command
	    ResultSet rs = stmt.executeQuery(
		    "SELECT COUNT(*) FROM verkauf " +
		    filterStr
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
		    "SELECT vd.rechnungs_nr, SUM(vd.ges_preis) AS rechnungs_betrag, "+
            "verkauf.ec_zahlung, verkauf.kunde_gibt, " +
		    "DATE_FORMAT(verkauf.verkaufsdatum, '"+bc.dateFormatSQL+"'), " +
		    "verkauf.verkaufsdatum " +
		    "FROM verkauf_details AS vd " +
            "INNER JOIN verkauf USING (rechnungs_nr) " +
		    filterStr +
		    "GROUP BY vd.rechnungs_nr " +
		    "ORDER BY vd.rechnungs_nr DESC " +
		    "LIMIT " + (currentPage-1)*bc.rowsPerPage + "," + bc.rowsPerPage
		    );
	    // Now do something with the ResultSet ...
	    while (rs.next()) {
		Vector<Object> row = new Vector<Object>();
		row.add("");
		row.add(rs.getString(1));
                String p = bc.priceFormatter(rs.getString(2));
                if (!p.equals("")) p += ' ' + bc.currencySymbol;
                row.add(p);
                row.add(rs.getBoolean(3) ? "EC" : "Bar");
                p = bc.priceFormatter(rs.getString(4));
                if (!p.equals("")) p += ' ' + bc.currencySymbol;
                row.add(p);
                row.add(rs.getString(5));
		row.add("");
		data.add(row);

                dates.add(rs.getString(6));
	    }
	    rs.close();
	    stmt.close();
        connection.close();
	} catch (SQLException ex) {
	    logger.error("Exception:", ex);
        showDBErrorDialog(ex.getMessage());
	}
	myTable = new AnyJComponentJTable(this.data, overviewLabels);
//	myTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
//	myTable.setFillsViewportHeight(true);
    }

    abstract void addOtherStuff();

    abstract void addButtonsToTable();

    void showTable(){
        allPanel = new JPanel(new BorderLayout());
        allPanel.setBorder(BorderFactory.createTitledBorder(titleStr));
            headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.PAGE_AXIS));

            addOtherStuff();

        JPanel pageChangePanel = new JPanel();
        pageChangePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
    //	pageChangePanel.setMaximumSize(new Dimension(1024,30));
        prevButton = new JButton("<<");
        if (this.currentPage <= 1)
            prevButton.setEnabled(false);
            nextButton = new JButton(">>");
            if (this.currentPage >= totalPage)
                nextButton.setEnabled(false);
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
            headerPanel.add(pageChangePanel);

                allPanel.add(headerPanel, BorderLayout.NORTH);

            addButtonsToTable();
            setOverviewTableProperties(myTable);
            JScrollPane scrollPane = new JScrollPane(myTable);
            allPanel.add(scrollPane, BorderLayout.CENTER);

            this.add(allPanel, BorderLayout.CENTER);
        }

        protected void updateTable(){
        this.remove(allPanel);
        this.revalidate();
        fillDataArray();
        showTable();
    }

    private Vector< Vector<Object> > getDetailData(int detailRow) {
        // Now select details of the invoice
        Vector< Vector<Object> > detailData = new Vector< Vector<Object> >();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT vd.position, a.kurzname, a.artikel_name, ra.aktionsname, " +
                "a.artikel_nr, a.sortiment, a.menge, a.einheit, " +
                "(p.toplevel_id IS NULL AND p.sub_id = 1) AS manu_rabatt, " +
                "(p.toplevel_id IS NULL AND p.sub_id = 1 AND a.artikel_id = 2) AS rechnung_rabatt, " +
                "(p.toplevel_id IS NULL AND p.sub_id = 3) AS pfand, " +
                "vd.stueckzahl, vd.ges_preis, vd.mwst_satz " +
                "FROM verkauf_details AS vd LEFT JOIN artikel AS a USING (artikel_id) " +
                "LEFT JOIN produktgruppe AS p USING (produktgruppen_id) "+
                "LEFT JOIN rabattaktion AS ra USING (rabatt_id) " +
                "WHERE vd.rechnungs_nr = ?"
            );
            pstmtSetInteger(pstmt, 1, Integer.parseInt(this.data.get(detailRow).get(1).toString()));
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet ...
            while (rs.next()) {
                Integer pos = rs.getString(1) == null ? null : rs.getInt(1);
                String kurzname = rs.getString(2);
                String artikelname = rs.getString(3);
                String aktionsname = rs.getString(4);
                String artikelnummer = rs.getString(5);
                boolean sortiment = rs.getBoolean(6);
                BigDecimal menge_bd = rs.getBigDecimal(7);
                String einheit = rs.getString(8);
                String menge = formatMengeForOutput(menge_bd, einheit);
                boolean manuRabatt = rs.getBoolean(9);
                boolean rechnungRabatt = rs.getBoolean(10);
                boolean pfand = rs.getBoolean(11);
                String stueck = rs.getString(12);
                BigDecimal stueckDec = new BigDecimal(0);
                if (stueck != null)
                    stueckDec = new BigDecimal(stueck);
                String gesPreis = rs.getString(13);
                BigDecimal gesPreisDec = new BigDecimal(gesPreis);
                BigDecimal mwst = new BigDecimal(rs.getString(14));
                String einzelPreis = "";
                if (stueck != null){
                    einzelPreis = bc.priceFormatter(
                            gesPreisDec.divide(stueckDec, 10, RoundingMode.HALF_UP ) )+' '+bc.currencySymbol;
                }
                gesPreis = bc.priceFormatter(gesPreis)+' '+bc.currencySymbol;
                String name = "";
                String color = "default";
                if ( aktionsname != null ) { // Aktionsrabatt
                    name = einrueckung+aktionsname;
                    color = "red"; artikelnummer = "RABATT";// einzelPreis = "";
                }
                else if ( rechnungRabatt ){ // Manueller Rabatt auf Rechnung
                    name = artikelname; color = "red";
                    artikelnummer = "RABATT";// einzelPreis = "";
                }
                else if ( manuRabatt ){ // Manueller Rabatt auf Artikel
                    name = einrueckung+artikelname; color = "red"; artikelnummer = "RABATT";
                }
                else if ( pfand && stueckDec.signum() > 0 ){
                    name = einrueckung+artikelname; color = "blue"; artikelnummer = "PFAND";
                }
                else if ( pfand && stueckDec.signum() < 0 ){
                    name = artikelname; color = "green"; artikelnummer = "LEERGUT";
                }
                else {
                    if ( kurzname != null && !kurzname.equals("") ){
                        name = kurzname;
                    } else if (artikelname != null ){
                        name = artikelname;
                    }
                    if ( stueckDec.signum() < 0 ){
                        color = "green";
                    }
                    else if ( !sortiment ){ color = "gray"; }
                    else { color = "default"; }
                }

                KassierArtikel ka = new KassierArtikel(bc);
                ka.setPosition(pos);
                ka.setArtikelID(null);
                ka.setRabattID(null);
                ka.setName(name);
                ka.setColor(color);
                ka.setType(null);
                ka.setMenge(menge);
                ka.setStueckzahl(stueckDec.intValue());
                ka.setEinzelpreis(new BigDecimal( bc.priceFormatterIntern(einzelPreis) ));
                ka.setGesPreis(gesPreisDec);
                ka.setMwst(mwst);
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
            zahlungsModus = coolRow.get(3).toString().toLowerCase();
            try {
                kundeGibt = new BigDecimal( bc.priceFormatterIntern(coolRow.get(4).toString()) );
            } catch (NumberFormatException ex) {
                kundeGibt = null;
            }
        datum = this.dates.get(detailRow);
        rechnungsNr = Integer.parseInt(coolRow.get(1).toString());

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
        overviewTable.setValueAt( myTable.getValueAt(detailRow,overviewLabels.size()-1), 0, overviewLabels.size()-1 );
        setOverviewTableProperties(overviewTable);

        headerPanel.add(overviewTable.getTableHeader());
        headerPanel.add(overviewTable);
        JTextField header = new JTextField("Details dieser Rechnung:", 25);
        header.setEditable(false);
        headerPanel.add(header);

        allPanel.add(headerPanel, BorderLayout.NORTH);

        Vector< Vector<Object> > detailData = getDetailData(detailRow);
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

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
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
            updateTable();
            return;
        }
        final int numberOfRows = detailButtons.size();
        int detailRow = -1;
        for (int i=0; i<numberOfRows; i++){
            if (e.getSource() == detailButtons.get(i) ){
            detailRow = i;
            break;
            }
        }
        if (detailRow > -1){
            showDetailTable(detailRow, this.titleStr);
            return;
        }
        if (e.getSource() == quittungsButton){
                TreeMap<BigDecimal, Vector<BigDecimal>> mwstValues = calculateMwStValuesInRechnung();
                BigDecimal totalPrice = new BigDecimal( getTotalPrice() );
                BigDecimal rueckgeld = null;
                if (kundeGibt != null){
                    rueckgeld = kundeGibt.subtract(totalPrice);
                }
                DateTime datet = null;
                if (!datum.equals(""))
                    datet = new DateTime(datum);
                else
                    datet = new DateTime(now());
                TSETransaction tx = tse.getTransaction(rechnungsNr);
                Quittung myQuittung = new Quittung(this.pool, this.mainWindow,
                        datet, rechnungsNr, kassierArtikel,
                        mwstValues, zahlungsModus,
                        totalPrice, kundeGibt, rueckgeld, tx);
                myQuittung.printReceipt();
            return;
        }
    }
}
