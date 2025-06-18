document.addEventListener('DOMContentLoaded', () => {
    const generateBtn = document.getElementById('generateBtn');
    const scheduleFileInput = document.getElementById('scheduleFile');
    const studentFileInput = document.getElementById('studentFile');
    const teachersFileInput = document.getElementById('teachersFile');
    const resultDiv = document.getElementById('result');
    const streamFilter = document.getElementById('streamFilter');
    const subjectFilter = document.getElementById('subjectFilter');
    const teacherFilter = document.getElementById('teacherFilter');
    const applyFiltersBtn = document.getElementById('applyFiltersBtn');
    const filteredResultsDiv = document.getElementById('filteredResults');
    const clearDataBtn = document.getElementById('clearDataBtn');

    displayFilteredTables();

    clearDataBtn.addEventListener('click', async () => {
        const confirmed = await showConfirmationDialog(
            'Вы уверены, что хотите очистить все данные?',
            'Это действие удалит ВСЕ данные из ВСЕХ таблиц. Отменить это действие будет невозможно.'
        );
        
        if (!confirmed) return;

        try {
            resultDiv.textContent = 'Очистка данных...';
            const response = await fetch('/api/clearAllData', { method: 'POST' });
            
            if (!response.ok) {
                const error = await response.text();
                throw new Error(error || `Ошибка сервера: ${response.status}`);
            }

            resultDiv.innerHTML = '<div class="success-message">Все данные успешно очищены!</div>';
            displayFilteredTables();
        } catch (error) {
            resultDiv.innerHTML = `<div class="error-message">Ошибка: ${error.message}</div>`;
            console.error('Ошибка при очистке данных:', error);
        }
    });

    generateBtn.addEventListener('click', async () => {
        const scheduleFile = scheduleFileInput.files[0];
        const studentFile = studentFileInput.files[0];
        const teachersFile = teachersFileInput.files[0];
        
        if (!scheduleFile || !studentFile || !teachersFile) {
            resultDiv.innerHTML = '<div class="error-message">Пожалуйста, выберите все три файла</div>';
            return;
        }

        const isScheduleValid = scheduleFile.name.endsWith('.json') && 
                              scheduleFile.type === 'application/json';
        const isStudentValid = studentFile.name.match(/\.(xlsx|xls)$/i) && 
                             (studentFile.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
                              studentFile.type === 'application/vnd.ms-excel');
        const isTeachersValid = teachersFile.name.endsWith('.json') && 
                              teachersFile.type === 'application/json';

        if (!isScheduleValid || !isStudentValid || !isTeachersValid) {
            let errorMessage = 'Неверные форматы файлов:';
            if (!isScheduleValid) errorMessage += '<br>- Расписание должно быть в формате JSON';
            if (!isStudentValid) errorMessage += '<br>- База студентов должна быть в формате Excel (XLSX/XLS)';
            if (!isTeachersValid) errorMessage += '<br>- База преподавателей должна быть в формате JSON';
            
            resultDiv.innerHTML = `<div class="error-message">${errorMessage}</div>`;
            return;
        }

        const formData = new FormData();
        formData.append('scheduleFile', scheduleFile);
        formData.append('studentFile', studentFile);
        formData.append('teachersFile', teachersFile);

        try {
            resultDiv.textContent = 'Обработка файлов...';
            const response = await fetch('/api/generateTables', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                const error = await response.text();
                throw new Error(error || `Ошибка сервера: ${response.status}`);
            }

            const result = await response.json();
            resultDiv.innerHTML = '<div class="success-message">Таблицы успешно сгенерированы!</div>';
            displayFilteredTables();
        } catch (error) {
            resultDiv.innerHTML = `<div class="error-message">Ошибка: ${error.message}</div>`;
            console.error('Ошибка при отправке файлов:', error);
        }
    });

    function showConfirmationDialog(title, message) {
        return new Promise((resolve) => {
            const overlay = document.createElement('div');
            overlay.className = 'overlay';
            
            const dialog = document.createElement('div');
            dialog.className = 'confirmation-dialog';
            dialog.innerHTML = `
                <h3>${title}</h3>
                <p>${message}</p>
                <div class="dialog-buttons">
                    <button id="confirmYes" class="danger-btn">Да, очистить</button>
                    <button id="confirmNo">Отмена</button>
                </div>
            `;
            
            document.body.appendChild(overlay);
            document.body.appendChild(dialog);
            
            document.getElementById('confirmYes').addEventListener('click', () => {
                document.body.removeChild(overlay);
                document.body.removeChild(dialog);
                resolve(true);
            });
            
            document.getElementById('confirmNo').addEventListener('click', () => {
                document.body.removeChild(overlay);
                document.body.removeChild(dialog);
                resolve(false);
            });
        });
    }

    async function displayFilteredTables() {
        const streamFilterValue = streamFilter.value.trim();
        const subjectFilterValue = subjectFilter.value.trim();
        const teacherFilterValue = teacherFilter.value.trim();

        filteredResultsDiv.innerHTML = '<p>Загрузка данных...</p>';
        
        try {
            const params = new URLSearchParams();
            if (streamFilterValue) params.append('stream', streamFilterValue);
            if (subjectFilterValue) params.append('subject', subjectFilterValue);
            if (teacherFilterValue) params.append('teacher', teacherFilterValue);

            const response = await fetch(`/api/getFilteredLinks?${params.toString()}`);
            
            if (!response.ok) throw new Error('Ошибка загрузки таблиц');
            
            const links = await response.json();
            filteredResultsDiv.innerHTML = links.length === 0 
                ? '<p>Нет таблиц, соответствующих фильтрам</p>' 
                : links.map(link => `
                    <div class="table-link">
                        <strong>${link.stream} - ${link.subject}</strong>
                        <span>Преподаватель: ${link.teacher}</span>
                        <a href="${link.link}" target="_blank">Открыть таблицу</a>
                    </div>
                `).join('');
        } catch (error) {
            filteredResultsDiv.innerHTML = `<div class="error-message">Ошибка: ${error.message}</div>`;
            console.error('Ошибка:', error);
        }
    }

    function debounce(func, timeout = 500) {
        let timer;
        return (...args) => {
            clearTimeout(timer);
            timer = setTimeout(() => func.apply(this, args), timeout);
        };
    }

    streamFilter.addEventListener('input', debounce(displayFilteredTables));
    subjectFilter.addEventListener('input', debounce(displayFilteredTables));
    teacherFilter.addEventListener('input', debounce(displayFilteredTables));
    applyFiltersBtn.addEventListener('click', displayFilteredTables);
});