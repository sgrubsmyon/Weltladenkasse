package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

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
    private BigDecimal totalPrice;

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
            BigDecimal tp) {
	super(conn, mw);
        datetime = dt;
        articleNames = an; stueckzahlen = sz;
        einzelpreise = ep; preise = p;
        mwsts = m; mwstsAndTheirValues = matv;
        totalPrice = tp;
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
            //sheet.getCellAt("C5").setValue(datetime.format('hh:mm'));
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
                //OOUtils.open(sheet.getSpreadSheet().saveAs(tmpFile));
                sheet.getSpreadSheet().saveAs(tmpFile);
                final OpenDocument doc = new OpenDocument();
                doc.loadFrom(tmpFile);

                // Show time !
                final JFrame mainFrame = new JFrame("Viewer");
                DefaultDocumentPrinter printer = new DefaultDocumentPrinter();
                ODSViewerPanel viewerPanel = new ODSViewerPanel(doc, printer, true);
                mainFrame.setContentPane(viewerPanel);
                mainFrame.pack();
                mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainFrame.setLocation(10, 10);
                mainFrame.setVisible(true);

                /*
                // Print.
                ODTPrinter printer = new ODTPrinter(doc);
                //PrinterJob job = PrinterJob.getPrinterJob();
                //job.setPrintable(printer);
                //boolean doPrint = job.printDialog();
                //if (doPrint){
                //    try {
                //        job.print();
                //    } catch (PrinterException ex) {
                //        System.out.println("The job did not successfully complete. Exception: " + ex.getMessage());
                //        ex.printStackTrace();
                //    }
                //}

                PrinterJob job = PrinterJob.getPrinterJob();
                String printerName = "epson_tmu220";

                PrintServiceAttributeSet attrSet = new HashPrintServiceAttributeSet();
                attrSet.add(new PrinterName(printerName, null));
                //attrSet.add(MediaSizeName.ISO_A4);
                //attrSet.add(new MediaPrintableArea(2, 2, 60, 80, MediaPrintableArea.MM));
                PrintService[] pservices = PrintServiceLookup.lookupPrintServices(null, attrSet);
                System.out.println("PrintServices: "+pservices);
                PrintService ps;
                PageFormat pageFormat;
                try {
                    ps = pservices[0];
                    job.setPrintService(ps);   // Try setting the printer you want
                } catch (ArrayIndexOutOfBoundsException e){
                    System.err.println("Error: No printer named '"+printerName+"', using default printer.");
                    //pageFormat = job.defaultPage();  // Set the default printer instead.
                } catch (PrinterException exception) {
                    System.err.println("Printing error: " + exception);
                }
                //pageFormat = new PageFormat();    // If you want to adjust heigh and width etc. of your paper.
                pageFormat = job.defaultPage();
                Paper paper = pageFormat.getPaper();
                paper.setSize(64. / 25.4 * 72., 80. / 25.4 * 72.);
                paper.setImageableArea(0. / 25.4 * 72., 0. / 25.4 * 72., 62. / 25.4 * 72., 80. / 25.4 * 72.);
                pageFormat.setPaper(paper);
                System.out.println("pageFormat height: "+pageFormat.getHeight());
                System.out.println("pageFormat width: "+pageFormat.getWidth());

                //job.setPrintable(printer, pageFormat);
                Book book = new Book();//java.awt.print.Book
                book.append(new HelloWorldPrinter(), pageFormat);
                book.append(printer, pageFormat);
                job.setPageable(book);
                //job.setPrintable(new HelloWorldPrinter(), pageFormat);

                try {
                    job.print();   // Actual print command
                } catch (PrinterException exception) {
                    System.err.println("Printing error: " + exception);
                }
                */

                //printer.print(doc);
                tmpFile.deleteOnExit();
                //tmpFile.delete();
            } catch (FileNotFoundException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            } catch (IOException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }

            //} catch (FileNotFoundException ex) {
            //    System.out.println("Exception: " + ex.getMessage());
            //    ex.printStackTrace();
            //} catch (IOException ex) {
            //    System.out.println("Exception: " + ex.getMessage());
            //    ex.printStackTrace();
            //}

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
