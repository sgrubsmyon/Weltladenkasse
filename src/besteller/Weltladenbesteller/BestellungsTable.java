package Weltladenbesteller;

// Basic Java stuff:
import java.util.*; // for Vector

// GUI stuff:
import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import WeltladenDB.ArticleSelectTable;

public class BestellungsTable extends ArticleSelectTable {
    /**
     *    The constructor.
     *       */
    public BestellungsTable(Vector< Vector<Object> > data, Vector<String> columns,
            Vector<String> colors) {
        super(data, columns, colors);
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        // add custom rendering here
        if ( getColumnName(column).equals("Stückzahl") ){
            Component cc;
            if (renderer instanceof JSpinner){
                cc = ( (JSpinner.NumberEditor) ((JSpinner)renderer).getEditor() ).getTextField();
            } else {
                cc = c;
            }
            cc.setFont( cc.getFont().deriveFont(Font.BOLD) );
            int vpeColIndex = convertColumnIndexToView( getColumn("VPE").getModelIndex() );
            try {
                int vpeInt = Integer.parseInt(getValueAt(row, vpeColIndex).toString());
                int stueck = Integer.parseInt(getValueAt(row, column).toString());
                if (stueck < vpeInt){
                    cc.setForeground(Color.red);
                } else {
                    cc.setForeground(Color.green.darker().darker());
                }
            } catch (Exception ex) {
                cc.setForeground(Color.black); // if sth. goes wrong: default color
            }
        }
        //c.setBackground(Color.LIGHT_GRAY);
        return c;
    }
}
