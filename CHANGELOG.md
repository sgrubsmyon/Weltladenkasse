## v2.0.0

  * Weltladenkasse ist TSE-ready
  * Kassierdaten werden im für TSEen geeigneten Format gemacht
  * Beispiele:
    * Stornierungen werden als neue Gegenbuchung aufgenommen anstatt wie bisher bei der alten Buchung eine "storno"-Flag auf `TRUE` zu setzen
    * Anzahlungen und Gutscheine werden anders gespeichert, so dass eine Verknüpfung zwischen der Rechnung, in der die Anzahlung/der Gutschein ausgestellt und der Rechnung, in der sie auf-/eingelöst wurde, möglich ist
    * Der Trainingsmodus bucht nicht in eine separate Datenbank (wäre als "Schattenkasse" nicht DSFinVK-konform!), sondern in gleich strukturierte Kopien der "echten" Tabellen
    * Es werden TSE-Daten in die zusätzlichen Tabellen `tse_transaction` (bei Einzelaufzeichnung, also jeder Rechnung) und `abrechnung_tag_tse` (bei Kassenabschluss) geschrieben, aus denen später die DSFinVK-CSV-Dateien `transactions_tse.csv` und `tse.csv` generiert werden können
  * Die DSFinVK-CSV-Dateien können noch nicht generiert werden, es werden aber bei jedem Tagesabschluss TSE-Exports gemacht