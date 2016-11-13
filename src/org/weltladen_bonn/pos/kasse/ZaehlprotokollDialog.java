package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:

import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JSpinnerDateEditor;
import hirondelle.date4j.DateTime;
import org.weltladen_bonn.pos.DialogWindow;
import org.weltladen_bonn.pos.MainWindowGrundlage;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

// MySQL Connector/J stuff:
// GUI stuff:
//import java.awt.BorderLayout;
//import java.awt.FlowLayout;
//import java.awt.Dimension;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import javax.swing.JFrame;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTable;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
// DateTime from date4j (http://www.date4j.net/javadoc/index.html)
//import java.util.Calendar;
// JCalendar

public class ZaehlprotokollDialog extends DialogWindow
        implements ChangeListener {
    // Attribute:
    private AbrechnungenTag abrechnungen;

    private JButton okButton;
    private JButton cancelButton;

    // Methoden:
    public ZaehlprotokollDialog(Connection conn, MainWindowGrundlage mw,
                                AbrechnungenTag at, JDialog dia) {
        super(conn, mw, dia);
        this.abrechnungen = at;
        showAll();
    }

    protected void showHeader() {
        /**
         * Informations-Panel
         * */
        headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        // borders:
        int top = 10, left = 10, bottom = 10, right = 10;
        headerPanel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));

        JTextArea erklaerText = new JTextArea(7, 30);
        erklaerText.append("Der Kassenbestand in allen Münz- und Scheinsorten " +
                "ist vollständig zu protokollieren!\n\n" +
                "Bitte zähle, wie viele Münzen und Scheine von jeder Sorte " +
                "in der Kasse sind, und trage es hier ein:" );
        erklaerText = makeLabelStyle(erklaerText);
        erklaerText.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        headerPanel.add(erklaerText);

        allPanel.add(headerPanel, BorderLayout.NORTH);
    }

    protected void showMiddle() {
        /**
         * Spinner-Panel
         * */
        JPanel middlePanel = new JPanel();
        middlePanel.setLayout(new BoxLayout(middlePanel, BoxLayout.Y_AXIS));

        JPanel muenzPanel = new JPanel(new GridBagLayout());
        muenzPanel.setBorder(BorderFactory.createTitledBorder("Münzen" ));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 10;
        c.insets = new Insets(3, 10, 3, 10);

        JLabel muenze_1cent_label = new JLabel("1 Cent:" );
        JLabel muenze_2cent_label = new JLabel("2 Cent:" );
        JLabel muenze_5cent_label = new JLabel("5 Cent:" );
        JLabel muenze_10cent_label = new JLabel("10 Cent:" );
        JLabel muenze_20cent_label = new JLabel("20 Cent:" );
        JLabel muenze_50cent_label = new JLabel("50 Cent:" );
        JLabel muenze_1euro_label = new JLabel("1 Euro:" );
        JLabel muenze_2euro_label = new JLabel("2 Euro:" );

        Icon muenze_1cent_icon = new ImageIcon(getClass().getResource("/resources/icons/coins/1cent_klein.gif" ), "1 Cent");
        Icon muenze_2cent_icon = new ImageIcon(getClass().getResource("/resources/icons/coins/2cent_klein.gif" ), "2 Cent");
        Icon muenze_5cent_icon = new ImageIcon(getClass().getResource("/resources/icons/coins/5cent_klein.gif" ), "5 Cent");
        Icon muenze_10cent_icon = new ImageIcon(getClass().getResource("/resources/icons/coins/10cent_klein.gif" ), "10 Cent");
        Icon muenze_20cent_icon = new ImageIcon(getClass().getResource("/resources/icons/coins/20cent_klein.gif" ), "20 Cent");
        Icon muenze_50cent_icon = new ImageIcon(getClass().getResource("/resources/icons/coins/50cent_klein.gif" ), "50 Cent");
        Icon muenze_1euro_icon = new ImageIcon(getClass().getResource("/resources/icons/coins/1euro_klein.gif" ), "1 Euro");
        Icon muenze_2euro_icon = new ImageIcon(getClass().getResource("/resources/icons/coins/2euro_klein.gif" ), "2 Euro");

        JSpinner muenze_1cent_spinner = new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1));
        JSpinner muenze_2cent_spinner = new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1));
        JSpinner muenze_5cent_spinner = new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1));
        JSpinner muenze_10cent_spinner = new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1));
        JSpinner muenze_20cent_spinner = new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1));
        JSpinner muenze_50cent_spinner = new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1));
        JSpinner muenze_1euro_spinner = new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1));
        JSpinner muenze_2euro_spinner = new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1));

        JFormattedTextField muenze_1cent_field = new JFormattedTextField();
        JFormattedTextField muenze_2cent_field = new JFormattedTextField();
        JFormattedTextField muenze_5cent_field = new JFormattedTextField();
        JFormattedTextField muenze_10cent_field = new JFormattedTextField();
        JFormattedTextField muenze_20cent_field = new JFormattedTextField();
        JFormattedTextField muenze_50cent_field = new JFormattedTextField();
        JFormattedTextField muenze_1euro_field = new JFormattedTextField();
        JFormattedTextField muenze_2euro_field = new JFormattedTextField();
