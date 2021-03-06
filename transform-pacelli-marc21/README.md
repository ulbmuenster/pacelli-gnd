# Transformiere Pacelli nach Marc21 Service

Dieser Service ist Teil des Pacelli-GND-Projekts, das die Daten, die im 
Rahmen der Erstellung der Pacelli-Edition erfasst worden sind, in die
GND überführt.

Dieser Teilservice überführt die Daten aus dem Pacelli-Format in das 
Marc21-XML-Format.

## Konfiguration

Die Konfiguration erfolgt in der Datei `src/main/resources/application.yml`. Dort sind die entsprechenden 
Einstellungen für `quarkus.datasource.username`, `quarkus.datasource.password` und `quarkus.datasource.jdbc.url`
vorzunehmen. Soll eine andere Datenbank als `PostgreSQL` verwendet werden, so muss auch `quarkus.datasource.db-kind`
entsprechend angepasst werden und der Jdbc-Treiber in der `pom.xml` ersetzt werden. Quarkus unterstützt standardmäßi 
neben PostgreSQL auch MySQL, MariaDB, H2, derby, DB2 und Microsoft SQL Server.
  
## Erzeugen und Aufruf der Anwendung

Die Anwendung wird mit dem Kommando `mvn package` erzeugt.
Dabei wird die Datei `transform-pacelli-marc21-2.0.0-runner.jar` im `/target` Verzeichnis erstellt.
Die Datei ist aber ohne die Abhängigkeiten im `target/lib` Verzeichnis nicht lauffähig.

Die Anwendung kann mittels `java -jar target/transform-pacelli-marc21-2.0.0-runner.jar` ausgeführt werden.
Dafür ist mindestens Java 11 erforderlich.
