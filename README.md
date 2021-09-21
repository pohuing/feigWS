# feigWS
feigWS ist eine Webanwendung zum Konfigurieren und Benutzen von Feig UHF Lesegeräten zwecks Zweitmessung in Wettkämpfen.
Aktuell wird nur für den LRU1002 entwickelt.

## Aufbau des Quellcodes
Die Anwendung besteht aus zwei Komponenten. Ein Thread welcher ständig mit dem Leser kommuniziert nach neuen Tags fragt und loggt in BrmReadThread. 

Und eine Spring Webanwendung zum Konfigurieren des Lesers und Anzuzeigen der Logs.