//        kundeGibtField.getDocument().addDocumentListener(new DocumentListener() {
//            public void insertUpdate(DocumentEvent e) {
//                updateRueckgeld();
//            }
//
//            public void removeUpdate(DocumentEvent e) {
//                updateRueckgeld();
//            }
//
//            public void changedUpdate(DocumentEvent e) {
//                // Plain text components do not fire these events
//            }
//        });
        ((AbstractDocument) muenze_1cent_field.getDocument()).setDocumentFilter(bc.geldFilter);
        ((AbstractDocument) muenze_2cent_field.getDocument()).setDocumentFilter(bc.geldFilter);
        ((AbstractDocument) muenze_5cent_field.getDocument()).setDocumentFilter(bc.geldFilter);
        ((AbstractDocument) muenze_10cent_field.getDocument()).setDocumentFilter(bc.geldFilter);
        ((AbstractDocument) muenze_20cent_field.getDocument()).setDocumentFilter(bc.geldFilter);
        ((AbstractDocument) muenze_50cent_field.getDocument()).setDocumentFilter(bc.geldFilter);
        ((AbstractDocument) muenze_1euro_field.getDocument()).setDocumentFilter(bc.geldFilter);
        ((AbstractDocument) muenze_2euro_field.getDocument()).setDocumentFilter(bc.geldFilter);

        c.gridy = 2;
        c.gridx = 0;
        muenzPanel.add(new JLabel("Anzahl:"), c);
        c.gridy = 4;
        c.gridx = 0;
        muenzPanel.add(new JLabel("Betrag:"), c);

        c.gridy = 0;
        c.gridx = 1;
        c.anchor = GridBagConstraints.CENTER;
        muenzPanel.add(muenze_1cent_label, c);
        c.gridy = 0;
        c.gridx = 3;
        c.anchor = GridBagConstraints.CENTER;
        muenzPanel.add(muenze_2cent_label, c);
        c.gridy = 0;
        c.gridx = 5;
        c.anchor = GridBagConstraints.CENTER;
        muenzPanel.add(muenze_5cent_label, c);
        c.gridy = 0;
        c.gridx = 7;
        c.anchor = GridBagConstraints.CENTER;
        muenzPanel.add(muenze_10cent_label, c);
        c.gridy = 0;
        c.gridx = 9;
        c.anchor = GridBagConstraints.CENTER;
        muenzPanel.add(muenze_20cent_label, c);
        c.gridy = 0;
        c.gridx = 11;
        c.anchor = GridBagConstraints.CENTER;
        muenzPanel.add(muenze_50cent_label, c);
        c.gridy = 0;
        c.gridx = 13;
        c.anchor = GridBagConstraints.CENTER;
        muenzPanel.add(muenze_1euro_label, c);
        c.gridy = 0;
        c.gridx = 15;
        c.anchor = GridBagConstraints.CENTER;
        muenzPanel.add(muenze_2euro_label, c);

        c.gridy = 1;
        c.gridx = 1;
        muenzPanel.add(new JLabel(muenze_1cent_icon), c);
        c.gridy = 1;
        c.gridx = 3;
        muenzPanel.add(new JLabel(muenze_2cent_icon), c);
        c.gridy = 1;
        c.gridx = 5;
        muenzPanel.add(new JLabel(muenze_5cent_icon), c);
        c.gridy = 1;
        c.gridx = 7;
        muenzPanel.add(new JLabel(muenze_10cent_icon), c);
        c.gridy = 1;
        c.gridx = 9;
        muenzPanel.add(new JLabel(muenze_20cent_icon), c);
        c.gridy = 1;
        c.gridx = 11;
        muenzPanel.add(new JLabel(muenze_50cent_icon), c);
        c.gridy = 1;
        c.gridx = 13;
        muenzPanel.add(new JLabel(muenze_1euro_icon), c);
        c.gridy = 1;
        c.gridx = 15;
        muenzPanel.add(new JLabel(muenze_2euro_icon), c);

        c.gridy = 2;
        c.gridx = 1;
        muenzPanel.add(muenze_1cent_spinner, c);
        c.gridy = 2;
        c.gridx = 3;
        muenzPanel.add(muenze_2cent_spinner, c);
        c.gridy = 2;
        c.gridx = 5;
        muenzPanel.add(muenze_5cent_spinner, c);
        c.gridy = 2;
        c.gridx = 7;
        muenzPanel.add(muenze_10cent_spinner, c);
        c.gridy = 2;
        c.gridx = 9;
        muenzPanel.add(muenze_20cent_spinner, c);
        c.gridy = 2;
        c.gridx = 11;
        muenzPanel.add(muenze_50cent_spinner, c);
        c.gridy = 2;
        c.gridx = 13;
        muenzPanel.add(muenze_1euro_spinner, c);
        c.gridy = 2;
        c.gridx = 15;
        muenzPanel.add(muenze_2euro_spinner, c);

        c.gridy = 3;
        c.gridx = 1;
        muenzPanel.add(new JLabel("oder"), c);
        c.gridy = 3;
        c.gridx = 3;
        muenzPanel.add(new JLabel("oder"), c);
        c.gridy = 3;
        c.gridx = 5;
        muenzPanel.add(new JLabel("oder"), c);
        c.gridy = 3;
        c.gridx = 7;
        muenzPanel.add(new JLabel("oder"), c);
        c.gridy = 3;
        c.gridx = 9;
        muenzPanel.add(new JLabel("oder"), c);
        c.gridy = 3;
        c.gridx = 11;
        muenzPanel.add(new JLabel("oder"), c);
        c.gridy = 3;
        c.gridx = 13;
        muenzPanel.add(new JLabel("oder"), c);
        c.gridy = 3;
        c.gridx = 15;
        muenzPanel.add(new JLabel("oder"), c);

        c.gridy = 4;
        c.gridx = 1;
        muenzPanel.add(muenze_1cent_field, c);
        c.gridy = 4;
        c.gridx = 3;
        muenzPanel.add(muenze_2cent_field, c);
        c.gridy = 4;
        c.gridx = 5;
        muenzPanel.add(muenze_5cent_field, c);
        c.gridy = 4;
        c.gridx = 7;
        muenzPanel.add(muenze_10cent_field, c);
        c.gridy = 4;
        c.gridx = 9;
        muenzPanel.add(muenze_20cent_field, c);
        c.gridy = 4;
        c.gridx = 11;
        muenzPanel.add(muenze_50cent_field, c);
        c.gridy = 4;
        c.gridx = 13;
        muenzPanel.add(muenze_1euro_field, c);
        c.gridy = 4;
        c.gridx = 15;
        muenzPanel.add(muenze_2euro_field, c);

        c.gridy = 4;
        c.gridx = 2;
        muenzPanel.add(new JLabel(bc.currencySymbol), c);
        c.gridy = 4;
        c.gridx = 4;
        muenzPanel.add(new JLabel(bc.currencySymbol), c);
        c.gridy = 4;
        c.gridx = 6;
        muenzPanel.add(new JLabel(bc.currencySymbol), c);
        c.gridy = 4;
        c.gridx = 8;
        muenzPanel.add(new JLabel(bc.currencySymbol), c);
        c.gridy = 4;
        c.gridx = 10;
        muenzPanel.add(new JLabel(bc.currencySymbol), c);
        c.gridy = 4;
        c.gridx = 12;
        muenzPanel.add(new JLabel(bc.currencySymbol), c);
        c.gridy = 4;
        c.gridx = 14;
        muenzPanel.add(new JLabel(bc.currencySymbol), c);
        c.gridy = 4;
        c.gridx = 16;
        muenzPanel.add(new JLabel(bc.currencySymbol), c);





        JPanel scheinPanel = new JPanel(new GridBagLayout());
        scheinPanel.setBorder(BorderFactory.createTitledBorder("Scheine" ));
        GridBagConstraints c2 = new GridBagConstraints();
        c2.anchor = GridBagConstraints.CENTER;
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.ipady = 10;
        c2.insets = new Insets(3, 10, 3, 10);

        JLabel schein_5euro_label = new JLabel("5 Euro:" );
        JLabel schein_10euro_label = new JLabel("10 Euro:" );
        JLabel schein_20euro_label = new JLabel("20 Euro:" );
        JLabel schein_50euro_label = new JLabel("50 Euro:" );
        JLabel schein_100euro_label = new JLabel("100 Euro:" );
        JLabel schein_200euro_label = new JLabel("200 Euro:" );

        Icon schein_5euro_icon = new ImageIcon(getClass().getResource("/resources/icons/banknotes/5euro_neu_vorne_klein.jpg" ), "5 Euro");
        Icon schein_10euro_icon = new ImageIcon(getClass().getResource("/resources/icons/banknotes/10euro_neu_vorne_klein.jpg" ), "10 Euro");
        Icon schein_20euro_icon = new ImageIcon(getClass().getResource("/resources/icons/banknotes/20euro_neu_vorne_klein.jpg" ), "20 Euro");
        Icon schein_50euro_icon = new ImageIcon(getClass().getResource("/resources/icons/banknotes/50euro_neu_vorne_klein.jpg" ), "50 Euro");
        Icon schein_100euro_icon = new ImageIcon(getClass().getResource("/resources/icons/banknotes/100euro_klein.gif" ), "100 Euro");
        Icon schein_200euro_icon = new ImageIcon(getClass().getResource("/resources/icons/banknotes/200euro_klein.gif" ), "200 Euro");

        JSpinner schein_5euro_spinner = new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1));
        JSpinner schein_10euro_spinner = new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1));
        JSpinner schein_20euro_spinner = new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1));
        JSpinner schein_50euro_spinner = new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1));
        JSpinner schein_100euro_spinner = new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1));
        JSpinner schein_200euro_spinner = new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1));

        JFormattedTextField schein_5euro_field = new JFormattedTextField();
        JFormattedTextField schein_10euro_field = new JFormattedTextField();
        JFormattedTextField schein_20euro_field = new JFormattedTextField();
        JFormattedTextField schein_50euro_field = new JFormattedTextField();
        JFormattedTextField schein_100euro_field = new JFormattedTextField();
        JFormattedTextField schein_200euro_field = new JFormattedTextField();
        ((AbstractDocument) schein_5euro_field.getDocument()).setDocumentFilter(bc.geldFilter);
        ((AbstractDocument) schein_10euro_field.getDocument()).setDocumentFilter(bc.geldFilter);
        ((AbstractDocument) schein_20euro_field.getDocument()).setDocumentFilter(bc.geldFilter);
        ((AbstractDocument) schein_50euro_field.getDocument()).setDocumentFilter(bc.geldFilter);
        ((AbstractDocument) schein_100euro_field.getDocument()).setDocumentFilter(bc.geldFilter);
        ((AbstractDocument) schein_200euro_field.getDocument()).setDocumentFilter(bc.geldFilter);

        c2.gridy = 2;
        c2.gridx = 0;
        scheinPanel.add(new JLabel("Anzahl:"), c2);
        c2.gridy = 4;
        c2.gridx = 0;
        scheinPanel.add(new JLabel("Betrag:"), c2);

        c2.gridy = 0;
        c2.gridx = 1;
        scheinPanel.add(schein_5euro_label, c2);
        c2.gridy = 0;
        c2.gridx = 3;
        scheinPanel.add(schein_10euro_label, c2);
        c2.gridy = 0;
        c2.gridx = 5;
        scheinPanel.add(schein_20euro_label, c2);
        c2.gridy = 0;
        c2.gridx = 7;
        scheinPanel.add(schein_50euro_label, c2);
        c2.gridy = 0;
        c2.gridx = 9;
        scheinPanel.add(schein_100euro_label, c2);
        c2.gridy = 0;
        c2.gridx = 11;
        scheinPanel.add(schein_200euro_label, c2);

        c2.gridy = 1;
        c2.gridx = 1;
        scheinPanel.add(new JLabel(schein_5euro_icon), c2);
        c2.gridy = 1;
        c2.gridx = 3;
        scheinPanel.add(new JLabel(schein_10euro_icon), c2);
        c2.gridy = 1;
        c2.gridx = 5;
        scheinPanel.add(new JLabel(schein_20euro_icon), c2);
        c2.gridy = 1;
        c2.gridx = 7;
        scheinPanel.add(new JLabel(schein_50euro_icon), c2);
        c2.gridy = 1;
        c2.gridx = 9;
        scheinPanel.add(new JLabel(schein_100euro_icon), c2);
        c2.gridy = 1;
        c2.gridx = 11;
        scheinPanel.add(new JLabel(schein_200euro_icon), c2);

        c2.gridy = 2;
        c2.gridx = 1;
        scheinPanel.add(schein_5euro_spinner, c2);
        c2.gridy = 2;
        c2.gridx = 3;
        scheinPanel.add(schein_10euro_spinner, c2);
        c2.gridy = 2;
        c2.gridx = 5;
        scheinPanel.add(schein_20euro_spinner, c2);
        c2.gridy = 2;
        c2.gridx = 7;
        scheinPanel.add(schein_50euro_spinner, c2);
        c2.gridy = 2;
        c2.gridx = 9;
        scheinPanel.add(schein_100euro_spinner, c2);
        c2.gridy = 2;
        c2.gridx = 11;
        scheinPanel.add(schein_200euro_spinner, c2);

        c2.gridy = 3;
        c2.gridx = 1;
        scheinPanel.add(new JLabel("oder"), c2);
        c2.gridy = 3;
        c2.gridx = 3;
        scheinPanel.add(new JLabel("oder"), c2);
        c2.gridy = 3;
        c2.gridx = 5;
        scheinPanel.add(new JLabel("oder"), c2);
        c2.gridy = 3;
        c2.gridx = 7;
        scheinPanel.add(new JLabel("oder"), c2);
        c2.gridy = 3;
        c2.gridx = 9;
        scheinPanel.add(new JLabel("oder"), c2);
        c2.gridy = 3;
        c2.gridx = 11;
        scheinPanel.add(new JLabel("oder"), c2);

        c2.gridy = 4;
        c2.gridx = 1;
        scheinPanel.add(schein_5euro_field, c2);
        c2.gridy = 4;
        c2.gridx = 3;
        scheinPanel.add(schein_10euro_field, c2);
        c2.gridy = 4;
        c2.gridx = 5;
        scheinPanel.add(schein_20euro_field, c2);
        c2.gridy = 4;
        c2.gridx = 7;
        scheinPanel.add(schein_50euro_field, c2);
        c2.gridy = 4;
        c2.gridx = 9;
        scheinPanel.add(schein_100euro_field, c2);
        c2.gridy = 4;
        c2.gridx = 11;
        scheinPanel.add(schein_200euro_field, c2);

        c2.gridy = 4;
        c2.gridx = 2;
        scheinPanel.add(new JLabel(bc.currencySymbol), c2);
        c2.gridy = 4;
        c2.gridx = 4;
        scheinPanel.add(new JLabel(bc.currencySymbol), c2);
        c2.gridy = 4;
        c2.gridx = 6;
        scheinPanel.add(new JLabel(bc.currencySymbol), c2);
        c2.gridy = 4;
        c2.gridx = 8;
        scheinPanel.add(new JLabel(bc.currencySymbol), c2);
        c2.gridy = 4;
        c2.gridx = 10;
        scheinPanel.add(new JLabel(bc.currencySymbol), c2);
        c2.gridy = 4;
        c2.gridx = 12;
        scheinPanel.add(new JLabel(bc.currencySymbol), c2);

        middlePanel.add(muenzPanel);
        middlePanel.add(scheinPanel);

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
        footerPanel.add(okButton);
        cancelButton = new JButton("Abbrechen" );
        cancelButton.setMnemonic(KeyEvent.VK_A);
        cancelButton.addActionListener(this);
        footerPanel.add(cancelButton);
        allPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    /**
     * Needed for ChangeListener.
     */
    public void stateChanged(ChangeEvent e) {
//	if (e.getSource() == dateSpinner){
//
//	}
    }

    protected int submit() {
//        java.sql.Timestamp selectedTimestamp = new java.sql.Timestamp( dateChooser.getDate().getTime() +
//                timeModel.getDate().getTime() );
//        this.abrechnungen.setSelectedZeitpunkt( selectedTimestamp.toString() );
        return 0;
    }

    /**
     * * Each non abstract class that implements the ActionListener
     * must have this method.
     *
     * @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            submit();
            this.window.dispose();
            return;
        }
        if (e.getSource() == cancelButton) {
            // communicate that insert abrechnung was canceled:
//            this.abrechnungen.setSelectedZeitpunkt( null );
            this.window.dispose();
            return;
        }
        super.actionPerformed(e);
    }

    // will data be lost on close?
    protected boolean willDataBeLost() {
        return false;
    }
}
