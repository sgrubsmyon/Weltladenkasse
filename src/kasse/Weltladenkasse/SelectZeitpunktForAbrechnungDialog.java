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
    private final Date firstDate;
    private final Date lastDate;
    // time of day, but since the epoch (needed by JSpinner showing time only):
    private final Date firstDayTime;
    private final Date lastDayTime;
    private final Date startOfDay;
    private final Date endOfDay;

    private JDateChooser dateChooser;
    private JSpinner dateSpinner;
    private JSpinner timeSpinner;
    private SpinnerDateModel timeModel;

    private JButton okButton;
    private JButton cancelButton;

    // Methoden:
    public SelectZeitpunktForAbrechnungDialog(Connection conn, MainWindowGrundlage mw,
            AbrechnungenTag at, JDialog dia,
            DateTime fd, DateTime ld) {
	super(conn, mw, dia);
        this.parentWindow = at;
        // make second = 0, because seconds cause problems:
        this.firstDateTime = fd.minus(0, 0, 0, 0, 0, fd.getSecond(), 0, DateTime.DayOverflow.LastDay);
        // make second = 0, because seconds cause problems:
        this.lastDateTime = ld.minus(0, 0, 0, 0, 0, ld.getSecond(), 0, DateTime.DayOverflow.LastDay);
        this.firstDate = new Date( firstDateTime.getStartOfDay().getMilliseconds(TimeZone.getDefault()) );
        this.lastDate = new Date( lastDateTime.getStartOfDay().getMilliseconds(TimeZone.getDefault()) );

        // following are times since the epoch (needed for JSpinner with time only):
        long timeZoneOffset = TimeZone.getDefault().getOffset(0); // offset of
                                        // this PC's time zone to UTC (at time of the epoch, i.e. long = 0
            // this offset is automatically added by Java to display a time in
            // the local time zone, so we need to subtract it to have a time as
            // in UTC, but displayed in the local time zone (it's ugly, I
            // know, but I also don't want to change the Spinner's Locale to
            // UK!), otherwise: e.g. 18:00 after epoch is displayed as 19:00
            // after epoch if we are in CET
            // The whole problem is this one: http://stackoverflow.com/questions/13741371/jspinner-with-spinnerdatemodel-weird-behaviour
        this.firstDayTime = new Date( firstDateTime.getMilliseconds(TimeZone.getDefault()) -
                firstDateTime.getStartOfDay().getMilliseconds(TimeZone.getDefault()) -
                timeZoneOffset);
        this.lastDayTime = new Date( lastDateTime.getMilliseconds(TimeZone.getDefault()) -
                lastDateTime.getStartOfDay().getMilliseconds(TimeZone.getDefault()) -
                timeZoneOffset);
        this.startOfDay = new Date(0 - timeZoneOffset); // THE EPOCH!
        this.endOfDay = new Date( firstDateTime.getEndOfDay().getMilliseconds(TimeZone.getDefault()) -
                firstDateTime.getStartOfDay().getMilliseconds(TimeZone.getDefault()) -
                timeZoneOffset);

        //System.out.println("First DateTime: "+this.firstDateTime);
        //System.out.println("Last DateTime: "+this.lastDateTime);
        //System.out.println("First Date: "+this.firstDate);
        //System.out.println("Last Date: "+this.lastDate);
        //System.out.println("First Day's Time: "+this.firstDayTime);
        //System.out.println("Last Day's Time: "+this.lastDayTime);
        //System.out.println("Start of Day: "+this.startOfDay);
        //System.out.println("End of Day: "+this.endOfDay);
        showAll();
    }

    protected void showHeader() {
        headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
            JLabel erklaerText = new JLabel("Die Rechnungen dieser Abrechnung "+
                    "umfassen mehr als einen Tag:");
            erklaerText.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            headerPanel.add(erklaerText);
            JPanel zeitraumPanel = new JPanel();
            zeitraumPanel.setLayout(new BoxLayout(zeitraumPanel, BoxLayout.Y_AXIS));
            zeitraumPanel.setBorder(BorderFactory.createTitledBorder("Zeitraum der Abrechnung"));
                JPanel zeitraumGridPanel = new JPanel();
                    zeitraumGridPanel.setLayout(new GridLayout(2, 2)); // 2 rows, 2 columns
                    JLabel fruehLabel = new JLabel("Früheste Rechnung: ");
                    JLabel fruehZeitLabel = new JLabel(firstDateTime.format(dateFormatDate4j));
                    JLabel spaetLabel = new JLabel("Späteste Rechnung: ");
                    JLabel spaetZeitLabel = new JLabel(lastDateTime.format(dateFormatDate4j));
                    fruehLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
                    fruehZeitLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
                    spaetLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
                    spaetZeitLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
                    zeitraumGridPanel.add(fruehLabel);
                    zeitraumGridPanel.add(fruehZeitLabel);
                    zeitraumGridPanel.add(spaetLabel);
                    zeitraumGridPanel.add(spaetZeitLabel);
                zeitraumPanel.add(zeitraumGridPanel);
            headerPanel.add(zeitraumPanel);
            JLabel[] wasTunText = new JLabel[]{
                new JLabel("Der Zeitpunkt der Abrechnung muss manuell gewählt werden."),
                new JLabel("Bitte versuche, den geeignetsten Zeitpunkt für diese "+
                "Abrechnung zu benutzen."),
                new JLabel("(z.B. Ende des Tages, zu dem diese Abrechnung hauptsächlich gehört.)")
            };
            for (JLabel text : wasTunText){
                text.setAlignmentX(JComponent.LEFT_ALIGNMENT);
                headerPanel.add(text);
            }
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

        timeModel = new SpinnerDateModel();
        timeModel.setValue(lastDayTime);
        timeModel.setStart(startOfDay); // no constraint
        timeModel.setEnd(lastDayTime);
        timeSpinner = new JSpinner(timeModel);
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
        timeSpinner.setEditor(timeEditor);
	timeSpinner.addChangeListener(this);
        middlePanel.add(timeSpinner);
        middlePanel.add(new JLabel("Uhr"));

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

    void setStartTime(Date minTime) {
        if ( timeModel.getDate().before(minTime) ){
            // prevent blocking because time is out of bounds
            timeModel.setValue(minTime);
        }
        timeModel.setStart(minTime);
    }

    void setEndTime(Date maxTime) {
        if ( timeModel.getDate().after(maxTime) ){
            // prevent blocking because time is out of bounds
            timeModel.setValue(maxTime);
        }
        timeModel.setEnd(maxTime);
    }

    /** Needed for ChangeListener. */
    public void stateChanged(ChangeEvent e) {
	if (e.getSource() == dateSpinner){
            System.out.println("date chooser: "+dateChooser.getDate());
            if ( dateChooser.getDate().equals(this.lastDate) ){
                System.out.println("lastDate selected: "+this.lastDate+".");
                setStartTime(startOfDay); // no constraint
                setEndTime(lastDayTime);
            }
            else if ( dateChooser.getDate().equals(this.firstDate) ){
                System.out.println("firstDate selected: "+this.firstDate+".");
                setStartTime(firstDayTime);
                setEndTime(endOfDay); // no constraint
            }
            else {
                System.out.println("middle date selected.");
                setStartTime(startOfDay); // no constraint
                setEndTime(endOfDay); // no constraint
            }
            return;
	}
	if (e.getSource() == timeSpinner){
            System.out.println("time spinner: "+timeSpinner.getValue());
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
