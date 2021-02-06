/**
 * CalendarButton.java
 *
 * @author Don Corley <don@donandann.com>
 * @version 1.0.0
 */
 
package org.weltladen_bonn.pos.jcalendarbutton;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.util.Date;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

/** 
 * A JCalendarButton is a button that displays a popup calendar (A JCalendarPopup).
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
public class JCalendarButton extends JButton
    implements PropertyChangeListener, ActionListener {
    private static final long serialVersionUID = 1L;

    /**
     * The language property key.
     */
    public static final String LANGUAGE_PARAM = "language";
    /**
     * The name of the date property (defaults to "date").
     */
    String dateParam = JCalendarPopup.DATE_PARAM;
    /**
     * The initial date for this button.
     */
    Date targetDate = null;
    /**
     * The language to use.
     */
    String languageString = null;
    
    /**
     * Creates new CalendarButton.
     */
    public JCalendarButton()
    {
        super();
        // Create icons
        try   {
            Icon icon  = new ImageIcon( JCalendarButton.class.getResource("/resources/icons/Calendar.gif") );
            this.setIcon(icon);
        } catch (Exception ex)  {
            this.setText("Date");
        }
        
        this.setName("JCalendarButton");

        this.setMargin(JCalendarPopup.NO_INSETS);
        this.setOpaque(false);
        
        this.addActionListener(this);

        // default to today's date
        Date currentDate = new Date();
        this.init(null, currentDate, null);
    }
    /**
     * Creates new CalendarButton.
     * @param dateTarget The initial date for this button.
     */
    public JCalendarButton(Date dateTarget)
    {
        this();
        this.init(null, dateTarget, null);
    }
    /**
     * Creates new CalendarButton.
     * @param strDateParam The name of the date property (defaults to "date").
     * @param dateTarget The initial date for this button.
     */
    public JCalendarButton(String strDateParam, Date dateTarget)
    {
    	this();
        this.init(strDateParam, dateTarget, null);
    }
    /**
     * Creates new CalendarButton.
     * @param strDateParam The name of the date property (defaults to "date").
     * @param dateTarget The initial date for this button.
     * @param strLanguage The language to use.
     */
    public JCalendarButton(String strDateParam, Date dateTarget, String strLanguage)
    {
    	this();
        this.init(strDateParam, dateTarget, strLanguage);
    }
    /**
     * Creates new CalendarButton.
     * @param strDateParam The name of the date property (defaults to "date").
     * @param dateTarget The initial date for this button.
     * @param strLanguage The language to use.
     */
    public void init(String strDateParam, Date dateTarget, String strLanguage)
    {
        if (strDateParam == null)
            strDateParam = JCalendarPopup.DATE_PARAM;
        dateParam = strDateParam; 
        targetDate = dateTarget;
        languageString = strLanguage;
    }
    /**
     * Get the current date.
     */
    public Date getTargetDate()
    {
        return targetDate;
    }
    /**
     * Set the current date.
     */
    public void setTargetDate(Date dateTarget)
    {
        targetDate = dateTarget;
	this.fireStateChanged();
    }
    /**
     * Get the name of the date property for this button.
     */
    public String getDateParam()
    {
        return dateParam;
    }
    /**
     * Get the name of the date property for this button.
     */
    public void setDateParam(String newParam)
    {
        dateParam = newParam;
    }
    /**
     * Get the language.
     * @return
     */
    public String getLanguage()
    {
		return languageString;
	}
    /**
     * Set the language.
     * @param languageString
     */
	public void setLanguage(String languageString)
	{
		this.languageString = languageString;
	}
    /**
     * The user pressed the button, display the JCalendarPopup.
     * @param e The ActionEvent.
     */
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == this)
        {
            Date dateTarget = this.getTargetDate();
            JCalendarPopup popup = JCalendarPopup.createCalendarPopup(this.getDateParam(), dateTarget, this, languageString);
            popup.addPropertyChangeListener(this);
        }
    }
    /**
     * Propagate the change to my listeners.
     * Watch for date and language changes, so I can keep up to date.
     * @param evt The property change event.
     */
    public void propertyChange(final java.beans.PropertyChangeEvent evt)
    {
        this.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
        if (dateParam.equalsIgnoreCase(evt.getPropertyName()))
            if (evt.getNewValue() instanceof Date){
                targetDate = (Date)evt.getNewValue();
		this.fireStateChanged();
	    }
        if (LANGUAGE_PARAM.equalsIgnoreCase(evt.getPropertyName()))
            if (evt.getNewValue() instanceof String)
        {
            languageString = (String)evt.getNewValue();
        }
    }
}
