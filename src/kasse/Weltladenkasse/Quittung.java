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

// MySQL Connector/J stuff:
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// OpenDocument stuff:
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.jopendocument.dom.OOUtils;

import WeltladenDB.WindowContent;
import WeltladenDB.MainWindowGrundlage;

public class Quittung extends WindowContent {
    private String date;
    private String time;
    private Vector<String> articleNames;
    private Vector<Integer> stueckzahlen;
    private Vector<BigDecimal> einzelpreise;
    private Vector<BigDecimal> preise;
    private Vector<BigDecimal> mwsts;
    private HashMap< BigDecimal, Vector<BigDecimal> > mwstsAndTheirValues;
    private BigDecimal totalPrice;

    private int artikelIndex = 0;
    private int rowOffset = 6;
    private Vector<BigDecimal> mwstList;

    /**
     *    The constructor.
     *       */
    public Quittung(Connection conn, MainWindowGrundlage mw,
            String d, String t,
            Vector<String> an, Vector<Integer> sz,
            Vector<BigDecimal> ep, Vector<BigDecimal> p,
            Vector<BigDecimal> m,
            HashMap< BigDecimal, Vector<BigDecimal> > matv,
            BigDecimal tp) {
	super(conn, mw);
        date = d; time = t;
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
        rowOffset = 6;
        if (artikelIndex > 0){
            // Delete header
            sheet.removeRows(0, 6); // last row is exclusive
            rowOffset = 0;
        } else {
            // Fill header
            sheet.getCellAt("A5").setValue(date);
            sheet.getCellAt("C5").setValue(time);
            rowOffset = 6;
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
                // Save to file and open it.
                File file = new File("test.ods");
                OOUtils.open(sheet.getSpreadSheet().saveAs(file));
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
