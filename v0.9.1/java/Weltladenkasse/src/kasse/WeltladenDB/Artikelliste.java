package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
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

public class Artikelliste extends WindowContent implements ItemListener, TableModelListener, ListSelectionListener {
    // Attribute:
    private ArtikellisteContainer container;
    private String toplevel_id;
    private String sub_id;
    private String subsub_id;
    private String gruppenname;

    private JPanel allPanel;
    private JButton backButton;
    private JCheckBox inaktivCheckBox;
    private boolean showInaktive = false;
    private JCheckBox internalCheckBox;
    private boolean showInternals = false;
    private JTextField searchField;
    private JButton searchButton;
    private JButton stopButton;
    private JButton saveButton;
    private JButton revertButton;
    private JButton editButton;
    private JButton newButton;
    private JButton importButton;
    private JButton exportButton;

    private String filterStr = "";
    private String aktivFilterStr = " AND artikel.aktiv = TRUE ";
    private String orderByStr = "artikel_name";

    // The table holding the items
    private JTable myTable;
    private Vector< Vector<Object> > data;
    public Vector< Vector<Object> > originalData;
    private Vector<String> columnLabels;
    private Vector<Boolean> activeRowBools;
    public Vector<Boolean> varPreisBools;
    private Vector<String> produktGruppeIDs;
    private Vector<String> lieferantIDs;

    // Vectors storing table edits
    private Vector<String> editArtikelName;
    private Vector<String> editArtikelNummer;
    private Vector<String> changedName;
    private Vector<String> changedNummer;
    private Vector<String> changedBarcode;
    private Vector<String> changedVKP;
    private Vector<String> changedEKP;
    private Vector<String> changedVPE;
    private Vector<String> changedHerkunft;
    private Vector<Boolean> changedAktiv;

    // Dialog to read items from file
    private JDialog readFromFileDialog;
    //private ArtikelImport itemsFromFile;

