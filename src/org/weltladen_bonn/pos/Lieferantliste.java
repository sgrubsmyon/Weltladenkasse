package org.weltladen_bonn.pos;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.mariadb.jdbc.MariaDbPoolDataSource;

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

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Lieferantliste extends WindowContent implements ItemListener, TableModelListener,
       ListSelectionListener, DocumentListener {
    // Attribute:
    private static final Logger logger = LogManager.getLogger(Lieferantliste.class);
    
    private JPanel allPanel;
    private JPanel lieferantListPanel;
    private JScrollPane scrollPane;
    private JCheckBox inaktivCheckBox;
    private boolean showInaktive = false;
    private JTextField filterField;
    private JButton emptyFilterButton;
    private JButton saveButton;
    private JButton revertButton;
    private JButton newButton;

    private String filterStr = "";
    private String aktivFilterStr = " AND lieferant.aktiv = TRUE ";
    private String orderByStr = "lieferant.lieferant_id";

    // The table holding the items
    private JTable myTable;
    private Vector< Vector<Object> > data;
    protected Vector< Vector<Object> > originalData;
    protected Vector< Vector<Object> > displayData;
    protected Vector<Integer> displayIndices;
    private Vector<String> columnLabels;
    protected Vector<Boolean> activeRowBools;
    private Vector<Integer> lieferantIDs;

    // Vectors storing table edits
    private Vector<Integer> editedLieferantIDs;
    private Vector<String> changedLieferantName;
    private Vector<String> changedLieferantKurzname;
    private Vector<Boolean> changedAktiv;

    // Dialog to read items from file
    private JDialog readFromFileDialog;

    // Methoden:
    public Lieferantliste(MariaDbPoolDataSource pool, MainWindowGrundlage mw) {
        super(pool, mw);

        fillDataArray();
        showAll();
    }

    private void fillDataArray() {
        this.data = new Vector< Vector<Object> >();
        columnLabels = new Vector<String>();
        columnLabels.add("Lieferant-Nr."); columnLabels.add("Lieferant-Name");
        columnLabels.add("Kurzname");
        columnLabels.add("# Artikel"); columnLabels.add("Aktiv");
        lieferantIDs = new Vector<Integer>();
        activeRowBools = new Vector<Boolean>();

        String filter = "lieferant_id != 1 "; // exclude 'unbekannt'
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT lieferant_id, lieferant_name, lieferant_kurzname, n_artikel, aktiv "+
                    "FROM lieferant "+
                    "WHERE " + filter +
                    aktivFilterStr +
                    "ORDER BY " + orderByStr
                    );
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Integer lieferant_id = rs.getInt(1);
                String lieferant = rs.getString(2);
                String kurzname = rs.getString(3);
                Integer nArticles = rs.getString(4) == null ? null : rs.getInt(4);
                Boolean aktivBool = rs.getBoolean(5);

                Vector<Object> row = new Vector<Object>();
                    row.add(lieferant_id);
                    row.add(lieferant);
                    row.add(kurzname);
                    row.add(nArticles);
                    row.add(aktivBool);
                data.add(row);
                lieferantIDs.add(lieferant_id);
                activeRowBools.add(aktivBool);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        this.originalData = new Vector< Vector<Object> >();
        for ( Vector<Object> row : data ){
            Vector<Object> originalRow = new Vector<Object>();
            originalRow.addAll(row);
            originalData.add(originalRow);
        }
        displayData = new Vector< Vector<Object> >(data);
        initiateDisplayIndices();
        editedLieferantIDs = new Vector<Integer>();
        changedLieferantName = new Vector<String>();
        changedLieferantKurzname = new Vector<String>();
        changedAktiv = new Vector<Boolean>();
    }

    private void putChangesIntoDB() {
        for (int index=0; index<editedLieferantIDs.size(); index++){
            Integer lief_id = editedLieferantIDs.get(index);
            String lieferantName = changedLieferantName.get(index);
            String lieferantKurzname = changedLieferantKurzname.get(index);
            Boolean aktivBool = changedAktiv.get(index);

            int result = updateLieferant(lief_id, lieferantName, lieferantKurzname, aktivBool);
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Lieferant mit Nr. "+lief_id+" konnte nicht geändert werden.",
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
        bottomPanel.add(bottomLeftPanel);

          JPanel bottomRightPanel = new JPanel();
          bottomRightPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
            newButton = new JButton("Neuen Lieferanten eingeben");
            newButton.setMnemonic(KeyEvent.VK_N);
            newButton.addActionListener(this);
            bottomRightPanel.add(newButton);
        bottomPanel.add(bottomRightPanel);
        allPanel.add(bottomPanel, BorderLayout.SOUTH);

        enableButtons();

        this.add(allPanel, BorderLayout.CENTER);
    }

    void enableButtons() {
        saveButton.setEnabled(editedLieferantIDs.size() > 0);
        revertButton.setEnabled(editedLieferantIDs.size() > 0);
        newButton.setEnabled(editedLieferantIDs.size() == 0);
    }

    // subclass the AbstractTableModel to set editable cells etc.
    protected class LieferantlisteTableModel extends AbstractTableModel {
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
            return getValueAt(0, c) == null ? String.class : getValueAt(0, c).getClass();
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
    protected class LieferantlisteTable extends AnyJComponentJTable {
        public LieferantlisteTable(TableModel m, Integer columnMargin,
                Integer minColumnWidth, Integer maxColumnWidth){
            super(m, columnMargin, minColumnWidth, maxColumnWidth);
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            String header = this.getColumnName(col);
            if ( activeRowBools.get(row) ){
                if ( header.equals("Lieferant-Nr.") || header.equals("# Artikel") ) {
                    return false;
                }
                return true;
            } else {
                if ( header.equals("Aktiv") ) {
                    return true;
                }
                return false;
            }
        }

        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);
            // add custom rendering here
            int realRowIndex = convertRowIndexToModel(row);
            realRowIndex = displayIndices.get(realRowIndex); // convert from displayData index to data index
            if ( ! activeRowBools.get(realRowIndex) ){ // for rows with inactive items
                c.setFont( c.getFont().deriveFont(Font.ITALIC) );
                c.setForeground(Color.BLUE);
            }
            else {
                c.setFont( c.getFont().deriveFont(Font.PLAIN) );
                c.setForeground(Color.BLACK);
            }
            return c;
        }
    }

    void initiateTable() {
        myTable = new LieferantlisteTable(new LieferantlisteTableModel(),
                columnMargin, minColumnWidth, maxColumnWidth);
        myTable.setAutoCreateRowSorter(true);
        myTable.getModel().addTableModelListener(this);
        myTable.getSelectionModel().addListSelectionListener(this);
        setTableProperties(myTable);
    }

    void showTable() {
        lieferantListPanel = new JPanel();
        lieferantListPanel.setLayout(new BorderLayout());
        lieferantListPanel.setBorder(BorderFactory.createTitledBorder("Lieferanten"));

        initiateTable();

        scrollPane = new JScrollPane(myTable);
        lieferantListPanel.add(scrollPane, BorderLayout.CENTER);
        allPanel.add(lieferantListPanel, BorderLayout.CENTER);
    }

    void updateTable() {
        applyFilter(filterStr, displayData, displayIndices);
        lieferantListPanel.remove(scrollPane);
	lieferantListPanel.revalidate();

        scrollPane = new JScrollPane(myTable);
        lieferantListPanel.add(scrollPane);
        enableButtons();
    }

    void setTableProperties(JTable myTable) {
        myTable.getColumn("Lieferant-Nr.").setCellRenderer(zentralAusrichter);
        myTable.getColumn("Lieferant-Name").setCellRenderer(linksAusrichter);

        myTable.getColumn("Lieferant-Nr.").setPreferredWidth(20);
        myTable.getColumn("Lieferant-Name").setPreferredWidth(100);
        myTable.getColumn("# Artikel").setPreferredWidth(20);
        myTable.getColumn("Aktiv").setPreferredWidth(10);
    }

    public void updateAll() {
        // old (view gets lost):
        //this.remove(allPanel);
        //this.revalidate();
        //fillDataArray();
        //showAll();
        // new, much better, keeping view:
        fillDataArray();
        updateTable();
    }

    /** Needed for ItemListener. */
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
        if (source == inaktivCheckBox) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                aktivFilterStr = "";
                showInaktive = true;
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                aktivFilterStr = " AND lieferant.aktiv = TRUE ";
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
        Integer origLieferantID = lieferantIDs.get(dataRow);
        String origLieferantName = originalData.get(dataRow).get(model.findColumn("Lieferant-Name")).toString();
        String origLieferantKurzname = originalData.get(dataRow).get(model.findColumn("Kurzname")).toString();

        // post-edit edited cell
        String value = model.getValueAt(row, column).toString().replaceAll("\\s","");
        if ( value.equals("") ){
            // replace whitespace only entries with nothing
            model.removeTableModelListener(this); // remove listener before doing changes
            model.setValueAt(value, row, column);
            model.addTableModelListener(this);
        }
        String header = model.getColumnName(column);
        if ( header.equals("Lieferant-Name") && value.equals("") ){
            // user tried to delete the lieferant (not allowed)
            // reset to original value
            model.setValueAt(origLieferantName, row, column);
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

        int lieferantIndex = editedLieferantIDs.indexOf(origLieferantID); // look up lieferant in change list
        if (changed){
            // get and store all the values of the edited row
            String lieferant = model.getValueAt(row, model.findColumn("Lieferant-Name")).toString();
            if ( !lieferant.equals(origLieferantName) ){
                if ( isLieferantAlreadyKnown(lieferant) ){
                    // not allowed: changing lieferant to a name that is already registered in DB
                    if ( isLieferantInactive(lieferant) ){
                        JOptionPane.showMessageDialog(this, "Fehler: Lieferant "+
                                lieferant+" bereits vorhanden, aber inaktiv!\n"+
                                "Bei Bedarf wieder auf aktiv setzen.",
                                "Info", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Fehler: Lieferant "+
                                lieferant+" bereits vorhanden!\n"+
                                "Wird zurückgesetzt.",
                                "Info", JOptionPane.INFORMATION_MESSAGE);
                    }
                    model.setValueAt(origLieferantName, row, model.findColumn("Lieferant-Name"));
                    return;
                }
            }
            String kurzname = model.getValueAt(row, model.findColumn("Kurzname")).toString();
            boolean aktiv = model.getValueAt(row, model.findColumn("Aktiv")).toString().equals("true") ? true : false;

            // update the vectors caching the changes
            if (lieferantIndex != -1){ // this row has been changed before, update the change cache
                changedLieferantName.set(lieferantIndex, lieferant);
                changedLieferantKurzname.set(lieferantIndex, kurzname);
                changedAktiv.set(lieferantIndex, aktiv);
            } else { // an edit occurred in a row that is not in the list of changes yet
                editedLieferantIDs.add(origLieferantID);
                changedLieferantName.add(lieferant);
                changedLieferantKurzname.add(kurzname);
                changedAktiv.add(aktiv);
            }
        } else if (!changed) {
            // update the vectors caching the changes
            if (lieferantIndex != -1){ // this row has been changed before, all changes undone
                editedLieferantIDs.remove(lieferantIndex); // remove item from list of changes
                changedLieferantName.remove(lieferantIndex);
                changedLieferantKurzname.remove(lieferantIndex);
                changedAktiv.remove(lieferantIndex);
            }
        }

        enableButtons();
    }

    void showNewLieferantDialog() {
        JDialog newLieferantDialog = new JDialog(this.mainWindow, "Neuen Lieferanten hinzufügen", true);
        LieferantNeuEingeben newLieferants = new LieferantNeuEingeben(this.pool, this.mainWindow, this, newLieferantDialog);
        newLieferantDialog.getContentPane().add(newLieferants, BorderLayout.CENTER);
        newLieferantDialog.pack();
        newLieferantDialog.setVisible(true);
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
        if (e.getSource() == saveButton){
            putChangesIntoDB();
            updateAll();
            return;
        }
        if (e.getSource() == revertButton){
            updateAll();
            return;
        }
        if (e.getSource() == newButton){
            showNewLieferantDialog();
            return;
        }
        if (e.getSource() == inaktivCheckBox){
            if ( editedLieferantIDs.size() > 0 ){
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
            filterField.requestFocus();
	    return;
	}
    }
}
