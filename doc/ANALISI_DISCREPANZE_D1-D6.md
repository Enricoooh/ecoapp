# Analisi delle Discrepanze tra Documenti (D1-D6) e Codebase

**Data Analisi:** 19 gennaio 2026  
**Versione:** 1.0  
**Focus:** Creazione Utente e Login

---

## 1. Riepilogo Esecutivo

L'analisi ha identificato **discrepanze significative** tra i documenti di specifica (D1-D6) e l'implementazione effettiva nella codebase, in particolare per quanto riguarda il modulo di autenticazione (login e registrazione).

### Discrepanza Principale Critica
> **Il modello architetturale descritto nei documenti D3 e D5 prevede una gestione utenti amministrata (admin-managed), mentre l'implementazione reale utilizza un modello di auto-registrazione (self-registration).**

---

## 2. Analisi per Documento

### 2.1 D1 - Proposta Iniziale

**Status:** ✅ Coerente con l'implementazione

Il documento D1 descrive ad alto livello i servizi dell'applicazione senza entrare nel dettaglio del flusso di registrazione/login. Non ci sono discrepanze significative.

---

### 2.2 D2 - Piano di Progetto

**Status:** ✅ Coerente con l'implementazione

Il documento D2 è un piano organizzativo che non descrive funzionalità tecniche di login/registrazione. Nessuna discrepanza rilevata.

---

### 2.3 D3 - Documento dei Requisiti

**Status:** ❌ DISCREPANZE CRITICHE

#### 2.3.1 Sezione "Registrazione / Creazione Utente"

| Requisito | Descrizione D3 | Implementazione Reale | Discrepanza |
|-----------|----------------|----------------------|-------------|
| **RF27** | "L'amministratore deve poter creare un nuovo account utente" | L'utente si registra autonomamente via `POST /api/auth/register` | ❌ **CRITICA** - Nessun admin |
| **RF28** | "L'amministratore deve poter specificare email, nickname, ruolo, stato iniziale" | Non esiste pannello admin | ❌ **CRITICA** |
| **RF29** | "Il sistema genera password temporanea e la invia via email" | L'utente sceglie la propria password | ❌ **CRITICA** |
| **RF30** | "Password temporanea deve avere scadenza (es. 24h)" | Non implementato | ❌ **CRITICA** |
| **RF31** | "Al primo accesso il sistema richiede cambio password" | Non implementato | ❌ Non esiste primo accesso forzato |
| **RF32** | "Validazione formato email e password min 6 caratteri" | ✅ Implementato | ✅ OK |
| **RF33** | "Invio email di benvenuto alla creazione account" | ✅ Implementato via Resend API | ✅ OK |
| **RF34** | "Registrazione fallisce se email già esistente" | ✅ Implementato (409 Conflict) | ✅ OK |
| **RF35** | "Messaggio di errore specifico se email già registrata" | ✅ Implementato | ✅ OK |
| **RF36** | "Admin può assegnare ruolo (es. utente standard, moderatore)" | Non esiste sistema ruoli | ❌ **CRITICA** |
| **RF37** | "Sistema registra log di ogni operazione di creazione" | Non implementato audit log | ❌ Non implementato |
| **RF38** | "Se invio email fallisce, account creato comunque" | ✅ Implementato (try-catch Resend) | ✅ OK |

#### 2.3.2 Sezione "Login"

| Requisito | Descrizione D3 | Implementazione Reale | Discrepanza |
|-----------|----------------|----------------------|-------------|
| **RF19** | "Account non attivo → messaggio appropriato" | Non esiste stato "attivo/non attivo" | ❌ **CRITICA** |
| **RF20** | "Blocco dopo 5 tentativi falliti per 15 minuti" | ✅ Implementato in `server.js` | ✅ OK |
| **RF21** | "Token sessione con scadenza configurabile" | ✅ Implementato (7d default, 30d remember me) | ✅ OK |
| **RF22** | "Opzione 'Rimani connesso' estende scadenza" | ✅ Implementato | ✅ OK |
| **RF23** | "Sistema registra log di ogni accesso" | ❌ Non implementato | ❌ Mancante |
| **RF24** | "Messaggio errore generico per credenziali errate" | ✅ Implementato (401 "Invalid credentials") | ✅ OK |
| **RF25** | "Password non visibile di default, toggle mostra/nascondi" | ✅ Implementato in UI | ✅ OK |
| **RF26** | "Se account in attesa di verifica → invito a controllare email" | Non esiste stato verifica | ❌ **CRITICA** |

#### 2.3.3 Entità Dati Non Implementate

D3 descrive le seguenti entità che **NON esistono** nel sistema:

