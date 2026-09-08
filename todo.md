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

Nằm ở domain, không nằm ở controller:

- `MessageRules` — ai được sửa/mở/xem, `unlockAt` phải ở tương lai, ẩn content theo role + status.
- `Message` command methods — `compose` / `applyEdit` / `cancel` / `open` / `markAvailable` / `claimRecipient`; `openedAt` không overwrite.

- [x] `unlock_at` phải ở tương lai tại thời điểm tạo.
- [x] Nội dung không được sửa khi `status ∈ {AVAILABLE, OPENED}`.
- [x] Chỉ cho phép sửa khi `LOCKED` (title/content/`unlock_at` nếu vẫn còn tương lai).
- [x] Chỉ người nhận (email khớp user đang đăng nhập) mới được **mở** message.
- [x] `opened_at` chỉ set một lần, không overwrite.
- [x] Người gửi không đọc được content của message gửi cho người khác khi vẫn `LOCKED`?  
  Quyết định: **người gửi được xem message mình tạo** (kể cả LOCKED), người nhận chỉ xem content khi `AVAILABLE`/`OPENED`.
- [x] Khi user đăng ký bằng email đã từng được ghi là recipient → backfill `recipient_user_id`.

---

## 3. Security & Auth

- [x] Spring Security stateless + JWT access token + refresh token.
- [x] Password: BCrypt.
- [x] API:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/refresh`
  - `POST /api/v1/auth/logout`
  - `GET  /api/v1/users/me`
- [x] Validation email/password/displayName.
- [x] CORS cho frontend sau này.
- [x] Rate limit cơ bản cho login/register (optional nhưng nên có).
- [x] Không lộ stacktrace; chuẩn hóa error response.

---

## 4. Message API

- [x] `POST   /api/v1/messages` — tạo message
  - body: title, content, unlockAt, recipientEmail (nếu omitted/self → email của chính user)
- [x] `GET    /api/v1/messages/sent` — danh sách đã gửi (sender)
- [x] `GET    /api/v1/messages/inbox` — danh sách nhận được
  - inbox LOCKED: trả metadata, **không trả content**
  - inbox AVAILABLE/OPENED: trả content
- [x] `GET    /api/v1/messages/{id}` — chi tiết (áp dụng rule ẩn content)
- [x] `PATCH  /api/v1/messages/{id}` — sửa khi còn LOCKED và là sender
- [x] `DELETE /api/v1/messages/{id}` — hủy khi còn LOCKED và là sender → `CANCELLED`
- [x] `POST   /api/v1/messages/{id}/open` — người nhận mở
  - chỉ khi status = AVAILABLE
  - set OPENED + openedAt
  - idempotent: nếu đã OPENED thì trả về message, không đổi openedAt

### 4.1. DTO / validation

- [x] CreateMessageRequest, UpdateMessageRequest, MessageResponse, MessageSummaryResponse
- [x] Bean Validation: title length, content not blank, unlockAt future, email format
- [x] Mapper entity ↔ DTO, ẩn content theo role + status

---

## 5. Unlock scheduler & email

- [x] Job định kỳ (mỗi 30–60 giây): tìm `LOCKED` có `unlock_at <= now()`
- [x] Batch update → `AVAILABLE` + enqueue/send notification
- [x] Xử lý concurrency: `SELECT ... FOR UPDATE SKIP LOCKED` hoặc equivalent
- [x] Email service (Spring Mail)
  - template: subject + body (plain + HTML đơn giản)
  - nội dung: người gửi, title, thời điểm mở, link/hướng dẫn mở inbox
- [x] Retry email khi fail (notification_status = FAILED, job retry)
- [x] Không gửi lại nếu đã SENT
- [x] Local: MailHog/Mailpit; Prod: SMTP env (host, port, user, pass, from)

---

## 6. Exception, logging, observability

- [x] `BusinessException` + error codes (auth + message: `MESSAGE_LOCKED`, `NOT_RECIPIENT`, `MESSAGE_NOT_EDITABLE`, ...)
- [x] `@RestControllerAdvice` — 400/401/403/404/409; không lộ stacktrace
- [ ] Request logging (không log content message đầy đủ ở prod)
- [ ] Actuator health + info (không expose secrets)

---

## 7. Testing

- [x] Unit test domain/service:
  - [x] không sửa sau AVAILABLE/OPENED
  - [x] open chỉ khi AVAILABLE
  - [x] openedAt set 1 lần
  - [x] domain: compose / edit / cancel / open / markAvailable / claimRecipient / ẩn content
  - [x] unlock job chuyển đúng trạng thái
  - [x] email: SENT / FAILED retry / không gửi lại SENT / template không lộ content
- [x] Integration test (Testcontainers PostgreSQL):
  - [x] register/login
  - [x] create message self/other
  - [x] inbox ẩn content khi LOCKED
  - [x] open flow + openedAt
  - [x] email gửi khi unlock (mock mail sender)
- [x] Scheduler test với clock cố định (`Clock` bean để inject thời gian)

---

## 8. Chất lượng & bàn giao

- [ ] `.gitignore`, `.editorconfig`
- [x] OpenAPI/Swagger UI (`/swagger-ui.html`, `/swagger-ui/index.html`, spec `/v3/api-docs`)
- [ ] Seed data dev (1 user + 1 message sắp unlock)
- [x] Frontend website (`frontend/`, Vite + React + TypeScript) — xem mục 9
- [ ] Kiểm tra end-to-end trên UI (register → compose → inbox → open)
- [x] Cập nhật `todo.md`: đánh dấu xong từng hạng mục khi implement

---

## Thứ tự triển khai (đã chốt)

1. ~~Database~~  
2. ~~Entity~~  
3. ~~Auth~~  
4. ~~Message~~  
5. ~~Business Rule~~  
6. ~~Scheduler~~  
7. ~~Email~~  
8. Test  
9. ~~Frontend~~  
10. Admin dashboard  

---

## 9. Frontend website (đã làm)

Stack: **Vite + React 19 + TypeScript + React Router** trong thư mục `frontend/`. Dev server `http://localhost:5173`, proxy `/api` → backend `http://localhost:8080`. Email inbox link mặc định: `http://localhost:5173/inbox`.

