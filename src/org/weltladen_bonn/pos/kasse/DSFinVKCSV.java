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
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.io.BufferedWriter;
import java.io.FileWriter;

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
    private HashMap<String, String> decimalSymbols = new HashMap<String, String>();
    private HashMap<String, String> digitGroupingSymbols = new HashMap<String, String>();
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

    private HashMap<String, HashMap<String, DSFinVKColumn>> csvFileColumns = new HashMap<String, HashMap<String, DSFinVKColumn>>();

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

        // XXX only for testing:
        writeToCSV_Stamm_TSE(1510);
    }

    private void copySchemaFiles() {
        // copy the files `index.xml` and `gdpdu-01-09-2004.dtd` to the export dir if they do not already exist there
        Path path = Paths.get(exportDir + bc.fileSep + "index.xml");
        if (!Files.exists(path)) {
            try {
                Files.copy(
                    Paths.get("dsfinv-k/index.xml"),
                    path
                );
            } catch (IOException ex) {
                logger.error("Exception: {}", ex);
            }
        }
        path = Paths.get(exportDir + bc.fileSep + "gdpdu-01-09-2004.dtd");
        if (!Files.exists(path)) {
            try {
                Files.copy(
                    Paths.get("dsfinv-k/gdpdu-01-09-2004.dtd"),
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
            String decimalSymbol = table.getElementsByTagName("DecimalSymbol").item(0).getTextContent();
            String digitGroupingSymbol = table.getElementsByTagName("DigitGroupingSymbol").item(0).getTextContent();
            
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
            HashMap<String, DSFinVKColumn> colMap = new HashMap<String, DSFinVKColumn>();
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

    /**
     *  STAMMDATENMODUL
     *
     */
    
    public void writeToCSV_Stamm_TSE(int abrechnung_tag_id) {
        String filename = "tse.csv";
        HashMap<String, String> fields = new HashMap<String, String>();
        // Add a row to the file for Tagesabrechnung with abrechnung_tag_id
        // Get it mostly from the table `abrechnung_tag_tse`
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT "+
                "  at.z_kasse_id, at.zeitpunkt_real, at.id, "+
                "  att.tse_id, att.tse_serial, att.tse_sig_algo, "+
                "  att.tse_time_format, att.tse_pd_encoding, att.tse_public_key, "+
                "  att.tse_cert_i, att.tse_cert_ii "+
                "FROM abrechnung_tag AS at LEFT JOIN abrechnung_tag_tse AS att "+
                "  USING (id) "+
                "WHERE id = ?");
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                fields.put("Z_KASSE_ID", rs.getString(1));
                fields.put("Z_ERSTELLUNG", zErstellungDate(rs.getString(2)));
                fields.put("Z_NR", rs.getString(3));
                fields.put("TSE_ID", rs.getString(4));
                fields.put("TSE_SERIAL", rs.getString(5));
                fields.put("TSE_SIG_ALGO", rs.getString(6));
                fields.put("TSE_ZEITFORMAT", rs.getString(7));
                fields.put("TSE_PD_ENCODING", rs.getString(8));
                fields.put("TSE_PUBLIC_KEY", rs.getString(9));
                fields.put("TSE_ZERTIFIKAT_I", rs.getString(10));
                fields.put("TSE_ZERTIFIKAT_II", rs.getString(11));
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        logger.debug(fields);
    }

    
    /**
     *  KASSENABSCHLUSSMODUL
     *
     */

    /**
     *  EINZELAUFZEICHNUNGSMODUL
     *
     */

    public void writeToCSV_TSE_Transaktionen() {
        String filename = "transactions_tse.csv";
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