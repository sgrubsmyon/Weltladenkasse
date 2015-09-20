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
        super(conn, mw, bestellen);
        this.bestellen = bestellen;
        this.tabbedPane = tabbedPane;
    }

    protected void resetOther() {
        bestellen.preisField.setText("");
        bestellen.preisField.setEditable(false);
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

    @Override
    protected void setPriceField() {
        setAnzahlSpinner();

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

                Artikel article = getArticle(selectedArticleID);
                Vector<Artikel> selectedArticles = new Vector<Artikel>();
                selectedArticles.add(article);

                showEditDialog(selectedArticles);

                artikelPreis = getRecSalePrice(selectedArticleID);
                if (artikelPreis == null || artikelPreis.equals("")){
                    artikelPreis = getSalePrice(selectedArticleID);
                }
                if (artikelPreis == null)
                    artikelPreis = "";
                System.out.println("artikelPreis: "+artikelPreis);
            }
            bestellen.preisField.getDocument().removeDocumentListener(this);
            bestellen.preisField.setText( bc.decimalMark(artikelPreis) );
            bestellen.preisField.getDocument().addDocumentListener(this);
        }
        else {
            bestellen.preisField.setEditable(true);
        }
        int setgroesse = getSetSize(selectedArticleID);
        if (setgroesse > 1){
            bestellen.setLabel.setText("pro Set ("+setgroesse+"-er Set)");
        } else {
            bestellen.setLabel.setText("");
        }
    }

    @Override
    protected void resetPriceField() {
    }

    private void showEditDialog(Vector<Artikel> selectedArticles) {
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
