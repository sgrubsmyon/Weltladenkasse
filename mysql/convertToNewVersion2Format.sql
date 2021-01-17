-- Execute command:
--    sudo mysql --local-infile -u root < mysql/convertToNewVersion2Format.sql
-- or:
--    mysql --local-infile -h localhost -u root -p < mysql/convertToNewVersion2Format.sql

USE kasse;

--------------------
-- abrechnung_tag --
--------------------

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

-- Fill the new tables:
-- SELECT DISTINCT id, zeitpunkt, zeitpunkt_real, kassenstand_id, (SELECT MIN(rechnungs_nr) FROM verkauf WHERE verkaufsdatum > (SELECT DISTINCT zeitpunkt FROM abrechnung_tag_copy WHERE id = at.id - 1) AND verkaufsdatum <= zeitpunkt) AS rechnungs_nr_von, (SELECT MAX(rechnungs_nr) FROM verkauf WHERE verkaufsdatum > (SELECT DISTINCT zeitpunkt FROM abrechnung_tag_copy WHERE id = at.id - 1) AND verkaufsdatum <= zeitpunkt) AS rechnungs_nr_bis FROM abrechnung_tag_copy AS at LIMIT 5;
INSERT INTO abrechnung_tag
    SELECT DISTINCT id, zeitpunkt, zeitpunkt_real, kassenstand_id,
        (SELECT MIN(rechnungs_nr) FROM verkauf WHERE
            verkaufsdatum > IFNULL((SELECT DISTINCT zeitpunkt_real FROM abrechnung_tag_copy WHERE id = at.id - 1), '0001-01-01') AND
            verkaufsdatum <= zeitpunkt_real
        ) AS rechnungs_nr_von,
        (SELECT MAX(rechnungs_nr) FROM verkauf WHERE
            verkaufsdatum > IFNULL((SELECT DISTINCT zeitpunkt_real FROM abrechnung_tag_copy WHERE id = at.id - 1), '0001-01-01') AND
            verkaufsdatum <= zeitpunkt_real
        ) AS rechnungs_nr_bis
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

----------------------
-- abrechnung_monat --
----------------------

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

---------------------
-- abrechnung_jahr --
---------------------

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

---------------------
-- tse_transaction --
---------------------

CREATE TABLE tse_transaction (
    transaction_number INTEGER(10) UNSIGNED NOT NULL,
    rechnungs_nr INTEGER(10) UNSIGNED DEFAULT NULL,
    transaction_start CHAR(29) DEFAULT NULL,
    transaction_end CHAR(29) DEFAULT NULL,
    process_type VARCHAR(30) DEFAULT NULL,
    signature_counter INTEGER(10) UNSIGNED DEFAULT NULL,
    signatur VARCHAR(512) DEFAULT NULL,
    tse_error VARCHAR(200) DEFAULT NULL,
    process_data VARCHAR(60) DEFAULT NULL,
    PRIMARY KEY (transaction_number),
    FOREIGN KEY (rechnungs_nr) REFERENCES verkauf(rechnungs_nr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
GRANT INSERT ON kasse.tse_transaction TO 'mitarbeiter'@'localhost';
