-- ==============================================================================
-- SEED DATA: AQUARIUM 3D E-COMMERCE & MULTI-WAREHOUSE PLATFORM
-- Realistic production-like seed data covering 6-layer 3D BOM & DOA Livestock
-- All UUIDs strictly use valid hex characters [0-9, a-f]
-- ==============================================================================

-- 1. SEED ROLES
INSERT INTO roles (name, description) VALUES
('CUSTOMER', 'Khách hàng cá nhân mua sắm và tùy biến bể cá 3D'),
('SUPPLIER', 'Nhà cung cấp, chủ shop cá cảnh & thiết bị thủy sinh'),
('ADMIN', 'Quản trị viên toàn quyền hệ thống sàn thương mại điện tử'),
('TECHNICIAN', 'Kỹ thuật viên chuyên trách giao lắp, set layout và bảo dưỡng tại nhà')
ON CONFLICT (name) DO NOTHING;

-- 2. SEED USERS (Password hashes are bcrypt/argon placeholders for initial dev)
-- Default initial dev password for these mock users is: Dev@123456
INSERT INTO users (id, email, password_hash, full_name, phone, role, status, email_verified) VALUES
('a0000000-0000-0000-0000-000000000001', 'admin@aquarium3d.vn', '$2a$12$e8rG.hP9.Z99.SampleAdminHashPlaceholderDev123', 'Hệ Thống Quản Trị Viên Sàn', '0901234567', 'ADMIN', 'ACTIVE', TRUE),
('a0000000-0000-0000-0000-000000000002', 'supplier_hanoi@aquarium3d.vn', '$2a$12$e8rG.hP9.Z99.SampleSupplierHashHanoiDev123', 'Nguyễn Thế Hoàng (AquaArt Hà Nội)', '0912345678', 'SUPPLIER', 'ACTIVE', TRUE),
('a0000000-0000-0000-0000-000000000003', 'supplier_hcm@aquarium3d.vn', '$2a$12$e8rG.hP9.Z99.SampleSupplierHashSaigonDev123', 'Trần Minh Tuấn (Saigon Aqua Studio)', '0987654321', 'SUPPLIER', 'ACTIVE', TRUE),
('a0000000-0000-0000-0000-000000000004', 'tech_dung@aquarium3d.vn', '$2a$12$e8rG.hP9.Z99.SampleTechHashPlaceholderDev123', 'Lê Quốc Dũng (Master Aquascaper)', '0933445566', 'TECHNICIAN', 'ACTIVE', TRUE),
('a0000000-0000-0000-0000-000000000005', 'customer_nam@gmail.com', '$2a$12$e8rG.hP9.Z99.SampleCustomerHashPlaceholderDev123', 'Nguyễn Phương Nam', '0977889900', 'CUSTOMER', 'ACTIVE', TRUE)
ON CONFLICT (email) DO NOTHING;

-- 3. SEED SUPPLIERS
INSERT INTO suppliers (id, user_id, store_name, slug, description, logo_url, rating, review_count, commission_rate, status) VALUES
('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', 'AquaArt Showroom Hà Nội', 'aquaart-hanoi', 'Chuyên gia bể thủy sinh siêu trong, đèn WRGB cao cấp và cá cảnh nhiệt đới chuẩn thẩm mỹ tự nhiên.', '/images/suppliers/aquaart_logo.png', 4.95, 142, 8.00, 'ACTIVE'),
('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000003', 'Saigon Aqua Studio', 'saigon-aqua-studio', 'Phân phối độc quyền lũa đá Bonsai gia công tinh xảo, giống cá Neon Vua khỏe đẹp và hệ thống lọc thùng ngầm.', '/images/suppliers/saigon_aqua_logo.png', 4.90, 98, 8.00, 'ACTIVE')
ON CONFLICT (slug) DO NOTHING;

