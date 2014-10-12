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
import javax.swing.filechooser.FileNameExtensionFilter;

public class DumpDatabase extends WindowContent {
    private JButton dumpButton;
    private JButton readButton;
    private FileExistsAwareFileChooser sqlSaveChooser;
    private JFileChooser sqlLoadChooser;

    private Connection adminConn;

    private TabbedPaneGrundlage tabbedPane;

    /**
     *    The constructor.
     *       */
    public DumpDatabase(Connection conn, MainWindowGrundlage mw, TabbedPaneGrundlage tp)
    {
	super(conn, mw);
        tabbedPane = tp;

        JPanel buttonPanel = new JPanel();
            dumpButton = new JButton("DB exportieren");
            readButton = new JButton("DB importieren");
            dumpButton.addActionListener(this);
            readButton.addActionListener(this);
            buttonPanel.add(dumpButton);
            buttonPanel.add(readButton);
        this.add(buttonPanel);
    }

    void initializeSaveChooser() {
        sqlSaveChooser = new FileExistsAwareFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "SQL Dokumente", "sql");
        sqlSaveChooser.setFileFilter(filter);
    }

    void initializeLoadChooser() {
        sqlLoadChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "SQL Dokumente", "sql");
        sqlLoadChooser.setFileFilter(filter);
    }

    String askForDumpFilename() {
        int returnVal = sqlSaveChooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            File file = sqlSaveChooser.getSelectedFile();
            System.out.println("Selected dump file "+file.getName());
            //return file.getName();
            return file.getAbsolutePath();
        } else {
            System.out.println("Save command cancelled by user.");
        }
        return null;
    }

    String askForReadFilename() {
        int returnVal = sqlLoadChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            File file = sqlLoadChooser.getSelectedFile();
            System.out.println("Selected read file "+file.getName());
            //return file.getName();
            return file.getAbsolutePath();
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

    private String constructProgramString(String cmd, String path) {
        String program = "";
        if (path.length() == 0){
            program = cmd;
        } else {
            if ( path.endsWith("\"") ){
                program = path.substring(0, path.length()-1)+fileSep+cmd+"\"";
            } else {
                program = path+fileSep+cmd;
            }
        }
        return program;
    }

    void dumpDatabase(String password, String filename) {
        // From: http://www.jvmhost.com/articles/mysql-postgresql-dump-restore-java-jsp-code#sthash.6M0ty78M.dpuf
        //String executeCmd = "mysqldump -u kassenadmin -p"+password+" kasse -r "+filename;
        String program = constructProgramString("mysqldump", this.mysqlPath);
        System.out.println("MySQL path from config.properties: *"+program+"*");
        String[] executeCmd = new String[] {program, "--no-create-info", "--replace",
            "-ukassenadmin", "-p"+password, "kasse", "-r", filename};
        try {
            Runtime shell = Runtime.getRuntime();
            Process proc = shell.exec(executeCmd);
            int processComplete = proc.waitFor();
            if (processComplete == 0) {
                System.out.println("Dump created successfully");
                JOptionPane.showMessageDialog(this,
                        "Datenbank-Dump '"+filename+"' wurde erfolgreich angelegt.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
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
        //String[] executeCmd = new String[] {"/bin/sh", "-c", "mysql -u kassenadmin -p"+password+" kasse < "+filename};
        String program = constructProgramString("mysql", this.mysqlPath);
        System.out.println("MySQL path from config.properties: *"+program+"*");
        String[] executeCmd = new String[] {program, "--local-infile",
            "-hlocalhost", "-ukassenadmin", "-p"+password,
            "-e", "source "+filename, "kasse"};
        try {
            Runtime shell = Runtime.getRuntime();
            Process proc = shell.exec(executeCmd);
            BufferedReader stdInput = new BufferedReader(new
                                     InputStreamReader(proc.getInputStream()));
            BufferedReader stdError = new BufferedReader(new
                                     InputStreamReader(proc.getErrorStream()));
            System.out.println("Here is the standard output of the command "+executeCmd+":\n");
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
            int processComplete = proc.waitFor();
            if (processComplete == 0) {
                System.out.println("Dump read in successfully");
                JOptionPane.showMessageDialog(this,
                        "Datenbank-Dump '"+filename+"' wurde erfolgreich eingelesen.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
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
            initializeSaveChooser();
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
            initializeLoadChooser();
            String filename = askForReadFilename();
            if (filename != null){
                readDatabase(password, filename);
                // update all tabs
                tabbedPane.recreateTabbedPane();
            }
        }
    }
}
