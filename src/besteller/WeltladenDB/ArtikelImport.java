package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.io.*; // for File
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// OpenDocument stuff:
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.jopendocument.dom.OOUtils;

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
//import javax.swing.filechooser.*;
import javax.swing.text.html.HTMLDocument;

public class ArtikelImport extends ArtikelDialogWindowGrundlage implements ArtikelNeuInterface {
    // Attribute:
    protected ArtikelNeu artikelNeu;
    protected UpdateTableFunctor utf;

    private JButton fileButton;
    private JFileChooser fc;
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
	super(conn, mw, pw, dia);
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

    void showHeader() {
        headerPanel = new JPanel();
	headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setMinimumSize(minLogDimension);
        headerPanel.setPreferredSize(prefLogDimension);

        fc = new JFileChooser();

        JPanel fileButtonPanel = new JPanel();
        fileButton = new JButton("Datei auswählen");
        fileButton.setMnemonic(KeyEvent.VK_D);
	fileButton.addActionListener(this);
	//fileButton.setEnabled(artikelNeu.data.size()==0);
        fileButtonPanel.add(fileButton);
        headerPanel.add(fileButtonPanel);

        //JPanel logPanel = new JPanel();
        //logPanel.setBorder(BorderFactory.createTitledBorder("Log"));
        log = new JEditorPane();
        //log.setMargin(new Insets(5,5,5,5));
        log.setEditable(false);
        log.setContentType("text/html");
        log.setMinimumSize(minLogDimension);
        log.setPreferredSize(prefLogDimension);
        //System.out.println(logString);
        log.setText(logString+logStringEnd);
        //StringReader sr = new StringReader(logString+logStringEnd);
        //try {
        //    log.read(sr, new HTMLDocument());
        //} catch (IOException ex) {
        //    System.out.println("Exception: " + ex.getMessage());
        //    ex.printStackTrace();
        //}
        JScrollPane logScrollPane = new JScrollPane(log);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log"));
        //logScrollPane.setBorder( BorderFactory.createEmptyBorder(5,5,5,5) );
        logScrollPane.setMinimumSize(minLogDimension);
        logScrollPane.setPreferredSize(prefLogDimension);
        //logPanel.add(logScrollPane);
        //headerPanel.add(logPanel);
        //
        headerPanel.add(logScrollPane);
    }

