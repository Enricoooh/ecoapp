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
  const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN

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
  res.json({ message: 'EcoApp Backend API', status: 'running' });
});

// Register endpoint
app.post('/api/auth/register', async (req, res) => {
  try {
    const { email, password, name } = req.body;

    // Validation
    if (!email || !password || !name) {
      return res.status(400).json({ error: 'Email, password e nome sono richiesti' });
    }

    if (password.length < 6) {
      return res.status(400).json({ error: 'La password deve essere di almeno 6 caratteri' });
    }

    // Check if user already exists
    const db = readDB();
    const existingUser = db.users.find(u => u.email === email);
    if (existingUser) {
      return res.status(409).json({ error: 'Email già registrata' });
    }

    // Hash password
    const hashedPassword = await bcrypt.hash(password, 10);

    // Create new user
    const newUser = {
      id: Date.now().toString(),
      email,
      password: hashedPassword,
      name,
      level: 'Eco-Novizio',
      totalPoints: 0,
      co2Saved: 0.0,
      createdAt: new Date().toISOString()
    };

    db.users.push(newUser);
    writeDB(db);

    // Generate JWT token
    const token = jwt.sign(
      { id: newUser.id, email: newUser.email },
      JWT_SECRET,
      { expiresIn: '30d' }
    );

    res.status(201).json({
      message: 'Registrazione completata',
      token,
      user: {
        id: newUser.id,
        email: newUser.email,
        name: newUser.name,
        level: newUser.level,
        totalPoints: newUser.totalPoints,
        co2Saved: newUser.co2Saved
      }
    });
  } catch (error) {
    console.error('Register error:', error);
    res.status(500).json({ error: 'Errore del server' });
  }
});

// Login endpoint
app.post('/api/auth/login', async (req, res) => {
  try {
    const { email, password } = req.body;

    // Validation
    if (!email || !password) {
      return res.status(400).json({ error: 'Email e password sono richiesti' });
    }

    // Find user
    const db = readDB();
    const user = db.users.find(u => u.email === email);
    if (!user) {
      return res.status(401).json({ error: 'Credenziali non valide' });
    }

    // Verify password
    const isPasswordValid = await bcrypt.compare(password, user.password);
    if (!isPasswordValid) {
      return res.status(401).json({ error: 'Credenziali non valide' });
    }

    // Generate JWT token
    const token = jwt.sign(
      { id: user.id, email: user.email },
      JWT_SECRET,
      { expiresIn: '30d' }
    );

    res.json({
      message: 'Login effettuato',
      token,
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
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

// Get user profile endpoint (protected)
app.get('/api/user/profile', authenticateToken, (req, res) => {
  try {
    const db = readDB();
    const user = db.users.find(u => u.id === req.user.id);

    if (!user) {
      return res.status(404).json({ error: 'Utente non trovato' });
    }

    res.json({
      id: user.id,
      email: user.email,
      name: user.name,
      level: user.level,
      totalPoints: user.totalPoints,
      co2Saved: user.co2Saved
    });
  } catch (error) {
    console.error('Profile error:', error);
    res.status(500).json({ error: 'Errore del server' });
  }
});

// Update user profile endpoint (protected)
app.put('/api/user/profile', authenticateToken, (req, res) => {
  try {
    const { name, totalPoints, co2Saved } = req.body;
    const db = readDB();
    const userIndex = db.users.findIndex(u => u.id === req.user.id);

    if (userIndex === -1) {
      return res.status(404).json({ error: 'Utente non trovato' });
    }

    // Update user data
    if (name) db.users[userIndex].name = name;
    if (totalPoints !== undefined) db.users[userIndex].totalPoints = totalPoints;
    if (co2Saved !== undefined) db.users[userIndex].co2Saved = co2Saved;

    // Update level based on points
    const points = db.users[userIndex].totalPoints;
    if (points >= 5000) {
      db.users[userIndex].level = 'Eco-Maestro';
    } else if (points >= 2500) {
      db.users[userIndex].level = 'Eco-Esperto';
    } else if (points >= 1000) {
      db.users[userIndex].level = 'Eco-Warrior';
    } else if (points >= 500) {
      db.users[userIndex].level = 'Eco-Guardiano';
    } else {
      db.users[userIndex].level = 'Eco-Novizio';
    }

    writeDB(db);

    res.json({
      message: 'Profilo aggiornato',
      user: {
        id: db.users[userIndex].id,
        email: db.users[userIndex].email,
        name: db.users[userIndex].name,
        level: db.users[userIndex].level,
        totalPoints: db.users[userIndex].totalPoints,
        co2Saved: db.users[userIndex].co2Saved
      }
    });
  } catch (error) {
    console.error('Update profile error:', error);
    res.status(500).json({ error: 'Errore del server' });
  }
});

// Start server
app.listen(PORT, () => {
  console.log(`🚀 EcoApp Backend running on port ${PORT}`);
});
