const mongoose = require('mongoose');

const userQuestSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  questId: { type: Number, required: true },
  actual_progress: { type: Number, default: 0 },
  times_completed: { type: Number, default: 0 },
  is_currently_active: { type: Boolean, default: false }
});

userQuestSchema.index({ userId: 1, questId: 1 }, { unique: true });

module.exports = mongoose.model('UserQuest', userQuestSchema);
