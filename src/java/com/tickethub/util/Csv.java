package com.tickethub.util;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Xuất CSV (UTF-8 có BOM để Excel đọc đúng tiếng Việt). */
public final class Csv {

    private Csv() {}

    public static PrintWriter begin(HttpServletResponse resp, String fileName) throws IOException {
        resp.setContentType("text/csv;charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20"));
        PrintWriter w = resp.getWriter();
        w.write('\uFEFF');
        return w;
    }

    public static void row(PrintWriter w, Object... cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(cell(cells[i]));
        }
        sb.append("\r\n");
        w.write(sb.toString());
    }

    public static void row(PrintWriter w, List<?> cells) { row(w, cells.toArray()); }

    private static String cell(Object o) {
        String s = o == null ? "" : o.toString();
        // chống CSV injection khi mở bằng Excel
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0 && !s.matches("-?[0-9.]+")) s = "'" + s;
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
