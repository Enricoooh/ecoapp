const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '.env') });
const express = require('express');
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const cors = require('cors');
const { Resend } = require('resend');

// Models
const User = require('./models/User');
const GlobalQuest = require('./models/GlobalQuest');
const UserQuest = require('./models/UserQuest');

const app = express();
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'ecoapp-super-secret-key-change-in-production';
const MONGODB_URI = process.env.MONGODB_URI;

// Resend email configuration
const RESEND_API_KEY = process.env.RESEND_API_KEY;
const resend = RESEND_API_KEY ? new Resend(RESEND_API_KEY) : null;

if (!RESEND_API_KEY) {
  console.warn('⚠️  RESEND_API_KEY non configurata - email disabilitate');
}

// MongoDB Connection
mongoose.connect(MONGODB_URI)
  .then(() => console.log('✅ MongoDB connected'))
  .catch(err => {
    console.error('❌ MongoDB connection error:', err);
    process.exit(1);
  });

// Login attempts tracking (in-memory)
const loginAttempts = {};
const MAX_ATTEMPTS = 5;
const BLOCK_DURATION_MS = 15 * 60 * 1000; // 15 minuti

// Middleware
app.use(cors());
app.use(express.json());

// ============== HELPER FUNCTIONS ==============

const calculateLevel = (points) => {
  if (points >= 10000) return 'Eco-Leggenda';
  if (points >= 5000) return 'Eco-Eroe';
  if (points >= 2000) return 'Eco-Guerriero';
  if (points >= 1000) return 'Eco-Apprendista';
  return 'Eco-Novizio';
};

