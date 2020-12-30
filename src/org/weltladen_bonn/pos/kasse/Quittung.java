package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.io.*;
import java.lang.Process; // for executing system commands
import java.lang.Runtime; // for executing system commands

// GUI stuff:
import javax.swing.*; // JFrame, JPanel, JTable, JButton etc.
import java.awt.event.ActionEvent;
import java.awt.print.*;
import javax.print.*; // PrintService
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

// MySQL Connector/J stuff:
import org.mariadb.jdbc.MariaDbPoolDataSource;

// DateTime from date4j (http://www.date4j.net/javadoc/index.html)
import hirondelle.date4j.DateTime;

// jOpenDocument stuff:
import org.jopendocument.dom.spreadsheet.*; // SpreadSheet
import org.jopendocument.dom.OOUtils;
import org.jopendocument.model.OpenDocument;
import org.jopendocument.print.ODTPrinter;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.weltladen_bonn.pos.*;

public class Quittung extends WindowContent {
    private static final Logger logger = LogManager.getLogger(Quittung.class);

    private DateTime datetime;
    private Integer rechnungsNr;
    private Vector<KassierArtikel> kassierArtikel;
    private LinkedHashMap< Integer, Vector<BigDecimal> > mwstsAndTheirValues;
    private String zahlungsModus;
    private BigDecimal totalPrice;
    private BigDecimal kundeGibt;
    private BigDecimal rueckgeld;

    private int artikelIndex = 0;
    private int rowOffset = 7;
    private Vector<BigDecimal> mwstList;

    /**
     *    The constructor.
     *       */
    public Quittung(MariaDbPoolDataSource pool, MainWindowGrundlage mw,
            DateTime dt, Integer rechnungsNr,
            Vector<KassierArtikel> ka,
            LinkedHashMap< Integer, Vector<BigDecimal> > matv,
            String zm, BigDecimal tp, BigDecimal kgb, BigDecimal rg) {
	    super(pool, mw);
        this.datetime = dt;
        this.rechnungsNr = rechnungsNr;
        this.kassierArtikel = ka;
        this.mwstsAndTheirValues = matv;
        this.zahlungsModus = zm;
        this.totalPrice = tp;
        this.kundeGibt = kgb; this.rueckgeld = rg;
    }

