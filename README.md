# 🌱 EcoApp - Guida per l'Utente Finale

EcoApp è la tua guida personale verso uno stile di vita più sostenibile. Monitora il tuo impatto ambientale, completa sfide e scala i livelli insieme ai tuoi amici!

---

## 📲 Come Installare l'Applicazione

Poiché EcoApp è attualmente in fase di sviluppo (Beta), l'installazione avviene tramite il file **APK**. Segui questi semplici passaggi:

1. **Scarica il file APK**: Ottieni il file `app-debug.apk`
2. **Trasferimento**: Se hai scaricato il file sul PC, invialo al tuo smartphone via cavo, email o cloud (Google Drive/Telegram).
3. **Autorizza l'installazione**:
   - Apri il file sul tuo smartphone.
   - Se compare un avviso di sicurezza, clicca su **Impostazioni** e attiva l'opzione **"Autorizza da questa fonte"**.
4. **Installa**: Torna indietro e clicca su **Installa**.
5. **Avvio**: Una volta terminato, clicca su **Apri**.

> **Nota importante**: Al primo avvio, il caricamento del login o della registrazione potrebbe richiedere circa **30-60 secondi**. Questo succede perché il server (ospitato su Render) ha bisogno di qualche istante per "svegliarsi" dopo un periodo di inattività. Una volta avviato, l'app sarà velocissima!

---

## 🎮 Funzionalità Principali

### 1. Le Tue Missioni (Quest)
Nella sezione **Quest**, troverai sfide quotidiane per ridurre il tuo impatto ambientale:
- **Accetta**: Sfoglia le missioni globali e accetta quelle che preferisci.
- **Aggiorna**: Segna i tuoi progressi con i tasti **+** e **-**.
- **Completa**: Raggiungi l'obiettivo per guadagnare punti e salvare CO2!

### 2. Il Tuo Profilo Eco
Monitora i tuoi traguardi in tempo reale:
- **Punti e Livelli**: Scala i ranghi da *Eco-Novizio* a *Eco-Leggenda*.
- **Badge**: Sblocca medaglie digitali per i tuoi successi (es. "Amico della Terra", "Influencer Ambientale").
- **Statistiche**: Guarda quanta CO2 totale hai salvato grazie alle tue azioni.

### 3. Social e Amici
- **Cerca**: Trova i tuoi amici tramite il loro nickname o email.
- **Confronta**: Visualizza i profili dei tuoi amici per vedere il loro impatto e i loro badge.
- **Richieste**: Gestisci le richieste di amicizia in sospeso direttamente nella lista amici.

---

## 💡 Consigli per l'uso
- **Controlla il Profilo**: Dopo aver completato una missione, torna nella scheda Profilo per vedere il tuo nuovo punteggio aggiornato.
- **Dettagli Badge**: Clicca su un badge nel tuo profilo per leggere la descrizione e come lo hai ottenuto.
- **Scroll**: Se una missione ha una descrizione lunga, puoi scorrere la pagina per leggere tutti i dettagli e gli obiettivi EU collegati.

---

## 🛠 Per Sviluppatori

### Stack Tecnologico
- **App Android**: Java, Gradle, Material Design
- **Backend**: Node.js, Express.js, MongoDB Atlas
- **Autenticazione**: JWT (jsonwebtoken), bcryptjs
- **Email**: Resend API (opzionale)

### Setup Backend Locale

```bash
cd backend
npm install
```

Crea un file `.env` nella cartella `backend`:
```env
PORT=3000
MONGODB_URI=mongodb+srv://user:password@cluster.mongodb.net/ecoapp
JWT_SECRET=your-secret-key
RESEND_API_KEY=re_xxxxx  # opzionale
```

Avvia il server:
```bash
npm start
```

### Endpoints API Principali

Tutte le rotte (tranne auth) richiedono header: `Authorization: Bearer <token>`

| Categoria | Metodo | Endpoint | Descrizione |
|-----------|--------|----------|-------------|
| **Auth** | POST | `/api/auth/register` | Registrazione utente |
| | POST | `/api/auth/login` | Login (ritorna JWT) |
| **Profilo** | GET | `/api/user/profile` | Ottieni profilo |
| | PUT | `/api/user/profile` | Aggiorna profilo |
| **Amici** | GET | `/api/user/friends` | Lista amici |
| | POST | `/api/user/friends/request` | Invia richiesta |
| | POST | `/api/user/friends/respond` | Accetta/rifiuta |
| **Quest** | GET | `/api/quests` | Quest globali disponibili |
| | GET | `/api/user/quests` | Progressi utente |
| | POST | `/api/user/quests/update` | Aggiorna progresso |

### Sistema di Gamification

**Livelli** (basati sui punti totali):
| Livello | Punti |
|---------|-------|
| Eco-Novizio | 0+ |
| Eco-Apprendista | 1.000+ |
| Eco-Guerriero | 2.000+ |
| Eco-Eroe | 5.000+ |
| Eco-Leggenda | 10.000+ |

**Badge** sbloccabili:
- 🌱 *Eco-Novizio* - Registrazione completata
- 🌿 *Pioniere Verde* - Prima quest completata
- 🌍 *Amico della Terra* - 10kg CO2 risparmiati
- 👥 *Influencer Ambientale* - 5 amici aggiunti
- ⚔️ *Eco-Guerriero* - 2000 punti raggiunti
- 🦸 *Salvatore del Pianeta* - 100kg CO2 risparmiati

### Deploy Backend su Render.com

> **Nota**: Il backend è già deployato e funzionante su `https://ecoapp-p5gp.onrender.com`. L'app Android è configurata per usare questo server. Le istruzioni seguenti sono per chi volesse deployare una propria istanza.

1. Crea account su [render.com](https://render.com)
2. "New +" → "Web Service" → Connetti repo GitHub
3. Configura:
   - **Root Directory**: `backend`
   - **Build Command**: `npm install`
   - **Start Command**: `npm start`
4. Aggiungi Environment Variables:
   - `MONGODB_URI` - Crea un cluster gratuito su [MongoDB Atlas](https://www.mongodb.com/atlas) e copia la connection string
   - `JWT_SECRET` - Una stringa segreta a tua scelta (es. `openssl rand -hex 32`)
   - `RESEND_API_KEY` - (opzionale) Per invio email, crea account su [resend.com](https://resend.com)
5. Aggiorna `BASE_URL` in `ApiClient.java` con il nuovo URL Render

---
*UNIVE - EcoGroup: Ingegneria del Software 2025/2026* 🌍✨
