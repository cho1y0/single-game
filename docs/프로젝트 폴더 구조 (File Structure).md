프로젝트 폴더 구조 (File Structure)
프론트엔드, 백엔드, 데이터베이스 스크립트를 철저히 분리하여 모듈화된 관리를 지향합니다.

~~~text
MixMaster_Clone/
├── .git/
├── .gitignore                 # 통합 Git 설정 (Unity와 Java 빌드 파일 등 일괄 통제)
├── README.md                  # 프로젝트 개요 및 아키텍처 문서 (본 문서)
│
├── client/                    # 프론트엔드 (Unity 2D)
│   ├── Assets/                # 게임 리소스 및 코드
│   │   ├── Scripts/           # C# 스크립트 (Player, API통신, 오브젝트 풀링 등)
│   │   ├── Sprites/           # 2D 이미지 리소스 (캐릭터, 맵 타일)
│   │   └── Scenes/            # 유니티 씬 파일 (Town_Mekrita, Field_1 등)
│   └── ProjectSettings/       # 유니티 환경 설정 파일
│
├── server/                    # 백엔드 (Java, JSP, Tomcat)
│   ├── pom.xml                # Maven 의존성 관리 (JSON 파서, DB 커넥터 등)
│   ├── src/                   # 비즈니스 로직 (Mix 확률 계산, 전투 결과 검증 등)
│   └── web/                   # API 엔드포인트 설정 및 웹 리소스
│
└── database/                  # 데이터베이스 형상 관리
    ├── 01_ddl_tables.sql      # 핵심 14개 테이블 CREATE 스크립트
    └── 02_dml_dummy.sql       # 초기 몬스터 도감, 믹스 레시피 등 INSERT 스크립트
~~~