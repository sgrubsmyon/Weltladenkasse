package Weltladenkasse;

// Basic Java stuff:
//import java.util.*;

import javax.swing.text.*; // for DocumentFilter, AbstractDocument, JTextComponent

// write a DocumentFilter that overrides functions insertString and replace
//					(remove method doesn't need to be overridden):
public class DocumentSizeFilter extends DocumentFilter {
    int maxCharacters;

    public DocumentSizeFilter(int maxChars){
        maxCharacters = maxChars;
    }

    public void insertString(FilterBypass fb, int offset, String newText, AttributeSet attr) throws BadLocationException {
        // truncate string up to maxCharacters
        int newLength = fb.getDocument().getLength() + newText.length();
        int overflow = newLength - maxCharacters;
        if (newLength > maxCharacters)
    	newText = newText.substring(0, newText.length()-overflow);
        super.insertString(fb, offset, newText, attr);
    }

    public void replace(FilterBypass fb, int offset, int length, String newText, AttributeSet attr) throws BadLocationException {
        // truncate string up to maxCharacters
        int newLength = fb.getDocument().getLength() + newText.length() - length;
        int overflow = newLength - maxCharacters;
        if (newLength > maxCharacters)
    	newText = newText.substring(0, newText.length()-overflow);
        super.replace(fb, offset, length, newText, attr);
    }
}
