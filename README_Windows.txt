1. Falls noch nicht geschehen, Java von http://www.java.com/de/ herunterladen und installieren.

2. Falls noch nicht geschehen, MySQL von http://dev.mysql.com/downloads/windows/installer/ herunterladen und installieren.

   Port in der Firewall nicht öffnen.

   "Development Default" oder (vermutlich besser) "Server only" auswählen

   "Development Machine" auswählen

   Root-Passwort setzen und 2 User erstellen (Passwörter merken oder notieren):
      1. Username: mitarbeiter (klein geschrieben), Host: localhost, Role: Backup Admin
      2. Username: kassenadmin (klein geschrieben), Host: localhost, Role: DB Admin

   Ansonsten Vorgaben übernehmen.

3. Im Ordner 'mysql' (befindet sich im selben Ordner wie diese Datei) auf generateDB.bat doppelklicken.

      Root-Passwort (bei MySQL-Installation gesetzt) dreimal eingeben.

      Bei Fehlern (wenn nicht dreimal nach Passwort gefragt wurde) die Datei
      generateDB.bat bearbeiten (mit Editor/Notepad) und den Pfad zu MySQL
      anpassen (muss auf Verzeichnis mit mysql.exe und mysqldump.exe
      verweisen).

4. Datei config.properties löschen und Datei config_Windows.properties in config.properties umbenennen.

5. Ggf. Pfad zu MySQL in config.properties anpassen (muss auf Verzeichnis mit mysql.exe und
      mysqldump.exe verweisen).

6. Zum Starten der Software auf Weltladenbesteller_vX.X.X.jar (für Bestell-Programm) oder
      Weltladenkasse_vX.X.X.jar (für Kassier-Software) doppelklicken.
      Danach Mitarbeiter-Passwort (oben notiert) eingeben.

7. Datenbank-Dump importieren im Registerreiter "DB Import/Export".
      Kassenadmin-Passwort (oben notiert) eingeben und Datei auswählen.

8. Nach Änderungen an der Datenbank: Datenbank-Dump exportieren im Registerreiter "DB Import/Export".
      Kassenadmin-Passwort (oben notiert) eingeben und Datei auswählen.
