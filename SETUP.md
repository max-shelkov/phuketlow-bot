# 🚀 Руководство по настройке PhuketLow Telegram Bot

## ✅ Процесс установки и запуска

### Шаг 1: Получение токена бота

1. Откройте Telegram и найдите **@BotFather** (https://t.me/BotFather)
2. Отправьте команду `/newbot`
3. Следуйте инструкциям:
   - Введите имя бота (например, `PhuketLow Bot`)
   - Введите уникальное имя пользователя (например, `phuketlow_bot`)
4. Вы получите сообщение с токеном вида: `123456789:ABCdefGHIjklMNOpqrsTUVwxyzABCDefGH`
5. **Скопируйте этот токен** - он вам понадобится

### Шаг 2: Настройка проекта

1. Откройте файл: `src/main/kotlin/com/phuketlow/bot/config/Config.kt`
2. Замените `YOUR_BOT_TOKEN_HERE` на ваш реальный токен:

```kotlin
object Config {
    const val TOKEN = "123456789:ABCdefGHIjklMNOpqrsTUVwxyzABCDefGH"
}
```

⚠️ **ВАЖНО:** Никогда не коммитьте этот файл в Git! Файл уже добавлен в `.gitignore` для безопасности.

### Шаг 3: Запуск бота

#### На Windows:
```bash
gradlew.bat run
```

#### На Linux/Mac:
```bash
./gradlew run
```

Вы должны увидеть вывод:
```
[main] INFO PhuketLowBot - Starting PhuketLow Telegram Bot...
[main] INFO PhuketLowBot - ✅ Bot connected! Username: @your_bot_name
[main] INFO PhuketLowBot - 🤖 PhuketLow Bot is running!
```

### Шаг 4: Тестирование

1. Откройте Telegram
2. Найдите вашего бота по имени (например, `@phuketlow_bot`)
3. Отправьте команду `/start`
4. Вы должны увидеть главное меню с кнопками

## 🔧 Требования

- **Java 17+** - Убедитесь, что у вас установлена Java 17 или новее
- **Git** (опционально) - для управления версиями
- **Интернет** - для подключения к Telegram API

## ❓ Решение проблем

### Ошибка: "ERROR: Bot token not configured!"
**Решение:** Убедитесь, что вы заменили `YOUR_BOT_TOKEN_HERE` на реальный токен в `Config.kt`

### Ошибка: "Connection refused" или "Network error"
**Решение:** Проверьте интернет-соединение и убедитесь, что Telegram API доступен в вашей стране

### Ошибка при сборке проекта
**Решение:** Выполните `gradlew.bat clean build` (на Windows) или `./gradlew clean build` (на Linux/Mac)

## 📚 Полезные ссылки

- [Telegram Bot API Documentation](https://core.telegram.org/bots/api)
- [BotFather](https://t.me/BotFather)
- [Kotlin Documentation](https://kotlinlang.org/docs/kdom.html)

## 💡 Советы

- Бот использует **polling** (опрос обновлений), это означает, что он активно запрашивает новые сообщения каждый раз
- Для более высокой производительности можно использовать **webhooks**, но это требует веб-сервера
- Токен бота должен быть конфиденциальным - не делитесь им!

---

**Готово!** Ваш бот должен быть запущен и готов к взаимодействию! 🎉

