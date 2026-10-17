INSERT INTO plays (id, title, duration_minutes, age_limit, archived) VALUES
    (1, 'Гамлет', 165, 16, FALSE),
    (2, 'Ревизор', 140, 12, FALSE),
    (3, 'Чайка', 150, 16, FALSE),
    (4, 'Старая пьеса', 120, 0, TRUE);

INSERT INTO theatre_halls (id, name, capacity) VALUES
    (1, 'Большой зал', 100),
    (2, 'Малая сцена', 40),
    (3, 'Экспериментальная площадка', 20);

INSERT INTO users (id, name, email) VALUES
    (1, 'Иван Петров', 'ivan.petrov@example.com'),
    (2, 'Мария Сидорова', 'maria.sidorova@example.com'),
    (3, 'Алексей Кузнецов', 'alexey.kuznetsov@example.com'),
    (4, 'Ольга Смирнова', 'olga.smirnova@example.com');

INSERT INTO performances (id, play_id, hall_id, start_time, base_price, status) VALUES
    (1, 1, 1, now() + interval '1 day' + interval '19 hours', 1500.00, 'SCHEDULED'),
    (2, 2, 2, now() + interval '2 days' + interval '18 hours', 1200.00, 'SCHEDULED'),
    (3, 1, 1, now() + interval '5 days' + interval '19 hours', 1500.00, 'SCHEDULED');

INSERT INTO performances (id, play_id, hall_id, start_time, base_price, status) VALUES
    (4, 3, 2, now() - interval '5 days', 900.00, 'COMPLETED');

INSERT INTO tickets (id, performance_id, user_id, seat_number, status) VALUES
    (1, 1, 1, 1, 'SOLD'),
    (2, 1, 2, 2, 'SOLD'),
    (3, 1, NULL, 3, 'RESERVED'),
    (4, 4, 3, 10, 'SOLD');

SELECT setval(pg_get_serial_sequence('plays', 'id'), (SELECT max(id) FROM plays));
SELECT setval(pg_get_serial_sequence('theatre_halls', 'id'), (SELECT max(id) FROM theatre_halls));
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT max(id) FROM users));
SELECT setval(pg_get_serial_sequence('performances', 'id'), (SELECT max(id) FROM performances));
SELECT setval(pg_get_serial_sequence('tickets', 'id'), (SELECT max(id) FROM tickets));
