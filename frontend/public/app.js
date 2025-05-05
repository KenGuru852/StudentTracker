document.addEventListener('DOMContentLoaded', () => {
    const messageEl = document.getElementById('message');
    
    fetch('/api/hello')
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.text();
        })
        .then(text => {
            messageEl.textContent = text;
            messageEl.style.color = 'green';
        })
        .catch(error => {
            console.error('Ошибка:', error);
            messageEl.textContent = 'Ошибка при загрузке данных с сервера';
            messageEl.style.color = 'red';
        });
});