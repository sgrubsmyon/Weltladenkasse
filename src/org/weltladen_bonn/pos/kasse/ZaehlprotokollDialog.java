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
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

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

    private Vector<JSpinner> muenz_spinners;
    private Vector<JSpinner> schein_spinners;
    private Vector<JFormattedTextField> muenz_fields;
    private Vector<JFormattedTextField> schein_fields;
    private Vector<BigDecimal> muenz_werte;
    private Vector<BigDecimal> schein_werte;

    private JFormattedTextField summeField;

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

        JTextArea erklaerText = new JTextArea(3, 30);
        erklaerText.append("Der Kassenbestand in allen Münz- und Scheinsorten " +
                "ist vollständig zu protokollieren!\n\n" +
                "Bitte zähle, wie viele Münzen und Scheine von jeder Sorte " +
                "in der Kasse sind, und trage es hier ein:" );
        erklaerText = makeLabelStyle(erklaerText);
        erklaerText.setFont(bc.mediumFont);
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

        Vector<String> muenz_namen = new Vector<>();
        muenz_namen.add("1 Cent");
        muenz_namen.add("2 Cent");
        muenz_namen.add("5 Cent");
        muenz_namen.add("10 Cent");
        muenz_namen.add("20 Cent");
        muenz_namen.add("50 Cent");
        muenz_namen.add("1 Euro");
        muenz_namen.add("2 Euro");

        muenz_werte = new Vector<>();
        muenz_werte.add(new BigDecimal("0.01"));
        muenz_werte.add(new BigDecimal("0.02"));
        muenz_werte.add(new BigDecimal("0.05"));
        muenz_werte.add(new BigDecimal("0.10"));
        muenz_werte.add(new BigDecimal("0.20"));
        muenz_werte.add(new BigDecimal("0.50"));
        muenz_werte.add(new BigDecimal("1.00"));
        muenz_werte.add(new BigDecimal("2.00"));

        Vector<JLabel> muenz_labels = new Vector<>();
        for (String name : muenz_namen) {
            muenz_labels.add(new JLabel(name));
        }

        Vector<ImageIcon> muenz_icons = new Vector<>();
        muenz_icons.add(new ImageIcon(getClass().getResource("/resources/icons/coins/1cent_klein.gif" ), "1 Cent"));
        muenz_icons.add(new ImageIcon(getClass().getResource("/resources/icons/coins/2cent_klein.gif" ), "2 Cent"));
        muenz_icons.add(new ImageIcon(getClass().getResource("/resources/icons/coins/5cent_klein.gif" ), "5 Cent"));
        muenz_icons.add(new ImageIcon(getClass().getResource("/resources/icons/coins/10cent_klein.gif" ), "10 Cent"));
        muenz_icons.add(new ImageIcon(getClass().getResource("/resources/icons/coins/20cent_klein.gif" ), "20 Cent"));
        muenz_icons.add(new ImageIcon(getClass().getResource("/resources/icons/coins/50cent_klein.gif" ), "50 Cent"));
        muenz_icons.add(new ImageIcon(getClass().getResource("/resources/icons/coins/1euro_klein.gif" ), "1 Euro"));
        muenz_icons.add(new ImageIcon(getClass().getResource("/resources/icons/coins/2euro_klein.gif" ), "2 Euro"));

        muenz_spinners = new Vector<>();
        for (String name : muenz_namen) {
            muenz_spinners.add(new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1)));
            muenz_spinners.lastElement().addChangeListener(this);
            muenz_spinners.lastElement().setFont(bc.mediumFont);
            JSpinner.NumberEditor anzahlEditor = new JSpinner.NumberEditor(muenz_spinners.lastElement(), "###");
            muenz_spinners.lastElement().setEditor(anzahlEditor);
            JFormattedTextField anzahlField = anzahlEditor.getTextField();
//            preventSpinnerOverflow(anzahlField);
            ((NumberFormatter) anzahlField.getFormatter()).setAllowsInvalid(false); // accept
                                                                                    // only allowed values (i.e. numbers)
        }


        muenz_fields = new Vector<>();
        for (String name : muenz_namen) {
            muenz_fields.add(new JFormattedTextField("0,00"));
            ((AbstractDocument) muenz_fields.lastElement().getDocument()).setDocumentFilter(bc.geldFilter);
            muenz_fields.lastElement().setEnabled(false);
            muenz_fields.lastElement().setFont(bc.mediumFont);
            muenz_fields.lastElement().setHorizontalAlignment(JTextField.RIGHT);
        }
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

        c.gridy = 2;
        c.gridx = 0;
        muenzPanel.add(new JLabel("Anzahl:"), c);
        c.gridy = 4;
        c.gridx = 0;
        muenzPanel.add(new JLabel("Betrag:"), c);

        int index = 1;
        for (JLabel label : muenz_labels) {
            c.gridy = 0;
            c.gridx = index;
            c.anchor = GridBagConstraints.CENTER;
            muenzPanel.add(label, c);
            index += 2;
        }

        index = 1;
        for (ImageIcon icon : muenz_icons) {
            c.gridy = 1;
            c.gridx = index;
            muenzPanel.add(new JLabel(icon), c);
            index += 2;
        }

        index = 1;
        for (JSpinner spinner : muenz_spinners) {
            c.gridy = 2;
            c.gridx = index;
            muenzPanel.add(spinner, c);
            index += 2;
        }

