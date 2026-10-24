package com.tickethub.controller;

import com.tickethub.dao.EventDAO;
import com.tickethub.model.Event;
import com.tickethub.util.Admin;
import com.tickethub.util.Jdbc;
import com.tickethub.util.Uploads;
import com.tickethub.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Quản lý sự kiện: danh sách, thêm, sửa (kèm tải ảnh), xóa, ghim nổi bật, đổi trạng thái. */
@WebServlet(name = "AdminEventsServlet", urlPatterns = {"/admin/events"})
@MultipartConfig(maxFileSize = Uploads.MAX_BYTES, maxRequestSize = Uploads.MAX_BYTES + 200_000)
public class AdminEventsServlet extends HttpServlet {

    private final EventDAO dao = new EventDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // JSON của 1 sự kiện (đổ vào form Sửa)
        if ("json".equals(req.getParameter("format"))) {
            try {
                Event e = dao.adminGet(Web.intp(req, "id", 0));
                if (e == null) { Web.fail(resp, 404, "Không tìm thấy sự kiện."); return; }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("success", true);
                m.put("id", e.getEventId());
                m.put("title", e.getTitle());
                m.put("categoryId", e.getCategoryId());
                m.put("description", e.getDescription());
                m.put("location", e.getLocation());
                m.put("date", e.getDateInput());
                m.put("capacity", e.getTotalCapacity());
                m.put("minPrice", e.getMinPrice());
                m.put("bannerColor", e.getBannerColor());
                m.put("tagLabel", e.getTagLabel());
                m.put("status", e.getStatus());
                m.put("featured", e.isFeatured());
                m.put("image", e.getImagePath());
                m.put("orderCount", e.getOrderCount());
                Web.json(resp, 200, m);
            } catch (Exception ex) { Admin.handleError(resp, ex); }
            return;
        }

        String keyword = Web.str(req, "q");
        int cat = Web.intp(req, "category", 0);
        String status = Web.str(req, "status");
        Admin.prepare(req, "events", "Sự kiện", "Tạo, chỉnh sửa và theo dõi từng sự kiện");
        try {
            req.setAttribute("events", dao.adminList(keyword, cat, status));
            req.setAttribute("categories", dao.getAllCategories());
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("loadError", e.getMessage());
        }
        req.setAttribute("q", keyword);
        req.setAttribute("categoryId", cat);
        req.setAttribute("statusFilter", status);
        req.setAttribute("statuses", Event.STATUSES);
        req.getRequestDispatcher("/WEB-INF/views/admin/events.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = Web.str(req, "action");
        try {
            switch (action) {
                case "save":    save(req, resp); break;
                case "delete":
                    dao.delete(Web.intp(req, "id", 0));
                    Web.ok(resp, "Đã xóa sự kiện.");
                    break;
                case "feature":
                    dao.setFeatured(Web.intp(req, "id", 0), "1".equals(Web.str(req, "value")));
                    Web.ok(resp, "1".equals(Web.str(req, "value")) ? "Đã ghim làm sự kiện nổi bật ở trang chủ." : "Đã bỏ ghim nổi bật.");
                    break;
                case "status": {
                    String st = Web.str(req, "value");
                    if (!Arrays.asList(Event.STATUSES).contains(st)) { Web.fail(resp, 400, "Trạng thái không hợp lệ."); return; }
                    dao.setStatus(Web.intp(req, "id", 0), st);
                    Web.ok(resp, "Đã chuyển trạng thái sang \"" + st + "\".");
                    break;
                }
                default: Web.fail(resp, 400, "Yêu cầu không hợp lệ.");
            }
        } catch (Exception e) {
            Admin.handleError(resp, e);
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        int id = Web.intp(req, "id", 0);
        String title = Web.str(req, "title");
        String location = Web.str(req, "location");
        String desc = Web.str(req, "description");
        String tag = Web.str(req, "tagLabel");
        String color = Web.str(req, "bannerColor");
        String status = Web.str(req, "status");
        int category = Web.intp(req, "categoryId", 0);
        int capacity = Web.intp(req, "capacity", 0);

        if (title.length() < 3 || title.length() > 200) { Web.fail(resp, 400, "Tên sự kiện phải từ 3 đến 200 ký tự."); return; }
        if (location.isEmpty() || location.length() > 255) { Web.fail(resp, 400, "Vui lòng nhập địa điểm (tối đa 255 ký tự)."); return; }
        if (desc.length() > 2000) { Web.fail(resp, 400, "Mô tả tối đa 2000 ký tự."); return; }
        if (tag.length() > 100) { Web.fail(resp, 400, "Nhãn hiển thị tối đa 100 ký tự."); return; }
        if (!color.matches("#[0-9a-fA-F]{6}")) color = "#3a68ff";
        if (capacity < 1 || capacity > 1_000_000) { Web.fail(resp, 400, "Sức chứa phải từ 1 đến 1.000.000."); return; }
        if (!Arrays.asList(Event.STATUSES).contains(status)) status = Event.STATUS_UPCOMING;
        if (category <= 0) { Web.fail(resp, 400, "Vui lòng chọn danh mục."); return; }

        Timestamp when;
        try { when = Timestamp.valueOf(LocalDateTime.parse(Web.str(req, "date"))); }
        catch (Exception e) { Web.fail(resp, 400, "Ngày giờ diễn ra không hợp lệ."); return; }

        // ---- ảnh: tải lên > chọn ảnh có sẵn > giữ ảnh cũ ----
        String image = null;
        jakarta.servlet.http.Part part = null;
        try { part = req.getPart("image"); } catch (Exception ignored) {}
        String uploaded = Uploads.saveImage(part);
        String preset = Web.str(req, "imageChoice");
        if (uploaded != null) image = uploaded;
        else if (preset.matches("assets/images/events/event-[1-6]\\.svg")) image = preset;
        else if (preset.matches("uploads/[a-f0-9]{32}\\.(png|jpg|gif|webp)")) image = preset;

        boolean featured = "1".equals(Web.str(req, "featured")) || "on".equals(Web.str(req, "featured"));

        Event e = new Event();
        e.setCategoryId(category);
        e.setTitle(title);
        e.setLocation(location);
        e.setDescription(desc);
        e.setTagLabel(tag);
        e.setBannerColor(color);
        e.setStatus(status);
        e.setTotalCapacity(capacity);
        e.setEventDate(when);
        e.setFeatured(featured);
        e.setImageUrl(image);

        if (id > 0) {
            Event old = dao.adminGet(id);
            if (old == null) { Web.fail(resp, 404, "Không tìm thấy sự kiện."); return; }
            if (capacity < old.getIssuedTickets()) {
                Web.fail(resp, 400, "Sức chứa không được nhỏ hơn tổng vé đã phát hành (" + old.getIssuedTickets() + "). Hãy giảm số lượng ở mục Loại vé trước.");
                return;
            }
            e.setEventId(id);
            if (image == null) e.setImageUrl(old.getImageUrl());
            dao.update(e);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            Web.ok(resp, "Đã lưu thay đổi sự kiện.", m);
        } else {
            BigDecimal price;
            try { price = Web.decimal(Web.str(req, "minPrice")); if (price.signum() < 0) throw new Exception(); }
            catch (Exception ex) { Web.fail(resp, 400, "Giá vé thấp nhất không hợp lệ."); return; }
            e.setMinPrice(price);
            int newId = dao.create(e);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", newId);
            Web.ok(resp, "Đã tạo sự kiện mới. Hãy thêm các hạng vé ở mục Loại vé.", m);
        }
    }
}
