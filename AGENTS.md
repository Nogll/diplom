# Agents Guide

## Project Overview

**Title**: Дипломная работа - Формирование многомерной матрицы взаимодействия биоактивных веществ растительного происхождения с различными рецепторами (с применением методов и технологий ИИ)

**Purpose**: Web application for analyzing scientific articles and extracting information about plant-derived bioactive compounds using AI (Google Gemini or OpenAI). Includes AI-powered search in knowledge base and PubMed for natural language queries.

## Tech Stack

### Backend
- **Language**: Kotlin (1.9.25)
- **Framework**: Spring Boot 3.5.7
- **Java Version**: 21
- **Database**: H2 (file-based, development) → PostgreSQL (production-ready)
- **ORM**: JPA/Hibernate
- **Migrations**: Liquibase
- **AI**: Google Gemini SDK (com.google.genai:2.0.0) / OpenAI Java Client (com.openai:openai-java:4.13.0)
- **API Docs**: SpringDoc OpenAPI/Swagger
- **HTML Parsing**: Jsoup (1.17.2) for PubMed integration
- **HTTP Client**: Spring RestClient for external API calls
- **Markdown Rendering**: marked.js (12.0.0) for chat summary display
- **Scheduling**: Spring Scheduling for async chat processing

### Frontend
- HTML5, CSS3, JavaScript (vanilla, no frameworks)
- Bootstrap 5 (via CDN)
- No build tools (pure static files)

### Build System
- Gradle (Kotlin DSL)
- Location: `build.gradle.kts`

## Project Structure

```
src/main/
├── kotlin/io/github/nogll/diplom/
│   ├── DiplomApplication.kt           # Main Spring Boot application
│   ├── config/
│   │   └── MvcConfig.kt               # MVC configuration, CORS setup
│   ├── controllers/
│   │   ├── ArticleController.kt       # REST API (/api/v1/*)
│   │   ├── PubMedController.kt        # PubMed search endpoints
│   │   ├── ChatController.kt          # Chat frontend + API endpoints
│   │   └── FrontendController.kt      # Frontend routing (/)
│   ├── dto/
│   │   ├── ProcessArticleRequest.kt   # Request for processing articles
│   │   ├── ArticleDto.kt              # Article data transfer
│   │   ├── InteractionDto.kt          # Interaction data transfer
│   │   ├── InteractionResponse.kt     # Paginated response wrapper
│   │   ├── ArticleResponse.kt         # Paginated article response
│   │   ├── SourceDto.kt               # AI source data transfer
│   │   ├── PubMedArticleDto.kt       # PubMed article data
│   │   ├── PubMedSearchResponse.kt   # PubMed search results
│   │   └── ChatResponse.kt            # Chat API responses
│   ├── entity/
│   │   ├── Model.kt                   # AI model metadata
│   │   ├── Plant.kt                   # Plant entity
│   │   ├── Compound.kt                # Bioactive compound entity
│   │   ├── Article.kt                 # Scientific article entity
│   │   ├── Source.kt                  # Source linking article+model+raw response
│   │   ├── Interaction.kt             # Main entity (plant-compound-effects)
│   │   ├── Chat.kt                    # Chat entity with status machine
│   │   ├── PubMedQuery.kt             # PubMed queries linked to chats
│   │   └── ChatInteraction.kt         # Interactions linked to chats
│   ├── llm/
│   │   ├── UserQueryLLM.kt            # LLM interface for query analysis
│   │   ├── ArticleProcessingLLM.kt   # LLM interface for article extraction
│   │   ├── DbSearchLLM.kt             # LLM interface for DB search
│   │   ├── SummaryLLM.kt              # LLM interface for summary generation
│   │   └── GeminiService.kt           # Legacy Gemini service
│   ├── service/llmclient/
│   │   ├── gemini/                    # Gemini implementations
│   │   │   ├── GeminiUserQueryLLM.kt
│   │   │   ├── GeminiArticleProcessingLLM.kt
│   │   │   ├── GeminiDbSearchLLM.kt
│   │   │   └── GeminiSummaryLLM.kt
│   │   └── openai/                    # OpenAI implementations
│   │       ├── OpenAiUserQueryLLM.kt
│   │       ├── OpenAiArticleProcessingLLM.kt
│   │       ├── OpenAiDbSearchLLM.kt
│   │       └── OpenAiSummaryLLM.kt
│   ├── repository/
│   │   ├── ModelRepository.kt         # JPA repositories
│   │   ├── PlantRepository.kt
│   │   ├── CompoundRepository.kt
│   │   ├── ArticleRepository.kt
│   │   ├── SourceRepository.kt
│   │   ├── InteractionRepository.kt   # Custom queries with JOIN FETCH
│   │   ├── ChatRepository.kt          # Chat repository
│   │   ├── PubMedQueryRepository.kt   # PubMed query repository
│   │   └── ChatInteractionRepository.kt # Chat interaction repository
│   └── service/
│       ├── ArticleProcessingService.kt # Business logic + CSV generation
│       ├── PubMedService.kt            # PubMed search and parsing
│       ├── ChatProcessingService.kt    # Chat pipeline orchestration
│       └── ChatWorker.kt               # Scheduled worker for async processing
└── resources/
    ├── db/changelog/
    │   ├── db.changelog-master.yaml    # Liquibase master file
    │   └── db.changelog-1.0.yaml       # Initial schema + add raw_response
    ├── static/
    │   ├── index.html                  # Main frontend page
    │   ├── chat.html                   # AI chat interface
    │   ├── app.js                      # Main page JavaScript
    │   └── styles.css                  # Custom styles
    └── application.yaml                # Spring configuration
```

