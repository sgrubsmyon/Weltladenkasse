package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
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
//import javax.swing.JTextArea;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;

// JCalendarButton
import org.weltladen_bonn.pos.jcalendarbutton.JCalendarButton;
//import java.util.Calendar;
//import java.util.Date;

import org.weltladen_bonn.pos.WindowContent;
import org.weltladen_bonn.pos.MainWindowGrundlage;
import org.weltladen_bonn.pos.ArtikelGrundlage;
import org.weltladen_bonn.pos.AnyJComponentJTable;

public class Rabattaktionen extends ArtikelGrundlage implements ChangeListener, TableModelListener {
    // Attribute:
    private int currentPage = 1;
    private int totalPage;
    private String filterStr = "WHERE r.bis != r.von OR r.bis IS NULL ";
    private OptionTabbedPane tabbedPane;

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
    private JButton heuteButton;
    private JButton resetButton;
    private JButton prevButton;
    private JButton nextButton;

    private JButton newButton;
    // Dialog to enter new items
    private JDialog newRabattDialog;
    private JDialog editRabattDialog;
    private RabattDialog newRabatt;
    private RabattDialog editRabatt;

    // The bottom panel which holds button.
    private JPanel allPanel;
    private JPanel historyPanel;
    // The table holding the invoices. This is "anonymously subclassed" and two method are overridden
    private AnyJComponentJTable myTable;

    private Vector<Vector<Object>> data;
    private Vector<String> columnLabels;
    private Vector<JButton> editButtons;
    private Vector<JButton> deleteButtons;
    private Vector<Integer> rabattIDs;

    private String kassenstandZahl;
    private int kassenstandZahlInt;

    // Methoden:

    /**
     *    The constructor.
     *       */
    public Rabattaktionen(MariaDbPoolDataSource pool, MainWindowGrundlage mw, OptionTabbedPane tabbedPane) {
	    super(pool, mw);
        this.tabbedPane = tabbedPane;

	    initiateSpinners();
	    showAll();
    }

    private void queryEarliestAndLatestRabattaktion() {
	int earlyDay = 0; int earlyMonth = 0; int earlyYear = 0;
	int lateDay = 0; int lateMonth = 0; int lateYear = 0;
	try {
        Connection connection = this.pool.getConnection();
	    // Create statement for MySQL database
	    Statement stmt = connection.createStatement();
	    // Run MySQL command
	    ResultSet rs = stmt.executeQuery(
		    "SELECT DAY(MIN(von)), MONTH(MIN(von)), YEAR(MIN(von)), " +
                    "DAY(MAX(bis)), MONTH(MAX(bis)), YEAR(MAX(bis)) FROM rabattaktion"
		    );
	    // Now do something with the ResultSet ...
	    rs.next();
	    earlyDay = rs.getInt(1);
	    earlyMonth = rs.getInt(2);
	    earlyYear = rs.getInt(3);
	    lateDay = rs.getInt(4);
	    lateMonth = rs.getInt(5);
	    lateYear = rs.getInt(6);
	    rs.close();
	    stmt.close();
        connection.close();
	} catch (SQLException ex) {
	    System.out.println("Exception: " + ex.getMessage());
	    ex.printStackTrace();
	}
	Calendar earlyCalendar = Calendar.getInstance();
	earlyCalendar.set(Calendar.YEAR, earlyYear);
	earlyCalendar.set(Calendar.MONTH, earlyMonth-1);
	earlyCalendar.set(Calendar.DAY_OF_MONTH, earlyDay-1); // for strange reasons, we need day-1
	oneDayBeforeEarliestDate = earlyCalendar.getTime();
	earlyCalendar.set(Calendar.DAY_OF_MONTH, earlyDay);
	earliestDate = earlyCalendar.getTime();

        Date now = new Date(); // current date
        if ( earlyYear == 0 ){
            oneDayBeforeEarliestDate = now;
            earliestDate = now;
        }
	Calendar lateCalendar = Calendar.getInstance();
	lateCalendar.set(Calendar.YEAR, lateYear);
	lateCalendar.set(Calendar.MONTH, lateMonth-1);
	lateCalendar.set(Calendar.DAY_OF_MONTH, lateDay);
	latestDate = lateCalendar.getTime();
        if ( lateYear == 0 ){
            latestDate = now;
        }
        // final check:
        if (latestDate.before(earliestDate)) {
            Date tmp = earliestDate;
            earliestDate = latestDate;
            latestDate = tmp;
            oneDayBeforeEarliestDate = earliestDate;
        }
    }

