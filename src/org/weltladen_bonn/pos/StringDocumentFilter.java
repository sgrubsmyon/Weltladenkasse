package org.weltladen_bonn.pos;

import javax.swing.text.*; // for DocumentFilter, AbstractDocument, JTextComponent

public class StringDocumentFilter extends DocumentFilter {
    private int numchars = 180;

    public StringDocumentFilter() {
        super();
    }

    public StringDocumentFilter(int nchar) {
        super();
        numchars = nchar;
    }

    public void setNChar(int nchar) {
        numchars = nchar;
    }

    public int getNChar() {
        return numchars;
    }

    private boolean test(String text) {
        return text.length() <= numchars;
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String newText,
            AttributeSet attr) throws BadLocationException {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.insert(offset, newText);

        if (!test(sb.toString())) {
            // truncate the new text up to threshold
            try {
                newText = newText.substring(0, numchars-offset);
            } catch (IndexOutOfBoundsException ex) {
                newText = "";
            }
        }
        super.insertString(fb, offset, newText, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String newText,
            AttributeSet attrs) throws BadLocationException {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.replace(offset, offset + length, newText);

        if (!test(sb.toString())) {
            // truncate the new text up to threshold
            try {
                newText = newText.substring(0, numchars-offset);
            } catch (IndexOutOfBoundsException ex) {
                newText = "";
            }
        }
        super.replace(fb, offset, length, newText, attrs);
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws
            BadLocationException {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.delete(offset, offset + length);

        if (test(sb.toString())) {
            super.remove(fb, offset, length);
        } else {
            // warn the user and don't allow the insert
        }
    }
}
