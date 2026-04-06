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

/**
 * 유저 위치 정보 주기적 저장 API
 * 엔드포인트: POST /api/user/move
 */
@WebServlet("/api/user/move")
public class MoveServlet extends HttpServlet {
    private static final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        try {
            BufferedReader reader = req.getReader();
            JsonObject jsonRequest = gson.fromJson(reader, JsonObject.class);
            
            // 1. 필수 파라미터 검증 (user_id, current_map_id, pos_x, pos_y)
            if (jsonRequest == null || !jsonRequest.has("user_id") || !jsonRequest.has("current_map_id") 
                    || !jsonRequest.has("pos_x") || !jsonRequest.has("pos_y")) {
                resp.getWriter().write(ApiResponse.error(400, "필수 파라미터가 누락되었습니다.").toJson());
                return;
            }
            
            String userId = jsonRequest.get("user_id").getAsString();
            int currentMapId = jsonRequest.get("current_map_id").getAsInt();
            float posX = jsonRequest.get("pos_x").getAsFloat();
            float posY = jsonRequest.get("pos_y").getAsFloat();

            // 2. DB 업데이트 통신
            try (Connection conn = DBConnection.getConnection()) {
                String sql = "UPDATE tb_user SET current_map_id = ?, pos_x = ?, pos_y = ? WHERE user_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, currentMapId);
                    pstmt.setFloat(2, posX);
                    pstmt.setFloat(3, posY);
                    pstmt.setString(4, userId);
                    
                    // executeUpdate()는 변경된 행의 개수를 반환합니다.
                    int updatedRows = pstmt.executeUpdate();
                    if (updatedRows > 0) {
                        // 데이터 반환 없이 성공 메시지만 전달 (성공 응답 헬퍼 메서드 2번 사용)
                        resp.getWriter().write(ApiResponse.success().toJson());
                    } else {
                        resp.getWriter().write(ApiResponse.error(404, "존재하지 않는 유저입니다.").toJson());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write(ApiResponse.error(500, "서버 내부 오류가 발생했습니다.").toJson());
        }
    }
}