DROP DATABASE IF EXISTS kasse;
CREATE DATABASE kasse CHARACTER SET utf8 COLLATE utf8_unicode_ci;

USE kasse;

CREATE TABLE lieferant (
    lieferant_id int(10) unsigned NOT NULL AUTO_INCREMENT,
    lieferant_name varchar(50) NOT NULL,
    aktiv BOOL NOT NULL DEFAULT TRUE,
    PRIMARY KEY (lieferant_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE mwst (
    mwst_id int(10) unsigned NOT NULL AUTO_INCREMENT,
    mwst_satz decimal(6,5) NOT NULL,
    PRIMARY KEY (mwst_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE pfand (
    pfand_id int(10) unsigned NOT NULL AUTO_INCREMENT,
    artikel_id int(10) unsigned NOT NULL,
    PRIMARY KEY (pfand_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE produktgruppe (
    produktgruppen_id int(10) unsigned NOT NULL AUTO_INCREMENT,
    toplevel_id int(10) unsigned DEFAULT 1,
    sub_id int(10) unsigned DEFAULT NULL,
    subsub_id int(10) unsigned DEFAULT NULL,
    produktgruppen_name varchar(50) NOT NULL,
    mwst_id int(10) unsigned DEFAULT NULL,
    pfand_id int(10) unsigned DEFAULT NULL,
    aktiv BOOL NOT NULL DEFAULT TRUE,
    PRIMARY KEY (produktgruppen_id),
    FOREIGN KEY (mwst_id) REFERENCES mwst(mwst_id),
    FOREIGN KEY (pfand_id) REFERENCES pfand(pfand_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE artikel (
    artikel_id int(10) unsigned NOT NULL AUTO_INCREMENT,
    artikel_name varchar(160) NOT NULL,
    artikel_nr varchar(30) NOT NULL,
    barcode varchar(30) DEFAULT NULL,
    vk_preis decimal(13,2),
    ek_preis decimal(13,2),
    lieferant_id int(10) unsigned DEFAULT 1,
    produktgruppen_id int(10) unsigned NOT NULL DEFAULT 8,
    herkunft varchar(100) DEFAULT NULL,
    von DATETIME DEFAULT NULL,
    bis DATETIME DEFAULT NULL,
    aktiv BOOL NOT NULL DEFAULT TRUE,
    variabler_preis BOOL NOT NULL DEFAULT FALSE,
    vpe int(10) unsigned DEFAULT NULL,
    bestand int DEFAULT NULL,
    PRIMARY KEY (artikel_id),
    FOREIGN KEY (lieferant_id) REFERENCES lieferant(lieferant_id),
    FOREIGN KEY (produktgruppen_id) REFERENCES produktgruppe(produktgruppen_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
ALTER TABLE pfand ADD FOREIGN KEY (artikel_id) REFERENCES artikel(artikel_id);

CREATE TABLE rabattaktion (
    rabatt_id int(10) unsigned NOT NULL AUTO_INCREMENT,
    aktionsname varchar(50) DEFAULT NULL,
    rabatt_relativ decimal(6,5) DEFAULT NULL,
    rabatt_absolut decimal(13,2) DEFAULT NULL,
    mengenrabatt_schwelle int(10) unsigned DEFAULT NULL,
    mengenrabatt_anzahl_kostenlos int(10) unsigned DEFAULT NULL,
    mengenrabatt_relativ decimal(6,5) DEFAULT NULL,
    von DATETIME NOT NULL,
    bis DATETIME,
    produktgruppen_id int(10) unsigned DEFAULT NULL,
    artikel_id int(10) unsigned DEFAULT NULL,
    PRIMARY KEY (rabatt_id),
    FOREIGN KEY (produktgruppen_id) REFERENCES produktgruppe(produktgruppen_id),
    FOREIGN KEY (artikel_id) REFERENCES artikel(artikel_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE verkauf (
    rechnungs_nr int(10) unsigned NOT NULL AUTO_INCREMENT,
    verkaufsdatum DATETIME NOT NULL,
    storniert BOOL NOT NULL DEFAULT FALSE,
    ec_zahlung BOOL NOT NULL DEFAULT FALSE,
    PRIMARY KEY (rechnungs_nr)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
CREATE TABLE verkauf_details (
    vd_id int(10) unsigned NOT NULL AUTO_INCREMENT,
    rechnungs_nr int(10) unsigned NOT NULL,
    position int(5) unsigned DEFAULT NULL,
    artikel_id int(10) unsigned DEFAULT NULL,
    rabatt_id int(10) unsigned DEFAULT NULL,
    stueckzahl int(5) NOT NULL DEFAULT 1,
    ges_preis decimal(13,2) NOT NULL,
    mwst_satz decimal(6,5) NOT NULL,
    PRIMARY KEY (vd_id),
    FOREIGN KEY (rechnungs_nr) REFERENCES verkauf(rechnungs_nr),
    FOREIGN KEY (artikel_id) REFERENCES artikel(artikel_id),
    FOREIGN KEY (rabatt_id) REFERENCES rabattaktion(rabatt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE kassenstand (
    kassenstand_id int(10) unsigned NOT NULL AUTO_INCREMENT,
    buchungsdatum DATETIME NOT NULL,
    neuer_kassenstand decimal(13,2) NOT NULL,
    manuell BOOL NOT NULL DEFAULT FALSE,
    rechnungs_nr int(10) unsigned DEFAULT NULL,
    kommentar varchar(70),
    PRIMARY KEY (kassenstand_id),
    FOREIGN KEY (rechnungs_nr) REFERENCES verkauf(rechnungs_nr)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE abrechnung_tag (
    id int(10) unsigned NOT NULL AUTO_INCREMENT,
    zeitpunkt DATETIME NOT NULL,
    mwst_satz decimal(6,5) NOT NULL,
    mwst_netto decimal(13,2) NOT NULL,
    mwst_betrag decimal(13,2) NOT NULL,
    bar_brutto decimal(13,2) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
CREATE TABLE abrechnung_monat (
    id int(10) unsigned NOT NULL AUTO_INCREMENT,
    monat DATE NOT NULL,
    mwst_satz decimal(6,5) NOT NULL,
    mwst_netto decimal(13,2) NOT NULL,
    mwst_betrag decimal(13,2) NOT NULL,
    bar_brutto decimal(13,2) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
CREATE TABLE abrechnung_jahr (
    id int(10) unsigned NOT NULL AUTO_INCREMENT,
    jahr YEAR NOT NULL,
    mwst_satz decimal(6,5) NOT NULL,
    mwst_netto decimal(13,2) NOT NULL,
    mwst_betrag decimal(13,2) NOT NULL,
    bar_brutto decimal(13,2) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE bestellung (
    bestell_nr int(10) unsigned NOT NULL AUTO_INCREMENT,
    bestell_datum DATETIME NOT NULL,
    jahr YEAR NOT NULL DEFAULT 2000,
    kw int(2) NOT NULL DEFAULT 1,
    PRIMARY KEY (bestell_nr)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
CREATE TABLE bestellung_details (
    bestell_nr int(10) unsigned NOT NULL,
    position int(5) unsigned NOT NULL,
    artikel_id int(10) unsigned NOT NULL,
    stueckzahl int(5) NOT NULL DEFAULT 1,
    PRIMARY KEY (bestell_nr, position),
    FOREIGN KEY (bestell_nr) REFERENCES bestellung(bestell_nr),
    FOREIGN KEY (artikel_id) REFERENCES artikel(artikel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

GRANT ALL PRIVILEGES ON kasse.* TO 'kassenadmin'@'localhost';
GRANT SELECT ON kasse.* TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE ON kasse.artikel TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE ON kasse.mwst TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.lieferant TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE ON kasse.produktgruppe TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE ON kasse.verkauf TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.verkauf_details TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.kassenstand TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE ON kasse.rabattaktion TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.pfand TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.abrechnung_tag TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.abrechnung_monat TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.abrechnung_jahr TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE, DELETE ON kasse.bestellung TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE, DELETE ON kasse.bestellung_details TO 'mitarbeiter'@'localhost';
