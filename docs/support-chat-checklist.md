# Чек-лист: реализация чата с ИИ-поддержкой

> План работ по задаче из [support-chat-task.md](./support-chat-task.md).
> Отмечайте `[x]` по мере выполнения. Дата начала: 2026-11-20.

## 1. Подготовка

- [x] Изучить код проекта и согласовать архитектуру
- [x] Запушить текущую версию в `main` (коммит `88f3adc`)
- [x] Написать промт-документ `docs/support-chat-task.md`
- [x] Получить/проверить `AI_API_KEY` (ключ new-api) — добавлен в `.env`
- [x] Проверить доступность LLM API — **HTTP 200, ответ получен** (2026-11-20)
- [x] Проверить точное имя модели — `qwen3.8-27b` (через `GET /v1/models`);
  `Qwen-3.8 27B` с пробелом не работает (503 model_not_found)

## 2. Конфигурация

- [x] `Config.kt` — добавить `AI_API_KEY`, `AI_BASE_URL` (default `https://ai.dshelkov.com`), `AI_MODEL` (default `qwen3.8-27b`)
- [x] `Config.kt.template` — синхронизирован с `Config.kt` (AI-переменные + описание env)

## 3. LLM-клиент

- [x] `ai/AiClient.kt` — `chat(messages): String`, POST `/v1/chat/completions`, Bearer-авторизация, read timeout 120 c, `AiException`
- [x] Проверить компиляцию `AiClient.kt` — `gradlew clean compileKotlin` → BUILD SUCCESSFUL (2026-11-20)

## 4. Состояние диалога

- [x] `state/ConversationStore.kt` — история на чат: `add`, `history`, `clear`, `cleanup`; MAX_MESSAGES=20, TTL=30 мин
- [x] `state/SupportMode.kt` — `ConcurrentHashMap<Long, Boolean>`: `enter`, `exit`, `isActive`
- [x] `Main.kt` — периодический вызов `ConversationStore.cleanup()` в цикле polling (каждые 5 минут)
- [x] Все три файла подтверждены на диске, `gradlew clean compileKotlin` → BUILD SUCCESSFUL

## 5. База знаний и лог неизвестных вопросов

- [x] `src/main/knowledge/support_knowledge.md` — создана **тестовая** база знаний
  (вопрос → ответ; цены/контакты помечены «ТЕСТОВЫЕ ДАННЫЕ» — заменить реальными)
- [x] `knowledge/KnowledgeBase.kt` — `load()` при старте: читает файл в память (2 кандидата пути: локальный и деплой)
- [x] `Main.kt` — вызов `KnowledgeBase.load()` при инициализации
- [x] `.gitignore` — добавлен `data/` (рабочие файлы сервера)
- [x] `deploy/deploy.sh` — копирование `src/main/knowledge/` в `/opt/phuketlow-bot` + создание `data/`
- [x] `gradlew clean compileKotlin` → BUILD SUCCESSFUL

## 6. Секция поддержки

- [x] `sections/Support.kt`:
  - [x] `SYSTEM_PROMPT` — ассистент PhuketLow; отвечать **ТОЛЬКО на основе базы знаний**
    (база вставляется в промпт); если ответа в базе нет — ответить строго маркером `[[NO_ANSWER]]`
  - [x] `enter(chatId)` — приветствие + клавиатура с «⬅️ Меню»
  - [x] `ask(chatId, text)` — история → typing → AiClient → ответ;
    - [x] ответ содержит `[[NO_ANSWER]]` → вежливый отказ + запись в `data/unanswered_questions.log`
      (timestamp | chat_id | вопрос)
    - [x] try/catch `AiException` → вежливая ошибка + кнопка меню
  - [x] `exit(chatId)` — сброс истории и режима
- [x] Решить вопрос parse_mode — ответы LLM отправляются **без parse_mode**
  (новый `TelegramClient.sendMessagePlain`), служебные сообщения — HTML

## 7. Интеграция в бота

- [x] `MainMenu.kt` — кнопка «💬 Поддержка» → callback `support`
- [x] `CallbackHandler.kt` — ветка `data == "support"` → `Support.enter(chatId)`
- [x] `MessageHandler.kt` — роутинг:
  - [x] режим активен + текст не команда выхода → `Support.ask(chatId, text)`
  - [x] `/start`, `/menu`, «Меню» → `Support.exit(chatId)` + `MainMenu.show(chatId)`
- [x] `gradlew clean compileKotlin` → BUILD SUCCESSFUL

## 8. Тестирование

- [x] `gradlew.bat build` — успешная сборка
- [x] Запустить локально (`TELEGRAM_BOT_TOKEN` + `AI_API_KEY` в окружении)
- [x] Сценарий: меню → «Поддержка» → вопрос (есть в базе знаний) → корректный ответ
- [x] Сценарий: вопрос, **нет** в базе знаний → вежливый отказ + запись в `data/unanswered_questions.log`
  (проверено: «Как добраться до Бангкока?», «За сколько времени надо бронировать яхту?» — записаны, UTF-8 корректный)
- [x] Сценарий: несколько вопросов подряд (история диалога работает)
- [x] Сценарий: «Меню» из поддержки → выход, история сброшена
- [x] Сценарий: обычный режим (без поддержки) — старые сценарии не сломаны (виллы, меню)
- [ ] Сценарий: ошибка LLM API (неверный ключ/таймаут) → вежливое сообщение, бот не падает
  (логика есть в коде: try/catch AiException; не проверялся на живом сбое)

## 9. Деплой

- [ ] Добавить `AI_API_KEY` (и при необходимости `AI_BASE_URL`, `AI_MODEL`) в `.env` на сервере
- [ ] `bash deploy/deploy.sh` на VDS (проверить, что `knowledge/` скопировался)
- [ ] Проверить `systemctl status phuketlow-bot` и логи `journalctl -u phuketlow-bot -f`
- [ ] Прогнать сценарий из Telegram на боевом боте (в т.ч. «нет в базе знаний»)
- [ ] Запушить финальную версию в `main`
