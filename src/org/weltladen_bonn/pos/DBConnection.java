package org.weltladen_bonn.pos;

// Basic Java stuff:
import java.util.*; // for Vector, String

// MySQL Connector/J stuff:
import java.sql.*; // SQLException, DriverManager, Connection, Statement, ResultSet
// import org.mariadb.jdbc.exceptions.jdbc4.CommunicationsException; // this does not exist in new MariaDB driver

// GUI stuff:
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class DBConnection {
    protected BaseClass bc;
    public String passwordReturn;

    // Connection to MySQL database:
    public Connection conn = null;
    public boolean connectionWorks = false;
    protected boolean noConnectionToServer = false;
    // protected boolean passwordWrong = false;

    public DBConnection() {
    }

    public DBConnection(BaseClass bc) {
        this.bc = bc;
        Vector<String> okPassword = showPasswordDialog("Bitte Mitarbeiter-Passwort eingeben", "mitarbeiter");
        this.passwordReturn = okPassword.get(0);
    }

    protected Vector<String> showPasswordDialog(String title, String user) {
        Vector<String> okPassword = new Vector<String>(2);
        while (true){
            JLabel label = new JLabel("");
            // if (passwordWrong){ label = new JLabel("Falsches Passwort!"); }
            if (noConnectionToServer){ label = new JLabel("Falsches Passwort oder keine Verbindung zum Datenbankserver..."); }
            okPassword = showPasswordWindow(title, label);
            if (okPassword.get(0) == "CANCEL"){
                return okPassword;
            }
            if (okPassword.get(0) == "OK"){
                String password = okPassword.get(1);
                createConnection(user, password);

                if (connectionWorks){
                    return okPassword;
                }
            }
        }
    }

    protected void createConnection(String user, String password) {
        connectionWorks = false;
        noConnectionToServer = false;
        // passwordWrong = false;
	try {
	    // Load JDBC driver and register with DriverManager
            // Class.forName("org.mariadb.jdbc.Driver").newInstance(); // not needed with new MariaDB driver
	    // Obtain connection to MySQL database from DriverManager
            // this.conn = DriverManager.getConnection("jdbc:mysql://"+bc.mysqlHost+"/kasse",
                // user, password);
            this.conn = DriverManager.getConnection("jdbc:mariadb://"+bc.mysqlHost+":3306/kasse?user="+user+"&password="+password);
            connectionWorks = true;
        // } catch (CommunicationsException ex) {
        //     System.out.println("Exception: " + ex.getMessage());
        //     System.out.println("Connection to MySQL database failed. Check network "+
        //         "connectivity of server and client,");
        //     noConnectionToServer = true;
	} catch (SQLException ex) {
	    System.out.println("Exception: " + ex.getMessage());
            System.out.println("Perhaps password wrong or DB not online.");
            // passwordWrong = true;
            noConnectionToServer = true;
	} catch (Exception ex) {
	    System.out.println("Exception: " + ex.getMessage());
	    ex.printStackTrace();
	}
    }

    private static Vector<String> showPasswordWindow(String title, JLabel optionalLabel) {
        // retrieve the password from the user, set focus to password field
        // gracefully taken from http://blogger.ziesemer.com/2007/03/java-password-dialog.html,
        // @ack credit: Mark A. Ziesemer, Anonymous, Akerbos (see comments)
        final JPasswordField pf = new JPasswordField();
        JOptionPane jop = null;
        if (optionalLabel.getText().length() > 0){
            jop = new JOptionPane(new Object[]{optionalLabel, pf},
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);
        } else {
            jop = new JOptionPane(pf,
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);
        }
        JDialog dialog = jop.createDialog(title);
        dialog.addWindowFocusListener(new WindowAdapter(){
            @Override
            public void windowGainedFocus(WindowEvent e){
                pf.requestFocusInWindow();
            }
        });
        pf.addFocusListener(new FocusListener() {
            public void focusGained( FocusEvent e ) {
                pf.selectAll();
            }
            public void focusLost( FocusEvent e ) {
                if ( pf.getPassword().length == 0 ) {
                    pf.requestFocusInWindow();
                }
            }
        });
        dialog.setVisible(true);
        int result = (Integer)jop.getValue();
        dialog.dispose();

        Vector<String> okPassword = new Vector<String>(2);
        String ok, password;
        if (result == JOptionPane.OK_OPTION){
            ok = "OK";
            password = new String(pf.getPassword());
        }
        else {
            ok = "CANCEL";
            password = null;
        }
        okPassword.add(ok);
        okPassword.add(password);
        return okPassword;
    }
}
