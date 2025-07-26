const express = require('express');
const WebSocket = require('ws');

const app = express();
const server = app.listen(8083, () => console.log("Audio service running on port 8083"));

const wss = new WebSocket.Server({ server });
wss.on('connection', (ws) => {
  ws.on('message', (msg) => {
    console.log('Message:', msg.toString());
    ws.send('Réponse IA audio simulée');
  });
});
