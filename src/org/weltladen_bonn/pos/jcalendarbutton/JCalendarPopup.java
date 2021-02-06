/**
 * CalendarPopup.java
 *
 * Created on March 5, 2000, 5:07 AM
 * @author Don Corley <don@donandann.com>
 * @version 1.0.0
 */
 
package org.weltladen_bonn.pos.jcalendarbutton;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;

/** 
 * A JCalendarPopup is a popup calendar the user can click on to change a date.
 * @author  Don Corley <don@donandann.com>
 * @version 1.4.3
 *  Copyright (C) 2010  Don Corley
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class JCalendarPopup extends JPanel
    implements MouseListener {
	private static final long serialVersionUID = 1L;

	/**
     * The default param for the date property.
     */
    public static final String DATE_PARAM = "date";
    /**
     * The date param for this popup.
     */
    protected String dateParam = DATE_PARAM;

    public static Border ROLLOVER_BORDER = new LineBorder(Color.GRAY, 1);
    public static Border SELECTED_BORDER = new LineBorder(Color.BLUE, 1);
    public static Border EMPTY_BORDER = new EmptyBorder(1, 1, 1, 1);
    /**
     * Constant - Milliseconds in a day.
     */
    public static final long KMS_IN_A_DAY = 24 * 60 * 60 * 1000;    // Milliseconds in a day
    protected Date targetDate = null;
    protected Date targetPanelDate = null;
    protected Date selectedDate = null;
    protected Date nowDate = null;
    protected boolean firstTime = true;
    protected int targetComponent = 0;

    protected Calendar calendar = Calendar.getInstance();
    protected StringBuffer stringBuffer = new StringBuffer();
    protected DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL);

    // Variables declaration - do not modify
    private JPanel monthPanel;
    private JButton previousMonthButton;
    private JLabel monthLabel;
    private JButton nextMonthButton;

    private JPanel yearPanel;
    private JButton previousYearButton;
    private JLabel yearLabel;
    private JButton nextYearButton;

    private JPanel panelDays;
    // End of variables declaration
    /**
     * Transfer the focus after selecting the date (default = true).
     */
    protected boolean transferFocus = true;
    /**
     * The name of the calendar popup icon.
     */
    public static final String CALENDAR_ICON = "Calendar";
    /**
     * The Insets around this button.
     */
    public static final Insets NO_INSETS = new Insets(0, 0, 0, 0);

    /**
     * Creates new form CalendarPopup.
     */
    public JCalendarPopup()
    {
        super();
    }
    /**
     * Creates new form CalendarPopup.
     * @param date The initial date for this button.
     */
    public JCalendarPopup(Date date)
    {
        this();
        this.init(null, date, null);
    }
    /**
     * Creates new form CalendarPopup.
     * @param strDateParam The name of the date property (defaults to "date").
     * @param date The initial date for this button.
     */
    public JCalendarPopup(String strDateParam, Date date)
    {
        this();
        this.init(strDateParam, date, null);
    }
    /**
     * Creates new form CalendarPopup.
     * @param strDateParam The name of the date property (defaults to "date").
     * @param date The initial date for this button.
     * @param strLanguage The language to use.
     */
    public JCalendarPopup(String strDateParam, Date date, String strLanguage)
    {
        this();
        this.init(strDateParam, date, strLanguage);
    }
    /**
     * Creates new form CalendarPopup.
     * @param strDateParam The name of the date property (defaults to "date").
     * @param date The initial date for this button.
     * @param strLanguage The language to use.
     */
    public void init(String strDateParam, Date dateTarget, String strLanguage)
    {
        if (strDateParam != null)
            dateParam = strDateParam;      // Property name
        initComponents();
        this.setName("JCalendarPopup");

        nowDate = new Date();
        if (dateTarget == null)
            dateTarget = nowDate;
        selectedDate = dateTarget;

        if (strLanguage != null)
        {
            Locale locale = new Locale(strLanguage, "");
            if (locale != null)
            {
                calendar = Calendar.getInstance(locale);
                dateFormat = DateFormat.getDateInstance(DateFormat.FULL, locale);
            }
        }
        calendar.setTime(dateTarget);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        targetDate = calendar.getTime();
        this.layoutCalendar(targetDate);
        if (dateTarget == nowDate)
            selectedDate = targetDate;  // If they want a default date, make the time 12:00
    }
    /**
     * Add all the components to this calendar panel.
     * @param dateTarget This date needs to be in the calendar.
     */
    public void layoutCalendar(Date dateTarget)
    {
        calendar.setTime(dateTarget);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        targetPanelDate = calendar.getTime();
        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date dateFirstOfMonth = calendar.getTime();
        calendar.setTime(dateFirstOfMonth);
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DATE, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date dateLastOfMonth = calendar.getTime();
        Date dateCalendarFirstDate = this.getFirstDateInCalendar(dateFirstOfMonth);
        Date dateCalendar = new Date(dateCalendarFirstDate.getTime());

        targetComponent = (int)((targetDate.getTime() - dateCalendarFirstDate.getTime()) / KMS_IN_A_DAY);
        if (targetComponent < 0)
            targetComponent--;

        String strYear = this.getDateString(dateTarget, DateFormat.YEAR_FIELD);
        String strMonth = this.getDateString(dateTarget, DateFormat.MONTH_FIELD);
        monthLabel.setText(strMonth);
        yearLabel.setText(strYear);
        int iDayOfWeekComponent = 0;
        int iDayComponent = 7;
        for (; iDayComponent < panelDays.getComponentCount(); iDayOfWeekComponent++, iDayComponent++)
        {
            if (iDayOfWeekComponent < 7)
            {
                JLabel labelDayOfWeek = (JLabel)panelDays.getComponent(iDayOfWeekComponent);
                String strWeek = this.getDateString(dateCalendar, DateFormat.DAY_OF_WEEK_FIELD);
                if ((strWeek != null) && (strWeek.length() > 0))
                    labelDayOfWeek.setText(strWeek.substring(0, 1));
                else
                    labelDayOfWeek.setText(Integer.toString(iDayOfWeekComponent));
            }
            JLabel labelDay = (JLabel)panelDays.getComponent(iDayComponent);
            String strDay = this.getDateString(dateCalendar, DateFormat.DATE_FIELD);
            labelDay.setText(strDay);
            if ((dateCalendar.before(dateFirstOfMonth))
                || (dateCalendar.after(dateLastOfMonth)))
                    labelDay.setForeground(Color.GRAY);
            else
                    labelDay.setForeground(Color.BLACK);
            labelDay.setBackground(panelDays.getBackground());
            if (targetComponent == iDayComponent - 7)
                labelDay.setBorder(SELECTED_BORDER);
            else
                labelDay.setBorder(EMPTY_BORDER);

            calendar.setTime(dateCalendar);
            calendar.add(Calendar.DATE, 1);
            dateCalendar = calendar.getTime();

            if (firstTime)
            {
                labelDay.addMouseListener(this);
                labelDay.setName(Integer.toString(iDayComponent - 7));
            }
        }
        firstTime = false;
    }
    /**
     * This method is called from within the constructor to initialize the form.
     */
    private void initComponents()
    {
        ClassLoader cl = this.getClass().getClassLoader();

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        monthPanel = new JPanel();
        monthPanel.setName("monthPanel");
        monthPanel.setLayout (new BoxLayout(monthPanel, BoxLayout.X_AXIS));
        previousMonthButton = new JButton();
        previousMonthButton.setName("previousMonthButton");
        monthLabel = new JLabel();
        monthLabel.setName("monthLabel");
        try   {
            Icon icon  = new ImageIcon(cl.getResource("images/buttons/" + "Back" + ".gif"));
            previousMonthButton.setIcon(icon);
        } catch (Exception ex)  {
            previousMonthButton.setText("<");
        }
        previousMonthButton.setMargin(new Insets(2, 2, 2, 2));
        previousMonthButton.addActionListener(new ActionListener () {
            public void actionPerformed (ActionEvent evt) {
                prevMonthActionPerformed (evt);
            }
        }
        );
        monthPanel.add(previousMonthButton);
        monthPanel.add(Box.createHorizontalGlue());
        monthLabel.setText ("month");
        monthLabel.setHorizontalAlignment (SwingConstants.CENTER);
        monthPanel.add (monthLabel);

        nextMonthButton = new JButton();
        nextMonthButton.setName("nextMonthButton");
        nextMonthButton.setAlignmentX (1.0F);
        try   {
            Icon icon  = new ImageIcon(cl.getResource("images/buttons/" + "Forward" + ".gif"));
            nextMonthButton.setIcon(icon);
        } catch (Exception ex)  {
            nextMonthButton.setText(">");
        }
        nextMonthButton.setMargin (new Insets(2, 2, 2, 2));
        nextMonthButton.addActionListener (new ActionListener () {
            public void actionPerformed (ActionEvent evt) {
                nextMonthActionPerformed (evt);
            }
        }
        );
        monthPanel.add(Box.createHorizontalGlue());
        monthPanel.add (nextMonthButton);
        add (monthPanel);

        yearPanel = new JPanel();
        yearPanel.setName("yearPanel");
        yearPanel.setLayout (new BoxLayout (yearPanel, BoxLayout.X_AXIS));
        previousYearButton = new JButton();
        previousYearButton.setName("previousYearButton");
        yearLabel = new JLabel();
        yearLabel.setName("yearLabel");
        try   {
            Icon icon  = new ImageIcon(cl.getResource("images/buttons/" + "Back" + ".gif"));
            previousYearButton.setIcon(icon);
        } catch (Exception ex)  {
            previousYearButton.setText("<");
        }
        previousYearButton.setMargin (new Insets(2, 2, 2, 2));
        previousYearButton.addActionListener (new ActionListener () {
            public void actionPerformed (ActionEvent evt) {
                prevYearActionPerformed (evt);
            }
        }
        );
        yearPanel.add (previousYearButton);
        yearPanel.add(Box.createHorizontalGlue());
        yearLabel.setText ("Year");
        yearLabel.setHorizontalAlignment (SwingConstants.CENTER);
        yearPanel.add (yearLabel);

        yearPanel.add(Box.createHorizontalGlue());
        nextYearButton = new JButton();
        nextYearButton.setName("nextYearButton");
        nextYearButton.setAlignmentX (1.0F);
        try   {
            Icon icon  = new ImageIcon(cl.getResource("images/buttons/" + "Forward" + ".gif"));
            nextYearButton.setIcon(icon);
        } catch (Exception ex)  {
            nextYearButton.setText(">");
        }
        nextYearButton.setMargin (new Insets(2, 2, 2, 2));
        nextYearButton.addActionListener (new ActionListener () {
            public void actionPerformed (ActionEvent evt) {
                nextYearActionPerformed (evt);
            }
        }
        );
        yearPanel.add (nextYearButton);
        add (yearPanel);

        panelDays = new JPanel();
        panelDays.setName("panelDays");
        panelDays.setLayout (new GridLayout (7, 7));
        for (int i = 1; i <= 7; i++)
        {
            JLabel label = new JLabel();
            label.setText(Integer.toString(i));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            panelDays.add(label);
        }

        for (int i = 1; i <= 7 * 6; i++)
        {
            JLabel label = new JLabel();
            label.setBorder(EMPTY_BORDER);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            panelDays.add(label);
        }

        add (panelDays);
        
        this.setBorder(new EmptyBorder(2, 2, 2, 2));
    }
    /**
     * User pressed the "next month" button, change the calendar.
     * @param evt The action event (ignored).
     */
    private void nextMonthActionPerformed (ActionEvent evt)
    {
        calendar.setTime(targetPanelDate);
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        targetPanelDate = calendar.getTime();
        this.layoutCalendar(targetPanelDate);
    }
    /**
     * User pressed the "previous month" button, change the calendar.
     * @param evt The action event (ignored).
     */
    private void prevMonthActionPerformed (ActionEvent evt)
    {
        calendar.setTime(targetPanelDate);
        calendar.add(Calendar.MONTH, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        targetPanelDate = calendar.getTime();
        this.layoutCalendar(targetPanelDate);
    }
    /**
     * User pressed the "next year" button, change the calendar.
     * @param evt The action event (ignored).
     */
    private void nextYearActionPerformed (ActionEvent evt)
    {
        calendar.setTime(targetPanelDate);
        calendar.add(Calendar.YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        targetPanelDate = calendar.getTime();
        this.layoutCalendar(targetPanelDate);
    }
    /**
     * User pressed the "previous year" button, change the calendar.
     * @param evt The action event (ignored).
     */
    private void prevYearActionPerformed (ActionEvent evt)
    {
        calendar.setTime(targetPanelDate);
        calendar.add(Calendar.YEAR, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        targetPanelDate = calendar.getTime();
        this.layoutCalendar(targetPanelDate);
    }
    /**
     * Given the first date of the calendar, get the first date of that week.
     * @param dateTarget A valid date.
     * @return The first day in the week (day of week depends on Locale).
     */
    public Date getFirstDateInCalendar(Date dateTarget)
    {
        // Now get the first box on the calendar
        int iFirstDayOfWeek = calendar.getFirstDayOfWeek();
        calendar.setTime(dateTarget);
        int iTargetDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int iOffset = -Math.abs(iTargetDayOfWeek - iFirstDayOfWeek);
        calendar.add(Calendar.DATE, iOffset);

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }
    /**
     * Convert this data to a string (using the supplied format).
     * @param dateTarget The date to convert to a string.
     * @param iDateFormat The format for the date.
     * @return The date as a string.
     */
    public String getDateString(Date dateTarget, int iDateFormat)
    {
        stringBuffer.setLength(0);
        FieldPosition fieldPosition = new FieldPosition(iDateFormat);
        String string = null;
        string = dateFormat.format(dateTarget, stringBuffer, fieldPosition).toString();
        int iBegin = fieldPosition.getBeginIndex();
        int iEnd = fieldPosition.getEndIndex();
        string = string.substring(iBegin, iEnd);
        return string;
    }
    /**
     * Invoked when the mouse button has been clicked (pressed
     * and released) on a component.
     */
    public void mouseClicked(MouseEvent evt)
    {
        JLabel button = (JLabel)evt.getSource();
        int iOffsetDay = Integer.parseInt(button.getName());
        iOffsetDay -= targetComponent;
        calendar.setTime(selectedDate);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int ms = calendar.get(Calendar.MILLISECOND);
        calendar.setTime(targetDate);
        calendar.add(Calendar.DATE, iOffsetDay);
        if (hour == 0)
            if (minute == 0)
            if (second == 0)
            if (ms == 0)
                hour = 12;
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, ms);
        Date date = calendar.getTime();
        JPopupMenu popupMenu = this.getJPopupMenu();
        if (popupMenu != null)
        { // I'm not sure this is the correct code, but here it is!
            Component invoker = popupMenu.getInvoker();
            this.getParent().remove(this);      // Just being careful
            Container container = popupMenu.getParent();
            popupMenu.setVisible(false);
            container.remove(popupMenu);
            if (invoker != null)
                if (transferFocus)
                    invoker.transferFocus();    // Focus on next component after invoker
        }
        Date oldDate = selectedDate;
        if (selectedDate == targetDate)
            oldDate = null;
        this.firePropertyChange(dateParam, oldDate, date);
    }
    /**
     * Invoked when a mouse button has been pressed on a component.
     */
    public void mousePressed(MouseEvent e)
    {
    }
    /**
     * Invoked when a mouse button has been released on a component.
     */
    public void mouseReleased(MouseEvent e)
    {
    }
    private Border oldBorder = EMPTY_BORDER;
    /**
     * Invoked when the mouse enters a component.
     */
    public void mouseEntered(MouseEvent evt)
    {
        JLabel button = (JLabel)evt.getSource();
        oldBorder = button.getBorder();
        button.setBorder(ROLLOVER_BORDER);
    }
    /**
     * Invoked when the mouse exits a component.
     */
    public void mouseExited(MouseEvent evt)
    {
        JLabel button = (JLabel)evt.getSource();
        button.setBorder(oldBorder);
    }
    /**
     * Get the parent popup menu.
     * @return The popup menu.
     */
    private JPopupMenu getJPopupMenu()
    {
        Container parent = this.getParent();
        while (parent != null)
        {
            if (parent instanceof JPopupMenu)
                return (JPopupMenu)parent;
            parent = parent.getParent();
        }
        return null;
    }
    /**
     * Create this calendar in a popup menu and synchronize the text field on change.
     * @param dateTarget The initial date for this button.
     * @param button The calling button.
     */
    public static JCalendarPopup createCalendarPopup(Date dateTarget, Component button)
    {
        return JCalendarPopup.createCalendarPopup(null, dateTarget, button, null);
    }
    /**
     * Create this calendar in a popup menu and synchronize the text field on change.
     * @param strDateParam The name of the date property (defaults to "date").
     * @param dateTarget The initial date for this button.
     * @param button The calling button.
     */
    public static JCalendarPopup createCalendarPopup(String strDateParam, Date dateTarget, Component button)
    {
        return JCalendarPopup.createCalendarPopup(null, dateTarget, button, null);
    }
    /**
     * Create this calendar in a popup menu and synchronize the text field on change.
     * @param strDateParam The name of the date property (defaults to "date").
     * @param dateTarget The initial date for this button.
     * @param strLanguage The language to use.
     * @param button The calling button.
     */
    public static JCalendarPopup createCalendarPopup(String strDateParam, Date dateTarget, Component button, String strLanguage)
    {
        JPopupMenu popup = new JPopupMenu();
        JComponent c = (JComponent)popup; //?.getContentPane();
        c.setLayout(new BorderLayout());
        JCalendarPopup calendar = new JCalendarPopup(strDateParam, dateTarget, strLanguage);
        c.add(calendar, BorderLayout.CENTER);
        popup.show(button, button.getBounds().width, 0);
        return calendar;
    }
    /**
     * Create this calendar in a popup menu and synchronize the text field on change.
     * @param strDateParam The name of the date property (defaults to "date").
     * @param dateTarget The initial date for this button.
     */
    public static JButton createCalendarButton(String strDateParam, Date dateTarget)
    {
        JCalendarButton button = new JCalendarButton(strDateParam, dateTarget);
        button.setMargin(NO_INSETS);
        button.setOpaque(false);

        return button;
    }
    /**
     * Enable/Disable the transfer of focus after selecting date.
     */
    public void setTransferFocus(boolean bTransferFocus)
    {
        transferFocus = bTransferFocus;
    }
}