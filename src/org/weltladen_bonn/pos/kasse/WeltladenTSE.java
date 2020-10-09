package org.weltladen_bonn.pos.kasse;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.Properties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.Path;

// GUI stuff:
import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

// TSE: (BSI, use implementation by Bundesdruckerei/D-Trust/cryptovision)
import com.cryptovision.SEAPI.TSE;
import com.cryptovision.SEAPI.TSE.LCS;
import com.cryptovision.SEAPI.TSE.AuthenticateUserResult;
import com.cryptovision.SEAPI.TSE.AuthenticationResult;
import com.cryptovision.SEAPI.exceptions.SEException;
import com.cryptovision.SEAPI.exceptions.ErrorTSECommandDataInvalid;
import com.cryptovision.SEAPI.exceptions.ErrorSECommunicationFailed;
import com.cryptovision.SEAPI.exceptions.ErrorSigningSystemOperationDataFailed;
import com.cryptovision.SEAPI.exceptions.ErrorStorageFailure;
import com.cryptovision.SEAPI.exceptions.ErrorRetrieveLogMessageFailed;
import com.cryptovision.SEAPI.exceptions.ErrorSecureElementDisabled;

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
    private Path pin_path = FileSystems.getDefault().getPath(System.getProperty("user.home"), ".Weltladenkasse_tse");

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
            logger.fatal("Unable to open connection to TSE, given configuration provided by '{}'.", "config_tse.txt");
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
            byte[] adminPIN = null;
            if (transport_state) {
                JOptionPane.showMessageDialog(this.mainWindow,
                    "ACHTUNG: Eine noch nicht initialisierte TSE wurde gefunden.\n"+
                    "Dies kommt vor, wenn eine neue TSE zum ersten mal eingesetzt wird.\n"+
                    "Es können jetzt die PINs und PUKs gesetzt werden, um die TSE zu initialisieren.\n"+
                    "Danach ist die TSE 'in Benutzung'.",
                    "Uninitialisierte TSE gefunden", JOptionPane.INFORMATION_MESSAGE);
                adminPIN = showInitializeTSEDialog();
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
            if (tse.getLifeCycleState() == LCS.notInitialized) {
                initializeTSE(adminPIN);
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

    public void printStatusValues() {
        System.out.println(
            "Eindeutige D-Trust-ID: "+
            new String(tse.getUniqueId(), StandardCharsets.ISO_8859_1) // (Abfrage der eindeutigen Identifikation einer jeden D-Trust TSE)
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
            System.out.println(
                "Lebenszyklus: "+
                lcs
            );
            System.out.println(
                "Gesamte Speichergröße: "+
                tse.getTotalLogMemory() / 1024 / 1024 + // (Abfrage der Größe des gesamten Speichers für abgesicherte Anwendungs- und Protokolldaten)
                " MB"
            );
            System.out.println(
                "Verfügbare Speichergröße: "+
                tse.getAvailableLogMemory() / 1024 / 1024 + // (Abfrage der Größe des freien Speichers für abgesicherte Anwendungs- und Protokolldaten)
                " MB"
            );
            System.out.println(
                "Verschleiß des Speichers: "+
                tse.getWearIndicator() // (Verschleißabfrage für den Speicher der abgesicherte Anwendungs- und Protokolldaten)
            );
            if (lcs.toString() != "notInitialized") {
                byte[] data = tse.exportSerialNumbers(); // (Rückgabe aller Signaturschlüssel-Seriennummern, sowie deren Verwendung)
		        byte[] serialNumber = Arrays.copyOfRange(data, 6, 6+32);
                System.out.println(
                    "Seriennummer(n) des/der Schlüssel(s): "+
                    serialNumber
                );
                System.out.println(
                    "Öffentlicher Schlüssel: "+
                    tse.exportPublicKey(serialNumber) // (Rückgabe eines öffentlichen Schlüssels)
                );
                System.out.println(
                    "Ablaufdatum des Zertifikats: "+
                    tse.getCertificateExpirationDate(serialNumber) // (Abfrage des Ablaufdatums eines Zertifikats)
                );
                System.out.println(
                    "Signatur-Zähler der letzten Signatur: "+
                    tse.getSignatureCounter(serialNumber) // (Abfrage des Signatur-Zählers der letzten Signatur)
                );
                System.out.println(
                    "Signatur-Algorithmus: "+
                    tse.getSignatureAlgorithm() // (Abfrage des Signatur-Algorithmus zur Absicherung von Anwendungs- und Protokolldaten)
                );
                System.out.println(
                    "Zuordnungen von IDs zu Schlüsseln: "+
                    new String(tse.getERSMappings(), StandardCharsets.ISO_8859_1) // (Abfrage aller Zuordnungen von Identifikationsnummern zu Signaturschlüsseln)
                );
                System.out.println(
                    "Maximale Clientzahl: "+
                    tse.getMaxNumberOfClients() // (Abfrage der maximal gleichzeitig unterstützten Kassen-Terminals)
                );
                System.out.println(
                    "Aktuelle Clientzahl: "+
                    tse.getCurrentNumberOfClients() // (Abfrage der derzeit in Benutzung befindlichen Kassen-Terminals)
                );
                System.out.println(
                    "Maximale Zahl offener Transaktionen: "+
                    tse.getMaxNumberOfTransactions() // (Abfrage der maximal gleichzeitig offenen Transaktionen)
                );
                System.out.println(
                    "Aktuelle Zahl offener Transaktionen: "+
                    tse.getCurrentNumberOfTransactions() // (Abfrage der Anzahl der derzeit offenen Transaktionen)
                );
                System.out.println(
                    "Transaktionsnummer der letzten Transaktionen: "+
                    tse.getTransactionCounter() // (Abfrage der Transaktionsnummer der letzten Transaktion)
                );
                System.out.println(
                    "Letzte Protokolldaten: "+
                    new String(tse.readLogMessage(), StandardCharsets.ISO_8859_1) // (Lesen des letzten gespeicherten und abgesicherten Anwendungs- und Protokolldatensatzes)
                );
            }
        } catch (SEException ex) {
            logger.error("Error at reading of TSE status values");
            logger.error("Exception: {}", ex);
        }
    }

    private byte[] showInitializeTSEDialog() {
        JDialog dialog = new JDialog(this.mainWindow, "Bitte PINs und PUKs der TSE eingeben", true);
        TSEInitDialog tseid = new TSEInitDialog(this.mainWindow, dialog, this);
        dialog.getContentPane().add(tseid, BorderLayout.CENTER);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        return tseid.getAdminPIN();
    }

    private byte[] showPINentryDialog(String role, String numbertype, int places) {
        JDialog dialog = new JDialog(this.mainWindow, "Bitte "+role+" "+numbertype+" der TSE eingeben", true);
        TSEPINEntryDialog tseped = new TSEPINEntryDialog(this.mainWindow, dialog, this, role, numbertype, places);
        dialog.getContentPane().add(tseped, BorderLayout.CENTER);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        return tseped.getPIN();
    }

    public void setPINandPUK(byte[] adminPIN, byte[] adminPUK, byte[] timeAdminPIN, byte[] timeAdminPUK) {
        boolean passed = false;
        String error = "";
        try {
            System.out.println("BEFORE initializePinValues():");
            printStatusValues();
            tse.initializePinValues(adminPIN, adminPUK, timeAdminPIN, timeAdminPUK);
            System.out.println("AFTER initializePinValues():");
            printStatusValues();
            passed = true;
        } catch (ErrorTSECommandDataInvalid ex) {
            error = "Data given to TSE's initializePinValues() invalid";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorSECommunicationFailed ex) {
            error = "SE communication failed";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorSigningSystemOperationDataFailed ex) {
            error = "Signing system operation data failed";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorStorageFailure ex) {
            error = "Storage failure";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (SEException ex) {
            error = "Unknown error during initializePinValues()";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        }
        if (!passed) {
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Die PINs und PUKs der TSE konnten nicht gesetzt werden!\n\n"+
                "Fehler: "+error+"\n\n"+
                "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                "wird die Kassensoftware jetzt beendet. Bitte Fehler beheben und\n"+
                "erneut versuchen.",
                "Fehlgeschlagene Initialisierung der TSE", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error
            System.exit(1);
        } else {
            writeTimeAdminPINtoFile(timeAdminPIN);
        }
    }

    public void writeTimeAdminPINtoFile(byte[] timeAdminPIN) {
        try {
            // Create a file in user's home directory that only she is allowed to read/write to (at least on Linux where file permissions exist)
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Files.createFile(pin_path);
            } else {
                Set<PosixFilePermission> ownerOnly = PosixFilePermissions.fromString("rw-------");
                FileAttribute<?> permissions = PosixFilePermissions.asFileAttribute(ownerOnly);
                Files.createFile(pin_path, permissions);
            }
            // Save the timeAdminPIN as a property to that file
            Properties props = new Properties();
            props.setProperty("timeAdminPIN", new String(timeAdminPIN));
            props.store(new FileOutputStream(pin_path.toFile()), "TSE properties for Weltladenkasse");
        } catch (IOException ex) {
            logger.error("Could not create file '~/.Weltladenkasse_tse' storing the TSE timeAdminPIN");
            logger.error("Exception: {}", ex);
        }
    }

    // method for reading the timeAdminPIN from a file in home directory whenever it is needed
    private byte[] readTimeAdminPINFromFile() {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(pin_path.toFile()));
            return props.getProperty("timeAdminPIN").getBytes();
        } catch (FileNotFoundException ex) {
            logger.error("File '~/.Weltladenkasse_tse' for storing timeAdminPIN not found.");
            logger.error("Exception: {}", ex);
            return showPINentryDialog("TimeAdmin", "PIN", 8);
        } catch (IOException ex) {
            logger.error("Could not read timeAdminPIN from file '~/.Weltladenkasse_tse'");
            logger.error("Exception: {}", ex);
            return showPINentryDialog("TimeAdmin", "PIN", 8);
        }
    }

    private void initializeTSE(byte[] adminPIN) {
        if (null == adminPIN) {
            adminPIN = showPINentryDialog("Admin", "PIN", 8);
        }
        boolean passed = false;
        String error = "";
        try {
            AuthenticateUserResult res = tse.authenticateUser("Admin", adminPIN);
            if (res.authenticationResult != AuthenticationResult.ok) {
                JOptionPane.showMessageDialog(this.mainWindow,
                    "ACHTUNG: Authentifizierungsfehler bei der TSE!\n\n"+
                    "authenticationResult: "+res.authenticationResult.toString()+"\n\n"+
                    "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                    "wird die Kassensoftware jetzt beendet. Bitte Fehler beheben und\n"+
                    "erneut versuchen.",
                    "Fehlgeschlagene Authentifizierung bei der TSE", JOptionPane.ERROR_MESSAGE);
                // Exit application upon this fatal error
                System.exit(1);
            }
            System.out.println("BEFORE initialize():");
            printStatusValues();
            tse.initialize();
            System.out.println("AFTER initialize():");
            printStatusValues();
            passed = true;
        } catch (ErrorSigningSystemOperationDataFailed ex) {
            error = "Signing system operation data failed";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorRetrieveLogMessageFailed ex) {
            error = "Retrieve log message failed";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorStorageFailure ex) {
            error = "Storage failure";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorSecureElementDisabled ex) {
            error = "Secure Element disabled";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (SEException ex) {
            error = "Unknown error during initializeTSE()";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        }
        if (!passed) {
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Die TSE konnte nicht initialisiert werden!\n\n"+
                "Fehler: "+error+"\n\n"+
                "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                "wird die Kassensoftware jetzt beendet. Bitte Fehler beheben und\n"+
                "erneut versuchen.",
                "Fehlgeschlagene Initialisierung der TSE", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error
            System.exit(1);
        }
    }
}
