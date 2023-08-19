#!/bin/bash

mysql --local-infile -hlocalhost -ukassenadmin -p -e "source DB_Dump_kasse_nach_preisaenderung.sql" kasse && sudo systemctl start mariadb.service && systemctl status mariadb.service
