package org.weltladen_bonn.pos;

// basic Java stuff:
import java.util.Vector;

// GUI stuff:
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

// for combobox popup menu listener
import java.lang.reflect.Field;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicComboBoxUI;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class IncrementalSearchComboBox extends JComboBox<String> implements DocumentListener {
    private static final Logger logger = LogManager.getLogger(IncrementalSearchComboBox.class);

    protected JTextComponent textFeld;
    protected String filterStr;
    protected int searchThresh = 3;
    public boolean changeMode = false;
    public boolean setBoxMode = false;
    public Vector<String[]> items;
    protected MyKeyListener keyListener;

// credit to: http://stackoverflow.com/questions/11278209/how-can-i-make-comboboxs-list-wider
    protected Vector<Integer> columnWidths = new Vector<Integer>(3); // support max. 3 columns

    public IncrementalSearchComboBox(String fstr) {
        super();
        this.filterStr = fstr;
        //this.setRenderer(new MonoColRenderer());
        //this.setEditor(new MyComboBoxEditor());
        this.setEditable(true);
        this.addPopupMouseListener(new MyMouseListener());
        textFeld = (JTextComponent)this.getEditor().getEditorComponent();
        keyListener = new MyKeyListener();
        textFeld.addKeyListener(keyListener);
        textFeld.setDocument(new ChangeableDocument());
        textFeld.getDocument().addDocumentListener(this);
        items = new Vector<String[]>();
    }

    public void setFilterStr(String filterStr) {
        this.filterStr = filterStr;
    }

    public void setSearchThreshold(int searchThresh) {
        this.searchThresh = searchThresh;
    }

    protected String parseName(String name) {
        int widthCut = 70;
        int endIndex = name.length() > widthCut ? widthCut : name.length();
        return name.substring(0, endIndex);
    }

    private void resetColumnWidths() {
        columnWidths = new Vector<Integer>(3); // support max. 3 columns
        for (int j=0; j<3; j++) {
            columnWidths.add(0);
        }
    }

    private void getColumnWidths() {
        if (items.size() > 0) {
            Font font = this.getFont();
            FontMetrics metrics = this.getFontMetrics(font);
            for (int i=0; i<items.size(); i++) {
                String[] itm = items.get(i);
                //for (String s : items.get(i)){
                int end = Math.min(3, items.get(0).length); // only consider first 3 items for width
                for (int j=0; j<end; j++) {
                    String s = itm[j];
                    int colWidth = metrics.stringWidth( parseName(s) );
                    columnWidths.set(j, Math.max(columnWidths.get(j), colWidth));
                }
            }
        }
    }

    public void clearItemCache() {
        this.changeMode = true; // prevent "Attempt to mutate in notification" exception
            items.clear();
            resetColumnWidths();
            this.removeAllItems();
        this.changeMode = false;
    }

    public void emptyBox() {
        clearItemCache();
        textFeld.setText("");
    }

    public void setBox(String[] item) {
        clearItemCache();
        this.changeMode = true;
            items.add(item);
            getColumnWidths();
            this.addItem(item[0]);
        this.changeMode = false;
        this.setBoxMode = true;
            textFeld.setText(item[0]);
        this.setBoxMode = false;
        this.setPopupVisible(false);
    }

//    @Override
//        public void showPopup() {
//            super.showPopup();
//            System.out.println("### !!! Delightful showPopup() called!!! Rejoice!!! First Item: "+this.getItemAt(0));
//        }
//
//    @Override
//        public void hidePopup() {
//            super.hidePopup();
//            System.out.println("### !!! Nasty hidePopup() called. First Item: "+this.getItemAt(0));
//        }

    public void setItems(Vector<String[]> istr) {
        clearItemCache();
        if (istr.size() == 1){
            this.setBox(istr.get(0));
            //fireActionEvent(); // needed?
        } else {
            this.changeMode = true;
                for (String[] item : istr){
                    items.add(item);
                    this.addItem(item[0]);
                }
                getColumnWidths();
            this.changeMode = false;
        }
        if ( this.getItemCount() > 1 ){
            this.requestFocus();
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    //getColumnWidths();
                    setPopupVisible(true);
                }
            });
        }
        else {
            this.setPopupVisible(false);
        }
    }

    public abstract Vector<String[]> doQuery();

    // do the incremental search:
    protected void incrementalSearch() {
        this.setPopupVisible(false); // hide popup during editing, so that it will be resized

        // clear all items
        clearItemCache();

        if (textFeld.getText().length() >= searchThresh){
            Vector<String[]> searchResults = doQuery();
            this.changeMode = true; // prevent "Attempt to mutate in notification" exception
            for (String[] item : searchResults){
                items.add(item);
                this.addItem(item[0]);
            }
            getColumnWidths();
            this.changeMode = false;
        }

        if (this.getItemCount() > 0) {
            this.setPopupVisible(true);
        }
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
        incrementalSearch();
    }
    public void removeUpdate(DocumentEvent e) {
        insertUpdate(e);
    }
    public void changedUpdate(DocumentEvent e) {
	// Plain text components do not fire these events
    }

    // need a low-level key listener to prevent an input box insertion when up/down key is pressed.
    // enter press puts the selected item into the box and removes all others
    public class MyKeyListener extends KeyAdapter {
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

    // need a low-level mouse listener to put the item that was clicked on in the box and remove all
    // others
    public class MyMouseListener extends MouseAdapter {
        @Override
            public void mouseReleased(MouseEvent e) {
                if (getSelectedIndex() >= 0 && getSelectedIndex() < items.size()){
                    String[] item = items.get(getSelectedIndex());
                    setBox(item);
                    fireActionEvent(); // this is needed
                }
            }
    }

    // unfortunately, setting a MouseListener on a ComboBox popup is quite complicated:
    // this is from: http://engin-tekin.blogspot.de/2009/10/hrefhttpkfd.html (Thanks to Engin Tekin)
    public void addPopupMouseListener(MouseAdapter ml) {
        try {
            Field popupInBasicComboBoxUI = BasicComboBoxUI.class.getDeclaredField("popup");
            popupInBasicComboBoxUI.setAccessible(true);
            BasicComboPopup popup = (BasicComboPopup) popupInBasicComboBoxUI.get(this.getUI());

            Field scrollerInBasicComboPopup = BasicComboPopup.class.getDeclaredField("scroller");
            scrollerInBasicComboPopup.setAccessible(true);
            JScrollPane scroller = (JScrollPane) scrollerInBasicComboPopup.get(popup);

            scroller.getViewport().getView().addMouseListener(ml);
        }
        catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    // need to sub-class the Document class to have a document that is changeable from the
    // DocumentListener methods (we have a bool to indicate that we return from the insertString
    // etc. functions, before the super methods are invoked, this prevents a
    // "java.lang.IllegalStateException: Attempt to mutate in notification")
    public class ChangeableDocument extends PlainDocument {
        @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                if (changeMode){
                    return;
                }
                super.insertString(offs, str, a); // insert the string into the document
            }
        @Override
            public void remove(int offs, int len) throws BadLocationException {
                if (changeMode){
                    return;
                }
                super.remove(offs, len); // this makes the removal and fires removeUpdate
            }
        @Override
            public void replace(int offs, int len, String text, AttributeSet attrs) throws BadLocationException {
                if (changeMode){
                    return;
                }
                super.replace(offs, len, text, attrs);
            }
    }

    /*
    class MonoColRenderer extends JLabel implements ListCellRenderer {
        public MonoColRenderer() {
            this.setOpaque(true);
        }
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            Color foreground, background;
            if (isSelected) {
                background = new Color(list.getSelectionBackground().getRGB());
                foreground = new Color(list.getSelectionForeground().getRGB());
            } else {
                background = new Color(list.getBackground().getRGB());
                foreground = new Color(list.getForeground().getRGB());
            }
            this.setBackground(background);
            this.setForeground(foreground);

            String[] item = (String[])value;
            this.setText(item[0]);
            return this;
        }
    }

    class MyComboBoxEditor extends JTextField implements ComboBoxEditor {
        public Component getEditorComponent() {
            return this;
        }

        public Object getItem(){
            return this.getText();
        }

        public void setItem(Object anObject){
            if (anObject != null){
                if (anObject instanceof String[]){
                    this.setText( ((String[])anObject)[0] );
                } else {
                    //this.setText(anObject.toString());
                }
            }
        }
    }
    */

}
