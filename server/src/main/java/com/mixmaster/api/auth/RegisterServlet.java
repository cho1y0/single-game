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
import com.google.gson.Gson;
import com.mixmaster.util.DatabaseUtil;

/**
 * 회원가입 API 처리 서블릿 (엔드포인트: /api/auth/register)
 */
@WebServlet("/api/auth/register")
public class RegisterServlet extends HttpServlet {

    // 유니티에서 보낸 요청 데이터를 매핑할 DTO 클래스
    private static class RegisterRequestDto {
        String username;
        String password;
    }

    // 유니티로 돌려보낼 응답 데이터를 매핑할 DTO 클래스
    private static class RegisterResponseDto {
        boolean success;
        String message;

        public RegisterResponseDto(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1. 응답 설정 (JSON 형식)
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        req.setCharacterEncoding("UTF-8");

        // 2. 유니티에서 보낸 JSON Body 읽어오기
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        // 3. Gson을 이용해 JSON 데이터를 자바 객체로 변환
        Gson gson = new Gson();
        RegisterRequestDto requestDto = gson.fromJson(sb.toString(), RegisterRequestDto.class);
        String username = (requestDto != null && requestDto.username != null) ? requestDto.username.trim() : "";
        String password = (requestDto != null && requestDto.password != null) ? requestDto.password : "";

        System.out.println("[INFO] Register Attempt - Username: " + username);

        PrintWriter out = resp.getWriter();
        RegisterResponseDto responseDto;

        // 4. 아이디 중복 검사 및 DB 저장 로직
        if (username.isEmpty() || password.isEmpty()) {
            responseDto = new RegisterResponseDto(false, "아이디와 비밀번호를 제대로 입력해주세요.");
        } else if (isUserExist(username)) {
            // 이미 DB에 같은 아이디가 있다면
            System.out.println("[WARN] Register Failed - Username already exists: " + username);
            responseDto = new RegisterResponseDto(false, "이미 존재하는 아이디입니다.");
        } else {
            // DB에 새 유저 정보 인서트(Insert)
            boolean isInserted = registerNewUser(username, password);
            if (isInserted) {
                System.out.println("[INFO] Register Success - New user created: " + username);
                responseDto = new RegisterResponseDto(true, "회원가입이 완료되었습니다!");
            } else {
                System.err.println("[ERROR] Register Failed - Database insert error.");
                responseDto = new RegisterResponseDto(false, "회원가입 처리 중 서버 오류가 발생했습니다.");
            }
        }

        // 결과를 유니티로 전송
        out.print(gson.toJson(responseDto));
        out.flush();
    }

    // 아이디가 이미 존재하는지 검사하는 메서드
    private boolean isUserExist(String username) {
        String sql = "SELECT COUNT(*) FROM mixmaster.tb_user_auth WHERE username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0; // 0보다 크면 이미 존재함
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // 새로운 유저 정보를 DB에 저장하는 메서드
    private boolean registerNewUser(String username, String password) {
        String sql = "INSERT INTO mixmaster.tb_user_auth (username, password) VALUES (?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashPassword(password)); // SHA-256 암호화 적용
            return pstmt.executeUpdate() > 0; // 성공적으로 1줄이 추가되었는지 확인
        } catch (Exception e) { e.printStackTrace(); }
        return false;
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