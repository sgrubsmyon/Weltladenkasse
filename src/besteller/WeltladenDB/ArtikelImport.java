package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.io.*; // for File
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.*;

// OpenDocument stuff:
import org.jopendocument.dom.spreadsheet.*;

// GUI stuff:
//import java.awt.BorderLayout;
//import java.awt.FlowLayout;
//import java.awt.Dimension;
import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
import java.awt.event.*;

//import javax.swing.JFrame;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTable;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.beans.*; // PropertyChangeListener

public class ArtikelImport extends DialogWindow implements ArtikelNeuInterface, PropertyChangeListener {
    // Attribute:
    protected Artikelliste artikelListe;
    protected ArtikelNeu artikelNeu;
    protected UpdateTableFunctor utf;

    private JButton fileButton;
    private JProgressBar progressBar;
    private Task readInTask;
    private JFileChooser odsChooser;
    private File file;
    private Sheet sheet;
    private String logString;
    private final String logStringStart = "<html>\n<body>\n"+
        "<style type=\"text/css\">\n"+
//        "body { font-family:monospace };\n"+
//        "div.red { color:red };\n"+
//        "div.green { color:green };\n"+
//        "div.blue { color:blue };\n"+
        "</style>\n";
    private final String baseStyle = "font-family:monospace;";
    private final String redStyle = baseStyle+"color:red";
    private final String greenStyle = baseStyle+"color:green";
    private final String blueStyle = baseStyle+"color:blue";
    private final String newline = "<br>\n";
    private final String logStringEnd = "</body>\n</html>";
    private JEditorPane log;
    private final Dimension minLogDimension = new Dimension(400,100);
    private final Dimension prefLogDimension = new Dimension(600,200);

    private JSplitPane splitPane;

    protected JButton submitButton;
    protected JButton deleteButton;

    // Methoden:
    public ArtikelImport(Connection conn, MainWindowGrundlage mw, Artikelliste pw, JDialog dia) {
	super(conn, mw, dia);
        artikelListe = pw;
        logString = logStringStart;
        //"<style type=\"text/css\" media=\"screen\"> \" font-family: sans-serif; \">\n";

        utf = new UpdateTableFunctor() {
            public void updateTable() {
                artikelNeu.updateTable(allPanel);
                splitPane.setBottomComponent(artikelNeu.tablePanel);
            }
        };
        artikelNeu = new ArtikelNeu(conn, mw, utf);

        showAll();
    }

    protected void showHeader() {
        headerPanel = new JPanel();
	headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        //headerPanel.setMinimumSize(minLogDimension);
        //headerPanel.setPreferredSize(prefLogDimension);

        odsChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "ODS Spreadsheet-Dokumente", "ods");
        odsChooser.setFileFilter(filter);

        JPanel fileButtonPanel = new JPanel();
        fileButton = new JButton("Datei auswählen");
        fileButton.setMnemonic(KeyEvent.VK_D);
	fileButton.addActionListener(this);
	//fileButton.setEnabled(artikelNeu.data.size()==0);
        fileButtonPanel.add(fileButton);
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        fileButtonPanel.add(progressBar);
        headerPanel.add(fileButtonPanel);

        log = new JEditorPane();
        //log.setMargin(new Insets(5,5,5,5));
        log.setEditable(false);
        log.setContentType("text/html");
        log.setMinimumSize(minLogDimension);
        log.setPreferredSize(prefLogDimension);
        log.setText(logString+logStringEnd);
        JScrollPane logScrollPane = new JScrollPane(log);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log"));
        //logScrollPane.setBorder( BorderFactory.createEmptyBorder(5,5,5,5) );
        //logScrollPane.setMinimumSize(minLogDimension);
        //logScrollPane.setPreferredSize(prefLogDimension);
        headerPanel.add(logScrollPane);

