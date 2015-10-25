package org.weltladen_bonn.pos;

// basic Java stuff:
import java.util.Vector;
import java.util.Collections;
import java.util.Comparator;

import java.lang.ArrayIndexOutOfBoundsException;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
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
import javax.swing.DefaultListCellRenderer;

public class ArtikelNameComboBox extends IncrementalSearchComboBox {
    private Connection conn; // connection to MySQL database

    public ArtikelNameComboBox(Connection conn, String fstr) {
        super(fstr);
        this.setRenderer(new MultiColRenderer());
        this.conn = conn;
    }

    public String[] parseArtikelName() {
        try {
            String[] item = this.items.get(this.getSelectedIndex());
            return new String[]{item[0], item[1]};
        } catch (ArrayIndexOutOfBoundsException ex){
            System.out.println("For some reason, selected index in ArtikelNameComboBox is "+
                    this.getSelectedIndex()+", which is out of bounds for array `items`.");
            return new String[]{"", ""};
        }
    }

    public Vector<String[]> doQuery() {
        Vector<String[]> searchResults = new Vector<String[]>();
        try {
            // construct where clause from the textFeld words, separated by spaces:
            String[] words = textFeld.getText().split("\\s+");
            //System.out.print("Search words: {");
            //for (int i=0; i<words.length; i++){
            //    System.out.print("\""+words[i]+"\",");
            //}
            //System.out.println("}");
            String whereClause = "";
            if (words.length > 0){
                whereClause += "artikel_name LIKE ? ";
            }
            for (int i=1; i<words.length; i++){
                whereClause += "AND artikel_name LIKE ? ";
            }
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT DISTINCT a.artikel_name, l.lieferant_name, a.sortiment FROM artikel AS a " +
                    "LEFT JOIN produktgruppe AS p USING (produktgruppen_id) " +
                    "LEFT JOIN lieferant AS l USING (lieferant_id) " +
                    "WHERE " + whereClause +
                    "AND a.aktiv = TRUE " + filterStr +
                    "ORDER BY a.artikel_name, l.lieferant_name"
                    );
            for (int i=0; i<words.length; i++){
                pstmt.setString(i+1, "%"+words[i]+"%");
            }
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet ...
            while (rs.next()) {
                String artName = rs.getString(1);
                String liefName = rs.getString(2) != null ? rs.getString(2) : "";
                Boolean sortiment = rs.getBoolean(3);
                searchResults.add(new String[]{artName, liefName, sortiment.toString()});
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

    // based on: http://www.coderanch.com/t/340213/GUI/java/Multiple-Columns-JCombobox
    class MultiColRenderer extends JPanel implements ListCellRenderer<Object> {
        JLabel[] column = new JLabel[2];
        public MultiColRenderer() {
            setLayout(new GridLayout(0,2));
            for (int i=0; i<column.length; i++){
                column[i] = new JLabel();
                //column[i].setOpaque(false);
                add(column[i]);
            }
        }
        public Component getListCellRendererComponent(JList<?> list, Object value,
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
                column[0].setText( parseName(items.get(index)[0]) );
                column[1].setText("   "+items.get(index)[1]);
                if ( ! Boolean.parseBoolean(items.get(index)[2]) ){
                    foreground = Color.GRAY;
                }
                column[0].setForeground(foreground);
                column[1].setForeground(foreground);
            } else {
                column[0].setText("qqqqqqqqqqqqqqqq");
                column[1].setText("qqqq");
            }
            return this;
        }
    }

    //// From: http://stackoverflow.com/questions/9171258/colored-jcombobox-with-colored-items-and-focus
    ////JList#setSelectionForeground(...) version
    //static class ColoredCellRenderer implements ListCellRenderer {
    //    protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
    //    private final Color selectionBackground = new Color(240,200,200);
    //    public Component getListCellRendererComponent(
    //            JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    //        Color fgc = Color.valueOf((String)value).color;
    //        if(index<0) {
    //            //comboBox.setForeground(fgc); //Windows, CDE/Motif Look & Feel
    //            list.setSelectionForeground(fgc);
    //            list.setSelectionBackground(selectionBackground);
    //        }
    //        JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(
    //                list, value, index, isSelected, cellHasFocus);
    //        if (index != -1) {
    //            renderer.setForeground(fgc);
    //        }
    //        return renderer;
    //            }
    //}

    ////html version
    //static class ComboHtmlRenderer extends DefaultListCellRenderer {
    //    private final Color selectionBackground = new Color(240,200,200);
    //    @Override public Component getListCellRendererComponent(
    //            JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
    //        Color fgc = Color.valueOf((String)value).color;
    //        if(index<0) {
    //            list.setSelectionBackground(selectionBackground);
    //        }
    //        JLabel l = (JLabel)super.getListCellRendererComponent(
    //                list, value, index, isSelected, hasFocus);
    //        l.setText("<html><font color="+hex(fgc)+">"+value);
    //        l.setBackground(isSelected?selectionBackground:list.getBackground());
    //        return l;
    //            }
    //    private static String hex(Color c) {
    //        return String.format("#%06x", c.getRGB()&0xffffff);
    //    }
    //}

}

