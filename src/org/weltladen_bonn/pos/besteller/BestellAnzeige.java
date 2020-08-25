package org.weltladen_bonn.pos.besteller;

// Basic Java stuff:
import java.util.*; // for Vector, Date
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.math.RoundingMode;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.ParseException;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.mariadb.jdbc.MariaDbPoolDataSource;

// OpenDocument stuff:
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.jopendocument.dom.OOUtils;

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
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.filechooser.FileNameExtensionFilter;

// JCalendar
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JSpinnerDateEditor;

import org.weltladen_bonn.pos.*;

public class BestellAnzeige extends BestellungsGrundlage implements DocumentListener {
    // Attribute:
    private int currentPage = 1;
    private int totalPage;
    private String bestellungsZahl;
    private int bestellungsZahlInt;

    private TabbedPane tabbedPane;

    private JDateChooser dateChooserStart;
    private JDateChooser dateChooserEnd;
    private Date earliestDate;
    private Date latestDate;
    private JButton changeDateButton;
    private JButton resetButton;

    private FileExistsAwareFileChooser odsChooser;
    private JButton prevButton;
    private JButton nextButton;
    private JButton exportButton; // click the button to print the order
    private JButton editButton; // click the button to edit the order (in the Bestellen tab)
    private JTextField filterField;
    private JButton emptyFilterButton;

    private Vector<Object> selBestellNrUndTyp;
    protected Vector< Vector<Object> > bestellNummernUndTyp;
    private Vector<Vector<Object>> orderData;
    private Vector<String> orderLabels;
    private Vector< Vector<Object> > orderDetailData;
    private Vector<Integer> orderDetailArtikelIDs;
    private Vector<String> orderDetailColors;
    private Vector< Vector<Object> > orderDetailDisplayData;
    private Vector<Integer> orderDetailDisplayIndices;

    private JSplitPane splitPane;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private JPanel orderTablePanel;
    private JPanel orderDetailTablePanel;
    private JScrollPane orderDetailScrollPane;
    protected AnyJComponentJTable orderTable;
    private BestellungsTable orderDetailTable;

    private String filterStrOrders = ""; // show only orders from within a date range
    private String filterStrOrderDetail = ""; // show only specific items of the order

    // Methoden:

