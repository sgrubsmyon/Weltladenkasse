package org.weltladen_bonn.pos;

// Basic Java stuff:
import java.util.*; // for Vector, Collections, String
import java.math.*; // for monetary value representation and arithmetic with correct rounding
import java.io.InputStream;
import java.io.FileInputStream;
import java.text.*; // for NumberFormat
import java.awt.*; // for Color
import javax.swing.*;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BaseClass {
    private static final Logger logger = LogManager.getLogger(BaseClass.class);

    public final Locale myLocale = Locale.GERMAN;

    // configurable parameters (set default values here, overwritten later when parsing config.txt):
    public String currencySymbol = "€";
    public String operationMode = "normal"; /** either 'normal' or 'training' (TSE logs transactions explicitly as AVTraining and they are written to special SQL table) */
    public String mysqlHost = "localhost"; /** hostname of mysql server */
    public String mysqlPath = ""; /** path where mysql and mysqldump lie around */
    public String sofficePath = ""; /** path where soffice lies around */
    public String printerName = "quittungsdrucker"; /** name of receipt printer as set in CUPS */
    public String printerDeviceFile = "/dev/ttyUSB0"; /** name of receipt printer as set in CUPS */
    public String displayManufacturer = "Wincor Nixdorf"; /** manufacturer of customer display */
    public String displayModel = "BA63/USB"; /** model name of customer display */
    public Integer displayWidth = 20; /** number of chars on one row */
    public Integer displayShowWelcomeInterval = 30000; /** number of milliseconds after which to show welcome screen */
    public Integer displayBlankInterval = 300000; /** number of milliseconds after which to blank screen to prevent burn-in */
    public String dateFormatSQL = "%d.%m.%Y, %H:%i Uhr";
    public String dateFormatJava = "dd.MM.yyyy, HH:mm 'Uhr'";
    public String dateFormatDate4j = "DD.MM.YYYY, hh:mm:ss |Uhr|";
    public String delimiter = ";"; // for CSV export/import
    public Integer rowsPerPage = 32;
    public BigDecimal sollMuenzKassenstand = new BigDecimal("50.00");
    public BigDecimal sollScheinKassenstand = new BigDecimal("100.00");
    public String exportDirAbrechnungTag = "'Dokumente/Kasse/Tagesabrechnungen/'yyyy'/'MM MMMM";
    public String exportDirAbrechnungMonat = "'Dokumente/Kasse/Monatsabrechnungen/'yyyy";
    public String exportDirAbrechnungJahr = "'Dokumente/Kasse/Jahresabrechnungen'";
    public String exportDirBestellung = "'Dokumente/Bestellungen/Bestellungen FHZ 'yyyy";

    // TSE and DSFinV-K setup:
    public String finDatDir = ".Weltladenkasse_Finanzdaten"; // for export of financial data
    public String Z_KASSE_ID = "YLUE036019"; // is used as clientID when talking to TSE
    public String TAXONOMIE_VERSION = "2.2";
    public String NAME;
    public String STRASSE;
    public String PLZ;
    public String ORT;
    public String LAND;
    public String STNR;
    public String USTID;
    public String NAME_FOR_RECEIPT;
    public String PHONE;
    public String URL;
    public String LOC_NAME;
    public String LOC_STRASSE;
    public String LOC_PLZ;
    public String LOC_ORT;
    public String LOC_LAND;
    public String LOC_USTID;
    public String KASSE_BRAND;
    public String KASSE_MODELL;
    public String KASSE_SERIENNR;
    public String KASSE_SW_BRAND = "Weltladenkasse";
    public String KASSE_SW_VERSION;
    public String KASSE_BASISWAEH_CODE = "EUR";
    public Integer TERMINAL_ID;
    public String TERMINAL_BRAND;
    public String TERMINAL_MODELL;
    public String TERMINAL_SERIENNR;
    public String TERMINAL_SW_BRAND = "Weltladenkasse";
    public String TERMINAL_SW_VERSION;
    public Integer TSE_ID;
    public String TSE_PD_ENCODING = "UTF-8";

    // Lexware setup:
    public String LEXWARE_BELEGDATUM_FORMAT;
    public String LEXWARE_BELEGNUMMERNKREIS;
    public String LEXWARE_BUCHUNGSTEXT_ERLOESE;
    public String LEXWARE_BUCHUNGSTEXT_ERLOESE_OHNE_STEUER;
    public String LEXWARE_BUCHUNGSTEXT_GELDTRANSIT_KARTE;
    public String LEXWARE_BUCHUNGSTEXT_GELDTRANSIT_KASSE;
    public String LEXWARE_BUCHUNGSTEXT_KASSENDIFFERENZ;
    public Integer LEXWARE_SOLL_KONTO_ERLOESE;
    public Integer LEXWARE_SOLL_KONTO_GELDTRANSIT_KARTE;
    public Integer LEXWARE_SOLL_KONTO_GELDTRANSIT_KASSE;
    public Integer LEXWARE_SOLL_KONTO_KASSENDIFFERENZ;
    public Integer LEXWARE_HABEN_KONTO_ERLOESE_OHNE_STEUER;
    public Integer LEXWARE_HABEN_KONTO_ERLOESE_REDUZIERTE_STEUER;
    public Integer LEXWARE_HABEN_KONTO_ERLOESE_NORMALE_STEUER;
    public Integer LEXWARE_HABEN_KONTO_GELDTRANSIT_KARTE;
    public Integer LEXWARE_HABEN_KONTO_GELDTRANSIT_KASSE;
    public Integer LEXWARE_HABEN_KONTO_KASSENDIFFERENZ;
    public Integer LEXWARE_STEUERSCHLUESSEL_OHNE_STEUER = null;
    public Integer LEXWARE_STEUERSCHLUESSEL_REDUZIERTE_STEUER = null;
    public Integer LEXWARE_STEUERSCHLUESSEL_NORMALE_STEUER = null;
    public String LEXWARE_KOSTENSTELLE_1 = "";
    public String LEXWARE_KOSTENSTELLE_2 = "";
    public Integer LEXWARE_ZUSATZANGABEN = null;

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

    private void parseErrorMessage(Exception ex, String name, Object value) {
        logger.error("Exception:", ex);
        JOptionPane.showMessageDialog(null, "Fehler in der Konfigurationsdatei config.properties bei '"+name+"'.\n"+
            "Es wird der Standardwert "+value+" benutzt.", "Fehler", JOptionPane.ERROR_MESSAGE);
    }

    private void loadConfigFile() {
        // load config file:
        String filename = "config.properties";
        Properties props = new Properties();
        try {
            InputStream fis = new FileInputStream(filename);
            props.load(fis);
        } catch (Exception ex) {
            logger.error("Exception:", ex);
            JOptionPane.showMessageDialog(null, "Fehler in der Konfigurationsdatei config.properties.\n"+
                "Es werden die Standardwerte benutzt.", "Fehler",
                JOptionPane.ERROR_MESSAGE);
        }
        try { this.currencySymbol = props.getProperty("currencySymbol"); } catch (Exception ex) {
            parseErrorMessage(ex, "currencySymbol", this.currencySymbol);
        }
        try { this.operationMode = props.getProperty("operationMode"); } catch (Exception ex) {
            parseErrorMessage(ex, "operationMode", this.operationMode);
        }
        try { this.mysqlHost = props.getProperty("mysqlHost"); } catch (Exception ex) {
            parseErrorMessage(ex, "mysqlHost", this.mysqlHost);
        }
        try { this.mysqlPath = props.getProperty("mysqlPath"); } catch (Exception ex) {
            parseErrorMessage(ex, "mysqlPath", this.mysqlPath);
        }
        try { this.sofficePath = props.getProperty("sofficePath"); } catch (Exception ex) {
            parseErrorMessage(ex, "sofficePath", this.sofficePath);
        }
        try { this.printerName = props.getProperty("printerName"); } catch (Exception ex) {
            parseErrorMessage(ex, "printerName", this.printerName);
        }
        try { this.printerDeviceFile = props.getProperty("printerDeviceFile"); } catch (Exception ex) {
            parseErrorMessage(ex, "printerDeviceFile", this.printerDeviceFile);
        }
        try { this.displayManufacturer = props.getProperty("displayManufacturer"); } catch (Exception ex) {
            parseErrorMessage(ex, "displayManufacturer", this.displayManufacturer);
        }
        try { this.displayModel = props.getProperty("displayModel"); } catch (Exception ex) {
            parseErrorMessage(ex, "displayModel", this.displayModel);
        }
        try { this.displayWidth = Integer.parseInt(props.getProperty("displayWidth")); } catch (Exception ex) {
            parseErrorMessage(ex, "displayWidth", this.displayWidth);
        }
        try { this.displayShowWelcomeInterval = Integer.parseInt(props.getProperty("displayShowWelcomeInterval")); } catch (Exception ex) {
            parseErrorMessage(ex, "displayShowWelcomeInterval", this.displayShowWelcomeInterval);
        }
        try { this.displayBlankInterval = Integer.parseInt(props.getProperty("displayBlankInterval")); } catch (Exception ex) {
            parseErrorMessage(ex, "displayBlankInterval", this.displayBlankInterval);
        }
        try { this.dateFormatSQL = props.getProperty("dateFormatSQL"); } catch (Exception ex) {
            parseErrorMessage(ex, "dateFormatSQL", this.dateFormatSQL);
        }
        try { this.dateFormatJava = props.getProperty("dateFormatJava"); } catch (Exception ex) {
            parseErrorMessage(ex, "dateFormatJava", this.dateFormatJava);
        }
        try { this.dateFormatDate4j = props.getProperty("dateFormatDate4j"); } catch (Exception ex) {
            parseErrorMessage(ex, "dateFormatDate4j", this.dateFormatDate4j);
        }
        try { this.delimiter = props.getProperty("delimiter"); } catch (Exception ex) { // for CSV export/import
            parseErrorMessage(ex, "delimiter", this.delimiter);
        }
        try { this.rowsPerPage = Integer.parseInt(props.getProperty("rowsPerPage")); } catch (Exception ex) {
            parseErrorMessage(ex, "rowsPerPage", this.rowsPerPage);
        }
        try { this.sollMuenzKassenstand = new BigDecimal(props.getProperty("sollMuenzKassenstand")); } catch (Exception ex) {
            parseErrorMessage(ex, "sollMuenzKassenstand", this.sollMuenzKassenstand);
        }
        try { this.sollScheinKassenstand = new BigDecimal(props.getProperty("sollScheinKassenstand")); } catch (Exception ex) {
            parseErrorMessage(ex, "sollScheinKassenstand", this.sollScheinKassenstand);
        }
        try { this.exportDirAbrechnungTag = props.getProperty("exportDirAbrechnungTag"); } catch (Exception ex) {
            parseErrorMessage(ex, "exportDirAbrechnungTag", this.exportDirAbrechnungTag);
        }
        try { this.exportDirAbrechnungMonat = props.getProperty("exportDirAbrechnungMonat"); } catch (Exception ex) {
            parseErrorMessage(ex, "exportDirAbrechnungMonat", this.exportDirAbrechnungMonat);
        }
        try { this.exportDirAbrechnungJahr = props.getProperty("exportDirAbrechnungJahr"); } catch (Exception ex) {
            parseErrorMessage(ex, "exportDirAbrechnungJahr", this.exportDirAbrechnungJahr);
        }
        try { this.exportDirBestellung = props.getProperty("exportDirBestellung"); } catch (Exception ex) {
            parseErrorMessage(ex, "exportDirBestellung", this.exportDirBestellung);
        }
        
        // TSE and DSFinV-K setup:
        try { this.finDatDir = props.getProperty("finDatDir"); } catch (Exception ex) {
            parseErrorMessage(ex, "finDatDir", this.finDatDir);
        }
        try { this.Z_KASSE_ID = props.getProperty("Z_KASSE_ID"); } catch (Exception ex) {
            parseErrorMessage(ex, "Z_KASSE_ID", this.Z_KASSE_ID);
        }
        try { this.TAXONOMIE_VERSION = props.getProperty("TAXONOMIE_VERSION"); } catch (Exception ex) {
            parseErrorMessage(ex, "TAXONOMIE_VERSION", this.TAXONOMIE_VERSION);
        }
        try { this.NAME = props.getProperty("NAME"); } catch (Exception ex) {
            parseErrorMessage(ex, "NAME", this.NAME);
        }
        try { this.STRASSE = props.getProperty("STRASSE"); } catch (Exception ex) {
            parseErrorMessage(ex, "STRASSE", this.STRASSE);
        }
        try { this.PLZ = props.getProperty("PLZ"); } catch (Exception ex) {
            parseErrorMessage(ex, "PLZ", this.PLZ);
        }
        try { this.ORT = props.getProperty("ORT"); } catch (Exception ex) {
            parseErrorMessage(ex, "ORT", this.ORT);
        }
        try { this.LAND = props.getProperty("LAND"); } catch (Exception ex) {
            parseErrorMessage(ex, "LAND", this.LAND);
        }
        try { this.STNR = props.getProperty("STNR"); } catch (Exception ex) {
            parseErrorMessage(ex, "STNR", this.STNR);
        }
        try { this.USTID = props.getProperty("USTID"); } catch (Exception ex) {
            parseErrorMessage(ex, "USTID", this.USTID);
        }
        try { this.NAME_FOR_RECEIPT = props.getProperty("NAME_FOR_RECEIPT"); } catch (Exception ex) {
            parseErrorMessage(ex, "NAME_FOR_RECEIPT", this.NAME_FOR_RECEIPT);
        }
        try { this.PHONE = props.getProperty("PHONE"); } catch (Exception ex) {
            parseErrorMessage(ex, "PHONE", this.PHONE);
        }
        try { this.URL = props.getProperty("URL"); } catch (Exception ex) {
            parseErrorMessage(ex, "URL", this.URL);
        }
        try { this.LOC_NAME = props.getProperty("LOC_NAME"); } catch (Exception ex) {
            parseErrorMessage(ex, "LOC_NAME", this.LOC_NAME);
        }
        try { this.LOC_STRASSE = props.getProperty("LOC_STRASSE"); } catch (Exception ex) {
            parseErrorMessage(ex, "LOC_STRASSE", this.LOC_STRASSE);
        }
        try { this.LOC_PLZ = props.getProperty("LOC_PLZ"); } catch (Exception ex) {
            parseErrorMessage(ex, "LOC_PLZ", this.LOC_PLZ);
        }
        try { this.LOC_ORT = props.getProperty("LOC_ORT"); } catch (Exception ex) {
            parseErrorMessage(ex, "LOC_ORT", this.LOC_ORT);
        }
        try { this.LOC_LAND = props.getProperty("LOC_LAND"); } catch (Exception ex) {
            parseErrorMessage(ex, "LOC_LAND", this.LOC_LAND);
        }
        try { this.LOC_USTID = props.getProperty("LOC_USTID"); } catch (Exception ex) {
            parseErrorMessage(ex, "LOC_USTID", this.LOC_USTID);
        }
        try { this.KASSE_BRAND = props.getProperty("KASSE_BRAND"); } catch (Exception ex) {
            parseErrorMessage(ex, "KASSE_BRAND", this.KASSE_BRAND);
        }
        try { this.KASSE_MODELL = props.getProperty("KASSE_MODELL"); } catch (Exception ex) {
            parseErrorMessage(ex, "KASSE_MODELL", this.KASSE_MODELL);
        }
        try { this.KASSE_SERIENNR = props.getProperty("KASSE_SERIENNR"); } catch (Exception ex) {
            parseErrorMessage(ex, "KASSE_SERIENNR", this.KASSE_SERIENNR);
        }
        try { this.KASSE_SW_BRAND = props.getProperty("KASSE_SW_BRAND"); } catch (Exception ex) {
            parseErrorMessage(ex, "KASSE_SW_BRAND", this.KASSE_SW_BRAND);
        }
        try { this.KASSE_SW_VERSION = props.getProperty("KASSE_SW_VERSION"); } catch (Exception ex) {
            parseErrorMessage(ex, "KASSE_SW_VERSION", this.KASSE_SW_VERSION);
        }
        try { this.KASSE_BASISWAEH_CODE = props.getProperty("KASSE_BASISWAEH_CODE"); } catch (Exception ex) {
            parseErrorMessage(ex, "KASSE_BASISWAEH_CODE", this.KASSE_BASISWAEH_CODE);
        }
        try { this.TERMINAL_ID = Integer.parseInt(props.getProperty("TERMINAL_ID")); } catch (Exception ex) {
            parseErrorMessage(ex, "TERMINAL_ID", this.TERMINAL_ID);
        }
        try { this.TERMINAL_BRAND = props.getProperty("TERMINAL_BRAND"); } catch (Exception ex) {
            parseErrorMessage(ex, "TERMINAL_BRAND", this.TERMINAL_BRAND);
        }
        try { this.TERMINAL_MODELL = props.getProperty("TERMINAL_MODELL"); } catch (Exception ex) {
            parseErrorMessage(ex, "TERMINAL_MODELL", this.TERMINAL_MODELL);
        }
        try { this.TERMINAL_SERIENNR = props.getProperty("TERMINAL_SERIENNR"); } catch (Exception ex) {
            parseErrorMessage(ex, "TERMINAL_SERIENNR", this.TERMINAL_SERIENNR);
        }
        try { this.TERMINAL_SW_BRAND = props.getProperty("TERMINAL_SW_BRAND"); } catch (Exception ex) {
            parseErrorMessage(ex, "TERMINAL_SW_BRAND", this.TERMINAL_SW_BRAND);
        }
        try { this.TERMINAL_SW_VERSION = props.getProperty("TERMINAL_SW_VERSION"); } catch (Exception ex) {
            parseErrorMessage(ex, "TERMINAL_SW_VERSION", this.TERMINAL_SW_VERSION);
        }
        try { this.TSE_ID = Integer.parseInt(props.getProperty("TSE_ID")); } catch (Exception ex) {
            parseErrorMessage(ex, "TSE_ID", this.TSE_ID);
        }
        try { this.TSE_PD_ENCODING = props.getProperty("TSE_PD_ENCODING"); } catch (Exception ex) {
            parseErrorMessage(ex, "TSE_PD_ENCODING", this.TSE_PD_ENCODING);
        }

        // Lexware setup:
        try { this.LEXWARE_BELEGDATUM_FORMAT = props.getProperty("LEXWARE_BELEGDATUM_FORMAT"); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_BELEGDATUM_FORMAT", this.LEXWARE_BELEGDATUM_FORMAT);
        }
        try { this.LEXWARE_BELEGNUMMERNKREIS = props.getProperty("LEXWARE_BELEGNUMMERNKREIS"); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_BELEGNUMMERNKREIS", this.LEXWARE_BELEGNUMMERNKREIS);
        }
        try { this.LEXWARE_BUCHUNGSTEXT_ERLOESE = props.getProperty("LEXWARE_BUCHUNGSTEXT_ERLOESE"); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_BUCHUNGSTEXT_ERLOESE", this.LEXWARE_BUCHUNGSTEXT_ERLOESE);
        }
        try { this.LEXWARE_BUCHUNGSTEXT_ERLOESE_OHNE_STEUER = props.getProperty("LEXWARE_BUCHUNGSTEXT_ERLOESE_OHNE_STEUER"); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_BUCHUNGSTEXT_ERLOESE_OHNE_STEUER", this.LEXWARE_BUCHUNGSTEXT_ERLOESE_OHNE_STEUER);
        }
        try { this.LEXWARE_BUCHUNGSTEXT_GELDTRANSIT_KARTE = props.getProperty("LEXWARE_BUCHUNGSTEXT_GELDTRANSIT_KARTE"); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_BUCHUNGSTEXT_GELDTRANSIT_KARTE", this.LEXWARE_BUCHUNGSTEXT_GELDTRANSIT_KARTE);
        }
        try { this.LEXWARE_BUCHUNGSTEXT_GELDTRANSIT_KASSE = props.getProperty("LEXWARE_BUCHUNGSTEXT_GELDTRANSIT_KASSE"); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_BUCHUNGSTEXT_GELDTRANSIT_KASSE", this.LEXWARE_BUCHUNGSTEXT_GELDTRANSIT_KASSE);
        }
        try { this.LEXWARE_BUCHUNGSTEXT_KASSENDIFFERENZ = props.getProperty("LEXWARE_BUCHUNGSTEXT_KASSENDIFFERENZ"); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_BUCHUNGSTEXT_KASSENDIFFERENZ", this.LEXWARE_BUCHUNGSTEXT_KASSENDIFFERENZ);
        }
        try { this.LEXWARE_SOLL_KONTO_ERLOESE = Integer.parseInt(props.getProperty("LEXWARE_SOLL_KONTO_ERLOESE")); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_SOLL_KONTO_ERLOESE", this.LEXWARE_SOLL_KONTO_ERLOESE);
        }
        try { this.LEXWARE_SOLL_KONTO_GELDTRANSIT_KARTE = Integer.parseInt(props.getProperty("LEXWARE_SOLL_KONTO_GELDTRANSIT_KARTE")); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_SOLL_KONTO_GELDTRANSIT_KARTE", this.LEXWARE_SOLL_KONTO_GELDTRANSIT_KARTE);
        }
        try { this.LEXWARE_SOLL_KONTO_GELDTRANSIT_KASSE = Integer.parseInt(props.getProperty("LEXWARE_SOLL_KONTO_GELDTRANSIT_KASSE")); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_SOLL_KONTO_GELDTRANSIT_KASSE", this.LEXWARE_SOLL_KONTO_GELDTRANSIT_KASSE);
        }
        try { this.LEXWARE_SOLL_KONTO_KASSENDIFFERENZ = Integer.parseInt(props.getProperty("LEXWARE_SOLL_KONTO_KASSENDIFFERENZ")); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_SOLL_KONTO_KASSENDIFFERENZ", this.LEXWARE_SOLL_KONTO_KASSENDIFFERENZ);
        }
        try { this.LEXWARE_HABEN_KONTO_ERLOESE_OHNE_STEUER = Integer.parseInt(props.getProperty("LEXWARE_HABEN_KONTO_ERLOESE_OHNE_STEUER")); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_HABEN_KONTO_ERLOESE_OHNE_STEUER", this.LEXWARE_HABEN_KONTO_ERLOESE_OHNE_STEUER);
        }
        try { this.LEXWARE_HABEN_KONTO_ERLOESE_REDUZIERTE_STEUER = Integer.parseInt(props.getProperty("LEXWARE_HABEN_KONTO_ERLOESE_REDUZIERTE_STEUER")); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_HABEN_KONTO_ERLOESE_REDUZIERTE_STEUER", this.LEXWARE_HABEN_KONTO_ERLOESE_REDUZIERTE_STEUER);
        }
        try { this.LEXWARE_HABEN_KONTO_ERLOESE_NORMALE_STEUER = Integer.parseInt(props.getProperty("LEXWARE_HABEN_KONTO_ERLOESE_NORMALE_STEUER")); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_HABEN_KONTO_ERLOESE_NORMALE_STEUER", this.LEXWARE_HABEN_KONTO_ERLOESE_NORMALE_STEUER);
        }
        try { this.LEXWARE_HABEN_KONTO_GELDTRANSIT_KARTE = Integer.parseInt(props.getProperty("LEXWARE_HABEN_KONTO_GELDTRANSIT_KARTE")); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_HABEN_KONTO_GELDTRANSIT_KARTE", this.LEXWARE_HABEN_KONTO_GELDTRANSIT_KARTE);
        }
        try { this.LEXWARE_HABEN_KONTO_GELDTRANSIT_KASSE = Integer.parseInt(props.getProperty("LEXWARE_HABEN_KONTO_GELDTRANSIT_KASSE")); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_HABEN_KONTO_GELDTRANSIT_KASSE", this.LEXWARE_HABEN_KONTO_GELDTRANSIT_KASSE);
        }
        try { this.LEXWARE_HABEN_KONTO_KASSENDIFFERENZ = Integer.parseInt(props.getProperty("LEXWARE_HABEN_KONTO_KASSENDIFFERENZ")); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_HABEN_KONTO_KASSENDIFFERENZ", this.LEXWARE_HABEN_KONTO_KASSENDIFFERENZ);
        }
        try { this.LEXWARE_STEUERSCHLUESSEL_OHNE_STEUER = props.getProperty("LEXWARE_STEUERSCHLUESSEL_OHNE_STEUER").equals("") ? null : Integer.parseInt(props.getProperty("LEXWARE_STEUERSCHLUESSEL_OHNE_STEUER")); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_STEUERSCHLUESSEL_OHNE_STEUER", this.LEXWARE_STEUERSCHLUESSEL_OHNE_STEUER);
        }
        try { this.LEXWARE_STEUERSCHLUESSEL_REDUZIERTE_STEUER = props.getProperty("LEXWARE_STEUERSCHLUESSEL_REDUZIERTE_STEUER").equals("") ? null : Integer.parseInt(props.getProperty("LEXWARE_STEUERSCHLUESSEL_REDUZIERTE_STEUER")); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_STEUERSCHLUESSEL_REDUZIERTE_STEUER", this.LEXWARE_STEUERSCHLUESSEL_REDUZIERTE_STEUER);
        }
        try { this.LEXWARE_STEUERSCHLUESSEL_NORMALE_STEUER = props.getProperty("LEXWARE_STEUERSCHLUESSEL_NORMALE_STEUER").equals("") ? null : Integer.parseInt(props.getProperty("LEXWARE_STEUERSCHLUESSEL_NORMALE_STEUER")); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_STEUERSCHLUESSEL_NORMALE_STEUER", this.LEXWARE_STEUERSCHLUESSEL_NORMALE_STEUER);
        }
        try { this.LEXWARE_KOSTENSTELLE_1 = props.getProperty("LEXWARE_KOSTENSTELLE_1"); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_KOSTENSTELLE_1", this.LEXWARE_KOSTENSTELLE_1);
        }
        try { this.LEXWARE_KOSTENSTELLE_2 = props.getProperty("LEXWARE_KOSTENSTELLE_2"); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_KOSTENSTELLE_2", this.LEXWARE_KOSTENSTELLE_2);
        }
        try { this.LEXWARE_ZUSATZANGABEN = props.getProperty("LEXWARE_ZUSATZANGABEN").equals("") ? null : Integer.parseInt(props.getProperty("LEXWARE_ZUSATZANGABEN")); } catch (Exception ex) {
            parseErrorMessage(ex, "LEXWARE_ZUSATZANGABEN", this.LEXWARE_ZUSATZANGABEN);
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
