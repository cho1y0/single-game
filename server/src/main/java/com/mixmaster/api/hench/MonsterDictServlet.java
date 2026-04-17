package com.mixmaster.api.hench;

import com.google.gson.Gson;
import com.mixmaster.util.DatabaseUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * 몬스터 도감 정보를 반환하는 API (엔드포인트: /api/monster/dict)
 */
@WebServlet("/api/monster/dict")
public class MonsterDictServlet extends HttpServlet {

    private static class MonsterDto {
        int monsterId;
        String name;
        String race;
        int tier;
        int baseLevel;
    }

    private static class MonsterListResponseDto {
        boolean success;
        List<MonsterDto> monsters;
        String message;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        MonsterListResponseDto responseDto = new MonsterListResponseDto();
        responseDto.monsters = new ArrayList<>();
        
        // 테스트를 위해 최대 20마리의 도감 정보를 가져옵니다.
        String sql = "SELECT monster_id, name, race, tier, base_level FROM mixmaster.tb_monster_dict LIMIT 20";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                MonsterDto monster = new MonsterDto();
                monster.monsterId = rs.getInt("monster_id");
                monster.name = rs.getString("name");
                monster.race = rs.getString("race");
                monster.tier = rs.getInt("tier");
                monster.baseLevel = rs.getInt("base_level");
                responseDto.monsters.add(monster);
            }
            responseDto.success = true;
            responseDto.message = "몬스터 정보를 성공적으로 불러왔습니다.";
        } catch (Exception e) {
            e.printStackTrace();
            responseDto.success = false;
            responseDto.message = "DB 조회 중 오류 발생";
        }

        PrintWriter out = resp.getWriter();
        out.print(new Gson().toJson(responseDto));
        out.flush();
    }
}
