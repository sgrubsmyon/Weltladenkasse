package org.weltladen_bonn.pos;

// basic Java stuff:
import java.util.*;

// MySQL Connector/J stuff:
import java.sql.*;
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.*;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BarcodeComboBox extends IncrementalSearchComboBox {
    private static final Logger logger = LogManager.getLogger(BarcodeComboBox.class);

    // private Connection conn; // connection to MySQL database
    private MariaDbPoolDataSource pool; // pool of connections to MySQL database

    public BarcodeComboBox(MariaDbPoolDataSource pool, String fstr) {
        super(fstr);
        this.pool = pool;
        textFeld.removeKeyListener(keyListener);
        textFeld.addKeyListener(new BarcodeKeyListener());
    }

    public Vector<String[]> doQuery() {
        Vector<String[]> searchResults = new Vector<String[]>();
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT DISTINCT barcode FROM artikel AS a " +
                    "INNER JOIN produktgruppe AS p USING (produktgruppen_id) " +
                    "WHERE barcode LIKE ? AND a.aktiv = TRUE " + filterStr +
                    "ORDER BY barcode"
                    );
            pstmt.setString(1, "%"+textFeld.getText()+"%");
            ResultSet rs = pstmt.executeQuery();
            // Now do something with the ResultSet ...
            while (rs.next()) {
                searchResults.add(new String[]{rs.getString(1)});
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception: {}", ex);
            JOptionPane.showMessageDialog(this,
                "Verbindung zum Datenbank-Server unterbrochen?\n"+
                "Fehlermeldung: "+ex.getMessage(),
                "Fehler", JOptionPane.ERROR_MESSAGE);
        }
        // sort the results
        //Collections.sort(searchResults, new Comparator<String[]>() { // anonymous class for sorting alphabetically ignoring case
        //    public int compare(String[] str1, String[] str2){ return str1[0].compareToIgnoreCase(str2[0]); }
        //});
        return searchResults;
    }

    /**
     *    * Each non abstract class that implements the DocumentListener
     *      must have these methods.
     *
     *    @param e the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
        if (setBoxMode){
            return;
        }
        // clear all items
        clearItemCache();
        setPopupVisible(false);
    }
    public void removeUpdate(DocumentEvent e) {
        insertUpdate(e);
    }
    public void changedUpdate(DocumentEvent e) {
	// Plain text components do not fire these events
    }

    // need a low-level key listener to prevent an input box insertion when up/down key is pressed.
    // enter press puts the selected item into the box and removes all others
    public class BarcodeKeyListener extends KeyAdapter {
        @Override
            public void keyPressed(KeyEvent e) {
                if ( e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_PAGE_DOWN || e.getKeyCode() == KeyEvent.VK_KP_DOWN ||
                        e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_PAGE_UP || e.getKeyCode() == KeyEvent.VK_KP_UP ){
                                        // if up/down/pg up/pg down key was pressed: go into changeMode (don't write into input box)
                    changeMode = true;
                }
                else if ( e.getKeyCode() == KeyEvent.VK_ENTER ){
                                        // unfortunately, when up/down is pressed, the item
                                        // selection already takes place. When user presses enter,
                                        // nothing changes (no new selected item), so the
                                        // ItemListener is not called.
                                        // I finally stopped using ItemListener and use only
                                        // listeners on enter and mouse click.
                    if (items.size() == 0) {
                        incrementalSearch();
                        if (items.size() != 1) {
                            if (getItemCount() > 0) {
                                SwingUtilities.invokeLater(new Runnable(){
                                    public void run() {
                                        setPopupVisible(true);
                                    }
                                });
                            }
                            return; // do not log it in yet
                        }
                    }
                    if (getSelectedIndex() >= 0 && getSelectedIndex() < items.size()){
                        String[] item = items.get(getSelectedIndex());
                        setBox(item);
                        fireActionEvent(); // this is actually not needed, because ENTER key already fired an action event before ("comboBoxEdited")
                    }
                }
            }
        @Override
            public void keyReleased(KeyEvent e) {
                changeMode = false;
            }
    }
}

