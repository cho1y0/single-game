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
    
    // 유저 정보 (로그인 시 서버로부터 받아옴)
    public string Nickname { get; private set; }
    public int Level { get; private set; }

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
        public string nickname;
        public int level;
    }

    // 회원가입 요청 시 서버로 보낼 JSON 데이터 구조
    [Serializable]
    private class RegisterRequestDto
    {
        public string username;
        public string password;
    }

    // 회원가입 성공/실패 시 서버로부터 받을 JSON 응답 구조
    [Serializable]
    private class RegisterResponseDto
    {
        public bool success;
        public string message;
    }

    // 캐릭터 생성 요청 DTO
    [Serializable]
    private class CreateCharacterRequestDto
    {
        public string sessionKey; // 서버에서 유저를 식별하기 위한 세션 키
        public string nickname;
        public int charId;
    }

    // 캐릭터 생성 응답 DTO
    [Serializable]
    private class CreateCharacterResponseDto
    {
        public bool success;
        public string message;
    }

    // 유저 정보 요청 DTO
    [Serializable]
    private class UserInfoRequestDto
    {
        public string sessionKey;
    }

    // 유저 정보 응답 DTO (외부 스크립트에서 써야 하므로 public 선언)
    [Serializable]
    public class UserInfoResponseDto
    {
        public bool success;
        public int charId;
        public float posX;
        public float posY;
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
                        Nickname = response.nickname;
                        Level = response.level;

                        Debug.Log($"[AuthManager] 로그인 성공! SessionKey: {SessionKey}, 닉네임: {Nickname}, 레벨: {Level}");
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
    /// 서버에 회원가입 요청을 보냅니다.
    /// </summary>
    public void Register(string username, string password, Action<bool, string> onComplete)
    {
        RegisterRequestDto requestDto = new RegisterRequestDto
        {
            username = username,
            password = password
        };

        string jsonBody = JsonUtility.ToJson(requestDto);

        NetworkManager.Instance.PostRequest("/api/auth/register", jsonBody, 
            onSuccess: (responseJson) => 
            {
                try
                {
                    RegisterResponseDto response = JsonUtility.FromJson<RegisterResponseDto>(responseJson);
                    if (response.success)
                    {
                        Debug.Log($"[AuthManager] 회원가입 성공!");
                        onComplete?.Invoke(true, response.message);
                    }
                    else
                    {
                        Debug.LogWarning($"[AuthManager] 회원가입 실패: {response.message}");
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
    /// 서버에 캐릭터 생성 요청을 보냅니다.
    /// </summary>
    public void CreateCharacter(string nickname, int charId, Action<bool, string> onComplete)
    {
        CreateCharacterRequestDto requestDto = new CreateCharacterRequestDto
        {
            sessionKey = this.SessionKey, // 로그인 시 발급받은 세션키를 함께 보냄
            nickname = nickname,
            charId = charId
        };

        string jsonBody = JsonUtility.ToJson(requestDto);

        NetworkManager.Instance.PostRequest("/api/user/create", jsonBody, 
            onSuccess: (responseJson) => 
            {
                try
                {
                    CreateCharacterResponseDto response = JsonUtility.FromJson<CreateCharacterResponseDto>(responseJson);
                    if (response.success)
                    {
                        Nickname = nickname; // 성공 시 클라이언트의 닉네임 정보도 업데이트
                        Debug.Log($"[AuthManager] 캐릭터 생성 성공! 닉네임: {Nickname}");
                        onComplete?.Invoke(true, response.message);
                    }
                    else
                    {
                        Debug.LogWarning($"[AuthManager] 캐릭터 생성 실패: {response.message}");
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
    /// 서버에 현재 접속 중인 유저의 세부 정보(캐릭터ID, 위치 등)를 요청합니다.
    /// </summary>
    public void GetUserInfo(Action<UserInfoResponseDto> onComplete)
    {
        if (!IsLoggedIn)
        {
            Debug.LogWarning("[AuthManager] 로그인되어 있지 않아 유저 정보를 요청할 수 없습니다.");
            onComplete?.Invoke(new UserInfoResponseDto { success = false, message = "로그인 필요" });
            return;
        }

        UserInfoRequestDto requestDto = new UserInfoRequestDto { sessionKey = this.SessionKey };
        string jsonBody = JsonUtility.ToJson(requestDto);

        NetworkManager.Instance.PostRequest("/api/user/info", jsonBody, 
            onSuccess: (responseJson) => 
            {
                try
                {
                    UserInfoResponseDto response = JsonUtility.FromJson<UserInfoResponseDto>(responseJson);
                    onComplete?.Invoke(response);
                }
                catch (Exception e)
                {
                    Debug.LogError($"[AuthManager] 응답 JSON 파싱 오류: {e.Message}");
                    onComplete?.Invoke(new UserInfoResponseDto { success = false, message = "응답 데이터 처리 오류" });
                }
            }, 
            onError: (errorMsg) => 
            {
                Debug.LogError($"[AuthManager] 네트워크 오류: {errorMsg}");
                onComplete?.Invoke(new UserInfoResponseDto { success = false, message = "서버 통신 실패" });
            }
        );
    }

    /// <summary>
    /// 로그아웃 처리 (로컬 세션 삭제)
    /// </summary>
    public void Logout()
    {
        SessionKey = null;
        Nickname = null;
        Level = 0;
        Debug.Log("[AuthManager] 로그아웃 되었습니다.");
    }
}