# TicketHub – Hệ Thống Bán Vé Sự Kiện Trực Tuyến (NetBeans Ant Web Application)

Dự án Java Web hoàn chỉnh theo mô hình **MVC (Model - View - Controller)**, cấu trúc **NetBeans Ant thuần túy (KHÔNG DÙNG MAVEN)**, chạy trên **NetBeans IDE**, **JDK 17**, **Apache Tomcat 10.1.x** (Jakarta EE 10), và cơ sở dữ liệu **Microsoft SQL Server**.

---

## 1. Cấu Trúc Thư Mục Chuẩn NetBeans Ant (Không Dùng Maven)

```
TicketHub/
├── build.xml                                    # Tập tin build Ant của NetBeans
├── database.sql                                 # Script tạo Database SQL Server và nạp dữ liệu thực tế
├── README.md                                    # Hướng dẫn chi tiết
├── nbproject/                                   # Cấu hình dự án NetBeans
│   ├── project.xml
│   ├── project.properties
│   └── build-impl.xml
├── src/
│   └── java/                                    # [SOURCE PACKAGES] Mã nguồn Java
│       └── com/tickethub/
│           ├── context/
│           │   └── DBContext.java               # Kết nối JDBC SQL Server
│           ├── model/                           # [MODEL] Thực thể dữ liệu
│           │   ├── User.java, Category.java, Event.java, TicketType.java
│           │   ├── Order.java, OrderDetail.java
│           │   └── DashboardStats.java, TicketTypeStat.java, DailySaleStat.java
│           ├── dao/                             # [DAO] Truy vấn SQL Server
│           │   ├── EventDAO.java                # Lấy sự kiện, danh mục, tìm kiếm
│           │   ├── TicketTypeDAO.java           # Lấy loại vé & cập nhật vé đã bán
│           │   ├── OrderDAO.java                # Phân trang, lọc đơn hàng, tạo đơn
│           │   ├── DashboardDAO.java            # Tính toán thống kê KPI & biểu đồ
│           │   └── UserDAO.java                 # Đăng nhập, đăng ký tài khoản
│           └── controller/                      # [CONTROLLER] Xử lý nghiệp vụ (Servlets)
│               ├── AuthFilter.java              # Bắt đăng nhập trước khi vào /dashboard, /orders, /admin/*
│               ├── HomeController.java          # Trang chủ (/home)
│               ├── EventDetailServlet.java      # Lấy chi tiết vé (/api/event-tickets)
│               ├── CheckoutServlet.java         # Lưu đơn vào DB (/checkout)
│               ├── DashboardServlet.java        # Tổng quan bán vé (/dashboard)
│               ├── OrdersServlet.java           # Quản lý đơn hàng (/orders)
│               ├── AuthServlet.java             # Đăng nhập / Đăng ký (/auth/*)
│               └── CreateEventServlet.java      # Tạo sự kiện mới (/admin/create-event)
└── web/                                         # [WEB PAGES] Giao diện web & Thư viện
    ├── WEB-INF/
    │   ├── web.xml                              # Deployment Descriptor (Jakarta EE 10 / Tomcat 10.1)
    │   ├── lib/                                 # [THƯ VIỆN JAR ĐÃ TẢI SẴN]
    │   │   ├── mssql-jdbc-12.4.2.jre11.jar      # Driver kết nối SQL Server
    │   │   ├── jakarta.servlet.jsp.jstl-3.0.1.jar # Thư viện thẻ JSTL 3.0
    │   │   ├── jakarta.servlet.jsp.jstl-api-3.0.0.jar
    │   │   ├── gson-2.10.1.jar                  # Thư viện JSON cho AJAX
    │   │   ├── jakarta.servlet-api-6.0.0.jar
    │   │   └── jakarta.servlet.jsp-api-3.1.1.jar
    │   └── views/                               # [VIEW] Các trang JSP (No HTML)
    │       ├── common/sidebar.jsp               # Sidebar quản trị
    │       ├── home.jsp                         # Trang chủ (Có Logo góc trên bên trái)
    │       ├── dashboard.jsp                    # Trang Tổng quan bán vé (Khớp hình 2)
    │       └── orders.jsp                       # Trang Quản lý đơn hàng (Khớp hình 3)
    ├── assets/
    │   ├── css/style.css, dashboard.css
    │   ├── js/app.js, page.js                   # page.js: hiệu ứng hiện dần + chuyển trang mượt
    │   └── images/logo.svg                      # Logo TicketHub chuẩn nhận diện
    │       └── events/event-1..6.svg            # Ảnh minh họa từng sự kiện (có chuyển động)
    └── index.jsp                                # Tự động điều hướng về Controller /home
```

