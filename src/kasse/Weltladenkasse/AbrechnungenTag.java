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

// DateTime from date4j (http://www.date4j.net/javadoc/index.html)
import hirondelle.date4j.DateTime;

import WeltladenDB.MainWindowGrundlage;

public class AbrechnungenTag extends Abrechnungen {
    // Attribute:
    private AbrechnungenTabbedPane tabbedPane;

    private JButton submitButton;
    private boolean submitButtonEnabled;

    // Methoden:
    /**
     *    The constructor.
     *       */
    public AbrechnungenTag(Connection conn, MainWindowGrundlage mw, AbrechnungenTabbedPane tp){
        super(conn, mw, "", "Tagesabrechnung", "yyyy-MM-dd HH:mm:ss", "dd.MM. (E)",
                "dd.MM.yyyy HH:mm:ss (E)", "zeitpunkt", "abrechnung_tag");
        tabbedPane = tp;
	showTable();
        // just for testing:
        showSelectZeitpunktDialog(new DateTime("2015-01-13 20:01:32"),
                new DateTime("2015-01-14 18:00:00"));
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

    String queryEarliestVerkauf() {
        String date = "";
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MIN(verkaufsdatum) "+
                    "FROM verkauf WHERE storniert = FALSE AND verkaufsdatum > "+
                    "IFNULL((SELECT MAX(zeitpunkt) FROM abrechnung_tag),'0001-01-01')");
            rs.next(); date = rs.getString(1); rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return date;
    }

    String now() {
        String date = "";
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT NOW()");
            rs.next(); date = rs.getString(1); rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return date;
    }

    void queryIncompleteAbrechnung() { // create new abrechnung (for display) from time of last abrechnung until now
        System.out.println("queryIncompleteAbrechnung()");
        String date = now();

        // for filling the diplayed table:
        // first, get the totals:
        Vector<BigDecimal> values = queryIncompleteAbrechnungTag_Totals();
        // store in map under date
        incompleteAbrechnungsDate = date;
        incompleteAbrechnungsTotals = values;
        incompleteAbrechnungsVATs = new HashMap<BigDecimal, Vector<BigDecimal>>();

        // second, get values grouped by mwst
        HashMap<BigDecimal, Vector<BigDecimal>> abrechnungNettoBetrag = queryIncompleteAbrechnungTag_VATs();
        int rowCount = 0;
        for ( Map.Entry< BigDecimal, Vector<BigDecimal> > entry : abrechnungNettoBetrag.entrySet() ){
            BigDecimal mwst = entry.getKey();
            Vector<BigDecimal> mwstValues = entry.getValue();
            incompleteAbrechnungsVATs.put(mwst, mwstValues);
            mwstSet.add(mwst);
            rowCount++;

            submitButtonEnabled = true;
        }
        if ( rowCount == 0 ){ // empty, there are no verkaeufe!!!
            submitButtonEnabled = false;
        }
    }

    HashMap<BigDecimal, BigDecimal> queryIncompleteAbrechnung_BarBruttoVATs() {
        HashMap<BigDecimal, BigDecimal> abrechnungBarBrutto = new HashMap<BigDecimal, BigDecimal>();
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT mwst_satz, SUM(ges_preis) AS bar_brutto " +
                    "FROM verkauf_details INNER JOIN verkauf USING (rechnungs_nr) " +
                    "WHERE storniert = FALSE AND verkaufsdatum > " +
                    "IFNULL((SELECT MAX(zeitpunkt) FROM abrechnung_tag), '0001-01-01') AND ec_zahlung = FALSE " +
                    "GROUP BY mwst_satz"
                    );
            while (rs.next()) {
                BigDecimal mwst_satz = rs.getBigDecimal(1);
                BigDecimal bar_brutto = rs.getBigDecimal(2);
                abrechnungBarBrutto.put(mwst_satz, bar_brutto);
                //System.out.println(mwst_satz+"  "+bar_brutto);
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return abrechnungBarBrutto;
    }

