package Weltladenkasse;

// Basic Java stuff:
import java.util.*; // for Vector
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
//import javax.swing.JTextArea;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.*; // for AbstractDocument, JTextComponent
import javax.swing.event.*;
//import java.beans.PropertyChangeEvent;
//import java.beans.PropertyChangeListener;

// JCalendarButton
import jcalendarbutton.org.JCalendarButton;
//import java.util.Calendar;
//import java.util.Date;

import WeltladenDB.WindowContent;
import WeltladenDB.AnyJComponentJTable;
import WeltladenDB.CurrencyDocumentFilter;
import WeltladenDB.JComponentCellRenderer;
import WeltladenDB.JComponentCellEditor;

public class Kassenstand extends WindowContent implements ChangeListener, DocumentListener, ItemListener {
    // Attribute:
    private int kassenstaendeProSeite = 25;
    private int currentPage = 1;
    private int totalPage;
    private String filterStr = "";

    private boolean showRechnungen = false;
    private JCheckBox rechnungsCheckBox;

    // Text Fields
    private JTextField neuerKassenstandField;
    private JTextField differenzField;
    private JTextField kommentarField;
    //Formats to format and parse numbers
    private JButton returnButton;

    // for date change
    private JSpinner startSpinner;
    private JSpinner endSpinner;
    private SpinnerDateModel startDateModel;
    private SpinnerDateModel endDateModel;
    private JCalendarButton calButtStart;
    private JCalendarButton calButtEnd;
    private Date oneDayBeforeEarliestDate;
    private Date earliestDate;
    private Date latestDate;
    private JButton changeDateButton;
    private JButton resetButton;
    private JButton prevButton;
    private JButton nextButton;

    // The bottom panel which holds button.
    private JPanel allPanel;
    private JPanel historyPanel;
    // The table holding the invoices. This is "anonymously subclassed" and two method are overridden
    private AnyJComponentJTable myTable;

    private Vector<Vector> data;
    private Vector<String> columnLabels;
    private String kassenstandZahl;
    private int kassenstandZahlInt;

    // Methoden:

    /**
     *    The constructor.
     *       */
    public Kassenstand(Connection conn, MainWindow mw)
    {
	super(conn, mw);

	fillDataArray(filterStr);
	queryEarliestKassenstand();
	initiateSpinners();
	showAll();
    }

    private void queryEarliestKassenstand(){
	int day = 0;
        int month = 0;
        int year = 0;
	try {
	    // Create statement for MySQL database
	    Statement stmt = this.conn.createStatement();
	    // Run MySQL command
	    ResultSet rs = stmt.executeQuery(
		    "SELECT DAY(MIN(buchungsdatum)), MONTH(MIN(buchungsdatum)), " +
		    "YEAR(MIN(buchungsdatum)) FROM kassenstand"
		    );
	    // Now do something with the ResultSet ...
	    rs.next();
	    day = rs.getInt(1);
	    month = rs.getInt(2);
	    year = rs.getInt(3);
	    rs.close();
	    stmt.close();
	} catch (SQLException ex) {
	    System.out.println("Exception: " + ex.getMessage());
	    ex.printStackTrace();
	}
	Calendar calendar = Calendar.getInstance();
	calendar.set(Calendar.YEAR, year);
	calendar.set(Calendar.MONTH, month-1);
	calendar.set(Calendar.DAY_OF_MONTH, day-1); // for strange reasons, we need day-1
	oneDayBeforeEarliestDate = calendar.getTime();
	calendar.set(Calendar.DAY_OF_MONTH, day);
	earliestDate = calendar.getTime();
	latestDate = new Date(); // current date
        if ( year == 0 ){
            earliestDate = new Date();
            oneDayBeforeEarliestDate = new Date();
        }
    }

