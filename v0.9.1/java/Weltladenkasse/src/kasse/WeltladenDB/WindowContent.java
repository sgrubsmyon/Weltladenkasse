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

    protected BigDecimal calculateVAT(BigDecimal totalPrice, BigDecimal mwst){
        BigDecimal one = new BigDecimal(1);
        //return totalPrice.multiply( one.subtract( one.divide(one.add(mwst), 10, RoundingMode.HALF_UP) ) ); // VAT = bruttoPreis * ( 1. - 1./(1.+mwst) );
        return totalPrice.divide(one.add(mwst), 10, RoundingMode.HALF_UP).multiply(mwst); // VAT = bruttoPreis / (1.+mwst) * mwst;
    }

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

    protected String vatFormatter(String vat) {
        return vatFormat.format( (new BigDecimal(vat)).multiply(new BigDecimal("100.")) ).replace('.',',') + " %";
    }

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
