package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.math.RoundingMode;

// GUI stuff:
import java.awt.*; // BorderLayout, FlowLayout, Dimension, Event, Component, Color, Font
import java.awt.event.*; // MouseEvent, ActionEvent, ActionListener

import javax.swing.*; // JButton
import javax.swing.event.*; // ActionEvent
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.*;

// MySQL Connector/J stuff:
import java.sql.*;

import WeltladenDB.*;

public abstract class RechnungsGrundlage extends ArtikelGrundlage {
    protected JTextField totalPriceField;
    protected Vector<KassierArtikel> kassierArtikel;
    protected Vector<BigDecimal> mwsts;
    protected String zahlungsModus;
    protected BigDecimal kundeGibt;
    protected String datum;
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
        mwsts = new Vector<BigDecimal>();
        zahlungsModus = "unbekannt";
        kundeGibt = null;
        datum = "";
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
        removeDefaultKeyBindings(totalPriceField);
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
                removeDefaultKeyBindings(vatField);
                vatField.setHorizontalAlignment(JTextField.RIGHT);
                totalPricePanel.add(vatField);
            }
        }
        return totalPricePanel;
    }

    protected void setTableProperties(ArticleSelectTable table) {
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


    protected LinkedHashMap< BigDecimal, Vector<BigDecimal> > getMwstsAndTheirValues() {
        Vector<BigDecimal> vats = retrieveVATs();
        // LinkedHashMap preserves insertion order
        LinkedHashMap< BigDecimal, Vector<BigDecimal> > mwstsAndTheirValues =
            new LinkedHashMap< BigDecimal, Vector<BigDecimal> >();
        for ( BigDecimal vat : vats ){
            //if (vat.signum() != 0){
            if ( mwsts.contains(vat) ){
                Vector<BigDecimal> values = new Vector<BigDecimal>();
                BigDecimal brutto = calculateTotalVATUmsatz(vat);
                BigDecimal steuer = calculateTotalVATAmount(vat);
                BigDecimal netto = new BigDecimal(
                        bc.priceFormatterIntern(brutto.subtract(steuer))
                        );
                values.add(netto); // Netto
                values.add(steuer); // Steuer
                values.add(brutto); // Umsatz
                mwstsAndTheirValues.put(vat, values);
            }
            //}
        }
        return mwstsAndTheirValues;
    }


    protected String getTotalPrice() {
        return bc.priceFormatterIntern( totalPriceField.getText() );
    }
}
