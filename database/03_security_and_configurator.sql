-- Đảm bảo psql đọc đúng tiếng Việt dù chạy từ PowerShell/CMD với code page khác
SET client_encoding = 'UTF8';

-- ==============================================================================
-- MIGRATION 03: BẢO MẬT + DANH MỤC LINH KIỆN STUDIO 3D
-- Chạy SAU 01_schema.sql và 02_seed_data.sql. An toàn khi chạy nhiều lần (idempotent).
-- Dùng cho cả DB mới lẫn DB đã có dữ liệu từ phiên bản trước.
-- ==============================================================================

-- 1. Refresh token xoay vòng (identity-service): thời điểm thu hồi + token thay thế
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ;
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS replaced_by UUID;
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- 2. Kho đã giữ hàng cho từng dòng đơn (để nhả hàng khi hủy / trừ kho khi giao)
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS warehouse_id UUID REFERENCES warehouses(id) ON DELETE SET NULL;

-- 3. Tài khoản seed cũ dùng hash giả (không phải BCrypt) nên không đăng nhập được.
--    Thay bằng hash BCrypt hợp lệ của mật khẩu DEV: Dev@123456 — CHỈ áp dụng cho đúng các hash giả cũ.
--    ⚠ Không dùng các tài khoản này trên môi trường thật.
UPDATE users
SET password_hash = '$2a$12$W5QIDsm6g7Tz/4FH0zGJQOic5OpLoKrgOYSEmUjxiygYQqW.QMLGO'
WHERE password_hash LIKE '$2a$12$e8rG.hP9.Z99.Sample%';

-- 4. Tồn kho lọc Silent-Flow phải nằm trong kho của chính nhà cung cấp bán nó (AquaArt Hà Nội).
--    Dòng cũ ở showroom Sài Gòn (thuộc nhà cung cấp khác) không được dùng để giữ hàng.
INSERT INTO inventory_items (warehouse_id, product_variant_id, stock_quantity, reserved_quantity, low_stock_threshold)
VALUES ('c0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000007', 20, 0, 4)
ON CONFLICT (warehouse_id, product_variant_id) DO NOTHING;

-- 5. Danh mục linh kiện Studio 3D — giá ở đây là NGUỒN GIÁ DUY NHẤT khi đặt bể tự thiết kế
INSERT INTO categories (id, parent_id, name, slug, description, level) VALUES
(9, NULL, 'Linh Kiện Studio 3D', 'linh-kien-studio-3d', 'Bể, kệ, phông nền, cá và vật trang trí dùng trong Studio 3D tự thiết kế', 1)
ON CONFLICT DO NOTHING;

