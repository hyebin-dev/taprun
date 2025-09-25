package taprun.core;

public class Settings {

    // VsAiMode
    public static final double VSAI_TRACK_LENGTH_M = 50.0; // 결승선까지 총 거리(m)
    public static final double VSAI_STEP_PER_TAP_M = 1.00; // 탭 1회당 이동 거리(m)
    public static final double VSAI_QUIET_GAP_SEC = 0.06; // 연속 입력 간 최소 간격(초) - 홀드 방지
    public static final int VSAI_TRACK_COLS = 50; // 트랙 칸 수

    public static final boolean VSAI_USE_EMOJI = true; // 콘솔이 이모지 미지원이면 false로 전환
    public static final String VSAI_CELL_FILL = "—"; // 트랙 바닥
    
    public static final String VSAI_PLAYER_MARK_EMOJI = "🏃‍♀️"; // 플레이어 위치(이모지)
    public static final String VSAI_AI_MARK_EMOJI = "🏃"; // AI 위치(이모지)
    public static final String VSAI_PLAYER_MARK_ASCII = "P"; // 플레이어 위치(ASCII)
    public static final String VSAI_AI_MARK_ASCII = "A"; // AI 위치(ASCII)

    // VsAiMode - 난이도별 AI 속도(TPS: 초당 탭 수)
    public static final double VSAI_AI_TPS_EASY = 3.0;
    public static final double VSAI_AI_TPS_NORMAL = 5.0;
    public static final double VSAI_AI_TPS_HARD = 9.0;

    // Monster Mode
    public static final double MONSTER_TIME_LIMIT_SEC = 7.0; // 제한 시간(초)
    public static final int MONSTER_DAMAGE_PER_HIT = 10; // 엔터 1회당 데미지
}
