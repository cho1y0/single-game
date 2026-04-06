package com.mixmaster.api.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mixmaster.api.response.ApiResponse;
import com.mixmaster.api.util.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 로그인 및 유저 초기 데이터 로드 API
 * 엔드포인트: POST /api/auth/login
 */
@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {
    private static final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1. 입출력 인코딩 설정 (한글 깨짐 방지 및 JSON 응답 설정)
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        try {
            // 2. 클라이언트(Unity)가 보낸 JSON 바디 읽기
            BufferedReader reader = req.getReader();
            JsonObject jsonRequest = gson.fromJson(reader, JsonObject.class);
            
            if (jsonRequest == null || !jsonRequest.has("user_id")) {
                resp.getWriter().write(ApiResponse.error(400, "user_id가 필요합니다.").toJson());
                return;
            }
            
            String userId = jsonRequest.get("user_id").getAsString();

            // 3. DB 통신: 유저 검증 및 정보 조회
            try (Connection conn = DBConnection.getConnection()) {
                // SQL Injection 방지를 위한 PreparedStatement 사용 (보안 문서 준수)
                String sql = "SELECT u.user_id, u.nickname, u.level, u.gold, u.current_map_id " +
                             "FROM tb_user u " +
                             "WHERE u.user_id = ?";
                             
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, userId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            // 4. 로그인 성공: 데이터를 Map에 담아 ApiResponse로 감싸서 반환
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("user_id", rs.getString("user_id"));
                            userData.put("nickname", rs.getString("nickname"));
                            userData.put("level", rs.getInt("level"));
                            userData.put("gold", rs.getInt("gold"));
                            userData.put("current_map_id", rs.getInt("current_map_id"));

                            resp.getWriter().write(ApiResponse.success(userData).toJson());
                        } else {
                            resp.getWriter().write(ApiResponse.error(404, "존재하지 않는 유저입니다.").toJson());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write(ApiResponse.error(500, "서버 내부 오류가 발생했습니다.").toJson());
        }
    }
}