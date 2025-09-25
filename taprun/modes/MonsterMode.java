package taprun.modes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.HashSet;
import java.util.Set;

import taprun.core.RankingManager;
import taprun.core.Settings;
import taprun.core.Utils;
import taprun.core.Game;

/* 몬스터 연타 배틀 모드.
 * 두 플레이어가 주사위로 선/후공을 정한 뒤,
 * 동일한 몬스터를 상대로 제한시간 내(빈 Enter 연타) 더 빨리 처치한 사람이 라운드를 승리한다.
 * 총 3라운드를 진행하며, 라운드 승수가 많은 플레이어가 최종 우승한다(동률 시 무승부).
 * 각 라운드의 몬스터는 중복되지 않으며, 실패(시간 초과)는 큰 시간값으로 기록된다.
 * 랭킹은 3경기 모두 완주한 플레이어만 합계 시간 기준으로 등록/갱신된다.
 */

public class MonsterMode extends Game {

    public MonsterMode(Settings settings) {
        super(settings);
    }

    @Override
    public void start() {
    	start(new Scanner(System.in));
     	}

    // 외부에서 전달된 Scanner로 동일 입력 스트림(System.in)을 공유하며 시작
    public void start(Scanner scanner) {
    	
        // 선/후공 결정: 주사위 굴리기
        System.out.println("선후공을 정합니다. 각자 주사위를 굴려주세요!");
        System.out.print("플레이어 1, Enter를 눌러 주사위를 굴리세요: ");
        scanner.nextLine(); // 시작 신호(엔터) 대기
        int dice1 = (int)(Math.random() * 6) + 1;
        System.out.println("플레이어 1의 주사위: " + dice1);
        System.out.println(Utils.getDiceArt(dice1));
        System.out.print("플레이어 2, Enter를 눌러 주사위를 굴리세요: ");
        scanner.nextLine();
        int dice2 = (int)(Math.random() * 6) + 1;
        System.out.println("플레이어 2의 주사위: " + dice2);
        System.out.println(Utils.getDiceArt(dice2));

        // 동점이면 선/후공이 확정될 때까지 반복해서 굴린다.
        while (dice1 == dice2) {
            System.out.println("동점! 다시 굴립니다.");

            System.out.print("플레이어 1, Enter를 눌러 주사위를 굴리세요: ");
            scanner.nextLine();
            dice1 = (int)(Math.random() * 6) + 1;
            System.out.println("플레이어 1의 주사위: " + dice1);
            System.out.println(Utils.getDiceArt(dice1));

            System.out.print("플레이어 2, Enter를 눌러 주사위를 굴리세요: ");
            scanner.nextLine();
            dice2 = (int)(Math.random() * 6) + 1;
            System.out.println("플레이어 2의 주사위: " + dice2);
            System.out.println(Utils.getDiceArt(dice2));
        }

        // 선/후공에 따라 이름 입력 순서 결정
        String[] playerOrder = new String[2];
        if (dice1 > dice2) {
            System.out.println("플레이어 1이 선공입니다!");
            System.out.print("플레이어 1 이름을 입력하세요: ");
            playerOrder[0] = scanner.nextLine().trim();
            System.out.print("플레이어 2 이름을 입력하세요: ");
            playerOrder[1] = scanner.nextLine().trim();
        } else {
            System.out.println("플레이어 2가 선공입니다!");
            System.out.print("플레이어 2 이름을 입력하세요: ");
            playerOrder[0] = scanner.nextLine().trim();
            System.out.print("플레이어 1 이름을 입력하세요: ");
            playerOrder[1] = scanner.nextLine().trim();
        }

        GameCore game = new GameCore(scanner, playerOrder[0], playerOrder[1]);
        game.start();
    }

