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

    public static byte[] hexToBytes(String hex)
    {
        hex = hex.replaceAll("[^A-Fa-f0-9]", "");
        if (1 == hex.length() % 2)
            hex = "0" + hex;
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = (byte)((Character.digit(hex.charAt(i*2), 16) << 4) + Character.digit(hex.charAt(i*2+1), 16));
        return bytes;
    }
 
    public static byte[] exportCertificate(TSE tse, byte[] serialNumberKey) throws SEException {
        int len = 2 * serialNumberKey.length;
        ArrayList<TSETarFile> tarList = exportCertificatesAsList(tse);
        TSETarFile file;
        for (int i = 0; i < tarList.size(); i++) {
            file = tarList.get(i);
            System.out.println("hexToBytes:" + hexToBytes(file.name.substring(0, len)));
            System.out.println("hexStringToByteArray:" + WeltladenTSE.hexStringToByteArray(file.name.substring(0, len)));
            if (
                len < file.name.length() &&
                Arrays.equals(serialNumberKey, hexToBytes(file.name.substring(0, len)))
            ) return file.value;
        }
        throw new com.cryptovision.SEAPI.exceptions.ErrorTSECommandDataInvalid();
    }
}