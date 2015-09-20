package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for String
import java.math.BigDecimal;
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

    @Override
    protected void setPriceField() {
        Artikel a = getArticle(selectedArticleID);
        String vkPreis = a.getVKP();
        String empfVkPreis = a.getEmpfVKP();
        String kurzname = a.getKurzname();
        BigDecimal menge = a.getMenge();
        preisschilder.vkPreisField.setText(bc.priceFormatter(vkPreis));
        preisschilder.empfVkPreisField.setText(bc.priceFormatter(empfVkPreis));
        preisschilder.kurznameField.setText(kurzname);
        preisschilder.mengeField.setText(bc.unifyDecimal(menge));
        preisschilder.vkPreisField.setEditable(true);
        preisschilder.empfVkPreisField.setEditable(true);
        preisschilder.kurznameField.setEditable(true);
        preisschilder.mengeField.setEditable(true);
        preisschilder.vkPreisField.requestFocus();
        preisschilder.vkPreisField.selectAll();
        
    }

    @Override
    protected void resetPriceField() {
        preisschilder.vkPreisField.setText("");
        preisschilder.empfVkPreisField.setText("");
        preisschilder.kurznameField.setText("");
        preisschilder.mengeField.setText("");
        preisschilder.vkPreisField.setEditable(false);
        preisschilder.empfVkPreisField.setEditable(false);
        preisschilder.kurznameField.setEditable(false);
        preisschilder.mengeField.setEditable(false);
    }

    protected void articleSelectFinishedFocus() {
    }

    protected void setButtonsEnabled() {
        preisschilder.setButtonsEnabled();
    }
}
