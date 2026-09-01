# Cyber IPTV 0.2 – Android / Fire TV

Erste private MVP-Version eines reinen IPTV-Players. Die App enthält keine Sender und keinen Zugang. Sie funktioniert ausschließlich mit rechtmäßig bereitgestellten Xtream-Codes-Zugangsdaten.

## Enthalten

- Parallele Prüfung der Server beim App-Start; der schnellste gültige Server gewinnt
- Der zuletzt funktionierende Server wird bevorzugt und dauerhaft gemerkt
- Automatischer API-Failover mit kurzen Verbindungszeiten statt minutenlangem Warten
- Automatischer Stream-Failover im Player, wenn die Wiedergabe abbricht
- Acht bereitgestellte Server-/Backup-URLs bleiben zusätzlich direkt auswählbar
- Live-TV mit Media3/ExoPlayer und HLS
- Filme
- Serienübersicht
- Suche
- Favoriten per langem Tastendruck bzw. langer OK-Taste
- Fire-TV-Launcher und Fernbedienungsfokus
- Ausgewogener Puffer: schneller Start, größere Reserve bei schwankendem WLAN

## APK bauen

1. Projektordner in einer aktuellen Version von Android Studio öffnen.
2. Android SDK 36 installieren lassen und die Gradle-Synchronisierung abwarten.
3. **Build > Build APK(s)** wählen.
4. Die Debug-APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`.
5. Auf dem Fire TV die Entwickleroptionen und „Unbekannte Apps installieren“ für die verwendete Übertragungs-App erlauben und die APK installieren.

## Bedienung

- Beim Start werden der erste hinterlegte Server und die private Konfiguration automatisch verwendet.
- Alternativ eine der eingebauten Server-/Backup-URLs auswählen.
- Benutzername und Passwort eingeben.
- Sender/Film auswählen und OK drücken.
- Favorit: Eintrag lange gedrückt halten.

## Stand 0.2

Serien werden bereits aufgelistet; Episodenauswahl und echte EPG-Programmdaten sind für die nächste Version vorgesehen. Diese private Ein-Gerät-Version lädt die Zugangsdaten aus `app/src/main/assets/private_config.json`. Diese Datei darf niemals in ein öffentliches Repository hochgeladen werden. Projekt und APK deshalb nicht weitergeben.