---

## 2. Hướng Dẫn Cài Đặt & Chạy Trong NetBeans

### Bước 1: Khởi Tạo Cơ Sở Dữ Liệu SQL Server
1. Mở **SQL Server Management Studio (SSMS)**.
2. Mở file `database.sql` tại thư mục dự án và nhấn **Execute** (`F5`) để tạo database `TicketHubDB` cùng toàn bộ dữ liệu mẫu thực tế.
3. Mở file `src/java/com/tickethub/context/DBContext.java`, kiểm tra tài khoản `USER_ID` và `PASSWORD` cho phù hợp với máy của bạn:
   ```java
   private static final String USER_ID = "sa";
   private static final String PASSWORD = "123"; // Đổi thành mật khẩu sa máy bạn nếu khác
   ```

### Bước 2: Mở Dự Án Trong NetBeans IDE
1. Mở **NetBeans IDE** (phiên bản bất kỳ).
2. Chọn menu **File** -> **Open Project...**.
3. Chọn thư mục dự án:
   `C:\Users\Admin\.gemini\antigravity\scratch\TicketHub`
   *(NetBeans sẽ hiển thị biểu tượng quả địa cầu Web Application của dự án Ant).*

### Bước 3: Chạy Dự Án Với Tomcat 10.1.x
1. Nhấp chuột phải vào tên dự án **TicketHub** -> chọn **Properties**:
   - Mục **Sources**: Chọn **Source/Binary Format** là **17** (JDK 17).
   - Mục **Run**: Chọn Server là **Apache Tomcat 10.1.x**.
2. Nhấn **Run** (hoặc phím tắt `F6`).
3. Truy cập trên trình duyệt:
   - **Trang chủ**: `http://localhost:8080/TicketHub/home`
   - **Dashboard Tổng quan bán vé**: `http://localhost:8080/TicketHub/dashboard`
   - **Quản lý Đơn hàng**: `http://localhost:8080/TicketHub/orders`

---

## 3. Cập Nhật Mới

- **Phân quyền riêng cho từng tài khoản** (`AuthFilter`):

  | Vai trò | Được làm | Không được |
  |---|---|---|
  | Khách (chưa đăng nhập) | Xem sự kiện | Mua vé, xem vé, vào quản trị (bị chuyển về form đăng nhập) |
  | **CUSTOMER** | Mua vé, xem **chỉ vé của chính mình** tại `/orders` | Vào `/dashboard`, `/admin/*` (bị chuyển về trang chủ kèm thông báo) |
  | **ADMIN** | `/dashboard`, `/admin/orders` (tất cả đơn), tạo sự kiện | Mua vé (dùng tài khoản khách hàng) |

  Đơn hàng luôn gắn với `user_id` của người mua; `/orders` (MyTicketsServlet) chỉ truy vấn đơn của tài khoản đang đăng nhập, `/admin/orders` (OrdersServlet) dành riêng cho ADMIN. Menu, footer và các nút cũng hiển thị theo vai trò.
- **Ảnh sự kiện**: cột mới `image_url` trong bảng `Events` (xem `database.sql`). Nếu để trống, hệ thống tự dùng ảnh mặc định theo mã sự kiện / danh mục (`Event.getImagePath()`), nên sự kiện tạo mới từ Dashboard vẫn có ảnh.
- **Trang chủ**: hero có slider ảnh sự kiện tự chạy (đổi cả tiêu đề, giá, nút MUA VÉ), lưới sự kiện dùng ảnh thay cho ô ngày, nền sáng nhiều màu, footer thông tin chi tiết.
- **Animation**: modal mở/đóng mượt, mascot không còn "nhảy cóc", chuyển trang có hiệu ứng, thanh tiến trình khi tải vé, tôn trọng `prefers-reduced-motion`.

> Nếu đã tạo database từ trước, hãy chạy lại `database.sql` (hoặc `ALTER TABLE Events ADD image_url VARCHAR(255) NULL;`) rồi **Clean and Build** dự án.

---

## 4. Trang Quản Trị (ADMIN) – làm lại hoàn chỉnh

Đăng nhập bằng tài khoản ADMIN (mặc định `admin@tickethub.vn` / `admin123`). Mọi con số đều tính từ dữ liệu thật trong SQL Server (không còn số liệu mẫu cố định).

