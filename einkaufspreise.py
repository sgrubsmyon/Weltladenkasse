#!/usr/bin/env python
# -*- coding: utf-8 -*-
from optparse import OptionParser
from getpass import getpass

'''
Skript zum halb-automatisierten Setzen der Einkaufsrabatte und Einkaufspreise,
z.T. gemäß der Rabattmatrix des FHZ (zwecks Inventur).

* TODO: (optional) die Rundung bei Berechnung des EK-Preises aus dem Rabatt
nicht MySQL überlassen, sondern in Python machen. Dann hat man mehr Kontrolle
und kann z.B.  aussuchen, ob man als Rundungsmethode ROUND_HALF_UP (wie bisher
von uns in Java genutzt und offenbar default in MySQL ist) oder eine andere
Methode. Siehe hier:
    https://docs.python.org/3/library/decimal.html?highlight=decimal#module-decimal
bei getcontext().rounding.
Nachteil: Skript ist dann komplizierter und langsamer, da man für jeden Artikel
ein einzelnes Query braucht, das den EKP, den Python aus dem Rabatt berechnet
hat, setzt.

* TODO einfügen
  * Kalender BfdW
  * Kalender DNH
  * Kohle
  * WL-Kochbuch

Check, ob noch Preise fehlen:
> SELECT produktgruppen_id, produktgruppen_name, COUNT(*) FROM artikel JOIN produktgruppe USING (produktgruppen_id) WHERE ek_preis IS NULL AND vk_preis IS NOT NULL AND artikel.aktiv = TRUE GROUP BY produktgruppen_id;
> SELECT produktgruppen_id, produktgruppen_name, lieferant_name, artikel_name FROM artikel JOIN produktgruppe USING (produktgruppen_id) JOIN lieferant USING (lieferant_id) WHERE ek_preis IS NULL AND vk_preis IS NOT NULL AND artikel.aktiv = TRUE;
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
        help="The mitarbeiter password of the MySQL DB. If not set, password is "
        "prompted for.")
parser.add_option("-n", "--dry-run", action="store_true",
        default=False,
        dest="DRY_RUN",
        help="Do not change the DB, only show what would be changed?")
parser.add_option("-e", "--exclude-existing", action="store_true",
        default=False,
        dest="EXCLUDE_EXISTING",
        help="Exclude the DB entries that already have the corrent rabatt/ekp?")

# get parsed args
(options, args) = parser.parse_args()
pwd = options.PWD
if (pwd is None):
    pwd = getpass("Please enter mitarbeiter password of MySQL DB. ")

# https://dev.mysql.com/downloads/connector/python/
import mysql.connector
import numpy as np
#import decimal
#from decimal import Decimal


def count_missing_empf_vkp(conn):
    cursor = conn.cursor()
    query = ("SELECT COUNT(*) FROM artikel "
            "WHERE empf_vk_preis IS NULL AND vk_preis IS NOT NULL")
    cursor.execute(query)
    count = cursor.fetchone()[0]
    cursor.close()
    return count


def set_vkp_as_empf_vkp(conn):
    cursor = conn.cursor()
    query = ("UPDATE artikel SET empf_vk_preis = vk_preis*setgroesse "
            "WHERE empf_vk_preis IS NULL AND vk_preis IS NOT NULL")
    cursor.execute(query)
    conn.commit()
    cursor.close()


def construct_prod_gr_id_filter(conn, prod_gr='Kaffee'):
    #with conn:
    cursor = conn.cursor()
    query = ("SELECT toplevel_id, sub_id, subsub_id FROM produktgruppe WHERE "
            "produktgruppen_name = %s")
    cursor.execute(query, [prod_gr])
    #print(query, [prod_gr])
    top_id, sub_id, subsub_id = cursor.fetchall()[0]
    filtr = "AND toplevel_id = %s "
    filtr_vals = [top_id]
    if (sub_id is not None):
        filtr += "AND sub_id = %s "
        filtr_vals += [sub_id]
    if (subsub_id is not None):
        filtr += "AND subsub_id = %s "
        filtr_vals += [subsub_id]
    return (filtr, filtr_vals)



def select(conn, selector_str, selector_vals, prod_gr='Kaffee',
        value_name='ek_rabatt', value_label='EK-Rabatt', value=0.15):
    res = np.array([])
    #with conn:
    cursor = conn.cursor()
    filtr, filtr_vals = construct_prod_gr_id_filter(conn, prod_gr=prod_gr)
    query = ("SELECT artikel_nr, artikel_name, "+value_name+" FROM artikel AS a "
        "INNER JOIN lieferant AS l USING (lieferant_id) "
        "INNER JOIN produktgruppe AS p USING (produktgruppen_id) "
        "WHERE " + selector_str + " " + filtr)#"AND a.aktiv = TRUE"
    print("%s für folgende Artikel wird gesetzt auf: %s" % (value_label,
        value))
    #print(query)
    cursor.execute(query, selector_vals + filtr_vals)
    res = np.array(cursor.fetchall())
    cursor.close()
    print(res)
    return res.transpose()


def select_rabatt(conn, selector_str, selector_vals, prod_gr='Kaffee', rabatt=0.15):
    res = select(conn, selector_str, selector_vals, prod_gr=prod_gr,
            value_name='ek_rabatt', value_label='EK-Rabatt', value=rabatt)
    return res


def select_rabatt_by_lieferant(conn, lieferant='GEPA', prod_gr='Kaffee', rabatt=0.15):
    selector_str = "lieferant_name = %s"
    selector_vals = [lieferant]
    if options.EXCLUDE_EXISTING:
        selector_str += " AND (ek_rabatt IS NULL OR ek_rabatt != %s)"
        selector_vals += [rabatt]
    res = select_rabatt(conn, selector_str=selector_str,
            selector_vals=selector_vals, prod_gr=prod_gr, rabatt=rabatt)
    return res


def select_rabatt_by_name(conn, lieferant='GEPA', name='%credit%', prod_gr='Kaffee',
        rabatt=0.15):
    selector_str = "lieferant_name = %s AND artikel_name LIKE %s"
    selector_vals = [lieferant, name]
    if options.EXCLUDE_EXISTING:
        selector_str += " AND (ek_rabatt IS NULL OR ek_rabatt != %s)"
        selector_vals += [rabatt]
    res = select_rabatt(conn, selector_str=selector_str,
            selector_vals=selector_vals, prod_gr=prod_gr, rabatt=rabatt)
    return res


def select_ekp(conn, selector_str, selector_vals, prod_gr='Honig',
        einkaufspreis=4.85):
    res = select(conn, selector_str, selector_vals, prod_gr=prod_gr,
            value_name='ek_preis', value_label='EK-Preis', value=einkaufspreis)
    return res


def select_ekp_by_lieferant(conn, lieferant='Olaf Müller', prod_gr='Honig',
        einkaufspreis=4.85):
    selector_str = "lieferant_name = %s"
    selector_vals = [lieferant]
    if options.EXCLUDE_EXISTING:
        selector_str += " AND (ek_preis IS NULL OR ek_preis != %s)"
        selector_vals += [einkaufspreis]
    res = select_ekp(conn, selector_str=selector_str,
            selector_vals=selector_vals, prod_gr=prod_gr,
            einkaufspreis=einkaufspreis)
    return res


def select_ekp_by_name(conn, lieferant='FairMail', name='FairMail Postkarte',
        prod_gr='Grußkarten', einkaufspreis=0.55):
    selector_str = "lieferant_name = %s AND artikel_name LIKE %s"
    selector_vals = [lieferant, name]
    if options.EXCLUDE_EXISTING:
        selector_str += " AND (ek_preis IS NULL OR ek_preis != %s)"
        selector_vals += [einkaufspreis]
    res = select_ekp(conn, selector_str=selector_str,
            selector_vals=selector_vals, prod_gr=prod_gr,
            einkaufspreis=einkaufspreis)
    return res



###



#        return (bc.one.subtract(ekRabatt)).multiply(empfVKPreis); # Einkaufspreis
#                                                                  # = (1 -
#                                                                  # rabatt) *
#                                                                  # Empf.
#                                                                  # VK-Preis
def update(conn, selector_str, selector_vals, prod_gr='Kaffee', set_str='ek_rabatt = %s',
        set_vals=[0.15]):
    #with conn:
    cursor = conn.cursor()
    filtr, filtr_values = construct_prod_gr_id_filter(conn, prod_gr=prod_gr)
    query = ("UPDATE artikel "
        "INNER JOIN lieferant AS l USING (lieferant_id) "
        "INNER JOIN produktgruppe AS p USING (produktgruppen_id) "
        "SET " + set_str + " "
        "WHERE " + selector_str + " " + filtr)#"AND a.aktiv = TRUE"
    #print(query)
    cursor.execute(query, set_vals + selector_vals + filtr_values)
    conn.commit()
    cursor.close()


def update_rabatt(conn, selector_str, selector_vals, prod_gr='Kaffee', rabatt=0.15):
    update(conn, selector_str, selector_vals, prod_gr=prod_gr,
            set_str='ek_rabatt = %s, ek_preis = (1.-%s)*empf_vk_preis',
            set_vals=[rabatt, rabatt])


def update_rabatt_by_lieferant(conn, lieferant='GEPA', prod_gr='Kaffee', rabatt=0.15):
    selector_str = "lieferant_name = %s"
    selector_vals = [lieferant]
    if options.EXCLUDE_EXISTING:
        selector_str += " AND (ek_rabatt IS NULL OR ek_rabatt != %s)"
        selector_vals += [rabatt]
    update_rabatt(conn, selector_str=selector_str,
            selector_vals=selector_vals, prod_gr=prod_gr, rabatt=rabatt)


def update_rabatt_by_name(conn, lieferant='GEPA', name='%credit%', prod_gr='Kaffee',
        rabatt=0.15):
    selector_str = "lieferant_name = %s AND artikel_name LIKE %s"
    selector_vals = [lieferant, name]
    if options.EXCLUDE_EXISTING:
        selector_str += " AND (ek_rabatt IS NULL OR ek_rabatt != %s)"
        selector_vals += [rabatt]
    update_rabatt(conn, selector_str=selector_str,
            selector_vals=selector_vals, prod_gr=prod_gr, rabatt=rabatt)


def update_ekp(conn, selector_str, selector_vals, prod_gr='Honig',
        einkaufspreis=4.85):
    update(conn, selector_str, selector_vals, prod_gr=prod_gr,
            set_str='ek_preis = %s', set_vals=[einkaufspreis])


def update_ekp_by_lieferant(conn, lieferant='Olaf Müller', prod_gr='Honig',
        einkaufspreis=4.85):
    selector_str = "lieferant_name = %s"
    selector_vals = [lieferant]
    if options.EXCLUDE_EXISTING:
        selector_str += " AND (ek_preis IS NULL OR ek_preis != %s)"
        selector_vals += [einkaufspreis]
    update_ekp(conn, selector_str=selector_str,
            selector_vals=selector_vals, prod_gr=prod_gr,
            einkaufspreis=einkaufspreis)


def update_ekp_by_name(conn, lieferant='FairMail', name='FairMail Postkarte',
        prod_gr='Grußkarten', einkaufspreis=0.55):
    selector_str = "lieferant_name = %s AND artikel_name LIKE %s"
    selector_vals = [lieferant, name]
    if options.EXCLUDE_EXISTING:
        selector_str += " AND (ek_preis IS NULL OR ek_preis != %s)"
        selector_vals += [einkaufspreis]
    update_ekp(conn, selector_str=selector_str,
            selector_vals=selector_vals, prod_gr=prod_gr,
            einkaufspreis=einkaufspreis)



###



def rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Kaffee', rabatt=0.15):
    select_rabatt_by_lieferant(conn, lieferant=lieferant, prod_gr=prod_gr,
            rabatt=rabatt)
    if not options.DRY_RUN:
        update_rabatt_by_lieferant(conn, lieferant=lieferant, prod_gr=prod_gr,
                rabatt=rabatt)

def rabatt_setzen_by_name(conn, lieferant='GEPA', name='%credit%',
        prod_gr='Kaffee', rabatt=0.15):
    select_rabatt_by_name(conn, lieferant=lieferant, name=name, prod_gr=prod_gr,
            rabatt=rabatt)
    if not options.DRY_RUN:
        update_rabatt_by_name(conn, lieferant=lieferant, name=name, prod_gr=prod_gr,
                rabatt=rabatt)



def ekp_setzen_by_lieferant(conn, lieferant='Olaf Müller', prod_gr='Honig',
        einkaufspreis=4.85):
    select_ekp_by_lieferant(conn, lieferant=lieferant, prod_gr=prod_gr,
            einkaufspreis=einkaufspreis)
    if not options.DRY_RUN:
        update_ekp_by_lieferant(conn, lieferant=lieferant, prod_gr=prod_gr,
                einkaufspreis=einkaufspreis)


def ekp_setzen_by_name(conn, lieferant='FairMail', name='FairMail Postkarte',
        prod_gr='Grußkarten', einkaufspreis=0.55):
    select_ekp_by_name(conn, lieferant=lieferant, name=name, prod_gr=prod_gr,
            einkaufspreis=einkaufspreis)
    if not options.DRY_RUN:
        update_ekp_by_name(conn, lieferant=lieferant, name=name, prod_gr=prod_gr,
                einkaufspreis=einkaufspreis)




conn = mysql.connector.connect(host=options.HOST, user="mitarbeiter",
        password=pwd, db="kasse")


########################################
# Setzen von fehlenden Empf. VK-Preisen
########################################

count = count_missing_empf_vkp(conn)
if (count > 0):
    print("Bei %s Artikeln gibt es einen VK-Preis aber keinen empf. VK-Preis." % count)
    if not options.DRY_RUN:
        yesno = input("Soll für diese Artikel der VK-Preis als empf. VK-Preis übernommen "
                "werden? (y/n) ")
        if (yesno.lower() == 'y'):
            set_vkp_as_empf_vkp(conn)


#########################
# Setzen der EK-Rabatte
#########################

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
# rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Sonstige Lebensmittel', rabatt=0.33)
# rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Essig und Öl', rabatt=0.33)
# rabatt_setzen_by_lieferant(conn, lieferant='GEPA', prod_gr='Feinkost', rabatt=0.33)
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
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Kaffeeprodukte', rabatt=0.17)
### Kaffee oikocredit (ep)
rabatt_setzen_by_name(conn, lieferant='El Puente', name='%credit%',
        prod_gr='Kaffeeprodukte', rabatt=0.17)
### Kaffee rhein. affaire (ep)
rabatt_setzen_by_name(conn, lieferant='El Puente', name='%affaire%',
        prod_gr='Kaffeeprodukte', rabatt=0.17)
### Kaffee Ruanda (ep)
rabatt_setzen_by_name(conn, lieferant='El Puente', name='%Ruanda%',
        prod_gr='Kaffeeprodukte', rabatt=0.17)
### LM ep1
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Gewürze und Kräuter', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Soßen und Würzpasten', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Brotaufstriche', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Nüsse', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Trockenfrüchte', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Tee', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Getreide und Hülsenfrüchte', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Nudeln', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Essig und Öl', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Feinkost', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Sonstige Lebensmittel', rabatt=0.35)
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Saisonartikel', rabatt=0.35)
### LM ep2
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Zucker', rabatt=0.31)
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Kakao und Trinkschokolade', rabatt=0.31)
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Getränke', rabatt=0.31)
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Süßwaren und Snacks', rabatt=0.31)
### KHW ep
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Kunsthandwerk', rabatt=0.42)


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
### LM dwp2 + Zotter-Schokoladen (solange der Rabatt gleich wie bei dwp bleibt! wird vom FHZ über dwp bestellt)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Zucker', rabatt=0.29)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Kakao und Trinkschokolade', rabatt=0.29)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Getränke', rabatt=0.29)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Süßwaren und Snacks', rabatt=0.29)
rabatt_setzen_by_lieferant(conn, lieferant='dwp', prod_gr='Saisonartikel', rabatt=0.29)
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


### LM Hans Pfeffer --> besser Lieferant für beides "Bannmühle"
rabatt_setzen_by_lieferant(conn, lieferant='Bannmühle', prod_gr='Getränke', rabatt=0.23)
rabatt_setzen_by_lieferant(conn, lieferant='Bannmühle/dwp', prod_gr='Getränke', rabatt=0.23)


### Café Libertad
rabatt_setzen_by_lieferant(conn, lieferant='Café Libertad', prod_gr='Kaffeeprodukte', rabatt=0.17)


### Café Chavalo
rabatt_setzen_by_lieferant(conn, lieferant='Cafe Chavalo', prod_gr='Kaffeeprodukte', rabatt=0.10)


### Salinas
rabatt_setzen_by_lieferant(conn, lieferant='Salinas', prod_gr='Gewürze und Kräuter', rabatt=0.23)


### Harms-Verlag
rabatt_setzen_by_lieferant(conn, lieferant='Harms-Verlag', prod_gr='Bücher', rabatt=0.40)


### Zubehör faire/sonst. --> = Teefilter und Kaffeefilter, die jetzt nicht mehr von der GEPA sind, Lieferant ist jetzt "FHZ"
rabatt_setzen_by_name(conn, lieferant='FHZ Rheinland', name='%tee%',
        prod_gr='Ergänzungsprodukte', rabatt=0.35)
rabatt_setzen_by_name(conn, lieferant='FHZ Rheinland', name='%kaffee%',
        prod_gr='Ergänzungsprodukte', rabatt=0.35)
### FHZ Weihnachtsgaben
rabatt_setzen_by_name(conn, lieferant='FHZ Rheinland', name='%Weihnachtsgaben%ohne Umschlag%',
        prod_gr='Saisonartikel KHW', rabatt=0.33)
rabatt_setzen_by_name(conn, lieferant='FHZ Rheinland', name='%Weihnachtsgaben%mit Umschlag%',
        prod_gr='Saisonartikel KHW', rabatt=0.32)
rabatt_setzen_by_name(conn, lieferant='FHZ Rheinland', name='Umschlag%Weihnachtsgaben%',
        prod_gr='Saisonartikel KHW', rabatt=0.00)
# Zubehör anderes: Wein-Geschenkkarton (für 2 oder 3 Flaschen) und Holz-Präsentbox "Centopassi" --> beim FHZ nachgefragt: Rabatt = 0,00, Lieferant bei nicht-LT auf FHZ gesetzt!
rabatt_setzen_by_name(conn, lieferant='FHZ Rheinland', name='%Wein-Geschenkkarton%',
        prod_gr='Ergänzungsprodukte', rabatt=0.00)
rabatt_setzen_by_name(conn, lieferant='FHZ Rheinland', name='%Holz-Präsentbox%',
        prod_gr='Ergänzungsprodukte', rabatt=0.00)
rabatt_setzen_by_name(conn, lieferant='Libera Terra', name='%Holz-Präsentbox%',
        prod_gr='Ergänzungsprodukte', rabatt=0.00)


### Tragetasche von EP
rabatt_setzen_by_name(conn, lieferant='El Puente', name='%Papiertragetasche%',
        prod_gr='Ergänzungsprodukte', rabatt=0.00)
### Geschenkgutschein v. GEPA, nur fürs Bestellen, VK-Preis ist 0,00 € (Artnr 8929806)
rabatt_setzen_by_name(conn, lieferant='GEPA', name='%Geschenkgutschein%',
        prod_gr='Ergänzungsprodukte', rabatt=0.00)
### Bücher (z.B. die Kochbücher von EP)
rabatt_setzen_by_lieferant(conn, lieferant='El Puente', prod_gr='Bücher', rabatt=0.15)


#######################
# Setzen der EK-Preise
#######################

### Olaf Müller
ekp_setzen_by_lieferant(conn, lieferant='Olaf Müller', prod_gr='Honig',
        einkaufspreis=4.85)


### Fair Mail Karten
ekp_setzen_by_name(conn, lieferant='FairMail', name='FairMail Postkarte',
        prod_gr='Grußkarten', einkaufspreis=0.55)
ekp_setzen_by_name(conn, lieferant='FairMail', name='FairMail Klappkarte',
        prod_gr='Grußkarten', einkaufspreis=1.05)


### Postkarten Ackermann
ekp_setzen_by_lieferant(conn, lieferant='Postkarten Ackermann', prod_gr='Grußkarten',
        einkaufspreis=0.70)


### Biolog. Station
ekp_setzen_by_lieferant(conn, lieferant='Biologische Station Bonn',
        prod_gr='Alkoholfreie Getränke pfandfrei', einkaufspreis=6.00)


### Bantam
ekp_setzen_by_name(conn, lieferant='Bantam', name='%mais%',
        prod_gr='Saisonartikel', einkaufspreis=1.37)
ekp_setzen_by_name(conn, lieferant='Bantam', name='%tomaten%',
        prod_gr='Saisonartikel', einkaufspreis=1.77)
ekp_setzen_by_name(conn, lieferant='Bantam', name='%seedball%',
        prod_gr='Saisonartikel', einkaufspreis=2.35)


### Nager IT
ekp_setzen_by_name(conn, lieferant='Nager IT', name='%2 tasten%',
        prod_gr='Sonstiges Kunsthandwerk', einkaufspreis=26.22)
ekp_setzen_by_name(conn, lieferant='Nager IT', name='%3 tasten%',
        prod_gr='Sonstiges Kunsthandwerk', einkaufspreis=29.05)


### Memo
ekp_setzen_by_name(conn, lieferant='Memo', name='collegeblock%green%',
        prod_gr='Sonstiges Papier', einkaufspreis=1.96)
ekp_setzen_by_name(conn, lieferant='Memo', name='collegeblock%kariert%',
        prod_gr='Sonstiges Papier', einkaufspreis=1.65)
ekp_setzen_by_name(conn, lieferant='Memo', name='collegeblock%liniert%',
        prod_gr='Sonstiges Papier', einkaufspreis=1.65)
ekp_setzen_by_name(conn, lieferant='Memo', name='%briefumschlag%',
        prod_gr='Sonstiges Papier', einkaufspreis=0.04)
ekp_setzen_by_name(conn, lieferant='Memo', name='%kopierpapier%',
        prod_gr='Sonstiges Papier', einkaufspreis=2.37)
ekp_setzen_by_name(conn, lieferant='Memo', name='10 teelichte%',
        prod_gr='Ergänzungsprodukte', einkaufspreis=4.15)
ekp_setzen_by_name(conn, lieferant='Memo', name='teelichtbehälter%',
        prod_gr='Ergänzungsprodukte', einkaufspreis=0.53)


### Exil Music
ekp_setzen_by_name(conn, lieferant='Exil Music', name='Putumayo%',
        prod_gr='Putumayo', einkaufspreis=10.00)


conn.close()


# List all articles still missing an ek_preis, even though they have an
# empf_vk_preis:
# > SELECT artikel_id, artikel_name, artikel_nr, vk_preis, empf_vk_preis, ek_rabatt, ek_preis, aktiv FROM artikel WHERE empf_vk_preis IS NOT NULL AND ek_preis IS NULL;

# List all articles that are part of any bestellung, but don't have an ek_preis:
# > SELECT a.artikel_id, bestell_nr, artikel_name, artikel_nr, vk_preis, empf_vk_preis, ek_rabatt, ek_preis, aktiv FROM artikel AS a INNER JOIN bestellung_details USING (artikel_id) WHERE ek_preis IS NULL;

# List all articles still missing a rabatt:
# > SELECT lieferant_name, artikel_nr, artikel_name, produktgruppen_name FROM artikel JOIN lieferant USING (lieferant_id) JOIN produktgruppe USING (produktgruppen_id) WHERE ek_rabatt IS NULL;

# Check if the rounding has worked as desired:
# > SELECT artikel_nr, artikel_name, vk_preis, empf_vk_preis, ek_rabatt, ek_preis, (1.-ek_rabatt)*empf_vk_preis FROM artikel WHERE ek_preis IS NOT NULL LIMIT 100;
