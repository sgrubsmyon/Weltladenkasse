package org.weltladen_bonn.pos.kasse;

import org.weltladen_bonn.pos.WindowContent;
import org.weltladen_bonn.pos.LongOperationIndicatorDialog;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
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
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.Comparator;
import java.util.Base64;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding
// import java.math.BigInteger;

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
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.util.ASN1Dump;
import org.bouncycastle.asn1.bsi.BSIObjectIdentifiers;
import java.io.ByteArrayInputStream;

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

/**
 * This class is for interaction with a TSE (Technische Sicherheitseinrichtung),
 * based on BSI TR-03151 SE API. In this case, TR-03151 is implemented by
 * a device from cryptovision/D-Trust/Bundesdruckerei, but it could be
 * a different vendor as well (e.g. EPSON).
 */
public class WeltladenTSE extends WindowContent {
    private static final Logger logger = LogManager.getLogger(WeltladenTSE.class);
    
    private String defaultProcessType = "Kassenbeleg-V1";

    protected class TSETransaction {
        public Integer rechnungsNr = null; // for connecting TSE data to SQL table 'verkauf'
        public Long txNumber = null; // of the StartTransaction operation
        public Long startTimeUnix = null; // of the StartTransaction operation
        public String startTimeString = null; // of the StartTransaction operation
        public Long endTimeUnix = null; // of the FinishTransaction operation
        public String endTimeString = null; // of the FinishTransaction operation
        public String processType = null; // of the FinishTransaction operation
        public String processData = null; // of the FinishTransaction operation
        public Long sigCounter = null; // of the FinishTransaction operation
        public String signatureBase64 = null; // of the FinishTransaction operation
        public String tseError = null; // error message in case of error
    }

    private TSE tse = null;
    private TSETransaction tx = new TSETransaction();
    private boolean tseInUse = true;
    private boolean loggedIn = false;
    private Path pinPath = FileSystems.getDefault().getPath(System.getProperty("user.home"), ".Weltladenkasse_tse");
    public static String dateFormatDSFinVK = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"; // YYYY-MM-DDThh:mm:ss.fffZ, see https://www.bzst.de/DE/Unternehmen/Aussenpruefungen/DigitaleSchnittstelleFinV/digitaleschnittstellefinv_node.html
    public static String dateFormatQuittung = "yyyy-MM-dd'T'HH:mm:ss.SSS"; // shorter, but also ISO 8601, so that it fits on one line of the Quittung

    private static long nextSyncTime = 0;
    private static int timeSyncInterval = 0;

    private LinkedHashMap<String, String> statusValues;

