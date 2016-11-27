package org.weltladen_bonn.pos.kasse;

import org.weltladen_bonn.pos.BaseClass;
import org.weltladen_bonn.pos.DialogWindow;
import org.weltladen_bonn.pos.MainWindowGrundlage;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by mvoge on 26.11.16.
 */
class KassenstandZuruecksetzenDialog extends DialogWindow
        implements DocumentListener {
    private LinkedHashMap<BigDecimal, Integer> zaehlprotokoll;

    public KassenstandZuruecksetzenDialog(Connection conn, MainWindowGrundlage mw, JDialog dia,
                                          LinkedHashMap<BigDecimal, Integer> zaehlprotokoll) {
        super(conn, mw, dia);
        this.zaehlprotokoll = zaehlprotokoll;
        showAll();
    }

    @Override
    protected void showHeader() {
        headerPanel = new JPanel();

        // borders:
        int top = 5, left = 5, bottom = 5, right = 5;
        headerPanel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));

        BigDecimal gezaehlterKassenstand = new BigDecimal("0");
        for ( Map.Entry<BigDecimal, Integer> entry : zaehlprotokoll.entrySet() ) {
            BigDecimal wert = entry.getKey();
            BigDecimal anzahl = new BigDecimal(entry.getValue());
            gezaehlterKassenstand = gezaehlterKassenstand.add(wert.multiply(anzahl));
        }

        BigDecimal sollKassenstand = bc.sollMuenzKassenstand.add(bc.sollScheinKassenstand);
        BigDecimal delta = gezaehlterKassenstand.subtract(sollKassenstand);

        // CONTINUE HERE, CALCULATE schein AND muenz deltas
        BigDecimal muenzStand = new BigDecimal("0");
        for (BigDecimal wert : bc.muenz_werte) {
            BigDecimal anzahl = new BigDecimal(zaehlprotokoll.get(wert));
            muenzStand = muenzStand.add(wert.multiply(anzahl));
        }
        BigDecimal muenzDelta = muenzStand.subtract(bc.sollMuenzKassenstand);

        BigDecimal scheinStand = new BigDecimal("0");
        for (BigDecimal wert : bc.schein_werte) {
            BigDecimal anzahl = new BigDecimal(zaehlprotokoll.get(wert));
            scheinStand = scheinStand.add(wert.multiply(anzahl));
        }
        BigDecimal scheinDelta = scheinStand.subtract(bc.sollScheinKassenstand);

        String gesamtDeltaText = delta.signum() >= 0 ?
                bc.priceFormatter(delta)+" "+bc.currencySymbol+" aus der Kasse entnommen werden, " :
                bc.priceFormatter(delta.multiply(bc.minusOne))+" "+bc.currencySymbol+" in die Kasse gegeben werden, ";
        String muenzDeltaText = muenzDelta.signum() >= 0 ?
                bc.priceFormatter(muenzDelta)+" "+bc.currencySymbol+" in Münzen aus der Kasse entnommen werden" :
                bc.priceFormatter(muenzDelta.multiply(bc.minusOne))+" "+bc.currencySymbol+" in Münzen in die Kasse gegeben werden";
        String scheinDeltaText = scheinDelta.signum() >= 0 ?
                bc.priceFormatter(scheinDelta)+" "+bc.currencySymbol+" in Scheinen aus der Kasse entnommen werden" :
                bc.priceFormatter(scheinDelta.multiply(bc.minusOne))+" "+bc.currencySymbol+" in Scheinen in die Kasse gegeben werden";

        JTextArea erklaerText = new JTextArea(20, 40);
        erklaerText.append("Der Kassenstand soll, soweit möglich, auf "+
                bc.priceFormatter(sollKassenstand)+" "+bc.currencySymbol+" "+
                "gebracht werden. Idealerweise "+bc.priceFormatter(bc.sollScheinKassenstand)+" "+bc.currencySymbol+" "+
                "in Scheinen und "+bc.priceFormatter(bc.sollMuenzKassenstand)+" "+bc.currencySymbol+" in Münzen.\n\n" +
                "Bei einem gezählten Kassenstand von "+
                bc.priceFormatter(gezaehlterKassenstand)+" "+bc.currencySymbol+" müssen dafür "+
                gesamtDeltaText +
                "und zwar müssen:\n\n"+
                "\t● "+scheinDeltaText+",\n"+
                "\t● "+muenzDeltaText+".\n\n"+
                "Falls Wechselgeld gebraucht wird, kann es in der Wechselgeldkasse gefunden werden (siehe Wiki).");
        erklaerText = makeLabelStyle(erklaerText);
        erklaerText.setFont(BaseClass.mediumFont);
        //erklaerText.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        headerPanel.add(erklaerText);

        allPanel.add(headerPanel, BorderLayout.NORTH);
    }

    @Override
    protected void showMiddle() {
    }

    @Override
    protected void showFooter() {
        footerPanel = new JPanel();
        allPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    @Override
    protected int submit() {
        return 0;
    }

    @Override
    protected boolean willDataBeLost() {
        return false;
    }

    @Override
    public void insertUpdate(DocumentEvent documentEvent) {

    }

    @Override
    public void removeUpdate(DocumentEvent documentEvent) {

    }

    @Override
    public void changedUpdate(DocumentEvent documentEvent) {
        // Plain text components do not fire these events
    }
}