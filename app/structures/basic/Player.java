package structures.basic;

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

    // deck and hand management
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
    
 // hello this is a change
    /**
     * Story Card #2 (Over Draw): 
     * Draws a card from the deck and attempts to add it to the hand.
     * If the hand is full (6 cards), the HandManager rejects it,
     * and the card is effectively deleted (lost).
     */
    public void drawCard(ActorRef out) {
        // Draw a card from the top of the deck
        Card drawnCard = deck.drawTopCard();

        if (drawnCard != null) {
            // Attempt to add the card to the hand. HandManager automatically checks capacity.
            boolean isAdded = handManager.addCardToHand(drawnCard);

            if (isAdded) {
                // Hand is not full, card successfully added. Render it to the frontend.
                int handPosition = handManager.getHandCards().size();
                BasicCommands.drawCard(out, drawnCard, handPosition, 0);
                try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
            } else {
                // Hand is full (Over Draw), card is discarded
                // Display a notification on the frontend to inform the player that the card is lost
                BasicCommands.addPlayer1Notification(out, "Over Draw! Card Lost", 2);
                try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
            }
        } else {
            BasicCommands.addPlayer1Notification(out, "Deck is empty!", 2);
        }
    }
    
    // Get method
    public Deck getDeck() { return deck; }
    public HandManager getHandManager() { return handManager; }
}