    private Sheet createSheetFromTemplate() {
        final Sheet sheet;
        try {
            String filename = "vorlagen"+bc.fileSep+"Quittung.ods";
            File infile = new File(filename);
            if (!infile.exists()){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Quittungsvorlage "+
                        "'"+filename+"' nicht gefunden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            sheet = SpreadSheet.createFromFile(infile).getSheet(0);
        } catch (IOException ex) {
            logger.error("Exception:", ex);
            return null;
        }
        return sheet;
    }

    private void editHeader(Sheet sheet) {
        // if this is not the first page:
        rowOffset = 7;
        if (artikelIndex > 0){
            // Delete header
            sheet.removeRows(0, rowOffset); // last row is exclusive
            rowOffset = 0;
        } else {
            // Fill header
            if (datetime != null) {
                sheet.getCellAt("A5").setValue(datetime.format(bc.dateFormatDate4j));
            }
            if (rechnungsNr != null) {
                sheet.getCellAt("D6").setValue(rechnungsNr);
            } else {
                sheet.getCellAt("A6").setValue("");
            }
            rowOffset = 7;
        }
    }

    private int insertItems(Sheet sheet) {
        int row = rowOffset; // start here in ods document
        for (int i=artikelIndex; i<kassierArtikel.size(); i++){
            if (kassierArtikel.get(i).getStueckzahl() > 1){
                sheet.setValueAt(kassierArtikel.get(i).getStueckzahl().toString()+" x", 1, row);
                sheet.setValueAt(kassierArtikel.get(i).getEinzelPreis(), 2, row);
                row++;
            }
            sheet.setValueAt(kassierArtikel.get(i).getName(), 0, row);
            sheet.setValueAt(kassierArtikel.get(i).getGesPreis(), 3, row);
            Integer mwstIndex = mwstList.indexOf(kassierArtikel.get(i).getMwst())+1;
            sheet.setValueAt(mwstIndex, 4, row);
            row++;
            artikelIndex = i+1; // index of next item
            // if list becomes too long: print this sheet and create new sheet
            if (row > 199+rowOffset){
                break;
            }
        }
        return row; // first empty row
    }

    private void editFooter(Sheet sheet) {
        // if this is not the last page:
        if (artikelIndex < kassierArtikel.size()){
            // Delete footer
            sheet.removeRow(215+rowOffset);
        } else {
            // Fill footer
            if ( zahlungsModus.equals("bar") ){
                sheet.setValueAt("Bar", 0, 215+rowOffset);
                if (kundeGibt != null && rueckgeld != null){
                    sheet.setValueAt("Kunde gibt", 0, 216+rowOffset);
                    sheet.setValueAt("Rückgeld", 0, 217+rowOffset);
                    sheet.setValueAt(kundeGibt, 3, 216+rowOffset);
                    sheet.setValueAt(rueckgeld, 3, 217+rowOffset);
                }
            } else if ( zahlungsModus.equals("ec") ){
                sheet.setValueAt("EC", 0, 215+rowOffset);
            }
            sheet.setValueAt(totalPrice, 2, 215+rowOffset);
            // fill mwst values
            int row = 213+rowOffset-mwstList.size(); // now at header of mwst values
            sheet.setValueAt("MwSt.", 0, row);
            sheet.setValueAt("Netto", 1, row);
            sheet.setValueAt("Steuer", 2, row);
            sheet.setValueAt("Umsatz", 3, row);
            row++;
            Integer mwstIndex = 1;
            for ( Vector<BigDecimal> mwstValues : mwstsAndTheirValues.values() ){
                BigDecimal steuersatz = mwstValues.get(0);
                sheet.setValueAt(bc.vatFormatter(steuersatz), 0, row);
                int col = 1;
                for (int i = 1; i < mwstValues.size(); i++) { // omit first value (Steuersatz)
                    BigDecimal val = mwstValues.get(i);
                    sheet.setValueAt(val, col, row);
                    col++;
                }
                sheet.setValueAt(mwstIndex, col, row);
                row++;
                mwstIndex++;
            }
        }
    }

    void printQuittungFromJava(File tmpFile) {
        /** Complicated method: printing from Java (doesn't work) */
        final OpenDocument doc = new OpenDocument();
        doc.loadFrom(tmpFile);
        //doc.loadFrom(new File("/tmp/Untitled.ods"));

        // Print.
        ODTPrinter printer = new ODTPrinter(doc);
        PrinterJob job = PrinterJob.getPrinterJob();

        // Get a handle on the printer, by requiring a printer with the given name
        PrintServiceAttributeSet attrSet = new HashPrintServiceAttributeSet();
        attrSet.add(new PrinterName(bc.printerName, null));
        PrintService[] pservices = PrintServiceLookup.lookupPrintServices(null, attrSet);
        PrintService ps;
        try {
            ps = pservices[0];
            job.setPrintService(ps);   // Try setting the printer you want
        } catch (ArrayIndexOutOfBoundsException e){
            System.err.println("Error: No printer named '"+bc.printerName+"', using default printer.");
        } catch (PrinterException exception) {
            System.err.println("Printing error: " + exception);
        }
        PageFormat pageFormat = job.defaultPage();
        Paper paper = pageFormat.getPaper();
        paper.setSize(76. / 25.4 * 72., 279.4 / 25.4 * 72.);
        paper.setImageableArea(8. / 25.4 * 72., 3. / 25.4 * 72., 68. / 25.4 * 72., 259.4 / 25.4 * 72.);
        pageFormat.setPaper(paper);

        // Book with setPageable instead of setPrintable
        Book book = new Book();//java.awt.print.Book
        book.append(printer, pageFormat);
        job.setPageable(book);

        try {
            job.print();   // Actual print command
        } catch (PrinterException exception) {
            System.err.println("Printing error: " + exception);
        }
    }

    private void printQuittungWithSoffice(String filename) {
        /** Simple method: printing with openoffice command line tool 'soffice' */
        String program = constructProgramPath(bc.sofficePath, "soffice");
        String[] executeCmd = new String[] {program, "--headless",
            "-pt", bc.printerName, filename};
        String log = "Print command: ";
        for (String s : executeCmd){
            log += s+" ";
        }
        logger.info(log);
        try {
            Runtime shell = Runtime.getRuntime();
            Process proc = shell.exec(executeCmd);
            int processComplete = proc.waitFor();
            if (processComplete == 0) {
                logger.info("Printing of file '"+filename+"' with soffice was successful");
            } else {
                logger.info("Could not print file '"+filename+"' with soffice");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void printReceipt() {
        // Create a list from the set of mwst values
        mwstList = new Vector<BigDecimal>(mwstsAndTheirValues.size());
        for (int i = 0; i < mwstsAndTheirValues.size(); i++) {
            mwstList.add(mwstsAndTheirValues.values().get(i).get(0));
        }
        artikelIndex = 0;
        while (artikelIndex < kassierArtikel.size()) {
            // Create a new sheet from the template file
            final Sheet sheet = createSheetFromTemplate();
            if (sheet == null) return;

            editHeader(sheet);

            // Insert items
            int startRemRow = insertItems(sheet);
            int endRemRow = 213+rowOffset-mwstList.size()-1; // two rows above header of mwst values (one row of
                                                            // empty space)
            editFooter(sheet);

            // Remove all empty rows between item list and footer
            sheet.removeRows(startRemRow, endRemRow); // last row is exclusive

            try {
                // Save to temp file.
                File tmpFile = File.createTempFile("Quittung", ".ods");
                //OOUtils.open(sheet.getSpreadSheet().saveAs(tmpFile)); // for testing
                sheet.getSpreadSheet().saveAs(tmpFile);

                // Complicated method: printing from Java (doesn't work)
                //printQuittungFromJava(tmpFile);

                // Simple method: printing with openoffice command line tool 'soffice'
                printQuittungWithSoffice(tmpFile.getAbsolutePath());

                tmpFile.deleteOnExit();
                //tmpFile.delete();
            } catch (FileNotFoundException ex) {
                logger.error("Exception:", ex);
            } catch (IOException ex) {
                logger.error("Exception:", ex);
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
    }
}