    // RoundResult: 각 라운드의 결과를 저장
    static class RoundResult {
        int roundNumber; // 라운드 번호(1..TOTAL_ROUNDS)
        double player1Time; // 플레이어1 기록(초) — 실패 시 999.xx로 저장
        double player2Time; // 플레이어2 기록(초)
        String winnerName; // 라운드 승자 이름(무승부 시 "무승부")

        public RoundResult(int roundNumber, double p1Time, double p2Time, String winnerName) {
            this.roundNumber = roundNumber;
            this.player1Time = p1Time;
            this.player2Time = p2Time;
            this.winnerName = winnerName;
        }
    }

    // Game: 게임의 전체 흐름(라운드 진행, 요약/랭킹 반영)
    static class GameCore {
        private final int TOTAL_ROUNDS = 3; // 총 3라운드 고정

        private BattlePlayer player1; // 선공 플레이어
        private BattlePlayer player2; // 후공 플레이어
        private List<RoundResult> results; // 라운드별 결과 리스트
        private final Scanner scanner; // 입력용 Scanner(공유)
        private String finalWinner; // 최종 우승자 이름(무승부면 빈 문자열)

        public GameCore(Scanner scanner, String firstPlayerName, String secondPlayerName) {
            this.player1 = new BattlePlayer(firstPlayerName);
            this.player2 = new BattlePlayer(secondPlayerName);
            this.results = new ArrayList<>();
            this.scanner = scanner;
        }

        public void start() {
            System.out.println("===============================");
            System.out.println(" 몬스터 연타 배틀 ");
            System.out.println("===============================");
            System.out.printf("총 %d 라운드를 진행합니다.\n", TOTAL_ROUNDS);
            System.out.println("같은 몬스터를 더 빨리 잡는 플레이어가 라운드를 승리합니다.");
            System.out.println("시작하려면 Enter를 누르세요...");
            scanner.nextLine(); // 시작 전 대기

            // 이번 경기에서 이미 등장한 몬스터 이름을 기억하여 중복 방지
            Set<String> usedMonsterNames = new HashSet<>();

            for (int i = 1; i <= TOTAL_ROUNDS; i++) {
                System.out.printf("\n--- 라운드 %d / %d ---\n", i, TOTAL_ROUNDS);

                // 선공 시작 키(1) 확인 루프 — 잘못된 입력이면 안내 후 재요청
                System.out.println("선공 플레이어가 게임을 시작하려면 '1'을 입력한 후 엔터를 누르세요.");
                while (true) {
                    String input = scanner.nextLine().trim();
                    if (input.equals("1")) break;
                    System.out.println("'1'을 입력한 후 엔터를 눌러야 게임이 시작됩니다.");
                }

                // 라운드용 몬스터를 중복되지 않게 랜덤 선택
                Monster roundMonster;
                while (true) {
                    roundMonster = Monster.createRandomMonster();
                    if (!usedMonsterNames.contains(roundMonster.getName())) {
                        usedMonsterNames.add(roundMonster.getName());
                        break;
                    }
                }
                Battle battle = new Battle(scanner);

                // 선공 플레이어 전투
                double p1_time = battle.start(player1, roundMonster);
                System.out.printf("%s의 기록: %.2f초\n", player1.getName(), p1_time);

                // 후공 시작 키(2) 확인 루프
                System.out.println("후공 플레이어가 게임을 시작하려면 '2'를 입력한 후 엔터를 누르세요.");
                while (true) {
                    String input = scanner.nextLine().trim();
                    if (input.equals("2")) break;
                    System.out.println("'2'를 입력한 후 엔터를 눌러야 게임이 시작됩니다.");
                }

                // 동일 몬스터로 공정하게 재도전(HP 리셋)
                roundMonster.resetHp();
                double p2_time = battle.start(player2, roundMonster);
                System.out.printf("%s의 기록: %.2f초\n", player2.getName(), p2_time);

                // 라운드 승자 판정 및 결과 누적
                String roundWinnerName;
                if (p1_time < p2_time) {
                    player1.addWin();
                    roundWinnerName = player1.getName();
                    System.out.printf("\n>> 라운드 %d의 승자는 %s입니다! <<\n", i, player1.getName());
                } else if (p2_time < p1_time) {
                    player2.addWin();
                    roundWinnerName = player2.getName();
                    System.out.printf("\n>> 라운드 %d의 승자는 %s입니다! <<\n", i, player2.getName());
                } else {
                    roundWinnerName = "무승부";
                    System.out.println("\n>> 이번 라운드는 무승부입니다! <<\n");
                }

                results.add(new RoundResult(i, p1_time, p2_time, roundWinnerName));

                // 현재 스코어 보드 출력
                System.out.printf("현재 스코어 - %s: %d승 | %s: %d승\n",
                        player1.getName(), player1.getWins(), player2.getName(), player2.getWins());

                // 마지막 라운드가 아니면 다음 라운드로 넘어가기 전 대기
                if (i < TOTAL_ROUNDS) {
                    System.out.println("다음 라운드를 진행하려면 Enter를 누르세요...");
                    scanner.nextLine();
                }
            }
            // 전체 경기 요약 및 최종 결과 출력/랭킹 반영
            displaySummaryAndFinalResult();
        }

