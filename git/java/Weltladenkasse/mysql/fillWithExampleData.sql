USE kasse;

LOAD DATA LOCAL INFILE 'produktgruppen.dat' INTO TABLE produktgruppe;
LOAD DATA LOCAL INFILE 'lieferant.dat' INTO TABLE lieferant;

INSERT INTO abrechnung_tag SET zeitpunkt = '2013-10-30 18:54:28', mwst_satz = 0.07, mwst_netto = 222.33, mwst_betrag = 15.56, bar_brutto = 171.39;
INSERT INTO abrechnung_tag SET zeitpunkt = '2013-10-30 18:54:28', mwst_satz = 0.19, mwst_netto = 189.42, mwst_betrag = 35.99, bar_brutto = 170.00;
INSERT INTO abrechnung_tag SET zeitpunkt = '2013-10-31 19:01:32', mwst_satz = 0.07, mwst_netto = 182.83, mwst_betrag = 12.80, bar_brutto = 175.50;
INSERT INTO abrechnung_tag SET zeitpunkt = '2013-10-31 19:01:32', mwst_satz = 0.19, mwst_netto = 203.59, mwst_betrag = 38.68, bar_brutto = 150.00;

/*
LOAD DATA LOCAL INFILE 'artikel.dat' INTO TABLE artikel;
LOAD DATA LOCAL INFILE 'verkauf.dat' INTO TABLE verkauf;
LOAD DATA LOCAL INFILE 'verkauf_details.dat' INTO TABLE verkauf_details;
LOAD DATA LOCAL INFILE 'kassenstand.dat' INTO TABLE kassenstand;

INSERT INTO rabattaktion SET aktionsname="Jasmintee billiger!!!", rabatt_relativ=0.1, von="2012-08-06 00:00:00", bis="2012-08-10 23:59:59", artikel_id=10;
INSERT INTO rabattaktion SET aktionsname="Gewürz!!!", rabatt_relativ=0.25, von="2012-08-06 00:00:00", bis="2012-08-31 23:59:59", produktgruppen_id=27;
INSERT INTO rabattaktion SET aktionsname="orange tree!!!", rabatt_absolut=0.5, von="2012-08-06 00:00:00", bis="2012-08-31 23:59:59", artikel_id=12;
INSERT INTO rabattaktion SET aktionsname="ohne Ende Pasta Quinoa", rabatt_relativ=0.15, von="2012-08-06 00:00:00", artikel_id=15;
INSERT INTO rabattaktion SET aktionsname="Mengenrabatt Grüner Ceylon Tee", mengenrabatt_schwelle=5, mengenrabatt_relativ=0.25, von="2012-08-25 00:00:00", bis="2012-09-25 23:59:59", artikel_id=11;
INSERT INTO rabattaktion SET aktionsname="Mengenrabatt Mascobado Herzen", mengenrabatt_schwelle=4, mengenrabatt_anzahl_kostenlos=1, von="2012-08-25 00:00:00", bis="2012-09-25 23:59:59", artikel_id=16;
INSERT INTO rabattaktion SET aktionsname="Abgelaufener Apfel-Mango-Saft", rabatt_absolut=0.95, von="2012-08-25 00:00:00", artikel_id=18;
*/
