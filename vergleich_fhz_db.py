#!/usr/bin/env python

import sys
import numpy as np
import pandas as pd
from decimal import Decimal

fhz_filename = 'Artikelliste_Bestellvorlage_Lebensmittelpreisliste_1.2-2015.csv'
wlb_filename = 'Artikelliste_DB_Dump_2015_KW40_LM.csv'

def indexDuplicationCheck(df):
    '''
    Returns all indices of DataFrame `df` that have duplicates, as a list.
    Index consists of tuples ('Lieferant', 'Artikel-Nr.').
    '''
    idx = df.index.tolist()
    dup_indices = set()
    for i in idx:
        count = 0
        for ii in idx:
            if i == ii:
                count += 1
        if count != 1:
            dup_indices.add(i)
    dup_indices = list(dup_indices)
    return dup_indices


def returnRoundedPrice(preis):
    '''
    `preis`: Price as Decimal
    '''
    preis_str = str(preis)
    if ('.' in preis_str):
        cent_index = preis_str.index('.') + 2
        if ( (len(preis_str) > cent_index) and
                (preis_str[cent_index] == '9') ):
            # round up, i.e. add one cent:
            preis += Decimal('0.01')
        elif ( (len(preis_str) > cent_index) and
                (preis_str[cent_index] == '8') ):
            # round up, i.e. add one cent:
            preis += Decimal('0.02')
        elif ( (len(preis_str) > cent_index) and
                (preis_str[cent_index-1:cent_index+1] == '45') ):
            # round up, i.e. add one cent:
            preis += Decimal('0.05')
    return preis


def writeOutAsCSV(df, filename, only_index=False):
    out = df.reset_index()
    c = out.columns.tolist()
    # change column order: first Produktgruppe, then index (Lieferant,
    # Artikelnummer)
    c = c[2:3] + c[0:2] + c[3:]
    out = out.reindex_axis(c, axis=1)
    if only_index:
        out.to_csv(filename, sep=';', index=False, columns=(c[1], c[2]))
    else:
        out.to_csv(filename, sep=';', index=False)


def removeEmptyRow(df):
    index = df.index.tolist()
    mask = []
    for i in index:
        if i == ('', ''):
            mask.append(False)
        else:
            mask.append(True)
    return df[mask]



# load data
fhz = pd.read_csv(fhz_filename, sep=';', dtype=str, index_col=(1, 2))
#   input CSV list must contain only Lebensmittel and Getränke (LM) and not
#   Kunsthandwerk and other (KHW), if that's the case also for the FHZ CSV list
wlb = pd.read_csv(wlb_filename, sep=';', dtype=str, index_col=(1, 2))

# Check for duplicates in fhz:
fhz_dup_indices = indexDuplicationCheck(fhz)
print("Folgende Artikel kommen mehrfach in '%s' vor:" % fhz_filename)
for i in fhz_dup_indices:
    print(fhz.loc[i])
    print("---------------")
print("---------------")

# Remove all newlines ('\n') from all fields
fhz.replace(to_replace='\n', value=', ', inplace=True, regex=True)
wlb.replace(to_replace='\n', value=', ', inplace=True, regex=True)

# homogenize data
# print Lieferanten:
print('\n')
print("WLB:", set(map(lambda i: i[0], wlb.index)))
print("FHZ:", set(map(lambda i: i[0], fhz.index)))
fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('Fairtrade Center Breisgau', i[1])
    if i[0] == 'ftc' else i, fhz.index.tolist())), names=fhz.index.names)
fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('Café Libertad', i[1])
    if i[0] == 'Café\nLibertad' else i, fhz.index.tolist())), names=fhz.index.names)
fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('Ethiquable', i[1])
    if i[0] == 'ethiquable' else i, fhz.index.tolist())), names=fhz.index.names)
fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('Libera Terra', i[1])
    if i[0] == 'Libera\nTerra' else i, fhz.index.tolist())), names=fhz.index.names)
fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('unbekannt', i[1])
    if type(i[0]) == float and np.isnan(i[0]) else i, fhz.index.tolist())), names=fhz.index.names)
print("FHZ neu:", set(map(lambda i: i[0], fhz.index)))

# Adopt values from FHZ (for existing articles):
count = 0
print('\n')
wlb_neu = wlb.copy()
geaenderte_preise = pd.DataFrame(columns=wlb_neu.columns,
        index=pd.MultiIndex.from_tuples([('','')], names=wlb_neu.index.names))
