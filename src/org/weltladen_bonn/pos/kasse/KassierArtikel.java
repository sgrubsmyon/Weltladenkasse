package org.weltladen_bonn.pos.kasse;

// Basic Java stuff:
import java.util.*; // for Vector, Collections, String
import java.math.BigDecimal; // for monetary value representation and arithmetic with correct rounding

import org.weltladen_bonn.pos.BaseClass;

public class KassierArtikel {
    /**
     * Class describing one row in the JTable of Kassieren and of Rechnungen.
     */
    private BaseClass bc;

    // All attributes:
    private Integer position;
    private Integer artikel_id;
    private Integer rabatt_id;
    private String artikel_name;
    private String color;
    private String type;
    private String menge;
    private Integer stueckzahl;
    private BigDecimal einzelpreis;
    private BigDecimal ges_preis;
    private BigDecimal mwst_satz;

    public KassierArtikel(BaseClass bc) {
        this.bc = bc;
        this.position = null;
        this.artikel_id = null;
        this.rabatt_id = null;
        this.artikel_name = "";
        this.color = "default";
        this.type = "artikel";
        this.menge = "";
        this.stueckzahl = 1;
        this.einzelpreis = new BigDecimal("0.00");
        this.ges_preis = new BigDecimal("0.00");
        this.mwst_satz = new BigDecimal("0.00");
    }

    public KassierArtikel(BaseClass bc, Integer position, Integer artikel_id,
            Integer rabatt_id, String artikel_name, String color, String type,
            String menge, Integer stueckzahl, BigDecimal einzelpreis,
            BigDecimal ges_preis, BigDecimal mwst_satz) {
        this.bc = bc;
        setPosition(position);
        setArtikelID(artikel_id);
        setRabattID(rabatt_id);
        setName(artikel_name);
        setColor(color);
        setType(type);
        setMenge(menge);
        setStueckzahl(stueckzahl);
        setEinzelPreis(einzelpreis);
        setGesPreis(ges_preis);
        setMwst(mwst_satz);
    }

    /**
     * Comparators
     */
    @Override
    public boolean equals(Object o) {
        if ( !(o instanceof KassierArtikel) ){
            return false;
        } else {
            KassierArtikel a = (KassierArtikel)o;
            if ( !bc.equalsThatHandlesNull(position, a.getPosition()) ){ return false; }
            if ( !bc.equalsThatHandlesNull(artikel_id, a.getArtikelID()) ){ return false; }
            if ( !bc.equalsThatHandlesNull(rabatt_id, a.getRabattID()) ){ return false; }
            if ( !bc.equalsThatHandlesNull(artikel_name, a.getName()) ){ return false; }
            if ( !bc.equalsThatHandlesNull(color, a.getColor()) ){ return false; }
            if ( !bc.equalsThatHandlesNull(type, a.getType()) ){ return false; }
            if ( !bc.equalsThatHandlesNull(mwst_satz, a.getMwst()) ){ return false; }
        }
        return true;
    }

    public boolean equalsInAttribute(String attr, KassierArtikel a) {
        return bc.equalsThatHandlesNull(get(attr), a.get(attr));
    }

    /**
     * Getters
     */
    public Object get(String objName) {
        if ( objName.equals("position") )
            return getPosition();
        if ( objName.equals("artikel_id") )
            return getArtikelID();
        if ( objName.equals("rabatt_id") )
            return getRabattID();
        if ( objName.equals("name") )
            return getName();
        if ( objName.equals("color") )
            return getColor();
        if ( objName.equals("type") )
            return getType();
        if ( objName.equals("menge") )
            return getMenge();
        if ( objName.equals("stueckzahl") )
            return getStueckzahl();
        if ( objName.equals("einzelpreis") )
            return getEinzelPreis();
        if ( objName.equals("ges_preis") )
            return getGesPreis();
        if ( objName.equals("mwst") )
            return getMwst();
        return null;
    }

    public Integer getPosition() {
        return position;
    }

    public Integer getArtikelID() {
        return artikel_id;
    }

    public Integer getRabattID() {
        return rabatt_id;
    }

    public String getName() {
        return artikel_name;
    }

    public String getColor() {
        return color;
    }

    public String getType() {
        return type;
    }

    public String getMenge() {
        return menge;
    }

    public Integer getStueckzahl() {
        return stueckzahl;
    }

    public BigDecimal getEinzelPreis() {
        return einzelpreis;
    }

    public BigDecimal getGesPreis() {
        return ges_preis;
    }

    public BigDecimal getMwst() {
        return mwst_satz;
    }


    /**
     * Setters
     */
    public void setPosition(Integer position) {
        this.position = position;
    }

    public void setArtikelID(Integer artikel_id) {
        this.artikel_id = artikel_id;
    }

    public void setRabattID(Integer rabatt_id) {
        this.rabatt_id = rabatt_id;
    }

    public void setName(String name) {
        if (artikel_name == null)
            this.artikel_name = "";
        else
            this.artikel_name = name;
    }

    public void setColor(String color) {
        if (color == null)
            this.color = "default";
        else
            this.color = color;
    }

    public void setType(String type) {
        if (type == null)
            this.type = "artikel";
        else
            this.type = type;
    }

    public void setMenge(String menge) {
        if (menge == null)
            this.menge = "";
        else
            this.menge = menge;
    }

    public void setStueckzahl(Integer stueckzahl) {
        if (stueckzahl == null)
            this.stueckzahl = 1;
        else
            this.stueckzahl = stueckzahl;
    }

    public void setEinzelPreis(BigDecimal einzelpreis) {
        if (einzelpreis == null)
            this.einzelpreis = new BigDecimal("0.00");
        else
            this.einzelpreis = einzelpreis;
    }

    public void setGesPreis(BigDecimal ges_preis) {
        if (ges_preis == null)
            this.ges_preis = new BigDecimal("0.00");
        else
            this.ges_preis = ges_preis;
    }

    public void setMwst(BigDecimal mwst_satz) {
        if (mwst_satz == null)
            this.mwst_satz = new BigDecimal("0.00");
        else
            this.mwst_satz = mwst_satz;
    }
}