//        index = 1;
//        for (String name : muenz_namen) {
//            c.gridy = 3;
//            c.gridx = index;
//            muenzPanel.add(new JLabel("oder"), c);
//            index += 2;
//        }

        index = 1;
        for (JFormattedTextField field : muenz_fields) {
            c.gridy = 4;
            c.gridx = index;
            muenzPanel.add(field, c);
            index += 2;
        }

        index = 2;
        for (String name : muenz_namen) {
            c.gridy = 4;
            c.gridx = index;
            muenzPanel.add(new JLabel(bc.currencySymbol), c);
            index += 2;
        }





        JPanel scheinPanel = new JPanel(new GridBagLayout());
        scheinPanel.setBorder(BorderFactory.createTitledBorder("Scheine" ));
        GridBagConstraints c2 = new GridBagConstraints();
        c2.anchor = GridBagConstraints.CENTER;
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.ipady = 10;
        c2.insets = new Insets(3, 10, 3, 10);

        Vector<String> schein_namen = new Vector<>();
        schein_namen.add("5 Euro");
        schein_namen.add("10 Euro");
        schein_namen.add("20 Euro");
        schein_namen.add("50 Euro");
        schein_namen.add("100 Euro");
        schein_namen.add("200 Euro");

        schein_werte = new Vector<>();
        schein_werte.add(new BigDecimal("5"));
        schein_werte.add(new BigDecimal("10"));
        schein_werte.add(new BigDecimal("20"));
        schein_werte.add(new BigDecimal("50"));
        schein_werte.add(new BigDecimal("100"));
        schein_werte.add(new BigDecimal("200"));

        Vector<JLabel> schein_labels = new Vector<>();
        for (String name : schein_namen) {
            schein_labels.add(new JLabel(name));
        }

        Vector<ImageIcon> schein_icons = new Vector<>();
        schein_icons.add(new ImageIcon(getClass().getResource("/resources/icons/banknotes/5euro_neu_vorne_klein.jpg" ), "5 Euro"));
        schein_icons.add(new ImageIcon(getClass().getResource("/resources/icons/banknotes/10euro_neu_vorne_klein.jpg" ), "10 Euro"));
        schein_icons.add(new ImageIcon(getClass().getResource("/resources/icons/banknotes/20euro_neu_vorne_klein.jpg" ), "20 Euro"));
        schein_icons.add(new ImageIcon(getClass().getResource("/resources/icons/banknotes/50euro_neu_vorne_klein.jpg" ), "50 Euro"));
        schein_icons.add(new ImageIcon(getClass().getResource("/resources/icons/banknotes/100euro_klein.gif" ), "100 Euro"));
        schein_icons.add(new ImageIcon(getClass().getResource("/resources/icons/banknotes/200euro_klein.gif" ), "200 Euro"));

        schein_spinners = new Vector<>();
        for (String name : schein_namen) {
            schein_spinners.add(new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1)));
            schein_spinners.lastElement().addChangeListener(this);
            schein_spinners.lastElement().setFont(bc.mediumFont);
            JSpinner.NumberEditor anzahlEditor = new JSpinner.NumberEditor(schein_spinners.lastElement(), "###");
            schein_spinners.lastElement().setEditor(anzahlEditor);
            JFormattedTextField anzahlField = anzahlEditor.getTextField();
