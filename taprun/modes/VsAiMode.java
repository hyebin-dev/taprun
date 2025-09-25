package taprun.modes;

import taprun.core.RankingManager;
import taprun.core.Settings;

import java.util.Scanner;

/*
 * AI 대전 모드.
 * - Enter 입력을 "탭"으로 간주. 최소 간격(quiet gap) 이상일 때만 1스텝 전진.
 * - AI는 난이도별 TPS(Taps Per Second)로 시간에 비례해 전진.
 * - 트랙은 오른쪽→왼쪽 진행. 왼쪽 "=="가 결승선.
 * - 종료: 플레이어 완주 또는 AI 이론 완주 시각(elapsedSec ≥ aiFinishTimeSec) 도달.
 * - 승자: 시간 비교(동시 골인은 플레이어 우선).
 * - 랭킹: 플레이어가 완주했고 승리한 경우에만 기록.
 */
public class VsAiMode {

    public enum Difficulty { EASY, NORMAL, HARD } // 난이도 종류(열거형)

    private final String playerName; // 플레이어 이름
    private final Difficulty difficulty; // 선태된 난이도값
    private double playerDistance; // 플레이어의 누적 이동 거리(m)
    private double aiDistance; // AI의 누적 이동 거리(m)
    private double playerFinishTimeSec = -1; // 플레이어 완주 시간(초). 미완주 시 -1을 유지
    private final double aiFinishTimeSec; // AI가 이론적으로 완주하는 데 걸리는 시간(초)
    private final double aiBaseTps; // 난이도에 따른 AI의 초당 탭 수(TPS)

    public VsAiMode(String playerName, Difficulty difficulty) {
        this.playerName = playerName;
        this.difficulty = difficulty;

        // 난이도에 따라 AI의 기본 TPS 결정 
        switch (difficulty) {
            case EASY:   aiBaseTps = Settings.VSAI_AI_TPS_EASY;   break;
            case NORMAL: aiBaseTps = Settings.VSAI_AI_TPS_NORMAL; break;
            case HARD:   aiBaseTps = Settings.VSAI_AI_TPS_HARD;   break;
            default: throw new IllegalArgumentException("존재하지 않는 난이도 : " + difficulty);
        }

        // AI의 이론 완주 시간 = 트랙 길이 / (스텝당 이동거리 × 초당 탭 수)
        aiFinishTimeSec = Settings.VSAI_TRACK_LENGTH_M /
                (Settings.VSAI_STEP_PER_TAP_M * aiBaseTps);
    }

    public void start(Scanner scanner) {
        System.out.println();
        System.out.println("=== 대전 모드 (vs AI) ===");
        System.out.println("규칙: 오른쪽에서 왼쪽으로 달립니다. 왼쪽 '==' 가 결승선!");
        System.out.println("시작하려면 Enter!");
        scanner.nextLine(); // 시작 입력(엔터) 대기

        final long startNs = System.nanoTime(); // 경기 시작 시각(나노초) 기준 — 상대적 경과 시간 계산에 사용
        // 플레이어 탭 유효성 판정용 최소 간격(quiet gap)을 나노초로 환산
        final long quietGapNs = (long) (Settings.VSAI_QUIET_GAP_SEC * 1_000_000_000L);
        long lastAcceptedNs = startNs; // 초기값: 시작 시각

        while (true) {
            if (!scanner.hasNextLine()) break; // 입력 스트림 종료 방어
            scanner.nextLine(); // 한 번의 탭(엔터) 입력
            long nowNs = System.nanoTime();
            double elapsedSec = (nowNs - startNs) / 1_000_000_000.0; // 경과 시간(초)

            // AI는 경과 시간에 비례해  이동 (트랙 길이를 넘지 않도록)
            aiDistance = Math.min(
                    Settings.VSAI_TRACK_LENGTH_M,
                    aiBaseTps * elapsedSec * Settings.VSAI_STEP_PER_TAP_M
            );
            
            // 플레이어 이동
            // quiet gap 이상 간격이 확보된 경우에만 1스텝 인정 - 홀드/오토 입력 방지
            if (nowNs - lastAcceptedNs >= quietGapNs) {
                playerDistance += Settings.VSAI_STEP_PER_TAP_M;
                lastAcceptedNs = nowNs;

                // 플레이어가 처음으로 결승선 이상에 도달한 순간의 시간을 기록
                if (playerFinishTimeSec < 0 && playerDistance >= Settings.VSAI_TRACK_LENGTH_M) {
                    playerFinishTimeSec = elapsedSec;
                }
            }
            drawFrame(elapsedSec); // 현재 상태 출력
            
            // 종료 판정: 플레이어가 완주했거나, AI 이론 완주 시각이 도래한 경우
            boolean aiFinishedNow = (elapsedSec >= aiFinishTimeSec);
            if (playerFinishTimeSec >= 0 || aiFinishedNow) {
                break;
            }
        }

        // 경기 결과 출력
        System.out.println();
        System.out.println("=== 결과 ===");

        if (playerFinishTimeSec >= 0) {
            System.out.println(playerName + " 완주: " + String.format("%.2f", playerFinishTimeSec) + "s");
        } else {
            System.out.println(playerName + " 미완주 (최종 " + String.format("%.2f", playerDistance) + "m)");
        }

        System.out.println(
                "AI 완주: " + String.format("%.2f", aiFinishTimeSec) + "s" +
                (aiDistance >= Settings.VSAI_TRACK_LENGTH_M ? "" : " (진행 중)") // 완주 못했을 경우
        );

        // 시간 기준 승자 계산 및 표시
        String winner = decideWinnerByTime();
        System.out.println("승자: " + winner);

        /*
         * 랭킹 등록 조건:
         * - 플레이어가 완주했고(playerFinishTimeSec >= 0)
         * - AI보다 같거나 빠른 시간(동시 골인은 플레이어 우선)으로 완주한 경우
         */
        boolean playerWon = (playerFinishTimeSec >= 0 && playerFinishTimeSec <= aiFinishTimeSec);
        if (playerWon) {
            RankingManager.saveVsAiModeRanking(playerName, playerFinishTimeSec, difficulty.toString());
        } else if (playerFinishTimeSec >= 0) {
            System.out.println("완주했지만 AI를 이기지 못해 랭킹에 등록되지 않았습니다.");
        } else {
            System.out.println("미완주로 인해 랭킹에 등록되지 않았습니다.");
        }
    }