const checkBadges = (user) => {
  const badges = user.badges || [];
  const currentPoints = user.totalPoints;
  const currentCO2 = user.co2Saved;
  const friendCount = (user.friends || []).length;

  const badgeDefinitions = [
    { id: 1, name: 'Eco-Novizio', description: 'Benvenuto nel mondo della sostenibilità!', trigger: () => true },
    { id: 2, name: 'Pioniere Verde', description: 'Hai completato la tua prima missione!', trigger: () => currentPoints > 0 },
    { id: 3, name: 'Amico della Terra', description: 'Hai salvato i tuoi primi 10kg di CO2.', trigger: () => currentCO2 >= 10 },
    { id: 4, name: 'Influencer Ambientale', description: 'Hai aggiunto i tuoi primi 5 amici.', trigger: () => friendCount >= 5 },
    { id: 5, name: 'Eco-Guerriero', description: 'Hai raggiunto 2000 punti!', trigger: () => currentPoints >= 2000 },
    { id: 6, name: 'Salvatore del Pianeta', description: 'Hai salvato 100kg di CO2!', trigger: () => currentCO2 >= 100 }
  ];

  const newBadges = [];
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

const isAccountBlocked = (email) => {
  const attempt = loginAttempts[email];
  if (!attempt) return { blocked: false };
  if (attempt.blockedUntil && Date.now() < attempt.blockedUntil) {
    const remainingMs = attempt.blockedUntil - Date.now();
    const remainingMin = Math.ceil(remainingMs / 60000);
    return { blocked: true, remainingMin };
  }
  if (attempt.blockedUntil && Date.now() >= attempt.blockedUntil) {
    delete loginAttempts[email];
  }
  return { blocked: false };
};

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

const resetAttempts = (email) => {
  delete loginAttempts[email];
};

const sendWelcomeEmail = async (email, name) => {
  if (!resend) {
    console.log('📧 Email non inviata (Resend non configurato) - utente:', email);
    return;
  }
  try {
    await resend.emails.send({
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
    console.log('✅ Welcome email sent to:', email);
  } catch (error) {
    console.error('❌ Email error:', error.message);
  }
};

// JWT Middleware
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];
  if (!token) return res.status(401).json({ error: 'Token mancante' });

  jwt.verify(token, JWT_SECRET, (err, decoded) => {
    if (err) return res.status(403).json({ error: 'Token non valido' });
    req.user = decoded;
    next();
  });
};

// ============== ROUTES ==============

// Health check
app.get('/', (req, res) => {
  res.json({ message: 'EcoApp Backend API', status: 'running', db: 'MongoDB' });
});

// ============== AUTH ==============

app.post('/api/auth/register', async (req, res) => {
  try {
    const { email, password, name, nickname } = req.body;

    if (!email || !password || !name) {
      return res.status(400).json({ error: 'Email, password e nome sono richiesti' });
    }

    const existingUser = await User.findOne({ email: email.toLowerCase() });
    if (existingUser) {
      return res.status(409).json({ error: 'Email già registrata' });
    }

    const hashedPassword = await bcrypt.hash(password, 10);
    
    const user = new User({
      email: email.toLowerCase(),
      password: hashedPassword,
      name,
      nickname: nickname || name.split(' ')[0] + Math.floor(Math.random() * 1000),
      badges: [{ id: 1, name: 'Eco-Novizio', description: 'Benvenuto nel mondo della sostenibilità!', unlockedAt: new Date().toISOString() }]
    });

    await user.save();

    // Inizializza quest per l'utente
    const globalQuests = await GlobalQuest.find();
    if (globalQuests.length > 0) {
      const userQuests = globalQuests.map(q => ({
        userId: user._id,
        questId: q.id,
        actual_progress: 0,
        times_completed: 0,
        isActive: false
      }));
      await UserQuest.insertMany(userQuests);
    }

    const token = jwt.sign({ id: user._id.toString(), email: user.email }, JWT_SECRET, { expiresIn: '7d' });

    sendWelcomeEmail(user.email, user.name);

    res.status(201).json({
      message: 'Registrazione completata',
      token,
      user: {
        id: user._id,
        email: user.email,
        name: user.name,
        nickname: user.nickname,
        level: user.level,
        totalPoints: user.totalPoints,
        co2Saved: user.co2Saved,
        badges: user.badges
      }
    });
  } catch (error) {
    console.error('Register error:', error);
    res.status(500).json({ error: 'Errore durante la registrazione' });
  }
});

app.post('/api/auth/login', async (req, res) => {
  try {
    const { email, password, rememberMe } = req.body;

    const blockStatus = isAccountBlocked(email);
    if (blockStatus.blocked) {
      return res.status(429).json({ 
        error: `Troppi tentativi falliti. Riprova tra ${blockStatus.remainingMin} minuti.`,
        blockedMinutes: blockStatus.remainingMin
      });
    }

    const user = await User.findOne({ email: email.toLowerCase() });
    if (!user) {
      recordFailedAttempt(email);
      return res.status(401).json({ error: 'Credenziali non valide' });
    }

    const validPassword = await bcrypt.compare(password, user.password);
    if (!validPassword) {
      recordFailedAttempt(email);
      return res.status(401).json({ error: 'Credenziali non valide' });
    }

    resetAttempts(email);

    const tokenExpiry = rememberMe ? '30d' : '7d';
    const token = jwt.sign(
      { id: user._id.toString(), email: user.email },
      JWT_SECRET,
      { expiresIn: tokenExpiry }
    );

    res.json({
      token,
      user: {
        id: user._id,
        email: user.email,
        name: user.name,
        nickname: user.nickname,
        bio: user.bio,
        urlImmagineProfilo: user.urlImmagineProfilo,
        level: user.level,
        totalPoints: user.totalPoints,
        co2Saved: user.co2Saved,
        badges: user.badges || []
      }
    });
  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({ error: 'Errore durante il login' });
  }
});

// ============== USER PROFILE ==============

app.get('/api/user/profile', authenticateToken, async (req, res) => {
  try {
    const user = await User.findById(req.user.id);
    if (!user) return res.status(404).json({ error: 'Utente non trovato' });

    res.json({
      id: user._id,
      email: user.email,
      name: user.name,
      nickname: user.nickname,
      bio: user.bio,
      urlImmagineProfilo: user.urlImmagineProfilo,
      level: user.level,
      totalPoints: user.totalPoints,
      co2Saved: user.co2Saved,
      badges: user.badges || [],
      followerCount: 0,
      followingCount: (user.friends || []).length
    });
  } catch (error) {
    console.error('Profile error:', error);
    res.status(500).json({ error: 'Errore nel recupero profilo' });
  }
});

app.put('/api/user/profile', authenticateToken, async (req, res) => {
  try {
    const { name, email, nickname, bio, urlImmagineProfilo, password } = req.body;
    
    const updateData = {};
    if (name) updateData.name = name;
    if (nickname) updateData.nickname = nickname;
    if (bio !== undefined) updateData.bio = bio;
    if (urlImmagineProfilo !== undefined) updateData.urlImmagineProfilo = urlImmagineProfilo;
    
    if (email) {
      const existingUser = await User.findOne({ email: email.toLowerCase(), _id: { $ne: req.user.id } });
      if (existingUser) return res.status(409).json({ error: 'Email già in uso' });
      updateData.email = email.toLowerCase();
    }
    
    if (password) {
      updateData.password = await bcrypt.hash(password, 10);
    }

    const user = await User.findByIdAndUpdate(req.user.id, { $set: updateData }, { new: true });
    if (!user) return res.status(404).json({ error: 'Utente non trovato' });

    res.json({
      message: 'Profilo aggiornato',
      user: {
        id: user._id,
        email: user.email,
        name: user.name,
        nickname: user.nickname,
        bio: user.bio,
        urlImmagineProfilo: user.urlImmagineProfilo,
        level: user.level,
        totalPoints: user.totalPoints,
        co2Saved: user.co2Saved
      }
    });
  } catch (error) {
    console.error('Update profile error:', error);
    res.status(500).json({ error: 'Errore aggiornamento profilo' });
  }
});

// ============== FRIENDS ==============

app.get('/api/user/friends', authenticateToken, async (req, res) => {
  try {
    const user = await User.findById(req.user.id).populate('friends', 'name nickname level totalPoints co2Saved urlImmagineProfilo badges');
    if (!user) return res.status(404).json({ error: 'Utente non trovato' });

    const friendsList = (user.friends || []).map(f => ({
      id: f._id,
      name: f.name,
      nickname: f.nickname,
      level: f.level,
      urlImmagineProfilo: f.urlImmagineProfilo,
      badges: f.badges || []
    }));

    res.json(friendsList);
  } catch (error) {
    console.error('Friends error:', error);
    res.status(500).json({ error: 'Errore nel recupero amici' });
  }
});

app.post('/api/user/friends/add', authenticateToken, async (req, res) => {
  try {
    const { query } = req.body;
    
    const friend = await User.findOne({ 
      $or: [
        { email: query.toLowerCase() }, 
        { nickname: query }
      ] 
    });
    
    if (!friend) return res.status(404).json({ error: 'Utente non trovato' });
    if (friend._id.toString() === req.user.id) {
      return res.status(400).json({ error: 'Non puoi aggiungere te stesso' });
    }

    const user = await User.findById(req.user.id);
    if (user.friends.includes(friend._id)) {
      return res.status(400).json({ error: 'Utente già presente negli amici' });
    }

    user.friends.push(friend._id);
    user.badges = checkBadges(user);
    await user.save();

    res.json({ message: 'Amico aggiunto con successo', friend: friend.name });
  } catch (error) {
    console.error('Add friend error:', error);
    res.status(500).json({ error: 'Errore aggiunta amico' });
  }
});

app.post('/api/user/friends/remove', authenticateToken, async (req, res) => {
  try {
    const { friendId } = req.body;

    await User.findByIdAndUpdate(req.user.id, { $pull: { friends: friendId } });
    await User.findByIdAndUpdate(friendId, { $pull: { friends: req.user.id } });

    res.json({ message: 'Amico rimosso con successo' });
  } catch (error) {
    console.error('Remove friend error:', error);
    res.status(500).json({ error: 'Errore rimozione amico' });
  }
});

app.post('/api/user/friends/request', authenticateToken, async (req, res) => {
  try {
    const { query } = req.body;
    
    const receiver = await User.findOne({ 
      $or: [
        { email: query.toLowerCase() }, 
        { nickname: query }
      ] 
    });
    
    if (!receiver) return res.status(404).json({ error: 'Utente non trovato' });
    if (receiver._id.toString() === req.user.id) {
      return res.status(400).json({ error: 'Non puoi aggiungere te stesso' });
    }

    const sender = await User.findById(req.user.id);
    if (sender.friends.includes(receiver._id)) {
      return res.status(400).json({ error: 'Già amici' });
    }
    if (receiver.pendingRequests.includes(sender._id)) {
      return res.status(400).json({ error: 'Richiesta già inviata' });
    }

    await User.findByIdAndUpdate(receiver._id, { $addToSet: { pendingRequests: sender._id } });

    res.json({ message: 'Richiesta inviata' });
  } catch (error) {
    console.error('Friend request error:', error);
    res.status(500).json({ error: 'Errore invio richiesta' });
  }
});

app.get('/api/user/friends/requests', authenticateToken, async (req, res) => {
  try {
    const user = await User.findById(req.user.id).populate('pendingRequests', 'name nickname email urlImmagineProfilo');
    if (!user) return res.status(404).json({ error: 'Utente non trovato' });

    const requests = (user.pendingRequests || []).map(r => ({
      id: r._id,
      name: r.name,
      nickname: r.nickname,
      urlImmagineProfilo: r.urlImmagineProfilo
    }));

    res.json(requests);
  } catch (error) {
    console.error('Friend requests error:', error);
    res.status(500).json({ error: 'Errore recupero richieste' });
  }
});

app.post('/api/user/friends/respond', authenticateToken, async (req, res) => {
  try {
    const { senderId, action } = req.body;

    const user = await User.findById(req.user.id);
    const sender = await User.findById(senderId);

    if (!user || !sender) return res.status(404).json({ error: 'Utente non trovato' });

    // Rimuovi dalla pending list
    await User.findByIdAndUpdate(user._id, { $pull: { pendingRequests: sender._id } });

    if (action === 'accept') {
      // Aggiungi a entrambi gli amici
      await User.findByIdAndUpdate(user._id, { $addToSet: { friends: sender._id } });
      await User.findByIdAndUpdate(sender._id, { $addToSet: { friends: user._id } });

      // Aggiorna badge per entrambi
      const updatedUser = await User.findById(user._id);
      const updatedSender = await User.findById(sender._id);
      
      updatedUser.badges = checkBadges(updatedUser);
      updatedSender.badges = checkBadges(updatedSender);
      
      await updatedUser.save();
      await updatedSender.save();
    }

    res.json({ message: action === 'accept' ? 'Accettata' : 'Rifiutata' });
  } catch (error) {
    console.error('Friend respond error:', error);
    res.status(500).json({ error: 'Errore risposta richiesta' });
  }
});

app.get('/api/user/profile/:id', authenticateToken, async (req, res) => {
  try {
    const user = await User.findById(req.params.id);
    if (!user) return res.status(404).json({ error: 'Utente non trovato' });
    res.json(user); // Restituisci l'oggetto utente completo
  } catch (error) {
    res.status(500).json({ error: 'Errore server' });
  }
});

// ============== QUESTS ==============

app.get('/api/quests', authenticateToken, async (req, res) => {
  try {
    const quests = await GlobalQuest.find().sort({ id: 1 });
    res.json(quests);
  } catch (error) {
    console.error('Global quests error:', error);
    res.status(500).json({ error: 'Errore nel caricamento delle missioni' });
  }
});

app.get('/api/user/quests', authenticateToken, async (req, res) => {
  try {
    const userQuests = await UserQuest.find({ userId: req.user.id });
    // Mappa al formato atteso dall'app
    const formatted = userQuests.map(q => ({
      questId: q.questId,
      actual_progress: q.actual_progress,
      times_completed: q.times_completed,
      is_currently_active: q.isActive
    }));
    res.json(formatted);
  } catch (error) {
    console.error('User quests error:', error);
    res.status(500).json([]);
  }
});

app.post('/api/user/quests/set-actual-progress', authenticateToken, async (req, res) => {
  try {
    const { questId, actual_progress } = req.body;
    
    const quest = await UserQuest.findOneAndUpdate(
      { userId: req.user.id, questId: Number(questId) },
      { actual_progress: Number(actual_progress) },
      { new: true, upsert: true }
    );

    res.json({ success: true, quest });
  } catch (error) {
    console.error('Set progress error:', error);
    res.status(500).json({ error: 'Errore aggiornamento progresso' });
  }
});

app.post('/api/user/quests/set-times-completed', authenticateToken, async (req, res) => {
  try {
    const { questId, times_completed } = req.body;
    
    const quest = await UserQuest.findOneAndUpdate(
      { userId: req.user.id, questId: Number(questId) },
      { times_completed: Number(times_completed) },
      { new: true, upsert: true }
    );

    res.json({ success: true, quest });
  } catch (error) {
    console.error('Set times completed error:', error);
    res.status(500).json({ error: 'Errore aggiornamento' });
  }
});

app.post('/api/user/quests/set-is-active', authenticateToken, async (req, res) => {
  try {
    const { questId, is_currently_active } = req.body;
    
    const quest = await UserQuest.findOneAndUpdate(
      { userId: req.user.id, questId: Number(questId) },
      { isActive: Boolean(is_currently_active) },
      { new: true, upsert: true }
    );

    res.json({ success: true, quest });
  } catch (error) {
    console.error('Set active error:', error);
    res.status(500).json({ error: 'Errore aggiornamento quest' });
  }
});

app.post('/api/user/quests/update', authenticateToken, async (req, res) => {
  try {
    const { questId, progressIncrement } = req.body;

    const globalQuest = await GlobalQuest.findOne({ id: Number(questId) });
    if (!globalQuest) return res.status(404).json({ error: 'Quest globale non trovata' });

    let userQuest = await UserQuest.findOne({ userId: req.user.id, questId: Number(questId) });
    
    if (!userQuest) {
      userQuest = new UserQuest({ 
        userId: req.user.id, 
        questId: Number(questId), 
        actual_progress: Number(progressIncrement), 
        times_completed: 0, 
        isActive: true 
      });
    } else {
      userQuest.actual_progress += Number(progressIncrement);
      // IMPORTANTE: se l'utente sta accettando la quest, attivarla
      if (!userQuest.isActive) {
        userQuest.isActive = true;
      }
    }

    // Check completamento
    if (userQuest.actual_progress >= globalQuest.max_progress) {
      userQuest.times_completed += 1;
      userQuest.actual_progress = 0;
      userQuest.isActive = false; // Sposta la quest da Ongoing a Completed

      // Aggiorna utente
      const user = await User.findById(req.user.id);
      user.totalPoints += globalQuest.reward_points;
      user.co2Saved += globalQuest.CO2_saved;
      user.level = calculateLevel(user.totalPoints);
      user.badges = checkBadges(user);
      await user.save();
    }

    await userQuest.save();

    res.json({
      message: 'Progresso aggiornato',
      userQuest: {
        questId: userQuest.questId,
        actual_progress: userQuest.actual_progress,
        times_completed: userQuest.times_completed,
        is_currently_active: userQuest.isActive
      }
    });
  } catch (error) {
    console.error('Quest update error:', error);
    res.status(500).json({ error: 'Errore aggiornamento quest' });
  }
});

// ============== DEBUG/ADMIN ==============

app.get('/api/admin/dump/:collection', async (req, res) => {
  try {
    const { collection } = req.params;
    
    let data;
    switch(collection) {
      case 'users':
        data = await User.find().select('-password');
        break;
      case 'global-quests':
        data = await GlobalQuest.find();
        break;
      case 'user-quests':
        data = await UserQuest.find();
        break;
      default:
        return res.status(404).json({ error: 'Collection non riconosciuta' });
    }
    
    res.json(data);
  } catch (error) {
    console.error('Dump error:', error);
    res.status(500).json({ error: 'Errore durante la lettura' });
  }
});

// ============== START SERVER ==============

app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 EcoApp Server running on port ${PORT}`);
  console.log(`📦 Database: MongoDB Atlas`);
});
