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

public class Produktgruppenliste extends WindowContent implements ItemListener, TableModelListener,
       ListSelectionListener, DocumentListener {
    // Attribute:
    private JPanel allPanel;
    private JPanel produktgruppenListPanel;
    private JScrollPane scrollPane;
    private JCheckBox inaktivCheckBox;
    private boolean showInaktive = false;
    private JTextField filterField;
    private JButton emptyFilterButton;
    private JButton saveButton;
    private JButton revertButton;
    private JButton editButton;
    private JButton newButton;

    private String filterStr = "";
    private String aktivFilterStr = " AND p.aktiv = TRUE ";
    private String orderByStr = "p.toplevel_id, p.sub_id, p.subsub_id";

    // The table holding the items
    private JTable myTable;
    private Vector< Vector<Object> > data;
    protected Vector< Vector<Object> > originalData;
    protected Vector< Vector<Object> > displayData;
    protected Vector<Integer> displayIndices;
    private Vector<String> columnLabels;
    protected Vector<Boolean> activeRowBools;
    private Vector<Integer> produktgruppenIDs;
    private Vector< Vector<Integer> > produktgruppenIDsList;
    private Vector<Integer> mwstIDs;
    private Vector<Integer> pfandIDs;

    // Vectors storing table edits
    private Vector<Integer> editedProduktgruppenIDs;
    private Vector<String> changedProduktgruppenName;
    private Vector<Boolean> changedAktiv;

    // Dialog to read items from file
    private JDialog readFromFileDialog;

    // Methoden:
    public Produktgruppenliste(Connection conn, MainWindowGrundlage mw) {
        super(conn, mw);

        fillDataArray();
        showAll();
    }

    private void fillDataArray() {
        this.data = new Vector< Vector<Object> >();
        columnLabels = new Vector<String>();
        columnLabels.add("Produktgruppen-Index"); columnLabels.add("Produktgruppen-Name");
        columnLabels.add("# Artikel"); columnLabels.add("MwSt."); columnLabels.add("Pfand");
        columnLabels.add("Aktiv");
        produktgruppenIDs = new Vector<Integer>();
        produktgruppenIDsList = new Vector< Vector<Integer> >();
        mwstIDs = new Vector<Integer>();
        pfandIDs = new Vector<Integer>();
        activeRowBools = new Vector<Boolean>();

        String filter = "p.toplevel_id IS NOT NULL "; // exclude 'unbekannt'
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT p.produktgruppen_id, p.toplevel_id, p.sub_id, p.subsub_id, "+
                    "p.produktgruppen_name, "+
                    "p.aktiv, "+
                    "p.mwst_id, m.mwst_satz, p.pfand_id, a.artikel_name "+
                    "FROM produktgruppe AS p "+
                    "INNER JOIN mwst AS m USING(mwst_id) "+ // change to
                                        // LEFT JOIN to allow editing of "Sonstiges" that has no associated VAT
                    "LEFT JOIN pfand USING(pfand_id) "+
                    "LEFT JOIN artikel AS a USING(artikel_id) "+
                    "WHERE " + filter +
                    aktivFilterStr +
                    "ORDER BY " + orderByStr
                    );
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Integer produktgruppen_id = rs.getInt(1);
                Vector<Integer> ids = new Vector<Integer>();
                ids.add( rs.getString(2) == null ? null : rs.getInt(2) );
                ids.add( rs.getString(3) == null ? null : rs.getInt(3) );
                ids.add( rs.getString(4) == null ? null : rs.getInt(4) );
                String produktgruppe = rs.getString(5);
                Boolean aktivBool = rs.getBoolean(6);
                Integer mwst_id = rs.getString(7) == null ? null : rs.getInt(7);
                BigDecimal mwst_satz = rs.getBigDecimal(8);
                Integer pfand_id = rs.getString(9) == null ? null : rs.getInt(9);
                String pfand_name = rs.getString(10) == null ? "" : rs.getString(10);
                String produktgruppenNumber = "";
                if (ids.get(0) != null) produktgruppenNumber += ids.get(0).toString();
                if (ids.get(1) != null) produktgruppenNumber += "."+ids.get(1).toString();
                if (ids.get(2) != null) produktgruppenNumber += "."+ids.get(2).toString();

                Integer nArticles = howManyActiveArticlesWithProduktgruppe(produktgruppen_id);

                Vector<Object> row = new Vector<Object>();
                    row.add(produktgruppenNumber);
                    row.add(produktgruppe);
                    row.add(nArticles);
                    row.add( vatFormatter(mwst_satz) );
                    row.add(pfand_name);
                    row.add(aktivBool);
                data.add(row);
                produktgruppenIDs.add(produktgruppen_id);
                produktgruppenIDsList.add(ids);
                mwstIDs.add(mwst_id);
                pfandIDs.add(pfand_id);
                activeRowBools.add(aktivBool);
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
        editedProduktgruppenIDs = new Vector<Integer>();
        changedProduktgruppenName = new Vector<String>();
        changedAktiv = new Vector<Boolean>();
    }

    private void putChangesIntoDB() {
        for (int index=0; index<editedProduktgruppenIDs.size(); index++){
            Integer prodgr_id = editedProduktgruppenIDs.get(index);
            String produktgruppenName = changedProduktgruppenName.get(index);
            Boolean aktivBool = changedAktiv.get(index);

            int result = updateProdGr(prodgr_id, produktgruppenName, aktivBool);
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Produktgruppen mit Nr. "+prodgr_id+" konnte nicht geändert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                continue; // continue with next item
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
          inaktivCheckBox = new JCheckBox("inaktive anzeigen");
          inaktivCheckBox.setMnemonic(KeyEvent.VK_A);
          inaktivCheckBox.setSelected(showInaktive);
          inaktivCheckBox.addItemListener(this);
          inaktivCheckBox.addActionListener(this);
          topLeftPanel.add(inaktivCheckBox);
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

            editButton = new JButton("Markierte Prod.Gr. bearbeiten");
            editButton.setMnemonic(KeyEvent.VK_B);
            editButton.addActionListener(this);
            bottomLeftPanel.add(editButton);
        bottomPanel.add(bottomLeftPanel);

          JPanel bottomRightPanel = new JPanel();
          bottomRightPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
            newButton = new JButton("Neue Produktgruppe eingeben");
            newButton.setMnemonic(KeyEvent.VK_N);
            newButton.addActionListener(this);
            bottomRightPanel.add(newButton);
        bottomPanel.add(bottomRightPanel);
        allPanel.add(bottomPanel, BorderLayout.SOUTH);

        enableButtons();

        this.add(allPanel, BorderLayout.CENTER);
    }

    void enableButtons() {
        saveButton.setEnabled(editedProduktgruppenIDs.size() > 0);
        revertButton.setEnabled(editedProduktgruppenIDs.size() > 0);
        editButton.setEnabled(myTable.getSelectedRowCount() > 0);
        newButton.setEnabled(editedProduktgruppenIDs.size() == 0);
    }

    // subclass the AbstractTableModel to set editable cells etc.
    protected class ProduktgruppenlisteTableModel extends AbstractTableModel {
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

        public Class getColumnClass(int c) { /* JTable uses this method to
                                              * determine the default renderer/editor for each cell.
                                              * If we didn't implement this
                                              * method, then the last
                                              * column would contain text
                                              * ("true"/"false"), rather
                                              * than a check box.
                                              */
            return getValueAt(0, c).getClass();
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

    // subclass the JTable to set font properties and tool tip text
    protected class ProduktgruppenlisteTable extends AnyJComponentJTable {
        public ProduktgruppenlisteTable(TableModel m, Integer columnMargin,
                Integer minColumnWidth, Integer maxColumnWidth){
            super(m, columnMargin, minColumnWidth, maxColumnWidth);
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            String header = this.getColumnName(col);
            if ( activeRowBools.get(row) ){
                if ( header.equals("Produktgruppen-Name") || header.equals("Aktiv") ) {
                    return true;
                }
                return false;
            } else {
                if ( header.equals("Aktiv") ) {
                    return true;
                }
                return false;
            }
        }

        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
            JComponent c = (JComponent)super.prepareRenderer(renderer, row, col);
            // add custom rendering here
            int realRowIndex = convertRowIndexToModel(row);
            realRowIndex = displayIndices.get(realRowIndex); // convert from displayData index to data index
            int realColIndex = convertColumnIndexToModel(col); // user might have changed column order
            if ( ! activeRowBools.get(realRowIndex) ){ // for rows with inactive items
                c.setFont( c.getFont().deriveFont(Font.ITALIC) );
                c.setForeground(Color.BLUE);
            }
            else {
                c.setFont( c.getFont().deriveFont(Font.PLAIN) );
                c.setForeground(Color.BLACK);
            }
            // indent the produktgruppen_name
            if ( this.getColumnName(realColIndex).equals("Produktgruppen-Name") ) {
                ProduktgruppenIndentedRenderer pir = new ProduktgruppenIndentedRenderer(produktgruppenIDsList);
                int indent = pir.getIndent(realRowIndex);
                c.setBorder(BorderFactory.createEmptyBorder(0,indent,0,0));//5 is the indent, modify to suit
            }
            return c;
        }
    }

    void initiateTable() {
        myTable = new ProduktgruppenlisteTable(new ProduktgruppenlisteTableModel(),
                columnMargin, minColumnWidth, maxColumnWidth);
        myTable.setAutoCreateRowSorter(true);
        myTable.getModel().addTableModelListener(this);
        myTable.getSelectionModel().addListSelectionListener(this);
        setTableProperties(myTable);
    }

    void showTable() {
        produktgruppenListPanel = new JPanel();
        produktgruppenListPanel.setLayout(new BorderLayout());
        produktgruppenListPanel.setBorder(BorderFactory.createTitledBorder("Produktgruppen"));

        initiateTable();

        scrollPane = new JScrollPane(myTable);
        produktgruppenListPanel.add(scrollPane, BorderLayout.CENTER);
        allPanel.add(produktgruppenListPanel, BorderLayout.CENTER);
    }

    void updateTable() {
        applyFilter();
        produktgruppenListPanel.remove(scrollPane);
	produktgruppenListPanel.revalidate();

        initiateTable();

        scrollPane = new JScrollPane(myTable);
        produktgruppenListPanel.add(scrollPane);
        enableButtons();
    }

    void setTableProperties(JTable myTable) {
        myTable.getColumn("Produktgruppen-Index").setCellRenderer(linksAusrichter);
        myTable.getColumn("Produktgruppen-Name").setCellRenderer(linksAusrichter);
        myTable.getColumn("MwSt.").setCellRenderer(rechtsAusrichter);
        myTable.getColumn("Pfand").setCellRenderer(linksAusrichter);

        myTable.getColumn("Produktgruppen-Index").setPreferredWidth(5);
        myTable.getColumn("Produktgruppen-Name").setPreferredWidth(100);
        myTable.getColumn("# Artikel").setPreferredWidth(5);
        myTable.getColumn("MwSt.").setPreferredWidth(5);
        myTable.getColumn("Pfand").setPreferredWidth(10);
        myTable.getColumn("Aktiv").setPreferredWidth(1);
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
                aktivFilterStr = " AND p.aktiv = TRUE ";
                showInaktive = false;
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
        Integer origProduktgruppenID = produktgruppenIDs.get(dataRow);
        String origProduktgruppenName = originalData.get(dataRow).get(model.findColumn("Produktgruppen-Name")).toString();

        // post-edit edited cell
        String value = model.getValueAt(row, column).toString().replaceAll("\\s","");
        if ( value.equals("") ){
            // replace whitespace only entries with nothing
            model.removeTableModelListener(this); // remove listener before doing changes
            model.setValueAt(value, row, column);
            model.addTableModelListener(this);
        }
        String header = model.getColumnName(column);
        if ( header.equals("Produktgruppen-Name") && value.equals("") ){
            // user tried to delete the produktgruppe (not allowed)
            // reset to original value
            model.setValueAt(origProduktgruppenName, row, column);
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

        int produktgruppenIndex = editedProduktgruppenIDs.indexOf(origProduktgruppenID); // look up produktgruppe in change list
        if (changed){
            // get and store all the values of the edited row
            String produktgruppe = model.getValueAt(row, model.findColumn("Produktgruppen-Name")).toString();
            if ( !produktgruppe.equals(origProduktgruppenName) ){
                if ( isProdGrAlreadyKnown(produktgruppe) ){
                    // not allowed: changing produktgruppe to a name that is already registered in DB
                    if ( isProdGrInactive(produktgruppe) ){
                        JOptionPane.showMessageDialog(this, "Fehler: Produktgruppe "+
                                produktgruppe+" bereits vorhanden, aber inaktiv!\n"+
                                "Bei Bedarf wieder auf aktiv setzen.",
                                "Info", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Fehler: Produktgruppe "+
                                produktgruppe+" bereits vorhanden!\n"+
                                "Wird zurückgesetzt.",
                                "Info", JOptionPane.INFORMATION_MESSAGE);
                    }
                    model.setValueAt(origProduktgruppenName, row, model.findColumn("Produktgruppen-Name"));
                    return;
                }
            }
            boolean aktiv = model.getValueAt(row, model.findColumn("Aktiv")).toString().equals("true") ? true : false;

            // update the vectors caching the changes
            if (produktgruppenIndex != -1){ // this row has been changed before, update the change cache
                changedProduktgruppenName.set(produktgruppenIndex, produktgruppe);
                changedAktiv.set(produktgruppenIndex, aktiv);
            } else { // an edit occurred in a row that is not in the list of changes yet
                editedProduktgruppenIDs.add(origProduktgruppenID);
                changedProduktgruppenName.add(produktgruppe);
                changedAktiv.add(aktiv);
            }
        } else if (!changed) {
            // update the vectors caching the changes
            if (produktgruppenIndex != -1){ // this row has been changed before, all changes undone
                editedProduktgruppenIDs.remove(produktgruppenIndex); // remove item from list of changes
                changedProduktgruppenName.remove(produktgruppenIndex);
                changedAktiv.remove(produktgruppenIndex);
            }
        }

        enableButtons();
    }

    void showEditDialog() {
        // get data from the selected rows
        Vector< Vector<Object> > selectedData = new Vector< Vector<Object> >();
        Vector<Integer> selectedProdGrIDs = new Vector<Integer>();
        Vector<Integer> selectedMwStIDs = new Vector<Integer>();
        Vector<Integer> selectedPfandIDs = new Vector<Integer>();
        int[] selection = myTable.getSelectedRows();
        for (int i = 0; i < selection.length; i++) {
            selection[i] = myTable.convertRowIndexToModel(selection[i]);
            selection[i] = displayIndices.get(selection[i]); // convert from displayData index to data index
            selectedData.add( data.get(selection[i]) );
            selectedProdGrIDs.add( produktgruppenIDs.get(selection[i]) );
            selectedMwStIDs.add( mwstIDs.get(selection[i]) );
            selectedPfandIDs.add( pfandIDs.get(selection[i]) );
        }
        JDialog editDialog = new JDialog(this.mainWindow, "Produktgruppe(n) bearbeiten", true);
        ProduktgruppeBearbeiten bearb = new ProduktgruppeBearbeiten(this.conn, this.mainWindow, this, editDialog,
                selectedData, selectedProdGrIDs, selectedMwStIDs, selectedPfandIDs);
        editDialog.getContentPane().add(bearb, BorderLayout.CENTER);
        editDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        WindowAdapterDialog wad = new WindowAdapterDialog(bearb, editDialog, "Achtung: Änderungen gehen verloren (noch nicht abgeschickt).\nWirklich schließen?");
        editDialog.addWindowListener(wad);
        editDialog.pack();
        editDialog.setVisible(true);
    }

    void showNewProduktgruppenDialog() {
        JDialog newProduktgruppenDialog = new JDialog(this.mainWindow, "Neue Produktgruppe hinzufügen", true);
        ProduktgruppeNeuEingeben newProduktgruppe = new ProduktgruppeNeuEingeben(this.conn, this.mainWindow, this, newProduktgruppenDialog);
        newProduktgruppenDialog.getContentPane().add(newProduktgruppe, BorderLayout.CENTER);
        newProduktgruppenDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        WindowAdapterDialog wad = new WindowAdapterDialog(newProduktgruppe, newProduktgruppenDialog, "Achtung: Änderungen gehen verloren (noch nicht abgeschickt).\nWirklich schließen?");
        newProduktgruppenDialog.addWindowListener(wad);
        newProduktgruppenDialog.pack();
        newProduktgruppenDialog.setVisible(true);
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
            showNewProduktgruppenDialog();
            return;
        }
        if (e.getSource() == inaktivCheckBox){
            if ( editedProduktgruppenIDs.size() > 0 ){
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
        if (e.getSource() == emptyFilterButton){
            filterField.setText("");
	    return;
	}
    }
}
