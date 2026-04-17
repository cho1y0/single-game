using UnityEngine;
using TMPro; // TextMeshPro 네임스페이스
using UnityEngine.UI;

/// <summary>
/// 유니티 클라이언트에서 회원가입 UI를 제어하는 클래스
/// </summary>
public class RegisterUI : MonoBehaviour
{
    [Header("UI References")]
    public TMP_InputField usernameInput;
    public TMP_InputField passwordInput;
    public TMP_InputField passwordConfirmInput; // 비밀번호 재입력 확인용
    public Button registerButton;
    public Button closeButton; // 창 닫기 또는 뒤로가기 버튼
    public GameObject loginPanel; // 돌아갈 로그인 패널
    public TextMeshProUGUI messageText;

    private void Start()
    {
        // 버튼 클릭 이벤트 연결
        registerButton.onClick.AddListener(OnRegisterButtonClicked);
        
        if (closeButton != null)
        {
            closeButton.onClick.AddListener(OnCloseButtonClicked);
        }
        
        messageText.text = ""; // 시작 시 메시지 초기화
    }

    private void OnRegisterButtonClicked()
    {
        string username = usernameInput.text;
        string password = passwordInput.text;
        string passwordConfirm = passwordConfirmInput.text;

        // 1. 입력값 빈 칸 검사
        if (string.IsNullOrEmpty(username) || string.IsNullOrEmpty(password) || string.IsNullOrEmpty(passwordConfirm))
        {
            ShowMessage("모든 정보를 입력해주세요.", Color.red);
            return;
        }

        // 1-1. 길이 검사 (DB 용량 초과 방지 및 보안)
        if (username.Length < 4 || username.Length > 16)
        {
            ShowMessage("아이디는 4자 이상, 16자 이하로 입력해주세요.", Color.red);
            return;
        }
        
        if (password.Length < 4)
        {
            ShowMessage("비밀번호는 최소 4자 이상 입력해주세요.", Color.red);
            return;
        }

        // 2. 비밀번호 일치 검사
        if (password != passwordConfirm)
        {
            ShowMessage("비밀번호가 일치하지 않습니다.", Color.red);
            return;
        }

        // 3. 회원가입 요청 중 상태 표시
        ShowMessage("서버와 통신 중...", Color.yellow);
        registerButton.interactable = false; // 중복 클릭 방지

        // 4. AuthManager를 통한 회원가입 API 호출
        AuthManager.Instance.Register(username, password, (success, message) =>
        {
            registerButton.interactable = true; // 응답 후 버튼 다시 활성화
            ShowMessage(message, success ? Color.green : Color.red);

            if (success)
            {
                Debug.Log("[RegisterUI] 회원가입 완료. 로그인 화면으로 전환 등의 처리가 필요합니다.");
                // 가입 성공 시 로그인 창으로 자동 복귀
                if (loginPanel != null) loginPanel.SetActive(true);
                this.gameObject.SetActive(false);
            }
        });
    }

    private void OnCloseButtonClicked()
    {
        // 닫기/뒤로가기 누르면 다시 로그인 창 켜고, 내 창은 끄기
        if (loginPanel != null) loginPanel.SetActive(true);
        this.gameObject.SetActive(false);
    }

    private void ShowMessage(string msg, Color color)
    {
        messageText.text = msg;
        messageText.color = color;
    }
}