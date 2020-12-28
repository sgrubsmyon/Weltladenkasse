package org.weltladen_bonn.pos.kasse;

import org.weltladen_bonn.pos.BaseClass;
import org.weltladen_bonn.pos.LongOperationIndicatorDialog;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Set;
import java.util.Properties;
import java.util.HashMap;
import java.util.Vector;
import java.util.Base64;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Collections;

// GUI stuff:
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;

// TSE: (BSI, use implementation by Bundesdruckerei/D-Trust/cryptovision)
import com.cryptovision.SEAPI.TSE;
import com.cryptovision.SEAPI.TSE.LCS;
import com.cryptovision.SEAPI.TSE.AuthenticateUserResult;
import com.cryptovision.SEAPI.TSE.AuthenticationResult;
import com.cryptovision.SEAPI.TSE.StartTransactionResult;
import com.cryptovision.SEAPI.TSE.UpdateTransactionResult;
import com.cryptovision.SEAPI.TSE.FinishTransactionResult;
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
import com.cryptovision.SEAPI.exceptions.ErrorStartTransactionFailed;
import com.cryptovision.SEAPI.exceptions.ErrorUpdateTransactionFailed;
import com.cryptovision.SEAPI.exceptions.ErrorNoTransaction;
import com.cryptovision.SEAPI.exceptions.ErrorFinishTransactionFailed;
import com.cryptovision.SEAPI.exceptions.ErrorIdNotFound;
import com.cryptovision.SEAPI.exceptions.ErrorStreamWrite;
import com.cryptovision.SEAPI.exceptions.ErrorUnexportedStoredData;
import com.cryptovision.SEAPI.exceptions.ErrorTooManyRecords;

