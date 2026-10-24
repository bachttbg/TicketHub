package com.tickethub.controller;

import com.google.gson.Gson;
import com.tickethub.dao.UserDAO;
import com.tickethub.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "AuthServlet", urlPatterns = {"/auth/login", "/auth/register", "/auth/logout"})
public class AuthServlet extends HttpServlet {

    private UserDAO userDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
        gson = new Gson();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        String uri = request.getRequestURI();

        if (uri.endsWith("/login")) {
            handleLogin(request, response);
        } else if (uri.endsWith("/register")) {
            handleRegister(request, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String uri = request.getRequestURI();
        if (uri.endsWith("/logout")) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/home");
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();

        String emailOrPhone = request.getParameter("email");
        String password = request.getParameter("password");

        if (emailOrPhone == null || emailOrPhone.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Vui lòng nhập đầy đủ thông tin đăng nhập!");
            response.getWriter().write(gson.toJson(result));
            return;
        }

        User user = userDAO.login(emailOrPhone, password);
        if (user != null) {
            HttpSession session = request.getSession(true);
            session.setAttribute("currentUser", user);

            result.put("success", true);
            result.put("fullName", user.getFullName());
            result.put("role", user.getRole());
            result.put("redirect", "ADMIN".equalsIgnoreCase(user.getRole()) ? request.getContextPath() + "/dashboard" : "");
        } else {
            result.put("success", false);
            result.put("message", "Email/Số điện thoại hoặc mật khẩu không chính xác!");
        }

        response.getWriter().write(gson.toJson(result));
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();

        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");

        if (fullName == null || fullName.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Vui lòng điền đầy đủ các thông tin bắt buộc!");
            response.getWriter().write(gson.toJson(result));
            return;
        }

        if (userDAO.checkEmailExists(email)) {
            result.put("success", false);
            result.put("message", "Email này đã được sử dụng! Vui lòng chọn email khác.");
            response.getWriter().write(gson.toJson(result));
            return;
        }

        User newUser = new User();
        newUser.setFullName(fullName.trim());
        newUser.setEmail(email.trim());
        newUser.setPhone(phone != null ? phone.trim() : "");
        newUser.setPassword(password);

        boolean ok = userDAO.register(newUser);
        if (ok) {
            User registeredUser = userDAO.login(email, password);
            HttpSession session = request.getSession(true);
            session.setAttribute("currentUser", registeredUser);

            result.put("success", true);
            result.put("fullName", fullName);
        } else {
            result.put("success", false);
            result.put("message", "Đăng ký không thành công, vui lòng thử lại!");
        }

        response.getWriter().write(gson.toJson(result));
    }
}
