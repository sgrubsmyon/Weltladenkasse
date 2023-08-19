#!/bin/bash

sudo systemctl start mariadb.service && mysql --local-infile -hlocalhost -ukassenadmin -p -e "source DB_Dump_kasse_nach_preisaenderung.sql" kasse && systemctl status mariadb.service
