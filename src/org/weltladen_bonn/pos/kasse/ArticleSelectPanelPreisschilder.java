package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for String
import java.awt.Color;
import java.math.BigDecimal;
// MySQL Connector/J stuff:
import java.sql.*; // Connection, Statement, ResultSet

import org.weltladen_bonn.pos.*;

public class ArticleSelectPanelPreisschilder extends ArticleSelectPanelGrundlage {
    PreisschilderFormular preisschilder;

    public ArticleSelectPanelPreisschilder(Connection conn, MainWindowGrundlage mw,
            PreisschilderFormular preisschilder) {
        super(conn, mw, preisschilder, null);
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
        String einheit = a.getEinheit();
        String herkunft = a.getHerkunft();
        Boolean sortiment = a.getSortiment();
        preisschilder.vkPreisField.setText(bc.priceFormatter(vkPreis));
        preisschilder.empfVkPreisField.setText(bc.priceFormatter(empfVkPreis));
        preisschilder.kurznameField.setText(kurzname);
        preisschilder.mengeField.setText(bc.unifyDecimal(menge));
        preisschilder.einheitField.setText(einheit);
        preisschilder.herkunftField.setText(herkunft);
        preisschilder.sortimentBox.setSelected(sortiment);
        
        preisschilder.vkPreisField.setEditable(true);
        preisschilder.empfVkPreisField.setEditable(true);
        preisschilder.kurznameField.setEditable(true);
        preisschilder.mengeField.setEditable(true);
        preisschilder.einheitField.setEditable(true);
        preisschilder.herkunftField.setEditable(true);
        preisschilder.sortimentBox.setEnabled(true);
        
        preisschilder.vkPreisField.requestFocus();
        preisschilder.vkPreisField.selectAll();
        
    }

    @Override
    protected void resetPriceField() {
        preisschilder.vkPreisField.setText("");
        preisschilder.empfVkPreisField.setText("");
        preisschilder.kurznameField.setText("");
        preisschilder.mengeField.setText("");
        preisschilder.einheitField.setText("");
        preisschilder.herkunftField.setText("");
        preisschilder.sortimentBox.setSelected(false);
        
        preisschilder.vkPreisField.setEditable(false);
        preisschilder.empfVkPreisField.setEditable(false);
        preisschilder.kurznameField.setEditable(false);
        preisschilder.mengeField.setEditable(false);
        preisschilder.einheitField.setEditable(false);
        preisschilder.herkunftField.setEditable(false);
        preisschilder.sortimentBox.setEnabled(false);
    }

    protected void articleSelectFinishedFocus() {
    }

    protected void setButtonsEnabled() {
        preisschilder.setButtonsEnabled();
    }
}
