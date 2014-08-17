package Weltladenbesteller;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.math.RoundingMode;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// OpenDocument stuff:
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

import WeltladenDB.MainWindowGrundlage;
import WeltladenDB.ArtikelGrundlage;
import WeltladenDB.AnyJComponentJTable;
import WeltladenDB.JComponentCellRenderer;
import WeltladenDB.JComponentCellEditor;

public class BestellAnzeige extends BestellungsGrundlage {
    // Attribute:
    protected int bestellungenProSeite = 25;
    protected int currentPage = 1;
    protected int totalPage;
    protected String bestellungsZahl;
    protected int bestellungsZahlInt;

    protected String filterStr; // show only specific items of the order

    private TabbedPane tabbedPane;

    private JFileChooser fc;
    protected JButton prevButton;
    protected JButton nextButton;
    protected JButton exportButton; // click the button to print the order
    protected JButton editButton; // click the button to edit the order (in the Bestellen tab)
    protected JTextField filterField;

    protected int selBestellNr;
    protected Vector<Integer> bestellNummern;
    protected Vector< Vector<String> > orderData;
    protected Vector<String> orderLabels;
    protected Vector< Vector<Object> > orderDetailData;
    protected Vector<Integer> orderDetailArtikelIDs;

    private JSplitPane splitPane;
    private JPanel orderPanel;
    private JPanel orderDetailPanel;
    protected AnyJComponentJTable orderTable;
    protected BestellungsTable orderDetailTable;

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
            JPanel tablePanel = new JPanel();
            tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));

                // Header
                JLabel headerLabel = new JLabel("Details der Bestellung:");
                headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                tablePanel.add(headerLabel);

                // Table with general order data:
                Vector<String> bestellung = orderData.get(bestellNummern.indexOf(bestellNr));
                Vector< Vector<String> > bestellData = new Vector< Vector<String> >();
                bestellData.add(bestellung);
                JTable bestellTable = new JTable(bestellData, orderLabels);
                JScrollPane sp1 = new JScrollPane(bestellTable);
                sp1.setPreferredSize(new Dimension((int)sp1.getPreferredSize().getWidth(), 40));
                tablePanel.add(sp1);

                // Table with order details:
                retrieveOrderDetailData(bestellNr);
                orderDetailTable = new BestellungsTable(orderDetailData, columnLabels);
                setTableProperties(orderDetailTable);

                JScrollPane sp2 = new JScrollPane(orderDetailTable);
                tablePanel.add(sp2);

            orderDetailPanel.add(tablePanel, BorderLayout.CENTER);

            // Panel for buttons
            JPanel buttonPanel = new JPanel(new FlowLayout());
            exportButton = new JButton("Exportieren");
            editButton = new JButton("Bearbeiten");
	    exportButton.addActionListener(this);
	    editButton.addActionListener(this);
            buttonPanel.add(exportButton);
            buttonPanel.add(editButton);
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
/*
        // Load the file.
        File file = new File("/resources/Bestellvorlage.ods");
        final Sheet sheet = SpreadSheet.createFromFile(file).getSheet(0);
        // Change date.
        sheet.getCellAt("I10").setValue(new Date());
        // Change strings.
        sheet.setValueAt("Filling test", 1, 1);
        sheet.getCellAt("B27").setValue("On site support");
        // Change number.
        sheet.getCellAt("F24").setValue(3);
        // Or better yet use a named range
        // (relative to the first cell of the range, wherever it might be).
        sheet.getSpreadSheet().getTableModel("Products").setValueAt(1, 5, 4);
        // Save to file and open it.
        File outputFile = new File("fillingTest.ods");
        OOUtils.open(sheet.getSpreadSheet().saveAs(outputFile));
*/

        // Get the data to save (create a copy)
        Vector< Vector<Object> > data = new Vector< Vector<Object> >(orderDetailData);
        // Convert columns as needed
        int index = columnLabels.indexOf("Einzelpreis");
        for (int row=0; row<data.size(); row++){
            String value = data.get(row).get(index).toString();
            value = value.replace(',','.').replace(currencySymbol, "").trim();
            BigDecimal newValue = new BigDecimal(value);
            data.get(row).set(index, newValue);
        }
        index = columnLabels.indexOf("VPE");
        for (int row=0; row<data.size(); row++){
            String value = (String) data.get(row).get(index);
            Integer newValue = null;
            if (value != null){
                newValue = Integer.parseInt(value);
            }
            data.get(row).set(index, newValue);
        }
        index = columnLabels.indexOf("Stückzahl");
        for (int row=0; row<data.size(); row++){
            String value = data.get(row).get(index).toString();
            Integer newValue = Integer.parseInt(value);
            data.get(row).set(index, newValue);
        }
        TableModel model = new DefaultTableModel(data, columnLabels);

        try {
            // Save the data to an ODS file and open it.
            SpreadSheet.createEmpty(model).saveAs(file);
            OOUtils.open(file);
        } catch (FileNotFoundException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
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
