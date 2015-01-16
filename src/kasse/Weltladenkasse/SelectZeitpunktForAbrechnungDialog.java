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
// JCalendar
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JSpinnerDateEditor;

import WeltladenDB.DialogWindow;
import WeltladenDB.MainWindowGrundlage;

public class SelectZeitpunktForAbrechnungDialog extends DialogWindow
    implements DocumentListener, ItemListener, ChangeListener {
    // Attribute:
    private AbrechnungenTag parentWindow;
    private final DateTime firstDateTime;
    private final DateTime lastDateTime;
    private final Date firstDateWithTime;
    private final Date lastDateWithTime;
    private final Date firstDate;
    private final Date lastDate;

    private JDateChooser dateChooser;
    private JSpinner dateSpinner;
    private JSpinner timeSpinner;
    private SpinnerDateModel timeModel;
    private JSpinner hourSpinner;
    private JSpinner minuteSpinner;
    private SpinnerNumberModel hourModel;
    private SpinnerNumberModel minuteModel;

    private JButton okButton;
    private JButton cancelButton;

    // Methoden:
    public SelectZeitpunktForAbrechnungDialog(Connection conn, MainWindowGrundlage mw,
            AbrechnungenTag at, JDialog dia,
            DateTime fd, DateTime ld) {
	super(conn, mw, dia);
        this.parentWindow = at;
        this.firstDateTime = fd;
        this.lastDateTime = ld;
        this.lastDateWithTime = new Date( lastDateTime.getMilliseconds(TimeZone.getDefault()) );
        this.firstDateWithTime = new Date( firstDateTime.getMilliseconds(TimeZone.getDefault()) );
        this.firstDate = new Date( firstDateTime.getStartOfDay().getMilliseconds(TimeZone.getDefault()) );
        this.lastDate = new Date( lastDateTime.getStartOfDay().getMilliseconds(TimeZone.getDefault()) );
        System.out.println("First Date: "+this.firstDate);
        System.out.println("Last Date: "+this.lastDate);
        System.out.println("First Date With Time: "+this.firstDateWithTime);
        System.out.println("Last Date With Time: "+this.lastDateWithTime);
        showAll();
    }

    protected void showHeader() {
        headerPanel = new JPanel();
        allPanel.add(headerPanel);
    }

    protected void showMiddle() {
        JPanel middlePanel = new JPanel();

        JSpinnerDateEditor sdEdit = new JSpinnerDateEditor();
        dateSpinner = (JSpinner)sdEdit.getUiComponent();
        dateChooser = new JDateChooser((Date)lastDate.clone(), null, sdEdit);
        dateChooser.setMinSelectableDate((Date)firstDate.clone());
        dateChooser.setMaxSelectableDate((Date)lastDate.clone());
        dateChooser.setLocale(myLocale);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd.MM.yyyy"));
	dateSpinner.addChangeListener(this);
        middlePanel.add(dateChooser);

        DateTime oneDBefFirst = firstDateTime.getStartOfDay().minusDays(1); // for strange reasons, we need day-1
        Date oneDayBeforeStartValue = new Date( oneDBefFirst.getMilliseconds(TimeZone.getDefault()) );

        timeModel = new SpinnerDateModel();
        timeModel.setValue(lastDateWithTime);
        timeModel.setStart(lastDate);
        timeModel.setEnd(lastDateWithTime);
        timeSpinner = new JSpinner(timeModel);
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
        timeSpinner.setEditor(timeEditor);
        middlePanel.add(timeSpinner);

        /*
        hourModel = new SpinnerNumberModel(
                lastDateTime.getHour(), // initial value
                new Integer(0), // min
                lastDateTime.getHour(), // max (null == no max)
                new Integer(1)); // step
        hourSpinner = new JSpinner(hourModel);
        JSpinner.NumberEditor hourEditor = new JSpinner.NumberEditor(hourSpinner, "00");
        hourEditor.getTextField().setColumns(2);
        ( (NumberFormatter) hourEditor.getTextField().getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
        hourSpinner.setEditor(hourEditor);
	hourSpinner.addChangeListener(this);
        middlePanel.add(hourSpinner);

        middlePanel.add(new JLabel(":"));

        minuteModel = new SpinnerNumberModel(
                lastDateTime.getMinute(), // initial value
                new Integer(0), // min
                lastDateTime.getMinute(), // max (null == no max)
                new Integer(1)); // step
        minuteSpinner = new JSpinner(minuteModel);
        JSpinner.NumberEditor minuteEditor = new JSpinner.NumberEditor(minuteSpinner, "00");
        minuteEditor.getTextField().setColumns(2);
        ( (NumberFormatter) minuteEditor.getTextField().getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
        minuteSpinner.setEditor(minuteEditor);
	minuteSpinner.addChangeListener(this);
        middlePanel.add(minuteSpinner);
        */

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

    void setMinimumHour(Integer minHour) {
        /*
        if ((Integer)hourSpinner.getValue() < minHour){
            hourSpinner.setValue(minHour);
        }
        hourModel.setMinimum(minHour);
        */
    }

    void setMinimumMinute(Integer minMinute) {
        /*
        if ((Integer)minuteSpinner.getValue() < minMinute){
            minuteSpinner.setValue(minMinute);
        }
        minuteModel.setMinimum(minMinute);
        */
    }

    void setMaximumHour(Integer maxHour) {
        /*
        if ((Integer)hourSpinner.getValue() > maxHour){
            hourSpinner.setValue(maxHour);
        }
        hourModel.setMaximum(maxHour);
        */
    }

    void setMaximumMinute(Integer maxMinute) {
        /*
        if ((Integer)minuteSpinner.getValue() > maxMinute){
            minuteSpinner.setValue(maxMinute);
        }
        minuteModel.setMaximum(maxMinute);
        */
    }

    /** Needed for ChangeListener. */
    public void stateChanged(ChangeEvent e) {
	if (e.getSource() == dateSpinner){
            System.out.println("date chooser: "+dateChooser.getDate());
            if ( dateChooser.getDate().equals(this.lastDate) ){
                System.out.println("lastDate selected: "+this.lastDate+".");
                setMinimumHour(new Integer(0));
                setMinimumMinute(new Integer(0));
                setMaximumHour(lastDateTime.getHour());
                if ( ((Integer)hourSpinner.getValue()).equals(lastDateTime.getHour()) ){
                    // if last hour is selected: restrict minute
                    setMaximumMinute(lastDateTime.getMinute());
                }
            }
            else if ( dateChooser.getDate().equals(this.firstDate) ){
                System.out.println("firstDate selected: "+this.firstDate+".");
                setMinimumHour(firstDateTime.getHour());
                if ( ((Integer)hourSpinner.getValue()).equals(firstDateTime.getHour()) ){
                    // if first hour is selected: restrict minute
                    setMinimumMinute(firstDateTime.getMinute());
                }
                setMaximumHour(new Integer(23));
                setMaximumMinute(new Integer(59));
            }
            else {
                System.out.println("middle date selected.");
                setMinimumHour(new Integer(0));
                setMinimumMinute(new Integer(0));
                setMaximumHour(new Integer(23));
                setMaximumMinute(new Integer(59));
            }
            return;
	}
	if (e.getSource() == hourSpinner){
            // update minute's maximum
            if ( dateChooser.getDate().equals(this.lastDate) &&
                   ((Integer)hourSpinner.getValue()).equals(lastDateTime.getHour()) ){
                // if we are on last day and in last hour: restrict minute
                setMaximumMinute(lastDateTime.getMinute());
            } else {
                // in all other cases: no restriction
                setMaximumMinute(new Integer(59));
            }
            // update minute's minimum
            if ( dateChooser.getDate().equals(this.firstDate) &&
                   ((Integer)hourSpinner.getValue()).equals(firstDateTime.getHour()) ){
                // if we are on first day and in first hour: restrict minute
                setMinimumMinute(firstDateTime.getMinute());
            } else {
                // in all other cases: no restriction
                setMinimumMinute(new Integer(0));
            }
            return;
        }
	if (e.getSource() == minuteSpinner){
            return;
        }
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
