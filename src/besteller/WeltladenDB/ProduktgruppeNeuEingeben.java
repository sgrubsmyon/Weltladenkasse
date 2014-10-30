package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// GUI stuff:
//import java.awt.BorderLayout;
//import java.awt.FlowLayout;
//import java.awt.Dimension;
import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
import java.awt.event.*;

//import javax.swing.JFrame;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTable;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*; // for DocumentListener
import javax.swing.text.*; // for DocumentFilter

public class ProduktgruppeNeuEingeben extends DialogWindow
    implements DocumentListener {
    // Attribute:
    protected JButton submitButton;
    protected JButton deleteButton;
    protected Produktgruppenliste produktgruppenListe;

    protected JPanel allPanel;
    protected JPanel headerPanel;
    protected JPanel footerPanel;

    private JTextField produktgruppenNameField;

    // Methoden:
    public ProduktgruppeNeuEingeben(Connection conn, MainWindowGrundlage mw, Produktgruppenliste pw, JDialog dia) {
	super(conn, mw, dia);
        this.produktgruppenListe = pw;
        showAll();
    }

    protected void showAll() {
	allPanel = new JPanel();
	allPanel.setLayout(new BoxLayout(allPanel, BoxLayout.Y_AXIS));

        showHeader();
        showFooter();

	this.add(allPanel, BorderLayout.CENTER);
    }

    void showHeader() {
        headerPanel = new JPanel();
        JPanel namePanel = new JPanel();
        namePanel.setBorder(BorderFactory.createTitledBorder("Produktgruppen-Name"));
        produktgruppenNameField = new JTextField("");
        produktgruppenNameField.setColumns(30);
        namePanel.add(produktgruppenNameField);
        headerPanel.add(namePanel);

        KeyAdapter enterAdapter = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
                    if (submitButton.isEnabled()){
                        submitButton.doClick();
                    }
                }
            }
        };

        produktgruppenNameField.getDocument().addDocumentListener(this);
        produktgruppenNameField.addKeyListener(enterAdapter);
        allPanel.add(headerPanel);
    }

    void showFooter() {
        footerPanel = new JPanel();
        submitButton = new JButton("Speichern");
        submitButton.setMnemonic(KeyEvent.VK_A);
        submitButton.addActionListener(this);
        submitButton.setEnabled( checkIfFormIsComplete() );
        footerPanel.add(submitButton);
        deleteButton = new JButton("Verwerfen");
        deleteButton.setMnemonic(KeyEvent.VK_V);
        deleteButton.addActionListener(this);
        deleteButton.setEnabled(true);
        footerPanel.add(deleteButton);
        closeButton = new JButton("Schließen");
        closeButton.setMnemonic(KeyEvent.VK_S);
        closeButton.addActionListener(this);
        closeButton.setEnabled(true);
        footerPanel.add(closeButton);
        allPanel.add(footerPanel);
    }

    public boolean checkIfFormIsComplete() {
        String produktgruppe = produktgruppenNameField.getText();
        boolean produktgruppeOK = (produktgruppe.replaceAll("\\s","").length() > 0);
        if (produktgruppeOK){
            produktgruppeOK = !isProduktgruppeAlreadyKnown(produktgruppe);
        }
        return produktgruppeOK;
    }

    public int submit() {
        return insertNewProduktgruppe(produktgruppenNameField.getText());
    }

    /**
     *    * Each non abstract class that implements the DocumentListener
     *      must have these methods.
     *
     *    @param e the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
	// check if form is valid (if item can be added to list)
        submitButton.setEnabled( checkIfFormIsComplete() );
    }
    public void removeUpdate(DocumentEvent e) {
	// check if form is valid (if item can be added to list)
	insertUpdate(e);
    }
    public void changedUpdate(DocumentEvent e) {
	// Plain text components do not fire these events
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
	if (e.getSource() == submitButton){
            int result = submit();
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Produktgruppe "+produktgruppenNameField.getText()+" konnte nicht eingefügt werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            } else {
                produktgruppenListe.updateAll();
                closeButton.doClick();
            }
            return;
        }
	if (e.getSource() == deleteButton){
            produktgruppenNameField.setText("");
            return;
        }
        super.actionPerformed(e);
    }
}
