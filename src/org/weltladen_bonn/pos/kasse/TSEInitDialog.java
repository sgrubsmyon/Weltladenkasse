package org.weltladen_bonn.pos.kasse;

import org.weltladen_bonn.pos.BaseClass;
import org.weltladen_bonn.pos.DialogWindow;
import org.weltladen_bonn.pos.MainWindowGrundlage;
import org.weltladen_bonn.pos.BaseClass.BigLabel;
import org.weltladen_bonn.pos.kasse.WeltladenTSE.TSEStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TSEInitDialog extends DialogWindow implements WindowListener, DocumentListener {
    private static final Logger logger = LogManager.getLogger(TSEInitDialog.class);

    private WeltladenTSE tse = null;

    private JTextField adminPINField;
    private JTextField adminPUKField;
    private JTextField timeAdminPINField;
    private JTextField timeAdminPUKField;

    private JButton okButton;
    private JButton cancelButton;
    private boolean aborted = true;

    // Methoden:
    public TSEInitDialog(MainWindowGrundlage mw, JDialog dia, WeltladenTSE _tse) {
        super(null, mw, dia);
        showAll();
        dia.addWindowListener(this);
        this.tse = _tse;
    }

    // will data be lost on close?
    protected boolean willDataBeLost() {
        return false;
    }

    protected void showHeader() {
        /**
         * Informations-Panel
         * */
        headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        
        // borders:
        int top = 5, left = 5, bottom = 5, right = 5;
        headerPanel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));

        JTextArea erklaerText = new JTextArea(2, 30);
        erklaerText.append("Die TSE muss vor ihrer ersten Benutzung initialisiert werden.\n" +
                "Dies geschieht durch die Eingabe der PIN- und PUK-Codes.");
        erklaerText = makeLabelStyle(erklaerText);
        erklaerText.setFont(BaseClass.mediumFont);
        erklaerText.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        headerPanel.add(erklaerText);

        allPanel.add(headerPanel, BorderLayout.NORTH);
    }

    protected void showMiddle() {
        /**
         * TextField-Panel
         * */
        JPanel middlePanel = new JPanel();
        middlePanel.setLayout(new BoxLayout(middlePanel, BoxLayout.Y_AXIS));

        // borders:
        int top = 10, left = 10, bottom = 10, right = 10;
        middlePanel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));

        JPanel adminPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.VERTICAL;
        c.anchor = GridBagConstraints.CENTER;
        c.ipadx = 15;
        c.ipady = 10;
        c.insets = new Insets(3, 0, 3, 3);
        c.gridy = 0;
        c.gridx = 0;
        adminPanel.add(new BigLabel("Admin PIN: (8-stellig)"), c);
        c.gridx = 1;
        adminPINField = new JTextField();
        adminPINField.setColumns(20);
        adminPINField.setHorizontalAlignment(SwingConstants.RIGHT);
        adminPINField.getDocument().addDocumentListener(this);
        adminPanel.add(adminPINField, c);
        c.gridy = 1;
        c.gridx = 0;
        adminPanel.add(new BigLabel("Admin PUK: (10-stellig)"), c);
        c.gridx = 1;
        adminPUKField = new JTextField();
        adminPUKField.setColumns(20);
        adminPUKField.setHorizontalAlignment(SwingConstants.RIGHT);
        adminPUKField.getDocument().addDocumentListener(this);
        adminPanel.add(adminPUKField, c);
        c.gridy = 0;
        c.gridx = 2;
        adminPanel.add(new BigLabel("TimeAdmin PIN: (8-stellig)"), c);
        c.gridx = 3;
        timeAdminPINField = new JTextField();
        timeAdminPINField.setColumns(20);
        timeAdminPINField.setHorizontalAlignment(SwingConstants.RIGHT);
        timeAdminPINField.getDocument().addDocumentListener(this);
        adminPanel.add(timeAdminPINField, c);
        c.gridy = 1;
        c.gridx = 2;
        adminPanel.add(new BigLabel("TimeAdmin PUK: (10-stellig)"), c);
        c.gridx = 3;
        timeAdminPUKField = new JTextField();
        timeAdminPUKField.setColumns(20);
        timeAdminPUKField.setHorizontalAlignment(SwingConstants.RIGHT);
        timeAdminPUKField.getDocument().addDocumentListener(this);
        adminPanel.add(timeAdminPUKField, c);

        middlePanel.add(adminPanel);

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

    public byte[] getAdminPIN() {
        return adminPINField.getText().getBytes();
    }

    /**
     * Each non abstract class that implements the ActionListener
     * must have this method.
     *
     * @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            int answer = JOptionPane.showConfirmDialog(this,
                "Ganz sicher, dass die PINs und PUKs stimmen?\n"+
                "Bitte die PINs/PUKs sorgfältig notieren.\n\n"+
                "Dieser Schritt kann nicht rückgängig gemacht werden und\n"+
                "die PINs/PUKs können nicht erneut gesetzt werden!",
                "Fortfahren?",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                this.aborted = false;
                byte[] adminPIN = adminPINField.getText().getBytes();
                byte[] adminPUK = adminPUKField.getText().getBytes();
                byte[] timeAdminPIN = timeAdminPINField.getText().getBytes();
                byte[] timeAdminPUK = timeAdminPUKField.getText().getBytes();
                tse.setPINandPUK(adminPIN, adminPUK, timeAdminPIN, timeAdminPUK);
                this.window.dispose();
            }
            return;
        }
        if (e.getSource() == cancelButton) {
            this.window.dispose();
            return;
        }
        super.actionPerformed(e);
    }
    
    /**
     * Each non abstract class that implements the WindowListener
     * must have these methods.
     *
     * @param e the action event.
     **/
    public void windowClosed(WindowEvent e) {
        if (this.aborted) {
            logger.fatal("TSE initialization was canceled by user!");
            tse.setStatus(TSEStatus.failed);
            tse.setFailReason("Die Initialisierung der TSE wurde abgebrochen. Bitte beim nächsten Start der Kassensoftware die TSE initialisieren.");
            tse.showTSEFailWarning();
        }
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }
    
    public void windowClosing(WindowEvent e) {
    }
    
    public void windowOpened(WindowEvent e) {
    }

    /**
     * Each non abstract class that implements the DocumentListener must have
     * these methods.
     *
     * @param documentEvent
     *            the document event.
     **/
    @Override
    public void insertUpdate(DocumentEvent documentEvent) {
        if (
            adminPINField.getDocument().getLength() == 8 &&
            adminPUKField.getDocument().getLength() == 10 &&
            timeAdminPINField.getDocument().getLength() == 8 &&
            timeAdminPUKField.getDocument().getLength() == 10
        ) {
            okButton.setEnabled(true);
        } else {
            okButton.setEnabled(false);
        }
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