        // 전체 경기 요약과 최종 우승자 계산/출력(+ 랭킹 저장)
        private void displaySummaryAndFinalResult() {
            System.out.println("\n\n===============================");
            System.out.println(" 경기 결과 요약 ");
            System.out.println("===============================");

            double player1TotalTime = 0; // 성공 라운드 합계
            double player2TotalTime = 0;
            int player1ValidRounds = 0; // 성공 라운드 수(실패=999.xx 제외)
            int player2ValidRounds = 0;

            // 라운드별 기록 출력 및 합산(실패 라운드는 합계에서 제외)
            for (RoundResult result : results) {
                String p1TimeString = result.player1Time > 999 ? "실패" : String.format("%.2f초", result.player1Time);
                String p2TimeString = result.player2Time > 999 ? "실패" : String.format("%.2f초", result.player2Time);

                System.out.printf("[Round %d] %s : %s / %s : %s => %s 승리\n",
                        result.roundNumber,
                        player1.getName(), p1TimeString,
                        player2.getName(), p2TimeString,
                        result.winnerName);

                if (result.player1Time <= 999) {
                    player1TotalTime += result.player1Time;
                    player1ValidRounds++;
                }
                if (result.player2Time <= 999) {
                    player2TotalTime += result.player2Time;
                    player2ValidRounds++;
                }
            }

            // 최종 스코어(승수) 출력 및 우승자 판정
            System.out.println("\n--- 최종 결과 ---");
            System.out.printf("%s: %d승\n", player1.getName(), player1.getWins());
            System.out.printf("%s: %d승\n", player2.getName(), player2.getWins());

            setFinalWinner(""); // 기본값은 빈 문자열(무승부 대비)
            if (player1.getWins() > player2.getWins()) {
                System.out.printf("최종 우승: %s! 축하합니다!\n", player1.getName());
                setFinalWinner(player1.getName());
            } else if (player2.getWins() > player1.getWins()) {
                System.out.printf("최종 우승: %s! 축하합니다!\n", player2.getName());
                setFinalWinner(player2.getName());
            } else {
                System.out.println("무승부입니다! 둘 다 대단하군요!");
            }

            // 랭킹 등록: 3경기 모두 완주(=성공 라운드 3/3)하고 이름이 있어야 대상
            boolean player1CanRank = (player1ValidRounds == TOTAL_ROUNDS && !player1.getName().isEmpty());
            boolean player2CanRank = (player2ValidRounds == TOTAL_ROUNDS && !player2.getName().isEmpty());

            // 합계 시간 안내(등록 가능/불가 메시지 구분)
            if (player1ValidRounds == TOTAL_ROUNDS) {
                if (!player1.getName().isEmpty()) {
                    System.out.printf("\n%s의 3경기 합계 시간: %.2f초\n", player1.getName(), player1TotalTime);
                } else { // 이름 X -> 등록 불가
                    System.out.printf("\n플레이어 1의 3경기 합계 시간: %.2f초 (이름이 입력되지 않아 랭킹에 등록되지 않습니다)\n", player1TotalTime);
                }
            } else { // 3경기 완주 X -> 등록 불가
                System.out.printf("\n%s는 %d경기만 완주하여 랭킹 대상이 아닙니다.\n", player1.getName(), player1ValidRounds);
            }

            if (player2ValidRounds == TOTAL_ROUNDS) {
                if (!player2.getName().isEmpty()) {
                    System.out.printf("%s의 3경기 합계 시간: %.2f초\n", player2.getName(), player2TotalTime);
                } else {
                    System.out.printf("플레이어 2의 3경기 합계 시간: %.2f초 (이름이 입력되지 않아 랭킹에 등록되지 않습니다)\n", player2TotalTime);
                }
            } else {
                System.out.printf("%s는 %d경기만 완주하여 랭킹 대상이 아닙니다.\n", player2.getName(), player2ValidRounds);
            }

            // 완주한 플레이어들의 기록을 조용히 저장(기존보다 좋을 때만 갱신)
            boolean anyUpdated = false;

            if (player1CanRank) {
                boolean updated1 = RankingManager.saveMonsterModeRankingQuiet(player1.getName(), player1TotalTime);
                anyUpdated = anyUpdated || updated1;
            }
            if (player2CanRank) {
                boolean updated2 = RankingManager.saveMonsterModeRankingQuiet(player2.getName(), player2TotalTime);
                anyUpdated = anyUpdated || updated2;
            }

            // 하나라도 갱신되었는지에 따라 안내 메시지 출력 + 최종 랭킹 보기
            if (player1CanRank || player2CanRank) {
                if (anyUpdated) {
                    System.out.println("🏆 새로운 기록이 랭킹에 등록되었습니다!");
                } else {
                    System.out.println("아쉽습니다! 기존 기록이 더 높아서 등록되지 않았습니다.");
                }
                RankingManager.showMonsterModeRanking();
            }
        }

