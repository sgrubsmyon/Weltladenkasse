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

public class AbrechnungenMonat extends Abrechnungen {
    // Attribute:

    // Methoden:
    /**
     *    The constructor.
     *       */
    public AbrechnungenMonat(Connection conn, MainWindowGrundlage mw){
	super(conn, mw, "", "Monatsabrechnungen", "yyyy-MM-dd", "MMM yyyy", "monat", "abrechnung_monat");
	showTable();
    }

    void addOtherStuff() {
        JPanel otherPanel = new JPanel();
        otherPanel.add(new JLabel("(Zahlen in rot werden als neue Monatsabrechnung gespeichert.)"));
        tablePanel.add(otherPanel);
    }

    String returnMaxAbechnungDate() {
        String result = "";
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT MAX(monat) FROM abrechnung_monat"
                    );
            rs.next(); result = rs.getString(1); rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        if (result == null){
            result = "0001-01-01"; // set date very far back, every possible date should be after this one (hopefully)
        }
        return result;
    }

    Vector<String> returnAllNewMonths(String maxDate) { // all months to be put into the db (abrechnung_monat table)
        Vector<String> result = new Vector<String>();
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT DISTINCT DATE_FORMAT(zeitpunkt,'%Y-%m-01') FROM abrechnung_tag "+
                    "WHERE zeitpunkt > (? + INTERVAL 1 MONTH)"
                    );
            pstmt.setString(1, maxDate);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()){
                result.add(rs.getString(1));
            }
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    HashMap<BigDecimal, Vector<BigDecimal>> queryMonatsAbrechnung(String month) {
        HashMap<BigDecimal, Vector<BigDecimal>> abrechnung = new HashMap<BigDecimal, Vector<BigDecimal>>();
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT mwst_satz, SUM(mwst_netto), SUM(mwst_betrag), SUM(bar_brutto) FROM abrechnung_tag "+
                    "WHERE zeitpunkt > ? AND zeitpunkt < (? + INTERVAL 1 MONTH) GROUP BY mwst_satz"
                    );
            pstmt.setString(1, month);
            pstmt.setString(2, month);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()){
                BigDecimal mwst_satz = rs.getBigDecimal(1);
                Vector<BigDecimal> betraege = new Vector<BigDecimal>();
                betraege.add(rs.getBigDecimal(2)); // mwst_netto
                betraege.add(rs.getBigDecimal(3)); // mwst_betrag
                betraege.add(rs.getBigDecimal(4)); // bar_brutto
                abrechnung.put(mwst_satz, betraege);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return abrechnung;
    }

    void insertNewMonths(Vector<String> months) { // all months to be put into the db (abrechnung_monat table)
        try {
            for (String month : months){
                System.out.println("new month: "+month);
                PreparedStatement pstmt = this.conn.prepareStatement(
                        "SELECT ? < DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')"
                        );
                pstmt.setString(1, month);
                ResultSet rs = pstmt.executeQuery();
                rs.next(); boolean doIt = rs.getBoolean(1); rs.close();
                if (doIt){
                    HashMap<BigDecimal, Vector<BigDecimal>> sachen = queryMonatsAbrechnung(month);
                    for ( Map.Entry< BigDecimal, Vector<BigDecimal> > entry : sachen.entrySet() ){
                        BigDecimal mwst_satz = entry.getKey();
                        Vector<BigDecimal> betraege = entry.getValue();
                        System.out.println("mwst_satz: "+mwst_satz);
                        System.out.println("betraege "+betraege);
                        pstmt = this.conn.prepareStatement(
                                "INSERT INTO abrechnung_monat SET monat = ?, "+
                                "mwst_satz = ?, "+
                                "mwst_netto = ?, "+
                                "mwst_betrag = ?, "+
                                "bar_brutto = ?"
                                );
                        pstmt.setString(1, month);
                        pstmt.setBigDecimal(2, mwst_satz);
                        pstmt.setBigDecimal(3, betraege.get(0));
                        pstmt.setBigDecimal(4, betraege.get(1));
                        pstmt.setBigDecimal(5, betraege.get(2));
                        int result = pstmt.executeUpdate();
                        pstmt.close();
                        if (result != 0){
                            // do nothing
                        }
                        else {
                            JOptionPane.showMessageDialog(this,
                                    "Fehler: Monatsabrechnung für Monat "+month+", MwSt.-Satz "+mwst_satz+" konnte nicht gespeichert werden.",
                                    "Fehler", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void queryIncompleteAbrechnung() { // create new abrechnung (for display) from time of last abrechnung until now
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT DATE_FORMAT(CURDATE(), '%Y-%m-01')"
                    );
            rs.next(); String cur_month = rs.getString(1); rs.close();
            stmt.close();

            // totals (sum over mwsts)
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT SUM(mwst_netto + mwst_betrag), SUM(bar_brutto), SUM(mwst_netto + mwst_betrag) - SUM(bar_brutto) FROM abrechnung_tag " +
                       //   ^^^ Gesamt Brutto              ^^^ Gesamt Bar Brutto      ^^^ Gesamt EC Brutto = Ges. Brutto - Ges. Bar Brutto
                    "WHERE zeitpunkt > ? AND zeitpunkt < (? + INTERVAL 1 MONTH)"
                    );
            pstmt.setString(1, cur_month);
            pstmt.setString(2, cur_month);
            rs = pstmt.executeQuery();
            rs.next();
            Vector<BigDecimal> totalsValues = new Vector<BigDecimal>();
            totalsValues.add(new BigDecimal(rs.getString(1) == null ? "0." : rs.getString(1))); 
            totalsValues.add(new BigDecimal(rs.getString(2) == null ? "0." : rs.getString(2))); 
            totalsValues.add(new BigDecimal(rs.getString(3) == null ? "0." : rs.getString(3))); 
            rs.close();
            pstmt.close();
            // store in map under date
            totalsMap.put(cur_month, totalsValues);

            // grouped by mwst
            HashMap<BigDecimal, Vector<BigDecimal>> sachen = queryMonatsAbrechnung(cur_month);
            int rowCount = 0;
            for ( Map.Entry< BigDecimal, Vector<BigDecimal> > entry : sachen.entrySet() ){
                BigDecimal mwst_satz = entry.getKey();
                Vector<BigDecimal> betraege = entry.getValue();
                Vector<BigDecimal> mwstValues = new Vector<BigDecimal>();
                mwstValues.add(betraege.get(0)); // mwst_netto
                mwstValues.add(betraege.get(1)); // mwst_betrag

                if ( abrechnungsMap.containsKey(cur_month) ){ // Abrechnung already exists, only add information
                    abrechnungsMap.get(cur_month).put(mwst_satz, mwstValues);
                } else { // start new Abrechnung
                    HashMap<BigDecimal, Vector<BigDecimal>> abrechnung = new HashMap<BigDecimal, Vector<BigDecimal>>();
                    abrechnung.put(mwst_satz, mwstValues);
                    abrechnungsMap.put(cur_month, abrechnung);
                }
                mwstSet.add(mwst_satz);
                rowCount++;

                System.out.println("Aktuell mwst_satz: "+mwst_satz);
                System.out.println("Aktuell betraege "+betraege);
            }
            if ( rowCount == 0 ){ // empty, there are no verkaeufe!!! Add zeros.
                HashMap<BigDecimal, Vector<BigDecimal>> abrechnung = new HashMap<BigDecimal, Vector<BigDecimal>>();
                Vector<BigDecimal> mwstValues = new Vector<BigDecimal>();
                mwstValues.add(new BigDecimal("0")); 
                mwstValues.add(new BigDecimal("0"));
                abrechnung.put(new BigDecimal("0.07"), mwstValues);
                abrechnung.put(new BigDecimal("0.19"), mwstValues);
                abrechnungsMap.put(cur_month, abrechnung);
            }
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void queryAbrechnungenSpecial() {
        String maxDate = returnMaxAbechnungDate();
        Vector<String> months = returnAllNewMonths(maxDate);
        System.out.println("max date is: "+maxDate);
        System.out.println("new months are: "+months);
        insertNewMonths(months);
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
    }
}
