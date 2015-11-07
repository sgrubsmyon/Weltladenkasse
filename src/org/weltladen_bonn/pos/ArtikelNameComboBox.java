package org.weltladen_bonn.pos;

// basic Java stuff:
import java.util.*;

import java.lang.ArrayIndexOutOfBoundsException;

// MySQL Connector/J stuff:
import java.sql.*;

// GUI stuff:
import java.awt.*;

import javax.swing.*;

public class ArtikelNameComboBox extends IncrementalSearchComboBox {
    private Connection conn; // connection to MySQL database
    private BaseClass bc;

    public ArtikelNameComboBox(Connection conn, String fstr, BaseClass bc) {
        super(fstr);
        this.setRenderer(new MultiColRenderer());
        this.conn = conn;
        this.bc = bc;
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
                    "SELECT DISTINCT a.artikel_name, l.lieferant_name, a.vk_preis, a.sortiment "+
                    "FROM artikel AS a " +
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
                String vkPreis = rs.getString(3) != null ? rs.getString(3) : "";
                Boolean sortiment = rs.getBoolean(4);
                if (!vkPreis.equals("")){
                    vkPreis = bc.priceFormatter(vkPreis)+" "+bc.currencySymbol;
                }

                searchResults.add(new String[]{artName, liefName, vkPreis, sortiment.toString()});
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
        JLabel[] columns = new JLabel[3];
        GridBagConstraints c1 = new GridBagConstraints();

        public MultiColRenderer() {
            setLayout(new GridBagLayout());
            //c1.fill = GridBagConstraints.HORIZONTAL;
            c1.ipadx = 5;
            c1.insets = new Insets(0, 100, 0, 100);
            for (int i=0; i<columns.length; i++){
                if (i == 2){
                    c1.anchor = GridBagConstraints.LINE_END;
                } else {
                    c1.anchor = GridBagConstraints.LINE_START;
                }
                c1.gridx = i;
                columns[i] = new JLabel();
                //columns[i].setOpaque(false);
                add(columns[i], c1);
                //add(columns[i]);
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
                columns[0].setText(parseName(items.get(index)[0]));
                columns[1].setText(items.get(index)[1]);
                columns[2].setText(items.get(index)[2]);
                if ( ! Boolean.parseBoolean(items.get(index)[3]) ){
                    foreground = Color.GRAY;
                }
                columns[0].setForeground(foreground);
                columns[1].setForeground(foreground);
                columns[2].setForeground(foreground);
            } else {
                columns[0].setText("qqqqqqqqqqqqqqqq");
                columns[1].setText("qqqq");
                columns[2].setText("qqqq");
            }
            columns[0].setFont(bc.mediumFont); // might be too big
            columns[1].setFont(bc.mediumFont);
            columns[2].setFont(bc.mediumFont);

            // full name as tooltip
	    if (index >= 0 && value != null) {
		list.setToolTipText(items.get(index)[0]);
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

