package org.weltladen_bonn.pos;

// Basic Java stuff:
import java.util.*; // for Vector
import java.text.*; // for NumberFormat, DecimalFormat

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.mariadb.jdbc.MariaDbPoolDataSource;

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

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ProduktgruppenbaumGrundlage extends WindowContent implements TreeSelectionListener {
    // Attribute:
    private static final Logger logger = LogManager.getLogger(ProduktgruppenbaumGrundlage.class);

    protected final String titleStr = "Produktgruppen";
    private String aktivFilterStr = " AND aktiv = TRUE ";

    // The panels
    protected JPanel groupListPanel;
    // The table holding the article list
    protected JTree tree;

    // Methoden:

    /**
     *    The constructor.
     *       */
    public ProduktgruppenbaumGrundlage(MariaDbPoolDataSource pool, MainWindowGrundlage mw) {
	    super(pool, mw);
	    showTree(titleStr);
    }

    public JPanel getGroupListPanel() { return groupListPanel; }
    public JTree getTree() { return tree; }

    public void setGroupListPanel(JPanel pa) { groupListPanel = pa; }
    public void setTree(JTree tr) { tree = tr; }

    void showTree(String titleStr) {
	groupListPanel = new JPanel();
	groupListPanel.setLayout(new BoxLayout(groupListPanel, BoxLayout.Y_AXIS));
	groupListPanel.setBorder(BorderFactory.createTitledBorder(titleStr));

        Integer artikelCount = returnTotalArticleCount(); // how many artikel are there in total?
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(
                new Gruppe(null,null,null, "Alle Artikel ("+artikelCount+")"));
        addProduktgruppenToRootNode(rootNode);
        tree = new JTree(rootNode);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION); // set the tree so that only one node is selectable
        tree.addTreeSelectionListener(this);

	JScrollPane scrollPane = new JScrollPane(tree);
//	scrollPane.setBounds(30,30,200,150);
	groupListPanel.add(scrollPane);

	this.add(groupListPanel, BorderLayout.CENTER);
    }

    protected class Gruppe {
        public Integer toplevel_id;
        public Integer sub_id;
        public Integer subsub_id;
        public String name;

        public Gruppe(Integer tid, Integer sid, Integer ssid, String gname) {
            toplevel_id = tid; sub_id = sid; subsub_id = ssid;
            name = gname;
        }

        public String toString() {
            return name;
        }
    }

    void addProduktgruppenToRootNode(DefaultMutableTreeNode rootNode) {
        DefaultMutableTreeNode groupNode = null;
        DefaultMutableTreeNode subgroupNode = null;
        DefaultMutableTreeNode subsubgroupNode = null;
        Integer artikelCount, topid, subid, subsubid;
        String groupname;

        Vector< Vector<String> > toplevelgroups = queryTopGruppen();
        for ( Vector<String> group : toplevelgroups ){
            topid = group.get(0) == null ? null : Integer.parseInt(group.get(0));
            subid = group.get(1) == null ? null : Integer.parseInt(group.get(1));
            subsubid = group.get(2) == null ? null : Integer.parseInt(group.get(2));
            groupname = group.get(3);
            try {
                artikelCount = Integer.parseInt(group.get(4)); // how many artikel are there in this gruppe?
            } catch (NumberFormatException ex) {
                logger.error("Exception:", ex);
                artikelCount = null;
            }
            groupNode = new DefaultMutableTreeNode(new Gruppe(topid, subid, subsubid, groupname+" ("+artikelCount+")"));
            Vector< Vector<String> > subgroups = querySubGruppen(topid);
            for ( Vector<String> subgroup : subgroups ){
                topid = subgroup.get(0) == null ? null : Integer.parseInt(subgroup.get(0));
                subid = subgroup.get(1) == null ? null : Integer.parseInt(subgroup.get(1));
                subsubid = subgroup.get(2) == null ? null : Integer.parseInt(subgroup.get(2));
                groupname = subgroup.get(3);
                try {
                    artikelCount = Integer.parseInt(subgroup.get(4));
                } catch (NumberFormatException ex) {
                    logger.error("Exception:", ex);
                    artikelCount = null;
                }
                subgroupNode = new DefaultMutableTreeNode(new Gruppe(topid, subid, subsubid, groupname+" ("+artikelCount+")"));
                Vector< Vector<String> > subsubgroups = querySubSubGruppen(topid, subid);
                for ( Vector<String> subsubgroup : subsubgroups ){
                    topid = subsubgroup.get(0) == null ? null : Integer.parseInt(subsubgroup.get(0));
                    subid = subsubgroup.get(1) == null ? null : Integer.parseInt(subsubgroup.get(1));
                    subsubid = subsubgroup.get(2) == null ? null : Integer.parseInt(subsubgroup.get(2));
                    groupname = subsubgroup.get(3);
                    try {
                        artikelCount = Integer.parseInt(subsubgroup.get(4));
                    } catch (NumberFormatException ex) {
                        logger.error("Exception:", ex);
                        artikelCount = null;
                    }
                    subsubgroupNode = new DefaultMutableTreeNode(new Gruppe(topid, subid, subsubid, groupname+" ("+artikelCount+")"));
                    subgroupNode.add(subsubgroupNode);
                }
                groupNode.add(subgroupNode);
            }
            rootNode.add(groupNode);
        }
    }

    protected void updateTree(String titleStr) {
	this.remove(groupListPanel);
	this.revalidate();
	showTree(titleStr);
    }

    protected void updateTree() {
        updateTree(this.titleStr);
    }

    //////////////////////////
    // DB Query Functions
    //////////////////////////

    private Vector< Vector<String> > queryTopGruppen() { // select top level nodes in tree
        Vector< Vector<String> > produktgruppen = new Vector< Vector<String> >();
	try {
        Connection connection = this.pool.getConnection();
	    // Create statement for MySQL database
	    Statement stmt = connection.createStatement();
	    // Run MySQL command
	    ResultSet rs = stmt.executeQuery(
		    "SELECT toplevel_id, sub_id, subsub_id, produktgruppen_name, n_artikel_rekursiv "+
                    "FROM produktgruppe " +
                    "WHERE toplevel_id IS NOT NULL AND sub_id IS NULL " + aktivFilterStr +
                    "ORDER BY toplevel_id"
		    );
	    // Now do something with the ResultSet ...
	    while (rs.next()){
                Vector<String> rowVector = new Vector<String>();
                rowVector.add(rs.getString(1));
                rowVector.add(rs.getString(2));
                rowVector.add(rs.getString(3));
                rowVector.add(rs.getString(4));
                rowVector.add(rs.getString(5));
                produktgruppen.add(rowVector);
            }
	    rs.close();
	    stmt.close();
        connection.close();
	} catch (SQLException ex) {
	    logger.error("Exception:", ex);
        showDBErrorDialog(ex.getMessage());
	}
        return produktgruppen;
    }

    private Vector< Vector<String> > querySubGruppen(Integer topid) { // select second level nodes in tree
        Vector< Vector<String> > subgruppen = new Vector< Vector<String> >();
	try {
        Connection connection = this.pool.getConnection();
        PreparedStatement pstmt = connection.prepareStatement(
          "SELECT toplevel_id, sub_id, subsub_id, produktgruppen_name, n_artikel_rekursiv "+
          "FROM produktgruppe " +
          "WHERE toplevel_id = ? " +
          "AND sub_id IS NOT NULL AND subsub_id IS NULL " + aktivFilterStr +
          "ORDER BY sub_id"
        );
        pstmtSetInteger(pstmt, 1, topid);
	    ResultSet rs = pstmt.executeQuery();
	    // Now do something with the ResultSet ...
	    while (rs.next()){
            Vector<String> rowVector = new Vector<String>();
            rowVector.add(rs.getString(1));
            rowVector.add(rs.getString(2));
            rowVector.add(rs.getString(3));
            rowVector.add(rs.getString(4));
            rowVector.add(rs.getString(5));
            subgruppen.add(rowVector);
        }
	    rs.close();
	    pstmt.close();
        connection.close();
	} catch (SQLException ex) {
	    logger.error("Exception:", ex);
        showDBErrorDialog(ex.getMessage());
	}
        return subgruppen;
    }

    private Vector< Vector<String> > querySubSubGruppen(Integer topid, Integer subid) { // select third level nodes in tree
        Vector< Vector<String> > subsubgruppen = new Vector< Vector<String> >();
	try {
        Connection connection = this.pool.getConnection();
        PreparedStatement pstmt = connection.prepareStatement(
            "SELECT toplevel_id, sub_id, subsub_id, produktgruppen_name, n_artikel_rekursiv "+
            "FROM produktgruppe " +
            "WHERE toplevel_id = ? " +
            "AND sub_id = ? AND subsub_id IS NOT NULL " + aktivFilterStr +
            "ORDER BY subsub_id"
        );
        pstmtSetInteger(pstmt, 1, topid);
        pstmtSetInteger(pstmt, 2, subid);
	    ResultSet rs = pstmt.executeQuery();
	    // Now do something with the ResultSet ...
	    while (rs.next()){
            Vector<String> rowVector = new Vector<String>();
            rowVector.add(rs.getString(1));
            rowVector.add(rs.getString(2));
            rowVector.add(rs.getString(3));
            rowVector.add(rs.getString(4));
            rowVector.add(rs.getString(5));
            subsubgruppen.add(rowVector);
        }
	    rs.close();
	    pstmt.close();
        connection.close();
	} catch (SQLException ex) {
	    logger.error("Exception:", ex);
        showDBErrorDialog(ex.getMessage());
	}
        return subsubgruppen;
    }

    private Integer returnTotalArticleCount() {
        Integer artikelCount = Integer.valueOf(0);
        try {
            Connection connection = this.pool.getConnection();
            // Create statement for MySQL database
            Statement stmt = connection.createStatement();
            // Run MySQL command
            ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM artikel INNER JOIN produktgruppe " +
                    "USING (produktgruppen_id) " +
                    "WHERE artikel.aktiv = TRUE AND produktgruppe.toplevel_id > 0"
                    );
            // Now do something with the ResultSet ...
            rs.next();
            artikelCount = rs.getInt(1);
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
        return artikelCount;
    }


    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public abstract void actionPerformed(ActionEvent e);

    /** Required by TreeSelectionListener interface. */
    public abstract void valueChanged(TreeSelectionEvent e);

}
