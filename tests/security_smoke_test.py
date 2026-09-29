"""Kiểm thử hồi quy bảo mật & nghiệp vụ end-to-end cho backend AquaRealm.

CHỈ CHẠY TRÊN DATABASE THỬ NGHIỆM (tạo từ database/01 -> 02 -> 03): script tạo tài khoản,
đặt/hủy đơn, duyệt nhà cung cấp, nhập kho... và cố tình khóa tài khoản / chạm rate-limit.

Cách chạy (Python 3.9+, không cần thư viện ngoài), khi 7 service + gateway đang chạy:
    python tests/security_smoke_test.py
Tùy chỉnh địa chỉ: AQ_GATEWAY_URL, AQ_IDENTITY_URL, AQ_FRONTEND_ORIGIN.
Lưu ý: bước cuối (H9) làm IP hiện tại bị giới hạn tần suất ~60 giây.
"""
import json
import os
import sys
import time
import urllib.error
import urllib.request

GW = os.environ.get("AQ_GATEWAY_URL", "http://127.0.0.1:8080/api/v1")
IDENTITY = os.environ.get("AQ_IDENTITY_URL", "http://127.0.0.1:8081/api/v1")
ORIGIN = os.environ.get("AQ_FRONTEND_ORIGIN", "http://localhost:5173")
CLIENT = {"X-Aquarium-Client": "web"}
PW = "Dev@123456"

results = []


def call(method, path, token=None, body=None, headers=None, base=GW, raw=None):
    url = path if path.startswith("http") else base + path
    data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Origin", ORIGIN)
    if data is not None:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            status, hdrs, text = resp.status, resp.headers, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        status, hdrs, text = e.code, e.headers, e.read().decode("utf-8", "replace")
    try:
        payload = json.loads(text) if text else None
    except json.JSONDecodeError:
        payload = {"_raw": text[:200]}
    return status, hdrs, payload


def check(name, cond, detail=""):
    results.append((name, bool(cond), detail))
    print(("PASS " if cond else "FAIL ") + name + ("" if cond else f"  -> {detail}"))


def cookie_from(hdrs, name="aq_refresh"):
    for c in hdrs.get_all("Set-Cookie") or []:
        if c.startswith(name + "="):
            return c.split(";")[0].split("=", 1)[1], c
    return None, None


def login(email, password=PW, base=GW):
    s, h, p = call("POST", "/auth/login", body={"email": email, "password": password}, base=base)
    token = p["data"]["accessToken"] if s == 200 else None
    refresh, _ = cookie_from(h)
    return s, h, p, token, refresh


def msg(p):
    return (p or {}).get("message")


# ---------------------------------------------------------------- A. CORS + AUTH
s, h, _ = call("OPTIONS", "/auth/login", headers={"Access-Control-Request-Method": "POST",
                                                   "Access-Control-Request-Headers": "content-type"})
check("A1 preflight allowed origin", s == 200 and h.get("Access-Control-Allow-Origin") == ORIGIN, (s, dict(h)))

s, h, p, cust_token, cust_refresh = login("customer_nam@gmail.com")
acao = h.get_all("Access-Control-Allow-Origin") or []
check("A2 login seed customer (valid bcrypt seed)", s == 200 and cust_token, (s, msg(p)))
check("A2b exactly one Access-Control-Allow-Origin header (duplicate CORS bug fixed)", len(acao) == 1 and acao[0] == ORIGIN, acao)
_, raw_cookie = cookie_from(h)
check("A2c refresh cookie HttpOnly+Secure+SameSite=Strict+Path", raw_cookie and all(x in raw_cookie for x in
      ["HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth"]), raw_cookie)
check("A2d refresh token NOT in JSON body", s == 200 and "refreshToken" not in p["data"], p and p.get("data", {}).keys())
check("A2e short-lived access token (15m)", s == 200 and p["data"]["expiresIn"] == 900, p and p["data"].get("expiresIn"))

req = urllib.request.Request(GW + "/auth/login", method="OPTIONS")
req.add_header("Origin", "http://evil.example")
req.add_header("Access-Control-Request-Method", "POST")
try:
    with urllib.request.urlopen(req, timeout=10) as r:
        evil_status, evil_acao = r.status, r.headers.get("Access-Control-Allow-Origin")
except urllib.error.HTTPError as e:
    evil_status, evil_acao = e.code, e.headers.get("Access-Control-Allow-Origin")
check("A3 preflight from foreign origin rejected", evil_status == 403 and not evil_acao, (evil_status, evil_acao))

