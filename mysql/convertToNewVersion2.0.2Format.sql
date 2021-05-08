-- Execute command:
--    sudo mysql --local-infile -u kassenadmin < mysql/convertToNewVersion2.0.2Format.sql
-- or:
--    mysql --local-infile -h localhost -u kassenadmin -p < mysql/convertToNewVersion2.0.2Format.sql

USE kasse;

-- -------
-- mwst --
-- -------

ALTER TABLE mwst ADD COLUMN dsfinvk_ust_schluessel INTEGER(10) UNSIGNED NOT NULL,
    ADD COLUMN dsfinvk_ust_beschr VARCHAR(55) DEFAULT NULL;
UPDATE mwst SET dsfinvk_ust_schluessel = 6, dsfinvk_ust_beschr = 'Umsatzsteuerfrei' WHERE mwst_id = 1;
UPDATE mwst SET dsfinvk_ust_schluessel = 2, dsfinvk_ust_beschr = 'Geltender ermäßigter Steuersatz (§ 12 Abs. 2 UStG)' WHERE mwst_id = 2;
UPDATE mwst SET dsfinvk_ust_schluessel = 1, dsfinvk_ust_beschr = 'Geltender allgemeiner Steuersatz (§ 12 Abs. 2 UStG)' WHERE mwst_id = 3;
UPDATE mwst SET dsfinvk_ust_schluessel = 4, dsfinvk_ust_beschr = 'Durchschnittsatz (§ 24 Abs. 1 Nr. 1 UStG)' WHERE mwst_id = 4;
UPDATE mwst SET dsfinvk_ust_schluessel = 3, dsfinvk_ust_beschr = 'Durchschnittsatz (§ 24 Abs. 1 Nr. 3 UStG) übrige Fälle' WHERE mwst_id = 5;

-- ---------------------
-- abrechnung_tag_tse --
-- ---------------------

-- mitigate that TSE_ID of (failed) TSE was not written to DB
UPDATE abrechnung_tag_tse SET tse_id = 1;

-- ------------------
-- tse_transaction --
-- ------------------

-- mitigate that TSE_ERROR message was too long for CSV file specification
UPDATE tse_transaction SET tse_error = 'Keine Verbindung zur TSE. TSE (SD-Karte, die seitlich in Laptop-Schlitz steckt) sitzt nicht richtig drin oder Konfiguration (z.B. Pfad) in Datei \'config_tse.txt\' falsch.' WHERE tse_error = 'Es konnte keine Verbindung zur TSE aufgebaut werden. Entweder die TSE (eine SD-Karte, die in einem Schlitz des Kassen-PCs steckt)\n   sitzt nicht richtig drin oder die Konfiguration (etwa der Pfad) in der Datei \'config_tse.txt\' ist falsch.';
