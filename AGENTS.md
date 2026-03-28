# AGENTS.md - Pet Care System

Guidelines for AI coding agents working in this repository.

---

## 1. Project Overview

**Stack**: Java 21, Spring Boot 3.3.x, Spring Cloud Alibaba, MyBatis-Flex, Maven multi-module

**Modules**:
- `pet-care-common`: Shared utilities, constants, DTOs, common configurations
- `pet-care-core`: Core business logic, REST APIs, main application entrypoint
- `pet-care-ai`: AI integration features (LLM, vector search, chat sessions)

**Key Dependencies**: Lombok, Hutool, SA-Token (JWT auth), MyBatis-Flex, Redisson, Caffeine, XXL-JOB, Kafka, Elasticsearch, Spring AI, Prometheus, Knife4j

---

## 2. Build & Run Commands

```bash
# Full project build (runs all tests)
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run core module
cd modules/pet-care-core && mvn spring-boot:run
```

### Test Commands

```bash
# Run all tests
mvn test

# Test specific module
mvn test -pl modules/pet-care-core

# Single test class (FQCN)
mvn test -pl modules/pet-care-core -Dtest=com.petcare.user.service.UserServiceTest

# Single test method
mvn test -Dtest=UserServiceTest#testMethodName

# Test with specific Spring profile
mvn test -Dspring.profiles.active=local -Dtest=MyTest
```

---

## 3. Code Style Guidelines

### General Rules
- **Lombok**: Use `@RequiredArgsConstructor` for dependency injection (NOT manual constructor). Use `@Slf4j` for logging.
- **Hutool**: Prefer Hutool utilities over custom implementations for dates, strings, collections, IO
- **No commented-out code**: Remove dead code
- **Java 21+ features**: Use `var`, pattern matching for instanceof, records for immutable DTOs

### Naming Conventions
| Element | Convention | Example |
|---------|-----------|---------|
| Packages | lowercase | `pvt.mktech.petcare.core.user.service` |
| Classes | PascalCase | `UserService`, `PetDTO` |
| Interfaces | PascalCase (no `I` prefix unless existing pattern) | `UserService` |
| Methods | camelCase | `getUserById` |
| Variables | camelCase | `userId`, `petName` |
| Constants | UPPER_SNAKE_CASE | `DEFAULT_PAGE_SIZE` |
| DB tables/columns | snake_case | `pet_breed`, `create_time` |

### Type Safety
- Specify generic type parameters: `List<User>` not `List`
- Use `Optional` for nullable return values
- Null check all external inputs and API parameters

### Imports
- Order: 1) Java stdlib, 2) Third-party (org.springframework, com.alibaba), 3) Project internal (pvt.mktech.petcare.*), 4) Static imports
- Avoid wildcard imports
- Remove unused imports

### Javadoc (Public Methods Only)
```java
/**
 * User registration
 * @param request Registration request DTO
 * @return New user ID
 * @author Michael Li
 * @since 2025-01-15
 */
```

---

## 4. Error Handling & API

- Use **global exception handling** from common module; throw custom runtime exceptions for business errors
- HTTP status codes: 400 (bad request), 401 (unauthenticated), 403 (forbidden), 404 (not found), 500 (internal error)
- Log exceptions: `log.error("Error: {}", message, e)`
- API documentation: Use SpringDoc annotations (`@Operation`, `@Parameter`) on controller endpoints

### DTOs & Entities
- **Entities**: Map to DB tables with MyBatis-Flex annotations (`@Table`, `@Id`, `@Column`)
- **DTOs**: Use for API requests/responses; never expose entities to clients
- **Validation**: Add Jakarta annotations (`@NotBlank`, `@NotNull`, `@Size`) to DTOs

---

## 5. Common Conventions

| Concern | Approach |
|---------|----------|
| Authentication | SA-Token (never custom auth logic) |
| Database | MyBatis-Flex (avoid raw SQL) |
| Distributed Cache | Redisson |
| In-memory Cache | Caffeine |
| Configuration | `@ConfigurationProperties` + `application.yml` |
| Scheduled Tasks | XXL-JOB |
| AI Integration | Spring AI + Spring AI Alibaba |
| Observability | Micrometer + Prometheus |
| Search | Elasticsearch |
| API Docs | Knife4j (available at `/doc.html`) |

---

## 6. Project Structure

```
petcare/
├── modules/
│   ├── pet-care-common/    # Shared code
│   ├── pet-care-core/      # Main app (port 8080)
│   └── pet-care-ai/        # AI services
├── scripts/db/             # Database initialization
└── docs/                   # Design docs, performance reports
```

### Application Entry Points
- **Core**: `pvt.mktech.petcare.CoreServiceApplication`
- **AI**: `pvt.mktech.petcare.AiServiceApplication`

---

## 7. Commit Guidelines

**Branch Naming**: `feature/<name>`, `fix/<issue>`, `refactor/<module>`, `docs/<desc>`

**Commit Format**:
```
<type>(<scope>): <subject>

<body>
```

**Types**: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`

---

## 8. Do's and Don'ts

✅ **Do**:
- Reuse utilities from `pet-care-common`
- Follow existing patterns in codebase
- Write unit tests for service layer methods
- Handle edge cases and errors

❌ **Don't**:
- Hardcode configuration values
- Introduce new dependencies without checking parent pom
- Expose secrets in code or logs
- Modify DTOs without considering backward compatibility
