package org.weltladen_bonn.pos;

// basic Java stuff:
import java.util.Vector;
import java.util.Collections;
import java.util.Comparator;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BarcodeComboBox extends IncrementalSearchComboBox {
    private Connection conn; // connection to MySQL database

    public BarcodeComboBox(Connection conn, String fstr) {
        super(fstr);
        this.conn = conn;
    }

    public Vector<String[]> doQuery() {
        Vector<String[]> searchResults = new Vector<String[]>();
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT DISTINCT barcode FROM artikel AS a " +
                    "INNER JOIN produktgruppe AS p USING (produktgruppen_id) " +
                    "WHERE barcode LIKE ? AND a.aktiv = TRUE " + filterStr +
                    "ORDER BY barcode"
                    );
            pstmt.setString(1, "%"+textFeld.getText()+"%");
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet ...
            while (rs.next()) {
                searchResults.add(new String[]{rs.getString(1)});
            }
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        // sort the results
        //Collections.sort(searchResults, new Comparator<String[]>() { // anonymous class for sorting alphabetically ignoring case
        //    public int compare(String[] str1, String[] str2){ return str1[0].compareToIgnoreCase(str2[0]); }
        //});
        return searchResults;
    }

}
