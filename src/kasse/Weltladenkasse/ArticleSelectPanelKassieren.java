package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for String

// MySQL Connector/J stuff:
import java.sql.*; // Connection, Statement, ResultSet

import WeltladenDB.*;

public class ArticleSelectPanelKassieren extends ArticleSelectPanelGrundlage {
    Kassieren kassieren;

    public ArticleSelectPanelKassieren(Connection conn, MainWindowGrundlage mw,
            Kassieren kassieren) {
        super(conn, mw);
        this.kassieren = kassieren;
    }

    protected void resetOther() {
        kassieren.preisField.setText("");
        kassieren.preisField.setEditable(false);
    }

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
            kassieren.preisField.getDocument().removeDocumentListener(this);
            kassieren.preisField.setText( bc.decimalMark(artikelPreis) );
            kassieren.preisField.getDocument().addDocumentListener(this);
        }
        else {
            kassieren.preisField.setEditable(true);
        }
    }

    protected void articleSelectFinishedFocus() {
        kassieren.anzahlField.requestFocus();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                kassieren.anzahlField.selectAll();
            }
        });
    }

    protected void setButtonsEnabled() {
        kassieren.setButtonsEnabled();
    }
}
