using UnityEngine;
using TMPro; // TextMeshPro 네임스페이스
using UnityEngine.UI;
using UnityEngine.SceneManagement; // 씬 전환을 위한 네임스페이스

public class LoginUI : MonoBehaviour
{
    [Header("UI References")]
    public TMP_InputField usernameInput;
    public TMP_InputField passwordInput;
    public Button loginButton;
    public Button openRegisterButton; // 회원가입 화면으로 가는 버튼
    public GameObject registerPanel; // 켜줄 회원가입 패널
    public TextMeshProUGUI messageText;

    [Header("Scene Settings")]
    public string characterCreateSceneName = "CharacterCreate";
    public string nextSceneName = "Mekrita"; // 에디터에서 실제 씬 이름으로 변경 가능

    private void Start()
    {
        // 로그인 버튼 클릭 이벤트 연결
        loginButton.onClick.AddListener(OnLoginButtonClicked);
        
        if (openRegisterButton != null)
            openRegisterButton.onClick.AddListener(OnOpenRegisterButtonClicked);
            
        messageText.text = ""; // 시작 시 메시지 초기화
    }

    private void OnLoginButtonClicked()
    {
        string username = usernameInput.text;
        string password = passwordInput.text;

        // 1. 입력값 빈 칸 검사
        if (string.IsNullOrEmpty(username) || string.IsNullOrEmpty(password))
        {
            messageText.text = "아이디와 비밀번호를 모두 입력해주세요.";
            messageText.color = Color.red;
            return;
        }

        // 2. 로그인 요청 중 상태 표시
        messageText.text = "서버와 통신 중...";
        messageText.color = Color.yellow;
        loginButton.interactable = false; // 중복 클릭 방지

        // 3. AuthManager를 통한 실제 로그인 API 호출
        AuthManager.Instance.Login(username, password, (success, message) =>
        {
            loginButton.interactable = true; // 응답 후 버튼 다시 활성화
            messageText.text = message;
            messageText.color = success ? Color.green : Color.red;

            if (success)
            {
                // 닉네임 유무에 따라 씬 이동 분기 처리
                if (string.IsNullOrEmpty(AuthManager.Instance.Nickname))
                {
                    Debug.Log("[LoginUI] 로그인 성공! 닉네임이 없어 캐릭터 생성 화면으로 이동합니다.");
                    SceneManager.LoadScene(characterCreateSceneName); // 인스펙터에서 지정한 씬으로 이동
                }
                else
                {
                    Debug.Log("[LoginUI] 로그인 성공! 마지리타 마을로 이동합니다.");
                    SceneManager.LoadScene(nextSceneName); // 인스펙터에서 지정한 씬으로 이동
                }
            }
        });
    }

    // 회원가입 열기 버튼을 눌렀을 때
    private void OnOpenRegisterButtonClicked()
    {
        if (registerPanel != null) registerPanel.SetActive(true); // 회원가입 창 켜기
        this.gameObject.SetActive(false); // 현재(로그인) 창 끄기
    }
}