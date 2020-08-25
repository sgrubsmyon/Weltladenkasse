package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:

import org.weltladen_bonn.pos.BaseClass;
import org.weltladen_bonn.pos.BaseClass.BigLabel;
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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Vector;
import org.mariadb.jdbc.MariaDbPoolDataSource;

public class ZaehlprotokollDialog extends DialogWindow
        implements ChangeListener, DocumentListener {
    // Attribute:
    private AbrechnungenTag abrechnungen;

    private Vector<JSpinner> muenz_spinners;
    private Vector<JSpinner> schein_spinners;
    private Vector<JFormattedTextField> muenz_fields;
    private Vector<JFormattedTextField> schein_fields;

    private JFormattedTextField summeField;
    private JFormattedTextField kassenstandField;
    private JFormattedTextField differenzField;

    private JTextArea kommentarErklaerText;
    private JTextArea kommentarArea;
    Boolean kommentarAreaIsVirgin = true;

    private JButton okButton;
    private JButton cancelButton;

    // Methoden:
    public ZaehlprotokollDialog(MariaDbPoolDataSource pool, MainWindowGrundlage mw,
                                AbrechnungenTag at, JDialog dia) {
        super(pool, mw, dia);
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
        int top = 5, left = 5, bottom = 5, right = 5;
        headerPanel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));

        JTextArea erklaerText = new JTextArea(2, 30);
        erklaerText.append("Der Kassenbestand in allen Münz- und Scheinsorten " +
                "ist vollständig zu protokollieren!\n" +
                "Bitte zählen, wie viele Münzen und Scheine von jeder Sorte " +
                "in der Kasse sind, und hier eintragen:" );
        erklaerText = makeLabelStyle(erklaerText);
//        erklaerText.setFont(BaseClass.mediumFont);
//        erklaerText.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
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
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.ipady = 5;
        c.insets = new Insets(3, 0, 3, 3);

        Vector<BigLabel> muenz_labels = new Vector<>();
        for (String name : bc.muenz_namen) {
            muenz_labels.add(new BigLabel(name));
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
        for (String name : bc.muenz_namen) {
            muenz_spinners.add(new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1)));
            JSpinner spinner = muenz_spinners.lastElement();
            spinner.addChangeListener(this);
            spinner.setFont(BaseClass.mediumFont);
            JSpinner.NumberEditor anzahlEditor = new JSpinner.NumberEditor(spinner, "###");
            spinner.setEditor(anzahlEditor);
            JFormattedTextField anzahlField = anzahlEditor.getTextField();
            anzahlField.setColumns(3);
