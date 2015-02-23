package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
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

public class ArtikelFormular extends WindowContent
    implements ArtikelFormularInterface {
    // Attribute:
    public JComboBox produktgruppenBox;
    public JComboBox lieferantBox;
    public JTextField nummerField;
    public JTextField barcodeField;
    public JTextField nameField;
    public JTextField kurznameField;
    public JTextField mengeField;
    public JTextField herkunftField;
    public JSpinner vpeSpinner;
    public JSpinner setSpinner;
    public JCheckBox sortimentBox;
    public JCheckBox lieferbarBox;
    public JComboBox beliebtBox;
    public JTextField vkpreisField;
    public JTextField empfvkpreisField;
    public JTextField ekrabattField;
    public JCheckBox preisVariabelBox;

    private boolean hasOriginalName = false;
    private boolean hasOriginalNummer = false;
    private boolean hasOriginalVKP = false;

    private Vector<String> produktgruppenNamen;
    public Vector<Integer> produktgruppenIDs;
    private Vector< Vector<Integer> > produktgruppenIDsList;
    private Vector<String> lieferantNamen;
    public Vector<Integer> lieferantIDs;

    // Methoden:
    public ArtikelFormular(Connection conn, MainWindowGrundlage mw) {
	super(conn, mw);
        fillComboBoxes();
    }
    public ArtikelFormular(Connection conn, MainWindowGrundlage mw, boolean hon, boolean honr, boolean hov) {
        this(conn, mw);
        hasOriginalName = hon;
        hasOriginalNummer = honr;
        hasOriginalVKP = hov;
    }

    public void fillComboBoxes() {
        produktgruppenNamen = new Vector<String>();
        produktgruppenIDs = new Vector<Integer>();
        produktgruppenIDsList = new Vector< Vector<Integer> >();
        lieferantNamen = new Vector<String>();
        lieferantIDs = new Vector<Integer>();
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT produktgruppen_id, toplevel_id, sub_id, subsub_id, produktgruppen_name "+
                    "FROM produktgruppe WHERE mwst_id IS NOT NULL AND toplevel_id IS NOT NULL "+
                    "AND aktiv = TRUE ORDER BY toplevel_id, sub_id, subsub_id"
                    );
            while (rs.next()) {
                Integer id = rs.getInt(1);
                Vector<Integer> ids = new Vector<Integer>();
                ids.add( rs.getString(2) == null ? null : rs.getInt(2) );
                ids.add( rs.getString(3) == null ? null : rs.getInt(3) );
                ids.add( rs.getString(4) == null ? null : rs.getInt(4) );
                String name = rs.getString(5);

                produktgruppenIDs.add(id);
                produktgruppenIDsList.add(ids);
                produktgruppenNamen.add(name);
            }
            rs.close();
            rs = stmt.executeQuery(
                    "SELECT lieferant_name, lieferant_id FROM lieferant ORDER BY lieferant_id"
                    );
            while (rs.next()) {
                String name = rs.getString(1);
                Integer id = rs.getInt(2);

                lieferantNamen.add(name);
                lieferantIDs.add(id);
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void showHeader(JPanel headerPanel, JPanel allPanel) {
	headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

            JPanel produktgruppenPanel = new JPanel();// produktgruppenPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            produktgruppenPanel.setBorder(BorderFactory.createTitledBorder("Produktgruppe"));
            produktgruppenBox = new JComboBox(produktgruppenNamen);
            produktgruppenBox.setRenderer(new ProduktgruppenIndentedRenderer(produktgruppenIDsList));
            produktgruppenPanel.add(produktgruppenBox);

            JPanel lieferantPanel = new JPanel();
            lieferantPanel.setBorder(BorderFactory.createTitledBorder("Lieferant"));
            lieferantBox = new JComboBox(lieferantNamen);
            lieferantPanel.add(lieferantBox);

            JPanel nummerPanel = new JPanel();
            nummerPanel.setBorder(BorderFactory.createTitledBorder("Artikelnummer"));
            nummerField = new JTextField("");
            nummerField.setColumns(10);
            nummerPanel.add(nummerField);

            JPanel barcodePanel = new JPanel();
            barcodePanel.setBorder(BorderFactory.createTitledBorder("Barcode"));
            barcodeField = new JTextField("");
            barcodeField.setColumns(20);
            barcodePanel.add(barcodeField);

        JPanel identPanel = new JPanel(); identPanel.setLayout(new FlowLayout());
        identPanel.add(produktgruppenPanel);
        identPanel.add(lieferantPanel);
        identPanel.add(nummerPanel);
        identPanel.add(barcodePanel);
        headerPanel.add(identPanel);

            JPanel namePanel = new JPanel();
            namePanel.setBorder(BorderFactory.createTitledBorder("Artikelname"));
            nameField = new JTextField("");
            nameField.setColumns(30);
            namePanel.add(nameField);

            JPanel kurznamePanel = new JPanel();
            kurznamePanel.setBorder(BorderFactory.createTitledBorder("Kurzname"));
            kurznameField = new JTextField("");
            kurznameField.setColumns(10);
            kurznamePanel.add(kurznameField);

            JPanel mengePanel = new JPanel();
            mengePanel.setBorder(BorderFactory.createTitledBorder("Menge (pro Artikel)"));
            mengeField = new JTextField("");
            mengeField.setColumns(6);
            ((AbstractDocument)mengeField.getDocument()).setDocumentFilter(mengeFilter);
            mengeField.setHorizontalAlignment(JTextField.RIGHT);
            mengePanel.add(mengeField);
            mengePanel.add(new JLabel("kg/l/Stk."));

            JPanel herkunftPanel = new JPanel();
            herkunftPanel.setBorder(BorderFactory.createTitledBorder("Herkunft"));
            herkunftField = new JTextField("");
            herkunftField.setColumns(20);
            herkunftPanel.add(herkunftField);

        JPanel describePanel = new JPanel();
        describePanel.add(namePanel);
        describePanel.add(kurznamePanel);
        describePanel.add(mengePanel);
        describePanel.add(herkunftPanel);
        headerPanel.add(describePanel);

            JPanel vpePanel = new JPanel();
            vpePanel.setBorder(BorderFactory.createTitledBorder("VPE (Verpackungseinheit)"));
            SpinnerNumberModel vpeModel = new SpinnerNumberModel(0, // initial value
                    0, // min
                    smallintMax, // max (null == no max)
                    1); // step
            vpeSpinner = new JSpinner(vpeModel);
            JSpinner.NumberEditor vpeEditor = new JSpinner.NumberEditor(vpeSpinner, "###");
            vpeSpinner.setEditor(vpeEditor);
            JFormattedTextField vpeField = vpeEditor.getTextField();
            ( (NumberFormatter) vpeField.getFormatter() )
                .setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
            vpeField.setColumns(3);
            vpePanel.add(vpeSpinner);
            vpePanel.add(new JLabel("Artikel/Sets pro Packung"));

            JPanel setPanel = new JPanel();
            setPanel.setBorder(BorderFactory.createTitledBorder("Setgröße"));
            SpinnerNumberModel setModel = new SpinnerNumberModel(1, // initial value
                    1, // min
                    smallintMax, // max (null == no max)
                    1); // step
            setSpinner = new JSpinner(setModel);
            JSpinner.NumberEditor setEditor = new JSpinner.NumberEditor(setSpinner, "###");
            setSpinner.setEditor(setEditor);
            JFormattedTextField setField = setEditor.getTextField();
            ( (NumberFormatter) setField.getFormatter() )
                .setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
            setField.setColumns(3);
            setPanel.add(setSpinner);
            setPanel.add(new JLabel("Artikel pro Set"));

            JPanel sortimentPanel = new JPanel();
            sortimentBox = new JCheckBox("Sortiment");
            sortimentBox.setSelected(false);
            sortimentPanel.add(sortimentBox);

            JPanel lieferbarPanel = new JPanel();
            lieferbarBox = new JCheckBox("sof. lieferbar");
            lieferbarBox.setSelected(false);
            lieferbarPanel.add(lieferbarBox);

            JPanel beliebtPanel = new JPanel();
            beliebtPanel.setBorder(BorderFactory.createTitledBorder("Beliebtheit"));
            beliebtBox = new JComboBox(beliebtNamen);
            beliebtPanel.add(beliebtBox);

        JPanel detailsPanel = new JPanel();
        detailsPanel.add(vpePanel);
        detailsPanel.add(setPanel);
        detailsPanel.add(sortimentPanel);
        detailsPanel.add(lieferbarPanel);
        detailsPanel.add(beliebtPanel);
        headerPanel.add(detailsPanel);

            JPanel vkpreisPanel = new JPanel();
            vkpreisPanel.setBorder(BorderFactory.createTitledBorder("VK-Preis (für Artikel)"));
            vkpreisField = new JTextField("");
            vkpreisField.setColumns(12);
            ((AbstractDocument)vkpreisField.getDocument()).setDocumentFilter(geldFilter);
            vkpreisField.setHorizontalAlignment(JTextField.RIGHT);
            vkpreisPanel.add(vkpreisField);
            vkpreisPanel.add(new JLabel(currencySymbol));

            JPanel empfvkpreisPanel = new JPanel();
            empfvkpreisPanel.setBorder(BorderFactory.createTitledBorder("Empf. VK-Preis (für Set)"));
            empfvkpreisField = new JTextField("");
            empfvkpreisField.setColumns(12);
            ((AbstractDocument)empfvkpreisField.getDocument()).setDocumentFilter(geldFilter);
            empfvkpreisField.setHorizontalAlignment(JTextField.RIGHT);
            empfvkpreisPanel.add(empfvkpreisField);
            empfvkpreisPanel.add(new JLabel(currencySymbol));

            JPanel ekrabattPanel = new JPanel();
            ekrabattPanel.setBorder(BorderFactory.createTitledBorder("EK-Rabatt"));
            ekrabattField = new JTextField("");
            ekrabattField.setColumns(6);
            ((AbstractDocument)ekrabattField.getDocument()).setDocumentFilter(relFilter);
            ekrabattField.setHorizontalAlignment(JTextField.RIGHT);
            ekrabattPanel.add(ekrabattField);
            ekrabattPanel.add(new JLabel("%"));

            JPanel preisVariabelPanel = new JPanel();
            //preisVariabelPanel.setBorder(BorderFactory.createTitledBorder("Variabler Preis"));
            preisVariabelBox = new JCheckBox("Preis variabel");
            preisVariabelBox.setSelected(false);
            preisVariabelPanel.add(preisVariabelBox);

        JPanel preisPanel = new JPanel();
        preisPanel.add(vkpreisPanel);
        preisPanel.add(empfvkpreisPanel);
        preisPanel.add(ekrabattPanel);
        preisPanel.add(preisVariabelPanel);
        headerPanel.add(preisPanel);

        allPanel.add(headerPanel);
    }

    public boolean checkIfFormIsComplete() {
        if ( nameField.isEnabled() && nameField.getText().replaceAll("\\s","").equals("") ){
            if (!hasOriginalName)
                return false;
        }
        if ( nummerField.isEnabled() && nummerField.getText().replaceAll("\\s","").equals("") ){
            if (!hasOriginalNummer)
                return false;
        }
        if ( preisVariabelBox.isEnabled() && !preisVariabelBox.isSelected() &&
                vkpreisField.isEnabled() && vkpreisField.getText().replaceAll("\\s","").equals("") ){
            if (!hasOriginalVKP)
                return false;
        }
        return true;

        //if (
        //        nameField.getText().length() > 0 && nummerField.getText().length() > 0 &&
        //        ( (vkpreisField.getText().length() > 0) || preisVariabelBox.isSelected() )
        //   ){
        //    return true;
        //} else {
        //    return false;
        //}
    }

    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
        if (source == preisVariabelBox) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                vkpreisField.setEnabled(false);
                empfvkpreisField.setEnabled(false);
                ekrabattField.setEnabled(false);
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                vkpreisField.setEnabled(true);
                empfvkpreisField.setEnabled(true);
                ekrabattField.setEnabled(true);
            }
        }
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
    }
}
