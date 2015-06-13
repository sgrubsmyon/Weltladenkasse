package Weltladenkasse;

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

public abstract class RechnungsGrundlage extends ArtikelGrundlage {

    protected JTextField totalPriceField;
    protected Vector<KassierArtikel> kassierArtikel;
    protected Vector<String> columnLabels;
    protected HashMap< BigDecimal, Vector<BigDecimal> > vatMap;

    // Die Ausrichter:
    protected final String einrueckung = "      ";

    /**
     *    The constructor.
     *       */
    public RechnungsGrundlage(Connection conn, MainWindowGrundlage mw)
    {
	super(conn, mw);
        initiateVectors();
    }
    private void initiateVectors() {
	columnLabels = new Vector<String>();
        columnLabels.add("Pos.");
	columnLabels.add("Artikel-Name"); columnLabels.add("Artikel-Nr."); columnLabels.add("Stückzahl");
        columnLabels.add("Einzelpreis"); columnLabels.add("Gesamtpreis"); columnLabels.add("MwSt.");
        kassierArtikel = new Vector<KassierArtikel>();
    }

    //////////////////////////////////
    // DB query functions:
    //////////////////////////////////
    protected Vector<BigDecimal> retrieveVATs() {
        Vector<BigDecimal> vats = new Vector<BigDecimal>();
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT mwst_satz FROM mwst ORDER BY mwst_id"
                    );
	    while (rs.next()) {
                vats.add(rs.getBigDecimal(1));
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return vats;
    }

    //////////////////////////////////
    // price calculation functions:
    //////////////////////////////////
    protected String calculateTotalPrice() {
        BigDecimal totalPrice = new BigDecimal(0);
        for ( KassierArtikel ka : kassierArtikel ){
            totalPrice = totalPrice.add(ka.getGesPreis());
        }
        return bc.priceFormatter(totalPrice);
    }

    protected BigDecimal calculateTotalVATUmsatz(BigDecimal vat) {
        /** Returns the total amount (brutto prices) that VAT tax is included
         *  in for entire Rechnung (MwSt.-Umsatz). */
        BigDecimal priceForVAT = new BigDecimal(0);
        for (int i=0; i<kassierArtikel.size(); i++){
            BigDecimal mwst = kassierArtikel.get(i).getMwst();
            if ( mwst.equals(vat) ){
                BigDecimal preis = kassierArtikel.get(i).getGesPreis();
                priceForVAT = priceForVAT.add(preis);
            }
        }
        return priceForVAT;
    }

    protected BigDecimal calculateTotalVATAmount(BigDecimal vat) {
        /** Returns the total amount of VAT tax that is included in Rechnung
         *  (MwSt.-Betrag). */
        BigDecimal priceForVAT = calculateTotalVATUmsatz(vat);
        BigDecimal totalVAT = new BigDecimal( bc.priceFormatterIntern(calculateVAT(priceForVAT, vat)) );
        return totalVAT;
    }

    protected JPanel createTotalPricePanel() {
        JPanel totalPricePanel = new JPanel();
        totalPricePanel.setLayout(new FlowLayout());
        JLabel totalPriceLabel = new JLabel("Gesamtpreis: ");
        totalPricePanel.add(totalPriceLabel);
        totalPriceField = new JTextField(calculateTotalPrice()+" "+bc.currencySymbol);
        totalPriceField.setEditable(false);
        totalPriceField.setColumns(7);
        totalPriceField.setHorizontalAlignment(JTextField.RIGHT);
        totalPricePanel.add(totalPriceField);

        totalPricePanel.add(new JLabel("   inkl.: "));
        Vector<BigDecimal> vats = retrieveVATs();
        this.vatMap = new HashMap< BigDecimal, Vector<BigDecimal> >();
        for ( BigDecimal vat : vats ){
            if (vat.signum() != 0){
                BigDecimal vatAmount = calculateTotalVATAmount(vat);
                BigDecimal vatBrutto = calculateTotalVATUmsatz(vat);
                BigDecimal vatNetto = vatBrutto.subtract(vatAmount);
                if (vatBrutto.signum() != 0){ // exclude 0 amounts
                    Vector<BigDecimal> vatVec = new Vector<BigDecimal>();
                    vatVec.add(vatNetto); vatVec.add(vatAmount);
                    this.vatMap.put(vat, vatVec);
                }
                String vatPercent = bc.vatFormatter(vat);
                totalPricePanel.add(new JLabel("   "+vatPercent+" MwSt.: "));
                JTextField vatField = new JTextField(bc.priceFormatter(vatAmount)+" "+bc.currencySymbol);
                vatField.setEditable(false);
                vatField.setColumns(7);
                vatField.setHorizontalAlignment(JTextField.RIGHT);
                totalPricePanel.add(vatField);
            }
        }
        return totalPricePanel;
    }

    protected class RechnungsTable extends AnyJComponentJTable {
        /**
         *    The constructor.
         *       */
        public RechnungsTable(Vector< Vector<Object> > data, Vector<String> columns) {
            super(data, columns);
        }

        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);
            // add custom rendering here
            c.setFont( c.getFont().deriveFont(Font.BOLD) );
            String color = kassierArtikel.get(row).getColor();
            if (color.equals("red")){ c.setForeground(Color.RED); }
            else if (color.equals("blue")){ c.setForeground(Color.BLUE); }
            else if (color.equals("green")){ c.setForeground(Color.GREEN.darker().darker()); }
            else if (color.equals("gray")){ c.setForeground(Color.GRAY); }
            else { c.setForeground(Color.BLACK); }
            //c.setBackground(Color.LIGHT_GRAY);
            return c;
        }
    }

    protected void setTableProperties(RechnungsTable table) {
	// Spalteneigenschaften:
//	table.getColumnModel().getColumn(0).setPreferredWidth(10);
	TableColumn pos = table.getColumn("Pos.");
	pos.setCellRenderer(zentralAusrichter);
	pos.setPreferredWidth(5);
	TableColumn artikelbez = table.getColumn("Artikel-Name");
	artikelbez.setCellRenderer(linksAusrichter);
	artikelbez.setPreferredWidth(150);
	TableColumn artikelnr = table.getColumn("Artikel-Nr.");
	artikelnr.setCellRenderer(rechtsAusrichter);
	artikelnr.setPreferredWidth(50);
	TableColumn stueckzahl = table.getColumn("Stückzahl");
	stueckzahl.setCellRenderer(rechtsAusrichter);
	TableColumn preis = table.getColumn("Einzelpreis");
	preis.setCellRenderer(rechtsAusrichter);
	TableColumn gespreis = table.getColumn("Gesamtpreis");
	gespreis.setCellRenderer(rechtsAusrichter);
	TableColumn mwst = table.getColumn("MwSt.");
	mwst.setCellRenderer(rechtsAusrichter);
	mwst.setPreferredWidth(5);
    }
}