s, _, p = call("GET", "/auth/me", token=cust_token)
check("A4 /auth/me with token", s == 200 and p["data"]["email"] == "customer_nam@gmail.com", (s, msg(p)))
s, _, p = call("GET", "/auth/me")
check("A4b /auth/me anonymous -> 401", s == 401, (s, msg(p)))
s, h, p = call("GET", "/auth/me", token="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.forged")
check("A4c forged token -> 401 INVALID_TOKEN", s == 401 and p["code"] == 1009, (s, p))

s, h, p = call("POST", "/auth/refresh", headers={"Cookie": f"aq_refresh={cust_refresh}"})
check("A5 refresh without X-Aquarium-Client header rejected (CSRF guard)", s == 403, (s, msg(p)))
s, h, p = call("POST", "/auth/refresh", headers={**CLIENT, "Cookie": f"aq_refresh={cust_refresh}"})
new_refresh, _ = cookie_from(h)
check("A5b refresh rotates token", s == 200 and p["data"]["accessToken"] and new_refresh and new_refresh != cust_refresh, (s, msg(p)))
cust_token = p["data"]["accessToken"] if s == 200 else cust_token
s, h, p = call("POST", "/auth/refresh", headers={**CLIENT, "Cookie": f"aq_refresh={cust_refresh}"})
check("A5c old refresh token rejected after rotation", s == 401, (s, msg(p)))

# ---------------------------------------------------------------- B. CATALOG
s, _, p = call("GET", "/products?size=50")
items = p["data"]["content"] if s == 200 else []
cfg = [i for i in items if (i.get("sku") or "").startswith("CFG-")]
check("B1 products list exposes defaultVariantSku", s == 200 and all(i.get("defaultVariantSku") for i in items), s)
check("B1b studio configurator products seeded", len(cfg) == 15, len(cfg))
s, _, p = call("GET", "/products?isCombo=true")
check("B2 isCombo filter applied", s == 200 and p["data"]["content"] and all(i["isCombo"] for i in p["data"]["content"]), s)
s, _, p = call("GET", "/products?sortBy=password")
check("B3 unknown sortBy -> 400 (was 500)", s == 400, (s, msg(p)))
s, _, p = call("GET", "/products/search?keyword=_")
page = (p or {}).get("data", {}).get("page") or {}
check("B4 LIKE wildcard escaped ('_' is literal, no product contains it)", s == 200 and page.get("totalElements") == 0, (s, page))
check("B4b page serialized as stable DTO {content, page}", s == 200 and "pageable" not in p["data"] and {"size", "number", "totalElements", "totalPages"} <= page.keys(), (s, p and list(p.get("data", {}).keys())))

# ---------------------------------------------------------------- C. CHECKOUT
address = {"recipientName": "Nguyen Test", "phone": "0901234567", "addressLine": "1 Test Street, Ha Noi"}
led_variant = next(i["defaultVariantId"] for i in items if i["sku"] == "LED-CHIHIROS-C2")
tank_variant = next(i["defaultVariantId"] for i in items if i["sku"] == "CFG-TANK-RECT-90")


def stock(variant_id):
    s, _, p = call("GET", f"/inventory/variants/{variant_id}")
    return p["data"] if s == 200 else None


led_before = stock(led_variant)
order_body = {
    "items": [{"sku": "LED-CHIHIROS-C2-BLK", "quantity": 1}, {"sku": "CFG-TANK-RECT-90", "quantity": 1},
              {"sku": "CFG-FISH-NEON", "quantity": 3}],
    "paymentMethod": "BANK_TRANSFER", "shippingAddress": address, "customerNotes": "test",
    # các trường giả mạo giá — server phải bỏ qua
    "shippingFee": 0, "discountAmount": 999999999, "finalAmount": 1, "price": 1,
}
s, _, p = call("POST", "/orders/checkout", body=order_body)
check("C1 checkout anonymous -> 401", s == 401, s)
s, _, p = call("POST", "/orders/checkout", token=cust_token, body=order_body)
order = p["data"] if s == 201 else None
check("C2 checkout real cart items", s == 201, (s, msg(p)))
if order:
    check("C2b server-side pricing ignores client money fields",
          float(order["totalAmount"]) == 2495000 and float(order["shippingFee"]) == 50000
          and float(order["discountAmount"]) == 0 and float(order["finalAmount"]) == 2545000,
          (order["totalAmount"], order["shippingFee"], order["discountAmount"], order["finalAmount"]))
    check("C2c split into 2 sub-orders (AquaArt + Saigon)", len(order["subOrders"]) == 2 and all(so.get("storeName") for so in order["subOrders"]),
          [(so.get("storeName"), len(so["items"])) for so in order["subOrders"]])
    check("C2d order number format", order["orderNumber"].startswith("ORD-") and len(order["orderNumber"]) == 21, order["orderNumber"])
