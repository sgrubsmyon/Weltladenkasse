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
    }

    protected abstract void createTabbedPane();

    public void recreateTabbedPane() {
        this.remove(tabbedPane);
	this.revalidate();
        if (parentTabbedPane != null){
            parentTabbedPane.recreateTabbedPane();
        }
        createTabbedPane();
    }
}
