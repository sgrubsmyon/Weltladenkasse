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
        super(conn, mw, preisschilder);
        this.preisschilder = preisschilder;
    }

    protected void resetOther() {
        preisschilder.preisField.setText("");
        preisschilder.preisField.setEditable(false);
    }

    protected void setPriceField() {
        boolean variablerPreis = getVariablePriceBool(selectedArticleID);
        if ( ! variablerPreis ){
            String artikelPreis = getSalePrice(selectedArticleID);
            preisschilder.preisField.getDocument().removeDocumentListener(this);
            preisschilder.preisField.setText("");
            preisschilder.preisField.setText( bc.decimalMark(artikelPreis) );
            preisschilder.preisField.getDocument().addDocumentListener(this);
        }
        else {
            preisschilder.preisField.setEditable(true);
        }
    }

    protected void articleSelectFinishedFocus() {
    }

    protected void setButtonsEnabled() {
        preisschilder.setButtonsEnabled();
    }
}
