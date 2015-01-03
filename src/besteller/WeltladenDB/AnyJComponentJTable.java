package WeltladenDB;

import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import java.lang.IllegalArgumentException;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

// for putting any JComponent (e.g. JButton) into a JTable: Code from http://www.codeguru.com/java/articles/162.shtml
public class AnyJComponentJTable extends JTable {
    // attributes:
    private HashSet<Integer> editableCols;

    // methods:
    public AnyJComponentJTable(){
        super();
        editableCols = new HashSet<Integer>();
    }
    public AnyJComponentJTable(TableModel m){
        super(m);
        editableCols = new HashSet<Integer>();
    }
    public AnyJComponentJTable(Vector v, Vector l){
        super(v, l);
        editableCols = new HashSet<Integer>();
    }
    public AnyJComponentJTable(Vector v, Vector l, Set<Integer> edCol){
        super(v, l);
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
            if( c.equals(Object.class) )
            {
                Object o = getValueAt(row,column);
                if( o != null )
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
            if( c.equals(Object.class) )
            {
                Object o = getValueAt(row,column);
                if( o != null )
                    c = getValueAt(row,column).getClass();
            }
            editor = getDefaultEditor(c);
        }
        return editor;
    }
    public boolean isCellEditable(int row, int col){
        if (editableCols.contains(col))
            return true;
        else
            return false;
    }
}
