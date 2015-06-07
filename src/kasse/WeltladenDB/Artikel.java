package WeltladenDB;

// Basic Java stuff:
import java.util.*; // for Vector, Collections, String
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

public class Artikel {
    /**
     * Class describing an article.
     */
    private BaseClass bc = new BaseClass();

    // All attributes:
    private Integer produktgruppen_id;
    private Integer lieferant_id;
    private String artikel_nr;
    private String artikel_name;
    private String kurzname;
    private BigDecimal menge;
    private String einheit;
    private String barcode;
    private String herkunft;
    private Integer vpe;
    private Integer setgroesse;
    private String vk_preis;
    private String empf_vk_preis;
    private String ek_rabatt;
    private String ek_preis;
    private Boolean variabler_preis;
    private Boolean sortiment;
    private Boolean lieferbar;
    private Integer beliebtheit;
    private Integer bestand;
    private Boolean aktiv;

    public Artikel() {
        produktgruppen_id = 8;
        lieferant_id = 1;
        artikel_nr = "";
        artikel_name = "";
        kurzname = null;
        menge = null;
        einheit = "kg";
        barcode = null;
        herkunft = null;
        vpe = null;
        setgroesse = 1;
        vk_preis = null;
        empf_vk_preis = null;
        ek_rabatt = null;
        ek_preis = null;
        variabler_preis = false;
        sortiment = false;
        lieferbar = false;
        beliebtheit = 2;
        bestand = null;
        aktiv = true;
    }

    public Artikel(Integer prodGrID, Integer liefID, String nummer,
            String name, String kname, BigDecimal menge_, String einh,
            String bcode, String herk, Integer vpe_, Integer setgr,
            String vkpreis, String empfvkpreis, String ekrabatt,
            String ekpreis, Boolean varpreis, Boolean sortim,
            Boolean liefbar, Integer beliebt, Integer bestand_,
            Boolean aktiv_) {
        setProdGrID(prodGrID);
        setLiefID(liefID);
        setNummer(nummer);
        setName(name);
        setKurzname(kname);
        setMenge(menge_);
        setEinheit(einh);
        setBarcode(bcode);
        setHerkunft(herk);
        setVPE(vpe_);
        setSetgroesse(setgr);
        setVKP(vkpreis);
        setEmpfVKP(empfvkpreis);
        setEKRabatt(ekrabatt);
        setEKP(ekpreis);
        setVarPreis(varpreis);
        setSortiment(sortim);
        setLieferbar(liefbar);
        setBeliebt(beliebt);
        setBestand(bestand_);
        setAktiv(aktiv_);
    }

