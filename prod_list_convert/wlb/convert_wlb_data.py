#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from optparse import OptionParser
from getpass import getpass

# https://dev.mysql.com/downloads/connector/python/
import mysql.connector

# from sqlalchemy import create_engine

import numpy as np
import pandas as pd
import re
import os
import warnings


def main():
    # install parser
    usage = "Usage: %prog   [OPTIONS]"
    parser = OptionParser(usage)

    parser.add_option("--wlb", type="string",
                      default="Artikelliste_LM.csv",
                      dest="WLB",
                      help="The path to the WLB .csv file to write output to. Default: 'Artikelliste_LM.csv'")
    parser.add_option("--host", type="string", default='localhost', dest="HOST",
                      help="The hostname of the host hosting the MySQL DB. (Default: "
                      "'localhost')")
    parser.add_option("-p", "--pwd", type="string", default=None, dest="PWD",
                      help="The admin(!) password of the MySQL DB. If not set, password is "
                      "prompted for.")

    # get parsed args
    (options, args) = parser.parse_args()
    pwd = options.PWD
    if (pwd is None):
        pwd = getpass("Please enter mitarbeiter password of MySQL DB. ")

    #############
    # Load data #
    #############

    conn = mysql.connector.connect(host=options.HOST, user="mitarbeiter",
                                   password=pwd, db="kasse")

    # Need to:
    #   sudo apt install libmariadb3 libmariadb-dev
    #   pip install mariadb
    # db_connection_str = f"mariadb+mariadbconnector://mitarbeiter:{pwd}@{options.HOST}/kasse" # optional: ...:port/kasse
    # db_connection_str = 'mysql+mysql.connector://mitarbeiter:{pwd}@{options.HOST}/kasse'
    # db_connection_str = f"mariadb:///?User=mitarbeiter&Password={pwd}&Database=kasse&Server={options.HOST}" # optional: &Port=3306
    # db_connection = create_engine(db_connection_str)
    # print(db_connection)

    # cursor = conn.cursor()
    query = ("SELECT "
             "produktgruppe.produktgruppen_name, lieferant.lieferant_name, artikel.artikel_nr, "
             "artikel.artikel_name, artikel.kurzname, artikel.menge, artikel.einheit, artikel.sortiment, "
             "artikel.lieferbar, artikel.beliebtheit, artikel.barcode, artikel.vpe, artikel.setgroesse, "
             "artikel.vk_preis, artikel.empf_vk_preis, artikel.ek_rabatt, artikel.ek_preis, "
             "artikel.variabler_preis, artikel.herkunft, artikel.bestand "
             "FROM artikel "
             "INNER JOIN produktgruppe USING (produktgruppen_id) "
             "INNER JOIN lieferant USING (lieferant_id) "
             "WHERE produktgruppe.toplevel_id IN (2, 3) AND "
             "artikel.aktiv = TRUE AND artikel.artikel_nr NOT LIKE 'SONSTIGES%';")  # AND a.aktiv = TRUE
    # print(query)
    wlb = pd.read_sql(query, conn)

    # cursor.execute(query)
    # ids, numbers, aktivs = np.array(cursor.fetchall()).transpose()
    # print(np.array(cursor.fetchall()).transpose())
    # ids = np.array(ids, dtype=int)
    # aktivs = np.array(ids, dtype=bool)
    # cursor.close()

    conn.close()

    # Convert some columns, esp. boolean:
    wlb["sortiment"] = ["Ja" if b == 1 else "Nein" for b in wlb["sortiment"]]
    wlb["lieferbar"] = ["Ja" if b == 1 else "Nein" for b in wlb["lieferbar"]]
    wlb["beliebtheit"] = [
        "ausgelistet" if d == -1 else "niedrig" if d == 1 else "mittel" if d == 2
        else "hoch" if d == 3 else "keine Angabe" for d in wlb["beliebtheit"]
    ]
    print(wlb["vpe"])
    wlb["vpe"] = wlb["vpe"].fillna(0).astype(np.int64)
    print(wlb["vpe"])
    wlb["variabler_preis"] = [
        "Ja" if b == 1 else "Nein" for b in wlb["variabler_preis"]
    ]

    wlb.to_csv(options.WLB, sep=';', index=False, header=[
        "Produktgruppe",
        "Lieferant",
        "Artikelnummer",
        "Bezeichnung | Einheit",
        "Kurzname",
        "Menge (kg/l/St.)",
        "Einheit",
        "Sortiment",
        "Sofort lieferbar",
        "Beliebtheit",
        "Barcode",
        "VPE",
        "Setgröße",
        "VK-Preis",
        "Empf. VK-Preis",
        "EK-Rabatt",
        "EK-Preis",
        "Variabel",
        "Herkunftsland",
        "Bestand"
    ])


if __name__ == '__main__':
    main()
