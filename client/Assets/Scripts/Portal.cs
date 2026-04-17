using UnityEngine;
using UnityEngine.SceneManagement;

/// <summary>
/// 캐릭터가 닿으면 다른 씬(마을 <-> 사냥터)으로 이동시켜주는 포탈 클래스
/// </summary>
public class Portal : MonoBehaviour
{
    [Header("포탈 설정")]
    [Tooltip("이동할 대상 씬의 정확한 이름을 입력하세요. (Majirita_Grassland)")]
    public string targetSceneName = "Majirita_Grassland"; // 기본값으로 사냥터 이름 지정

    // 2D 물리 엔진에서 Trigger 설정이 된 콜라이더에 무언가 닿았을 때 실행됨
    private void OnTriggerEnter2D(Collider2D collision)
    {
        // 닿은 오브젝트의 태그가 "Player"인지 확인 (몬스터가 닿아도 이동되지 않도록 방지)
        if (collision.CompareTag("Player"))
        {
            Debug.Log($"[Portal] 플레이어가 포탈에 닿았습니다. '{targetSceneName}' 씬으로 이동합니다.");
            
            // 지정된 씬으로 이동
            SceneManager.LoadScene(targetSceneName);
        }
    }
}