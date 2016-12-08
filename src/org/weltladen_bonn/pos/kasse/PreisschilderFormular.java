package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.Vector;
import java.io.*; // for File
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// GUI stuff:
// for BorderLayout, FlowLayout, Dimension:
import java.awt.*;
// ActionEvent, KeyAdapter, ...
import java.awt.event.*;

// MySQL Connector/J stuff:
// Connection, Statement, ResultSet
import java.sql.*;

// JFrame, JPanel, JTable, JButton etc.
import javax.swing.*;
// ChangeEvent, DocumentEvent, ...
import javax.swing.event.*;
import javax.swing.table.TableColumn;
// for DocumentFilter
import javax.swing.text.AbstractDocument;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.weltladen_bonn.pos.*;

public class PreisschilderFormular extends ArtikelGrundlage implements ArticleSelectUser, DocumentListener {
    /**
     * Form to select articles for price tag printing
     */
    private static final long serialVersionUID = 1L;

    // Text Fields
    private int selectedArticleID;

    private ArticleSelectPanelPreisschilder asPanel;
    protected JTextField vkPreisField;
    protected JTextField empfVkPreisField;
    protected JTextField kurznameField;
    protected JTextField mengeField;
    protected JTextField einheitField;
    protected JTextField herkunftField;
    protected JCheckBox sortimentBox;

    // Buttons
    private JButton hinzufuegenButton;
    private JButton csvListReadinButton;
    private JFileChooser csvChooser;
    private Vector<JButton> removeButtons;
    private Vector<JButton> editButtons;
    private JButton deleteButton;
    private JButton printButton;
    private JRadioButton lmButton;
    private JRadioButton khwButton;

    // The panels
    private JPanel allPanel;
    private JPanel articleListPanel;

    // The table holding the purchase articles.
    private ArticleSelectTable myTable;
    private JScrollPane scrollPane;
    private Vector<String> columnLabels;
    private Vector<Vector<Object>> data;

    //private Vector<Integer> artikelIDs;
    private Vector<String> articleNumbers;
    private Vector<String> lieferanten;
    private Vector<String> articleNames;
    private Vector<String> kurzlieferanten;
    private Vector<String> kurznamen;
    private Vector<String> mengen;
    private Vector<String> preise;
    private Vector<String> kgPreise;
    private Vector<String> einheiten;
    private Vector<String> herkuenfte;
    protected Vector<String> colors;
    private Vector<String> types;

    // Methoden:

    /**
     * The constructor.
     */
    public PreisschilderFormular(Connection conn, MainWindowGrundlage mw, TabbedPaneGrundlage tp) {
        super(conn, mw);
        initiateVectors();

        csvChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "CSV Text-Dokumente", "csv");
        csvChooser.setFileFilter(filter);

