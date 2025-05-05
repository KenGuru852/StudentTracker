FROM node:18-alpine

WORKDIR /app

# Установка зависимостей
COPY package*.json ./
RUN npm install

# Копирование остальных файлов
COPY . .

EXPOSE 3000

CMD ["npm", "run", "dev"]