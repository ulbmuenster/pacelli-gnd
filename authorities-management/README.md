# Das Authorities Management Projekt

Dieses Projekt ist gestartet als Teil des Pacelli-GND-Projekts, ist aber darüber hinaus auch in weiteren Projekten
nützlich.

Es handelt sich um einen REST-basierten Service, der die Nutzung der SRU-Schnittstelle der GND erleichtert. Der 
Service ermöglicht sowohl die Suche in der GND als auch die Erzeugung neuer bzw. die Aktualisierung bereits bestehender
Datensätze. Durch die Auswertung der Antworten im Fehlerfall erhält der Nutzer die Möglichkeit die übermittelten Daten
noch einmal zu korrigieren.

## Konfiguration

Die Konfiguration erfolgt in der Datei `src/main/resources/application.yml`. Dort finden sich Einstellungen, die 
dynamisch durch Einstellungen im Secret `ams-secret` ersetzt werden müssen, weil die Werte nicht in einem Git-Repository
veröffentlicht werden sollten. Ein Weg ist, sich eine Datei `application-prod.yml` mit folgendem Inhalt zu erstellen:
```yaml
dnb:
  gnd:
    service:
      token:
        AuthoritiesManagerL1: abc
        AuthoritiesManagerL2: abc
        AuthoritiesManagerL3: Hier das Passwort für Level 3 eintragen
        AuthoritiesManagerL4: abc
        AuthoritiesManagerL5: abc
        AuthoritiesManagerL6: abc
        AuthoritiesManagerL7: abc
      update:
        url: https://services.dnb.de/sru_ru/
      search:
        url: https://services.dnb.de/sru/cbs
        token: Hier das Token eintragen
```
Daraus wird dann mit dem Kommando
```shell
kubectl create secret generic ams-secret --from-file=application.yml=src/main/resources/kubernetes/application-prod.yml
```
das Secret generiert.

Durch die Verwendung von Quarkusprofilen wird auch die Verwendung des GND-Testsystems unterstützt, so dass bei der 
Entwicklung keine fehlerbehafteten (Test)-Daten erstellt werden. Das GND-Testsystem unterstützt z.T. andere Indizes
als das Produktivsystem.

## Erzeugen und Aufruf der Anwendung

Die Anwendung wird mit dem Kommando 
```shell
mvn clean package
``` 
erzeugt. Außer der Anwendung wird ein Docker-Container erstellt und in das Repository `ulb.wwu.io` committed.
Im Verzeichnis `target/kubernetes` werden auch Kubernetes-Artefakte generiert, die ein Deployment, den Service und die 
nötigen Role's und RoleBinding's erzeugen, die für den (lesenden) Zugriff auf das Secret `ams-secret`
benötigt werden.

