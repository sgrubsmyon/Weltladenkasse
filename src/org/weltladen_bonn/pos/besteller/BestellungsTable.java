package org.weltladen_bonn.pos.besteller;

// Basic Java stuff:
import java.util.*; // for Vector

// GUI stuff:
import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import org.weltladen_bonn.pos.ArticleSelectTable;
import org.weltladen_bonn.pos.BaseClass;

public class BestellungsTable extends ArticleSelectTable {
    private BaseClass bc;

    /**
     *    The constructor.
     *       */
    public BestellungsTable(BaseClass bc, Vector< Vector<Object> > data, Vector<String> columns,
            Vector<String> colors) {
        super(data, columns, colors);
        this.bc = bc;
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        Object value = this.getValueAt(row, column); // here, no conversion must be done (don't really get why, because tool tip and articleIndex need it)
        // add custom rendering here
        String cname = getColumnName(column);
        if ( cname.equals("Stückzahl") ){
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
        if ( cname.equals("Beliebtheit") ){
            Integer index = bc.beliebtWerte.indexOf( Integer.parseInt(value.toString()) );
            c.setFont( c.getFont().deriveFont(Font.PLAIN) );
            c.setForeground( bc.beliebtFarben.get(index) );
            if (c instanceof JLabel){
                JLabel label = (JLabel)c;
                label.setText( bc.beliebtKuerzel.get(index) );
            }
        }
        //c.setBackground(Color.LIGHT_GRAY);
        return c;
    }
}
