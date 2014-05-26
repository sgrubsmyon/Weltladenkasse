package Weltladenbesteller;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
import java.math.RoundingMode;

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
//import javax.swing.JTextArea;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;
import javax.swing.table.*;

import WeltladenDB.MainWindowGrundlage;
import WeltladenDB.AnyJComponentJTable;
import WeltladenDB.JComponentCellRenderer;
import WeltladenDB.JComponentCellEditor;

public class BestellAnzeige extends ArtikelGrundlage {
    // Attribute:
    protected int bestellungenProSeite = 25;
    protected int currentPage = 1;
    protected int totalPage;

    protected String filterStr; // show only specific items of the order

    protected JPanel navigationPanel; // on the left
    protected JPanel tablePanel; // on the right
    protected AnyJComponentJTable orderTable;
    protected AnyJComponentJTable orderDetailTable;

    protected JButton prevButton;
    protected JButton nextButton;
    protected JButton editButton; // click the button to edit the order (in the Bestellen tab)

    protected Vector< Vector<String> > orderData;
    protected Vector<String> orderLabels;
    protected Vector< Vector<String> > orderDetailData;
    protected Vector<String> orderDetailLabels;
    protected String bestellungsZahl;
    protected int bestellungsZahlInt;

    // Methoden:

    /**
     *    The constructor.
     *       */
    public BestellAnzeige(Connection conn, MainWindowGrundlage mw)
    {
	super(conn, mw);
    }
