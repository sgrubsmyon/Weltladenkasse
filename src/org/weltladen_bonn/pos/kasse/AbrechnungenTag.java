package org.weltladen_bonn.pos.kasse;

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

import org.weltladen_bonn.pos.MainWindowGrundlage;

public class AbrechnungenTag extends Abrechnungen {
    // Attribute:
    private AbrechnungenTabbedPane abrechTabbedPane;
    private TabbedPane tabbedPane;

    private JButton submitButton;
    private boolean submitButtonEnabled;

    private String selectedZeitpunkt = null;
    private HashMap<BigDecimal, Integer> zaehlprotokoll = null;

    // Methoden:
    /**
     *    The constructor.
     *       */
    public AbrechnungenTag(Connection conn, MainWindowGrundlage mw, AbrechnungenTabbedPane atp, TabbedPane tp){
        super(conn, mw, "", "Tagesabrechnung", "yyyy-MM-dd HH:mm:ss", "dd.MM. HH:mm (E)",
                "zeitpunkt", "abrechnung_tag");
        this.abrechTabbedPane = atp;
        this.tabbedPane = tp;
	showTable();
    }

    void setSelectedZeitpunkt(String zp) {
        this.selectedZeitpunkt = zp;
    }

    void setZaehlprotokoll(HashMap<BigDecimal, Integer> zp) {
        this.zaehlprotokoll = zp;
    }

    void addOtherStuff() {
        JPanel otherPanel = new JPanel();
        submitButton = new JButton("Tagesabrechnung machen");
        submitButton.setEnabled(submitButtonEnabled);
        submitButton.addActionListener(this);
        otherPanel.add(submitButton);
        otherPanel.add(new JLabel("(Zahlen in rot werden als neue Tagesabrechnung gespeichert.)"));
        headerPanel.add(otherPanel);
    }