# Loop over fhz numerical index
for i in range(len(fhz)):
    fhz_row = fhz.iloc[i]
    fhz_preis = Decimal(fhz_row['Empf. VK-Preis'])
    name = fhz_row.name
    try:
        wlb_row = wlb_neu.loc[name]
        # adopt the rec. sale price and the "Lieferbarkeit" directly and completely
        # See
        # http://pandas.pydata.org/pandas-docs/stable/indexing.html#indexing-view-versus-copy,
        # very bottom
        wlb_neu.loc[name, 'Empf. VK-Preis'] = str(fhz_preis)
        wlb_neu.loc[name, 'Sofort lieferbar'] = fhz_row['Sofort lieferbar']
        wlb_preis = Decimal(wlb_row['VK-Preis'])
        setgroesse = int(wlb_row['Setgröße'])

        # adopt the sale price only if significantly changed:
        if not fhz_preis.is_nan() and not wlb_preis.is_nan():
            if (fhz_preis >= Decimal('2.')*wlb_preis) or (setgroesse > 1):
                # price is at least twice, this indicates that this is a set:
                print("Setgröße > 1 detektiert.")
                print("WLB-Preis:", wlb_preis, "FHZ-Preis:", fhz_preis,
                        "(%s)" % fhz_row['Bezeichnung | Einheit'])
                fhz_preis = round(fhz_preis / setgroesse, 2)
                print("Alte (WLB) setgroesse:", setgroesse,
                        "   (Bitte prüfen, ob korrekt!)")
                print("Ändere FHZ-Preis zu FHZ-Preis / %s = %s" % (setgroesse,
                    fhz_preis))
                print("")
            if ( abs(fhz_preis - wlb_preis) > 0.021 ):
                # price seems to deviate more than usual
                count += 1
                if wlb_row['Sortiment'] == 'Ja':
                    print("Alter (WLB) Preis: %s   Neuer (FHZ) Preis: %s\n"
                            "FHZ: %s   (%s)    Sortiment: %s\n"
                            "WLB: %s   (%s)    Sortiment: %s" % (wlb_preis, fhz_preis,
                                fhz_row['Bezeichnung | Einheit'], name, fhz_row['Sortiment'],
                                wlb_row['Bezeichnung | Einheit'], name, wlb_row['Sortiment']))
                fhz_preis = returnRoundedPrice(fhz_preis)
                if wlb_row['Sortiment'] == 'Ja':
                    print("Ändere Preis von:", str(wlb_preis), " zu:", str(fhz_preis))
                wlb_neu.loc[name, 'VK-Preis'] = str(fhz_preis)
                geaenderte_preise = geaenderte_preise.append(wlb_row)
                if wlb_row['Sortiment'] == 'Ja':
                    print("---------------")
        # adopt the article name
        wlb_neu.loc[name, 'Bezeichnung | Einheit'] = fhz_row['Bezeichnung | Einheit']
    except KeyError:
        pass
print(count, "Artikel haben geänderten Preis.")

# Round up all articles' prices:
count = 0
print('\n')
for i in range(len(wlb_neu)):
    wlb_row = wlb_neu.iloc[i]
    name = wlb_row.name
    alter_preis = Decimal(wlb_row['VK-Preis'])
    neuer_preis = returnRoundedPrice(alter_preis)
    if (not alter_preis.is_nan() and not neuer_preis.is_nan() and
            neuer_preis != alter_preis):
        count += 1
        print("Runde Preis von:", str(alter_preis), " zu:", str(neuer_preis),
                '(%s, %s)' % (wlb_row['Bezeichnung | Einheit'],
                wlb_row['Sortiment']))
        wlb_neu.loc[name, 'VK-Preis'] = str(neuer_preis)
        geaenderte_preise = geaenderte_preise.append(wlb_row)
print(count, "VK-Preise wurden gerundet.")
geaenderte_preise = removeEmptyRow(geaenderte_preise)

# Check for duplicates in geaenderte_preise:
gp_dup_indices = indexDuplicationCheck(geaenderte_preise)
print("Folgende Artikel kommen mehrfach in 'geaenderte_preise' vor:")
for i in gp_dup_indices:
    print(geaenderte_preise.loc[i])

writeOutAsCSV(geaenderte_preise, 'test_geänderte_preise.csv', only_index=True)
mask = geaenderte_preise['Sortiment'] == 'Ja'
gp_sortiment = geaenderte_preise[mask]
writeOutAsCSV(gp_sortiment, 'test_geänderte_preise_sortiment.csv',
        only_index=True)

count = 0
print('\n')
for i in range(len(fhz)):
    fhz_row = fhz.iloc[i]
    fhz_preis = Decimal(fhz_row['Empf. VK-Preis'])
    try:
        wlb_row = wlb_neu.loc[name]
        wlb_preis = Decimal(wlb_row['Empf. VK-Preis'])
    except KeyError:
        wlb_preis = Decimal(np.nan)
    if ( not fhz_preis.is_nan() and not wlb_preis.is_nan() and
            abs(fhz_preis - wlb_preis) > 0.021 ):
        count += 1
        print('FHZ: %s WLB: %s' % (fhz_preis, wlb_preis),
                '(%s) (%s)' % (fhz_row['Bezeichnung | Einheit'],
                    wlb_row['Bezeichnung | Einheit']))
print(count, "Differenzen gefunden.")
writeOutAsCSV(wlb_neu, 'test.csv')

# Add new, so far unknown articles from FHZ
# Get empty df:
count = 0
print('\n')
wlb_neue_artikel = pd.DataFrame(columns=wlb_neu.columns,
        index=pd.MultiIndex.from_tuples([('','')], names=wlb_neu.index.names))
for i in range(len(fhz)):
    fhz_row = fhz.iloc[i]
    fhz_preis = Decimal(fhz_row['Empf. VK-Preis'])
    name = fhz_row.name
    try:
        wlb_neu.loc[name]
    except KeyError:
        count += 1
        # From:
        # http://stackoverflow.com/questions/10715965/add-one-row-in-a-pandas-dataframe
        wlb_neue_artikel = wlb_neue_artikel.append(fhz_row)
        fhz_preis = returnRoundedPrice(fhz_preis)
        wlb_neue_artikel.loc[name, 'VK-Preis'] = str(fhz_preis)
        print('"%s" nicht in WLB. (%s)' % (fhz_row['Bezeichnung | Einheit'], name))
print(count, "Artikel nicht in WLB.")
wlb_neue_artikel = removeEmptyRow(wlb_neue_artikel)
writeOutAsCSV(wlb_neue_artikel, 'test_neue_artikel.csv')

# Show list of articles missing in FHZ (so not orderable!)
count = 0
print('\n')
# Loop over wlb numerical index
for i in range(len(wlb_neu)):
    wlb_row = wlb_neu.iloc[i]
    name = wlb_row.name
    try:
        fhz.loc[name]
    except KeyError:
        count += 1
        print('"%s" nicht in FHZ. (%s)' % (wlb_row['Bezeichnung | Einheit'], name))
print(count, "Artikel nicht in FHZ.")
