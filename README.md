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
3. Bei Bedarf Namen des Quittungsdruckers in Datei `config.properties` anpassen

Software auf Ubuntu ausführen:
------------------------------
```
java -jar Weltladenkasse_v1.2.2.jar
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

6. Ggf. Pfad zu soffice (LibreOffice/OpenOffice) in `config.properties`
   anpassen (muss auf Verzeichnis mit `soffice.exe` verweisen). Dies wird
   nur benörigt, wenn man Quittungen drucken möchte.

7. Bei Bedarf Namen des Quittungsdruckers in Datei `config.properties` anpassen

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

## Dependencies:

Download and put into the folder `lib` the following jars:

* mysql-connector-java-5.1.25-bin.jar: http://dev.mysql.com/downloads/connector/j/
* jOpenDocument-1.3.jar: http://www.jopendocument.org/downloads.html
* date4j.jar: http://www.date4j.net
* jcalendar-1.4.jar: http://toedter.com/jcalendar/
* hidapi-1.1.jar: https://code.google.com/p/javahidapi/

Unjar the hidapi-1.1.jar (with `jar -xvf hidapi-1.1.jar`) and copy the content
of the `native/*` folders (files ending with `.so`, `.dll` or `.jnilib`) into
folder `resources/natives`.

## Optionally: Install receipt printer

### EPSON TM-U220 dot matrix printer

* Create udev rule for receipt printer.
(Don't edit `/lib/udev/rules.d/50-udev-default.rules`, will be overwritten on update.)
Create new file `/etc/udev/rules.d/99-own.rules`, add:
```
# for EPSON TM-U220 receipt printer:
KERNEL=="ttyUSB0", GROUP="lp", MODE="0660"
```

* Install Linux drivers for EPSON TM-U220 printer.


## Optionally: Install customer display

* Create udev rule for customer display, according to https://github.com/signal11/hidapi/blob/master/udev/99-hid.rules:
In file `/etc/udev/rules.d/99-own.rules` add:
```
# for Wincor/Nixdorf BA63 USB customer display:
# HIDAPI/libusb
SUBSYSTEM=="usb", ATTRS{idVendor}=="0aa7", ATTRS{idProduct}=="0200", MODE="0666"
```
Might need to change the hexnumbers of vendor id and product id according to
your make and model. Get the numbers (in decimal) by running Weltladenkasse on
terminal with display plugged in.


## Optionally: Configure barcode scanner

* Configure scanner prefix as "ctrl-c/alt-c" (to gain barcode box focus for every
scan) according to "Honeywell MetroSelect Single-Line Scanner Configuration
Guide":
    1. Scan barcode "Enter/Exit Configuration Mode" on page 1-1 (p. 7)
    2. Scan barcode "Configurable Prefix Character #1" on page 8-1 (p. 43)
    3. Scan barcodes "1", "7", "5" on page 16-1 (81) for left ctrl key /
	"1", "7", "4" on page 16-1 (81) for left alt key
    4. Scan barcode "Configurable Prefix Character #2" on page 8-1
    5. Scan barcodes "0", "9", "9" on page 16-2 (82) for "c"
    6. Scan barcode "Enter/Exit Configuration Mode" on page 1-1

Set beeper options with codes on page 7-4 (p. 36).


## Optionally: Python scripts

To run the script `einkaufspreise.py` (which sets discount rates and order
prices), you need to download
* mysql-connector-python-2.1.3.tar.gz (platform independent): https://dev.mysql.com/downloads/connector/python/
untar and run:
```
python ./setup.py install
```
or, alternatively, use pip:
```
$ sudo pip install mysql-connector
```
or your package manager, e.g. on Arch:
```
$ sudo pacman -S python-mysql-connector
```


## Optionally: Good profilers:
* JProfiler (proprietary): https://www.ej-technologies.com/download/jprofiler/files
* Profiler4j: http://profiler4j.sourceforge.net/


## Compile:
```
ant
```

Compile and run locally:
```
ant && java -jar Weltladenkasse_v1.2.2.jar
ant && java -jar Weltladenbesteller_v1.2.2.jar
```

Compile and make release (**CAUTION:** default release dir is `../releases`, adjust to your needs):
```
ant && ./make_release.sh
```

Compile without jar creation (much faster) and run locally:
```
ant develop && ./run_kasse.sh
ant develop && ./run_besteller.sh
```
