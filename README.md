# Bioactive Compounds Analysis System

**Дипломная работа:** Формирование многомерной матрицы взаимодействия биоактивных веществ растительного происхождения с различными рецепторами (с применением методов и технологий ИИ)

## Описание

Web-приложение для анализа научных статей и извлечения информации о биоактивных соединениях растительного происхождения с использованием AI модели Google Gemini.

## Технологии

- **Backend**: Spring Boot 3.5.7, Kotlin
- **Database**: H2 (dev), PostgreSQL (production-ready)
- **ORM**: JPA/Hibernate
- **Migrations**: Liquibase
- **AI**: Google Gemini 2.5 Flash
- **Frontend**: HTML5, CSS3, JavaScript, Bootstrap 5
- **API Documentation**: OpenAPI/Swagger

## Структура проекта

```
src/main/
├── kotlin/io/github/nogll/diplom/
│   ├── config/
│   │   └── MvcConfig.kt              # Конфигурация MVC и CORS
│   ├── controllers/
│   │   ├── ArticleController.kt      # REST API endpoints
│   │   └── FrontendController.kt     # Frontend routing
│   ├── dto/
│   │   ├── ProcessArticleRequest.kt
│   │   ├── ArticleDto.kt
│   │   ├── InteractionDto.kt
│   │   └── InteractionResponse.kt
│   ├── entity/
│   │   ├── Model.kt
│   │   ├── Plant.kt
│   │   ├── Compound.kt
│   │   ├── Article.kt
│   │   ├── Source.kt
│   │   └── Interaction.kt            # Main entity
│   ├── llm/
│   │   └── GeminiService.kt          # AI service
│   ├── repository/                   # JPA repositories
│   ├── service/
│   │   └── ArticleProcessingService.kt
│   └── DiplomApplication.kt
└── resources/
    ├── db/changelog/                 # Liquibase migrations
    ├── static/                       # Frontend files
    │   ├── index.html
    │   ├── app.js
    │   └── styles.css
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

### 3. API
- `POST /api/v1/articles/process` - обработка статьи
- `GET /api/v1/articles` - получить список статей
- `GET /api/v1/interactions` - получить взаимодействия с фильтрами

## Запуск

1. Установите Google API ключ в переменную окружения или настройте аутентификацию Google Cloud
2. Соберите проект:
   ```bash
   ./gradlew build
   ```
3. Запустите приложение:
   ```bash
   ./gradlew bootRun
   ```
4. Откройте браузер: http://localhost:8080

## База данных

Приложение использует H2 file-based базу данных для разработки. Данные сохраняются в `./data/diplomdb`. Структура БД:

- **model** - AI модели для извлечения данных
- **article** - научные статьи
- **source** - связь статей с моделями
- **plant** - растения
- **compound** - биоактивные соединения
- **interactions** - взаимодействия (plant + compound + effects + parts + source)

Миграции Liquibase автоматически создают структуру БД при старте приложения. Данные сохраняются между перезапусками.

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
- Используется Google Gemini 2.5 Flash модель
- Структурированный вывод в формате JSON
- Автоматическое создание/найск существующих записей для растений и соединений

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

## Лицензия

Этот проект является частью дипломной работы.

