package org.weltladen_bonn.pos.kasse;

import org.weltladen_bonn.pos.BaseClass;
import org.weltladen_bonn.pos.DialogWindow;
import org.weltladen_bonn.pos.MainWindowGrundlage;
import org.weltladen_bonn.pos.BaseClass.BigLabel;
import org.weltladen_bonn.pos.IntegerDocumentFilter;

// Basic Java stuff:
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.*;
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.StyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GutscheinEinloesenDialog extends DialogWindow implements DocumentListener {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LogManager.getLogger(TSEPINOrPUKEntryDialog.class);

    private JTextField gutscheinNrField;
    private JPanel gutscheinFoundPanelContainer = new JPanel(new BorderLayout());
    private JPanel gutscheinFoundPanel = new JPanel(new FlowLayout());
    private JTextField einloesWertField;
    private BigDecimal restWert = null;

    private JTextArea notFoundLabel = niceTextArea("Gutschein konnte nicht gefunden werden!");

    private JButton okButton;
    private JButton cancelButton;
    private boolean aborted = true;

    // Methoden:
    public GutscheinEinloesenDialog(MariaDbPoolDataSource pool, MainWindowGrundlage mw, JDialog dia) {
        super(pool, mw, dia);
        showAll();
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

        JTextArea erklaerText = new JTextArea(2, 40);
        erklaerText.append("Welcher Gutschein soll eingelöst werden?\nBitte Gutschein-Nr. und gewünschten Wert eingeben.");
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

        JPanel gutscheinPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.VERTICAL;
        c.anchor = GridBagConstraints.CENTER;
        
        c.ipadx = 15;
        c.ipady = 10;
        c.insets = new Insets(3, 0, 3, 3);
        c.gridx = 0; c.gridy = 0;
        gutscheinPanel.add(new BigLabel("Gutschein-Nr.:"), c);
        c.gridx = 1; c.gridy = 0;
        gutscheinNrField = new JTextField();
        gutscheinNrField.setColumns(30);
        gutscheinNrField.setHorizontalAlignment(SwingConstants.RIGHT);
        gutscheinNrField.getDocument().addDocumentListener(this);
        ((AbstractDocument)gutscheinNrField.getDocument()).setDocumentFilter(
            new IntegerDocumentFilter(1, null, "Gutschein-Nr.", this) // smallest allowed value is 1
        );
        gutscheinPanel.add(gutscheinNrField, c);

        c.gridwidth = 2;
        c.gridx = 0; c.gridy = 1;
        gutscheinFoundPanelContainer.add(gutscheinFoundPanel, BorderLayout.CENTER);
        buildGutscheinFoundPanel(new JComponent[] {notFoundLabel});
        gutscheinFoundPanelContainer.setPreferredSize(new Dimension(600, 100));
        gutscheinPanel.add(gutscheinFoundPanelContainer, c);

        c.gridwidth = 1;
        c.gridx = 0; c.gridy = 2;
        gutscheinPanel.add(new BigLabel("Einzulösender Wert:"), c);
        c.gridx = 1; c.gridy = 2;
        einloesWertField = new JTextField();
        einloesWertField.setColumns(30);
        einloesWertField.setHorizontalAlignment(SwingConstants.RIGHT);
        einloesWertField.getDocument().addDocumentListener(this);
        einloesWertField.setEnabled(false);
        ((AbstractDocument)einloesWertField.getDocument()).setDocumentFilter(
            bc.geldFilter
        );
        gutscheinPanel.add(einloesWertField, c);
        c.gridx = 2; c.gridy = 2;
        gutscheinPanel.add(new BigLabel(bc.currencySymbol), c);

        middlePanel.add(gutscheinPanel);

        allPanel.add(middlePanel, BorderLayout.CENTER);
    }

    private void buildGutscheinFoundPanel(JComponent[] components) {
        gutscheinFoundPanelContainer.remove(gutscheinFoundPanel);
        gutscheinFoundPanelContainer.revalidate();
        gutscheinFoundPanel = new JPanel(new FlowLayout());
        gutscheinFoundPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        for (JComponent component : components) {
            component.setAlignmentX(Component.CENTER_ALIGNMENT);
            gutscheinFoundPanel.add(component);
        }
        gutscheinFoundPanelContainer.add(gutscheinFoundPanel, BorderLayout.CENTER);
    }

    private JTextArea niceTextArea(String text) {
        JTextArea niceArea = new JTextArea(3, 35);
        niceArea.append(text);
        niceArea = makeLabelStyle(niceArea);
        niceArea.setFont(BaseClass.mediumFont);
        niceArea.setAlignmentY(JTextArea.CENTER_ALIGNMENT);
        // niceArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        // niceArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // // center vertically: (did not get it to work)
        // JTextPane pane = new JTextPane();
        // pane.setDocument(niceArea.getDocument());
        // StyledDocument doc = pane.getStyledDocument();
        // SimpleAttributeSet center = new SimpleAttributeSet();
        // StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        // doc.setParagraphAttributes(0, doc.getLength(), center, false);

        return niceArea;
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

    public String[] queryForGutschein(int nr) {
        String[] answer = new String[] {"not found"};
        restWert = null;
        try {
            // insert into table gutschein
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT rest, DATE_FORMAT(ausstelldatum, '"+bc.dateFormatSQL+"'), startwert "+
                "FROM ("+
                "  SELECT "+
                "    MIN(restbetrag) AS rest, "+
                "    MIN(datum) AS ausstelldatum, "+
                "    MAX(restbetrag) AS startwert "+
                "  FROM gutschein WHERE gutschein_nr = ?"+
                ") AS d"
            );
            pstmtSetInteger(pstmt, 1, nr);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) { // exactly one row should be returned (contains only NULLs if gutschein was not found)
                restWert = rs.getBigDecimal(1);
                String rest = bc.priceFormatter(restWert)+" "+bc.currencySymbol;
                String ausstelldatum = rs.getString(2);
                String startwert = bc.priceFormatter(rs.getString(3))+" "+bc.currencySymbol;
                if (rest != null && ausstelldatum != null && startwert != null) {
                    answer = new String[] {rest, ausstelldatum, startwert};
                }
            } else {
                logger.error("No row was returned from DB, but it should have been");
                answer = new String[] {"Fehler: Datenbank hat nichts zurückgeliefert"};
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            answer = new String[] {"Fehler in der Datenbank-Abfrage"};
            showDBErrorDialog(ex.getMessage());
        }
        return answer;
    }

    @Override
    protected int submit() {
        return 0;
    }

    public Integer getGutscheinNr() {
        return Integer.parseInt(gutscheinNrField.getText());
    }

    public BigDecimal getEinloesWert() {
        return new BigDecimal(bc.priceFormatterIntern(einloesWertField.getText()));
    }

    public boolean getAborted() {
        return aborted;
    }

    /**
     * Each non abstract class that implements the ActionListener
     * must have this method.
     *
     * @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            this.aborted = false;
            this.window.dispose();
            return;
        }
        if (e.getSource() == cancelButton) {
            this.window.dispose();
            return;
        }
        super.actionPerformed(e);
    }

    private void checkEinloesWert() {
        BigDecimal wert = null;
        try {
            wert = new BigDecimal(bc.priceFormatterIntern(einloesWertField.getText()));
        } catch (NumberFormatException ex) {
            wert = null;
        }
        boolean decision = false;
        if (wert != null) {
            if (wert.compareTo(bc.zero) > 0) decision = true;
            if (restWert != null && wert.compareTo(restWert) > 0) {
                decision = false;
                JOptionPane.showMessageDialog(this,
                    "Eingegebener Wert ist größer als vorhandener Restbetrag!",
                    "Einzulösender Wert zu groß", JOptionPane.WARNING_MESSAGE);
            }
        }
        okButton.setEnabled(decision);
    }

    /**
     * Each non abstract class that implements the DocumentListener must have
     * these methods.
     *
     * @param documentEvent
     *            the document event.
     **/
    @Override
    public void insertUpdate(DocumentEvent e) {
        if (e.getDocument() == gutscheinNrField.getDocument()) {
            Integer nr = null;
            try {
                nr = Integer.parseInt(gutscheinNrField.getText());
            } catch (NumberFormatException ex) {
                nr = null;
            }
            if (nr == null) {
                buildGutscheinFoundPanel(new JComponent[] {notFoundLabel});
                einloesWertField.setEnabled(false);
                okButton.setEnabled(false);
            } else if (nr < 200) { // TODO ändern, nachdem alle alten Gutscheine (<200) eingelöst worden sind
                buildGutscheinFoundPanel(new JComponent[] {niceTextArea(
                    "Bei Gutscheinen mit Nr. unter 200 bitte in der Papierliste "+
                    "den vorhandenen Restbetrag prüfen und die Einlösung auch in die "+
                    "Papierliste eintragen!"
                )});
                einloesWertField.setEnabled(true);
                restWert = null;
                checkEinloesWert();
            } else {
                String[] gutscheinData = queryForGutschein(nr);
                if (gutscheinData.length == 3) {
                    BigLabel rest = new BigLabel(gutscheinData[0]);
                    BigLabel ausstelldatum = new BigLabel(gutscheinData[1]);
                    BigLabel startwert = new BigLabel(gutscheinData[2]);
                    rest.setForeground(Color.GREEN.darker());
                    ausstelldatum.setForeground(Color.BLUE);
                    startwert.setForeground(Color.GRAY);
                    buildGutscheinFoundPanel(new JComponent[] {
                        new BigLabel("Vorhandener Restbetrag: "),
                        rest,
                        new BigLabel(", ausgestellt am: "),
                        ausstelldatum,
                        new BigLabel(" (Wert ursprünglich: "),
                        startwert,
                        new BigLabel(")")
                    });
                    einloesWertField.setEnabled(true);
                    checkEinloesWert();
                } else if (gutscheinData[0].startsWith("Fehler")) {
                    buildGutscheinFoundPanel(new JComponent[] {niceTextArea(
                        gutscheinData[0]
                    )});
                    einloesWertField.setEnabled(false);
                    okButton.setEnabled(false);
                } else {
                    buildGutscheinFoundPanel(new JComponent[] {notFoundLabel});
                    einloesWertField.setEnabled(false);
                    okButton.setEnabled(false);
                }
            }
            return;
        }
        if (e.getDocument() == einloesWertField.getDocument()) {
            checkEinloesWert();
            return;
        }
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        insertUpdate(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        // Plain text components do not fire these events
    }
}