led_after = stock(led_variant)
check("C3 stock reserved on checkout", led_after["totalReserved"] == led_before["totalReserved"] + 1,
      (led_before["totalReserved"], led_after["totalReserved"]))

s, _, p = call("POST", "/orders/checkout", token=cust_token, body={**order_body, "paymentMethod": "VIETQR"})
check("C4 invalid enum -> 400 with field name (was 500)", s == 400 and "paymentMethod" in (msg(p) or ""), (s, msg(p)))
s, _, p = call("POST", "/orders/checkout", token=cust_token, body={**order_body, "items": [{"sku": "NO-SUCH-SKU", "quantity": 1}]})
check("C5 unknown SKU -> 404", s == 404 and "NO-SUCH-SKU" in (msg(p) or ""), (s, msg(p)))
s, _, p = call("POST", "/orders/checkout", token=cust_token, body={**order_body, "items": [{"sku": "CFG-FISH-NEON", "quantity": 1000}]})
check("C6 quantity > 99 -> 400", s == 400, (s, msg(p)))
s, _, p = call("POST", "/orders/checkout", token=cust_token, body={**order_body, "items": [{"sku": "CFG-TANK-RECT-90", "quantity": 60}, {"sku": "CFG-TANK-RECT-90", "quantity": 30}]})
check("C7 oversell blocked (INSUFFICIENT_STOCK)", s == 400 and p["code"] == 3002, (s, msg(p)))
tank_after_fail = stock(tank_variant)
check("C7b failed checkout rolls back reservations", tank_after_fail["totalReserved"] == 1, tank_after_fail["totalReserved"])

s, _, p = call("GET", "/orders/me", token=cust_token)
check("C8 /orders/me lists own orders", s == 200 and any(o["id"] == order["id"] for o in p["data"]), s)

# Tạo khách hàng thứ 2 (qua identity trực tiếp để không tốn hạn mức gateway)
email2 = f"attacker{int(time.time())}@example.com"
s, _, p = call("POST", "/auth/register", body={"email": email2, "password": "Attack3r2026", "fullName": "Attacker"}, base=IDENTITY)
attacker_token = p["data"]["accessToken"] if s == 201 else None
check("C9 register second customer", s == 201, (s, msg(p)))
s, _, p = call("GET", f"/orders/{order['id']}", token=attacker_token)
check("C10 IDOR: other user cannot read order (404)", s == 404, s)
s, _, p = call("POST", f"/orders/{order['id']}/cancel", token=attacker_token)
check("C10b IDOR: other user cannot cancel order", s == 404, s)
s, _, p = call("GET", f"/orders/user/{order['userId']}", token=attacker_token)
check("C10c /orders/user/{id} admin-only", s == 403, s)

s, _, p = call("POST", f"/orders/{order['id']}/cancel", token=cust_token)
check("C11 owner cancels order", s == 200 and p["data"]["status"] == "CANCELLED", (s, msg(p)))
led_released = stock(led_variant)
check("C11b reservation released after cancel", led_released["totalReserved"] == led_before["totalReserved"], led_released["totalReserved"])
s, _, p = call("POST", f"/orders/{order['id']}/cancel", token=cust_token)
check("C12 cannot cancel twice", s == 400 and p["code"] == 4002, (s, msg(p)))

# ---------------------------------------------------------------- D. SUPPLIER FULFILMENT
s, _, p = call("POST", "/orders/checkout", token=cust_token, body={"items": [{"sku": "LED-CHIHIROS-C2-BLK", "quantity": 2}],
                                                                  "paymentMethod": "COD", "shippingAddress": address})
order2 = p["data"] if s == 201 else None
check("D1 COD order placed", s == 201, (s, msg(p)))
sub_id = order2["subOrders"][0]["id"]
_, _, _, hn_token, _ = login("supplier_hanoi@aquarium3d.vn")
_, _, _, hcm_token, _ = login("supplier_hcm@aquarium3d.vn")
_, _, _, admin_token, _ = login("admin@aquarium3d.vn")
check("D2 seed supplier/admin can log in", hn_token and hcm_token and admin_token, (bool(hn_token), bool(hcm_token), bool(admin_token)))
s, _, p = call("GET", "/sub-orders/mine", token=hn_token)
mine = [so for so in (p["data"] if s == 200 else []) if so["id"] == sub_id]
check("D3 supplier sees own sub-order with shipping address", s == 200 and mine and mine[0].get("shippingAddress"), s)
s, _, p = call("GET", "/sub-orders/mine", token=cust_token)
check("D3b customer cannot access supplier API", s == 403, s)
s, _, p = call("PATCH", f"/sub-orders/{sub_id}/status", token=hcm_token, body={"status": "CONFIRMED"})
check("D4 other supplier cannot update sub-order", s == 403, (s, msg(p)))
s, _, p = call("PATCH", f"/sub-orders/{sub_id}/status", token=hn_token, body={"status": "SHIPPING"})
check("D5 invalid transition PENDING->SHIPPING -> 409", s == 409, (s, msg(p)))
before_ship = stock(led_variant)
for st in ("CONFIRMED", "PACKING", "SHIPPING"):
    s, _, p = call("PATCH", f"/sub-orders/{sub_id}/status", token=hn_token, body={"status": st})
    check(f"D6 transition -> {st}", s == 200 and p["data"]["status"] == st, (s, msg(p)))
