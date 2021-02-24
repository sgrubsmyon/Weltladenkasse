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
            stornoButtons.add(new JButton("Storno"));
            stornoButtons.get(i).addActionListener(this);
            myTable.setValueAt( stornoButtons.get(i), i, overviewLabels.size()-1 );
        }
    }

    private void stornieren(int stornoRow) {
        Integer rechnungsnummer = Integer.parseInt(data.get(stornoRow).get(1).toString());
        String zahlungsModus = data.get(stornoRow).get(3).toString();
        try { 
            Connection connection = this.pool.getConnection();
            
            // insert Gegenbuchung into verkauf
            PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO "+tableForMode("verkauf")+" SET verkaufsdatum = NOW(), "+
                "storno_von = ?, ec_zahlung = ?, kunde_gibt = NULL"
            );
            pstmtSetInteger(pstmt, 1, rechnungsnummer);
            pstmtSetBoolean(pstmt, 2, !zahlungsModus.equals("Bar"));
            int result1 = pstmt.executeUpdate();

            // retrieve the storno rechnungs_nr
            pstmt = connection.prepareStatement(
                "SELECT rechnungs_nr FROM "+tableForMode("verkauf")+" WHERE storno_von = ?"
            );
            pstmtSetInteger(pstmt, 1, rechnungsnummer);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); int stornorechnungsnummer = rs.getInt(1); rs.close();
            
            // insert Gegenbuchung (negated values) into verkauf_mwst
            pstmt = connection.prepareStatement(
                "INSERT INTO "+tableForMode("verkauf_mwst")+" SELECT "+
                "?, mwst_satz, -mwst_netto, -mwst_betrag "+
                "FROM "+tableForMode("verkauf_mwst")+" "+
                "WHERE rechnungs_nr = ?"
            );
            pstmtSetInteger(pstmt, 1, stornorechnungsnummer);
            pstmtSetInteger(pstmt, 2, rechnungsnummer);
            int result2 = pstmt.executeUpdate();

            // insert Gegenbuchung (negated stueckzahl) into verkauf_details
            pstmt = connection.prepareStatement(
                "INSERT INTO "+tableForMode("verkauf_details")+" SELECT NULL, "+
                "?, position, artikel_id, rabatt_id, -stueckzahl, -ges_preis, mwst_satz "+
                "FROM "+tableForMode("verkauf_details")+" "+
                "WHERE rechnungs_nr = ?"
            );
            pstmtSetInteger(pstmt, 1, stornorechnungsnummer);
            pstmtSetInteger(pstmt, 2, rechnungsnummer);
            int result3 = pstmt.executeUpdate();

            if (result1 != 0 && result2 != 0 && result3 != 0){
                JOptionPane.showMessageDialog(this, "Rechnung " + rechnungsnummer + " wurde storniert.",
                    "Stornierung ausgeführt", JOptionPane.INFORMATION_MESSAGE);

                if (zahlungsModus.equals("Bar")) { // if Barzahlung
                    insertStornoIntoKassenstand(rechnungsnummer, stornorechnungsnummer);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                    "Fehler: Rechnung " + rechnungsnummer + " konnte nicht storniert werden.",
                    "Fehler bei Stornierung", JOptionPane.ERROR_MESSAGE);
            }

            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        updateTable();
    }

    private void insertStornoIntoKassenstand(int rechnungsNr, int stornoRechnungsNr) {
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT SUM(ges_preis) FROM "+tableForMode("verkauf_details")+" WHERE rechnungs_nr = ?"
                    );
            pstmtSetInteger(pstmt, 1, rechnungsNr);
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
            pstmtSetInteger(pstmt, 1, stornoRechnungsNr);
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
