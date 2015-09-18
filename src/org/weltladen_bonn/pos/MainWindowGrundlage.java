package org.weltladen_bonn.pos;

// Basic Java stuff:
import java.util.*; // for Vector
import java.util.Date; // Date is ambiguous
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.*; // SQLException, DriverManager, Connection, Statement, ResultSet

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

// interface holding the window of the application GUI:
public abstract class MainWindowGrundlage extends JFrame {
    //***************************************************
    // Members
    //***************************************************
    public BaseClass bc;

    // Connection to MySQL database:
    public DBConnection dbconn = null;
    public Connection conn = null;

    // Panels:
    protected JPanel holdAll = new JPanel(); // The top level panel which holds all.
    protected JPanel contentPanel = null; // The panel holding the main window content.
    protected JPanel bottomPanel = new JPanel(); // The bottom panel which holds date and kassenstand bar.

    //variables:
    protected String dateTime;

    //Labels:
    protected JLabel dateTimeLabel;
    protected JLabel kassenstandLabel;


    //***************************************************
    // Methods
    //***************************************************

    /**
     *    The constructor.
     *       */
    public MainWindowGrundlage() {
        bc = new BaseClass();
        initiate();
    }

    public void initiate(){
        this.dbconn = new DBConnection(bc);
        if (!dbconn.connectionWorks) return;
        this.conn = dbconn.conn;

	holdAll.setLayout(new BorderLayout());

	bottomPanel.setLayout(new FlowLayout());

	Date now = new Date();
	dateTime = now.toString();
	dateTimeLabel = new JLabel(this.dateTime);
	kassenstandLabel = new JLabel( bc.priceFormatter(retrieveKassenstand())+" "+bc.currencySymbol );
	bottomPanel.add(dateTimeLabel);
	bottomPanel.add(kassenstandLabel);

	holdAll.add(bottomPanel, BorderLayout.SOUTH);
	this.getContentPane().add(holdAll, BorderLayout.CENTER);
    }



    public BigDecimal retrieveKassenstand(){
        BigDecimal ks = new BigDecimal("0.00");
	try {
	    // Create statement for MySQL database
	    Statement stmt = this.conn.createStatement();
	    // Run MySQL command
	    ResultSet rs = stmt.executeQuery(
                    "SELECT neuer_kassenstand FROM kassenstand WHERE "+
                    "kassenstand_id = (SELECT MAX(kassenstand_id) FROM kassenstand)"
                    );
	    if ( rs.next() ){ ks = rs.getBigDecimal(1); }
	    rs.close();
	    stmt.close();
	} catch (SQLException ex) {
	    System.out.println("Exception: " + ex.getMessage());
	    ex.printStackTrace();
	}
        return ks;
    }

    // Setters & Getters:
    public void setContentPanel(JPanel panel){
	this.contentPanel = panel;
	holdAll.add(this.contentPanel, BorderLayout.CENTER);
    }

    public void changeContentPanel(JPanel panel){
//	this.contentPanel.removeAll();
	holdAll.remove(this.contentPanel);
	holdAll.revalidate();
	this.contentPanel = panel;
	holdAll.add(this.contentPanel, BorderLayout.CENTER);
    }

    public void updateBottomPanel(){
	holdAll.remove(this.bottomPanel);
	holdAll.revalidate();
	this.bottomPanel = new JPanel(); // The bottom panel which holds date and kassenstand bar.
	Date now = new Date();
	dateTime = now.toString();
	dateTimeLabel = new JLabel(dateTime);
	kassenstandLabel = new JLabel( bc.priceFormatter(retrieveKassenstand())+" "+bc.currencySymbol );
	this.bottomPanel.add(dateTimeLabel);
	this.bottomPanel.add(kassenstandLabel);
	holdAll.add(this.bottomPanel, BorderLayout.SOUTH);
    }

}
