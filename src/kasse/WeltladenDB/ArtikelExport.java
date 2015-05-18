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

    private final HashMap<String, Integer> indexMap;

    // Methoden:
    public ArtikelExport(Connection conn, MainWindowGrundlage mw, Artikelliste pw, HashMap<String, Integer> idxMap) {
	super(conn, mw);
        this.artikelListe = pw;
        this.indexMap = idxMap;

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
        final Vector<String> columnLabels = new Vector<String>();
            columnLabels.add("Produktgruppe");
            columnLabels.add("Lieferant");
            columnLabels.add("Artikelnummer");
            columnLabels.add("Bezeichnung | Einheit");
            columnLabels.add("Kurzname");
            columnLabels.add("Menge (kg/l/Stk.)");
            columnLabels.add("Einheit");
            columnLabels.add("Sortiment");
            columnLabels.add("Sofort lieferbar");
            columnLabels.add("Beliebtheit");
            columnLabels.add("Barcode");
            columnLabels.add("VPE");
            columnLabels.add("Setgröße");
            columnLabels.add("VK-Preis");
            columnLabels.add("Empf. VK-Preis");
            columnLabels.add("EK-Rabatt");
            columnLabels.add("EK-Preis");
            columnLabels.add("Variabel");
            columnLabels.add("Herkunftsland");
            columnLabels.add("Bestand");
        // table rows
        final Vector< Vector<Object> > data = new Vector< Vector<Object> >();
        for (int i=0; i<artikelListe.originalData.size(); i++){
            if (artikelListe.activeRowBools.get(i) == false){
                // skip deactivated articles
                continue;
            }
            String produktgruppe = artikelListe.originalData.get(i).get(indexMap.get("produktgruppe")).toString();
            String lieferant = artikelListe.originalData.get(i).get(indexMap.get("lieferant")).toString();
            String nummer = artikelListe.originalData.get(i).get(indexMap.get("nummer")).toString();
            String name = artikelListe.originalData.get(i).get(indexMap.get("name")).toString();
            String kurzname = artikelListe.originalData.get(i).get(indexMap.get("kurzname")).toString();
            BigDecimal menge;
            try {
                menge = new BigDecimal(
                        artikelListe.originalData.get(i).get(indexMap.get("menge")).toString().replace(',','.') );
            } catch (NumberFormatException ex) {
                menge = null;
            }
            String einheit = artikelListe.originalData.get(i).get(indexMap.get("einheit")).toString();
            String sortimentStr = artikelListe.sortimentBools.get(i) ? "Ja" : "Nein";
            String lieferbarStr = artikelListe.lieferbarBools.get(i) ? "Ja" : "Nein";
            String beliebtheit = beliebtNamen.get(artikelListe.beliebtIndices.get(i));
            String barcode = artikelListe.originalData.get(i).get(indexMap.get("barcode")).toString();
            Integer vpe;
            try {
                vpe = Integer.parseInt( artikelListe.originalData.get(i).get(indexMap.get("vpe")).toString() );
            } catch (NumberFormatException ex) {
                vpe = null;
            }
            Integer setgroesse;
            try {
                setgroesse = Integer.parseInt( artikelListe.originalData.get(i).get(indexMap.get("setgroesse")).toString() );
            } catch (NumberFormatException ex) {
                setgroesse = null;
            }
            Boolean var = artikelListe.varPreisBools.get(i);
            BigDecimal vkp = null;
            BigDecimal empf_vkp = null;
            BigDecimal ekrabatt = null;
            BigDecimal ekp = null;
            if (!var){
                try {
                    vkp = new BigDecimal(
                            priceFormatterIntern(artikelListe.originalData.get(i).get(indexMap.get("vkp")).toString()));
                } catch (NumberFormatException ex) {
                    vkp = null;
                }
                try {
                    empf_vkp = new BigDecimal(
                            priceFormatterIntern(artikelListe.originalData.get(i).get(indexMap.get("evkp")).toString()));
                } catch (NumberFormatException ex) {
                    empf_vkp = null;
                }
                try {
                    ekrabatt = new BigDecimal( vatParser(artikelListe.originalData.get(i).get(indexMap.get("ekr")).toString()) );
                } catch (NumberFormatException ex) {
                    ekrabatt = null;
                }
                try {
                    ekp = new BigDecimal(
                            priceFormatterIntern(artikelListe.originalData.get(i).get(indexMap.get("ekp")).toString()));
                } catch (NumberFormatException ex) {
                    ekp = null;
                }
            }
            String varStr = var ? "Ja" : "Nein";
            String herkunft = artikelListe.originalData.get(i).get(indexMap.get("herkunft")).toString();
            Integer bestand;
            try {
                bestand = Integer.parseInt( artikelListe.originalData.get(i).get(indexMap.get("bestand")).toString() );
            } catch (NumberFormatException ex) {
                bestand = null;
            }

            Vector<Object> row = new Vector<Object>();
                row.add(produktgruppe); row.add(lieferant); row.add(nummer);
                row.add(name); row.add(kurzname); row.add(menge);
                row.add(einheit); row.add(sortimentStr); row.add(lieferbarStr);
                row.add(beliebtheit); row.add(barcode); row.add(vpe);
                row.add(setgroesse); row.add(vkp); row.add(empf_vkp);
                row.add(ekrabatt); row.add(ekp); row.add(varStr);
                row.add(herkunft); row.add(bestand);
            data.add(row);
        }

        TableModel model = new DefaultTableModel(data, columnLabels);

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
    }
}
