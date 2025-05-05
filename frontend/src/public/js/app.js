document.addEventListener('DOMContentLoaded', () => {
    const generateBtn = document.getElementById('generateBtn');
    const jsonFileInput = document.getElementById('jsonFile');
    const xlsxFileInput = document.getElementById('xlsxFile');
    const resultDiv = document.getElementById('result');

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
                throw new Error(`Ошибка сервера: ${response.status}`);
            }

            const result = await response.text();
            resultDiv.textContent = result;
        } catch (error) {
            resultDiv.textContent = `Ошибка: ${error.message}`;
            console.error('Ошибка при отправке файлов:', error);
        }
    });
});