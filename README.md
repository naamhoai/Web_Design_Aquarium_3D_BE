# Web_Design_Aquarium_3D_BE

Backend microservices (Spring Boot 3.3 / Java 21) cho sàn thương mại điện tử bể cá thủy sinh AquaRealm.

| Service | Cổng | Chức năng |
|---|---|---|
| `gateway-service` | 8080 | Cổng vào duy nhất: định tuyến, CORS, giới hạn tần suất, giới hạn kích thước request |
| `identity-service` | 8081 | Đăng ký / đăng nhập, JWT, refresh token xoay vòng, chống dò mật khẩu |
| `supplier-service` | 8082 | Nhà cung cấp, kho / showroom, duyệt gian hàng (admin) |
| `catalog-service` | 8083 | Danh mục, sản phẩm, biến thể (chỉ đọc, công khai) |
| `aquarium-3d-service` | 8084 | BOM bóc tách 6 tầng, kiểm tra tương thích sinh học, bản vẽ 3D |
| `inventory-service` | 8085 | Tồn kho đa kho, cách ly kiểm dịch, ghi nhận hao hụt |
| `order-service` | 8086 | Giỏ hàng, đặt hàng (tách đơn theo nhà cung cấp), xử lý đơn con |
| `common-core` | – | Thư viện dùng chung: `ApiResponse`, xử lý lỗi, **bảo mật JWT** |

## Chạy trên máy dev

1. Tạo database `aquarium_db` và chạy lần lượt `database/01_schema.sql`, `02_seed_data.sql`, `03_security_and_configurator.sql` (xem [database/README.md](database/README.md)).
   DB đã có từ phiên bản cũ: **chỉ cần chạy `03_security_and_configurator.sql`**.
2. Sao chép `.env.example` thành `.env`, điền `POSTGRES_PASSWORD` và `JWT_SECRET`.
3. Chạy `start_dev.bat` ở thư mục gốc dự án (tự nạp `.env`, build `common-core`, mở 7 service + frontend).
   Dừng bằng `stop_dev.bat`.

Tài khoản mẫu (chỉ dev, mật khẩu `Dev@123456`): `customer_nam@gmail.com`, `supplier_hanoi@aquarium3d.vn`,
`supplier_hcm@aquarium3d.vn`, `admin@aquarium3d.vn`, `tech_dung@aquarium3d.vn`.
**Đổi mật khẩu hoặc xóa các tài khoản này trước khi triển khai thật.**

## Mô hình bảo mật

- **Xác thực**: access token JWT (HS256, 15 phút) do identity-service phát hành; **mọi** service tự xác minh
  chữ ký, issuer, audience và loại token (không tin header do client gửi).
  Refresh token là chuỗi ngẫu nhiên 256-bit trong **cookie HttpOnly + Secure + SameSite=Strict**, DB chỉ lưu
  giá trị băm SHA-256; mỗi lần làm mới sẽ xoay vòng token, dùng lại token cũ ⇒ thu hồi toàn bộ phiên.
- **Phân quyền (deny-by-default)**: endpoint nào không khai báo trong `aquarium.security.public-endpoints`
  đều yêu cầu đăng nhập. Người dùng lấy từ JWT, không nhận `userId` từ client; kiểm tra quyền sở hữu ở mọi
  thao tác (đơn hàng, giỏ hàng, bản vẽ, kho, gian hàng) — người khác nhận 404 để không lộ dữ liệu.
  Duyệt nhà cung cấp / hoa hồng / hoàn tất đơn: chỉ `ADMIN`.
- **Toàn vẹn giá**: giá, phí ship, giảm giá luôn do server tính theo SKU; tồn kho được giữ bằng câu lệnh
  UPDATE có điều kiện / khóa dòng nên không bán vượt tồn khi đặt hàng đồng thời.
- **Chống lạm dụng**: gateway giới hạn 300 request/phút/IP (20/phút cho đăng nhập/đăng ký/làm mới token),
  identity khóa email 15 phút sau 5 lần sai mật khẩu; giới hạn kích thước body ở gateway và từng service.
- **Mật khẩu**: BCrypt cost 12, tối thiểu 8 ký tự gồm chữ và số; so khớp với hash giả khi email không tồn tại
  để không lộ email nào đã đăng ký.
- **Cấu hình**: không còn secret mặc định trong mã nguồn — thiếu `POSTGRES_PASSWORD` / `JWT_SECRET` thì
  service không khởi động. Các service chỉ lắng nghe trên `127.0.0.1` (đi qua gateway); Swagger tắt mặc định.
- **Lỗi**: không trả thông điệp nội bộ (SQL, stack trace) về client; JSON sai định dạng ⇒ 400, không phải 500.
- **Header**: `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy`, CSP cho `/api/**`.

### Kiểm thử hồi quy bảo mật
`tests/security_smoke_test.py` (Python 3, không cần thư viện ngoài) kiểm tra ~95 điểm: CORS, cookie refresh,
xoay vòng/thu hồi token, IDOR đơn hàng, giá tính phía server, chống bán vượt tồn, phân quyền nhà cung cấp/kho,
khóa đăng nhập, rate-limit... Chạy sau mỗi lần sửa backend — **chỉ trên database thử nghiệm** vì script tạo dữ liệu thật:

```bash
python tests/security_smoke_test.py
```

### Việc cần làm khi triển khai thật
- Đặt gateway sau reverse proxy HTTPS (nginx/Caddy), bật `RATE_LIMIT_TRUST_XFF=true` nếu proxy thêm `X-Forwarded-For`.
- Đổi mật khẩu PostgreSQL (mật khẩu mặc định cũ từng nằm trong lịch sử git) và dùng `JWT_SECRET` riêng cho từng môi trường.
- Chạy nhiều instance: chuyển bộ đếm rate-limit / khóa đăng nhập sang Redis.
