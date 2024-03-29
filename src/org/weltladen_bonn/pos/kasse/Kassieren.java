package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.util.Date;
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.math.RoundingMode;

// MySQL Connector/J stuff:
import java.sql.*;
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import java.awt.*; // BorderLayout, FlowLayout, Dimension
import java.awt.event.*; // ActionEvent, ActionListener

import javax.swing.*; // JFrame, JPanel, JTable, JButton, ...
import javax.swing.table.*;
import javax.swing.text.*; // for DocumentFilter
import javax.swing.event.*;

// DateTime from date4j (http://www.date4j.net/javadoc/index.html)
import hirondelle.date4j.DateTime;

import org.weltladen_bonn.pos.*;
import org.weltladen_bonn.pos.BaseClass.BigLabel;
import org.weltladen_bonn.pos.BaseClass.BigButton;
import org.weltladen_bonn.pos.kasse.WeltladenTSE.TSETransaction;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main class of the cashier POS software
 */
public class Kassieren extends RechnungsGrundlage implements ArticleSelectUser, DocumentListener {
    private static final Logger logger = LogManager.getLogger(Kassieren.class);

    private static final long serialVersionUID = 1L;
    // Attribute:
    private final BigDecimal mitarbeiterRabatt = new BigDecimal("0.1");
    private final boolean allowMitarbeiterRabatt = true;
    private final BigDecimal ecSchwelle = new BigDecimal("10.00");

    private MainWindow mw;
    private Kundendisplay display;
    private WeltladenTSE tse;
    private TabbedPane tabbedPane;

    private int selectedArticleID;
    private int selectedStueck;
    
    private Color gutscheinColor = Color.MAGENTA.darker().darker();

    protected ArticleSelectPanelKassieren asPanel;
    private JButton sonstigesButton;
    private JButton gutscheinVerkaufenButton;
    private JButton sevenPercentButton;
    private JButton nineteenPercentButton;
    private JSpinner anzahlSpinner;
    protected JFormattedTextField anzahlField;
    protected JTextField preisField;
    private JTextField kundeGibtField;
    private JTextField bigPriceField;
    private JTextField rueckgeldField;
    private JTextField individuellRabattRelativField;
    private JTextField individuellRabattAbsolutField;
    private JTextField abweichenderPreisField;
    // Buttons
    private JButton hinzufuegenButton;
    private JButton leergutButton;
    private JButton rueckgabeButton;
    private JButton zwischensummeButton;
    private JButton barButton;
    private JButton ecButton;
    private JButton stornoButton;
    private JButton passendButton;
    private JButton gutscheinEinloesenButton;
    private JLabel zahlungsLabel;
    private JButton neuerKundeButton;
    private JButton individuellRabattRelativButton;
    private JButton individuellRabattAbsolutButton;
    private JButton mitarbeiterRabattButton;
    private JButton abweichenderPreisButton;
    private JButton anzahlungNeuButton;
    private JTextField anzahlungsBetragField;
    private JButton anzahlungNeuOKButton;
    private JButton anzahlungAufloesButton;

    private Vector<JButton> rabattButtons;

    // The panels
    private JPanel allPanel;
    private JPanel rabattPanel;
    private JPanel abweichenderPreisPanel;
    private JPanel anzahlungsPanel;
    private JPanel anzahlungNeuPanel;
    private JPanel articleListPanel;

    // The table holding the purchase articles.
    private ArticleSelectTable myTable;
    private JScrollPane scrollPane;
    private Vector<Vector<Object>> data;
    private Vector<JButton> removeButtons;

    // Methoden:

    /**
     * The constructor.
     */
    public Kassieren(MariaDbPoolDataSource pool, MainWindowGrundlage mw, TabbedPane tp) {
        super(pool, mw);
        if (mw instanceof MainWindow) {
            this.mw = (MainWindow) mw;
            display = this.mw.getDisplay();
            tse = this.mw.getTSE();
        } else {
            this.mw = null;
            display = null;
            tse = null;
        }
        this.tabbedPane = tp;

        columnLabels.add("Entfernen");

        setupKeyboardShortcuts();

        rabattButtons = new Vector<JButton>();
        rabattButtons.add(new BigButton("  5%"));
        rabattButtons.add(new BaseClass.BigButton("10%"));
        rabattButtons.add(new BaseClass.BigButton("15%"));
        rabattButtons.add(new BaseClass.BigButton("20%"));
        rabattButtons.add(new BaseClass.BigButton("25%"));

        showButtons();
        emptyTable();
        showAll();
        clearAll();
        asPanel.emptyArtikelBox();
    }