// For decoding/encoding output from the TSE:
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.util.ASN1Dump;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.bsi.BSIObjectIdentifiers;
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

    private BaseClass bc;

    private TSE tse = null;
    private boolean tseInUse = true;
    private boolean loggedIn = false;
    private MainWindow mainWindow = null;
    private Path pinPath = FileSystems.getDefault().getPath(System.getProperty("user.home"), ".Weltladenkasse_tse");
    private String dateFormatDSFinVK = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"; // YYYY-MM-DDThh:mm:ss.fffZ, see https://www.bzst.de/DE/Unternehmen/Aussenpruefungen/DigitaleSchnittstelleFinV/digitaleschnittstellefinv_node.html

    private static long nextSyncTime = 0;
    private static int timeSyncInterval = 0;

    public static String[] statusValueKeys = {
        "Eindeutige D-Trust-ID",
        "BSI-Zertifizierungsnummer",
        "Firmware-Version",
        "Gesamte Speichergröße",
        "Verfügbare Speichergröße",
        "Verschleiß des Speichers",
        "Lebenszyklus",
        "Transaktionsnummer",
        "Signatur-Zähler",
        "Seriennummer der TSE (Hex)",
        "Seriennummer der TSE (Base64)",
        "Seriennummern aller Schlüssel (ASN.1)",
        "Öffentlicher Schlüssel (Hex)",
        "Öffentlicher Schlüssel (Base64)",
        "Ablaufdatum des Zertifikats",
        "Ablaufdatum des Zertifikats (Unix-Time)",
        "Zeitformat",
        "Signatur-Algorithmus",
        "Signatur-Algorithmus (ASN.1)",
        "TSE_OID des Signatur-Algorithmus", // 0.4.0.127.0.7.1.1.4.1.3 stands for ecdsa-plain-SHA256 (https://github.com/bcgit/bc-java/blob/master/core/src/main/java/org/bouncycastle/asn1/bsi/BSIObjectIdentifiers.java, http://www.bouncycastle.org/docs/docs1.5on/org/bouncycastle/asn1/bsi/BSIObjectIdentifiers.html#ecdsa_plain_SHA256)
        "Zuordnungen von Kassen-IDs zu Schlüsseln (ASN.1)",
        "Maximale Anzahl Kassen-Terminals",
        "Aktuelle Anzahl Kassen-Terminals",
        "Maximale Zahl offener Transaktionen",
        "Aktuelle Zahl offener Transaktionen",
        "Unterstützte Transaktionsaktualisierungsvarianten",
        "Letzte Protokolldaten (ASN.1)"
    };

    /**
     *    The constructor.
     *
     */
    public WeltladenTSE(MainWindow mw, BaseClass bc) {
        this.mainWindow = mw;
        this.bc = bc;
        connectToTSE();
        if (tseInUse) {
            LongOperationIndicatorDialog dialog = new LongOperationIndicatorDialog(
                new JLabel("TSE wird initialisiert..."),
                null
            );

            checkInitializationStatus();
            // System.out.println("\n\n*** WRITING FIRST TRANSACTION TO TSE ***");
            // System.out.println("\n --- Status before: \n");
            // printStatusValues();
            // for (int i = 0; i < 50; i++) {
            writeTestTransaction();
            // }
            // System.out.println("\n --- Status after: \n");
            // printStatusValues();
            // exportTransactionData();

            // exportFullTransactionData("./export_full_before_delete.tar");
            // exportPartialTransactionDataBySigCounter("./export_partial_test.tar", (long)0, (long)10);
            // deletePartialTransactionDataBySigCounter((long)(0 + 10));
            // exportFullTransactionData("./export_full_after_delete.tar");

            // // Test partial deletion:
            // exportPartialTransactionDataBySigCounter("/tmp/tse_export_31_40.tar", (long)30, (long)10);
            // deletePartialTransactionDataBySigCounter((long)45); // error: unexported data
            // exportPartialTransactionDataBySigCounter("/tmp/tse_export_31_40_2.tar", (long)0, (long)10);
            // exportPartialTransactionDataBySigCounter("/tmp/tse_export_41_50.tar", (long)40, (long)10);
            // deletePartialTransactionDataBySigCounter((long)40);
            // deletePartialTransactionDataBySigCounter((long)40); // error: id not found
            // deletePartialTransactionDataBySigCounter((long)41);
            // deletePartialTransactionDataBySigCounter((long)50);
            // deletePartialTransactionDataBySigCounter((long)60); // error: unexported data
            // exportPartialTransactionDataBySigCounter("/tmp/tse_export_51_60.tar", (long)0, (long)10);
            // exportPartialTransactionDataBySigCounter("/tmp/tse_export_51_60_2.tar", (long)50, (long)10);
            // deletePartialTransactionDataBySigCounter((long)60);

            dialog.dispose();
        }
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
            // Probably has no effect and tseStartUpWorkaroundLoop() is way better:
            // logger.info("Before tse.close()");
            // tse.close(); // disconnect to fix communication issues
            // logger.info("After tse.close()");
            // tse = TSE.getInstance("config_tse.txt"); // re-connect to continue
        } catch (FileNotFoundException ex) {
            logger.warn("TSE config file not found under '{}'", "config_tse.txt");
            logger.warn("Exception:", ex);
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
            logger.fatal("Exception:", ex);
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Die Datei 'config_tse.txt' konnte nicht eingelesen werden!\n"+
                "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                "wird die Kassensoftware jetzt beendet. Bitte Fehler in der Datei beheben und\n"+
                "erneut versuchen.",
                "Konfiguration der TSE nicht lesbar", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error, because a TSE config file is present
            // (so it seems usage of TSE is desired), but it could not be read from it.
            disconnectFromTSE();
            System.exit(1);
        } catch (SEException ex) {
            logger.fatal("Unable to open connection to TSE, given configuration provided by '{}'.", "config_tse.txt");
            logger.fatal("Exception:", ex);
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
            disconnectFromTSE();
            System.exit(1);
        } catch(Throwable ex) {
            logger.fatal("Throwable caught in connectToTSE");
            logger.fatal("Exception:", ex);
        }
    }

    public void disconnectFromTSE() {
        if (tseInUse) {
            try {
                tse.close();
            } catch (IOException ex) {
                logger.fatal("IOException upon closing connection to TSE");
                logger.fatal("Exception:", ex);
            } catch (SEException ex) {
                logger.fatal("SEException upon closing connection to TSE");
                logger.fatal("Exception:", ex);
            } catch(Throwable ex) {
                logger.fatal("Throwable caught in disconnectFromTSE");
                logger.fatal("Exception:", ex);
            }
        }
    }

    private LongOperationIndicatorDialog installLongOperationIndicatorDialogWithCancelButton() {
        JPanel buttonPanel = new JPanel();
        JButton cancelButton = new JButton("Abbrechen");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logger.info("User canceled the start-up workaround loop");
                disconnectFromTSE();
                System.exit(0);
            }
        });
        buttonPanel.add(cancelButton);
        LongOperationIndicatorDialog dialog = new LongOperationIndicatorDialog(
            new JLabel("Versuche, Verbindung zur TSE aufzubauen (kann ca. 30-60 Sekunden dauern)..."),
            buttonPanel
        );
        return dialog;
    }

    private void tseStartUpWorkaroundLoop() {
        byte[] timeAdminPIN = readTimeAdminPINFromFile();
        LongOperationIndicatorDialog dialog = installLongOperationIndicatorDialogWithCancelButton();
        boolean tseOperational = false;
        while (!tseOperational) {
            logger.info("Trying to authenticate as user 'TimeAdmin'...");
            String message = authenticateAs("TimeAdmin", timeAdminPIN, false);
            if (message == "OK") {
                logger.info("Success!!! Now we can continue normally...");
                dialog.dispose();
                logOutAs("TimeAdmin");
                tseOperational = true;
            } else {
                logger.info("Failed. Trying again...");
                // Because connection to TSE was closed, we need to re-open it:
                connectToTSE();
            }
        }
    }

    private void checkInitializationStatus() {
        loggedIn = false;
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
                    disconnectFromTSE();
                    System.exit(1);
                }
                logger.info("TSE's PIN and PUK values successfully set!");
            }
            if (tse.getLifeCycleState() == LCS.notInitialized) {
                logger.info("TSE still uninitialized. Now initializing...");
                authenticateAs("Admin", adminPIN, true);
                loggedIn = true;
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
                    loggedIn = false;
                    disconnectFromTSE();
                    System.exit(1);
                }
                logger.info("TSE successfully initialized!");
            }
            // In any case:
            logger.info("Updating TSE's time for the first time after booting up");
            tseStartUpWorkaroundLoop();
            initTimeVars(); // set the time sync interval counter for the first time after booting up
            updateTimeWithoutChecking();
            // Re-check if time was actually set:
            if (tse.getLifeCycleState() == LCS.noTime) {
                logger.fatal("TSE time update failed!");
                JOptionPane.showMessageDialog(this.mainWindow,
                    "ACHTUNG: Die Aktualisierung der Zeit der TSE ist fehlgeschlagen!\n"+
                    "Ohne Zeitaktualisierung kann eine TSE nicht verwendet werden.\n"+
                    "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                    "Bitte beim nächsten Start der Kassensoftware erneut probieren.",
                    "Fehler beim Setzen der Zeit der TSE", JOptionPane.ERROR_MESSAGE);
                if (loggedIn) {
                    logOutAs("Admin");
                    loggedIn = false;
                }
                disconnectFromTSE();
                System.exit(1);
            }
            logger.info("TSE time successfully updated!");
            // If client ID not yet mapped to key:
            if (encodeByteArrayAsHexString(tse.getERSMappings()).equals("3000")) { // XXX is this the general value for unmapped?
                logger.debug("01 ERS Mappings equal 0");
                if (!loggedIn) {
                    logger.debug("02 Before authenticateAs");
                    authenticateAs("Admin", adminPIN, true);
                    logger.debug("03 After authenticateAs");
                    loggedIn = true;
                }
                logger.debug("04 Before getSerialNumber");
                byte[] serialNumber = getSerialNumber();
                logger.debug("05 After getSerialNumber");
                mapClientIDToKey(serialNumber);
                logger.debug("06 After mapClientIDToKey");
                // Re-check if client ID is still unmapped to key:
                if (encodeByteArrayAsHexString(tse.getERSMappings()).equals("3000")) { // XXX is this the general value for unmapped?
                    logger.fatal("Mapping of client ID to TSE key failed!");
                    JOptionPane.showMessageDialog(this.mainWindow,
                        "ACHTUNG: Die Aktualisierung der Zeit der TSE ist fehlgeschlagen!\n"+
                        "Ohne Zeitaktualisierung kann eine TSE nicht verwendet werden.\n"+
                        "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                        "Bitte beim nächsten Start der Kassensoftware erneut probieren.",
                        "Fehler beim Setzen der Zeit der TSE", JOptionPane.ERROR_MESSAGE);
                    logOutAs("Admin");
                    loggedIn = false;
                    disconnectFromTSE();
                    System.exit(1);
                }
                logger.info("client ID successfully mapped to TSE key!");
            }
            if (loggedIn) {
                logger.debug("07 Before logOutAs");
                logOutAs("Admin");
                loggedIn = false;
                logger.debug("08 After logOutAs");
            }
            logger.debug("09 After everything");
        } catch (SEException ex) {
            logger.fatal("Unable to check initialization status of TSE");
            logger.fatal("Exception:", ex);
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Es konnte nicht geprüft werden, ob die TSE bereits initialisiert ist!\n"+
                "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                "Bitte Fehler beheben und erneut versuchen.",
                "Prüfung der TSE-Initialisierung fehlgeschlagen", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error, because a TSE config file is present
            // (so it seems usage of TSE is desired), but maybe configuration is wrong or
            // TSE is not plugged in and no connection to TSE could be made.
            if (loggedIn) {
                logOutAs("Admin");
                loggedIn = false;
            }
            disconnectFromTSE();
            System.exit(1);
        }
        return;
    }

    private String encodeByteArrayAsHexString(byte[] data) {
        return new String(Hex.encode(data));
    }

    private String decodeASN1ByteArray(byte[] data) {
        String result = "null";
        try {
            ASN1InputStream ais = new ASN1InputStream(new ByteArrayInputStream(data));
            result = ASN1Dump.dumpAsString(ais.readObject(), true);
            ais.close();
        } catch (IOException ex) {
            logger.error("Failed to decode byte array using ASN1InputStream");
            logger.error("Exception:", ex);
        }
        return result;
    }

    private String decodeASN1ByteArrayObjectIdentifier(byte[] data) {
        String result = "null";
        try {
            ASN1Primitive oid = ASN1ObjectIdentifier.fromByteArray(data);
            result = oid.toString();
        } catch (IOException ex) {
            logger.error("Failed to decode byte array using ASN1ObjectIdentifier.fromByteArray()");
            logger.error("Exception:", ex);
        } catch (Exception ex) {
            logger.error("Failed to decode byte array using ASN1ObjectIdentifier.fromByteArray()");
            logger.error("Exception:", ex);
        }
        return result;
    }

    public String getSignatureAlgorithm() {
        String oid = "";
        try {
            oid = decodeASN1ByteArrayObjectIdentifier(tse.getSignatureAlgorithm());
        } catch (SEException ex) {
            logger.error("Error at getting TSE signature algorithm");
            logger.error("Exception:", ex);
        }
        if (oid.equals("["+BSIObjectIdentifiers.ecdsa_plain_SHA256.getId()+"]")) {
            return "ecdsa-plain-SHA256"; // used in cryptovision TSEs by default, see https://tse-support.cryptovision.com/confluence/pages/viewpage.action?pageId=10190881
        } else if (oid.equals("["+BSIObjectIdentifiers.ecdsa_plain_SHA1.getId()+"]")) {
            return "ecdsa-plain-SHA1";
        } else if (oid.equals("["+BSIObjectIdentifiers.ecdsa_plain_SHA224.getId()+"]")) {
            return "ecdsa-plain-SHA224";
        } else if (oid.equals("["+BSIObjectIdentifiers.ecdsa_plain_SHA384.getId()+"]")) {
            return "ecdsa-plain-SHA384";
        } else if (oid.equals("["+BSIObjectIdentifiers.ecdsa_plain_SHA512.getId()+"]")) {
            return "ecdsa-plain-SHA512";
        } else if (oid.equals("["+BSIObjectIdentifiers.ecdsa_plain_RIPEMD160.getId()+"]")) {
            return "ecdsa-plain-RIPEMD160";
        // More algorithms defined here: https://tse-support.cryptovision.com/confluence/pages/viewpage.action?pageId=10190881
        } else if (oid.equals("[0.4.0.127.0.7.3.7.1.1]")) {
            return "id-SE-API-transaction-log";
        } else if (oid.equals("[0.4.0.127.0.7.3.7.1.2]")) {
            return "id-SE-API-system-log";
        } else if (oid.equals("[0.4.0.127.0.7.3.7.1.3]")) {
            return "id-SE-API-SE-audit-log";
        } else {
            return "unknown";
        }
    }

    public String unixTimeToCalTime(long unixTime) {
        java.util.Date date = new java.util.Date(unixTime * 1000);
        return new SimpleDateFormat(dateFormatDSFinVK).format(date);
    }

    public String unixTimeToCalTime(long unixTime, String dateFormat) {
        java.util.Date date = new java.util.Date(unixTime * 1000);
        return new SimpleDateFormat(dateFormat).format(date);
    }

    public HashMap<String, String> retrieveTSEStatusValues(Vector<String> interestingValues) {
        HashMap<String, String> values = new HashMap<String, String>();
        if (interestingValues.size() == 0 || interestingValues.contains("Eindeutige D-Trust-ID")) {
            // Abfrage der eindeutigen Identifikation einer jeden D-Trust TSE
            values.put("Eindeutige D-Trust-ID", encodeByteArrayAsHexString(tse.getUniqueId()));
        }
        try {
            if (interestingValues.size() == 0 || interestingValues.contains("BSI-Zertifizierungsnummer")) {
                // Abfrage der BSI-Zertifizierungsnummer, Beispiel: "BSI-K-TR-0374-2020"
                values.put("BSI-Zertifizierungsnummer", tse.getCertificationId());
            }
            if (interestingValues.size() == 0 || interestingValues.contains("Firmware-Version")) {
                // Abfrage des Firmware Identifikations-Strings
                values.put("Firmware-Version", tse.getFirmwareId());
            }
            if (interestingValues.size() == 0 || interestingValues.contains("Gesamte Speichergröße")) {
                // Abfrage der Größe des gesamten Speichers für abgesicherte Anwendungs- und Protokolldaten
                values.put("Gesamte Speichergröße", tse.getTotalLogMemory() / 1024 / 1024 + " MB");
            }
            if (interestingValues.size() == 0 || interestingValues.contains("Verfügbare Speichergröße")) {
                // Abfrage der Größe des freien Speichers für abgesicherte Anwendungs- und Protokolldaten
                values.put("Verfügbare Speichergröße", tse.getAvailableLogMemory() / 1024 / 1024 + " MB");
            }
            if (interestingValues.size() == 0 || interestingValues.contains("Verschleiß des Speichers")) {
                // Verschleißabfrage für den Speicher der abgesicherte Anwendungs- und Protokolldaten
                values.put("Verschleiß des Speichers", String.valueOf(tse.getWearIndicator()));
            }
            // Status-Abfrage des Lebenszyklus
            LCS lcs = tse.getLifeCycleState();
            if (interestingValues.size() == 0 || interestingValues.contains("Lebenszyklus")) {
                values.put("Lebenszyklus", lcs.toString());
            }
            if (lcs != LCS.notInitialized) {
                // Erklärung zu Seriennummern auf https://tse-support.cryptovision.com/confluence/display/TDI/TSE-Seriennummer:
                // "Eine Seriennummer ist der SHA-256 Hashwert des öffentlichen Schlüssels, der zu dem Schlüsselpaar gehört,
                //  dessen privater Schlüssel zum Signieren der Protokollmeldungen verwendet wird."
                byte[] serialNumberData = tse.exportSerialNumbers(); // (Rückgabe aller Signaturschlüssel-Seriennummern, sowie deren Verwendung)
		        byte[] serialNumber = Arrays.copyOfRange(serialNumberData, 6, 6+32);
                if (interestingValues.size() == 0 || interestingValues.contains("Transaktionsnummer")) {
                    // Abfrage der Transaktionsnummer der letzten Transaktion, XXX sollte auf Quittung gedruckt werden!n
                    values.put("Transaktionsnummer", String.valueOf(tse.getTransactionCounter()));
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Signatur-Zähler")) {
                    // Abfrage des Signatur-Zählers der letzten Signatur, XXX sollte auf Quittung gedruckt werden!
                    values.put("Signatur-Zähler", String.valueOf(tse.getSignatureCounter(serialNumber)));
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Seriennummer der TSE (Hex)")) {
                    // Erste und auch einzige Signaturschlüssel-Seriennummer, XXX sollte auf Quittung gedruckt werden!
                    values.put("Seriennummer der TSE (Hex)", encodeByteArrayAsHexString(serialNumber));
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Seriennummer der TSE (Base64)")) {
                    // Erste und auch einzige Signaturschlüssel-Seriennummer, XXX sollte auf Quittung gedruckt werden!
                    values.put("Seriennummer der TSE (Base64)", byteArrayToBase64String(serialNumber));
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Seriennummern aller Schlüssel (ASN.1)")) {
                    // Alle Signaturschlüssel-Seriennummern
                    values.put("Seriennummern aller Schlüssel (ASN.1)", decodeASN1ByteArray(serialNumberData));
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Öffentlicher Schlüssel (Hex)")) {
                    // Rückgabe eines öffentlichen Schlüssels
                    values.put("Öffentlicher Schlüssel (Hex)", encodeByteArrayAsHexString(tse.exportPublicKey(serialNumber)));
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Öffentlicher Schlüssel (Base64)")) {
                    // Rückgabe eines öffentlichen Schlüssels
                    values.put("Öffentlicher Schlüssel (Base64)", byteArrayToBase64String(tse.exportPublicKey(serialNumber)));
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Ablaufdatum des Zertifikats") ||
                    interestingValues.contains("Ablaufdatum des Zertifikats (Unix-Time)")) {
                    long expirationTimestamp = tse.getCertificateExpirationDate(serialNumber);
                    if (interestingValues.size() == 0 || interestingValues.contains("Ablaufdatum des Zertifikats")) {
                        // Abfrage des Ablaufdatums eines Zertifikats
                        values.put("Ablaufdatum des Zertifikats", unixTimeToCalTime(expirationTimestamp, bc.dateFormatJava));
                    }
                    if (interestingValues.size() == 0 || interestingValues.contains("Ablaufdatum des Zertifikats (Unix-Time)")) {
                        // Abfrage des Ablaufdatums eines Zertifikats
                        values.put("Ablaufdatum des Zertifikats (Unix-Time)", String.valueOf(expirationTimestamp));
                    }
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Zeitformat")) {
                    // aus FirstBoot.java übernommen:
                    values.put("Zeitformat", tse.getTimeSyncVariant().toString());
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Signatur-Algorithmus")) {
                    // Abfrage des Signatur-Algorithmus zur Absicherung von Anwendungs- und Protokolldaten
                    values.put("Signatur-Algorithmus", getSignatureAlgorithm());
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Signatur-Algorithmus (ASN.1)")) {
                    // Abfrage des Signatur-Algorithmus zur Absicherung von Anwendungs- und Protokolldaten
                    values.put("Signatur-Algorithmus (ASN.1)", decodeASN1ByteArray(tse.getSignatureAlgorithm()));
                }
                if (interestingValues.size() == 0 || interestingValues.contains("TSE_OID des Signatur-Algorithmus")) {
                    // Abfrage des Signatur-Algorithmus zur Absicherung von Anwendungs- und Protokolldaten
                    values.put("TSE_OID des Signatur-Algorithmus", decodeASN1ByteArrayObjectIdentifier(tse.getSignatureAlgorithm()));
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Zuordnungen von Kassen-IDs zu Schlüsseln (ASN.1)")) {
                    // Abfrage aller Zuordnungen von Identifikationsnummern zu Signaturschlüsseln
                    values.put("Zuordnungen von Kassen-IDs zu Schlüsseln (ASN.1)", decodeASN1ByteArray(tse.getERSMappings()));
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Maximale Anzahl Kassen-Terminals")) {
                    // Abfrage der maximal gleichzeitig unterstützten Kassen-Terminals
                    values.put("Maximale Anzahl Kassen-Terminals", String.valueOf(tse.getMaxNumberOfClients()));
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Aktuelle Anzahl Kassen-Terminals")) {
                    // Abfrage der derzeit in Benutzung befindlichen Kassen-Terminals
                    values.put("Aktuelle Anzahl Kassen-Terminals", String.valueOf(tse.getCurrentNumberOfClients()));
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Maximale Zahl offener Transaktionen")) {
                    // Abfrage der maximal gleichzeitig offenen Transaktionen
                    values.put("Maximale Zahl offener Transaktionen", String.valueOf(tse.getMaxNumberOfTransactions()));
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Aktuelle Zahl offener Transaktionen")) {
                    // Abfrage der Anzahl der derzeit offenen Transaktionen
                    values.put("Aktuelle Zahl offener Transaktionen", String.valueOf(tse.getCurrentNumberOfTransactions()));
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Unterstützte Transaktionsaktualisierungsvarianten")) {
                    // aus FirstBoot.java übernommen
                    values.put("Unterstützte Transaktionsaktualisierungsvarianten", tse.getSupportedTransactionUpdateVariants().toString());
                }
                if (interestingValues.size() == 0 || interestingValues.contains("Letzte Protokolldaten (ASN.1)")) {
                    // Lesen des letzten gespeicherten und abgesicherten Anwendungs- und Protokolldatensatzes
                    values.put("Letzte Protokolldaten (ASN.1)", decodeASN1ByteArray(tse.readLogMessage()));
                }
            }
        } catch (SEException ex) {
            logger.error("Error at reading of TSE status values");
            logger.error("Exception:", ex);
        } finally {
            return values;
        }
    }

    public HashMap<String, String> retrieveTSEStatusValues() {
        return retrieveTSEStatusValues(new Vector<String>(0));
    }

    private void printStatusValues() {
        String[] interestingValuesArray = {
            // "Eindeutige D-Trust-ID", "Firmware-Version", "BSI-Zertifizierungsnummer",
            "Gesamte Speichergröße", "Verfügbare Speichergröße", "Verschleiß des Speichers",
            "Lebenszyklus", "Transaktionsnummer", "Signatur-Zähler",
            // "Seriennummer der TSE (Hex)", "Öffentlicher Schlüssel (Hex)",
            // "Ablaufdatum des Zertifikats", "Signatur-Algorithmus (ASN.1)",
            "Zuordnungen von Kassen-IDs zu Schlüsseln (ASN.1)"
        };
        Vector<String> interestingValues = new Vector<String>(Arrays.asList(interestingValuesArray));
        HashMap<String, String> values = retrieveTSEStatusValues(interestingValues);
        for (String s : interestingValues) {
            System.out.println(s + ": " + values.get(s));
        }
    }

     private void printAllStatusValues() {
        HashMap<String, String> values = retrieveTSEStatusValues();
        Set<String> interestingValues = values.keySet();
        for (String s : interestingValues) {
            System.out.println(s + ": " + values.get(s));
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
        System.out.println("\nBEFORE initializePinValues():");
        printStatusValues();
        LongOperationIndicatorDialog dialog = installLongOperationIndicatorDialogWithCancelButton();
        boolean tseOperational = false;
        while (!tseOperational) {
            logger.info("Trying to set PIN and PUK...");
            try {
                tse.initializePinValues(adminPIN, adminPUK, timeAdminPIN, timeAdminPUK);
                logger.info("Success!!! Now we can continue normally...");
                dialog.dispose();
                passed = true;
                tseOperational = true;
            } catch (ErrorTSECommandDataInvalid ex) {
                error = "Data given to TSE's initializePinValues() invalid";
                logger.fatal("Fatal Error: {}", error);
                logger.fatal("Exception:", ex);
            } catch (ErrorSECommunicationFailed ex) {
                error = "SE communication failed";
                logger.fatal("Fatal Error: {}", error);
                logger.fatal("Exception:", ex);
            } catch (ErrorSigningSystemOperationDataFailed ex) {
                error = "Signing system operation data failed";
                logger.fatal("Fatal Error: {}", error);
                logger.fatal("Exception:", ex);
            } catch (ErrorStorageFailure ex) {
                error = "Storage failure";
                logger.fatal("Fatal Error: {}", error);
                logger.fatal("Exception:", ex);
            } catch (SEException ex) {
                error = "Unknown error during initializePinValues()";
                logger.fatal("Fatal Error: {}", error);
                logger.fatal("Exception:", ex);
            }
            if (!passed) {
                // JOptionPane.showMessageDialog(this.mainWindow,
                //     "ACHTUNG: Die PINs und PUKs der TSE konnten nicht gesetzt werden!\n\n"+
                //     "Fehler: "+error+".\n\n"+
                //     "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                //     "wird die Kassensoftware jetzt beendet. Bitte Fehler beheben und\n"+
                //     "erneut versuchen.",
                //     "Fehlgeschlagene Initialisierung der TSE", JOptionPane.ERROR_MESSAGE);
                // Exit application upon this fatal error
                logger.info("Failed. Trying again...");
                disconnectFromTSE();
                connectToTSE();
            } else {
                writeTimeAdminPINtoFile(timeAdminPIN);
            }
        }
        System.out.println("\nAFTER initializePinValues():");
        printStatusValues();
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
            logger.error("Exception:", ex);
        }
        try {
            // Save the timeAdminPIN as a property to that file
            Properties props = new Properties();
            props.setProperty("timeAdminPIN", new String(timeAdminPIN));
            props.store(new FileOutputStream(pinPath.toFile()), "TSE properties for Weltladenkasse");
        } catch (IOException ex) {
            logger.error("Could not write TSE timeAdminPIN to file '~/.Weltladenkasse_tse'.");
            logger.error("Exception:", ex);
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
            logger.error("Exception:", ex);
            return showPINentryDialog("TimeAdmin", "PIN", 8);
        } catch (IOException ex) {
            logger.error("Could not read timeAdminPIN from file '~/.Weltladenkasse_tse'");
            logger.error("Exception:", ex);
            return showPINentryDialog("TimeAdmin", "PIN", 8);
        }
    }

    private String authenticateAs(String user, byte[] pin, boolean exitOnFatal) {
        if (null == pin) {
            pin = showPINentryDialog(user, "PIN", 8);
        }
        boolean passed = false;
        String message = "OK";
        try {
            AuthenticateUserResult res = tse.authenticateUser(user, pin);
            if (res.authenticationResult != AuthenticationResult.ok) {
                message = "Authentication error for user "+user+": "+res.authenticationResult.toString();
                logger.fatal("Fatal Error: {}", message);
                JOptionPane.showMessageDialog(this.mainWindow,
                    "ACHTUNG: Authentifizierungsfehler als User "+user+" bei der TSE!\n\n"+
                    "authenticationResult: "+res.authenticationResult.toString()+"\n\n"+
                    "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                    "wird die Kassensoftware jetzt beendet. Bitte Fehler beheben und\n"+
                    "erneut versuchen.",
                    "Fehlgeschlagene Authentifizierung bei der TSE", JOptionPane.ERROR_MESSAGE);
                // Exit application upon this fatal error
                disconnectFromTSE();
                System.exit(1);
            }
            passed = true;
        } catch (ErrorSigningSystemOperationDataFailed ex) {
            message = "Signing system operation data failed";
            logger.fatal("Fatal Error: {}", message);
            logger.fatal("Exception:", ex);
        } catch (ErrorRetrieveLogMessageFailed ex) {
            message = "Retrieve log message failed";
            logger.fatal("Fatal Error: {}", message);
            logger.fatal("Exception:", ex);
        } catch (ErrorStorageFailure ex) {
            message = "Storage failure";
            logger.fatal("Fatal Error: {}", message);
            logger.fatal("Exception:", ex);
        } catch (ErrorSecureElementDisabled ex) {
            message = "Secure element disabled";
            logger.fatal("Fatal Error: {}", message);
            logger.fatal("Exception:", ex);
        } catch (SEException ex) {
            message = "Unknown error during authenticateUser()";
            logger.fatal("Fatal Error: {}", message);
            logger.fatal("Exception:", ex);
        }
        if (!passed) {
            logger.info("Closing connection to TSE after this fatal error.");
            disconnectFromTSE();
            if (exitOnFatal) {
                JOptionPane.showMessageDialog(this.mainWindow,
                    "ACHTUNG: Es konnte sich nicht als User "+user+" an der TSE angemeldet werden!\n\n"+
                    "Fehler: "+message+".\n\n"+
                    "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                    "wird die Kassensoftware jetzt beendet. Bitte Fehler beheben und\n"+
                    "erneut versuchen.",
                    "Fehlgeschlagene Initialisierung der TSE", JOptionPane.ERROR_MESSAGE);
                // Exit application upon this fatal error
                System.exit(1);
            }
        }
        return message;
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
            logger.fatal("Exception:", ex);
        } catch (ErrorSigningSystemOperationDataFailed ex) {
            error = "Signing system operation data failed";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorUserNotAuthenticated ex) {
            error = "User ID not authenticated";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorUserNotAuthorized ex) {
            error = "User not authorized";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorRetrieveLogMessageFailed ex) {
            error = "Retrieve log message failed";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorStorageFailure ex) {
            error = "Storage failure";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorSecureElementDisabled ex) {
            error = "Secure element disabled";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (SEException ex) {
            error = "Unknown error during logOut()";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
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
            disconnectFromTSE();
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
            logger.fatal("Exception:", ex);
        } catch (ErrorRetrieveLogMessageFailed ex) {
            error = "Retrieve log message failed";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorStorageFailure ex) {
            error = "Storage failure";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorSecureElementDisabled ex) {
            error = "Secure Element disabled";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (SEException ex) {
            error = "Unknown error during initialize()";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
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
            loggedIn = false;
            disconnectFromTSE();
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
            logger.fatal("Exception:", ex);
        } catch (ErrorSecureElementDisabled ex) {
            error = "Secure Element disabled";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (SEException ex) {
            error = "Unknown error during getTimeSyncInterval()";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
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
            disconnectFromTSE();
            System.exit(1);
        }
    }

    private void updateTimeWithoutChecking() {
        // Check if update of time is necessary (and only then do it):
        long currentUtcTime = System.currentTimeMillis() / 1000;
        if (currentUtcTime <= nextSyncTime) {
            return; // do nothing
        }
        byte[] timeAdminPIN = readTimeAdminPINFromFile();
        boolean passed = false;
        String error = "";
        try {
            // System.out.println("\nBEFORE updateTime():");
            // printStatusValues();
            logger.info("Updating TSE's time...");
            authenticateAs("TimeAdmin", timeAdminPIN, true);
            tse.updateTime(currentUtcTime);
            logOutAs("TimeAdmin");
            logger.info("...done updating TSE's time");
            nextSyncTime = currentUtcTime + timeSyncInterval;
            // System.out.println("\nAFTER updateTime():");
            // printStatusValues();
            passed = true;
        } catch (ErrorUpdateTimeFailed ex) {
            error = "Update time failed";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorRetrieveLogMessageFailed ex) {
            error = "Retrieve log message failed";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorStorageFailure ex) {
            error = "Storage failure";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorSeApiNotInitialized ex) {
            error = "SE API not initialized";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorCertificateExpired ex) {
            error = "Certificate expired";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorSecureElementDisabled ex) {
            error = "Secure Element disabled";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorUserNotAuthorized ex) {
            error = "User not authorized";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorUserNotAuthenticated ex) {
            error = "User not authenticated";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (SEException ex) {
            error = "Unknown error during updateTime()";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
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
            disconnectFromTSE();
            System.exit(1);
        }
    }

    private void updateTime() {
        updateTimeWithoutChecking();
        // Re-check if time was actually set:
        try {
            if (tse.getLifeCycleState() == LCS.noTime) {
                logger.fatal("TSE time update failed!");
                JOptionPane.showMessageDialog(this.mainWindow,
                    "ACHTUNG: Die Aktualisierung der Zeit der TSE ist fehlgeschlagen!\n"+
                    "Ohne Zeitaktualisierung kann eine TSE nicht verwendet werden.\n"+
                    "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                    "Bitte beim nächsten Start der Kassensoftware erneut probieren.",
                    "Fehler beim Setzen der Zeit der TSE", JOptionPane.ERROR_MESSAGE);
                disconnectFromTSE();
                System.exit(1);
            }
        } catch (ErrorSECommunicationFailed ex) {
            logger.fatal("SE Communication failed!");
            logger.fatal("Exception:", ex);
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Die Kommunikation mit der TSE nach dem Setzen der Zeit ist fehlgeschlagen!\n"+
                "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.",
                "Fehler beim Setzen der Zeit der TSE", JOptionPane.ERROR_MESSAGE);
            disconnectFromTSE();
            System.exit(1);
        } catch (SEException ex) {
            logger.fatal("SE Communication failed!");
            logger.fatal("Exception:", ex);
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Unbekannter Fehler nach dem Setzen der Zeit der TSE!\n"+
                "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.",
                "Fehler beim Setzen der Zeit der TSE", JOptionPane.ERROR_MESSAGE);
            disconnectFromTSE();
            System.exit(1);
        }
        logger.info("TSE time successfully updated!");
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
            logger.fatal("Exception:", ex);
        } catch (SEException ex) {
            error = "Unknown error during exportSerialNumbers()";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
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
            if (loggedIn) {
                logOutAs("Admin");
                loggedIn = false;
            }
            disconnectFromTSE();
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
            /** Configure the TSE to use the ERS "WeltladenBonnKasse" with the given transaction key (serial number) */
            tse.mapERStoKey("WeltladenBonnKasse", serialNumber);
            System.out.println("\nAFTER mapERStoKey():");
            printStatusValues();
            passed = true;
        } catch (ErrorSigningSystemOperationDataFailed ex) {
            error = "Signing system operation data failed";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorRetrieveLogMessageFailed ex) {
            error = "Retrieve log message failed";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorStorageFailure ex) {
            error = "Storage failure";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorSeApiNotInitialized ex) {
            error = "SE API not initialized";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorCertificateExpired ex) {
            error = "Certificate expired";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorTimeNotSet ex) {
            error = "Time not set";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorSecureElementDisabled ex) {
            error = "Secure Element disabled";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorUserNotAuthorized ex) {
            error = "User not authorized";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorUserNotAuthenticated ex) {
            error = "User not authenticated";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorNoSuchKey ex) {
            error = "No such key";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorSECommunicationFailed ex) {
            error = "SE communication failed";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (ErrorERSalreadyMapped ex) {
            error = "ERS already mapped";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        } catch (SEException ex) {
            error = "Unknown error during mapERStoKey()";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        }
        if (!passed) {
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Die ClientID konnte nicht dem TSE-Schlüssel zugeordnet werden!\n\n"+
                "Fehler: "+error+".\n\n"+
                "Die TSE kann daher nicht verwendet werden. Da der Betrieb ohne TSE ILLEGAL ist,\n"+
                "wird die Kassensoftware jetzt beendet. Bitte Fehler beheben und\n"+
                "erneut versuchen.",
                "Fehlgeschlagene Client-ID-Zuordnung der TSE", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error
            logOutAs("Admin");
            loggedIn = false;
            disconnectFromTSE();
            System.exit(1);
        }
    }

    private String byteArrayToByteString(byte[] byteArray) {
        String res = "";
        for (byte b : byteArray) {
            res += b + " ";
        }
        return res.substring(0, res.length() - 1); // omit last empty string
    }

    private String byteArrayToIntString(byte[] byteArray) {
        String res = "";
        for (byte b : byteArray) {
            res += (int)b + " ";
        }
        return res.substring(0, res.length() - 1); // omit last empty string
    }

    private String byteArrayToCharString(byte[] byteArray) {
        String res = "";
        for (byte b : byteArray) {
            res += (char)b + " ";
        }
        return res.substring(0, res.length() - 1); // omit last empty string
    }

    private String byteArrayToBase64String(byte[] byteArray) {
        return Base64.getEncoder().encodeToString(byteArray);
    }

    private void writeTestTransaction() {
        try {
            long n = tse.getCurrentNumberOfClients();
            logger.debug("Number of clients: {}", n);
            n = tse.getCurrentNumberOfTransactions();
            logger.debug("Number of transactions: {}", n);

            /** Start a new transaction for the ERS (cash register) "WeltladenBonnKasse" */
            StartTransactionResult result = tse.startTransaction("WeltladenBonnKasse", "processData".getBytes(), "whateverProcessType", "additionalData".getBytes());
            logger.debug("StartTransaction: transactionNumber: {}", result.transactionNumber);
            logger.debug("StartTransaction: signatureCounter: {}", result.signatureCounter);
            logger.debug("StartTransaction: logTime (unix): {}", result.logTime);
            logger.debug("StartTransaction: logTime (cal): {}", unixTimeToCalTime(result.logTime));
            // logger.debug("StartTransaction: serialNumber (Hex): {}", encodeByteArrayAsHexString(result.serialNumber));
            logger.debug("StartTransaction: signatureValue (Base64): {}", byteArrayToBase64String(result.signatureValue));

            /** again some status information */
            n = tse.getCurrentNumberOfTransactions();
            logger.debug("Number of open transactions: {}", n);

            // /** Update the transaction */
            // UpdateTransactionResult updRes = tse.updateTransaction("WeltladenBonnKasse", result.transactionNumber, new byte[TSE.MAX_SIZE_TRANSPORT_LAYER-100], "anyProcessTypeString");
            // logger.debug("UpdateTransaction: signatureCounter: {}", updRes.signatureCounter);
            // logger.debug("UpdateTransaction: logTime: {}", updRes.logTime);
            // // logger.debug("UpdateTransaction: serialNumber (Hex): {}", encodeByteArrayAsHexString(updRes.serialNumber));
            // logger.debug("UpdateTransaction: signatureValue (Base64): {}", byteArrayToBase64String(updRes.signatureValue));

            // /** again some status information */
            // n = tse.getCurrentNumberOfTransactions();
            // logger.debug("Number of open transactions: {}", n);

            /** receive list of all pending transaction numbers */
            long[] openTransactions = tse.getOpenTransactions();
            logger.debug("Open transactions: {}", openTransactions);

            /** Finish the transaction */
            FinishTransactionResult finRes = tse.finishTransaction("WeltladenBonnKasse", result.transactionNumber, "lastData".getBytes(), "maybeYetAnotherProcessType", null);
            logger.debug("FinishTransaction: signatureCounter: {}", finRes.signatureCounter);
            logger.debug("FinishTransaction: logTime (unix): {}", finRes.logTime);
            logger.debug("FinishTransaction: logTime (cal): {}", unixTimeToCalTime(finRes.logTime));
            logger.debug("FinishTransaction: logTime (cal): {}", unixTimeToCalTime(finRes.logTime, bc.dateFormatJava));
            // logger.debug("FinishTransaction: serialNumber (Hex): {}", encodeByteArrayAsHexString(finRes.serialNumber));
            logger.debug("FinishTransaction: signatureValue (Base64): {}", byteArrayToBase64String(finRes.signatureValue));

            /** again some status information - should be 0 again*/
            n = tse.getCurrentNumberOfTransactions();
            logger.debug("Number of open transactions: {}", n);

            /** should be empty */
            openTransactions = tse.getOpenTransactions();
            logger.debug("Open transactions: {}", openTransactions);

            byte[] tx = getTransaction(result.transactionNumber);

            System.out.println();
            System.out.println("::: Base64 :::");
            System.out.println(byteArrayToBase64String(tx));
            System.out.println();
            System.out.println("::: ASN1 :::");
            System.out.println(decodeASN1ByteArray(tx));
            System.out.println();
            System.out.println("::: Hex :::");
            System.out.println(encodeByteArrayAsHexString(tx));

            try {
                FileOutputStream fout = new FileOutputStream(new File("/tmp/tse_export1.tar"));
                fout.write(tx);
                fout.close();
            } catch (Exception ex) {
                logger.error(ex);
            }
            exportPartialTransactionDataByTXNumber("/tmp/tse_export2.tar", result.transactionNumber, result.transactionNumber, null);
        } catch (ErrorSeApiNotInitialized ex) {
            logger.fatal("SE API not initialized");
            logger.fatal("Exception:", ex);
        } catch (ErrorSecureElementDisabled ex) {
            logger.fatal("Secure Element disabled");
            logger.fatal("Exception:", ex);
        } catch (ErrorStartTransactionFailed ex) {
            logger.fatal("Start transaction failed");
            logger.fatal("Exception:", ex);
        } catch (ErrorRetrieveLogMessageFailed ex) {
            logger.fatal("Retrieve log message failed");
            logger.fatal("Exception:", ex);
        } catch (ErrorStorageFailure ex) {
            logger.fatal("Storage failure");
            logger.fatal("Exception:", ex);
        } catch (ErrorTimeNotSet ex) {
            logger.fatal("Time not set");
            logger.fatal("Exception:", ex);
        } catch (ErrorCertificateExpired ex) {
            logger.fatal("Certificate expired");
            logger.fatal("Exception:", ex);
        } catch (ErrorUpdateTransactionFailed ex) {
            logger.fatal("Update transaction failed");
            logger.fatal("Exception:", ex);
        } catch (ErrorNoTransaction ex) {
            logger.fatal("No transaction");
            logger.fatal("Exception:", ex);
        } catch (ErrorFinishTransactionFailed ex) {
            logger.fatal("Finish transaction failed");
            logger.fatal("Exception:", ex);
        } catch (SEException ex) {
            logger.fatal("Unknown error during writeTestTransaction()");
            logger.fatal("Exception:", ex);
        }
    }

    private byte[] getTransaction(Long txNumber) {
        try {
            byte[] exportData = tse.exportData(null, txNumber, null, null, null, null, null);
            return exportData;
        } catch (IOException ex) {
            logger.fatal("IO exception during exportTransactionData()");
            logger.fatal("Exception:", ex);
        } catch (SEException ex) {
            logger.fatal("SE exception during exportTransactionData()");
            logger.fatal("Exception:", ex);
        }
        return null;
    }

    private String exportTransactionData(String filename, Long startTXNumber, Long endTXNumber, Long startDate, Long endDate, Long maxRecords) {
        /** Export transaction logs */
        try {
            // alternative 2 - provide file name
            tse.exportData(null, null, startTXNumber, endTXNumber, startDate, endDate, maxRecords, filename);
            return "OK";
        } catch (FileNotFoundException ex) {
            logger.fatal("File not found exception during exportTransactionData()");            
            logger.fatal("Exception:", ex);
            return "FileNotFoundException";
        } catch (IOException ex) {
            logger.fatal("IO exception during exportTransactionData()");
            logger.fatal("Exception:", ex);
            return "IOException";
        } catch (ErrorTooManyRecords ex) {
            logger.error("Error: Too many records during exportTransactionData(). Maximum number of records is too small.");
            logger.error("Exception:", ex);
            return "ErrorTooManyRecords";
        } catch (SEException ex) {
            logger.fatal("SE exception during exportTransactionData()");
            logger.fatal("Exception:", ex);
            return "SEException";
        }
    }

    public String exportFullTransactionData(String filename) {
        return exportTransactionData(filename, null, null, null, null, null);
    }

    public String exportPartialTransactionDataByTXNumber(String filename, Long startTXNumber, Long endTXNumber, Long maxRecords) {
        return exportTransactionData(filename, startTXNumber, endTXNumber, null, null, maxRecords);
    }

    public String exportPartialTransactionDataByDate(String filename, Long startDate, Long endDate, Long maxRecords) {
        return exportTransactionData(filename, null, null, startDate, endDate, maxRecords);
    }

    public String exportPartialTransactionDataBySigCounter(String filename, Long lastExcludedSignatureCounter, Long maxRecords) {
        /** Export transaction logs */
        try {
            OutputStream stream = new FileOutputStream(new File(filename));
            byte[] serial = getSerialNumber();
            tse.exportMoreData(serial, lastExcludedSignatureCounter, maxRecords, stream);
            stream.close();
            return "OK";
        } catch (FileNotFoundException ex) {
            logger.fatal("File not found exception during exportPartialTransactionDataBySigCounter()");            
            logger.fatal("Exception:", ex);
            return "FileNotFoundException";
        } catch (IOException ex) {
            logger.fatal("IO exception during exportPartialTransactionDataBySigCounter()");
            logger.fatal("Exception:", ex);
            return "IOException";
        } catch (SEException ex) {
            logger.fatal("SE exception during exportPartialTransactionDataBySigCounter()");
            logger.fatal("Exception:", ex);
            return "SEException";
        }
    }

    public Long discoverHighestExportedSigCounterFromExport(String filename) {
        Long highestSig = (long)-1;
        try {
            Process p = Runtime.getRuntime().exec("tar -tf "+filename);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            highestSig = (long)0;
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                Matcher matcher = Pattern.compile("Sig-([0-9]+)").matcher(s);
                if (matcher.find()) {
                    String match = matcher.group(1);
                    Long sig = Long.parseLong(match);
                    if (sig > highestSig) highestSig = sig;
                }
            }
        } catch (IOException ex) {
            logger.error("Discovering the last exported sig counter from export did not work.");
            logger.error("Exception:", ex);
        }
        return highestSig;
    }

    public String deletePartialTransactionDataBySigCounter(Long lastIncludedSignatureCounter) {
        /** Delete some transaction logs */
        try {
            byte[] serial = getSerialNumber();
            if (!loggedIn) {
                authenticateAs("Admin", null, true);
                loggedIn = true;
            }
            tse.deleteStoredDataUpTo(serial, lastIncludedSignatureCounter);
            if (loggedIn) {
                logOutAs("Admin");
                loggedIn = false;
            }
            return "OK";
        } catch (ErrorNoSuchKey ex) {
            logger.fatal("No such key error during deletePartialTransactionDataBySigCounter()");
            logger.fatal("Exception:", ex);
            return "ErrorNoSuchKey";
        } catch (ErrorIdNotFound ex) {
            logger.fatal("ID not found error during deletePartialTransactionDataBySigCounter()");
            logger.fatal("Exception:", ex);
            return "ErrorIdNotFound";
        } catch (ErrorStreamWrite ex) {
            logger.fatal("Stream write error during deletePartialTransactionDataBySigCounter()");
            logger.fatal("Exception:", ex);
            return "ErrorStreamWrite";
        } catch (SEException ex) {
            logger.fatal("SE exception during deletePartialTransactionDataBySigCounter()");
            logger.fatal("Exception:", ex);
            return "SEException";
        }
    }

    private String deleteFullTransactionData() {
        /** Delete all transaction logs */
        try {
            if (!loggedIn) {
                authenticateAs("Admin", null, true);
                loggedIn = true;
            }
            tse.deleteStoredData();
            if (loggedIn) {
                logOutAs("Admin");
                loggedIn = false;
            }
            return "OK";
        } catch (ErrorUnexportedStoredData ex) {
            logger.fatal("Error: There is still unexported stored data, so cannot deleteFullTransactionData()");
            logger.fatal("Exception:", ex);
            return "ErrorUnexportedStoredData";
        } catch (ErrorSeApiNotInitialized ex) {
            logger.fatal("SE API not initialized error during deleteFullTransactionData()");
            logger.fatal("Exception:", ex);
            return "ErrorSeApiNotInitialized";
        } catch (ErrorUserNotAuthorized ex) {
            logger.fatal("User not authorized error during deleteFullTransactionData()");
            logger.fatal("Exception:", ex);
            return "ErrorUserNotAuthorized";
        } catch (ErrorUserNotAuthenticated ex) {
            logger.fatal("User not authenticated error during deleteFullTransactionData()");
            logger.fatal("Exception:", ex);
            return "ErrorUserNotAuthenticated";
        } catch (SEException ex) {
            logger.fatal("SE exception during deleteFullTransactionData()");
            logger.fatal("Exception:", ex);
            return "SEException";
        }
    }

}