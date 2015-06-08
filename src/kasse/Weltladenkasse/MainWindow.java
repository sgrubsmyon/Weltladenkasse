package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
//import java.sql.*;

// GUI stuff:
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import WeltladenDB.MainWindowGrundlage;

// Class holding the window of the application GUI:
public class MainWindow extends MainWindowGrundlage implements ActionListener {
    //***************************************************
    // Members
    //***************************************************
    TabbedPane myTabbedPane;

    // class to talk to Kundendisplay
    private Kundendisplay display;

    private JButton beendenButton = new JButton("Beenden");

    //***************************************************
    // Methods
    //***************************************************

    /**
     *    The constructor.
     *       */
    public MainWindow(String password){
        super(password);

        display = new Kundendisplay(bc);

        if (connectionWorks){
            myTabbedPane = new TabbedPane(this.conn, this);
            setContentPanel(myTabbedPane);
        }
	//topPanel.setLayout(new FlowLayout());
	//beendenButton.addActionListener(this);
	//topPanel.add(beendenButton);
	//holdAll.add(topPanel, BorderLayout.NORTH);
    }

    @Override
    public void dispose() {
        if (display != null)
            display.closeDevice();
        super.dispose();
    }


    public Kundendisplay getDisplay() {
        return display;
    }


    public boolean isThereIncompleteAbrechnungTag() {
        return myTabbedPane.isThereIncompleteAbrechnungTag();
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e){
	//if (e.getSource() == beendenButton){
	//    int answer = JOptionPane.showConfirmDialog(this,
	//	    "Programm beenden?", "Beenden",
	//	    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
	//    if (answer == JOptionPane.YES_OPTION)
        //        System.exit(0);
	//    return;
	//}
    }
}