    // Methoden:
    public Artikelliste(Connection conn, ArtikellisteContainer ac, String tid, String sid, String ssid, String gn) {
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
        columnLabels.add("Name"); columnLabels.add("Nummer"); columnLabels.add("Barcode");
        columnLabels.add("Produktgruppe");
        columnLabels.add("VK-Preis"); columnLabels.add("EK-Preis"); columnLabels.add("VPE");
        columnLabels.add("MwSt."); //columnLabels.add("Betrag MwSt.");
        columnLabels.add("Ab/Seit"); columnLabels.add("Bis");
        columnLabels.add("Lieferant"); columnLabels.add("Herkunft"); columnLabels.add("Aktiv");
        activeRowBools = new Vector<Boolean>();
        varPreisBools = new Vector<Boolean>();
        produktGruppeIDs = new Vector<String>();
        lieferantIDs = new Vector<String>();

        String filter = "";
        if (toplevel_id == null){ // if user clicked on "Alle Artikel"
            if (showInternals)
                filter = "TRUE "; // to show all items, also internal ones (where toplevel_id == null)
            else
                filter = "produktgruppe.toplevel_id > 0 ";
        } else {
            filter = "produktgruppe.toplevel_id = " + toplevel_id + " ";
        }
        if (sub_id != null)
            filter += " AND produktgruppe.sub_id = " + sub_id + " ";
        if (subsub_id != null)
            filter += " AND produktgruppe.subsub_id = " + subsub_id + " ";
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT produktgruppen_id, produktgruppen_name, "+
                    "artikel_name, artikel_nr, barcode, "+
                    "vk_preis, variabler_preis, ek_preis, vpe, mwst_satz, "+
                    "DATE_FORMAT(von, '"+dateFormatSQL+"'), DATE_FORMAT(bis, '"+dateFormatSQL+"'), "+
                    "lieferant_id, lieferant_name, "+
                    "herkunft, artikel.aktiv "+
                    "FROM artikel LEFT JOIN lieferant USING (lieferant_id) "+
                    "LEFT JOIN produktgruppe USING (produktgruppen_id) "+
                    "LEFT JOIN mwst USING (mwst_id) "+
                    "WHERE " + filter +
                    "AND ( artikel_name LIKE '%" + filterStr + "%' OR artikel_nr LIKE '%" + filterStr + "%' OR lieferant_name LIKE '%" + filterStr + "%' " +
                    "OR herkunft LIKE '%" + filterStr + "%' ) " +
                    aktivFilterStr +
                    "ORDER BY " + orderByStr
                    );
            while (rs.next()) {
                Vector<Object> row = new Vector<Object>();
                String produktgruppen_id = rs.getString(1);
                String gruppenname = rs.getString(2);
                String name = rs.getString(3);
                String nr = rs.getString(4);
                String barcode = rs.getString(5);
                String vkp = rs.getString(6);
                String var = rs.getString(7);
                String ekp = rs.getString(8);
                String vpe = rs.getString(9);
                String mwst = rs.getString(10);
                String mwstBetrag = "";
                String von = rs.getString(11);
                String bis = rs.getString(12);
                String lieferant_id = rs.getString(13);
                String lieferant = rs.getString(14);
                String herkunft = rs.getString(15);
                Boolean aktivBool = rs.getBoolean(16);

                if (barcode == null){ barcode = ""; }
                if (vpe == null){ vpe = ""; }
                if (ekp == null){ ekp = ""; }
                else { ekp = priceFormatter(ekp)+" "+currencySymbol; }
                String vkpOutput = "";
                String mwstOutput = "";
                if (vkp != null){ vkpOutput = priceFormatter(vkp)+" "+currencySymbol; }
                if (mwst != null){ mwstOutput = vatFormatter(mwst); }
                if (vkp == null){ vkpOutput = ""; mwstBetrag = ""; }
                if (mwst == null){ mwstOutput = ""; mwstBetrag = ""; }
                if (var.equals("1")){ vkpOutput = "variabel"; ekp = "variabel"; mwstBetrag = "variabel"; }
                else if (vkp != null && mwst != null) {
                    mwstBetrag = priceFormatter( calculateVAT(new BigDecimal(vkp), new BigDecimal(mwst)) )+" "+currencySymbol;
                }
                if (von == null) von = "";
                if (bis == null) bis = "";
                if (lieferant_id == null) lieferant_id = "1";
                if (lieferant == null) lieferant = "";
                if (herkunft == null) herkunft = "";

                row.add(name); row.add(nr); row.add(barcode);
                row.add(gruppenname);
                row.add(vkpOutput); row.add(ekp); row.add(vpe);
                row.add(mwstOutput); //row.add(mwstBetrag);
                row.add(von); row.add(bis);
                row.add(lieferant); row.add(herkunft); row.add(aktivBool);
                data.add(row);
                activeRowBools.add(aktivBool);
                varPreisBools.add(var.equals("1"));
                produktGruppeIDs.add(produktgruppen_id);
                lieferantIDs.add(lieferant_id);
            }
            rs.close();
            stmt.close();
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
        editArtikelName = new Vector<String>();
        editArtikelNummer = new Vector<String>();
        changedName = new Vector<String>();
        changedNummer = new Vector<String>();
        changedBarcode = new Vector<String>();
        changedVKP = new Vector<String>();
        changedEKP = new Vector<String>();
        changedVPE = new Vector<String>();
        changedHerkunft = new Vector<String>();
        changedAktiv = new Vector<Boolean>();
    }

    private void putChangesIntoDB() {
        for (int index=0; index<editArtikelName.size(); index++){
            String prod_id = "";
            String lief_id = "";
            String var_preis = "";
            try {
                Statement stmt = this.conn.createStatement();
                // query produktgruppen_id, lieferant_id and variabler_preis for edited item:
                ResultSet rs = stmt.executeQuery(
                        "SELECT produktgruppen_id, lieferant_id, variabler_preis FROM artikel WHERE "+
                        "artikel_name = \""+editArtikelName.get(index)+"\" AND "+
                        "artikel_nr = \""+editArtikelNummer.get(index)+"\" AND aktiv = TRUE"
                        );
                // Now do something with the ResultSet, should be only one result ...
                rs.next();
                prod_id = rs.getString(1);
                lief_id = rs.getString(2);
                var_preis = rs.getString(3);
                rs.close();
                stmt.close();
            } catch (SQLException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }

            // set old item to inactive:
            int result = setItemInactive(editArtikelName.get(index), editArtikelNummer.get(index));
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Artikel "+editArtikelName.get(index)+" mit Nummer "+editArtikelNummer.get(index)+" konnte nicht geändert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                continue; // continue with next item
            }
            if ( changedAktiv.get(index) == true ){ // only if the item wasn't set inactive voluntarily: add new item with new properties
                result = insertNewItem(editArtikelName.get(index), editArtikelNummer.get(index), changedBarcode.get(index),
                        var_preis, changedVKP.get(index), changedEKP.get(index), changedVPE.get(index),
                        prod_id, lief_id, changedHerkunft.get(index));
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel "+editArtikelName.get(index)+" mit Nummer "+editArtikelNummer.get(index)+" konnte nicht geändert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    result = setItemActive(editArtikelName.get(index), editArtikelNummer.get(index));
                    if (result == 0){
                        JOptionPane.showMessageDialog(this,
                                "Fehler: Artikel "+editArtikelName.get(index)+" mit Nummer "+editArtikelNummer.get(index)+" konnte nicht wieder hergestellt werden. Artikel ist nun gelöscht (inaktiv).",
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
          JLabel searchFieldLabel = new JLabel("Filter");
          searchField = new JTextField(filterStr);
          searchField.setColumns(20);
          searchField.addKeyListener(new KeyAdapter() {
              public void keyPressed(KeyEvent e) {
                  if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
                      searchButton.doClick();
                  }
              }
          });
          searchButton = new JButton("Los!");
          searchButton.addActionListener(this);
          stopButton = new JButton("Stop");
          stopButton.addActionListener(this);

          topRightPanel.add(searchFieldLabel);
          topRightPanel.add(searchField);
          topRightPanel.add(searchButton);
          topRightPanel.add(stopButton);
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
        saveButton.setEnabled(editArtikelName.size() > 0);
        revertButton.setEnabled(editArtikelName.size() > 0);
        editButton.setEnabled(myTable.getSelectedRowCount() > 0);
        newButton.setEnabled(editArtikelName.size() == 0);
        importButton.setEnabled(editArtikelName.size() == 0);
        exportButton.setEnabled(editArtikelName.size() == 0);
    }

    void showTable() {
        JPanel artikelListPanel = new JPanel();
        artikelListPanel.setLayout(new BorderLayout());
        artikelListPanel.setBorder(BorderFactory.createTitledBorder(this.gruppenname));

        myTable = new JTable(new AbstractTableModel() { // subclass the AbstractTableModel to set editable cells etc.
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
            public int getRowCount() { return data.size(); }
            public int getColumnCount() { return columnLabels.size(); }
            public Object getValueAt(int row, int col) {
                return data.get(row).get(col);
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
                        if ( ! data.get(row).get(col).equals("variabel") )
                            return true;
                    }
                    else if (
                            header.equals("Name") || header.equals("Nummer") ||
                            header.equals("Barcode") || header.equals("VPE") ||
                            header.equals("Herkunft") || header.equals("Aktiv")
                            ){
                        return true;
                    }
                }
                return false;
            }
            public void setValueAt(Object value, int row, int col) {
                Vector<Object> rowentries = data.get(row);
                rowentries.set(col, value);
                data.set(row, rowentries);
                fireTableCellUpdated(row, col);
            }
        } ) { // subclass the JTable to set font properties and tool tip text
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                // add custom rendering here
                int realRowIndex = convertRowIndexToModel(row);
                if ( ! activeRowBools.get(realRowIndex) ){ // for rows with inactive items
                    c.setFont( c.getFont().deriveFont(Font.ITALIC) );
                    c.setForeground(Color.GRAY);
                }
                else {
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

        // extra cell editor that has the CurrencyDocumentFilter
        class GeldEditor extends DefaultCellEditor {
            JTextField textField;

            public GeldEditor() {
                super(new JTextField()); // call to super must be first statement in constructor
                textField = (JTextField)getComponent();
                CurrencyDocumentFilter geldFilter = new CurrencyDocumentFilter();
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

        // extra cell editor that has the IntegerDocumentFilter
        class AnzahlEditor extends DefaultCellEditor {
            JTextField textField;

            public AnzahlEditor() {
                super(new JTextField()); // call to super must be first statement in constructor
                textField = (JTextField)getComponent();
                IntegerDocumentFilter intFilter = new IntegerDocumentFilter();
                ((AbstractDocument)textField.getDocument()).setDocumentFilter(intFilter);
            }

            ////Override to invoke setText on the document filtered text field.
            //@Override
            //public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            //        int row, int column) {
            //    this.textField.setText(""); // delete history of the (old) text field, maybe from a different cell
            //    JTextField newTextField = (JTextField)super.getTableCellEditorComponent(table, value, isSelected, row, column);
            //    //newTextField.setText(value.toString()); // if this line is present, then the
            //                                    // DocumentFilter is called twice (not good)
            //    return newTextField;
            //}
        }
        AnzahlEditor anzahlEditor = new AnzahlEditor();
        myTable.getColumn("VPE").setCellEditor(anzahlEditor);

        JScrollPane scrollPane = new JScrollPane(myTable);
        artikelListPanel.add(scrollPane, BorderLayout.CENTER);
        allPanel.add(artikelListPanel, BorderLayout.CENTER);
    }

    void setTableProperties(JTable myTable) {
        myTable.getColumn("Produktgruppe").setCellRenderer(linksAusrichter);
        myTable.getColumn("Name").setCellRenderer(linksAusrichter);
        myTable.getColumn("Nummer").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("Barcode").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("VK-Preis").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("EK-Preis").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("VPE").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("MwSt.").setCellRenderer(rechtsAusrichter);
        //myTable.getColumn("Betrag MwSt.").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("Ab/Seit").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("Bis").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("Lieferant").setCellRenderer(linksAusrichter);
        myTable.getColumn("Herkunft").setCellRenderer(linksAusrichter);

        myTable.getColumn("Produktgruppe").setPreferredWidth(70);
        myTable.getColumn("Name").setPreferredWidth(100);
        myTable.getColumn("Nummer").setPreferredWidth(70);
        myTable.getColumn("Barcode").setPreferredWidth(70);
        myTable.getColumn("VK-Preis").setPreferredWidth(30);
        myTable.getColumn("EK-Preis").setPreferredWidth(30);
        myTable.getColumn("VPE").setPreferredWidth(10);
        myTable.getColumn("MwSt.").setPreferredWidth(20);
        //myTable.getColumn("Betrag MwSt.").setPreferredWidth(30);
        myTable.getColumn("Ab/Seit").setPreferredWidth(70);
        myTable.getColumn("Bis").setPreferredWidth(70);
        myTable.getColumn("Lieferant").setPreferredWidth(70);
        myTable.getColumn("Herkunft").setPreferredWidth(100);
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
        int column = e.getColumn();
        AbstractTableModel model = (AbstractTableModel)e.getSource();
        String origArtikelName = originalData.get(row).get(model.findColumn("Name")).toString();
        String origArtikelNummer = originalData.get(row).get(model.findColumn("Nummer")).toString();
        int nummerIndex = editArtikelNummer.indexOf(origArtikelNummer); // look up artikelNummer in change list
        int nameIndex = editArtikelName.indexOf(origArtikelName); // look up artikelName in change list

        // post-edit edited cell
        String value = model.getValueAt(row, column).toString().replaceAll("\\s","");
        if ( value.equals("") ){
            // replace whitespace only entries with nothing
            model.removeTableModelListener(this); // remove listener before doing changes
            model.setValueAt(value, row, column);
            model.addTableModelListener(this);
        }
        String header = model.getColumnName(column);
        if ( header.equals("Name") && value.equals("") ){
            // user tried to delete the name (not allowed)
            // reset to original value
            model.setValueAt(origArtikelName, row, column);
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
                    model.setValueAt(originalData.get(row).get(column).toString(), row, column);
                }
            }
        }

        // Compare entire row to original data
        boolean changed = false;
        for ( int col=0; col<data.get(row).size(); col++){ // compare entire row to original data
            String colName = model.getColumnName(col);
            String val = data.get(row).get(col).toString();
            String origVal = originalData.get(row).get(col).toString();
            if ( ! val.equals( origVal ) ){
                changed = true;
                break;
            }
        }

        if (changed){
            // get and store all the values of the edited row
            String artikelName = model.getValueAt(row, model.findColumn("Name")).toString();
            String artikelNummer = model.getValueAt(row, model.findColumn("Nummer")).toString();
            if ( !artikelName.equals(origArtikelName) || !artikelNummer.equals(origArtikelNummer) ){
                if ( isItemAlreadyKnown(artikelName, artikelNummer) ){
                    // not allowed: changing name and nummer to a pair that is already registered in DB
                    JOptionPane.showMessageDialog(this, "Fehler: Kombination Namme/Nummer bereits vorhanden! Wird zurückgesetzt.",
                            "Info", JOptionPane.INFORMATION_MESSAGE);
                    model.setValueAt(origArtikelName, row, model.findColumn("Name"));
                    model.setValueAt(origArtikelNummer, row, model.findColumn("Nummer"));
                    return;
                }
            }
            String barcode = model.getValueAt(row, model.findColumn("Barcode")).toString();
            String vkpreis = model.getValueAt(row, model.findColumn("VK-Preis")).toString();
            String ekpreis = model.getValueAt(row, model.findColumn("EK-Preis")).toString();
            String vpe = model.getValueAt(row, model.findColumn("VPE")).toString();
            boolean aktiv = model.getValueAt(row, model.findColumn("Aktiv")).toString().equals("true") ? true : false;
            String herkunft = model.getValueAt(row, model.findColumn("Herkunft")).toString();

            // update the vectors caching the changes
            if (nummerIndex == nameIndex && nummerIndex != -1){ // this row has been changed before, update the change cache
                changedName.set(nummerIndex, artikelName);
                changedNummer.set(nummerIndex, artikelNummer);
                changedBarcode.set(nummerIndex, barcode);
                changedVKP.set(nummerIndex, vkpreis);
                changedEKP.set(nummerIndex, ekpreis);
                changedVPE.set(nummerIndex, vpe);
                changedHerkunft.set(nummerIndex, herkunft);
                changedAktiv.set(nummerIndex, aktiv);
            } else { // an edit occurred in a row that is not in the list of changes yet
                editArtikelName.add(origArtikelName);
                editArtikelNummer.add(origArtikelNummer);
                changedName.add(artikelName);
                changedNummer.add(artikelNummer);
                changedBarcode.add(barcode);
                changedVKP.add(vkpreis);
                changedEKP.add(ekpreis);
                changedVPE.add(vpe);
                changedHerkunft.add(herkunft);
                changedAktiv.add(aktiv);
            }
        } else if (!changed) {
            // update the vectors caching the changes
            if (nummerIndex == nameIndex && nummerIndex != -1){ // this row has been changed before, all changes undone
                editArtikelName.remove(nummerIndex); // remove item from list of changes
                editArtikelNummer.remove(nummerIndex);
                changedNummer.remove(nummerIndex);
                changedName.remove(nummerIndex);
                changedBarcode.remove(nummerIndex);
                changedVKP.remove(nummerIndex);
                changedEKP.remove(nummerIndex);
                changedVPE.remove(nummerIndex);
                changedHerkunft.remove(nummerIndex);
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
        Vector<String> selectedProdGrIDs = new Vector<String>();
        Vector<String> selectedLiefIDs = new Vector<String>();
        Vector<Boolean> selectedVarPreisBools = new Vector<Boolean>();
        int[] selection = myTable.getSelectedRows();
        for (int i = 0; i < selection.length; i++) {
            selection[i] = myTable.convertRowIndexToModel(selection[i]);
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
        if (e.getSource() == searchButton){
            if ( editArtikelName.size() > 0 ){
                int answer = changeLossConfirmDialog();
                if (answer == JOptionPane.YES_OPTION){
                    filterStr = searchField.getText();
                    updateAll();
                } else {
                    searchField.setText(filterStr);
                }
            } else {
                filterStr = searchField.getText();
                updateAll();
            }
            return;
        }
        if (e.getSource() == stopButton){
            searchField.setText("");
            searchButton.doClick();
            return;
        }
        if (e.getSource() == inaktivCheckBox){
            if ( editArtikelName.size() > 0 ){
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
            if ( editArtikelName.size() > 0 ){
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
