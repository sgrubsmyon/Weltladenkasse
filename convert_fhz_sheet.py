#!/usr/bin/env python3
# -*- coding: utf-8 -*-


def main():
    from optparse import OptionParser

    # install parser
    usage = "Usage: %prog   [OPTIONS]"
    parser = OptionParser(usage)

    parser.add_option("--fhz", type="string",
                      default='Bestellvorlage Lebensmittelpreisliste 3.0 2022.ods',
                      dest="FHZ",
                      help="The path to the FHZ .ods file.")
#     parser.add_option("-n", action="store_true",
#                       default=False,
#                       dest="ADOPT_NAMES",
#                       help="Artikelnamen ('Bezeichnung | Einheit') vom FHZ übernehmen?")

    # get parsed args
    (options, args) = parser.parse_args()

    #############
    # Load data #
    #############

#     import numpy as np
    import pandas as pd
    from pandas_ods_reader import read_ods

    fhz = read_ods(options.FHZ)

# # For testing in ipython3:
# fhz = read_ods('Bestellvorlage Lebensmittelpreisliste 3.0 2022.ods')

    # Find out where data actually starts
    header_index = fhz.index[fhz.iloc[:, 0] == 'Neu'][0]
    # Extract actual column names and correct them
    colnames = fhz.iloc[header_index, :].tolist()
    index_einheit = colnames.index('Einheit')
    colnames[index_einheit] = 'Menge (kg/l/St.)'
    colnames[index_einheit+1] = 'Einheit'
    colnames[index_einheit+2] = 'Verpackung'
    colnames[colnames.index('vegan (v)')] = 'vegan'
    colnames[colnames.index('Artikel-    nummer')] = 'Artikelnummer'
    colnames[colnames.index('x')] = 'Sofort lieferbar'
    colnames[colnames.index('je Ein-   heit')] = 'je Einheit'
    colnames[colnames.index('Preis-ände-rung')] = 'Preisänderung'
    colnames[colnames.index('Bestell- menge (Stk.)')] = 'Bestellmenge (Stk.)'
    colnames[colnames.index('vorgemerkt ')] = 'vorgemerkt'
    # Discard the bullshit header
    fhz = fhz.iloc[(header_index+1):, :]
    # Set real column names
    fhz.columns = colnames

    # Delete empty rows (e.g. only a category heading)
    fhz = fhz.loc[fhz['Artikelnummer'].notnull()]