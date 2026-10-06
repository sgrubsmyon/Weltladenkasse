/*
 * Copyright (c) 2019-2024
 * cv cryptovision GmbH
 * Munscheidstr. 14
 * 45886 Gelsenkirchen
 * Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/**
 * Java package for the TR-03151 Secure Element API by cryptovision (Java version)
 */
package com.cryptovision.SEAPI;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import com.cryptovision.SEAPI.exceptions.ErrorDescriptionSetByManufacturer;
import com.cryptovision.SEAPI.exceptions.ErrorIdNotFound;
import com.cryptovision.SEAPI.exceptions.ErrorRestoreFailed;
import com.cryptovision.SEAPI.exceptions.ErrorSECommunicationFailed;
import com.cryptovision.SEAPI.exceptions.ErrorSecureElementDisabled;
import com.cryptovision.SEAPI.exceptions.ErrorSelftestFailed;
import com.cryptovision.SEAPI.exceptions.ErrorSigningSystemOperationDataFailed;
import com.cryptovision.SEAPI.exceptions.ErrorStorageFailure;
import com.cryptovision.SEAPI.exceptions.ErrorTSECommandDataInvalid;
import com.cryptovision.SEAPI.exceptions.ErrorTSEResponseDataInvalid;
import com.cryptovision.SEAPI.exceptions.SEException;

/**
 * TR-03151 Secure Element API
 * <p>
 * Please refer to BSI specification for parameter and exceptions definition where not explicitly defined below.
 * <p>
 * Use {@link #getInstance(String)} to start.
 * {@link #close()} shall be used on application shutdown (latest), to close any transport internal resources.
 *
 * @author cv cryptovision GmbH
 * @version TR-03151 Version 1.0.1 (@cite TR-03151);<br>
 * w/ TR-03153 Version 1.0.1 (@cite TR-03153);<br>
 * w/ Klarstellungen TR-03153 (@cite KuA-03153)
 */
public abstract class TSE {

	private static final String SE_API_VERSION_STRING = "cryptovision SE-API v3.1";
	private static final byte[] SE_API_VERSION = new byte[] { 3, 1 };

	/** Timeouts apply: Admin is automatically logged out after 10 minutes. */
	public static final String USER_ID_ADMIN = "Admin";
	/** Timeouts apply: TimeAdmin is automatically logged out after 2 seconds */
	public static final String USER_ID_TIME_ADMIN = "TimeAdmin";

	/**
	 * Note: since SE-API 3.0, {@link #open()} must also be called.
	 * @param configFileName name of configuration file, either relative or absolute. For documentation of file content, please see @ref TLConfigFile
	 * @return {@link TSE} instance.
	 * @throws SEException depending on transport mode and configuration defined in config file.
	 * @throws FileNotFoundException
	 * @throws IOException open/read errors.
	 */
	public static TSE getInstance(String configFileName) throws SEException, FileNotFoundException, IOException {
		Properties props = new Properties();
		FileReader reader = new FileReader(configFileName);
		props.load(reader);
		reader.close();
		props.setProperty("filename", configFileName);

		return getInstance(props);
	}

