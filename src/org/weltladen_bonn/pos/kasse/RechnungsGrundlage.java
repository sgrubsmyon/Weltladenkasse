package org.weltladen_bonn.pos.kasse;

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
import org.mariadb.jdbc.MariaDbPoolDataSource;

import org.weltladen_bonn.pos.*;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class RechnungsGrundlage extends ArtikelGrundlage {
    private static final Logger logger = LogManager.getLogger(RechnungsGrundlage.class);

    protected JTextField totalPriceField;
    protected Vector<KassierArtikel> kassierArtikel;
    protected Vector<BigDecimal> mwsts;
    protected String zahlungsModus;
    protected BigDecimal kundeGibt;
    protected String datum;
    protected Integer rechnungsNr;
    protected Vector<String> columnLabels;
    protected TreeMap< BigDecimal, Vector<BigDecimal> > vatMap;

    // Die Ausrichter:
    protected final String einrueckung = "      ";

    /**
     *    The constructor.
     *       */
    public RechnungsGrundlage(MariaDbPoolDataSource pool, MainWindowGrundlage mw)
    {
	    super(pool, mw);
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
        rechnungsNr = null;
    }

    //////////////////////////////////
    // DB query functions:
    //////////////////////////////////
    protected LinkedHashMap<Integer, BigDecimal> retrieveVATs() {
        // LinkedHashMap preserves insertion order
        LinkedHashMap<Integer, BigDecimal> vats = new LinkedHashMap<Integer, BigDecimal>();
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT mwst_id, mwst_satz FROM mwst ORDER BY mwst_id"
            );
	        while (rs.next()) {
                vats.put(rs.getInt(1), rs.getBigDecimal(2));
            }
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
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

    protected TreeMap< BigDecimal, Vector<BigDecimal> > calculateMwStValuesInRechnung() { // TreeMap is sorted according to the natural ordering of its keys
        // Calculate the VAT values for all the VAT percentages included in the
        //   currently displayed Rechnung, but only for these, not for all VATs
        //   that currently exist
        TreeMap< BigDecimal, Vector<BigDecimal> > mwstValues =
            new TreeMap< BigDecimal, Vector<BigDecimal> >();
        for (int i=0; i<kassierArtikel.size(); i++){
            BigDecimal steuersatz = kassierArtikel.get(i).getMwst();
            BigDecimal brutto = kassierArtikel.get(i).getGesPreis();
            BigDecimal steuer = calculateVAT(brutto, steuersatz);
            BigDecimal netto = brutto.subtract(steuer);
            Vector<BigDecimal> values;
            if (mwstValues.containsKey(steuersatz)) {
                values = mwstValues.get(steuersatz);
            } else {
                values = new Vector<BigDecimal>();
                values.add(new BigDecimal(0));
                values.add(new BigDecimal(0));
                values.add(new BigDecimal(0));
            }
            values.set(0, values.get(0).add(netto));
            values.set(1, values.get(1).add(steuer));
            values.set(2, values.get(2).add(brutto)); // = Umsatz
            mwstValues.put(steuersatz, values);
        }
        return mwstValues;
    }

    /* Don't use this method for historical Rechnung, only for current Rechnung in Kassieren or storno in HeutigeRechnungen (problem when MwSt values change)
       (assume MwSt does not change within one Tagesabrechnung) */
    protected HashMap<Integer, Vector<BigDecimal>> getAllCurrentMwstValuesByID() {
        LinkedHashMap<Integer, BigDecimal> vats = retrieveVATs();
        HashMap<Integer, Vector<BigDecimal>> mwstIDsAndValues = new HashMap< Integer, Vector<BigDecimal> >();
        TreeMap<BigDecimal, Vector<BigDecimal>> mwstValues = calculateMwStValuesInRechnung();
        for ( Map.Entry<Integer, BigDecimal​> vat : vats.entrySet() ){
            BigDecimal steuersatz = vat.getValue();
            //if (steuersatz.signum() != 0){ // need to calculate 0% also for correct booking (DSFinV-K)
            if ( mwstValues.containsKey(steuersatz) ){
                Vector<BigDecimal> values = mwstValues.get(steuersatz);
                Vector<BigDecimal> v = new Vector<BigDecimal>();
                v.add(steuersatz);
                v.add(values.get(0));
                v.add(values.get(1));
                v.add(values.get(2)); // = Umsatz
                mwstIDsAndValues.put(vat.getKey(), v);
            }
            //}
        }
        return mwstIDsAndValues;
    }

    protected JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        JPanel totalPricePanel = new JPanel();
        totalPricePanel.setLayout(new FlowLayout());
        totalPricePanel.add(Box.createRigidArea(new Dimension(20,0)));
        JLabel totalPriceLabel = new JLabel("Gesamtpreis: ");
        totalPricePanel.add(totalPriceLabel);
        totalPriceField = new JTextField(calculateTotalPrice()+" "+bc.currencySymbol);
        totalPriceField.setEditable(false);
        totalPriceField.setColumns(7);
        removeDefaultKeyBindings(totalPriceField);
        totalPriceField.setHorizontalAlignment(JTextField.RIGHT);
        totalPricePanel.add(totalPriceField);
        bottomPanel.add(totalPricePanel, BorderLayout.WEST);
        
        JPanel vatPanel = new JPanel();
        vatPanel.setLayout(new FlowLayout());

        vatPanel.add(new JLabel("   inkl.: "));
        vatMap = calculateMwStValuesInRechnung();
        Set<BigDecimal> vats = vatMap.keySet();
        // Sorting not needed when using TreeSet for vatMap:
        // logger.info("unsorted key set: {}", vats);
        // ArrayList<BigDecimal> vatsList = new ArrayList<BigDecimal>(vats); // convert set to list in order to sort
        // Collections.sort(vatsList);
        // logger.info("sorted key list: {}", vatsList);
        for ( BigDecimal vat : vats ){
            Vector<BigDecimal> values;
            if (vatMap.containsKey(vat)) {
                values = vatMap.get(vat);
            } else {
                values = new Vector<BigDecimal>();
                values.add(new BigDecimal(0));
                values.add(new BigDecimal(0));
                values.add(new BigDecimal(0));
            }
            BigDecimal steuer = values.get(1);
            String vatPercent = bc.vatFormatter(vat);
            vatPanel.add(new JLabel("   "+vatPercent+" MwSt.: "));
            JTextField vatField = new JTextField(bc.priceFormatter(steuer)+" "+bc.currencySymbol);
            vatField.setEditable(false);
            vatField.setColumns(7);
            removeDefaultKeyBindings(vatField);
            vatField.setHorizontalAlignment(JTextField.RIGHT);
            vatPanel.add(vatField);
        }
        bottomPanel.add(vatPanel, BorderLayout.CENTER);

        return bottomPanel;
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

    protected String getTotalPrice() {
        return bc.priceFormatterIntern( totalPriceField.getText() );
    }
}
