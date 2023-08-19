#!/bin/bash

mysqldump -hlocalhost -ukassenadmin -p kasse -r DB_Dump_kasse_nach_preisaenderung.sql && rsync -aPvi DB_Dump_kasse_nach_preisaenderung.sql futon_vpn:.
