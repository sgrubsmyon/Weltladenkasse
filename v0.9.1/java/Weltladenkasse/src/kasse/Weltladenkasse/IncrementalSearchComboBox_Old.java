package Weltladenkasse;

// original code from: http://www.orbital-computer.de/JComboBox/source/AutoCompletion.java
// (doc at http://www.orbital-computer.de/JComboBox)

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;
import java.util.HashSet;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

/* This work is hereby released into the Public Domain.
 * To view a copy of the public domain dedication, visit
 * http://creativecommons.org/licenses/publicdomain/
 */
public class IncrementalSearchComboBox_Old extends JComboBox {
    ComboBoxModel model;
    JTextComponent editor;
    private String internalFilterStr = " AND (toplevel_id IS NOT NULL OR sub_id = 2) "; 
              // show all 'normal' items (toplevel_id IS NOT NULL), and in addition Gutscheine (where toplevel_id is NULL and sub_id is 2)
    // flag to indicate if setModel has been called
    // subsequent calls to remove/insertString should be ignored
    boolean setModelBool=false;
    boolean selectionCompleteBool=false;
    boolean hidePopupOnFocusLoss;
    boolean keepText=false;
    boolean onlySetText=false;
    boolean hitUpDownArrow=false;
    
    KeyListener editorKeyListener;
    FocusListener editorFocusListener;
    
    private Connection conn; // connection to MySQL database
    private Vector<String> searchCache = new Vector<String>();

    class ISDocument extends PlainDocument {
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            if (setModelBool || selectionCompleteBool) return; // return immediately when selecting an item (prevents infinite loop) or when selection complete
            super.insertString(offs, str, a); // insert the string into the document
            if (!onlySetText){ // don't do incrementalSearch() when not appropriate
                if (offs > 0 && offs < editor.getDocument().getLength()-1) // something was inserted not at the end => start all over again
                    searchCache.clear();
                keepText = false;
                incrementalSearch();
            }
        }

