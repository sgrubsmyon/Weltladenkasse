package Weltladenkasse;

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

import WeltladenDB.MainWindowGrundlage;
import WeltladenDB.AnyJComponentJTable;
import WeltladenDB.JComponentCellRenderer;
import WeltladenDB.JComponentCellEditor;

public abstract class Rechnungen extends RechnungsGrundlage {
    // Attribute:
    protected int rechnungenProSeite = 25;
    protected int currentPage = 1;
    protected int totalPage;

    protected String filterStr;
    protected String titleStr;

    // The bottom panel which holds button.
    protected JPanel bottomPanel;
    protected JPanel navigationPanel;
    protected JPanel tablePanel;
    // The table holding the invoices. This is "anonymously subclassed" and two method are overridden
    protected AnyJComponentJTable myTable;

    protected JButton heuteButton = new JButton(    "  Aktuelle Rechnungen  ");
    protected JButton archivButton = new JButton(   "      Alte Rechnungen     ");
    protected JButton storniertButton = new JButton("Stornierte Rechnungen");
    protected JButton prevButton;
    protected JButton nextButton;

    protected JButton removeDetailButton = new JButton("-");
    protected Vector<JButton> detailButtons;
    protected Vector<JButton> stornoButtons;

    protected Vector< Vector<String> > data;
    protected Vector<String> overviewLabels;
    protected String rechnungsZahl;
    protected int rechnungsZahlInt;

    // Methoden:

    /**
     *    The constructor.
     *       */
    public Rechnungen(Connection conn, MainWindowGrundlage mw, String fs, String ts)
    {
	super(conn, mw);
        filterStr = fs;
        titleStr = ts;

	navigationPanel = new JPanel();
	navigationPanel.setLayout(new BoxLayout(navigationPanel, BoxLayout.Y_AXIS));
	heuteButton.addActionListener(this);
	archivButton.addActionListener(this);
	storniertButton.addActionListener(this);
	navigationPanel.add(heuteButton);
	navigationPanel.add(Box.createRigidArea(new Dimension(0,6)));
	navigationPanel.add(archivButton);
	navigationPanel.add(Box.createRigidArea(new Dimension(0,6)));
	navigationPanel.add(storniertButton);
//	Dimension largestSize = storniertButton.getSize();
	heuteButton.setPreferredSize(new Dimension(200,50));
	archivButton.setPreferredSize(new Dimension(200,50));
	heuteButton.setMinimumSize(new Dimension(200,50));
	archivButton.setMinimumSize(new Dimension(200,50));
//	heuteButton.setMaximumSize(largestSize);
//	archivButton.setMaximumSize(largestSize);

	bottomPanel = new JPanel();
	bottomPanel.setLayout(new FlowLayout());

	this.add(navigationPanel, BorderLayout.WEST);
	this.add(bottomPanel, BorderLayout.SOUTH);

	fillDataArray();
    }

    void fillDataArray(){
	this.data = new Vector< Vector<String> >();
	overviewLabels = new Vector<String>();
	overviewLabels.add("");
	overviewLabels.add("Rechnungs-Nr."); overviewLabels.add("Betrag"); overviewLabels.add("Datum");
	overviewLabels.add("");
	try {
	    // Create statement for MySQL database
	    Statement stmt = this.conn.createStatement();
	    // Run MySQL command
	    ResultSet rs = stmt.executeQuery(
		    "SELECT vd.rechnungs_nr, SUM(vd.ges_preis) AS rechnungs_betrag, " +
		    "DATE_FORMAT(verkauf.verkaufsdatum, '"+dateFormatSQL+"') " +
		    "FROM verkauf_details AS vd " +
                    "INNER JOIN verkauf USING (rechnungs_nr) " +
		    filterStr +
		    "GROUP BY vd.rechnungs_nr " +
		    "ORDER BY vd.rechnungs_nr DESC " +
		    "LIMIT " + (currentPage-1)*rechnungenProSeite + "," + rechnungenProSeite
		    );
	    // Now do something with the ResultSet ...
	    while (rs.next()) {
		Vector<String> row = new Vector<String>();
		row.add("");
		row.add(rs.getString(1)); row.add(rs.getString(2) + ' ' + currencySymbol); row.add(rs.getString(3));
		row.add("");
		// change dots to commas
		row.set(2, row.get(2).replace('.',','));
		data.add(row);
	    }
	    rs.close();
	    rs = stmt.executeQuery(
		    //"SELECT COUNT(DISTINCT rechnungs_nr) FROM verkauf_details " +
		    "SELECT COUNT(verkauf.rechnungs_nr) FROM verkauf " +
		    filterStr
		    );
	    // Now do something with the ResultSet ...
	    rs.next();
	    rechnungsZahl = rs.getString(1);
	    rechnungsZahlInt = Integer.parseInt(rechnungsZahl);
	    totalPage = rechnungsZahlInt/rechnungenProSeite + 1;
	    rs.close();
	    stmt.close();
	} catch (SQLException ex) {
	    System.out.println("Exception: " + ex.getMessage());
	    ex.printStackTrace();
	}
	myTable = new AnyJComponentJTable(this.data, overviewLabels);
//	myTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
//	myTable.setFillsViewportHeight(true);
    }

