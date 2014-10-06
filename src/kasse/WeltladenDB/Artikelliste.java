package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// GUI stuff:
//import java.awt.BorderLayout;
//import java.awt.FlowLayout;
//import java.awt.Dimension;
import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
import java.awt.event.*;

//import javax.swing.JFrame;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTable;
//import javax.swing.JTextArea;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.event.*; // for TableModelListener
import javax.swing.text.*; // for DocumentFilter

public class Artikelliste extends WindowContent implements ItemListener, TableModelListener,
       ListSelectionListener, DocumentListener {
    // Attribute:
    private ArtikellisteContainer container;
    private Integer toplevel_id;
    private Integer sub_id;
    private Integer subsub_id;
    private String gruppenname;

    private JPanel allPanel;
    private JPanel artikelListPanel;
    private JScrollPane scrollPane;
    private JButton backButton;
    private JCheckBox inaktivCheckBox;
    private boolean showInaktive = false;
    private JCheckBox internalCheckBox;
    private boolean showInternals = false;
    private JTextField filterField;
    private JButton saveButton;
    private JButton revertButton;
    private JButton editButton;
    private JButton newButton;
    private JButton importButton;
    private JButton exportButton;

    private String filterStr = "";
    private String aktivFilterStr = " AND artikel.aktiv = TRUE ";
    //private String orderByStr = "artikel_name";
    private String orderByStr = "p.toplevel_id, p.sub_id, p.subsub_id, artikel_name";

    // The table holding the items
    private JTable myTable;
    private Vector< Vector<Object> > data;
    protected Vector< Vector<Object> > originalData;
    protected Vector< Vector<Object> > displayData;
    protected Vector<Integer> displayIndices;
    private Vector<String> columnLabels;
    protected Vector<Boolean> sortimentBools;
    protected Vector<Boolean> activeRowBools;
    protected Vector<Boolean> varPreisBools;
    private Vector<Integer> produktGruppeIDs;
    private Vector<Integer> lieferantIDs;

    // Vectors storing table edits
    private Vector<String> editLieferant;
    private Vector<String> editArtikelNummer;
    private Vector<String> changedNummer;
    private Vector<String> changedName;
    private Vector<BigDecimal> changedMenge;
    private Vector<String> changedBarcode;
    private Vector<String> changedHerkunft;
    private Vector<Integer> changedVPE;
    private Vector<String> changedVKP;
    private Vector<String> changedEKP;
    private Vector<Boolean> changedSortiment;
    private Vector<Boolean> changedAktiv;

    // Dialog to read items from file
    private JDialog readFromFileDialog;
    //private ArtikelImport itemsFromFile;

    // Methoden:
    protected Artikelliste(Connection conn, ArtikellisteContainer ac, Integer tid, Integer sid, Integer ssid, String gn) {
        super(conn, ac.getMainWindowPointer());

        this.container = ac;
        this.toplevel_id = tid;
        this.sub_id = sid;
        this.subsub_id = ssid;
        this.gruppenname = gn;

        fillDataArray();
        showAll();
    }

    private void fillDataArray() {
        this.data = new Vector< Vector<Object> >();
        columnLabels = new Vector<String>();
        columnLabels.add("Produktgruppe"); columnLabels.add("Lieferant");
        columnLabels.add("Nummer"); columnLabels.add("Name");
        columnLabels.add("Menge"); columnLabels.add("Barcode");
        columnLabels.add("Herkunft"); columnLabels.add("VPE");
        columnLabels.add("VK-Preis"); columnLabels.add("EK-Preis");
        columnLabels.add("MwSt."); //columnLabels.add("Betrag MwSt.");
        columnLabels.add("Ab/Seit"); columnLabels.add("Bis");
        columnLabels.add("Sortiment"); columnLabels.add("Aktiv");
        sortimentBools = new Vector<Boolean>();
        activeRowBools = new Vector<Boolean>();
        varPreisBools = new Vector<Boolean>();
        produktGruppeIDs = new Vector<Integer>();
        lieferantIDs = new Vector<Integer>();

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
                    "SELECT produktgruppen_id, produktgruppen_name, "+
                    "lieferant_id, lieferant_name, "+
                    "artikel_nr, artikel_name, "+
                    "menge, barcode, "+
                    "herkunft, vpe, "+
                    "vk_preis, ek_preis, variabler_preis, mwst_satz, "+
                    "DATE_FORMAT(von, '"+dateFormatSQL+"'), DATE_FORMAT(bis, '"+dateFormatSQL+"'), "+
                    "sortiment, artikel.aktiv "+
                    "FROM artikel LEFT JOIN lieferant USING (lieferant_id) "+
                    "LEFT JOIN produktgruppe AS p USING (produktgruppen_id) "+
                    "LEFT JOIN mwst USING (mwst_id) "+
                    "WHERE " + filter +
                    aktivFilterStr +
                    "ORDER BY " + orderByStr
                    );
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Integer produktgruppen_id = rs.getInt(1);
                String gruppenname = rs.getString(2);
                Integer lieferant_id = rs.getInt(3);
                String lieferant = rs.getString(4);
                String nr = rs.getString(5);
                String name = rs.getString(6);
                String menge = rs.getString(7) == null ? null :
                    rs.getBigDecimal(7).stripTrailingZeros().toPlainString();
                String barcode = rs.getString(8);
                String herkunft = rs.getString(9);
                String vpe = rs.getString(10);
                String vkp = rs.getString(11);
                String ekp = rs.getString(12);
                Boolean var = rs.getBoolean(13);
                String mwst = rs.getString(14);
                String mwstBetrag = "";
                String von = rs.getString(15);
                String bis = rs.getString(16);
                Boolean sortimentBool = rs.getBoolean(17);
                Boolean aktivBool = rs.getBoolean(18);

                if (lieferant_id == null) lieferant_id = 1; // corresponds to "unknown"
                if (lieferant == null) lieferant = "";
                if (menge == null){ menge = ""; }
                if (barcode == null){ barcode = ""; }
                if (herkunft == null) herkunft = "";
                if (vpe == null){ vpe = ""; }
                String vkpOutput = "";
                if (vkp != null){ vkpOutput = priceFormatter(vkp)+" "+currencySymbol; }
                if (ekp == null){ ekp = ""; }
                else { ekp = priceFormatter(ekp)+" "+currencySymbol; }
                String mwstOutput = "";
                if (mwst != null){ mwstOutput = vatFormatter(mwst); }
                if (vkp == null){ vkpOutput = ""; mwstBetrag = ""; }
                if (mwst == null){ mwstOutput = ""; mwstBetrag = ""; }
                else if (vkp != null && mwst != null) {
                    mwstBetrag = priceFormatter( calculateVAT(new BigDecimal(vkp), new BigDecimal(mwst)) )+" "+currencySymbol;
                }
                if (var == true){ vkpOutput = "variabel"; ekp = "variabel"; mwstBetrag = "variabel"; }
                if (von == null) von = "";
                if (bis == null) bis = "";

                Vector<Object> row = new Vector<Object>();
                    row.add(gruppenname); row.add(lieferant);
                    row.add(nr); row.add(name);
                    row.add(menge.replace('.', ',')); row.add(barcode);
                    row.add(herkunft); row.add(vpe);
                    row.add(vkpOutput); row.add(ekp);
                    row.add(mwstOutput); //row.add(mwstBetrag);
                    row.add(von); row.add(bis);
                    row.add(sortimentBool); row.add(aktivBool);
                data.add(row);
                produktGruppeIDs.add(produktgruppen_id);
                lieferantIDs.add(lieferant_id);
                sortimentBools.add(sortimentBool);
                activeRowBools.add(aktivBool);
                varPreisBools.add(var);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        this.originalData = new Vector< Vector<Object> >();
        for ( Vector<Object> row : data ){
            Vector<Object> originalRow = new Vector<Object>();
            originalRow.addAll(row);
            originalData.add(originalRow);
        }
        displayData = new Vector< Vector<Object> >(data);
        initiateDisplayIndices();
        editLieferant = new Vector<String>();
        editArtikelNummer = new Vector<String>();
        changedNummer = new Vector<String>();
        changedName = new Vector<String>();
        changedMenge = new Vector<BigDecimal>();
        changedBarcode = new Vector<String>();
        changedHerkunft = new Vector<String>();
        changedVPE = new Vector<Integer>();
        changedVKP = new Vector<String>();
        changedEKP = new Vector<String>();
        changedSortiment = new Vector<Boolean>();
        changedAktiv = new Vector<Boolean>();
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
                result = insertNewItem(prod_id, lief_id,
                        changedNummer.get(index), changedName.get(index),
                        changedMenge.get(index), changedBarcode.get(index),
                        changedHerkunft.get(index), changedVPE.get(index),
                        changedVKP.get(index), changedEKP.get(index),
                        var_preis, changedSortiment.get(index));
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

    void showAll() {
        allPanel = new JPanel();
        allPanel.setLayout(new BorderLayout());

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

        showTable();

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

        enableButtons();

        this.add(allPanel, BorderLayout.CENTER);
    }

    void enableButtons() {
        saveButton.setEnabled(editLieferant.size() > 0);
        revertButton.setEnabled(editLieferant.size() > 0);
        editButton.setEnabled(myTable.getSelectedRowCount() > 0);
        newButton.setEnabled(editLieferant.size() == 0);
        importButton.setEnabled(editLieferant.size() == 0);
        exportButton.setEnabled(editLieferant.size() == 0);
    }

    void initiateTable() {
        myTable = new JTable( new AbstractTableModel() { // subclass the AbstractTableModel to set editable cells etc.
            public String getColumnName(int col) {
                return columnLabels.get(col);
            }
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
                return displayData.get(row).get(col);
            }
            public Class getColumnClass(int c) { // JTable uses this method to determine the default renderer/editor for each cell.
                                                 // If we didn't implement this method, then the last column would contain text ("true"/"false"),
                                                 // rather than a check box.
                return getValueAt(0, c).getClass();
            }
            public boolean isCellEditable(int row, int col) {
                String header = this.getColumnName(col);
                if ( activeRowBools.get(row) ){
                    if ( header.equals("VK-Preis") || header.equals("EK-Preis") ) {
                        if ( ! displayData.get(row).get(col).equals("variabel") )
                            return true;
                    }
                    else if (
                            header.equals("Nummer") || header.equals("Name") ||
                            header.equals("Menge") || header.equals("Barcode") ||
                            header.equals("Herkunft") || header.equals("VPE") ||
                            header.equals("Sortiment") || header.equals("Aktiv")
                            ){
                        return true;
                    }
                }
                return false;
            }
            public void setValueAt(Object value, int row, int col) {
                Vector<Object> rowentries = displayData.get(row);
                rowentries.set(col, value);
                displayData.set(row, rowentries);
                int dataRow = displayIndices.get(row); // convert from displayData index to data index
                data.set(dataRow, rowentries);
                fireTableCellUpdated(row, col);
            }
        } ) { // subclass the JTable to set font properties and tool tip text
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                // add custom rendering here
                int realRowIndex = convertRowIndexToModel(row);
                realRowIndex = displayIndices.get(realRowIndex); // convert from displayData index to data index
                if ( ! activeRowBools.get(realRowIndex) ){ // for rows with inactive items
                    c.setFont( c.getFont().deriveFont(Font.ITALIC) );
                    c.setForeground(Color.BLUE);
                }
                else if ( ! sortimentBools.get(realRowIndex) ){ // for articles not in sortiment
                    c.setFont( c.getFont().deriveFont(Font.PLAIN) );
                    c.setForeground(Color.GRAY);
                }
                else {
                    c.setFont( c.getFont().deriveFont(Font.PLAIN) );
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
            // Implement table cell tool tips.
            public String getToolTipText(MouseEvent e) {
                Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                int realRowIndex = convertRowIndexToModel(rowIndex); // user might have changed row order
                int realColIndex = convertColumnIndexToModel(colIndex); // user might have changed column order
                String tip = this.getModel().getValueAt(realRowIndex, realColIndex).toString();
                return tip;
            }
            // Implement table header tool tips.
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        Point p = e.getPoint();
                        int colIndex = columnAtPoint(p);
                        int realColIndex = convertColumnIndexToModel(colIndex); // user might have changed column order
                        tip = columnLabels.get(realColIndex);
                        return tip;
                    }
                };
            }
        };
        myTable.setAutoCreateRowSorter(true);
        myTable.getModel().addTableModelListener(this);
        myTable.getSelectionModel().addListSelectionListener(this);
        //myTable.setDefaultRenderer( JComponent.class, new JComponentCellRenderer() );
        //myTable.setDefaultEditor( JComponent.class, new JComponentCellEditor() );
        setTableProperties(myTable);

        // extra cell editor that has the NumberDocumentFilter
        class NumberEditor extends DefaultCellEditor {
            JTextField textField;

            public NumberEditor() {
                super(new JTextField()); // call to super must be first statement in constructor
                textField = (JTextField)getComponent();
                NumberDocumentFilter numFilter = new NumberDocumentFilter(5, 8);
                ((AbstractDocument)textField.getDocument()).setDocumentFilter(numFilter);
            }
        }
        NumberEditor numberEditor = new NumberEditor();
        myTable.getColumn("Menge").setCellEditor(numberEditor);

        // extra cell editor that has the IntegerDocumentFilter
        class AnzahlEditor extends DefaultCellEditor {
            JTextField textField;

            public AnzahlEditor() {
                super(new JTextField()); // call to super must be first statement in constructor
                textField = (JTextField)getComponent();
                IntegerDocumentFilter intFilter = new IntegerDocumentFilter();
                ((AbstractDocument)textField.getDocument()).setDocumentFilter(intFilter);
            }
        }
        AnzahlEditor anzahlEditor = new AnzahlEditor();
        myTable.getColumn("VPE").setCellEditor(anzahlEditor);

        // extra cell editor that has the CurrencyDocumentFilter
        class GeldEditor extends DefaultCellEditor {
            JTextField textField;

            public GeldEditor() {
                super(new JTextField()); // call to super must be first statement in constructor
                textField = (JTextField)getComponent();
                NumberDocumentFilter geldFilter = new NumberDocumentFilter(2, 13);
                ((AbstractDocument)textField.getDocument()).setDocumentFilter(geldFilter);
            }

            //Override to invoke setText on the document filtered text field.
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                    int row, int column) {
                this.textField.setText(""); // delete history of the (old) text field, maybe from a different cell
                JTextField newTextField = (JTextField)super.getTableCellEditorComponent(table, value, isSelected, row, column);
                //newTextField.setText(value.toString()); // if this line is present, then the
                                                // DocumentFilter is called twice (not good)
                return newTextField;
            }
        }
        GeldEditor geldEditor = new GeldEditor();
        myTable.getColumn("VK-Preis").setCellEditor(geldEditor);
        myTable.getColumn("EK-Preis").setCellEditor(geldEditor);
    }

    void showTable() {
        initiateTable();

        artikelListPanel = new JPanel();
        artikelListPanel.setLayout(new BorderLayout());
        artikelListPanel.setBorder(BorderFactory.createTitledBorder(this.gruppenname));

        scrollPane = new JScrollPane(myTable);
        artikelListPanel.add(scrollPane, BorderLayout.CENTER);
        allPanel.add(artikelListPanel, BorderLayout.CENTER);
    }

    void updateTable() {
        artikelListPanel.remove(scrollPane);
	artikelListPanel.revalidate();
        initiateTable();
        scrollPane = new JScrollPane(myTable);
        artikelListPanel.add(scrollPane);
        enableButtons();
    }

    void setTableProperties(JTable myTable) {
        myTable.getColumn("Produktgruppe").setCellRenderer(linksAusrichter);
        myTable.getColumn("Lieferant").setCellRenderer(linksAusrichter);
        myTable.getColumn("Nummer").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("Name").setCellRenderer(linksAusrichter);
        myTable.getColumn("Menge").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("Barcode").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("Herkunft").setCellRenderer(linksAusrichter);
        myTable.getColumn("VPE").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("VK-Preis").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("EK-Preis").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("MwSt.").setCellRenderer(rechtsAusrichter);
        //myTable.getColumn("Betrag MwSt.").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("Ab/Seit").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("Bis").setCellRenderer(rechtsAusrichter);

        myTable.getColumn("Produktgruppe").setPreferredWidth(70);
        myTable.getColumn("Lieferant").setPreferredWidth(70);
        myTable.getColumn("Nummer").setPreferredWidth(70);
        myTable.getColumn("Name").setPreferredWidth(100);
        myTable.getColumn("Menge").setPreferredWidth(30);
        myTable.getColumn("Barcode").setPreferredWidth(70);
        myTable.getColumn("Herkunft").setPreferredWidth(100);
        myTable.getColumn("VPE").setPreferredWidth(10);
        myTable.getColumn("VK-Preis").setPreferredWidth(30);
        myTable.getColumn("EK-Preis").setPreferredWidth(30);
        myTable.getColumn("MwSt.").setPreferredWidth(20);
        //myTable.getColumn("Betrag MwSt.").setPreferredWidth(30);
        myTable.getColumn("Ab/Seit").setPreferredWidth(70);
        myTable.getColumn("Bis").setPreferredWidth(70);
        myTable.getColumn("Sortiment").setPreferredWidth(20);
        myTable.getColumn("Aktiv").setPreferredWidth(20);
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
            //Now that we know which button was pushed, find out
            //whether it was selected or deselected.
            if (e.getStateChange() == ItemEvent.SELECTED) {
                aktivFilterStr = "";
                showInaktive = true;
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                aktivFilterStr = " AND artikel.aktiv = TRUE ";
                showInaktive = false;
            }
        } else if (source == internalCheckBox) {
            //Now that we know which button was pushed, find out
            //whether it was selected or deselected.
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

        // post-edit edited cell
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
            model.setValueAt(origLieferant, row, column);
        }
        if ( header.equals("Nummer") && value.equals("") ){
            // user tried to delete the nummer (not allowed)
            // reset to original value
            model.setValueAt(origArtikelNummer, row, column);
        }
        value = model.getValueAt(row, column).toString().replace(currencySymbol,"")
            .replaceAll("\\s","").replace(',','.');
        if ( header.equals("VK-Preis") || header.equals("EK-Preis") ){
            if ( !value.equals("") ){
                // format the entered money value appropriately
                value = priceFormatter(value)+" "+currencySymbol;
                model.removeTableModelListener(this); // remove listener before doing changes
                model.setValueAt(value, row, column); // update table cell with currency symbol
                model.addTableModelListener(this);
            } else {
                if ( header.equals("VK-Preis") ){
                    // user tried to delete the vkpreis (not allowed)
                    // reset to original value
                    model.setValueAt(originalData.get(dataRow).get(column).toString(), row, column);
                }
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
                    model.setValueAt(origLieferant, row, model.findColumn("Lieferant"));
                    model.setValueAt(origArtikelNummer, row, model.findColumn("Nummer"));
                    return;
                }
            }
            String artikelName = model.getValueAt(row, model.findColumn("Name")).toString();
            BigDecimal menge;
            try {
                menge = new BigDecimal( model.getValueAt(row, model.findColumn("Menge")).toString().replace(',', '.') );
            } catch (NumberFormatException ex){ menge = null; }
            String barcode = model.getValueAt(row, model.findColumn("Barcode")).toString();
            String herkunft = model.getValueAt(row, model.findColumn("Herkunft")).toString();
            Integer vpe;
            try {
                vpe = Integer.parseInt( model.getValueAt(row, model.findColumn("VPE")).toString() );
            } catch (NumberFormatException ex){ vpe = null; }
            String vkpreis = model.getValueAt(row, model.findColumn("VK-Preis")).toString();
            String ekpreis = model.getValueAt(row, model.findColumn("EK-Preis")).toString();
            boolean sortiment = model.getValueAt(row, model.findColumn("Sortiment")).toString().equals("true") ? true : false;
            boolean aktiv = model.getValueAt(row, model.findColumn("Aktiv")).toString().equals("true") ? true : false;

            // update the vectors caching the changes
            if (nummerIndex == lieferantIndex && nummerIndex != -1){ // this row has been changed before, update the change cache
                changedNummer.set(nummerIndex, artikelNummer);
                changedName.set(nummerIndex, artikelName);
                changedMenge.set(nummerIndex, menge);
                changedBarcode.set(nummerIndex, barcode);
                changedHerkunft.set(nummerIndex, herkunft);
                changedVPE.set(nummerIndex, vpe);
                changedVKP.set(nummerIndex, vkpreis);
                changedEKP.set(nummerIndex, ekpreis);
                changedSortiment.set(nummerIndex, sortiment);
                changedAktiv.set(nummerIndex, aktiv);
            } else { // an edit occurred in a row that is not in the list of changes yet
                editLieferant.add(origLieferant);
                editArtikelNummer.add(origArtikelNummer);
                changedNummer.add(artikelNummer);
                changedName.add(artikelName);
                changedMenge.add(menge);
                changedBarcode.add(barcode);
                changedHerkunft.add(herkunft);
                changedVPE.add(vpe);
                changedVKP.add(vkpreis);
                changedEKP.add(ekpreis);
                changedSortiment.add(sortiment);
                changedAktiv.add(aktiv);
            }
        } else if (!changed) {
            // update the vectors caching the changes
            if (nummerIndex == lieferantIndex && nummerIndex != -1){ // this row has been changed before, all changes undone
                editLieferant.remove(nummerIndex); // remove item from list of changes
                editArtikelNummer.remove(nummerIndex);
                changedNummer.remove(nummerIndex);
                changedName.remove(nummerIndex);
                changedMenge.remove(nummerIndex);
                changedBarcode.remove(nummerIndex);
                changedHerkunft.remove(nummerIndex);
                changedVPE.remove(nummerIndex);
                changedVKP.remove(nummerIndex);
                changedEKP.remove(nummerIndex);
                changedSortiment.remove(nummerIndex);
                changedAktiv.remove(nummerIndex);
            }
        }

        enableButtons();
    }

    private class WindowAdapterArtikelDialog extends WindowAdapter {
        private ArtikelDialogWindowGrundlage dwindow;
        private JDialog dialog;
        private String warnMessage;
        public WindowAdapterArtikelDialog(ArtikelDialogWindowGrundlage adw, JDialog dia, String wm) {
            super();
            this.dwindow = adw;
            this.dialog = dia;
            this.warnMessage = wm;
        }
        @Override
        public void windowClosing(WindowEvent we) {
            if ( this.dwindow.willDataBeLost() ){
                int answer = JOptionPane.showConfirmDialog(dialog,
                        warnMessage, "Warnung",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (answer == JOptionPane.YES_OPTION){
                    dialog.dispose();
                } else {
                    // do nothing
                }
            } else {
                dialog.dispose();
            }
        }
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
        WindowAdapterArtikelDialog waad = new WindowAdapterArtikelDialog(bearb, editDialog, "Achtung: Änderungen gehen verloren (noch nicht abgeschickt).\nWirklich schließen?");
        editDialog.addWindowListener(waad);
        editDialog.pack();
        editDialog.setVisible(true);
    }

    void showNewItemDialog() {
        JDialog newItemDialog = new JDialog(this.mainWindow, "Neue Artikel hinzufügen", true);
        ArtikelNeuEingeben newItems = new ArtikelNeuEingeben(this.conn, this.mainWindow, this, newItemDialog, toplevel_id, sub_id, subsub_id);
        newItemDialog.getContentPane().add(newItems, BorderLayout.CENTER);
        newItemDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        WindowAdapterArtikelDialog waad = new WindowAdapterArtikelDialog(newItems, newItemDialog, "Achtung: Neue Artikel gehen verloren (noch nicht abgeschickt).\nWirklich schließen?");
        newItemDialog.addWindowListener(waad);
        newItemDialog.pack();
        newItemDialog.setVisible(true);
    }

    void showReadFromFileDialog() {
        readFromFileDialog = new JDialog(this.mainWindow, "Artikel aus Datei einlesen", true);
        ArtikelImport itemsFromFile = new ArtikelImport(this.conn, this.mainWindow, this, readFromFileDialog);
        readFromFileDialog.getContentPane().add(itemsFromFile, BorderLayout.CENTER);
        readFromFileDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        WindowAdapterArtikelDialog waad = new WindowAdapterArtikelDialog(itemsFromFile, readFromFileDialog, "Achtung: Neue Artikel gehen verloren (noch nicht abgeschickt).\nWirklich schließen?");
        readFromFileDialog.addWindowListener(waad);
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
