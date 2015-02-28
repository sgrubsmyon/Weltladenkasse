package WeltladenDB;

import java.util.*;
import java.lang.IllegalArgumentException;
import java.awt.*;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.table.*;

// for putting any JComponent (e.g. JButton) into a JTable: Code from http://www.codeguru.com/java/articles/162.shtml
public class AnyJComponentJTable extends JTable {
    // attributes:
    private HashSet<Integer> editableCols = new HashSet<Integer>();

    // methods:
    public AnyJComponentJTable(){
        super();
    }

    public AnyJComponentJTable(TableModel m){
        super(m);
    }

    public AnyJComponentJTable(TableModel m, Integer columnMargin,
            Integer minColumnWidth, Integer maxColumnWidth){
        super(m);
        for (int colIndex=0; colIndex<this.getColumnCount(); colIndex++){
            resizeColumnToFitContent(colIndex, columnMargin, minColumnWidth, maxColumnWidth);
        }
    }

    public AnyJComponentJTable(Vector data, Vector labels){
        super(data, labels);
    }

    public AnyJComponentJTable(Vector data, Vector labels, Integer
            columnMargin, Integer minColumnWidth, Integer maxColumnWidth){
        super(data, labels);
        for (int colIndex=0; colIndex<this.getColumnCount(); colIndex++){
            resizeColumnToFitContent(colIndex, columnMargin, minColumnWidth, maxColumnWidth);
        }
    }

    public AnyJComponentJTable(Vector data, Vector labels, Set<Integer> edCol){
        super(data, labels);
        editableCols = new HashSet<Integer>(edCol);
    }

    public void setColEditableTrue(int colNumber){
        if ( colNumber < 0 || colNumber >= this.getColumnCount() )
            throw new IllegalArgumentException();
        editableCols.add(colNumber);
    }

    public void setColEditableFalse(int colNumber){
        if ( colNumber < 0 || colNumber >= this.getColumnCount() )
            throw new IllegalArgumentException();
        editableCols.remove(colNumber);
    }

    public TableCellRenderer getCellRenderer(int row, int column) {
        // always return button-capable renderer if this is a JComponent
        // (otherwise custom renderer that was set manually)
        if ( getValueAt(row, column) instanceof JComponent ){
            return new JComponentCellRenderer();
        }
        TableColumn tableColumn = getColumnModel().getColumn(column);
        TableCellRenderer renderer = tableColumn.getCellRenderer();
        if (renderer == null) {
            Class c = getColumnClass(column);
            if ( c.equals(Object.class) ){
                Object o = getValueAt(row,column);
                if ( o != null )
                    c = getValueAt(row,column).getClass();
            }
            renderer = getDefaultRenderer(c);
        }
        return renderer;
    }

    public TableCellEditor getCellEditor(int row, int column) {
        // always return button-capable editor if this is a JComponent
        // (otherwise custom editor that was set manually)
        if ( getValueAt(row, column) instanceof JComponent ){
            return new JComponentCellEditor();
        }
        TableColumn tableColumn = getColumnModel().getColumn(column);
        TableCellEditor editor = tableColumn.getCellEditor();
        if (editor == null) {
            Class c = getColumnClass(column);
            if ( c.equals(Object.class) ){
                Object o = getValueAt(row,column);
                if ( o != null )
                    c = getValueAt(row,column).getClass();
            }
            editor = getDefaultEditor(c);
        }
        return editor;
    }

    public boolean isCellEditable(int row, int col){
        if (editableCols.contains(col) || getValueAt(row, col) instanceof JButton)
            return true;
        else
            return false;
    }

    // Implement table header tool tips.
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            public String getToolTipText(MouseEvent e) {
                Point p = e.getPoint();
                int colIndex = columnAtPoint(p);
                String tip = null;
                //int realColIndex = convertColumnIndexToModel(colIndex); // user might have changed column order
                try { tip = getColumnName(colIndex); }
                catch (Exception ex) { } // e.g. catch if cell contains NullPointer
                return tip;
            }
        };
    }

    // Implement table cell tool tips.
    public String getToolTipText(MouseEvent e) {
        Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);
        //int realRowIndex = convertRowIndexToModel(rowIndex); // user might have changed row order
        //int realColIndex = convertColumnIndexToModel(colIndex); // user might have changed column order
        String tip = null;
        if ( getValueAt(rowIndex, colIndex) instanceof JComponent ){
            // no tool tip for Components
            return tip;
        }
        try { tip = getValueAt(rowIndex, colIndex).toString(); }
        catch (Exception ex) { } // e.g. catch if cell contains NullPointer
        return tip;
    }

    public void resizeColumnToFitContent(TableColumn column, Integer margin,
            Integer minColumnWidth, Integer maxColumnWidth){
        /** Found on http://www.programcreek.com/java-api-examples/index.php?api=javax.swing.table.TableCellRenderer */
        int columnIndex = column.getModelIndex();
        TableCellRenderer renderer = column.getHeaderRenderer();
        if (renderer == null) {
            renderer = getTableHeader().getDefaultRenderer();
        }
        Component c = renderer.getTableCellRendererComponent(this, column.getHeaderValue(), false, false, 0, 0);
        // start with header width:
        //int maxWidth = c.getPreferredSize().width;
        // or ignore header width:
        int maxWidth = 0;
        if (minColumnWidth != null){
            maxWidth = minColumnWidth;
        }
        for (int row=0; row < getRowCount(); row++) {
            renderer = getCellRenderer(row, columnIndex);
            c = renderer.getTableCellRendererComponent(this,
                    getValueAt(row, columnIndex), false, false, row, columnIndex);
            maxWidth = Math.max(maxWidth, c.getPreferredSize().width);
        }
        if ( maxColumnWidth != null && maxWidth > maxColumnWidth){
            //System.out.println("column: "+column.getHeaderValue().toString()+"; maxWidth = "+
            //        maxWidth+" > "+maxColumnWidth+" = maxColumnWidth");
            maxWidth = maxColumnWidth;
        }
        column.setPreferredWidth(maxWidth + margin);
    }

    public void resizeColumnToFitContent(int columnIndex, Integer margin,
            Integer minColumnWidth, Integer maxColumnWidth){
        TableColumn column = getColumnModel().getColumn(columnIndex);
        resizeColumnToFitContent(column, margin, minColumnWidth, maxColumnWidth);
    }
}
