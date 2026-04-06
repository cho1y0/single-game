## 1. 프로젝트 폴더 구조 (File Structure)
프론트엔드, 백엔드, 데이터베이스 스크립트를 철저히 분리하여 모듈화된 관리를 지향합니다.

~~~text
MixMaster_Clone/
├── .git/
├── .gitignore                 # 최상위 Git 설정 (Unity, Java 빌드 파일 등 제외)
├── README.md                  # 프로젝트 개요 및 실행/빌드 방법
│
├── client/                    # 프론트엔드 (Unity 2D)
│   ├── Assets/                # 게임 리소스 및 코드
│   │   ├── Scripts/           # C# 스크립트 (Player, API통신, UI 처리 등)
│   │   ├── Sprites/           # 2D 이미지 리소스 (캐릭터, 맵 타일)
│   │   └── Scenes/            # 유니티 씬 파일 (Town_Mekrita, Field_1 등)
│   └── ProjectSettings/       # 유니티 환경 설정 파일
│
├── server/                    # 백엔드 (Java, JSP, Tomcat)
│   ├── src/                   # 비즈니스 로직 (Mix 확률 계산, 전투 결과 검증 등)
│   ├── web/                   # API 엔드포인트 설정 및 웹 리소스
│   └── lib/                   # MySQL Connector, JSON 파서 등 외부 라이브러리
│
└── database/                  # 데이터베이스 형상 관리
    ├── 01_ddl_tables.sql      # 핵심 8개 테이블 CREATE 스크립트
    └── 02_dml_dummy.sql       # 초기 몬스터 도감, 믹스 레시피 등 INSERT 스크립트
~~~