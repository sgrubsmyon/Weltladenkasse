USE kasse;

--
-- Dumping data for table `produktgruppe`
--

LOCK TABLES `produktgruppe` WRITE;
/*!40000 ALTER TABLE `produktgruppe` DISABLE KEYS */;
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `std_einheit`, `n_artikel`, `n_artikel_rekursiv`, `aktiv`) VALUES (1,NULL,NULL,NULL,'Intern',NULL,NULL,NULL,NULL,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `std_einheit`, `n_artikel`, `n_artikel_rekursiv`, `aktiv`) VALUES (2,NULL,1,NULL,'Rabatt',NULL,NULL,NULL,NULL,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `std_einheit`, `n_artikel`, `n_artikel_rekursiv`, `aktiv`) VALUES (3,NULL,1,1,'Rabatt 0% MwSt',1,NULL,NULL,NULL,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `std_einheit`, `n_artikel`, `n_artikel_rekursiv`, `aktiv`) VALUES (4,NULL,1,2,'Rabatt 7% MwSt',2,NULL,NULL,NULL,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `std_einheit`, `n_artikel`, `n_artikel_rekursiv`, `aktiv`) VALUES (5,NULL,1,3,'Rabatt 19% MwSt',3,NULL,NULL,NULL,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `std_einheit`, `n_artikel`, `n_artikel_rekursiv`, `aktiv`) VALUES (6,NULL,2,NULL,'Gutschein',3,NULL,NULL,NULL,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `std_einheit`, `n_artikel`, `n_artikel_rekursiv`, `aktiv`) VALUES (7,NULL,3,NULL,'Pfand',3,NULL,NULL,NULL,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `std_einheit`, `n_artikel`, `n_artikel_rekursiv`, `aktiv`) VALUES (8,NULL,4,NULL,'Pfand optional',3,NULL,NULL,NULL,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `std_einheit`, `n_artikel`, `n_artikel_rekursiv`, `aktiv`) VALUES (9,1,NULL,NULL,'Sonstiges',NULL,NULL,NULL,NULL,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `std_einheit`, `n_artikel`, `n_artikel_rekursiv`, `aktiv`) VALUES (10,1,1,NULL,'Sonstiges 0% MwSt',1,NULL,NULL,NULL,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `std_einheit`, `n_artikel`, `n_artikel_rekursiv`, `aktiv`) VALUES (11,1,2,NULL,'Sonstiges 7% MwSt',2,NULL,NULL,NULL,NULL,1);
INSERT INTO `produktgruppe` (`produktgruppen_id`, `toplevel_id`, `sub_id`, `subsub_id`, `produktgruppen_name`, `mwst_id`, `pfand_id`, `std_einheit`, `n_artikel`, `n_artikel_rekursiv`, `aktiv`) VALUES (12,1,3,NULL,'Sonstiges 19% MwSt',3,NULL,NULL,NULL,NULL,1);
/*!40000 ALTER TABLE `produktgruppe` ENABLE KEYS */;
UNLOCK TABLES;
