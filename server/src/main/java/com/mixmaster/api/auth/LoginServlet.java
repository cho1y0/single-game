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

        public LoginResponseDto(boolean success, String sessionKey, String message) {
            this.success = success;
            this.sessionKey = sessionKey;
            this.message = message;
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
            
            // TODO: 설계서에 명시된 tb_user_auth 테이블에 발급된 sessionKey와 접속 시간을 업데이트(UPDATE) 해야 함
            System.out.println("[INFO] Login Success - SessionKey issued: " + sessionKey);

            responseDto = new LoginResponseDto(true, sessionKey, "로그인에 성공했습니다.");
        } else {
            // 로그인 실패
            System.out.println("[WARN] Login Failed - Invalid credentials for: " + username);
            responseDto = new LoginResponseDto(false, "", "아이디 또는 비밀번호가 일치하지 않습니다.");
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
                    return password.equals(dbPassword); // 유니티에서 온 비번과 DB 비번이 완벽히 똑같은지 비교
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] DB 조회 중 오류: " + e.getMessage());
            return false;
        }
    }
}