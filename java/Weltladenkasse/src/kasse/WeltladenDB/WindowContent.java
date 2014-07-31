package WeltladenDB;

// Basic Java stuff:
import java.util.Date;
import java.util.Locale;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.math.RoundingMode;

// MySQL Connector/J stuff:
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// GUI stuff:
import java.awt.event.*;
import java.awt.BorderLayout;

//import javax.swing.JFrame;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTable;
//import javax.swing.JTextArea;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerDateModel;

// JCalendarButton
import jcalendarbutton.org.JCalendarButton;

public abstract class WindowContent extends JPanel implements ActionListener {

    // mySQL Connection:
    protected Connection conn = null;
    protected MainWindowGrundlage mainWindow = null;
    protected String currencySymbol;
    protected Locale myLocale = Locale.GERMAN;
    protected final String dateFormatSQL = "%d.%m.%Y, %H:%i Uhr";
    protected final String dateFormatJava = "dd.MM.yyyy, HH:mm 'Uhr'";
    protected final String delimiter = ";"; // for CSV export/import
    protected final String fileSep = System.getProperty("file.separator");
    protected final String lineSep = System.getProperty("line.separator");

    // Die Ausrichter:
    protected DefaultTableCellRenderer rechtsAusrichter = new DefaultTableCellRenderer();
    protected DefaultTableCellRenderer linksAusrichter = new DefaultTableCellRenderer();
    // Formats to format and parse numbers
    protected NumberFormat amountFormat;
    protected NumberFormat vatFormat;

    // Methoden:
    // Setter:
    void setConnection(Connection conn){
	this.conn = conn;
    }
    void setMainWindowPointer(MainWindowGrundlage mw){
	this.mainWindow = mw;
        this.currencySymbol = mw.currencySymbol;
    }
    // Getter:
    Connection getConnection(){
	return this.conn;
    }
    MainWindowGrundlage getMainWindowPointer(){
	return this.mainWindow;
    }

    /**
     *    The constructors.
     *       */
    public WindowContent() { } // default constructor
    public WindowContent(Connection conn, MainWindowGrundlage mw) {
	this.conn = conn;
	amountFormat = new DecimalFormat("0.00");
	//amountFormat = NumberFormat.getCurrencyInstance(myLocale);
	vatFormat = new DecimalFormat("0.####");
	rechtsAusrichter.setHorizontalAlignment(JLabel.RIGHT);
	linksAusrichter.setHorizontalAlignment(JLabel.LEFT);
	this.setLayout(new BorderLayout());

	this.mainWindow = mw;
        this.currencySymbol = mw.currencySymbol;
    }

    /**
     * Price calculation methods
     */

    protected BigDecimal calculateVAT(BigDecimal totalPrice, BigDecimal mwst){
        BigDecimal one = new BigDecimal(1);
        //return totalPrice.multiply( one.subtract( one.divide(one.add(mwst), 10, RoundingMode.HALF_UP) ) ); // VAT = bruttoPreis * ( 1. - 1./(1.+mwst) );
        return totalPrice.divide(one.add(mwst), 10, RoundingMode.HALF_UP).multiply(mwst); // VAT = bruttoPreis / (1.+mwst) * mwst;
    }

    /**
     * Number formatting methods
     */

    protected String priceFormatter(String priceStr) {
        try {
            BigDecimal price = new BigDecimal( priceStr.replace(currencySymbol,"").replaceAll("\\s","").replace(',','.') );
            return priceFormatter(price);
        } catch (NumberFormatException nfe) {
            return "";
        }
    }

    protected String priceFormatterIntern(String priceStr) {
        try {
            BigDecimal price = new BigDecimal( priceStr.replace(currencySymbol,"").replaceAll("\\s","").replace(',','.'));
            return priceFormatterIntern(price);
        } catch (NumberFormatException nfe) {
            return "";
        }
    }

    protected String priceFormatter(BigDecimal price) {
        return priceFormatterIntern(price).replace('.',',');
    }

    protected String priceFormatterIntern(BigDecimal price) {
        //return amountFormat.format( price.setScale(2, RoundingMode.HALF_UP) ).replace(',','.'); // for 2 digits after period sign and "0.5-is-rounded-up" rounding
        return price.setScale(2, RoundingMode.HALF_UP).toString(); // for 2 digits after period sign and "0.5-is-rounded-up" rounding
    }

    protected String vatFormatter(BigDecimal vat) {
        return vatFormatter(vat.toString());
    }