INSERT INTO products (id, supplier_id, category_id, name, slug, sku, short_description, base_price, is_combo, is_3d_customizable, is_livestock, is_fragile_glass, status) VALUES
('d1000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 9, 'Bể Chữ Nhật Siêu Trong 90×45×45cm (Studio 3D)', 'studio-3d-cfg-tank-rect-90', 'CFG-TANK-RECT-90', 'Bể kính siêu trong 180 lít dùng trong Studio 3D', 1500000, FALSE, TRUE, FALSE, TRUE, 'ACTIVE'),
('d1000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 9, 'Bể Lục Giác Nghệ Thuật 70×70×60cm (Studio 3D)', 'studio-3d-cfg-tank-hex-70', 'CFG-TANK-HEX-70', 'Bể lục giác 120 lít dùng trong Studio 3D', 2200000, FALSE, TRUE, FALSE, TRUE, 'ACTIVE'),
('d1000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001', 9, 'Bể Tròn Mini 40×40×35cm (Studio 3D)', 'studio-3d-cfg-tank-bowl-40', 'CFG-TANK-BOWL-40', 'Bể tròn mini 30 lít dùng trong Studio 3D', 950000, FALSE, TRUE, FALSE, TRUE, 'ACTIVE'),
('d1000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000001', 9, 'Kệ Gỗ Sồi Cổ Điển', 'studio-3d-cfg-stand-wood', 'CFG-STAND-WOOD', 'Kệ gỗ sồi đặt bể', 800000, FALSE, TRUE, FALSE, FALSE, 'ACTIVE'),
('d1000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000001', 9, 'Kệ Khung Sắt Tĩnh Điện', 'studio-3d-cfg-stand-metal', 'CFG-STAND-METAL', 'Kệ khung sắt sơn tĩnh điện', 1200000, FALSE, TRUE, FALSE, FALSE, 'ACTIVE'),
('d1000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000001', 9, 'Phông Nền Thủy Cung Huyền Bí', 'studio-3d-cfg-bg-deep-blue', 'CFG-BG-DEEP-BLUE', 'Phông nền dán sau bể', 150000, FALSE, TRUE, FALSE, FALSE, 'ACTIVE'),
('d1000000-0000-0000-0000-000000000007', 'b0000000-0000-0000-0000-000000000001', 9, 'Phông Nền Rừng Thủy Sinh Amazon', 'studio-3d-cfg-bg-amazon', 'CFG-BG-AMAZON', 'Phông nền dán sau bể', 250000, FALSE, TRUE, FALSE, FALSE, 'ACTIVE'),
('d1000000-0000-0000-0000-000000000008', 'b0000000-0000-0000-0000-000000000001', 9, 'Phông Nền Cổ Trấn Đổ Nát', 'studio-3d-cfg-bg-ruins', 'CFG-BG-RUINS', 'Phông nền dán sau bể', 350000, FALSE, TRUE, FALSE, FALSE, 'ACTIVE'),
('d1000000-0000-0000-0000-000000000009', 'b0000000-0000-0000-0000-000000000002', 9, 'Cá Đĩa Discus', 'studio-3d-cfg-fish-discus', 'CFG-FISH-DISCUS', 'Cá đĩa (giá mỗi con)', 250000, FALSE, TRUE, TRUE, FALSE, 'ACTIVE'),
('d1000000-0000-0000-0000-000000000010', 'b0000000-0000-0000-0000-000000000002', 9, 'Cá Thần Tiên', 'studio-3d-cfg-fish-angel', 'CFG-FISH-ANGEL', 'Cá thần tiên (giá mỗi con)', 90000, FALSE, TRUE, TRUE, FALSE, 'ACTIVE'),
('d1000000-0000-0000-0000-000000000011', 'b0000000-0000-0000-0000-000000000002', 9, 'Cá Neon Xanh', 'studio-3d-cfg-fish-neon', 'CFG-FISH-NEON', 'Cá neon (giá mỗi con)', 15000, FALSE, TRUE, TRUE, FALSE, 'ACTIVE'),
('d1000000-0000-0000-0000-000000000012', 'b0000000-0000-0000-0000-000000000002', 9, 'Cá Hề Nemo', 'studio-3d-cfg-fish-clown', 'CFG-FISH-CLOWN', 'Cá hề (giá mỗi con)', 80000, FALSE, TRUE, TRUE, FALSE, 'ACTIVE'),
('d1000000-0000-0000-0000-000000000013', 'b0000000-0000-0000-0000-000000000002', 9, 'Lũa Thủy Sinh Tự Nhiên', 'studio-3d-cfg-decor-driftwood', 'CFG-DECOR-DRIFTWOOD', 'Lũa đã xử lý chìm', 180000, FALSE, TRUE, FALSE, FALSE, 'ACTIVE'),
('d1000000-0000-0000-0000-000000000014', 'b0000000-0000-0000-0000-000000000002', 9, 'Đá Cảnh Rêu Phong', 'studio-3d-cfg-decor-stone', 'CFG-DECOR-STONE', 'Đá cảnh bố cục', 120000, FALSE, TRUE, FALSE, FALSE, 'ACTIVE'),
('d1000000-0000-0000-0000-000000000015', 'b0000000-0000-0000-0000-000000000002', 9, 'Khóm Cây Ráy Lùn', 'studio-3d-cfg-decor-plant', 'CFG-DECOR-PLANT', 'Cây thủy sinh ráy lùn', 45000, FALSE, TRUE, FALSE, FALSE, 'ACTIVE')
ON CONFLICT DO NOTHING;

-- Mỗi linh kiện có đúng 1 biến thể, SKU biến thể trùng SKU sản phẩm
INSERT INTO product_variants (id, product_id, sku, name, price, weight_grams, dimensions_cm, attributes)
SELECT ('e1' || substr(p.id::text, 3))::uuid, p.id, p.sku, p.name, p.base_price, 1000,
       '{"length": 30, "width": 30, "height": 30}'::jsonb, '{"source": "studio-3d"}'::jsonb
FROM products p
WHERE p.category_id = 9 AND p.sku LIKE 'CFG-%'
ON CONFLICT DO NOTHING;

-- Tồn kho: bể/kệ/phông nền ở showroom AquaArt Hà Nội; cá ở trại Củ Chi; trang trí ở showroom Sài Gòn
INSERT INTO inventory_items (warehouse_id, product_variant_id, stock_quantity, reserved_quantity, low_stock_threshold)
SELECT CASE
           WHEN pv.sku LIKE 'CFG-FISH-%' THEN 'c0000000-0000-0000-0000-000000000003'::uuid
           WHEN pv.sku LIKE 'CFG-DECOR-%' THEN 'c0000000-0000-0000-0000-000000000002'::uuid
           ELSE 'c0000000-0000-0000-0000-000000000001'::uuid
       END,
       pv.id,
       CASE WHEN pv.sku LIKE 'CFG-FISH-%' THEN 300 WHEN pv.sku LIKE 'CFG-BG-%' THEN 100 ELSE 50 END,
       0,
       5
FROM product_variants pv
WHERE pv.sku LIKE 'CFG-%'
ON CONFLICT (warehouse_id, product_variant_id) DO NOTHING;

-- 6. Dữ liệu sinh học cho các loài trong Studio 3D (phục vụ cảnh báo tương thích)
INSERT INTO biological_rules (id, species_name, min_tank_liters, ph_min, ph_max, temp_min, temp_max, bioload_factor, aggressiveness_level, school_min_quantity, swimming_layer, caution_notes) VALUES
(4, 'Cá Đĩa (Discus)', 200.0, 6.0, 7.0, 28.0, 31.0, 2.00, 2, 5, 'MID', 'Cần nước mềm, ấm và rất sạch; nuôi đàn 5-6 con, tránh cá bơi nhanh tranh ăn.'),
(5, 'Cá Hề (Clownfish)', 75.0, 7.8, 8.4, 24.0, 27.0, 1.20, 3, 1, 'MID', 'Là cá NƯỚC MẶN — không thể nuôi chung với cá nước ngọt.')
ON CONFLICT DO NOTHING;

INSERT INTO species_compatibilities (species_a_id, species_b_id, is_compatible, conflict_reason) VALUES
(3, 4, TRUE, 'Cá Thần Tiên và Cá Đĩa cùng nguồn gốc Amazon, có thể nuôi chung trong bể lớn.'),
(1, 5, FALSE, 'Cá Hề sống ở nước mặn, Cá Neon sống ở nước ngọt.'),
(3, 5, FALSE, 'Cá Hề sống ở nước mặn, Cá Thần Tiên sống ở nước ngọt.'),
(4, 5, FALSE, 'Cá Hề sống ở nước mặn, Cá Đĩa sống ở nước ngọt.')
ON CONFLICT DO NOTHING;

-- 7. Đồng bộ sequence sau các lần chèn id tường minh (tránh lỗi trùng khóa khi thêm mới sau này)
SELECT setval(pg_get_serial_sequence('categories', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM categories), 1));
SELECT setval(pg_get_serial_sequence('biological_rules', 'id'), GREATEST((SELECT COALESCE(MAX(id), 0) FROM biological_rules), 1));
