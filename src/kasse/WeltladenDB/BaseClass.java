package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections, String
import java.math.*; // for monetary value representation and arithmetic with correct rounding
import java.io.InputStream;
import java.io.FileInputStream;
import java.text.*; // for NumberFormat

public class BaseClass {
    public final Locale myLocale = Locale.GERMAN;
    public String currencySymbol;
    public String mysqlPath; /** path where mysql and mysqldump lie around */
    public String sofficePath; /** path where soffice lies around */
    public String printerName; /** name of receipt printer as set in CUPS */
    public String displayManufacturer; /** manufacturer of customer display */
    public String displayModel; /** model name of customer display */
    public String dateFormatSQL;
    public String dateFormatJava;
    public String dateFormatDate4j;
    public String delimiter; // for CSV export/import
    public final String fileSep = System.getProperty("file.separator");
    public final String lineSep = System.getProperty("line.separator");
    public final int smallintMax = 32767;
    public final BigDecimal one = new BigDecimal("1");
    public final BigDecimal minusOne = new BigDecimal("-1");
    public final BigDecimal percent = new BigDecimal("0.01");
    public final BigDecimal hundred = new BigDecimal("100");
    public final BigDecimal thousand = new BigDecimal("1000");

    // Formats to format and parse numbers
    //protected NumberFormat amountFormat;
    protected NumberFormat vatFormat;

    public BaseClass() {
        loadConfigFile();

	//amountFormat = new DecimalFormat("0.00");
	//amountFormat = NumberFormat.getCurrencyInstance(myLocale);
	vatFormat = new DecimalFormat("0.####");
    }

    private String removeQuotes() {
        this.printerName = this.printerName.replaceAll("\"","");
    }

    private void loadConfigFile() {
        // load config file:
        String filename = "config.properties";
        try {
            InputStream fis = new FileInputStream(filename);
            Properties props = new Properties();
            props.load(fis);

            this.currencySymbol = props.getProperty("currencySymbol");
            this.mysqlPath = props.getProperty("mysqlPath");
            this.sofficePath = props.getProperty("sofficePath");
            this.printerName = props.getProperty("printerName");
            this.displayManufacturer = props.getProperty("displayManufacturer");
            this.displayModel = props.getProperty("displayModel");
            this.dateFormatSQL = props.getProperty("dateFormatSQL");
            this.dateFormatJava = props.getProperty("dateFormatJava");
            this.dateFormatDate4j = props.getProperty("dateFormatDate4j");
            this.delimiter = props.getProperty("delimiter"); // for CSV export/import
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            this.currencySymbol = "€";
            this.mysqlPath = "";
            this.sofficePath = "";
            this.printerName = "epson_tmu220";
            this.displayManufacturer = "Wincor Nixdorf";
            this.displayModel = "BA63/USB";
            this.dateFormatSQL = "%d.%m.%Y, %H:%i Uhr";
            this.dateFormatJava = "dd.MM.yyyy, HH:mm 'Uhr'";
            this.dateFormatDate4j = "DD.MM.YYYY, hh:mm |Uhr|";
            this.delimiter = ";"; // for CSV export/import
        }
        this.printerName = removeQuotes(this.printerName);
        this.printerName = removeQuotes(this.displayManufacturer);
        this.printerName = removeQuotes(this.displayModel);
    }

    /**
     * Number formatting methods
     */

    public String decimalMark(String valueStr) {
        /** Use uniform decimal mark */
        if (valueStr == null)
            return "";
        return valueStr.replace('.',',');
    }

    public String unifyDecimalIntern(BigDecimal value) {
        /** Strip trailing zeros */
        if (value == null)
            return "";
        return value.stripTrailingZeros().toPlainString();
    }

    public String unifyDecimal(BigDecimal value) {
        /** Strip trailing zeros and use uniform decimal mark */
        return decimalMark( unifyDecimalIntern(value) );
    }

    public String unifyDecimalIntern(String valueStr) {
        /** Strip trailing zeros */
        if (valueStr == null)
            return "";
        try {
            BigDecimal val = new BigDecimal( valueStr.replaceAll("\\s","").replace(',','.') );
            return unifyDecimalIntern(val);
        } catch (NumberFormatException nfe) {
            return "";
        }
    }

    public String unifyDecimal(String valueStr) {
        /** Strip trailing zeros and use uniform decimal mark */
        return decimalMark( unifyDecimalIntern(valueStr) );
    }

    public String priceFormatterIntern(BigDecimal price) {
        if (price == null){
            return "";
        }
        // for 2 digits after period sign and "0.5-is-rounded-up" rounding:
        return price.setScale(2, RoundingMode.HALF_UP).toString();
    }

    public String priceFormatter(BigDecimal price) {
        return decimalMark( priceFormatterIntern(price) );
    }

    public String priceFormatterIntern(String priceStr) {
        if (priceStr == null)
            return "";
        try {
            BigDecimal price = new BigDecimal( priceStr.replace(currencySymbol,"").replaceAll("\\s","").replace(',','.') );
            return priceFormatterIntern(price);
        } catch (NumberFormatException nfe) {
            return "";
        }
    }

    public String priceFormatter(String priceStr) {
        return decimalMark( priceFormatterIntern(priceStr) );
    }

    public String vatFormatter(String vat) {
        /** Input `vat` is e.g. "0.01"
         *  Returns "1%" */
        if (vat == null)
            return "";
        vat = vat.replace(',','.');
        try {
            String vatFormatted = unifyDecimal(
                    vatFormat.format( new BigDecimal(vat).multiply(hundred) )
                    ) + "%";
            return vatFormatted;
        } catch (NumberFormatException nfe) {
            return "";
        }
    }

    public String vatFormatter(BigDecimal vat) {
        /** Input `vat` is e.g. 0.01
         *  Returns "1%" */
        if (vat == null){
            return "";
        }
        return vatFormatter(vat.toString());
    }

    public String vatPercentRemover(String vat) {
        if (vat == null)
            return "";
        return vat.replace("%","").replaceAll("\\s","");
    }

    public String vatParser(String vat) {
        /** Input `vat` is e.g. "1%"
         *  Returns "0.01" */
        if (vat == null)
            return "";
        try {
            BigDecimal vatDecimal = new BigDecimal( vatPercentRemover(vat).replace(',','.') ).multiply(percent);
            return vatDecimal.toString();
        } catch (NumberFormatException nfe) {
            return "";
        }
    }
}
