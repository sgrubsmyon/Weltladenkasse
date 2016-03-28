#!/usr/bin/env python
# -*- coding: utf-8 -*-
from optparse import OptionParser
from getpass import getpass

# install parser
usage = "Usage: %prog   [OPTIONS]"
parser = OptionParser(usage)

parser.add_option("--host", type="string",
        default='localhost',
        dest="HOST",
        help="The hostname of the host hosting the MySQL DB. (Default: "
        "'localhost')")
parser.add_option("-p", "--pwd", type="string",
        default=None,
        dest="PWD",
        help="The admin(!) password of the MySQL DB. If not set, password is "
        "prompted for.")

# get parsed args
(options, args) = parser.parse_args()
pwd = options.PWD
if (pwd is None):
    pwd = getpass("Please enter mitarbeiter password of MySQL DB. ")

# https://dev.mysql.com/downloads/connector/python/
import mysql.connector
import numpy as np

def select(conn, lieferant='GEPA', prod_gr='Kaffee', rabatt=0.15):
    res = np.array([])
    #with conn:
    cursor = conn.cursor()
    query = ("SELECT artikel_nr, artikel_name FROM artikel AS a "
        "INNER JOIN lieferant AS l USING (lieferant_id) "
        "INNER JOIN produktgruppe AS p USING (produktgruppen_id) "
        "WHERE lieferant_name = %s AND produktgruppen_name = %s")#"AND a.aktiv = TRUE"
    print("EK-Rabatt für folgende Artikel wird gesetzt auf: %s%%" % (100*rabatt))
    #print(query)
    cursor.execute(query, (lieferant, prod_gr))
    res = np.array(cursor.fetchall()).transpose()
    cursor.close()
    print(res)
    return res

def update(conn, lieferant='GEPA', prod_gr='Kaffee', rabatt=0.15):
    #with conn:
    cursor = conn.cursor()
    query = ("UPDATE artikel "
        "INNER JOIN lieferant AS l USING (lieferant_id) "
        "INNER JOIN produktgruppe AS p USING (produktgruppen_id) "
        "SET ek_rabatt = %s "
        "WHERE lieferant_name = %s AND produktgruppen_name = %s")#"AND a.aktiv = TRUE"
    #print(query)
    cursor.execute(query, (rabatt, lieferant, prod_gr))
    conn.commit()
    cursor.close()

conn = mysql.connector.connect(host=options.HOST, user="mitarbeiter",
        password=pwd, db="kasse")

lieferant = 'GEPA'
prod_gr = 'Kaffee'
rabatt = 0.15
select(conn, lieferant=lieferant, prod_gr=prod_gr, rabatt=rabatt)
update(conn, lieferant=lieferant, prod_gr=prod_gr, rabatt=rabatt)

conn.close()
