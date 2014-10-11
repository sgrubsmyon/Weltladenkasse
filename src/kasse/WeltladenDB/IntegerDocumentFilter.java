package WeltladenDB;

import javax.swing.text.*; // for DocumentFilter, AbstractDocument, JTextComponent

// based on: http://stackoverflow.com/questions/11093326/restricting-jtextfield-input-to-integers
public class IntegerDocumentFilter extends DocumentFilter {
    private boolean test(String text) {
        // also allow empty strings:
        if ( text.equals("") ){
            return true;
        }
        try {
            Integer.parseInt(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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