    /**
     * Comparators
     */
    private boolean equalsThatHandlesNull(Object a, Object b) {
        System.out.println(a+" "+b);
        if ( (a != null) && (b != null) ){
            if ( a.equals(b) ){ return true; }
        } else {
            if ( (a == null) && (b == null) ){ return true; }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if ( !(o instanceof Artikel) ){
            return false;
        } else {
            Artikel a = (Artikel)o;
            System.out.println("prodGrID");
            if ( !equalsThatHandlesNull(produktgruppen_id, a.getProdGrID()) ){ return false; }
            System.out.println("liefID");
            if ( !equalsThatHandlesNull(lieferant_id, a.getLiefID()) ){ return false; }
            System.out.println("nummer");
            if ( !equalsThatHandlesNull(artikel_nr, a.getNummer()) ){ return false; }
            System.out.println("name");
            if ( !equalsThatHandlesNull(artikel_name, a.getName()) ){ return false; }
            System.out.println("kname");
            if ( !equalsThatHandlesNull(kurzname, a.getKurzname()) ){ return false; }
            System.out.println("menge");
            if ( !equalsThatHandlesNull(menge, a.getMenge()) ){ return false; }
            System.out.println("einheit");
            if ( !equalsThatHandlesNull(einheit, a.getEinheit()) ){ return false; }
            System.out.println("barcode");
            if ( !equalsThatHandlesNull(barcode, a.getBarcode()) ){ return false; }
            System.out.println("herkunft");
            if ( !equalsThatHandlesNull(herkunft, a.getHerkunft()) ){ return false; }
            System.out.println("vpe");
            if ( !equalsThatHandlesNull(vpe, a.getVPE()) ){ return false; }
            System.out.println("setgr");
            if ( !equalsThatHandlesNull(setgroesse, a.getSetgroesse()) ){ return false; }
            System.out.println("vkp");
            if ( !equalsThatHandlesNull(vk_preis, a.getVKP()) ){ return false; }
            System.out.println("evkp");
            if ( !equalsThatHandlesNull(empf_vk_preis, a.getEmpfVKP()) ){ return false; }
            System.out.println("ekr");
            if ( !equalsThatHandlesNull(ek_rabatt, a.getEKRabatt()) ){ return false; }
            System.out.println("ekp");
            if ( !equalsThatHandlesNull(ek_preis, a.getEKP()) ){ return false; }
            System.out.println("var");
            if ( !equalsThatHandlesNull(variabler_preis, a.getVarPreis()) ){ return false; }
            System.out.println("sort");
            if ( !equalsThatHandlesNull(sortiment, a.getSortiment()) ){ return false; }
            System.out.println("lief");
            if ( !equalsThatHandlesNull(lieferbar, a.getLieferbar()) ){ return false; }
            System.out.println("beliebt");
            if ( !equalsThatHandlesNull(beliebtheit, a.getBeliebt()) ){ return false; }
            System.out.println("bestand");
            if ( !equalsThatHandlesNull(bestand, a.getBestand()) ){ return false; }
            System.out.println("aktiv");
            if ( !equalsThatHandlesNull(aktiv, a.getAktiv()) ){ return false; }
        }
        return true;
    }

    public boolean equalsInAttribute(String attr, Artikel a) {
        return equalsThatHandlesNull(get(attr), a.get(attr));
    }

    /**
     * Getters
     */
    public Object get(String objName) {
        if ( objName.equals("prodGrID") )
            return getProdGrID();
        if ( objName.equals("liefID") )
            return getLiefID();
        if ( objName.equals("nummer") )
            return getNummer();
        if ( objName.equals("name") )
            return getName();
        if ( objName.equals("kurzname") )
            return getKurzname();
        if ( objName.equals("menge") )
            return getMenge();
        if ( objName.equals("einheit") )
            return getEinheit();
        if ( objName.equals("barcode") )
            return getBarcode();
        if ( objName.equals("herkunft") )
            return getHerkunft();
        if ( objName.equals("vpe") )
            return getVPE();
        if ( objName.equals("setgroesse") )
            return getSetgroesse();
        if ( objName.equals("vkp") )
            return getVKP();
        if ( objName.equals("empfVKP") )
            return getEmpfVKP();
        if ( objName.equals("ekRabatt") )
            return getEKRabatt();
        if ( objName.equals("ekp") )
            return getEKP();
        if ( objName.equals("varPreis") )
            return getVarPreis();
        if ( objName.equals("sortiment") )
            return getSortiment();
        if ( objName.equals("lieferbar") )
            return getLieferbar();
        if ( objName.equals("beliebt") )
            return getBeliebt();
        if ( objName.equals("bestand") )
            return getBestand();
        if ( objName.equals("aktiv") )
            return getAktiv();
        return null;
    }

    public Integer getProdGrID() {
        return produktgruppen_id;
    }

    public Integer getLiefID() {
        return lieferant_id;
    }

    public String getNummer() {
        return artikel_nr;
    }

    public String getName() {
        return artikel_name;
    }

    public String getKurzname() {
        return kurzname;
    }

    public BigDecimal getMenge() {
        return menge;
    }

    public String getEinheit() {
        return einheit;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getHerkunft() {
        return herkunft;
    }

    public Integer getVPE() {
        return vpe;
    }

    public Integer getSetgroesse() {
        return setgroesse;
    }

    public String getVKP() {
        return vk_preis;
    }

    public String getEmpfVKP() {
        return empf_vk_preis;
    }

    public String getEKRabatt() {
        return ek_rabatt;
    }

    public String getEKP() {
        return ek_preis;
    }

    public Boolean getVarPreis() {
        return variabler_preis;
    }

    public Boolean getSortiment() {
        return sortiment;
    }

    public Boolean getLieferbar() {
        return lieferbar;
    }

    public Integer getBeliebt() {
        return beliebtheit;
    }

    public Integer getBestand() {
        return bestand;
    }

    public Boolean getAktiv() {
        return aktiv;
    }

    /**
     * Setters
     */
    public void setProdGrID(Integer prodGrID) {
        produktgruppen_id = prodGrID;
    }

    public void setLiefID(Integer liefID) {
        lieferant_id = liefID;
    }

    public void setNummer(String nummer) {
        artikel_nr = nummer;
    }

    public void setName(String name) {
        artikel_name = name;
    }

    public void setKurzname(String kname) {
        kurzname = kname;
    }

    public void setMenge(BigDecimal menge_) {
        if (menge_ == null)
            menge = null;
        else
            menge = new BigDecimal( bc.unifyDecimalIntern(menge_) );
    }

    public void setEinheit(String einh) {
        einheit = einh;
    }

    public void setBarcode(String bcode) {
        barcode = bcode;
    }

    public void setHerkunft(String herk) {
        herkunft = herk;
    }

    public void setVPE(Integer vpe_) {
        vpe = vpe_;
    }

    public void setSetgroesse(Integer setgr) {
        setgroesse = setgr;
    }

    public void setVKP(String vkp) {
        if (vkp == null)
            vk_preis = "";
        else
            vk_preis = bc.priceFormatterIntern(vkp);
    }

    public void setEmpfVKP(String evkp) {
        if (evkp == null)
            evkp = "";
        else
            empf_vk_preis = bc.priceFormatterIntern(evkp);
    }

    public void setEKRabatt(String ekrabatt) {
        if (ekrabatt == null)
            ek_rabatt = "";
        else
            ek_rabatt = bc.vatFormatter( bc.vatParser(ekrabatt) );
    }

    public void setEKP(String ekp) {
        if (ekp == null)
            ek_preis = "";
        else
            ek_preis = bc.priceFormatterIntern(ekp);
    }

    public void setVarPreis(Boolean varpreis) {
        variabler_preis = varpreis;
    }

    public void setSortiment(Boolean sortim) {
        sortiment = sortim;
    }

    public void setLieferbar(Boolean liefbar) {
        lieferbar = liefbar;
    }

    public void setBeliebt(Integer beliebt) {
        beliebtheit = beliebt;
    }

    public void setBestand(Integer bestand_) {
        bestand = bestand_;
    }

    public void setAktiv(Boolean aktiv_) {
        aktiv = aktiv_;
    }
}
