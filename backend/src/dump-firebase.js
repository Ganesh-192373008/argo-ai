const { db } = require('./firebase');

async function dumpDatabase() {
  console.log('===================================================');
  console.log('🔥 GOOGLE FIREBASE CLOUD DATABASE (agri-app-fd900)');
  console.log('===================================================\n');

  if (!db) {
    console.error('❌ Firebase instance not connected');
    return;
  }

  // 1. Fetch Users
  try {
    const usersSnap = await db.collection('users').get();
    console.log(`👥 USERS COLLECTION (${usersSnap.size} Documents):`);
    usersSnap.forEach(doc => {
      console.log(` - Document ID: [${doc.id}]`);
      console.log(JSON.stringify(doc.data(), null, 2));
    });
  } catch (e) {
    console.log('Users collection:', e.message);
  }

  console.log('\n---------------------------------------------------');

  // 2. Fetch History
  try {
    const historySnap = await db.collection('history').get();
    console.log(`🔬 SCAN HISTORY COLLECTION (${historySnap.size} Documents):`);
    historySnap.forEach(doc => {
      console.log(` - Scan ID: [${doc.id}]`);
      console.log(JSON.stringify(doc.data(), null, 2));
    });
  } catch (e) {
    console.log('History collection:', e.message);
  }

  console.log('\n===================================================');
}

dumpDatabase();
