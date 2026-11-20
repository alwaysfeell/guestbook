insert into users (username, password, role) values
    ('admin',     '$2a$10$vL.n9qdabg2eq64e6uNqHuGjSal91mBbEEes0d9K5D7281vh2bOF.', 'ADMIN'),
    ('olena',     '$2a$10$MkfSmhLh/yE2S5ueP8ixsekSTXU7LxVOzSuumWh.DOx1MyXyH/aRG', 'USER'),
    ('andriy',    '$2a$10$EzSFbd4.j4PUdiBy.rl23.oB4xYZS8k3q4pl3RMVDj6zLYvmiEB46', 'USER'),
    ('vladyslav', '$2a$10$FKGq7MCz1Joj6cEwbtz7rOXbo/aKJTlgdyLdaducs1Ztc.aEjtjva', 'USER'),
    ('iryna',     '$2a$10$GTdS8i/nE/51hnU4bxQ8oe0LFZCxkgAz3pwwB.8FAwEJMw6GNuUO6', 'USER');

insert into books (title, author, pub_year) values
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

insert into comments (book_id, user_id, text, created_at) values
    (12, 1, 'Додано до списку рекомендованої літератури курсу.', current_timestamp - interval '200' hour),
    (1, 2, 'Перечитую щороку, особливо «Заповіт».', current_timestamp - interval '120' hour),
    (6, 5, 'Захоплива пригодницька історія, прочитала за два вечори.', current_timestamp - interval '96' hour),
    (11, 2, 'Після неї по-іншому дивишся на залежності між модулями.', current_timestamp - interval '72' hour),
    (2, 3, 'Повість про громаду, яка вміє захищати себе разом.', current_timestamp - interval '60' hour),
    (10, 4, 'Must-read для кожного Java-розробника: правило бойскаута працює.', current_timestamp - interval '50' hour),
    (3, 2, 'Найкраща драма-феєрія в українській літературі.', current_timestamp - interval '30' hour),
    (8, 4, 'Bounded context і ubiquitous language нарешті стали зрозумілими.', current_timestamp - interval '26' hour),
    (7, 3, 'Repository, Unit of Work, Data Mapper - саме те, що робить Hibernate.', current_timestamp - interval '20' hour),
    (12, 4, 'Розділ про builder і immutable-класи використав у лабораторних.', current_timestamp - interval '5' hour),
    (11, 3, 'Пояснює, чому бізнес-логіка не повинна залежати від БД.', current_timestamp - interval '2' hour),
    (4, 5, 'Неймовірна атмосфера Карпат і гуцульських легенд.', current_timestamp - interval '1' hour);
