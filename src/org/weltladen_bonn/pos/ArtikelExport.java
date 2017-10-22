package org.weltladen_bonn.pos;

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
        FileNameExtensionFilter filter = new FileNameExtensionFilter("ODS Spreadsheet-Dokumente", "ods");
        odsChooser.setFileFilter(filter);
        odsChooser.setSelectedFile(new File("vorlagen" + bc.fileSep + "Artikelliste.ods"));

        int returnVal = odsChooser.showSaveDialog(pw);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File file = odsChooser.getSelectedFile();

            // writeCSVToFile(file);
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
        columnLabels.add("Menge (kg/l/St.)");
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
        final Vector<Vector<Object>> data = new Vector<Vector<Object>>();
        for (int i = 0; i < artikelListe.articles.size(); i++) {
            if (artikelListe.articles.get(i).getAktiv() == false) {
                // skip deactivated articles
                continue;
            }
            String produktgruppe = getProduktgruppe(artikelListe.articles.get(i).getProdGrID());
            String lieferant = getLieferant(artikelListe.articles.get(i).getLiefID());
            String nummer = artikelListe.articles.get(i).getNummer();
            String name = artikelListe.articles.get(i).getName();
            String kurzname = artikelListe.articles.get(i).getKurzname();
            BigDecimal menge = artikelListe.articles.get(i).getMenge();
            String einheit = artikelListe.articles.get(i).getEinheit();
            String sortimentStr = artikelListe.articles.get(i).getSortiment() ? "Ja" : "Nein";
            String lieferbarStr = artikelListe.articles.get(i).getLieferbar() ? "Ja" : "Nein";
            Integer beliebtWert = artikelListe.articles.get(i).getBeliebt();
            Integer beliebtIndex = 0;
            if (beliebtWert != null) {
                try {
                    beliebtIndex = bc.beliebtWerte.indexOf(beliebtWert);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    System.out.println("Unknown beliebtWert: " + beliebtWert);
                }
            }
            String beliebtheit = bc.beliebtNamen.get(beliebtIndex);
            String barcode = artikelListe.articles.get(i).getBarcode();
            Integer vpe = artikelListe.articles.get(i).getVPE();
            Integer setgroesse = artikelListe.articles.get(i).getSetgroesse();
            Boolean var = artikelListe.articles.get(i).getVarPreis();
            BigDecimal vkp = null;
            BigDecimal empf_vkp = null;
            BigDecimal ekrabatt = null;
            BigDecimal ekp = null;
            if (!var) {
                try {
                    vkp = new BigDecimal(artikelListe.articles.get(i).getVKP());
                } catch (NumberFormatException|NullPointerException ex) {
                    vkp = null;
                }
                try {
                    empf_vkp = new BigDecimal(artikelListe.articles.get(i).getEmpfVKP());
                } catch (NumberFormatException|NullPointerException ex) {
                    empf_vkp = null;
                }
                try {
                    ekrabatt = new BigDecimal(bc.vatParser(artikelListe.articles.get(i).getEKRabatt()));
                } catch (NumberFormatException|NullPointerException ex) {
                    ekrabatt = null;
                }
                try {
                    ekp = new BigDecimal(artikelListe.articles.get(i).getEKP());
                } catch (NumberFormatException|NullPointerException ex) {
                    ekp = null;
                }
            }
            String varStr = var ? "Ja" : "Nein";
            String herkunft = artikelListe.articles.get(i).getHerkunft();
            Integer bestand = artikelListe.articles.get(i).getBestand();
            String bestandStr = bestand == null ? "" : bestand.toString();

            Vector<Object> row = new Vector<Object>();
            row.add(produktgruppe);
            row.add(lieferant);
            row.add(nummer);
            row.add(name);
            row.add(kurzname);
            row.add(menge);
            row.add(einheit);
            row.add(sortimentStr);
            row.add(lieferbarStr);
            row.add(beliebtheit);
            row.add(barcode);
            row.add(vpe);
            row.add(setgroesse);
            row.add(vkp);
            row.add(empf_vkp);
            row.add(ekrabatt);
            row.add(ekp);
            row.add(varStr);
            row.add(herkunft);
            row.add(bestandStr);

            // For poor man's progress indication:
            System.out.println(row);

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
     * * Each non abstract class that implements the ActionListener must have
     * this method.
     *
     * @param e
     *            the action event.
     **/
    public void actionPerformed(ActionEvent e) {
    }
}
