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
import java.awt.event.*;

//import javax.swing.JFrame;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTable;
//import javax.swing.JTextArea;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;

import WeltladenDB.MainWindowGrundlage;

public class HeutigeRechnungen extends Rechnungen {
    // Attribute:
    private RechnungenTabbedPane tabbedPane;

    // Methoden:
    /**
     *    The constructor.
     *       */
    public HeutigeRechnungen(Connection conn, MainWindowGrundlage mw, RechnungenTabbedPane tp){
	super(conn, mw, "WHERE verkauf.verkaufsdatum > " +
                "IFNULL((SELECT MAX(zeitpunkt) FROM abrechnung_tag),'0001-01-01') AND "+
                "verkauf.storniert = FALSE ", "Heutige Rechnungen");
        tabbedPane = tp;
	showTable();
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
        myTable.setColEditableTrue(0); // first column has buttons
        myTable.setColEditableTrue(overviewLabels.size()-1); // last column has buttons
    }

    private void stornieren(int stornoRow) {
	Integer rechnungsnummer = Integer.parseInt(data.get(stornoRow).get(1));
	try {
            PreparedStatement pstmt = this.conn.prepareStatement(
		    "UPDATE verkauf SET verkauf.storniert = 1 WHERE verkauf.rechnungs_nr = ?"
		    );
            pstmtSetInteger(pstmt, 1, rechnungsnummer);
	    int result = pstmt.executeUpdate();
	    if (result != 0){
		JOptionPane.showMessageDialog(this, "Rechnung " + rechnungsnummer + " wurde storniert.",
			"Stornierung ausgeführt", JOptionPane.INFORMATION_MESSAGE);

                insertStornoIntoKassenstand(rechnungsnummer);
	    }
	    else {
		JOptionPane.showMessageDialog(this,
			"Fehler: Rechnung " + rechnungsnummer + " konnte nicht storniert werden.",
			"Fehler bei Stornierung", JOptionPane.ERROR_MESSAGE);
	    }
	    pstmt.close();
	} catch (SQLException ex) {
	    System.out.println("Exception: " + ex.getMessage());
	    ex.printStackTrace();
	}
	updateTable();
    }

    private void insertStornoIntoKassenstand(int rechnungsNr) {
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT SUM(ges_preis) FROM verkauf_details WHERE rechnungs_nr = ?"
                    );
            pstmtSetInteger(pstmt, 1, rechnungsNr);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); BigDecimal betrag = rs.getBigDecimal(1); rs.close();
            pstmt.close();
            BigDecimal alterKassenstand = mainWindow.retrieveKassenstand();
            BigDecimal neuerKassenstand = alterKassenstand.subtract(betrag);
            pstmt = this.conn.prepareStatement(
                    "INSERT INTO kassenstand SET rechnungs_nr = ?,"+
                    "buchungsdatum = NOW(), "+
                    "manuell = FALSE, neuer_kassenstand = ?, kommentar = ?"
                    );
            pstmtSetInteger(pstmt, 1, rechnungsNr);
            pstmt.setBigDecimal(2, neuerKassenstand);
            pstmt.setString(3, "Storno");
            int result = pstmt.executeUpdate();
            pstmt.close();
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Kassenstand konnte nicht geändert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            } else {
                mainWindow.updateBottomPanel();
            }
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e)
    {
	if (e.getSource() == removeDetailButton){
	    updateTable();
	    return;
	}
	final int numberOfRows = detailButtons.size();
	int detailRow=-1;
	int stornoRow=-1;
	for (int i=0; i<numberOfRows; i++){
	    if (e.getSource() == detailButtons.get(i) ){
		detailRow = i;
		break;
	    }
	}
	if (detailRow == -1){
	    for (int i=0; i<numberOfRows; i++){
		if (e.getSource() == stornoButtons.get(i) ){
		    stornoRow = i;
		    break;
		}
	    }
	}
	if (detailRow > -1){
	    showDetailTable(detailRow, this.titleStr);
	    return;
	}
	if (stornoRow > -1){
	    int answer = JOptionPane.showConfirmDialog(this,
		    "Rechnung " + (String)data.get(stornoRow).get(1) + " wirklich stornieren?", "Storno",
		    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
	    if (answer == JOptionPane.YES_OPTION){
		stornieren(stornoRow);
                tabbedPane.recreateTabbedPane();
            }
	    return;
	}
    }
}
