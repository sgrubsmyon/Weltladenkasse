package org.weltladen_bonn.pos.kasse;

import org.weltladen_bonn.pos.BaseClass;
import org.weltladen_bonn.pos.DialogWindow;
import org.weltladen_bonn.pos.MainWindowGrundlage;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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
    private AbrechnungenTag abrechnungenTag;
    private BigDecimal sollKassenstand;
    private JTextField newKassenstandField;
    private JButton submitButton;

    public KassenstandZuruecksetzenDialog(Connection conn, MainWindowGrundlage mw, JDialog dia,
                                          LinkedHashMap<BigDecimal, Integer> zaehlprotokoll, AbrechnungenTag at) {
        super(conn, mw, dia);
        this.zaehlprotokoll = zaehlprotokoll;
        this.abrechnungenTag = at;
        showAll();
    }

    @Override
    protected void showHeader() {
        headerPanel = new JPanel();

        // borders:
//        int top = 5, left = 5, bottom = 5, right = 5;
//        headerPanel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));

        BigDecimal gezaehlterKassenstand = new BigDecimal("0");
        for ( Map.Entry<BigDecimal, Integer> entry : zaehlprotokoll.entrySet() ) {
            BigDecimal wert = entry.getKey();
            BigDecimal anzahl = new BigDecimal(entry.getValue());
            gezaehlterKassenstand = gezaehlterKassenstand.add(wert.multiply(anzahl));
        }

        sollKassenstand = bc.sollMuenzKassenstand.add(bc.sollScheinKassenstand);
        BigDecimal delta = gezaehlterKassenstand.subtract(sollKassenstand);

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

	BigDecimal kleinsterSchein = bc.schein_werte.get(0);
	Boolean muenzMangel = false;
	BigDecimal muenzTausch = new BigDecimal("0");
	if (muenzDelta.signum() < 0) {
		muenzMangel = true;
		muenzTausch = muenzDelta.multiply(bc.minusOne).add(muenzStand.remainder(kleinsterSchein));
		BigDecimal delta_remainder = delta.remainder(kleinsterSchein);
		BigDecimal delta_glatt = delta.subtract(delta_remainder);
		muenzDelta = delta_remainder;
		scheinDelta = delta_glatt;
	}
	Boolean scheinMangel = false;
	BigDecimal scheinTausch = new BigDecimal("0");
	if (scheinDelta.signum() < 0) {
		scheinMangel = true;
		scheinTausch = scheinDelta.multiply(bc.minusOne);
		muenzDelta = delta;
		scheinDelta = new BigDecimal("0");
	}

        JTextPane erklaerText = new JTextPane();
        erklaerText.setEditable(false);
        erklaerText.setMinimumSize(new Dimension(400,400));
        erklaerText.setPreferredSize(new Dimension(800,500));
        erklaerText.setContentType("text/html");
	String t = "<html>\n" +
                "  <head>\n" +
                "    <style type='text/css'>\n" +
                "      body { font-family: sans-serif; font-weight: bold; font-size: 13px; }\n" +
                "      .red { color: red; }\n" +
                "      .green { color: green; }\n" +
                "      .blue { color: blue; }\n" +
                "    </style>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "<p>Der Kassenstand soll auf <span class='blue'>"+
                bc.priceFormatter(sollKassenstand)+" "+bc.currencySymbol+"</span> "+
                "gebracht werden. Idealerweise <span class='blue'>"+
                bc.priceFormatter(bc.sollScheinKassenstand)+" "+bc.currencySymbol+"</span> "+
                "in Scheinen (fünf 10 €- und zehn 5 €-Scheine) und <span class='blue'>"+
                bc.priceFormatter(bc.sollMuenzKassenstand)+" "+bc.currencySymbol+"</span> in Münzen.</p>\n" +
                "<p>Gezählter Kassenstand: <span class='green'>"+
                bc.priceFormatter(gezaehlterKassenstand)+" "+bc.currencySymbol+"</span> (<span class='green'>"+
                bc.priceFormatter(scheinStand)+" "+bc.currencySymbol+"</span> in Scheinen, <span class='green'>"+
                bc.priceFormatter(muenzStand)+" "+bc.currencySymbol+"</span> in Münzen).</p>\n"+
		"<p>Aus der Kasse müssen entnommen werden: <span class='red'>"+
		bc.priceFormatter(delta)+" "+bc.currencySymbol+"</span>.</p>\n"+
                "Vorschlag:</p>\n"+
                "<ul>\n"+
                "  <li><span class='red'>"+bc.priceFormatter(scheinDelta)+" "+bc.currencySymbol+"</span> Scheingeld ";
	if (!muenzMangel & !scheinMangel) {
		t = t + "(am besten so, dass fünf 10 €-Scheine und zehn 5 €-Scheine verbleiben) ";
	}
	t = t + "und</li>\n"+
                "  <li><span class='red'>"+bc.priceFormatter(muenzDelta)+" "+bc.currencySymbol+"</span> Münzgeld.</li>\n"+
                "</ul>\n";
	if (muenzMangel & !scheinMangel) {
		t = t + "<p><b>NACH</b> der Geldentnahme zu beachten:</p>\n"+
			"<p>Weniger als <span class='blue'>"+bc.priceFormatter(bc.sollMuenzKassenstand)+" "+bc.currencySymbol+"</span> Münzgeld in der Kasse! "+
			"Bitte versuche, Scheine in Münzen zu tauschen.</p>\n"+
			"<p>Vorschlag: Nimm <span class='red'>"+bc.priceFormatter(muenzTausch)+" "+bc.currencySymbol+"</span> Scheingeld aus der Kasse und tausche "+
			"es mit Münzgeld aus der Wechselgeldkasse "+
			"(am besten so, dass fünf 10 €-Scheine und zehn 5 €-Scheine in der Kasse verbleiben).</p>\n"+
			"<p>Die Wechselgeldkasse befindet sich hinten im Lager (siehe Wiki).</p>\n"+
			"<p><b>WICHTIG:</b> Der Betrag in der Kasse darf sich durchs Tauschen nicht verändern und soll "+
			"immer noch <span class='blue'>"+bc.priceFormatter(sollKassenstand)+" "+bc.currencySymbol+"</span> betragen!</p>\n";
	} else if (scheinMangel & !muenzMangel) {
		t = t + "<p><b>NACH</b> der Geldentnahme zu beachten:</p>\n"+
			"<p>Weniger als <span class='blue'>"+bc.priceFormatter(bc.sollScheinKassenstand)+" "+bc.currencySymbol+"</span> Scheingeld in der Kasse! "+
			"Bitte versuche, Münzen in Scheine zu tauschen.</p>\n"+
			"<p>Vorschlag: Nimm <span class='red'>"+bc.priceFormatter(scheinTausch)+" "+bc.currencySymbol+"</span> Münzgeld aus der Kasse und tausche "+
			"es mit Scheingeld aus der Wechselgeldkasse "+
			"(am besten so, dass sich danach fünf 10 €-Scheine und zehn 5 €-Scheine in der Kasse befinden).</p>\n"+
			"<p>Die Wechselgeldkasse befindet sich hinten im Lager (siehe Wiki).</p>\n"+
			"<p><b>WICHTIG:</b> Der Betrag in der Kasse darf sich durchs Tauschen nicht verändern und soll "+
			"immer noch <span class='blue'>"+bc.priceFormatter(sollKassenstand)+" "+bc.currencySymbol+"</span> betragen.</p>\n";
	}
	t = t + "</body>\n"+
		"</html>";
        erklaerText.setText(t);
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
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));

        JPanel gridPanel = new JPanel(new GridBagLayout());
