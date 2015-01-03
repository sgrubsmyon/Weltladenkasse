package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.io.*; // for File
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

// OpenDocument stuff:
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.jopendocument.dom.OOUtils;

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
import javax.swing.text.html.HTMLDocument;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ArtikelExport extends WindowContent {
    // Attribute:
    private FileExistsAwareFileChooser odsChooser;
    private Artikelliste artikelListe;

    // Methoden:
    public ArtikelExport(Connection conn, MainWindowGrundlage mw, Artikelliste pw) {
	super(conn, mw);
        this.artikelListe = pw;

        odsChooser = new FileExistsAwareFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "ODS Spreadsheet-Dokumente", "ods");
        odsChooser.setFileFilter(filter);
        odsChooser.setSelectedFile(new File("vorlagen"+fileSep+"Artikelliste.ods"));

        int returnVal = odsChooser.showSaveDialog(pw);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            final File file = odsChooser.getSelectedFile();

            //writeCSVToFile(file);
            writeToODSFile(file);

            System.out.println("Written to " + file.getName());
        } else {
            System.out.println("Open command cancelled by user.");
        }
    }

    void writeToODSFile(File file) {
        // table header
        final Vector<String> columns = new Vector<String>();
            columns.add("Produktgruppe"); columns.add("Lieferant");
            columns.add("Artikelnummer"); columns.add("Bezeichnung | Einheit");
            columns.add("Kurzname");
            columns.add("Menge (kg/l/Stk.)"); columns.add("Barcode");
            columns.add("Herkunftsland"); columns.add("VPE");
            columns.add("VK-Preis"); columns.add("EK-Preis");
            columns.add("Variabel"); columns.add("Sortiment");
        // table rows
        final Vector< Vector<Object> > data = new Vector< Vector<Object> >();
        for (int i=0; i<artikelListe.originalData.size(); i++){
            if (artikelListe.activeRowBools.get(i) == false){
                // skip deactivated articles
                continue;
            }
            String produktgruppe = artikelListe.originalData.get(i).get(0).toString();
            String lieferant = artikelListe.originalData.get(i).get(1).toString();
            String nummer = artikelListe.originalData.get(i).get(2).toString();
            String name = artikelListe.originalData.get(i).get(3).toString();
            String kurzname = artikelListe.originalData.get(i).get(4).toString();
            BigDecimal menge;
            try {
                menge = new BigDecimal(
                        artikelListe.originalData.get(i).get(5).toString().replace(',','.') );
            } catch (NumberFormatException ex) {
                menge = null;
            }
            String barcode = artikelListe.originalData.get(i).get(6).toString();
            String herkunft = artikelListe.originalData.get(i).get(7).toString();
            Integer vpe;
            try {
                vpe = Integer.parseInt( artikelListe.originalData.get(i).get(8).toString() );
            } catch (NumberFormatException ex) {
                vpe = null;
            }
            Boolean var = artikelListe.varPreisBools.get(i);
            BigDecimal vkp = null;
            BigDecimal ekp = null;
            if (!var){
                try {
                    vkp = new BigDecimal(
                            priceFormatterIntern(artikelListe.originalData.get(i).get(9).toString()));
                } catch (NumberFormatException ex) {
                    vkp = null;
                }
                try {
                    ekp = new BigDecimal(
                            priceFormatterIntern(artikelListe.originalData.get(i).get(10).toString()));
                } catch (NumberFormatException ex) {
                    ekp = null;
                }
            }
            String varStr = var ? "Ja" : "Nein";
            String sortimentStr = artikelListe.sortimentBools.get(i) ? "Ja" : "Nein";

            Vector<Object> row = new Vector<Object>();
                row.add(produktgruppe); row.add(lieferant); row.add(nummer);
                row.add(name); row.add(kurzname); row.add(menge); row.add(barcode);
                row.add(herkunft); row.add(vpe); row.add(vkp); row.add(ekp);
                row.add(varStr); row.add(sortimentStr);
            data.add(row);
        }

        TableModel model = new DefaultTableModel(data, columns);

        try {
            // Save the data to an ODS file and open it.
            SpreadSheet.createEmpty(model).saveAs(file);
            OOUtils.open(file);
        } catch (FileNotFoundException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        //super.actionPerformed(e);
    }
}
