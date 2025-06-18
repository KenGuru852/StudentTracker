import json

def extract_teachers(input_file, output_file):
    """
    Читает JSON-файл с расписанием, извлекает всех преподавателей
    и сохраняет их с единой почтой в новый JSON-файл.
    """
    try:
        # Чтение входного JSON-файла
        with open(input_file, 'r', encoding='utf-8') as f:
            schedule_data = json.load(f)
        
        # Сбор уникальных преподавателей
        teachers = set()
        for entry in schedule_data:
            if "ФизическоеЛицо" in entry:
                teacher_name = entry["ФизическоеЛицо"]
                if teacher_name:  # Проверка на пустое значение
                    teachers.add(teacher_name)
        
        # Сортировка преподавателей по алфавиту
        sorted_teachers = sorted(teachers)
        
        # Формирование списка преподавателей с почтой
        teachers_list = [
            {
                "full_name": teacher,
                "email": "studenttrackerteachertest@gmail.com",
            }
            for teacher in sorted_teachers
        ]
        
        # Сохранение в выходной JSON-файл
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(teachers_list, f, ensure_ascii=False, indent=2)
        
        print(f"Успешно обработано {len(teachers_list)} преподавателей")
        print(f"Результат сохранён в файл: {output_file}")
    
    except FileNotFoundError:
        print(f"Ошибка: файл {input_file} не найден")
    except json.JSONDecodeError:
        print(f"Ошибка: файл {input_file} не является валидным JSON")
    except Exception as e:
        print(f"Произошла ошибка: {str(e)}")

# Использование функции
input_json = "fullSchedule.json"  # Имя входного файла
output_json = "fullTeacher.json"  # Имя выходного файла
extract_teachers(input_json, output_json)