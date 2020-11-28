package org.weltladen_bonn.pos.besteller;

// Basic Java stuff:
import java.util.*; // for Vector
import java.io.*; // for File
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.*;
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import java.awt.*; // BorderLayout, FlowLayout, Dimension
import java.awt.event.*; // ActionEvent, ActionListener

import javax.swing.*; // JFrame, JPanel, JTable, JButton, ...
import javax.swing.table.*;
import javax.swing.text.*; // for DocumentFilter
import javax.swing.event.*;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.weltladen_bonn.pos.*;

public class Bestellen extends BestellungsGrundlage implements
    ArticleSelectUser, DocumentListener, TableModelListener {
    // Attribute:
    private static final Logger logger = LogManager.getLogger(Bestellen.class);

    private final BigDecimal minusOne = new BigDecimal(-1);
    private final BigDecimal percent = new BigDecimal("0.01");

    protected int selBestellNr = -1;
    protected String selTyp = "LM";
    protected int selJahr = -1;
    protected int selKW = -1;

    private TabbedPane tabbedPane;

    private int selectedArticleID;
    private int selectedNumberOfVPEs = 1;

    protected ArticleSelectPanelBestellen asPanel;
    protected JSpinner anzahlSpinner;
    private JSpinner vpeSpinner;
    protected JFormattedTextField anzahlField;
    private JFormattedTextField vpeSpinnerField;
    private boolean vpeOrAnzahlIsChanged = false;
    protected JTextField vpeField;
    protected JTextField preisField;
    protected JLabel setLabel;
    private JSpinner jahrSpinner;
    private JFormattedTextField jahrField;
    private JSpinner kwSpinner;
    private JFormattedTextField kwField;
    private JTextField typField;
    private JTextField filterField;
    private JButton emptyFilterButton;
    // Buttons
    private JButton changeTypButton;
    private JButton emptyBarcodeButton;
    private JButton emptyArtikelButton;
    private JButton emptyNummerButton;
    private JButton hinzufuegenButton;
    protected JButton changeButton;
    private Vector<JButton> removeButtons;
    private JButton abschliessenButton;
    private JButton verwerfenButton;

    protected JLabel beliebtKuller;
    protected JTextArea beliebtText;

    // The panels
    private JPanel allPanel;
    private JPanel articleListPanel;
    private JScrollPane articleScrollPane;
    private JPanel abschliessenPanel;

    // The table holding the purchase articles.
    private BestellungsTable orderTable;
    protected Vector< Vector<Object> > data; // holding full/original data
    protected Vector< Vector<Object> > displayData; // holding the subset of data used for display
                                                    // (after applying filter)
    private Vector<Integer> displayIndices; // holding indices that rows in displayData have in data
    private Vector<Integer> artikelIDs;
    private Vector<String> colors;
    private Vector<Integer> positions;

    private String filterStr = "";

    // Methoden:

    /**
     *    The constructor.
     *       */
    public Bestellen(MariaDbPoolDataSource pool, MainWindowGrundlage mw, TabbedPane tp) {
        super(pool, mw);
        tabbedPane = tp;

        columnLabels.add("Entfernen");

        emptyTable();
        showAll();
        doCSVBackupReadin();
        asPanel.emptyArtikelBox();
    }

    void preventSpinnerOverflow(JSpinner spinner) {
        AbstractDocument doc = new PlainDocument() {
            @Override
            public void setDocumentFilter(DocumentFilter filter) {
                if (filter instanceof IntegerDocumentFilter) { // w/o this if, it's not working
                                // maybe the DocumentFilter is reset to a default filter for Spinners
                    super.setDocumentFilter(filter);
                }
            }
        };
        doc.setDocumentFilter(
                new IntegerDocumentFilter(
                    (Integer)((SpinnerNumberModel)spinner.getModel()).getMinimum(),
                    (Integer)((SpinnerNumberModel)spinner.getModel()).getMaximum(), "Anzahl", this
                    )
                );
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor)spinner.getEditor();
        JFormattedTextField field = editor.getTextField();
        field.setDocument(doc);
    }

    void showAll(){
      allPanel = new JPanel(new BorderLayout());

      JPanel northPanel = new JPanel();

      JPanel formPanel = new JPanel();
      formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.PAGE_AXIS));

      JPanel datePanel = new JPanel();
      datePanel.setLayout(new FlowLayout());
      datePanel.add(new JLabel("Rechnung für"));
      /////
      JLabel jahrLabel = new JLabel("Jahr:");
      datePanel.add(jahrLabel);
      Calendar rightNow = Calendar.getInstance();
      int year = rightNow.get(Calendar.YEAR);
      if (selJahr > 0){
        year = selJahr;
      }
      SpinnerNumberModel jahrModel = new SpinnerNumberModel(year, // initial value
        0, // min
        null, // max (null == no max)
        1); // step
      jahrSpinner = new JSpinner(jahrModel);
      JSpinner.NumberEditor jahrEditor = new JSpinner.NumberEditor(jahrSpinner, "####");
      jahrSpinner.setEditor(jahrEditor);
      jahrField = jahrEditor.getTextField();
      ( (NumberFormatter) jahrField.getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
      jahrField.getDocument().addDocumentListener(this);
      jahrField.setColumns(4);
      removeDefaultKeyBindings(jahrField);
      jahrLabel.setLabelFor(jahrSpinner);
      datePanel.add(jahrSpinner);
      /////
      JLabel kwLabel = new JLabel("KW:");
      datePanel.add(kwLabel);
      int week = rightNow.get(Calendar.WEEK_OF_YEAR)+1; // default: following week
      if (selKW > 0){
        week = selKW;
      }
      if (week > 53) {
        week = 53;
      }
      SpinnerNumberModel kwModel = new SpinnerNumberModel(week, // initial value
        1, // min
        53, // max (null == no max)
        1); // step
      kwSpinner = new JSpinner(kwModel);
      JSpinner.NumberEditor kwEditor = new JSpinner.NumberEditor(kwSpinner, "##");
      kwSpinner.setEditor(kwEditor);
      kwField = kwEditor.getTextField();
      ( (NumberFormatter) kwField.getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
      kwField.getDocument().addDocumentListener(this);
      kwField.setColumns(2);
      removeDefaultKeyBindings(kwField);
      kwLabel.setLabelFor(kwSpinner);
      datePanel.add(kwSpinner);
      ///////
      JLabel typLabel = new JLabel("Typ:");
      datePanel.add(typLabel);
      typField = new JTextField(selTyp);
      StringDocumentFilter sdf = new StringDocumentFilter(12);
      ((AbstractDocument)typField.getDocument()).setDocumentFilter(sdf);
      typField.getDocument().addDocumentListener(this);
      typField.setColumns(6);
      removeDefaultKeyBindings(typField);
      typLabel.setLabelFor(typField);
      datePanel.add(typField);
      changeTypButton = new JButton("Typ ändern");
      changeTypButton.addActionListener(this);
      datePanel.add(changeTypButton);
      ///////
      datePanel.add(new JLabel("Bestell-Nr.:"));
      JTextField bestNrField = new JTextField("");
      bestNrField.setColumns(6);
      removeDefaultKeyBindings(bestNrField);
      bestNrField.setHorizontalAlignment(JTextField.RIGHT);
      if (selBestellNr > 0){
        bestNrField.setText(Integer.valueOf(selBestellNr).toString());
      }
      bestNrField.setEditable(false);
      datePanel.add(bestNrField);
      formPanel.add(datePanel);

      asPanel = new ArticleSelectPanelBestellen(this.pool, mainWindow, this, tabbedPane);
      formPanel.add(asPanel);

      JPanel chooseArticlePanel = new JPanel();
      chooseArticlePanel.setLayout(new FlowLayout());
      JLabel anzahlLabel = new JLabel("Anzahl: ");
      chooseArticlePanel.add(anzahlLabel);
      SpinnerNumberModel anzahlModel = new SpinnerNumberModel(1, // initial value
      1, // min
      bc.smallintMax, // max (null == no max)
      1); // step
      anzahlSpinner = new JSpinner(anzahlModel);
      JSpinner.NumberEditor anzahlEditor = new JSpinner.NumberEditor(anzahlSpinner, "###");
      anzahlSpinner.setEditor(anzahlEditor);
      anzahlField = anzahlEditor.getTextField();
      ( (NumberFormatter) anzahlField.getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
      anzahlField.getDocument().addDocumentListener(this);
      anzahlField.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
            if (preisField.isEditable())
            preisField.requestFocus();
            else {
              if (hinzufuegenButton.isEnabled()){
                anzahlSpinner.setValue(Integer.parseInt(anzahlField.getText()));
                hinzufuegenButton.doClick();
              }
            }
          }
        }
      });
      anzahlField.setColumns(4);
      removeDefaultKeyBindings(anzahlField);
      preventSpinnerOverflow(anzahlSpinner);
      anzahlLabel.setLabelFor(anzahlSpinner);
      chooseArticlePanel.add(anzahlSpinner);

      SpinnerNumberModel vpeModel = new SpinnerNumberModel(1, // initial value
        0, // min
        null, // max (null == no max)
        1); // step
      vpeSpinner = new JSpinner(vpeModel);
      JSpinner.NumberEditor vpeEditor = new JSpinner.NumberEditor(vpeSpinner, "###");
      vpeSpinner.setEditor(vpeEditor);
      vpeSpinnerField = vpeEditor.getTextField();
      ( (NumberFormatter) vpeSpinnerField.getFormatter() ).setAllowsInvalid(false); // accept only allowed values (i.e. numbers)
      vpeSpinnerField.getDocument().addDocumentListener(this);
      vpeSpinnerField.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
            if (preisField.isEditable())
            preisField.requestFocus();
            else {
              if (hinzufuegenButton.isEnabled()){
                vpeSpinner.setValue(Integer.parseInt(vpeSpinnerField.getText()));
                hinzufuegenButton.doClick();
              }
            }
          }
        }
      });
      vpeSpinnerField.setColumns(3);
      removeDefaultKeyBindings(vpeSpinnerField);
      chooseArticlePanel.add(vpeSpinner);

      JLabel vpeLabel = new JLabel("VPE: ");
      chooseArticlePanel.add(vpeLabel);
      vpeField = new JTextField("");
      vpeField.setEditable(false);
      vpeField.setColumns(3);
      removeDefaultKeyBindings(vpeField);
      vpeField.setHorizontalAlignment(JTextField.RIGHT);
      vpeLabel.setLabelFor(vpeField);
      chooseArticlePanel.add(vpeField);

      JLabel preisLabel = new JLabel("VK-Preis: ");
      chooseArticlePanel.add(preisLabel);
      preisField = new JTextField("");
      preisField.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) { if ( e.getKeyCode() == KeyEvent.VK_ENTER  ){
          if (hinzufuegenButton.isEnabled()){
            hinzufuegenButton.doClick();
          }
        } }
      });
      preisField.getDocument().addDocumentListener(this);
      ((AbstractDocument)preisField.getDocument()).setDocumentFilter(bc.geldFilter);
      preisField.setEditable(false);
      preisField.setColumns(6);
      removeDefaultKeyBindings(preisField);
      preisField.setHorizontalAlignment(JTextField.RIGHT);
      chooseArticlePanel.add(preisField);
      chooseArticlePanel.add(new JLabel(bc.currencySymbol));
      setLabel = new JLabel("");
      chooseArticlePanel.add(setLabel);

      hinzufuegenButton = new JButton("Hinzufügen");
      hinzufuegenButton.setMnemonic(KeyEvent.VK_H);
      hinzufuegenButton.addActionListener(this);
      hinzufuegenButton.setEnabled(false);
      chooseArticlePanel.add(hinzufuegenButton);

      changeButton = new JButton("Verändern");
      changeButton.setMnemonic(KeyEvent.VK_V);
      changeButton.addActionListener(this);
      changeButton.setEnabled(false);
      chooseArticlePanel.add(changeButton);
      formPanel.add(chooseArticlePanel);

      JPanel beliebtPanel = new JPanel();
      beliebtKuller = new JLabel( bc.beliebtKuerzel.get(bc.beliebtNamen.indexOf("keine Angabe")) );
      beliebtKuller.setFont(bc.bigFont);
      beliebtKuller.setForeground( Color.LIGHT_GRAY );
      beliebtText = new JTextArea(3, 20);
      beliebtText = makeLabelStyle(beliebtText);
      beliebtText.setEditable(false);
      beliebtPanel.add(beliebtKuller);
      beliebtPanel.add(beliebtText);

      northPanel.add(formPanel);
      northPanel.add(beliebtPanel);
      allPanel.add(northPanel, BorderLayout.NORTH);

      showTable();

      this.add(allPanel, BorderLayout.CENTER);
    }

    void initiateTable() {
      orderTable = new BestellungsTable(bc, displayData, columnLabels, colors);
      removeDefaultKeyBindings(orderTable, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
      orderTable.getModel().addTableModelListener(this);
      setTableProperties(orderTable);
      TableColumn entf = orderTable.getColumn("Entfernen");
      entf.setPreferredWidth(200);

      // set up spinner column:
      TableColumn col = orderTable.getColumn("Stückzahl");
      SpinnerNumberModel model = new SpinnerNumberModel(1, 1, bc.smallintMax, 1);
      col.setCellRenderer(new JSpinnerRenderer(model));
      JSpinnerEditor se = new JSpinnerEditor(model);
      preventSpinnerOverflow(se.getSpinner());
      col.setCellEditor(se);
      orderTable.setColEditable(col.getModelIndex(), true);
    }

    void showTable(){
      initiateTable();

      articleListPanel = new JPanel();
      articleListPanel.setLayout(new BoxLayout(articleListPanel, BoxLayout.PAGE_AXIS));
      articleListPanel.setBorder(BorderFactory.createTitledBorder("Gewählte Artikel"));

      //articleScrollPane = new ScrollPane();
      //articleScrollPane.add(orderTable);
      articleScrollPane = new JScrollPane(orderTable);
      articleListPanel.add(articleScrollPane);

      allPanel.add(articleListPanel, BorderLayout.CENTER);

      abschliessenPanel = new JPanel();
      abschliessenPanel.setLayout(new FlowLayout());
      abschliessenButton = new JButton("Bestellung abschließen");
      abschliessenButton.setEnabled(false);
      abschliessenButton.addActionListener(this);
      abschliessenButton.setAlignmentX(JComponent.CENTER_ALIGNMENT);
      abschliessenPanel.add(abschliessenButton);

      verwerfenButton = new JButton("Verwerfen");
      verwerfenButton.setEnabled(false);
      verwerfenButton.addActionListener(this);
      verwerfenButton.setAlignmentX(JComponent.CENTER_ALIGNMENT);
      abschliessenPanel.add(verwerfenButton);

      JLabel filterLabel = new JLabel("Filter:");
      filterLabel.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
      abschliessenPanel.add(filterLabel);
      filterField = new JTextField("");
      filterStr = "";
      filterField.setColumns(20);
      removeDefaultKeyBindings(filterField);
      filterField.getDocument().addDocumentListener(this);
      filterField.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
      abschliessenPanel.add(filterField);
      emptyFilterButton = new JButton("x");
      emptyFilterButton.addActionListener(this);
      abschliessenPanel.add(emptyFilterButton);
      allPanel.add(abschliessenPanel, BorderLayout.SOUTH);
    }

    void emptyTable(){
      data = new Vector< Vector<Object> >();
      displayData = new Vector< Vector<Object> >();
      displayIndices = new Vector<Integer>();
      colors = new Vector<String>();
      initiateTable();
      artikelIDs = new Vector<Integer>();
      positions = new Vector<Integer>();
      removeButtons = new Vector<JButton>();
    }

    private void clearAll(){
        data.clear();
        displayData.clear();
        displayIndices.clear();
        artikelIDs.clear();
        colors.clear();
        positions.clear();
        removeButtons.clear();
        selBestellNr = -1;
        selJahr = -1;
        selKW = -1;

        setButtonsEnabled();
    }

    protected void updateAll(){
	     this.remove(allPanel);
	     this.revalidate();
       // create table anew
       showAll();
       //updateTable();
       setButtonsEnabled(); // for abschliessenButton
    }

    private void updateTable(){
        applyFilter(filterStr, displayData, displayIndices);
        articleListPanel.remove(articleScrollPane);
	      articleListPanel.revalidate();

        initiateTable();

        articleScrollPane = new JScrollPane(orderTable);
        articleListPanel.add(articleScrollPane);
        setButtonsEnabled();
    }

    protected int numberOfRows() {
        return data.size();
    }





    // CSV export:
    public void doCSVBackup() {
        String backupFilename =
            System.getProperty("user.home")+bc.fileSep+".Weltladenkasse_Bestellung_"+selTyp+".backup";
        File file = new File(backupFilename);

        String fileStr = "";
        // general infos:
        fileStr += "#BestellNr;Jahr;KW"+bc.lineSep;
        fileStr += selBestellNr + bc.delimiter;
        fileStr += selJahr + bc.delimiter;
        fileStr += selKW + bc.lineSep;
        // format of csv file:
        fileStr += "#Lieferant;Art.-Nr.;Artikelname;VK-Preis;VPE;Stueck;Beliebtheit;color;artikelID"+bc.lineSep;
        for (int i=data.size()-1; i>=0; i--){
            String lieferant = data.get(i).get(1).toString();
            String nummer = data.get(i).get(2).toString();
            String name = data.get(i).get(3).toString();
            String vkp = "";
            try {
                vkp = data.get(i).get(4).toString();
            } catch (NullPointerException ex) { }
            String vpe = "";
            try {
                vpe = data.get(i).get(5).toString();
            } catch (NullPointerException ex) { }
            String stueck = data.get(i).get(6).toString();
            String beliebt = data.get(i).get(7).toString();
            String color = colors.get(i);
            String artikelID = artikelIDs.get(i).toString();

            fileStr += lieferant + bc.delimiter;
            fileStr += nummer + bc.delimiter;
            fileStr += name + bc.delimiter;
            fileStr += vkp + bc.delimiter;
            fileStr += vpe + bc.delimiter;
            fileStr += stueck + bc.delimiter;
            fileStr += beliebt + bc.delimiter;
            fileStr += color + bc.delimiter;
            fileStr += artikelID + bc.lineSep;
        }

        //System.out.println(fileStr);

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(fileStr);
        } catch (Exception ex) {
            logger.error("Error writing to file {}", file.getName());
            logger.error("Exception:", ex);
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception ex) {
                logger.error("Error closing file {}", file.getName());
                logger.error("Exception:", ex);
            }
        }
    }


    private void doCSVBackupReadin() {
        // clear possible previous order
        clearAll();
        updateTable();

        String backupFilename =
            System.getProperty("user.home")+bc.fileSep+".Weltladenkasse_Bestellung_"+selTyp+".backup";
        File file = new File(backupFilename);

        try {
            // use Reader classes for text files:
            BufferedReader in = new BufferedReader(new FileReader(file)); // Lesen einer Textdatei mit Default Zeichensatz-Codierung, see http://www.wsoftware.de/practices/charsets.html
            String line;
            // parse general info at top:
            while ( (line = in.readLine()) != null) {
                line = line.replaceAll("#.*",""); // remove commented lines
                // get the fields
                String[] fields = line.split(bc.delimiter);
                if (fields.length < 3 ){
                    continue;
                }
                selBestellNr = Integer.parseInt(fields[0]);
                selJahr = Integer.parseInt(fields[1]);
                selKW = Integer.parseInt(fields[2]);
                break;
            }
            // parse articles:
            while ( (line = in.readLine()) != null) {
                line = line.replaceAll("#.*",""); // remove commented lines
                // get the fields
                String[] fields = line.split(bc.delimiter);
                if (fields.length < 8 ){
                    continue;
                }

                String lieferant = fields[0];
                String nummer = fields[1];
                String name = fields[2];
                String vkp = fields[3];
                String vpe = fields[4];
                Integer stueck = Integer.parseInt(fields[5]);
                Integer beliebt = Integer.parseInt(fields[6]);
                String color = fields[7];
                Integer artikelID = Integer.parseInt(fields[8]);

                hinzufuegen(artikelID, lieferant, nummer, name,
                        vkp, vpe, stueck, beliebt, color);
            }
            updateAll();
        } catch (FileNotFoundException ex) {
            logger.info("No backup file found. No backed up order loaded.");
        } catch (IOException ex) {
            logger.error("Exception:", ex);
        }
    }



    private void renameCSVBackupFile(String newTyp) {
        String oldBackupFilename =
            System.getProperty("user.home")+bc.fileSep+".Weltladenkasse_Bestellung_"+selTyp+".backup";
        File oldFile = new File(oldBackupFilename);
        String newBackupFilename =
            System.getProperty("user.home")+bc.fileSep+".Weltladenkasse_Bestellung_"+newTyp+".backup";
        File newFile = new File(newBackupFilename);
        if ( newFile.exists() ){
            int answer = JOptionPane.showConfirmDialog(this,
                    "Es gibt schon eine Bestellung vom Typ '"+newTyp+"'.\n"+
                    "Überschreiben?", "Warnung",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.NO_OPTION){
                return;
            } else {
                // try to delete the existing file so that rename might work
                if ( !newFile.delete() ){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Andere Bestellung konnte nicht überschrieben werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }
        // Rename backup file
        if ( !oldFile.renameTo(newFile) ) {
            JOptionPane.showMessageDialog(this,
                    "Fehler: Bestellungs-Typ konnte nicht geändert werden.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }
        selTyp = newTyp;
        doCSVBackupReadin();
        //asPanel.emptyArtikelBox();
    }


    private void setPriceField() {
    }

    protected void setButtonsEnabled() {
        if (preisField.getText().length() > 0) {
            hinzufuegenButton.setEnabled(true);
        } else {
            hinzufuegenButton.setEnabled(false);
        }
        if (artikelIDs.size() > 0) {
            abschliessenButton.setEnabled(true);
            verwerfenButton.setEnabled(true);
        } else {
            abschliessenButton.setEnabled(false);
            verwerfenButton.setEnabled(false);
        }
    }

    protected void updateAnzahlColor(Integer vpe) {
        if (anzahlField.getText().length() == 0){
            return;
        }
        if (vpe <= 0){
            anzahlField.setForeground(Color.black);
        } else {
            Integer stueck = Integer.parseInt(anzahlField.getText());
            if (stueck < vpe){
                anzahlField.setForeground(Color.red);
            } else {
                anzahlField.setForeground(Color.green.darker().darker());
            }
        }
    }

    private void updateAnzahlSpinner(Integer vpe) {
        if (vpe > 0){
            logger.trace("updateAnzahlSpinner at work.");
            Integer nvpe = (Integer)vpeSpinner.getValue();
            Integer stueck = Integer.valueOf(nvpe * vpe);
            if (
                    stueck >= (Integer)((SpinnerNumberModel)anzahlSpinner.getModel()).getMinimum() &&
                    stueck <= (Integer)((SpinnerNumberModel)anzahlSpinner.getModel()).getMaximum()
               ){
                this.vpeOrAnzahlIsChanged = true;
                    anzahlSpinner.setValue(stueck);
                this.vpeOrAnzahlIsChanged = false;
            }
        }
    }

    private void updateVPESpinner(Integer vpe) {
        if (vpe > 0){
            logger.trace("updateVPESpinner at work.");
            Integer stueck = (Integer)anzahlSpinner.getValue();
            Integer nvpe = Integer.valueOf(stueck / vpe);
            this.vpeOrAnzahlIsChanged = true;
                vpeSpinner.setValue(nvpe);
                selectedNumberOfVPEs = (Integer)vpeSpinner.getValue();
            this.vpeOrAnzahlIsChanged = false;
        }
    }

    protected void hinzufuegen(Integer artikelID, String lieferant,
            String artikelNummer, String artikelName, String vkp, String vpe,
            Integer stueck, Integer beliebt, String color) {
        artikelIDs.add(0, artikelID);
        colors.add(0, color);
        Integer lastPos = 0;
        try {
            lastPos = positions.firstElement();
        } catch (NoSuchElementException ex) { }
        positions.add(0, lastPos+1);
        removeButtons.add(0, new JButton("-"));
        removeButtons.firstElement().addActionListener(this);

        Vector<Object> row = new Vector<Object>();
            row.add(positions.firstElement());
            row.add(lieferant); row.add(artikelNummer); row.add(artikelName);
            row.add(vkp); row.add(vpe); row.add(stueck);
            row.add(beliebt);
            row.add(removeButtons.firstElement());
        data.add(0, row);

        displayData = new Vector< Vector<Object> >(data);
        initiateDisplayIndices();

        // scroll the table to the bottom
        //articleScrollPane.setScrollPosition(0, orderTable.getHeight());
    }

    private void fuegeArtikelHinzu(Integer stueck) {
        if (asPanel.artikelBox.getItemCount() != 1 || asPanel.nummerBox.getItemCount() != 1){
            logger.error("Error: article not selected unambiguously.");
            return;
        }
        Artikel a = getArticle(selectedArticleID);
        String artikelNummer = a.getNummer();
        String artikelName = a.getName();
        String lieferant = getShortLieferantName(selectedArticleID);
        String vpe = vpeField.getText();
        Integer vpeInt = vpe.length() > 0 ? Integer.parseInt(vpe) : 0;
        String artikelPreis = bc.priceFormatterIntern( preisField.getText() );
        artikelPreis = bc.decimalMark(artikelPreis)+' '+bc.currencySymbol;
        String artikelMwSt = getVAT(selectedArticleID);
        artikelMwSt = bc.vatFormatter(artikelMwSt);
        Integer beliebt = a.getBeliebt();
        Boolean sortiment = a.getSortiment();
        String color = sortiment ? "default" : "gray";

        hinzufuegen(selectedArticleID, lieferant, artikelNummer, artikelName,
                artikelPreis, vpe, stueck, beliebt, color);
        updateAll();
        asPanel.emptyArtikelBox();

        // save a CSV backup to hard disk
        doCSVBackup();
    }

    private Vector<Object> abschliessen() {
        int bestellNr = -1;
        String typ = "";
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt;
            if (selBestellNr > 0){
                pstmt = connection.prepareStatement("INSERT INTO bestellung "+
                    "SET bestell_nr = ?, typ = ?, bestell_datum = NOW(), jahr = ?, kw = ?");
            } else {
                pstmt = connection.prepareStatement("INSERT INTO bestellung "+
                    "SET typ = ?, bestell_datum = NOW(), jahr = ?, kw = ?");
            }
            int fieldCounter = 1;
            if (selBestellNr > 0){
                pstmtSetInteger(pstmt, fieldCounter, selBestellNr); fieldCounter++;
            }
            typ = typField.getText();
            pstmt.setString(fieldCounter, typ); fieldCounter++;
            pstmtSetInteger(pstmt, fieldCounter, Integer.parseInt(jahrField.getText())); fieldCounter++;
            pstmtSetInteger(pstmt, fieldCounter, Integer.parseInt(kwField.getText())); fieldCounter++;
            int result = pstmt.executeUpdate();
            pstmt.close();
            if (result == 0){
                JOptionPane.showMessageDialog(this,
                        "Fehler: Bestellung konnte nicht abgespeichert werden.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
            if (selBestellNr > 0){
                bestellNr = selBestellNr;
            } else {
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT MAX(bestell_nr) FROM bestellung"
                        );
                rs.next(); bestellNr = rs.getInt(1); rs.close();
                stmt.close();
            }
            for (int i=0; i<artikelIDs.size(); i++){
                pstmt = connection.prepareStatement(
                        "INSERT INTO bestellung_details SET bestell_nr = ?, "+
                        "typ = ?, position = ?, artikel_id = ?, stueckzahl = ?"
                        );
                pstmtSetInteger(pstmt, 1, bestellNr);
                pstmt.setString(2, typ);
                pstmtSetInteger(pstmt, 3, positions.get(i));
                pstmtSetInteger(pstmt, 4, artikelIDs.get(i));
                pstmtSetInteger(pstmt, 5, (Integer)data.get(i).get( columnLabels.indexOf("Stückzahl") ));
                result = pstmt.executeUpdate();
                pstmt.close();
                if (result == 0){
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Artikel mit ID "+artikelIDs.get(i)+" konnte nicht abgespeichert werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            JOptionPane.showMessageDialog(this,
                    "Fehler: Bestellung konnte nicht vollständig abgespeichert werden.\n"+
                    "Keine Verbindung zum Datenbank-Server?\n"+
                    "Fehlermeldung: "+ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            bestellNr = -1;
        }
        Vector<Object> bestNrUndTyp = new Vector<Object>();
            bestNrUndTyp.add(bestellNr); bestNrUndTyp.add(typ);
        return bestNrUndTyp;
    }

    private void verwerfen() {
        clearAll();
        updateAll();
        asPanel.emptyArtikelBox();
        // save a CSV backup to hard disk
        doCSVBackup();
    }

    /**
     * A class implementing ArticleSelectUser must have this method.
     */
    public void updateSelectedArticleID(int selectedArticleID) {
        this.selectedArticleID = selectedArticleID;
    }

    /**
     *    * Each non abstract class that implements the TableModelListener
     *      must have these methods.
     *
     *    @param e the table model event.
     **/
    public void tableChanged(TableModelEvent e) {
        // save a CSV backup to hard disk
        doCSVBackup();
    }

    /**
     *    * Each non abstract class that implements the DocumentListener
     *      must have these methods.
     *
     *    @param e the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
        if (e.getDocument() == typField.getDocument()) {
            selTyp = typField.getText();
            doCSVBackupReadin();
            typField.requestFocus();
        }
        if (e.getDocument() == preisField.getDocument()) {
            setButtonsEnabled();
            return;
        }
        if (e.getDocument() == anzahlField.getDocument()) {
            if (this.vpeOrAnzahlIsChanged) return;
            //System.out.println("anzahlField DocumentListener fired.");
            //System.out.println("anzahlField.getText(): "+anzahlField.getText());
            //System.out.println("anzahlSpinner.getValue(): "+anzahlSpinner.getValue());
            String vpe = getVPE(selectedArticleID);
            Integer vpeInt = vpe.length() > 0 ? Integer.parseInt(vpe) : 0;
            updateAnzahlColor(vpeInt);
            updateVPESpinner(vpeInt);
            return;
        }
        if (e.getDocument() == vpeSpinnerField.getDocument()) {
            if (this.vpeOrAnzahlIsChanged) return;
            if ( (Integer)vpeSpinner.getValue() == selectedNumberOfVPEs ){
                return; // return if there was no change (e.g. only focus on spinner)
            } else {
                selectedNumberOfVPEs = (Integer)vpeSpinner.getValue();
            }
            //System.out.println("vpeSpinnerField DocumentListener fired.");
            //System.out.println("vpeSpinnerField DocumentListener: vpeOrAnzahlIsChanged = "+vpeOrAnzahlIsChanged);
            //System.out.println("anzahlField.getText(): "+anzahlField.getText());
            //System.out.println("anzahlSpinner.getValue(): "+anzahlSpinner.getValue());
            String vpe = getVPE(selectedArticleID);
            Integer vpeInt = vpe.length() > 0 ? Integer.parseInt(vpe) : 0;
            updateAnzahlSpinner(vpeInt);
            return;
        }
        if (e.getDocument() == filterField.getDocument()) {
            String oldFilterStr = new String(filterStr);
            filterStr = filterField.getText();
            if ( !filterStr.contains(oldFilterStr) ){
                // user has deleted from, not added to the filter string, reset the displayData
                displayData = new Vector< Vector<Object> >(data);
                initiateDisplayIndices();
            }
            updateTable();
            return;
        }
    }
    public void removeUpdate(DocumentEvent e) {
        insertUpdate(e);
    }
    public void changedUpdate(DocumentEvent e) {
	      // Plain text components do not fire these events
    }

    private void initiateDisplayIndices() {
        displayIndices = new Vector<Integer>();
        for (int i=0; i<data.size(); i++){
            displayIndices.add(i);
        }
    }

    void refreshPositionsInData() {
        for (int i=0; i<positions.size(); i++){
            data.get(i).set(0, positions.get(i));
        }
    }

    private static Vector<String> showChangeTypDialog(String oldTyp) {
        final JTextField newTypField = new JTextField(oldTyp);
        JOptionPane jop = new JOptionPane(new Object[]{"Neuer Bestellungs-Typ:", newTypField},
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = jop.createDialog("Bitte neuen Bestellungs-Typ eingeben");
        dialog.addWindowFocusListener(new WindowAdapter(){
            @Override
            public void windowGainedFocus(WindowEvent e){
                newTypField.requestFocusInWindow();
            }
        });
        newTypField.addFocusListener(new FocusListener() {
            public void focusGained( FocusEvent e ) {
                newTypField.selectAll();
            }
            public void focusLost( FocusEvent e ) {
                if ( newTypField.getText().length() == 0 ) {
                    newTypField.requestFocusInWindow();
                }
            }
        });
        dialog.setVisible(true);
        int result = (Integer)jop.getValue();
        dialog.dispose();

        Vector<String> okTyp = new Vector<String>(2);
        String ok;
        if (result == JOptionPane.OK_OPTION){ ok = "OK"; }
        else { ok = "CANCEL"; }
        String typ = new String(newTypField.getText());
        okTyp.add(ok);
        okTyp.add(typ);
        return okTyp;
    }

    private void showEditDialog() {
      Artikel article = getArticle(selectedArticleID);
      Vector<Artikel> selectedArticles = new Vector<Artikel>();
      selectedArticles.add(article);

      asPanel.showEditDialog(selectedArticles);

      boolean variablerPreis = getVariablePriceBool(selectedArticleID);
      if ( ! variablerPreis ){
        String artikelPreis = getRecSalePrice(selectedArticleID);
        if (artikelPreis == null || artikelPreis.equals("")){
          artikelPreis = getSalePrice(selectedArticleID);
        }
        if (artikelPreis == null)
        artikelPreis = "";
        preisField.getDocument().removeDocumentListener(this);
        preisField.setText( bc.decimalMark(artikelPreis) );
        preisField.getDocument().addDocumentListener(this);
      } else {
        preisField.getDocument().removeDocumentListener(this);
        preisField.setText("");
        preisField.getDocument().addDocumentListener(this);
        preisField.setEditable(true);
      }
    }

    /**
    *    * Each non abstract class that implements the ActionListener
    *      must have this method.
    *
    *    @param e the action event.
    **/
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == hinzufuegenButton){
        Integer stueck = (Integer)anzahlSpinner.getValue();
        fuegeArtikelHinzu(stueck);
        return;
      }
      if (e.getSource() == changeButton){
        showEditDialog();
        return;
      }
      if (e.getSource() == changeTypButton){
        Vector<String> okTyp = showChangeTypDialog(typField.getText());
        if (okTyp.get(0) == "OK"){
          renameCSVBackupFile(okTyp.get(1));
        }
      }
      if (e.getSource() == abschliessenButton){
        Vector<Object> bestellNrUndTyp = abschliessen();
        if ( (Integer)bestellNrUndTyp.get(0) > 0 ){ // if abschliessen was successful
          verwerfen();
          // update the BestellAnzeige tab
          tabbedPane.recreateTabbedPane();
          // switch to BestellAnzeige tab
          tabbedPane.switchToBestellAnzeige(bestellNrUndTyp);
        }
        return;
      }
      if (e.getSource() == verwerfenButton){
        int answer = JOptionPane.showConfirmDialog(this,
        "Wirklich verwerfen?", "Bestellung löschen",
        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
          verwerfen();
        }
        return;
      }
      int removeIndex = -1;
      for (int i=0; i<removeButtons.size(); i++){
        if ( e.getSource() == removeButtons.get(i) ){
          removeIndex = i;
          break;
        }
      }
      if (removeIndex > -1){
        data.remove(removeIndex);
        artikelIDs.remove(removeIndex);
        colors.remove(removeIndex);
        removeButtons.remove(removeIndex);

        positions.remove(removeIndex);
        for (int i=removeIndex-1; i>=0; i--){
          positions.set(i, positions.get(i)-1);
        }
        refreshPositionsInData();

        int removeRow = displayIndices.indexOf(removeIndex);
        displayData.remove(removeRow);
        displayIndices.remove(removeRow);
        // propagate change in displayIndices:
        for (int i=removeRow; i<displayIndices.size(); i++){
          displayIndices.set(i, displayIndices.get(i)-1);
        }

        //updateAll();
        //asPanel.emptyArtikelBox();
        updateTable();

        // save a CSV backup to hard disk
        doCSVBackup();
        return;
      }
      if (e.getSource() == emptyFilterButton){
        filterField.setText("");
        filterField.requestFocus();
        return;
      }
    }
  }
