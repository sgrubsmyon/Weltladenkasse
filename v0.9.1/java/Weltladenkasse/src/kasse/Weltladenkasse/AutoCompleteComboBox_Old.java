package Weltladenkasse;

// original code from: http://www.orbital-computer.de/JComboBox/source/AutoCompletion.java
// (doc at http://www.orbital-computer.de/JComboBox)

import java.util.Vector;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.text.*;

/* This work is hereby released into the Public Domain.
 * To view a copy of the public domain dedication, visit
 * http://creativecommons.org/licenses/publicdomain/
 */
public abstract class AutoCompleteComboBox_Old extends JComboBox {
    ComboBoxModel model;
    JTextComponent editor;
    // flag to indicate if setSelectedItem/setModel has been called
    // subsequent calls to remove/insertString should be ignored
    boolean selecting=false;
    boolean hidePopupOnFocusLoss;
    boolean hitBackspaceOnSelection;
    boolean onlySetText=false;
    
    KeyListener editorKeyListener;
    FocusListener editorFocusListener;
    
    protected Connection conn; // connection to MySQL database
    protected Vector<String> searchCache = new Vector<String>();

    class ACDocument extends PlainDocument {
        public void remove(int offs, int len) throws BadLocationException {
            if (selecting) return; // return immediately when selecting an item
            if (onlySetText){ // only run super method
                super.remove(offs, len);
                return;
            }
            searchCache.clear(); // something was removed from the string => start all over again and cache new search results
            super.remove(offs, len); // this makes the removal and fires removeUpdate
                                    // call this after clearing cearchCache, so that searchCache is up to date for removeUpdate
            doMySQLQuery(editor.getText());
        }

        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            if (selecting) return; // return immediately when selecting an item
            super.insertString(offs, str, a); // insert the string into the document
            if (onlySetText){ // only run super method
                editor.setCaretPosition(editor.getDocument().getLength());
                return;
            }
            doMySQLQuery(editor.getText()); // run MySQL query function here (if 3 or more characters and comboBox empty)
            // lookup and select a matching item
            Object item = lookupItem(editor.getDocument().getText(0, editor.getDocument().getLength()));
            if (item != null) {
                setSelectedItem(item);
            } else {
                // keep old item selected if there is no match
                //item = getSelectedItem();
                // imitate no insert (later on offs will be incremented by str.length(): selection won't move forward)
                offs = offs-str.length();
                // provide feedback to the user that his input has been received but can not be accepted
                getToolkit().beep(); // when available use: UIManager.getLookAndFeel().provideErrorFeedback(this);
            }
            if (item != null) {
                // auto-complete (put text in)
                setText(item.toString());
                // select the completed part
                highlightCompletedText(offs+str.length());
            }
        }
    }

    public AutoCompleteComboBox_Old(Connection conn) {
        super(new DefaultComboBoxModel());
        this.conn = conn;
        this.setEditable(true);
        model = this.getModel();

        this.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!selecting) highlightCompletedText(0);
            }
        });

        this.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals("editor")) configureEditor((ComboBoxEditor) e.getNewValue());
                if (e.getPropertyName().equals("model")) model = (ComboBoxModel) e.getNewValue();
            }
        });

        editorKeyListener = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if ( e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN ){
                    onlySetText = true;
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

        // Highlight whole text when gaining focus
        editorFocusListener = new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                highlightCompletedText(0);
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
        selecting = true;
        this.setModel(model);
        selecting = false;
        setText( searchCache.size() > 0 ? searchCache.firstElement() : "");
    }
    
    public Vector<String> getSearchCache(){
        return searchCache;
    }
    
    public void setSelectedItem(String item){
        selecting = true;
        model.setSelectedItem(item);
        selecting = false;
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
            editor = (JTextComponent) newEditor.getEditorComponent();
            editor.addKeyListener(editorKeyListener);
            editor.addFocusListener(editorFocusListener);
            editor.setDocument(new ACDocument());
        }
    }
    
    protected void setText(String text) {
        try {
            // remove all text and insert the completed string
            onlySetText = true;
            editor.getDocument().remove(0, editor.getDocument().getLength());
            editor.getDocument().insertString(0, text, null);
            onlySetText = false;
        } catch (BadLocationException e) {
            throw new RuntimeException(e.toString());
        }
    }
    
    protected void highlightCompletedText(int start) {
        editor.setCaretPosition(editor.getDocument().getLength());
        editor.moveCaretPosition(start);
    }

    public void emptyBox() {
        searchCache.clear();
        model = new DefaultComboBoxModel(searchCache);
        selecting = true;
        setModel(model);
        selecting = false;
        setText("");
    }
    
    public void collapseToSelectedString(String selStr) {
        searchCache.clear();
        searchCache.add(selStr);
        model = new DefaultComboBoxModel(searchCache);
        selecting = true;
        setModel(model);
        selecting = false;
        setText(searchCache.firstElement());
    }
    
    // only implemented in non-abstract subclasses
    public abstract void doMySQLQuery(String pattern);

    protected Object lookupItem(String pattern) {
        Object selectedItem = model.getSelectedItem();
        // only search for a different item if the currently selected does not match
        if (selectedItem != null && startsWithIgnoreCase(selectedItem.toString(), pattern)) {
            return selectedItem;
        } else {
            // iterate over all items
            for (int i=0, n=model.getSize(); i < n; i++) {
                Object currentItem = model.getElementAt(i);
                // current item starts with the pattern?
                if (currentItem != null && startsWithIgnoreCase(currentItem.toString(), pattern)) {
                    return currentItem;
                }
            }
        }
        // no item starts with the pattern => return null
        return null;
    }
    
    // checks if str1 starts with str2 - ignores case
    protected boolean startsWithIgnoreCase(String str1, String str2) {
        return str1.toUpperCase().startsWith(str2.toUpperCase());
    }
}
