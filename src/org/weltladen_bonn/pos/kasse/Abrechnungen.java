package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.ParseException;

// OpenDocument stuff:
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.jopendocument.dom.OOUtils;

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
import javax.swing.filechooser.FileNameExtensionFilter;

import org.weltladen_bonn.pos.WindowContent;
import org.weltladen_bonn.pos.MainWindowGrundlage;
import org.weltladen_bonn.pos.AnyJComponentJTable;
import org.weltladen_bonn.pos.FileExistsAwareFileChooser;

public abstract class Abrechnungen extends WindowContent {
    // Attribute:
    private int abrechnungenProSeite = 7;
    int currentPage = 1;
    int totalPage;

    protected String filterStr;
    private String titleStr;
    String dateInFormat;
    private String dateOutFormat;
    private String exportDirFormat;
    private String timeName;
    private String abrechnungsTableName;

    // The bottom panel which holds button.
    protected JPanel allPanel;
    protected JPanel headerPanel;
    //protected JPanel footerPanel;
    // The table holding the invoices. This is "anonymously subclassed" and two method are overridden
    protected AbrechnungsTable myTable;

    JButton prevButton;
    JButton nextButton;
    private Vector<JButton> exportButtons;
    private Vector<String> abrechnungsDates;
    Vector<Integer> abrechnungsIDs;
    private Vector< Vector<BigDecimal> > abrechnungsTotals;
    protected Vector< HashMap<BigDecimal, Vector<BigDecimal>> > abrechnungsVATs;
    String incompleteAbrechnungsDate;
    Vector<BigDecimal> incompleteAbrechnungsTotals;
    HashMap<BigDecimal, Vector<BigDecimal>> incompleteAbrechnungsVATs;
    TreeSet<BigDecimal> mwstSet;
    protected Vector< Vector<Object> > data;
    protected Vector< Vector<Color> > colors;
    Vector< Vector<String> > fontStyles;
    protected Vector<String> columnLabels;
    private int abrechnungsZahl;
    private FileExistsAwareFileChooser odsChooser;

    // Methoden:

