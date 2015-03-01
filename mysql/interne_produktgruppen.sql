USE kasse;

--
-- Dumping data for table `produktgruppe`
--

LOCK TABLES `produktgruppe` WRITE;
/*!40000 ALTER TABLE `produktgruppe` DISABLE KEYS */;
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `aktiv`) VALUES (1,NULL,NULL,NULL,'Intern',NULL,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `aktiv`) VALUES (2,NULL,1,NULL,'Rabatt',NULL,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `aktiv`) VALUES (3,NULL,1,1,'Rabatt 0% MwSt',1,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `aktiv`) VALUES (4,NULL,1,2,'Rabatt 7% MwSt',2,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `aktiv`) VALUES (5,NULL,1,3,'Rabatt 19% MwSt',3,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `aktiv`) VALUES (6,NULL,2,NULL,'Gutschein',3,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `aktiv`) VALUES (7,NULL,3,NULL,'Pfand',NULL,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `aktiv`) VALUES (8,1,NULL,NULL,'Sonstiges',NULL,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `aktiv`) VALUES (9,1,1,NULL,'Sonstiges 0% MwSt',1,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `aktiv`) VALUES (10,1,2,NULL,'Sonstiges 7% MwSt',2,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `aktiv`) VALUES (11,1,3,NULL,'Sonstiges 19% MwSt',3,NULL,1);
/*!40000 ALTER TABLE `produktgruppe` ENABLE KEYS */;
UNLOCK TABLES;
