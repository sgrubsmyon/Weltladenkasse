#!/usr/bin/env python3
# -*- coding: utf-8 -*-

def main():
    from optparse import OptionParser

    # install parser
    usage = "Usage: %prog   [OPTIONS]"
    parser = OptionParser(usage)

    parser.add_option("--output", type="string",
                      default='Artikelliste_El_Puente_Master.csv',
                      dest="OUTPUT",
                      help="The path to the output .csv file being written to.")
    # parser.add_option("--only-arrows", action="store_true",
    #                   default=False,
    #                   dest="ARROWS",
    #                   help="Keep only rows with arrows (changes in price). Remove all other rows.")

    # get parsed args
    (options, args) = parser.parse_args()

    print(f'Writing result to CSV file "{options.OUTPUT}".')

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
    ep = ep.loc[np.invert(
        ep['Bezeichnung 1'].str.startswith('Rückenretikett'))]
    ep = ep.loc[np.invert(ep['Bezeichnung 1'].str.startswith('Vorderetikett'))]
    ep = ep.loc[np.invert(
        ep['Bezeichnung 1'].str.startswith('Vorderretikett'))]
    ep = ep.loc[np.invert(ep['Bezeichnung 1'].str.startswith('Zusatzetikett'))]
    ep = ep.loc[np.invert(
        ep['Bezeichnung 1'].str.startswith('46 Zusatzetikett'))]
    ep = ep.loc[np.invert(
        ep['Bezeichnung 1'].str.startswith('34 Zusatzetiket'))]

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
    ep['Einheit'] = 'St.'  # default value
    # Take Gewichteinheit if not null: (primary)
    no_gewichteinheit = ep.Gewichteinheit.isnull()
    ep.loc[~no_gewichteinheit,
           'Einheit'] = ep.loc[~no_gewichteinheit, 'Gewichteinheit']
    # # Take Mengenschlüssel if not null: (secondary)
    # no_mengenschluessel = ep.Mengenschlüssel.isnull()
    # ep.loc[no_gewichteinheit & ~no_mengenschluessel, 'Einheit'] = ep.loc[no_gewichteinheit & ~no_mengenschluessel, 'Mengenschlüssel']

    # Set missing values of 'Menge'
    ep['Menge (kg/l/St.)'] = 1  # default value
    no_gewicht = (ep.Gewicht.isnull()) | (ep.Gewicht <= 0)
    ep.loc[~no_gewicht, 'Menge (kg/l/St.)'] = ep.loc[~no_gewicht, 'Gewicht']
    # If "Einheit" = "Set", try to parse set size from Bezeichnung Fließtext with regex r/[0-9]+er.Set/
    pattern = re.compile(r'^.*([0-9]+)er.Set.*$')
    set_products = ep['Bezeichnung Fließtext'].map(
        lambda bez: pattern.search(bez) != None)
    guessed_setsize = ep.loc[set_products, 'Bezeichnung Fließtext'].map(
        lambda bez: int(re.sub(r'^.*([0-9]+)er.Set.*$', r'\1', bez)))
    ep.loc[set_products, 'Menge (kg/l/St.)'] = guessed_setsize
    ep.loc[set_products, 'Einheit'] = 'St.'

    # Convert 'g' to 'kg'
    ep.loc[ep.Einheit == 'g',
           'Menge (kg/l/St.)'] = ep.loc[ep.Einheit == 'g', 'Menge (kg/l/St.)'] / 1000
    ep.loc[ep.Einheit == 'g', 'Einheit'] = 'kg'
    # Convert 'ml' to 'l'
    ep.loc[ep.Einheit == 'ml',
           'Menge (kg/l/St.)'] = ep.loc[ep.Einheit == 'ml', 'Menge (kg/l/St.)'] / 1000
    ep.loc[ep.Einheit == 'ml', 'Einheit'] = 'l'

    # We don't want no 'Set' in 'Einheit'
    ep.loc[ep.Einheit == 'Set', 'Einheit'] = 'St.'

    # Now, we should have only 'kg', 'l', and 'St.' left in 'Einheit'

    # For debugging:
    # ep.loc[no_gewichteinheit, ['Bezeichnung Fließtext', 'Gewicht', 'Gewichteinheit']]
    # ep.loc[no_mengenschluessel, ['Bezeichnung Fließtext', 'Gewicht', 'Gewichteinheit']]
    # ep.iloc[1:30][['Artikelnummer', 'Gewichteinheit', 'Mengenschlüssel', 'Einheit']]
    # ep.loc[~no_gewicht, ['Menge (kg/l/St.)', 'Bezeichnung Fließtext', 'Gewicht']]

