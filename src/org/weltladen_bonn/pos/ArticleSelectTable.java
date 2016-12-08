package org.weltladen_bonn.pos;

// Basic Java stuff:
import java.util.*; // for Vector, String

// GUI stuff:
import java.awt.*; // BorderLayout, FlowLayout, Dimension, Event, Component, Color, Font
import javax.swing.table.*;

public class ArticleSelectTable extends AnyJComponentJTable {
    private Vector<String> colors;

    /**
     *    The constructor.
     *       */
    public ArticleSelectTable(Vector< Vector<Object> > data, Vector<String> columns,
            Vector<String> colors) {
        super(data, columns);
        this.colors = colors;
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        // add custom rendering here
        c.setFont( c.getFont().deriveFont(Font.BOLD) );
        String color = colors.get(row);
        switch (color) {
            case "red":
                c.setForeground(Color.RED);
                break;
            case "blue":
                c.setForeground(Color.BLUE);
                break;
            case "green":
                c.setForeground(Color.GREEN.darker().darker());
                break;
            case "gray":
                c.setForeground(Color.GRAY);
                break;
            default:
                c.setForeground(Color.BLACK);
                break;
        }
        //c.setBackground(Color.LIGHT_GRAY);
        return c;
    }
}
