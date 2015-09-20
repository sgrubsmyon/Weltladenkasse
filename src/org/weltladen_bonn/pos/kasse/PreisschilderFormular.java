package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.*; // Connection, Statment, ResultSet

// GUI stuff:
import java.awt.*; // for BorderLayout, FlowLayout, Dimension
import java.awt.event.*;

import javax.swing.*; // JFrame, JPanel, JTable, JButton etc.
import javax.swing.table.*;
import javax.swing.text.*; // for DocumentFilter
import javax.swing.event.*;

import org.weltladen_bonn.pos.*;

public class PreisschilderFormular extends ArtikelGrundlage implements ArticleSelectUser, DocumentListener {
    // Attribute:
    private TabbedPaneGrundlage tabbedPane;

    // Text Fields
    private int selectedArticleID;

    private ArticleSelectPanelPreisschilder asPanel;
    protected JTextField vkPreisField;
    protected JTextField empfVkPreisField;
    protected JTextField kurznameField;
    protected JTextField mengeField;

    // Buttons
    private JButton hinzufuegenButton;
    private Vector<JButton> removeButtons;
    private JButton deleteButton;
    private JButton printButton;

    // The panels
    private JPanel allPanel;
    private JPanel articleListPanel;

    // The table holding the purchase articles.
    private ArticleSelectTable myTable;
    private JScrollPane scrollPane;
    private Vector<String> columnLabels;
    private Vector<Vector<Object>> data;

    private Vector<Integer> artikelIDs;
    private Vector<String> articleNames;
    private Vector<String> mengen;
    private Vector<String> preise;
    private Vector<String> lieferanten;
    private Vector<String> kg_preise;
    protected Vector<String> colors;
    protected Vector<String> types;

    // Methoden:

    /**
     * The constructor.
     */
    public PreisschilderFormular(Connection conn, MainWindowGrundlage mw, TabbedPaneGrundlage tp) {
        super(conn, mw);
        tabbedPane = tp;

        initiateVectors();

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
        columnLabels.add("MwSt.");
        columnLabels.add("Entfernen");
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
        ((AbstractDocument) vkPreisField.getDocument()).setDocumentFilter(geldFilter);
        vkPreisField.setEditable(false);
        vkPreisField.setColumns(6);
        vkPreisField.setHorizontalAlignment(JTextField.RIGHT);

        empfVkPreisField = new JTextField("");
        empfVkPreisField.addKeyListener(enterAdapter);
        empfVkPreisField.getDocument().addDocumentListener(this);
        ((AbstractDocument) empfVkPreisField.getDocument()).setDocumentFilter(geldFilter);
        empfVkPreisField.setEditable(false);
        empfVkPreisField.setColumns(6);
        empfVkPreisField.setHorizontalAlignment(JTextField.RIGHT);

        kurznameField = new JTextField("");
        kurznameField.addKeyListener(enterAdapter);
        kurznameField.getDocument().addDocumentListener(this);
        ((AbstractDocument) kurznameField.getDocument()).setDocumentFilter(kurznameFilter);
        kurznameField.setEditable(false);
        kurznameField.setColumns(20);

        mengeField = new JTextField("");
        mengeField.addKeyListener(enterAdapter);
        mengeField.getDocument().addDocumentListener(this);
        ((AbstractDocument) mengeField.getDocument()).setDocumentFilter(mengeFilter);
        mengeField.setEditable(false);
        mengeField.setColumns(10);
        mengeField.setHorizontalAlignment(JTextField.RIGHT);

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

        JPanel hinzufuegenButtonPanel = new JPanel();
        hinzufuegenButton = new JButton("Hinzufügen");
        hinzufuegenButton.setMnemonic(KeyEvent.VK_H);
        hinzufuegenButton.addActionListener(this);
        hinzufuegenButton.setEnabled(false);
        hinzufuegenButtonPanel.add(hinzufuegenButton);

        formPanel.add(propertiesPanel);
        formPanel.add(hinzufuegenButtonPanel);

        allPanel.add(formPanel, BorderLayout.NORTH);

        showTable();

        JPanel buttonPanel = new JPanel(new BorderLayout());
        JPanel centerPanel = new JPanel();
        deleteButton = new JButton("Löschen");
        deleteButton.setMnemonic(KeyEvent.VK_L);
        if (data.size() == 0)
            deleteButton.setEnabled(false);
        deleteButton.addActionListener(this);
        centerPanel.add(deleteButton);

        printButton = new JButton("Artikel drucken");
        printButton.setMnemonic(KeyEvent.VK_D);
        if (data.size() == 0)
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
        TableColumn mwst = table.getColumn("MwSt.");
        mwst.setCellRenderer(rechtsAusrichter);
        mwst.setPreferredWidth(30);
        TableColumn entf = table.getColumn("Entfernen");
        entf.setPreferredWidth(2);
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
        data = new Vector<Vector<Object>>();
        artikelIDs = new Vector<Integer>();
        articleNames = new Vector<String>();
        mengen = new Vector<String>();
        preise = new Vector<String>();
        lieferanten = new Vector<String>();
        kg_preise = new Vector<String>();
        colors = new Vector<String>();
        types = new Vector<String>();
        removeButtons = new Vector<JButton>();
    }

