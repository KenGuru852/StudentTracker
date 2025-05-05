const express = require('express');
const path = require('path');
const fetch = require('node-fetch');

const app = express();
const PORT = 3000;
const BACKEND_URL = 'http://backend:8080';

app.use(express.static(path.join(__dirname, '../public')));

app.get('/api/hello', async (req, res) => {
    try {
        const response = await fetch(`${BACKEND_URL}/api/hello`);
        if (!response.ok) throw new Error('Backend response not OK');
        const text = await response.text();
        res.send(text);
    } catch (error) {
        console.error('Backend request failed:', error);
        res.status(502).send('Backend service unavailable');
    }
});

app.listen(PORT, () => {
    console.log(`Frontend server running on http://localhost:${PORT}`);
    console.log(`Proxying backend requests to: ${BACKEND_URL}`);
});