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

/**
 * 아이템 생산 API
 * 엔드포인트: POST /api/item/produce
 */
@WebServlet("/api/item/produce")
public class ProduceServlet extends HttpServlet {
    private static final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        try {
            BufferedReader reader = req.getReader();
            JsonObject jsonRequest = gson.fromJson(reader, JsonObject.class);

            if (jsonRequest == null || !jsonRequest.has("user_id") || !jsonRequest.has("recipe_id")) {
                resp.getWriter().write(ApiResponse.error(400, "user_id와 recipe_id가 필요합니다.").toJson());
                return;
            }

            String userId = jsonRequest.get("user_id").getAsString();
            int recipeId = jsonRequest.get("recipe_id").getAsInt();

            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // 1. 레시피 정보 조회
                    int reqItem1 = 0, reqItem2 = 0, reqItem3 = 0, resultItem = 0;
                    String recipeSql = "SELECT recipe_item_id, main_material_id, sub_material_id, result_item_id FROM tb_production_recipe WHERE recipe_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(recipeSql)) {
                        pstmt.setInt(1, recipeId);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            if (rs.next()) {
                                reqItem1 = rs.getInt("recipe_item_id");
                                reqItem2 = rs.getInt("main_material_id");
                                reqItem3 = rs.getInt("sub_material_id");
                                resultItem = rs.getInt("result_item_id");
                            } else {
                                resp.getWriter().write(ApiResponse.error(404, "존재하지 않는 레시피입니다.").toJson());
                                return;
                            }
                        }
                    }

                    // 2. 유저 인벤토리에 재료가 모두 있는지 1개씩 차감 시도 (수량이 부족하면 실패)
                    String deductSql = "UPDATE tb_user_inventory SET quantity = quantity - 1 WHERE user_id = ? AND item_id = ? AND quantity >= 1";
                    try (PreparedStatement pstmt = conn.prepareStatement(deductSql)) {
                        int[] requiredItems = {reqItem1, reqItem2, reqItem3};
                        for (int itemId : requiredItems) {
                            pstmt.setString(1, userId);
                            pstmt.setInt(2, itemId);
                            int updated = pstmt.executeUpdate();
                            if (updated == 0) {
                                conn.rollback();
                                resp.getWriter().write(ApiResponse.error(400, "생산 재료가 부족합니다. (아이템 ID: " + itemId + ")").toJson());
                                return;
                            }
                        }
                    }

                    // 3. 수량이 0이 된 재료는 인벤토리에서 삭제 (정리)
                    String cleanupSql = "DELETE FROM tb_user_inventory WHERE user_id = ? AND quantity <= 0";
                    try (PreparedStatement pstmt = conn.prepareStatement(cleanupSql)) {
                        pstmt.setString(1, userId);
                        pstmt.executeUpdate();
                    }

                    // 4. 결과물 지급 (있으면 수량+1, 없으면 새로 추가)
                    String insertSql = "INSERT INTO tb_user_inventory (user_id, item_id, quantity) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE quantity = quantity + 1";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                        pstmt.setString(1, userId);
                        pstmt.setInt(2, resultItem);
                        pstmt.executeUpdate();
                    }

                    conn.commit();

                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("result_item_id", resultItem);
                    resp.getWriter().write(ApiResponse.success(resultData).toJson());

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