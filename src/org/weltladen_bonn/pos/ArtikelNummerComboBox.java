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
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import javax.swing.JOptionPane;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ArtikelNummerComboBox extends IncrementalSearchComboBox {
    private static final Logger logger = LogManager.getLogger(ArtikelNummerComboBox.class);

    // private Connection conn; // connection to MySQL database
    private MariaDbPoolDataSource pool; // pool of connections to MySQL database

    public ArtikelNummerComboBox(MariaDbPoolDataSource pool, String fstr) {
        super(fstr);
        this.pool = pool;
    }

    public Vector<String[]> doQuery() {
        Vector<String[]> searchResults = new Vector<String[]>();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT DISTINCT artikel_nr FROM artikel AS a " +
                    "INNER JOIN produktgruppe AS p USING (produktgruppen_id) " +
                    "WHERE artikel_nr LIKE ? AND a.aktiv = TRUE " + filterStr +
                    "ORDER BY artikel_nr"
                    );
            pstmt.setString(1, "%"+textFeld.getText().replaceAll(" ","%")+"%");
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet ...
            while (rs.next()) { 
                searchResults.add(new String[]{rs.getString(1)});
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            JOptionPane.showMessageDialog(this,
                "Verbindung zum Datenbank-Server unterbrochen?\n"+
                "Fehlermeldung: "+ex.getMessage(),
                "Fehler", JOptionPane.ERROR_MESSAGE);
        }
        // sort the results
        //Collections.sort(searchResults, new Comparator<String[]>() { // anonymous class for sorting alphabetically ignoring case
        //    public int compare(String[] str1, String[] str2){ return str1[0].compareToIgnoreCase(str2[0]); }
        //});
        return searchResults;
    }

}

