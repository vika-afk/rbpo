INSERT INTO plays (id, title, duration_minutes, age_limit, archived) VALUES
    (1, 'Гамлет', 165, 16, FALSE),
    (2, 'Ревизор', 140, 12, FALSE),
    (3, 'Чайка', 150, 16, FALSE),
    (4, 'Старая пьеса', 120, 0, TRUE);

INSERT INTO theatre_halls (id, name, capacity) VALUES
    (1, 'Большой зал', 100),
    (2, 'Малая сцена', 40),
    (3, 'Экспериментальная площадка', 20);

INSERT INTO users (id, name, email, password, role, enabled, blocked) VALUES
    (1, 'Admin', 'admin@example.com', '$2a$10$21B4jzedovJmzdxYMSfTRe.MAhWI8aPoaXhPaH.On6Dz9weqySr/i', 'ADMIN', TRUE, FALSE),
    (2, 'Manager', 'manager@example.com', '$2a$10$21B4jzedovJmzdxYMSfTRe.MAhWI8aPoaXhPaH.On6Dz9weqySr/i', 'MANAGER', TRUE, FALSE),
    (3, 'User', 'user@example.com', '$2a$10$21B4jzedovJmzdxYMSfTRe.MAhWI8aPoaXhPaH.On6Dz9weqySr/i', 'USER', TRUE, FALSE),
    (4, 'Blocked', 'blocked@example.com', '$2a$10$21B4jzedovJmzdxYMSfTRe.MAhWI8aPoaXhPaH.On6Dz9weqySr/i', 'USER', TRUE, TRUE),
    (5, 'Other', 'other@example.com', '$2a$10$21B4jzedovJmzdxYMSfTRe.MAhWI8aPoaXhPaH.On6Dz9weqySr/i', 'USER', TRUE, FALSE);

INSERT INTO performances (id, play_id, hall_id, start_time, base_price, status) VALUES
    (1, 1, 1, now() + interval '1 day' + interval '19 hours', 1500.00, 'SCHEDULED'),
    (2, 2, 2, now() + interval '2 days' + interval '18 hours', 1200.00, 'SCHEDULED'),
    (3, 1, 1, now() + interval '5 days' + interval '19 hours', 1500.00, 'SCHEDULED'),
    (4, 3, 2, now() - interval '5 days', 900.00, 'COMPLETED');

INSERT INTO tickets (id, performance_id, user_id, seat_number, status) VALUES
    (1, 1, 3, 1, 'SOLD'),
    (2, 1, 3, 2, 'SOLD'),
    (3, 1, NULL, 3, 'RESERVED'),
    (4, 4, 3, 10, 'SOLD'),
    (5, 1, 5, 4, 'SOLD');

SELECT setval(pg_get_serial_sequence('plays', 'id'), (SELECT max(id) FROM plays));
SELECT setval(pg_get_serial_sequence('theatre_halls', 'id'), (SELECT max(id) FROM theatre_halls));
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT max(id) FROM users));
SELECT setval(pg_get_serial_sequence('performances', 'id'), (SELECT max(id) FROM performances));
SELECT setval(pg_get_serial_sequence('tickets', 'id'), (SELECT max(id) FROM tickets));
