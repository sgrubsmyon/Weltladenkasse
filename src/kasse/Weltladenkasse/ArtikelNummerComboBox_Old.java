package Weltladenkasse;

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

// GUI stuff:
import javax.swing.DefaultComboBoxModel;

public class ArtikelNummerComboBox_Old extends AutoCompleteComboBox_Old {
    private String internalFilterStr = " AND (toplevel_id IS NOT NULL OR sub_id = 2) ";
              // show all 'normal' items (toplevel_id IS NOT NULL), and in addition Gutscheine (where toplevel_id is NULL and sub_id is 2)

    public ArtikelNummerComboBox_Old(Connection conn) {
        super(conn);
    }

    public void doMySQLQuery(String pattern){
        if ( searchCache.size() == 0 && pattern.length() >= 3 ){
            // cache the search results (do mysql query)
            try {
                PreparedStatement pstmt = this.conn.prepareStatement(
                        "SELECT artikel_nr FROM artikel AS a " +
                        "INNER JOIN produktgruppe AS p USING (produktgruppen_id) " +
                        "WHERE artikel_nr LIKE ? AND a.aktiv = TRUE " + internalFilterStr
                        );
                pstmt.setString(1, pattern.replaceAll(" ","%")+"%");
                ResultSet rs = pstmt.executeQuery();
                // Now do something with the ResultSet ...
                while (rs.next()) { searchCache.add(rs.getString(1)); }
                rs.close();
                pstmt.close();
            } catch (SQLException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
            Collections.sort(searchCache, new Comparator<String>() { // anonymous class for sorting alphabetically ignoring case
                public int compare(String str1, String str2){ return str1.compareToIgnoreCase(str2); }
            });
        }
        // fill combo box with new items
        Vector<String> newModelVector = new Vector<String>(searchCache);
        //        if (searchCache.size() != 1 || keepText) // wenn Ergebnis nicht eindeutig oder beim Löschen => behalte getippten Text bei
        //            newModelVector.add(0, typeText); // add typed text at front
        model = new DefaultComboBoxModel(newModelVector);
        selecting = true;
        setModel(model);
        selecting = false;
        //setText(newModelVector.firstElement());
        showPopup();
    }

}
