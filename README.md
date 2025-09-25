# Tap Run — 콘솔 탭 러닝 게임 (Java)

세 가지 미니 게임을 콘솔에서 즐기는 **탭(Enter) 기반 러닝 게임**입니다.

- **Race Mode**: 두 플레이어가 20번의 *빈 Enter* 입력 시간을 겨룹니다. (주사위로 선/후공 결정, 시작키 게이팅 포함)
- **Monster Mode**: 제한 시간 안에 *빈 Enter* 연타로 같은 몬스터를 빠르게 처치(3라운드).
- **Vs AI Mode**: AI와 경주. *최소 입력 간격(Quiet Gap)* 으로 오토/홀드 입력 방지, `System.nanoTime()` 기반 타이머.

## 주요 포인트
- **치트 방지**: Quiet Gap 이상일 때만 탭 1회 인정
- **정확한 타이머**: `System.nanoTime()`
- **랭킹 시스템**: 텍스트 파일(`rankings/`)에 기록 저장  
  - 동일 이름인 경우 **더 좋은 기록만 갱신(업서트)**  
  - VsAI는 **난이도별 그룹(EASY/NORMAL/HARD)** 정렬/표시  
  - **파일에는 전체 이력 저장**, 콘솔은 상위 20개만 출력

> `rankings/` 폴더는 `.gitignore` 처리되어 레포에는 올라가지 않습니다.

## 실행 방법 (Windows)

> **JDK 11+** 권장. 저장소 루트에서 아래 명령 실행

```bat
javac -encoding UTF-8 -d . taprun\core\*.java taprun\modes\*.java taprun\Main.java
java taprun.Main
