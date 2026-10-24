package com.tickethub.controller;

import com.tickethub.dao.SettingsDAO;
import com.tickethub.dao.UserDAO;
import com.tickethub.model.User;
import com.tickethub.util.Admin;
import com.tickethub.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Cài đặt: hồ sơ quản trị, đổi mật khẩu, thông số bán vé và thông tin liên hệ của website. */
@WebServlet(name = "AdminSettingsServlet", urlPatterns = {"/admin/settings"})
public class AdminSettingsServlet extends HttpServlet {

    private final SettingsDAO settings = new SettingsDAO();
    private final UserDAO users = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Admin.prepare(req, "settings", "Cài đặt", "Hồ sơ, bảo mật và thông số hệ thống");
        User me = (User) req.getSession().getAttribute("currentUser");
        try {
            User fresh = users.getById(me.getUserId());
            req.setAttribute("me", fresh != null ? fresh : me);
        } catch (Exception e) {
            req.setAttribute("me", me);
        }
        req.setAttribute("cfg", settings.getAll());
        String tab = Web.str(req, "tab");
        req.setAttribute("tab", tab.equals("security") || tab.equals("system") ? tab : "profile");
        req.getRequestDispatcher("/WEB-INF/views/admin/settings.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User me = (User) req.getSession().getAttribute("currentUser");
        try {
            switch (Web.str(req, "action")) {
                case "profile": {
                    String name = Web.str(req, "fullName"), phone = Web.str(req, "phone");
                    if (name.length() < 2 || name.length() > 100) { Web.fail(resp, 400, "Họ tên phải từ 2 đến 100 ký tự."); return; }
                    if (!phone.matches("[0-9+ ]{9,15}")) { Web.fail(resp, 400, "Số điện thoại không hợp lệ."); return; }
                    users.updateProfile(me.getUserId(), name, phone);
                    me.setFullName(name);
                    me.setPhone(phone);
                    req.getSession().setAttribute("currentUser", me);
                    Web.ok(resp, "Đã cập nhật hồ sơ.");
                    break;
                }
                case "password": {
                    String cur = req.getParameter("current") == null ? "" : req.getParameter("current");
                    String nw = req.getParameter("newPassword") == null ? "" : req.getParameter("newPassword");
                    String cf = req.getParameter("confirm") == null ? "" : req.getParameter("confirm");
                    if (!users.checkPassword(me.getUserId(), cur)) { Web.fail(resp, 400, "Mật khẩu hiện tại không đúng."); return; }
                    if (nw.length() < 6) { Web.fail(resp, 400, "Mật khẩu mới phải có ít nhất 6 ký tự."); return; }
                    if (!nw.equals(cf)) { Web.fail(resp, 400, "Xác nhận mật khẩu không khớp."); return; }
                    if (nw.equals(cur)) { Web.fail(resp, 400, "Mật khẩu mới phải khác mật khẩu hiện tại."); return; }
                    users.changePassword(me.getUserId(), nw);
                    Web.ok(resp, "Đã đổi mật khẩu.");
                    break;
                }
                case "system": {
                    Map<String, String> v = new LinkedHashMap<>();
                    String fee = Web.str(req, "service_fee").replace(".", "");
                    String code = Web.str(req, "coupon_code").toUpperCase();
                    String pct = Web.str(req, "coupon_percent");
                    String email = Web.str(req, "support_email");
                    if (!fee.matches("\\d{1,7}")) { Web.fail(resp, 400, "Phí dịch vụ phải là số từ 0 đến 9.999.999."); return; }
                    if (!code.matches("[A-Z0-9_-]{0,20}")) { Web.fail(resp, 400, "Mã giảm giá chỉ gồm chữ, số, - hoặc _ (tối đa 20 ký tự)."); return; }
                    if (!pct.matches("\\d{1,3}") || Integer.parseInt(pct) > 100) { Web.fail(resp, 400, "Phần trăm giảm phải từ 0 đến 100."); return; }
                    if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) { Web.fail(resp, 400, "Email hỗ trợ không hợp lệ."); return; }
                    String hotline = Web.str(req, "hotline"), address = Web.str(req, "address"), hours = Web.str(req, "support_hours");
                    if (hotline.isEmpty() || hotline.length() > 30) { Web.fail(resp, 400, "Hotline từ 1 đến 30 ký tự."); return; }
                    if (address.isEmpty() || address.length() > 200) { Web.fail(resp, 400, "Địa chỉ từ 1 đến 200 ký tự."); return; }
                    if (hours.isEmpty() || hours.length() > 100) { Web.fail(resp, 400, "Giờ hỗ trợ từ 1 đến 100 ký tự."); return; }
                    v.put("service_fee", fee); v.put("coupon_code", code); v.put("coupon_percent", pct);
                    v.put("support_email", email); v.put("hotline", hotline); v.put("address", address); v.put("support_hours", hours);
                    settings.save(v);
                    Web.ok(resp, "Đã lưu cài đặt hệ thống. Thay đổi áp dụng ngay cho trang bán vé.");
                    break;
                }
                default: Web.fail(resp, 400, "Yêu cầu không hợp lệ.");
            }
        } catch (Exception e) {
            Admin.handleError(resp, e);
        }
    }
}
