const express = require('express');
const path = require('path');
const fileUpload = require('express-fileupload');
const FormData = require('form-data');
const fetch = (...args) =>
    import('node-fetch').then(({ default: fetch }) => fetch(...args));
const cors = require('cors');

const app = express();
const PORT = 3000;

const BACKEND_URL = 'http://backend:8080'; 

app.use(cors());
app.use(fileUpload({
  limits: { fileSize: 10 * 1024 * 1024 * 1024 },
  abortOnLimit: true
}));

app.use(express.static(path.join(__dirname, 'public')));
app.use('/styles', express.static(path.join(__dirname, 'public', 'styles')));
app.use('/js', express.static(path.join(__dirname, 'public', 'js')));

app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.get('/api/getFilteredLinks', async (req, res) => {
    try {
        const { stream, subject, teacher } = req.query;
        const params = new URLSearchParams();
        if (stream) params.append('stream', stream);
        if (subject) params.append('subject', subject);
        if (teacher) params.append('teacher', teacher);

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

app.post('/api/clearAllData', async (req, res) => {
    try {
        const response = await fetch(`${BACKEND_URL}/api/clearAllData`, {
            method: 'POST'
        });
        
        res.status(response.status).send(await response.text());
    } catch (error) {
        console.error('Backend connection error:', error);
        res.status(502).send('Ошибка соединения с бэкендом');
    }
});

app.post('/api/generateTables', async (req, res) => {
    try {
        if (!req.files?.scheduleFile || !req.files?.studentFile || !req.files?.teachersFile) {
            return res.status(400).send('Необходимо загрузить все три файла');
        }

        const scheduleFile = req.files.scheduleFile;
        const studentFile = req.files.studentFile;
        const teachersFile = req.files.teachersFile;

        const isScheduleValid = scheduleFile.name.endsWith('.json') && 
                              scheduleFile.mimetype === 'application/json';
        const isStudentValid = studentFile.name.match(/\.(xlsx|xls)$/i) && 
                             (studentFile.mimetype === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
                              studentFile.mimetype === 'application/vnd.ms-excel');
        const isTeachersValid = teachersFile.name.endsWith('.json') && 
                              teachersFile.mimetype === 'application/json';

        if (!isScheduleValid || !isStudentValid || !isTeachersValid) {
            let errorMessage = 'Неверные форматы файлов:';
            if (!isScheduleValid) errorMessage += '\n- Расписание должно быть в формате JSON';
            if (!isStudentValid) errorMessage += '\n- База студентов должна быть в формате Excel (XLSX/XLS)';
            if (!isTeachersValid) errorMessage += '\n- База преподавателей должна быть в формате JSON';
            
            return res.status(400).send(errorMessage);
        }

        const formData = new FormData();
        formData.append('scheduleFile', scheduleFile.data, {
            filename: scheduleFile.name,
            contentType: scheduleFile.mimetype
        });
        formData.append('studentFile', studentFile.data, {
            filename: studentFile.name,
            contentType: studentFile.mimetype
        });
        formData.append('teachersFile', teachersFile.data, {
            filename: teachersFile.name,
            contentType: teachersFile.mimetype
        });

        const response = await fetch(`${BACKEND_URL}/api/generateTables`, {
            method: 'POST',
            body: formData,
            headers: formData.getHeaders()
        });

        res.status(response.status).json(await response.json());
    } catch (error) {
        console.error('Backend connection error:', error);
        res.status(502).json({ error: 'Ошибка соединения с бэкендом' });
    }
});

app.listen(PORT, () => {
    console.log(`Frontend server running on http://localhost:${PORT}`);
    console.log(`Backend URL: ${BACKEND_URL}`);
});