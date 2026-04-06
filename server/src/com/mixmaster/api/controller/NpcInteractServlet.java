package com.mixmaster.api.controller;

import com.google.gson.Gson;
import com.mixmaster.api.response.ApiResponse;
import com.mixmaster.api.util.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * NPC 상호작용 데이터 호출 API
 * 엔드포인트: GET /api/npc/interact
 */
@WebServlet("/api/npc/interact")
public class NpcInteractServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        String npcIdParam = req.getParameter("npc_id");
        if (npcIdParam == null) {
            resp.getWriter().write(ApiResponse.error(400, "npc_id 파라미터가 필요합니다.").toJson());
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT name, role FROM tb_npc_info WHERE npc_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, Integer.parseInt(npcIdParam));
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, String> npcData = new HashMap<>();
                        npcData.put("name", rs.getString("name"));
                        npcData.put("role", rs.getString("role"));
                        resp.getWriter().write(ApiResponse.success(npcData).toJson());
                    } else {
                        resp.getWriter().write(ApiResponse.error(404, "NPC를 찾을 수 없습니다.").toJson());
                    }
                }
            }
        } catch (Exception e) {
            resp.getWriter().write(ApiResponse.error(500, "서버 오류 발생").toJson());
        }
    }
}