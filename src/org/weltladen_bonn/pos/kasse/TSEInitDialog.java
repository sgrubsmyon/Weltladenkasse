package org.weltladen_bonn.pos.kasse;

import org.weltladen_bonn.pos.BaseClass;
import org.weltladen_bonn.pos.DialogWindow;
import org.weltladen_bonn.pos.MainWindowGrundlage;
import org.weltladen_bonn.pos.BaseClass.BigLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TSEInitDialog extends DialogWindow implements WindowListener {
    private static final Logger logger = LogManager.getLogger(TSEInitDialog.class);

    private JTextField adminPINField;
    private JTextField adminPUKField;
    private JTextField timeAdminPINField;
    private JTextField timeAdminPUKField;

    private JButton okButton;
    private JButton cancelButton;
    private boolean aborted = true;

    // Methoden:
    public TSEInitDialog(MainWindowGrundlage mw, JDialog dia) {
        super(null, mw, dia);
        showAll();
        dia.addWindowListener(this);
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

        JPanel adminPanel = new JPanel(new GridBagLayout());
        adminPanel.setBorder(BorderFactory.createTitledBorder("Admin"));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.ipady = 5;
        c.insets = new Insets(3, 0, 3, 3);
        c.gridy = 0;
        c.gridx = 0;
        adminPanel.add(new BigLabel("Admin PIN:"), c);
        c.gridx = 1;
        adminPINField = new JTextField();
        adminPanel.add(adminPINField, c);
        c.gridx = 2;
        adminPanel.add(new BigLabel("Admin PUK:"), c);
        c.gridx = 3;
        adminPanel.add(new BigLabel("TEXTFELD"), c);

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

    /**
     * Each non abstract class that implements the ActionListener
     * must have this method.
     *
     * @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            // int answer = JOptionPane.showConfirmDialog(this,
            //         "Bitte genau prüfen, ob die Eingaben stimmen!\n\n"+
            //                 "Stimmt alles?",
            //         "Alles korrekt?",
            //         JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            // if (answer == JOptionPane.YES_OPTION) {
            //     LinkedHashMap<BigDecimal, Integer> zaehlprotokoll = getZaehlprotokoll();
            //     // communicate that zehlprotokoll was successful:
            //     this.abrechnungen.setZaehlprotokoll(zaehlprotokoll);
            //     this.abrechnungen.setZaehlprotokollKommentar(kommentarArea.getText());
            //     this.window.dispose();
            // }
            this.aborted = false;
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
            JOptionPane.showMessageDialog(this.window,
                "ACHTUNG: Die Initialisierung der TSE wurde abgebrochen!\n"+
                "Ohne Initialisierung kann eine neue TSE nicht verwendet werden.\n"+
                "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                "Bitte beim nächsten Start der Kassensoftware die TSE initialisieren.",
                "Abbruch der Initialisierung der TSE", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
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
}