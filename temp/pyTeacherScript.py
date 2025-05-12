import json
import transliterate
from faker import Faker
from typing import Dict, List

def generate_teacher_emails(input_file: str, output_file: str) -> None:
    """
    Создает JSON-файл с уникальными преподавателями и их email-адресами.
    
    :param input_file: Путь к исходному JSON-файлу с расписанием
    :param output_file: Путь для сохранения результата
    """
    try:
        # Чтение исходного файла
        with open(input_file, 'r', encoding='utf-8') as f:
            schedule_data = json.load(f)
        
        if not isinstance(schedule_data, list):
            raise ValueError("JSON должен содержать массив объектов")

        # Словарь для хранения преподавателей (ключ - ФИО, значение - email)
        teachers = {}
        
        # Инициализация Faker для английских имен
        fake = Faker()
        
        # Функция для генерации email на основе ФИО
        def generate_email(full_name: str) -> str:
            try:
                # Разбиваем ФИО на части
                parts = full_name.split()
                last_name = parts[0] if len(parts) > 0 else ""
                first_name = parts[1] if len(parts) > 1 else ""
                middle_name = parts[2] if len(parts) > 2 else ""
                
                # Транслитерируем в латиницу
                last_name_lat = transliterate.translit(last_name, reversed=True).replace("'", "")
                first_initial = transliterate.translit(first_name[0], reversed=True).replace("'", "") if first_name else ""
                middle_initial = transliterate.translit(middle_name[0], reversed=True).replace("'", "") if middle_name else ""
                
                # Формируем email
                email = f"{last_name_lat.lower()}.{first_initial.lower()}{middle_initial.lower()}@university.edu"
                return email
            except:
                # Если возникла ошибка при генерации, используем случайный email
                return fake.email()

        # Обработка всех записей расписания
        for entry in schedule_data:
            teacher_name = entry.get("ФизическоеЛицо")
            if teacher_name and teacher_name not in teachers:
                teachers[teacher_name] = generate_email(teacher_name)
        
        # Преобразуем словарь в список объектов для сохранения
        result = [{"ФИО": name, "Email": email} for name, email in teachers.items()]
        
        # Сохранение результата
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(result, f, ensure_ascii=False, indent=4, sort_keys=True)
        
        print(f"Успешно обработано {len(result)} преподавателей. Результат сохранен в {output_file}")
    
    except Exception as e:
        print(f"Ошибка: {str(e)}")

# Пример использования
if __name__ == "__main__":
    input_json = "demoSchedule.json"      # Ваш исходный файл с расписанием
    output_json = "demoTeachers.json"  # Файл для сохранения результата
    
    generate_teacher_emails(input_json, output_json)