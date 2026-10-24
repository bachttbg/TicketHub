package com.tickethub.controller;

import com.tickethub.dao.OrderDAO;
import com.tickethub.model.Order;
import com.tickethub.util.VnPay;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * IPN: VNPay gọi thẳng vào server để báo kết quả (server-to-server).
 * Chỉ hoạt động khi website có địa chỉ public (VNPay sandbox không gọi được vào localhost);
 * khi chạy localhost, kết quả vẫn được ghi nhận qua Return URL (VnPayReturnServlet).
 */
@WebServlet(name = "VnPayIpnServlet", urlPatterns = {"/payment/vnpay-ipn"})
public class VnPayIpnServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String out;
        try {
            if (!VnPay.verify(req)) {
                out = rsp("97", "Invalid Checksum");
            } else {
                int orderId = Integer.parseInt(req.getParameter("vnp_TxnRef"));
                Order o = orderDAO.getOrderById(orderId);
                if (o == null) {
                    out = rsp("01", "Order not Found");
                } else if (new BigDecimal(req.getParameter("vnp_Amount")).movePointLeft(2).compareTo(o.getTotalAmount()) != 0) {
                    out = rsp("04", "Invalid Amount");
                } else if (!"Chờ thanh toán".equals(o.getPaymentStatus())) {
                    out = rsp("02", "Order already confirmed");
                } else {
                    boolean ok = "00".equals(req.getParameter("vnp_ResponseCode"))
                              && "00".equals(req.getParameter("vnp_TransactionStatus"));
                    orderDAO.completeGatewayPayment(orderId, ok, req.getParameter("vnp_TransactionNo"));
                    out = rsp("00", "Confirm Success");
                }
            }
        } catch (Exception e) {
            out = rsp("99", "Unknown error");
        }
        resp.getWriter().write(out);
    }

    private static String rsp(String code, String msg) {
        return "{\"RspCode\":\"" + code + "\",\"Message\":\"" + msg + "\"}";
    }
}
