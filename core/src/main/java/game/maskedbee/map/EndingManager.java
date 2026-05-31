package game.maskedbee.map;

public class EndingManager {
    private static EndingManager instance;

    private boolean gameEnded;
    private EndingType currentEnding;

    private EndingManager() {
    }

    public static EndingManager getInstance() {
        if (instance == null) {
            instance = new EndingManager();
        }
        return instance;
    }

    public void triggerEnding(EndingType ending) {

        if (gameEnded) {
            return;
        }

        gameEnded = true;
        currentEnding = ending;

        System.out.println("Ending Triggered: " + ending);
    }

    public boolean isGameEnded() {
        return gameEnded;
    }

    public EndingType getCurrentEnding() {
        return currentEnding;
    }

    public void reset() {
        gameEnded = false;
        currentEnding = null;
    }
}
