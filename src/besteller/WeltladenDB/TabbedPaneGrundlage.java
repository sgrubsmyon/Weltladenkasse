package WeltladenDB;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

import javax.swing.JTabbedPane;

public abstract class TabbedPaneGrundlage extends WindowContent {
    protected JTabbedPane tabbedPane;

    // Methoden:
    public TabbedPaneGrundlage(Connection conn, MainWindowGrundlage mw) {
	super(conn, mw);
        createTabbedPane();
    }

    protected abstract void createTabbedPane();

    public void recreateTabbedPane() {
        this.remove(tabbedPane);
	this.revalidate();
        createTabbedPane();
    }
}
