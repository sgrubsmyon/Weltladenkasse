package Weltladenbesteller;

// Basic Java stuff:
import java.util.*; // for Vector

// GUI stuff:
import java.awt.Component;
import java.awt.Font;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.table.*;

import WeltladenDB.AnyJComponentJTable;

public class BestellungsTable extends AnyJComponentJTable {
    protected Vector<String> colors;

    /**
     *    The constructor.
     *       */
    public BestellungsTable(Vector< Vector<Object> > data, Vector<String> columns,
            Vector<String> cs) {
        super(data, columns);
        colors = cs;
    }

    @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);
            // add custom rendering here
            c.setFont( c.getFont().deriveFont(Font.BOLD) );
            String color = colors.get(row);
            int realColIndex = convertColumnIndexToModel(column); // user might have changed column order
            if ( getColumnName(realColIndex).equals("Stückzahl") ){
                if (color.equals("red")){ c.setForeground(Color.red); }
                else if (color.equals("blue")){ c.setForeground(Color.blue); }
                else if (color.equals("green")){ c.setForeground(Color.green.darker().darker()); }
                else { c.setForeground(Color.black); }
            } else {
                c.setForeground(Color.black);
            }
            //c.setBackground(Color.LIGHT_GRAY);
            return c;
        }

    // Implement table cell tool tips.
    @Override
        public String getToolTipText(MouseEvent e) {
            Point p = e.getPoint();
            int rowIndex = rowAtPoint(p);
            int colIndex = columnAtPoint(p);
            int realRowIndex = convertRowIndexToModel(rowIndex); // user might have changed row order
            int realColIndex = convertColumnIndexToModel(colIndex); // user might have changed column order
            String tip = "";
            if ( !getColumnName(realColIndex).equals("Entfernen") ){ // exclude column with buttons
                tip = this.getModel().getValueAt(realRowIndex, realColIndex).toString();
            }
            return tip;
        }
    // Implement table header tool tips.
    @Override
        protected JTableHeader createDefaultTableHeader() {
            return new JTableHeader(columnModel) {
                public String getToolTipText(MouseEvent e) {
                    String tip = null;
                    Point p = e.getPoint();
                    int colIndex = columnAtPoint(p);
                    int realColIndex = convertColumnIndexToModel(colIndex); // user might have changed column order
                    tip = getColumnName(realColIndex);
                    return tip;
                }
            };
        }
}
