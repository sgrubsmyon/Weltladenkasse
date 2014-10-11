package WeltladenDB;

import javax.swing.text.*; // for DocumentFilter, AbstractDocument, JTextComponent

public class StringDocumentFilter extends DocumentFilter {
    private int numchars = 180;

    public StringDocumentFilter(int nchar) {
        super();
        numchars = nchar;
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

        if (test(sb.toString())) {
            super.insertString(fb, offset, newText, attr);
        } else {
            // warn the user and don't allow the insert
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String newText,
            AttributeSet attrs) throws BadLocationException {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.replace(offset, offset + length, newText);

        if (test(sb.toString())) {
            super.replace(fb, offset, length, newText, attrs);
        } else {
            // warn the user and don't allow the insert
        }
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
