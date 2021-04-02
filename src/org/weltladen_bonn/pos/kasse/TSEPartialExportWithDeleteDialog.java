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

/*
Ausgangspunkt dieser Klasse:

Selektives Löschen von Logdaten
===============================
(https://tse-support.cryptovision.com/jira/servicedesk/customer/kb/view/6619503)

Es ist bei der cryptovision TSE möglich, nur Logdaten bis zu einer SignaturNummer
(Anm. MV: Signatur-Zähler, d.h. signatureCounter) zu löschen:

  deleteStoredDataUpTo()

Zusammen mit der Funktion exportMoreData() lässt sich so ein schrittweiser Export -
beispielsweise tageweise - sehr leicht realisieren.
*/

public class TSEPartialExportWithDeleteDialog extends DialogWindow implements DocumentListener {
    private static final Logger logger = LogManager.getLogger(TSEPartialExportDialog.class);

    private JTextField numberRecordsField;

    private JButton okButton;
    private JButton cancelButton;
    private boolean aborted = true;


    // Methoden:
    public TSEPartialExportWithDeleteDialog(MainWindowGrundlage mw, JDialog dia) {
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

        JTextArea erklaerText = new JTextArea(7, 30);
        erklaerText.append(
            "Es werden alle auf der TSE vorhandenen Signaturdaten bis zu einer "+
            "bestimmten Signatur zuerst exportiert und dann von der TSE gelöscht.\n"+
            "Dies sollte nur benutzt werden, wenn der Speicherplatz auf der TSE "+
            "knapp zu werden droht.\n"+
            "Bitte die exportierte Datei gut aufbewahren und auch Sicherungskopien anlegen!"
        );
        erklaerText = makeLabelStyle(erklaerText);
        erklaerText.setFont(BaseClass.mediumFont);
        erklaerText.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        headerPanel.add(erklaerText);

        allPanel.add(headerPanel, BorderLayout.NORTH);
    }

    private JTextField setupTextField() {
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
        numberRecordsField = setupTextField();
        ((AbstractDocument)numberRecordsField.getDocument()).setDocumentFilter(
            new IntegerDocumentFilter(1, null, "Signaturen", this)
        );

        // JPanel adminPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.VERTICAL;
        c.anchor = GridBagConstraints.WEST;
        c.ipadx = 15;
        c.ipady = 10;
        c.insets = new Insets(3, 10, 3, 3);
        // *** Radio Buttons:
        // *** Labels and Text Fields:
        c.gridy = 0;
        c.gridx = 0; middlePanel.add(new JLabel("Anzahl Signaturen, die exportiert und gelöscht werden sollen:"), c);
        c.gridx = 1; middlePanel.add(numberRecordsField, c);

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

    public Long getNumberRecords() {
        String s = numberRecordsField.getText();
        return s.length() > 0 ? Long.parseLong(s) : null;
    }

    /**
     * Each non abstract class that implements the ActionListener
     * must have this method.
     *
     * @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        updateOKButton();
        if (e.getSource() == okButton) {
            aborted = false;
            this.window.dispose();
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
        if (
            numberRecordsField.getDocument().getLength() > 0
        ) {
            okButton.setEnabled(true);
        } else {
            okButton.setEnabled(false);
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