    public void initiateSpinners(){
	queryEarliestAndLatestRabattaktion();
        System.out.println("Rabattaktionen spinner values: "+oneDayBeforeEarliestDate+" "+earliestDate+" "+latestDate);
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

    void fillDataArray(){
	data = new Vector<Vector<Object>>();
	columnLabels = new Vector<String>();
        editButtons = new Vector<JButton>();
        deleteButtons = new Vector<JButton>();
	rabattIDs = new Vector<Integer>();
	columnLabels.add("Aktionsname"); columnLabels.add("von"); columnLabels.add("bis");
        columnLabels.add("Artikel"); columnLabels.add("Produktgruppe");
        columnLabels.add("Rabatt relativ"); columnLabels.add("Rabatt absolut");
	columnLabels.add("Mengenrabatt-Schwelle"); columnLabels.add("Mengenrabatt Anzahl kostenlos"); columnLabels.add("Mengenrabatt relativ");
	columnLabels.add("Bearbeiten"); columnLabels.add("Löschen");
	try {
        Connection connection = this.pool.getConnection();
	    // Create statement for MySQL database
	    Statement stmt = connection.createStatement();
	    // Run MySQL command
	    ResultSet rs = stmt.executeQuery(
		    "SELECT r.rabatt_id, r.aktionsname, r.rabatt_relativ, r.rabatt_absolut, "+
                    "r.mengenrabatt_schwelle, r.mengenrabatt_anzahl_kostenlos, r.mengenrabatt_relativ, "+
                    "r.von > NOW(), r.bis > NOW() OR r.bis IS NULL, "+
                    "DATE_FORMAT(r.von,'"+bc.dateFormatSQL+"'), DATE_FORMAT(r.bis,'"+bc.dateFormatSQL+"'), "+
                    "artikel.artikel_name, produktgruppe.produktgruppen_name "+
                    "FROM rabattaktion AS r LEFT JOIN produktgruppe USING (produktgruppen_id) "+
                    "LEFT JOIN artikel USING (artikel_id) "+
		    this.filterStr +
		    "ORDER BY r.von DESC " +
		    "LIMIT " + (currentPage-1)*bc.rowsPerPage + "," + bc.rowsPerPage
		    );
	    // Now do something with the ResultSet ...
	    while (rs.next()) {
            Integer rabattID = rs.getInt(1);
            String aktionsname = rs.getString(2);
            String rabattRel = rs.getString(3);
            String rabattAbs = rs.getString(4);
            String schwelle = rs.getString(5);
            String mengenAnz = rs.getString(6);
            String mengenRel = rs.getString(7);
            Boolean vonAfterNow = rs.getBoolean(8);
            Boolean bisAfterNow = rs.getBoolean(9);
            String von = rs.getString(10); // configured format
            String bis = rs.getString(11); // configured format
            String artikel = rs.getString(12);
            String produktgr = rs.getString(13);

            aktionsname = aktionsname == null ? "" : aktionsname;
            rabattRel = rabattRel == null ? "" : bc.vatFormatter(rabattRel);
            rabattAbs = rabattAbs == null ? "" : rabattAbs.replace('.',',')+" "+bc.currencySymbol;
            schwelle = schwelle == null ? "" : schwelle;
            mengenAnz = mengenAnz == null ? "" : mengenAnz;
            mengenRel = mengenRel == null ? "" : bc.vatFormatter(mengenRel);
            von = von == null ? "" : von;
            bis = bis == null ? "" : bis;
            artikel = artikel == null ? "" : artikel;
            produktgr = produktgr == null ? "" : produktgr;

            editButtons.add(new JButton("B"));
            editButtons.lastElement().addActionListener(this);
            editButtons.lastElement().setEnabled(false);
            deleteButtons.add(new JButton("L"));
            deleteButtons.lastElement().addActionListener(this);
            deleteButtons.lastElement().setEnabled(false);
            
            Vector<Object> row = new Vector<Object>();
            row.add(aktionsname); row.add(von); row.add(bis);
            row.add(artikel); row.add(produktgr);
                    row.add(rabattRel); row.add(rabattAbs);
            row.add(schwelle); row.add(mengenAnz); row.add(mengenRel);
            row.add(editButtons.lastElement()); row.add(deleteButtons.lastElement());
            data.add(row);

            if ( vonAfterNow != null ){ // if von date is null, there's something fishy -> don't enable anything
                if ( vonAfterNow ){ // Rabattaktion lies in the future
                    editButtons.lastElement().setEnabled(true); // everything can be changed
                    deleteButtons.lastElement().setEnabled(true); // can still be deleted
                } else if ( bisAfterNow ){ // Rabattaktion has started, but not ended yet
                    editButtons.lastElement().setEnabled(true); // only name and bis date can be changed
                    deleteButtons.lastElement().setEnabled(true); // this actually doesn't delete it, but sets bis to now
                }
            }
            rabattIDs.add(rabattID);
	    }
	    rs.close();
	    rs = stmt.executeQuery(
		    "SELECT COUNT(*) FROM rabattaktion AS r " +
		    this.filterStr
		    );
	    // Now do something with the ResultSet ...
	    rs.next();
	    kassenstandZahl = rs.getString(1);
	    kassenstandZahlInt = Integer.parseInt(kassenstandZahl);
	    totalPage = kassenstandZahlInt/bc.rowsPerPage + 1;
	    rs.close();
	    stmt.close();
        connection.close();
	} catch (SQLException ex) {
	    System.out.println("Exception: " + ex.getMessage());
	    ex.printStackTrace();
	}
        myTable = new AnyJComponentJTable(data, columnLabels) { // subclass the AnyJComponentJTable to set font properties and tool tip text
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                // add custom rendering here
                //int realRowIndex = convertRowIndexToModel(row);
                //c.setForeground(Color.BLACK);
                return c;
            }
        };
//	myTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
//	myTable.setFillsViewportHeight(true);
    }