## Database Schema

### Tables
1. **model** - AI model metadata (name, description)
2. **article** - Scientific articles (url, title, abstract)
3. **source** - Links articles to models + stores raw AI response
   - `article_id` → article.id
   - `model_id` → model.id
   - `raw_response` TEXT (stores full AI JSON response)
4. **plant** - Plant species/genus names
5. **compound** - Bioactive compound names
6. **interactions** - Plant-compound relationships
   - `plant_id` → plant.id
   - `compound_id` → compound.id
   - `effects` TEXT (JSON array of effects)
   - `plant_parts` TEXT (JSON array of parts)
   - `source_id` → source.id
7. **chats** - Chat sessions with users
   - `id` UUID (primary key)
   - `status` VARCHAR(50) (NEW, USER_MESSAGE, USER_MESSAGE_PROCESS, SEARCH_PUBMED, SEARCH_DB, SUMMARY, COMPLETE, FAILED)
   - `user_message` TEXT
   - `keywords` TEXT (JSON array as string)
   - `summary` TEXT (Markdown format)
   - `created_at` TIMESTAMP
   - `last_update` TIMESTAMP
   - `version` BIGINT (for optimistic locking)
8. **pubmed_queries** - PubMed queries linked to chats
   - `id` BIGINT (primary key)
   - `chat_id` UUID → chats.id
   - `query` VARCHAR(500)
9. **chat_interactions** - Links chats to relevant interactions
   - `id` BIGINT (primary key)
   - `chat_id` UUID → chats.id
   - `interaction_id` BIGINT → interactions.id

### Database Configuration
- **Dev**: H2 file-based at `./data/diplomdb`
- **URL**: `jdbc:h2:file:./data/diplomdb;AUTO_SERVER=TRUE`
- **Credentials**: sa/password
- **Console**: http://localhost:8080/h2-console

## Key Features

### 1. Article Processing
- User submits article (URL, title, abstract)
- Gemini AI extracts structured data
- Saves to database with raw AI response

### 2. Data Viewing
- List all articles with pagination
- Filter interactions by:
  - Plant name
  - Compound name
  - Effect/mechanism
- View raw AI responses per article

### 3. PubMed Search Integration
- Search PubMed database directly from the application
- Display article titles and URLs from search results
- Lazy-load abstracts on demand (click "Show Abstract")
- Upload articles directly from PubMed search results
- Pagination support for search results
- Uses Spring RestClient and Jsoup for HTML parsing

### 4. CSV Export
- Export all interactions or filtered subset to CSV
- One row per effect (expanded format for analysis)
- Columns: row, plant, compound, effect, article, model
- Respects current filter settings
- Timestamped filenames
- Ready for pandas/Excel analysis

