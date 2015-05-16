package WeltladenDB;

// Basic Java stuff:
import java.io.*; // for InputStream
import java.util.*; // for Vector, Collections
import java.util.Date;
import java.text.*; // for NumberFormat
import java.math.*; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.*;

// GUI stuff:
import java.awt.event.*;
import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

// DateTime from date4j (http://www.date4j.net/javadoc/index.html)
import hirondelle.date4j.DateTime;

// JCalendarButton
import jcalendarbutton.org.JCalendarButton;

public abstract class WindowContent extends JPanel implements ActionListener {

    // mySQL Connection:
    protected Connection conn = null;
    protected MainWindowGrundlage mainWindow = null;
    protected String currencySymbol;
    protected Locale myLocale = Locale.GERMAN;
    protected String mysqlPath;
    protected String sofficePath;
    protected String printerName;
    protected String dateFormatSQL;
    protected String dateFormatJava;
    protected String dateFormatDate4j;
    protected String delimiter; // for CSV export/import
    protected final String fileSep = System.getProperty("file.separator");
    protected final String lineSep = System.getProperty("line.separator");
    protected final int smallintMax = 32767;
    protected final BigDecimal one = new BigDecimal("1");
    protected final BigDecimal minusOne = new BigDecimal("-1");
    protected final BigDecimal percent = new BigDecimal("0.01");
    protected final BigDecimal hundred = new BigDecimal("100.");

    // Die Ausrichter:
    protected DefaultTableCellRenderer rechtsAusrichter = new DefaultTableCellRenderer();
    protected DefaultTableCellRenderer linksAusrichter = new DefaultTableCellRenderer();
    protected DefaultTableCellRenderer zentralAusrichter = new DefaultTableCellRenderer();
    protected final int columnMargin = 20; /** number of pixels of space between table columns */
    protected final int minColumnWidth = 20; /** minimally allowed pixel width of table columns */
    protected final int maxColumnWidth = 150; /** maximally allowed pixel width of table columns */
    // Formats to format and parse numbers
    //protected NumberFormat amountFormat;
    protected NumberFormat vatFormat;

    protected PositiveNumberDocumentFilter geldFilter = new PositiveNumberDocumentFilter(2, 13);
    protected PositiveNumberDocumentFilter relFilter = new PositiveNumberDocumentFilter(3, 6);
    protected PositiveNumberDocumentFilter mengeFilter = new PositiveNumberDocumentFilter(5, 8);
    protected IntegerDocumentFilter intFilter = new IntegerDocumentFilter(-smallintMax, smallintMax, this);

    protected Vector<Integer> beliebtWerte;
    protected Vector<String> beliebtNamen;
    protected Vector<String> beliebtKuerzel;
    protected Vector<Color> beliebtFarben;

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
    public Connection getConnection(){
	return this.conn;
    }
    public MainWindowGrundlage getMainWindowPointer(){
	return this.mainWindow;
    }