    void showMiddle() {
        artikelNeu.showTable(allPanel);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                headerPanel,
                artikelNeu.tablePanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.3);
        allPanel.add(splitPane);
    }

    void showFooter() {
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
    boolean willDataBeLost() {
        return artikelNeu.willDataBeLost();
    }

    public int checkIfItemAlreadyKnown(String lieferant, String nummer) {
        return artikelNeu.checkIfItemAlreadyKnown(lieferant, nummer);
    }

    public void submit() {
        artikelNeu.submit();
    }

    public void emptyTable() {
        artikelNeu.emptyTable();
    }

    //////////////////
    // DB functions
    //////////////////
    private String queryGruppenID(String gruppenname) {
        String gruppenid = "NULL";
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT produktgruppen_id FROM produktgruppe WHERE produktgruppen_name = ?"
                    );
            pstmt.setString(1, gruppenname);
            ResultSet rs = pstmt.executeQuery();
            if ( rs.next() )
                gruppenid = rs.getString(1) == null ? "NULL" : rs.getString(1);
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
                    "SELECT produktgruppen_id, artikel_name, menge, barcode, herkunft, "+
                    "vpe, vk_preis, ek_preis, variabler_preis, sortiment " +
                    "FROM artikel "+
                    "WHERE lieferant_id = ? AND artikel_nr = ? AND aktiv = TRUE"
                    );
            pstmt.setInt(1, lieferantid);
            pstmt.setString(2, artikelnummer);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            results.add(rs.getString(1)); // produktgruppen_id
            results.add(rs.getString(2)); // artikel_name
            results.add(rs.getString(3) == null ? "NULL" : rs.getString(3)); // menge
            results.add(rs.getString(4) == null ? "NULL" : rs.getString(4)); // barcode
            results.add(rs.getString(5) == null ? "NULL" : rs.getString(5)); // herkunft
            results.add(rs.getString(6) == null ? "NULL" : rs.getString(6)); // vpe
            results.add(rs.getString(7) == null ? "NULL" : rs.getString(7)); // vk_preis
            results.add(rs.getString(8) == null ? "NULL" : rs.getString(8)); // ek_preis
            results.add(rs.getString(9));                                    // variabler_preis
            results.add(rs.getString(10));                                   // sortiment
            rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return results;
    }

    void parseFile(File file) {
        logString += "<div>-------</div>\n";
        logString += "<div style=\""+baseStyle+"\">Datei " + file.getName() + " wurde geöffnet...</div>\n";
        log.setText(logString+logStringEnd);

        // Load the file
        final Sheet sheet;
        try {
            sheet = SpreadSheet.createFromFile(file).getSheet(0);
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        for (int rowIndex = 1; rowIndex<sheet.getRowCount(); rowIndex++) {
            // ^ ignore first line with table header (column labels)
            int lineCount = rowIndex+1;

            String gruppenname = (String)sheet.getValueAt(0, rowIndex);
            if (gruppenname.length() == 0){
                logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte 1: Keine Produktgruppe).</div>\n";
                log.setText(logString+logStringEnd);
                continue;
            }
            String lieferant = (String)sheet.getValueAt(1, rowIndex);
            if (lieferant.length() == 0){ lieferant = "unbekannt"; }
            String nummer = (String)sheet.getValueAt(2, rowIndex);
            if ( nummer.length() == 0 ){
                logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte 3: Keine Artikelnummer).</div>\n";
                log.setText(logString+logStringEnd);
                continue;
            }
            String name = (String)sheet.getValueAt(3, rowIndex);
            if ( name.length() == 0 ){
                logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte 4: Kein Artikelname).</div>\n";
                log.setText(logString+logStringEnd);
                continue;
            }
            String menge = (String)sheet.getValueAt(4, rowIndex);
            if (menge.length() == 0){ menge = "NULL"; }
            String barcode = (String)sheet.getValueAt(5, rowIndex);
            if (barcode.length() == 0){ barcode = "NULL"; }
            String herkunft = (String)sheet.getValueAt(6, rowIndex);
            if (herkunft.length() == 0){ herkunft = "NULL"; }
            String vpe = (String)sheet.getValueAt(7, rowIndex);
            if (vpe.length() == 0){ vpe = "NULL"; }
            String vkpreis = (String)sheet.getValueAt(8, rowIndex);
            if (vkpreis.length() == 0){ vkpreis = "NULL"; }
            String ekpreis = (String)sheet.getValueAt(9, rowIndex);
            if (ekpreis.length() == 0){ ekpreis = "NULL"; }
            String variabel = (String)sheet.getValueAt(10, rowIndex);
            if (variabel.length() == 0){ variabel = "false"; }
            String sortiment = (String)sheet.getValueAt(11, rowIndex);
            if (sortiment.length() == 0){ sortiment = "false"; }

            System.out.println(lineCount+" "+gruppenname+" "+name);

            // parse the fields
            String gruppenid = queryGruppenID(gruppenname);
            if (gruppenid.equals("NULL")){
                logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte A: Produktgruppe unbekannt).</div>\n";
                log.setText(logString+logStringEnd);
                continue;
            }
            if ( !variabel.equalsIgnoreCase("true") && !variabel.equalsIgnoreCase("false") &&
                    !variabel.equalsIgnoreCase("yes") && !variabel.equalsIgnoreCase("no") &&
                    !variabel.equalsIgnoreCase("1") && !variabel.equalsIgnoreCase("0") &&
                    !variabel.equalsIgnoreCase("wahr") && !variabel.equalsIgnoreCase("falsch") &&
                    !variabel.equalsIgnoreCase("ja") && !variabel.equalsIgnoreCase("nein") ){
                logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte K: 'Variabel').</div>\n";
                log.setText(logString+logStringEnd);
                continue;
                    }
            if ( variabel.equalsIgnoreCase("true") || variabel.equalsIgnoreCase("yes") || variabel.equalsIgnoreCase("1") ||
                    variabel.equalsIgnoreCase("wahr") || variabel.equalsIgnoreCase("ja") ){
                variabel = "1";
                vkpreis = "NULL";
                ekpreis = "NULL";
            } else {
                variabel = "0";
                if ( !vkpreis.equals("NULL") ){
                    try {
                        vkpreis = priceFormatterIntern(vkpreis);
                    } catch (NumberFormatException ex) {
                        logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte I: 'VK-Preis').</div>\n";
                        log.setText(logString+logStringEnd);
                        continue;
                    }
                }
                if ( !ekpreis.equals("NULL") ){
                    try {
                        ekpreis = priceFormatterIntern(ekpreis);
                    } catch (NumberFormatException ex) {
                        logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte J: 'EK-Preis').</div>\n";
                        log.setText(logString+logStringEnd);
                        continue;
                    }
                }
            }
            if ( !sortiment.equalsIgnoreCase("true") && !sortiment.equalsIgnoreCase("false") &&
                    !sortiment.equalsIgnoreCase("yes") && !sortiment.equalsIgnoreCase("no") &&
                    !sortiment.equalsIgnoreCase("1") && !sortiment.equalsIgnoreCase("0") &&
                    !sortiment.equalsIgnoreCase("wahr") && !sortiment.equalsIgnoreCase("falsch") &&
                    !sortiment.equalsIgnoreCase("ja") && !sortiment.equalsIgnoreCase("nein") ){
                logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte L: 'Sortiment').</div>\n";
                log.setText(logString+logStringEnd);
                continue;
                    }
            if ( sortiment.equalsIgnoreCase("true") || sortiment.equalsIgnoreCase("yes") || sortiment.equalsIgnoreCase("1") ||
                    sortiment.equalsIgnoreCase("wahr") || sortiment.equalsIgnoreCase("ja") ){
                sortiment = "1";
            } else {
                sortiment = "0";
            }
            Integer lieferantid = queryLieferantID(lieferant);
            if (lieferantid == null){
                logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte B: Lieferant unbekannt).</div>\n";
                log.setText(logString+logStringEnd);
                continue;
            }
            Integer vpeInt = null;
            if (!vpe.equals("NULL")){
                try {
                    vpeInt = Integer.parseInt(vpe);
                } catch (NumberFormatException ex) {
                    logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte H: 'VPE').</div>\n";
                    log.setText(logString+logStringEnd);
                    continue;
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
                colors.add( menge.equals(allFields.get(2)) ? Color.black : Color.red ); // menge
                colors.add( barcode.equals(allFields.get(3)) ? Color.black : Color.red ); // barcode
                colors.add( herkunft.equals(allFields.get(4)) ? Color.black : Color.red ); // herkunft
                colors.add( vpe.equals(allFields.get(5)) ? Color.black : Color.red ); // vpe
                if (variabel.equals("0")){ // compare prices:
                    colors.add( vkpreis.equals(allFields.get(6)) ? Color.black : Color.red ); // vkpreis
                    colors.add( ekpreis.equals(allFields.get(7)) ? Color.black : Color.red ); // ekpreis
                } else {
                    colors.add(Color.black);
                    colors.add(Color.black);
                }
                colors.add( variabel.equals(allFields.get(8)) ? Color.black : Color.red ); // variabel
                colors.add( sortiment.equals(allFields.get(9)) ? Color.black : Color.red ); // sortiment
                colors.add(Color.black); // entfernen
            }
            else {
                for (int i=0; i<13; i++){
                    colors.add(Color.black);
                }
            }
            boolean itemChanged = false;
            for (int i=0; i<colors.size(); i++){
                if (colors.get(i) == Color.red){
                    itemChanged = true;
                    break;
                }
            }
            if ( itemAlreadyKnown == 0 || (itemChanged && itemAlreadyKnown != 2) ){ // if item not known or item changed
                // add new item to the list
                artikelNeu.selProduktgruppenIDs.add( Integer.parseInt(gruppenid) );
                artikelNeu.selLieferantIDs.add(lieferantid);
                artikelNeu.lieferanten.add(lieferant);
                artikelNeu.artikelNummern.add(nummer);
                artikelNeu.artikelNamen.add(name);
                artikelNeu.mengen.add(menge);
                artikelNeu.barcodes.add(barcode);
                artikelNeu.herkuenfte.add(herkunft);
                artikelNeu.vpes.add(vpeInt);
                artikelNeu.vkPreise.add(vkpreis);
                artikelNeu.ekPreise.add(ekpreis);
                artikelNeu.variablePreise.add( variabel.equals("0") ? false : true );
                artikelNeu.sortimente.add( sortiment.equals("0") ? false : true );
                artikelNeu.removeButtons.add(new JButton("-"));
                artikelNeu.removeButtons.lastElement().addActionListener(this);
                artikelNeu.colorMatrix.add(colors);

                Vector<Object> row = new Vector<Object>();
                    row.add(gruppenname);
                    row.add(lieferant);
                    row.add(nummer);
                    row.add(name);
                    row.add( menge == "NULL" ? "" : menge );
                    row.add( barcode == "NULL" ? "" : barcode );
                    row.add( herkunft.equals("NULL") ? "" : herkunft );
                    row.add( vpe.equals("NULL") ? "" : vpe );
                    row.add( vkpreis.equals("NULL") ? "" : vkpreis.replace('.',',')+" "+currencySymbol );
                    row.add( ekpreis.equals("NULL") ? "" : ekpreis.replace('.',',')+" "+currencySymbol );
                    row.add( variabel.equals("0") ? false : true );
                    row.add( sortiment.equals("0") ? false : true );
                    row.add(artikelNeu.removeButtons.lastElement());
                artikelNeu.data.add(row);
            }
            if (itemAlreadyKnown == 0){
                //logString += "<div style=\""+baseStyle+"\">Artikel \""+ name + "\" wird hinzugefügt.</div>\n";
                //log.setText(logString+logStringEnd);
                log.updateUI();
            }
            else if (itemAlreadyKnown == 2){ // item already in table
                logString += "<div style=\""+redStyle+"\">Artikel \""+ name + "\" wird nicht erneut hinzugefügt/verändert.</div>\n";
                log.setText(logString+logStringEnd);
                log.updateUI();
            }
            else if (itemChanged){
                logString += "<div style=\""+baseStyle+"\">Artikel \""+ name + "\" wird verändert.</div>\n";
                log.setText(logString+logStringEnd);
                log.updateUI();
            }
        }
        logString += "<div style=\""+baseStyle+"\">Datei " + file.getName() + " wurde komplett eingelesen.</div>\n";
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
	if (e.getSource() == fileButton){
            int returnVal = fc.showOpenDialog(this.window);
            if (returnVal == JFileChooser.APPROVE_OPTION){
                File file = fc.getSelectedFile();

                // This is where a real application would open the file.
                parseFile(file);
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
            artikelListe.updateAll();
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