### 5. API Endpoints
- `POST /api/v1/articles/process` - Process article
- `GET /api/v1/articles` - Get all articles (paginated)
- `GET /api/v1/articles/{id}/sources` - Get sources for article
- `GET /api/v1/sources/{id}` - Get specific source with raw response
- `GET /api/v1/interactions` - Get filtered interactions (paginated)
- `GET /api/v1/interactions/csv` - Download interactions as CSV
- `GET /api/v1/pubmed/search` - Search PubMed (query, page)
- `GET /api/v1/pubmed/article/abstract` - Get article abstract by URL
- `POST /api/v1/pubmed/article/process` - Process article from PubMed
- `GET /chat` - Create new chat and redirect
- `GET /chat/{id}` - Open chat page
- `GET /api/v1/chat/{id}` - Get chat state (JSON)
- `POST /api/v1/chat/{id}/message` - Send message to chat

### 6. AI Chat - Knowledge Base and PubMed Search
- Natural language user queries
- Automatic formation of semantically relevant PubMed queries
- Finding relevant publications in PubMed
- Contextual search within internal interaction database
- Summary generation based on found data with source links
- Asynchronous processing through worker-based pipeline
- Markdown-formatted summary display
- Processing states: query analysis → PubMed search → DB search → summary generation
- Status tracking: NEW → USER_MESSAGE → USER_MESSAGE_PROCESS → SEARCH_PUBMED → SEARCH_DB → SUMMARY → COMPLETE

### 7. Frontend
- Five main sections:
  - Upload form for articles
  - Articles list with source viewing
  - Interactions list with filtering and CSV export
  - PubMed search with article discovery
  - AI Chat interface for natural language queries
- Bootstrap 5 UI with modals
- Pagination with event listeners (fixed closure issues)
- Markdown rendering for chat summaries (marked.js)
- Separate scrollable container for large summaries
- Copy button for markdown text

## Critical Implementation Details

### Transaction Management ⚠️
**IMPORTANT**: Due to JPA lazy-loading issues that were solved:

1. **Service Layer**: All repository methods use `@Transactional(readOnly = true)` for reads
2. **Repository Layer**: Custom queries use `LEFT JOIN FETCH` to eagerly load all relations
3. **Controller Layer**: NO `@Transactional` annotations
4. **JOIN FETCH**: Prevents N+1 queries and lazy-loading exceptions

Example from `InteractionRepository.kt`:
```kotlin
@Query("SELECT DISTINCT i FROM Interaction i " +
        "LEFT JOIN FETCH i.plant " +
        "LEFT JOIN FETCH i.compound " +
        "LEFT JOIN FETCH i.source s " +
        "LEFT JOIN FETCH s.model " +
        "LEFT JOIN FETCH s.article")
fun findAllWithRelations(pageable: Pageable): Page<Interaction>
```

### Entity Classes
- **Source.kt**: Changed from `data class` to regular `class` for lateinit properties
- **Interaction.kt**: Regular `class` with helper methods for JSON serialization
- **Why**: Data classes with lateinit cause issues with JPA/Hibernate

### AI Integration
- **Multiple LLM Backends**: Google Gemini and OpenAI support (switchable via configuration)
- **Modular Architecture**: LLM interfaces for different tasks (UserQueryLLM, ArticleProcessingLLM, DbSearchLLM, SummaryLLM)
- **Conditional Beans**: `@ConditionalOnProperty` for automatic implementation selection
- **Structured Output**: JSON schema for Gemini, structuredOutput for OpenAI
- **Raw Response**: Stored in `Source.rawResponse` for debugging/auditing
- **Format**: Always returns JSON array of plant-compound-effect objects
- **Configuration**: `llm.model` property in `application.yaml` (openai or gemini)
- **OpenAI Config**: `llm.openai.base-url` and `llm.openai.api-key` in `application.yaml`

### Frontend Pagination
- Uses event listeners instead of inline onclick
- Global filter state for maintaining filters across pages
- `data-page` attributes for clean separation
- Proper closure handling

