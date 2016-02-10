package org.weltladen_bonn.pos;

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

public class Artikelliste extends ArtikelGrundlage implements ItemListener,
       TableModelListener, ListSelectionListener, DocumentListener {
    // Attribute:
    private ArtikellisteContainer container;
    protected Integer toplevel_id = 1;
    protected Integer sub_id = 3;
    protected Integer subsub_id = null;
    protected String produktgruppenname = "Sonstiges 19% MwSt (0)";
    private String searchStr = null;

    protected JPanel allPanel;
    protected JPanel artikelListPanel;
    protected JScrollPane scrollPane;
    protected JButton backButton;
    protected JCheckBox inaktivCheckBox;
    protected boolean showInaktive = false;
    protected JCheckBox sortimentCheckBox;
    protected boolean showOnlySortiment = false;
    protected JCheckBox internalCheckBox;
    protected boolean showInternals = false;
    protected JTextField filterField;
    private JButton emptyFilterButton;

    private String filterStr = "";
    private String aktivFilterStr = " AND artikel.aktiv = TRUE ";
    private String sortimentFilterStr = "";
    //private String orderByStr = "artikel_name";
    private String orderByStr = "p.toplevel_id, p.sub_id, p.subsub_id, artikel_nr";

    // The table holding the items
    protected AnyJComponentJTable myTable;
    protected Vector< Vector<Object> > data;
    protected Vector< Vector<Object> > originalData;
    protected Vector< Vector<Object> > displayData;
    protected Vector<Integer> displayIndices;
    protected Vector<String> columnLabels;
    protected HashMap<String, Integer> indexMap;

    protected Vector<Artikel> articles;
    protected Vector<Integer> artikelIDs;

    protected Vector<String> linksColumns;
    protected Vector<String> rechtsColumns;
    protected Vector<String> zentralColumns;
    protected Vector<String> smallColumns;
    protected Vector<String> editableColumns;
    protected Vector<String> moneyColumns;
    protected Vector<String> decimalColumns;

    protected HashMap<String, DocumentFilter> documentFilterMap;

    // Vectors storing table edits
    protected Vector<Artikel> editedArticles;
    private Vector<Artikel> changedArticles;

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

    protected Artikelliste(Connection conn, ArtikellisteContainer ac, String searchStr) {
        super(conn, ac.getMainWindowPointer());
        this.container = ac;
        this.searchStr = searchStr;
        this.produktgruppenname = "Suchergebnis (0)";

        fillDataArray();
        showAll();
    }

    protected Artikelliste(Connection conn, ArtikellisteContainer ac) {
        super(conn, ac.getMainWindowPointer());
        this.container = ac;

        fillDataArray();
        showAll();
    }

    protected Artikelliste(Connection conn, MainWindowGrundlage mwp) {
        super(conn, mwp);
    }

    private PreparedStatement prepareStatement(String filter) {
        String queryStr =
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
            "DATE_FORMAT(von, '"+bc.dateFormatSQL+"'), DATE_FORMAT(bis, '"+bc.dateFormatSQL+"'), "+
            "artikel.aktiv "+
            "FROM artikel LEFT JOIN lieferant USING (lieferant_id) "+
            "LEFT JOIN produktgruppe AS p USING (produktgruppen_id) "+
            "LEFT JOIN mwst USING (mwst_id) "+
            "WHERE " + filter +
            aktivFilterStr +
            sortimentFilterStr +
            "ORDER BY " + orderByStr;
        PreparedStatement pstmt = null;
        //System.out.println("Artikelliste SQL query string:\n"+queryStr);
        try {
            pstmt = this.conn.prepareStatement(queryStr);
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return pstmt;
    }

    private PreparedStatement prepareStatementProduktgruppe() {
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
        return prepareStatement(filter);
    }

    private PreparedStatement prepareStatementSearchString() {
        String[] words = searchStr.split("\\s+");
        String baseClause = "(produktgruppen_name LIKE ? OR lieferant_name LIKE ? OR "+
            "artikel_nr LIKE ? OR artikel_name LIKE ? OR kurzname LIKE ? OR "+
            "einheit LIKE ? OR barcode LIKE ? OR herkunft LIKE ?) ";
        int nQuestionMark = 8;
        String filter = "";
        if (words.length > 0){
            filter += baseClause;
        }
        for (int i=1; i<words.length; i++){
            filter += "AND "+baseClause;
        }
        PreparedStatement pstmt = null;
        try {
            pstmt = prepareStatement(filter);
            for (int i=0; i<words.length; i++) {
                for (int j=i*nQuestionMark+1; j<=(i+1)*nQuestionMark; j++) {
                    pstmt.setString(j, "%"+words[i]+"%");
                    //System.out.println("Setting string "+j+" on pstmt to: %"+words[i]+"%");
                }
            }
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return pstmt;
    }

    private void queryDatabase(PreparedStatement pstmt) {
        try {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Integer artikel_id = rs.getInt(1);
                Integer produktgruppen_id = rs.getInt(2);
                String gruppenname = rs.getString(3);
                Integer lieferant_id = rs.getInt(4);
                String lieferant = rs.getString(5);
                String nummer = rs.getString(6);
                String name = rs.getString(7);
                String kurzname = rs.getString(8);
                BigDecimal menge = rs.getBigDecimal(9);
                String einheit = rs.getString(10);
                String barcode = rs.getString(11);
                String herkunft = rs.getString(12);
                Integer vpe = rs.getString(13) == null ? null : rs.getInt(13);
                Integer setgroesse = rs.getString(14) == null ? null : rs.getInt(14);
                String vkp = rs.getString(15);
                String empf_vkp = rs.getString(16);
                String ek_rabatt = bc.vatFormatter( rs.getString(17) );
                String ekp = rs.getString(18);
                Boolean var = rs.getBoolean(19);
                String mwst = rs.getString(20);
                Boolean sortiment = rs.getBoolean(21);
                Boolean lieferbar = rs.getBoolean(22);
                Integer beliebtWert = rs.getInt(23);
                Integer bestand = rs.getString(24) == null ? null : rs.getInt(24);
                String von = rs.getString(25);
                String bis = rs.getString(26);
                Boolean aktiv = rs.getBoolean(27);

                if (lieferant_id == null) lieferant_id = 1; // corresponds to "unknown"
                if (lieferant == null) lieferant = "";
                if (kurzname == null) kurzname = "";
                String mengeStr = "";
                if (menge != null){ mengeStr = bc.unifyDecimal(menge); }
                if (einheit == null){ einheit = ""; }
                if (barcode == null){ barcode = ""; }
                if (herkunft == null) herkunft = "";
                String vpeStr = "";
                if (vpe != null){ vpeStr = vpe.toString(); }
                String setgrStr = "";
                if (setgroesse != null){ setgrStr = setgroesse.toString(); }
                String vkpOutput = "";
                if (vkp != null){ vkpOutput = bc.priceFormatter(vkp); }
                String empfVKPOutput = "";
                if (empf_vkp != null){ empfVKPOutput = bc.priceFormatter(empf_vkp); }
                String ekRabattOutput = "";
                if (ek_rabatt != null){ ekRabattOutput = bc.vatPercentRemover(ek_rabatt); }
                String ekpOutput = "";
                if (ekp != null){ ekpOutput = bc.priceFormatter(ekp); }
                String mwstOutput = "";
                if (mwst != null){ mwstOutput = bc.vatFormatter(mwst); }
                if (var == true){ vkpOutput = "variabel"; empfVKPOutput = "variabel";
                    ekRabattOutput = "variabel"; ekpOutput = "variabel"; }
                Integer beliebtIndex = 0;
                if (beliebtWert != null){
                    try {
                        beliebtIndex = bc.beliebtWerte.indexOf(beliebtWert);
                    } catch (ArrayIndexOutOfBoundsException ex){
                        System.out.println("Unknown beliebtWert: "+beliebtWert);
                    }
                }
                String bestandStr = "";
                if (bestand != null){ bestandStr = bestand.toString(); }
                if (von == null) von = "";
                if (bis == null) bis = "";

                Vector<Object> row = new Vector<Object>();
                    row.add(gruppenname);
                    row.add(lieferant); row.add(nummer);
                    row.add(name); row.add(kurzname);
                    row.add(mengeStr); row.add(einheit);
                    row.add(vkpOutput);
                    row.add(sortiment);
                    row.add(lieferbar);
                    row.add(beliebtWert); row.add(barcode);
                    row.add(vpeStr); row.add(setgrStr);
                    row.add(empfVKPOutput); row.add(ekRabattOutput);
                    row.add(ekpOutput); row.add(mwstOutput);
                    row.add(herkunft); row.add(bestandStr);
                    row.add(von); row.add(bis);
                    row.add(aktiv);
                data.add(row);

                Artikel article = new Artikel(bc, produktgruppen_id, lieferant_id,
                        nummer, name, kurzname, menge, einheit, barcode,
                        herkunft, vpe, setgroesse, vkp, empf_vkp, ek_rabatt,
                        ekp, var, sortiment, lieferbar, beliebtWert, bestand,
                        aktiv);
                articles.add(article);
                artikelIDs.add(artikel_id);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
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
        columnLabels.add("Bis");
        columnLabels.add("Aktiv");

        indexMap = new HashMap<String, Integer>();
        indexMap.put("produktgruppe", columnLabels.indexOf("Produktgruppe"));
        indexMap.put("lieferant", columnLabels.indexOf("Lieferant"));
        indexMap.put("nummer", columnLabels.indexOf("Nummer"));
        indexMap.put("name", columnLabels.indexOf("Name"));
        indexMap.put("kurzname", columnLabels.indexOf("Kurzname"));
        indexMap.put("menge", columnLabels.indexOf("Menge"));
        indexMap.put("einheit", columnLabels.indexOf("Einheit"));
        indexMap.put("vkp", columnLabels.indexOf("VK-Preis"));
        indexMap.put("sortiment", columnLabels.indexOf("Sortiment"));
        indexMap.put("lieferbar", columnLabels.indexOf("Lieferbar"));
        indexMap.put("beliebt", columnLabels.indexOf("Beliebtheit"));
        indexMap.put("barcode", columnLabels.indexOf("Barcode"));
        indexMap.put("vpe", columnLabels.indexOf("VPE"));
        indexMap.put("setgroesse", columnLabels.indexOf("Setgröße"));
        indexMap.put("evkp", columnLabels.indexOf("Empf. VK-Preis"));
        indexMap.put("ekr", columnLabels.indexOf("EK-Rabatt"));
        indexMap.put("ekp", columnLabels.indexOf("EK-Preis"));
        indexMap.put("herkunft", columnLabels.indexOf("Herkunft"));
        indexMap.put("bestand", columnLabels.indexOf("Bestand"));
        indexMap.put("aktiv", columnLabels.indexOf("Aktiv"));

        articles = new Vector<Artikel>();
        artikelIDs = new Vector<Integer>();

        linksColumns = new Vector<String>();
        linksColumns.add("Nummer");
        linksColumns.add("Produktgruppe"); linksColumns.add("Lieferant");
        linksColumns.add("Name"); linksColumns.add("Kurzname");
        linksColumns.add("Herkunft"); linksColumns.add("Ab/Seit");
        linksColumns.add("Bis");
        //
        rechtsColumns = new Vector<String>();
        rechtsColumns.add("Menge");
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

        documentFilterMap = new HashMap<String, DocumentFilter>();
        documentFilterMap.put("Nummer", bc.nummerFilter);
        documentFilterMap.put("Name", bc.nameFilter);
        documentFilterMap.put("Kurzname", bc.kurznameFilter);
        documentFilterMap.put("Menge", bc.mengeFilter);
        documentFilterMap.put("Einheit", bc.einheitFilter);
        for (String cname : moneyColumns){
            documentFilterMap.put(cname, bc.geldFilter);
        }
        documentFilterMap.put("Beliebtheit", bc.beliebtFilter);
        documentFilterMap.put("Barcode", bc.nummerFilter);
        documentFilterMap.put("VPE", bc.vpeFilter);
        documentFilterMap.put("Setgröße", bc.vpeFilter);
        documentFilterMap.put("EK-Rabatt", bc.relFilter);
        documentFilterMap.put("Herkunft", bc.herkunftFilter);
        documentFilterMap.put("Bestand", bc.intFilter);

        PreparedStatement pstmt;
        if (this.searchStr == null) {
            pstmt = prepareStatementProduktgruppe();
        } else {
            pstmt = prepareStatementSearchString();
        }
        queryDatabase(pstmt);
        refreshOriginalData();
        displayData = new Vector< Vector<Object> >(data);
        initiateDisplayIndices();
        editedArticles = new Vector<Artikel>();
        changedArticles = new Vector<Artikel>();
    }

    protected void refreshOriginalData() {
        this.originalData = new Vector< Vector<Object> >();
        for ( Vector<Object> row : data ){
            Vector<Object> originalRow = new Vector<Object>();
            originalRow.addAll(row);
            originalData.add(originalRow);
        }
    }

    protected void putChangesIntoDB() {
        for (int index=0; index < editedArticles.size(); index++){
            Artikel origArticle = editedArticles.get(index);
            Artikel newArticle = changedArticles.get(index);
            updateArticle(origArticle, newArticle);
        }
    }


    protected void showAll() {
        allPanel = new JPanel();
        allPanel.setLayout(new BorderLayout());

        showTopPanel();
        showTable();

        container.enableButtons();

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
          emptyFilterButton = new JButton("x");
          emptyFilterButton.addActionListener(this);
          topRightPanel.add(emptyFilterButton);
        topPanel.add(topRightPanel, BorderLayout.EAST);
        allPanel.add(topPanel, BorderLayout.NORTH);
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
            int realRowIndex = convertRowIndexToModel(row);
            int articleIndex = realRowIndex;
            try {
                articleIndex = displayIndices.get(realRowIndex); // convert from displayData index to data index
            } catch (ArrayIndexOutOfBoundsException ex){
                System.out.println("No display data at row "+realRowIndex);
                System.out.println("No information about editability in isCellEditable().");
                return false;
            }
            /*
            System.out.println("is cell editable? row "+row+", realRowIndex "+realRowIndex+", articleIndex "+articleIndex);
            System.out.println("is cell editable? displayData at realRowIndex: "+displayData.get(realRowIndex));
            System.out.println("is cell editable? articles at articleIndex: "+articles.get(articleIndex).getName()+", "+articles.get(articleIndex).getKurzname());
            */
            String header = this.getColumnName(col);
            if ( articles.get(articleIndex).getAktiv() ){
                if ( moneyColumns.contains(header) || header.equals("EK-Rabatt") ) {
                    if ( ! displayData.get(realRowIndex).get(col).equals("variabel") )
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
            Object value = this.getValueAt(row, column); // here, no conversion must be done (don't really get why, because tool tip and articleIndex need it)
            //System.out.println("prepare renderer row: "+row+" value: "+value);
            int realRowIndex = convertRowIndexToModel(row);
            int articleIndex = realRowIndex;
            try {
                articleIndex = displayIndices.get(realRowIndex); // convert from displayData index to data index
            } catch (ArrayIndexOutOfBoundsException ex){
                System.out.println("No display data at row "+realRowIndex);
                System.out.println("No special rendering possible.");
                return c;
            }
            /*
            System.out.println("prepare renderer: row "+row+", realRowIndex "+realRowIndex+", articleIndex "+articleIndex);
            System.out.println("prepare renderer: displayData at realRowIndex: "+displayData.get(realRowIndex));
            System.out.println("prepare renderer: articles at articleIndex: "+articles.get(articleIndex).getName()+", "+articles.get(articleIndex).getKurzname());
            */
            // for rows with inactive items, set color:
            if ( ! articles.get(articleIndex).getAktiv() ){
                c.setFont( c.getFont().deriveFont(Font.ITALIC) );
                c.setForeground(Color.BLUE);
            }
            // for articles not in sortiment, set color:
            else if ( ! articles.get(articleIndex).getSortiment() ){
                c.setFont( c.getFont().deriveFont(Font.PLAIN) );
                c.setForeground(Color.GRAY);
            }
            else {
                c.setFont( c.getFont().deriveFont(Font.PLAIN) );
                c.setForeground(Color.BLACK);
            }
            // override the above if this is the "Beliebtheit" column:
            if ( this.getColumnName(column).equals("Beliebtheit") ){
                Integer index = bc.beliebtWerte.indexOf( Integer.parseInt(value.toString()) );
                c.setFont( c.getFont().deriveFont(Font.PLAIN) );
                c.setForeground( bc.beliebtFarben.get(index) );
            }
            // now, render the text:
            if (c instanceof JLabel){
                JLabel label = (JLabel)c;
                String cname = this.getColumnName(column);
                String valueStr = "";
                if ( cname.equals("Beliebtheit") ){
                    // BeliebtRenderer:
                    Integer index = bc.beliebtWerte.indexOf( Integer.parseInt(value.toString()) );
                    label.setText( bc.beliebtKuerzel.get(index) );
                }
                if ( moneyColumns.contains(cname) ){
                    // GeldRenderer:
                    if (value != null){
                        valueStr = value.toString();
                        if ( !valueStr.equals("variabel") ){
                            valueStr = bc.priceFormatter(valueStr);
                            if ( !valueStr.equals("") ){
                                valueStr += " "+bc.currencySymbol;
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
                                    bc.vatParser(bc.vatFormatter(valueStr))).multiply(bc.percent);
                            valueStr = bc.vatFormatter(fracValue);
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
                JTextField textField = (JTextField)c;
                String cname = this.getColumnName(column);

                if ( documentFilterMap.containsKey(cname) ){
                    DocumentFilter filter = documentFilterMap.get(cname);
                    ((AbstractDocument)textField.getDocument()).setDocumentFilter(filter);
                }

                // select all text in TextField:
                textField.selectAll();
            }
            return c;
        }

        @Override
        public void removeEditor() {
            // remove the DocumentFilter as well
            Component c = this.getEditorComponent();
            if (c instanceof JTextField){
                JTextField textField = (JTextField)c;
                ((AbstractDocument)textField.getDocument()).setDocumentFilter(null);
            }
            super.removeEditor();
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            String defaultTip = super.getToolTipText(e);
            Point p = e.getPoint();
            int colIndex = columnAtPoint(p);
            // override the default tool tip if this is the "Beliebtheit" column:
            if ( this.getColumnName(colIndex).equals("Beliebtheit") ){
                int rowIndex = rowAtPoint(p);
                // conversion of row index to model would lead to wrong results
                // when sorting is used (seems like the conversion is done
                // internally already)
                /*
                int realRowIndex = convertRowIndexToModel(rowIndex);
                System.out.println("prepare renderer: rowIndex "+rowIndex+", realRowIndex "+realRowIndex);
                System.out.println("prepare renderer: displayData at rowIndex: "+displayData.get(rowIndex));
                System.out.println("prepare renderer: displayData at realRowIndex: "+displayData.get(realRowIndex));
                */
                Integer value = Integer.parseInt(this.getValueAt(rowIndex, colIndex).toString());
                Integer index = bc.beliebtWerte.indexOf(value);
                String name = bc.beliebtNamen.get(index);
                return name+" ("+value+")";
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

    protected void updateTable() {
        // does not work correctly when table is sorted and filter is used:
        /*
        artikelListPanel.remove(scrollPane);
	artikelListPanel.revalidate();

        applyFilter(filterStr, displayData, displayIndices);

        scrollPane = new JScrollPane(myTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        artikelListPanel.add(scrollPane);
        // replace general number of items with displayed number of items:
        String borderLabel = this.produktgruppenname.
            replaceAll(" \\([0-9]*\\)$", " ("+displayData.size()+")");
        artikelListPanel.setBorder(BorderFactory.createTitledBorder(borderLabel));
        */
        allPanel.remove(artikelListPanel);
        allPanel.revalidate();

        applyFilter(filterStr, displayData, displayIndices);

        showTable();
        container.enableButtons();
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
        // new, much better, keeping view:
        fillDataArray();
        updateTable();
        container.updateTree();
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
        container.enableButtons();
    }


    private void parseCell(AbstractTableModel model, int row, int column, int dataRow,
            Artikel origArticle) {
        /**
         * post-edit edited cell (parse bad mistakes)
         */
        // the row index and column index given to this function are already converted to model
        /*
        System.out.println("parse cell: row "+row+", col "+column);
        System.out.println("parse cell: dataRow "+dataRow);
        System.out.println("parse cell: model at row and col: "+model.getValueAt(row, column));
        System.out.println("parse cell: data at dataRow "+data.get(dataRow));
        System.out.println("parse cell: origArticle: "+origArticle.getName()+", "+
                origArticle.getKurzname()+", "+origArticle.getVKP()+", "+origArticle.getEKP());
        */

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
                model.setValueAt(getLieferant(origArticle.getLiefID()), row, column);
            model.addTableModelListener(this);
        }
        if ( header.equals("Nummer") && value.equals("") ){
            // user tried to delete the nummer (not allowed)
            // reset to original value
            model.removeTableModelListener(this); // remove listener before doing changes
                model.setValueAt(origArticle.getNummer(), row, column);
            model.addTableModelListener(this);
        }
        if ( header.equals("VK-Preis") && value.equals("") ){
            // user tried to delete the vkpreis (not allowed)
            // reset to original value
            model.removeTableModelListener(this); // remove listener before doing changes
                model.setValueAt(originalData.get(dataRow).get(column).toString(),
                        row, column);
            model.addTableModelListener(this);
        }
        // get uniform formatting (otherwise e.g. change of only decimal symbol isn't ignored)
        if ( moneyColumns.contains(header) ){
            model.removeTableModelListener(this); // remove listener before doing changes
                model.setValueAt(bc.priceFormatter(value), row, column);
            model.addTableModelListener(this);
        } else if ( decimalColumns.contains(header) ){
            value = bc.unifyDecimal( model.getValueAt(row, column).toString() ); // uniformify decimal number
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
    }


    private boolean hasRowChanged(int dataRow) {
        /**
         * Compare entire row to original data
         */
        boolean changed = false;
        for ( int col=0; col<data.get(dataRow).size(); col++){ // compare entire row to original data
            String val = data.get(dataRow).get(col).toString();
            String origVal = originalData.get(dataRow).get(col).toString();
            if ( ! val.equals( origVal ) ){
                changed = true;
                break;
            }
        }
        return changed;
    }


    private Artikel getNewArticle(AbstractTableModel model, int row, Artikel origArticle,
            TableModelEvent e) {
        /**
         * Get and store all the values of the edited row
         */
        Artikel a = new Artikel(bc);
        a.setProdGrID(origArticle.getProdGrID());
        a.setVarPreis(origArticle.getVarPreis());

        String lieferant = model.getValueAt(row, model.findColumn("Lieferant")).toString();
        Integer liefID = getLieferantID(lieferant);
        a.setLiefID(liefID);
        String artikelNummer = model.getValueAt(row, model.findColumn("Nummer")).toString();
        a.setNummer(artikelNummer);
        if ( !liefID.equals(origArticle.getLiefID()) || !artikelNummer.equals(origArticle.getNummer()) ){
            if ( isArticleAlreadyKnown(liefID, artikelNummer) ){
                // not allowed: changing name and nummer to a pair that is already registered in DB
                JOptionPane.showMessageDialog(this,
                        "Fehler: Kombination Lieferant/Nummer bereits vorhanden! Wird zurückgesetzt.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                model.removeTableModelListener(this); // remove listener before doing changes
                    model.setValueAt(getLieferant(origArticle.getLiefID()), row, model.findColumn("Lieferant"));
                    model.setValueAt(origArticle.getNummer(), row, model.findColumn("Nummer"));
                model.addTableModelListener(this);
                tableChanged(e);
                return null;
            }
        }
        a.setName( model.getValueAt(row, model.findColumn("Name")).toString() );
        a.setKurzname( model.getValueAt(row, model.findColumn("Kurzname")).toString() );
        BigDecimal menge;
        try {
            menge = new BigDecimal(model.getValueAt(row, model.findColumn("Menge")).toString().replace(',','.'));
            //menge = new BigDecimal( unifyDecimalIntern(model.getValueAt(row, model.findColumn("Menge")).toString()) );
        } catch (NumberFormatException ex){ menge = null; }
        a.setMenge(menge);
        a.setEinheit( model.getValueAt(row, model.findColumn("Einheit")).toString() );
        a.setBarcode( model.getValueAt(row, model.findColumn("Barcode")).toString() );
        a.setHerkunft( model.getValueAt(row, model.findColumn("Herkunft")).toString() );
        Integer vpe;
        try {
            vpe = Integer.parseInt( model.getValueAt(row, model.findColumn("VPE")).toString() );
        } catch (NumberFormatException ex){ vpe = null; }
        a.setVPE(vpe);
        Integer setgroesse;
        try {
            setgroesse = Integer.parseInt( model.getValueAt(row, model.findColumn("Setgröße")).toString() );
        } catch (NumberFormatException ex){ setgroesse = 1; }
        a.setSetgroesse(setgroesse);
        a.setVKP( model.getValueAt(row, model.findColumn("VK-Preis")).toString() );
        a.setEmpfVKP( model.getValueAt(row, model.findColumn("Empf. VK-Preis")).toString() );
        a.setEKRabatt( model.getValueAt(row, model.findColumn("EK-Rabatt")).toString() );
        a.setEKP( model.getValueAt(row, model.findColumn("EK-Preis")).toString() );
        boolean sortiment = model.getValueAt(row,
                model.findColumn("Sortiment")).toString().equals("true") ? true : false;
        a.setSortiment(sortiment);
        boolean lieferbar = model.getValueAt(row,
                model.findColumn("Lieferbar")).toString().equals("true") ? true : false;
        a.setLieferbar(lieferbar);
        Integer beliebt;
        try {
            beliebt = Integer.parseInt( model.getValueAt(row, model.findColumn("Beliebtheit")).toString() );
        } catch (NumberFormatException ex){ beliebt = bc.beliebtNamen.indexOf("keine Angabe"); }
        a.setBeliebt(beliebt);
        Integer bestand;
        try {
            bestand = Integer.parseInt( model.getValueAt(row, model.findColumn("Bestand")).toString() );
        } catch (NumberFormatException ex){ bestand = null; }
        a.setBestand(bestand);
        boolean aktiv = model.getValueAt(row,
                model.findColumn("Aktiv")).toString().equals("true") ? true : false;
        a.setAktiv(aktiv);

        return a;
    }


    /** Needed for TableModelListener. */
    public void tableChanged(TableModelEvent e) {
        // get info about edited cell
        int row = e.getFirstRow();
        System.out.println("first edited row: "+row);
        // index is already converted to model, i.e. displayData index
        //int realRowIndex = model.convertRowIndexToModel(row);
        int dataRow = displayIndices.get(row); // convert from displayData index to data index
        int column = e.getColumn();
        AbstractTableModel model = (AbstractTableModel)e.getSource();

        Integer origArtikelID = artikelIDs.get(dataRow);
        Artikel origArticle = getArticle(origArtikelID);
        int changeIndex = editedArticles.indexOf(origArticle); // look up artikelNummer in change list
        System.out.println("changeIndex: "+changeIndex);

        parseCell(model, row, column, dataRow, origArticle);

        boolean changed = hasRowChanged(dataRow);

        if (changed){
            Artikel newArticle = getNewArticle(model, row, origArticle, e);
            if (newArticle == null)
                return;

            // update the vectors caching the changes
            if (changeIndex != -1){ // this row has been changed before, update the change cache
                changedArticles.set(changeIndex, newArticle);
            } else { // an edit occurred in a row that is not in the list of changes yet
                editedArticles.add(origArticle);
                changedArticles.add(newArticle);
            }
        } else if (!changed) {
            // update the vectors caching the changes
            if (changeIndex != -1){ // this row has been changed before, all changes undone
                editedArticles.remove(changeIndex);
                changedArticles.remove(changeIndex);
            }
        }
        container.enableButtons();
    }


    protected int numberOfSelectedInactiveArticles() {
        int count = 0;
        int[] selection = myTable.getSelectedRows();
        for (int i = 0; i < selection.length; i++) {
            // index must be converted first to model (displayData), then to
            // data/articles
            selection[i] = myTable.convertRowIndexToModel(selection[i]);
            selection[i] = displayIndices.get(selection[i]); // convert from displayData index to data/articles index
            if ( !articles.get(selection[i]).getAktiv() ) {
                count++;
            }
        }
        return count;
    }


    void showEditDialog() {
        // get data from the selected rows
        Vector<Artikel> selectedArticles = new Vector<Artikel>();
        int[] selection = myTable.getSelectedRows();
        for (int i = 0; i < selection.length; i++) {
            // index must be converted first to model (displayData), then to
            // data/articles
            //System.out.println("show edit dialog: selection[i] index "+selection[i]);
            selection[i] = myTable.convertRowIndexToModel(selection[i]);
            //System.out.println("show edit dialog: selection[i] index converted to model "+selection[i]);
            selection[i] = displayIndices.get(selection[i]); // convert from displayData index to data/articles index
            //System.out.println("show edit dialog: selection[i] index converted to data/articles index "+selection[i]);
            selectedArticles.add( articles.get(selection[i]) );
            //System.out.println("show edit dialog: data at selection[i]: "+data.get(selection[i]));
            //System.out.println("show edit dialog: articles at selection[i]: "+articles.get(selection[i]).getName()+", "+
            //        articles.get(selection[i]).getKurzname()+", "+articles.get(selection[i]).getVKP()+", "+articles.get(selection[i]).getEKP());
        }
        JDialog editDialog = new JDialog(this.mainWindow, "Artikel bearbeiten", true);
        ArtikelBearbeiten bearb = new ArtikelBearbeiten(this.conn, this.mainWindow, this, editDialog,
                selectedArticles);
        editDialog.getContentPane().add(bearb, BorderLayout.CENTER);
        editDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        WindowAdapterDialog wad = new WindowAdapterDialog(bearb, editDialog,
                "Achtung: Änderungen gehen verloren (noch nicht abgeschickt).\nWirklich schließen?");
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

    void showNewSimilarItemDialog() {
        // get data from the selected row
        int index = myTable.getSelectedRows()[0];
        // index must be converted first to model (displayData), then to
        // data/articles
        index = myTable.convertRowIndexToModel(index);
        index = displayIndices.get(index); // convert from displayData index to data/articles index
        Artikel selectedArticle = articles.get(index);
        JDialog newItemDialog = new JDialog(this.mainWindow, "Ähnliche Artikel hinzufügen", true);
        ArtikelNeuEingeben newItems = new ArtikelNeuEingeben(this.conn, this.mainWindow, this, newItemDialog, selectedArticle.getProdGrID());
        newItems.setOriginalValues(selectedArticle);
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
        ArtikelExport itemsToFile = new ArtikelExport(this.conn, this.mainWindow, this, indexMap);
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
            String oldFilterStr = new String(filterStr);
            filterStr = filterField.getText();
            if ( !filterStr.contains(oldFilterStr) ){
                // user has deleted from, not added to the filter string, reset the displayData
                displayData = new Vector< Vector<Object> >(data);
                initiateDisplayIndices();
            }
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

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == backButton){
            if ( editedArticles.size() > 0 ){
                int answer = changeLossConfirmDialog();
                if (answer == JOptionPane.YES_OPTION){
                    container.switchToProduktgruppenliste();
                } else {
                    // do nothing, stay in this view
                }
            } else {
                container.switchToProduktgruppenliste();
            }
            return;
        }
        if (e.getSource() == inaktivCheckBox){
            if ( editedArticles.size() > 0 ){
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
            if ( editedArticles.size() > 0 ){
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
            if ( editedArticles.size() > 0 ){
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
        if (e.getSource() == emptyFilterButton){
            filterField.setText("");
            filterField.requestFocus();
	    return;
	}
    }
}
