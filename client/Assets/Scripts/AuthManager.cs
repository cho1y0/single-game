using System;
using UnityEngine;

/// <summary>
/// 로그인 및 유저 세션 관리를 담당하는 매니저 클래스
/// </summary>
public class AuthManager : MonoBehaviour
{
    public static AuthManager Instance;

    // 발급받은 세션 키 (로그인 성공 시 저장)
    public string SessionKey { get; private set; }
    public bool IsLoggedIn => !string.IsNullOrEmpty(SessionKey);

    private void Awake()
    {
        // 싱글톤 패턴 유지
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

    // 로그인 요청 시 서버로 보낼 JSON 데이터 구조
    [Serializable]
    private class LoginRequestDto
    {
        public string username;
        public string password;
    }

    // 로그인 성공/실패 시 서버로부터 받을 JSON 응답 구조
    [Serializable]
    private class LoginResponseDto
    {
        public bool success;
        public string sessionKey;
        public string message;
    }

    /// <summary>
    /// 서버에 로그인 요청을 보냅니다.
    /// </summary>
    /// <param name="username">유저 아이디</param>
    /// <param name="password">유저 비밀번호</param>
    /// <param name="onComplete">완료 후 실행될 콜백 (성공여부, 메시지)</param>
    public void Login(string username, string password, Action<bool, string> onComplete)
    {
        LoginRequestDto requestDto = new LoginRequestDto
        {
            username = username,
            password = password
        };

        string jsonBody = JsonUtility.ToJson(requestDto);

        // GDD 명세서 6. 주요 API 명세서 참고: POST /api/auth/login
        NetworkManager.Instance.PostRequest("/api/auth/login", jsonBody, 
            onSuccess: (responseJson) => 
            {
                try
                {
                    LoginResponseDto response = JsonUtility.FromJson<LoginResponseDto>(responseJson);
                    if (response.success)
                    {
                        SessionKey = response.sessionKey;
                        Debug.Log($"[AuthManager] 로그인 성공! SessionKey: {SessionKey}");
                        onComplete?.Invoke(true, response.message);
                    }
                    else
                    {
                        Debug.LogWarning($"[AuthManager] 로그인 실패: {response.message}");
                        onComplete?.Invoke(false, response.message);
                    }
                }
                catch (Exception e)
                {
                    Debug.LogError($"[AuthManager] 응답 JSON 파싱 오류: {e.Message}");
                    onComplete?.Invoke(false, "응답 데이터 처리 중 오류가 발생했습니다.");
                }
            }, 
            onError: (errorMsg) => 
            {
                Debug.LogError($"[AuthManager] 네트워크 오류: {errorMsg}");
                onComplete?.Invoke(false, "서버와의 통신에 실패했습니다.");
            }
        );
    }

    /// <summary>
    /// 로그아웃 처리 (로컬 세션 삭제)
    /// </summary>
    public void Logout()
    {
        SessionKey = null;
        Debug.Log("[AuthManager] 로그아웃 되었습니다.");
    }
}