package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
//import java.sql.Date;
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
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

// JCalendarButton
import org.weltladen_bonn.pos.jcalendarbutton.JCalendarButton;
import java.util.Calendar;
import java.util.Date;

import org.weltladen_bonn.pos.WindowContent;
import org.weltladen_bonn.pos.MainWindowGrundlage;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AlteRechnungen extends Rechnungen implements ChangeListener {
    // Attribute:
    private static final Logger logger = LogManager.getLogger(AlteRechnungen.class);

    private JSpinner startSpinner;
    private JSpinner endSpinner;
    private SpinnerDateModel startDateModel;
    private SpinnerDateModel endDateModel;
    private JCalendarButton calButtStart;
    private JCalendarButton calButtEnd;
    private Date oneDayBeforeEarliestDate;
    private Date earliestDate;
    private Date latestDate;
    private JButton changeDateButton;
    private JButton resetButton;
    // private DateChooserPanel calPanel = new DateChooserPanel();

    // Methoden:
    /**
     * The constructor.
     */
    public AlteRechnungen(MariaDbPoolDataSource pool, MainWindowGrundlage mw) {
        super(pool, mw, "", "Alte Rechnungen");
        setFilterStr("WHERE v.rechnungs_nr <= "+
            "(SELECT MAX(rechnungs_nr_bis) FROM "+tableForMode("abrechnung_tag")+") ");
        queryEarliestRechnung();
        initiateSpinners();
        createAllPanel();
        showTable();
    }

    private void queryEarliestRechnung() {
        int day = 0;
        int month = 0;
        int year = 0;
        try {
            Connection connection = this.pool.getConnection();
            // Create statement for MySQL database
            Statement stmt = connection.createStatement();
            // Run MySQL command
            ResultSet rs = stmt
                    .executeQuery("SELECT DAY(MIN(verkaufsdatum)), MONTH(MIN(verkaufsdatum)), "
                            + "YEAR(MIN(verkaufsdatum)) FROM "+tableForMode("verkauf"));
            // Now do something with the ResultSet ...
            rs.next();
            day = rs.getInt(1);
            month = rs.getInt(2);
            year = rs.getInt(3);
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day - 1); // for strange reasons, we
                                                      // need day-1
        oneDayBeforeEarliestDate = calendar.getTime();
        calendar.set(Calendar.DAY_OF_MONTH, day);
        earliestDate = calendar.getTime();

        Date now = nowDate(); // current date
        if (year == 0) {
            oneDayBeforeEarliestDate = now;
            earliestDate = now;
        }
        latestDate = now;
        // final check:
        if (latestDate.before(earliestDate)) {
            Date tmp = earliestDate;
            earliestDate = latestDate;
            latestDate = tmp;
            oneDayBeforeEarliestDate = earliestDate;
        }
    }

    private void initiateSpinners() {
        logger.debug(
                "AlteRechnungen spinner values: " + oneDayBeforeEarliestDate + " <= " + earliestDate + " <= " + latestDate);
        startDateModel = new SpinnerDateModel(earliestDate, // Startwert
                oneDayBeforeEarliestDate, // kleinster Wert
                latestDate, // groesster Wert
                Calendar.YEAR);// ignored for user input
        startSpinner = new JSpinner(startDateModel);
        startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, "dd/MM/yyyy"));
        startSpinner.addChangeListener(this);
        calButtStart = new JCalendarButton(earliestDate);
        calButtStart.addChangeListener(this);
        endDateModel = new SpinnerDateModel(latestDate, // Startwert
                oneDayBeforeEarliestDate, // kleinster Wert
                latestDate, // groesster Wert
                Calendar.YEAR);// ignored for user input
        endSpinner = new JSpinner(endDateModel);
        endSpinner.setEditor(new JSpinner.DateEditor(endSpinner, "dd/MM/yyyy"));
        endSpinner.addChangeListener(this);
        calButtEnd = new JCalendarButton(latestDate);
        calButtEnd.addChangeListener(this);
    }

    void addOtherStuff() {
        JPanel datePanel = new JPanel();
        datePanel.setLayout(new FlowLayout());
        // datePanel.setMaximumSize(new Dimension(1024,30));
        JLabel startDateLabel = new JLabel("Startdatum:");
        datePanel.add(startDateLabel);
        startDateLabel.setLabelFor(startSpinner);
        datePanel.add(startSpinner);
        datePanel.add(calButtStart);
        datePanel.add(Box.createRigidArea(new Dimension(10, 0))); // add empty
                                                                  // space
        JLabel endDateLabel = new JLabel("Enddatum:");
        datePanel.add(endDateLabel);
        endDateLabel.setLabelFor(endSpinner);
        datePanel.add(endSpinner);
        datePanel.add(calButtEnd);
        datePanel.add(Box.createRigidArea(new Dimension(10, 0))); // add empty
                                                                  // space
        changeDateButton = new JButton(
                new ImageIcon(WindowContent.class.getResource("/resources/icons/refreshButtonSmall.gif")));
        changeDateButton.addActionListener(this);
        datePanel.add(changeDateButton);
        resetButton = new JButton("Reset");
        resetButton.addActionListener(this);
        datePanel.add(resetButton);
        headerPanel.add(datePanel);
    }

    void addButtonsToTable() {
        // create the buttons for each row:
        detailButtons = new Vector<JButton>();
        for (int i = 0; i < data.size(); i++) {
            detailButtons.add(new JButton("+"));
            detailButtons.get(i).addActionListener(this);
            myTable.setValueAt(detailButtons.get(i), i, 0);
        }
    }

    protected String getZKasseId() {
        // query the historical Z_KASSE_ID corresponding to the currently displayed
        // detail rechnung (rechnungsNr)
        String z_kasse_id = "";
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT z_kasse_id "+
                "FROM "+tableForMode("abrechnung_tag")+" "+
                "WHERE rechnungs_nr_von <= ? AND rechnungs_nr_bis >= ?"
            );
            pstmtSetInteger(pstmt, 1, rechnungsNr);
            pstmtSetInteger(pstmt, 2, rechnungsNr);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                z_kasse_id = rs.getString(1);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return z_kasse_id;
    }

    protected LinkedHashMap<String, String> getTSEStatusValues() {
        // query the historical TSE status values corresponding to the currently displayed
        // detail rechnung (rechnungsNr)
        LinkedHashMap<String, String> tseStatusValues = new LinkedHashMap<String, String>();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT "+
                "tse_serial, "+
                "tse_sig_algo, "+
                "tse_time_format, "+
                "tse_pd_encoding, "+
                "tse_public_key, "+
                "tse_cert_i, "+
                "tse_cert_ii "+
                "FROM "+tableForMode("abrechnung_tag_tse")+" "+
                "INNER JOIN "+tableForMode("abrechnung_tag")+" USING (id) "+
                "WHERE rechnungs_nr_von <= ? AND rechnungs_nr_bis >= ?"
            );
            pstmtSetInteger(pstmt, 1, rechnungsNr);
            pstmtSetInteger(pstmt, 2, rechnungsNr);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                tseStatusValues.put("Seriennummer der TSE (Hex)", rs.getString(1));
                tseStatusValues.put("Signatur-Algorithmus", rs.getString(2));
                tseStatusValues.put("Zeitformat", rs.getString(3));
                tseStatusValues.put("Encoding der processData-Strings", rs.getString(4));
                tseStatusValues.put("Öffentlicher Schlüssel (Base64)", rs.getString(5));
                String cert = rs.getString(6);
                String cert_ii = rs.getString(7);
                if (cert != null && cert_ii != null) {
                    cert = cert + cert_ii;
                }
                tseStatusValues.put("TSE-Zertifikat (Base64)", cert);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return tseStatusValues;
    }


    /**
     * * Each non abstract class that implements the ChangeListener must have
     * this method.
     *
     * @param e
     *            the change event.
     **/
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == startSpinner) {
            SpinnerModel dateModel = startSpinner.getModel();
            if (dateModel instanceof SpinnerDateModel) {
                calButtStart.setTargetDate(((SpinnerDateModel) dateModel).getDate());
            }
        }
        if (e.getSource() == endSpinner) {
            SpinnerModel dateModel = endSpinner.getModel();
            if (dateModel instanceof SpinnerDateModel) {
                calButtEnd.setTargetDate(((SpinnerDateModel) dateModel).getDate());
            }
        }
        if (e.getSource() == calButtStart) {
            SpinnerModel dateModel = startSpinner.getModel();
            Date newDate = calButtStart.getTargetDate();
            if (newDate.before(earliestDate)) {
                newDate = earliestDate;
                calButtStart.setTargetDate(newDate);
            }
            if (newDate.after(latestDate)) {
                newDate = latestDate;
                calButtStart.setTargetDate(newDate);
            }
            if (dateModel instanceof SpinnerDateModel) {
                if (newDate != null) {
                    ((SpinnerDateModel) dateModel).setValue(newDate);
                }
            }
        }
        if (e.getSource() == calButtEnd) {
            SpinnerModel dateModel = endSpinner.getModel();
            Date newDate = calButtEnd.getTargetDate();
            if (newDate.before(earliestDate)) {
                newDate = earliestDate;
                calButtEnd.setTargetDate(newDate);
            }
            if (newDate.after(latestDate)) {
                newDate = latestDate;
                calButtEnd.setTargetDate(newDate);
            }
            if (dateModel instanceof SpinnerDateModel) {
                if (newDate != null) {
                    ((SpinnerDateModel) dateModel).setValue(newDate);
                }
            }
        }
    }

    /**
     * * Each non abstract class that implements the ActionListener must have
     * this method.
     *
     * @param e
     *            the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (e.getSource() == changeDateButton) {
            SpinnerModel startDateModel = startSpinner.getModel();
            SpinnerModel endDateModel = endSpinner.getModel();
            Date startDate = null;
            if (startDateModel instanceof SpinnerDateModel) {
                startDate = ((SpinnerDateModel) startDateModel).getDate();
            }
            Date endDate = null;
            if (endDateModel instanceof SpinnerDateModel) {
                endDate = ((SpinnerDateModel) endDateModel).getDate();
            }
            java.sql.Date startDateSQL = new java.sql.Date(startDate.getTime());
            java.sql.Date endDateSQL = new java.sql.Date(endDate.getTime());
            String startDateStr = startDateSQL.toString();
            String endDateStr = endDateSQL.toString();
            this.filterStr = "WHERE DATE(v.verkaufsdatum) >= DATE('" + startDateStr + "') "
                    + "AND DATE(v.verkaufsdatum) <= DATE('" + endDateStr + "') ";
            updateTable();
            return;
        }
        if (e.getSource() == resetButton) {
            this.filterStr = "WHERE DATE(v.verkaufsdatum) < CURRENT_DATE() ";
            initiateSpinners();
            updateTable();
            return;
        }
    }
}
