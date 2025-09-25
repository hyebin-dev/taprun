package taprun.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RankingManager {
    // 랭킹 파일이 저장될 폴더 이름
    private static final String RANKING_DIR = "rankings";
    // 각 게임 모드별 랭킹 파일 이름
    private static final String RACE_MODE_FILE = "Ranking_RaceMode.txt";
    private static final String MONSTER_MODE_FILE = "Ranking_MonsterMode.txt";
    private static final String VSAI_MODE_FILE = "Ranking_VsAiMode.txt";
    // 날짜와 시간을 저장할 때 사용할 형식 지정
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 난이도 고정 순서(정렬/표시/저장 모두 일관)
    // - 문자열 정렬이 아닌 명시적 우선순위를 부여해 EASY < NORMAL < HARD 로 일괄 정렬
    private static final Map<String, Integer> DIFF_ORDER = new HashMap<>();
    static {
        DIFF_ORDER.put("EASY", 0);
        DIFF_ORDER.put("NORMAL", 1);
        DIFF_ORDER.put("HARD", 2);
    }

    // 랭킹에 들어갈 한 사람의 기록을 저장하는 클래스입니다.
    public static class RankingEntry {
        private String playerName; // 플레이어 이름
        private double time; // 기록(초 단위) — 낮을수록 좋음
        private String difficulty; // 대전 모드의 난이도 (Race/Monster 모드에서는 빈 문자열)
        private LocalDateTime dateTime; // 기록이 저장된 날짜와 시간

        // Race/Monster 모드용 생성자 (난이도 없음)
        public RankingEntry(String playerName, double time, LocalDateTime dateTime) {
            this.playerName = playerName;
            this.time = time;
            this.dateTime = dateTime;
            this.difficulty = "";
        }

        // VsAiMode(대전 모드)용 생성자 (난이도 포함)
        public RankingEntry(String playerName, double time, String difficulty, LocalDateTime dateTime) {
            this.playerName = playerName;
            this.time = time;
            this.difficulty = difficulty;
            this.dateTime = dateTime;
        }

        // 접근자 — 파일 저장/표시/정렬 시 사용
        public String getPlayerName() { return playerName; }
        public double getTime() { return time; }
        public String getDifficulty() { return difficulty; }
        public LocalDateTime getDateTime() { return dateTime; }
    }

    // 랭킹 폴더가 없으면 새로 만드는 메서드입니다.
    private static void initializeRankingDirectory() {
        try {
            Path rankingPath = Paths.get(RANKING_DIR);
            if (!Files.exists(rankingPath)) {
                Files.createDirectories(rankingPath); // 폴더가 없으면 생성
            }
        } catch (IOException e) {
            // 파일 시스템 오류에 대한 사용자 메시지 + 디버그 로그 분리
            System.err.println("랭킹 디렉토리 생성 실패: " + e.getMessage());
            System.out.println("랭킹 저장 폴더를 만들지 못했습니다. 권한/디스크 상태를 확인해주세요.");
        }
    }

    // 동일 이름의 기존 기록이 있으면 더 좋은(=더 작은) time일 때 갱신, 없으면 추가
    private static boolean upsertTimeByName(List<RankingEntry> list, RankingEntry newEntry) {
        int existingIdx = -1;
        for (int i = 0; i < list.size(); i++) {
            RankingEntry e = list.get(i);
            if (e.getPlayerName().equals(newEntry.getPlayerName())) {
                existingIdx = i;
                break;
            }
        }
        if (existingIdx >= 0) {
            // 더 좋은 기록(작은 시간)일 때만 교체
            if (newEntry.getTime() <= list.get(existingIdx).getTime()) {
                list.set(existingIdx, newEntry);
                return true; // 갱신 발생
            }
            return false; // 기존이 더 좋음 → 변경 없음
        } else {
            list.add(newEntry); // 신규 추가
            return true;
        }
    }

    // 이름 + 난이도 (VsAiMode 전용)
    private static boolean upsertTimeByNameAndDiff(List<RankingEntry> list, RankingEntry newEntry) {
        int existingIdx = -1;
        for (int i = 0; i < list.size(); i++) {
            RankingEntry e = list.get(i);
            if (e.getPlayerName().equals(newEntry.getPlayerName())
                    && e.getDifficulty().equals(newEntry.getDifficulty())) {
                existingIdx = i;
                break;
            }
        }
        if (existingIdx >= 0) {
            if (newEntry.getTime() <= list.get(existingIdx).getTime()) {
                list.set(existingIdx, newEntry);
                return true;
            }
            return false;
        } else {
            list.add(newEntry);
            return true;
        }
    }

    // 달리기 경주 모드
    public static boolean saveRaceModeRanking(String playerName, double time) {
        if (playerName == null || playerName.trim().isEmpty()) { // 이름이 없으면 등록 불가
            System.out.println("플레이어 이름이 입력되지 않아 랭킹에 등록되지 않습니다.");
            return false;
        }
        initializeRankingDirectory();
        List<RankingEntry> rankings = loadRankings(RACE_MODE_FILE, false); // 기존 전체 로드
        boolean updated = upsertTimeByName(rankings, new RankingEntry(playerName, time, LocalDateTime.now()));
        rankings.sort(Comparator.comparingDouble(RankingEntry::getTime));   // 시간 오름차순(빠른 순)
        saveRankings(RACE_MODE_FILE, rankings, false); // 전체 저장(Top N 제한 없음)
        System.out.println(updated ? "🏆 새로운 기록이 랭킹에 등록되었습니다!" :
                                     "아쉽습니다! 기존 기록이 더 높아서 등록되지 않았습니다.");
        showTopRankings(rankings, "달리기 경주 모드", false); // 화면 출력은 Top 20으로 제한
        return updated;
    }

    public static boolean saveRaceModeRankingQuiet(String playerName, double time) {
        if (playerName == null || playerName.trim().isEmpty()) return false;
        initializeRankingDirectory();
        List<RankingEntry> rankings = loadRankings(RACE_MODE_FILE, false);
        boolean updated = upsertTimeByName(rankings, new RankingEntry(playerName, time, LocalDateTime.now()));
        rankings.sort(Comparator.comparingDouble(RankingEntry::getTime));
        saveRankings(RACE_MODE_FILE, rankings, false); // 조용히 저장만(출력 X)
        return updated;
    }

    // 몬스터 모드
    public static boolean saveMonsterModeRanking(String playerName, double totalTime) {
        if (playerName == null || playerName.trim().isEmpty()) {
            System.out.println("플레이어 이름이 입력되지 않아 랭킹에 등록되지 않습니다.");
            return false;
        }
        initializeRankingDirectory();
        List<RankingEntry> rankings = loadRankings(MONSTER_MODE_FILE, false);
        boolean updated = upsertTimeByName(rankings, new RankingEntry(playerName, totalTime, LocalDateTime.now()));
        rankings.sort(Comparator.comparingDouble(RankingEntry::getTime));
        saveRankings(MONSTER_MODE_FILE, rankings, false); // 전체 저장
        System.out.println(updated ? "🏆 새로운 기록이 랭킹에 등록되었습니다!" :
                                     "아쉽습니다! 기존 기록이 더 높아서 등록되지 않았습니다.");
        showTopRankings(rankings, "몬스터 죽이기 모드", false);
        return updated;
    }

    public static boolean saveMonsterModeRankingQuiet(String playerName, double totalTime) {
        if (playerName == null || playerName.trim().isEmpty()) return false;
        initializeRankingDirectory();
        List<RankingEntry> rankings = loadRankings(MONSTER_MODE_FILE, false);
        boolean updated = upsertTimeByName(rankings, new RankingEntry(playerName, totalTime, LocalDateTime.now()));
        rankings.sort(Comparator.comparingDouble(RankingEntry::getTime));
        saveRankings(MONSTER_MODE_FILE, rankings, false); // 전체 저장
        return updated;
    }

    // VsAiMode
    public static boolean saveVsAiModeRanking(String playerName, double time, String difficulty) {
        if (playerName == null || playerName.trim().isEmpty()) {
            System.out.println("플레이어 이름이 입력되지 않아 랭킹에 등록되지 않습니다.");
            return false;
        }
        initializeRankingDirectory();
        List<RankingEntry> rankings = loadRankings(VSAI_MODE_FILE, true);
        boolean updated = upsertTimeByNameAndDiff(rankings,
                new RankingEntry(playerName, time, difficulty, LocalDateTime.now()));

        // 난이도 → 고정 순서 → 시간 순 정렬
        // - 다중 정렬 키: (난이도 우선순위) 다음 (기록시간)
        rankings.sort(Comparator
                .comparing((RankingEntry e) -> DIFF_ORDER.getOrDefault(e.getDifficulty(), 99))
                .thenComparingDouble(RankingEntry::getTime));

        saveRankings(VSAI_MODE_FILE, rankings, true); // 전체 저장
        System.out.println(updated ? "🏆 새로운 기록이 랭킹에 등록되었습니다!" :
                                     "아쉽습니다! 기존 기록이 더 높아서 등록되지 않았습니다.");
        showTopRankings(rankings, "대전 모드 (vs AI)", true);
        return updated;
    }

    // 랭킹 파일에서 기록들을 불러오는 메서드
    // 일반 모드 → "[순위] 이름 - 12.34초 (yyyy-MM-dd HH:mm:ss)"
    // VsAI 모드 → 난이도 헤더 라인("=== EASY 난이도 랭킹 ===")를 만나면 이후 레코드에 해당 난이도로 인식
    private static List<RankingEntry> loadRankings(String fileName, boolean hasDifficulty) {
        List<RankingEntry> rankings = new ArrayList<>();
        Path filePath = Paths.get(RANKING_DIR, fileName);
        if (!Files.exists(filePath)) return rankings; // 파일 없으면 빈 목록 반환(초기 상태)

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            String currentDifficulty = "EASY"; // VsAI 모드에서 헤더를 만나기 전 기본값
            while ((line = reader.readLine()) != null) {
                try {
                    if (line.trim().isEmpty()) continue; // 빈 줄 스킵

                    // 난이도 헤더
                    if (hasDifficulty && line.startsWith("===") && line.contains("난이도")) {
                        // 헤더 내용에 따라 현재 파싱 난이도를 갱신
                        if (line.contains("EASY")) currentDifficulty = "EASY";
                        else if (line.contains("NORMAL")) currentDifficulty = "NORMAL";
                        else if (line.contains("HARD")) currentDifficulty = "HARD";
                        continue; // 헤더 라인은 레코드가 아님
                    }
                    // [1위] 플레이어명 - 10.25초 (2024-12-22 14:30:52)
                    if (line.startsWith("[")) {
                        String withoutRank = line.substring(line.indexOf("]") + 1).trim(); // "[n위]" 제거
                        String[] parts = withoutRank.split(" - "); // "이름 - 시간초 (날짜)"로 분리
                        if (parts.length >= 2) {
                            String name = parts[0].trim();
                            String timeAndDate = parts[1];

                            int secondsIndex = timeAndDate.indexOf("초");
                            int openParen = timeAndDate.indexOf("(");
                            int closeParen = timeAndDate.indexOf(")");
                            // 안전하게 위치 검사 후 파싱
                            if (secondsIndex > 0 && openParen > secondsIndex && closeParen > openParen) {
                                double time = Double.parseDouble(timeAndDate.substring(0, secondsIndex));
                                String dateStr = timeAndDate.substring(openParen + 1, closeParen);
                                LocalDateTime dateTime = LocalDateTime.parse(dateStr, DATE_FORMAT);
                                if (hasDifficulty) {
                                    rankings.add(new RankingEntry(name, time, currentDifficulty, dateTime));
                                } else {
                                    rankings.add(new RankingEntry(name, time, dateTime));
                                }
                            }
                        }
                    }
                } catch (Exception e) { // 잘못된 라인은 스킵하고 다음으로 진행
                    System.err.println("랭킹 파일 라인 파싱 오류: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("랭킹 파일 로드 실패: " + e.getMessage());
            System.out.println("랭킹 파일을 불러오지 못했습니다. 파일 권한/손상 여부를 확인해주세요.");
        }
        return rankings;
    }

    // 랭킹 정보를 파일에 저장 (전체 저장)
    // - 출력은 Top 20로 제한하지만, 파일에는 전체를 보존해 이력 관리/재정렬 등에 유리
    private static void saveRankings(String fileName, List<RankingEntry> rankings, boolean hasDifficulty) {
        Path filePath = Paths.get(RANKING_DIR, fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            if (hasDifficulty) {
                // 난이도별 그룹으로 나눠 저장
                Map<String, List<RankingEntry>> difficultyGroups = new HashMap<>();
                for (RankingEntry entry : rankings) {
                    difficultyGroups.computeIfAbsent(entry.getDifficulty(), k -> new ArrayList<>()).add(entry);
                }
                String[] difficulties = {"EASY", "NORMAL", "HARD"};
                for (String diff : difficulties) {
                    List<RankingEntry> list = difficultyGroups.get(diff);
                    if (list == null || list.isEmpty()) continue;

                    writer.write("=== " + diff + " 난이도 랭킹 ===");
                    writer.newLine();

                    list.sort(Comparator.comparingDouble(RankingEntry::getTime)); // 각 난이도 내 시간순
                    // 전체 저장 (상위 N 제한 제거)
                    for (int i = 0; i < list.size(); i++) {
                        RankingEntry e = list.get(i);
                        writer.write(String.format("[%d위] %s - %.2f초 (%s)",
                                i + 1, e.getPlayerName(), e.getTime(),
                                e.getDateTime().format(DATE_FORMAT)));
                        writer.newLine();
                    }
                    writer.newLine(); // 난이도 섹션 간 공백 라인
                }
            } else {
                // 일반 랭킹 전체 저장
                for (int i = 0; i < rankings.size(); i++) {
                    RankingEntry e = rankings.get(i);
                    writer.write(String.format("[%d위] %s - %.2f초 (%s)",
                            i + 1, e.getPlayerName(), e.getTime(),
                            e.getDateTime().format(DATE_FORMAT)));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("랭킹 파일 저장 실패: " + e.getMessage());
            System.out.println("랭킹 저장에 실패했습니다. 디스크 용량/권한을 확인해주세요.");
        }
    }

    public static void showRaceModeRanking() {
        List<RankingEntry> rankings = loadRankings(RACE_MODE_FILE, false);
        showTopRankings(rankings, "달리기 경주 모드", false);
    }

    public static void showMonsterModeRanking() {
        List<RankingEntry> rankings = loadRankings(MONSTER_MODE_FILE, false);
        showTopRankings(rankings, "몬스터 죽이기 모드", false);
    }

    public static void showVsAiModeRanking() {
        List<RankingEntry> rankings = loadRankings(VSAI_MODE_FILE, true);
        showTopRankings(rankings, "대전 모드 (vs AI)", true);
    }

    // 화면 출력은 Top 20만
    // - 파일에는 전체 기록을 저장하되, 사용자에게는 상위 20개만 보여 UI 과밀을 방지
    private static void showTopRankings(List<RankingEntry> rankings, String gameMode, boolean hasDifficulty) {
        System.out.println("\n=== " + gameMode + " 랭킹 ===");
        if (rankings.isEmpty()) {
            System.out.println("아직 등록된 랭킹이 없습니다.");
            return;
        }
        if (hasDifficulty) {
            // 난이도별로 묶어서 각 섹션의 상위 20개만 출력
            Map<String, List<RankingEntry>> groups = new HashMap<>();
            for (RankingEntry e : rankings) {
                groups.computeIfAbsent(e.getDifficulty(), k -> new ArrayList<>()).add(e);
            }
            String[] difficulties = {"EASY", "NORMAL", "HARD"};
            for (String diff : difficulties) {
                List<RankingEntry> list = groups.get(diff);
                if (list == null || list.isEmpty()) continue;
                System.out.println("\n--- " + diff + " 난이도 ---");
                list.sort(Comparator.comparingDouble(RankingEntry::getTime));
                for (int i = 0; i < Math.min(20, list.size()); i++) {
                    RankingEntry e = list.get(i);
                    System.out.printf("[%d위] %s - %.2f초 (%s)\n",
                            i + 1, e.getPlayerName(), e.getTime(),
                            e.getDateTime().format(DATE_FORMAT));
                }
            }
        } else {
            // 단일 랭킹: 상위 20개만 출력
            for (int i = 0; i < Math.min(20, rankings.size()); i++) {
                RankingEntry e = rankings.get(i);
                System.out.printf("[%d위] %s - %.2f초 (%s)\n",
                        i + 1, e.getPlayerName(), e.getTime(),
                        e.getDateTime().format(DATE_FORMAT));
            }
        }
        System.out.println();
    }
}
