# Das Pacelli-GND-Projekt

Dieses Projekt besteht aus sechs einzelnen Modulen. Vier der sechs Module sind Kommandozeilen-Applikationen,
die dazu dienen, die im Projekt erzeugten Metadaten zu importieren, nach Marc21 zu transformieren, einer erste 
Anreicherung zu verbesserter Konformität mit den GND-Normen durchzuführen und ein Mapping für ein im Projekt erzeugtes Feld auf 
Schlagwörter zu erzeugen.

## Die Module
### import-authorities

Dieses Modul ist eine Kommandozeilenapplikation. Sie dient dem Import der im Pacelli-Projekt erfassten Personendaten
in eine Datenbank. Es finden keine weiteren Anpassungen oder Transformationen der Daten statt.

### transform-pacelli-marc21

Eine weitere Kommandozeilenapplikation, die das Metadatenformat des Pacelli-Projekts in Marc21 überführt.

### enrich-marc21

Diese Applikation überprüft in Datensätzen, die eine GND-Id enthalten, ob es sich dabei um individualisierte oder 
nicht-individualisierte Personendaten handelt. Handelt es sich um nicht-individualisierte Daten, so wird die GND-Id
entfernt, weil sie nicht verwendet werden kann.

### prepare-meta-mapping

Die Daten der Pacelli-Edition enthalten ein Feld `meta` mit Informationen, die auf kein Feld der GND abgebildet werden 
können. Zu großen Teilen sind dies biographische Angaben, die (in ähnlicher Form) bereits in einem entsprechenden Feld 
vorhanden sind. Um die Angaben verwenden zu können, werden `meta`-Felder, die nur aus einem Wort bestehen, auf 
GND-Schlagwörter und Synonyme abgebildet.
Dies ist ein Prozess, der durch eine verbesserte Analyse der Einträge (z.B. durch Natural Language Processing) optimiert 
werden kann.

### authorities-management

Es handelt sich um einen REST-basierten Service, der die Nutzung der SRU-Schnittstelle der GND erleichtert. Der 
Service ermöglicht sowohl die Suche in der GND als auch die Erzeugung neuer bzw. die Aktualisierung bereits bestehender
Datensätze. Durch die Auswertung der Antworten im Fehlerfall erhält der Nutzer die Möglichkeit die übermittelten Daten
noch einmal zu korrigieren.
 
### authority-editor

Diese Webapplikation dient der Kontrolle und einer einfachen Möglichkeit der Bearbeitung der entstandenen Marc21-Daten, 
da es nicht empfehlenswert ist, die Daten ohne die Kontrolle durch eine erfahrene bibliothekarische Fachkraft 
in die GND einzuspielen. Die Applikation dient dazu, die bereits im Projekt ermittelten GND-ID's zu überprüfen (im 
Modul enrich-marc21 wird ja nur überprüft, ob es sich um einen Pn-Satz handelt). Die durch das Projekt hinzugefügten 
Daten werden hierbei visuell hervorgehoben.
Für Datensätze ohne GND-ID kann eine Suche durchgeführt werden. Es wird beachtet, dass nur individualisierte 
Personendaten zur Auswahl angeboten werden. Wird kein Treffer in der GND entdeckt ist noch zu überprüfen, ob die 
vorliegenden Daten zur Erzeugung eines individualisierten Personennormdatensatzes ausreichen. 

## Erzeugen der Anwendungen

Die Anwendungen werden mit dem Kommando `mvn package` erzeugt. Dies erzeugt sämtliche Module auf einmal.
Alternativ kann auch jedes Modul einzeln erzeugt werden. Vgl. die entsprechenden `README.md`-Dateien in den Unterverzeichnissen. 
