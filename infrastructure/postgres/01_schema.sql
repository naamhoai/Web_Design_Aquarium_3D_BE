-- ==============================================================================
-- DATABASE SCHEMA: AQUARIUM 3D E-COMMERCE & MULTI-WAREHOUSE PLATFORM
-- Version: 1.0.0 (Supports 158 Use Cases)
-- Engine: PostgreSQL 14+ (Tested on PostgreSQL 18.4)
-- Designed for High Scalability, Modular 3D BOM & Living Livestock (DOA)
-- ==============================================================================

-- 0. ENABLE REQUIRED EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. DROP EXISTING TYPES & TABLES (FOR CLEAN RE-RUNNABILITY IF NEEDED)
-- DROP SCHEMA public CASCADE; CREATE SCHEMA public;

-- 2. CUSTOM ENUM TYPES
DO $$ BEGIN
    CREATE TYPE user_role_enum AS ENUM ('CUSTOMER', 'SUPPLIER', 'ADMIN', 'TECHNICIAN');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE user_status_enum AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE supplier_status_enum AS ENUM ('PENDING', 'ACTIVE', 'SUSPENDED', 'REJECTED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE warehouse_type_enum AS ENUM ('SHOWROOM', 'CENTRAL_HUB', 'LIVESTOCK_FARM');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE product_status_enum AS ENUM ('DRAFT', 'ACTIVE', 'OUT_OF_STOCK', 'ARCHIVED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE movement_type_enum AS ENUM ('IMPORT', 'EXPORT', 'BOM_DISASSEMBLY', 'BOM_ASSEMBLY', 'MORTALITY_WRITEOFF', 'ADJUSTMENT', 'TRANSFER');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE quarantine_status_enum AS ENUM ('IN_QUARANTINE', 'PASSED', 'FAILED_INFECTED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE order_status_enum AS ENUM ('PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED', 'REFUNDED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE sub_order_status_enum AS ENUM ('PENDING', 'CONFIRMED', 'PACKING', 'SHIPPING', 'DELIVERED', 'COMPLETED', 'CANCELLED', 'DISPUTED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE payment_method_enum AS ENUM ('COD', 'VNPAY', 'MOMO', 'BANK_TRANSFER');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE payment_status_enum AS ENUM ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE shipping_provider_enum AS ENUM ('GHN', 'GHTK', 'VIETTEL_POST', 'GRAB_EXPRESS_LIVE_2H', 'IN_HOUSE_TRUCK');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE warranty_type_enum AS ENUM ('DOA_LIVE_FISH', 'GLASS_LEAK_5YR', 'EQUIPMENT_DEFECT', 'PLANT_MELTED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE warranty_status_enum AS ENUM ('PENDING', 'APPROVED', 'REJECTED', 'REFUNDED', 'REPLACED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE payout_status_enum AS ENUM ('PENDING', 'PROCESSING', 'COMPLETED', 'REJECTED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE asset_type_enum AS ENUM ('TANK', 'COVER', 'LIGHT', 'FILTER', 'SOIL', 'HARDSCAPE', 'PLANT', 'FISH', 'ACCESSORY');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- 3. AUTOMATIC UPDATED_AT TRIGGER FUNCTION
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ==============================================================================
-- MODULE 1: AUTHENTICATION, USERS & ADDRESSES (U01-U06, A01-A05)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    avatar_url TEXT,
    role user_role_enum NOT NULL DEFAULT 'CUSTOMER',
    status user_status_enum NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL
);

CREATE TABLE IF NOT EXISTS user_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address_line TEXT NOT NULL,
    ward VARCHAR(100),
    district VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token TEXT UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- MODULE 2: SUPPLIERS & MULTI-WAREHOUSE INFRASTRUCTURE (S01-S06, A06-A10)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    store_name VARCHAR(200) NOT NULL,
    slug VARCHAR(220) UNIQUE NOT NULL,
    description TEXT,
    logo_url TEXT,
    banner_url TEXT,
    business_license VARCHAR(100),
    tax_code VARCHAR(50),
    rating NUMERIC(3,2) DEFAULT 5.00 CHECK (rating >= 0 AND rating <= 5.00),
    review_count INT DEFAULT 0,
    commission_rate NUMERIC(5,2) DEFAULT 8.00 CHECK (commission_rate >= 0),
    status supplier_status_enum NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL
);

CREATE TABLE IF NOT EXISTS warehouses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    warehouse_type warehouse_type_enum NOT NULL DEFAULT 'SHOWROOM',
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    latitude NUMERIC(10,8),
    longitude NUMERIC(11,8),
    contact_phone VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS supplier_staff (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    warehouse_id UUID REFERENCES warehouses(id) ON DELETE SET NULL,
    permissions JSONB DEFAULT '["INVENTORY_READ", "ORDER_READ"]'::jsonb,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (supplier_id, user_id)
);

-- ==============================================================================
-- MODULE 3: CATEGORIES, PRODUCTS & MODULAR 6-LAYER BOM (U07-U14, S07-S16)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS categories (
    id SERIAL PRIMARY KEY,
    parent_id INT REFERENCES categories(id) ON DELETE SET NULL,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) UNIQUE NOT NULL,
    description TEXT,
    icon_url TEXT,
    level INT DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    category_id INT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(280) UNIQUE NOT NULL,
    sku VARCHAR(100) UNIQUE NOT NULL,
    short_description TEXT,
    description TEXT,
    base_price NUMERIC(15,2) NOT NULL CHECK (base_price >= 0),
    is_combo BOOLEAN DEFAULT FALSE,
    is_3d_customizable BOOLEAN DEFAULT FALSE,
    is_livestock BOOLEAN DEFAULT FALSE,
    is_fragile_glass BOOLEAN DEFAULT FALSE,
    status product_status_enum NOT NULL DEFAULT 'ACTIVE',
    total_sales INT DEFAULT 0,
    rating NUMERIC(3,2) DEFAULT 5.00 CHECK (rating >= 0 AND rating <= 5.00),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL
);

CREATE TABLE IF NOT EXISTS product_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    price NUMERIC(15,2) NOT NULL CHECK (price >= 0),
    original_price NUMERIC(15,2),
    weight_grams INT DEFAULT 500,
    dimensions_cm JSONB DEFAULT '{"length": 30, "width": 30, "height": 35}'::jsonb,
    attributes JSONB DEFAULT '{}'::jsonb,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    is_thumbnail BOOLEAN DEFAULT FALSE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_boms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    component_variant_id UUID NOT NULL REFERENCES product_variants(id) ON DELETE RESTRICT,
    layer_index INT NOT NULL CHECK (layer_index BETWEEN 1 AND 6),
    layer_name VARCHAR(100) NOT NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    is_required BOOLEAN DEFAULT TRUE,
    can_swap BOOLEAN DEFAULT TRUE,
    auto_disassemble_on_stockout BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (parent_product_id, component_variant_id)
);

