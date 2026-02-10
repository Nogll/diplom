# Bioactive Compounds Analysis System

**Дипломная работа:** Формирование многомерной матрицы взаимодействия биоактивных веществ растительного происхождения с различными рецепторами (с применением методов и технологий ИИ)

## Описание

Web-приложение для анализа научных статей и извлечения информации о биоактивных соединениях растительного происхождения с использованием AI моделей (Google Gemini или OpenAI). Включает AI-поиск в базе знаний и PubMed для формирования комплексных ответов на естественно-языковые запросы.

## Технологии

- **Backend**: Spring Boot 3.5.7, Kotlin
- **Database**: H2 (dev), PostgreSQL (production-ready)
- **ORM**: JPA/Hibernate
- **Migrations**: Liquibase
- **AI**: Google Gemini 2.5 Flash / OpenAI (переключается через конфигурацию)
- **Frontend**: HTML5, CSS3, JavaScript, Bootstrap 5
- **API Documentation**: OpenAPI/Swagger
- **HTML Parsing**: Jsoup (для интеграции с PubMed)
- **HTTP Client**: Spring RestClient
- **Markdown Rendering**: marked.js (для отображения summary в чате)
- **Scheduling**: Spring Scheduling (для асинхронной обработки чатов)

## Структура проекта

```
src/main/
├── kotlin/io/github/nogll/diplom/
│   ├── config/
│   │   └── MvcConfig.kt              # Конфигурация MVC и CORS
│   ├── controllers/
│   │   ├── ArticleController.kt      # REST API endpoints
│   │   ├── PubMedController.kt       # PubMed search endpoints
│   │   ├── ChatController.kt         # Chat frontend + API endpoints
│   │   └── FrontendController.kt     # Frontend routing
│   ├── dto/
│   │   ├── ProcessArticleRequest.kt
│   │   ├── ArticleDto.kt
│   │   ├── InteractionDto.kt
│   │   ├── InteractionResponse.kt
│   │   ├── ChatResponse.kt           # Chat API responses
│   │   ├── PubMedArticleDto.kt       # PubMed article data
│   │   └── PubMedSearchResponse.kt   # PubMed search results
│   ├── entity/
│   │   ├── Model.kt
│   │   ├── Plant.kt
│   │   ├── Compound.kt
│   │   ├── Article.kt
│   │   ├── Source.kt
│   │   ├── Interaction.kt            # Main entity
│   │   ├── Chat.kt                   # Chat entity with status machine
│   │   ├── PubMedQuery.kt            # PubMed queries linked to chats
│   │   └── ChatInteraction.kt        # Interactions linked to chats
│   ├── llm/
│   │   ├── UserQueryLLM.kt           # LLM interface for query analysis
│   │   ├── ArticleProcessingLLM.kt   # LLM interface for article extraction
│   │   ├── DbSearchLLM.kt            # LLM interface for DB search
│   │   ├── SummaryLLM.kt             # LLM interface for summary generation
│   │   └── GeminiService.kt          # Legacy Gemini service
│   ├── service/llmclient/
│   │   ├── gemini/                   # Gemini implementations
│   │   │   ├── GeminiUserQueryLLM.kt
│   │   │   ├── GeminiArticleProcessingLLM.kt
│   │   │   ├── GeminiDbSearchLLM.kt
│   │   │   └── GeminiSummaryLLM.kt
│   │   └── openai/                   # OpenAI implementations
│   │       ├── OpenAiUserQueryLLM.kt
│   │       ├── OpenAiArticleProcessingLLM.kt
│   │       ├── OpenAiDbSearchLLM.kt
│   │       └── OpenAiSummaryLLM.kt
│   ├── repository/                   # JPA repositories
│   │   ├── ModelRepository.kt
│   │   ├── PlantRepository.kt
│   │   ├── CompoundRepository.kt
│   │   ├── ArticleRepository.kt
│   │   ├── SourceRepository.kt
│   │   ├── InteractionRepository.kt
│   │   ├── ChatRepository.kt
│   │   ├── PubMedQueryRepository.kt
│   │   └── ChatInteractionRepository.kt
│   ├── service/
│   │   ├── ArticleProcessingService.kt # Business logic + CSV generation
│   │   ├── PubMedService.kt            # PubMed search and parsing
│   │   ├── ChatProcessingService.kt    # Chat pipeline orchestration
│   │   └── ChatWorker.kt               # Scheduled worker for async processing
│   └── DiplomApplication.kt           # Main application (with @EnableScheduling)
└── resources/
    ├── db/changelog/                 # Liquibase migrations
    ├── static/                       # Frontend files
    │   ├── index.html                # Main page
    │   ├── chat.html                 # AI chat interface
    │   ├── app.js                    # Main page JavaScript
    │   └── styles.css                # Custom styles
    └── application.yaml
```

