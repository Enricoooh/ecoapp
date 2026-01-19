const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const cors = require('cors');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'ecoapp-super-secret-key-change-in-production';

// Middleware
app.use(cors());
app.use(express.json());

// Database file path
const DB_FILE = path.join(__dirname, 'users.json');
const GLOBAL_QUESTS_FILE = path.join(__dirname, 'global_quests.json');
const USER_QUESTS_FILE = path.join(__dirname, 'user_quest.json');

// Initialize database file if not exists
if (!fs.existsSync(DB_FILE)) {
  fs.writeFileSync(DB_FILE, JSON.stringify({ users: [] }, null, 2));
}

// Helper functions
const readDB = () => {
  const data = fs.readFileSync(DB_FILE, 'utf8');
  return JSON.parse(data);
};

const writeDB = (data) => {
  fs.writeFileSync(DB_FILE, JSON.stringify(data, null, 2));
};

// Middleware to verify JWT token
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ error: 'Token mancante' });
  }

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({ error: 'Token non valido' });
    }
    req.user = user;
    next();
  });
};

// Routes

// Health check
app.get('/', (req, res) => {
  res.json({ message: 'EcoApp Backend API (BETA)', status: 'running' });
});

// Register endpoint
app.post('/api/auth/register', async (req, res) => {
  try {
    const { email, password, name, nickname } = req.body;

    if (!email || !password || !name) {
      return res.status(400).json({ error: 'Email, password e nome sono richiesti' });
    }

    const db = readDB();
    if (db.users.find(u => u.email === email)) {
      return res.status(409).json({ error: 'Email già registrata' });
    }

    const hashedPassword = await bcrypt.hash(password, 10);

    const newUser = {
      id: Date.now().toString(),
      email,
      password: hashedPassword,
      name,
      nickname: nickname || name.split(' ')[0] + Math.floor(Math.random() * 1000),
      bio: '',
      urlImmagineProfilo: '',
      level: 'Eco-Novizio',
      totalPoints: 0,
      co2Saved: 0.0,
      friends: [], // Lista ID amici
      createdAt: new Date().toISOString()
    };

    db.users.push(newUser);
    writeDB(db);

    // --- SINCRONIZZAZIONE CON USER_QUEST.JSON ---
    // 1. Leggi il file user_quest.json (se non esiste, crea un oggetto vuoto)
    let allProgress = {};
    if (fs.existsSync(USER_QUESTS_FILE)) {
        allProgress = JSON.parse(fs.readFileSync(USER_QUESTS_FILE, 'utf8'));
    }

    // 2. Inizializza l'ID del nuovo utente con un array vuoto di quest
    allProgress[newUser.id] = []; 

    // 3. Salva il file aggiornato
    fs.writeFileSync(USER_QUESTS_FILE, JSON.stringify(allProgress, null, 2));



    const token = jwt.sign({ id: newUser.id, email: newUser.email }, JWT_SECRET, { expiresIn: '30d' });

    res.status(201).json({
      message: 'Registrazione completata',
      token,
      user: {
        id: newUser.id,
        email: newUser.email,
        name: newUser.name,
        nickname: newUser.nickname,
        level: newUser.level,
        totalPoints: newUser.totalPoints,
        co2Saved: newUser.co2Saved
      }
    });
  } catch (error) {
    res.status(500).json({ error: 'Errore del server' });
  }
});

// Login endpoint
app.post('/api/auth/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    const db = readDB();
    const user = db.users.find(u => u.email === email);

    if (!user || !(await bcrypt.compare(password, user.password))) {
      return res.status(401).json({ error: 'Credenziali non valide' });
    }

    const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: '30d' });

    res.json({
      token,
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        nickname: user.nickname,
        level: user.level,
        totalPoints: user.totalPoints,
        co2Saved: user.co2Saved
      }
    });
  } catch (error) {
    res.status(500).json({ error: 'Errore del server' });
  }
});

// Get user profile
app.get('/api/user/profile', authenticateToken, (req, res) => {
  const db = readDB();
  const user = db.users.find(u => u.id === req.user.id);
  if (!user) return res.status(404).json({ error: 'Utente non trovato' });

  res.json({
    id: user.id,
    email: user.email,
    name: user.name,
    nickname: user.nickname,
    bio: user.bio,
    urlImmagineProfilo: user.urlImmagineProfilo,
    level: user.level,
    totalPoints: user.totalPoints,
    co2Saved: user.co2Saved,
    followerCount: 0, // Da implementare con logica reale
    followingCount: user.friends.length
  });
});

