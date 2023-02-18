#!/usr/bin/env python3
# -*- coding: utf-8 -*-

def main():
    from optparse import OptionParser

    # install parser
    usage = "Usage: %prog   [OPTIONS]"
    parser = OptionParser(usage)

    # parser.add_option("--fhz", type="string",
    #                   default='Bestellvorlage Lebensmittelpreisliste 3.0 2022.ods',
    #                   dest="FHZ",
    #                   help="The path to the FHZ .ods file. Output is written to the same filename, but with extension .csv.")
    # parser.add_option("--only-arrows", action="store_true",
    #                   default=False,
    #                   dest="ARROWS",
    #                   help="Keep only rows with arrows (changes in price). Remove all other rows.")

    # get parsed args
    (options, args) = parser.parse_args()

    #############
    # Load data #
    #############

    import numpy as np
    import pandas as pd
    import re
    import os
    import warnings

    url = 'http://www.epsupport.de/download/Artikelliste_El_Puente_Master.xlsx'
    # url = 'prod_list_convert/ep/data/Artikelliste_El_Puente_Master.xlsx'
    ep = pd.read_excel(url)

    path = os.path.dirname(os.path.realpath(__file__))
    prod_group_dict = pd.read_csv(os.path.join(
        path, 'prod_group_dict_ep.csv'), sep=';', dtype=str)
    # prod_group_dict = pd.read_csv('prod_list_convert/ep/prod_group_dict_ep.csv', sep=';', dtype=str)

    # Filter out all obsolete products
    ep = ep.loc[ep.Rubrik != 'Ausgelistet']
    ep = ep.loc[ep['VK-Preis von'] > 0]
    ep = ep.loc[ep.Warengruppe != 'Transport, Versandkosten']
    # ep = ep.loc[(ep.Warengruppe != 'Verpackung') | (ep.Artikelgruppe != 'Verpackung')]
    ep = ep.loc[ep.Artikelnummer != 'XXX']
    ep = ep.loc[ep.Artikelnummer != 'KANTINE']
    # Excluce temporary price reductions ("Abverkauf"/"Angebot") whose article number ends with "A"
    pattern = re.compile(r'^[a-zA-Z0-9]{3}-[a-zA-Z0-9]{2}-[a-zA-Z0-9]{3}A$')
    ep = ep.loc[ep.Artikelnummer.map(lambda a: pattern.search(a) == None)]
    ep = ep.loc[ep['Bezeichnung 1'] != 'test']
    ep = ep.loc[ep['Bezeichnung 1'] != 'testlepaddd']
    ep = ep.loc[ep['Bezeichnung 2'] != 'Test 1']
    ep = ep.loc[ep['Bezeichnung 2'] != 'Test 2']
    ep = ep.loc[ep['Bezeichnung 2'] != 'Test 3']
    ep = ep.loc[ep['Bezeichnung 2'] != 'Test 4']
    # Exclude all Pfand articles because we have a different Pfand system
    ep = ep.loc[np.invert(ep.Artikelnummer.str.contains('PFAND'))]
    ep = ep.loc[ep.Artikelnummer != 'COLAFLASCHE']
    ep = ep.loc[ep.Artikelnummer != 'COLAKISTE']
    ep = ep.loc[ep.Artikelnummer != 'EINWEG']
    # Exclude "Etiketten" (labels)
    # pattern = re.compile(r'[0-9]{3}RS$')
    # ep = ep.loc[ep.Artikelnummer.map(lambda a: pattern.search(a) == None)]
    # pattern = re.compile(r'[0-9]{3}VS$')
    # ep = ep.loc[ep.Artikelnummer.map(lambda a: pattern.search(a) == None)]
    # pattern = re.compile(r'[0-9]{3}VR$')
    # ep = ep.loc[ep.Artikelnummer.map(lambda a: pattern.search(a) == None)]
    # pattern = re.compile(r'[0-9]{3}ITVR$')
    # ep = ep.loc[ep.Artikelnummer.map(lambda a: pattern.search(a) == None)]
    ep = ep.loc[np.invert(ep['Bezeichnung 1'].str.startswith('Rückenetikett'))]
    ep = ep.loc[np.invert(ep['Bezeichnung 1'].str.startswith('Rückeetikett'))]
    ep = ep.loc[np.invert(ep['Bezeichnung 1'].str.startswith('Rückenretikett'))]
    ep = ep.loc[np.invert(ep['Bezeichnung 1'].str.startswith('Vorderetikett'))]
    ep = ep.loc[np.invert(ep['Bezeichnung 1'].str.startswith('Vorderretikett'))]
    ep = ep.loc[np.invert(ep['Bezeichnung 1'].str.startswith('Zusatzetikett'))]
    ep = ep.loc[np.invert(ep['Bezeichnung 1'].str.startswith('46 Zusatzetikett'))]
    ep = ep.loc[np.invert(ep['Bezeichnung 1'].str.startswith('34 Zusatzetiket'))]
    
    # ep.to_excel('prod_list_convert/ep/test.xlsx') # look for strange things
    # pattern = re.compile(r'^[a-zA-Z0-9]{3}-[a-zA-Z0-9]{2}-[a-zA-Z0-9]+$')
    # ep.loc[ep.Artikelnummer.map(lambda a: pattern.search(a) == None)].to_excel('prod_list_convert/ep/test.xlsx') # look for strange things

    # Extract the product groups
    ep['Produktgruppe'] = ep['Hauptgruppe'] + ' - ' + \
        ep['Warengruppe'] + ' - ' + ep['Artikelgruppe']
    prod_groups = ep['Produktgruppe'].unique()
    # print(prod_groups) # Copy from terminal and insert into LibreOffice


    # Translate the EP product groups to WLB product groups using the dictionary.
    # Start with empty string by default for those without match.
    ep['Produktgruppe_WLB'] = ''
    for i in ep.index:
        pg_ep = ep.loc[i, 'Produktgruppe']
        pg_series = prod_group_dict.loc[prod_group_dict.EP == pg_ep, 'WLB']
        if len(pg_series) > 0:
            pg = pg_series.iloc[0]  # get first element
            ep.loc[i, 'Produktgruppe_WLB'] = pg
        else:
            warnings.warn(
                f'EP-Produktgruppe "{pg_ep}" bisher unbekannt!!! Bitte in `prod_group_dict_ep.csv` eintragen!')

    # Set missing values for products without 'Einheit'
    no_gewichteinheit = ep.Gewichteinheit.isnull()
    no_mengenschluessel = ep.Mengenschlüssel.isnull()
    ep['Einheit'] = 'St.' # default value
    # Take Gewichteinheit if not null: (primary)
    ep.loc[~no_gewichteinheit, 'Einheit'] = ep.loc[~no_gewichteinheit, 'Gewichteinheit']
    # Take Mengenschlüssel if not null: (secondary)
    ep.loc[no_gewichteinheit & ~no_mengenschluessel, 'Einheit'] = ep.loc[no_gewichteinheit & ~no_mengenschluessel, 'Mengenschlüssel']
    
    # Set missing values of 'Menge'
    ep['Menge (kg/l/St.)'] = 1 # default value
    no_gewicht = (ep.Gewicht.isnull()) | (ep.Gewicht <= 0)
    ep.loc[~no_gewicht, 'Menge (kg/l/St.)'] = ep.loc[~no_gewicht, 'Gewicht']