    void showAll(){
	allPanel = new JPanel();
	allPanel.setLayout(new BoxLayout(allPanel, BoxLayout.Y_AXIS));

	fillDataArray();
	showTable();

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            newButton = new JButton("Neue Rabattaktion eingeben");
            newButton.setMnemonic(KeyEvent.VK_N);
            newButton.addActionListener(this);
        buttonPanel.add(newButton);
        allPanel.add(buttonPanel);

	this.add(allPanel, BorderLayout.CENTER);
    }

    void showTable(){
        myTable.getModel().addTableModelListener(this);
        myTable.setAutoCreateRowSorter(true);
//	myTable.setBounds(71,53,150,100);
//	myTable.setToolTipText("Tabelle kann nur gelesen werden.");
	setTableProperties(myTable);
//	myTable.setAutoResizeMode(5);

	historyPanel = new JPanel();
	historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
	historyPanel.setBorder(BorderFactory.createTitledBorder("Liste der Rabattaktionen"));

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
	    heuteButton = new JButton("Heute");
	    heuteButton.addActionListener(this);
	    datePanel.add(heuteButton);
	    resetButton = new JButton("Alle");
	    resetButton.addActionListener(this);
	    datePanel.add(resetButton);
	    historyPanel.add(datePanel);

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
	    int currentPageMin = (currentPage-1)*bc.rowsPerPage + 1;
	    int currentPageMax = bc.rowsPerPage*currentPage;
	    currentPageMax = (currentPageMax <= kassenstandZahlInt) ? currentPageMax : kassenstandZahlInt;
	    JLabel header = new JLabel("Seite "+ currentPage +" von "+ totalPage + ", Rabattaktionen "+
		currentPageMin + " bis "+ currentPageMax +" von "+ kassenstandZahlInt);
	    pageChangePanel.add(header);
	    historyPanel.add(pageChangePanel);

	    JScrollPane scrollPane = new JScrollPane(myTable);
	    historyPanel.add(scrollPane);

	allPanel.add(historyPanel);
    }

