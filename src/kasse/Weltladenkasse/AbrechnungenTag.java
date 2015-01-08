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
    private AbrechnungenTabbedPane tabbedPane;

    private JButton submitButton;
    private boolean submitButtonEnabled;

    // Methoden:
    /**
     *    The constructor.
     *       */
    public AbrechnungenTag(Connection conn, MainWindowGrundlage mw, AbrechnungenTabbedPane tp){
	super(conn, mw, "", "Tagesabrechnungen", "yyyy-MM-dd HH:mm:ss", "dd.MM. (E)", "zeitpunkt", "abrechnung_tag");
        tabbedPane = tp;
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
        abrechnungsDates.add(date);
        abrechnungsTotals.add(values);
        abrechnungsVATs.add(new HashMap<BigDecimal, Vector<BigDecimal>>());

        // second, get values grouped by mwst
        HashMap<BigDecimal, Vector<BigDecimal>> abrechnungNettoBetrag = queryIncompleteAbrechnungTag_VATs();
        int rowCount = 0;
        for ( Map.Entry< BigDecimal, Vector<BigDecimal> > entry : abrechnungNettoBetrag.entrySet() ){
            BigDecimal mwst = entry.getKey();
            Vector<BigDecimal> mwstValues = entry.getValue();
            abrechnungsVATs.lastElement().put(mwst, mwstValues);
            mwstSet.add(mwst);
            rowCount++;

            submitButtonEnabled = true;
        }
        if ( rowCount == 0 ){ // empty, there are no verkaeufe!!!
            submitButtonEnabled = false;
        }
    }

    void insertTagesAbrechnung() { // create new abrechnung (and save in DB) from time of last abrechnung until now
        try {
            String date = now();
            // get netto values grouped by mwst:
            HashMap<BigDecimal, Vector<BigDecimal>> abrechnungNettoBetrag = queryIncompleteAbrechnungTag_VATs();
            // get totals (bar brutto) grouped by mwst:
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT mwst_satz, SUM(ges_preis) AS bar_brutto " +
                    "FROM verkauf_details INNER JOIN verkauf USING (rechnungs_nr) " +
                    "WHERE storniert = FALSE AND verkaufsdatum > " +
                    "IFNULL((SELECT MAX(zeitpunkt) FROM abrechnung_tag), '0001-01-01') AND ec_zahlung = FALSE " +
                    "GROUP BY mwst_satz"
                    );
            HashMap<BigDecimal, BigDecimal> abrechnungBarBrutto = new HashMap<BigDecimal, BigDecimal>();
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
                        "INSERT INTO abrechnung_tag SET zeitpunkt = ?, mwst_satz = ?, " +
                        "mwst_netto = ?, mwst_betrag = ?, "+
                        "bar_brutto = ?"
                        );
                pstmt.setString(1, date);
                pstmt.setBigDecimal(2, mwst_satz);
                pstmt.setBigDecimal(3, mwst_netto);
                pstmt.setBigDecimal(4, mwst_betrag);
                pstmt.setBigDecimal(5, bar_brutto);
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
            //tabbedPane.recreateTabbedPane();
            updateTable();
	    return;
	}
    }
}
