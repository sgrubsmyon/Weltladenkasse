package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
//import java.sql.*;

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
    public Connection conn = null;
    public boolean connectionWorks = false;

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
    public MainWindowGrundlage(String password){
        bc = new BaseClass();
        initiate(password);
    }

    public void initiate(String password){
        createConnection(password);
        if (!connectionWorks) return;

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

    protected void createConnection(String password) {
        connectionWorks = true;
	try {
	    // Load JDBC driver and register with DriverManager
	    Class.forName("com.mysql.jdbc.Driver").newInstance();
	    // Obtain connection to MySQL database from DriverManager
	    this.conn = DriverManager.getConnection("jdbc:mysql://localhost/kasse",
		    "mitarbeiter", password);
	} catch (Exception ex) {
	    System.out.println("Exception: " + ex.getMessage());
	    System.out.println("Probably password wrong.");
	    //ex.printStackTrace();
            connectionWorks = false;
	}
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
