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
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TSEPINEntryDialog extends DialogWindow implements WindowListener, DocumentListener {
    private static final Logger logger = LogManager.getLogger(TSEInitDialog.class);

    private WeltladenTSE tse = null;

    private JTextField pinField;
    private String role;
    private String numbertype;
    private int places;

    private JButton okButton;
    private JButton cancelButton;
    private boolean aborted = true;

    // Methoden:
    public TSEPINEntryDialog(MainWindowGrundlage mw, JDialog dia, WeltladenTSE _tse, String r, String nt, int p) {
        super(null, mw, dia);
        this.tse = _tse;
        this.role = r;
        this.numbertype = nt;
        this.places = p;
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
        if (role == "TimeAdmin" && numbertype == "PIN") {
            erklaerText.append(
                "Die TimeAdmin PIN der TSE konnte nicht geladen werden.\n"+
                "Bitte jetzt eingeben.\n"+
                "Es wird dann (erneut) versucht, die PIN dauerhaft\n"+
                "zu speichern.");
        } else {
            erklaerText.append("Bitte die "+role+" "+numbertype+" der TSE eingeben.");
        }
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
        adminPanel.add(new BigLabel(role+" "+numbertype+": ("+places+"-stellig)"), c);
        c.gridx = 1;
        pinField = new JTextField();
        pinField.setColumns(20);
        pinField.setHorizontalAlignment(SwingConstants.RIGHT);
        pinField.getDocument().addDocumentListener(this);
        adminPanel.add(pinField, c);

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

    public byte[] getPIN() {
        return pinField.getText().getBytes();
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
                "Sicher, dass die "+role+" "+numbertype+" stimmt?",
                "Fortfahren?",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                this.aborted = false;
                if (role == "TimeAdmin" && numbertype == "PIN") {
                    byte[] timeAdminPIN = pinField.getText().getBytes();
                    tse.writeTimeAdminPINtoFile(timeAdminPIN);
                }
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
            logger.fatal("TSE PIN entry was canceled by user!");
            JOptionPane.showMessageDialog(this.window,
                "ACHTUNG: Die "+numbertype+"-Eingabe der TSE wurde abgebrochen!\n"+
                "Ohne "+numbertype+" kann die TSE nicht verwendet werden.\n"+
                "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                "Bitte beim nächsten mal die "+numbertype+" eingeben.",
                "Abbruch der "+numbertype+"-Eingabe der TSE", JOptionPane.ERROR_MESSAGE);
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

    /**
     * Each non abstract class that implements the DocumentListener must have
     * these methods.
     *
     * @param documentEvent
     *            the document event.
     **/
    @Override
    public void insertUpdate(DocumentEvent documentEvent) {
        if (pinField.getDocument().getLength() == 8) {
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