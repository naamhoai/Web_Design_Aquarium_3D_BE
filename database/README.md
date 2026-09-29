# HƯỚNG DẪN CƠ SỞ DỮ LIỆU POSTGRESQL - AQUARIUM 3D PLATFORM

Cơ sở dữ liệu: `aquarium_db`
Hệ quản trị: PostgreSQL 14+ (đã kiểm thử trên PostgreSQL 18)

---

## 1. Các tệp trong thư mục

- `01_schema.sql`: toàn bộ cấu trúc bảng (38 bảng), ENUM, khóa UUID, khóa ngoại, CHECK, chỉ mục và trigger `updated_at`.
- `02_seed_data.sql`: dữ liệu mẫu cho môi trường **dev** (tài khoản, 2 nhà cung cấp, 3 kho, danh mục, combo Nano Cube
  bóc tách 6 tầng, metadata 3D, quy tắc sinh học, tồn kho).
- `03_security_and_configurator.sql`: migration **chạy được nhiều lần** — cột phục vụ refresh token xoay vòng,
  kho giữ hàng cho từng dòng đơn, sửa hash mật khẩu tài khoản mẫu, đồng bộ sequence, danh mục linh kiện Studio 3D
  (nguồn giá khi đặt bể tự thiết kế) và dữ liệu sinh học bổ sung.
- `test_queries.sql`: câu lệnh kiểm tra BOM 6 tầng, tồn kho đa showroom và cảnh báo tương thích.

Tất cả các file đều bắt đầu bằng `SET client_encoding = 'UTF8';` để tiếng Việt không bị lỗi khi chạy từ PowerShell/CMD.

Tài khoản mẫu có mật khẩu dev `Dev@123456` — **đổi mật khẩu hoặc xóa trước khi triển khai thật**.

---

## 2. Cài mới

```powershell
$env:PGPASSWORD = Read-Host "Mật khẩu PostgreSQL"
$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
& $psql -U postgres -h localhost -p 5432 -d aquarium_db -v ON_ERROR_STOP=1 -f "01_schema.sql"
& $psql -U postgres -h localhost -p 5432 -d aquarium_db -v ON_ERROR_STOP=1 -f "02_seed_data.sql"
& $psql -U postgres -h localhost -p 5432 -d aquarium_db -v ON_ERROR_STOP=1 -f "03_security_and_configurator.sql"
```

## 3. Nâng cấp DB đã tạo từ phiên bản trước

Chỉ cần chạy:

```powershell
& $psql -U postgres -h localhost -p 5432 -d aquarium_db -v ON_ERROR_STOP=1 -f "03_security_and_configurator.sql"
```

## 4. Dùng pgAdmin / DBeaver

Kết nối `aquarium_db`, mở và chạy (Execute) lần lượt `01_schema.sql` → `02_seed_data.sql` → `03_security_and_configurator.sql`.

## 5. Docker

`docker compose up -d postgres` (trong thư mục `Web_Design_Aquarium_3D_BE`) tự chạy cả 3 file khi tạo volume lần đầu.
