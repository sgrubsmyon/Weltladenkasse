USE kasse;

--
-- Dumping data for table `artikel`
--

LOCK TABLES `artikel` WRITE;
/*!40000 ALTER TABLE `artikel` DISABLE KEYS */;
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (10,10,1,'SONSTIGES1','Variabler Preis 7% MwSt, Sonstige','Var. Preis 7%',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (11,11,1,'SONSTIGES2','Variabler Preis 19% MwSt, Sonstige','Var. Preis 19%',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (12,17,1,'SONSTIGES3','Lebensmittel, Sonstige','Lebensmittel',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (13,67,1,'SONSTIGES4','Alkohol pfandfrei, Sonstige','Alkohol pfandfrei',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (14,71,1,'SONSTIGES5','Getränk pfandfrei, Sonstige','Getränk pfandfrei',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (15,72,1,'SONSTIGES6','Getränk 8 ct. Pfand, Sonstige','Getränk 8 ct. Pfand',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (16,73,1,'SONSTIGES7','Getränk 15 ct. Pfand, Sonstige','Getränk 15 ct. Pfand',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (17,74,1,'SONSTIGES8','Getränk 25 ct. Pfand, Sonstige','Getränk 25 ct. Pfand',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (18,76,1,'SONSTIGES9','Kunsthandwerk, Sonstige','Kunsthandwerk',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (19,77,1,'SONSTIGES10','Schmuck, Sonstige','Schmuck',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (20,78,1,'SONSTIGES11','Geschirr/Glaswaren, Sonstige','Geschirr/Glaswaren',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (21,79,1,'SONSTIGES12','Dekoartikel, Sonstige','Dekoartikel',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (22,80,1,'SONSTIGES13','Korb, Sonstige','Korb',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (23,81,1,'SONSTIGES14','Musikinstrument, Sonstige','Musikinstrument',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (23,82,1,'SONSTIGES15','Spielzeug, Sonstige','Spielzeug',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (24,83,1,'SONSTIGES15','Textilware, Sonstige','Textilware',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (25,84,1,'SONSTIGES16','Tasche, Sonstige','Tasche',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (26,86,1,'SONSTIGES17','CD, Sonstige','CD',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (27,87,1,'SONSTIGES18','Putumayo, Sonstige','Putumayo',NULL,NULL,NULL,NULL,1,15.90,NULL,NULL,NULL,0,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (28,89,1,'SONSTIGES19','Kleidung, Sonstige','Kleidung',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (29,92,1,'SONSTIGES20','Schuhe, Sonstige','Schuhe',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (30,94,1,'SONSTIGES21','Papierware, Sonstige','Papierware',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (31,95,1,'SONSTIGES22','Grußkarte, Sonstige','Grußkarte',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (32,96,1,'SONSTIGES23','Kalender, Sonstige','Kalender',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (33,97,1,'SONSTIGES24','Buch, Sonstige','Buch',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (34,98,1,'SONSTIGES25','Briefpapier/-umschlag, Sonstige','Briefpapier/-umschlag',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (35,99,1,'SONSTIGES26','Ergänzungsprodukt, Sonstige','Ergänzungsprodukt',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
/*!40000 ALTER TABLE `artikel` ENABLE KEYS */;
UNLOCK TABLES;