-- 4. SEED MULTI-WAREHOUSES (Showrooms & Farms)
INSERT INTO warehouses (id, supplier_id, name, code, warehouse_type, address, city, district, latitude, longitude, contact_phone) VALUES
('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'Showroom AquaArt Ba Đình', 'WH-HN-BD-01', 'SHOWROOM', 'Số 18 Đường Hoàng Hoa Thám, Phường Thụy Khuê', 'Hà Nội', 'Quận Ba Đình', 21.039200, 105.819800, '0243888999'),
('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', 'Showroom Saigon Aqua Quận 3', 'WH-HCM-Q3-01', 'SHOWROOM', 'Số 250 Nguyễn Đình Chiểu, Phường Võ Thị Sáu', 'TP. Hồ Chí Minh', 'Quận 3', 10.776500, 106.688100, '0283999888'),
('c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000002', 'Trại Ươm & Cách Ly Cá Củ Chi', 'WH-HCM-CC-FARM', 'LIVESTOCK_FARM', 'Tỉnh lộ 8, Xã Tân An Hội', 'TP. Hồ Chí Minh', 'Huyện Củ Chi', 10.972300, 106.495200, '0283777666')
ON CONFLICT (code) DO NOTHING;

-- 5. SEED CATEGORIES (Hierarchical Tree)
INSERT INTO categories (id, parent_id, name, slug, description, level) VALUES
(1, NULL, 'Bể Thủy Sinh Trọn Bộ (Combos)', 'be-thuy-sinh-tron-bo', 'Các mẫu bể thủy sinh hoàn thiện kèm phối cảnh 3D', 1),
(2, NULL, 'Bể Kính Siêu Trong (Tanks)', 'be-kinh-sieu-trong', 'Bể kính siêu trong Opti-white vát cạnh giấu keo thẩm mỹ', 1),
(3, NULL, 'Đèn Chiếu Sáng Thủy Sinh (Lighting)', 'den-thuy-sinh', 'Đèn LED WRGB quang phổ kích rêu màu và quang hợp', 1),
(4, NULL, 'Hệ Thống Lọc & Bơm (Filtration)', 'he-thong-loc-bom', 'Lọc thùng, lọc đáy ngầm, bơm tuần hoàn siêu êm', 1),
(5, NULL, 'Phân Nền & Cốt Nền (Substrate)', 'phan-nen-cot-nen', 'Đất nền nham thạch vi sinh, cốt nền dinh dưỡng lâu năm', 1),
(6, NULL, 'Lũa & Đá Cảnh (Hardscape)', 'lua-da-canh', 'Lũa Bonsai, đá Tiger, đá Da Voi đã xử lý vi sinh', 1),
(7, NULL, 'Cá Cảnh & Sinh Vật Sống (Livestock)', 'ca-canh-sinh-vat-song', 'Cá bầy đàn, cá dọn bể, tép cảnh có bảo hành sống khỏe DOA', 1),
(8, NULL, 'Cây Thủy Sinh (Aquatic Plants)', 'cay-thuy-sinh', 'Rêu rải nền, bucep, tiêu thảo, cây tiền cảnh cấy mô', 1)
ON CONFLICT (id) DO NOTHING;

-- 6. SEED PRODUCTS & VARIANTS (Matching the 2 Videos & 6 Layers)
-- Master Combo Product: Bể Nano Cube Rimless 30x30x35cm
INSERT INTO products (id, supplier_id, category_id, name, slug, sku, short_description, description, base_price, is_combo, is_3d_customizable, is_livestock, is_fragile_glass, status) VALUES
('d0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 1, 'Bể Thủy Sinh Nano Cube Rimless 30x30x35cm Full Setup 3D', 'be-thuy-sinh-nano-cube-rimless-30x30x35cm', 'COMBO-NANO-30', 'Combo bể Nano 30cm bóc tách 6 tầng công nghệ cao cấp, tương thích mô phỏng 3D', 'Bộ sản phẩm trọn gói đồng bộ gồm đèn LED Cantilever Slimline, nắp kính, đàn cá Neon dạ quang, lũa Bonsai rêu, bể kính siêu trong 5mm, phân nền ADA Amazonia và hệ thống lọc Silent-Flow ngầm.', 3850000, TRUE, TRUE, FALSE, TRUE, 'ACTIVE'),
-- Layer 1: Đèn LED Slimline
('d0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 3, 'Đèn LED Cantilever Slimline Chihiros C2 RGB 6500K', 'den-led-cantilever-slimline-chihiros-c2', 'LED-CHIHIROS-C2', 'Đèn LED thủy sinh siêu mỏng kẹp thành bể, tích hợp app điều khiển quang phổ Bluetooth', 'Quang phổ WRGB full dải màu, hỗ trợ cây quang hợp rực rỡ và phát quang màu cá Neon.', 950000, FALSE, TRUE, FALSE, FALSE, 'ACTIVE'),
-- Layer 2: Nắp Kính Chống Bay Hơi
('d0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001', 2, 'Nắp Kính Siêu Trong & Cụm Kẹp Mica 30x30cm', 'nap-kinh-sieu-trong-mica-30x30cm', 'COVER-GLASS-30', 'Nắp kính Opti-white chống thoát ẩm, chống cá nhảy, vát góc luồn dây thẩm mỹ', 'Giúp giữ ổn định nhiệt độ mặt nước, chống bụi và ngăn các loài cá hiếu động nhảy ra ngoài.', 220000, FALSE, TRUE, FALSE, TRUE, 'ACTIVE'),
-- Layer 3: Hệ Sinh Thái Bonsai Lũa & Đàn Cá Neon Vua
('d0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000002', 6, 'Cụm Lũa Bonsai Mini Taiwan & Đàn Cá Neon Vua (12 Con)', 'cum-lua-bonsai-mini-taiwan-ca-neon', 'HARDSCAPE-BONSAI-NEON', 'Cụm lũa phong cách Bonsai tán rêu Mini Taiwan đã tạo hình cùng đàn cá Neon Vua khỏe mạnh', 'Lũa đã xử lý chìm 100% không tiết màu, kết hợp rêu xanh mướt và đàn cá bơi bầy đàn.', 850000, FALSE, TRUE, TRUE, FALSE, 'ACTIVE'),
-- Layer 4: Bể Kính Siêu Trong Vát Cạnh
('d0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000001', 2, 'Bể Kính Siêu Trong Opti-White 30x30x35cm Kính 5mm', 'be-kinh-sieu-trong-opti-white-30x30x35cm', 'TANK-OPTI-303035', 'Bể kính siêu trong 4 mặt vát cạnh 45 độ, dán giấu chỉ silicon Wacker Đức', 'Độ trong suốt 92%, bảo hành rò rỉ keo 5 năm chính hãng.', 580000, FALSE, TRUE, FALSE, TRUE, 'ACTIVE'),
-- Layer 5: Phân Nền ADA Amazonia & Nham Thạch Đen
('d0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000002', 5, 'Phân Nền Thủy Sinh ADA Aqua Soil Amazonia & Nham Thạch Đáy', 'phan-nen-ada-amazonia-nham-thach', 'SOIL-ADA-NANO-SET', 'Bộ đất nền dinh dưỡng hữu cơ kết hợp hạt nham thạch đen thông thoáng rễ', 'Giúp ổn định pH ở mức 6.2 - 6.8, tạo môi trường vàng cho cá cảnh và cây thủy sinh.', 450000, FALSE, TRUE, FALSE, FALSE, 'ACTIVE'),
-- Layer 6: Hệ Thống Bơm Lọc Silent-Flow
('d0000000-0000-0000-0000-000000000007', 'b0000000-0000-0000-0000-000000000001', 4, 'Hệ Thống Lọc Thùng Ngầm Silent-Flow 500L/h Cực Êm', 'he-thong-loc-thung-silent-flow-500lh', 'FILTER-SILENT-500', 'Lọc ngầm giấu thẩm mỹ dưới đáy, độ ồn dưới 25dB, tích hợp matrix vi sinh cao cấp', 'Tuần hoàn nước 500 lít/giờ, giữ nước trong vắt pha lê chuẩn studio.', 800000, FALSE, TRUE, FALSE, FALSE, 'ACTIVE')
ON CONFLICT (slug) DO NOTHING;

-- 7. SEED PRODUCT VARIANTS
INSERT INTO product_variants (id, product_id, sku, name, price, original_price, weight_grams, dimensions_cm, attributes) VALUES
('e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'COMBO-NANO-30-FULL', 'Bể Nano Cube 30cm Trọn Bộ Standard', 3850000, 4200000, 18500, '{"length": 30, "width": 30, "height": 35}', '{"color": "Piano Black Studio", "light": "RGB Full", "volume_liters": 31.5}'),
('e0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', 'LED-CHIHIROS-C2-BLK', 'Đèn LED Cantilever Slimline 24W Đen Mờ', 950000, 1050000, 800, '{"length": 25, "width": 12, "height": 30}', '{"power_watts": 24, "lumens": 1580, "kelvin": 6500}'),
('e0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', 'COVER-GLASS-30-CLEAR', 'Nắp Kính Opti-White 30x30cm (Kèm 4 Kẹp)', 220000, 250000, 1200, '{"length": 29, "width": 29, "height": 0.4}', '{"material": "Opti-white glass 4mm", "clips": 4}'),
('e0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000004', 'HARDSCAPE-BONSAI-SET', 'Cụm Lũa Bonsai Rêu & 12 Cá Neon Vua', 850000, 950000, 2500, '{"length": 22, "width": 18, "height": 24}', '{"wood_type": "Lũa Linh Sam", "moss": "Mini Taiwan", "fish_quantity": 12}'),
('e0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000005', 'TANK-OPTI-303035-CLR', 'Bể Kính Rimless Siêu Trong 30x30x35cm', 580000, 650000, 7200, '{"length": 30, "width": 30, "height": 35}', '{"glass_thickness_mm": 5, "glass_grade": "Opti-white 92%"}'),
('e0000000-0000-0000-0000-000000000006', 'd0000000-0000-0000-0000-000000000006', 'SOIL-ADA-NANO-SET-01', 'Set Phân Nền ADA 3L + Nham Thạch Đen 1kg', 450000, 500000, 4000, '{"length": 30, "width": 30, "height": 6}', '{"brand": "ADA Japan", "weight_kg": 4}'),
('e0000000-0000-0000-0000-000000000007', 'd0000000-0000-0000-0000-000000000007', 'FILTER-SILENT-500-SIL', 'Hệ Thống Lọc Thùng Silent-Flow 500L/h', 800000, 900000, 2200, '{"length": 18, "width": 18, "height": 28}', '{"flow_rate_lph": 500, "noise_db": 24, "power_watts": 8}')
ON CONFLICT (sku) DO NOTHING;

-- 8. SEED MODULAR 6-LAYER BILL OF MATERIALS (BOM) FOR COMBO
-- Ánh xạ Combo cha ra đúng 6 tầng linh kiện của video Exploded View
INSERT INTO product_boms (parent_product_id, component_variant_id, layer_index, layer_name, quantity, is_required, can_swap) VALUES
('d0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', 1, 'Hệ Thống Chiếu Sáng LED Slimline Cantilever', 1, TRUE, TRUE),
('d0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', 2, 'Nắp Kính Siêu Trong & Kẹp Mica Chống Nhảy Cá', 1, FALSE, TRUE),
('d0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000004', 3, 'Hệ Sinh Thái Cá Neon Vua & Lũa Bonsai Rêu Taiwan', 1, TRUE, TRUE),
('d0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000005', 4, 'Bể Kính Rimless Siêu Trong 4 Mặt Giấu Chỉ', 1, TRUE, FALSE),
('d0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000006', 5, 'Phân Nền Thủy Sinh ADA Amazonia & Nham Thạch', 1, TRUE, TRUE),
('d0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000007', 6, 'Hệ Thống Bơm Tuần Hoàn & Lọc Thùng Silent-Flow', 1, TRUE, TRUE)
ON CONFLICT (parent_product_id, component_variant_id) DO NOTHING;

-- 9. SEED 3D ASSETS METADATA
INSERT INTO assets_3d (name, type, product_variant_id, file_url, format, poly_count, bounding_box) VALUES
('Model 3D Bể Rimless 30x30x35', 'TANK', 'e0000000-0000-0000-0000-000000000005', '/models/tank_nano_30.glb', 'GLB', 4800, '{"width": 0.30, "height": 0.35, "depth": 0.30}'),
('Model 3D Đèn Cantilever C2', 'LIGHT', 'e0000000-0000-0000-0000-000000000002', '/models/light_chihiros_c2.glb', 'GLB', 7200, '{"width": 0.25, "height": 0.30, "depth": 0.12}'),
('Model 3D Nắp Kính Chống Bốc Hơi', 'COVER', 'e0000000-0000-0000-0000-000000000003', '/models/cover_glass_30.glb', 'GLB', 1200, '{"width": 0.29, "height": 0.01, "depth": 0.29}'),
('Model 3D Layout Bonsai Rêu & Cá Neon', 'HARDSCAPE', 'e0000000-0000-0000-0000-000000000004', '/models/bonsai_driftwood_moss.glb', 'GLB', 18500, '{"width": 0.22, "height": 0.24, "depth": 0.18}'),
('Model 3D Đất Nền Phẳng Vi Sinh', 'SOIL', 'e0000000-0000-0000-0000-000000000006', '/models/soil_layer_30.glb', 'GLB', 2400, '{"width": 0.29, "height": 0.06, "depth": 0.29}'),
('Model 3D Bơm Lọc Đáy Ngầm', 'FILTER', 'e0000000-0000-0000-0000-000000000007', '/models/filter_silent_flow.glb', 'GLB', 5600, '{"width": 0.18, "height": 0.28, "depth": 0.18}')
ON CONFLICT (product_variant_id) DO NOTHING;

-- 10. SEED BIOLOGICAL RULES & COMPATIBILITY
INSERT INTO biological_rules (id, species_name, min_tank_liters, ph_min, ph_max, temp_min, temp_max, bioload_factor, aggressiveness_level, school_min_quantity, swimming_layer, caution_notes) VALUES
(1, 'Cá Neon Vua (Cardinal Tetra)', 20.0, 5.5, 7.0, 24.0, 28.0, 0.40, 1, 6, 'MID', 'Cần nuôi theo đàn từ 6 con trở lên để cá không bị stress. Không nuôi chung với cá dữ to miệng.'),
(2, 'Tép Đỏ Red Cherry (Neocaridina)', 10.0, 6.5, 7.5, 22.0, 26.0, 0.10, 1, 10, 'BOTTOM', 'Nhạy cảm với hóa chất diệt ốc và kim loại nặng (Đồng). Thích hợp bể nhiều rêu và lũa trú ẩn.'),
(3, 'Cá Thần Tiên (Angelfish)', 80.0, 6.0, 7.5, 26.0, 30.0, 2.50, 3, 2, 'MID', 'Khi trưởng thành có bản năng săn mồi, sẽ ăn các loại cá nhỏ hoặc tép kiểng con.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO species_compatibilities (species_a_id, species_b_id, is_compatible, conflict_reason) VALUES
(1, 2, TRUE, 'Rất hòa hợp, cá Neon bơi tầng giữa, tép nhặt thức ăn tầng đáy.'),
(1, 3, FALSE, 'Cá Thần Tiên trưởng thành có thể tấn công và nuốt trọn cá Neon nhỏ.'),
(2, 3, FALSE, 'Tép kiểng là thức ăn tự nhiên ưa thích của Cá Thần Tiên.')
ON CONFLICT (species_a_id, species_b_id) DO NOTHING;

-- 11. SEED INVENTORY ITEMS FOR MULTI-WAREHOUSE
-- Stock in Hanoi Showroom & Saigon Showroom
INSERT INTO inventory_items (warehouse_id, product_variant_id, stock_quantity, reserved_quantity, low_stock_threshold) VALUES
('c0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 15, 2, 3),
('c0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', 30, 0, 5),
('c0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', 50, 1, 10),
('c0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000005', 25, 3, 5),
('c0000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000004', 40, 2, 8),
('c0000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000006', 60, 0, 15),
('c0000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000007', 20, 1, 4)
ON CONFLICT (warehouse_id, product_variant_id) DO UPDATE SET stock_quantity = EXCLUDED.stock_quantity;

-- 12. SEED SYSTEM SETTINGS
INSERT INTO system_settings (key, value, description) VALUES
('platform_commission_rate', '{"default_rate_percent": 8.0, "vip_rate_percent": 5.0}'::jsonb, 'Tỷ lệ hoa hồng chiết khấu sàn cho mỗi đơn hàng thành công'),
('doa_policy_hours', '{"video_claim_window_hours": 2, "auto_refund_under_vnd": 500000}'::jsonb, 'Chính sách bảo hành cá sống DOA: Thời hạn người mua gửi video unbox sau khi shipper giao'),
('technician_service_base_fee', '{"hanoi_fee": 250000, "hcm_fee": 250000, "overtime_surcharge": 100000}'::jsonb, 'Bảng giá điều phối kỹ thuật viên đến tận nhà lắp ráp bể 3D')
ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;