    /**
     *    The constructor.
     *       */
    public Abrechnungen(MariaDbPoolDataSource pool, MainWindowGrundlage mw, String fs, String ts, String dif, String dof,
                        String tn, String atn)
    {
        super(pool, mw);
        filterStr = fs;
        titleStr = ts;
        dateInFormat = dif;
        dateOutFormat = dof;
        timeName = tn;
        abrechnungsTableName = atn;

        //footerPanel = new JPanel();
        //footerPanel.setLayout(new FlowLayout());
        //this.add(footerPanel, BorderLayout.SOUTH);

        fillDataArray();

        odsChooser = new FileExistsAwareFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "ODS Spreadsheet-Dokumente", "ods");
        odsChooser.setFileFilter(filter);
    }

    public void setExportDirFormat(String exportDirFormat) {
        this.exportDirFormat = exportDirFormat;
    }

    boolean isThereIncompleteAbrechnung() {
        BigDecimal totalBrutto = incompleteAbrechnungsTotals.get(0);
        return totalBrutto.signum() != 0;
    }

    Integer id() {
        Integer id = -1;
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT IFNULL("+
                    "(SELECT MAX(id) FROM "+abrechnungsTableName+"), "+
                    "0)");
            rs.next(); id = rs.getInt(1)+1; rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return id;
    }

    abstract void queryIncompleteAbrechnung();

    abstract void queryAbrechnungenSpecial();

    // since all Abrechnungen need to include the incomplete Tagesabrechnung, include code here to share
    protected Vector<BigDecimal> queryIncompleteAbrechnungTag_Totals() {
        Vector<BigDecimal> values = new Vector<>();
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            // for filling the diplayed table:

            // first, get the totals:
            // Gesamt Brutto
            ResultSet rs = stmt.executeQuery(
                    "SELECT SUM(ges_preis) AS ges_brutto " +
                    "FROM verkauf_details INNER JOIN verkauf USING (rechnungs_nr) " +
                    "WHERE storniert = FALSE AND verkaufsdatum > " +
                    "IFNULL((SELECT MAX(zeitpunkt_real) FROM abrechnung_tag),'0001-01-01') "
                    );
            rs.next();
                BigDecimal tagesGesamtBrutto =
                    new BigDecimal(rs.getString(1) == null ? "0" : rs.getString(1));
            rs.close();
            // Gesamt Bar Brutto
            rs = stmt.executeQuery(
                    "SELECT SUM(ges_preis) AS ges_bar_brutto " +
                    "FROM verkauf_details INNER JOIN verkauf USING (rechnungs_nr) " +
                    "WHERE storniert = FALSE AND verkaufsdatum > " +
                    "IFNULL((SELECT MAX(zeitpunkt_real) FROM abrechnung_tag),'0001-01-01') AND ec_zahlung = FALSE "
                    );
            rs.next();
                BigDecimal tagesGesamtBarBrutto =
                    new BigDecimal(rs.getString(1) == null ?  "0" : rs.getString(1));
            rs.close();
            // Gesamt EC Brutto
            BigDecimal tagesGesamtECBrutto = tagesGesamtBrutto.subtract(tagesGesamtBarBrutto);

            values.add(tagesGesamtBrutto);
            values.add(tagesGesamtBarBrutto);
            values.add(tagesGesamtECBrutto);

            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return values;
    }
    ///////////
    protected HashMap<BigDecimal, Vector<BigDecimal>> queryIncompleteAbrechnungTag_VATs() {
        HashMap<BigDecimal, Vector<BigDecimal>> map = new HashMap<>();
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                // OLD: SUM OF ROUND in MySQL (MySQL's round is ROUND_HALF_UP
                // as in Java (away from zero), "kaufmännisches Runden", see
                // http://dev.mysql.com/doc/refman/5.0/en/precision-math-rounding.html,
                // https://en.wikipedia.org/wiki/Rounding,
                // https://de.wikipedia.org/wiki/Rundung)
                    //"SELECT mwst_satz, SUM( ROUND(ges_preis / (1.+mwst_satz), 2) ) AS mwst_netto, " +
                    //"SUM( ROUND(ges_preis / (1. + mwst_satz) * mwst_satz, 2) ) AS mwst_betrag " +
                    //"FROM verkauf_details INNER JOIN verkauf USING (rechnungs_nr) " +
                    //"WHERE storniert = FALSE AND verkaufsdatum > " +
                    //"IFNULL((SELECT MAX(zeitpunkt) FROM abrechnung_tag),'0001-01-01') " +
                    //"GROUP BY mwst_satz"
                // NEW: ROUND OF SUM
                    //"SELECT mwst_satz, SUM( ges_preis / (1.+mwst_satz) ) AS mwst_netto, " +
                    //"SUM( ges_preis / (1. + mwst_satz) * mwst_satz ) AS mwst_betrag " +
                    //"FROM verkauf_details INNER JOIN verkauf USING (rechnungs_nr) " +
                    //"WHERE storniert = FALSE AND verkaufsdatum > " +
                    //"IFNULL((SELECT MAX(zeitpunkt) FROM abrechnung_tag),'0001-01-01') " +
                    //"GROUP BY mwst_satz"
                // NEWER: Da für jede Rechnung einzeln summiert (und dann gerundet) werden müsste,
                // ist es einfacher (und auch sicherer), die gerundete MwSt.-Information separat für jede
                // Rechnung zu speichern (in Tabelle `verkauf_mwst`) und nur noch darüber zu summieren
                    "SELECT mwst_satz, SUM(mwst_netto), SUM(mwst_betrag) "+
                    "FROM verkauf_mwst INNER JOIN verkauf USING (rechnungs_nr) "+
                    "WHERE storniert = FALSE AND verkaufsdatum > "+
                    "IFNULL((SELECT MAX(zeitpunkt_real) FROM abrechnung_tag), '0001-01-01') " +
                    "GROUP BY mwst_satz"
                    );
            while (rs.next()) {
                //System.out.println(rs.getBigDecimal(1));
                BigDecimal mwst_satz = rs.getBigDecimal(1);
            // OLD: SUM OF ROUND (see above, rounding in MySQL)
                //BigDecimal mwst_netto = rs.getBigDecimal(2);
                //BigDecimal mwst_betrag = rs.getBigDecimal(3);
            // NEW: ROUND OF SUM (rounding in Java)
                //BigDecimal mwst_netto = new BigDecimal( bc.priceFormatterIntern(rs.getBigDecimal(2)) );
                //BigDecimal mwst_betrag = new BigDecimal( bc.priceFormatterIntern(rs.getBigDecimal(3)) );
            // NEWER: Rounding was already done when Rechnung was saved, not necessary here
                BigDecimal mwst_netto = rs.getBigDecimal(2);
                BigDecimal mwst_betrag = rs.getBigDecimal(3);
                Vector<BigDecimal> values = new Vector<>();
                values.add( mwst_netto.add(mwst_betrag) ); // = brutto
                values.add(mwst_netto);
                values.add(mwst_betrag);
                map.put(mwst_satz, values);
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return map;
    }

    void queryAbrechnungen() {
        queryAbrechnungenSpecial();

        abrechnungsDates = new Vector<>();
        abrechnungsIDs = new Vector<>();
        abrechnungsTotals = new Vector<>();
        abrechnungsVATs = new Vector<>();
        mwstSet = new TreeSet<>();

        if (this.currentPage == 1){
            queryIncompleteAbrechnung();
        }

        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            // first, derive the limits of the real query from the number of rows that belong to
            // the desired abrechnung range:
            int offset = ((currentPage-1)*abrechnungenProSeite-1); // "-1" because of one red column on 1st page
            offset = offset < 0 ? 0 : offset;
            int noOfColumns = currentPage > 1 ? abrechnungenProSeite : abrechnungenProSeite-1; // "-1" on first page only (because red column needs space too)

            // second, get the total amounts
            String query = "SELECT id, "+timeName+", SUM(mwst_netto + mwst_betrag), "+
                    // ^^^ Gesamt Brutto
                    "SUM(bar_brutto), SUM(mwst_netto + mwst_betrag) - SUM(bar_brutto) "+
                    // ^^^ Gesamt Bar Brutto      ^^^ Gesamt EC Brutto = Ges. Brutto - Ges. Bar Brutto
                    "FROM "+abrechnungsTableName+" "+
                    "WHERE TRUE "+
                    filterStr +
                    "GROUP BY id, "+timeName+" ORDER BY id DESC "+
                    "LIMIT " + offset + "," + noOfColumns;
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                String date = rs.getString(2);
                Vector<BigDecimal> values = new Vector<>();
                values.add(rs.getBigDecimal(3));
                values.add(rs.getBigDecimal(4));
                values.add(rs.getBigDecimal(5));
                // store in vectors
                abrechnungsIDs.add(rs.getInt(1));
                abrechnungsDates.add(date);
                abrechnungsTotals.add(values);
                abrechnungsVATs.add(new HashMap<>());
            }
            rs.close();

            // third, get the actual abrechnungen (for each date):
            for (String date : abrechnungsDates){
                PreparedStatement pstmt = connection.prepareStatement(
                        "SELECT mwst_satz, mwst_netto, mwst_betrag " +
                        "FROM "+abrechnungsTableName+" " +
                        "WHERE "+timeName+" = ? " +
                        filterStr +
                        "ORDER BY mwst_satz "
                        );
                pstmt.setString(1, date);
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    BigDecimal mwst = rs.getBigDecimal(1);
                    Vector<BigDecimal> values = new Vector<BigDecimal>();
                    values.add( rs.getBigDecimal(2).add(rs.getBigDecimal(3)) ); // brutto: sum of the two
                    values.add(rs.getBigDecimal(2)); // netto
                    values.add(rs.getBigDecimal(3)); // betrag (amount)
                    // store the values at the positions that match abrechnungsDates and abrechnungsTotals:
                    int index = abrechnungsDates.indexOf(date);
                    abrechnungsVATs.get(index).put(mwst, values);
                    mwstSet.add(mwst);
                }
                rs.close();
            }

            // fourth, get total number of abrechnungen:
            rs = stmt.executeQuery(
                    "SELECT COUNT(DISTINCT id) FROM "+abrechnungsTableName+" " +
                    "WHERE TRUE " +
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

    String formatDate(String date, String dateFormat) {
        SimpleDateFormat sdfIn = new SimpleDateFormat(this.dateInFormat);
        SimpleDateFormat sdfOut = new SimpleDateFormat(dateFormat);
        String formattedDate = "";
        try {
            formattedDate = sdfOut.format( sdfIn.parse(date) );
        } catch (ParseException ex) {
            System.out.println("ParseException: " + ex.getMessage());
            ex.printStackTrace();
        }
        return formattedDate;
    }

    Date createDate(String date) {
        SimpleDateFormat sdfIn = new SimpleDateFormat(this.dateInFormat);
        Date d = null;
        try {
            d = sdfIn.parse(date);
        } catch (ParseException ex) {
            System.out.println("ParseException: " + ex.getMessage());
            ex.printStackTrace();
        }
        return d;
    }

    String dateForFilename(String date) {
        return date.replaceAll(" ", "_").replaceAll(":", "");
    }

    void fillHeaderColumn() {
        // fill header column
        columnLabels.add("");
        data.add(new Vector<>()); data.lastElement().add("Gesamt Brutto");
        colors.add(new Vector<>()); colors.lastElement().add(Color.BLACK);
        fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold");
        data.add(new Vector<>()); data.lastElement().add("Gesamt Bar Brutto");
        colors.add(new Vector<>()); colors.lastElement().add(Color.BLACK);
        fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold");
        data.add(new Vector<>()); data.lastElement().add("Gesamt EC Brutto");
        colors.add(new Vector<>()); colors.lastElement().add(Color.BLACK);
        fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold");
        for (BigDecimal mwst : mwstSet){
            data.add(new Vector<>()); data.lastElement().add(bc.vatFormatter(mwst)+" MwSt. Brutto");
            colors.add(new Vector<>()); colors.lastElement().add(Color.BLACK);
            fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold");
            data.add(new Vector<>()); data.lastElement().add(bc.vatFormatter(mwst)+" MwSt. Netto");
            colors.add(new Vector<>()); colors.lastElement().add(Color.BLACK);
            fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold");
            data.add(new Vector<>()); data.lastElement().add(bc.vatFormatter(mwst)+" MwSt. Betrag");
            colors.add(new Vector<>()); colors.lastElement().add(Color.BLACK);
            fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold");
        }
        data.add(new Vector<>()); data.lastElement().add(""); // row for exportButtons
        colors.add(new Vector<>()); colors.lastElement().add(Color.BLACK);
        fontStyles.add(new Vector<>()); fontStyles.lastElement().add("normal");
    }

    int fillDataArrayColumnWithData(String date, Vector<BigDecimal> totals, HashMap<BigDecimal, Vector<BigDecimal>> vats, Color color) {
        String formattedDate = formatDate(date, this.dateOutFormat);
        columnLabels.add(formattedDate);
        // add Gesamt Brutto
        data.get(0).add( bc.priceFormatter( totals.get(0) )+" "+bc.currencySymbol );
        colors.get(0).add(color);
        fontStyles.get(0).add("normal");
        // add Gesamt Bar Brutto
        data.get(1).add( bc.priceFormatter( totals.get(1) )+" "+bc.currencySymbol );
        colors.get(1).add(color);
        fontStyles.get(1).add("normal");
        // add Gesamt EC Brutto
        data.get(2).add( bc.priceFormatter( totals.get(2) )+" "+bc.currencySymbol );
        colors.get(2).add(color);
        fontStyles.get(2).add("normal");
        // add VATs
        int rowIndex = 3;
        for (BigDecimal mwst : mwstSet){
            for (int i=0; i<3; i++){
                if (vats != null && vats.containsKey(mwst)){
                    BigDecimal bd = vats.get(mwst).get(i);
                    data.get(rowIndex).add( bc.priceFormatter(bd)+" "+bc.currencySymbol );
                } else {
                    data.get(rowIndex).add( bc.priceFormatter("0")+" "+bc.currencySymbol );
                }
                colors.get(rowIndex).add(color);
                fontStyles.get(rowIndex).add("normal");
                rowIndex++;
            }
        }
        return rowIndex;
    }

    int fillIncompleteDataColumn() {
        int rowIndex = fillDataArrayColumnWithData(incompleteAbrechnungsDate, incompleteAbrechnungsTotals, incompleteAbrechnungsVATs, Color.RED);
        data.get(rowIndex).add(""); // instead of exportButton
        colors.get(rowIndex).add(Color.BLACK);
        fontStyles.get(rowIndex).add("normal");
        rowIndex++;
        return rowIndex;
    }

    int fillDataArrayColumn(int colIndex) {
        String date = abrechnungsDates.get(colIndex);
        Vector<BigDecimal> totals = abrechnungsTotals.get(colIndex);
        HashMap<BigDecimal, Vector<BigDecimal>> vats = abrechnungsVATs.get(colIndex); // map with values for each mwst
        int rowIndex = fillDataArrayColumnWithData(date, totals, vats, Color.BLACK);
        rowIndex = addExportButton(rowIndex);
        return rowIndex;
    }

    int addExportButton(int rowIndex) {
        // add export buttons in last row:
        exportButtons.add(new JButton("Exportieren"));
        exportButtons.lastElement().addActionListener(this);
        data.get(rowIndex).add(exportButtons.lastElement());
        colors.get(rowIndex).add(Color.BLACK);
        fontStyles.get(rowIndex).add("normal");
        rowIndex++;
        return rowIndex;
    }

    void fillDataArray(){
        queryAbrechnungen();

        columnLabels = new Vector<>();
        data = new Vector<>();
        colors = new Vector<>();
        fontStyles = new Vector<>();
        exportButtons = new Vector<>();

        fillHeaderColumn();

        if (currentPage == 1){
            // fill red (incomplete, so unsaved) data column
            fillIncompleteDataColumn();
        }

        // fill data columns with black (already saved in DB) abrechnungen
        for (int colIndex=0; colIndex<abrechnungsDates.size(); colIndex++){
            fillDataArrayColumn(colIndex);
        }
        myTable = new AbrechnungsTable(data, columnLabels);
        //	myTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
        //	myTable.setFillsViewportHeight(true);
    }

    abstract void addOtherStuff();

    void showTable(){
        allPanel = new JPanel(new BorderLayout());
        allPanel.setBorder(BorderFactory.createTitledBorder(titleStr));

        headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.PAGE_AXIS));
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
        headerPanel.add(pageChangePanel);

        addOtherStuff();

        allPanel.add(headerPanel, BorderLayout.NORTH);

        setTableProperties(myTable);
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

    protected void setTableProperties(AnyJComponentJTable table) {
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

    Vector<Object> fillSpreadSheet(int exportIndex) {
        // Get data
        String date = abrechnungsDates.get(exportIndex);
        Date ddate = createDate(date);
        System.out.println("ddate: " + ddate);
        Integer id = abrechnungsIDs.get(exportIndex);
        Vector<BigDecimal> totals = abrechnungsTotals.get(exportIndex);
        HashMap<BigDecimal, Vector<BigDecimal>> vats = abrechnungsVATs.get(exportIndex); // map with values for each mwst

        // Load the template file
        final Sheet sheet;
        try {
            String filename = "vorlagen" + bc.fileSep + titleStr + ".ods";
            File infile = new File(filename);
            if (!infile.exists()) {
                JOptionPane.showMessageDialog(this,
                        "Fehler: Zur '" + titleStr + "' gibt es keine Vorlage " +
                                "'" + filename + "'.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            sheet = SpreadSheet.createFromFile(infile).getSheet(0);
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }

        // Change date
        sheet.getCellAt("B7").setValue(ddate);
        // Laufende Nummer:
        sheet.getCellAt("B8").setValue(id);

        // Set Totals:
        Integer rowIndex = 9;
        for (BigDecimal total : totals) {
            sheet.setValueAt(total, 1, rowIndex);
            rowIndex++;
        }
        rowIndex++; // empty row

        // Set VATs:
        for (BigDecimal mwst : mwstSet) {
            if (vats.containsKey(mwst)) {
                sheet.setValueAt(bc.vatFormatter(mwst) + " MwSt. Brutto:", 0, rowIndex);
                sheet.setValueAt(bc.vatFormatter(mwst) + " MwSt. Netto:", 0, rowIndex + 1);
                sheet.setValueAt(bc.vatFormatter(mwst) + " MwSt. Betrag:", 0, rowIndex + 2);
                for (int i = 0; i < 3; i++) {
                    sheet.setValueAt(vats.get(mwst).get(i), 1, rowIndex);
                    rowIndex++;
                }
                rowIndex++; // empty row
            }
        }

        Vector<Object> v = new Vector<>();
        v.add(sheet);
        v.add(rowIndex);
        return v;
    }

    private void writeSpreadSheet(Sheet sheet, File file) {
        try {
            // Save to file and open it.
            OOUtils.open(sheet.getSpreadSheet().saveAs(file));
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void export(int exportIndex){
        String date = abrechnungsDates.get(exportIndex);
        File exportDir = new File(System.getProperty("user.home")+bc.fileSep+formatDate(date, this.exportDirFormat));
        boolean ok = true;
        if (!exportDir.exists()) {
            ok = exportDir.mkdirs();
        }
        if (ok) {
            odsChooser.setCurrentDirectory(exportDir);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Fehler: Ordner für "+titleStr+" unter "+exportDir+" existiert nicht "+
                            "und konnte nicht angelegt werden.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
        odsChooser.setSelectedFile(new File(titleStr+"_WL_Bonn_"+dateForFilename(date)+".ods"));
        int returnVal = odsChooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            File file = odsChooser.getSelectedFile();

            Vector<Object> v = fillSpreadSheet(exportIndex);
            Sheet sheet = (Sheet) v.firstElement();
            writeSpreadSheet(sheet, file);

            System.out.println("Written to " + file.getName());
        } else {
            System.out.println("Save command cancelled by user.");
        }
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        int exportIndex = -1;
        for (int i=0; i<exportButtons.size(); i++){
            if (e.getSource() == exportButtons.get(i) ){
                exportIndex = i;
                System.out.println("exportIndex: "+exportIndex);
                break;
            }
        }
        if (exportIndex > -1){
            export(exportIndex);
        }
    }

    private class AbrechnungsTable extends AnyJComponentJTable {
        AbrechnungsTable(Vector<Vector<Object>> data, Vector<String> columns) {
            super(data, columns);
        }
        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);
            int realColIndex = convertColumnIndexToModel(column); // user might have changed column order
            int realRowIndex = convertRowIndexToModel(row); // user might have changed row order
            // add custom rendering here
            if (fontStyles.get(realRowIndex).get(realColIndex).equals("bold")) {
                c.setFont(c.getFont().deriveFont(Font.BOLD));
            } else if (fontStyles.get(realRowIndex).get(realColIndex).equals("bold-italic")) {
                c.setFont(c.getFont().deriveFont(Font.BOLD | Font.ITALIC));
            }
            c.setForeground(colors.get(realRowIndex).get(realColIndex));
            return c;
        }
    }
}