//        gridPanel.setBorder(BorderFactory.createTitledBorder("Summe" ));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 5;
        c.insets = new Insets(3, 10, 3, 10);

        JTextArea erklaerText = new JTextArea(5, 30);
        erklaerText.append("Bitte den neuen Kassenstand nach der Geldentnahme "+
                "rechts eingeben.\n"+
                "(Also im Normalfall "+bc.priceFormatter(sollKassenstand)+" "+bc.currencySymbol+")");
        erklaerText = makeLabelStyle(erklaerText);
        erklaerText.setFont(BaseClass.mediumFont);
        int top = 5, left = 5, bottom = 5, right = 5;
        erklaerText.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));

        newKassenstandField = new JTextField(bc.priceFormatter("150"));
        newKassenstandField.setColumns(10);
        newKassenstandField.getDocument().addDocumentListener(this);
        ((AbstractDocument) newKassenstandField.getDocument()).setDocumentFilter(bc.geldFilter);
        newKassenstandField.setFont(BaseClass.mediumFont);
        newKassenstandField.setHorizontalAlignment(JTextField.RIGHT);

        c.gridy = 0;
        c.gridx = 0;
        gridPanel.add(erklaerText, c);

        c.gridy = 0;
        c.gridx = 1;
        gridPanel.add(newKassenstandField, c);

        c.gridy = 0;
        c.gridx = 2;
        gridPanel.add(new BaseClass.BigLabel(bc.currencySymbol), c);

        footerPanel.add(gridPanel);

        JPanel buttonPanel = new JPanel();
        submitButton = new JButton("Kassenstand setzen");
        submitButton.setMnemonic(KeyEvent.VK_S);
        submitButton.addActionListener(this);
        buttonPanel.add(submitButton);
        footerPanel.add(buttonPanel);

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
        if (newKassenstandField.getDocument().getLength() == 0) {
            submitButton.setEnabled(false);
        } else {
            submitButton.setEnabled(true);
        }
    }

    @Override
    public void removeUpdate(DocumentEvent documentEvent) {
        insertUpdate(documentEvent);
    }

    @Override
    public void changedUpdate(DocumentEvent documentEvent) {
        // Plain text components do not fire these events
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == submitButton){
            int result = insertIntoKassenstand(
                    new BigDecimal(bc.priceFormatterIntern(newKassenstandField.getText())),
                    false,
                    "Tagesabschluss"
            );
            if (result == 0) {
                JOptionPane.showMessageDialog(this,
                        "Fehler: Kassenstand konnte nicht geändert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            } else {
                this.abrechnungenTag.setKassenstandWasChanged(true);
            }
            this.window.dispose();
        }
    }
}
