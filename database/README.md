# HƯỚNG DẪN CƠ SỞ DỮ LIỆU POSTGRESQL - AQUARIUM 3D PLATFORM

Cơ sở dữ liệu: `aquarium_db`
Hệ quản trị: PostgreSQL 14+ (Hiện đang chạy trên PostgreSQL 18.4)
Mục đích: Phục vụ 158 Use Cases cho sàn E-Commerce, Quản lý kho đa điểm, Mô hình 3D bóc tách linh kiện và Bảo hành sinh vật sống (DOA).

---

## 1. Cấu Trúc Các Tệp Tin Trong Thư Mục

- `01_schema.sql`: Toàn bộ cấu trúc bảng (38 bảng và views), ENUM types, khóa chính UUID v4, khóa ngoại, ràng buộc kiểm tra CHECK, chỉ mục (B-Tree, GIN cho JSONB) và triggers tự động cập nhật thời gian (`updated_at`).
- `02_seed_data.sql`: Dữ liệu mẫu chuẩn hóa bao gồm:
  - 4 vai trò người dùng (Admin, 2 Nhà cung cấp Hà Nội & TP.HCM, Kỹ thuật viên, Khách hàng).
  - 3 kho hàng (Showroom Hà Nội, Showroom TP.HCM, Trại cách ly cá Củ Chi).
  - Cây danh mục sản phẩm thủy sinh.
  - Combo bể Nano Cube bóc tách 6 tầng linh kiện (BOM) khớp với video thực tế.
  - Metadata mô hình 3D (bounding box, định dạng GLB).
  - Quy tắc tương thích sinh học (pH, nhiệt độ, bio-load) giữa Cá Neon Vua, Tép Red Cherry, Cá Thần Tiên.
  - Tồn kho ban đầu theo từng showroom.
- `test_queries.sql`: Các câu lệnh kiểm tra mẫu để xác minh bóc tách BOM 6 tầng, tồn kho đa showroom và cảnh báo tương thích cá cảnh.
- `setup_db.ps1`: Script PowerShell hỗ trợ chạy nhanh khi cần reset hoặc triển khai trên máy mới.

---

## 2. Cách Chạy Lại Cơ Sở Dữ Liệu Khi Cần Reset

### Cách 1: Chạy bằng dòng lệnh PowerShell
```powershell
# Chạy schema DDL
$env:PGPASSWORD="<your_password>"; & "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h localhost -p 5432 -d aquarium_db -f "01_schema.sql"

# Nạp dữ liệu mẫu
$env:PGPASSWORD="<your_password>"; & "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h localhost -p 5432 -d aquarium_db -f "02_seed_data.sql"
```

### Cách 2: Mở bằng pgAdmin / DBeaver / Navicat
1. Kết nối tới Database `aquarium_db`.
2. Mở file `01_schema.sql` -> Bấm Execute (F5).
3. Mở file `02_seed_data.sql` -> Bấm Execute (F5).
