-- ==============================================================================
-- VERIFICATION QUERIES: TEST 6-LAYER BOM & PLATFORM FUNCTIONALITY
-- ==============================================================================

-- 1. KIỂM TRA BÓC TÁCH 6 TẦNG LINH KIỆN BỂ CÁ (EXPLODED VIEW BOM)
SELECT 
    b.layer_index AS "Tang",
    b.layer_name AS "Ten_Linh_Kien",
    pv.name AS "Ten_Bien_The",
    pv.sku AS "SKU",
    pv.price AS "Gia_Ban_Le",
    b.is_required AS "Bat_Buoc",
    b.can_swap AS "Duoc_Doi"
FROM product_boms b
JOIN products p ON b.parent_product_id = p.id
JOIN product_variants pv ON b.component_variant_id = pv.id
WHERE p.sku = 'COMBO-NANO-30'
ORDER BY b.layer_index ASC;

-- 2. KIỂM TRA TỒN KHO ĐA ĐIỂM (SHOWROOM HÀ NỘI & SÀI GÒN)
SELECT 
    w.name AS "Showroom_Kho",
    w.city AS "Thanh_Pho",
    pv.sku AS "SKU",
    pv.name AS "Ten_San_Pham",
    i.stock_quantity AS "Ton_Kho_Thuc",
    i.reserved_quantity AS "Dang_Dat_Giu",
    i.low_stock_threshold AS "Nguong_Bao_Dong"
FROM inventory_items i
JOIN warehouses w ON i.warehouse_id = w.id
JOIN product_variants pv ON i.product_variant_id = pv.id
ORDER BY w.city, pv.sku;

-- 3. KIỂM TRA QUY TẮC TƯƠNG THÍCH SINH HỌC & CÁNH BÁO
SELECT 
    br1.species_name AS "Loai_Ca_A",
    br2.species_name AS "Loai_Ca_B",
    sc.is_compatible AS "Nuoi_Chung_Duoc",
    sc.conflict_reason AS "Luu_Y_Canh_Bao"
FROM species_compatibilities sc
JOIN biological_rules br1 ON sc.species_a_id = br1.id
JOIN biological_rules br2 ON sc.species_b_id = br2.id;
