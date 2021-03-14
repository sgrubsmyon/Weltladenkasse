package org.weltladen_bonn.pos.kasse;

import org.weltladen_bonn.pos.BaseClass;
import org.weltladen_bonn.pos.DialogWindow;
import org.weltladen_bonn.pos.MainWindowGrundlage;
import org.weltladen_bonn.pos.BaseClass.BigLabel;
import org.weltladen_bonn.pos.IntegerDocumentFilter;
import org.weltladen_bonn.pos.AnyJComponentJTable;
import org.weltladen_bonn.pos.ArticleSelectTable;

// Basic Java stuff:
import java.util.Vector;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.mariadb.jdbc.MariaDbPoolDataSource;

// GUI stuff:
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AnzahlungAufloesDialog extends DialogWindow implements DocumentListener {
    private static final Logger logger = LogManager.getLogger(AnzahlungAufloesDialog.class);

    private int anzahlungArtikelID = 4;

    private int pageNumber = 1;
    private int selRechNr;
    protected Vector<Integer> rechnungsNummern;
    private Vector<Vector<Object>> anzahlungData;
    private Vector<String> anzahlungLabels;
    private Vector< Vector<Object> > anzahlungDetailData;
    private Vector<Integer> anzahlungDetailArtikelIDs;
    private Vector<String> anzahlungDetailColors;
    private Vector< Vector<Object> > anzahlungDetailDisplayData;

    private JSplitPane splitPane;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private JButton loadMoreButton;
    private JPanel anzahlungTablePanel;
    private JPanel anzahlungDetailTablePanel;
    private JScrollPane anzahlungDetailScrollPane;
    protected AnyJComponentJTable anzahlungTable;
    private ArticleSelectTable anzahlungDetailTable;

    private JButton okButton;
    private JButton cancelButton;
    private boolean aborted = true;

    // Methoden:
    public AnzahlungAufloesDialog(MariaDbPoolDataSource pool, MainWindowGrundlage mw, JDialog dia) {
        super(pool, mw, dia);
        selRechNr = -1;
        showAll();
    }

    // will data be lost on close?
    protected boolean willDataBeLost() {
        return false;
    }

    protected void showHeader() {
        /**
         * Information-Panel
         * */
        headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        
        // borders:
        int top = 5, left = 5, bottom = 5, right = 5;
        headerPanel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));

        JTextArea erklaerText = new JTextArea(1, 30);
        erklaerText.append("Welche offene Anzahlung soll aufgelöst werden?");
        erklaerText = makeLabelStyle(erklaerText);
        erklaerText.setFont(BaseClass.mediumFont);
        erklaerText.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        headerPanel.add(erklaerText);

        allPanel.add(headerPanel, BorderLayout.NORTH);
    }

    protected void showMiddle() {
        /**
         * Main-Panel
         * */
        leftPanel = new JPanel();
        rightPanel = new JPanel();
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            leftPanel, rightPanel);
        splitPane.setOneTouchExpandable(true);
        //splitPane.setResizeWeight(0.3);
        splitPane.setDividerLocation(0.3);
        allPanel.add(splitPane, BorderLayout.CENTER);
        showTables();
    }

    void showTables() {
        anzahlungData = new Vector< Vector<Object> >();
        rechnungsNummern = new Vector<Integer>();
        retrieveAnzahlungData();
        showLeftPanel();
        showRightPanel(selRechNr);
    }

    void showLeftPanel() {
        leftPanel.setLayout(new BorderLayout());

        // Panel for the "Load more" button and anzahlung table
        anzahlungTablePanel = new JPanel(new BorderLayout());

        JPanel loadMorePanel = new JPanel();
        loadMorePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        loadMoreButton = new JButton("Mehr laden");
        loadMoreButton.addActionListener(this);
        loadMorePanel.add(loadMoreButton);
        anzahlungTablePanel.add(loadMorePanel, BorderLayout.NORTH);

        anzahlungLabels = new Vector<String>();
        anzahlungLabels.add("Rechnungs-Nr.");
        anzahlungLabels.add("Datum");
        anzahlungLabels.add("Anzahlung");
        anzahlungLabels.add("Rechnungssumme");
        anzahlungTable = new AnyJComponentJTable(anzahlungData, anzahlungLabels){
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                // add custom rendering here
                c.setForeground(Color.black); // keep it black
                return c;
            }
        };
        // selection listener:
        //orderTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
        anzahlungTable.setFillsViewportHeight(true);
        anzahlungTable.getSelectionModel().addListSelectionListener(new RowListener());

        // set table properties:
        TableColumn nr = anzahlungTable.getColumn("Rechnungs-Nr.");
        nr.setCellRenderer(rechtsAusrichter);
        nr.setPreferredWidth(100);
        TableColumn typ = anzahlungTable.getColumn("Datum");
        typ.setCellRenderer(zentralAusrichter);
        typ.setPreferredWidth(200);
        TableColumn jahr = anzahlungTable.getColumn("Anzahlung");
        jahr.setCellRenderer(rechtsAusrichter);
        jahr.setPreferredWidth(100);
        TableColumn kw = anzahlungTable.getColumn("Rechnungssumme");
        kw.setCellRenderer(rechtsAusrichter);
        kw.setPreferredWidth(100);

        JScrollPane scrollPane = new JScrollPane(anzahlungTable);
        anzahlungTablePanel.add(scrollPane, BorderLayout.CENTER);
        
        leftPanel.add(anzahlungTablePanel, BorderLayout.CENTER);
    }

    public void showRightPanel(int rechnungsNr) {
        if (rechnungsNr > 0) {
            rightPanel.setLayout(new BorderLayout());
        }
    }

    private class RowListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                logger.debug("The mouse button has not yet been released");
                return;
            }
            int[] selRows = anzahlungTable.getSelectedRows();
            if ( selRows.length == 1 ){
                int realRowIndex = anzahlungTable.convertRowIndexToModel(selRows[0]); // user might have changed row order
                selRechNr = rechnungsNummern.get(realRowIndex);
            } else {
                selRechNr = -1;
            }
            updateRightPanel();
        }
    }

    private void updateLeftPanel() {
        leftPanel = new JPanel();
        splitPane.setLeftComponent(leftPanel);
        showLeftPanel();
    }

    private void updateRightPanel() {
        rightPanel = new JPanel();
        splitPane.setRightComponent(rightPanel);
        showRightPanel(selRechNr);
    }

    void retrieveAnzahlungData() {
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
		        "SELECT anzahlung_in_rech_nr, DATE_FORMAT(datum, "+
                "'"+bc.dateFormatSQL+"') "+
                "FROM "+tableForMode("anzahlung")+" "+
                "WHERE "+
                "datum > NOW() - INTERVAL 6 * ? MONTH AND "+
                "datum <= NOW() - INTERVAL 6 * ? MONTH "+
                "GROUP BY anzahlung_in_rech_nr "+
                "HAVING COUNT(*) = 1 "+
                "ORDER BY anzahlung_in_rech_nr DESC"
		    );
            pstmtSetInteger(pstmt, 1, pageNumber);
            pstmtSetInteger(pstmt, 2, pageNumber - 1);
            ResultSet rs = pstmt.executeQuery();
            Vector<Vector<Object>> incompleteData = new Vector<Vector<Object>>();
            while (rs.next()) {
                rechnungsNummern.add(rs.getInt(1));
                Vector<Object> row = new Vector<Object>();
                row.add(rs.getString(1));
                row.add(rs.getString(2));
                incompleteData.add(row);
            }
            rs.close();
            pstmt.close();
            // if (incompleteData.size() == 0) {
            //     loadMoreButton.setEnabled(false);
            // }
            for (Vector<Object> row : incompleteData) {
                int rechNr = Integer.parseInt((String)row.get(0));
                pstmt = connection.prepareStatement(
                    "SELECT SUM(ges_preis) FROM "+tableForMode("verkauf_details")+" "+
                    "WHERE rechnungs_nr = ? AND artikel_id = ?"
                );
                pstmtSetInteger(pstmt, 1, rechNr);
                pstmtSetInteger(pstmt, 2, anzahlungArtikelID);
                rs = pstmt.executeQuery();
                rs.next(); row.add(bc.priceFormatter(rs.getString(1))+" "+bc.currencySymbol); rs.close();
                pstmt.close();
                pstmt = connection.prepareStatement(
                    "SELECT SUM(stueckzahl*vk_preis) FROM "+tableForMode("verkauf_details")+" "+
                    "INNER JOIN "+tableForMode("artikel")+" USING (artikel_id) "+
                    "WHERE rechnungs_nr = ? AND position < ("+
                    "  SELECT MIN(position) FROM "+tableForMode("verkauf_details")+" "+
                    "  WHERE rechnungs_nr = ? AND artikel_id = ?"+
                    ")"
                );
                pstmtSetInteger(pstmt, 1, rechNr);
                pstmtSetInteger(pstmt, 2, rechNr);
                pstmtSetInteger(pstmt, 3, anzahlungArtikelID);
                rs = pstmt.executeQuery();
                rs.next(); row.add(bc.priceFormatter(rs.getString(1))+" "+bc.currencySymbol); rs.close();
                pstmt.close();
                anzahlungData.add(row);
            }
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            showDBErrorDialog(ex.getMessage());
        }
    }

    void retrieveAnzahlungDetailData(int rechnungsNr) {
        anzahlungDetailData = new Vector< Vector<Object> >();
        anzahlungDetailArtikelIDs = new Vector<Integer>();
        anzahlungDetailColors = new Vector<String>();
        // try {
        //     Connection connection = this.pool.getConnection();
        //     PreparedStatement pstmt = connection.prepareStatement(
        //             "SELECT bd.position, l.lieferant_name, a.artikel_nr, a.artikel_name, "+
        //             "a.empf_vk_preis, a.vk_preis, a.vpe, bd.stueckzahl, a.beliebtheit, a.sortiment, bd.artikel_id "+
        //             "FROM bestellung_details AS bd "+
        //             "LEFT JOIN artikel AS a USING (artikel_id) "+
        //             "LEFT JOIN lieferant AS l USING (lieferant_id) "+
        //             "WHERE bd.bestell_nr = ? "+
        //             "ORDER BY bd.position DESC"
        //             );
        //     pstmtSetInteger(pstmt, 1, rechnungsNr);
        //     ResultSet rs = pstmt.executeQuery();
        //     // Now do something with the ResultSet, should be only one result ...
        //     while ( rs.next() ){
        //         String pos = rs.getString(1);
        //         String lieferant = rs.getString(2);
        //         String artikelNummer = rs.getString(3);
        //         String artikelName = rs.getString(4);
        //         String empf_vkpreis = rs.getString(5);
        //         String vkpreis = rs.getString(6);
        //         String vpe = rs.getString(7);
        //         //Integer vpeInt = rs.getInt(7);
        //         //vpeInt = vpeInt > 0 ? vpeInt : 0;
        //         Integer stueck = rs.getInt(8);
        //         Integer beliebt = rs.getInt(9);
        //         Boolean sortimentBool = rs.getBoolean(10);
        //         String color = sortimentBool ? "default" : "gray";
        //         Integer artikelID = rs.getInt(11);

        //         String vkp;
        //         if (empf_vkpreis == null || empf_vkpreis.equals("")){
        //             vkp = vkpreis;
        //         } else {
        //             vkp = empf_vkpreis;
        //         }

        //         Vector<Object> row = new Vector<Object>();
        //             row.add(pos);
        //             row.add(lieferant); row.add(artikelNummer); row.add(artikelName);
        //             row.add(bc.priceFormatter(vkp)+" "+bc.currencySymbol); row.add(vpe);
        //             row.add(stueck); row.add(beliebt);
        //             row.add(""); // row.add(removeButtons.lastElement())
        //         anzahlungDetailData.add(row);
        //         anzahlungDetailArtikelIDs.add(artikelID);
        //         anzahlungDetailColors.add(color);
        //     }
        //     rs.close();
        //     pstmt.close();
        //     connection.close();
        // } catch (SQLException ex) {
        //     logger.error("Exception:", ex);
        //     showDBErrorDialog(ex.getMessage());
        // }
        anzahlungDetailDisplayData = new Vector< Vector<Object> >(anzahlungDetailData);
    }

    protected void showFooter() {
        /**
         * Button-Panel
         * */
        footerPanel = new JPanel();
        okButton = new JButton("OK" );
        okButton.setMnemonic(KeyEvent.VK_O);
        okButton.addActionListener(this);
        okButton.setEnabled(false);
        footerPanel.add(okButton);
        cancelButton = new JButton("Abbrechen" );
        cancelButton.setMnemonic(KeyEvent.VK_A);
        cancelButton.addActionListener(this);
        footerPanel.add(cancelButton);
        allPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    @Override
    protected int submit() {
        return 0;
    }

    public boolean getAborted() {
        return aborted;
    }

    public int getSelectedRechNr() {
        return selRechNr;
    }

    /**
     * Each non abstract class that implements the ActionListener
     * must have this method.
     *
     * @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
        updateOKButton();
        if (e.getSource() == loadMoreButton) {
            pageNumber += 1;
            retrieveAnzahlungData();
            updateLeftPanel();
        }
        if (e.getSource() == okButton) {
            aborted = false;
            this.window.dispose();
            return;
        }
        if (e.getSource() == cancelButton) {
            aborted = true;
            this.window.dispose();
            return;
        }
        super.actionPerformed(e);
    }

    private void updateOKButton() {
        // ...
    }

    /**
     * Each non abstract class that implements the DocumentListener must have
     * these methods.
     * @param documentEvent
     *            the document event.
     **/
    @Override
    public void insertUpdate(DocumentEvent documentEvent) {
        updateOKButton();
    }

    @Override
    public void removeUpdate(DocumentEvent documentEvent) {
        insertUpdate(documentEvent);
    }

    @Override
    public void changedUpdate(DocumentEvent documentEvent) {
        // Plain text components do not fire these events
    }
}