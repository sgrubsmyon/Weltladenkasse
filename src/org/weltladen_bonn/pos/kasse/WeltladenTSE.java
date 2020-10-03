package org.weltladen_bonn.pos.kasse;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Arrays;

// GUI stuff:
import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

// TSE: (BSI, use implementation by Bundesdruckerei/D-Trust/cryptovision)
import com.cryptovision.SEAPI.TSE;
import com.cryptovision.SEAPI.TSE.LCS;
import com.cryptovision.SEAPI.exceptions.SEException;

// Logging:
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is for interaction with a TSE (Technische Sicherheitseinrichtung),
 * based on BSI TR-03151 SE API. In this case, TR-03151 is implemented by
 * a device from cryptovision/D-Trust/Bundesdruckerei, but it could be
 * a different vendor as well (e.g. EPSON).
 */
public class WeltladenTSE {
    private static final Logger logger = LogManager.getLogger(Kundendisplay.class);

    private TSE tse = null;
    private MainWindow mainWindow = null;

    /**
     *    The constructor.
     *
     */
    public WeltladenTSE(MainWindow mw) {
        this.mainWindow = mw;
        connectToTSE();
        printStatusValues();
        checkInitializationStatus();
    }

    /**
     * Check if there is a TSE and if it needs to be initialized.
     */
    private void connectToTSE() {
        // Configure TSE:
        try {
            tse = TSE.getInstance("config_tse.txt");
        } catch (FileNotFoundException ex) {
            logger.warn("TSE config file not found under '{}'", "config_tse.txt");
            logger.warn("Exception: {}", ex);
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Es wird ohne TSE gearbeitet (weil keine Datei 'config_tse.txt' vorhanden ist)!!!\n"+
                "Dies ist im Geschäftsbetrieb ILLEGAL und darf also nur für Testzwecke sein!!!\n"+
                "Wurde aus Versehen der Testmodus gewählt?",
                "Wirklich ohne TSE kassieren?", JOptionPane.WARNING_MESSAGE);
            logger.info("TSE object tse = {}", tse);
        } catch (IOException ex) {
            logger.fatal("There is a TSE config file '{}', but it could not be read from it.", "config_tse.txt");
            logger.fatal("Exception: {}", ex);
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Die Datei 'config_tse.txt' konnte nicht eingelesen werden!\n"+
                "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                "wird die Kassensoftware jetzt beendet. Bitte Fehler in der Datei beheben und\n"+
                "erneut versuchen.",
                "Konfiguration der TSE nicht lesbar", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error, because a TSE config file is present
            // (so it seems usage of TSE is desired), but it could not be read from it.
            System.exit(1);
        } catch (SEException ex) {
            logger.fatal("Unable to open connection to TSE, given configuration provided by '{}'.", "config.txt");
            logger.fatal("Exception: {}", ex);
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Es konnte keine Verbindung zur TSE aufgebaut werden!\n"+
                "Entweder die TSE (eine SD-Karte, die rechts am Laptop in einem Schlitz steckt)\n"+
                "sitzt nicht richtig drin oder die Konfiguration in der Datei 'config_tse.txt'\n"+
                "ist falsch.\n"+
                "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                "Bitte Fehler beheben und erneut versuchen.",
                "Verbindung zur TSE nicht möglich", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error, because a TSE config file is present
            // (so it seems usage of TSE is desired), but maybe configuration is wrong or
            // TSE is not plugged in and no connection to TSE could be made.
            System.exit(1);
        }
        return;
    }

    private void checkInitializationStatus() {
        try {
            boolean[] pin_status = tse.getPinStatus();
            boolean transport_state = pin_status[0];
            if (transport_state) {
                JOptionPane.showMessageDialog(this.mainWindow,
                    "ACHTUNG: Eine noch nicht initialisierte TSE wurde gefunden.\n"+
                    "Dies kommt vor, wenn eine neue TSE zum ersten mal eingesetzt wird.\n"+
                    "Es können jetzt die PINs und PUKs gesetzt werden, um die TSE zu initialisieren.\n"+
                    "Danach ist die TSE 'in Benutzung'.",
                    "Uninitialisierte TSE gefunden", JOptionPane.INFORMATION_MESSAGE);
                initializeTSE();
                // Re-check if TSE was actually initialized:
                pin_status = tse.getPinStatus();
                transport_state = pin_status[0];
                if (transport_state) {
                    logger.fatal("TSE initialization failed! (TSE still in transport state after initialization)");
                    JOptionPane.showMessageDialog(this.mainWindow,
                        "ACHTUNG: Die Initialisierung der TSE ist fehlgeschlagen!\n"+
                        "Ohne Initialisierung kann eine neue TSE nicht verwendet werden.\n"+
                        "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                        "Bitte beim nächsten Start der Kassensoftware erneut probieren.",
                        "Fehler bei der Initialisierung der TSE", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
        } catch (SEException ex) {
            logger.fatal("Unable to check initialization status of TSE");
            logger.fatal("Exception: {}", ex);
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Es konnte nicht geprüft werden, ob die TSE bereits initialisiert ist!\n"+
                "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                "Bitte Fehler beheben und erneut versuchen.",
                "Prüfung der TSE-Initialisierung fehlgeschlagen", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error, because a TSE config file is present
            // (so it seems usage of TSE is desired), but maybe configuration is wrong or
            // TSE is not plugged in and no connection to TSE could be made.
            System.exit(1);
        }
        return;
    }

    private void printStatusValues() {
        System.out.println(
            "Eindeutige D-Trust-ID: "+
            new String(tse.getUniqueId()) // (Abfrage der eindeutigen Identifikation einer jeden D-Trust TSE)
        );
        try {
            LCS lcs = tse.getLifeCycleState(); // (Status-Abfrage des Lebenszyklus)
            System.out.println(
                "BSI-Zertifizierungsnummer: "+
                tse.getCertificationId() // (Abfrage der BSI-Zertifizierungsnummer, Beispiel: "BSI-K-TR-0374-2020")
            );
            System.out.println(
                "Firmware-ID: "+
                tse.getFirmwareId() // (Abfrage des Firmware Identifikations-Strings)
            );
            System.out.println("Lebenszyklus: "+lcs);
            if (lcs.toString() != "notInitialized") {
                System.out.println(
                    "Seriennummer(n) des/der Schlüssel(s): "+
                    tse.exportSerialNumbers() // (Rückgabe aller Signaturschlüssel-Seriennummern, sowie deren Verwendung)
                );
                // System.out.println(
                //     "Ablaufdatum des Zertifikats: "+
                // );
            }
        } catch (SEException ex) {
            logger.error("Error at reading of TSE status values");
            logger.error("Exception: {}", ex);
        }
        // tse.getCertificateExpirationDate() // (Abfrage des Ablaufdatums eines Zertifikats)
        // tse.getERSMappings() // (Abfrage aller Zuordnungen von Identifikationsnummern zu Signaturschlüsseln)
        // tse.readLogMessage() // (Lesen des letzten gespeicherten und abgesicherten Anwendungs- und Protokolldatensatzes)
        // tse.getMaxNumberOfClients() // (Abfrage der maximal gleichzeitig unterstützten Kassen-Terminals)
        // tse.getCurrentNumberOfClients() // (Abfrage der derzeit in Benutzung befindlichen Kassen-Terminals)
        // tse.getMaxNumberOfTransactions() // (Abfrage der maximal gleichzeitig offenen Transaktionen)
        // tse.getCurrentNumberOfTransactions() // (Abfrage der Anzahl der derzeit offenen Transaktionen)
        // tse.getTransactionCounter() // (Abfrage der Transaktionsnummer der letzten Transaktion)
        // tse.getTotalLogMemory() // (Abfrage der Größe des gesamten Speichers für abgesicherte Anwendungs- und Protokolldaten)
        // tse.getAvailableLogMemory() // (Abfrage der Größe des freien Speichers für abgesicherte Anwendungs- und Protokolldaten)
        // tse.getSignatureCounter() // (Abfrage des Signatur-Zählers  der letzten Signatur)
        // tse.getSignatureAlgorithm() // (Abfrage des Signatur-Algorithmus zur Absicherung von Anwendungs- und Protokolldaten)
        // tse.getWearIndicator() // (Verschleißabfrage für den Speicher der abgesicherte Anwendungs- und Protokolldaten)
        // tse.exportPublicKey() // (Rückgabe eines öffentlichen Schlüssels)
    }

    private void initializeTSE() {
        JDialog dialog = new JDialog(this.mainWindow, "Bitte PINs und PUKs der TSE eingeben", true);
        TSEInitDialog tseid = new TSEInitDialog(this.mainWindow, dialog, tse);
        dialog.getContentPane().add(tseid, BorderLayout.CENTER);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        return;
    }

}