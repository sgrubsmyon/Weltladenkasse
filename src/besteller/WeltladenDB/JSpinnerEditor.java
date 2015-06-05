package WeltladenDB;

import java.util.*;

import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*; // for NumberFormatter
import javax.swing.table.TableCellEditor;

public class JSpinnerEditor implements TableCellEditor{
    private List listeners = new ArrayList();
    private JSpinner spinner;

    // Standardkonstruktor
    public JSpinnerEditor(SpinnerModel m){
        initiateSpinner(m);
    }

    private void initiateSpinner(SpinnerModel m){
        spinner = new JSpinner(m);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "###");
        spinner.setEditor(editor);
        JFormattedTextField field = editor.getTextField();
        ( (NumberFormatter) field.getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
    }

    public JSpinner getSpinner() {
        return spinner;
    }

    // Möglicherweise möchte jemand über Ereignisse des Editors
    // informiert werden
    public void addCellEditorListener(CellEditorListener l) {
        listeners.add( l );
    }

    // Ein CellEditorListener entfernen
    public void removeCellEditorListener(CellEditorListener l) {
        listeners.remove( l );
    }

    // Gibt den aktuellen Wert des Editors zurück.
    public Object getCellEditorValue() {
        return spinner.getValue();
    }

    // Gibt eine Component zurück, welche auf dem JTable dargestellt wird,
    // und mit der der Benutzer interagieren kann.
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        System.out.println("getTableCellEditorComponent() is called. 1 value: "+value);
        // only if need be:
        //SwingUtilities.invokeLater(new Runnable(){
        //    public void run(){
        SpinnerNumberModel mold = (SpinnerNumberModel)spinner.getModel();
        SpinnerNumberModel m = new SpinnerNumberModel((Number)value,
                mold.getMinimum(), mold.getMaximum(), mold.getStepSize());
        initiateSpinner(m);
        //JSpinner.NumberEditor editor = (JSpinner.NumberEditor)spinner.getEditor();
        //JFormattedTextField field = editor.getTextField();
        //field.setText(value.toString());
        //spinner.setValue(value);
        System.out.println("2 value: "+spinner.getValue());
        //System.out.println("2 textField value: "+field.getText());
        //    }
        //});
        //System.out.println("3 value: "+spinner.getValue());
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
            ((CellEditorListener)listeners.get( i )).editingCanceled( e );
    }

    // Benachrichtig alle Listener, dass das Editieren beendet wurde
    protected void fireEditingStopped(){
        ChangeEvent e = new ChangeEvent(spinner);
        for( int i = 0, n = listeners.size(); i<n; i++ )
            ((CellEditorListener)listeners.get( i )).editingStopped( e );
    }
}
