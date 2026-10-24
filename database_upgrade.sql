-- =====================================================================
-- NÂNG CẤP DATABASE CŨ LÊN BẢN QUẢN TRỊ MỚI (không mất dữ liệu)
-- Ứng dụng cũng tự chạy các bước 1-4 khi khởi động (SchemaUpdater),
-- file này dùng khi bạn muốn chạy thủ công hoặc cần bước 5 (tùy chọn).
-- Có thể chạy lại nhiều lần.
-- =====================================================================
USE TicketHubDB;
GO

-- 1) Ảnh sự kiện
IF COL_LENGTH('Events', 'image_url') IS NULL
    ALTER TABLE Events ADD image_url VARCHAR(255) NULL;
GO

-- 2) Soát vé tại cổng
IF COL_LENGTH('Orders', 'checked_in_qty') IS NULL
    ALTER TABLE Orders ADD checked_in_qty INT NOT NULL DEFAULT 0;
IF COL_LENGTH('Orders', 'last_checkin_at') IS NULL
    ALTER TABLE Orders ADD last_checkin_at DATETIME NULL;
GO

-- 2b) Mã giao dịch cổng thanh toán VNPay
IF COL_LENGTH('Orders', 'payment_ref') IS NULL
    ALTER TABLE Orders ADD payment_ref VARCHAR(100) NULL;
GO

-- 3) Cài đặt hệ thống (phí dịch vụ, mã giảm giá, thông tin liên hệ)
IF OBJECT_ID('Settings', 'U') IS NULL
    CREATE TABLE Settings (setting_key VARCHAR(60) PRIMARY KEY, setting_value NVARCHAR(500) NOT NULL);
GO

-- 4) Giá trị mặc định
IF NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = 'service_fee')    INSERT INTO Settings VALUES ('service_fee', '5000');
IF NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = 'coupon_code')    INSERT INTO Settings VALUES ('coupon_code', 'TICKIE10');
IF NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = 'coupon_percent') INSERT INTO Settings VALUES ('coupon_percent', '10');
IF NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = 'hotline')        INSERT INTO Settings VALUES ('hotline', '1900 6868');
IF NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = 'support_email')  INSERT INTO Settings VALUES ('support_email', 'support@tickethub.vn');
IF NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = 'address')        INSERT INTO Settings VALUES ('address', N'Hà Nội, Việt Nam');
IF NOT EXISTS (SELECT 1 FROM Settings WHERE setting_key = 'support_hours')  INSERT INTO Settings VALUES ('support_hours', N'08:00 – 22:00 mỗi ngày');
GO

-- 5) (TÙY CHỌN) Đồng bộ lại "số vé đã bán" với đơn hàng thật.
--    Dữ liệu mẫu cũ có số vé đã bán (VD 180/200) không khớp với các đơn hàng,
--    nên Dashboard/Loại vé sẽ hiện sai. Chạy câu lệnh dưới để tính lại theo đơn hàng.
UPDATE TicketTypes
SET sold_quantity = COALESCE((
    SELECT SUM(od.quantity)
    FROM OrderDetails od JOIN Orders o ON o.order_id = od.order_id
    WHERE od.ticket_type_id = TicketTypes.ticket_type_id AND o.payment_status <> N'Đã hủy'), 0);
GO
