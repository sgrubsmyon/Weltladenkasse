package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Process; // for executing system commands
import java.lang.Runtime; // for executing system commands

// GUI stuff:
import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;
import javax.swing.JFrame;
import java.awt.print.*;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

// MySQL Connector/J stuff:
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// DateTime from date4j (http://www.date4j.net/javadoc/index.html)
import hirondelle.date4j.DateTime;

// OpenDocument stuff:
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.jopendocument.dom.OOUtils;
import org.jopendocument.model.OpenDocument;
import org.jopendocument.print.DefaultDocumentPrinter;
import org.jopendocument.print.ODTPrinter;
import org.jopendocument.panel.ODSViewerPanel;

import WeltladenDB.WindowContent;
import WeltladenDB.MainWindowGrundlage;

public class Quittung extends WindowContent {
    private DateTime datetime;
    private Vector<String> articleNames;
    private Vector<Integer> stueckzahlen;
    private Vector<BigDecimal> einzelpreise;
    private Vector<BigDecimal> preise;
    private Vector<BigDecimal> mwsts;
    private LinkedHashMap< BigDecimal, Vector<BigDecimal> > mwstsAndTheirValues;
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
    public Quittung(Connection conn, MainWindowGrundlage mw,
            DateTime dt,
            Vector<String> an, Vector<Integer> sz,
            Vector<BigDecimal> ep, Vector<BigDecimal> p,
            Vector<BigDecimal> m,
            LinkedHashMap< BigDecimal, Vector<BigDecimal> > matv,
            String zm, BigDecimal tp, BigDecimal kgb, BigDecimal rg) {
	super(conn, mw);
        this.datetime = dt;
        this.articleNames = an; this.stueckzahlen = sz;
        this.einzelpreise = ep; this.preise = p;
        this.mwsts = m; this.mwstsAndTheirValues = matv;
        this.zahlungsModus = zm;
        this.totalPrice = tp;
        this.kundeGibt = kgb; this.rueckgeld = rg;
    }

    private Sheet createSheetFromTemplate() {
        final Sheet sheet;
        try {
            String filename = "vorlagen"+fileSep+"Quittung.ods";
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
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
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
            sheet.getCellAt("A5").setValue(datetime.format(dateFormatDate4j));
            rowOffset = 7;
        }
    }

    private int insertItems(Sheet sheet) {
        int row = rowOffset; // start here in ods document
        for (int i=artikelIndex; i<articleNames.size(); i++){
            if (stueckzahlen.get(i) > 1){
                sheet.setValueAt(stueckzahlen.get(i).toString()+" x", 1, row);
                sheet.setValueAt(einzelpreise.get(i), 2, row);
                row++;
            }
            sheet.setValueAt(articleNames.get(i), 0, row);
            sheet.setValueAt(preise.get(i), 3, row);
            Integer mwstIndex = mwstList.indexOf(mwsts.get(i))+1;
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
        if (artikelIndex < articleNames.size()){
            // Delete footer
            sheet.removeRow(215+rowOffset);
        } else {
            // Fill footer
            if (zahlungsModus == "bar"){
                sheet.setValueAt("Bar", 0, 215+rowOffset);
                if (kundeGibt != null && rueckgeld != null){
                    sheet.setValueAt("Kunde gibt", 0, 217+rowOffset);
                    sheet.setValueAt("Rückgeld", 0, 218+rowOffset);
                    sheet.setValueAt(kundeGibt, 3, 217+rowOffset);
                    sheet.setValueAt(rueckgeld, 3, 218+rowOffset);
                }
            } else if (zahlungsModus == "ec"){
                sheet.setValueAt("Karte", 0, 215+rowOffset);
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
            for ( Map.Entry< BigDecimal, Vector<BigDecimal> > entry : mwstsAndTheirValues.entrySet() ){
                sheet.setValueAt(vatFormatter(entry.getKey()), 0, row);
                int col = 1;
                for ( BigDecimal val : entry.getValue() ){
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
        attrSet.add(new PrinterName(this.printerName, null));
        PrintService[] pservices = PrintServiceLookup.lookupPrintServices(null, attrSet);
        PrintService ps;
        try {
            ps = pservices[0];
            job.setPrintService(ps);   // Try setting the printer you want
        } catch (ArrayIndexOutOfBoundsException e){
            System.err.println("Error: No printer named '"+this.printerName+"', using default printer.");
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
        String program = constructProgramPath(this.sofficePath, "soffice");
        String[] executeCmd = new String[] {program, "--headless",
            "-pt", this.printerName, filename};
        //for (String s : executeCmd){
        //    System.out.println(s);
        //}
        try {
            Runtime shell = Runtime.getRuntime();
            Process proc = shell.exec(executeCmd);
            /*
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));
            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));
            System.out.println("Here is the standard output of the soffice print command (if any):");
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }
            System.out.println("Here is the standard error of the command (if any):");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
            */
            int processComplete = proc.waitFor();
            if (processComplete == 0) {
                System.out.println("Printing of file '"+filename+"' with soffice was successful");
            } else {
                System.out.println("Could not print file '"+filename+"' with soffice");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void printReceipt() {
        // Create a list from the set of mwst values
        mwstList = new Vector<BigDecimal>(mwstsAndTheirValues.keySet());
        artikelIndex = 0;
        while (artikelIndex < articleNames.size()) {
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
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            } catch (IOException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
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
