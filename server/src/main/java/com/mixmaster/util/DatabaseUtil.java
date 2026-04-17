package com.mixmaster.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * MySQL 데이터베이스 커넥션을 제공하는 유틸리티 클래스
 */
public class DatabaseUtil {
    private static String URL;
    private static String USER;
    private static String PASS;

    static {
        // 1. Tomcat 환경 클래스로더에서 먼저 찾기
        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties");
        
        // 2. 못 찾으면 일반 자바 클래스로더에서 한 번 더 찾기
        if (input == null) {
            input = DatabaseUtil.class.getClassLoader().getResourceAsStream("db.properties");
        }

        if (input != null) {
            try (InputStream is = input) {
                Properties prop = new Properties();
                prop.load(is);
                URL = prop.getProperty("db.url", "");
                USER = prop.getProperty("db.user", "");
                PASS = prop.getProperty("db.password", "");
                System.out.println("[INFO] db.properties 읽기 성공! (접속 시도 USER: '" + USER + "')");
            } catch (Exception e) {
                System.err.println("[ERROR] db.properties 읽기 실패: " + e.getMessage());
            }
        } else {
            System.err.println("[WARN] 클래스패스에서 db.properties 파일을 찾을 수 없습니다.");
            throw new RuntimeException("보안 정책 위반: db.properties 파일을 찾을 수 없어 DB 연결을 차단합니다.");
        }

        try {
            // MySQL JDBC 드라이버 로드
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("[INFO] MySQL JDBC 드라이버 로드 완료");
        } catch (Exception e) {
            System.err.println("[ERROR] DatabaseUtil 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