        // 최종 우승자 이름 조회자
        public String getFinalWinner() {
            return finalWinner;
        }

        // 최종 우승자 이름 설정자
        public void setFinalWinner(String finalWinner) {
            this.finalWinner = finalWinner;
        }
    }

    // 한 라운드의 전투
    static class Battle {
        private final double TIME_LIMIT = Settings.MONSTER_TIME_LIMIT_SEC; // 제한 시간(초)
        private final int DAMAGE_PER_HIT = Settings.MONSTER_DAMAGE_PER_HIT; // 엔터당 데미지
        private final Scanner scanner; // 입력용 Scanner

        public Battle(Scanner scanner) {
            this.scanner = scanner;
        }

        // 플레이어가 제한 시간 내에 연타로 몬스터를 처치하는데 걸린 시간 반환(실패 시 999.99)
        public double start(BattlePlayer player, Monster monster) {
            System.out.printf("\n야생의 %s (HP: %d)이(가) 나타났다!\n", monster.getName(), monster.getMaxHp());
            System.out.println(monster.getArt());
            System.out.printf("\n%s의 차례! %.1f초 안에 엔터를 연타하여 몬스터를 처치하세요!\n", player.getName(), TIME_LIMIT);
            System.out.println("준비... 시작하려면 Enter를 누르세요!");
            scanner.nextLine(); // 시작 신호

            long startTime = System.nanoTime(); // 시작 시각(나노초)

            // 몬스터가 살아 있는 동안 반복
            while (monster.isAlive()) {
                double elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000.0;

                // 시간 초과 시 실패 처리(매우 큰 시간으로 반환)
                if (elapsedTime >= TIME_LIMIT) {
                    System.out.println("\n시간 초과! 몬스터를 처치하는 데 실패했습니다...");
                    return 999.99;
                }

                // 현재 전투 상태(HP 바/남은 시간) 표시 후 입력 대기
                displayBattleStatus(monster, elapsedTime);
                String line = scanner.nextLine();
                if (line.length() == 0) {  // 빈 엔터만 유효 타격으로 인정
                    monster.takeDamage(DAMAGE_PER_HIT);
                }
            }

            // 처치 완료 시각 계산
            long endTime = System.nanoTime();
            double finalTime = (endTime - startTime) / 1_000_000_000.0;
            
            System.out.printf("\n몬스터를 물리쳤습니다! (기록: %.2f초)\n", finalTime);
            return finalTime;
        }

