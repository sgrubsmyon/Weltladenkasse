package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.io.*; // for File
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

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

public class ArtikelReadIn extends ArtikelDialogWindowGrundlage implements ArtikelNeuInterface {
    // Attribute:
    protected ArtikelNeu artikelNeu;
    protected UpdateTableFunctor utf;

    private final String delimiter = ";";
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
    public ArtikelReadIn(Connection conn, MainWindowGrundlage mw, Artikelliste pw, JDialog dia) {
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

    public int checkIfItemAlreadyKnown(String name, String nummer) {
        return artikelNeu.checkIfItemAlreadyKnown(name, nummer);
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
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT produktgruppen_id FROM produktgruppe WHERE produktgruppen_name = '"+gruppenname+"'"
                    );
            if ( rs.next() )
                gruppenid = rs.getString(1) == null ? "NULL" : rs.getString(1);
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return gruppenid;
    }

    private String queryLieferantID(String lieferantname) {
        String lieferantid = "NULL";
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT lieferant_id FROM lieferant WHERE lieferant_name = '"+lieferantname+"'"
                    );
            if ( rs.next() )
                lieferantid = rs.getString(1) == null ? "NULL" : rs.getString(1);
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return lieferantid;
    }

    private Vector<String> queryAllFields(String artikelname, String artikelnummer) {
        Vector<String> results = new Vector<String>();
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT produktgruppen_id, barcode, vk_preis, ek_preis, variabler_preis, vpe, " +
                    "lieferant_id, herkunft FROM artikel "+
                    "WHERE artikel_name = '"+artikelname+"' AND artikel_nr = '"+artikelnummer+"' AND aktiv = TRUE"
                    );
            rs.next();
            results.add(rs.getString(1)); // produktgruppen_id
            results.add(rs.getString(2) == null ? "NULL" : rs.getString(2)); // barcode
            results.add(rs.getString(3) == null ? "NULL" : rs.getString(3)); // vk_preis
            results.add(rs.getString(4) == null ? "NULL" : rs.getString(4)); // ek_preis
            results.add(rs.getString(5));                                    // variabler_preis
            results.add(rs.getString(6) == null ? "NULL" : rs.getString(6)); // vpe
            results.add(rs.getString(7) == null ? "NULL" : rs.getString(7)); // lieferant_id
            results.add(rs.getString(8) == null ? "NULL" : rs.getString(8)); // herkunft
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return results;
    }

    void parseFile(File file) {
        logString += "<div>-------</div>\n";
        logString += "<div style=\""+baseStyle+"\">Datei " + file.getName() + " wird geöffnet...</div>\n";
        log.setText(logString+logStringEnd);
        try {
            //// use InputStream classes for binary files:
            //FileInputStream fis = new FileInputStream(file);
            //// Here BufferedInputStream is added for fast reading.
            //BufferedInputStream bis = new BufferedInputStream(fis);
            //DataInputStream in = new DataInputStream(bis);
            //fis.close();
            //bis.close();
            // use Reader classes for text files:
            BufferedReader in = new BufferedReader(new FileReader(file)); // Lesen einer Textdatei mit Default Zeichensatz-Codierung, see http://www.wsoftware.de/practices/charsets.html
            int lineCount = 0;
            String line = "";
            while ( (line = in.readLine()) != null) {
                System.out.println(line);
                lineCount++;
                line = line.replaceAll("#.*","");
                line = line.replaceAll("\"","");
                line = line.replaceAll("\'","\\\\\'"); // four backslashes are for one! See: http://www.xyzws.com/javafaq/how-many-backslashes/198

                // get the fields
                String[] fields = line.split(delimiter);
                if (fields.length < 5 ){
                    logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (nicht genug Felder).</div>\n";
                    log.setText(logString+logStringEnd);
                    continue;
                }
                String gruppenname = fields[0];
                String name = fields[1];
                String nummer = fields[2];
                String barcode = fields[3].length() == 0 ? "NULL" : fields[3];
                String vkpreis = fields[4];
                String ekpreis = "NULL";
                if (fields.length > 5)
                    ekpreis = fields[5].length() == 0 ? "NULL" : fields[5];
                String variabel = "false";
                if (fields.length > 6)
                    variabel = fields[6].length() == 0 ? "false" : fields[6];
                String vpe = "NULL";
                if (fields.length > 7)
                    vpe = fields[7].length() == 0 ? "NULL" : fields[7];
                String lieferant = "";
                if (fields.length > 8)
                    lieferant = fields[8];
                String herkunft = "NULL";
                if (fields.length > 9)
                    herkunft = fields[9].length() == 0 ? "NULL" : fields[9];

                // parse the fields
                String gruppenid = queryGruppenID(gruppenname);
                if (gruppenid.equals("NULL")){
                    logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte 1: Produktgruppe unbekannt).</div>\n";
                    log.setText(logString+logStringEnd);
                    continue;
                }
                if ( name.length() == 0 ){
                    logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte 2: Kein Artikelname).</div>\n";
                    log.setText(logString+logStringEnd);
                    continue;
                }
                if ( nummer.length() == 0 ){
                    logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte 3: Keine Artikelnummer).</div>\n";
                    log.setText(logString+logStringEnd);
                    continue;
                }
                if ( !variabel.equalsIgnoreCase("true") && !variabel.equalsIgnoreCase("false") &&
                        !variabel.equalsIgnoreCase("yes") && !variabel.equalsIgnoreCase("no") &&
                        !variabel.equalsIgnoreCase("1") && !variabel.equalsIgnoreCase("0") &&
                        !variabel.equalsIgnoreCase("wahr") && !variabel.equalsIgnoreCase("falsch") &&
                        !variabel.equalsIgnoreCase("ja") && !variabel.equalsIgnoreCase("nein") ){
                    logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte 7: 'Variabel').</div>\n";
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
                    try {
                        vkpreis = priceFormatterIntern(vkpreis);
                    } catch (NumberFormatException ex) {
                        logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte 5: 'VK-Preis').</div>\n";
                        log.setText(logString+logStringEnd);
                        continue;
                    }
                    if ( !ekpreis.equals("NULL") ){
                        try {
                            ekpreis = priceFormatterIntern(ekpreis);
                        } catch (NumberFormatException ex) {
                            logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte 6: 'EK-Preis').</div>\n";
                            log.setText(logString+logStringEnd);
                            continue;
                        }
                    }
                }
                String lieferantid = "";
                if (lieferant.length() == 0){
                    lieferantid = "1";
                    lieferant = "unbekannt";
                }
                else {
                    lieferantid = queryLieferantID(lieferant);
                }
                if (lieferantid.equals("NULL")){
                    logString += "<div style=\""+redStyle+"\">Zeile "+lineCount+" wurde ignoriert (Fehler in Spalte 8: Lieferant unbekannt).</div>\n";
                    log.setText(logString+logStringEnd);
                    continue;
                }

                Vector<Color> colors = new Vector<Color>();
                int itemAlreadyKnown = checkIfItemAlreadyKnown(name, nummer);
                if (itemAlreadyKnown == 1){ // item already in db
                    // compare every field:
                    Vector<String> allFields = queryAllFields(name, nummer);

                    colors.add( gruppenid.equals(allFields.get(0)) ? Color.black : Color.red ); // produktgruppe
                    colors.add(Color.black); // name
                    colors.add(Color.black); // nummer
                    colors.add( barcode.equals(allFields.get(1)) ? Color.black : Color.red ); // barcode
                    if (variabel.equals("0")){ // compare prices:
                        colors.add( vkpreis.equals(allFields.get(2)) ? Color.black : Color.red ); // vkpreis
                        colors.add( ekpreis.equals(allFields.get(3)) ? Color.black : Color.red ); // ekpreis
                    } else {
                        colors.add(Color.black);
                        colors.add(Color.black);
                    }
                    colors.add( variabel.equals(allFields.get(4)) ? Color.black : Color.red ); // variabel
                    colors.add( vpe.equals(allFields.get(5)) ? Color.black : Color.red ); // vpe
                    colors.add( lieferantid.equals(allFields.get(6)) ? Color.black : Color.red ); // lieferantid
                    colors.add( herkunft.equals(allFields.get(7)) ? Color.black : Color.red ); // herkunft
                    colors.add(Color.black); // entfernen
                }
                else {
                    for (int i=0; i<10; i++){
                        colors.add(Color.black);
                    }
                }
                boolean itemChanged = false;
                for (int i=0; i<9; i++){
                    if (colors.get(i) == Color.red){
                        itemChanged = true;
                        break;
                    }
                }
                if ( itemAlreadyKnown == 0 || (itemChanged && itemAlreadyKnown != 2) ){ // if item not known or item changed
                    // add new item to the list
                    artikelNeu.artikelNamen.add(name);
                    artikelNeu.artikelNummern.add(nummer);
                    artikelNeu.barcodes.add(barcode);
                    artikelNeu.vkPreise.add(vkpreis);
                    artikelNeu.ekPreise.add(ekpreis);
                    artikelNeu.variablePreise.add(variabel);
                    artikelNeu.vpes.add(vpe);
                    artikelNeu.selLieferantIDs.add(lieferantid);
                    artikelNeu.selProduktgruppenIDs.add(gruppenid);
                    artikelNeu.herkuenfte.add(herkunft);
                    artikelNeu.removeButtons.add(new JButton("-"));
                    artikelNeu.removeButtons.lastElement().addActionListener(this);
                    artikelNeu.colorMatrix.add(colors);

                    Vector<Object> row = new Vector<Object>();
                    row.add(gruppenname);
                    row.add(name);
                    row.add(nummer);
                    row.add( barcode == "NULL" ? "" : barcode );
                    row.add( vkpreis.equals("NULL") ? "" : vkpreis.replace('.',',')+" "+currencySymbol );
                    row.add( ekpreis.equals("NULL") ? "" : ekpreis.replace('.',',')+" "+currencySymbol );
                    row.add( variabel.equals("TRUE") ? true : false );
                    row.add( vpe.equals("NULL") ? "" : vpe );
                    row.add( barcode == "NULL" ? "" : barcode );
                    row.add(lieferant);
                    row.add( herkunft.equals("NULL") ? "" : herkunft );
                    row.add(artikelNeu.removeButtons.lastElement());
                    artikelNeu.data.add(row);
                }
                if (itemAlreadyKnown == 0){
                    logString += "<div style=\""+baseStyle+"\">Artikel \""+ name + "\" wird hinzugefügt.</div>\n";
                    log.setText(logString+logStringEnd);
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
            in.close();
        } catch (FileNotFoundException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
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
            int returnVal = fc.showOpenDialog(this.window);
            if (returnVal == JFileChooser.APPROVE_OPTION){
                File file = fc.getSelectedFile();

                // This is where a real application would open the file.
                parseFile(file);
                //utf.updateTable();
                updateAll();

                System.out.println("Opened " + file.getName() + ".");
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
