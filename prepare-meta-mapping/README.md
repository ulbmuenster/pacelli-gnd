# Prepare Meta Mapping Service

Dieser Service ist Teil des Normdaten-Projekts, das die Daten, die im 
Rahmen der Erstellung der Pacelli-Edition erfasst worden sind, in die
GND überführt.

Dieser Teilservice sucht zu den Meta-Informationen passende Schlagworte 
in der GND, die dann in die entsprechenden Datensätze eingefügt werden können.

## Konfiguration

Die Konfiguration erfolgt in der Datei `src/main/resources/application.yml`. Dort sind die entsprechenden 
Einstellungen für `quarkus.datasource.username`, `quarkus.datasource.password` und `quarkus.datasource.jdbc.url`
vorzunehmen. Soll eine andere Datenbank als `PostgreSQL` verwendet werden, so muss auch `quarkus.datasource.db-kind`
entsprechend angepasst werden und der Jdbc-Treiber in der `pom.xml` ersetzt werden. Quarkus unterstützt standardmäßi 
neben PostgreSQL auch MySQL, MariaDB, H2, derby, DB2 und Microsoft SQL Server.
  
## Erzeugen und Aufruf der Anwendung

Die Anwendung wird mit dem Kommando `./mvnw package` erzeugt.
Dabei wird die Datei `prepare-meta-mapping-2.0.0-runner.jar` im `/target` Verzeichnis erstellt.
Die Datei ist aber ohne die Abhängigkeiten im `target/lib` Verzeichnis nicht lauffähig.

Die Anwendung kann mittels `java -jar target/prepare-meta-mapping-2.0.0-runner.jar` ausgeführt werden.
Dafür ist mindestens Java 11 erforderlich.
