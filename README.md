# Guestbook — каталог книг і відгуки (Maven + Spring Boot / Spring MVC + Spring Data JPA/H2 + Spring Security + Spring AOP)

Репозиторій: https://github.com/alwaysfeell/guestbook

| Лабораторна | Що зроблено | Де код |
|---|---|---|
| №1 | Базовий застосунок «Книга відгуків» (один модуль WAR) | тег [`lab1`](https://github.com/alwaysfeell/guestbook/tree/lab1) |
| №2 | Трирівнева архітектура: модулі core / persistence / web, каталог книг, пошук, пагінація, правило видалення за 24 години, ArchUnit | тег [`lab2`](https://github.com/alwaysfeell/guestbook/tree/lab2) |
| №3 | HTTP-рівень на мікрофреймворку Javalin: REST-маршрути, Context, middleware, app.exception() | гілка [`lab3`](https://github.com/alwaysfeell/guestbook/tree/lab3) |
| №4 | Spring Boot: IoC-контейнер, біни, DI через конструктор і поле, @Bean, application.yaml | гілка [`lab4`](https://github.com/alwaysfeell/guestbook/tree/lab4) |
| №5 | Spring MVC: контролери @RestController замість сервлетів, DispatcherServlet, @EnableWebMvc, @RestControllerAdvice, CRUD відгуків | гілка [`lab5`](https://github.com/alwaysfeell/guestbook/tree/lab5) |
| №6 | Thymeleaf: серверний рендеринг сторінок, form binding (th:object, th:field), th:if / th:unless, фрагменти, локалізація uk / en | гілка [`lab6`](https://github.com/alwaysfeell/guestbook/tree/lab6) |
| №7 | FreeMarker: HTML-листи з шаблонів .ftl (header / footer, умовні блоки, списки), надсилання через Gmail SMTP (JavaMailSender) | гілка [`lab7`](https://github.com/alwaysfeell/guestbook/tree/lab7) |
| №8 | JPA/Hibernate замість JDBC: сутності BookEntity / UserEntity / CommentEntity, зв'язки One-To-Many / Many-To-One, Spring Data репозиторії, міграції Flyway, сторінка коментарів користувача | гілка [`lab8`](https://github.com/alwaysfeell/guestbook/tree/lab8) |
| №9 | Spring Security: форма логіну, реєстрація з підтвердженням email, ролі USER / ADMIN, власний UserDetailsService, захист /books і /api/books, @PreAuthorize, сторінки 403 / 404, аудит входу і виходу | гілка [`lab9`](https://github.com/alwaysfeell/guestbook/tree/lab9) |
| №10 | Обробка винятків і AOP: власні винятки в core, `CommentService.delete(bookId, commentId, createdAt)`, `GlobalApiExceptionHandler` (JSON 400 / 404), аспекти логування і вимірювання часу, юніт-тести з Mockito, інтеграційний тест MockMvc | гілка [`lab10`](https://github.com/alwaysfeell/guestbook/tree/lab10) |
 
---

# Лабораторна робота №10 — обробка винятків, AOP і тестування

Видалення відгуку перенесено в сервіс core: `CommentService.delete(bookId, commentId, createdAt)` перевіряє дані
і кидає власні винятки, глобальний обробник у web перетворює їх на JSON-відповіді, а аспекти AOP логують виклики
сервісів без змін у бізнес-коді.

## Запуск
```bash
mvn clean install
mvn -pl web spring-boot:run
```
`mvn clean install` проганяє юніт-тести (Surefire) і інтеграційні тести `*IT` (Failsafe).

## Видалення відгуку `POST /comments/delete` (лише ADMIN)
Параметри форми: `bookId`, `commentId`, `createdAt` (ISO-8601, наприклад `2026-11-18T10:00:00Z`).
Кнопка «Видалити» на сторінці книги надсилає їх прихованими полями. Успіх — redirect на `/books/{bookId}/comments`.

| Ситуація | Виняток core | Відповідь |
|---|---|---|
| `bookId <= 0` або `commentId <= 0` | `InvalidCommentDeleteException` | `400 {"error": "Некоректний ідентифікатор книги або коментаря"}` |
| немає `createdAt` / дата в майбутньому / не збігається з БД | `InvalidCommentDeleteException` | `400 {"error": "..."}` |
| відгук старший за 24 години | `CommentTooOldException` | `400 {"error": "Коментар створено більше ніж 24 години тому і не може бути видалений"}` |
| книги немає | `BookNotFoundException` | `404 {"error": "Книгу з id=… не знайдено"}` |
| відгуку немає | `CommentNotFoundException` | `404 {"error": "Коментар з id=… не знайдено у книзі з id=…"}` |

Винятки успадковують `ValidationException`, `ConflictException` і `NotFoundException` з попередніх робіт.

## AOP
- `CommentServiceLoggingAspect` — `@Around("execution(* sumdu.edu.ua.core.service.CommentService.delete(..))")`:
  параметри виклику, час виконання, помилка (виняток прокидається далі).
- `ServiceTimingAspect` — `@Around("execution(* sumdu.edu.ua.core.service..*(..))")`: час кожного методу сервісів,
  рядки `[timing] BookService.findById() took 1.25 ms`.

## Додатково (+20%)
- `CommentService.add` / `update` відхиляють текст із забороненими словами (`спам`, `казино`, `ставки`, `spam`,
  `casino`) винятком `ForbiddenWordsException`.
- `MvcExceptionHandler` (`@ControllerAdvice` для HTML-контролерів) робить redirect на `/problem`
  і показує повідомлення через flash-атрибути.

## Тести
`mvn clean install` — 185 тестів: core 58, persistence 20, web 102 і 5 інтеграційних.
`CommentServiceTest` (JUnit 5 + Mockito: `mock`, `assertThrows`, `verifyNoInteractions`, `verify(repo).delete`),
`CommentDeleteExceptionIT` (`@SpringBootTest(classes = AppInit.class)`, `@AutoConfigureMockMvc`, `@MockBean CommentService`),
`CommentsControllerTest` (JSON-помилки з реальним сервісом), `AopLoggingTest` (логи аспектів),
`MvcExceptionHandlerTest` (redirect на `/problem`).

---

# Лабораторна робота №9 — Spring Security: автентифікація та авторизація

Застосунок захищено Spring Security. Користувачі завантажуються з бази даних через власний
`AppUserDetailsService` (модуль core), паролі зберігаються як BCrypt-хеші, а доступ до сторінок,
REST API і методів сервісів залежить від ролі `USER` або `ADMIN`.

## Запуск
```bash
mvn clean install
mvn -pl web spring-boot:run
```
Під час старту Flyway застосовує нову міграцію `V3__security.sql` (email, ознака підтвердження і код підтвердження).

| Логін | Пароль | Роль |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `olena`, `andriy`, `vladyslav`, `iryna` | `user123` | USER |

## Права доступу
| Ресурс | Гість | USER | ADMIN |
|---|---|---|---|
| `/css/**`, `/js/**`, `/images/**`, `/img/**`, `/login`, `/register`, `/confirm`, `/403`, `/404` | так | так | так |
| `GET /books`, `GET /books/{id}/comments`, `GET /users/**` | → `/login` | так | так |
| `POST /books/{id}/comments` (новий відгук від імені поточного користувача) | → `/login` | так | так |
| `GET/POST /books/add`, `POST /books/{id}/delete`, `POST /books/{id}/comments/{cid}/delete`, `/info`, `/h2-console` | → `/login` | 403 | так |
| `GET /api/books`, `GET /api/books/{id}` | 401 | так | так |
| `POST /api/books`, `DELETE /api/books/{id}` | 401 | 403 | так |

Після входу — redirect на `/books`, після виходу (`POST /logout`) — на `/login?logout`.

## Реєстрація і підтвердження email
1. `GET /register` — форма: логін, email, пароль, повтор пароля.
2. `POST /register` — пароль хешується BCrypt, роль `USER`, акаунт вимкнений (`enabled=false`), генерується код.
3. На email надходить лист (шаблон FreeMarker `confirm_account.ftl`) з посиланням `/confirm?code=...`.
   Якщо SMTP не налаштовано, посилання записується в лог.
4. До підтвердження вхід повертає `/login?disabled`; після `GET /confirm?code=...` — `/login?confirmed`.

Листи надсилаються через Gmail SMTP із `mail.properties` (див. ЛР 7) або через Mailpit:
`mvn -pl web spring-boot:run -Dspring-boot.run.profiles=mailpit`.

## REST API у Postman
Authorization → Basic Auth (`admin` / `admin123` або `olena` / `user123`). Тіло POST — raw JSON:
```json
{"title": "Intermezzo", "author": "Михайло Коцюбинський", "pubYear": 1908}
```
Без авторизації API повертає `401 Unauthorized` у форматі JSON, без потрібної ролі — `403 Forbidden`.
Ланцюжок `/api/**` не створює HTTP-сесію і не використовує CSRF.

## Нові та змінені файли
```
core/src/main/java/sumdu/edu/ua/core/
├── security/AppUserDetailsService.java     implements UserDetailsService
├── domain/User.java                        email, enabled, confirmationCode
└── service/UserService.java                register(), confirm(), PasswordEncoder
persistence/src/main/resources/db/migration/V3__security.sql
web/src/main/java/sumdu/edu/ua/web/
├── security/SecurityConfig.java            два SecurityFilterChain (API і сайт), BCrypt, @EnableMethodSecurity
├── security/ApiSecurityHandler.java        JSON 401 / 403 для /api/**
├── security/SecurityAuditListener.java     аудит login / logout / access denied
├── controller/AuthController.java          /login, /register, /confirm
├── controller/BooksApiController.java      /api/books
└── controller/ErrorPagesController.java    /403, /404
web/src/main/resources/templates/
├── auth/login.html, register.html, register-success.html, confirm-error.html
└── error/403.html, 404.html
```
`@PreAuthorize` у core: `BookService.add / save / delete` і `CommentService.update / delete` — `hasRole('ADMIN')`,
`CommentService.add` — `isAuthenticated()`.

## Тести
`mvn clean install` — 159 тестів: core 45, persistence 20, web 94. Нові: `SecurityConfigTest` (правила доступу,
логін, логаут, CSRF, кнопки за ролями), `ApiSecurityTest` (401 / 403 / 201 / 204 через Basic Auth),
`RegistrationTest` (реєстрація, лист на GreenMail, підтвердження, вхід), `MethodSecurityTest` (`@PreAuthorize`),
`ErrorPagesTest`, `SecurityAuditListenerTest`, `AppUserDetailsServiceTest`.

---

# Лабораторна робота №8 — JPA, Hibernate, Flyway і сторінка коментарів користувача

Шар persistence переведено з JDBC на Spring Data JPA (Hibernate 6). Схему БД створюють міграції Flyway,
а не `schema.sql`. З'явилась сутність користувача: кожен відгук належить книзі і користувачу,
а сторінка `/users/{id}/comments` показує всі відгуки одного користувача.

## Запуск
```bash
mvn clean install
mvn -pl web spring-boot:run
```
Під час старту Flyway застосовує міграції `V1__init.sql` і `V2__seed_data.sql` до файлової бази
`./data/library.mv.db`. Hibernate виводить усі SQL-запити в консоль (`spring.jpa.show-sql=true`).

| Адреса | Сторінка |
|---|---|
| http://localhost:8080/books | Каталог книг |
| http://localhost:8080/books/{id}/comments | Книга, її відгуки і форма нового відгуку |
| http://localhost:8080/users | Користувачі і кількість їхніх відгуків |
| http://localhost:8080/users/{id}/comments | Коментарі користувача |
| http://localhost:8080/h2-console | H2 Console: JDBC URL `jdbc:h2:file:./data/library;AUTO_SERVER=TRUE`, user `sa`, пароль порожній |

## Налаштування (`web/src/main/resources/application.yaml`)
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/library;AUTO_SERVER=TRUE
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
  h2:
    console:
      enabled: true
```

## Модуль persistence
```
persistence/src/main/
├── java/sumdu/edu/ua/persistence/
│   ├── config/JpaConfig.java                  @EntityScan, @EnableJpaRepositories
│   ├── entity/
│   │   ├── BookEntity.java                    @OneToMany(mappedBy = "book")
│   │   ├── UserEntity.java                    @OneToMany(mappedBy = "user")
│   │   └── CommentEntity.java                 @ManyToOne book, @ManyToOne user
│   ├── repository/
│   │   ├── BookRepository.java                JpaRepository<BookEntity, Long>
│   │   ├── UserRepository.java                JpaRepository<UserEntity, Long>
│   │   └── CommentRepository.java             findByUserId(Long userId)
│   └── adapter/
│       ├── EntityMapper.java
│       ├── JpaCatalogRepositoryAdapter.java   implements CatalogRepositoryPort
│       ├── JpaUserRepositoryAdapter.java      implements UserRepositoryPort
│       └── JpaCommentRepositoryAdapter.java   implements CommentRepositoryPort
└── resources/db/migration/
    ├── V1__init.sql                           таблиці users, books, comments і зовнішні ключі
    └── V2__seed_data.sql                      5 користувачів, 12 книг, 12 відгуків
```
Класи JDBC (`Db`, `DbInit`, `JdbcBookRepository`, `JdbcCommentRepository`) і файли `schema.sql`, `data.sql` видалено.
Модуль core не залежить від JPA: сервіси `BookService`, `UserService`, `CommentService` працюють через порти
і позначені `@jakarta.transaction.Transactional`.

## Тести
`mvn clean install` — 110 тестів: core 35, persistence 17 (`@DataJpaTest` з `ddl-auto=validate` перевіряє,
що сутності відповідають схемі Flyway), web 58 (MockMvc, сторінки користувачів, каскадне видалення,
ArchUnit-правило «жоден модуль не використовує JDBC»).

---

# Лабораторна робота №7 — FreeMarker і Gmail SMTP

Після додавання книги через форму `/books/add` застосунок формує HTML-лист із шаблону FreeMarker
і надсилає його через Gmail SMTP. Сторінки сайту, як і раніше, рендерить Thymeleaf.

## Налаштування пошти
1. Увімкнути двоетапну перевірку в акаунті Google і створити App Password
   (https://myaccount.google.com/apppasswords).
2. Скопіювати `mail.properties.example` у корінь проєкту під назвою `mail.properties` і вписати свої дані:
```properties
spring.mail.username=yourmail@gmail.com
spring.mail.password=yourpassword
app.mail.to=yourmail@gmail.com
```
Файл `mail.properties` підключається через `spring.config.import` і внесений у `.gitignore`,
тому пароль не потрапляє в репозиторій.

## Запуск
```bash
mvn clean install
mvn -pl web spring-boot:run
```
Для перевірки без Gmail можна запустити локальний SMTP-перехоплювач Mailpit (http://localhost:8025)
і профіль `mailpit`:
```bash
mvn -pl web spring-boot:run -Dspring-boot.run.profiles=mailpit
```

## Файли
```
web/src/main/
├── java/sumdu/edu/ua/
│   ├── config/EmailFreemarkerConfig.java
│   └── web/mail/
│       ├── AppMailProperties.java
│       ├── EmailTemplateProcessor.java
│       └── MailService.java
└── resources/
    ├── application.yaml
    ├── application-mailpit.yaml
    ├── static/img/logo.png
    └── mail-templates/
        ├── new_book.ftl
        └── layout/
            ├── header.ftl
            └── footer.ftl
```

## Тести
79 тестів: core — 23, persistence — 7, web — 49. Нові: `EmailTemplateProcessorTest` (рендеринг шаблону,
раритетне видання, формат дати, екранування HTML), `MailServiceTest` і `SmtpAuthenticationTest`
(надсилання та помилка автентифікації на тестовому SMTP-сервері GreenMail), `MailConfigTest`.

---

# Лабораторна робота №6 — Thymeleaf

REST-контролери з Лаб 5 замінено MVC-контролерами, які повертають ім'я шаблону.
HTML-сторінки формує Thymeleaf на сервері з даних об'єкта `Model`. Модулі core і persistence не змінювались.

## Запуск
```bash
mvn clean install
mvn -pl web spring-boot:run
```
Після старту: http://localhost:8080/books. Мова перемикається посиланнями UA / EN (параметр `?lang=`),
вибір зберігається в cookie `lang` на 30 днів. Без cookie мова береться із заголовка `Accept-Language`
(uk, en), для інших мов — українська.

## Маршрути
| Метод і шлях | Контролер | Шаблон |
|---|---|---|
| `GET /` | `HomeController` | redirect на `/books` |
| `GET /books?q=&page=` | `BooksController.getAllBooks` | `books.html` |
| `GET /books/add` | `BooksController.showAddForm` | `book-form.html` |
| `POST /books/add` | `BooksController.addBook` | redirect на `/books` або `book-form.html` з помилками |
| `GET /books/{bookId}/comments` | `CommentsController.list` | `book-comments.html` |
| `POST /books/{bookId}/comments` | `CommentsController.add` | redirect або `book-comments.html` з помилками |
| `POST /books/{bookId}/comments/{commentId}/delete` | `CommentsController.delete` | redirect |
| `GET /info` | `InfoController` | JSON |

## Шаблони
```
web/src/main/resources/
├── messages.properties          (uk, за замовчуванням)
├── messages_en.properties
├── static/css/style.css
└── templates/
    ├── books.html
    ├── book-form.html
    ├── book-comments.html
    ├── error.html
    └── fragments/
        ├── head.html
        ├── header.html
        └── footer.html
```

## Тести
65 тестів: core — 23, persistence — 7, web — 35 (`BooksControllerTest`, `CommentsControllerTest`,
`LocalizationTest`, `WebMvcConfigTest`, `SpringContextTest`, ArchUnit).

---

# Лабораторна робота №5 — Spring MVC

Сервлети з Лаб 4 (`BooksServlet`, `CommentsServlet`, `InfoServlet`, `ErrorFilter`, `ErrorPageServlet`)
замінено контролерами Spring MVC. Усі HTTP-запити тепер приймає один `DispatcherServlet`
(його реєструє Spring Boot на шляху `/`) і за анотаціями `@GetMapping`, `@PostMapping`, `@PutMapping`,
`@DeleteMapping` передає потрібному методу контролера. JSON формує Jackson автоматично.

## Запуск
```bash
mvn clean install
mvn -pl web spring-boot:run
```
Після старту: http://localhost:8080/ (каталог), http://localhost:8080/info (біни і маршрути). Зупинка: `Ctrl+C`.


## Структура модуля web
| Клас | Анотації | Призначення |
|---|---|---|
| `controller/BooksController` | `@RestController`, `@RequestMapping("/books")` | GET /books, GET /books/{id}, POST /books |
| `controller/CommentsController` | `@RestController`, `@RequestMapping("/comments")` | GET, POST, PUT /{id}, DELETE /{id} — повний CRUD |
| `controller/InfoController` | `@Controller` + `@ResponseBody` | GET /info |
| `controller/HomeController` | `@Controller` | GET / → логічне представлення `index` → `/index.html` |
| `dto/*` | `record` | тіла запитів (`@RequestBody`) і відповідь `BookDetails` |
| `error/GlobalExceptionHandler` | `@RestControllerAdvice` | винятки → HTTP-статус + JSON `ApiError` |
| `error/ApiErrorController` | `@RestController`, `ErrorController` | шлях `/error` у форматі `ApiError` |
| `config/WebConfig` | `@Configuration`, `@EnableWebMvc` | статичні ресурси, ViewResolver, Jackson-конвертер |

## Ендпоїнти
| Метод і шлях | Тіло | Відповідь |
|---|---|---|
| `GET /books?q=&page=&size=&sort=` | — | 200, сторінка книг |
| `GET /books/{id}?page=&size=` | — | 200, `{book, comments}`; 404; 400 |
| `POST /books` | `{"title","author","pubYear"}` | 201 + `Location`; 400; 415 |
| `GET /comments?bookId=` | — | 200; 400 без bookId; 404 |
| `POST /comments` | `{"bookId","author","text"}` | 201; 400; 404 |
| `PUT /comments/{id}?bookId=` | `{"text"}` | 200; 404; 409 (старше 24 год) |
| `DELETE /comments/{id}?bookId=` | — | 204; 404; 409 |
| `GET /info` | — | 200, параметри, біни, маршрути DispatcherServlet |

POST і PUT приймають лише `Content-Type: application/json` (у Postman: Body → raw → JSON).

## Обробка помилок
| Виняток | Статус |
|---|---|
| `ValidationException` (core), `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException`, `HttpMessageNotReadableException` | 400 |
| `NotFoundException` (core), `NoResourceFoundException` | 404 |
| `HttpRequestMethodNotSupportedException` | 405 + заголовок `Allow` |
| `ConflictException` (core) | 409 |
| `HttpMediaTypeNotSupportedException` | 415 |
| будь-який інший `Exception` | 500 |

## Тести
58 тестів: core — 23, persistence — 7, web — 28 (`BooksControllerTest`, `CommentsControllerTest` на MockMvc,
`WebMvcConfigTest`, `SpringContextTest`, ArchUnit).

---

# Лабораторна робота №4 — Spring Core & Boot

Застосунок із Лаб 2 (сервлети + core + persistence) переведено на **Spring Boot**.
Об'єкти більше не створюються вручну (клас `Beans` і слухач `AppInit` з `web.xml` видалено) —
їх створює і зв'язує IoC-контейнер Spring. Javalin-версія з Лаб 3 залишилась у гілці `lab3`.

## Запуск
```bash
mvn clean install
mvn -pl web spring-boot:run
```
Після старту: http://localhost:8080/ (каталог), http://localhost:8080/info (параметри і біни). Зупинка: `Ctrl+C`.
Альтернатива: `java -jar web/target/guestbook.war` (виконуваний WAR із вбудованим Tomcat).

## Біни
| Бін | Модуль | Як оголошено | Ін'єкція |
|---|---|---|---|
| `Db` | persistence | `@Component` | конструктор, `@Value("${app.db.url}")` |
| `DbInit` | persistence | `@Component` + `@PostConstruct` | конструктор (`Db`) |
| `JdbcBookRepository`, `JdbcCommentRepository` | persistence | `@Repository` | конструктор (`Db`) |
| `CoreConfig` | web (`config`) | `@Configuration` | — |
| `clock`, `bookService`, `commentService` | web (`config`) | `@Bean` у `CoreConfig` | параметри методів (порти, `Clock`, `@Value`) |
| `ServletConfig` | web | `@Configuration` | — |
| `booksServlet`, `commentsServlet`, `infoServlet`, `errorPageServlet`, `errorFilter` | web | `@Bean` (`ServletRegistrationBean`, `FilterRegistrationBean`) | сервіси → конструктори сервлетів |
| `AppProperties` | web | `@ConfigurationProperties("app")` | значення з application.yaml |
| `AppInfoService` | web | `@Service` | конструктор + поле (`@Autowired`, `@Value`) |
| `StartupReport` | web | `@Component` | конструктор; `@EventListener`, `@PreDestroy` |

Класи `core` анотацій Spring не мають (це перевіряє ArchUnit) — вони реєструються методами `@Bean`.

## Конфігурація (`web/src/main/resources/application.yaml`)
| Параметр | Значення | Хто використовує |
|---|---|---|
| `spring.application.name` | guestbook | `AppInfoService` (`@Value` у полі) |
| `server.port` | 8080 | вбудований Tomcat |
| `app.title` | Каталог книг і відгуки | `AppProperties` |
| `app.db.url` | jdbc:h2:file:./data/books;AUTO_SERVER=TRUE | `Db` |
| `app.catalog.default-page-size` | 10 | `BooksServlet`, `CommentsServlet` |
| `app.comments.delete-window` | 24h | `CommentService` (правило видалення відгуку) |
| `logging.*` | рівні та формат логів | Spring Boot (замість logback.xml) |

Параметр можна перевизначити без зміни файлу: `mvn -pl web spring-boot:run -Dspring-boot.run.arguments=--server.port=9090`.

## Ендпоїнти
Ті самі, що в Лаб 2 (`/books`, `/books/{id}`, `/comments`, `/comments/{id}?bookId=`), плюс `GET /info` —
параметри застосунку і список бінів. Сторінки: `/` і `/book.html?id=N` (тепер у `src/main/resources/static`).

## Тести
33 тести: юніт- і ArchUnit-тести з Лаб 2 + `SpringContextTest` (`@SpringBootTest`): завантаження бінів,
singleton-скоуп, реєстрація сервлетів, параметри з application.yaml, ін'єкція через конструктор і поле,
HTTP-запити через вбудований Tomcat (БД у пам'яті).

---

# Лабораторна робота №3 — мікрофреймворк Javalin

Модулі `core` і `persistence` не змінювались. У модулі `web` додано Javalin 6.2.0:
клас запуску `sumdu.edu.ua.web.JavalinBookApp` і контролери в пакеті `sumdu.edu.ua.web.javalin`.
Сервлетна версія з Лаб 2 залишилась у проєкті для порівняння.

## Запуск
```bash
mvn clean install
mvn -pl web exec:java        # Javalin-версія
mvn -pl web jetty:run        # сервлетна версія, запускати не одночасно - обидві на порту 8080
```
Після старту: http://localhost:8080/ (HTML-клієнт REST API). Зупинка: `Ctrl+C`.

## REST API
| Метод | URL | Опис | Коди |
|---|---|---|---|
| GET | `/api/books?q=&page=&size=&sort=` | список книг (query-параметри) | 200, 400 |
| GET | `/api/books/{id}` | одна книга (path-параметр) | 200, 400, 404 |
| POST | `/api/books` | додати книгу, JSON `{"title","author","pubYear"}` | 201, 400 |
| GET | `/api/books/{bookId}/comments?page=&size=` | відгуки книги | 200, 400, 404 |
| POST | `/api/comments` | додати відгук, JSON `{"bookId","author","text"}` | 201, 400, 404 |
| DELETE | `/api/comments/{id}?bookId=` | видалити відгук (лише протягом 24 год) | 204, 400, 404, 409 |

Помилки повертаються у форматі `ApiError`; збій БД — 500 `Internal server error`.

## Middleware і обробка винятків
- `app.before()` — INFO-лог кожного запиту: `GET /api/books`;
- `app.after()` — INFO-лог зі статусом: `GET /api/books -> 200`;
- `app.exception()` — ValidationException → 400, NotFoundException → 404, ConflictException → 409, будь-який інший виняток → 500 (ERROR-лог).

---

# Лабораторна робота №2 — трирівнева архітектура

## Версії
| Інструмент | Версія |
|---|---|
| JDK | 21.0.13 (Eclipse Temurin), байткод release 17 |
| Maven | 3.10.0 |
| Jetty (jetty-maven-plugin) | 11.0.24 |
| Jakarta Servlet API | 5.0.0 (provided) |
| H2 | 2.2.224 |
| Jackson (jackson-databind + jackson-datatype-jsr310) | 2.17.2 |
| SLF4J / Logback | 2.0.13 / 1.5.6 |
| JUnit Jupiter | 5.10.2 |
| ArchUnit | 1.3.0 |

## Модулі
| Модуль | Packaging | Що містить | Залежить від |
|---|---|---|---|
| `core` | jar | доменні моделі (Book, Comment, Page, PageRequest), порти (CatalogRepositoryPort, CommentRepositoryPort), сервіси з бізнес-правилами | slf4j-api |
| `persistence` | jar | реалізація портів через JDBC/H2, `schema.sql`, `data.sql` | core, h2 |
| `web` | war | сервлети, фільтр помилок, точка збирання залежностей, HTML-сторінки | core, persistence*, Servlet API |

\* від `persistence` залежить лише пакет `sumdu.edu.ua.config` (клас `Beans`).
Пакет `sumdu.edu.ua.web` про persistence не знає — це перевіряє ArchUnit.

## Запуск
```bash
mvn clean install
mvn -pl web jetty:run
```
Перша команда збирає всі модулі й проганяє всі тести (25, у т.ч. ArchUnit), друга запускає Jetty для модуля web.
Після старту відкрити: http://localhost:8080/  
Зупинити сервер: `Ctrl+C`.

> На Windows перед запуском варто виконати `chcp 65001`, щоб кирилиця в логах відображалась коректно.

## База даних
- JDBC URL: `jdbc:h2:file:./data/books;AUTO_SERVER=TRUE`
- Користувач: `sa`, пароль порожній
- Файл БД: `./data/books.mv.db` — у корені проєкту, створюється автоматично
- Таблиці `books` і `comments` створюються з `schema.sql` при старті; якщо каталог порожній, `data.sql` додає 12 книг і 2 відгуки (один з них «створений» 48 годин тому — для перевірки правила 24 годин)
- Щоб почати з чистої БД: зупинити сервер і видалити `data/books.*`
## Ендпоїнти
| Метод | URL | Опис | Коди |
|---|---|---|---|
| GET | `/books?q=&page=&size=&sort=` | список книг: пошук за назвою/автором, пагінація (`page` з 0, `size` 1–100), сортування `id`/`title`/`author`/`pubYear` + `,asc`/`,desc` | 200, 400, 500 |
| GET | `/books/{id}?page=&size=` | сторінка книги: книга + її відгуки | 200, 400, 404, 500 |
| POST | `/books` (`title`, `author`, `pubYear`) | додати книгу | 201, 400, 500 |
| GET | `/comments?bookId=&page=&size=` | відгуки книги (нові зверху) | 200, 400, 404, 500 |
| POST | `/comments` (`bookId`, `author`, `text`) | додати відгук | 201, 400, 404, 500 |
| DELETE | `/comments/{id}?bookId=` | видалити відгук — лише протягом 24 год після створення | 204, 400, 404, 409, 500 |

Сторінки: `/` — каталог книг, `/book.html?id=N` — сторінка книги з відгуками.

Усі відповіді — `application/json;charset=UTF-8`. Єдиний формат помилки:
```json
{"status":409,"error":"Conflict","message":"Comment can be deleted only within 24 hours after creation","path":"/comments/1","timestamp":"2026-11-06T16:38:11.602Z"}
```

## Логування
- `INFO` — створення/видалення відгуку (id, bookId, author, length), створення книги;
- `WARN` — помилки клієнта 4xx (метод, шлях, код, причина);
- `ERROR` — помилки сервера 5xx (зі stacktrace).
```
INFO  s.e.u.web.http.CommentsServlet - Comment created: id=3, bookId=3, author='Влад', length=12
WARN  s.edu.ua.web.error.ErrorFilter - DELETE /comments/1?bookId=11 -> 409: Comment can be deleted only within 24 hours after creation
ERROR s.edu.ua.web.error.ErrorFilter - GET /books/11 -> 500: DB error while listing comments
```

## Архітектурні тести (ArchUnit)
- `core`: core не залежить від Servlet/JDBC/H2 і від інших модулів; порти — інтерфейси; домен не залежить від сервісів; усі конструктори core публічні.
- `persistence`: репозиторії/DAO лише в persistence і реалізують порти core; persistence не залежить від web/Servlet.
- `web`: web не залежить від persistence і JDBC; сервлети/фільтри лише у web; дотримання шарів (layeredArchitecture).
## Структура
```
guestbook/
├── pom.xml                        parent POM (packaging=pom)
├── core/
│   └── src/main/java/sumdu/edu/ua/core/
│       ├── domain/                Book, Comment, Page, PageRequest
│       ├── exception/             ValidationException, NotFoundException, ConflictException
│       ├── port/                  CatalogRepositoryPort, CommentRepositoryPort
│       └── service/               BookService, CommentService
├── persistence/
│   └── src/main/
│       ├── java/sumdu/edu/ua/persistence/jdbc/   Db, DbInit, JdbcBookRepository, JdbcCommentRepository
│       └── resources/             schema.sql, data.sql
└── web/
    └── src/main/
        ├── java/sumdu/edu/ua/
        │   ├── config/            AppInit, Beans
        │   └── web/
        │       ├── http/          BooksServlet, CommentsServlet, HttpSupport
        │       └── error/         ErrorFilter, ErrorPageServlet, ApiError
        ├── resources/logback.xml
        └── webapp/                index.html, book.html, WEB-INF/web.xml
```
Тести лежать у `src/test/java` кожного модуля.
 
---

# Лабораторна робота №1 — «Книга відгуків»

> Код цієї лабораторної збережено під тегом `lab1`. Команди та структура нижче актуальні для нього:
> `git checkout lab1` (повернутися назад: `git checkout main`).

## Версії
| Інструмент | Версія |
|---|---|
| JDK | 21.0.13 (Eclipse Temurin) |
| Maven | 3.10.0 |
| Jetty (jetty-maven-plugin) | 11.0.24 |
| Jakarta Servlet API | 5.0.0 (provided) |
| H2 | 2.2.224 |
| Jackson (jackson-databind) | 2.17.2 |
| SLF4J / Logback | 2.0.13 / 1.5.6 |

## Запуск
```bash
mvn clean compile
mvn jetty:run
```
Після старту відкрити: http://localhost:8080/  
Зупинити сервер: `Ctrl+C`.

> На Windows перед запуском варто виконати `chcp 65001`, щоб кирилиця в логах відображалась коректно.

## База даних
- JDBC URL: `jdbc:h2:file:./data/guest;AUTO_SERVER=TRUE`
- Користувач: `sa`, пароль порожній
- Файл БД: `./data/guest.mv.db` — у корені проєкту, створюється автоматично під час першого запуску
- Таблиця `comments` створюється класом `AppInit` при старті додатка:
```sql
create table if not exists comments(
  id         bigint generated by default as identity primary key,
  author     varchar(64)   not null,
  text       varchar(1000) not null,
  created_at timestamp     not null default current_timestamp
)
```

## Ендпоїнти
| Метод | URL | Опис | Відповідь |
|---|---|---|---|
| GET | `/` | HTML-сторінка з формою та списком відгуків | 200 |
| GET | `/comments` | JSON-список коментарів (останні зверху) | 200; 500 `DB error` |
| POST | `/comments` | Додати коментар (параметри `author`, `text`) | 204 без тіла; 400 — помилка валідації; 500 `DB error` |

Валідація: `author` — обов'язково, ≤ 64 символів; `text` — обов'язково, ≤ 1000 символів.

Приклад відповіді `GET /comments`:
```json
[
  {"id": 3, "author": "Влад 3", "text": "ТЕСТ 3", "createdAt": "2026-11-05T18:35:51.076659Z"},
  {"id": 2, "author": "Владислав 2", "text": "ТЕСТ 2", "createdAt": "2026-11-05T18:35:28.441408Z"},
  {"id": 1, "author": "Vladyslav 1", "text": "TEST 1", "createdAt": "2026-11-05T18:35:09.768413Z"}
]
```
Час `createdAt` зберігається в UTC.

## Логування
Після успішного додавання коментаря в консоль пишеться INFO-лог:
```
20:35:09.790 INFO  a.w.CommentsServlet - New comment: id=1, author='Vladyslav 1', length=6
```
Помилки БД логуються на рівні ERROR.

## Структура
```
guestbook/
├── pom.xml
├── README.md
├── .gitignore
└── src/main/
    ├── java/app/web/
    │   ├── AppInit.java          # ServletContextListener — створює таблицю при старті
    │   ├── CommentDao.java       # DAO: init / add / list
    │   ├── CommentsServlet.java  # GET/POST /comments
    │   └── Db.java               # JDBC-з'єднання з H2
    ├── resources/
    │   └── logback.xml           # налаштування логів
    └── webapp/
        ├── index.html            # сторінка з формою
        └── WEB-INF/
            └── web.xml           # listener + servlet mapping
```
 