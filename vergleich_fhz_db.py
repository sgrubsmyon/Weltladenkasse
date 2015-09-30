#!/usr/bin/env python

import sys
import numpy as np
import pandas as pd
from decimal import Decimal

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
    return preis


def writeOutAsCSV(df, filename):
    out = df.reset_index()
    c = out.columns.tolist()
    # change column order: first Produktgruppe, then index (Lieferant,
    # Artikelnummer)
    c = c[2:3] + c[0:2] + c[3:]
    out = out.reindex_axis(c, axis=1)
    out.to_csv(filename, sep=';', index=False)


# load data
fhz = pd.read_csv('Artikelliste_Bestellvorlage_Lebensmittelpreisliste_1.2-2015.csv',
        sep=';', dtype=str, index_col=(1,2))
#   input CSV list must contain only Lebensmittel and Getränke (LM) and not
#   Kunsthandwerk and other (KHW), if that's the case also for the FHZ CSV list
wlb = pd.read_csv('Artikelliste_DB_Dump_2015_KW39_LM.csv', sep=';', dtype=str,
        index_col=(1,2))

# Remove all newlines ('\n') from all fields
fhz.replace(to_replace='\n', value=', ', inplace=True, regex=True)
wlb.replace(to_replace='\n', value=', ', inplace=True, regex=True)

# homogenize data
# print Lieferanten:
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

'''
#wlb_nummer_lower = np.array(list(map(lambda s: str(s).lower(), wlb.Artikelnummer.values)))
count = 0
for i in range(len(fhz)):
    fhz_preis = float(fhz['Empf. VK-Preis'][i])
    #i_wlb = (wlb.Lieferant == fhz.Lieferant[i]) & (wlb_nummer_lower == str(fhz.Artikelnummer[i]).lower())
    i_wlb = (wlb.Lieferant == fhz.Lieferant[i]) & (wlb.Artikelnummer == fhz.Artikelnummer[i])
    wlb_preis = wlb['VK-Preis'][i_wlb].values
    wlb_preis = float(wlb_preis[0]) if len(wlb_preis) > 0 else np.nan
    if ( np.isnan(fhz_preis) or np.isnan(wlb_preis) or abs(fhz_preis - wlb_preis) > 0.011 ):
        count += 1
        print('FHZ:', fhz_preis, 'WLB:', wlb_preis,
                '('+str(fhz['Bezeichnung | Einheit'][i]).replace('\n',' ')+')',
                '('+str(wlb['Bezeichnung | Einheit'][i_wlb]).replace('\n',' ')+')')
print(count, "Differenzen gefunden.\n")
'''

# Adopt values from FHZ (for existing articles):
count = 0
print("---------------")
wlb_neu = wlb.copy()
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
            if ( abs(fhz_preis - wlb_preis) > 0.011 ):
                # price seems to deviate more than usual
                count += 1
                print("Alter (WLB) Preis:", wlb_preis, "   Neuer (FHZ) Preis:", fhz_preis,
                        "(%s)" % fhz_row['Bezeichnung | Einheit'])
                fhz_preis = returnRoundedPrice(fhz_preis)
                print("Ändere Preis von:", str(wlb_preis), " zu:", str(fhz_preis))
                wlb_neu.loc[name, 'VK-Preis'] = str(fhz_preis)
                print("---------------")
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
    if (neuer_preis != alter_preis):
        count += 1
        print("Runde Preis von:", str(alter_preis), " zu:", str(neuer_preis),
                '(%s, %s)' % (wlb_row['Bezeichnung | Einheit'],
                wlb_row['Sortiment']))
        wlb_neu.loc[name, 'VK-Preis'] = str(neuer_preis)
print(count, "VK-Preise wurden gerundet.")

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
            abs(fhz_preis - wlb_preis) > 0.011 ):
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
        index=pd.MultiIndex.from_tuples([('','')], names=wlb.index.names))
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
        wlb_row = wlb_neue_artikel.iloc[-1]
        fhz_preis = returnRoundedPrice(fhz_preis)
        wlb_neue_artikel.loc[name, 'VK-Preis'] = str(fhz_preis)
        print('"%s" nicht in WLB. (%s)' % (fhz_row['Bezeichnung | Einheit'], name))
print(count, "Artikel nicht in WLB.")
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
