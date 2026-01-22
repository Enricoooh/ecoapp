const mongoose = require('mongoose');

const badgeSchema = new mongoose.Schema({
  id: Number,
  name: String,
  description: String,
  unlockedAt: String
}, { _id: false });

const userSchema = new mongoose.Schema({
  email: { type: String, required: true, unique: true, lowercase: true, trim: true },
  password: { type: String, required: true },
  name: { type: String, required: true },
  nickname: { type: String, default: '' },
  bio: { type: String, default: '' },
  urlImmagineProfilo: { type: String, default: '' },
  level: { type: String, default: 'Eco-Novizio' },
  totalPoints: { type: Number, default: 0 },
  co2Saved: { type: Number, default: 0 },
  friends: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }],
  pendingRequests: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }],
  badges: [badgeSchema],
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('User', userSchema);