    /**
     *    The constructors.
     *       */
    public WindowContent(Connection conn, MainWindowGrundlage mw) {
	this.conn = conn;
	//amountFormat = new DecimalFormat("0.00");
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
            this.sofficePath = props.getProperty("sofficePath"); // path where soffice lies around
            this.printerName = props.getProperty("printerName"); // name of receipt printer
            this.dateFormatSQL = props.getProperty("dateFormatSQL");
            this.dateFormatJava = props.getProperty("dateFormatJava");
            this.dateFormatDate4j = props.getProperty("dateFormatDate4j");
            this.delimiter = props.getProperty("delimiter"); // for CSV export/import
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            this.mysqlPath = "";
            this.sofficePath = "";
            this.printerName = "epson_tmu220";
            this.dateFormatSQL = "%d.%m.%Y, %H:%i Uhr";
            this.dateFormatJava = "dd.MM.yyyy, HH:mm 'Uhr'";
            this.dateFormatDate4j = "DD.MM.YYYY, hh:mm |Uhr|";
            this.delimiter = ";"; // for CSV export/import
        }
        this.printerName = this.printerName.replaceAll("\"","");
        fillBeliebtWerte();
    }

    protected void fillBeliebtWerte() {
        beliebtWerte = new Vector<Integer>();
        beliebtNamen = new Vector<String>();
        beliebtKuerzel = new Vector<String>();
        beliebtFarben = new Vector<Color>();
        beliebtWerte.add(0); beliebtNamen.add("keine Angabe"); beliebtKuerzel.add("●"); beliebtFarben.add(Color.GRAY);
        beliebtWerte.add(1); beliebtNamen.add("niedrig");      beliebtKuerzel.add("●"); beliebtFarben.add(Color.RED);
        beliebtWerte.add(2); beliebtNamen.add("mittel");       beliebtKuerzel.add("●"); beliebtFarben.add(Color.YELLOW);
        beliebtWerte.add(3); beliebtNamen.add("hoch");         beliebtKuerzel.add("●"); beliebtFarben.add(Color.GREEN);
    }


    protected class WindowAdapterDialog extends WindowAdapter {
        private DialogWindow dwindow;
        private JDialog dialog;
        private String warnMessage;
        public WindowAdapterDialog(DialogWindow dw, JDialog dia, String wm) {
            super();
            this.dwindow = dw;
            this.dialog = dia;
            this.warnMessage = wm;
        }
        @Override
        public void windowClosing(WindowEvent we) {
            if ( this.dwindow.willDataBeLost() ){
                int answer = JOptionPane.showConfirmDialog(dialog,
                        warnMessage, "Warnung",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (answer == JOptionPane.YES_OPTION){
                    dialog.dispose();
                } else {
                    // do nothing
                }
            } else {
                dialog.dispose();
            }
        }
    }

    /**
     * General useful helper functions.
     */

    protected Date dateFromDateTime(DateTime dt) {
        /** Returns Date constructed from this DateTime */
        return new Date( dt.getMilliseconds(TimeZone.getDefault()) );
    }

    protected Date dateFromDateTime(DateTime dt, DateTime zeroPoint) {
        /** Returns Date constructed from this DateTime, relative to zeroPoint */
        return new Date( dt.getMilliseconds(TimeZone.getDefault()) -
                zeroPoint.getMilliseconds(TimeZone.getDefault()) );
    }

    protected JTextArea makeLabelStyle(JTextArea textArea) {
        /** Make a JTextArea able to be used as a multi-line label */
        if (textArea == null)
            return null;
        textArea.setFont(UIManager.getFont("Label.font"));
        textArea.setEditable(false);
        textArea.setCursor(null);
        textArea.setOpaque(false);
        textArea.setFocusable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        // important so that with LineWrap, textArea doesn't become huge:
        //    (set it to sth. small, it will expand as much as needed)
        textArea.setPreferredSize(new Dimension(10, 10));
        return textArea;
    }

    protected String constructProgramPath(String dir, String program) {
        String path = "";
        if (dir.length() == 0){
            path = program;
        } else {
            if ( dir.endsWith("\"") ){
                path = dir.substring(0, dir.length()-1)+fileSep+program+"\"";
            } else {
                path = dir+fileSep+program;
            }
        }
        return path;
    }

    /**
     * Price calculation methods
     */

    protected BigDecimal calculateVAT(BigDecimal totalPrice, BigDecimal mwst){
        //return totalPrice.multiply( one.subtract( one.divide(one.add(mwst), 10, RoundingMode.HALF_UP) ) ); // VAT = bruttoPreis * ( 1. - 1./(1.+mwst) );
        return totalPrice.divide(one.add(mwst), 10, RoundingMode.HALF_UP).multiply(mwst); // VAT = bruttoPreis / (1.+mwst) * mwst;
    }

    protected BigDecimal calculateEKP(BigDecimal empfVKPreis, BigDecimal ekRabatt){
        return ( one.subtract(ekRabatt) ).multiply(empfVKPreis); // Einkaufspreis = (1 - rabatt) * Empf. VK-Preis
    }

    protected BigDecimal calculateEKP(String empfVKPreis, BigDecimal ekRabatt){
        BigDecimal empfvkpDecimal;
        try {
            empfvkpDecimal = new BigDecimal(priceFormatterIntern(empfVKPreis));
        } catch (NumberFormatException ex) {
            return null;
        }
        return calculateEKP(empfvkpDecimal, ekRabatt);
    }

    protected BigDecimal calculateEKP(String empfVKPreis, String ekRabatt){
        BigDecimal rabatt;
        try {
            rabatt = new BigDecimal( vatParser(ekRabatt) );
        } catch (NumberFormatException ex) {
            return null;
        }
        return calculateEKP(empfVKPreis, rabatt);
    }

    protected String figureOutEKP(String empfVKPreis, String ekRabatt, String ekPreis){
        /** If empfVKPreis and ekRabatt are both valid numbers, calculate EK-Preis
         *  from them and return it. Otherwise, fall back to using ekPreis.
         */
        BigDecimal preis = calculateEKP(empfVKPreis, ekRabatt);
        if (preis != null){
            return priceFormatterIntern(preis);
        } else {
            return priceFormatterIntern(ekPreis);
        }
    }

    /**
     * Number formatting methods
     */

    protected String decimalMark(String value) {
        /** Use uniform decimal mark */
        return value.replace('.',',');
    }

    protected String unifyDecimal(BigDecimal value) {
        /** Strip trailing zeros and use uniform decimal mark */
        return decimalMark( value.stripTrailingZeros().toPlainString() );
    }

    protected String unifyDecimal(String value) {
        /** Strip trailing zeros and use uniform decimal mark */
        try {
            BigDecimal val = new BigDecimal( value.replaceAll("\\s","").replace(',','.') );
            return unifyDecimal(val);
        } catch (NumberFormatException nfe) {
            return "";
        }
    }

    protected String priceFormatterIntern(BigDecimal price) {
        if (price == null){
            return "";
        }
        // for 2 digits after period sign and "0.5-is-rounded-up" rounding:
        return price.setScale(2, RoundingMode.HALF_UP).toString();
    }

    protected String priceFormatter(BigDecimal price) {
        return decimalMark( priceFormatterIntern(price) );
    }

    protected String priceFormatterIntern(String priceStr) {
        try {
            BigDecimal price = new BigDecimal( priceStr.replace(currencySymbol,"").replaceAll("\\s","").replace(',','.') );
            return priceFormatterIntern(price);
        } catch (NumberFormatException nfe) {
            return "";
        }
    }

    protected String priceFormatter(String priceStr) {
        try {
            BigDecimal price = new BigDecimal( priceStr.replace(currencySymbol,"").replaceAll("\\s","").replace(',','.') );
            return priceFormatter(price);
        } catch (NumberFormatException nfe) {
            return "";
        }
    }

    protected String vatFormatter(String vat) {
        /** Input `vat` is e.g. "0.01"
         *  Returns "1%" */
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

    protected String vatFormatter(BigDecimal vat) {
        /** Input `vat` is e.g. 0.01
         *  Returns "1%" */
        if (vat == null){
            return "";
        }
        return vatFormatter(vat.toString());
    }

    protected String vatPercentRemover(String vat) {
        return vat.replace("%","").replaceAll("\\s","");
    }

    protected String vatParser(String vat) {
        /** Input `vat` is e.g. "1%"
         *  Returns "0.01" */
        try {
            BigDecimal vatDecimal = new BigDecimal( vatPercentRemover(vat).replace(',','.') ).multiply(percent);
            return vatDecimal.toString();
        } catch (NumberFormatException nfe) {
            return "";
        }
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
                    "SELECT COUNT(artikel_id) > 0 FROM artikel INNER JOIN lieferant USING (lieferant_id) "+
                    "WHERE lieferant_name = ? AND artikel_nr = ? AND artikel.aktiv = TRUE"
                    );
            pstmt.setString(1, lieferant);
            pstmt.setString(2, nummer);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            exists = rs.getBoolean(1);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return exists;
    }

    protected int setItemInactive(Integer lieferant_id, String nummer) {
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "UPDATE artikel SET aktiv = FALSE, bis = NOW() WHERE "+
                    "lieferant_id = ? AND "+
                    "artikel_nr = ? AND aktiv = TRUE"
                    );
            pstmt.setInt(1, lieferant_id);
            pstmt.setString(2, nummer);
            result = pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    protected int setItemActive(Integer lieferant_id, String nummer) {
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "UPDATE artikel SET aktiv = TRUE, bis = NOW() WHERE "+
                    "lieferant_id = ? AND "+
                    "artikel_nr = ? AND aktiv = FALSE"
                    );
            pstmt.setInt(1, lieferant_id);
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
            lieferant_id, String nummer, String name, String kurzname,
            BigDecimal menge, String barcode, String herkunft, Integer vpe,
            Integer setgroesse, String vkpreis, String empfvkpreis,
            String ekrabatt, String ekpreis, Boolean var_preis, Boolean sortiment,
            Boolean lieferbar, Integer beliebt, Integer bestand) {
        // add row for new item (with updated fields)
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;

        if (kurzname.equals("") || kurzname.equals("NULL")){ kurzname = null; }
        if (barcode.equals("") || barcode.equals("NULL")){ barcode = null; }
        if (herkunft.equals("") || herkunft.equals("NULL")){ herkunft = null; }
        if ( vpe == null || vpe.equals(0) ){ vpe = null; }
        if ( setgroesse == null || setgroesse.equals(0) ){ setgroesse = 1; }

        BigDecimal vkpDecimal;
        try {
            vkpDecimal = new BigDecimal(priceFormatterIntern(vkpreis));
        } catch (NumberFormatException ex) {
            vkpDecimal = null;
        }

        BigDecimal empfvkpDecimal;
        try {
            empfvkpDecimal = new BigDecimal(priceFormatterIntern(empfvkpreis));
        } catch (NumberFormatException ex) {
            empfvkpDecimal = null;
        }

        BigDecimal ekrabattDecimal;
        try {
            ekrabattDecimal = new BigDecimal( vatParser(ekrabatt) );
        } catch (NumberFormatException ex) {
            ekrabattDecimal = null;
        }

        BigDecimal ekpDecimal;
        try {
            ekpDecimal = new BigDecimal(priceFormatterIntern(ekpreis));
        } catch (NumberFormatException ex) {
            ekpDecimal = null;
        }

        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "INSERT INTO artikel SET "+
                    "produktgruppen_id = ?, lieferant_id = ?, "+
                    "artikel_nr = ?, artikel_name = ?, "+
                    "kurzname = ?, "+
                    "menge = ?, "+
                    "barcode = ?, "+
                    "herkunft = ?, "+
                    "vpe = ?, "+
                    "setgroesse = ?, "+
                    "vk_preis = ?, empf_vk_preis = ?, "+
                    "ek_rabatt = ?, ek_preis = ?, "+
                    "variabler_preis = ?, sortiment = ?, "+
                    "lieferbar = ?, beliebtheit = ?, "+
                    "bestand = ?, "+
                    "von = NOW(), aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, produktgruppen_id);
            pstmtSetInteger(pstmt, 2, lieferant_id);
            pstmt.setString(3, nummer);
            pstmt.setString(4, name);
            pstmt.setString(5, kurzname);
            pstmt.setBigDecimal(6, menge);
            pstmt.setString(7, barcode);
            pstmt.setString(8, herkunft);
            pstmtSetInteger(pstmt, 9, vpe);
            pstmtSetInteger(pstmt, 10, setgroesse);
            pstmt.setBigDecimal(11, vkpDecimal);
            pstmt.setBigDecimal(12, empfvkpDecimal);
            pstmt.setBigDecimal(13, ekrabattDecimal);
            pstmt.setBigDecimal(14, ekpDecimal);
            pstmtSetBoolean(pstmt, 15, var_preis);
            pstmtSetBoolean(pstmt, 16, sortiment);
            pstmtSetBoolean(pstmt, 17, lieferbar);
            pstmtSetInteger(pstmt, 18, beliebt);
            pstmtSetInteger(pstmt, 19, bestand);
            result = pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    protected boolean isLieferantAlreadyKnown(String lieferant) {
        boolean exists = false;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT COUNT(lieferant_id) > 0 FROM lieferant "+
                    "WHERE lieferant_name = ?"
                    );
            pstmt.setString(1, lieferant);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            exists = rs.getBoolean(1);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return exists;
    }

    protected boolean isLieferantInactive(String lieferant) {
        boolean inactive = false;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT aktiv FROM lieferant "+
                    "WHERE lieferant_name = ?"
                    );
            pstmt.setString(1, lieferant);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            inactive = !rs.getBoolean(1);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return inactive;
    }

    protected int howManyActiveArticlesWithLieferant(Integer lieferant_id) {
        int nArticles = 0;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT COUNT(a.artikel_id) FROM artikel AS a "+
                    "INNER JOIN lieferant USING (lieferant_id) "+
                    "WHERE a.lieferant_id = ? AND a.aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, lieferant_id);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            nArticles = rs.getInt(1);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return nArticles;
    }

    protected boolean thereAreActiveArticlesWithLieferant(Integer lieferant_id) {
        return howManyActiveArticlesWithLieferant(lieferant_id) > 0;
    }

    protected int updateLieferant(Integer lieferant_id, String lieferant_name, Boolean aktiv) {
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;
        if (aktiv == false){
            // check if there are still active articles with this lieferant
            if (thereAreActiveArticlesWithLieferant(lieferant_id)){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Es gibt noch aktive Artikel mit dem Lieferanten "+lieferant_name+".",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                return result;
            }
        }
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "UPDATE lieferant SET lieferant_name = ?, aktiv = ? WHERE "+
                    "lieferant_id = ?"
                    );
            pstmt.setString(1, lieferant_name);
            pstmt.setBoolean(2, aktiv);
            pstmt.setInt(3, lieferant_id);
            result = pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    protected int setLieferantInactive(Integer lieferant_id) {
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;
        // check if there are still active articles with this lieferant
        if (thereAreActiveArticlesWithLieferant(lieferant_id)){
            JOptionPane.showMessageDialog(this,
                "Fehler: Es gibt noch aktive Artikel mit dem Lieferanten Nr. "+lieferant_id+".",
                "Fehler", JOptionPane.ERROR_MESSAGE);
            return result;
        }
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "UPDATE lieferant SET aktiv = FALSE WHERE "+
                    "lieferant_id = ?"
                    );
            pstmt.setInt(1, lieferant_id);
            result = pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    protected int setLieferantActive(Integer lieferant_id) {
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "UPDATE lieferant SET aktiv = TRUE WHERE "+
                    "lieferant_id = ?"
                    );
            pstmt.setInt(1, lieferant_id);
            result = pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    protected int insertNewLieferant(String lieferantName) {
        // add row for new item (with updated fields)
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;
        if ( isLieferantAlreadyKnown(lieferantName) ) return 0;

        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "INSERT INTO lieferant SET "+
                    "lieferant_name = ?, "+
                    "aktiv = TRUE"
                    );
            pstmt.setString(1, lieferantName);
            result = pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    protected boolean isProdGrAlreadyKnown(String produktgruppe) {
        boolean exists = false;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT COUNT(produktgruppen_id) > 0 FROM produktgruppe "+
                    "WHERE produktgruppen_name = ?"
                    );
            pstmt.setString(1, produktgruppe);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            exists = rs.getBoolean(1);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return exists;
    }

    protected boolean isProdGrInactive(String produktgruppe) {
        boolean inactive = false;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT aktiv FROM produktgruppe "+
                    "WHERE produktgruppen_name = ?"
                    );
            pstmt.setString(1, produktgruppe);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            inactive = !rs.getBoolean(1);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return inactive;
    }

    protected int howManyActiveArticlesWithProduktgruppe(Integer produktgruppen_id) {
        int nArticles = 0;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT COUNT(a.artikel_id) FROM artikel AS a "+
                    "INNER JOIN produktgruppe USING (produktgruppen_id) "+
                    "WHERE a.produktgruppen_id = ? AND a.aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, produktgruppen_id);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            nArticles = rs.getInt(1);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return nArticles;
    }

    protected boolean thereAreActiveArticlesWithProduktgruppe(Integer produktgruppen_id) {
        return howManyActiveArticlesWithProduktgruppe(produktgruppen_id) > 0;
    }

    protected int updateProdGr(Integer produktgruppen_id, String produktgruppen_name, Boolean aktiv) {
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;
        if (aktiv == false){
            // check if there are still active articles with this lieferant
            if (thereAreActiveArticlesWithProduktgruppe(produktgruppen_id)){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Es gibt noch aktive Artikel mit der Produktgruppe "+produktgruppen_name+".",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                return result;
            }
        }
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "UPDATE produktgruppe SET produktgruppen_name = ?, aktiv = ? WHERE "+
                    "produktgruppen_id = ?"
                    );
            pstmt.setString(1, produktgruppen_name);
            pstmt.setBoolean(2, aktiv);
            pstmt.setInt(3, produktgruppen_id);
            result = pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    protected int updateProdGr(Integer produktgruppen_id, Integer topid, Integer subid, Integer
            subsubid, String newName, Integer mwst_id, Integer pfand_id, Boolean aktiv) {
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;
        if (aktiv == false){
            // check if there are still active articles with this lieferant
            if (thereAreActiveArticlesWithProduktgruppe(produktgruppen_id)){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Es gibt noch aktive Artikel mit der Produktgruppe "+newName+".",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                return result;
            }
        }
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "UPDATE produktgruppe SET toplevel_id = ?, sub_id = ?, subsub_id = ?, "+
                    "produktgruppen_name = ?, mwst_id = ?, pfand_id = ?, aktiv = ? WHERE "+
                    "produktgruppen_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, topid);
            pstmtSetInteger(pstmt, 2, subid);
            pstmtSetInteger(pstmt, 3, subsubid);
            pstmt.setString(4, newName);
            pstmtSetInteger(pstmt, 5, mwst_id);
            pstmtSetInteger(pstmt, 6, pfand_id);
            pstmt.setBoolean(7, aktiv);
            pstmt.setInt(8, produktgruppen_id);
            result = pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    protected int setProdGrInactive(Integer produktgruppen_id) {
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;
        // check if there are still active articles with this lieferant
        if (thereAreActiveArticlesWithProduktgruppe(produktgruppen_id)){
            JOptionPane.showMessageDialog(this,
                "Fehler: Es gibt noch aktive Artikel mit der Produktgruppe Nr. "+produktgruppen_id+".",
                "Fehler", JOptionPane.ERROR_MESSAGE);
            return result;
        }
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "UPDATE produktgruppe SET aktiv = FALSE WHERE "+
                    "produktgruppen_id = ?"
                    );
            pstmt.setInt(1, produktgruppen_id);
            result = pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    protected int setProdGrActive(Integer produktgruppen_id) {
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "UPDATE produktgruppe SET aktiv = TRUE WHERE "+
                    "produktgruppen_id = ?"
                    );
            pstmt.setInt(1, produktgruppen_id);
            result = pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    protected int insertNewProdGr(Integer topid, Integer subid, Integer
            subsubid, String newName, Integer mwst_id, Integer pfand_id) {
        // add row for new item (with updated fields)
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;
        if ( isProdGrAlreadyKnown(newName) ) return 0;

        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "INSERT INTO produktgruppe SET "+
                    "toplevel_id = ?, sub_id = ?, subsub_id = ?, "+
                    "produktgruppen_name = ?, mwst_id = ?, pfand_id = ?, "+
                    "aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, topid);
            pstmtSetInteger(pstmt, 2, subid);
            pstmtSetInteger(pstmt, 3, subsubid);
            pstmt.setString(4, newName);
            pstmtSetInteger(pstmt, 5, mwst_id);
            pstmtSetInteger(pstmt, 6, pfand_id);
            result = pstmt.executeUpdate();
            pstmt.close();
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