### Liquibase Migrations
- Master file includes versioned changelogs
- Schema changes in separate changesets
- Auto-runs on startup
- Tracked in DATABASECHANGELOG table
- `db.changelog-1.0.yaml`: Initial schema (model, article, source, plant, compound, interactions)
- `db.changelog-2.0.yaml`: Chat schema (chats, pubmed_queries, chat_interactions with optimistic locking)

### Asynchronous Chat Processing
- **ChatWorker**: `@Scheduled` task that polls for chats in various states
- **Processing Pipeline**: Worker-based pipeline for background processing
- **Status Machine**: ChatStatus enum tracks processing stages
- **Optimistic Locking**: `@Version` annotation on Chat entity prevents concurrent update conflicts
- **Polling Interval**: Worker checks for pending tasks periodically
- **State Transitions**: NEW → USER_MESSAGE → USER_MESSAGE_PROCESS → SEARCH_PUBMED → SEARCH_DB → SUMMARY → COMPLETE
- **Error Handling**: Failed chats marked with FAILED status
- **Configuration Limits**: Max 3 articles per query, max 15 total articles

### PubMed Integration
- **PubMedService**: Uses Spring RestClient to fetch HTML from PubMed
- **HTML Parsing**: Jsoup library with multiple selector fallbacks for robustness
- **Search**: Parses search results page to extract article titles and URLs
- **Abstract Extraction**: Fetches individual article pages and extracts abstracts using multiple selector strategies
- **Pagination**: Calculates total pages from search results
- **Error Handling**: Graceful fallbacks if parsing fails

### CSV Export
- **Format**: Standard CSV with proper escaping for special characters
- **Expansion**: One row per effect (if interaction has 3 effects, creates 3 rows)
- **Filtering**: Respects current filter parameters (plantName, compoundName, effect)
- **Columns**: row (sequential number), plant, compound, effect, article (URL), model
- **File Naming**: Timestamped filenames (interactions_YYYYMMDD_HHMMSS.csv)
- **Use Case**: Designed for pandas/Excel analysis and data science workflows

### Chat Frontend
- **Markdown Rendering**: Summary displayed as formatted Markdown document (marked.js library)
- **Separate Scroll**: Summary in separate container with max-height and overflow-y
- **Copy Button**: "Copy Markdown" button to copy raw markdown text to clipboard
- **Visual Feedback**: Button turns green when copied
- **Polling**: Frontend polls chat state every 2 seconds
- **Status Display**: Visual indicators for different processing states
- **Result Display**: Shows queries, interactions, and summary for completed chats
- **Dynamic Updates**: UI updates based on chat status (NEW, COMPLETE, FAILED enable input)

## Common Tasks

### Running the Application
```bash
./gradlew bootRun
# Server starts on http://localhost:8080
```

### Building
```bash
./gradlew build -x test
```

### Adding New Features

#### Adding a New Entity
1. Create entity class in `entity/`
2. Create repository interface in `repository/` with JOIN FETCH if needed
3. Add Liquibase changeset in `db/changelog/`
4. Update service layer as needed
5. Add DTOs in `dto/`
6. Add controller endpoints

#### Adding API Endpoints
- All API routes under `/api/v1/`
- Use `ArticleController` for data endpoints
- Use `FrontendController` for UI routing
- Return appropriate HTTP status codes

#### Frontend Changes
- Edit `static/index.html`, `static/chat.html`, `app.js`, or `styles.css`
- No build process - changes are instant
- Use event listeners, not inline handlers

## Known Issues & Solutions

### Lazy-Loading Fix
**Problem**: "could not initialize proxy - no session"
**Solution**: JOIN FETCH in queries + @Transactional in service layer

### Pagination Click Handling
**Problem**: onClick not firing in generated HTML
**Solution**: Event listeners attached after HTML generation

### H2 Warnings
**Problem**: Various H2 deprecation warnings
**Solution**: Suppressed with configuration (minor, safe to ignore)

### Optimistic Locking
**Problem**: `ObjectOptimisticLockingFailureException` when multiple transactions try to update same Chat
**Solution**: Added `@Version` annotation to Chat entity + `version` column in database

