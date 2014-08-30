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
    
    private JPanel topPanel = new JPanel(); // The top panel which holds main navigation bar.

    // Buttons:
    // Buttons for top navigation panel:
    private JButton kassierenButton = new JButton("Kassieren");
    private JButton rechnungenButton = new JButton("Rechnungen");
    private JButton abrechnungenButton = new JButton("Abrechnungen");
    private JButton kassenstandButton = new JButton("Kassenstand");
    private JButton optionenButton = new JButton("Optionen");
    private JButton statistikenButton = new JButton("Statistiken");
    private JButton beendenButton = new JButton("Beenden");

    //***************************************************
    // Methods
    //***************************************************

    /**
     *    The constructor.
     *       */
    public MainWindow(String password){
        super(password);
        setTopPanel();
    }

    public void setTopPanel(){
	topPanel.setLayout(new FlowLayout());
	kassierenButton.addActionListener(this);
	rechnungenButton.addActionListener(this);
	abrechnungenButton.addActionListener(this);
	kassenstandButton.addActionListener(this);
	optionenButton.addActionListener(this);
	statistikenButton.addActionListener(this);
	beendenButton.addActionListener(this);
	topPanel.add(kassierenButton);
	topPanel.add(rechnungenButton);
	topPanel.add(abrechnungenButton);
	topPanel.add(kassenstandButton);
	topPanel.add(optionenButton);
	topPanel.add(statistikenButton);
	topPanel.add(beendenButton);

	holdAll.add(topPanel, BorderLayout.NORTH);
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e){
	if (e.getSource() == kassierenButton){
	    Kassieren myKassieren = new Kassieren(this.conn, this);
	    changeContentPanel(myKassieren);
	    return;
	}
	if (e.getSource() == rechnungenButton){
	    HeutigeRechnungen myRech = new HeutigeRechnungen(this.conn, this);
	    changeContentPanel(myRech);
	    return;
	}
	if (e.getSource() == abrechnungenButton){
	    AbrechnungenTag myAbrech = new AbrechnungenTag(this.conn, this);
	    changeContentPanel(myAbrech);
	    return;
	}
	if (e.getSource() == kassenstandButton){
	    Kassenstand myKassenstand = new Kassenstand(this.conn, this);
	    changeContentPanel(myKassenstand);
	    return;
	}
	if (e.getSource() == optionenButton){
	    OptionTabbedPane myOptPane = new OptionTabbedPane(this.conn, this);
	    changeContentPanel(myOptPane);
	    return;
	}
	if (e.getSource() == beendenButton){
	    int answer = JOptionPane.showConfirmDialog(this,
		    "Programm beenden?", "Beenden",
		    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
	    if (answer == JOptionPane.YES_OPTION)
                System.exit(0);
	    return;
	}
    }
}