        allPanel.add(headerPanel);
    }

    protected void showMiddle() {
        artikelNeu.showTable(allPanel);

        //splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        //        headerPanel,
        //        artikelNeu.tablePanel);
        //splitPane.setOneTouchExpandable(true);
        //splitPane.setResizeWeight(0.3);
        //allPanel.add(splitPane);
    }

    protected void showFooter() {
        footerPanel = new JPanel();
        submitButton = new JButton("Abschicken");
        submitButton.setMnemonic(KeyEvent.VK_A);
        submitButton.addActionListener(this);
        if (artikelNeu.data.size() == 0){
            submitButton.setEnabled(false);
        } else {
            submitButton.setEnabled(true);
        }
        footerPanel.add(submitButton);
        deleteButton = new JButton("Verwerfen");
        deleteButton.setMnemonic(KeyEvent.VK_V);
        deleteButton.addActionListener(this);
        if (artikelNeu.data.size() == 0){
            deleteButton.setEnabled(false);
        } else {
            deleteButton.setEnabled(true);
        }
        footerPanel.add(deleteButton);
        closeButton = new JButton("Schließen");
        closeButton.setMnemonic(KeyEvent.VK_S);
        closeButton.addActionListener(this);
        if ( !willDataBeLost() ){
            closeButton.setEnabled(true);
        } else {
            closeButton.setEnabled(false);
        }
        footerPanel.add(closeButton);
        allPanel.add(footerPanel);
    }

    // will data be lost on close?
    protected boolean willDataBeLost() {
        return artikelNeu.willDataBeLost();
    }

    public int checkIfItemAlreadyKnown(String lieferant, String nummer) {
        return artikelNeu.checkIfItemAlreadyKnown(lieferant, nummer);
    }

    public int submit() {
        return artikelNeu.submit();
    }

    public void emptyTable() {
        artikelNeu.emptyTable();
    }

    //////////////////
    // DB functions
    //////////////////
    private String queryGruppenID(String gruppenname) {
        String gruppenid = "";
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT produktgruppen_id FROM produktgruppe WHERE produktgruppen_name = ?"
                    );
            pstmt.setString(1, gruppenname);
            ResultSet rs = pstmt.executeQuery();
            if ( rs.next() )
                gruppenid = rs.getString(1) == null ? "" : rs.getString(1);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return gruppenid;
    }

    private Integer queryLieferantID(String lieferantname) {
        Integer lieferantid = null;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT lieferant_id FROM lieferant WHERE lieferant_name = ?"
                    );
            pstmt.setString(1, lieferantname);
            ResultSet rs = pstmt.executeQuery();
            if ( rs.next() )
                lieferantid = rs.getString(1) == null ? null : rs.getInt(1);
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return lieferantid;
    }

    private Vector<String> queryAllFields(Integer lieferantid, String artikelnummer) {
        Vector<String> results = new Vector<String>();
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT produktgruppen_id, artikel_name, kurzname, menge, einheit, "+
                    "sortiment, lieferbar, beliebtheit, "+
                    "barcode, vpe, setgroesse, vk_preis, empf_vk_preis, ek_rabatt, ek_preis, variabler_preis, "+
                    "herkunft, bestand " +
                    "FROM artikel "+
                    "WHERE lieferant_id = ? AND artikel_nr = ? AND aktiv = TRUE"
                    );
            pstmt.setInt(1, lieferantid);
            pstmt.setString(2, artikelnummer);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            results.add(rs.getString(1)); // produktgruppen_id
            results.add(rs.getString(2)); // artikel_name
            results.add(rs.getString(3) == null ? "" : rs.getString(3)); // kurzname
            results.add(rs.getString(4) == null ? "" : rs.getString(4)); // menge
            results.add(rs.getString(5) == null ? "" : rs.getString(5)); // einheit
            results.add(rs.getString(6));                               // sortiment
            results.add(rs.getString(7) == null ? "" : rs.getString(7)); // lieferbar
            results.add(rs.getString(8) == null ? "" : rs.getString(8)); // beliebtheit
            results.add(rs.getString(9) == null ? "" : rs.getString(9)); // barcode
            results.add(rs.getString(10) == null ? "" : rs.getString(10)); // vpe
            results.add(rs.getString(11) == null ? "" : rs.getString(11)); // setgroesse
            results.add(rs.getString(12) == null ? "" : rs.getString(12)); // vk_preis
            results.add(rs.getString(13) == null ? "" : rs.getString(13)); // empf_vk_preis
            results.add(rs.getString(14) == null ? "" : vatFormatter(rs.getString(14))); // ek_rabatt
            results.add(rs.getString(15) == null ? "" : rs.getString(15)); // ek_preis
            results.add(rs.getString(16));                               // variabler_preis
            results.add(rs.getString(17) == null ? "" : rs.getString(17)); // herkunft
            results.add(rs.getString(18) == null ? "" : rs.getString(18)); // bestand
            // edit menge:
            try {
                results.set(3, new BigDecimal(results.get(3)).stripTrailingZeros().toPlainString());
            } catch (NumberFormatException ex) {
                results.set(3, "");
            }
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return results;
    }

    boolean boolStringInvalid(String boolStr) {
        return !boolStr.equalsIgnoreCase("true") && !boolStr.equalsIgnoreCase("false") &&
            !boolStr.equalsIgnoreCase("yes") && !boolStr.equalsIgnoreCase("no") &&
            !boolStr.equalsIgnoreCase("1") && !boolStr.equalsIgnoreCase("0") &&
            !boolStr.equalsIgnoreCase("wahr") && !boolStr.equalsIgnoreCase("falsch") &&
            !boolStr.equalsIgnoreCase("ja") && !boolStr.equalsIgnoreCase("nein");
    }

    boolean parseBoolString(String boolStr) {
        return boolStr.equalsIgnoreCase("true") ||
            boolStr.equalsIgnoreCase("yes") ||
            boolStr.equalsIgnoreCase("1") ||
            boolStr.equalsIgnoreCase("wahr") ||
            boolStr.equalsIgnoreCase("ja");
    }

    int processRow(Sheet sheet, int rowIndex, int emptyLineCount) {
        int lineCount = rowIndex+1;

        //
        // read the fields from this row of the sheet
        //
        // Produktgruppe
        String gruppenname = sheet.getValueAt(0, rowIndex).toString();
        if (gruppenname.length() == 0){
            emptyLineCount++;
            logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte 1: Keine Produktgruppe).</div>\n";
            log.setText(logString+logStringEnd);
            return emptyLineCount;
        } else {
            emptyLineCount = 0;
        }
        // Lieferant
        String lieferant = sheet.getValueAt(1, rowIndex).toString();
        if (lieferant.length() == 0){ lieferant = "unbekannt"; }
        // Nummer
        String nummer = sheet.getValueAt(2, rowIndex).toString();
        if ( nummer.length() == 0 ){
            logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte 3: Keine Artikelnummer).</div>\n";
            log.setText(logString+logStringEnd);
            return emptyLineCount;
        }
        // Name
        String name = sheet.getValueAt(3, rowIndex).toString();
        if ( name.length() == 0 ){
            logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte 4: Keine Artikelbezeichnung).</div>\n";
            log.setText(logString+logStringEnd);
            return emptyLineCount;
        }
        // Kurzname
        String kurzname = sheet.getValueAt(4, rowIndex).toString();
        // Menge
        String menge = sheet.getValueAt(5, rowIndex).toString();
        // Einheit
        String einheit = sheet.getValueAt(6, rowIndex).toString();
        // Sortiment
        String sortiment = sheet.getValueAt(7, rowIndex).toString();
        if (sortiment.length() == 0){ sortiment = "false"; }
        // Lieferbar
        String lieferbar = sheet.getValueAt(8, rowIndex).toString();
        if (lieferbar.length() == 0){ lieferbar = "false"; }
        // Beliebtheit
        String beliebt = sheet.getValueAt(9, rowIndex).toString();
        // Barcode
        String barcode = sheet.getValueAt(10, rowIndex).toString();
        // VPE
        String vpe = sheet.getValueAt(11, rowIndex).toString();
        // Setgröße
        String setgroesse = sheet.getValueAt(12, rowIndex).toString();
        // Preise
        String vkpreis = sheet.getValueAt(13, rowIndex).toString();
        String empf_vkpreis = sheet.getValueAt(14, rowIndex).toString();
        String ekrabatt = sheet.getValueAt(15, rowIndex).toString();
        String ekpreis = sheet.getValueAt(16, rowIndex).toString();
        String variabel = sheet.getValueAt(17, rowIndex).toString();
        if (variabel.length() == 0){ variabel = "false"; }
        // Herkunft
        String herkunft = sheet.getValueAt(18, rowIndex).toString();
        // Bestand
        String bestand = sheet.getValueAt(19, rowIndex).toString();

        //System.out.println(lineCount+" "+gruppenname+" "+name);

        //
        // parse the fields
        //
        // Produktgruppe
        String gruppenid = queryGruppenID(gruppenname);
        if (gruppenid.equals("")){
            logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte A: Produktgruppe unbekannt).</div>\n";
            log.setText(logString+logStringEnd);
            return emptyLineCount;
        }
        // Lieferant
        Integer lieferantid = queryLieferantID(lieferant);
        if (lieferantid == null){
            logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte B: Lieferant unbekannt).</div>\n";
            log.setText(logString+logStringEnd);
            return emptyLineCount;
        }
        // Menge
        BigDecimal mengeDecimal = null;
        if (!menge.equals("")){
            try {
                mengeDecimal = new BigDecimal( menge.replace(',', '.') ).stripTrailingZeros();
                menge = mengeDecimal.toPlainString();
            } catch (NumberFormatException ex) {
                logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte E: 'Menge').</div>\n";
                log.setText(logString+logStringEnd);
                return emptyLineCount;
            }
        }
        // Sortiment
        if ( boolStringInvalid(sortiment) ){
            logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte H: 'Sortiment').</div>\n";
            log.setText(logString+logStringEnd);
            return emptyLineCount;
        }
        if ( parseBoolString(sortiment) ){
            sortiment = "1";
        } else {
            sortiment = "0";
        }
        // Lieferbar
        if ( boolStringInvalid(lieferbar) ){
            logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte I: 'Sofort lieferbar').</div>\n";
            log.setText(logString+logStringEnd);
            return emptyLineCount;
        }
        if ( parseBoolString(lieferbar) ){
            lieferbar = "1";
        } else {
            lieferbar = "0";
        }
        // Beliebtheit
        Integer beliebtWert = 0;
        if (!beliebt.equals("")){
            try {
                beliebtWert = beliebtWerte.get( beliebtNamen.indexOf(beliebt) );
            } catch (ArrayIndexOutOfBoundsException ex) {
                logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte J: 'Beliebtheit').</div>\n";
                log.setText(logString+logStringEnd);
                return emptyLineCount;
            }
        }
        // VPE
        Integer vpeInt = null;
        if (!vpe.equals("")){
            try {
                vpeInt = Integer.parseInt(vpe);
            } catch (NumberFormatException ex) {
                logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte I: 'VPE').</div>\n";
                log.setText(logString+logStringEnd);
                return emptyLineCount;
            }
        }
        // Setgröße
        Integer setgrInt = 1;
        if (!setgroesse.equals("")){
            try {
                setgrInt = Integer.parseInt(setgroesse);
            } catch (NumberFormatException ex) {
                logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte M: 'Setgröße').</div>\n";
                log.setText(logString+logStringEnd);
                return emptyLineCount;
            }
        } else {
            setgroesse = "1";
        }
        // Preise
        if ( boolStringInvalid(variabel) ){
            logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte L: 'Variabel').</div>\n";
            log.setText(logString+logStringEnd);
            return emptyLineCount;
        }
        if ( parseBoolString(variabel) ){
            variabel = "1";
            vkpreis = "";
            empf_vkpreis = "";
            ekrabatt = "";
            ekpreis = "";
        } else {
            variabel = "0";
            if ( !vkpreis.equals("") ){
                vkpreis = priceFormatterIntern(vkpreis);
                if ( vkpreis.equals("") ){
                    // price could not be parsed
                    logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte N: 'VK-Preis').</div>\n";
                    log.setText(logString+logStringEnd);
                    return emptyLineCount;
                }
            }
            if ( !empf_vkpreis.equals("") ){
                empf_vkpreis = priceFormatterIntern(empf_vkpreis);
                if ( empf_vkpreis.equals("") ){
                    // price could not be parsed
                    logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte O: 'Empf. VK-Preis').</div>\n";
                    log.setText(logString+logStringEnd);
                    return emptyLineCount;
                }
            }
            if ( !ekrabatt.equals("") ){
                ekrabatt = vatFormatter(ekrabatt);
                if ( ekrabatt.equals("") ){
                    // rabatt could not be parsed
                    logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte P: 'EK-Rabatt').</div>\n";
                    log.setText(logString+logStringEnd);
                    return emptyLineCount;
                }
            }
            if ( !ekpreis.equals("") ){
                ekpreis = priceFormatterIntern(ekpreis);
                if ( ekpreis.equals("") ){
                    // price could not be parsed
                    logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte Q: 'EK-Preis').</div>\n";
                    log.setText(logString+logStringEnd);
                    return emptyLineCount;
                }
            }
            String origEKPreis = ekpreis;
            ekpreis = figureOutEKP(empf_vkpreis, ekrabatt, ekpreis);
            if ( !origEKPreis.equals("") && !ekpreis.equals(origEKPreis) ){
                // if calculated ekpreis according to empf_vkpreis and ekrabatt does not match ekpreis:
                //    use the calculated ekpreis, but give a warning
                logString += "<div style=\""+redStyle+"\">EK-Preis von "+
                    priceFormatter(origEKPreis)+" "+currencySymbol+
                    "aus Zeile "+lineCount+" wird durch berechneten "+
                    "EK-Preis von "+priceFormatter(ekpreis)+" "+
                    currencySymbol+" ersetzt!.</div>\n";
                log.setText(logString+logStringEnd);
            }
        }
        // Bestand
        Integer bestandInt = null;
        if (!bestand.equals("")){
            try {
                bestandInt = Integer.parseInt(bestand);
            } catch (NumberFormatException ex) {
                logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte T: 'Bestand').</div>\n";
                log.setText(logString+logStringEnd);
                return emptyLineCount;
            }
        }

        Vector<Color> colors = new Vector<Color>();
        int itemAlreadyKnown = checkIfItemAlreadyKnown(lieferant, nummer);
        if (itemAlreadyKnown == 1){ // item already in db
            // compare every field:
            Vector<String> allFields = queryAllFields(lieferantid, nummer);

            colors.add( gruppenid.equals(allFields.get(0)) ? Color.black : Color.red ); // produktgruppe
            colors.add(Color.black); // lieferant
            colors.add(Color.black); // nummer
            colors.add( name.equals(allFields.get(1)) ? Color.black : Color.red ); // name
            colors.add( kurzname.equals(allFields.get(2)) ? Color.black : Color.red ); // kurzname
            colors.add( menge.equals(allFields.get(3)) ? Color.black : Color.red ); // menge
            colors.add( einheit.equals(allFields.get(4)) ? Color.black : Color.red ); // einheit
            colors.add( sortiment.equals(allFields.get(5)) ? Color.black : Color.red ); // sortiment
            colors.add( lieferbar.equals(allFields.get(6)) ? Color.black : Color.red ); // lieferbar
            colors.add( beliebtWert.toString().equals(allFields.get(7)) ? Color.black : Color.red ); // beliebt
            colors.add( barcode.equals(allFields.get(8)) ? Color.black : Color.red ); // barcode
            colors.add( vpe.equals(allFields.get(9)) ? Color.black : Color.red ); // vpe
            colors.add( setgroesse.equals(allFields.get(10)) ? Color.black : Color.red ); // setgroesse
            if (variabel.equals("0")){ // compare prices:
                colors.add( vkpreis.equals(allFields.get(11)) ? Color.black : Color.red ); // vkpreis
                colors.add( empf_vkpreis.equals(allFields.get(12)) ? Color.black : Color.red ); // empf_vkpreis
                colors.add( ekrabatt.equals(allFields.get(13)) ? Color.black : Color.red ); // ekrabatt
                colors.add( ekpreis.equals(allFields.get(14)) ? Color.black : Color.red ); // ekpreis
            } else {
                colors.add(Color.black);
                colors.add(Color.black);
                colors.add(Color.black);
                colors.add(Color.black);
            }
            colors.add( variabel.equals(allFields.get(15)) ? Color.black : Color.red ); // variabel
            colors.add( herkunft.equals(allFields.get(16)) ? Color.black : Color.red ); // herkunft
            colors.add( bestand.equals(allFields.get(17)) ? Color.black : Color.red ); // bestand
            colors.add(Color.black); // entfernen
        } else {
            for (int i=0; i<20; i++){
                colors.add(Color.black);
            }
        }
        boolean itemChanged = false;
        for (int i=0; i<colors.size(); i++){
            if (colors.get(i) == Color.red){
                itemChanged = true;
                System.out.println("Row "+lineCount+", Change in column "+i);
                //break;
            }
        }
        if ( itemAlreadyKnown == 0 || (itemChanged && itemAlreadyKnown != 2) ){ // if item not known or item changed
            // add new item to the list
            artikelNeu.selProduktgruppenIDs.add( Integer.parseInt(gruppenid) );
            artikelNeu.selLieferantIDs.add(lieferantid);
            artikelNeu.lieferanten.add(lieferant);
            artikelNeu.artikelNummern.add(nummer);
            artikelNeu.artikelNamen.add(name);
            artikelNeu.kurznamen.add(kurzname);
            artikelNeu.mengen.add(mengeDecimal);
            artikelNeu.einheiten.add(einheit);
            artikelNeu.sortimente.add( sortiment.equals("0") ? false : true );
            artikelNeu.lieferbarBools.add( lieferbar.equals("0") ? false : true );
            artikelNeu.beliebtWerte.add(beliebtWert);
            artikelNeu.barcodes.add(barcode);
            artikelNeu.vpes.add(vpeInt);
            artikelNeu.sets.add(setgrInt);
            artikelNeu.vkPreise.add(vkpreis);
            artikelNeu.empfvkPreise.add(empf_vkpreis);
            artikelNeu.ekRabatte.add(ekrabatt);
            artikelNeu.ekPreise.add(ekpreis);
            artikelNeu.variablePreise.add( variabel.equals("0") ? false : true );
            artikelNeu.herkuenfte.add(herkunft);
            artikelNeu.bestaende.add(bestandInt);

            artikelNeu.removeButtons.add(new JButton("-"));
            artikelNeu.removeButtons.lastElement().addActionListener(this);
            artikelNeu.colorMatrix.add(colors);

            Vector<Object> row = new Vector<Object>();
            row.add(gruppenname);
            row.add(lieferant);
            row.add(nummer);
            row.add(name);
            row.add(kurzname);
            row.add( unifyDecimal(menge) );
            row.add(einheit);
            row.add( sortiment.equals("0") ? false : true );
            row.add( lieferbar.equals("0") ? false : true );
            row.add(beliebt);
            row.add(barcode);
            row.add(vpe);
            row.add(setgroesse);
            row.add( vkpreis.equals("") ? "" : priceFormatter(vkpreis)+" "+currencySymbol );
            row.add( empf_vkpreis.equals("") ? "" : priceFormatter(empf_vkpreis)+" "+currencySymbol );
            row.add( ekrabatt.equals("") ? "" : ekrabatt );
            row.add( ekpreis.equals("") ? "" : priceFormatter(ekpreis)+" "+currencySymbol );
            row.add( variabel.equals("0") ? false : true );
            row.add(herkunft);
            row.add(bestand);
            row.add(artikelNeu.removeButtons.lastElement());
            artikelNeu.data.add(row);
        }
        if (itemAlreadyKnown == 0){
            //logString += "<div style=\""+baseStyle+"\">Artikel \""+ name + "\" von "+lieferant+" mit Nr. "+nummer+" wird hinzugefügt.</div>\n";
            //log.setText(logString+logStringEnd);
        }
        else if (itemAlreadyKnown == 2){ // item already in table
            logString += "<div style=\""+redStyle+"\">Artikel \""+ name + "\" von "+lieferant+" mit Nr. "+nummer+" wird nicht erneut hinzugefügt/verändert.</div>\n";
            log.setText(logString+logStringEnd);
        }
        else if (itemChanged){
            logString += "<div style=\""+baseStyle+"\">Artikel \""+ name + "\" von "+lieferant+" mit Nr. "+nummer+" wird verändert.</div>\n";
            log.setText(logString+logStringEnd);
        }

        return emptyLineCount;
    }

    class Task extends SwingWorker<Void, Void> {
        /*
         * Main task. Executed in background thread. Following
         * http://docs.oracle.com/javase/tutorial/displayCode.html?code=http://docs.oracle.com/javase/tutorial/uiswing/examples/components/ProgressBarDemoProject/src/components/ProgressBarDemo.java
         * See also: http://www.javacreed.com/swing-worker-example/
         */
        @Override
        public Void doInBackground() {
            int emptyLineCount = 0;
            int size = sheet.getRowCount();
            //Initialize progress property.
            setProgress(1);
            for (int rowIndex=1; rowIndex<size; rowIndex++) {
                // ^ ignore first line with table header (column labels)
                emptyLineCount = processRow(sheet, rowIndex, emptyLineCount);
                if (emptyLineCount >= 10) break; // don't do an endless loop, stop after 10 empty lines
                setProgress((rowIndex + 1) * 100 / size); // needs to be int between 0 and 100
            }
            return null;
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            setCursor(null); //turn off the wait cursor
            System.out.println("Datei " + file.getName() + " wurde komplett eingelesen.");
            logString += "<div style=\""+baseStyle+"\">Datei " + file.getName() + " wurde komplett eingelesen.</div>\n";
            log.setText(logString+logStringEnd);
            updateAll();
        }
    }


    void parseFile() {
        logString += "<div>-------</div>\n";
        logString += "<div style=\""+baseStyle+"\">Datei " + file.getName() + " wird geöffnet...</div>\n";
        log.setText(logString+logStringEnd);

        // Load the file
        try {
            sheet = SpreadSheet.createFromFile(file).getSheet(0);
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        // initiate progress bar:
        progressBar.setMinimum(1);
        progressBar.setMaximum(sheet.getRowCount());

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        //Instances of javax.swing.SwingWorker are not reusuable, so
        //we create new instances as needed.
        readInTask = new Task();
        readInTask.addPropertyChangeListener(this);
        readInTask.execute();
    }

    /**
     * Invoked when task's progress property changes.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName()) {
            int progress = (Integer)evt.getNewValue();
            progressBar.setValue(progress);
            //log.append(String.format(
            //            "Completed %d%% of task.\n", readInTask.getProgress()));
        }
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
	if (e.getSource() == fileButton){
            int returnVal = odsChooser.showOpenDialog(this.window);
            if (returnVal == JFileChooser.APPROVE_OPTION){
                file = odsChooser.getSelectedFile();
                parseFile();
                //utf.updateTable();
                updateAll();

                System.out.println("Opened " + file.getName());
            } else {
                System.out.println("Open command cancelled by user.");
            }
            return;
        }
	if (e.getSource() == submitButton){
            submit();
            artikelListe.fillDataArray();
            artikelListe.updateTable();
            emptyTable();
            //utf.updateTable();
            updateAll();
            return;
        }
	if (e.getSource() == deleteButton){
            emptyTable();
            //utf.updateTable();
            updateAll();
            return;
        }
        super.actionPerformed(e);
        artikelNeu.actionPerformed(e);
    }
}
