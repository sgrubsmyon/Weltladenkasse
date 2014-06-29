package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector

// MySQL Connector/J stuff:
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class ArtikelGrundlage extends WindowContent {
    /**
     *    The constructor.
     *       */
    public ArtikelGrundlage(Connection conn, MainWindowGrundlage mw)
    {
	super(conn, mw);
    }

    //////////////////////////////////
    // DB query functions:
    //////////////////////////////////
    public int getArticleID(String artikelName, String lieferant, String artikelNummer) {
        // get artikelID for artikelName and artikelNummer
        int artikelID = -1;
        String lieferantQuery = lieferant.equals("") ? "IS NULL" : "= ?";
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT a.artikel_id FROM artikel AS a " +
                    "LEFT JOIN lieferant AS l USING (lieferant_id) " +
                    "WHERE a.artikel_name = ? " +
                    "AND l.lieferant_name "+lieferantQuery+" " +
                    "AND a.artikel_nr = ? " +
                    "AND a.aktiv = TRUE"
                    );
            pstmt.setString(1, artikelName);
            int artikelNrIndex = 2;
            if (!lieferant.equals("")){
                pstmt.setString(2, lieferant);
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
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT a.artikel_name, l.lieferant_name FROM artikel AS a " +
                    "LEFT JOIN lieferant AS l USING (lieferant_id) " +
                    "WHERE a.artikel_id = ? " +
                    "AND a.aktiv = TRUE"
                    );
            pstmt.setInt(1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            artikelName = rs.getString(1);
            lieferant = rs.getString(2) != null ? rs.getString(2) : "";
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return new String[]{artikelName, lieferant};
    }

    protected String[] getArticleNumber(int artikelID) {
        String artikelNumber = new String();
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT artikel_nr FROM artikel WHERE artikel_id = ? " +
                    "AND aktiv = TRUE"
                    );
            pstmt.setInt(1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); artikelNumber = rs.getString(1); rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return new String[]{artikelNumber};
    }

    protected boolean getVariablePriceBool(int artikelID) {
        // is price variable for artikelID?
        boolean variabel = false;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT variabler_preis FROM artikel WHERE artikel_id = ?"
                    );
            pstmt.setInt(1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); variabel = ( rs.getInt(1) != 0 ); rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return variabel;
    }

    protected String getPrice(int artikelID) {
        // get price from DB
        String price = "";
        try {
            // return regular price:
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT vk_preis FROM artikel WHERE artikel_id = ?"
                    );
            pstmt.setInt(1, artikelID);
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
            pstmt.setInt(1, artikelID);
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

    protected String getVPE(int artikelID) {
        String vpe = "";
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT vpe FROM artikel WHERE artikel_id = ?"
                    );
            pstmt.setInt(1, artikelID);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            vpe = rs.getString(1) != null ? rs.getString(1) : "";
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return vpe;
    }
}
