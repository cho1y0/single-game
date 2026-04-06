-- ==============================================================================
-- MixMaster Single Clone - Database DDL Script (14 Tables)
-- ==============================================================================
CREATE DATABASE IF NOT EXISTS mixmaster_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mixmaster_db;

-- ------------------------------------------------------------------------------
-- [1. 정적 데이터 (Static Data)]
-- ------------------------------------------------------------------------------

-- 1. 시스템 설정 테이블 (마스터리 보너스 비율 등)
CREATE TABLE IF NOT EXISTS tb_system_config (
    config_key VARCHAR(50) PRIMARY KEY COMMENT '설정 키 (예: mastery_bonus_percent)',
    config_value VARCHAR(255) NOT NULL COMMENT '설정 값',
    description VARCHAR(255) COMMENT '설정 설명'
) ENGINE=InnoDB COMMENT='시스템 글로벌 설정 정보';

-- 2. 캐릭터 사전 (디트, 진, 펜릴, 포이)
CREATE TABLE IF NOT EXISTS tb_character_dict (
    char_id INT AUTO_INCREMENT PRIMARY KEY,
    char_name VARCHAR(50) NOT NULL COMMENT '캐릭터 이름',
    base_hp INT NOT NULL COMMENT '기본 체력',
    base_mp INT NOT NULL COMMENT '기본 마나',
    growth_stat_info VARCHAR(255) COMMENT '레벨업 당 스탯 성장치 (JSON 등)'
) ENGINE=InnoDB COMMENT='4인 캐릭터 기본 정보';

-- 3. 몬스터/헨치 사전
CREATE TABLE IF NOT EXISTS tb_monster_dict (
    monster_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '몬스터 이름',
    race VARCHAR(20) NOT NULL COMMENT '종족 (드래곤, 악마, 짐승 등)',
    tier INT NOT NULL COMMENT '믹스 단계 (1~10)',
    base_level INT NOT NULL COMMENT '기본 레벨',
    is_mutant TINYINT(1) DEFAULT 0 COMMENT '돌연변이 여부 (0:일반, 1:돌연변이)',
    base_hp INT NOT NULL,
    base_atk INT NOT NULL
) ENGINE=InnoDB COMMENT='몬스터 및 헨치 도감 정보';

