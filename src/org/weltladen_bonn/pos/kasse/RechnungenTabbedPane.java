package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.math.RoundingMode;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
//import java.awt.BorderLayout;
//import java.awt.FlowLayout;
//import java.awt.Dimension;
import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
import java.awt.event.*;

//import javax.swing.JFrame;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTable;
//import javax.swing.JTextArea;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;
import javax.swing.table.*;

import org.weltladen_bonn.pos.MainWindowGrundlage;
import org.weltladen_bonn.pos.TabbedPaneGrundlage;

public class RechnungenTabbedPane extends TabbedPaneGrundlage {
    private HeutigeRechnungen myRech;
    private AlteRechnungen myArchiv;
    private StornierteRechnungen myStorniert;

    // Methoden:
    /**
     *    The constructor.
     *       */
    public RechnungenTabbedPane(MariaDbPoolDataSource pool, MainWindowGrundlage mw, TabbedPaneGrundlage ptp) {
	    super(pool, mw, ptp);
    }

    @Override
    protected void createTabbedPane() {
        tabbedPane = new JTabbedPane();

        myRech = new HeutigeRechnungen(this.pool, this.mainWindow, this);
        myArchiv = new AlteRechnungen(this.pool, this.mainWindow);
        myStorniert = new StornierteRechnungen(this.pool, this.mainWindow);
        tabbedPane.addTab("Heutige Rechnungen", null, myRech, "Rechnungen von heute");
        tabbedPane.addTab("Alte Rechnungen", null, myArchiv, "Rechnungen von gestern und früher");
        tabbedPane.addTab("Stornierte Rechnungen", null, myStorniert, "Heutige stornierte Rechnungen");

        this.add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e){
    }
}
