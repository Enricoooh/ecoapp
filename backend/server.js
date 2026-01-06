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

// Initialize database file if not exists
/*
if (!fs.existsSync(DB_FILE)) {
  fs.writeFileSync(DB_FILE, JSON.stringify({ users: [] }, null, 2));
}
*/

// Initialize database file with users AND quests if not exists
if (!fs.existsSync(DB_FILE)) {
  const initialData = {
    users: [],
    quests: [
        {
            id: "0",
            name: "Usa la borraccia",
            type: "Alimentation",
            actual_progress: 0,
            max_progress: 5,
            imageID: 2131165271,
            description: "Usa la borraccia per 5 giorni invece delle bottiglie di plastica.",
            imagesEU: [2131165271, 2131165272],
            reward_points: 50
       },
       {
           id: "1",
           name: "Mobilità Verde",
           type: "Mobility",
           actual_progress: 0,
           max_progress: 10,
           imageID: 2131165273,
           description: "Percorri 10km a piedi o in bici per ridurre le emissioni.",
           imagesEU: [],
           reward_points: 100
       },
       {
           id: "2",
           name: "Doccia Breve",
           type: "Home",
           actual_progress: 0,
           max_progress: 3,
           imageID: 2131165274,
           description: "Riduci il tempo della doccia a un massimo di 5 minuti per 3 volte.",
           imagesEU: [],
           reward_points: 30
       },
       {
           id: "3",
           name: "Cena a km 0",
           type: "Alimentation",
           actual_progress: 0,
           max_progress: 1,
           imageID: 2131165275,
           description: "Prepara una cena usando solo prodotti locali o del tuo orto.",
           imagesEU: [],
           reward_points: 70
       },
       {
           id: "4",
           name: "Eroe del Riciclo",
           type: "Waste",
           actual_progress: 0,
           max_progress: 20,
           imageID: 2131165276,
           description: "Differenzia correttamente 20 oggetti tra plastica, carta e vetro.",
           imagesEU: [],
           reward_points: 150
       }
    ]
  };
  fs.writeFileSync(DB_FILE, JSON.stringify(initialData, null, 2));
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

// 1. Ottieni tutte le quest disponibili
app.get('/api/quests', authenticateToken, (req, res) => {
  const db = readDB();
  res.json(db.quests);
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

// 3. Aggiorna il progresso di una quest per l'utente
app.post('/api/user/quests/update', authenticateToken, (req, res) => {
  const { questId, progressIncrement } = req.body;
  const db = readDB();
  const userIndex = db.users.findIndex(u => u.id === req.user.id);

  if (userIndex === -1) return res.status(404).json({ error: 'Utente non trovato' });

  const quest = db.quests.find(q => q.id === questId);
  if (!quest) return res.status(404).json({ error: 'Quest non trovata' });

  // Inizializza la lista quest dell'utente se vuota
  if (!db.users[userIndex].userQuests) db.users[userIndex].userQuests = [];

  let userQuest = db.users[userIndex].userQuests.find(q => q.questId === questId);

  if (!userQuest) {// Prima volta che l'utente affronta questa quest
    userQuest = {
      questId: questId,
      actual_progress: progressIncrement,
      max_progress: quest.max_progress,
      completed: false
    };
    db.users[userIndex].userQuests.push(userQuest);
  } else {
    // Incrementa progresso esistente
    if (!userQuest.completed) {
      userQuest.actual_progress += progressIncrement;
      if (userQuest.actual_progress >= userQuest.max_progress) {
        userQuest.actual_progress = userQuest.max_progress;
        userQuest.completed = true;

        // Assegna punti all'utente
        db.users[userIndex].totalPoints += quest.reward_points;
        // Esempio CO2 risparmiata (1kg per quest completata)
        db.users[userIndex].co2Saved += 1.0;
      }
    }
  }

  writeDB(db);
  res.json({
    message: 'Progresso aggiornato',
    userQuest,
    totalPoints: db.users[userIndex].totalPoints
  });
});


app.listen(PORT, () => console.log(`🚀 EcoApp Server [Beta] running on port ${PORT}`));