//            preventSpinnerOverflow(anzahlField);
            ((NumberFormatter) anzahlField.getFormatter()).setAllowsInvalid(false); // accept
                                                                                    // only allowed values (i.e. numbers)
        }

        schein_fields = new Vector<>();
        for (String name : schein_namen) {
            schein_fields.add(new JFormattedTextField(bc.priceFormatter("0")));
            ((AbstractDocument) schein_fields.lastElement().getDocument()).setDocumentFilter(bc.geldFilter);
            schein_fields.lastElement().setEnabled(false);
            schein_fields.lastElement().setFont(bc.mediumFont);
            schein_fields.lastElement().setHorizontalAlignment(JTextField.RIGHT);
        }

        c.gridy = 2;
        c.gridx = 0;
        scheinPanel.add(new JLabel("Anzahl:"), c);
        c.gridy = 4;
        c.gridx = 0;
        scheinPanel.add(new JLabel("Betrag:"), c);

        index = 1;
        for (JLabel label : schein_labels) {
            c.gridy = 0;
            c.gridx = index;
            c.anchor = GridBagConstraints.CENTER;
            scheinPanel.add(label, c);
            index += 2;
        }

        index = 1;
        for (ImageIcon icon : schein_icons) {
            c.gridy = 1;
            c.gridx = index;
            scheinPanel.add(new JLabel(icon), c);
            index += 2;
        }

        index = 1;
        for (JSpinner spinner : schein_spinners) {
            c.gridy = 2;
            c.gridx = index;
            scheinPanel.add(spinner, c);
            index += 2;
        }

//        index = 1;
//        for (String name : schein_namen) {
//            c.gridy = 3;
//            c.gridx = index;
//            scheinPanel.add(new JLabel("oder"), c);
//            index += 2;
//        }

        index = 1;
        for (JFormattedTextField field : schein_fields) {
            c.gridy = 4;
            c.gridx = index;
            scheinPanel.add(field, c);
            index += 2;
        }

        index = 2;
        for (String name : schein_namen) {
            c.gridy = 4;
            c.gridx = index;
            scheinPanel.add(new JLabel(bc.currencySymbol), c);
            index += 2;
        }



        JPanel summePanel = new JPanel(new BorderLayout());
        summePanel.setBorder(BorderFactory.createTitledBorder("Summe" ));

        summeField = new JFormattedTextField(bc.priceFormatter("0"));
        summeField.setColumns(7);
        summeField.setFont(bc.mediumFont);
        summeField.setHorizontalAlignment(JTextField.RIGHT);
        ((AbstractDocument) summeField.getDocument()).setDocumentFilter(bc.geldFilter);
        summeField.setEnabled(false);
        JPanel summeFieldPanel = new JPanel();
        summeFieldPanel.add(new JLabel("Gezählter Betrag:"));
        summeFieldPanel.add(summeField);
        summeFieldPanel.add(new JLabel(bc.currencySymbol));
        summePanel.add(summeFieldPanel, BorderLayout.WEST);

        JFormattedTextField kassenstandField;
        kassenstandField = new JFormattedTextField(bc.priceFormatter(mainWindow.retrieveKassenstand()));
        kassenstandField.setColumns(7);
        kassenstandField.setFont(bc.mediumFont);
        kassenstandField.setHorizontalAlignment(JTextField.RIGHT);
        ((AbstractDocument) kassenstandField.getDocument()).setDocumentFilter(bc.geldFilter);
        kassenstandField.setEnabled(false);
        JPanel kassenstandFieldPanel = new JPanel();
        kassenstandFieldPanel.add(new JLabel("Soll-Kassenstand:"));
        kassenstandFieldPanel.add(kassenstandField);
        kassenstandFieldPanel.add(new JLabel(bc.currencySymbol));
        summePanel.add(kassenstandFieldPanel, BorderLayout.EAST);



        middlePanel.add(muenzPanel);
        middlePanel.add(scheinPanel);
        middlePanel.add(summePanel);

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

    private void refreshSum() {
        BigDecimal sum = new BigDecimal("0");
        for (JFormattedTextField field : muenz_fields) {
            sum = sum.add(new BigDecimal(bc.priceFormatterIntern(field.getText())));
            System.out.println(sum);
        }
        for (JFormattedTextField field : schein_fields) {
            sum = sum.add(new BigDecimal(bc.priceFormatterIntern(field.getText())));
            System.out.println(sum);
        }
        summeField.setText(bc.priceFormatter(sum));
    }

    /**
     * Needed for ChangeListener.
     */
    public void stateChanged(ChangeEvent e) {
        int index = 0;
        for (JSpinner spinner : muenz_spinners) {
            if (e.getSource() == spinner) {
                BigDecimal anzahl = new BigDecimal((Integer) spinner.getValue());
                BigDecimal wert = muenz_werte.elementAt(index);
                muenz_fields.elementAt(index).setText(bc.priceFormatter(anzahl.multiply(wert)));
                System.out.println("Eins");
                refreshSum();
                System.out.println("Drei");
                return;
            }
            index++;
        }

        index = 0;
        for (JSpinner spinner : schein_spinners) {
            if (e.getSource() == spinner) {
                BigDecimal anzahl = new BigDecimal((Integer) spinner.getValue());
                BigDecimal wert = schein_werte.elementAt(index);
                schein_fields.elementAt(index).setText(bc.priceFormatter(anzahl.multiply(wert)));
                refreshSum();
                return;
            }
            index++;
        }
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
