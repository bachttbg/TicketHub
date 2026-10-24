package com.tickethub.controller;

import com.tickethub.util.Uploads;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** Phục vụ ảnh sự kiện đã tải lên: /uploads/<tên-file>. */
@WebServlet(name = "UploadServlet", urlPatterns = {"/uploads/*"})
public class UploadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getPathInfo() == null ? "" : req.getPathInfo().substring(1);
        if (!name.matches("[a-f0-9]{32}\\.(png|jpg|gif|webp)")) { resp.sendError(404); return; }
        File f = new File(Uploads.dir(), name);
        if (!f.isFile()) { resp.sendError(404); return; }
        String ext = name.substring(name.lastIndexOf('.') + 1);
        resp.setContentType(ext.equals("jpg") ? "image/jpeg" : "image/" + ext);
        resp.setHeader("Cache-Control", "public, max-age=2592000, immutable");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setContentLengthLong(f.length());
        Files.copy(f.toPath(), resp.getOutputStream());
    }
}
