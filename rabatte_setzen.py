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
    pwd = getpass("Please enter admin(!) password of MySQL DB. ")

# https://dev.mysql.com/downloads/connector/python/
import mysql.connector
import numpy as np

def select(conn, query):
    res = np.array([])
    #with conn:
    cursor = conn.cursor()
    #print(query)
    cursor.execute(query)
    things = cursor.fetchall()
    res = np.array(things)
    res = res.transpose()
    cursor.close()
    return res

def update(conn, query):
    #with conn:
    cursor = conn.cursor()
    #print(query)
    cursor.execute(query)
    cursor.close()

conn = mysql.connector.connect(host=options.HOST, user="mitarbeiter",
        password=pwd, db="kasse")

rabatt = 0.15
print("EK-Rabatt für folgende Artikel wird gesetzt auf: %s%%" % (100*rabatt))
query = ("SELECT artikel_nr, artikel_name FROM artikel AS a "
    "INNER JOIN lieferant AS l USING (lieferant_id) "
    "INNER JOIN produktgruppe AS p USING (produktgruppen_id) "
    "WHERE lieferant_name = 'GEPA' AND produktgruppen_name = 'Kaffee'")#"AND a.aktiv = TRUE"
print(select(conn, query))
query = ("UPDATE artikel "
    "INNER JOIN lieferant AS l USING (lieferant_id) "
    "INNER JOIN produktgruppe AS p USING (produktgruppen_id) "
    "SET ek_rabatt = %s "
    "WHERE lieferant_name = 'GEPA' AND produktgruppen_name = 'Kaffee'" % rabatt)#"AND a.aktiv = TRUE"
update(conn, query)
