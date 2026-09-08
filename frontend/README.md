# Future Message — Frontend

Vite + React + TypeScript. Giao diện cho API `http://localhost:8080`.

## Chạy local

Cần backend đang chạy (PostgreSQL + Spring Boot, xem README gốc).

```bash
cd frontend
npm install
npm run dev
```

Mở [http://localhost:5173](http://localhost:5173). Dev server proxy `/api` tới `http://localhost:8080`.

## Build

```bash
npm run build
npm run preview
```

Production: set `VITE_API_BASE_URL` (ví dụ `https://api.example.com`) nếu frontend không cùng origin với API.
