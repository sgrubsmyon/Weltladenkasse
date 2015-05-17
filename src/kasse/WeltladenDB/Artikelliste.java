package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.*;

// GUI stuff:
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.event.*; // for TableModelListener
import javax.swing.text.*; // for DocumentFilter

public class Artikelliste extends ArtikelGrundlage implements ItemListener, TableModelListener,
       ListSelectionListener, DocumentListener {
    // Attribute:
    private ArtikellisteContainer container;
    protected Integer toplevel_id;
    protected Integer sub_id;
    protected Integer subsub_id;
    protected String produktgruppenname;

    protected JPanel allPanel;
    protected JPanel artikelListPanel;
    protected JScrollPane scrollPane;
    protected JButton backButton;
    protected JCheckBox inaktivCheckBox;
    protected boolean showInaktive = false;
    protected JCheckBox sortimentCheckBox;
    protected boolean showOnlySortiment = true;
    protected JCheckBox internalCheckBox;
    protected boolean showInternals = false;
    protected JTextField filterField;
    private JButton saveButton;
    private JButton revertButton;
    private JButton editButton;
    private JButton newButton;
    private JButton importButton;
    private JButton exportButton;

    private String filterStr = "";
    private String aktivFilterStr = " AND artikel.aktiv = TRUE ";
    private String sortimentFilterStr = " AND artikel.sortiment = TRUE ";
    //private String orderByStr = "artikel_name";
    private String orderByStr = "p.toplevel_id, p.sub_id, p.subsub_id, artikel_nr";

    // The table holding the items
    protected AnyJComponentJTable myTable;
    protected Vector< Vector<Object> > data;
    protected Vector< Vector<Object> > originalData;
    protected Vector< Vector<Object> > displayData;
    protected Vector<Integer> displayIndices;
    protected Vector<String> columnLabels;
    protected Vector<Integer> artikelIDs;
    private Vector<Integer> produktGruppeIDs;
    private Vector<Integer> lieferantIDs;
    protected Vector<Boolean> sortimentBools;
    protected Vector<Boolean> lieferbarBools;
    protected Vector<Boolean> activeRowBools;
    protected Vector<Boolean> varPreisBools;
    protected Vector<Integer> beliebtIndices;

    protected Vector<String> linksColumns;
    protected Vector<String> rechtsColumns;
    protected Vector<String> zentralColumns;
    protected Vector<String> smallColumns;
    protected Vector<String> editableColumns;
    protected Vector<String> moneyColumns;
    protected Vector<String> decimalColumns;

    // Vectors storing table edits
    private Vector<String> editLieferant;
    private Vector<String> editArtikelNummer;
    private Vector<String> changedNummer;
    private Vector<String> changedName;
    private Vector<String> changedKurzname;
    private Vector<BigDecimal> changedMenge;
    private Vector<String> changedEinheit;
    private Vector<String> changedBarcode;
    private Vector<String> changedHerkunft;
    private Vector<Integer> changedVPE;
    private Vector<Integer> changedSetgroesse;
    private Vector<String> changedVKP;
    private Vector<String> changedEmpfVKP;
    private Vector<String> changedEKRabatt;
    private Vector<String> changedEKP;
    private Vector<Boolean> changedSortiment;
    private Vector<Boolean> changedLieferbar;
    private Vector<Integer> changedBeliebt;
    private Vector<Integer> changedBestand;
    private Vector<Boolean> changedAktiv;

    // Dialog to read items from file
    private JDialog readFromFileDialog;
    //private ArtikelImport itemsFromFile;

    // Methoden:
    protected Artikelliste(Connection conn, ArtikellisteContainer ac, Integer
            tid, Integer sid, Integer ssid, String gn) {
        super(conn, ac.getMainWindowPointer());

        this.container = ac;
        this.toplevel_id = tid;
        this.sub_id = sid;
        this.subsub_id = ssid;
        this.produktgruppenname = gn;

        fillDataArray();
        showAll();
    }

    protected Artikelliste(Connection conn, MainWindowGrundlage mwp) {
        super(conn, mwp);
    }

    protected void fillDataArray() {
        this.data = new Vector< Vector<Object> >();
        columnLabels = new Vector<String>();
        columnLabels.add("Produktgruppe");
        columnLabels.add("Lieferant");
        columnLabels.add("Nummer");
        columnLabels.add("Name");
        columnLabels.add("Kurzname");
        columnLabels.add("Menge");
        columnLabels.add("Einheit");
        columnLabels.add("VK-Preis");
        columnLabels.add("Sortiment");
        columnLabels.add("Lieferbar");
        columnLabels.add("Beliebtheit");
        columnLabels.add("Barcode");
        columnLabels.add("VPE");
        columnLabels.add("Setgröße");
        columnLabels.add("Empf. VK-Preis");
        columnLabels.add("EK-Rabatt");
        columnLabels.add("EK-Preis");
        columnLabels.add("MwSt.");
        columnLabels.add("Herkunft");
        columnLabels.add("Bestand");
        columnLabels.add("Ab/Seit");
        columnLabels.add("Bis"); columnLabels.add("Aktiv");
        artikelIDs = new Vector<Integer>();
        produktGruppeIDs = new Vector<Integer>();
        lieferantIDs = new Vector<Integer>();
        sortimentBools = new Vector<Boolean>();
        lieferbarBools = new Vector<Boolean>();
        activeRowBools = new Vector<Boolean>();
        varPreisBools = new Vector<Boolean>();
        beliebtIndices = new Vector<Integer>();

        linksColumns = new Vector<String>();
        linksColumns.add("Produktgruppe"); linksColumns.add("Lieferant");
        linksColumns.add("Name"); linksColumns.add("Kurzname");
        linksColumns.add("Herkunft"); linksColumns.add("Ab/Seit");
        linksColumns.add("Bis");
        //
        rechtsColumns = new Vector<String>();
        rechtsColumns.add("Nummer"); rechtsColumns.add("Menge");
        rechtsColumns.add("Barcode"); rechtsColumns.add("VPE");
        rechtsColumns.add("Setgröße"); rechtsColumns.add("VK-Preis");
        rechtsColumns.add("Empf. VK-Preis"); rechtsColumns.add("EK-Rabatt");
        rechtsColumns.add("EK-Preis"); rechtsColumns.add("MwSt.");
        rechtsColumns.add("Bestand");
        //
        zentralColumns = new Vector<String>();
        zentralColumns.add("Beliebtheit");
        zentralColumns.add("Einheit");
        //
        smallColumns = new Vector<String>();
        smallColumns.add("Ab/Seit"); smallColumns.add("Bis");

        editableColumns = new Vector<String>();
        editableColumns.add("Nummer"); editableColumns.add("Name");
        editableColumns.add("Kurzname"); editableColumns.add("Menge");
        editableColumns.add("Einheit");
        editableColumns.add("Barcode"); editableColumns.add("Herkunft");
        editableColumns.add("VPE"); editableColumns.add("Setgröße");
        editableColumns.add("Sortiment"); editableColumns.add("Lieferbar");
        editableColumns.add("Beliebtheit"); editableColumns.add("Bestand");
        editableColumns.add("Aktiv");
        //
        moneyColumns = new Vector<String>();
        moneyColumns.add("VK-Preis"); moneyColumns.add("Empf. VK-Preis");
        moneyColumns.add("EK-Preis");
        //
        decimalColumns = new Vector<String>(moneyColumns);
        decimalColumns.add("Menge"); decimalColumns.add("EK-Rabatt");

        String filter = "";
        if (toplevel_id == null){ // if user clicked on "Alle Artikel"
            if (showInternals)
                filter = "TRUE "; // to show all items, also internal ones (where toplevel_id == null)
            else
                filter = "p.toplevel_id > 0 ";
        } else {
            filter = "p.toplevel_id = " + toplevel_id + " ";
        }
        if (sub_id != null)
            filter += " AND p.sub_id = " + sub_id + " ";
        if (subsub_id != null)
            filter += " AND p.subsub_id = " + subsub_id + " ";
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT artikel_id, produktgruppen_id, produktgruppen_name, "+
                    "lieferant_id, lieferant_name, "+
                    "artikel_nr, artikel_name, "+
                    "kurzname, "+
                    "menge, einheit, barcode, "+
                    "herkunft, vpe, "+
                    "setgroesse, "+
                    "vk_preis, empf_vk_preis, ek_rabatt, ek_preis, variabler_preis, mwst_satz, "+
                    "sortiment, lieferbar, "+
                    "beliebtheit, bestand, "+
                    "DATE_FORMAT(von, '"+dateFormatSQL+"'), DATE_FORMAT(bis, '"+dateFormatSQL+"'), "+
                    "artikel.aktiv "+
                    "FROM artikel LEFT JOIN lieferant USING (lieferant_id) "+
                    "LEFT JOIN produktgruppe AS p USING (produktgruppen_id) "+
                    "LEFT JOIN mwst USING (mwst_id) "+
                    "WHERE " + filter +
                    aktivFilterStr +
                    sortimentFilterStr +
                    "ORDER BY " + orderByStr
                    );
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Integer artikel_id = rs.getInt(1);
                Integer produktgruppen_id = rs.getInt(2);
                String gruppenname = rs.getString(3);
                Integer lieferant_id = rs.getInt(4);
                String lieferant = rs.getString(5);
                String nr = rs.getString(6);
                String name = rs.getString(7);
                String kurzname = rs.getString(8);
                String menge = rs.getString(9);
                String einheit = rs.getString(10);
                String barcode = rs.getString(11);
                String herkunft = rs.getString(12);
                Integer vpe = rs.getInt(13);
                Integer setgroesse = rs.getInt(14);
                String vkp = rs.getString(15);
                String empf_vkp = rs.getString(16);
                String ek_rabatt = rs.getString(17);
                String ekp = rs.getString(18);
                Boolean var = rs.getBoolean(19);
                String mwst = rs.getString(20);
                Boolean sortimentBool = rs.getBoolean(21);
                Boolean lieferbarBool = rs.getBoolean(22);
                Integer beliebtWert = rs.getInt(23);
                String bestand = rs.getString(24);
                String von = rs.getString(25);
                String bis = rs.getString(26);
                Boolean aktivBool = rs.getBoolean(27);

                if (lieferant_id == null) lieferant_id = 1; // corresponds to "unknown"
                if (lieferant == null) lieferant = "";
                if (kurzname == null) kurzname = "";
                if (menge == null){ menge = ""; }
                else { menge = unifyDecimal(menge); }
                if (einheit == null){ einheit = ""; }
                if (barcode == null){ barcode = ""; }
                if (herkunft == null) herkunft = "";
                //if (vpe == null){ vpe = ""; }
                String vkpOutput = "";
                if (vkp != null){ vkpOutput = priceFormatter(vkp); }
                if (empf_vkp == null){ empf_vkp = ""; }
                else { empf_vkp = priceFormatter(empf_vkp); }
                if (ek_rabatt == null){ ek_rabatt = ""; }
                else { ek_rabatt = vatPercentRemover( vatFormatter(ek_rabatt) ); }
                if (ekp == null){ ekp = ""; }
                else { ekp = priceFormatter(ekp); }
                String mwstOutput = "";
                if (mwst != null){ mwstOutput = vatFormatter(mwst); }
                if (vkp == null){ vkpOutput = ""; }
                if (mwst == null){ mwstOutput = ""; }
                if (var == true){ vkpOutput = "variabel"; empf_vkp = "variabel";
                    ek_rabatt = "variabel"; ekp = "variabel"; }
                Integer beliebtIndex = 0;
                if (beliebtWert != null){
                    try {
                        beliebtIndex = beliebtWerte.indexOf(beliebtWert);
                    } catch (ArrayIndexOutOfBoundsException ex){
                        System.out.println("Unknown beliebtWert: "+beliebtWert);
                    }
                }
                if (bestand == null){ bestand = ""; }
                if (von == null) von = "";
                if (bis == null) bis = "";

                Vector<Object> row = new Vector<Object>();
                    row.add(gruppenname);
                    row.add(lieferant); row.add(nr);
                    row.add(name); row.add(kurzname);
                    row.add(menge); row.add(einheit);
                    row.add(vkpOutput);
                    row.add(sortimentBool);
                    row.add(lieferbarBool);
                    row.add(beliebtIndex); row.add(barcode);
                    row.add(vpe); row.add(setgroesse);
                    row.add(empf_vkp); row.add(ek_rabatt);
                    row.add(ekp); row.add(mwstOutput);
                    row.add(herkunft); row.add(bestand);
                    row.add(von); row.add(bis);
                    row.add(aktivBool);
                data.add(row);
                artikelIDs.add(artikel_id);
                produktGruppeIDs.add(produktgruppen_id);
                lieferantIDs.add(lieferant_id);
                sortimentBools.add(sortimentBool);
                lieferbarBools.add(lieferbarBool);
                activeRowBools.add(aktivBool);
                varPreisBools.add(var);
                beliebtIndices.add(beliebtIndex);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        refreshOriginalData();
        displayData = new Vector< Vector<Object> >(data);
        initiateDisplayIndices();
        editLieferant = new Vector<String>();
        editArtikelNummer = new Vector<String>();
        changedNummer = new Vector<String>();
        changedName = new Vector<String>();
        changedKurzname = new Vector<String>();
        changedMenge = new Vector<BigDecimal>();
        changedEinheit = new Vector<String>();
        changedBarcode = new Vector<String>();
        changedHerkunft = new Vector<String>();
        changedVPE = new Vector<Integer>();
        changedSetgroesse = new Vector<Integer>();
        changedVKP = new Vector<String>();
        changedEmpfVKP = new Vector<String>();
        changedEKRabatt = new Vector<String>();
        changedEKP = new Vector<String>();
        changedSortiment = new Vector<Boolean>();
        changedLieferbar = new Vector<Boolean>();
        changedBeliebt = new Vector<Integer>();
        changedBestand = new Vector<Integer>();
        changedAktiv = new Vector<Boolean>();
    }

    protected void refreshOriginalData() {
        this.originalData = new Vector< Vector<Object> >();
        for ( Vector<Object> row : data ){
            Vector<Object> originalRow = new Vector<Object>();
            originalRow.addAll(row);
            originalData.add(originalRow);
        }
    }

    private void putChangesIntoDB() {
        for (int index=0; index<editLieferant.size(); index++){
            Integer prod_id = null;
            Integer lief_id = null;
            Boolean var_preis = null;
            try {
                PreparedStatement pstmt = this.conn.prepareStatement(
                        "SELECT produktgruppen_id, lieferant_id, variabler_preis FROM artikel "+
                        "LEFT JOIN lieferant USING (lieferant_id) WHERE "+
                        "lieferant_name = ? AND "+
                        "artikel_nr = ? AND artikel.aktiv = TRUE"
                        );
                pstmt.setString(1, editLieferant.get(index));
                pstmt.setString(2, editArtikelNummer.get(index));
                ResultSet rs = pstmt.executeQuery();
                // Now do something with the ResultSet, should be only one result ...
                rs.next();
                prod_id = rs.getInt(1);
                lief_id = rs.getInt(2);
                var_preis = rs.getBoolean(3);
                rs.close();
                pstmt.close();
            } catch (SQLException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }

            // set old item to inactive:
            int result = setItemInactive(lief_id, editArtikelNummer.get(index));
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Artikel von "+editLieferant.get(index)+" mit Nummer "+editArtikelNummer.get(index)+" konnte nicht geändert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                continue; // continue with next item
            }
            if ( changedAktiv.get(index) == true ){ // only if the item wasn't set inactive voluntarily: add new item with new properties
                String ekpreis = figureOutEKP(changedEmpfVKP.get(index), changedEKRabatt.get(index), changedEKP.get(index));
                result = insertNewItem(prod_id, lief_id,
                        changedNummer.get(index), changedName.get(index),
                        changedKurzname.get(index), changedMenge.get(index),
                        changedEinheit.get(index),
                        changedBarcode.get(index), changedHerkunft.get(index),
                        changedVPE.get(index), changedSetgroesse.get(index),
                        changedVKP.get(index), changedEmpfVKP.get(index),
                        changedEKRabatt.get(index), ekpreis, var_preis,
                        changedSortiment.get(index),
                        changedLieferbar.get(index), changedBeliebt.get(index),
                        changedBestand.get(index));
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel von "+editLieferant.get(index)+" mit Nummer "+editArtikelNummer.get(index)+" konnte nicht geändert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    result = setItemActive(lief_id, editArtikelNummer.get(index));
                    if (result == 0){
                        JOptionPane.showMessageDialog(this,
                                "Fehler: Artikel von "+editLieferant.get(index)+" mit Nummer "+editArtikelNummer.get(index)+" konnte nicht wieder hergestellt werden. Artikel ist nun gelöscht (inaktiv).",
                                "Fehler", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }


    protected void showAll() {
        allPanel = new JPanel();
        allPanel.setLayout(new BorderLayout());

        showTopPanel();
        showTable();
        showBottomPanel();

        enableButtons();

        this.add(allPanel, BorderLayout.CENTER);
    }


    protected void showTopPanel() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        JPanel topLeftPanel = new JPanel();
        topLeftPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
          backButton = new JButton("Zurück");
          backButton.setMnemonic(KeyEvent.VK_Z);
          backButton.addActionListener(this);
          topLeftPanel.add(backButton);

          inaktivCheckBox = new JCheckBox("inaktive anzeigen");
          inaktivCheckBox.setMnemonic(KeyEvent.VK_A);
          inaktivCheckBox.setSelected(showInaktive);
          inaktivCheckBox.addItemListener(this);
          inaktivCheckBox.addActionListener(this);
          topLeftPanel.add(inaktivCheckBox);

          sortimentCheckBox = new JCheckBox("nur Sortiment anzeigen");
          sortimentCheckBox.setMnemonic(KeyEvent.VK_S);
          sortimentCheckBox.setSelected(showOnlySortiment);
          sortimentCheckBox.addItemListener(this);
          sortimentCheckBox.addActionListener(this);
          topLeftPanel.add(sortimentCheckBox);

          // Show internal items as well:
          //internalCheckBox = new JCheckBox("interne anzeigen");
          //internalCheckBox.setMnemonic(KeyEvent.VK_I);
          //internalCheckBox.setSelected(showInternals);
          //internalCheckBox.addItemListener(this);
          //topLeftPanel.add(internalCheckBox);
        topPanel.add(topLeftPanel, BorderLayout.WEST);
        JPanel topRightPanel = new JPanel();
        topRightPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
          JLabel filterLabel = new JLabel("Filter:");
          topRightPanel.add(filterLabel);
          filterField = new JTextField("");
          filterField.setColumns(20);
          filterField.getDocument().addDocumentListener(this);

          topRightPanel.add(filterField);
        topPanel.add(topRightPanel, BorderLayout.EAST);
        allPanel.add(topPanel, BorderLayout.NORTH);
    }


    protected void showBottomPanel() {
        JPanel bottomPanel = new JPanel();
	bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
          JPanel bottomLeftPanel = new JPanel();
          bottomLeftPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
            saveButton = new JButton("Änderungen speichern");
            saveButton.addActionListener(this);
            bottomLeftPanel.add(saveButton);

            revertButton = new JButton("Änderungen verwerfen");
            revertButton.addActionListener(this);
            bottomLeftPanel.add(revertButton);

            editButton = new JButton("Markierte Artikel bearbeiten");
            editButton.setMnemonic(KeyEvent.VK_B);
            editButton.addActionListener(this);
            bottomLeftPanel.add(editButton);
        bottomPanel.add(bottomLeftPanel);

          JPanel bottomRightPanel = new JPanel();
          bottomRightPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
            newButton = new JButton("Neue Artikel eingeben");
            newButton.setMnemonic(KeyEvent.VK_N);
            newButton.addActionListener(this);
            bottomRightPanel.add(newButton);

            importButton = new JButton("Artikel importieren");
            importButton.setMnemonic(KeyEvent.VK_D);
            importButton.addActionListener(this);
            bottomRightPanel.add(importButton);

            exportButton = new JButton("Artikel exportieren");
            exportButton.setMnemonic(KeyEvent.VK_B);
            exportButton.addActionListener(this);
            bottomRightPanel.add(exportButton);
        bottomPanel.add(bottomRightPanel);
        allPanel.add(bottomPanel, BorderLayout.SOUTH);
    }


    protected void enableButtons() {
        saveButton.setEnabled(editLieferant.size() > 0);
        revertButton.setEnabled(editLieferant.size() > 0);
        editButton.setEnabled(myTable.getSelectedRowCount() > 0);
        newButton.setEnabled(editLieferant.size() == 0);
        importButton.setEnabled(editLieferant.size() == 0);
        exportButton.setEnabled(editLieferant.size() == 0);
    }


    public class ArtikellisteTableModel extends AbstractTableModel {
        // Subclass the AbstractTableModel to set display data and
        // synchronize underlying data Vector.
        // Needed to prevent exception "java.lang.IllegalArgumentException: Identifier not found"
        public String getColumnName(int col) {
            return columnLabels.get(col);
        }
        // Needed to prevent exception "java.lang.IllegalArgumentException: Identifier not found"
        public int findColumn(String name) {
            int col=0;
            for (String s : columnLabels){
                if (s.equals(name)){
                    return col;
                }
                col++;
            }
            return -1;
        }
        public int getRowCount() { return displayData.size(); }
        public int getColumnCount() { return columnLabels.size(); }
        public Object getValueAt(int row, int col) {
            Object obj;
            try {
                obj = displayData.get(row).get(col);
            } catch (ArrayIndexOutOfBoundsException ex){
                System.out.println("No data at row "+row+", column "+col);
                obj = "";
            }
            return obj;
        }
        public void setValueAt(Object value, int row, int col) {
            Vector<Object> rowentries = displayData.get(row);
            rowentries.set(col, value);
            displayData.set(row, rowentries);
            int dataRow = displayIndices.get(row); // convert from displayData index to data index
            data.set(dataRow, rowentries);
            fireTableCellUpdated(row, col);
        }
    }

    protected class ArtikellisteTable extends AnyJComponentJTable {
        public ArtikellisteTable(TableModel m, Integer columnMargin,
                Integer minColumnWidth, Integer maxColumnWidth){
            super(m, columnMargin, minColumnWidth, maxColumnWidth);
        }

        // Subclass the AnyJComponentJTable to set editable cells, font properties and tool tip text.
        @Override
        public boolean isCellEditable(int row, int col) {
            String header = this.getColumnName(col);
            if ( activeRowBools.get(row) ){
                if ( moneyColumns.contains(header) || header.equals("EK-Rabatt") ) {
                    if ( ! displayData.get(row).get(col).equals("variabel") )
                        return true;
                }
                else if ( editableColumns.contains(header) ){
                    return true;
                }
            }
            return false;
        }

        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            // custom rendering
            Component c = super.prepareRenderer(renderer, row, column);
            Object value = this.getValueAt(row, column);
            int realRowIndex = row;
            realRowIndex = convertRowIndexToModel(realRowIndex);
            realRowIndex = displayIndices.get(realRowIndex); // convert from displayData index to data index
            // for rows with inactive items, set color:
            if ( ! activeRowBools.get(realRowIndex) ){
                c.setFont( c.getFont().deriveFont(Font.ITALIC) );
                c.setForeground(Color.BLUE);
            }
            // for articles not in sortiment, set color:
            else if ( ! sortimentBools.get(realRowIndex) ){
                c.setFont( c.getFont().deriveFont(Font.PLAIN) );
                c.setForeground(Color.GRAY);
            }
            else {
                c.setFont( c.getFont().deriveFont(Font.PLAIN) );
                c.setForeground(Color.BLACK);
            }
            // override the above if this is the "Beliebtheit" column:
            if ( this.getColumnName(column).equals("Beliebtheit") ){
                Integer index = Integer.parseInt(value.toString());
                c.setFont( c.getFont().deriveFont(Font.PLAIN) );
                c.setForeground( beliebtFarben.get(index) );
            }
            // now, render the text:
            if (c instanceof JLabel){
                JLabel label = (JLabel)c;
                String cname = this.getColumnName(column);
                String valueStr = "";
                if ( cname.equals("Beliebtheit") ){
                    // BeliebtRenderer:
                    //Integer index = beliebtIndices.get(realRowIndex);
                    Integer index = Integer.parseInt(value.toString());
                    label.setText( beliebtKuerzel.get(index) );
                }
                if ( moneyColumns.contains(cname) ){
                    // GeldRenderer:
                    if (value != null){
                        valueStr = value.toString();
                        if ( !valueStr.equals("variabel") ){
                            valueStr = priceFormatter(valueStr);
                            if ( !valueStr.equals("") ){
                                valueStr += " "+currencySymbol;
                            }
                        }
                    }
                    label.setText(valueStr);
                }
                if ( cname.equals("EK-Rabatt") ){
                    // PercentRenderer:
                    if (value != null){
                        valueStr = value.toString();
                        if ( !valueStr.equals("variabel") && !valueStr.equals("") ){
                            BigDecimal fracValue = new BigDecimal(
                                    vatParser(vatFormatter(valueStr))
                                    ).multiply(percent);
                            valueStr = vatFormatter(fracValue);
                        }
                    }
                    label.setText(valueStr);
                }
            }
            return c;
        }

        @Override
        public Component prepareEditor(TableCellEditor editor, int row, int column) {
            Component c = super.prepareEditor(editor, row, column);
            if (c instanceof JTextField){
                System.out.println("Is running prepareEditor(): row = "+row+"   col = "+column);
                JTextField textField = (JTextField)c;
                String cname = this.getColumnName(column);
                Vector<String> intColumns = new Vector<String>();
                intColumns.add("VPE"); intColumns.add("Setgröße");
                if ( intColumns.contains(cname) ){
                    IntegerDocumentFilter filter =
                        new IntegerDocumentFilter(1, smallintMax, container);
                    ((AbstractDocument)textField.getDocument()).setDocumentFilter(filter);
                }
                else if ( cname.equals("Menge") ){
                    ((AbstractDocument)textField.getDocument()).setDocumentFilter(mengeFilter);
                }
                else if ( cname.equals("Bestand") ){
                    ((AbstractDocument)textField.getDocument()).setDocumentFilter(intFilter);
                }
                else if ( moneyColumns.contains(cname) ){
                    ((AbstractDocument)textField.getDocument()).setDocumentFilter(geldFilter);
                }
                else if ( cname.equals("EK-Rabatt") ){
                    ((AbstractDocument)textField.getDocument()).setDocumentFilter(relFilter);
                }
                else if ( cname.equals("Beliebtheit") ){
                    Integer minBeliebt = Collections.min(beliebtWerte);
                    Integer maxBeliebt = Collections.max(beliebtWerte);
                    IntegerDocumentFilter beliebtFilter =
                        new IntegerDocumentFilter(minBeliebt, maxBeliebt, container);
                    ((AbstractDocument)textField.getDocument()).setDocumentFilter(beliebtFilter);
                }
                else {
                    // just a normal field, replace any DocumentFilter from before with default:
                    ((AbstractDocument)textField.getDocument()).setDocumentFilter(new DocumentFilter());
                }
            }
            return c;
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            String defaultTip = super.getToolTipText(e);
            Point p = e.getPoint();
            int colIndex = columnAtPoint(p);
            // override the default tool tip if this is the "Beliebtheit" column:
            if ( this.getColumnName(colIndex).equals("Beliebtheit") ){
                int rowIndex = rowAtPoint(p);
                //int realRowIndex = rowIndex;
                //realRowIndex = convertRowIndexToModel(realRowIndex);
                //realRowIndex = displayIndices.get(realRowIndex); // convert from displayData index to data index
                //Integer index = beliebtIndices.get(realRowIndex);
                Integer index = Integer.parseInt(this.getValueAt(rowIndex, colIndex).toString());
                String name = beliebtNamen.get(index);
                return name+" ("+index+")";
            }
            return defaultTip;
        }
    }

    protected void initiateTable() {
        // replace general number of items with displayed number of items:
        String borderLabel = this.produktgruppenname.
            replaceAll(" \\([0-9]*\\)$", " ("+displayData.size()+")");
        artikelListPanel.setBorder(BorderFactory.createTitledBorder(borderLabel));
	//artikelListPanel.revalidate();

        myTable = new ArtikellisteTable(new ArtikellisteTableModel(),
                columnMargin, minColumnWidth, maxColumnWidth);
        myTable.setAutoCreateRowSorter(true);
        myTable.getModel().addTableModelListener(this);
        myTable.getSelectionModel().addListSelectionListener(this);
        setTableProperties(myTable);
    }

    protected void showTable() {
        artikelListPanel = new JPanel();
        artikelListPanel.setLayout(new BorderLayout());

        initiateTable();

        scrollPane = new JScrollPane(myTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        myTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        artikelListPanel.add(scrollPane, BorderLayout.CENTER);
        allPanel.add(artikelListPanel, BorderLayout.CENTER);
    }

    void updateTable() {
        artikelListPanel.remove(scrollPane);
	artikelListPanel.revalidate();
        initiateTable();
        scrollPane = new JScrollPane(myTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        myTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        artikelListPanel.add(scrollPane);
        enableButtons();
    }

    protected void setTableProperties(AnyJComponentJTable myTable) {
        for (String cname : linksColumns){
            myTable.getColumn(cname).setCellRenderer(linksAusrichter);
        }
        for (String cname : rechtsColumns){
            myTable.getColumn(cname).setCellRenderer(rechtsAusrichter);
        }
        for (String cname : zentralColumns){
            myTable.getColumn(cname).setCellRenderer(zentralAusrichter);
        }
        // resize small columns:
        for (String cname : smallColumns){
            myTable.getColumn(cname).setPreferredWidth(minColumnWidth);
        }
    }

    public void updateAll() {
        this.remove(allPanel);
        this.revalidate();
        fillDataArray();
        showAll();
    }

    /** Needed for ItemListener. */
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
        if (source == inaktivCheckBox) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                aktivFilterStr = "";
                showInaktive = true;
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                aktivFilterStr = " AND artikel.aktiv = TRUE ";
                showInaktive = false;
            }
        } else if (source == sortimentCheckBox) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                sortimentFilterStr = " AND artikel.sortiment = TRUE ";
                showOnlySortiment = true;
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                sortimentFilterStr = "";
                showOnlySortiment = false;
            }
        } else if (source == internalCheckBox) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                showInternals = true;
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                showInternals = false;
            }
        }
    }

    /** Needed for ListSelectionListener.
     * Invoked when the row selection changes. */
    public void valueChanged(ListSelectionEvent e) {
        enableButtons();
    }

    /** Needed for TableModelListener. */
    public void tableChanged(TableModelEvent e) {
        // get info about edited cell
        int row = e.getFirstRow();
        int dataRow = displayIndices.get(row); // convert from displayData index to data index
        int column = e.getColumn();
        AbstractTableModel model = (AbstractTableModel)e.getSource();
        String origLieferant = originalData.get(dataRow).get(model.findColumn("Lieferant")).toString();
        String origArtikelNummer = originalData.get(dataRow).get(model.findColumn("Nummer")).toString();
        int nummerIndex = editArtikelNummer.indexOf(origArtikelNummer); // look up artikelNummer in change list
        int lieferantIndex = editLieferant.indexOf(origLieferant); // look up lieferant in change list

        //
        // post-edit edited cell (parse bad mistakes)
        //
        String value = model.getValueAt(row, column).toString().replaceAll("\\s","");
        if ( value.equals("") ){
            // replace whitespace only entries with nothing
            model.removeTableModelListener(this); // remove listener before doing changes
                model.setValueAt(value, row, column);
            model.addTableModelListener(this);
        }
        String header = model.getColumnName(column);
        if ( header.equals("Lieferant") && value.equals("") ){
            // user tried to delete the lieferant (not allowed)
            // reset to original value
            model.removeTableModelListener(this); // remove listener before doing changes
                model.setValueAt(origLieferant, row, column);
            model.addTableModelListener(this);
        }
        if ( header.equals("Nummer") && value.equals("") ){
            // user tried to delete the nummer (not allowed)
            // reset to original value
            model.removeTableModelListener(this); // remove listener before doing changes
                model.setValueAt(origArtikelNummer, row, column);
            model.addTableModelListener(this);
        }
        if ( header.equals("VK-Preis") && value.equals("") ){
            // user tried to delete the vkpreis (not allowed)
            // reset to original value
            model.removeTableModelListener(this); // remove listener before doing changes
                model.setValueAt(originalData.get(dataRow).get(column).toString(), row, column);
            model.addTableModelListener(this);
        }
        // get uniform formatting (otherwise e.g. change of only decimal symbol isn't ignored)
        if ( moneyColumns.contains(header) ){
            model.removeTableModelListener(this); // remove listener before doing changes
                model.setValueAt(priceFormatter(value), row, column);
            model.addTableModelListener(this);
        } else if ( decimalColumns.contains(header) ){
            value = unifyDecimal( model.getValueAt(row, column).toString() ); // uniformify decimal number
            model.removeTableModelListener(this); // remove listener before doing changes
                model.setValueAt(value, row, column);
            model.addTableModelListener(this);
        } else if ( header.equals("Beliebtheit") ){
            // make sure there is no unparseable entry (empty or just "-")
            try { Integer.parseInt(value); }
            catch (NumberFormatException ex) {
                model.removeTableModelListener(this); // remove listener before doing changes
                    model.setValueAt(0, row, column);
                model.addTableModelListener(this);
            }
        }

        // Compare entire row to original data
        boolean changed = false;
        for ( int col=0; col<data.get(dataRow).size(); col++){ // compare entire row to original data
            String colName = model.getColumnName(col);
            String val = data.get(dataRow).get(col).toString();
            String origVal = originalData.get(dataRow).get(col).toString();
            if ( ! val.equals( origVal ) ){
                changed = true;
                break;
            }
        }

        if (changed){
            // get and store all the values of the edited row
            String lieferant = model.getValueAt(row, model.findColumn("Lieferant")).toString();
            String artikelNummer = model.getValueAt(row, model.findColumn("Nummer")).toString();
            if ( !lieferant.equals(origLieferant) || !artikelNummer.equals(origArtikelNummer) ){
                if ( isItemAlreadyKnown(lieferant, artikelNummer) ){
                    // not allowed: changing name and nummer to a pair that is already registered in DB
                    JOptionPane.showMessageDialog(this, "Fehler: Kombination Lieferant/Nummer bereits vorhanden! Wird zurückgesetzt.",
                            "Info", JOptionPane.INFORMATION_MESSAGE);
                    model.removeTableModelListener(this); // remove listener before doing changes
                        model.setValueAt(origLieferant, row, model.findColumn("Lieferant"));
                        model.setValueAt(origArtikelNummer, row, model.findColumn("Nummer"));
                    model.addTableModelListener(this);
                    tableChanged(e);
                    return;
                }
            }
            String artikelName = model.getValueAt(row, model.findColumn("Name")).toString();
            String kurzname = model.getValueAt(row, model.findColumn("Kurzname")).toString();
            BigDecimal menge;
            try {
                menge = new BigDecimal(model.getValueAt(row, model.findColumn("Menge")).toString().replace(',','.'));
            } catch (NumberFormatException ex){ menge = null; }
            String einheit = model.getValueAt(row, model.findColumn("Einheit")).toString();
            String barcode = model.getValueAt(row, model.findColumn("Barcode")).toString();
            String herkunft = model.getValueAt(row, model.findColumn("Herkunft")).toString();
            Integer vpe;
            try {
                vpe = Integer.parseInt( model.getValueAt(row, model.findColumn("VPE")).toString() );
            } catch (NumberFormatException ex){ vpe = null; }
            Integer setgroesse;
            try {
                setgroesse = Integer.parseInt( model.getValueAt(row, model.findColumn("Setgröße")).toString() );
            } catch (NumberFormatException ex){ setgroesse = 1; }
            String vkpreis = model.getValueAt(row, model.findColumn("VK-Preis")).toString();
            String empfvkpreis = model.getValueAt(row, model.findColumn("Empf. VK-Preis")).toString();
            String ekrabatt = model.getValueAt(row, model.findColumn("EK-Rabatt")).toString();
            String ekpreis = model.getValueAt(row, model.findColumn("EK-Preis")).toString();
            boolean sortiment = model.getValueAt(row, model.findColumn("Sortiment")).toString().equals("true") ? true : false;
            boolean lieferbar = model.getValueAt(row, model.findColumn("Lieferbar")).toString().equals("true") ? true : false;
            Integer beliebt;
            try {
                beliebt = Integer.parseInt( model.getValueAt(row, model.findColumn("Beliebtheit")).toString() );
            } catch (NumberFormatException ex){ beliebt = null; }
            Integer bestand;
            try {
                bestand = Integer.parseInt( model.getValueAt(row, model.findColumn("Bestand")).toString() );
            } catch (NumberFormatException ex){ bestand = null; }
            boolean aktiv = model.getValueAt(row, model.findColumn("Aktiv")).toString().equals("true") ? true : false;

            // update the vectors caching the changes
            if (nummerIndex == lieferantIndex && nummerIndex != -1){ // this row has been changed before, update the change cache
                changedNummer.set(nummerIndex, artikelNummer);
                changedName.set(nummerIndex, artikelName);
                changedKurzname.set(nummerIndex, kurzname);
                changedMenge.set(nummerIndex, menge);
                changedEinheit.set(nummerIndex, einheit);
                changedBarcode.set(nummerIndex, barcode);
                changedHerkunft.set(nummerIndex, herkunft);
                changedVPE.set(nummerIndex, vpe);
                changedSetgroesse.set(nummerIndex, setgroesse);
                changedVKP.set(nummerIndex, vkpreis);
                changedEmpfVKP.set(nummerIndex, empfvkpreis);
                changedEKRabatt.set(nummerIndex, ekrabatt);
                changedEKP.set(nummerIndex, ekpreis);
                changedSortiment.set(nummerIndex, sortiment);
                changedLieferbar.set(nummerIndex, lieferbar);
                changedBeliebt.set(nummerIndex, beliebt);
                changedBestand.set(nummerIndex, bestand);
                changedAktiv.set(nummerIndex, aktiv);
            } else { // an edit occurred in a row that is not in the list of changes yet
                editLieferant.add(origLieferant);
                editArtikelNummer.add(origArtikelNummer);
                changedNummer.add(artikelNummer);
                changedName.add(artikelName);
                changedKurzname.add(kurzname);
                changedMenge.add(menge);
                changedEinheit.add(einheit);
                changedBarcode.add(barcode);
                changedHerkunft.add(herkunft);
                changedVPE.add(vpe);
                changedSetgroesse.add(setgroesse);
                changedVKP.add(vkpreis);
                changedEmpfVKP.add(empfvkpreis);
                changedEKRabatt.add(ekrabatt);
                changedEKP.add(ekpreis);
                changedSortiment.add(sortiment);
                changedLieferbar.add(lieferbar);
                changedBeliebt.add(beliebt);
                changedBestand.add(bestand);
                changedAktiv.add(aktiv);
            }
        } else if (!changed) {
            // update the vectors caching the changes
            if (nummerIndex == lieferantIndex && nummerIndex != -1){ // this row has been changed before, all changes undone
                editLieferant.remove(nummerIndex); // remove item from list of changes
                editArtikelNummer.remove(nummerIndex);
                changedNummer.remove(nummerIndex);
                changedName.remove(nummerIndex);
                changedKurzname.remove(nummerIndex);
                changedMenge.remove(nummerIndex);
                changedEinheit.remove(nummerIndex);
                changedBarcode.remove(nummerIndex);
                changedHerkunft.remove(nummerIndex);
                changedVPE.remove(nummerIndex);
                changedSetgroesse.remove(nummerIndex);
                changedVKP.remove(nummerIndex);
                changedEmpfVKP.remove(nummerIndex);
                changedEKRabatt.remove(nummerIndex);
                changedEKP.remove(nummerIndex);
                changedSortiment.remove(nummerIndex);
                changedLieferbar.remove(nummerIndex);
                changedBeliebt.remove(nummerIndex);
                changedBestand.remove(nummerIndex);
                changedAktiv.remove(nummerIndex);
            }
        }
        enableButtons();
    }

    void showEditDialog() {
        // get data from the selected rows
        Vector< Vector<Object> > selectedData = new Vector< Vector<Object> >();
        Vector<Integer> selectedProdGrIDs = new Vector<Integer>();
        Vector<Integer> selectedLiefIDs = new Vector<Integer>();
        Vector<Boolean> selectedVarPreisBools = new Vector<Boolean>();
        int[] selection = myTable.getSelectedRows();
        for (int i = 0; i < selection.length; i++) {
            selection[i] = myTable.convertRowIndexToModel(selection[i]);
            selection[i] = displayIndices.get(selection[i]); // convert from displayData index to data index
            selectedData.add( data.get(selection[i]) );
            selectedProdGrIDs.add( produktGruppeIDs.get(selection[i]) );
            selectedLiefIDs.add( lieferantIDs.get(selection[i]) );
            selectedVarPreisBools.add( varPreisBools.get(selection[i]) );
        }
        JDialog editDialog = new JDialog(this.mainWindow, "Artikel bearbeiten", true);
        ArtikelBearbeiten bearb = new ArtikelBearbeiten(this.conn, this.mainWindow, this, editDialog,
                selectedData, selectedProdGrIDs, selectedLiefIDs, selectedVarPreisBools);
        editDialog.getContentPane().add(bearb, BorderLayout.CENTER);
        editDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        WindowAdapterDialog wad = new WindowAdapterDialog(bearb, editDialog, "Achtung: Änderungen gehen verloren (noch nicht abgeschickt).\nWirklich schließen?");
        editDialog.addWindowListener(wad);
        editDialog.pack();
        editDialog.setVisible(true);
    }

    void showNewItemDialog() {
        JDialog newItemDialog = new JDialog(this.mainWindow, "Neue Artikel hinzufügen", true);
        ArtikelNeuEingeben newItems = new ArtikelNeuEingeben(this.conn, this.mainWindow, this, newItemDialog, toplevel_id, sub_id, subsub_id);
        newItemDialog.getContentPane().add(newItems, BorderLayout.CENTER);
        newItemDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        WindowAdapterDialog wad = new WindowAdapterDialog(newItems, newItemDialog, "Achtung: Neue Artikel gehen verloren (noch nicht abgeschickt).\nWirklich schließen?");
        newItemDialog.addWindowListener(wad);
        newItemDialog.pack();
        newItemDialog.setVisible(true);
    }

    void showReadFromFileDialog() {
        readFromFileDialog = new JDialog(this.mainWindow, "Artikel aus Datei einlesen", true);
        ArtikelImport itemsFromFile = new ArtikelImport(this.conn, this.mainWindow, this, readFromFileDialog);
        readFromFileDialog.getContentPane().add(itemsFromFile, BorderLayout.CENTER);
        readFromFileDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        WindowAdapterDialog wad = new WindowAdapterDialog(itemsFromFile, readFromFileDialog, "Achtung: Neue Artikel gehen verloren (noch nicht abgeschickt).\nWirklich schließen?");
        readFromFileDialog.addWindowListener(wad);
        readFromFileDialog.pack();
        readFromFileDialog.setVisible(true);
    }

    void showExportDialog() {
        ArtikelExport itemsToFile = new ArtikelExport(this.conn, this.mainWindow, this);
    }

    int changeLossConfirmDialog() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Achtung: Änderungen gehen verloren. Fortfahren?", "Änderungen werden gelöscht",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return answer;
    }

    /**
     *    * Each non abstract class that implements the DocumentListener
     *      must have these methods.
     *
     *    @param e the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
        if (e.getDocument() == filterField.getDocument()){
            filterStr = filterField.getText();
            applyFilter();
            updateTable();
        }
    }
    public void removeUpdate(DocumentEvent e) {
        insertUpdate(e);
    }
    public void changedUpdate(DocumentEvent e) {
	// Plain text components do not fire these events
    }

    private void initiateDisplayIndices() {
        displayIndices = new Vector<Integer>();
        for (int i=0; i<data.size(); i++){
            displayIndices.add(i);
        }
    }

    private void applyFilter() {
        displayData = new Vector< Vector<Object> >(data);
        initiateDisplayIndices();
        if (filterStr.length() == 0){
            return;
        }
        for (int i=0; i<data.size(); i++){
            boolean contains = false;
            for (Object obj : data.get(i)){
                String str;
                try {
                    str = (String) obj;
                    str = str.toLowerCase();
                } catch (ClassCastException ex) {
                    str = "";
                }
                if (str.contains(filterStr.toLowerCase())){
                    contains = true;
                    break;
                }
            }
            if (!contains){
                int display_index = displayIndices.indexOf(i);
                displayData.remove(display_index);
                displayIndices.remove(display_index);
            }
        }
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == backButton){
            container.switchToProduktgruppenliste();
            return;
        }
        if (e.getSource() == saveButton){
            putChangesIntoDB();
            updateAll();
            return;
        }
        if (e.getSource() == revertButton){
            updateAll();
            return;
        }
        if (e.getSource() == editButton){
            showEditDialog();
            return;
        }
        if (e.getSource() == newButton){
            showNewItemDialog();
            return;
        }
        if (e.getSource() == importButton){
            showReadFromFileDialog();
            return;
        }
        if (e.getSource() == exportButton){
            showExportDialog();
            return;
        }
        if (e.getSource() == inaktivCheckBox){
            if ( editLieferant.size() > 0 ){
                int answer = changeLossConfirmDialog();
                if (answer == JOptionPane.YES_OPTION){
                    updateAll();
                } else {
                    inaktivCheckBox.setSelected(!showInaktive);
                }
            } else {
                updateAll();
            }
            return;
        }
        if (e.getSource() == sortimentCheckBox){
            if ( editLieferant.size() > 0 ){
                int answer = changeLossConfirmDialog();
                if (answer == JOptionPane.YES_OPTION){
                    updateAll();
                } else {
                    sortimentCheckBox.setSelected(!showOnlySortiment);
                }
            } else {
                updateAll();
            }
            return;
        }
        if (e.getSource() == internalCheckBox){
            if ( editLieferant.size() > 0 ){
                int answer = changeLossConfirmDialog();
                if (answer == JOptionPane.YES_OPTION){
                    updateAll();
                } else {
                    internalCheckBox.setSelected(!showInternals);
                }
            } else {
                updateAll();
            }
            return;
        }
    }
}
