package structures.basic;

import java.util.ArrayList;
import java.util.List;

public class Deck {
    private List<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>();
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }
    public Card drawTopCard() {
        if (cards != null && !cards.isEmpty()) {
            return cards.remove(0);
        }
        return null;
    }

    public int getDeckSize() {
        return cards != null ? cards.size() : 0;
    }
}
