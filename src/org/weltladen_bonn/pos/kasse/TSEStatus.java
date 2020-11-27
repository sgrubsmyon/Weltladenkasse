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
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
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
    private FileExistsAwareFileChooser logExportChooser;
    private JFileChooser sqlLoadChooser;

    private TabbedPaneGrundlage tabbedPane;

    // class to talk to TSE
    private WeltladenTSE tse;
    HashMap<String, String> statusValues;

    private JPanel panel;
    private JPanel statusPanelContainer;
    private JPanel statusPanel;
    private JScrollPane statusPanelScrollPane;

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
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // borders:
        int top = 10, left = 10, bottom = 10, right = 10;
        panel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));

        statusPanelContainer = new JPanel();
        statusPanelContainer.setLayout(new BoxLayout(statusPanelContainer, BoxLayout.Y_AXIS));
        panel.add(statusPanelContainer);
        showStatusPanel();
        showButtonPanel();
        
        this.add(panel);

        scrollPaneToTop();
    }

    private void showStatusPanel() {
        statusPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.ipadx = 15;
        c.ipady = 10;
        c.insets = new Insets(3, 0, 3, 3);
        
        int row = 0;
        for (String k : tse.statusValueKeys) {
            if (row > 0) {
                // First a separator to separate from last item
                c.gridy = row;
                c.gridx = 0; c.gridwidth = 2; // fill entire row
                statusPanel.add(new JSeparator(JSeparator.HORIZONTAL), c); // first column
                c.gridwidth = 1; // back to normal
                row++; // new row
            }
            JLabel key = new JLabel(k+":");
            String valueText = statusValues.get(k);
            // Determine how many rows are needed for display:
            String[] valueTextSplit = valueText.split("\n");
            int rows = valueTextSplit.length;
            for (String s : valueTextSplit) {
                if (s.length() > 100) rows++;
            }
            JTextArea value = new JTextArea(valueText, rows, 100);
            value = makeLabelStyle(value);
            value.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            // value.setFont(BaseClass.mediumFont);
            c.gridy = row;
            c.gridx = 0; statusPanel.add(key, c); // first column
            c.gridx = 1; statusPanel.add(value, c); // second column
            row++; // new row
        }

        statusPanelScrollPane = new JScrollPane(statusPanel);
        statusPanelScrollPane.setBorder(BorderFactory.createTitledBorder("Status-Werte der TSE"));
        statusPanelScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        statusPanelContainer.add(statusPanelScrollPane);
    }

    private void scrollPaneToTop() {
        // Scroll pane to top:
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                statusPanel.scrollRectToVisible(new Rectangle(0,0,1,1)); // scroll to top, really, to the TOP!
            }
        });
    }

    private void showButtonPanel() {
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
    }

    void initializeExportChooser(String filename) {
        logExportChooser = new FileExistsAwareFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "TAR-Archive", "tar");
        logExportChooser.setFileFilter(filter);
        logExportChooser.setSelectedFile(new File(filename));
    }

    String askForExportFilename() {
        int returnVal = logExportChooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            File file = logExportChooser.getSelectedFile();
            logger.info("Selected TSE transaction log export file "+file.getAbsolutePath());
            return file.getAbsolutePath();
        } else {
            logger.info("Save command cancelled by user.");
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
            updateStatusValues();
            statusPanelContainer.remove(statusPanelScrollPane);
            statusPanelContainer.revalidate();
            showStatusPanel();
            return;
        }
        if (e.getSource() == exportButton || e.getSource() == exportPartButton){
            initializeExportChooser("tse_export.tar");
            String message = "";
            String filename = "";
            if (e.getSource() == exportButton) {
                filename = askForExportFilename();
                if (filename != null) {
                    message = tse.exportFullTransactionData(filename);
                }
            } else if (e.getSource() == exportPartButton) {
                JDialog dialog = new JDialog(this.mainWindow, "Wie sollen die Daten der TSE ausgewählt werden?", true);
                TSEPartialExportDialog tseped = new TSEPartialExportDialog(this.mainWindow, dialog);
                dialog.getContentPane().add(tseped, BorderLayout.CENTER);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                // tseped.getPIN();
                boolean aborted = tseped.getAborted();
                // filename = askForExportFilename();
                // if (filename != null) {
                //     message = tse.exportPartialTransactionDataByTXNumber(filename, (long)10, (long)15, null);
                // }
            }
            if (message == "OK") {
                logger.info("TSE export created successfully");
                JOptionPane.showMessageDialog(this,
                        "TSE-Export '"+filename+"' wurde erfolgreich angelegt.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            } else if (message == "") {
                // do nothing, operation was canceled by user.
                logger.info("TSE export canceled by user");
            } else {
                logger.info("Could not create the TSE export");
                JOptionPane.showMessageDialog(this,
                        "Fehler: TSE-Export '"+filename+"' konnte nicht erstellt werden.\n"+
                        "Fehlermeldung: "+message,
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
            return;    
        }
    }
}
