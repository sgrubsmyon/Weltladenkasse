#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# TODO:
# * GEPA      891097110      Agenda 21 Biokaffee\nBohne: neuer Name ("Bio Café, Bohne")
#   und Verpackung, Art.-Nr. gleich!

'''
1.) Aktuellen DB-Dump einspielen:
    ./preisaenderung_00_import_recent_db_dump.sh
2.) In Verzeichnis der Preisänderung wechseln.
3.) Skript "convert_wlb_data.py" aufrufen:
    ../../../git/prod_list_convert/wlb/convert_wlb_data.py
    Das Skript erstellt die Datei "Artikelliste_LM.csv" im aktuellen Verzeichnis.
4.) Skript "convert_fhz_sheet.py" aufrufen mit dem Pfad zur FHZ-Bestellvorlage:
    ../../../git/prod_list_convert/fhz/convert_fhz_sheet.py --fhz Bestellvorlage_XXX.ods
    Das Skript erstellt eine CSV-Datei im selben Ordner mit selbem Namen wie die ODS-Datei,
    nur mit Endung ".csv".
5.) Dieses Skript aufrufen mit:
    ../../../git/preisaenderung.py --fhz Bestellvorlage_XXX.csv --wlb Artikelliste_LM.csv > log.txt
     (Optionaler Parameter -n übernimmt den Artikelnamen vom FHZ, wir haben
     aber häufig Fehler im Namen korrigiert/angepasst, daher lieber nicht -n
     benutzen. Außerdem generiert das unnötig viele Änderungen.)
6.) Datei log.txt anschauen und evtl. Probleme beheben, z.B.:
    * Dopplungen von Artikelnummern korrigieren (werden als erstes aufgelistet),
        diese erzeugen später weitere Fehlermeldungen (Fehler evtl. ans FHZ
        melden)
    * Änderungen prüfen und ggf. eingreifen
    * ACHTUNG: Wenn Menge oder Einheit sich geändert haben, muss ggf. der Artikelname
        in preisänderung_irgendeine_änderung.csv von Hand geändert werden, wenn nicht -n benutzt wird.
        Am besten auf einem Zettel oder in `change_name.txt` notieren und hinterher händisch machen.
    * Angebliche neue Artikel prüfen, ob nur ein Tippfehler in der Artikelnummer
        ist (Fehler evtl. ans FHZ melden)
    * Auch gucken, ob neue Artikel eigentlich beim FHZ durchgestrichen sind und
        wir sie schon aus der DB entfernt haben.
7.) Punkte 5 und 6 so lange ausführen, bis alles OK ist.
8.) Ergebnisse werden gespeichert in Dateien:
    * "preisänderung.csv" (alle Artikel, aktualisiert)
    * "preisänderung_irgendeine_änderung.csv" (alle Artikel, bei denen sich
        irgendwas geändert hat; für beschleunigtes Einlesen ggü. "preisänderung.csv",
        denn Artikel ohne Änderung würden beim Einlesen sowieso ignoriert werden,
        verlangsamen aber den Einleseprozess)
    * "preisänderung_geänderte_preise.csv" (alle Artikel, deren Preis sich verändert hat)
    * "preisänderung_geänderte_preise_sortiment.csv" (alle Sortimentsartikel, deren Preis sich verändert hat, für Import im Preisschilder-Menü)
    * "preisänderung_geänderte_preise_sortiment_alle_felder.csv" (wie oben, für Import in LibreOffice zum besseren Erkennen der Artikel von Hand)
    * "preisänderung_neue_artikel.csv" (alle neuen Artikel)
9.) "preisänderung.csv" bzw. für schnelleres Einlesen "preisänderung_irgendeine_änderung.csv" mit LibreOffice öffnen:
    * Separated by: Semicolon
    * String Delimiter: '"'!
    * Problem 1:
        nämlich dass Artikelnummern, die mit "0" beginnen ihre führenden Nullen
        verlieren.
        Daher: Die Spalte "Artikelnummer" anklicken und Typ auf "Text" setzen
            (https://www.youtube.com/watch?v=TrVbvKzLhgs).
    * Problem 2:
        nämlich dass dreistellige Mengenangaben wie "0.126 kg" zu "126 kg"
        werden.
        Daher: Die Spalte "Menge (kg/l/St.)" anklicken und Typ auf "Text"
            setzen.
10.) Als ods-Datei speichern (Save As, "Artikelliste_LM_neu.ods").
11.) Die Datei "preisänderung_neue_artikel.csv" mit LibreOffice öffnen,
    Semicolon als Separator, String Delimiter: '"'. Auch hier wieder: Die Spalte
    "Artikelnummer" anklicken und Typ auf "Text" setzen, Spalte "Menge (kg/l/St.)"
    anklicken und Typ auf "Text" setzen.
12.) Speichern als "preisänderung_neue_artikel.ods".
13.) Auf Kassen-Server Skript "./dump_and_stop_db.sh" ausführen. Dies stoppt
    den Kassenserver, sodass niemand in der Zwischenzeit kassieren kann, weil
    jegliche Änderung an der DB, die in der Zwischenzeit passiert, verloren
    gehen würde.
14.) Lokal Skript "../../../git/preisaenderung_01_import_db.sh" ausführen.
15.) Weltladenkasse lokal starten mit "run_kasse.sh".
16.) In "Weltladenkasse -> Optionen -> Artikelliste" auf "Artikel importieren" klicken und
    die Datei "Artikelliste_LM_neu.ods" auswählen.
17.) In "Weltladenkasse -> Optionen -> Artikelliste" auf "Artikel importieren" klicken und
    die Datei "preisänderung_neue_artikel.ods" auswählen.
18.) Evtl. schon vorhandene Artikel (z.B. Wein-Geschenkkartons), die jetzt rot
    markiert sind, aus "preisänderung_neue_artikel.ods" löschen. Wenn Änderungen
    nötig sind (z.B. Preis), dann die hier rot markierten Artikel von Hand
    verändern.
19.) Umbenennungen etc. z.B. aus `change_name.txt` anwenden.
19.) In "Weltladenkasse -> Preisschilder" auf "Datei einlesen" klicken und
    "preisänderung_geänderte_preise_sortiment.csv" auswählen. Neue Preislisten
    mit "Artikel drucken" speichern (ODS-Dateien) und an Koordination schicken.
20.) Lokal Skript "../../../git/preisaenderung_02_dump_db.sh" ausführen.
21.) Auf Kassen-Server Skript "./import_and_start_db.sh" ausführen.

###

Alte Schritte 8 bis 11 jetzt obsolet, seit es das Skript "convert_fhz_sheet.py" gibt
(falls es irgendwann noch mal gebraucht wird):

8.) Von der letzten Preisänderung die Datei "..._mitFormeln.ods" in den
    aktuellen Ordner als
    "Artikelliste_Bestellvorlage_Lebensmittelpreisliste_XXX_mitFormeln.ods"
    kopieren.
9.) Neue FHZ-Preisliste öffnen
    * Wir benötigen Spalten D bis M (von "Lieferant" bis "je Einheit"), diese
        Spalten markieren, kopieren (Ctrl-C)
    * In die Datei "..._mitFormeln.ods" ganz rechts (ab Spalte V) einfügen mit
        Ctrl-Shift-V (Formatierung wird gelöscht)
    * Alle Zeilen, die leer sind oder Überschrift (Produktgruppe) enthalten, löschen und zwar so:
        In Spalte "Lieferant" oder "Bezeichnung" mit Ctrl-Down springen, Zellen
        aus zu löschenden Zeilen markieren, Ctrl-Minus, Delete entire row(s)
    * Artikeln ohne Lieferant einen Lieferanten geben:
        Wein-Geschenkkarton -> FHZ Rheinland
        Kaffeefilter, Teefilter, Teefilter-Clip -> FHZ Rheinland
    * Alle Pfandartikel (leere Flaschen und Kästen) löschen (von PFAND1 bis
        9999386), denn wir haben ein anderes Pfandsystem (bei uns entspricht
        PFAND2 der 0,33 l Flasche für 8 ct, dafür haben wir nicht die
        GEPA-Pfandflasche 9999385 und GEPA-Pfandkiste 9999386)
10.) Datei
    "Artikelliste_Bestellvorlage_Lebensmittelpreisliste_XXX_mitFormeln.ods"
    kopieren, einfügen und umbenennen in
    "Artikelliste_Bestellvorlage_Lebensmittelpreisliste_XXX_ohneFormeln.ods".
    Neue Datei "ohneFormeln" öffnen. Die linken Spalten (A bis T) markieren,
    kopieren (Ctrl-C) und mit Ctrl-Shift-V einfügen, so dass die Formeln
    verschwinden. Dann den rechten Block (ab Spalte V) markieren und mit
    Ctrl-Minus löschen.
    * Nach fehlender Einheit suchen (mit Ctrl-Down zu Lücken springen), in
        fast allen Fällen (außer z.B. 70 g Gewürzgeschenkbox, Kokoblock) "St."
        eintragen und Menge anpassen (z.B. 5 für Muskatnüsse)
    OR:
    * After running script, search for "zu 0 " in output, correct the Menge
        values in the FHZ file and Einheit to "St." (e.g. Vanilleschoten
        'ma110100', 'sl115108', 'rfb116032')
    Speichern.
11.) "File -> Save a Copy" und als csv-Datei
    "Artikelliste_Bestellvorlage_Lebensmittelpreisliste_XXX.csv" exportieren.
    WICHTIG: Als "Field Delimiter" ';' auswählen, als "Text Delimiter" '"'!

###

Noch älterer Schritt 8 jetzt noch obsoleter, falls es irgendwann noch mal gebraucht wird:

8.) Neue FHZ-Preisliste öffnen
    * Wir benötigen Spalten D bis M (von "Lieferant" bis "je Einheit"), diese
        Spalten markieren, kopieren und in leeres Dokument (Ctrl-N) einfügen mit
        Ctrl-Shift-V (Formatierung wird gelöscht)
    * 'File -> Save As' als "Artikelliste_Bestellvorlage_Lebensmittelpreisliste_XXX.ods"
    * Alle Zeilen, die leer sind oder Überschrift (Produktgruppe) enthalten, löschen:
        In Spalte "Lieferant" oder "Bezeichnung" mit Ctrl-Down springen, Zellen aus Zeilen
        markieren, Ctrl-Minus, Delete entire row(s)
    * Artikeln ohne Lieferant einen Lieferanten geben:
        Wein-Geschenkkarton -> FHZ Rheinland
        Kaffeefilter, Teefilter, Teefilter-Clip -> FHZ Rheinland
    * Alle Pfandartikel (leere Flaschen und Kästen) löschen (von PFAND1 bis
        9999386), denn wir haben ein anderes Pfandsystem (bei uns entspricht
        PFAND2 der 0,33 l Flasche für 8 ct, dafür haben wir nicht die
        GEPA-Pfandflasche 9999385 und GEPA-Pfandkiste 9999386)
    * Spalten so benennen und arrangieren wie in der Artikelliste-Datei (gleiche Reihenfolge):
        * Preis (Spalte "je Einheit") geht in "Empf. VK-Preis"
        * "Bezeichnung" geht in "Kurzname"
        * "VPE" -> "VPE", "Herkunftsland" -> "Herkunftsland"
        * Spalten "Einheit" ("250", "g", "Beutel") und "x" ganz nach rechts verschieben in Spalten U bis X
            (Einheit in U bis W, x in X)
        * Spalte "Variabel" auf "Nein" setzen
        * Andere Spalten (z.B. Sortiment, Beliebtheit, Barcode, Setgröße, VK-Preis, EK-Rabatt, EK-Preis, Bestand) leer lassen
    * mit Formeln bearbeiten:
        =CONCATENATE(E2; " | "; F2; " "; G2; " "; H2)       für "Bezeichnung | Einheit"
                                                            dabei ist E2 der Kurzname (FHZ-Bezeichnung), F2, G2, H2 sind "250", "g", "Beutel"
        =F2/1000                                            für "Menge"
        =IF(G2="g"; "kg"; IF(G2="ml";"l";""))               für "Einheit"
        =IF(X3="x", "Ja", "Nein")                           für "Sofort lieferbar"
	* Einheit-Spalte kopieren, mit Strg-V einfügen (nur Werte)
        * nach fehlender Einheit suchen (mit Ctrl-Down zu Lücken springen), in
            fast allen Fällen (außer z.B. 70 g Gewürzgeschenkbox, Kokoblock) "St."
            eintragen und Menge anpassen (z.B.  5 für Muskatnüsse)
        OR:
        * After running script, search for "zu 0 " in output, correct the Menge
            values in the FHZ file and Einheit to "St." (e.g. Vanilleschoten
            'ma110100', 'sl115108', 'rfb116032')
    Looks like not necessary anymore:
    (* Copy VPE column into vim, then
        :%s/[^0-9]//g
      Save as blabla, cat in terminal and copy and paste in LibreOffice)
    * Alle Spalten mit Formeln (einzeln) markieren, Strg-C, an gleicher Stelle
      Strg-Shift-V (nur Werte einfügen), sodass die Formel überschrieben wird
    * Dann die Spalten, die in Formeln benutzt wurden, löschen (U bis X)
    * Spalte "Menge": Markieren, "Format Cells", 5 decimal places
'''

