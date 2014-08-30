/**
 * JTimePopup.java
 *
 * @author Don Corley <don@donandann.com>
 * @version 1.0.0
 */
 
package jcalendarbutton.org;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/** 
 * A JTimePopup is a popup calendar the user can click on to change a date.
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
public class JTimePopup extends JList
        implements ListSelectionListener
{
	private static final long serialVersionUID = 1L;

    /**
     * The date param for this popup.
     */
    protected String m_strDateParam = JCalendarPopup.DATE_PARAM;

    /**
     * Constant - Milliseconds in a day.
     */
    public static final long KMS_IN_A_DAY = 24 * 60 * 60 * 1000;    // Milliseconds in a day
    protected Date targetTime = null;
    protected Date selectedTime = null;
    protected Date timeNow = null;

    protected DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
    protected Calendar calendar = Calendar.getInstance();

    /**
     * Transfer the focus after selecting the date (default = true).
     */
    protected boolean transferFocus = true;
    /**
     * The name of the calendar popup icon.
     */
    public static final String TIME_ICON = "Time";

    /**
     * Creates new form TimePopup.
     */
    public JTimePopup()
    {
        super();
    }
    /**
     * Creates new form TimePopup.
     * @param date The initial date for this button.
     */
    public JTimePopup(Date date)
    {
        this();
        this.init(null, date, null);
    }
    /**
     * Creates new form TimePopup.
     * @param strDateParam The name of the date property (defaults to "date").
     * @param date The initial date for this button.
     */
    public JTimePopup(String strDateParam, Date date)
    {
        this();
        this.init(strDateParam, date, null);
    }
    /**
     * Creates new form TimePopup.
     * @param strDateParam The name of the date property (defaults to "date").
     * @param date The initial date for this button.
     * @param strLanguage The language to use.
     */
    public JTimePopup(String strDateParam, Date date, String strLanguage)
    {
        this();
        this.init(strDateParam, date, strLanguage);
    }
    /**
     * Creates new form TimePopup.
     * @param strDateParam The name of the date property (defaults to "date").
     * @param date The initial date for this button.
     * @param strLanguage The language to use.
     */
    public void init(String strDateParam, Date timeTarget, String strLanguage)
    {
        if (strDateParam != null)
            m_strDateParam = strDateParam;      // Property name

        timeNow = new Date();
        if (timeTarget == null)
            timeTarget = timeNow;
        selectedTime = timeTarget;

        if (strLanguage != null)
        {
            Locale locale = new Locale(strLanguage, "");
            if (locale != null)
            {
                calendar = Calendar.getInstance(locale);
                timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
            }
        }
        targetTime = new Date(timeTarget.getTime());
        this.layoutCalendar(targetTime);
        
        this.addListSelectionListener(this);
    }
    /**
     * Add all the components to this calendar panel.
     * @param dateTarget This date needs to be in the calendar.
     */
    public void layoutCalendar(Date timeTarget)
    {
        calendar.setTime(timeTarget);
        
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
                
        String[] array = new String[24 * 2];
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        int selectedIndex = -1;
        for (int i = 0; i < array.length; i++)
        {
            if (hour == calendar.get(Calendar.HOUR_OF_DAY))
                if (minute == calendar.get(Calendar.MINUTE))
                    selectedIndex = i;
            Date time = calendar.getTime();
            String strTime = timeFormat.format(time);
            array[i] = strTime;
            calendar.add(Calendar.MINUTE, 30);
        }
        DefaultComboBoxModel model = new DefaultComboBoxModel(array);
        this.setVisibleRowCount(10);
        this.setModel(model);
        if (selectedIndex != -1)
            this.setSelectedIndex(selectedIndex);
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
    public static JTimePopup createTimePopup(Date dateTarget, Component button)
    {
        return JTimePopup.createTimePopup(null, dateTarget, button, null);
    }
    /**
     * Create this calendar in a popup menu and synchronize the text field on change.
     * @param strDateParam The name of the date property (defaults to "date").
     * @param dateTarget The initial date for this button.
     * @param button The calling button.
     */
    public static JTimePopup createTimePopup(String strDateParam, Date dateTarget, Component button)
    {
        return JTimePopup.createTimePopup(null, dateTarget, button, null);
    }
    /**
     * Create this calendar in a popup menu and synchronize the text field on change.
     * @param strDateParam The name of the date property (defaults to "date").
     * @param dateTarget The initial date for this button.
     * @param strLanguage The language to use.
     * @param button The calling button.
     */
    public static JTimePopup createTimePopup(String strDateParam, Date dateTarget, Component button, String strLanguage)
    {
        JPopupMenu popup = new JPopupMenu();
        JComponent c = (JComponent)popup; //?.getContentPane();
        c.setLayout(new BorderLayout());
        JTimePopup calendar = new JTimePopup(strDateParam, dateTarget, strLanguage);
        JScrollPane scrollPane = new JScrollPane(calendar);
        if (calendar.getSelectedIndex() != -1)
            calendar.ensureIndexIsVisible(calendar.getSelectedIndex());
        c.add(scrollPane, BorderLayout.CENTER);
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
        JTimeButton button = new JTimeButton(strDateParam, dateTarget);
//        button.setMargin(NO_INSETS);
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
  /** 
   * Called whenever the value of the selection changes.
   * @param e the event that characterizes the change.
   */
  public void valueChanged(ListSelectionEvent e)
  {
      int index = this.getSelectedIndex();
      if (index != -1)
      {
        int hour = index / 2;
        int minute = (int)(((float)index / 2 - hour) * 60);
        calendar.setTime(targetTime);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
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
        Date oldTime = selectedTime;
        if (selectedTime == targetTime)
            oldTime = null;
        this.firePropertyChange(m_strDateParam, oldTime, date);
      }
  }
}