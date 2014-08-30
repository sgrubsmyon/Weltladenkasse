package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.math.RoundingMode;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
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
            HashMap<BigDecimal, Vector<BigDecimal>> abrechnungNettoBetrag = getAbrechnungGroupedByMwst(stmt);
            int rowCount = 0;
            for ( Map.Entry< BigDecimal, Vector<BigDecimal> > entry : abrechnungNettoBetrag.entrySet() ){
                BigDecimal mwst = entry.getKey();
                Vector<BigDecimal> mwstValues = entry.getValue();
                if ( abrechnungsMap.containsKey(date) ){ // Abrechnung already exists, only add information
                    abrechnungsMap.get(date).put(mwst, mwstValues);
                } else { // start new Abrechnung
                    HashMap<BigDecimal, Vector<BigDecimal>> abrechnung = new HashMap<BigDecimal, Vector<BigDecimal>>();
                    abrechnung.put(mwst, mwstValues);
                    abrechnungsMap.put(date, abrechnung);
                }
                mwstSet.add(mwst);
                rowCount++;

                submitButtonEnabled = true;
            }
            if ( rowCount == 0 ){ // empty, there are no verkaeufe!!! Add zeros.
                HashMap<BigDecimal, Vector<BigDecimal>> abrechnung = new HashMap<BigDecimal, Vector<BigDecimal>>();
                Vector<BigDecimal> mwstValues = new Vector<BigDecimal>();
                mwstValues.add(new BigDecimal("0")); 
                mwstValues.add(new BigDecimal("0"));
                abrechnung.put(new BigDecimal("0.07"), mwstValues);
                abrechnung.put(new BigDecimal("0.19"), mwstValues);
                abrechnungsMap.put(date, abrechnung);

                submitButtonEnabled = false;
            }
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    HashMap<BigDecimal, Vector<BigDecimal>> getAbrechnungGroupedByMwst(Statement stmt) {
        HashMap<BigDecimal, Vector<BigDecimal>> map = new HashMap<BigDecimal, Vector<BigDecimal>>();
        try {
            ResultSet rs = stmt.executeQuery(
                    "SELECT mwst_satz, SUM( ROUND(ges_preis / (1.+mwst_satz), 2) ) AS mwst_netto, " +
                    "SUM( ROUND(ges_preis / (1. + mwst_satz) * mwst_satz, 2) ) AS mwst_betrag " +
                    "FROM verkauf_details INNER JOIN verkauf USING (rechnungs_nr) " +
                    "WHERE storniert = FALSE AND verkaufsdatum > (SELECT MAX(zeitpunkt) FROM abrechnung_tag) " +
                    "GROUP BY mwst_satz"
                    );
            while (rs.next()) {
                BigDecimal mwst_satz = rs.getBigDecimal(1);
                BigDecimal mwst_netto = rs.getBigDecimal(2);
                BigDecimal mwst_betrag = rs.getBigDecimal(3);
                Vector<BigDecimal> values = new Vector<BigDecimal>();
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
            HashMap<BigDecimal, Vector<BigDecimal>> abrechnungNettoBetrag = getAbrechnungGroupedByMwst(stmt);
            // get totals (bar brutto):
            HashMap<BigDecimal, BigDecimal> abrechnungBarBrutto = new HashMap<BigDecimal, BigDecimal>();
            ResultSet rs = stmt.executeQuery(
                    "SELECT mwst_satz, SUM(ges_preis) AS bar_brutto " +
                    "FROM verkauf_details INNER JOIN verkauf USING (rechnungs_nr) " +
                    "WHERE storniert = FALSE AND verkaufsdatum > " +
                    "(SELECT MAX(zeitpunkt) FROM abrechnung_tag) AND ec_zahlung = FALSE " +
                    "GROUP BY mwst_satz"
                    );
            while (rs.next()) {
                BigDecimal mwst_satz = rs.getBigDecimal(1);
                BigDecimal bar_brutto = rs.getBigDecimal(2);
                abrechnungBarBrutto.put(mwst_satz, bar_brutto);
                System.out.println(mwst_satz+"  "+bar_brutto);
            }
            rs.close();
            stmt.close();
            System.out.println("mwst_satz  mwst_netto  mwst_betrag  bar_brutto");
            System.out.println("----------------------------------------------");
            for ( Map.Entry< BigDecimal, Vector<BigDecimal> > entry : abrechnungNettoBetrag.entrySet() ){
                BigDecimal mwst_satz = entry.getKey();
                Vector<BigDecimal> values = entry.getValue();
                BigDecimal mwst_netto = values.get(0);
                BigDecimal mwst_betrag = values.get(1);
                BigDecimal bar_brutto = new BigDecimal("0.00");
                if ( abrechnungBarBrutto.containsKey(mwst_satz) ){
                    bar_brutto = abrechnungBarBrutto.get(mwst_satz);
                }
                System.out.println(mwst_satz+"  "+mwst_netto+"  "+mwst_betrag+"   "+bar_brutto);
                PreparedStatement pstmt = this.conn.prepareStatement(
                        "INSERT INTO abrechnung_tag SET zeitpunkt = NOW(), mwst_satz = ?, " +
                        "mwst_netto = ?, mwst_betrag = ?, "+
                        "bar_brutto = ?"
                        );
                pstmt.setBigDecimal(1, mwst_satz);
                pstmt.setBigDecimal(2, mwst_netto);
                pstmt.setBigDecimal(3, mwst_betrag);
                pstmt.setBigDecimal(4, bar_brutto);
                int result = pstmt.executeUpdate();
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Tagesabrechnung konnte nicht gespeichert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
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
