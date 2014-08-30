package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.BorderFactory;

public class ProduktgruppenIndentedRenderer extends DefaultListCellRenderer {
    private final int einrueckung = 20;
    private Vector< Vector<Integer> > produktgruppenIDsList;

    public ProduktgruppenIndentedRenderer( Vector< Vector<Integer> > idsList ){
        super();
        this.produktgruppenIDsList = idsList;
    }

    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        JLabel lbl = (JLabel)super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
        int indent = 0;
        if (index >= 0){
            Integer tid = produktgruppenIDsList.get(index).get(0); // top level
            Integer sid = produktgruppenIDsList.get(index).get(1); // sub id
            Integer ssid = produktgruppenIDsList.get(index).get(2); // subsub id
            if ( ssid != null ){ // maximale Einrueckung
                indent = 2*einrueckung;
            }
            else if ( sid != null ){ // eine Stufe Einrueckung
                indent = 1*einrueckung;
            }
            // sonst keine Einrueckung
        }
        lbl.setBorder(BorderFactory.createEmptyBorder(0,indent,0,0));//5 is the indent, modify to suit
        return lbl;
    }
}
