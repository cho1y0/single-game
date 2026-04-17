package com.mixmaster.api.auth;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import com.google.gson.Gson;
import com.mixmaster.util.DatabaseUtil;

/**
 * 로그인 API 처리 서블릿 (엔드포인트: /api/auth/login)
 */
@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {

    // 요청 데이터를 매핑할 DTO 클래스
    private static class LoginRequestDto {
        String username;
        String password;
    }

    // 응답 데이터를 매핑할 DTO 클래스
    private static class LoginResponseDto {
        boolean success;
        String sessionKey;
        String message;
        String nickname;
        int level;

        public LoginResponseDto(boolean success, String sessionKey, String message, String nickname, int level) {
            this.success = success;
            this.sessionKey = sessionKey;
            this.message = message;
            this.nickname = nickname;
            this.level = level;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1. 응답 인코딩 및 CORS 설정 (JSON 형식으로 응답)
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        req.setCharacterEncoding("UTF-8");

        // 2. 유니티 클라이언트에서 보낸 JSON Body 읽기
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String jsonString = sb.toString();

        // 3. Gson을 이용한 JSON 파싱
        Gson gson = new Gson();
        LoginRequestDto requestDto = gson.fromJson(jsonString, LoginRequestDto.class);
        String username = (requestDto != null && requestDto.username != null) ? requestDto.username : "";
        String password = (requestDto != null && requestDto.password != null) ? requestDto.password : "";

        System.out.println("[INFO] Login Attempt - Username: " + username);

        PrintWriter out = resp.getWriter();

        // 4. 데이터베이스 검증 (현재는 임시로 test / 1234 만 성공 처리)
        boolean isValidUser = checkDatabaseForUser(username, password);

        LoginResponseDto responseDto;
        if (isValidUser) {
            // 로그인 성공: 세션 키(UUID) 발급
            String sessionKey = UUID.randomUUID().toString();
            
            // DB에 발급된 sessionKey와 접속 시간을 업데이트(UPDATE)
            boolean isUpdated = updateSessionKey(username, sessionKey);
            
            if (isUpdated) {
                System.out.println("[INFO] Login Success - SessionKey issued and saved: " + sessionKey);
                
                // DB(tb_user)에서 유저 닉네임과 레벨 정보 가져오기
                UserInfo userInfo = fetchUserInfo(username);
                
                responseDto = new LoginResponseDto(
                        true, 
                        sessionKey, 
                        "로그인에 성공했습니다.", 
                        userInfo.nickname, 
                        userInfo.level
                );
            } else {
                System.err.println("[ERROR] Login Failed - Failed to update SessionKey in DB.");
                responseDto = new LoginResponseDto(false, "", "서버 오류로 세션을 생성하지 못했습니다.", "", 0);
            }
        } else {
            // 로그인 실패
            System.out.println("[WARN] Login Failed - Invalid credentials for: " + username);
            responseDto = new LoginResponseDto(false, "", "아이디 또는 비밀번호가 일치하지 않습니다.", "", 0);
        }
        
        // DTO를 JSON 문자열로 변환하여 응답
        out.print(gson.toJson(responseDto));
        out.flush();
    }

    // [보안 고려사항 B] SQL Injection 방지를 위한 PreparedStatement 사용 예시 뼈대
    private boolean checkDatabaseForUser(String username, String password) {

        // mixmaster 데이터베이스의 tb_user_auth 테이블에서 유저를 조회합니다.
        String sql = "SELECT password FROM mixmaster.tb_user_auth WHERE username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String dbPassword = rs.getString("password");
                    // SHA-256으로 암호화된 비밀번호와 대조
                    return hashPassword(password).equals(dbPassword); 
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] DB 조회 중 오류: " + e.getMessage());
            return false;
        }
    }

    // 발급된 세션 키와 현재 접속 시간을 DB에 업데이트하는 메서드
    private boolean updateSessionKey(String username, String sessionKey) {
        // NOW() 함수를 사용하여 MySQL 서버의 현재 시간을 last_login에 기록합니다.
        String sql = "UPDATE mixmaster.tb_user_auth SET session_key = ?, last_login = NOW() WHERE username = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sessionKey);
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0; // 업데이트된 행(row)이 1개 이상이면 성공
        } catch (Exception e) {
            System.err.println("[ERROR] 세션 키 DB 업데이트 중 오류: " + e.getMessage());
            return false;
        }
    }

    // 유저 정보를 담을 임시 클래스
    private static class UserInfo {
        String nickname = "";
        int level = 0;
    }

    // tb_user 테이블에서 유저 정보를 조회하는 메서드
    private UserInfo fetchUserInfo(String username) {
        UserInfo info = new UserInfo();
        String sql = "SELECT nickname, level FROM mixmaster.tb_user WHERE username = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    info.nickname = rs.getString("nickname");
                    info.level = rs.getInt("level");
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] 유저 정보(tb_user) 조회 중 오류: " + e.getMessage());
        }
        return info;
    }

    // 단방향 해시 알고리즘(SHA-256)을 이용한 비밀번호 암호화 헬퍼 메서드
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(password.getBytes());
            byte[] byteData = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : byteData) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("비밀번호 암호화 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}