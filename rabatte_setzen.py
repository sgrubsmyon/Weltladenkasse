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

def construct_prod_gr_id_filter(conn, prod_gr='Kaffee'):
    #with conn:
    cursor = conn.cursor()
    query = ("SELECT toplevel_id, sub_id, subsub_id FROM produktgruppe WHERE "
            "produktgruppen_name = %s")
    cursor.execute(query, [prod_gr])
    #print(query, [prod_gr])
    top_id, sub_id, subsub_id = cursor.fetchall()[0]
    filtr = "AND toplevel_id = %s "
    values = [top_id]
    if (sub_id is not None):
        filtr += "AND sub_id = %s "
        values += [sub_id]
    if (subsub_id is not None):
        filtr += "AND subsub_id = %s "
        values += [subsub_id]
    return (filtr, values)



def select(conn, selector_str, selector_vals, prod_gr='Kaffee', rabatt=0.15):
    res = np.array([])
    #with conn:
    cursor = conn.cursor()
    filtr, values = construct_prod_gr_id_filter(conn, prod_gr=prod_gr)
    query = ("SELECT artikel_nr, artikel_name, ek_rabatt FROM artikel AS a "
        "INNER JOIN lieferant AS l USING (lieferant_id) "
        "INNER JOIN produktgruppe AS p USING (produktgruppen_id) "
        "WHERE " + selector_str + filtr)#"AND a.aktiv = TRUE"
    print("EK-Rabatt für folgende Artikel wird gesetzt auf: %s%%" % (100*rabatt))
    #print(query)
    cursor.execute(query, selector_vals + values)
    res = np.array(cursor.fetchall()).transpose()
    cursor.close()
    print(res)
    return res

def select_by_lieferant(conn, lieferant='GEPA', prod_gr='Kaffee', rabatt=0.15):
    res = select(conn, selector_str="lieferant_name = %s ",
            selector_vals=[lieferant], prod_gr=prod_gr, rabatt=rabatt)
    return res

def select_by_name(conn, lieferant='GEPA', name='%credit%', prod_gr='Kaffee',
        rabatt=0.15):
    res = select(conn, selector_str="lieferant_name = %s AND artikel_name LIKE %s ",
            selector_vals=[lieferant, name], prod_gr=prod_gr, rabatt=rabatt)
    return res



def update(conn, selector_str, selector_vals, prod_gr='Kaffee', rabatt=0.15):
    #with conn:
    cursor = conn.cursor()
    filtr, values = construct_prod_gr_id_filter(conn, prod_gr=prod_gr)
    query = ("UPDATE artikel "
        "INNER JOIN lieferant AS l USING (lieferant_id) "
        "INNER JOIN produktgruppe AS p USING (produktgruppen_id) "
        "SET ek_rabatt = %s "
        "WHERE " + selector_str + filtr)#"AND a.aktiv = TRUE"
    #print(query)
    cursor.execute(query, [rabatt] + selector_vals + values)
    conn.commit()
    cursor.close()

def update_by_lieferant(conn, lieferant='GEPA', prod_gr='Kaffee', rabatt=0.15):
    update(conn, selector_str="lieferant_name = %s ",
            selector_vals=[lieferant], prod_gr=prod_gr, rabatt=rabatt)

def update_by_name(conn, lieferant='GEPA', name='%credit%', prod_gr='Kaffee',
        rabatt=0.15):
    update(conn, selector_str="lieferant_name = %s AND artikel_name LIKE %s ",
            selector_vals=[lieferant, name], prod_gr=prod_gr, rabatt=rabatt)



def rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Kaffee', rabatt=0.15):
    select_by_lieferant(conn, lieferant=lieferant, prod_gr=prod_gr, rabatt=rabatt)
    #update_by_lieferant(conn, lieferant=lieferant, prod_gr=prod_gr, rabatt=rabatt)

def rabatt_setzen_by_name(conn, lieferant='GEPA', name='%credit%',
        prod_gr='Kaffee', rabatt=0.15):
    select_by_name(conn, lieferant=lieferant, name=name, prod_gr=prod_gr,
            rabatt=rabatt)
    #update_by_name(conn, lieferant=lieferant, name=name, prod_gr=prod_gr,
    #        rabatt=rabatt)




conn = mysql.connector.connect(host=options.HOST, user="mitarbeiter",
        password=pwd, db="kasse")


