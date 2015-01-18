package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

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

public class AbrechnungenJahr extends Abrechnungen {
    // Attribute:

    // Methoden:
    /**
     *    The constructor.
     *       */
    public AbrechnungenJahr(Connection conn, MainWindowGrundlage mw){
	super(conn, mw, "", "Jahresabrechnung", "yyyy", "yyyy",
                "yyyy", "jahr", "abrechnung_jahr");
	showTable();
    }

    void addOtherStuff() {
        JPanel otherPanel = new JPanel();
        otherPanel.add(new JLabel("(Zahlen in rot werden als neue Jahresabrechnung gespeichert.)"));
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
            result = "0001"; // set date very far back, every possible date should be after this one (hopefully)
        }
        return result+"-01-01";
    }

    Vector<String> returnAllNewYears(String maxDate) { // all years to be put into the db (abrechnung_jahr table)
        Vector<String> result = new Vector<String>();
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT DISTINCT DATE_FORMAT(monat, '%Y-01-01') FROM abrechnung_monat "+
                    "WHERE monat >= (? + INTERVAL 1 YEAR)"
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

    HashMap<BigDecimal, Vector<BigDecimal>> queryJahresAbrechnung(String year) {
        HashMap<BigDecimal, Vector<BigDecimal>> abrechnung = new HashMap<BigDecimal, Vector<BigDecimal>>();
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT mwst_satz, SUM(mwst_netto), SUM(mwst_betrag), SUM(bar_brutto) FROM abrechnung_tag "+
                    "WHERE zeitpunkt >= ? AND zeitpunkt < (? + INTERVAL 1 YEAR) GROUP BY mwst_satz"
                    );
            pstmt.setString(1, year);
            pstmt.setString(2, year);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()){
                BigDecimal mwst_satz = rs.getBigDecimal(1);
                Vector<BigDecimal> betraege = new Vector<BigDecimal>();
                betraege.add( rs.getBigDecimal(2).add(rs.getBigDecimal(3)) ); // mwst_netto+mwst_betrag (=brutto)
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

    void insertNewYears(Vector<String> years) { // all years to be put into the db (abrechnung_jahr table)
        try {
            for (String year : years){
                Integer id = id();
                System.out.println("new year: "+year);
                PreparedStatement pstmt = this.conn.prepareStatement(
                        "SELECT ? < DATE_FORMAT(CURRENT_DATE, '%Y-01-01')"
                        );
                pstmt.setString(1, year);
                ResultSet rs = pstmt.executeQuery();
                rs.next(); boolean doIt = rs.getBoolean(1); rs.close();
                pstmt.close();
                if (doIt){
                    HashMap<BigDecimal, Vector<BigDecimal>> sachen = queryJahresAbrechnung(year);
                    for ( Map.Entry< BigDecimal, Vector<BigDecimal> > entry : sachen.entrySet() ){
                        BigDecimal mwst_satz = entry.getKey();
                        Vector<BigDecimal> betraege = entry.getValue();
                        System.out.println("mwst_satz: "+mwst_satz);
                        System.out.println("betraege "+betraege);
                        pstmt = this.conn.prepareStatement(
                                "INSERT INTO abrechnung_jahr SET id = ?, jahr = ?, "+
                                "mwst_satz = ?, "+
                                "mwst_netto = ?, "+
                                "mwst_betrag = ?, "+
                                "bar_brutto = ?"
                                );
                        pstmt.setInt(1, id);
                        pstmt.setString(2, year.substring(0,4));
                        pstmt.setBigDecimal(3, mwst_satz);
                        pstmt.setBigDecimal(4, betraege.get(1));
                        pstmt.setBigDecimal(5, betraege.get(2));
                        pstmt.setBigDecimal(6, betraege.get(3));
                        int result = pstmt.executeUpdate();
                        pstmt.close();
                        if (result != 0){
                            // do nothing
                        }
                        else {
                            JOptionPane.showMessageDialog(this,
                                    "Fehler: Jahresabrechnung für Jahr "+year.substring(0,4)+", "+
                                    "MwSt.-Satz "+mwst_satz+" konnte nicht gespeichert werden.",
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
                    "SELECT DATE_FORMAT(CURDATE(), '%Y-01-01')"
                    );
            rs.next(); String cur_year = rs.getString(1); rs.close();
            stmt.close();

            // totals (sum over mwsts)
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT SUM(mwst_netto + mwst_betrag), SUM(bar_brutto), SUM(mwst_netto + mwst_betrag) - SUM(bar_brutto) FROM abrechnung_tag " +
                       //   ^^^ Gesamt Brutto              ^^^ Gesamt Bar Brutto      ^^^ Gesamt EC Brutto = Ges. Brutto - Ges. Bar Brutto
                    "WHERE zeitpunkt >= ? AND zeitpunkt < (? + INTERVAL 1 YEAR)"
                    );
            pstmt.setString(1, cur_year);
            pstmt.setString(2, cur_year);
            rs = pstmt.executeQuery();
            rs.next();
            Vector<BigDecimal> totalsValues = new Vector<BigDecimal>();
            totalsValues.add(new BigDecimal(rs.getString(1) == null ? "0." : rs.getString(1)));
            totalsValues.add(new BigDecimal(rs.getString(2) == null ? "0." : rs.getString(2)));
            totalsValues.add(new BigDecimal(rs.getString(3) == null ? "0." : rs.getString(3)));
            rs.close();
            pstmt.close();

            // add the incomplete day
            Vector<BigDecimal> totalsValuesDay = queryIncompleteAbrechnungTag_Totals();
            totalsValues.set( 0, totalsValues.get(0).add(totalsValuesDay.get(0)) );
            totalsValues.set( 1, totalsValues.get(1).add(totalsValuesDay.get(1)) );
            totalsValues.set( 2, totalsValues.get(2).add(totalsValuesDay.get(2)) );

            // store in vectors
            incompleteAbrechnungsDate = cur_year;
            incompleteAbrechnungsTotals = totalsValues;
            incompleteAbrechnungsVATs = new HashMap<BigDecimal, Vector<BigDecimal>>();

            // grouped by mwst
            HashMap<BigDecimal, Vector<BigDecimal>> sachen = queryJahresAbrechnung(cur_year);
            for ( Map.Entry< BigDecimal, Vector<BigDecimal> > entry : sachen.entrySet() ){
                BigDecimal mwst_satz = entry.getKey();
                Vector<BigDecimal> betraege = entry.getValue();
                Vector<BigDecimal> mwstValues = new Vector<BigDecimal>();
                mwstValues.add(betraege.get(0)); // mwst_netto+mwst_betrag (=brutto)
                mwstValues.add(betraege.get(1)); // mwst_netto
                mwstValues.add(betraege.get(2)); // mwst_betrag
                incompleteAbrechnungsVATs.put(mwst_satz, mwstValues);
                mwstSet.add(mwst_satz);
            }

            // add the incomplete day
            HashMap<BigDecimal, Vector<BigDecimal>> sachenTag = queryIncompleteAbrechnungTag_VATs();
            for ( Map.Entry< BigDecimal, Vector<BigDecimal> > entry : sachenTag.entrySet() ){
                BigDecimal mwst_satz = entry.getKey();
                Vector<BigDecimal> mwstValues = entry.getValue();
                //Vector<BigDecimal> betraege = entry.getValue();
                //Vector<BigDecimal> mwstValues = new Vector<BigDecimal>();
                //mwstValues.add(betraege.get(0)); // mwst_netto+mwst_betrag (=brutto)
                //mwstValues.add(betraege.get(1)); // mwst_netto
                //mwstValues.add(betraege.get(2)); // mwst_betrag

                if ( incompleteAbrechnungsVATs.containsKey(mwst_satz) ){ // mwst exists, add numbers
                    incompleteAbrechnungsVATs.get(mwst_satz).
                        set( 0, incompleteAbrechnungsVATs.get(mwst_satz).get(0).add(mwstValues.get(0)) );
                    incompleteAbrechnungsVATs.get(mwst_satz).
                        set( 1, incompleteAbrechnungsVATs.get(mwst_satz).get(1).add(mwstValues.get(1)) );
                    incompleteAbrechnungsVATs.get(mwst_satz).
                        set( 2, incompleteAbrechnungsVATs.get(mwst_satz).get(2).add(mwstValues.get(2)) );
                } else { // only add information
                    incompleteAbrechnungsVATs.put(mwst_satz, mwstValues);
                }
                mwstSet.add(mwst_satz);
            }
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void queryAbrechnungenSpecial() {
        String maxDate = returnMaxAbechnungDate();
        Vector<String> years = returnAllNewYears(maxDate);
        System.out.println("max date is: "+maxDate);
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
