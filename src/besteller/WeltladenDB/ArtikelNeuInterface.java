package WeltladenDB;

public interface ArtikelNeuInterface {
    public void emptyTable();
    public int checkIfItemAlreadyKnown(String name, String nummer);
    public int submit();
}
