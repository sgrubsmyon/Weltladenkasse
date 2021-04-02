package org.weltladen_bonn.pos;

// Basic Java stuff:
//import java.util.*; // for Vector, Collections

// MySQL Connector/J stuff:
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
//import java.awt.BorderLayout;
//import java.awt.FlowLayout;
//import java.awt.Dimension;
import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
import java.awt.event.*;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.BoxLayout;

public abstract class DialogWindow extends WindowContent {
    // Attribute:
    protected JDialog window;
    protected JPanel allPanel;
    protected JPanel headerPanel;
    protected JPanel footerPanel;

    protected JButton closeButton;

    // Methoden:
    public DialogWindow(MariaDbPoolDataSource pool, MainWindowGrundlage mw, JDialog dia) {
        super(pool, mw);
        this.window = dia;
    }

    protected abstract void showHeader();
    protected abstract void showMiddle();
    protected abstract void showFooter();

    protected void showAll() {
        allPanel = new JPanel(new BorderLayout());

        showHeader();
        showMiddle();
        showFooter();

        this.add(allPanel, BorderLayout.CENTER);
    }

    protected void updateAll(){
        this.remove(allPanel);
        this.revalidate();
        showAll();
    }

    protected abstract int submit();

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

    // will data be lost on close?
    protected abstract boolean willDataBeLost();
}
