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
    (12, 1, 'Додано до списку рекомендованої літератури курсу.', dateadd('HOUR', -200, current_timestamp)),
    (1, 2, 'Перечитую щороку, особливо «Заповіт».', dateadd('HOUR', -120, current_timestamp)),
    (6, 5, 'Захоплива пригодницька історія, прочитала за два вечори.', dateadd('HOUR', -96, current_timestamp)),
    (11, 2, 'Після неї по-іншому дивишся на залежності між модулями.', dateadd('HOUR', -72, current_timestamp)),
    (2, 3, 'Повість про громаду, яка вміє захищати себе разом.', dateadd('HOUR', -60, current_timestamp)),
    (10, 4, 'Must-read для кожного Java-розробника: правило бойскаута працює.', dateadd('HOUR', -50, current_timestamp)),
    (3, 2, 'Найкраща драма-феєрія в українській літературі.', dateadd('HOUR', -30, current_timestamp)),
    (8, 4, 'Bounded context і ubiquitous language нарешті стали зрозумілими.', dateadd('HOUR', -26, current_timestamp)),
    (7, 3, 'Repository, Unit of Work, Data Mapper - саме те, що робить Hibernate.', dateadd('HOUR', -20, current_timestamp)),
    (12, 4, 'Розділ про builder і immutable-класи використав у лабораторних.', dateadd('HOUR', -5, current_timestamp)),
    (11, 3, 'Пояснює, чому бізнес-логіка не повинна залежати від БД.', dateadd('HOUR', -2, current_timestamp)),
    (4, 5, 'Неймовірна атмосфера Карпат і гуцульських легенд.', dateadd('HOUR', -1, current_timestamp));