import re
from decimal import Decimal


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


def specialTreatment(row, preis, wlb_neu, name):
    '''
    `row`: One row of the FHZ DataFrame
    `preis`: Price as Decimal
    `wlb_neu`: DataFrame holding the new WLB data
    `name`: ID (combination Lieferant and Artikelnummer) to identify product
    '''
    # Mini-Schoko-Täfelchen Großpackung GEPA
    if row.Artikelnummer == '8901827' or row.Artikelnummer == '8901828':
        # Adopt the recommented sales price as procurement price
        wlb_neu.loc[name, 'EK-Preis'] = preis
        # Always add 2.40 EUR to the price so that we earn something from it
        preis += Decimal('2.40')
    return preis


def returnRoundedPrice(preis):
    '''
    `preis`: Price as Decimal
    '''
    preis_str = str(preis)
    if ('.' in preis_str):
        cent_index = preis_str.index('.') + 2
        if ((len(preis_str) > cent_index) and
                (preis_str[cent_index] == '9')):
            # round up, i.e. add one cent:
            preis += Decimal('0.01')
        elif ((len(preis_str) > cent_index) and
                (preis_str[cent_index] == '8')):
            # round up, i.e. add two cents:
            preis += Decimal('0.02')
        elif ((len(preis_str) > cent_index) and
                (preis_str[cent_index-1:cent_index+1] == '45')):
            # round up, i.e. add five cents:
            preis += Decimal('0.05')
    return preis