    private void setupKeyboardShortcuts() {
        // keyboard shortcuts:
        KeyStroke kunsthandwerkShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0);
        KeyStroke hinzufuegenShortcut = KeyStroke.getKeyStroke("ctrl H");
        KeyStroke leergutShortcut = KeyStroke.getKeyStroke("ctrl L");
        KeyStroke leergutShortcutNumPad = KeyStroke.getKeyStroke(KeyEvent.VK_DIVIDE, 0);
        KeyStroke rueckgabeShortcut = KeyStroke.getKeyStroke("ctrl R");
        KeyStroke rueckgabeShortcutNumPad = KeyStroke.getKeyStroke(KeyEvent.VK_MULTIPLY, 0);
        KeyStroke zwischensummeShortcut = KeyStroke.getKeyStroke("ctrl Z");
        KeyStroke zwischensummeShortcutNumPad = KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0);
        // KeyStroke barShortcut = KeyStroke.getKeyStroke("ctrl B");
        // KeyStroke ecShortcut = KeyStroke.getKeyStroke("ctrl E");
        // KeyStroke stornierenShortcut = KeyStroke.getKeyStroke("ctrl S");

        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(kunsthandwerkShortcut, "khw");
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(hinzufuegenShortcut, "hinzufuegen");
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(leergutShortcut, "leergut");
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(leergutShortcutNumPad, "leergut");
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(rueckgabeShortcut, "rueckgabe");
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(rueckgabeShortcutNumPad, "rueckgabe");
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(zwischensummeShortcut, "zws");
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(zwischensummeShortcutNumPad, "zws_num");
        this.getActionMap().put("khw", new KHWAction());
        this.getActionMap().put("hinzufuegen", new HinzufuegenAction());
        this.getActionMap().put("leergut", new LeergutAction());
        this.getActionMap().put("rueckgabe", new RueckgabeAction());
        this.getActionMap().put("zws", new ZWSAction());
        this.getActionMap().put("zws_num", new ZWSNumPadAction());

        // this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(barShortcut,
        // "bar");
        // this.getActionMap().put("bar", new BarAction());
        // this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ecShortcut,
        // "ec");
        // this.getActionMap().put("ec", new ECAction());
        // this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stornierenShortcut,
        // "stornieren");
        // this.getActionMap().put("stornieren", new StornoAction());
    }

    private class KHWAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            setSelectedArticle(sonstigesKHWArtikelID); // Sonstiges Kunsthandwerk
        }
    }

    private class ZWSAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            zwischensummeButton.doClick();
        }
    }

    private class ZWSNumPadAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            zwischensummeButton.doClick();
        }
    }

    private class HinzufuegenAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            hinzufuegenButton.doClick();
        }
    }

    private class LeergutAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            leergutButton.doClick();
        }
    }

    private class RueckgabeAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            rueckgabeButton.doClick();
        }
    }

    // private class BarAction extends AbstractAction {
    // public void actionPerformed(ActionEvent e) {
    // barButton.doClick();
    // }
    // }
    //
    // private class ECAction extends AbstractAction {
    // public void actionPerformed(ActionEvent e) {
    // ecButton.doClick();
    // }
    // }
    //
    // private class StornoAction extends AbstractAction {
    // public void actionPerformed(ActionEvent e) {
    // stornoButton.doClick();
    // }
    // }

    void preventSpinnerOverflow(JFormattedTextField spinnerField) {
        AbstractDocument doc = new PlainDocument() {
            @Override
            public void setDocumentFilter(DocumentFilter filter) {
                if (filter instanceof IntegerDocumentFilter) { // w/o this if, it's not working
                    // maybe the DocumentFilter is reset to a default filter for spinners
                    super.setDocumentFilter(filter);
                }
            }
        };
        doc.setDocumentFilter(
          new IntegerDocumentFilter((Integer) ((SpinnerNumberModel) anzahlSpinner.getModel()).getMinimum(),
            (Integer) ((SpinnerNumberModel) anzahlSpinner.getModel()).getMaximum(), "Anzahl", this)
        );
        spinnerField.setDocument(doc);
    }

    private class BigPriceField extends JTextField {
        public BigPriceField() {
            super();
            initialize();
        }

        public BigPriceField(String str) {
            super(str);
            initialize();
        }

        private void initialize() {
            this.setColumns(6);
            this.setHorizontalAlignment(JTextField.RIGHT);
            this.setFont(bc.bigFont);
        }
    }

    void showAll() {
        updateRabattButtons();

        allPanel = new JPanel(new BorderLayout());

        JPanel artikelFormularPanel = new JPanel();
        artikelFormularPanel.setLayout(new BoxLayout(artikelFormularPanel, BoxLayout.PAGE_AXIS));

        JPanel articleSelectPanel = new JPanel();
        articleSelectPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));

        asPanel = new ArticleSelectPanelKassieren(this.pool, mainWindow, this, tabbedPane);
        articleSelectPanel.add(asPanel);

        JPanel sonstigesPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c1 = new GridBagConstraints();
        c1.anchor = GridBagConstraints.CENTER;
        c1.fill = GridBagConstraints.BOTH;
        c1.ipady = 5;
        c1.insets = new Insets(1, 0, 1, 0);
        sonstigesPanel.setBorder(BorderFactory.createTitledBorder("Variabler Preis"));
        sonstigesButton = new BaseClass.BigButton("Sonstiges...");
        gutscheinVerkaufenButton = new JButton("Gutschein verk.");
        gutscheinVerkaufenButton.setBackground(gutscheinColor);
        gutscheinVerkaufenButton.setForeground(Color.WHITE);
        sevenPercentButton = new BaseClass.BigButton("7% MwSt.");
        nineteenPercentButton = new BaseClass.BigButton("19% MwSt.");
        sonstigesButton.addActionListener(this);
        gutscheinVerkaufenButton.addActionListener(this);
        sevenPercentButton.addActionListener(this);
        nineteenPercentButton.addActionListener(this);

        c1.gridx = 0; c1.gridy = 0;
        sonstigesPanel.add(sonstigesButton, c1);
        c1.gridx = 0; c1.gridy = 1;
        sonstigesPanel.add(gutscheinVerkaufenButton, c1);
        c1.gridx = 1; c1.gridy = 0;
        sonstigesPanel.add(sevenPercentButton, c1);
        c1.gridx = 1; c1.gridy = 1;
        sonstigesPanel.add(nineteenPercentButton, c1);

        articleSelectPanel.add(sonstigesPanel);
        artikelFormularPanel.add(articleSelectPanel);

        JPanel spinnerPanel = new JPanel();
        spinnerPanel.setLayout(new FlowLayout());
        BigLabel anzahlLabel = new BigLabel("Anzahl: ");
        spinnerPanel.add(anzahlLabel);
        SpinnerNumberModel anzahlModel = new SpinnerNumberModel(1, // initial value
                1, // min
                bc.smallintMax, // max (null == no max)
                1); // step
        anzahlSpinner = new JSpinner(anzahlModel);
        anzahlSpinner.setFont(BaseClass.mediumFont);
        JSpinner.NumberEditor anzahlEditor = new JSpinner.NumberEditor(anzahlSpinner, "###");
        anzahlSpinner.setEditor(anzahlEditor);
        anzahlField = anzahlEditor.getTextField();
        preventSpinnerOverflow(anzahlField);
        ((NumberFormatter) anzahlField.getFormatter()).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
        anzahlField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (preisField.isEditable())
                        preisField.requestFocus();
                    else {
                        if (hinzufuegenButton.isEnabled()) {
                            anzahlSpinner.setValue(Integer.parseInt(anzahlField.getText()));
                            hinzufuegenButton.doClick();
                        }
                    }
                }
            }
        });
        anzahlField.setColumns(4);
        removeDefaultKeyBindings(anzahlField);
        anzahlField.addKeyListener(removeNumPadAdapter);
        anzahlLabel.setLabelFor(anzahlSpinner);
        spinnerPanel.add(anzahlSpinner);

        BigLabel preisLabel = new BigLabel("Preis: ");
        spinnerPanel.add(preisLabel);
        preisField = new JTextField("");
        preisField.setFont(BaseClass.mediumFont);
        preisField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (hinzufuegenButton.isEnabled()) {
                        hinzufuegenButton.doClick();
                    }
                }
            }
        });
        preisField.getDocument().addDocumentListener(this);
        ((AbstractDocument) preisField.getDocument()).setDocumentFilter(bc.geldFilter);
        preisField.setEditable(false);
        preisField.setColumns(6);
        removeDefaultKeyBindings(preisField);
        preisField.addKeyListener(removeNumPadAdapter);
        preisField.setHorizontalAlignment(JTextField.RIGHT);
        spinnerPanel.add(preisField);
        spinnerPanel.add(new BigLabel(bc.currencySymbol));

        hinzufuegenButton = new BaseClass.BigButton("Hinzufügen");
        hinzufuegenButton.setBackground(Color.BLUE.darker());
        hinzufuegenButton.setForeground(Color.WHITE);
        hinzufuegenButton.setMnemonic(KeyEvent.VK_H);
        hinzufuegenButton.addActionListener(this);
        hinzufuegenButton.setEnabled(false);
        spinnerPanel.add(hinzufuegenButton);

        leergutButton = new BaseClass.BigButton("Leergut");
        leergutButton.setBackground(Color.GREEN.darker().darker());
        leergutButton.setForeground(Color.WHITE);
        leergutButton.setMnemonic(KeyEvent.VK_L);
        leergutButton.addActionListener(this);
        leergutButton.setEnabled(false);
        spinnerPanel.add(leergutButton);

        rueckgabeButton = new BaseClass.BigButton("Rückgabe");
        rueckgabeButton.setBackground(Color.ORANGE);
        rueckgabeButton.setMnemonic(KeyEvent.VK_R);
        rueckgabeButton.addActionListener(this);
        rueckgabeButton.setEnabled(false);
        spinnerPanel.add(rueckgabeButton);
        artikelFormularPanel.add(spinnerPanel);

        allPanel.add(artikelFormularPanel, BorderLayout.NORTH);

        showTable();

        JPanel bezahlPanel = new JPanel();
        bezahlPanel.setLayout(new BoxLayout(bezahlPanel, BoxLayout.PAGE_AXIS));

        JPanel zwischensummePanel = new JPanel();
        zwischensummePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JPanel zahlungsButtonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c2 = new GridBagConstraints();
        c2.anchor = GridBagConstraints.CENTER;
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.ipady = 10;
        c2.insets = new Insets(3, 0, 3, 0);
        barButton = new BaseClass.BigButton("Bar");
        barButton.setMnemonic(KeyEvent.VK_B);
        barButton.setEnabled(false);
        barButton.addActionListener(this);

        ecButton = new BaseClass.BigButton("EC");
        ecButton.setMnemonic(KeyEvent.VK_E);
        ecButton.setEnabled(false);
        ecButton.addActionListener(this);

        stornoButton = new BaseClass.BigButton("Storno");
        stornoButton.setMnemonic(KeyEvent.VK_S);
        if (data.size() == 0)
            stornoButton.setEnabled(false);
        stornoButton.addActionListener(this);

        c2.gridy = 0;
        zahlungsButtonPanel.add(barButton, c2);
        c2.gridy = 1;
        zahlungsButtonPanel.add(ecButton, c2);
        c2.gridy = 2;
        zahlungsButtonPanel.add(stornoButton, c2);

        zwischensummePanel.add(zahlungsButtonPanel);

        JPanel gridPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c3 = new GridBagConstraints();
        JPanel kundeGibtPanel = new JPanel();
        kundeGibtPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        kundeGibtField = new BigPriceField("");
        kundeGibtField.setColumns(5);
        removeDefaultKeyBindings(kundeGibtField);
        kundeGibtField.addKeyListener(removeNumPadAdapter);
        kundeGibtField.setEditable(false);
        kundeGibtField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateRueckgeld();
            }

            public void removeUpdate(DocumentEvent e) {
                updateRueckgeld();
            }

            public void changedUpdate(DocumentEvent e) {
                // Plain text components do not fire these events
            }
        });
        kundeGibtField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (barButton.isEnabled())
                        bar();
                }
            }
        });
        ((AbstractDocument) kundeGibtField.getDocument()).setDocumentFilter(bc.geldFilter);
        kundeGibtPanel.add(kundeGibtField);
        kundeGibtPanel.add(new BigLabel(bc.currencySymbol));
        passendButton = new BaseClass.BigButton("Passt");
        passendButton.setEnabled(false);
        passendButton.addActionListener(this);
        kundeGibtPanel.add(passendButton);

        JPanel gutscheinPanel = new JPanel();
        gutscheinPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        gutscheinEinloesenButton = new BaseClass.BigButton("GUTSCHEIN EINLÖSEN");
        gutscheinEinloesenButton.setBackground(gutscheinColor);
        gutscheinEinloesenButton.setForeground(Color.WHITE);
        gutscheinEinloesenButton.addActionListener(this);
        gutscheinPanel.add(gutscheinEinloesenButton);

        JPanel zuZahlenPanel = new JPanel();
        zuZahlenPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        bigPriceField = new BigPriceField("");
        bigPriceField.setEditable(false);
        removeDefaultKeyBindings(bigPriceField);
        bigPriceField.addKeyListener(removeNumPadAdapter);
        zuZahlenPanel.add(bigPriceField);

        JPanel rueckgeldPanel = new JPanel();
        rueckgeldPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        rueckgeldField = new BigPriceField("");
        rueckgeldField.setEditable(false);
        removeDefaultKeyBindings(rueckgeldField);
        rueckgeldField.addKeyListener(removeNumPadAdapter);
        rueckgeldPanel.add(rueckgeldField);

        c3.gridy = 0;
        c3.gridx = 0;
        c3.anchor = GridBagConstraints.EAST;
        gridPanel.add(new BigLabel("KUNDE GIBT:"), c3);

        c3.gridy = 0;
        c3.gridx = 1;
        c3.anchor = GridBagConstraints.WEST;
        gridPanel.add(kundeGibtPanel, c3);

        c3.gridy = 0;
        c3.gridx = 2;
        c3.anchor = GridBagConstraints.EAST;
        gridPanel.add(new BigLabel("ZU ZAHLEN:"), c3);

        c3.gridy = 0;
        c3.gridx = 3;
        c3.anchor = GridBagConstraints.WEST;
        gridPanel.add(zuZahlenPanel, c3);

        c3.gridy = 1;
        c3.gridx = 0;
        c3.anchor = GridBagConstraints.EAST;
        gridPanel.add(new BigLabel("GUTSCHEIN:"), c3);

        c3.gridy = 1;
        c3.gridx = 1;
        c3.anchor = GridBagConstraints.WEST;
        gridPanel.add(gutscheinPanel, c3);

        c3.gridy = 1;
        c3.gridx = 2;
        c3.anchor = GridBagConstraints.EAST;
        gridPanel.add(new BigLabel("RÜCKGELD:"), c3);

        c3.gridy = 1;
        c3.gridx = 3;
        c3.anchor = GridBagConstraints.EAST;
        gridPanel.add(rueckgeldPanel, c3);

        zwischensummePanel.add(gridPanel);
        bezahlPanel.add(zwischensummePanel);

        JPanel neuerKundePanel = new JPanel();
        neuerKundePanel.setLayout(new BoxLayout(neuerKundePanel, BoxLayout.PAGE_AXIS));
        zahlungsLabel = new BigLabel(" ");
        zahlungsLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        neuerKundePanel.add(zahlungsLabel);
        JPanel fertigButtonPanel = new JPanel(new BorderLayout());
        // center
        JPanel centerPanel = new JPanel();
        neuerKundeButton = new BaseClass.BigButton("Fertig/Nächster Kunde");
        neuerKundeButton.setBackground(Color.BLACK);
        neuerKundeButton.setForeground(Color.WHITE);
        neuerKundeButton.setEnabled(false);
        neuerKundeButton.addActionListener(this);
        centerPanel.add(neuerKundeButton);
        fertigButtonPanel.add(centerPanel, BorderLayout.CENTER);
        neuerKundePanel.add(fertigButtonPanel);
        bezahlPanel.add(neuerKundePanel);

        allPanel.add(bezahlPanel, BorderLayout.SOUTH);

        this.add(allPanel, BorderLayout.CENTER);
    }

    Integer showRabattButtons(GridBagConstraints c, Integer i) {
        for (JButton rbutton : rabattButtons) {
            rbutton.addActionListener(this);
            c.gridy = i;
            i++;
            rabattPanel.add(rbutton, c);
        }
        mitarbeiterRabattButton = new BaseClass.BigButton("Mitarbeiter");
        mitarbeiterRabattButton.addActionListener(this);
        c.gridy = i;
        i++;
        rabattPanel.add(mitarbeiterRabattButton, c);
        return i;
    }

    Integer showRabattIndividuellPanel(GridBagConstraints c, Integer i) {
        JPanel individuellRabattPanel = new JPanel(new GridBagLayout());
        individuellRabattPanel.setBorder(BorderFactory.createTitledBorder("individuell"));

        individuellRabattRelativField = new JTextField("");
        individuellRabattRelativField.setColumns(5);
        removeDefaultKeyBindings(individuellRabattRelativField);
        individuellRabattRelativField.addKeyListener(removeNumPadAdapter);
        individuellRabattRelativField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (individuellRabattRelativField.getText().length() > 0) {
                    individuellRabattRelativButton.setEnabled(true);
                } else {
                    individuellRabattRelativButton.setEnabled(false);
                }
            }

            public void removeUpdate(DocumentEvent e) {
                this.insertUpdate(e);
            }

            public void changedUpdate(DocumentEvent e) {
                // Plain text components do not fire these events
            }
        });
        ((AbstractDocument) individuellRabattRelativField.getDocument()).setDocumentFilter(bc.relFilter);
        individuellRabattRelativField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    individuellRabattRelativButton.doClick();
                }
            }
        });
        individuellRabattRelativField.setHorizontalAlignment(JTextField.RIGHT);
        individuellRabattRelativButton = new BaseClass.BigButton("OK");
        individuellRabattRelativButton.addActionListener(this);

        individuellRabattAbsolutField = new JTextField("");
        individuellRabattAbsolutField.setColumns(5);
        removeDefaultKeyBindings(individuellRabattAbsolutField);
        individuellRabattAbsolutField.addKeyListener(removeNumPadAdapter);
        individuellRabattAbsolutField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (individuellRabattAbsolutField.getText().length() > 0) {
                    individuellRabattAbsolutButton.setEnabled(true);
                } else {
                    individuellRabattAbsolutButton.setEnabled(false);
                }
            }

            public void removeUpdate(DocumentEvent e) {
                this.insertUpdate(e);
            }

            public void changedUpdate(DocumentEvent e) {
                // Plain text components do not fire these events
            }
        });
        ((AbstractDocument) individuellRabattAbsolutField.getDocument()).setDocumentFilter(bc.geldFilter);
        individuellRabattAbsolutField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    individuellRabattAbsolutButton.doClick();
                }
            }
        });
        individuellRabattAbsolutField.setHorizontalAlignment(JTextField.RIGHT);
        individuellRabattAbsolutButton = new BaseClass.BigButton("OK");
        individuellRabattAbsolutButton.addActionListener(this);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.insets = new Insets(3, 3, 3, 3);
        c2.anchor = GridBagConstraints.EAST;
        c2.gridy = 0;
        c2.gridx = 0;
        individuellRabattPanel.add(individuellRabattRelativField, c2);
        c2.anchor = GridBagConstraints.WEST;
        c2.gridy = 0;
        c2.gridx = 1;
        individuellRabattPanel.add(new BigLabel("%"), c2);
        c2.anchor = GridBagConstraints.EAST;
        c2.gridy = 0;
        c2.gridx = 2;
        individuellRabattPanel.add(individuellRabattRelativButton, c2);
        c2.anchor = GridBagConstraints.EAST;
        c2.gridy = 1;
        c2.gridx = 0;
        individuellRabattPanel.add(individuellRabattAbsolutField, c2);
        c2.anchor = GridBagConstraints.WEST;
        c2.gridy = 1;
        c2.gridx = 1;
        individuellRabattPanel.add(new BigLabel(bc.currencySymbol), c2);
        c2.anchor = GridBagConstraints.EAST;
        c2.gridy = 1;
        c2.gridx = 2;
        individuellRabattPanel.add(individuellRabattAbsolutButton, c2);
        c.gridy = i;
        i++;
        rabattPanel.add(individuellRabattPanel, c);
        c.weighty = 1.;
        c.gridy = i;
        i++;

        return i;
    }

    void showAbweichenderPreisPanel() {
        abweichenderPreisPanel = new JPanel(new GridBagLayout());
        abweichenderPreisPanel.setBorder(BorderFactory.createTitledBorder("Abweichender Preis"));

        abweichenderPreisField = new JTextField("");
        abweichenderPreisField.setColumns(5);
        removeDefaultKeyBindings(abweichenderPreisField);
        abweichenderPreisField.addKeyListener(removeNumPadAdapter);
        abweichenderPreisField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (abweichenderPreisField.getText().length() > 0) {
                    abweichenderPreisButton.setEnabled(true);
                } else {
                    abweichenderPreisButton.setEnabled(false);
                }
            }

            public void removeUpdate(DocumentEvent e) {
                this.insertUpdate(e);
            }

            public void changedUpdate(DocumentEvent e) {
                // Plain text components do not fire these events
            }
        });
        ((AbstractDocument) abweichenderPreisField.getDocument()).setDocumentFilter(bc.geldFilter);
        abweichenderPreisField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    abweichenderPreisButton.doClick();
                }
            }
        });
        abweichenderPreisField.setHorizontalAlignment(JTextField.RIGHT);
        abweichenderPreisButton = new BaseClass.BigButton("OK");
        abweichenderPreisButton.addActionListener(this);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.insets = new Insets(3, 3, 3, 3);
        c2.anchor = GridBagConstraints.EAST;
        c2.gridy = 0;
        c2.gridx = 0;
        abweichenderPreisPanel.add(abweichenderPreisField, c2);
        c2.anchor = GridBagConstraints.WEST;
        c2.gridy = 0;
        c2.gridx = 1;
        abweichenderPreisPanel.add(new BigLabel(bc.currencySymbol), c2);
        c2.anchor = GridBagConstraints.EAST;
        c2.gridy = 0;
        c2.gridx = 2;
        abweichenderPreisPanel.add(abweichenderPreisButton, c2);
    }

    void showAnzahlungsPanel() {
        anzahlungsPanel = new JPanel(new GridBagLayout());
        anzahlungsPanel.setBorder(BorderFactory.createTitledBorder("Anzahlungen"));

        anzahlungNeuButton = new BaseClass.BigButton("Neue Anzahlung");
        anzahlungNeuButton.setBackground(new Color(70, 197, 80));
        anzahlungNeuButton.addActionListener(this);
        anzahlungNeuPanel = new JPanel(new GridBagLayout());
        anzahlungAufloesButton = new BaseClass.BigButton("Anzahlung auflösen");
        anzahlungAufloesButton.setBackground(new Color(70, 197, 175));
        anzahlungAufloesButton.addActionListener(this);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.anchor = GridBagConstraints.NORTH;
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.ipady = 10;
        c2.insets = new Insets(3, 0, 3, 0);
        c2.gridy = 0;
        anzahlungsPanel.add(anzahlungNeuButton, c2);
        c2.gridy = 1;
        anzahlungsPanel.add(anzahlungNeuPanel, c2);
        c2.gridy = 2;
        anzahlungsPanel.add(anzahlungAufloesButton, c2);
    }

    void showAnzahlungNeuPanel() {
        anzahlungsPanel.remove(anzahlungNeuPanel);
        anzahlungsPanel.revalidate();
        anzahlungNeuPanel = new JPanel(new GridBagLayout());

        anzahlungsBetragField = new JTextField("");
        anzahlungsBetragField.setColumns(5);
        removeDefaultKeyBindings(anzahlungsBetragField);
        anzahlungsBetragField.addKeyListener(removeNumPadAdapter);
        anzahlungsBetragField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (anzahlungsBetragField.getText().length() > 0) {
                    anzahlungNeuOKButton.setEnabled(true);
                } else {
                    anzahlungNeuOKButton.setEnabled(false);
                }
            }

            public void removeUpdate(DocumentEvent e) {
                this.insertUpdate(e);
            }

            public void changedUpdate(DocumentEvent e) {
                // Plain text components do not fire these events
            }
        });
        ((AbstractDocument) anzahlungsBetragField.getDocument()).setDocumentFilter(bc.geldFilter);
        anzahlungsBetragField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    anzahlungNeuOKButton.doClick();
                }
            }
        });
        anzahlungsBetragField.setHorizontalAlignment(JTextField.RIGHT);
        anzahlungNeuOKButton = new BaseClass.BigButton("OK");
        anzahlungNeuOKButton.addActionListener(this);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.insets = new Insets(3, 3, 3, 3);
        c2.anchor = GridBagConstraints.EAST;
        c2.gridy = 0;
        c2.gridx = 0;
        anzahlungNeuPanel.add(new JLabel("Betrag:"), c2);
        c2.gridy = 1;
        c2.gridx = 0;
        anzahlungNeuPanel.add(anzahlungsBetragField, c2);
        c2.anchor = GridBagConstraints.WEST;
        c2.gridy = 1;
        c2.gridx = 1;
        anzahlungNeuPanel.add(new BigLabel(bc.currencySymbol), c2);
        c2.anchor = GridBagConstraints.EAST;
        c2.gridy = 1;
        c2.gridx = 2;
        anzahlungNeuPanel.add(anzahlungNeuOKButton, c2);
        c2.anchor = GridBagConstraints.NORTH;
        c2.ipady = 10;
        c2.insets = new Insets(3, 0, 3, 0);
        c2.gridx = 0;
        c2.gridy = 1;
        anzahlungsPanel.add(anzahlungNeuPanel, c2);
    }

    void hideAnzahlungNeuPanel() {
        anzahlungsPanel.remove(anzahlungNeuPanel);
        anzahlungsPanel.revalidate();
        anzahlungNeuPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c2 = new GridBagConstraints();
        c2.anchor = GridBagConstraints.NORTH;
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.ipady = 10;
        c2.insets = new Insets(3, 0, 3, 0);
        c2.gridy = 1;
        anzahlungsPanel.add(anzahlungNeuPanel, c2);
    }

    void showButtons() {
        JPanel buttonPanel = new JPanel(new BorderLayout());
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.PAGE_AXIS));

        rabattPanel = new JPanel();
        rabattPanel.setLayout(new GridBagLayout());
        rabattPanel.setBorder(BorderFactory.createTitledBorder("Rabatt"));

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 10;
        c.insets = new Insets(3, 0, 3, 0);

        Integer i = 0;
        i = showRabattButtons(c, i);
        i = showRabattIndividuellPanel(c, i);
        showAbweichenderPreisPanel();
        showAnzahlungsPanel();

        northPanel.add(rabattPanel);
        northPanel.add(abweichenderPreisPanel);
        northPanel.add(anzahlungsPanel);
        buttonPanel.add(northPanel, BorderLayout.NORTH);
        this.add(buttonPanel, BorderLayout.WEST);
    }

    void updateRabattButtons() {
        boolean enabled = false;
        boolean abweichEnabled = false;
        if (kassierArtikel.size() > 0) {
            enabled = false;
            abweichEnabled = false;
            for (int i = kassierArtikel.size() - 1; i >= 0; i--) {
                String type = kassierArtikel.get(i).getType();
                if (type.equals("rabatt")) {
                    // Artikel hat schon Rabatt, keinen Rabatt
                    // mehr erlauben
                    break;
                }
                if (type.equals("leergut")) {
                    // Es handelt sich um Leergut, kein Rabatt
                    // erlauben
                    break;
                }
                if (type.equals("rueckgabe")) {
                    // Es handelt sich um eine Rückgabe, kein Rabatt
                    // erlauben, aber abweichender Preis muss möglich sein,
                    // damit man auch rabattierte Artikel zur Not darüber
                    // richtig zurückgeben kann
                    abweichEnabled = true;
                    break;
                }
                if (type.equals("gutschein") || type.equals("gutscheineinloesung")) {
                    // Es handelt sich um einen Gutschein, kein Rabatt
                    // erlauben
                    break;
                }
                if (type.equals("artikel")) {
                    // Artikel hatte wohl noch keinen Rabatt, Rabatt erlauben
                    enabled = true;
                    abweichEnabled = true;
                    break;
                }
            }
        }
        for (JButton rbutton : rabattButtons) {
            rbutton.setEnabled(enabled);
        }
        mitarbeiterRabattButton.setEnabled(false);
        individuellRabattRelativField.setText("");
        individuellRabattRelativField.setEditable(enabled);
        individuellRabattRelativButton.setEnabled(false);
        individuellRabattAbsolutField.setText("");
        individuellRabattAbsolutField.setEditable(enabled);
        individuellRabattAbsolutButton.setEnabled(false);
        abweichenderPreisField.setText("");
        abweichenderPreisField.setEditable(abweichEnabled);
        abweichenderPreisButton.setEnabled(false);
    }

    void updateRabattButtonsZwischensumme() {
        boolean enabled = false;
        if (!kassierArtikel.lastElement().getType().equals("rabattrechnung")) {
            // determine place to start (after last "Rabatt auf Rechnung")
            int startIndex = 0;
            for (int i = kassierArtikel.size() - 1; i >= 0; i--) {
                if (kassierArtikel.get(i).getType().equals("rabattrechnung")) {
                    startIndex = i + 1;
                    break;
                }
            }
            // scan through artikel list to search for artikels without rabatt
            int artikelCount = 0;
            for (int i = startIndex; i < kassierArtikel.size(); i++) {
                if (kassierArtikel.get(i).getType().equals("artikel")) {
                    artikelCount++;
                } else if (kassierArtikel.get(i).getType().equals("rabatt")) {
                    artikelCount = 0;
                }
                if (artikelCount > 1) { // there was an article without Rabatt
                    enabled = true;
                    break;
                }
            }
            if (artikelCount > 0) {
                enabled = true;
            } // (at least) last article had no Rabatt, this is needed in case
              // there is only one article without Rabatt
        }
        for (JButton rbutton : rabattButtons) {
            rbutton.setEnabled(enabled);
        }
        if (allowMitarbeiterRabatt) {
            mitarbeiterRabattButton.setEnabled(enabled);
        }
        individuellRabattRelativField.setText("");
        individuellRabattRelativField.setEditable(enabled);
        individuellRabattRelativButton.setEnabled(false);
        individuellRabattAbsolutField.setText("");
        individuellRabattAbsolutField.setEditable(false);
        individuellRabattAbsolutButton.setEnabled(false);
        abweichenderPreisField.setText("");
        abweichenderPreisField.setEditable(false);
        abweichenderPreisButton.setEnabled(false);
    }

    void showTable() {
        Vector<String> colors = new Vector<String>();
        for (KassierArtikel a : kassierArtikel) {
            colors.add(a.getColor());
        }
        myTable = new ArticleSelectTable(data, columnLabels, colors);
        removeDefaultKeyBindings(myTable, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        myTable.addKeyListener(removeNumPadAdapter);
        myTable.setFont(BaseClass.mediumFont);
        myTable.setRowHeight(20);
        // myTable.setBounds(71,53,150,100);
        setTableProperties(myTable);
        TableColumn entf = myTable.getColumn("Entfernen");
        entf.setPreferredWidth(2);
        // myTable.setAutoResizeMode(5);

        articleListPanel = new JPanel(new BorderLayout());
        articleListPanel.setBorder(BorderFactory.createTitledBorder("Gewählte Artikel"));

        scrollPane = new JScrollPane(myTable);
        articleListPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = createBottomPanel();
        JPanel zwischensummePanel = new JPanel();
        zwischensummePanel.setLayout(new FlowLayout());
        zwischensummeButton = new BaseClass.BigButton("ZWS");
        zwischensummeButton.setBackground(Color.RED.darker());
        zwischensummeButton.setForeground(Color.WHITE);
        zwischensummeButton.setMnemonic(KeyEvent.VK_Z);
        zwischensummeButton.addActionListener(this);
        if (data.size() == 0) {
            zwischensummeButton.setEnabled(false);
            anzahlungNeuButton.setEnabled(false); // whenever ZWS is possible, also Anzahlung must be possible
            hideAnzahlungNeuPanel();
            anzahlungAufloesButton.setEnabled(true);
        } else {
            anzahlungNeuButton.setEnabled(true);
            anzahlungAufloesButton.setEnabled(true);
            for (KassierArtikel ka : kassierArtikel) {
                if (ka.getType().equals("anzahlung") || ka.getType().equals("anzahlungsaufloesung")) {
                    anzahlungNeuButton.setEnabled(false);
                    hideAnzahlungNeuPanel();
                    if (ka.getType().equals("anzahlung")) {
                        anzahlungAufloesButton.setEnabled(false);
                    }
                    break;
                }
            }
        }
        zwischensummePanel.add(zwischensummeButton);
        zwischensummePanel.add(Box.createRigidArea(new Dimension(20,0)));
        bottomPanel.add(zwischensummePanel, BorderLayout.EAST);
        articleListPanel.add(bottomPanel, BorderLayout.SOUTH);

        allPanel.add(articleListPanel, BorderLayout.CENTER);
    }

    void emptyTable() {
        data = new Vector<Vector<Object>>();
        kassierArtikel = new Vector<KassierArtikel>();
        removeButtons = new Vector<JButton>();
        mwsts = new Vector<BigDecimal>();
    }

    private void clearAll() {
        data.clear();
        kassierArtikel.clear();
        removeButtons.clear();
        mwsts.clear();
        zahlungsModus = "bar";
    }

    private void updateAll() {
        this.remove(allPanel);
        this.revalidate();
        showAll();
        asPanel.emptyArtikelBox();

        // scroll the table scroll pane to bottom:
        scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
    }

    private void updateTable() {
        allPanel.remove(articleListPanel);
        allPanel.revalidate();
        showTable();
    }

    void updateRueckgeld() {
        if (kundeGibtField.getDocument().getLength() == 0) {
            rueckgeldField.setText("");
            // neuerKundeButton.setEnabled(false);

            if (display != null && display.deviceWorks()) {
                display.printZWS(bigPriceField.getText());
            }
        } else {
            BigDecimal totalPrice = new BigDecimal(getTotalPrice());
            BigDecimal kundeGibt = new BigDecimal(getKundeGibt());
            BigDecimal rueckgeld = kundeGibt.subtract(totalPrice);
            if (rueckgeld.signum() < 0) {
                rueckgeldField.setForeground(Color.RED);
                // neuerKundeButton.setEnabled(false);
            } else {
                rueckgeldField.setForeground(Color.GREEN.darker().darker());
                // neuerKundeButton.setEnabled(true);
            }
            rueckgeldField.setText(bc.priceFormatter(rueckgeld) + ' ' + bc.currencySymbol);

            if (display != null && display.deviceWorks()) {
                String kundeGibtStr = bc.priceFormatter(kundeGibt) + ' ' + bc.currencySymbol;
                String rueckgeldStr = bc.priceFormatter(rueckgeld) + ' ' + bc.currencySymbol;
                display.printReturnMoney(kundeGibtStr, rueckgeldStr);
            }
        }
    }

    String getKundeGibt() {
        return bc.priceFormatterIntern(kundeGibtField.getText());
    }

    int getLastArticleIndex() {
        int index = -1;
        for (int i = kassierArtikel.size() - 1; i >= 0; i--) {
            String type = kassierArtikel.get(i).getType();
            if (type.equals("artikel") || type.equals("rueckgabe")) {
                index = i;
                break;
            }
        }
        return index;
    }

    //////////////////////////////////
    // DB query functions:
    //////////////////////////////////
    private void checkForRabatt() {
        /*
         * QUERY ALL THE RABATTAKTIONEN, STORE THEM IN VECTORS, ORDERED BY
         * RABATT MODE:
         * =====================================================================
         * ======= 1. rabatt for artikel_id: artikel_id of artikel = x SELECT
         * rabatt_absolut, rabatt_relativ, mengenrabatt_schwelle,
         * mengenrabatt_relativ, mengenrabatt_anzahl_kostenlos FROM rabattaktion
         * WHERE artikel_id = x;
         *
         * 2. rabatt for produktgruppe: artikel_id of artikel = x SELECT
         * toplevel_id AS x, sub_id AS y, subsub_id AS z FROM produktgruppe AS p
         * INNER JOIN artikel AS a USING (produktgruppen_id) WHERE a.artikel_id
         * = x;
         *
         * toplevel_id, sub_id, subsub_id of artikel = x, y, z SELECT
         * rabatt_absolut, rabatt_relativ, mengenrabatt_schwelle,
         * mengenrabatt_relativ, mengenrabatt_anzahl_kostenlos FROM rabattaktion
         * AS r INNER JOIN produktgruppe AS p USING (produktgruppen_id) WHERE
         * (toplevel_id) = x AND (sub_id = y OR sub_id IS NULL) AND (subsub_id =
         * z OR subsub_id IS NULL);
         */
        int artikelID = selectedArticleID;
        BigDecimal stueck = new BigDecimal(selectedStueck);
        BigDecimal einzelpreis = new BigDecimal(bc.priceFormatterIntern(preisField.getText()));
        Vector<Vector<Object>> einzelrabattAbsolutVector = new Vector<Vector<Object>>();
        Vector<Vector<Object>> einzelrabattRelativVector = new Vector<Vector<Object>>();
        Vector<Vector<Object>> mengenrabattAnzahlVector = new Vector<Vector<Object>>();
        Vector<Vector<Object>> mengenrabattRelativVector = new Vector<Vector<Object>>();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    // get alle Rabatte auf artikelID
                    "SELECT rabatt_absolut, rabatt_relativ, mengenrabatt_schwelle, mengenrabatt_anzahl_kostenlos, "
                            + "mengenrabatt_relativ, aktionsname, rabatt_id FROM rabattaktion WHERE artikel_id = ? "
                            + "AND von <= NOW() AND IFNULL(bis >= NOW(), true)");
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                BigDecimal einzelAbsolut = rs.getString(1) == null ? null : new BigDecimal(rs.getString(1));
                BigDecimal einzelRelativ = rs.getString(2) == null ? null : new BigDecimal(rs.getString(2));
                BigDecimal mengenSchwelle = rs.getString(3) == null ? null : new BigDecimal(rs.getInt(3));
                BigDecimal mengenAnzahl = rs.getString(4) == null ? null : new BigDecimal(rs.getInt(4));
                BigDecimal mengenRelativ = rs.getString(5) == null ? null : new BigDecimal(rs.getString(5));
                String aktionsname = rs.getString(6);
                int rabattID = rs.getInt(7);
                addRabatteToVectors(einzelAbsolut, einzelRelativ, mengenSchwelle, mengenAnzahl, mengenRelativ,
                        aktionsname, rabattID, einzelrabattAbsolutVector, einzelrabattRelativVector,
                        mengenrabattAnzahlVector, mengenrabattRelativVector);
            }
            rs.close();
            pstmt.close();
            pstmt = connection.prepareStatement(
                    // get toplevel_id, sub_id, subsub_id for produktgruppenID
                    // of artikelID
                    "SELECT toplevel_id, sub_id, subsub_id FROM produktgruppe AS p INNER JOIN "
                            + "artikel AS a USING (produktgruppen_id) WHERE a.artikel_id = ?");
            pstmtSetInteger(pstmt, 1, artikelID);
            rs = pstmt.executeQuery();
            rs.next();
            int toplevelID = rs.getInt(1);
            int subID = rs.getInt(2);
            int subsubID = rs.getInt(3);
            rs.close();
            pstmt.close();
            pstmt = connection.prepareStatement(
                    // get alle Rabatte auf produktgruppe
                    "SELECT rabatt_absolut, rabatt_relativ, mengenrabatt_schwelle, mengenrabatt_anzahl_kostenlos, "
                            + "mengenrabatt_relativ, aktionsname, rabatt_id FROM rabattaktion AS r INNER JOIN produktgruppe AS p "
                            + "USING (produktgruppen_id) WHERE (toplevel_id = ?) AND "
                            + "(sub_id = ? OR sub_id IS NULL) AND (subsub_id = ? OR subsub_id IS NULL) "
                            + "AND von <= NOW() AND IFNULL(bis >= NOW(), true)");
            pstmtSetInteger(pstmt, 1, toplevelID);
            pstmtSetInteger(pstmt, 2, subID);
            pstmtSetInteger(pstmt, 3, subsubID);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                BigDecimal einzelAbsolut = rs.getString(1) == null ? null : new BigDecimal(rs.getString(1));
                BigDecimal einzelRelativ = rs.getString(2) == null ? null : new BigDecimal(rs.getString(2));
                BigDecimal mengenSchwelle = rs.getString(3) == null ? null : new BigDecimal(rs.getInt(3));
                BigDecimal mengenAnzahl = rs.getString(4) == null ? null : new BigDecimal(rs.getInt(4));
                BigDecimal mengenRelativ = rs.getString(5) == null ? null : new BigDecimal(rs.getString(5));
                String aktionsname = rs.getString(6);
                int rabattID = rs.getInt(7);
                addRabatteToVectors(einzelAbsolut, einzelRelativ, mengenSchwelle, mengenAnzahl, mengenRelativ,
                        aktionsname, rabattID, einzelrabattAbsolutVector, einzelrabattRelativVector,
                        mengenrabattAnzahlVector, mengenrabattRelativVector);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }

        /*
         * RABATTAKTIONEN IN DIESER REIHENFOLGE ANWENDEN:
         * ============================================== Es gibt 4 Rabatt Modi:
         * 1. Alle Einzelrabatte, absolut 2. Alle Einzelrabatte, relativ 3. Alle
         * Mengenrabatte, Anzahl kostenlos 4. Alle Mengenrabatte, relativ
         */
        for (Vector<Object> vector : einzelrabattAbsolutVector) {
            int rabattID = (Integer) vector.get(0);
            String aktionsname = (String) vector.get(1);
            BigDecimal einzelAbsRabatt = (BigDecimal) vector.get(2);
            BigDecimal reduktion = stueck.multiply(einzelAbsRabatt).multiply(bc.minusOne);
            einzelpreis = einzelpreis.subtract(einzelAbsRabatt);

            addRabattRow(rabattID, aktionsname, reduktion, stueck);
        }
        for (Vector<Object> vector : einzelrabattRelativVector) {
            int rabattID = (Integer) vector.get(0);
            String aktionsname = (String) vector.get(1);
            BigDecimal einzelRelRabatt = (BigDecimal) vector.get(2);
            BigDecimal reduktion = new BigDecimal(bc.priceFormatterIntern(
                    stueck.multiply(einzelRelRabatt).multiply(einzelpreis).multiply(bc.minusOne)));
            einzelpreis = einzelpreis.subtract(einzelRelRabatt.multiply(einzelpreis));

            addRabattRow(rabattID, aktionsname, reduktion, stueck);
        }
        for (Vector<Object> vector : mengenrabattAnzahlVector) {
            int rabattID = (Integer) vector.get(0);
            String aktionsname = (String) vector.get(1);
            BigDecimal mengenSchwelle = (BigDecimal) vector.get(2);
            BigDecimal mengenAnzKostenlos = (BigDecimal) vector.get(3);
            if (stueck.compareTo(mengenSchwelle) >= 0) { // if stueck >=
                                                         // mengenSchwelle
                BigDecimal reduktion = (new BigDecimal(stueck.intValue() / mengenSchwelle.intValue()))
                        .multiply(mengenAnzKostenlos).multiply(einzelpreis).multiply(bc.minusOne);
                stueck = stueck.subtract(mengenAnzKostenlos);

                addRabattRow(rabattID, aktionsname, reduktion, stueck);
            }
        }
        for (Vector<Object> vector : mengenrabattRelativVector) {
            int rabattID = (Integer) vector.get(0);
            String aktionsname = (String) vector.get(1);
            BigDecimal mengenSchwelle = (BigDecimal) vector.get(2);
            BigDecimal mengenRelRabatt = (BigDecimal) vector.get(3);
            if (stueck.compareTo(mengenSchwelle) >= 0) { // if stueck >=
                                                         // mengenSchwelle
                BigDecimal reduktion = new BigDecimal(bc.priceFormatterIntern(
                        stueck.multiply(mengenRelRabatt).multiply(einzelpreis).multiply(bc.minusOne)));
                einzelpreis = einzelpreis.subtract(mengenRelRabatt.multiply(einzelpreis));

                addRabattRow(rabattID, aktionsname, reduktion, stueck);
            }
        }
    }

    private void addRabatteToVectors(BigDecimal einzelAbsolut, BigDecimal einzelRelativ, BigDecimal mengenSchwelle,
            BigDecimal mengenAnzahl, BigDecimal mengenRelativ, String aktionsname, int rabattID,
            Vector<Vector<Object>> einzelrabattAbsolutVector, Vector<Vector<Object>> einzelrabattRelativVector,
            Vector<Vector<Object>> mengenrabattAnzahlVector, Vector<Vector<Object>> mengenrabattRelativVector) {
        if (einzelAbsolut != null) {
            Vector<Object> temp = new Vector<Object>(3);
            temp.add(Integer.valueOf(rabattID));
            temp.add(aktionsname);
            temp.add(einzelAbsolut);
            einzelrabattAbsolutVector.add(temp);
        }
        if (einzelRelativ != null) {
            Vector<Object> temp = new Vector<Object>(3);
            temp.add(Integer.valueOf(rabattID));
            temp.add(aktionsname);
            temp.add(einzelRelativ);
            einzelrabattRelativVector.add(temp);
        }
        if (mengenSchwelle != null && mengenAnzahl != null) {
            Vector<Object> temp = new Vector<Object>(4);
            temp.add(Integer.valueOf(rabattID));
            temp.add(aktionsname);
            temp.add(mengenSchwelle);
            temp.add(mengenAnzahl);
            mengenrabattAnzahlVector.add(temp);
        }
        if (mengenSchwelle != null && mengenRelativ != null) {
            Vector<Object> temp = new Vector<Object>(4);
            temp.add(Integer.valueOf(rabattID));
            temp.add(aktionsname);
            temp.add(mengenSchwelle);
            temp.add(mengenRelativ);
            mengenrabattRelativVector.add(temp);
        }
    }

    private void addRabattRow(int rabattID, String aktionsname, BigDecimal reduktion, BigDecimal stueck) {
        BigDecimal artikelMwSt = kassierArtikel.lastElement().getMwst();

        hinzufuegenRaw(null, rabattID, einrueckung + aktionsname,
            "RABATT", "red", "rabatt", "", stueck.intValue(),
            reduktion, reduktion, artikelMwSt, null,
            false, false, null);
    }

    private void checkForPfand() {
        BigDecimal stueck = new BigDecimal(selectedStueck);
        int pfandArtikelID = queryPfandArtikelID(selectedArticleID);
        // gab es Pfand? Wenn ja, fuege Zeile in Tabelle:
        if (pfandArtikelID > 0) {
            BigDecimal pfand = new BigDecimal(getSalePrice(pfandArtikelID));
            String pfandName = getArticleName(pfandArtikelID)[0];
            BigDecimal gesamtPfand = pfand.multiply(stueck);
            BigDecimal pfandMwSt = kassierArtikel.lastElement().getMwst();

            hinzufuegenRaw(pfandArtikelID, null, einrueckung + pfandName,
                "PFAND", "blue", "pfand", "", stueck.intValue(),
                pfand, gesamtPfand, pfandMwSt, null,
                false, false, null);
        }
    }

    private int queryPfandArtikelID(int artikelID) {
        int pfandArtikelID = -1;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT pfand.artikel_id FROM artikel INNER JOIN produktgruppe USING (produktgruppen_id) "
                            + "INNER JOIN pfand USING (pfand_id) " + "WHERE artikel.artikel_id = ?");
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) { // artikel hat Pfand
                pfandArtikelID = rs.getInt(1);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return pfandArtikelID;
    }

    private boolean artikelHasPfand() {
        int artikelID = selectedArticleID;
        boolean hasPfand = false;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement("SELECT p.pfand_id IS NOT NULL FROM artikel AS a "
                    + "INNER JOIN produktgruppe AS p USING (produktgruppen_id) " + "WHERE a.artikel_id = ?");
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            hasPfand = rs.getBoolean(1);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return hasPfand;
    }

    //////////////////////////////////
    // DB functions
    //////////////////////////////////
    private Integer maxRechnungsNr() {
        Integer maxRechNr = null;
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MAX(rechnungs_nr) FROM "+tableForMode("verkauf"));
            rs.next();
            maxRechNr = rs.getInt(1);
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return maxRechNr;
    }

    String queryLatestVerkauf() {
        String date = "";
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MAX(verkaufsdatum) "+
                    "FROM "+tableForMode("verkauf"));
            rs.next(); date = rs.getString(1); rs.close();
            if (date == null){
                date = "";
            }
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return date;
    }

    private int insertIntoVerkauf(boolean ec, BigDecimal kundeGibt) {
        int rechnungsNr = -1;
        try {
            String latestVerkaufStr = queryLatestVerkauf();
            if (!latestVerkaufStr.equals("")) {
                DateTime latestVerkauf = new DateTime(latestVerkaufStr);
                DateTime now = new DateTime(now());
                if (now.lt(latestVerkauf)) {
                    JOptionPane.showMessageDialog(this, "Fehler: Rechnung kann nicht gespeichert werden, da das aktuelle Datum vor dem der letzten Rechnung liegt.\n"+
                            "Bitte das Datum im Kassenserver korrigieren.", "Fehler",
                            JOptionPane.ERROR_MESSAGE);
                    return rechnungsNr;
                }
            }
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO "+tableForMode("verkauf")+" SET verkaufsdatum = NOW(), ec_zahlung = ?, kunde_gibt = ?"
            );
            pstmtSetBoolean(pstmt, 1, ec);
            pstmt.setBigDecimal(2, kundeGibt);
            int result = pstmt.executeUpdate();
            pstmt.close();
            if (result == 0) {
                JOptionPane.showMessageDialog(this, "Fehler: Rechnung konnte nicht abgespeichert werden.", "Fehler",
                        JOptionPane.ERROR_MESSAGE);
                return rechnungsNr;
            }
            rechnungsNr = maxRechnungsNr();
            for (Map.Entry<BigDecimal, Vector<BigDecimal>> entry : this.vatMap.entrySet()) {
                pstmt = connection.prepareStatement("INSERT INTO "+tableForMode("verkauf_mwst")+" SET rechnungs_nr = ?, mwst_satz = ?, "
                        + "mwst_netto = ?, mwst_betrag = ?");
                pstmtSetInteger(pstmt, 1, rechnungsNr);
                pstmt.setBigDecimal(2, entry.getKey());
                pstmt.setBigDecimal(3, entry.getValue().get(0));
                pstmt.setBigDecimal(4, entry.getValue().get(1));
                result = pstmt.executeUpdate();
                pstmt.close();
                if (result == 0) {
                    JOptionPane.showMessageDialog(this,
                            "Fehler: MwSt.-Information für Rechnung konnte nicht abgespeichert werden.", "Fehler",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
            for (int i = 0; i < kassierArtikel.size(); i++) {
                pstmt = connection.prepareStatement("INSERT INTO "+tableForMode("verkauf_details")+" SET rechnungs_nr = ?, position = ?, "
                        + "artikel_id = ?, rabatt_id = ?, stueckzahl = ?, ges_preis = ?, mwst_satz = ?");
                pstmtSetInteger(pstmt, 1, rechnungsNr);
                pstmtSetInteger(pstmt, 2, kassierArtikel.get(i).getPosition());
                pstmtSetInteger(pstmt, 3, kassierArtikel.get(i).getArtikelID());
                pstmtSetInteger(pstmt, 4, kassierArtikel.get(i).getRabattID());
                pstmtSetInteger(pstmt, 5, kassierArtikel.get(i).getStueckzahl());
                pstmt.setBigDecimal(6, kassierArtikel.get(i).getGesPreis());
                pstmt.setBigDecimal(7, kassierArtikel.get(i).getMwst());
                result = pstmt.executeUpdate();
                pstmt.close();
                if (result == 0) {
                    JOptionPane.showMessageDialog(this, "Fehler: Artikel mit ID " + kassierArtikel.get(i).getArtikelID()
                            + " konnte nicht abgespeichert werden.", "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return rechnungsNr;
    }

    private void insertIntoKassenstand(int rechnungsNr) {
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("SELECT verkaufsdatum FROM "+tableForMode("verkauf")+" WHERE rechnungs_nr = ?");
            pstmtSetInteger(pstmt, 1, rechnungsNr);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            String verkaufsdatum = rs.getString(1);
            rs.close();
            pstmt.close();
            BigDecimal betrag = new BigDecimal(getTotalPrice());
            BigDecimal alterKassenstand = mainWindow.retrieveKassenstand();
            BigDecimal neuerKassenstand = alterKassenstand.add(betrag);
            pstmt = connection.prepareStatement(
                "INSERT INTO "+tableForMode("kassenstand")+" SET rechnungs_nr = ?, buchungsdatum = ?, "+
                "manuell = FALSE, neuer_kassenstand = ?"
            );
            pstmtSetInteger(pstmt, 1, rechnungsNr);
            pstmt.setString(2, verkaufsdatum);
            pstmt.setBigDecimal(3, neuerKassenstand);
            int result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();
            if (result == 0) {
                JOptionPane.showMessageDialog(this, "Fehler: Kassenstand konnte nicht geändert werden.", "Fehler",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                mainWindow.updateBottomPanel();
            }
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    private void maybeInsertIntoAnzahlung(int rechnungsNr) {
        // find out if Anzahlung has been made in this rechnung:
        boolean thisIsAnzahlung = false;
        for (KassierArtikel ka : kassierArtikel) {
            if (ka.getType().equals("anzahlung")) {
                thisIsAnzahlung = true;
                break;
            }
        }
        if (thisIsAnzahlung) {
            try {
                // insert into table anzahlung
                Connection connection = this.pool.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(
                    "INSERT INTO "+tableForMode("anzahlung")+" SET "+
                    "datum = (SELECT verkaufsdatum FROM "+tableForMode("verkauf")+" WHERE rechnungs_nr = ?), "+
                    "anzahlung_in_rech_nr = ?"
                );
                pstmtSetInteger(pstmt, 1, rechnungsNr);
                pstmtSetInteger(pstmt, 2, rechnungsNr);
                int result = pstmt.executeUpdate();
                pstmt.close();
                if (result == 0) {
                    JOptionPane.showMessageDialog(this, "Fehler: Anzahlung konnte nicht gespeichert werden.", "Fehler",
                        JOptionPane.ERROR_MESSAGE);
                }
                reconstructPrices(); // need to recover zeroed prices
                // to store actual prices, need to fetch all the vd_ids in order
                pstmt = connection.prepareStatement(
                    "SELECT vd_id FROM "+tableForMode("verkauf_details")+" "+
                    "WHERE rechnungs_nr = ?"
                );
                pstmtSetInteger(pstmt, 1, rechnungsNr);
                ResultSet rs = pstmt.executeQuery();
                Vector<Integer> vdIDs = new Vector<Integer>();
                while (rs.next()) {
                    vdIDs.add(rs.getInt(1));
                }
                rs.close();
                // now insert prices into table anzahlung_details for each article belonging
                // to anzahlung (starting at beginning, up to artikel_id == anzahlungArtikelID)
                for (int i = 0; i < kassierArtikel.size(); i++) {
                    KassierArtikel ka = kassierArtikel.get(i);
                    if (ka.getArtikelID() == anzahlungArtikelID) break; // rest does not belong to anzahlung
                    pstmt = connection.prepareStatement(
                        "INSERT INTO "+tableForMode("anzahlung_details")+" "+
                        "SET rechnungs_nr = ?, vd_id = ?, "+
                        "ges_preis = ?");
                    pstmtSetInteger(pstmt, 1, rechnungsNr);
                    pstmtSetInteger(pstmt, 2, vdIDs.get(i));
                    pstmt.setBigDecimal(3, ka.getGesPreis());
                    result = pstmt.executeUpdate();
                    pstmt.close();
                    if (result == 0) {
                        JOptionPane.showMessageDialog(this, "Fehler: Artikel mit vd_id " + vdIDs.get(i)+
                        " konnte nicht in Anzahlung gespeichert werden.", "Fehler", JOptionPane.ERROR_MESSAGE);
                    }
                }
                connection.close();
            } catch (SQLException ex) {
                logger.error("Exception:", ex);
                showDBErrorDialog(ex.getMessage());
            }
        }
    }

    private void maybeInsertAufloesungIntoAnzahlung(int rechnungsNr) {
        // fuege Zeile in aufloesung Tabelle für jede Anzahlungsaufloesung:
        for (KassierArtikel ka : kassierArtikel) {
            if (ka.getType().equals("anzahlungsaufloesung")) {
                try {
                    // insert into table anzahlung
                    Connection connection = this.pool.getConnection();
                    PreparedStatement pstmt = connection.prepareStatement(
                        "INSERT INTO "+tableForMode("anzahlung")+" SET "+
                        "datum = (SELECT verkaufsdatum FROM "+tableForMode("verkauf")+" WHERE rechnungs_nr = ?), "+
                        "anzahlung_in_rech_nr = ?, "+
                        "aufloesung_in_rech_nr = ?"
                    );
                    pstmtSetInteger(pstmt, 1, rechnungsNr);
                    pstmtSetInteger(pstmt, 2, ka.getAnzahlungRechNr());
                    pstmtSetInteger(pstmt, 3, rechnungsNr);
                    int result = pstmt.executeUpdate();
                    pstmt.close();
                    connection.close();
                    if (result == 0) {
                        JOptionPane.showMessageDialog(this, "Fehler: Anzahlung konnte nicht gespeichert werden.", "Fehler",
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    logger.error("Exception:", ex);
                    showDBErrorDialog(ex.getMessage());
                }
            }
        }
    }

    private void maybeInsertIntoGutschein(int rechnungsNr) {
        for (KassierArtikel ka : kassierArtikel) {
            if (ka.getType().equals("gutschein")) {
                try {
                    // insert into table gutschein
                    Connection connection = this.pool.getConnection();
                    PreparedStatement pstmt = connection.prepareStatement(
                        "INSERT INTO "+tableForMode("gutschein")+" SET "+
                        "gutschein_nr = ?, "+
                        "datum = (SELECT verkaufsdatum FROM "+tableForMode("verkauf")+" WHERE rechnungs_nr = ?), "+
                        "gutschein_in_vd_id = (SELECT vd_id FROM "+tableForMode("verkauf_details")+" "+
                        "    WHERE rechnungs_nr = ? AND position = ?), "+
                        "einloesung_in_vd_id = NULL, "+
                        "restbetrag = ?"
                    );
                    pstmtSetInteger(pstmt, 1, ka.getGutscheinNr());
                    pstmtSetInteger(pstmt, 2, rechnungsNr);
                    pstmtSetInteger(pstmt, 3, rechnungsNr);
                    pstmtSetInteger(pstmt, 4, ka.getPosition());
                    pstmt.setBigDecimal(5, ka.getEinzelPreis());
                    int result = pstmt.executeUpdate();
                    pstmt.close();
                    connection.close();
                    if (result == 0) {
                        JOptionPane.showMessageDialog(this, "Fehler: Gutschein Nr. " + ka.getGutscheinNr() +
                        " konnte nicht in Gutscheintabelle gespeichert werden.", "Fehler", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    logger.error("Exception:", ex);
                    showDBErrorDialog(ex.getMessage());
                }
            }
        }
    }

    private void maybeInsertEinloesungIntoGutschein(int rechnungsNr) {
        for (KassierArtikel ka : kassierArtikel) {
            if (ka.getType().equals("gutscheineinloesung")) {
                // TODO implement this also for gutscheinNr < 200!!!
                Integer gutscheinNr = ka.getGutscheinNr();
                try {
                    Connection connection = this.pool.getConnection();

                    if (gutscheinNr >= 200) { // TODO ändern, nachdem alle alten Gutscheine (<200) eingelöst worden sind
                        // "normal" new voucher

                        // Because MySQL prevents SELECT subqueries on the same table as INSERTing INTO,
                        // need to perform these two subqueries separately:
    
                        // 1: query for gutschein_in_vd_id
                        PreparedStatement pstmt = connection.prepareStatement(
                            "SELECT gutschein_in_vd_id FROM "+tableForMode("gutschein")+
                            "    WHERE gutschein_nr = ? AND einloesung_in_vd_id IS NULL"
                        );
                        pstmtSetInteger(pstmt, 1, gutscheinNr);
                        ResultSet rs = pstmt.executeQuery();
                        Integer gutschein_in_vd_id = null;
                        if (rs.next()) {
                            gutschein_in_vd_id = rs.getInt(1);
                        }
                        rs.close();
                        if (gutschein_in_vd_id == null) {
                            JOptionPane.showMessageDialog(this, "Fehler: Gutschein Nr. " + gutscheinNr +
                            " konnte nicht als Einlösung in Gutscheintabelle gespeichert werden,\n"+
                            "weil die gutschein_in_vd_id nicht ermittelt werden konnte.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                            pstmt.close();
                            connection.close();
                        } else {
    
                            // 2: query for restbetrag
                            pstmt = connection.prepareStatement(
                                "SELECT (SELECT MIN(restbetrag) FROM "+tableForMode("gutschein")+" "+
                                "    WHERE gutschein_nr = ?) - ?"
                            );
                            pstmtSetInteger(pstmt, 1, gutscheinNr);
                            pstmt.setBigDecimal(2, ka.getEinzelPreis().abs());
                            rs = pstmt.executeQuery();
                            BigDecimal restbetrag = null;
                            if (rs.next()) {
                                restbetrag = rs.getBigDecimal(1);
                            }
                            rs.close();
                            if (restbetrag == null) {
                                JOptionPane.showMessageDialog(this, "Fehler: Gutschein Nr. " + gutscheinNr +
                                " konnte nicht als Einlösung in Gutscheintabelle gespeichert werden,\n"+
                                "weil der Restbetrag nicht ermittelt werden konnte.",
                                "Fehler", JOptionPane.ERROR_MESSAGE);
                                pstmt.close();
                                connection.close();
                            } else {
                                
                                // 3: now, insert into table gutschein
                                pstmt = connection.prepareStatement(
                                    "INSERT INTO "+tableForMode("gutschein")+" SET "+
                                    "gutschein_nr = ?, "+
                                    "datum = (SELECT verkaufsdatum FROM "+tableForMode("verkauf")+" WHERE rechnungs_nr = ?), "+
                                    "gutschein_in_vd_id = ?, "+
                                    "einloesung_in_vd_id = (SELECT vd_id FROM "+tableForMode("verkauf_details")+" "+
                                    "    WHERE rechnungs_nr = ? AND position = ?), "+
                                    "restbetrag = ?"
                                );
                                pstmtSetInteger(pstmt, 1, gutscheinNr);
                                pstmtSetInteger(pstmt, 2, rechnungsNr);
                                pstmtSetInteger(pstmt, 3, gutschein_in_vd_id);
                                pstmtSetInteger(pstmt, 4, rechnungsNr);
                                pstmtSetInteger(pstmt, 5, ka.getPosition());
                                pstmt.setBigDecimal(6, restbetrag);
                                int result = pstmt.executeUpdate();
                                pstmt.close();
                                connection.close();
                                if (result == 0) {
                                    JOptionPane.showMessageDialog(this, "Fehler: Gutschein Nr. " + gutscheinNr +
                                    " konnte nicht als Einlösung in Gutscheintabelle gespeichert werden.",
                                    "Fehler", JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }
                    } else {
                        // old voucher listed on paper

                        // insert into table gutschein
                        PreparedStatement pstmt = connection.prepareStatement(
                            "INSERT INTO "+tableForMode("gutschein")+" SET "+
                            "gutschein_nr = ?, "+
                            "datum = (SELECT verkaufsdatum FROM "+tableForMode("verkauf")+" WHERE rechnungs_nr = ?), "+
                            "gutschein_in_vd_id = NULL, "+
                            "einloesung_in_vd_id = (SELECT vd_id FROM "+tableForMode("verkauf_details")+" "+
                            "    WHERE rechnungs_nr = ? AND position = ?), "+
                            "restbetrag = 0.00"
                        );
                        pstmtSetInteger(pstmt, 1, gutscheinNr);
                        pstmtSetInteger(pstmt, 2, rechnungsNr);
                        pstmtSetInteger(pstmt, 3, rechnungsNr);
                        pstmtSetInteger(pstmt, 4, ka.getPosition());
                        int result = pstmt.executeUpdate();
                        pstmt.close();
                        connection.close();
                        if (result == 0) {
                            JOptionPane.showMessageDialog(this, "Fehler: Gutschein Nr. " + gutscheinNr +
                            " konnte nicht als Einlösung in Gutscheintabelle gespeichert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } catch (SQLException ex) {
                    logger.error("Exception:", ex);
                    showDBErrorDialog(ex.getMessage());
                }
            }
        }
    }

    protected void setButtonsEnabled() {
        if (preisField.getText().length() > 0) {
            hinzufuegenButton.setEnabled(true);
            leergutButton.setEnabled(artikelHasPfand());
            rueckgabeButton.setEnabled(true);
        } else {
            hinzufuegenButton.setEnabled(false);
            leergutButton.setEnabled(false);
            rueckgabeButton.setEnabled(false);
        }
    }

    private boolean checkKundeGibtField() {
        if (kundeGibtField.getDocument().getLength() == 0) {
            BigDecimal price = new BigDecimal(getTotalPrice());
            if (price.compareTo(bc.zero) <= 0) {
                kundeGibtField.setText(bc.priceFormatter(bc.zero));
            } else {
                JOptionPane.showMessageDialog(this, "Bitte Betrag bei 'KUNDE GIBT:' eintragen!",
                        "Fehlender Kundenbetrag", JOptionPane.WARNING_MESSAGE);
                kundeGibtField.requestFocus();
                return false;
            }
        }
        return true;
    }

    private Integer getLastPosition() {
        Integer lastPos = 0;
        for (int i = kassierArtikel.size() - 1; i >= 0; i--) {
            Integer val = kassierArtikel.get(i).getPosition();
            if (val != null) {
                lastPos = val;
                break;
            }
        }
        return lastPos;
    }

    private void updateDisplay(String kurzname, Integer stueck, String artikelPreis) {
        if (display != null && display.deviceWorks()) {
            String zws = totalPriceField.getText();
            display.printArticle(kurzname, stueck, artikelPreis, zws);
            tabbedPane.esWirdKassiert = true;
        }
    }

    private void hinzufuegenRaw(Integer artID, Integer rabID, String kurzname, String artikelNummer,
                                String color, String type, String menge, Integer stueck,
                                BigDecimal einzelPreis, BigDecimal gesPreis, BigDecimal artikelMwSt,
                                Integer gutscheinNr, boolean addPosition, boolean addRemButton,
                                Integer index) {
        if (kassierArtikel.size() == 0) {
            // First item is added, this is a new TSE transaction
            tse.startTransaction();
        }
        
        KassierArtikel ka = new KassierArtikel(bc);
        if (addPosition) {
            Integer lastPos = getLastPosition();
            ka.setPosition(lastPos + 1);
        } else {
            ka.setPosition(null);
        }
        ka.setArtikelID(artID);
        ka.setRabattID(rabID);
        ka.setName(kurzname);
        ka.setColor(color);
        ka.setType(type);
        ka.setMenge(menge);
        ka.setStueckzahl(stueck);
        ka.setEinzelPreis(einzelPreis);
        ka.setGesPreis(gesPreis);
        ka.setMwst(artikelMwSt);
        ka.setGutscheinNr(gutscheinNr);
        if (index == null) {
            kassierArtikel.add(ka);
        } else {
            kassierArtikel.add(index, ka);
        }

        mwsts.add(artikelMwSt);
        if (addRemButton) {
            removeButtons.add(new JButton("-"));
            removeButtons.lastElement().addActionListener(this);
        } else {
            removeButtons.add(null);
        }

        String einzelPreisString = bc.priceFormatter(einzelPreis) + ' ' + bc.currencySymbol;
        String gesPreisString = bc.priceFormatter(gesPreis) + ' ' + bc.currencySymbol;

        Vector<Object> row = new Vector<Object>();
        if (addPosition) {
            row.add(ka.getPosition());
        } else {
            row.add("");
        }
        row.add(kurzname);
        row.add(artikelNummer);
        row.add(stueck.toString());
        row.add(einzelPreisString);
        row.add(gesPreisString);
        row.add(bc.vatFormatter(artikelMwSt));
        if (addRemButton) {
            row.add(removeButtons.lastElement());
        } else {
            row.add("");
        }
        if (index == null) {
            data.add(row);
        } else {
            data.add(index, row);
        }
    }

    private void hinzufuegen(int artID, String kurzname, String artikelNummer, String color, String type, String menge,
                             Integer stueck, BigDecimal artikelPreis, BigDecimal gesPreis, String artikelMwSt,
                             Integer gutscheinNr) {
        hinzufuegenRaw(artID, null, kurzname, artikelNummer, color,
                       type, menge, stueck, artikelPreis, gesPreis,
                       new BigDecimal(artikelMwSt), gutscheinNr, true, true, null);

        if (type.equals("artikel") || type.equals("rueckgabe")) {
            checkForRabatt();
            checkForPfand();
        }
        updateAll();
        updateDisplay(kurzname, stueck, bc.priceFormatter(artikelPreis));
    }

    private void artikelHinzufuegen(Integer stueck, String type, String color) {
        selectedStueck = stueck;
        Artikel a = getArticle(selectedArticleID);
        String artikelNummer = a.getNummer();
        String menge = formatMengeForOutput(a.getMenge(), a.getEinheit());
        BigDecimal artikelPreis = new BigDecimal(bc.priceFormatterIntern(preisField.getText()));
        BigDecimal gesPreis = artikelPreis.multiply(new BigDecimal(stueck));
        String kurzname = getShortName(a);
        String artikelMwSt = getVAT(selectedArticleID);
        hinzufuegen(selectedArticleID, kurzname, artikelNummer, color, type, menge,
                    stueck, artikelPreis, gesPreis, artikelMwSt, null);
    }

    private void artikelNormalHinzufuegen() {
        if (asPanel.artikelBox.getItemCount() != 1 || asPanel.nummerBox.getItemCount() != 1) {
            logger.error("Error: article not selected unambiguously.");
            JOptionPane.showMessageDialog(this, "Fehler: Artikel nicht eindeutig ausgewählt.", "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        Integer stueck = (Integer) anzahlSpinner.getValue();
        Artikel a = getArticle(selectedArticleID);
        Boolean sortiment = a.getSortiment();
        String color = sortiment ? "default" : "gray";
        artikelHinzufuegen(stueck, "artikel", color);
    }

    private void artikelRueckgabeHinzufuegen() {
        Integer stueck = -(Integer) anzahlSpinner.getValue();
        artikelHinzufuegen(stueck, "rueckgabe", "green");
    }

    private void leergutHinzufuegen() {
        int pfandArtikelID = queryPfandArtikelID(selectedArticleID);
        // gab es Pfand? Wenn ja, fuege Zeile in Tabelle:
        if (pfandArtikelID > 0) {
            Integer stueck = -(Integer) anzahlSpinner.getValue();
            selectedStueck = stueck;
            BigDecimal pfand = new BigDecimal(getSalePrice(pfandArtikelID));
            String pfandName = getArticleName(pfandArtikelID)[0];
            BigDecimal gesamtPfand = pfand.multiply(new BigDecimal(stueck));
            String pfandMwSt = getVAT(selectedArticleID);
            hinzufuegen(pfandArtikelID, pfandName, "LEERGUT", "green", "leergut", "",
                        stueck, pfand, gesamtPfand, pfandMwSt, null);
        }
    }

    private void anzahlungHinzufuegen() {
        Integer stueck = 1;
        selectedStueck = stueck;
        String anzahlungName = getArticleName(anzahlungArtikelID)[0];
        BigDecimal gesamtAnzahlung = new BigDecimal(bc.priceFormatterIntern(anzahlungsBetragField.getText()));
        // discover all the contained VATs and split the Anzahlung according to contribution to total price
        BigDecimal gesUmsatz = new BigDecimal(getTotalPrice());
        // vatMap = calculateMwStValuesInRechnung(); // should have been calculated before
        for ( BigDecimal steuersatz : vatMap.keySet() ){
            BigDecimal brutto = vatMap.get(steuersatz).get(2); // = Umsatz
            // Anteil des Steuersatzes am Gesamtumsatz:
            BigDecimal anzahlungsWert = brutto.divide(gesUmsatz, 10, RoundingMode.HALF_UP).multiply(gesamtAnzahlung);
            String mwst = steuersatz.toString();
            hinzufuegen(anzahlungArtikelID, anzahlungName, "ANZAHLUNG", "red", "anzahlung", "",
                        stueck, anzahlungsWert, anzahlungsWert, mwst, null);
        }
        // now zero all prices except the anzahlung
        for (int i = 0; i < kassierArtikel.size(); i++) {
            if (!kassierArtikel.get(i).getType().equals("anzahlung")) {
                // kassierArtikel.get(i).setEinzelpreis(bc.zero);
                kassierArtikel.get(i).setGesPreis(bc.zero);
                kassierArtikel.get(i).setPartOfAnzahlung(true);
                // data.get(i).set(4, ""); // column Einzelpreis
                data.get(i).set(5, ""); // column Gesamtpreis
                updateAll();
            }
        }
        updateDisplay(anzahlungName, stueck, bc.priceFormatter(gesamtAnzahlung));
    }

    private Integer getMaxGutscheinNr() {
        Integer maxGutscheinNr = null;
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT IFNULL(MAX(gutschein_nr), 199) FROM gutschein WHERE gutschein_in_vd_id IS NOT NULL"
            );
            if (rs.next()) {
                maxGutscheinNr = rs.getInt(1);
                for (KassierArtikel ka : kassierArtikel) {
                    logger.debug(ka.getGutscheinNr());
                    if (ka.getGutscheinNr() != null && ka.getGutscheinNr() > maxGutscheinNr) {
                        maxGutscheinNr = ka.getGutscheinNr();
                    }
                }
            }
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return maxGutscheinNr;
    }

    private void gutscheineHinzufuegen() {
        Integer nGutscheine = (Integer) anzahlSpinner.getValue();
        Integer maxGutscheinNr = getMaxGutscheinNr();
        BigDecimal gutscheinWert = new BigDecimal(bc.priceFormatterIntern(preisField.getText()));
        if (maxGutscheinNr != null) {
            for (int i=0; i<nGutscheine; i++) {
                // fuege nGutscheine separate Gutscheine hinzu, jeder mit distinkter gutscheinNr
                gutscheinHinzufuegen(maxGutscheinNr+i+1, gutscheinWert);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Fehler: Gutschein-Nr. kann nicht bestimmt werden.",
            "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void gutscheinHinzufuegen(int gutscheinNr, BigDecimal gutscheinWert) {
        Integer stueck = 1;
        selectedStueck = stueck;
        String gutscheinName = "Gutschein Nr. "+gutscheinNr;
        String mwst = getVAT(gutscheinArtikelID);
        hinzufuegen(gutscheinArtikelID, gutscheinName, "GUTSCHEIN", "green", "gutschein", "",
                    stueck, gutscheinWert, gutscheinWert, mwst, gutscheinNr);
    }

    private void gutscheinEinloesungHinzufuegen(int gutscheinNr, BigDecimal einloesWert) {
        Integer stueck = 1;
        selectedStueck = stueck;
        String gutscheinName = "Einlösung Gutschein Nr. "+gutscheinNr;
        einloesWert = einloesWert.abs().multiply(bc.minusOne);
        String mwst = getVAT(gutscheineinloesungArtikelID);
        hinzufuegen(gutscheineinloesungArtikelID, gutscheinName, "GUTSCHEIN", "green", "gutscheineinloesung", "",
                    stueck, einloesWert, einloesWert, mwst, gutscheinNr);
    }

    private void zwischensumme() {
        bigPriceField.setText(totalPriceField.getText());
        updateRabattButtonsZwischensumme();

        barButton.setEnabled(true);
        ecButton.setEnabled(true);
        stornoButton.setEnabled(true);

        if (display != null && display.deviceWorks()) {
            display.printZWS(bigPriceField.getText());
        }

        if (zahlungsModus.equals("bar")) {
            bar();
        } else if (zahlungsModus.equals("ec")) {
            ec();
        }
    }

    private void bar() {
        zahlungsModus = "bar";
        zahlungsLabel.setText("Bar-Zahlung. Bitte jetzt abrechnen.");
        kundeGibtField.setEditable(true);
        passendButton.setEnabled(true);
        // neuerKundeButton.setEnabled(false);
        neuerKundeButton.setEnabled(true);
        kundeGibtField.requestFocus();
    }

    private void ec() {
        zahlungsModus = "ec";
        zahlungsLabel.setText("EC-Zahlung. Bitte jetzt EC-Gerät bedienen.");
        kundeGibtField.setText("");
        kundeGibtField.setEditable(false);
        passendButton.setEnabled(false);
        neuerKundeButton.setEnabled(true);
        neuerKundeButton.requestFocus();
    }

    private void gutscheinEinloesen() {
        selectedArticleID = gutscheineinloesungArtikelID; // internal Gutscheineinlösung artikel_id
        JDialog dialog = new JDialog(this.mainWindow, "Bitte Nr. des einzulösenden Gutscheins eingeben", true);
        GutscheinEinloesenDialog ged = new GutscheinEinloesenDialog(this.pool, this.mainWindow, dialog);
        dialog.getContentPane().add(ged, BorderLayout.CENTER);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        boolean aborted = ged.getAborted();
        if (!aborted) {
            Integer gutscheinNr = ged.getGutscheinNr();
            BigDecimal einloesWert = ged.getEinloesWert();
            logger.debug(gutscheinNr);
            logger.debug(einloesWert);
            // TODO ändern, nachdem alle alten Gutscheine (<200) eingelöst worden sind
            if (gutscheinNr < 200) {
                selectedArticleID = variablerPreis19PZArtikelID; // a workaround because "legacy" vouchers (2015 - 2021-03-31) use to be booked with 19% VAT
                anzahlSpinner.setValue(1);
                preisField.setText(bc.priceFormatter(einloesWert));
                artikelRueckgabeHinzufuegen();
                // hack to make the article "appear" to be a voucher:
                kassierArtikel.get(kassierArtikel.size() - 1).setName("Gutscheineinlösung");
                kassierArtikel.get(kassierArtikel.size() - 1).setType("gutscheineinloesung");
                kassierArtikel.get(kassierArtikel.size() - 1).setGutscheinNr(gutscheinNr);
                data.get(data.size() - 1).set(1, "Gutscheineinlösung");
                data.get(data.size() - 1).set(2, "GUTSCHEINEINLOES");
            } else {
                gutscheinEinloesungHinzufuegen(gutscheinNr, einloesWert);
            }
            zwischensumme();
        }
    }

    private BigDecimal queryGutscheinRestbetrag(int gutscheinNr) {
        BigDecimal restbetrag = null;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT MIN(restbetrag) FROM gutschein WHERE gutschein_nr = ?"
            );
            pstmtSetInteger(pstmt, 1, gutscheinNr);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); restbetrag = rs.getBigDecimal(1); rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return restbetrag;
    }

    private int neuerKunde() {
        // Send data to TSE:
        Vector<String> zahlung = new Vector<String>();
        zahlung.add(kundeGibtField.isEditable() ? "Bar" : "Unbar");
        zahlung.add( bc.priceFormatterIntern(getTotalPrice()) );
        // Omit currency code because it's always in EUR
        Vector<Vector<String>> zahlungen = new Vector<Vector<String>>();
        zahlungen.add(zahlung);
        HashMap<Integer, Vector<BigDecimal>> mwstIDsAndValues = getAllCurrentMwstValuesByID();

        int rechnungsNr = -1;
        boolean ec = true;
        BigDecimal kundeGibt = null;
        if (kundeGibtField.isEditable()) { // if Barzahlung
            ec = false;
            kundeGibt = new BigDecimal(getKundeGibt());
        }
        rechnungsNr = insertIntoVerkauf(ec, kundeGibt);
        if (rechnungsNr < 0){
            return rechnungsNr;
        }
        
        /* Zitat DSFinV-K: (S. 109) "Für jeden Steuersatz werden hier die Bruttoumsätze je Steuersatz [...] aufgelistet." */
        // Bruttoumsatz = 4. Element in mwstIDsAndValues (get(3))
        TSETransaction tx = null;
        LinkedHashMap<String, String> tseStatusValues = null;
        // always finish the transaction, also when TSE is not in use (has failed), in which case end date is determined by Kasse
        tx = tse.finishTransaction(
            rechnungsNr,
            mwstIDsAndValues.get(3) != null ? mwstIDsAndValues.get(3).get(3) : null, // steuer_allgemein = mwst_id: 3 = 19% MwSt
            mwstIDsAndValues.get(2) != null ? mwstIDsAndValues.get(2).get(3) : null, // steuer_ermaessigt = mwst_id: 2 = 7% MwSt
            mwstIDsAndValues.get(5) != null ? mwstIDsAndValues.get(5).get(3) : null, // steuer_durchschnitt_nr3 = mwst_id: 5 = 10,7% MwSt
            mwstIDsAndValues.get(4) != null ? mwstIDsAndValues.get(4).get(3) : null, // steuer_durchschnitt_nr1 = mwst_id: 4 = 5,5% MwSt
            mwstIDsAndValues.get(1) != null ? mwstIDsAndValues.get(1).get(3) : null, // steuer_null = mwst_id: 1 = 0% MwSt
            zahlungen
        );
        if (tse.inUse()) { // only if TSE is operational, tseStatusValues is not null
            tseStatusValues = tse.getTSEStatusValues();
        }

        if (ec == false) { // if Barzahlung
            insertIntoKassenstand(rechnungsNr);
            printQuittung(rechnungsNr, tx, tseStatusValues); // always print receipt, it's not optional anymore
        } else { // EC-Zahlung
            printQuittung(rechnungsNr, tx, tseStatusValues);
            // Thread.sleep(5000); // wait for 5 seconds, no, printer is too slow anyway and this blocks UI unnecessarily
            printQuittung(rechnungsNr, tx, tseStatusValues);
        }
        maybeInsertIntoAnzahlung(rechnungsNr);
        maybeInsertAufloesungIntoAnzahlung(rechnungsNr);
        maybeInsertIntoGutschein(rechnungsNr);
        maybeInsertEinloesungIntoGutschein(rechnungsNr);
        clearAll();
        updateAll();

        tabbedPane.esWirdKassiert = false;
        if (mw != null) {
            mw.setDisplayWelcomeTimer();
            mw.setDisplayBlankTimer();
        }
        return rechnungsNr;
    }

    private void stornieren() {
        clearAll();
        updateAll();
        if (display != null && display.deviceWorks()) {
            display.showWelcomeScreen();
        }
        tabbedPane.esWirdKassiert = false;
        if (mw != null) {
            mw.setDisplayBlankTimer();
        }
        // Cancel TSE transaction that was on-going:
        tse.cancelTransaction();
    }

    private void artikelRabattierenRelativ(BigDecimal rabattRelativ) {
        // Get data:
        int i = kassierArtikel.size() - 1;
        while (!kassierArtikel.get(i).getType().equals("artikel")) {
            i--;
        }
        // Now i points to the Artikel that gets the Rabatt
        BigDecimal einzelPreis = kassierArtikel.get(i).getEinzelPreis();
        BigDecimal gesPreis = kassierArtikel.get(i).getGesPreis();
        BigDecimal einzelReduktion = new BigDecimal(bc.priceFormatterIntern(
            rabattRelativ.multiply(einzelPreis).multiply(bc.minusOne)
        ));
        BigDecimal gesReduktion = new BigDecimal(bc.priceFormatterIntern(
            rabattRelativ.multiply(gesPreis).multiply(bc.minusOne)
        ));
        BigDecimal artikelMwSt = kassierArtikel.get(i).getMwst();
        String rabattName = getArticleName(artikelRabattArtikelID)[0];

        hinzufuegenRaw(artikelRabattArtikelID, null, einrueckung + rabattName,
                       "RABATT", "red", "rabatt", "", selectedStueck,
                       einzelReduktion, gesReduktion, artikelMwSt, null,
                       false, false, i + 1);

        updateAll();
    }

    private void artikelRabattierenAbsolut(BigDecimal rabattAbsolut) {
        // is this Rabatt or Aufschlag? (rabattAbsolut > 0 or < 0?)
        int specialArticleID = rabattAbsolut.signum() > 0 ? artikelRabattArtikelID : preisanpassungArtikelID;
        
        // Get data
        int i = kassierArtikel.size() - 1;
        while (!kassierArtikel.get(i).getType().equals("artikel")) {
            i--;
        }
        // Now i points to the Artikel that gets the Rabatt
        BigDecimal einzelReduktion = rabattAbsolut.multiply(bc.minusOne);
        BigDecimal stueck = new BigDecimal(selectedStueck);
        BigDecimal gesReduktion = einzelReduktion.multiply(stueck);
        BigDecimal artikelMwSt = kassierArtikel.get(i).getMwst();
        String rabattName = getArticleName(specialArticleID)[0];

        hinzufuegenRaw(specialArticleID, null, einrueckung + rabattName,
                       rabattAbsolut.signum() > 0 ? "RABATT" : "ANPASSUNG",
                       "red", "rabatt", "", selectedStueck,
                       einzelReduktion, gesReduktion, artikelMwSt, null,
                       false, false, i + 1);

        updateAll();
    }

    private void addToHashMap(HashMap<BigDecimal, BigDecimal> hashMap, int artikelIndex) {
        BigDecimal mwst = kassierArtikel.get(artikelIndex).getMwst();
        BigDecimal gesPreis = kassierArtikel.get(artikelIndex).getGesPreis();
        boolean found = false;
        for (Map.Entry<BigDecimal, BigDecimal> entry : hashMap.entrySet()) {
            if (entry.getKey().compareTo(mwst) == 0) {
                entry.setValue(entry.getValue().add(gesPreis));
                found = true;
                break;
            }
        }
        if (!found) { // make new entry
            hashMap.put(mwst, gesPreis);
        }
    }

    private void rechnungRabattierenRelativ(BigDecimal rabattRelativ) {
        int artikelIndex = -1;
        int rabattCounter = -1;
        HashMap<BigDecimal, BigDecimal> rabattArtikelPreise = new HashMap<BigDecimal, BigDecimal>();

        // determine place to start (after last "Rabatt auf Rechnung")
        int startIndex = 0;
        for (int i = kassierArtikel.size() - 1; i >= 0; i--) {
            if (kassierArtikel.get(i).getType().equals("rabattrechnung")) {
                startIndex = i + 1;
                break;
            }
        }

        // scan through artikel list to search for artikels without rabatt
        for (int i = startIndex; i < kassierArtikel.size(); i++) {
            if (kassierArtikel.get(i).getType().equals("artikel")) {
                if (rabattCounter == 0) { // previous artikel had no rabatt
                    addToHashMap(rabattArtikelPreise, artikelIndex);
                }
                artikelIndex = i;
                rabattCounter = 0;
            } else if (kassierArtikel.get(i).getType().equals("rabatt")) {
                rabattCounter++;
            }
        }
        if (rabattCounter == 0) { // for last artikel
            addToHashMap(rabattArtikelPreise, artikelIndex);
        }
        // for ( Map.Entry<BigDecimal, BigDecimal> entry :
        // rabattArtikelPreise.entrySet() ){
        // System.out.println(entry.getKey() + " " + entry.getValue());
        // }

        String rabattName = getArticleName(rechnungRabattArtikelID)[0];

        // Loop over the VAT levels:
        for (Map.Entry<BigDecimal, BigDecimal> entry : rabattArtikelPreise.entrySet()) {
            BigDecimal reduktion = new BigDecimal(
                    bc.priceFormatterIntern(rabattRelativ.multiply(entry.getValue()).multiply(bc.minusOne)));
            BigDecimal mwst = entry.getKey();

            hinzufuegenRaw(rechnungRabattArtikelID, null, rabattName,
                "RABATT", "red", "rabattrechnung", "", 1,
                reduktion, reduktion, mwst, null,
                false, true, null);

            // updateAll fuer Arme
            this.remove(allPanel);
            this.revalidate();
            showAll();

            zwischensumme();
        }
    }

    private void removeRabattAufRechnung() {
        for (int i = kassierArtikel.size() - 1; i >= 0; i--) {
            if (!kassierArtikel.get(i).getType().equals("rabattrechnung")) {
                break; // stop at first row that is no "Rabatt auf Rechnung"
            } else { // remove the rabatt row
                data.remove(i);
                kassierArtikel.remove(i);
                mwsts.remove(i);
                removeButtons.remove(i);
            }
        }
    }

    /**
     * * Each non abstract class that implements the DocumentListener must have
     * these methods.
     *
     * @param e
     *            the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
        if (e.getDocument() == preisField.getDocument()) {
            setButtonsEnabled();
            return;
        }
    }

    public void removeUpdate(DocumentEvent e) {
        insertUpdate(e);
    }

    public void changedUpdate(DocumentEvent e) {
        // Plain text components do not fire these events
    }

    void refreshPositionsInData() {
        for (int i = 0; i < kassierArtikel.size(); i++) {
            data.get(i).set(0, kassierArtikel.get(i).getPosition());
        }
    }

    void reconstructPrices() {
        for (int i = 0; i < kassierArtikel.size(); i++) {
            KassierArtikel ka = kassierArtikel.get(i);
            BigDecimal gesPreis = ka.getEinzelPreis().multiply(new BigDecimal(ka.getStueckzahl()));
            ka.setGesPreis(gesPreis);
            data.get(i).set(5, bc.priceFormatter(gesPreis));
        }
    }

    /**
     * A class implementing ArticleSelectUser must have this method.
     */
    public void updateSelectedArticleID(int selectedArticleID) {
        this.selectedArticleID = selectedArticleID;
    }

    void printQuittung(Integer rechnungsNr, TSETransaction tx, LinkedHashMap<String, String> tseStatusValues) {
        TreeMap<BigDecimal, Vector<BigDecimal>> mwstValues = calculateMwStValuesInRechnung();
        BigDecimal totalPrice = new BigDecimal(getTotalPrice());
        BigDecimal kundeGibt = null, rueckgeld = null;
        if (kundeGibtField.getDocument().getLength() > 0) {
            kundeGibt = new BigDecimal(getKundeGibt());
            rueckgeld = kundeGibt.subtract(totalPrice);
        }
        if (rechnungsNr == null) {
            rechnungsNr = maxRechnungsNr() + 1;
        }
        Quittung myQuittung = new Quittung(this.pool, this.mainWindow,
            new DateTime(now()), rechnungsNr, null,
            kassierArtikel, mwstValues, zahlungsModus,
            totalPrice, kundeGibt, rueckgeld,
            tx, bc.Z_KASSE_ID, tseStatusValues);
        myQuittung.printReceipt();
    }

    private void setSelectedArticle(int id) {
        Artikel a = getArticle(id);
        String[] nummer = new String[]{a.getNummer()};
        String vkPreis = a.getVKP() != null ? a.getVKP() : "";
        if (!vkPreis.equals("")){
            vkPreis = bc.priceFormatter(vkPreis)+" "+bc.currencySymbol;
        }
        String[] name = new String[]{a.getName(),
            getShortLieferantName(id),
            vkPreis,
            a.getSortiment().toString(),
            a.getLiefID().toString()};
        asPanel.nummerBox.setBox(nummer);
        asPanel.artikelBox.setBox(name);
        asPanel.checkIfFormIsComplete();
    }

    void unsetFields() {
        individuellRabattRelativField.setText("");
        individuellRabattAbsolutField.setText("");
        abweichenderPreisField.setText("");
    }

    /**
     * * Each non abstract class that implements the ActionListener must have
     * this method.
     *
     * @param e
     *            the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sonstigesButton) {
            asPanel.artikelField.setText("");
            asPanel.artikelField.setText(", Sonstige");
            asPanel.artikelField.requestFocus();
            return;
        }
        if (e.getSource() == gutscheinVerkaufenButton) {
            setSelectedArticle(gutscheinArtikelID); // Variabler Preis 7%
            return;
        }
        if (e.getSource() == sevenPercentButton) {
            setSelectedArticle(variablerPreis7PZArtikelID); // Variabler Preis 7%
            return;
        }
        if (e.getSource() == nineteenPercentButton) {
            setSelectedArticle(variablerPreis19PZArtikelID); // Variabler Preis 19%
            return;
        }
        if (e.getSource() == hinzufuegenButton) {
            removeRabattAufRechnung();
            if (selectedArticleID == gutscheinArtikelID) {
                gutscheineHinzufuegen();
            } else {
                artikelNormalHinzufuegen();
            }
            return;
        }
        if (e.getSource() == leergutButton) {
            removeRabattAufRechnung();
            leergutHinzufuegen();
            return;
        }
        if (e.getSource() == rueckgabeButton) {
            removeRabattAufRechnung();
            if (selectedArticleID == gutscheinArtikelID) {
                String gutscheinNrStr = JOptionPane.showInputDialog(this, "Bitte Gutschein-Nr. eingeben: ");
                Integer gutscheinNr = null;
                try {
                    gutscheinNr = Integer.parseInt(gutscheinNrStr);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this,
                        "Fehlerhafte Eingabe.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                }
                if (gutscheinNr != null) {
                    if (gutscheinNr < 200) { // TODO ändern, nachdem alle alten Gutscheine (<200) eingelöst worden sind
                        JOptionPane.showMessageDialog(this,
                            "Es können nur Gutscheine mit Nr. unter 200 zurückgegeben werden.\n"+
                            "Gutscheine mit Nr. unter 200 bitte einlösen.",
                            "Info", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        BigDecimal restbetrag = queryGutscheinRestbetrag(gutscheinNr);
                        if (restbetrag == null) {
                            JOptionPane.showMessageDialog(this,
                                "Gutschein wurde nicht gefunden.",
                                "Info", JOptionPane.INFORMATION_MESSAGE);
                        } else if (restbetrag.compareTo(bc.zero) <= 0) {
                            JOptionPane.showMessageDialog(this,
                                "Dieser Gutschein wurde bereits vollständig eingelöst.",
                                "Info", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            gutscheinEinloesungHinzufuegen(gutscheinNr, restbetrag);
                        }
                    }
                }
            } else {
                artikelRueckgabeHinzufuegen();
            }
            return;
        }
        if (e.getSource() == zwischensummeButton) {
            zwischensumme();
            return;
        }
        if (e.getSource() == barButton) {
            bar();
            return;
        }
        if (e.getSource() == ecButton) {
            if ((new BigDecimal(getTotalPrice())).compareTo(ecSchwelle) < 0) {
                int answer = JOptionPane.showConfirmDialog(this,
                    "ACHTUNG: Gesamtbetrag unter " + bc.priceFormatter(ecSchwelle) + " " + bc.currencySymbol + " !\n" +
                    "Wirklich EC-Zahlung erlauben?",
                    "Warnung", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (answer == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            ec();
            return;
        }
        if (e.getSource() == passendButton) {
            kundeGibtField.setText( bc.priceFormatter(getTotalPrice()) );
            return;
        }
        if (e.getSource() == gutscheinEinloesenButton) {
            gutscheinEinloesen();
            return;
        }
        if (e.getSource() == stornoButton) {
            int answer = JOptionPane.showConfirmDialog(this,
                "Wirklich stornieren?", "Storno",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                stornieren();
            }
            return;
        }
        if (e.getSource() == neuerKundeButton) {
            if (zahlungsModus.equals("bar")) {
                boolean ok = checkKundeGibtField();
                if (!ok) {
                    return;
                }
            }
            int rechnungsNr = neuerKunde();
            if (rechnungsNr > 0) {
                tabbedPane.recreateTabbedPane();
            }
            return;
        }
        if (e.getSource() == mitarbeiterRabattButton) {
            rechnungRabattierenRelativ(mitarbeiterRabatt);
            return;
        }
        int rabattIndex = -1;
        for (int i = 0; i < rabattButtons.size(); i++) {
            if (e.getSource() == rabattButtons.get(i)) {
                rabattIndex = i;
                break;
            }
        }
        if (rabattIndex > -1) {
            BigDecimal rabattAnteil = new BigDecimal(bc.vatParser(rabattButtons.get(rabattIndex).getText()));
            if (barButton.isEnabled()) {
                rechnungRabattierenRelativ(rabattAnteil);
            } else {
                artikelRabattierenRelativ(rabattAnteil);
            }
            return;
        }
        if (e.getSource() == individuellRabattRelativButton) {
            BigDecimal rabattAnteil = new BigDecimal(bc.vatParser(individuellRabattRelativField.getText()));
            if (barButton.isEnabled()) {
                rechnungRabattierenRelativ(rabattAnteil);
            } else {
                artikelRabattierenRelativ(rabattAnteil);
            }
            unsetFields();
            return;
        }
        if (e.getSource() == individuellRabattAbsolutButton) {
            BigDecimal rabatt = new BigDecimal(bc.priceFormatterIntern(individuellRabattAbsolutField.getText()));
            if (barButton.isEnabled()) {
                // do nothing
            } else {
                artikelRabattierenAbsolut(rabatt);
            }
            unsetFields();
            return;
        }
        if (e.getSource() == abweichenderPreisButton) {
            int i = getLastArticleIndex();
            BigDecimal neuerEinzelPreis = new BigDecimal(bc.priceFormatterIntern(abweichenderPreisField.getText()));
            BigDecimal alterEinzelPreis = kassierArtikel.get(i).getEinzelPreis();
            BigDecimal rabatt = alterEinzelPreis.subtract(neuerEinzelPreis);
            artikelRabattierenAbsolut(rabatt);
            unsetFields();
            return;
        }
        if (e.getSource() == anzahlungNeuButton) {
            showAnzahlungNeuPanel();
        }
        if (e.getSource() == anzahlungNeuOKButton) {
            anzahlungHinzufuegen();
            zwischensumme();
        }
        if (e.getSource() == anzahlungAufloesButton) {
            JDialog dialog = new JDialog(this.mainWindow, "Anzahlung auflösen", true);
            AnzahlungAufloesDialog aad = new AnzahlungAufloesDialog(this.pool, this.mainWindow, dialog);
            dialog.getContentPane().add(aad, BorderLayout.CENTER);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            boolean aborted = aad.getAborted();
            if (!aborted) {
                int anzahlungRechNr = aad.getSelectedRechNr();
                Vector<KassierArtikel> kas = aad.getKassierArtikel();
                for (KassierArtikel ka : kas) {
                    boolean addPos = true, addRemButton = true;
                    switch (ka.getType()) {
                        case "rabatt": addPos = false; addRemButton = false; break;
                        case "rabattrechnung": addPos = false; addRemButton = true; break;
                        case "pfand": addPos = false; addRemButton = false; break;
                        default: addPos = true; addRemButton = true; break;
                    }
                    hinzufuegenRaw(ka.getArtikelID(), ka.getRabattID(), ka.getName(),
                                   ka.getArtikelNummer(), ka.getColor(), ka.getType(),
                                   ka.getMenge(), ka.getStueckzahl(), ka.getEinzelPreis(),
                                   ka.getGesPreis(), ka.getMwst(), ka.getGutscheinNr(),
                                   addPos, addRemButton, null);
                    if (ka.getType().equals("anzahlungsaufloesung")) {
                        // store the anzahlungRechNr with the KassierArtikel;
                        kassierArtikel.get(kassierArtikel.size() - 1).setAnzahlungRechNr(anzahlungRechNr);
                    }
                    updateAll();
                    updateDisplay(ka.getName(), ka.getStueckzahl(), bc.priceFormatter(ka.getEinzelPreis()));
                    zwischensumme();
                }
            }
        }
        int removeRow = -1;
        for (int i = 0; i < removeButtons.size(); i++) {
            if (e.getSource() == removeButtons.get(i)) {
                removeRow = i;
                break;
            }
        }
        if (removeRow > -1) {
            if (kassierArtikel.get(removeRow).getType() == "anzahlung")
                reconstructPrices();

            data.remove(removeRow);
            kassierArtikel.remove(removeRow);
            mwsts.remove(removeRow);
            removeButtons.remove(removeRow);

            // remove extra rows (Rabatt oder Pfand):
            while (removeRow < kassierArtikel.size() && removeButtons.get(removeRow) == null) {
                data.remove(removeRow);
                kassierArtikel.remove(removeRow);
                mwsts.remove(removeRow);
                removeButtons.remove(removeRow);
            }

            for (int i = removeRow; i < kassierArtikel.size(); i++) {
                Integer oldVal = kassierArtikel.get(i).getPosition();
                if (oldVal != null) {
                    kassierArtikel.get(i).setPosition(oldVal - 1);
                }
            }
            removeRabattAufRechnung();
            refreshPositionsInData();
            updateAll();
            if (display != null && display.deviceWorks()) {
                display.clearScreen();
            }
            if (kassierArtikel.size() == 0) {
                // Last item was removed, cancel the TSE transaction
                tse.cancelTransaction();
            }
            return;
        }
    }

}
