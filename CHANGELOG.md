## Übersicht über die Versionen und ihre Verwendung

+-----------------------------------+------------------------------------+
| Version | `Z_NR` von | `Z_NR` bis | Datum des Wechsels auf die Version |
+---------+------------+------------+------------------------------------+
| v2.0.0  | 1511       | 1533       | 02.04.2021                         |
| v2.0.1  | 1534       | 1535       | 06.05.2021                         |
| v2.0.2  | 1536       | 1888       | 09.05.2021                         |
| v2.0.3  | 1889       | 2091       | 24.07.2022                         |
| v2.0.4  | 2092       | 2443       | 02.04.2023                         |
| v2.0.5  | 2444       |            | 22.06.2024                         |
+-----------------------------------+------------------------------------+

## v2.0.5

* Header des Kassenbons kann konfiguriert werden
* Kassenbon kann auch dann gedruckt werden, wenn TSE ausfiel während Tagesabschluss
* Tagesabrechnungen werden als Lexware-kompatible CSV-Datei rausgeschrieben
* Sinnvollere Fehlermeldung, wenn TSE während Tagesabschluss ausfällt
* Export der Tagesabrechnungen und TSE-Daten in die Nextcloud
* Letzte Rechnung mit v2.0.4: Nr. 49238 am 22.06.2024 (bis `Z_NR`/`abrechnung_tag.id` 2443)
* Erste Rechnung mit v2.0.5: Nr. 49239 am 22.06.2024 (ab `Z_NR`/`abrechnung_tag.id` 2444)

## v2.0.4

  * Wechsel des Kassenservers, der die Kassierdaten in MySQL speichert
  * Wechsel von Raspberry Pi 2 Model B Rev 1.1 (Seriennr. 000000003b8a8285) auf Thin Client Fujitso Futro S920 (Seriennr. YLUE036019)
  * Letzte Rechnung mit v2.0.3: Nr. 43562 am 01.04.2023 (bis `Z_NR`/`abrechnung_tag.id` 2091)
  * Erste Rechnung mit v2.0.4: Nr. 43563 am 03.04.2023 (ab `Z_NR`/`abrechnung_tag.id` 2092)

## v2.0.3

  * Bug (eingeführt mit v2.0.0) behoben, durch den bei Storno von EC-Rechnungen fälschlicherweise immer der Kassenstand um den Rechnungsbetrag reduziert wurde. User wird nun gefragt, ob eine EC-Buchung ausgeführt wurde, also das Konto de\*r Kund\*in belastet wurde und der/die Kund\*in ausbezahlt werden muss. Falls nein, wird Kassenstand nicht geändert.
  * Bug behoben, durch den abgebrochene Transaktionen (AVBelegabbruch) nicht in `transactions_tse.csv` geschrieben wurden. Fehlende Transaktionen wurden einmalig retrospektiv geschrieben.
  * Ab jetzt werden die USt-Sätze in der Datei `vat.csv` nach dem DSFinV-K-USt-Schlüssel sortiert geschrieben. Für die Einheitlichkeit wurde retrospektiv neu geschrieben.
  * EC-Schwelle (Mindestwert für EC-Zahlungen) von 20 € auf 10 € herabgesetzt.
  * Implementierung der CSV-Exports in die Dateien:
    * `businesscases.csv` (Kassenabschlussmodul)
  * Einmaliges rückwirkendes Beschreiben Datei `businesscases.csv` für die Daten aus den Perioden von v2.0.0 und v2.0.1 und v2.0.2
  * Letzte Rechnung mit v2.0.2: Nr. 40047 am 24.07.2022 (bis `Z_NR`/`abrechnung_tag.id` 1888)
  * Erste Rechnung mit v2.0.3: Nr. 40047 am 25.07.2022 (ab `Z_NR`/`abrechnung_tag.id` 1889)

## v2.0.2

  * Fertigstellung des DSFinV-K-Stammdatenmoduls
  * Implementierung der CSV-Exports in die Dateien:
    * `cashpointclosing.csv` (Stammdatenmodul)
    * `location.csv` (Stammdatenmodul)
    * `cashregister.csv` (Stammdatenmodul)
    * `slaves.csv` (Stammdatenmodul)
    * `pa.csv` (Stammdatenmodul)
    * `vat.csv` (Stammdatenmodul)
    * `tse.csv` (Stammdatenmodul)
    * `transactions_tse.csv` (Einzelaufzeichnungsmodul)
  * Einmaliges rückwirkendes Beschreiben der obigen Dateien für die Daten aus den Perioden von v2.0.0 und v2.0.1
  * Letzte Rechnung mit v2.0.1: Nr. 34992 am 08.05.2021 (bis `Z_NR`/`abrechnung_tag.id` 1535)
  * Erste Rechnung mit v2.0.2: Nr. 34993 am 09.05.2021 (ab `Z_NR`/`abrechnung_tag.id` 1536)

## v2.0.1

  * Kritischer Bug mit Gutscheinen behoben: Gutscheine mit neuem System und neuer Besteuerung (0%) wurden nicht beginnend bei Nr. 200 ausgestellt, sondern mit Nr. des höchsten eingelösten Gutscheins + 1 (im konkreten Fall 169). Gutschein 169, der in die Papierliste als 172 eingetragen wurde, wurde manuell in der Kasse auf Nr. 200 geändert, mit Verweis auf Papierliste.
  * Letzte Rechnung mit v2.0.0: Nr. 34954 am 06.05.2021 (bis `Z_NR`/`abrechnung_tag.id` 1533)

## v2.0.0

  * Weltladenkasse ist TSE-ready
  * Kassierdaten werden im für TSEen geeigneten Format gemacht
  * Beispiele:
    * Stornierungen werden als neue Gegenbuchung aufgenommen anstatt wie bisher bei der alten Buchung eine "storno"-Flag auf `TRUE` zu setzen
    * Anzahlungen und Gutscheine werden anders gespeichert, so dass eine Verknüpfung zwischen der Rechnung, in der die Anzahlung/der Gutschein ausgestellt und der Rechnung, in der sie auf-/eingelöst wurde, möglich ist
    * Der Trainingsmodus bucht nicht in eine separate Datenbank (wäre als "Schattenkasse" nicht DSFinVK-konform!), sondern in gleich strukturierte Kopien der "echten" Tabellen
    * Es werden TSE-Daten in die zusätzlichen Tabellen `tse_transaction` (bei Einzelaufzeichnung, also jeder Rechnung) und `abrechnung_tag_tse` (bei Kassenabschluss) geschrieben, aus denen später die DSFinVK-CSV-Dateien `transactions_tse.csv` und `tse.csv` generiert werden können
  * Die DSFinVK-CSV-Dateien können noch nicht generiert werden, es werden aber bei jedem Tagesabschluss TSE-Exports gemacht
  * Erste Rechnung mit v2.0.0: Nr. 34657 am 02.04.2021 (ab `Z_NR`/`abrechnung_tag.id` 1511)
