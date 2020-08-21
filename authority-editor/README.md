# Authority Editor Projekt

Diese Webapplikation dient der Kontrolle und einer einfachen Möglichkeit der Bearbeitung der entstandenen Marc21-Daten, 
da es nicht empfehlenswert ist, die Daten ohne die Kontrolle durch eine erfahrene bibliothekarische Fachkraft 
in die GND einzuspielen. Die Applikation dient dazu, die bereits im Projekt ermittelten GND-ID's zu überprüfen (im 
Modul enrich-marc21 wird ja nur überprüft, ob es sich um einen Pn-Satz handelt). Die durch das Projekt hinzugefügten 
Daten werden hierbei visuell hervorgehoben.
Für Datensätze ohne GND-ID kann eine Suche durchgeführt werden. Es wird beachtet, dass nur individualisierte 
Personendaten zur Auswahl angeboten werden. Wird kein Treffer in der GND entdeckt ist noch zu überprüfen, ob die 
vorliegenden Daten zur Erzeugung eines individualisierten Personennormdatensatzes ausreichen. 

## Konfiguration

Die Konfiguration erfolgt in der Datei `src/main/resources/application.yml`. Dort sind die entsprechenden 
Einstellungen für `quarkus.datasource.username`, `quarkus.datasource.password` und `quarkus.datasource.jdbc.url`
vorzunehmen. Soll eine andere Datenbank als `PostgreSQL` verwendet werden, so muss auch `quarkus.datasource.db-kind`
entsprechend angepasst werden und der Jdbc-Treiber in der `pom.xml` ersetzt werden. Quarkus unterstützt standardmäßi 
neben PostgreSQL auch MySQL, MariaDB, H2, derby, DB2 und Microsoft SQL Server.
  
## Erzeugen und Aufruf der Anwendung

Die Anwendung wird mit dem Kommando `mvn package` erzeugt.
Dabei wird die Datei `authority-editor-2.0.0-runner.jar` im `/target` Verzeichnis erstellt.
Die Datei ist aber ohne die Abhängigkeiten im `target/lib` Verzeichnis nicht lauffähig.

Die Anwendung kann mittels `java -jar target/authority-editor-2.0.0-runner.jar` ausgeführt werden.
Dafür ist mindestens Java 11 erforderlich.
