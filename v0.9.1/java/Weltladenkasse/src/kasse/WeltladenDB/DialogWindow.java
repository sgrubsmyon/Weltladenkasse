package WeltladenDB;

// Basic Java stuff:
//import java.util.*; // for Vector, Collections

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

import javax.swing.JDialog;
import javax.swing.JButton;

public abstract class DialogWindow extends WindowContent {
    // Attribute:
    protected JDialog window;
    protected JButton closeButton;

    // Methoden:
    public DialogWindow(Connection conn, MainWindowGrundlage mw, JDialog dia) {
	super(conn, mw);
        this.window = dia;
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
	if (e.getSource() == closeButton){
            this.window.dispose();
            return;
        }
    }
}
