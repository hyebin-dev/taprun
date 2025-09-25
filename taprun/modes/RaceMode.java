package taprun.modes;

import taprun.core.Game;
import taprun.core.Settings;
import taprun.core.Utils;
import taprun.core.RankingManager;
import java.util.Scanner;

/* 달리기 경주 모드.
 * 두 플레이어가 주사위로 선/후공을 정한 뒤,
 * 각자 20회 Enter 입력 시간을 겨루어 더 빠른 기록이 승리한다.
 * 승자(또는 무승부 시 양쪽)의 기록은 랭킹에 저장된다.
 */
public class RaceMode extends Game {

    private String secondPlayer;

    public RaceMode(Settings settings) {
        super(settings);
    }

    // 시작점
    @Override
    public void start() {
        start(new Scanner(System.in));
    }

    // 외부에서 주입한 Scanner로 게임 시작
    public void start(Scanner scanner) {
        System.out.println("=== 달리기 경주 모드 ===");

        // 선/후공 결정: 주사위 굴리기
        System.out.println("선후공을 정합니다. 각자 주사위를 굴려주세요!");
        System.out.print("플레이어 1, Enter를 눌러 주사위를 굴리세요: ");
        scanner.nextLine(); // 시작 신호(엔터) 대기
        int dice1 = (int) (Math.random() * 6) + 1;
        System.out.println("플레이어 1의 주사위: " + dice1);
        System.out.println(Utils.getDiceArt(dice1)); // 주사위 ASCII 아트

        System.out.print("플레이어 2, Enter를 눌러 주사위를 굴리세요: ");
        scanner.nextLine();
        int dice2 = (int) (Math.random() * 6) + 1;
        System.out.println("플레이어 2의 주사위: " + dice2);
        System.out.println(Utils.getDiceArt(dice2));

        // 동점이면 선/후공이 결정될 때까지 재시도
        while (dice1 == dice2) {
            System.out.println("동점! 다시 굴립니다.");
            System.out.print("플레이어 1, Enter를 눌러 주사위를 굴리세요: ");
            scanner.nextLine();
            dice1 = (int) (Math.random() * 6) + 1;
            System.out.println("플레이어 1의 주사위: " + dice1);
            System.out.println(Utils.getDiceArt(dice1));

            System.out.print("플레이어 2, Enter를 눌러 주사위를 굴리세요: ");
            scanner.nextLine();
            dice2 = (int) (Math.random() * 6) + 1;
            System.out.println("플레이어 2의 주사위: " + dice2);
            System.out.println(Utils.getDiceArt(dice2));
        }

        // 선/후공 확정 및 이름 입력 순서 결정
        String firstPlayer;
        if (dice1 > dice2) {
            System.out.println("플레이어 1이 선공입니다!");
            firstPlayer = "플레이어 1";
            setSecondPlayer("플레이어 2");
        } else {
            System.out.println("플레이어 2가 선공입니다!");
            firstPlayer = "플레이어 2";
            setSecondPlayer("플레이어 1");
        }

        // firstPlayer를 기준으로 이름 입력 순서 결정
        String[] names = new String[2];
        if (firstPlayer.equals("플레이어 1")) {
            System.out.print("플레이어 1 이름을 입력하세요: ");
            names[0] = scanner.nextLine().trim();
            System.out.print("플레이어 2 이름을 입력하세요: ");
            names[1] = scanner.nextLine().trim();
        } else {
            System.out.print("플레이어 2 이름을 입력하세요: ");
            names[0] = scanner.nextLine().trim();
            System.out.print("플레이어 1 이름을 입력하세요: ");
            names[1] = scanner.nextLine().trim();
        }

        // 각자 20회 ‘빈 Enter’ 입력 소요 시간 기록
        double[] times = new double[2]; // 각 플레이어 기록(초)

        final int TRACK_LENGTH = 20; // 필요한 입력 횟수(=트랙 길이)
        final String HORSE = "🐎"; // 진행 마커(말 이모지)
        final String TRACK_SYMBOL = "-"; // 빈 트랙 표시

        for (int i = 0; i < 2; i++) {
            System.out.println();
            String requiredKey = (i == 0) ? "1" : "2"; // 선공=1, 후공=2 시작 키
            System.out.println(names[i] + "님의 차례입니다.");
            System.out.println("시작하려면 '" + requiredKey + "' 입력 후 Enter를 누르세요!");
            System.out.println("※ 레이스가 시작되면 '빈 Enter'만 유효합니다. (아무 글자 입력 없이 Enter)");

            // 시작 신호: 해당 플레이어의 시작 키가 들어올 때까지 대기 (반복 안내)
            System.out.printf("%s 플레이어가 게임을 시작하려면 '%s'를 입력한 후 Enter를 누르세요.%n",
                    (i == 0 ? "선공" : "후공"), requiredKey);

            while (true) {
                String startKey = scanner.nextLine().trim();
                if (startKey.equals(requiredKey)) break;
                System.out.printf("'%s'를 입력한 후 Enter를 눌러야 게임이 시작됩니다.%n", requiredKey);
            }

            System.out.println("준비 완료! 이제 '빈 Enter'를 20번 빠르게 누르세요! (말이 오른쪽 끝까지 달립니다)");
            int count = 0; // 현재까지 유효 입력 수
            long startNs = System.nanoTime(); // 타이머 시작(나노초)

            // 레이스 루프: '빈 Enter'만 유효, 그 외 입력은 무시
            while (count < TRACK_LENGTH) {
                String in = scanner.nextLine();
                if (in.length() != 0) { // 글자가 섞여 있으면 무시(시간은 계속 흐름)
                    continue;
                }
                count++;

                // 트랙 그리기: |🐎----...----|
                StringBuilder track = new StringBuilder();
                track.append("|");
                for (int j = 0; j < TRACK_LENGTH; j++) {
                    if (j == count - 1) track.append(HORSE);
                    else track.append(TRACK_SYMBOL);
                }
                track.append("|");
                System.out.println(track);

                // 진행 상황 간헐 출력(5회마다 혹은 완료 시)
                if (count % 5 == 0 || count == TRACK_LENGTH) {
                    System.out.println(count + "번 입력!");
                }
            }

            long endNs = System.nanoTime();
            times[i] = (endNs - startNs) / 1_000_000_000.0; // 경과 시간(초)
            System.out.println(names[i] + "님의 기록: " + String.format("%.2f", times[i]) + "초");

            // 플레이어2 안내(자동 시작 방지: '2'키 요구)
            if (i == 0) {
                System.out.println("이제 " + names[1] + "님의 차례입니다. 시작하려면 '2' 입력 후 Enter를 누르세요!");
            }
        }
        
        // 결과 집계 및 랭킹 저장
        System.out.println();
        System.out.println("=== 결과 ===");
        System.out.println(names[0] + " 기록: " + String.format("%.2f", times[0]) + "초");
        System.out.println(names[1] + " 기록: " + String.format("%.2f", times[1]) + "초");

        // 승자 결정(동률이면 무승부)
        if (times[0] < times[1]) {
            System.out.println("승자: " + names[0] + "! 축하합니다!");
        } else if (times[1] < times[0]) {
            System.out.println("승자: " + names[1] + "! 축하합니다!");
        } else {
            System.out.println("무승부입니다!");
        }

        // 두 플레이어 모두 조용히 저장(기존 기록보다 좋을 때만 갱신)
        boolean anyUpdated = false;
        if (!names[0].isEmpty()) {
            boolean updated1 = RankingManager.saveRaceModeRankingQuiet(names[0], times[0]);
            anyUpdated = anyUpdated || updated1;
        }
        if (!names[1].isEmpty()) {
            boolean updated2 = RankingManager.saveRaceModeRankingQuiet(names[1], times[1]);
            anyUpdated = anyUpdated || updated2;
        }

        // 기록 갱신 여부 안내
        if (anyUpdated) {
            System.out.println("🏆 새로운 기록이 랭킹에 등록되었습니다!");
        } else {
            System.out.println("아쉽습니다! 기존 기록이 더 높아서 등록되지 않았습니다.");
        }
        RankingManager.showRaceModeRanking(); // 최종 완성된 랭킹 출력
    }
    
    public String getSecondPlayer() {
        return secondPlayer;
    }

    public void setSecondPlayer(String secondPlayer) {
        this.secondPlayer = secondPlayer;
    }
}
