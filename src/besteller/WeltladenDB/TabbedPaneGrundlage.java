package WeltladenDB;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

import javax.swing.JTabbedPane;

public abstract class TabbedPaneGrundlage extends WindowContent {
    protected JTabbedPane tabbedPane;
    protected TabbedPaneGrundlage parentTabbedPane = null;

    // Methoden:
    public TabbedPaneGrundlage(Connection conn, MainWindowGrundlage mw, TabbedPaneGrundlage ptp) {
	super(conn, mw);
        parentTabbedPane = ptp;
        createTabbedPane();
        //updateNArticleInProduktgruppe();
        //updateNArticleInLieferant();
    }

    protected abstract void createTabbedPane();

    public void recreateTabbedPane() {
        recreateTabbedPane(true);
    }

    public void recreateTabbedPane(boolean switchBack) {
        int selIndex = tabbedPane.getSelectedIndex();
        this.remove(tabbedPane);
	this.revalidate();
        createTabbedPane();
        if (switchBack){
            tabbedPane.setSelectedIndex(selIndex);
        }
        if (parentTabbedPane != null){
            parentTabbedPane.recreateTabbedPane(switchBack);
        }
    }
}
