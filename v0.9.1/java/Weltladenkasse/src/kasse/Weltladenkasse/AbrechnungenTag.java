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

public class AbrechnungenTag extends Abrechnungen {
    // Attribute:

    private JButton submitButton;
    private boolean submitButtonEnabled;

    // Methoden:
    /**
     *    The constructor.
     *       */
    public AbrechnungenTag(Connection conn, MainWindowGrundlage mw){
	super(conn, mw, "", "Tagesabrechnungen", "yyyy-MM-dd HH:mm:ss", "dd.MM. (E)", "zeitpunkt", "abrechnung_tag");
	showTable();
    }

    void addOtherStuff() {
        JPanel otherPanel = new JPanel();
        submitButton = new JButton("Tagesabrechnung machen");
        submitButton.setEnabled(submitButtonEnabled);
        submitButton.addActionListener(this);
        otherPanel.add(submitButton);
        otherPanel.add(new JLabel("(Zahlen in rot werden als neue Tagesabrechnung gespeichert.)"));
        tablePanel.add(otherPanel);
    }

    void queryIncompleteAbrechnung() { // create new abrechnung (for display) from time of last abrechnung until now
        try {
            Statement stmt = this.conn.createStatement();
            // for filling the diplayed table:

            // first, get the totals:
            // Gesamt Brutto
            ResultSet rs = stmt.executeQuery(
                    "SELECT SUM(ges_preis) " +
                    "FROM verkauf_details INNER JOIN verkauf USING (rechnungs_nr) " +
                    "WHERE storniert = FALSE AND verkaufsdatum > (SELECT MAX(zeitpunkt) FROM abrechnung_tag) "
                    );
            rs.next(); BigDecimal tagesGesamtBrutto = new BigDecimal(rs.getString(1) == null ? "0" : rs.getString(1)); rs.close();
            // Gesamt Bar Brutto
            rs = stmt.executeQuery(
                    "SELECT NOW(), SUM(ges_preis) AS bar_brutto " +
                    "FROM verkauf_details INNER JOIN verkauf USING (rechnungs_nr) " +
                    "WHERE storniert = FALSE AND verkaufsdatum > (SELECT MAX(zeitpunkt) FROM abrechnung_tag) AND ec_zahlung = FALSE "
                    );
            rs.next(); String date = rs.getString(1); BigDecimal tagesGesamtBarBrutto = new BigDecimal(rs.getString(2) == null ? "0" : rs.getString(2)); rs.close();
            // Gesamt EC Brutto
            BigDecimal tagesGesamtECBrutto = tagesGesamtBrutto.subtract(tagesGesamtBarBrutto);
            Vector<BigDecimal> values = new Vector<BigDecimal>();
            values.add(tagesGesamtBrutto); 
            values.add(tagesGesamtBarBrutto);
            values.add(tagesGesamtECBrutto);
            // store in map under date
            totalsMap.put(date, values);

            // second, get values grouped by mwst
            HashMap<String, Vector<String>> abrechnungNettoBetrag = getAbrechnungGroupedByMwst(stmt);
            int rowCount = 0;
            for ( Map.Entry< String, Vector<String> > entry : abrechnungNettoBetrag.entrySet() ){
                String mwst = entry.getKey();
                Vector<String> mwstValues = entry.getValue();
                if ( abrechnungsMap.containsKey(date) ){ // Abrechnung already exists, only add information
                    abrechnungsMap.get(date).put(mwst, mwstValues);
                } else { // start new Abrechnung
                    HashMap<String, Vector<String>> abrechnung = new HashMap<String, Vector<String>>();
                    abrechnung.put(mwst, mwstValues);
                    abrechnungsMap.put(date, abrechnung);
                }
                mwstSet.add(mwst);
                rowCount++;

                submitButtonEnabled = true;
            }
            if ( rowCount == 0 ){ // empty, there are no verkaeufe!!! Add zeros.
                HashMap<String, Vector<String>> abrechnung = new HashMap<String, Vector<String>>();
                Vector<String> mwstValues = new Vector<String>();
                mwstValues.add("0"); 
                mwstValues.add("0");
                abrechnung.put("0.07", mwstValues);
                abrechnung.put("0.19", mwstValues);
                abrechnungsMap.put(date, abrechnung);

                submitButtonEnabled = false;
            }
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    HashMap<String, Vector<String>> getAbrechnungGroupedByMwst(Statement stmt) {
        HashMap<String, Vector<String>> map = new HashMap<String, Vector<String>>();
        try {
            ResultSet rs = stmt.executeQuery(
                    "SELECT mwst_satz, SUM( ROUND(ges_preis / (1.+mwst_satz), 2) ) AS mwst_netto, " +
                    "SUM( ROUND(ges_preis / (1. + mwst_satz) * mwst_satz, 2) ) AS mwst_betrag " +
                    "FROM verkauf_details INNER JOIN verkauf USING (rechnungs_nr) " +
                    "WHERE storniert = FALSE AND verkaufsdatum > (SELECT MAX(zeitpunkt) FROM abrechnung_tag) " +
                    "GROUP BY mwst_satz"
                    );
            while (rs.next()) {
                String mwst_satz = rs.getString(1);
                String mwst_netto = rs.getString(2);
                String mwst_betrag = rs.getString(3);
                Vector<String> values = new Vector<String>();
                values.add(mwst_netto);
                values.add(mwst_betrag);
                map.put(mwst_satz, values);
            }
            rs.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return map;
    }

    void insertTagesAbrechnung() { // create new abrechnung (and save in DB) from time of last abrechnung until now
        try {
            Statement stmt = this.conn.createStatement();
            // get netto values grouped by mwst:
            HashMap<String, Vector<String>> abrechnungNettoBetrag = getAbrechnungGroupedByMwst(stmt);
            // get totals (bar brutto):
            HashMap<String, String> abrechnungBarBrutto = new HashMap<String, String>();
            ResultSet rs = stmt.executeQuery(
                    "SELECT mwst_satz, SUM(ges_preis) AS bar_brutto " +
                    "FROM verkauf_details INNER JOIN verkauf USING (rechnungs_nr) " +
                    "WHERE storniert = FALSE AND verkaufsdatum > (SELECT MAX(zeitpunkt) FROM abrechnung_tag) AND ec_zahlung = FALSE " +
                    "GROUP BY mwst_satz"
                    );
            while (rs.next()) {
                String mwst_satz = rs.getString(1);
                String bar_brutto = rs.getString(2);
                abrechnungBarBrutto.put(mwst_satz, bar_brutto);
                System.out.println(mwst_satz+"  "+bar_brutto);
            }
            rs.close();
            System.out.println("mwst_satz  mwst_netto  mwst_betrag  bar_brutto");
            System.out.println("----------------------------------------------");
            for ( Map.Entry< String, Vector<String> > entry : abrechnungNettoBetrag.entrySet() ){
                String mwst_satz = entry.getKey();
                Vector<String> values = entry.getValue();
                String mwst_netto = values.get(0);
                String mwst_betrag = values.get(1);
                String bar_brutto = "0.00";
                if ( abrechnungBarBrutto.containsKey(mwst_satz) ){
                    bar_brutto = abrechnungBarBrutto.get(mwst_satz);
                }
                System.out.println(mwst_satz+"  "+mwst_netto+"  "+mwst_betrag+"   "+bar_brutto);
                int result = stmt.executeUpdate(
                        "INSERT INTO abrechnung_tag SET zeitpunkt = NOW(), mwst_satz = "+mwst_satz+", " +
                        "mwst_netto = "+mwst_netto+", mwst_betrag = "+mwst_betrag+", "+
                        "bar_brutto = "+bar_brutto
                        );
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Tagesabrechnung konnte nicht gespeichert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void queryAbrechnungenSpecial() {
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e){
        super.actionPerformed(e);
	if (e.getSource() == prevButton){
	    if (this.currentPage > 1)
		this.currentPage--;
	    updateTable();
	    return;
	}
	if (e.getSource() == nextButton){
	    if (this.currentPage < totalPage)
		this.currentPage++;
	    updateTable();
	    return;
	}
	if (e.getSource() == submitButton){
            insertTagesAbrechnung();
            updateTable();
	    return;
	}
    }
}
