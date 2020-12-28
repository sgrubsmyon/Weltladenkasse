package org.weltladen_bonn.pos.kasse;

import java.util.Arrays;
import java.util.ArrayList;

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

}