    private void clearAll() {
        data.clear();
        artikelIDs.clear();
        articleNames.clear();
        mengen.clear();
        preise.clear();
        lieferanten.clear();
        kg_preise.clear();
        colors.clear();
        types.clear();
        removeButtons.clear();
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
    }

    protected void setButtonsEnabled() {
        if (vkPreisField.getText().length() > 0) {
            hinzufuegenButton.setEnabled(true);
        } else {
            hinzufuegenButton.setEnabled(false);
        }
    }

    private void hinzufuegen() {
        if (asPanel.artikelBox.getItemCount() != 1 || asPanel.nummerBox.getItemCount() != 1) {
            System.out.println("Error: article not selected unambiguously.");
            return;
        }
        Artikel origArticle = getArticle(selectedArticleID);

        // check if user has changed any of the editable values
        String vkPreis = origArticle.getVKP();
        vkPreis = vkPreis == null ? "" : vkPreis;
        String empfVkPreis = origArticle.getEmpfVKP();
        empfVkPreis = empfVkPreis == null ? "" : empfVkPreis;
        String kurzname = origArticle.getKurzname();
        kurzname = kurzname == null ? "" : kurzname;
        String menge = bc.unifyDecimalIntern(origArticle.getMenge());
        String user_vkPreis = bc.priceFormatterIntern(vkPreisField.getText());
        String user_empfVkPreis = bc.priceFormatterIntern(empfVkPreisField.getText());
        String user_kurzname = kurznameField.getText();
        String user_menge = bc.unifyDecimalIntern(mengeField.getText());
        if (!vkPreis.equals(user_vkPreis) || !empfVkPreis.equals(user_empfVkPreis) || !kurzname.equals(user_kurzname)
                || !menge.equals(user_menge)) {
            // user has edited the article, update the article
            Artikel newArticle = getArticle(selectedArticleID);
            newArticle.setVKP(user_vkPreis);
            newArticle.setEmpfVKP(user_empfVkPreis);
            newArticle.setKurzname(user_kurzname);
            newArticle.setMenge(new BigDecimal(user_menge));
            updateArticle(origArticle, newArticle);

            System.out.println("old selectedArticleID: " + selectedArticleID);
            asPanel.updateSelectedArticleID();
            System.out.println("new selectedArticleID: " + selectedArticleID);
        }

        Artikel a = getArticle(selectedArticleID);

        String liefkurz = getShortLieferantName(selectedArticleID);
        String artikelNummer = a.getNummer();
        kurzname = getShortName(a);
        String artikelMwSt = getVAT(selectedArticleID);
        Boolean sortiment = a.getSortiment();
        String color = sortiment ? "default" : "gray";
        String[] menge_preis_kg_preis = getMengePriceAndPricePerKg(a);
        menge = menge_preis_kg_preis[0];
        String preis = menge_preis_kg_preis[1];
        String kg_preis = menge_preis_kg_preis[2];

        // for PreisschilderExport:
        artikelIDs.add(selectedArticleID);
        lieferanten.add(liefkurz);
        articleNames.add(kurzname);
        mengen.add(menge);
        preise.add(preis);
        kg_preise.add(kg_preis);

        colors.add(color);
        types.add("artikel");
        removeButtons.add(new JButton("-"));
        removeButtons.lastElement().addActionListener(this);

        Vector<Object> row = new Vector<Object>();
        row.add(liefkurz);
        row.add(artikelNummer);
        row.add(kurzname);
        row.add(preis);
        row.add(menge);
        row.add(kg_preis);
        row.add(bc.vatFormatter(artikelMwSt));
        row.add(removeButtons.lastElement());
        data.add(row);

        updateAll();
    }

    private void artikelHinzufuegen() {
        hinzufuegen();
    }

    private void delete() {
        clearAll();
        updateAll();
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
        if (e.getSource() == deleteButton) {
            delete();
            return;
        }
        if (e.getSource() == printButton) {
            new PreisschilderExport(this.conn, this.mainWindow, articleNames, mengen, preise, lieferanten, kg_preise);
            return;
        }
        int removeRow = -1;
        for (int i = 0; i < removeButtons.size(); i++) {
            if (e.getSource() == removeButtons.get(i)) {
                removeRow = i;
                break;
            }
        }
        if (removeRow > -1) {
            data.remove(removeRow);
            artikelIDs.remove(removeRow);
            articleNames.remove(removeRow);
            mengen.remove(removeRow);
            preise.remove(removeRow);
            lieferanten.remove(removeRow);
            kg_preise.remove(removeRow);
            colors.remove(removeRow);
            types.remove(removeRow);
            removeButtons.remove(removeRow);

            // remove extra rows (Rabatt oder Pfand):
            while (removeRow < removeButtons.size() && removeButtons.get(removeRow) == null) {
                data.remove(removeRow);
                artikelIDs.remove(removeRow);
                articleNames.remove(removeRow);
                mengen.remove(removeRow);
                preise.remove(removeRow);
                lieferanten.remove(removeRow);
                kg_preise.remove(removeRow);
                colors.remove(removeRow);
                types.remove(removeRow);
                removeButtons.remove(removeRow);
            }

            updateAll();
            return;
        }
    }

}
