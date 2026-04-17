using UnityEngine;

/// <summary>
/// 서버 통신 관련 설정을 에디터에서 관리하기 위한 ScriptableObject
/// </summary>
[CreateAssetMenu(fileName = "ServerConfig", menuName = "MixMaster/ServerConfig")]
public class ServerConfig : ScriptableObject
{
    [Header("서버 엔드포인트 설정")]
    public string baseUrl = "http://localhost:8080";

    [Header("네트워크 통신 설정")]
    public int timeoutSeconds = 10; // 타임아웃 (초 단위)
}