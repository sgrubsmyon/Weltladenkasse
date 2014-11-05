package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
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
import javax.swing.event.*; // for DocumentListener
import javax.swing.text.*; // for DocumentFilter

public class ProduktgruppeFormular extends WindowContent
    implements ProduktgruppeFormularInterface {
    // Attribute:
    public JComboBox mwstBox;
    public JComboBox pfandBox;
    public JTextField nameField;

    private Vector<String> mwsts;
    public Vector<Integer> mwstIDs;
    private Vector<String> pfandNamen;
    public Vector<Integer> pfandIDs;

    // Methoden:
    public ProduktgruppeFormular(Connection conn, MainWindowGrundlage mw) {
	super(conn, mw);

        fillComboBoxes();
    }

    public void fillComboBoxes() {
        mwsts = new Vector<String>();
        mwstIDs = new Vector<Integer>();
        pfandNamen = new Vector<String>();
        pfandIDs = new Vector<Integer>();
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT mwst_id, mwst_satz FROM mwst "+
                    "ORDER BY mwst_id"
                    );
            while (rs.next()) {
                Integer id = rs.getInt(1);
                BigDecimal satz = rs.getBigDecimal(2);
                String mwst = vatFormatter(satz);

                mwstIDs.add(id);
                mwsts.add(mwst);
            }
            rs.close();
            rs = stmt.executeQuery(
                    "SELECT pfand_id, artikel_name FROM pfand "+
                    "INNER JOIN artikel USING(pfand_id) ORDER BY pfand_id"
                    );
            while (rs.next()) {
                Integer id = rs.getInt(1);
                String name = rs.getString(2);

                pfandIDs.add(id);
                pfandNamen.add(name);
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void showHeader(JPanel headerPanel, JPanel allPanel) {
	headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

            JPanel namePanel = new JPanel();
            namePanel.setBorder(BorderFactory.createTitledBorder("Artikelname"));
            nameField = new JTextField("");
            nameField.setColumns(30);
            namePanel.add(nameField);

        JPanel describePanel = new JPanel();
        describePanel.add(namePanel);
        headerPanel.add(describePanel);

            JPanel mwstPanel = new JPanel();// mwstPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            mwstPanel.setBorder(BorderFactory.createTitledBorder("MwSt."));
            mwstBox = new JComboBox(mwsts);
            mwstPanel.add(mwstBox);

            JPanel pfandPanel = new JPanel();
            pfandPanel.setBorder(BorderFactory.createTitledBorder("Pfand"));
            pfandBox = new JComboBox(pfandNamen);
            pfandPanel.add(pfandBox);

        JPanel propertiesPanel = new JPanel(); propertiesPanel.setLayout(new FlowLayout());
        propertiesPanel.add(mwstPanel);
        propertiesPanel.add(pfandPanel);
        headerPanel.add(propertiesPanel);

        allPanel.add(headerPanel);
    }

    public boolean checkIfFormIsComplete() {
        if ( nameField.isEnabled() && nameField.getText().replaceAll("\\s","").equals("") )
            return false;
        return true;
    }

    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
    }
}
