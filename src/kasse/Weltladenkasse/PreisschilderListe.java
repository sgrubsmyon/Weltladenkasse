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

    private Vector<String> articleNames;
    private Vector<String> mengen;
    private Vector<String> preise;
    private Vector<String> lieferanten;
    private Vector<String> kg_preise;

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


    void fillPriceTagVectors() {
        articleNames = new Vector<String>();
        mengen = new Vector<String>();
        preise = new Vector<String>();
        lieferanten = new Vector<String>();
        kg_preise = new Vector<String>();
        for (int i=0; i<data.size(); i++){
            if ( (Boolean)data.get(i).get(0) == true ){
                int id = artikelIDs.get(i);
                String kurzname = getShortName(id);
                String liefkurz = getShortLieferantName(id);
                String preis = priceFormatter(
                        data.get(i).get(columnLabels.indexOf("VK-Preis")).toString() )+
                        " "+currencySymbol;
                String[] menge_kg_preis = getMengeAndPricePerKg(id);
                String menge = menge_kg_preis[0];
                String kg_preis = menge_kg_preis[1];

                articleNames.add(kurzname);
                mengen.add(menge);
                preise.add(preis);
                lieferanten.add(liefkurz);
                kg_preise.add(kg_preis);
            }
        }
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
            fillPriceTagVectors();
            new PreisschilderExport(this.conn, this.mainWindow,
                    articleNames, mengen, preise, lieferanten, kg_preise);
            return;
        }
        super.actionPerformed(e);
    }
}
