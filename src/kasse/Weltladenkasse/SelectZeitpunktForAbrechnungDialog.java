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
    private DateTime firstDateTime;
    private DateTime lastDateTime;
    private Date firstDate;
    private Date lastDate;

    private JSpinner dateSpinner;
    private JSpinner hourSpinner;
    private JSpinner minuteSpinner;
    private SpinnerDateModel dateModel;
    private SpinnerNumberModel hourModel;
    private SpinnerNumberModel minuteModel;
    private JCalendarButton calButt;

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
        this.firstDate = new Date( firstDateTime.getStartOfDay().getMilliseconds(TimeZone.getDefault()) );
        this.lastDate = new Date( lastDateTime.getEndOfDay().getMilliseconds(TimeZone.getDefault()) );
        showAll();
    }

    protected void showHeader() {
        headerPanel = new JPanel();
        allPanel.add(headerPanel);
    }

    protected void showMiddle() {
        JPanel middlePanel = new JPanel();

        Date initialValue = lastDate;
        DateTime oneDBefFirst = firstDateTime.getStartOfDay().minusDays(1); // for strange reasons, we need day-1
        Date oneDayBeforeStartValue = new Date( oneDBefFirst.getMilliseconds(TimeZone.getDefault()) );
        Date endValue = lastDate;
        dateModel = new SpinnerDateModel(initialValue, // Startwert
                                     oneDayBeforeStartValue, // kleinster Wert
                                     endValue, // groesster Wert
                                     Calendar.YEAR);//ignored for user input
        dateSpinner = new JSpinner(dateModel);
        //dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd.MM.yyyy, HH:mm 'Uhr'"));
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd.MM.yyyy"));
	dateSpinner.addChangeListener(this);
        middlePanel.add(dateSpinner);

	calButt = new JCalendarButton(initialValue);
	calButt.addChangeListener(this);
        middlePanel.add(calButt);

        JDateChooser dateChooser = new JDateChooser(initialValue, null, new JSpinnerDateEditor());
        dateChooser.setMinSelectableDate(firstDate);
        dateChooser.setMaxSelectableDate(lastDate);
        middlePanel.add(dateChooser);

        hourModel = new SpinnerNumberModel(
                lastDateTime.getHour(), // initial value
                new Integer(0), // min
                lastDateTime.getHour(), // max (null == no max)
                new Integer(1)); // step
        hourSpinner = new JSpinner(hourModel);
        JSpinner.NumberEditor hourEditor = new JSpinner.NumberEditor(hourSpinner, "###");
        hourEditor.getTextField().setColumns(2);
        ( (NumberFormatter) hourEditor.getTextField().getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
        hourSpinner.setEditor(hourEditor);
        middlePanel.add(hourSpinner);

        middlePanel.add(new JLabel(":"));

        minuteModel = new SpinnerNumberModel(
                lastDateTime.getMinute(), // initial value
                new Integer(0), // min
                lastDateTime.getMinute(), // max (null == no max)
                new Integer(1)); // step
        minuteSpinner = new JSpinner(minuteModel);
        JSpinner.NumberEditor minuteEditor = new JSpinner.NumberEditor(minuteSpinner, "###");
        minuteEditor.getTextField().setColumns(2);
        ( (NumberFormatter) minuteEditor.getTextField().getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
        minuteSpinner.setEditor(minuteEditor);
        middlePanel.add(minuteSpinner);

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
	if (e.getSource() == dateSpinner){
	    SpinnerModel dateModel = dateSpinner.getModel();
	    if (dateModel instanceof SpinnerDateModel) {
		System.out.println( ((SpinnerDateModel)dateModel).getDate() );
		calButt.setTargetDate(((SpinnerDateModel)dateModel).getDate());
	    }
	}
	if (e.getSource() == calButt){
	    SpinnerModel dateModel = dateSpinner.getModel();
	    Date newDate = calButt.getTargetDate();
	    if ( newDate.before(firstDate) ){
		newDate = firstDate;
		calButt.setTargetDate(newDate);
	    }
	    if ( newDate.after(lastDate) ){
		newDate = lastDate;
		calButt.setTargetDate(newDate);
	    }
	    if (dateModel instanceof SpinnerDateModel) {
		if (newDate != null){
		    ((SpinnerDateModel)dateModel).setValue(newDate);
		}
	    }
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