after_ship = stock(led_variant)
check("D7 shipping converts reservation into stock-out",
      after_ship["totalStock"] == before_ship["totalStock"] - 2 and after_ship["totalReserved"] == before_ship["totalReserved"] - 2,
      (before_ship["totalStock"], after_ship["totalStock"], before_ship["totalReserved"], after_ship["totalReserved"]))
s, _, p = call("GET", f"/orders/{order2['id']}", token=cust_token)
check("D8 master order status follows sub-orders (SHIPPED)", s == 200 and p["data"]["status"] == "SHIPPED", (s, p and p["data"].get("status")))
s, _, p = call("POST", f"/orders/{order2['id']}/cancel", token=cust_token)
check("D9 cannot cancel after shipping", s == 400, (s, msg(p)))
s, _, p = call("PATCH", f"/sub-orders/{sub_id}/status", token=hn_token, body={"status": "DELIVERED"})
check("D10 supplier marks DELIVERED", s == 200, (s, msg(p)))
s, _, p = call("PATCH", f"/sub-orders/{sub_id}/status", token=hn_token, body={"status": "COMPLETED"})
check("D11 supplier cannot mark COMPLETED (admin only)", s == 409, (s, msg(p)))
s, _, p = call("PATCH", f"/sub-orders/{sub_id}/status", token=admin_token, body={"status": "COMPLETED"})
check("D12 admin completes", s == 200, (s, msg(p)))
s, _, p = call("GET", f"/orders/{order2['id']}", token=cust_token)
check("D13 master order COMPLETED", s == 200 and p["data"]["status"] == "COMPLETED", p and p["data"].get("status"))

# ---------------------------------------------------------------- E. SUPPLIER SERVICE
s, _, p = call("GET", "/suppliers/aquaart-hanoi")
check("E1 public supplier page hides tax/commission", s == 200 and "taxCode" not in p["data"] and "commissionRate" not in p["data"], (s, p and list(p["data"].keys())))
s, _, p = call("GET", "/suppliers")
check("E2 supplier list anonymous -> 401", s == 401, s)
s, _, p = call("GET", "/suppliers/me")
check("E3 /suppliers/me anonymous -> 401 (not 500)", s == 401, (s, msg(p)))
s, _, p = call("PATCH", "/suppliers/b0000000-0000-0000-0000-000000000001/status?status=ACTIVE&commissionRate=0", token=attacker_token)
check("E4 customer cannot approve/zero commission", s == 403, (s, msg(p)))
slug = f"shop-{int(time.time())}"
s, _, p = call("POST", "/suppliers/register", token=attacker_token, body={"storeName": "Attacker Shop", "slug": slug})
new_supplier = p["data"] if s == 201 else None
check("E5 register supplier bound to caller, PENDING", s == 201 and new_supplier["status"] == "PENDING", (s, msg(p)))
s, _, p = call("POST", f"/suppliers/{new_supplier['id']}/warehouses", token=attacker_token,
               body={"name": "X", "code": "WH-X-01", "warehouseType": "SHOWROOM", "address": "a", "city": "c", "district": "d"})
check("E6 unapproved (CUSTOMER role) cannot create warehouse", s == 403, (s, msg(p)))
s, _, p = call("PATCH", f"/suppliers/{new_supplier['id']}/status?status=ACTIVE&commissionRate=80", token=admin_token)
check("E7 commission > 50% rejected", s == 400, (s, msg(p)))
s, _, p = call("PATCH", f"/suppliers/{new_supplier['id']}/status?status=ACTIVE", token=admin_token)
check("E8 admin approves supplier", s == 200 and p["data"]["status"] == "ACTIVE", (s, msg(p)))
_, _, p, attacker_supplier_token, _ = login(email2, "Attack3r2026", base=IDENTITY)
s, _, me = call("GET", "/auth/me", token=attacker_supplier_token)
check("E9 approved user now has SUPPLIER role after re-login", me and me["data"]["role"] == "SUPPLIER", me and me["data"].get("role"))