def writeOutAsCSV(df, filename, only_index=False):
    out = df.reset_index()
    c = out.columns.tolist()
    # change column order: first Produktgruppe, then Lieferant, then Artikelnummer, then rest
    c = [c[2], ] + [c[0], ] + [c[-1], ] + c[3:-1]
    out = out.reindex(c, axis=1)
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


def main():

    from optparse import OptionParser

    # install parser
    usage = "Usage: %prog   [OPTIONS]"
    parser = OptionParser(usage)

    parser.add_option("--fhz", type="string",
                      default='Artikelliste_Bestellvorlage_Lebensmittelpreisliste_1.2-2015.csv',
                      dest="FHZ",
                      help="The path to FHZ .csv file.")
    parser.add_option("--wlb", type="string",
                      default='Artikelliste_DB_Dump_2015_KW40_LM.csv',
                      dest="WLB",
                      help="The path to WLB .csv file.")
    parser.add_option("-n", action="store_true",
                      default=False,
                      dest="ADOPT_NAMES",
                      help="Artikelnamen ('Bezeichnung | Einheit') vom FHZ übernehmen?")

    # get parsed args
    (options, args) = parser.parse_args()

    #############
    # Load data #
    #############

    import numpy as np
    import pandas as pd

    fhz = pd.read_csv(options.FHZ, sep=';', dtype=str, index_col=(1, 2))
    #   input CSV list must contain only Lebensmittel and Getränke (LM) and not
    #   Kunsthandwerk and other (KHW), if that's the case also for the FHZ CSV list
    wlb = pd.read_csv(options.WLB, sep=';', dtype=str, index_col=(1, 2))

    ###################
    # Homogenize data #
    ###################

    # Remove all newlines ('\n') from all fields
    fhz.replace(to_replace='\n', value=', ', inplace=True, regex=True)
    wlb.replace(to_replace='\n', value=', ', inplace=True, regex=True)
    fhz.replace(to_replace=';', value=',', inplace=True, regex=True)
    wlb.replace(to_replace=';', value=',', inplace=True, regex=True)

    # homogenize Lieferanten:
    fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('El Puente', i[1])
                                                   if i[0] == 'EP' else i, fhz.index.tolist())), names=fhz.index.names)
    fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('El Puente', i[1])
                                                   if i[0] == 'EP\n(fairfood)' else i, fhz.index.tolist())), names=fhz.index.names)
    fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('Fairtrade Center Breisgau', i[1])
                                                   if i[0] == 'ftc' else i, fhz.index.tolist())), names=fhz.index.names)
    fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('Café Libertad', i[1])
                                                   if i[0] == 'Café\nLibertad' else i, fhz.index.tolist())), names=fhz.index.names)
    fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('Ethiquable', i[1])
                                                   if i[0] == 'ethiquable' else i, fhz.index.tolist())), names=fhz.index.names)
    fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('Libera Terra', i[1])
                                                   if i[0] == 'Libera\nTerra' else i, fhz.index.tolist())), names=fhz.index.names)
    fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('WeltPartner', i[1])
                                                   if i[0] == 'Welt Partner' else i, fhz.index.tolist())), names=fhz.index.names)
    fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('WeltPartner', i[1])
                                                   if i[0] == 'Welt\nPartner' else i, fhz.index.tolist())), names=fhz.index.names)
    fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('FAIR Handelshaus Bayern', i[1])
                                                   if i[0] == 'Fair Han-delshaus Bayern' else i, fhz.index.tolist())), names=fhz.index.names)
    fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('unbekannt', i[1])
                                                   if type(i[0]) == float and np.isnan(i[0]) else i, fhz.index.tolist())), names=fhz.index.names)
    fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('FHZ Rheinland', i[1])
                                                   if i[0] == 'FHZ' else i, fhz.index.tolist())), names=fhz.index.names)
    fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: ('FHZ Rheinland', i[1])
                                                   if i[0] == 'unbekannt' else i, fhz.index.tolist())), names=fhz.index.names)
    print('\n\n\n')
    print('Lieferanten in FHZ, die es nicht in WLB gibt (bitte ggf. in Kasse neu anlegen vor Einlesen der Produkte):')
    fhz_lieferanten = sorted(set(map(lambda i: i[0], fhz.index)))
    wlb_lieferanten = sorted(set(map(lambda i: i[0], wlb.index)))
    missing_lieferanten = set()
    for l in fhz_lieferanten:
        if not l in wlb_lieferanten:
            missing_lieferanten.add(l)
    print(sorted(missing_lieferanten))

    # Make all article numbers lower case for better comparison:
    # First store the original article numbers:
    fhz = fhz.rename(
        columns={'Artikelnummer': 'Artikelnummer kleingeschrieben'})
    wlb = wlb.rename(
        columns={'Artikelnummer': 'Artikelnummer kleingeschrieben'})
    fhz.index.names = ['Lieferant', 'Artikelnummer kleingeschrieben']
    wlb.index.names = ['Lieferant', 'Artikelnummer kleingeschrieben']
    fhz['Artikelnummer'] = list(map(lambda i: i[1], fhz.index.tolist()))
    wlb['Artikelnummer'] = list(map(lambda i: i[1], wlb.index.tolist()))
    fhz.index = pd.MultiIndex.from_tuples(list(map(lambda i: (i[0], i[1].lower()),
                                                   fhz.index.tolist())), names=fhz.index.names)
    wlb.index = pd.MultiIndex.from_tuples(list(map(lambda i: (i[0], i[1].lower()),
                                                   wlb.index.tolist())), names=wlb.index.names)

    # Improve performance and avoid PerformanceWarning:
    fhz = fhz.sort_index()
    wlb = wlb.sort_index()

    # Check for duplicates in fhz:
    fhz_dup_indices = indexDuplicationCheck(fhz)
    print("Folgende Artikel kommen mehrfach in '%s' vor:" % options.FHZ)
    for i in fhz_dup_indices:
        print(fhz.loc[i])
        print("---------------")
    print("---------------")

    # Check for duplicates in wlb:
    wlb_dup_indices = indexDuplicationCheck(wlb)
    print("Folgende Artikel kommen mehrfach in '%s' vor:" % options.WLB)
    for i in wlb_dup_indices:
        print(wlb.loc[i])
        print("---------------")
    print("---------------")

    #################################################
    # Adopt values from FHZ (for existing articles) #
    #################################################

    def adopt_values(fhz_row, fhz_preis, fhz_preis_empf, name, sth_printed, price_changed, sth_changed,
                     wlb_neu, wlb_row, geaenderte_preise, irgendeine_aenderung, count):
        # print(wlb_row)
        # print(type(wlb_row))
        # adopt the rec. sales price and the "Lieferbarkeit" directly and completely
        # See
        # http://pandas.pydata.org/pandas-docs/stable/indexing.html#indexing-view-versus-copy,
        # very bottom
        wlb_neu.loc[name, 'Empf. VK-Preis'] = str(fhz_preis_empf)
        if (fhz_row['Sofort lieferbar'] == 'Ja' or fhz_row['Sofort lieferbar'] == 'Nein'):
            wlb_neu.loc[name, 'Sofort lieferbar'] = fhz_row['Sofort lieferbar']
        # print(wlb_row['VK-Preis'])
        # print(type(wlb_row['VK-Preis']))
        # print(Decimal(wlb_row['VK-Preis']))
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
                print("FHZ-Preis wird zu FHZ-Preis / %s = %s" % (setgroesse,
                                                                 fhz_preis))
                print("")
                sth_printed = True
            fhz_preis = specialTreatment(fhz_row, fhz_preis, wlb_neu, name)
            #fhz_preis = returnRoundedPrice(fhz_preis) # Do not round anymore
            if (abs(fhz_preis - wlb_preis) > 0.):
                # price seems to deviate more than usual
                count += 1
                if wlb_row['Sortiment'] == 'Ja':
                    print("Alter (WLB) Preis: %s   Neuer (FHZ) Preis: %s\n"
                          "FHZ: %s   (%s)    Sortiment: %s\n"
                          "WLB: %s   (%s)    Sortiment: %s" % (wlb_preis, fhz_preis,
                                                               fhz_row['Bezeichnung | Einheit'], name, fhz_row['Sortiment'],
                                                               wlb_row['Bezeichnung | Einheit'], name, wlb_row['Sortiment']))
                    print("Ändere Preis von:", str(
                        wlb_preis), " zu:", str(fhz_preis))
                    sth_printed = True
                wlb_neu.loc[name, 'VK-Preis'] = str(fhz_preis)
                geaenderte_preise = pd.concat(
                    [geaenderte_preise, wlb_neu.loc[[name]]])
                irgendeine_aenderung = pd.concat(
                    [irgendeine_aenderung, wlb_neu.loc[[name]]])
                price_changed = True
                sth_changed = True

        if options.ADOPT_NAMES:
            # adopt the article name
            wlb_neu.loc[name,
                        'Bezeichnung | Einheit'] = fhz_row['Bezeichnung | Einheit']

        # adopt VPE
        if fhz_row['VPE'] != wlb_row['VPE']:
            print("Ändere VPE für %s (%s) von %s (WLB) zu %s (FHZ)" % (name,
                                                                       wlb_row['Bezeichnung | Einheit'], wlb_row['VPE'], fhz_row['VPE']))
            wlb_neu.loc[name, 'VPE'] = fhz_row['VPE']
            sth_printed = True
            if not sth_changed:
                irgendeine_aenderung = pd.concat(
                    [irgendeine_aenderung, wlb_neu.loc[[name]]])
                sth_changed = True
            else:
                irgendeine_aenderung.loc[name, 'VPE'] = fhz_row['VPE']

        # adopt Menge
        fhz_menge = float(fhz_row['Menge (kg/l/St.)']) / setgroesse
        if fhz_menge != float(wlb_row['Menge (kg/l/St.)']):
            print("Ändere Menge für %s (%s) von %s (WLB) zu %s (FHZ)" % (name,
                                                                         wlb_row['Bezeichnung | Einheit'], float(
                                                                             wlb_row['Menge (kg/l/St.)']),
                                                                         fhz_menge))
            wlb_neu.loc[name, 'Menge (kg/l/St.)'] = '%.5f' % fhz_menge
            sth_printed = True
            if not price_changed:
                geaenderte_preise = pd.concat(
                    [geaenderte_preise, wlb_neu.loc[[name]]])
                price_changed = True
            if not sth_changed:
                irgendeine_aenderung = pd.concat(
                    [irgendeine_aenderung, wlb_neu.loc[[name]]])
                sth_changed = True
            else:
                irgendeine_aenderung.loc[name,
                                         'Menge (kg/l/St.)'] = '%.5f' % fhz_menge

        # adopt Einheit
        if fhz_row['Einheit'] != wlb_row['Einheit']:
            print("Ändere Einheit für %s (%s) von %s (WLB) zu %s (FHZ)" % (name,
                                                                           wlb_row['Bezeichnung | Einheit'], wlb_row['Einheit'], fhz_row['Einheit']))
            wlb_neu.loc[name, 'Einheit'] = fhz_row['Einheit']
            sth_printed = True
            if not sth_changed:
                irgendeine_aenderung = pd.concat(
                    [irgendeine_aenderung, wlb_neu.loc[[name]]])
                sth_changed = True
            else:
                irgendeine_aenderung.loc[name, 'Einheit'] = fhz_row['Einheit']

        return (fhz_row, fhz_preis, name, sth_printed, price_changed, sth_changed,
                wlb_neu, wlb_row, geaenderte_preise, irgendeine_aenderung, count)

    count = 0
    print('\n\n\n')
    wlb_neu = wlb.copy()
    # Improve performance and avoid PerformanceWarning:
    wlb_neu = wlb_neu.sort_index()
    geaenderte_preise = pd.DataFrame(columns=wlb_neu.columns,
                                     index=pd.MultiIndex.from_tuples([('', '')], names=wlb_neu.index.names))
    irgendeine_aenderung = pd.DataFrame(columns=wlb_neu.columns,
                                        index=pd.MultiIndex.from_tuples([('', '')], names=wlb_neu.index.names))
    geaenderte_preise = geaenderte_preise.sort_index()
    irgendeine_aenderung = irgendeine_aenderung.sort_index()
    # Loop over fhz numerical index
    for i in range(len(fhz)):
        fhz_row = fhz.iloc[i]
        fhz_preis_empf = Decimal(fhz_row['Empf. VK-Preis'])
        fhz_preis = Decimal()
        try:
            fhz_preis = Decimal(fhz_row['VK-Preis'])
            if fhz_preis.is_nan() or fhz_preis == 0:
                fhz_preis = fhz_preis_empf
        except:
            fhz_preis = fhz_preis_empf
        name = fhz_row.name
        sth_printed = False
        price_changed = False
        sth_changed = False
        try:
            wlb_match = wlb_neu.loc[name]
            if type(wlb_match) == pd.DataFrame:
                for j in range(len(wlb_match)):
                    wlb_row = wlb_match.iloc[j]
                    (fhz_row, fhz_preis, name, sth_printed, price_changed,
                     sth_changed, wlb_neu, wlb_row, geaenderte_preise,
                     irgendeine_aenderung, count) = adopt_values(
                        fhz_row, fhz_preis, fhz_preis_empf, name, sth_printed,
                        price_changed, sth_changed, wlb_neu, wlb_row,
                        geaenderte_preise, irgendeine_aenderung, count)
            else:
                (fhz_row, fhz_preis, name, sth_printed, price_changed,
                 sth_changed, wlb_neu, wlb_match, geaenderte_preise,
                 irgendeine_aenderung, count) = adopt_values(
                    fhz_row, fhz_preis, fhz_preis_empf, name, sth_printed,
                    price_changed, sth_changed, wlb_neu, wlb_match,
                    geaenderte_preise, irgendeine_aenderung, count)

        except KeyError:
            pass
        if sth_printed:
            # this ends processing of this article
            print("---------------")
    print(count, "Artikel haben geänderten Preis.")

    ##################################
    ## Round up all articles' prices #
    ##################################
    # No, not anymore

    ## Round up all articles' prices:
    #count = 0
    #print('\n\n\n')
    #for i in range(len(wlb_neu)):
    #    wlb_row = wlb_neu.iloc[i]
    #    name = wlb_row.name
    #    alter_preis = Decimal(wlb_row['VK-Preis'])
    #    neuer_preis = returnRoundedPrice(alter_preis)
    #    if (not alter_preis.is_nan() and not neuer_preis.is_nan() and
    #            neuer_preis != alter_preis):
    #        count += 1
    #        print("Runde Preis von:", str(alter_preis), " zu:", str(neuer_preis),
    #              '(%s, %s)' % (wlb_row['Bezeichnung | Einheit'],
    #                            wlb_row['Sortiment']))
    #        wlb_neu.loc[name, 'VK-Preis'] = str(neuer_preis)
    #        geaenderte_preise = pd.concat(
    #            [geaenderte_preise, wlb_neu.iloc[[i]]])
    #        irgendeine_aenderung = pd.concat(
    #            [irgendeine_aenderung, wlb_neu.iloc[[i]]])
    #print(count, "VK-Preise wurden gerundet.")
    #geaenderte_preise = removeEmptyRow(geaenderte_preise)
    #irgendeine_aenderung = removeEmptyRow(irgendeine_aenderung)

    # Check for duplicates in geaenderte_preise:
    gp_dup_indices = indexDuplicationCheck(geaenderte_preise)
    print("Folgende Artikel kommen mehrfach in 'geaenderte_preise' vor:")
    for i in gp_dup_indices:
        print(geaenderte_preise.loc[i])

    writeOutAsCSV(geaenderte_preise,
                  'preisänderung_geänderte_preise.csv', only_index=True)
    mask = geaenderte_preise['Sortiment'] == 'Ja'
    gp_sortiment = geaenderte_preise[mask]
    writeOutAsCSV(gp_sortiment, 'preisänderung_geänderte_preise_sortiment.csv',
                  only_index=True)
    writeOutAsCSV(gp_sortiment,
                  'preisänderung_geänderte_preise_sortiment_alle_felder.csv')

    #####################
    # Consistency check #
    #####################

    count = 0
    print('\n\n\n')
    for i in range(len(fhz)):
        fhz_row = fhz.iloc[i]
        wlb_row = {'Bezeichnung | Einheit': 'Nicht gefunden'}
        name = fhz_row.name
        fhz_preis = Decimal(fhz_row['Empf. VK-Preis'])
        try:
            wlb_match = wlb_neu.loc[name]
            if type(wlb_match) == pd.DataFrame:
                for j in range(len(wlb_match)):
                    wlb_row = wlb_match.iloc[j]
                    wlb_preis = Decimal(wlb_row['Empf. VK-Preis'])
            else:
                wlb_preis = Decimal(wlb_match['Empf. VK-Preis'])
        except KeyError:
            wlb_preis = Decimal(np.nan)
        if (not fhz_preis.is_nan() and not wlb_preis.is_nan() and
                # abs(fhz_preis - wlb_preis) > 0.021 ):
                abs(fhz_preis - wlb_preis) > 0.):
            count += 1
            print('FHZ: %s WLB: %s' % (fhz_preis, wlb_preis),
                  '(%s) (%s)' % (fhz_row['Bezeichnung | Einheit'],
                                 wlb_row['Bezeichnung | Einheit']))
    print(count, "Differenzen gefunden.")
    writeOutAsCSV(wlb_neu, 'preisänderung.csv')

    #######################
    # Articles not in WLB #
    #######################

    # Add new, so far unknown articles from FHZ
    print('\n\n\n')
    print("Neue Artikel (in FHZ vorhanden, nicht in WLB):")
    # Get empty df:
    wlb_neue_artikel = pd.DataFrame(columns=wlb_neu.columns,
                                    index=pd.MultiIndex.from_tuples([('', '')], names=wlb_neu.index.names))
    wlb_neue_artikel = wlb_neue_artikel.sort_index()
    count = 0
    for i in range(len(fhz)):
        fhz_row = fhz.iloc[i]
        fhz_preis_empf = Decimal(fhz_row['Empf. VK-Preis'])
        fhz_preis = Decimal()
        try:
            fhz_preis = Decimal(fhz_row['VK-Preis'])
            if fhz_preis == 0 or fhz_preis.is_nan():
                fhz_preis = fhz_preis_empf
        except:
            fhz_preis = fhz_preis_empf
        name = fhz_row.name
        try:
            wlb_neu.loc[name]
        except KeyError:
            count += 1
            wlb_neue_artikel = pd.concat([wlb_neue_artikel, fhz.iloc[[i]]])
            #fhz_preis = returnRoundedPrice(fhz_preis) # Do not round anymore
            wlb_neue_artikel.loc[name, 'VK-Preis'] = str(fhz_preis)
            print('"%s" nicht in WLB. %s' %
                  (fhz_row['Bezeichnung | Einheit'], name))
    print(count, 'Artikel nicht in WLB.')
    wlb_neue_artikel = removeEmptyRow(wlb_neue_artikel)
    writeOutAsCSV(wlb_neue_artikel, 'preisänderung_neue_artikel.csv')

    #######################
    # Articles not in FHZ #
    #######################

    # Show list of articles missing in FHZ (so not orderable!)
    print('\n\n\n')
    print("Alte Artikel (in WLB vorhanden, nicht in FHZ, können nicht mehr bestellt werden!):")
    # Get empty df:
    wlb_alte_artikel = pd.DataFrame(columns=wlb_neu.columns,
                                    index=pd.MultiIndex.from_tuples([('', '')], names=wlb_neu.index.names))
    wlb_alte_artikel = wlb_alte_artikel.sort_index()
    count = 0
    # Loop over wlb numerical index
    for i in range(len(wlb_neu)):
        wlb_row = wlb_neu.iloc[i]
        name = wlb_row.name
        try:
            fhz.loc[name]
        except KeyError:
            # Add exceptions here:
            if \
                    not wlb_row['Artikelnummer'].startswith('SONSTIGES'):
                    # and not name[0] == 'Christoph Bäcker' \
                    # and not name[0] == 'Bingenheimer Saatgut AG' \
                    # and not name[0] == 'Bantam' \
                    # and not name[0] == 'Biologische Station Bonn' \
                    # and not name[0] == 'Imkerei Uni Bonn':
                count += 1
                wlb_alte_artikel = pd.concat(
                    [wlb_alte_artikel, wlb_neu.iloc[[i]]])
                # Change 'popularity' to 'ausgelistet' so that it will not be ordered any more:
                wlb_alte_artikel.loc[name, 'Beliebtheit'] = 'ausgelistet'
                if not name in irgendeine_aenderung.index:
                    irgendeine_aenderung = pd.concat(
                        [irgendeine_aenderung, wlb_neu.iloc[[i]]])
                irgendeine_aenderung.loc[name, 'Beliebtheit'] = 'ausgelistet'
                print('"%s" nicht in FHZ. %s' %
                      (wlb_row['Bezeichnung | Einheit'], name))
    print(count, 'Artikel nicht in FHZ.')
    wlb_alte_artikel = removeEmptyRow(wlb_alte_artikel)
    writeOutAsCSV(wlb_alte_artikel, 'preisänderung_alte_artikel.csv')

    # Check for duplicates in irgendeine_aenderung:
    gp_dup_indices = indexDuplicationCheck(irgendeine_aenderung)
    print('Folgende Artikel kommen mehrfach in "irgendeine_aenderung" vor:')
    for i in gp_dup_indices:
        print(irgendeine_aenderung.loc[i])

    writeOutAsCSV(irgendeine_aenderung,
                  'preisänderung_irgendeine_änderung.csv')


if __name__ == '__main__':
    main()
