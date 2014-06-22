package Weltladenbesteller;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.math.RoundingMode;

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
import javax.swing.table.*;

import WeltladenDB.MainWindowGrundlage;
import WeltladenDB.ArtikelGrundlage;
import WeltladenDB.AnyJComponentJTable;
import WeltladenDB.JComponentCellRenderer;
import WeltladenDB.JComponentCellEditor;

public class BestellAnzeige extends ArtikelGrundlage {
    // Attribute:
    protected int bestellungenProSeite = 25;
    protected int currentPage = 1;
    protected int totalPage;
    protected String bestellungsZahl;
    protected int bestellungsZahlInt;

    protected String filterStr; // show only specific items of the order

    protected JButton prevButton;
    protected JButton nextButton;
    protected JButton editButton; // click the button to edit the order (in the Bestellen tab)
    protected JTextField filterField;

    private int selBestellNr;
    private Vector<Integer> bestellNummern;
    protected Vector< Vector<String> > orderData;
    protected Vector<String> orderLabels;
    protected Vector< Vector<String> > orderDetailData;
    protected Vector<String> orderDetailLabels;

    private JSplitPane splitPane;
    private JPanel orderPanel;
    private JPanel orderDetailPanel;
    protected AnyJComponentJTable orderTable;
    protected BestellungsTable orderDetailTable;

    // Methoden:

    /**
     *    The constructor.
     *       */
    public BestellAnzeige(Connection conn, MainWindowGrundlage mw)
    {
	super(conn, mw);
        selBestellNr = -1;
        showAll();
    }

    void showAll() {
        orderPanel = new JPanel();
        orderDetailPanel = new JPanel();
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                orderPanel,
                orderDetailPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.3);
        this.add(splitPane);
        showTables();
    }

    void showTables() {
        showOrderTable();
        showOrderDetailTable(selBestellNr);
    }

    void showOrderTable() {
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
                int realRowIndex = convertRowIndexToModel(rowIndex); // user might have changed row order
                int realColIndex = convertColumnIndexToModel(colIndex); // user might have changed column order
                String tip = "";
                tip = this.getModel().getValueAt(realRowIndex, realColIndex).toString();
                return tip;
            }
            // Implement table header tool tips.
            @Override
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        Point p = e.getPoint();
                        int colIndex = columnAtPoint(p);
                        int realColIndex = convertColumnIndexToModel(colIndex); // user might have changed column order
                        tip = getColumnName(realColIndex);
                        return tip;
                    }
                };
            }
        };

        JScrollPane scrollPane = new JScrollPane(orderTable);
        orderPanel.add(scrollPane);
    }

    public void showOrderDetailTable(int bestellNr) {
        selBestellNr = bestellNr;
        // XXX CONTINUE HERE!!!
    }

    private void updateAll(){
	this.remove(splitPane);
	this.revalidate();
        selBestellNr = -1;
	showAll();
    }

    void retrieveOrderData() {
        orderData = new Vector< Vector<String> >();
        bestellNummern = new Vector<Integer>();
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT bestell_nr, jahr, kw, bestell_datum FROM bestellung " +
		    "ORDER BY bestell_nr DESC LIMIT " + (currentPage-1)*bestellungenProSeite + "," + bestellungenProSeite
                    );
            // Now do something with the ResultSet, should be only one result ...
            while ( rs.next() ){
                bestellNummern.add(rs.getInt(1));
                Vector<String> row = new Vector<String>();
                System.out.println(rs.getString(2));
                row.add(rs.getString(1));
                row.add(rs.getString(2));
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

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
    }
}
