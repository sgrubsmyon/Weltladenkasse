/**
 * TimeButton.java

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
 * A JTimeButton is a button that displays a popup time (A JTimePopup).
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
public class JTimeButton extends JButton
    implements PropertyChangeListener, ActionListener
{
	private static final long serialVersionUID = 1L;

	/**
     * The language property key.
     */
    public static final String LANGUAGE_PARAM = "language";
    /**
     * The name of the date property (defaults to "time").
     */
    String timeParam = JCalendarPopup.DATE_PARAM;
    /**
     * The initial date for this button.
     */
    Date targetTime = null;
    /**
     * The language to use.
     */
    String languageString = null;
    
	/**
     * Creates new TimeButton.
     */
    public JTimeButton()
    {
        super();
        // Get current classloader
        ClassLoader cl = this.getClass().getClassLoader();
        // Create icons
        try   {
            Icon icon  = new ImageIcon(cl.getResource("images/buttons/" + JTimePopup.TIME_ICON + ".gif"));
            this.setIcon(icon);
        } catch (Exception ex)  {
            this.setText("change");
        }

        this.setMargin(JCalendarPopup.NO_INSETS);
        this.setOpaque(false);
        
        this.addActionListener(this);
    }
    /**
     * Creates new TimeButton.
     * @param dateTarget The initial date for this button.
     */
    public JTimeButton(Date timeTarget)
    {
    	this();
        this.init(null, timeTarget, null);
    }
    /**
     * Creates new TimeButton.
     * @param strDateParam The name of the date property (defaults to 'date').
     * @param dateTarget The initial date for this button.
     */
    public JTimeButton(String strDateParam, Date timeTarget)
    {
    	this();
        this.init(strDateParam, timeTarget, null);
    }
    /**
     * Creates new TimeButton.
     * @param strDateParam The name of the date property (defaults to 'date').
     * @param dateTarget The initial date for this button.
     * @param strLanguage The language to use.
     */
    public JTimeButton(String strDateParam, Date timeTarget, String strLanguage)
    {
    	this();
        this.init(strDateParam, timeTarget, strLanguage);
    }
    /**
     * Creates new TimeButton.
     * @param strDateParam The name of the date property (defaults to 'date').
     * @param dateTarget The initial date for this button.
     * @param language The language to use.
     */
    public void init(String param, Date time, String language)
    {
        if (param == null)
            param = JCalendarPopup.DATE_PARAM;
        timeParam = param; 
        targetTime = time;
        languageString = language;
        
        this.setName("JTimeButton");
    }
    /**
     * Set the current date.
     */
    public void setTargetDate(Date time)
    {
        targetTime = time;
    }
    /**
     * Get the current date.
     */
    public Date getTargetDate()
    {
        return targetTime;
    }
    /**
     * Get the name of the date property for this button.
     */
    public String getTimeParam()
    {
        return timeParam;
    }
    /**
     * Get the name of the date property for this button.
     */
    public void setTimeParam(String timeParam)
    {
        this.timeParam = timeParam;
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
     * The user pressed the button, display the JTimePopup.
     * @param e The ActionEvent.
     */
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == this)
        {
            Date dateTarget = this.getTargetDate();
            JTimePopup popup = JTimePopup.createTimePopup(this.getTimeParam(), dateTarget, this, languageString);
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
        if (timeParam.equalsIgnoreCase(evt.getPropertyName()))
            if (evt.getNewValue() instanceof Date)
                targetTime = (Date)evt.getNewValue();
        if (LANGUAGE_PARAM.equalsIgnoreCase(evt.getPropertyName()))
            if (evt.getNewValue() instanceof String)
        {
                languageString = (String)evt.getNewValue();
        }
    }
}