const express = require('express');
const path = require('path');
const fileUpload = require('express-fileupload');
const FormData = require('form-data');
const fetch = (...args) =>
    import('node-fetch').then(({ default: fetch }) => fetch(...args));
const cors = require('cors'); // Импортируем cors

const app = express();
const PORT = 3000;

const BACKEND_URL = 'http://backend:8080'; 

app.use(cors()); // Используем cors
app.use(fileUpload());

app.use(express.static(path.join(__dirname, 'public')));
app.use('/styles', express.static(path.join(__dirname, 'public', 'styles')));
app.use('/js', express.static(path.join(__dirname, 'public', 'js')));

app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.get('/api/getFilteredLinks', async (req, res) => {
    try {
        const { stream, subject } = req.query;
        const params = new URLSearchParams();
        if (stream) params.append('stream', stream);
        if (subject) params.append('subject', subject);
        
        const response = await fetch(`${BACKEND_URL}/api/getFilteredLinks?${params}`);
        
        if (!response.ok) {
            throw new Error(`Backend error: ${response.status}`);
        }
        
        const data = await response.json();
        res.set('Content-Type', 'application/json');
        res.send(data);
    } catch (error) {
        console.error('Proxy error:', error);
        res.status(500).json({ 
            error: error.message,
            details: `Failed to connect to backend at ${BACKEND_URL}`
        });
    }
});

app.post('/api/generateTables', async (req, res) => {
    try {
        if (!req.files?.jsonFile || !req.files?.xlsxFile) {
            return res.status(400).send('Необходимо загрузить оба файла');
        }

        const formData = new FormData();
        formData.append('jsonFile', req.files.jsonFile.data, {
            filename: req.files.jsonFile.name,
            contentType: req.files.jsonFile.mimetype
        });
        formData.append('xlsxFile', req.files.xlsxFile.data, {
            filename: req.files.xlsxFile.name,
            contentType: req.files.xlsxFile.mimetype
        });

        const response = await fetch(`${BACKEND_URL}/api/generateTables`, {
            method: 'POST',
            body: formData,
            headers: formData.getHeaders() 
        });

        res.status(response.status).send(await response.text());
    } catch (error) {
        console.error('Backend connection error:', {
            message: error.message,
            url: BACKEND_URL
        });
        res.status(502).send('Ошибка соединения с бэкендом');
    }
});

app.listen(PORT, () => {
    console.log(`Frontend server running on http://localhost:${PORT}`);
    console.log(`Backend URL: ${BACKEND_URL}`);
});
