# Tap Run — 콘솔 탭(Enter) 러닝 미니게임 (Java)

> Enter 키 연타만으로 즐기는 3가지 콘솔 러닝 미니게임  
> **치트 방지(Quiet Gap)** · **nanoTime 기반 정확한 타이머** · **파일 랭킹 저장/업서트**

[![Java 11](https://img.shields.io/badge/Java-11-blue)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 1) 프로젝트 소개
콘솔에서 **Enter(탭) 입력 타이밍/속도**로 승부하는 러닝 미니게임입니다.  
단순 연타 게임처럼 보이지만, **오토/홀드 입력을 막는 치트 방지 로직(Quiet Gap)**과  
**`System.nanoTime()`을 이용한 정밀한 시간 측정**,  
**텍스트 파일 기반 랭킹 시스템**을 직접 설계/구현했습니다.

---

## 2) 게임 모드

### 🏁 Race Mode
- **조작**: 빈 Enter 입력
- **목표**: 두 플레이어가 **20번 Enter를 더 빠르게** 입력하면 승리
- **특징**
  - 주사위로 선/후공 결정
  - 시작 키(1/2) 입력 게이팅으로 **조기 입력/실수 방지**

### 👾 Monster Mode
- **조작**: Enter 연타
- **목표**: 제한 시간 안에 Enter 연타로 몬스터 HP를 0으로 만들기
- **특징**
  - **3라운드 누적 진행**
  - 라운드별 처치 속도 기록

### 🤖 Vs AI Mode
- **조작**: Enter 입력
- **목표**: AI보다 빠르게 결승점 도달
- **특징**
  - **Quiet Gap(최소 입력 간격)**을 만족할 때만 “1탭”으로 인정  
    → 오토/홀드 입력 방지
  - 난이도(EASY/NORMAL/HARD)별 AI 속도/패턴 분리

---

## 3) 핵심 기능

### ✅ 치트 방지: Quiet Gap
- 연속 입력 간격이 **Quiet Gap 이상일 때만 탭 1회로 인정**
- 홀드 입력/매크로성 초고속 입력 무효화
- Vs AI / Monster 등 전 모드에 공통 적용

### ✅ 정밀 타이머
- `System.nanoTime()` 기반으로 Enter 입력 시간 측정
- 밀리초 단위 오차를 줄여 **공정한 기록 경쟁** 가능

### ✅ 랭킹 시스템 (파일 저장)
- 텍스트 파일(`rankings/`)에 기록을 저장
- 같은 이름이 다시 등장하면 **더 좋은 기록만 업서트(갱신)**
- Vs AI는 **난이도 그룹(EASY/NORMAL/HARD)별로 정렬/표시**
- 파일에는 전체 이력 저장, 콘솔에는 상위 20개만 출력

> `rankings/` 폴더는 `.gitignore` 처리되어 레포에는 올라가지 않습니다.

---

## 4) 프로젝트 구조
```text
taprun/
 ├ Main.java              # 게임 진입/모드 선택
 ├ core/                  # 공통 로직(타이머, 입력 처리, 유틸)
 ├ modes/                 # Race / Monster / Vs AI 모드 구현
 └ rankings/              # 랭킹 파일 저장 폴더(레포 제외)
 core: 입력/시간 측정/공통 룰 같은 “게임 엔진 역할”

modes: 각 모드의 규칙과 진행을 독립적으로 구현
→ 모드 추가/확장이 쉬운 OOP 구조

5) 실행 방법 (Windows)

JDK 11+ 권장. 저장소 루트에서 아래 명령 실행

javac -encoding UTF-8 -d . taprun\core\*.java taprun\modes\*.java taprun\Main.java
java taprun.Main

6) Demo

GIF/스크린샷 추가 예정
(assets 폴더가 없으면 아래 이미지는 지워도 OK)

7) 내 역할 & 핵심 기여

치트 방지(Quiet Gap) 설계/구현

문제: Enter 홀드/매크로로 기록 왜곡 가능

해결: 최소 입력 간격 조건 도입 → 정상 탭만 인정

nanoTime 기반 정밀 타이머 구현

입력 구간별 시간 측정 안정화

타이머 오차로 인한 승패 왜곡 최소화

랭킹 업서트/난이도 그룹 정렬 로직 구현

같은 이름은 항상 최고 기록만 유지

Vs AI는 난이도별 랭킹 분리로 공정성 확보

UX 디테일 개선

선/후공 주사위, 시작 키 게이팅

진행바/HP바로 콘솔 가시성 향상

입력/파일 파싱 예외 처리

잘못된 입력 방어

깨진 랭킹 라인은 스킵 처리로 안정성 확보

8) 개선 계획 (Roadmap)

 3-2-1 카운트다운 스타트

 모드별 튜토리얼/도움말 UI 추가

 Mac/Linux 실행 스크립트(.sh) 제공

 Demo GIF 및 플레이 영상 첨부

 신규 모드(예: Time Attack / Endless) 확장