-- ==============================================================================
-- MODULE 4: 3D CONFIGURATOR & BIOLOGY RULES ENGINE (D01-D30)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS assets_3d (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    type asset_type_enum NOT NULL,
    product_variant_id UUID UNIQUE REFERENCES product_variants(id) ON DELETE SET NULL,
    file_url TEXT NOT NULL,
    thumbnail_url TEXT,
    format VARCHAR(20) DEFAULT 'GLB',
    poly_count INT,
    bounding_box JSONB NOT NULL DEFAULT '{"width": 0.3, "height": 0.35, "depth": 0.3}'::jsonb,
    lod_levels JSONB DEFAULT '{"high": null, "low": null}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_designs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    share_slug VARCHAR(220) UNIQUE NOT NULL,
    thumbnail_url TEXT,
    tank_dimensions JSONB NOT NULL,
    scene_data JSONB NOT NULL,
    bom_snapshot JSONB NOT NULL,
    total_price NUMERIC(15,2) NOT NULL DEFAULT 0,
    is_public BOOLEAN DEFAULT TRUE,
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS biological_rules (
    id SERIAL PRIMARY KEY,
    species_name VARCHAR(150) UNIQUE NOT NULL,
    product_id UUID REFERENCES products(id) ON DELETE CASCADE,
    min_tank_liters NUMERIC(6,1) NOT NULL CHECK (min_tank_liters > 0),
    ph_min NUMERIC(3,1) NOT NULL CHECK (ph_min >= 0 AND ph_min <= 14),
    ph_max NUMERIC(3,1) NOT NULL CHECK (ph_max >= ph_min AND ph_max <= 14),
    temp_min NUMERIC(4,1) NOT NULL,
    temp_max NUMERIC(4,1) NOT NULL CHECK (temp_max >= temp_min),
    bioload_factor NUMERIC(4,2) DEFAULT 1.00,
    aggressiveness_level INT DEFAULT 1 CHECK (aggressiveness_level BETWEEN 1 AND 5),
    school_min_quantity INT DEFAULT 1,
    swimming_layer VARCHAR(50) DEFAULT 'MID',
    caution_notes TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS species_compatibilities (
    id SERIAL PRIMARY KEY,
    species_a_id INT NOT NULL REFERENCES biological_rules(id) ON DELETE CASCADE,
    species_b_id INT NOT NULL REFERENCES biological_rules(id) ON DELETE CASCADE,
    is_compatible BOOLEAN DEFAULT TRUE,
    conflict_reason TEXT,
    UNIQUE (species_a_id, species_b_id)
);

-- ==============================================================================
-- MODULE 5: MULTI-WAREHOUSE INVENTORY & LIVESTOCK CONTROL (S17-S24, A11-A16)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS inventory_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    product_variant_id UUID NOT NULL REFERENCES product_variants(id) ON DELETE RESTRICT,
    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    low_stock_threshold INT DEFAULT 5 CHECK (low_stock_threshold >= 0),
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (warehouse_id, product_variant_id)
);

CREATE TABLE IF NOT EXISTS inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id) ON DELETE CASCADE,
    movement_type movement_type_enum NOT NULL,
    quantity INT NOT NULL,
    reference_order_id UUID,
    note TEXT,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS livestock_quarantines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id) ON DELETE CASCADE,
    batch_code VARCHAR(100) NOT NULL,
    arrival_date DATE NOT NULL,
    quarantine_end_date DATE NOT NULL,
    initial_quantity INT NOT NULL CHECK (initial_quantity > 0),
    current_healthy_quantity INT NOT NULL CHECK (current_healthy_quantity >= 0),
    status quarantine_status_enum NOT NULL DEFAULT 'IN_QUARANTINE',
    inspection_notes TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mortality_logs (
    id BIGSERIAL PRIMARY KEY,
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id) ON DELETE CASCADE,
    death_count INT NOT NULL CHECK (death_count > 0),
    cause_of_death VARCHAR(255),
    temperature_recorded NUMERIC(4,1),
    ph_recorded NUMERIC(3,1),
    logged_by UUID REFERENCES users(id) ON DELETE SET NULL,
    logged_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- MODULE 6: CART, ORDERS & MULTI-VENDOR SPLITTING (U15-U26, S25-S32, A17-A26)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_variant_id UUID NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    user_design_id UUID REFERENCES user_designs(id) ON DELETE SET NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    custom_configuration JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(50) UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    total_amount NUMERIC(15,2) NOT NULL CHECK (total_amount >= 0),
    shipping_fee NUMERIC(15,2) DEFAULT 0,
    discount_amount NUMERIC(15,2) DEFAULT 0,
    final_amount NUMERIC(15,2) NOT NULL CHECK (final_amount >= 0),
    status order_status_enum NOT NULL DEFAULT 'PENDING_PAYMENT',
    payment_method payment_method_enum NOT NULL DEFAULT 'COD',
    payment_status payment_status_enum NOT NULL DEFAULT 'PENDING',
    shipping_address JSONB NOT NULL,
    customer_notes TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sub_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    subtotal NUMERIC(15,2) NOT NULL CHECK (subtotal >= 0),
    shipping_fee NUMERIC(15,2) DEFAULT 0,
    commission_amount NUMERIC(15,2) DEFAULT 0,
    payout_amount NUMERIC(15,2) NOT NULL CHECK (payout_amount >= 0),
    status sub_order_status_enum NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sub_order_id UUID NOT NULL REFERENCES sub_orders(id) ON DELETE CASCADE,
    product_variant_id UUID NOT NULL REFERENCES product_variants(id) ON DELETE RESTRICT,
    product_name VARCHAR(255) NOT NULL,
    variant_name VARCHAR(200) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    price NUMERIC(15,2) NOT NULL CHECK (price >= 0),
    quantity INT NOT NULL CHECK (quantity > 0),
    subtotal NUMERIC(15,2) NOT NULL CHECK (subtotal >= 0),
    layer_index INT,
    bom_parent_sku VARCHAR(100),
    is_livestock BOOLEAN DEFAULT FALSE,
    is_fragile_glass BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_shipments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sub_order_id UUID NOT NULL REFERENCES sub_orders(id) ON DELETE CASCADE,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    shipping_provider shipping_provider_enum NOT NULL,
    tracking_number VARCHAR(100),
    shipping_cost NUMERIC(15,2) DEFAULT 0,
    is_express_live_2h BOOLEAN DEFAULT FALSE,
    has_oxygen_tank BOOLEAN DEFAULT FALSE,
    fragile_packing_confirmed BOOLEAN DEFAULT FALSE,
    dispatched_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_technician_services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    technician_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    service_date DATE NOT NULL,
    time_slot VARCHAR(50) NOT NULL,
    service_fee NUMERIC(15,2) NOT NULL CHECK (service_fee >= 0),
    aquarium_specifications JSONB,
    setup_checklist JSONB DEFAULT '{"tank_leveled": false, "leak_tested": false, "soil_planted": false, "filter_primed": false}'::jsonb,
    customer_signature_url TEXT,
    status VARCHAR(50) DEFAULT 'SCHEDULED',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- MODULE 7: PAYMENT, ESCROW & SUPPLIER WALLET (P01-P03, S33-S36, A27-A32)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    transaction_code VARCHAR(100) UNIQUE NOT NULL,
    provider payment_method_enum NOT NULL,
    amount NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    status payment_status_enum NOT NULL DEFAULT 'PENDING',
    payload JSONB,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS escrow_wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sub_order_id UUID UNIQUE NOT NULL REFERENCES sub_orders(id) ON DELETE CASCADE,
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    held_amount NUMERIC(15,2) NOT NULL CHECK (held_amount >= 0),
    release_due_date TIMESTAMPTZ NOT NULL,
    is_released BOOLEAN DEFAULT FALSE,
    released_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS supplier_wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id UUID UNIQUE NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    balance NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    pending_balance NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (pending_balance >= 0),
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id UUID NOT NULL REFERENCES supplier_wallets(id) ON DELETE CASCADE,
    amount NUMERIC(15,2) NOT NULL,
    type VARCHAR(50) NOT NULL,
    reference_id UUID,
    note TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payout_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    amount NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    bank_name VARCHAR(100) NOT NULL,
    bank_account_number VARCHAR(50) NOT NULL,
    account_holder_name VARCHAR(150) NOT NULL,
    status payout_status_enum NOT NULL DEFAULT 'PENDING',
    processed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- MODULE 8: REVIEWS, DOA CLAIMS & GLASS WARRANTY (U27-U34, S37-S40, A33-A38)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS product_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    order_item_id UUID UNIQUE REFERENCES order_items(id) ON DELETE SET NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    media_urls JSONB DEFAULT '[]'::jsonb,
    is_verified_purchase BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS supplier_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS warranty_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id UUID NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    claim_type warranty_type_enum NOT NULL,
    video_proof_url TEXT NOT NULL,
    photo_proof_urls JSONB DEFAULT '[]'::jsonb,
    reported_dead_quantity INT DEFAULT 1 CHECK (reported_dead_quantity > 0),
    reason TEXT NOT NULL,
    status warranty_status_enum NOT NULL DEFAULT 'PENDING',
    resolution_notes TEXT,
    resolved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sub_order_id UUID NOT NULL REFERENCES sub_orders(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    reason TEXT NOT NULL,
    evidence_urls JSONB DEFAULT '[]'::jsonb,
    admin_verdict TEXT,
    status VARCHAR(50) DEFAULT 'OPEN',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ
);