    String queryEarliestVerkauf() {
        String date = "";
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MIN(verkaufsdatum) "+
                    "FROM verkauf WHERE storniert = FALSE AND verkaufsdatum > "+
                    "IFNULL((SELECT MAX(zeitpunkt_real) FROM abrechnung_tag),'0001-01-01')");
            rs.next(); date = rs.getString(1); rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return date;
    }

    String queryLatestVerkauf() {
        String date = "";
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MAX(verkaufsdatum) "+
                    "FROM verkauf WHERE storniert = FALSE AND verkaufsdatum > "+
                    "IFNULL((SELECT MAX(zeitpunkt_real) FROM abrechnung_tag), '0001-01-01')");
            rs.next(); date = rs.getString(1); rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return date;
    }

    void queryIncompleteAbrechnung() { // create new abrechnung (for display) from time of last abrechnung until now
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
                    "IFNULL((SELECT MAX(zeitpunkt_real) FROM abrechnung_tag), '0001-01-01') AND ec_zahlung = FALSE " +
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

    void showSelectZeitpunktDialog(DateTime firstDate, DateTime lastDate, DateTime nowDate) {
        JDialog dialog = new JDialog(this.mainWindow, "Zeitpunkt manuell auswählen", true);
        SelectZeitpunktForAbrechnungDialog selZeitpunkt = 
            new SelectZeitpunktForAbrechnungDialog(this.conn, this.mainWindow,
                    this, dialog, firstDate, lastDate, nowDate);
        dialog.getContentPane().add(selZeitpunkt, BorderLayout.CENTER);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setVisible(true);
    }

    String decideOnZeitpunkt(String firstDate, String lastDate, String nowDate) {
        DateTime firstD = new DateTime(firstDate);
        DateTime lastD = new DateTime(lastDate);
        DateTime nowD = new DateTime(nowDate);
        if ( firstD.isSameDayAs(nowD) ){
            // everything is as it should be:
            // all purchases from this abrechnung on the same day
            // simply use now as zeitpunkt of abrechnung
            return nowDate;
        } else {
            // if abrechnung spans more than one day:
            // show dialog window telling user about first and last date and
            // asking her to specify the desired zeitpunkt of abrechnung
            showSelectZeitpunktDialog(firstD, lastD, nowD);
            return this.selectedZeitpunkt;
        }
    }

    void deleteAbrechnungIfNeedBe(String abrechnungsName, String timeName,
            String zeitpunktParsing, String zeitpunkt) {
        /** Delete Monats-/Jahresabrechnung if there was already one for the given zeitpunkt */
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT COUNT(*) FROM "+abrechnungsName+" "+
                    "WHERE "+timeName+" = "+zeitpunktParsing
                    );
            pstmt.setString(1, zeitpunkt);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); int count = rs.getInt(1); rs.close();
            pstmt.close();
            if (count > 0){
                pstmt = this.conn.prepareStatement(
                        "DELETE FROM "+abrechnungsName+" "+
                        "WHERE "+timeName+" = "+zeitpunktParsing
                        );
                pstmt.setString(1, zeitpunkt);
                int result = pstmt.executeUpdate();
                pstmt.close();
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Alte Abrechnung konnte nicht aus Tabelle "+
                            "'"+abrechnungsName+"' gelöscht werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private Integer insertTagesAbrechnung() {
        /** create new abrechnung (and save in DB) from time of last abrechnung until now */
        Integer id = null;
        try {
            id = id();
            String firstDate = queryEarliestVerkauf();
            String lastDate = queryLatestVerkauf();
            String nowDate = now();
            String zeitpunkt = decideOnZeitpunkt(firstDate, lastDate, nowDate);
            System.out.println("Selected Zeitpunkt: "+zeitpunkt);
            if (zeitpunkt == null){
                System.out.println("insertTagesAbrechnung was cancelled!");
                return null; // don't do anything, user cancelled (or did not select date properly)
            }
            // get ID of current kassenstand (highest ID due to auto-increment)
            Integer kassenstand_id = mainWindow.retrieveKassenstandId();
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
                //System.out.println("INSERT INTO abrechnung_tag: id: "+id+
                //        "  "+mwst_satz+"  "+mwst_netto+"  "+mwst_betrag+
                //        "   "+bar_brutto);
                PreparedStatement pstmt = this.conn.prepareStatement(
                        "INSERT INTO abrechnung_tag SET id = ?, "+
                                "zeitpunkt = ?, "+
                                "zeitpunkt_real = ?, "+
                                "mwst_satz = ?, "+
                                "mwst_netto = ?, "+
                                "mwst_betrag = ?, "+
                                "bar_brutto = ?, "+
                                "kassenstand_id = ?"
                );
                pstmtSetInteger(pstmt, 1, id);
                pstmt.setString(2, zeitpunkt);
                pstmt.setString(3, nowDate);
                pstmt.setBigDecimal(4, mwst_satz);
                pstmt.setBigDecimal(5, mwst_netto);
                pstmt.setBigDecimal(6, mwst_betrag);
                pstmt.setBigDecimal(7, bar_brutto);
                pstmtSetInteger(pstmt, 8, kassenstand_id);
                int result = pstmt.executeUpdate();
                pstmt.close();
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Tagesabrechnung konnte nicht gespeichert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    id = null;
                }
            }
            // NEED TO REDO Monats/Jahresabrechnung if needed (check if zeitpunkt lies in old month/year)!!!
            deleteAbrechnungIfNeedBe("abrechnung_monat", "monat", "DATE_FORMAT(?, '%Y-%m-01')", zeitpunkt);
            deleteAbrechnungIfNeedBe("abrechnung_jahr", "jahr", "YEAR(?)", zeitpunkt);
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Fehler: Tagesabrechnung konnte nicht gespeichert werden.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            id = null;
        }
        return id;
    }

    private void insertZaehlprotokoll(Integer abrechnung_tag_id) {
        try {
            for ( Map.Entry<BigDecimal, Integer> entry : zaehlprotokoll.entrySet() ){
                BigDecimal wert = entry.getKey();
                Integer anzahl = entry.getValue();
                PreparedStatement pstmt = this.conn.prepareStatement(
                        "INSERT INTO zaehlprotokoll SET abrechnung_tag_id = ?, "+
                                "anzahl = ?, "+
                                "einheit = ?"
                );
                pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
                pstmtSetInteger(pstmt, 2, anzahl);
                pstmt.setBigDecimal(3, wert);
                int result = pstmt.executeUpdate();
                pstmt.close();
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Zählprotokoll konnte nicht gespeichert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Fehler: Zählprotokoll konnte nicht gespeichert werden.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    void showZaehlprotokollDialog() {
        JDialog dialog = new JDialog(this.mainWindow, "Erfassung des Kassenbestandes", true);
        ZaehlprotokollDialog zaehlprotokoll = new ZaehlprotokollDialog(this.conn, this.mainWindow, this, dialog);
        dialog.getContentPane().add(zaehlprotokoll, BorderLayout.CENTER);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setVisible(true);
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
            showZaehlprotokollDialog();
            if (this.zaehlprotokoll != null) {
                tabbedPane.kassenstandNeedsToChange = true;
                Integer id = insertTagesAbrechnung();
                if (id != null) {
                    insertZaehlprotokoll(id);
                }
                abrechTabbedPane.recreateTabbedPane();
            }
        }
    }
}
