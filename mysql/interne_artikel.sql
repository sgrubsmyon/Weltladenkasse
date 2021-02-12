USE kasse;

--
-- Dumping data for table `artikel`
--

LOCK TABLES `artikel` WRITE;
/*!40000 ALTER TABLE `artikel` DISABLE KEYS */;
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `einheit`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (1,2,1,'RABATT1','Manueller Rabatt','Rabatt',NULL,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `einheit`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (2,2,1,'RABATT2','Rabatt auf Rechnung','Rabatt',NULL,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `einheit`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (3,6,1,'GUTSCHEIN','Gutschein','Gutschein',NULL,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,NULL,1,1,0,0,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `einheit`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (4,7,1,'PFAND1','Pfand 0,08 €','Pfand',NULL,NULL,NULL,NULL,NULL,1,0.08,0.08,NULL,0.08,0,1,0,-1,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `einheit`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (5,7,1,'PFAND2','Pfand 0,15 €','Pfand',NULL,NULL,NULL,NULL,NULL,1,0.15,0.15,NULL,0.15,0,1,0,-1,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `einheit`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (6,7,1,'PFAND3','Pfand 0,25 €','Pfand',NULL,NULL,NULL,NULL,NULL,1,0.25,0.25,NULL,0.25,0,1,0,-1,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `einheit`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (7,8,1,'PFANDKISTE','Pfand Kiste, Pfandkiste','Pfand Kiste',NULL,NULL,NULL,NULL,NULL,1,1.50,1.50,NULL,1.50,0,1,0,-1,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `einheit`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (8,8,1,'COLAKISTE','Pfand Cola-Kiste, Colakiste','Pfand Cola-Kiste',NULL,NULL,NULL,NULL,NULL,1,3.18,3.18,NULL,3.18,0,1,0,-1,NULL,NULL,NULL,1);
INSERT INTO `artikel` (`artikel_id`, `produktgruppen_id`, `lieferant_id`, `artikel_nr`, `artikel_name`, `kurzname`, `menge`, `einheit`, `barcode`, `herkunft`, `vpe`, `setgroesse`, `vk_preis`, `empf_vk_preis`, `ek_rabatt`, `ek_preis`, `variabler_preis`, `sortiment`, `lieferbar`, `beliebtheit`, `bestand`, `von`, `bis`, `aktiv`) VALUES (9,8,1,'PFAND6','Pfand Kaffee-Eimer','Pfand Kaffee-Eimer',NULL,NULL,NULL,NULL,NULL,1,2.60,2.60,NULL,2.60,0,0,0,-1,NULL,NULL,NULL,1);
/*!40000 ALTER TABLE `artikel` ENABLE KEYS */;
UNLOCK TABLES;