	/**
	 * Note: since SE-API 3.0, {@link #open()} must also be called.
	 * @param props Java Properties as you would define them in a config file @ref TLConfigFile
	 * @return {@link TSE} instance.
	 * @throws SEException depending on transport mode and configuration defined in config file.
	 * @throws FileNotFoundException
	 * @throws IOException open/read errors.
	 */
	public static TSE getInstance(Properties props) throws SEException, FileNotFoundException, IOException {
		try {
			String connector = props.getProperty("connector");
			if(connector == null)
				connector = "com.cryptovision.SEAPI.TSEConnector";

			return (TSE) Class.forName(connector).getDeclaredConstructor(Properties.class).newInstance(props);
		} catch (InvocationTargetException e) {
			if(e.getTargetException() instanceof RuntimeException)
				throw (RuntimeException) e.getTargetException();
			else if(e.getTargetException() instanceof SEException)
				throw (SEException) e.getTargetException();
			else
				throw new Error(e.getTargetException());
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	/**
	 * Initialize connection to TSE device.
	 *
	 * @throws FileNotFoundException
	 * @throws IOException open/read errors.
	 * @throws ErrorSECommunicationFailed TSE could not be started, Secure Element not available. All log data can be exported, but tar files will not contain certificates.
	 * @throws ErrorSelftestFailed
	 * @throws SEException depending on transport mode and configuration defined in config file.
	 *
	 * @since 3.0.0
	 */
	public abstract void open() throws SEException, IOException;

	/**
	 * Initialize connection to TSE device.
	 *
	 * @param checkIntegrity <code>false</code> to skip initial internal integrity check
	 *
	 * @throws FileNotFoundException
	 * @throws IOException open/read errors.
	 * @throws ErrorSECommunicationFailed TSE could not be started, Secure Element not available. All log data can be exported, but tar files will not contain certificates.
	 * @throws ErrorSelftestFailed
	 * @throws SEException depending on transport mode and configuration defined in config file.
	 *
	 * @since 3.1.0
	 */
	public abstract void open(boolean checkIntegrity) throws SEException, IOException;

	/**
	 * Reinitialize connection to TSE device.
	 * <p>
	 * Can be used in {@link LCS#sleep LCS.sleep or LCS.closed}.<br>
	 * This might or might not power cycle or reset the device.
	 *
	 * @throws SEException depending on transport mode and configuration defined in config file.
	 * @throws FileNotFoundException
	 * @throws IOException failed.
	 * @throws ErrorTSEResponseDataInvalid if TSE found is incompatible with current initialization
	 *
	 * @since 2.4.0
	 */
	public abstract void reopen() throws SEException, FileNotFoundException, IOException;

	/**
	 * Reinitialize connection to TSE device.
	 * <p>
	 * Can be used in {@link LCS#sleep LCS.sleep or LCS.closed}.<br>
	 * This might or might not power cycle or reset the device.
	 *
	 * @param checkIntegrity <code>false</code> to skip initial internal integrity check
	 *
	 * @throws SEException depending on transport mode and configuration defined in config file.
	 * @throws FileNotFoundException
	 * @throws IOException failed.
	 * @throws ErrorTSEResponseDataInvalid if TSE found is incompatible with current initialization
	 *
	 * @since 3.1.0
	 */
	public abstract void reopen(boolean checkIntegrity) throws SEException, FileNotFoundException, IOException;

	/**
	 * @return API version as String
	 */
	public static String getApiVersionString() {
		return SE_API_VERSION_STRING;
	}

	/**
	 * @return API major/minor version as byte[2]
	 */
	public static byte[] getApiVersion() {
		return SE_API_VERSION;
	}

	/**
	 * @return implementation version as String
	 */
	public abstract String getImplementationVersionString();

	/**
	 * @return implementation version as byte[3] where first two bytes match API version
	 */
	public abstract byte[] getImplementationVersion();

	/**
	 * @return some identifier guaranteed to be unambiguous for every cryptovision TSE
	 * @since 2.1
	 */
	public abstract byte[] getUniqueId() throws SEException;

	/**
	 * @return certification ID as assigned by BSI ("BSI-K-TR-0374-2019" for cryptovision TSE)
	 * @since 2.1
	 */
	public abstract String getCertificationId() throws SEException;

	/**
	 * @return some firmware identifier
	 */
	public abstract String getFirmwareId() throws SEException;

	/**
	 * TSE version
	 * @since 2.4.1
	 */
	public enum TSE_version { v1, v2, v2_1 };

	/**
	 * @return TSE version
	 * @since 2.4.1
	 */
	public abstract TSE_version getHardwareVersion() throws SEException;

	/**
	 * @return <code>true</code> if factory reset is available
	 * @since 3.0.0
	 */
	public abstract boolean isEngineeringSample() throws SEException;

	/**
	 * Apply firmware update to TSE.
	 * <p><ul>
	 * <li>Requires IO timeout of at least 5000ms.
	 * <li>Requires Admin authentication.
	 * <li>Requires {@link LCS#active} (or {@link LCS#noTime}).
	 * <li>Recommends time set.
	 * </ul>
	 *
	 * @throws ErrorStorageFailure
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 * @throws ErrorFirmwareUpdateFailed
	 *
	 * @since 2.4.0
	 */
	public abstract void updateFirmware(byte[] binary) throws SEException;

	/**
	 * TSE Life Cycle State
	 */
	public enum LCS {
		unknown, 		/**< undefined life cycle state	*/
		notInitialized,	/**< {@link TSE#initialize() initialize()} not called yet */
		noTime,			/**< time not set */
		active,			/**< ready to sign transactions */
		deactivated,	/**< after {@link TSE#lockTransactionLogging() lockTransactionLogging()} */
		disabled,		/**< after {@link TSE#disableSecureElement() disableSecureElement()} */
		seError,		/**< Secure Element not available @since 2.4.0 */
		sleep,			/**< device was powered down (by OS or card reader) @since 2.4.0 */
		closed			/**< device handle was closed @since 2.4.0 */
		;				//	KEEP on separate line to mollify Doxygen

		public static LCS from(byte value) throws ErrorTSEResponseDataInvalid {
			for(LCS v : values())
				if(v.ordinal() == value)
					return v;
			throw new ErrorTSEResponseDataInvalid();
		}
	};

	/**
	 * {@link TSE#selfTest(int) selftest()} return value:
	 * <ul>
	 * <li>{@link #completed}
	 * <li>{@link #errorsFound}
	 * <li>{@link #level}
	 * <li>{@link #counters}
	 * <li>{@link #log}
	 * </ul>
	 */
	public static class TestResult {
		/** indicates whether or not testing was completed during given maximum runtime */
		public boolean completed;
		/** indicates if any errors have been found during current test sequence */
		public boolean errorsFound;
		/** abstract level of device status, @see <a href="https://support.cryptovision.com/tse/">online FAQ</a> */
		public int level;
		/** number of errors of different type, @see <a href="https://support.cryptovision.com/tse/">online FAQ</a> */
		public int[] counters;
		/** error log to be analyzed by manufacturer */
		public String log;
	}
	/**
	 * Start extended device self test.
	 * <p>
	 * Test runtime can be limited to not block the device for too long. Return value indicates if more
	 * testing needs to done for full coverage. Device will continue testing on next call but start from
	 * beginning after {@link TSE#getInstance(Properties) getInstance()} or {@link TSE#reopen() reopen()}.
	 * @param maxDuration intended test runtime in seconds
	 * @return most recent {@link TestResult}
	 * @since 3.0.0
	 */
	public abstract TestResult selfTest(int maxDuration) throws SEException;

	/**
	 * @return current life cycle state<br/>
	 *         Note: <code>transport=MSCJava10Transport</code> with <code>MSCJava10Reopen=true</code> will return {@link LCS#sleep sleep} instead of {@link LCS#closed closed}.
	 * @throws ErrorSECommunicationFailed
	 * @since 2.0
	 */
	public abstract LCS getLifeCycleState() throws SEException;

	/**
	 * @return "PIN in transport state" per Admin PIN, Admin PUK, TimeAdmin PIN, TimeAdmin PUK
	 * @throws ErrorSECommunicationFailed
	 * @since 0.97
	 */
	public abstract boolean[] getPinStatus() throws SEException;

	/**
	 * Initialize PUK value if still in transport state.
	 *
	 * @note  PUK values cannot be changed once initialized.
	 *
	 * @param userId
	 * @param puk new PUK must be 10 bytes
	 *
	 * @throws ErrorUserIdNotManaged
	 * @throws ErrorSECommunicationFailed e.g. PUK already initialized
	 * @throws ErrorTSECommandDataInvalid e.g. wrong length
	 * @since 2.4.0
	 */
	public abstract void initializePuk(String userId, byte[] puk) throws SEException;

	/**
	 * Set PIN/PUK values if in transport state.
	 *
	 * @note  The length of either PIN must be exactly  8 bytes.
	 * @note  The length of either PUK must be exactly 10 bytes.
	 * @note  PUK values cannot be changed once initialized.
	 *
	 *
	 * @param adminPIN 8 bytes or <code>null</code> to not touch it
	 * @param adminPUK 10 bytes or <code>null</code> to not touch it
	 * @param timePIN 8 bytes or <code>null</code> to not touch it
	 * @param timePUK 10 bytes or <code>null</code> to not touch it
	 * @throws ErrorTSECommandDataInvalid
	 * @throws ErrorSECommunicationFailed
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorStorageFailure
	 * @since 0.97
	 * @deprecated Use {@link #initializePuk(String, byte[])}
	 * and {@link #unblockUser(String, byte[], byte[])} instead to avoid communication timeouts.
	 */ @Deprecated
	public final void initializePinValues(byte[] adminPIN, byte[] adminPUK, byte[] timePIN, byte[] timePUK) throws SEException {
		if(adminPUK == null && adminPIN != null
		|| timePUK  == null && timePIN  != null)
			throw new ErrorTSECommandDataInvalid("must provide PUK to be able to initialize PIN");

		if(adminPUK != null) {
			initializePuk(USER_ID_ADMIN, adminPUK);
			if(adminPIN != null)
				unblockUser(USER_ID_ADMIN, adminPUK, adminPIN);
		}
		if(timePUK != null) {
			initializePuk(USER_ID_TIME_ADMIN, timePUK);
			if(timePIN != null)
				unblockUser(USER_ID_TIME_ADMIN, timePUK, timePIN);
		}
	}

	/**
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorStoringInitDataFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 * @throws ErrorDescriptionNotSetByManufacturer
	 * @throws ErrorDescriptionSetByManufacturer
	 * @throws ErrorSECommunicationFailed e.g. already initialized
	 */
	public abstract void initialize() throws SEException;
	/**
	 * @param  description	Textual description for this TSE. Pass <code>null</code> as the cryptovision TSE does not support (re-) naming the TSE.
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorStoringInitDataFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 * @throws ErrorDescriptionNotSetByManufacturer
	 * @throws ErrorDescriptionSetByManufacturer
	 * @since 2.0
	 */
	public final void initialize(String description) throws SEException {
		if(description != null)
			throw new ErrorDescriptionSetByManufacturer();
		initialize();
	}

	/**
	 * This command can be used to temporarily deactivate the TSE.
	 * It is meant to be used as protection of an entirely personalized TSE (i.e. with PINs assigned and ERSs mapped) during transport.
	 * <p><ul>
	 * <li>Requires Admin authentication.
	 * <li>Requires time set.
	 * </ul>
	 *
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 * @throws ErrorSeApiDeactivated
	 * @throws ErrorTimeNotSet
	 * @since 0.97
	 * @deprecated Now defined by BSI as {@link #lockTransactionLogging()}.
	 */ @Deprecated
	public abstract void deactivateTSE() throws SEException;

	/**
	 * <p><ul>
	 * <li>Requires Admin authentication.
	 * <li>Requires time set.
	 * </ul>
	 *
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 * @throws ErrorSeApiDeactivated
	 * @throws ErrorTimeNotSet
	 * @since 2.4.0
	 */
	public abstract void lockTransactionLogging() throws SEException;

	/**
	 * This command works identical to the {@link #initialize()} command.
	 * It is meant to enable an entirely personalized TSE (i.e. with PINs assigned and ERSs mapped) after final installation in the ERS.
	 * <p><ul>
	 * <li>Requires Admin authentication.
	 * </ul>
	 *
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 * @throws ErrorSeApiNotDeactivated
	 * @since 0.97
	 * @deprecated Now defined by BSI as {@link #unlockTransactionLogging()}.
	 */ @Deprecated
	public abstract void activateTSE() throws SEException;

	/**
	 * <p><ul>
	 * <li>Requires Admin authentication.
	 * </ul>
	 *
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 * @throws ErrorSeApiNotDeactivated
	 * @since 2.4.0
	 */
	public abstract void unlockTransactionLogging() throws SEException;

	/**
	 * Map the serial number of an ERS to a signature key.
	 * <p>
	 * This assigns an existing private key to a client (cash register).<br>
	 * Method can also be used to delete such a mapping, but see {@link #unmapERS(String)}.
	 * <p><ul>
	 * <li>Requires Admin authentication - TSE v1 only.
	 * <li>Requires time set.
	 * <li>Pass <code>null</code> for <code>serialNumberKey</code> to delete a mapping.
	 * </ul>
	 *
	 * @param	clientId		serial number of the ERS (String of 1 to 30 bytes)
	 * @param	serialNumberKey	ID of the key to be mapped (SHA256 hash value of the public key) or<br><code>null</code> to remove a client-mapping
	 *
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorStoringInitDataFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorTimeNotSet
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorUserNotAuthorized - TSE v1 only
	 * @throws ErrorUserNotAuthenticated - TSE v1 only
	 * @throws ErrorNoSuchKey unknown <code>serialNumberKey</code>
	 * @throws ErrorNoERS with <code>serialNumberKey == null</code>: clientId not mapped
	 * @throws ErrorERSalreadyMapped
	 *
	 * @deprecated use {@link #registerClient(String)}
	 */ @Deprecated
	public abstract void mapERStoKey(String clientId, byte[] serialNumberKey) throws SEException;

	/**
	 * Register a clientId.
	 * <p><ul>
	 * <li>Requires time set.
	 * <li>Requires Admin authentication - TSE v1 only.
	 * </ul>
	 *
	 * @param	clientId		serial number of the ERS (String of 1 to 30 bytes)
	 *
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorStoringInitDataFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorTimeNotSet
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorERSalreadyMapped
	 * @since 2.4.0
	 */
	public abstract void registerClient(String clientId) throws SEException;

	/**
	 * Unmap the serial number of an ERS.
	 * <p>
	 * This deletes a client (cash register) to private key mapping.
	 * <p><ul>
	 * <li>Requires Admin authentication - TSE v1 only.
	 * <li>Requires time set.
	 * </ul>
	 *
	 * @param	clientId		serial number of the ERS (String of 1 to 30 bytes)
	 *
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorStoringInitDataFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorTimeNotSet
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorUserNotAuthorized - TSE v1 only
	 * @throws ErrorUserNotAuthenticated - TSE v1 only
	 * @throws ErrorSECommunicationFailed clientId not mapped
	 * @since 2.3
	 *
	 * @deprecated use {@link #deregisterClient(String)}
	 */ @Deprecated
	public abstract void unmapERS(String clientId) throws SEException;

	/**
	 * Unregister a clientId
	 * <p><ul>
	 * <li>Requires Admin authentication - TSE v1 only.
	 * <li>Requires time set.
	 * </ul>
	 *
	 * @param	clientId		serial number of the ERS (String of 1 to 30 bytes)
	 *
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorStoringInitDataFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorTimeNotSet
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorUserNotAuthorized - TSE v1 only
	 * @throws ErrorUserNotAuthenticated - TSE v1 only
	 * @throws ErrorSECommunicationFailed clientId not mapped
	 * @since 2.4.0
	 */
	public final void deregisterClient(String clientId) throws SEException {
		unmapERS(clientId);
	}

	/**
	 * @param	unixTime	new time to set in UnixTime format
	 *
	 * @throws ErrorUpdateTimeFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 */
	public abstract void updateTime(long unixTime) throws SEException;

	/**
	 * @throws ErrorDisableSecureElementFailed
	 * @throws ErrorTimeNotSet
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 */
	public abstract void disableSecureElement() throws SEException;

	/**
	 * maximum size of <code>processData</code>.
	 * @since 0.99+
	 * @deprecated see {@link #MAX_SIZE_TRANSPORT_LAYER}
	 */ @Deprecated
	public static final int MAX_LEN_PROCESS_DATA    = 8096;
	/**
	 * the actual limitation is on the overall size of the TSE command
	 * @since 2.0
	 */
	public static final int MAX_SIZE_TRANSPORT_LAYER = 8192;

	/**
	 * common fields in xxxTransaction return values
	 */
	static class TransactionResult {
		public long logTime;			/**< time logged by the CSP */
		public byte[] serialNumber;		/**< signature key serial number */
		public long signatureCounter;	/**< signature counter */
		public byte[] signatureValue;   /**< signature */
	}

	/**
	 * {@link TSE#startTransaction() startTransaction()} return value
	 */
	public static class StartTransactionResult extends TransactionResult {
		public long transactionNumber;	/**< transaction number assigned */
	}

	/**
	 * @param	clientId		ID of the ERS starting the transaction
	 * @param	processData		process data (specified in DSFinV-K)
	 * @param	processType		process type (specified in DSFinV-K)
	 * @param	additionalData	RFU and must not be used according to TR-03151
	 *
	 * @throws ErrorStartTransactionFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE locked for transaction logging, see {@link #lockTransactionLogging()}
	 * @throws ErrorTimeNotSet
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract StartTransactionResult startTransaction(String clientId, byte[] processData, String processType, byte[] additionalData) throws SEException;

	/**
	 * {@link TSE#updateTransaction() updateTransaction()} return value
	 */
	public static class UpdateTransactionResult extends TransactionResult { }
	/**
	 * @throws ErrorUpdateTransactionFailed
	 * @throws ErrorLogMessageRetrievalFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorNoTransaction
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE locked for transaction logging, see {@link #lockTransactionLogging()}
	 * @throws ErrorTimeNotSet
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract UpdateTransactionResult updateTransaction(String clientId, long transactionNumber, byte[] processData, String processType) throws SEException;

	/**
	 * {@link TSE#finishTransaction() finishTransaction()} return value
	 */
	public static class FinishTransactionResult extends TransactionResult { }
	/**
	 * @param	clientId			ID of the ERS finishing the transaction
	 * @param	transactionNumber	transaction number to finish
	 * @param	processData			process data (specified in DSFinV-K)
	 * @param	processType			process type (specified in DSFinV-K)
	 * @param	additionalData		RFU and must not be used according to TR-03151
	 *
	 * @throws ErrorFinishTransactionFailed
	 * @throws ErrorNoTransaction missing in BSI API
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE locked for transaction logging, see {@link #lockTransactionLogging()}
	 * @throws ErrorTimeNotSet
	 * @throws ErrorCertificateExpired
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract FinishTransactionResult finishTransaction(String clientId, long transactionNumber, byte[] processData, String processType, byte[] additionalData) throws SEException;

	/**
	 * @return list of dangling ("open") transactions from the SE.
	 *
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @throws ErrorTimeNotSet
	 * @throws ErrorSecureElementDisabled
	 * @since 0.97
	 */
	public abstract long[] getOpenTransactions() throws SEException;

	/**
	 * Get size of tar file exported by {@link #exportData(String, Long, Long, Long, Long, Long, Long) exportData()} methods.
	 *
	 * Use <code>null</code> to mark optional parameters as undefined.
	 *
	 * @param	clientId				ID of the ERS to export data for
	 * @param	transactionNumber		single transaction number to export
	 * @param	startTransactionNumber	start of transaction number range to export
	 * @param	endTransactionNumber	end of transaction number range to export
	 * @param	startDate				start of date range to export
	 * @param	endDate					end of date range to export
	 *
	 * @throws ErrorIdNotFound
	 * @throws ErrorTransactionNumberNotFound
	 * @throws ErrorNoDataAvailable
	 * @throws ErrorParameterMismatch
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @since 2.4.0
	 */
	public abstract long getExportSize(String clientId, Long transactionNumber, Long startTransactionNumber, Long endTransactionNumber, Long startDate, Long endDate) throws SEException, IOException;

	/**
	 * Use <code>null</code> to mark optional parameters as undefined.
	 *
	 * @param	clientId				ID of the ERS to export data for
	 * @param	transactionNumber		single transaction number to export
	 * @param	startTransactionNumber	start of transaction number range to export
	 * @param	endTransactionNumber	end of transaction number range to export
	 * @param	startDate				start of date range to export
	 * @param	endDate					end of date range to export
	 * @param	maximumNumberRecords	max. number of records to export
	 *
	 * @throws ErrorIdNotFound
	 * @throws ErrorTransactionNumberNotFound
	 * @throws ErrorNoDataAvailable
	 * @throws ErrorTooManyRecords
	 * @throws ErrorParameterMismatch
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @throws IOException
	 * @since 0.99
	 */
	public abstract byte[] exportData(String clientId, Long transactionNumber, Long startTransactionNumber, Long endTransactionNumber, Long startDate, Long endDate, Long maximumNumberRecords) throws SEException, IOException;
	/**
	 * Use <code>null</code> to mark optional parameters as undefined.
	 *
	 * @param	clientId				ID of the ERS to export data for
	 * @param	transactionNumber		single transaction number to export
	 * @param	startTransactionNumber	start of transaction number range to export
	 * @param	endTransactionNumber	end of transaction number range to export
	 * @param	startDate				start of date range to export
	 * @param	endDate					end of date range to export
	 * @param	maximumNumberRecords	max. number of records to export
	 * @param 	fileName 				Export data written to disk. File must not yet exist.
	 *
	 * @throws ErrorIdNotFound
	 * @throws ErrorTransactionNumberNotFound
	 * @throws ErrorNoDataAvailable
	 * @throws ErrorTooManyRecords
	 * @throws ErrorParameterMismatch
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @throws IOException
	 * @since 0.99
	 */
	public abstract void exportData(String clientId, Long transactionNumber, Long startTransactionNumber, Long endTransactionNumber, Long startDate, Long endDate, Long maximumNumberRecords, String fileName) throws SEException, IOException;
	/**
	 * Use <code>null</code> to mark optional parameters as undefined.
	 *
	 * @param	clientId				ID of the ERS to export data for
	 * @param	transactionNumber		single transaction number to export
	 * @param	startTransactionNumber	start of transaction number range to export
	 * @param	endTransactionNumber	end of transaction number range to export
	 * @param	startDate				start of date range to export
	 * @param	endDate					end of date range to export
	 * @param	maximumNumberRecords	max. number of records to export
	 * @param	stream					Export data is written to this instance.
	 *
	 * @throws ErrorIdNotFound
	 * @throws ErrorTransactionNumberNotFound
	 * @throws ErrorNoDataAvailable
	 * @throws ErrorTooManyRecords
	 * @throws ErrorParameterMismatch
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @throws IOException
	 * @since 0.99
	 */
	public abstract void exportData(String clientId, Long transactionNumber, Long startTransactionNumber, Long endTransactionNumber, Long startDate, Long endDate, Long maximumNumberRecords, OutputStream stream) throws SEException, IOException;
	/**
	 * Use -1 (transactionNumber) / 0 and Long.MAX_VALUE (for long parameters) to mark optional parameters as undefined.
	 *
	 * @param	clientId				ID of the ERS to export data for
	 * @param	transactionNumber		single transaction number to export
	 * @param	startTransactionNumber	start of transaction number range to export
	 * @param	endTransactionNumber	end of transaction number range to export
	 * @param	startDate				start of date range to export
	 * @param	endDate					end of date range to export
	 * @param	maximumNumberRecords	max. number of records to export
	 *
	 * @throws ErrorIdNotFound
	 * @throws ErrorTransactionNumberNotFound
	 * @throws ErrorNoDataAvailable
	 * @throws ErrorTooManyRecords
	 * @throws ErrorParameterMismatch
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @throws IOException
	 * @deprecated Use {@link #exportData(String, Long, Long, Long, Long, Long, Long)} instead.
	 */ @Deprecated
	public final byte[] exportData(String clientId, long transactionNumber, long startTransactionNumber, long endTransactionNumber, long startDate, long endDate, long maximumNumberRecords) throws SEException, IOException {
		Long transactionNumberL      = transactionNumber;
		Long startTransactionNumberL = startTransactionNumber;
		Long endTransactionNumberL   = endTransactionNumber;
		Long startDateL              = startDate;
		Long endDateL                = endDate;
		Long maximumNumberRecordsL   = maximumNumberRecords;
		if(transactionNumber == Long.MAX_VALUE)      transactionNumberL = null;
		if(startTransactionNumber == 0
		|| startTransactionNumber == Long.MAX_VALUE) startTransactionNumberL = null;
		if(endTransactionNumber == Long.MAX_VALUE)   endTransactionNumberL = null;
		if(startDate == 0
		|| startDate == Long.MAX_VALUE)              startDateL = null;
		if(endDate == Long.MAX_VALUE)                endDateL = null;
		if(maximumNumberRecords == Long.MAX_VALUE)   maximumNumberRecordsL = null;
		return exportData(clientId, transactionNumberL, startTransactionNumberL, endTransactionNumberL, startDateL, endDateL, maximumNumberRecordsL);
	}

	/**
	 * Get size of tar file exported by {@link #exportMoreData(byte[], Long, Long, OutputStream)}.
	 *
	 * @param 	previousSignatureCounter last seen signature counter
	 * @param	maximumNumberRecords	max. number of records to export
	 *
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @throws ErrorNoSuchKey
	 * @throws ErrorIdNotFound
	 * @since 2.4.0
	 */
	public abstract long getExportSize(Long previousSignatureCounter, Long maximumNumberRecords) throws SEException;

	/**
	 * OuputStream extension to receive total size of exported data.
	 *
	 * Implement {@link TSEOutputStream} to receive a {@link #total(long)} callback call
	 * from {@link #exportData(String, Long, Long, Long, Long, Long, Long, OutputStream)} and
	 * {@link #exportMoreData(byte[], Long, Long, OutputStream)}.
	 * @since 2.1
	 */
	public abstract static class TSEOutputStream extends OutputStream {
		/** default implementation saves total number of bytes */
		protected long size;
		/**
		 * Called by export methods to provide total number of bytes in this export.
		 *
		 * @param size
		 */
		public void total(long size) {
			// default implementation: save value
			this.size = size;
		}
		/**
		 * Called by export methods to append data.
		 */
		public abstract void write(byte[] b) throws IOException;
		public final void write(int b) {};
	}
	/**
	 * Continue data export, e.g. immediately after last seen log entry.
	 * <p>
	 * In contrast to BSI defined APIs {@link TSE#exportData(String, Long, Long, Long, Long, Long, Long) exportData()},
	 * <code>maximumNumberRecords</code> will <i>limit</i> the number of exported log entries but not <i>block</i> the export
	 * if more data is available.
	 *
	 * @param 	serialNumberKey          serial of key used in previous log entry.
	 * @param 	previousSignatureCounter last seen signature counter
	 * @param	maximumNumberRecords	max. number of records to export
	 * @param	stream					Export data is written to this instance.
	 *
	 * @throws ErrorNoSuchKey
	 * @throws ErrorIdNotFound
	 * @throws ErrorStreamWrite
	 * @since 2.0
	 * @deprecated For TSE v2 use {@link #exportMoreData(Long, Long, OutputStream)}.
	 */ @Deprecated
	public abstract void exportMoreData(byte[] serialNumberKey, Long previousSignatureCounter, Long maximumNumberRecords, OutputStream stream) throws SEException;
	/**
	 * Continue data export, e.g. immediately after last seen log entry.
	 * <p>
	 * In contrast to BSI defined APIs {@link TSE#exportData(String, Long, Long, Long, Long, Long, Long) exportData()},
	 * <code>maximumNumberRecords</code> will <i>limit</i> the number of exported log entries but not <i>block</i> the export
	 * if more data is available.
	 *
	 * @param 	previousSignatureCounter last seen signature counter
	 * @param	maximumNumberRecords	max. number of records to export
	 * @param	stream					Export data is written to this instance.
	 *
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @throws ErrorIdNotFound
	 * @throws ErrorStreamWrite
	 * @since 2.4.0
	 */
	public abstract void exportMoreData(Long previousSignatureCounter, Long maximumNumberRecords, OutputStream stream) throws SEException;

	/**
	 * Delete oldest data.
	 *
	 * @param 	serialNumberKey			serial of key
	 * @param 	signatureCounter		highest signature counter to delete
	 *
	 * @throws ErrorNoSuchKey
	 * @throws ErrorIdNotFound
	 * @throws ErrorStreamWrite
	 * @since 2.1
	 * @deprecated For TSE v2 use {@link #deleteStoredDataUpTo(Long)}.
	 */ @Deprecated
	public abstract void deleteStoredDataUpTo(byte[] serialNumberKey, Long signatureCounter) throws SEException;
	/**
	 * Delete oldest data.
	 *
	 * @param 	signatureCounter		highest signature counter to delete
	 *
	 * @throws ErrorIdNotFound
	 * @throws ErrorStreamWrite
	 * @throws ErrorSigningSystemOperationDataFailed if system log could not be created
	 * @throws ErrorStorageFailure if deletion or storage of system log failed
	 * @since 2.4.0
	 */
	public abstract void deleteStoredDataUpTo(Long signatureCounter) throws SEException;

	/**
	 * @throws ErrorExportCertFailed
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @since 0.97
	 */
	public abstract byte[] exportCertificates() throws SEException;
	/**
	 * @deprecated typo in method name compared to TR-03151, use {@link #exportCertificates()}
	 */ @Deprecated
	public final byte[] exportCertificate() throws SEException { return exportCertificates(); }

	/**
	 * @param serialNumberKey
	 * @return certificate expiration date encoded as unix time
	 * @note expiration date is X.509 "notAfter" field +1
	 * @since 2.0
	 * @deprecated For TSE v2 use {@link #getCertificateExpirationDate()}.
	 */ @Deprecated
	public abstract long getCertificateExpirationDate(byte[] serialNumberKey) throws SEException;
	/**
	 * @return certificate expiration date encoded as unix time
	 * @note expiration date is X.509 "notAfter" field +1
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @since 2.4.0
	 */
	public abstract long getCertificateExpirationDate() throws SEException;

	/**
	 * @return ASN.1 encoded sequence of mappings ERS to serial number
	 * @since 2.0
	 * @deprecated For TSE v2 use {@link #listClients()}.
	 */ @Deprecated
	public abstract byte[] getERSMappings() throws SEException;

	/**
	 * @return list of mapped clientIds
	 * @since 2.4.0, not available on TSE v1.
	 */
	public abstract String[] listClients() throws SEException;

	/**
	 * @throws ErrorRestoreFailed
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 */
	public final void restoreFromBackup(byte[] restoreData) throws SEException {
		throw new ErrorRestoreFailed("unimplemented");
	}

	/**
	 * @throws ErrorNoLogMessage
	 * @throws ErrorReadingLogMessage
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract byte[] readLogMessage() throws SEException;

	/**
	 * @throws ErrorExportSerialNumbersFailed
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 */
	public abstract byte[] exportSerialNumbers() throws SEException;

	/**
	 * @throws ErrorGetMaxNumberOfClientsFailed
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract long getMaxNumberOfClients() throws SEException;

	/**
	 * @throws ErrorGetCurrentNumberOfClientsFailed
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract long getCurrentNumberOfClients() throws SEException;

	/**
	 * @throws ErrorGetMaxNumberTransactionsFailed
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract long getMaxNumberOfTransactions() throws SEException;

	/**
	 * @throws ErrorGetCurrentNumberOfTransactionsFailed
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract long getCurrentNumberOfTransactions() throws SEException;

	/**
	 * @return current transaction counter (last used value)
	 * @since 2.0
	 */
	public abstract long getTransactionCounter() throws SEException;

	/**
	 * @return size of log memory in bytes.
	 * @since 2.0
	 */
	public abstract long getTotalLogMemory() throws SEException;

	/**
	 * @return remaining log memory in bytes.
	 * @since 2.0
	 */
	public abstract long getAvailableLogMemory() throws SEException;

	/**
	 * @return smallest signature counter stored in log memory.
	 * @throws ErrorNoDataAvailable
	 * @since 2.4.0, not available on TSE v1.
	 */
	public abstract long getMinSignatureCounter() throws SEException;

	/**
	 * @return biggest signature counter stored in log memory.
	 * @throws ErrorNoDataAvailable
	 * @since 2.4.0, not available on TSE v1.
	 */
	public abstract long getMaxSignatureCounter() throws SEException;

	/**
	 * @return next unexported signature counter stored in log memory. <br><code>counter-1</code> to be used in {@link #exportMoreData(byte[], Long, Long, OutputStream) exportMoreData()}.
	 * @throws ErrorNoDataAvailable if no (unexported) log messages stored.
	 * @since 2.4.0, not available on TSE v1.
	 */
	public abstract long getNextSignatureCounter() throws SEException;

	/**
	 * @return For values below 100, typical data retention is more than 10 years. Bigger values indicate shorter data retention, but at least 1 year.
	 * @since 2.0
	 */
	public abstract int getWearIndicator() throws SEException;

	/**
	 * @return raw TSE core temperature value
	 * @since 3.0.0
	 */
	public abstract int getTemperature() throws SEException;

	/**
	 * @param vcc exact µSD supply voltage in mV
	 * @return TSE core temperature in °C if <code>vcc</code> was given correctly, +/- 3K
	 * @since 3.0.0
	 */
	public abstract float getTemperature(int vcc) throws SEException;

	/**
	 * @return current signature counter (last used value) for key
	 * @since 2.0
	 * @deprecated For TSE v2 use {@link #getSignatureCounter()}.
	 */ @Deprecated
	public abstract long getSignatureCounter(byte[] serialNumberKey) throws SEException;
	/**
	 * @return current signature counter (last used value)
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @since 2.4.0
	 */
	public abstract long getSignatureCounter() throws SEException;

	/**
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated
	 * @since 2.0
	 * @deprecated For TSE v2 use {@link #exportPublicKey()}.
	 */ @Deprecated
	public abstract byte[] exportPublicKey(byte[] serialNumberKey) throws SEException;
	/**
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @since 2.4.0
	 */
	public abstract byte[] exportPublicKey() throws SEException;

	/**
	 * Export TSE signing certificate file.
	 * @throws ErrorSeApiNotInitialized
	 * @since 2.4.0, not available on TSE v1.
	 */
	public abstract byte[] getCertificate() throws SEException;

	/**
	 * List of update variants.
	 *
	 * @note Only signed updates are supported.
	 */
	public enum UpdateVariants {
		signed, 			/**< UpdateTransaction returns signature */
		unsigned, 			/**< UpdateTransaction does not return a signature */
		signedAndUnsigned	/**< Both signed and unsigned are supported */
		;					//	KEEP on separate line to mollify Doxygen

		public static UpdateVariants from(byte value) throws ErrorTSEResponseDataInvalid {
			for(UpdateVariants v : values())
				if(v.ordinal() == value)
					return v;

			throw new ErrorTSEResponseDataInvalid();
		}
	};
	/**
	 * @throws ErrorGetSupportedUpdateVariantsFailed
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSeApiDeactivated TSE v1 only
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract UpdateVariants getSupportedTransactionUpdateVariants() throws SEException;

	/**
	 * Time sync variants according to spec.
	 *
	 * @note cryptovision TSE always uses {@link #unixTime}
	 */
	public enum SyncVariants {
		noInput,			/**< */
		utcTime, 			/**< UTC time */
		generalizedTime, 	/**< generalized time */
		unixTime			/**< unix time */
		;					//	KEEP on separate line to mollify Doxygen

		public static SyncVariants from(byte value) throws ErrorTSEResponseDataInvalid {
			for(SyncVariants v : values())
				if(v.ordinal() == value)
					return v;

			throw new ErrorTSEResponseDataInvalid();
		}
	};
	/**
	 * @return {@link SyncVariants#unixTime}
	 * @since 2.0
	 */
	public abstract SyncVariants getTimeSyncVariant() throws SEException;

	/**
	 * @return ASN.1 encoded signature algorithm as encoded into signed data
	 * @since 2.0
	 */
	public abstract byte[] getSignatureAlgorithm() throws SEException;

	/**
	 * @return proposed update interval for the CSP time base (number of seconds)
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSecureElementDisabled
	 * @since 2.0
	 */
	public abstract int getTimeSyncInterval() throws SEException;

	/**
	 * @return proposed update interval for the CSP time base (number of seconds)
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorSecureElementDisabled
	 * @since 0.97
	 * @deprecated see {@link #getTimeSyncInterval()}
	 */ @Deprecated
	public final int getTimeUpdateInterval() throws SEException {
		return getTimeSyncInterval();
	}

	/**
	 * @throws ErrorDeleteStoredDataFailed
	 * @throws ErrorUnexportedStoredData
	 * @throws ErrorSeApiNotInitialized
	 * @throws ErrorUserNotAuthorized
	 * @throws ErrorUserNotAuthenticated
	 * @throws ErrorSigningSystemOperationDataFailed not on TSE v1 - if system log could not be created
	 * @throws ErrorStorageFailure if deletion or (not on TSE v1) storage of system log failed
	 * @note (only) TR-03153 v1.0.1 Table 6 calls it "deleteSecuredData".
	 */
	public abstract void deleteStoredData() throws SEException;

	/**
	 * Possible authentication results
	 */
	public enum AuthenticationResult {
		ok,				/**< no error */
		failed,			/**< authentication failed */
		pinIsBlocked,	/**< the PIN is blocked */
		unknownUserId,	/**< the userId is unknown */
		error 			/**< some other error */
		;				//	KEEP on separate line to mollify Doxygen

		public static AuthenticationResult from(byte value) throws SEException {
			for(AuthenticationResult v : values())
				if(v.ordinal() == value)
					return v;

			throw new ErrorTSEResponseDataInvalid();
		}
	};
	/**
	 * {@link TSE#authenticateUser() authenticateUser()} return value
	 */
	public static class AuthenticateUserResult {
		public AuthenticationResult authenticationResult;	/**< result of the authentication */
		public short remainingRetries;						/**< remaining retries of the PIN */
	};
	/**
	 * Login user using PIN.
	 *
	 * @note   Timeouts apply: TimeAdmin is automatically logged out after 2 seconds, Admin after 10 minutes.
	 *
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract AuthenticateUserResult authenticateUser(String userId, byte[] pin) throws SEException;


	/**
	 * Log out a user.
	 *
	 * @throws ErrorUserIdNotManaged
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorUserIdNotAuthenticated -> we use ErrorUserNotAuthorized instead
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorSecureElementDisabled
	 */
	public abstract void logOut(String userId) throws SEException;

	/**
	 * {@link TSE#unblockUser() unblockUser()} return value
	 */
	public static class UnblockUserResult {
		public AuthenticationResult authenticationResult;	/**< result of the authentication */
	}

	/**
	 * @note   The new PIN **must** be different from the previous PIN!
	 *
	 * @throws ErrorUserIdNotManaged missing in BSI API
	 * @throws ErrorSigningSystemOperationDataFailed
	 * @throws ErrorRetrieveLogMessageFailed
	 * @throws ErrorStorageFailure
	 * @throws ErrorSecureElementDisabled
	 * @throws ErrorTSECommandDataInvalid
	 */
	public abstract UnblockUserResult unblockUser(String userId, byte[] puk, byte[] newPin) throws SEException;

	/**
	 * shut down the transport layer.
	 */
	public abstract void close() throws IOException, SEException;
}
