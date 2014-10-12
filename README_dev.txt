Compile:
#cd bin/; cd .. && ant && cd - && java Weltladenkasse.Kasse
#ant && cd build && java -cp ../lib/mysql-connector-java-5.1.25-bin.jar:. Weltladenkasse.Kasse; cd ..
#ant && java -jar Weltladenkasse_v0.9.3.jar
#ant && java -jar Weltladenbesteller_v0.9.3.jar
ant && ./make_release.sh

Create JAR file:
jar cfe Weltladenkasse_v0.9.3.jar Weltladenkasse.Kasse icons/ jcalendarbutton/ Weltladenkasse/

Download most recent MySQL Connector:
http://dev.mysql.com/downloads/connector/j/

Download one-jar-appgen-X.XX.jar:
http://one-jar.sourceforge.net/index.php?page=downloads&file=downloads
Build:
    $ java -jar one-jar-appgen-0.97.jar
        Enter project path (project name is last segment): c:/tmp/test-one-jar
        Enter java package name: com.example.onejar
    $ cd c:/tmp/test-one-jar 
    $ ant 
    $ cd build 
    $ java -jar test-one-jar.jar
        test_one_jar main entry point, args=[]
        test_one_jar main is running
        test_one_jar OK.
    Add source code to the src directory, library jars to the lib directory, and rebuild.

