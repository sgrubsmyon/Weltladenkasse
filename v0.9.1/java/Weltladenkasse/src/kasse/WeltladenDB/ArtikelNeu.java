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
    public Artikelliste artikelListe;
    UpdateTableFunctor updateTableFunc;

    public AnyJComponentJTable myTable;
    public Vector< Vector<Object> > data;
    public Vector<String> columnLabels;
    public Vector<String> artikelNamen;
    public Vector<String> artikelNummern;
    public Vector<String> barcodes;
    public Vector<String> vkPreise;
    public Vector<String> ekPreise;
    public Vector<String> variablePreise;
    public Vector<String> vpes;
    public Vector<String> selLieferantIDs;
    public Vector<String> selProduktgruppenIDs;
    public Vector<String> herkuenfte;
    public Vector<JButton> removeButtons;
    public Vector< Vector<Color> > colorMatrix;

    public JPanel tablePanel;

    // Methoden:
    public ArtikelNeu(Connection conn, MainWindowGrundlage mw, Artikelliste pw, UpdateTableFunctor utf) {
	super(conn, mw);
        this.artikelListe = pw;
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
        variablePreise = new Vector<String>();
        vpes = new Vector<String>();
        selLieferantIDs = new Vector<String>();
        selProduktgruppenIDs = new Vector<String>();
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
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(artikel_id) FROM artikel WHERE artikel_name = '"+name+"' AND artikel_nr = '"+nummer+"' AND aktiv = TRUE"
                    );
            rs.next();
            int count = rs.getInt(1);
            if ( count > 0 ){ exists = 1; } // item already in db
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
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
            try {
                Statement stmt = this.conn.createStatement();
                if (itemChanged){
                    // set old item to inactive:
                    int result = stmt.executeUpdate(
                            "UPDATE artikel SET aktiv = FALSE, bis = NOW() WHERE artikel_name = \""+artikelNamen.get(i)+"\" AND "+
                            "artikel_nr = \""+artikelNummern.get(i)+"\" AND aktiv = TRUE"
                            );
                    if (result == 0){
                        JOptionPane.showMessageDialog(this,
                                "Fehler: Artikel "+artikelNamen.get(i)+" mit Nummer "+artikelNummern.get(i)+" konnte nicht geändert werden.",
                                "Fehler", JOptionPane.ERROR_MESSAGE);
                    }
                }
                String barcode = barcodes.get(i);
                String herkunft = herkuenfte.get(i);
                if ( !barcode.equals("NULL") ){ barcode = "'"+barcode+"'"; }
                if ( !herkunft.equals("NULL") ){ herkunft = "'"+herkunft+"'"; }
                int result = stmt.executeUpdate(
                        "INSERT INTO artikel SET artikel_name = '"+artikelNamen.get(i)+"', artikel_nr = '"+artikelNummern.get(i)+"', " +
                        "barcode = "+barcode+", " +
                        "vk_preis = "+vkPreise.get(i)+", ek_preis = "+ekPreise.get(i)+", lieferant_id = "+selLieferantIDs.get(i)+", " +
                        "produktgruppen_id = "+selProduktgruppenIDs.get(i)+", herkunft = "+herkunft+", " +
                        "von = NOW(), aktiv = TRUE, variabler_preis = "+variablePreise.get(i) +
                        ", vpe = "+vpes.get(i)
                        );
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel "+artikelNamen.get(i)+" mit Nummer "+artikelNummern.get(i)+" konnte nicht in DB gespeichert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
                stmt.close();
            } catch (SQLException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
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
