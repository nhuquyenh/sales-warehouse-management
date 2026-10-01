package com.saleswarehouse.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnectionFactory {

    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DATABASE = "sales_warehouse";
    private static final String DEFAULT_USERNAME = "sales_user";

    private final String url;
    private final String username;
    private final String password;

    static {
        try {
            Class.forName(MYSQL_DRIVER);
        } catch (ClassNotFoundException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    public DatabaseConnectionFactory() {
        String host = setting("db.host", "MYSQL_HOST", DEFAULT_HOST);
        String port = setting("db.port", "MYSQL_PORT", DEFAULT_PORT);
        String database = setting("db.name", "MYSQL_DATABASE", DEFAULT_DATABASE);

        this.url = setting(
                "db.url",
                "DB_URL",
                "jdbc:mysql://%s:%s/%s"
                        .formatted(host, port, database)
                        + "?useUnicode=true"
                        + "&characterEncoding=UTF-8"
                        + "&serverTimezone=Asia/Ho_Chi_Minh"
                        + "&allowPublicKeyRetrieval=true"
                        + "&useSSL=false"
        );
        this.username = firstNonBlank(
                System.getProperty("db.username"),
                System.getenv("DB_USERNAME"),
                System.getenv("MYSQL_USER"),
                DEFAULT_USERNAME
        );
        this.password = firstNonBlank(
                System.getProperty("db.password"),
                System.getenv("DB_PASSWORD"),
                System.getenv("MYSQL_PASSWORD")
        );
    }

    public Connection getConnection() throws SQLException {
        if (password == null) {
            throw new IllegalStateException(
                    "Database password is not configured. Set DB_PASSWORD or MYSQL_PASSWORD."
            );
        }
        return DriverManager.getConnection(url, username, password);
    }

    private static String setting(String propertyName, String environmentName, String defaultValue) {
        return firstNonBlank(
                System.getProperty(propertyName),
                System.getenv(environmentName),
                defaultValue
        );
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
