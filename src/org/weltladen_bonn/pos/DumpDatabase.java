package org.weltladen_bonn.pos;

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
import org.mariadb.jdbc.MariaDbPoolDataSource;

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
    public DumpDatabase(MariaDbPoolDataSource pool, MainWindowGrundlage mw, TabbedPaneGrundlage tp)
    {
	super(pool, mw);
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

    void initializeSaveChooser(String filename) {
        sqlSaveChooser = new FileExistsAwareFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "SQL Dokumente", "sql");
        sqlSaveChooser.setFileFilter(filter);
        sqlSaveChooser.setSelectedFile(new File(filename));
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
            System.out.println("Selected dump file "+file.getAbsolutePath());
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
            System.out.println("Selected read file "+file.getAbsolutePath());
            //return file.getName();
            return file.getAbsolutePath();
        } else {
            System.out.println("Open command cancelled by user.");
        }
        return null;
    }

    String askForAdminPassword() {
        DBConnectionAdmin dbconna = new DBConnectionAdmin(bc);
        return dbconna.password;
    }

    void dumpDatabase(String password, String filename) {
        // From: http://www.jvmhost.com/articles/mysql-postgresql-dump-restore-java-jsp-code#sthash.6M0ty78M.dpuf
        String program = constructProgramPath(bc.mysqlPath, "mysqldump");
        System.out.println("MySQL path from config.properties: *"+program+"*");
        // 'destructive' dump, resulting in exact copy of DB:
        String[] executeCmd = new String[] {program,
            "-h"+bc.mysqlHost, "-ukassenadmin", "-p"+password, "kasse", "-r", filename};
        // 'non-destructive' (incremental) dump using replace:
        //String[] executeCmd = new String[] {program, "--no-create-info", "--replace",
        //    "-ukassenadmin", "-p"+password, "kasse", "-r", filename};
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
        String program = constructProgramPath(bc.mysqlPath, "mysql");
        System.out.println("MySQL path from config.properties: *"+program+"*");
        String[] executeCmd = new String[] {program, "--local-infile",
            "-h"+bc.mysqlHost, "-ukassenadmin", "-p"+password,
            "-e", "source "+filename, "kasse"};
        try {
            Runtime shell = Runtime.getRuntime();
            Process proc = shell.exec(executeCmd);
            BufferedReader stdInput = new BufferedReader(new
                                     InputStreamReader(proc.getInputStream()));
            BufferedReader stdError = new BufferedReader(new
                                     InputStreamReader(proc.getErrorStream()));
            System.out.println("Here is the standard output of the mysql read-in command (if any):");
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }
            System.out.println("Here is the standard error of the mysql read-in command (if any):");
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

    private String[] queryMostRecentOrderYearAndWeek() {
        String year = "", week = "";
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT jahr, kw "+
                    "FROM bestellung ORDER BY "+
                    "bestell_nr DESC LIMIT 1"
                    );
            // Now do something with the ResultSet, should be only one result ...
            rs.next();
            year = rs.getString(1).substring(0,4);
            week = rs.getString(2);
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        String[] yw = {year, week};
        return yw;
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
            String[] yw = queryMostRecentOrderYearAndWeek();
            String year = yw[0];
            String week = String.format("%02d", Integer.parseInt(yw[1]));
            initializeSaveChooser("DB_Dump_"+year+"_KW"+week+".sql");
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
