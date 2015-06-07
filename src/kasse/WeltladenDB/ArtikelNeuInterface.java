package WeltladenDB;

public interface ArtikelNeuInterface {
    public void emptyTable();
    public int checkIfItemAlreadyKnown(Integer lief_id, String nummer);
    public int submit();
}
