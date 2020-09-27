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

Auf Arch-System installieren:
-------------------------------

1. Terminal öffnen und ins Verzeichnis mit der Software wechseln (wo sich
diese Datei `README.md` befindet):
```
cd Pfad/zu/meinem/Verzeichnis
```
2. Skript install-arch.sh ausführen:
```
./install-arch.sh
```
3. Bei Bedarf Namen des Quittungsdruckers in Datei `config.properties` anpassen


Software auf Ubuntu ausführen:
------------------------------
```
java -jar Weltladenkasse_v1.5.0.jar
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

* mariadb-java-client-2.6.2.jar: https://downloads.mariadb.org/connector-java/
* jOpenDocument-1.3.jar: http://www.jopendocument.org/downloads.html
* date4j.jar: http://www.date4j.net
* jcalendar-1.4.jar: http://toedter.com/jcalendar/
* hidapi-1.1.jar: https://code.google.com/p/javahidapi/
* log4j-api-2.13.3.jar and log4j-core-2.13.3.jar (extracted from apache-log4j-2.13.3-bin.tar.gz): https://logging.apache.org/log4j/2.x/download.html

For using TSE (German fiscalisation "Secure Element") from Bundesdruckerei/D-Trust/cryptovision:

* jna-5.6.0.jar: https://github.com/java-native-access/jna#download
* bcprov-jdk15on-166.jar: http://www.bouncycastle.org/latest_releases.html (only needed to compile cryptovision TSE test code)
* `libse-msc-io_linux-x86-64.so`: https://tse-support.cryptovision.com/confluence/display/TDI/cryptovision+TSE+-+Download-Bereich
    (download `SE-API-Java.zip`, required file is in `se-api-impl/dll/`, put it inside folder `lib/linux-x86-64/`)

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
(
https://download.epson-biz.com/modules/pos/index.php?page=soft&pcat=5&scat=32
https://download.epson-biz.com/modules/pos/index.php?page=single_soft&cid=6408&scat=32&pcat=5
https://epson.com/Support/Point-of-Sale/Impact-Printers-%28Dot-Matrix%29/Epson-TM-U220/s/SPT_C31C514103?review-filter=Linux
)
* For installation on Arch:
  * Install: cmake gcc libcups cups
  * Enable and start org.cups.cupsd.service or disable org.cups.cupsd.service and enable org.cups.cupsd.socket (see https://wiki.archlinux.org/index.php/CUPS)
  * sudo ./build.sh
  * sudo ./install.sh
  * Turn on and connect printer
  * sudo lpadmin -p TM-U220 -v socket://localhost/TM-U220 -P ppd/tm-impact-receipt-rastertotmir.ppd -E
  * (Open http://localhost:631 and add Epson printer)

* In CUPS:
  * Enter as URI: serial:/dev/ttyUSB0
  * And enter correct settings from Self-Test (Press FEED button while turning printer on)
```
Baud Rate: 9600
Parity = None
Bits = 8
Flow Control = DTR/DSR
```
  * Or enter directly the URL: serial:/dev/ttyUSB0?baud=9600+bits=8+parity=none+flow=dtrdsr
  * Use printer name `quittungsdrucker`.
  * At least on Arch, might need to manually choose the PPD file from the downloaded and
    built printer driver dir in CUPS.

  * Set paper roll width to 58 mm
  * Set lower resolution for faster printing

* After installing printer in CUPS (Can follow instructions in
  tmu-cups/manual/UsersManual.en.html), print using this command:
$ lpr -P quittungsdrucker test.txt

 Paper width    number of chars
--------------------------------
    57 mm        31 (33 max.)


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
ant && java -jar Weltladenkasse_v1.5.0.jar
ant && java -jar Weltladenbesteller_v1.5.0.jar
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
