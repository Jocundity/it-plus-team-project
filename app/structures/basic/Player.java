package structures.basic;

import structures.basic.Card;
import java.util.List;
import akka.actor.ActorRef;
import commands.BasicCommands;
import utils.StaticConfFiles;

/**
 * A basic representation of of the Player. A player
 * has health and mana.
 * * @author Dr. Richard McCreadie
 *
 */
public class Player {

    private int health;
    private int mana;
    private int turnNumber;
    private String avatarConfigFile;

    // 你加入的卡组和手牌管理
    private Deck deck;
    private HandManager handManager;

    public Player() {
        super();
        this.health = 20;
        this.mana = 0;
        this.turnNumber = 1;
        this.deck = new Deck();
        this.handManager = new HandManager();
    }

    public Player(int health, int mana) {
        super();
        this.health = health;
        this.mana = mana;
        this.deck = new Deck();
        this.handManager = new HandManager();
    }

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public int getMana() { return mana; }
    public void setMana(int mana) { this.mana = mana; }
    public int getTurnNumber() { return turnNumber; }

    public String getAvatarConfigFile() { return StaticConfFiles.humanAvatar; }

    public void showLife(ActorRef out) {
        BasicCommands.setPlayer1Health(out, this);
        try {Thread.sleep(200);} catch (InterruptedException e) {e.printStackTrace();}
    }

    public void showMana(ActorRef out) {
        BasicCommands.setPlayer1Mana(out, this);
        try {Thread.sleep(200);} catch (InterruptedException e) {e.printStackTrace();}
    }

    public void startTurn(ActorRef out) {
        mana = turnNumber + 1;
        turnNumber++;
        showMana(out);
    }

    public void drainMana(ActorRef out) {
        mana = 0;
        showMana(out);
    }

    // 你的 Get 方法
    public Deck getDeck() { return deck; }
    public HandManager getHandManager() { return handManager; }
    // Initialise deck from a list of cards
public void initDeck(List<Card> cards) {
    this.deck.setCards(cards);
}

// Draw a card from the top of the deck and render it in the player's hand
public void drawCard(ActorRef out) {
    Card drawn = deck.drawTopCard();
    if (drawn == null) return;

    boolean added = handManager.addCardToHand(drawn);
    if (!added) return; // hand full -> discarded

    int pos = handManager.getHandCards().size(); // 1..6
    BasicCommands.drawCard(out, drawn, pos, 0);

    try { Thread.sleep(200); } catch (InterruptedException e) { e.printStackTrace(); }
}

}