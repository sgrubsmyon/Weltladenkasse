package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.math.RoundingMode;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

import java.text.SimpleDateFormat;
import java.text.ParseException;

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

import WeltladenDB.WindowContent;
import WeltladenDB.MainWindowGrundlage;
import WeltladenDB.AnyJComponentJTable;
import WeltladenDB.JComponentCellRenderer;
import WeltladenDB.JComponentCellEditor;

public abstract class Abrechnungen extends WindowContent {
    // Attribute:
    protected int abrechnungenProSeite = 6;
    protected int currentPage = 1;
    protected int totalPage;

    protected String filterStr;
    protected String titleStr;
    protected String dateInFormat;
    protected String dateOutFormat;
    protected String timeName;
    protected String abrechnungTableName;

    // The bottom panel which holds button.
    protected JPanel bottomPanel;
    protected JPanel tablePanel;
    // The table holding the invoices. This is "anonymously subclassed" and two method are overridden
    protected AbrechnungsTable myTable;

    protected JButton freiButton = new JButton("Frei");
    protected JButton prevButton;
    protected JButton nextButton;

    protected TreeMap< String, HashMap<BigDecimal, Vector<BigDecimal>> > abrechnungsMap;
    protected TreeMap< String, Vector<BigDecimal> > totalsMap;
    protected TreeSet<BigDecimal> mwstSet;
    protected Vector< Vector<String> > data;
    protected Vector<String> columnLabels;
    protected int abrechnungsZahl;

    // Methoden:

    /**
     *    The constructor.
     *       */
    public Abrechnungen(Connection conn, MainWindowGrundlage mw, String fs, String ts, String dif, String dof, String tn, String atn)
    {
	super(conn, mw);
        filterStr = fs;
        titleStr = ts;
        dateInFormat = dif;
        dateOutFormat = dof;
        timeName = tn;
        abrechnungTableName = atn;

	bottomPanel = new JPanel();
	bottomPanel.setLayout(new FlowLayout());

	this.add(bottomPanel, BorderLayout.SOUTH);

	fillDataArray();
    }

    abstract void queryIncompleteAbrechnung();

    abstract void queryAbrechnungenSpecial();

