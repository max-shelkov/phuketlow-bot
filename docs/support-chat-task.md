# Задача: Чат с ИИ-поддержкой в Telegram-боте PhuketLow

> **Назначение этого файла:** это промт-документ. Если сессия прервалась или что-то пошло не так,
> передайте этот файл новому агенту — в нём вся необходимая информация для продолжения работы.

---

## 1. Цель задачи

Добавить в Telegram-бот **чат с поддержкой**: пользователь задаёт вопрос текстом,
LLM (Qwen-3.8 27B на собственном сервере) формирует ответ, бот отправляет его пользователю.

### Требования к UX
- Вход в режим поддержки — кнопка «💬 Поддержка» в главном меню.
- В режиме поддержки **любой текст** пользователя уходит в LLM (кроме команд выхода: `/start`, `/menu`, «Меню»).
- Выход из режима — «Меню»/`/menu`/кнопка «⬅️ Меню» → главное меню, история диалога сбрасывается.
- Пока LLM думает — индикатор «печатает…» (`sendChatAction(chatId, "typing")`).
- При ошибке LLM API — вежливое сообщение об ошибке + кнопка возврата в меню, режим поддержки не ломается.
- История диалога: последние ~20 сообщений на чат, TTL 30 минут неактивности → сброс.

### База знаний (важно!)
- У бота есть **файл с информацией** (база знаний, например `src/main/knowledge/support_knowledge.md`),
  которую разрешено предоставлять пользователям.
- **Все ответы ИИ формируются ИСКЛЮЧИТЕЛЬНО на основе данных из этого файла** (system prompt обязывает
  не выдумывать и не использовать «внешние» знания).
- Если нужной информации в файле **нет** — ИИ вежливо говорит, что не может ответить на данный вопрос.
- Такой вопрос **записывается в отдельный файл `unanswered_questions`** (например
  `data/unanswered_questions.log`): timestamp + chat_id + текст вопроса. Файл — для владельца бота,
  чтобы пополнять базу знаний.
- Механика определения «нет информации»: в system prompt просим модель, если ответа нет в базе знаний,
  отвечать строго специальной фразой-маркером (например `[[NO_ANSWER]]`); бот детектит маркер в ответе,
  отправляет пользователю вежливый отказ и пишет вопрос в `unanswered_questions`.

---

## 2. Текущее состояние проекта

### Стек
- **Kotlin 2.2.0, JVM 17**, Gradle (`build.gradle.kts`), сборка: `gradlew.bat installDist` / `run`
- **OkHttp 4.11** — HTTP-клиент (Telegram API и, по плану, LLM API)
- **Gson 2.10.1** — JSON
- **SLF4J (slf4j-simple)** — логирование
- **Apache POI 5.2.5** — чтение `real_estate.xlsx`
- **НЕТ** фреймворков (Ktor/Spring) — всё на прямых HTTP-вызовах, long polling

### Архитектура (как работает сейчас)
```
Main.kt (main)
  ├─ проверка TELEGRAM_BOT_TOKEN, getMe()
  ├─ VillaStore.init() — читает src/main/xlsx/real_estate.xlsx (лист rental) в память
  └─ цикл long polling: getUpdates(offset, timeout=30)
        ├─ message        → MessageHandler.handle(message)
        └─ callback_query → CallbackHandler.handle(callback)
```

- **`config/Config.kt`** — `object Config`, читает `System.getenv(...)`.
  ⚠️ Файл в `.gitignore`! В репозитории есть `Config.kt.template`.
  Сейчас: `TOKEN` из `TELEGRAM_BOT_TOKEN`.
- **`telegram/TelegramClient.kt`** — `object`, единая точка доступа к Bot API:
  `sendRequest(method, body)`, `sendMessage(chatId, text, parseMode="Markdown")`,
  `sendMessageWithMarkup`, `sendMediaGroup`, `sendChatAction`, `answerCallbackQuery`, `setChatMenuButton`.
  ⚠️ `sendMessage` по умолчанию использует **parse_mode Markdown** — текст с `*`, `_`, `[` может
  сломать отправку (Telegram вернёт 400). Ответы LLM лучше отправлять с `parseMode = "HTML"`
  или без parse_mode (plain text) — **решить при реализации**.
