package taprun;

import taprun.modes.MonsterMode;
import taprun.modes.RaceMode;
import taprun.modes.VsAiMode;
import taprun.core.Settings;
import taprun.core.RankingManager;

import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

    	// 표준 입력용 Scanner 하나를 앱 생명주기 전체에서 재사용
    	// Scanner를 여러 번 만들면 System.in 버퍼 처리/개행 잔여 문제로 혼란이 생길 수 있음.
        Scanner scanner = new Scanner(System.in);

        Settings settings = new Settings();
        boolean exitRequested = false;

        // 메인 메뉴 루프
        while (!exitRequested) {
            System.out.println("==== Tap Run Game ====");
            System.out.println("1. 달리기 경주 모드");
            System.out.println("2. 몬스터 죽이기 모드");
            System.out.println("3. 대전 모드 (vs AI)");
            System.out.println("4. 랭킹 보기");
            System.out.println("5. 게임 종료");
            System.out.print("선택하세요: ");

            int choice = 0;

            try {
                choice = scanner.nextInt();
                scanner.nextLine(); // 남아있는 개행을 제거

                if (choice == 1) { // 달리기 경주 모드
                    RaceMode raceMode = new RaceMode(settings);
                    // 이 모드 내부에서 엔터 입력을 명확히 받도록 scanner 전달
                    raceMode.start(scanner);

                } else if (choice == 2) { // 몬스터 죽이기 모드
                    MonsterMode monsterMode = new MonsterMode(settings);
                    monsterMode.start(scanner);

                } else if (choice == 3) { // 대전 모드(vs AI)
                    String playerName;
                    while (true) {
                        System.out.print("플레이어 이름을 입력하세요: ");
                        playerName = scanner.nextLine().trim();
                        if (!playerName.isEmpty()) break;
                        System.out.println("이름은 1자 이상이어야 합니다.");
                    }

                    // 난이도 선택(1~3). 잘못된 입력/범위는 루프 재시도.
                    System.out.println("난이도를 선택하세요: 1. EASY  2. NORMAL  3. HARD");
                    int diffNum = 0;
                    while (true) {
                        System.out.print("난이도 번호 입력: ");
                        try {
                            diffNum = scanner.nextInt();
                            scanner.nextLine(); // 남아있는 개행을 제거
                            if (diffNum >= 1 && diffNum <= 3) {
                                break;
                            } else {
                                System.out.println("1~3 사이의 숫자를 입력하세요.");
                            }
                        } catch (InputMismatchException e) { // 숫자가 아닌 입력 방어
                            System.out.println("숫자를 입력해야 합니다.");
                            scanner.next(); // 잘못된 토큰 소비
                        }
                    }

                    // 사용자가 입력한 난이도 번호(1~3)를 열거형 값(EASY/NORMAL/HARD)으로 매핑한다.
                    VsAiMode.Difficulty difficulty = VsAiMode.Difficulty.EASY;
                    if (diffNum == 2) difficulty = VsAiMode.Difficulty.NORMAL;
                    else if (diffNum == 3) difficulty = VsAiMode.Difficulty.HARD;

                    // 모드 시작
                    VsAiMode vsAiMode = new VsAiMode(playerName, difficulty);
                    vsAiMode.start(scanner);

                } else if (choice == 4) {
                    showRankingMenu(scanner); // 랭킹 모드

                } else if (choice == 5) {
                    System.out.println("게임을 종료합니다."); // 프로그램 종료
                    exitRequested = true;

                } else {
                    // 지정된 메뉴 범위 이외의 숫자 처리
                    System.out.println("잘못된 입력입니다. 1~5 사이의 숫자를 입력해주세요.");
                }

            } catch (InputMismatchException e) {
                System.out.println("오류: 숫자를 입력해야 합니다!");
                scanner.next(); // 잘못된 토큰 소비
                continue; // 메인 메뉴 재표시
            }

            // 게임 모드(1~3번) 실행이 끝난 뒤 사용자에게 다음 동작을 물어본다.
            if (!exitRequested && choice >= 1 && choice <= 3) {
                while (true) {
                    System.out.println();
                    System.out.println("1. 메인 메뉴로 돌아가기");
                    System.out.println("2. 종료");
                    System.out.print("선택하세요: ");

                    String again = scanner.nextLine();

                    if (again.trim().equals("1")) {
                        break;
                    } else if (again.trim().equals("2")) {
                        System.out.println("게임을 종료합니다.");
                        exitRequested = true;
                        break;
                    } else {
                        System.out.println("잘못된 입력입니다. 1 또는 2를 입력해주세요.");
                    }
                }
            }
        }
        scanner.close();
    }

    // 랭킹 메뉴
    private static void showRankingMenu(Scanner scanner) {

        while (true) {
            System.out.println("\n==== 랭킹 보기 ====");
            System.out.println("1. 달리기 경주 모드 랭킹");
            System.out.println("2. 몬스터 죽이기 모드 랭킹");
            System.out.println("3. 대전 모드 (vs AI) 랭킹");
            System.out.println("4. 메인 메뉴로 돌아가기");
            System.out.print("선택하세요: ");

            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); // 남아있는 개행을 제거

                // 모드별 랭킹 표시
                if (choice == 1) {
                    RankingManager.showRaceModeRanking();
                } else if (choice == 2) {
                    RankingManager.showMonsterModeRanking();
                } else if (choice == 3) {
                    RankingManager.showVsAiModeRanking();
                } else if (choice == 4) {
                    break;
                } else {
                    System.out.println("잘못된 입력입니다. 1~4 사이의 숫자를 입력해주세요.");
                }

                // 사용자에게 결과를 확인할 시간을 주기 위한 일시 정지
                System.out.println("\n계속하려면 Enter를 누르세요...");
                scanner.nextLine(); // 남아있는 개행을 제거

            } catch (InputMismatchException e) {
                System.out.println("오류: 숫자를 입력해야 합니다!");
                scanner.next(); // 잘못된 토큰 소비
            }
        }
    }
}

