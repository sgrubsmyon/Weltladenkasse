package org.weltladen_bonn.pos;

// Basic Java stuff:
import java.util.*; // for Vector
import java.util.Date; // Date is ambiguous
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.*; // SQLException, DriverManager, Connection, Statement, ResultSet
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.weltladen_bonn.pos.SplashScreen;

// interface holding the window of the application GUI:
public abstract class MainWindowGrundlage extends JFrame {
    private static final Logger logger = LogManager.getLogger(MainWindowGrundlage.class);

    //***************************************************
    // Members
    //***************************************************
    public BaseClass bc;

    // Connection to MySQL database:
    public DBConnection dbconn = null;
    // public Connection conn = null;
    public MariaDbPoolDataSource pool = null;

    // Panels:
    protected JPanel holdAll = new JPanel(); // The top level panel which holds all.
    protected JPanel contentPanel = null; // The panel holding the main window content.
    protected JPanel bottomPanel = new JPanel(); // The bottom panel which holds date and kassenstand bar.

    //variables:
    protected String dateTime;

    //Labels:
    protected JLabel dateTimeLabel;
    protected JLabel kassenstandLabel;
    protected JLabel hostLabel;

    protected SplashScreen splash;

    //***************************************************
    // Methods
    //***************************************************

    /**
     *    The constructor.
     *       */
    public MainWindowGrundlage(SplashScreen spl, int nTasks) {
        this.splash = spl;
        this.splash.setStatusLabel("Lade Basis-Klasse...");
        this.splash.setProgress(1 * 100 / nTasks);
        bc = new BaseClass();
        initiate(nTasks);
    }

    private void initiate(int nTasks) {
        this.splash.setStatusLabel("Stelle Verbindung zur Datenbank her...");
        this.splash.setProgress(2 * 100 / nTasks);
        this.dbconn = new DBConnection(bc);
        if (!this.dbconn.connectionWorks) return;
        this.pool = this.dbconn.pool;

        this.splash.setStatusLabel("Baue Fenster auf...");
        this.splash.setProgress(3 * 100 / nTasks);
        holdAll.setLayout(new BorderLayout());

        bottomPanel.setLayout(new FlowLayout());
        holdAll.add(bottomPanel, BorderLayout.SOUTH);
        updateBottomPanel();

        this.getContentPane().add(holdAll, BorderLayout.CENTER);
    }

    protected String tableForMode(String tableName) {
        return bc.operationMode.equals("normal") ? tableName : "training_"+tableName;
    }

    public BigDecimal retrieveKassenstand() {
        BigDecimal ks = new BigDecimal("0.00");
        try {
            // Grab connection from the pool
            Connection connection = this.pool.getConnection();
            // Create statement for MySQL database
            Statement stmt = connection.createStatement();
            // Run MySQL command
            ResultSet rs = stmt.executeQuery(
                "SELECT neuer_kassenstand FROM "+tableForMode("kassenstand")+" WHERE "+
                "kassenstand_id = (SELECT MAX(kassenstand_id) FROM "+tableForMode("kassenstand")+")"
            );
            if ( rs.next() ){ ks = rs.getBigDecimal(1); }
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            JOptionPane.showMessageDialog(this,
                "Verbindung zum Datenbank-Server unterbrochen?\n"+
                "Fehlermeldung: "+ex.getMessage(),
                "Fehler", JOptionPane.ERROR_MESSAGE);
        }
        return ks;
    }


    public Integer retrieveKassenstandId() {
        Integer id = null;
        try {
            // Grab connection from the pool
            Connection connection = this.pool.getConnection();
            // Create statement for MySQL database
            Statement stmt = connection.createStatement();
            // Run MySQL command
            ResultSet rs = stmt.executeQuery(
                    "SELECT MAX(kassenstand_id) FROM "+tableForMode("kassenstand")
            );
            if ( rs.next() ){ id = rs.getInt(1); }
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            JOptionPane.showMessageDialog(this,
                "Verbindung zum Datenbank-Server unterbrochen?\n"+
                "Fehlermeldung: "+ex.getMessage(),
                "Fehler", JOptionPane.ERROR_MESSAGE);
        }
        return id;
    }


    // Setters & Getters:
    public void setContentPanel(JPanel panel) {
        this.contentPanel = panel;
        holdAll.add(this.contentPanel, BorderLayout.CENTER);
    }

    public void changeContentPanel(JPanel panel) {
//	this.contentPanel.removeAll();
        holdAll.remove(this.contentPanel);
        holdAll.revalidate();
        this.contentPanel = panel;
        holdAll.add(this.contentPanel, BorderLayout.CENTER);
    }

    public void updateBottomPanel() {
        holdAll.remove(this.bottomPanel);
        holdAll.revalidate();
        this.bottomPanel = new JPanel(); // The bottom panel which holds date and kassenstand bar.
        Date now = new Date();
        dateTime = now.toString();
        dateTimeLabel = new JLabel(dateTime);
        kassenstandLabel = new JLabel( bc.priceFormatter(retrieveKassenstand())+" "+bc.currencySymbol );
        hostLabel = new JLabel("("+bc.mysqlHost+")");
        this.bottomPanel.add(dateTimeLabel);
        this.bottomPanel.add(kassenstandLabel);
        this.bottomPanel.add(hostLabel);
        holdAll.add(this.bottomPanel, BorderLayout.SOUTH);
    }

}
