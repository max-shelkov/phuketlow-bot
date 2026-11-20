#!/usr/bin/env bash
# Деплой PhuketLow Telegram Bot на VDS (Ubuntu/Debian)
# Запуск: bash deploy.sh  (из корня репозитория, на сервере)
set -euo pipefail

APP_NAME="phuketlow-bot"
APP_DIR="/opt/${APP_NAME}"
SERVICE_USER="${SUDO_USER:-root}"

echo "==> 1. Установка JDK 17 и git (если нет)"
if ! command -v java >/dev/null 2>&1; then
    sudo apt-get update
    sudo apt-get install -y openjdk-17-jdk-headless git
fi
java -version

echo "==> 2. Сборка приложения"
./gradlew installDist --no-daemon

echo "==> 3. Копирование в ${APP_DIR}"
sudo mkdir -p "${APP_DIR}"
sudo cp -r build/install/phuketlow-telegram-bot/. "${APP_DIR}/"
# Данные (xlsx) бот ищет относительно рабочей директории
sudo mkdir -p "${APP_DIR}/src/main/xlsx"
sudo cp src/main/xlsx/*.xlsx "${APP_DIR}/src/main/xlsx/" 2>/dev/null || true
sudo chown -R "${SERVICE_USER}:" "${APP_DIR}"

echo "==> 4. Файл окружения с токеном"
if [ -f .env ]; then
    sudo cp .env "${APP_DIR}/.env"
    sudo chown "${SERVICE_USER}:" "${APP_DIR}/.env"
    sudo chmod 600 "${APP_DIR}/.env"
else
    echo "ВНИМАНИЕ: .env не найден. Создайте ${APP_DIR}/.env с строкой TELEGRAM_BOT_TOKEN=..."
fi

echo "==> 5. Настройка systemd-сервиса"
sudo cp deploy/bot.service /etc/systemd/system/${APP_NAME}.service
sudo systemctl daemon-reload
sudo systemctl enable ${APP_NAME}
sudo systemctl restart ${APP_NAME}

echo "==> Готово! Статус:"
systemctl status ${APP_NAME} --no-pager
echo
echo "Логи: journalctl -u ${APP_NAME} -f"
