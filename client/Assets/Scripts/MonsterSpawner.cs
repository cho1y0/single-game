using UnityEngine;
using System;
using System.Collections.Generic;

/// <summary>
/// 서버에서 몬스터 정보를 받아와 필드에 스폰하는 클래스
/// </summary>
public class MonsterSpawner : MonoBehaviour
{
    // 서버의 JSON 데이터와 1:1 매핑되는 클래스들
    [Serializable]
    public class MonsterDto
    {
        public int monsterId;
        public string name;
        public string race;
        public int tier;
        public int baseLevel;
    }

    [Serializable]
    public class MonsterListResponseDto
    {
        public bool success;
        public List<MonsterDto> monsters;
        public string message;
    }

    [Header("몬스터 스폰 설정")]
    [Tooltip("스폰할 몬스터의 기본 프리팹 (예: 뽈구, 슬라임 등)")]
    public GameObject defaultMonsterPrefab;
    
    [Tooltip("스폰될 구역의 최소/최대 좌표")]
    public Vector2 spawnAreaMin = new Vector2(-15f, -15f);
    public Vector2 spawnAreaMax = new Vector2(15f, 15f);

    private void Start()
    {
        // 씬이 시작되면 서버에 몬스터 정보를 요청합니다.
        FetchMonsterData();
    }

    private void FetchMonsterData()
    {
        Debug.Log("[MonsterSpawner] 서버에 몬스터 도감 정보를 요청합니다...");
        
        NetworkManager.Instance.GetRequest("/api/monster/dict", 
            onSuccess: (responseJson) => 
            {
                try
                {
                    MonsterListResponseDto response = JsonUtility.FromJson<MonsterListResponseDto>(responseJson);
                    if (response.success && response.monsters != null)
                    {
                        Debug.Log($"[MonsterSpawner] 몬스터 {response.monsters.Count}종 로드 완료. 스폰을 시작합니다.");
                        SpawnMonsters(response.monsters);
                    }
                    else
                    {
                        Debug.LogError($"[MonsterSpawner] 몬스터 로드 실패: {response.message}");
                    }
                }
                catch (Exception e)
                {
                    Debug.LogError($"[MonsterSpawner] JSON 파싱 오류: {e.Message}");
                }
            },
            onError: (errorMsg) => 
            {
                Debug.LogError($"[MonsterSpawner] 네트워크 오류: {errorMsg}");
            }
        );
    }

    private void SpawnMonsters(List<MonsterDto> monsters)
    {
        foreach (var monster in monsters)
        {
            // 맵 내에 무작위 위치 지정
            float randomX = UnityEngine.Random.Range(spawnAreaMin.x, spawnAreaMax.x);
            float randomY = UnityEngine.Random.Range(spawnAreaMin.y, spawnAreaMax.y);
            Vector3 spawnPos = new Vector3(randomX, randomY, 0);

            // 프리팹 생성
            if (defaultMonsterPrefab != null)
            {
                GameObject spawnedObj = Instantiate(defaultMonsterPrefab, spawnPos, Quaternion.identity);
                
                // 생성된 오브젝트의 이름을 보기 쉽게 변경 (예: [1티어] 뽈구 (Lv.1))
                spawnedObj.name = $"[{monster.tier}티어] {monster.name} (Lv.{monster.baseLevel})";
                
                // 방금 생성한 몬스터 오브젝트에 붙어있는 MonsterController를 가져옵니다.
                MonsterController controller = spawnedObj.GetComponent<MonsterController>();
                // 추후 이곳에서 서버에서 받아온 체력, 공격력 등을 controller로 넘겨줄 수 있습니다.
            }
        }
    }
}