    abstract void addButtonsToTable();

    void showTable(){
	tablePanel = new JPanel();
	tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
	tablePanel.setBorder(BorderFactory.createTitledBorder(titleStr));

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
	int currentPageMin = (currentPage-1)*rechnungenProSeite + 1;
	int currentPageMax = rechnungenProSeite*currentPage;
	currentPageMax = (currentPageMax <= rechnungsZahlInt) ? currentPageMax : rechnungsZahlInt;
	JLabel header = new JLabel("Seite "+ currentPage +" von "+ totalPage + ", Rechnungen "+
	    currentPageMin + " bis "+ currentPageMax +" von "+ rechnungsZahlInt);
	pageChangePanel.add(header);
	tablePanel.add(pageChangePanel);

	addButtonsToTable();
	myTable.setDefaultRenderer( JComponent.class, new JComponentCellRenderer() );
	myTable.setDefaultEditor( JComponent.class, new JComponentCellEditor() );
//	myTable.setBounds(71,53,150,100);
//	myTable.setToolTipText("Tabelle kann nur gelesen werden.");
	setOverviewTableProperties(myTable);
//	myTable.setAutoResizeMode(5);

	JScrollPane scrollPane = new JScrollPane(myTable);
//	scrollPane.setBounds(30,30,200,150);
	tablePanel.add(scrollPane);

	this.add(tablePanel, BorderLayout.CENTER);
    }

    protected void updateTable(){
	this.remove(tablePanel);
	this.revalidate();
	fillDataArray();
	showTable();
    }

    protected void showDetailTable(int detailRow, String detailTitleStr) {
        // clear everything:
	this.remove(tablePanel);
	this.revalidate();
	tablePanel = new JPanel();
	tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        colors.clear();
        preise.clear();
        mwsts.clear();

	// First row: Show total of invoice
	Vector<String> coolRow = new Vector<String>(5);
	coolRow.add("");
	coolRow.add(this.data.get(detailRow).get(1));
	coolRow.add(this.data.get(detailRow).get(2));
	coolRow.add(this.data.get(detailRow).get(3));
	coolRow.add("");
	Vector<Vector> overviewData = new Vector<Vector>(1);
//	overviewData.add(this.data.get(detailRow));
	overviewData.add(coolRow);

        AnyJComponentJTable overviewTable = new AnyJComponentJTable(overviewData, overviewLabels){
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                // add custom rendering here
                c.setForeground(Color.black); // keep it black
                return c;
            }
        };
	removeDetailButton.addActionListener(this);
	overviewTable.setValueAt( removeDetailButton, 0, 0 );
	overviewTable.setValueAt( myTable.getValueAt(detailRow,overviewLabels.size()-1), 0, overviewLabels.size()-1 );
	overviewTable.setDefaultRenderer( JComponent.class, new JComponentCellRenderer() );
	overviewTable.setDefaultEditor( JComponent.class, new JComponentCellEditor() );
	setOverviewTableProperties(overviewTable);
        if ( overviewTable.getValueAt(0, 0) instanceof JButton )
            overviewTable.setColEditableTrue(0); // here is the remove detail button
        if ( overviewTable.getValueAt(0, overviewLabels.size()-1) instanceof JButton )
            overviewTable.setColEditableTrue(overviewLabels.size()-1);

	tablePanel.setBorder(BorderFactory.createTitledBorder(detailTitleStr));
	tablePanel.add(overviewTable.getTableHeader());
	tablePanel.add(overviewTable);
	JTextField header = new JTextField("Details dieser Rechnung:", 25);
	header.setEditable(false);
	tablePanel.add(header);

