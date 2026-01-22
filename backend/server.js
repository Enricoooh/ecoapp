require('dotenv').config();
const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const { Resend } = require('resend');

const app = express();
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'ecoapp-super-secret-key-change-in-production';

// Resend email configuration
const RESEND_API_KEY = process.env.RESEND_API_KEY;
const resend = RESEND_API_KEY ? new Resend(RESEND_API_KEY) : null;

if (!RESEND_API_KEY) {
  console.warn('⚠️  ATTENZIONE: RESEND_API_KEY non configurata! Le email di benvenuto non verranno inviate.');
  console.warn('   Per abilitare l\'invio email, crea un file .env con RESEND_API_KEY=re_xxx');
}

// Login attempts tracking (in-memory)
const loginAttempts = {};
const MAX_ATTEMPTS = 5;
const BLOCK_DURATION_MS = 15 * 60 * 1000; // 15 minuti

// Middleware
app.use(cors());
app.use(express.json());

// Database file path
const DB_FILE = path.join(__dirname, 'users.json');
const GLOBAL_QUESTS_FILE = path.join(__dirname, 'global_quests.json');
const USER_QUESTS_FILE = path.join(__dirname, 'user_quests.json');

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

const calculateLevel = (points) => {
  if (points >= 10000) return 'Eco-Leggenda';
  if (points >= 5000) return 'Eco-Eroe';
  if (points >= 2000) return 'Eco-Guerriero';
  if (points >= 1000) return 'Eco-Apprendista';
  return 'Eco-Novizio';
};