    /**
     *    The constructor.
     *
     */
    public WeltladenTSE(MariaDbPoolDataSource pool, MainWindow mw) {
        super(pool, mw);
        connectToTSE();
        if (tseInUse) {
            LongOperationIndicatorDialog dialog = new LongOperationIndicatorDialog(
                new JLabel("TSE wird initialisiert..."),
                null
            );
            
            updateTSEStatusValues(); // so that status values can be printed from inside checkInitializationStatus()
            checkInitializationStatus();
            updateTSEStatusValues(); // might have changes inside checkInitializationStatus()
            printAllStatusValues();
            // System.out.println("\n\n*** WRITING FIRST TRANSACTION TO TSE ***");
            // System.out.println("\n --- Status before: \n");
            // printStatusValues();
            // for (int i = 0; i < 50; i++) {
            // writeTestTransaction();
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
                /* Cancel a potentially unfinished transaction: */
                if (tx.txNumber != null) {
                    cancelTransaction();
                }
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
            // If currently configured client ID (Z_KASSE_ID) is not yet mapped to key:
            Vector<String> clientIDs = decodeClientIDsFromERSMappings(tse.getERSMappings());
            logger.debug("Mapped clientIDs: {}", clientIDs);
            if (!clientIDs.contains(bc.Z_KASSE_ID)) {
                logger.debug("01 ERS Mappings do not contain configured Z_KASSE_ID '{}', need to insert mapping!", bc.Z_KASSE_ID);
                if (!loggedIn) {
                    logger.debug("02 Before authenticateAs");
                    JOptionPane.showMessageDialog(this.mainWindow,
                        "Noch keine Zuordnung der Kassen-ID (Z_KASSE_ID in config.properties)\n"+
                        "zum Schlüssel der TSE vorhanden!\n"+
                        "Bitte jetzt das TSE-Admin-Passwort eingeben, damit die Zuordnung\n"+
                        "angelegt werden kann.",
                        "Fehlende Zuordnung in der TSE", JOptionPane.INFORMATION_MESSAGE);
                    authenticateAs("Admin", adminPIN, true);
                    logger.debug("03 After authenticateAs");
                    loggedIn = true;
                }
                logger.debug("04 Before getSerialNumber");
                byte[] serialNumber = getSerialNumber();
                logger.debug("05 After getSerialNumber");
                mapClientIDToKey(serialNumber);
                logger.debug("06 After mapClientIDToKey");
                clientIDs = decodeClientIDsFromERSMappings(tse.getERSMappings());
                logger.debug("Mapped clientIDs now: {}", clientIDs);
                // Re-check if client ID is still unmapped to key:
                if (!clientIDs.contains(bc.Z_KASSE_ID)) {
                    logger.fatal("Mapping of client ID to TSE key failed!");
                    JOptionPane.showMessageDialog(this.mainWindow,
                        "ACHTUNG: Die Zuordnung der Kassen-ID (Z_KASSE_ID in config.properties)\n"+
                        "zum Schlüssel der TSE ist fehlgeschlagen!\n"+
                        "Ohne Kassen-ID-Zuordnung kann eine TSE nicht verwendet werden.\n"+
                        "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                        "Bitte beim nächsten Start der Kassensoftware erneut probieren.",
                        "Fehler beim Anlegen einer Zuordnung in der TSE", JOptionPane.ERROR_MESSAGE);
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

    public static String byteArrayToByteString(byte[] byteArray) {
        String res = "[";
        for (byte b : byteArray) {
            res += b + ", ";
        }
        res = res.substring(0, res.length() - 2); // omit last separator
        res += "]";
        return res;
    }

    public static String byteArrayToIntString(byte[] byteArray) {
        String res = "[";
        for (byte b : byteArray) {
            res += (int)b + " ";
        }
        res = res.substring(0, res.length() - 2); // omit last separator
        res += "]";
        return res;
    }

    // https://stackoverflow.com/questions/7619058/convert-a-byte-array-to-integer-in-java-and-vice-versa
    public static String byteArrayToSingleIntString(byte[] byteArray) {
        ByteBuffer wrapped = ByteBuffer.wrap(byteArray); // big-endian by default
        // logger.debug("byteArray bytes: {}", byteArrayToByteString(byteArray));
        // logger.debug("byteArray length: {}", byteArray.length);
        // logger.debug("first 4 bytes: {}", wrapped.getInt(0));
        // logger.debug("second 4 bytes: {}", wrapped.getInt(4));
        // logger.debug("third 4 bytes: {}", wrapped.getInt(8));
        // logger.debug("fourth 4 bytes: {}", wrapped.getInt(12));
        // logger.debug("first 8 bytes: {}", wrapped.getLong(0));
        // logger.debug("second 8 bytes: {}", wrapped.getLong(8));
        // logger.debug("Hex converted to BigInteger: {}", new BigInteger(byteArrayToHexString(byteArray), 16));
        String res = "";
        int pos = 0;
        while (pos < byteArray.length) {
            res += wrapped.getLong(pos); // read 8 bytes from the byte array
            pos += 8;
        }
        return res;
    }

    public static String byteArrayToCharString(byte[] byteArray) {
        String res = "";
        for (byte b : byteArray) {
            res += (char)b;
        }
        return res.substring(0, res.length());
    }

    public static String byteArrayToHexString(byte[] data) {
        return new String(Hex.encode(data));
    }

    public static byte[] hexStringToByteArray(String data) {
        return Hex.decode(data);
    }

    public static String byteArrayToBase64String(byte[] byteArray) {
        return Base64.getEncoder().encodeToString(byteArray);
    }

    public static String byteArrayToASN1String(byte[] data) {
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

    public static String byteArrayToASN1ObjectIdentifierString(byte[] data) {
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

    public static Vector<String> decodeClientIDsFromERSMappings(byte[] data) {
        Vector<String> result = new Vector<String>();
        ASN1Sequence mappings = ASN1Sequence.getInstance(data);
        for (int i = 0; i < mappings.size(); i++) {
            ASN1Sequence mapping = ASN1Sequence.getInstance(mappings.getObjectAt(i));
            // clientID is always the first element:
            ASN1OctetString octstr = ASN1OctetString.getInstance(mapping.getObjectAt(0));
            String clientID = byteArrayToCharString(octstr.getOctets());
            result.add(clientID);
        }
        return result;
    }

    public static String unixTimeToCalTime(long unixTime) {
        java.util.Date date = new java.util.Date(unixTime * 1000);
        return new SimpleDateFormat(dateFormatDSFinVK).format(date);
    }

    public static String unixTimeToCalTime(long unixTime, String dateFormat) {
        java.util.Date date = new java.util.Date(unixTime * 1000);
        return new SimpleDateFormat(dateFormat).format(date);
    }

    public String getSignatureAlgorithm() {
        String oid = "";
        try {
            oid = byteArrayToASN1ObjectIdentifierString(tse.getSignatureAlgorithm());
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

    public void updateTSEStatusValues() {
        statusValues = new LinkedHashMap<String, String>();
        // Abfrage der eindeutigen Identifikation einer jeden D-Trust TSE
        statusValues.put("Eindeutige D-Trust-ID (Hex)", byteArrayToHexString(tse.getUniqueId()));
        try {
            // Abfrage der BSI-Zertifizierungsnummer, Beispiel: "BSI-K-TR-0374-2020"
            statusValues.put("BSI-Zertifizierungsnummer", tse.getCertificationId());
            // Abfrage des Firmware Identifikations-Strings
            statusValues.put("Firmware-Version", tse.getFirmwareId());
            // Abfrage der Größe des gesamten Speichers für abgesicherte Anwendungs- und Protokolldaten
            statusValues.put("Gesamte Speichergröße", tse.getTotalLogMemory() / 1024 / 1024 + " MB");
            // Abfrage der Größe des freien Speichers für abgesicherte Anwendungs- und Protokolldaten
            statusValues.put("Verfügbare Speichergröße", tse.getAvailableLogMemory() / 1024 / 1024 + " MB");
            // Verschleißabfrage für den Speicher der abgesicherte Anwendungs- und Protokolldaten
            statusValues.put("Verschleiß des Speichers", String.valueOf(tse.getWearIndicator()));
            // Status-Abfrage des Lebenszyklus
            LCS lcs = tse.getLifeCycleState();
            statusValues.put("Lebenszyklus", lcs.toString());
            if (lcs != LCS.notInitialized) {
                // Erklärung zu Seriennummern auf https://tse-support.cryptovision.com/confluence/display/TDI/TSE-Seriennummer:
                // "Eine Seriennummer ist der SHA-256 Hashwert des öffentlichen Schlüssels, der zu dem Schlüsselpaar gehört,
                //  dessen privater Schlüssel zum Signieren der Protokollmeldungen verwendet wird."
                byte[] serialNumberData = tse.exportSerialNumbers(); // (Rückgabe aller Signaturschlüssel-Seriennummern, sowie deren Verwendung)
		        byte[] serialNumber = Arrays.copyOfRange(serialNumberData, 6, 6+32);
                // Abfrage der Transaktionsnummer der letzten Transaktion
                statusValues.put("Transaktionsnummer", String.valueOf(tse.getTransactionCounter()));
                // Abfrage des Signatur-Zählers der letzten Signatur
                statusValues.put("Signatur-Zähler", String.valueOf(tse.getSignatureCounter(serialNumber)));
                // Erste und auch einzige Signaturschlüssel-Seriennummer
                statusValues.put("Seriennummer der TSE (Hex)", byteArrayToHexString(serialNumber));
                // Erste und auch einzige Signaturschlüssel-Seriennummer
                statusValues.put("Seriennummer der TSE (Base64)", byteArrayToBase64String(serialNumber));
                // Alle Signaturschlüssel-Seriennummern
                statusValues.put("Seriennummern aller Schlüssel (ASN.1)", byteArrayToASN1String(serialNumberData));
                // Rückgabe eines öffentlichen Schlüssels
                statusValues.put("Öffentlicher Schlüssel (Hex)", byteArrayToHexString(tse.exportPublicKey(serialNumber)));
                // Rückgabe eines öffentlichen Schlüssels
                statusValues.put("Öffentlicher Schlüssel (Base64)", byteArrayToBase64String(tse.exportPublicKey(serialNumber)));
                long expirationTimestamp = tse.getCertificateExpirationDate(serialNumber);
                // Abfrage des Ablaufdatums eines Zertifikats
                statusValues.put("Ablaufdatum des Zertifikats", unixTimeToCalTime(expirationTimestamp, bc.dateFormatJava));
                // Abfrage des Ablaufdatums eines Zertifikats
                statusValues.put("Ablaufdatum des Zertifikats (Unix-Time)", String.valueOf(expirationTimestamp));
                // aus FirstBoot.java übernommen:
                statusValues.put("Zeitformat", tse.getTimeSyncVariant().toString());
                // Abfrage des Signatur-Algorithmus zur Absicherung von Anwendungs- und Protokolldaten
                statusValues.put("Signatur-Algorithmus", getSignatureAlgorithm());
                // Abfrage des Signatur-Algorithmus zur Absicherung von Anwendungs- und Protokolldaten
                statusValues.put("Signatur-Algorithmus (ASN.1)", byteArrayToASN1String(tse.getSignatureAlgorithm()));
                // Abfrage des Signatur-Algorithmus zur Absicherung von Anwendungs- und Protokolldaten
                // 0.4.0.127.0.7.1.1.4.1.3 stands for ecdsa-plain-SHA256 (https://github.com/bcgit/bc-java/blob/master/core/src/main/java/org/bouncycastle/asn1/bsi/BSIObjectIdentifiers.java, http://www.bouncycastle.org/docs/docs1.5on/org/bouncycastle/asn1/bsi/BSIObjectIdentifiers.html#ecdsa_plain_SHA256)
                statusValues.put("TSE_OID des Signatur-Algorithmus", byteArrayToASN1ObjectIdentifierString(tse.getSignatureAlgorithm()));
                // Abfrage aller Zuordnungen von Identifikationsnummern zu Signaturschlüsseln
                statusValues.put("Zuordnungen von Kassen-IDs zu Schlüsseln (ASN.1)", byteArrayToASN1String(tse.getERSMappings()));
                // Abfrage der maximal gleichzeitig unterstützten Kassen-Terminals
                statusValues.put("Maximale Anzahl Kassen-Terminals", String.valueOf(tse.getMaxNumberOfClients()));
                // Abfrage der derzeit in Benutzung befindlichen Kassen-Terminals
                statusValues.put("Aktuelle Anzahl Kassen-Terminals", String.valueOf(tse.getCurrentNumberOfClients()));
                // Abfrage der maximal gleichzeitig offenen Transaktionen
                statusValues.put("Maximale Zahl offener Transaktionen", String.valueOf(tse.getMaxNumberOfTransactions()));
                // Abfrage der Anzahl der derzeit offenen Transaktionen
                statusValues.put("Aktuelle Zahl offener Transaktionen", String.valueOf(tse.getCurrentNumberOfTransactions()));
                // aus FirstBoot.java übernommen
                statusValues.put("Unterstützte Transaktionsaktualisierungsvarianten", tse.getSupportedTransactionUpdateVariants().toString());
                byte[] certificate = TSEUntar.exportCertificate(tse, serialNumber);
                statusValues.put("TSE-Zertifikat (Hex)", byteArrayToHexString(certificate));
                statusValues.put("TSE-Zertifikat (Base64)", byteArrayToBase64String(certificate));
                // Lesen des letzten gespeicherten und abgesicherten Anwendungs- und Protokolldatensatzes
                statusValues.put("Letzte Protokolldaten (ASN.1)", byteArrayToASN1String(tse.readLogMessage()));
            }
        } catch (SEException ex) {
            logger.error("Error at reading of TSE status values");
            logger.error("Exception:", ex);
        }
    }

    public LinkedHashMap<String, String> getTSEStatusValues() {
        return statusValues;
    }

    // public LinkedHashMap<String, String> retrieveTSEStatusValues(Vector<String> interestingValues) {
    //     LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
    //     for (String k : statusValues.keySet()) {
    //         if (interestingValues.contains("Eindeutige D-Trust-ID (Hex)")) {
    //             values.put(k, statusValues.get(k));
    //         }
    //     }
    //     return values;
    // }

    private void printStatusValues() {
        String[] interestingValuesArray = {
            // "Eindeutige D-Trust-ID (Hex)", "Firmware-Version", "BSI-Zertifizierungsnummer",
            "Gesamte Speichergröße", "Verfügbare Speichergröße", "Verschleiß des Speichers",
            "Lebenszyklus", "Transaktionsnummer", "Signatur-Zähler",
            // "Seriennummer der TSE (Hex)", "Öffentlicher Schlüssel (Hex)",
            // "Ablaufdatum des Zertifikats", "Signatur-Algorithmus (ASN.1)",
            "Zuordnungen von Kassen-IDs zu Schlüsseln (ASN.1)"
        };
        // Vector<String> interestingValues = new Vector<String>(Arrays.asList(interestingValuesArray));
        for (String s : interestingValuesArray) {
            System.out.println(s + ": " + statusValues.get(s));
        }
    }

     private void printAllStatusValues() {
        for (String k : statusValues.keySet()) {
            System.out.println(k + ": " + statusValues.get(k));
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
            /** Configure the TSE to use the ERS bc.Z_KASSE_ID with the given transaction key (serial number) */
            tse.mapERStoKey(bc.Z_KASSE_ID, serialNumber);
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

    private void writeTestTransaction() {
        try {
            long n = tse.getCurrentNumberOfClients();
            logger.debug("Number of clients: {}", n);
            n = tse.getCurrentNumberOfTransactions();
            logger.debug("Number of transactions: {}", n);

            /** Start a new transaction for the ERS (cash register) */
            StartTransactionResult result = tse.startTransaction(bc.Z_KASSE_ID, "processData".getBytes(), "whateverProcessType", "additionalData".getBytes());
            logger.debug("StartTransaction: transactionNumber: {}", result.transactionNumber);
            logger.debug("StartTransaction: signatureCounter: {}", result.signatureCounter);
            logger.debug("StartTransaction: logTime (unix): {}", result.logTime);
            logger.debug("StartTransaction: logTime (cal): {}", unixTimeToCalTime(result.logTime));
            // logger.debug("StartTransaction: serialNumber (Hex): {}", byteArrayToHexString(result.serialNumber));
            logger.debug("StartTransaction: signatureValue (Base64): {}", byteArrayToBase64String(result.signatureValue));
            logger.debug("StartTransaction: signatureValue (Hex): {}", byteArrayToHexString(result.signatureValue));

            /** again some status information */
            n = tse.getCurrentNumberOfTransactions();
            logger.debug("Number of open transactions: {}", n);

            // /** Update the transaction */
            // UpdateTransactionResult updRes = tse.updateTransaction(bc.Z_KASSE_ID, result.transactionNumber, new byte[TSE.MAX_SIZE_TRANSPORT_LAYER-100], "anyProcessTypeString");
            // logger.debug("UpdateTransaction: signatureCounter: {}", updRes.signatureCounter);
            // logger.debug("UpdateTransaction: logTime: {}", updRes.logTime);
            // // logger.debug("UpdateTransaction: serialNumber (Hex): {}", byteArrayToHexString(updRes.serialNumber));
            // logger.debug("UpdateTransaction: signatureValue (Base64): {}", byteArrayToBase64String(updRes.signatureValue));

            // /** again some status information */
            // n = tse.getCurrentNumberOfTransactions();
            // logger.debug("Number of open transactions: {}", n);

            /** receive list of all pending transaction numbers */
            long[] openTransactions = tse.getOpenTransactions();
            logger.debug("Open transactions: {}", openTransactions);

            /** Finish the transaction */
            FinishTransactionResult finRes = tse.finishTransaction(bc.Z_KASSE_ID, result.transactionNumber, "lastData".getBytes(), "maybeYetAnotherProcessType", null);
            logger.debug("FinishTransaction: signatureCounter: {}", finRes.signatureCounter);
            logger.debug("FinishTransaction: logTime (unix): {}", finRes.logTime);
            logger.debug("FinishTransaction: logTime (cal): {}", unixTimeToCalTime(finRes.logTime));
            logger.debug("FinishTransaction: logTime (cal): {}", unixTimeToCalTime(finRes.logTime, bc.dateFormatJava));
            // logger.debug("FinishTransaction: serialNumber (Hex): {}", byteArrayToHexString(finRes.serialNumber));
            logger.debug("FinishTransaction: signatureValue (Base64): {}", byteArrayToBase64String(finRes.signatureValue));
            logger.debug("FinishTransaction: signatureValue (Hex): {}", byteArrayToHexString(finRes.signatureValue));

            /** again some status information - should be 0 again */
            n = tse.getCurrentNumberOfTransactions();
            logger.debug("Number of open transactions: {}", n);

            /** should be empty */
            openTransactions = tse.getOpenTransactions();
            logger.debug("Open transactions: {}", openTransactions);

            byte[] transx = getTransaction(result.transactionNumber);
            System.out.println(TSEUntar.extractStartTransactionAsASN1(transx));
            System.out.println(TSEUntar.extractFinishTransactionAsASN1(transx));
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

    public Integer getSignatureCounter() {
        Integer sigCount = null;
        try {
            byte[] serial = getSerialNumber();
            long sigCountLong = tse.getSignatureCounter(serial);
            sigCount = Math.toIntExact(sigCountLong);
        } catch (SEException ex) {
            logger.fatal("SE exception during getSignatureCounter()");
            logger.fatal("Exception:", ex);
        } catch (ArithmeticException ex) {
            logger.error("Current signatureCounter is too large to store as integer in DB!!!");
            logger.error("Exception:", ex);
        }
        return sigCount;
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

    /** Start a new transaction on the TSE */
    public void startTransaction() {
        String error = "";
        boolean passed = false;
        /* Cancel a potentially unfinished transaction: */
        if (tx.txNumber != null) {
            cancelTransaction();
        }
        try {
            /* Zitat DSFinV-K v2.2 (Anhang I, S. 115): (https://www.bzst.de/DE/Unternehmen/Aussenpruefungen/DigitaleSchnittstelleFinV/digitaleschnittstellefinv_node.html)
                "Für alle Vorgangstypen gilt, dass processType und processData für die StartTransaction-Operation immer leer sind."
                "StartTransaction wird unmittelbar mit Beginn eines Vorgangs an der Kasse aufgerufen."
                "Die UpdateTransaction-Operation wird beim processType Kassenbeleg nicht verwendet, da die processData sich erst
                bei FinishTransaction ändern."
            */
            tx = new TSETransaction();
            StartTransactionResult result = tse.startTransaction(bc.Z_KASSE_ID, null, null, null);
            long n = tse.getCurrentNumberOfTransactions();
            tx.txNumber = result.transactionNumber;
            tx.startTimeUnix = result.logTime;
            tx.startTimeString = unixTimeToCalTime(result.logTime);
            logger.debug("Number of open transactions: {}", tse.getCurrentNumberOfTransactions());
            logger.debug("New transaction:");
            logger.debug("TX number: {}", tx.txNumber);
            logger.debug("TX start time: {}", tx.startTimeString);
            passed = true;
        } catch (ErrorStartTransactionFailed ex) {
            error = "Start transaction failed";
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
        } catch (ErrorTimeNotSet ex) {
            error = "Time not set";
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
        } catch (SEException ex) {
            error = "Unknown error during startTransaction()";
            logger.fatal("Fatal Error: {}", error);
            logger.fatal("Exception:", ex);
        }
        if (!passed) {
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Es konnte keine TSE-Transaktion gestartet werden!\n\n"+
                "Fehler: "+error+".\n\n"+
                "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                "Bitte Fehler beheben und erneut versuchen.",
                "Fehlgeschlagener Transaktionsstart der TSE", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error
            disconnectFromTSE();
            System.exit(1);
        }
    };

    private String sendFinishTransaction(String processData, Integer rechnungsNr) {
        String message = "";
        try {
            if (tx.txNumber != null) {
                String processType = defaultProcessType;
                FinishTransactionResult result = tse.finishTransaction(bc.Z_KASSE_ID, tx.txNumber, processData.getBytes(), processType, null);
                tx.rechnungsNr = rechnungsNr;
                tx.endTimeUnix = result.logTime;
                tx.endTimeString = unixTimeToCalTime(result.logTime);
                tx.processType = processType;
                tx.processData = processData;
                tx.sigCounter = result.signatureCounter;
                tx.signatureBase64 = byteArrayToBase64String(result.signatureValue);
                logger.debug("Finishing transaction:");
                logger.debug("Rechnungsnummer: {}", tx.rechnungsNr);
                logger.debug("TX number: {}", tx.txNumber);
                logger.debug("TX start time: {}", tx.startTimeString);
                logger.debug("TX end time: {}", tx.endTimeString);
                logger.debug("TX processType: {}", tx.processType);
                logger.debug("TX processData: {}", tx.processData);
                logger.debug("TX sig counter: {}", tx.sigCounter);
                logger.debug("TX signature base64: {}", tx.signatureBase64);
                logger.debug("Number of open transactions: {}", tse.getCurrentNumberOfTransactions());
            }
            message = "OK";
        } catch (ErrorFinishTransactionFailed ex) {
            message = "Start transaction failed";
            logger.fatal("Fatal Error: {}", message);
            logger.fatal("Exception:", ex);
        } catch (ErrorNoTransaction ex) {
            message = "No transaction (transaction number wrong)";
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
        } catch (ErrorSeApiNotInitialized ex) {
            message = "SE API not initialized";
            logger.fatal("Fatal Error: {}", message);
            logger.fatal("Exception:", ex);
        } catch (ErrorTimeNotSet ex) {
            message = "Time not set";
            logger.fatal("Fatal Error: {}", message);
            logger.fatal("Exception:", ex);
        } catch (ErrorCertificateExpired ex) {
            message = "Certificate expired";
            logger.fatal("Fatal Error: {}", message);
            logger.fatal("Exception:", ex);
        } catch (ErrorSecureElementDisabled ex) {
            message = "Secure Element disabled";
            logger.fatal("Fatal Error: {}", message);
            logger.fatal("Exception:", ex);
        } catch (SEException ex) {
            message = "Unknown error during startTransaction()";
            logger.fatal("Fatal Error: {}", message);
            logger.fatal("Exception:", ex);
        }
        if (message != "OK") {
            tx.tseError = message;
            storeTransactionInDB();
            // Make room for next transaction:
            tx = new TSETransaction();
        }
        return message;
    }
    
    /** Cancel the started TSE transaction, i.e. payment never happens */
    public TSETransaction cancelTransaction() {
        /* Zitat DSFinV-K v2.2 (Anhang A, S. 45): (https://www.bzst.de/DE/Unternehmen/Aussenpruefungen/DigitaleSchnittstelleFinV/digitaleschnittstellefinv_node.html)
            "Der Vorgangstyp „AVBelegabbruch“ kennzeichnet alle Vorgänge, die nach Transaktionsbeginn abgebrochen werden.
             Eine tatsächliche Bezahlung darf im Zusammenhang mit diesem Vorgangstyp nicht erfolgen."
        */
        String processData = "AVBelegabbruch^0.00_0.00_0.00_0.00_0.00^"; // siehe hierzu auch: https://support.gastro-mis.de/support/solutions/articles/36000246958-avbelegabbruch
        String message = sendFinishTransaction(processData, null);
        storeTransactionInDB();
        TSETransaction res_tx = tx;
        // Make room for next transaction:
        tx = new TSETransaction();
        if (message != "OK") {
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Die TSE-Transaktion konnte nicht abgebrochen werden!\n\n"+
                "Fehler: "+message+".\n\n"+
                "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                "Bitte Fehler beheben und erneut versuchen.",
                "Fehlgeschlagener Transaktionsabbruch der TSE", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error
            disconnectFromTSE();
            System.exit(1);
        }
        return res_tx;
    }

    private String renderProcessData(BigDecimal steuer_allgemein, BigDecimal steuer_ermaessigt, /* Für Steuersätze, siehe DSFinV-K v2.2 (Anhang I, S. 110, S. 25) */
                                     BigDecimal steuer_durchschnitt_nr3, BigDecimal steuer_durchschnitt_nr1,
                                     BigDecimal steuer_null,
                                     Vector<Vector<String>> zahlungen) { /* Für Zahlungen, siehe DSFinV-K v2.2 (Anhang I, S. 110f)
                                         Format hier: pro Zahlung 1. String Bar|Unbar, 2. String Betrag, 3. String (optional) Währungscode */
        /* Zitat DSFinV-K v2.2 (Anhang A, S. 42): (https://www.bzst.de/DE/Unternehmen/Aussenpruefungen/DigitaleSchnittstelleFinV/digitaleschnittstellefinv_node.html)
            "Der Vorgangstyp „Beleg“ umfasst alle Vorgänge, die über die Kasse abgeschlossen werden.
             Der Vorgangstyp umfasst neben der Rechnung (§ 14 UStG) auch Gutschriften und Korrekturrechnungen.
             Beim Vorgangstyp „Beleg“ sind alle Zahlarten möglich.
             Der Vorgangstyp „Beleg“ ist immer dann zu wählen, wenn eine Änderung der Vermögenszusammensetzung
             des Betriebes aus dem Vorgang resultiert."
        */
        
        /* Process the VAT values: */

        steuer_allgemein = steuer_allgemein == null ? new BigDecimal("0.00") : steuer_allgemein;
        steuer_ermaessigt = steuer_ermaessigt == null ? new BigDecimal("0.00") : steuer_ermaessigt;
        steuer_durchschnitt_nr3 = steuer_durchschnitt_nr3 == null ? new BigDecimal("0.00") : steuer_durchschnitt_nr3;
        steuer_durchschnitt_nr1 = steuer_durchschnitt_nr1 == null ? new BigDecimal("0.00") : steuer_durchschnitt_nr1;
        steuer_null = steuer_null == null ? new BigDecimal("0.00") : steuer_null;
        String processData = "Beleg^";
        // processData += String.format("%s_%s_%s_%s_%s^",
        //     bc.priceFormatterIntern(steuer_allgemein), bc.priceFormatterIntern(steuer_ermaessigt),
        //     bc.priceFormatterIntern(steuer_durchschnitt_nr3), bc.priceFormatterIntern(steuer_durchschnitt_nr1),
        //     bc.priceFormatterIntern(steuer_null));
        processData += String.format("%.2f_%.2f_%.2f_%.2f_%.2f^", steuer_allgemein, steuer_ermaessigt,
            steuer_durchschnitt_nr3, steuer_durchschnitt_nr1, steuer_null);

        /* Process the payment values: */

        // Need to sort payments for correct order as defined in DSFinV-K, p. 110f
        zahlungen.sort(new Comparator<Vector<String>>() {
            public int compare(Vector<String> one, Vector<String> two) {
                // Bar first:
                if (!one.get(0).equals(two.get(0))) return one.get(0).equals("Bar") ? -1 : 1;
                // Within Bar and Unbar, sort alphabetically by currency codes if provided
                if (one.size() == 3 && two.size() == 3) return one.get(2).compareTo(two.get(2));
                // No currency code provided, so size of 2 instead of 3, comes first
                return one.size() - two.size();
            }
        });

        int i = 0;
        for (Vector<String> zahlung : zahlungen) {
            if (i > 0) processData += "_";
            processData += String.format("%.2f:%s", new BigDecimal(zahlung.get(1)), zahlung.get(0));
            if (zahlung.size() > 2) processData += String.format(":%s", zahlung.get(2));
            i += 1;
        }
        return processData;

        /* Test:
            /* Example from DSFinV-K (p. 111):
                Should return: Beleg^75.33_7.99_0.00_0.00_0.00^10.00:Bar_5.00:Bar:CHF_5.00:Bar:USD_64.30:Unbar
                Result:        Beleg^75.33_7.99_0.00_0.00_0.00^10.00:Bar_5.00:Bar:CHF_5.00:Bar:USD_64.30:Unbar
            * /
            Vector<Vector<String>> v = new Vector<Vector<String>>(2);
            Vector<String> z1 = new Vector<String>();
            z1.add("Bar"); z1.add("5.00"); z1.add("CHF");
            Vector<String> z2 = new Vector<String>();
            z2.add("Bar"); z2.add("5.00"); z2.add("USD");
            Vector<String> z3 = new Vector<String>();
            z3.add("Bar"); z3.add("10.00");
            Vector<String> z4 = new Vector<String>();
            z4.add("Unbar"); z4.add("64.30");
            v.add(z4);
            v.add(z2);
            v.add(z1);
            v.add(z3);
            System.out.println(renderProcessData(new BigDecimal("75.33"), new BigDecimal("7.99"), null, null, null, v));
        */
    }

     /** Finish the TSE transaction by entering payment details */
    public TSETransaction finishTransaction(int rechnungsNr,
                                            /* Für Steuersätze, siehe DSFinV-K v2.2 (Anhang I, S. 110, S. 25) */
                                            BigDecimal steuer_allgemein, // (19% MwSt)
                                            BigDecimal steuer_ermaessigt, // (7% MwSt)
                                            BigDecimal steuer_durchschnitt_nr3, // (10,7%) Durchschnittsatz (§ 24 Abs. 1 Nr. 3 UStG)
                                            BigDecimal steuer_durchschnitt_nr1, // (5,5%) Durchschnittsatz (§ 24 Abs. 1 Nr. 1 UStG)
                                            BigDecimal steuer_null, // (0% MwSt)
                                            Vector<Vector<String>> zahlungen) { /* Für Zahlungen, siehe DSFinV-K v2.2 (Anhang I, S. 110f)
                                                Format hier: pro Zahlung 1. String Bar|Unbar, 2. String Betrag, 3. String (optional) Währungscode */
        String processData = renderProcessData(steuer_allgemein, steuer_ermaessigt,
                                               steuer_durchschnitt_nr3, steuer_durchschnitt_nr1,
                                               steuer_null, zahlungen);
        String message = sendFinishTransaction(processData, rechnungsNr);
        storeTransactionInDB();
        TSETransaction res_tx = tx;
        // Make room for next transaction:
        tx = new TSETransaction();
        if (message != "OK") {
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Die TSE-Transaktion konnte nicht abgeschlossen werden!\n\n"+
                "Fehler: "+message+".\n\n"+
                "Da der Betrieb ohne TSE ILLEGAL ist, wird die Kassensoftware jetzt beendet.\n"+
                "Bitte Fehler beheben und erneut versuchen.",
                "Fehlgeschlagener Transaktionsabschluss der TSE", JOptionPane.ERROR_MESSAGE);
            // Exit application upon this fatal error
            disconnectFromTSE();
            System.exit(1);
        }
        return res_tx;
    }

    private void storeTransactionInDB() {
        // store tx in the DB using this.pool
        String message = "";
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO tse_transaction SET "+
                "transaction_number = ?, "+
                "rechnungs_nr = ?, "+
                "transaction_start = ?, "+
                "transaction_end = ?, "+
                "process_type = ?, "+
                "signature_counter = ?, "+
                "signature_base64 = ?, "+
                "tse_error = ?, "+
                "process_data = ?"
            );
            pstmtSetInteger(pstmt, 1, tx.txNumber == null ? null : Math.toIntExact(tx.txNumber));
            pstmtSetInteger(pstmt, 2, tx.rechnungsNr);
            pstmt.setString(3, tx.startTimeString);
            pstmt.setString(4, tx.endTimeString);
            pstmt.setString(5, tx.processType);
            pstmtSetInteger(pstmt, 6, tx.sigCounter == null ? null : Math.toIntExact(tx.sigCounter));
            pstmt.setString(7, tx.signatureBase64);
            pstmt.setString(8, tx.tseError);
            pstmt.setString(9, tx.processData);
            int result = pstmt.executeUpdate();
            if (result == 0){
                message = "executeUpdate() returned 0";
            }
            pstmt.close();
            connection.close();
            message = "OK";
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
            message = ex.getMessage();
        } catch (ArithmeticException ex) {
            logger.error("Exception:", ex);
            message = "One of txNumber or sigCounter is too large to store as integer in DB!!!\n"+
                ex.getMessage();
        }
        if (message != "OK") {
            JOptionPane.showMessageDialog(this.mainWindow,
                "ACHTUNG: Die TSE-Transaktion konnte nicht in der Datenbank gespeichert werden.\n"+
                "Verbindung zum Datenbank-Server unterbrochen?\n"+
                "Fehlermeldung: "+message,
                "Fehler beim Speichern der TSE-Transaktion in der DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    public TSETransaction getTransactionByRechNr(int rechnungsNr) {
        TSETransaction transaction = null;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT "+
                "transaction_number, transaction_start, transaction_end, "+
                "process_type, signature_counter, signature_base64, tse_error, "+
                "process_data "+
                "FROM tse_transaction WHERE rechnungs_nr = ?"
            );
            pstmtSetInteger(pstmt, 1, rechnungsNr);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                transaction = new TSETransaction();
                transaction.rechnungsNr = rechnungsNr;
                transaction.txNumber = (long)rs.getInt(1);
                transaction.startTimeString = rs.getString(2);
                transaction.endTimeString = rs.getString(3);
                transaction.processType = rs.getString(4);
                transaction.sigCounter = (long)rs.getInt(5);
                transaction.signatureBase64 = rs.getString(6);
                transaction.tseError = rs.getString(7);
                transaction.processData = rs.getString(8);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
        }
        return transaction;
    }

    public TSETransaction getTransactionByTxNumber(int txNumber) {
        TSETransaction transaction = null;
        try {
            Connection connection = this.pool.getConnection();
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT "+
                "rechnungs_nr, transaction_start, transaction_end, "+
                "process_type, signature_counter, signature_base64, tse_error, "+
                "process_data "+
                "FROM tse_transaction WHERE transaction_number = ?"
            );
            pstmtSetInteger(pstmt, 1, txNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                transaction = new TSETransaction();
                transaction.rechnungsNr = rs.getInt(1);
                transaction.txNumber = (long)txNumber;
                transaction.startTimeString = rs.getString(2);
                transaction.endTimeString = rs.getString(3);
                transaction.processType = rs.getString(4);
                transaction.sigCounter = (long)rs.getInt(5);
                transaction.signatureBase64 = rs.getString(6);
                transaction.tseError = rs.getString(7);
                transaction.processData = rs.getString(8);
            }
            rs.close();
            pstmt.close();
            connection.close();
        } catch (SQLException ex) {
            logger.error("Exception:", ex);
        }
        return transaction;
    }

    /**
     *    * Each non abstract class that implements the ActionListener
     *      must have this method.
     *
     *    @param e the action event.
     **/
    public void actionPerformed(ActionEvent e) {
    }

}