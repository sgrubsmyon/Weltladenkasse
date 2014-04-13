package Weltladenkasse;

// GUI stuff:
import javax.swing.text.*; // for DocumentFilter

// write a DocumentFilter that overrides functions insertString and replace
//					(remove method doesn't need to be overridden):
public class FloatDocumentFilter extends DocumentFilter {
    String getLegalText(FilterBypass fb, int offset, String newText) {
        String legalText = newText.replaceAll("[^-0-9,.]","");
        if (offset != 0) // no minus sign allowed!
            legalText = legalText.replaceAll("-","");
        String oldText = new String("");
        try {
            oldText = fb.getDocument().getText(0, fb.getDocument().getLength());
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        if ( oldText.contains(",") || oldText.contains(".") ){
            legalText = legalText.replaceAll("[,.]","");
            return legalText;
        }
        else {
            int strLength = legalText.length();
            if (strLength == 1) return legalText;
            else {
                String validLegalText = new String("");
                boolean hadDecimalSign = false;
                for (int i = 0; i<strLength; i++){ 
                    char c = legalText.charAt(i);
                    if (c == ',' || c == '.'){
                        if (hadDecimalSign) { } // do nothing (already had a ',' or '.', don't add second one)
                        else {
                            hadDecimalSign = true; // first decimal sign seen
                            validLegalText += c; // add decimal sign
                        }
                    }
                    else {
                        validLegalText += c; // add character
                    }
                }
                return validLegalText;
            }
        }
    }

    public void insertString(FilterBypass fb, int offset, String newText, AttributeSet attr) throws BadLocationException {
        // replace illegal characters with nothing, then send them to the super constructor
        String legalText = getLegalText(fb, offset, newText);
        super.insertString(fb, offset, legalText, attr);
    }

    public void replace(FilterBypass fb, int offset, int length, String newText, AttributeSet attr) throws BadLocationException {
        if (newText != null) {
            String legalText = getLegalText(fb, offset, newText);
            newText = legalText;
        }
        super.replace(fb, offset, length, newText, attr);
    }
}
