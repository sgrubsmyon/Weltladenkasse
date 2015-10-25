package org.weltladen_bonn.pos;

// basic Java stuff:
import java.util.Vector;

// GUI stuff:
import java.awt.Component;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.SwingUtilities;

// for combobox popup menu listener
import java.lang.reflect.Field;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicComboBoxUI;

public abstract class IncrementalSearchComboBox extends JComboBox<String> implements DocumentListener {
    protected JTextComponent textFeld;
    protected String filterStr;
    public boolean changeMode = false;
    public boolean setBoxMode = false;
    public Vector<String[]> items;

// credit to: http://stackoverflow.com/questions/11278209/how-can-i-make-comboboxs-list-wider
/********************* Start Insert */
    protected boolean layingOut = false;
    protected int widestLength = 0;
/********************* Stop Insert */

    public IncrementalSearchComboBox(String fstr) {
        super();
        this.filterStr = fstr;
        //this.setRenderer(new MonoColRenderer());
        //this.setEditor(new MyComboBoxEditor());
        this.setEditable(true);
        textFeld = (JTextComponent)this.getEditor().getEditorComponent();
        textFeld.addKeyListener(new MyKeyListener());
        addPopupMouseListener(new MyMouseListener());
        textFeld.setDocument(new ChangeableDocument());
        textFeld.getDocument().addDocumentListener(this);
        items = new Vector<String[]>();
    }

// credit to: http://stackoverflow.com/questions/11278209/how-can-i-make-comboboxs-list-wider
// (see also http://stackoverflow.com/questions/956003/how-can-i-change-the-width-of-a-jcombobox-dropdown-list)
// (see also http://tips4java.wordpress.com/2010/11/28/combo-box-popup/)
/********************* Start Insert */
    // Setting the JComboBox wide
    public void setWide() {
        widestLength = getWidestItemWidth();
    }

    public Dimension getSize() {
        Dimension dim = super.getSize();
        if (!layingOut)
            dim.width = Math.max(widestLength, dim.width);
        return dim;
    }

    protected String parseName(String name) {
        int widthCut = 65;
        int endIndex = name.length() > widthCut ? widthCut : name.length();
        return name.substring(0, endIndex);
    }

    public int getWidestItemWidth() {
        Font font = this.getFont();
        FontMetrics metrics = this.getFontMetrics(font);
        int widest = 0;
        for (int i=0; i<items.size(); i++) {
            int lineWidth = 0;
            String[] itm = items.get(i);
            //for (String s : items.get(i)){
            int end = Math.min(2, itm.length); // only consider first 2 items for width
            for (int j=0; j<end; j++) {
                String s = itm[j];
                lineWidth += metrics.stringWidth( parseName(s) );
            }
            widest = Math.max(widest, lineWidth);
        }
        return widest-50;
    }

    public void doLayout() {
        try {
            layingOut = true;
            super.doLayout();
        } finally {
            layingOut = false;
        }
    }
/********************* Stop Insert */

    public void emptyBox() {
        this.changeMode = true;
            items.clear();
            this.removeAllItems();
        this.changeMode = false;
        textFeld.setText("");
    }

    public void setBox(String[] item) {
        this.changeMode = true;
            items.clear();
            this.removeAllItems();
            items.add(item);
            this.addItem(item[0]);
            //this.setSelectedItem(item);
        this.changeMode = false;
        this.setBoxMode = true;
            textFeld.setText(item[0]);
        this.setBoxMode = false;
        this.setWide();
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
        this.changeMode = true;
            items.clear();
            this.removeAllItems();
        this.changeMode = false;
        if (istr.size() == 1){
            this.setBox(istr.get(0));
            //System.out.println("This is setItems in SearchBox.");
            //fireActionEvent(); // needed?
        } else {
            this.changeMode = true;
                //System.out.println("\n*******\nsetting items\n*****");
                for (String[] item : istr){
                    items.add(item);
                    this.addItem(item[0]);
                }
            this.changeMode = false;
        }
        if ( this.getItemCount() > 1 ){
            this.requestFocus();
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    setWide();
                    showPopup();
                }
            });
        }
        else { this.hidePopup(); }
        this.setWide();
    }

    public abstract Vector<String[]> doQuery();

    // do the incremental search:
    protected void incrementalSearch() {
        this.hidePopup(); // hide popup during editing, so that it will be resized

        // clear all items
        this.changeMode = true; // prevent "Attempt to mutate in notification" exception
            items.clear();
            this.removeAllItems();
        this.changeMode = false;

        if (textFeld.getText().length() >= 3){
            Vector<String[]> searchResults = doQuery();
            //System.out.println("### !!! Doing MqSQL query. Result: "+searchResults);
            this.changeMode = true; // prevent "Attempt to mutate in notification" exception
                for (String[] item : searchResults){
                    items.add(item);
                    this.addItem(item[0]);
                }
            this.changeMode = false;
        }

        if (this.getItemCount() > 0){
            this.setWide();
            this.showPopup();
        }
        //// Create a generic NullPointerException:
        //Integer foo = null; Integer bar = null; foo = foo + bar;
    }

    /**
     *    * Each non abstract class that implements the DocumentListener
     *      must have these methods.
     *
     *    @param e the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
        //System.out.println("in insertUpdate. setBoxMode = "+setBoxMode);
        if (setBoxMode){
            return;
        }
        incrementalSearch();
    }
    public void removeUpdate(DocumentEvent e) {
        //System.out.println("in removeUpdate. setBoxMode = "+setBoxMode);
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
                        System.out.println("This is KeyListener in SearchBox.");
                        fireActionEvent(); // this is actually not needed, because ENTER key alrady fired an action event before ("comboBoxEdited")
                    }
                }
            }
        @Override
            public void keyReleased(KeyEvent e) {
                //System.out.println("key released.");
                changeMode = false;
            }
    }

    // need a low-level mouse listener to put the item that was clicked on in the box and remove all
    // others
    public class MyMouseListener extends MouseAdapter {
        @Override
            public void mouseReleased(MouseEvent e) {
                System.out.println("mouse released.");
                if (getSelectedIndex() >= 0 && getSelectedIndex() < items.size()){
                    String[] item = items.get(getSelectedIndex());
                    System.out.println(item[0]);
                    setBox(item);
                    System.out.println("This is MouseListener in SearchBox.");
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
