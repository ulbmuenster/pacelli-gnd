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

Die Konfiguration erfolgt in der Datei `src/main/resources/application.yml`. Einige der Einstellungen dort sind reine 
Platzhalter, die von Einstellungen in der ConfigMap `mae-cm` und dem Secret `mae-secret` überschrieben werden.
Die entsprechenden Definitionen finden sich nicht im Git-Repository, weil sie Zugangsdaten enthalten, die nicht
veröffentlicht werden sollen. Eine Definition der ConfigMap `mae-cm` sieht so aus:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mae-cm
data:
  quarkus.datasource.jdbc.url: "Hier die jdbc-URL des DB-Servers eintragen"
  quarkus.oidc.auth-server-url: "Hier die URL des OIDC-Servers eintragen"
  quarkus.oidc.client-id: "Hier die Client-ID eintragen, die auf dem OIDC-Server zugeordnet ist"
  quarkus.oidc.authentication.scopes: "Hier die Scopes im OIDC-spezifischen Format eintragen"
  de.wwu.ulb.mae.users: "Hier eine Liste von Usern eintragen. Das Format hängt vom OIDC-Server ab"
  de.wwu.ulb.mae.oidc: "dex"
```
Erzeugt wird die ConfigMap dann mit
```shell
kubectl apply -f src/main/resources/kubernetes/mae-cm.yml -n pacelli-gnd
```
Das Secret `mae-secret` kann in einer Datei `application-prod.yml` wie folgt definiert werden:
```yaml
quarkus:
  datasource:
    username: DB-Userid
    password: Passwort
  oidc:
    client-id: client-id auf dem OIDC-Server
    credentials:
      client-secret:
        value: Secret des OIDC-Servers
        method: basic
```
Man erzeugt das Secret `mae-secret` daraus mit dem Kommando 
```shell
kubectl create secret generic mae-secret --from-file=application.yml=src/main/resources/kubernetes/application-prod.yml
```
Soll eine andere Datenbank als `PostgreSQL` verwendet werden, so muss auch `quarkus.datasource.db-kind`
entsprechend angepasst werden und der Jdbc-Treiber in der `pom.xml` ersetzt werden. Quarkus unterstützt standardmäßi 
neben PostgreSQL auch MySQL, MariaDB, H2, derby, DB2 und Microsoft SQL Server.
  
## Erzeugen und Aufruf der Anwendung

Die Anwendung wird mit dem Kommando
```shell
mvn clean package
``` 
erzeugt. Außer der Anwendung wird ein Docker-Container erstellt und in das Repository `ulb.wwu.io` committed.
Im Verzeichnis `target/kubernetes` werden auch Kubernetes-Artefakte generiert, die ein Deployment, den Service und die
nötigen Role's und RoleBinding's erzeugen, die für den (lesenden) Zugriff die ConfigMap `mae-cm` und auf das Secret `mae-secret`
benötigt werden.
