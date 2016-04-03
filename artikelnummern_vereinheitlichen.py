#!/usr/bin/env python
# -*- coding: utf-8 -*-
from optparse import OptionParser
from getpass import getpass

'''
Skript zum halb-automatisierten Setzen der Einkaufsrabatte gemäß der
Rabattmatrix des FHZ (zwecks Inventur).
'''

# install parser
usage = "Usage: %prog   [OPTIONS]"
parser = OptionParser(usage)

parser.add_option("--host", type="string", default='localhost', dest="HOST",
        help="The hostname of the host hosting the MySQL DB. (Default: "
        "'localhost')")
parser.add_option("-p", "--pwd", type="string", default=None, dest="PWD",
        help="The admin(!) password of the MySQL DB. If not set, password is "
        "prompted for.")
parser.add_option("--do-it", action="store_true", default=False, dest="DOIT",
        help="CAREFUL: Writes the changes (shown when this param is not "
        "set) to the DB! First inspect if everything is OK without setting "
        "--do-it!")

# get parsed args
(options, args) = parser.parse_args()
pwd = options.PWD
if (pwd is None):
    pwd = getpass("Please enter mitarbeiter password of MySQL DB. ")

# https://dev.mysql.com/downloads/connector/python/
import mysql.connector
import numpy as np

import preisaenderung as p


def select_all(conn, lieferant='EP'):
    #with conn:
    cursor = conn.cursor()
    query = ("SELECT artikel_id, artikel_nr, a.aktiv FROM artikel AS a "
	    "INNER JOIN lieferant USING (lieferant_id) "
            "WHERE lieferant_name = %s")# AND a.aktiv = TRUE
    #print(query)
    cursor.execute(query, [lieferant])
    ids, numbers, aktivs = np.array(cursor.fetchall()).transpose()
    ids = np.array(ids, dtype=int)
    aktivs = np.array(ids, dtype=bool)
    cursor.close()
    return ids, numbers, aktivs


def select_active_article_ids_with(conn, lieferant, nr):
    #with conn:
    cursor = conn.cursor()
    query = ("SELECT artikel_id FROM artikel AS a "
	    "INNER JOIN lieferant USING (lieferant_id) "
            "WHERE lieferant_name = %s AND artikel_nr = %s AND a.aktiv = TRUE")
    #print(query)
    cursor.execute(query, [lieferant, nr])
    try:
        ids = list( np.array(cursor.fetchall(), dtype=int).transpose()[0] )
    except:
        ids = []
    cursor.close()
    return ids


def update_artikel_nr(conn, artikel_id, new_artikel_nr):
    #with conn:
    cursor = conn.cursor()
    query = ("UPDATE artikel "
        "SET artikel_nr = %s "
        "WHERE artikel_id = %s")
    #print(query)
    cursor.execute(query, [new_artikel_nr, int(artikel_id)])
    conn.commit()
    cursor.close()


def set_inactive(conn, artikel_id):
    #with conn:
    cursor = conn.cursor()
    query = ("UPDATE artikel "
        "SET aktiv = FALSE, bis = NOW() "
        "WHERE artikel_id = %s")
    #print(query)
    cursor.execute(query, [int(artikel_id)])
    conn.commit()
    cursor.close()


def process(lieferant, conversion_func):
    ids, numbers, aktivs = select_all(conn, lieferant=lieferant)
    for id_, number, aktiv in zip(ids, numbers, aktivs):
        new_number = conversion_func(number)
        if new_number != number:
            # Just change the wrong number to correct number, because there is no
            # conflicting article.
            if options.DOIT:
                print("ID %s, old '%s', new '%s' IS APPLIED TO DB!" % (id_, number, new_number))
                update_artikel_nr(conn, id_, new_number)
            else:
                print("old '%s', new '%s'" % (number, new_number))
            # Look for conflicts
            active_ids = select_active_article_ids_with(conn, lieferant, new_number)
            if not options.DOIT:
                active_ids += [id_] if aktiv else []
            if len(active_ids) > 1:
                active_ids = sorted(active_ids)
                print("      active article IDs with new artikel_nr: %s (%s is "
                        "corrected article, %s is kept active)" %
                        (active_ids, id_, max(active_ids)))
                if options.DOIT:
                    # There is now more than one article which has the corrected number.
                    # Keep the article with highest ID (newest article) and set the
                    # others inactive.
                    active_ids.remove(max(active_ids))
                    for i in active_ids:
                        set_inactive(conn, i)


conn = mysql.connector.connect(host=options.HOST, user="mitarbeiter",
        password=pwd, db="kasse")

print("------------- EP --------------")
process('EP', p.convert_art_number_ep)

print("\n\n\n")
print("------------- dwp --------------")
process('dwp', p.convert_art_number_dwp)

conn.close()
