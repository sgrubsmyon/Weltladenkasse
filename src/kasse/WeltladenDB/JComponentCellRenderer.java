package WeltladenDB;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.JComponent;
import javax.swing.table.TableCellRenderer;

public class JComponentCellRenderer implements TableCellRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        return (JComponent)value;
    }
}
