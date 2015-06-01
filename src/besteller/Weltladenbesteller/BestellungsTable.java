package Weltladenbesteller;

// Basic Java stuff:
import java.util.*; // for Vector

// GUI stuff:
import java.awt.Component;
import java.awt.Font;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.table.*;

import WeltladenDB.AnyJComponentJTable;

public class BestellungsTable extends AnyJComponentJTable {
    private Vector<Boolean> sortimentBools;
    private Vector<Integer> displayIndices;

    /**
     *    The constructor.
     *       */
    public BestellungsTable(Vector< Vector<Object> > data, Vector<String>
            columns, Vector<Integer> dispInd, Vector<Boolean> sortBools)
    {
        super(data, columns);
        displayIndices = dispInd;
        sortimentBools = sortBools;
        //int stueckCol = columns.indexOf("Stückzahl");
        //for (int i=0; i<data.size(); i++){
        //    System.out.println("Editing cell at "+i+","+stueckCol);
        //    editCellAt(i, stueckCol);
        //}
    }

    @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);
            // add custom rendering here
            if ( getColumnName(column).equals("Stückzahl") ){
                JFormattedTextField tf = ((JSpinner.NumberEditor)((JSpinner)c).getEditor()).getTextField();
                tf.setFont( tf.getFont().deriveFont(Font.BOLD) );
                int vpeColIndex = convertColumnIndexToView( getColumn("VPE").getModelIndex() );
                try {
                    int vpeInt = Integer.parseInt(getValueAt(row, vpeColIndex).toString());
                    int stueck = (Integer)((JSpinner)getValueAt(row, column)).getValue();
                    if (stueck < vpeInt){
                        tf.setForeground(Color.red);
                    } else {
                        tf.setForeground(Color.green.darker().darker());
                    }
                } catch (Exception ex) {
                    tf.setForeground(Color.black); // if sth. goes wrong: default color
                }
            } else {
                c.setFont( c.getFont().deriveFont(Font.BOLD) );
                int realRowIndex = convertRowIndexToModel(row);
                realRowIndex = displayIndices.get(realRowIndex); // convert from displayData index to data index
                if ( sortimentBools.get( realRowIndex ) == false ){
                    c.setForeground(Color.GRAY); // not in sortiment
                } else {
                    c.setForeground(Color.BLACK); // in sortiment
                }
            }
            //c.setBackground(Color.LIGHT_GRAY);
            return c;
        }
}