# ---------------------------------------------------------------- F. INVENTORY
stock_body = {"warehouseId": "c0000000-0000-0000-0000-000000000001", "productVariantId": led_variant, "quantity": 5}
s, _, p = call("POST", "/inventory/stock-in", body=stock_body)
check("F1 stock-in anonymous -> 401", s == 401, s)
s, _, p = call("POST", "/inventory/stock-in", token=cust_token, body=stock_body)
check("F2 stock-in by customer -> 403", s == 403, s)
s, _, p = call("POST", "/inventory/stock-in", token=hcm_token, body=stock_body)
check("F3 supplier cannot stock another supplier's warehouse", s == 404, (s, msg(p)))
s, _, p = call("POST", "/inventory/stock-in", token=hn_token, body=stock_body)
check("F4 owner stock-in", s == 200, (s, msg(p)))
s, _, p = call("POST", "/inventory/reserve", token=hn_token, body={**stock_body, "quantity": 1})
check("F5 manual reserve admin-only", s == 403, s)
s, _, p = call("POST", "/inventory/stock-out", token=hn_token, body={**stock_body, "quantity": 100000})
check("F6 free stock-out cannot exceed available", s == 400, (s, msg(p)))
fish_variant = next(i["defaultVariantId"] for i in items if i["sku"] == "CFG-FISH-NEON")
s, _, p = call("GET", f"/inventory/variants/{fish_variant}")
fish_item = next(w for w in p["data"]["warehouseBreakdown"] if w["warehouseId"] == "c0000000-0000-0000-0000-000000000003")
batch = f"LOT-{int(time.time())}"
s, _, p = call("POST", "/inventory/quarantine", token=hcm_token, body={"inventoryItemId": fish_item["id"], "batchCode": batch,
               "arrivalDate": "2026-09-01", "quarantineEndDate": "2026-09-15", "initialQuantity": 20})
qid = p["data"]["id"] if s == 201 else None
check("F7 register quarantine batch", s == 201, (s, msg(p)))
s, _, p = call("PATCH", f"/inventory/quarantine/{qid}/status", token=hcm_token, body={"status": "PASSED", "currentHealthyQuantity": 25})
check("F8 healthy > initial rejected", s == 400, (s, msg(p)))
s, _, p = call("PATCH", f"/inventory/quarantine/{qid}/status", token=hcm_token, body={"status": "PASSED", "currentHealthyQuantity": 18})
check("F9 quarantine PASSED", s == 200, (s, msg(p)))
s, _, p2 = call("GET", f"/inventory/variants/{fish_variant}")
fish_item2 = next(w for w in p2["data"]["warehouseBreakdown"] if w["warehouseId"] == fish_item["warehouseId"])
check("F10 passed fish added to stock", fish_item2["stockQuantity"] == fish_item["stockQuantity"] + 18, (fish_item["stockQuantity"], fish_item2["stockQuantity"]))
s, _, p = call("PATCH", f"/inventory/quarantine/{qid}/status", token=hcm_token, body={"status": "FAILED_INFECTED"})
check("F11 final quarantine status is immutable", s == 409, (s, msg(p)))

# ---------------------------------------------------------------- G. 3D DESIGNS + BIOLOGY
design = {"name": "Test tank", "tankDimensions": json.dumps({"shape": "rectangle"}), "sceneData": json.dumps({"items": []}),
          "components": [{"sku": "CFG-TANK-RECT-90", "quantity": 1}, {"sku": "CFG-FISH-NEON", "quantity": 6}],
          "totalPrice": 1, "userId": "a0000000-0000-0000-0000-000000000001"}
s, _, p = call("POST", "/3d/designs", body=design)
check("G1 save design anonymous -> 401", s == 401, s)
s, _, p = call("POST", "/3d/designs", token=cust_token, body=design)
d = p["data"] if s == 201 else None
check("G2 save design: owner from token, server price", s == 201 and float(d["totalPrice"]) == 1590000 and d["userId"] != "a0000000-0000-0000-0000-000000000001",
      (s, msg(p), d and d.get("totalPrice")))
