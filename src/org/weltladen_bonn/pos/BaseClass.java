package org.weltladen_bonn.pos;

// Basic Java stuff:
import java.util.*; // for Vector, Collections, String
import java.math.*; // for monetary value representation and arithmetic with correct rounding
import java.io.InputStream;
import java.io.FileInputStream;
import java.text.*; // for NumberFormat
import java.awt.*; // for Color
import javax.swing.*;
import javax.swing.text.*; // for DocumentFilter

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BaseClass {
    private static final Logger logger = LogManager.getLogger(BaseClass.class);

    public final Locale myLocale = Locale.GERMAN;

    // configurable parameters:
    public String currencySymbol;
    public String mysqlHost; /** hostname of mysql server */
    public String mysqlPath; /** path where mysql and mysqldump lie around */
    public String sofficePath; /** path where soffice lies around */
    public String printerName; /** name of receipt printer as set in CUPS */
    public String displayManufacturer; /** manufacturer of customer display */
    public String displayModel; /** model name of customer display */
    public Integer displayWidth; /** number of chars on one row */
    public Integer displayShowWelcomeInterval; /** number of milliseconds after which to show welcome screen */
    public Integer displayBlankInterval; /** number of milliseconds after which to blank screen to prevent burn-in */
    public String dateFormatSQL;
    public String dateFormatJava;
    public String dateFormatDate4j;
    public String delimiter; // for CSV export/import
    public Integer rowsPerPage;
    public BigDecimal sollMuenzKassenstand;
    public BigDecimal sollScheinKassenstand;
    public String exportDirAbrechnungTag;
    public String exportDirAbrechnungMonat;
    public String exportDirAbrechnungJahr;
    public String exportDirBestellung;

    // TSE and DSFinV-K setup:
    public String z_kasse_id; // is used as clientID when talking to TSE

    public final String fileSep = System.getProperty("file.separator");
    public final String lineSep = System.getProperty("line.separator");
    public final int smallintMax = 32767;
    public final BigDecimal zero = new BigDecimal("0");
    public final BigDecimal one = new BigDecimal("1");
    public final BigDecimal minusOne = new BigDecimal("-1");
    public final BigDecimal percent = new BigDecimal("0.01");
    public final BigDecimal hundred = new BigDecimal("100");
    public final BigDecimal thousand = new BigDecimal("1000");

    // Formats to format and parse numbers
    //protected NumberFormat amountFormat;
    protected NumberFormat vatFormat;

    public static Font mediumFont = new Font("Tahoma", Font.BOLD, 16);
    public static Font bigFont = new Font("Tahoma", Font.BOLD, 32);

    public PositiveNumberDocumentFilter geldFilter = new PositiveNumberDocumentFilter(2, 13);
    public PositiveNumberDocumentFilter relFilter = new PositiveNumberDocumentFilter(3, 6);
    public PositiveNumberDocumentFilter mengeFilter = new PositiveNumberDocumentFilter(5, 8);
    public StringDocumentFilter einheitFilter = new StringDocumentFilter(10);
    public StringDocumentFilter nummerFilter = new StringDocumentFilter(30);
    public StringDocumentFilter nameFilter = new StringDocumentFilter(180);
    public StringDocumentFilter kurznameFilter = new StringDocumentFilter(50);
    public StringDocumentFilter herkunftFilter = new StringDocumentFilter(100);
    public IntegerDocumentFilter intFilter;
    public IntegerDocumentFilter vpeFilter;
    public IntegerDocumentFilter beliebtFilter;

    public Vector<Integer> beliebtWerte;
    public Vector<String> beliebtNamen;
    public Vector<String> beliebtBeschreibungen;
    public Vector<String> beliebtKuerzel;
    public Vector<Color> beliebtFarben;
    protected Integer minBeliebt, maxBeliebt;

    public Vector<String> muenz_namen = new Vector<>();
    public Vector<BigDecimal> muenz_werte = new Vector<>();
    public Vector<String> schein_namen = new Vector<>();
    public Vector<BigDecimal> schein_werte = new Vector<>();

    public BaseClass() {
        loadConfigFile();

        //amountFormat = new DecimalFormat("0.00");
        //amountFormat = NumberFormat.getCurrencyInstance(myLocale);
        vatFormat = new DecimalFormat("0.####");

        intFilter = new IntegerDocumentFilter(-smallintMax, smallintMax);
        vpeFilter = new IntegerDocumentFilter(1, smallintMax);

        fillBeliebtWerte();
        fillMuenzWerte();
        fillScheinWerte();
    }

    private void fillBeliebtWerte() {
        beliebtWerte = new Vector<Integer>();
        beliebtNamen = new Vector<String>();
        beliebtBeschreibungen = new Vector<String>();
        beliebtKuerzel = new Vector<String>();
        beliebtFarben = new Vector<Color>();
        beliebtWerte.add(-1);
        beliebtNamen.add("ausgelistet");
        beliebtBeschreibungen.add("Nicht bestellen! (ausgelistet oder nicht bestellbar)");
        beliebtKuerzel.add("x");
        beliebtFarben.add(Color.RED.darker());
        beliebtWerte.add(0);
        beliebtNamen.add("keine Angabe");
        beliebtBeschreibungen.add("?");
        beliebtKuerzel.add("●");
        beliebtFarben.add(Color.GRAY);
        beliebtWerte.add(1);
        beliebtNamen.add("niedrig");
        beliebtBeschreibungen.add("Erst bestellen, wenn nur noch 1 Pkg. im Regal ist");
        beliebtKuerzel.add("●");
        beliebtFarben.add(Color.RED);
        beliebtWerte.add(2);
        beliebtNamen.add("mittel");
        beliebtBeschreibungen.add("Bestellen, wenn noch 3-4 Pkg. im Regal sind (Lager leer)");
        beliebtKuerzel.add("●");
        beliebtFarben.add(Color.YELLOW);
        beliebtWerte.add(3);
        beliebtNamen.add("hoch");
        beliebtBeschreibungen.add("Sollte immer vorrätig sein");
        beliebtKuerzel.add("●");
        beliebtFarben.add(Color.GREEN);
        minBeliebt = Collections.min(beliebtWerte);
        maxBeliebt = Collections.max(beliebtWerte);
        beliebtFilter = new IntegerDocumentFilter(minBeliebt, maxBeliebt);
    }

    private void fillMuenzWerte() {
        muenz_namen.add("1 Cent");
        muenz_namen.add("2 Cent");
        muenz_namen.add("5 Cent");
        muenz_namen.add("10 Cent");
        muenz_namen.add("20 Cent");
        muenz_namen.add("50 Cent");
        muenz_namen.add("1 Euro");
        muenz_namen.add("2 Euro");

        muenz_werte.add(new BigDecimal("0.01"));
        muenz_werte.add(new BigDecimal("0.02"));
        muenz_werte.add(new BigDecimal("0.05"));
        muenz_werte.add(new BigDecimal("0.10"));
        muenz_werte.add(new BigDecimal("0.20"));
        muenz_werte.add(new BigDecimal("0.50"));
        muenz_werte.add(new BigDecimal("1.00"));
        muenz_werte.add(new BigDecimal("2.00"));
    }

    private void fillScheinWerte() {
        schein_namen.add("5 Euro");
        schein_namen.add("10 Euro");
        schein_namen.add("20 Euro");
        schein_namen.add("50 Euro");
        schein_namen.add("100 Euro");
        schein_namen.add("200 Euro");

        schein_werte = new Vector<>();
        schein_werte.add(new BigDecimal("5.00"));
        schein_werte.add(new BigDecimal("10.00"));
        schein_werte.add(new BigDecimal("20.00"));
        schein_werte.add(new BigDecimal("50.00"));
        schein_werte.add(new BigDecimal("100.00"));
        schein_werte.add(new BigDecimal("200.00"));
    }

    private String removeQuotes(String s) {
        return s.replaceAll("\"","");
    }

    private void loadConfigFile() {
        // load config file:
        String filename = "config.properties";
        try {
            InputStream fis = new FileInputStream(filename);
            Properties props = new Properties();
            props.load(fis);

            this.currencySymbol = props.getProperty("currencySymbol");
            this.mysqlHost = props.getProperty("mysqlHost");
            this.mysqlPath = props.getProperty("mysqlPath");
            this.sofficePath = props.getProperty("sofficePath");
            this.printerName = props.getProperty("printerName");
            this.displayManufacturer = props.getProperty("displayManufacturer");
            this.displayModel = props.getProperty("displayModel");
            this.displayWidth = Integer.parseInt(props.getProperty("displayWidth"));
            this.displayShowWelcomeInterval = Integer.parseInt(props.getProperty("displayShowWelcomeInterval"));
            this.displayBlankInterval = Integer.parseInt(props.getProperty("displayBlankInterval"));
            this.dateFormatSQL = props.getProperty("dateFormatSQL");
            this.dateFormatJava = props.getProperty("dateFormatJava");
            this.dateFormatDate4j = props.getProperty("dateFormatDate4j");
            this.delimiter = props.getProperty("delimiter"); // for CSV export/import
            this.rowsPerPage = Integer.parseInt(props.getProperty("rowsPerPage"));
            this.sollMuenzKassenstand = new BigDecimal(props.getProperty("sollMuenzKassenstand"));
            this.sollScheinKassenstand = new BigDecimal(props.getProperty("sollScheinKassenstand"));
            this.exportDirAbrechnungTag = props.getProperty("exportDirAbrechnungTag");
            this.exportDirAbrechnungMonat = props.getProperty("exportDirAbrechnungMonat");
            this.exportDirAbrechnungJahr = props.getProperty("exportDirAbrechnungJahr");
            this.exportDirBestellung = props.getProperty("exportDirBestellung");
            
            // TSE and DSFinV-K setup:
            this.z_kasse_id = props.getProperty("Z_KASSE_ID");
        } catch (Exception ex) {
            logger.error("Exception:", ex);
            JOptionPane.showMessageDialog(null, "Fehler in der Konfigurationsdatei config.properties.\n"+
                    "Es werden die Standardwerte benutzt.", "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            this.currencySymbol = "€";
            this.mysqlHost = "localhost";
            this.mysqlPath = "";
            this.sofficePath = "";
            this.printerName = "quittungsdrucker";
            this.displayManufacturer = "Wincor Nixdorf";
            this.displayModel = "BA63/USB";
            this.displayWidth = 20;
            this.displayShowWelcomeInterval = 30000;
            this.displayBlankInterval = 300000;
            this.dateFormatSQL = "%d.%m.%Y, %H:%i Uhr";
            this.dateFormatJava = "dd.MM.yyyy, HH:mm 'Uhr'";
            this.dateFormatDate4j = "DD.MM.YYYY, hh:mm:ss |Uhr|";
            this.delimiter = ";"; // for CSV export/import
            this.rowsPerPage = 32;
            this.sollMuenzKassenstand = new BigDecimal("50.00");
            this.sollScheinKassenstand = new BigDecimal("100.00");
            this.exportDirAbrechnungTag = "'Dokumente/Kasse/Tagesabrechnungen/'yyyy'/'MM MMMM";
            this.exportDirAbrechnungMonat = "'Dokumente/Kasse/Monatsabrechnungen/'yyyy";
            this.exportDirAbrechnungJahr = "'Dokumente/Kasse/Jahresabrechnungen'";
            this.exportDirBestellung = "'Dokumente/Bestellungen/Bestellungen FHZ 'yyyy";

            // TSE and DSFinV-K setup:
            this.z_kasse_id = "WeltladenBonnKasse-01";
        }
        this.mysqlHost = removeQuotes(this.mysqlHost);
        this.printerName = removeQuotes(this.printerName);
        this.displayManufacturer = removeQuotes(this.displayManufacturer);
        this.displayModel = removeQuotes(this.displayModel);
    }

    /**
     * Nested classes that add style
     */
    public static class BigLabel extends JLabel {
        /**
         * JLabel with bigger than normal font
         */
        private static final long serialVersionUID = 1L;

        public BigLabel() {
            super();
            initialize();
        }

        public BigLabel(String str) {
            super(str);
            initialize();
        }

        private void initialize() {
            this.setFont(mediumFont);
        }
    }

    public static class BigButton extends JButton {
        public BigButton() {
            super();
            initialize();
        }

        public BigButton(String str) {
            super(str);
            initialize();
        }

        private void initialize() {
            this.setFont(mediumFont);
        }
    }


    /**
     * General helper functions
     */

    public static boolean equalsThatHandlesNull(Object a, Object b) {
        if ( (a != null) && (b != null) ){
            if ( a.equals(b) ){ return true; }
        } else {
            if ( (a == null) && (b == null) ){ return true; }
        }
        return false;
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
            BigDecimal price = new BigDecimal(
                priceStr.replace(currencySymbol,"").replaceAll("\\s","").replace(',','.')
            );
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
            BigDecimal vatDecimal = new BigDecimal(
                    vatPercentRemover(vat).replace(',','.')
                    ).multiply(percent);
            return vatDecimal.toString();
        } catch (NumberFormatException nfe) {
            return "";
        }
    }

}
