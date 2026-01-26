const mongoose = require('mongoose');

const globalQuestSchema = new mongoose.Schema({
  id: { type: Number, required: true, unique: true },
  name: { type: String, required: true },
  description: String,
  //type: { type: String, enum: ['Alimentation', 'Mobility', 'Home', 'Waste']},
  type: String,
  max_progress: { type: Number, default: 1 },
  CO2_saved: { type: Number, default: 0 },
  reward_points: { type: Number, default: 0 },
  quest_image: String,
  images_eu_goals: [String]
});

module.exports = mongoose.model('GlobalQuest', globalQuestSchema);
