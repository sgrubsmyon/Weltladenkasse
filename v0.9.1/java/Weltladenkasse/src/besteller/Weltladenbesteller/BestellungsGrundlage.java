package Weltladenbesteller;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.math.RoundingMode;

// GUI stuff:
import java.awt.Component;
import java.awt.Font;
import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.*;
import java.awt.event.MouseEvent;
import java.awt.Point;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

import WeltladenDB.MainWindowGrundlage;
import WeltladenDB.ArtikelGrundlage;
import WeltladenDB.AnyJComponentJTable;

public abstract class BestellungsGrundlage extends ArtikelGrundlage {

    protected JTextField totalPriceField;

    protected Vector<String> preise;
    protected Vector<String> mwsts;
    protected Vector<String> colors;
    protected Vector<String> types;

    protected Vector<String> columnLabels;

    // Die Ausrichter:
    protected final String einrueckung = "      ";

    /**
     *    The constructor.
     *       */
    public BestellungsGrundlage(Connection conn, MainWindowGrundlage mw)
    {
	super(conn, mw);
        initiateVectors();
    }
    private void initiateVectors() {
	columnLabels = new Vector<String>();
	columnLabels.add("Lieferant"); columnLabels.add("Artikel-Nr."); columnLabels.add("Artikel-Name"); 
        columnLabels.add("Einzelpreis"); columnLabels.add("VPE"); columnLabels.add("Stückzahl");
        preise = new Vector<String>();
        mwsts = new Vector<String>();
        colors = new Vector<String>();
        types = new Vector<String>();
    }

    //////////////////////////////////
    // DB query functions:
    //////////////////////////////////
    private HashMap<Integer, String> retrieveVATs() {
        HashMap<Integer, String> vatMap = new HashMap<Integer, String>();
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT mwst_id, mwst_satz FROM mwst"
                    );
	    while (rs.next()) {  
                vatMap.put(rs.getInt(1), rs.getString(2));
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return vatMap;
    }

    //////////////////////////////////
    // price calculation functions:
    //////////////////////////////////
    protected String calculateTotalPrice() {
        BigDecimal totalPrice = new BigDecimal(0);
        for ( String preisString : preise ){
            BigDecimal preis = new BigDecimal(preisString);
            totalPrice = totalPrice.add(preis);
        }
        return priceFormatter(totalPrice);
    }

    protected String calculateTotalVAT(String vat) {
        BigDecimal priceForVAT = new BigDecimal(0);
        for (int i=0; i<mwsts.size(); i++){ 
            String mwstString = mwsts.get(i);
            if ( mwstString.equals(vat) ){
                BigDecimal preis = new BigDecimal( preise.get(i) );
                priceForVAT = priceForVAT.add(preis);
            }
        }
        BigDecimal totalVAT = calculateVAT(priceForVAT, new BigDecimal(vat)); 
        return priceFormatter(totalVAT);
    }

    protected JPanel createTotalPricePanel() {
        JPanel totalPricePanel = new JPanel();
        totalPricePanel.setLayout(new FlowLayout());
        JLabel totalPriceLabel = new JLabel("Ges. Preis: ");
        totalPricePanel.add(totalPriceLabel);
        totalPriceField = new JTextField(calculateTotalPrice()+" "+currencySymbol);
        totalPriceField.setEditable(false);
        totalPriceField.setColumns(7);
        totalPriceField.setHorizontalAlignment(JTextField.RIGHT);
        totalPricePanel.add(totalPriceField);

        totalPricePanel.add(new JLabel("   inkl.: "));
        HashMap<Integer, String> vatMap = retrieveVATs();
        for ( Map.Entry<Integer, String> v : vatMap.entrySet() ){
            String vat = v.getValue();
            BigDecimal vatValue = new BigDecimal(vat);
            if (vatValue.signum() != 0){
                String vatPercent = vatFormatter(vat);
                totalPricePanel.add(new JLabel("   "+vatPercent+" MwSt.: "));
                JTextField vatField = new JTextField(calculateTotalVAT(vat)+" "+currencySymbol);
                vatField.setEditable(false);
                vatField.setColumns(7);
                vatField.setHorizontalAlignment(JTextField.RIGHT);
                totalPricePanel.add(vatField);
            }
        }
        return totalPricePanel;
    }

    protected class BestellungsTable extends AnyJComponentJTable {
        /**
         *    The constructor.
         *       */
        public BestellungsTable(Vector< Vector<Object> > data, Vector<String> columns) {
            super(data, columns);
        }

        @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                // add custom rendering here
                c.setFont( c.getFont().deriveFont(Font.BOLD) );
                String color = colors.get(row);
                int realColIndex = convertColumnIndexToModel(column); // user might have changed column order
                if ( columnLabels.get(realColIndex).equals("Stückzahl") ){
                    if (color.equals("red")){ c.setForeground(Color.red); }
                    else if (color.equals("blue")){ c.setForeground(Color.blue); }
                    else if (color.equals("green")){ c.setForeground(Color.green.darker().darker()); }
                    else { c.setForeground(Color.black); }
                } else { 
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
                String tip = "";
                if ( !columnLabels.get(realColIndex).equals("Entfernen") ){ // exclude column with buttons
                    tip = this.getModel().getValueAt(realRowIndex, realColIndex).toString();
                }
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

    protected void setTableProperties(BestellungsTable table) {
	// Spalteneigenschaften:
//	table.getColumnModel().getColumn(0).setPreferredWidth(10);
	TableColumn lieferant = table.getColumn("Lieferant");
	lieferant.setCellRenderer(linksAusrichter);
	lieferant.setPreferredWidth(50);
	TableColumn artikelnr = table.getColumn("Artikel-Nr.");
	artikelnr.setCellRenderer(linksAusrichter);
	artikelnr.setPreferredWidth(50);
	TableColumn artikelbez = table.getColumn("Artikel-Name");
	artikelbez.setCellRenderer(linksAusrichter);
	artikelbez.setPreferredWidth(150);
	TableColumn preis = table.getColumn("Einzelpreis");
	preis.setCellRenderer(rechtsAusrichter);
	preis.setPreferredWidth(30);
	TableColumn vpe = table.getColumn("VPE");
	vpe.setCellRenderer(rechtsAusrichter);
	vpe.setPreferredWidth(5);
	TableColumn stueckzahl = table.getColumn("Stückzahl");
	stueckzahl.setCellRenderer(rechtsAusrichter);
	stueckzahl.setPreferredWidth(5);
	TableColumn entf = table.getColumn("Entfernen");
	entf.setPreferredWidth(5);
    }

}
