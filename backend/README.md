# EcoApp Backend API

Backend REST API per EcoApp con autenticazione JWT.

## Setup Locale

```bash
cd backend
npm install
npm start
```

Il server partirà su `http://localhost:3000`

## Deploy su Render.com

1. Vai su [render.com](https://render.com) e crea account
2. Clicca "New +" → "Web Service"
3. Connetti il tuo repository GitHub
4. Configura:
   - **Name**: ecoapp-api
   - **Root Directory**: `backend`
   - **Environment**: Node
   - **Build Command**: `npm install`
   - **Start Command**: `npm start`
5. Clicca "Create Web Service"
6. Copia l'URL generato (es. `https://ecoapp-api.onrender.com`)
7. Usa questo URL come `BASE_URL` nell'app Android (file ApiClient.java)

## Endpoints API

### POST /api/auth/register
Registra nuovo utente
```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "Mario Rossi"
}
```

### POST /api/auth/login
Login utente esistente
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

### GET /api/user/profile
Ottieni profilo utente (richiede token JWT nell'header Authorization: Bearer TOKEN)

### PUT /api/user/profile
Aggiorna profilo utente (richiede token JWT)
```json
{
  "name": "Nuovo Nome",
  "totalPoints": 1500,
  "co2Saved": 20.5
}
```

## Note
- Il database è un file JSON (`users.json`)
- Le password sono hashate con bcrypt
- I token JWT scadono dopo 30 giorni
- CORS è abilitato per tutte le origini
