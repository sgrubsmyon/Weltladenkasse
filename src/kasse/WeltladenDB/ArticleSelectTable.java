package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, String

// GUI stuff:
import java.awt.*; // BorderLayout, FlowLayout, Dimension, Event, Component, Color, Font
import javax.swing.table.*;

public class ArticleSelectTable extends AnyJComponentJTable {
    Vector<String> colors;

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
        if (color.equals("red")){ c.setForeground(Color.RED); }
        else if (color.equals("blue")){ c.setForeground(Color.BLUE); }
        else if (color.equals("green")){ c.setForeground(Color.GREEN.darker().darker()); }
        else if (color.equals("gray")){ c.setForeground(Color.GRAY); }
        else { c.setForeground(Color.BLACK); }
        //c.setBackground(Color.LIGHT_GRAY);
        return c;
    }
}