check("G2b share slug has 64-bit entropy", d and len(d["shareSlug"]) == len("design-") + 16, d and d["shareSlug"])
s, _, p = call("GET", f"/3d/designs/share/{d['shareSlug']}")
check("G3 public design viewable anonymously", s == 200, s)
s, _, p = call("POST", "/3d/designs", token=cust_token, body={**design, "isPublic": False})
private_slug = p["data"]["shareSlug"]
s, _, p = call("GET", f"/3d/designs/share/{private_slug}")
check("G4 private design hidden from anonymous (404)", s == 404, s)
s, _, p = call("GET", f"/3d/designs/share/{private_slug}", token=cust_token)
check("G4b owner can view private design", s == 200, s)
s, _, p = call("POST", "/3d/designs", token=cust_token, body={**design, "sceneData": "not-json"})
check("G5 invalid JSON field -> 400", s == 400, (s, msg(p)))
s, _, p = call("POST", "/3d/biology/check-compatibility", body={"speciesIds": [1, 3], "tankVolumeLiters": 100})
check("G6 biology: neon + angelfish incompatible, JSON uses isCompatible", s == 200 and p["data"].get("isCompatible") is False and "isBioLoadSafe" in p["data"], (s, p and p.get("data")))
s, _, p = call("POST", "/3d/biology/check-compatibility", body={"speciesIds": [1]})
check("G7 biology: missing tank volume -> 400 (was NPE 500)", s == 400, (s, msg(p)))

# ---------------------------------------------------------------- H. HARDENING
s, _, p = call("POST", "/3d/designs", token=cust_token, raw=b'{"name":"x","sceneData":"' + b"a" * 200000 + b'"}')
check("H1 oversized body -> 413", s == 413, (s, msg(p)))
s, _, p = call("POST", "/orders/checkout", token=cust_token, raw=b"{not json")
check("H2 malformed JSON -> 400", s == 400, (s, msg(p)))
s, h, p = call("GET", "/products?size=1")
check("H3 security headers on API responses", h.get("X-Content-Type-Options") == "nosniff" and h.get("X-Frame-Options") == "DENY"
      and "default-src 'none'" in (h.get("Content-Security-Policy") or ""), dict(h))

# Brute-force lockout (direct to identity so gateway quota is unaffected)
bf_email = f"bf{int(time.time())}@example.com"
call("POST", "/auth/register", body={"email": bf_email, "password": "Brute4Force", "fullName": "BF"}, base=IDENTITY)
for _ in range(5):
    login(bf_email, "wrong-password", base=IDENTITY)
s, _, p, _, _ = login(bf_email, "Brute4Force", base=IDENTITY)
check("H4 account locked after 5 failed logins (even correct password)", s == 429 and p["code"] == 1011, (s, msg(p)))
s, h, p = call("POST", "/auth/login", body={"email": bf_email, "password": "Brute4Force"}, base=IDENTITY)
check("H4b lockout response carries Retry-After", s == 429 and h.get("Retry-After") == "900", (s, h.get("Retry-After")))
s, _, p = call("POST", "/auth/register", body={"email": "weak@example.com", "password": "123456", "fullName": "W"}, base=IDENTITY)
check("H5 weak password rejected", s == 400, (s, p and p.get("data")))
s, _, p = call("POST", "/auth/register", body={"email": email2.upper(), "password": "Another1pass", "fullName": "Dup"}, base=IDENTITY)
check("H6 duplicate email (case-insensitive) rejected", s == 400, (s, msg(p)))

# Logout + refresh reuse detection
s, h, p, tok, r1 = login("tech_dung@aquarium3d.vn")
s, h, p = call("POST", "/auth/refresh", headers={**CLIENT, "Cookie": f"aq_refresh={r1}"})
r2, _ = cookie_from(h)
print("   ... waiting 21s to exceed refresh-rotation grace window")
time.sleep(21)
s, h, p = call("POST", "/auth/refresh", headers={**CLIENT, "Cookie": f"aq_refresh={r1}"})
check("H7 reuse of rotated refresh token rejected", s == 401, s)
s, h, p = call("POST", "/auth/refresh", headers={**CLIENT, "Cookie": f"aq_refresh={r2}"})
check("H7b reuse detection revokes the whole session family", s == 401, (s, msg(p)))
s, h, p, tok, r3 = login("tech_dung@aquarium3d.vn")
s, h, p = call("POST", "/auth/logout", headers={**CLIENT, "Cookie": f"aq_refresh={r3}"})
_, cleared = cookie_from(h)
check("H8 logout clears cookie", s == 200 and cleared and "Max-Age=0" in cleared, cleared)
s, h, p = call("POST", "/auth/refresh", headers={**CLIENT, "Cookie": f"aq_refresh={r3}"})
check("H8b refresh after logout rejected", s == 401, s)

