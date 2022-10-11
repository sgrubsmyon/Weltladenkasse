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
    import re

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

    # Extract the product groups
    prod_groups = fhz.loc[fhz['Artikelnummer'].isnull(), 'Bezeichnung']
    prod_groups = prod_groups[prod_groups.notnull()]

    # Delete empty rows (e.g. only a group heading)
    fhz = fhz.loc[fhz['Artikelnummer'].notnull()]

    # For rows with empty "Lieferant": set to FHZ Rheinland
    fhz.loc[fhz['Lieferant'].isnull(), 'Lieferant'] = 'FHZ Rheinland'
    
    # Generate a column containing the FHZ product group for each product
    fhz.loc[:, 'Produktgruppe'] = [prod_groups.loc[prod_groups.index < i].tolist()[-1] for i in fhz.index]

    # Delete all products in groups 'Pfand' and 'Pfandeimer' because we have a different Pfand system
    fhz = fhz.loc[(fhz['Produktgruppe'] != 'Pfand') & (fhz['Produktgruppe'] != 'Pfandeimer')]

    # Set missing values for products without 'Einheit'
    no_einheit = fhz.Einheit.isnull()
    fhz.loc[no_einheit, 'Einheit'] = 'St.' # default value
    # deviations from default:
    fhz.loc[no_einheit & (fhz.Bezeichnung.str.contains('Mango-Monkeys')), 'Einheit'] = 'g'
    fhz.loc[no_einheit & (fhz.Bezeichnung.str.contains('Mandeln geröstet & gesalzen')), 'Einheit'] = 'g'

    # Set missing values of 'Menge'
    no_menge = fhz['Menge (kg/l/St.)'].isnull()
    fhz.loc[no_menge, 'Menge (kg/l/St.)'] = 1 # default value
    # deviations from default
    pattern = re.compile(r'(\b[0-9]+) St')
    muskatnuss = fhz.Bezeichnung.str.contains('Muskatnu')
    str = fhz.loc[no_menge & muskatnuss].Bezeichnung.to_string()
    fhz.loc[no_menge & muskatnuss, 'Menge (kg/l/St.)'] = re.search(pattern, str).group(1)
    vanille = fhz.Bezeichnung.str.contains('Vanille Schoten')
    str = fhz.loc[no_menge & vanille].Bezeichnung.to_string()
    fhz.loc[no_menge & vanille, 'Menge (kg/l/St.)'] = re.search(pattern, str).group(1)

    # fhz.loc[no_einheit, ['Bezeichnung', 'Menge (kg/l/St.)', 'Einheit']]
    # fhz.loc[no_menge, ['Bezeichnung', 'Menge (kg/l/St.)', 'Einheit']]


    # Write out resulting CSV file
    fhz.to_csv(options.FHZ[:-3]+'csv', sep=';')