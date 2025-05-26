document.addEventListener('DOMContentLoaded', () => {
    const generateBtn = document.getElementById('generateBtn');
    const jsonFileInput = document.getElementById('jsonFile');
    const xlsxFileInput = document.getElementById('xlsxFile');
    const resultDiv = document.getElementById('result');
    const streamFilter = document.getElementById('streamFilter');
    const subjectFilter = document.getElementById('subjectFilter');
    const applyFiltersBtn = document.getElementById('applyFiltersBtn');
    const filteredResultsDiv = document.getElementById('filteredResults');

    // Загружаем таблицы при старте (если они есть)
    displayFilteredTables();

    // Обработчик для кнопки "Генерировать"
    generateBtn.addEventListener('click', async () => {
        const jsonFile = jsonFileInput.files[0];
        const xlsxFile = xlsxFileInput.files[0];
        
        if (!jsonFile || !xlsxFile) {
            resultDiv.textContent = 'Пожалуйста, выберите оба файла';
            return;
        }

        const formData = new FormData();
        formData.append('jsonFile', jsonFile);
        formData.append('xlsxFile', xlsxFile);

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

    // Функция для фильтрации таблиц
    async function displayFilteredTables() {
        const streamFilterValue = streamFilter.value.trim();
        const subjectFilterValue = subjectFilter.value.trim();
        
        filteredResultsDiv.innerHTML = 'Загрузка...';
        
        try {
            const params = new URLSearchParams();
            if (streamFilterValue) params.append('stream', streamFilterValue);
            if (subjectFilterValue) params.append('subject', subjectFilterValue);
            
            const response = await fetch(`/api/getFilteredLinks?${params.toString()}`, {
                headers: {
                    'Accept': 'application/json'
                }
            });
            
            if (!response.ok) {
                throw new Error('Ошибка загрузки таблиц');
            }
            
            const links = await response.json();
            
            if (links.length === 0) {
                filteredResultsDiv.innerHTML = 'Нет таблиц, соответствующих фильтрам';
                return;
            }
            
            // Отображаем результаты
            filteredResultsDiv.innerHTML = links.map(link => `
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

    // Дебаунс для фильтрации при вводе
    function debounce(func, timeout = 500) {
        let timer;
        return (...args) => {
            clearTimeout(timer);
            timer = setTimeout(() => func.apply(this, args), timeout);
        };
    }

    // Слушатели для фильтров
    streamFilter.addEventListener('input', debounce(displayFilteredTables));
    subjectFilter.addEventListener('input', debounce(displayFilteredTables));
    applyFiltersBtn.addEventListener('click', displayFilteredTables);
});