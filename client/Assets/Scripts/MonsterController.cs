using System.Collections;
using UnityEngine;

/// <summary>
/// 몬스터의 기본 AI (무작위 배회) 및 스탯을 관리하는 컨트롤러
/// </summary>
[RequireComponent(typeof(Rigidbody2D))]
public class MonsterController : MonoBehaviour
{
    [Header("이동 설정")]
    public float moveSpeed = 1.5f;     // 이동 속도
    public float minMoveTime = 1f;     // 최소 이동 시간
    public float maxMoveTime = 3f;     // 최대 이동 시간
    public float minIdleTime = 1.5f;   // 최소 대기 시간
    public float maxIdleTime = 4f;     // 최대 대기 시간

    private Rigidbody2D rb;
    private Vector2 moveDirection;
    private bool isMoving = false;

    private void Awake()
    {
        rb = GetComponent<Rigidbody2D>();
        
        // 탑뷰 2D 게임이므로 중력의 영향을 받지 않도록 설정합니다.
        rb.gravityScale = 0f;
        
        // 몬스터가 물리 충돌로 인해 빙글빙글 도는 것을 방지합니다.
        rb.freezeRotation = true;
    }

    private void Start()
    {
        // 몬스터가 생성되자마자 배회 AI(코루틴)를 시작합니다.
        StartCoroutine(WanderRoutine());
    }

    private IEnumerator WanderRoutine()
    {
        while (true)
        {
            // 1. 대기 (Idle) 상태
            isMoving = false;
            yield return new WaitForSeconds(Random.Range(minIdleTime, maxIdleTime));

            // 2. 이동 (Move) 상태
            isMoving = true;
            // X, Y축 각각 -1.0 ~ 1.0 사이의 무작위 방향을 정하고 정규화(대각선 속도 보정)합니다.
            moveDirection = new Vector2(Random.Range(-1f, 1f), Random.Range(-1f, 1f)).normalized;
            yield return new WaitForSeconds(Random.Range(minMoveTime, maxMoveTime));
        }
    }

    private void FixedUpdate()
    {
        // isMoving이 true일 때만 물리 엔진을 이용해 몬스터를 부드럽게 이동시킵니다.
        if (isMoving)
        {
            rb.MovePosition(rb.position + moveDirection * moveSpeed * Time.fixedDeltaTime);
        }
    }
}