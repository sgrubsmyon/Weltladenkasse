package WeltladenDB;

// Basic Java stuff:
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
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
import java.sql.Types;

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
    protected String mysqlPath;
    protected String dateFormatSQL;
    protected String dateFormatJava;
    protected String delimiter; // for CSV export/import
    protected final String fileSep = System.getProperty("file.separator");
    protected final String lineSep = System.getProperty("line.separator");

    // Die Ausrichter:
    protected DefaultTableCellRenderer rechtsAusrichter = new DefaultTableCellRenderer();
    protected DefaultTableCellRenderer linksAusrichter = new DefaultTableCellRenderer();
    protected DefaultTableCellRenderer zentralAusrichter = new DefaultTableCellRenderer();
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
    public WindowContent(Connection conn, MainWindowGrundlage mw) {
	this.conn = conn;
	amountFormat = new DecimalFormat("0.00");
	//amountFormat = NumberFormat.getCurrencyInstance(myLocale);
	vatFormat = new DecimalFormat("0.####");
	rechtsAusrichter.setHorizontalAlignment(JLabel.RIGHT);
	linksAusrichter.setHorizontalAlignment(JLabel.LEFT);
	zentralAusrichter.setHorizontalAlignment(JLabel.CENTER);
	this.setLayout(new BorderLayout());

	this.mainWindow = mw;
        this.currencySymbol = mw.currencySymbol;

        // load config file:
        String filename = "config.properties";
        try {
            InputStream fis = new FileInputStream(filename);
            Properties props = new Properties();
            props.load(fis);

            this.mysqlPath = props.getProperty("mysqlPath"); // path where mysql and mysqldump lie around
            this.dateFormatSQL = props.getProperty("dateFormatSQL");
            this.dateFormatJava = props.getProperty("dateFormatJava");
            this.delimiter = props.getProperty("delimiter"); // for CSV export/import
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            this.mysqlPath = "";
            this.dateFormatSQL = "%d.%m.%Y, %H:%i Uhr";
            this.dateFormatJava = "dd.MM.yyyy, HH:mm 'Uhr'";
            this.delimiter = ";"; // for CSV export/import
        }
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

    protected void pstmtSetInteger(PreparedStatement pstmt, int paramIndex, Integer x) {
        /** Home made method to put Integer class instances (that can be null)
         *  into a DB and treat them accordingly (Java null becomes SQL NULL) */
        try {
            if (x == null){
                pstmt.setNull(paramIndex, Types.INTEGER);
            } else {
                pstmt.setInt(paramIndex, x);
            }
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    protected void pstmtSetBoolean(PreparedStatement pstmt, int paramIndex, Boolean x) {
        /** Home made method to put Boolean class instances (that can be null)
         *  into a DB and treat them accordingly (Java null becomes SQL NULL) */
        try {
            if (x == null){
                pstmt.setNull(paramIndex, Types.INTEGER);
            } else {
                pstmt.setBoolean(paramIndex, x);
            }
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    protected boolean isItemAlreadyKnown(String lieferant, String nummer) {
        boolean exists = false;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT COUNT(artikel_id) FROM artikel INNER JOIN lieferant USING (lieferant_id) "+
                    "WHERE lieferant_name = ? AND artikel_nr = ? AND artikel.aktiv = TRUE"
                    );
            pstmt.setString(1, lieferant);
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

    protected int setItemInactive(Integer lieferantid, String nummer) {
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "UPDATE artikel SET aktiv = FALSE, bis = NOW() WHERE "+
                    "lieferant_id = ? AND "+
                    "artikel_nr = ? AND aktiv = TRUE"
                    );
            pstmt.setInt(1, lieferantid);
            pstmt.setString(2, nummer);
            result = pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    protected int setItemActive(Integer lieferantid, String nummer) {
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "UPDATE artikel SET aktiv = TRUE, bis = NOW() WHERE "+
                    "lieferant_id = ? AND "+
                    "artikel_nr = ? AND aktiv = FALSE"
                    );
            pstmt.setInt(1, lieferantid);
            pstmt.setString(2, nummer);
            result = pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    protected int insertNewItem(Integer produktgruppen_id, Integer
            lieferant_id, String nummer, String name, BigDecimal menge, String
            barcode, String herkunft, Integer vpe, String vkpreis, String
            ekpreis, Boolean var_preis, Boolean sortiment) {
        // add row for new item (with updated fields)
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;

        if (barcode.equals("") || barcode.equals("NULL")){ barcode = null; }

        if (herkunft.equals("") || herkunft.equals("NULL")){ herkunft = null; }

        if ( vpe == null || vpe.equals(0) ){ vpe = null; }

        BigDecimal vkpDecimal;
        try {
            vkpDecimal = new BigDecimal(priceFormatterIntern(vkpreis));
        } catch (NumberFormatException ex) {
            vkpDecimal = null;
        }

        BigDecimal ekpDecimal;
        try {
            ekpDecimal = new BigDecimal(priceFormatterIntern(ekpreis));
        } catch (NumberFormatException ex) {
            ekpDecimal = null;
        }

        try {
            Statement stmt = this.conn.createStatement();
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "INSERT INTO artikel SET "+
                    "produktgruppen_id = ?, lieferant_id = ?, "+
                    "artikel_nr = ?, artikel_name = ?, "+
                    "menge = ?, "+
                    "barcode = ?, "+
                    "herkunft = ?, "+
                    "vpe = ?, "+
                    "vk_preis = ?, ek_preis = ?, "+
                    "variabler_preis = ?, sortiment = ?, "+
                    "von = NOW(), aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, produktgruppen_id);
            pstmtSetInteger(pstmt, 2, lieferant_id);
            pstmt.setString(3, nummer);
            pstmt.setString(4, name);
            pstmt.setBigDecimal(5, menge);
            pstmt.setString(6, barcode);
            pstmt.setString(7, herkunft);
            pstmtSetInteger(pstmt, 8, vpe);
            pstmt.setBigDecimal(9, vkpDecimal);
            pstmt.setBigDecimal(10, ekpDecimal);
            pstmtSetBoolean(pstmt, 11, var_preis);
            pstmtSetBoolean(pstmt, 12, sortiment);
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
