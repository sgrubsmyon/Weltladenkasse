package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.*;

// GUI stuff:
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.tree.*;

import WeltladenDB.Artikelliste;

public class PreisschilderListe extends Artikelliste {
    // Attribute:
    private PreisschilderListeContainer prlContainer;

    private JButton printButton;

    // Methoden:
    protected PreisschilderListe(Connection conn, PreisschilderListeContainer pc,
            Integer tid, Integer sid, Integer ssid, String gn) {
        super(conn, pc.getMainWindowPointer());

        this.prlContainer = pc;
        this.toplevel_id = tid;
        this.sub_id = sid;
        this.subsub_id = ssid;
        this.produktgruppenname = gn;

        fillDataArray();
        showAll();
        int minColWidth = myTable.getColumn("Drucken").getPreferredWidth();
        myTable.resizeColumnToFitContent(0, columnMargin, minColWidth, maxColumnWidth);
    }

    protected void fillDataArray() {
        super.fillDataArray();
        // add special first column to select articles for which to print price tag
        columnLabels.add(0, "Drucken");
        for ( Vector<Object> row : data ){
            // set all Drucken bools to false
            row.add(0, false);
        }
        refreshOriginalData();
        displayData = new Vector< Vector<Object> >(data);

        // make only Drucken column editable
        editableColumns = new Vector<String>();
        editableColumns.add("Drucken");
    }


    @Override
    protected void showBottomPanel() {
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
          JPanel bottomCenterPanel = new JPanel();
            printButton = new JButton("Markierte Artikel drucken");
            printButton.setMnemonic(KeyEvent.VK_D);
            printButton.addActionListener(this);
            bottomCenterPanel.add(printButton);
        bottomPanel.add(bottomCenterPanel, BorderLayout.CENTER);

        allPanel.add(bottomPanel, BorderLayout.SOUTH);
    }


    protected void enableButtons() {
        printButton.setEnabled( areArticlesSelectedForPrint() );
    }


    boolean areArticlesSelectedForPrint() {
        for ( Vector<Object> row : data ){
            if ( (Boolean)row.get(0) == true )
                return true;
        }
        return false;
    }


    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == backButton){
            prlContainer.switchToProduktgruppenliste();
            return;
        }
        if (e.getSource() == printButton){
            return;
        }
        super.actionPerformed(e);
    }
}
