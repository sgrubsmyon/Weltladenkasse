package org.weltladen_bonn.pos.kasse;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.Properties;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.FileAlreadyExistsException;
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
import com.cryptovision.SEAPI.exceptions.ErrorUserIdNotManaged;
import com.cryptovision.SEAPI.exceptions.ErrorUserNotAuthenticated;
import com.cryptovision.SEAPI.exceptions.ErrorUserNotAuthorized;
import com.cryptovision.SEAPI.exceptions.ErrorSeApiNotInitialized;
import com.cryptovision.SEAPI.exceptions.ErrorUpdateTimeFailed;
import com.cryptovision.SEAPI.exceptions.ErrorCertificateExpired;
import com.cryptovision.SEAPI.exceptions.ErrorTimeNotSet;
import com.cryptovision.SEAPI.exceptions.ErrorNoSuchKey;
import com.cryptovision.SEAPI.exceptions.ErrorERSalreadyMapped;

// For decoding/encoding output from the TSE:
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.util.ASN1Dump;
import java.io.ByteArrayInputStream;

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
    private boolean tseInUse = true;
    private MainWindow mainWindow = null;
    private Path pinPath = FileSystems.getDefault().getPath(System.getProperty("user.home"), ".Weltladenkasse_tse");

    private static long nextSyncTime = 0;
    private static int timeSyncInterval = 0;

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

    public boolean inUse() {
        return tseInUse;
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
                "Dies ist im Geschäftsbetrieb ILLEGAL und darf also nur für Testzwecke geschehen!!!\n"+
                "Wurde aus Versehen der Testmodus gewählt?",
                "Wirklich ohne TSE kassieren?", JOptionPane.WARNING_MESSAGE);
            logger.info("TSE object tse = {}", tse);
            tseInUse = false;
            logger.info("tseInUse = {}", tseInUse);
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
        boolean logged_in = false;
        try {
            boolean[] pinStatus = tse.getPinStatus();
            boolean transportState = pinStatus[0];
            byte[] adminPIN = null;
            if (transportState) {
                logger.info("TSE found, which is still in transport state!");
                JOptionPane.showMessageDialog(this.mainWindow,
                    "ACHTUNG: Eine noch nicht initialisierte TSE wurde gefunden.\n"+
                    "Dies kommt vor, wenn eine neue TSE zum ersten mal eingesetzt wird.\n"+
                    "Es können jetzt die PINs und PUKs gesetzt werden, um die TSE zu initialisieren.\n"+
                    "Danach ist die TSE 'in Benutzung'.",
                    "Uninitialisierte TSE gefunden", JOptionPane.INFORMATION_MESSAGE);
                adminPIN = showPINPUKDialog();
                // Re-check if PINs and PUKs were successfully set:
                pinStatus = tse.getPinStatus();
                transportState = pinStatus[0];
                if (transportState) {
                    logger.fatal("TSE PIN and PUK setting failed! (TSE still in transport state after setting PINs and PUKs)");
                    JOptionPane.showMessageDialog(this.mainWindow,
                        "ACHTUNG: Das Setzen der PINs und PUKs der TSE ist fehlgeschlagen!\n"+
                        "Ohne Setzen der PINs/PUKs kann eine neue TSE nicht verwendet werden.\n"+
                        "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                        "Bitte beim nächsten Start der Kassensoftware erneut probieren.",
                        "Fehler beim Setzen der PINs/PUKs der TSE", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
                logger.info("TSE's PIN and PUK values successfully set!");
            }
            if (tse.getLifeCycleState() == LCS.notInitialized) {
                logger.info("TSE still uninitialized. Now initializing...");
                authenticateAs("Admin", adminPIN);
                logged_in = true;
                initializeTSE();
                // Re-check if TSE was actually initialized:
                if (tse.getLifeCycleState() == LCS.notInitialized) {
                    logger.fatal("TSE initialization failed!");
                    JOptionPane.showMessageDialog(this.mainWindow,
                        "ACHTUNG: Die Initialisierung der TSE ist fehlgeschlagen!\n"+
                        "Ohne Initialisierung kann eine neue TSE nicht verwendet werden.\n"+
                        "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                        "Bitte beim nächsten Start der Kassensoftware erneut probieren.",
                        "Fehler bei der Initialisierung der TSE", JOptionPane.ERROR_MESSAGE);
                    logOutAs("Admin");
                    System.exit(1);
                }
                logger.info("TSE successfully initialized!");
            }
            // In any case:
            logger.info("Updating TSE's time for the first time after booting up");
            initTimeVars(); // set the time sync interval counter for the first time after booting up
            updateTime();
            // Re-check if time was actually set:
            if (tse.getLifeCycleState() == LCS.noTime) {
                logger.fatal("TSE time update failed!");
                JOptionPane.showMessageDialog(this.mainWindow,
                    "ACHTUNG: Die Aktualisierung der Zeit der TSE ist fehlgeschlagen!\n"+
                    "Ohne Zeitaktualisierung kann eine TSE nicht verwendet werden.\n"+
                    "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                    "Bitte beim nächsten Start der Kassensoftware erneut probieren.",
                    "Fehler beim Setzen der Zeit der TSE", JOptionPane.ERROR_MESSAGE);
                if (logged_in) {
                    logOutAs("Admin");
                }
                System.exit(1);
            }
            logger.info("TSE time successfully updated!");
            // XXX TODO
            // if not yet mapped client ID to key:
                // if (!logged_in) {
                //     authenticateAs("Admin", adminPIN);
                //     logged_in = true;
                // }
                // byte[] serialNumber = getSerialNumber();
                // mapClientIDToKey(serialNumber);
                // // Re-check client ID was actually mapped to key:
                // // XXX TODO
                // // if (tse.getLifeCycleState() == LCS.???) {
                // // }
            if (logged_in) {
                logOutAs("Admin");
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
            if (logged_in) {
                logOutAs("Admin");
            }
            System.exit(1);
        }
        return;
    }

    private String encodeByteArrayAsHexString(byte[] data) {
        return new String(Hex.encode(data));
    }

    private String decodeASN1ByteArray(byte[] data) {
        String result = null;
        try {
            ASN1InputStream ais = new ASN1InputStream(new ByteArrayInputStream(data));
            result = ASN1Dump.dumpAsString(ais.readObject(), true);
            ais.close();
        } catch (IOException ex) {
            logger.error("Failed to decode byte array using ASN1InputStream");
            logger.error("Exception: {}", ex);
        }
        return result;
    }

    public void printStatusValues() {
        System.out.println(
            "Eindeutige D-Trust-ID: "+
            encodeByteArrayAsHexString(tse.getUniqueId()) // (Abfrage der eindeutigen Identifikation einer jeden D-Trust TSE)
        );
        try {
            LCS lcs = tse.getLifeCycleState(); // (Status-Abfrage des Lebenszyklus)
            System.out.println(
                "BSI-Zertifizierungsnummer: "+
                tse.getCertificationId() // (Abfrage der BSI-Zertifizierungsnummer, Beispiel: "BSI-K-TR-0374-2020")
            );
            System.out.println(
                "Firmware-Version: "+
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
            if (lcs != LCS.notInitialized) {
                byte[] data = tse.exportSerialNumbers(); // (Rückgabe aller Signaturschlüssel-Seriennummern, sowie deren Verwendung)
		        byte[] serialNumber = Arrays.copyOfRange(data, 6, 6+32);
                System.out.println(
                    "Seriennummer(n) des/der Schlüssel(s) (Hex): "+
                    encodeByteArrayAsHexString(serialNumber)
                );
                System.out.println(
                    "Öffentlicher Schlüssel (Hex): "+
                    encodeByteArrayAsHexString(tse.exportPublicKey(serialNumber)) // (Rückgabe eines öffentlichen Schlüssels)
                );
                System.out.println(
                    "Ablaufdatum des Zertifikats: "+
                    tse.getCertificateExpirationDate(serialNumber) // (Abfrage des Ablaufdatums eines Zertifikats)
                );
                System.out.println(
                    "Zeitformat: "+
                    tse.getTimeSyncVariant() // (aus FirstBoot.java übernommen)
                );
                System.out.println(
                    "Signatur-Algorithmus (Hex): "+
                    encodeByteArrayAsHexString(tse.getSignatureAlgorithm()) // (Abfrage des Signatur-Algorithmus zur Absicherung von Anwendungs- und Protokolldaten)
                );
                System.out.println(
                    "Signatur-Algorithmus (ASN.1): "+
                    decodeASN1ByteArray(tse.getSignatureAlgorithm()) // (Abfrage des Signatur-Algorithmus zur Absicherung von Anwendungs- und Protokolldaten)
                );
                System.out.println(
                    "Signatur-Zähler der letzten Signatur: "+
                    tse.getSignatureCounter(serialNumber) // (Abfrage des Signatur-Zählers der letzten Signatur)
                );
                System.out.println(
                    "Zuordnungen von Kassen-IDs zu Schlüsseln (Hex): "+
                    encodeByteArrayAsHexString(tse.getERSMappings()) // (Abfrage aller Zuordnungen von Identifikationsnummern zu Signaturschlüsseln)
                );
                System.out.println(
                    "Zuordnungen von Kassen-IDs zu Schlüsseln (ASN.1): "+
                    decodeASN1ByteArray(tse.getERSMappings()) // (Abfrage aller Zuordnungen von Identifikationsnummern zu Signaturschlüsseln)
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
                    "Transaktionsnummer der letzten Transaktion: "+
                    tse.getTransactionCounter() // (Abfrage der Transaktionsnummer der letzten Transaktion)
                );
                System.out.println(
                    "Unterstützte Transaktionsaktualisierungsvarianten: "+
                    tse.getSupportedTransactionUpdateVariants() // (aus FirstBoot.java übernommen)
                );
                System.out.println(
                    "Letzte Protokolldaten (ASN.1): "+
                    decodeASN1ByteArray(tse.readLogMessage()) // (Lesen des letzten gespeicherten und abgesicherten Anwendungs- und Protokolldatensatzes)
                );
            }
        } catch (SEException ex) {
            logger.error("Error at reading of TSE status values");
            logger.error("Exception: {}", ex);
        }
    }

    private byte[] showPINPUKDialog() {
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
            System.out.println("\nBEFORE initializePinValues():");
            printStatusValues();
            tse.initializePinValues(adminPIN, adminPUK, timeAdminPIN, timeAdminPUK);
            System.out.println("\nAFTER initializePinValues():");
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
                "Fehler: "+error+".\n\n"+
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
                Files.createFile(pinPath);
            } else {
                Set<PosixFilePermission> ownerOnly = PosixFilePermissions.fromString("rw-------");
                FileAttribute<?> permissions = PosixFilePermissions.asFileAttribute(ownerOnly);
                Files.createFile(pinPath, permissions);
            }
        } catch (FileAlreadyExistsException ex) {
            logger.warn("File '~/.Weltladenkasse_tse' storing the TSE timeAdminPIN already exists. It is now overwritten.");
        } catch (IOException ex) {
            logger.error("Could not create file '~/.Weltladenkasse_tse' storing the TSE timeAdminPIN");
            logger.error("Exception: {}", ex);
        }
        try {
            // Save the timeAdminPIN as a property to that file
            Properties props = new Properties();
            props.setProperty("timeAdminPIN", new String(timeAdminPIN));
            props.store(new FileOutputStream(pinPath.toFile()), "TSE properties for Weltladenkasse");
        } catch (IOException ex) {
            logger.error("Could not write TSE timeAdminPIN to file '~/.Weltladenkasse_tse'.");
            logger.error("Exception: {}", ex);
        }
    }

    // method for reading the timeAdminPIN from a file in home directory whenever it is needed
    private byte[] readTimeAdminPINFromFile() {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(pinPath.toFile()));
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

    private void authenticateAs(String user, byte[] pin) {
        if (null == pin) {
            pin = showPINentryDialog(user, "PIN", 8);
        }
        boolean passed = false;
        String error = "";
        try {
            AuthenticateUserResult res = tse.authenticateUser(user, pin);
            if (res.authenticationResult != AuthenticationResult.ok) {
                error = "Authentication error for user "+user+": "+res.authenticationResult.toString();
                logger.fatal("Fatal Error: {}", error);
                JOptionPane.showMessageDialog(this.mainWindow,
                    "ACHTUNG: Authentifizierungsfehler als User "+user+" bei der TSE!\n\n"+
                    "authenticationResult: "+res.authenticationResult.toString()+"\n\n"+
                    "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                    "wird die Kassensoftware jetzt beendet. Bitte Fehler beheben und\n"+
                    "erneut versuchen.",
                    "Fehlgeschlagene Authentifizierung bei der TSE", JOptionPane.ERROR_MESSAGE);
                // Exit application upon this fatal error
                System.exit(1);
            }
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
            error = "Secure element disabled";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (SEException ex) {
            error = "Unknown error during authenticateUser()";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        }
        if (!passed) {
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Es konnte sich nicht als User "+user+" an der TSE angemeldet werden!\n\n"+
                "Fehler: "+error+".\n\n"+
                "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                "wird die Kassensoftware jetzt beendet. Bitte Fehler beheben und\n"+
                "erneut versuchen.",
                "Fehlgeschlagene Initialisierung der TSE", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error
            System.exit(1);
        }
    }

    private void logOutAs(String user) {
        boolean passed = false;
        String error = "";
        try {
            tse.logOut(user);
            passed = true;
        } catch (ErrorUserIdNotManaged ex) {
            error = "User ID not managed";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorSigningSystemOperationDataFailed ex) {
            error = "Signing system operation data failed";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorUserNotAuthenticated ex) {
            error = "User ID not authenticated";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorUserNotAuthorized ex) {
            error = "User not authorized";
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
            error = "Secure element disabled";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (SEException ex) {
            error = "Unknown error during logOut()";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        }
        if (!passed) {
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Es konnte sich nicht als "+user+" von der TSE abgemeldet werden!\n\n"+
                "Fehler: "+error+".\n\n"+
                "Dies weist auf eine fehlerhafte TSE hin. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                "wird die Kassensoftware jetzt beendet. Bitte Fehler beheben und\n"+
                "erneut versuchen.",
                "Fehlgeschlagene Abmeldung von der TSE", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error
            System.exit(1);
        }
    }

    private void initializeTSE() {
        boolean passed = false;
        String error = "";
        try {
            System.out.println("\nBEFORE initialize():");
            printStatusValues();
            tse.initialize();
            System.out.println("\nAFTER initialize():");
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
            error = "Unknown error during initialize()";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        }
        if (!passed) {
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Die TSE konnte nicht initialisiert werden!\n\n"+
                "Fehler: "+error+".\n\n"+
                "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                "wird die Kassensoftware jetzt beendet. Bitte Fehler beheben und\n"+
                "erneut versuchen.",
                "Fehlgeschlagene Initialisierung der TSE", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error
            logOutAs("Admin");
            System.exit(1);
        }
    }

    private void initTimeVars() {
        boolean passed = false;
        String error = "";
        try {
            nextSyncTime = 0;
            timeSyncInterval = tse.getTimeSyncInterval();
            passed = true;
        } catch (ErrorSeApiNotInitialized ex) {
            error = "SE API not initialized";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorSecureElementDisabled ex) {
            error = "Secure Element disabled";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (SEException ex) {
            error = "Unknown error during getTimeSyncInterval()";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        }
        if (!passed) {
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Das Zeitaktualisierungsintervall der TSE konnte nicht ausgelesen werden!\n\n"+
                "Fehler: "+error+".\n\n"+
                "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                "wird die Kassensoftware jetzt beendet. Bitte Fehler beheben und\n"+
                "erneut versuchen.",
                "Fehlgeschlagene Zeitaktualisierung der TSE", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error
            System.exit(1);
        }
    }

    private void updateTime() {
        // Check if update of time is necessary (and only then do it):
        long currentUtcTime = System.currentTimeMillis() / 1000;
        if (currentUtcTime <= nextSyncTime) {
            return; // do nothing
        }
        byte[] timeAdminPIN = readTimeAdminPINFromFile();
        boolean passed = false;
        String error = "";
        try {
            System.out.println("\nBEFORE updateTime():");
            printStatusValues();
            authenticateAs("TimeAdmin", timeAdminPIN);
            tse.updateTime(currentUtcTime);
            logOutAs("TimeAdmin");
            nextSyncTime = currentUtcTime + timeSyncInterval;
            System.out.println("\nAFTER updateTime():");
            printStatusValues();
            passed = true;
        } catch (ErrorUpdateTimeFailed ex) {
            error = "Update time failed";
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
        } catch (ErrorSeApiNotInitialized ex) {
            error = "SE API not initialized";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorCertificateExpired ex) {
            error = "Certificate expired";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorSecureElementDisabled ex) {
            error = "Secure Element disabled";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorUserNotAuthorized ex) {
            error = "User not authorized";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorUserNotAuthenticated ex) {
            error = "User not authenticated";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (SEException ex) {
            error = "Unknown error during updateTime()";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        }
        if (!passed) {
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Die Zeit der TSE konnte nicht aktualisiert werden!\n\n"+
                "Fehler: "+error+".\n\n"+
                "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                "wird die Kassensoftware jetzt beendet. Bitte Fehler beheben und\n"+
                "erneut versuchen.",
                "Fehlgeschlagene Zeitaktualisierung der TSE", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error
            logOutAs("TimeAdmin");
            System.exit(1);
        }
    }

    private byte[] getSerialNumber() {
        boolean passed = false;
        String error = "";
        byte[] serialNumber = null;
        try {
            /** Get the serial numbers of the available keys */
            byte[] data = tse.exportSerialNumbers();
            serialNumber = Arrays.copyOfRange(data, 6, 6+32);
            passed = true;
        } catch (ErrorSeApiNotInitialized ex) {
            error = "SE API not initialized";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (SEException ex) {
            error = "Unknown error during exportSerialNumbers()";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        }
        if (!passed) {
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Die Seriennummer des TSE-Schlüssels konnte nicht ausgelesen werden!\n\n"+
                "Fehler: "+error+".\n\n"+
                "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                "wird die Kassensoftware jetzt beendet. Bitte Fehler beheben und\n"+
                "erneut versuchen.",
                "Fehlgeschlagene Seriennummerauslesung der TSE", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error
            logOutAs("Admin");
            System.exit(1);
        }
        return serialNumber;
    }

    private void mapClientIDToKey(byte[] serialNumber) {
        boolean passed = false;
        String error = "";
        try {
            System.out.println("\nBEFORE mapERStoKey():");
            printStatusValues();

            byte[] mappings = tse.getERSMappings();
            // System.out.println(decodeASN1ByteArray(mappings));

            /** Configure the TSE to use the ERS "WeltladenBonnKasse" with the given transaction key (serial number) */
            tse.mapERStoKey("WeltladenBonnKasse", serialNumber);

            mappings = tse.getERSMappings();
            // System.out.println(decodeASN1ByteArray(mappings));

            System.out.println("\nAFTER mapERStoKey():");
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
        } catch (ErrorSeApiNotInitialized ex) {
            error = "SE API not initialized";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorCertificateExpired ex) {
            error = "Certificate expired";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorTimeNotSet ex) {
            error = "Time not set";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorSecureElementDisabled ex) {
            error = "Secure Element disabled";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorUserNotAuthorized ex) {
            error = "User not authorized";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorUserNotAuthenticated ex) {
            error = "User not authenticated";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorNoSuchKey ex) {
            error = "No such key";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorSECommunicationFailed ex) {
            error = "SE communication failed";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (ErrorERSalreadyMapped ex) {
            error = "ERS already mapped";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        } catch (SEException ex) {
            error = "Unknown error during mapERStoKey()";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception: {}", ex);
        }
        if (!passed) {
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Die Zeit der TSE konnte nicht aktualisiert werden!\n\n"+
                "Fehler: "+error+".\n\n"+
                "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                "wird die Kassensoftware jetzt beendet. Bitte Fehler beheben und\n"+
                "erneut versuchen.",
                "Fehlgeschlagene Zeitaktualisierung der TSE", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error
            logOutAs("Admin");
            System.exit(1);
        }
    }

}
