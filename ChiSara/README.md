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
./gradlew test            # esegue ScoringTest (JVM, niente device)
```

## Build ed esecuzione su un dispositivo reale

> L'emulatore **non** riceve notifiche vere da WhatsApp/Telegram/Instagram:
> serve un telefono fisico con quelle app installate e attive.

### Opzione A — Android Studio (consigliata)
1. Apri la cartella `ChiSara/` in **Android Studio** (Giraffe o più recente).
2. Lascia che sincronizzi Gradle e scarichi l'Android SDK (compileSdk 34).
3. Collega il telefono via USB con il **debug USB** attivo.
4. Premi **Run ▶** e scegli il dispositivo.

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

Poi **disattiva le anteprime dei messaggi** nelle app sorgente (istruzioni
in-app), altrimenti il testo è già visibile nella tendina e il gioco non ha senso.

### Come testarlo
1. Concedi i permessi e disattiva le anteprime (almeno su una app, es. Telegram).
2. Fatti mandare un messaggio da un contatto.
3. Dovresti vedere **solo** "📩 Nuovo messaggio misterioso — tocca per indovinare".
4. Apri l'app, prova a indovinare **senza** aprire prima l'app sorgente.
5. Se apri WhatsApp/Telegram prima di rispondere → "hai sbirciato", 0 punti.

## ⚠️ Limitazioni note / note di fattibilità

Punti su cui il design ha dovuto scendere a compromessi con le API Android:

- **Anteprime vs. identità del mittente.** Su alcune app (in particolare
  WhatsApp) l'interruttore "Mostra anteprima" nasconde *sia* il testo *sia* il
  nome del mittente: con l'anteprima **off**, la notifica riporta solo "WhatsApp
  / Nuovo messaggio" e **il mittente non è disponibile** → non possiamo sapere
  chi è, quindi la notifica viene scartata. Il gioco funziona nel punto dolce in
  cui la notifica **contiene il nome del mittente ma non il testo** (tipico di
  Telegram con "anteprima messaggio" disattivata). WhatsApp è il caso più
  ostico; consiglio di testare prima con **Telegram**.
- **Race col drawer.** Cancelliamo e sostituiamo la notifica in
  `onNotificationPosted`, ma se è già stata mostrata come *heads-up* per una
  frazione di secondo l'utente potrebbe intravederla. È intrinseco all'approccio
  `NotificationListenerService`.
- **Re-post delle app.** WhatsApp/Telegram aggiornano la stessa notifica più
  volte ("sta scrivendo…", "N messaggi"). C'è un **dedupe** su `sbn.key` entro
  una finestra breve, ma i gruppi e i messaggi multipli restano un'area da
  raffinare (i gruppi per ora sono **esclusi dall'MVP**).
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
Statistiche con **grafico andamento del punteggio**) e una schermata
**Impostazioni** (DataStore) per scegliere le app tracciate e la penalità per le
risposte sbagliate. Prossimi passi suggeriti: gestione dei **messaggi di
gruppo**, hardening dei casi limite di re-post, e statistiche più ricche.
