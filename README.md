# PhuketLow Telegram Bot

Telegram-бот на Kotlin для помощи туристам на Пхукете.

## 📋 Функционал

Основное меню бота включает следующие разделы:
- 🏠 **Real Estate** — аренда и покупка недвижимости
- 🎭 **Excursions** — популярные экскурсии на Пхукете
- 🚗 **Car Rental** — аренда транспорта и трансферы
- 🛥️ **Yachts** — аренда яхт и морские прогулки
- 💱 **Currency Exchange** — курсы валют

## 🚀 Быстрый старт

### Требования
- Java 17 или выше
- Gradle 8.x (автоматически через gradlew)

### Установка

1. Клонируйте репозиторий:
```bash
git clone <repository-url>
cd phuketlow_bot/kotlin-bot
```

2. Получите токен бота у [BotFather](https://t.me/BotFather) в Telegram

3. Отредактируйте файл `src/main/kotlin/com/phuketlow/bot/config/Config.kt`:
```kotlin
const val TOKEN = "YOUR_TELEGRAM_BOT_TOKEN"
```

4. Запустите бота:
```bash
# На Windows:
gradlew.bat run

# На Linux/Mac:
./gradlew run
```

## 📁 Структура проекта

```
kotlin-bot/
├── build.gradle.kts                                    # Конфигурация Gradle
├── settings.gradle.kts                                 # Настройки проекта
├── .gitignore                                          # Игнорируемые файлы
├── README.md                                           # Документация
├── gradlew.bat                                         # Gradle wrapper (Windows)
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
└── src/main/kotlin/com/phuketlow/bot/
    ├── Main.kt                                         # Точка входа и логика бота
    └── config/
        └── Config.kt                                   # Конфигурация (токен)
```

## 🔧 Технологии

- **Kotlin** — основной язык разработки
- **OkHttp** — HTTP клиент для работы с Telegram Bot API
- **Gson** — JSON обработка
- **SLF4J** — логирование
- **Gradle** — система сборки

## 🐛 Исправленные проблемы

✅ Добавлен файл `gradlew.bat` для запуска на Windows  
✅ Заменена недоступная зависимость на доступные (OkHttp + Gson)  
✅ Переписана логика бота для использования Telegram Bot API напрямую  
✅ Добавлена JitPack репозитория для поддержки GitHub зависимостей  
✅ Исправлена ошибка логирования в Main.kt  

## 📚 Документация

- [Telegram Bot API](https://core.telegram.org/bots/api)
- [OkHttp Documentation](https://square.github.io/okhttp/)
- [Gson Documentation](https://github.com/google/gson)

## 📝 Лицензия

MIT
