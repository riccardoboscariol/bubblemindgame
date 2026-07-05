# Chi Sarà 🕵️ (prototipo MVP)

App Android nativa (Kotlin + Jetpack Compose + Material 3) che intercetta le
notifiche dei messaggi da **WhatsApp, Telegram e Instagram**, le nasconde dietro
una notifica "cieca", e ti fa **indovinare chi te le ha mandate**. Più il
contatto scrive di rado, più punti vali se ci azzecchi (scoring basato
sull'informazione, `-log2(p)`).

> ⚠️ Prototipo locale. Nessun backend, nessuna pubblicazione Play Store (il
> permesso *Notification access* richiede una *Sensitive Permissions
> Declaration* nella Play Console — se ne parla quando il prototipo funziona).

## Architettura

```
app/src/main/java/com/chisara/app/
├── ChiSaraApp.kt                 # Application: possiede il GameRepository condiviso
├── MainActivity.kt               # host Compose + deep link dalla notifica cieca
├── data/
│   ├── db/                       # Room: entità, DAO, database, converters
│   │   ├── entity/               # Contact, NotificationEvent, GuessAttempt
│   │   └── dao/
│   ├── settings/                 # DataStore: pacchetti tracciati + penalità (configurabili)
│   └── repository/GameRepository.kt   # unico source of truth (write: intercettazione, read: UI)
├── notification/
│   ├── ChiSaraNotificationListener.kt # NotificationListenerService + logica anti-sbirciatina
│   ├── BlindNotifier.kt          # pubblica la notifica cieca sostitutiva
│   ├── BootReceiver.kt           # riavvia il listener dopo il reboot
│   └── NotificationConfig.kt     # pacchetti tracciati (configurabili) + costanti canale
├── scoring/Scoring.kt            # funzione PURA e testabile (self-information / log-score)
├── usage/UsageChecker.kt         # UsageStatsManager: rileva la "sbirciatina"
├── permissions/PermissionUtils.kt# stato + deep link ai due permessi speciali
├── viewmodel/GameViewModel.kt    # MVVM: StateFlow per permessi, pending, stats, guess
└── ui/                           # Compose: onboarding, home, guess, reveal, stats

app/src/test/java/.../ScoringTest.kt   # unit test della logica di scoring
```

Pattern: **MVVM** con `ViewModel` + `StateFlow`, Room per la persistenza, tutto
locale sul device.

## Come funziona la logica anti-sbirciatina

1. `onNotificationPosted` scarta subito ciò che non è un'app tracciata, le
   notifiche di riepilogo/gruppo/ongoing e i mittenti non identificabili.
2. Se il **corpo del messaggio è già leggibile** (anteprime attive), la notifica
   viene lasciata stare e **non entra nel gioco** — la vedresti comunque.
3. Altrimenti: si salva il vero mittente, si **cancella** la notifica originale
   (`cancelNotification`) e si pubblica una notifica cieca generica.
4. Quando rispondi, `UsageChecker` interroga `UsageStatsManager`: se il pacchetto
   sorgente è andato in foreground **tra l'arrivo e la risposta**, la sessione è
   **invalidata** ("hai sbirciato") e non prende punti.

## Scoring (testabile in isolamento)

`Scoring` è puro Kotlin, zero dipendenze Android:

```
p(contatto) = (messaggi_dal_contatto + 1) / (messaggi_totali_app + n_contatti)   # Laplace smoothing
punteggio   = round(K * -log2(p)),  clampato in [0, maxScore]                     # se corretto
            = wrongAnswerPenalty (0 di default, configurabile)                    # se sbagliato
```

Esegui i test:

```bash
./gradlew test               # unit test JVM: ScoringTest + AppSettingsTest (niente device)
./gradlew connectedAndroidTest   # test strumentati Room in-memory (serve un device/emulatore)
```

## Build ed esecuzione su un dispositivo reale

> L'emulatore **non** riceve notifiche vere da WhatsApp/Telegram/Instagram:
> serve un telefono fisico con quelle app installate e attive.

### Opzione A — Android Studio (consigliata)
1. Apri la cartella `ChiSara/` in **Android Studio** (Giraffe o più recente).
2. Lascia che sincronizzi Gradle e scarichi l'Android SDK (compileSdk 34).
3. Collega il telefono via USB con il **debug USB** attivo.
4. Premi **Run ▶** e scegli il dispositivo.

### Opzione C — scarica l'APK dalla CI (senza compilare nulla)
Ad ogni push su questo repo, la GitHub Action **Android CI**
(`.github/workflows/android.yml`) esegue gli unit test e compila l'APK debug.
Vai su **Actions → ultimo run → Artifacts** e scarica **`chisara-debug-apk`**,
poi installalo sul telefono:
```bash
adb install -r app-debug.apk
```

### Opzione B — riga di comando
```bash
cd ChiSara
# crea local.properties con il path del tuo Android SDK:
echo "sdk.dir=/percorso/al/tuo/Android/sdk" > local.properties

./gradlew assembleDebug                       # compila l'APK
./gradlew installDebug                         # installa sul device collegato
# oppure manualmente:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Primo avvio (onboarding)
Al primo avvio l'app richiede **due permessi speciali** (non sono runtime
permission, vanno concessi dalle Impostazioni di sistema — l'app fa il deep link):

1. **Accesso alle notifiche** (`Notification access`) → per intercettare/sostituire.
2. **Accesso all'utilizzo** (`Usage access` / `PACKAGE_USAGE_STATS`) → per l'anti-sbirciatina.
3. Su Android 13+ anche il permesso **notifiche** (`POST_NOTIFICATIONS`).

Di default è attiva la modalità **"gioca anche con le anteprime attive"**: l'app
intercetta il messaggio, prende il mittente e **nasconde il testo al posto tuo**.
Così funziona su WhatsApp/Telegram/Instagram con le impostazioni di fabbrica,
senza dover disattivare le anteprime. In Impostazioni puoi passare alla modalità
"pura" (gioca solo con i messaggi già oscurati dall'app).

### Come testarlo
1. Concedi i due permessi dall'onboarding.
2. Fatti mandare un messaggio da un contatto (funziona su tutte e tre le app).
3. Dovresti vedere **solo** "📩 Nuovo messaggio misterioso — tocca per indovinare".
4. Apri l'app, prova a indovinare **senza** aprire prima l'app sorgente.
5. Se apri WhatsApp/Telegram/Instagram prima di rispondere → "hai sbirciato", 0 punti.

## ⚠️ Limitazioni note / note di fattibilità

Punti su cui il design ha dovuto scendere a compromessi con le API Android:

- **Anteprime e identità del mittente.** Con le anteprime **attive** (default di
  tutte e tre le app) la notifica contiene mittente + testo: la modalità di
  default "gioca anche con le anteprime attive" intercetta e nasconde il testo,
  usando il **titolo** come mittente. Con le anteprime **disattivate**, quasi
  tutte le app nascondono *anche il nome del mittente* (title = "WhatsApp",
  "Nuovo messaggio"): in quel caso non sappiamo chi è e la notifica viene
  scartata. Quindi, controintuitivamente, per giocare conviene tenere le
  anteprime **attive** e lasciar fare all'app.
- **Race col drawer / heads-up.** Cancelliamo e sostituiamo la notifica in
  `onNotificationPosted`, ma se è già stata mostrata come *heads-up* per una
  frazione di secondo l'utente potrebbe intravederla. È il compromesso della
  modalità "gioca con le anteprime attive" e intrinseco a
  `NotificationListenerService`; la modalità "pura" lo evita ma fa partire molti
  meno round. L'anti-sbirciatina via UsageStats copre l'apertura dell'app, non
  l'occhiata di striscio al banner.
- **Re-post delle app.** WhatsApp/Telegram aggiornano la stessa notifica più
  volte ("sta scrivendo…", "N messaggi"). C'è un **dedupe** su `sbn.key` entro
  una finestra breve; i messaggi multipli ravvicinati restano un'area da
  raffinare.
- **Gruppi.** Disattivati di default, attivabili da Impostazioni. Funzionano solo
  quando la notifica del gruppo espone il **nome di chi ha scritto** (tipicamente
  via MessagingStyle): se l'app mostra solo il nome del gruppo, non è indovinabile
  e viene scartato.
- **UsageStatsManager.** L'accesso all'utilizzo va concesso a mano; su alcune
  ROM OEM il deep link non evidenzia la nostra app. Inoltre gli eventi hanno una
  granularità/latenza non nulla: applichiamo un piccolo margine anti-skew.
- **Restrizioni background / reboot.** Il `NotificationListenerService` è
  generalmente esente dalle ottimizzazioni batteria e viene ri-collegato dal
  sistema; `BootReceiver` chiama `requestRebind` dopo il reboot/aggiornamento.
  Su alcuni OEM aggressivi (Xiaomi, Huawei…) può servire disattivare a mano
  l'ottimizzazione batteria per l'app.
- **Play Store.** Non affrontato qui volutamente.

## Stato dell'MVP

Implementato: struttura Gradle, listener con anti-sbirciatina, Room (3 entità),
scoring puro + unit test, schermate Compose (Onboarding, Home, Guess, Reveal,
Statistiche con **grafico andamento del punteggio**), schermata **Impostazioni**
(DataStore: app tracciate, penalità, inclusione gruppi), **gestione opzionale dei
messaggi di gruppo** (indovina chi ha scritto nel gruppo), e test strumentati
Room in-memory sul flusso intercettazione→guess.

Include anche una **migrazione Room 1→2** (niente più azzeramento dello storico
al cambio schema) e una **GitHub Action** che builda l'APK debug + gira i test ad
ogni push, con l'APK scaricabile come artifact.

Prossimi passi suggeriti (post-MVP): hardening dei casi limite di
re-post/gruppi su più OEM, statistiche più ricche, e la *Sensitive Permissions
Declaration* per la pubblicazione.
