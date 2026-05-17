INSERT INTO creators (name, email, created_at)
VALUES ('김강사', 'creator1@example.com', NOW()),
       ('이강사', 'creator2@example.com', NOW());

INSERT INTO klassmates (name, email, phone_number, created_at)
VALUES ('홍길동', 'student1@example.com', '010-1111-2222', NOW()),
       ('김철수', 'student2@example.com', '010-3333-4444', NOW()),
       ('이영희', 'student3@example.com', '010-5555-6666', NOW());

-- OPEN: max=5, remaining=3 (student1·student2 수강 확정, student3 취소)
-- CLOSED: max=2, remaining=0 (student1·student3 수강 확정, student2 대기열)
-- DRAFT: 수강 신청 불가
INSERT INTO klasses (creator_id, title, description, price, status, max_capacity, remaining_capacity, start_date, end_date, created_at, updated_at)
VALUES (1, '스프링 부트 입문', '스프링 부트 기초부터 실전까지', 50000, 'OPEN', 5, 3, '2026-06-01', '2026-08-31', NOW(), NOW()),
       (1, 'JPA 심화', 'JPA와 Hibernate 심화 학습', 80000, 'CLOSED', 2, 0, '2026-06-01', '2026-08-31', NOW(), NOW()),
       (2, 'React 기초', 'React 기초 과정', 60000, 'DRAFT', 15, 15, NULL, NULL, NOW(), NOW());

INSERT INTO enrollments (klassmate_id, klass_id, status, cancel_reason, created_at, updated_at)
VALUES (1, 1, 'CONFIRMED', NULL, NOW(), NOW()),
       (2, 1, 'CONFIRMED', NULL, NOW(), NOW()),
       (3, 1, 'CANCELLED', 'PAYMENT_TIMEOUT', NOW(), NOW()),
       (1, 2, 'CONFIRMED', NULL, NOW(), NOW()),
       (3, 2, 'CONFIRMED', NULL, NOW(), NOW());

INSERT INTO waitlists (klassmate_id, klass_id, created_at)
VALUES (2, 2, NOW());