## Возможности

### 1. Обработка статей
- Загрузка абстракта научной статьи
- Автоматическое извлечение информации с помощью AI
- Сохранение данных в базе данных

### 2. Просмотр данных
- Список всех статей с пагинацией
- Фильтрация взаимодействий по:
  - Названию растения
  - Названию соединения
  - Эффекту/механизму действия
- Просмотр исходных ответов AI для каждой статьи

### 3. Поиск в PubMed
- Поиск статей напрямую в базе данных PubMed
- Отображение результатов поиска с названиями и URL статей
- Ленивая загрузка абстрактов (по клику "Show Abstract")
- Загрузка статей напрямую из результатов поиска PubMed
- Пагинация результатов поиска

### 4. Экспорт в CSV
- Экспорт всех взаимодействий или отфильтрованного подмножества в CSV
- Одна строка на эффект (расширенный формат для анализа)
- Колонки: row, plant, compound, effect, article, model
- Учитывает текущие настройки фильтров
- Имена файлов с временной меткой
- Готово для анализа в pandas/Excel

### 5. AI Chat - Поиск в базе знаний и PubMed
- Естественно-языковые запросы пользователя
- Автоматическое формирование семантически релевантных PubMed запросов
- Поиск релевантных публикаций в PubMed
- Контекстуальный поиск во внутренней базе данных взаимодействий
- Генерация summary на основе найденных данных с ссылками на источники
- Асинхронная обработка через worker-based pipeline
- Отображение summary в формате Markdown
- Состояния обработки: анализ запроса → поиск PubMed → поиск в БД → генерация summary

### 6. API
- `POST /api/v1/articles/process` - обработка статьи
- `GET /api/v1/articles` - получить список статей (paginated)
- `GET /api/v1/articles/{id}/sources` - получить источники для статьи
- `GET /api/v1/sources/{id}` - получить конкретный источник с исходным ответом
- `GET /api/v1/interactions` - получить взаимодействия с фильтрами (paginated)
- `GET /api/v1/interactions/csv` - скачать взаимодействия в формате CSV
- `GET /api/v1/pubmed/search` - поиск в PubMed (query, page)
- `GET /api/v1/pubmed/article/abstract` - получить абстракт статьи по URL
- `POST /api/v1/pubmed/article/process` - обработать статью из PubMed
- `GET /chat` - создать новый чат и перенаправить
- `GET /chat/{id}` - открыть страницу чата
- `GET /api/v1/chat/{id}` - получить состояние чата (JSON)
- `POST /api/v1/chat/{id}/message` - отправить сообщение в чат

## Запуск

1. Установите API ключ для выбранной AI модели:
   - **OpenAI**: установите `OPENAI_API_KEY` в переменную окружения (по умолчанию используется OpenAI)
   - **Gemini**: установите Google API ключ или настройте аутентификацию Google Cloud
2. Настройте выбор модели в `application.yaml`:
   ```yaml
   llm:
     model: openai  # или gemini
     openai:
       base-url: https://api.artemox.com/v1
       api-key: ${OPENAI_API_KEY}
   ```
3. Соберите проект:
   ```bash
   ./gradlew build
   ```
4. Запустите приложение:
   ```bash
   ./gradlew bootRun
   ```
5. Откройте браузер: http://localhost:8080

## База данных

Приложение использует H2 file-based базу данных для разработки. Данные сохраняются в `./data/diplomdb`. Структура БД:

- **model** - AI модели для извлечения данных
- **article** - научные статьи
- **source** - связь статей с моделями + сохранение raw_response от AI
- **plant** - растения
- **compound** - биоактивные соединения
- **interactions** - взаимодействия (plant + compound + effects + parts + source)
- **chats** - чаты с пользователями (UUID ID, status, user_message, keywords, summary, timestamps, version для optimistic locking)
- **pubmed_queries** - PubMed запросы, связанные с чатами
- **chat_interactions** - связи между чатами и найденными взаимодействиями

