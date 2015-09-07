package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for String

// MySQL Connector/J stuff:
import java.sql.*; // Connection, Statement, ResultSet

import WeltladenDB.*;

public class ArticleSelectPanelPreisschilder extends ArticleSelectPanelGrundlage {
    PreisschilderFormular preisschilder;

    public ArticleSelectPanelPreisschilder(Connection conn, MainWindowGrundlage mw,
            PreisschilderFormular preisschilder) {
        super(conn, mw);
        this.preisschilder = preisschilder;
    }

    protected void resetOther() {
        preisschilder.preisField.setText("");
        preisschilder.preisField.setEditable(false);
    }

    protected void setPriceField() {
        boolean variablerPreis = getVariablePriceBool(selectedArtikelID);
        if ( ! variablerPreis ){
            String artikelPreis = getSalePrice(selectedArtikelID);
            preisField.getDocument().removeDocumentListener(this);
            preisField.setText("");
            preisField.setText( bc.decimalMark(artikelPreis) );
            preisField.getDocument().addDocumentListener(this);
        }
        else {
            preisField.setEditable(true);
        }
    }

    protected void articleSelectFinishedFocus() {
    }

    protected void setButtonsEnabled() {
        preisschilder.setButtonsEnabled();
    }
}
