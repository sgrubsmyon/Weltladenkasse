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

public class AbrechnungenJahr extends Abrechnungen {
    // Attribute:

    // Methoden:
    /**
     *    The constructor.
     *       */
    public AbrechnungenJahr(Connection conn, MainWindowGrundlage mw){
	super(conn, mw, "", "Monatsabrechnungen", "yyyy-MM-dd", "MMM yyyy", "jahr", "abrechnung_jahr");
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
                    "SELECT MAX(jahr) FROM abrechnung_jahr"
                    );
            rs.next(); result = rs.getString(1); rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        if (result == null){
            result = "0000"; // set date very far back, every possible date should be after this one (hopefully)
        }
        return result;
    }

    Vector<String> returnAllNewYears(String maxYear) { // all years to be put into the db (abrechnung_jahr table)
        Vector<String> result = new Vector<String>();
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT DISTINCT DATE_FORMAT(monat,'%Y') FROM abrechnung_monat WHERE monat > '"+maxYear+"' + INTERVAL 1 YEAR)"
                    );
            while(rs.next()){
                result.add(rs.getString(1));
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    HashMap<String, Vector<String>> queryMonatsAbrechnung(Statement stmt, String year) {
        HashMap<String, Vector<String>> abrechnung = new HashMap<String, Vector<String>>();
        try {
            ResultSet rs = stmt.executeQuery(
                    "SELECT mwst_satz, SUM(mwst_netto), SUM(mwst_betrag), SUM(bar_brutto) FROM abrechnung_tag "+
                    "WHERE zeitpunkt > '"+year+"' AND zeitpunkt < ('"+year+"' + INTERVAL 1 MONTH) GROUP BY mwst_satz"
                    );
            while (rs.next()){
                String mwst_satz = rs.getString(1);
                Vector<String> betraege = new Vector<String>();
                betraege.add(rs.getString(2)); // mwst_netto
                betraege.add(rs.getString(3)); // mwst_betrag
                betraege.add(rs.getString(4)); // bar_brutto
                abrechnung.put(mwst_satz, betraege);
            }
            rs.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return abrechnung;
    }

    void insertNewYears(Vector<String> years) { // all years to be put into the db (abrechnung_jahr table)
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = null;
            for (String year : years){
                System.out.println("new year: "+year);
                rs = stmt.executeQuery(
                        "SELECT '"+year+"' < DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')"
                        );
                rs.next(); boolean doIt = rs.getBoolean(1); rs.close();
                if (doIt){
                    HashMap<String, Vector<String>> sachen = queryMonatsAbrechnung(stmt, year);
                    for ( Map.Entry< String, Vector<String> > entry : sachen.entrySet() ){
                        String mwst_satz = entry.getKey();
                        Vector<String> betraege = entry.getValue();
                        System.out.println("mwst_satz: "+mwst_satz);
                        System.out.println("betraege "+betraege);
                        int result = stmt.executeUpdate(
                                "INSERT INTO abrechnung_jahr SET jahr = '"+year+"', "+
                                "mwst_satz = "+mwst_satz+", "+
                                "mwst_netto = "+betraege.get(0)+", "+
                                "mwst_betrag = "+betraege.get(1)+", "+
                                "bar_brutto = "+betraege.get(2)
                                );
                        if (result != 0){
                            // do nothing
                        }
                        else {
                            JOptionPane.showMessageDialog(this,
                                    "Fehler: Monatsabrechnung für Monat "+year+", MwSt.-Satz "+mwst_satz+" konnte nicht gespeichert werden.",
                                    "Fehler", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
            stmt.close();
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
            rs.next(); String cur_year = rs.getString(1); rs.close();

            // totals (sum over mwsts)
            rs = stmt.executeQuery(
                    "SELECT SUM(mwst_netto + mwst_betrag), SUM(bar_brutto), SUM(mwst_netto + mwst_betrag) - SUM(bar_brutto) FROM abrechnung_tag " +
                       //   ^^^ Gesamt Brutto              ^^^ Gesamt Bar Brutto      ^^^ Gesamt EC Brutto = Ges. Brutto - Ges. Bar Brutto
                    "WHERE zeitpunkt > '"+cur_year+"' AND zeitpunkt < ('"+cur_year+"' + INTERVAL 1 MONTH)"
                    );
            rs.next();
            Vector<BigDecimal> totalsValues = new Vector<BigDecimal>();
            totalsValues.add(new BigDecimal(rs.getString(1) == null ? "0." : rs.getString(1))); 
            totalsValues.add(new BigDecimal(rs.getString(2) == null ? "0." : rs.getString(2))); 
            totalsValues.add(new BigDecimal(rs.getString(3) == null ? "0." : rs.getString(3))); 
            rs.close();
            // store in map under date
            totalsMap.put(cur_year, totalsValues);

            // grouped by mwst
            HashMap<String, Vector<String>> sachen = queryMonatsAbrechnung(stmt, cur_year);
            int rowCount = 0;
            for ( Map.Entry< String, Vector<String> > entry : sachen.entrySet() ){
                String mwst_satz = entry.getKey();
                Vector<String> betraege = entry.getValue();
                Vector<String> mwstValues = new Vector<String>();
                mwstValues.add(betraege.get(0)); // mwst_netto
                mwstValues.add(betraege.get(1)); // mwst_betrag

                if ( abrechnungsMap.containsKey(cur_year) ){ // Abrechnung already exists, only add information
                    abrechnungsMap.get(cur_year).put(mwst_satz, mwstValues);
                } else { // start new Abrechnung
                    HashMap<String, Vector<String>> abrechnung = new HashMap<String, Vector<String>>();
                    abrechnung.put(mwst_satz, mwstValues);
                    abrechnungsMap.put(cur_year, abrechnung);
                }
                mwstSet.add(mwst_satz);
                rowCount++;

                System.out.println("Aktuell mwst_satz: "+mwst_satz);
                System.out.println("Aktuell betraege "+betraege);
            }
            if ( rowCount == 0 ){ // empty, there are no verkaeufe!!! Add zeros.
                HashMap<String, Vector<String>> abrechnung = new HashMap<String, Vector<String>>();
                Vector<String> mwstValues = new Vector<String>();
                mwstValues.add("0"); 
                mwstValues.add("0");
                abrechnung.put("0.07", mwstValues);
                abrechnung.put("0.19", mwstValues);
                abrechnungsMap.put(cur_year, abrechnung);
            }

            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void queryAbrechnungenSpecial() {
        String maxYear = returnMaxAbechnungDate();
        Vector<String> years = returnAllNewYears(maxYear);
        System.out.println("max date is: "+maxYear);
        System.out.println("new years are: "+years);
        insertNewYears(years);
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