1. **TokenVerificaEmail** - Non implementato (nessuna verifica email)
2. **Stato Utente** (attivo/in attesa/sospeso) - Non implementato
3. **Sistema Ruoli** - Non implementato
4. **Audit Log** - Non implementato

---

### 2.4 D4 - Piano di Testing

**Status:** ❌ DISCREPANZE CRITICHE

I seguenti test case sono **non eseguibili** perché testano funzionalità inesistenti:

| Test Case | Descrizione | Problema |
|-----------|-------------|----------|
| **TC-L03** | "Login con account non attivo" | Non esiste stato account |
| **TC-R01** | "Admin crea account con tutti i dati obbligatori" | Non esiste pannello admin |
| **TC-R02** | "Admin crea account con dati mancanti" | Non esiste pannello admin |
| **TC-R06** | "Primo accesso con password temporanea" | Non esiste password temporanea |

I seguenti test case sono **eseguibili**:

| Test Case | Descrizione | Status |
|-----------|-------------|--------|
| **TC-L01** | "Login con credenziali valide" | ✅ Testabile |
| **TC-L02** | "Login con password errata" | ✅ Testabile |
| **TC-L04** | "Blocco dopo 5 tentativi" | ✅ Testabile |
| **TC-L05** | "Remember me estende sessione" | ✅ Testabile |
| **TC-R03** | "Registrazione con email esistente" | ✅ Testabile |
| **TC-R04** | "Invio email di benvenuto" | ✅ Testabile |

---

### 2.5 D5 - Documento di Progettazione

**Status:** ❌ DISCREPANZE CRITICHE

#### 2.5.1 Modello dei Dati - Registrazione

D5 Sezione 3.1 "Registrazione / Creazione Utente" descrive:

| Entità D5 | Implementazione | Discrepanza |
|-----------|-----------------|-------------|
| **RichiestaRegistrazione** | Esiste come `RegisterRequest.java` | ✅ OK |
| **TokenVerificaEmail** | ❌ NON ESISTE | ❌ **CRITICA** |
| Stato utente "in attesa di verifica" | ❌ NON ESISTE | ❌ **CRITICA** |
| Tabella **TokenVerifica** nel DB | ❌ NON ESISTE | ❌ **CRITICA** |

#### 2.5.2 Modello di Controllo - Registrazione

D5 descrive il seguente flusso (Sezione 3.2):

```
1. Compilazione form
2. Validazione client
3. POST /auth/register
4. Validazione server
5. Creazione utente in stato "in attesa di verifica" ← NON IMPLEMENTATO
6. Generazione TokenVerificaEmail ← NON IMPLEMENTATO
7. Invio mail di attivazione ← NON IMPLEMENTATO (esiste solo welcome email)
8. Utente clicca link verifica ← NON IMPLEMENTATO
9. Account diventa "attivo" ← NON IMPLEMENTATO
```

**Flusso Reale Implementato:**
```
1. Compilazione form
2. Validazione client
3. POST /api/auth/register
4. Validazione server
5. Creazione utente (immediatamente attivo)
6. Generazione JWT
7. Invio email benvenuto (opzionale, non bloccante)
8. Risposta con token + user
```

#### 2.5.3 Modello di Controllo - Login

D5 descrive:

| Controllo D5 | Implementazione | Discrepanza |
|--------------|-----------------|-------------|
| Verifica account "attivo" | Non esiste | ❌ **CRITICA** |
| Silent refresh token | Non implementato | ❌ Mancante |
| Modalità offline limitata | Non implementato | ❌ Mancante |
| Messaggio "account non verificato" | Non esiste | ❌ **CRITICA** |

#### 2.5.4 Diagrammi di Sequenza

I diagrammi UML in D5 mostrano:
- **Figura "Registrazione – Diagramma di Sequenza"**: Include `MailServer` per invio link attivazione → NON IMPLEMENTATO
- **Figura "Login – Diagramma di Sequenza"**: Include verifica stato account → NON IMPLEMENTATO

---

### 2.6 D6 - Codice Consegnato