############################
    # Add missing columns:

    ep['Lieferant'] = 'El Puente'
    # Make joined Bezeichnung, but better than 'Bezeichnung Fließtext'

    def rem_trailing_comma(s): return re.sub(
        r',+$', r'', str(s).strip()).strip()
    ep['Bezeichnung'] = [
        ', '.join([
            bez for bez in [
                rem_trailing_comma(bez1),
                rem_trailing_comma(bez2),
                rem_trailing_comma(bez3),
                rem_trailing_comma(bez4)
            ] if str(bez) != 'nan' and not bez == ''
        ])
        for bez1, bez2, bez3, bez4 in zip(
            ep['Bezeichnung 1'],
            ep['Bezeichnung 2'],
            ep['Bezeichnung 3'],
            ep['Bezeichnung 4']
        )
    ]
    ep['Bezeichnung | Einheit'] = ep['Bezeichnung'] + ' | ' + \
        np.where(ep.Gewicht.notnull() & (ep.Gewicht > 0), ep.Gewicht.apply('{0:g}'.format),
                 np.where(ep['Menge (kg/l/St.)'].notnull(), ep['Menge (kg/l/St.)'].apply('{0:g}'.format), '')) + \
        np.where(ep.Gewicht.notnull() & (ep.Gewicht > 0) & ep.Gewichteinheit.notnull(), ' ' + ep.Gewichteinheit,
                 np.where(ep.Einheit.notnull(), ' ' + ep.Einheit, '')) + \
        np.where(ep.Gewicht.notnull() & (ep.Gewicht > 0) & ep.Gewichteinheit.notnull(
        ) & ep.Mengenschlüssel.notnull(), ' ' + ep.Mengenschlüssel, '')
    ep['Kurzname'] = ep['Bezeichnung 1']
    ep['Sortiment'] = ''
    ep['Beliebtheit'] = ''
    ep['Barcode'] = ep['EAN-Produkt']
    ep['Setgröße'] = ''
    ep['VK-Preis'] = ep['VK-Preis bis']
    ep['Empf. VK-Preis'] = ep['VK-Preis von']
    ep['EK-Rabatt'] = ''
    ep['EK-Preis'] = ''
    ep['Variabel'] = 'Nein'
    ep['Bestand'] = ''
    ep['Sofort lieferbar'] = ''

    # Use correct datatypes for columns
    # ep['Artikelnummer'] = ep['Artikelnummer'].astype(str)
    ep['VPE'] = np.nan_to_num(ep['VPE']).astype(int)
    ep['Barcode'] = ep['Barcode'].astype(int)

    # Rename columns
    ep = ep.rename(
        columns={'Produktgruppe': 'Produktgruppe_EP'})
    ep = ep.rename(
        columns={'Produktgruppe_WLB': 'Produktgruppe'})

    # Reorder columns to be in the correct format that is needed
    ep = ep[[
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
    # ...

    # TODO check if file exists and ask if user wants it overwritten
    # Write out resulting CSV file
    ep.to_csv(options.OUTPUT, sep=';', index=False)
    # For testing:
    # fhz.to_csv('Bestellvorlage Lebensmittelpreisliste 3.0 2022.csv', sep=';', index = False)


if __name__ == '__main__':
    main()
