package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.text.SimpleDateFormat;
import java.text.ParseException;

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

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.weltladen_bonn.pos.MainWindowGrundlage;

public class AbrechnungenJahr extends Abrechnungen {
    // Attribute:
    private static final Logger logger = LogManager.getLogger(AbrechnungenJahr.class);

    // Methoden:
    /**
     *    The constructor.
     *       */
    public AbrechnungenJahr(MariaDbPoolDataSource pool, MainWindowGrundlage mw){
        super(pool, mw, "", "Jahresabrechnung", "yyyy", "yyyy",
                "jahr", "abrechnung_jahr");
        this.setExportDirFormat(bc.exportDirAbrechnungJahr);
        showTable();
    }

    void addOtherStuff() {
        JPanel otherPanel = new JPanel();
        otherPanel.add(new JLabel("(Zahlen in rot werden als neue Jahresabrechnung gespeichert.)"));
        headerPanel.add(otherPanel);
    }

    String returnMaxAbechnungDate() {
        String result = "";
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT MAX(jahr) FROM "+tableForMode("abrechnung_jahr")
            );
            rs.next(); result = rs.getString(1); rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        if (result == null){
            result = "0001-01-01"; // set date very far back, every possible date should be after this one (hopefully)
        }
        return result; // +"-01-01" adding this is somehow not needed, but I need it on the mysql cmd line, ???
    }

    Vector<String> returnAllNewYears(String maxDate) { // all years to be put into the db (abrechnung_jahr table)
        Vector<String> result = new Vector<String>();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT DISTINCT DATE_FORMAT(zeitpunkt, '%Y-01-01') FROM "+tableForMode("abrechnung_tag")+" "+ // Select the distinct years of all Tagesabrechnungen after the last Jahresabrechnung
                "WHERE zeitpunkt >= (? + INTERVAL 1 YEAR)"
            );
            pstmt.setString(1, maxDate);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()){
                result.add(rs.getString(1));
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }

    Vector<Integer> queryJahresAbrechnungRange(String year) {
        Vector<Integer> range = new Vector<Integer>();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT MIN(id), MAX(id) FROM "+tableForMode("abrechnung_tag")+" "+
                "WHERE zeitpunkt >= ? AND zeitpunkt < (? + INTERVAL 1 YEAR)"
            );
            pstmt.setString(1, year);
            pstmt.setString(2, year);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            range.add(rs.getInt(1));
            range.add(rs.getInt(2));
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return range;
    }

    HashMap<BigDecimal, Vector<BigDecimal>> queryJahresAbrechnung(int minID, int maxID) {
        HashMap<BigDecimal, Vector<BigDecimal>> abrechnung = new HashMap<BigDecimal, Vector<BigDecimal>>();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT mwst_satz, SUM(mwst_netto), SUM(mwst_betrag), SUM(bar_brutto) FROM "+tableForMode("abrechnung_tag_mwst")+" "+
                "INNER JOIN "+tableForMode("abrechnung_tag")+" USING (id) "+
                "WHERE id >= ? AND id <= ? GROUP BY mwst_satz"
            );
            pstmt.setInt(1, minID);
            pstmt.setInt(2, maxID);
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
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return abrechnung;
    }

    void insertNewYears(Vector<String> years) { // all years to be put into the db (abrechnung_jahr table)
        try {
            Connection connection = this.pool.getConnection();
            for (String year : years){
                logger.info("new year: "+year);
                PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT ? < DATE_FORMAT(CURRENT_DATE, '%Y-01-01')"
                );
                pstmt.setString(1, year);
                ResultSet rs = pstmt.executeQuery();
                rs.next(); boolean doIt = rs.getBoolean(1); rs.close();
                pstmt.close();
                if (doIt){
                    Integer id = null;

                    Vector<Integer> range = queryJahresAbrechnungRange(year);
                    pstmt = connection.prepareStatement(
                        "INSERT INTO "+tableForMode("abrechnung_jahr")+" SET "+
                        "jahr = ?, "+
                        "abrechnung_tag_id_von = ?, "+
                        "abrechnung_tag_id_bis = ?"
                    );
                    pstmt.setString(1, year.substring(0,4));
                    pstmt.setInt(2, range.get(0));
                    pstmt.setInt(3, range.get(1));
                    int result = pstmt.executeUpdate();
                    pstmt.close();
                    if (result == 0){
                        JOptionPane.showMessageDialog(this,
                            "Fehler: Jahresabrechnung für Jahr "+year.substring(0,4)+", "+
                            "konnte nicht gespeichert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    } else {
                        id = id();

                        HashMap<BigDecimal, Vector<BigDecimal>> sachen = queryJahresAbrechnung(range.get(0), range.get(1));
                        for ( Map.Entry< BigDecimal, Vector<BigDecimal> > entry : sachen.entrySet() ){
                            BigDecimal mwst_satz = entry.getKey();
                            Vector<BigDecimal> betraege = entry.getValue();
                            logger.info("mwst_satz: "+mwst_satz);
                            logger.info("betraege "+betraege);
                            pstmt = connection.prepareStatement(
                                "INSERT INTO "+tableForMode("abrechnung_jahr_mwst")+" SET "+
                                "id = ?, "+
                                "mwst_satz = ?, "+
                                "mwst_netto = ?, "+
                                "mwst_betrag = ?, "+
                                "bar_brutto = ?"
                            );
                            pstmt.setInt(1, id);
                            pstmt.setBigDecimal(2, mwst_satz);
                            pstmt.setBigDecimal(3, betraege.get(1));
                            pstmt.setBigDecimal(4, betraege.get(2));
                            pstmt.setBigDecimal(5, betraege.get(3));
                            result = pstmt.executeUpdate();
                            pstmt.close();
                            if (result == 0){
                                JOptionPane.showMessageDialog(this,
                                    "Fehler: MwSt.-Betrag der Jahresabrechnung für Jahr "+year.substring(0,4)+", "+
                                    "MwSt.-Satz '"+mwst_satz+"', konnte nicht gespeichert werden.",
                                    "Fehler", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }
            }
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    @Override
    void queryIncompleteAbrechnung() { // create new abrechnung (for display) from time of last abrechnung until now
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT DATE_FORMAT(CURDATE(), '%Y-01-01')"
                    );
            rs.next(); String cur_year = rs.getString(1); rs.close();
            stmt.close();

            // totals (sum over mwsts)
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT SUM(mwst_netto + mwst_betrag), SUM(bar_brutto), SUM(mwst_netto + mwst_betrag) - SUM(bar_brutto) FROM "+tableForMode("abrechnung_tag_mwst")+" " +
                       //   ^^^ Gesamt Brutto              ^^^ Gesamt Bar Brutto      ^^^ Gesamt EC Brutto = Ges. Brutto - Ges. Bar Brutto
                    "INNER JOIN "+tableForMode("abrechnung_tag")+" USING (id) "+
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
            connection.close();

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
            Vector<Integer> range = queryJahresAbrechnungRange(cur_year);
            HashMap<BigDecimal, Vector<BigDecimal>> sachen = queryJahresAbrechnung(range.get(0), range.get(1));
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
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    void queryAbrechnungenSpecial() {
        String maxDate = returnMaxAbechnungDate();
        Vector<String> years = returnAllNewYears(maxDate);
        logger.debug("maxDate in AbrechnungJahr: {}", maxDate);
        logger.debug("new years are: {}", years);
        insertNewYears(years);
    }

    Date createDate(String date) {
        SimpleDateFormat sdfIn = new SimpleDateFormat(this.dateInFormat);
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(sdfIn.parse(date));
            cal.set(Calendar.DAY_OF_MONTH, 15);
        } catch (ParseException ex) {
            logger.error("ParseException:", ex);
        }
        return cal.getTime();
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