        emptyTable();
        showAll();
        asPanel.emptyBarcodeBox();
    }

    private void initiateVectors() {
        columnLabels = new Vector<String>();
        columnLabels.add("Lieferant");
        columnLabels.add("Artikel-Nr.");
        columnLabels.add("Name");
        columnLabels.add("Preis");
        columnLabels.add("Menge");
        columnLabels.add("Preis pro kg/l/St.");
        columnLabels.add("Einheit");
        columnLabels.add("Herkunft");
        columnLabels.add("MwSt.");
        columnLabels.add("Entfernen");
        columnLabels.add("Ändern");
        colors = new Vector<String>();
        types = new Vector<String>();
    }

    private class EnterAdapter extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (hinzufuegenButton.isEnabled()) {
                    hinzufuegenButton.doClick();
                }
            }
        }
    }

    void showAll() {
        allPanel = new JPanel(new BorderLayout());

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.PAGE_AXIS));

        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        asPanel = new ArticleSelectPanelPreisschilder(conn, mainWindow, this);
        formPanel.add(asPanel);

        EnterAdapter enterAdapter = new EnterAdapter();

        JPanel propertiesPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 3, 3, 3);

        vkPreisField = new JTextField("");
        vkPreisField.addKeyListener(enterAdapter);
        vkPreisField.getDocument().addDocumentListener(this);
        ((AbstractDocument) vkPreisField.getDocument()).setDocumentFilter(bc.geldFilter);
        vkPreisField.setEditable(false);
        vkPreisField.setColumns(6);
        vkPreisField.setHorizontalAlignment(JTextField.RIGHT);
        removeDefaultKeyBindings(vkPreisField);

        empfVkPreisField = new JTextField("");
        empfVkPreisField.addKeyListener(enterAdapter);
        empfVkPreisField.getDocument().addDocumentListener(this);
        ((AbstractDocument) empfVkPreisField.getDocument()).setDocumentFilter(bc.geldFilter);
        empfVkPreisField.setEditable(false);
        empfVkPreisField.setColumns(6);
        empfVkPreisField.setHorizontalAlignment(JTextField.RIGHT);
        removeDefaultKeyBindings(empfVkPreisField);

        kurznameField = new JTextField("");
        kurznameField.addKeyListener(enterAdapter);
        kurznameField.getDocument().addDocumentListener(this);
        ((AbstractDocument) kurznameField.getDocument()).setDocumentFilter(bc.kurznameFilter);
        kurznameField.setEditable(false);
        kurznameField.setColumns(20);
        removeDefaultKeyBindings(kurznameField);

        mengeField = new JTextField("");
        mengeField.addKeyListener(enterAdapter);
        mengeField.getDocument().addDocumentListener(this);
        ((AbstractDocument) mengeField.getDocument()).setDocumentFilter(bc.mengeFilter);
        mengeField.setEditable(false);
        mengeField.setColumns(10);
        mengeField.setHorizontalAlignment(JTextField.RIGHT);
        removeDefaultKeyBindings(mengeField);

        einheitField = new JTextField("");
        einheitField.addKeyListener(enterAdapter);
        einheitField.getDocument().addDocumentListener(this);
        ((AbstractDocument) einheitField.getDocument()).setDocumentFilter(bc.einheitFilter);
        einheitField.setEditable(false);
        einheitField.setColumns(10);
        einheitField.setHorizontalAlignment(JTextField.RIGHT);
        removeDefaultKeyBindings(einheitField);

        herkunftField = new JTextField("");
        herkunftField.addKeyListener(enterAdapter);
        herkunftField.getDocument().addDocumentListener(this);
        ((AbstractDocument) herkunftField.getDocument()).setDocumentFilter(bc.herkunftFilter);
        herkunftField.setEditable(false);
        herkunftField.setColumns(20);
        removeDefaultKeyBindings(herkunftField);

        sortimentBox = new JCheckBox("Sortiment");
        sortimentBox.setEnabled(false);
        sortimentBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                if (sortimentBox.isSelected()) {
                    sortimentBox.setForeground(Color.BLACK);
                } else {
                    sortimentBox.setForeground(Color.RED.darker());
                }
            }
        });

        c.anchor = GridBagConstraints.EAST;
        c.gridy = 0;
        c.gridx = 0;
        propertiesPanel.add(new JLabel("VK-Preis: (pro Artikel)"), c);
        c.anchor = GridBagConstraints.EAST;
        c.gridy = 0;
        c.gridx = 1;
        propertiesPanel.add(vkPreisField, c);
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0;
        c.gridx = 2;
        propertiesPanel.add(new JLabel(bc.currencySymbol), c);
        c.anchor = GridBagConstraints.CENTER;
        c.gridy = 0;
        c.gridx = 3;
        propertiesPanel.add(Box.createRigidArea(new Dimension(10, 0)), c);
        c.anchor = GridBagConstraints.EAST;
        c.gridy = 0;
        c.gridx = 4;
        propertiesPanel.add(new JLabel("Empf. VK-Preis: (pro Set)"), c);
        c.anchor = GridBagConstraints.EAST;
        c.gridy = 0;
        c.gridx = 5;
        propertiesPanel.add(empfVkPreisField, c);
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0;
        c.gridx = 6;
        propertiesPanel.add(new JLabel(bc.currencySymbol), c);

        c.anchor = GridBagConstraints.EAST;
        c.gridy = 1;
        c.gridx = 0;
        propertiesPanel.add(new JLabel("Kurzname: "), c);
        c.anchor = GridBagConstraints.EAST;
        c.gridy = 1;
        c.gridx = 1;
        propertiesPanel.add(kurznameField, c);
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 1;
        c.gridx = 2;
        propertiesPanel.add(new JLabel(""), c);
        c.anchor = GridBagConstraints.CENTER;
        c.gridy = 1;
        c.gridx = 3;
        propertiesPanel.add(Box.createRigidArea(new Dimension(10, 0)), c);
        c.anchor = GridBagConstraints.EAST;
        c.gridy = 1;
        c.gridx = 4;
        propertiesPanel.add(new JLabel("Menge: "), c);
        c.anchor = GridBagConstraints.EAST;
        c.gridy = 1;
        c.gridx = 5;
        propertiesPanel.add(mengeField, c);
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 1;
        c.gridx = 6;
        propertiesPanel.add(new JLabel("kg/l/St."), c);

        c.anchor = GridBagConstraints.EAST;
        c.gridy = 2;
        c.gridx = 0;
        propertiesPanel.add(new JLabel("Herkunft: "), c);
        c.anchor = GridBagConstraints.EAST;
        c.gridy = 2;
        c.gridx = 1;
        propertiesPanel.add(herkunftField, c);
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 2;
        c.gridx = 2;
        propertiesPanel.add(new JLabel(""), c);
        c.anchor = GridBagConstraints.CENTER;
        c.gridy = 2;
        c.gridx = 3;
        propertiesPanel.add(Box.createRigidArea(new Dimension(10, 0)), c);
        c.anchor = GridBagConstraints.EAST;
        c.gridy = 2;
        c.gridx = 4;
        propertiesPanel.add(new JLabel("Einheit: "), c);
        c.anchor = GridBagConstraints.EAST;
        c.gridy = 2;
        c.gridx = 5;
        propertiesPanel.add(einheitField, c);
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 2;
        c.gridx = 6;
        propertiesPanel.add(sortimentBox, c);

        JPanel hinzufuegenPanel = new JPanel(new BorderLayout());
        JPanel hinzufuegenButtonPanel = new JPanel();
        hinzufuegenButton = new JButton("Hinzufügen");
        hinzufuegenButton.setMnemonic(KeyEvent.VK_H);
        hinzufuegenButton.addActionListener(this);
        hinzufuegenButton.setEnabled(false);
        hinzufuegenButtonPanel.add(hinzufuegenButton);
        hinzufuegenPanel.add(hinzufuegenButtonPanel, BorderLayout.CENTER);
        JPanel csvButtonPanel = new JPanel();
        csvListReadinButton = new JButton("Datei einlesen");
        csvListReadinButton.addActionListener(this);
        csvButtonPanel.add(csvListReadinButton);
        hinzufuegenPanel.add(csvButtonPanel, BorderLayout.EAST);

        formPanel.add(propertiesPanel);
        formPanel.add(hinzufuegenPanel);

        allPanel.add(formPanel, BorderLayout.NORTH);

        showTable();

        JPanel buttonPanel = new JPanel(new BorderLayout());
        JPanel centerPanel = new JPanel();
        deleteButton = new JButton("Löschen");
        deleteButton.setMnemonic(KeyEvent.VK_L);
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(this);
        centerPanel.add(deleteButton);

        JPanel radioButtonPanel = new JPanel();
        lmButton = new JRadioButton("LM");
        lmButton.setSelected(true);
        khwButton = new JRadioButton("KHW");
        // Group the radio buttons.
        ButtonGroup typeSelectGroup = new ButtonGroup();
        typeSelectGroup.add(lmButton);
        typeSelectGroup.add(khwButton);
        radioButtonPanel.add(new JLabel("Art: "));
        radioButtonPanel.add(lmButton);
        radioButtonPanel.add(khwButton);
        centerPanel.add(radioButtonPanel);

        printButton = new JButton("Artikel drucken");
        printButton.setMnemonic(KeyEvent.VK_D);
        printButton.setEnabled(false);
        printButton.addActionListener(this);
        centerPanel.add(printButton);

        buttonPanel.add(centerPanel, BorderLayout.CENTER);

        allPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.add(allPanel, BorderLayout.CENTER);
    }

    protected void setTableProperties(ArticleSelectTable table) {
        // Spalteneigenschaften:
        // table.getColumnModel().getColumn(0).setPreferredWidth(10);
        TableColumn lief = table.getColumn("Lieferant");
        lief.setCellRenderer(zentralAusrichter);
        lief.setPreferredWidth(5);
        TableColumn artikelnr = table.getColumn("Artikel-Nr.");
        artikelnr.setCellRenderer(linksAusrichter);
        artikelnr.setPreferredWidth(50);
        TableColumn artikelbez = table.getColumn("Name");
        artikelbez.setCellRenderer(linksAusrichter);
        artikelbez.setPreferredWidth(150);
        TableColumn preis = table.getColumn("Preis");
        preis.setCellRenderer(rechtsAusrichter);
        preis.setPreferredWidth(30);
        TableColumn menge = table.getColumn("Menge");
        menge.setCellRenderer(rechtsAusrichter);
        menge.setPreferredWidth(30);
        TableColumn kg_preis = table.getColumn("Preis pro kg/l/St.");
        kg_preis.setCellRenderer(rechtsAusrichter);
        kg_preis.setPreferredWidth(30);
        TableColumn einheit = table.getColumn("Einheit");
        einheit.setCellRenderer(zentralAusrichter);
        einheit.setPreferredWidth(30);
        TableColumn herkunft = table.getColumn("Herkunft");
        herkunft.setCellRenderer(zentralAusrichter);
        herkunft.setPreferredWidth(50);
        TableColumn mwst = table.getColumn("MwSt.");
        mwst.setCellRenderer(rechtsAusrichter);
        mwst.setPreferredWidth(30);
        TableColumn entf = table.getColumn("Entfernen");
        entf.setPreferredWidth(2);
        TableColumn aend = table.getColumn("Ändern");
        aend.setPreferredWidth(2);
    }

    void showTable() {
        myTable = new ArticleSelectTable(data, columnLabels, colors);
        setTableProperties(myTable);

        articleListPanel = new JPanel();
        articleListPanel.setLayout(new BoxLayout(articleListPanel, BoxLayout.PAGE_AXIS));
        articleListPanel.setBorder(BorderFactory.createTitledBorder("Gewählte Artikel"));
        scrollPane = new JScrollPane(myTable);
        articleListPanel.add(scrollPane);
        allPanel.add(articleListPanel, BorderLayout.CENTER);
    }

    void emptyTable() {
        data = new Vector<>();
        //artikelIDs = new Vector<Integer>();
        articleNumbers = new Vector<>();
        lieferanten = new Vector<>();
        articleNames = new Vector<>();
        kurzlieferanten = new Vector<>();
        kurznamen = new Vector<>();
        mengen = new Vector<>();
        preise = new Vector<>();
        kgPreise = new Vector<>();
        einheiten = new Vector<>();
        herkuenfte = new Vector<>();
        colors = new Vector<>();
        types = new Vector<>();
        removeButtons = new Vector<>();
        editButtons = new Vector<>();
    }

    private void clearAll() {
        data.clear();
        //artikelIDs.clear();
        articleNumbers.clear();
        lieferanten.clear();
        articleNames.clear();
        kurzlieferanten.clear();
        kurznamen.clear();
        mengen.clear();
        preise.clear();
        kgPreise.clear();
        einheiten.clear();
        herkuenfte.clear();
        colors.clear();
        types.clear();
        removeButtons.clear();
        editButtons.clear();
    }

    private void updateAll() {
        this.remove(allPanel);
        this.revalidate();
        showAll();
        asPanel.emptyBarcodeBox();
        // scroll the table scroll pane to bottom:
        scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
    }

    private void updateTable() {
        allPanel.remove(articleListPanel);
        allPanel.revalidate();
        showTable();
        // scroll the table scroll pane to bottom:
        scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());

        deleteButton.setEnabled(data.size() > 0);
        printButton.setEnabled(data.size() > 0);
    }

    protected void setButtonsEnabled() {
        if (vkPreisField.getText().length() > 0) {
            hinzufuegenButton.setEnabled(true);
        } else {
            hinzufuegenButton.setEnabled(false);
        }
    }

    private void checkIfArticleChanged(Artikel origArticle) {
        String vkPreis = origArticle.getVKP();
        vkPreis = vkPreis == null ? "" : vkPreis;
        String empfVkPreis = origArticle.getEmpfVKP();
        empfVkPreis = empfVkPreis == null ? "" : empfVkPreis;
        String kurzname = origArticle.getKurzname();
        kurzname = kurzname == null ? "" : kurzname;
        String menge = bc.unifyDecimalIntern(origArticle.getMenge());
        String einheit = origArticle.getEinheit();
        String herkunft = origArticle.getHerkunft();
        herkunft = herkunft == null ? "" : herkunft;
        Boolean sortiment = origArticle.getSortiment();

        String user_vkPreis = bc.priceFormatterIntern(vkPreisField.getText());
        String user_empfVkPreis = bc.priceFormatterIntern(empfVkPreisField.getText());
        String user_kurzname = kurznameField.getText();
        String user_menge = bc.unifyDecimalIntern(mengeField.getText());
        String user_einheit = einheitField.getText();
        String user_herkunft = herkunftField.getText();
        Boolean user_sortiment = sortimentBox.isSelected();
        if (!vkPreis.equals(user_vkPreis) || !empfVkPreis.equals(user_empfVkPreis) || !kurzname.equals(user_kurzname)
                || !menge.equals(user_menge) || !einheit.equals(user_einheit) || !herkunft.equals(user_herkunft)
                || !sortiment.equals(user_sortiment)) {
            // user has edited the article, update the article
            Artikel newArticle = getArticle(selectedArticleID);
            newArticle.setVKP(user_vkPreis);
            newArticle.setEmpfVKP(user_empfVkPreis);
            newArticle.setKurzname(user_kurzname);
            newArticle.setMenge(new BigDecimal(user_menge));
            newArticle.setEinheit(user_einheit);
            newArticle.setHerkunft(user_herkunft);
            newArticle.setSortiment(user_sortiment);
            updateArticle(origArticle, newArticle);

            System.out.println("old selectedArticleID: " + selectedArticleID);
            asPanel.updateSelectedArticleID();
            System.out.println("new selectedArticleID: " + selectedArticleID);
        }
    }

    private void hinzufuegen(Artikel a) {
        String artikelNummer = a.getNummer();
        String lieferant = getLieferantName(selectedArticleID);
        String artikelName = a.getName();
        String liefkurz = getShortLieferantName(selectedArticleID);
        String kurzname = getShortName(a);
        String artikelMwSt = getVAT(selectedArticleID);
        Boolean sortiment = a.getSortiment();
        String color = sortiment ? "default" : "gray";
        String[] menge_preis_kg_preis = getMengePriceAndPricePerKg(a);
        String menge = menge_preis_kg_preis[0];
        String preis = menge_preis_kg_preis[1];
        String kgPreis = menge_preis_kg_preis[2];
        String einheit = a.getEinheit();
        String herkunft = a.getHerkunft() == null ? "" : a.getHerkunft();

        // for PreisschilderExport:
        //artikelIDs.add(selectedArticleID);
        articleNumbers.add(artikelNummer);
        lieferanten.add(lieferant);
        articleNames.add(artikelName);
        kurzlieferanten.add(liefkurz);
        kurznamen.add(kurzname);
        mengen.add(menge);
        preise.add(preis);
        kgPreise.add(kgPreis);
        einheiten.add(einheit);
        herkuenfte.add(herkunft);

        colors.add(color);
        types.add("artikel");
        removeButtons.add(new JButton("Entfernen"));
        removeButtons.lastElement().addActionListener(this);
        editButtons.add(new JButton("Ändern"));
        editButtons.lastElement().addActionListener(this);

        Vector<Object> row = new Vector<Object>();
        row.add(liefkurz);
        row.add(artikelNummer);
        row.add(kurzname);
        row.add(preis);
        row.add(menge);
        row.add(kgPreis);
        row.add(einheit);
        row.add(herkunft);
        row.add(bc.vatFormatter(artikelMwSt));
        row.add(removeButtons.lastElement());
        row.add(editButtons.lastElement());
        data.add(row);

        updateTable();
    }

    private void artikelHinzufuegen() {
        if (asPanel.artikelBox.getItemCount() != 1 || asPanel.nummerBox.getItemCount() != 1) {
            System.out.println("Error: article not selected unambiguously.");
            return;
        }
        Artikel origArticle = getArticle(selectedArticleID);
        // check if user has changed any of the editable values
        checkIfArticleChanged(origArticle);
        Artikel a = getArticle(selectedArticleID);

        hinzufuegen(a);
        asPanel.emptyBarcodeBox();
    }

    private void delete() {
        clearAll();
        updateAll();
    }


    private void doCSVListReadin(File file) {
        try {
            // use Reader classes for text files:
            BufferedReader in = new BufferedReader(new FileReader(file)); // Lesen einer Textdatei mit Default Zeichensatz-Codierung, see http://www.wsoftware.de/practices/charsets.html
            // ignore the first line as header line:
            String firstLine = in.readLine();
            // parse file:
            String line;
            while ( (line = in.readLine()) != null) {
                line = line.replaceAll("#.*",""); // remove comment
                // get the fields
                String[] fields = line.split(bc.delimiter);

                String lieferant = fields[0];
                String nummer = fields[1];
                System.out.println(lieferant+"   "+nummer);

                selectedArticleID = getArticleID(lieferant, nummer);
                if (selectedArticleID < 0) {
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel von '"+lieferant+"' mit Nummer '"+nummer+"' nicht in Datenbank gefunden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                } else {
                    Artikel a = getArticle(selectedArticleID);
                    hinzufuegen(a);
                }
            }
        } catch (FileNotFoundException ex) {
            System.out.println("CSV list file for price tags not found.");
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    /**
     * A class implementing ArticleSelectUser must have this method.
     */
    public void updateSelectedArticleID(int selectedArticleID) {
        this.selectedArticleID = selectedArticleID;
    }

    /**
     * * Each non abstract class that implements the DocumentListener must have
     * these methods.
     *
     * @param e
     *            the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
        setButtonsEnabled();
    }

    public void removeUpdate(DocumentEvent e) {
        insertUpdate(e);
    }

    public void changedUpdate(DocumentEvent e) {
        // Plain text components do not fire these events
    }

    void removeRow(int rowIndex) {
        data.remove(rowIndex);
        //artikelIDs.remove(rowIndex);
        articleNumbers.remove(rowIndex);
        lieferanten.remove(rowIndex);
        articleNames.remove(rowIndex);
        kurzlieferanten.remove(rowIndex);
        kurznamen.remove(rowIndex);
        mengen.remove(rowIndex);
        preise.remove(rowIndex);
        kgPreise.remove(rowIndex);
        einheiten.remove(rowIndex);
        herkuenfte.remove(rowIndex);
        colors.remove(rowIndex);
        types.remove(rowIndex);
        removeButtons.remove(rowIndex);
        editButtons.remove(rowIndex);

        // remove extra rows (Rabatt oder Pfand):
        while (rowIndex < removeButtons.size() && removeButtons.get(rowIndex) == null) {
            data.remove(rowIndex);
            //artikelIDs.remove(rowIndex);
            articleNumbers.remove(rowIndex);
            lieferanten.remove(rowIndex);
            articleNames.remove(rowIndex);
            kurzlieferanten.remove(rowIndex);
            kurznamen.remove(rowIndex);
            mengen.remove(rowIndex);
            preise.remove(rowIndex);
            kgPreise.remove(rowIndex);
            einheiten.remove(rowIndex);
            herkuenfte.remove(rowIndex);
            colors.remove(rowIndex);
            types.remove(rowIndex);
            removeButtons.remove(rowIndex);
            editButtons.remove(rowIndex);
        }
        updateTable();
    }

    /**
     * * Each non abstract class that implements the ActionListener must have
     * this method.
     *
     * @param e
     *            the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == hinzufuegenButton) {
            artikelHinzufuegen();
            return;
        }
        if (e.getSource() == csvListReadinButton) {
            int returnVal = csvChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION){
                File file = csvChooser.getSelectedFile();
                doCSVListReadin(file);
                System.out.println("Opened " + file.getName());
            } else {
                System.out.println("Open command cancelled by user.");
            }
            return;
        }
        if (e.getSource() == deleteButton) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Wirklich löschen?", "Löschen",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                delete();
            }
            return;
        }
        if (e.getSource() == printButton) {
            String type = lmButton.isSelected() ? "lm" : khwButton.isSelected() ? "khw" : "";
            new PreisschilderExport(this.conn, this.mainWindow, type, kurzlieferanten, kurznamen, articleNumbers, mengen,
                    preise, kgPreise, einheiten, herkuenfte);
            return;
        }
        int rowIndex = -1;
        for (int i = 0; i < removeButtons.size(); i++) {
            if (e.getSource() == removeButtons.get(i)) {
                rowIndex = i;
                break;
            }
        }
        if (rowIndex > -1) {
            removeRow(rowIndex);
            return;
        }
        rowIndex = -1;
        for (int i = 0; i < editButtons.size(); i++) {
            if (e.getSource() == editButtons.get(i)) {
                rowIndex = i;
                break;
            }
        }
        if (rowIndex > -1) {
            Boolean sortiment = colors.get(rowIndex).equals("gray") ? false : true;
            String[] nummer = new String[]{articleNumbers.get(rowIndex)};
            String[] name = new String[]{articleNames.get(rowIndex),
                lieferanten.get(rowIndex),
                sortiment.toString()};
            removeRow(rowIndex);
            asPanel.nummerBox.setBox(nummer);
            asPanel.artikelBox.setBox(name);
            asPanel.checkIfFormIsComplete();
            return;
        }
    }
}
