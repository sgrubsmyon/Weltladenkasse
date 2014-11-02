package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

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
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;
import javax.swing.table.*;

public abstract class ArtikelDialogWindowGrundlage extends DialogWindow {
    // Attribute:
    protected JPanel allPanel;
    protected Artikelliste artikelListe;

    protected JPanel headerPanel;
    protected JPanel footerPanel;

    // Methoden:
    public ArtikelDialogWindowGrundlage(Connection conn, MainWindowGrundlage mw, Artikelliste pw, JDialog dia) {
	super(conn, mw, dia);
        this.artikelListe = pw;
    }

    protected void showAll() {
	allPanel = new JPanel();
	allPanel.setLayout(new BoxLayout(allPanel, BoxLayout.Y_AXIS));

        showHeader();
        showMiddle();
        showFooter();

	this.add(allPanel, BorderLayout.CENTER);
    }

    abstract void showHeader();
    abstract void showMiddle();
    abstract void showFooter();

    protected void updateAll(){
	this.remove(allPanel);
	this.revalidate();
	showAll();
    }

    abstract int submit();
}