-- 4. 몬스터 드롭 정보
CREATE TABLE IF NOT EXISTS tb_monster_drop (
    drop_id INT AUTO_INCREMENT PRIMARY KEY,
    monster_id INT NOT NULL COMMENT '몬스터 ID',
    item_id INT NOT NULL COMMENT '드롭될 아이템/코어 ID',
    drop_rate DECIMAL(5,2) NOT NULL COMMENT '드롭 확률 (0.00 ~ 100.00)',
    FOREIGN KEY (monster_id) REFERENCES tb_monster_dict(monster_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='몬스터별 드롭 아이템 및 확률';

-- 5. 스킬 사전
CREATE TABLE IF NOT EXISTS tb_skill_dict (
    skill_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    skill_type VARCHAR(20) NOT NULL COMMENT '액티브 / 패시브',
    damage_multiplier DECIMAL(5,2) COMMENT '데미지 계수',
    effect_description VARCHAR(255) COMMENT '스킬 효과 설명'
) ENGINE=InnoDB COMMENT='캐릭터 및 헨치 스킬 정보';

-- 6. 믹스 공식 (레시피)
CREATE TABLE IF NOT EXISTS tb_mix_formula (
    formula_id INT AUTO_INCREMENT PRIMARY KEY,
    material_1_id INT NOT NULL COMMENT '주재료 몬스터 ID',
    material_2_id INT NOT NULL COMMENT '보조재료 몬스터 ID',
    result_monster_id INT NOT NULL COMMENT '결과 몬스터 ID',
    tier INT NOT NULL COMMENT '믹스 단계 (1~10)',
    base_rate DECIMAL(5,2) NOT NULL COMMENT '기본 성공 확률 (0.00 ~ 100.00)'
) ENGINE=InnoDB COMMENT='헨치 믹스 레시피 및 확률';

-- 7. 아이템 생산 레시피
CREATE TABLE IF NOT EXISTS tb_production_recipe (
    recipe_id INT AUTO_INCREMENT PRIMARY KEY,
    recipe_item_id INT NOT NULL COMMENT '필요한 생산의 서 아이템 ID',
    main_material_id INT NOT NULL COMMENT '주재료 아이템 ID',
    sub_material_id INT NOT NULL COMMENT '보조재료 아이템 ID',
    result_item_id INT NOT NULL COMMENT '결과물 장비/아이템 ID'
) ENGINE=InnoDB COMMENT='NPC 아이템 생산 레시피';

-- 8. 맵 정보
CREATE TABLE IF NOT EXISTS tb_map_info (
    map_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '맵 이름 (예: 마지리타)',
    map_type VARCHAR(20) NOT NULL COMMENT '안전지대 / 일반필드 / 던전 / 자유전투',
    required_level INT DEFAULT 1 COMMENT '입장 제한 레벨'
) ENGINE=InnoDB COMMENT='월드 맵 기본 정보';

-- 9. NPC 정보
CREATE TABLE IF NOT EXISTS tb_npc_info (
    npc_id INT AUTO_INCREMENT PRIMARY KEY,
    map_id INT NOT NULL COMMENT '위치한 맵 ID',
    name VARCHAR(50) NOT NULL,
    role VARCHAR(30) NOT NULL COMMENT '역할 (믹스빌더, 상인 등)',
    pos_x FLOAT NOT NULL,
    pos_y FLOAT NOT NULL,
    FOREIGN KEY (map_id) REFERENCES tb_map_info(map_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='맵별 NPC 배치 정보';

-- ------------------------------------------------------------------------------
-- [2. 동적 데이터 (Dynamic/User Data)]
-- ------------------------------------------------------------------------------

-- 10. 유저 인증 정보 (로그인 및 세션)
CREATE TABLE IF NOT EXISTS tb_user_auth (
    user_id VARCHAR(50) PRIMARY KEY COMMENT '유저 고유 ID (로컬 로그인용)',
    session_key VARCHAR(100) COMMENT '현재 발급된 세션 키',
    last_login_time DATETIME COMMENT '마지막 접속 시간'
) ENGINE=InnoDB COMMENT='유저 접속 및 인증 관리';

-- 11. 유저 기본 정보
CREATE TABLE IF NOT EXISTS tb_user (
    user_id VARCHAR(50) PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL UNIQUE,
    char_id INT NOT NULL COMMENT '선택한 캐릭터 ID (디트, 진 등)',
    level INT DEFAULT 1,
    gold INT DEFAULT 0,
    current_map_id INT DEFAULT 1,
    pos_x FLOAT DEFAULT 0.0,
    pos_y FLOAT DEFAULT 0.0,
    FOREIGN KEY (user_id) REFERENCES tb_user_auth(user_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='유저 기본 스탯 및 위치 정보';

-- 12. 유저 보유 헨치 (인벤토리/동행 헨치)
CREATE TABLE IF NOT EXISTS tb_user_hench (
    hench_uid BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '헨치 고유 UID',
    user_id VARCHAR(50) NOT NULL,
    monster_id INT NOT NULL COMMENT '어떤 몬스터인지',
    level INT NOT NULL COMMENT '현재 레벨 (기본레벨 + 25 한계)',
    exp INT DEFAULT 0 COMMENT '현재 경험치',
    prefix VARCHAR(20) COMMENT '성향 (힘쎈, 날쎈 등)',
    equip_slot TINYINT DEFAULT 0 COMMENT '장착 슬롯 (0:보관함, 1~3:동행슬롯)',
    equipped_item_id INT COMMENT '장착중인 헨치 아이템 ID',
    FOREIGN KEY (user_id) REFERENCES tb_user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (monster_id) REFERENCES tb_monster_dict(monster_id)
) ENGINE=InnoDB COMMENT='유저가 소유한 개별 헨치 정보';

-- 13. 유저 인벤토리 (소비/재료 아이템)
CREATE TABLE IF NOT EXISTS tb_user_inventory (
    item_uid BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    item_id INT NOT NULL COMMENT '아이템 고유 ID',
    quantity INT DEFAULT 1 COMMENT '보유 수량',
    FOREIGN KEY (user_id) REFERENCES tb_user(user_id) ON DELETE CASCADE,
    UNIQUE KEY (user_id, item_id) -- 같은 유저의 동일 아이템은 한 줄로 합치기 위함
) ENGINE=InnoDB COMMENT='유저 아이템 보관함';

-- 14. 믹스 마스터리 기록
CREATE TABLE IF NOT EXISTS tb_mix_mastery (
    mastery_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    tier INT NOT NULL COMMENT '믹스 단계 (1~10)',
    mastery_point INT DEFAULT 0 COMMENT '누적 마스터리 포인트',
    is_master TINYINT(1) DEFAULT 0 COMMENT '마스터 달성 여부 (0:미달성, 1:달성)',
    FOREIGN KEY (user_id) REFERENCES tb_user(user_id) ON DELETE CASCADE,
    UNIQUE KEY (user_id, tier) -- 한 유저는 티어당 하나의 레코드만 가짐
) ENGINE=InnoDB COMMENT='유저별 단계별 믹스 마스터리 정보';