### 9.1. Nền tảng

- [x] Scaffold Vite (`react-ts`), `react-router-dom`, favicon phong bì, Google Fonts (Fraunces + Outfit).
- [x] Proxy Vite `/api` → `localhost:8080`; biến `VITE_API_BASE_URL` cho bản production khác origin.
- [x] Cập nhật `MAIL_INBOX_URL` mặc định, `.env.example`, README (cách chạy frontend).
- [x] CORS: cho phép origin Vite (`localhost` / `127.0.0.1`, mọi cổng); login/register không còn 403 Invalid CORS. Vite proxy bỏ header `Origin` khi forward sang backend.

### 9.2. Auth trên UI

- [x] Đăng ký (`POST /api/v1/auth/register`) — email, mật khẩu ≥ 8, tên hiển thị.
- [x] Đăng nhập (`POST /api/v1/auth/login`).
- [x] Lưu access + refresh token + user trên `localStorage`.
- [x] Tự refresh access token khi API trả 401; refresh fail → đăng xuất.
- [x] Đăng xuất gọi `POST /api/v1/auth/logout` rồi xóa session.
- [x] Route bảo vệ (`/inbox`, `/sent`, `/compose`, chi tiết/sửa thư); guest-only cho login/register.
- [x] Nút mắt hiện/ẩn mật khẩu ở đăng nhập và đăng ký.
- [x] Map mã lỗi API sang tiếng Việt (`EMAIL_ALREADY_EXISTS`, `INVALID_CREDENTIALS`, `MESSAGE_LOCKED`, …).

### 9.3. Message trên UI

- [x] Landing: giới thiệu sản phẩm + 3 bước Viết / Khóa / Mở.
- [x] **Viết thư**: cho chính mình hoặc email người khác; chọn ngày/giờ mở (UTC+7) + nút nhanh 1 phút → 1 tuần khi thử.
- [x] **Đã gửi**: thư gửi người khác hiện title/content; thư gửi cho chính mình ẩn đến khi mở. **Hộp thư**: ẩn đến khi mở. Hover phong bì: phóng to nhẹ.
- [x] Chi tiết thư: countdown khi còn khóa; người nhận `AVAILABLE` bấm **Mở tin nhắn** (`POST .../open`).
- [x] Sửa / hủy thư khi còn `LOCKED` và là người gửi (`PATCH` / `DELETE`).
- [x] Badge trạng thái: `LOCKED` / `AVAILABLE` / `OPENED` / `CANCELLED`.

### 9.4. Chạy frontend

```bash
cd frontend
npm install
npm run dev
```

---

## 10. Admin dashboard (chưa làm)

Mục tiêu: trang quản trị cho **ADMIN** theo dõi vận hành (user, message, email unlock), không thay thế hộp thư người dùng. User thường **không** thấy route/API này.

Quyết định đã chốt cho phiên này:

- Role: `USER` | `ADMIN`. Đăng ký công khai luôn tạo `USER`. Không tự nâng quyền trên UI.
- Bootstrap admin: seed / env (`ADMIN_EMAIL` + `ADMIN_PASSWORD`) lúc khởi động nếu chưa có user đó.
- JWT access token gắn `role`; `JwtAuthenticationFilter` map `ROLE_USER` / `ROLE_ADMIN` (hiện filter hard-code `ROLE_USER`).
- `/api/v1/admin/**` chỉ `ROLE_ADMIN`. User thường → 403 `FORBIDDEN`.
- Admin xem **metadata** mọi message. **Content**: chỉ khi `AVAILABLE` / `OPENED` (không đọc thư còn `LOCKED` — giữ invariant privacy).
- Admin **không** mở hộp thư thay người nhận (`POST .../open` vẫn chỉ recipient).
- Soft-disable user: `enabled=false` → không login/refresh; message đã tạo vẫn tồn tại.