### Kaffee gepa1
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Kaffee', rabatt=0.15)
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Entkoffeiniert', rabatt=0.15)
### Kaffee gepa2
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Espresso', rabatt=0.21)
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Kaffeepads', rabatt=0.21)
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Getreidekaffee', rabatt=0.21)
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Guarana', rabatt=0.21)
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Instantkaffee', rabatt=0.21)
### LM gepa1
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Gewürze und Kräuter', rabatt=0.33)
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Soßen und Würzpasten', rabatt=0.33)
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Brotaufstriche', rabatt=0.33)
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Nüsse', rabatt=0.33)
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Trockenfrüchte', rabatt=0.33)
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Tee', rabatt=0.33)
### nichts drin aber trotzdem zu LM1 (wenn, dann wäre es vermutlich da)
#rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Sonstige Lebensmittel', rabatt=0.33)
#rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Essig und Öl', rabatt=0.33)
#rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Feinkost', rabatt=0.33)
### LM gepa2
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Zucker', rabatt=0.29)
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Kakao und Trinkschokolade', rabatt=0.29)
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Getränke', rabatt=0.29)
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Süßwaren und Snacks', rabatt=0.29)
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Saisonartikel', rabatt=0.29)
### LM gepa Reis, Körner, Nudeln
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Getreide und Hülsenfrüchte', rabatt=0.17)
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Nudeln', rabatt=0.17)
### KHW gepa
rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Kunsthandwerk', rabatt=0.40)
### KHW gepa Dr. Bronner
rabatt_setzen_by_name(conn, lieferant='GEPA', name='%Dr. Bronner%', prod_gr='Kosmetik', rabatt=0.25)


### Kaffee ep
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Kaffeeprodukte', rabatt=0.17)
### Kaffee oikocredit (ep)
rabatt_setzen_by_name(conn, lieferant='EP', name='%credit%',
        prod_gr='Kaffeeprodukte', rabatt=0.17)
### Kaffee rhein. affaire (ep)
rabatt_setzen_by_name(conn, lieferant='EP', name='%affaire%',
        prod_gr='Kaffeeprodukte', rabatt=0.17)
### Kaffee Ruanda (ep)
rabatt_setzen_by_name(conn, lieferant='EP', name='%Ruanda%',
        prod_gr='Kaffeeprodukte', rabatt=0.17)
### LM ep1
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Gewürze und Kräuter', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Soßen und Würzpasten', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Brotaufstriche', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Nüsse', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Trockenfrüchte', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Tee', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Getreide und Hülsenfrüchte', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Nudeln', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Essig und Öl', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Feinkost', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Sonstige Lebensmittel', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Saisonartikel', rabatt=0.35)
### LM ep2
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Zucker', rabatt=0.31)
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Kakao und Trinkschokolade', rabatt=0.31)
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Getränke', rabatt=0.31)
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Süßwaren und Snacks', rabatt=0.31)
### KHW ep
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Kunsthandwerk', rabatt=0.42)


### Kaffee dwp
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Kaffeeprodukte', rabatt=0.17)
rabatt_setzen_by_name(conn, lieferant='dwp', name='%kaffee%',
        prod_gr='Saisonartikel', rabatt=0.17)
### LM dwp1
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Gewürze und Kräuter', rabatt=0.33)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Soßen und Würzpasten', rabatt=0.33)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Brotaufstriche', rabatt=0.33)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Nüsse', rabatt=0.33)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Trockenfrüchte', rabatt=0.33)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Tee', rabatt=0.33)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Getreide und Hülsenfrüchte', rabatt=0.33)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Nudeln', rabatt=0.33)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Sonstige Lebensmittel', rabatt=0.33)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Essig und Öl', rabatt=0.33)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Feinkost', rabatt=0.33)
rabatt_setzen_by_name(conn, lieferant='dwp', name='%tee%',
        prod_gr='Saisonartikel', rabatt=0.33)
### LM dwp2
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Zucker', rabatt=0.29)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Kakao und Trinkschokolade', rabatt=0.29)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Getränke', rabatt=0.29)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Süßwaren und Snacks', rabatt=0.29)
rabatt_setzen_by_lieferant(conn, lieferant='Zotter', prod_gr='Süßwaren und Snacks', rabatt=0.29)
rabatt_setzen_by_lieferant(conn, lieferant='Zotter', prod_gr='Saisonartikel', rabatt=0.29)
### KHW dwp
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Kunsthandwerk', rabatt=0.40)