############################
    # XXX Continue here
    # If "Einheit" = "Set", try to parse set size from Bezeichnung Fließtext with regex r/[0-9]+er.Set/
    pattern = re.compile(r'^.*([0-9]+)er.Set.*$')
    set_products = ep['Bezeichnung Fließtext'].map(lambda bez: pattern.search(bez) != None)
    guessed_setsize = ep.loc[set_products, 'Bezeichnung Fließtext'].map(lambda bez: int(re.sub(r'^.*([0-9]+)er.Set.*$', r'\1', bez)))
    ep.loc[set_products, 'Menge (kg/l/St.)'] = guessed_setsize
    ep.loc[set_products, 'Einheit'] = 'St.'
    
    # Convert 'g' to 'kg'
    # Convert 'ml' to 'l'

    # For debugging:
    # ep.loc[no_gewichteinheit, ['Bezeichnung Fließtext', 'Gewicht', 'Gewichteinheit']]
    # ep.loc[no_mengenschluessel, ['Bezeichnung Fließtext', 'Gewicht', 'Gewichteinheit']]
    # ep.iloc[1:30][['Artikelnummer', 'Gewichteinheit', 'Mengenschlüssel', 'Einheit']]
    # ep.loc[~no_gewicht, ['Menge (kg/l/St.)', 'Bezeichnung Fließtext', 'Gewicht']]

    # Add missing columns:
    ep['Bezeichnung | Einheit'] = ep['Bezeichnung Fließtext'] + ' | ' + \
        np.where(ep['Menge (kg/l/St.)'].notnull(), ep['Menge (kg/l/St.)'].astype(int).astype(str), '') + \
        np.where(ep.Einheit.notnull(), ' ' + ep.Einheit, '') + \
        np.where(ep.Mengenschlüssel.notnull(), ' ' + ep.Mengenschlüssel, '')
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
        fhz.loc[(fhz.Einheit == 'g') | (fhz.Einheit == 'ml'),
                'Menge (kg/l/St.)'] / 1000.0
    fhz.loc[fhz.Einheit == 'g', 'Einheit'] = 'kg'
    fhz.loc[fhz.Einheit == 'ml', 'Einheit'] = 'l'
    fhz['Sofort lieferbar'] = ["Ja" if x ==
                               "x" else "Nein" for x in fhz['Sofort lieferbar']]

    # Use correct datatypes for columns
    artnummer_float = [type(x) == float for x in fhz.Artikelnummer]
    fhz.loc[artnummer_float, 'Artikelnummer'] = fhz.loc[artnummer_float, 'Artikelnummer'] \
        .astype(int).astype(str)
    fhz['Artikelnummer'] = fhz['Artikelnummer'].astype(str)
    fhz['Menge (kg/l/St.)'] = fhz['Menge (kg/l/St.)'].astype(float)
    fhz['VPE'] = fhz['VPE'].astype(int)
    fhz['Empf. VK-Preis'] = fhz['Empf. VK-Preis'].astype(float)

    # Rename columns
    fhz = fhz.rename(
        columns={'Produktgruppe': 'Produktgruppe_FHZ'})
    fhz = fhz.rename(
        columns={'Produktgruppe_WLB': 'Produktgruppe'})

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

    # Special treatment of certain products
    # Mini-Schoko-Täfelchen Großpackung GEPA
    minis = [fhz.Artikelnummer == '8901827', fhz.Artikelnummer == '8901828']
    for mini in minis:
        # VPE ist zwar 5, aber auf 1 lassen, weil wir sowieso keinen Rabatt kriegen und 500 Täfelchen ein MHD-Problem verursachen
        fhz.loc[mini, 'VPE'] = 1
        fhz.loc[mini, 'Menge (kg/l/St.)'] = 100.0
        fhz.loc[mini, 'Einheit'] = 'St.'

    # TODO check if file exists and ask if user wants it overwritten
    # Write out resulting CSV file
    fhz.to_csv(options.FHZ[:-3]+'csv', sep=';', index=False)
    # For testing:
    # fhz.to_csv('Bestellvorlage Lebensmittelpreisliste 3.0 2022.csv', sep=';', index = False)


if __name__ == '__main__':
    main()
