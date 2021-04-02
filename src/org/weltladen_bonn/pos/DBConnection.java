package org.weltladen_bonn.pos;

// Basic Java stuff:
import java.util.*; // for Vector, String

// MySQL Connector/J stuff:
import java.sql.*; // SQLException, DriverManager, Connection, Statement, ResultSet
// import org.mariadb.jdbc.exceptions.jdbc4.CommunicationsException; // this does not exist in new MariaDB driver
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DBConnection {
    private static final Logger logger = LogManager.getLogger(DBConnection.class);

    protected BaseClass bc;
    public String passwordReturn;

    // Connection to MySQL database:
    public Connection conn = null;
    public MariaDbPoolDataSource pool = null;
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
                // createConnection(user, password);
                createConnectionPool(user, password);

                if (connectionWorks){
                    return okPassword;
                }
            }
        }
    }

    /* 
       Deprecated: old version with single DB connection
    */
    protected void createConnection(String user, String password) {
        connectionWorks = false;
        noConnectionToServer = false;
        // passwordWrong = false;
        try {
            // Obtain connection to MySQL database from DriverManager
            this.conn = DriverManager.getConnection("jdbc:mariadb://"+bc.mysqlHost+":3306/kasse?user="+user+"&password="+password);
            connectionWorks = true;
        } catch (SQLException ex) {
            logger.error("Exception: Perhaps password wrong or DB offline.");
            logger.error("Exception:", ex);
            noConnectionToServer = true;
            // showDBErrorDialog(ex.getMessage());
        } catch (Exception ex) {
            logger.error("Exception:", ex);
        }
    }

    /* 
        New version for MariaDB Java client 2.6.2:
        Connection pool has advantages over a standard connection, e.g.
        in case the connection is closed by the DB, it is automatically
        re-established by the pool
    */
    protected void createConnectionPool(String user, String password) {
        connectionWorks = false;
        noConnectionToServer = false;
        try {
            this.pool = new MariaDbPoolDataSource("jdbc:mariadb://"+bc.mysqlHost+":3306/kasse?user="+user+"&password="+password+"&maxPoolSize=10&connectTimeout=3000");
            // Test the connection: DB may be offline or password may be wrong:
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT CONNECTION_ID()");
            // ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM "+tableForMode("verkauf"));
            rs.next();
            logger.info("DBConnection CONNECTION_ID: "+rs.getLong(1));
            rs.close();
            stmt.close();
            connection.close();
            connectionWorks = true;
        } catch (SQLException ex) {
            logger.error("Exception: Perhaps password wrong or DB offline.");
            logger.error("Exception:", ex);
            noConnectionToServer = true;
            // showDBErrorDialog(ex.getMessage());
        } catch (Exception ex) {
            logger.error("Exception:", ex);
        }
        // this.pool.close(); // no, we want to use it as long as program is open
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
