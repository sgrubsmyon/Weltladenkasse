package org.weltladen_bonn.pos;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
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
    public JComboBox<String> parentProdGrBox;
    public JTextField nameField;
    public JComboBox<String> mwstBox;
    public JComboBox<String> pfandBox;

    private Vector<String> parentProdGrs;
    public Vector<Integer> parentProdGrIDs;
    private Vector< Vector<Integer> > parentProdGrIDsList;
    private Vector<String> mwsts;
    public Vector<Integer> mwstIDs;
    private Vector<String> pfandNamen;
    public Vector<Integer> pfandIDs;

    // Methoden:
    public ProduktgruppeFormular(Connection conn, MainWindowGrundlage mw) {
	super(conn, mw);

        fillComboBoxes();
    }

    public Vector<Integer> findProdGrIDs(Integer parentProdGrID) {
        Vector<Integer> ids = new Vector<Integer>();
        if (parentProdGrID == null || parentProdGrID < 0){
            ids.add(null); ids.add(null); ids.add(null);
        } else {
            try {
                PreparedStatement pstmt = this.conn.prepareStatement(
                        "SELECT toplevel_id, sub_id, subsub_id FROM produktgruppe "+
                        "WHERE produktgruppen_id = ?"
                        );
                pstmt.setInt(1, parentProdGrID);
                ResultSet rs = pstmt.executeQuery();
                rs.next();
                ids.add( rs.getString(1) != null ? rs.getInt(1) : null );
                ids.add( rs.getString(2) != null ? rs.getInt(2) : null );
                ids.add( rs.getString(3) != null ? rs.getInt(3) : null );
                rs.close();
                pstmt.close();
            } catch (SQLException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        return ids;
    }

    private int currentMaxTopID() {
        int id = 0;
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MAX(toplevel_id) FROM produktgruppe");
            rs.next(); id = rs.getInt(1); rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return id;
    }

    private int currentMaxSubID(Integer topID) {
        int id = 0;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT MAX(sub_id) FROM produktgruppe WHERE toplevel_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, topID);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); id = rs.getInt(1); rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return id;
    }

    private int currentMaxSubsubID(Integer topID, Integer subID) {
        int id = 0;
        try {
            PreparedStatement pstmt = this.conn.prepareStatement(
                    "SELECT MAX(subsub_id) FROM produktgruppe WHERE toplevel_id = ? AND sub_id = ?"
                    );
            pstmtSetInteger(pstmt, 1, topID);
            pstmtSetInteger(pstmt, 2, subID);
            ResultSet rs = pstmt.executeQuery();
            rs.next(); id = rs.getInt(1); rs.close();
            pstmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        return id;
    }

    public Vector<Integer> idsOfNewProdGr(Integer parentProdGrID) {
        Vector<Integer> parentIDs = findProdGrIDs(parentProdGrID);
        Integer topID = parentIDs.get(0);
        Integer subID = parentIDs.get(1);
        Integer subsubID = parentIDs.get(2);
        if (topID == null){
            // get current max toplevel_id and increment by one
            topID = currentMaxTopID() + 1;
            subID = null;
            subsubID = null;
        } else if (subID == null){
            // get current max sub_id and increment by one
            subID = currentMaxSubID(topID) + 1;
            subsubID = null;
        } else {
            // get current max subsub_id and increment by one
            subsubID = currentMaxSubsubID(topID, subID) + 1;
        }
        Vector<Integer> ids = new Vector<Integer>();
        ids.add(topID);
        ids.add(subID);
        ids.add(subsubID);
        return ids;
    }

    public void fillComboBoxes() {
        parentProdGrs = new Vector<String>();
        parentProdGrIDs = new Vector<Integer>();
        parentProdGrIDsList = new Vector< Vector<Integer> >();
        mwsts = new Vector<String>();
        mwstIDs = new Vector<Integer>();
        pfandNamen = new Vector<String>();
        pfandIDs = new Vector<Integer>();

        parentProdGrs.add("Keiner");
        parentProdGrIDs.add(null);
        Vector<Integer> ids = new Vector<Integer>();
        ids.add(null); ids.add(null); ids.add(null);
        parentProdGrIDsList.add(ids);
        pfandNamen.add("Kein Pfand");
        pfandIDs.add(null);
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT produktgruppen_id, toplevel_id, sub_id, subsub_id, produktgruppen_name "+
                    "FROM produktgruppe "+
                    "WHERE toplevel_id IS NOT NULL AND subsub_id IS NULL "+
                    "ORDER BY toplevel_id, sub_id, subsub_id"
                    );
            while (rs.next()) {
                Integer id = rs.getInt(1);
                ids = new Vector<Integer>();
                ids.add( rs.getString(2) == null ? null : rs.getInt(2) );
                ids.add( rs.getString(3) == null ? null : rs.getInt(3) );
                ids.add( rs.getString(4) == null ? null : rs.getInt(4) );
                String name = rs.getString(5);

                parentProdGrIDs.add(id);
                parentProdGrIDsList.add(ids);
                parentProdGrs.add(name);
            }
            rs.close();
            rs = stmt.executeQuery(
                    "SELECT mwst_id, mwst_satz FROM mwst "+
                    "ORDER BY mwst_id"
                    );
            while (rs.next()) {
                Integer id = rs.getInt(1);
                BigDecimal satz = rs.getBigDecimal(2);
                String mwst = bc.vatFormatter(satz);
                mwstIDs.add(id);
                mwsts.add(mwst);
            }
            rs.close();
            rs = stmt.executeQuery(
                    "SELECT pfand_id, artikel_name FROM pfand "+
                    "INNER JOIN artikel USING(artikel_id) ORDER BY pfand_id"
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

            JPanel parentProdGrPanel = new JPanel();
            parentProdGrPanel.setBorder(BorderFactory.createTitledBorder("Untergruppe von"));
            parentProdGrBox = new JComboBox<String>(parentProdGrs);
            parentProdGrBox.setRenderer(new ProduktgruppenIndentedRenderer(parentProdGrIDsList));
            parentProdGrPanel.add(parentProdGrBox);

            JPanel namePanel = new JPanel();
            namePanel.setBorder(BorderFactory.createTitledBorder("Produktgruppen-Name"));
            nameField = new JTextField("");
            nameField.setColumns(30);
            namePanel.add(nameField);

        JPanel describePanel = new JPanel();
        describePanel.add(parentProdGrPanel);
        describePanel.add(namePanel);
        headerPanel.add(describePanel);

            JPanel mwstPanel = new JPanel();// mwstPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            mwstPanel.setBorder(BorderFactory.createTitledBorder("MwSt."));
            mwstBox = new JComboBox<String>(mwsts);
            mwstPanel.add(mwstBox);

            JPanel pfandPanel = new JPanel();
            pfandPanel.setBorder(BorderFactory.createTitledBorder("Pfand"));
            pfandBox = new JComboBox<String>(pfandNamen);
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
