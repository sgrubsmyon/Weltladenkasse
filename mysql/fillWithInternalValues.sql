USE kasse;

INSERT INTO lieferant SET lieferant_name="unbekannt";

INSERT INTO mwst SET mwst_satz=.00;
INSERT INTO mwst SET mwst_satz=.07;
INSERT INTO mwst SET mwst_satz=.19;

LOAD DATA LOCAL INFILE 'interne_produktgruppen.dat' INTO TABLE produktgruppe;

LOAD DATA LOCAL INFILE 'interne_artikel.dat' INTO TABLE artikel;

INSERT INTO pfand SET artikel_id=4;
INSERT INTO pfand SET artikel_id=5;
INSERT INTO pfand SET artikel_id=6;
INSERT INTO pfand SET artikel_id=7;
