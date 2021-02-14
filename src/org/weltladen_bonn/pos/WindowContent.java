package org.weltladen_bonn.pos;

// Basic Java stuff:
import java.io.*; // for InputStream
import java.util.*; // for Vector, Collections
import java.util.Date; // because Date alone ambiguous due to java.sql.Date
import java.math.*; // for monetary value representation and arithmetic with correct rounding
import java.text.SimpleDateFormat;
import java.text.ParseException;

// MySQL Connector/J stuff:
import java.sql.*;
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import java.awt.event.*;
import java.awt.*;

import javax.swing.*;
import javax.swing.text.*; // for DocumentFilter
import javax.swing.table.*;

// DateTime from date4j (http://www.date4j.net/javadoc/index.html)
import hirondelle.date4j.DateTime;

// old calendar button:
import org.weltladen_bonn.pos.jcalendarbutton.JCalendarButton;
// new calendar button:
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JSpinnerDateEditor;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class WindowContent extends JPanel implements ActionListener {
    private static final Logger logger = LogManager.getLogger(WindowContent.class);

    // mySQL Connection:
    // protected Connection conn = null;
    protected MariaDbPoolDataSource pool; // pool of connections to MySQL database
    protected MainWindowGrundlage mainWindow = null;
    protected BaseClass bc = null;

    // Die Ausrichter:
    protected DefaultTableCellRenderer rechtsAusrichter = new DefaultTableCellRenderer();
    protected DefaultTableCellRenderer linksAusrichter = new DefaultTableCellRenderer();
    protected DefaultTableCellRenderer zentralAusrichter = new DefaultTableCellRenderer();
    protected final int columnMargin = 20;
    /** number of pixels of space between table columns */
    protected final int minColumnWidth = 20;
    /** minimally allowed pixel width of table columns */
    protected final int maxColumnWidth = 150;
    /** maximally allowed pixel width of table columns */

    protected IntegerDocumentFilter intFilter;
    protected IntegerDocumentFilter vpeFilter;
    protected IntegerDocumentFilter beliebtFilter;



    // Methoden:
    // Setter:
    // void setConnection(Connection conn) {
    //     this.conn = conn;
    // }
    void setConnectionPool(MariaDbPoolDataSource pool) {
        this.pool = pool;
    }

    void setMainWindowPointer(MainWindowGrundlage mw) {
        this.mainWindow = mw;
        this.bc = mw.bc;
    }

    // Getter:
    // public Connection getConnection() {
    //     return this.conn;
    // }
    public MariaDbPoolDataSource getConnectionPool() {
        return this.pool;
    }

    public MainWindowGrundlage getMainWindowPointer() {
        return this.mainWindow;
    }

    /**
     * The constructors.
     */
    public WindowContent(MariaDbPoolDataSource pool, MainWindowGrundlage mw) {
        this.pool = pool;
        this.mainWindow = mw;
        this.bc = mw.bc;

        intFilter = new IntegerDocumentFilter(-bc.smallintMax, bc.smallintMax, this);
        vpeFilter = new IntegerDocumentFilter(1, bc.smallintMax, this);
        beliebtFilter = new IntegerDocumentFilter(bc.minBeliebt, bc.maxBeliebt, this);

        rechtsAusrichter.setHorizontalAlignment(JLabel.RIGHT);
        linksAusrichter.setHorizontalAlignment(JLabel.LEFT);
        zentralAusrichter.setHorizontalAlignment(JLabel.CENTER);
        this.setLayout(new BorderLayout());
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
            if (this.dwindow.willDataBeLost()) {
                int answer = JOptionPane.showConfirmDialog(dialog, warnMessage, "Warnung", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (answer == JOptionPane.YES_OPTION) {
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
        return new Date(dt.getMilliseconds(TimeZone.getDefault()));
    }

    protected Date dateFromDateTime(DateTime dt, DateTime zeroPoint) {
        /**
         * Returns Date constructed from this DateTime, relative to zeroPoint
         */
        return new Date(dt.getMilliseconds(TimeZone.getDefault()) - zeroPoint.getMilliseconds(TimeZone.getDefault()));
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
        // (set it to sth. small, it will expand as much as needed)
        textArea.setPreferredSize(new Dimension(10, 10));
        return textArea;
    }

    protected JTextPane makeLabelStyle(JTextPane textArea) {
        /** Make a JTextArea able to be used as a multi-line label */
        if (textArea == null)
            return null;
        textArea.setFont(UIManager.getFont("Label.font"));
        textArea.setEditable(false);
        textArea.setCursor(null);
        textArea.setOpaque(false);
        textArea.setFocusable(false);
//        textArea.setLineWrap(true);
//        textArea.setWrapStyleWord(true);
        // important so that with LineWrap, textArea doesn't become huge:
        // (set it to sth. small, it will expand as much as needed)
//        textArea.setPreferredSize(new Dimension(10, 10));
        return textArea;
    }

    protected String constructProgramPath(String dir, String program) {
        String path = "";
        if (dir.length() == 0) {
            path = program;
        } else {
            if (dir.endsWith("\"")) {
                path = dir.substring(0, dir.length() - 1) + bc.fileSep + program + "\"";
            } else {
                path = dir + bc.fileSep + program;
            }
        }
        return path;
    }

    protected void applyFilter(String filterStr, Vector<Vector<Object>> data, Vector<Integer> indices) {
        /**
         * Filter all rows in `data` using the filter strings given in
         * `filterStr`, which are space separated. Each filter string must be
         * present (case insensitive). This behaviour is the same as an SQL
         * query using 'foo LIKE "%str1%" AND foo LIKE "%str2%" AND ...'.
         * Returns only the rows of `data` and `indices` that correspond to rows
         * in `data` containing all the filter strings.
         */
        if (filterStr.length() < 3) {
            return;
        }
        // Search in each row
        final Vector<Vector<Object>> fullData = new Vector<Vector<Object>>(data);
        final Vector<Integer> fullIndices = new Vector<Integer>(indices);
        for (int i = 0; i < fullData.size(); i++) {
            boolean contains = true;
            String row = "";
            for (Object obj : fullData.get(i)) {
                // omit UI components:
                if (obj != null && !(obj instanceof JComponent)) {
                    String str = obj.toString().toLowerCase();
                    row = row.concat(str + " ");
                }
            }
            // row must contain (somewhere) each whitespace separated filter
            // word
            for (String fstr : filterStr.split("\\s+")) {
                if (fstr.equals(""))
                    continue;
                if (!row.contains(fstr.toLowerCase())) {
                    contains = false;
                    break;
                }
            }
            if (!contains) {
                int display_index = indices.indexOf(fullIndices.get(i));
                data.remove(display_index);
                indices.remove(display_index);
            }
        }
    }

    protected String handleMissingSalePrice(String title, String artikelname, String artikelnummer, String lieferant,
            String barcode) {
        JPanel preisPanel = new JPanel();
        JTextField preisField = new JTextField("");
        ((AbstractDocument) preisField.getDocument()).setDocumentFilter(bc.geldFilter);
        preisField.setColumns(6);
        preisField.setHorizontalAlignment(JTextField.RIGHT);
        preisPanel.add(preisField);
        preisPanel.add(new JLabel(bc.currencySymbol));

        ArrayList<Object> objects = new ArrayList<Object>();
        objects.add(new JLabel("Wie viel kostet der Artikel?"));
        objects.add(new JLabel("\"" + artikelname + "\""));
        JPanel labelPanel = new JPanel();
        if (artikelnummer != null)
            labelPanel.add(new JLabel("Artikel-Nr.: " + artikelnummer));
        if (lieferant != null)
            labelPanel.add(new JLabel("    von: " + lieferant));
        objects.add(labelPanel);
        if (barcode != null)
            objects.add(new JLabel("Barcode: " + barcode));
        objects.add(preisPanel);

        JOptionPane jop = new JOptionPane(objects.toArray(), JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = jop.createDialog(title);
        dialog.setVisible(true);
        int result = (Integer) jop.getValue();
        dialog.dispose();

        String artikelPreis = "";
        if (result == JOptionPane.OK_OPTION) {
            artikelPreis = new String(preisField.getText());
            if (artikelPreis == null)
                return "";
        }
        return artikelPreis;
    }

    /**
     * Price calculation methods
     */

    protected BigDecimal calculateVAT(BigDecimal totalPrice, BigDecimal mwst) {
        /*
         * VAT = bruttoPreis / (1.+mwst) * mwst;
         * Alternative:
         *  // VAT = bruttoPreis * ( 1. - 1./(1.+mwst) );
         *  return totalPrice.multiply(
         *      one.subtract( one.divide(one.add(mwst), 10, RoundingMode.HALF_UP) )
         *      );
         */
        BigDecimal vat = totalPrice.divide(bc.one.add(mwst), 10, RoundingMode.HALF_UP).multiply(mwst);
        return new BigDecimal(bc.priceFormatterIntern(vat)); // this line is for rounding to 2 decimal places after separator
    }

    private BigDecimal calculateEKP(BigDecimal empfVKPreis, BigDecimal ekRabatt) {
        /*
         * Einkaufspreis = (1 - rabatt) * Empf. VK-Preis
         */
        return (bc.one.subtract(ekRabatt)).multiply(empfVKPreis);
    }

    private BigDecimal calculateEKP(String empfVKPreis, BigDecimal ekRabatt) {
        BigDecimal empfvkpDecimal;
        try {
            empfvkpDecimal = new BigDecimal(bc.priceFormatterIntern(empfVKPreis));
        } catch (NumberFormatException ex) {
            return null;
        }
        return calculateEKP(empfvkpDecimal, ekRabatt);
    }

    BigDecimal calculateEKP(String empfVKPreis, String ekRabatt) {
        BigDecimal rabatt;
        try {
            rabatt = new BigDecimal(bc.vatParser(ekRabatt));
        } catch (NumberFormatException ex) {
            return null;
        }
        return calculateEKP(empfVKPreis, rabatt);
    }

    String figureOutEKP(String empfVKPreis, String ekRabatt, String ekPreis) {
        /**
         * If empfVKPreis and ekRabatt are both valid numbers, calculate
         * EK-Preis from them and return it. Otherwise, fall back to using
         * ekPreis.
         */
        BigDecimal preis = calculateEKP(empfVKPreis, ekRabatt);
        if (preis != null) {
            return bc.priceFormatterIntern(preis);
        } else {
            return bc.priceFormatterIntern(ekPreis);
        }
    }

    boolean empfVKPAndEKRabattValid(String empfVKPreis, String ekRabatt) {
        /**
         * If empfVKPreis and ekRabatt are both valid numbers, return true, else
         * false.
         */
        try {
            new BigDecimal(bc.priceFormatterIntern(empfVKPreis));
        } catch (NumberFormatException ex) {
            return false;
        }
        try {
            new BigDecimal(bc.vatParser(ekRabatt));
        } catch (NumberFormatException ex) {
            return false;
        }
        return true;
    }

    /**
     * DB methods
     */

    protected void showDBErrorDialog(String message) {
        JOptionPane.showMessageDialog(this,
            "Verbindung zum Datenbank-Server unterbrochen?\n"+
            "Fehlermeldung: "+message,
            "Fehler", JOptionPane.ERROR_MESSAGE);
    }

    protected void pstmtSetInteger(PreparedStatement pstmt, int paramIndex, Integer x) {
        /**
         * Home made method to put Integer class instances (that can be null)
         * into a DB and treat them accordingly (Java null becomes SQL NULL)
         */
        try {
            if (x == null) {
                pstmt.setNull(paramIndex, Types.INTEGER);
            } else {
                pstmt.setInt(paramIndex, x);
            }
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
    }

    protected void pstmtSetBoolean(PreparedStatement pstmt, int paramIndex, Boolean x) {
        /**
         * Home made method to put Boolean class instances (that can be null)
         * into a DB and treat them accordingly (Java null becomes SQL NULL)
         */
        try {
            if (x == null) {
                pstmt.setNull(paramIndex, Types.INTEGER);
            } else {
                pstmt.setBoolean(paramIndex, x);
            }
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
    }

    boolean isArticleAlreadyKnown(Integer lieferant_id, String nummer) {
        boolean exists = false;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement("SELECT COUNT(*) > 0 FROM artikel "
                    + "WHERE lieferant_id = ? AND artikel_nr = ? AND artikel.aktiv = TRUE");
            pstmtSetInteger(pstmt, 1, lieferant_id);
            pstmt.setString(2, nummer);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            exists = rs.getBoolean(1);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return exists;
    }

    boolean doesArticleHaveBarcode(Integer artikel_id) {
        boolean hasBarcode = false;
        try {
            // barcode shall not be NULL and not contain only whitespace
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("SELECT (barcode IS NOT NULL AND barcode NOT REGEXP '^[[:blank:]]*$') "+
                            "FROM artikel WHERE artikel_id = ?");
            pstmtSetInteger(pstmt, 1, artikel_id);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            hasBarcode = rs.getBoolean(1);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return hasBarcode;
    }

    boolean doesArticleHaveVarPrice(Integer artikel_id) {
        boolean hasVarPrice = false;
        try {
            // barcode shall not be NULL and not contain only whitespace
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("SELECT variabler_preis "+
                            "FROM artikel WHERE artikel_id = ?");
            pstmtSetInteger(pstmt, 1, artikel_id);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            hasVarPrice = rs.getBoolean(1);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return hasVarPrice;
    }

    private int setArticleInactive(Artikel a) {
        // returns 0 if there was an error, otherwise number of rows affected
        // (>0)
        int result = 0;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement("UPDATE artikel SET aktiv = FALSE, bis = NOW() WHERE "
                    + "lieferant_id = ? AND " + "artikel_nr = ? AND aktiv = TRUE");
            pstmtSetInteger(pstmt, 1, a.getLiefID());
            pstmt.setString(2, a.getNummer());
            result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();

            // update the `n_artikel` fields
            updateNArtikelInProduktgruppeFor(a.getProdGrID());
            updateNArtikelRekursivRecursivelyInProduktgruppeFor(a.getProdGrID());
            updateNArtikelInLieferantFor(a.getLiefID());
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }

    private int setArticleActive(Artikel a) {
        // returns 0 if there was an error, otherwise number of rows affected
        // (>0)
        int result = 0;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement("UPDATE artikel SET aktiv = TRUE, bis = NOW() WHERE "
                    + "lieferant_id = ? AND " + "artikel_nr = ? AND aktiv = FALSE");
            pstmtSetInteger(pstmt, 1, a.getLiefID());
            pstmt.setString(2, a.getNummer());
            result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();

            // update the `n_artikel` fields
            updateNArtikelInProduktgruppeFor(a.getProdGrID());
            updateNArtikelRekursivRecursivelyInProduktgruppeFor(a.getProdGrID());
            updateNArtikelInLieferantFor(a.getLiefID());
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }

    int insertNewArticle(Artikel a) {
        // add row for new item (with updated fields)
        // returns 0 if there was an error, otherwise number of rows affected
        // (>0)
        int result = 0;

        if (a.getKurzname().equals("") || a.getKurzname().equals("NULL")) {
            a.setKurzname(null);
        }
        if (a.getEinheit().equals("") || a.getEinheit().equals("NULL")) {
            a.setEinheit(null);
        }
        if (a.getBarcode().equals("") || a.getBarcode().equals("NULL")) {
            a.setBarcode(null);
        }
        if (a.getHerkunft().equals("") || a.getHerkunft().equals("NULL")) {
            a.setHerkunft(null);
        }
        if (a.getVPE() == null || a.getVPE().equals(0)) {
            a.setVPE(null);
        }
        if (a.getSetgroesse() == null || a.getSetgroesse().equals(0)) {
            a.setSetgroesse(1);
        }

        BigDecimal vkpDecimal;
        try {
            vkpDecimal = new BigDecimal(bc.priceFormatterIntern(a.getVKP()));
        } catch (NumberFormatException ex) {
            vkpDecimal = null;
        }

        BigDecimal empfvkpDecimal;
        try {
            empfvkpDecimal = new BigDecimal(bc.priceFormatterIntern(a.getEmpfVKP()));
        } catch (NumberFormatException ex) {
            empfvkpDecimal = null;
        }

        BigDecimal ekrabattDecimal;
        try {
            ekrabattDecimal = new BigDecimal(bc.vatParser(a.getEKRabatt()));
        } catch (NumberFormatException ex) {
            ekrabattDecimal = null;
        }

        BigDecimal ekpDecimal;
        try {
            ekpDecimal = new BigDecimal(bc.priceFormatterIntern(a.getEKP()));
        } catch (NumberFormatException ex) {
            ekpDecimal = null;
        }

        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement("INSERT INTO artikel SET "
                    + "produktgruppen_id = ?, lieferant_id = ?, " + "artikel_nr = ?, artikel_name = ?, "
                    + "kurzname = ?, " + "menge = ?, " + "einheit = ?, " + "barcode = ?, " + "herkunft = ?, "
                    + "vpe = ?, " + "setgroesse = ?, " + "vk_preis = ?, empf_vk_preis = ?, "
                    + "ek_rabatt = ?, ek_preis = ?, " + "variabler_preis = ?, sortiment = ?, "
                    + "lieferbar = ?, beliebtheit = ?, " + "bestand = ?, " + "von = NOW(), aktiv = TRUE");
            pstmtSetInteger(pstmt, 1, a.getProdGrID());
            pstmtSetInteger(pstmt, 2, a.getLiefID());
            pstmt.setString(3, a.getNummer());
            pstmt.setString(4, a.getName());
            pstmt.setString(5, a.getKurzname());
            pstmt.setBigDecimal(6, a.getMenge());
            pstmt.setString(7, a.getEinheit());
            pstmt.setString(8, a.getBarcode());
            pstmt.setString(9, a.getHerkunft());
            pstmtSetInteger(pstmt, 10, a.getVPE());
            pstmtSetInteger(pstmt, 11, a.getSetgroesse());
            pstmt.setBigDecimal(12, vkpDecimal);
            pstmt.setBigDecimal(13, empfvkpDecimal);
            pstmt.setBigDecimal(14, ekrabattDecimal);
            pstmt.setBigDecimal(15, ekpDecimal);
            pstmtSetBoolean(pstmt, 16, a.getVarPreis());
            pstmtSetBoolean(pstmt, 17, a.getSortiment());
            pstmtSetBoolean(pstmt, 18, a.getLieferbar());
            pstmtSetInteger(pstmt, 19, a.getBeliebt());
            pstmtSetInteger(pstmt, 20, a.getBestand());
            result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();

            // update the `n_artikel` fields
            updateNArtikelInProduktgruppeFor(a.getProdGrID());
            updateNArtikelRekursivRecursivelyInProduktgruppeFor(a.getProdGrID());
            updateNArtikelInLieferantFor(a.getLiefID());
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }

    private String selectArticleString() {
        return "SELECT " + "produktgruppen_id, " + "lieferant_id, " + "artikel_nr, " + "artikel_name, " + "kurzname, "
                + "menge, " + "einheit, " + "barcode, " + "herkunft, " + "vpe, " + "setgroesse, "
                + "vk_preis, empf_vk_preis, " + "ek_rabatt, ek_preis, " + "variabler_preis, sortiment, "
                + "lieferbar, beliebtheit, " + "bestand ";
    }

    private Artikel parseArticleResultSet(ResultSet rs) {
        Artikel a = new Artikel(bc);
        try {
            rs.next();
            a.setProdGrID(rs.getInt(1));
            a.setLiefID(rs.getInt(2));
            a.setNummer(rs.getString(3));
            a.setName(rs.getString(4));
            a.setKurzname(rs.getString(5));
            a.setMenge(rs.getBigDecimal(6));
            a.setEinheit(rs.getString(7));
            a.setBarcode(rs.getString(8));
            a.setHerkunft(rs.getString(9));
            a.setVPE(rs.getInt(10));
            a.setSetgroesse(rs.getInt(11));
            Boolean var = rs.getBoolean(16);
            a.setVKP(var ? "" : rs.getString(12));
            a.setEmpfVKP(var ? "" : rs.getString(13));
            a.setEKRabatt(bc.vatFormatter(rs.getString(14)));
            a.setEKP(var ? "" : rs.getString(15));
            a.setVarPreis(var);
            a.setSortiment(rs.getBoolean(17));
            a.setLieferbar(rs.getBoolean(18));
            a.setBeliebt(rs.getInt(19));
            a.setBestand(rs.getInt(20));
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return a;
    }

    protected Artikel getArticle(Integer artikel_id) {
        Artikel a = new Artikel(bc);
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement(selectArticleString() + "FROM artikel " + "WHERE artikel_id = ?");
            pstmtSetInteger(pstmt, 1, artikel_id);
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result
            // ...
            a = parseArticleResultSet(rs);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return a;
    }

    protected Artikel getArticle(Integer lieferant_id, String artikel_nr) {
        Artikel a = new Artikel(bc);
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(selectArticleString() + "FROM artikel "
                    + "WHERE lieferant_id = ? AND " + "artikel_nr = ? AND artikel.aktiv = TRUE");
            pstmtSetInteger(pstmt, 1, lieferant_id);
            pstmt.setString(2, artikel_nr);
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result
            // ...
            a = parseArticleResultSet(rs);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return a;
    }

    String getProduktgruppe(Integer produktgruppen_id) {
        String produktgruppe = "";
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("SELECT produktgruppen_name FROM produktgruppe " + "WHERE produktgruppen_id = ?");
            pstmtSetInteger(pstmt, 1, produktgruppen_id);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            produktgruppe = rs.getString(1);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return produktgruppe;
    }

    String getLieferant(Integer lieferant_id) {
        String lieferant = "";
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("SELECT lieferant_name FROM lieferant " + "WHERE lieferant_id = ?");
            pstmtSetInteger(pstmt, 1, lieferant_id);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            lieferant = rs.getString(1);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return lieferant;
    }

    Integer getLieferantID(String lieferant) {
        Integer lieferant_id = 1;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("SELECT lieferant_id FROM lieferant " + "WHERE lieferant_name = ?");
            pstmt.setString(1, lieferant);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                lieferant_id = rs.getInt(1);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return lieferant_id;
    }

    protected Vector<Object> getLieferantIDArtikelNummer(Integer artikel_id) {
        Vector<Object> liefIDAndNr = new Vector<Object>();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("SELECT lieferant_id, artikel_nr FROM artikel " + "WHERE artikel_id = ?");
            pstmtSetInteger(pstmt, 1, artikel_id);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            Integer lieferant_id = rs.getInt(1);
            String artikel_nr = rs.getString(2);
            liefIDAndNr.add(lieferant_id);
            liefIDAndNr.add(artikel_nr);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return liefIDAndNr;
    }

    boolean isLieferantAlreadyKnown(String lieferant) {
        boolean exists = false;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("SELECT COUNT(*) > 0 FROM lieferant " + "WHERE lieferant_name = ?");
            pstmt.setString(1, lieferant);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            exists = rs.getBoolean(1);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return exists;
    }

    boolean isLieferantInactive(String lieferant) {
        boolean inactive = false;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("SELECT aktiv FROM lieferant " + "WHERE lieferant_name = ?");
            pstmt.setString(1, lieferant);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            inactive = !rs.getBoolean(1);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return inactive;
    }

    private int howManyActiveArticlesWithLieferant(Integer lieferant_id) {
        int nArticles = 0;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("SELECT n_artikel FROM lieferant " + "WHERE lieferant_id = ?");
            pstmtSetInteger(pstmt, 1, lieferant_id);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            nArticles = rs.getInt(1);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return nArticles;
    }

    private boolean thereAreActiveArticlesWithLieferant(Integer lieferant_id) {
        return howManyActiveArticlesWithLieferant(lieferant_id) > 0;
    }

    private int queryActiveArticlesWithLieferant(Integer lieferant_id) {
        int nArticles = 0;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement("SELECT COUNT(*) FROM artikel AS a "
                    + "INNER JOIN lieferant USING (lieferant_id) " + "WHERE a.lieferant_id = ? AND a.aktiv = TRUE");
            pstmtSetInteger(pstmt, 1, lieferant_id);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            nArticles = rs.getInt(1);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return nArticles;
    }

    private int setNArtikelForLieferant(int liefID, int nArticles) {
        // returns 0 if there was an error, otherwise number of rows affected
        // (>0)
        int result = 0;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("UPDATE lieferant SET n_artikel = ? WHERE " + "lieferant_id = ?");
            pstmtSetInteger(pstmt, 1, nArticles);
            pstmtSetInteger(pstmt, 2, liefID);
            result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }

    private void updateNArtikelInLieferantFor(int lieferant_id) {
        //System.out.print("Upating lieferant_id " + lieferant_id + " to n_artikel = ");
        int nArticles = queryActiveArticlesWithLieferant(lieferant_id);
        //System.out.println(nArticles);
        int result = 0;
        result = setNArtikelForLieferant(lieferant_id, nArticles);
        if (result == 0) {
            System.err.println(
                    "ERROR: Could not set `n_artikel` to " + nArticles + " for lieferant_id = " + lieferant_id);
            JOptionPane.showMessageDialog(this, "Fehler: Lieferant mit ID " + lieferant_id
                    + ": `n_artikel` konnte nicht " + "auf " + nArticles + " gesetzt werden.", "Fehler",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    void updateNArtikelInLieferant() {
        /**
         * For all rows in `lieferant` that have `n_artikel` = NULL, query for
         * the number of active articles and set `n_artikel`.
         */
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT lieferant_id FROM " + "lieferant WHERE n_artikel IS NULL");
            while (rs.next()) {
                int liefID = rs.getInt(1);
                updateNArtikelInLieferantFor(liefID);
            }
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
    }

    protected void updateArticle(Artikel oldArticle, Artikel newArticle) {
        // set old item to inactive:
        int result = setArticleInactive(oldArticle);
        if (result == 0) {
            JOptionPane.showMessageDialog(this,
                    "Fehler: Artikel von " + getLieferant(oldArticle.getLiefID()) + " mit " + "Nummer "
                            + oldArticle.getNummer() + " konnte nicht " + "geändert werden.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (newArticle.getAktiv()) { // only if the item wasn't
            // set inactive voluntarily: add new item with new properties
            String ekpreis = figureOutEKP(newArticle.getEmpfVKP(), newArticle.getEKRabatt(), newArticle.getEKP());
            newArticle.setEKP(ekpreis);
            result = insertNewArticle(newArticle);
            if (result == 0) {
                JOptionPane.showMessageDialog(this,
                        "Fehler: Artikel von " + getLieferant(oldArticle.getLiefID()) + "mit Nummer "
                                + oldArticle.getNummer() + " konnte " + "nicht geändert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                result = setArticleActive(oldArticle);
                if (result == 0) {
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel von " + getLieferant(oldArticle.getLiefID()) + " mit Nummer "
                                    + oldArticle.getNummer() + " konnte nicht "
                                    + "wieder hergestellt werden. Artikel ist nun " + "gelöscht (inaktiv).",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    int updateLieferant(Integer lieferant_id, String lieferant_name, String lieferant_kurzname,
                        Boolean aktiv) {
        // returns 0 if there was an error, otherwise number of rows affected
        // (>0)
        int result = 0;
        if (!aktiv) {
            // check if there are still active articles with this lieferant
            if (thereAreActiveArticlesWithLieferant(lieferant_id)) {
                JOptionPane.showMessageDialog(this,
                        "Fehler: Es gibt noch aktive Artikel mit dem Lieferanten " + lieferant_name + ".", "Fehler",
                        JOptionPane.ERROR_MESSAGE);
                return result;
            }
        }
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("UPDATE lieferant SET lieferant_name = ?, lieferant_kurzname = ?, aktiv = ? "
                            + "WHERE lieferant_id = ?");
            pstmt.setString(1, lieferant_name);
            pstmt.setString(2, lieferant_kurzname);
            pstmt.setBoolean(3, aktiv);
            pstmtSetInteger(pstmt, 4, lieferant_id);
            result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }

    protected int setLieferantInactive(Integer lieferant_id) {
        // returns 0 if there was an error, otherwise number of rows affected
        // (>0)
        int result = 0;
        // check if there are still active articles with this lieferant
        if (thereAreActiveArticlesWithLieferant(lieferant_id)) {
            JOptionPane.showMessageDialog(this,
                    "Fehler: Es gibt noch aktive Artikel mit dem Lieferanten Nr. " + lieferant_id + ".", "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            return result;
        }
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("UPDATE lieferant SET aktiv = FALSE WHERE " + "lieferant_id = ?");
            pstmtSetInteger(pstmt, 1, lieferant_id);
            result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }

    protected int setLieferantActive(Integer lieferant_id) {
        // returns 0 if there was an error, otherwise number of rows affected
        // (>0)
        int result = 0;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("UPDATE lieferant SET aktiv = TRUE WHERE " + "lieferant_id = ?");
            pstmtSetInteger(pstmt, 1, lieferant_id);
            result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }

    int insertNewLieferant(String lieferantName, String lieferantKurzname) {
        // add row for new item (with updated fields)
        // returns 0 if there was an error, otherwise number of rows affected
        // (>0)
        int result = 0;
        if (isLieferantAlreadyKnown(lieferantName))
            return 0;

        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement("INSERT INTO lieferant SET " + "lieferant_name = ?, "
                    + "lieferant_kurzname = ?, " + "aktiv = TRUE");
            pstmt.setString(1, lieferantName);
            pstmt.setString(2, lieferantKurzname);
            result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }

    boolean isProdGrAlreadyKnown(String produktgruppe) {
        boolean exists = false;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("SELECT COUNT(*) > 0 FROM produktgruppe " + "WHERE produktgruppen_name = ?");
            pstmt.setString(1, produktgruppe);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            exists = rs.getBoolean(1);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return exists;
    }

    boolean isProdGrInactive(String produktgruppe) {
        boolean inactive = false;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("SELECT aktiv FROM produktgruppe " + "WHERE produktgruppen_name = ?");
            pstmt.setString(1, produktgruppe);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            inactive = !rs.getBoolean(1);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return inactive;
    }

    private int howManyActiveArticlesWithProduktgruppe(Integer produktgruppen_id) {
        int nArticles = 0;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("SELECT n_artikel FROM produktgruppe " + "WHERE produktgruppen_id = ? ");
            pstmtSetInteger(pstmt, 1, produktgruppen_id);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            nArticles = rs.getInt(1);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return nArticles;
    }

    private boolean thereAreActiveArticlesWithProduktgruppe(Integer produktgruppen_id) {
        return howManyActiveArticlesWithProduktgruppe(produktgruppen_id) > 0;
    }

    private int queryActiveArticlesWithProduktgruppe(Integer produktgruppen_id) {
        int nArticles = 0;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM artikel AS a " + "INNER JOIN produktgruppe USING (produktgruppen_id) "
                            + "WHERE a.produktgruppen_id = ? AND a.aktiv = TRUE");
            pstmtSetInteger(pstmt, 1, produktgruppen_id);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            nArticles = rs.getInt(1);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return nArticles;
    }

    private Vector<Integer> queryProdGrHierarchy(Integer produktgruppen_id) {
        Vector<Integer> ids = new Vector<Integer>();
        ids.add(null);
        ids.add(null);
        ids.add(null);
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT toplevel_id, sub_id, subsub_id FROM produktgruppe " + "WHERE produktgruppen_id = ?");
            pstmtSetInteger(pstmt, 1, produktgruppen_id);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            ids.set(0, rs.getString(1) == null ? null : rs.getInt(1));
            ids.set(1, rs.getString(2) == null ? null : rs.getInt(2));
            ids.set(2, rs.getString(3) == null ? null : rs.getInt(3));
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return ids;
    }

    private Integer queryProdGrID(Integer topid, Integer subid, Integer subsubid) {
        Integer produktgruppen_id = null;
        try {
            if (topid == null) {
                return produktgruppen_id;
            }
            String subid_str = (subid == null) ? "IS NULL" : "= ?";
            String subsubid_str = (subsubid == null) ? "IS NULL" : "= ?";
            //System.out.println(topid + " " + subid + " " + subsubid);
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement("SELECT produktgruppen_id FROM produktgruppe "
                    + "WHERE toplevel_id = ? AND sub_id " + subid_str + " AND subsub_id " + subsubid_str);
            int i = 1;
            pstmtSetInteger(pstmt, i, topid);
            i++;
            if (subid != null)
                pstmtSetInteger(pstmt, i, subid);
            i++;
            if (subsubid != null)
                pstmtSetInteger(pstmt, i, subsubid);
            i++;
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            produktgruppen_id = rs.getInt(1);
            //System.out.println(produktgruppen_id);
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return produktgruppen_id;
    }

    private int queryRecursiveActiveArticlesWithProduktgruppe(Integer produktgruppen_id) {
        int nArticles = 0;
        try {
            Vector<Integer> ids = queryProdGrHierarchy(produktgruppen_id);
            Integer topid = ids.get(0), subid = ids.get(1), subsubid = ids.get(2);

            String filter = "";
            if (topid == null)
                filter = "produktgruppe.toplevel_id > 0 ";
            else
                filter = "produktgruppe.toplevel_id = " + topid + " ";
            if (subid != null)
                filter += " AND produktgruppe.sub_id = " + subid + " ";
            if (subsubid != null)
                filter += " AND produktgruppe.subsub_id = " + subsubid + " ";

            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM artikel AS a "
                    + "INNER JOIN produktgruppe USING (produktgruppen_id) " + "WHERE a.aktiv = TRUE AND " + filter);
            rs.next();
            nArticles = rs.getInt(1);
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return nArticles;
    }

    private int setNArtikelForProduktgruppe(int prodGrID, int nArticles) {
        // returns 0 if there was an error, otherwise number of rows affected
        // (>0)
        int result = 0;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("UPDATE produktgruppe SET n_artikel = ? WHERE " + "produktgruppen_id = ?");
            pstmtSetInteger(pstmt, 1, nArticles);
            pstmtSetInteger(pstmt, 2, prodGrID);
            result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }

    private int setNArtikelRekursivForProduktgruppe(int prodGrID, int nArticles) {
        // returns 0 if there was an error, otherwise number of rows affected
        // (>0)
        int result = 0;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "UPDATE produktgruppe SET n_artikel_rekursiv = ? WHERE " + "produktgruppen_id = ?");
            pstmtSetInteger(pstmt, 1, nArticles);
            pstmtSetInteger(pstmt, 2, prodGrID);
            result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }

    private void updateNArtikelInProduktgruppeFor(int produktgruppen_id) {
        //System.out.print("Upating produktgruppen_id " + produktgruppen_id + " to n_artikel = ");
        int nArticles = queryActiveArticlesWithProduktgruppe(produktgruppen_id);
        //System.out.println(nArticles);
        int result = 0;
        result = setNArtikelForProduktgruppe(produktgruppen_id, nArticles);
        if (result == 0) {
            System.err.println("ERROR: Could not set `n_artikel` to " + nArticles + " for produktgruppen_id = "
                    + produktgruppen_id);
            JOptionPane.showMessageDialog(this, "Fehler: Produktgruppe mit ID " + produktgruppen_id
                    + ": `n_artikel` konnte nicht " + "auf " + nArticles + " gesetzt werden.", "Fehler",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    void updateNArtikelInProduktgruppe() {
        /**
         * For all rows in `produktgruppe` that have `n_artikel` = NULL, query
         * for the number of active articles and set `n_artikel`.
         */
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt
                    .executeQuery("SELECT produktgruppen_id FROM " + "produktgruppe WHERE n_artikel IS NULL");
            while (rs.next()) {
                int prodGrID = rs.getInt(1);
                updateNArtikelInProduktgruppeFor(prodGrID);
            }
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
    }

    private void updateNArtikelRekursivInProduktgruppeFor(int produktgruppen_id) {
        //System.out.print("Upating produktgruppen_id " + produktgruppen_id + " to n_artikel_rekursiv = ");
        int nArticles = queryRecursiveActiveArticlesWithProduktgruppe(produktgruppen_id);
        //System.out.println(nArticles);
        int result = 0;
        result = setNArtikelRekursivForProduktgruppe(produktgruppen_id, nArticles);
        if (result == 0) {
            System.err.println("ERROR: Could not set `n_artikel_rekursiv` to " + nArticles + " for produktgruppen_id = "
                    + produktgruppen_id);
            JOptionPane
                    .showMessageDialog(
                            this, "Fehler: Produktgruppe mit ID " + produktgruppen_id
                                    + ": `n_artikel_rekursiv` konnte nicht " + "auf " + nArticles + " gesetzt werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateNArtikelRekursivRecursivelyInProduktgruppeFor(int produktgruppen_id) {
        Vector<Integer> prodGrIDs = new Vector<Integer>();
        prodGrIDs.add(produktgruppen_id);

        Vector<Integer> ids = queryProdGrHierarchy(produktgruppen_id);
        Integer topid = ids.get(0), subid = ids.get(1), subsubid = ids.get(2);
        if (topid != null && subid != null && subsubid != null) {
            prodGrIDs.add(queryProdGrID(topid, subid, null));
        }
        if (topid != null && subid != null) {
            prodGrIDs.add(queryProdGrID(topid, null, null));
        }

        for (Integer id : prodGrIDs) {
            updateNArtikelRekursivInProduktgruppeFor(id);
        }
    }

    void updateNArtikelRekursivInProduktgruppe() {
        /**
         * For all rows in `produktgruppe` that have `n_artikel_rekursiv` =
         * NULL, query for the recursive number of active articles and set
         * `n_artikel_rekursiv`.
         */
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt
                    .executeQuery("SELECT produktgruppen_id FROM " + "produktgruppe WHERE n_artikel_rekursiv IS NULL");
            while (rs.next()) {
                int prodGrID = rs.getInt(1);
                updateNArtikelRekursivInProduktgruppeFor(prodGrID);
            }
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
    }

    int updateProdGr(Integer produktgruppen_id, String produktgruppen_name, Boolean aktiv) {
        // returns 0 if there was an error, otherwise number of rows affected
        // (>0)
        int result = 0;
        if (!aktiv) {
            // check if there are still active articles with this lieferant
            if (thereAreActiveArticlesWithProduktgruppe(produktgruppen_id)) {
                JOptionPane.showMessageDialog(this,
                        "Fehler: Es gibt noch aktive Artikel mit der Produktgruppe " + produktgruppen_name + ".",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                return result;
            }
        }
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "UPDATE produktgruppe SET produktgruppen_name = ?, aktiv = ? WHERE " + "produktgruppen_id = ?");
            pstmt.setString(1, produktgruppen_name);
            pstmt.setBoolean(2, aktiv);
            pstmtSetInteger(pstmt, 3, produktgruppen_id);
            result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }

    int updateProdGr(Integer produktgruppen_id, Integer topid, Integer subid, Integer subsubid,
                     String newName, Integer mwst_id, Integer pfand_id, Boolean aktiv) {
        // returns 0 if there was an error, otherwise number of rows affected
        // (>0)
        int result = 0;
        if (!aktiv) {
            // check if there are still active articles with this lieferant
            if (thereAreActiveArticlesWithProduktgruppe(produktgruppen_id)) {
                JOptionPane.showMessageDialog(this,
                        "Fehler: Es gibt noch aktive Artikel mit der Produktgruppe " + newName + ".", "Fehler",
                        JOptionPane.ERROR_MESSAGE);
                return result;
            }
        }
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("UPDATE produktgruppe SET toplevel_id = ?, sub_id = ?, subsub_id = ?, "
                            + "produktgruppen_name = ?, mwst_id = ?, pfand_id = ?, aktiv = ? WHERE "
                            + "produktgruppen_id = ?");
            pstmtSetInteger(pstmt, 1, topid);
            pstmtSetInteger(pstmt, 2, subid);
            pstmtSetInteger(pstmt, 3, subsubid);
            pstmt.setString(4, newName);
            pstmtSetInteger(pstmt, 5, mwst_id);
            pstmtSetInteger(pstmt, 6, pfand_id);
            pstmt.setBoolean(7, aktiv);
            pstmtSetInteger(pstmt, 8, produktgruppen_id);
            result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }

    protected int setProdGrInactive(Integer produktgruppen_id) {
        // returns 0 if there was an error, otherwise number of rows affected
        // (>0)
        int result = 0;
        // check if there are still active articles with this lieferant
        if (thereAreActiveArticlesWithProduktgruppe(produktgruppen_id)) {
            JOptionPane.showMessageDialog(this,
                    "Fehler: Es gibt noch aktive Artikel mit der Produktgruppe Nr. " + produktgruppen_id + ".",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return result;
        }
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("UPDATE produktgruppe SET aktiv = FALSE WHERE " + "produktgruppen_id = ?");
            pstmtSetInteger(pstmt, 1, produktgruppen_id);
            result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }

    protected int setProdGrActive(Integer produktgruppen_id) {
        // returns 0 if there was an error, otherwise number of rows affected
        // (>0)
        int result = 0;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("UPDATE produktgruppe SET aktiv = TRUE WHERE " + "produktgruppen_id = ?");
            pstmtSetInteger(pstmt, 1, produktgruppen_id);
            result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }

    int insertNewProdGr(Integer topid, Integer subid, Integer subsubid, String newName, Integer mwst_id,
                        Integer pfand_id) {
        // add row for new item (with updated fields)
        // returns 0 if there was an error, otherwise number of rows affected
        // (>0)
        int result = 0;
        if (isProdGrAlreadyKnown(newName))
            return 0;

        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection
                    .prepareStatement("INSERT INTO produktgruppe SET " + "toplevel_id = ?, sub_id = ?, subsub_id = ?, "
                            + "produktgruppen_name = ?, mwst_id = ?, pfand_id = ?, " + "aktiv = TRUE");
            pstmtSetInteger(pstmt, 1, topid);
            pstmtSetInteger(pstmt, 2, subid);
            pstmtSetInteger(pstmt, 3, subsubid);
            pstmt.setString(4, newName);
            pstmtSetInteger(pstmt, 5, mwst_id);
            pstmtSetInteger(pstmt, 6, pfand_id);
            result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }

    protected String tableForMode(String tableName) {
        return bc.operationMode.equals("normal") ? tableName : "training_"+tableName;
    }

    protected int insertIntoKassenstand(BigDecimal neuerKassenstand, Boolean entnahme, String kommentar) {
        int result = 0;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO "+tableForMode("kassenstand")+" SET buchungsdatum = NOW(), " +
                "neuer_kassenstand = ?, manuell = TRUE, " +
                "entnahme = ?, kommentar = ?"
            );
            pstmt.setBigDecimal(1, neuerKassenstand);
            pstmt.setBoolean(2, entnahme);
            pstmt.setString(3, kommentar);
            result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return result;
    }



    /**
     * Calendar methods
     */

    protected String now() {
        String date = "";
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT NOW()");
            rs.next(); date = rs.getString(1); rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return date;
    }

    protected Date nowDate() {
        Date date = new Date();
        try {
            date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(now());
        } catch (ParseException ex) {
            logger.error("Could not parse NOW() date from MySQL DB. Exception:", ex);
        }
        return date;
    }

    protected void setCalButtFromSpinner(SpinnerModel m, JCalendarButton b) {
        if (m instanceof SpinnerDateModel) {
            b.setTargetDate(((SpinnerDateModel) m).getDate());
        }
    }

    protected void setSpinnerFromCalButt(SpinnerModel m, JCalendarButton b, Date earliestDate, Date latestDate) {
        Date newDate = b.getTargetDate();
        if (earliestDate != null) {
            if (newDate.before(earliestDate)) {
                newDate = earliestDate;
                b.setTargetDate(newDate);
            }
        }
        if (latestDate != null) {
            if (newDate.after(latestDate)) {
                newDate = latestDate;
                b.setTargetDate(newDate);
            }
        }
        if (m instanceof SpinnerDateModel) {
            if (newDate != null) {
                ((SpinnerDateModel) m).setValue(newDate);
            }
        }
    }

    protected Vector<Object> setupDateChooser(String label, Date initDate, Date minDate, Date maxDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(minDate);
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH)-1);
        Date oneDayBeforeMinDate = cal.getTime();

        JLabel dateLabel = new JLabel(label);
        JSpinnerDateEditor sdEdit = new JSpinnerDateEditor();
        JDateChooser dateChooser = new JDateChooser((Date)initDate.clone(), null, sdEdit);
        //dateChooser.setMinSelectableDate((Date)minDate.clone());
        dateChooser.setMinSelectableDate((Date)oneDayBeforeMinDate.clone());
        dateChooser.setMaxSelectableDate((Date)maxDate.clone());
        dateChooser.setLocale(bc.myLocale);
        JSpinner dateSpinner = (JSpinner)sdEdit.getUiComponent();
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd.MM.yyyy"));
        // bringt nichts:
        //dateSpinner.setModel(new SpinnerDateModel(initDate, // Startwert
        //        oneDayBeforeMinDate, // kleinster Wert
        //        maxDate, // groesster Wert
        //        Calendar.YEAR));
        dateLabel.setLabelFor(dateChooser);
        Vector<Object> retVec = new Vector<Object>();
        retVec.add(dateLabel);
        retVec.add(dateChooser);
        retVec.add(dateSpinner);
        return retVec;
    }
}
