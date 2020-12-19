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
    private JButton exportAndDeleteButton;
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
        JPanel buttonPanel1 = new JPanel();
        JPanel buttonPanel2 = new JPanel();

        updateButton = new JButton("Statuswerte aktualisieren");
        exportButton = new JButton("TSE-Log vollständig exportieren");
        exportPartButton = new JButton("TSE-Log teilweise exportieren");
        exportAndDeleteButton = new JButton("TSE-Log exportieren und löschen");
        updateButton.addActionListener(this);
        exportButton.addActionListener(this);
        exportPartButton.addActionListener(this);
        exportAndDeleteButton.addActionListener(this);
        buttonPanel1.add(updateButton);
        buttonPanel1.add(exportButton);
        buttonPanel2.add(exportPartButton);
        buttonPanel2.add(exportAndDeleteButton);
        
        panel.add(buttonPanel1);
        panel.add(buttonPanel2);
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
        if (e.getSource() == exportButton || e.getSource() == exportPartButton || e.getSource() == exportAndDeleteButton){
            initializeExportChooser("tse_export.tar");
            String message = "";
            String filename = "";
            Long highestExportedSig = (long)-1;
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
                boolean aborted = tseped.getAborted();
                if (!aborted) {
                    filename = askForExportFilename();
                    if (filename != null) {
                        Long maxRecords = tseped.maxRecordsMode() ? tseped.getMaxNumRecords() : null;
                        if (tseped.txNumberMode()) {
                            logger.info("Exporting partial transaction data by TX number, from TX {} to TX {}, limited by {} signatures",
                                tseped.getTxNumberStart(), tseped.getTxNumberEnd(), maxRecords);
                            message = tse.exportPartialTransactionDataByTXNumber(filename, tseped.getTxNumberStart(), tseped.getTxNumberEnd(), maxRecords);
                        } else if (tseped.dateMode()) {
                            logger.info("Exporting partial transaction data by Unix date, from {} to {}, limited by {} signatures",
                                tseped.getDateStart(), tseped.getDateEnd(), maxRecords);
                            message = tse.exportPartialTransactionDataByDate(filename, tseped.getDateStart(), tseped.getDateEnd(), maxRecords);
                        } else if (tseped.sigCounterMode()) {
                            logger.info("Exporting partial transaction data by Sig counter, excluding everything up to {}, limited by {} signatures",
                                tseped.getSigCounterLastExcluded(), maxRecords);
                            message = tse.exportPartialTransactionDataBySigCounter(filename, tseped.getSigCounterLastExcluded(), maxRecords);
                        }
                    }
                }
            } else if (e.getSource() == exportAndDeleteButton) {
                JDialog dialog = new JDialog(this.mainWindow, "Wie viel soll von der TSE exportiert und gelöscht werden?", true);
                TSEPartialExportWithDeleteDialog tseped = new TSEPartialExportWithDeleteDialog(this.mainWindow, dialog);
                dialog.getContentPane().add(tseped, BorderLayout.CENTER);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                boolean aborted = tseped.getAborted();
                if (!aborted) {
                    filename = askForExportFilename();
                    if (filename != null) {
                        Long numRecords = tseped.getNumberRecords();
                        logger.info("Exporting partial transaction data by Sig counter, excluding everything up to {}, limited by {} signatures",
                            0, numRecords);
                        message = tse.exportPartialTransactionDataBySigCounter(filename, (long)0, numRecords);
                        if (message == "OK") {
                            highestExportedSig = tse.discoverHighestExportedSigCounterFromExport(filename);
                            logger.info("Highest exported Sig counter was found to be: {}", highestExportedSig);
                            if (highestExportedSig <= 0) {
                                logger.fatal("It seems there was an error in discoverHighestExportedSigCounterFromExport()");
                                JOptionPane.showMessageDialog(this,
                                    "Fehler: Die höchste exportierte Signatur konnte nicht ermittelt werden.\n"+
                                    "Eventuell ist beim Export etwas schief gelaufen.\n"+
                                    "Daher werden jetzt auch keine Daten von der TSE gelöscht.",
                                    "Fehler", JOptionPane.ERROR_MESSAGE);
                            } else {
                                logger.info("Now deleting all signatures up to {} from the TSE.", highestExportedSig);
                                message = tse.deletePartialTransactionDataBySigCounter(highestExportedSig);
                            }
                        }
                    }
                }
            }
            if (message == "OK") {
                logger.info("TSE export created successfully");
                JOptionPane.showMessageDialog(this,
                    "TSE-Export '"+filename+"' wurde erfolgreich angelegt.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
                if (e.getSource() == exportAndDeleteButton) {
                    JOptionPane.showMessageDialog(this,
                        "Alle Signaturen bis "+highestExportedSig+" wurden von der TSE gelöscht.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                }
            } else if (message == "") {
                // do nothing, operation was canceled by user.
                logger.info("TSE export canceled by user");
            } else if (message == "ErrorTooManyRecords") {
                // inform the user:
                JOptionPane.showMessageDialog(this,
                    "Fehler: Maximale Anzahl Signaturen ist zu klein.\n"+
                    "TSE-Export '"+filename+"' konnte nicht erstellt werden.\n"+
                    "Hinweis: Falls nicht nach dem Signatur-Zähler gefiltert wird,\n"+
                    "muss die maximale Anzahl Signaturen groß genug sein, um alle\n"+
                    "exportierten Transaktionen umfassen zu können. Man kann es also\n"+
                    "auch gleich weglassen.\n"+
                    "Fehlermeldung: "+message,
                    "Fehler", JOptionPane.ERROR_MESSAGE);
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