-- ==============================================================================
-- MODULE 9: SYSTEM CONFIG & AUDIT LOGS (A39-A50)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS system_settings (
    key VARCHAR(100) PRIMARY KEY,
    value JSONB NOT NULL,
    description TEXT,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_table VARCHAR(100) NOT NULL,
    entity_id UUID,
    ip_address VARCHAR(45),
    user_agent TEXT,
    old_data JSONB,
    new_data JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- PERFORMANCE INDEXES (FOR HIGH-CONCURRENCY SCALABILITY)
-- ==============================================================================

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_user_addresses_user_id ON user_addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_suppliers_slug ON suppliers(slug);
CREATE INDEX IF NOT EXISTS idx_warehouses_supplier_id ON warehouses(supplier_id);
CREATE INDEX IF NOT EXISTS idx_categories_slug ON categories(slug);
CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories(parent_id);
CREATE INDEX IF NOT EXISTS idx_products_supplier_id ON products(supplier_id);
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_slug ON products(slug);
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_product_variants_product_id ON product_variants(product_id);
CREATE INDEX IF NOT EXISTS idx_product_variants_sku ON product_variants(sku);
CREATE INDEX IF NOT EXISTS idx_product_boms_parent ON product_boms(parent_product_id);
CREATE INDEX IF NOT EXISTS idx_product_boms_layer ON product_boms(layer_index);
CREATE INDEX IF NOT EXISTS idx_inventory_lookup ON inventory_items(warehouse_id, product_variant_id);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_sub_orders_order_id ON sub_orders(order_id);
CREATE INDEX IF NOT EXISTS idx_sub_orders_supplier_id ON sub_orders(supplier_id);
CREATE INDEX IF NOT EXISTS idx_order_items_sub_order ON order_items(sub_order_id);
CREATE INDEX IF NOT EXISTS idx_warranty_claims_order_item ON warranty_claims(order_item_id);
CREATE INDEX IF NOT EXISTS idx_user_designs_user_id ON user_designs(user_id);
CREATE INDEX IF NOT EXISTS idx_user_designs_share_slug ON user_designs(share_slug);

CREATE INDEX IF NOT EXISTS idx_product_variants_attrs ON product_variants USING gin (attributes);
CREATE INDEX IF NOT EXISTS idx_user_designs_scene ON user_designs USING gin (scene_data);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);

-- ==============================================================================
-- TRIGGERS FOR AUTO-UPDATING TIMESTAMPS
-- ==============================================================================

DO $$ BEGIN
    CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TRIGGER trg_user_addresses_updated_at BEFORE UPDATE ON user_addresses FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TRIGGER trg_suppliers_updated_at BEFORE UPDATE ON suppliers FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TRIGGER trg_warehouses_updated_at BEFORE UPDATE ON warehouses FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TRIGGER trg_products_updated_at BEFORE UPDATE ON products FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TRIGGER trg_product_variants_updated_at BEFORE UPDATE ON product_variants FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TRIGGER trg_inventory_items_updated_at BEFORE UPDATE ON inventory_items FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TRIGGER trg_orders_updated_at BEFORE UPDATE ON orders FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TRIGGER trg_sub_orders_updated_at BEFORE UPDATE ON sub_orders FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TRIGGER trg_user_designs_updated_at BEFORE UPDATE ON user_designs FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
EXCEPTION WHEN duplicate_object THEN null; END $$;
