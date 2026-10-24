package com.tickethub.util;

import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Lưu ảnh sự kiện do admin tải lên (ngoài thư mục ứng dụng để không mất khi build lại). */
public final class Uploads {

    private Uploads() {}

    public static final long MAX_BYTES = 3L * 1024 * 1024;

    public static File dir() {
        String custom = System.getProperty("tickethub.uploads");
        File d;
        if (custom != null && !custom.isEmpty()) d = new File(custom);
        else {
            String base = System.getProperty("catalina.base");
            d = new File(base != null ? base : System.getProperty("user.home"), "tickethub-uploads");
        }
        if (!d.exists()) d.mkdirs();
        return d;
    }

    /** Lưu ảnh, trả về đường dẫn tương đối (uploads/xxx.png). Ném lỗi nếu không phải ảnh hợp lệ. */
    public static String saveImage(Part part) throws IOException {
        if (part == null || part.getSize() == 0) return null;
        if (part.getSize() > MAX_BYTES) throw new Jdbc.DataAccessException("Ảnh quá lớn (tối đa 3MB).");
        byte[] head = new byte[12];
        int n;
        try (InputStream in = part.getInputStream()) { n = in.read(head); }
        String ext = sniff(head, n);
        if (ext == null) throw new Jdbc.DataAccessException("Chỉ chấp nhận ảnh PNG, JPG, GIF hoặc WEBP.");
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, new File(dir(), name).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return "uploads/" + name;
    }

    /** Nhận diện định dạng theo nội dung (không tin vào đuôi file hay Content-Type do client gửi). */
    private static String sniff(byte[] b, int n) {
        if (n >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (n >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (n >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return "gif";
        if (n >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }
}
