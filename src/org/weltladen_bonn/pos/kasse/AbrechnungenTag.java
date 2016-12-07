package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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

public class AbrechnungenTag extends Abrechnungen {
    // Attribute:
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
    private Vector< Vector< LinkedHashMap<BigDecimal, Integer> > > zaehlprotokolle;

    private Integer zpNumber;
    private TreeSet<BigDecimal> zpEinheiten;

    private Vector<JButton> editButtons;

    private Boolean kassenstandWasChanged = false;

    // Methoden:
    /**
     *    The constructor.
     *       */
    public AbrechnungenTag(Connection conn, MainWindowGrundlage mw, AbrechnungenTabbedPane atp, TabbedPane tp, Integer exportIndex){
        super(conn, mw, "", "Tagesabrechnung", "yyyy-MM-dd HH:mm:ss", "dd.MM. HH:mm (E)",
                "zeitpunkt", "abrechnung_tag");
        this.abrechTabbedPane = atp;
        this.tabbedPane = tp;
        showTable();
        if (exportIndex != null) {
          export(exportIndex);
        }
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


    private ResultSet doQueryStornos(Integer abrechnung_tag_id) throws SQLException {
        // Summe über Stornos:
        PreparedStatement pstmt = this.conn.prepareStatement(
                // SELECT mwst_satz, SUM(mwst_netto + mwst_betrag) FROM verkauf_mwst INNER JOIN verkauf USING (rechnungs_nr) WHERE storniert = TRUE AND verkaufsdatum > IFNULL((SELECT zeitpunkt_real FROM abrechnung_tag WHERE id = 17 LIMIT 1), '0001-01-01') AND verkaufsdatum < IFNULL((SELECT zeitpunkt_real FROM abrechnung_tag WHERE id = 18 LIMIT 1), '9999-01-01') GROUP BY mwst_satz;
                "SELECT mwst_satz, SUM(mwst_netto + mwst_betrag) " +
                        "FROM verkauf_mwst INNER JOIN verkauf USING (rechnungs_nr) " +
                        "WHERE storniert = TRUE AND " +
                        "verkaufsdatum >= IFNULL((SELECT zeitpunkt_real FROM abrechnung_tag WHERE id = ? LIMIT 1), '0001-01-01') AND " +
                        "verkaufsdatum < IFNULL((SELECT zeitpunkt_real FROM abrechnung_tag WHERE id = ? LIMIT 1), '9999-01-01') " +
                        "GROUP BY mwst_satz"
        );
        pstmtSetInteger(pstmt, 1, abrechnung_tag_id - 1);
        pstmtSetInteger(pstmt, 2, abrechnung_tag_id);
        return pstmt.executeQuery();
    }

    private ResultSet doQueryRetouren(Integer abrechnung_tag_id) throws SQLException {
        // Summe über Retouren:
        PreparedStatement pstmt = this.conn.prepareStatement(
                // SELECT mwst_satz, SUM(ges_preis) FROM verkauf_details INNER JOIN verkauf USING (rechnungs_nr) INNER JOIN artikel USING (artikel_id) WHERE storniert = FALSE AND verkaufsdatum > IFNULL((SELECT zeitpunkt_real FROM abrechnung_tag WHERE id = 0 LIMIT 1), '0001-01-01') AND verkaufsdatum < IFNULL((SELECT zeitpunkt_real FROM abrechnung_tag WHERE id = 9999999999999 LIMIT 1), '9999-01-01') AND stueckzahl < 0 AND produktgruppen_id >= 9 GROUP BY mwst_satz;
                "SELECT mwst_satz, SUM(ges_preis) " +
                        "FROM verkauf_details " +
                        "INNER JOIN verkauf USING (rechnungs_nr) " +
                        "LEFT JOIN artikel USING (artikel_id) " + // left join needed because Rabattaktionen do not have an artikel_id
                        "WHERE storniert = FALSE AND " +
                        "verkaufsdatum >= IFNULL((SELECT zeitpunkt_real FROM abrechnung_tag WHERE id = ? LIMIT 1), '0001-01-01') AND " +
                        "verkaufsdatum < IFNULL((SELECT zeitpunkt_real FROM abrechnung_tag WHERE id = ? LIMIT 1), '9999-01-01') AND " +
                        "stueckzahl < 0 AND ( produktgruppen_id NOT IN (1, 6, 7, 8) OR produktgruppen_id IS NULL ) " + // exclude internal articles, Gutschein, and Pfand
                        // produktgruppen_id is null for Rabattaktionen
                        "GROUP BY mwst_satz"
        );
        pstmtSetInteger(pstmt, 1, abrechnung_tag_id - 1);
        pstmtSetInteger(pstmt, 2, abrechnung_tag_id);
        return pstmt.executeQuery();
    }

    private ResultSet doQueryEntnahmen(Integer abrechnung_tag_id) throws SQLException {
        // Summe über Entnahmen:
        PreparedStatement pstmt = this.conn.prepareStatement(
                // SELECT SUM(entnahme_betrag) FROM (SELECT kassenstand_id AS kid, neuer_kassenstand - (SELECT neuer_kassenstand FROM kassenstand WHERE kassenstand_id = kid-1) AS entnahme_betrag FROM kassenstand WHERE buchungsdatum > IFNULL((SELECT zeitpunkt_real FROM abrechnung_tag WHERE id = 0 LIMIT 1), '0001-01-01') AND buchungsdatum < IFNULL((SELECT zeitpunkt_real FROM abrechnung_tag WHERE id = 9999999999999 LIMIT 1), '9999-01-01') AND entnahme = TRUE) AS entnahme_table;
                "SELECT SUM(entnahme_betrag) " +
                        "FROM (" +
                        "SELECT kassenstand_id AS kid, " +
                        "neuer_kassenstand - (SELECT neuer_kassenstand FROM kassenstand WHERE kassenstand_id = kid-1) AS entnahme_betrag " +
                        "FROM kassenstand WHERE " +
                        "buchungsdatum >= IFNULL((SELECT zeitpunkt_real FROM abrechnung_tag WHERE id = ? LIMIT 1), '0001-01-01') AND " +
                        "buchungsdatum < IFNULL((SELECT zeitpunkt_real FROM abrechnung_tag WHERE id = ? LIMIT 1), '9999-01-01') AND " +
                        "entnahme = TRUE" +
                        ") AS entnahme_table"
        );
        pstmtSetInteger(pstmt, 1, abrechnung_tag_id - 1);
        pstmtSetInteger(pstmt, 2, abrechnung_tag_id);
        return pstmt.executeQuery();
    }

    void queryStornosRetourenEntnahmen() {
        // the queries concerning Stornierungen, Retouren and Entnahmen
        abrechnungsStornos = new Vector<>();
        abrechnungsRetouren = new Vector<>();
        abrechnungsEntnahmen = new Vector<>();
        try {
            for (Integer id : abrechnungsIDs) {
                // Summe über Stornos:
                ResultSet rs = doQueryStornos(id);
                HashMap<BigDecimal, BigDecimal> map = new HashMap<>();
                while (rs.next()) {
                    BigDecimal mwst_satz = rs.getBigDecimal(1);
                    BigDecimal storno_sum = rs.getBigDecimal(2);
                    map.put(mwst_satz, storno_sum);
                    mwstSet.add(mwst_satz);
                }
                rs.close();
                abrechnungsStornos.add(map);

                // Summe über Retouren:
                rs = doQueryRetouren(id);
                HashMap<BigDecimal, BigDecimal> map2 = new HashMap<>();
                while (rs.next()) {
                    BigDecimal mwst_satz = rs.getBigDecimal(1);
                    BigDecimal retoure_sum = rs.getBigDecimal(2);
                    map2.put(mwst_satz, retoure_sum);
                    mwstSet.add(mwst_satz);
                }
                rs.close();
                abrechnungsRetouren.add(map2);

                // Summe über Entnahmen:
                rs = doQueryEntnahmen(id);
                BigDecimal entnahme_sum = new BigDecimal("0.00");
                if (rs.next()) {
                    BigDecimal sum = rs.getBigDecimal(1);
                    if (sum != null) {
                        entnahme_sum = sum;
                    }
                }
                abrechnungsEntnahmen.add(entnahme_sum);
                rs.close();
            }
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void queryZaehlprotokoll() {
        // the queries concerning zaehlprotokolle
        zaehlprotokollZeitpunkte = new Vector<>();
        zaehlprotokollKommentare = new Vector<>();
        zaehlprotokollSummen = new Vector<>();
        zaehlprotokollSollKassenstaende = new Vector<>();
        zaehlprotokollDifferenzen = new Vector<>();
        zaehlprotokolle = new Vector<>();
        try {
            for (Integer id : abrechnungsIDs) {
                PreparedStatement pstmt = this.conn.prepareStatement(
                        "SELECT id, zeitpunkt, kommentar, aktiv " +
                                "FROM zaehlprotokoll " +
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
                    PreparedStatement pstmt2 = this.conn.prepareStatement(
                            "SELECT SUM(anzahl*einheit) " +
                                    "FROM zaehlprotokoll_details " +
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
                    pstmt2 = this.conn.prepareStatement(
                            "SELECT einheit, anzahl " +
                                    "FROM zaehlprotokoll_details " +
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
                pstmt = this.conn.prepareStatement(
                        "SELECT neuer_kassenstand " +
                                "FROM kassenstand INNER JOIN abrechnung_tag USING (kassenstand_id) " +
                                "WHERE abrechnung_tag.id = ? " +
                                "LIMIT 1"
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
                //
                zaehlprotokollZeitpunkte.add(zeitpunkte);
                zaehlprotokollKommentare.add(kommentare);
                zaehlprotokollSummen.add(summen);
                if (sollKassenstand != null) {
                    zaehlprotokollSollKassenstaende.add(sollKassenstand);
                }
                zaehlprotokollDifferenzen.add(differenzen);
                zaehlprotokolle.add(zps);
            }
//            System.out.println(zaehlprotokollZeitpunkte);
//            System.out.println(zaehlprotokollKommentare);
//            System.out.println(zaehlprotokollSummen);
//            System.out.println(zaehlprotokollSollKassenstaende);
//            System.out.println(zaehlprotokollDifferenzen);
//            System.out.println(zaehlprotokolle);
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
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
        try {
            Integer id = id(); // ID of new, yet to come, abrechnung
            // Summe über Stornos:
            ResultSet rs = doQueryStornos(id);
            while (rs.next()) {
                BigDecimal mwst_satz = rs.getBigDecimal(1);
                BigDecimal storno_sum = rs.getBigDecimal(2);
                incompleteAbrechnungsStornos.put(mwst_satz, storno_sum);
                mwstSet.add(mwst_satz);
            }
            rs.close();

            // Summe über Retouren:
            rs = doQueryRetouren(id);
            while (rs.next()) {
                BigDecimal mwst_satz = rs.getBigDecimal(1);
                BigDecimal retoure_sum = rs.getBigDecimal(2);
                incompleteAbrechnungsRetouren.put(mwst_satz, retoure_sum);
                mwstSet.add(mwst_satz);
            }
            rs.close();

            // Summe über Entnahmen:
            rs = doQueryEntnahmen(id);
            if (rs.next()) {
                incompleteAbrechnungsEntnahmen = rs.getBigDecimal(1);
            } else {
                incompleteAbrechnungsEntnahmen = new BigDecimal("0.00");
            }
            rs.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private HashMap<BigDecimal, BigDecimal> queryIncompleteAbrechnung_BarBruttoVATs() {
        HashMap<BigDecimal, BigDecimal> abrechnungBarBrutto = new HashMap<>();
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT mwst_satz, SUM(ges_preis) AS bar_brutto " +
                    "FROM verkauf_details INNER JOIN verkauf USING (rechnungs_nr) " +
                    "WHERE storniert = FALSE AND verkaufsdatum > " +
                    "IFNULL((SELECT MAX(zeitpunkt_real) FROM abrechnung_tag), '0001-01-01') AND ec_zahlung = FALSE " +
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
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
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

        // empty row as separator before storno
        data.add(new Vector<>());
        data.lastElement().add("");
        colors.add(new Vector<>());
        colors.lastElement().add(Color.BLACK);
        fontStyles.add(new Vector<>());
        fontStyles.lastElement().add("bold");
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

        // empty row as separator before zaehlprotokoll
        data.add(new Vector<>());
        data.lastElement().add("");
        colors.add(new Vector<>());
        colors.lastElement().add(Color.BLACK);
        fontStyles.add(new Vector<>());
        fontStyles.lastElement().add("bold");

        zpNumber = maxZaehlprotokollNumber();
        zpEinheiten = zaehlprotokollEinheiten();
        System.out.println(zpNumber);
        System.out.println(zpEinheiten);
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
            data.add(new Vector<>()); data.lastElement().add("Summe gezählter Kassenstand");
            colors.add(new Vector<>()); colors.lastElement().add(def_col);
            fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold");
            data.add(new Vector<>()); data.lastElement().add("Soll-Kassenstand");
            colors.add(new Vector<>()); colors.lastElement().add(def_col);
            fontStyles.add(new Vector<>()); fontStyles.lastElement().add("bold");
            data.add(new Vector<>()); data.lastElement().add("Differenz");
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

        data.get(rowIndex).add(""); // empty row as separator before storno
        colors.get(rowIndex).add(Color.BLACK);
        fontStyles.get(rowIndex).add("normal");
        rowIndex++;
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

        data.get(rowIndex).add(""); // empty row as separator before zaehlprotokoll
        colors.get(rowIndex).add(Color.BLACK);
        fontStyles.get(rowIndex).add("normal");
        rowIndex++;
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

        data.get(rowIndex).add(""); // empty row as separator before storno
        colors.get(rowIndex).add(Color.BLACK);
        fontStyles.get(rowIndex).add("normal");
        rowIndex++;
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

        data.get(rowIndex).add(""); // empty row as separator before zaehlprotokoll
        colors.get(rowIndex).add(Color.BLACK);
        fontStyles.get(rowIndex).add("normal");
        rowIndex++;
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


    private String queryEarliestVerkauf() {
        String date = "";
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MIN(verkaufsdatum) "+
                    "FROM verkauf WHERE storniert = FALSE AND verkaufsdatum > "+
                    "IFNULL((SELECT MAX(zeitpunkt_real) FROM abrechnung_tag),'0001-01-01')");
            rs.next(); date = rs.getString(1); rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return date;
    }

    private String queryLatestVerkauf() {
        String date = "";
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MAX(verkaufsdatum) "+
                    "FROM verkauf WHERE storniert = FALSE AND verkaufsdatum > "+
                    "IFNULL((SELECT MAX(zeitpunkt_real) FROM abrechnung_tag), '0001-01-01')");
            rs.next(); date = rs.getString(1); rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return date;
    }


    private void showSelectZeitpunktDialog(DateTime firstDate, DateTime lastDate, DateTime nowDate) {
        JDialog dialog = new JDialog(this.mainWindow, "Zeitpunkt manuell auswählen", true);
        SelectZeitpunktForAbrechnungDialog selZeitpunkt = 
            new SelectZeitpunktForAbrechnungDialog(this.conn, this.mainWindow,
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
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT COUNT(*) FROM "+abrechnungsName+" "+
                    "WHERE "+timeName+" = "+zeitpunktParsing
                    );
            pstmt.setString(1, zeitpunkt);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); int count = rs.getInt(1); rs.close();
            pstmt.close();
            if (count > 0){
                pstmt = this.conn.prepareStatement(
                        "DELETE FROM "+abrechnungsName+" "+
                        "WHERE "+timeName+" = "+zeitpunktParsing
                        );
                pstmt.setString(1, zeitpunkt);
                int result = pstmt.executeUpdate();
                pstmt.close();
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Alte Abrechnung konnte nicht aus Tabelle "+
                            "'"+abrechnungsName+"' gelöscht werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private Integer insertTagesAbrechnung() {
        /** create new abrechnung (and save in DB) from time of last abrechnung until now */
        Integer id = null;
        try {
            id = id();
            String firstDate = queryEarliestVerkauf();
            String lastDate = queryLatestVerkauf();
            String nowDate = now();
            String zeitpunkt = decideOnZeitpunkt(firstDate, lastDate, nowDate);
            System.out.println("Selected Zeitpunkt: "+zeitpunkt);
            if (zeitpunkt == null){
                System.out.println("insertTagesAbrechnung was cancelled!");
                return null; // don't do anything, user cancelled (or did not select date properly)
            }
            // get ID of current kassenstand (highest ID due to auto-increment)
            Integer kassenstand_id = mainWindow.retrieveKassenstandId();
            // get netto values grouped by mwst:
            HashMap<BigDecimal, Vector<BigDecimal>> abrechnungNettoBetrag = queryIncompleteAbrechnungTag_VATs();
            // get totals (bar brutto) grouped by mwst:
            HashMap<BigDecimal, BigDecimal> abrechnungBarBrutto = queryIncompleteAbrechnung_BarBruttoVATs();
            //System.out.println("mwst_satz  mwst_netto  mwst_betrag  bar_brutto");
            //System.out.println("----------------------------------------------");
            for ( Map.Entry< BigDecimal, Vector<BigDecimal> > entry : abrechnungNettoBetrag.entrySet() ){
                BigDecimal mwst_satz = entry.getKey();
                Vector<BigDecimal> values = entry.getValue();
                BigDecimal mwst_netto = values.get(1);
                BigDecimal mwst_betrag = values.get(2);
                BigDecimal bar_brutto = new BigDecimal("0.00");
                if ( abrechnungBarBrutto.containsKey(mwst_satz) ){
                    bar_brutto = abrechnungBarBrutto.get(mwst_satz);
                }
                //System.out.println("INSERT INTO abrechnung_tag: id: "+id+
                //        "  "+mwst_satz+"  "+mwst_netto+"  "+mwst_betrag+
                //        "   "+bar_brutto);
                PreparedStatement pstmt = this.conn.prepareStatement(
                        "INSERT INTO abrechnung_tag SET id = ?, "+
                                "zeitpunkt = ?, "+
                                "zeitpunkt_real = ?, "+
                                "mwst_satz = ?, "+
                                "mwst_netto = ?, "+
                                "mwst_betrag = ?, "+
                                "bar_brutto = ?, "+
                                "kassenstand_id = ?"
                );
                pstmtSetInteger(pstmt, 1, id);
                pstmt.setString(2, zeitpunkt);
                pstmt.setString(3, nowDate);
                pstmt.setBigDecimal(4, mwst_satz);
                pstmt.setBigDecimal(5, mwst_netto);
                pstmt.setBigDecimal(6, mwst_betrag);
                pstmt.setBigDecimal(7, bar_brutto);
                pstmtSetInteger(pstmt, 8, kassenstand_id);
                int result = pstmt.executeUpdate();
                pstmt.close();
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Tagesabrechnung konnte nicht gespeichert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    id = null;
                }
            }
            // NEED TO REDO Monats/Jahresabrechnung if needed (check if zeitpunkt lies in old month/year)!!!
            deleteAbrechnungIfNeedBe("abrechnung_monat", "monat", "DATE_FORMAT(?, '%Y-%m-01')", zeitpunkt);
            deleteAbrechnungIfNeedBe("abrechnung_jahr", "jahr", "YEAR(?)", zeitpunkt);
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Fehler: Tagesabrechnung konnte nicht gespeichert werden.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            id = null;
        }
        return id;
    }

    private Integer maxZaehlprotokollID() {
        Integer maxZaehlID = null;
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MAX(id) FROM zaehlprotokoll");
            rs.next();
            maxZaehlID = rs.getInt(1);
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return maxZaehlID;
    }

    private void insertZaehlprotokoll(Integer abrechnung_tag_id) {
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "INSERT INTO zaehlprotokoll SET abrechnung_tag_id = ?, "+
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
                pstmt = this.conn.prepareStatement(
                        "INSERT INTO zaehlprotokoll_details SET zaehlprotokoll_id = ?, "+
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
            if (result == 0) {
                JOptionPane.showMessageDialog(this,
                        "Fehler: Zählprotokoll-Details konnten nicht gespeichert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Fehler: Zählprotokoll konnte nicht gespeichert werden.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setZaehlprotokollInactive(Integer abrechnung_tag_id) {
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "UPDATE zaehlprotokoll SET aktiv = FALSE "+
                            "WHERE abrechnung_tag_id = ? AND aktiv = TRUE"
            );
            pstmtSetInteger(pstmt, 1, abrechnung_tag_id);
            int result = pstmt.executeUpdate();
            pstmt.close();
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Altes Zählprotokoll konnte nicht inaktiv gesetzt werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Fehler: Altes Zählprotokoll konnte nicht inaktiv gesetzt werden.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showZaehlprotokollDialog() {
        JDialog dialog = new JDialog(this.mainWindow, "Erfassung des Kassenbestands", true);
        ZaehlprotokollDialog zd = new ZaehlprotokollDialog(this.conn, this.mainWindow, this, dialog);
        dialog.getContentPane().add(zd, BorderLayout.CENTER);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void showZaehlprotokollEditDialog(int editIndex) {
        JDialog dialog = new JDialog(this.mainWindow, "Bearbeitung des Kassenbestands", true);
        ZaehlprotokollDialog zd = new ZaehlprotokollDialog(this.conn, this.mainWindow, this, dialog);
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
        KassenstandZuruecksetzenDialog kzd = new KassenstandZuruecksetzenDialog(this.conn, this.mainWindow, dialog, this.zaehlprotokoll, this);
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

        // Set Stornos, Retouren, Entnahmen:
        for (BigDecimal mwst : mwstSet) {
            sheet.setValueAt("Storno ("+bc.vatFormatter(mwst) + " MwSt.)", 0, rowIndex);
            if (abrechnungsStornos != null && abrechnungsStornos.get(exportIndex).containsKey(mwst)){
                BigDecimal bd = abrechnungsStornos.get(exportIndex).get(mwst);
                sheet.setValueAt(bd, 1, rowIndex);
            } else {
                sheet.setValueAt(0., 1, rowIndex);
            }
            rowIndex++;
        }
        for (BigDecimal mwst : mwstSet) {
            sheet.setValueAt("Retouren ("+bc.vatFormatter(mwst) + " MwSt.)", 0, rowIndex);
            if (abrechnungsRetouren != null && abrechnungsRetouren.get(exportIndex).containsKey(mwst)){
                BigDecimal bd = abrechnungsRetouren.get(exportIndex).get(mwst);
                sheet.setValueAt(bd, 1, rowIndex);
            } else {
                sheet.setValueAt(0., 1, rowIndex);
            }
            rowIndex++;
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
                System.out.println(zpDate);
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
                            "Bitte im folgenden Fenster den Ordner\n"+
                            "'Dokumente/Kasse/Tagesabrechnungen/Aktuelles Jahr/Aktueller Monat'\n"+
                            "als Speicherort auswählen.\n\n"+
                            "Danach öffnet sich die Abrechnung automatisch.\n"+
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
