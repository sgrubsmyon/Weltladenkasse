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
                      help="The path to the output '.csv' file being written to. It will also be written to the same filenane with extension '.xlsx'.")
    # parser.add_option("--only-arrows", action="store_true",
    #                   default=False,
    #                   dest="ARROWS",
    #                   help="Keep only rows with arrows (changes in price). Remove all other rows.")

    # get parsed args
    (options, args) = parser.parse_args()

    xlsx_output = options.OUTPUT[:-3] + 'xlsx'
    print(
        f'Writing result to CSV file "{options.OUTPUT}" and "{xlsx_output}".')

    #############
    # Load data #
    #############

    import numpy as np
    import pandas as pd
    import requests
    import re
    import os
    import warnings

    # Download file for parsing with pandas
    url = 'http://www.epsupport.de/download/Artikelliste_El_Puente_Master.xlsx'
    # url = 'prod_list_convert/ep/data/Artikelliste_El_Puente_Master.xlsx'
    # url = 'Artikelliste_El_Puente_Master.xlsx'
    ep = pd.read_excel(url)

    # As service to user, also download and store the original .xlsx file
    myfile = requests.get(url)
    open(xlsx_output, 'wb').write(myfile.content)

    path = os.path.dirname(os.path.realpath(__file__))
    # path = "/home/mvoge/code/Weltladenkasse/git/prod_list_convert/ep"
    prod_group_dict = pd.read_csv(os.path.join(
        path, 'prod_group_dict_ep.csv'), sep=';', dtype=str)
    # prod_group_dict = pd.read_csv('prod_list_convert/ep/prod_group_dict_ep.csv', sep=';', dtype=str)

    # Filter out all obsolete products
    ep = ep.loc[ep.Rubrik != 'Ausgelistet']
    ep = ep.loc[ep['VK-Preis von'] > 0]
    ep = ep.loc[ep.Warengruppe != 'Transport, Versandkosten']
    # ep = ep.loc[(ep.Warengruppe != 'Verpackung') | (ep.Artikelgruppe != 'Verpackung')]
    ep = ep.loc[(ep.Hauptgruppe != 'Handwerk') |
                (ep.Warengruppe != 'Verpackung')]
    ep = ep.loc[ep.Artikelnummer != 'XXX']
    # Excluce all article numbers starting with at least 4 consecutive letters without numbers
    pattern = re.compile(r'^[a-zA-Z]{4}')
    ep = ep.loc[ep.Artikelnummer.map(lambda a: pattern.search(a) == None)]
    # Excluce temporary price reductions ("Abverkauf"/"Angebot") whose article number ends with "A"
    pattern = re.compile(r'^[a-zA-Z0-9]{3}-[a-zA-Z0-9]{2}-[a-zA-Z0-9]{3}A$')
    ep = ep.loc[ep.Artikelnummer.map(lambda a: pattern.search(a) == None)]
    ep = ep.loc[ep['Bezeichnung 1'] != 'test']
    ep = ep.loc[ep['Bezeichnung 1'] != 'testlepaddd']
    ep = ep.loc[ep['Bezeichnung 2'] != 'Test 1']
    ep = ep.loc[ep['Bezeichnung 2'] != 'Test 2']
    ep = ep.loc[ep['Bezeichnung 2'] != 'Test 3']
    ep = ep.loc[ep['Bezeichnung 2'] != 'Test 4']
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

    # Convert article numbers in wrong format (no minus signs) to correct format
    pattern = re.compile(r'^[a-zA-Z]{2}[a-zA-Z0-9][0-9]{5}$')
    wrong_artnum_filter = ep.Artikelnummer.map(
        lambda a: pattern.search(a) != None)

    # Correct the wrong formatted article numbers:
    # correct_artnum = ep.loc[wrong_artnum_filter, "Artikelnummer"].map(
    #     lambda a: re.sub(
    #         r'^([a-zA-Z]{2}[a-zA-Z0-9])([0-9]{2})([0-9]{3})', r'\1-\2-\3', a)
    # )
    # correct_artnum_filter = ep.Artikelnummer.map(lambda a: a in correct_artnum.tolist())
    # ep.loc[wrong_artnum_filter, "Artikelnummer"] = correct_artnum

    # Compare:
    # Wrong article number format:
    # wrong_df = ep.loc[wrong_artnum_filter, ["Artikelnummer", "VK-Preis bis"]]
    # wrong_df = wrong_df.rename(columns = {"VK-Preis bis": "VK-Preis wrong"})
    # ### Same article number in correct format: (duplicates)
    # correct_df = ep.loc[correct_artnum_filter, ["Artikelnummer", "VK-Preis bis"]]
    # correct_df = correct_df.rename(columns = {"VK-Preis bis": "VK-Preis correct"})
    # # These are duplicates, present both in "wrong" and "correct" format.
    # # It seems that the "wrong formatted" articles are simply outdated (usually lower price, can check with https://shop.el-puente.de/art_number):
    # wrong_df.merge(correct_df, on = "Artikelnummer")
    # # What about "wrong formatted" articles not listed in correct format? (they do not have duplicates)
    # list(filter(lambda a: a not in correct_df.Artikelnummer.tolist(), wrong_df.Artikelnummer))
    # # It seems all except two (BO1-10-201 and BO1-10-202, which is Dinkel-Quinoa-Gebäck) do not exist anymore in
    # # shop.el-puente.de and there is no "correct formatted" duplicate because the correct duplicate is "Ausgelistet"
    # # and was filtered out.

    # Decision: remove all the articles with wrong formatted article numbers
    ep = ep.loc[~wrong_artnum_filter]

    # ep.to_excel('prod_list_convert/ep/test.xlsx') # look for strange things
    # pattern = re.compile(r'^[a-zA-Z0-9]{3}-[a-zA-Z0-9]{2}-[a-zA-Z0-9]+$')
    # ep.loc[ep.Artikelnummer.map(lambda a: pattern.search(a) == None)].to_excel('prod_list_convert/ep/test.xlsx') # look for strange things

    # Extract the product groups
    ep['Produktgruppe'] = ep['Hauptgruppe'] + ' - ' + \
        ep['Warengruppe'] + ' - ' + ep['Artikelgruppe']
    # prod_groups = ep['Produktgruppe'].unique()
    # print(prod_groups) # Copy from terminal and insert into LibreOffice

    # Translate the EP product groups to WLB product groups using the dictionary.
    # Start with empty string by default for those without match.
    ep['Produktgruppe_WLB'] = ''
    for i in ep.index:
        pg_ep = ep.loc[i, 'Produktgruppe']
        # Special cases of product group depending on parameters of the product:
        if pg_ep == 'Lebensmittel - Getränke - Säfte (19% Mwst.)':
            if ep.loc[i, 'Gewicht'] == 330:  # 330 ml --> 8 Cent Pfand
                ep.loc[i, 'Produktgruppe_WLB'] = 'Alkoholfreie Getränke 8 Cent Pfand'
            elif ep.loc[i, 'Gewicht'] == 1000:  # 1 Liter --> 15 Cent Pfand
                ep.loc[i, 'Produktgruppe_WLB'] = 'Alkoholfreie Getränke 15 Cent Pfand'
            else:
                ep.loc[i, 'Produktgruppe_WLB'] = 'Sonstige Getränke'
        else:
            # Normal case: (can use the dictionary to look up product group)
            pg_series = prod_group_dict.loc[prod_group_dict.EP == pg_ep, 'WLB']
            if len(pg_series) > 0:
                pg = pg_series.iloc[0]  # get first element
                ep.loc[i, 'Produktgruppe_WLB'] = pg
            else:
                warnings.warn(
                    f'EP-Produktgruppe "{pg_ep}" bisher unbekannt!!! Bitte in `prod_group_dict_ep.csv` eintragen!')

    # Adjustments of the product group
    # Seasonal articles inside the group "Dekoartikel" (mapped from "Kleindeko")
    xmas_pattern = re.compile(r'Weihnacht|Baumanhänger|Engel|Krippe')
    easter_pattern = re.compile(r'Oster|\"Ei\"|\bEi\b')
    ep.loc[
        (ep['Bezeichnung 1'].map(lambda a: xmas_pattern.search(a) != None)),
        "Produktgruppe_WLB"
    ] = "KHW Weihnachten"
    ep.loc[
        (ep['Bezeichnung 1'].map(lambda a: easter_pattern.search(a) != None)),
        "Produktgruppe_WLB"
    ] = "KHW Ostern"

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
    ep.loc[ep.VPE.isna(), 'VPE'] = 1
    ep['VPE'] = ep['VPE'].astype(int)
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


if __name__ == '__main__':
    main()
