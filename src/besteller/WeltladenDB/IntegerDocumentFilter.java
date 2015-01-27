package WeltladenDB;

import javax.swing.text.*; // for DocumentFilter, AbstractDocument, JTextComponent
import javax.swing.JOptionPane;

// based on: http://stackoverflow.com/questions/11093326/restricting-jtextfield-input-to-integers
public class IntegerDocumentFilter extends DocumentFilter {
    private Integer minValue = null;
    private Integer maxValue = null;
    private String valueName = null;
    private WindowContent parentWindow = null;

    public IntegerDocumentFilter(Integer theMaxValue, String theValueName, WindowContent theWC) {
        super();
        maxValue = theMaxValue;
        valueName = theValueName;
        parentWindow = theWC;
    }

    public IntegerDocumentFilter(Integer theMinValue, Integer theMaxValue, String theValueName,
            WindowContent theWC) {
        super();
        System.out.println("constructor in IntegerDocumentFilter.");
        minValue = theMinValue;
        maxValue = theMaxValue;
        valueName = theValueName;
        parentWindow = theWC;
        System.out.println("minValue: "+minValue);
        System.out.println("maxValue: "+maxValue);
        System.out.println("valueName "+valueName);
    }

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

    private String rangeCheck(String text) {
        System.out.println("rangeCheck in IntegerDocumentFilter.");
        Integer value = Integer.parseInt(text);
        if (maxValue != null && value > maxValue){
            JOptionPane.showMessageDialog(parentWindow,
                    valueName+" übersteigt den maximal erlaubten Wert von "+maxValue+"!\n"+
                    "Wird auf "+maxValue+" reduziert.",
                    valueName+" zu groß", JOptionPane.WARNING_MESSAGE);
            value = maxValue;
        } if (minValue != null && value < minValue){
            JOptionPane.showMessageDialog(parentWindow,
                    valueName+" unterschreitet den minimal erlaubten Wert von "+minValue+"!\n"+
                    "Wird auf "+minValue+" erhöht.",
                    valueName+" zu klein", JOptionPane.WARNING_MESSAGE);
            value = minValue;
        }
        return value.toString();
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String newText,
            AttributeSet attr) throws BadLocationException {
        System.out.println("insertString in IntegerDocumentFilter.");
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.insert(offset, newText);

        String origString = sb.toString();
        if (test(origString)) {
            String newString = rangeCheck(origString);
            if (newString.equals(origString)){
                super.insertString(fb, offset, newText, attr);
            } else {
                // TODO Check if this does what it should
                // but it seems as if this method is never called
                super.insertString(fb, 0, newString, attr);
            }
        } else {
            // warn the user and don't allow the insert
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String newText,
            AttributeSet attrs) throws BadLocationException {
        System.out.println("replace in IntegerDocumentFilter.");
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.replace(offset, offset + length, newText);

        String origString = sb.toString();
        if (test(origString)) {
            String newString = rangeCheck(origString);
            if (newString.equals(origString)){
                super.replace(fb, offset, length, newText, attrs);
            } else {
                super.replace(fb, 0, newString.length(), newString, attrs);
            }
        } else {
            // warn the user and don't allow the insert
        }
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws
            BadLocationException {
        System.out.println("remove in IntegerDocumentFilter.");
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
