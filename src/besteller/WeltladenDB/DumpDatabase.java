package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector
import java.io.*; // for File
import java.lang.Process; // for executing system commands
import java.lang.Runtime; // for executing system commands

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// GUI stuff:
import java.awt.*;
import java.awt.event.*;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JPasswordField;

public class DumpDatabase extends WindowContent {
    private JButton dumpButton;
    private JButton readButton;
    private JFileChooser fc;

    private Connection adminConn;

    /**
     *    The constructor.
     *       */
    public DumpDatabase(Connection conn, MainWindowGrundlage mw)
    {
	super(conn, mw);

        JPanel buttonPanel = new JPanel();
            dumpButton = new JButton("DB exportieren");
            readButton = new JButton("DB importieren");
            dumpButton.addActionListener(this);
            readButton.addActionListener(this);
            buttonPanel.add(dumpButton);
            buttonPanel.add(readButton);
        this.add(buttonPanel);
    }

    void initializeDumpDialog() {
        fc = new JFileChooser(){
            // override approveSelection to get a confirmation dialog if file exists
            @Override
            public void approveSelection(){
                File f = getSelectedFile();
                if (f.exists() && getDialogType() == SAVE_DIALOG){
                    int result = JOptionPane.showConfirmDialog(this,
                            "Datei existiert bereits. Überschreiben?",
                            "Datei existiert",
                            JOptionPane.YES_NO_CANCEL_OPTION);
                    switch (result){
                        case JOptionPane.YES_OPTION:
                            super.approveSelection();
                            return;
                        case JOptionPane.NO_OPTION:
                            return;
                        case JOptionPane.CLOSED_OPTION:
                            return;
                        case JOptionPane.CANCEL_OPTION:
                            cancelSelection();
                            return;
                    }
                }
                super.approveSelection();
            }
        };
    }
    
    void initializeReadDialog() {
        fc = new JFileChooser();
    }

