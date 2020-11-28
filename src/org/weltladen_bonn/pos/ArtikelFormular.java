package org.weltladen_bonn.pos;

// Basic Java stuff:
import java.util.*; // for Vector, Collections
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import org.mariadb.jdbc.MariaDbPoolDataSource;

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

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ArtikelFormular extends WindowContent
    implements ArtikelFormularInterface {
    // Attribute:
    private static final Logger logger = LogManager.getLogger(ArtikelFormular.class);

    public JComboBox<String> produktgruppenBox;
    public JComboBox<String> lieferantBox;
    public JTextField nummerField;
    public JTextField barcodeField;
    public JTextField nameField;
    public JTextField kurznameField;
    public JTextField mengeField;
    public JTextField einheitField;
    public JTextField herkunftField;
    public JSpinner vpeSpinner;
    public JSpinner setSpinner;
    public JCheckBox sortimentBox;
    public JCheckBox lieferbarBox;
    public JComboBox<String> beliebtBox;
    public JTextField vkpreisField;
    public JTextField empfvkpreisField;
    public JTextField ekrabattField;
    public JTextField ekpreisField;
    public JCheckBox preisVariabelBox;

    private boolean hasOriginalName = false;
    private boolean hasOriginalNummer = false;
    private boolean hasOriginalVKP = false;

    private Vector<String> produktgruppenNamen;
    public Vector<Integer> produktgruppenIDs;
    private Vector< Vector<Integer> > produktgruppenIDsList;
    private Vector<String> produktgruppenEinheiten;
    private Vector<String> lieferantNamen;
    public Vector<Integer> lieferantIDs;

    // Methoden:
    public ArtikelFormular(MariaDbPoolDataSource pool, MainWindowGrundlage mw) {
	super(pool, mw);
        fillComboBoxes();
    }
    public ArtikelFormular(MariaDbPoolDataSource pool, MainWindowGrundlage mw, boolean hon, boolean honr, boolean hov) {
        this(pool, mw);
        hasOriginalName = hon;
        hasOriginalNummer = honr;
        hasOriginalVKP = hov;
    }

    public void fillComboBoxes() {
        produktgruppenNamen = new Vector<String>();
        produktgruppenIDs = new Vector<Integer>();
        produktgruppenIDsList = new Vector< Vector<Integer> >();
        produktgruppenEinheiten = new Vector<String>();
        lieferantNamen = new Vector<String>();
        lieferantIDs = new Vector<Integer>();
        try {
            Connection connection = this.pool.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT produktgruppen_id, toplevel_id, sub_id, subsub_id, produktgruppen_name, std_einheit "+
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
                String einheit = rs.getString(6) == null ? "" : rs.getString(6);

                produktgruppenIDs.add(id);
                produktgruppenIDsList.add(ids);
                produktgruppenNamen.add(name);
                produktgruppenEinheiten.add(einheit);
            }
            rs.close();
            rs = stmt.executeQuery(
                    "SELECT lieferant_name, lieferant_id FROM lieferant ORDER BY lieferant_name"
                    );
            while (rs.next()) {
                String name = rs.getString(1);
                Integer id = rs.getInt(2);

                lieferantNamen.add(name);
                lieferantIDs.add(id);
            }
            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            // System.out.println("Exception: " + ex.getMessage());
            // ex.printStackTrace();
            showDBErrorDialog(ex.getMessage());
        }
    }

    void showHeader(JPanel headerPanel, JPanel allPanel) {
	headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

            JPanel produktgruppenPanel = new JPanel();// produktgruppenPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            produktgruppenPanel.setBorder(BorderFactory.createTitledBorder("Produktgruppe"));
            produktgruppenBox = new JComboBox<String>(produktgruppenNamen);
            produktgruppenBox.setRenderer(new ProduktgruppenIndentedRenderer(produktgruppenIDsList));
            produktgruppenBox.setPrototypeDisplayValue("   Kakao und Trinkschokolade");
            produktgruppenBox.setMaximumRowCount(20);
            produktgruppenBox.addActionListener(this);
            produktgruppenPanel.add(produktgruppenBox);

            JPanel lieferantPanel = new JPanel();
            lieferantPanel.setBorder(BorderFactory.createTitledBorder("Lieferant"));
            lieferantBox = new JComboBox<String>(lieferantNamen);
            lieferantBox.setSelectedItem("unbekannt");
            lieferantBox.setPrototypeDisplayValue("Fairtrade Center Breisgau");
            lieferantBox.setMaximumRowCount(20);
            lieferantPanel.add(lieferantBox);

            JPanel nummerPanel = new JPanel();
            nummerPanel.setBorder(BorderFactory.createTitledBorder("Artikelnummer"));
            nummerField = new JTextField("");
            nummerField.setColumns(10);
            ((AbstractDocument)nummerField.getDocument()).setDocumentFilter(bc.nummerFilter);
            nummerPanel.add(nummerField);

            JPanel barcodePanel = new JPanel();
            barcodePanel.setBorder(BorderFactory.createTitledBorder("Barcode"));
            barcodeField = new JTextField("");
            barcodeField.setColumns(20);
            ((AbstractDocument)barcodeField.getDocument()).setDocumentFilter(bc.nummerFilter);
            barcodePanel.add(barcodeField);

        JPanel identPanel = new JPanel(); identPanel.setLayout(new FlowLayout());
        identPanel.add(produktgruppenPanel);
        identPanel.add(lieferantPanel);
        identPanel.add(nummerPanel);
        identPanel.add(barcodePanel);
        headerPanel.add(identPanel);

            JPanel namePanel = new JPanel();
            namePanel.setBorder(BorderFactory.createTitledBorder("Bezeichnung | Einheit"));
            nameField = new JTextField("");
            nameField.setColumns(30);
            ((AbstractDocument)nameField.getDocument()).setDocumentFilter(bc.nameFilter);
            namePanel.add(nameField);

            JPanel kurznamePanel = new JPanel();
            kurznamePanel.setBorder(BorderFactory.createTitledBorder("Kurzname"));
            kurznameField = new JTextField("");
            kurznameField.setColumns(10);
            ((AbstractDocument)kurznameField.getDocument()).setDocumentFilter(bc.kurznameFilter);
            kurznamePanel.add(kurznameField);

            JPanel herkunftPanel = new JPanel();
            herkunftPanel.setBorder(BorderFactory.createTitledBorder("Herkunft"));
            herkunftField = new JTextField("");
            herkunftField.setColumns(20);
            ((AbstractDocument)herkunftField.getDocument()).setDocumentFilter(bc.herkunftFilter);
            herkunftPanel.add(herkunftField);

            JPanel beliebtPanel = new JPanel();
            beliebtPanel.setBorder(BorderFactory.createTitledBorder("Beliebtheit"));
            Vector<String> beliebtNamenValues = new Vector<String>();
            for (int i=0; i<bc.beliebtNamen.size(); i++) {
                beliebtNamenValues.add(bc.beliebtNamen.get(i)+" ("+bc.beliebtWerte.get(i)+")");
            }
            //beliebtBox = new JComboBox<String>(bc.beliebtNamen);
            beliebtBox = new JComboBox<String>(beliebtNamenValues);
            beliebtBox.setMaximumRowCount(bc.beliebtNamen.size());
            beliebtPanel.add(beliebtBox);

        JPanel describePanel = new JPanel();
        describePanel.add(namePanel);
        describePanel.add(kurznamePanel);
        describePanel.add(herkunftPanel);
        describePanel.add(beliebtPanel);
        headerPanel.add(describePanel);

            JPanel mengePanel = new JPanel();
            mengePanel.setBorder(BorderFactory.createTitledBorder("Menge (pro Artikel)"));
            mengeField = new JTextField("");
            mengeField.setColumns(10);
            ((AbstractDocument)mengeField.getDocument()).setDocumentFilter(bc.mengeFilter);
            mengeField.setHorizontalAlignment(JTextField.RIGHT);
            mengePanel.add(mengeField);
            mengePanel.add(new JLabel("kg/l/St."));

            JPanel einheitPanel = new JPanel();
            einheitPanel.setBorder(BorderFactory.createTitledBorder("Einheit (kg/l/St.)"));
            einheitField = new JTextField("kg");
            einheitField.setColumns(10);
            ((AbstractDocument)einheitField.getDocument()).setDocumentFilter(bc.einheitFilter);
            einheitPanel.add(einheitField);

            JPanel vpePanel = new JPanel();
            vpePanel.setBorder(BorderFactory.createTitledBorder("VPE (Verpackungseinheit)"));
            SpinnerNumberModel vpeModel = new SpinnerNumberModel(1, // initial value
                    0, // min
                    bc.smallintMax, // max (null == no max)
                    1); // step
            vpeSpinner = new JSpinner(vpeModel);
            JSpinner.NumberEditor vpeEditor = new JSpinner.NumberEditor(vpeSpinner, "###");
            vpeSpinner.setEditor(vpeEditor);
            JFormattedTextField vpeField = vpeEditor.getTextField();
            ( (NumberFormatter) vpeField.getFormatter() )
                .setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
            vpeField.setColumns(3);
            vpePanel.add(vpeSpinner);
            vpePanel.add(new JLabel("Sets pro Packung"));

            JPanel setPanel = new JPanel();
            setPanel.setBorder(BorderFactory.createTitledBorder("Setgröße"));
            SpinnerNumberModel setModel = new SpinnerNumberModel(1, // initial value
                    1, // min
                    bc.smallintMax, // max (null == no max)
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
            sortimentBox.setSelected(true);
            sortimentPanel.add(sortimentBox);

            JPanel lieferbarPanel = new JPanel();
            lieferbarBox = new JCheckBox("sof. lieferbar");
            lieferbarBox.setSelected(false);
            lieferbarPanel.add(lieferbarBox);

        JPanel detailsPanel = new JPanel();
        detailsPanel.add(mengePanel);
        detailsPanel.add(einheitPanel);
        detailsPanel.add(vpePanel);
        detailsPanel.add(setPanel);
        detailsPanel.add(sortimentPanel);
        detailsPanel.add(lieferbarPanel);
        headerPanel.add(detailsPanel);

            JPanel vkpreisPanel = new JPanel();
            vkpreisPanel.setBorder(BorderFactory.createTitledBorder("VK-Preis (für Artikel)"));
            vkpreisField = new JTextField("");
            vkpreisField.setColumns(15);
            ((AbstractDocument)vkpreisField.getDocument()).setDocumentFilter(bc.geldFilter);
            vkpreisField.setHorizontalAlignment(JTextField.RIGHT);
            vkpreisPanel.add(vkpreisField);
            vkpreisPanel.add(new JLabel(bc.currencySymbol));

            JPanel empfvkpreisPanel = new JPanel();
            empfvkpreisPanel.setBorder(BorderFactory.createTitledBorder("Empf. VK-Preis (für Set)"));
            empfvkpreisField = new JTextField("");
            empfvkpreisField.setColumns(15);
            ((AbstractDocument)empfvkpreisField.getDocument()).setDocumentFilter(bc.geldFilter);
            empfvkpreisField.setHorizontalAlignment(JTextField.RIGHT);
            empfvkpreisPanel.add(empfvkpreisField);
            empfvkpreisPanel.add(new JLabel(bc.currencySymbol));

            JPanel ekrabattPanel = new JPanel();
            ekrabattPanel.setBorder(BorderFactory.createTitledBorder("EK-Rabatt"));
            ekrabattField = new JTextField("");
            ekrabattField.setColumns(6);
            ((AbstractDocument)ekrabattField.getDocument()).setDocumentFilter(bc.relFilter);
            ekrabattField.setHorizontalAlignment(JTextField.RIGHT);
            ekrabattPanel.add(ekrabattField);
            ekrabattPanel.add(new JLabel("%"));

            JPanel ekpreisPanel = new JPanel();
            ekpreisPanel.setBorder(BorderFactory.createTitledBorder("EK-Preis (für Set)"));
            ekpreisField = new JTextField("");
            ekpreisField.setColumns(15);
            ((AbstractDocument)ekpreisField.getDocument()).setDocumentFilter(bc.geldFilter);
            ekpreisField.setHorizontalAlignment(JTextField.RIGHT);
            ekpreisPanel.add(ekpreisField);
            ekpreisPanel.add(new JLabel(bc.currencySymbol));

            JPanel preisVariabelPanel = new JPanel();
            //preisVariabelPanel.setBorder(BorderFactory.createTitledBorder("Variabler Preis"));
            preisVariabelBox = new JCheckBox("Preis variabel");
            preisVariabelBox.setSelected(false);
            preisVariabelPanel.add(preisVariabelBox);

        JPanel preisPanel = new JPanel();
        preisPanel.add(vkpreisPanel);
        preisPanel.add(empfvkpreisPanel);
        preisPanel.add(ekrabattPanel);
        preisPanel.add(ekpreisPanel);
        preisPanel.add(preisVariabelPanel);
        headerPanel.add(preisPanel);

        allPanel.add(headerPanel, BorderLayout.NORTH);
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
    }

    public void updateEKPreisField() {
        /** If both empfvkpreisField and ekrabattField are filled, then
         *  ekpreisField is disabled and displays the resulting calculated
         *  EK-Preis. Otherwise, it can be edited. If it is edited, both
         *  empfvkpreisField and ekrabattField are cleared.
         */
        if ( preisVariabelBox.isSelected() ){
            // we have a variable price, don't touch it!
            return;
        }
        if ( empfVKPAndEKRabattValid(empfvkpreisField.getText(), ekrabattField.getText()) ){
            ekpreisField.setEnabled(false);
            BigDecimal ekpreis = calculateEKP(empfvkpreisField.getText(), ekrabattField.getText());
            ekpreisField.setText( bc.priceFormatter(ekpreis) );
        } else {
            ekpreisField.setEnabled(true);
        }
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

    public boolean showArticleKnownWarning(int itemAlreadyKnown) {
        if (itemAlreadyKnown == 1){
            JOptionPane.showMessageDialog(this,
                    "Ein Artikel mit diesem Lieferant und dieser Nummer ist bereits in der Datenbank.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return true;
        }
        else if (itemAlreadyKnown == 2){
            JOptionPane.showMessageDialog(this,
                    "Ein Artikel mit diesem Lieferant und dieser Nummer ist bereits in der angezeigten Tabelle.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return true;
        }
        return false;
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == produktgruppenBox){
            int prodGrIndex = produktgruppenBox.getSelectedIndex();
            String einheit = "";
            if (prodGrIndex >= 0 && prodGrIndex < produktgruppenEinheiten.size()) {
                einheit = produktgruppenEinheiten.get(prodGrIndex);
            }
            einheitField.setText(einheit);
        }
    }
}