        public void remove(int offs, int len) throws BadLocationException {
            if (setModelBool) return; // return immediately when setModelBool an item (prevents infinite loop)
            if (onlySetText){ // only run super method, don't run incrementalSearch
                super.remove(offs, len);
                return;
            }
            searchCache.clear(); // something was removed from the string => start all over again and cache new search results
            super.remove(offs, len); // this makes the removal and fires removeUpdate
                                    // call this after clearing cearchCache, so that searchCache is up to date for removeUpdate
            keepText = true;
            selectionCompleteBool = false;
            incrementalSearch();
        }
    }

    public IncrementalSearchComboBox_Old(Connection conn) {
        super(new DefaultComboBoxModel());
        this.conn = conn;
        this.setEditable(true);
        model = this.getModel();

        editorKeyListener = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if ( e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN ){ // listen if up/down arrow key was pressed
                    onlySetText = true;
                    selectionCompleteBool = false;
                }
                else {
                    onlySetText = false;
                }
                if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
                    String selectedString = editor.getText();
                    // only enter selStr if one of the strings in serchCache match the selStr: (don't enter bullshit,
                    // prevent exceptions)
                    for (String s : searchCache){
                        if ( selectedString.equals(s) ){ 
                            collapseToSelectedString(selectedString);
                            return;
                        }
                    }
                    // if selStr has not been found in searchCache, the user has been in error
                    // => empty box
                    emptyBox();
                }
            }
        };

        // Bug 5100422 on Java 1.5: Editable JComboBox won't hide popup when tabbing out
        hidePopupOnFocusLoss=System.getProperty("java.version").startsWith("1.5");

        editorFocusListener = new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (isDisplayable()){ 
                    setPopupVisible(true);
                    showPopup();
                }
            }
            public void focusLost(FocusEvent e) {
                // Workaround for Bug 5100422 - Hide Popup on focus loss
                if (hidePopupOnFocusLoss) setPopupVisible(false);
            }
        };

        configureEditor(this.getEditor());
    }

    public void setConnection(Connection conn){
        this.conn = conn;
    }
    
    public void setSearchCache(Vector<String> cache){
        this.searchCache = cache;
        model = new DefaultComboBoxModel(searchCache);
        setModelBool = true;
        this.setModel(model);
        setModelBool = false;
        setText( searchCache.size() > 0 ? searchCache.firstElement() : "");
    }
    
    public Vector<String> getSearchCache(){
        return searchCache;
    }
    
    public int getNumberOfSearchCacheStrings(){
        return searchCache.size();
    }
    
    public JTextComponent getEditorComponent(){
        return editor;
    }
    
    void configureEditor(ComboBoxEditor newEditor) {
        if (editor != null) {
            editor.removeKeyListener(editorKeyListener);
            editor.removeFocusListener(editorFocusListener);
        }
        
        if (newEditor != null) {
            editor = (JTextComponent)newEditor.getEditorComponent();
            editor.addKeyListener(editorKeyListener);
            editor.addFocusListener(editorFocusListener);
            editor.setDocument(new ISDocument());
        }
    }

    public void emptyBox() {
        searchCache.clear();
        model = new DefaultComboBoxModel(searchCache);
        setModelBool = true;
        setModel(model);
        setModelBool = false;
        setText("");
    }

    public void collapseToSelectedString(String selStr) {
        searchCache.clear();
        searchCache.add(selStr);
        model = new DefaultComboBoxModel(searchCache);
        setModelBool = true;
        setModel(model);
        setModelBool = false;
        setText(searchCache.firstElement());
        selectionCompleteBool = true; // block input to box
    }

    void incrementalSearch(){
        String typeText = editor.getText();
        if ( searchCache.size() == 0 && typeText.length() >= 3 ){
            // cache the search results (do mysql query)
            try {
                PreparedStatement pstmt = this.conn.prepareStatement(
                        "SELECT artikel_name FROM artikel AS a " +
                        "INNER JOIN produktgruppe AS p USING (produktgruppen_id) " +
                        "WHERE artikel_name LIKE ? AND a.aktiv = TRUE " + internalFilterStr
                        );
                pstmt.setString(1, "%"+typeText.replaceAll(" ","%")+"%");
                ResultSet rs = pstmt.executeQuery();
                // Now do something with the ResultSet ...
                while (rs.next()) { searchCache.add(rs.getString(1)); }
                rs.close();
                pstmt.close();
            } catch (SQLException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
            Collections.sort(searchCache, new Comparator<String>() { // anonymous class for sorting alphabetically ignoring case
                public int compare(String str1, String str2){ return str1.compareToIgnoreCase(str2); }
            });
        }
        else if ( searchCache.size() > 0 ){
            // search results already cached (no need for db query), eliminate results that don't fit anymore
            String[] searchStrings = typeText.split(" ");
            HashSet<String> toRemove = new HashSet<String>();
            for (String s : searchCache){
                for (int i=0; i<searchStrings.length; i++){
                    if ( !s.toLowerCase().contains(searchStrings[i].toLowerCase()) ){
                        toRemove.add(s);
                        break;
                    }
                }
            }
            for (String s : toRemove){ searchCache.remove(s); }
        }
        // fill combo box with new items
        Vector<String> newModelVector = new Vector<String>(searchCache);
        if (searchCache.size() != 1 || keepText) // wenn Ergebnis nicht eindeutig oder beim Löschen => behalte getippten Text bei
            newModelVector.add(0, typeText); // add typed text at front
        model = new DefaultComboBoxModel(newModelVector);
        setModelBool = true;
        setModel(model);
        setModelBool = false;
        setText(newModelVector.firstElement()); // setze Name in Feld
        if (searchCache.size() == 1 && newModelVector.size() == 1) // wenn Ergebnis eindeutig => blockiere Input ins Feld
            selectionCompleteBool = true; // block input to box
        showPopup();
    }

    private void setText(String text) {
        try {
            // remove all text and insert the completed string
            onlySetText = true;
            selectionCompleteBool = false; // to allow text insertion
            editor.getDocument().remove(0, editor.getDocument().getLength());
            editor.getDocument().insertString(0, text, null);
            onlySetText = false;
        } catch (BadLocationException e) {
            throw new RuntimeException(e.toString());
        }
    }
    
}
