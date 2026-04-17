using System;
using System.Collections;
using System.Text;
using UnityEngine;
using UnityEngine.Networking;

/// <summary>
/// 서버(Java/Tomcat)와의 HTTP 통신을 전담하는 공통 네트워크 매니저
/// </summary>
public class NetworkManager : MonoBehaviour
{
    public static NetworkManager Instance;

    [Header("서버 환경 설정")]
    [Tooltip("에디터에서 생성한 ServerConfig 에셋을 연결해주세요.")]
    public ServerConfig serverConfig;

    // serverConfig가 연결되지 않았을 때를 대비한 기본값
    private string BaseUrl => (serverConfig != null) ? serverConfig.baseUrl : "http://localhost:8080";
    private int Timeout => (serverConfig != null) ? serverConfig.timeoutSeconds : 10;

    private void Awake()
    {
        // 싱글톤 패턴: 게임 내내 단 하나만 존재하도록 유지
        if (Instance == null)
        {
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }
        else
        {
            Destroy(gameObject);
        }
    }

    /// <summary>
    /// 서버로 POST 요청을 보내는 공통 함수
    /// </summary>
    public void PostRequest(string endpoint, string jsonBody, Action<string> onSuccess, Action<string> onError)
    {
        StartCoroutine(SendPostRequest(endpoint, jsonBody, onSuccess, onError));
    }

    private IEnumerator SendPostRequest(string endpoint, string jsonBody, Action<string> onSuccess, Action<string> onError)
    {
        string url = BaseUrl + endpoint;
        using (UnityWebRequest request = new UnityWebRequest(url, "POST"))
        {
            byte[] bodyRaw = Encoding.UTF8.GetBytes(jsonBody);
            request.uploadHandler = new UploadHandlerRaw(bodyRaw);
            request.downloadHandler = new DownloadHandlerBuffer();
            request.SetRequestHeader("Content-Type", "application/json");
            
            // GDD 명세: 서버 응답 지연 시 무한 로딩 방지
            request.timeout = Timeout;

            yield return request.SendWebRequest();

            if (request.result == UnityWebRequest.Result.ConnectionError || request.result == UnityWebRequest.Result.ProtocolError)
            {
                Debug.LogError($"[Network Error] {request.error}");
                onError?.Invoke(request.error);
            }
            else
            {
                Debug.Log($"[Network Success] {request.downloadHandler.text}");
                onSuccess?.Invoke(request.downloadHandler.text);
            }
        }
    }

    /// <summary>
    /// 서버로 GET 요청을 보내는 공통 함수
    /// </summary>
    public void GetRequest(string endpoint, Action<string> onSuccess, Action<string> onError)
    {
        StartCoroutine(SendGetRequest(endpoint, onSuccess, onError));
    }

    private IEnumerator SendGetRequest(string endpoint, Action<string> onSuccess, Action<string> onError)
    {
        string url = BaseUrl + endpoint;
        using (UnityWebRequest request = UnityWebRequest.Get(url))
        {
            // GDD 명세: 서버 응답 지연 시 무한 로딩 방지
            request.timeout = Timeout;

            yield return request.SendWebRequest();

            if (request.result == UnityWebRequest.Result.ConnectionError || request.result == UnityWebRequest.Result.ProtocolError)
            {
                Debug.LogError($"[Network Error] {request.error}");
                onError?.Invoke(request.error);
            }
            else
            {
                Debug.Log($"[Network Success] {request.downloadHandler.text}");
                onSuccess?.Invoke(request.downloadHandler.text);
            }
        }
    }
}