    public void updateAll(){
	this.remove(allPanel);
	this.revalidate();
	showAll();
    }

    private void setTableProperties(JTable table){
	// Spalteneigenschaften:
	table.getColumn("Aktionsname").setCellRenderer(linksAusrichter);
	table.getColumn("von").setCellRenderer(linksAusrichter);
	table.getColumn("bis").setCellRenderer(linksAusrichter);
	table.getColumn("Artikel").setCellRenderer(linksAusrichter);
	table.getColumn("Produktgruppe").setCellRenderer(linksAusrichter);
	table.getColumn("Rabatt relativ").setCellRenderer(rechtsAusrichter);
	table.getColumn("Rabatt absolut").setCellRenderer(rechtsAusrichter);
	table.getColumn("Mengenrabatt-Schwelle").setCellRenderer(rechtsAusrichter);
	table.getColumn("Mengenrabatt Anzahl kostenlos").setCellRenderer(rechtsAusrichter);
	table.getColumn("Mengenrabatt relativ").setCellRenderer(rechtsAusrichter);

	table.getColumn("Aktionsname").setPreferredWidth(100);
	table.getColumn("von").setPreferredWidth(30);
	table.getColumn("bis").setPreferredWidth(30);
	table.getColumn("Artikel").setPreferredWidth(30);
	table.getColumn("Produktgruppe").setPreferredWidth(30);
	table.getColumn("Rabatt relativ").setPreferredWidth(5);
	table.getColumn("Rabatt absolut").setPreferredWidth(5);
	table.getColumn("Mengenrabatt-Schwelle").setPreferredWidth(5);
	table.getColumn("Mengenrabatt Anzahl kostenlos").setPreferredWidth(5);
	table.getColumn("Mengenrabatt relativ").setPreferredWidth(5);
	table.getColumn("Bearbeiten").setPreferredWidth(1);
	table.getColumn("Löschen").setPreferredWidth(1);
    }

    private void changeViewDates(Date startDate, Date endDate) {
        java.sql.Date startDateSQL = new java.sql.Date( startDate.getTime() );
        java.sql.Date endDateSQL = new java.sql.Date( endDate.getTime() );
        String startDateStr = startDateSQL.toString();
        String endDateStr = endDateSQL.toString();
        this.filterStr = "WHERE r.bis != r.von AND "+
            "( ( DATE(r.von) >= DATE('"+startDateStr+"') AND DATE(r.von) <= DATE('"+endDateStr+"') ) OR "+
              "( DATE(r.bis) >= DATE('"+startDateStr+"') AND DATE(r.bis) <= DATE('"+endDateStr+"') ) OR "+
              "( DATE(r.von) <= DATE('"+endDateStr+"') AND ISNULL(r.bis) ) "+
            ") ";
        updateAll();
    }

