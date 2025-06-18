import json

def get_unique_persons(json_file):
    # Загружаем данные из JSON файла
    with open(json_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    # Собираем все уникальные физические лица
    unique_persons = set()
    for entry in data:
        if "ФизическоеЛицо" in entry:
            unique_persons.add(entry["ФизическоеЛицо"])
    
    # Выводим результат
    print("Уникальные физические лица:")
    for i, person in enumerate(sorted(unique_persons), 1):
        print(f"{person}")

# Укажите путь к вашему JSON файлу
json_file_path = "schedule_IP_groups.json"  # Замените на актуальный путь к файлу
get_unique_persons(json_file_path)