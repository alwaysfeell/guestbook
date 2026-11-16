# Guestbook — каталог книг і відгуки (Maven + Spring Boot / Spring MVC + JDBC/H2)

Репозиторій: https://github.com/alwaysfeell/guestbook

| Лабораторна | Що зроблено | Де код |
|---|---|---|
| №1 | Базовий застосунок «Книга відгуків» (один модуль WAR) | тег [`lab1`](https://github.com/alwaysfeell/guestbook/tree/lab1) |
| №2 | Трирівнева архітектура: модулі core / persistence / web, каталог книг, пошук, пагінація, правило видалення за 24 години, ArchUnit | тег [`lab2`](https://github.com/alwaysfeell/guestbook/tree/lab2) |
| №3 | HTTP-рівень на мікрофреймворку Javalin: REST-маршрути, Context, middleware, app.exception() | гілка [`lab3`](https://github.com/alwaysfeell/guestbook/tree/lab3) |
| №4 | Spring Boot: IoC-контейнер, біни, DI через конструктор і поле, @Bean, application.yaml | гілка [`lab4`](https://github.com/alwaysfeell/guestbook/tree/lab4) |
| №5 | Spring MVC: контролери @RestController замість сервлетів, DispatcherServlet, @EnableWebMvc, @RestControllerAdvice, CRUD відгуків | гілка [`lab5`](https://github.com/alwaysfeell/guestbook/tree/lab5) |
| №6 | Thymeleaf: серверний рендеринг сторінок, form binding (th:object, th:field), th:if / th:unless, фрагменти, локалізація uk / en | гілка [`lab6`](https://github.com/alwaysfeell/guestbook/tree/lab6) |
 
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
 