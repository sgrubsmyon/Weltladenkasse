package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
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
import javax.swing.event.*; // for DocumentListener
import javax.swing.text.*; // for DocumentFilter

// DateTime from date4j (http://www.date4j.net/javadoc/index.html)
import hirondelle.date4j.DateTime;
// JCalendarButton
import jcalendarbutton.org.JCalendarButton;
//import java.util.Calendar;
//import java.util.Date;

import WeltladenDB.DialogWindow;
import WeltladenDB.MainWindowGrundlage;

public class SelectZeitpunktForAbrechnungDialog extends DialogWindow
    implements DocumentListener, ItemListener, ChangeListener {
    // Attribute:
    private DateTime firstDate;
    private DateTime lastDate;

    private JSpinner dateSpinner;
    private SpinnerDateModel dateModel;
    private JCalendarButton calButt;

    private JButton okButton;
    private JButton cancelButton;

    // Methoden:
    public SelectZeitpunktForAbrechnungDialog(Connection conn, MainWindowGrundlage mw, JDialog dia,
            DateTime fd, DateTime ld) {
	super(conn, mw, dia);
        this.firstDate = fd;
        this.lastDate = ld;
        showAll();
    }

    protected void showHeader() {
        headerPanel = new JPanel();
        allPanel.add(headerPanel);
    }

    protected void showMiddle() {
        JPanel middlePanel = new JPanel();
        Date initialValue = new Date( firstDate.getMilliseconds(TimeZone.getDefault()) );
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, firstDate.getYear());
            calendar.set(Calendar.MONTH, firstDate.getMonth()-1);
            calendar.set(Calendar.DAY_OF_MONTH, firstDate.getDay()-1); // for strange reasons, we need day-1
            Date oneDayBeforeEarliestDate = calendar.getTime();
            //DateTime oneDBefEearD = firstDate.minusDays(1);
            //Date oneDayBeforeEarliestDate = new Date( oneDBefEearD.getMilliseconds(TimeZone.getDefault()) );
	//calendar.set(Calendar.DAY_OF_MONTH, firstDate.getDay());
	//initialValue = calendar.getTime();
        Date endValue = new Date( lastDate.getMilliseconds(TimeZone.getDefault()) );
        dateModel = new SpinnerDateModel(initialValue, // Startwert
                                     initialValue, // kleinster Wert
                                     endValue, // groesster Wert
                                     Calendar.YEAR);//ignored for user input
        dateSpinner = new JSpinner(dateModel);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));
	dateSpinner.addChangeListener(this);
        middlePanel.add(dateSpinner);
	calButt = new JCalendarButton(initialValue);
	calButt.addChangeListener(this);
        middlePanel.add(calButt);
        allPanel.add(middlePanel);
    }

    protected void showFooter() {
        footerPanel = new JPanel();
        okButton = new JButton("OK");
        okButton.setMnemonic(KeyEvent.VK_O);
        okButton.addActionListener(this);
        footerPanel.add(okButton);
        cancelButton = new JButton("Abbrechen");
        cancelButton.setMnemonic(KeyEvent.VK_A);
        cancelButton.addActionListener(this);
        footerPanel.add(cancelButton);
        allPanel.add(footerPanel);
    }

    public int submit() {
        return 0;
    }

    /** Needed for ChangeListener. */
    public void stateChanged(ChangeEvent e) {
    }

    /** Needed for ItemListener. */
    public void itemStateChanged(ItemEvent e) {
    }

    /**
     *    * Each non abstract class that implements the DocumentListener
     *      must have these methods.
     *
     *    @param e the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
    }
    public void removeUpdate(DocumentEvent e) {
    }
    public void changedUpdate(DocumentEvent e) {
    }


    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
	if (e.getSource() == okButton){
            submit();
            this.window.dispose();
            return;
        }
	if (e.getSource() == cancelButton){
            // communicate that insert abrechnung was canceled
            this.window.dispose();
            return;
        }
        super.actionPerformed(e);
    }

    // will data be lost on close?
    protected boolean willDataBeLost() {
        return false;
    }
}
