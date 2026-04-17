using UnityEngine;
using System.Collections.Generic;
using System.Linq;
using Cinemachine; // 시네머신 네임스페이스 (Cinemachine 2.x 버전용)

public class PlayerSpawner : MonoBehaviour
{
    // 캐릭터 ID와 프리팹을 짝지어 관리하는 구조체 (인스펙터에서 보기 편함)
    [System.Serializable]
    public struct CharacterPrefabMapping
    {
        public int charId;
        public GameObject prefab;
    }

    [Header("캐릭터 프리팹 목록")]
    public List<CharacterPrefabMapping> characterPrefabs;

    [Header("스폰 실패 시 사용할 기본 캐릭터")]
    public GameObject defaultPrefab; // 예: 디트 프리팹

    private void Start()
    {
        // 개발 편의성: 로그인 씬을 거치지 않고 마지리타 씬에서 바로 Play를 눌렀을 때의 에러 방지
        if (AuthManager.Instance == null)
        {
            Debug.LogWarning("[PlayerSpawner] AuthManager가 없습니다! 에디터 테스트 모드로 간주하고 기본 캐릭터를 스폰합니다.");
            SpawnCharacter(0, 0, 0); // 에디터 테스트용 기본 캐릭터 강제 스폰
            return;
        }

        // 마지리타 씬이 시작되자마자 내 캐릭터 정보를 서버에 요청합니다.
        AuthManager.Instance.GetUserInfo(OnUserInfoReceived);
    }
    
    private void OnUserInfoReceived(AuthManager.UserInfoResponseDto response)
    {
        if (response.success)
        {
            Debug.Log($"[PlayerSpawner] 정보 로드 성공! 캐릭터ID: {response.charId}, 위치: ({response.posX}, {response.posY})");
            SpawnCharacter(response.charId, response.posX, response.posY);
        }
        else
        {
            Debug.LogError($"[PlayerSpawner] 유저 정보를 불러오지 못했습니다: {response.message}");
            // 에러 시 안전하게 기본 캐릭터를 (0,0)에 스폰
            SpawnCharacter(0, 0, 0); // ID를 0으로 보내면 defaultPrefab을 사용하게 됨
        }
    }

    private void SpawnCharacter(int charId, float x, float y)
    {
        // 리스트에서 charId에 맞는 프리팹을 찾습니다.
        GameObject prefabToSpawn = characterPrefabs.FirstOrDefault(p => p.charId == charId).prefab;

        // 만약 리스트에 없거나 charId가 0이면(로드 실패 시) 기본 프리팹을 사용합니다.
        if (prefabToSpawn == null)
        {
            Debug.LogWarning($"[PlayerSpawner] ID({charId})에 해당하는 캐릭터 프리팹을 찾지 못했습니다. 기본 캐릭터를 스폰합니다.");
            prefabToSpawn = defaultPrefab;
            if (prefabToSpawn == null)
            {
                Debug.LogError("[PlayerSpawner] 기본 프리팹(defaultPrefab)조차 설정되지 않아 스폰할 수 없습니다!");
                return;
            }
        }

        Vector3 spawnPosition = new Vector3(x, y, 0);
        
        // 선택된 캐릭터 프리팹을 지정된 위치에 생성
        GameObject myPlayer = Instantiate(prefabToSpawn, spawnPosition, Quaternion.identity);
        myPlayer.name = $"{prefabToSpawn.name}(Clone)"; // Hierarchy 창에서 알아보기 쉽게 이름 설정

        // 씬에 있는 단 하나의 CinemachineVirtualCamera를 자동으로 찾아옵니다. (Unity 6 권장 방식)
        CinemachineVirtualCamera vcam = UnityEngine.Object.FindFirstObjectByType<CinemachineVirtualCamera>();
        if (vcam != null)
        {
            // Follow 속성에 생성된 플레이어를 할당합니다.
            vcam.Follow = myPlayer.transform;
            Debug.Log($"[PlayerSpawner] 시네머신 카메라가 '{myPlayer.name}' 추적을 시작합니다.");
        }
        else
        {
            Debug.LogWarning("[PlayerSpawner] 씬에서 CinemachineVirtualCamera를 찾지 못했습니다. 카메라 추적이 작동하지 않습니다.");
        }
    }
}