#!/usr/bin/env python

import sys
import numpy as np
import pandas as pd

# load data
fhz = pd.read_csv('Artikelliste_Bestellvorlage_Lebensmittelpreisliste_1.2-2015.csv',
        sep=';', dtype=str)
wlb = pd.read_csv('Artikelliste_DB_Dump_2015_KW39.csv', sep=';', dtype=str)

# homogenize data
print(set(fhz['Lieferant']))
print(set(wlb['Lieferant']))
fhz.Lieferant[ fhz.Lieferant == 'ftc' ] = 'Fairtrade Center Breisgau'
fhz.Lieferant[ fhz.Lieferant == 'Café\nLibertad' ] = 'Café Libertad'
fhz.Lieferant[ fhz.Lieferant == 'ethiquable' ] = 'Ethiquable'
fhz.Lieferant[ fhz.Lieferant == 'Libera\nTerra' ] = 'Libera Terra'
fhz.Lieferant[ fhz.Lieferant.isnull() ] = 'unbekannt'
print(set(fhz['Lieferant']))
print(set(wlb['Lieferant']))

# TODO
# * Remove all newlines ('\n') from all fields
# * Round up all prices that end with '9' by adding 0.01
# * Show list of articles missing in FHZ (so not orderable!), in this case,
#   input CSV list must contain only Lebensmittel and Getränke
# * Make function doing the '9' ending check and rounding up

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
wlb_neu = wlb.copy()
for i in range(len(fhz)):
    fhz_preis = float(fhz['Empf. VK-Preis'][i])
    #i_wlb = (wlb_neu.Lieferant == fhz.Lieferant[i]) & (wlb_nummer_lower == str(fhz.Artikelnummer[i]).lower())
    i_wlb = (wlb.Lieferant == fhz.Lieferant[i]) & (wlb.Artikelnummer == fhz.Artikelnummer[i])
    # adopt the rec. sale price and the "Lieferbarkeit" directly and completely
    wlb_neu['Empf. VK-Preis'][i_wlb] = str(fhz_preis)
    wlb_neu['Sofort lieferbar'][i_wlb] = fhz['Sofort lieferbar'][i]
    # adopt the sale price only if significantly changed:
    wlb_preis = wlb['VK-Preis'][i_wlb].values
    if (len(wlb_preis) > 1):
        print("Multiple articles with Lieferant %s and Nummer %s" %
                (fhz.Lieferant[i], fhz.Artikelnummer[i]))
        sys.exit()
    wlb_preis = float(wlb_preis[0]) if len(wlb_preis) > 0 else np.nan
    if ( fhz_preis >= 2.*wlb_preis ):
        # price is at least twice, this indicates that this is a set:
        print("WLB price:", wlb_preis, "FHZ price:", fhz_preis,
                "(%s)" % fhz['Bezeichnung | Einheit'][i])
        setgroesse = round(fhz_preis / wlb_preis)
        fhz_preis = round(fhz_preis / setgroesse, 2)
        old_setgroesse = wlb['Setgröße'][i_wlb].values
        old_setgroesse = old_setgroesse[0] if len(old_setgroesse) > 0 else np.nan
        if (setgroesse != old_setgroesse):
            print("Old (WLB) setgroesse:", old_setgroesse,
                    "   New guessed setgroesse:", setgroesse)
            print("Changing setgroesse from:", old_setgroesse, " to:",
                    setgroesse)
            wlb_neu['Setgröße'][i_wlb] = str(setgroesse)
    if ( abs(fhz_preis - wlb_preis) > 0.011 ):
        # price seems to deviate more than usual
        print("Old (WLB) price:", wlb_preis, "   New (FHZ) price:", fhz_preis,
                "(%s)" % fhz['Bezeichnung | Einheit'][i])
        fhz_preis_str = str(fhz_preis)
        cent_index = fhz_preis_str.index('.') + 2
        if ( (len(fhz_preis_str) > cent_index) and
                (fhz_preis_str[cent_index] == '9') ):
            # round up, i.e. add one cent:
            fhz_preis += 0.01
        print("Changing price from:", wlb_preis, " to:", fhz_preis)
        wlb_neu['VK-Preis'][i_wlb] = str(fhz_preis)
    print("\n")

count = 0
for i in range(len(fhz)):
    fhz_preis = float(fhz['Empf. VK-Preis'][i])
    #i_wlb = (wlb_neu.Lieferant == fhz.Lieferant[i]) & (wlb_nummer_lower == str(fhz.Artikelnummer[i]).lower())
    i_wlb = (wlb.Lieferant == fhz.Lieferant[i]) & (wlb.Artikelnummer == fhz.Artikelnummer[i])
    wlb_preis = wlb_neu['Empf. VK-Preis'][i_wlb].values
    wlb_preis = float(wlb_preis[0]) if len(wlb_preis) > 0 else np.nan
    if ( not np.isnan(fhz_preis) and not np.isnan(wlb_preis) and abs(fhz_preis -
        wlb_preis) > 0.011 ):
        count += 1
        print('FHZ:', fhz_preis, 'WLB:', wlb_preis,
                '('+str(fhz['Bezeichnung | Einheit'][i]).replace('\n',' ')+')',
                '('+str(wlb_neu['Bezeichnung | Einheit'][i_wlb]).replace('\n',' ')+')')
print(count, "Differenzen gefunden.")
wlb_neu.to_csv('test.csv', sep=';', index=False)
#    if ( np.isnan(fhz_preis) or np.isnan(wlb_preis) or abs(fhz_preis - wlb_preis) > 0.011):
#        print('FHZ:', fhz.iloc[i])
#fhz['Bezeichnung | Einheit'][5]
#fhz.iloc[5]

# Add new, so far unknown articles from FHZ
# Get empty df:
wlb_neue_artikel = pd.DataFrame()
# Just add column definitions:
wlb_neue_artikel = wlb_neue_artikel.add(wlb.iloc[0])
for i in range(len(fhz)):
    fhz_preis = float(fhz['Empf. VK-Preis'][i])
    i_wlb = (wlb.Lieferant == fhz.Lieferant[i]) & (wlb.Artikelnummer == fhz.Artikelnummer[i])
    wlb_preis = wlb['VK-Preis'][i_wlb].values
    if (len(wlb_preis) == 0):
        # From:
        # http://stackoverflow.com/questions/30989531/how-do-i-copy-a-row-from-one-pandas-dataframe-to-another-pandas-dataframe
        wlb_neue_artikel.loc[fhz.index[i]] = fhz.iloc[i]
        fhz_preis_str = str(fhz_preis)
        if ('.' in fhz_preis_str):
            cent_index = fhz_preis_str.index('.') + 2
            if ( (len(fhz_preis_str) > cent_index) and
                    (fhz_preis_str[cent_index] == '9') ):
                # round up, i.e. add one cent:
                fhz_preis += 0.01
        wlb_neue_artikel['VK-Preis'][-1] = str(fhz_preis)
wlb_neue_artikel.to_csv('test_neue_artikel.csv', sep=';', index=False)