Миграции Liquibase автоматически создают структуру БД при старте приложения. Данные сохраняются между перезапусками.

### Миграции
- `db.changelog-1.0.yaml` - основная схема БД (model, article, source, plant, compound, interactions)
- `db.changelog-2.0.yaml` - схема для чатов (chats, pubmed_queries, chat_interactions)

## Настройка для PostgreSQL

Для использования PostgreSQL измените `application.yaml`:

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/diplom
    username: your_user
    password: your_password
```

## Swagger UI

API документация доступна по адресу: http://localhost:8080/swagger-ui.html

## H2 Console

В режиме разработки доступна H2 Console: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/diplomdb`
- Username: `sa`
- Password: `password`

## Технические детали

### Transaction Management
- Все read-only операции помечены `@Transactional(readOnly = true)` в сервисном слое
- Write операции используют `@Transactional` без readOnly
- Transactions предотвращают проблемы с lazy-loading в JPA

### AI Integration
- **Поддержка двух LLM бэкендов**: Google Gemini и OpenAI (переключение через конфигурацию)
- **Модульная архитектура**: интерфейсы LLM для разных задач (UserQueryLLM, ArticleProcessingLLM, DbSearchLLM, SummaryLLM)
- **Условные бины**: `@ConditionalOnProperty` для автоматического выбора реализации
- **Структурированный вывод**: JSON schema для Gemini, structuredOutput для OpenAI
- **Сохранение raw ответов**: все ответы AI сохраняются в `Source.rawResponse` для отладки и аудита
- **Автоматическое создание/поиск**: существующих записей для растений и соединений

### AI Chat Pipeline
- **Асинхронная обработка**: worker-based pipeline через `@Scheduled` задачи
- **Машина состояний**: ChatStatus enum для отслеживания прогресса
- **Optimistic locking**: `@Version` в Chat entity для предотвращения конфликтов
- **Статусы**: NEW → USER_MESSAGE → USER_MESSAGE_PROCESS → SEARCH_PUBMED → SEARCH_DB → SUMMARY → COMPLETE
- **Ограничения**: максимум 3 статьи на запрос, максимум 15 статей всего
- **Логирование**: подробное логирование всех этапов обработки (DEBUG уровень для пакета `io.github.nogll.diplom`)

### PubMed Integration
- Использует Spring RestClient для получения HTML из PubMed
- Парсинг HTML с помощью библиотеки Jsoup
- Множественные стратегии селекторов для надежности
- Извлечение абстрактов с индивидуальных страниц статей

### CSV Export
- Стандартный формат CSV с правильным экранированием специальных символов
- Расширение: одна строка на эффект (если взаимодействие имеет 3 эффекта, создается 3 строки)
- Фильтрация: учитывает текущие параметры фильтров
- Колонки: row (последовательный номер), plant, compound, effect, article (URL), model
- Имена файлов с временной меткой
- Предназначено для анализа в pandas/Excel

### Chat Frontend
- **Markdown rendering**: summary отображается как отформатированный Markdown документ (библиотека marked.js)
- **Отдельный скролл**: summary в отдельном контейнере с ограниченной высотой
- **Копирование**: кнопка "Copy Markdown" для копирования исходного Markdown текста
- **Динамическое обновление**: опрос состояния чата через API каждые 2 секунды
- **Статусы**: визуальные индикаторы для разных состояний обработки
- **Отображение результатов**: queries, interactions и summary для завершенных чатов

## Формат входных данных

При обработке статьи AI извлекает следующую информацию:

```json
[
  {
    "plant": "Curcuma longa",
    "compound": "curcumin",
    "effects": ["anti-inflammatory", "antioxidant", "inhibits COX-2"],
    "part": ["root"]
  }
]
```

## Формат CSV экспорта

Экспортированный CSV файл содержит следующие колонки:

```csv
row,plant,compound,effect,article,model
1,Curcuma longa,curcumin,anti-inflammatory,https://example.com/article,gemini-2.5-flash
2,Curcuma longa,curcumin,antioxidant,https://example.com/article,gemini-2.5-flash
3,Garlic,allicin,antimicrobial,https://example.com/article2,gemini-2.5-flash
```

Каждый эффект создает отдельную строку, что удобно для анализа в pandas или Excel.

## Лицензия

Этот проект является частью дипломной работы.