### 10.1. Domain, DB, security

- [x] Enum `UserRole`: `USER` | `ADMIN`.
- [x] Flyway `V6__user_role_and_enabled.sql`:
  - `users.role VARCHAR(20) NOT NULL DEFAULT 'USER'` + check `USER`/`ADMIN`
  - `users.enabled BOOLEAN NOT NULL DEFAULT TRUE`
  - index `users(role)` nếu cần lọc admin
- [x] Entity `User`: `role`, `enabled`; default `USER` / `true`.
- [x] `UserPrincipal` + JWT claim `role`; `GET /api/v1/users/me` và `UserResponse` trả `role`.
- [x] `SecurityConfig`: `requestMatchers("/api/v1/admin/**").hasRole("ADMIN")`; bật method security nếu dùng `@PreAuthorize`.
- [x] Login: user `enabled=false` → 401 `INVALID_CREDENTIALS` (cùng message, không lộ “bị khóa”).
- [x] Error codes mới nếu cần: `NOT_ADMIN`, `USER_DISABLED`, `CANNOT_MODIFY_SELF_ROLE`, `LAST_ADMIN`.
- [x] Seed admin từ env (dev + README); không commit mật khẩu thật.

### 10.2. Admin API (`/api/v1/admin`)

Dùng `PageResponse` sẵn có. Không log content message đầy đủ.

- [ ] `GET /api/v1/admin/stats` — tổng user; message theo `status`; notification `PENDING`/`SENT`/`FAILED`; message unlock trong 24h tới.
- [ ] `GET /api/v1/admin/users` — phân trang; query `q` (email / displayName), `enabled`, `role`.
- [ ] `GET /api/v1/admin/users/{id}` — hồ sơ + số message đã gửi / inbox gắn email đó.
- [ ] `PATCH /api/v1/admin/users/{id}` — `enabled`; **không** đổi role qua API công khai (tránh tự phong / tự hạ last admin). Nếu sau này cho đổi role: cấm tự hạ chính mình nếu là admin cuối.
- [ ] `GET /api/v1/admin/messages` — phân trang; filter `status`, `notificationStatus`, `senderEmail`, `recipientEmail`.
- [ ] `GET /api/v1/admin/messages/{id}` — chi tiết vận hành (sender, recipient, status, unlockAt, openedAt, notification); ẩn `content` khi `LOCKED` / `CANCELLED` chưa từng mở.
- [ ] `POST /api/v1/admin/messages/{id}/retry-notification` — chỉ khi `notification_status = FAILED` và status `AVAILABLE`/`OPENED`; set lại `PENDING` để job gửi lại.
- [ ] Không thêm API admin sửa nội dung / đổi `unlockAt` / mở hộp thư hộ user.

### 10.3. Frontend admin

Cùng app `frontend/` (Vite + React). Route `/admin/*`, không tách app mới.

- [ ] `RequireAdmin`: chưa login → `/login`; `role !== ADMIN` → 403 / trang “Không có quyền”.
- [ ] Nav: chỉ ADMIN thấy link **Quản trị**.
- [ ] `/admin` — dashboard: số liệu từ `stats` (thẻ user, message theo trạng thái, email FAILED).
- [ ] `/admin/users` — bảng tìm kiếm, bật/tắt `enabled`.
- [ ] `/admin/users/:id` — chi tiết user.
- [ ] `/admin/messages` — bảng filter status / notification; badge FAILED nổi bật.
- [ ] `/admin/messages/:id` — metadata + content nếu được phép; nút **Gửi lại email** khi FAILED.
- [ ] Map lỗi admin sang tiếng Việt (`FORBIDDEN`, `USER_DISABLED`, …).
- [ ] Style thống nhất Shell hiện tại; bảng/filter rõ, không cần chart library.

### 10.4. Test & bàn giao

- [ ] Unit/integration: user thường 403 `/api/v1/admin/**`; admin 200; JWT có `ROLE_ADMIN`.
- [ ] User disabled không login được.
- [ ] Stats đếm đúng; list filter + pagination.
- [ ] Retry notification: FAILED → PENDING; SENT không retry.
- [ ] Admin không nhận `content` của message `LOCKED`.
- [ ] Cập nhật README + Swagger tag **Admin**.
- [ ] Seed 1 admin + vài message (LOCKED / FAILED) để thử UI.
- [ ] Kiểm tra UI: login admin → dashboard → users → messages → retry email (Mailpit).

---

## Ngoài phạm vi phiên này (ghi nhận, chưa làm)

- OAuth (Google)
- File đính kèm / media
- Multi-language email
- Redis queue (có thể nâng cấp sau nếu volume lớn)
- Admin: xem content thư LOCKED, sửa/xóa hộ user, audit log, 2FA admin