//            preventSpinnerOverflow(anzahlField);
            ((NumberFormatter) anzahlField.getFormatter()).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
            anzahlField.addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                      public void run() {
                        anzahlField.selectAll();
                      }
                    });
                }
                public void focusLost(FocusEvent e) {
                  // do nothing
                }
            });
        }


        muenz_fields = new Vector<>();
        for (String name : bc.muenz_namen) {
            muenz_fields.add(new JFormattedTextField("0,00"));
            JFormattedTextField field = muenz_fields.lastElement();
            ((AbstractDocument) field.getDocument()).setDocumentFilter(bc.geldFilter);
            field.setEditable(false);
            field.setFocusable(false);
            field.setFont(BaseClass.mediumFont);
            field.setHorizontalAlignment(JFormattedTextField.RIGHT);
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
        muenzPanel.add(new BigLabel("Anzahl:"), c);
        c.gridy = 4;
        c.gridx = 0;
        muenzPanel.add(new BigLabel("Betrag:"), c);

        int index = 1;
        for (BigLabel label : muenz_labels) {
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
//        for (String name : bc.muenz_namen) {
//            c.gridy = 3;
//            c.gridx = index;
//            muenzPanel.add(new BigLabel("oder"), c);
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
        for (String name : bc.muenz_namen) {
            c.gridy = 4;
            c.gridx = index;
            muenzPanel.add(new BigLabel(bc.currencySymbol), c);
            index += 2;
        }





        JPanel scheinPanel = new JPanel(new GridBagLayout());
        scheinPanel.setBorder(BorderFactory.createTitledBorder("Scheine" ));
        GridBagConstraints c2 = new GridBagConstraints();
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.anchor = GridBagConstraints.CENTER;
        c2.ipady = 5;
        c2.insets = new Insets(3, 0, 3, 3);

        Vector<BigLabel> schein_labels = new Vector<>();
        for (String name : bc.schein_namen) {
            schein_labels.add(new BigLabel(name));
        }

        Vector<ImageIcon> schein_icons = new Vector<>();
        schein_icons.add(new ImageIcon(getClass().getResource("/resources/icons/banknotes/5euro_neu_vorne_klein.jpg" ), "5 Euro"));
        schein_icons.add(new ImageIcon(getClass().getResource("/resources/icons/banknotes/10euro_neu_vorne_klein.jpg" ), "10 Euro"));
        schein_icons.add(new ImageIcon(getClass().getResource("/resources/icons/banknotes/20euro_neu_vorne_klein.jpg" ), "20 Euro"));
        schein_icons.add(new ImageIcon(getClass().getResource("/resources/icons/banknotes/50euro_neu_vorne_klein.jpg" ), "50 Euro"));
        schein_icons.add(new ImageIcon(getClass().getResource("/resources/icons/banknotes/100euro_klein.gif" ), "100 Euro"));
        schein_icons.add(new ImageIcon(getClass().getResource("/resources/icons/banknotes/200euro_klein.gif" ), "200 Euro"));

        schein_spinners = new Vector<>();
        for (String name : bc.schein_namen) {
            schein_spinners.add(new JSpinner(new SpinnerNumberModel(0, 0, bc.smallintMax, 1)));
            JSpinner spinner = schein_spinners.lastElement();
            spinner.addChangeListener(this);
            spinner.setFont(BaseClass.mediumFont);
            JSpinner.NumberEditor anzahlEditor = new JSpinner.NumberEditor(spinner, "###");
            spinner.setEditor(anzahlEditor);
            JFormattedTextField anzahlField = anzahlEditor.getTextField();
//            preventSpinnerOverflow(anzahlField);
            ((NumberFormatter) anzahlField.getFormatter()).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
            anzahlField.addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                      public void run() {
                        anzahlField.selectAll();
                      }
                    });
                }
                public void focusLost(FocusEvent e) {
                  // do nothing
                }
            });
        }

        schein_fields = new Vector<>();
        for (String name : bc.schein_namen) {
            schein_fields.add(new JFormattedTextField(bc.priceFormatter("0")));
            JFormattedTextField field = schein_fields.lastElement();
            ((AbstractDocument) field.getDocument()).setDocumentFilter(bc.geldFilter);
            field.setEditable(false);
            field.setFocusable(false);
            field.setFont(BaseClass.mediumFont);
            field.setHorizontalAlignment(JFormattedTextField.RIGHT);
        }

        c2.gridy = 2;
        c2.gridx = 0;
        scheinPanel.add(new BigLabel("Anzahl:"), c2);
        c2.gridy = 4;
        c2.gridx = 0;
        scheinPanel.add(new BigLabel("Betrag:"), c2);

        index = 1;
        for (BigLabel label : schein_labels) {
            c2.gridy = 0;
            c2.gridx = index;
            c2.anchor = GridBagConstraints.CENTER;
            scheinPanel.add(label, c2);
            index += 2;
        }

        index = 1;
        for (ImageIcon icon : schein_icons) {
            c2.gridy = 1;
            c2.gridx = index;
            scheinPanel.add(new JLabel(icon), c2);
            index += 2;
        }

        index = 1;
        for (JSpinner spinner : schein_spinners) {
            c2.gridy = 2;
            c2.gridx = index;
            scheinPanel.add(spinner, c2);
            index += 2;
        }

