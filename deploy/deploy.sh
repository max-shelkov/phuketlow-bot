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

echo "==> 2. Config.kt (gitignored) — копирую из защищённой папки .config/"
CONFIG_SRC=".config/Config.kt"
CONFIG_DST="src/main/kotlin/com/phuketlow/bot/config/Config.kt"
if [ -f "$CONFIG_SRC" ]; then
    cp "$CONFIG_SRC" "$CONFIG_DST"
    echo "    OK: $CONFIG_SRC -> $CONFIG_DST"
else
    echo "ВНИМАНИЕ: $CONFIG_SRC не найден. Создайте папку .config/ и положите туда Config.kt"
    echo "    (см. Config.kt.template). Сборка может упасть с Unresolved reference."
fi

echo "==> 3. Сборка приложения"
./gradlew installDist --no-daemon

echo "==> 4. Копирование в ${APP_DIR}"
sudo mkdir -p "${APP_DIR}"
sudo cp -r build/install/phuketlow-telegram-bot/. "${APP_DIR}/"
# Данные (xlsx) бот ищет относительно рабочей директории
sudo mkdir -p "${APP_DIR}/src/main/xlsx"
sudo cp src/main/xlsx/*.xlsx "${APP_DIR}/src/main/xlsx/" 2>/dev/null || true
# База знаний ИИ-поддержки
sudo mkdir -p "${APP_DIR}/src/main/knowledge"
sudo cp src/main/knowledge/*.md "${APP_DIR}/src/main/knowledge/" 2>/dev/null || true
# Каталог для runtime-данных (unanswered_questions.log)
sudo mkdir -p "${APP_DIR}/data"
sudo chown -R "${SERVICE_USER}:" "${APP_DIR}"

echo "==> 5. Файл окружения с токеном"
if [ -f .env ]; then
    sudo cp .env "${APP_DIR}/.env"
    sudo chown "${SERVICE_USER}:" "${APP_DIR}/.env"
    sudo chmod 600 "${APP_DIR}/.env"
else
    echo "ВНИМАНИЕ: .env не найден. Создайте ${APP_DIR}/.env с строкой TELEGRAM_BOT_TOKEN=..."
fi

echo "==> 6. Настройка systemd-сервиса"
sudo cp deploy/bot.service /etc/systemd/system/${APP_NAME}.service
sudo systemctl daemon-reload
sudo systemctl enable ${APP_NAME}
sudo systemctl restart ${APP_NAME}

echo "==> Готово! Статус:"
systemctl status ${APP_NAME} --no-pager
echo
echo "Логи: journalctl -u ${APP_NAME} -f"