// Logica Badge
const checkBadges = (user) => {
  const badges = user.badges || [];
  const currentPoints = user.totalPoints;
  const currentCO2 = user.co2Saved;
  const friendCount = (user.friends || []).length;

  const newBadges = [];

  // Definizione dei badge
  const badgeDefinitions = [
    { id: 1, name: 'Eco-Novizio', description: 'Benvenuto nel mondo della sostenibilità!', trigger: () => true },
    { id: 2, name: 'Pioniere Verde', description: 'Hai completato la tua prima missione!', trigger: () => user.totalPoints > 0 },
    { id: 3, name: 'Amico della Terra', description: 'Hai salvato i tuoi primi 10kg di CO2.', trigger: () => currentCO2 >= 10 },
    { id: 4, name: 'Influencer Ambientale', description: 'Hai aggiunto i tuoi primi 5 amici.', trigger: () => friendCount >= 5 },
    { id: 5, name: 'Eco-Guerriero', description: 'Hai raggiunto 2000 punti!', trigger: () => currentPoints >= 2000 },
    { id: 6, name: 'Salvatore del Pianeta', description: 'Hai salvato 100kg di CO2!', trigger: () => currentCO2 >= 100 }
  ];

  badgeDefinitions.forEach(def => {
    if (!badges.find(b => b.id === def.id) && def.trigger()) {
      newBadges.push({
        id: def.id,
        name: def.name,
        description: def.description,
        unlockedAt: new Date().toISOString()
      });
    }
  });

  return [...badges, ...newBadges];
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

// Helper: Check if account is blocked
const isAccountBlocked = (email) => {
  const attempt = loginAttempts[email];
  if (!attempt) return { blocked: false };

  if (attempt.blockedUntil && Date.now() < attempt.blockedUntil) {
    const remainingMs = attempt.blockedUntil - Date.now();
    const remainingMin = Math.ceil(remainingMs / 60000);
    return { blocked: true, remainingMin };
  }

  // Block expired, reset
  if (attempt.blockedUntil && Date.now() >= attempt.blockedUntil) {
    delete loginAttempts[email];
  }
  return { blocked: false };
};

// Helper: Record failed attempt
const recordFailedAttempt = (email) => {
  if (!loginAttempts[email]) {
    loginAttempts[email] = { count: 0, lastAttempt: null, blockedUntil: null };
  }

  loginAttempts[email].count++;
  loginAttempts[email].lastAttempt = Date.now();

  if (loginAttempts[email].count >= MAX_ATTEMPTS) {
    loginAttempts[email].blockedUntil = Date.now() + BLOCK_DURATION_MS;
  }
};

// Helper: Reset attempts on successful login
const resetAttempts = (email) => {
  delete loginAttempts[email];
};

// Helper: Send welcome email
const sendWelcomeEmail = async (email, name) => {
  // Verifica che Resend sia configurato
  if (!resend) {
    console.log('📧 Email di benvenuto non inviata (RESEND_API_KEY non configurata) - utente:', email);
    return;
  }

  try {
    const response = await resend.emails.send({
      from: 'EcoApp <noreply@ecoapp.unica.dev>',
      to: email,
      subject: '🌱 Benvenuto in EcoApp!',
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
          <h1 style="color: #58CC02;">🌱 Benvenuto in EcoApp, ${name}!</h1>
          <p>La tua registrazione è stata completata con successo.</p>
          <p>Sei pronto a iniziare la tua avventura eco-sostenibile! 🌍</p>
          <p>Completa le quest giornaliere per guadagnare punti e ridurre la tua impronta ambientale.</p>
          <br>
          <p style="color: #666;">Il Team EcoApp 🎮</p>
        </div>
      `
    });
    console.log('✅ Welcome email sent to:', email, '- Response:', JSON.stringify(response));
  } catch (error) {
    console.error('❌ Failed to send welcome email:', error.message);
    console.error('   Full error:', JSON.stringify(error, null, 2));
    // Non blocchiamo la registrazione se l'email fallisce
  }
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
      pendingRequests: [], // richieste pendenti ricevute
      badges: [],
      createdAt: new Date().toISOString()
    };

    // Sblocca il primo badge alla registrazione
    newUser.badges = checkBadges(newUser);

    db.users.push(newUser);
    writeDB(db);

    // --- SINCRONIZZAZIONE CON USER_QUESTS.JSON ---
    // 1. Leggi il file user_quests.json (se non esiste, crea un oggetto vuoto)
    let allProgress = {};
    if (fs.existsSync(USER_QUESTS_FILE)) {
        allProgress = JSON.parse(fs.readFileSync(USER_QUESTS_FILE, 'utf8'));
    }

    // 2. Inizializza l'ID del nuovo utente con un array vuoto di quest
    allProgress[newUser.id] = [];

    // 3. Salva il file aggiornato
    fs.writeFileSync(USER_QUESTS_FILE, JSON.stringify(allProgress, null, 2));

    // Send welcome email (non-blocking)
    sendWelcomeEmail(newUser.email, newUser.name);

    const token = jwt.sign({ id: newUser.id, email: newUser.email }, JWT_SECRET, { expiresIn: '7d' });

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
    const { email, password, rememberMe } = req.body;

    // Check if account is blocked
    const blockStatus = isAccountBlocked(email);
    if (blockStatus.blocked) {
      return res.status(429).json({
        error: `Troppi tentativi falliti. Riprova tra ${blockStatus.remainingMin} minuti.`,
        blockedMinutes: blockStatus.remainingMin
      });
    }

    const db = readDB();
    const user = db.users.find(u => u.email === email);

    if (!user || !(await bcrypt.compare(password, user.password))) {
      recordFailedAttempt(email);
      return res.status(401).json({ error: 'Credenziali non valide' });
    }

    // Reset attempts on successful login
    resetAttempts(email);

    // Token expiry based on rememberMe
    const tokenExpiry = rememberMe ? '30d' : '7d';
    const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: tokenExpiry });

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
    console.error('Login error:', error);
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

  // Controlla badge dopo aggiunta amico
  db.users[userIndex].badges = checkBadges(db.users[userIndex]);

  writeDB(db);

  res.json({ message: 'Amico aggiunto con successo', friend: friendToAdd.name });
});

// Remove friend (Reciprocal removal)
app.post('/api/user/friends/remove', authenticateToken, (req, res) => {
    const { friendId } = req.body;
    const db = readDB();
    const user = db.users.find(u => u.id === req.user.id);
    const friend = db.users.find(u => u.id === friendId);

    if (!user) return res.status(404).json({ error: 'Utente non trovato' });

    // Rimuovi reciprocamente
    if (user.friends) {
        user.friends = user.friends.filter(id => id !== friendId);
    }
    if (friend && friend.friends) {
        friend.friends = friend.friends.filter(id => id !== req.user.id);
    }

    writeDB(db);
    res.json({ message: 'Amico rimosso con successo' });
});

// --- NEW FRIEND REQUEST ENDPOINTS ---

// Invia richiesta di amicizia
app.post('/api/user/friends/request', authenticateToken, (req, res) => {
    const { query } = req.body;
    const db = readDB();
    const sender = db.users.find(u => u.id === req.user.id);
    const receiver = db.users.find(u => u.email === query || u.nickname === query);

    if (!receiver) return res.status(404).json({ error: 'Utente non trovato' });
    if (receiver.id === sender.id) return res.status(400).json({ error: 'Non puoi aggiungere te stesso' });
    if ((sender.friends || []).includes(receiver.id)) return res.status(400).json({ error: 'Già amici' });

    if (!receiver.pendingRequests) receiver.pendingRequests = [];
    if (receiver.pendingRequests.includes(sender.id)) return res.status(400).json({ error: 'Richiesta già inviata' });

    receiver.pendingRequests.push(sender.id);
    writeDB(db);
    res.json({ message: 'Richiesta inviata' });
});

// Ottieni richieste di amicizia pendenti (ricevute)
app.get('/api/user/friends/requests', authenticateToken, (req, res) => {
    const db = readDB();
    const user = db.users.find(u => u.id === req.user.id);
    const requests = db.users.filter(u => (user.pendingRequests || []).includes(u.id)).map(u => ({
        id: u.id,
        name: u.name,
        nickname: u.nickname,
        urlImmagineProfilo: u.urlImmagineProfilo
    }));
    res.json(requests);
});

// Rispondi a una richiesta di amicizia (accetta o rifiuta)
app.post('/api/user/friends/respond', authenticateToken, (req, res) => {
    const { senderId, action } = req.body; // 'accept' o 'decline'
    const db = readDB();
    const receiver = db.users.find(u => u.id === req.user.id);
    const sender = db.users.find(u => u.id === senderId);

    if (!receiver || !sender) return res.status(404).json({ error: 'Utente non trovato' });

    receiver.pendingRequests = (receiver.pendingRequests || []).filter(id => id !== senderId);

    if (action === 'accept') {
        if (!receiver.friends) receiver.friends = [];
        if (!sender.friends) sender.friends = [];
        if (!receiver.friends.includes(senderId)) receiver.friends.push(senderId);
        if (!sender.friends.includes(receiver.id)) sender.friends.push(receiver.id);

        // Controlla badge dopo accettazione
        receiver.badges = checkBadges(receiver);
        sender.badges = checkBadges(sender);
    }
    writeDB(db);
    res.json({ message: action === 'accept' ? 'Accettata' : 'Rifiutata' });
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

// 2. Ottieni lo stato delle quest dell'utente dal file SEPARATO
app.get('/api/user/quests', authenticateToken, (req, res) => {
  try {
    const userId = req.user.id.toString();
    const data = JSON.parse(fs.readFileSync(USER_QUESTS_FILE, 'utf8'));

    const userQuests = data[userId] || [];
    res.json(userQuests);
  } catch (error) {
    res.status(500).json([]);
  }
});

// 3. Aggiorna il progresso di una quest salvandolo nel file SEPARATO user_quests.json
app.post('/api/user/quests/update', authenticateToken, (req, res) => {
    const { questId, progressIncrement } = req.body;
    const userId = req.user.id.toString();

    const questsData = JSON.parse(fs.readFileSync(GLOBAL_QUESTS_FILE, 'utf8'));
    const globalQuest = questsData.find(q => q.id === Number(questId));
    if (!globalQuest) return res.status(404).json({ error: 'Quest globale non trovata' });

    let allProgress = {};
    if (fs.existsSync(USER_QUESTS_FILE)) {
        allProgress = JSON.parse(fs.readFileSync(USER_QUESTS_FILE, 'utf8'));
    }

    if (!allProgress[userId]) {
        allProgress[userId] = [];
    }

    let userQuest = allProgress[userId].find(q => q.questId === Number(questId));

    if (!userQuest) {
        userQuest = {
            questId: Number(questId),
            actual_progress: Number(progressIncrement),
            times_completed: Number(0),
            is_currently_active: true
        };
        allProgress[userId].push(userQuest);
    } else {
        userQuest.actual_progress += Number(progressIncrement);

        if (userQuest.actual_progress >= Number(globalQuest.max_progress)) {
            userQuest.times_completed += 1;
            userQuest.actual_progress = 0;

            const usersDb = JSON.parse(fs.readFileSync(DB_FILE, 'utf8'));
            const uIdx = usersDb.users.findIndex(u => u.id === userId);
            if (uIdx !== -1) {
                usersDb.users[uIdx].totalPoints += Number(globalQuest.reward_points);
                usersDb.users[uIdx].co2Saved += Number(globalQuest.CO2_saved);

                // Aggiorna il livello in base ai nuovi punti
                usersDb.users[uIdx].level = calculateLevel(usersDb.users[uIdx].totalPoints);

                // Controlla badge dopo completamento missione
                usersDb.users[uIdx].badges = checkBadges(usersDb.users[uIdx]);

                fs.writeFileSync(DB_FILE, JSON.stringify(usersDb, null, 2));
            }
        }
    }

    fs.writeFileSync(USER_QUESTS_FILE, JSON.stringify(allProgress, null, 2));

    res.json({
        message: 'Progresso aggiornato nel file separato',
        userQuest
    });

});

// --- AREA DEBUG ---

const FILES_MAP = {
    'users': DB_FILE,
    'global-quests': GLOBAL_QUESTS_FILE,
    'user-quests': USER_QUESTS_FILE
};

app.get('/api/admin/dump/:filename', (req, res) => {
    const targetFile = FILES_MAP[req.params.filename];

    if (!targetFile) {
        return res.status(404).json({ error: "File non riconosciuto" });
    }

    try {
        if (!fs.existsSync(targetFile)) {
            return res.status(404).json({ error: "Il file non esiste" });
        }

        const data = fs.readFileSync(targetFile, 'utf8');
        res.header("Content-Type", "application/json");
        res.send(data);
    } catch (err) {
        res.status(500).json({ error: "Errore durante la lettura del file" });
    }
});

// Avvia il server
app.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 EcoApp Server [Beta] running on port ${PORT}`);
});
