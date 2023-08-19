#!/bin/bash

mysqldump -hlocalhost -ukassenadmin -p kasse -r DB_Dump_kasse_$(date -I)_vor_preisaenderung.sql && sudo systemctl stop mariadb.service && systemctl status mariadb.service
