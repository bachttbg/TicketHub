package com.tickethub.controller;

import com.tickethub.dao.OrderDAO;
import com.tickethub.model.Order;
import com.tickethub.util.VnPay;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * VNPay gọi trình duyệt của khách về đây sau khi thanh toán (Return URL).
 * Kiểm tra chữ ký + số tiền rồi mới cập nhật đơn hàng, cuối cùng hiển thị trang kết quả.
 */
@WebServlet(name = "VnPayReturnServlet", urlPatterns = {"/payment/vnpay-return"})
public class VnPayReturnServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        boolean success = false;
        String message;
        Order order = null;

        if (!VnPay.verify(req)) {
            message = "Chữ ký không hợp lệ! Dữ liệu trả về từ VNPay có thể đã bị thay đổi.";
        } else {
            String code = req.getParameter("vnp_ResponseCode");
            String status = req.getParameter("vnp_TransactionStatus");
            boolean paidOk = "00".equals(code) && (status == null || "00".equals(status));
            try {
                int orderId = Integer.parseInt(req.getParameter("vnp_TxnRef"));
                order = orderDAO.getOrderById(orderId);
                if (order == null) {
                    message = "Không tìm thấy đơn hàng tương ứng.";
                } else {
                    // Số tiền VNPay trả về (đã nhân 100) phải khớp với đơn hàng
                    BigDecimal paid = new BigDecimal(req.getParameter("vnp_Amount")).movePointLeft(2);
                    if (paid.compareTo(order.getTotalAmount()) != 0) {
                        message = "Số tiền thanh toán không khớp với đơn hàng.";
                    } else {
                        orderDAO.completeGatewayPayment(orderId, paidOk, req.getParameter("vnp_TransactionNo"));
                        order = orderDAO.getOrderById(orderId); // đọc lại trạng thái mới nhất
                        success = paidOk;
                        message = VnPay.describe(code);
                    }
                }
            } catch (Exception e) {
                message = "Không xử lý được kết quả thanh toán: " + e.getMessage();
            }
        }

        req.setAttribute("success", success);
        req.setAttribute("message", message);
        req.setAttribute("order", order);
        req.setAttribute("transactionNo", req.getParameter("vnp_TransactionNo"));
        req.setAttribute("bankCode", req.getParameter("vnp_BankCode"));
        req.getRequestDispatcher("/WEB-INF/views/payment-result.jsp").forward(req, resp);
    }
}
