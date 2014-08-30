package Weltladenbesteller;

// Basic Java stuff:
import java.util.*; // for Vector
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

import WeltladenDB.MainWindowGrundlage;
import WeltladenDB.ArtikelGrundlage;
import WeltladenDB.AnyJComponentJTable;
import WeltladenDB.JComponentCellRenderer;
import WeltladenDB.JComponentCellEditor;

public class BestellAnzeige extends BestellungsGrundlage implements DocumentListener {
    // Attribute:
    private int bestellungenProSeite = 25;
    private int currentPage = 1;
    private int totalPage;
    private String bestellungsZahl;
    private int bestellungsZahlInt;

    private TabbedPane tabbedPane;

    private JFileChooser fc;
    private JButton prevButton;
    private JButton nextButton;
    private JButton exportButton; // click the button to print the order
    private JButton editButton; // click the button to edit the order (in the Bestellen tab)
    private JTextField filterField;

    private int selBestellNr;
    private Vector<Integer> bestellNummern;
    private Vector< Vector<String> > orderData;
    private Vector<String> orderLabels;
    private Vector< Vector<Object> > orderDetailData;
    private Vector<Integer> orderDetailArtikelIDs;
    private Vector< Vector<Object> > orderDetailDisplayData;
    private Vector<Integer> orderDetailDisplayIndices;

    private JSplitPane splitPane;
    private JPanel orderPanel;
    private JPanel orderDetailPanel;
    private JPanel orderDetailTablePanel;
    private JScrollPane orderDetailScrollPane;
    private AnyJComponentJTable orderTable;
    private BestellungsTable orderDetailTable;

    private String filterStr = ""; // show only specific items of the order

    // Methoden:

    /**
     *    The constructor.
     *       */
    public BestellAnzeige(Connection conn, MainWindowGrundlage mw, TabbedPane tp)
    {
	super(conn, mw);
        tabbedPane = tp;
        selBestellNr = -1;
        showAll();
        initializeExportDialog();
    }