- **`telegram/Keyboard.kt`** — `Keyboard.inline(buttons: List<Pair<текст, callback_data>>, withMenuButton=true)`.
  Кнопка «⬅️ Меню» (data=`menu`) добавляется автоматически, если `withMenuButton=true`.
- **`handlers/MessageHandler.kt`** — сейчас: `/start`|`/menu`|«Меню» → `MainMenu.show(chatId)`,
  всё остальное → «Я понимаю только команды». **Здесь появится роутинг в режим поддержки.**
- **`handlers/CallbackHandler.kt`** — `menu` → MainMenu; `villa_*` → карточка виллы;
  `real_estate*` → `RealEstate.handleCallback`; остальное → заглушки (excursions, cars, yachts, currency).
- **`sections/MainMenu.kt`** — главное меню, 5 кнопок (real_estate, excursions, cars, yachts, currency).
  **Здесь добавить кнопку «💬 Поддержка» → callback `support`.**
- **`sections/RealEstate.kt`** — пример «секции»: `handleCallback(data, chatId): SectionResponse`
  и `showVillaCard` (альбом фото по URL с GitHub raw: `max-shelkov/phuketlow-bot-photos`).
- **`sections/SectionResponse.kt`** — `data class SectionResponse(text, keyboard)`.
- **`data/VillaStore.kt`** — xlsx → `List<Villa>` в памяти; фото по публичным URL.
- **`model/Villa.kt`** — id, name, category, bedrooms, area, description, photos.

### Деплой
- VDS (Ubuntu/Debian), `deploy/deploy.sh` + systemd `deploy/bot.service`.
- Приложение в `/opt/phuketlow-bot`, секреты в `/opt/phuketlow-bot/.env`
  (`EnvironmentFile=`), запуск `bin/phuketlow-telegram-bot`.
- **Новые переменные окружения (AI_API_KEY и т.д.) нужно добавить в `.env` на сервере.**

### Git
- Репозиторий: `max-shelkov/phuketlow-bot`, ветка `main`.
- `.gitignore` содержит: `Config.kt`, `config/*.json` (API-ключи Google Drive), `.env`, build-артефакты.
- **Никогда не коммитить секреты** (токены, API-ключи, `phulet-low-googledisk-api.json`).

---

## 3. LLM: подключение

- **Модель:** Qwen-3.8 27B (reasoning-модель, в API зарегистрирована как `qwen3.8-27b`;
  альтернативный id `qwen3.8:27b` тоже работает)
- **Хостинг:** собственный сервер, доступ через **new-api** (универсальный AI-шлюз,
  OpenAI-совместимый API). Веб-интерфейс new-api: `https://ai.dshelkov.com`
- **Base URL:** `https://ai.dshelkov.com`
- **Эндпоинт:** `POST https://ai.dshelkov.com/v1/chat/completions`
- **Формат запроса:** стандартный OpenAI chat completions:
  ```json
  {
    "model": "qwen3.8-27b",
    "messages": [
      {"role": "system", "content": "..."},
      {"role": "user", "content": "..."},
      {"role": "assistant", "content": "..."}
    ]
  }
  ```
- **Авторизация:** `Authorization: Bearer <AI_API_KEY>` (ключ выдаётся в панели new-api).
- **Ответ:** `choices[0].message.content` — текст ответа.
  ⚠️ Модель reasoning-ная: в ответе есть поле `reasoning_content` (внутренние размышления) —
  его **не использовать**, только `content`.
- **Ограничения:** модель локальная, может отвечать медленно (read timeout ≥ 120 c),
  возможны 429/5xx при нагрузке — нужна обработка ошибок.
- **Проверено (2026-11-20):** `POST /v1/chat/completions` с `qwen3.8-27b` → HTTP 200, ответ получен.
  ⚠️ На Windows PowerShell 5.1 `Set-Content -Encoding UTF8` добавляет BOM — new-api его не
  принимает (400 invalid JSON). Для curl-тестов писать JSON без BOM
  (`[System.IO.File]::WriteAllText` + `UTF8Encoding $false`).

