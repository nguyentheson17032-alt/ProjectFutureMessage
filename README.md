# Future Message

Backend cho phép người dùng viết tin nhắn gửi cho chính mình hoặc người khác trong tương lai. Message bị khóa đến đúng `unlockAt`. Khi đến hạn, hệ thống chuyển trạng thái, gửi email, cho phép người nhận mở, và ghi nhận thời điểm mở.

## Stack

- Java 21
- Spring Boot 3.5
- Spring Data JPA + PostgreSQL
- Flyway
- Spring Mail
- Spring Scheduler
- Docker Compose (PostgreSQL + Mailpit)

Timezone mặc định của ứng dụng: **Asia/Ho_Chi_Minh**. Timestamp trong database lưu UTC (`timestamptz`).

## Chạy local

### 1. Yêu cầu

- JDK 21 (`JAVA_HOME` trỏ tới JDK 21)
- Docker Desktop
- Maven Wrapper đã có sẵn (`mvnw` / `mvnw.cmd`) — không cần cài Maven toàn cục

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

### 4. Chạy bằng Docker (sau khi đã `docker compose up -d` postgres/mailpit)

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

## Flow nghiệp vụ

1. User tạo message cho chính mình hoặc email người khác, kèm `unlockAt` ở tương lai.
2. Message ở trạng thái `LOCKED`. Người nhận chưa thấy nội dung.
3. Khi đến `unlockAt`, scheduler chuyển sang `AVAILABLE` và gửi email cho người nhận.
4. Người nhận gọi API mở message. Hệ thống lưu `openedAt` và chuyển sang `OPENED`.
5. Sau khi `AVAILABLE` / `OPENED`, nội dung không được sửa.

## API overview

Các endpoint dưới đây là kế hoạch. Phần auth và message API sẽ được implement ở các bước tiếp theo.

### Auth

| Method | Path | Mô tả |
| --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Đăng ký |
| `POST` | `/api/v1/auth/login` | Đăng nhập, nhận JWT |
| `POST` | `/api/v1/auth/refresh` | Làm mới access token |
| `POST` | `/api/v1/auth/logout` | Đăng xuất |
| `GET` | `/api/v1/users/me` | Thông tin user hiện tại |

### Messages

| Method | Path | Mô tả |
| --- | --- | --- |
| `POST` | `/api/v1/messages` | Tạo message |
| `GET` | `/api/v1/messages/sent` | Danh sách đã gửi |
| `GET` | `/api/v1/messages/inbox` | Hộp thư đến (ẩn content khi `LOCKED`) |
| `GET` | `/api/v1/messages/{id}` | Chi tiết (ẩn content theo rule) |
| `PATCH` | `/api/v1/messages/{id}` | Sửa khi còn `LOCKED` |
| `DELETE` | `/api/v1/messages/{id}` | Hủy khi còn `LOCKED` |
| `POST` | `/api/v1/messages/{id}/open` | Người nhận mở message |

## Cấu trúc package

```
com.futuremessage
├── config      # timezone, Jackson, scheduler, ...
├── domain      # entity, enum, value object
├── repository
├── service
├── scheduler   # job unlock + gửi mail
├── web         # controller, dto, mapper
├── security    # JWT, filter, current user
├── mail
└── common      # exception, error code, pagination
```

Schema database do Flyway quản lý tại `src/main/resources/db/migration`. Migrations sẽ được thêm ở bước domain model.