//        index = 1;
//        for (String name : bc.schein_namen) {
//            c2.gridy = 3;
//            c2.gridx = index;
//            scheinPanel.add(new BigLabel("oder"), c2);
//            index += 2;
//        }

        index = 1;
        for (JFormattedTextField field : schein_fields) {
            c2.gridy = 4;
            c2.gridx = index;
            scheinPanel.add(field, c2);
            index += 2;
        }

        index = 2;
        for (String name : bc.schein_namen) {
            c2.gridy = 4;
            c2.gridx = index;
            scheinPanel.add(new BigLabel(bc.currencySymbol), c2);
            index += 2;
        }



        JPanel summePanel = new JPanel(new GridBagLayout());
        summePanel.setBorder(BorderFactory.createTitledBorder("Summe" ));
        GridBagConstraints c3 = new GridBagConstraints();
        c3.anchor = GridBagConstraints.CENTER;
        c3.fill = GridBagConstraints.HORIZONTAL;
        c3.ipady = 5;
        c3.insets = new Insets(3, 0, 3, 3);

        summeField = new JFormattedTextField(bc.priceFormatter("0"));
        summeField.setColumns(7);
        summeField.setFont(BaseClass.mediumFont);
        summeField.setHorizontalAlignment(JFormattedTextField.RIGHT);
        summeField.setEditable(false);
        summeField.setFocusable(false);

        c3.gridy = 0;
        c3.gridx = 0;
        summePanel.add(new BigLabel("Gezählter Betrag:"), c3);
        c3.gridy = 0;
        c3.gridx = 1;
        summePanel.add(summeField, c3);
        c3.gridy = 0;
        c3.gridx = 2;
        summePanel.add(new BigLabel(bc.currencySymbol), c3);

        kassenstandField = new JFormattedTextField(bc.priceFormatter(mainWindow.retrieveKassenstand()));
        kassenstandField.setColumns(7);
        kassenstandField.setFont(BaseClass.mediumFont);
        kassenstandField.setForeground(Color.BLUE);
        kassenstandField.setHorizontalAlignment(JFormattedTextField.RIGHT);
        kassenstandField.setEditable(false);
        kassenstandField.setFocusable(false);

        c3.gridy = 0;
        c3.gridx = 3;
        summePanel.add(Box.createRigidArea(new Dimension(10, 0)), c3); // add empty space

        c3.gridy = 0;
        c3.gridx = 4;
        summePanel.add(new BigLabel("Soll-Kassenstand:"), c3);
        c3.gridy = 0;
        c3.gridx = 5;
        summePanel.add(kassenstandField, c3);
        c3.gridy = 0;
        c3.gridx = 6;
        summePanel.add(new BigLabel(bc.currencySymbol), c3);

        BigDecimal differenz = new BigDecimal(bc.priceFormatterIntern(summeField.getText())).subtract(mainWindow.retrieveKassenstand());
        differenzField = new JFormattedTextField(bc.priceFormatter(differenz));
        differenzField.setColumns(7);
        differenzField.setFont(BaseClass.mediumFont);
        if (differenz.signum() == 0) {
            differenzField.setForeground(Color.GREEN.darker().darker());
        } else {
            differenzField.setForeground(Color.RED);
        }
        differenzField.setHorizontalAlignment(JFormattedTextField.RIGHT);
        differenzField.setEditable(false);
        differenzField.setFocusable(false);

        c3.gridy = 1;
        c3.gridx = 0;
        summePanel.add(new BigLabel("Differenz:"), c3);
        c3.gridy = 1;
        c3.gridx = 1;
        summePanel.add(differenzField, c3);
        c3.gridy = 1;
        c3.gridx = 2;
        summePanel.add(new BigLabel(bc.currencySymbol), c3);



        JPanel kommentarPanel = new JPanel(new GridBagLayout());
        kommentarPanel.setBorder(BorderFactory.createTitledBorder("Kommentar" ));
        GridBagConstraints c4 = new GridBagConstraints();
        c4.anchor = GridBagConstraints.CENTER;
        c4.fill = GridBagConstraints.HORIZONTAL;
        c4.ipady = 5;
//        c4.insets = new Insets(3, 10, 3, 10);
        c4.insets = new Insets(3, 0, 3, 3);


        kommentarErklaerText = new JTextArea(5, 30);
        kommentarErklaerText.append("Bitte rechts eingeben:\n"+
                "Was ist der Grund für die Differenz?\n"+
                "(Wenn die Differenz 0,00 "+bc.currencySymbol+" ist, dann einfach \"Kasse stimmt\" o.ä. eintragen.)");
        kommentarErklaerText = makeLabelStyle(kommentarErklaerText);
//        kommentarErklaerText.setFont(BaseClass.mediumFont);
        int top = 5, left = 5, bottom = 5, right = 5;
        kommentarErklaerText.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));

        kommentarArea = new JTextArea(5, 25);
        kommentarArea.setText("Kommentar eingeben...");
        kommentarArea.setLineWrap(true); // nice line wrapping
        kommentarArea.setWrapStyleWord(true); // nice line wrapping
        kommentarArea.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                if (kommentarAreaIsVirgin) {
                    kommentarArea.setText("");
                    kommentarAreaIsVirgin = false;
                }
            }

            public void focusLost(FocusEvent e) {
                // do nothing
            }
        });
