package WeltladenDB;

// basic Java stuff:
import java.util.Vector;
import java.util.Collections;
import java.util.Comparator;

import java.lang.ArrayIndexOutOfBoundsException;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

// GUI stuff:
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class ArtikelNameComboBox extends IncrementalSearchComboBox {
    private Connection conn; // connection to MySQL database

    public ArtikelNameComboBox(Connection conn, String fstr) {
        super(fstr);
        this.setRenderer(new MultiColRenderer());
        this.conn = conn;
    }

    public String[] parseArtikelName() {
        String[] item = this.items.get(this.getSelectedIndex());
        return new String[]{( item[0] ).replaceAll("\'","\\\\\'"),
            ( item[1] ).replaceAll("\'","\\\\\'")};
    }

    public Vector<String[]> doQuery() {
        Vector<String[]> searchResults = new Vector<String[]>();
        try {
            // Create statement for MySQL database
            Statement stmt = this.conn.createStatement();
            // Run MySQL command
            ResultSet rs = stmt.executeQuery(
                    "SELECT DISTINCT a.artikel_name, l.lieferant_name FROM artikel AS a " +
                    "LEFT JOIN produktgruppe AS p USING (produktgruppen_id) " +
                    "LEFT JOIN lieferant AS l USING (lieferant_id) " +
                    "WHERE artikel_name LIKE '%"+textFeld.getText().replaceAll("\'","\\\\\'")+"%' AND a.aktiv = TRUE " + filterStr +
                    "ORDER BY a.artikel_name, l.lieferant_name"
                    );
            // Now do something with the ResultSet ...
            while (rs.next()) { 
                String artName = rs.getString(1);
                String liefName = rs.getString(2) != null ? rs.getString(2) : "";
                searchResults.add(new String[]{artName, liefName});
            }
            rs.close();
            stmt.close();
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

    // based on: http://www.coderanch.com/t/340213/GUI/java/Multiple-Columns-JCombobox
    class MultiColRenderer extends JPanel implements ListCellRenderer {
        JLabel[] column = new JLabel[2];
        public MultiColRenderer() {
            setLayout(new GridLayout(0,2));
            for (int i=0; i<column.length; i++){
                column[i] = new JLabel();
                //column[i].setOpaque(false);
                add(column[i]);
            }
        }
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            Color foreground, background;
            if (isSelected) {
                background = new Color(list.getSelectionBackground().getRGB());
                foreground = new Color(list.getSelectionForeground().getRGB());
            } else {
                background = new Color(list.getBackground().getRGB());
                foreground = new Color(list.getForeground().getRGB());
            }
            this.setBackground(background);
            this.setForeground(foreground);

            if (index >= 0 && index < items.size()){
                column[0].setText(items.get(index)[0]);
                column[1].setText("   "+items.get(index)[1]);
            } else {
                column[0].setText("qqqqqqqqqqqqqqqq");
                column[1].setText("qqqq");
            }
            return this;
        }
    }
}

