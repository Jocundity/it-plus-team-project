package structures.basic;

import structures.basic.Card;

import java.util.ArrayList;
import java.util.List;
import akka.actor.ActorRef;
import commands.BasicCommands;
import utils.StaticConfFiles;

/**
 * A basic representation of of the Player. A player
 * has health and mana.
 * 
 * @author Dr. Richard McCreadie
 */
public class Player {

    private int health;
    private int maxHealth;
    private int mana;
    private int turnNumber;
    private String avatarConfigFile;
    private boolean hornEquipped = false;
    private int hornDurability = 0;


    // deck and hand management
    private Deck deck;
    private HandManager handManager;
    private List<Card> hand; 

    public Player() {
        super();
        this.health = 20;
        this.mana = 0;
        this.turnNumber = 1;
        this.deck = new Deck();
        this.handManager = new HandManager();
        this.hand = getHandManager().getHandCards();
    }

    public Player(int health, int mana) {
        super();
        this.health = health;
        this.maxHealth = health;
        this.mana = mana;
        this.deck = new Deck();
        this.handManager = new HandManager();
    }

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    
    // Getters and setters for max health
    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }

    public int getMana() { return mana; }
    public void setMana(int mana) { this.mana = mana; }

    public boolean isHornEquipped() {
        return hornEquipped;
    }

    public void setHornEquipped(boolean hornEquipped) {
        this.hornEquipped = hornEquipped;
    }

    public int getHornDurability() {
        return hornDurability;
    }

    public void setHornDurability(int hornDurability) {
        this.hornDurability = hornDurability;
    }
    public int getTurnNumber() { return turnNumber; }

    public String getAvatarConfigFile() { return StaticConfFiles.humanAvatar; }

    public void showLife(ActorRef out) {
        BasicCommands.setPlayer1Health(out, this);
        try { Thread.sleep(200); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    public void showMana(ActorRef out) {
        BasicCommands.setPlayer1Mana(out, this);
        try { Thread.sleep(200); } catch (InterruptedException e) { e.printStackTrace(); }
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

    /**
     * Initialise deck from a list of cards
     */
    public void initDeck(List<Card> cards) {
        this.deck.setCards(cards);
    }

    /**
     * Story Card #2 (Over Draw):
     * Draws a card from the deck and attempts to add it to the hand.
     * If the hand is full (6 cards), the card is discarded and a UI notification is shown.
     */
    public void drawCard(ActorRef out) {

        Card drawnCard = deck.drawTopCard();

        if (drawnCard == null) {
            BasicCommands.addPlayer1Notification(out, "Deck is empty!", 2);
            try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
            return;
        }

        boolean isAdded = handManager.addCardToHand(drawnCard);

        if (isAdded) {
            int handPosition = handManager.getHandCards().size(); // 1..6
            BasicCommands.drawCard(out, drawnCard, handPosition, 0);
            try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
        } else {
            BasicCommands.addPlayer1Notification(out, "Over Draw! Card Lost", 2);
            try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    // Getters
    public Deck getDeck() { return deck; }
    public HandManager getHandManager() { return handManager; }
    public ArrayList<Card> getHand() {return (ArrayList<Card>) hand; }
    
}