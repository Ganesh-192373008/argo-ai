const { db } = require('./firebase');

async function testFullFirebaseSync() {
  console.log('\n===================================================');
  console.log('🔍 AUDITING LIVE FIREBASE DATABASE SYNC (agri-app-fd900)...');
  console.log('===================================================\n');

  if (!db) {
    console.error('❌ Firebase Firestore instance is NOT initialized!');
    return;
  }

  const collections = ['users', 'history', 'communityPosts', 'marketPrices', 'govSchemes'];

  for (const colName of collections) {
    try {
      // 1. Perform Write Test
      const testId = `sync_test_${Date.now()}`;
      const docRef = db.collection(colName).doc(testId);
      const testPayload = {
        syncStatus: 'SUCCESSFUL',
        collection: colName,
        timestamp: new Date().toISOString(),
        projectId: 'agri-app-fd900'
      };

      await docRef.set(testPayload);
      console.log(`✅ [${colName}] WRITE PASSED -> Created document ${testId}`);

      // 2. Perform Read Test
      const snap = await docRef.get();
      if (snap.exists && snap.data().syncStatus === 'SUCCESSFUL') {
        console.log(`✅ [${colName}] READ PASSED  -> Data verified from Google Cloud Firestore`);
      } else {
        console.error(`❌ [${colName}] READ FAILED`);
      }

      // 3. Clean up test document
      await docRef.delete();
      console.log(`✅ [${colName}] DELETE CLEANUP PASSED`);
      console.log('---------------------------------------------------');
    } catch (err) {
      console.error(`❌ [${colName}] SYNC ERROR:`, err.message);
    }
  }

  console.log('\n===================================================');
  console.log('🎉 LIVE SYNC AUDIT COMPLETE FOR ALL 5 COLLECTIONS!');
  console.log('===================================================\n');
}

testFullFirebaseSync();
