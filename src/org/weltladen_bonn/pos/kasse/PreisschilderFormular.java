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
    protected JTextField ekPreisField;

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
    private Vector< Vector<Object> > data;

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
     *    The constructor.
     *       */
    public PreisschilderFormular(Connection conn, MainWindowGrundlage mw, TabbedPaneGrundlage tp) {
	super(conn, mw);
        tabbedPane = tp;

        initiateVectors();
        columnLabels.add("Entfernen");

        emptyTable();
	showAll();
        asPanel.emptyBarcodeBox();
    }

    private void initiateVectors() {
	columnLabels = new Vector<String>();
        columnLabels.add("Lieferant"); columnLabels.add("Artikel-Name");
        columnLabels.add("Artikel-Nr."); columnLabels.add("Barcode");
        columnLabels.add("Einzelpreis"); columnLabels.add("MwSt.");
        colors = new Vector<String>();
        types = new Vector<String>();
    }

    void showAll(){
	allPanel = new JPanel();
	allPanel.setLayout(new BoxLayout(allPanel, BoxLayout.Y_AXIS));

        asPanel = new ArticleSelectPanelPreisschilder(conn, mainWindow, this);
        allPanel.add(asPanel);

        JPanel propertiesPanel = new JPanel();
            JLabel vkPreisLabel = new JLabel("VK-Preis: ");
            vkPreisField = new JTextField("");
            vkPreisField.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) { if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
                    if (hinzufuegenButton.isEnabled()){
                        hinzufuegenButton.doClick();
                    }
                } }
            });
            vkPreisField.getDocument().addDocumentListener(this);
            ((AbstractDocument)vkPreisField.getDocument()).setDocumentFilter(geldFilter);
            vkPreisField.setEditable(false);
            vkPreisField.setColumns(6);
            vkPreisField.setHorizontalAlignment(JTextField.RIGHT);
        propertiesPanel.add(vkPreisLabel);
        propertiesPanel.add(vkPreisField);
        propertiesPanel.add(new JLabel(bc.currencySymbol));
            JLabel ekPreisLabel = new JLabel("EK-Preis: ");
            ekPreisField = new JTextField("");
            ekPreisField.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) { if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
                    if (hinzufuegenButton.isEnabled()){
                        hinzufuegenButton.doClick();
                    }
                } }
            });
            ekPreisField.getDocument().addDocumentListener(this);
            ((AbstractDocument)ekPreisField.getDocument()).setDocumentFilter(geldFilter);
            ekPreisField.setEditable(false);
            ekPreisField.setColumns(6);
            ekPreisField.setHorizontalAlignment(JTextField.RIGHT);
        propertiesPanel.add(ekPreisLabel);
        propertiesPanel.add(ekPreisField);
        propertiesPanel.add(new JLabel(bc.currencySymbol));

        JPanel hinzufuegenButtonPanel = new JPanel();
            hinzufuegenButton = new JButton("Hinzufügen");
            hinzufuegenButton.setMnemonic(KeyEvent.VK_H);
            hinzufuegenButton.addActionListener(this);
            hinzufuegenButton.setEnabled(false);
        hinzufuegenButtonPanel.add(hinzufuegenButton);

        allPanel.add(propertiesPanel);
        allPanel.add(hinzufuegenButtonPanel);

	showTable();

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());
            JPanel centerPanel = new JPanel();
                deleteButton = new JButton("Löschen");
                deleteButton.setMnemonic(KeyEvent.VK_L);
                if (data.size() == 0) deleteButton.setEnabled(false);
                deleteButton.addActionListener(this);
                centerPanel.add(deleteButton);

                printButton = new JButton("Artikel drucken");
                printButton.setMnemonic(KeyEvent.VK_D);
                if (data.size() == 0) printButton.setEnabled(false);
                printButton.addActionListener(this);
                centerPanel.add(printButton);
            buttonPanel.add(centerPanel, BorderLayout.CENTER);
        allPanel.add(buttonPanel);

	this.add(allPanel, BorderLayout.CENTER);
    }



    protected void setTableProperties(ArticleSelectTable table) {
	// Spalteneigenschaften:
//	table.getColumnModel().getColumn(0).setPreferredWidth(10);
	TableColumn pos = table.getColumn("Lieferant");
	pos.setCellRenderer(zentralAusrichter);
	pos.setPreferredWidth(5);
	TableColumn artikelbez = table.getColumn("Artikel-Name");
	artikelbez.setCellRenderer(linksAusrichter);
	artikelbez.setPreferredWidth(150);
	TableColumn artikelnr = table.getColumn("Artikel-Nr.");
	artikelnr.setCellRenderer(linksAusrichter);
	artikelnr.setPreferredWidth(50);
	TableColumn barcode = table.getColumn("Barcode");
	barcode.setCellRenderer(linksAusrichter);
	TableColumn preis = table.getColumn("Einzelpreis");
	preis.setCellRenderer(rechtsAusrichter);
	TableColumn mwst = table.getColumn("MwSt.");
	mwst.setCellRenderer(rechtsAusrichter);
	mwst.setPreferredWidth(5);
    }


    void showTable(){
	myTable = new ArticleSelectTable(data, columnLabels, colors);
	setTableProperties(myTable);
	TableColumn entf = myTable.getColumn("Entfernen");
	entf.setPreferredWidth(2);

	articleListPanel = new JPanel();
	articleListPanel.setLayout(new BoxLayout(articleListPanel, BoxLayout.Y_AXIS));
	articleListPanel.setBorder(BorderFactory.createTitledBorder("Gewählte Artikel"));
            scrollPane = new JScrollPane(myTable);
            articleListPanel.add(scrollPane);
	allPanel.add(articleListPanel);
    }

    void emptyTable(){
	data = new Vector< Vector<Object> >();
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

    private void clearAll(){
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

    private void updateAll(){
	this.remove(allPanel);
	this.revalidate();
	showAll();
        asPanel.emptyBarcodeBox();

        // scroll the table scroll pane to bottom:
        scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
    }

    private void updateTable(){
	allPanel.remove(articleListPanel);
	allPanel.revalidate();
	showTable();
    }






    protected void setButtonsEnabled() {
        if (vkPreisField.getText().length() > 0) {
            hinzufuegenButton.setEnabled(true);
            hinzufuegenButton.requestFocus();
        }
        else {
            hinzufuegenButton.setEnabled(false);
        }
    }

    private void hinzufuegen() {
        if (asPanel.artikelBox.getItemCount() != 1 || asPanel.nummerBox.getItemCount() != 1){
            System.out.println("Error: article not selected unambiguously.");
            return;
        }
        Artikel a = getArticle(selectedArticleID);
        String artikelNummer = a.getNummer();
        String kurzname = getShortName(a);
        String liefkurz = getShortLieferantName(selectedArticleID);
        String barcode = a.getBarcode();
        String artikelMwSt = getVAT(selectedArticleID);
        Boolean sortiment = a.getSortiment();
        String color = sortiment ? "default" : "gray";
        String[] menge_preis_kg_preis = getMengePriceAndPricePerKg(a);
        String menge = menge_preis_kg_preis[0];
        String preis = menge_preis_kg_preis[1];
        String kg_preis = menge_preis_kg_preis[2];

        // for PreisschilderExport:
        artikelIDs.add(selectedArticleID);
        articleNames.add(kurzname);
        mengen.add(menge);
        preise.add(preis);
        lieferanten.add(liefkurz);
        kg_preise.add(kg_preis);

        colors.add(color);
        types.add("artikel");
        removeButtons.add(new JButton("-"));
        removeButtons.lastElement().addActionListener(this);

        Vector<Object> row = new Vector<Object>();
            row.add(liefkurz);
            row.add(kurzname); row.add(artikelNummer); row.add(barcode);
            row.add(preis); row.add(bc.vatFormatter(artikelMwSt));
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
     *    * Each non abstract class that implements the DocumentListener
     *      must have these methods.
     *
     *    @param e the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
        if (e.getDocument() == vkPreisField.getDocument()){
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

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == hinzufuegenButton){
            artikelHinzufuegen();
	    return;
	}
	if (e.getSource() == deleteButton){
            delete();
	    return;
	}
	if (e.getSource() == printButton){
            new PreisschilderExport(this.conn, this.mainWindow,
                    articleNames, mengen, preise, lieferanten, kg_preise);
	    return;
	}
	int removeRow = -1;
	for (int i=0; i<removeButtons.size(); i++){
	    if (e.getSource() == removeButtons.get(i) ){
		removeRow = i;
		break;
	    }
	}
        if (removeRow > -1){
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
            while ( removeRow < removeButtons.size() && removeButtons.get(removeRow) == null ){
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