    void showAll() {
        orderPanel = new JPanel();
        orderDetailPanel = new JPanel();
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                orderPanel,
                orderDetailPanel);
        splitPane.setOneTouchExpandable(true);
        //splitPane.setResizeWeight(0.3);
        splitPane.setDividerLocation(0.3);
        this.add(splitPane);
        showTables();
    }

    void showTables() {
        showOrderTable();
        showOrderDetailTable(selBestellNr);
    }

    void showOrderTable() {
        orderPanel.setLayout(new BorderLayout());
        orderLabels = new Vector<String>();
        orderLabels.add("Nr.");
        orderLabels.add("Jahr");
        orderLabels.add("KW");
        orderLabels.add("Datum");
        retrieveOrderData();
        orderTable = new AnyJComponentJTable(orderData, orderLabels){
            // Implement table cell tool tips.
            @Override
            public String getToolTipText(MouseEvent e) {
                Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                String tip = "";
                try { tip = getValueAt(rowIndex, colIndex).toString(); }
                catch (ArrayIndexOutOfBoundsException ex) { }
                return tip;
            }
            // Implement table header tool tips.
            @Override
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        Point p = e.getPoint();
                        int colIndex = columnAtPoint(p);
                        String tip = null;
                        try { tip = getColumnName(colIndex); }
                        catch (Exception ex) { }
                        return tip;
                    }
                };
            }
        };
        // selection listener:
        //orderTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
        orderTable.setFillsViewportHeight(true);
        orderTable.getSelectionModel().addListSelectionListener(new RowListener());

        JScrollPane scrollPane = new JScrollPane(orderTable);
        orderPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private class RowListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                System.out.println("The mouse button has not yet been released");
                return;
            }
            System.out.println("ListSelection");
            int[] selRows = orderTable.getSelectedRows();
            if ( selRows.length == 1 ){
                int realRowIndex = orderTable.convertRowIndexToModel(selRows[0]); // user might have changed row order
                selBestellNr = bestellNummern.get(realRowIndex);
            } else {
                selBestellNr = -1;
            }
            updateDetailPanel();
        }
    }

    public void showOrderDetailTable(int bestellNr) {
        if ( bestellNr > 0 ){
            orderDetailPanel.setLayout(new BorderLayout());

            // Panel for header and both tables
            orderDetailTablePanel = new JPanel();
            orderDetailTablePanel.setLayout(new BoxLayout(orderDetailTablePanel, BoxLayout.Y_AXIS));

                // Header
                JLabel headerLabel = new JLabel("Details der Bestellung:");
                headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                orderDetailTablePanel.add(headerLabel);

                // Table with general order data:
                Vector<String> bestellung = orderData.get(bestellNummern.indexOf(bestellNr));
                Vector< Vector<String> > bestellData = new Vector< Vector<String> >();
                bestellData.add(bestellung);
                JTable bestellTable = new JTable(bestellData, orderLabels);
                JScrollPane sp1 = new JScrollPane(bestellTable);
                sp1.setPreferredSize(new Dimension((int)sp1.getPreferredSize().getWidth(), 40));
                orderDetailTablePanel.add(sp1);

                // Table with order details:
                retrieveOrderDetailData(bestellNr);
                orderDetailTable = new BestellungsTable(orderDetailDisplayData, columnLabels);
                setTableProperties(orderDetailTable);

                orderDetailScrollPane = new JScrollPane(orderDetailTable);
                orderDetailTablePanel.add(orderDetailScrollPane);

            orderDetailPanel.add(orderDetailTablePanel, BorderLayout.CENTER);

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

            orderDetailPanel.add(buttonPanel, BorderLayout.SOUTH);
        }
    }

    private void updateAll(){
	this.remove(splitPane);
	this.revalidate();
        selBestellNr = -1;
	showAll();
    }

    private void updateDetailPanel(){
        orderDetailPanel = new JPanel();
        splitPane.setRightComponent(orderDetailPanel);
	//this.revalidate();
	showOrderDetailTable(selBestellNr);
    }

    private void updateDetailTable() {
        orderDetailTablePanel.remove(orderDetailScrollPane);
	orderDetailTablePanel.revalidate();

        orderDetailTable = new BestellungsTable(orderDetailDisplayData, columnLabels);
        setTableProperties(orderDetailTable);

        orderDetailScrollPane = new JScrollPane(orderDetailTable);
        orderDetailTablePanel.add(orderDetailScrollPane);
    }

    void retrieveOrderData() {
        orderData = new Vector< Vector<String> >();
        bestellNummern = new Vector<Integer>();
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT bestell_nr, jahr, kw, DATE_FORMAT(bestell_datum, "+
                    "'"+dateFormatSQL+"') FROM bestellung ORDER BY "+
                    "bestell_nr DESC LIMIT " +
                    (currentPage-1)*bestellungenProSeite + "," + bestellungenProSeite
                    );
            // Now do something with the ResultSet, should be only one result ...
            while ( rs.next() ){
                bestellNummern.add(rs.getInt(1));
                Vector<String> row = new Vector<String>();
                row.add(rs.getString(1));
                row.add(rs.getString(2).substring(0,4));
                row.add(rs.getString(3));
                row.add(rs.getString(4));
                orderData.add(row);
            }
	    rs.close();
	    rs = stmt.executeQuery(
		    "SELECT COUNT(bestell_nr) FROM bestellung"
		    );
	    // Now do something with the ResultSet ...
	    rs.next();
	    bestellungsZahl = rs.getString(1);
	    bestellungsZahlInt = Integer.parseInt(bestellungsZahl);
	    totalPage = bestellungsZahlInt/bestellungenProSeite + 1;
	    rs.close();
	    stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void retrieveOrderDetailData(int bestellNr) {
        orderDetailData = new Vector< Vector<Object> >();
        orderDetailArtikelIDs = new Vector<Integer>();
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT l.lieferant_name, a.artikel_nr, a.artikel_name, "+
                    "a.vk_preis, a.vpe, bd.stueckzahl, bd.artikel_id "+
                    "FROM bestellung_details AS bd "+
                    "LEFT JOIN artikel AS a USING (artikel_id) "+
                    "LEFT JOIN lieferant AS l USING (lieferant_id) "+
                    "WHERE bd.bestell_nr = ?"
                    );
            pstmt.setInt(1, bestellNr);
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result ...
            while ( rs.next() ){
                String lieferant = rs.getString(1);
                String artikelNummer = rs.getString(2);
                String artikelName = rs.getString(3);
                String vkp = rs.getString(4);
                String vpe = rs.getString(5);
                //Integer vpeInt = rs.getInt(5);
                //vpeInt = vpeInt > 0 ? vpeInt : 0;
                Integer stueck = rs.getInt(6);
                Integer artikelID = rs.getInt(7);

                Vector<Object> row = new Vector<Object>();
                row.add(lieferant); row.add(artikelNummer); row.add(artikelName);
                row.add(priceFormatter(vkp)+" "+currencySymbol); row.add(vpe); row.add(stueck);
                row.add(""); // row.add(removeButtons.lastElement())
                orderDetailData.add(row);
                orderDetailArtikelIDs.add(artikelID);
            }
	    rs.close();
	    pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        orderDetailDisplayData = new Vector< Vector<Object> >(orderDetailData);
        initiateDisplayIndices();
    }

    Vector< Vector<Object> > retrieveOrderDetailData_forExport(int bestellNr) {
        Vector< Vector<Object> > orderExportData = new Vector< Vector<Object> >();
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT l.lieferant_name, a.artikel_nr, a.artikel_name, "+
                    "a.vpe, a.vk_preis, a.ek_preis, m.mwst_satz, bd.stueckzahl "+
                    "FROM bestellung_details AS bd "+
                    "LEFT JOIN artikel AS a USING (artikel_id) "+
                    "LEFT JOIN lieferant AS l USING (lieferant_id) "+
                    "LEFT JOIN produktgruppe AS p USING (produktgruppen_id) "+
                    "LEFT JOIN mwst AS m USING (mwst_id) "+
                    "WHERE bd.bestell_nr = ? "+
                    "ORDER BY p.toplevel_id, p.sub_id, p.subsub_id"
                    );
            pstmt.setInt(1, bestellNr);
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result ...
            while ( rs.next() ){
                String lieferant = rs.getString(1);
                String artikelNummer = rs.getString(2);
                String artikelName = rs.getString(3);
                Integer vpe = rs.getString(4) == null ? null : rs.getInt(4);
                BigDecimal vkp = rs.getBigDecimal(5);
                BigDecimal ekp = rs.getBigDecimal(6);
                BigDecimal mwst = rs.getBigDecimal(7);
                Integer stueck = rs.getInt(8);

                Vector<Object> row = new Vector<Object>();
                row.add(lieferant); row.add(artikelNummer); row.add(artikelName);
                row.add(vpe); row.add(vkp); row.add(ekp); row.add(mwst); row.add(stueck);
                orderExportData.add(row);
            }
	    rs.close();
	    pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return orderExportData;
    }

    private void deleteOrderFromDB(int bestellNr) {
        if (bestellNr > 0){
            try {
                PreparedStatement pstmt = this.conn.prepareStatement(
                        "DELETE bestellung, bestellung_details FROM bestellung "+
                        "INNER JOIN bestellung_details USING (bestell_nr) "+
                        "WHERE bestell_nr = ?"
                        );
                pstmt.setInt(1, bestellNr);
                int result = pstmt.executeUpdate();
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Bestellung konnte nicht zum Ändern aus DB entfernt werden.\n"+
                            "Sie könnte beim nächsten Abschließen doppelt in DB enthalten sein.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
                pstmt.close();
            } catch (SQLException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    void initializeExportDialog() {
        fc = new JFileChooser(){
            // override approveSelection to get a confirmation dialog if file exists
            @Override
            public void approveSelection(){
                File f = getSelectedFile();
                if (f.exists() && getDialogType() == SAVE_DIALOG){
                    int result = JOptionPane.showConfirmDialog(this,
                            "Datei existiert bereits. Überschreiben?",
                            "Datei existiert",
                            JOptionPane.YES_NO_CANCEL_OPTION);
                    switch (result){
                        case JOptionPane.YES_OPTION:
                            super.approveSelection();
                            return;
                        case JOptionPane.NO_OPTION:
                            return;
                        case JOptionPane.CLOSED_OPTION:
                            return;
                        case JOptionPane.CANCEL_OPTION:
                            cancelSelection();
                            return;
                    }
                }
                super.approveSelection();
            }
        };
    }

    void writeSpreadSheet(File file) {
        // Load the template file
        final Sheet sheet;
        try {
            File infile = new File("Bestellvorlage.ods");
            sheet = SpreadSheet.createFromFile(infile).getSheet(0);
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        // Get general order data
        Vector<String> bestellung = orderData.get(bestellNummern.indexOf(selBestellNr));
        //int jahr = Integer.parseInt(bestellung.get(1));
        Integer kw = Integer.parseInt(bestellung.get(2));
        String oldDate = bestellung.get(3);
        String newDate = oldDate;
        try {
            // reformat the date to be without hour:
            Date date = new SimpleDateFormat(dateFormatJava).parse(oldDate);
            newDate = new SimpleDateFormat("dd.MM.yyyy").format(date);;
        } catch (java.text.ParseException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        // Change date
        sheet.getCellAt("C2").setValue(newDate);
        // Change KW
        sheet.getCellAt("H2").setValue(kw);

        // Insert order items
        Vector< Vector<Object> > data = retrieveOrderDetailData_forExport(selBestellNr);
        System.out.println("Export data: "+data);
        for (int row=0; row<data.size(); row++){
            for (int col=0; col<data.get(row).size(); col++){
                System.out.println("Setting value at "+(13+row)+","+col+": "+data.get(row).get(col));
                sheet.setValueAt(data.get(row).get(col), col, 13+row);
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
            filterStr = filterField.getText();
            applyFilter();
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

    private void applyFilter() {
        orderDetailDisplayData = new Vector< Vector<Object> >(orderDetailData);
        initiateDisplayIndices();
        if (filterStr.length() == 0){
            return;
        }
        for (int i=0; i<orderDetailData.size(); i++){
            boolean contains = false;
            for (Object obj : orderDetailData.get(i)){
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
                int display_index = orderDetailDisplayIndices.indexOf(i);
                orderDetailDisplayData.remove(display_index);
                orderDetailDisplayIndices.remove(display_index);
            }
        }
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == editButton){
            if (selBestellNr > 0){
                if (!tabbedPane.bestellenTableIsEmpty()){
                    int answer = JOptionPane.showConfirmDialog(this,
                            "Achtung: Bestellen-Tab enthält bereits eine Bestellung.\nDaten gehen verloren. Fortfahren?",
                            "Warnung",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (answer == JOptionPane.YES_OPTION){
                        // continue (below)
                    } else {
                        return;
                    }
                }
                deleteOrderFromDB(selBestellNr);
                Vector<String> bestellung = orderData.get(bestellNummern.indexOf(selBestellNr));
                int jahr = Integer.parseInt(bestellung.get(1));
                int kw = Integer.parseInt(bestellung.get(2));
                // put order data into Bestellen tab
                tabbedPane.setBestellenTable(selBestellNr, jahr, kw, orderDetailArtikelIDs, orderDetailData);
                // clear the BestellAnzeige
                updateAll();
                // switch to Bestellen tab
                tabbedPane.switchToBestellen();
            }
	    return;
	}
        if (e.getSource() == exportButton){
            int returnVal = fc.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION){
                File file = fc.getSelectedFile();

                writeSpreadSheet(file);

                System.out.println("Written to " + file.getName());
            } else {
                System.out.println("Open command cancelled by user.");
            }
            return;
	}
    }
}
