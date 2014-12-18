Auf Ubuntu-System installieren:
-------------------------------

1. Terminal öffnen und ins Verzeichnis mit der Software wechseln (wo sich
diese Datei `README.md` befindet):
```
cd Pfad/zu/meinem/Verzeichnis
```

2. Skript install-ubuntu.sh ausführen:
```
./install-ubuntu.sh
```

Software auf Ubuntu ausführen:
------------------------------
```
java -jar Weltladenkasse_v0.9.3.jar
```

Eine Version des Java Runtime Environment (JRE) muss installiert sein,
um die Software ausführen zu können (z.B. mit dem Befehl `sudo apt-get install default-jre`).

Auf Windows-System installieren:
--------------------------------

1. Falls noch nicht geschehen, Java von http://www.java.com/de/ herunterladen und installieren.

2. Falls noch nicht geschehen, MySQL von http://dev.mysql.com/downloads/windows/installer/ herunterladen und installieren.

  * Port in der Firewall nicht öffnen.

  * "Development Default" oder (vermutlich besser) "Server only" auswählen

  * "Development Machine" auswählen

  * Root-Passwort setzen und 2 User erstellen (Passwörter merken oder notieren):
  Die Usernamen müssen **EXAKT** stimmen!
    1. Username: mitarbeiter (klein geschrieben), Host: localhost, Role: Backup Admin
    2. Username: kassenadmin (klein geschrieben), Host: localhost, Role: DB Admin

  * Ansonsten Vorgaben übernehmen.

3. Im Ordner `mysql` (befindet sich im selben Ordner wie diese Datei) auf `generateDB.bat` doppelklicken.

  * Root-Passwort (bei MySQL-Installation gesetzt) dreimal eingeben.

  * Bei Fehlern (wenn nicht dreimal nach Passwort gefragt wurde) die Datei
  `generateDB.bat` bearbeiten (mit Editor/Notepad) und den Pfad zu MySQL
  anpassen (muss auf Verzeichnis mit `mysql.exe` und `mysqldump.exe`
  verweisen).

4. Datei `config.properties` löschen und Datei `config_Windows.properties` in `config.properties` umbenennen.

5. Ggf. Pfad zu MySQL in `config.properties` anpassen (muss auf Verzeichnis mit `mysql.exe` und
      `mysqldump.exe` verweisen).

Software auf Windows ausführen:
-------------------------------

Zum Starten der Software auf `Weltladenbesteller_vX.X.X.jar` (für
Bestell-Programm) oder `Weltladenkasse_vX.X.X.jar` (für Kassier-Software)
doppelklicken. Danach Mitarbeiter-Passwort (oben notiert) eingeben.

Datenbank-Daten im-/exportieren:
--------------------------------

1. Datenbank-Dump importieren im Registerreiter "DB Import/Export".
   Kassenadmin-Passwort (oben notiert) eingeben und SQL-Datei auswählen.

2. Nach Änderungen an der Datenbank: Datenbank-Dump exportieren im Registerreiter "DB Import/Export".
   Kassenadmin-Passwort (oben notiert) eingeben und SQL-Datei auswählen.

For Developers:
---------------

Compile:
```
ant
```

Compile and run locally:
```
ant && java -jar Weltladenkasse_v0.9.3.jar
ant && java -jar Weltladenbesteller_v0.9.3.jar
```

Compile and make release (**CAUTION:** default release dir is `../releases`, adjust to your needs):
```
ant && ./make_release.sh
```
