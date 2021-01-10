package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector

// MySQL Connector/J stuff:
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import java.awt.event.*;

//import javax.swing.JFrame;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTable;
//import javax.swing.JTextArea;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;

import org.weltladen_bonn.pos.MainWindowGrundlage;

public class StornierteRechnungen extends Rechnungen {
    // Attribute:

    // Methoden:
    /**
     *    The constructor.
     *       */
    public StornierteRechnungen(MariaDbPoolDataSource pool, MainWindowGrundlage mw){
        super(pool, mw, "WHERE verkauf.storniert = TRUE AND " +
                "verkauf.rechnungs_nr > IFNULL((SELECT MAX(rechnungs_nr_bis) FROM abrechnung_tag), 0) ",
                "Stornierte Rechnungen");
        showTable();
    }

    void addOtherStuff() {
    }

    void addButtonsToTable(){
	// create the buttons for each row:
	detailButtons = new Vector<JButton>();
	for (int i=0; i<data.size(); i++){
	    detailButtons.add(new JButton("+"));
	    detailButtons.get(i).addActionListener(this);
	    myTable.setValueAt( detailButtons.get(i), i, 0 );
	}
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
    }
}