    void showSelectZeitpunktDialog(DateTime firstDate, DateTime lastDate) {
        JDialog dialog = new JDialog(this.mainWindow, "Zeitpunkt manuell auswählen", true);
        SelectZeitpunktForAbrechnungDialog selZeitpunkt = new SelectZeitpunktForAbrechnungDialog(this.conn, this.mainWindow, dialog, firstDate, lastDate);
        dialog.getContentPane().add(selZeitpunkt, BorderLayout.CENTER);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        //WindowAdapterDialog wad = new WindowAdapterDialog(selZeitpunkt, dialog, "Achtung: Neue Artikel gehen verloren (noch nicht abgeschickt).\nWirklich schließen?");
        //dialog.addWindowListener(wad);
        dialog.pack();
        dialog.setVisible(true);
    }

    String decideOnZeitpunkt(String firstDate, String lastDate) {
        DateTime firstD = new DateTime(firstDate);
        DateTime lastD = new DateTime(lastDate);
        if ( firstD.isSameDayAs(lastD) ){
            // everything is as it should be:
            // all purchases from this abrechnung on the same day
            // simply use the lastDate (now) as zeitpunkt for abrechnung
            return lastDate;
        }
        // if abrechnung spans more than one day:
        // show dialog window telling user about first and last date and
        // asking her to specify the desired zeitpunkt of abrechnung
        showSelectZeitpunktDialog(firstD, lastD);
        return lastDate;
    }

    void insertTagesAbrechnung() { // create new abrechnung (and save in DB) from time of last abrechnung until now
        try {
            Integer id = id();
            String firstDate = queryEarliestVerkauf();
            String lastDate = now();
            String zeitpunkt = decideOnZeitpunkt(firstDate, lastDate);
            // get netto values grouped by mwst:
            HashMap<BigDecimal, Vector<BigDecimal>> abrechnungNettoBetrag = queryIncompleteAbrechnungTag_VATs();
            // get totals (bar brutto) grouped by mwst:
            HashMap<BigDecimal, BigDecimal> abrechnungBarBrutto = queryIncompleteAbrechnung_BarBruttoVATs();
            //System.out.println("mwst_satz  mwst_netto  mwst_betrag  bar_brutto");
            //System.out.println("----------------------------------------------");
            for ( Map.Entry< BigDecimal, Vector<BigDecimal> > entry : abrechnungNettoBetrag.entrySet() ){
                BigDecimal mwst_satz = entry.getKey();
                Vector<BigDecimal> values = entry.getValue();
                BigDecimal mwst_netto = values.get(1);
                BigDecimal mwst_betrag = values.get(2);
                BigDecimal bar_brutto = new BigDecimal("0.00");
                if ( abrechnungBarBrutto.containsKey(mwst_satz) ){
                    bar_brutto = abrechnungBarBrutto.get(mwst_satz);
                }
                System.out.println(mwst_satz+"  "+mwst_netto+"  "+mwst_betrag+"   "+bar_brutto);
                PreparedStatement pstmt = this.conn.prepareStatement(
                        "INSERT INTO abrechnung_tag SET id = ?, zeitpunkt = ?, "+
                        "mwst_satz = ?, "+
                        "mwst_netto = ?, "+
                        "mwst_betrag = ?, "+
                        "bar_brutto = ?"
                        );
                pstmt.setInt(1, id);
                pstmt.setString(2, zeitpunkt);
                pstmt.setBigDecimal(3, mwst_satz);
                pstmt.setBigDecimal(4, mwst_netto);
                pstmt.setBigDecimal(5, mwst_betrag);
                pstmt.setBigDecimal(6, bar_brutto);
                int result = pstmt.executeUpdate();
                pstmt.close();
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
            tabbedPane.recreateTabbedPane();
	    return;
	}
    }
}
