Auf Ubuntu-System installieren:
-------------------------------

1. Terminal öffnen und ins Verzeichnis mit der Software wechseln (wo sich
diese Datei README.txt befindet):

    cd Pfad/zu/meinem/Verzeichnis

2. Skript install-ubuntu.sh ausführen:

    ./install-ubuntu.sh

Software ausführen:
-------------------

    java -jar Weltladenkasse_v0.9.3.jar

Eine Version des Java Runtime Environment (JRE) muss installiert sein,
um die Software ausführen zu können (z.B. mit dem Befehl 'sudo apt-get
install default-jre').

Datenbank-Daten im-/exportieren:
--------------------------------

1. Datenbank-Dump importieren im Registerreiter "DB Import/Export".
      Kassenadmin-Passwort (oben notiert) eingeben und SQL-Datei auswählen.

2. Nach Änderungen an der Datenbank: Datenbank-Dump exportieren im Registerreiter "DB Import/Export".
      Kassenadmin-Passwort (oben notiert) eingeben und SQL-Datei auswählen.

