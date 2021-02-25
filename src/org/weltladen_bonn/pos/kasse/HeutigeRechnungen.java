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
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import java.awt.event.*;

//import javax.swing.JFrame;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTable;
//import javax.swing.JTextArea;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;

import org.weltladen_bonn.pos.MainWindowGrundlage;
import org.weltladen_bonn.pos.kasse.WeltladenTSE.TSETransaction;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HeutigeRechnungen extends Rechnungen {
    // Attribute:
    private static final Logger logger = LogManager.getLogger(HeutigeRechnungen.class);

    private RechnungenTabbedPane tabbedPane;

    // Methoden:
    /**
     *    The constructor.
     *       */
    public HeutigeRechnungen(MariaDbPoolDataSource pool, MainWindowGrundlage mw, RechnungenTabbedPane tp){
	    super(pool, mw, "", "Heutige Rechnungen");
        setFilterStr("WHERE v.rechnungs_nr > " +
            "IFNULL((SELECT MAX(rechnungs_nr_bis) FROM "+tableForMode("abrechnung_tag")+"), 0) ");
        tabbedPane = tp;
	    showTable();
    }

    void addOtherStuff() {
    }

    void addButtonsToTable(){
        // create the buttons for each row:
        detailButtons = new Vector<JButton>();
        stornoButtons = new Vector<JButton>();
        for (int i=0; i<data.size(); i++){
            detailButtons.add(new JButton("+"));
            detailButtons.get(i).addActionListener(this);
            myTable.setValueAt( detailButtons.get(i), i, 0 );
            stornoButtons.add(null);
            if (data.get(i).get(2) == null) { // exclude canceling bookings since they cannot be canceled
                if (!stornoStatuses.get(i)) { // exclude already canceled bookings
                    stornoButtons.set(i, new JButton("Storno"));
                    stornoButtons.get(i).addActionListener(this);
                    myTable.setValueAt( stornoButtons.get(i), i, overviewLabels.size()-1 );
                }
            }
        }
    }

    private void stornieren(int stornoRow) {
        Integer rechNr = Integer.parseInt(data.get(stornoRow).get(1).toString());
        String zahlMod = data.get(stornoRow).get(4).toString();
        Integer stornoRechNr = insertStornoIntoVerkauf(rechNr, zahlMod);
        updateTable(); // Update the table so that you can load the details of the storno booking.
                       // This saves us a massive amount of code rewrite in order to fetch all the details
                       // of the booking (kassierArtikel, mwstValues etc.)
        showDetailTable(0, this.titleStr);
        insertStornoIntoTSE(stornoRechNr);
        if (stornoRechNr != null && zahlMod.equals("Bar")) { // if Barzahlung
            insertStornoIntoKassenstand(rechNr, stornoRechNr);
            printQuittung();
        } else { // EC-Zahlung
            printQuittung();
            // Thread.sleep(5000); // wait for 5 seconds, no, printer is too slow anyway and this blocks UI unnecessarily
            printQuittung();
        }
    }

    private Integer insertStornoIntoVerkauf(int rechNr, String zahlMod) {
        Integer stornoRechNr = null;
        try { 
            Connection connection = this.pool.getConnection();
            
            // insert Gegenbuchung into verkauf
            PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO "+tableForMode("verkauf")+" SET verkaufsdatum = NOW(), "+
                "storno_von = ?, ec_zahlung = ?, kunde_gibt = NULL"
            );
            pstmtSetInteger(pstmt, 1, rechNr);
            pstmtSetBoolean(pstmt, 2, !zahlMod.equals("Bar"));
            int result1 = pstmt.executeUpdate();

            // retrieve the storno rechnungs_nr
            pstmt = connection.prepareStatement(
                "SELECT rechnungs_nr FROM "+tableForMode("verkauf")+" WHERE storno_von = ?"
            );
            pstmtSetInteger(pstmt, 1, rechNr);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); stornoRechNr = rs.getInt(1); rs.close();
            
            // insert Gegenbuchung (negated values) into verkauf_mwst
            pstmt = connection.prepareStatement(
                "INSERT INTO "+tableForMode("verkauf_mwst")+" SELECT "+
                "?, mwst_satz, -mwst_netto, -mwst_betrag "+
                "FROM "+tableForMode("verkauf_mwst")+" "+
                "WHERE rechnungs_nr = ?"
            );
            pstmtSetInteger(pstmt, 1, stornoRechNr);
            pstmtSetInteger(pstmt, 2, rechNr);
            int result2 = pstmt.executeUpdate();

            // insert Gegenbuchung (negated stueckzahl) into verkauf_details
            pstmt = connection.prepareStatement(
                "INSERT INTO "+tableForMode("verkauf_details")+" SELECT NULL, "+
                "?, position, artikel_id, rabatt_id, -stueckzahl, -ges_preis, mwst_satz "+
                "FROM "+tableForMode("verkauf_details")+" "+
                "WHERE rechnungs_nr = ?"
            );
            pstmtSetInteger(pstmt, 1, stornoRechNr);
            pstmtSetInteger(pstmt, 2, rechNr);
            int result3 = pstmt.executeUpdate();

            if (result1 != 0 && result2 != 0 && result3 != 0){
                JOptionPane.showMessageDialog(this, "Rechnung " + rechNr + " wurde storniert.",
                    "Stornierung ausgeführt", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Fehler: Rechnung " + rechNr + " konnte nicht storniert werden.",
                    "Fehler bei Stornierung", JOptionPane.ERROR_MESSAGE);
            }

            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return stornoRechNr;
    }

    private void insertStornoIntoTSE(int stornoRechNr) {
        tse.startTransaction();

        // Send data to TSE:
        Vector<String> zahlung = new Vector<String>();
        zahlung.add("Bar"); // zahlMod.equals("Bar") ? "Bar" : "Unbar" // Weltladen can only pay back in bar, so always use "Bar" here, even in case of EC payment
        zahlung.add( bc.priceFormatterIntern(getTotalPrice()) );
        // Omit currency code because it's always in EUR
        Vector<Vector<String>> zahlungen = new Vector<Vector<String>>();
        zahlungen.add(zahlung);
        HashMap<Integer, Vector<BigDecimal>> mwstIDsAndValues = getAllCurrentMwstValuesByID();
        
        // always finish the transaction, also when TSE is not in use (has failed), in which case end date is determined by Kasse
        tse.finishTransaction(
            stornoRechNr,
            mwstIDsAndValues.get(3) != null ? mwstIDsAndValues.get(3).get(3) : null, // steuer_allgemein = mwst_id: 3 = 19% MwSt
            mwstIDsAndValues.get(2) != null ? mwstIDsAndValues.get(2).get(3) : null, // steuer_ermaessigt = mwst_id: 2 = 7% MwSt
            mwstIDsAndValues.get(5) != null ? mwstIDsAndValues.get(5).get(3) : null, // steuer_durchschnitt_nr3 = mwst_id: 5 = 10,7% MwSt
            mwstIDsAndValues.get(4) != null ? mwstIDsAndValues.get(4).get(3) : null, // steuer_durchschnitt_nr1 = mwst_id: 4 = 5,5% MwSt
            mwstIDsAndValues.get(1) != null ? mwstIDsAndValues.get(1).get(3) : null, // steuer_null = mwst_id: 1 = 0% MwSt
            zahlungen
        );
    }

    private void insertStornoIntoKassenstand(int rechNr, int stornoRechNr) {
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT SUM(ges_preis) FROM "+tableForMode("verkauf_details")+" WHERE rechnungs_nr = ?"
            );
            pstmtSetInteger(pstmt, 1, rechNr);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); BigDecimal betrag = rs.getBigDecimal(1); rs.close();
            pstmt.close();
            BigDecimal alterKassenstand = mainWindow.retrieveKassenstand();
            BigDecimal neuerKassenstand = alterKassenstand.subtract(betrag);
            pstmt = connection.prepareStatement(
                "INSERT INTO "+tableForMode("kassenstand")+" SET rechnungs_nr = ?,"+
                "buchungsdatum = NOW(), "+
                "manuell = FALSE, neuer_kassenstand = ?, kommentar = ?"
            );
            pstmtSetInteger(pstmt, 1, stornoRechNr);
            pstmt.setBigDecimal(2, neuerKassenstand);
            pstmt.setString(3, "Storno");
            int result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Kassenstand konnte nicht geändert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            } else {
                mainWindow.updateBottomPanel();
            }
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        final int numberOfRows = stornoButtons.size();
        int stornoRow=-1;
        for (int i=0; i<numberOfRows; i++){
            if (e.getSource() == stornoButtons.get(i) ){
                stornoRow = i;
                break;
            }
        }
	    if (stornoRow > -1){
            int answer = JOptionPane.showConfirmDialog(this,
                    "Rechnung " + (String) data.get(stornoRow).get(1) + " wirklich stornieren?", "Storno",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                stornieren(stornoRow);
                tabbedPane.recreateTabbedPane();
            }
            return;
        }
    }
}
