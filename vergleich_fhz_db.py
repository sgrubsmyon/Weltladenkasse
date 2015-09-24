#!/usr/bin/env python

import numpy as np
import pandas as pd

# load data
fhz = pd.read_csv('Artikelliste_Bestellvorlage_Lebensmittelpreisliste_1.2-2015.csv', sep=';')
wlb = pd.read_csv('Artikelliste_DB_Dump_2015_KW39.csv', sep=';')

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

wlb_nummer_lower = np.array(list(map(lambda s: str(s).lower(), wlb.Artikelnummer.values)))
count = 0
for i in range(len(fhz)):
    fhz_preis = fhz['Empf. VK-Preis'][i]
    i_wlb = (wlb.Lieferant == fhz.Lieferant[i]) & (wlb_nummer_lower == str(fhz.Artikelnummer[i]).lower())
#    wlb_preis = wlb['VK-Preis'][ (wlb.Lieferant == fhz.Lieferant[i]) & (wlb_nummer_lower == str(fhz.Artikelnummer[i]).lower()) ].values
    wlb_preis = wlb['VK-Preis'][i_wlb].values
    wlb_preis = wlb_preis[0] if len(wlb_preis) > 0 else np.nan
    if ( np.isnan(fhz_preis) or np.isnan(wlb_preis) or abs(fhz_preis - wlb_preis) > 0.01):
        count += 1
        print('FHZ:', fhz_preis, 'WLB:', wlb_preis,
                '('+str(fhz['Bezeichnung | Einheit'][i]).replace('\n',' ')+')',
                '('+str(wlb['Bezeichnung | Einheit'][i_wlb]).replace('\n',' ')+')')
print(count, "Differenzen gefunden.")

# adopt the rec. sale price and the "Lieferbarkeit" directly and completely
wlb_neu = wlb.copy()
for i in range(len(fhz)):
    fhz_preis = fhz['Empf. VK-Preis'][i]
    i_wlb = (wlb_neu.Lieferant == fhz.Lieferant[i]) & (wlb_nummer_lower == str(fhz.Artikelnummer[i]).lower())
    wlb_neu['Empf. VK-Preis'][i_wlb] = fhz_preis
    wlb_neu['Sofort lieferbar'][i_wlb] = fhz['Sofort lieferbar'][i]

count = 0
for i in range(len(fhz)):
    fhz_preis = fhz['Empf. VK-Preis'][i]
    i_wlb = (wlb_neu.Lieferant == fhz.Lieferant[i]) & (wlb_nummer_lower == str(fhz.Artikelnummer[i]).lower())
#    wlb_preis = wlb_neu['VK-Preis'][ (wlb_neu.Lieferant == fhz.Lieferant[i]) & (wlb_nummer_lower == str(fhz.Artikelnummer[i]).lower()) ].values
    wlb_preis = wlb_neu['Empf. VK-Preis'][i_wlb].values
    wlb_preis = wlb_preis[0] if len(wlb_preis) > 0 else np.nan
    if ( np.isnan(fhz_preis) or np.isnan(wlb_preis) or abs(fhz_preis - wlb_preis) > 0.01):
        count += 1
        print('FHZ:', fhz_preis, 'WLB:', wlb_preis,
                '('+str(fhz['Bezeichnung | Einheit'][i]).replace('\n',' ')+')',
                '('+str(wlb_neu['Bezeichnung | Einheit'][i_wlb]).replace('\n',' ')+')')
print(count, "Differenzen gefunden.")

wlb_neu.to_csv('test.csv', sep=';', index=False)
#    if ( np.isnan(fhz_preis) or np.isnan(wlb_preis) or abs(fhz_preis - wlb_preis) > 0.01):
#        print('FHZ:', fhz.iloc[i])
#fhz['Bezeichnung | Einheit'][5]
#fhz.iloc[5]

