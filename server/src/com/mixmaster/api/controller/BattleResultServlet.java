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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 전투 결과 및 아이템 드롭 처리 API
 * 엔드포인트: POST /api/battle/result
 */
@WebServlet("/api/battle/result")
public class BattleResultServlet extends HttpServlet {
    private static final Gson gson = new Gson();
    private static final Random random = new Random();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        try {
            BufferedReader reader = req.getReader();
            JsonObject jsonRequest = gson.fromJson(reader, JsonObject.class);

            if (jsonRequest == null || !jsonRequest.has("user_id") || !jsonRequest.has("defeated_monster_id")) {
                resp.getWriter().write(ApiResponse.error(400, "user_id와 defeated_monster_id가 필요합니다.").toJson());
                return;
            }

            String userId = jsonRequest.get("user_id").getAsString();
            int monsterId = jsonRequest.get("defeated_monster_id").getAsInt();

            List<Integer> droppedItems = new ArrayList<>();

            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // 1. 해당 몬스터의 드롭 테이블 조회 및 확률 굴리기
                    String dropSql = "SELECT item_id, drop_rate FROM tb_monster_drop WHERE monster_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(dropSql)) {
                        pstmt.setInt(1, monsterId);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            while (rs.next()) {
                                int itemId = rs.getInt("item_id");
                                double dropRate = rs.getDouble("drop_rate");
                                
                                if (random.nextDouble() * 100.0 <= dropRate) {
                                    droppedItems.add(itemId);
                                }
                            }
                        }
                    }

                    // 2. 획득한 아이템을 인벤토리에 지급
                    if (!droppedItems.isEmpty()) {
                        String insertSql = "INSERT INTO tb_user_inventory (user_id, item_id, quantity) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE quantity = quantity + 1";
                        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                            for (int itemId : droppedItems) {
                                pstmt.setString(1, userId);
                                pstmt.setInt(2, itemId);
                                pstmt.executeUpdate();
                            }
                        }
                    }
                    conn.commit();
                    
                    resp.getWriter().write(ApiResponse.success(droppedItems).toJson());
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (Exception e) {
            resp.getWriter().write(ApiResponse.error(500, "서버 오류 발생").toJson());
        }
    }
}