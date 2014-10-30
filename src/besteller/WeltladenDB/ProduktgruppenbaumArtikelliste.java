package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector
import java.text.*; // for NumberFormat, DecimalFormat

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
//import javax.swing.JTextArea;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.*;

public class ProduktgruppenbaumArtikelliste extends ProduktgruppenbaumGrundlage {
    // Attribute:
    private ArtikellisteContainer container;

    // Methoden:

    /**
     *    The constructor.
     *       */
    public ProduktgruppenbaumArtikelliste(Connection conn, MainWindowGrundlage mw, ArtikellisteContainer ac) {
	super(conn, mw);

        container = ac;
    }
    //public ProduktgruppenbaumArtikelliste( ProduktgruppenbaumArtikelliste rhs ) {
    //    super(rhs);
    //    container = rhs.getContainer();
    //}

    public ArtikellisteContainer getContainer() {
        return container;
    }

    public void setContainer(ArtikellisteContainer ac) {
        container = ac;
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e){
    }

    /** Required by TreeSelectionListener interface. */
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node == null) return;
        Object nodeObject = node.getUserObject();
        if ( nodeObject instanceof Gruppe ){ // one of the product groups selected
            Gruppe gruppe = (Gruppe) nodeObject;
            Integer topid = gruppe.toplevel_id;
            Integer subid = gruppe.sub_id;
            Integer subsubid = gruppe.subsub_id;
            String gruppenname = gruppe.name;
            container.switchToArtikelliste(topid, subid, subsubid, gruppenname);
        }
    }
}
