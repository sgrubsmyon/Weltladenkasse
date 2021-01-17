DROP DATABASE IF EXISTS kasse;
CREATE DATABASE kasse CHARACTER SET utf8 COLLATE utf8_general_ci;

USE kasse;

CREATE TABLE lieferant (
    lieferant_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    lieferant_name VARCHAR(50) NOT NULL,
    lieferant_kurzname VARCHAR(10) DEFAULT NULL,
    n_artikel INTEGER(10) UNSIGNED DEFAULT NULL,
    aktiv BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (lieferant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE mwst (
    mwst_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    mwst_satz DECIMAL(6,5) NOT NULL,
    PRIMARY KEY (mwst_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE pfand (
    pfand_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    artikel_id INTEGER(10) UNSIGNED NOT NULL,
    PRIMARY KEY (pfand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE produktgruppe (
    produktgruppen_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    toplevel_id INTEGER(10) UNSIGNED DEFAULT 1,
    sub_id INTEGER(10) UNSIGNED DEFAULT NULL,
    subsub_id INTEGER(10) UNSIGNED DEFAULT NULL,
    produktgruppen_name VARCHAR(50) NOT NULL,
    mwst_id INTEGER(10) UNSIGNED DEFAULT NULL,
    pfand_id INTEGER(10) UNSIGNED DEFAULT NULL,
    std_einheit VARCHAR(10) DEFAULT NULL,
    n_artikel INTEGER(10) UNSIGNED DEFAULT NULL,
    n_artikel_rekursiv INTEGER(10) UNSIGNED DEFAULT NULL,
    aktiv BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (produktgruppen_id),
    FOREIGN KEY (mwst_id) REFERENCES mwst(mwst_id),
    FOREIGN KEY (pfand_id) REFERENCES pfand(pfand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE artikel (
    artikel_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    produktgruppen_id INTEGER(10) UNSIGNED NOT NULL DEFAULT 8,
    lieferant_id INTEGER(10) UNSIGNED NOT NULL DEFAULT 1,
    artikel_nr VARCHAR(30) NOT NULL,
    artikel_name VARCHAR(180) NOT NULL,
    kurzname VARCHAR(50) DEFAULT NULL,
    menge DECIMAL(8,5) DEFAULT NULL,
    einheit VARCHAR(10) DEFAULT "kg",
    barcode VARCHAR(30) DEFAULT NULL,
    herkunft VARCHAR(100) DEFAULT NULL,
    vpe SMALLINT(5) UNSIGNED DEFAULT NULL,
    setgroesse SMALLINT(5) UNSIGNED NOT NULL DEFAULT 1,
    vk_preis DECIMAL(13,2) DEFAULT NULL,
    empf_vk_preis DECIMAL(13,2) DEFAULT NULL,
    ek_rabatt DECIMAL(6,5) DEFAULT NULL,
    ek_preis DECIMAL(13,2) DEFAULT NULL,
    variabler_preis BOOLEAN NOT NULL DEFAULT FALSE,
    sortiment BOOLEAN NOT NULL DEFAULT FALSE,
    lieferbar BOOLEAN NOT NULL DEFAULT FALSE,
    beliebtheit TINYINT(1) NOT NULL DEFAULT 0,
    bestand SMALLINT(5) DEFAULT NULL,
    von DATETIME DEFAULT NULL,
    bis DATETIME DEFAULT NULL,
    aktiv BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (artikel_id),
    FOREIGN KEY (lieferant_id) REFERENCES lieferant(lieferant_id),
    FOREIGN KEY (produktgruppen_id) REFERENCES produktgruppe(produktgruppen_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
ALTER TABLE pfand ADD FOREIGN KEY (artikel_id) REFERENCES artikel(artikel_id);

CREATE TABLE rabattaktion (
    rabatt_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    aktionsname VARCHAR(50) DEFAULT NULL,
    rabatt_relativ DECIMAL(6,5) DEFAULT NULL,
    rabatt_absolut DECIMAL(13,2) DEFAULT NULL,
    mengenrabatt_schwelle INTEGER(10) UNSIGNED DEFAULT NULL,
    mengenrabatt_anzahl_kostenlos INTEGER(10) UNSIGNED DEFAULT NULL,
    mengenrabatt_relativ DECIMAL(6,5) DEFAULT NULL,
    von DATETIME NOT NULL,
    bis DATETIME,
    produktgruppen_id INTEGER(10) UNSIGNED DEFAULT NULL,
    artikel_id INTEGER(10) UNSIGNED DEFAULT NULL,
    PRIMARY KEY (rabatt_id),
    FOREIGN KEY (produktgruppen_id) REFERENCES produktgruppe(produktgruppen_id),
    FOREIGN KEY (artikel_id) REFERENCES artikel(artikel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE verkauf (
    rechnungs_nr INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    verkaufsdatum DATETIME NOT NULL,
    storniert BOOLEAN NOT NULL DEFAULT FALSE,
    ec_zahlung BOOLEAN NOT NULL DEFAULT FALSE,
    kunde_gibt DECIMAL(13,2) DEFAULT NULL,
    PRIMARY KEY (rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE verkauf_mwst (
    rechnungs_nr INTEGER(10) UNSIGNED NOT NULL,
    mwst_satz DECIMAL(6,5) NOT NULL,
    mwst_netto DECIMAL(13,2) NOT NULL,
    mwst_betrag DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (rechnungs_nr, mwst_satz),
    FOREIGN KEY (rechnungs_nr) REFERENCES verkauf(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE verkauf_details (
    vd_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    rechnungs_nr INTEGER(10) UNSIGNED NOT NULL,
    position SMALLINT(5) UNSIGNED DEFAULT NULL,
    artikel_id INTEGER(10) UNSIGNED DEFAULT NULL,
    rabatt_id INTEGER(10) UNSIGNED DEFAULT NULL,
    stueckzahl SMALLINT(5) NOT NULL DEFAULT 1,
    ges_preis DECIMAL(13,2) NOT NULL,
    mwst_satz DECIMAL(6,5) NOT NULL,
    PRIMARY KEY (vd_id),
    FOREIGN KEY (rechnungs_nr) REFERENCES verkauf(rechnungs_nr),
    FOREIGN KEY (artikel_id) REFERENCES artikel(artikel_id),
    FOREIGN KEY (rabatt_id) REFERENCES rabattaktion(rabatt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE kassenstand (
    kassenstand_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    buchungsdatum DATETIME NOT NULL,
    neuer_kassenstand DECIMAL(13,2) NOT NULL,
    manuell BOOLEAN NOT NULL DEFAULT FALSE,
    entnahme BOOLEAN NOT NULL DEFAULT FALSE,
    rechnungs_nr INTEGER(10) UNSIGNED DEFAULT NULL,
    kommentar VARCHAR(70),
    PRIMARY KEY (kassenstand_id),
    FOREIGN KEY (rechnungs_nr) REFERENCES verkauf(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- CREATE TABLE abrechnung_tag (
--     id INTEGER(10) UNSIGNED NOT NULL,
--     zeitpunkt DATETIME NOT NULL,
--     zeitpunkt_real DATETIME NOT NULL,
--     mwst_satz DECIMAL(6,5) NOT NULL,
--     mwst_netto DECIMAL(13,2) NOT NULL,
--     mwst_betrag DECIMAL(13,2) NOT NULL,
--     bar_brutto DECIMAL(13,2) NOT NULL,
--     kassenstand_id INTEGER(10) UNSIGNED DEFAULT NULL,
--     PRIMARY KEY (id, mwst_satz),
--     FOREIGN KEY (kassenstand_id) REFERENCES kassenstand(kassenstand_id)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE abrechnung_tag (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    zeitpunkt DATETIME NOT NULL,
    zeitpunkt_real DATETIME NOT NULL,
    kassenstand_id INTEGER(10) UNSIGNED DEFAULT NULL,
    rechnungs_nr_von INTEGER(10) UNSIGNED NOT NULL,
    rechnungs_nr_bis INTEGER(10) UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (kassenstand_id) REFERENCES kassenstand(kassenstand_id),
    FOREIGN KEY (rechnungs_nr_von) REFERENCES verkauf(rechnungs_nr),
    FOREIGN KEY (rechnungs_nr_bis) REFERENCES verkauf(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE abrechnung_tag_mwst (
    id INTEGER(10) UNSIGNED NOT NULL,
    mwst_satz DECIMAL(6,5) NOT NULL,
    mwst_netto DECIMAL(13,2) NOT NULL,
    mwst_betrag DECIMAL(13,2) NOT NULL,
    bar_brutto DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (id, mwst_satz)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE zaehlprotokoll (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    abrechnung_tag_id INTEGER(10) UNSIGNED NOT NULL,
    zeitpunkt DATETIME NOT NULL,
    kommentar TEXT NOT NULL,
    aktiv BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    FOREIGN KEY (abrechnung_tag_id) REFERENCES abrechnung_tag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE zaehlprotokoll_details (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    zaehlprotokoll_id INTEGER(10) UNSIGNED NOT NULL,
    anzahl SMALLINT(5) UNSIGNED NOT NULL,
    einheit DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (zaehlprotokoll_id) REFERENCES zaehlprotokoll(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- CREATE TABLE abrechnung_monat (
--     id INTEGER(10) UNSIGNED NOT NULL,
--     monat DATE NOT NULL,
--     mwst_satz DECIMAL(6,5) NOT NULL,
--     mwst_netto DECIMAL(13,2) NOT NULL,
--     mwst_betrag DECIMAL(13,2) NOT NULL,
--     bar_brutto DECIMAL(13,2) NOT NULL,
--     PRIMARY KEY (id, mwst_satz)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE abrechnung_monat (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    monat DATE NOT NULL,
    abrechnung_tag_id_von INTEGER(10) UNSIGNED NOT NULL,
    abrechnung_tag_id_bis INTEGER(10) UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (abrechnung_tag_id_von) REFERENCES abrechnung_tag(id),
    FOREIGN KEY (abrechnung_tag_id_bis) REFERENCES abrechnung_tag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE abrechnung_monat_mwst (
    id INTEGER(10) UNSIGNED NOT NULL,
    mwst_satz DECIMAL(6,5) NOT NULL,
    mwst_netto DECIMAL(13,2) NOT NULL,
    mwst_betrag DECIMAL(13,2) NOT NULL,
    bar_brutto DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (id, mwst_satz)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- CREATE TABLE abrechnung_jahr (
--     id INTEGER(10) UNSIGNED NOT NULL,
--     jahr YEAR NOT NULL,
--     mwst_satz DECIMAL(6,5) NOT NULL,
--     mwst_netto DECIMAL(13,2) NOT NULL,
--     mwst_betrag DECIMAL(13,2) NOT NULL,
--     bar_brutto DECIMAL(13,2) NOT NULL,
--     PRIMARY KEY (id, mwst_satz)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE abrechnung_jahr (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    jahr YEAR NOT NULL,
    abrechnung_tag_id_von INTEGER(10) UNSIGNED NOT NULL,
    abrechnung_tag_id_bis INTEGER(10) UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (abrechnung_tag_id_von) REFERENCES abrechnung_tag(id),
    FOREIGN KEY (abrechnung_tag_id_bis) REFERENCES abrechnung_tag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE abrechnung_jahr_mwst (
    id INTEGER(10) UNSIGNED NOT NULL,
    mwst_satz DECIMAL(6,5) NOT NULL,
    mwst_netto DECIMAL(13,2) NOT NULL,
    mwst_betrag DECIMAL(13,2) NOT NULL,
    bar_brutto DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (id, mwst_satz)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE bestellung (
    bestell_nr INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    typ VARCHAR(12) NOT NULL DEFAULT "",
    bestell_datum DATETIME NOT NULL,
    jahr YEAR NOT NULL DEFAULT 2000,
    kw TINYINT(2) UNSIGNED NOT NULL DEFAULT 1,
    PRIMARY KEY (bestell_nr, typ)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE bestellung_details (
    bestell_nr INTEGER(10) UNSIGNED NOT NULL,
    typ VARCHAR(12) NOT NULL DEFAULT "",
    position SMALLINT(5) UNSIGNED NOT NULL,
    artikel_id INTEGER(10) UNSIGNED NOT NULL,
    stueckzahl SMALLINT(5) NOT NULL DEFAULT 1,
    PRIMARY KEY (bestell_nr, typ, position),
    FOREIGN KEY (bestell_nr) REFERENCES bestellung(bestell_nr),
    FOREIGN KEY (artikel_id) REFERENCES artikel(artikel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE tse_transaction (
    transaction_number INTEGER(10) UNSIGNED NOT NULL,
    rechnungs_nr INTEGER(10) UNSIGNED DEFAULT NULL,
    transaction_start CHAR(29) DEFAULT NULL,
    transaction_end CHAR(29) DEFAULT NULL,
    process_type VARCHAR(30) DEFAULT NULL,
    signature_counter INTEGER(10) UNSIGNED DEFAULT NULL,
    signature_base64 VARCHAR(512) DEFAULT NULL,
    tse_error VARCHAR(200) DEFAULT NULL,
    process_data VARCHAR(60) DEFAULT NULL,
    PRIMARY KEY (transaction_number),
    FOREIGN KEY (rechnungs_nr) REFERENCES verkauf(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- tx start and end times are prepared in format '2021-01-17T19:13:45.000+01:00' in Java,
--     so they have a fixed length of 29 chars
-- process_type will probably only ever need 14 chars (e.g. initially Kassenbeleg-V1, then Kassenbeleg-V2, ...)
--     but in the specs, it has (up to) 30 characters, so better make sure it works
-- Example signatur in Base64:
--     XLBGMLidNi+vICwIAPO4B/PyDgSna+5EluXgchQOdNvNYfxcdJS/32mnmars0BxM+Dwk4W3kYaCavU7IEmPnIQ==
-- so 88 chars, but let's stick to specs where it says up to 512 chars
-- Example process_data with absurdly large amounts:
--         Beleg^2190.00_1795.00_0.00_0.00_0.00^3985.00:Unbar
--         AVBelegabbruch^0.00_0.00_0.00_0.00_0.00^
--     Just to make sure, let's assume each tax class has a sum with 4 digits:
--         Beleg^9999.00_9999.00_9999.00_9999.00_9999.00^49995.00:Unbar
--     Even this extreme case would fit into a 60 chars long field

SOURCE grants.sql;
