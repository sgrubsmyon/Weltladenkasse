package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.io.*; // for File
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
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
//import javax.swing.filechooser.*;
import javax.swing.text.html.HTMLDocument;

public class ArtikelExport extends WindowContent {
    // Attribute:
    private final String delimiter = ";";
    private JFileChooser fc;

    // Methoden:
    public ArtikelExport(Connection conn, MainWindowGrundlage mw, Artikelliste pw) {
	super(conn, mw);

        fc = new JFileChooser();
        int returnVal = fc.showSaveDialog(pw);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            File file = fc.getSelectedFile();

            writeCSVToFile(file);

            System.out.println("Opened " + file.getName() + ".");
        } else {
            System.out.println("Open command cancelled by user.");
        }
    }

    void writeCSVToFile(File file) {
        // format of csv file:
        // Produktgruppe;Artikelname;Art.-Nr.;Barcode;VK-Preis;EK-Preis;Variabel;VPE;Lieferant;Herkunft
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        //super.actionPerformed(e);
    }
}