    String askForDumpFilename() {
        int returnVal = fc.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            File file = fc.getSelectedFile();
            System.out.println("Selected dump file "+file.getName());
            return file.getName();
        } else {
            System.out.println("Save command cancelled by user.");
        }
        return null;
    }

    String askForReadFilename() {
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            File file = fc.getSelectedFile();
            System.out.println("Selected read file "+file.getName());
            return file.getName();
            //return file.getAbsolutePath();
        } else {
            System.out.println("Open command cancelled by user.");
        }
        return null;
    }

    boolean createAdminConnection(String password) {
        boolean connectionWorks = true;
	try {
	    // Load JDBC driver and register with DriverManager
	    Class.forName("com.mysql.jdbc.Driver").newInstance();
	    // Obtain connection to MySQL database from DriverManager
	    adminConn = DriverManager.getConnection("jdbc:mysql://localhost/kasse",
		    "kassenadmin", password);
	} catch (Exception ex) {
	    System.out.println("Exception: " + ex.getMessage());
	    System.out.println("Probably password wrong.");
	    //ex.printStackTrace();
            connectionWorks = false;
	}
        return connectionWorks;
    }

    String askForAdminPassword() {
        boolean passwdIncorrect = false;
        while (true){
            JLabel label = new JLabel("");
            if (passwdIncorrect){ label = new JLabel("Falsches Passwort!"); }
            Vector<String> result = showPasswordWindow(label);
            if (result.get(0) == "CANCEL"){
                return null;
            }
            if (result.get(0) == "OK"){
                String password = result.get(1);

                if ( createAdminConnection(password) ){
                    System.out.println("Admin password was correct.");
                    return password;
                } else {
                    System.out.println("Admin password was incorrect.");
                    passwdIncorrect = true;
                }
            }
        }
    }

    private static Vector<String> showPasswordWindow(JLabel label) {
        // retrieve the password from the user, set focus to password field
        // gracefully taken from http://blogger.ziesemer.com/2007/03/java-password-dialog.html,
        // @ack credit: Mark A. Ziesemer, Anonymous, Akerbos (see comments)
        final JPasswordField pf = new JPasswordField();
        JOptionPane jop = null;
        if (label.getText().length() > 0){
            jop = new JOptionPane(new Object[]{label, pf},
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);
        } else {
            jop = new JOptionPane(pf,
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);
        }
        JDialog dialog = jop.createDialog("Bitte Kassenadmin-Passwort eingeben");
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
        String ok;
        if (result == JOptionPane.OK_OPTION){ ok = "OK"; }
        else { ok = "CANCEL"; }
        String password = new String(pf.getPassword());
        okPassword.add(ok);
        okPassword.add(password);
        return okPassword;
    }

    void dumpDatabase(String password, String filename) {
        // From: http://www.jvmhost.com/articles/mysql-postgresql-dump-restore-java-jsp-code#sthash.6M0ty78M.dpuf
        String executeCmd = "mysqldump -u kassenadmin -p"+password+" kasse -r "+filename;
        try {
            Process runtimeProcess = Runtime.getRuntime().exec(executeCmd);
            int processComplete = runtimeProcess.waitFor();
            if (processComplete == 0) {
                System.out.println("Dump created successfully");
            } else {
                System.out.println("Could not create the dump");
                JOptionPane.showMessageDialog(this,
                        "Fehler: Dump-Datei "+filename+" konnte nicht erstellt werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void readDatabase(String password, String filename) {
        // alternative: User JDBC connection for admin and 'SOURCE filename;' (http://stackoverflow.com/questions/105776/how-do-i-restore-a-mysql-dump-file), but doesn't work (SyntaxError)
        //try {
        //    Statement stmt = adminConn.createStatement();
        //    int result = stmt.executeUpdate("SOURCE "+filename);
        //    stmt.close();
        //    if (result == 0){
        //        JOptionPane.showMessageDialog(this,
        //                "Fehler: Dump-Datei "+filename+" konnte nicht eingelesen werden.",
        //                "Fehler", JOptionPane.ERROR_MESSAGE);
        //    }
        //} catch (SQLException ex) {
        //    System.out.println("Exception: " + ex.getMessage());
        //    ex.printStackTrace();
        //    JOptionPane.showMessageDialog(this,
        //            "Fehler: Dump-Datei "+filename+" konnte nicht eingelesen werden.",
        //            "Fehler", JOptionPane.ERROR_MESSAGE);
        //}
        // From: http://stackoverflow.com/questions/14691112/import-a-dump-file-to-mysql-jdbc
        // Use mysqlimport
        //String executeCmd = "mysql -u kassenadmin -p"+password+" kasse < "+filename;
        String[] executeCmd = new String[]{"/bin/sh", "-c", "mysql -u kassenadmin -p"+password+" kasse < "+filename};
        try {
            Process p = Runtime.getRuntime().exec(executeCmd);
            BufferedReader stdInput = new BufferedReader(new
                                     InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new
                                     InputStreamReader(p.getErrorStream()));
            System.out.println("Here is the standard output of the command "+executeCmd+":\n");
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
            int processComplete = p.waitFor();
            if (processComplete == 0) {
                System.out.println("Dump read in successfully");
            } else {
                System.out.println("Could not read in the dump");
                JOptionPane.showMessageDialog(this,
                        "Fehler: Dump-Datei "+filename+" konnte nicht gelesen werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == dumpButton){
            String password = askForAdminPassword();
            if (password == null){
                return;
            }
            initializeDumpDialog();
            String filename = askForDumpFilename();
            if (filename != null){
                dumpDatabase(password, filename);
            }
            return;
        }
        if (e.getSource() == readButton){
            String password = askForAdminPassword();
            if (password == null){
                return;
            }
            initializeReadDialog();
            String filename = askForReadFilename();
            if (filename != null){
                readDatabase(password, filename);
            }
        }
    }
}
