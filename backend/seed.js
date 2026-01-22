require('dotenv').config();
const mongoose = require('mongoose');
const fs = require('fs');
const path = require('path');
const GlobalQuest = require('./models/GlobalQuest');

const MONGODB_URI = process.env.MONGODB_URI;

async function seed() {
  try {
    await mongoose.connect(MONGODB_URI);
    console.log('✅ Connected to MongoDB');

    const questsPath = path.join(__dirname, 'global_quests.json');
    const globalQuests = JSON.parse(fs.readFileSync(questsPath, 'utf8'));

    await GlobalQuest.deleteMany({});
    await GlobalQuest.insertMany(globalQuests);
    
    console.log(`✅ Seeded ${globalQuests.length} global quests`);
    process.exit(0);
  } catch (error) {
    console.error('❌ Seed error:', error);
    process.exit(1);
  }
}

seed();
