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

    /**
     *    The constructor.
     *       */
    public BestellungsTable(Vector< Vector<Object> > data, Vector<String> columns) {
        super(data, columns);
    }

    @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);
            // add custom rendering here
            c.setFont( c.getFont().deriveFont(Font.BOLD) );
            if ( getColumnName(column).equals("Stückzahl") ){
                //int vpeColIndex = convertColumnIndexToView( getColumnModel().getColumnIndex("VPE") );
                int vpeColIndex = convertColumnIndexToView( getColumn("VPE").getModelIndex() );
                try {
                    int vpeInt = Integer.parseInt(getValueAt(row, vpeColIndex).toString());
                    int stueck = Integer.parseInt(getValueAt(row, column).toString());
                    if (stueck < vpeInt){
                        c.setForeground(Color.red);
                    } else {
                        c.setForeground(Color.green.darker().darker());
                    }
                } catch (Exception ex) {
                    c.setForeground(Color.black); // if sth. goes wrong: default color
                }
            } else {
                c.setForeground(Color.black); // if not stueck column: default color
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
            String tip = null;
            if ( !getColumnName(colIndex).equals("Entfernen") ){ // exclude column with buttons
                try { tip = getValueAt(rowIndex, colIndex).toString(); }
                catch (Exception ex) { } // e.g. catch if cell contains NullPointer
            }
            return tip;
        }
    // Implement table header tool tips.
    @Override
        protected JTableHeader createDefaultTableHeader() {
            return new JTableHeader(columnModel) {
                public String getToolTipText(MouseEvent e) {
                    Point p = e.getPoint();
                    int colIndex = columnAtPoint(p);
                    String tip = null;
                    try { tip = getColumnName(colIndex); }
                    catch (Exception ex) { }
                    return tip;
                }
            };
        }
}