# ---------------------------------------------------------------- I. ACCOUNT, QUOTE & FILTERS
new_phone = "09" + str(int(time.time()))[-8:]
s, _, p = call("PATCH", "/account/profile", token=cust_token, body={"fullName": "Nam Nguyen", "phone": new_phone})
check("I1 update own profile", s == 200 and p["data"]["fullName"] == "Nam Nguyen" and p["data"]["phone"] == new_phone, (s, msg(p)))
s, _, p = call("PATCH", "/account/profile", token=cust_token, body={"phone": "123"})
check("I2 invalid phone rejected", s == 400, (s, msg(p)))
s, _, p = call("PATCH", "/account/profile", token=attacker_token, body={"phone": new_phone})
check("I3 phone already used by another account rejected", s == 400 and p["code"] == 1005, (s, msg(p)))
s, _, p = call("PATCH", "/account/profile", body={"fullName": "x"})
check("I4 profile update anonymous -> 401", s == 401, s)

addr = {"recipientName": "Nam", "phone": "0901234567", "addressLine": "12 Hang Bac", "ward": "Hang Bac",
        "district": "Hoan Kiem", "city": "Ha Noi"}
s, _, p = call("POST", "/account/addresses", token=cust_token, body=addr)
a1 = p["data"] if s == 201 else {}
s, _, p = call("POST", "/account/addresses", token=cust_token,
               body={**addr, "addressLine": "34 Le Loi", "district": "Quan 1", "city": "Ho Chi Minh", "isDefault": True})
a2 = p["data"] if s == 201 else {}
s, _, p = call("GET", "/account/addresses", token=cust_token)
book = p["data"] if s == 200 else []
check("I5 address book: first address default, isDefault switches, default listed first",
      a1.get("isDefault") is True and a2.get("isDefault") is True and book and book[0]["id"] == a2.get("id")
      and sum(1 for a in book if a["isDefault"]) == 1, [(a["id"][:8], a["isDefault"]) for a in book])
s1, _, _ = call("PUT", f"/account/addresses/{a1.get('id')}", token=attacker_token, body=addr)
s2, _, _ = call("DELETE", f"/account/addresses/{a1.get('id')}", token=attacker_token)
s3, _, _ = call("PATCH", f"/account/addresses/{a1.get('id')}/default", token=attacker_token)
check("I6 IDOR: other user cannot edit/delete/default someone's address (404)", (s1, s2, s3) == (404, 404, 404), (s1, s2, s3))
call("DELETE", f"/account/addresses/{a2.get('id')}", token=cust_token)
s, _, p = call("GET", "/account/addresses", token=cust_token)
book = p["data"] if s == 200 else []
check("I7 deleting the default address promotes another one", len(book) == 1 and book[0]["isDefault"] is True, book)
s, _, p = call("POST", "/account/addresses", token=cust_token, body={**addr, "phone": "abc"})
check("I8 invalid address rejected with field error", s == 400 and "phone" in ((p or {}).get("data") or {}), (s, p))

# Đổi mật khẩu (gọi thẳng identity để không tốn hạn mức auth của gateway)
pw_email = f"pw{int(time.time())}@example.com"
s, h, p = call("POST", "/auth/register", body={"email": pw_email, "password": "OldPass123", "fullName": "PW"}, base=IDENTITY)
pw_token = p["data"]["accessToken"] if s == 201 else None
old_refresh, _ = cookie_from(h)
s, _, p = call("PUT", "/account/password", token=pw_token,
               body={"currentPassword": "WrongPass1", "newPassword": "NewPass456"}, base=IDENTITY)
check("I9 change password with wrong current password -> 400", s == 400 and p["code"] == 1006, (s, msg(p)))
s, _, p = call("PUT", "/account/password", token=pw_token,
               body={"currentPassword": "OldPass123", "newPassword": "OldPass123"}, base=IDENTITY)
check("I10 new password must differ from current", s == 400, (s, msg(p)))
s, h, p = call("PUT", "/account/password", token=pw_token,
               body={"currentPassword": "OldPass123", "newPassword": "NewPass456"}, base=IDENTITY)
new_cookie, raw_new = cookie_from(h)
check("I11 change password -> 200 + new HttpOnly refresh cookie", s == 200 and new_cookie and "HttpOnly" in (raw_new or ""), (s, msg(p)))
s, _, p = call("POST", "/auth/refresh", headers={**CLIENT, "Cookie": f"aq_refresh={old_refresh}"}, base=IDENTITY)
check("I12 sessions from before the password change are revoked", s == 401, s)
s, _, p, _, _ = login(pw_email, "OldPass123", base=IDENTITY)
s2, _, p2, _, _ = login(pw_email, "NewPass456", base=IDENTITY)
check("I13 old password rejected, new password works", s == 400 and s2 == 200, (s, s2))

