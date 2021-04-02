package org.weltladen_bonn.pos.kasse;

import java.util.Arrays;
import java.util.ArrayList;

import com.cryptovision.SEAPI.TSE;
import com.cryptovision.SEAPI.exceptions.SEException;

import org.bouncycastle.util.encoders.Hex;

/** https://tse-support.cryptovision.com/confluence/display/TDI/Die+Felder+TSE_ZERTIFIKAT_I+und+II+der+DSFinV-K */
public class TSEUntar {
 
    public static ArrayList<TSETarFile> untar(byte[] tarArchive) {
        final int TAR_BLOCK_SIZE = 512;
        final int TAR_NAME_OFFSET = 0;
        final int TAR_NAME_LENGTH = 100;
        final int TAR_SIZE_OFFSET = 124;
        final int TAR_SIZE_LENGTH = 12;
        final int TAR_TIME_OFFSET = 136;
        final int TAR_TIME_LENGTH = 12;
        ArrayList<TSETarFile> tarList = new ArrayList<TSETarFile>();
        TSETarFile tarFile;
        int len;
        int pos = 0;
        while ((pos + 2 * TAR_BLOCK_SIZE) < tarArchive.length)
        {
            len = Integer.parseUnsignedInt((new String(tarArchive, pos + TAR_SIZE_OFFSET, TAR_SIZE_LENGTH)).replace("\0", ""),8);
            if (pos + TAR_BLOCK_SIZE + len > tarArchive.length)
                break;
            tarFile = new TSETarFile();
            tarFile.name = (new String(tarArchive, pos + TAR_NAME_OFFSET, TAR_NAME_LENGTH)).replace("\0", "");
            tarFile.time = Long.parseUnsignedLong((new String(tarArchive, pos + TAR_TIME_OFFSET, TAR_TIME_LENGTH)).replace("\0", ""),8);
            tarFile.value = Arrays.copyOfRange(tarArchive,pos + TAR_BLOCK_SIZE,pos + TAR_BLOCK_SIZE + len);
            tarList.add(tarFile);
            pos += TAR_BLOCK_SIZE * (1 + (len + TAR_BLOCK_SIZE - 1) / TAR_BLOCK_SIZE);
        }
        return tarList;
    }

    public static ArrayList<TSETarFile> exportCertificatesAsList(TSE tse) throws SEException {
        return untar(tse.exportCertificates());
    }
 
    public static byte[] exportCertificate(TSE tse, byte[] serialNumberKey) throws SEException {
        int len = 2 * serialNumberKey.length;
        ArrayList<TSETarFile> tarList = exportCertificatesAsList(tse);
        TSETarFile file;
        for (int i = 0; i < tarList.size(); i++) {
            file = tarList.get(i);
            if (
                len < file.name.length() &&
                Arrays.equals(serialNumberKey, WeltladenTSE.hexStringToByteArray(file.name.substring(0, len)))
            ) return file.value;
        }
        throw new com.cryptovision.SEAPI.exceptions.ErrorTSECommandDataInvalid();
    }

    public static String extractFinishTransactionAsASN1(byte[] tx) {
        String result = "";
        ArrayList<TSETarFile> tarList = untar(tx);
        for (TSETarFile file : tarList) {
            if (file.name.contains("_Finish_")) {
                // System.out.println(file.name + ":");
                // System.out.println(file.time + ":");
                result = WeltladenTSE.byteArrayToASN1String(file.value);

                // Nun muss noch die Signatur aus ASN.1 extrahiert und von Hex in Base64 umgewandelt werden.
                // Zur Entschlüsselung des ASN1Dumps: (https://tse-support.cryptovision.com/jira/servicedesk/customer/portal/5/TDI-613)

                // 3081F7
                // 0201 02
                //     (version 2)
                // 0609 04007F000703070101
                //     (certifiedDataType=id-SE-API-transaction-log)
                // 8011 46696E6973685472616E73616374696F6E
                //     (operationType="FinishTransaction")
                // 811B 50727A656D6F277320416E64726F69646B617373652053756E4D69
                //     (clientId="DemokasseBundesdruckerei-01")
                // 8227 44656D6F6B6173736542756E64673647275636B657265692D3031
                //     (processData="Beleg^0.00_2.00_0.00_0.00_0.00^2.00:Bar")
                // 830E 4B617373656E62656C65672D5631
                //     (processType="Kassenbeleg-V1")
                // 8502 0080
                //     (transactionNumber=128)
                // 0420 4A3F03A2DEC81878B432548668F603D14F7B7F90D230E30C87C1A705DCE1C890
                //     (serialNumber=Sj8Dot7IGHi0MlSGaPYD0U97f5DSMOMMh8GnBdzhyJA=)
                // 300C (signatureAlgorithm)
                //     060A 04007F00070101040103
                //         (algorithm=ecdsa-plain-SHA256)
                // 0202 037E
                //     (signatureCounter=894)
                // 0204 5EF9A440
                //     ([logTime] unixTime=2020-06-29T08:20:16.000Z)
                // 0440 6B6E751EC018683AF74C5B587C03FC54BC948F5EF3B111DB7D64EF93BEDAF7B5
                //         AC9C3089820470CF04A3292679000F2B069551195CFEA909D0DBE39AE32753E8
                //     (signatureValue=a251HsAYaDr3TFtYfAP8VLyUj17zsRHbfWTvk77a97WsnDCJggRwzwSjKSZ5AA8rBpVRGVz+qQnQ2+Oa4ydT6A==)

                // Die Definition hierzu ist in der TR-03151 (https://www.bsi.bund.de/DE/Publikationen/TechnischeRichtlinien/tr03151/tr03151_node.html)
                // ab Seite 10 (2 Log messages and their creation).
                // Die übergreifende Struktur ist in Table 2, und für Transaction Logs in Tabelle 4 festgelegt.
            }
        }
        return result;
    }

    public static String extractStartTransactionAsASN1(byte[] tx) {
        String result = "";
        ArrayList<TSETarFile> tarList = untar(tx);
        for (TSETarFile file : tarList) {
            if (file.name.contains("_Start_")) {
                // System.out.println(file.name + ":");
                // System.out.println(file.time + ":");
                result = WeltladenTSE.byteArrayToASN1String(file.value);
            }
        }
        return result;
    }

}