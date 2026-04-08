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
    public TextMeshProUGUI messageText;

    private void Start()
    {
        // 로그인 버튼 클릭 이벤트 연결
        loginButton.onClick.AddListener(OnLoginButtonClicked);
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
                Debug.Log("[LoginUI] 로그인 성공! 마지리타 마을로 이동합니다.");
                SceneManager.LoadScene("Majirita"); // "Majirita" 씬으로 전환
            }
        });
    }
}