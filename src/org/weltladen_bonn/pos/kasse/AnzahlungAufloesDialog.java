package org.weltladen_bonn.pos.kasse;

import org.weltladen_bonn.pos.BaseClass;
import org.weltladen_bonn.pos.DialogWindow;
import org.weltladen_bonn.pos.MainWindowGrundlage;
import org.weltladen_bonn.pos.BaseClass.BigLabel;
import org.weltladen_bonn.pos.IntegerDocumentFilter;
import org.weltladen_bonn.pos.AnyJComponentJTable;
import org.weltladen_bonn.pos.ArticleSelectTable;

// Basic Java stuff:
import java.util.Vector;
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.math.RoundingMode;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AnzahlungAufloesDialog extends DialogWindow {
    private static final Logger logger = LogManager.getLogger(AnzahlungAufloesDialog.class);

    private int pageNumber = 1;
    private int selRechNr;
    protected Vector<Integer> rechnungsNummern;
    private Vector<Vector<Object>> anzahlungData;
    private Vector<String> anzahlungLabels;
    private Vector<KassierArtikel> kassierArtikel;
    private Vector< Vector<Object> > anzahlungDetailData;
    private Vector<String> anzahlungDetailColors;

    private JSplitPane splitPane;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private JButton loadMoreButton;
    private JPanel anzahlungTablePanel;
    private JPanel anzahlungDetailTablePanel;
    private JScrollPane anzahlungDetailScrollPane;
    protected AnyJComponentJTable anzahlungTable;
    private ArticleSelectTable anzahlungDetailTable;

    private JButton okButton;
    private JButton cancelButton;
    private boolean aborted = true;

    // Die Ausrichter:
    protected final String einrueckung = "      ";

    // Methoden:
    public AnzahlungAufloesDialog(MariaDbPoolDataSource pool, MainWindowGrundlage mw, JDialog dia) {
        super(pool, mw, dia);
        selRechNr = -1;
        showAll();
    }

    // will data be lost on close?
    protected boolean willDataBeLost() {
        return false;
    }

    protected void showHeader() {
        /**
         * Information-Panel
         * */
        headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        
        // borders:
        int top = 5, left = 5, bottom = 5, right = 5;
        headerPanel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));

        JTextArea erklaerText = new JTextArea(1, 30);
        erklaerText.append("Welche offene Anzahlung soll aufgelöst werden?");
        erklaerText = makeLabelStyle(erklaerText);
        erklaerText.setFont(BaseClass.mediumFont);
        erklaerText.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        headerPanel.add(erklaerText);

        allPanel.add(headerPanel, BorderLayout.NORTH);
    }

    protected void showMiddle() {
        /**
         * Main-Panel
         * */
        leftPanel = new JPanel();
        rightPanel = new JPanel();
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            leftPanel, rightPanel);
        splitPane.setOneTouchExpandable(true);
        //splitPane.setResizeWeight(0.3);
        splitPane.setDividerLocation(0.3);
        allPanel.add(splitPane, BorderLayout.CENTER);
        showTables();
    }

    void showTables() {
        anzahlungData = new Vector< Vector<Object> >();
        rechnungsNummern = new Vector<Integer>();
        retrieveAnzahlungData();
        showLeftPanel();
        showRightPanel(selRechNr);
    }

    void showLeftPanel() {
        leftPanel.setLayout(new BorderLayout());

        // Panel for the "Load more" button and anzahlung table
        anzahlungTablePanel = new JPanel(new BorderLayout());

        JPanel loadMorePanel = new JPanel();
        loadMorePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        loadMoreButton = new JButton("Mehr laden");
        loadMoreButton.addActionListener(this);
        loadMorePanel.add(loadMoreButton);
        anzahlungTablePanel.add(loadMorePanel, BorderLayout.NORTH);

        anzahlungLabels = new Vector<String>();
        anzahlungLabels.add("Rechnungs-Nr.");
        anzahlungLabels.add("Datum");
        anzahlungLabels.add("Anzahlung");
        anzahlungLabels.add("Rechnungssumme");
        anzahlungTable = new AnyJComponentJTable(anzahlungData, anzahlungLabels){
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                // add custom rendering here
                c.setForeground(Color.black); // keep it black
                return c;
            }
        };
        // selection listener:
        //orderTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
        anzahlungTable.setFillsViewportHeight(true);
        anzahlungTable.getSelectionModel().addListSelectionListener(new RowListener());

        // set table properties:
        TableColumn nr = anzahlungTable.getColumn("Rechnungs-Nr.");
        nr.setCellRenderer(rechtsAusrichter);
        nr.setPreferredWidth(100);
        TableColumn typ = anzahlungTable.getColumn("Datum");
        typ.setCellRenderer(zentralAusrichter);
        typ.setPreferredWidth(200);
        TableColumn jahr = anzahlungTable.getColumn("Anzahlung");
        jahr.setCellRenderer(rechtsAusrichter);
        jahr.setPreferredWidth(100);
        TableColumn kw = anzahlungTable.getColumn("Rechnungssumme");
        kw.setCellRenderer(rechtsAusrichter);
        kw.setPreferredWidth(100);

        JScrollPane scrollPane = new JScrollPane(anzahlungTable);
        anzahlungTablePanel.add(scrollPane, BorderLayout.CENTER);
        
        leftPanel.add(anzahlungTablePanel, BorderLayout.CENTER);
    }

    public void showRightPanel(int rechnungsNr) {
        rightPanel.setLayout(new BorderLayout());

        // Panel for header and both tables
        anzahlungDetailTablePanel = new JPanel();
        anzahlungDetailTablePanel.setLayout(new BoxLayout(anzahlungDetailTablePanel, BoxLayout.Y_AXIS));

        // Header
        JLabel headerLabel = new JLabel("Artikel der Anzahlung:");
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        anzahlungDetailTablePanel.add(headerLabel);

        /*
        // Table with general anzahlung data:
        Vector<String> bestellung = anzahlungData.get(bestellNummernUndTyp.indexOf(bestellNrUndTyp));
        Vector< Vector<String> > bestellData = new Vector< Vector<String> >();
        bestellData.add(bestellung);
        JTable bestellTable = new JTable(bestellData, anzahlungLabels);
        JScrollPane sp1 = new JScrollPane(bestellTable);
        sp1.setPreferredSize(new Dimension((int)sp1.getPreferredSize().getWidth(), 40));
        anzahlungDetailTablePanel.add(sp1);
        */

        Vector<String> columnLabels = new Vector<String>();
        columnLabels.add("Pos.");
        columnLabels.add("Artikel-Name"); columnLabels.add("Artikel-Nr."); columnLabels.add("Stückzahl");
        columnLabels.add("Einzelpreis"); columnLabels.add("Gesamtpreis"); columnLabels.add("MwSt.");

        // Table with anzahlung details:
        if (rechnungsNr > 0) {
            retrieveAnzahlungDetailData(rechnungsNr);
        }
        anzahlungDetailTable = new ArticleSelectTable(anzahlungDetailData, columnLabels, anzahlungDetailColors);
        setTableProperties(anzahlungDetailTable);

        anzahlungDetailScrollPane = new JScrollPane(anzahlungDetailTable);
        anzahlungDetailTablePanel.add(anzahlungDetailScrollPane);
        anzahlungDetailTablePanel.setMinimumSize(new Dimension(600, 400));

        rightPanel.add(anzahlungDetailTablePanel, BorderLayout.CENTER);
    }

    private void setTableProperties(ArticleSelectTable table) {
        // Spalteneigenschaften:
        //	table.getColumnModel().getColumn(0).setPreferredWidth(10);
        TableColumn pos = table.getColumn("Pos.");
        pos.setCellRenderer(zentralAusrichter);
        pos.setPreferredWidth(5);
        TableColumn artikelbez = table.getColumn("Artikel-Name");
        artikelbez.setCellRenderer(linksAusrichter);
        artikelbez.setPreferredWidth(150);
        TableColumn artikelnr = table.getColumn("Artikel-Nr.");
        artikelnr.setCellRenderer(rechtsAusrichter);
        artikelnr.setPreferredWidth(50);
        TableColumn stueckzahl = table.getColumn("Stückzahl");
        stueckzahl.setCellRenderer(rechtsAusrichter);
        TableColumn preis = table.getColumn("Einzelpreis");
        preis.setCellRenderer(rechtsAusrichter);
        TableColumn gespreis = table.getColumn("Gesamtpreis");
        gespreis.setCellRenderer(rechtsAusrichter);
        TableColumn mwst = table.getColumn("MwSt.");
        mwst.setCellRenderer(rechtsAusrichter);
        mwst.setPreferredWidth(5);
    }

    private class RowListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int[] selRows = anzahlungTable.getSelectedRows();
            if ( selRows.length == 1 ){
                int realRowIndex = anzahlungTable.convertRowIndexToModel(selRows[0]); // user might have changed row order
                selRechNr = rechnungsNummern.get(realRowIndex);
            } else {
                selRechNr = -1;
            }
            updateRightPanel();
            if (selRechNr > 0) {
                okButton.setEnabled(true);
            } else {
                okButton.setEnabled(false);
            }
        }
    }

    private void updateLeftPanel() {
        leftPanel = new JPanel();
        splitPane.setLeftComponent(leftPanel);
        showLeftPanel();
    }

    private void updateRightPanel() {
        rightPanel = new JPanel();
        splitPane.setRightComponent(rightPanel);
        showRightPanel(selRechNr);
    }

    void retrieveAnzahlungData() {
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT anzahlung_in_rech_nr, DATE_FORMAT(datum, "+
                "'"+bc.dateFormatSQL+"') "+
                "FROM "+tableForMode("anzahlung")+" "+
                "WHERE "+
                "datum > NOW() - INTERVAL 6 * ? MONTH AND "+
                "datum <= NOW() - INTERVAL 6 * ? MONTH "+
                "GROUP BY anzahlung_in_rech_nr "+
                "HAVING COUNT(*) = 1 "+
                "ORDER BY anzahlung_in_rech_nr DESC"
		    );
            pstmtSetInteger(pstmt, 1, pageNumber);
            pstmtSetInteger(pstmt, 2, pageNumber - 1);
            ResultSet rs = pstmt.executeQuery();
            Vector<Vector<Object>> incompleteData = new Vector<Vector<Object>>();
            while (rs.next()) {
                rechnungsNummern.add(rs.getInt(1));
                Vector<Object> row = new Vector<Object>();
                row.add(rs.getString(1));
                row.add(rs.getString(2));
                incompleteData.add(row);
            }
            rs.close();
            pstmt.close();
            for (Vector<Object> row : incompleteData) {
                int rechNr = Integer.parseInt((String)row.get(0));
                // What is Anzahlung?
                pstmt = connection.prepareStatement(
                    "SELECT SUM(ges_preis) FROM "+tableForMode("verkauf_details")+" "+
                    "WHERE rechnungs_nr = ? AND artikel_id = ?"
                );
                pstmtSetInteger(pstmt, 1, rechNr);
                pstmtSetInteger(pstmt, 2, anzahlungArtikelID);
                rs = pstmt.executeQuery();
                rs.next(); row.add(bc.priceFormatter(rs.getString(1))+" "+bc.currencySymbol); rs.close();
                pstmt.close();
                // What is Rechnungssumme?
                pstmt = connection.prepareStatement(
                    "SELECT SUM(ges_preis) FROM "+tableForMode("anzahlung_details")+" "+
                    "WHERE rechnungs_nr = ?"
                );
                pstmtSetInteger(pstmt, 1, rechNr);
                rs = pstmt.executeQuery();
                rs.next(); row.add(bc.priceFormatter(rs.getString(1))+" "+bc.currencySymbol); rs.close();
                pstmt.close();
                anzahlungData.add(row);
            }
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    void retrieveAnzahlungDetailData(int rechnungsNr) {
        kassierArtikel = new Vector<KassierArtikel>();
        anzahlungDetailData = new Vector< Vector<Object> >();
        anzahlungDetailColors = new Vector<String>();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT vd.position, vd.artikel_id, vd.rabatt_id, "+
                "a.kurzname, a.artikel_name, ra.aktionsname, " +
                "a.artikel_nr, a.sortiment, a.menge, a.einheit, " +
                "(p.toplevel_id IS NULL AND p.sub_id = 3) AS pfand, " +
                "vd.stueckzahl, ad.ges_preis, vd.ges_preis, vd.mwst_satz, " +
                "gs.gutschein_nr " +
                "FROM "+tableForMode("verkauf_details")+" AS vd " +
                "LEFT JOIN "+tableForMode("anzahlung_details")+" AS ad " +
                "  ON vd.rechnungs_nr = ad.rechnungs_nr AND vd.vd_id = ad.vd_id " +
                "LEFT JOIN artikel AS a USING (artikel_id) " +
                "LEFT JOIN produktgruppe AS p USING (produktgruppen_id) "+
                "LEFT JOIN rabattaktion AS ra USING (rabatt_id) " +
                "LEFT JOIN "+tableForMode("gutschein")+" AS gs ON vd.vd_id = gs.gutschein_in_vd_id " +
                "WHERE vd.rechnungs_nr = ? AND " +
                "vd.vd_id <= ("+
                "  SELECT MAX(vd_id) FROM "+tableForMode("verkauf_details")+" "+
                "  WHERE rechnungs_nr = ? AND artikel_id = ?"+
                ")"
            );
            pstmtSetInteger(pstmt, 1, rechnungsNr);
            pstmtSetInteger(pstmt, 2, rechnungsNr);
            pstmtSetInteger(pstmt, 3, anzahlungArtikelID);
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet, should be only one result ...
            while ( rs.next() ){
                Integer pos = rs.getString(1) == null ? null : rs.getInt(1);
                Integer artikelID = rs.getString(2) == null ? null : rs.getInt(2);
                Integer rabattID = rs.getString(3) == null ? null : rs.getInt(3);
                String kurzname = rs.getString(4);
                String artikelname = rs.getString(5);
                String aktionsname = rs.getString(6);
                String artikelnummer = rs.getString(7);
                boolean sortiment = rs.getBoolean(8);
                BigDecimal menge_bd = rs.getBigDecimal(9);
                String einheit = rs.getString(10);
                String menge = formatMengeForOutput(menge_bd, einheit);
                boolean pfand = rs.getBoolean(11);
                String stueck = rs.getString(12);
                BigDecimal stueckDec = new BigDecimal(0);
                if (stueck != null) {
                    stueckDec = new BigDecimal(stueck);
                }
                String gesPreis = rs.getString(13);
                String gesPreisInCaseOfAnzahlung = rs.getString(14);
                BigDecimal gesPreisInCaseOfAnzahlungDec = new BigDecimal(gesPreisInCaseOfAnzahlung);
                BigDecimal mwst = new BigDecimal(rs.getString(15));
                Integer gutscheinNr = rs.getInt(16);
                BigDecimal gesPreisDec = null;
                BigDecimal einzelPreis = null;
                if (gesPreis != null) {
                    gesPreisDec = new BigDecimal(gesPreis);
                    if (stueck != null) {
                        einzelPreis = gesPreisDec.divide(stueckDec, 10, RoundingMode.HALF_UP);
                    }
                } else {
                    gesPreisDec = gesPreisInCaseOfAnzahlungDec;
                    einzelPreis = gesPreisDec;
                }
                gesPreis = bc.priceFormatter(gesPreis)+' '+bc.currencySymbol;
                String name = artikelname;
                String color = "default";
                String type = "artikel";
                if (artikelID == gutscheinArtikelID) {
                    // name = artikelNameGutschein();
                    color = "green";
                    type = "gutschein";
                } else if (artikelID == gutscheineinloesungArtikelID) {
                    // name = artikelNameGutscheinEinloes();
                    color = "green";
                    type = "gutscheineinloesung";
                } else if ( aktionsname != null ) { // Aktionsrabatt
                    name = einrueckung+aktionsname;
                    artikelnummer = "RABATT";
                    color = "red";
                    type = "rabatt";
                    // menge = "";
                } else if ( artikelID == artikelRabattArtikelID ){ // Manueller Rabatt auf Artikel
                    name = einrueckung+artikelname;
                    artikelnummer = "RABATT";
                    color = "red";
                    type = "rabatt";
                    // menge = "";
                } else if ( artikelID == rechnungRabattArtikelID ){ // Manueller Rabatt auf Rechnung
                    artikelnummer = "RABATT";
                    color = "red";
                    type = "rabattrechnung";
                    menge = "";
                } else if ( artikelID == preisanpassungArtikelID ){ // Manuelle Preisanpassung auf Artikel
                    name = einrueckung+artikelname;
                    artikelnummer = "ANPASSUNG";
                    color = "red";
                    type = "rabatt";
                    // menge = "";
                } else if ( pfand && stueckDec.signum() > 0 ){
                    name = einrueckung+artikelname;
                    artikelnummer = "PFAND";
                    color = "blue";
                    type = "pfand";
                    // menge = "";
                } else if ( pfand && stueckDec.signum() < 0 ){
                    artikelnummer = "LEERGUT";
                    color = "green";
                    type = "leergut";
                    // menge = "";
                } else {
                    if ( kurzname != null && !kurzname.equals("") ){
                        name = kurzname;
                    } else if (artikelname != null ){
                        name = artikelname;
                    } else {
                        name = "";
                    }
                    if ( stueckDec.signum() < 0 ){
                        color = "green";
                        type = "rueckgabe";
                    } else if ( !sortiment ){ color = "gray"; }
                    else { color = "default"; }
                }

                if (artikelID != anzahlungArtikelID) {
                    // show this artikel in detail table
                    Vector<Object> row = new Vector<Object>();
                    row.add(pos);
                    row.add(name); row.add(artikelnummer); row.add(stueck);
                    row.add(bc.priceFormatter(einzelPreis)+' '+bc.currencySymbol);
                    row.add(gesPreis); row.add(bc.vatFormatter(mwst));
                    anzahlungDetailData.add(row);
                    anzahlungDetailColors.add(color);
                } else {
                    // this is the anzahlung itself
                    // invert price (it's a kind of rabatt) and change type etc.
                    artikelID = anzahlungsaufloesungArtikelID;
                    // find the date for the anzahlung:
                    String date = (String)anzahlungData.get(rechnungsNummern.indexOf(rechnungsNr)).get(1);
                    name = "Anzahlung vom "+date+", Rech.-Nr. "+rechnungsNr;
                    artikelnummer = "ANZAHLUNGSAUFLÖSUNG";
                    color = "red";
                    type = "anzahlungsaufloesung";
                    menge = "";
                    gesPreisDec = gesPreisInCaseOfAnzahlungDec.multiply(bc.minusOne);
                    einzelPreis = gesPreisDec;
                }

                // add this artikel for addition to rechnung
                KassierArtikel ka = new KassierArtikel(bc);
                ka.setPosition(pos);
                ka.setArtikelID(artikelID);
                ka.setRabattID(rabattID);
                ka.setName(name);
                ka.setArtikelNummer(artikelnummer);
                ka.setColor(color);
                ka.setType(type);
                ka.setMenge(menge);
                ka.setStueckzahl(stueckDec.intValue());
                ka.setEinzelPreis(einzelPreis);
                ka.setGesPreis(gesPreisDec);
                ka.setMwst(mwst);
                ka.setGutscheinNr(gutscheinNr);
                kassierArtikel.add(ka);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    protected void showFooter() {
        /**
         * Button-Panel
         * */
        footerPanel = new JPanel();
        okButton = new JButton("OK" );
        okButton.setMnemonic(KeyEvent.VK_O);
        okButton.addActionListener(this);
        okButton.setEnabled(false);
        footerPanel.add(okButton);
        cancelButton = new JButton("Abbrechen" );
        cancelButton.setMnemonic(KeyEvent.VK_A);
        cancelButton.addActionListener(this);
        footerPanel.add(cancelButton);
        allPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    @Override
    protected int submit() {
        return 0;
    }

    public boolean getAborted() {
        return aborted;
    }

    public int getSelectedRechNr() {
        return selRechNr;
    }

    public Vector<KassierArtikel> getKassierArtikel() {
        return kassierArtikel;
    }

    /**
     * Each non abstract class that implements the ActionListener
     * must have this method.
     *
     * @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == loadMoreButton) {
            pageNumber += 1;
            retrieveAnzahlungData();
            updateLeftPanel();
            selRechNr = -1;
            okButton.setEnabled(false);
        }
        if (e.getSource() == okButton) {
            aborted = false;
            this.window.dispose();
            return;
        }
        if (e.getSource() == cancelButton) {
            aborted = true;
            this.window.dispose();
            return;
        }
        super.actionPerformed(e);
    }
}
