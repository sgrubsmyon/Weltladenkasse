package org.weltladen_bonn.pos.besteller;

// Basic Java stuff:
import java.util.*; // for String

// MySQL Connector/J stuff:
import java.sql.*; // Connection, Statement, ResultSet

// GUI stuff:
import java.awt.*; // BorderLayout, FlowLayout, Dimension

import javax.swing.*; // JDialog, JOptionPane, JFrame, JPanel, JTable, JButton, ...

import org.weltladen_bonn.pos.*;

public class ArticleSelectPanelBestellen extends ArticleSelectPanelGrundlage {
    private Bestellen bestellen;
    private TabbedPane tabbedPane;

    public ArticleSelectPanelBestellen(Connection conn, MainWindowGrundlage mw,
            Bestellen bestellen, TabbedPane tabbedPane) {
        super(conn, mw, bestellen, " AND (toplevel_id IS NOT NULL OR sub_id = 2 OR sub_id = 3 OR sub_id = 4) ");
          // filterStr: exceptions for Gutschein (sub_id = 2), regular Pfand for inventories (sub_id = 3) and Pfand optional (sub_id = 4)
        this.bestellen = bestellen;
        this.tabbedPane = tabbedPane;
    }

    protected void resetOther() {
        bestellen.preisField.setText("");
        bestellen.preisField.setEditable(false);
        bestellen.changeButton.setEnabled(false);
    }

    private void setAnzahlSpinner() {
        String vpe = getVPE(selectedArticleID);
        Integer vpeInt = vpe.length() > 0 ? Integer.parseInt(vpe) : 0;
        if (vpeInt > 0){
            bestellen.anzahlSpinner.setValue(vpeInt);
        } else {
            bestellen.anzahlSpinner.setValue(1);
        }
        bestellen.vpeField.setText(vpe);
        bestellen.updateAnzahlColor(vpeInt);
    }

    private void setKuller(Artikel a) {
        int index = bc.beliebtWerte.indexOf(a.getBeliebt());
        bestellen.beliebtKuller.setText( bc.beliebtKuerzel.get(index) );
        bestellen.beliebtKuller.setForeground( bc.beliebtFarben.get(index) );
        String description = bc.beliebtBeschreibungen.get(index);
        Font font = bestellen.beliebtText.getFont();
        // vertical centering für Arme:
        FontMetrics metrics = bestellen.beliebtText.getFontMetrics(font);
        int textwidth = metrics.stringWidth(description);
        long boxwidth = Math.round( bestellen.beliebtText.getPreferredSize().getWidth() );
        if (textwidth <= boxwidth) {
            description = "\n"+description+"\n";
        } else if (textwidth <= 2.*boxwidth) {
            description = "\n"+description;
        }
        bestellen.beliebtText.setText(description);
    }

    @Override
    protected void setPriceField() {
        Artikel article = getArticle(selectedArticleID);

        setAnzahlSpinner();
        setKuller(article);

        boolean variablerPreis = getVariablePriceBool(selectedArticleID);
        if ( ! variablerPreis ){
            String artikelPreis = getRecSalePrice(selectedArticleID);
            if (artikelPreis == null || artikelPreis.equals("")){
                artikelPreis = getSalePrice(selectedArticleID);
            }
            if (artikelPreis == null || artikelPreis.equals("")){
                JOptionPane.showMessageDialog(this,
                        "Für diesen Artikel muss erst der Preis festgelegt werden!",
                        "Info", JOptionPane.INFORMATION_MESSAGE);

                Vector<Artikel> selectedArticles = new Vector<Artikel>();
                selectedArticles.add(article);

                showEditDialog(selectedArticles);

                boolean varPreis2 = getVariablePriceBool(selectedArticleID);
                if ( !varPreis2 ){
                    artikelPreis = getRecSalePrice(selectedArticleID);
                    if (artikelPreis == null || artikelPreis.equals("")){
                        artikelPreis = getSalePrice(selectedArticleID);
                    }
                    if (artikelPreis == null)
                        artikelPreis = "";
                } else {
                    artikelPreis = "";
                    bestellen.preisField.setEditable(true);
                }
            }
            bestellen.preisField.getDocument().removeDocumentListener(this);
            bestellen.preisField.setText( bc.decimalMark(artikelPreis) );
            bestellen.preisField.getDocument().addDocumentListener(this);
        } else {
            bestellen.preisField.setEditable(true);
        }
        int setgroesse = getSetSize(selectedArticleID);
        if (setgroesse > 1){
            bestellen.setLabel.setText("pro Set ("+setgroesse+"-er Set)");
        } else {
            bestellen.setLabel.setText("");
        }
        bestellen.changeButton.setEnabled(true);
    }

    @Override
    protected void resetPriceField() {
    }

    protected void showEditDialog(Vector<Artikel> selectedArticles) {
        JDialog editDialog = new JDialog(mainWindow, "Artikel bearbeiten", true);
        ArtikelBearbeiten bearb = new ArtikelBearbeiten(conn,
                mainWindow, tabbedPane.getArtikelliste(), editDialog,
                selectedArticles);
        bearb.getArtikelFormular().sortimentBox.setSelected(true);
        editDialog.getContentPane().add(bearb, BorderLayout.CENTER);
        editDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        WindowAdapterDialog wad = new WindowAdapterDialog(bearb, editDialog,
                "Achtung: Änderungen gehen verloren (noch nicht abgeschickt).\nWirklich schließen?");
        editDialog.addWindowListener(wad);
        editDialog.pack();
        editDialog.setVisible(true);

        updateSelectedArticleID();
    }

    protected void articleSelectFinishedFocus() {
        bestellen.anzahlField.requestFocus();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                bestellen.anzahlField.selectAll();
            }
        });
    }

    protected void setButtonsEnabled() {
        bestellen.setButtonsEnabled();
    }

}
