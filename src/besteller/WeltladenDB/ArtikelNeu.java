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

    public Vector<Integer> selProduktgruppenIDs;
    public Vector<Integer> selLieferantIDs;
    public Vector<String> lieferanten;
    public Vector<String> artikelNummern;
    public Vector<String> artikelNamen;
    public Vector<String> kurznamen;
    public Vector<BigDecimal> mengen;
    public Vector<String> barcodes;
    public Vector<String> herkuenfte;
    public Vector<Integer> vpes;
    public Vector<Integer> sets; // the set size (how many articles in one set)
    public Vector<String> vkPreise;
    public Vector<String> empfvkPreise;
    public Vector<String> ekRabatte;
    public Vector<String> ekPreise;
    public Vector<Boolean> variablePreise;
    public Vector<Boolean> sortimente;
    public Vector<Boolean> lieferbarBools;
    public Vector<Integer> beliebtWerte;
    public Vector<Integer> bestaende;
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
        columnLabels.add("Produktgruppe"); columnLabels.add("Lieferant");
        columnLabels.add("Nummer"); columnLabels.add("Name");
        columnLabels.add("Kurzname"); columnLabels.add("Menge");
        columnLabels.add("Sortiment"); columnLabels.add("Lieferbar");
        columnLabels.add("Beliebtheit"); columnLabels.add("Barcode");
        columnLabels.add("VPE"); columnLabels.add("Setgr.");
        columnLabels.add("VK-Preis"); columnLabels.add("Empf. VK-Preis");
        columnLabels.add("EK-Rabatt"); columnLabels.add("EK-Preis");
        columnLabels.add("Variabel"); columnLabels.add("Herkunft");
        columnLabels.add("Bestand"); columnLabels.add("Entf.");
    }

    public void emptyTable(){
	data = new Vector< Vector<Object> >();

        selProduktgruppenIDs = new Vector<Integer>();
        selLieferantIDs = new Vector<Integer>();
        lieferanten = new Vector<String>();
        artikelNummern = new Vector<String>();
        artikelNamen = new Vector<String>();
        kurznamen = new Vector<String>();
        mengen = new Vector<BigDecimal>();
        barcodes = new Vector<String>();
        herkuenfte = new Vector<String>();
        vpes = new Vector<Integer>();
        sets = new Vector<Integer>();
        vkPreise = new Vector<String>();
        empfvkPreise = new Vector<String>();
        ekRabatte = new Vector<String>();
        ekPreise = new Vector<String>();
        variablePreise = new Vector<Boolean>();
        sortimente = new Vector<Boolean>();
        lieferbarBools = new Vector<Boolean>();
        beliebtWerte = new Vector<Integer>();
        bestaende = new Vector<Integer>();
        removeButtons = new Vector<JButton>();
        colorMatrix = new Vector< Vector<Color> >();
    }

    public void showTable(JPanel allPanel) {
        myTable = new AnyJComponentJTable(data, columnLabels){ // subclass the JTable to set font properties
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                // add custom rendering here
                //c.setFont( c.getFont().deriveFont(Font.BOLD) );
                Color color = colorMatrix.get(row).get(column);
                c.setForeground(color);
                return c;
            }
        };
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
        table.getColumn("Lieferant").setCellRenderer(linksAusrichter);
        table.getColumn("Nummer").setCellRenderer(rechtsAusrichter);
        table.getColumn("Name").setCellRenderer(linksAusrichter);
        table.getColumn("Kurzname").setCellRenderer(linksAusrichter);
        table.getColumn("Menge").setCellRenderer(rechtsAusrichter);
        table.getColumn("Barcode").setCellRenderer(linksAusrichter);
        table.getColumn("Herkunft").setCellRenderer(linksAusrichter);
        table.getColumn("VPE").setCellRenderer(rechtsAusrichter);
        table.getColumn("Setgr.").setCellRenderer(rechtsAusrichter);
        table.getColumn("VK-Preis").setCellRenderer(rechtsAusrichter);
        table.getColumn("Empf. VK-Preis").setCellRenderer(rechtsAusrichter);
        table.getColumn("EK-Preis").setCellRenderer(rechtsAusrichter);
        table.getColumn("Beliebtheit").setCellRenderer(linksAusrichter);

        table.getColumn("Produktgruppe").setPreferredWidth(70);
        table.getColumn("Lieferant").setPreferredWidth(50);
        table.getColumn("Nummer").setPreferredWidth(50);
        table.getColumn("Name").setPreferredWidth(100);
        table.getColumn("Kurzname").setPreferredWidth(100);
        table.getColumn("Menge").setPreferredWidth(30);
        table.getColumn("Barcode").setPreferredWidth(50);
        table.getColumn("Herkunft").setPreferredWidth(100);
        table.getColumn("VPE").setPreferredWidth(30);
        table.getColumn("Setgr.").setPreferredWidth(30);
        table.getColumn("VK-Preis").setPreferredWidth(50);
        table.getColumn("Empf. VK-Preis").setPreferredWidth(50);
        table.getColumn("EK-Preis").setPreferredWidth(50);
        table.getColumn("Variabel").setPreferredWidth(30);
        table.getColumn("Sortiment").setPreferredWidth(30);
        table.getColumn("Lieferbar").setPreferredWidth(30);
        table.getColumn("Beliebtheit").setPreferredWidth(30);
        table.getColumn("Entf.").setPreferredWidth(30);
    }

    public int checkIfItemAlreadyKnown(String lieferant, String nummer) {
        int exists = 0;
        if ( isItemAlreadyKnown(lieferant, nummer) ){
            exists = 1;
        }
        // always look into table, too
        for (int i=0; i<data.size(); i++){
            String tableLieferant = data.get(i).get(1).toString();
            String tableNummer = data.get(i).get(2).toString();
            if (tableLieferant.equals(lieferant) && tableNummer.equals(nummer)){
                exists = 2; // item already in table
                break;
            }
        }
        return exists;
    }

    public int submit() {
        for (int i=0; i<data.size(); i++){
            boolean itemChanged = false;
            for (int col=0; col<colorMatrix.get(i).size(); col++){
                if (colorMatrix.get(i).get(col) == Color.red){
                    itemChanged = true;
                    break;
                }
            }
            if (itemChanged){
                // set old item to inactive:
                int result = setItemInactive(selLieferantIDs.get(i), artikelNummern.get(i));
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel \""+artikelNamen.get(i)+"\" von "+lieferanten.get(i)+" mit Nummer "+artikelNummern.get(i)+" konnte nicht geändert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    continue; // don't insert this new item, continue with next
                }
            }
            int result = insertNewItem(selProduktgruppenIDs.get(i),
                    selLieferantIDs.get(i), artikelNummern.get(i),
                    artikelNamen.get(i), kurznamen.get(i), mengen.get(i), barcodes.get(i),
                    herkuenfte.get(i), vpes.get(i), sets.get(i), vkPreise.get(i), empfvkPreise.get(i),
                    ekRabatte.get(i), ekPreise.get(i), variablePreise.get(i),
                    sortimente.get(i), lieferbarBools.get(i), beliebtWerte.get(i), bestaende.get(i));
            if (result == 0){
                if (itemChanged){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel \""+artikelNamen.get(i)+"\" von "+lieferanten.get(i)+" mit Nummer "+artikelNummern.get(i)+" konnte nicht geändert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    result = setItemActive(selLieferantIDs.get(i), artikelNummern.get(i));
                    if (result == 0){
                        JOptionPane.showMessageDialog(this,
                                "Fehler: Artikel \""+artikelNamen.get(i)+"\" von "+lieferanten.get(i)+" mit Nummer "+artikelNummern.get(i)+" konnte nicht wieder hergestellt werden. Artikel ist nun gelöscht (inaktiv).",
                                "Fehler", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel \""+artikelNamen.get(i)+"\" von "+lieferanten.get(i)+" mit Nummer "+artikelNummern.get(i)+" konnte nicht in DB gespeichert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        return 0;
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

            selProduktgruppenIDs.remove(removeRow);
            selLieferantIDs.remove(removeRow);
            lieferanten.remove(removeRow);
            artikelNummern.remove(removeRow);
            artikelNamen.remove(removeRow);
            kurznamen.remove(removeRow);
            mengen.remove(removeRow);
            barcodes.remove(removeRow);
            herkuenfte.remove(removeRow);
            vpes.remove(removeRow);
            sets.remove(removeRow);
            vkPreise.remove(removeRow);
            empfvkPreise.remove(removeRow);
            ekRabatte.remove(removeRow);
            ekPreise.remove(removeRow);
            variablePreise.remove(removeRow);
            sortimente.remove(removeRow);
            lieferbarBools.remove(removeRow);
            beliebtWerte.remove(removeRow);
            bestaende.remove(removeRow);
            removeButtons.remove(removeRow);
            colorMatrix.remove(removeRow);

            updateTableFunc.updateTable();
            return;
        }
    }
}