        // 전투 진행 상황(HP 바, 남은 시간)을 한 줄로 표시(캐리지 리턴으로 같은 줄 갱신 시도)
        private void displayBattleStatus(Monster monster, double elapsedTime) {
            int hpBarSize = 20; // HP 바 총 블록 수
            int currentHpBlocks = (int)(((double)monster.getCurrentHp() / monster.getMaxHp()) * hpBarSize);
            if (currentHpBlocks < 0) currentHpBlocks = 0; // 하한 보정

            StringBuilder hpBar = new StringBuilder("[");
            for (int i = 0; i < hpBarSize; i++) {
                hpBar.append(i < currentHpBlocks ? "█" : " ");
            }
            hpBar.append("]");

            System.out.printf("\r%s HP: %s %d/%d | 남은 시간: %.1f초",
                    monster.getName(), hpBar, monster.getCurrentHp(), monster.getMaxHp(),
                    Math.max(0, Settings.MONSTER_TIME_LIMIT_SEC - elapsedTime));
        }
    }

    // Monster: 몬스터 엔터티(이름/HP/아트/생성)
    static class Monster {
        private String name; // 이름
        private int maxHp; // 최대 HP
        private int currentHp; // 현재 HP

        private Monster(String name, int hp) {
            this.name = name;
            this.maxHp = hp;
            this.currentHp = hp;
        }

        // 데미지 반영(HP는 0 미만으로 떨어지지 않도록 보정)
        public void takeDamage(int damage) {
            this.currentHp -= damage;
            if (this.currentHp < 0) {
                this.currentHp = 0;
            }
        }

        // 다음 플레이어 차례를 위해 HP를 최대치로 복구
        public void resetHp() {
            this.currentHp = this.maxHp;
        }

        public String getName() { return name; }
        public int getMaxHp() { return maxHp; }
        public int getCurrentHp() { return currentHp; }
        public String getArt() { return Utils.getMonsterArt(name); }
        public boolean isAlive() { return this.currentHp > 0; }

        // 라운드 시작 시 랜덤 몬스터 생성(이름/HP를 동일 인덱스로 매핑)
        public static Monster createRandomMonster() {
            String[] monsterNames = {
                "박쥐", "슬라임", "버섯", "고블린", "구울",
                "스켈레톤", "미믹", "오크", "와이번", "골렘"
            };
            int[] monsterHps = {
                80, 100, 120, 150, 180, 200, 220, 250, 300, 350
            };

            int index = new Random().nextInt(monsterNames.length);
            return new Monster(monsterNames[index], monsterHps[index]);
        }
    }

    // BattlePlayer: 라운드 승수 관리(이름/승리 횟수)
    static class BattlePlayer {
        private String name; // 플레이어 이름
        private int wins; // 승리 라운드 수

        public BattlePlayer(String name) {
            this.name = name;
            this.wins = 0;
        }

        public void addWin() {
            this.wins++;
        }

        public String getName() {
            return name;
        }

        public int getWins() {
            return wins;
        }
    }
}
