const tls = require('tls');

const EMAIL = 'ganeshgidda4@gmail.com';
const PASSWORDS = ['zjwbiuhkljcwwgz', 'zjwbiuhkljcwwgza', 'zjwbiuhk ljcwwgz'];

function testPass(pass) {
  return new Promise((resolve) => {
    console.log('Testing password:', pass);
    const socket = tls.connect(465, 'smtp.gmail.com', () => {});
    let step = 0;
    let success = false;

    socket.on('data', (data) => {
      const msg = data.toString();
      if (step === 0 && msg.startsWith('220')) {
        step = 1;
        socket.write('EHLO localhost\r\n');
      } else if (step === 1 && msg.includes('250')) {
        step = 2;
        socket.write('AUTH LOGIN\r\n');
      } else if (step === 2 && msg.startsWith('334')) {
        step = 3;
        socket.write(Buffer.from(EMAIL).toString('base64') + '\r\n');
      } else if (step === 3 && msg.startsWith('334')) {
        step = 4;
        socket.write(Buffer.from(pass.replace(/\s+/g, '')).toString('base64') + '\r\n');
      } else if (step === 4) {
        if (msg.startsWith('235')) {
          console.log('✅ SUCCESSFUL AUTH FOR PASSWORD:', pass);
          success = true;
        } else {
          console.log('❌ AUTH FAILED:', msg.trim());
        }
        socket.end();
        resolve(success);
      }
    });

    socket.on('error', (err) => {
      console.log('Err:', err.message);
      resolve(false);
    });
  });
}

async function run() {
  for (const p of PASSWORDS) {
    const ok = await testPass(p);
    if (ok) break;
  }
}

run();
