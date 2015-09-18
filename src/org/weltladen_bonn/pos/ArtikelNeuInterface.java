package org.weltladen_bonn.pos;

public interface ArtikelNeuInterface {
    public void emptyTable();
    public int checkIfArticleAlreadyKnown(Integer lief_id, String nummer);
    public int submit();
}