**Status:** ✅ Documento Corretto (descrive l'implementazione reale)

D6 è l'unico documento che **riflette accuratamente** l'implementazione:

#### Conferme D6:
- ✅ JWT con scadenza 30 giorni (7d default, 30d con remember me)
- ✅ bcrypt per hash password (salt rounds: 10)
- ✅ Nessuna email verification ("scelta consapevole per semplicità MVP")
- ✅ File JSON come database
- ✅ SharedPreferences per persistenza token
- ✅ Confetti animation su registrazione

#### Limitazioni Dichiarate in D6 (Sezione 11.1):
> "Email verification: non implementata (scelta consapevole per semplicità MVP)"

Questa è l'unica ammissione esplicita della differenza rispetto ai requisiti.

---

## 3. Riepilogo Discrepanze per Categoria

### 3.1 Funzionalità NON Implementate ma Documentate

| Funzionalità | Documenti che la descrivono | Impatto |
|--------------|---------------------------|---------|
| Pannello Admin per creazione utenti | D3, D4 | CRITICO |
| Stati utente (attivo/in attesa/sospeso) | D3, D5 | CRITICO |
| Sistema ruoli (utente/moderatore/admin) | D3 | CRITICO |
| Token verifica email | D3, D5 | CRITICO |
| Email attivazione con link | D3, D5 | CRITICO |
| Password temporanea con scadenza | D3 | CRITICO |
| Cambio password obbligatorio primo accesso | D3 | CRITICO |
| Audit log operazioni | D3 | MEDIO |
| Log accessi | D3 | MEDIO |
| Silent refresh token | D5 | BASSO |
| Modalità offline limitata | D5 | BASSO |

### 3.2 Funzionalità Implementate ma NON Documentate

| Funzionalità | Dove Implementata | Documenti |
|--------------|------------------|-----------|
| Login blocking (5 tentativi/15 min) | `server.js` | Solo in D6 |
| Email benvenuto via Resend | `server.js` | Solo in D6 |
| Remember me con JWT 30d | `server.js`, `LoginRequest.java` | Solo in D6 |
| Confetti animation | `RegisterActivity.java` | Solo in D6 |

### 3.3 Funzionalità Correttamente Documentate e Implementate

| Funzionalità | Status |
|--------------|--------|
| Validazione email e password (min 6 char) | ✅ |
| Errore 409 se email già registrata | ✅ |
| Messaggio errore generico credenziali errate | ✅ |
| Toggle mostra/nascondi password | ✅ |
| Token JWT con scadenza | ✅ |
| Persistenza sessione locale | ✅ |

---

## 4. Proposte di Modifica ai Documenti

### 4.1 Modifiche a D3 (Documento dei Requisiti)

#### 4.1.1 Riscrivere Sezione "Registrazione / Creazione Utente"

**Testo Attuale (da rimuovere):**
> "L'amministratore deve poter creare un nuovo account utente..."

**Testo Proposto:**
> "Il sistema deve permettere agli utenti di registrarsi autonomamente tramite un form di registrazione accessibile dalla schermata di login. La registrazione richiede: email valida, password (minimo 6 caratteri), nome utente."

#### 4.1.2 Requisiti da RIMUOVERE

- **RF27** → Rimuovere completamente
- **RF28** → Rimuovere completamente
- **RF29** → Rimuovere completamente
- **RF30** → Rimuovere completamente
- **RF31** → Rimuovere completamente
- **RF36** → Rimuovere completamente
- **RF37** → Sostituire con "Nice to have" o rimuovere

#### 4.1.3 Requisiti da MODIFICARE

**RF19 (Login account non attivo):**
- Rimuovere il requisito oppure riformulare:
> "RF19: In una versione futura, il sistema potrà supportare stati utente (attivo/sospeso). Per l'MVP, tutti gli account sono immediatamente attivi."

**RF26 (Account in attesa verifica):**
- Rimuovere completamente

#### 4.1.4 Requisiti da AGGIUNGERE

```
RF-NEW-01: Il sistema deve permettere l'auto-registrazione degli utenti
RF-NEW-02: Al completamento della registrazione, il sistema deve generare un JWT valido
RF-NEW-03: Il sistema deve inviare una email di benvenuto (non bloccante)
RF-NEW-04: Il blocco tentativi login deve essere basato su IP+email (5 tentativi, 15 minuti)
```

---

### 4.2 Modifiche a D4 (Piano di Testing)

#### Test Case da RIMUOVERE
- **TC-L03** (Login account non attivo)
- **TC-R01** (Admin crea account)
- **TC-R02** (Admin dati mancanti)
- **TC-R06** (Password temporanea)

#### Test Case da AGGIUNGERE

```
TC-REG-01: Registrazione con dati validi
  - Input: email valida, password ≥6 char, nome
  - Expected: 201 Created, JWT restituito, email benvenuto inviata

TC-REG-02: Registrazione con email già esistente
  - Input: email già registrata
  - Expected: 409 Conflict, messaggio "Email already registered"

TC-REG-03: Registrazione con password troppo corta
  - Input: password < 6 char
  - Expected: 400 Bad Request, messaggio specifico

TC-LOGIN-BLOCK: Verifica blocco dopo 5 tentativi
  - Pre: 5 tentativi falliti
  - Input: tentativo 6
  - Expected: 429 Too Many Requests

TC-LOGIN-REMEMBER: Verifica remember me
  - Input: login con rememberMe=true
  - Expected: JWT con expiry 30d invece di 7d
```

---

### 4.3 Modifiche a D5 (Documento di Progettazione)

#### 4.3.1 Sezione 3.1 - Modello dei Dati

**Rimuovere:**
- Entità `TokenVerificaEmail`
- Riferimenti a tabella `TokenVerifica`
- Stato utente "in attesa di verifica"

**Modificare:**
> "Per la versione MVP, il sistema utilizza un modello di registrazione semplificato dove l'utente è immediatamente attivo alla creazione dell'account."

#### 4.3.2 Sezione 3.2 - Modello di Controllo

**Riscrivere il flusso di registrazione:**

```
Scenario principale (registrazione self-service):
1. Compilazione form (email, password, conferma password, nome)
2. Validazione client (formato email, password min 6 char, match conferma)
3. POST /api/auth/register
4. Server valida unicità email
5. Server crea utente con password hashata (bcrypt)
6. Server genera JWT (30d expiry)
7. Server invia email benvenuto via Resend (non bloccante)
8. Risposta con token + dati utente
9. Client salva in SharedPreferences
10. Navigazione a MainActivity
```

#### 4.3.3 Aggiornare Diagrammi UML

I diagrammi di sequenza devono essere aggiornati per riflettere:
- Nessun `MailServer` per link attivazione
- Nessuna verifica stato account
- Flusso diretto registrazione → JWT → home

---

### 4.4 Modifiche a D6 (Codice Consegnato)

**D6 è corretto** - nessuna modifica necessaria.

Suggerimento: aggiungere una nota esplicita che evidenzia le differenze rispetto ai requisiti originali:

> **Nota di Allineamento:** L'implementazione MVP ha adottato un modello di registrazione semplificato (self-service) rispetto al modello admin-managed descritto nei requisiti originali (D3). Questa scelta è stata fatta per velocizzare lo sviluppo e sarà riconciliata nei documenti D3 e D5.

---

## 5. Matrice di Tracciabilità Aggiornata

| Requisito Originale | Status | Azione Richiesta |
|---------------------|--------|------------------|
| RF19 | ❌ Non applicabile | Rimuovere |
| RF20 | ✅ Implementato | Nessuna |
| RF21 | ✅ Implementato | Nessuna |
| RF22 | ✅ Implementato | Nessuna |
| RF23 | ❌ Non implementato | Spostare a "Future" |
| RF24 | ✅ Implementato | Nessuna |
| RF25 | ✅ Implementato | Nessuna |
| RF26 | ❌ Non applicabile | Rimuovere |
| RF27 | ❌ Non applicabile | Rimuovere |
| RF28 | ❌ Non applicabile | Rimuovere |
| RF29 | ❌ Non applicabile | Rimuovere |
| RF30 | ❌ Non applicabile | Rimuovere |
| RF31 | ❌ Non applicabile | Rimuovere |
| RF32 | ✅ Implementato | Nessuna |
| RF33 | ✅ Implementato | Nessuna |
| RF34 | ✅ Implementato | Nessuna |
| RF35 | ✅ Implementato | Nessuna |
| RF36 | ❌ Non applicabile | Rimuovere |
| RF37 | ❌ Non implementato | Spostare a "Future" |
| RF38 | ✅ Implementato | Nessuna |

---

## 6. Requisiti Non Funzionali - Verifica

| RNF | Descrizione | Status |
|-----|-------------|--------|
| RNF17 | Tracciabilità operazioni | ❌ Non implementato |
| RNF-Security | bcrypt per password | ✅ Implementato |
| RNF-Performance | Timeout 30s | ✅ Implementato |

---

## 7. Conclusioni

### Riepilogo Numerico

| Categoria | Conteggio |
|-----------|-----------|
| Requisiti Login/Registrazione totali | 20 |
| Requisiti correttamente implementati | 10 (50%) |
| Requisiti non applicabili (da rimuovere) | 8 (40%) |
| Requisiti mancanti (future) | 2 (10%) |

### Priorità delle Modifiche

1. **ALTA**: Allineare D3 rimuovendo requisiti admin-managed
2. **ALTA**: Aggiornare D4 con test case corretti
3. **MEDIA**: Aggiornare D5 con flussi e diagrammi corretti
4. **BASSA**: D6 è già corretto, solo nota aggiuntiva opzionale

### Impatto Architetturale

La discrepanza principale (admin-managed vs self-registration) è una **decisione architetturale significativa** che deve essere esplicitamente documentata come variazione rispetto ai requisiti originali. Si suggerisce di aggiungere una sezione "Decisioni Architetturali e Deviazioni" in D5 o D6.

---

*Documento generato automaticamente dall'analisi della codebase EcoApp*
