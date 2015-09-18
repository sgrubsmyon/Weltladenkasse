package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for String

// MySQL Connector/J stuff:
import java.sql.*; // Connection, Statement, ResultSet

import org.weltladen_bonn.pos.*;

public class ArticleSelectPanelPreisschilder extends ArticleSelectPanelGrundlage {
    PreisschilderFormular preisschilder;

    public ArticleSelectPanelPreisschilder(Connection conn, MainWindowGrundlage mw,
            PreisschilderFormular preisschilder) {
        super(conn, mw, preisschilder);
        this.preisschilder = preisschilder;
    }

    protected void resetOther() {
        preisschilder.vkPreisField.setText("");
        preisschilder.vkPreisField.setEditable(false);
    }

    protected void setPriceField() {
        boolean variablerPreis = getVariablePriceBool(selectedArticleID);
        if ( ! variablerPreis ){
            String artikelPreis = getSalePrice(selectedArticleID);
            preisschilder.vkPreisField.getDocument().removeDocumentListener(this);
            preisschilder.vkPreisField.setText("");
            preisschilder.vkPreisField.setText( bc.decimalMark(artikelPreis) );
            preisschilder.vkPreisField.getDocument().addDocumentListener(this);
        }
        else {
            preisschilder.vkPreisField.setEditable(true);
        }
    }

    protected void articleSelectFinishedFocus() {
    }

    protected void setButtonsEnabled() {
        preisschilder.setButtonsEnabled();
    }
}