# Báo giá giỏ hàng (công khai, cùng công thức với checkout)
q_items = [{"sku": "CFG-TANK-RECT-90", "quantity": 1}, {"sku": "CFG-FISH-NEON", "quantity": 3},
           {"sku": "LED-CHIHIROS-C2-BLK", "quantity": 1}]
s, _, p = call("POST", "/orders/quote", body={"items": q_items, "totalAmount": 1, "shippingFee": 0})
q = p["data"] if s == 200 else {}
q_lines = {l["sku"]: l for g in q.get("groups", []) for l in g["items"]}
check("I14 quote: DB prices, server shipping split per supplier, totals consistent",
      s == 200 and float(q_lines["CFG-TANK-RECT-90"]["price"]) == 1500000
      and float(q_lines["CFG-FISH-NEON"]["subtotal"]) == 45000
      and float(q["shippingFee"]) == 50000 and len(q["groups"]) == 2
      and sum(float(g["shippingFee"]) for g in q["groups"]) == 50000
      and float(q["total"]) == float(q["subtotal"]) + 50000
      and float(q["subtotal"]) == sum(float(l["subtotal"]) for l in q_lines.values())
      and q["orderable"] is True, (s, q))
s, _, p = call("POST", "/orders/quote", body={"items": [{"sku": "NO-SUCH-SKU", "quantity": 1}, {"sku": "CFG-TANK-RECT-90", "quantity": 99}]})
q = p["data"] if s == 200 else {}
check("I15 quote flags unknown SKU and insufficient stock (not orderable)",
      s == 200 and q.get("unavailableSkus") == ["NO-SUCH-SKU"] and q.get("orderable") is False
      and q["groups"][0]["items"][0]["inStock"] is False, (s, q))
s, _, p = call("POST", "/orders/quote", body={"items": []})
check("I16 empty quote rejected", s == 400, (s, msg(p)))

# Bộ lọc sản phẩm mới
s, _, p = call("GET", "/products?keyword=neon&size=50")
res = p["data"]["content"] if s == 200 else []
check("I17 keyword filter on /products", s == 200 and any("NEON" in i["sku"] for i in res) and len(res) < len(items), [i["sku"] for i in res])
s, _, p = call("GET", "/products?isLivestock=true&size=50")
res = p["data"]["content"] if s == 200 else []
check("I18 isLivestock filter", s == 200 and res and all(i["isLivestock"] for i in res), [i["sku"] for i in res])
hn_supplier = "b0000000-0000-0000-0000-000000000001"
s, _, p = call("GET", f"/products?supplierId={hn_supplier}&size=50")
res = p["data"]["content"] if s == 200 else []
check("I19 supplierId filter", s == 200 and res and all(i["supplierId"] == hn_supplier for i in res), len(res))
s, _, p = call("GET", "/products?keyword=_&size=5")
check("I20 keyword wildcard escaped in /products", s == 200 and p["data"]["page"]["totalElements"] == 0, (s, p and p.get("data", {}).get("page")))
s, _, p = call("GET", "/products?excludeCategoryId=9&size=100")
res = p["data"]["content"] if s == 200 else []
check("I20b excludeCategoryId hides Studio components", s == 200 and res and not any(i["categoryId"] == 9 for i in res), len(res))
s, _, p = call("GET", "/products?supplierId=not-a-uuid")
check("I21 malformed supplierId -> 400", s == 400, s)

# Nhãn sản phẩm trong màn hình kho & link chia sẻ mới
s, _, p = call("GET", "/inventory/warehouses/c0000000-0000-0000-0000-000000000001", token=hn_token)
inv = p["data"] if s == 200 else []
check("I22 warehouse inventory includes product name + SKU", s == 200 and inv and all(i.get("productName") and i.get("sku") for i in inv), inv[:1])
check("I23 design share URL points to the Studio route", d and (d.get("shareUrl") or "").startswith("/studio?design="), d and d.get("shareUrl"))

# Gateway rate limit on auth endpoints (last: it throttles this IP for up to 60s)
codes, retry, body_code = [], None, None
for _ in range(30):
    s, h, p = call("POST", "/auth/refresh")   # không cookie => identity trả 403 nhanh, nhưng vẫn tính vào hạn mức gateway
    codes.append(s)
    if s == 429:
        retry, body_code = h.get("Retry-After"), (p or {}).get("code")
        break
check("H9 gateway rate-limits auth endpoint (429 + Retry-After)", s == 429 and retry and body_code == 1010, (codes, retry, body_code))

passed = sum(1 for _, ok, _ in results if ok)
print(f"\n==== {passed}/{len(results)} checks passed ====")
sys.exit(0 if passed == len(results) else 1)
