package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;

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

public class HeutigeRechnungen extends Rechnungen {
    // Attribute:

    // Methoden:
    /**
     *    The constructor.
     *       */
    public HeutigeRechnungen(Connection conn, MainWindowGrundlage mw){
	super(conn, mw, "WHERE verkauf.verkaufsdatum > IFNULL((SELECT MAX(zeitpunkt) FROM abrechnung_tag),'01-01-0001') AND "+
                "verkauf.storniert = FALSE ", "Heutige Rechnungen");
	showTable();
    }

    void addButtonsToTable(){
	// create the buttons for each row:
	detailButtons = new Vector<JButton>();
	stornoButtons = new Vector<JButton>();
	for (int i=0; i<data.size(); i++){
	    detailButtons.add(new JButton("+"));
	    detailButtons.get(i).addActionListener(this);
	    myTable.setValueAt( detailButtons.get(i), i, 0 );
	    stornoButtons.add(new JButton("Storno"));
	    stornoButtons.get(i).addActionListener(this);
	    myTable.setValueAt( stornoButtons.get(i), i, overviewLabels.size()-1 );
	}
        myTable.setColEditableTrue(0); // first column has buttons
        myTable.setColEditableTrue(overviewLabels.size()-1); // last column has buttons
    }

    private void stornieren(int stornoRow) {
	Integer rechnungsnummer = Integer.parseInt(data.get(stornoRow).get(1));
	try {
            PreparedStatement pstmt = this.conn.prepareStatement(
		    "UPDATE verkauf SET verkauf.storniert = 1 WHERE verkauf.rechnungs_nr = ?"
		    );
            pstmtSetInteger(pstmt, 1, rechnungsnummer);
	    int result = pstmt.executeUpdate();
	    if (result != 0){
		JOptionPane.showMessageDialog(this, "Rechnung " + rechnungsnummer + " wurde storniert.",
			"Stornierung ausgeführt", JOptionPane.INFORMATION_MESSAGE);
	    }
	    else {
		JOptionPane.showMessageDialog(this,
			"Fehler: Rechnung " + rechnungsnummer + " konnte nicht storniert werden.",
			"Fehler bei Stornierung", JOptionPane.ERROR_MESSAGE);
	    }
	    pstmt.close();
	} catch (SQLException ex) {
	    System.out.println("Exception: " + ex.getMessage());
	    ex.printStackTrace();
	}
	updateTable();
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e)
    {
	if (e.getSource() == heuteButton){
	    updateTable();
	    return;
	}
	if (e.getSource() == archivButton){
	    AlteRechnungen myArchiv = new AlteRechnungen(this.conn, this.mainWindow);
	    this.mainWindow.changeContentPanel(myArchiv);
	    return;
	}
	if (e.getSource() == storniertButton){
	    StornierteRechnungen myStorniert = new StornierteRechnungen(this.conn, this.mainWindow);
	    this.mainWindow.changeContentPanel(myStorniert);
	    return;
	}
	if (e.getSource() == removeDetailButton){
	    updateTable();
	    return;
	}
	final int numberOfRows = detailButtons.size();
	int detailRow=-1;
	int stornoRow=-1;
	for (int i=0; i<numberOfRows; i++){
	    if (e.getSource() == detailButtons.get(i) ){
		detailRow = i;
		break;
	    }
	}
	if (detailRow == -1){
	    for (int i=0; i<numberOfRows; i++){
		if (e.getSource() == stornoButtons.get(i) ){
		    stornoRow = i;
		    break;
		}
	    }
	}
	if (detailRow > -1){
	    showDetailTable(detailRow, this.titleStr);
	    return;
	}
	if (stornoRow > -1){
	    int answer = JOptionPane.showConfirmDialog(this,
		    "Rechnung " + (String)data.get(stornoRow).get(1) + " wirklich stornieren?", "Storno",
		    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
	    if (answer == JOptionPane.YES_OPTION)
		stornieren(stornoRow);
	    return;
	}
    }
}