### PostgreSQL bytea Type Issue
**Problem**: `function lower(bytea) does not exist` in PostgreSQL queries
**Solution**: Reordered WHERE clauses in JPQL queries to ensure correct parameter type inference

## API Response Format

### Success Response
```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 10,
  "currentPage": 0,
  "size": 10
}
```

### Interaction DTO
```json
{
  "id": 1,
  "plant": "Curcuma longa",
  "compound": "curcumin",
  "effects": ["anti-inflammatory", "antioxidant"],
  "plantParts": ["root"],
  "model": "gemini-2.5-flash",
  "articleTitle": "Study Title..."
}
```

## Configuration Files

### application.yaml
- H2 database settings
- Liquibase configuration
- JPA/Hibernate settings
- `open-in-view: false` (important for transaction management)
- LLM configuration:
  ```yaml
  llm:
    model: openai  # or gemini
    openai:
      base-url: https://api.artemox.com/v1
      api-key: ${OPENAI_API_KEY}
  ```
- Logging configuration:
  ```yaml
  logging:
    level:
      root: INFO
      io.github.nogll.diplom: DEBUG  # Debug logging for application package
  ```

### build.gradle.kts
- Spring Boot 3.5.7
- Kotlin 1.9.25
- AllOpen plugin for JPA entities
- Google Gemini SDK (com.google.genai:2.0.0)
- OpenAI Java Client (com.openai:openai-java:4.13.0)
- Liquibase core
- Jsoup (1.17.2) for PubMed HTML parsing
- SpringDoc OpenAPI for API documentation

## Testing the Application

### Manual Testing Flow
1. Start application: `./gradlew bootRun`
2. Visit: http://localhost:8080
3. Upload test article
4. View interactions
5. Apply filters
6. Check pagination
7. View raw AI responses
8. Test PubMed search: Search for articles, view abstracts, upload from PubMed
9. Test CSV export: Download CSV with and without filters
10. Test AI Chat: 
    - Click "AI Chat" link
    - Enter natural language query (e.g., "What are the effects of garlic compounds?")
    - Observe status changes and processing pipeline
    - View generated summary in Markdown format
    - Test copy button for markdown text

### Test Data Example
```json
{
  "url": "https://example.com/article",
  "title": "Test Article",
  "abstract": "Curcuma longa contains curcumin with anti-inflammatory effects..."
}
```

## Future Enhancements (Not Implemented)

- User authentication
- Advanced search
- Admin panel
- Batch processing
- API rate limiting
- Metrics and analytics dashboard
- Visualization (heatmaps, network graphs, statistical charts)

## Dependencies to Know

- `spring-boot-starter-web` - REST API
- `spring-boot-starter-data-jpa` - Database access
- `spring-boot-starter-webflux` - WebFlux for RestClient
- `liquibase-core` - Migrations
- `com.google.genai` - Gemini AI SDK
- `com.openai:openai-java` - OpenAI Java Client
- `springdoc-openapi` - API docs
- `org.jsoup:jsoup` - HTML parsing for PubMed
- `marked.js` - Markdown rendering (via CDN in chat.html)
- `h2` - Development database
- `postgresql` - Production database (configured, not used)

## Quick Reference

### Service Layer Pattern
```kotlin
@Service
class MyService {
    @Transactional(readOnly = true)
    fun readSomething() { }
    
    @Transactional
    fun writeSomething() { }
}
```

### Repository Pattern
```kotlin
@Repository
interface MyRepository : JpaRepository<MyEntity, Long> {
    @Query("SELECT m FROM MyEntity m LEFT JOIN FETCH m.relation")
    fun findWithRelations(): List<MyEntity>
}
```

### Controller Pattern
```kotlin
@RestController
@RequestMapping("/api/v1")
class MyController(
    private val myService: MyService
) {
    @GetMapping("/endpoint")
    fun getSomething() = myService.readSomething()
}
```

## Contact & Notes

- This is a bachelor's thesis project
- Currently MVP stage with AI Chat functionality
- H2 database for development
- PostgreSQL ready for production
- No authentication implemented
- API is public (consider adding security in production)
- OpenAI is default LLM backend (configurable via application.yaml)
- Debug logging enabled for `io.github.nogll.diplom` package