    protected String vatFormatter(String vat) {
        vat = vat.replace(',','.');
        String vatFormatted = "";
        try {
            vatFormatted = vatFormat.format( (new BigDecimal(vat)).multiply(new BigDecimal("100.")) ).replace('.',',') + " %";
        } catch (NumberFormatException nfe) {
            System.out.println("vat = "+vat);
            System.out.println("Exception: " + nfe.getMessage());
            nfe.printStackTrace();
        }
        return vatFormatted;
    }

    /**
     * DB methods
     */

    protected boolean isItemAlreadyKnown(String name, String nummer) {
        boolean exists = false;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT COUNT(artikel_id) FROM artikel WHERE artikel_name = ? AND artikel_nr = ? AND aktiv = TRUE"
                    );
            pstmt.setString(1, name);
            pstmt.setString(2, nummer);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            exists = count > 0;
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return exists;
    }

    protected int setItemInactive(String name, String nummer) {
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "UPDATE artikel SET aktiv = FALSE, bis = NOW() WHERE "+
                    "artikel_name = ? AND "+
                    "artikel_nr = ? AND aktiv = TRUE"
                    );
            pstmt.setString(1, name);
            pstmt.setString(2, nummer);
            result = pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    protected int setItemActive(String name, String nummer) {
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "UPDATE artikel SET aktiv = TRUE, bis = NOW() WHERE "+
                    "artikel_name = ? AND "+
                    "artikel_nr = ? AND aktiv = FALSE"
                    );
            pstmt.setString(1, name);
            pstmt.setString(2, nummer);
            result = pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    protected int insertNewItem(String name, String nummer, String barcode,
            Boolean var_preis, String vkpreis, String ekpreis, Integer vpe,
            Integer produktgruppen_id, Integer lieferant_id,
            String herkunft){
        // add row for new item (with updated fields)
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;

        // prepare value strings:
        if (barcode.equals("") || barcode.equals("NULL")){ barcode = null; }

        BigDecimal vkpDecimal = null;
        if (! vkpreis.equals("")){
            vkpDecimal = new BigDecimal(priceFormatterIntern(vkpreis));
        }

        BigDecimal ekpDecimal = null;
        if (! ekpreis.equals("")){
            ekpDecimal = new BigDecimal(priceFormatterIntern(ekpreis));
        }

        if (vpe.equals(0)){ vpe = null; }

        if (herkunft.equals("") || herkunft.equals("NULL")){ herkunft = null; }

        try {
            Statement stmt = this.conn.createStatement();
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "INSERT INTO artikel SET "+
                    "artikel_name = ?, "+
                    "artikel_nr = ?, "+
                    "barcode = ?, "+
                    "vk_preis = ?, ek_preis = ?, "+
                    "vpe = ?, "+
                    "produktgruppen_id = ?, lieferant_id = ?, "+
                    "herkunft = ?, von = NOW(), " +
                    "aktiv = TRUE, variabler_preis = ?"
                    );
            pstmt.setString(1, name);
            pstmt.setString(2, nummer);
            pstmt.setString(3, barcode);
            pstmt.setBigDecimal(4, vkpDecimal);
            pstmt.setBigDecimal(5, ekpDecimal);
            pstmt.setInt(6, vpe);
            pstmt.setInt(7, produktgruppen_id);
            pstmt.setInt(8, lieferant_id);
            pstmt.setString(9, herkunft);
            pstmt.setBoolean(10, var_preis);
            result = pstmt.executeUpdate();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /**
     * Calendar methods
     */

    protected void setCalButtFromSpinner(SpinnerModel m, JCalendarButton b) {
        if (m instanceof SpinnerDateModel) {
            b.setTargetDate(((SpinnerDateModel)m).getDate());
        }
    }
    protected void setSpinnerFromCalButt(SpinnerModel m, JCalendarButton b, Date earliestDate, Date latestDate) {
        Date newDate = b.getTargetDate();
        if ( earliestDate != null ){
            if ( newDate.before(earliestDate) ){
                newDate = earliestDate;
                b.setTargetDate(newDate);
            }
        }
        if ( latestDate != null ){
            if ( newDate.after(latestDate) ){
                newDate = latestDate;
                b.setTargetDate(newDate);
            }
        }
        if (m instanceof SpinnerDateModel) {
            if (newDate != null){
                ((SpinnerDateModel)m).setValue(newDate);
            }
        }
    }
}
