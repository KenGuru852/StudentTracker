import json
from typing import List, Dict, Union

def remove_duplicates(input_file: str, output_file: str, key_fields: List[str] = None) -> None:
    """
    Удаляет дубликаты JSON-объектов из файла.
    
    Параметры:
        input_file: путь к входному JSON-файлу
        output_file: путь для сохранения результата
        key_fields: список полей для определения уникальности (если None, сравниваются все поля)
    """
    try:
        # Чтение данных из файла
        with open(input_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        if not isinstance(data, list):
            raise ValueError("JSON должен содержать массив объектов")
        
        # Функция для создания ключа сравнения
        def get_comparison_key(obj: Dict) -> tuple:
            if key_fields:
                return tuple(obj.get(field) for field in key_fields)
            return tuple(sorted(obj.items()))
        
        # Удаление дубликатов с сохранением порядка
        seen = set()
        unique_data = []
        for item in data:
            key = get_comparison_key(item)
            if key not in seen:
                seen.add(key)
                unique_data.append(item)
        
        # Сохранение результата
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(unique_data, f, ensure_ascii=False, indent=4)
        
        print(f"Удалено {len(data) - len(unique_data)} дубликатов. Результат сохранен в {output_file}")
    
    except Exception as e:
        print(f"Ошибка: {str(e)}")

# Пример использования
if __name__ == "__main__":
    input_json = "fullScheduleDemo.json"    # Ваш исходный файл
    output_json = "fullSchedule.json"  # Файл для сохранения результата
    
    # Указываем поля для проверки уникальности (или None для сравнения всех полей)
    key_fields = ["ДеньНедели", "Неделя", "Курс", "Группа", "Дисциплина"]
    
    remove_duplicates(input_json, output_json, key_fields)