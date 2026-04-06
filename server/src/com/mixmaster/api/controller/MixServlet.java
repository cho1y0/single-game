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
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 헨치 믹스 처리 API
 * 엔드포인트: POST /api/hench/mix
 */
@WebServlet("/api/hench/mix")
public class MixServlet extends HttpServlet {
    private static final Gson gson = new Gson();
    private static final Random random = new Random();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        try {
            BufferedReader reader = req.getReader();
            JsonObject jsonRequest = gson.fromJson(reader, JsonObject.class);

            // 1. 필수 파라미터 검증 (유저ID, 레시피ID, 재료로 쓸 헨치 고유번호 2개)
            if (jsonRequest == null || !jsonRequest.has("user_id") || !jsonRequest.has("formula_id")
                    || !jsonRequest.has("material_1_uid") || !jsonRequest.has("material_2_uid")) {
                resp.getWriter().write(ApiResponse.error(400, "필수 파라미터가 누락되었습니다.").toJson());
                return;
            }

            String userId = jsonRequest.get("user_id").getAsString();
            int formulaId = jsonRequest.get("formula_id").getAsInt();
            long mat1Uid = jsonRequest.get("material_1_uid").getAsLong();
            long mat2Uid = jsonRequest.get("material_2_uid").getAsLong();

            if (mat1Uid == mat2Uid) {
                resp.getWriter().write(ApiResponse.error(400, "동일한 헨치를 두 번 올릴 수 없습니다.").toJson());
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                // 트랜잭션 시작 (중간에 에러나면 모든 DB 변경사항을 취소(Rollback)하기 위함)
                conn.setAutoCommit(false); 

                try {
                    // 2. 레시피 정보(성공률, 티어, 결과 몬스터) 조회
                    int tier = 0; double baseRate = 0; int resultMonsterId = 0;
                    String formulaSql = "SELECT tier, base_rate, result_monster_id FROM tb_mix_formula WHERE formula_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(formulaSql)) {
                        pstmt.setInt(1, formulaId);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            if (rs.next()) {
                                tier = rs.getInt("tier");
                                baseRate = rs.getDouble("base_rate");
                                resultMonsterId = rs.getInt("result_monster_id");
                            } else {
                                resp.getWriter().write(ApiResponse.error(404, "존재하지 않는 믹스 공식입니다.").toJson());
                                return;
                            }
                        }
                    }

                    // 3. 시스템 설정에서 마스터리 보너스 수치(%) 가져오기
                    double bonusPercent = 0.0;
                    String configSql = "SELECT config_value FROM tb_system_config WHERE config_key = 'mastery_bonus_percent'";
                    try (PreparedStatement pstmt = conn.prepareStatement(configSql); ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) bonusPercent = Double.parseDouble(rs.getString("config_value"));
                    }

                    // 4. 유저가 마스터한 하위 단계 개수 확인
                    int masteredCount = 0;
                    String masterySql = "SELECT COUNT(*) AS cnt FROM tb_mix_mastery WHERE user_id = ? AND tier < ? AND is_master = 1";
                    try (PreparedStatement pstmt = conn.prepareStatement(masterySql)) {
                        pstmt.setString(1, userId); pstmt.setInt(2, tier);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            if (rs.next()) masteredCount = rs.getInt("cnt");
                        }
                    }

                    // 5. 재료 헨치 삭제 (소유권 검증 포함)
                    String deleteSql = "DELETE FROM tb_user_hench WHERE user_id = ? AND hench_uid IN (?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                        pstmt.setString(1, userId); pstmt.setLong(2, mat1Uid); pstmt.setLong(3, mat2Uid);
                        int deletedRows = pstmt.executeUpdate();
                        if (deletedRows != 2) {
                            conn.rollback(); // 소유하지 않은 헨치거나 개수가 안맞으면 트랜잭션 롤백
                            resp.getWriter().write(ApiResponse.error(400, "재료 헨치가 존재하지 않거나 소유자가 아닙니다.").toJson());
                            return;
                        }
                    }

                    // 6. 대망의 믹스 확률 계산 및 판정 (기획서 공식 적용)
                    double finalRate = baseRate + (masteredCount * bonusPercent);
                    double roll = random.nextDouble() * 100.0; // 0.00 ~ 99.99
                    boolean isSuccess = roll <= finalRate;

                    Long newHenchUid = null;
                    if (isSuccess) {
                        // 7. 성공 시: 결과 헨치 추가 (1레벨, 보관함 슬롯 0번)
                        // 기획서 4.1 항목에 맞게 랜덤 성향(Prefix) 부여
                        String[] prefixes = {"힘쎈", "날쎈", "정확한", "운좋은", "속성강한"};
                        String randomPrefix = prefixes[random.nextInt(prefixes.length)];
                        
                        String insertSql = "INSERT INTO tb_user_hench (user_id, monster_id, level, exp, prefix, equip_slot) VALUES (?, ?, 1, 0, ?, 0)";
                        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                            pstmt.setString(1, userId); pstmt.setInt(2, resultMonsterId); pstmt.setString(3, randomPrefix);
                            pstmt.executeUpdate();
                            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                                if (rs.next()) newHenchUid = rs.getLong(1);
                            }
                        }
                    }

                    // 8. 마스터리 누적 (데이터가 없으면 INSERT, 있으면 UPDATE)
                    String upsertMasterySql = "INSERT INTO tb_mix_mastery (user_id, tier, mastery_point, is_master) VALUES (?, ?, 1, 0) " +
                                              "ON DUPLICATE KEY UPDATE mastery_point = mastery_point + 1";
                    try (PreparedStatement pstmt = conn.prepareStatement(upsertMasterySql)) {
                        pstmt.setString(1, userId); pstmt.setInt(2, tier);
                        pstmt.executeUpdate();
                    }

                    conn.commit(); // 모든 DB 작업이 정상 완료되면 커밋(확정)

                    // 9. 클라이언트에게 결과 반환
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("success", isSuccess);
                    resultData.put("final_rate", finalRate);
                    if (isSuccess) resultData.put("new_hench_uid", newHenchUid);
                    
                    String message = isSuccess ? "믹스에 성공했습니다!" : "믹스에 실패했습니다. 재료가 파괴됩니다.";
                    resp.getWriter().write(new ApiResponse<>(200, message, resultData).toJson());

                } catch (Exception ex) {
                    conn.rollback(); // 중간에 에러 나면 재료 삭제 등 모두 롤백
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write(ApiResponse.error(500, "서버 내부 오류가 발생했습니다.").toJson());
        }
    }
}