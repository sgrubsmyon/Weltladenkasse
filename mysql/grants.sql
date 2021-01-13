GRANT ALL PRIVILEGES ON kasse.* TO 'kassenadmin'@'localhost';
GRANT SELECT ON kasse.* TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE ON kasse.artikel TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE ON kasse.mwst TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE ON kasse.lieferant TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE ON kasse.produktgruppe TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE ON kasse.verkauf TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.verkauf_mwst TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.verkauf_details TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.kassenstand TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE ON kasse.rabattaktion TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE ON kasse.pfand TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.abrechnung_tag TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.abrechnung_tag_mwst TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE ON kasse.zaehlprotokoll TO 'mitarbeiter'@'localhost';
GRANT INSERT ON kasse.zaehlprotokoll_details TO 'mitarbeiter'@'localhost';
GRANT INSERT, DELETE ON kasse.abrechnung_monat TO 'mitarbeiter'@'localhost';
GRANT INSERT, DELETE ON kasse.abrechnung_monat_mwst TO 'mitarbeiter'@'localhost';
GRANT INSERT, DELETE ON kasse.abrechnung_jahr TO 'mitarbeiter'@'localhost';
GRANT INSERT, DELETE ON kasse.abrechnung_jahr_mwst TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE, DELETE ON kasse.bestellung TO 'mitarbeiter'@'localhost';
GRANT INSERT, UPDATE, DELETE ON kasse.bestellung_details TO 'mitarbeiter'@'localhost';
