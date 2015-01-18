package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector

// MySQL Connector/J stuff:
import java.sql.Connection;

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

import WeltladenDB.MainWindowGrundlage;

public class StornierteRechnungen extends Rechnungen {
    // Attribute:

    // Methoden:
    /**
     *    The constructor.
     *       */
    public StornierteRechnungen(Connection conn, MainWindowGrundlage mw){
	super(conn, mw, "WHERE verkauf.storniert = TRUE AND " +
                "verkauf.verkaufsdatum > IFNULL((SELECT MAX(zeitpunkt_real) FROM abrechnung_tag), '0001-01-01') ",
                "Stornierte Rechnungen");
	showTable();
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
    public void actionPerformed(ActionEvent e)
    {
	if (e.getSource() == removeDetailButton){
	    updateTable();
	    return;
	}
	final int numberOfRows = detailButtons.size();
	int detailRow=-1;
	for (int i=0; i<numberOfRows; i++){
	    if (e.getSource() == detailButtons.get(i) ){
		detailRow = i;
		break;
	    }
	}
	if (detailRow != -1){
	    showDetailTable(detailRow, this.titleStr);
	    return;
	}
    }
}
