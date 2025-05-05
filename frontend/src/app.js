const express = require('express');
const path = require('path');
const fileUpload = require('express-fileupload');
const FormData = require('form-data');
const fetch = (...args) =>
    import('node-fetch').then(({ default: fetch }) => fetch(...args));
const app = express();
const PORT = 3000;

const BACKEND_URL = 'http://backend:8080'; 

app.use(fileUpload());
app.use(express.static(path.join(__dirname, 'public')));
app.use('/styles', express.static(path.join(__dirname, 'public', 'styles')));
app.use('/js', express.static(path.join(__dirname, 'public', 'js')));

app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
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
