-- Execute command:
--    sudo mysql --local-infile -u root < mysql/convertToNewVersion2Format.sql
-- or:
--    mysql --local-infile -h localhost -u root -p < mysql/convertToNewVersion2Format.sql
-- then:
--    sudo mv -i /var/lib/mysql/kasse/neues_v2.0_format_uebersetzung_rechnungs_nr_alt_neu.csv ..
--    sudo chown xxx:xxx ../neues_v2.0_format_uebersetzung_rechnungs_nr_alt_neu.csv
-- and make backup of the CSV file!

USE kasse;

-- -------
-- mwst --
-- -------

INSERT INTO `mwst` (`mwst_id`, `mwst_satz`) VALUES (4,0.05500);
INSERT INTO `mwst` (`mwst_id`, `mwst_satz`) VALUES (5,0.10700);

-- -----------------
-- abrechnung_tag --
-- -----------------

-- create temporary abrechnung_tag copy:
CREATE TABLE abrechnung_tag_copy (
    id INTEGER(10) UNSIGNED NOT NULL,
    zeitpunkt DATETIME NOT NULL,
    zeitpunkt_real DATETIME NOT NULL,
    mwst_satz DECIMAL(6,5) NOT NULL,
    mwst_netto DECIMAL(13,2) NOT NULL,
    mwst_betrag DECIMAL(13,2) NOT NULL,
    bar_brutto DECIMAL(13,2) NOT NULL,
    kassenstand_id INTEGER(10) UNSIGNED DEFAULT NULL,
    PRIMARY KEY (id, mwst_satz),
    FOREIGN KEY (kassenstand_id) REFERENCES kassenstand(kassenstand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO abrechnung_tag_copy SELECT * FROM abrechnung_tag;

-- also need zaehlprotokoll copy because of FK constraint:
CREATE TABLE zaehlprotokoll_copy (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    abrechnung_tag_id INTEGER(10) UNSIGNED NOT NULL,
    zeitpunkt DATETIME NOT NULL,
    kommentar TEXT NOT NULL,
    aktiv BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    FOREIGN KEY (abrechnung_tag_id) REFERENCES abrechnung_tag_copy(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO zaehlprotokoll_copy SELECT * FROM zaehlprotokoll;
CREATE TABLE zaehlprotokoll_details_copy (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    zaehlprotokoll_id INTEGER(10) UNSIGNED NOT NULL,
    anzahl SMALLINT(5) UNSIGNED NOT NULL,
    einheit DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (zaehlprotokoll_id) REFERENCES zaehlprotokoll_copy(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO zaehlprotokoll_details_copy SELECT * FROM zaehlprotokoll_details;

-- Create abrechnung_tag in new format:
DROP TABLE zaehlprotokoll_details;
DROP TABLE zaehlprotokoll;
DROP TABLE abrechnung_tag;
CREATE TABLE abrechnung_tag (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    z_kasse_id VARCHAR(30) NOT NULL,
    zeitpunkt DATETIME NOT NULL,
    zeitpunkt_real DATETIME NOT NULL,
    kassenstand_id INTEGER(10) UNSIGNED DEFAULT NULL,
    rechnungs_nr_von INTEGER(10) UNSIGNED NOT NULL,
    rechnungs_nr_bis INTEGER(10) UNSIGNED NOT NULL,
    last_tse_sig_counter INTEGER(10) DEFAULT NULL,
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

-- Fill the new tables:
-- SELECT DISTINCT id, zeitpunkt, zeitpunkt_real, kassenstand_id, (SELECT MIN(rechnungs_nr) FROM verkauf WHERE verkaufsdatum > (SELECT DISTINCT zeitpunkt FROM abrechnung_tag_copy WHERE id = at.id - 1) AND verkaufsdatum <= zeitpunkt) AS rechnungs_nr_von, (SELECT MAX(rechnungs_nr) FROM verkauf WHERE verkaufsdatum > (SELECT DISTINCT zeitpunkt FROM abrechnung_tag_copy WHERE id = at.id - 1) AND verkaufsdatum <= zeitpunkt) AS rechnungs_nr_bis, NULL AS last_tse_sig_counter FROM abrechnung_tag_copy AS at LIMIT 5;
INSERT INTO abrechnung_tag
    SELECT DISTINCT id, '877666797878-01', zeitpunkt, zeitpunkt_real, kassenstand_id,
        (SELECT MIN(rechnungs_nr) FROM verkauf WHERE
            verkaufsdatum > IFNULL((SELECT DISTINCT zeitpunkt_real FROM abrechnung_tag_copy WHERE id = at.id - 1), '0001-01-01') AND
            verkaufsdatum <= zeitpunkt_real
        ) AS rechnungs_nr_von,
        (SELECT MAX(rechnungs_nr) FROM verkauf WHERE
            verkaufsdatum > IFNULL((SELECT DISTINCT zeitpunkt_real FROM abrechnung_tag_copy WHERE id = at.id - 1), '0001-01-01') AND
            verkaufsdatum <= zeitpunkt_real
        ) AS rechnungs_nr_bis,
        NULL AS last_tse_sig_counter
    FROM abrechnung_tag_copy AS at;
-- SELECT id, mwst_satz, mwst_netto, mwst_betrag, bar_brutto FROM abrechnung_tag_copy LIMIT 5;
INSERT INTO abrechnung_tag_mwst
    SELECT id, mwst_satz, mwst_netto, mwst_betrag, bar_brutto FROM abrechnung_tag_copy;
INSERT INTO zaehlprotokoll SELECT * FROM zaehlprotokoll_copy;
INSERT INTO zaehlprotokoll_details SELECT * FROM zaehlprotokoll_details_copy;

-- Delete the temporary tables:
DROP TABLE zaehlprotokoll_details_copy;
DROP TABLE zaehlprotokoll_copy;
DROP TABLE abrechnung_tag_copy;

-- Grant default user access right to new MwSt. table: (need to do as root user)
GRANT INSERT ON kasse.abrechnung_tag_mwst TO 'mitarbeiter'@'localhost';

-- --------------------
-- abrechnung_monat --
-- --------------------

-- create temporary abrechnung_monat copy:
CREATE TABLE abrechnung_monat_copy (
    id INTEGER(10) UNSIGNED NOT NULL,
    monat DATE NOT NULL,
    mwst_satz DECIMAL(6,5) NOT NULL,
    mwst_netto DECIMAL(13,2) NOT NULL,
    mwst_betrag DECIMAL(13,2) NOT NULL,
    bar_brutto DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (id, mwst_satz)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO abrechnung_monat_copy SELECT * FROM abrechnung_monat;

-- Create abrechnung_monat in new format:
DROP TABLE abrechnung_monat;
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

-- Fill the new tables:
-- SELECT DISTINCT id, monat, (SELECT MIN(id) FROM abrechnung_tag WHERE zeitpunkt >= monat AND zeitpunkt < (monat + INTERVAL 1 MONTH)) AS abrechnung_tag_id_von, (SELECT MAX(id) FROM abrechnung_tag WHERE zeitpunkt >= monat AND zeitpunkt < (monat + INTERVAL 1 MONTH)) AS abrechnung_tag_id_bis FROM abrechnung_monat_copy LIMIT 10;
INSERT INTO abrechnung_monat
    SELECT DISTINCT id, monat,
        (SELECT MIN(id) FROM abrechnung_tag WHERE
            zeitpunkt >= monat AND zeitpunkt < (monat + INTERVAL 1 MONTH)
        ) AS abrechnung_tag_id_von,
        (SELECT MAX(id) FROM abrechnung_tag WHERE
            zeitpunkt >= monat AND zeitpunkt < (monat + INTERVAL 1 MONTH)
        ) AS abrechnung_tag_id_bis
    FROM abrechnung_monat_copy;
-- SELECT id, mwst_satz, mwst_netto, mwst_betrag, bar_brutto FROM abrechnung_monat_copy LIMIT 20;
INSERT INTO abrechnung_monat_mwst
    SELECT id, mwst_satz, mwst_netto, mwst_betrag, bar_brutto FROM abrechnung_monat_copy;

-- Delete the temporary table:
DROP TABLE abrechnung_monat_copy;

-- Grant default user access right to new MwSt. table: (need to do as root user)
GRANT INSERT, DELETE ON kasse.abrechnung_monat_mwst TO 'mitarbeiter'@'localhost';

-- -------------------
-- abrechnung_jahr --
-- -------------------

-- create temporary abrechnung_jahr copy:
CREATE TABLE abrechnung_jahr_copy (
    id INTEGER(10) UNSIGNED NOT NULL,
    jahr YEAR NOT NULL,
    mwst_satz DECIMAL(6,5) NOT NULL,
    mwst_netto DECIMAL(13,2) NOT NULL,
    mwst_betrag DECIMAL(13,2) NOT NULL,
    bar_brutto DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (id, mwst_satz)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO abrechnung_jahr_copy SELECT * FROM abrechnung_jahr;

-- Create abrechnung_jahr in new format:
DROP TABLE abrechnung_jahr;
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

-- Fill the new tables:
-- SELECT DISTINCT id, jahr, (SELECT MIN(id) FROM abrechnung_tag WHERE zeitpunkt >= DATE_FORMAT(jahr, '%Y-01-01') AND zeitpunkt < (DATE_FORMAT(jahr, '%Y-01-01') + INTERVAL 1 YEAR)) AS abrechnung_tag_id_von, (SELECT MAX(id) FROM abrechnung_tag WHERE zeitpunkt >= DATE_FORMAT(jahr, '%Y-01-01') AND zeitpunkt < (DATE_FORMAT(jahr, '%Y-01-01') + INTERVAL 1 YEAR)) AS abrechnung_tag_id_bis FROM abrechnung_jahr_copy;
INSERT INTO abrechnung_jahr
    SELECT DISTINCT id, jahr,
        (SELECT MIN(id) FROM abrechnung_tag WHERE
            zeitpunkt >= DATE_FORMAT(jahr, '%Y-01-01') AND
            zeitpunkt < (DATE_FORMAT(jahr, '%Y-01-01') + INTERVAL 1 YEAR)
        ) AS abrechnung_tag_id_von,
        (SELECT MAX(id) FROM abrechnung_tag WHERE
            zeitpunkt >= DATE_FORMAT(jahr, '%Y-01-01') AND
            zeitpunkt < (DATE_FORMAT(jahr, '%Y-01-01') + INTERVAL 1 YEAR)
        ) AS abrechnung_tag_id_bis
    FROM abrechnung_jahr_copy;
-- SELECT id, mwst_satz, mwst_netto, mwst_betrag, bar_brutto FROM abrechnung_jahr_copy;
INSERT INTO abrechnung_jahr_mwst
    SELECT id, mwst_satz, mwst_netto, mwst_betrag, bar_brutto FROM abrechnung_jahr_copy;

-- Delete the temporary table:
DROP TABLE abrechnung_jahr_copy;

-- Grant default user access right to new MwSt. table: (need to do as root user)
GRANT INSERT, DELETE ON kasse.abrechnung_jahr_mwst TO 'mitarbeiter'@'localhost';

-- -------------------
-- tse_transaction --
-- -------------------

CREATE TABLE tse_transaction (
    transaction_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    transaction_number INTEGER(10) UNSIGNED DEFAULT NULL,
    rechnungs_nr INTEGER(10) UNSIGNED DEFAULT NULL,
    training BOOLEAN NOT NULL DEFAULT FALSE,
    transaction_start CHAR(29) DEFAULT NULL,
    transaction_end CHAR(29) DEFAULT NULL,
    process_type VARCHAR(30) DEFAULT NULL,
    signature_counter INTEGER(10) UNSIGNED DEFAULT NULL,
    signature_base64 VARCHAR(512) DEFAULT NULL,
    tse_error TINYTEXT DEFAULT NULL,
    process_data VARCHAR(65) DEFAULT NULL,
    PRIMARY KEY (transaction_id),
    -- If planning to use method `getTransactionByTxNumber()`, can also create index on column `transaction_number`
    FOREIGN KEY (rechnungs_nr) REFERENCES verkauf(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
GRANT INSERT ON kasse.tse_transaction TO 'mitarbeiter'@'localhost';

-- ---------------
-- Artikelmenge --
-- ---------------

-- New Quittung format (2 rows per article) also prints "Menge",
-- so set it to sensible values for internal articles (NULL, so
-- that nothing is printed)

UPDATE artikel SET menge = NULL, einheit = NULL WHERE artikel_id < 10; -- artikel_id from 1 up to 9 is internal articles like Rabatt and Pfand
ALTER TABLE artikel MODIFY einheit VARCHAR(10) DEFAULT NULL;

-- -----------
-- Training --
-- -----------

CREATE TABLE training_verkauf (
    rechnungs_nr INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    verkaufsdatum DATETIME NOT NULL,
    storno_von INTEGER(10) UNSIGNED DEFAULT NULL,
    ec_zahlung BOOLEAN NOT NULL DEFAULT FALSE,
    kunde_gibt DECIMAL(13,2) DEFAULT NULL,
    PRIMARY KEY (rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE training_verkauf_mwst (
    rechnungs_nr INTEGER(10) UNSIGNED NOT NULL,
    mwst_satz DECIMAL(6,5) NOT NULL,
    mwst_netto DECIMAL(13,2) NOT NULL,
    mwst_betrag DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (rechnungs_nr, mwst_satz),
    FOREIGN KEY (rechnungs_nr) REFERENCES training_verkauf(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE training_verkauf_details (
    vd_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    rechnungs_nr INTEGER(10) UNSIGNED NOT NULL,
    position SMALLINT(5) UNSIGNED DEFAULT NULL,
    artikel_id INTEGER(10) UNSIGNED DEFAULT NULL,
    rabatt_id INTEGER(10) UNSIGNED DEFAULT NULL,
    stueckzahl SMALLINT(5) NOT NULL DEFAULT 1,
    ges_preis DECIMAL(13,2) NOT NULL,
    mwst_satz DECIMAL(6,5) NOT NULL,
    PRIMARY KEY (vd_id),
    FOREIGN KEY (rechnungs_nr) REFERENCES training_verkauf(rechnungs_nr),
    FOREIGN KEY (artikel_id) REFERENCES artikel(artikel_id),
    FOREIGN KEY (rabatt_id) REFERENCES rabattaktion(rabatt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE training_kassenstand (
    kassenstand_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    buchungsdatum DATETIME NOT NULL,
    neuer_kassenstand DECIMAL(13,2) NOT NULL,
    manuell BOOLEAN NOT NULL DEFAULT FALSE,
    entnahme BOOLEAN NOT NULL DEFAULT FALSE,
    rechnungs_nr INTEGER(10) UNSIGNED DEFAULT NULL,
    kommentar VARCHAR(70),
    PRIMARY KEY (kassenstand_id),
    FOREIGN KEY (rechnungs_nr) REFERENCES training_verkauf(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE training_abrechnung_tag (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    z_kasse_id VARCHAR(30) NOT NULL,
    zeitpunkt DATETIME NOT NULL,
    zeitpunkt_real DATETIME NOT NULL,
    kassenstand_id INTEGER(10) UNSIGNED DEFAULT NULL,
    rechnungs_nr_von INTEGER(10) UNSIGNED NOT NULL,
    rechnungs_nr_bis INTEGER(10) UNSIGNED NOT NULL,
    last_tse_sig_counter INTEGER(10) DEFAULT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (kassenstand_id) REFERENCES training_kassenstand(kassenstand_id),
    FOREIGN KEY (rechnungs_nr_von) REFERENCES training_verkauf(rechnungs_nr),
    FOREIGN KEY (rechnungs_nr_bis) REFERENCES training_verkauf(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE training_abrechnung_tag_mwst (
    id INTEGER(10) UNSIGNED NOT NULL,
    mwst_satz DECIMAL(6,5) NOT NULL,
    mwst_netto DECIMAL(13,2) NOT NULL,
    mwst_betrag DECIMAL(13,2) NOT NULL,
    bar_brutto DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (id, mwst_satz)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE training_zaehlprotokoll (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    abrechnung_tag_id INTEGER(10) UNSIGNED NOT NULL,
    zeitpunkt DATETIME NOT NULL,
    kommentar TEXT NOT NULL,
    aktiv BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    FOREIGN KEY (abrechnung_tag_id) REFERENCES training_abrechnung_tag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE training_zaehlprotokoll_details (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    zaehlprotokoll_id INTEGER(10) UNSIGNED NOT NULL,
    anzahl SMALLINT(5) UNSIGNED NOT NULL,
    einheit DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (zaehlprotokoll_id) REFERENCES training_zaehlprotokoll(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE training_abrechnung_monat (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    monat DATE NOT NULL,
    abrechnung_tag_id_von INTEGER(10) UNSIGNED NOT NULL,
    abrechnung_tag_id_bis INTEGER(10) UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (abrechnung_tag_id_von) REFERENCES training_abrechnung_tag(id),
    FOREIGN KEY (abrechnung_tag_id_bis) REFERENCES training_abrechnung_tag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE training_abrechnung_monat_mwst (
    id INTEGER(10) UNSIGNED NOT NULL,
    mwst_satz DECIMAL(6,5) NOT NULL,
    mwst_netto DECIMAL(13,2) NOT NULL,
    mwst_betrag DECIMAL(13,2) NOT NULL,
    bar_brutto DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (id, mwst_satz)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE training_abrechnung_jahr (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    jahr YEAR NOT NULL,
    abrechnung_tag_id_von INTEGER(10) UNSIGNED NOT NULL,
    abrechnung_tag_id_bis INTEGER(10) UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (abrechnung_tag_id_von) REFERENCES training_abrechnung_tag(id),
    FOREIGN KEY (abrechnung_tag_id_bis) REFERENCES training_abrechnung_tag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE training_abrechnung_jahr_mwst (
    id INTEGER(10) UNSIGNED NOT NULL,
    mwst_satz DECIMAL(6,5) NOT NULL,
    mwst_netto DECIMAL(13,2) NOT NULL,
    mwst_betrag DECIMAL(13,2) NOT NULL,
    bar_brutto DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (id, mwst_satz)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

GRANT INSERT ON kasse.training_verkauf TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.training_verkauf_mwst TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.training_verkauf_details TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.training_kassenstand TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.training_abrechnung_tag TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.training_abrechnung_tag_mwst TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE ON kasse.training_zaehlprotokoll TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.training_zaehlprotokoll_details TO 'mitarbeiter'@'localhost';
GRANT INSERT, DELETE ON kasse.training_abrechnung_monat TO 'mitarbeiter'@'localhost';
GRANT INSERT, DELETE ON kasse.training_abrechnung_monat_mwst TO 'mitarbeiter'@'localhost';
GRANT INSERT, DELETE ON kasse.training_abrechnung_jahr TO 'mitarbeiter'@'localhost';
GRANT INSERT, DELETE ON kasse.training_abrechnung_jahr_mwst TO 'mitarbeiter'@'localhost';

-- ------------------------------------
-- verkauf (Storno als Gegenbuchung) --
-- ------------------------------------

-- create temporary verkauf copy:
CREATE TABLE verkauf_copy (
    rechnungs_nr INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    verkaufsdatum DATETIME NOT NULL,
    storniert BOOLEAN NOT NULL DEFAULT FALSE,
    ec_zahlung BOOLEAN NOT NULL DEFAULT FALSE,
    kunde_gibt DECIMAL(13,2) DEFAULT NULL,
    PRIMARY KEY (rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO verkauf_copy SELECT * FROM verkauf;

-- also need copies of the FK constraint tables:
CREATE TABLE verkauf_mwst_copy (
    rechnungs_nr INTEGER(10) UNSIGNED NOT NULL,
    mwst_satz DECIMAL(6,5) NOT NULL,
    mwst_netto DECIMAL(13,2) NOT NULL,
    mwst_betrag DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (rechnungs_nr, mwst_satz),
    FOREIGN KEY (rechnungs_nr) REFERENCES verkauf_copy(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO verkauf_mwst_copy SELECT * FROM verkauf_mwst;
CREATE TABLE verkauf_details_copy (
    vd_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    rechnungs_nr INTEGER(10) UNSIGNED NOT NULL,
    position SMALLINT(5) UNSIGNED DEFAULT NULL,
    artikel_id INTEGER(10) UNSIGNED DEFAULT NULL,
    rabatt_id INTEGER(10) UNSIGNED DEFAULT NULL,
    stueckzahl SMALLINT(5) NOT NULL DEFAULT 1,
    ges_preis DECIMAL(13,2) NOT NULL,
    mwst_satz DECIMAL(6,5) NOT NULL,
    PRIMARY KEY (vd_id),
    FOREIGN KEY (rechnungs_nr) REFERENCES verkauf_copy(rechnungs_nr),
    FOREIGN KEY (artikel_id) REFERENCES artikel(artikel_id),
    FOREIGN KEY (rabatt_id) REFERENCES rabattaktion(rabatt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO verkauf_details_copy SELECT * FROM verkauf_details;
CREATE TABLE kassenstand_copy (
    kassenstand_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    buchungsdatum DATETIME NOT NULL,
    neuer_kassenstand DECIMAL(13,2) NOT NULL,
    manuell BOOLEAN NOT NULL DEFAULT FALSE,
    entnahme BOOLEAN NOT NULL DEFAULT FALSE,
    rechnungs_nr INTEGER(10) UNSIGNED DEFAULT NULL,
    kommentar VARCHAR(70),
    PRIMARY KEY (kassenstand_id),
    FOREIGN KEY (rechnungs_nr) REFERENCES verkauf_copy(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- There was a different column order, so need to explicitly name them:
INSERT INTO kassenstand_copy SELECT kassenstand_id, buchungsdatum,
    neuer_kassenstand, manuell, entnahme, rechnungs_nr, kommentar
    FROM kassenstand;
CREATE TABLE abrechnung_tag_copy (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    z_kasse_id VARCHAR(30) NOT NULL,
    zeitpunkt DATETIME NOT NULL,
    zeitpunkt_real DATETIME NOT NULL,
    kassenstand_id INTEGER(10) UNSIGNED DEFAULT NULL,
    rechnungs_nr_von INTEGER(10) UNSIGNED NOT NULL,
    rechnungs_nr_bis INTEGER(10) UNSIGNED NOT NULL,
    last_tse_sig_counter INTEGER(10) DEFAULT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (kassenstand_id) REFERENCES kassenstand_copy(kassenstand_id),
    FOREIGN KEY (rechnungs_nr_von) REFERENCES verkauf_copy(rechnungs_nr),
    FOREIGN KEY (rechnungs_nr_bis) REFERENCES verkauf_copy(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO abrechnung_tag_copy SELECT * FROM abrechnung_tag;
CREATE TABLE zaehlprotokoll_copy (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    abrechnung_tag_id INTEGER(10) UNSIGNED NOT NULL,
    zeitpunkt DATETIME NOT NULL,
    kommentar TEXT NOT NULL,
    aktiv BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    FOREIGN KEY (abrechnung_tag_id) REFERENCES abrechnung_tag_copy(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO zaehlprotokoll_copy SELECT * FROM zaehlprotokoll;
CREATE TABLE zaehlprotokoll_details_copy (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    zaehlprotokoll_id INTEGER(10) UNSIGNED NOT NULL,
    anzahl SMALLINT(5) UNSIGNED NOT NULL,
    einheit DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (zaehlprotokoll_id) REFERENCES zaehlprotokoll_copy(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO zaehlprotokoll_details_copy SELECT * FROM zaehlprotokoll_details;
CREATE TABLE abrechnung_monat_copy (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    monat DATE NOT NULL,
    abrechnung_tag_id_von INTEGER(10) UNSIGNED NOT NULL,
    abrechnung_tag_id_bis INTEGER(10) UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (abrechnung_tag_id_von) REFERENCES abrechnung_tag_copy(id),
    FOREIGN KEY (abrechnung_tag_id_bis) REFERENCES abrechnung_tag_copy(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO abrechnung_monat_copy SELECT * FROM abrechnung_monat;
CREATE TABLE abrechnung_jahr_copy (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    jahr YEAR NOT NULL,
    abrechnung_tag_id_von INTEGER(10) UNSIGNED NOT NULL,
    abrechnung_tag_id_bis INTEGER(10) UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (abrechnung_tag_id_von) REFERENCES abrechnung_tag_copy(id),
    FOREIGN KEY (abrechnung_tag_id_bis) REFERENCES abrechnung_tag_copy(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO abrechnung_jahr_copy SELECT * FROM abrechnung_jahr;
CREATE TABLE tse_transaction_copy (
    transaction_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    transaction_number INTEGER(10) UNSIGNED DEFAULT NULL,
    rechnungs_nr INTEGER(10) UNSIGNED DEFAULT NULL,
    training BOOLEAN NOT NULL DEFAULT FALSE,
    transaction_start CHAR(29) DEFAULT NULL,
    transaction_end CHAR(29) DEFAULT NULL,
    process_type VARCHAR(30) DEFAULT NULL,
    signature_counter INTEGER(10) UNSIGNED DEFAULT NULL,
    signature_base64 VARCHAR(512) DEFAULT NULL,
    tse_error TINYTEXT DEFAULT NULL,
    process_data VARCHAR(65) DEFAULT NULL,
    PRIMARY KEY (transaction_id),
    -- If planning to use method `getTransactionByTxNumber()`, can also create index on column `transaction_number`
    FOREIGN KEY (rechnungs_nr) REFERENCES verkauf_copy(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO tse_transaction_copy SELECT * FROM tse_transaction;

-- drop the original verkauf and FK constraint tables:
DROP TABLE tse_transaction, abrechnung_jahr, abrechnung_monat,
  zaehlprotokoll_details, zaehlprotokoll,
  abrechnung_tag, kassenstand, verkauf_details,
  verkauf_mwst, verkauf;

-- create the new verkauf and FK constraint tables and fill them:
CREATE TABLE verkauf (
    rechnungs_nr INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    verkaufsdatum DATETIME NOT NULL,
    storno_von INTEGER(10) UNSIGNED DEFAULT NULL,
    ec_zahlung BOOLEAN NOT NULL DEFAULT FALSE,
    kunde_gibt DECIMAL(13,2) DEFAULT NULL,
    PRIMARY KEY (rechnungs_nr),
    FOREIGN KEY (storno_von) REFERENCES verkauf(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- ------------------------------------
-- Insert the normal transactions:
INSERT INTO verkauf SELECT rechnungs_nr, verkaufsdatum,
    NULL, ec_zahlung, kunde_gibt FROM verkauf_copy;
-- ------------------------------------
-- Make room for the rechnung_nr of the storno (cancelling) transactions:
--   (directly after the original transaction)
SET @n_storno = (SELECT COUNT(*) FROM verkauf_copy WHERE storniert = TRUE);
SET @loop_var = @n_storno;
DELIMITER $$ -- Need to change delimiter to be able to execute several statements (a while loop) (see https://stackoverflow.com/questions/6628390/mysql-trouble-with-creating-user-defined-function-udf/6628455#6628455)
WHILE @loop_var > 0 DO
    SET @row_num = 0;
    -- Select the @loop_var'th storno rechnungs_nr
    SET @storno_nr = (
        SELECT rechnungs_nr FROM (
            SELECT rechnungs_nr, @row_num := @row_num + 1 AS rank
            FROM verkauf_copy WHERE storniert = TRUE
        ) AS d WHERE rank = @loop_var
    );
    -- Increment all rechnungs_nrs afterwards,
    --   descendingly to avoid temporary duplicates
    UPDATE verkauf SET rechnungs_nr = rechnungs_nr + 1
        WHERE rechnungs_nr > @storno_nr
        ORDER BY rechnungs_nr DESC;
    SET @loop_var = @loop_var - 1; -- Continue with next storno (descendingly)
END WHILE;
$$
DELIMITER ;
-- Remove variables:
SET @n_storno = NULL;
SET @loop_var = NULL;
SET @storno_nr = NULL;
SET @row_num = NULL;
-- ------------------------------------
-- Create temporary table with translation from old to new rechnungs_nr
CREATE TABLE rechnungs_nr_alt_neu (
    rechnungs_nr_alt INTEGER(10) UNSIGNED NOT NULL,
    rechnungs_nr_neu INTEGER(10) UNSIGNED NOT NULL,
    PRIMARY KEY (rechnungs_nr_alt)
);
INSERT INTO rechnungs_nr_alt_neu
    SELECT alt.rechnungs_nr, neu.rechnungs_nr
    FROM verkauf_copy AS alt INNER JOIN verkauf AS neu USING (verkaufsdatum);
-- ------------------------------------
-- Insert the storno (cancelling) transactions into the created gaps:
INSERT INTO verkauf SELECT
    rechnungs_nr_neu + 1, IFNULL(
    -- ^ rechnungs_nr     ^ verkaufsdatum
        (SELECT buchungsdatum FROM kassenstand_copy WHERE rechnungs_nr = altneu.rechnungs_nr_alt AND manuell = 0 AND kommentar = "Storno"),
        DATE_ADD(verkaufsdatum, INTERVAL 1 MINUTE)
    ),
    rechnungs_nr_neu, ec_zahlung,  NULL
    -- ^ storno_von   ^ ec_zahlung ^ kunde_gibt
    FROM verkauf_copy AS v INNER JOIN rechnungs_nr_alt_neu AS altneu
    ON v.rechnungs_nr = altneu.rechnungs_nr_alt
    WHERE storniert = TRUE;
-- ------------------------------------
CREATE TABLE verkauf_mwst (
    rechnungs_nr INTEGER(10) UNSIGNED NOT NULL,
    mwst_satz DECIMAL(6,5) NOT NULL,
    mwst_netto DECIMAL(13,2) NOT NULL,
    mwst_betrag DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (rechnungs_nr, mwst_satz),
    FOREIGN KEY (rechnungs_nr) REFERENCES verkauf(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- ------------------------------------
-- Insert the normal transactions:
INSERT INTO verkauf_mwst SELECT
    rechnungs_nr_neu, mwst_satz, mwst_netto, mwst_betrag
    FROM verkauf_mwst_copy AS v INNER JOIN rechnungs_nr_alt_neu AS altneu
    ON v.rechnungs_nr = altneu.rechnungs_nr_alt;
-- Insert the storno (cancelling) transactions with inverse values:
INSERT INTO verkauf_mwst SELECT
    (SELECT rechnungs_nr FROM verkauf WHERE storno_von = altneu.rechnungs_nr_neu) AS rechnungs_nr,
    mwst_satz, -mwst_netto, -mwst_betrag
    FROM verkauf_mwst_copy AS v INNER JOIN rechnungs_nr_alt_neu AS altneu
    ON v.rechnungs_nr = altneu.rechnungs_nr_alt
    WHERE altneu.rechnungs_nr_neu IN
        (SELECT storno_von FROM verkauf WHERE storno_von IS NOT NULL);
-- ------------------------------------
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
-- ------------------------------------
-- Insert the normal transactions:
INSERT INTO verkauf_details SELECT vd_id, rechnungs_nr_neu, position,
    artikel_id, rabatt_id, stueckzahl, ges_preis, mwst_satz
    FROM verkauf_details_copy AS v INNER JOIN rechnungs_nr_alt_neu AS altneu
    ON v.rechnungs_nr = altneu.rechnungs_nr_alt;
-- Insert the storno (cancelling) transactions with inverse stueckzahl:
INSERT INTO verkauf_details SELECT NULL,
    (SELECT rechnungs_nr FROM verkauf WHERE storno_von = altneu.rechnungs_nr_neu) AS rechnungs_nr,
    position, artikel_id, rabatt_id, -stueckzahl, -ges_preis, mwst_satz
    FROM verkauf_details_copy AS v INNER JOIN rechnungs_nr_alt_neu AS altneu
    ON v.rechnungs_nr = altneu.rechnungs_nr_alt
    WHERE altneu.rechnungs_nr_neu IN
        (SELECT storno_von FROM verkauf WHERE storno_von IS NOT NULL);
-- ------------------------------------
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
-- ------------------------------------
-- Insert the normal transactions:
INSERT INTO kassenstand SELECT kassenstand_id, buchungsdatum, neuer_kassenstand,
    manuell, entnahme, rechnungs_nr_neu, kommentar
    FROM kassenstand_copy AS v LEFT JOIN rechnungs_nr_alt_neu AS altneu -- left join because rechnungs_nr can be NULL
    ON v.rechnungs_nr = altneu.rechnungs_nr_alt;
-- Increase the rechnungs_nr of the storno bookings by one, since this is their number generated above on row 598
UPDATE kassenstand SET rechnungs_nr = rechnungs_nr + 1 WHERE manuell = 0 AND kommentar = "Storno";
-- ------------------------------------
CREATE TABLE abrechnung_tag (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    z_kasse_id VARCHAR(30) NOT NULL,
    zeitpunkt DATETIME NOT NULL,
    zeitpunkt_real DATETIME NOT NULL,
    kassenstand_id INTEGER(10) UNSIGNED DEFAULT NULL,
    rechnungs_nr_von INTEGER(10) UNSIGNED NOT NULL,
    rechnungs_nr_bis INTEGER(10) UNSIGNED NOT NULL,
    last_tse_sig_counter INTEGER(10) DEFAULT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (kassenstand_id) REFERENCES kassenstand(kassenstand_id),
    FOREIGN KEY (rechnungs_nr_von) REFERENCES verkauf(rechnungs_nr),
    FOREIGN KEY (rechnungs_nr_bis) REFERENCES verkauf(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- ------------------------------------
-- Insert the normal transactions:
INSERT INTO abrechnung_tag SELECT id, z_kasse_id, zeitpunkt, zeitpunkt_real,
    kassenstand_id, altneu_von.rechnungs_nr_neu, altneu_bis.rechnungs_nr_neu,
    last_tse_sig_counter
    FROM abrechnung_tag_copy AS v INNER JOIN rechnungs_nr_alt_neu AS altneu_von
    ON v.rechnungs_nr_von = altneu_von.rechnungs_nr_alt
    INNER JOIN rechnungs_nr_alt_neu AS altneu_bis
    ON v.rechnungs_nr_bis = altneu_bis.rechnungs_nr_alt;
-- ------------------------------------
CREATE TABLE zaehlprotokoll (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    abrechnung_tag_id INTEGER(10) UNSIGNED NOT NULL,
    zeitpunkt DATETIME NOT NULL,
    kommentar TEXT NOT NULL,
    aktiv BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    FOREIGN KEY (abrechnung_tag_id) REFERENCES abrechnung_tag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- ------------------------------------
-- Insert the normal transactions:
INSERT INTO zaehlprotokoll SELECT * FROM zaehlprotokoll_copy;
-- ------------------------------------
CREATE TABLE zaehlprotokoll_details (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    zaehlprotokoll_id INTEGER(10) UNSIGNED NOT NULL,
    anzahl SMALLINT(5) UNSIGNED NOT NULL,
    einheit DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (zaehlprotokoll_id) REFERENCES zaehlprotokoll(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- ------------------------------------
-- Insert the normal transactions:
INSERT INTO zaehlprotokoll_details SELECT * FROM zaehlprotokoll_details_copy;
-- ------------------------------------
CREATE TABLE abrechnung_monat (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    monat DATE NOT NULL,
    abrechnung_tag_id_von INTEGER(10) UNSIGNED NOT NULL,
    abrechnung_tag_id_bis INTEGER(10) UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (abrechnung_tag_id_von) REFERENCES abrechnung_tag(id),
    FOREIGN KEY (abrechnung_tag_id_bis) REFERENCES abrechnung_tag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- ------------------------------------
-- Insert the normal transactions:
INSERT INTO abrechnung_monat SELECT * FROM abrechnung_monat_copy;
-- ------------------------------------
CREATE TABLE abrechnung_jahr (
    id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    jahr YEAR NOT NULL,
    abrechnung_tag_id_von INTEGER(10) UNSIGNED NOT NULL,
    abrechnung_tag_id_bis INTEGER(10) UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (abrechnung_tag_id_von) REFERENCES abrechnung_tag(id),
    FOREIGN KEY (abrechnung_tag_id_bis) REFERENCES abrechnung_tag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- ------------------------------------
-- Insert the normal transactions:
INSERT INTO abrechnung_jahr SELECT * FROM abrechnung_jahr_copy;
-- ------------------------------------
CREATE TABLE tse_transaction (
    transaction_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    transaction_number INTEGER(10) UNSIGNED DEFAULT NULL,
    rechnungs_nr INTEGER(10) UNSIGNED DEFAULT NULL,
    training BOOLEAN NOT NULL DEFAULT FALSE,
    transaction_start CHAR(29) DEFAULT NULL,
    transaction_end CHAR(29) DEFAULT NULL,
    process_type VARCHAR(30) DEFAULT NULL,
    signature_counter INTEGER(10) UNSIGNED DEFAULT NULL,
    signature_base64 VARCHAR(512) DEFAULT NULL,
    tse_error TINYTEXT DEFAULT NULL,
    process_data VARCHAR(65) DEFAULT NULL,
    PRIMARY KEY (transaction_id),
    -- If planning to use method `getTransactionByTxNumber()`, can also create index on column `transaction_number`
    FOREIGN KEY (rechnungs_nr) REFERENCES verkauf(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- ------------------------------------
-- Insert the normal transactions:
INSERT INTO tse_transaction SELECT transaction_id, transaction_number, rechnungs_nr_neu,
    training, transaction_start, transaction_end, process_type, signature_counter,
    signature_base64, tse_error, process_data
    FROM tse_transaction_copy AS v LEFT JOIN rechnungs_nr_alt_neu AS altneu -- left join because rechnungs_nr can be NULL
    ON v.rechnungs_nr = altneu.rechnungs_nr_alt;
-- ------------------------------------

-- drop the temporary translation table:
-- export the translation table into a CSV file for documentation purposes:
-- this adds column header row with "rechnungs_nr_alt   rechnungs_nr_neu", but does
-- not work on Raspi:
-- (SELECT 'rechnungs_nr_alt', 'rechnungs_nr_neu')
--     UNION
-- (SELECT rechnungs_nr_alt, rechnungs_nr_neu FROM rechnungs_nr_alt_neu)
--     INTO OUTFILE 'neues_v2.0_format_uebersetzung_rechnungs_nr_alt_neu.csv'; -- IMPORTANT: copy this file before it gets lost upon reboot!!!
SELECT rechnungs_nr_alt, rechnungs_nr_neu FROM rechnungs_nr_alt_neu
    INTO OUTFILE 'neues_v2.0_format_uebersetzung_rechnungs_nr_alt_neu.csv'; -- IMPORTANT: copy this file before it gets lost upon reboot!!!
DROP TABLE rechnungs_nr_alt_neu;
-- drop the temporary copies:
DROP TABLE tse_transaction_copy, abrechnung_jahr_copy, abrechnung_monat_copy,
  zaehlprotokoll_details_copy, zaehlprotokoll_copy,
  abrechnung_tag_copy, kassenstand_copy, verkauf_details_copy,
  verkauf_mwst_copy, verkauf_copy;

-- finally, since storno is now performed by inserting a new Gegenbuchung,
-- we can revoke the UPDATE grant for table verkauf
REVOKE UPDATE ON kasse.verkauf FROM 'mitarbeiter'@'localhost';

-- ---------------------------
-- Insert internal articles --
-- ---------------------------

-- Drop FK constraints:
ALTER TABLE pfand DROP CONSTRAINT pfand_ibfk_1;
ALTER TABLE rabattaktion DROP CONSTRAINT rabattaktion_ibfk_2;
ALTER TABLE verkauf_details DROP CONSTRAINT verkauf_details_ibfk_2;
ALTER TABLE bestellung_details DROP CONSTRAINT bestellung_details_ibfk_2;
ALTER TABLE training_verkauf_details DROP CONSTRAINT training_verkauf_details_ibfk_2;
-- Update tables, i.e. shift artikel_id by 4 to make room:
UPDATE artikel SET artikel_id = artikel_id + 3
    WHERE artikel_id >= 3 ORDER BY artikel_id DESC;
UPDATE pfand SET artikel_id = artikel_id + 3
    WHERE artikel_id >= 3;
UPDATE rabattaktion SET artikel_id = artikel_id + 3
    WHERE artikel_id >= 3;
UPDATE verkauf_details SET artikel_id = artikel_id + 3
    WHERE artikel_id >= 3;
UPDATE bestellung_details SET artikel_id = artikel_id + 3
    WHERE artikel_id >= 3;
UPDATE training_verkauf_details SET artikel_id = artikel_id + 3
    WHERE artikel_id >= 3;
UPDATE artikel SET artikel_id = artikel_id + 1
    WHERE artikel_id >= 7 ORDER BY artikel_id DESC;
UPDATE pfand SET artikel_id = artikel_id + 1
    WHERE artikel_id >= 7;
UPDATE rabattaktion SET artikel_id = artikel_id + 1
    WHERE artikel_id >= 7;
UPDATE verkauf_details SET artikel_id = artikel_id + 1
    WHERE artikel_id >= 7;
UPDATE bestellung_details SET artikel_id = artikel_id + 1
    WHERE artikel_id >= 7;
UPDATE training_verkauf_details SET artikel_id = artikel_id + 1
    WHERE artikel_id >= 7;
-- Additionally, make extra room for potential future insertions: (up to 99 internal article slots)
UPDATE artikel SET artikel_id = artikel_id + 86
    WHERE artikel_id >= 14 ORDER BY artikel_id DESC;
UPDATE rabattaktion SET artikel_id = artikel_id + 86
    WHERE artikel_id >= 14;
UPDATE verkauf_details SET artikel_id = artikel_id + 86
    WHERE artikel_id >= 14;
UPDATE bestellung_details SET artikel_id = artikel_id + 86
    WHERE artikel_id >= 14;
UPDATE training_verkauf_details SET artikel_id = artikel_id + 86
    WHERE artikel_id >= 14;
-- Add FK constraints again:
ALTER TABLE pfand ADD FOREIGN KEY (artikel_id) REFERENCES artikel(artikel_id);
ALTER TABLE rabattaktion ADD FOREIGN KEY (artikel_id) REFERENCES artikel(artikel_id);
ALTER TABLE verkauf_details ADD FOREIGN KEY (artikel_id) REFERENCES artikel(artikel_id);
ALTER TABLE bestellung_details ADD FOREIGN KEY (artikel_id) REFERENCES artikel(artikel_id);
ALTER TABLE training_verkauf_details ADD FOREIGN KEY (artikel_id) REFERENCES artikel(artikel_id);
-- Insert the 4 new articles:
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `einheit`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (3,2,1,'ANPASSUNG','Manuelle Preisanpassung','Preisanpassung',NULL,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `einheit`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (4,2,1,'ANZAHLUNG','Anzahlung','Anzahlung',NULL,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `einheit`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (5,2,1,'ANZAHLAUFLOES','Anzahlungsauflösung','Anzahl.auflös.',NULL,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `einheit`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (7,6,1,'GUTSCHEINEINLOES','Gutscheineinlösung','Gutscheineinlösung',NULL,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);

-- ------------
-- anzahlung --
-- ------------

CREATE TABLE anzahlung (
    anzahlung_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    datum DATETIME NOT NULL,
    anzahlung_in_rech_nr INTEGER(10) UNSIGNED NOT NULL,
    aufloesung_in_rech_nr INTEGER(10) UNSIGNED DEFAULT NULL,
    PRIMARY KEY (anzahlung_id),
    FOREIGN KEY (anzahlung_in_rech_nr) REFERENCES verkauf(rechnungs_nr),
    FOREIGN KEY (aufloesung_in_rech_nr) REFERENCES verkauf(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE anzahlung_details (
    ad_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    rechnungs_nr INTEGER(10) UNSIGNED NOT NULL,
    vd_id INTEGER(10) UNSIGNED NOT NULL,
    ges_preis DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (ad_id),
    FOREIGN KEY (rechnungs_nr) REFERENCES verkauf_details(rechnungs_nr),
    FOREIGN KEY (vd_id) REFERENCES verkauf_details(vd_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE training_anzahlung (
    anzahlung_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    datum DATETIME NOT NULL,
    anzahlung_in_rech_nr INTEGER(10) UNSIGNED NOT NULL,
    aufloesung_in_rech_nr INTEGER(10) UNSIGNED DEFAULT NULL,
    PRIMARY KEY (anzahlung_id),
    FOREIGN KEY (anzahlung_in_rech_nr) REFERENCES training_verkauf(rechnungs_nr),
    FOREIGN KEY (aufloesung_in_rech_nr) REFERENCES training_verkauf(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE training_anzahlung_details (
    ad_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    rechnungs_nr INTEGER(10) UNSIGNED NOT NULL,
    vd_id INTEGER(10) UNSIGNED NOT NULL,
    ges_preis DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (ad_id),
    FOREIGN KEY (rechnungs_nr) REFERENCES training_verkauf_details(rechnungs_nr),
    FOREIGN KEY (vd_id) REFERENCES training_verkauf_details(vd_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

GRANT INSERT ON kasse.anzahlung TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.anzahlung_details TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.training_anzahlung TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.training_anzahlung_details TO 'mitarbeiter'@'localhost';

-- Set old Anzahlung articles to inactive:
UPDATE artikel SET aktiv = FALSE WHERE artikel_ID IN (31293, 31294, 31295);
-- check if any Anzahlung article is still found in article select panel

-- ---------------------
-- abrechnung_tag_tse --
-- ---------------------

CREATE TABLE abrechnung_tag_tse (
    id INTEGER(10) UNSIGNED NOT NULL,
    tse_id INTEGER(10) UNSIGNED DEFAULT NULL,
    tse_serial VARCHAR(68) DEFAULT NULL,
    tse_sig_algo VARCHAR(21) DEFAULT NULL,
    tse_time_format VARCHAR(31) DEFAULT NULL,
    tse_pd_encoding VARCHAR(5) DEFAULT NULL,
    tse_public_key VARCHAR(512) DEFAULT NULL,
    tse_cert_i VARCHAR(1000) DEFAULT NULL,
    tse_cert_ii VARCHAR(1000) DEFAULT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (id) REFERENCES abrechnung_tag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE training_abrechnung_tag_tse (
    id INTEGER(10) UNSIGNED NOT NULL,
    tse_id INTEGER(10) UNSIGNED DEFAULT NULL,
    tse_serial VARCHAR(68) DEFAULT NULL,
    tse_sig_algo VARCHAR(21) DEFAULT NULL,
    tse_time_format VARCHAR(31) DEFAULT NULL,
    tse_pd_encoding VARCHAR(5) DEFAULT NULL,
    tse_public_key VARCHAR(512) DEFAULT NULL,
    tse_cert_i VARCHAR(1000) DEFAULT NULL,
    tse_cert_ii VARCHAR(1000) DEFAULT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (id) REFERENCES training_abrechnung_tag(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

GRANT INSERT ON kasse.abrechnung_tag_tse TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.training_abrechnung_tag_tse TO 'mitarbeiter'@'localhost';


-- ------------
-- gutschein --
-- ------------

-- Gutschein muss mit 0% versteuert werden, weil es sich um einen "Mehrzweckgutschein" handelt,
-- also ein zahlungsmittelähnliches Instrument gemäß § 3 Abs. 15 Satz 2 UStG
UPDATE produktgruppe SET mwst_id = 1 WHERE produktgruppen_id = 6;

CREATE TABLE gutschein (
    gutschein_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    gutschein_nr INTEGER(10) UNSIGNED NOT NULL,
    datum DATETIME NOT NULL,
    gutschein_in_vd_id INTEGER(10) UNSIGNED DEFAULT NULL,
    einloesung_in_vd_id INTEGER(10) UNSIGNED DEFAULT NULL,
    restbetrag DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (gutschein_id),
    FOREIGN KEY (gutschein_in_vd_id) REFERENCES verkauf_details(vd_id),
    FOREIGN KEY (einloesung_in_vd_id) REFERENCES verkauf_details(vd_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE training_gutschein (
    gutschein_id INTEGER(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    gutschein_nr INTEGER(10) UNSIGNED NOT NULL,
    datum DATETIME NOT NULL,
    gutschein_in_vd_id INTEGER(10) UNSIGNED DEFAULT NULL,
    einloesung_in_vd_id INTEGER(10) UNSIGNED DEFAULT NULL,
    restbetrag DECIMAL(13,2) NOT NULL,
    PRIMARY KEY (gutschein_id),
    FOREIGN KEY (gutschein_in_vd_id) REFERENCES training_verkauf_details(vd_id),
    FOREIGN KEY (einloesung_in_vd_id) REFERENCES training_verkauf_details(vd_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

GRANT INSERT ON kasse.gutschein TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.training_gutschein TO 'mitarbeiter'@'localhost';
