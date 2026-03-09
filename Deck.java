package finalProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class Deck {
    private List<Card> cards = new ArrayList<>();
    private int top = 0;

    public Deck(int numDecks) {
        String[] suits = {"Diamonds", "Clubs", "Hearts", "Spades"};
        String[] ranks = {"2","3","4","5","6","7","8","9","T","J","Q","K","A"};
        for (int d = 0; d < numDecks; d++) {
            for (String s : suits) // iterates through each suit
                for (String r : ranks) // iterates through each rank
                    cards.add(new Card(r, s)); // creates a new card object and add it to the list of cards with rank r and suit s
        }
    }

    public void shuffle() {
        Collections.shuffle(cards, new Random());
        top = 0;
    }

    public Card deal() {
        if (top >= cards.size()) shuffle();
        return cards.get(top++);
    }
}
