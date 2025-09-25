package taprun.core;

public abstract class Game {

    // 공용 설정 객체(난이도/파라미터 등). 하위 클래스에서 접근 가능하도록 protected.
    protected Settings settings;

    // 생성자: 외부에서 설정을 주입받아 저장
    public Game(Settings settings) {
        this.settings = settings;
    }

    // 게임 실행 진입점: 각 모드가 구체 동작을 구현
    public abstract void start();
}
