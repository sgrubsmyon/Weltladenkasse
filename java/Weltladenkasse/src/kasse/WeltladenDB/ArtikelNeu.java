package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections

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
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;
import javax.swing.table.*;

public class ArtikelNeu extends WindowContent
    implements ArtikelNeuInterface {
    // Attribute:
    UpdateTableFunctor updateTableFunc;

    public AnyJComponentJTable myTable;
    public Vector< Vector<Object> > data;
    public Vector<String> columnLabels;
    public Vector<String> artikelNamen;
    public Vector<String> artikelNummern;
    public Vector<String> barcodes;
    public Vector<String> vkPreise;
    public Vector<String> ekPreise;
    public Vector<Boolean> variablePreise;
    public Vector<Integer> vpes;
    public Vector<Integer> selLieferantIDs;
    public Vector<Integer> selProduktgruppenIDs;
    public Vector<String> herkuenfte;
    public Vector<JButton> removeButtons;
    public Vector< Vector<Color> > colorMatrix;

    public JPanel tablePanel;

    // Methoden:
    public ArtikelNeu(Connection conn, MainWindowGrundlage mw, UpdateTableFunctor utf) {
	super(conn, mw);
        this.updateTableFunc = utf;

        initiateTable();
        emptyTable();
    }

    private void initiateTable() {
        columnLabels = new Vector<String>();
        columnLabels.add("Produktgruppe"); columnLabels.add("Name"); columnLabels.add("Nummer"); columnLabels.add("Barcode");
        columnLabels.add("VK-Preis"); columnLabels.add("EK-Preis"); columnLabels.add("Variabel");
        columnLabels.add("VPE");
        columnLabels.add("Lieferant"); columnLabels.add("Herkunft"); columnLabels.add("Entf.");
    }

    public void emptyTable(){
	data = new Vector< Vector<Object> >();
        artikelNamen = new Vector<String>();
        artikelNummern = new Vector<String>();
        barcodes = new Vector<String>();
        vkPreise = new Vector<String>();
        ekPreise = new Vector<String>();
        variablePreise = new Vector<Boolean>();
        vpes = new Vector<Integer>();
        selLieferantIDs = new Vector<Integer>();
        selProduktgruppenIDs = new Vector<Integer>();
        herkuenfte = new Vector<String>();
        removeButtons = new Vector<JButton>();
        colorMatrix = new Vector< Vector<Color> >();
    }

    public void showTable(JPanel allPanel) {
        myTable = new AnyJComponentJTable(data, columnLabels){ // subclass the JTable to set font properties and tool tip text
            // Implement table cell tool tips.
            public String getToolTipText(MouseEvent e) {
                Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                int realRowIndex = convertRowIndexToModel(rowIndex); // user might have changed row order
                int realColIndex = convertColumnIndexToModel(colIndex); // user might have changed column order
                String tip = "";
                if (realColIndex < columnLabels.size()-1) // prevent tool tip in button row
                    tip = this.getModel().getValueAt(realRowIndex, realColIndex).toString();
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
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                // add custom rendering here
                //c.setFont( c.getFont().deriveFont(Font.BOLD) );
                Color color = colorMatrix.get(row).get(column);
                c.setForeground(color);
                return c;
            }
        };
        myTable.setColEditableTrue(columnLabels.size()-1); // last column has buttons
	myTable.setDefaultRenderer( JComponent.class, new JComponentCellRenderer() );
	myTable.setDefaultEditor( JComponent.class, new JComponentCellEditor() );
        setTableProperties(myTable);

	tablePanel = new JPanel();
	tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
	tablePanel.setBorder(BorderFactory.createTitledBorder("Neue Artikel"));
            JScrollPane scrollPane = new JScrollPane(myTable);
            tablePanel.add(scrollPane);

	allPanel.add(tablePanel);
    }

    private void setTableProperties(JTable table) {
        table.getColumn("Produktgruppe").setCellRenderer(linksAusrichter);
        table.getColumn("Name").setCellRenderer(linksAusrichter);
        table.getColumn("Nummer").setCellRenderer(rechtsAusrichter);
        table.getColumn("VK-Preis").setCellRenderer(rechtsAusrichter);
        table.getColumn("EK-Preis").setCellRenderer(rechtsAusrichter);
        table.getColumn("VPE").setCellRenderer(rechtsAusrichter);
        table.getColumn("Lieferant").setCellRenderer(linksAusrichter);
        table.getColumn("Herkunft").setCellRenderer(linksAusrichter);
        table.getColumn("Produktgruppe").setPreferredWidth(100);
        table.getColumn("Name").setPreferredWidth(100);
        table.getColumn("Nummer").setPreferredWidth(70);
        table.getColumn("VK-Preis").setPreferredWidth(50);
        table.getColumn("EK-Preis").setPreferredWidth(50);
        table.getColumn("Variabel").setPreferredWidth(30);
        table.getColumn("VPE").setPreferredWidth(30);
        table.getColumn("Lieferant").setPreferredWidth(70);
        table.getColumn("Herkunft").setPreferredWidth(100);
        table.getColumn("Entf.").setPreferredWidth(30);
    }

    public int checkIfItemAlreadyKnown(String name, String nummer) {
        int exists = 0;
        if ( isItemAlreadyKnown(name, nummer) ){
            exists = 1;
        }
        // always look into table, too
        for (int i=0; i<data.size(); i++){
            String tableName = data.get(i).get(1).toString();
            String tableNummer = data.get(i).get(2).toString();
            if (tableName.equals(name) && tableNummer.equals(nummer)){
                exists = 2; // item already in table
                break;
            }
        }
        return exists;
    }

    public void submit() {
        for (int i=0; i<data.size(); i++){
            boolean itemChanged = false;
            for (int col=0; col<9; col++){
                if (colorMatrix.get(i).get(col) == Color.red){
                    itemChanged = true;
                    break;
                }
            }
            if (itemChanged){
                // set old item to inactive:
                int result = setItemInactive(artikelNamen.get(i), artikelNummern.get(i));
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel "+artikelNamen.get(i)+" mit Nummer "+artikelNummern.get(i)+" konnte nicht geändert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    continue; // don't insert this new item, continue with next
                }
            }
            int result = insertNewItem(artikelNamen.get(i), artikelNummern.get(i), barcodes.get(i),
                    variablePreise.get(i), vkPreise.get(i), ekPreise.get(i), vpes.get(i),
                    selProduktgruppenIDs.get(i), selLieferantIDs.get(i), herkuenfte.get(i));
            if (result == 0){
                if (itemChanged){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel "+artikelNamen.get(i)+" mit Nummer "+artikelNummern.get(i)+" konnte nicht geändert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    result = setItemActive(artikelNamen.get(i), artikelNummern.get(i));
                    if (result == 0){
                        JOptionPane.showMessageDialog(this,
                                "Fehler: Artikel "+artikelNamen.get(i)+" mit Nummer "+artikelNummern.get(i)+" konnte nicht wieder hergestellt werden. Artikel ist nun gelöscht (inaktiv).",
                                "Fehler", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel "+artikelNamen.get(i)+" mit Nummer "+artikelNummern.get(i)+" konnte nicht in DB gespeichert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public void updateTable(JPanel allPanel) {
        allPanel.remove(tablePanel);
        allPanel.revalidate();
        showTable(allPanel);
    }

    // will data be lost on close?
    public boolean willDataBeLost() {
        if (data.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
	int removeRow = -1;
	for (int i=0; i<removeButtons.size(); i++){
	    if (e.getSource() == removeButtons.get(i) ){
		removeRow = i;
		break;
	    }
	}
        if (removeRow > -1){
            data.remove(removeRow);
            artikelNamen.remove(removeRow);
            artikelNummern.remove(removeRow);
            barcodes.remove(removeRow);
            vkPreise.remove(removeRow);
            ekPreise.remove(removeRow);
            variablePreise.remove(removeRow);
            vpes.remove(removeRow);
            selLieferantIDs.remove(removeRow);
            selProduktgruppenIDs.remove(removeRow);
            herkuenfte.remove(removeRow);
            removeButtons.remove(removeRow);
            colorMatrix.remove(removeRow);
            updateTableFunc.updateTable();
            return;
        }
    }
}