package WeltladenDB;

// Basic Java stuff:
import java.io.*; // for InputStream
import java.util.*; // for Vector, Collections
import java.util.Date; // because Date alone ambiguous due to java.sql.Date
import java.math.*; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.*;

// GUI stuff:
import java.awt.event.*;
import java.awt.*;

import javax.swing.*;
import javax.swing.text.*; // for DocumentFilter
import javax.swing.table.*;

// DateTime from date4j (http://www.date4j.net/javadoc/index.html)
import hirondelle.date4j.DateTime;

// JCalendarButton
import jcalendarbutton.org.JCalendarButton;

public abstract class WindowContent extends JPanel implements ActionListener {

    // mySQL Connection:
    protected Connection conn = null;
    protected MainWindowGrundlage mainWindow = null;
    protected BaseClass bc = null;

    // Die Ausrichter:
    protected DefaultTableCellRenderer rechtsAusrichter = new DefaultTableCellRenderer();
    protected DefaultTableCellRenderer linksAusrichter = new DefaultTableCellRenderer();
    protected DefaultTableCellRenderer zentralAusrichter = new DefaultTableCellRenderer();
    protected final int columnMargin = 20; /** number of pixels of space between table columns */
    protected final int minColumnWidth = 20; /** minimally allowed pixel width of table columns */
    protected final int maxColumnWidth = 150; /** maximally allowed pixel width of table columns */

    protected PositiveNumberDocumentFilter geldFilter = new PositiveNumberDocumentFilter(2, 13);
    protected PositiveNumberDocumentFilter relFilter = new PositiveNumberDocumentFilter(3, 6);
    protected PositiveNumberDocumentFilter mengeFilter = new PositiveNumberDocumentFilter(5, 8);
    protected StringDocumentFilter einheitFilter = new StringDocumentFilter(10);
    protected StringDocumentFilter nummerFilter = new StringDocumentFilter(30);
    protected StringDocumentFilter nameFilter = new StringDocumentFilter(180);
    protected StringDocumentFilter kurznameFilter = new StringDocumentFilter(50);
    protected StringDocumentFilter herkunftFilter = new StringDocumentFilter(100);
    protected IntegerDocumentFilter intFilter;
    protected IntegerDocumentFilter vpeFilter;
    protected IntegerDocumentFilter beliebtFilter;

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
        this.bc = mw.bc;
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
	this.mainWindow = mw;
        this.bc = mw.bc;

        intFilter = new IntegerDocumentFilter(-bc.smallintMax, bc.smallintMax, this);
        vpeFilter = new IntegerDocumentFilter(1, bc.smallintMax, this);