    private void setBisDate(Integer rabattID, String aktionsname, String bis) {
        int answer = JOptionPane.showConfirmDialog(this,
                "Rabattaktion \'"+aktionsname+"\' beenden/löschen?",
                "Rabattaktion löschen",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.YES_OPTION){
            try {
                Connection connection = this.pool.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(
                        "UPDATE rabattaktion SET bis = "+bis+" WHERE rabatt_id = ?"
                        );
                pstmtSetInteger(pstmt, 1, rabattID);
                int result = pstmt.executeUpdate();
                if (result != 0){
                    // update everything
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Fehler: Rabattaktion konnte nicht gelöscht werden.",
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
                pstmt.close();
                connection.close();
            } catch (SQLException ex) {
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else { // NO_OPTION
            JOptionPane.showMessageDialog(this, "Datenbank unverändert!",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    private void setBisDateToVonDate(Integer rabattID, String aktionsname) {
        setBisDate(rabattID, aktionsname, "von");
    }
    private void setBisDateToNow(Integer rabattID, String aktionsname) {
        setBisDate(rabattID, aktionsname, "NOW()");
    }

    private Boolean isVonDateAfterNow(Integer rabattID) {
        Boolean vonAfterNow = null;
	try {
        Connection connection = this.pool.getConnection();
        PreparedStatement pstmt = connection.prepareStatement(
            "SELECT von > NOW() FROM rabattaktion WHERE rabatt_id = ?"
        );
        pstmtSetInteger(pstmt, 1, rabattID);
	    ResultSet rs = pstmt.executeQuery();
	    rs.next(); vonAfterNow = rs.getBoolean(1); rs.close();
	    pstmt.close();
        connection.close();
	} catch (SQLException ex) {
	    System.out.println("Exception: " + ex.getMessage());
	    ex.printStackTrace();
	}
        return vonAfterNow;
    }
    private Boolean isBisDateAfterNow(Integer rabattID) {
        Boolean bisAfterNow = null;
	try {
        Connection connection = this.pool.getConnection();
        PreparedStatement pstmt = connection.prepareStatement(
            "SELECT bis > NOW() OR bis IS NULL FROM rabattaktion WHERE rabatt_id = ?"
        );
        pstmtSetInteger(pstmt, 1, rabattID);
	    ResultSet rs = pstmt.executeQuery();
	    rs.next(); bisAfterNow = rs.getBoolean(1); rs.close();
	    pstmt.close();
        connection.close();
	} catch (SQLException ex) {
	    System.out.println("Exception: " + ex.getMessage());
	    ex.printStackTrace();
	}
        return bisAfterNow;
    }

    private class WindowAdapterNewRabatt extends WindowAdapter {
        private RabattDialog newRabatt;
        private JDialog dialog;
        public WindowAdapterNewRabatt(RabattDialog rd, JDialog dia) {
            super();
            this.newRabatt = rd;
            this.dialog = dia;
        }
        @Override
        public void windowClosing(WindowEvent we) {
            if ( newRabatt.isFormComplete() ){
                int answer = JOptionPane.showConfirmDialog(dialog,
                        "Achtung: Rabattaktion ist noch nicht gespeichert.\nWirklich schließen?", "Neue Rabattaktion wird gelöscht",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (answer == JOptionPane.YES_OPTION){
                    dialog.dispose();
                } else {
                    // do nothing
                }
            } else {
                dialog.dispose();
            }
        }
    }

    private class WindowAdapterEditRabatt extends WindowAdapter {
        private RabattDialog editRabatt;
        private JDialog dialog;
        public WindowAdapterEditRabatt(RabattDialog rd, JDialog dia) {
            super();
            this.editRabatt = rd;
            this.dialog = dia;
        }
        @Override
        public void windowClosing(WindowEvent we) {
            if ( editRabatt.areThereChanges() ){
                int answer = JOptionPane.showConfirmDialog(dialog,
                        "Achtung: Änderungen an der Rabattaktion sind noch nicht gespeichert.\nWirklich schließen?", "Änderungen gehen verloren",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (answer == JOptionPane.YES_OPTION){
                    dialog.dispose();
                } else {
                    // do nothing
                }
            } else {
                dialog.dispose();
            }
        }
    }

    void showNewRabattDialog() {
        newRabattDialog = new JDialog(this.mainWindow, "Neue Rabattaktion hinzufügen", true);
        newRabatt = new RabattDialog(this.pool, this.mainWindow, this, newRabattDialog, tabbedPane);
        newRabattDialog.getContentPane().add(newRabatt, BorderLayout.CENTER);
        newRabattDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        WindowAdapterNewRabatt wanr = new WindowAdapterNewRabatt(newRabatt, newRabattDialog);
        newRabattDialog.addWindowListener(wanr);
        newRabattDialog.pack();
        newRabattDialog.setVisible(true);
    }

    void showEditRabattDialog(Integer rabattID, boolean onlyNameAndBis) {
        editRabattDialog = new JDialog(this.mainWindow, "Rabattaktion bearbeiten", true);
        editRabatt = new RabattDialog(this.pool, this.mainWindow, this, editRabattDialog, tabbedPane,
                "Rabattaktion bearbeiten", true, rabattID, onlyNameAndBis);
        editRabattDialog.getContentPane().add(editRabatt, BorderLayout.CENTER);
        editRabattDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        WindowAdapterEditRabatt waer = new WindowAdapterEditRabatt(editRabatt, editRabattDialog);
        editRabattDialog.addWindowListener(waer);
        editRabattDialog.pack();
        editRabattDialog.setVisible(true);
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == newButton){
            showNewRabattDialog();
            return;
        }
	else if (e.getSource() == changeDateButton){
            changeViewDates(startDateModel.getDate(), endDateModel.getDate());
            return;
	}
	else if (e.getSource() == heuteButton){
            Date now = new Date(); // refresh the now-time
            startDateModel.setValue(now);
            endDateModel.setValue(now);
            changeViewDates(now, now);
	    return;
	}
	else if (e.getSource() == resetButton){
	    this.filterStr = "WHERE r.bis != r.von ";
	    initiateSpinners();
	    updateAll();
	    return;
	}
	else if (e.getSource() == prevButton){
	    if (this.currentPage > 1)
		this.currentPage--;
	    updateAll();
	    return;
	}
	else if (e.getSource() == nextButton){
	    if (this.currentPage < totalPage)
		this.currentPage++;
	    updateAll();
	    return;
	}
	int deleteRow = -1;
	for (int i=0; i<deleteButtons.size(); i++){
	    if (e.getSource() == deleteButtons.get(i) ){
		deleteRow = i;
		break;
	    }
	}
        if (deleteRow > -1){
            Integer rabattID = rabattIDs.get(deleteRow);
            Boolean vonAfterNow = isVonDateAfterNow(rabattID);
            Boolean bisAfterNow = isBisDateAfterNow(rabattID);
            if ( vonAfterNow != null ){
                String aktionsname = (String)data.get(deleteRow).get(0);
                if ( vonAfterNow ){ // if Rabattaktion still hasn't started yet
                    System.out.println("Setting bis date to von date to delete Rabattaktion.");
                    setBisDateToVonDate(rabattID, aktionsname);
                } else if ( bisAfterNow ){ // if Rabattaktion has started, but hasn't ended yet
                    System.out.println("Setting bis date to now to end Rabattaktion.");
                    setBisDateToNow(rabattID, aktionsname);
                }
            }
            initiateSpinners();
            updateAll();
            return;
        }
	int editRow = -1;
	for (int i=0; i<editButtons.size(); i++){
	    if (e.getSource() == editButtons.get(i) ){
		editRow = i;
		break;
	    }
	}
        if (editRow > -1){
            Integer rabattID = rabattIDs.get(editRow);
            Boolean vonAfterNow = isVonDateAfterNow(rabattID);
            Boolean bisAfterNow = isBisDateAfterNow(rabattID);
            if ( vonAfterNow != null ){ // if von date is null, there's something fishy -> don't allow editing
                if ( vonAfterNow ){ // Rabattaktion lies in the future
                    showEditRabattDialog(rabattID, false); // everything can be changed
                } else if ( bisAfterNow ){ // Rabattaktion has started, but not ended yet
                    showEditRabattDialog(rabattID, true); // only name and bis date can be changed
                }
            }
            initiateSpinners();
            updateAll();
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
            setCalButtFromSpinner(startDateModel, calButtStart);
	}
	else if (e.getSource() == endSpinner){
            setCalButtFromSpinner(endDateModel, calButtEnd);
	}
	else if (e.getSource() == calButtStart){
            setSpinnerFromCalButt(startDateModel, calButtStart, earliestDate, latestDate);
	}
	else if (e.getSource() == calButtEnd){
            setSpinnerFromCalButt(endDateModel, calButtEnd, earliestDate, latestDate);
	}
    }




    /** Needed for TableModelListener. */
    public void tableChanged(TableModelEvent e) {
    }
}