    private void initiateSpinners(){
        startDateModel = new SpinnerDateModel(earliestDate, // Startwert
                                     oneDayBeforeEarliestDate, // kleinster Wert
                                     latestDate, // groesster Wert
                                     Calendar.YEAR);//ignored for user input
        startSpinner = new JSpinner(startDateModel);
        startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, "dd/MM/yyyy"));
	startSpinner.addChangeListener(this);
	calButtStart = new JCalendarButton(earliestDate);
	calButtStart.addChangeListener(this);
        endDateModel = new SpinnerDateModel(latestDate, // Startwert
                                     oneDayBeforeEarliestDate, // kleinster Wert
                                     latestDate, // groesster Wert
                                     Calendar.YEAR);//ignored for user input
        endSpinner = new JSpinner(endDateModel);
        endSpinner.setEditor(new JSpinner.DateEditor(endSpinner, "dd/MM/yyyy"));
	endSpinner.addChangeListener(this);
	calButtEnd = new JCalendarButton(latestDate);
	calButtEnd.addChangeListener(this);
    }

    void fillDataArray(String filterStr){
	data = new Vector<Vector>();
	columnLabels = new Vector<String>();
	columnLabels.add("Buchungsdatum"); columnLabels.add("Neuer Kassenstand"); columnLabels.add("Manuell?");
	columnLabels.add("Rechnungsnr."); columnLabels.add("Erläuternder Kommentar");
	try {
	    // Create statement for MySQL database
	    Statement stmt = this.conn.createStatement();
            String ausblendeString = new String();
            if (showRechnungen){
                ausblendeString = "TRUE ";
            } else {
                ausblendeString = "manuell = TRUE ";
            }
	    // Run MySQL command
	    ResultSet rs = stmt.executeQuery(
		    "SELECT DATE_FORMAT(buchungsdatum,'"+dateFormatSQL+"'), neuer_kassenstand, " +
		    "manuell, rechnungs_nr, kommentar " +
		    "FROM kassenstand " +
                    "WHERE " + ausblendeString +
		    filterStr +
		    "ORDER BY buchungsdatum DESC " +
		    "LIMIT " + (currentPage-1)*kassenstaendeProSeite + "," + kassenstaendeProSeite
		    );
	    // Now do something with the ResultSet ...
	    while (rs.next()) {
		Vector<String> row = new Vector<String>();
		row.add(rs.getString(1)); row.add(rs.getString(2)+" "+currencySymbol);
		row.add(rs.getString(3).contentEquals("0") ? "Nein" : "Ja"); row.add(rs.getString(4));
		row.add(rs.getString(5));
		// change dots to commas
		row.set(1, row.get(1).replace('.',','));
		data.add(row);
	    }
	    rs.close();
	    rs = stmt.executeQuery(
		    "SELECT COUNT(buchungsdatum) FROM kassenstand " +
		    filterStr
		    );
	    // Now do something with the ResultSet ...
	    rs.next();
	    kassenstandZahl = rs.getString(1);
	    kassenstandZahlInt = Integer.parseInt(kassenstandZahl);
	    totalPage = kassenstandZahlInt/kassenstaendeProSeite + 1;
	    rs.close();
	    stmt.close();
	} catch (SQLException ex) {
	    System.out.println("Exception: " + ex.getMessage());
	    ex.printStackTrace();
	}
	myTable = new AnyJComponentJTable(data, columnLabels);
//	myTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
//	myTable.setFillsViewportHeight(true);
    }

    void showAll(){
	allPanel = new JPanel();
	allPanel.setLayout(new BoxLayout(allPanel, BoxLayout.Y_AXIS));

	allPanel.add(Box.createRigidArea(new Dimension(0,10))); // add empty space
	JPanel kassenstandPanel = new JPanel();
	kassenstandPanel.setLayout(new FlowLayout());
	JLabel kassenstandLabel = new JLabel("Aktueller Kassenstand:");
	kassenstandPanel.add(kassenstandLabel);
	JTextField kassenstandField = new JTextField("", 10);
        kassenstandField.setHorizontalAlignment(JTextField.RIGHT);
	kassenstandField.setText(mainWindow.getKassenstand());
	kassenstandField.setEditable(false);
	kassenstandLabel.setLabelFor(kassenstandField);
	kassenstandPanel.add(kassenstandField);
	allPanel.add(kassenstandPanel);

	JPanel manuellAendernPanel = new JPanel();
	manuellAendernPanel.setLayout(new BoxLayout(manuellAendernPanel, BoxLayout.Y_AXIS));
	manuellAendernPanel.setBorder(BorderFactory.createTitledBorder("Kassenstand manuell ändern"));
	//
	JPanel kassenstandAendernPanel = new JPanel();
	kassenstandAendernPanel.setLayout(new FlowLayout());
	    JLabel neuerKassenstandLabel = new JLabel("Neuer Kassenstand:");
	    kassenstandAendernPanel.add(neuerKassenstandLabel);
//	    neuerKassenstandField = new JFormattedTextField(amountFormat);
	    neuerKassenstandField = new JTextField();
	    neuerKassenstandField.setColumns(10);
            neuerKassenstandField.setHorizontalAlignment(JTextField.RIGHT);
	    neuerKassenstandField.getDocument().addDocumentListener(this);
	    CurrencyDocumentFilter df = new CurrencyDocumentFilter();
	    ((AbstractDocument)neuerKassenstandField.getDocument()).setDocumentFilter(df);
	    neuerKassenstandLabel.setLabelFor(neuerKassenstandField);
	    kassenstandAendernPanel.add(neuerKassenstandField);
	    kassenstandAendernPanel.add(new JLabel(currencySymbol));
	    //
	    kassenstandAendernPanel.add(Box.createRigidArea(new Dimension(12,0)));
	    //
	    JLabel differenzLabel = new JLabel("ODER Differenz:");
	    kassenstandAendernPanel.add(differenzLabel);
//	    differenzField = new JFormattedTextField(amountFormat);
	    differenzField = new JTextField();
	    differenzField.setColumns(10);
            differenzField.setHorizontalAlignment(JTextField.RIGHT);
	    differenzField.getDocument().addDocumentListener(this);
	    ((AbstractDocument)differenzField.getDocument()).setDocumentFilter(df);
	    differenzLabel.setLabelFor(differenzField);
	    kassenstandAendernPanel.add(differenzField);
	    kassenstandAendernPanel.add(new JLabel(currencySymbol));
	manuellAendernPanel.add(kassenstandAendernPanel);
	//
	JPanel kommentarPanel = new JPanel();
	kommentarPanel.setLayout(new FlowLayout());
	    JLabel kommentarLabel = new JLabel("Erläuternder Kommentar:");
	    kommentarPanel.add(kommentarLabel);
	    kommentarField = new JTextField("", 25);
	    kommentarField.getDocument().addDocumentListener(this);
	    DocumentSizeFilter dsf = new DocumentSizeFilter(70);
	    ((AbstractDocument)kommentarField.getDocument()).setDocumentFilter(dsf);
	    kommentarLabel.setLabelFor(kommentarField);
	    kommentarPanel.add(kommentarField);
	    //
	    returnButton = new JButton("Abschicken");
	    returnButton.addActionListener(this);
	    returnButton.setEnabled(false);
	    kommentarPanel.add(returnButton);
	manuellAendernPanel.add(kommentarPanel);
	allPanel.add(manuellAendernPanel);

	fillDataArray(filterStr);
	showTable();

	this.add(allPanel, BorderLayout.CENTER);
    }

    void showTable(){
	myTable.setDefaultRenderer( JComponent.class, new JComponentCellRenderer() );
	myTable.setDefaultEditor( JComponent.class, new JComponentCellEditor() );
        myTable.setAutoCreateRowSorter(true);
//	myTable.setBounds(71,53,150,100);
//	myTable.setToolTipText("Tabelle kann nur gelesen werden.");
	setTableProperties(myTable);
//	myTable.setAutoResizeMode(5);

	historyPanel = new JPanel();
	historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
	historyPanel.setBorder(BorderFactory.createTitledBorder("Verlauf der Kassenstände"));

	    JPanel datePanel = new JPanel();
	    datePanel.setLayout(new FlowLayout());
	    //datePanel.setMaximumSize(new Dimension(1024,30));
	    JLabel startDateLabel = new JLabel("Startdatum:");
	    datePanel.add(startDateLabel);
	    startDateLabel.setLabelFor(startSpinner);
	    datePanel.add(startSpinner);
	    datePanel.add(calButtStart);
	    datePanel.add(Box.createRigidArea(new Dimension(10,0))); // add empty space
	    JLabel endDateLabel = new JLabel("Enddatum:");
	    datePanel.add(endDateLabel);
	    endDateLabel.setLabelFor(endSpinner);
	    datePanel.add(endSpinner);
	    datePanel.add(calButtEnd);
	    datePanel.add(Box.createRigidArea(new Dimension(10,0))); // add empty space
	    changeDateButton = new JButton(new ImageIcon( WindowContent.class.getResource("/resources/icons/refreshButtonSmall.gif") ));
	    changeDateButton.addActionListener(this);
	    datePanel.add(changeDateButton);
	    resetButton = new JButton("Zurücksetzen");
	    resetButton.addActionListener(this);
	    datePanel.add(resetButton);
            historyPanel.add(datePanel);

            JPanel checkBoxPanel = new JPanel();
            rechnungsCheckBox = new JCheckBox("Rechnungen anzeigen");
            rechnungsCheckBox.setSelected(showRechnungen);
            rechnungsCheckBox.addItemListener(this);
            //rechnungsCheckBox.addActionListener(this);
            checkBoxPanel.add(rechnungsCheckBox);
            historyPanel.add(checkBoxPanel);

	    JPanel pageChangePanel = new JPanel();
	    pageChangePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
	    //	pageChangePanel.setMaximumSize(new Dimension(1024,30));
	    prevButton = new JButton("<<");
	    if (this.currentPage <= 1)
		prevButton.setEnabled(false);
	    nextButton = new JButton(">>");
	    if (this.currentPage >= totalPage)
		nextButton.setEnabled(false);
	    pageChangePanel.add(prevButton);
	    pageChangePanel.add(nextButton);
	    prevButton.addActionListener(this);
	    nextButton.addActionListener(this);
	    int currentPageMin = (currentPage-1)*kassenstaendeProSeite + 1;
	    int currentPageMax = kassenstaendeProSeite*currentPage;
	    currentPageMax = (currentPageMax <= kassenstandZahlInt) ? currentPageMax : kassenstandZahlInt;
	    JLabel header = new JLabel("Seite "+ currentPage +" von "+ totalPage + ", Kassenstand "+
		currentPageMin + " bis "+ currentPageMax +" von "+ kassenstandZahlInt);
	    pageChangePanel.add(header);
	    historyPanel.add(pageChangePanel);

	    JScrollPane scrollPane = new JScrollPane(myTable);
	    historyPanel.add(scrollPane);

	allPanel.add(historyPanel);
    }

    private void updateAll(String filterStr){
	this.remove(allPanel);
	this.revalidate();
	fillDataArray(filterStr);
	showAll();
    }

    private void updateTable(String filterStr){
	allPanel.remove(historyPanel);
	allPanel.revalidate();
	fillDataArray(filterStr);
	showTable();
    }

    private void setTableProperties(AnyJComponentJTable table){
	// Spalteneigenschaften:
//	table.getColumnModel().getColumn(0).setPreferredWidth(10);
	TableColumn buchungsDatum = table.getColumn("Buchungsdatum");
	buchungsDatum.setCellRenderer(rechtsAusrichter);
	buchungsDatum.setPreferredWidth(50);
	TableColumn betrag = table.getColumn("Neuer Kassenstand");
	betrag.setCellRenderer(rechtsAusrichter);
	TableColumn manuell = table.getColumn("Manuell?");
	manuell.setCellRenderer(rechtsAusrichter);
	manuell.setPreferredWidth(5);
	TableColumn rechnungsNr = table.getColumn("Rechnungsnr.");
	rechnungsNr.setCellRenderer(rechtsAusrichter);
	rechnungsNr.setPreferredWidth(8);
	TableColumn kommentarCol = table.getColumn("Erläuternder Kommentar");
	kommentarCol.setCellRenderer(rechtsAusrichter);
	kommentarCol.setPreferredWidth(70);
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
	if (e.getSource() == returnButton){
	    // eigentlich unmoeglich:
	    if (neuerKassenstandField.getText().length() > 0 && differenzField.getText().length() > 0){
		JOptionPane.showMessageDialog(this, "Sowohl neuer Kassenstand als auch Differenz eingegeben!",
			"Fehler", JOptionPane.ERROR_MESSAGE);
		neuerKassenstandField.setText("");
		differenzField.setText("");
		kommentarField.setText("");
	    }
	    String kommentar = kommentarField.getText();
	    // neuerKassenstand mode:
	    if (neuerKassenstandField.getText().length() > 0){
		String text = neuerKassenstandField.getText();
		if ( ! text.matches(".*[0-9].*") ){ // if contains no digit: throw error
		    JOptionPane.showMessageDialog(this, "Fehlerhafter Betrag eingegeben!",
			"Fehler", JOptionPane.ERROR_MESSAGE);
		    neuerKassenstandField.setText("");
		    differenzField.setText("");
		    kommentarField.setText("");
		    return;
		}
		text = priceFormatter(text)+" "+currencySymbol;
		int answer = JOptionPane.showConfirmDialog(this,
			"Kassenstand wirklich auf "+text+" setzen "+
			"mit Kommentar \n\""+kommentar+"\"?", "Kassenstand ändern",
			JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (answer == JOptionPane.YES_OPTION){
		    BigDecimal newValue = new BigDecimal( priceFormatterIntern(text) );
		    try {
                        PreparedStatement pstmt = this.conn.prepareStatement(
                                "INSERT INTO kassenstand SET buchungsdatum = "+
                                "NOW(), neuer_kassenstand = ?, " +
                                "manuell = TRUE, kommentar = ?"
				);
                        pstmt.setBigDecimal(1, newValue);
                        pstmt.setString(2, kommentar);
                        int result = pstmt.executeUpdate();
			if (result != 0){
			    // update everything
			    mainWindow.setKassenstand(text);
			    neuerKassenstandField.setText("");
			    differenzField.setText("");
			    kommentarField.setText("");
			    updateAll(this.filterStr);
			}
			else {
			    JOptionPane.showMessageDialog(this,
				    "Fehler: Kassenstand konnte nicht geändert werden.",
				    "Fehler", JOptionPane.ERROR_MESSAGE);
			}
			pstmt.close();
		    } catch (SQLException ex) {
			System.out.println("Exception: " + ex.getMessage());
			ex.printStackTrace();
		    }
		}
		else { return; }
	    }
	    // differenz mode:
	    else if (differenzField.getText().length() > 0){
		String text = differenzField.getText();
		if ( ! text.matches(".*[0-9].*") ){ // if contains no digit: throw error
		    JOptionPane.showMessageDialog(this, "Fehlerhafte Differenz eingegeben!",
			"Fehler", JOptionPane.ERROR_MESSAGE);
		    neuerKassenstandField.setText("");
		    differenzField.setText("");
		    kommentarField.setText("");
		    return;
		}
		text = priceFormatter(text)+" "+currencySymbol;
		BigDecimal differenz = new BigDecimal( priceFormatterIntern(text) );
		String erhoehenReduzieren = new String("");
		if (text.charAt(0) == '-'){
		    erhoehenReduzieren = "reduzieren";
		    text = text.substring(1, text.length()); // strip off the "-"
		}
		else erhoehenReduzieren = "erhöhen";
		int answer = JOptionPane.showConfirmDialog(this,
			"Kassenstand wirklich um "+text+" "+erhoehenReduzieren+" "+
			"mit Kommentar \n\""+kommentar+"\"?", "Kassenstand ändern",
			JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (answer == JOptionPane.YES_OPTION){
		    BigDecimal oldValue = new BigDecimal( priceFormatterIntern(mainWindow.getKassenstand()) );
		    BigDecimal newValue = oldValue.add(differenz);
		    try {
                        PreparedStatement pstmt = this.conn.prepareStatement(
                                "INSERT INTO kassenstand SET buchungsdatum = "+
                                "NOW(), neuer_kassenstand = ?, " +
                                "manuell = TRUE, kommentar = ?"
				);
                        pstmt.setBigDecimal(1, newValue);
                        pstmt.setString(2, kommentar);
                        int result = pstmt.executeUpdate();
			if (result != 0){
			    // update everything
			    mainWindow.setKassenstand(text);
			    neuerKassenstandField.setText("");
			    differenzField.setText("");
			    kommentarField.setText("");
			    updateAll(this.filterStr);
			}
			else {
			    JOptionPane.showMessageDialog(this,
				    "Fehler: Kassenstand konnte nicht geändert werden.",
				    "Fehler", JOptionPane.ERROR_MESSAGE);
			}
			pstmt.close();
		    } catch (SQLException ex) {
			System.out.println("Exception: " + ex.getMessage());
			ex.printStackTrace();
		    }
		}
		else { return; }
	    }
	    // eigentlich unmoeglich:
	    else {
		JOptionPane.showMessageDialog(this, "Kein Betrag eingegeben!",
			"Fehler", JOptionPane.ERROR_MESSAGE);
		neuerKassenstandField.setText("");
		differenzField.setText("");
		kommentarField.setText("");
	    }
	    return;
	}
	else if (e.getSource() == changeDateButton){
	    SpinnerModel startDateModel = startSpinner.getModel();
	    SpinnerModel endDateModel = endSpinner.getModel();
	    Date startDate = null;
	    if (startDateModel instanceof SpinnerDateModel) {
		startDate = ((SpinnerDateModel)startDateModel).getDate();
	    }
	    Date endDate = null;
	    if (endDateModel instanceof SpinnerDateModel) {
		endDate = ((SpinnerDateModel)endDateModel).getDate();
	    }
	    java.sql.Date startDateSQL = new java.sql.Date( startDate.getTime() );
	    java.sql.Date endDateSQL = new java.sql.Date( endDate.getTime() );
	    String startDateStr = startDateSQL.toString();
	    String endDateStr = endDateSQL.toString();
	    this.filterStr = "AND DATE(buchungsdatum) >= DATE('"+startDateStr+"') "+
		"AND DATE(buchungsdatum) <= DATE('"+endDateStr+"') ";
	    updateTable(this.filterStr);
	    return;
	}
	else if (e.getSource() == resetButton){
	    this.filterStr = "";
	    initiateSpinners();
	    updateTable(this.filterStr);
	    return;
	}
	else if (e.getSource() == prevButton){
	    if (this.currentPage > 1)
		this.currentPage--;
	    updateTable(this.filterStr);
	    return;
	}
	else if (e.getSource() == nextButton){
	    if (this.currentPage < totalPage)
		this.currentPage++;
	    updateTable(this.filterStr);
	    return;
	}
    }

    /**
     *    * Each non abstract class that implements the ChangeListener
     *      must have this method.
     *
     *    @param e the change event.
     **/
    public void stateChanged(ChangeEvent e) {
	if (e.getSource() == startSpinner){
	    SpinnerModel dateModel = startSpinner.getModel();
	    if (dateModel instanceof SpinnerDateModel) {
		calButtStart.setTargetDate(((SpinnerDateModel)dateModel).getDate());
	    }
	}
	else if (e.getSource() == endSpinner){
	    SpinnerModel dateModel = endSpinner.getModel();
	    if (dateModel instanceof SpinnerDateModel) {
		calButtEnd.setTargetDate(((SpinnerDateModel)dateModel).getDate());
	    }
	}
	else if (e.getSource() == calButtStart){
	    SpinnerModel dateModel = startSpinner.getModel();
	    Date newDate = calButtStart.getTargetDate();
	    if ( newDate.before(earliestDate) ){
		newDate = earliestDate;
		calButtStart.setTargetDate(newDate);
	    }
	    if ( newDate.after(latestDate) ){
		newDate = latestDate;
		calButtStart.setTargetDate(newDate);
	    }
	    if (dateModel instanceof SpinnerDateModel) {
		if (newDate != null){
		    ((SpinnerDateModel)dateModel).setValue(newDate);
		}
	    }
	}
	else if (e.getSource() == calButtEnd){
	    SpinnerModel dateModel = endSpinner.getModel();
	    Date newDate = calButtEnd.getTargetDate();
	    if ( newDate.before(earliestDate) ){
		newDate = earliestDate;
		calButtEnd.setTargetDate(newDate);
	    }
	    if ( newDate.after(latestDate) ){
		newDate = latestDate;
		calButtEnd.setTargetDate(newDate);
	    }
	    if (dateModel instanceof SpinnerDateModel) {
		if (newDate != null){
		    ((SpinnerDateModel)dateModel).setValue(newDate);
		}
	    }
	}
    }




    void setOtherFieldEditable(DocumentEvent e) {
	JTextField thisFieldPointer = null;
	JTextField otherFieldPointer = null;
	if ( e.getDocument() == neuerKassenstandField.getDocument() ){
	    thisFieldPointer = neuerKassenstandField;
	    otherFieldPointer = differenzField;
	}
	else if ( e.getDocument() == differenzField.getDocument() ){
	    thisFieldPointer = differenzField;
	    otherFieldPointer = neuerKassenstandField;
	}
	else {
	    return;
	}
	if (thisFieldPointer.getText().length() > 0){
	    otherFieldPointer.setEditable(false);
	}
	else {
	    otherFieldPointer.setEditable(true);
	}
    }

    void checkIfFormIsComplete() {
	if ( (neuerKassenstandField.getText().length() > 0 || differenzField.getText().length() > 0) &&
		kommentarField.getText().length() > 0 ){
	    returnButton.setEnabled(true);
	}
	else {
	    returnButton.setEnabled(false);
	}
    }
    /**
     *    * Each non abstract class that implements the DocumentListener
     *      must have these methods.
     *
     *    @param e the document event.
     **/
    public void insertUpdate(DocumentEvent e) {
	// check if form is valid (if Kassenstand can be changed)
	setOtherFieldEditable(e);
	checkIfFormIsComplete();
    }
    public void removeUpdate(DocumentEvent e) {
	// check if form is valid (if Kassenstand can be changed)
	setOtherFieldEditable(e);
	checkIfFormIsComplete();
    }
    public void changedUpdate(DocumentEvent e) {
	//Plain text components do not fire these events
    }

    /** Needed for ItemListener. */
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
        if (source == rechnungsCheckBox) {
            //Now that we know which button was pushed, find out
            //whether it was selected or deselected.
            if (e.getStateChange() == ItemEvent.SELECTED) {
                showRechnungen = true;
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                showRechnungen = false;
            }
        }
        updateAll(this.filterStr);
    }

}

    /**
     *    * Each non abstract class that implements the PropertyChangeListener
     *      must have this method.
     *
     *    @param e the property change event.
     **/
/*
    public void propertyChange(PropertyChangeEvent e){
        if (e.getSource() == neuerKassenstandField){
            // replace illegal characters with nothing
            String legalText = neuerKassenstandField.getText().replaceAll("[^0-9]","");
            neuerKassenstandField.setText(
                    neuerKassenstandField.getText().replaceAll(".",","));
        }
	else if (e.getSource() == differenzField){
            // replace illegal characters with nothing
            //differenzField.setText(
            //        differenzField.getText().replaceAll("[^0-9]",""));
        }
	else if (e.getSource() == kommentarField){
            // check if entered string is not wihtespace-only
        }
    }
*/
