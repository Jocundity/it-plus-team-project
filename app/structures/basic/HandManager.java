package structures.basic;

import java.util.ArrayList;
import java.util.List;

public class HandManager {
    private List<Card> handCards;
    private final int MAX_HAND_SIZE = 6;

    public HandManager() {
        this.handCards = new ArrayList<>();
    }
    public boolean addCardToHand(Card card) {
        if (card == null) return false;

        if (handCards.size() < MAX_HAND_SIZE) {
            handCards.add(card);
            return true;
        } else {
            System.out.println("Hand is full (max 6). Card discarded: " + card.getCardname());
            return false;
        }
    }

    public List<Card> getHandCards() {
        return handCards;
    }
}