	rechtsAusrichter.setHorizontalAlignment(JLabel.RIGHT);
	linksAusrichter.setHorizontalAlignment(JLabel.LEFT);
	zentralAusrichter.setHorizontalAlignment(JLabel.CENTER);
	this.setLayout(new BorderLayout());

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
        Integer minBeliebt = Collections.min(beliebtWerte);
        Integer maxBeliebt = Collections.max(beliebtWerte);
        beliebtFilter = new IntegerDocumentFilter(minBeliebt, maxBeliebt, this);
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
                path = dir.substring(0, dir.length()-1)+bc.fileSep+program+"\"";
            } else {
                path = dir+bc.fileSep+program;
            }
        }
        return path;
    }

    protected void applyFilter(String filterStr, Vector< Vector<Object> > data, Vector<Integer> indices) {
        /** Filter all rows in `data` using the filter strings given in `filterStr`, which
         *  are space separated. Each filter string must be present (case insensitive).
         *  This behaviour is the same as an SQL query using 'foo LIKE "%str1%" AND foo LIKE "%str2%" AND ...'.
         *  Returns only the rows of `data` and `indices` that correspond to rows in `data`
         *  containing all the filter strings. */
        if (filterStr.length() < 3){
            return;
        }
        // Search in each row
        final Vector< Vector<Object> > fullData = new Vector< Vector<Object> >(data);
        final Vector<Integer> fullIndices = new Vector<Integer>(indices);
        for (int i=0; i<fullData.size(); i++){
            boolean contains = true;
            String row = "";
            for ( Object obj : fullData.get(i) ){
                // omit UI components:
                if ( !(obj instanceof JComponent) ){
                    String str = obj.toString().toLowerCase();
                    row = row.concat(str+" ");
                }
            }
            // row must contain (somewhere) each whitespace separated filter word
            for ( String fstr : filterStr.split(" ") ){
                if ( fstr.equals("") )
                    continue;
                if ( ! row.contains(fstr.toLowerCase()) ){
                    contains = false;
                    break;
                }
            }
            if (!contains){
                int display_index = indices.indexOf( fullIndices.get(i) );
                data.remove(display_index);
                indices.remove(display_index);
            }
        }
    }

    protected String handleMissingSalePrice(String title) {
        JLabel label = new JLabel("Wie viel kostet dieser Artikel?");

        JPanel preisPanel = new JPanel();
        JTextField preisField = new JTextField("");
        ((AbstractDocument)preisField.getDocument()).setDocumentFilter(geldFilter);
        preisField.setColumns(6);
        preisField.setHorizontalAlignment(JTextField.RIGHT);
        preisPanel.add(preisField);
        preisPanel.add(new JLabel(bc.currencySymbol));

        JOptionPane jop = new JOptionPane(new Object[]{label, preisPanel},
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = jop.createDialog(title);
        dialog.setVisible(true);
        int result = (Integer)jop.getValue();
        dialog.dispose();

        String artikelPreis = "";
        if (result == JOptionPane.OK_OPTION){
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
        //return totalPrice.multiply( one.subtract( one.divide(one.add(mwst), 10, RoundingMode.HALF_UP) ) ); // VAT = bruttoPreis * ( 1. - 1./(1.+mwst) );
        return totalPrice.divide(bc.one.add(mwst), 10, RoundingMode.HALF_UP).multiply(mwst); // VAT = bruttoPreis / (1.+mwst) * mwst;
    }

    protected BigDecimal calculateEKP(BigDecimal empfVKPreis, BigDecimal ekRabatt) {
        return ( bc.one.subtract(ekRabatt) ).multiply(empfVKPreis); // Einkaufspreis = (1 - rabatt) * Empf. VK-Preis
    }

    protected BigDecimal calculateEKP(String empfVKPreis, BigDecimal ekRabatt) {
        BigDecimal empfvkpDecimal;
        try {
            empfvkpDecimal = new BigDecimal( bc.priceFormatterIntern(empfVKPreis) );
        } catch (NumberFormatException ex) {
            return null;
        }
        return calculateEKP(empfvkpDecimal, ekRabatt);
    }

    protected BigDecimal calculateEKP(String empfVKPreis, String ekRabatt) {
        BigDecimal rabatt;
        try {
            rabatt = new BigDecimal( bc.vatParser(ekRabatt) );
        } catch (NumberFormatException ex) {
            return null;
        }
        return calculateEKP(empfVKPreis, rabatt);
    }

    protected String figureOutEKP(String empfVKPreis, String ekRabatt, String ekPreis) {
        /** If empfVKPreis and ekRabatt are both valid numbers, calculate EK-Preis
         *  from them and return it. Otherwise, fall back to using ekPreis.
         */
        BigDecimal preis = calculateEKP(empfVKPreis, ekRabatt);
        if (preis != null){
            return bc.priceFormatterIntern(preis);
        } else {
            return bc.priceFormatterIntern(ekPreis);
        }
    }

    protected boolean empfVKPAndEKRabattValid(String empfVKPreis, String ekRabatt) {
        /** If empfVKPreis and ekRabatt are both valid numbers, return true, else false.
         */
        try {
            new BigDecimal( bc.priceFormatterIntern(empfVKPreis) );
        } catch (NumberFormatException ex) {
            return false;
        }
        try {
            new BigDecimal( bc.vatParser(ekRabatt) );
        } catch (NumberFormatException ex) {
            return false;
        }
        return true;
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

    protected boolean isItemAlreadyKnown(Integer lieferant_id, String nummer) {
        boolean exists = false;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT COUNT(artikel_id) > 0 FROM artikel "+
                    "WHERE lieferant_id = ? AND artikel_nr = ? AND artikel.aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, lieferant_id);
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

    protected int insertNewItem(Artikel a) {
        // add row for new item (with updated fields)
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;

        if (a.getKurzname().equals("") || a.getKurzname().equals("NULL")){
            a.setKurzname(null);
        }
        if (a.getEinheit().equals("") || a.getEinheit().equals("NULL")){
            a.setEinheit(null);
        }
        if (a.getBarcode().equals("") || a.getBarcode().equals("NULL")){
            a.setBarcode(null);
        }
        if (a.getHerkunft().equals("") || a.getHerkunft().equals("NULL")){
            a.setHerkunft(null);
        }
        if (a.getVPE() == null || a.getVPE().equals(0)){
            a.setVPE(null);
        }
        if (a.getSetgroesse() == null || a.getSetgroesse().equals(0)){
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
            ekrabattDecimal = new BigDecimal( bc.vatParser(a.getEKRabatt()) );
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
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "INSERT INTO artikel SET "+
                    "produktgruppen_id = ?, lieferant_id = ?, "+
                    "artikel_nr = ?, artikel_name = ?, "+
                    "kurzname = ?, "+
                    "menge = ?, "+
                    "einheit = ?, "+
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
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }


    private String selectArticleString() {
        return "SELECT "+
            "produktgruppen_id, "+
            "lieferant_id, "+
            "artikel_nr, "+
            "artikel_name, "+
            "kurzname, "+
            "menge, "+
            "einheit, "+
            "barcode, "+
            "herkunft, "+
            "vpe, "+
            "setgroesse, "+
            "vk_preis, empf_vk_preis, "+
            "ek_rabatt, ek_preis, "+
            "variabler_preis, sortiment, "+
            "lieferbar, beliebtheit, "+
            "bestand ";
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
            a.setEKRabatt( bc.vatFormatter(rs.getString(14)) );
            a.setEKP(var ? "" : rs.getString(15));
            a.setVarPreis(var);
            a.setSortiment(rs.getBoolean(17));
            a.setLieferbar(rs.getBoolean(18));
            a.setBeliebt(rs.getInt(19));
            a.setBestand(rs.getInt(20));
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return a;
    }

    protected Artikel getArticle(Integer artikel_id) {
        Artikel a = new Artikel(bc);
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    selectArticleString()+
                    "FROM artikel "+
                    "WHERE artikel_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, artikel_id);
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result ...
            a = parseArticleResultSet(rs);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return a;
    }


    protected Artikel getArticle(Integer lieferant_id, String artikel_nr) {
        Artikel a = new Artikel(bc);
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    selectArticleString()+
                    "FROM artikel "+
                    "WHERE lieferant_id = ? AND "+
                    "artikel_nr = ? AND artikel.aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, lieferant_id);
            pstmt.setString(2, artikel_nr);
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result ...
            a = parseArticleResultSet(rs);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return a;
    }

    protected String getProduktgruppe(Integer produktgruppen_id) {
        String produktgruppe = "";
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT produktgruppen_name FROM produktgruppe "+
                    "WHERE produktgruppen_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, produktgruppen_id);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            produktgruppe = rs.getString(1);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return produktgruppe;
    }

    protected String getLieferant(Integer lieferant_id) {
        String lieferant = "";
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT lieferant_name FROM lieferant "+
                    "WHERE lieferant_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, lieferant_id);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            lieferant = rs.getString(1);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return lieferant;
    }

    protected Integer getLieferantID(String lieferant) {
        Integer lieferant_id = 1;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT lieferant_id FROM lieferant "+
                    "WHERE lieferant_name = ?"
                    );
            pstmt.setString(1, lieferant);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            lieferant_id = rs.getInt(1);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return lieferant_id;
    }

    protected Vector<Object> getLieferantIDArtikelNummer(Integer artikel_id) {
        Vector<Object> liefIDAndNr = new Vector<Object>();
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT lieferant_id, artikel_nr FROM artikel "+
                    "WHERE artikel_id = ? AND artikel.aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, artikel_id);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            Integer lieferant_id = rs.getInt(1);
            String artikel_nr = rs.getString(2);
            liefIDAndNr.add(lieferant_id);
            liefIDAndNr.add(artikel_nr);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return liefIDAndNr;
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

    protected void updateArticle(Artikel oldArticle, Artikel newArticle) {
        // set old item to inactive:
        int result = setItemInactive(oldArticle.getLiefID(), oldArticle.getNummer());
        if (result == 0){
            JOptionPane.showMessageDialog(this,
                    "Fehler: Artikel von "+getLieferant( oldArticle.getLiefID() )+" mit "+
                    "Nummer "+oldArticle.getNummer()+" konnte nicht "+
                    "geändert werden.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if ( newArticle.getAktiv() == true ){ // only if the item wasn't
            // set inactive voluntarily: add new item with new properties
            String ekpreis = figureOutEKP(newArticle.getEmpfVKP(),
                    newArticle.getEKRabatt(), newArticle.getEKP());
            newArticle.setEKP(ekpreis);
            result = insertNewItem(newArticle);
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Artikel von "+getLieferant( oldArticle.getLiefID() )+
                        "mit Nummer "+oldArticle.getNummer()+" konnte "+
                        "nicht geändert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                result = setItemActive(oldArticle.getLiefID(), oldArticle.getNummer());
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel von "+
                            getLieferant( oldArticle.getLiefID() )+" mit Nummer "+
                            oldArticle.getNummer()+" konnte nicht "+
                            "wieder hergestellt werden. Artikel ist nun "+
                            "gelöscht (inaktiv).",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    protected int updateLieferant(Integer lieferant_id, String lieferant_name,
            String lieferant_kurzname, Boolean aktiv) {
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
                    "UPDATE lieferant SET lieferant_name = ?, lieferant_kurzname = ?, aktiv = ? "+
                    "WHERE lieferant_id = ?"
                    );
            pstmt.setString(1, lieferant_name);
            pstmt.setString(2, lieferant_kurzname);
            pstmt.setBoolean(3, aktiv);
            pstmt.setInt(4, lieferant_id);
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

    protected int insertNewLieferant(String lieferantName, String lieferantKurzname) {
        // add row for new item (with updated fields)
        // returns 0 if there was an error, otherwise number of rows affected (>0)
        int result = 0;
        if ( isLieferantAlreadyKnown(lieferantName) ) return 0;

        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "INSERT INTO lieferant SET "+
                    "lieferant_name = ?, "+
                    "lieferant_kurzname = ?, "+
                    "aktiv = TRUE"
                    );
            pstmt.setString(1, lieferantName);
            pstmt.setString(2, lieferantKurzname);
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
