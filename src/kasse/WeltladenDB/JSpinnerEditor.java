package WeltladenDB;

import java.util.*;

import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*; // for NumberFormatter
import javax.swing.table.TableCellEditor;

public class JSpinnerEditor implements TableCellEditor{
    private ArrayList<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
    private JSpinner spinner;
    private JTable table;
    private int editingRow;
    private int editingColumn;

    // Standardkonstruktor
    public JSpinnerEditor(SpinnerNumberModel m){
        initiateSpinner(m);
    }

    private void initiateSpinner(SpinnerNumberModel m){
        spinner = new JSpinner(m);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "###");
        spinner.setEditor(editor);
        JFormattedTextField field = editor.getTextField();
        ( (NumberFormatter) field.getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
    }

    public JSpinner getSpinner() {
        return spinner;
    }

    private JFormattedTextField getField() {
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor)spinner.getEditor();
        JFormattedTextField field = editor.getTextField();
        return field;
    }

    private void updateSpinner(Number value) {
        SpinnerNumberModel mold = (SpinnerNumberModel)spinner.getModel();
        SpinnerNumberModel m = new SpinnerNumberModel(value,
                mold.getMinimum(), mold.getMaximum(), mold.getStepSize());
        initiateSpinner(m);
    }

    private void updateTable() {
        JFormattedTextField field = getField();
        try {
            Integer newValue = Integer.parseInt(field.getText());
            table.setValueAt(newValue, editingRow, editingColumn);
        } catch (NumberFormatException ex){
        }
    }

    // Möglicherweise möchte jemand über Ereignisse des Editors
    // informiert werden
    public void addCellEditorListener(CellEditorListener l) {
        listeners.add(l);
    }

    // Ein CellEditorListener entfernen
    public void removeCellEditorListener(CellEditorListener l) {
        listeners.remove(l);
    }

    // Gibt den aktuellen Wert des Editors zurück.
    public Object getCellEditorValue() {
        return spinner.getValue();
    }

    // Gibt eine Component zurück, welche auf dem JTable dargestellt wird,
    // und mit der der Benutzer interagieren kann.
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        updateSpinner((Number)value);
        this.table = table;
        this.editingRow = row;
        this.editingColumn = column;
        // seems to have no effect at all:
        //SwingUtilities.invokeLater(new Runnable(){
        //    public void run(){
        //        JSpinner.NumberEditor editor = (JSpinner.NumberEditor)spinner.getEditor();
        //        JFormattedTextField field = editor.getTextField();
        //        field.selectAll();
        //    }
        //});
        return spinner;
    }

    // Gibt an, ob die Zelle editierbar ist. Das EventObject kann
    // ein MouseEvent, ein KeyEvent oder sonst was sein.
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    // Gibt an, ob die Editor-Component selektiert werden muss, um
    // sie zu benutzen. Diese Editor soll immer selektiert werden,
    // deshalb wird hier true zurückgegeben
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    // Bricht das editieren der Zelle ab
    public void cancelCellEditing() {
        fireEditingCanceled();
    }

    // Stoppt das editieren der Zelle, sofern möglich.
    // Da der JSpinner immer einen gültigen Wert anzeigt, kann auch
    // jederzeit gestoppt werden (return-Wert = true)
    public boolean stopCellEditing() {
        fireEditingStopped();
        return true;
    }

    // Benachrichtig alle Listener, dass das Editieren abgebrochen wurde
    protected void fireEditingCanceled(){
        ChangeEvent e = new ChangeEvent(spinner);
        for( int i = 0, n = listeners.size(); i<n; i++ )
            ((CellEditorListener)listeners.get(i)).editingCanceled( e );
        updateTable();
    }

    // Benachrichtig alle Listener, dass das Editieren beendet wurde
    protected void fireEditingStopped(){
        ChangeEvent e = new ChangeEvent(spinner);
        for( int i = 0, n = listeners.size(); i<n; i++ )
            ((CellEditorListener)listeners.get(i)).editingStopped( e );
        updateTable();
    }
}
