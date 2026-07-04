package hr.brajnovic.td.economy;

public class Economy {

    private int gold;
    private int lives;

    public Economy(int startingGold, int startingLives) {
        this.gold = startingGold;
        this.lives = startingLives;
    }

    public int getGold() {
        return gold;
    }

    public int getLives() {
        return lives;
    }

    public boolean trySpend(int amount) {
        if (amount > gold) {
            return false;
        }
        gold -= amount;
        return true;
    }

    public void addGold(int amount) {
        gold += amount;
    }

    public void loseLives(int amount) {
        lives = Math.max(0, lives - amount);
    }

    public boolean isGameOver() {
        return lives <= 0;
    }
}
