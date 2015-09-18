package org.weltladen_bonn.pos;

import java.awt.Component;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;

public class JComponentCellRenderer implements TableCellRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        return (JComponent)value;
    }
}
