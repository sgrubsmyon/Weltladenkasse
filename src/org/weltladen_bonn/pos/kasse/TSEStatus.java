package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.io.*; // for File

// MySQL Connector/J stuff:
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import java.awt.*;
import java.awt.event.*;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JPasswordField;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.weltladen_bonn.pos.WindowContent;
import org.weltladen_bonn.pos.TabbedPaneGrundlage;
import org.weltladen_bonn.pos.FileExistsAwareFileChooser;
// import org.weltladen_bonn.pos.BaseClass.BigLabel;

public class TSEStatus extends WindowContent {
    private static final Logger logger = LogManager.getLogger(TSEStatus.class);

    private JButton updateButton;
    private JButton exportButton;
    private JButton exportPartButton;
    private FileExistsAwareFileChooser sqlSaveChooser;
    private JFileChooser sqlLoadChooser;

    private TabbedPaneGrundlage tabbedPane;

    // class to talk to TSE
    private WeltladenTSE tse;
    HashMap<String, String> statusValues;

    /**
     *    The constructor.
     *       */
    public TSEStatus(MariaDbPoolDataSource pool, MainWindow mw, TabbedPaneGrundlage tp) {
	    super(pool, mw);
        tabbedPane = tp;
        tse = mw.getTSE();
        updateStatusValues();
        showPanel();
    }

    private void updateStatusValues() {
        statusValues = tse.retrieveTSEStatusValues();
    }

    private void showPanel() {
        /**
         * TextField-Panel
         * */
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // borders:
        int top = 10, left = 10, bottom = 10, right = 10;
        panel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));

        JPanel statusPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.VERTICAL;
        c.anchor = GridBagConstraints.WEST;
        c.ipadx = 15;
        c.ipady = 10;
        c.insets = new Insets(3, 0, 3, 3);
        
        int row = 0;
        for (String k : tse.statusValueKeys) {
            JLabel key = new JLabel(k+":");
            String valueText = statusValues.get(k);
            // Determine how many rows are needed for display:
            String[] valueTextSplit = valueText.split("\n");
            int rows = valueTextSplit.length;
            for (String s : valueTextSplit) {
                if (s.length() > 100) rows++;
            }
            System.out.println(k+": "+rows+" "+valueText.length());
            JTextArea value = new JTextArea(valueText, rows, 100);
            value = makeLabelStyle(value);
            value.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            // value.setFont(BaseClass.mediumFont);
            c.gridy = row; // new row
            c.gridx = 0; statusPanel.add(key, c); // first column
            c.gridx = 1; statusPanel.add(value, c); // second column
            row++;
        }

        JScrollPane statusPanelScrollPane = new JScrollPane(statusPanel);
        panel.add(statusPanelScrollPane);

        JPanel buttonPanel = new JPanel();
        updateButton = new JButton("Statuswerte aktualisieren");
        exportButton = new JButton("TSE-Log vollständig exportieren");
        exportPartButton = new JButton("TSE-Log teilweise exportieren");
        updateButton.addActionListener(this);
        exportButton.addActionListener(this);
        exportPartButton.addActionListener(this);
        buttonPanel.add(updateButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(exportPartButton);
        panel.add(buttonPanel);

        this.add(panel);
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
            logger.info("Selected dump file "+file.getAbsolutePath());
            //return file.getName();
            return file.getAbsolutePath();
        } else {
            logger.info("Save command cancelled by user.");
        }
        return null;
    }

    String askForReadFilename() {
        int returnVal = sqlLoadChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            File file = sqlLoadChooser.getSelectedFile();
            logger.info("Selected read file "+file.getAbsolutePath());
            //return file.getName();
            return file.getAbsolutePath();
        } else {
            logger.info("Open command cancelled by user.");
        }
        return null;
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == updateButton){
            
        }
        else if (e.getSource() == exportButton){
            
        }
        else if (e.getSource() == exportPartButton){
            
        }
    }
}