// Update user profile
app.put('/api/user/profile', authenticateToken, async (req, res) => {
  try {
    const { name, email, nickname, bio, urlImmagineProfilo, password } = req.body;
    const db = readDB();
    const userIndex = db.users.findIndex(u => u.id === req.user.id);

    if (userIndex === -1) return res.status(404).json({ error: 'Utente non trovato' });

    if (name) db.users[userIndex].name = name;
    if (nickname) db.users[userIndex].nickname = nickname;
    if (bio !== undefined) db.users[userIndex].bio = bio;
    if (urlImmagineProfilo !== undefined) db.users[userIndex].urlImmagineProfilo = urlImmagineProfilo;

    if (email && email !== db.users[userIndex].email) {
      if (db.users.find(u => u.email === email)) return res.status(409).json({ error: 'Email già in uso' });
      db.users[userIndex].email = email;
    }

    if (password) {
      db.users[userIndex].password = await bcrypt.hash(password, 10);
    }

    writeDB(db);
    res.json({ message: 'Profilo aggiornato', user: db.users[userIndex] });
  } catch (error) {
    res.status(500).json({ error: 'Errore del server' });
  }
});

// --- FRIENDS ENDPOINTS ---

// Get friends list
app.get('/api/user/friends', authenticateToken, (req, res) => {
  const db = readDB();
  const user = db.users.find(u => u.id === req.user.id);

  if (!user) return res.status(404).json({ error: 'Utente non trovato' });

  // Recupera i profili completi degli amici partendo dai loro ID
  const friendsProfiles = db.users
    .filter(u => user.friends.includes(u.id))
    .map(u => ({
      id: u.id,
      name: u.name,
      nickname: u.nickname,
      urlImmagineProfilo: u.urlImmagineProfilo,
      level: u.level
    }));

  res.json(friendsProfiles);
});

// Add friend by nickname or email
app.post('/api/user/friends/add', authenticateToken, (req, res) => {
  const { query } = req.body; // Può essere email o nickname
  const db = readDB();
  const userIndex = db.users.findIndex(u => u.id === req.user.id);

  if (userIndex === -1) return res.status(404).json({ error: 'Utente non trovato' });

  // Trova l'utente da aggiungere
  const friendToAdd = db.users.find(u => u.email === query || u.nickname === query);

  if (!friendToAdd) return res.status(404).json({ error: 'Utente non trovato' });
  if (friendToAdd.id === req.user.id) return res.status(400).json({ error: 'Non puoi aggiungere te stesso' });

  if (db.users[userIndex].friends.includes(friendToAdd.id)) {
    return res.status(400).json({ error: 'Utente già presente negli amici' });
  }

  // Aggiungi l'ID alla lista amici
  db.users[userIndex].friends.push(friendToAdd.id);
  writeDB(db);

  res.json({ message: 'Amico aggiunto con successo', friend: friendToAdd.name });
});



// --- QUESTS ENDPOINTS ---

// 1. Ottieni tutte le quest dal file separato
app.get('/api/quests', authenticateToken, (req, res) => {
  try {
    // Legge il file quests.json
    const data = fs.readFileSync(GLOBAL_QUESTS_FILE, 'utf8');
    const quests = JSON.parse(data);
    
    // Invia la lista delle quest all'app Android
    res.json(quests);
  }
  catch (error) {
    console.error("Errore lettura quests.json:", error);
    res.status(500).json({ error: 'Errore nel caricamento delle missioni' });
  }
});

// 2. Ottieni lo stato delle quest dell'utente (completate/in corso)
app.get('/api/user/quests', authenticateToken, (req, res) => {
  const db = readDB();
  const user = db.users.find(u => u.id === req.user.id);

  if (!user) return res.status(404).json({ error: 'Utente non trovato' });

  // Se l'utente non ha ancora il campo quests, inizializzalo
  const userQuests = user.userQuests || [];
  res.json(userQuests);
});

