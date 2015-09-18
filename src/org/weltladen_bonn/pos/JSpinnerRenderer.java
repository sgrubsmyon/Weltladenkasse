package org.weltladen_bonn.pos;

import java.awt.Component;
import javax.swing.*;
import javax.swing.text.*; // for NumberFormatter
import javax.swing.table.TableCellRenderer;

public class JSpinnerRenderer extends JSpinner implements TableCellRenderer {
 
    public JSpinnerRenderer(SpinnerModel m) {
        setModel(m);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(this, "###");
        this.setEditor(editor);
        JFormattedTextField field = editor.getTextField();
        ( (NumberFormatter) field.getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
    }
 
    public Component getTableCellRendererComponent(final JTable table,
            final Object value, final boolean isSelected,
            final boolean hasFocus, final int row, final int column) {
        setValue(value);
        return this;
    }
}
