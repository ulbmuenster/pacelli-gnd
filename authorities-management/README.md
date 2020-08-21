# Das Authorities Management Projekt

Dieses Projekt ist gestartet als Teil des Pacelli-GND-Projekts, ist aber darüber hinaus auch in weiteren Projekten
nützlich.

Es handelt sich um einen REST-basierten Service, der die Nutzung der SRU-Schnittstelle der GND erleichtert. Der 
Service ermöglicht sowohl die Suche in der GND als auch die Erzeugung neuer bzw. die Aktualisierung bereits bestehender
Datensätze. Durch die Auswertung der Antworten im Fehlerfall erhält der Nutzer die Möglichkeit die übermittelten Daten
noch einmal zu korrigieren.

## Konfiguration

Die Konfiguration erfolgt in der Datei `src/main/resources/application.yml`. Dort finden sich Einstellungen, die durch 
lokale Parameter ersetzt werden müssen, z.B. wird in `dnb.gnd.service.search.token` das Token eingetragen, welches von 
der DNB für die Nutzung der GND zugewiesen wird.

Durch die Verwendung von Quarkusprofilen wird auch die Verwendung des GND-Testsystems unterstützt, so dass bei der 
Entwicklung keine fehlerbehafteten (Test)-Daten erstellt werden. Das GND-Testsystem unterstützt z.T. andere Indizes
als das Produktivsystem.

## Erzeugen und Aufruf der Anwendung

Die Anwendung wird mit dem Kommando `mvn package` erzeugt.
Dabei wird die Datei `authorities-management-2.0.0-runner.jar` im `/target` Verzeichnis erstellt.
Die Datei ist aber ohne die Abhängigkeiten im `target/lib` Verzeichnis nicht lauffähig.

Die Anwendung kann mittels `java -jar target/authorities-management-2.0.0-runner.jar` ausgeführt werden.
Dafür ist mindestens Java 11 erforderlich.

