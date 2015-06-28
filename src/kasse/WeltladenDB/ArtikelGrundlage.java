package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.*; // for BigDecimal and RoundingMode

// MySQL Connector/J stuff:
import java.sql.*; // Connection, Statement, ResultSet ...

public abstract class ArtikelGrundlage extends WindowContent {
    /**
     *    The constructor.
     *       */
    public ArtikelGrundlage(Connection conn, MainWindowGrundlage mw) {
	super(conn, mw);
    }

    //////////////////////////////////
    // DB query functions:
    //////////////////////////////////
    public int getArticleID(String lieferant, String artikelNummer) {
        // get artikelID for lieferant and artikelNummer
        int artikelID = -1;
        String lieferantQuery = lieferant.equals("") ? "IS NULL" : "= ?";
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT a.artikel_id FROM artikel AS a " +
                    "LEFT JOIN lieferant AS l USING (lieferant_id) " +
                    "WHERE l.lieferant_name "+lieferantQuery+" " +
                    "AND a.artikel_nr = ? " +
                    "AND a.aktiv = TRUE"
                    );
            int artikelNrIndex = 1;
            if (!lieferant.equals("")){
                pstmt.setString(artikelNrIndex, lieferant);
                artikelNrIndex++;
            }
            pstmt.setString(artikelNrIndex, artikelNummer);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); artikelID = rs.getInt(1); rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return artikelID;
    }

    protected String[] getArticleName(int artikelID) {
        String artikelName = new String();
        String lieferant = new String();
        Boolean sortiment = false;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT a.artikel_name, l.lieferant_name, a.sortiment FROM artikel AS a " +
                    "LEFT JOIN lieferant AS l USING (lieferant_id) " +
                    "WHERE a.artikel_id = ? " +
                    "AND a.aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            artikelName = rs.getString(1);
            lieferant = rs.getString(2) != null ? rs.getString(2) : "";
            sortiment = rs.getBoolean(3);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return new String[]{artikelName, lieferant, sortiment.toString()};
    }

    protected String[] getArticleNumber(int artikelID) {
        String artikelNumber = new String();
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT artikel_nr FROM artikel WHERE artikel_id = ? " +
                    "AND aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); artikelNumber = rs.getString(1); rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return new String[]{artikelNumber};
    }

    protected String getShortName(int artikelID) {
        String kurzname = new String();
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT IFNULL(kurzname, artikel_name) FROM artikel WHERE artikel_id = ? " +
                    "AND aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); kurzname = rs.getString(1); rs.close();
            if ( kurzname.equals("") ){
                pstmt = this.conn.prepareStatement(
                        "SELECT artikel_name FROM artikel WHERE artikel_id = ? " +
                        "AND aktiv = TRUE"
                        );
                pstmtSetInteger(pstmt, 1, artikelID);
                rs = pstmt.executeQuery();
                rs.next(); kurzname = rs.getString(1); rs.close();
            }
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return kurzname;
    }

    protected String getShortLieferantName(int artikelID) {
        String liefkurz = new String();
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT IFNULL(lieferant_kurzname, lieferant_name) "+
                    "FROM lieferant AS l INNER JOIN artikel AS a USING (lieferant_id) WHERE artikel_id = ? "+
                    "AND a.aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); liefkurz = rs.getString(1); rs.close();
            if ( liefkurz.equals("") ){
                pstmt = this.conn.prepareStatement(
                    "SELECT lieferant_name "+
                    "FROM lieferant AS l INNER JOIN artikel AS a USING (lieferant_id) WHERE artikel_id = ? "+
                    "AND a.aktiv = TRUE"
                        );
                pstmtSetInteger(pstmt, 1, artikelID);
                rs = pstmt.executeQuery();
                rs.next(); liefkurz = rs.getString(1); rs.close();
            }
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return liefkurz;
    }

    protected String getBarcode(int artikelID) {
        String barcode = new String();
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT barcode FROM artikel WHERE artikel_id = ? " +
                    "AND aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); barcode = rs.getString(1); rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return barcode;
    }

    protected boolean getVariablePriceBool(int artikelID) {
        // is price variable for artikelID?
        boolean variabel = false;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT variabler_preis FROM artikel WHERE artikel_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); variabel = ( rs.getInt(1) != 0 ); rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return variabel;
    }

    protected String getSalePrice(int artikelID) {
        /** returns vk_preis from DB, this is price per article */
        String price = "";
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT vk_preis FROM artikel WHERE artikel_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); price = rs.getString(1); rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return price;
    }

    protected String getRecSalePrice(int artikelID) {
        /** returns empf_vk_preis from DB, this is price per set */
        String price = "";
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT empf_vk_preis FROM artikel WHERE artikel_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); price = rs.getString(1); rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return price;
    }

    protected String getVAT(int artikelID) {
        // get VAT rate for artikelID
        String vat = "";
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT mwst.mwst_satz FROM mwst INNER JOIN produktgruppe USING (mwst_id) "+
                    "INNER JOIN artikel USING (produktgruppen_id) WHERE artikel.artikel_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result ...
            rs.next(); vat = rs.getString(1); rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return vat;
    }

    protected String[] getMengePriceAndPricePerKg(int artikelID) {
        BigDecimal menge_bd = new BigDecimal("0");
        String einheit = new String("");
        BigDecimal preis_bd = new BigDecimal("0");
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT menge, einheit, vk_preis FROM artikel WHERE artikel_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            if ( rs.next() ){
                menge_bd = rs.getString(1) != null ? rs.getBigDecimal(1) : new BigDecimal("0");
                einheit = rs.getString(2) != null ? rs.getString(2) : new String("");
                preis_bd = rs.getString(3) != null ? rs.getBigDecimal(3) : new BigDecimal("0");
            }
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }

        String menge = "";
        String preis = bc.priceFormatter(preis_bd)+" "+bc.currencySymbol;
        String kg_preis = "";
        if (menge_bd.signum() > 0){
            BigDecimal preis_pro_kg = preis_bd.divide(menge_bd, 10, RoundingMode.HALF_UP);
            kg_preis = bc.priceFormatter(preis_pro_kg)+" "+bc.currencySymbol;
            if ( einheit.equals("kg") || einheit.equals("l") ){
                if ( menge_bd.compareTo(bc.one) < 0 ){ // if menge < 1 kg or 1 l
                    menge_bd = menge_bd.multiply(bc.thousand);
                    if ( einheit.equals("kg") )
                        einheit = "g";
                    else if ( einheit.equals("l") )
                        einheit = "ml";
                }
            }
            menge = (bc.unifyDecimal(menge_bd)+" "+einheit).trim();
        }
        return new String[]{menge, preis, kg_preis};
    }

    protected String getVPE(int artikelID) {
        String vpe = "";
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT vpe FROM artikel WHERE artikel_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            if ( rs.next() ){
                vpe = rs.getString(1) != null ? rs.getString(1) : "";
            }
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return vpe;
    }

    protected int getSetSize(int artikelID) {
        // is price variable for artikelID?
        int setgroesse = 1;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT setgroesse FROM artikel WHERE artikel_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); setgroesse = rs.getInt(1); rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return setgroesse;
    }

    protected Boolean getSortimentBool(int artikelID) {
        Boolean sortimentBool = false;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT sortiment FROM artikel "+
                    "WHERE artikel_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result ...
            rs.next(); sortimentBool = rs.getBoolean(1); rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return sortimentBool;
    }
}
