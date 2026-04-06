package com.mixmaster.api.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * MySQL 데이터베이스 연결을 관리하는 유틸리티 클래스 (보안 규정 준수)
 */
public class DBConnection {
    private static String URL;
    private static String USER;
    private static String PASSWORD;

    // 클래스 로드 시 db.properties 파일에서 민감 정보를 읽어옵니다.
    static {
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                System.err.println("[ERROR] db.properties 설정 파일을 찾을 수 없습니다.");
            } else {
                prop.load(input);
                URL = prop.getProperty("db.url");
                USER = prop.getProperty("db.user");
                PASSWORD = prop.getProperty("db.password");
            }
            
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("[INFO] MySQL JDBC 드라이버 로드 성공");
        } catch (Exception e) {
            System.err.println("[ERROR] 데이터베이스 초기화 및 드라이버 로드 실패");
            e.printStackTrace();
        }
    }

    /**
     * 데이터베이스와 연결된 Connection 객체를 반환합니다.
     */
    public static Connection getConnection() throws SQLException {
        if (URL == null || USER == null || PASSWORD == null) {
            throw new SQLException("데이터베이스 접속 정보가 설정되지 않았습니다. db.properties를 확인하세요.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}