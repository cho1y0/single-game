using UnityEngine;
using TMPro;
using UnityEngine.UI;
using UnityEngine.SceneManagement;

public class CharacterCreateUI : MonoBehaviour
{
    [Header("UI References")]
    public TMP_InputField nicknameInput;
    
    [Header("Character Selection Buttons")]
    public Button btnDitt;   // 1: 디트
    public Button btnJin;    // 2: 진
    public Button btnPenril; // 3: 펜릴
    public Button btnPhoy;   // 4: 포이
    
    [Header("Action Buttons")]
    public Button createButton; // 생성 완료 버튼
    public TextMeshProUGUI messageText;

    [Header("Scene Settings")]
    public string nextSceneName = "Mekrita"; // 에디터에서 실제 씬 이름(예: Town_Mekrita)으로 맞춰주세요.

    private int selectedCharId = 0; // 0은 미선택, 1~4는 선택된 캐릭터 ID

    private void Start()
    {
        // 각 캐릭터 버튼에 클릭 이벤트 연결 (클릭 시 ID와 이름 전달)
        btnDitt.onClick.AddListener(() => OnCharacterSelected(1, "디트"));
        btnJin.onClick.AddListener(() => OnCharacterSelected(2, "진"));
        btnPenril.onClick.AddListener(() => OnCharacterSelected(3, "펜릴"));
        btnPhoy.onClick.AddListener(() => OnCharacterSelected(4, "포이"));

        // 캐릭터 생성 버튼 이벤트 연결
        createButton.onClick.AddListener(OnCreateButtonClicked);

        messageText.text = "캐릭터를 선택하고 닉네임을 입력하세요.";
    }

    private void OnCharacterSelected(int charId, string charName)
    {
        selectedCharId = charId;
        ShowMessage($"[{charName}] 캐릭터가 선택되었습니다.", Color.white);
        
        // 선택된 버튼의 색상을 바꾸어 유저가 알기 쉽게 강조
        btnDitt.image.color = (charId == 1) ? Color.yellow : Color.white;
        btnJin.image.color = (charId == 2) ? Color.yellow : Color.white;
        btnPenril.image.color = (charId == 3) ? Color.yellow : Color.white;
        btnPhoy.image.color = (charId == 4) ? Color.yellow : Color.white;
    }

    private void OnCreateButtonClicked()
    {
        string nickname = nicknameInput.text.Trim(); // Trim()으로 양옆 공백 제거

        // 1. 유효성 검사
        if (string.IsNullOrEmpty(nickname))
        {
            ShowMessage("닉네임을 입력해주세요.", Color.red);
            return;
        }

        // 1-1. 닉네임 길이 검사
        if (nickname.Length < 2 || nickname.Length > 12)
        {
            ShowMessage("닉네임은 2자 이상, 12자 이하로 입력해주세요.", Color.red);
            return;
        }

        if (selectedCharId == 0)
        {
            ShowMessage("캐릭터를 먼저 선택해주세요.", Color.red);
            return;
        }

        // 2. 서버 요청
        ShowMessage("캐릭터 생성 중...", Color.yellow);
        createButton.interactable = false;

        AuthManager.Instance.CreateCharacter(nickname, selectedCharId, (success, message) =>
        {
            createButton.interactable = true;
            ShowMessage(message, success ? Color.green : Color.red);

            if (success)
            {
                // 인스펙터에서 설정한 씬 이름으로 안전하게 이동합니다.
                SceneManager.LoadScene(nextSceneName);
            }
        });
    }

    private void ShowMessage(string msg, Color color)
    {
        messageText.text = msg;
        messageText.color = color;
    }
}