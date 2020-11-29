package org.weltladen_bonn.pos.kasse;

import org.weltladen_bonn.pos.BaseClass;
import org.weltladen_bonn.pos.DialogWindow;
import org.weltladen_bonn.pos.MainWindowGrundlage;
import org.weltladen_bonn.pos.BaseClass.BigLabel;
import org.weltladen_bonn.pos.IntegerDocumentFilter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TSEPartialExportDialog extends DialogWindow implements DocumentListener {
    private static final Logger logger = LogManager.getLogger(TSEPartialExportDialog.class);

    private JTextField txNumberStartField;
    private JTextField txNumberEndField;
    private JTextField dateStartField;
    private JTextField dateEndField;
    private JTextField sigCounterLastExcludedField;
    private JCheckBox limitRecords;
    private JTextField maxNumRecordsField;

    private JButton okButton;
    private JButton cancelButton;
    private boolean aborted = true;


    // Methoden:
    public TSEPartialExportDialog(MainWindowGrundlage mw, JDialog dia) {
        super(null, mw, dia);
        showAll();
    }

    // will data be lost on close?
    protected boolean willDataBeLost() {
        return false;
    }

    protected void showHeader() {
        /**
         * Information-Panel
         * */
        headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        
        // borders:
        int top = 5, left = 5, bottom = 5, right = 5;
        headerPanel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));

        JTextArea erklaerText = new JTextArea(1, 30);
        erklaerText.append("Wie sollen die Daten der TSE ausgewählt werden?");
        erklaerText = makeLabelStyle(erklaerText);
        erklaerText.setFont(BaseClass.mediumFont);
        erklaerText.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        headerPanel.add(erklaerText);

        allPanel.add(headerPanel, BorderLayout.NORTH);
    }

    private JTextField setUpTextField() {
        JTextField field = new JTextField("");
        field.setColumns(10);
        field.setHorizontalAlignment(SwingConstants.RIGHT);
        field.getDocument().addDocumentListener(this);
        
        return field;
    }

    protected void showMiddle() {
        /**
         * Main-Panel
         * */
        JPanel middlePanel = new JPanel(new GridBagLayout());

        // borders:
        int top = 10, left = 10, bottom = 10, right = 10;
        middlePanel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));

        // Create widgets:
        JRadioButton txButton = new JRadioButton("Nach Transaktionsnummer:");
        txButton.setMnemonic(KeyEvent.VK_T);
        txButton.setActionCommand("tx");
        txButton.addActionListener(this);
        txButton.setSelected(true);

        JRadioButton dateButton = new JRadioButton("Nach Datum (im Unixtime-Format):");
        dateButton.setMnemonic(KeyEvent.VK_D);
        dateButton.setActionCommand("date");
        dateButton.addActionListener(this);

        JRadioButton sigButton = new JRadioButton("Nach Signatur-Zähler:");
        sigButton.setMnemonic(KeyEvent.VK_S);
        sigButton.setActionCommand("sig");
        sigButton.addActionListener(this);

        //Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(txButton);
        group.add(dateButton);
        group.add(sigButton);

        txNumberStartField = setUpTextField();
        txNumberEndField = setUpTextField();
        dateStartField = setUpTextField();
        dateEndField = setUpTextField();
        sigCounterLastExcludedField = setUpTextField();
        ((AbstractDocument)txNumberStartField.getDocument()).setDocumentFilter(
            new IntegerDocumentFilter(1, null, "Transaktionsnummer", this)
        );
        ((AbstractDocument)txNumberEndField.getDocument()).setDocumentFilter(
            new IntegerDocumentFilter(1, null, "Transaktionsnummer", this)
        );
        ((AbstractDocument)dateStartField.getDocument()).setDocumentFilter(
            new IntegerDocumentFilter(null, null, "Unix-Timestamp", this)
        );
        ((AbstractDocument)dateEndField.getDocument()).setDocumentFilter(
            new IntegerDocumentFilter(null, null, "Unix-Timestamp", this)
        );
        ((AbstractDocument)sigCounterLastExcludedField.getDocument()).setDocumentFilter(
            new IntegerDocumentFilter(0, null, "Signatur-Zähler", this)
        );
        
        limitRecords = new JCheckBox("Exportdaten begrenzen");
        limitRecords.addActionListener(this);
        maxNumRecordsField = setUpTextField();
        ((AbstractDocument)maxNumRecordsField.getDocument()).setDocumentFilter(
            new IntegerDocumentFilter(1, null, "Einträge", this)
        );

        // default setup:
        dateStartField.setEnabled(false);
        dateEndField.setEnabled(false);
        sigCounterLastExcludedField.setEnabled(false);
        maxNumRecordsField.setEnabled(false);
        
        //Register a listener for the radio buttons.
        txButton.addActionListener(this);
        dateButton.addActionListener(this);
        sigButton.addActionListener(this);

        // JPanel adminPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.VERTICAL;
        c.anchor = GridBagConstraints.WEST;
        c.ipadx = 15;
        c.ipady = 10;
        c.insets = new Insets(3, 10, 3, 3);
        // *** Radio Buttons:
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridx = 0; middlePanel.add(txButton, c);
        c.gridx = 2; middlePanel.add(dateButton, c);
        c.gridx = 4; middlePanel.add(sigButton, c);
        c.gridwidth = 1;
        // *** Labels and Text Fields:
        c.gridy = 1;
        c.gridx = 0; middlePanel.add(new JLabel("Erste Nummer:"), c);
        c.gridx = 1; middlePanel.add(txNumberStartField, c);
        c.gridx = 2; middlePanel.add(new JLabel("Erstes Datum:"), c);
        c.gridx = 3; middlePanel.add(dateStartField, c);
        c.gridx = 4; middlePanel.add(new JLabel("Letzter nicht enthaltener Zähler:"), c);
        c.gridx = 5; middlePanel.add(sigCounterLastExcludedField, c);
        // next row:
        c.gridy = 2;
        c.gridx = 0; middlePanel.add(new JLabel("Letzte Nummer:"), c);
        c.gridx = 1; middlePanel.add(txNumberEndField, c);
        c.gridx = 2; middlePanel.add(new JLabel("Letztes Datum:"), c);
        c.gridx = 3; middlePanel.add(dateEndField, c);
        // next row:
        c.gridy = 3;
        c.gridwidth = 6;
        c.gridx = 0; middlePanel.add(limitRecords, c);
        c.gridwidth = 1;
        // next row:
        c.gridy = 4;
        c.gridx = 0; middlePanel.add(new JLabel("Maximale Anzahl Einträge"), c);
        c.gridx = 1; middlePanel.add(maxNumRecordsField, c);

        allPanel.add(middlePanel, BorderLayout.CENTER);
    }

    protected void showFooter() {
        /**
         * Button-Panel
         * */
        footerPanel = new JPanel();
        okButton = new JButton("OK" );
        okButton.setMnemonic(KeyEvent.VK_O);
        okButton.addActionListener(this);
        okButton.setEnabled(false);
        footerPanel.add(okButton);
        cancelButton = new JButton("Abbrechen" );
        cancelButton.setMnemonic(KeyEvent.VK_A);
        cancelButton.addActionListener(this);
        footerPanel.add(cancelButton);
        allPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    @Override
    protected int submit() {
        return 0;
    }

    public boolean getAborted() {
        return aborted;
    }

    public boolean txNumberMode() {
        return txNumberStartField.isEnabled();
    }

    public boolean dateMode() {
        return dateStartField.isEnabled();
    }

    public boolean sigCounterMode() {
        return sigCounterLastExcludedField.isEnabled();
    }

    public boolean maxRecordsMode() {
        return maxNumRecordsField.isEnabled();
    }

    public Long getTxNumberStart() {
        return Long.parseLong(txNumberStartField.getText());
    }

    public Long getTxNumberEnd() {
        return Long.parseLong(txNumberEndField.getText());
    }

    public Long getDateStart() {
        return Long.parseLong(dateStartField.getText());
    }

    public Long getDateEnd() {
        return Long.parseLong(dateEndField.getText());
    }

    public Long getSigCounterLastExcluded() {
        return Long.parseLong(sigCounterLastExcludedField.getText());
    }

    public Long getMaxNumRecords() {
        return Long.parseLong(maxNumRecordsField.getText());
    }

    /**
     * Each non abstract class that implements the ActionListener
     * must have this method.
     *
     * @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        updateOKButton();
        if (e.getActionCommand() == "tx") {
            dateStartField.setEnabled(false);
            dateEndField.setEnabled(false);
            sigCounterLastExcludedField.setEnabled(false);

            txNumberStartField.setEnabled(true);
            txNumberEndField.setEnabled(true);
            return;
        }
        if (e.getActionCommand() == "date") {
            txNumberStartField.setEnabled(false);
            txNumberEndField.setEnabled(false);
            sigCounterLastExcludedField.setEnabled(false);

            dateStartField.setEnabled(true);
            dateEndField.setEnabled(true);
            return;
        }
        if (e.getActionCommand() == "sig") {
            txNumberStartField.setEnabled(false);
            txNumberEndField.setEnabled(false);
            dateStartField.setEnabled(false);
            dateEndField.setEnabled(false);

            sigCounterLastExcludedField.setEnabled(true);
            return;
        }
        if (e.getSource() == limitRecords) {
            maxNumRecordsField.setEnabled(limitRecords.isSelected());
        }
        if (e.getSource() == okButton) {
            // int answer = JOptionPane.showConfirmDialog(this,
            //     "Sicher, dass die "+role+" "+numbertype+" stimmt?",
            //     "Fortfahren?",
            //     JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            // if (answer == JOptionPane.YES_OPTION) {
            //     this.aborted = false;
            //     if (role == "TimeAdmin" && numbertype == "PIN") {
            //         byte[] timeAdminPIN = pinField.getText().getBytes();
            //         tse.writeTimeAdminPINtoFile(timeAdminPIN);
            //     }
            aborted = false;
            this.window.dispose();
            // }
            return;
        }
        if (e.getSource() == cancelButton) {
            aborted = true;
            this.window.dispose();
            return;
        }
        super.actionPerformed(e);
    }

    private void updateOKButton() {
        if (txNumberStartField.isEnabled()) {
            if (
                txNumberStartField.getDocument().getLength() > 0 &&
                txNumberEndField.getDocument().getLength() > 0
            ) {
                okButton.setEnabled(true);
            } else {
                okButton.setEnabled(false);
            }
        } else if (dateStartField.isEnabled()) {
            if (
                dateStartField.getDocument().getLength() > 0 &&
                dateEndField.getDocument().getLength() > 0
            ) {
                okButton.setEnabled(true);
            } else {
                okButton.setEnabled(false);
            }
        } else if (sigCounterLastExcludedField.isEnabled()) {
            if (
                sigCounterLastExcludedField.getDocument().getLength() > 0
            ) {
                okButton.setEnabled(true);
            } else {
                okButton.setEnabled(false);
            }
        }
    }

    /**
     * Each non abstract class that implements the DocumentListener must have
     * these methods.
     * @param documentEvent
     *            the document event.
     **/
    @Override
    public void insertUpdate(DocumentEvent documentEvent) {
        updateOKButton();
    }

    @Override
    public void removeUpdate(DocumentEvent documentEvent) {
        insertUpdate(documentEvent);
    }

    @Override
    public void changedUpdate(DocumentEvent documentEvent) {
        // Plain text components do not fire these events
    }
}