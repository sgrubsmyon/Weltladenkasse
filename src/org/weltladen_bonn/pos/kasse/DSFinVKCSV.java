package org.weltladen_bonn.pos.kasse;

/** 
 * Class for generation of the 20 DSFinV-K CSV files:
 *   * Stammdatenmodul: (one row per Tagesabschluss)
 *       * cashpointclosing.csv
 *       * ...
 *       * tse.csv
 *   * Kassenabschlussmodul: (one row per Tagesabschluss)
 *       * ...
 *   * Einzelaufzeichnungsmodul: (one row per Verkauf/transaction)
 *       * ...
 *       * TSE_Transaktionen
 */

import org.weltladen_bonn.pos.BaseClass;

// MySQL Connector/J stuff:
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.mariadb.jdbc.MariaDbPoolDataSource;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DSFinVKCSV {
    private static final Logger logger = LogManager.getLogger(DSFinVKCSV.class);

    private MainWindow mw;
    private BaseClass bc;
    private MariaDbPoolDataSource pool; // pool of connections to MySQL database

    /**
     *    The constructor.
     *
     */
    public DSFinVKCSV(MainWindow mw, BaseClass bc, MariaDbPoolDataSource pool) {
        this.mw = mw;
        this.bc = bc;
        this.pool = pool;
    }

}