//        kommentarArea.setFont(BaseClass.mediumFont);
        kommentarArea.getDocument().addDocumentListener(this);
        JScrollPane kommentarScrollPane = new JScrollPane(kommentarArea);

        c4.gridy = 0;
        c4.gridx = 0;
        kommentarPanel.add(kommentarErklaerText, c4);
        c4.gridy = 0;
        c4.gridx = 1;
        kommentarPanel.add(kommentarScrollPane, c4);



        middlePanel.add(muenzPanel);
        middlePanel.add(scheinPanel);
        middlePanel.add(summePanel);
        middlePanel.add(kommentarPanel);

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

    private void refreshSum() {
        BigDecimal sum = new BigDecimal("0");
        for (JFormattedTextField field : muenz_fields) {
            sum = sum.add(new BigDecimal(bc.priceFormatterIntern(field.getText())));
        }
        for (JFormattedTextField field : schein_fields) {
            sum = sum.add(new BigDecimal(bc.priceFormatterIntern(field.getText())));
        }
        summeField.setText(bc.priceFormatter(sum));

        BigDecimal differenz = sum.subtract(new BigDecimal(bc.priceFormatterIntern(kassenstandField.getText())));
        differenzField.setText(bc.priceFormatter(differenz));
        if (differenz.signum() == 0) {
            differenzField.setForeground(Color.GREEN.darker().darker());
        } else {
            differenzField.setForeground(Color.RED);
        }
    }

    /**
     * Needed for ChangeListener.
     */
    public void stateChanged(ChangeEvent e) {
        int index = 0;
        for (JSpinner spinner : muenz_spinners) {
            if (e.getSource() == spinner) {
                BigDecimal anzahl = new BigDecimal((Integer) spinner.getValue());
                BigDecimal wert = bc.muenz_werte.elementAt(index);
                muenz_fields.elementAt(index).setText(bc.priceFormatter(anzahl.multiply(wert)));
                refreshSum();
                return;
            }
            index++;
        }

        index = 0;
        for (JSpinner spinner : schein_spinners) {
            if (e.getSource() == spinner) {
                BigDecimal anzahl = new BigDecimal((Integer) spinner.getValue());
                BigDecimal wert = bc.schein_werte.elementAt(index);
                schein_fields.elementAt(index).setText(bc.priceFormatter(anzahl.multiply(wert)));
                refreshSum();
                return;
            }
            index++;
        }
    }

    private LinkedHashMap<BigDecimal, Integer> getZaehlprotokoll() {
        LinkedHashMap<BigDecimal, Integer> zaehlprotokoll = new LinkedHashMap<>();
        int index = 0;
        for (BigDecimal wert : bc.muenz_werte) {
            Integer anzahl = (Integer) muenz_spinners.get(index).getValue();
            zaehlprotokoll.put(wert, anzahl);
            index++;
        }
        index = 0;
        for (BigDecimal wert : bc.schein_werte) {
            Integer anzahl = (Integer) schein_spinners.get(index).getValue();
            zaehlprotokoll.put(wert, anzahl);
            index++;
        }
        return zaehlprotokoll;
    }

    void setZaehlprotokoll(LinkedHashMap<BigDecimal, Integer> zaehlprotokoll) {
        int index = 0;
        for (BigDecimal wert : bc.muenz_werte) {
            Integer anzahl = zaehlprotokoll.get(wert);
            muenz_spinners.get(index).setValue(anzahl);
            index++;
        }
        index = 0;
        for (BigDecimal wert : bc.schein_werte) {
            Integer anzahl = zaehlprotokoll.get(wert);
            schein_spinners.get(index).setValue(anzahl);
            index++;
        }
    }

    void setKassenstand(BigDecimal kassenstand) {
        kassenstandField.setText(bc.priceFormatter(kassenstand));
        refreshSum();
    }

    void setKommentarErklaerText(String text) {
        kommentarErklaerText.setText(text);
    }

    /**
     * * Each non abstract class that implements the ActionListener
     * must have this method.
     *
     * @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Bitte genau prüfen, ob die Eingaben stimmen!\n\n"+
                            "Stimmt alles?",
                    "Alles korrekt?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                LinkedHashMap<BigDecimal, Integer> zaehlprotokoll = getZaehlprotokoll();
                // communicate that zehlprotokoll was successful:
                this.abrechnungen.setZaehlprotokoll(zaehlprotokoll);
                this.abrechnungen.setZaehlprotokollKommentar(kommentarArea.getText());
                this.window.dispose();
            }
            return;
        }
        if (e.getSource() == cancelButton) {
            // communicate that zaehlprotokoll was canceled:
            this.abrechnungen.setZaehlprotokoll(null);
            this.abrechnungen.setZaehlprotokollKommentar(null);
            this.window.dispose();
            return;
        }
        super.actionPerformed(e);
    }

    // will data be lost on close?
    protected boolean willDataBeLost() {
        return false;
    }

    /**
     * * Each non abstract class that implements the DocumentListener must have
     * these methods.
     *
     * @param documentEvent
     *            the document event.
     **/
    @Override
    public void insertUpdate(DocumentEvent documentEvent) {
        if (kommentarArea.getDocument().getLength() >= 5) {
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
