package com.tickethub.context;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBContext - Kết nối JDBC tới Microsoft SQL Server (Ant Project - NetBeans - Tomcat 10)
 */
public class DBContext {

    private static final String SERVER_NAME = "localhost";
    private static final String PORT_NUMBER = "1433";
    private static final String DATABASE_NAME = "TicketHubDB";
    private static final String USER_ID = "sa";
    private static final String PASSWORD = "24092007"; // Đổi mật khẩu sa của bạn tại đây

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        String url = String.format(
            "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=true;trustServerCertificate=true;characterEncoding=UTF-8",
            SERVER_NAME, PORT_NUMBER, DATABASE_NAME
        );
        return DriverManager.getConnection(url, USER_ID, PASSWORD);
    }

    public static void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception ignored) {}
        }   
    }
}
