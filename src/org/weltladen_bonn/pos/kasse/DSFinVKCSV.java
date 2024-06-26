package org.weltladen_bonn.pos.kasse;

/** 
 * Class for generation of the 20 DSFinV-K CSV files:
 *   * Stammdatenmodul: (one row per Tagesabschluss)
 *       * cashpointclosing.csv
 *       * ...
 *       * tse.csv
 *   * Kassenabschlussmodul: (one row per Tagesabschluss)
 *       * ...
 *   * Einzelaufzeichnungsmodul: (one row per Verkauf/transaction)
 *       * ...
 *       * TSE_Transaktionen
 */

import org.weltladen_bonn.pos.BaseClass;
import org.weltladen_bonn.pos.WindowContent;

// Basic Java stuff
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.nio.file.Path;
import java.nio.file.Files;

// XML parsing
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

// CSV file writing
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.math.RoundingMode;
import java.nio.file.StandardOpenOption;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.text.CollationElementIterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import java.awt.event.ActionEvent;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DSFinVKCSV extends WindowContent {
    private static final long serialVersionUID = 1L;

    private static final Logger logger = LogManager.getLogger(DSFinVKCSV.class);

    private BaseClass bc;
    
    private String exportDir;

    // the CSV file settings, to be parsed from `index.xml`:
    private HashMap<String, Character> decimalSymbols = new HashMap<String, Character>();
    private HashMap<String, Character> digitGroupingSymbols = new HashMap<String, Character>();
    private HashMap<String, String> columnDelimiters = new HashMap<String, String>();
    private HashMap<String, String> recordDelimiters = new HashMap<String, String>();
    private HashMap<String, String> textEncapsulators = new HashMap<String, String>();

    private enum DSFinVKColumnType {
        ALPHANUMERIC,
        NUMERIC,
        UNKNOWN,
    };

    private class DSFinVKColumn {
        public DSFinVKColumnType type = DSFinVKColumnType.UNKNOWN;
        public Integer maxLength = null;
        public Integer accuracy = null;
    }

    private HashMap<String, LinkedHashMap<String, DSFinVKColumn>> csvFileColumns = new HashMap<String, LinkedHashMap<String, DSFinVKColumn>>();

    private SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SimpleDateFormat zErstellungDateFormat = new SimpleDateFormat(WeltladenTSE.dateFormatZErstellung);

    /**
     *    The constructor.
     *
     */
    public DSFinVKCSV(MariaDbPoolDataSource pool, MainWindow mw) {
        super(pool, mw);
        this.bc = mw.bc;
        exportDir = setupFinDatDir()[0]; // create financial data dir if it does not exist
        copySchemaFiles();
        parseIndexXML();
    }

    private void copySchemaFiles() {
        // copy the files `index.xml` and `gdpdu-01-09-2004.dtd` to the export dir if they do not already exist there
        Path path = Path.of(exportDir + bc.fileSep + "index.xml");
        if (!Files.exists(path)) {
            try {
                Files.copy(
                    Path.of("dsfinv-k/index.xml"),
                    path
                );
            } catch (IOException ex) {
                logger.error("Exception: {}", ex);
            }
        }
        path = Path.of(exportDir + bc.fileSep + "gdpdu-01-09-2004.dtd");
        if (!Files.exists(path)) {
            try {
                Files.copy(
                    Path.of("dsfinv-k/gdpdu-01-09-2004.dtd"),
                    path
                );
            } catch (IOException ex) {
                logger.error("Exception: {}", ex);
            }
        }
    }

    private void parseIndexXML() {
        String filename = "dsfinv-k/index.xml";
        
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // https://stackoverflow.com/questions/8699620/how-to-validate-xml-with-dtd-using-java
            // Enable using the DTD file provided by DSFinV-K (does not work)
            dbf.setValidating(true);

            // optional, but recommended
            // process XML securely, avoid attacks like XML External Entities (XXE)
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            
            // parse XML file
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(new ErrorHandler() { // for DTD
                @Override
                public void error(SAXParseException ex) throws SAXException {
                    logger.error(ex);
                }
                @Override
                public void fatalError(SAXParseException ex) throws SAXException {
                    logger.error(ex);
                }
                @Override
                public void warning(SAXParseException ex) throws SAXException {
                    logger.error(ex);
                }
            });
            try {
                doc = db.parse(filename);
            } catch (SAXException ex) {
                logger.error("Falling back to not using DTD!!!");
                dbf = DocumentBuilderFactory.newInstance();
                db = dbf.newDocumentBuilder();
                try {
                    doc = db.parse(filename);
                } catch (SAXException | IOException ex2) {
                    logger.error("Without DTD still not working!!!");
                    logger.error(ex2);
                }
            } catch (IOException ex) {
                logger.error(ex);
            }
        } catch (ParserConfigurationException ex) {
            logger.error(ex);
        }
        
        // optional, but recommended
        // http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
        doc.getDocumentElement().normalize();

        NodeList tables = doc.getElementsByTagName("Table");
        for (int i = 0; i < tables.getLength(); i++) {
            // Node tableNode = tables.item(i);
            // if (tableNode.getNodeType() == Node.ELEMENT_NODE) { // this check should be unnecessary
            // }
            Element table = (Element) tables.item(i);
            String tableFileName = table.getElementsByTagName("URL").item(0).getTextContent();
            String tableName = table.getElementsByTagName("Name").item(0).getTextContent();
            Character decimalSymbol = table.getElementsByTagName("DecimalSymbol").item(0).getTextContent().toCharArray()[0];
            Character digitGroupingSymbol = table.getElementsByTagName("DigitGroupingSymbol").item(0).getTextContent().toCharArray()[0];
            
            Element variable = (Element) table.getElementsByTagName("VariableLength").item(0); // there should be only one such node
            String columnDelimiter = variable.getElementsByTagName("ColumnDelimiter").item(0).getTextContent();
            String recordDelimiter = variable.getElementsByTagName("RecordDelimiter").item(0).getTextContent();
            String textEncapsulator = variable.getElementsByTagName("TextEncapsulator").item(0).getTextContent();
            
            decimalSymbols.put(tableFileName, decimalSymbol);
            digitGroupingSymbols.put(tableFileName, digitGroupingSymbol);
            columnDelimiters.put(tableFileName, columnDelimiter);
            recordDelimiters.put(tableFileName, recordDelimiter);
            textEncapsulators.put(tableFileName, textEncapsulator);
            
            NodeList columns = variable.getElementsByTagName("VariableColumn");
            // LinkedHashMap preverves insertion order, in this case column order defined in the index.xml file
            LinkedHashMap<String, DSFinVKColumn> colMap = new LinkedHashMap<String, DSFinVKColumn>();
            for (int j = 0; j < columns.getLength(); j++) {
                Element column = (Element) columns.item(j);
                String colName = column.getElementsByTagName("Name").item(0).getTextContent();
                DSFinVKColumn col = new DSFinVKColumn();
                if (column.getElementsByTagName("AlphaNumeric").getLength() > 0) {
                    col.type = DSFinVKColumnType.ALPHANUMERIC;
                    if (column.getElementsByTagName("MaxLength").getLength() > 0) {
                        col.maxLength = Integer.parseInt(
                            column.getElementsByTagName("MaxLength").item(0).getTextContent()
                        );
                    }
                } else if (column.getElementsByTagName("Numeric").getLength() > 0) {
                    col.type = DSFinVKColumnType.NUMERIC;
                    if (column.getElementsByTagName("Accuracy").getLength() > 0) {
                        col.accuracy = Integer.parseInt(
                            column.getElementsByTagName("Accuracy").item(0).getTextContent()
                        );
                    }
                }
                colMap.put(colName, col);
            }
            csvFileColumns.put(tableFileName, colMap);
        }
        // for (String fn : csvFileColumns.keySet()) {
        //     logger.debug("{}:", fn);
        //     for (String cn : csvFileColumns.get(fn).keySet()) {
        //         logger.debug("    {}: type = {}, maxLength = {}, accuracy = {}", cn, csvFileColumns.get(fn).get(cn).type,
        //             csvFileColumns.get(fn).get(cn).maxLength, csvFileColumns.get(fn).get(cn).accuracy);
        //     }
        // }
    }

    private String zErstellungDate(String dateStr) {
        String date = "";
        try {
            date = zErstellungDateFormat.format(sqlDateFormat.parse(dateStr));
        } catch (ParseException ex) {
            logger.error(ex);
        }
        return date;
    }

    public HashMap<String, String> get_ZKasseID_ZErstellung_ZNr(int abrechnung_tag_id) {
        HashMap<String, String> zvalues = new HashMap<String, String>();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT "+
                "  at.z_kasse_id, at.zeitpunkt_real, at.id "+
                "FROM abrechnung_tag AS at "+
                "WHERE id = ?");
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                zvalues.put("Z_KASSE_ID", rs.getString(1));
                zvalues.put("Z_ERSTELLUNG", zErstellungDate(rs.getString(2)));
                zvalues.put("Z_NR", rs.getString(3));
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return zvalues;
    }

    /**
     * Writing of the CSV file
     *
    */

    private void writeToCSV(String filename, HashMap<String, String> fields) {
        String csvFilename = exportDir + bc.fileSep + filename;

        String colDel = columnDelimiters.get(filename);
        String rowDel = recordDelimiters.get(filename);
        Character decSep = decimalSymbols.get(filename);
        Character grSep = digitGroupingSymbols.get(filename);
        String textEnc = textEncapsulators.get(filename);

        LinkedHashMap<String, DSFinVKColumn> colDefs = csvFileColumns.get(filename);

        String csvStr = "";
        if (fields.size() > 0) {
            for (String colName : colDefs.keySet()) {
                String col = fields.get(colName);
                if (col != null) {
                    // if necessary, format the column string according to index.xml specification
                    DSFinVKColumn colSpec = colDefs.get(colName);
                    if (colSpec != null && colSpec.type == DSFinVKColumnType.ALPHANUMERIC) {
                        // truncate string if too long
                        if (colSpec.maxLength != null && col.length() > colSpec.maxLength) {
                            col = col.substring(0, colSpec.maxLength);
                        }
                        // escape any occurrences of the text encapsulator with double occurrence of the text encapsulator
                        // (this is meant for " as encapsulator, which is currently the only encapsulator used)
                        col = col.replaceAll(textEnc, textEnc+textEnc);
                        // now encapsulate the text with the text encapsulator
                        col = textEnc + col + textEnc;
                    } else if (colSpec != null && colSpec.type == DSFinVKColumnType.NUMERIC) {
                        // if you also want grouping separators in integer numbers:
                        // DecimalFormatSymbols mySymbols = new DecimalFormatSymbols(bc.myLocale);
                        // mySymbols.setDecimalSeparator(decSep);
                        // mySymbols.setGroupingSeparator(grSep);
                        // DecimalFormat myFormatter = new DecimalFormat("###,###.###", mySymbols);
                        // myFormatter.setRoundingMode(RoundingMode.HALF_UP);
                        // if (colSpec.accuracy != null && colSpec.accuracy > 0) {
                        //     myFormatter.setMinimumFractionDigits(colSpec.accuracy);
                        //     myFormatter.setMaximumFractionDigits(colSpec.accuracy);
                        //     Float colFloat = Float.parseFloat(col);
                        //     col = myFormatter.format(colFloat);
                        // } else {
                        //     myFormatter.setMinimumFractionDigits(0);
                        //     myFormatter.setMaximumFractionDigits(0);
                        //     Integer colInt = Integer.parseInt(col);
                        //     col = myFormatter.format(colInt);
                        // }
                        // if not:
                        if (colSpec.accuracy != null && colSpec.accuracy > 0) {
                            DecimalFormatSymbols mySymbols = new DecimalFormatSymbols(bc.myLocale);
                            mySymbols.setDecimalSeparator(decSep);
                            mySymbols.setGroupingSeparator(grSep);
                            DecimalFormat myFormatter = new DecimalFormat("###,###.###", mySymbols);
                            myFormatter.setRoundingMode(RoundingMode.HALF_UP);
                            myFormatter.setMinimumFractionDigits(colSpec.accuracy);
                            myFormatter.setMaximumFractionDigits(colSpec.accuracy);
                            Float colFloat = Float.parseFloat(col);
                            col = myFormatter.format(colFloat);
                        }
                    }
                } else {
                    col = "";
                }
                csvStr += col + colDel;
            }
            // remove the very last column separator:
            csvStr = csvStr.substring(0, csvStr.length() - colDel.length());
            csvStr += rowDel;
        }

        try {
            if (!Files.exists(Path.of(csvFilename))) {
                // create the file with a header row (data start at second row, as specified in index.xml: "<From>2</From>")
                String headerStr = "";
                for (String colName : colDefs.keySet()) {
                    headerStr += colName + colDel;
                }
                // remove the very last column separator:
                headerStr = headerStr.substring(0, headerStr.length() - colDel.length());
                headerStr += rowDel;
                Files.writeString(Path.of(csvFilename), headerStr,
                    StandardOpenOption.CREATE, // create file if not exists
                    StandardOpenOption.APPEND); // append to file if exists
            }
            if (csvStr.length() > 0) {
                Files.writeString(Path.of(csvFilename), csvStr,
                    StandardOpenOption.CREATE, // create file if not exists
                    StandardOpenOption.APPEND); // append to file if exists
            }
        } catch (IOException ex) {
            logger.error("Error writing to file {}", csvFilename);
            logger.error("Exception:", ex);
        }
    }

    /**
     *  STAMMDATENMODUL
     *
     */

    public void writeToCSV_Stamm_Abschluss(int abrechnung_tag_id) {
        String filename = "cashpointclosing.csv";
        HashMap<String, String> fields = new HashMap<String, String>();
        // Get data mostly from the table `abrechnung_tag`
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT "+
                "  at.z_kasse_id, at.zeitpunkt_real, at.id, "+
                "  DATE(at.zeitpunkt), at.rechnungs_nr_von, at.rechnungs_nr_bis, "+
                "  (SELECT SUM(mwst_netto + mwst_betrag) FROM abrechnung_tag_mwst WHERE id = at.id), "+
                "  (SELECT SUM(bar_brutto) FROM abrechnung_tag_mwst WHERE id = at.id) "+
                "FROM abrechnung_tag AS at "+
                "WHERE id = ?");
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                fields.put("Z_KASSE_ID", rs.getString(1));
                fields.put("Z_ERSTELLUNG", zErstellungDate(rs.getString(2)));
                fields.put("Z_NR", rs.getString(3));
                fields.put("Z_BUCHUNGSTAG", rs.getString(4));
                fields.put("TAXONOMIE_VERSION", bc.TAXONOMIE_VERSION);
                fields.put("Z_START_ID", rs.getString(5));
                fields.put("Z_ENDE_ID", rs.getString(6));
                fields.put("NAME", bc.NAME);
                fields.put("STRASSE", bc.STRASSE);
                fields.put("PLZ", bc.PLZ);
                fields.put("ORT", bc.ORT);
                fields.put("LAND", bc.LAND);
                fields.put("STNR", bc.STNR);
                fields.put("USTID", bc.USTID);
                fields.put("Z_SE_ZAHLUNGEN", rs.getString(7));
                fields.put("Z_SE_BARZAHLUNGEN", rs.getString(8));
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        writeToCSV(filename, fields);
    }

    public void writeToCSV_Stamm_Orte(int abrechnung_tag_id, HashMap<String, String> zvalues) {
        String filename = "location.csv";
        HashMap<String, String> fields = new HashMap<String, String>();
        // Get data mostly from file config.properties (config.txt)
        fields.put("Z_KASSE_ID", zvalues.get("Z_KASSE_ID"));
        fields.put("Z_ERSTELLUNG", zvalues.get("Z_ERSTELLUNG"));
        fields.put("Z_NR", zvalues.get("Z_NR"));
        fields.put("LOC_NAME", bc.LOC_NAME);
        fields.put("LOC_STRASSE", bc.LOC_STRASSE);
        fields.put("LOC_PLZ", bc.LOC_PLZ);
        fields.put("LOC_ORT", bc.LOC_ORT);
        fields.put("LOC_LAND", bc.LOC_LAND);
        fields.put("LOC_USTID", bc.LOC_USTID);
        writeToCSV(filename, fields);
    }

    public void writeToCSV_Stamm_Kassen(int abrechnung_tag_id, HashMap<String, String> zvalues) {
        String filename = "cashregister.csv";
        HashMap<String, String> fields = new HashMap<String, String>();
        // Get data mostly from file config.properties (config.txt)
        fields.put("Z_KASSE_ID", zvalues.get("Z_KASSE_ID"));
        fields.put("Z_ERSTELLUNG", zvalues.get("Z_ERSTELLUNG"));
        fields.put("Z_NR", zvalues.get("Z_NR"));
        fields.put("KASSE_BRAND", bc.KASSE_BRAND);
        fields.put("KASSE_MODELL", bc.KASSE_MODELL);
        fields.put("KASSE_SERIENNR", bc.KASSE_SERIENNR);
        fields.put("KASSE_SW_BRAND", bc.KASSE_SW_BRAND);
        fields.put("KASSE_SW_VERSION", bc.KASSE_SW_VERSION);
        fields.put("KASSE_BASISWAEH_CODE", bc.KASSE_BASISWAEH_CODE);
        fields.put("KEINE_UST_ZUORDNUNG", "0");
        // Anhang_G_Uebersicht.xlsx: KEINE_UST_ZUORDNUNG: string (1) -> 0,1 oder ""
        // Bei uns (Nicht-Aktivierung des Feldes): immer "0"
        writeToCSV(filename, fields);
    }

    public void writeToCSV_Stamm_Terminals(int abrechnung_tag_id, HashMap<String, String> zvalues) {
        String filename = "slaves.csv";
        HashMap<String, String> fields = new HashMap<String, String>();
        // Get data mostly from file config.properties (config.txt)
        fields.put("Z_KASSE_ID", zvalues.get("Z_KASSE_ID"));
        fields.put("Z_ERSTELLUNG", zvalues.get("Z_ERSTELLUNG"));
        fields.put("Z_NR", zvalues.get("Z_NR"));
        fields.put("TERMINAL_ID", bc.TERMINAL_ID.toString());
        fields.put("TERMINAL_BRAND", bc.TERMINAL_BRAND);
        fields.put("TERMINAL_MODELL", bc.TERMINAL_MODELL);
        fields.put("TERMINAL_SERIENNR", bc.TERMINAL_SERIENNR);
        fields.put("TERMINAL_SW_BRAND", bc.TERMINAL_SW_BRAND);
        fields.put("TERMINAL_SW_VERSION", bc.TERMINAL_SW_VERSION);
        writeToCSV(filename, fields);
    }

    public void writeToCSV_Stamm_Agenturen(int abrechnung_tag_id) {
        String filename = "pa.csv";
        HashMap<String, String> fields = new HashMap<String, String>();
        writeToCSV(filename, fields); // do not write to file, but create file with header if not exists
    }

    public void writeToCSV_Stamm_USt(int abrechnung_tag_id, HashMap<String, String> zvalues) {
        String filename = "vat.csv";
        // Get data mostly from the table `mwst`
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT "+
                "  dsfinvk_ust_schluessel, 100 * mwst_satz, dsfinvk_ust_beschr "+
                "FROM mwst ORDER BY dsfinvk_ust_schluessel");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                HashMap<String, String> fields = new HashMap<String, String>();
                fields.put("Z_KASSE_ID", zvalues.get("Z_KASSE_ID"));
                fields.put("Z_ERSTELLUNG", zvalues.get("Z_ERSTELLUNG"));
                fields.put("Z_NR", zvalues.get("Z_NR"));
                fields.put("UST_SCHLUESSEL", rs.getString(1));
                fields.put("UST_SATZ", rs.getString(2));
                fields.put("UST_BESCHR", rs.getString(3));
                writeToCSV(filename, fields);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }
    
    public void writeToCSV_Stamm_TSE(int abrechnung_tag_id, HashMap<String, String> zvalues) {
        String filename = "tse.csv";
        HashMap<String, String> fields = new HashMap<String, String>();
        // Get data mostly from the table `abrechnung_tag_tse`
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT "+
                "  tse_id, tse_serial, tse_sig_algo, "+
                "  tse_time_format, tse_pd_encoding, tse_public_key, "+
                "  tse_cert_i, tse_cert_ii "+
                "FROM abrechnung_tag_tse "+
                "WHERE id = ?");
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                fields.put("Z_KASSE_ID", zvalues.get("Z_KASSE_ID"));
                fields.put("Z_ERSTELLUNG", zvalues.get("Z_ERSTELLUNG"));
                fields.put("Z_NR", zvalues.get("Z_NR"));
                fields.put("TSE_ID", rs.getString(1));
                fields.put("TSE_SERIAL", rs.getString(2));
                fields.put("TSE_SIG_ALGO", rs.getString(3));
                fields.put("TSE_ZEITFORMAT", rs.getString(4));
                fields.put("TSE_PD_ENCODING", rs.getString(5));
                fields.put("TSE_PUBLIC_KEY", rs.getString(6));
                fields.put("TSE_ZERTIFIKAT_I", rs.getString(7));
                fields.put("TSE_ZERTIFIKAT_II", rs.getString(8));
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        writeToCSV(filename, fields);
    }

    
    /**
     *  KASSENABSCHLUSSMODUL
     *
     */

    public void writeToCSV_Z_GV_Typ(int abrechnung_tag_id, HashMap<String, String> zvalues) {
        String filename = "businesscases.csv";
        // Get data from tables `kassenstand`, `abrechnung_tag_mwst`, ...
        // Gruppiert nach GV_TYP und Umsatzsteuersatz jeweils die Summe der in dieser
        //   Tagesabrechnung enthaltenen Beträge: (siehe Seite 28 der DSFinV-K und Anhang C, ab Seite 47):

        // GV_TYP "Anfangsbestand" = 150 €, also der `kassenstand` zu Beginn eines Buchungstages
        writeToCSV_Z_GV_Typ_Anfangsbestand(abrechnung_tag_id, zvalues, filename);
        // GV_TYP "Umsatz"
        writeToCSV_Z_GV_Typ_Umsatz(abrechnung_tag_id, zvalues, filename);
        // GV_TYP "Pfand"
        writeToCSV_Z_GV_Typ_Pfand(abrechnung_tag_id, zvalues, filename);
        // GV_TYP "PfandRueckzahlung"
        writeToCSV_Z_GV_Typ_PfandRueckzahlung(abrechnung_tag_id, zvalues, filename);
        // GV_TYP "Rabatt"
        writeToCSV_Z_GV_Typ_Rabatt(abrechnung_tag_id, zvalues, filename);
        // GV_TYP "Aufschlag" (nur für den hypothetischen Fall eines negativen Rabatts)
        writeToCSV_Z_GV_Typ_Aufschlag(abrechnung_tag_id, zvalues, filename);
        // GV_TYP "MehrzweckgutscheinKauf"
        writeToCSV_Z_GV_Typ_MehrzweckgutscheinKauf(abrechnung_tag_id, zvalues, filename);
        // GV_TYP "MehrzweckgutscheinEinloesung"
        writeToCSV_Z_GV_Typ_MehrzweckgutscheinEinloesung(abrechnung_tag_id, zvalues, filename);
        // GV_TYP "Anzahlungseinstellung"
        // GV_TYP "Anzahlungsaufloesung"
        // GV_TYP "Geldtransit" = Entnahme von Geld aus der Kasse bei Tagesabschluss (aus Tabelle `kassenstand` zu entnehmen)
        // GV_TYP "DifferenzSollIst = Kassendifferenz bei Tagesabschluss
    }

    public void writeToCSV_Z_GV_Typ_Anfangsbestand(int abrechnung_tag_id, HashMap<String, String> zvalues, String filename) {
        HashMap<String, String> fields = new HashMap<String, String>();
        // Get data mostly from the table `kassenstand`
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                // SELECT at.z_kasse_id, at.zeitpunkt_real, at.id, (SELECT dsfinvk_ust_schluessel FROM mwst WHERE mwst_satz = 0.00) AS ust_schluessel, (SELECT neuer_kassenstand FROM kassenstand WHERE kassenstand_id > (SELECT kassenstand_id FROM kassenstand WHERE rechnungs_nr = 288) AND rechnungs_nr IS NULL AND manuell = TRUE AND entnahme = FALSE LIMIT 1) AS anfangsbestand FROM abrechnung_tag AS at WHERE id = 289;
                "SELECT "+
                "  (SELECT dsfinvk_ust_schluessel FROM mwst WHERE mwst_satz = 0.00) AS ust_schluessel, "+
                "  IFNULL("+
                "    (SELECT neuer_kassenstand FROM kassenstand "+
                "     WHERE kassenstand_id > "+
                "       (SELECT kassenstand_id FROM kassenstand WHERE rechnungs_nr = ? - 1) AND "+
                "        rechnungs_nr IS NULL AND manuell = TRUE AND entnahme = FALSE "+
                "     LIMIT 1"+ // SELECT * FROM kassenstand WHERE kassenstand_id > (SELECT kassenstand_id FROM kassenstand WHERE rechnungs_nr = 288) AND rechnungs_nr IS NULL AND manuell = TRUE AND entnahme = FALSE LIMIT 1;
                "    ), 150.00) AS anfangsbestand");
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                fields.put("Z_KASSE_ID", zvalues.get("Z_KASSE_ID"));
                fields.put("Z_ERSTELLUNG", zvalues.get("Z_ERSTELLUNG"));
                fields.put("Z_NR", zvalues.get("Z_NR"));
                fields.put("GV_TYP", "Anfangsbestand");
                fields.put("GV_NAME", "Anfangsbestand");
                fields.put("AGENTUR_ID", "0");
                fields.put("UST_SCHLUESSEL", rs.getString(1));
                fields.put("Z_UMS_BRUTTO", rs.getString(2));
                fields.put("Z_UMS_NETTO", rs.getString(2));
                fields.put("Z_UST", "0.00");
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        writeToCSV(filename, fields);
    }

    public void writeToCSV_Z_GV_Typ_Umsatz(int abrechnung_tag_id, HashMap<String, String> zvalues, String filename) {
        // Get data mostly from the table `verkauf_mwst`
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                // SELECT mwst_satz, dsfinvk_ust_schluessel, SUM(mwst_netto + mwst_betrag), SUM(mwst_netto), SUM(mwst_betrag) FROM verkauf_mwst INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) WHERE at.id = 1577 AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel;
                "SELECT mwst_satz, dsfinvk_ust_schluessel, SUM(mwst_netto + mwst_betrag), SUM(mwst_netto), SUM(mwst_betrag) "+
                "FROM verkauf_mwst INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) "+
                "WHERE at.id = ? AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis "+
                "GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel"
                // Alternative using verkauf_details instead of verkauf_mwsts:
                // SELECT mwst_satz, dsfinvk_ust_schluessel, SUM(ges_preis), SUM(ROUND(ges_preis / (1 + mwst_satz), 2)), SUM(ROUND(mwst_satz * ges_preis / (1 + mwst_satz), 2)) FROM verkauf_details INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) WHERE at.id = 1577 AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel;
            );
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                HashMap<String, String> fields = new HashMap<String, String>();
                fields.put("Z_KASSE_ID", zvalues.get("Z_KASSE_ID"));
                fields.put("Z_ERSTELLUNG", zvalues.get("Z_ERSTELLUNG"));
                fields.put("Z_NR", zvalues.get("Z_NR"));
                fields.put("GV_TYP", "Umsatz");
                fields.put("GV_NAME", "Umsatz");
                fields.put("AGENTUR_ID", "0");
                fields.put("UST_SCHLUESSEL", rs.getString(2));
                fields.put("Z_UMS_BRUTTO", rs.getString(3));
                fields.put("Z_UMS_NETTO", rs.getString(4));
                fields.put("Z_UST", rs.getString(5));
                writeToCSV(filename, fields);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    public void writeToCSV_Z_GV_Typ_Pfand(int abrechnung_tag_id, HashMap<String, String> zvalues, String filename) {
        // Get data mostly from the table `verkauf_details`
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                // SELECT mwst_satz, dsfinvk_ust_schluessel, SUM(ges_preis), SUM(ROUND(ges_preis / (1 + mwst_satz), 2)), SUM(ROUND(mwst_satz * ges_preis / (1 + mwst_satz), 2)) FROM verkauf_details INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) INNER JOIN artikel USING (artikel_id) INNER JOIN produktgruppe USING (produktgruppen_id) WHERE at.id = 1506 AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis AND toplevel_id IS NULL AND (sub_id = 3 OR sub_id = 4) AND stueckzahl > 0 GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel;
                "SELECT mwst_satz, dsfinvk_ust_schluessel, "+
                "SUM(ges_preis), SUM(ROUND(ges_preis / (1 + mwst_satz), 2)), SUM(ROUND(mwst_satz * ges_preis / (1 + mwst_satz), 2)) "+
                "FROM verkauf_details INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) "+
                "INNER JOIN artikel USING (artikel_id) INNER JOIN produktgruppe USING (produktgruppen_id) "+ // for info about product group
                "WHERE at.id = ? AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis "+
                "AND toplevel_id IS NULL AND (sub_id = 3 OR sub_id = 4) AND stueckzahl > 0 "+ // select Pfand
                "GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel"
            );
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                HashMap<String, String> fields = new HashMap<String, String>();
                fields.put("Z_KASSE_ID", zvalues.get("Z_KASSE_ID"));
                fields.put("Z_ERSTELLUNG", zvalues.get("Z_ERSTELLUNG"));
                fields.put("Z_NR", zvalues.get("Z_NR"));
                fields.put("GV_TYP", "Pfand");
                fields.put("GV_NAME", "Pfand");
                fields.put("AGENTUR_ID", "0");
                fields.put("UST_SCHLUESSEL", rs.getString(2));
                fields.put("Z_UMS_BRUTTO", rs.getString(3));
                fields.put("Z_UMS_NETTO", rs.getString(4));
                fields.put("Z_UST", rs.getString(5));
                writeToCSV(filename, fields);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    public void writeToCSV_Z_GV_Typ_PfandRueckzahlung(int abrechnung_tag_id, HashMap<String, String> zvalues, String filename) {
        // Get data mostly from the table `verkauf_details`
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                // SELECT mwst_satz, dsfinvk_ust_schluessel, SUM(ges_preis), SUM(ROUND(ges_preis / (1 + mwst_satz), 2)), SUM(ROUND(mwst_satz * ges_preis / (1 + mwst_satz), 2)) FROM verkauf_details INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) INNER JOIN artikel USING (artikel_id) INNER JOIN produktgruppe USING (produktgruppen_id) WHERE at.id = 1506 AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis AND toplevel_id IS NULL AND (sub_id = 3 OR sub_id = 4) AND stueckzahl < 0 GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel;
                "SELECT mwst_satz, dsfinvk_ust_schluessel, "+
                "SUM(ges_preis), SUM(ROUND(ges_preis / (1 + mwst_satz), 2)), SUM(ROUND(mwst_satz * ges_preis / (1 + mwst_satz), 2)) "+
                "FROM verkauf_details INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) "+
                "INNER JOIN artikel USING (artikel_id) INNER JOIN produktgruppe USING (produktgruppen_id) "+ // for info about product group
                "WHERE at.id = ? AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis "+
                "AND toplevel_id IS NULL AND (sub_id = 3 OR sub_id = 4) AND stueckzahl < 0 "+ // select PfandRueckzahlung (Leergut)
                "GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel"
            );
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                HashMap<String, String> fields = new HashMap<String, String>();
                fields.put("Z_KASSE_ID", zvalues.get("Z_KASSE_ID"));
                fields.put("Z_ERSTELLUNG", zvalues.get("Z_ERSTELLUNG"));
                fields.put("Z_NR", zvalues.get("Z_NR"));
                fields.put("GV_TYP", "PfandRueckzahlung");
                fields.put("GV_NAME", "PfandRueckzahlung");
                fields.put("AGENTUR_ID", "0");
                fields.put("UST_SCHLUESSEL", rs.getString(2));
                fields.put("Z_UMS_BRUTTO", rs.getString(3));
                fields.put("Z_UMS_NETTO", rs.getString(4));
                fields.put("Z_UST", rs.getString(5));
                writeToCSV(filename, fields);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    public void writeToCSV_Z_GV_Typ_Rabatt(int abrechnung_tag_id, HashMap<String, String> zvalues, String filename) {
        // Get data mostly from the table `verkauf_details`
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                // SELECT mwst_satz, dsfinvk_ust_schluessel, SUM(ges_preis), SUM(ROUND(ges_preis / (1 + mwst_satz), 2)), SUM(ROUND(mwst_satz * ges_preis / (1 + mwst_satz), 2)) FROM verkauf_details INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) WHERE at.id = 1686 AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis AND (artikel_id = 1 OR artikel_id = 2 OR rabatt_id IS NOT NULL) GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel;
                // SELECT mwst_satz, dsfinvk_ust_schluessel, SUM(ges_preis), SUM(ROUND(ges_preis / (1 + mwst_satz), 2)), SUM(ROUND(mwst_satz * ges_preis / (1 + mwst_satz), 2)) FROM verkauf_details INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) WHERE at.id = 1689 AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis AND (artikel_id = 1 OR artikel_id = 2 OR rabatt_id IS NOT NULL) GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel;
                // SELECT mwst_satz, dsfinvk_ust_schluessel, SUM(ges_preis), SUM(ROUND(ges_preis / (1 + mwst_satz), 2)), SUM(ROUND(mwst_satz * ges_preis / (1 + mwst_satz), 2)) FROM verkauf_details INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) WHERE at.id = 1690 AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis AND (artikel_id = 1 OR artikel_id = 2 OR rabatt_id IS NOT NULL) GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel;
                "SELECT mwst_satz, dsfinvk_ust_schluessel, "+
                "SUM(ges_preis), SUM(ROUND(ges_preis / (1 + mwst_satz), 2)), "+
                "SUM(ROUND(mwst_satz * ges_preis / (1 + mwst_satz), 2)) "+
                "FROM verkauf_details INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) "+
                "WHERE at.id = ? AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis "+
                "AND (artikel_id = 1 OR artikel_id = 2 OR rabatt_id IS NOT NULL) "+ // select Rabatt: either "Manueller Rabatt" (on single item) or "Rabatt auf Rechnung" (on whole order) or via Rabattaktion
                "GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel"
            );
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                HashMap<String, String> fields = new HashMap<String, String>();
                fields.put("Z_KASSE_ID", zvalues.get("Z_KASSE_ID"));
                fields.put("Z_ERSTELLUNG", zvalues.get("Z_ERSTELLUNG"));
                fields.put("Z_NR", zvalues.get("Z_NR"));
                fields.put("GV_TYP", "Rabatt");
                fields.put("GV_NAME", "Rabatt");
                fields.put("AGENTUR_ID", "0");
                fields.put("UST_SCHLUESSEL", rs.getString(2));
                fields.put("Z_UMS_BRUTTO", rs.getString(3));
                fields.put("Z_UMS_NETTO", rs.getString(4));
                fields.put("Z_UST", rs.getString(5));
                writeToCSV(filename, fields);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    public void writeToCSV_Z_GV_Typ_Aufschlag(int abrechnung_tag_id, HashMap<String, String> zvalues, String filename) {
        // Get data mostly from the table `verkauf_details`
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                // SELECT mwst_satz, dsfinvk_ust_schluessel, SUM(ges_preis), SUM(ROUND(ges_preis / (1 + mwst_satz), 2)), SUM(ROUND(mwst_satz * ges_preis / (1 + mwst_satz), 2)) FROM verkauf_details INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) WHERE at.id = 1689 AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis AND artikel_id = 3 GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel;
                "SELECT mwst_satz, dsfinvk_ust_schluessel, "+
                "SUM(ges_preis), SUM(ROUND(ges_preis / (1 + mwst_satz), 2)), "+
                "SUM(ROUND(mwst_satz * ges_preis / (1 + mwst_satz), 2)) "+
                "FROM verkauf_details INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) "+
                "WHERE at.id = ? AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis "+
                "AND artikel_id = 3 "+ // select Aufschlag: special article "Manuelle Preisanpassung"
                "GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel"
            );
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                HashMap<String, String> fields = new HashMap<String, String>();
                fields.put("Z_KASSE_ID", zvalues.get("Z_KASSE_ID"));
                fields.put("Z_ERSTELLUNG", zvalues.get("Z_ERSTELLUNG"));
                fields.put("Z_NR", zvalues.get("Z_NR"));
                fields.put("GV_TYP", "Aufschlag");
                fields.put("GV_NAME", "Aufschlag");
                fields.put("AGENTUR_ID", "0");
                fields.put("UST_SCHLUESSEL", rs.getString(2));
                fields.put("Z_UMS_BRUTTO", rs.getString(3));
                fields.put("Z_UMS_NETTO", rs.getString(4));
                fields.put("Z_UST", rs.getString(5));
                writeToCSV(filename, fields);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    public void writeToCSV_Z_GV_Typ_MehrzweckgutscheinKauf(int abrechnung_tag_id, HashMap<String, String> zvalues, String filename) {
        // Get data mostly from the table `verkauf_details`
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                // SELECT mwst_satz, dsfinvk_ust_schluessel, SUM(ges_preis), SUM(ROUND(ges_preis / (1 + mwst_satz), 2)), SUM(ROUND(mwst_satz * ges_preis / (1 + mwst_satz), 2)) FROM verkauf_details INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) WHERE at.id = 1578 AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis AND artikel_id = 6 GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel;
                "SELECT mwst_satz, dsfinvk_ust_schluessel, "+
                "SUM(ges_preis), SUM(ROUND(ges_preis / (1 + mwst_satz), 2)), "+
                "SUM(ROUND(mwst_satz * ges_preis / (1 + mwst_satz), 2)) "+
                "FROM verkauf_details INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) "+
                "WHERE at.id = ? AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis "+
                "AND artikel_id = 6 "+ // select MehrzweckgutscheinKauf: special article "Gutschein"
                "GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel"
            );
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                HashMap<String, String> fields = new HashMap<String, String>();
                fields.put("Z_KASSE_ID", zvalues.get("Z_KASSE_ID"));
                fields.put("Z_ERSTELLUNG", zvalues.get("Z_ERSTELLUNG"));
                fields.put("Z_NR", zvalues.get("Z_NR"));
                fields.put("GV_TYP", "MehrzweckgutscheinKauf");
                fields.put("GV_NAME", "MehrzweckgutscheinKauf");
                fields.put("AGENTUR_ID", "0");
                fields.put("UST_SCHLUESSEL", rs.getString(2));
                fields.put("Z_UMS_BRUTTO", rs.getString(3));
                fields.put("Z_UMS_NETTO", rs.getString(4));
                fields.put("Z_UST", rs.getString(5));
                writeToCSV(filename, fields);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }
    
    public void writeToCSV_Z_GV_Typ_MehrzweckgutscheinEinloesung(int abrechnung_tag_id, HashMap<String, String> zvalues, String filename) {
        // Get data mostly from the table `verkauf_details`
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                // SELECT mwst_satz, dsfinvk_ust_schluessel, SUM(ges_preis), SUM(ROUND(ges_preis / (1 + mwst_satz), 2)), SUM(ROUND(mwst_satz * ges_preis / (1 + mwst_satz), 2)) FROM verkauf_details INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) WHERE at.id = 1659 AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis AND artikel_id = 7 GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel;
                "SELECT mwst_satz, dsfinvk_ust_schluessel, "+
                "SUM(ges_preis), SUM(ROUND(ges_preis / (1 + mwst_satz), 2)), "+
                "SUM(ROUND(mwst_satz * ges_preis / (1 + mwst_satz), 2)) "+
                "FROM verkauf_details INNER JOIN abrechnung_tag AS at INNER JOIN mwst USING (mwst_satz) "+
                "WHERE at.id = ? AND rechnungs_nr >= at.rechnungs_nr_von AND rechnungs_nr <= at.rechnungs_nr_bis "+
                "AND artikel_id = 7 "+ // select MehrzweckgutscheinEinloesung: special article "Gutscheineinlösung"
                "GROUP BY mwst_satz ORDER BY dsfinvk_ust_schluessel"
            );
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                HashMap<String, String> fields = new HashMap<String, String>();
                fields.put("Z_KASSE_ID", zvalues.get("Z_KASSE_ID"));
                fields.put("Z_ERSTELLUNG", zvalues.get("Z_ERSTELLUNG"));
                fields.put("Z_NR", zvalues.get("Z_NR"));
                fields.put("GV_TYP", "MehrzweckgutscheinEinloesung");
                fields.put("GV_NAME", "MehrzweckgutscheinEinloesung");
                fields.put("AGENTUR_ID", "0");
                fields.put("UST_SCHLUESSEL", rs.getString(2));
                fields.put("Z_UMS_BRUTTO", rs.getString(3));
                fields.put("Z_UMS_NETTO", rs.getString(4));
                fields.put("Z_UST", rs.getString(5));
                writeToCSV(filename, fields);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }
    

    /**
     *  EINZELAUFZEICHNUNGSMODUL
     *
     */

    public void writeToCSV_TSE_Transaktionen(int abrechnung_tag_id) {
        String filename = "transactions_tse.csv";
        // Get data mostly from the table `tse_transaction`
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT "+
                "  at.z_kasse_id, at.zeitpunkt_real, at.id, "+
                "  tt.rechnungs_nr, att.tse_id, tt.transaction_number, "+
                "  tt.transaction_start, tt.transaction_end, tt.process_type, "+
                "  tt.signature_counter, tt.signature_base64, tt.tse_error, tt.process_data "+
                "FROM abrechnung_tag AS at LEFT JOIN abrechnung_tag_tse AS att USING (id), "+
                "  tse_transaction AS tt "+
                "WHERE at.id = ? "+
                "  AND STR_TO_DATE(SUBSTRING(tt.transaction_start, 1, 19), '%Y-%m-%dT%H:%i:%s') >= (SELECT zeitpunkt_real FROM abrechnung_tag WHERE id = at.id - 1) "+
                "  AND STR_TO_DATE(SUBSTRING(tt.transaction_start, 1, 19), '%Y-%m-%dT%H:%i:%s') <= at.zeitpunkt_real");
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                HashMap<String, String> fields = new HashMap<String, String>();
                fields.put("Z_KASSE_ID", rs.getString(1));
                fields.put("Z_ERSTELLUNG", zErstellungDate(rs.getString(2)));
                fields.put("Z_NR", rs.getString(3));
                fields.put("BON_ID", rs.getString(4));
                fields.put("TSE_ID", rs.getString(5));
                fields.put("TSE_TANR", rs.getString(6));
                fields.put("TSE_TA_START", rs.getString(7));
                fields.put("TSE_TA_ENDE", rs.getString(8));
                fields.put("TSE_TA_VORGANGSART", rs.getString(9));
                fields.put("TSE_TA_SIGZ", rs.getString(10));
                fields.put("TSE_TA_SIG", rs.getString(11));
                fields.put("TSE_TA_FEHLER", rs.getString(12));
                fields.put("TSE_VORGANGSDATEN", rs.getString(13));
                writeToCSV(filename, fields);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }


    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e){
    }
}
