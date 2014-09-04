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
//import javax.swing.filechooser.*;
import javax.swing.text.html.HTMLDocument;

public class ArtikelExport extends WindowContent {
    // Attribute:
    private JFileChooser fc;
    private Artikelliste artikelListe;

    // Methoden:
    public ArtikelExport(Connection conn, MainWindowGrundlage mw, Artikelliste pw) {
	super(conn, mw);
        this.artikelListe = pw;

        fc = new JFileChooser(){
            // override approveSelection to get a confirmation dialog if file exists
            @Override
            public void approveSelection(){
                File f = getSelectedFile();
                if (f.exists() && getDialogType() == SAVE_DIALOG){
                    int result = JOptionPane.showConfirmDialog(this,
                            "Datei existiert bereits. Überschreiben?",
                            "Datei existiert",
                            JOptionPane.YES_NO_CANCEL_OPTION);
                    switch (result){
                        case JOptionPane.YES_OPTION:
                            super.approveSelection();
                            return;
                        case JOptionPane.NO_OPTION:
                            return;
                        case JOptionPane.CLOSED_OPTION:
                            return;
                        case JOptionPane.CANCEL_OPTION:
                            cancelSelection();
                            return;
                    }
                }
                super.approveSelection();
            }
        };

        int returnVal = fc.showSaveDialog(pw);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            final File file = fc.getSelectedFile();

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
            columns.add("Art.-Nr."); columns.add("Artikelname"); columns.add("Barcode");
            columns.add("Herkunft"); columns.add("VPE");
            columns.add("VK-Preis"); columns.add("EK-Preis"); columns.add("Variabel");
        // table rows
        final Vector< Vector<Object> > data = new Vector< Vector<Object> >();
        for (int i=0; i<artikelListe.originalData.size(); i++){
            if (artikelListe.activeRowBools.get(i) == false){
                // skip deactivated articles
                continue;
            }
            String produktgruppe = (String)artikelListe.originalData.get(i).get(0);
            String name = (String)artikelListe.originalData.get(i).get(1);
            String nummer = (String)artikelListe.originalData.get(i).get(2);
            String barcode = (String)artikelListe.originalData.get(i).get(3);
            Boolean var = artikelListe.varPreisBools.get(i);
            String vkp = var ? "" : (String)artikelListe.originalData.get(i).get(4);
            String ekp = var ? "" : (String)artikelListe.originalData.get(i).get(5);
            String varStr = var ? "Ja" : "Nein";
            String vpe = (String)artikelListe.originalData.get(i).get(6);
            String lieferant = (String)artikelListe.originalData.get(i).get(10);
            String herkunft = (String)artikelListe.originalData.get(i).get(11);

            Vector<Object> row = new Vector<Object>();
                row.add(produktgruppe); row.add(lieferant);
                row.add(nummer); row.add(name); row.add(barcode);
                row.add(herkunft); row.add(vpe);
                row.add(vkp); row.add(ekp); row.add(varStr);
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

    /*
    void writeCSVToFile(File file) {
        // format of csv file:
        String fileStr = "";
        fileStr += "#Produktgruppe;Artikelname;Art.-Nr.;Barcode;VK-Preis;EK-Preis;Variabel;VPE;Lieferant;Herkunft"+lineSep;
        for (int i=0; i<artikelListe.originalData.size(); i++){
            String produktgruppe = (String)artikelListe.originalData.get(i).get(3);
            String lieferant = (String)artikelListe.originalData.get(i).get(10);
            String nummer = (String)artikelListe.originalData.get(i).get(1);
            String name = (String)artikelListe.originalData.get(i).get(0);
            String barcode = (String)artikelListe.originalData.get(i).get(2);
            String herkunft = (String)artikelListe.originalData.get(i).get(11);
            String vpe = (String)artikelListe.originalData.get(i).get(6);
            Boolean var = artikelListe.varPreisBools.get(i);
            String varStr = var ? "Ja" : "Nein";
            String vkp = var ? "" : (String)artikelListe.originalData.get(i).get(4);
            String ekp = var ? "" : (String)artikelListe.originalData.get(i).get(5);

            fileStr += produktgruppe + delimiter;
            fileStr += lieferant + delimiter;
            fileStr += nummer + delimiter;
            fileStr += name + delimiter;
            fileStr += barcode + delimiter;
            fileStr += herkunft + delimiter;
            fileStr += vpe + delimiter;
            fileStr += vkp + delimiter;
            fileStr += ekp + delimiter;
            fileStr += varStr + lineSep;
        }

        System.out.println(fileStr);

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(fileStr);
        } catch (Exception ex) {
            System.out.println("Error writing to file " + file.getName());
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception ex) {
                System.out.println("Error closing file " + file.getName());
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
    */

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
