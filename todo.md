# FUTURE MESSAGE — Backend Implementation Todo

Dự án: **Future Message**  
Stack mục tiêu: **Java 21 + Spring Boot 3 + Spring Security + JWT + Spring Data JPA + PostgreSQL + Flyway + Spring Mail + Spring Scheduler + Docker**  
Mục tiêu: website cho phép người dùng viết tin nhắn gửi cho chính mình hoặc người khác trong tương lai. Message bị khóa đến đúng thời điểm mở, sau đó hệ thống chuyển trạng thái, gửi email, cho phép mở, ghi nhận thời điểm mở, và không cho sửa nội dung sau khi đã mở.

---

## 0. Phạm vi sản phẩm (đã chốt)

- Người dùng tạo message cho chính mình hoặc người khác.
- Message có thời điểm mở (`unlockAt`). Trước thời điểm này: **LOCKED**.
- Khi đến `unlockAt`, hệ thống:
  - Chuyển message sang **AVAILABLE**.
  - Gửi email notification cho người nhận.
  - Cho phép người nhận mở message.
  - Lưu `openedAt` khi message được mở lần đầu.
- Sau khi đã mở: **không cho sửa nội dung**.
- Người nhận có thể chưa có tài khoản tại thời điểm tạo message (gửi theo email).

---

## 1. Khởi tạo dự án & nền tảng kỹ thuật

- [x] Tạo Spring Boot project (`future-message`) với Maven, Java 21, package `com.futuremessage`.
- [x] Cấu trúc module/package theo layered architecture:
  - `config` — security, mail, scheduler, jackson, timezone, exception handler
  - `domain` — entity, enum, value object
  - `repository`
  - `service` + `scheduler`
  - `web` — controller, dto, mapper
  - `security` — JWT, filter, current user
  - `mail`
  - `common` — exception, error code, pagination
- [x] File cấu hình:
  - `application.yml` (dev)
  - `application-prod.yml`
  - `.env.example`
- [x] Docker Compose: PostgreSQL + (tùy chọn) MailHog / Mailpit cho local.
- [x] Dockerfile multi-stage cho app.
- [x] Flyway + thư mục `db/migration`.
- [x] Timezone mặc định: `Asia/Ho_Chi_Minh` (UTC+7). Lưu DB theo `timestamptz` / UTC.
- [x] README: cách chạy local, env vars, API overview, flow nghiệp vụ.

---

## 2. Domain model & database

### 2.1. Enum / trạng thái

- [x] `MessageStatus`
  - `LOCKED` — chưa đến thời điểm mở
  - `AVAILABLE` — đã đến thời điểm mở, chưa được người nhận mở
  - `OPENED` — người nhận đã mở
  - `CANCELLED` — người gửi hủy trước khi mở (nếu cho phép)
- [x] `RecipientType`: `SELF` | `OTHER`
- [x] `NotificationStatus`: `PENDING` | `SENT` | `FAILED`

### 2.2. Bảng & entity

- [x] `users`
  - id, email (unique), password_hash, display_name, email_verified, created_at, updated_at
- [x] `messages`
  - id
  - sender_id (FK users)
  - recipient_email (normalized lowercase)
  - recipient_user_id (nullable, gắn khi user đã tồn tại hoặc sau khi đăng ký)
  - recipient_type
  - title
  - content (text)
  - unlock_at
  - status
  - opened_at (nullable)
  - notification_status
  - notified_at (nullable)
  - created_at, updated_at
  - version (optimistic lock)
- [x] `refresh_tokens` (hoặc equivalent) cho JWT refresh
- [x] Index bắt buộc:
  - `messages(status, unlock_at)` — job unlock
  - `messages(recipient_email)`
  - `messages(sender_id, created_at)`
  - `messages(recipient_user_id, status)`
- [x] Java entity / enum mapping (bước **Entity**)
  - `User`, `Message`, `RefreshToken`
  - `UserRepository`, `MessageRepository`, `RefreshTokenRepository`

### 2.3. Flyway migrations

- [x] `V1__init_users.sql`
- [x] `V2__init_messages.sql`
- [x] `V3__init_refresh_tokens.sql`
- [x] `V4__indexes_and_constraints.sql`

### 2.4. Quy tắc nghiệp vụ (invariant)

- [ ] `unlock_at` phải ở tương lai tại thời điểm tạo.
- [ ] Nội dung không được sửa khi `status ∈ {AVAILABLE, OPENED}`.
- [ ] Chỉ cho phép sửa khi `LOCKED` (title/content/`unlock_at` nếu vẫn còn tương lai).
- [ ] Chỉ người nhận (email khớp user đang đăng nhập) mới được **mở** message.
- [ ] `opened_at` chỉ set một lần, không overwrite.
- [ ] Người gửi không đọc được content của message gửi cho người khác khi vẫn `LOCKED`?  
  Quyết định: **người gửi được xem message mình tạo** (kể cả LOCKED), người nhận chỉ xem content khi `AVAILABLE`/`OPENED`.