### Libera Terra
rabatt_setzen_by_lieferant(conn, lieferant='Libera Terra', prod_gr='Lebensmittel', rabatt=0.33)
rabatt_setzen_by_lieferant(conn, lieferant='Libera Terra', prod_gr='Getränke', rabatt=0.33)


### ctm
rabatt_setzen_by_lieferant(conn, lieferant='CTM', prod_gr='Lebensmittel', rabatt=0.23)
rabatt_setzen_by_lieferant(conn, lieferant='CTM', prod_gr='Getränke', rabatt=0.23)


### eth
rabatt_setzen_by_lieferant(conn, lieferant='Ethiquable', prod_gr='Lebensmittel', rabatt=0.23)
rabatt_setzen_by_lieferant(conn, lieferant='Ethiquable', prod_gr='Getränke', rabatt=0.23)


### LM fairkauf
rabatt_setzen_by_lieferant(conn, lieferant='Fairkauf', prod_gr='Lebensmittel', rabatt=0.23)
rabatt_setzen_by_lieferant(conn, lieferant='Fairkauf', prod_gr='Getränke', rabatt=0.23)
### KHW fairkauf
rabatt_setzen_by_lieferant(conn, lieferant='Fairkauf', prod_gr='Kunsthandwerk', rabatt=0.35)


### KHW Globo
rabatt_setzen_by_lieferant(conn, lieferant='Globo', prod_gr='Kunsthandwerk', rabatt=0.32)


### KHW ftc
rabatt_setzen_by_lieferant(conn, lieferant='Fairtrade Center Breisgau', prod_gr='Kunsthandwerk', rabatt=0.40)


### LM Hans Pfeffer
rabatt_setzen_by_lieferant(conn, lieferant='Bannmühle', prod_gr='Getränke', rabatt=0.23)
rabatt_setzen_by_lieferant(conn, lieferant='Bannmühle/dwp', prod_gr='Getränke', rabatt=0.23)


### Café Libertad
rabatt_setzen_by_lieferant(conn, lieferant='Café Libertad', prod_gr='Kaffeeprodukte', rabatt=0.17)


### cafe chavalo
rabatt_setzen_by_lieferant(conn, lieferant='Café Chavalo', prod_gr='Kaffeeprodukte', rabatt=0.10)


### Zubehör faire/sonst. --> unklar, ob es da was gibt, wäre 35 %

# Zubehör fehlt noch: Wein-Geschenkkarton (für 2 oder 3 Flaschen) und Holz-Präsentbox "Centopassi" --> beim FHZ nachgefragt


### Zubehör gepa
rabatt_setzen_by_name(conn, lieferant='GEPA/and.', name='%tee%',
        prod_gr='Ergänzungsprodukte', rabatt=0.32)
rabatt_setzen_by_name(conn, lieferant='GEPA/and.', name='%kaffee%',
        prod_gr='Ergänzungsprodukte', rabatt=0.32)
rabatt_setzen_by_name(conn, lieferant='unbekannt', name='%tee%',
        prod_gr='Ergänzungsprodukte', rabatt=0.32)
rabatt_setzen_by_name(conn, lieferant='unbekannt', name='%kaffee%',
        prod_gr='Ergänzungsprodukte', rabatt=0.32)


### Tragetasche sollte zu Ergänzungsprodukte, oder?
rabatt_setzen_by_name(conn, lieferant='EP', name='%Papiertragetasche%',
        prod_gr='Sonstiges Kunsthandwerk', rabatt=0.00)
rabatt_setzen_by_name(conn, lieferant='EP', name='%Papiertragetasche%',
        prod_gr='Ergänzungsprodukte', rabatt=0.00)
### Dieser Artikel ist noch nicht in der Datenbank, sollte aber rein für einfacheres Bestellen
rabatt_setzen_by_name(conn, lieferant='GEPA', name='%Gutschein%',
        prod_gr='Ergänzungsprodukte', rabatt=0.00)


### Bücher
rabatt_setzen_by_lieferant(conn, lieferant='EP', prod_gr='Bücher', rabatt=0.15)


conn.close()
