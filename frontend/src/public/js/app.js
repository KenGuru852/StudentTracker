document.addEventListener('DOMContentLoaded', () => {
    const generateBtn = document.getElementById('generateBtn');
    const jsonFileInput = document.getElementById('jsonFile');
    const xlsxFileInput = document.getElementById('xlsxFile');
    const teachersFileInput = document.getElementById('teachersFile');
    const resultDiv = document.getElementById('result');
    const streamFilter = document.getElementById('streamFilter');
    const subjectFilter = document.getElementById('subjectFilter');
    const applyFiltersBtn = document.getElementById('applyFiltersBtn');
    const filteredResultsDiv = document.getElementById('filteredResults');
    const clearDataBtn = document.getElementById('clearDataBtn');

    // Загружаем таблицы при старте (если они есть)
    displayFilteredTables();

    clearDataBtn.addEventListener('click', async () => {
        const confirmed = await showConfirmationDialog(
            'Вы уверены, что хотите очистить все данные?',
            'Это действие удалит все таблицы, студентов, группы и расписание. Отменить это действие будет невозможно.'
        );
        
        if (!confirmed) return;

        try {
            resultDiv.textContent = 'Очистка данных...';
            const response = await fetch('/api/clearAllData', { method: 'POST' });
            
            if (!response.ok) {
                const error = await response.text();
                throw new Error(error || `Ошибка сервера: ${response.status}`);
            }

            resultDiv.textContent = 'Все данные успешно очищены!';
            displayFilteredTables();
        } catch (error) {
            resultDiv.textContent = `Ошибка: ${error.message}`;
            console.error('Ошибка при очистке данных:', error);
        }
    });

    generateBtn.addEventListener('click', async () => {
        const jsonFile = jsonFileInput.files[0];
        const xlsxFile = xlsxFileInput.files[0];
        const teachersFile = teachersFileInput.files[0];
        
        if (!jsonFile || !xlsxFile || !teachersFile) {
            resultDiv.textContent = 'Пожалуйста, выберите все три файла';
            return;
        }

        const formData = new FormData();
        formData.append('jsonFile', jsonFile);
        formData.append('xlsxFile', xlsxFile);
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
            resultDiv.textContent = 'Таблицы успешно сгенерированы!';
            displayFilteredTables();
        } catch (error) {
            resultDiv.textContent = `Ошибка: ${error.message}`;
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
                <button id="confirmYes">Да, очистить</button>
                <button id="confirmNo">Отмена</button>
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
        
        filteredResultsDiv.innerHTML = 'Загрузка...';
        
        try {
            const params = new URLSearchParams();
            if (streamFilterValue) params.append('stream', streamFilterValue);
            if (subjectFilterValue) params.append('subject', subjectFilterValue);
            
            const response = await fetch(`/api/getFilteredLinks?${params.toString()}`);
            
            if (!response.ok) throw new Error('Ошибка загрузки таблиц');
            
            const links = await response.json();
            filteredResultsDiv.innerHTML = links.length === 0 
                ? 'Нет таблиц, соответствующих фильтрам' 
                : links.map(link => `
                    <div class="table-link">
                        <strong>${link.stream} - ${link.subject}</strong><br>
                        <a href="${link.link}" target="_blank">${link.link}</a>
                    </div>
                `).join('');
        } catch (error) {
            filteredResultsDiv.innerHTML = `Ошибка: ${error.message}`;
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
    applyFiltersBtn.addEventListener('click', displayFilteredTables);
});