### Переменные окружения (план)
| Переменная | По умолчанию | Описание |
|---|---|---|
| `AI_API_KEY` | — (обязательна) | Ключ new-api |
| `AI_BASE_URL` | `https://ai.dshelkov.com` | Base URL new-api |
| `AI_MODEL` | `qwen3.8-27b` | Имя модели (проверено через `GET /v1/models`) |

---

## 4. План реализации (архитектура)

```
Пользователь → MessageHandler
                 ├─ режим поддержки НЕ активен:
                 │    /start, /menu, «Меню» → MainMenu.show
                 │    остальное → «Я понимаю только команды» (как сейчас)
                 └─ режим поддержки АКТИВЕН:
                      /start, /menu, «Меню» → выход из режима + MainMenu.show
                      остальное → Support.ask(chatId, text)
                                     ├─ ConversationStore.add(user)
                                     ├─ TelegramClient.sendChatAction(typing)
                                     ├─ AiClient.chat([system] + история + [user])
                                     ├─ ConversationStore.add(assistant)
                                     └─ TelegramClient.sendMessage(ответ)
```

### Новые файлы
1. **`ai/AiClient.kt`** — `object AiClient`:
   - `data class ChatMessage(role, content)`
   - `fun chat(messages: List<ChatMessage>): String` — POST `/v1/chat/completions` на OkHttp
     (отдельный OkHttpClient: connect 15 c, read 120 c), заголовок `Authorization: Bearer`.
   - `class AiException(message)` — при ошибке сети/API/пустом ответе.
2. **`state/ConversationStore.kt`** — `object`:
   - `ConcurrentHashMap<Long, Conversation>` (chatId → история + lastActiveAt)
   - `add(chatId, message)`, `history(chatId): List<ChatMessage>`, `clear(chatId)`, `cleanup()`
   - MAX_MESSAGES = 20, TTL = 30 мин. `cleanup()` вызывать периодически (например, в цикле polling в Main.kt раз в N итераций).
3. **`state/SupportMode.kt`** (или внутри `Support.kt`) — `ConcurrentHashMap<Long, Boolean>`:
   чаты, находящиеся в режиме поддержки. `enter(chatId)`, `exit(chatId)`, `isActive(chatId)`.
4. **`src/main/knowledge/support_knowledge.md`** — **база знаний**: информация о сервисах PhuketLow
   (виллы, экскурсии, авто, яхты, валюта, контакты, цены — что разрешено сообщать).
   Читается один раз при старте (`KnowledgeBase.load()`), подставляется в system prompt.
   ⚠️ Файл должен попасть в git (это контент, не секрет) и копироваться при деплое (как xlsx).
5. **`data/UnansweredQuestions.kt`** (или внутри `Support.kt`) — запись в `data/unanswered_questions.log`:
   `ISO-timestamp | chat_id | вопрос`. Аппенд-режим, синхронизированный доступ.
   ⚠️ Каталог `data/` — в `.gitignore` (рабочие данные сервера).
6. **`sections/Support.kt`** — `object Support`:
   - `SYSTEM_PROMPT` — ассистент PhuketLow; **отвечать ТОЛЬКО на основе базы знаний** (база вставляется
     в промпт); если ответа в базе нет — ответить строго маркером `[[NO_ANSWER]]`
   - `enter(chatId)` — приветствие + клавиатура (кнопка «⬅️ Меню»)
   - `ask(chatId, text)` — история → typing → AiClient → ответ;
     если ответ содержит `[[NO_ANSWER]]` → вежливый отказ пользователю + `UnansweredQuestions.log(chatId, text)`;
     try/catch `AiException` → сообщение об ошибке
   - `exit(chatId)` — `ConversationStore.clear(chatId)` + сброс режима

### Изменения в существующих файлах
- **`config/Config.kt`** — добавить `AI_API_KEY`, `AI_BASE_URL`, `AI_MODEL` (см. таблицу выше).
  ⚠️ Не забыть обновить `Config.kt.template` (он в git, а Config.kt — нет).
