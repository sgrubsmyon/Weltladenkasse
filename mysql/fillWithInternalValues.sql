USE kasse;

LOCK TABLES `lieferant` WRITE;
/*!40000 ALTER TABLE `lieferant` DISABLE KEYS */;
INSERT INTO `lieferant` (`lieferant_id`, `lieferant_name`, `lieferant_kurzname`, `n_artikel`, `aktiv`) VALUES (1,'unbekannt','unbekannt',NULL,1);
/*!40000 ALTER TABLE `lieferant` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `mwst` WRITE;
/*!40000 ALTER TABLE `mwst` DISABLE KEYS */;
INSERT INTO `mwst` (`mwst_id`, `mwst_satz`, `dsfinvk_ust_schluessel`, `dsfinvk_ust_beschr`) VALUES (1,0.00000,6,'Umsatzsteuerfrei');
INSERT INTO `mwst` (`mwst_id`, `mwst_satz`, `dsfinvk_ust_schluessel`, `dsfinvk_ust_beschr`) VALUES (2,0.07000,2,'Geltender ermäßigter Steuersatz (§ 12 Abs. 2 UStG)');
INSERT INTO `mwst` (`mwst_id`, `mwst_satz`, `dsfinvk_ust_schluessel`, `dsfinvk_ust_beschr`) VALUES (3,0.19000,1,'Geltender allgemeiner Steuersatz (§ 12 Abs. 2 UStG)');
INSERT INTO `mwst` (`mwst_id`, `mwst_satz`, `dsfinvk_ust_schluessel`, `dsfinvk_ust_beschr`) VALUES (4,0.05500,4,'Durchschnittsatz (§ 24 Abs. 1 Nr. 1 UStG)');
INSERT INTO `mwst` (`mwst_id`, `mwst_satz`, `dsfinvk_ust_schluessel`, `dsfinvk_ust_beschr`) VALUES (5,0.10700,3,'Durchschnittsatz (§ 24 Abs. 1 Nr. 3 UStG) übrige Fälle');
/*!40000 ALTER TABLE `mwst` ENABLE KEYS */;
UNLOCK TABLES;

/*LOAD DATA LOCAL INFILE 'interne_produktgruppen.dat' INTO TABLE produktgruppe;*/
/*LOAD DATA LOCAL INFILE 'interne_artikel.dat' INTO TABLE artikel;*/
SOURCE interne_produktgruppen.sql;
SOURCE interne_artikel.sql;

LOCK TABLES `pfand` WRITE;
/*!40000 ALTER TABLE `pfand` DISABLE KEYS */;
INSERT INTO `pfand` (`pfand_id`, `artikel_id`) VALUES (1,8);
INSERT INTO `pfand` (`pfand_id`, `artikel_id`) VALUES (2,9);
INSERT INTO `pfand` (`pfand_id`, `artikel_id`) VALUES (3,10);
INSERT INTO `pfand` (`pfand_id`, `artikel_id`) VALUES (4,11);
INSERT INTO `pfand` (`pfand_id`, `artikel_id`) VALUES (5,12);
INSERT INTO `pfand` (`pfand_id`, `artikel_id`) VALUES (6,13);
/*!40000 ALTER TABLE `pfand` ENABLE KEYS */;
UNLOCK TABLES;
