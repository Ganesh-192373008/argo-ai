const { sendPasswordResetLinkEmail } = require('./mailer');

async function test() {
  console.log('Sending test password reset email...');
  const res = await sendPasswordResetLinkEmail('giddathimmannagariganesh3008.sse@savemail.com', 'http://localhost:3000/reset-password?email=giddathimmannagariganesh3008.sse@savemail.com&token=test');
  console.log('Result:', res);
}

test();