    // 한 프레임의 레이스 상황(트랙/거리/시간)을 콘솔로 출력
    private void drawFrame(double elapsedSec) {
        System.out.println();
        System.out.println("----------------------------------------------");
        System.out.println("t=" + String.format("%.2f", elapsedSec) + "s  목표: "
                + (int) Settings.VSAI_TRACK_LENGTH_M + "m  (← 왼쪽 결승선)");

        // 플레이어/AI 각각의 트랙을 출력
        System.out.println(buildLaneRightToLeft(
                playerName, playerDistance,
                Settings.VSAI_USE_EMOJI ? Settings.VSAI_PLAYER_MARK_EMOJI : Settings.VSAI_PLAYER_MARK_ASCII
        ));
        System.out.println(buildLaneRightToLeft(
                "AI", aiDistance,
                Settings.VSAI_USE_EMOJI ? Settings.VSAI_AI_MARK_EMOJI : Settings.VSAI_AI_MARK_ASCII
        ));

        // 수치 요약(거리)
        System.out.println(playerName + ": " + String.format("%.2f", playerDistance) + "m");
        System.out.println("AI: " + String.format("%.2f", aiDistance) + "m");
        System.out.println("----------------------------------------------");
    }

    private String buildLaneRightToLeft(String label, double distanceM, String marker) {
        double ratio = distanceM / Settings.VSAI_TRACK_LENGTH_M;
        if (ratio < 0) ratio = 0; // 최소 도착점 - 도착지점을 넘어가지 않도록
        if (ratio > 1) ratio = 1; // 최대 출발점

        // 오른쪽에서 왼쪽으로 갈수록 인덱스가 작아지도록 위치 계산 (오른쪽→왼쪽 진행 트랙)
        // 진행 비율(ratio)에 따라 현재 위치 인덱스(pos) 계산
        int pos = (int)((1.0 - ratio) * (Settings.VSAI_TRACK_COLS - 1));

        // 트랙 생성
        String[] lane = new String[Settings.VSAI_TRACK_COLS];
        for (int i = 0; i < Settings.VSAI_TRACK_COLS; i++) lane[i] = Settings.VSAI_CELL_FILL;

        // 결승선(좌측 끝 0,1번 칸)에 "=" 표시 — 트랙 폭이 2 이상일 때만 안전하게 처리
        if (Settings.VSAI_TRACK_COLS >= 2) {
            lane[0] = "=";
            lane[1] = "=";
        }

        // 결승선과 마커(이모지)가 겹치는 경우 레인 내 마커 대신 FINISH 플래그로만 표현
        if (pos > 1) {
            lane[pos] = marker;
        }

        // 라벨 + 트랙 문자열 합치기
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-7s", label)).append(" |");
        for (int i = 0; i < Settings.VSAI_TRACK_COLS; i++) sb.append(lane[i]);
        sb.append("| ").append(distanceM >= Settings.VSAI_TRACK_LENGTH_M ? "FINISH!" : "");
        return sb.toString();
    }

    // 플레이어가 완주했다면 AI 이론 완주 시간과 비교(동시 골인은 플레이어 우선), 미완주면 AI 승
    private String decideWinnerByTime() {
        if (playerFinishTimeSec >= 0) {
            if (playerFinishTimeSec < aiFinishTimeSec) return playerName;
            if (playerFinishTimeSec > aiFinishTimeSec) return "AI";
            return playerName; // 동시 골인 - 플레이어 승
        }
        return "AI"; // 플레이어 미완주 - AI 승
    }
    public Difficulty getDifficulty() {
        return difficulty;
    }
}
