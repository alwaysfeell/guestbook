# Guestbook — каталог книг і відгуки (Maven + Jetty 11 + JDBC/H2)

Репозиторій: https://github.com/alwaysfeell/guestbook

| Лабораторна | Що зроблено | Де код |
|---|---|---|
| №1 | Базовий застосунок «Книга відгуків» (один модуль WAR) | тег [`lab1`](https://github.com/alwaysfeell/guestbook/tree/lab1) |
| №2 | Трирівнева архітектура: модулі core / persistence / web, каталог книг, пошук, пагінація, правило видалення за 24 години, ArchUnit | гілка `main` |
 
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
 