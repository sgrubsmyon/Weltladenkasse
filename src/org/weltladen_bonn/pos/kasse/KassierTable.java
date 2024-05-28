package org.weltladen_bonn.pos.kasse;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.table.TableCellRenderer;

import org.weltladen_bonn.pos.ArticleSelectTable;
import org.weltladen_bonn.pos.BaseClass;

public class KassierTable extends ArticleSelectTable {
    /**
     * The constructor.
     */
    public KassierTable(Vector<Vector<Object>> data, Vector<String> columns,
            Vector<String> colors) {
        super(data, columns, colors);
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        // add custom rendering here
        String cname = getColumnName(column);
        if (cname.equals("Stückzahl")) {
            Component cc;
            if (renderer instanceof JSpinner) {
                cc = ((JSpinner.NumberEditor) ((JSpinner) renderer).getEditor()).getTextField();
            } else {
                cc = c;
            }
            cc.setFont(cc.getFont().deriveFont(Font.BOLD));
            cc.setForeground(Color.black); // if sth. goes wrong: default color
        }
        return c;
    }
}
