package org.weltladen_bonn.pos.besteller;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.math.RoundingMode;

// GUI stuff:
import java.awt.Component;
import java.awt.Font;
import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.*;
import java.awt.event.MouseEvent;
import java.awt.Point;
import javax.swing.JTable;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

import org.weltladen_bonn.pos.MainWindowGrundlage;
import org.weltladen_bonn.pos.ArtikelGrundlage;
import org.weltladen_bonn.pos.AnyJComponentJTable;

public abstract class BestellungsGrundlage extends ArtikelGrundlage {
    protected Vector<String> columnLabels;

    protected JTextField totalPriceField;

    // Die Ausrichter:
    protected final String einrueckung = "      ";

    /**
     *    The constructor.
     *       */
    public BestellungsGrundlage(Connection conn, MainWindowGrundlage mw)
    {
	super(conn, mw);
	columnLabels = new Vector<String>();
        columnLabels.add("Pos.");
	columnLabels.add("Lieferant"); columnLabels.add("Artikel-Nr."); columnLabels.add("Artikel-Name"); 
        columnLabels.add("Einzelpreis"); columnLabels.add("VPE"); columnLabels.add("Stückzahl");
        columnLabels.add("Beliebtheit");
    }

    protected void setTableProperties(JTable table) {
	// Spalteneigenschaften:
	TableColumn pos = table.getColumn("Pos.");
	pos.setCellRenderer(zentralAusrichter);
	pos.setPreferredWidth(200);
	TableColumn lieferant = table.getColumn("Lieferant");
	lieferant.setCellRenderer(linksAusrichter);
	lieferant.setPreferredWidth(500);
	TableColumn artikelnr = table.getColumn("Artikel-Nr.");
	artikelnr.setCellRenderer(linksAusrichter);
	artikelnr.setPreferredWidth(500);
	TableColumn artikelbez = table.getColumn("Artikel-Name");
	artikelbez.setCellRenderer(linksAusrichter);
	artikelbez.setPreferredWidth(1500);
	TableColumn preis = table.getColumn("Einzelpreis");
	preis.setCellRenderer(rechtsAusrichter);
	preis.setPreferredWidth(300);
	TableColumn vpe = table.getColumn("VPE");
	vpe.setCellRenderer(rechtsAusrichter);
	vpe.setPreferredWidth(200);
	TableColumn stueckzahl = table.getColumn("Stückzahl");
	stueckzahl.setCellRenderer(rechtsAusrichter);
	stueckzahl.setPreferredWidth(200);
	TableColumn beliebt = table.getColumn("Beliebtheit");
	beliebt.setCellRenderer(zentralAusrichter);
	beliebt.setPreferredWidth(200);
    }
}
