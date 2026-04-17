-- ==============================================================================
-- MixMaster Single Clone - Database DML Script (Dummy Data)
-- ==============================================================================
USE mixmaster_db;

-- ------------------------------------------------------------------------------
-- [1. 정적 데이터 초기화 (Static Data)]
-- ------------------------------------------------------------------------------

-- 1. 시스템 설정
INSERT INTO tb_system_config (config_key, config_value, description) VALUES 
('mastery_bonus_percent', '2.5', '하위 단계 마스터 시 상위 믹스 성공 확률에 더해지는 보너스 수치(%)'),
('max_hench_level_offset', '25', '기본 레벨 대비 최대 성장 가능 레벨 (기본레벨 + 25)');

-- 2. 캐릭터 사전 (원작 고증 4인방)
INSERT INTO tb_character_dict (char_id, char_name, base_hp, base_mp, growth_stat_info) VALUES 
(1, '디트', 150, 50, '{"hp": 20, "mp": 5}'),
(2, '진', 120, 80, '{"hp": 15, "mp": 10}'),
(3, '펜릴', 100, 100, '{"hp": 10, "mp": 15}'),
(4, '포이', 110, 90, '{"hp": 12, "mp": 12}');

-- 3. 몬스터/헨치 사전 (1티어 및 믹스 결과용 2티어)
INSERT INTO tb_monster_dict (monster_id, name, race, tier, base_level, is_mutant, base_hp, base_atk) VALUES 
(1, '모냥', '짐승', 1, 1, 0, 50, 10),
(2, '뿔대장', '악마', 1, 2, 0, 60, 12),
(3, '민트호퍼', '곤충', 1, 3, 0, 45, 15),
(4, '이구아산', '드래곤', 2, 15, 0, 200, 40),
(5, '돌연변이 모냥', '짐승', 1, 5, 1, 80, 20); -- 돌연변이 개체

-- 4. 몬스터 드롭 정보 (아이템 ID 1001=코어, 2001=포션)
INSERT INTO tb_monster_drop (monster_id, item_id, drop_rate) VALUES 
(1, 1001, 5.00),  -- 모냥 코어 5% 드롭
(1, 2001, 30.00), -- 초보자 회복물약 30% 드롭
(2, 1002, 3.00);  -- 뿔대장 코어 3% 드롭

-- 5. 스킬 사전
INSERT INTO tb_skill_dict (skill_id, name, skill_type, damage_multiplier, effect_description) VALUES 
(1, '파워 스트라이크', '액티브', 1.5, '무기로 강하게 내리찍어 1.5배의 데미지를 줍니다.'),
(2, '두꺼운 피부', '패시브', 0.0, '받는 물리 피해가 영구적으로 10% 감소합니다.');

-- 6. 믹스 공식 (모냥 + 뿔대장 = 이구아산)
INSERT INTO tb_mix_formula (formula_id, material_1_id, material_2_id, result_monster_id, tier, base_rate) VALUES 
(1, 1, 2, 4, 2, 75.00);

-- 7. 아이템 생산 레시피 (생산의서 + 철광석 + 나뭇가지 = 초보자의 검)
INSERT INTO tb_production_recipe (recipe_id, recipe_item_id, main_material_id, sub_material_id, result_item_id) VALUES 
(1, 3001, 5001, 5002, 6001);

-- 8. 맵 정보
INSERT INTO tb_map_info (map_id, name, map_type, required_level) VALUES 
(1, '마지리타', '안전지대', 1),
(2, '초보자의 숲', '일반필드', 1),
(3, '메크리타', '안전지대', 10);

-- 9. NPC 정보
INSERT INTO tb_npc_info (npc_id, map_id, name, role, pos_x, pos_y) VALUES 
(1, 1, '믹스빌더 파찌', '믹스', 15.5, 20.0),
(2, 1, '상인 죠앤', '상점', 10.0, -5.5);

-- ------------------------------------------------------------------------------
-- [2. 동적 데이터 초기화 (User Dummy Data)]
-- ------------------------------------------------------------------------------

-- 10. 유저 인증
INSERT INTO tb_user_auth (user_id, session_key, last_login_time) VALUES 
('test_user_01', 'dummy_session_abc123', NOW());

-- 11. 유저 기본 정보 (디트 선택, 마지리타 위치)
INSERT INTO tb_user (user_id, nickname, char_id, level, gold, current_map_id, pos_x, pos_y) VALUES 
('test_user_01', '지존디트', 1, 5, 10000, 1, 0.0, 0.0);

-- 12. 유저 보유 헨치 (동행 슬롯 1번, 2번에 각각 모냥과 뿔대장 장착)
INSERT INTO tb_user_hench (user_id, monster_id, level, exp, prefix, equip_slot) VALUES 
('test_user_01', 1, 12, 450, '힘쎈', 1),
('test_user_01', 2, 8, 120, '날쎈', 2),
('test_user_01', 1, 1, 0, '운좋은', 0); -- 보관함에 있는 남는 모냥

-- 13. 유저 인벤토리 (물약 10개, 모냥 코어 1개 보유)
INSERT INTO tb_user_inventory (user_id, item_id, quantity) VALUES 
('test_user_01', 2001, 10),
('test_user_01', 1001, 1);

-- 14. 믹스 마스터리 기록 (1단계 마스터리 완료 상태)
INSERT INTO tb_mix_mastery (user_id, tier, mastery_point, is_master) VALUES 
('test_user_01', 1, 150, 1);