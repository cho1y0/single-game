package com.mixmaster.api.user;

import com.google.gson.Gson;
import com.mixmaster.util.DatabaseUtil;

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
import java.sql.SQLException;

/**
 * 캐릭터 생성 API 처리 서블릿 (엔드포인트: /api/user/create)
 */
@WebServlet("/api/user/create")
public class CharacterCreateServlet extends HttpServlet {

    // 유니티에서 보낸 요청 데이터를 매핑할 DTO 클래스
    private static class CreateCharacterRequestDto {
        String sessionKey;
        String nickname;
        int charId;
    }

    // 유니티로 돌려보낼 응답 데이터를 매핑할 DTO 클래스
    private static class CreateCharacterResponseDto {
        boolean success;
        String message;

        public CreateCharacterResponseDto(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1. 기본 설정
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        req.setCharacterEncoding("UTF-8");

        // 2. 요청 Body 읽기
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        // 3. JSON 파싱 및 기본 유효성 검사
        Gson gson = new Gson();
        PrintWriter out = resp.getWriter();
        CreateCharacterRequestDto requestDto = gson.fromJson(sb.toString(), CreateCharacterRequestDto.class);

        if (requestDto == null || requestDto.sessionKey == null || requestDto.nickname == null || requestDto.nickname.trim().isEmpty() || requestDto.charId <= 0) {
            out.print(gson.toJson(new CreateCharacterResponseDto(false, "요청 정보가 올바르지 않습니다.")));
            out.flush();
            return;
        }

        // 4. 세션 키로 유저 아이디(username) 조회
        String username = getUsernameBySessionKey(requestDto.sessionKey);
        if (username == null) {
            out.print(gson.toJson(new CreateCharacterResponseDto(false, "인증 정보가 유효하지 않습니다. 다시 로그인해주세요.")));
            out.flush();
            return;
        }

        System.out.println("[INFO] Character Create Attempt - User: " + username + ", Nickname: " + requestDto.nickname);

        // 5. DB 트랜잭션 내에서 비즈니스 로직 검증 및 캐릭터 생성 (Race Condition 방지)
        CreateCharacterResponseDto responseDto = createCharacterInTransaction(username, requestDto.nickname, requestDto.charId);

        // 6. 최종 결과 응답
        out.print(gson.toJson(responseDto));
        out.flush();
    }

    // 세션키로 유저 아이디를 찾는 메서드
    private String getUsernameBySessionKey(String sessionKey) {
        String sql = "SELECT username FROM mixmaster.tb_user_auth WHERE session_key = ?";
        try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sessionKey);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("username");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // 하나의 트랜잭션에서 닉네임 중복 체크, 유저 캐릭터 확인, 삽입을 일괄 처리하는 메서드
    private CreateCharacterResponseDto createCharacterInTransaction(String username, String nickname, int charId) {
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작

            // 1. 닉네임 중복 검사
            String checkNicknameSql = "SELECT COUNT(*) FROM mixmaster.tb_user WHERE nickname = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkNicknameSql)) {
                pstmt.setString(1, nickname);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return new CreateCharacterResponseDto(false, "이미 사용 중인 닉네임입니다.");
                    }
                }
            }

            // 2. 유저의 캐릭터 보유 여부 검사
            String checkUserSql = "SELECT COUNT(*) FROM mixmaster.tb_user WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkUserSql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return new CreateCharacterResponseDto(false, "이미 생성된 캐릭터가 있습니다.");
                    }
                }
            }

            // 3. 캐릭터 생성
            String insertSql = "INSERT INTO mixmaster.tb_user (username, nickname, char_id) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, nickname);
                pstmt.setInt(3, charId);
                pstmt.executeUpdate();
            }

            conn.commit(); // 모든 쿼리 성공 시 커밋
            System.out.println("[INFO] Character Create Success - User: " + username + ", Nickname: " + nickname);
            return new CreateCharacterResponseDto(true, "캐릭터가 성공적으로 생성되었습니다!");

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            System.err.println("[ERROR] Character Create Failed - DB error for user: " + username);
            e.printStackTrace();
            return new CreateCharacterResponseDto(false, "캐릭터 생성 중 서버 오류가 발생했습니다.");
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}