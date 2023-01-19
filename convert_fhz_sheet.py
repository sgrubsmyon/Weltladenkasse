#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# Find index of last occurrence of item in list l:
def index_of_last(item, l):
    # Take reverse of the list
    ll = l[::-1]
    # Find first occurrence in the reversed list
    rev_index = ll.index(item)
    index = len(ll) - 1 - rev_index
    return index

def main():
    from optparse import OptionParser

    # install parser
    usage = "Usage: %prog   [OPTIONS]"
    parser = OptionParser(usage)

    parser.add_option("--fhz", type="string",
                      default='Bestellvorlage Lebensmittelpreisliste 3.0 2022.ods',
                      dest="FHZ",
                      help="The path to the FHZ .ods file. Output is written to the same filename, but with extension .csv.")
    parser.add_option("--only-arrows", action="store_true",
                      default=False,
                      dest="ARROWS",
                      help="Keep only rows with arrows (changes in price). Remove all other rows.")

    # get parsed args
    (options, args) = parser.parse_args()

    #############
    # Load data #
    #############

    import numpy as np
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
    prod_groups = fhz.loc[(fhz['Artikelnummer'].isnull()) & (fhz['Lieferant'].isnull()), 'Bezeichnung']
    prod_groups = prod_groups[prod_groups.notnull()]

    # Delete empty rows (e.g. only a group heading)
    fhz = fhz.loc[fhz['Artikelnummer'].notnull()]

    if options.ARROWS:
        # Delete rows not containing an arrow (indicating a price change)
        fhz = fhz.loc[fhz['Preisänderung'].notnull()]

    # For rows with empty "Lieferant": set to FHZ Rheinland
    fhz.loc[fhz['Lieferant'].isnull(), 'Lieferant'] = 'FHZ Rheinland'

    # Generate a column containing the FHZ product group for each product
    fhz.loc[:, 'Produktgruppe'] = '' # start with emtpy string and fill it later
    for i in fhz.index:
        pg_indices = prod_groups.index[prod_groups.index < i]
        # Actual product group is located at last index:
        prod_group = prod_groups.loc[pg_indices[-1]].strip()
        # Product super group is harder to find:
        # Find last occurrence of several product groups one after the other (adjacent rows), so last diff of 1
        pg_index_diffs = pg_indices[1:] - pg_indices[:-1]
        sg_index = index_of_last(1, pg_index_diffs.tolist())
        # Now add all indices of 1 that are directly preceding (all other adjacent product group rows), if any
        sg_indices = [sg_index]
        is_one = True
        while is_one and sg_index >= 0:
            sg_index = sg_index - 1
            is_one = pg_index_diffs[sg_index] == 1
            if is_one: sg_indices.append(sg_index)
        sg_indices.sort()
        super_group = ' - '.join([g.strip() for g in prod_groups.iloc[sg_indices]])
        pg = super_group + ' - ' + prod_group
        fhz.loc[i, 'Produktgruppe'] = pg

    # Delete all products in groups 'Pfand' and 'Pfandeimer' because we have a different Pfand system
    fhz = fhz.loc[(fhz['Produktgruppe'] != 'Erfrischungsgetränke - Pfand') & (fhz['Produktgruppe'] != 'Großpackungen/Unverpackt - Kaffee - Pfandeimer')]

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
    muskatnuss = fhz.Bezeichnung.str.contains('Muskatnu') # caveat: this only works if there is only one atching row
    string = fhz.loc[no_menge & muskatnuss].Bezeichnung.to_string()
    fhz.loc[no_menge & muskatnuss, 'Menge (kg/l/St.)'] = re.search(pattern, string).group(1)
    vanille = fhz.Bezeichnung.str.contains('Vanille Schoten') # caveat: this only works if there is only one atching row
    string = fhz.loc[no_menge & vanille].Bezeichnung.to_string()
    fhz.loc[no_menge & vanille, 'Menge (kg/l/St.)'] = re.search(pattern, string).group(1)

    # For debugging:
    # fhz.loc[no_einheit, ['Bezeichnung', 'Menge (kg/l/St.)', 'Einheit']]
    # fhz.loc[no_menge, ['Bezeichnung', 'Menge (kg/l/St.)', 'Einheit']]

    # Add missing columns:
    fhz['Bezeichnung | Einheit'] = fhz.Bezeichnung + ' | ' + \
        fhz['Menge (kg/l/St.)'].astype(int).astype(str) + ' ' + fhz.Einheit + ' ' + \
            fhz.Verpackung
    fhz['Kurzname'] = fhz.Bezeichnung
    fhz['Sortiment'] = ''
    fhz['Beliebtheit'] = ''
    fhz['Barcode'] = ''
    fhz['Setgröße'] = ''
    fhz['VK-Preis'] = ''
    fhz['Empf. VK-Preis'] = fhz['je Einheit']
    fhz['EK-Rabatt'] = ''
    fhz['EK-Preis'] = ''
    fhz['Variabel'] = 'Nein'
    fhz['Bestand'] = ''

    # Modify some columns:
    fhz.loc[(fhz.Einheit == 'g') | (fhz.Einheit == 'ml'), 'Menge (kg/l/St.)'] = \
        fhz.loc[(fhz.Einheit == 'g') | (fhz.Einheit == 'ml'), 'Menge (kg/l/St.)'] / 1000.0
    fhz.loc[fhz.Einheit == 'g', 'Einheit'] = 'kg'
    fhz.loc[fhz.Einheit == 'ml', 'Einheit'] = 'l'
    fhz['Sofort lieferbar'] = [ "Ja" if x == "x" else "Nein" for x in fhz['Sofort lieferbar'] ]

    # Use correct datatypes for columns
    artnummer_float = [ type(x) == float for x in fhz.Artikelnummer ]
    fhz.loc[artnummer_float, 'Artikelnummer'] = fhz.loc[artnummer_float, 'Artikelnummer'] \
        .astype(int).astype(str)
    fhz['Artikelnummer'] = fhz['Artikelnummer'].astype(str)
    fhz['Menge (kg/l/St.)'] = fhz['Menge (kg/l/St.)'].astype(float)
    fhz['VPE'] = fhz['VPE'].astype(int)
    fhz['Empf. VK-Preis'] = fhz['Empf. VK-Preis'].astype(float)

    # Reorder columns to be in the correct format that is needed
    fhz = fhz[[
        'Produktgruppe',
        'Lieferant',
        'Artikelnummer',
        'Bezeichnung | Einheit',
        'Kurzname',
        'Menge (kg/l/St.)',
        'Einheit',
        'Sortiment',
        'Sofort lieferbar',
        'Beliebtheit',
        'Barcode',
        'VPE',
        'Setgröße',
        'VK-Preis',
        'Empf. VK-Preis',
        'EK-Rabatt',
        'EK-Preis',
        'Variabel',
        'Herkunftsland',
        'Bestand'
    ]]

    # TODO check if file exists and ask if user wants it overwritten
    # Write out resulting CSV file
    fhz.to_csv(options.FHZ[:-3]+'csv', sep=';', index = False)
    # For testing:
    # fhz.to_csv('Bestellvorlage Lebensmittelpreisliste 3.0 2022.csv', sep=';', index = False)


if __name__ == '__main__':
    main()
