package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.text.SimpleDateFormat;
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.Connection;

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
import java.util.Date;
// JCalendar
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JSpinnerDateEditor;

import WeltladenDB.DialogWindow;
import WeltladenDB.MainWindowGrundlage;

public class SelectZeitpunktForAbrechnungDialog extends DialogWindow
    implements ChangeListener {
    // Attribute:
    private AbrechnungenTag abrechnungen;
    private final DateTime firstDateTime;
    private final DateTime lastDateTime;
    private final DateTime nowDateTime;
    private final Date firstDate;
    private final Date nowDate;
    // time of day, but since the epoch (needed by JSpinner showing time only):
    private final Date firstDayTimeAfterEpoch;
    private final Date nowDayTimeAfterEpoch;
    private final Date startOfDayAfterEpoch;
    private final Date endOfDayAfterEpoch;

    private JDateChooser dateChooser;
    private JSpinner dateSpinner;
    private JSpinner timeSpinner;
    private SpinnerDateModel timeModel;

    private JButton okButton;
    private JButton cancelButton;

    // Methoden:
    public SelectZeitpunktForAbrechnungDialog(Connection conn, MainWindowGrundlage mw,
            AbrechnungenTag at, JDialog dia,
            DateTime fd, DateTime ld, DateTime nd) {
	super(conn, mw, dia);
        this.abrechnungen = at;
        // make second = 0, because seconds cause problems:
        this.firstDateTime = fd.minus(0, 0, 0, 0, 0, fd.getSecond(), 0, DateTime.DayOverflow.LastDay);
        // make second = 0, because seconds cause problems:
        this.lastDateTime = ld.minus(0, 0, 0, 0, 0, ld.getSecond(), 0, DateTime.DayOverflow.LastDay);
        // make second = 0, because seconds cause problems:
        this.nowDateTime = nd.minus(0, 0, 0, 0, 0, nd.getSecond(), 0, DateTime.DayOverflow.LastDay);
        this.firstDate = dateFromDateTime( firstDateTime.getStartOfDay() );
        this.nowDate = dateFromDateTime( nowDateTime.getStartOfDay() );

        // following are times since the epoch (needed for JSpinner with time only):
        // (2nd argument of dateFromDateTime() is zero point, which is subtracted)
        this.firstDayTimeAfterEpoch = dateFromDateTime( firstDateTime, firstDateTime.getStartOfDay() );
        this.nowDayTimeAfterEpoch = dateFromDateTime( nowDateTime, nowDateTime.getStartOfDay() );
        this.startOfDayAfterEpoch = new Date(0); // THE EPOCH ITSELF!
        this.endOfDayAfterEpoch = dateFromDateTime( nowDateTime.getEndOfDay(), nowDateTime.getStartOfDay() );

        //System.out.println("First DateTime: "+this.firstDateTime);
        //System.out.println("Last DateTime: "+this.nowDateTime);
        //System.out.println("First Date: "+this.firstDate);
        //System.out.println("Last Date: "+this.nowDate);
        //System.out.println("First Day's Time: "+this.firstDayTimeAfterEpoch);
        //System.out.println("Last Day's Time: "+this.nowDayTimeAfterEpoch);
        //System.out.println("Start of Day: "+this.startOfDayAfterEpoch);
        //System.out.println("End of Day: "+this.endOfDayAfterEpoch);
        showAll();
    }

    protected void showHeader() {
        /**
         * Informations-Panel
         * */
        headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        // borders:
        int top = 10, left = 10, bottom = 10, right = 10;
        headerPanel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));

        JTextArea erklaerText = new JTextArea(2, 30);
        erklaerText.append("Die Rechnungen dieser Abrechnung "+
                "umfassen mehr als einen Tag:");
        erklaerText = makeLabelStyle(erklaerText);
        erklaerText.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        headerPanel.add(erklaerText);

        JPanel zeitraumPanel = new JPanel();
        zeitraumPanel.setBorder(BorderFactory.createTitledBorder("Zeitraum der Abrechnung"));
        zeitraumPanel.setLayout(new GridLayout(3, 2)); // 3 rows, 2 columns
            JLabel fruehLabel = new JLabel("Früheste Rechnung: ");
            JLabel fruehZeitLabel = new JLabel(firstDateTime.format(dateFormatDate4j));
            JLabel spaetLabel = new JLabel("Späteste Rechnung: ");
            JLabel spaetZeitLabel = new JLabel(lastDateTime.format(dateFormatDate4j));
            JLabel nowLabel = new JLabel("Jetzt: ");
            JLabel nowZeitLabel = new JLabel(nowDateTime.format(dateFormatDate4j));
            zeitraumPanel.add(fruehLabel);
            zeitraumPanel.add(fruehZeitLabel);
            zeitraumPanel.add(spaetLabel);
            zeitraumPanel.add(spaetZeitLabel);
            zeitraumPanel.add(nowLabel);
            zeitraumPanel.add(nowZeitLabel);
        headerPanel.add(zeitraumPanel);

        JTextArea wasTunText = new JTextArea(7, 30);
        wasTunText.append("Der Zeitpunkt der Abrechnung muss manuell gewählt werden!\n\n"+
                "Bitte versuche, den geeignetsten Zeitpunkt für diese "+
                "Abrechnung zu benutzen (z.B. Ende des Tages, zu dem diese "+
                "Abrechnung hauptsächlich gehört)."
                );
        wasTunText = makeLabelStyle(wasTunText);
        wasTunText.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        headerPanel.add(wasTunText);

        allPanel.add(headerPanel);
    }

    protected void showMiddle() {
        /**
         * Spinner-Panel
         * */
        JPanel middlePanel = new JPanel();

        JSpinnerDateEditor sdEdit = new JSpinnerDateEditor();
        dateSpinner = (JSpinner)sdEdit.getUiComponent();
        dateChooser = new JDateChooser((Date)nowDate.clone(), null, sdEdit);
        dateChooser.setMinSelectableDate((Date)firstDate.clone());
        dateChooser.setMaxSelectableDate((Date)nowDate.clone());
        dateChooser.setLocale(myLocale);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd.MM.yyyy"));
	dateSpinner.addChangeListener(this);
        middlePanel.add(dateChooser);

        timeModel = new SpinnerDateModel();
        timeSpinner = new JSpinner(timeModel);
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
        SimpleDateFormat format = timeEditor.getFormat();
        format.setTimeZone(TimeZone.getTimeZone("GMT")); // this spinner only
                            // works on first day after epoch (which is in UTC),
                            // so set its time zone to UTC
                            // Otherwise: e.g. 18:00 after epoch is displayed as 19:00
                            // if we are in CET
                            // The whole problem is this one:
                            // http://stackoverflow.com/questions/13741371/jspinner-with-spinnerdatemodel-weird-behaviour
        timeSpinner.setEditor(timeEditor);
        timeModel.setValue(nowDayTimeAfterEpoch);
        timeModel.setStart(startOfDayAfterEpoch); // no constraint
        timeModel.setEnd(nowDayTimeAfterEpoch);
        middlePanel.add(timeSpinner);
        middlePanel.add(new JLabel("Uhr"));

        //System.out.println("timeSpinner getValue: "+timeSpinner.getValue());
        //System.out.println("timeSpinner getStart: "+timeModel.getStart());
        //System.out.println("timeSpinner getEnd: "+timeModel.getEnd());
        allPanel.add(middlePanel);
    }

    protected void showFooter() {
        /**
         * Button-Panel
         * */
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
            //System.out.println("date chooser: "+dateChooser.getDate());
            if ( dateChooser.getDate().equals(this.nowDate) ){
                //System.out.println("nowDate selected: "+this.nowDate+".");
                setStartTime(startOfDayAfterEpoch); // no constraint
                setEndTime(nowDayTimeAfterEpoch);
            }
            else if ( dateChooser.getDate().equals(this.firstDate) ){
                //System.out.println("firstDate selected: "+this.firstDate+".");
                setStartTime(firstDayTimeAfterEpoch);
                setEndTime(endOfDayAfterEpoch); // no constraint
            }
            else {
                //System.out.println("middle date selected.");
                setStartTime(startOfDayAfterEpoch); // no constraint
                setEndTime(endOfDayAfterEpoch); // no constraint
            }
            return;
	}
    }

    protected int submit() {
        java.sql.Timestamp selectedTimestamp = new java.sql.Timestamp( dateChooser.getDate().getTime() +
                timeModel.getDate().getTime() );
        this.abrechnungen.setSelectedZeitpunkt( selectedTimestamp.toString() );
        return 0;
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
            // communicate that insert abrechnung was canceled:
            this.abrechnungen.setSelectedZeitpunkt( null );
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