    void queryAbrechnungen() {
        queryAbrechnungenSpecial();

        abrechnungsMap = new TreeMap< String, HashMap<BigDecimal, Vector<BigDecimal>> >();
        mwstSet = new TreeSet<BigDecimal>();
        totalsMap = new TreeMap< String, Vector<BigDecimal> >();

        if (currentPage == 1){
            queryIncompleteAbrechnung();
        }

        try {
            Statement stmt = this.conn.createStatement();
            // first, derive the limits of the real query from the number of lines that belong to
            // the desired abrechnung range:
            int offset = ((currentPage-1)*abrechnungenProSeite-1); // "-1" because of one red column
            offset = offset < 0 ? 0 : offset;
            int noOfColumns = currentPage > 1 ? abrechnungenProSeite : abrechnungenProSeite-1; // "-1" on first page only (because red column needs space too)
            ResultSet rs = stmt.executeQuery(
                    "SELECT SUM(count) FROM " +
                    "(SELECT COUNT("+timeName+") AS count FROM "+abrechnungTableName+" GROUP BY "+timeName+" " +
                    "ORDER BY "+timeName+" DESC "+
                    "LIMIT 0," + offset + ") AS t"
                    );
            rs.next(); int lowerLimit = rs.getInt(1); rs.close();
            System.out.println("lowerLimit: "+lowerLimit);
            rs = stmt.executeQuery(
                    "SELECT SUM(count) FROM " +
                    "(SELECT COUNT("+timeName+") AS count FROM "+abrechnungTableName+" GROUP BY "+timeName+" " +
                    "ORDER BY "+timeName+" DESC "+
                    "LIMIT " + offset + "," + noOfColumns + ") AS t"
                    );
            rs.next(); int upperLimit = rs.getInt(1); rs.close();
            //System.out.println("Found limits: "+lowerLimit+" , "+upperLimit);
            // second, get the total amounts
            rs = stmt.executeQuery(
                    "SELECT "+timeName+", SUM(mwst_netto + mwst_betrag), " +
                                       // ^^^ Gesamt Brutto
                    "SUM(bar_brutto), SUM(mwst_netto + mwst_betrag) - SUM(bar_brutto) " +
                  // ^^^ Gesamt Bar Brutto      ^^^ Gesamt EC Brutto = Ges. Brutto - Ges. Bar Brutto
                    "FROM "+abrechnungTableName+" " +
                    filterStr +
                    "GROUP BY "+timeName+" ORDER BY "+timeName+" DESC " +
                    "LIMIT " + offset + "," + noOfColumns
                    );
            while (rs.next()) {
                String date = rs.getString(1);
                Vector<BigDecimal> values = new Vector<BigDecimal>();
                values.add(rs.getBigDecimal(2));
                values.add(rs.getBigDecimal(3));
                values.add(rs.getBigDecimal(4));
                // store in map under date
                totalsMap.put(date, values);
            }
            rs.close();
            // third, get the actual abrechnungen:
            rs = stmt.executeQuery(
                    "SELECT "+timeName+", mwst_satz, mwst_netto, mwst_betrag " +
                    " FROM "+abrechnungTableName+" " +
                    filterStr +
                    "ORDER BY "+timeName+" DESC, mwst_satz " +
                    "LIMIT " + lowerLimit + "," + upperLimit
                    );
            while (rs.next()) {
                String date = rs.getString(1);
                BigDecimal mwst = rs.getBigDecimal(2);
                Vector<BigDecimal> values = new Vector<BigDecimal>();
                values.add(rs.getBigDecimal(3));
                values.add(rs.getBigDecimal(4));
                if ( abrechnungsMap.containsKey(date) ){ // Abrechnung already exists, only add information
                    abrechnungsMap.get(date).put(mwst, values);
                } else { // start new Abrechnung
                    HashMap<BigDecimal, Vector<BigDecimal>> abrechnung =
                        new HashMap<BigDecimal, Vector<BigDecimal>>();
                    abrechnung.put(mwst, values);
                    abrechnungsMap.put(date, abrechnung);
                }
                mwstSet.add(mwst);
            }
            rs.close();
            // fourth, get total number of abrechnungen:
            rs = stmt.executeQuery(
                    "SELECT COUNT(DISTINCT "+timeName+") FROM "+abrechnungTableName+" " +
                    filterStr
                    );
            rs.next();
            abrechnungsZahl = rs.getInt(1) + 1;
            totalPage = (abrechnungsZahl-1)/abrechnungenProSeite + 1;
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    void fillDataArray(){
        queryAbrechnungen();
        columnLabels = new Vector<String>();
        data = new Vector< Vector<String> >();
        columnLabels.add("");
        // fill header column
        data.add(new Vector<String>()); data.lastElement().add("Gesamt Brutto");
        data.add(new Vector<String>()); data.lastElement().add("Gesamt Bar Brutto");
        data.add(new Vector<String>()); data.lastElement().add("Gesamt EC Brutto");
        for (BigDecimal mwst : mwstSet){
            data.add(new Vector<String>()); data.lastElement().add(vatFormatter(mwst)+" MwSt. Netto");
            data.add(new Vector<String>()); data.lastElement().add(vatFormatter(mwst)+" MwSt. Betrag");
        }
        // fill data columns
        int count = 0;
        for ( Map.Entry< String, HashMap<BigDecimal, Vector<BigDecimal>> > entry : abrechnungsMap.descendingMap().entrySet() ){
            String date = entry.getKey();
            SimpleDateFormat sdfIn = new SimpleDateFormat(dateInFormat);
            SimpleDateFormat sdfOut = new SimpleDateFormat(dateOutFormat);
            String formattedDate = "";
            try {
                formattedDate = sdfOut.format( sdfIn.parse(date) );
            } catch (ParseException ex) {
                System.out.println("ParseException: " + ex.getMessage());
                ex.printStackTrace();
            }
            //if (currentPage == 1 && count == 0) formattedDate = "Jetzt (neu)";
            count++;
            columnLabels.add(formattedDate);
            System.out.println(date);
            System.out.println(entry.getValue());
            System.out.println(totalsMap.get(date));
            // add Gesamt Brutto
            data.get(0).add( priceFormatter( totalsMap.get(date).get(0) )+" "+currencySymbol );
            // add Gesamt Bar Brutto
            data.get(1).add( priceFormatter( totalsMap.get(date).get(1) )+" "+currencySymbol );
            // add Gesamt EC Brutto
            data.get(2).add( priceFormatter( totalsMap.get(date).get(2) )+" "+currencySymbol );
            HashMap<BigDecimal, Vector<BigDecimal>> valueMap = entry.getValue(); // map with values for each mwst
            int dataIndex = 3;
            for (BigDecimal mwst : mwstSet){
                for (int i=0; i<2; i++){
                    if (valueMap.containsKey(mwst)){
                        BigDecimal bd = valueMap.get(mwst).get(i);
                        data.get(dataIndex).add( priceFormatter(bd)+" "+currencySymbol );
                    } else {
                        data.get(dataIndex).add("");
                    }
                    dataIndex++;
                }
            }
        }
        myTable = new AbrechnungsTable(data, columnLabels);
        //	myTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
        //	myTable.setFillsViewportHeight(true);
    }

    abstract void addOtherStuff();

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
        int currentPageMin = (currentPage-1)*abrechnungenProSeite + 1;
        int currentPageMax = abrechnungenProSeite*currentPage;
        currentPageMax = (currentPageMax <= abrechnungsZahl) ? currentPageMax : abrechnungsZahl;
        JLabel header = new JLabel("Seite "+ currentPage +" von "+ totalPage + ", Abrechnungen "+
                currentPageMin + " bis "+ currentPageMax +" von "+ abrechnungsZahl);
        pageChangePanel.add(header);
        tablePanel.add(pageChangePanel);

        addOtherStuff();

        myTable.setDefaultRenderer( JComponent.class, new JComponentCellRenderer() );
        myTable.setDefaultEditor( JComponent.class, new JComponentCellEditor() );
        //	myTable.setBounds(71,53,150,100);
        //	myTable.setToolTipText("Tabelle kann nur gelesen werden.");
        setTableProperties(myTable);
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

    protected class AbrechnungsTable extends AnyJComponentJTable {
        public AbrechnungsTable(Vector< Vector<String> > data, Vector<String> columns) {
            super(data, columns);
        }
        @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                int realColIndex = convertColumnIndexToModel(column); // user might have changed column order
                // add custom rendering here
                if (realColIndex == 0){
                    c.setFont( c.getFont().deriveFont(Font.BOLD) );
                }
                if (currentPage == 1 && realColIndex == 1){
                    c.setForeground(Color.red);
                }
                else {
                    c.setForeground(Color.black);
                }
                //c.setBackground(Color.LIGHT_GRAY);
                return c;
            }
            // Implement table cell tool tips.
        @Override
            public String getToolTipText(MouseEvent e) {
                Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                int realRowIndex = convertRowIndexToModel(rowIndex); // user might have changed row order
                int realColIndex = convertColumnIndexToModel(colIndex); // user might have changed column order
                String tip = this.getModel().getValueAt(realRowIndex, realColIndex).toString();
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
                        tip = columnLabels.get(realColIndex);
                        return tip;
                    }
                };
            }
    }

    protected void setTableProperties(AbrechnungsTable table) {
	// Spalteneigenschaften:
        for (int i=0; i<table.getColumnCount(); i++){
            TableColumn column = table.getColumnModel().getColumn(i);
            if (i==0){
                column.setPreferredWidth(20);
            } else {
                column.setCellRenderer(rechtsAusrichter);
                column.setPreferredWidth(10);
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
    }
}
