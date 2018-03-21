package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for String

// MySQL Connector/J stuff:
import java.sql.*; // Connection, Statement, ResultSet

// GUI stuff:
import javax.swing.SwingUtilities;

import org.weltladen_bonn.pos.*;

public class ArticleSelectPanelRabattDialog extends ArticleSelectPanelGrundlage {
    private OptionTabbedPane tabbedPane;

    public ArticleSelectPanelRabattDialog(Connection conn, MainWindowGrundlage mw,
            RabattDialog rd, OptionTabbedPane tabbedPane) {
        super(conn, mw, rd, null);
        this.tabbedPane = tabbedPane;
        String filterStr = " AND variabler_preis = FALSE AND toplevel_id IS NOT NULL ";
        barcodeBox.setFilterStr(filterStr);
        nummerBox.setFilterStr(filterStr);
        artikelBox.setFilterStr(filterStr);
    }

    @Override
    protected void resetOther() {
    }

    @Override
    protected void setPriceField() {
        boolean variablerPreis = getVariablePriceBool(selectedArticleID);
        if ( ! variablerPreis ){
            String artikelPreis = getSalePrice(selectedArticleID);
            if (artikelPreis == null || artikelPreis.equals("")){
                artikelPreis = handleMissingSalePrice("Bitte Verkaufspreis eingeben",
                        getShortName(selectedArticleID),
                        getArticleNumber(selectedArticleID)[0],
                        getArticleName(selectedArticleID)[1],
                        getBarcode(selectedArticleID));
                if (artikelPreis != null && !artikelPreis.equals("")){
                    Artikel origArticle = getArticle(selectedArticleID);
                    Artikel newArticle = getArticle(selectedArticleID);
                    newArticle.setVKP(artikelPreis);
                    newArticle.setEmpfVKP(artikelPreis);
                    updateArticle(origArticle, newArticle);

                    updateSelectedArticleID();
                    Artikelliste artikelListe = tabbedPane.getArtikelliste();
                    if (artikelListe != null){
                        artikelListe.updateAll();
                    }

                    artikelPreis = getSalePrice(selectedArticleID);
                    if (artikelPreis == null)
                        artikelPreis = "";
                }
            }
        }
        else {
        }
    }

    @Override
    protected void resetPriceField() {
    }

    protected void articleSelectFinishedFocus() {
    }

    protected void setButtonsEnabled() {
    }
}