| Trang | Đường dẫn | Chức năng |
|---|---|---|
| Tổng quan | `/dashboard` | 6 chỉ số có so sánh kỳ trước, biểu đồ xu hướng (doanh thu/vé/đơn), cơ cấu thanh toán, top sự kiện, đơn gần đây, cảnh báo (đơn treo >24h, vé sắp hết, sự kiện sắp diễn ra); lọc theo sự kiện và 7/30/90 ngày |
| Sự kiện | `/admin/events` | Thêm / sửa / xóa, tải ảnh lên (hoặc chọn ảnh mẫu), ghim nổi bật, ẩn/hiện, đổi trạng thái |
| Loại vé | `/admin/ticket-types` | Quản lý hạng vé, giá, số lượng phát hành; kiểm tra vượt sức chứa; tự cập nhật giá "Từ ..đ" |
| Đơn hàng | `/admin/orders` | Lọc (từ khóa, sự kiện, thanh toán, ngày, trạng thái), chi tiết đơn, xác nhận thanh toán, hủy / khôi phục đơn (tự hoàn / trừ tồn kho), xuất CSV |
| Soát vé QR | `/admin/checkin` | Nhập hoặc quét QR bằng camera, kiểm tra hợp lệ (chưa thanh toán / đã hủy / sai sự kiện), cho vào từng phần, hoàn tác, thống kê trực tiếp |
| Khách hàng | `/admin/customers` | Danh sách, chi tiêu, số đơn, lịch sử đơn, khách quay lại, xuất CSV |
| Báo cáo | `/admin/reports` | Theo ngày/tuần/tháng, theo sự kiện, loại vé, thanh toán, top khách; xuất CSV, in |
| Cài đặt | `/admin/settings` | Hồ sơ, đổi mật khẩu, phí dịch vụ, mã giảm giá, thông tin liên hệ chân trang |

Khách hàng có **mã QR** cho từng đơn ở trang *Vé của tôi* – chính là mã mà trang Soát vé đọc được.

**Sửa lỗi nghiêm trọng của luồng mua vé:** mã đơn trước đây chỉ có 50 giá trị ngẫu nhiên (sẽ trùng và lỗi sau ~50 đơn), không kiểm tra tồn kho (có thể bán lố), thời gian ghi cứng "hôm nay", và loại vé không kiểm tra thuộc đúng sự kiện. Nay mã đơn luôn duy nhất, tồn kho được trừ có điều kiện trong transaction.

### Nâng cấp database cũ
Ứng dụng tự thêm cột/bảng còn thiếu khi khởi động (`SchemaUpdater`). Nếu muốn chạy thủ công hoặc đồng bộ lại số vé đã bán, dùng `database_upgrade.sql`. Database mới: chạy `database.sql` (đã gồm ~270 đơn mẫu rải 60 ngày để biểu đồ có dữ liệu).

Ảnh tải lên được lưu ngoài thư mục ứng dụng: `<CATALINA_BASE>/tickethub-uploads` (đổi bằng `-Dtickethub.uploads=<đường dẫn>`).

---

## Thanh toán VNPay Sandbox

Luồng: **Chọn vé → Thanh toán → Server tạo đơn "Chờ thanh toán" (giữ vé) → chuyển sang cổng VNPay Sandbox → VNPay gọi về `/payment/vnpay-return` → kiểm tra chữ ký HMAC-SHA512 + số tiền → đơn thành "Đã thanh toán" (hoặc "Đã hủy" và trả vé về kho).**

1. Đăng ký tài khoản test: https://sandbox.vnpayment.vn/devreg/ (VNPay gửi email *TmnCode* và *HashSecret*).
2. Điền vào `src/java/vnpay.properties` (`vnpay.tmnCode`, `vnpay.hashSecret`), rồi Clean & Build.
   Hoặc dùng biến môi trường `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`.
3. Chạy lại ứng dụng (cột `Orders.payment_ref` được tự thêm bởi `SchemaUpdater`, hoặc chạy `database_upgrade.sql`).
4. Thẻ test: ngân hàng **NCB** · số thẻ `9704198526191432198` · tên `NGUYEN VAN A` · ngày phát hành `07/15` · OTP `123456`.

Các file liên quan: `util/VnPay.java`, `controller/CheckoutServlet.java`, `controller/VnPayReturnServlet.java`, `controller/VnPayIpnServlet.java`, `WEB-INF/views/payment-result.jsp`, `dao/OrderDAO.java` (phần "THANH TOÁN QUA CỔNG VNPAY").
Đơn bỏ dở quá 15 phút (`vnpay.expireMinutes`) tự bị hủy và trả vé về kho ở lần thanh toán kế tiếp.
