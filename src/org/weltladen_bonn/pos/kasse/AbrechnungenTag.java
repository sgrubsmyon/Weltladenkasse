package org.weltladen_bonn.pos.kasse;

import org.weltladen_bonn.pos.kasse.WeltladenTSE.TSEStatus;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// DateTime from date4j (http://www.date4j.net/javadoc/index.html)
import hirondelle.date4j.DateTime;

import org.jopendocument.dom.spreadsheet.Sheet;
import org.weltladen_bonn.pos.BaseClass;
import org.weltladen_bonn.pos.DialogWindow;
import org.weltladen_bonn.pos.MainWindowGrundlage;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class AbrechnungenTag extends Abrechnungen {
    private static final long serialVersionUID = 1L;

    // Attribute:
    private static final Logger logger = LogManager.getLogger(AbrechnungenTag.class);

    private AbrechnungenTabbedPane abrechTabbedPane;
    private TabbedPane tabbedPane;

    private JButton submitButton;
    private boolean submitButtonEnabled;

    private String selectedZeitpunkt = null;
    private LinkedHashMap<BigDecimal, Integer> zaehlprotokoll = null;
    private String zaehlprotokollKommentar = null;

    private Vector< HashMap<BigDecimal, BigDecimal> > abrechnungsStornos;
    private Vector< HashMap<BigDecimal, BigDecimal> > abrechnungsRetouren;
    private Vector<BigDecimal> abrechnungsEntnahmen;
    private HashMap<BigDecimal, BigDecimal> incompleteAbrechnungsStornos;
    private HashMap<BigDecimal, BigDecimal> incompleteAbrechnungsRetouren;
    private BigDecimal incompleteAbrechnungsEntnahmen;

    private Vector< Vector<String> > zaehlprotokollZeitpunkte;
    private Vector< Vector<String> > zaehlprotokollKommentare;
    private Vector< Vector<BigDecimal> > zaehlprotokollSummen;
    private Vector<BigDecimal> zaehlprotokollSollKassenstaende;
    private Vector< Vector<BigDecimal> > zaehlprotokollDifferenzen;
    private Vector< Vector<BigDecimal> > zaehlprotokollEinnahmen;
    private Vector< Vector< LinkedHashMap<BigDecimal, Integer> > > zaehlprotokolle;

    private Integer zpNumber;
    private TreeSet<BigDecimal> zpEinheiten;

    private Vector<JButton> editButtons;

    private Boolean kassenstandWasChanged = false;

    private WeltladenTSE tse;
    private DSFinVKCSV dsfinvk;

    // Methoden:
    /**
     *    The constructor.
     *       */
    AbrechnungenTag(MariaDbPoolDataSource pool, MainWindowGrundlage mw, AbrechnungenTabbedPane atp, TabbedPane tp, Integer exportIndex){
        super(pool, mw, "", "Tagesabrechnung", "yyyy-MM-dd HH:mm:ss", "dd.MM. HH:mm (E)",
                "zeitpunkt", "abrechnung_tag");
        if (mw instanceof MainWindow) {
            MainWindow mainw = (MainWindow) mw;
            tse = mainw.getTSE();
            dsfinvk = mainw.getDSFinVK();
        } else {
            tse = null;
            dsfinvk = null;
        }
        this.setExportDirFormat(bc.exportDirAbrechnungTag);
        this.abrechTabbedPane = atp;
        this.tabbedPane = tp;
        showTable();
        if (exportIndex != null) {
          export(exportIndex);
        }

        // // XXX Write into DSFinV-K CSV files retroactively:
        // String actualSWVersion = bc.KASSE_SW_VERSION;

        // // for the v2.0.0 days
        // bc.KASSE_SW_VERSION = "v2.0.0";
        // bc.TERMINAL_SW_VERSION = "v2.0.0";
        // for (int abr_id = 1511; abr_id <= 1533; abr_id++) {
        //     // writeIntoDSFinVKCSVFiles(abr_id);
        //     writeIntoDSFinVKCSVFilesTemporary(abr_id);
        // }

        // // for the v2.0.1 days
        // bc.KASSE_SW_VERSION = "v2.0.1";
        // bc.TERMINAL_SW_VERSION = "v2.0.1";
        // for (int abr_id = 1534; abr_id <= 1535; abr_id++) {
        //     // writeIntoDSFinVKCSVFiles(abr_id);
        //     writeIntoDSFinVKCSVFilesTemporary(abr_id);
        // }

        // // for the v2.0.2 days
        // bc.KASSE_SW_VERSION = "v2.0.2";
        // bc.TERMINAL_SW_VERSION = "v2.0.2";
        // for (int abr_id = 1536; abr_id <= 1888; abr_id++) {
        //    // writeIntoDSFinVKCSVFiles(abr_id);
        //    writeIntoDSFinVKCSVFilesTemporary(abr_id);
        // }

        // // back to normal
        // bc.KASSE_SW_VERSION = actualSWVersion;
        // bc.TERMINAL_SW_VERSION = actualSWVersion;
    }

    void setSelectedZeitpunkt(String zp) {
        this.selectedZeitpunkt = zp;
    }

    void setZaehlprotokoll(LinkedHashMap<BigDecimal, Integer> zp) {
        this.zaehlprotokoll = zp;
    }

    void setZaehlprotokollKommentar(String kommentar) {
        this.zaehlprotokollKommentar = kommentar;
    }

    void setKassenstandWasChanged(Boolean b) {
        kassenstandWasChanged = b;
    }


// ----------------------------------------------------------------------------

    private boolean abrechnungTagEmpty() {
        boolean empty = true;
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT MAX(id) IS NULL FROM "+tableForMode("abrechnung_tag")
            );
            if (rs.next()) {
                empty = rs.getBoolean(1);
            }
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return empty;
    }

    private PreparedStatement prepareStmtStornos(Connection connection, Integer abrechnung_tag_id, Boolean abrTagEmpty) throws SQLException {
        // Summe über Stornos:
        PreparedStatement pstmt = connection.prepareStatement(
            // SELECT mwst_satz, SUM(mwst_netto + mwst_betrag) FROM verkauf_mwst INNER JOIN verkauf USING (rechnungs_nr) WHERE rechnungs_nr IN (SELECT storno_von FROM verkauf WHERE storno_von IS NOT NULL) AND rechnungs_nr >= IF((SELECT COUNT(*) FROM abrechnung_tag) = 0, 0, (SELECT rechnungs_nr_von FROM abrechnung_tag WHERE id = 18)) AND rechnungs_nr <= IF((SELECT COUNT(*) FROM abrechnung_tag) = 0, 4294967295, (SELECT rechnungs_nr_bis FROM abrechnung_tag WHERE id = 18)) GROUP BY mwst_satz;
            "SELECT mwst_satz, SUM(mwst_netto + mwst_betrag) " +
            "FROM "+tableForMode("verkauf_mwst")+" INNER JOIN "+tableForMode("verkauf")+" USING (rechnungs_nr) " +
            "WHERE rechnungs_nr IN (SELECT storno_von FROM verkauf WHERE storno_von IS NOT NULL) AND " +
            "rechnungs_nr >= " +
            (abrTagEmpty ? "0" : "(SELECT rechnungs_nr_von FROM "+tableForMode("abrechnung_tag")+" WHERE id = ?)") + // if table is still completely empty: include all rechnungen
            " AND " +
            "rechnungs_nr <= " +
            (abrTagEmpty ? "4294967295" : "(SELECT rechnungs_nr_bis FROM "+tableForMode("abrechnung_tag")+" WHERE id = ?)") + // if table is still completely empty: include all rechnungen
            //              ^^^^^^^^^^ this is highest value for unsigned int
            " " +
            "GROUP BY mwst_satz"
        );
        if (!abrTagEmpty) {
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            pstmtSetInteger(pstmt, 2, abrechnung_tag_id);
        }
        return pstmt;
    }

    private PreparedStatement prepareStmtRetouren(Connection connection, Integer abrechnung_tag_id, Boolean abrTagEmpty) throws SQLException {
        // Summe über Retouren:
        PreparedStatement pstmt = connection.prepareStatement(
            // SELECT mwst_satz, SUM(ges_preis) FROM verkauf_details INNER JOIN verkauf USING (rechnungs_nr) LEFT JOIN artikel USING (artikel_id) WHERE rechnungs_nr >= IF((SELECT COUNT(*) FROM abrechnung_tag) = 0, 0, (SELECT rechnungs_nr_von FROM abrechnung_tag WHERE id = 1461)) AND rechnungs_nr <= IF((SELECT COUNT(*) FROM abrechnung_tag) = 0, 4294967295, (SELECT rechnungs_nr_bis FROM abrechnung_tag WHERE id = 1461)) AND stueckzahl < 0 AND ( produktgruppen_id NOT IN (1, 6, 7, 8) OR produktgruppen_id IS NULL ) AND storno_von IS NULL GROUP BY mwst_satz;
            "SELECT mwst_satz, SUM(ges_preis) " +
            "FROM "+tableForMode("verkauf_details")+" " +
            "INNER JOIN "+tableForMode("verkauf")+" USING (rechnungs_nr) " +
            "LEFT JOIN artikel USING (artikel_id) " + // left join needed because Rabattaktionen do not have an artikel_id
            "WHERE " +
            "rechnungs_nr >= " +
            (abrTagEmpty ? "0" : "(SELECT rechnungs_nr_von FROM "+tableForMode("abrechnung_tag")+" WHERE id = ?)") + // if table is still completely empty: include all rechnungen
            " AND " +
            "rechnungs_nr <= " +
            (abrTagEmpty ? "4294967295" : "(SELECT rechnungs_nr_bis FROM "+tableForMode("abrechnung_tag")+" WHERE id = ?)") + // if table is still completely empty: include all rechnungen
            //              ^^^^^^^^^^ this is highest value for unsigned int
            " AND " +
            "stueckzahl < 0 AND ( produktgruppen_id NOT IN (1, 6, 7, 8) OR produktgruppen_id IS NULL ) AND " + // exclude internal articles, Gutschein, and Pfand
            "storno_von IS NULL " + // exclude Storno
            // produktgruppen_id is null for Rabattaktionen
            "GROUP BY mwst_satz"
        );
        if (!abrTagEmpty) {
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            pstmtSetInteger(pstmt, 2, abrechnung_tag_id);
        }
        return pstmt;
    }

    private PreparedStatement prepareStmtEntnahmen(Connection connection, Integer abrechnung_tag_id) throws SQLException {
        // Summe über Entnahmen:
        PreparedStatement pstmt = connection.prepareStatement(
            // SELECT SUM(entnahme_betrag) FROM (SELECT kassenstand_id AS kid, neuer_kassenstand - (SELECT neuer_kassenstand FROM kassenstand WHERE kassenstand_id = kid-1) AS entnahme_betrag FROM kassenstand WHERE kassenstand_id > IFNULL((SELECT kassenstand_id FROM abrechnung_tag WHERE id = 0), 0) AND kassenstand_id < IFNULL((SELECT kassenstand_id FROM abrechnung_tag WHERE id = 9999999999999), 4294967295) AND entnahme = TRUE) AS entnahme_table;
            "SELECT SUM(entnahme_betrag) " +
            "FROM (" +
            "  SELECT kassenstand_id AS kid, " +
            "  neuer_kassenstand - (SELECT neuer_kassenstand FROM "+tableForMode("kassenstand")+" WHERE kassenstand_id = kid-1) AS entnahme_betrag " +
            "  FROM "+tableForMode("kassenstand")+" WHERE " +
            "  kassenstand_id > IFNULL((SELECT kassenstand_id FROM "+tableForMode("abrechnung_tag")+" WHERE id = ?), 0) AND " +
            "  kassenstand_id < IFNULL((SELECT kassenstand_id FROM "+tableForMode("abrechnung_tag")+" WHERE id = ?), 4294967295) AND " +
            "  entnahme = TRUE" +
            ") AS entnahme_table"
        );
        pstmtSetInteger(pstmt, 1, abrechnung_tag_id - 1);
        pstmtSetInteger(pstmt, 2, abrechnung_tag_id);
        return pstmt;
    }

    void queryStornosRetourenEntnahmen() {
        // the queries concerning Stornierungen, Retouren and Entnahmen
        abrechnungsStornos = new Vector<>();
        abrechnungsRetouren = new Vector<>();
        abrechnungsEntnahmen = new Vector<>();
        boolean abrTagEmpty = abrechnungTagEmpty();
        try {
            Connection connection = this.pool.getConnection();
            for (Integer id : abrechnungsIDs) {
                // Summe über Stornos:
                PreparedStatement pstmt = prepareStmtStornos(connection, id, abrTagEmpty);
                ResultSet rs = pstmt.executeQuery();
                HashMap<BigDecimal, BigDecimal> map = new HashMap<>();
                while (rs.next()) {
                    BigDecimal mwst_satz = rs.getBigDecimal(1);
                    BigDecimal storno_sum = rs.getBigDecimal(2);
                    map.put(mwst_satz, storno_sum);
                    mwstSet.add(mwst_satz);
                }
                rs.close();
                pstmt.close();
                abrechnungsStornos.add(map);

                // Summe über Retouren:
                pstmt = prepareStmtRetouren(connection, id, abrTagEmpty);
                rs = pstmt.executeQuery();
                HashMap<BigDecimal, BigDecimal> map2 = new HashMap<>();
                while (rs.next()) {
                    BigDecimal mwst_satz = rs.getBigDecimal(1);
                    BigDecimal retoure_sum = rs.getBigDecimal(2);
                    map2.put(mwst_satz, retoure_sum);
                    mwstSet.add(mwst_satz);
                }
                rs.close();
                pstmt.close();
                abrechnungsRetouren.add(map2);

                // Summe über Entnahmen:
                pstmt = prepareStmtEntnahmen(connection, id);
                rs = pstmt.executeQuery();
                BigDecimal entnahme_sum = new BigDecimal("0.00");
                if (rs.next()) {
                    BigDecimal sum = rs.getBigDecimal(1);
                    if (sum != null) {
                        entnahme_sum = sum;
                    }
                }
                rs.close();
                pstmt.close();
                abrechnungsEntnahmen.add(entnahme_sum);
            }
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    void queryZaehlprotokoll() {
        // the queries concerning zaehlprotokolle
        zaehlprotokollZeitpunkte = new Vector<>();
        zaehlprotokollKommentare = new Vector<>();
        zaehlprotokollSummen = new Vector<>();
        zaehlprotokollSollKassenstaende = new Vector<>();
        zaehlprotokollDifferenzen = new Vector<>();
        zaehlprotokollEinnahmen = new Vector<>();
        zaehlprotokolle = new Vector<>();
        try {
            Connection connection = this.pool.getConnection();
            for (Integer id : abrechnungsIDs) {
                PreparedStatement pstmt = connection.prepareStatement(
                        "SELECT id, zeitpunkt, kommentar, aktiv " +
                        "FROM "+tableForMode("zaehlprotokoll")+" " +
                        "WHERE abrechnung_tag_id = ? " +
                        "ORDER BY id DESC"
                );
                pstmtSetInteger(pstmt, 1, id);
                ResultSet rs = pstmt.executeQuery();
                Vector<String> zeitpunkte = new Vector<>();
                Vector<String> kommentare = new Vector<>();
                Vector<BigDecimal> summen = new Vector<>();
                Vector<LinkedHashMap<BigDecimal, Integer>> zps = new Vector<>();
                while (rs.next()) {
                    zeitpunkte.add(rs.getString(2));
                    kommentare.add(rs.getString(3));
                    //
                    PreparedStatement pstmt2 = connection.prepareStatement(
                            "SELECT SUM(anzahl*einheit) " +
                            "FROM "+tableForMode("zaehlprotokoll_details")+" " +
                            "WHERE zaehlprotokoll_id = ?"
                    );
                    pstmtSetInteger(pstmt2, 1, rs.getInt(1));
                    ResultSet rs2 = pstmt2.executeQuery();
                    BigDecimal summe = new BigDecimal("0");
                    if (rs2.next()) {
                        summe = rs2.getBigDecimal(1);
                    }
                    summen.add(summe);
                    //
                    pstmt2 = connection.prepareStatement(
                            "SELECT einheit, anzahl " +
                            "FROM "+tableForMode("zaehlprotokoll_details")+" " +
                            "WHERE zaehlprotokoll_id = ? " +
                            "ORDER BY einheit"
                    );
                    pstmtSetInteger(pstmt2, 1, rs.getInt(1));
                    rs2 = pstmt2.executeQuery();
                    LinkedHashMap<BigDecimal, Integer> zp = new LinkedHashMap<>();
                    while (rs2.next()) {
                        zp.put(rs2.getBigDecimal(1), rs2.getInt(2));
                    }
                    zps.add(zp);
                }
                rs.close();
                //
                pstmt = connection.prepareStatement(
                    // SELECT neuer_kassenstand FROM kassenstand INNER JOIN abrechnung_tag USING (kassenstand_id) WHERE abrechnung_tag.id = 324;
                    "SELECT neuer_kassenstand " +
                    "FROM "+tableForMode("kassenstand")+" INNER JOIN "+tableForMode("abrechnung_tag")+" AS at USING (kassenstand_id) " +
                    "WHERE at.id = ?"
                );
                pstmtSetInteger(pstmt, 1, id);
                rs = pstmt.executeQuery();
                BigDecimal sollKassenstand = null;
                if (rs.next()) {
                    sollKassenstand = rs.getBigDecimal(1);
                }
                //
                Vector<BigDecimal> differenzen = new Vector<>();
                for (BigDecimal summe : summen) {
                    BigDecimal diff = null;
                    if (sollKassenstand != null) {
                        diff = summe.subtract(sollKassenstand);
                    }
                    differenzen.add(diff);
                }
                // Select the first 'neuer_kassenstand' from a Tagesabschluss after the Tagesabschluss period
                pstmt = connection.prepareStatement(
                        // SELECT neuer_kassenstand FROM kassenstand WHERE kassenstand_id > (SELECT kassenstand_id FROM abrechnung_tag WHERE id = 324) AND manuell = TRUE AND entnahme = FALSE AND kommentar = 'Tagesabschluss' ORDER BY kassenstand_id LIMIT 1;
                        "SELECT neuer_kassenstand "+
                        "FROM "+tableForMode("kassenstand")+" "+
                        "WHERE kassenstand_id > "+
                        "(SELECT kassenstand_id FROM "+tableForMode("abrechnung_tag")+" WHERE id = ?) "+
                        "AND manuell = TRUE AND entnahme = FALSE AND kommentar = 'Tagesabschluss' "+
                        "ORDER BY kassenstand_id LIMIT 1"
                );
                pstmtSetInteger(pstmt, 1, id);
                rs = pstmt.executeQuery();
                BigDecimal neuerKassenstand = null;
                if (rs.next()) {
                    neuerKassenstand = rs.getBigDecimal(1);
                }
                //
                Vector<BigDecimal> einnahmen = new Vector<>();
                for (BigDecimal summe : summen) {
                    BigDecimal einnahme = null;
                    if (neuerKassenstand != null) {
                        einnahme = summe.subtract(neuerKassenstand);
                    }
                    einnahmen.add(einnahme);
                }
                //
                zaehlprotokollZeitpunkte.add(zeitpunkte);
                zaehlprotokollKommentare.add(kommentare);
                zaehlprotokollSummen.add(summen);
                if (sollKassenstand != null) {
                    zaehlprotokollSollKassenstaende.add(sollKassenstand);
                }
                zaehlprotokollDifferenzen.add(differenzen);
                zaehlprotokollEinnahmen.add(einnahmen);
                zaehlprotokolle.add(zps);
            }
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    @Override
    void queryAbrechnungen() {
        // the normal queries
        super.queryAbrechnungen();
        queryStornosRetourenEntnahmen();
        queryZaehlprotokoll();
    }

    @Override
    void queryIncompleteAbrechnung() { // create new abrechnung (for display) from time of last abrechnung until now
        String date = now();

        // for filling the displayed table:
        // first, get the totals:
        Vector<BigDecimal> values = queryIncompleteAbrechnungTag_Totals();
        // store in map under date
        incompleteAbrechnungsDate = date;
        incompleteAbrechnungsTotals = values;
        incompleteAbrechnungsVATs = new HashMap<>();

        // second, get values grouped by mwst
        HashMap<BigDecimal, Vector<BigDecimal>> abrechnungNettoBetrag = queryIncompleteAbrechnungTag_VATs();
        int rowCount = 0;
        for ( Map.Entry< BigDecimal, Vector<BigDecimal> > entry : abrechnungNettoBetrag.entrySet() ){
            BigDecimal mwst = entry.getKey();
            Vector<BigDecimal> mwstValues = entry.getValue();
            incompleteAbrechnungsVATs.put(mwst, mwstValues);
            mwstSet.add(mwst);
            rowCount++;

            submitButtonEnabled = true;
        }
        if ( rowCount == 0 ){ // empty, there are no verkaeufe!!!
            submitButtonEnabled = false;
        }

        // the queries concerning Stornierungen, Retouren and Entnahmen
        incompleteAbrechnungsStornos = new HashMap<>();
        incompleteAbrechnungsRetouren = new HashMap<>();
        boolean abrTagEmpty = abrechnungTagEmpty();
        try {
            Connection connection = this.pool.getConnection();
            Integer id = id() + 1; // ID of new, yet to come, abrechnung
            
            // Summe über Stornos:
            PreparedStatement pstmt = prepareStmtStornos(connection, id, abrTagEmpty);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                BigDecimal mwst_satz = rs.getBigDecimal(1);
                BigDecimal storno_sum = rs.getBigDecimal(2);
                incompleteAbrechnungsStornos.put(mwst_satz, storno_sum);
                mwstSet.add(mwst_satz);
            }
            rs.close();
            pstmt.close();

            // Summe über Retouren:
            pstmt = prepareStmtRetouren(connection, id, abrTagEmpty);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                BigDecimal mwst_satz = rs.getBigDecimal(1);
                BigDecimal retoure_sum = rs.getBigDecimal(2);
                incompleteAbrechnungsRetouren.put(mwst_satz, retoure_sum);
                mwstSet.add(mwst_satz);
            }
            rs.close();
            pstmt.close();

            // Summe über Entnahmen:
            pstmt = prepareStmtEntnahmen(connection, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                incompleteAbrechnungsEntnahmen = rs.getBigDecimal(1);
            } else {
                incompleteAbrechnungsEntnahmen = new BigDecimal("0.00");
            }
            rs.close();
            pstmt.close();
            
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    private HashMap<BigDecimal, BigDecimal> queryIncompleteAbrechnung_BarBruttoVATs() {
        HashMap<BigDecimal, BigDecimal> abrechnungBarBrutto = new HashMap<>();
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                // SELECT mwst_satz, SUM(ges_preis) AS bar_brutto FROM "+tableForMode("verkauf_details")+" INNER JOIN "+tableForMode("verkauf")+" USING (rechnungs_nr) WHERE rechnungs_nr > IFNULL((SELECT MAX(rechnungs_nr_bis) FROM abrechnung_tag), 0) AND ec_zahlung = FALSE GROUP BY mwst_satz;
                    "SELECT mwst_satz, SUM(ges_preis) AS bar_brutto " +
                    "FROM "+tableForMode("verkauf_details")+" INNER JOIN "+tableForMode("verkauf")+" USING (rechnungs_nr) " +
                    "WHERE rechnungs_nr > " +
                    "IFNULL((SELECT MAX(rechnungs_nr_bis) FROM "+tableForMode("abrechnung_tag")+"), 0) AND ec_zahlung = FALSE " +
                    "GROUP BY mwst_satz"
                    );
            while (rs.next()) {
                BigDecimal mwst_satz = rs.getBigDecimal(1);
                BigDecimal bar_brutto = rs.getBigDecimal(2);
                abrechnungBarBrutto.put(mwst_satz, bar_brutto);
                mwstSet.add(mwst_satz);
                //System.out.println(mwst_satz+"  "+bar_brutto);
            }
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return abrechnungBarBrutto;
    }

    @Override
    void queryAbrechnungenSpecial() {
    }


// ----------------------------------------------------------------------------


    private Integer maxZaehlprotokollNumber() {
        Integer maxNumber = 0;
        for (Vector<String> zeitpunkte : zaehlprotokollZeitpunkte) {
            Integer newSize = zeitpunkte.size();
            if (newSize > maxNumber) {
                maxNumber = newSize;
            }
        }
        return maxNumber;
    }

    private TreeSet<BigDecimal> zaehlprotokollEinheiten() {
        TreeSet<BigDecimal> einheiten = new TreeSet<>();
        for (Vector< LinkedHashMap<BigDecimal, Integer> > zps : zaehlprotokolle) {
            for (LinkedHashMap<BigDecimal, Integer> zp : zps){
                einheiten.addAll(zp.keySet());
            }
        }
        return einheiten;
    }

    @Override
    void fillHeaderColumn() {
        super.fillHeaderColumn();

        // ------------- Stornos, Retouren, Entnahmen

        for (BigDecimal mwst : mwstSet) {
            data.add(new Vector<>());
            data.lastElement().add("Storno ("+bc.vatFormatter(mwst)+" MwSt.)");
            colors.add(new Vector<>());
            colors.lastElement().add(Color.BLACK);
            fontStyles.add(new Vector<>());
            fontStyles.lastElement().add("bold");
        }
        for (BigDecimal mwst : mwstSet) {
            data.add(new Vector<>());
            data.lastElement().add("Retouren ("+bc.vatFormatter(mwst)+" MwSt.)");
            colors.add(new Vector<>());
            colors.lastElement().add(Color.BLACK);
            fontStyles.add(new Vector<>());
            fontStyles.lastElement().add("bold");
        }
        data.add(new Vector<>());
        data.lastElement().add("Entnahmen");
        colors.add(new Vector<>());
        colors.lastElement().add(Color.BLACK);
        fontStyles.add(new Vector<>());
        fontStyles.lastElement().add("bold");

        // ------------- Zaehlprotokoll

        zpNumber = maxZaehlprotokollNumber();
        zpEinheiten = zaehlprotokollEinheiten();
        logger.trace(zpNumber);
        logger.trace(zpEinheiten);
        for (int i = 0; i < zpNumber; i++) {
            Color def_col = Color.BLACK;
            if (i == 0) {
                data.add(new Vector<>()); data.lastElement().add("Aktives Zählprotokoll");
            } else {
                data.add(new Vector<>()); data.lastElement().add("Inaktives Zählprotokoll");
                def_col = Color.GRAY;
            }
            colors.add(new Vector<>()); colors.lastElement().add(def_col);
            fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold-italic");
            data.add(new Vector<>()); data.lastElement().add("Zeitpunkt");
            colors.add(new Vector<>()); colors.lastElement().add(def_col);
            fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold");
            for (BigDecimal einheit : zpEinheiten) {
                data.add(new Vector<>()); data.lastElement().add(bc.priceFormatter(einheit)+" "+bc.currencySymbol+", Anzahl =");
                colors.add(new Vector<>()); colors.lastElement().add(def_col);
                fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold");
            }
            data.add(new Vector<>()); data.lastElement().add("Zähl-Kassenstand");
            colors.add(new Vector<>()); colors.lastElement().add(def_col);
            fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold");
            data.add(new Vector<>()); data.lastElement().add("Soll-Kassenstand");
            colors.add(new Vector<>()); colors.lastElement().add(def_col);
            fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold");
            data.add(new Vector<>()); data.lastElement().add("Differenz");
            colors.add(new Vector<>()); colors.lastElement().add(def_col);
            fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold");
            data.add(new Vector<>()); data.lastElement().add("Bar-Tageseinnahmen");
            colors.add(new Vector<>()); colors.lastElement().add(def_col);
            fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold");
            data.add(new Vector<>()); data.lastElement().add("Kommentar");
            colors.add(new Vector<>()); colors.lastElement().add(def_col);
            fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold");
            if (i == 0) { // only after first zaehlprotokoll
                data.add(new Vector<>()); data.lastElement().add(""); // empty cell instead of edit zaehlprotokoll button
                colors.add(new Vector<>()); colors.lastElement().add(def_col);
                fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold");
            }
        }

        // need to initialize edit buttons some time before fillDataArrayColumn()
        editButtons = new Vector<>();
    }

    @Override
    int fillIncompleteDataColumn() {
        int rowIndex = super.fillIncompleteDataColumn();

        // ------------- Stornos, Retouren, Entnahmen

        for (BigDecimal mwst : mwstSet) {
            Color color = Color.BLACK;
            if (incompleteAbrechnungsStornos != null && incompleteAbrechnungsStornos.containsKey(mwst)){
                BigDecimal bd = incompleteAbrechnungsStornos.get(mwst);
                if (bd.signum() != 0) {
                    color = Color.BLUE;
                }
                data.get(rowIndex).add( bc.priceFormatter(bd)+" "+bc.currencySymbol );
            } else {
                data.get(rowIndex).add( bc.priceFormatter("0")+" "+bc.currencySymbol );
            }
            colors.get(rowIndex).add(color);
            fontStyles.get(rowIndex).add("normal");
            rowIndex++;
        }
        for (BigDecimal mwst : mwstSet) {
            Color color = Color.BLACK;
            if (incompleteAbrechnungsRetouren != null && incompleteAbrechnungsRetouren.containsKey(mwst)){
                BigDecimal bd = incompleteAbrechnungsRetouren.get(mwst);
                if (bd.signum() != 0) {
                    color = Color.BLUE;
                }
                data.get(rowIndex).add( bc.priceFormatter(bd)+" "+bc.currencySymbol );
            } else {
                data.get(rowIndex).add( bc.priceFormatter("0")+" "+bc.currencySymbol );
            }
            colors.get(rowIndex).add(color);
            fontStyles.get(rowIndex).add("normal");
            rowIndex++;
        }
        Color color = Color.BLACK;
        if (incompleteAbrechnungsEntnahmen != null){
            BigDecimal bd = incompleteAbrechnungsEntnahmen;
            if (bd.signum() != 0) {
                color = Color.RED;
            }
            data.get(rowIndex).add( bc.priceFormatter(bd)+" "+bc.currencySymbol );
        } else {
            data.get(rowIndex).add( bc.priceFormatter("0")+" "+bc.currencySymbol );
        }
        colors.get(rowIndex).add(color);
        fontStyles.get(rowIndex).add("normal");
        rowIndex++;

        // ------------- Zaehlprotokoll

        for (int i = 0; i < zpNumber; i++) {
            data.get(rowIndex).add(""); // empty row as separator before zaehlprotokoll
            colors.get(rowIndex).add(Color.BLACK);
            fontStyles.get(rowIndex).add("normal");
            rowIndex++;
            data.get(rowIndex).add(""); // Zeitpunkt
            colors.get(rowIndex).add(Color.BLACK);
            fontStyles.get(rowIndex).add("normal");
            rowIndex++;
            for (BigDecimal einheit : zpEinheiten) {
                data.get(rowIndex).add("");
                colors.get(rowIndex).add(Color.BLACK);
                fontStyles.get(rowIndex).add("normal");
                rowIndex++;
            }
            data.get(rowIndex).add(""); // Summe gezählter Kassenstand
            colors.get(rowIndex).add(Color.BLACK);
            fontStyles.get(rowIndex).add("normal");
            rowIndex++;
            data.get(rowIndex).add(""); // Soll-Kassenstand
            colors.get(rowIndex).add(Color.BLACK);
            fontStyles.get(rowIndex).add("normal");
            rowIndex++;
            data.get(rowIndex).add(""); // Differenz
            colors.get(rowIndex).add(Color.BLACK);
            fontStyles.get(rowIndex).add("normal");
            rowIndex++;
            data.get(rowIndex).add(""); // Einnahmen
            colors.get(rowIndex).add(Color.BLACK);
            fontStyles.get(rowIndex).add("normal");
            rowIndex++;
            data.get(rowIndex).add(""); // Kommentar
            colors.get(rowIndex).add(Color.BLACK);
            fontStyles.get(rowIndex).add("normal");
            rowIndex++;
            if (i == 0) { // only after first zaehlprotokoll
                data.get(rowIndex).add(""); // empty row instead of edit zaehlprotokoll button
                colors.get(rowIndex).add(Color.BLACK);
                fontStyles.get(rowIndex).add("normal");
                rowIndex++;
            }
        }
        return rowIndex;
    }

    private int addEditButton(int rowIndex) {
        // add zaehlprotokoll edit buttons in last row of latest zaehlprotokoll:
        editButtons.add(new JButton("Bearbeiten"));
        editButtons.lastElement().addActionListener(this);
        data.get(rowIndex).add(editButtons.lastElement());
        colors.get(rowIndex).add(Color.BLACK);
        fontStyles.get(rowIndex).add("normal");
        rowIndex++;
        return rowIndex;
    }

    @Override
    int fillDataArrayColumn(int colIndex) {
        int rowIndex = super.fillDataArrayColumn(colIndex);

        // ------------- Stornos, Retouren, Entnahmen

        for (BigDecimal mwst : mwstSet) {
            Color color = Color.BLACK;
            if (abrechnungsStornos != null && abrechnungsStornos.get(colIndex).containsKey(mwst)){
                BigDecimal bd = abrechnungsStornos.get(colIndex).get(mwst);
                if (bd.signum() != 0) {
                    color = Color.BLUE;
                }
                data.get(rowIndex).add( bc.priceFormatter(bd)+" "+bc.currencySymbol );
            } else {
                data.get(rowIndex).add( bc.priceFormatter("0")+" "+bc.currencySymbol );
            }
            colors.get(rowIndex).add(color);
            fontStyles.get(rowIndex).add("normal");
            rowIndex++;
        }
        for (BigDecimal mwst : mwstSet) {
            Color color = Color.BLACK;
            if (abrechnungsRetouren != null && abrechnungsRetouren.get(colIndex).containsKey(mwst)){
                BigDecimal bd = abrechnungsRetouren.get(colIndex).get(mwst);
                if (bd.signum() != 0) {
                    color = Color.BLUE;
                }
                data.get(rowIndex).add( bc.priceFormatter(bd)+" "+bc.currencySymbol );
            } else {
                data.get(rowIndex).add( bc.priceFormatter("0")+" "+bc.currencySymbol );
            }
            colors.get(rowIndex).add(color);
            fontStyles.get(rowIndex).add("normal");
            rowIndex++;
        }
        Color color = Color.BLACK;
        if (abrechnungsEntnahmen != null){
            BigDecimal bd = abrechnungsEntnahmen.get(colIndex);
            if (bd.signum() != 0) {
                color = Color.RED;
            }
            data.get(rowIndex).add( bc.priceFormatter(bd)+" "+bc.currencySymbol );
        } else {
            data.get(rowIndex).add( bc.priceFormatter("0")+" "+bc.currencySymbol );
        }
        colors.get(rowIndex).add(color);
        fontStyles.get(rowIndex).add("normal");
        rowIndex++;

        // ------------- Zaehlprotokoll

        for (int i = 0; i < zpNumber; i++) {
            Color def_col = Color.BLACK;
            Color soll_col = Color.BLUE;
            Color good_col = Color.GREEN.darker().darker();
            Color bad_col = Color.RED;
            if (i > 0) {
                def_col = Color.GRAY;
                soll_col = Color.GRAY;
                good_col = Color.GRAY;
                bad_col = Color.GRAY;
            }

            data.get(rowIndex).add(""); // empty row as separator before zaehlprotokoll
            colors.get(rowIndex).add(def_col);
            fontStyles.get(rowIndex).add("normal");
            rowIndex++;
            try {
                data.get(rowIndex).add(zaehlprotokollZeitpunkte.get(colIndex).get(i));
            } catch (ArrayIndexOutOfBoundsException ex) {
                data.get(rowIndex).add("");
            }
            colors.get(rowIndex).add(def_col);
            fontStyles.get(rowIndex).add("bold");
            rowIndex++;
            for (BigDecimal einheit : zpEinheiten) {
                try {
                    Integer anzahl = zaehlprotokolle.get(colIndex).get(i).get(einheit);
                    data.get(rowIndex).add(anzahl);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    data.get(rowIndex).add("");
                }
                colors.get(rowIndex).add(def_col);
                fontStyles.get(rowIndex).add("normal");
                rowIndex++;
            }
            try {
                data.get(rowIndex).add(bc.priceFormatter(zaehlprotokollSummen.get(colIndex).get(i))+" "+bc.currencySymbol);
            } catch (ArrayIndexOutOfBoundsException ex) {
                data.get(rowIndex).add("");
            }
            colors.get(rowIndex).add(def_col);
            fontStyles.get(rowIndex).add("bold");
            rowIndex++;
            try {
                zaehlprotokollZeitpunkte.get(colIndex).get(i);
                // only if the above access works, there are data => add kassenstand
                data.get(rowIndex).add(bc.priceFormatter(zaehlprotokollSollKassenstaende.get(colIndex))+" "+bc.currencySymbol);
            } catch (ArrayIndexOutOfBoundsException ex) {
                data.get(rowIndex).add("");
            }
            colors.get(rowIndex).add(soll_col);
            fontStyles.get(rowIndex).add("bold");
            rowIndex++;
            try {
                data.get(rowIndex).add(bc.priceFormatter(zaehlprotokollDifferenzen.get(colIndex).get(i))+" "+bc.currencySymbol);
                if (zaehlprotokollDifferenzen.get(colIndex).get(i).signum() == 0) {
                    colors.get(rowIndex).add(good_col);
                } else {
                    colors.get(rowIndex).add(bad_col);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                data.get(rowIndex).add("");
                colors.get(rowIndex).add(def_col);
            }
            fontStyles.get(rowIndex).add("bold");
            rowIndex++;
            try {
                data.get(rowIndex).add(bc.priceFormatter(zaehlprotokollEinnahmen.get(colIndex).get(i))+" "+bc.currencySymbol);
            } catch (ArrayIndexOutOfBoundsException ex) {
                data.get(rowIndex).add("");
            }
            colors.get(rowIndex).add(def_col);
            fontStyles.get(rowIndex).add("bold");
            rowIndex++;
            try {
                data.get(rowIndex).add(zaehlprotokollKommentare.get(colIndex).get(i));
            } catch (ArrayIndexOutOfBoundsException ex) {
                data.get(rowIndex).add("");
            }
            colors.get(rowIndex).add(def_col);
            fontStyles.get(rowIndex).add("bold");
            rowIndex++;
            if (i == 0) { // only after first zaehlprotokoll
                try {
                    zaehlprotokollZeitpunkte.get(colIndex).get(i);
                    // only if the above access works, there are data => add button
                    rowIndex = addEditButton(rowIndex);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    data.get(rowIndex).add(""); // empty cell instead of edit zaehlprotokoll button
                    colors.get(rowIndex).add(def_col);
                    fontStyles.get(rowIndex).add("normal");
                    rowIndex++;
                }

            }
        }
        return rowIndex;
    }

    @Override
    void addOtherStuff() {
        JPanel otherPanel = new JPanel();
        submitButton = new JButton("Tagesabrechnung machen");
        submitButton.setEnabled(submitButtonEnabled);
        submitButton.addActionListener(this);
        otherPanel.add(submitButton);
        otherPanel.add(new JLabel("(Zahlen in rot werden als neue Tagesabrechnung gespeichert.)"));
        headerPanel.add(otherPanel);
    }


// ----------------------------------------------------------------------------

    private Integer queryEarliestVerkauf() {
        Integer nr = null;
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MIN(rechnungs_nr) "+
                    "FROM "+tableForMode("verkauf")+" WHERE rechnungs_nr > "+
                    "IFNULL((SELECT MAX(rechnungs_nr_bis) FROM "+tableForMode("abrechnung_tag")+"), 0)");
            rs.next(); nr = rs.getInt(1); rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return nr;
    }

    private Integer queryLatestVerkauf() {
        Integer nr = null;
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MAX(rechnungs_nr) "+
                    "FROM "+tableForMode("verkauf")+" WHERE rechnungs_nr > "+
                    "IFNULL((SELECT MAX(rechnungs_nr_bis) FROM "+tableForMode("abrechnung_tag")+"), 0)");
            rs.next(); nr = rs.getInt(1); rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return nr;
    }

    private String queryVerkaufDate(Integer rechnungs_nr) {
        String date = "";
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT verkaufsdatum FROM "+tableForMode("verkauf")+" WHERE rechnungs_nr = ?"
            );
            pstmtSetInteger(pstmt, 1, rechnungs_nr);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); date = rs.getString(1); rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return date;
    }


    private void showSelectZeitpunktDialog(DateTime firstDate, DateTime lastDate, DateTime nowDate) {
        JDialog dialog = new JDialog(this.mainWindow, "Zeitpunkt manuell auswählen", true);
        SelectZeitpunktForAbrechnungDialog selZeitpunkt = 
            new SelectZeitpunktForAbrechnungDialog(this.pool, this.mainWindow,
                    this, dialog, firstDate, lastDate, nowDate);
        dialog.getContentPane().add(selZeitpunkt, BorderLayout.CENTER);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setVisible(true);
    }

    private String decideOnZeitpunkt(String firstDate, String lastDate, String nowDate) {
        DateTime firstD = new DateTime(firstDate);
        DateTime lastD = new DateTime(lastDate);
        DateTime nowD = new DateTime(nowDate);
        if ( firstD.isSameDayAs(nowD) ){
            // everything is as it should be:
            // all purchases from this abrechnung on the same day
            // simply use now as zeitpunkt of abrechnung
            return nowDate;
        } else {
            // if abrechnung spans more than one day:
            // show dialog window telling user about first and last date and
            // asking her to specify the desired zeitpunkt of abrechnung
            showSelectZeitpunktDialog(firstD, lastD, nowD);
            return this.selectedZeitpunkt;
        }
    }

    private void deleteAbrechnungIfNeedBe(String abrechnungsName, String timeName,
                                          String zeitpunktParsing, String zeitpunkt) {
        /** Delete Monats-/Jahresabrechnung if there was already one for the given zeitpunkt */
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT id FROM "+abrechnungsName+" "+
                "WHERE "+timeName+" = "+zeitpunktParsing
            );
            pstmt.setString(1, zeitpunkt);
            ResultSet rs = pstmt.executeQuery();
            Vector<Integer> ids = new Vector<Integer>();
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
            rs.close();
            pstmt.close();
            for (int id : ids) {
                pstmt = connection.prepareStatement(
                    "DELETE FROM "+abrechnungsName+"_mwst "+
                    "WHERE id = ?"
                );
                pstmt.setInt(1, id);
                int result = pstmt.executeUpdate();
                pstmt.close();
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                        "Fehler: MwSt.-Beträge von alter Abrechnung zu "+timeName+" '"+zeitpunkt+"' "+
                        "konnten nicht aus Tabelle "+
                        "'"+abrechnungsName+"_mwst' gelöscht werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                }
                pstmt = connection.prepareStatement(
                    "DELETE FROM "+abrechnungsName+" "+
                    "WHERE id = ?"
                );
                pstmt.setInt(1, id);
                result = pstmt.executeUpdate();
                pstmt.close();
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                        "Fehler: Alte Abrechnung zu "+timeName+" '"+zeitpunkt+"' "+
                        "konnte nicht aus Tabelle "+
                        "'"+abrechnungsName+"' gelöscht werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    private Integer previousLastSigCounter() {
        Integer prevLastSigCounter = null;
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT IFNULL(MAX(last_tse_sig_counter), 0) FROM "+tableForMode("abrechnung_tag"));
            rs.next();
            prevLastSigCounter = rs.getInt(1);
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return prevLastSigCounter;
    }

    private Integer previousLastTxNumber() {
        Integer prevLastTxNumber = 1;
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT transaction_number FROM tse_transaction "+
                "WHERE rechnungs_nr = (SELECT MAX(rechnungs_nr_bis) FROM "+tableForMode("abrechnung_tag")+")"
            );
            rs.next();
            prevLastTxNumber = rs.getInt(1);
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return prevLastTxNumber;
    }

    private Integer exportTSELog() {
        Integer lastSigCounter = null;
        if (tse.inUse()) {
            lastSigCounter = tse.getSignatureCounter(); // this is the last sig counter that will be included in the export
            Integer prevLastSigCounter = previousLastSigCounter();
            String[] res = setupFinDatDir();
            String exportDir = res[0];
            String dateStr = res[1];
            logger.info("previousLastSigCounter: {}", prevLastSigCounter);
            logger.info("lastSigCounter: {}", lastSigCounter);
            logger.info("Exporting TSE signatures from {} to {}", prevLastSigCounter + 1, lastSigCounter);
            String exportFilename = exportDir+bc.fileSep+"tse_export_"+dateStr+"_Sig_from_"+(prevLastSigCounter + 1)+"_to_"+lastSigCounter+".tar";
            logger.info("exportFilename: {}", exportFilename);
            String message = tse.exportPartialTransactionDataBySigCounter(exportFilename, (long)prevLastSigCounter, null);
            if (!message.equals("OK")) {
                // Try exporting by transaction number, not by signature counter, as a fallback:
                logger.error("!!! Tagesabrechnung TSE export via signature counter using tse.exportMoreData() failed!");
                logger.error("!!! Error message: {}", message);
                logger.error("!!! Trying to export via tx number using tse.exportData()...");
                Integer lastTxNumber = tse.getTransactionNumber(); // this is the last tx number that will be included in the export
                Integer prevLastTxNumber = previousLastTxNumber();
                logger.info("previousLastTxNumber: {}", prevLastTxNumber);
                logger.info("lastTxNumber: {}", lastTxNumber);
                logger.info("Exporting TSE transactions from {} to {}", prevLastTxNumber + 1, lastTxNumber);
                exportFilename = exportDir+bc.fileSep+"tse_export_"+dateStr+"_Tx_from_"+(prevLastTxNumber + 1)+"_to_"+lastTxNumber+".tar";
                logger.info("exportFilename: {}", exportFilename);
                message = tse.exportPartialTransactionDataByTXNumber(exportFilename, (long)(prevLastTxNumber + 1), null, null);
                if (!message.equals("OK")) {
                    // If it still did not work: inform user about failure
                    // lastSigCounter = null; // There can be the problem of TSECommunicationError when trying to export too old (how old?)
                        // transactions. If export fails due to old transactions, and lastSigCounter is set to null,
                        // it will be tried over and over again to start export at those old transactions and it will never work
                        // again. So let's rather live with one failed export file (will have 0 bytes) than break export forever.
                    logger.fatal("Could not create the TSE export for Tagesabrechnung");
                    if (tse.getStatus() != TSEStatus.failed) {
                        tse.setStatus(TSEStatus.failed);
                        tse.setFailReason("Die TSE-Daten des Tages konnten nach der Tagesabrechnung nicht exportiert werden");
                        tse.showTSEFailWarning();
                    }
                    JOptionPane.showMessageDialog(this,
                        "Fehler: TSE-Export des Tagesabschlusses konnte nicht erstellt werden!\n"+
                        "Das ist übel.\n"+
                        "Bitte der/dem Administrator*in Bescheid geben.\n"+
                        "     Fehlermeldung: "+message,
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        return lastSigCounter;
    }

    String substrIfLongerThan(int max_length, String s) {
        if (s.length() > max_length) {
            return s.substring(0, max_length);
        }
        return s;
    }

    private Integer insertTagesAbrechnung() {
        /** create new abrechnung (and save in DB) from time of last abrechnung until now */
        Integer id = null;
        try {
            Integer firstNr = queryEarliestVerkauf();
            Integer lastNr = queryLatestVerkauf();
            String firstDate = queryVerkaufDate(firstNr);
            String lastDate = queryVerkaufDate(lastNr);
            String nowDate = now();
            String zeitpunkt = decideOnZeitpunkt(firstDate, lastDate, nowDate);
            logger.info("Selected Zeitpunkt: "+zeitpunkt);
            if (zeitpunkt == null){
                logger.info("insertTagesAbrechnung was cancelled!");
                return null; // don't do anything, user cancelled (or did not select date properly)
            }
            // get ID of current kassenstand (highest ID due to auto-increment)
            Integer kassenstand_id = mainWindow.retrieveKassenstandId();
            Integer lastSigCounter = null;
            if (bc.operationMode.equals("normal")) {
                lastSigCounter = exportTSELog(); // no TSE exports in training mode
            }

            // Need to do this before inserting new Tagesabrechnung:
            // get netto values grouped by mwst:
            HashMap<BigDecimal, Vector<BigDecimal>> abrechnungNettoBetrag = queryIncompleteAbrechnungTag_VATs();
            // get totals (bar brutto) grouped by mwst:
            HashMap<BigDecimal, BigDecimal> abrechnungBarBrutto = queryIncompleteAbrechnung_BarBruttoVATs();

            // Make an entry in the abrechnung_tag table:
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO "+tableForMode("abrechnung_tag")+" SET "+
                "z_kasse_id = ?, "+
                "zeitpunkt = ?, "+
                "zeitpunkt_real = ?, "+
                "kassenstand_id = ?, " +
                "rechnungs_nr_von = ?, " +
                "rechnungs_nr_bis = ?, " +
                "last_tse_sig_counter = ?"
            );
            pstmt.setString(1, bc.Z_KASSE_ID);
            pstmt.setString(2, zeitpunkt);
            pstmt.setString(3, nowDate);
            pstmtSetInteger(pstmt, 4, kassenstand_id);
            pstmtSetInteger(pstmt, 5, firstNr);
            pstmtSetInteger(pstmt, 6, lastNr);
            pstmtSetInteger(pstmt, 7, lastSigCounter);
            int result = pstmt.executeUpdate();
            pstmt.close();
            if (result == 0) {
                JOptionPane.showMessageDialog(this,
                    "Fehler: Tagesabrechnung konnte nicht gespeichert werden.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            } else {
                id = id();

                // Make entries in the abrechnung_tag_mwst table:
                for ( Map.Entry< BigDecimal, Vector<BigDecimal> > entry : abrechnungNettoBetrag.entrySet() ){
                    BigDecimal mwst_satz = entry.getKey();
                    Vector<BigDecimal> values = entry.getValue();
                    BigDecimal mwst_netto = values.get(1);
                    BigDecimal mwst_betrag = values.get(2);
                    BigDecimal bar_brutto = new BigDecimal("0.00");
                    if ( abrechnungBarBrutto.containsKey(mwst_satz) ){
                        bar_brutto = abrechnungBarBrutto.get(mwst_satz);
                    }
                    pstmt = connection.prepareStatement(
                        "INSERT INTO "+tableForMode("abrechnung_tag_mwst")+" SET "+
                        "id = ?, "+
                        "mwst_satz = ?, "+
                        "mwst_netto = ?, "+
                        "mwst_betrag = ?, "+
                        "bar_brutto = ?"
                    );
                    pstmtSetInteger(pstmt, 1, id);
                    pstmt.setBigDecimal(2, mwst_satz);
                    pstmt.setBigDecimal(3, mwst_netto);
                    pstmt.setBigDecimal(4, mwst_betrag);
                    pstmt.setBigDecimal(5, bar_brutto);
                    result = pstmt.executeUpdate();
                    pstmt.close();
                    if (result == 0){
                        JOptionPane.showMessageDialog(this,
                            "Fehler: MwSt.-Betrag zum MwSt.-Satz '"+mwst_satz+"' der Tagesabrechnung konnte nicht gespeichert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                        id = null;
                    }
                }

                // Make one entry in the abrechnung_tag_tse table with TSE status values:
                LinkedHashMap<String, String> tseStatusValues = null;
                if (tse.inUse()) { // only if TSE is operational, tseStatusValues is not null
                    tseStatusValues = tse.getTSEStatusValues();
                }
                pstmt = connection.prepareStatement(
                    "INSERT INTO "+tableForMode("abrechnung_tag_tse")+" SET "+
                    "id = ?, "+
                    "tse_id = ?, "+
                    "tse_serial = ?, "+
                    "tse_sig_algo = ?, "+
                    "tse_time_format = ?, "+
                    "tse_pd_encoding = ?, "+
                    "tse_public_key = ?, "+
                    "tse_cert_i = ?, "+
                    "tse_cert_ii = ?"
                );
                pstmtSetInteger(pstmt, 1, id);
                pstmtSetInteger(pstmt, 2, bc.TSE_ID);
                if (tseStatusValues != null) {
                    pstmt.setString(3, substrIfLongerThan(68, tseStatusValues.get("Seriennummer der TSE (Hex)")));
                    pstmt.setString(4, substrIfLongerThan(21, tseStatusValues.get("Signatur-Algorithmus")));
                    pstmt.setString(5, substrIfLongerThan(31, tseStatusValues.get("Zeitformat")));
                    pstmt.setString(6, substrIfLongerThan(5, tseStatusValues.get("Encoding der processData-Strings")));
                    pstmt.setString(7, substrIfLongerThan(512, tseStatusValues.get("Öffentlicher Schlüssel (Base64)")));
                    String cert = tseStatusValues.get("TSE-Zertifikat (Base64)");
                    int cert_length = cert.length();
                    pstmt.setString(8, cert.substring(0, cert_length <= 1000 ? cert_length : 1000));
                    if (cert_length > 1000) {
                        pstmt.setString(9, cert.substring(1000, cert_length <= 2000 ? cert_length : 2000));
                        if (cert_length > 2000) {
                            JOptionPane.showMessageDialog(this,
                                "Fehler: TSE-Zertifikat mit "+cert_length+" Zeichen zu lang (mehr als 2000 Zeichen), bitte der/dem Administrator*in Bescheid geben!",
                                "Fehler", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        pstmt.setString(9, null);
                    }
                } else { // TSE not operational, so do not store TSE status values
                    pstmt.setString(3, null);
                    pstmt.setString(4, null);
                    pstmt.setString(5, null);
                    pstmt.setString(6, null);
                    pstmt.setString(7, null);
                    pstmt.setString(8, null);
                    pstmt.setString(9, null);
                    // // For testing:
                    // pstmtSetInteger(pstmt, 2, bc.TSE_ID);
                    // pstmt.setString(3, substrIfLongerThan(68, "4a3f03a2dec81878b432548668f603d14f7b7f90d230e30c87c1a705dce1c890"));
                    // pstmt.setString(4, substrIfLongerThan(21, "ecdsa-plain-SHA256"));
                    // pstmt.setString(5, substrIfLongerThan(31, "unixTime"));
                    // pstmt.setString(6, substrIfLongerThan(5, bc.TSE_PD_ENCODING));
                    // pstmt.setString(7, substrIfLongerThan(512, "BHhWOeisRpPBTGQ1W4VUH95TXx2GARf8e2NYZXJoInjtGqnxJ8sZ3CQpYgjI+LYEmW5A37sLWHsyU7nSJUBemyU="));
                    // String cert = "MEQCIAy4P9k+7x9saDO0uRZ4El8QwN+qTgYiv1DIaJIM-WRiuAiAt+saFDGjK2Yi5Cxgy7PprXQ5O0seRgx4ltdpW9REvwA==abcMEQCIAy4P9k+7x9saDO0uRZ4El8QwN+qTgYiv1DIaJIM-WRiuAiAt+saFDGjK2Yi5Cxgy7PprXQ5O0seRgx4ltdpW9REvwA==abcMEQCIAy4P9k+7x9saDO0uRZ4El8QwN+qTgYiv1DIaJIM-WRiuAiAt+saFDGjK2Yi5Cxgy7PprXQ5O0seRgx4ltdpW9REvwA==abcMEQCIAy4P9k+7x9saDO0uRZ4El8QwN+qTgYiv1DIaJIM-WRiuAiAt+saFDGjK2Yi5Cxgy7PprXQ5O0seRgx4ltdpW9REvwA==abcMEQCIAy4P9k+7x9saDO0uRZ4El8QwN+qTgYiv1DIaJIM-WRiuAiAt+saFDGjK2Yi5Cxgy7PprXQ5O0seRgx4ltdpW9REvwA==abcMEQCIAy4P9k+7x9saDO0uRZ4El8QwN+qTgYiv1DIaJIM-WRiuAiAt+saFDGjK2Yi5Cxgy7PprXQ5O0seRgx4ltdpW9REvwA==abcMEQCIAy4P9k+7x9saDO0uRZ4El8QwN+qTgYiv1DIaJIM-WRiuAiAt+saFDGjK2Yi5Cxgy7PprXQ5O0seRgx4ltdpW9REvwA==abcMEQCIAy4P9k+7x9saDO0uRZ4El8QwN+qTgYiv1DIaJIM-WRiuAiAt+saFDGjK2Yi5Cxgy7PprXQ5O0seRgx4ltdpW9REvwA==abcMEQCIAy4P9k+7x9saDO0uRZ4El8QwN+qTgYiv1DIaJIM-WRiuAiAt+saFDGjK2Yi5Cxgy7PprXQ5O0seRgx4ltdpW9REvwA==abcMEQCIAy4";
                    // int cert_length = cert.length();
                    // pstmt.setString(8, cert.substring(0, cert_length <= 1000 ? cert_length : 1000));
                    // if (cert_length > 1000) {
                    //     pstmt.setString(9, cert.substring(1000, cert_length <= 2000 ? cert_length : 2000));
                    //     if (cert_length > 2000) {
                    //         JOptionPane.showMessageDialog(this,
                    //             "Fehler: TSE-Zertifikat mit "+cert_length+" Zeichen zu lang (mehr als 2000 Zeichen), bitte der/dem Administrator*in Bescheid geben!",
                    //             "Fehler", JOptionPane.ERROR_MESSAGE);
                    //     }
                    // } else {
                    //     pstmt.setString(9, null);
                    // }
                }
                result = pstmt.executeUpdate();
                pstmt.close();
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                        "Fehler: TSE-Status-Werte konnten nicht zur Tagesabrechnung dazu gespeichert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                    id = null;
                }
            }

            connection.close();
            // NEED TO REDO Monats/Jahresabrechnung if needed (check if zeitpunkt lies in old month/year)!!!
            deleteAbrechnungIfNeedBe(tableForMode("abrechnung_monat"), "monat", "DATE_FORMAT(?, '%Y-%m-01')", zeitpunkt);
            deleteAbrechnungIfNeedBe(tableForMode("abrechnung_jahr"), "jahr", "YEAR(?)", zeitpunkt);

            // *********************************
            // * Write into DSFinV-K CSV files *
            // *********************************
            if (id != null) {
                writeIntoDSFinVKCSVFiles(id);
            }
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            JOptionPane.showMessageDialog(this,
                    "Fehler: Tagesabrechnung konnte nicht gespeichert werden.\n"+
                    "Keine Verbindung zum Datenbank-Server?\n"+
                    "Fehlermeldung: "+ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            id = null;
        }
        return id;
    }

    private void writeIntoDSFinVKCSVFiles(Integer id) {
        // Retrieve generally used values
        HashMap<String, String> zvalues = dsfinvk.get_ZKasseID_ZErstellung_ZNr(id);
        
        // STAMMDATENMODUL
        dsfinvk.writeToCSV_Stamm_Abschluss(id);
        dsfinvk.writeToCSV_Stamm_Orte(id, zvalues);
        dsfinvk.writeToCSV_Stamm_Kassen(id, zvalues);
        dsfinvk.writeToCSV_Stamm_Terminals(id, zvalues);
        dsfinvk.writeToCSV_Stamm_Agenturen(id);
        dsfinvk.writeToCSV_Stamm_USt(id, zvalues);
        dsfinvk.writeToCSV_Stamm_TSE(id, zvalues);

        // KASSENABSCHLUSSMODUL
        dsfinvk.writeToCSV_Z_GV_Typ(id, zvalues);

        // EINZELAUFZEICHNUNGSMODUL
        dsfinvk.writeToCSV_TSE_Transaktionen(id);
    }

    private void writeIntoDSFinVKCSVFilesTemporary(Integer id) {
        System.out.println(id);

        // Retrieve generally used values
        HashMap<String, String> zvalues = dsfinvk.get_ZKasseID_ZErstellung_ZNr(id);

        dsfinvk.writeToCSV_Stamm_USt(id, zvalues); // redo because now USt is sorted by DSFinV-K USt-Schlüssel

        // added in this version update:
        dsfinvk.writeToCSV_Z_GV_Typ(id, zvalues);
        
        dsfinvk.writeToCSV_TSE_Transaktionen(id); // need to redo due to bug
    }

    private Integer maxZaehlprotokollID() {
        Integer maxZaehlID = null;
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MAX(id) FROM "+tableForMode("zaehlprotokoll"));
            rs.next();
            maxZaehlID = rs.getInt(1);
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return maxZaehlID;
    }

    private void insertZaehlprotokoll(Integer abrechnung_tag_id) {
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "INSERT INTO "+tableForMode("zaehlprotokoll")+" SET abrechnung_tag_id = ?, "+
                    "zeitpunkt = ?, "+
                    "kommentar = ?"
            );
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            pstmt.setString(2, now());
            pstmt.setString(3, zaehlprotokollKommentar);
            int result = pstmt.executeUpdate();
            pstmt.close();
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Zählprotokoll konnte nicht gespeichert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
            for ( Map.Entry<BigDecimal, Integer> entry : zaehlprotokoll.entrySet() ){
                BigDecimal wert = entry.getKey();
                Integer anzahl = entry.getValue();
                pstmt = connection.prepareStatement(
                    "INSERT INTO "+tableForMode("zaehlprotokoll_details")+" SET zaehlprotokoll_id = ?, "+
                    "anzahl = ?, "+
                    "einheit = ?"
                );
                pstmtSetInteger(pstmt, 1, maxZaehlprotokollID());
                pstmtSetInteger(pstmt, 2, anzahl);
                pstmt.setBigDecimal(3, wert);
                result = pstmt.executeUpdate();
                pstmt.close();
                if (result == 0) {
                    break;
                }
            }
            connection.close();
            if (result == 0) {
                JOptionPane.showMessageDialog(this,
                        "Fehler: Zählprotokoll-Details konnten nicht gespeichert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            JOptionPane.showMessageDialog(this,
                    "Fehler: Zählprotokoll konnte nicht gespeichert werden.\n"+
                    "Keine Verbindung zum Datenbank-Server?\n"+
                    "Fehlermeldung: "+ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setZaehlprotokollInactive(Integer abrechnung_tag_id) {
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "UPDATE "+tableForMode("zaehlprotokoll")+" SET aktiv = FALSE "+
                    "WHERE abrechnung_tag_id = ? AND aktiv = TRUE"
            );
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            int result = pstmt.executeUpdate();
            pstmt.close();
            connection.close();
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Altes Zählprotokoll konnte nicht inaktiv gesetzt werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            JOptionPane.showMessageDialog(this,
                    "Fehler: Altes Zählprotokoll konnte nicht inaktiv gesetzt werden.\n"+
                    "Keine Verbindung zum Datenbank-Server?\n"+
                    "Fehlermeldung: "+ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showZaehlprotokollDialog() {
        JDialog dialog = new JDialog(this.mainWindow, "Erfassung des Kassenbestands", true);
        ZaehlprotokollDialog zd = new ZaehlprotokollDialog(this.pool, this.mainWindow, this, dialog);
        dialog.getContentPane().add(zd, BorderLayout.CENTER);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void showZaehlprotokollEditDialog(int editIndex) {
        JDialog dialog = new JDialog(this.mainWindow, "Bearbeitung des Kassenbestands", true);
        ZaehlprotokollDialog zd = new ZaehlprotokollDialog(this.pool, this.mainWindow, this, dialog);
        zd.setZaehlprotokoll(zaehlprotokolle.get(editIndex).get(0));
        zd.setKassenstand(zaehlprotokollSollKassenstaende.get(editIndex));
        zd.setKommentarErklaerText("Bitte rechts eingeben:\n"+
                "Was ist der Grund für diese Änderung?\n"+
                "Und ggf.: Was ist der Grund für die verbleibende Differenz?");
        dialog.getContentPane().add(zd, BorderLayout.CENTER);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void showKassenstandZuruecksetzenDialog() {
        JDialog dialog = new JDialog(this.mainWindow, "Kassenstand zurücksetzen", true);
        KassenstandZuruecksetzenDialog kzd = new KassenstandZuruecksetzenDialog(this.pool, this.mainWindow, dialog, this.zaehlprotokoll, this);
        dialog.getContentPane().add(kzd, BorderLayout.CENTER);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setVisible(true);
    }

    @Override
    Vector<Object> fillSpreadSheet(int exportIndex) {
        Vector<Object> v = super.fillSpreadSheet(exportIndex);
        Sheet sheet = (Sheet) v.firstElement();
        Integer rowIndex = (Integer) v.lastElement();

        // This is used only to find out what MwSt. is present:
        HashMap<BigDecimal, Vector<BigDecimal>> vats = abrechnungsVATs.get(exportIndex); // map with values for each mwst

        // Set Stornos, Retouren, Entnahmen:
        for (BigDecimal mwst : mwstSet) {
            if (vats.containsKey(mwst)) {
                sheet.setValueAt("Storno ("+bc.vatFormatter(mwst) + " MwSt.)", 0, rowIndex);
                if (abrechnungsStornos != null && abrechnungsStornos.get(exportIndex).containsKey(mwst)){
                    BigDecimal bd = abrechnungsStornos.get(exportIndex).get(mwst);
                    sheet.setValueAt(bd, 1, rowIndex);
                } else {
                    sheet.setValueAt(0., 1, rowIndex);
                }
                rowIndex++;
            }
        }
        for (BigDecimal mwst : mwstSet) {
            if (vats.containsKey(mwst)) {
                sheet.setValueAt("Retouren ("+bc.vatFormatter(mwst) + " MwSt.)", 0, rowIndex);
                if (abrechnungsRetouren != null && abrechnungsRetouren.get(exportIndex).containsKey(mwst)){
                    BigDecimal bd = abrechnungsRetouren.get(exportIndex).get(mwst);
                    sheet.setValueAt(bd, 1, rowIndex);
                } else {
                    sheet.setValueAt(0., 1, rowIndex);
                }
                rowIndex++;
            }
        }
        sheet.setValueAt("Entnahmen", 0, rowIndex);
        if (abrechnungsEntnahmen != null){
            BigDecimal bd = abrechnungsEntnahmen.get(exportIndex);
            sheet.setValueAt(bd, 1, rowIndex);
        } else {
            sheet.setValueAt(0., 1, rowIndex);
        }

        // Set fixed rowIndex here, so that special formatting is for the correct cells, also if less than two
        // MwSt.s are in use.
        // This will work as long as one or two MwSt. values are used.
        rowIndex = 27;

        // Set Zaehlprotokoll:
        if (zpNumber > 0) {
            int i = 0; // only print first (most recent) zaehlprotokoll
            try {
                sheet.setValueAt("Zählprotokoll:", 0, rowIndex);
                Date zpDate = createDate(zaehlprotokollZeitpunkte.get(exportIndex).get(i));
                logger.trace(zpDate);
                sheet.setValueAt(zpDate, 1, rowIndex);
            } catch (ArrayIndexOutOfBoundsException ignored) {}
            rowIndex++;
            for (BigDecimal einheit : zpEinheiten) {
                try {
                    sheet.setValueAt(bc.priceFormatter(einheit)+" "+bc.currencySymbol+", Anzahl:", 0, rowIndex);
                    Integer anzahl = zaehlprotokolle.get(exportIndex).get(i).get(einheit);
                    sheet.setValueAt(anzahl, 1, rowIndex);
                    sheet.setValueAt("Wert:", 2, rowIndex);
                    sheet.setValueAt(new BigDecimal(anzahl).multiply(einheit), 3, rowIndex);
                } catch (ArrayIndexOutOfBoundsException ignored) {}
                rowIndex++;
            }
            try {
                sheet.setValueAt("Zähl-Kassenstand:", 0, rowIndex);
                sheet.setValueAt(zaehlprotokollSummen.get(exportIndex).get(i), 1, rowIndex);
            } catch (ArrayIndexOutOfBoundsException ignored) {}
            rowIndex++;
            try {
                zaehlprotokollZeitpunkte.get(exportIndex).get(i);
                // only if the above access works, there are data => add kassenstand
                sheet.setValueAt("Soll-Kassenstand:", 0, rowIndex);
                sheet.setValueAt(zaehlprotokollSollKassenstaende.get(exportIndex), 1, rowIndex);
            } catch (ArrayIndexOutOfBoundsException ignored) {}
            rowIndex++;
            try {
                sheet.setValueAt("Differenz", 0, rowIndex);
                sheet.setValueAt(zaehlprotokollDifferenzen.get(exportIndex).get(i), 1, rowIndex);
            } catch (ArrayIndexOutOfBoundsException ignored) {}
            rowIndex++;
            try {
                sheet.setValueAt("Bar-Einnahmen", 0, rowIndex);
                sheet.setValueAt(zaehlprotokollEinnahmen.get(exportIndex).get(i), 1, rowIndex);
            } catch (ArrayIndexOutOfBoundsException ignored) {}
            rowIndex++;
            try {
                sheet.setValueAt("Kommentar", 0, rowIndex);
                sheet.setValueAt(zaehlprotokollKommentare.get(exportIndex).get(i), 1, rowIndex);
            } catch (ArrayIndexOutOfBoundsException ignored) {}
            rowIndex++;
        }

        v = new Vector<>();
        v.add(sheet);
        v.add(rowIndex);
        return v;
    }

    private String toStringIfNotNull(Object thing) {
        if (thing != null) {
            return thing.toString();
        } else {
            return "";
        }
    }

    // public HashMap<BigDecimal, Integer> getMwstDSFinVKSchluessel() {
    // HashMap<BigDecimal, Integer> result = new HashMap<BigDecimal, Integer>();
    // try {
    // Connection connection = this.pool.getConnection();
    // PreparedStatement pstmt = connection.prepareStatement(
    // "SELECT mwst_satz, dsfinvk_ust_schluessel "+ // dsfinvk_ust_beschr
    // "FROM mwst");
    // ResultSet rs = pstmt.executeQuery();
    // while (rs.next()) {
    // result.put(rs.getBigDecimal(1), rs.getInt(2));
    // }
    // rs.close();
    // pstmt.close();
    // connection.close();
    // } catch (SQLException ex) {
    // logger.error("Exception:", ex);
    // showDBErrorDialog(ex.getMessage());
    // }
    // return result;
    // }

    // private String getSteuerschluessel(BigDecimal mwst, HashMap<BigDecimal, Integer> mwstDSFinVKSchluessel) {
    //     String steuerschluessel = "";
    //     Integer dsfinvk_schluessel = mwstDSFinVKSchluessel.get(mwst);
    //     if (dsfinvk_schluessel == 6) {
    //         // these are bookings without VAT (tax-free)
    //         steuerschluessel = toStringIfNotNull(bc.LEXWARE_STEUERSCHLUESSEL_OHNE_STEUER);
    //     } else if (dsfinvk_schluessel == 2) {
    //         // reduced VAT rate
    //         steuerschluessel = toStringIfNotNull(bc.LEXWARE_STEUERSCHLUESSEL_REDUZIERTE_STEUER);
    //     } else if (dsfinvk_schluessel == 1) {
    //         // normal VAT rate
    //         steuerschluessel = toStringIfNotNull(bc.LEXWARE_STEUERSCHLUESSEL_NORMALE_STEUER);
    //     }
    //     return steuerschluessel;
    // }
    // problem with above approach: when VAT changes, we cannot export old
    // data correctly anymore

    private HashMap<String, String> getLexwareDataErloese(BigDecimal mwst) {
        String buchungstext = "";
        String sollkonto = "";
        String habenkonto = "";
        String steuerschluessel = "";
        if (mwst.compareTo(new BigDecimal("0")) <= 0) {
            // these are bookings without VAT (tax-free)
            buchungstext = bc.LEXWARE_BUCHUNGSTEXT_ERLOESE_OHNE_STEUER;
            sollkonto = toStringIfNotNull(bc.LEXWARE_SOLL_KONTO_ERLOESE);
            habenkonto = toStringIfNotNull(bc.LEXWARE_HABEN_KONTO_ERLOESE_OHNE_STEUER);
            steuerschluessel = toStringIfNotNull(bc.LEXWARE_STEUERSCHLUESSEL_OHNE_STEUER);
        } else if (mwst.compareTo(new BigDecimal("0.14")) < 0) {
            // if VAT smaller than 14%, assume that this is the "reduced" VAT rate
            // this leaves room for normal VAT being reduced (e.g. to 16%)
            // as well as reduced VAT being raised (e.g. to 10% or 12%)
            buchungstext = bc.LEXWARE_BUCHUNGSTEXT_ERLOESE.replaceAll("\\{\\{MWST\\}\\}", bc.vatFormatter(mwst));
            sollkonto = toStringIfNotNull(bc.LEXWARE_SOLL_KONTO_ERLOESE);
            habenkonto = toStringIfNotNull(bc.LEXWARE_HABEN_KONTO_ERLOESE_REDUZIERTE_STEUER);
            steuerschluessel = toStringIfNotNull(bc.LEXWARE_STEUERSCHLUESSEL_REDUZIERTE_STEUER);
        } else {
            // assume that this is the normal VAT rate
            buchungstext = bc.LEXWARE_BUCHUNGSTEXT_ERLOESE.replaceAll("\\{\\{MWST\\}\\}", bc.vatFormatter(mwst));
            sollkonto = toStringIfNotNull(bc.LEXWARE_SOLL_KONTO_ERLOESE);
            habenkonto = toStringIfNotNull(bc.LEXWARE_HABEN_KONTO_ERLOESE_NORMALE_STEUER);
            steuerschluessel = toStringIfNotNull(bc.LEXWARE_STEUERSCHLUESSEL_NORMALE_STEUER);
        }
        HashMap<String, String> result = new HashMap<String, String>();
        result.put("buchungstext", buchungstext);
        result.put("sollkonto", sollkonto);
        result.put("habenkonto", habenkonto);
        result.put("steuerschluessel", steuerschluessel);
        return result;
    }

    @Override
    void writeLexwareCSVFile(String filepathString, int exportIndex) {
        // Do not even need to call the super-method because it's empty
        // super.writeLexwareCSVFile(filepathString, exportIndex);

        // Get data
        String date = abrechnungsDates.get(exportIndex);
        String formattedDate = formatDate(date, bc.LEXWARE_BELEGDATUM_FORMAT);
        String id = abrechnungsIDs.get(exportIndex).toString(); // Laufende Nummer
        Vector<BigDecimal> totals = abrechnungsTotals.get(exportIndex);
        HashMap<BigDecimal, Vector<BigDecimal>> vats = abrechnungsVATs.get(exportIndex); // map with values for each
        // HashMap<BigDecimal, Integer> mwstDSFinVKSchluessel =
        // getMwstDSFinVKSchluessel();

        // Prepare meta-data on the CSV columns (defined in 'Schnittstellenbeschreibung
        // Lexware buchhaltung.pdf',
        // downloaded from
        // https://delta.lexware.de/sf_get_wmattachment.php?att=13299f5270c5ed27c1dff8f1470d0e,
        // linked on page
        // https://www.lexware.de/support/faq/faq-beitrag/000015582/?tx_support%5Bproduct%5D=34&tx_support_faqdetail%5Baction%5D=detail&tx_support_faqdetail%5Bcontroller%5D=Faq&cHash=61d8c14be3c71240cd4f5736203f97f1#section2)
        LinkedHashMap<String, CSVColumn> colDefs = new LinkedHashMap<String, CSVColumn>();
        CSVColumn col = new CSVColumn();
        col.type = CSVColumnType.ALPHANUMERIC;
        colDefs.put("Belegdatum", col);
        col = new CSVColumn();
        col.type = CSVColumnType.ALPHANUMERIC;
        col.maxLength = 9;
        colDefs.put("Belegnummernkreis", col);
        col = new CSVColumn();
        col.type = CSVColumnType.ALPHANUMERIC;
        col.maxLength = 10;
        colDefs.put("Belegnummer", col);
        col = new CSVColumn();
        col.type = CSVColumnType.ALPHANUMERIC;
        col.maxLength = 79;
        colDefs.put("Buchungstext", col);
        col = new CSVColumn();
        col.type = CSVColumnType.NUMERIC;
        colDefs.put("Sollkonto", col);
        col = new CSVColumn();
        col.type = CSVColumnType.NUMERIC;
        colDefs.put("Habenkonto", col);
        col = new CSVColumn();
        col.type = CSVColumnType.NUMERIC;
        colDefs.put("Steuerschlüssel", col);
        col = new CSVColumn();
        col.type = CSVColumnType.ALPHANUMERIC;
        col.maxLength = 19;
        colDefs.put("Kostenstelle 1", col);
        col = new CSVColumn();
        col.type = CSVColumnType.ALPHANUMERIC;
        col.maxLength = 19;
        colDefs.put("Kostenstelle 2", col);
        col = new CSVColumn();
        col.type = CSVColumnType.NUMERIC;
        col.accuracy = 2;
        colDefs.put("Buchungsbetrag Euro", col);
        col = new CSVColumn();
        col.type = CSVColumnType.NUMERIC;
        colDefs.put("Zusatzangaben", col);

        // 1. Row for the card-based paymenys
        // Prepare the data for writing
        HashMap<String, String> fields = new HashMap<String, String>();
        fields.put("Belegdatum", formattedDate);
        fields.put("Belegnummernkreis", bc.LEXWARE_BELEGNUMMERNKREIS);
        fields.put("Belegnummer", id); // Laufende Nummer
        fields.put("Buchungstext", bc.LEXWARE_BUCHUNGSTEXT_GELDTRANSIT_KARTE);
        fields.put("Sollkonto", toStringIfNotNull(bc.LEXWARE_SOLL_KONTO_GELDTRANSIT_KARTE));
        fields.put("Habenkonto", toStringIfNotNull(bc.LEXWARE_HABEN_KONTO_GELDTRANSIT_KARTE));
        fields.put("Steuerschlüssel", "");
        fields.put("Kostenstelle 1", bc.LEXWARE_KOSTENSTELLE_1);
        fields.put("Kostenstelle 2", bc.LEXWARE_KOSTENSTELLE_2);
        fields.put("Buchungsbetrag Euro", totals.get(2).toString());
        fields.put("Zusatzangaben", toStringIfNotNull(bc.LEXWARE_ZUSATZANGABEN));
        // Write to the CSV file
        CSVExport.writeToCSV(filepathString, fields, colDefs, this.bc, ";", "\n", ',', '.', "\"");

        // 2. Rows for each VAT rate
        // Prepare the data for writing
        for (Map.Entry<BigDecimal, Vector<BigDecimal>> entry : vats.entrySet()) {
            BigDecimal mwst = entry.getKey();
            HashMap<String, String> lex_data = getLexwareDataErloese(mwst);

            fields = new HashMap<String, String>();
            fields.put("Belegdatum", formattedDate);
            fields.put("Belegnummernkreis", bc.LEXWARE_BELEGNUMMERNKREIS);
            fields.put("Belegnummer", id); // Laufende Nummer
            fields.put("Buchungstext", lex_data.get("buchungstext"));
            fields.put("Sollkonto", lex_data.get("sollkonto"));
            fields.put("Habenkonto", lex_data.get("habenkonto"));
            fields.put("Steuerschlüssel", lex_data.get("steuerschluessel"));
            fields.put("Kostenstelle 1", bc.LEXWARE_KOSTENSTELLE_1);
            fields.put("Kostenstelle 2", bc.LEXWARE_KOSTENSTELLE_2);
            fields.put("Buchungsbetrag Euro", entry.getValue().get(0).toString());
            fields.put("Zusatzangaben", toStringIfNotNull(bc.LEXWARE_ZUSATZANGABEN));
            // Write to the CSV file
            CSVExport.writeToCSV(filepathString, fields, colDefs, this.bc, ";", "\n", ',', '.', "\"");
        }

        logger.info("Written CSV file to " + filepathString);
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e){
        super.actionPerformed(e);
        if (e.getSource() == prevButton){
            if (this.currentPage > 1)
                this.currentPage--;
            updateTable();
            return;
        }
        if (e.getSource() == nextButton){
            if (this.currentPage < totalPage)
                this.currentPage++;
            updateTable();
            return;
        }
        if (e.getSource() == submitButton){
            showZaehlprotokollDialog();
            if (this.zaehlprotokoll != null) {
                Integer id = insertTagesAbrechnung();
                if (id != null) {
                    insertZaehlprotokoll(id);
                    kassenstandWasChanged = false;
                    showKassenstandZuruecksetzenDialog();
                    tabbedPane.kassenstandNeedsToChange = !kassenstandWasChanged;
                    mainWindow.updateBottomPanel();
                    JOptionPane.showMessageDialog(this,
                            "Bitte im folgenden Fenster auf 'Speichern' klicken.\n"+
                            "Danach öffnet sich die Abrechnung automatisch.\n\n"+
                            "Bitte dann die Abrechnung drucken (z.B. mit Strg-P).",
                            "Hinweis", JOptionPane.INFORMATION_MESSAGE);
                    abrechTabbedPane.recreateTabbedPane(0);
                } else {
                  abrechTabbedPane.recreateTabbedPane();
                }
            }
            return;
        }
        int editIndex = -1;
        for (int i=0; i<editButtons.size(); i++){
            if (e.getSource() == editButtons.get(i) ){
                editIndex = i;
//                System.out.println("editIndex: "+editIndex);
                break;
            }
        }
        if (editIndex > -1){
            showZaehlprotokollEditDialog(editIndex);
            if (this.zaehlprotokoll != null) {
                Integer id = abrechnungsIDs.get(editIndex);
                if (id != null) {
                    setZaehlprotokollInactive(id);
                    insertZaehlprotokoll(id);
                }
                abrechTabbedPane.recreateTabbedPane();
            }
        }
    }
}
