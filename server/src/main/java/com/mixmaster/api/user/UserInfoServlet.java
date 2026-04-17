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

/**
 * 접속 중인 유저의 캐릭터 및 위치 정보를 반환하는 API (엔드포인트: /api/user/info)
 */
@WebServlet("/api/user/info")
public class UserInfoServlet extends HttpServlet {

    private static class UserInfoRequestDto {
        String sessionKey;
    }

    private static class UserInfoResponseDto {
        boolean success;
        int charId;
        float posX;
        float posY;
        String message;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        req.setCharacterEncoding("UTF-8");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        Gson gson = new Gson();
        UserInfoRequestDto requestDto = gson.fromJson(sb.toString(), UserInfoRequestDto.class);
        PrintWriter out = resp.getWriter();
        UserInfoResponseDto responseDto = new UserInfoResponseDto();

        if (requestDto == null || requestDto.sessionKey == null) {
            responseDto.success = false;
            responseDto.message = "세션 키가 없습니다.";
            out.print(gson.toJson(responseDto));
            return;
        }

        // 세션 키를 이용해 인증 테이블과 유저 테이블을 조인(JOIN)하여 캐릭터 정보를 가져옵니다.
        String sql = "SELECT u.char_id, u.pos_x, u.pos_y FROM mixmaster.tb_user u " +
                     "JOIN mixmaster.tb_user_auth a ON u.username = a.username " +
                     "WHERE a.session_key = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, requestDto.sessionKey);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    responseDto.success = true;
                    responseDto.charId = rs.getInt("char_id");
                    responseDto.posX = rs.getFloat("pos_x");
                    responseDto.posY = rs.getFloat("pos_y");
                    responseDto.message = "정보 로드 성공";
                } else {
                    responseDto.success = false;
                    responseDto.message = "캐릭터 정보를 찾을 수 없습니다.";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseDto.success = false;
            responseDto.message = "DB 조회 중 오류 발생";
        }

        out.print(gson.toJson(responseDto));
        out.flush();
    }
}