- [ ] Khi user đăng ký bằng email đã từng được ghi là recipient → backfill `recipient_user_id`.

---

## 3. Security & Auth

- [ ] Spring Security stateless + JWT access token + refresh token.
- [ ] Password: BCrypt.
- [ ] API:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/refresh`
  - `POST /api/v1/auth/logout`
  - `GET  /api/v1/users/me`
- [ ] Validation email/password/displayName.
- [ ] CORS cho frontend sau này.
- [ ] Rate limit cơ bản cho login/register (optional nhưng nên có).
- [ ] Không lộ stacktrace; chuẩn hóa error response.

---

## 4. Message API

- [ ] `POST   /api/v1/messages` — tạo message
  - body: title, content, unlockAt, recipientEmail (nếu omitted/self → email của chính user)
- [ ] `GET    /api/v1/messages/sent` — danh sách đã gửi (sender)
- [ ] `GET    /api/v1/messages/inbox` — danh sách nhận được
  - inbox LOCKED: trả metadata, **không trả content**
  - inbox AVAILABLE/OPENED: trả content
- [ ] `GET    /api/v1/messages/{id}` — chi tiết (áp dụng rule ẩn content)
- [ ] `PATCH  /api/v1/messages/{id}` — sửa khi còn LOCKED và là sender
- [ ] `DELETE /api/v1/messages/{id}` — hủy khi còn LOCKED và là sender → `CANCELLED`
- [ ] `POST   /api/v1/messages/{id}/open` — người nhận mở
  - chỉ khi status = AVAILABLE
  - set OPENED + openedAt
  - idempotent: nếu đã OPENED thì trả về message, không đổi openedAt

### 4.1. DTO / validation

- [ ] CreateMessageRequest, UpdateMessageRequest, MessageResponse, MessageSummaryResponse
- [ ] Bean Validation: title length, content not blank, unlockAt future, email format
- [ ] Mapper entity ↔ DTO, ẩn content theo role + status

---

## 5. Unlock scheduler & email

- [ ] Job định kỳ (mỗi 30–60 giây): tìm `LOCKED` có `unlock_at <= now()`
- [ ] Batch update → `AVAILABLE` + enqueue/send notification
- [ ] Xử lý concurrency: `SELECT ... FOR UPDATE SKIP LOCKED` hoặc equivalent
- [ ] Email service (Spring Mail)
  - template: subject + body (plain + HTML đơn giản)
  - nội dung: người gửi, title, thời điểm mở, link/hướng dẫn mở inbox
- [ ] Retry email khi fail (notification_status = FAILED, job retry)
- [ ] Không gửi lại nếu đã SENT
- [ ] Local: MailHog/Mailpit; Prod: SMTP env (host, port, user, pass, from)

---

## 6. Exception, logging, observability

- [ ] `BusinessException` + error codes (`MESSAGE_LOCKED`, `MESSAGE_NOT_EDITABLE`, `NOT_RECIPIENT`, `UNLOCK_NOT_REACHED`, ...)
- [ ] `@RestControllerAdvice` — 400/401/403/404/409/422
- [ ] Request logging (không log content message đầy đủ ở prod)
- [ ] Actuator health + info (không expose secrets)

---

## 7. Testing

- [ ] Unit test domain/service:
  - không sửa sau AVAILABLE/OPENED
  - open chỉ khi AVAILABLE
  - openedAt set 1 lần
  - unlock job chuyển đúng trạng thái
- [ ] Integration test (Testcontainers PostgreSQL):
  - register/login
  - create message self/other
  - inbox ẩn content khi LOCKED
  - open flow + openedAt
  - email gửi khi unlock (mock mail sender)
- [ ] Scheduler test với clock cố định (`Clock` bean để inject thời gian)

---

## 8. Chất lượng & bàn giao

- [ ] `.gitignore`, `.editorconfig`
- [ ] OpenAPI/Swagger UI (`/swagger-ui`)
- [ ] Seed data dev (1 user + 1 message sắp unlock)
- [ ] Kiểm tra end-to-end bằng curl/httpie (hoặc test) vì chưa có frontend
- [ ] Cập nhật `todo.md`: đánh dấu xong từng hạng mục khi implement

---

## Thứ tự triển khai (đã chốt)

1. ~~Database~~  
2. ~~Entity~~  
3. Auth  
4. Message  
5. Business Rule  
6. Scheduler  
7. Email  
8. Test  

---

## Ngoài phạm vi phiên này (ghi nhận, chưa làm)

- Frontend website
- OAuth (Google)
- File đính kèm / media
- Multi-language email
- Admin dashboard
- Redis queue (có thể nâng cấp sau nếu volume lớn)
