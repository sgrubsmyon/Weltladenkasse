1. Falls noch nicht geschehen, Java von http://www.java.com/de/ herunterladen und installieren.

2. Falls noch nicht geschehen, MySQL von http://dev.mysql.com/downloads/windows/installer/ herunterladen und installieren.

   Port in der Firewall nicht �ffnen.

   "Development Default" oder "Server only" ausw�hlen

   "Development Machine" ausw�hlen

   Root-Passwort setzen und 2 User erstellen (Passw�rter merken oder notieren):
      1. Username: mitarbeiter (klein geschrieben), Host: localhost, Role: Backup Admin
      2. Username: kassenadmin (klein geschrieben), Host: localhost, Role: DB Admin

   Ansonsten Vorgaben �bernehmen.

3. Im Ordner 'mysql' (befindet sich im selben Ordner wie diese Datei) auf generateDB.bat doppelklicken.
      Root-Passwort (bei MySQL-Installation gesetzt) dreimal eingeben.

4. Datei config.properties durch config_Windows.properties ersetzen.

5. Ggf. Pfad zu MySQL in config.properties anpassen.
