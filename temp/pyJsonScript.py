import json

def filter_ip1_groups(input_file, output_file):
    """
    Читает JSON-файл с расписанием, фильтрует записи, оставляя только группы, начинающиеся с "ИП-1",
    и сохраняет результат в новый JSON-файл.
    """
    try:
        # Чтение исходного JSON-файла
        with open(input_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # Фильтрация данных (предполагаем, что это список объектов)
        filtered_data = [item for item in data if item.get("Группа", "").startswith("ИП-1")]
        
        # Сохранение отфильтрованных данных в новый файл
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(filtered_data, f, ensure_ascii=False, indent=4)
        
        print(f"Успешно сохранено {len(filtered_data)} записей в файл {output_file}")
    
    except Exception as e:
        print(f"Произошла ошибка: {e}")

# Укажите пути к вашему файлу и выходному файлу
input_json = 'fullSchedule.json'  # Замените на ваш файл
output_json = 'demoSchedule.json'  # Выходной файл

filter_ip1_groups(input_json, output_json)