# Future Message

Website cho phép người dùng viết tin nhắn gửi cho chính mình hoặc người khác trong tương lai. Message bị khóa đến đúng `unlockAt`. Khi đến hạn, hệ thống chuyển trạng thái, gửi email, cho phép người nhận mở, và ghi nhận thời điểm mở.

## Stack

- Java 21
- Spring Boot 3.5
- Spring Data JPA + PostgreSQL
- Flyway
- Spring Mail
- Spring Scheduler
- springdoc-openapi (Swagger UI)
- Docker Compose (PostgreSQL + Mailpit)
- Frontend: Vite + React + TypeScript (`frontend/`)

Timezone mặc định của ứng dụng: **Asia/Ho_Chi_Minh**. Timestamp trong database lưu UTC (`timestamptz`).

## Chạy local

### 1. Yêu cầu

- JDK 21 (`JAVA_HOME` trỏ tới JDK 21)
- Docker Desktop
- Maven Wrapper đã có sẵn (`mvnw` / `mvnw.cmd`) — không cần cài Maven toàn cục
- Node.js 20+ (để chạy frontend)

### 2. Hạ tầng local

```bash
docker compose up -d
```

- PostgreSQL: `localhost:5432` / database `future_message` / user `future` / password `future`
- Mailpit SMTP: `localhost:1025`
- Mailpit UI: [http://localhost:8025](http://localhost:8025)

Sao chép biến môi trường mẫu nếu cần:

```bash
copy .env.example .env
```

### 3. Chạy ứng dụng

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Hoặc set `JAVA_HOME` rồi chạy IDE. App lắng nghe `http://localhost:8080`.

### 4. Chạy frontend

```bash
cd frontend
npm install
npm run dev
```

Mở [http://localhost:5173](http://localhost:5173). Vite proxy `/api` tới backend `http://localhost:8080` (CORS cũng cho phép origin này).

Luồng trên UI: Đăng ký / Đăng nhập → **Viết thư** (cho mình hoặc email khác, chọn `unlockAt`) → **Đã gửi** / **Hộp thư** → mở chi tiết. Inbox `LOCKED` không hiện nội dung. Khi thư `AVAILABLE`, người nhận bấm **Mở tin nhắn**. Access token hết hạn 15 phút — frontend tự gọi refresh.

### 5. Test API bằng Swagger UI

Mở [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) (redirect tới `/swagger-ui/index.html`). Spec OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs).

Hai URL này **không cần token**. Profile `prod` tắt Swagger (`springdoc.*.enabled=false`).

**Luồng thử nhanh**

1. Mở nhóm **Auth** → `POST /api/v1/auth/register` → **Try it out** → giữ example (`ada@example.com` / `password1`) hoặc đổi email nếu đã đăng ký → **Execute**.
2. Copy `accessToken` trong Response body (chuỗi JWT, không copy chữ `"accessToken"`).
3. Bấm **Authorize** (ổ khóa góc phải) → dán token vào ô `bearerAuth` → **Authorize** → **Close**.  
   **Không** gõ `Bearer ` trước token: Swagger tự thêm header `Authorization: Bearer <token>`.
4. Gọi `GET /api/v1/users/me` — phải ra 200 và email vừa đăng ký.
5. Nhóm **Messages** → `POST /api/v1/messages`:
   - Gửi cho chính mình: xóa `recipientEmail` hoặc để đúng email của bạn.
   - Gửi cho người khác: `recipientEmail` khác, ví dụ `bob@example.com`.
   - `unlockAt` phải ở **tương lai** (example `2030-01-01T00:00:00+07:00` là hợp lệ).
6. `GET /api/v1/messages/sent` — sender luôn thấy `content`.
7. Nếu muốn thử inbox ẩn content: register user thứ hai, Authorize bằng token của Bob, gọi `GET /api/v1/messages/inbox`. Message `LOCKED` **không** có field `content`.

Access token local hết hạn sau **15 phút**. Khi 401 `INVALID_TOKEN`, gọi `POST /api/v1/auth/refresh` với `refreshToken`, copy access token mới, Authorize lại.

`POST /api/v1/messages/{id}/open` chỉ thành công khi message đã `AVAILABLE` (scheduler unlock mỗi 30 giây sau `unlockAt`). Muốn thử ngay: tạo message với `unlockAt` vài phút nữa, đợi job, rồi Open.

### 6. Chạy test

Cần JDK 21. Integration test Postgres cần **Docker Desktop** (Testcontainers kéo `postgres:16-alpine`).

```bash
# Windows
.\mvnw.cmd test

# Chỉ integration test Postgres
.\mvnw.cmd test -Dtest=PostgresIntegrationTest
```

Unit / controller test dùng H2 in-memory. `PostgresIntegrationTest` chạy Flyway + `FOR UPDATE SKIP LOCKED` trên PostgreSQL 16 (Testcontainers). Docker Engine 29+ cần Docker API ≥ 1.44 — project đã set trong `src/test/resources/docker-java.properties`.

### 7. Chạy bằng Docker (sau khi đã `docker compose up -d` postgres/mailpit)

```bash
docker build -t future-message .
docker run --rm -p 8080:8080 --network host ^
  -e DB_URL=jdbc:postgresql://localhost:5432/future_message ^
  -e DB_USERNAME=future ^
  -e DB_PASSWORD=future ^
  future-message
```

Trên Linux/macOS dùng `\` thay cho `^`.

## Biến môi trường

| Biến | Mặc định | Mô tả |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` (không set) | Dùng `prod` trên production |
| `SERVER_PORT` | `8080` | Cổng HTTP |
| `DB_URL` | `jdbc:postgresql://localhost:5432/future_message` | JDBC URL |
| `DB_USERNAME` | `future` | User Postgres |
| `DB_PASSWORD` | `future` | Password Postgres |
| `MAIL_HOST` | `localhost` | SMTP host |
| `MAIL_PORT` | `1025` | SMTP port (Mailpit local) |
| `MAIL_USERNAME` | trống | SMTP user (prod) |
| `MAIL_PASSWORD` | trống | SMTP password (prod) |
| `MAIL_FROM` | `noreply@futuremessage.local` | Địa chỉ gửi |
| `MAIL_INBOX_URL` | `http://localhost:5173/inbox` | Link inbox trong email thông báo |
| `JWT_SECRET` | secret local (dev only) | HMAC key cho access token; **tối thiểu 32 bytes**. Prod bắt buộc set. |
| `ADMIN_EMAIL` | `admin@futuremessage.local` (dev) | Email tài khoản ADMIN bootstrap. Trống = không tạo. Prod: set khi cần tạo admin lần đầu. |
| `ADMIN_PASSWORD` | `adminpass1` (dev only) | Mật khẩu admin bootstrap (≥ 8). **Không** dùng giá trị này trên production. |
| `ADMIN_DISPLAY_NAME` | `Admin` | Tên hiển thị khi bootstrap tạo admin mới. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:[*],http://127.0.0.1:[*]` | Origin frontend được phép. `[*]` = mọi cổng (Vite 5173, v.v.) |
| `APP_SCHEDULER_UNLOCK_ENABLED` | `true` | Tắt job unlock (test profile đặt `false`) |
| `APP_SCHEDULER_UNLOCK_INTERVAL` | `30s` | Fixed delay giữa hai lần chạy job unlock |
| `APP_SCHEDULER_UNLOCK_BATCH_SIZE` | `50` | Số message khóa tối đa mỗi lần chạy |
| `APP_SCHEDULER_NOTIFICATION_ENABLED` | `true` | Tắt job email (test profile đặt `false`) |
| `APP_SCHEDULER_NOTIFICATION_INTERVAL` | `30s` | Fixed delay giữa hai lần chạy job email |
| `APP_SCHEDULER_NOTIFICATION_BATCH_SIZE` | `50` | Số email tối đa mỗi lần chạy |

## Flow nghiệp vụ

1. User tạo message cho chính mình hoặc email người khác, kèm `unlockAt` ở tương lai.
2. Message ở trạng thái `LOCKED`. Người nhận chưa thấy nội dung.
3. Khi đến `unlockAt`, scheduler unlock (mỗi 30 giây) khóa hàng `LOCKED` đến hạn bằng `FOR UPDATE SKIP LOCKED`, gọi `Message.markAvailable`, chuyển sang `AVAILABLE`.
4. Scheduler email (mỗi 30 giây) gửi thông báo cho `recipientEmail`. Thành công → `notificationStatus = SENT`. SMTP fail → `FAILED`, lần sau retry. Đã `SENT` thì không gửi lại.
5. Người nhận gọi API mở message. Hệ thống lưu `openedAt` và chuyển sang `OPENED`.
6. Sau khi `AVAILABLE` / `OPENED`, nội dung không được sửa.

## Business rules (domain)

Rule nằm ở `MessageRules` (ai được làm gì) và command methods trên `Message` (chuyển trạng thái). Service chỉ orchestration.

| Rule | Chỗ enforce |
| --- | --- |
| `unlockAt` phải ở tương lai khi tạo / đổi hạn | `MessageRules.requireFutureUnlockAt`, `Message.compose` / `applyEdit` |
| Chỉ sửa / hủy khi `LOCKED` | `Message.applyEdit`, `Message.cancel` |
| Chỉ người gửi được sửa / hủy | `MessageRules.requireSender` |
| Chỉ người nhận được mở | `MessageRules.requireRecipient` |
| Mở chỉ khi `AVAILABLE`; đã `OPENED` thì idempotent | `Message.open` |
| `openedAt` ghi một lần, không overwrite | `Message.setOpenedAt` |
| Người gửi luôn thấy content; người nhận chỉ thấy khi `AVAILABLE`/`OPENED` | `MessageRules.visibleContent` |
| Admin chỉ thấy content khi `AVAILABLE`/`OPENED` | `MessageRules.visibleContentForAdmin` |
| Đăng ký bằng email đã là recipient → gắn `recipient_user_id` | `Message.claimRecipient` + `AuthService.register` (bulk update) |
| `LOCKED` → `AVAILABLE` khi `unlockAt <= now` | `Message.markAvailable` — `UnlockScheduler` → `UnlockService` |
| Email unlock: PENDING/FAILED → SENT; không gửi lại SENT | `Message.markNotificationSent` / `markNotificationFailed` — `NotificationScheduler` → `NotificationService` |
| Admin retry email: FAILED → PENDING (chỉ AVAILABLE/OPENED) | `Message.queueNotificationRetry` — `AdminService.retryNotification` |

## API overview

Auth, Message và Admin API đã implement. Frontend nằm ở `frontend/` ([http://localhost:5173](http://localhost:5173)). Cách thử API không cần UI: [Swagger UI](http://localhost:8080/swagger-ui.html) (mục **Test API bằng Swagger UI** ở trên). Tag **Admin** trong Swagger cần token của user `ADMIN`.

### Auth

| Method | Path | Auth | Mô tả |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | public | Đăng ký, trả access + refresh token |
| `POST` | `/api/v1/auth/login` | public | Đăng nhập, trả access + refresh token |
| `POST` | `/api/v1/auth/refresh` | public (body: refresh token) | Xoay refresh token, cấp access token mới |
| `POST` | `/api/v1/auth/logout` | public (body: refresh token) | Xóa refresh token |
| `GET` | `/api/v1/users/me` | Bearer access token | Thông tin user hiện tại (`role`, `enabled`) |

Ví dụ:

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"ada@example.com\",\"password\":\"password1\",\"displayName\":\"Ada\"}"
```

Access token gửi header `Authorization: Bearer <token>`. Refresh token chỉ gửi qua body, không lưu raw trong database (chỉ lưu SHA-256 hash).

Đăng ký công khai luôn tạo `role=USER`. Local: lần đầu chạy app, bootstrap tạo ADMIN `admin@futuremessage.local` / `adminpass1` nếu email đó chưa tồn tại. User `enabled=false` không login/refresh được (cùng lỗi `INVALID_CREDENTIALS` / `INVALID_REFRESH_TOKEN`, không lộ “bị khóa”). API `/api/v1/admin/**` yêu cầu `ROLE_ADMIN`.

### Messages

Mọi endpoint dưới đây cần header `Authorization: Bearer <accessToken>`.

| Method | Path | Mô tả |
| --- | --- | --- |
| `POST` | `/api/v1/messages` | Tạo message (`title`, `content`, `unlockAt`; `recipientEmail` optional — bỏ trống = gửi cho chính mình) |
| `GET` | `/api/v1/messages/sent` | Danh sách đã gửi (sender luôn thấy `content`) |
| `GET` | `/api/v1/messages/inbox` | Hộp thư đến. `LOCKED`: không trả `content`. `AVAILABLE`/`OPENED`: trả `content`. Không gồm `CANCELLED`. |
| `GET` | `/api/v1/messages/{id}` | Chi tiết. Người lạ → 404. Người nhận chỉ thấy `content` khi `AVAILABLE`/`OPENED`. |
| `PATCH` | `/api/v1/messages/{id}` | Sửa `title`/`content`/`unlockAt` khi còn `LOCKED` và là sender |
| `DELETE` | `/api/v1/messages/{id}` | Hủy khi còn `LOCKED` và là sender → `CANCELLED` (soft delete) |
| `POST` | `/api/v1/messages/{id}/open` | Người nhận mở khi `AVAILABLE`. Idempotent nếu đã `OPENED`. |

Query `sent` / `inbox`: `page` (mặc định 0), `size` (mặc định 20, tối đa 100).

### Admin

Chỉ `ROLE_ADMIN`. User thường → 403 `FORBIDDEN`. Trang vận hành: xem user/message/email unlock, **không** thay hộp thư người dùng. Admin **không** được sửa nội dung, đổi `unlockAt`, hay gọi `POST /messages/{id}/open` hộ người nhận.

Content: admin chỉ thấy khi message `AVAILABLE` hoặc `OPENED`. `LOCKED` / `CANCELLED` trả metadata, không có field `content`.

| Method | Path | Mô tả |
| --- | --- | --- |
| `GET` | `/api/v1/admin/stats` | Tổng user; message theo `status`; notification `PENDING`/`SENT`/`FAILED`; số message `LOCKED` sẽ unlock trong 24h tới |
| `GET` | `/api/v1/admin/users` | Phân trang. Query `q` (email / displayName), `enabled`, `role` |
| `GET` | `/api/v1/admin/users/{id}` | Hồ sơ + `sentCount` / `inboxCount` |
| `PATCH` | `/api/v1/admin/users/{id}` | Body `{ "enabled": true\|false }`. Không đổi role |
| `GET` | `/api/v1/admin/messages` | Phân trang metadata (không `content`). Filter `status`, `notificationStatus`, `senderEmail`, `recipientEmail` |
| `GET` | `/api/v1/admin/messages/{id}` | Chi tiết vận hành; ẩn `content` khi `LOCKED` / `CANCELLED` |
| `POST` | `/api/v1/admin/messages/{id}/retry-notification` | Chỉ khi `notificationStatus=FAILED` và status `AVAILABLE`/`OPENED` → set `PENDING` để job gửi lại. `SENT` → 409 `NOTIFICATION_NOT_RETRYABLE` |

Ví dụ tạo message cho chính mình:

```bash
curl -s -X POST http://localhost:8080/api/v1/messages ^
  -H "Authorization: Bearer ACCESS_TOKEN" ^
  -H "Content-Type: application/json" ^
  -d "{\"title\":\"To future me\",\"content\":\"Keep going\",\"unlockAt\":\"2030-01-01T00:00:00+07:00\"}"
```

## Scheduler (unlock)

Job chạy trên 1 thread (`fm-scheduler-`), fixed delay 30s (dev/prod). Profile test/H2 tắt job (`app.scheduler.unlock.enabled=false`) vì H2 không dùng `FOR UPDATE SKIP LOCKED` như PostgreSQL. Integration test Postgres (`PostgresIntegrationTest`) gọi `UnlockService` trực tiếp trên Testcontainers.

Mỗi lần chạy:

1. `SELECT id FROM messages WHERE status = 'LOCKED' AND unlock_at <= now ORDER BY unlock_at LIMIT batchSize FOR UPDATE SKIP LOCKED`
2. Load entity, gọi `Message.markAvailable(now)` → `AVAILABLE`
3. `notificationStatus` giữ `PENDING` để job email gửi (không gửi lại nếu đã `SENT`)

Nhiều instance app: `SKIP LOCKED` bỏ qua hàng instance khác đang giữ. Cùng instance: pool size 1 nên job unlock và job email không chồng.

## Email (unlock notification)

Job chạy trên cùng thread pool (`fm-scheduler-`), fixed delay 30s (dev/prod). Profile test tắt job (`app.scheduler.notification.enabled=false`).

Mỗi lần chạy:

1. `SELECT id FROM messages WHERE status IN ('AVAILABLE', 'OPENED') AND notification_status IN ('PENDING', 'FAILED') ORDER BY unlock_at LIMIT batchSize FOR UPDATE SKIP LOCKED`
2. Compose email (subject + plain + HTML): người gửi, title, thời điểm mở, link inbox. **Không** nhúng `content` của message.
3. Gửi SMTP. Thành công → `Message.markNotificationSent(now)` (`SENT` + `notifiedAt`). Fail → `Message.markNotificationFailed()` (`FAILED`, lần sau retry).

Local: Mailpit SMTP `localhost:1025`, UI [http://localhost:8025](http://localhost:8025). Prod: `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` / `MAIL_FROM` / `MAIL_INBOX_URL`.

## Cấu trúc package

```
com.futuremessage
├── config      # timezone, Jackson, scheduler, ...
├── domain      # entity, enum, MessageRules
├── repository
├── service
├── scheduler   # UnlockScheduler — trigger job unlock
├── web         # controller, dto, mapper
├── security    # JWT, filter, current user
├── mail
└── common      # exception, error code, pagination
```

Schema database do Flyway quản lý tại `src/main/resources/db/migration`. Migrations sẽ được thêm ở bước domain model.
