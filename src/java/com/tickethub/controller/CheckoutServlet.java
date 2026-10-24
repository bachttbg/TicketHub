package com.tickethub.controller;

import com.google.gson.Gson;
import com.tickethub.dao.EventDAO;
import com.tickethub.dao.OrderDAO;
import com.tickethub.dao.TicketTypeDAO;
import com.tickethub.model.Event;
import com.tickethub.model.Order;
import com.tickethub.model.OrderDetail;
import com.tickethub.model.TicketType;
import com.tickethub.model.User;
import com.tickethub.util.VnPay;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@WebServlet(name = "CheckoutServlet", urlPatterns = {"/checkout"})
public class CheckoutServlet extends HttpServlet {

    private OrderDAO orderDAO;
    private EventDAO eventDAO;
    private TicketTypeDAO ticketTypeDAO;
    private Gson gson;
    private final com.tickethub.dao.SettingsDAO settingsDAO = new com.tickethub.dao.SettingsDAO();

    @Override
    public void init() throws ServletException {
        orderDAO = new OrderDAO();
        eventDAO = new EventDAO();
        ticketTypeDAO = new TicketTypeDAO();
        gson = new Gson();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();

        // Chỉ tài khoản CUSTOMER đã đăng nhập mới được mua vé (đơn hàng gắn với đúng tài khoản)
        HttpSession authSession = request.getSession(false);
        User buyer = authSession != null ? (User) authSession.getAttribute("currentUser") : null;
        if (buyer == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            result.put("success", false);
            result.put("needLogin", true);
            result.put("message", "Vui lòng đăng nhập để mua vé!");
            response.getWriter().write(gson.toJson(result));
            return;
        }
        if ("ADMIN".equalsIgnoreCase(buyer.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            result.put("success", false);
            result.put("message", "Tài khoản quản trị không dùng để mua vé. Hãy đăng nhập tài khoản khách hàng!");
            response.getWriter().write(gson.toJson(result));
            return;
        }

        if (!VnPay.isConfigured()) {
            result.put("success", false);
            result.put("message", "Chưa cấu hình VNPay Sandbox. Hãy điền vnpay.tmnCode và vnpay.hashSecret trong file src/java/vnpay.properties!");
            response.getWriter().write(gson.toJson(result));
            return;
        }

        try {
            // Giải phóng vé của các đơn VNPay bỏ dở quá hạn
            orderDAO.expirePendingGateway(VnPay.expireMinutes());

            int eventId = Integer.parseInt(request.getParameter("eventId"));
            String customerName = request.getParameter("customerName");
            String customerEmail = request.getParameter("customerEmail");
            String customerPhone = request.getParameter("customerPhone");
            String paymentMethod = request.getParameter("paymentMethod");
            // Mọi phương thức đều đi qua cổng VNPay, chỉ khác ngân hàng/kênh được mở sẵn
            String bankCode;
            if ("qr".equals(paymentMethod)) bankCode = "VNPAYQR";
            else if ("intl".equals(paymentMethod)) bankCode = "INTCARD";
            else { paymentMethod = "bank"; bankCode = "VNBANK"; }
            String couponCode = request.getParameter("couponCode");

            if (customerName == null || customerName.trim().isEmpty() ||
                customerEmail == null || customerEmail.trim().isEmpty() ||
                customerPhone == null || customerPhone.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Vui lòng điền đầy đủ thông tin người nhận vé!");
                response.getWriter().write(gson.toJson(result));
                return;
            }

            Event event = eventDAO.getEventById(eventId);
            if (event == null || event.isHidden()) {
                result.put("success", false);
                result.put("message", "Sự kiện không tồn tại!");
                response.getWriter().write(gson.toJson(result));
                return;
            }

            if (!event.isPurchasable()) {
                result.put("success", false);
                result.put("message", "Sự kiện này hiện không mở bán vé (" + event.getStatus() + ")!");
                response.getWriter().write(gson.toJson(result));
                return;
            }

            Map<String, String> settings = settingsDAO.getAll();

            String[] ticketTypeIds = request.getParameterValues("ticketTypeId");
            String[] quantities = request.getParameterValues("quantity");

            if (ticketTypeIds == null || quantities == null || ticketTypeIds.length == 0) {
                result.put("success", false);
                result.put("message", "Vui lòng chọn ít nhất 1 vé!");
                response.getWriter().write(gson.toJson(result));
                return;
            }

            List<OrderDetail> details = new ArrayList<>();
            BigDecimal subtotal = BigDecimal.ZERO;
            int totalQty = 0;
            StringBuilder summaryBuilder = new StringBuilder();

            for (int i = 0; i < ticketTypeIds.length; i++) {
                int ttId = Integer.parseInt(ticketTypeIds[i]);
                int qty = Integer.parseInt(quantities[i]);
                if (qty <= 0) continue;
                if (qty > 10) {
                    result.put("success", false);
                    result.put("message", "Mỗi loại vé chỉ được đặt tối đa 10 vé/đơn!");
                    response.getWriter().write(gson.toJson(result));
                    return;
                }

                TicketType tt = ticketTypeDAO.getTicketTypeById(ttId);
                if (tt == null || tt.getEventId() != eventId) continue; // chỉ nhận loại vé thuộc đúng sự kiện

                totalQty += qty;
                BigDecimal lineAmount = tt.getPrice().multiply(new BigDecimal(qty));
                subtotal = subtotal.add(lineAmount);

                if (summaryBuilder.length() > 0) summaryBuilder.append(", ");
                summaryBuilder.append(tt.getTypeName());

                OrderDetail od = new OrderDetail();
                od.setTicketTypeId(ttId);
                od.setTicketTypeName(tt.getTypeName());
                od.setQuantity(qty);
                od.setUnitPrice(tt.getPrice());
                od.setAmount(lineAmount);
                details.add(od);
            }

            if (totalQty == 0) {
                result.put("success", false);
                result.put("message", "Vui lòng chọn ít nhất 1 vé!");
                response.getWriter().write(gson.toJson(result));
                return;
            }

            // Phí dịch vụ và mã giảm giá lấy từ Cài đặt của trang quản trị
            BigDecimal serviceFee = settingsDAO.serviceFee(settings).multiply(new BigDecimal(totalQty));
            BigDecimal discount = BigDecimal.ZERO;
            String activeCoupon = settingsDAO.couponCode(settings);
            if (couponCode != null && !activeCoupon.isEmpty() && activeCoupon.equalsIgnoreCase(couponCode.trim())) {
                discount = subtotal.multiply(new BigDecimal(settingsDAO.couponPercent(settings)))
                                   .divide(new BigDecimal(100), 0, java.math.RoundingMode.HALF_UP);
            }
            BigDecimal totalAmount = subtotal.subtract(discount).add(serviceFee);

            String orderCode = null; // mã đơn được cấp khi lưu (xem OrderDAO.createOrder)

            Integer userId = buyer.getUserId();

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy");
            String orderTimeDesc = timeFormat.format(new Date());

            Order order = new Order();
            order.setOrderCode(orderCode);
            order.setUserId(userId);
            order.setEventId(eventId);
            order.setCustomerName(customerName.trim());
            order.setCustomerEmail(customerEmail.trim());
            order.setCustomerPhone(customerPhone.trim());
            order.setTicketTypeSummary(summaryBuilder.toString());
            order.setTotalQuantity(totalQty);
            order.setSubtotal(subtotal);
            order.setServiceFee(serviceFee);
            order.setDiscountAmount(discount);
            order.setTotalAmount(totalAmount);
            order.setPaymentMethod(paymentMethod);
            order.setPaymentStatus(OrderDAO.PENDING); // chỉ thành "Đã thanh toán" khi VNPay báo thành công
            order.setOrderTimeDesc(orderTimeDesc);

            orderDAO.createOrder(order, details);
            orderDAO.markAwaitingGateway(order.getOrderId());
            orderCode = order.getOrderCode();

            // Tạo URL sang cổng thanh toán VNPay Sandbox (mã giao dịch = order_id)
            String paymentUrl = VnPay.buildPaymentUrl(request,
                    String.valueOf(order.getOrderId()), totalAmount,
                    "Thanh toan don hang " + order.getOrderId(), bankCode);

            result.put("success", true);
            result.put("orderCode", orderCode);
            result.put("paymentUrl", paymentUrl);

        } catch (com.tickethub.util.Jdbc.DataAccessException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Lỗi xử lý đặt vé: " + e.getMessage());
        }

        response.getWriter().write(gson.toJson(result));
    }
}