	// Now select details of the invoice
	Vector< Vector<Object> > detailData = new Vector< Vector<Object> >();
	String artikelZahl = "";
	try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT a.artikel_name, ra.aktionsname, a.artikel_nr, "+
                    "(p.toplevel_id IS NULL AND p.sub_id = 1) AS manu_rabatt, (p.toplevel_id IS NULL AND p.sub_id = 3) AS pfand, "+
                    "vd.stueckzahl, vd.ges_preis, vd.mwst_satz "+
                    "FROM verkauf_details AS vd "+
                    "LEFT JOIN artikel AS a USING (artikel_id) "+
                    "LEFT JOIN produktgruppe AS p USING (produktgruppen_id) "+
                    "LEFT JOIN rabattaktion AS ra USING (rabatt_id) "+
                    "WHERE vd.rechnungs_nr = ?"
		    );
            pstmt.setInt(1, Integer.parseInt(this.data.get(detailRow).get(1)));
	    ResultSet rs = pstmt.executeQuery();
	    // Now do something with the ResultSet ...
	    while (rs.next()) {
                String artikelname = rs.getString(1);
                String aktionsname = rs.getString(2);
                String artikelnummer = rs.getString(3);
                boolean manuRabatt = rs.getBoolean(4);
                boolean pfand = rs.getBoolean(5);
                String stueck = rs.getString(6);
                BigDecimal stueckDec = new BigDecimal(0);
                if (stueck != null)
                    stueckDec = new BigDecimal(stueck);
                String gesPreis = rs.getString(7);
                BigDecimal gesPreisDec = new BigDecimal(gesPreis);
                preise.add(gesPreisDec);
                String mwst = rs.getString(8);
                mwsts.add(new BigDecimal(mwst));
                mwst = vatFormatter(mwst);
                String einzelPreis = "";
                if (stueck != null){
                    einzelPreis = priceFormatter( gesPreisDec.divide( stueckDec, 10, RoundingMode.HALF_UP ) )+' '+currencySymbol;
                }
                gesPreis = gesPreis.replace('.',',')+' '+currencySymbol;
                String name = "";
                String color = "default";
                if ( aktionsname != null ) { name = einrueckung+aktionsname; color = "red"; artikelnummer = "RABATT"; einzelPreis = ""; } // Aktionsrabatt
                else if ( stueck == null && manuRabatt ){ // Manueller Rabatt auf Rechnung
                    name = artikelname; color = "red"; artikelnummer = "RABATT"; einzelPreis = ""; }
                else if ( manuRabatt ){ name = einrueckung+artikelname; color = "red"; artikelnummer = "RABATT"; } // Manueller Rabatt auf Artikel
                else if ( pfand && stueckDec.signum() > 0 ){ name = einrueckung+artikelname; color = "blue"; artikelnummer = "PFAND"; }
                else if ( pfand && stueckDec.signum() < 0 ){ name = artikelname; color = "green"; artikelnummer = "LEERGUT"; }
                else if ( stueckDec.signum() < 0 ){ name = artikelname; color = "green"; }
                else if ( artikelname != null ){ name = artikelname; }
                colors.add(color);
		Vector<Object> row = new Vector<Object>();
		// add units
                row.add(name); row.add(artikelnummer); row.add(stueck);
                row.add(einzelPreis); row.add(gesPreis); row.add(mwst);
		detailData.add(row);
	    }
	    rs.close();
            pstmt.close();
            pstmt = this.conn.prepareStatement(
		    "SELECT COUNT(vd.artikel_id) FROM verkauf_details AS vd " +
		    "WHERE vd.rechnungs_nr = ?"
		    );
            pstmt.setInt(1, Integer.parseInt(this.data.get(detailRow).get(1)));
	    rs = pstmt.executeQuery();
	    // Now do something with the ResultSet ...
	    rs.next();
	    artikelZahl = rs.getString(1);
	    rs.close();
	    pstmt.close();
	} catch (SQLException ex) {
	    System.out.println("Exception: " + ex.getMessage());
	    ex.printStackTrace();
	}

	RechnungsTable detailTable = new RechnungsTable(detailData, columnLabels);
	detailTable.setDefaultRenderer( JComponent.class, new JComponentCellRenderer() );
	detailTable.setDefaultEditor( JComponent.class, new JComponentCellEditor() );
	setTableProperties(detailTable);

	JScrollPane detailScrollPane = new JScrollPane(detailTable);
	tablePanel.add(detailScrollPane);

	//JTextField footer = new JTextField(artikelZahl+" Artikel", 25);
	//footer.setEditable(false);
	//tablePanel.add(footer);

        JPanel totalPricePanel = createTotalPricePanel();
	tablePanel.add(totalPricePanel);

	this.add(tablePanel, BorderLayout.CENTER);
    }

    protected void setOverviewTableProperties(AnyJComponentJTable table){
	// Spalteneigenschaften:
	table.getColumnModel().getColumn(0).setPreferredWidth(10);
	TableColumn rechnungsNr = table.getColumn("Rechnungs-Nr.");
	rechnungsNr.setCellRenderer(rechtsAusrichter);
	rechnungsNr.setPreferredWidth(50);
	TableColumn betrag = table.getColumn("Betrag");
	betrag.setCellRenderer(rechtsAusrichter);
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
    public abstract void actionPerformed(ActionEvent e);
}
