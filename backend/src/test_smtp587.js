const net = require('net');
const tls = require('tls');

const EMAIL = 'ganeshgidda4@gmail.com';
const PASS = 'zjwbiuhkljcwwgz'; // user's app password

console.log('Testing Gmail SMTP on port 587 STARTTLS...');

const client = net.createConnection(587, 'smtp.gmail.com');
let tlsSocket = null;

client.on('connect', () => {
  console.log('Connected to smtp.gmail.com:587');
});

client.on('data', (data) => {
  const msg = data.toString();
  console.log('587 <', msg.trim());
  if (msg.startsWith('220')) {
    client.write('EHLO localhost\r\n');
  } else if (msg.includes('250-STARTTLS') || msg.includes('250 STARTTLS') || msg.includes('250-8BITMIME')) {
    if (!tlsSocket) {
      client.write('STARTTLS\r\n');
    }
  } else if (msg.startsWith('220 2.0.0')) {
    // Upgrade socket to TLS
    tlsSocket = tls.connect({
      socket: client,
      rejectUnauthorized: false
    }, () => {
      console.log('TLS Upgrade Complete!');
      tlsSocket.write('EHLO localhost\r\n');
    });

    let tlsStep = 0;
    tlsSocket.on('data', (tData) => {
      const tMsg = tData.toString();
      console.log('TLS <', tMsg.trim());
      if (tlsStep === 0 && tMsg.includes('250')) {
        tlsStep = 1;
        tlsSocket.write('AUTH LOGIN\r\n');
      } else if (tlsStep === 1 && tMsg.startsWith('334')) {
        tlsStep = 2;
        tlsSocket.write(Buffer.from(EMAIL).toString('base64') + '\r\n');
      } else if (tlsStep === 2 && tMsg.startsWith('334')) {
        tlsStep = 3;
        tlsSocket.write(Buffer.from(PASS).toString('base64') + '\r\n');
      } else if (tlsStep === 3) {
        if (tMsg.startsWith('235')) {
          console.log('🎉 GMAIL SMTP AUTH SUCCESSFUL ON PORT 587 STARTTLS!');
        } else {
          console.log('SMTP Auth Response:', tMsg.trim());
        }
        client.end();
      }
    });
  }
});

client.on('error', (err) => console.log('Client Error:', err.message));
