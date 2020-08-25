package org.weltladen_bonn.pos;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.*; // for BigDecimal and RoundingMode

// MySQL Connector/J stuff:
import java.sql.*; // Connection, Statement, ResultSet ...
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import java.awt.*;
import java.awt.event.*; // KeyEvent, KeyAdapter
import javax.swing.*; // JComponent, KeyStroke

public abstract class ArtikelGrundlage extends WindowContent {
    protected final RemoveNumPadAdapter removeNumPadAdapter = new RemoveNumPadAdapter();

    /**
     *    The constructor.
     *       */
    public ArtikelGrundlage(MariaDbPoolDataSource pool, MainWindowGrundlage mw) {
	    super(pool, mw);
    }

    protected void removeDefaultKeyBindings(JComponent field) {
        field.getInputMap().put(KeyStroke.getKeyStroke("ctrl H"), "none");
        field.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), "none");
    }

    protected void removeDefaultKeyBindings(JComponent field, int condition) {
        field.getInputMap(condition).put(KeyStroke.getKeyStroke("ctrl H"), "none");
        field.getInputMap(condition).put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), "none");
    }

    private class RemoveNumPadAdapter extends KeyAdapter {
        int pressedKeyCode = -1;

        @Override
        public void keyPressed(KeyEvent e) {
            pressedKeyCode = e.getKeyCode();
        }

        @Override
        public void keyTyped(KeyEvent e) {
            if (pressedKeyCode == KeyEvent.VK_ADD ||
                    pressedKeyCode == KeyEvent.VK_SUBTRACT ||
                    pressedKeyCode == KeyEvent.VK_MULTIPLY ||
                    pressedKeyCode == KeyEvent.VK_DIVIDE) {
                e.consume(); // ignore event
            }
        }
    }

    //////////////////////////////////
    // DB query functions:
    //////////////////////////////////
    public int getArticleID(String lieferant, String artikelNummer) {
        // get artikelID for lieferant and artikelNummer
        int artikelID = -1;
        String lieferantQuery = lieferant.equals("") ? "IS NULL" : "= ?";
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
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

    public int getArticleID(Integer lieferant_id, String artikelNummer) {
        // get artikelID for lieferant and artikelNummer
        int artikelID = -1;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT a.artikel_id FROM artikel AS a " +
                    "WHERE a.lieferant_id = ? "+
                    "AND a.artikel_nr = ? " +
                    "AND a.aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, lieferant_id);
            pstmt.setString(2, artikelNummer);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); artikelID = rs.getInt(1); rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return artikelID;
    }

    public String[] getArticleName(int artikelID) {
        String artikelName = new String();
        String lieferant = new String();
        Boolean sortiment = false;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
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

    public String[] getArticleNumber(int artikelID) {
        String artikelNumber = new String();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
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

    public String getShortName(int artikelID) {
        String kurzname = new String();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT IFNULL(kurzname, artikel_name) FROM artikel WHERE artikel_id = ? " +
                    "AND aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); kurzname = rs.getString(1); rs.close();
            if ( kurzname.equals("") ){
                pstmt = connection.prepareStatement(
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

    public String getShortName(Artikel a) {
        String kurzname = a.getKurzname();
        if ( kurzname == null || kurzname.equals("") ){
            kurzname = a.getName();
        }
        if ( kurzname == null ){
            kurzname = "";
        }
        return kurzname;
    }

    public String getLieferantName(int artikelID) {
        String lieferant = new String();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT lieferant_name "+
                    "FROM lieferant AS l INNER JOIN artikel AS a USING (lieferant_id) WHERE artikel_id = ? "+
                    "AND a.aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); lieferant = rs.getString(1); rs.close();
            if ( lieferant == null ) { lieferant = ""; }
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return lieferant;
    }

    public String getShortLieferantName(int artikelID) {
        String liefkurz = new String();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT IFNULL(lieferant_kurzname, lieferant_name) "+
                    "FROM lieferant AS l INNER JOIN artikel AS a USING (lieferant_id) WHERE artikel_id = ? "+
                    "AND a.aktiv = TRUE"
                    );
            pstmtSetInteger(pstmt, 1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); liefkurz = rs.getString(1); rs.close();
            if ( liefkurz.equals("") ){
                pstmt = connection.prepareStatement(
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

    public String getBarcode(int artikelID) {
        String barcode = new String();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
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

    public boolean getVariablePriceBool(int artikelID) {
        // is price variable for artikelID?
        boolean variabel = false;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
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

    public String getSalePrice(int artikelID) {
        /** returns vk_preis from DB, this is price per article */
        String price = "";
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
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

    public String getRecSalePrice(int artikelID) {
        /** returns empf_vk_preis from DB, this is price per set */
        String price = "";
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
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

    public String getVAT(int artikelID) {
        // get VAT rate for artikelID
        String vat = "";
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
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
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
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
        return calculatePricePerKg(menge_bd, einheit, preis_bd);
    }

    protected String[] getMengePriceAndPricePerKg(Artikel a) {
        return calculatePricePerKg(a.getMenge(), a.getEinheit(),
                new BigDecimal(a.getVKP()));
    }

    private String[] calculatePricePerKg(BigDecimal menge_bd, String einheit, BigDecimal preis_bd) {
        String menge = "";
        String preis = bc.priceFormatter(preis_bd)+" "+bc.currencySymbol;
        String kg_preis = "";
        try {
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
        } catch (NullPointerException ex) {
            System.out.println("Either menge_bd ("+menge_bd+") or einheit ("+einheit+") is null for this article.");
        }
        return new String[]{menge, preis, kg_preis};
    }

    public String getVPE(int artikelID) {
        String vpe = "";
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
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

    public int getSetSize(int artikelID) {
        // is price variable for artikelID?
        int setgroesse = 1;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
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

    public Boolean getSortimentBool(int artikelID) {
        Boolean sortimentBool = false;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
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
