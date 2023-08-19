#!/bin/bash

rsync -aPvi futon_vpn:DB_Dump_kasse_$(date -I)_vor_preisaenderung.sql . && mysql --local-infile -hlocalhost -ukassenadmin -p -e "source DB_Dump_kasse_$(date -I)_vor_preisaenderung.sql" kasse
