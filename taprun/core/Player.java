package taprun.core;

public class Player {

    private String name; // 플레이어 이름

    public Player(String name) { // 생성자
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void showStatus() {
        System.out.println("--- [" + name + "] 님의 상태 ---");
        System.out.println("--------------------");
    }
}