- **`sections/MainMenu.kt`** — добавить кнопку `"💬 Поддержка" to "support"` в список кнопок.
- **`handlers/CallbackHandler.kt`** — ветка `data == "support"` → `Support.enter(chatId)`.
- **`handlers/MessageHandler.kt`** — роутинг: если `SupportMode.isActive(chatId)` и текст не команда
  выхода → `Support.ask(chatId, text)`; команды выхода → `Support.exit(chatId)` + `MainMenu.show(chatId)`.
- **`Main.kt`** — при старте загрузить базу знаний (`KnowledgeBase.load()`);
  периодический `ConversationStore.cleanup()` в цикле polling.
- **`.gitignore`** — добавить `data/` (рабочие файлы: unanswered_questions.log).
- **`deploy/deploy.sh`** — копировать `src/main/knowledge/` в `/opt/phuketlow-bot` (как xlsx).

### Порядок работы
1. `Config.kt` + `Config.kt.template` — переменные окружения
2. `ai/AiClient.kt`
3. `state/ConversationStore.kt` + режим поддержки
4. `src/main/knowledge/support_knowledge.md` — наполнить базу знаний (с владельцем)
5. `sections/Support.kt` (system prompt с базой знаний, маркер `[[NO_ANSWER]]`, лог unanswered)
6. Кнопка в `MainMenu.kt`, ветки в `CallbackHandler.kt` и `MessageHandler.kt`
7. Сборка: `gradlew.bat build` (проверить компиляцию)
8. Ручной тест: запустить бота с `TELEGRAM_BOT_TOKEN` и `AI_API_KEY`, задать вопрос через Telegram
   (в т.ч. вопрос, которого нет в базе знаний → проверить отказ + запись в log)
9. Деплой: добавить `AI_API_KEY` в `.env` на сервере, `deploy.sh`

### Проверка LLM API до/во время разработки
```bash
curl -s https://ai.dshelkov.com/v1/chat/completions \
  -H "Authorization: Bearer $AI_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"Qwen-3.8 27B","messages":[{"role":"user","content":"Привет, кто ты?"}]}'
```

---

## 5. Статус работы (обновлять по ходу)

- [x] Изучен код проекта, согласована архитектура с владельцем
- [x] Требование: ответы ТОЛЬКО из файла базы знаний; неизвестные вопросы → вежливый отказ + лог в `unanswered_questions`
- [x] Текущая версия запушена в `main` (коммит `88f3adc`)
- [x] Написаны `ai/AiClient.kt`, `state/ConversationStore.kt`, `state/SupportMode.kt` — **на диске, компиляция успешна**
- [x] `Main.kt` — периодический `ConversationStore.cleanup()` (каждые 5 минут)
- [x] `src/main/knowledge/support_knowledge.md` — тестовая база знаний создана (заменить реальными данными)
- [x] `sections/Support.kt` + режим поддержки + лог unanswered_questions
- [x] `TelegramClient.sendMessagePlain` — ответы LLM без parse_mode
- [x] Кнопка «Поддержка» в MainMenu, роутинг в MessageHandler/CallbackHandler
- [ ] `Config.kt` — AI-переменные (уже добавлены в локальный Config.kt) + `Config.kt.template`
- [ ] `.gitignore` — `data/`; `deploy.sh` — копирование knowledge/
- [ ] Сборка `gradlew.bat build`
- [ ] Ручной тест в Telegram (в т.ч. сценарий «нет в базе знаний»)
- [ ] Деплой (`.env` на сервере)

### Замечания / открытые вопросы
- **parse_mode:** `TelegramClient.sendMessage` по умолчанию шлёт Markdown. Ответы LLM могут
  содержать `*`, `_`, `[` → риск ошибки 400 от Telegram. Решение: для ответов поддержки
  отправлять с `parseMode = "HTML"` (и просить LLM не использовать markdown) или plain text.
- **Имя модели:** проверено через `GET /v1/models` — корректный id `qwen3.8-27b`
  (вариант `Qwen-3.8 27B` с пробелом new-api не находит: 503 model_not_found).
  Альтернативный id `qwen3.8:27b` тоже доступен.
- **AI_API_KEY** — у владельца есть ключ new-api; в код/репозиторий его не вставлять, только `.env`.
