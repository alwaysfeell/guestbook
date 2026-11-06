insert into books(title, author, pub_year) values
    ('Кобзар', 'Тарас Шевченко', 1840),
    ('Захар Беркут', 'Іван Франко', 1883),
    ('Лісова пісня', 'Леся Українка', 1911),
    ('Тіні забутих предків', 'Михайло Коцюбинський', 1911),
    ('Місто', 'Валер''ян Підмогильний', 1928),
    ('Тигролови', 'Іван Багряний', 1944),
    ('Patterns of Enterprise Application Architecture', 'Martin Fowler', 2002),
    ('Domain-Driven Design', 'Eric Evans', 2003),
    ('Head First Design Patterns', 'Eric Freeman', 2004),
    ('Clean Code', 'Robert C. Martin', 2008),
    ('Clean Architecture', 'Robert C. Martin', 2017),
    ('Effective Java', 'Joshua Bloch', 2018);

insert into comments(book_id, author, text, created_at) values
    (11, 'Олена', 'Старий відгук: йому вже 2 доби, видалити не можна (409)', dateadd('HOUR', -48, current_timestamp)),
    (11, 'Андрій', 'Пояснює, чому бізнес-логіка не повинна залежати від БД', current_timestamp);
