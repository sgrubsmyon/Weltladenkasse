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
    BigDecimal sollKassenstand = new BigDecimal("150.00");
    LinkedHashMap<BigDecimal, Integer> zaehlprotokoll;

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
        BigDecimal delta = gezaehlterKassenstand.subtract(sollKassenstand);
        // CONTINUE HERE, CALCULATE schein AND muenz deltas

        JTextArea erklaerText = new JTextArea(20, 40);
        erklaerText.append("Der Kassenstand soll, soweit möglich, auf "+
                bc.priceFormatter(sollKassenstand)+" "+bc.currencySymbol+" "+
                "gebracht werden.\n\n" +
                "Bei einem gezählten Kassenstand von "+
                bc.priceFormatter(gezaehlterKassenstand)+" "+bc.currencySymbol+" müssen dafür "+
                bc.priceFormatter(delta)+" "+bc.currencySymbol+" aus der Kasse entnommen werden.\n\n"+
                "Vorschlag:\n");
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