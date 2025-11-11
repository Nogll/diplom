# Agents Guide

## Project Overview

**Title**: Дипломная работа - Формирование многомерной матрицы взаимодействия биоактивных веществ растительного происхождения с различными рецепторами (с применением методов и технологий ИИ)

**Purpose**: Web application for analyzing scientific articles and extracting information about plant-derived bioactive compounds using AI (Google Gemini 2.5 Flash).

## Tech Stack

### Backend
- **Language**: Kotlin (1.9.25)
- **Framework**: Spring Boot 3.5.7
- **Java Version**: 21
- **Database**: H2 (file-based, development) → PostgreSQL (production-ready)
- **ORM**: JPA/Hibernate
- **Migrations**: Liquibase
- **AI**: Google Gemini SDK (com.google.genai:1.24.0)
- **API Docs**: SpringDoc OpenAPI/Swagger
- **HTML Parsing**: Jsoup (1.17.2) for PubMed integration
- **HTTP Client**: Spring RestClient for external API calls

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
│   │   └── FrontendController.kt      # Frontend routing (/)
│   ├── dto/
│   │   ├── ProcessArticleRequest.kt   # Request for processing articles
│   │   ├── ArticleDto.kt              # Article data transfer
│   │   ├── InteractionDto.kt          # Interaction data transfer
│   │   ├── InteractionResponse.kt     # Paginated response wrapper
│   │   ├── ArticleResponse.kt         # Paginated article response
│   │   ├── SourceDto.kt               # AI source data transfer
│   │   ├── PubMedArticleDto.kt       # PubMed article data
│   │   └── PubMedSearchResponse.kt   # PubMed search results
│   ├── entity/
│   │   ├── Model.kt                   # AI model metadata
│   │   ├── Plant.kt                   # Plant entity
│   │   ├── Compound.kt                # Bioactive compound entity
│   │   ├── Article.kt                 # Scientific article entity
│   │   ├── Source.kt                  # Source linking article+model+raw response
│   │   └── Interaction.kt             # Main entity (plant-compound-effects)
│   ├── llm/
│   │   └── GeminiService.kt           # Google Gemini AI integration
│   ├── repository/
│   │   ├── ModelRepository.kt         # JPA repositories
│   │   ├── PlantRepository.kt
│   │   ├── CompoundRepository.kt
│   │   ├── ArticleRepository.kt
│   │   ├── SourceRepository.kt
│   │   └── InteractionRepository.kt   # Custom queries with JOIN FETCH
│   └── service/
│       ├── ArticleProcessingService.kt # Business logic + CSV generation
│       └── PubMedService.kt            # PubMed search and parsing
└── resources/
    ├── db/changelog/
    │   ├── db.changelog-master.yaml    # Liquibase master file
    │   └── db.changelog-1.0.yaml       # Initial schema + add raw_response
    ├── static/
    │   ├── index.html                  # Main frontend page
    │   ├── app.js                      # Frontend JavaScript logic
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
   - `raw_response` TEXT (NEW - stores full AI JSON response)
4. **plant** - Plant species/genus names
5. **compound** - Bioactive compound names
6. **interactions** - Plant-compound relationships
   - `plant_id` → plant.id
   - `compound_id` → compound.id
   - `effects` TEXT (JSON array of effects)
   - `plant_parts` TEXT (JSON array of parts)
   - `source_id` → source.id

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

### 6. Frontend
- Four main sections:
  - Upload form for articles
  - Articles list with source viewing
  - Interactions list with filtering and CSV export
  - PubMed search with article discovery
- Bootstrap 5 UI with modals
- Pagination with event listeners (fixed closure issues)

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
- **GeminiService**: Lazy initialization of client to avoid startup errors
- **Schema**: Strict JSON schema enforced for responses
- **Raw Response**: Stored in `Source.rawResponse` for debugging/auditing
- **Format**: Always returns JSON array of plant-compound-effect objects

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
- Edit `static/index.html`, `app.js`, or `styles.css`
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

### build.gradle.kts
- Spring Boot 3.5.7
- Kotlin 1.9.25
- AllOpen plugin for JPA entities
- Google Gemini SDK
- Liquibase core

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
- Multiple AI model support
- Advanced search
- Admin panel
- Batch processing
- API rate limiting
- Metrics and analytics dashboard

## Dependencies to Know

- `spring-boot-starter-web` - REST API
- `spring-boot-starter-data-jpa` - Database access
- `spring-boot-starter-webflux` - WebFlux for RestClient
- `liquibase-core` - Migrations
- `com.google.genai` - Gemini AI
- `springdoc-openapi` - API docs
- `org.jsoup:jsoup` - HTML parsing for PubMed
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
- Currently MVP stage
- H2 database for development
- PostgreSQL ready for production
- No authentication implemented
- API is public (consider adding security in production)