// 2. Ottieni lo stato delle quest dell'utente dal file SEPARATO
app.get('/api/user/quests', authenticateToken, (req, res) => {
  try {
    const userId = req.user.id;
    let allProgress = {};
    
    if (fs.existsSync(USER_QUESTS_FILE)) {
        allProgress = JSON.parse(fs.readFileSync(USER_QUESTS_FILE, 'utf8'));
    }

    // Restituisce le quest di questo specifico utente o un array vuoto
    const userQuests = allProgress[userId] || [];
    res.json(userQuests);
  } catch (error) {
    res.status(500).json({ error: 'Errore nel recupero progressi' });
  }
});

// 3. Aggiorna il progresso di una quest salvandolo nel file SEPARATO user_quest.json
app.post('/api/user/quests/update', authenticateToken, (req, res) => {
    const { questId, progressIncrement } = req.body;
    const userId = req.user.id; // Preso dal token JWT dell'utente loggato

    // 1. Leggi il file delle Quest Globali (per sapere il max_progress e i punti)
    const questsData = JSON.parse(fs.readFileSync(GLOBAL_QUESTS_FILE, 'utf8'));
    const globalQuest = questsData.find(q => q.id == questId);
    if (!globalQuest) return res.status(404).json({ error: 'Quest globale non trovata' });

    // 2. Leggi (o crea se non esiste) il file user_quest.json
    let allProgress = {};
    if (fs.existsSync(USER_QUESTS_FILE)) {
        allProgress = JSON.parse(fs.readFileSync(USER_QUESTS_FILE, 'utf8'));
    }

    // 3. Inizializza la lista progressi per questo utente se è la prima volta
    if (!allProgress[userId]) {
        allProgress[userId] = [];
    }

    // 4. Cerca se l'utente ha già iniziato QUESTA specifica quest
    let userQuest = allProgress[userId].find(q => q.questId == questId);

    if (!userQuest) {
        // Prima volta che l'utente fa questa missione
        userQuest = {
            questId: parseInt(questId),
            actual_progress: progressIncrement,
            times_completed: 0,
            is_currently_active: true
        };
        allProgress[userId].push(userQuest);
    } else {
        // Incrementa il progresso esistente
        userQuest.actual_progress += progressIncrement;

        // Logica di completamento
        if (userQuest.actual_progress >= globalQuest.max_progress) {
            userQuest.times_completed += 1;
            userQuest.actual_progress = 0; // Resetta per permettere di rifarla

            // AGGIORNAMENTO PUNTI (Qui devi comunque aggiornare users.json)
            const usersDb = JSON.parse(fs.readFileSync(DB_FILE, 'utf8'));
            const userIdx = usersDb.users.findIndex(u => u.id === userId);
            if (userIdx !== -1) {
                usersDb.users[userIdx].totalPoints += globalQuest.reward_points;
                usersDb.users[userIdx].co2Saved += globalQuest.CO2_saved;
                fs.writeFileSync(DB_FILE, JSON.stringify(usersDb, null, 2));
            }
        }
    }

    // 5. Salva i progressi nel file separato
    fs.writeFileSync(USER_QUESTS_FILE, JSON.stringify(allProgress, null, 2));

    res.json({
        message: 'Progresso aggiornato nel file separato',
        userQuest
    });

});

// --- AREA DEBUG (Da commentare o cancellare prima della consegna) ---

const FILES_MAP = {
    'users': DB_FILE,
    'global-quests': GLOBAL_QUESTS_FILE,
    'user-quests': USER_QUESTS_FILE
};

app.get('/api/admin/dump/:filename', (req, res) => {
    const targetFile = FILES_MAP[req.params.filename];

    if (!targetFile) {
        return res.status(404).json({ error: "File non riconosciuto. Usa: users, global-quests, o user-quests" });
    }

    try {
        if (!fs.existsSync(targetFile)) {
            return res.status(404).json({ error: `Il file ${req.params.filename} non esiste ancora sul server.` });
        }

        const data = fs.readFileSync(targetFile, 'utf8');
        res.header("Content-Type", "application/json");
        res.send(data); // Invia il contenuto del file JSON
    } catch (err) {
        res.status(500).json({ error: "Errore durante la lettura del file", details: err.message });
    }
});

// Avvia il server
app.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 EcoApp Server [Beta] running on port ${PORT}`);
});