    /**
     *    The constructor.
     *       */
    public BestellAnzeige(MariaDbPoolDataSource pool, MainWindowGrundlage mw, TabbedPane tp)
    {
	    super(pool, mw);
        tabbedPane = tp;
        selBestellNrUndTyp = new Vector<Object>();
        selBestellNrUndTyp.add(-1); selBestellNrUndTyp.add("");

        queryEarliestBestellung();
        showAll();

        odsChooser = new FileExistsAwareFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "ODS Spreadsheet-Dokumente", "ods");
        odsChooser.setFileFilter(filter);
    }

    private void queryEarliestBestellung() {
        int day = 0;
        int month = 0;
        int year = 0;
        try {
            Connection connection = this.pool.getConnection();
            // Create statement for MySQL database
            Statement stmt = connection.createStatement();
            // Run MySQL command
            ResultSet rs = stmt
                    .executeQuery("SELECT DAY(MIN(bestellung.bestell_datum)), MONTH(MIN(bestellung.bestell_datum)), "
                            + "YEAR(MIN(bestellung.bestell_datum)) FROM bestellung");
            // Now do something with the ResultSet ...
            rs.next();
            day = rs.getInt(1);
            month = rs.getInt(2);
            year = rs.getInt(3);
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        earliestDate = calendar.getTime();

        Date now = new Date(); // current date
        if (year == 0) {
            earliestDate = now;
        }
        latestDate = now;
        // final check:
        if (latestDate.before(earliestDate)) {
            Date tmp = earliestDate;
            earliestDate = latestDate;
            latestDate = tmp;
        }
    }


    void showAll() {
        leftPanel = new JPanel();
        rightPanel = new JPanel();
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                leftPanel,
                rightPanel);
        splitPane.setOneTouchExpandable(true);
        //splitPane.setResizeWeight(0.3);
        splitPane.setDividerLocation(0.3);
        this.add(splitPane);
        showTables();
    }

    void showTables() {
        showLeftPanel();
        showRightPanel(selBestellNrUndTyp);
    }

    void showLeftPanel() {
        leftPanel.setLayout(new BorderLayout());
        showDateChangePanel();
        showOrderTable();
    }

    void showDateChangePanel() {
        JPanel datePanel = new JPanel(new GridBagLayout());
        GridBagConstraints c1 = new GridBagConstraints();
        c1.anchor = GridBagConstraints.CENTER;
        c1.fill = GridBagConstraints.HORIZONTAL;
        c1.ipadx = 5;
        c1.insets = new Insets(0, 5, 0, 5);

        Vector<Object> vecStart = setupDateChooser("Startdatum:", earliestDate, earliestDate, latestDate);
        JLabel dateLabelStart = (JLabel)vecStart.get(0);
        dateChooserStart = (JDateChooser)vecStart.get(1);
        //dateSpinnerStart = (JSpinner)vecEnd.get(2);
	//dateSpinnerStart.addChangeListener(this);

        Vector<Object> vecEnd = setupDateChooser("Enddatum:", latestDate, earliestDate, latestDate);
        JLabel dateLabelEnd = (JLabel)vecEnd.get(0);
        dateChooserEnd = (JDateChooser)vecEnd.get(1);
        //dateSpinnerEnd = (JSpinner)vecEnd.get(2);
	//dateSpinnerEnd.addChangeListener(this);

        changeDateButton = new JButton(
                //new ImageIcon(WindowContent.class.getResource("/resources/icons/refreshButtonSmall.gif")));
                "Anwenden");
        changeDateButton.addActionListener(this);
        resetButton = new JButton("Reset");
        resetButton.addActionListener(this);

        c1.gridy = 0; c1.gridx = 0; datePanel.add(dateLabelStart, c1);
        c1.gridy = 0; c1.gridx = 1; datePanel.add(dateChooserStart, c1);
        c1.gridy = 0; c1.gridx = 2; datePanel.add(resetButton, c1);
        c1.gridy = 1; c1.gridx = 0; datePanel.add(dateLabelEnd, c1);
        c1.gridy = 1; c1.gridx = 1; datePanel.add(dateChooserEnd, c1);
        c1.gridy = 1; c1.gridx = 2; datePanel.add(changeDateButton, c1);

	leftPanel.add(datePanel, BorderLayout.NORTH);
    }

    void showOrderTable() {
        // Panel for the prev/next buttons and order table
        orderTablePanel = new JPanel(new BorderLayout());

        retrieveOrderData();

	JPanel pageChangePanel = new JPanel();
	pageChangePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
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
	currentPageMax = (currentPageMax <= bestellungsZahlInt) ? currentPageMax : bestellungsZahlInt;
	JLabel header = new JLabel("Seite "+ currentPage +" von "+ totalPage + ", Bestellungen "+
	    currentPageMin + " bis "+ currentPageMax +" von "+ bestellungsZahlInt);
	pageChangePanel.add(header);
	orderTablePanel.add(pageChangePanel, BorderLayout.NORTH);

        orderLabels = new Vector<String>();
        orderLabels.add("Nr.");
        orderLabels.add("Typ");
        orderLabels.add("Jahr");
        orderLabels.add("KW");
        orderLabels.add("Datum");
        orderTable = new AnyJComponentJTable(orderData, orderLabels){
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                // add custom rendering here
                c.setForeground(Color.black); // keep it black
                return c;
            }
        };
        // selection listener:
        //orderTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
        orderTable.setFillsViewportHeight(true);
        orderTable.getSelectionModel().addListSelectionListener(new RowListener());

        // set table properties:
	TableColumn nr = orderTable.getColumn("Nr.");
	nr.setCellRenderer(rechtsAusrichter);
	nr.setPreferredWidth(100);
	TableColumn typ = orderTable.getColumn("Typ");
	typ.setCellRenderer(zentralAusrichter);
	typ.setPreferredWidth(100);
	TableColumn jahr = orderTable.getColumn("Jahr");
	jahr.setCellRenderer(rechtsAusrichter);
	jahr.setPreferredWidth(100);
	TableColumn kw = orderTable.getColumn("KW");
	kw.setCellRenderer(rechtsAusrichter);
	kw.setPreferredWidth(100);
	TableColumn datum = orderTable.getColumn("Datum");
	datum.setCellRenderer(linksAusrichter);
	datum.setPreferredWidth(200);

        JScrollPane scrollPane = new JScrollPane(orderTable);
        orderTablePanel.add(scrollPane, BorderLayout.CENTER);
        leftPanel.add(orderTablePanel, BorderLayout.CENTER);
    }

    private class RowListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                System.out.println("The mouse button has not yet been released");
                return;
            }
            int[] selRows = orderTable.getSelectedRows();
            if ( selRows.length == 1 ){
                int realRowIndex = orderTable.convertRowIndexToModel(selRows[0]); // user might have changed row order
                selBestellNrUndTyp = bestellNummernUndTyp.get(realRowIndex);
            } else {
                selBestellNrUndTyp = new Vector<Object>();
                selBestellNrUndTyp.add(-1); selBestellNrUndTyp.add("");
            }
            updateRightPanel();
        }
    }

    public void showRightPanel(Vector<Object> bestellNrUndTyp) {
        if ( (Integer)bestellNrUndTyp.get(0) > 0 ){
            rightPanel.setLayout(new BorderLayout());

            // Panel for header and both tables
            orderDetailTablePanel = new JPanel();
            orderDetailTablePanel.setLayout(new BoxLayout(orderDetailTablePanel, BoxLayout.Y_AXIS));

                // Header
                JLabel headerLabel = new JLabel("Details der Bestellung:");
                headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                orderDetailTablePanel.add(headerLabel);

                /*
                // Table with general order data:
                Vector<String> bestellung = orderData.get(bestellNummernUndTyp.indexOf(bestellNrUndTyp));
                Vector< Vector<String> > bestellData = new Vector< Vector<String> >();
                bestellData.add(bestellung);
                JTable bestellTable = new JTable(bestellData, orderLabels);
                JScrollPane sp1 = new JScrollPane(bestellTable);
                sp1.setPreferredSize(new Dimension((int)sp1.getPreferredSize().getWidth(), 40));
                orderDetailTablePanel.add(sp1);
                */

                // Table with order details:
                retrieveOrderDetailData(bestellNrUndTyp);
                orderDetailTable = new BestellungsTable(bc, orderDetailDisplayData,
                        columnLabels, orderDetailColors);
                setTableProperties(orderDetailTable);

                orderDetailScrollPane = new JScrollPane(orderDetailTable);
                orderDetailTablePanel.add(orderDetailScrollPane);

            rightPanel.add(orderDetailTablePanel, BorderLayout.CENTER);

            // Panel for buttons
            JPanel buttonPanel = new JPanel(new FlowLayout());
            exportButton = new JButton("Exportieren");
            editButton = new JButton("Bearbeiten");
	    exportButton.addActionListener(this);
	    editButton.addActionListener(this);
            buttonPanel.add(exportButton);
            buttonPanel.add(editButton);

            JLabel filterLabel = new JLabel("Filter:");
            filterLabel.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
            buttonPanel.add(filterLabel);
            filterField = new JTextField("");
            filterField.setColumns(10);
            filterField.getDocument().addDocumentListener(this);
            filterField.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
            buttonPanel.add(filterField);
	    emptyFilterButton = new JButton("x");
	    emptyFilterButton.addActionListener(this);
	    buttonPanel.add(emptyFilterButton);

            rightPanel.add(buttonPanel, BorderLayout.SOUTH);
        }
    }

    private void updateAll(){
	this.remove(splitPane);
	this.revalidate();
        selBestellNrUndTyp = new Vector<Object>();
        selBestellNrUndTyp.add(-1); selBestellNrUndTyp.add("");
	showAll();
    }

    private void updateOrderTable(){
        leftPanel.remove(orderTablePanel);
        selBestellNrUndTyp = new Vector<Object>();
        selBestellNrUndTyp.add(-1); selBestellNrUndTyp.add("");
        showOrderTable();
        updateRightPanel();
    }

    private void updateRightPanel(){
        rightPanel = new JPanel();
        splitPane.setRightComponent(rightPanel);
	//this.revalidate();
	showRightPanel(selBestellNrUndTyp);
    }

    private void updateDetailTable() {
        applyFilter(filterStrOrderDetail, orderDetailDisplayData, orderDetailDisplayIndices);
        orderDetailTablePanel.remove(orderDetailScrollPane);
	orderDetailTablePanel.revalidate();

        orderDetailTable = new BestellungsTable(bc, orderDetailDisplayData,
                columnLabels, orderDetailColors);
        setTableProperties(orderDetailTable);

        orderDetailScrollPane = new JScrollPane(orderDetailTable);
        orderDetailTablePanel.add(orderDetailScrollPane);
    }

    void retrieveOrderData() {
        orderData = new Vector< Vector<Object> >();
        bestellNummernUndTyp = new Vector< Vector<Object> >();
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
	        ResultSet rs = stmt.executeQuery(
		        "SELECT COUNT(*) FROM bestellung "+
		        filterStrOrders
		    );
            // Now do something with the ResultSet ...
            rs.next();
            bestellungsZahl = rs.getString(1);
            bestellungsZahlInt = Integer.parseInt(bestellungsZahl);
            totalPage = bestellungsZahlInt/bc.rowsPerPage + 1;
                if (currentPage > totalPage) {
                    currentPage = totalPage;
                }
            rs.close();
            rs = stmt.executeQuery(
                    "SELECT bestell_nr, typ, jahr, kw, DATE_FORMAT(bestell_datum, "+
                    "'"+bc.dateFormatSQL+"') FROM bestellung "+
            filterStrOrders +
                    "ORDER BY bestell_nr DESC "+
                    "LIMIT " + (currentPage-1)*bc.rowsPerPage + "," + bc.rowsPerPage
                    );
            while ( rs.next() ){
                Vector<Object> bestNrUndTyp = new Vector<Object>();
                    bestNrUndTyp.add(rs.getInt(1)); bestNrUndTyp.add(rs.getString(2));
                bestellNummernUndTyp.add(bestNrUndTyp);
                Vector<Object> row = new Vector<Object>();
                row.add(rs.getString(1));
                row.add(rs.getString(2));
                row.add(rs.getString(3).substring(0,4));
                row.add(rs.getString(4));
                row.add(rs.getString(5));
                orderData.add(row);
            }
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
    }

    void retrieveOrderDetailData(Vector<Object> bestellNrUndTyp) {
        orderDetailData = new Vector< Vector<Object> >();
        orderDetailArtikelIDs = new Vector<Integer>();
        orderDetailColors = new Vector<String>();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT bd.position, l.lieferant_name, a.artikel_nr, a.artikel_name, "+
                    "a.empf_vk_preis, a.vk_preis, a.vpe, bd.stueckzahl, a.beliebtheit, a.sortiment, bd.artikel_id "+
                    "FROM bestellung_details AS bd "+
                    "LEFT JOIN artikel AS a USING (artikel_id) "+
                    "LEFT JOIN lieferant AS l USING (lieferant_id) "+
                    "WHERE bd.bestell_nr = ? AND bd.typ = ? "+
                    "ORDER BY bd.position DESC"
                    );
            pstmt.setInt(1, (Integer)bestellNrUndTyp.get(0));
            pstmt.setString(2, (String)bestellNrUndTyp.get(1));
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result ...
            while ( rs.next() ){
                String pos = rs.getString(1);
                String lieferant = rs.getString(2);
                String artikelNummer = rs.getString(3);
                String artikelName = rs.getString(4);
                String empf_vkpreis = rs.getString(5);
                String vkpreis = rs.getString(6);
                String vpe = rs.getString(7);
                //Integer vpeInt = rs.getInt(7);
                //vpeInt = vpeInt > 0 ? vpeInt : 0;
                Integer stueck = rs.getInt(8);
                Integer beliebt = rs.getInt(9);
                Boolean sortimentBool = rs.getBoolean(10);
                String color = sortimentBool ? "default" : "gray";
                Integer artikelID = rs.getInt(11);

                String vkp;
                if (empf_vkpreis == null || empf_vkpreis.equals("")){
                    vkp = vkpreis;
                } else {
                    vkp = empf_vkpreis;
                }

                Vector<Object> row = new Vector<Object>();
                    row.add(pos);
                    row.add(lieferant); row.add(artikelNummer); row.add(artikelName);
                    row.add(bc.priceFormatter(vkp)+" "+bc.currencySymbol); row.add(vpe);
                    row.add(stueck); row.add(beliebt);
                    row.add(""); // row.add(removeButtons.lastElement())
                orderDetailData.add(row);
                orderDetailArtikelIDs.add(artikelID);
                orderDetailColors.add(color);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        orderDetailDisplayData = new Vector< Vector<Object> >(orderDetailData);
        initiateDisplayIndices();
    }

    Vector< Vector<Object> > retrieveOrderDetailData_forExport(Vector<Object> bestellNrUndTyp) {
        Vector< Vector<Object> > exportData = new Vector< Vector<Object> >();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT bd.position, l.lieferant_name, a.artikel_nr, a.artikel_name, "+
                    "a.vpe, a.empf_vk_preis, a.vk_preis, a.ek_preis, m.mwst_satz, bd.stueckzahl "+
                    "FROM bestellung_details AS bd "+
                    "LEFT JOIN artikel AS a USING (artikel_id) "+
                    "LEFT JOIN lieferant AS l USING (lieferant_id) "+
                    "LEFT JOIN produktgruppe AS p USING (produktgruppen_id) "+
                    "LEFT JOIN mwst AS m USING (mwst_id) "+
                    "WHERE bd.bestell_nr = ? AND bd.typ = ? "+
                    "ORDER BY p.toplevel_id, p.sub_id, p.subsub_id, a.artikel_name"
                    );
            pstmt.setInt(1, (Integer)bestellNrUndTyp.get(0));
            pstmt.setString(2, (String)bestellNrUndTyp.get(1));
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result ...
            while ( rs.next() ){
                Integer pos = rs.getInt(1);
                String lieferant = rs.getString(2);
                String artikelNummer = rs.getString(3);
                String artikelName = rs.getString(4);
                Integer vpe = rs.getString(5) == null ? null : rs.getInt(5);
                BigDecimal empf_vkpreis = rs.getBigDecimal(6);
                BigDecimal vkpreis = rs.getBigDecimal(7);
                BigDecimal ekp = rs.getBigDecimal(8);
                BigDecimal mwst = rs.getBigDecimal(9);
                Integer stueck = rs.getInt(10);

                BigDecimal vkp;
                if (empf_vkpreis == null || empf_vkpreis.equals("")){
                    vkp = vkpreis;
                } else {
                    vkp = empf_vkpreis;
                }

                Vector<Object> row = new Vector<Object>();
                    //row.add(pos); // omit position
                    row.add(lieferant); row.add(artikelNummer); row.add(artikelName);
                    row.add(vpe); row.add(vkp); row.add(ekp); row.add(mwst); row.add(stueck);
                exportData.add(row);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return exportData;
    }

    Vector< Vector<Object> > retrieveInventurDetailData_forExport(Vector<Object> bestellNrUndTyp) {
        Vector< Vector<Object> > exportData = new Vector< Vector<Object> >();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT l.lieferant_kurzname, a.artikel_nr, a.artikel_name, "+
                    "a.vk_preis, a.empf_vk_preis, a.ek_rabatt, a.ek_preis, "+
                    "a.setgroesse, m.mwst_satz, bd.stueckzahl "+
                    "FROM bestellung_details AS bd "+
                    "LEFT JOIN artikel AS a USING (artikel_id) "+
                    "LEFT JOIN lieferant AS l USING (lieferant_id) "+
                    "LEFT JOIN produktgruppe AS p USING (produktgruppen_id) "+
                    "LEFT JOIN mwst AS m USING (mwst_id) "+
                    "WHERE bd.bestell_nr = ? AND bd.typ = ? "+
                    "ORDER BY p.toplevel_id, p.sub_id, p.subsub_id, a.artikel_name"
                    );
            pstmt.setInt(1, (Integer)bestellNrUndTyp.get(0));
            pstmt.setString(2, (String)bestellNrUndTyp.get(1));
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result ...
            while ( rs.next() ){
                String lieferant = rs.getString(1);
                String artikelNummer = rs.getString(2);
                String artikelName = rs.getString(3);
                BigDecimal vkpreis = rs.getBigDecimal(4);
                BigDecimal empf_vkpreis = rs.getBigDecimal(5);
                BigDecimal ekrabatt = rs.getBigDecimal(6);
                BigDecimal ekpreis_set = rs.getBigDecimal(7);
                Integer setgroesse = rs.getInt(8);
                BigDecimal setgroesseDecimal = new BigDecimal(setgroesse);
                BigDecimal mwst = rs.getBigDecimal(9);
                Integer stueck = rs.getInt(10);
                BigDecimal stueckDecimal = new BigDecimal(stueck);

                BigDecimal ekpreis = null;
                BigDecimal single_mwst_betrag = null;
                BigDecimal single_netto = null;
                BigDecimal total_brutto = null;
                BigDecimal total_mwst_betrag = null;
                BigDecimal total_netto = null;
                if (ekpreis_set != null) {
                    ekpreis = ekpreis_set.divide(setgroesseDecimal, 2, RoundingMode.HALF_UP);
                    single_mwst_betrag = new BigDecimal( bc.priceFormatterIntern(calculateVAT(ekpreis, mwst)) );
                    single_netto = ekpreis.subtract(single_mwst_betrag);
                    total_brutto = ekpreis.multiply(stueckDecimal);
                    //total_mwst_betrag = new BigDecimal( bc.priceFormatterIntern(calculateVAT(total_brutto, mwst)) );
                    //total_netto = total_brutto.subtract(total_mwst_betrag);
                    total_mwst_betrag = single_mwst_betrag.multiply(stueckDecimal);
                    total_netto = single_netto.multiply(stueckDecimal);
                }
                BigDecimal total_vkp = null;
                if (vkpreis != null) {
                    total_vkp = vkpreis.multiply(stueckDecimal);
                }

                if (ekrabatt != null) {
                    ekrabatt = ekrabatt.multiply(bc.hundred);
                }

                Vector<Object> row = new Vector<Object>();
                    row.add(lieferant); row.add(artikelNummer); row.add(artikelName);
                    row.add(vkpreis); row.add(empf_vkpreis); row.add(setgroesse); row.add(ekrabatt);
                    row.add(ekpreis_set); row.add(ekpreis); row.add(mwst.multiply(bc.hundred)); row.add(single_netto);
                    row.add(single_mwst_betrag); row.add(stueck); row.add(total_netto);
                    row.add(total_mwst_betrag); row.add(total_brutto); row.add(total_vkp);
                exportData.add(row);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return exportData;
    }

    private void deleteOrderFromDB(Vector<Object> bestellNrUndTyp) {
        if ( (Integer)bestellNrUndTyp.get(0) > 0 ){
            PreparedStatement pstmt = null;
            Connection connection = null;
            try {
                // try a transaction:
                connection = this.pool.getConnection();
                connection.setAutoCommit(false);
                // first delete from bestellung_details (child)
                pstmt = connection.prepareStatement(
                        "DELETE FROM bestellung_details "+
                        "WHERE bestell_nr = ? AND typ = ?"
                        );
                pstmt.setInt(1, (Integer)bestellNrUndTyp.get(0));
                pstmt.setString(2, (String)bestellNrUndTyp.get(1));
                int result = pstmt.executeUpdate();
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Bestellung konnte nicht zum Ändern aus DB entfernt werden.\n"+
                            "Sie könnte beim nächsten Abschließen doppelt in DB enthalten sein.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
                pstmt.close();
                // then delete from bestellung (parent)
                pstmt = connection.prepareStatement(
                        "DELETE FROM bestellung "+
                        "WHERE bestell_nr = ? AND typ = ?"
                        );
                pstmt.setInt(1, (Integer)bestellNrUndTyp.get(0));
                pstmt.setString(2, (String)bestellNrUndTyp.get(1));
                result = pstmt.executeUpdate();
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Bestellung konnte nicht zum Ändern aus DB entfernt werden.\n"+
                            "Sie könnte beim nächsten Abschließen doppelt in DB enthalten sein.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
                connection.commit();
            } catch (SQLException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
                try {
                    connection.rollback();
                } catch (SQLException ex2) {
                    System.out.println("Rollback failed!");
                    System.out.println("Exception: " + ex2.getMessage());
                    ex2.printStackTrace();
                    showDBErrorDialog("Rollback failed! Exception: "+ex2.getMessage());
                }
            } finally {
                try {
                    if (pstmt != null){
                        pstmt.close();
                    }
                } catch (SQLException ex) {
                    System.out.println("Exception: " + ex.getMessage());
                    ex.printStackTrace();
                    showDBErrorDialog(ex.getMessage());
                }
                try {
                    connection.setAutoCommit(true);
                    
                } catch (SQLException ex) {
                    System.out.println("Couldn't set auto-commit to true again after manual transaction.");
                    System.out.println("Exception: " + ex.getMessage());
                    ex.printStackTrace();
                    showDBErrorDialog("Couldn't set auto-commit to true again after manual transaction. Exception: "+ex.getMessage());
                }
                try {
                    connection.close();
                } catch (SQLException ex) {
                    System.out.println("Couldn't close the connection.");
                    System.out.println("Exception: " + ex.getMessage());
                    ex.printStackTrace();
                    showDBErrorDialog("Couldn't close the connection. Exception: "+ex.getMessage());
                }
            }
        }
    }

    void writeSpreadSheet(File file) {
        // Get general order data
        Vector<Object> bestellung = orderData.get(bestellNummernUndTyp.indexOf(selBestellNrUndTyp));
        String typ = bestellung.get(1).toString();
        if ( !typ.equals("IVT") ) {
            writeBestellung(bestellung, file);
        } else {
            writeInventur(bestellung, file);
        }
    }

    void writeBestellung(Vector<Object> bestellung, File file) {
        String typ = bestellung.get(1).toString();
        //int jahr = Integer.parseInt(bestellung.get(2));
        Integer kw = Integer.parseInt(bestellung.get(3).toString());
        String oldDate = bestellung.get(4).toString();
        String newDate = oldDate;
        try {
            // reformat the date to be without hour:
            Date date = new SimpleDateFormat(bc.dateFormatJava).parse(oldDate);
            newDate = new SimpleDateFormat("dd.MM.yyyy").format(date);;
        } catch (java.text.ParseException ex) {
            System.out.println("ParseException: " + ex.getMessage());
            ex.printStackTrace();
        }

        // Load the template file
        final Sheet sheet;
        try {
            String filename = "vorlagen"+bc.fileSep+"Bestellvorlage_"+typ+".ods";
            File infile = new File(filename);
            if (!infile.exists()){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Zum Bestell-Typ '"+typ+"' gibt es keine Bestellvorlage "+
                        "'"+filename+"'.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }
            sheet = SpreadSheet.createFromFile(infile).getSheet(0);
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        // Change date
        sheet.getCellAt("C2").setValue(newDate);
        // Change KW
        sheet.getCellAt("I2").setValue(kw);

        // Insert order items
        Vector< Vector<Object> > data = retrieveOrderDetailData_forExport(selBestellNrUndTyp);
        System.out.println("Export data: "+data);
        for (int row=0; row<data.size(); row++){
            for (int col=0; col<data.get(row).size(); col++){
                System.out.println("Setting value at "+(8+row)+","+col+": "+data.get(row).get(col));
                sheet.setValueAt(data.get(row).get(col), col, 8+row);
            }
        }

        try {
            // Save to file and open it.
            OOUtils.open(sheet.getSpreadSheet().saveAs(file));
        } catch (FileNotFoundException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void writeInventur(Vector<Object> inventur, File file) {
        String typ = inventur.get(1).toString();
        //int jahr = Integer.parseInt(inventur.get(2));
        String jahr = inventur.get(2).toString();
        String headline = "Inventur 31.12."+jahr;

        // Load the template file
        final Sheet sheet;
        try {
            String filename = "vorlagen"+bc.fileSep+"Inventur.ods";
            File infile = new File(filename);
            if (!infile.exists()){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Zum Bestell-Typ '"+typ+"' gibt es keine Bestellvorlage "+
                        "'"+filename+"'.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }
            sheet = SpreadSheet.createFromFile(infile).getSheet(0);
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        // Change headline
        sheet.getCellAt("A1").setValue(headline);

        // Insert inventory items
        Vector< Vector<Object> > data = retrieveInventurDetailData_forExport(selBestellNrUndTyp);
        //System.out.println("Export data: "+data);
        for (int row=0; row<data.size(); row++){
            for (int col=0; col<data.get(row).size(); col++){
                System.out.println("Setting value at "+(3+row)+","+col+": "+data.get(row).get(col));
                sheet.setValueAt(data.get(row).get(col), col, 3+row);
            }
        }

        try {
            // Save to file and open it.
            OOUtils.open(sheet.getSpreadSheet().saveAs(file));
        } catch (FileNotFoundException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    /**
     *    * Each non abstract class that implements the DocumentListener
     *      must have these methods.
     *
     *    @param e the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
        if (e.getDocument() == filterField.getDocument()){
            String oldFilterStr = new String(filterStrOrderDetail);
            filterStrOrderDetail = filterField.getText();
            if ( !filterStrOrderDetail.contains(oldFilterStr) ){
                // user has deleted from, not added to the filter string, reset the displayData
                orderDetailDisplayData = new Vector< Vector<Object> >(orderDetailData);
                initiateDisplayIndices();
            }
            updateDetailTable();
        }
    }
    public void removeUpdate(DocumentEvent e) {
        insertUpdate(e);
    }
    public void changedUpdate(DocumentEvent e) {
	// Plain text components do not fire these events
    }

    private void initiateDisplayIndices() {
        orderDetailDisplayIndices = new Vector<Integer>();
        for (int i=0; i<orderDetailData.size(); i++){
            orderDetailDisplayIndices.add(i);
        }
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
            updateOrderTable();
            return;
        }
        if (e.getSource() == nextButton) {
            if (this.currentPage < totalPage)
                this.currentPage++;
            updateOrderTable();
            return;
        }
        if (e.getSource() == changeDateButton) {
            Date startDate = dateChooserStart.getDate();
            Date endDate = dateChooserEnd.getDate();
            java.sql.Date startDateSQL = new java.sql.Date(startDate.getTime());
            java.sql.Date endDateSQL = new java.sql.Date(endDate.getTime());
            String startDateStr = startDateSQL.toString();
            String endDateStr = endDateSQL.toString();
            this.filterStrOrders = "WHERE DATE(bestellung.bestell_datum) >= DATE('" + startDateStr + "') "
                    + "AND DATE(bestellung.bestell_datum) <= DATE('" + endDateStr + "') ";
            updateOrderTable();
            return;
        }
        if (e.getSource() == resetButton) {
            this.filterStrOrders = "";
            updateAll();
            return;
        }
        if (e.getSource() == editButton){
            if ( (Integer)selBestellNrUndTyp.get(0) > 0){
                if (!tabbedPane.bestellenTableIsEmpty()){
                    int answer = JOptionPane.showConfirmDialog(this,
                            "Achtung: Bestellen-Tab enthält bereits eine Bestellung,\nderen Daten verloren gehen. Fortfahren?",
                            "Warnung",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (answer == JOptionPane.YES_OPTION){
                        // continue (below)
                    } else {
                        return;
                    }
                }
                deleteOrderFromDB(selBestellNrUndTyp);
                Vector<Object> bestellung = orderData.get(bestellNummernUndTyp.indexOf(selBestellNrUndTyp));
                int jahr = Integer.parseInt(bestellung.get(2).toString());
                int kw = Integer.parseInt(bestellung.get(3).toString());
                // put order data into Bestellen tab
                tabbedPane.setBestellenTable(selBestellNrUndTyp, jahr, kw,
                        orderDetailArtikelIDs, orderDetailColors,
                        orderDetailData);
                // clear the BestellAnzeige
                updateAll();
                // switch to Bestellen tab
                tabbedPane.switchToBestellen();
            }
	    return;
	}
        if (e.getSource() == exportButton){
            SimpleDateFormat sdfOut = new SimpleDateFormat(bc.exportDirBestellung);
            String formattedDate = sdfOut.format(new Date());
            File exportDir = new File(System.getProperty("user.home") + bc.fileSep + formattedDate);
            boolean ok = true;
            if (!exportDir.exists()) {
            	ok = exportDir.mkdirs();
            }
            if (ok) {
            	odsChooser.setCurrentDirectory(exportDir);
            } else {
            	JOptionPane.showMessageDialog(this,
                                "Fehler: Ordner für Bestellungen unter "+exportDir+" existiert nicht "+
                                "und konnte nicht angelegt werden.",
                                "Fehler", JOptionPane.ERROR_MESSAGE);
            }
            String typ = (String)selBestellNrUndTyp.get(1);
            Vector<Object> bestellung = orderData.get(bestellNummernUndTyp.indexOf(selBestellNrUndTyp));
            if ( !typ.equals("IVT") ){
                Integer kwi = Integer.parseInt(bestellung.get(3).toString());
                String kw = String.format("%02d", kwi);
                odsChooser.setSelectedFile(new File("Bestellung_WL_Bonn_"+typ+"_KW"+kw+".ods"));
            } else {
                String jahr = bestellung.get(2).toString();
                odsChooser.setSelectedFile(new File("Inventur_WL_Bonn_"+jahr+".ods"));
            }
            int returnVal = odsChooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION){
                File file = odsChooser.getSelectedFile();

                writeSpreadSheet(file);

                System.out.println("Written to " + file.getAbsolutePath());
            } else {
                System.out.println("Save command cancelled by user.");
            }
            return;
	}
        if (e.getSource() == emptyFilterButton){
            filterField.setText("");
            filterField.requestFocus();
	    return;
	}
    }
}
